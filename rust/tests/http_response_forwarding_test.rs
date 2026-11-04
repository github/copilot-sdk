#![allow(clippy::unwrap_used)]

use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use base64::Engine;
use bytes::Bytes;
use futures_util::{SinkExt, StreamExt, stream};
use github_copilot_sdk::{
    Client, ClientOptions, CopilotHttpRequest, CopilotHttpResponse, CopilotHttpResponseBody,
    CopilotRequestContext, CopilotRequestError, CopilotRequestHandler, CopilotWebSocketForwarder,
    CopilotWebSocketHandler, CopilotWebSocketMessage, CopilotWebSocketResponse,
    SDK_PROTOCOL_VERSION, Transport,
};
use http::HeaderMap;
use parking_lot::Mutex;
use serde_json::{Value, json};
use tokio::io::{AsyncBufReadExt, AsyncReadExt, AsyncWriteExt, BufReader};
use tokio::net::TcpListener;
use tokio::net::tcp::{OwnedReadHalf, OwnedWriteHalf};
use tokio::sync::mpsc;
use tokio::time::timeout;
use tokio_stream::wrappers::ReceiverStream;

const DEADLINE: Duration = Duration::from_secs(5);
const CHUNK_SIZE: usize = 32 * 1024;

async fn read_frame(reader: &mut BufReader<OwnedReadHalf>) -> Value {
    let mut length = None;
    loop {
        let mut line = String::new();
        assert_ne!(reader.read_line(&mut line).await.unwrap(), 0);
        if line == "\r\n" {
            break;
        }
        if let Some(value) = line.strip_prefix("Content-Length:") {
            length = Some(value.trim().parse::<usize>().unwrap());
        }
    }
    let mut body = vec![0; length.unwrap()];
    reader.read_exact(&mut body).await.unwrap();
    serde_json::from_slice(&body).unwrap()
}

async fn write_frame(writer: &mut OwnedWriteHalf, value: Value) {
    let body = serde_json::to_vec(&value).unwrap();
    let mut frame = format!("Content-Length: {}\r\n\r\n", body.len()).into_bytes();
    frame.extend_from_slice(&body);
    writer.write_all(&frame).await.unwrap();
}

/// A framed runtime peer exercising the public SDK client and real TCP transport.
struct Peer {
    client: Client,
    read: BufReader<OwnedReadHalf>,
    write: OwnedWriteHalf,
    _work: tempfile::TempDir,
}

impl Peer {
    async fn start(handler: impl CopilotRequestHandler) -> Self {
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let work = tempfile::tempdir().unwrap();
        let options = ClientOptions::new()
            .with_program(std::env::current_exe().unwrap())
            .with_cwd(work.path())
            .with_transport(Transport::External {
                host: "127.0.0.1".into(),
                port: listener.local_addr().unwrap().port(),
                connection_token: None,
            })
            .with_request_handler(handler);
        let (client, (read, write)) = timeout(DEADLINE, async {
            tokio::join!(Client::start(options), async {
                let (socket, _) = listener.accept().await.unwrap();
                socket.set_nodelay(true).unwrap();
                let (read, mut write) = socket.into_split();
                let mut read = BufReader::new(read);
                for method in ["connect", "llmInference.setProvider"] {
                    let message = read_frame(&mut read).await;
                    assert_eq!(message["method"], method);
                    let result = if method == "connect" {
                        json!({"ok": true, "version": "test", "protocolVersion": SDK_PROTOCOL_VERSION})
                    } else {
                        json!({"success": true})
                    };
                    write_frame(
                        &mut write,
                        json!({"jsonrpc": "2.0", "id": message["id"], "result": result}),
                    )
                    .await;
                }
                (read, write)
            })
        })
        .await
        .unwrap();
        Self {
            client: client.unwrap(),
            read,
            write,
            _work: work,
        }
    }

    async fn request(&mut self, url: &str) -> Value {
        self.begin_request(url).await;
        let head = self.next().await;
        assert_eq!(head["method"], "llmInference.httpResponseStart");
        self.ack(&head).await;
        head
    }

    async fn begin_request(&mut self, url: &str) {
        write_frame(
            &mut self.write,
            json!({
                "jsonrpc": "2.0", "id": 100000,
                "method": "llmInference.httpRequestStart",
                "params": {"requestId": "test", "method": "GET", "url": url, "headers": {}}
            }),
        )
        .await;
        self.request_chunk(json!({"requestId": "test", "data": "", "end": true}))
            .await;
    }

    async fn request_chunk(&mut self, params: Value) {
        write_frame(
            &mut self.write,
            json!({
                "jsonrpc": "2.0", "id": 100001,
                "method": "llmInference.httpRequestChunk", "params": params
            }),
        )
        .await;
    }

    async fn next(&mut self) -> Value {
        timeout(DEADLINE, async {
            loop {
                let value = read_frame(&mut self.read).await;
                if value.get("method").is_some() {
                    return value;
                }
                assert!(value.get("error").is_none(), "{value}");
            }
        })
        .await
        .expect("runtime response")
    }

    async fn ack(&mut self, message: &Value) {
        write_frame(
            &mut self.write,
            json!({"jsonrpc": "2.0", "id": message["id"], "result": {"accepted": true}}),
        )
        .await;
    }

    async fn stop(self) {
        self.client.stop().await.unwrap();
    }
}

struct BodyHandler(Mutex<Option<CopilotHttpResponseBody>>);

#[async_trait]
impl CopilotRequestHandler for BodyHandler {
    async fn send_request(
        &self,
        _request: CopilotHttpRequest,
        _context: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        Ok(CopilotHttpResponse::new(
            200,
            None,
            HeaderMap::new(),
            self.0.lock().take().unwrap(),
        ))
    }
}

async fn channel_peer() -> (Peer, mpsc::Sender<Result<Bytes, CopilotRequestError>>) {
    let (tx, rx) = mpsc::channel(1);
    let body = Box::pin(ReceiverStream::new(rx));
    let mut peer = Peer::start(BodyHandler(Mutex::new(Some(body)))).await;
    peer.request("http://unused.test").await;
    (peer, tx)
}

fn data(message: &Value) -> Vec<u8> {
    assert_eq!(message["method"], "llmInference.httpResponseChunk");
    assert_eq!(message["params"]["end"], false);
    assert_eq!(message["params"]["binary"], true);
    let decoded = base64::engine::general_purpose::STANDARD
        .decode(message["params"]["data"].as_str().unwrap())
        .unwrap();
    assert!(!decoded.is_empty() && decoded.len() <= CHUNK_SIZE);
    decoded
}

struct MetadataHandler {
    contexts: mpsc::UnboundedSender<CopilotRequestContext>,
    messages: mpsc::UnboundedSender<(CopilotWebSocketMessage, Option<CopilotRequestContext>)>,
    legacy: bool,
}

#[async_trait]
impl CopilotRequestHandler for MetadataHandler {
    async fn send_request(
        &self,
        request: CopilotHttpRequest,
        context: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        assert_eq!(request.body, b"request-body");
        self.contexts.send(context.clone()).unwrap();
        Ok(CopilotHttpResponse::new(
            200,
            None,
            HeaderMap::new(),
            Box::pin(stream::empty()),
        ))
    }

    async fn open_websocket(
        &self,
        context: &CopilotRequestContext,
        _response: CopilotWebSocketResponse,
    ) -> Result<Box<dyn CopilotWebSocketHandler>, CopilotRequestError> {
        self.contexts.send(context.clone()).unwrap();
        let socket = LegacyMetadataSocket(self.messages.clone());
        if self.legacy {
            Ok(Box::new(socket))
        } else {
            Ok(Box::new(ContextMetadataSocket(socket)))
        }
    }
}

struct LegacyMetadataSocket(
    mpsc::UnboundedSender<(CopilotWebSocketMessage, Option<CopilotRequestContext>)>,
);

#[async_trait]
impl CopilotWebSocketHandler for LegacyMetadataSocket {
    async fn send_request_message(
        &self,
        message: CopilotWebSocketMessage,
    ) -> Result<(), CopilotRequestError> {
        self.0.send((message, None)).unwrap();
        Ok(())
    }

    async fn close(&self) -> Result<(), CopilotRequestError> {
        Ok(())
    }
}

struct ContextMetadataSocket(LegacyMetadataSocket);

#[async_trait]
impl CopilotWebSocketHandler for ContextMetadataSocket {
    async fn send_request_message(
        &self,
        message: CopilotWebSocketMessage,
    ) -> Result<(), CopilotRequestError> {
        self.0.send_request_message(message).await
    }

    async fn send_request_message_with_context(
        &self,
        message: CopilotWebSocketMessage,
        context: &CopilotRequestContext,
    ) -> Result<(), CopilotRequestError> {
        self.0.0.send((message, Some(context.clone()))).unwrap();
        Ok(())
    }

    async fn close(&self) -> Result<(), CopilotRequestError> {
        Ok(())
    }
}

async fn begin_metadata_request(
    peer: &mut Peer,
    request_id: &str,
    invocation_id: Option<&str>,
    websocket: bool,
) {
    let mut params = json!({
        "requestId": request_id, "method": if websocket { "GET" } else { "POST" },
        "url": "http://unused.test", "headers": {"x-multiple": ["one", "two"]},
        "sessionId": "session", "agentId": "trajectory", "parentAgentId": "parent",
        "interactionType": "conversation-agent", "transport": if websocket { "websocket" } else { "http" },
    });
    if let Some(id) = invocation_id {
        params["agentInvocationId"] = json!(id);
    }
    write_frame(&mut peer.write, json!({
        "jsonrpc": "2.0", "id": 100000, "method": "llmInference.httpRequestStart", "params": params
    })).await;
}

#[tokio::test]
async fn invocation_metadata_http_survives_physical_retries() {
    for invocation_id in [Some("logical-invocation"), None, Some("")] {
        let (contexts, mut received) = mpsc::unbounded_channel();
        let (messages, _received_messages) = mpsc::unbounded_channel();
        let mut peer = Peer::start(Arc::new(MetadataHandler {
            contexts,
            messages,
            legacy: false,
        }))
        .await;
        for request_id in ["physical-first", "physical-retry"] {
            begin_metadata_request(&mut peer, request_id, invocation_id, false).await;
            peer.request_chunk(json!({
                "requestId": request_id, "data": base64::engine::general_purpose::STANDARD.encode(b"request-body"),
                "binary": true, "end": true,
            })).await;
            let head = peer.next().await;
            assert_eq!(head["params"]["status"], 200);
            peer.ack(&head).await;
            let end = peer.next().await;
            assert_eq!(end["params"]["end"], true);
            assert!(end["params"].get("error").is_none(), "{end}");
            peer.ack(&end).await;
            let ctx = timeout(DEADLINE, received.recv()).await.unwrap().unwrap();
            assert_eq!(ctx.agent_invocation_id.as_deref(), invocation_id);
            assert_eq!(ctx.clone().agent_invocation_id, ctx.agent_invocation_id);
            assert_eq!(ctx.request_id, request_id);
            assert_eq!(ctx.session_id.as_deref(), Some("session"));
            assert_eq!(ctx.agent_id.as_deref(), Some("trajectory"));
            assert_eq!(ctx.parent_agent_id.as_deref(), Some("parent"));
            assert_eq!(ctx.interaction_type.as_deref(), Some("conversation-agent"));
            assert_eq!(
                ctx.headers
                    .get_all("x-multiple")
                    .iter()
                    .map(|v| v.to_str().unwrap())
                    .collect::<Vec<_>>(),
                ["one", "two"]
            );
        }
        peer.stop().await;
    }
}

#[tokio::test]
async fn invocation_metadata_websocket_is_per_message_and_legacy_hooks_still_run() {
    for legacy in [false, true] {
        let (contexts, mut received_contexts) = mpsc::unbounded_channel();
        let (messages, mut received_messages) = mpsc::unbounded_channel();
        let mut peer = Peer::start(MetadataHandler {
            contexts,
            messages,
            legacy,
        })
        .await;
        // A frame racing ahead of start must retain its own invocation metadata.
        peer.request_chunk(json!({
            "requestId": "reused-socket", "data": "queued", "agentInvocationId": "queued-invocation"
        }))
        .await;
        begin_metadata_request(&mut peer, "reused-socket", Some("opening-invocation"), true).await;
        let head = peer.next().await;
        assert_eq!(head["params"]["status"], 101);
        peer.ack(&head).await;
        let connection = timeout(DEADLINE, received_contexts.recv())
            .await
            .unwrap()
            .unwrap();
        assert_eq!(
            connection.agent_invocation_id.as_deref(),
            Some("opening-invocation")
        );
        let (queued, ctx) = timeout(DEADLINE, received_messages.recv())
            .await
            .unwrap()
            .unwrap();
        assert_eq!(queued.data, b"queued");
        if !legacy {
            assert_eq!(
                ctx.unwrap().agent_invocation_id.as_deref(),
                Some("queued-invocation")
            );
        }

        for id in [
            Some("later-invocation"),
            Some("later-invocation"),
            None,
            Some(""),
        ] {
            let mut params = json!({"requestId": "reused-socket", "data": "message"});
            if let Some(id) = id {
                params["agentInvocationId"] = json!(id);
            }
            peer.request_chunk(params).await;
            let (message, ctx) = timeout(DEADLINE, received_messages.recv())
                .await
                .unwrap()
                .unwrap();
            assert_eq!(message.data, b"message");
            assert!(!message.binary);
            if legacy {
                assert!(ctx.is_none());
            } else {
                let ctx = ctx.unwrap();
                assert_eq!(ctx.agent_invocation_id.as_deref(), id);
                assert_eq!(ctx.request_id, connection.request_id);
                assert_eq!(ctx.session_id, connection.session_id);
                assert_eq!(ctx.agent_id, connection.agent_id);
            }
        }
        assert_eq!(
            connection.agent_invocation_id.as_deref(),
            Some("opening-invocation")
        );
        peer.request_chunk(json!({"requestId": "reused-socket", "data": "", "end": true}))
            .await;
        let end = peer.next().await;
        assert_eq!(end["params"]["end"], true);
        assert!(end["params"].get("error").is_none(), "{end}");
        peer.ack(&end).await;
        peer.stop().await;
    }
}

struct ForwardingMetadataHandler {
    url: String,
    transform: bool,
    contexts: mpsc::UnboundedSender<CopilotRequestContext>,
}

#[async_trait]
impl CopilotRequestHandler for ForwardingMetadataHandler {
    async fn open_websocket(
        &self,
        _context: &CopilotRequestContext,
        response: CopilotWebSocketResponse,
    ) -> Result<Box<dyn CopilotWebSocketHandler>, CopilotRequestError> {
        let mut builder = CopilotWebSocketForwarder::builder(self.url.clone(), HeaderMap::new());
        if self.transform {
            let contexts = self.contexts.clone();
            builder = builder
                .on_send_request_message_with_context(Arc::new(move |mut message, ctx| {
                    contexts.send(ctx.clone()).unwrap();
                    if message.data == b"drop" {
                        return None;
                    }
                    message.data.extend_from_slice(
                        format!(":{:?}", ctx.agent_invocation_id.as_deref()).as_bytes(),
                    );
                    Some(message)
                }))
                .on_send_request_message(Arc::new(|mut message| {
                    message.data.extend_from_slice(b":legacy");
                    Some(message)
                }));
        }
        Ok(Box::new(builder.connect(response).await?))
    }
}

#[tokio::test]
async fn invocation_metadata_forwarder_transforms_and_pass_through_preserve_messages() {
    for transform in [true, false] {
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let url = format!("ws://{}", listener.local_addr().unwrap());
        let upstream = tokio::spawn(async move {
            let (socket, _) = listener.accept().await.unwrap();
            let mut socket = tokio_tungstenite::accept_async(socket).await.unwrap();
            let mut messages = Vec::new();
            while let Some(message) = socket.next().await {
                match message.unwrap() {
                    tokio_tungstenite::tungstenite::Message::Text(text) => {
                        messages.push(text.to_string());
                    }
                    tokio_tungstenite::tungstenite::Message::Close(_) => {
                        socket.flush().await.unwrap();
                        return messages;
                    }
                    other => panic!("unexpected upstream message: {other:?}"),
                }
            }
            panic!("upstream ended without close")
        });
        let (contexts, mut received_contexts) = mpsc::unbounded_channel();
        let mut peer = Peer::start(ForwardingMetadataHandler {
            url,
            transform,
            contexts,
        })
        .await;
        begin_metadata_request(&mut peer, "forwarded-socket", Some("opening"), true).await;
        let head = peer.next().await;
        assert_eq!(head["params"]["status"], 101);
        peer.ack(&head).await;

        let mut expected = Vec::new();
        for (text, id) in [
            ("first", Some("later")),
            ("drop", Some("dropped")),
            ("missing", None),
            ("empty", Some("")),
        ] {
            let mut params = json!({"requestId": "forwarded-socket", "data": text});
            if let Some(id) = id {
                params["agentInvocationId"] = json!(id);
            }
            peer.request_chunk(params).await;
            if transform {
                let ctx = timeout(DEADLINE, received_contexts.recv())
                    .await
                    .unwrap()
                    .unwrap();
                assert_eq!(ctx.agent_invocation_id.as_deref(), id);
                if text != "drop" {
                    expected.push(format!("{text}:{id:?}:legacy"));
                }
            } else {
                expected.push(text.to_string());
            }
        }
        peer.request_chunk(json!({"requestId": "forwarded-socket", "data": "", "end": true}))
            .await;
        let end = peer.next().await;
        assert_eq!(end["params"]["end"], true);
        assert!(end["params"].get("error").is_none(), "{end}");
        peer.ack(&end).await;
        assert_eq!(
            timeout(DEADLINE, upstream).await.unwrap().unwrap(),
            expected
        );
        peer.stop().await;
    }
}

#[tokio::test]
async fn reads_ahead_under_one_pinned_ack_with_bounded_backpressure() {
    let (mut peer, tx) = channel_peer().await;
    tx.send(Ok(Bytes::from_static(b"first"))).await.unwrap();
    let first = peer.next().await;
    assert_eq!(data(&first), b"first");

    timeout(DEADLINE, async {
        for _ in 0..32 {
            tx.send(Ok(Bytes::from(vec![b'x'; 1024]))).await.unwrap();
        }
        // A permit proves the last queued fragment was consumed without an ACK.
        drop(tx.reserve().await.unwrap());
    })
    .await
    .expect("read-ahead while first ACK is outstanding");
    tx.send(Ok(Bytes::from_static(b"last"))).await.unwrap();
    assert!(
        timeout(Duration::from_millis(20), tx.reserve())
            .await
            .is_err(),
        "full read-ahead buffer must stop polling the source"
    );
    assert!(
        timeout(Duration::from_millis(20), peer.next())
            .await
            .is_err(),
        "must not duplicate or overtake the outstanding write"
    );
    peer.ack(&first).await;
    let combined = peer.next().await;
    assert_eq!(data(&combined), vec![b'x'; CHUNK_SIZE]);
    assert_ne!(combined["id"], first["id"]);
    drop(tx);
    peer.ack(&combined).await;
    let last = peer.next().await;
    assert_eq!(data(&last), b"last");
    peer.ack(&last).await;
    let end = peer.next().await;
    assert_eq!(end["params"]["end"], true);
    assert!(end["params"].get("error").is_none());
    peer.ack(&end).await;
    peer.stop().await;
}

#[tokio::test]
async fn sparse_bytes_flush_before_future_input_and_preserve_split_utf8() {
    let (mut peer, tx) = channel_peer().await;
    let mut received = Vec::new();
    // Both the UTF-8 code point and SSE delimiter cross source/write boundaries.
    for byte in b"data: \xf0\x9f\x8c\x8d\n\n" {
        tx.send(Ok(Bytes::copy_from_slice(&[*byte]))).await.unwrap();
        let message = peer.next().await;
        received.extend(data(&message));
        peer.ack(&message).await;
    }
    assert_eq!(received, b"data: \xf0\x9f\x8c\x8d\n\n");
    drop(tx);
    let end = peer.next().await;
    assert_eq!(end["params"]["end"], true);
    peer.ack(&end).await;
    peer.stop().await;
}

#[tokio::test]
async fn upstream_error_follows_buffered_partial_and_outstanding_ack() {
    let (mut peer, tx) = channel_peer().await;
    tx.send(Ok(Bytes::from_static(b"first"))).await.unwrap();
    let first = peer.next().await;
    tx.send(Ok(Bytes::from_static(b"partial"))).await.unwrap();
    tx.send(Err(CopilotRequestError::message("upstream failed")))
        .await
        .unwrap();
    timeout(DEADLINE, tx.closed()).await.unwrap();
    assert!(
        timeout(Duration::from_millis(20), peer.next())
            .await
            .is_err()
    );
    peer.ack(&first).await;
    let partial = peer.next().await;
    assert_eq!(data(&partial), b"partial");
    assert!(
        timeout(Duration::from_millis(20), peer.next())
            .await
            .is_err()
    );
    peer.ack(&partial).await;
    let end = peer.next().await;
    assert_eq!(end["params"]["end"], true);
    assert_eq!(end["params"]["error"]["message"], "upstream failed");
    peer.ack(&end).await;
    peer.stop().await;
}

#[tokio::test]
async fn cancellation_drops_source_without_waiting_for_data_ack() {
    let (mut peer, tx) = channel_peer().await;
    tx.send(Ok(Bytes::from_static(b"first"))).await.unwrap();
    let first = peer.next().await;
    assert_eq!(data(&first), b"first");
    peer.request_chunk(json!({"requestId": "test", "data": "", "cancel": true}))
        .await;
    let end = peer.next().await;
    assert_eq!(end["params"]["end"], true);
    assert_eq!(end["params"]["error"]["code"], "cancelled");
    timeout(DEADLINE, tx.closed()).await.unwrap();
    peer.ack(&end).await;
    peer.stop().await;
}

#[tokio::test]
async fn rejected_write_drops_source_and_reports_error() {
    let (mut peer, tx) = channel_peer().await;
    tx.send(Ok(Bytes::from_static(b"first"))).await.unwrap();
    let first = peer.next().await;
    write_frame(
        &mut peer.write,
        json!({
            "jsonrpc": "2.0", "id": first["id"],
            "error": {"code": -32603, "message": "write rejected"}
        }),
    )
    .await;
    timeout(DEADLINE, tx.closed()).await.unwrap();
    let end = peer.next().await;
    assert_eq!(end["params"]["end"], true);
    assert!(
        end["params"]["error"]["message"]
            .as_str()
            .unwrap()
            .contains("write rejected")
    );
    peer.ack(&end).await;
    peer.stop().await;
}

#[tokio::test]
async fn panicking_source_is_an_error_not_successful_eof() {
    let body = stream::once(async { Ok(Bytes::from_static(b"first")) }).chain(stream::poll_fn(
        |_| -> std::task::Poll<Option<Result<Bytes, CopilotRequestError>>> {
            panic!("failed source");
        },
    ));
    let mut peer = Peer::start(BodyHandler(Mutex::new(Some(Box::pin(body))))).await;
    peer.request("http://unused.test").await;
    let first = peer.next().await;
    assert_eq!(data(&first), b"first");
    peer.ack(&first).await;
    let end = peer.next().await;
    assert_eq!(end["params"]["end"], true);
    assert_eq!(
        end["params"]["error"]["message"],
        "HTTP response body stream panicked"
    );
    peer.ack(&end).await;
    peer.stop().await;
}

#[tokio::test]
async fn always_ready_source_does_not_starve_ack_or_cancellation() {
    let body = stream::repeat_with(|| Ok(Bytes::from_static(b"x")));
    let mut peer = Peer::start(BodyHandler(Mutex::new(Some(Box::pin(body))))).await;
    peer.request("http://unused.test").await;
    for _ in 0..3 {
        let chunk = peer.next().await;
        assert!(data(&chunk).iter().all(|byte| *byte == b'x'));
        peer.ack(&chunk).await;
    }
    let pending = peer.next().await;
    assert!(!data(&pending).is_empty());
    peer.request_chunk(json!({"requestId": "test", "data": "", "cancel": true}))
        .await;
    let end = peer.next().await;
    assert_eq!(end["params"]["error"]["code"], "cancelled");
    peer.ack(&end).await;
    peer.stop().await;
}

#[tokio::test]
async fn disconnected_consumer_drops_source_under_outstanding_ack() {
    let (mut peer, tx) = channel_peer().await;
    tx.send(Ok(Bytes::from_static(b"first"))).await.unwrap();
    assert_eq!(data(&peer.next().await), b"first");
    let Peer {
        client,
        read,
        write,
        _work,
    } = peer;
    drop(read);
    drop(write);
    timeout(DEADLINE, tx.closed()).await.unwrap();
    client.stop().await.unwrap();
}

struct ReturnAfterCancellation(BodyHandler);

#[async_trait]
impl CopilotRequestHandler for ReturnAfterCancellation {
    async fn send_request(
        &self,
        request: CopilotHttpRequest,
        context: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        context.cancel.cancelled().await;
        self.0.send_request(request, context).await
    }
}

#[tokio::test]
async fn cancellation_before_handler_returns_still_sends_head_before_terminal() {
    for acknowledge_head in [false, true] {
        let (tx, rx) = mpsc::channel(1);
        let body = Box::pin(ReceiverStream::new(rx));
        let mut peer =
            Peer::start(ReturnAfterCancellation(BodyHandler(Mutex::new(Some(body))))).await;
        peer.begin_request("http://unused.test").await;
        peer.request_chunk(json!({"requestId": "test", "data": "", "cancel": true}))
            .await;
        let head = peer.next().await;
        assert_eq!(head["method"], "llmInference.httpResponseStart");
        assert_eq!(head["params"]["status"], 200);
        if acknowledge_head {
            peer.ack(&head).await;
        }
        let end = peer.next().await;
        assert_eq!(end["method"], "llmInference.httpResponseChunk");
        assert_eq!(end["params"]["end"], true);
        assert_eq!(end["params"]["error"]["code"], "cancelled");
        timeout(DEADLINE, tx.closed()).await.unwrap();
        peer.ack(&end).await;
        assert!(
            timeout(Duration::from_millis(20), peer.next())
                .await
                .is_err()
        );
        peer.stop().await;
    }
}

#[tokio::test]
async fn cancellation_while_head_ack_is_withheld_drops_source() {
    let (tx, rx) = mpsc::channel(1);
    let body = Box::pin(ReceiverStream::new(rx));
    let mut peer = Peer::start(BodyHandler(Mutex::new(Some(body)))).await;
    peer.begin_request("http://unused.test").await;
    let head = peer.next().await;
    assert_eq!(head["method"], "llmInference.httpResponseStart");
    peer.request_chunk(json!({"requestId": "test", "data": "", "cancel": true}))
        .await;
    let end = peer.next().await;
    assert_eq!(end["params"]["error"]["code"], "cancelled");
    timeout(DEADLINE, tx.closed()).await.unwrap();
    peer.ack(&end).await;
    peer.stop().await;
}

struct ForwardingHandler;

#[async_trait]
impl CopilotRequestHandler for ForwardingHandler {}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn real_http_burst_preserves_headers_status_and_every_byte() {
    let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
    let url = format!("http://{}/stream", listener.local_addr().unwrap());
    let expected: Vec<u8> = (0..CHUNK_SIZE * 8 + 7).map(|i| (i % 256) as u8).collect();
    let payload = expected.clone();
    let upstream = tokio::spawn(async move {
        let (socket, _) = listener.accept().await.unwrap();
        let mut socket = BufReader::new(socket);
        loop {
            let mut line = String::new();
            assert_ne!(socket.read_line(&mut line).await.unwrap(), 0);
            if line == "\r\n" {
                break;
            }
        }
        let mut response = b"HTTP/1.1 201 Created\r\nContent-Type: text/event-stream\r\nX-Test: one\r\nX-Test: two\r\nTransfer-Encoding: chunked\r\nConnection: close\r\n\r\n".to_vec();
        for chunk in payload.chunks(1024) {
            response.extend_from_slice(format!("{:x}\r\n", chunk.len()).as_bytes());
            response.extend_from_slice(chunk);
            response.extend_from_slice(b"\r\n");
        }
        response.extend_from_slice(b"0\r\n\r\n");
        socket.write_all(&response).await.unwrap();
        socket.shutdown().await.unwrap();
    });
    let mut peer = Peer::start(ForwardingHandler).await;
    let head = peer.request(&url).await;
    assert_eq!(head["params"]["status"], 201);
    assert_eq!(head["params"]["statusText"], "Created");
    assert_eq!(head["params"]["headers"]["x-test"], json!(["one", "two"]));
    let mut received = Vec::new();
    let mut writes = 0;
    loop {
        let message = peer.next().await;
        if message["params"]["end"] == true {
            assert!(message["params"].get("error").is_none(), "{message}");
            peer.ack(&message).await;
            break;
        }
        received.extend(data(&message));
        writes += 1;
        // Give the real HTTP transport turns to populate read-ahead under an ACK.
        tokio::time::sleep(Duration::from_millis(2)).await;
        peer.ack(&message).await;
    }
    assert_eq!(received, expected);
    assert!(
        writes < 32,
        "bursty HTTP fragments were not combined: {writes}"
    );
    timeout(DEADLINE, upstream).await.unwrap().unwrap();
    peer.stop().await;
}
