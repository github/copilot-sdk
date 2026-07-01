use std::net::TcpListener;
use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use base64::Engine;
use bytes::Bytes;
use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::{
    Attachment, Client, CopilotHttpRequest, CopilotHttpResponse, CopilotRequestContext,
    CopilotRequestError, CopilotRequestHandler, MessageOptions, ProviderConfig,
    ResumeSessionConfig, SessionConfig, SessionLimitsConfig, Transport,
};
use http::{HeaderMap, HeaderValue};
use parking_lot::Mutex;
use serde_json::{Value, json};

use super::support::{
    DEFAULT_TEST_TOKEN, E2eContext, with_e2e_context, with_e2e_context_no_snapshot,
};

const SYNTHETIC_TEXT: &str = "OK from the synthetic stream.";
const CITATION_PROMPT: &str = "Summarize the attached PDF with citations enabled.";

fn session_limits(max_ai_credits: f64) -> SessionLimitsConfig {
    SessionLimitsConfig {
        max_ai_credits: Some(max_ai_credits),
    }
}

async fn send_and_get_next_exchange(
    ctx: &E2eContext,
    session: &github_copilot_sdk::session::Session,
    prompt: &str,
) -> Value {
    let existing_count = ctx.exchanges().len();
    session
        .send_and_wait(MessageOptions::new(prompt).with_wait_timeout(Duration::from_secs(120)))
        .await
        .expect("send_and_wait");
    let exchanges = ctx.exchanges();
    assert!(exchanges.len() > existing_count);
    exchanges[existing_count].clone()
}

fn assert_session_limits_status(exchange: &Value, expected_remaining: &str) {
    let messages = exchange["request"]["messages"]
        .as_array()
        .expect("request messages");
    for message in messages {
        if message["role"] != "user" {
            continue;
        }
        let Some(content) = message["content"].as_str() else {
            continue;
        };
        if !content.contains("<session_limits_status>") {
            continue;
        }
        assert!(
            content.contains(&format!("Remaining session limits: {expected_remaining}.")),
            "expected session limits status to include remaining {expected_remaining:?}, got {content:?}"
        );
        assert!(
            content.contains("Be frugal; avoid optional exploration and unnecessary tool calls."),
            "expected session limits status to include frugality instruction, got {content:?}"
        );
        return;
    }
    panic!("expected session limits status message");
}

fn task_agent_types(exchange: &Value) -> Vec<String> {
    let tools = exchange["request"]["tools"]
        .as_array()
        .expect("request tools");
    for tool in tools {
        if tool["function"]["name"] != "task" {
            continue;
        }
        return tool["function"]["parameters"]["properties"]["agent_type"]["enum"]
            .as_array()
            .expect("agent type enum")
            .iter()
            .map(|value| value.as_str().expect("agent type").to_string())
            .collect();
    }
    panic!("expected task tool in request");
}

#[tokio::test]
async fn should_apply_session_limits_on_create() {
    with_e2e_context(
        "session_config",
        "should_apply_session_limits_on_create",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_session_limits(session_limits(30.0)),
                    )
                    .await
                    .expect("create session");

                let exchange = send_and_get_next_exchange(
                    ctx,
                    &session,
                    "Acknowledge the current session limits.",
                )
                .await;
                assert_session_limits_status(&exchange, "30 AI credits");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_apply_session_limits_on_resume() {
    with_e2e_context(
        "session_config",
        "should_apply_session_limits_on_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session1 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session2 = client
                    .resume_session(
                        ResumeSessionConfig::new(session1.id().clone())
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_session_limits(session_limits(30.0)),
                    )
                    .await
                    .expect("resume session");

                let exchange = send_and_get_next_exchange(
                    ctx,
                    &session2,
                    "Acknowledge the current session limits.",
                )
                .await;
                assert_session_limits_status(&exchange, "30 AI credits");

                session2
                    .disconnect()
                    .await
                    .expect("disconnect resumed session");
                session1
                    .disconnect()
                    .await
                    .expect("disconnect original session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_apply_excluded_built_in_agents_on_create() {
    with_e2e_context(
        "session_config",
        "should_apply_excluded_built_in_agents_on_create",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;

                let baseline = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create baseline session");
                let baseline_exchange =
                    send_and_get_next_exchange(ctx, &baseline, "What is 1+1?").await;
                let baseline_agents = task_agent_types(&baseline_exchange);
                assert!(
                    baseline_agents.iter().any(|agent| agent == "explore"),
                    "expected baseline task agents to include explore, got {baseline_agents:?}"
                );
                baseline
                    .disconnect()
                    .await
                    .expect("disconnect baseline session");

                let excluded = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_excluded_builtin_agents(["explore"]),
                    )
                    .await
                    .expect("create excluded-agent session");
                let excluded_exchange =
                    send_and_get_next_exchange(ctx, &excluded, "What is 1+1?").await;
                let excluded_agents = task_agent_types(&excluded_exchange);
                assert!(!excluded_agents.is_empty());
                assert!(
                    !excluded_agents.iter().any(|agent| agent == "explore"),
                    "expected task agents not to include explore, got {excluded_agents:?}"
                );

                excluded
                    .disconnect()
                    .await
                    .expect("disconnect excluded-agent session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_apply_excluded_built_in_agents_on_resume() {
    with_e2e_context(
        "session_config",
        "should_apply_excluded_built_in_agents_on_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session1 = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let session2 = client
                    .resume_session(
                        ResumeSessionConfig::new(session1.id().clone())
                            .with_permission_handler(Arc::new(ApproveAllHandler))
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_excluded_builtin_agents(["explore"]),
                    )
                    .await
                    .expect("resume session");

                let exchange = send_and_get_next_exchange(ctx, &session2, "What is 1+1?").await;
                let agent_types = task_agent_types(&exchange);
                assert!(!agent_types.is_empty());
                assert!(
                    !agent_types.iter().any(|agent| agent == "explore"),
                    "expected task agents not to include explore, got {agent_types:?}"
                );

                session2
                    .disconnect()
                    .await
                    .expect("disconnect resumed session");
                session1
                    .disconnect()
                    .await
                    .expect("disconnect original session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[derive(Clone, Default)]
struct RecordingHandler {
    records: Arc<Mutex<Vec<RecordedRequest>>>,
}

#[derive(Clone)]
struct RecordedRequest {
    url: String,
    body: Vec<u8>,
}

impl RecordingHandler {
    fn inference_records(&self) -> Vec<RecordedRequest> {
        self.records
            .lock()
            .iter()
            .filter(|record| is_inference_url(&record.url))
            .cloned()
            .collect()
    }
}

#[async_trait]
impl CopilotRequestHandler for RecordingHandler {
    async fn send_request(
        &self,
        request: CopilotHttpRequest,
        _ctx: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        self.records.lock().push(RecordedRequest {
            url: request.url.clone(),
            body: request.body.clone(),
        });
        if is_inference_url(&request.url) {
            return Ok(synth_inference_response(&request.url, &request.body));
        }
        Ok(synth_non_inference_response(&request.url))
    }
}

fn is_inference_url(url: &str) -> bool {
    let url = url.to_lowercase();
    url.ends_with("/chat/completions")
        || url.ends_with("/responses")
        || url.ends_with("/v1/messages")
        || url.ends_with("/messages")
}

fn json_headers() -> HeaderMap {
    let mut headers = HeaderMap::new();
    headers.insert("content-type", HeaderValue::from_static("application/json"));
    headers
}

fn http_response(status: u16, headers: HeaderMap, body: Value) -> CopilotHttpResponse {
    let bytes = serde_json::to_vec(&body).expect("serialize response");
    let stream =
        futures_util::stream::once(
            async move { Ok::<Bytes, CopilotRequestError>(Bytes::from(bytes)) },
        );
    CopilotHttpResponse::new(status, None, headers, Box::pin(stream))
}

fn sse_response(body: String) -> CopilotHttpResponse {
    let mut headers = HeaderMap::new();
    headers.insert(
        "content-type",
        HeaderValue::from_static("text/event-stream"),
    );
    let stream = futures_util::stream::once(async move {
        Ok::<Bytes, CopilotRequestError>(Bytes::from(body.into_bytes()))
    });
    CopilotHttpResponse::new(200, None, headers, Box::pin(stream))
}

fn wants_stream(body: &[u8]) -> bool {
    String::from_utf8_lossy(body)
        .replace(char::is_whitespace, "")
        .contains("\"stream\":true")
}

fn anthropic_message_stream_body(text: &str) -> String {
    let events = [
        (
            "message_start",
            json!({
                "type": "message_start",
                "message": {
                    "id": "msg_stub_1",
                    "type": "message",
                    "role": "assistant",
                    "model": "claude-sonnet-4.5",
                    "content": [],
                    "stop_reason": null,
                    "stop_sequence": null,
                    "usage": { "input_tokens": 5, "output_tokens": 1 },
                },
            }),
        ),
        (
            "content_block_start",
            json!({
                "type": "content_block_start",
                "index": 0,
                "content_block": { "type": "text", "text": "" },
            }),
        ),
        (
            "content_block_delta",
            json!({
                "type": "content_block_delta",
                "index": 0,
                "delta": { "type": "text_delta", "text": text },
            }),
        ),
        (
            "content_block_stop",
            json!({ "type": "content_block_stop", "index": 0 }),
        ),
        (
            "message_delta",
            json!({
                "type": "message_delta",
                "delta": { "stop_reason": "end_turn", "stop_sequence": null },
                "usage": { "output_tokens": 7 },
            }),
        ),
        ("message_stop", json!({ "type": "message_stop" })),
    ];
    events
        .iter()
        .map(|(name, data)| format!("event: {name}\ndata: {data}\n\n"))
        .collect()
}

fn synth_non_inference_response(url: &str) -> CopilotHttpResponse {
    let lower = url.to_lowercase();
    if lower.ends_with("/models") {
        return http_response(
            200,
            json_headers(),
            json!({
                "data": [{
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
                }],
            }),
        );
    }
    if lower.contains("/policy") {
        return http_response(200, json_headers(), json!({ "state": "enabled" }));
    }
    http_response(200, json_headers(), json!({}))
}

fn synth_inference_response(url: &str, body: &[u8]) -> CopilotHttpResponse {
    let lower = url.to_lowercase();
    if lower.ends_with("/messages") {
        if wants_stream(body) {
            return sse_response(anthropic_message_stream_body(SYNTHETIC_TEXT));
        }
        return http_response(
            200,
            json_headers(),
            json!({
                "id": "msg_stub_1",
                "type": "message",
                "role": "assistant",
                "model": "claude-sonnet-4.5",
                "content": [{ "type": "text", "text": SYNTHETIC_TEXT }],
                "stop_reason": "end_turn",
                "stop_sequence": null,
                "usage": { "input_tokens": 5, "output_tokens": 7 },
            }),
        );
    }
    http_response(
        200,
        json_headers(),
        json!({
            "id": "chatcmpl-stub-1",
            "object": "chat.completion",
            "created": 1,
            "model": "claude-sonnet-4.5",
            "choices": [{
                "index": 0,
                "message": { "role": "assistant", "content": SYNTHETIC_TEXT },
                "finish_reason": "stop",
            }],
            "usage": { "prompt_tokens": 5, "completion_tokens": 7, "total_tokens": 12 },
        }),
    )
}

fn anthropic_provider() -> ProviderConfig {
    ProviderConfig::new("https://anthropic-citations.invalid/v1")
        .with_provider_type("anthropic")
        .with_api_key("test-provider-key")
        .with_model_id("claude-sonnet-4.5")
        .with_wire_model("claude-sonnet-4.5")
}

fn pdf_attachment() -> Attachment {
    let pdf_text =
        "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n";
    Attachment::Blob {
        data: base64::engine::general_purpose::STANDARD.encode(pdf_text),
        mime_type: "application/pdf".to_string(),
        display_name: Some("citation-source.pdf".to_string()),
    }
}

fn assert_anthropic_document_citations_enabled(request_body: &[u8]) {
    let body: Value = serde_json::from_slice(request_body).expect("Anthropic request body");
    let documents: Vec<&Value> = body["messages"]
        .as_array()
        .expect("messages")
        .iter()
        .flat_map(|message| message["content"].as_array().expect("message content"))
        .filter(|block| block["type"] == "document")
        .collect();

    assert_eq!(documents.len(), 1);
    assert_eq!(documents[0]["title"], "citation-source.pdf");
    assert_eq!(documents[0]["citations"]["enabled"], true);
}

#[tokio::test]
async fn should_enable_citations_for_anthropic_file_attachments_on_create() {
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let handler = RecordingHandler::default();
            let client = ctx.start_llm_client(handler.clone(), &[]).await;
            let session = client
                .create_session(
                    SessionConfig::default()
                        .with_permission_handler(Arc::new(ApproveAllHandler))
                        .with_model("claude-sonnet-4.5")
                        .with_enable_citations(true)
                        .with_provider(anthropic_provider()),
                )
                .await
                .expect("create session");

            session
                .send_and_wait(
                    MessageOptions::new(CITATION_PROMPT)
                        .with_wait_timeout(Duration::from_secs(120))
                        .with_attachments(vec![pdf_attachment()]),
                )
                .await
                .expect("send_and_wait");

            let inference_records = handler.inference_records();
            assert_eq!(inference_records.len(), 1);
            assert_anthropic_document_citations_enabled(&inference_records[0].body);

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_enable_citations_for_anthropic_file_attachments_on_resume() {
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let handler = RecordingHandler::default();
            let port = free_tcp_port();
            let token = "rust-citation-resume-token".to_string();
            let server = Client::start(
                ctx.client_options_with_transport(Transport::Tcp {
                    port,
                    connection_token: Some(token.clone()),
                })
                .with_request_handler(handler.clone()),
            )
            .await
            .expect("start TCP server client");
            let session1 = server
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");
            let resume_client =
                Client::start(ctx.client_options_with_transport(Transport::External {
                    host: "127.0.0.1".to_string(),
                    port,
                    connection_token: Some(token),
                }))
                .await
                .expect("start external client");
            let session2 = resume_client
                .resume_session(
                    ResumeSessionConfig::new(session1.id().clone())
                        .with_permission_handler(Arc::new(ApproveAllHandler))
                        .with_model("claude-sonnet-4.5")
                        .with_enable_citations(true)
                        .with_provider(anthropic_provider()),
                )
                .await
                .expect("resume session");

            session2
                .send_and_wait(
                    MessageOptions::new(CITATION_PROMPT)
                        .with_wait_timeout(Duration::from_secs(120))
                        .with_attachments(vec![pdf_attachment()]),
                )
                .await
                .expect("send_and_wait");

            let inference_records = handler.inference_records();
            assert_eq!(inference_records.len(), 1);
            assert_anthropic_document_citations_enabled(&inference_records[0].body);

            session2
                .disconnect()
                .await
                .expect("disconnect resumed session");
            session1
                .disconnect()
                .await
                .expect("disconnect original session");
            resume_client.stop().await.expect("stop external client");
            server.stop().await.expect("stop TCP server client");
        })
    })
    .await;
}

fn free_tcp_port() -> u16 {
    let listener = TcpListener::bind(("127.0.0.1", 0)).expect("bind free TCP port");
    listener.local_addr().expect("local addr").port()
}
