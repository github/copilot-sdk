/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;
use crate::generated::api_types::{
    CafeMetricsRelease, CafeMetricsResolveAdmissionRequest,
    CafeMetricsResolveAdmissionRequestFailed, CafeMetricsResolveAdmissionRequestFailedKind,
    CafeMetricsResolveAdmissionRequestSuccess, CafeMetricsResolveAdmissionRequestSuccessKind,
    CafeMetricsResolveAdmissionResult,
};

fn requests() -> [(CafeMetricsResolveAdmissionRequest, serde_json::Value); 2] {
    [
        (
            CafeMetricsResolveAdmissionRequest::Success(
                CafeMetricsResolveAdmissionRequestSuccess {
                    kind: CafeMetricsResolveAdmissionRequestSuccessKind::Success,
                    lookup_duration_ms: 12.5,
                    releases: vec![CafeMetricsRelease {
                        draft: Some(false),
                        prerelease: false,
                        tag_name: "v1.2.3".to_owned(),
                    }],
                },
            ),
            serde_json::json!({
                "kind": "success",
                "lookupDurationMs": 12.5,
                "releases": [{"draft": false, "prerelease": false, "tagName": "v1.2.3"}],
            }),
        ),
        (
            CafeMetricsResolveAdmissionRequest::Failed(CafeMetricsResolveAdmissionRequestFailed {
                kind: CafeMetricsResolveAdmissionRequestFailedKind::Failed,
                lookup_duration_ms: 3.25,
            }),
            serde_json::json!({"kind": "failed", "lookupDurationMs": 3.25}),
        ),
    ]
}

#[tokio::test]
async fn typed_admission_preserves_each_required_union_payload() {
    for (request, expected) in requests() {
        let home = tempfile::tempdir().unwrap();
        let (client_stream, server_stream) = tokio::io::duplex(8192);
        let (client_read, client_write) = tokio::io::split(client_stream);
        let (server_read, server_write) = tokio::io::split(server_stream);
        let client =
            Client::from_streams(client_read, client_write, home.path().to_owned()).unwrap();
        let (notifications, _) = tokio::sync::broadcast::channel(1);
        let (requests, _) = tokio::sync::mpsc::unbounded_channel();
        let server =
            jsonrpc::JsonRpcClient::new(server_write, server_read, notifications, requests);
        let (captured, mut received) = tokio::sync::mpsc::unbounded_channel();
        server
            .register_request_handler(
                "cafeMetrics.resolveAdmission",
                Arc::new(move |params| {
                    captured.send(params).unwrap();
                    Box::pin(async { Ok(serde_json::json!({"kind": "applied"})) })
                }),
            )
            .unwrap();
        let result = tokio::time::timeout(
            Duration::from_secs(5),
            client.rpc().cafe_metrics().resolve_admission(request),
        )
        .await;
        client.force_stop();
        server.force_close();

        assert!(matches!(
            result.unwrap().unwrap(),
            CafeMetricsResolveAdmissionResult::Applied(_)
        ));
        assert_eq!(received.try_recv().unwrap(), expected);
    }
}

#[cfg(feature = "local-runtime")]
#[tokio::test]
async fn ordinary_local_runtime_denies_cafe_without_disabling_standard_calls() {
    use crate::generated::api_types::{
        CafeMetricsEmbeddedPackageIdentity, CafeMetricsStartRequest,
    };

    let home = tempfile::tempdir().unwrap();
    let client = Client::start(
        ClientOptions::new()
            .with_transport(Transport::InProcess)
            .with_base_directory(home.path())
            .with_use_logged_in_user(false),
    )
    .await
    .unwrap();
    let started = client
        .rpc()
        .cafe_metrics()
        .start(CafeMetricsStartRequest {
            app_version: "0.0.1".to_owned(),
            build_version: "0.0.1".to_owned(),
            custom_provider_configured: false,
            embedded_package_identity: Some(CafeMetricsEmbeddedPackageIdentity::Dev),
        })
        .await;
    let typed_internal = crate::cli_host::rpc(&client)
        .cafe_metrics()
        .shutdown()
        .await;
    let ping = client
        .call("ping", Some(serde_json::json!({"message": "ordinary-sdk"})))
        .await;
    let stopped = client.stop().await;
    for (method, result) in [
        ("cafeMetrics.start", started.map(|_| ())),
        ("typed internal cafeMetrics.shutdown", typed_internal),
    ] {
        let error = result.expect_err("an ordinary SDK client must remain unprivileged");
        assert!(
            error
                .to_string()
                .contains("live trusted CLI host connection"),
            "{method}: {error}"
        );
    }
    assert_eq!(ping.unwrap()["message"], "pong: ordinary-sdk");
    stopped.unwrap();
}
