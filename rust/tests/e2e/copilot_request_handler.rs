//! End-to-end coverage for the Copilot request handler.
//!
//! These tests register a [`CopilotRequestHandler`] that either fabricates
//! well-formed model responses or forwards to a local upstream, then drive a
//! real agent turn and assert the runtime routed its model-layer HTTP/WebSocket
//! traffic through the handler. No recorded CAPI snapshot is used — the handler
//! replaces every outbound model call.
//!
//! Coverage mirrors the consolidated Node e2e set:
//! - `services_http_and_websocket_via_handler` — a single handler forwards both
//!   HTTP and WebSocket traffic to local upstreams (streaming round-trip).
//! - `threads_session_id_into_inference` — the runtime threads its session id
//!   into inference requests for both CAPI and BYOK sessions.
//! - `surfaces_handler_errors` — a handler that returns `Err` surfaces a
//!   transport error rather than hanging the turn.
//! - `observes_runtime_driven_cancel` — a handler that blocks until the consumer
//!   aborts observes the runtime-driven cancellation via `ctx.cancel`.

use std::sync::Arc;
use std::sync::atomic::{AtomicBool, AtomicU32, Ordering};
use std::time::{Duration, Instant};

use async_trait::async_trait;
use bytes::Bytes;
use futures_util::{SinkExt, StreamExt};
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::session_events::AssistantMessageData;
use github_copilot_sdk::{
    CopilotHttpRequest, CopilotHttpResponse, CopilotRequestContext, CopilotRequestError,
    CopilotRequestHandler, CopilotWebSocketHandler, CopilotWebSocketResponse,
    ForwardingCopilotWebSocketHandler, MessageOptions, ProviderConfig, SessionConfig, SessionEvent,
    forward_http,
};
use http::header::{HeaderName, HeaderValue};
use http::{HeaderMap, Uri};
use serde_json::{Value, json};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::{TcpListener, TcpStream};
use tokio_tungstenite::tungstenite::Message;

use super::support::with_e2e_context_no_snapshot;

const SYNTHETIC_TEXT: &str = "OK from the synthetic stream.";
const HANDLER_HTTP_TEXT: &str = "OK from synthetic HTTP upstream.";
const HANDLER_WS_TEXT: &str = "OK from synthetic WS upstream.";
const WS_SUPPORTED_ENDPOINTS: &[&str] = &["/responses", "ws:/responses"];

fn say_ok() -> MessageOptions {
    MessageOptions::new("Say OK.").with_wait_timeout(Duration::from_secs(120))
}

fn header_map(pairs: &[(&str, &str)]) -> HeaderMap {
    let mut headers = HeaderMap::new();
    for (name, value) in pairs {
        headers.insert(
            HeaderName::from_bytes(name.as_bytes()).unwrap(),
            HeaderValue::from_str(value).unwrap(),
        );
    }
    headers
}

fn json_headers() -> HeaderMap {
    header_map(&[("content-type", "application/json")])
}

fn sse_headers() -> HeaderMap {
    header_map(&[("content-type", "text/event-stream")])
}

fn assistant_text(event: &Option<SessionEvent>) -> String {
    event
        .as_ref()
        .and_then(|e| e.typed_data::<AssistantMessageData>())
        .map(|data| data.content)
        .unwrap_or_default()
}

fn is_inference_url(url: &str) -> bool {
    let url = url.to_lowercase();
    url.ends_with("/chat/completions")
        || url.ends_with("/responses")
        || url.ends_with("/v1/messages")
        || url.ends_with("/messages")
}

/// Detect `"stream": true` in a request body without depending on exact JSON
/// whitespace.
fn stream_true(body: &[u8]) -> bool {
    let text = String::from_utf8_lossy(body);
    let compact: String = text.chars().filter(|c| !c.is_whitespace()).collect();
    compact.contains("\"stream\":true")
}

fn sse(event_type: &str, data: &Value) -> String {
    format!(
        "event: {event_type}\ndata: {}\n\n",
        serde_json::to_string(data).unwrap()
    )
}

fn model_catalog(supported_endpoints: Option<&[&str]>) -> String {
    let mut model = json!({
        "id": "claude-sonnet-4.5",
        "name": "Claude Sonnet 4.5",
        "object": "model",
        "vendor": "Anthropic",
        "version": "1",
        "preview": false,
        "model_picker_enabled": true,
        "capabilities": {
            "type": "chat",
            "family": "claude-sonnet-4.5",
            "tokenizer": "o200k_base",
            "limits": {
                "max_context_window_tokens": 200000,
                "max_output_tokens": 8192,
            },
            "supports": {
                "streaming": true,
                "tool_calls": true,
                "parallel_tool_calls": true,
                "vision": true,
            },
        },
    });
    if let Some(endpoints) = supported_endpoints {
        model["supported_endpoints"] = json!(endpoints);
    }
    serde_json::to_string(&json!({ "data": [model] })).unwrap()
}

/// The ordered `/responses` event objects the runtime's reducer expects. Used
/// raw (one object == one WebSocket message) for the WS path and SSE-framed for
/// the HTTP path.
fn responses_events(text: &str, resp_id: &str) -> Vec<Value> {
    vec![
        json!({
            "type": "response.created",
            "response": { "id": resp_id, "object": "response", "status": "in_progress", "output": [] },
        }),
        json!({
            "type": "response.output_item.added",
            "output_index": 0,
            "item": { "id": "msg_1", "type": "message", "role": "assistant", "content": [] },
        }),
        json!({
            "type": "response.content_part.added",
            "output_index": 0,
            "content_index": 0,
            "part": { "type": "output_text", "text": "" },
        }),
        json!({ "type": "response.output_text.delta", "output_index": 0, "content_index": 0, "delta": text }),
        json!({ "type": "response.output_text.done", "output_index": 0, "content_index": 0, "text": text }),
        json!({
            "type": "response.completed",
            "response": {
                "id": resp_id,
                "object": "response",
                "status": "completed",
                "output": [{
                    "id": "msg_1",
                    "type": "message",
                    "role": "assistant",
                    "content": [{ "type": "output_text", "text": text }],
                }],
                "usage": { "input_tokens": 5, "output_tokens": 7, "total_tokens": 12 },
            },
        }),
    ]
}

/// Build a streaming HTTP response from a sequence of body chunks.
fn http_response(status: u16, headers: HeaderMap, chunks: Vec<Vec<u8>>) -> CopilotHttpResponse {
    let body = futures_util::stream::iter(
        chunks
            .into_iter()
            .map(|chunk| Ok::<Bytes, CopilotRequestError>(Bytes::from(chunk))),
    );
    CopilotHttpResponse::new(status, None, headers, Box::pin(body))
}

/// Serve the model catalog, model session and policy endpoints with an
/// empty-JSON fallback for anything unrecognised.
fn synth_non_inference_response(
    url: &str,
    supported_endpoints: Option<&[&str]>,
) -> CopilotHttpResponse {
    let lower = url.to_lowercase();
    if lower.ends_with("/models") {
        return http_response(
            200,
            json_headers(),
            vec![model_catalog(supported_endpoints).into_bytes()],
        );
    }
    if lower.contains("/models/session") {
        return http_response(200, HeaderMap::new(), vec![b"{}".to_vec()]);
    }
    if lower.contains("/policy") {
        return http_response(
            200,
            HeaderMap::new(),
            vec![br#"{"state":"enabled"}"#.to_vec()],
        );
    }
    http_response(200, json_headers(), vec![b"{}".to_vec()])
}

/// Synthesize a well-formed inference response, dispatching by URL and the
/// request body's stream flag exactly as a real reverse proxy would.
fn synth_inference_response(url: &str, body: &[u8], text: &str) -> CopilotHttpResponse {
    let wants_stream = stream_true(body);
    let lower = url.to_lowercase();

    if lower.contains("/responses") {
        let events = responses_events(text, "resp_stub_1");
        if !wants_stream {
            let last = serde_json::to_string(&events[events.len() - 1]["response"]).unwrap();
            return http_response(200, json_headers(), vec![last.into_bytes()]);
        }
        let chunks = events
            .iter()
            .map(|event| sse(event["type"].as_str().unwrap(), event).into_bytes())
            .collect();
        return http_response(200, sse_headers(), chunks);
    }

    if lower.contains("/chat/completions") && wants_stream {
        let base = || {
            json!({
                "id": "chatcmpl-stub-1",
                "object": "chat.completion.chunk",
                "created": 1,
                "model": "claude-sonnet-4.5",
            })
        };
        let mut c1 = base();
        c1["choices"] = json!([{ "index": 0, "delta": { "role": "assistant", "content": "" }, "finish_reason": null }]);
        let mut c2 = base();
        c2["choices"] =
            json!([{ "index": 0, "delta": { "content": text }, "finish_reason": null }]);
        let mut c3 = base();
        c3["choices"] = json!([{ "index": 0, "delta": {}, "finish_reason": "stop" }]);
        c3["usage"] = json!({ "prompt_tokens": 5, "completion_tokens": 7, "total_tokens": 12 });
        let mut chunks: Vec<Vec<u8>> = [c1, c2, c3]
            .iter()
            .map(|chunk| {
                format!("data: {}\n\n", serde_json::to_string(chunk).unwrap()).into_bytes()
            })
            .collect();
        chunks.push(b"data: [DONE]\n\n".to_vec());
        return http_response(200, sse_headers(), chunks);
    }

    let buffered = json!({
        "id": "chatcmpl-stub-1",
        "object": "chat.completion",
        "created": 1,
        "model": "claude-sonnet-4.5",
        "choices": [{
            "index": 0,
            "message": { "role": "assistant", "content": text },
            "finish_reason": "stop",
        }],
        "usage": { "prompt_tokens": 5, "completion_tokens": 7, "total_tokens": 12 },
    });
    http_response(
        200,
        json_headers(),
        vec![serde_json::to_string(&buffered).unwrap().into_bytes()],
    )
}

async fn wait_for_flag(flag: &AtomicBool, what: &str) {
    let deadline = Instant::now() + Duration::from_secs(60);
    while !flag.load(Ordering::SeqCst) {
        assert!(Instant::now() < deadline, "timed out waiting for {what}");
        tokio::time::sleep(Duration::from_millis(50)).await;
    }
}

async fn session_send(session: &github_copilot_sdk::session::Session) -> Option<SessionEvent> {
    session
        .send_and_wait(say_ok())
        .await
        .expect("send_and_wait")
}

// ---------------------------------------------------------------------------
// Scenario 1: handler — one handler forwards both HTTP and WebSocket traffic to
// local upstreams, mutating traffic on the way through.
// ---------------------------------------------------------------------------

#[derive(Clone, Default)]
struct HandlerCounters {
    http_requests: Arc<AtomicU32>,
    http_responses: Arc<AtomicU32>,
    ws_request_messages: Arc<AtomicU32>,
    ws_response_messages: Arc<AtomicU32>,
    upstream_ws_requests: Arc<AtomicU32>,
}

struct ForwardingHandler {
    http_authority: String,
    ws_authority: String,
    counters: HandlerCounters,
}

fn rewrite_authority(
    url: &str,
    scheme: &str,
    authority: &str,
) -> Result<String, CopilotRequestError> {
    let uri: Uri = url
        .parse()
        .map_err(|e| CopilotRequestError::message(format!("invalid url {url}: {e}")))?;
    let path_and_query = uri.path_and_query().map(|p| p.as_str()).unwrap_or("/");
    Ok(format!("{scheme}://{authority}{path_and_query}"))
}

#[async_trait]
impl CopilotRequestHandler for ForwardingHandler {
    async fn send_request(
        &self,
        mut request: CopilotHttpRequest,
        _ctx: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        self.counters.http_requests.fetch_add(1, Ordering::SeqCst);
        request.url = rewrite_authority(&request.url, "http", &self.http_authority)?;
        request
            .headers
            .insert("x-test-mutated", HeaderValue::from_static("1"));
        let mut response = forward_http(request).await?;
        self.counters.http_responses.fetch_add(1, Ordering::SeqCst);
        response
            .headers
            .insert("x-test-response-mutated", HeaderValue::from_static("1"));
        Ok(response)
    }

    async fn open_websocket(
        &self,
        ctx: &CopilotRequestContext,
        response: CopilotWebSocketResponse,
    ) -> Result<Box<dyn CopilotWebSocketHandler>, CopilotRequestError> {
        let ws_url = rewrite_authority(&ctx.url, "ws", &self.ws_authority)?;
        let request_counter = self.counters.ws_request_messages.clone();
        let response_counter = self.counters.ws_response_messages.clone();
        let handler = ForwardingCopilotWebSocketHandler::builder(ws_url, ctx.headers.clone())
            .on_send_request_message(Arc::new(move |message| {
                request_counter.fetch_add(1, Ordering::SeqCst);
                Some(message)
            }))
            .on_send_response_message(Arc::new(move |message| {
                response_counter.fetch_add(1, Ordering::SeqCst);
                Some(message)
            }))
            .connect(response)
            .await?;
        Ok(Box::new(handler))
    }
}

fn find_subsequence(haystack: &[u8], needle: &[u8]) -> Option<usize> {
    haystack
        .windows(needle.len())
        .position(|window| window == needle)
}

fn route_http_upstream(path: &str) -> (u16, &'static str, String) {
    if path.ends_with("/models") {
        (
            200,
            "application/json",
            model_catalog(Some(WS_SUPPORTED_ENDPOINTS)),
        )
    } else if path.ends_with("/models/session") {
        (200, "application/json", "{}".to_string())
    } else if path.contains("/policy") {
        (
            200,
            "application/json",
            r#"{"state":"enabled"}"#.to_string(),
        )
    } else if path.ends_with("/responses") {
        let mut body = String::new();
        for event in responses_events(HANDLER_HTTP_TEXT, "resp_stub_http") {
            body.push_str(&sse(event["type"].as_str().unwrap(), &event));
        }
        (200, "text/event-stream", body)
    } else {
        (
            404,
            "application/json",
            r#"{"error":"not_found"}"#.to_string(),
        )
    }
}

async fn serve_http_conn(socket: &mut TcpStream) -> std::io::Result<()> {
    let mut buf = Vec::new();
    let mut tmp = [0u8; 4096];
    let header_end = loop {
        let n = socket.read(&mut tmp).await?;
        if n == 0 {
            return Ok(());
        }
        buf.extend_from_slice(&tmp[..n]);
        if let Some(pos) = find_subsequence(&buf, b"\r\n\r\n") {
            break pos + 4;
        }
    };
    let head = String::from_utf8_lossy(&buf[..header_end]).to_string();
    let content_length = head
        .lines()
        .find_map(|line| {
            let (name, value) = line.split_once(':')?;
            if name.trim().eq_ignore_ascii_case("content-length") {
                value.trim().parse::<usize>().ok()
            } else {
                None
            }
        })
        .unwrap_or(0);
    let mut remaining = content_length.saturating_sub(buf.len() - header_end);
    while remaining > 0 {
        let n = socket.read(&mut tmp).await?;
        if n == 0 {
            break;
        }
        remaining = remaining.saturating_sub(n);
    }

    let request_path = head
        .lines()
        .next()
        .and_then(|line| line.split_whitespace().nth(1))
        .unwrap_or("/")
        .split('?')
        .next()
        .unwrap_or("/")
        .to_lowercase();
    let (status, content_type, body) = route_http_upstream(&request_path);
    let reason = if status == 200 { "OK" } else { "Not Found" };
    let head = format!(
        "HTTP/1.1 {status} {reason}\r\ncontent-type: {content_type}\r\ncontent-length: {}\r\nconnection: close\r\n\r\n",
        body.len()
    );
    socket.write_all(head.as_bytes()).await?;
    socket.write_all(body.as_bytes()).await?;
    socket.flush().await?;
    let _ = socket.shutdown().await;
    Ok(())
}

async fn start_http_upstream() -> String {
    let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
    let authority = listener.local_addr().unwrap().to_string();
    tokio::spawn(async move {
        while let Ok((mut socket, _)) = listener.accept().await {
            tokio::spawn(async move {
                let _ = serve_http_conn(&mut socket).await;
            });
        }
    });
    authority
}

async fn start_ws_upstream(counters: HandlerCounters) -> String {
    let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
    let authority = listener.local_addr().unwrap().to_string();
    tokio::spawn(async move {
        while let Ok((socket, _)) = listener.accept().await {
            let counters = counters.clone();
            tokio::spawn(async move {
                let ws = match tokio_tungstenite::accept_async(socket).await {
                    Ok(ws) => ws,
                    Err(_) => return,
                };
                let (mut write, mut read) = ws.split();
                while let Some(Ok(message)) = read.next().await {
                    match message {
                        Message::Text(_) | Message::Binary(_) => {
                            counters.upstream_ws_requests.fetch_add(1, Ordering::SeqCst);
                            for event in responses_events(HANDLER_WS_TEXT, "resp_stub_ws") {
                                let raw = serde_json::to_string(&event).unwrap();
                                if write.send(Message::Text(raw)).await.is_err() {
                                    return;
                                }
                            }
                        }
                        Message::Close(_) => break,
                        _ => {}
                    }
                }
            });
        }
    });
    authority
}

#[tokio::test]
async fn services_http_and_websocket_via_handler() {
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let counters = HandlerCounters::default();
            let http_authority = start_http_upstream().await;
            let ws_authority = start_ws_upstream(counters.clone()).await;

            let handler = ForwardingHandler {
                http_authority,
                ws_authority,
                counters: counters.clone(),
            };
            let client = ctx
                .start_llm_client(
                    handler,
                    &[("COPILOT_EXP_COPILOT_CLI_WEBSOCKET_RESPONSES", "true")],
                )
                .await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let result = session
                .send_and_wait(say_ok())
                .await
                .expect("send_and_wait");
            let _ = session.disconnect().await;

            assert!(
                counters.http_requests.load(Ordering::SeqCst) > 0,
                "expected the HTTP forwarder to fire"
            );
            assert!(
                counters.http_responses.load(Ordering::SeqCst) > 0,
                "expected the HTTP response mutation to fire"
            );
            assert!(
                counters.ws_request_messages.load(Ordering::SeqCst) > 0,
                "expected runtime → upstream ws messages"
            );
            assert!(
                counters.ws_response_messages.load(Ordering::SeqCst) > 0,
                "expected upstream → runtime ws messages"
            );
            assert!(
                counters.upstream_ws_requests.load(Ordering::SeqCst) > 0,
                "expected the upstream WS to receive request messages"
            );

            // Validate the final assistant response arrived (guards against truncated captures)
            let text = assistant_text(&result);
            assert!(
                text.contains("OK from synthetic") && text.contains("upstream"),
                "expected synthetic upstream content in assistant reply, got {text:?}"
            );

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

// ---------------------------------------------------------------------------
// Scenario 2: session id — the runtime threads the session id into CAPI and
// BYOK inference requests serviced entirely by the handler.
// ---------------------------------------------------------------------------

#[derive(Default)]
struct RecordingHandler {
    records: std::sync::Mutex<Vec<(String, Option<String>)>>,
}

impl RecordingHandler {
    fn inference_records(&self) -> Vec<(String, Option<String>)> {
        self.records
            .lock()
            .unwrap()
            .iter()
            .filter(|(url, _)| is_inference_url(url))
            .cloned()
            .collect()
    }
}

#[async_trait]
impl CopilotRequestHandler for RecordingHandler {
    async fn send_request(
        &self,
        request: CopilotHttpRequest,
        ctx: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        self.records
            .lock()
            .unwrap()
            .push((request.url.clone(), ctx.session_id.clone()));
        if is_inference_url(&request.url) {
            Ok(synth_inference_response(
                &request.url,
                &request.body,
                SYNTHETIC_TEXT,
            ))
        } else {
            Ok(synth_non_inference_response(&request.url, None))
        }
    }
}

#[tokio::test]
async fn threads_session_id_into_inference() {
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let handler = Arc::new(RecordingHandler::default());
            let client = ctx.start_llm_client(handler.clone(), &[]).await;

            // CAPI session.
            let capi_session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create CAPI session");
            let capi_session_id = capi_session.id().as_str().to_string();
            let result = session_send(&capi_session).await;
            let _ = capi_session.disconnect().await;

            let inference = handler.inference_records();
            assert!(
                !inference.is_empty(),
                "expected at least one intercepted inference request"
            );
            for (_, session_id) in &inference {
                assert_eq!(
                    session_id.as_deref(),
                    Some(capi_session_id.as_str()),
                    "CAPI inference request must carry the session id"
                );
            }
            assert!(
                assistant_text(&result).contains("OK from the synthetic"),
                "expected synthetic content in CAPI reply, got {:?}",
                assistant_text(&result)
            );

            // BYOK session.
            let before = handler.inference_records().len();
            let byok_config = SessionConfig::default()
                .with_permission_handler(Arc::new(ApproveAllHandler))
                .with_model("claude-sonnet-4.5")
                .with_provider(
                    ProviderConfig::new("https://byok.invalid/v1")
                        .with_provider_type("openai")
                        .with_wire_api("responses")
                        .with_api_key("byok-secret")
                        .with_model_id("claude-sonnet-4.5")
                        .with_wire_model("claude-sonnet-4.5"),
                );
            let byok_session = client
                .create_session(byok_config)
                .await
                .expect("create BYOK session");
            let byok_session_id = byok_session.id().as_str().to_string();
            let result = session_send(&byok_session).await;
            let _ = byok_session.disconnect().await;

            let inference = handler.inference_records();
            assert!(
                inference.len() > before,
                "expected at least one intercepted BYOK inference request"
            );
            for (_, session_id) in &inference[before..] {
                assert_eq!(
                    session_id.as_deref(),
                    Some(byok_session_id.as_str()),
                    "BYOK inference request must carry the session id"
                );
            }
            assert_ne!(
                byok_session_id, capi_session_id,
                "expected per-session ids to differ between turns"
            );
            assert!(
                assistant_text(&result).contains("OK from the synthetic"),
                "expected synthetic content in BYOK reply, got {:?}",
                assistant_text(&result)
            );

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

// ---------------------------------------------------------------------------
// Scenario 3a: errors — a handler that returns `Err` on an inference request
// surfaces a transport error rather than hanging the turn.
// ---------------------------------------------------------------------------

#[derive(Default)]
struct ThrowingHandler {
    inference_attempts: AtomicU32,
}

#[async_trait]
impl CopilotRequestHandler for ThrowingHandler {
    async fn send_request(
        &self,
        request: CopilotHttpRequest,
        _ctx: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        if !is_inference_url(&request.url) {
            return Ok(synth_non_inference_response(&request.url, None));
        }
        self.inference_attempts.fetch_add(1, Ordering::SeqCst);
        Err(CopilotRequestError::message(
            "synthetic-callback-transport-failure",
        ))
    }
}

#[tokio::test]
async fn surfaces_handler_errors() {
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let handler = Arc::new(ThrowingHandler::default());
            let client = ctx.start_llm_client(handler.clone(), &[]).await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            // The handler returns Err from the inference seam; the agent layer
            // surfaces it as an error rather than hanging.
            let send_result = session.send_and_wait(say_ok()).await;
            let _ = session.disconnect().await;

            assert!(
                handler.inference_attempts.load(Ordering::SeqCst) > 0,
                "expected the inference callback to be reached and raise"
            );
            if let Err(err) = send_result {
                assert!(
                    !err.to_string().is_empty(),
                    "expected a non-empty error string when an error surfaces"
                );
            }

            client.stop().await.expect("stop client");
        })
    })
    .await;
}

// ---------------------------------------------------------------------------
// Scenario 3b: runtime-driven cancel — the handler blocks an inference request
// until the consumer aborts the turn; the runtime cancels the in-flight request
// and the handler observes it via `ctx.cancel`.
// ---------------------------------------------------------------------------

#[derive(Default)]
struct CancellingHandler {
    inference_entered: AtomicBool,
    saw_abort: AtomicBool,
}

#[async_trait]
impl CopilotRequestHandler for CancellingHandler {
    async fn send_request(
        &self,
        request: CopilotHttpRequest,
        ctx: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        if !is_inference_url(&request.url) {
            return Ok(synth_non_inference_response(&request.url, None));
        }

        // Inference: never produce a response. Wait for the runtime to cancel
        // us, recording the abort, then propagate it as an error.
        self.inference_entered.store(true, Ordering::SeqCst);
        ctx.cancel.cancelled().await;
        self.saw_abort.store(true, Ordering::SeqCst);
        Err(CopilotRequestError::message("cancelled by runtime"))
    }
}

#[tokio::test]
async fn observes_runtime_driven_cancel() {
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let handler = Arc::new(CancellingHandler::default());
            let client = ctx.start_llm_client(handler.clone(), &[]).await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            session.send(say_ok()).await.expect("send");
            wait_for_flag(&handler.inference_entered, "inference entered").await;
            session.abort().await.expect("abort");
            wait_for_flag(&handler.saw_abort, "consumer observed cancellation").await;
            let _ = session.disconnect().await;

            assert!(
                handler.inference_entered.load(Ordering::SeqCst),
                "expected the inference callback to be entered"
            );
            assert!(
                handler.saw_abort.load(Ordering::SeqCst),
                "expected the consumer to observe the runtime-driven cancellation"
            );

            client.stop().await.expect("stop client");
        })
    })
    .await;
}
