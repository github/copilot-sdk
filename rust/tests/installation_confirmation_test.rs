// Copyright (c) Microsoft Corporation. All rights reserved.

#![allow(clippy::unwrap_used)]

use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::extension_launch_provider::{
    ExtensionLaunchProvider, ExtensionLaunchProviderResolveRequest,
    ExtensionLaunchProviderResolveResult,
};
use github_copilot_sdk::installation_confirmation::{
    InstallationConfirmationContext, InstallationConfirmationHandler,
    InstallationConfirmationRequest, InstallationDecision, McpInstallationReview,
};
use github_copilot_sdk::{
    CliProgram, Client, ClientOptions, CopilotHttpRequest, CopilotHttpResponse,
    CopilotRequestContext, CopilotRequestError, CopilotRequestHandler, Error, ErrorKind, Result,
    Transport,
};
use serde_json::{Value, json};
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt, DuplexStream, duplex};
use tokio::net::TcpListener;
use tokio::sync::{Notify, mpsc, oneshot};
use tokio::time::timeout;

const WAIT: Duration = Duration::from_secs(2);

async fn write_frame(writer: &mut (impl AsyncWrite + Unpin), value: Value) {
    let body = serde_json::to_vec(&value).unwrap();
    writer
        .write_all(format!("Content-Length: {}\r\n\r\n", body.len()).as_bytes())
        .await
        .unwrap();
    writer.write_all(&body).await.unwrap();
    writer.flush().await.unwrap();
}

async fn read_frame(reader: &mut (impl AsyncRead + Unpin)) -> Value {
    timeout(WAIT, read_frame_untimed(reader)).await.unwrap()
}

async fn read_frame_untimed(reader: &mut (impl AsyncRead + Unpin)) -> Value {
    let mut header = Vec::new();
    while !header.ends_with(b"\r\n\r\n") {
        header.push(reader.read_u8().await.unwrap());
    }
    let header = String::from_utf8(header).unwrap();
    let length = header
        .trim()
        .strip_prefix("Content-Length: ")
        .unwrap()
        .parse()
        .unwrap();
    let mut body = vec![0; length];
    reader.read_exact(&mut body).await.unwrap();
    serde_json::from_slice(&body).unwrap()
}

fn request(operation: &str) -> Value {
    let mut value: Value =
        serde_json::from_str(include_str!("fixtures/installation_confirmation.json")).unwrap();
    value["operationId"] = json!(operation);
    value["confirmationId"] = json!(format!("challenge-{operation}"));
    value["reviewFingerprint"] = json!(format!("fingerprint-{operation}"));
    value
}

#[test]
fn confirmation_review_preserves_optional_unrecognised_trust() {
    for trust in [
        json!(42),
        json!({"schemaVersion": "v2"}),
        json!({"status": "future-status", "unknown": "x".repeat(4097)}),
    ] {
        let mut wire = request("a");
        wire["review"]["review"]["catalogueTrust"] = trust;
        let request: InstallationConfirmationRequest =
            serde_json::from_value(wire.clone()).unwrap();
        assert_eq!(serde_json::to_value(request).unwrap(), wire);
    }
}

async fn confirm(writer: &mut (impl AsyncWrite + Unpin), id: u64, params: Value) {
    write_frame(
        writer,
        json!({
            "jsonrpc": "2.0", "id": id, "method": "installations.confirm", "params": params
        }),
    )
    .await;
}

async fn cancel(writer: &mut (impl AsyncWrite + Unpin), id: Value) {
    write_frame(
        writer,
        json!({
            "jsonrpc": "2.0", "method": "$/cancelRequest", "params": { "id": id }
        }),
    )
    .await;
}

struct Review {
    request: InstallationConfirmationRequest,
    context: InstallationConfirmationContext,
    decision: oneshot::Sender<Result<InstallationDecision>>,
}

struct ControlledHandler(mpsc::UnboundedSender<Review>);

#[async_trait]
impl InstallationConfirmationHandler for ControlledHandler {
    async fn confirm(
        &self,
        request: InstallationConfirmationRequest,
        context: InstallationConfirmationContext,
    ) -> Result<InstallationDecision> {
        let (decision, receive) = oneshot::channel();
        self.0
            .send(Review {
                request,
                context,
                decision,
            })
            .unwrap_or_else(|_| panic!("review receiver closed"));
        receive.await.unwrap()
    }
}

struct Peer {
    client: Client,
    reader: DuplexStream,
    writer: DuplexStream,
    reviews: mpsc::UnboundedReceiver<Review>,
    _directory: tempfile::TempDir,
}

impl Peer {
    fn new(configured: bool, start_router: bool) -> Self {
        let (client_writer, reader) = duplex(32768);
        let (writer, client_reader) = duplex(32768);
        let directory = tempfile::tempdir().unwrap();
        let (send, reviews) = mpsc::unbounded_channel();
        let client = if configured {
            Client::from_streams_with_installation_confirmation_handler(
                client_reader,
                client_writer,
                directory.path().into(),
                Arc::new(ControlledHandler(send)),
            )
        } else {
            Client::from_streams(client_reader, client_writer, directory.path().into())
        }
        .unwrap();
        if start_router {
            client.start_router_for_test();
        }
        Self {
            client,
            reader,
            writer,
            reviews,
            _directory: directory,
        }
    }

    async fn review(&mut self) -> Review {
        timeout(WAIT, self.reviews.recv()).await.unwrap().unwrap()
    }
}

impl Drop for Peer {
    fn drop(&mut self) {
        self.client.force_stop();
    }
}

#[tokio::test]
async fn same_session_reviews_finish_out_of_order_without_blocking_other_rpcs() {
    let mut peer = Peer::new(true, true);
    confirm(&mut peer.writer, 101, request("a")).await;
    confirm(&mut peer.writer, 102, request("b")).await;
    let first = peer.review().await;
    let second = peer.review().await;
    let (a, b) = if first.request.operation_id == "a" {
        (first, second)
    } else {
        (second, first)
    };
    assert_eq!(
        a.request.policy_session_id.as_deref(),
        Some("original-session")
    );
    assert_eq!(a.request.policy_session_id, b.request.policy_session_id);
    assert_eq!(a.request.expires_at, "2026-09-24T03:00:00Z");
    let McpInstallationReview::Install(review) = &a.request.review.review else {
        panic!("expected typed install review");
    };
    assert_eq!(
        review.effective_configuration.as_ref().unwrap().headers["X-Region"],
        "eu"
    );
    assert_eq!(serde_json::to_value(&a.request).unwrap(), request("a"));

    write_frame(
        &mut peer.writer,
        json!({
            "jsonrpc": "2.0", "id": 103, "method": "gitHubToken.getToken",
            "params": {"registrationId": "unknown", "host": "github.com", "reason": "initial"}
        }),
    )
    .await;
    assert_eq!(read_frame(&mut peer.reader).await["id"], 103);

    b.decision.send(Ok(InstallationDecision::Decline)).unwrap();
    let response = read_frame(&mut peer.reader).await;
    assert_eq!(response["id"], 102);
    assert_eq!(
        response["result"],
        json!({
            "confirmationId": "challenge-b", "reviewFingerprint": "fingerprint-b", "decision": "decline"
        })
    );
    a.decision.send(Ok(InstallationDecision::Confirm)).unwrap();
    let response = read_frame(&mut peer.reader).await;
    assert_eq!(response["id"], 101);
    assert_eq!(response["result"]["confirmationId"], "challenge-a");
    assert_eq!(response["result"]["decision"], "confirm");
}

#[tokio::test]
async fn request_then_deadline_cancellation_before_router_start_never_opens_review() {
    let mut peer = Peer::new(true, false);
    let client = peer.client.clone();
    let barrier = tokio::spawn(async move { client.call("barrier", None).await });
    let outbound = read_frame(&mut peer.reader).await;
    confirm(&mut peer.writer, 201, request("expired")).await;
    cancel(&mut peer.writer, json!(201)).await;
    write_frame(
        &mut peer.writer,
        json!({
            "jsonrpc": "2.0", "id": outbound["id"], "result": {}
        }),
    )
    .await;
    timeout(WAIT, barrier).await.unwrap().unwrap().unwrap();
    peer.client.start_router_for_test();
    let response = read_frame(&mut peer.reader).await;
    assert_eq!(response["id"], 201);
    assert_eq!(response["error"]["code"], -32800);
    assert!(peer.reviews.try_recv().is_err());
}

#[tokio::test]
async fn cancelled_a_cannot_approve_or_cancel_successor_b_on_the_same_session() {
    let mut peer = Peer::new(true, true);
    confirm(&mut peer.writer, 301, request("a")).await;
    let a = peer.review().await;
    cancel(&mut peer.writer, json!(301)).await;
    assert_eq!(read_frame(&mut peer.reader).await["error"]["code"], -32800);
    assert!(a.context.request_cancelled().is_cancelled());
    assert!(!a.context.connection_closed().is_cancelled());
    assert!(a.decision.send(Ok(InstallationDecision::Confirm)).is_err());

    confirm(&mut peer.writer, 302, request("b")).await;
    let b = peer.review().await;
    cancel(&mut peer.writer, json!(301)).await;
    cancel(&mut peer.writer, json!(999)).await;
    cancel(&mut peer.writer, json!("302")).await;
    write_frame(
        &mut peer.writer,
        json!({
            "jsonrpc": "2.0", "id": 303, "method": "gitHubToken.getToken",
            "params": {"registrationId": "unknown", "host": "github.com", "reason": "initial"}
        }),
    )
    .await;
    assert_eq!(read_frame(&mut peer.reader).await["id"], 303);
    assert!(!b.context.request_cancelled().is_cancelled());
    b.decision.send(Ok(InstallationDecision::Confirm)).unwrap();
    let response = read_frame(&mut peer.reader).await;
    assert_eq!(response["id"], 302);
    assert_eq!(response["result"]["confirmationId"], "challenge-b");
}

#[tokio::test]
async fn peer_closure_is_distinct_and_does_not_retire_another_connection() {
    let mut a = Peer::new(true, true);
    let mut b = Peer::new(true, true);
    confirm(&mut a.writer, 401, request("a")).await;
    confirm(&mut b.writer, 401, request("b")).await;
    let review_a = a.review().await;
    let review_b = b.review().await;
    a.writer.shutdown().await.unwrap();
    timeout(WAIT, review_a.context.connection_closed().cancelled())
        .await
        .unwrap();
    assert!(!review_a.context.request_cancelled().is_cancelled());
    assert!(!review_b.context.connection_closed().is_cancelled());
    assert!(!review_b.context.request_cancelled().is_cancelled());
    review_b
        .decision
        .send(Ok(InstallationDecision::Cancel))
        .unwrap();
    assert_eq!(
        read_frame(&mut b.reader).await["result"]["decision"],
        "cancel"
    );
}

#[tokio::test]
async fn dropping_the_client_retires_pending_review_without_waiting_for_peer_close() {
    let (client_writer, _reader) = duplex(32768);
    let (mut writer, client_reader) = duplex(32768);
    let directory = tempfile::tempdir().unwrap();
    let (send, mut reviews) = mpsc::unbounded_channel();
    let client = Client::from_streams_with_installation_confirmation_handler(
        client_reader,
        client_writer,
        directory.path().into(),
        Arc::new(ControlledHandler(send)),
    )
    .unwrap();
    client.start_router_for_test();
    confirm(&mut writer, 450, request("a")).await;
    let review = timeout(WAIT, reviews.recv()).await.unwrap().unwrap();
    drop(client);
    timeout(WAIT, review.context.connection_closed().cancelled())
        .await
        .unwrap();
    assert!(!review.context.request_cancelled().is_cancelled());
}

#[tokio::test]
async fn missing_handler_and_invalid_review_return_errors_without_user_invocation() {
    let mut missing = Peer::new(false, true);
    confirm(&mut missing.writer, 501, request("a")).await;
    assert_eq!(
        read_frame(&mut missing.reader).await["error"]["code"],
        -32601
    );

    let mut peer = Peer::new(true, true);
    let mut unknown_resource = request("a");
    unknown_resource["review"]["resource"] = json!("future-resource");
    let mut unknown_action = request("a");
    unknown_action["review"]["review"]["action"] = json!("future-action");
    let mut unknown_choice = request("a");
    unknown_choice["review"]["review"]["selectedChoice"]["installMethod"] = json!("future");
    let mut missing_url = request("a");
    missing_url["review"]["review"]["effectiveConfiguration"]
        .as_object_mut()
        .unwrap()
        .remove("url");
    for (index, params) in [
        Value::Null,
        unknown_resource,
        unknown_action,
        unknown_choice,
        missing_url,
    ]
    .into_iter()
    .enumerate()
    {
        confirm(&mut peer.writer, 510 + index as u64, params).await;
        assert_eq!(read_frame(&mut peer.reader).await["error"]["code"], -32602);
    }
    assert!(peer.reviews.try_recv().is_err());
}

#[tokio::test]
async fn legacy_optional_fields_are_not_replaced_by_inferred_context() {
    let mut peer = Peer::new(true, true);
    let mut params = request("legacy");
    params.as_object_mut().unwrap().remove("policySessionId");
    params["review"]["review"]
        .as_object_mut()
        .unwrap()
        .remove("effectiveConfiguration");
    confirm(&mut peer.writer, 601, params).await;
    let review = peer.review().await;
    assert!(review.request.policy_session_id.is_none());
    let McpInstallationReview::Install(install) = review.request.review.review else {
        panic!("expected install");
    };
    assert!(install.effective_configuration.is_none());
    review
        .decision
        .send(Ok(InstallationDecision::Decline))
        .unwrap();
    assert_eq!(
        read_frame(&mut peer.reader).await["result"]["decision"],
        "decline"
    );
}

#[tokio::test]
async fn handler_errors_and_unknown_decisions_never_become_approval() {
    let mut peer = Peer::new(true, true);
    for (id, decision) in [
        (
            701,
            Err(Error::with_message(
                ErrorKind::InvalidConfig,
                "review unavailable",
            )),
        ),
        (702, Ok(InstallationDecision::Unknown)),
    ] {
        confirm(&mut peer.writer, id, request("a")).await;
        peer.review().await.decision.send(decision).unwrap();
        let response = read_frame(&mut peer.reader).await;
        assert_eq!(response["error"]["code"], -32603);
        assert!(response.get("result").is_none());
    }
}

#[tokio::test]
async fn real_client_start_installs_global_receiver_without_registration_or_session_rpc() {
    let listener = TcpListener::bind(("127.0.0.1", 0)).await.unwrap();
    let port = listener.local_addr().unwrap().port();
    let (send, mut reviews) = mpsc::unbounded_channel();
    let server = tokio::spawn(async move {
        let (stream, _) = listener.accept().await.unwrap();
        let (mut reader, mut writer) = stream.into_split();
        let connect = read_frame(&mut reader).await;
        assert_eq!(connect["method"], "connect");
        write_frame(
            &mut writer,
            json!({
                "jsonrpc": "2.0", "id": connect["id"],
                "result": { "ok": true, "protocolVersion": 3, "version": "test" }
            }),
        )
        .await;
        confirm(&mut writer, 801, request("startup")).await;
        read_frame(&mut reader).await
    });
    let client = Client::start(
        ClientOptions::new()
            .with_program(CliProgram::Path("unused-external-transport".into()))
            .with_transport(Transport::External {
                host: "127.0.0.1".to_string(),
                port,
                connection_token: None,
            })
            .with_installation_confirmation_handler(ControlledHandler(send)),
    )
    .await
    .unwrap();
    let review = timeout(WAIT, reviews.recv()).await.unwrap().unwrap();
    review
        .decision
        .send(Ok(InstallationDecision::Confirm))
        .unwrap();
    let response = timeout(WAIT, server).await.unwrap().unwrap();
    assert_eq!(response["id"], 801);
    assert_eq!(response["result"]["decision"], "confirm");
    client.force_stop();
}

/// Never returns until released, standing in for a hung host callback.
struct BlockedLaunchProvider {
    entered: mpsc::UnboundedSender<()>,
    release: Arc<Notify>,
}

#[async_trait]
impl ExtensionLaunchProvider for BlockedLaunchProvider {
    async fn resolve(
        &self,
        _request: ExtensionLaunchProviderResolveRequest,
    ) -> Result<ExtensionLaunchProviderResolveResult> {
        self.entered.send(()).unwrap();
        self.release.notified().await;
        Ok(ExtensionLaunchProviderResolveResult { launch: None })
    }
}

#[tokio::test]
async fn blocked_global_callback_does_not_delay_confirmation_or_its_cancellation() {
    let listener = TcpListener::bind(("127.0.0.1", 0)).await.unwrap();
    let port = listener.local_addr().unwrap().port();
    let (send, mut reviews) = mpsc::unbounded_channel();
    let (entered, mut resolve_entered) = mpsc::unbounded_channel();
    let release = Arc::new(Notify::new());
    let (to_server, mut server_commands) = mpsc::unbounded_channel::<Value>();
    let (from_server, mut server_frames) = mpsc::unbounded_channel::<Value>();
    let server = tokio::spawn(async move {
        let (stream, _) = listener.accept().await.unwrap();
        let (mut reader, mut writer) = stream.into_split();
        let connect = read_frame(&mut reader).await;
        assert_eq!(connect["method"], "connect");
        write_frame(
            &mut writer,
            json!({
                "jsonrpc": "2.0", "id": connect["id"],
                "result": { "ok": true, "protocolVersion": 3, "version": "test" }
            }),
        )
        .await;
        let register = read_frame(&mut reader).await;
        assert_eq!(register["method"], "registerExtensionLaunchProvider");
        write_frame(
            &mut writer,
            json!({ "jsonrpc": "2.0", "id": register["id"], "result": {} }),
        )
        .await;
        let forward = tokio::spawn(async move {
            loop {
                let frame = read_frame_untimed(&mut reader).await;
                if from_server.send(frame).is_err() {
                    break;
                }
            }
        });
        while let Some(frame) = server_commands.recv().await {
            write_frame(&mut writer, frame).await;
        }
        forward.abort();
    });
    let client = Client::start(
        ClientOptions::new()
            .with_program(CliProgram::Path("unused-external-transport".into()))
            .with_transport(Transport::External {
                host: "127.0.0.1".to_string(),
                port,
                connection_token: None,
            })
            .with_extension_launch_provider(BlockedLaunchProvider {
                entered,
                release: release.clone(),
            })
            .with_installation_confirmation_handler(ControlledHandler(send)),
    )
    .await
    .unwrap();

    to_server
        .send(json!({
            "jsonrpc": "2.0", "id": 901, "method": "extensionLaunchProvider.resolve",
            "params": {
                "id": "project:blocked", "modulePath": "/extensions/blocked/index.js",
                "name": "Blocked", "source": "project"
            }
        }))
        .unwrap();
    timeout(WAIT, resolve_entered.recv())
        .await
        .unwrap()
        .unwrap();
    to_server
        .send(json!({
            "jsonrpc": "2.0", "id": 902, "method": "installations.confirm",
            "params": request("behind-blocked-callback")
        }))
        .unwrap();
    let review = timeout(WAIT, reviews.recv()).await.unwrap().unwrap();
    assert_eq!(review.request.operation_id, "behind-blocked-callback");

    to_server
        .send(json!({
            "jsonrpc": "2.0", "method": "$/cancelRequest", "params": { "id": 902 }
        }))
        .unwrap();
    timeout(WAIT, review.context.request_cancelled().cancelled())
        .await
        .unwrap();
    let response = timeout(WAIT, server_frames.recv()).await.unwrap().unwrap();
    assert_eq!(response["id"], 902);
    assert_eq!(response["error"]["code"], -32800);
    assert!(
        review
            .decision
            .send(Ok(InstallationDecision::Confirm))
            .is_err()
    );

    // The blocked callback still completes in order, and 902 is never approved late.
    release.notify_one();
    let response = timeout(WAIT, server_frames.recv()).await.unwrap().unwrap();
    assert_eq!(response["id"], 901);
    assert_eq!(response["result"], json!({}));
    drop(to_server);
    timeout(WAIT, server).await.unwrap().unwrap();
    client.force_stop();
}

/// A host inference handler that never completes, so any await on it would stall routing.
struct HungRequestHandler(mpsc::UnboundedSender<()>);

#[async_trait]
impl CopilotRequestHandler for HungRequestHandler {
    async fn send_request(
        &self,
        _request: CopilotHttpRequest,
        _context: &CopilotRequestContext,
    ) -> std::result::Result<CopilotHttpResponse, CopilotRequestError> {
        self.0.send(()).unwrap();
        std::future::pending().await
    }
}

#[tokio::test]
async fn hung_llm_inference_request_does_not_delay_confirmation_or_its_cancellation() {
    let listener = TcpListener::bind(("127.0.0.1", 0)).await.unwrap();
    let port = listener.local_addr().unwrap().port();
    let (send, mut reviews) = mpsc::unbounded_channel();
    let (entered, mut handler_entered) = mpsc::unbounded_channel();
    let (to_server, mut server_commands) = mpsc::unbounded_channel::<Value>();
    let (from_server, mut server_frames) = mpsc::unbounded_channel::<Value>();
    let server = tokio::spawn(async move {
        let (stream, _) = listener.accept().await.unwrap();
        let (mut reader, mut writer) = stream.into_split();
        for method in ["connect", "llmInference.setProvider"] {
            let message = read_frame(&mut reader).await;
            assert_eq!(message["method"], method);
            let result = if method == "connect" {
                json!({ "ok": true, "protocolVersion": 3, "version": "test" })
            } else {
                json!({ "success": true })
            };
            write_frame(
                &mut writer,
                json!({ "jsonrpc": "2.0", "id": message["id"], "result": result }),
            )
            .await;
        }
        let forward = tokio::spawn(async move {
            loop {
                let frame = read_frame_untimed(&mut reader).await;
                if from_server.send(frame).is_err() {
                    break;
                }
            }
        });
        while let Some(frame) = server_commands.recv().await {
            write_frame(&mut writer, frame).await;
        }
        forward.abort();
    });
    let client = Client::start(
        ClientOptions::new()
            .with_program(CliProgram::Path("unused-external-transport".into()))
            .with_transport(Transport::External {
                host: "127.0.0.1".to_string(),
                port,
                connection_token: None,
            })
            .with_request_handler(HungRequestHandler(entered))
            .with_installation_confirmation_handler(ControlledHandler(send)),
    )
    .await
    .unwrap();

    for (id, method, params) in [
        (
            911,
            "llmInference.httpRequestStart",
            json!({ "requestId": "hung", "method": "GET", "url": "https://example.test/", "headers": {} }),
        ),
        (
            912,
            "llmInference.httpRequestChunk",
            json!({ "requestId": "hung", "data": "", "end": true }),
        ),
    ] {
        to_server
            .send(json!({ "jsonrpc": "2.0", "id": id, "method": method, "params": params }))
            .unwrap();
        let ack = timeout(WAIT, server_frames.recv()).await.unwrap().unwrap();
        assert_eq!(ack["id"], id);
    }
    timeout(WAIT, handler_entered.recv())
        .await
        .unwrap()
        .unwrap();

    to_server
        .send(json!({
            "jsonrpc": "2.0", "id": 913, "method": "installations.confirm",
            "params": request("behind-hung-inference")
        }))
        .unwrap();
    let review = timeout(WAIT, reviews.recv()).await.unwrap().unwrap();
    assert_eq!(review.request.operation_id, "behind-hung-inference");
    to_server
        .send(json!({
            "jsonrpc": "2.0", "method": "$/cancelRequest", "params": { "id": 913 }
        }))
        .unwrap();
    timeout(WAIT, review.context.request_cancelled().cancelled())
        .await
        .unwrap();
    let response = timeout(WAIT, server_frames.recv()).await.unwrap().unwrap();
    assert_eq!(response["id"], 913);
    assert_eq!(response["error"]["code"], -32800);
    assert!(
        review
            .decision
            .send(Ok(InstallationDecision::Confirm))
            .is_err()
    );

    drop(to_server);
    timeout(WAIT, server).await.unwrap().unwrap();
    client.force_stop();
}
