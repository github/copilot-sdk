//! Real runtime-supervised AHP listener tests. See scripts/runtime-host-e2e.md.
#![cfg(all(feature = "test-support", target_os = "linux"))]
#![allow(clippy::unwrap_used)]

#[path = "e2e/runtime_host_support.rs"]
mod host_support;
#[allow(dead_code)]
#[path = "e2e/support.rs"]
mod support;

use std::time::Duration;

use ahp_ws::WebSocketTransport;
use github_copilot_sdk::rpc::{HostDisposeRequest, HostExitReason};
use github_copilot_sdk::{
    AhpHostOptions, Client, ClientOptions, ResumeSessionConfig, SessionId, Transport,
};
use serde_json::{Value, json};

use host_support::*;

#[tokio::test]
#[ignore = "requires final locally built runtime/provider/lite; source and candidate instructions in scripts/runtime-host-e2e.md"]
#[serial_test::serial]
async fn streams_real_ahp_turn_beside_sdk_session_on_same_runtime() {
    run(|ctx| {
        Box::pin(async move {
            let owner = start(ctx).await;
            let sdk = owner
                .create_session(
                    ctx.approve_all_session_config()
                        .with_model("claude-sonnet-5")
                        .with_streaming(true),
                )
                .await
                .unwrap();
            let host = owner
                .start_ahp_host(AhpHostOptions::default())
                .await
                .unwrap();
            let ahp = connect(&host).await;
            let (uri, chat, subscription) = create(&ahp, ctx).await;
            topology(&host, &owner, &home(ctx).join("ahp/sessions"));
            let (_, response) =
                tokio::join!(turn(&ahp, &chat, subscription), sdk.send_and_wait(PROMPT));
            assert!(
                response.unwrap().unwrap().data["content"]
                    .as_str()
                    .unwrap()
                    .contains('4')
            );
            let id = uri.strip_prefix("ahp-session:/").unwrap();
            let sessions = owner.list_sessions(None).await.unwrap();
            assert!(
                sessions
                    .iter()
                    .any(|session| session.session_id.as_str() == id)
            );
            assert!(
                sessions
                    .iter()
                    .any(|session| session.session_id == sdk.id())
            );
            let observer = owner
                .resume_session(
                    ResumeSessionConfig::new(SessionId::from(id)).with_permission_handler(
                        std::sync::Arc::new(github_copilot_sdk::handler::ApproveAllHandler),
                    ),
                )
                .await
                .unwrap();
            assert!(observer.get_events().await.unwrap().iter().any(|event| {
                event.event_type == "assistant.message"
                    && event.data["content"]
                        .as_str()
                        .is_some_and(|text| text.contains('4'))
            }));
            host.dispose().await.unwrap();
            stopped(&host, &ahp).await;
            assert!(!sdk.get_events().await.unwrap().is_empty());
            ahp.client.shutdown().await;
            owner.stop().await.unwrap();
        })
    })
    .await;
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn durable_catalog_resumes_after_dispose_and_sigkill() {
    run(|ctx| {
        Box::pin(async move {
            let owner = start(ctx).await;
            let sdk = owner
                .create_session(ctx.approve_all_session_config())
                .await
                .unwrap();
            let host = owner
                .start_ahp_host(AhpHostOptions::default())
                .await
                .unwrap();
            let ahp = connect(&host).await;
            let (uri, chat, subscription) = create(&ahp, ctx).await;
            turn(&ahp, &chat, subscription).await;
            host.dispose().await.unwrap();
            stopped(&host, &ahp).await;
            ahp.client.shutdown().await;
            let (options, exits) = exit_observer();
            let replacement = owner.start_ahp_host(options).await.unwrap();
            let ahp = connect(&replacement).await;
            resume(&ahp, &uri, Some(sdk.id().as_str())).await;
            kill(&replacement);
            assert_eq!(exit(&exits).await.reason, HostExitReason::Exited);
            stopped(&replacement, &ahp).await;
            ahp.client.shutdown().await;
            let recovered = owner
                .start_ahp_host(AhpHostOptions::default())
                .await
                .unwrap();
            let ahp = connect(&recovered).await;
            resume(&ahp, &uri, Some(sdk.id().as_str())).await;
            sdk.get_events().await.unwrap();
            recovered.dispose().await.unwrap();
            stopped(&recovered, &ahp).await;
            ahp.client.shutdown().await;
            owner.stop().await.unwrap();
        })
    })
    .await;
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn explicit_base_directory_survives_runtime_restart_and_excludes_other_writer() {
    run(|ctx| {
        Box::pin(async move {
            let base = home(ctx).join("explicit-base");
            std::fs::create_dir_all(&base).unwrap();
            let first = Client::start(options(ctx).with_base_directory(&base))
                .await
                .unwrap();
            let second = Client::start(options(ctx).with_base_directory(&base))
                .await
                .unwrap();
            let host = first
                .start_ahp_host(AhpHostOptions::default())
                .await
                .unwrap();
            let ahp = connect(&host).await;
            topology(&host, &first, &base.join("ahp/sessions"));
            let (uri, chat, subscription) = create(&ahp, ctx).await;
            turn(&ahp, &chat, subscription).await;
            let sdk = second
                .create_session(ctx.approve_all_session_config())
                .await
                .unwrap();
            let error = second
                .start_ahp_host(AhpHostOptions::default())
                .await
                .err()
                .unwrap();
            assert!(
                error.to_string().contains("catalog already in use"),
                "{error}"
            );
            sdk.get_events().await.unwrap();
            ahp.client.ping().await.unwrap();
            host.dispose().await.unwrap();
            stopped(&host, &ahp).await;
            ahp.client.shutdown().await;
            let old_pid = first.pid().unwrap();
            first.stop().await.unwrap();
            second.stop().await.unwrap();
            reaped(old_pid).await;
            let restarted = Client::start(options(ctx).with_base_directory(&base))
                .await
                .unwrap();
            let host = restarted
                .start_ahp_host(AhpHostOptions::default())
                .await
                .unwrap();
            let ahp = connect(&host).await;
            resume(&ahp, &uri, None).await;
            host.dispose().await.unwrap();
            stopped(&host, &ahp).await;
            ahp.client.shutdown().await;
            restarted.stop().await.unwrap();
        })
    })
    .await;
}

fn unused_port() -> u16 {
    std::net::TcpListener::bind(("127.0.0.1", 0))
        .unwrap()
        .local_addr()
        .unwrap()
        .port()
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn same_catalog_and_other_owner_rejected_disconnect_cleans_up_without_stopping_sdk() {
    run(|ctx| {
        Box::pin(async move {
            let port = unused_port();
            let owner = Client::start(options(ctx).with_transport(Transport::Tcp {
                port,
                connection_token: Some(RUNTIME_TOKEN.into()),
            }))
            .await
            .unwrap();
            let sdk = owner
                .create_session(ctx.approve_all_session_config())
                .await
                .unwrap();
            let other = Client::start(ClientOptions::new().with_transport(Transport::External {
                host: "127.0.0.1".into(),
                port,
                connection_token: Some(RUNTIME_TOKEN.into()),
            }))
            .await
            .unwrap();
            let (options, exits) = exit_observer();
            let host = other.start_ahp_host(options).await.unwrap();
            let ahp = connect(&host).await;
            let (uri, chat, subscription) = create(&ahp, ctx).await;
            turn(&ahp, &chat, subscription).await;
            topology(&host, &owner, &home(ctx).join("ahp/sessions"));
            let error = owner
                .start_ahp_host(AhpHostOptions::default())
                .await
                .err()
                .unwrap();
            assert!(
                error.to_string().contains("catalog already in use"),
                "{error}"
            );
            assert!(
                owner
                    .rpc()
                    .host()
                    .dispose(HostDisposeRequest {
                        host_id: host.host_id.clone()
                    })
                    .await
                    .is_err()
            );
            ahp.client.ping().await.unwrap();
            assert_eq!(other.pid(), None);
            // Abruptly close only the external owner's transport: no host.dispose,
            // runtime.shutdown, or owned child process is involved.
            other.force_stop();
            stopped(&host, &ahp).await;
            ahp.client.shutdown().await;
            sdk.get_events().await.unwrap();
            let replacement = owner
                .start_ahp_host(AhpHostOptions::default())
                .await
                .unwrap();
            let ahp = connect(&replacement).await;
            resume(&ahp, &uri, Some(sdk.id().as_str())).await;
            replacement.dispose().await.unwrap();
            stopped(&replacement, &ahp).await;
            ahp.client.shutdown().await;
            owner
                .create_session(ctx.approve_all_session_config())
                .await
                .unwrap();
            assert!(
                exits.lock().unwrap().is_empty(),
                "a lost connection cannot report a runtime exit"
            );
            owner.stop().await.unwrap();
        })
    })
    .await;
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn unexpected_exit_notifies_at_most_once_and_preserves_owner_session() {
    run(|ctx| {
        Box::pin(async move {
            let owner = start(ctx).await;
            let sdk = owner
                .create_session(ctx.approve_all_session_config())
                .await
                .unwrap();
            let (options, exits) = exit_observer();
            let host = owner.start_ahp_host(options).await.unwrap();
            let ahp = connect(&host).await;
            topology(&host, &owner, &home(ctx).join("ahp/sessions"));
            kill(&host);
            let observed = exit(&exits).await;
            assert_eq!(observed.host_id, host.host_id);
            assert_eq!(observed.reason, HostExitReason::Exited);
            stopped(&host, &ahp).await;
            sdk.get_events().await.unwrap();
            host.dispose().await.unwrap();
            host.dispose().await.unwrap();
            ahp.client.shutdown().await;
            owner.stop().await.unwrap();
            assert_eq!(exits.lock().unwrap().len(), 1);
        })
    })
    .await;
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn direct_rpc_concurrent_repeated_dispose_reaps_listener_and_notifies_once() {
    run(|ctx| {
        Box::pin(async move {
            let owner = start(ctx).await;
            let sdk = owner
                .create_session(ctx.approve_all_session_config())
                .await
                .unwrap();
            let (options, exits) = exit_observer();
            let host = owner.start_ahp_host(options).await.unwrap();
            let ahp = connect(&host).await;
            let request = HostDisposeRequest {
                host_id: host.host_id.clone(),
            };
            let rpc = owner.rpc().host();
            let (first, second) =
                tokio::join!(rpc.dispose(request.clone()), rpc.dispose(request.clone()));
            first.unwrap();
            second.unwrap();
            assert_eq!(exit(&exits).await.reason, HostExitReason::Disposed);
            stopped(&host, &ahp).await;
            rpc.dispose(request).await.unwrap();
            host.dispose().await.unwrap();
            rpc.dispose(HostDisposeRequest {
                host_id: uuid::Uuid::new_v4().to_string(),
            })
            .await
            .unwrap();
            sdk.get_events().await.unwrap();
            owner
                .create_session(ctx.approve_all_session_config())
                .await
                .unwrap();
            ahp.client.shutdown().await;
            owner.stop().await.unwrap();
            assert_eq!(exits.lock().unwrap().len(), 1);
        })
    })
    .await;
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn listener_defaults_explicit_ports_tokens_and_invalid_combinations() {
    run(|ctx| {
        Box::pin(async move {
            let owner = start(ctx).await;
            let host = owner
                .start_ahp_host(AhpHostOptions::default())
                .await
                .unwrap();
            let url = reqwest::Url::parse(&host.url).unwrap();
            assert_eq!(url.host_str(), Some("127.0.0.1"));
            assert!(url.port().unwrap() > 0);
            assert!(!host.token.as_ref().unwrap().is_empty());
            assert!(
                deadline(WebSocketTransport::connect(&host.url))
                    .await
                    .is_err()
            );
            let ahp = connect(&host).await;
            host.dispose().await.unwrap();
            stopped(&host, &ahp).await;
            ahp.client.shutdown().await;
            for (hostname, port) in [("0.0.0.0", unused_port()), ("localhost", 0), ("::1", 0)] {
                let options = AhpHostOptions::default()
                    .with_hostname(hostname)
                    .with_port(port.into())
                    .with_token("rust-supplied-token")
                    .with_require_connection_token(true);
                let host = owner.start_ahp_host(options).await.unwrap();
                let mut url = reqwest::Url::parse(&host.url).unwrap();
                match hostname {
                    "0.0.0.0" => assert_eq!(url.host_str(), Some("0.0.0.0")),
                    "::1" => assert_eq!(url.host_str(), Some("[::1]")),
                    _ => assert!(matches!(url.host_str(), Some("127.0.0.1" | "[::1]"))),
                }
                assert!(url.port().unwrap() > 0);
                if port != 0 {
                    assert_eq!(url.port(), Some(port));
                }
                assert_eq!(host.token.as_deref(), Some("rust-supplied-token"));
                if hostname == "0.0.0.0" {
                    url.set_host(Some("127.0.0.1")).unwrap();
                }
                assert!(
                    deadline(WebSocketTransport::connect(url.as_str()))
                        .await
                        .is_err()
                );
                let mut wrong_token = url.clone();
                wrong_token
                    .query_pairs_mut()
                    .append_pair("tkn", "wrong-token");
                assert!(
                    deadline(WebSocketTransport::connect(wrong_token.as_str()))
                        .await
                        .is_err()
                );
                let ahp = connect_url(url.as_str(), host.token.as_deref()).await;
                ahp.client.ping().await.unwrap();
                topology(&host, &owner, &home(ctx).join("ahp/sessions"));
                host.dispose().await.unwrap();
                stopped(&host, &ahp).await;
                ahp.client.shutdown().await;
            }
            for invalid in [
                json!({"token": ""}),
                json!({"token": "token", "requireConnectionToken": false}),
                json!({"port": -1}),
                json!({"port": 65536}),
                json!({"port": 1.5}),
                json!({"hostname": ""}),
            ] {
                let mut request = invalid;
                request["hostId"] = json!(uuid::Uuid::new_v4().to_string());
                // The raw RPC entry point also reaches runtime validation for
                // fractional ports, which the generated Rust integer rejects.
                assert!(owner.call("host.start", Some(request)).await.is_err());
            }
            let host = owner
                .start_ahp_host(AhpHostOptions::default())
                .await
                .unwrap();
            host.dispose().await.unwrap();
            owner.stop().await.unwrap();
        })
    })
    .await;
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn disabled_connection_token_still_requires_ahp_resource_authentication() {
    run(|ctx| {
        Box::pin(async move {
            let owner = start(ctx).await;
            let host = owner
                .start_ahp_host(AhpHostOptions::default().with_require_connection_token(false))
                .await
                .unwrap();
            assert_eq!(host.token, None);
            let ahp = connect(&host).await;
            ahp.client.ping().await.unwrap();
            let uri = format!("ahp-session:/{}", uuid::Uuid::new_v4());
            let unauthorized = ahp
                .client
                .request::<_, Value>("createSession", create_params(&ahp, ctx, &uri))
                .await;
            match unauthorized {
                Err(ahp::ClientError::Rpc(error)) => {
                    assert_eq!(
                        error.code,
                        ahp_types::errors::ahp_error_codes::AUTH_REQUIRED
                    );
                }
                other => panic!("expected AHP resource authentication requirement, got {other:?}"),
            }
            let (_, chat, subscription) = create(&ahp, ctx).await;
            turn(&ahp, &chat, subscription).await;
            host.dispose().await.unwrap();
            stopped(&host, &ahp).await;
            ahp.client.shutdown().await;
            owner.stop().await.unwrap();
        })
    })
    .await;
}

#[tokio::test]
#[ignore = "requires local runtime host artifacts"]
#[serial_test::serial]
async fn graceful_runtime_shutdown_closes_attached_ahp_session_and_reaps_child() {
    run(|ctx| {
        Box::pin(async move {
            let owner = start(ctx).await;
            let (options, exits) = exit_observer();
            let host = owner.start_ahp_host(options).await.unwrap();
            let ahp = connect(&host).await;
            create(&ahp, ctx).await;
            let runtime_pid = owner.pid().unwrap();
            topology(&host, &owner, &home(ctx).join("ahp/sessions"));
            deadline(owner.rpc().runtime().shutdown()).await.unwrap();
            let observed = exit(&exits).await;
            assert_eq!(observed.reason, HostExitReason::RuntimeShutdown);
            assert_eq!(observed.exit_code, Some(0));
            assert_eq!(observed.error, None);
            stopped(&host, &ahp).await;
            ahp.client.shutdown().await;
            owner.stop().await.unwrap();
            reaped(runtime_pid).await;
            tokio::time::sleep(Duration::from_millis(50)).await;
            assert_eq!(exits.lock().unwrap().len(), 1);
        })
    })
    .await;
}
