#![allow(clippy::unwrap_used)]

use std::path::Path;
use std::path::PathBuf;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use copilot::handler::{
    ApproveAllHandler, ExitPlanModeResult, HandlerEvent, HandlerResponse, PermissionResult,
    SessionHandler, UserInputResponse,
};
use copilot::types::{
    CommandDefinition, CustomAgentConfig, InfiniteSessionConfig, MessageOptions,
    ModelCapabilitiesOverride, ModelCapabilitiesOverrideLimits,
    ModelCapabilitiesOverrideLimitsVision, ModelCapabilitiesOverrideSupports, ProviderConfig,
    ResumeSessionConfig, SessionConfig, SessionEventType, SetModelOptions, ToolResult,
};
use copilot::Client;
use serde_json::Value;
use tokio::io::{duplex, AsyncWrite, AsyncWriteExt};
use tokio::sync::mpsc;
use tokio::time::timeout;

const TIMEOUT: Duration = Duration::from_secs(2);

struct NoopHandler;
#[async_trait]
impl SessionHandler for NoopHandler {
    async fn on_event(&self, _event: HandlerEvent) -> HandlerResponse {
        HandlerResponse::Ok
    }
}

async fn write_framed(writer: &mut (impl AsyncWrite + Unpin), body: &[u8]) {
    let header = format!("Content-Length: {}\r\n\r\n", body.len());
    writer.write_all(header.as_bytes()).await.unwrap();
    writer.write_all(body).await.unwrap();
    writer.flush().await.unwrap();
}

async fn read_framed(reader: &mut (impl tokio::io::AsyncRead + Unpin)) -> Value {
    let mut header = String::new();
    loop {
        let mut byte = [0u8; 1];
        tokio::io::AsyncReadExt::read_exact(reader, &mut byte)
            .await
            .unwrap();
        header.push(byte[0] as char);
        if header.ends_with("\r\n\r\n") {
            break;
        }
    }
    let length: usize = header
        .trim()
        .strip_prefix("Content-Length: ")
        .unwrap()
        .parse()
        .unwrap();
    let mut buf = vec![0u8; length];
    tokio::io::AsyncReadExt::read_exact(reader, &mut buf)
        .await
        .unwrap();
    serde_json::from_slice(&buf).unwrap()
}

fn make_client() -> (Client, tokio::io::DuplexStream, tokio::io::DuplexStream) {
    let (client_write, server_read) = duplex(8192);
    let (server_write, client_read) = duplex(8192);
    let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
    (client, server_read, server_write)
}

struct FakeServer {
    read: tokio::io::DuplexStream,
    write: tokio::io::DuplexStream,
    session_id: String,
}

impl FakeServer {
    async fn read_request(&mut self) -> Value {
        read_framed(&mut self.read).await
    }

    async fn respond(&mut self, request: &Value, result: Value) {
        let id = request["id"].as_u64().unwrap();
        let response = serde_json::json!({ "jsonrpc": "2.0", "id": id, "result": result });
        write_framed(&mut self.write, &serde_json::to_vec(&response).unwrap()).await;
    }

    async fn send_notification(&mut self, method: &str, params: Value) {
        let notification = serde_json::json!({
            "jsonrpc": "2.0",
            "method": method,
            "params": params,
        });
        write_framed(&mut self.write, &serde_json::to_vec(&notification).unwrap()).await;
    }

    async fn send_event(&mut self, event_type: &str, data: Value) {
        self.send_notification(
            "session.event",
            serde_json::json!({
                "sessionId": self.session_id,
                "event": {
                    "id": format!("evt-{}", rand_id()),
                    "timestamp": "2025-01-01T00:00:00Z",
                    "type": event_type,
                    "data": data,
                },
            }),
        )
        .await;
    }

    async fn send_request(&mut self, id: u64, method: &str, params: Value) {
        let request = serde_json::json!({
            "jsonrpc": "2.0",
            "id": id,
            "method": method,
            "params": params,
        });
        write_framed(&mut self.write, &serde_json::to_vec(&request).unwrap()).await;
    }

    async fn read_response(&mut self) -> Value {
        read_framed(&mut self.read).await
    }
}

async fn create_session_pair(
    handler: Arc<dyn SessionHandler>,
) -> (copilot::session::Session, FakeServer) {
    create_session_pair_with_capabilities(handler, serde_json::json!(null)).await
}

async fn create_session_pair_with_capabilities(
    handler: Arc<dyn SessionHandler>,
    capabilities: Value,
) -> (copilot::session::Session, FakeServer) {
    let (client, server_read, server_write) = make_client();
    let session_id = format!("test-session-{}", rand_id());

    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: session_id.clone(),
    };

    let create_handle = tokio::spawn({
        let client = client.clone();
        let handler = handler.clone();
        async move {
            client
                .create_session(SessionConfig::default(), handler, None, None)
                .await
                .unwrap()
        }
    });

    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
    let mut result = serde_json::json!({
        "sessionId": session_id,
        "workspacePath": "/tmp/workspace"
    });
    if !capabilities.is_null() {
        result["capabilities"] = capabilities;
    }
    server.respond(&create_req, result).await;

    let session = timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
    (session, server)
}

fn rand_id() -> u64 {
    static COUNTER: AtomicUsize = AtomicUsize::new(0);
    COUNTER.fetch_add(1, Ordering::Relaxed) as u64
}

#[tokio::test]
async fn create_session_sends_correct_rpc() {
    let (client, mut server_read, mut server_write) = make_client();

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(
                    SessionConfig {
                        model: Some("gpt-4".to_string()),
                        ..Default::default()
                    },
                    Arc::new(NoopHandler),
                    None,
                    None,
                )
                .await
                .unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.create");
    assert_eq!(request["params"]["model"], "gpt-4");

    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": { "sessionId": "s1", "workspacePath": "/ws" },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let session = timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
    assert_eq!(session.id(), "s1");
    assert_eq!(session.workspace_path(), Some(Path::new("/ws")));
}

#[tokio::test]
async fn create_session_forwards_extended_configuration() {
    let (client, mut server_read, mut server_write) = make_client();

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(
                    SessionConfig {
                        session_id: Some("custom-session".into()),
                        model: Some("gpt-5".to_string()),
                        client_name: Some("rust-sdk-tests".to_string()),
                        reasoning_effort: Some("high".to_string()),
                        model_capabilities: Some(ModelCapabilitiesOverride {
                            supports: Some(ModelCapabilitiesOverrideSupports {
                                vision: Some(true),
                                reasoning_effort: Some(true),
                            }),
                            limits: Some(ModelCapabilitiesOverrideLimits {
                                max_context_window_tokens: Some(32768),
                                vision: Some(ModelCapabilitiesOverrideLimitsVision {
                                    supported_media_types: Some(vec!["image/png".to_string()]),
                                    max_prompt_images: Some(8),
                                    max_prompt_image_size: Some(4096),
                                }),
                                ..Default::default()
                            }),
                        }),
                        config_dir: Some(PathBuf::from("/config")),
                        working_directory: Some(PathBuf::from("/workspace")),
                        available_tools: Some(vec!["powershell".to_string()]),
                        provider: Some(ProviderConfig {
                            provider_type: None,
                            base_url: Some("https://example.com".to_string()),
                            api_key: None,
                            bearer_token: None,
                            wire_api: None,
                            azure: None,
                            headers: None,
                        }),
                        custom_agents: Some(vec![CustomAgentConfig {
                            name: "reviewer".to_string(),
                            display_name: Some("Reviewer".to_string()),
                            description: None,
                            tools: Some(vec!["powershell".to_string()]),
                            prompt: None,
                            mcp_servers: None,
                            infer: Some(true),
                            skills: None,
                        }]),
                        agent: Some("reviewer".to_string()),
                        infinite_sessions: Some(InfiniteSessionConfig {
                            enabled: Some(true),
                            background_compaction_threshold: Some(0.8),
                            buffer_exhaustion_threshold: Some(0.95),
                        }),
                        commands: Some(vec![CommandDefinition {
                            name: "fix".to_string(),
                            description: Some("Fix the issue".to_string()),
                        }]),
                        ..Default::default()
                    },
                    Arc::new(NoopHandler),
                    None,
                    None,
                )
                .await
                .unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.create");
    assert_eq!(request["params"]["sessionId"], "custom-session");
    assert_eq!(request["params"]["model"], "gpt-5");
    assert_eq!(request["params"]["clientName"], "rust-sdk-tests");
    assert_eq!(request["params"]["reasoningEffort"], "high");
    assert_eq!(
        request["params"]["modelCapabilities"]["supports"]["vision"],
        true
    );
    assert_eq!(
        request["params"]["modelCapabilities"]["limits"]["maxContextWindowTokens"],
        32768
    );
    assert_eq!(
        request["params"]["modelCapabilities"]["limits"]["vision"]["maxPromptImages"],
        8
    );
    assert_eq!(request["params"]["configDir"], "/config");
    assert_eq!(request["params"]["workingDirectory"], "/workspace");
    assert_eq!(
        request["params"]["provider"]["baseUrl"],
        "https://example.com"
    );
    assert_eq!(request["params"]["agent"], "reviewer");
    assert_eq!(request["params"]["commands"][0]["name"], "fix");

    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": { "sessionId": "s1", "workspacePath": "/ws" },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let session = timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
    assert_eq!(session.id(), "s1");
}

#[tokio::test]
async fn resume_session_forwards_extended_configuration() {
    let (client, mut server_read, mut server_write) = make_client();

    let resume_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .resume_session(
                    ResumeSessionConfig {
                        session_id: "resume-me".into(),
                        client_name: Some("rust-sdk-tests".to_string()),
                        model: Some("claude-sonnet-4.6".to_string()),
                        reasoning_effort: Some("medium".to_string()),
                        model_capabilities: Some(ModelCapabilitiesOverride {
                            supports: Some(ModelCapabilitiesOverrideSupports {
                                vision: Some(true),
                                reasoning_effort: None,
                            }),
                            limits: Some(ModelCapabilitiesOverrideLimits {
                                max_output_tokens: Some(2048),
                                ..Default::default()
                            }),
                        }),
                        streaming: Some(true),
                        system_message: None,
                        tools: None,
                        available_tools: Some(vec!["powershell".to_string()]),
                        excluded_tools: None,
                        provider: Some(ProviderConfig {
                            provider_type: None,
                            base_url: Some("https://example.com".to_string()),
                            api_key: None,
                            bearer_token: None,
                            wire_api: None,
                            azure: None,
                            headers: None,
                        }),
                        working_directory: Some(PathBuf::from("/workspace")),
                        config_dir: Some(PathBuf::from("/config")),
                        mcp_servers: None,
                        env_value_mode: None,
                        enable_config_discovery: Some(true),
                        request_user_input: Some(true),
                        request_permission: Some(true),
                        request_exit_plan_mode: Some(true),
                        request_elicitation: Some(true),
                        skill_directories: None,
                        disabled_skills: Some(vec!["legacy".to_string()]),
                        hooks: Some(true),
                        custom_agents: Some(vec![CustomAgentConfig {
                            name: "reviewer".to_string(),
                            display_name: None,
                            description: Some("Reviewer agent".to_string()),
                            tools: None,
                            prompt: None,
                            mcp_servers: None,
                            infer: None,
                            skills: None,
                        }]),
                        agent: Some("reviewer".to_string()),
                        infinite_sessions: Some(InfiniteSessionConfig {
                            enabled: Some(true),
                            background_compaction_threshold: Some(0.75),
                            buffer_exhaustion_threshold: Some(0.9),
                        }),
                        commands: Some(vec![CommandDefinition {
                            name: "fix".to_string(),
                            description: Some("Fix the issue".to_string()),
                        }]),
                        disable_resume: Some(true),
                    },
                    Arc::new(NoopHandler),
                    None,
                    None,
                )
                .await
                .unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.resume");
    assert_eq!(request["params"]["sessionId"], "resume-me");
    assert_eq!(request["params"]["model"], "claude-sonnet-4.6");
    assert_eq!(request["params"]["reasoningEffort"], "medium");
    assert_eq!(
        request["params"]["modelCapabilities"]["supports"]["vision"],
        true
    );
    assert_eq!(
        request["params"]["modelCapabilities"]["limits"]["maxOutputTokens"],
        2048
    );
    assert_eq!(
        request["params"]["provider"]["baseUrl"],
        "https://example.com"
    );
    assert_eq!(request["params"]["workingDirectory"], "/workspace");
    assert_eq!(request["params"]["configDir"], "/config");
    assert_eq!(request["params"]["disabledSkills"][0], "legacy");
    assert_eq!(request["params"]["agent"], "reviewer");
    assert_eq!(request["params"]["disableResume"], true);

    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": { "sessionId": "resume-me" },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let reload_request = read_framed(&mut server_read).await;
    assert_eq!(reload_request["method"], "session.skills.reload");
    assert_eq!(reload_request["params"]["sessionId"], "resume-me");
    let reload_response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": reload_request["id"],
        "result": {},
    });
    write_framed(
        &mut server_write,
        &serde_json::to_vec(&reload_response).unwrap(),
    )
    .await;

    let session = timeout(TIMEOUT, resume_handle).await.unwrap().unwrap();
    assert_eq!(session.id(), "resume-me");
}

#[tokio::test]
async fn send_message_injects_session_id() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .send_message(MessageOptions::new("hello").with_mode("agent"))
                .await
        }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.send");
    assert_eq!(request["params"]["sessionId"], server.session_id);
    assert_eq!(request["params"]["prompt"], "hello");
    assert_eq!(request["params"]["mode"], "agent");

    server
        .respond(&request, serde_json::json!({"messageId": "msg-1"}))
        .await;
    let message_id = timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
    assert_eq!(message_id, "msg-1");
}

#[tokio::test]
async fn session_rpc_methods_send_correct_method_names() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let cases: Vec<(&str, Option<&str>)> = vec![
        ("session.abort", None),
        ("session.log", Some("message")),
        ("session.destroy", None),
    ];

    for (expected_method, extra_param_key) in cases {
        let s = session.clone();
        let handle = tokio::spawn(async move {
            match expected_method {
                "session.abort" => s.abort().await.map(|_| ()),
                "session.log" => s.log("test msg", None).await,
                "session.destroy" => s.disconnect().await,
                _ => unreachable!(),
            }
        });

        let request = server.read_request().await;
        assert_eq!(
            request["method"], expected_method,
            "wrong method for {expected_method}"
        );
        assert_eq!(request["params"]["sessionId"], server.session_id);
        if let Some(key) = extra_param_key {
            assert!(!request["params"][key].is_null(), "missing param {key}");
        }
        server.respond(&request, serde_json::json!({})).await;
        timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
    }
}

#[tokio::test]
async fn client_rpc_methods_send_correct_method_names() {
    let (client, mut server_read, mut server_write) = make_client();

    for expected_method in ["getStatus", "getAuthStatus"] {
        let c = client.clone();
        let handle = tokio::spawn(async move {
            match expected_method {
                "getStatus" => c.get_status().await.map(|_| ()),
                "getAuthStatus" => c.get_auth_status().await.map(|_| ()),
                _ => unreachable!(),
            }
        });

        let request = read_framed(&mut server_read).await;
        assert_eq!(request["method"], expected_method);
        let id = request["id"].as_u64().unwrap();
        let resp = serde_json::json!({ "jsonrpc": "2.0", "id": id, "result": {} });
        write_framed(&mut server_write, &serde_json::to_vec(&resp).unwrap()).await;
        timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
    }
}

#[tokio::test]
async fn list_sessions_returns_typed_metadata() {
    let (client, mut server_read, mut server_write) = make_client();

    let handle = tokio::spawn({
        let client = client.clone();
        async move { client.list_sessions(None).await.unwrap() }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.list");
    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": {
            "sessions": [{
                "sessionId": "s1",
                "startTime": "2025-01-01T00:00:00Z",
                "modifiedTime": "2025-01-01T01:00:00Z",
                "summary": "test session",
                "isRemote": false,
            }]
        },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let sessions = timeout(TIMEOUT, handle).await.unwrap().unwrap();
    assert_eq!(sessions.len(), 1);
    assert_eq!(sessions[0].session_id, "s1");
    assert_eq!(sessions[0].summary, Some("test session".to_string()));
}

#[tokio::test]
async fn delete_session_sends_session_id() {
    let (client, mut server_read, mut server_write) = make_client();

    let handle = tokio::spawn({
        let client = client.clone();
        async move { client.delete_session("s-to-delete").await }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.delete");
    assert_eq!(request["params"]["sessionId"], "s-to-delete");

    let id = request["id"].as_u64().unwrap();
    let resp = serde_json::json!({ "jsonrpc": "2.0", "id": id, "result": {} });
    write_framed(&mut server_write, &serde_json::to_vec(&resp).unwrap()).await;
    timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
}

#[tokio::test]
async fn list_models_returns_typed_model_info() {
    let (client, mut server_read, mut server_write) = make_client();

    let handle = tokio::spawn({
        let client = client.clone();
        async move { client.list_models().await.unwrap() }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "models.list");
    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": {
            "models": [
                { "id": "gpt-4", "name": "GPT-4" },
                { "id": "claude-sonnet-4", "name": "Claude Sonnet" },
            ]
        },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let models = timeout(TIMEOUT, handle).await.unwrap().unwrap();
    assert_eq!(models.len(), 2);
    assert_eq!(models[0].id, "gpt-4");
    assert_eq!(models[1].name, "Claude Sonnet");
}

#[tokio::test]
async fn get_messages_returns_typed_events() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move { session.get_messages().await.unwrap() }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.getMessages");
    server
        .respond(
            &request,
            serde_json::json!({
                "events": [{
                    "id": "e1",
                    "timestamp": "2025-01-01T00:00:00Z",
                    "type": "user.message",
                    "data": { "content": "hello" },
                }]
            }),
        )
        .await;

    let events = timeout(TIMEOUT, handle).await.unwrap().unwrap();
    assert_eq!(events.len(), 1);
    assert_eq!(events[0].event_type, SessionEventType::UserMessage);
}

#[tokio::test]
async fn set_model_returns_model_id() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move { session.set_model("claude-sonnet-4", None).await.unwrap() }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.model.switchTo");
    assert_eq!(request["params"]["modelId"], "claude-sonnet-4");
    server
        .respond(
            &request,
            serde_json::json!({ "modelId": "claude-sonnet-4" }),
        )
        .await;

    assert_eq!(
        timeout(TIMEOUT, handle).await.unwrap().unwrap(),
        Some("claude-sonnet-4".to_string())
    );
}

#[tokio::test]
async fn set_model_with_options_forwards_model_capabilities() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .set_model_with_options(
                    "claude-sonnet-4.6",
                    SetModelOptions::new()
                        .with_reasoning_effort("high")
                        .with_model_capabilities(ModelCapabilitiesOverride {
                            supports: Some(ModelCapabilitiesOverrideSupports {
                                vision: Some(true),
                                reasoning_effort: Some(true),
                            }),
                            limits: Some(ModelCapabilitiesOverrideLimits {
                                max_prompt_tokens: Some(8192),
                                ..Default::default()
                            }),
                        }),
                )
                .await
                .unwrap()
        }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.model.switchTo");
    assert_eq!(request["params"]["modelId"], "claude-sonnet-4.6");
    assert_eq!(request["params"]["reasoningEffort"], "high");
    assert_eq!(
        request["params"]["modelCapabilities"]["supports"]["vision"],
        true
    );
    assert_eq!(
        request["params"]["modelCapabilities"]["limits"]["maxPromptTokens"],
        8192
    );
    server
        .respond(
            &request,
            serde_json::json!({ "modelId": "claude-sonnet-4.6" }),
        )
        .await;

    assert_eq!(
        timeout(TIMEOUT, handle).await.unwrap().unwrap(),
        Some("claude-sonnet-4.6".to_string())
    );
}

#[tokio::test]
async fn elicitation_returns_typed_result() {
    let (session, mut server) = create_session_pair_with_capabilities(
        Arc::new(NoopHandler),
        serde_json::json!({ "ui": { "elicitation": true } }),
    )
    .await;
    let session = Arc::new(session);
    let schema = serde_json::json!({
        "type": "object",
        "properties": { "name": { "type": "string" } },
    });

    let handle = tokio::spawn({
        let session = session.clone();
        let schema = schema.clone();
        async move {
            session
                .elicitation("Enter your name", schema)
                .await
                .unwrap()
        }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.ui.elicitation");
    assert_eq!(request["params"]["message"], "Enter your name");
    assert_eq!(request["params"]["schema"], schema);
    server
        .respond(
            &request,
            serde_json::json!({ "action": "accept", "content": { "name": "Octocat" } }),
        )
        .await;

    let result = timeout(TIMEOUT, handle).await.unwrap().unwrap();
    assert_eq!(result.action, "accept");
    assert_eq!(result.content.unwrap()["name"], "Octocat");
}

#[tokio::test]
async fn tool_call_dispatches_to_handler() {
    struct ToolHandler;
    #[async_trait]
    impl SessionHandler for ToolHandler {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            match event {
                HandlerEvent::ExternalTool { invocation } => {
                    assert_eq!(invocation.tool_name, "read_file");
                    HandlerResponse::ToolResult(ToolResult::Text("file contents here".to_string()))
                }
                _ => HandlerResponse::Ok,
            }
        }
    }

    let (_session, mut server) = create_session_pair(Arc::new(ToolHandler)).await;
    server
        .send_request(
            100,
            "tool.call",
            serde_json::json!({
                "sessionId": server.session_id,
                "toolCallId": "tc-1",
                "toolName": "read_file",
                "arguments": { "path": "/foo.txt" },
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 100);
    assert_eq!(response["result"]["result"], "file contents here");
}

#[tokio::test]
async fn permission_request_dispatches_to_handler() {
    struct DenyHandler;
    #[async_trait]
    impl SessionHandler for DenyHandler {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            match event {
                HandlerEvent::PermissionRequest { .. } => {
                    HandlerResponse::Permission(PermissionResult::DeniedByRules)
                }
                _ => HandlerResponse::Ok,
            }
        }
    }

    let (_session, mut server) = create_session_pair(Arc::new(DenyHandler)).await;
    server
        .send_request(
            200,
            "permission.request",
            serde_json::json!({
                "sessionId": server.session_id,
                "requestId": "perm-1",
                "kind": "shell",
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 200);
    assert_eq!(response["result"]["result"]["kind"], "denied-by-rules");
}

#[tokio::test]
async fn user_input_request_dispatches_to_handler() {
    struct InputHandler;
    #[async_trait]
    impl SessionHandler for InputHandler {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            match event {
                HandlerEvent::UserInput { question, .. } => {
                    assert_eq!(question, "Pick a color");
                    HandlerResponse::UserInput(Some(UserInputResponse::new("blue", true)))
                }
                _ => HandlerResponse::Ok,
            }
        }
    }

    let (_session, mut server) = create_session_pair(Arc::new(InputHandler)).await;
    server
        .send_request(
            300,
            "userInput.request",
            serde_json::json!({
                "sessionId": server.session_id,
                "question": "Pick a color",
                "choices": ["red", "blue"],
                "allowFreeform": true,
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 300);
    assert_eq!(response["result"]["answer"], "blue");
    assert_eq!(response["result"]["wasFreeform"], true);
}

#[tokio::test]
async fn exit_plan_mode_dispatches_to_handler() {
    struct PlanHandler;
    #[async_trait]
    impl SessionHandler for PlanHandler {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            match event {
                HandlerEvent::ExitPlanMode { .. } => HandlerResponse::ExitPlanMode(
                    ExitPlanModeResult::new(true).with_selected_action("autopilot"),
                ),
                _ => HandlerResponse::Ok,
            }
        }
    }

    let (_session, mut server) = create_session_pair(Arc::new(PlanHandler)).await;
    server
        .send_request(
            400,
            "exitPlanMode.request",
            serde_json::json!({ "sessionId": server.session_id, "plan": "do the thing" }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["result"]["approved"], true);
    assert_eq!(response["result"]["selectedAction"], "autopilot");
}

#[tokio::test]
async fn approve_all_handler_approves_permission_and_plan() {
    let (_session, mut server) = create_session_pair(Arc::new(ApproveAllHandler)).await;

    server
        .send_request(
            500,
            "permission.request",
            serde_json::json!({
                "sessionId": server.session_id,
                "requestId": "perm-auto",
                "kind": "shell",
            }),
        )
        .await;
    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["result"]["result"]["kind"], "approved");

    server
        .send_request(
            501,
            "exitPlanMode.request",
            serde_json::json!({ "sessionId": server.session_id, "plan": "go" }),
        )
        .await;
    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["result"]["approved"], true);
}

#[tokio::test]
async fn session_event_notification_reaches_handler() {
    let (event_tx, mut event_rx) = mpsc::unbounded_channel::<SessionEventType>();

    struct EventCollector {
        tx: mpsc::UnboundedSender<SessionEventType>,
    }
    #[async_trait]
    impl SessionHandler for EventCollector {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            if let HandlerEvent::SessionEvent { event, .. } = event {
                self.tx.send(event.event_type.clone()).unwrap();
            }
            HandlerResponse::Ok
        }
    }

    let (_session, mut server) =
        create_session_pair(Arc::new(EventCollector { tx: event_tx })).await;
    server
        .send_event("session.idle", serde_json::json!({}))
        .await;

    let event_type = timeout(TIMEOUT, event_rx.recv()).await.unwrap().unwrap();
    assert_eq!(event_type, SessionEventType::SessionIdle);
}

#[tokio::test]
async fn router_routes_to_correct_session() {
    let (client, mut server_read, mut server_write) = make_client();
    let (tx1, mut rx1) = mpsc::unbounded_channel::<SessionEventType>();
    let (tx2, mut rx2) = mpsc::unbounded_channel::<SessionEventType>();

    struct Collector {
        tx: mpsc::UnboundedSender<SessionEventType>,
    }
    #[async_trait]
    impl SessionHandler for Collector {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            if let HandlerEvent::SessionEvent { event, .. } = event {
                self.tx.send(event.event_type.clone()).unwrap();
            }
            HandlerResponse::Ok
        }
    }

    // Create two sessions on the same client
    let mut sessions = Vec::new();
    for (tx, sid) in [(tx1, "s-one"), (tx2, "s-two")] {
        let h = tokio::spawn({
            let client = client.clone();
            async move {
                client
                    .create_session(
                        SessionConfig::default(),
                        Arc::new(Collector { tx }),
                        None,
                        None,
                    )
                    .await
                    .unwrap()
            }
        });
        let req = read_framed(&mut server_read).await;
        let id = req["id"].as_u64().unwrap();
        let resp = serde_json::json!({
            "jsonrpc": "2.0", "id": id,
            "result": { "sessionId": sid },
        });
        write_framed(&mut server_write, &serde_json::to_vec(&resp).unwrap()).await;
        sessions.push(timeout(TIMEOUT, h).await.unwrap().unwrap());
    }

    // Event for s-two should only reach rx2
    let notif = serde_json::json!({
        "jsonrpc": "2.0",
        "method": "session.event",
        "params": {
            "sessionId": "s-two",
            "event": { "id": "e1", "timestamp": "2025-01-01T00:00:00Z", "type": "assistant.message", "data": { "messageId": "m1", "content": "hi" } },
        },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&notif).unwrap()).await;
    assert_eq!(
        timeout(TIMEOUT, rx2.recv()).await.unwrap().unwrap(),
        SessionEventType::AssistantMessage
    );
    assert!(rx1.try_recv().is_err());

    // Event for s-one should only reach rx1
    let notif = serde_json::json!({
        "jsonrpc": "2.0",
        "method": "session.event",
        "params": {
            "sessionId": "s-one",
            "event": { "id": "e2", "timestamp": "2025-01-01T00:00:00Z", "type": "session.idle", "data": {} },
        },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&notif).unwrap()).await;
    assert_eq!(
        timeout(TIMEOUT, rx1.recv()).await.unwrap().unwrap(),
        SessionEventType::SessionIdle
    );
    assert!(rx2.try_recv().is_err());
}

#[tokio::test]
async fn send_and_wait_returns_last_assistant_message_on_idle() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .send_and_wait(MessageOptions::new("hello"), Some(Duration::from_secs(5)))
                .await
        }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.send");
    server
        .respond(&request, serde_json::json!({"messageId": "msg-1"}))
        .await;

    server
        .send_event(
            "assistant.message",
            serde_json::json!({ "messageId": "m1", "content": "Hello back!" }),
        )
        .await;
    server
        .send_event("session.idle", serde_json::json!({}))
        .await;

    let result = timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
    assert_eq!(result.message_id, "msg-1");
    let event = result
        .event
        .expect("should have captured assistant.message");
    assert_eq!(event.event_type, SessionEventType::AssistantMessage);
    if let copilot::types::SessionEventData::AssistantMessage(d) = &event.data {
        assert_eq!(d.content, "Hello back!");
    } else {
        panic!("expected AssistantMessage data");
    }
}

#[tokio::test]
async fn send_and_wait_returns_error_on_session_error() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .send_and_wait(MessageOptions::new("fail"), Some(Duration::from_secs(5)))
                .await
        }
    });

    let request = server.read_request().await;
    server.respond(&request, serde_json::json!({})).await;
    server
        .send_event(
            "session.error",
            serde_json::json!({ "errorType": "query", "message": "something went wrong" }),
        )
        .await;

    let err = timeout(TIMEOUT, handle)
        .await
        .unwrap()
        .unwrap()
        .unwrap_err();
    assert!(
        matches!(err, copilot::Error::Session(copilot::SessionError::AgentError(ref msg)) if msg.contains("something went wrong"))
    );
}

#[tokio::test]
async fn send_and_wait_times_out() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .send_and_wait(
                    MessageOptions::new("hello"),
                    Some(Duration::from_millis(100)),
                )
                .await
        }
    });

    let request = server.read_request().await;
    server.respond(&request, serde_json::json!({})).await;

    let err = timeout(Duration::from_secs(2), handle)
        .await
        .unwrap()
        .unwrap()
        .unwrap_err();
    assert!(matches!(
        err,
        copilot::Error::Session(copilot::SessionError::Timeout(_))
    ));
}

#[tokio::test]
async fn elicitation_requested_dispatches_to_handler_and_responds() {
    use copilot::types::ElicitationResult;

    struct ElicitHandler;
    #[async_trait]
    impl SessionHandler for ElicitHandler {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            match event {
                HandlerEvent::ElicitationRequest { request, .. } => {
                    assert_eq!(request.message, "Enter your name");
                    HandlerResponse::Elicitation(ElicitationResult {
                        action: "accept".to_string(),
                        content: Some(serde_json::json!({ "name": "Alice" })),
                    })
                }
                _ => HandlerResponse::Ok,
            }
        }
    }

    let (_session, mut server) = create_session_pair(Arc::new(ElicitHandler)).await;

    // CLI broadcasts elicitation.requested as a session event notification
    server
        .send_event(
            "elicitation.requested",
            serde_json::json!({
                "requestId": "elicit-1",
                "message": "Enter your name",
                "requestedSchema": {
                    "type": "object",
                    "properties": { "name": { "type": "string" } },
                    "required": ["name"]
                },
                "mode": "form",
            }),
        )
        .await;

    // The SDK should call session.ui.handlePendingElicitation RPC
    let rpc_call = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(rpc_call["method"], "session.ui.handlePendingElicitation");
    assert_eq!(rpc_call["params"]["requestId"], "elicit-1");
    assert_eq!(rpc_call["params"]["result"]["action"], "accept");
    assert_eq!(rpc_call["params"]["result"]["content"]["name"], "Alice");
}

#[tokio::test]
async fn elicitation_requested_cancels_on_handler_error() {
    struct FailHandler;
    #[async_trait]
    impl SessionHandler for FailHandler {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            match event {
                // Return Ok instead of Elicitation — SDK should treat as cancel
                HandlerEvent::ElicitationRequest { .. } => HandlerResponse::Ok,
                _ => HandlerResponse::Ok,
            }
        }
    }

    let (_session, mut server) = create_session_pair(Arc::new(FailHandler)).await;
    server
        .send_event(
            "elicitation.requested",
            serde_json::json!({
                "requestId": "elicit-2",
                "message": "Pick something",
            }),
        )
        .await;

    let rpc_call = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(rpc_call["method"], "session.ui.handlePendingElicitation");
    assert_eq!(rpc_call["params"]["result"]["action"], "cancel");
}

#[tokio::test]
async fn capabilities_captured_from_create_response() {
    let (client, mut server_read, mut server_write) = make_client();

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default(), Arc::new(NoopHandler), None, None)
                .await
                .unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": {
            "sessionId": "cap-session",
            "capabilities": {
                "ui": { "elicitation": true }
            }
        },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let session = timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
    let caps = session.capabilities();
    assert_eq!(caps.ui.as_ref().unwrap().elicitation, Some(true));
}

#[tokio::test]
async fn capabilities_changed_event_updates_session() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;

    // Initially no capabilities (create_session_pair doesn't send them)
    assert!(session.capabilities().ui.is_none());

    // CLI sends capabilities.changed event
    server
        .send_event(
            "capabilities.changed",
            serde_json::json!({
                "ui": { "elicitation": true }
            }),
        )
        .await;

    // Poll until the event loop processes the notification
    let caps = timeout(TIMEOUT, async {
        loop {
            let caps = session.capabilities();
            if caps.ui.is_some() {
                return caps;
            }
            tokio::time::sleep(Duration::from_millis(5)).await;
        }
    })
    .await
    .expect("capabilities should update within timeout");

    assert_eq!(caps.ui.as_ref().unwrap().elicitation, Some(true));
}

#[tokio::test]
async fn request_elicitation_sent_in_create_params() {
    let (client, mut server_read, mut server_write) = make_client();

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(
                    SessionConfig {
                        request_elicitation: Some(true),
                        ..Default::default()
                    },
                    Arc::new(NoopHandler),
                    None,
                    None,
                )
                .await
                .unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.create");
    assert_eq!(request["params"]["requestElicitation"], true);

    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": { "sessionId": "s-elicit" },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;
    let _ = timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
}

#[tokio::test]
async fn elicitation_methods_fail_without_capability() {
    let (session, _server) = create_session_pair(Arc::new(NoopHandler)).await;

    // Session created without capabilities — elicitation should fail
    let err = session
        .elicitation("test", serde_json::json!({}))
        .await
        .unwrap_err();
    assert!(matches!(
        err,
        copilot::Error::Session(copilot::SessionError::ElicitationNotSupported)
    ));

    let err = session.confirm("ok?").await.unwrap_err();
    assert!(matches!(
        err,
        copilot::Error::Session(copilot::SessionError::ElicitationNotSupported)
    ));
}

async fn create_session_pair_with_hooks(
    handler: Arc<dyn SessionHandler>,
    hooks: Arc<dyn copilot::hooks::SessionHooks>,
) -> (copilot::session::Session, FakeServer) {
    let (client, server_read, server_write) = make_client();
    let session_id = format!("test-session-{}", rand_id());

    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: session_id.clone(),
    };

    let create_handle = tokio::spawn({
        let client = client.clone();
        let handler = handler.clone();
        async move {
            client
                .create_session(SessionConfig::default(), handler, Some(hooks), None)
                .await
                .unwrap()
        }
    });

    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
    // Verify hooks: true is auto-set in the config
    assert_eq!(create_req["params"]["hooks"], true);
    server
        .respond(
            &create_req,
            serde_json::json!({
                "sessionId": session_id,
                "workspacePath": "/tmp/workspace"
            }),
        )
        .await;

    let session = timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
    (session, server)
}

#[tokio::test]
async fn hooks_invoke_dispatches_to_session_hooks() {
    use copilot::hooks::{HookEvent, HookOutput, PreToolUseOutput, SessionHooks};

    struct PolicyHooks;
    #[async_trait]
    impl SessionHooks for PolicyHooks {
        async fn on_hook(&self, event: HookEvent) -> HookOutput {
            match event {
                HookEvent::PreToolUse { input, .. } => {
                    if input.tool_name == "rm" {
                        HookOutput::PreToolUse(PreToolUseOutput {
                            permission_decision: Some("deny".to_string()),
                            permission_decision_reason: Some("destructive".to_string()),
                            ..Default::default()
                        })
                    } else {
                        HookOutput::None
                    }
                }
                _ => HookOutput::None,
            }
        }
    }

    let (_session, mut server) =
        create_session_pair_with_hooks(Arc::new(NoopHandler), Arc::new(PolicyHooks)).await;

    // Send a hooks.invoke request for a denied tool
    server
        .send_request(
            300,
            "hooks.invoke",
            serde_json::json!({
                "sessionId": server.session_id,
                "hookType": "preToolUse",
                "input": {
                    "timestamp": 1234567890,
                    "cwd": "/tmp",
                    "toolName": "rm",
                    "toolArgs": { "path": "/" }
                }
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 300);
    assert_eq!(response["result"]["output"]["permissionDecision"], "deny");
    assert_eq!(
        response["result"]["output"]["permissionDecisionReason"],
        "destructive"
    );
}

#[tokio::test]
async fn hooks_invoke_returns_empty_for_unregistered_hook() {
    use copilot::hooks::SessionHooks;

    struct EmptyHooks;
    #[async_trait]
    impl SessionHooks for EmptyHooks {}

    let (_session, mut server) =
        create_session_pair_with_hooks(Arc::new(NoopHandler), Arc::new(EmptyHooks)).await;

    server
        .send_request(
            301,
            "hooks.invoke",
            serde_json::json!({
                "sessionId": server.session_id,
                "hookType": "sessionEnd",
                "input": {
                    "timestamp": 1234567890,
                    "cwd": "/tmp",
                    "reason": "complete"
                }
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 301);
    assert_eq!(response["result"]["output"], serde_json::json!({}));
}

async fn create_session_pair_with_transforms(
    handler: Arc<dyn SessionHandler>,
    transforms: Arc<dyn copilot::transforms::SystemMessageTransform>,
) -> (copilot::session::Session, FakeServer) {
    let (client, server_read, server_write) = make_client();
    let session_id = format!("test-session-{}", rand_id());

    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: session_id.clone(),
    };

    let create_handle = tokio::spawn({
        let client = client.clone();
        let handler = handler.clone();
        async move {
            client
                .create_session(SessionConfig::default(), handler, None, Some(transforms))
                .await
                .unwrap()
        }
    });

    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
    // Verify transforms inject customize mode and section overrides
    assert_eq!(create_req["params"]["systemMessage"]["mode"], "customize");
    server
        .respond(
            &create_req,
            serde_json::json!({
                "sessionId": session_id,
                "workspacePath": "/tmp/workspace"
            }),
        )
        .await;

    let session = timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
    (session, server)
}

#[tokio::test]
async fn system_message_transform_dispatches_to_transform() {
    use copilot::transforms::{SystemMessageTransform, TransformContext};

    struct AppendTransform;
    #[async_trait]
    impl SystemMessageTransform for AppendTransform {
        fn section_ids(&self) -> Vec<String> {
            vec!["instructions".to_string()]
        }

        async fn transform_section(
            &self,
            _section_id: &str,
            content: &str,
            _ctx: TransformContext,
        ) -> Option<String> {
            Some(format!("{content}\nAlways be concise."))
        }
    }

    let (_session, mut server) =
        create_session_pair_with_transforms(Arc::new(NoopHandler), Arc::new(AppendTransform)).await;

    server
        .send_request(
            400,
            "systemMessage.transform",
            serde_json::json!({
                "sessionId": server.session_id,
                "sections": {
                    "instructions": { "content": "You are helpful." }
                }
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 400);
    assert_eq!(
        response["result"]["sections"]["instructions"]["content"],
        "You are helpful.\nAlways be concise."
    );
}

#[tokio::test]
async fn system_message_transform_returns_error_for_missing_sections() {
    use copilot::transforms::{SystemMessageTransform, TransformContext};

    struct DummyTransform;
    #[async_trait]
    impl SystemMessageTransform for DummyTransform {
        fn section_ids(&self) -> Vec<String> {
            vec!["instructions".to_string()]
        }

        async fn transform_section(
            &self,
            _section_id: &str,
            _content: &str,
            _ctx: TransformContext,
        ) -> Option<String> {
            None
        }
    }

    let (_session, mut server) =
        create_session_pair_with_transforms(Arc::new(NoopHandler), Arc::new(DummyTransform)).await;

    // Send request with no sections parameter
    server
        .send_request(
            401,
            "systemMessage.transform",
            serde_json::json!({
                "sessionId": server.session_id,
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 401);
    assert_eq!(response["error"]["code"], -32602);
}
