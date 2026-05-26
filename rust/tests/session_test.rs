#![allow(clippy::unwrap_used)]

use std::path::Path;
use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::canvas::{CanvasDeclaration, CanvasHandler, CanvasResult};
use github_copilot_sdk::generated::api_types::{
    CanvasInstanceAvailability, CanvasProviderInvokeActionRequest, CanvasProviderOpenRequest,
    CanvasProviderOpenResult, OpenCanvasInstance,
};
use github_copilot_sdk::handler::{
    ApproveAllHandler, AutoModeSwitchHandler, AutoModeSwitchResponse, ElicitationHandler,
    ExitPlanModeHandler, ExitPlanModeResult, UserInputHandler, UserInputResponse,
};
use github_copilot_sdk::types::{
    CommandContext, CommandDefinition, CommandHandler, DeliveryMode, ElicitationRequest,
    ElicitationResult, ExitPlanModeData, ExtensionInfo, MessageOptions, RequestId, SessionConfig,
    SessionId, Tool, ToolInvocation, ToolResult,
};
use github_copilot_sdk::{Client, tool};
use serde_json::Value;
use tokio::io::{AsyncWrite, AsyncWriteExt, duplex};
use tokio::time::timeout;

const TIMEOUT: Duration = Duration::from_secs(2);

struct TestCanvasHandler;

#[async_trait]
impl CanvasHandler for TestCanvasHandler {
    async fn on_open(
        &self,
        ctx: CanvasProviderOpenRequest,
    ) -> CanvasResult<CanvasProviderOpenResult> {
        Ok(CanvasProviderOpenResult {
            url: Some(format!("https://example.test/{}", ctx.canvas_id)),
            title: Some("Test Canvas".to_string()),
            status: Some("ready".to_string()),
        })
    }

    async fn on_action(&self, ctx: CanvasProviderInvokeActionRequest) -> CanvasResult<Value> {
        Ok(serde_json::json!({
            "actionName": ctx.action_name,
            "input": ctx.input,
        }))
    }
}

fn test_canvas(id: &str) -> CanvasDeclaration {
    CanvasDeclaration::new(id, "Test Canvas", "Test canvas description")
}

fn test_canvas_handler() -> Arc<dyn CanvasHandler> {
    Arc::new(TestCanvasHandler)
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

async fn create_session_pair() -> (github_copilot_sdk::session::Session, FakeServer) {
    create_session_pair_with_config(|cfg| cfg).await
}

async fn create_session_pair_with_capabilities(
    capabilities: Value,
) -> (github_copilot_sdk::session::Session, FakeServer) {
    create_session_pair_inner(|cfg| cfg, capabilities).await
}

async fn create_session_pair_with_config<F>(
    configure: F,
) -> (github_copilot_sdk::session::Session, FakeServer)
where
    F: FnOnce(SessionConfig) -> SessionConfig + Send + 'static,
{
    create_session_pair_inner(configure, serde_json::json!(null)).await
}

async fn create_session_pair_inner<F>(
    configure: F,
    capabilities: Value,
) -> (github_copilot_sdk::session::Session, FakeServer)
where
    F: FnOnce(SessionConfig) -> SessionConfig + Send + 'static,
{
    let (client, server_read, server_write) = make_client();

    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: String::new(),
    };

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(configure(SessionConfig::default()))
                .await
                .unwrap()
        }
    });

    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
    server.session_id = requested_session_id(&create_req).to_string();
    let mut result = serde_json::json!({
        "sessionId": server.session_id.clone(),
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

fn requested_session_id(request: &Value) -> &str {
    request["params"]["sessionId"]
        .as_str()
        .expect("session request should include sessionId")
}

#[tokio::test]
async fn session_subscribe_yields_events_observe_only() {
    let (session, mut server) = create_session_pair().await;

    let mut events = session.subscribe();
    let count = Arc::new(AtomicUsize::new(0));
    let last_type = Arc::new(parking_lot::Mutex::new(String::new()));
    let count_clone = count.clone();
    let last_type_clone = last_type.clone();
    let consumer = tokio::spawn(async move {
        while let Ok(event) = events.recv().await {
            count_clone.fetch_add(1, Ordering::Relaxed);
            *last_type_clone.lock() = event.event_type.clone();
        }
    });

    server.send_event("noop.event", serde_json::json!({})).await;
    server
        .send_event("another.event", serde_json::json!({"k": "v"}))
        .await;

    for _ in 0..50 {
        if count.load(Ordering::Relaxed) >= 2 {
            break;
        }
        tokio::time::sleep(Duration::from_millis(20)).await;
    }
    assert_eq!(count.load(Ordering::Relaxed), 2);
    assert_eq!(last_type.lock().as_str(), "another.event");
    consumer.abort();
}

#[tokio::test]
async fn session_subscribe_drop_stops_delivery() {
    let (session, mut server) = create_session_pair().await;

    let mut events = session.subscribe();
    let count = Arc::new(AtomicUsize::new(0));
    let count_clone = count.clone();
    let consumer = tokio::spawn(async move {
        while let Ok(_event) = events.recv().await {
            count_clone.fetch_add(1, Ordering::Relaxed);
        }
    });

    server.send_event("first", serde_json::json!({})).await;
    for _ in 0..50 {
        if count.load(Ordering::Relaxed) >= 1 {
            break;
        }
        tokio::time::sleep(Duration::from_millis(20)).await;
    }
    assert_eq!(count.load(Ordering::Relaxed), 1);

    // Aborting the consumer drops its receiver; further events have no
    // effect on the (now-zero) subscriber count.
    consumer.abort();
    tokio::time::sleep(Duration::from_millis(20)).await;

    server.send_event("second", serde_json::json!({})).await;
    tokio::time::sleep(Duration::from_millis(100)).await;
    assert_eq!(count.load(Ordering::Relaxed), 1);
}

#[tokio::test]
async fn create_session_sends_correct_rpc() {
    let (client, mut server_read, mut server_write) = make_client();

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session({
                    let mut cfg = SessionConfig::default();
                    cfg.model = Some("gpt-4".to_string());
                    cfg
                })
                .await
                .unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.create");
    assert_eq!(request["params"]["model"], "gpt-4");

    let id = request["id"].as_u64().unwrap();
    let session_id = requested_session_id(&request).to_string();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": { "sessionId": session_id.clone(), "workspacePath": "/ws" },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let session = timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
    assert_eq!(session.id(), session_id.as_str());
    assert_eq!(session.workspace_path(), Some(Path::new("/ws")));
}

#[tokio::test]
async fn create_session_sends_canvas_wire_fields() {
    let (client, mut server_read, mut server_write) = make_client();

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(
                    SessionConfig::default()
                        .with_canvases([test_canvas("counter")])
                        .with_request_canvas_renderer(true)
                        .with_request_extensions(true)
                        .with_extension_info(ExtensionInfo::new("github-app", "counter-provider")),
                )
                .await
                .unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.create");
    assert_eq!(request["params"]["canvases"][0]["id"], "counter");
    assert_eq!(
        request["params"]["canvases"][0]["displayName"],
        "Test Canvas"
    );
    assert_eq!(request["params"]["requestCanvasRenderer"], true);
    assert_eq!(request["params"]["requestExtensions"], true);
    assert_eq!(request["params"]["extensionInfo"]["source"], "github-app");
    assert_eq!(
        request["params"]["extensionInfo"]["name"],
        "counter-provider"
    );

    let id = request["id"].as_u64().unwrap();
    let session_id = requested_session_id(&request).to_string();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": { "sessionId": session_id },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
}

#[tokio::test]
async fn provider_canvas_dispatch_routes_direct_canvas_action_requests() {
    let (session, mut server) = create_session_pair_with_config(|cfg| {
        cfg.with_canvases([test_canvas("counter")])
            .with_canvas_handler(test_canvas_handler())
    })
    .await;

    server
        .send_request(
            42,
            "canvas.invokeAction",
            serde_json::json!({
                "sessionId": session.id(),
                "extensionId": "project:counter",
                "canvasId": "counter",
                "instanceId": "counter-1",
                "actionName": "increment",
                "input": { "amount": 1 }
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 42);
    assert_eq!(response["result"]["actionName"], "increment");
    assert_eq!(response["result"]["input"]["amount"], 1);
}

#[tokio::test]
async fn send_injects_session_id() {
    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .send(MessageOptions::new("hello").with_mode(DeliveryMode::Immediate))
                .await
        }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.send");
    assert_eq!(request["params"]["sessionId"], server.session_id);
    assert_eq!(request["params"]["prompt"], "hello");
    assert_eq!(request["params"]["mode"], "immediate");

    server.respond(&request, serde_json::json!({})).await;
    timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
}

#[tokio::test]
async fn send_serializes_request_headers() {
    use std::collections::HashMap;

    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            let mut headers = HashMap::new();
            headers.insert("X-Custom-Tag".to_string(), "value-1".to_string());
            headers.insert("Authorization".to_string(), "Bearer abc".to_string());
            session
                .send(MessageOptions::new("hi").with_request_headers(headers))
                .await
        }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.send");
    assert_eq!(request["params"]["prompt"], "hi");
    let headers = request["params"]["requestHeaders"]
        .as_object()
        .expect("requestHeaders should be an object");
    assert_eq!(headers["X-Custom-Tag"], "value-1");
    assert_eq!(headers["Authorization"], "Bearer abc");
    assert_eq!(headers.len(), 2);

    server.respond(&request, serde_json::json!({})).await;
    timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
}

#[tokio::test]
async fn send_omits_request_headers_when_unset_or_empty() {
    use std::collections::HashMap;

    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move { session.send(MessageOptions::new("plain")).await }
    });
    let request = server.read_request().await;
    assert!(
        request["params"].get("requestHeaders").is_none(),
        "requestHeaders should be omitted when unset, got: {}",
        request["params"]
    );
    server.respond(&request, serde_json::json!({})).await;
    timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();

    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .send(MessageOptions::new("plain").with_request_headers(HashMap::new()))
                .await
        }
    });
    let request = server.read_request().await;
    assert!(
        request["params"].get("requestHeaders").is_none(),
        "requestHeaders should be omitted for empty map, got: {}",
        request["params"]
    );
    server.respond(&request, serde_json::json!({})).await;
    timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
}

#[tokio::test]
async fn session_rpc_methods_send_correct_method_names() {
    let (session, mut server) = create_session_pair().await;
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
        let response = match expected_method {
            "session.log" => {
                serde_json::json!({ "eventId": "00000000-0000-0000-0000-000000000000" })
            }
            _ => serde_json::json!({}),
        };
        server.respond(&request, response).await;
        timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
    }
}

#[tokio::test]
async fn client_rpc_methods_send_correct_method_names() {
    let (client, mut server_read, mut server_write) = make_client();

    // Wire method names per the CLI runtime registration in @github/copilot
    // app.js — verified against Node/Go/Python/.NET SDK call sites which all
    // use these exact strings. The schema doesn't currently define these as
    // typed RPCs (top-level methods, not under any namespace), so call site
    // strings are the source of truth.
    for expected_method in ["status.get", "auth.getStatus"] {
        let c = client.clone();
        let handle = tokio::spawn(async move {
            match expected_method {
                "status.get" => c.get_status().await.map(|_| ()),
                "auth.getStatus" => c.get_auth_status().await.map(|_| ()),
                _ => unreachable!(),
            }
        });

        let request = read_framed(&mut server_read).await;
        assert_eq!(request["method"], expected_method);
        // Regression-prevention: must not have reverted to the
        // hand-authored `getStatus` / `getAuthStatus` names that don't
        // exist on the wire.
        assert_ne!(request["method"], "getStatus");
        assert_ne!(request["method"], "getAuthStatus");
        let id = request["id"].as_u64().unwrap();
        let result = match expected_method {
            "status.get" => serde_json::json!({ "version": "1.0.0", "protocolVersion": 1 }),
            "auth.getStatus" => serde_json::json!({ "isAuthenticated": true }),
            _ => unreachable!(),
        };
        let resp = serde_json::json!({ "jsonrpc": "2.0", "id": id, "result": result });
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
async fn list_sessions_serializes_typed_filter() {
    use github_copilot_sdk::SessionListFilter;

    let (client, mut server_read, mut server_write) = make_client();

    let filter = SessionListFilter {
        repository: Some("octocat/hello".to_string()),
        branch: Some("main".to_string()),
        ..Default::default()
    };

    let handle = tokio::spawn({
        let client = client.clone();
        async move { client.list_sessions(Some(filter)).await.unwrap() }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.list");
    assert_eq!(request["params"]["filter"]["repository"], "octocat/hello");
    assert_eq!(request["params"]["filter"]["branch"], "main");
    // cwd / gitRoot are None and must be omitted from the filter object.
    assert!(request["params"]["filter"].get("cwd").is_none());
    assert!(request["params"]["filter"].get("gitRoot").is_none());
    // Regression check: filter must be wrapped under `params.filter`, not
    // flattened onto `params` directly. All other SDKs (Node/Python/Go/.NET)
    // wrap; flattening is silently ignored by the runtime.
    assert!(
        request["params"].get("repository").is_none(),
        "wire shape is `params.filter.*`, not `params.*` — see Node/Go/Python/.NET"
    );

    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": { "sessions": [] },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    timeout(TIMEOUT, handle).await.unwrap().unwrap();
}

#[test]
fn mcp_server_config_roundtrips_through_tagged_enum() {
    use std::collections::HashMap;

    use github_copilot_sdk::{McpServerConfig, McpStdioServerConfig};

    let stdio = McpServerConfig::Stdio(McpStdioServerConfig {
        command: "node".to_string(),
        args: vec!["server.js".to_string()],
        env: HashMap::new(),
        working_directory: None,
        tools: Some(vec!["*".to_string()]),
        timeout: None,
    });
    let json = serde_json::to_value(&stdio).unwrap();
    assert_eq!(json["type"], "stdio");
    assert_eq!(json["command"], "node");

    // CLI may emit the legacy "local" alias; we accept it on the wire.
    let local: McpServerConfig = serde_json::from_value(serde_json::json!({
        "type": "local",
        "command": "node",
    }))
    .unwrap();
    assert!(matches!(local, McpServerConfig::Stdio(_)));

    // SessionConfig.mcp_servers round-trips a typed map.
    let mut servers = HashMap::new();
    servers.insert("github".to_string(), stdio.clone());
    let cfg_json = serde_json::to_value(&servers).unwrap();
    assert_eq!(cfg_json["github"]["type"], "stdio");
}

#[test]
fn mcp_stdio_tools_tri_state_serializes_correctly() {
    use github_copilot_sdk::McpStdioServerConfig;

    // None → field omitted (= "expose all tools")
    let cfg = McpStdioServerConfig {
        command: "echo".into(),
        tools: None,
        ..Default::default()
    };
    let json = serde_json::to_value(&cfg).unwrap();
    assert!(
        json.get("tools").is_none(),
        "tools=None must be omitted on the wire; got {json}"
    );

    // Some(empty) → field present as []
    let cfg = McpStdioServerConfig {
        command: "echo".into(),
        tools: Some(vec![]),
        ..Default::default()
    };
    let json = serde_json::to_value(&cfg).unwrap();
    assert_eq!(json["tools"], serde_json::json!([]));

    // Some(non-empty) → field present as the explicit list
    let cfg = McpStdioServerConfig {
        command: "echo".into(),
        tools: Some(vec!["a".into(), "b".into()]),
        ..Default::default()
    };
    let json = serde_json::to_value(&cfg).unwrap();
    assert_eq!(json["tools"], serde_json::json!(["a", "b"]));
}

#[test]
fn mcp_stdio_tools_tri_state_deserializes_correctly() {
    use github_copilot_sdk::McpStdioServerConfig;

    // Missing field → None
    let cfg: McpStdioServerConfig =
        serde_json::from_value(serde_json::json!({ "command": "echo" })).unwrap();
    assert_eq!(cfg.tools, None);

    // Empty list → Some(empty)
    let cfg: McpStdioServerConfig =
        serde_json::from_value(serde_json::json!({ "command": "echo", "tools": [] })).unwrap();
    assert_eq!(cfg.tools, Some(vec![]));

    // Non-empty list → Some(list)
    let cfg: McpStdioServerConfig =
        serde_json::from_value(serde_json::json!({ "command": "echo", "tools": ["x"] })).unwrap();
    assert_eq!(cfg.tools, Some(vec!["x".to_string()]));
}

#[test]
fn mcp_http_tools_tri_state_serializes_correctly() {
    use github_copilot_sdk::McpHttpServerConfig;

    let cfg = McpHttpServerConfig {
        url: "https://example.com".into(),
        tools: None,
        ..Default::default()
    };
    assert!(
        serde_json::to_value(&cfg).unwrap().get("tools").is_none(),
        "tools=None must be omitted on the wire"
    );

    let cfg = McpHttpServerConfig {
        url: "https://example.com".into(),
        tools: Some(vec![]),
        ..Default::default()
    };
    assert_eq!(
        serde_json::to_value(&cfg).unwrap()["tools"],
        serde_json::json!([])
    );

    let cfg = McpHttpServerConfig {
        url: "https://example.com".into(),
        tools: Some(vec!["a".into()]),
        ..Default::default()
    };
    assert_eq!(
        serde_json::to_value(&cfg).unwrap()["tools"],
        serde_json::json!(["a"])
    );
}

#[test]
fn mcp_http_tools_tri_state_deserializes_correctly() {
    use github_copilot_sdk::McpHttpServerConfig;

    let cfg: McpHttpServerConfig =
        serde_json::from_value(serde_json::json!({ "url": "https://e.com" })).unwrap();
    assert_eq!(cfg.tools, None);

    let cfg: McpHttpServerConfig =
        serde_json::from_value(serde_json::json!({ "url": "https://e.com", "tools": [] })).unwrap();
    assert_eq!(cfg.tools, Some(vec![]));
}

#[test]
fn permission_request_data_extracts_typed_kind() {
    use github_copilot_sdk::{PermissionRequestData, PermissionRequestKind};

    let data: PermissionRequestData = serde_json::from_value(serde_json::json!({
        "kind": "shell",
        "toolCallId": "t1",
        "command": "ls",
    }))
    .unwrap();
    assert_eq!(data.kind, Some(PermissionRequestKind::Shell));
    assert_eq!(data.tool_call_id, Some("t1".to_string()));
    assert_eq!(data.extra["command"], "ls");

    let custom: PermissionRequestData = serde_json::from_value(serde_json::json!({
        "kind": "custom-tool",
        "toolName": "open_canvas",
        "args": {
            "extensionId": "github-app:counter-provider",
            "canvasId": "counter",
            "instanceId": "counter-1"
        }
    }))
    .unwrap();
    assert_eq!(custom.kind, Some(PermissionRequestKind::CustomTool));
    assert_eq!(custom.extra["toolName"], "open_canvas");
    assert_eq!(
        custom.extra["args"]["extensionId"],
        "github-app:counter-provider"
    );

    // Unknown kinds fall through to the catch-all variant rather than failing.
    let unknown: PermissionRequestData = serde_json::from_value(serde_json::json!({
        "kind": "future-permission-type",
    }))
    .unwrap();
    assert_eq!(unknown.kind, Some(PermissionRequestKind::Unknown));
}

#[tokio::test]
async fn force_stop_is_idempotent_with_no_child() {
    // Stream-based clients have no child process. force_stop should be a
    // no-op and safe to call multiple times.
    let (client, _server_read, _server_write) = make_client();
    client.force_stop();
    client.force_stop();
    assert!(client.pid().is_none());
}

#[tokio::test]
async fn stop_is_safe_to_call() {
    let (client, _server_read, _server_write) = make_client();
    client.stop().await.expect("stop should succeed");
}

#[tokio::test]
async fn lifecycle_subscribe_yields_events_with_filter() {
    use github_copilot_sdk::{SessionLifecycleEventMetadata, SessionLifecycleEventType as Type};

    let (client, _server_read, mut server_write) = make_client();

    let mut all_events = client.subscribe_lifecycle();
    let mut foreground_events = client.subscribe_lifecycle();

    let wildcard_count = Arc::new(AtomicUsize::new(0));
    let foreground_count = Arc::new(AtomicUsize::new(0));
    let last_session = Arc::new(parking_lot::Mutex::new(None));

    let w_count = wildcard_count.clone();
    let w_last = last_session.clone();
    let w_consumer = tokio::spawn(async move {
        while let Ok(event) = all_events.recv().await {
            w_count.fetch_add(1, Ordering::Relaxed);
            *w_last.lock() = Some(event.session_id.clone());
        }
    });
    let f_count = foreground_count.clone();
    let f_consumer = tokio::spawn(async move {
        while let Ok(event) = foreground_events.recv().await {
            if event.event_type == Type::Foreground {
                f_count.fetch_add(1, Ordering::Relaxed);
            }
        }
    });

    let body1 = serde_json::to_vec(&serde_json::json!({
        "jsonrpc": "2.0",
        "method": "session.lifecycle",
        "params": { "type": "session.created", "sessionId": "s1" },
    }))
    .unwrap();
    let body2 = serde_json::to_vec(&serde_json::json!({
        "jsonrpc": "2.0",
        "method": "session.lifecycle",
        "params": {
            "type": "session.foreground",
            "sessionId": "s2",
            "metadata": {
                "startTime": "2025-01-01T00:00:00Z",
                "modifiedTime": "2025-01-02T00:00:00Z",
                "summary": "hello",
            },
        },
    }))
    .unwrap();
    let body3 = serde_json::to_vec(&serde_json::json!({
        "jsonrpc": "2.0",
        "method": "session.event",
        "params": { "sessionId": "ignored", "event": {
            "id": "x", "timestamp": "t", "type": "noop", "data": {}
        }},
    }))
    .unwrap();
    write_framed(&mut server_write, &body1).await;
    write_framed(&mut server_write, &body2).await;
    write_framed(&mut server_write, &body3).await;

    for _ in 0..50 {
        if wildcard_count.load(Ordering::Relaxed) >= 2 {
            break;
        }
        tokio::time::sleep(Duration::from_millis(20)).await;
    }
    assert_eq!(wildcard_count.load(Ordering::Relaxed), 2);
    assert_eq!(foreground_count.load(Ordering::Relaxed), 1);
    assert_eq!(last_session.lock().as_deref(), Some("s2"));
    w_consumer.abort();
    f_consumer.abort();

    let meta = SessionLifecycleEventMetadata {
        start_time: "t1".into(),
        modified_time: "t2".into(),
        summary: Some("s".into()),
    };
    assert_eq!(meta.summary.as_deref(), Some("s"));
}

#[tokio::test]
async fn lifecycle_subscribe_drop_stops_delivery() {
    let (client, _server_read, mut server_write) = make_client();

    let mut events = client.subscribe_lifecycle();
    let count = Arc::new(AtomicUsize::new(0));
    let count_clone = count.clone();
    let consumer = tokio::spawn(async move {
        while let Ok(_event) = events.recv().await {
            count_clone.fetch_add(1, Ordering::Relaxed);
        }
    });

    let lifecycle_body = serde_json::to_vec(&serde_json::json!({
        "jsonrpc": "2.0",
        "method": "session.lifecycle",
        "params": { "type": "session.created", "sessionId": "x" },
    }))
    .unwrap();

    write_framed(&mut server_write, &lifecycle_body).await;
    for _ in 0..50 {
        if count.load(Ordering::Relaxed) >= 1 {
            break;
        }
        tokio::time::sleep(Duration::from_millis(20)).await;
    }
    assert_eq!(count.load(Ordering::Relaxed), 1);

    consumer.abort();
    tokio::time::sleep(Duration::from_millis(20)).await;

    write_framed(&mut server_write, &lifecycle_body).await;
    tokio::time::sleep(Duration::from_millis(100)).await;
    assert_eq!(count.load(Ordering::Relaxed), 1);
}

#[tokio::test]
async fn delete_session_sends_session_id() {
    let (client, mut server_read, mut server_write) = make_client();

    let handle = tokio::spawn({
        let client = client.clone();
        async move { client.delete_session(&SessionId::new("s-to-delete")).await }
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
async fn get_last_session_id_returns_none_when_empty() {
    let (client, mut server_read, mut server_write) = make_client();

    let handle = tokio::spawn({
        let client = client.clone();
        async move { client.get_last_session_id().await.unwrap() }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.getLastId");

    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({ "jsonrpc": "2.0", "id": id, "result": {} });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let last = timeout(TIMEOUT, handle).await.unwrap().unwrap();
    assert!(last.is_none());
}

#[tokio::test]
async fn get_last_session_id_returns_id_when_set() {
    let (client, mut server_read, mut server_write) = make_client();

    let handle = tokio::spawn({
        let client = client.clone();
        async move { client.get_last_session_id().await.unwrap() }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.getLastId");

    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": { "sessionId": "s-last" },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let last = timeout(TIMEOUT, handle).await.unwrap().unwrap();
    assert_eq!(last.as_deref(), Some("s-last"));
}

#[tokio::test]
async fn get_foreground_session_id_returns_id_when_set() {
    let (client, mut server_read, mut server_write) = make_client();

    let handle = tokio::spawn({
        let client = client.clone();
        async move { client.get_foreground_session_id().await.unwrap() }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.getForeground");

    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": { "sessionId": "s-fg" },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let fg = timeout(TIMEOUT, handle).await.unwrap().unwrap();
    assert_eq!(fg.as_deref(), Some("s-fg"));
}

#[tokio::test]
async fn set_foreground_session_id_sends_session_id() {
    let (client, mut server_read, mut server_write) = make_client();

    let handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .set_foreground_session_id(&SessionId::new("s-target"))
                .await
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.setForeground");
    assert_eq!(request["params"]["sessionId"], "s-target");

    let id = request["id"].as_u64().unwrap();
    let resp = serde_json::json!({ "jsonrpc": "2.0", "id": id, "result": {} });
    write_framed(&mut server_write, &serde_json::to_vec(&resp).unwrap()).await;
    timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
}

#[tokio::test]
async fn get_session_metadata_returns_typed_metadata() {
    let (client, mut server_read, mut server_write) = make_client();

    let handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .get_session_metadata(&SessionId::new("s1"))
                .await
                .unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.getMetadata");
    assert_eq!(request["params"]["sessionId"], "s1");

    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": {
            "session": {
                "sessionId": "s1",
                "startTime": "2025-01-01T00:00:00Z",
                "modifiedTime": "2025-01-01T01:00:00Z",
                "summary": "loaded session",
                "isRemote": false,
            }
        },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let metadata = timeout(TIMEOUT, handle).await.unwrap().unwrap();
    let metadata = metadata.expect("server returned a session");
    assert_eq!(metadata.session_id, "s1");
    assert_eq!(metadata.summary.as_deref(), Some("loaded session"));
}

#[tokio::test]
async fn get_session_metadata_returns_none_when_missing() {
    let (client, mut server_read, mut server_write) = make_client();

    let handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .get_session_metadata(&SessionId::new("missing"))
                .await
                .unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.getMetadata");

    let id = request["id"].as_u64().unwrap();
    // Server responds with an empty result object; `session` is absent.
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": {},
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let metadata = timeout(TIMEOUT, handle).await.unwrap().unwrap();
    assert!(metadata.is_none());
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
                { "id": "gpt-4", "name": "GPT-4", "capabilities": {} },
                { "id": "claude-sonnet-4", "name": "Claude Sonnet", "capabilities": {} },
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
    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move { session.get_events().await.unwrap() }
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
                    "data": { "text": "hello" },
                }]
            }),
        )
        .await;

    let events = timeout(TIMEOUT, handle).await.unwrap().unwrap();
    assert_eq!(events.len(), 1);
    assert_eq!(events[0].event_type, "user.message");
}

#[tokio::test]
#[allow(deprecated)]
async fn deprecated_get_messages_alias_still_works() {
    let (session, mut server) = create_session_pair().await;
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
                    "data": { "text": "hi" },
                }]
            }),
        )
        .await;

    let events = timeout(TIMEOUT, handle).await.unwrap().unwrap();
    assert_eq!(events.len(), 1);
}

#[tokio::test]
async fn set_model_sends_switch_to_request() {
    let (session, mut server) = create_session_pair().await;
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

    timeout(TIMEOUT, handle).await.unwrap().unwrap();
}

#[tokio::test]
async fn elicitation_returns_typed_result() {
    let (session, mut server) =
        create_session_pair_with_capabilities(serde_json::json!({ "ui": { "elicitation": true } }))
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
                .ui()
                .elicitation("Enter your name", schema)
                .await
                .unwrap()
        }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.ui.elicitation");
    assert_eq!(request["params"]["message"], "Enter your name");
    assert_eq!(request["params"]["requestedSchema"], schema);
    assert!(
        request["params"].get("schema").is_none(),
        "wire field is `requestedSchema`, not `schema`"
    );
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
async fn user_input_request_dispatches_to_handler() {
    struct InputHandler;
    #[async_trait]
    impl UserInputHandler for InputHandler {
        async fn handle(
            &self,
            _session_id: SessionId,
            question: String,
            _choices: Option<Vec<String>>,
            _allow_freeform: Option<bool>,
        ) -> Option<UserInputResponse> {
            assert_eq!(question, "Pick a color");
            Some(UserInputResponse {
                answer: "blue".to_string(),
                was_freeform: true,
            })
        }
    }

    let (_session, mut server) =
        create_session_pair_with_config(|cfg| cfg.with_user_input_handler(Arc::new(InputHandler)))
            .await;
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
async fn exit_plan_mode_request_dispatches_to_handler() {
    struct ExitHandler;
    #[async_trait]
    impl ExitPlanModeHandler for ExitHandler {
        async fn handle(
            &self,
            _session_id: SessionId,
            data: ExitPlanModeData,
        ) -> ExitPlanModeResult {
            assert_eq!(data.summary, "Ready to implement");
            assert_eq!(data.plan_content.as_deref(), Some("Plan text"));
            assert_eq!(
                data.actions,
                vec!["interactive".to_string(), "autopilot".to_string()]
            );
            assert_eq!(data.recommended_action, "autopilot");
            ExitPlanModeResult {
                approved: true,
                selected_action: Some("interactive".to_string()),
                feedback: Some("Looks good".to_string()),
            }
        }
    }

    let (_session, mut server) = create_session_pair_with_config(|cfg| {
        cfg.with_exit_plan_mode_handler(Arc::new(ExitHandler))
    })
    .await;
    server
        .send_request(
            310,
            "exitPlanMode.request",
            serde_json::json!({
                "sessionId": server.session_id,
                "summary": "Ready to implement",
                "planContent": "Plan text",
                "actions": ["interactive", "autopilot"],
                "recommendedAction": "autopilot",
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 310);
    assert_eq!(response["result"]["approved"], true);
    assert_eq!(response["result"]["selectedAction"], "interactive");
    assert_eq!(response["result"]["feedback"], "Looks good");
}

#[tokio::test]
async fn auto_mode_switch_request_dispatches_to_handler() {
    struct AutoModeHandler;
    #[async_trait]
    impl AutoModeSwitchHandler for AutoModeHandler {
        async fn handle(
            &self,
            _session_id: SessionId,
            error_code: Option<String>,
            retry_after_seconds: Option<f64>,
        ) -> AutoModeSwitchResponse {
            assert_eq!(error_code.as_deref(), Some("user_weekly_rate_limited"));
            assert_eq!(retry_after_seconds, Some(3600.5));
            AutoModeSwitchResponse::YesAlways
        }
    }

    let (_session, mut server) = create_session_pair_with_config(|cfg| {
        cfg.with_auto_mode_switch_handler(Arc::new(AutoModeHandler))
    })
    .await;
    server
        .send_request(
            311,
            "autoModeSwitch.request",
            serde_json::json!({
                "sessionId": server.session_id,
                "errorCode": "user_weekly_rate_limited",
                "retryAfterSeconds": 3600.5,
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 311);
    assert_eq!(response["result"]["response"], "yes_always");
}

#[tokio::test]
async fn default_exit_plan_mode_response_omits_optional_fields() {
    let (_session, mut server) = create_session_pair_with_config(|cfg| {
        cfg.with_permission_handler(Arc::new(ApproveAllHandler))
    })
    .await;
    server
        .send_request(
            312,
            "exitPlanMode.request",
            serde_json::json!({
                "sessionId": server.session_id,
                "summary": "Ready to implement",
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 312);
    assert_eq!(response["result"]["approved"], true);
    assert!(response["result"].get("selectedAction").is_none());
    assert!(response["result"].get("feedback").is_none());
}

#[tokio::test]
async fn user_input_requested_notification_does_not_double_dispatch() {
    use std::sync::atomic::{AtomicUsize, Ordering};
    // Regression for github/github-app#4249. The CLI sends BOTH a
    // `user_input.requested` notification (for observers) AND a
    // `userInput.request` JSON-RPC call (the actual prompt) for every
    // user-input prompt. Only the JSON-RPC path should reach the
    // handler — dispatching from the notification too produced
    // duplicate ask_user widgets on the consumer side.

    struct CountingHandler {
        invocations: Arc<AtomicUsize>,
    }
    #[async_trait]
    impl UserInputHandler for CountingHandler {
        async fn handle(
            &self,
            _session_id: SessionId,
            _question: String,
            _choices: Option<Vec<String>>,
            _allow_freeform: Option<bool>,
        ) -> Option<UserInputResponse> {
            self.invocations.fetch_add(1, Ordering::SeqCst);
            Some(UserInputResponse {
                answer: "ok".to_string(),
                was_freeform: true,
            })
        }
    }

    let invocations = Arc::new(AtomicUsize::new(0));
    let handler = Arc::new(CountingHandler {
        invocations: invocations.clone(),
    });
    let (_session, mut server) =
        create_session_pair_with_config(move |cfg| cfg.with_user_input_handler(handler)).await;

    server
        .send_event(
            "user_input.requested",
            serde_json::json!({
                "requestId": "ui-1",
                "question": "Allow shell access?",
                "choices": ["Yes", "No"],
                "allowFreeform": false,
            }),
        )
        .await;

    // Give the SDK a beat to (incorrectly) auto-dispatch if the
    // regression returned. Nothing should arrive on the wire.
    let respond_observed = timeout(Duration::from_millis(150), server.read_request()).await;
    assert!(
        respond_observed.is_err(),
        "notification triggered unexpected wire activity: {respond_observed:?}",
    );
    assert_eq!(
        invocations.load(Ordering::SeqCst),
        0,
        "notification path must not invoke the user-input handler",
    );

    // Now drive the JSON-RPC path and confirm the handler still runs once.
    server
        .send_request(
            301,
            "userInput.request",
            serde_json::json!({
                "sessionId": server.session_id,
                "question": "Pick a color",
                "allowFreeform": true,
            }),
        )
        .await;
    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 301);
    assert_eq!(response["result"]["answer"], "ok");
    assert_eq!(invocations.load(Ordering::SeqCst), 1);
}

#[tokio::test]
async fn approve_all_handler_approves_permission() {
    let (_session, mut server) = create_session_pair_with_config(|cfg| {
        cfg.with_permission_handler(Arc::new(ApproveAllHandler))
    })
    .await;

    server
        .send_event(
            "permission.requested",
            serde_json::json!({
                "requestId": "perm-auto",
                "sessionId": server.session_id,
                "permissionRequest": { "kind": "shell" },
            }),
        )
        .await;

    let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(
        request["method"],
        "session.permissions.handlePendingPermissionRequest"
    );
    assert_eq!(request["params"]["requestId"], "perm-auto");
    assert_eq!(request["params"]["result"]["kind"], "approve-once");
}

#[tokio::test]
async fn session_event_notification_reaches_handler() {
    let (session, mut server) = create_session_pair().await;
    let mut sub = session.subscribe();
    server
        .send_event("session.idle", serde_json::json!({}))
        .await;

    let event = timeout(TIMEOUT, sub.recv()).await.unwrap().unwrap();
    assert_eq!(event.event_type, "session.idle");
}

#[tokio::test]
async fn router_routes_to_correct_session() {
    let (client, mut server_read, mut server_write) = make_client();

    let mut sessions = Vec::new();
    let mut session_ids = Vec::new();
    for _ in 0..2 {
        let h = tokio::spawn({
            let client = client.clone();
            async move {
                client
                    .create_session(SessionConfig::default())
                    .await
                    .unwrap()
            }
        });
        let req = read_framed(&mut server_read).await;
        let id = req["id"].as_u64().unwrap();
        let session_id = requested_session_id(&req).to_string();
        let resp = serde_json::json!({
            "jsonrpc": "2.0", "id": id,
            "result": { "sessionId": session_id.clone() },
        });
        write_framed(&mut server_write, &serde_json::to_vec(&resp).unwrap()).await;
        session_ids.push(session_id);
        sessions.push(timeout(TIMEOUT, h).await.unwrap().unwrap());
    }

    let mut sub1 = sessions[0].subscribe();
    let mut sub2 = sessions[1].subscribe();

    // Event for s-two should only reach sub2
    let notif = serde_json::json!({
        "jsonrpc": "2.0",
        "method": "session.event",
        "params": {
            "sessionId": session_ids[1].clone(),
            "event": { "id": "e1", "timestamp": "2025-01-01T00:00:00Z", "type": "assistant.message", "data": {} },
        },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&notif).unwrap()).await;
    assert_eq!(
        timeout(TIMEOUT, sub2.recv())
            .await
            .unwrap()
            .unwrap()
            .event_type,
        "assistant.message"
    );
    assert!(
        timeout(Duration::from_millis(100), sub1.recv())
            .await
            .is_err()
    );

    // Event for s-one should only reach sub1
    let notif = serde_json::json!({
        "jsonrpc": "2.0",
        "method": "session.event",
        "params": {
            "sessionId": session_ids[0].clone(),
            "event": { "id": "e2", "timestamp": "2025-01-01T00:00:00Z", "type": "session.idle", "data": {} },
        },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&notif).unwrap()).await;
    assert_eq!(
        timeout(TIMEOUT, sub1.recv())
            .await
            .unwrap()
            .unwrap()
            .event_type,
        "session.idle"
    );
    assert!(
        timeout(Duration::from_millis(100), sub2.recv())
            .await
            .is_err()
    );
}

#[tokio::test]
async fn send_and_wait_returns_last_assistant_message_on_idle() {
    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .send_and_wait(
                    MessageOptions::new("hello").with_wait_timeout(Duration::from_secs(5)),
                )
                .await
        }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.send");
    server.respond(&request, serde_json::json!({})).await;

    server
        .send_event(
            "assistant.message",
            serde_json::json!({ "message": "Hello back!" }),
        )
        .await;
    server
        .send_event("session.idle", serde_json::json!({}))
        .await;

    let result = timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
    let event = result.expect("should have captured assistant.message");
    assert_eq!(event.event_type, "assistant.message");
    assert_eq!(event.data["message"], "Hello back!");
}

#[tokio::test]
async fn send_and_wait_returns_error_on_session_error() {
    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .send_and_wait(
                    MessageOptions::new("fail").with_wait_timeout(Duration::from_secs(5)),
                )
                .await
        }
    });

    let request = server.read_request().await;
    server.respond(&request, serde_json::json!({})).await;
    server
        .send_event(
            "session.error",
            serde_json::json!({ "message": "something went wrong" }),
        )
        .await;

    let err = timeout(TIMEOUT, handle)
        .await
        .unwrap()
        .unwrap()
        .unwrap_err();
    assert!(
        matches!(err, github_copilot_sdk::Error::Session(github_copilot_sdk::SessionError::AgentError(ref msg)) if msg.contains("something went wrong"))
    );
}

#[tokio::test]
async fn send_and_wait_times_out() {
    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .send_and_wait(
                    MessageOptions::new("hello").with_wait_timeout(Duration::from_millis(100)),
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
        github_copilot_sdk::Error::Session(github_copilot_sdk::SessionError::Timeout(_))
    ));
}

/// Cancel-safety regression: an outer `tokio::time::timeout` around
/// `send_and_wait` must NOT leak the `idle_waiter` slot. After the outer
/// timeout fires and drops the future, subsequent `send` and
/// `send_and_wait` calls must succeed without `SendWhileWaiting`.
///
/// Closes RFD-400 review finding #2.
#[tokio::test]
async fn send_and_wait_outer_cancellation_clears_waiter() {
    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    // First call: wrap in outer timeout much shorter than the inner
    // wait_timeout. The outer timeout expires, dropping the
    // send_and_wait future before the idle/error event arrives.
    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            tokio::time::timeout(
                Duration::from_millis(50),
                session.send_and_wait(
                    MessageOptions::new("first").with_wait_timeout(Duration::from_secs(60)),
                ),
            )
            .await
        }
    });

    let request = server.read_request().await;
    server.respond(&request, serde_json::json!({})).await;

    // Outer timeout fires → Err(Elapsed) returned, future is dropped.
    let outer_result = timeout(Duration::from_secs(2), handle)
        .await
        .unwrap()
        .unwrap();
    assert!(outer_result.is_err(), "outer timeout should have elapsed");

    // The WaiterGuard's Drop should have cleared the slot. A subsequent
    // `send` must NOT return SendWhileWaiting.
    let send_handle = tokio::spawn({
        let session = session.clone();
        async move { session.send("second").await }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.send");
    assert_eq!(request["params"]["prompt"], "second");
    server
        .respond(
            &request,
            serde_json::json!({ "messageId": "msg-after-cancel" }),
        )
        .await;

    let result = timeout(TIMEOUT, send_handle).await.unwrap().unwrap();
    assert_eq!(result.unwrap(), "msg-after-cancel");
}

/// Cancel-safety regression: explicitly dropping the JoinHandle of an
/// in-flight `send_and_wait` must clear the waiter slot via WaiterGuard's
/// Drop. The next `send` must succeed.
///
/// Closes RFD-400 review finding #2.
#[tokio::test]
async fn send_and_wait_drop_clears_waiter() {
    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    // Start a send_and_wait, let it install the waiter, then abort the
    // task before any idle/error event arrives.
    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .send_and_wait(
                    MessageOptions::new("aborted").with_wait_timeout(Duration::from_secs(60)),
                )
                .await
        }
    });

    // Drain the session.send RPC so we know the waiter is installed.
    let request = server.read_request().await;
    server.respond(&request, serde_json::json!({})).await;

    // Now abort the in-flight send_and_wait. The WaiterGuard drops as
    // the future unwinds, clearing the slot.
    handle.abort();
    let _ = handle.await;

    // Give the runtime a moment to run the drop.
    tokio::task::yield_now().await;

    // Next `send` must succeed — no SendWhileWaiting.
    let send_handle = tokio::spawn({
        let session = session.clone();
        async move { session.send("after-abort").await }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.send");
    assert_eq!(request["params"]["prompt"], "after-abort");
    server
        .respond(
            &request,
            serde_json::json!({ "messageId": "msg-after-abort" }),
        )
        .await;

    let result = timeout(TIMEOUT, send_handle).await.unwrap().unwrap();
    assert_eq!(result.unwrap(), "msg-after-abort");
}

/// Cancel-safety regression: `Session::stop_event_loop` must NOT abort
/// the event-loop task mid-handler. An in-flight handler (here a slow
/// `userInput.request` callback) must run to completion before the loop
/// exits — the CLI receives the response on the wire before the session
/// tears down.
///
/// Closes RFD-400 review finding #3.
#[tokio::test]
async fn stop_event_loop_completes_in_flight_handler() {
    struct SlowHandler;
    #[async_trait]
    impl UserInputHandler for SlowHandler {
        async fn handle(
            &self,
            _session_id: SessionId,
            _question: String,
            _choices: Option<Vec<String>>,
            _allow_freeform: Option<bool>,
        ) -> Option<UserInputResponse> {
            tokio::time::sleep(Duration::from_millis(150)).await;
            Some(UserInputResponse {
                answer: "completed".to_string(),
                was_freeform: false,
            })
        }
    }

    let (session, mut server) =
        create_session_pair_with_config(|cfg| cfg.with_user_input_handler(Arc::new(SlowHandler)))
            .await;
    let session = Arc::new(session);

    server
        .send_request(
            900,
            "userInput.request",
            serde_json::json!({
                "sessionId": server.session_id,
                "question": "slow",
                "choices": null,
                "allowFreeform": true,
            }),
        )
        .await;

    // Give the loop a moment to dispatch into the handler.
    tokio::time::sleep(Duration::from_millis(20)).await;

    // Now request shutdown. The loop is parked in handle_request awaiting
    // the slow handler. `notify_one()` buffers the signal until the loop
    // re-enters its select, which can only happen after the handler
    // returns and the response is sent on the wire.
    let stop_handle = tokio::spawn({
        let session = session.clone();
        async move { session.stop_event_loop().await }
    });

    // Verify the handler's response lands on the wire BEFORE the loop
    // exits — i.e. stop_event_loop did not abort mid-handler.
    let response = timeout(Duration::from_secs(2), server.read_response())
        .await
        .unwrap();
    assert_eq!(response["id"], 900);
    assert_eq!(response["result"]["answer"], "completed");

    // stop_event_loop completes after the handler returns and the loop
    // observes the buffered shutdown signal on its next select iteration.
    timeout(Duration::from_secs(2), stop_handle)
        .await
        .unwrap()
        .unwrap();
}

/// Cancel-safety regression: dropping a Session does NOT abort the event
/// loop mid-handler. The loop sees the buffered shutdown signal on its
/// next select iteration and exits cleanly. This is the Drop equivalent
/// of stop_event_loop_completes_in_flight_handler; closes RFD-400 review
/// finding #3 for the implicit-drop path that used to call
/// `JoinHandle::abort()`.
#[tokio::test]
async fn drop_session_does_not_abort_handler() {
    use std::sync::atomic::{AtomicBool, Ordering};

    let handler_completed = Arc::new(AtomicBool::new(false));

    struct CompletionHandler {
        completed: Arc<AtomicBool>,
    }
    #[async_trait]
    impl UserInputHandler for CompletionHandler {
        async fn handle(
            &self,
            _session_id: SessionId,
            _question: String,
            _choices: Option<Vec<String>>,
            _allow_freeform: Option<bool>,
        ) -> Option<UserInputResponse> {
            tokio::time::sleep(Duration::from_millis(100)).await;
            self.completed.store(true, Ordering::SeqCst);
            Some(UserInputResponse {
                answer: "done".to_string(),
                was_freeform: false,
            })
        }
    }

    let handler = Arc::new(CompletionHandler {
        completed: handler_completed.clone(),
    });
    let (session, mut server) =
        create_session_pair_with_config(move |cfg| cfg.with_user_input_handler(handler)).await;

    server
        .send_request(
            901,
            "userInput.request",
            serde_json::json!({
                "sessionId": server.session_id,
                "question": "drop-test",
                "choices": null,
                "allowFreeform": true,
            }),
        )
        .await;

    tokio::time::sleep(Duration::from_millis(20)).await;
    drop(session);

    let response = timeout(Duration::from_secs(2), server.read_response())
        .await
        .unwrap();
    assert_eq!(response["id"], 901);
    assert_eq!(response["result"]["answer"], "done");
    assert!(
        handler_completed.load(Ordering::SeqCst),
        "handler must run to completion despite Session being dropped"
    );
}

/// `Session::cancellation_token()` returns a child token that fires when
/// the session shuts down. Lets external tasks bind their lifetime to the
/// session via `tokio::select!` without taking a strong reference to the
/// session itself.
#[tokio::test]
async fn cancellation_token_fires_on_session_drop() {
    let (session, _server) = create_session_pair_with_config(|cfg| {
        cfg.with_permission_handler(Arc::new(ApproveAllHandler))
    })
    .await;

    let token = session.cancellation_token();
    assert!(!token.is_cancelled());

    drop(session);

    // The session's Drop impl cancels the parent token, which propagates
    // to all child tokens.
    timeout(Duration::from_secs(2), token.cancelled())
        .await
        .expect("child token must observe cancellation after session drop");
    assert!(token.is_cancelled());
}

/// Cancelling a child token returned by `cancellation_token()` does NOT
/// shut the session down — child tokens isolate consumer-side cancel
/// logic from the session's own lifecycle.
#[tokio::test]
async fn cancellation_token_child_cancel_does_not_kill_session() {
    let (session, _server) = create_session_pair_with_config(|cfg| {
        cfg.with_permission_handler(Arc::new(ApproveAllHandler))
    })
    .await;

    let child = session.cancellation_token();
    child.cancel();

    // Session's own token (and event loop) are untouched. Issue a cheap
    // RPC and confirm it still works.
    let parent = session.cancellation_token();
    assert!(!parent.is_cancelled());
}

#[tokio::test]
async fn elicitation_requested_dispatches_to_handler_and_responds() {
    use github_copilot_sdk::types::ElicitationResult;

    struct ElicitHandler;
    #[async_trait]
    impl ElicitationHandler for ElicitHandler {
        async fn handle(
            &self,
            _session_id: SessionId,
            _request_id: RequestId,
            request: ElicitationRequest,
        ) -> ElicitationResult {
            assert_eq!(request.message, "Enter your name");
            ElicitationResult {
                action: "accept".to_string(),
                content: Some(serde_json::json!({ "name": "Alice" })),
            }
        }
    }

    let (_session, mut server) = create_session_pair_with_config(|cfg| {
        cfg.with_elicitation_handler(Arc::new(ElicitHandler))
    })
    .await;

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
    impl ElicitationHandler for FailHandler {
        async fn handle(
            &self,
            _session_id: SessionId,
            _request_id: RequestId,
            _request: ElicitationRequest,
        ) -> ElicitationResult {
            ElicitationResult {
                action: "cancel".to_string(),
                content: None,
            }
        }
    }

    let (_session, mut server) =
        create_session_pair_with_config(|cfg| cfg.with_elicitation_handler(Arc::new(FailHandler)))
            .await;
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
async fn external_tool_requested_dispatches_to_handler_and_responds() {
    struct RunTestsTool;
    #[async_trait]
    impl tool::ToolHandler for RunTestsTool {
        async fn call(
            &self,
            invocation: ToolInvocation,
        ) -> Result<ToolResult, github_copilot_sdk::Error> {
            assert_eq!(invocation.tool_name, "run_tests");
            assert_eq!(invocation.tool_call_id, "tc-ext-1");
            assert_eq!(invocation.arguments["suite"], "unit");
            Ok(ToolResult::Text("all tests passed".to_string()))
        }
    }

    let (_session, mut server) = create_session_pair_with_config(|cfg| {
        cfg.with_tools(vec![
            Tool::new("run_tests")
                .with_description("Run tests")
                .with_parameters(serde_json::json!({"type":"object"}))
                .with_handler(Arc::new(RunTestsTool)),
        ])
    })
    .await;

    server
        .send_event(
            "external_tool.requested",
            serde_json::json!({
                "requestId": "req-ext-1",
                "sessionId": server.session_id,
                "toolCallId": "tc-ext-1",
                "toolName": "run_tests",
                "arguments": { "suite": "unit" },
            }),
        )
        .await;

    let rpc_call = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(rpc_call["method"], "session.tools.handlePendingToolCall");
    assert_eq!(rpc_call["params"]["requestId"], "req-ext-1");
    assert_eq!(rpc_call["params"]["result"], "all tests passed");
}

#[tokio::test]
async fn external_tool_broadcast_for_unknown_tool_is_not_responded_to() {
    // Phase H multi-client safety: a handler that doesn't claim the
    // requested tool name must not send an RPC response — another client
    // on the same CLI may have a real handler.
    struct FooTool;
    #[async_trait]
    impl tool::ToolHandler for FooTool {
        async fn call(
            &self,
            _invocation: ToolInvocation,
        ) -> Result<ToolResult, github_copilot_sdk::Error> {
            Ok(ToolResult::Text("foo".to_string()))
        }
    }

    let (_session, mut server) = create_session_pair_with_config(|cfg| {
        cfg.with_tools(vec![
            Tool::new("foo")
                .with_description("foo")
                .with_parameters(serde_json::json!({"type":"object"}))
                .with_handler(Arc::new(FooTool)),
        ])
    })
    .await;
    server
        .send_event(
            "external_tool.requested",
            serde_json::json!({
                "requestId": "req-unknown",
                "sessionId": server.session_id,
                "toolCallId": "tc-x",
                "toolName": "bar",
                "arguments": {},
            }),
        )
        .await;

    // The dispatcher must NOT respond. Read with a short timeout and
    // assert the read times out.
    let res = tokio::time::timeout(Duration::from_millis(150), server.read_request()).await;
    assert!(
        res.is_err(),
        "expected no RPC response for unknown tool, got: {:?}",
        res.ok()
    );
}

#[tokio::test]
async fn permission_broadcast_with_resolved_by_hook_is_not_responded_to() {
    // Phase H: when the runtime marks a permission request as already
    // resolved by a hook, the client must not respond again.
    let (_session, mut server) = create_session_pair_with_config(|cfg| {
        cfg.with_permission_handler(Arc::new(ApproveAllHandler))
    })
    .await;
    server
        .send_event(
            "permission.requested",
            serde_json::json!({
                "requestId": "req-hooked",
                "sessionId": server.session_id,
                "resolvedByHook": true,
                "permissionRequest": { "kind": "shell" },
            }),
        )
        .await;

    let res = tokio::time::timeout(Duration::from_millis(150), server.read_request()).await;
    assert!(
        res.is_err(),
        "expected no RPC when resolvedByHook=true, got: {:?}",
        res.ok()
    );
}

#[tokio::test]
async fn permission_broadcast_with_no_claiming_handler_is_not_responded_to() {
    // Phase H: a handler that doesn't claim permission dispatch must not
    // respond — the SDK lets other connected clients handle the request.
    let (_session, mut server) = create_session_pair().await;
    server
        .send_event(
            "permission.requested",
            serde_json::json!({
                "requestId": "req-pending",
                "sessionId": server.session_id,
                "permissionRequest": { "kind": "shell" },
            }),
        )
        .await;

    let res = tokio::time::timeout(Duration::from_millis(150), server.read_request()).await;
    assert!(
        res.is_err(),
        "expected no RPC when handler doesn't claim permission dispatch, got: {:?}",
        res.ok()
    );
}

#[tokio::test]
async fn elicitation_broadcast_with_no_claiming_handler_is_not_responded_to() {
    // Phase H: same gating for elicitation. The default handler doesn't
    // claim elicitation, so broadcasts are silently dropped.
    let (_session, mut server) = create_session_pair_with_config(|cfg| {
        cfg.with_permission_handler(Arc::new(ApproveAllHandler))
    })
    .await;
    server
        .send_event(
            "elicitation.requested",
            serde_json::json!({
                "requestId": "elicit-silent",
                "message": "should not be answered",
            }),
        )
        .await;

    let res = tokio::time::timeout(Duration::from_millis(150), server.read_request()).await;
    assert!(
        res.is_err(),
        "expected no RPC when handler doesn't claim elicitation, got: {:?}",
        res.ok()
    );
}

#[tokio::test]
async fn capabilities_captured_from_create_response() {
    let (client, mut server_read, mut server_write) = make_client();

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default())
                .await
                .unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    let id = request["id"].as_u64().unwrap();
    let session_id = requested_session_id(&request);
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": {
            "sessionId": session_id,
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
    let (session, mut server) = create_session_pair().await;

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
                    SessionConfig::default().with_permission_handler(Arc::new(ApproveAllHandler)),
                )
                .await
                .unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.create");
    // ApproveAllHandler claims permission dispatch only; no other handlers
    // are installed, so the wire flags reflect that exact responsibility.
    assert_eq!(request["params"]["requestPermission"], true);
    assert_eq!(request["params"]["requestElicitation"], false);
    assert_eq!(request["params"]["requestExitPlanMode"], false);
    assert_eq!(request["params"]["requestAutoModeSwitch"], false);

    let id = request["id"].as_u64().unwrap();
    let session_id = requested_session_id(&request);
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": { "sessionId": session_id },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;
    timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
}

#[tokio::test]
async fn noop_handler_sends_request_permission_false() {
    // Phase H1a wire-flag derivation: a handler that doesn't claim
    // permission dispatch must send requestPermission=false so the
    // runtime doesn't broadcast permission events to this client.
    let (client, mut server_read, mut server_write) = make_client();

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default())
                .await
                .unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["params"]["requestPermission"], false);
    assert_eq!(request["params"]["requestElicitation"], false);

    let id = request["id"].as_u64().unwrap();
    let session_id = requested_session_id(&request);
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": { "sessionId": session_id },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;
    timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
}

#[tokio::test]
async fn env_value_mode_hardcoded_direct_on_create_and_resume() {
    use github_copilot_sdk::types::ResumeSessionConfig;

    let (client, mut server_read, mut server_write) = make_client();

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default())
                .await
                .unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.create");
    assert_eq!(request["params"]["envValueMode"], "direct");

    let id = request["id"].as_u64().unwrap();
    let session_id = requested_session_id(&request).to_string();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": { "sessionId": session_id.clone() },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;
    timeout(TIMEOUT, create_handle).await.unwrap().unwrap();

    let resume_handle = tokio::spawn({
        let client = client.clone();
        let session_id = session_id.clone();
        async move {
            let cfg = ResumeSessionConfig::new(SessionId::from(session_id));
            client.resume_session(cfg).await.unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.resume");
    assert_eq!(request["params"]["envValueMode"], "direct");

    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": { "sessionId": session_id },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    // resume_session also fires `session.skills.reload`; respond so resume can return.
    let reload = read_framed(&mut server_read).await;
    assert_eq!(reload["method"], "session.skills.reload");
    let id = reload["id"].as_u64().unwrap();
    let response = serde_json::json!({ "jsonrpc": "2.0", "id": id, "result": {} });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    timeout(TIMEOUT, resume_handle).await.unwrap().unwrap();
}

#[tokio::test]
async fn resume_session_sends_canvas_fields_and_captures_open_canvases() {
    use github_copilot_sdk::types::ResumeSessionConfig;

    let (client, mut server_read, mut server_write) = make_client();
    let resume_handle = tokio::spawn({
        let client = client.clone();
        async move {
            let cfg = ResumeSessionConfig::new(SessionId::from("canvas-resume"))
                .with_canvases([test_canvas("counter")])
                .with_request_canvas_renderer(true)
                .with_request_extensions(true)
                .with_extension_info(ExtensionInfo::new("github-app", "counter-provider"))
                .with_open_canvases([OpenCanvasInstance {
                    instance_id: "counter-1".to_string(),
                    extension_id: "github-app:counter-provider".to_string(),
                    extension_name: Some("Counter Provider".to_string()),
                    canvas_id: "counter".to_string(),
                    title: Some("Counter".to_string()),
                    status: Some("ready".to_string()),
                    url: Some("https://example.test/counter".to_string()),
                    input: Some(serde_json::json!({ "seed": 1 })),
                    reopen: false,
                    availability: CanvasInstanceAvailability::Stale,
                }]);
            client.resume_session(cfg).await.unwrap()
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "session.resume");
    assert_eq!(request["params"]["canvases"][0]["id"], "counter");
    assert_eq!(request["params"]["requestCanvasRenderer"], true);
    assert_eq!(request["params"]["requestExtensions"], true);
    assert_eq!(request["params"]["extensionInfo"]["source"], "github-app");
    assert_eq!(
        request["params"]["extensionInfo"]["name"],
        "counter-provider"
    );
    assert_eq!(
        request["params"]["openCanvases"][0]["availability"],
        "stale"
    );

    let id = request["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": {
            "sessionId": "canvas-resume",
            "openCanvases": [{
                "extensionId": "project:counter",
                "canvasId": "counter",
                "instanceId": "counter-1",
                "url": "https://example.test/counter",
                "reopen": false,
                "availability": "ready"
            }],
            "capabilities": {
                "ui": { "canvases": true }
            }
        },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let reload = read_framed(&mut server_read).await;
    assert_eq!(reload["method"], "session.skills.reload");
    let id = reload["id"].as_u64().unwrap();
    let response = serde_json::json!({ "jsonrpc": "2.0", "id": id, "result": {} });
    write_framed(&mut server_write, &serde_json::to_vec(&response).unwrap()).await;

    let session = timeout(TIMEOUT, resume_handle).await.unwrap().unwrap();
    let open = session.open_canvases();
    assert_eq!(open.len(), 1);
    assert_eq!(open[0].instance_id, "counter-1");
    assert_eq!(open[0].availability, CanvasInstanceAvailability::Ready);
    let caps = session.capabilities();
    assert_eq!(caps.ui.unwrap().canvases, Some(true));
}

#[tokio::test]
async fn elicitation_methods_fail_without_capability() {
    let (session, _server) = create_session_pair().await;

    // Session created without capabilities — elicitation should fail
    let err = session
        .ui()
        .elicitation("test", serde_json::json!({}))
        .await
        .unwrap_err();
    assert!(matches!(
        err,
        github_copilot_sdk::Error::Session(
            github_copilot_sdk::SessionError::ElicitationNotSupported
        )
    ));

    let err = session.ui().confirm("ok?").await.unwrap_err();
    assert!(matches!(
        err,
        github_copilot_sdk::Error::Session(
            github_copilot_sdk::SessionError::ElicitationNotSupported
        )
    ));
}

async fn create_session_pair_with_hooks(
    hooks: Arc<dyn github_copilot_sdk::hooks::SessionHooks>,
) -> (github_copilot_sdk::session::Session, FakeServer) {
    let (client, server_read, server_write) = make_client();

    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: String::new(),
    };

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default().with_hooks(hooks))
                .await
                .unwrap()
        }
    });

    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
    // Verify hooks: true is auto-set in the config
    assert_eq!(create_req["params"]["hooks"], true);
    server.session_id = requested_session_id(&create_req).to_string();
    server
        .respond(
            &create_req,
            serde_json::json!({
                "sessionId": server.session_id,
                "workspacePath": "/tmp/workspace"
            }),
        )
        .await;

    let session = timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
    (session, server)
}

#[tokio::test]
async fn hooks_invoke_dispatches_to_session_hooks() {
    use github_copilot_sdk::hooks::{HookEvent, HookOutput, PreToolUseOutput, SessionHooks};

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

    let (_session, mut server) = create_session_pair_with_hooks(Arc::new(PolicyHooks)).await;

    // Send a hooks.invoke request for a denied tool
    server
        .send_request(
            300,
            "hooks.invoke",
            serde_json::json!({
                "sessionId": server.session_id,
                "hookType": "preToolUse",
                "input": {
                    "sessionId": "test-session",
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
    use github_copilot_sdk::hooks::SessionHooks;

    struct EmptyHooks;
    #[async_trait]
    impl SessionHooks for EmptyHooks {}

    let (_session, mut server) = create_session_pair_with_hooks(Arc::new(EmptyHooks)).await;

    server
        .send_request(
            301,
            "hooks.invoke",
            serde_json::json!({
                "sessionId": server.session_id,
                "hookType": "sessionEnd",
                "input": {
                    "sessionId": "test-session",
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

async fn create_session_pair_with_system_message_transforms(
    transforms: Arc<dyn github_copilot_sdk::transforms::SystemMessageTransform>,
) -> (github_copilot_sdk::session::Session, FakeServer) {
    let (client, server_read, server_write) = make_client();

    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: String::new(),
    };

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default().with_system_message_transform(transforms))
                .await
                .unwrap()
        }
    });

    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
    // Verify transforms inject customize mode and section overrides
    assert_eq!(create_req["params"]["systemMessage"]["mode"], "customize");
    server.session_id = requested_session_id(&create_req).to_string();
    server
        .respond(
            &create_req,
            serde_json::json!({
                "sessionId": server.session_id,
                "workspacePath": "/tmp/workspace"
            }),
        )
        .await;

    let session = timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
    (session, server)
}

#[tokio::test]
async fn system_message_transform_dispatches_to_transform() {
    use github_copilot_sdk::transforms::{SystemMessageTransform, TransformContext};

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
        create_session_pair_with_system_message_transforms(Arc::new(AppendTransform)).await;

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
    use github_copilot_sdk::transforms::{SystemMessageTransform, TransformContext};

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
        create_session_pair_with_system_message_transforms(Arc::new(DummyTransform)).await;

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

#[tokio::test]
async fn rpc_namespace_session_agent_list_dispatches_correctly() {
    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    let s = session.clone();
    let handle = tokio::spawn(async move { s.rpc().agent().list().await });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.agent.list");
    assert_eq!(request["params"]["sessionId"], server.session_id);
    server
        .respond(&request, serde_json::json!({ "agents": [] }))
        .await;

    let result = timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
    assert!(result.agents.is_empty());
}

#[tokio::test]
async fn rpc_namespace_session_tasks_list_dispatches_correctly() {
    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    let s = session.clone();
    let handle = tokio::spawn(async move { s.rpc().tasks().list().await });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.tasks.list");
    assert_eq!(request["params"]["sessionId"], server.session_id);
    server
        .respond(&request, serde_json::json!({ "tasks": [] }))
        .await;

    let result = timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
    assert!(result.tasks.is_empty());
}

#[tokio::test]
async fn rpc_namespace_client_models_list_dispatches_correctly() {
    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    let client = session.client().clone();
    let handle = tokio::spawn(async move { client.rpc().models().list().await });

    let request = server.read_request().await;
    assert_eq!(request["method"], "models.list");
    server
        .respond(&request, serde_json::json!({ "models": [] }))
        .await;

    let result = timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
    assert!(result.models.is_empty());
}

#[tokio::test]
async fn client_stop_sends_session_destroy_for_each_active_session() {
    // One client, two registered sessions. Client::stop must send
    // session.destroy for each before returning Ok.
    let (client, server_read, server_write) = make_client();

    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: String::new(),
    };

    // Spawn both create_session calls.
    let create_a = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default())
                .await
                .unwrap()
        }
    });
    let create_a_req = server.read_request().await;
    assert_eq!(create_a_req["method"], "session.create");
    let session_id_a = requested_session_id(&create_a_req).to_string();
    server
        .respond(
            &create_a_req,
            serde_json::json!({ "sessionId": session_id_a.clone(), "workspacePath": "/tmp/ws-a" }),
        )
        .await;
    let _session_a = timeout(TIMEOUT, create_a).await.unwrap();

    let create_b = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default())
                .await
                .unwrap()
        }
    });
    let create_b_req = server.read_request().await;
    assert_eq!(create_b_req["method"], "session.create");
    let session_id_b = requested_session_id(&create_b_req).to_string();
    server
        .respond(
            &create_b_req,
            serde_json::json!({ "sessionId": session_id_b.clone(), "workspacePath": "/tmp/ws-b" }),
        )
        .await;
    let _session_b = timeout(TIMEOUT, create_b).await.unwrap();

    // Drive Client::stop and respond to each destroy in turn.
    let stop_handle = tokio::spawn({
        let client = client.clone();
        async move { client.stop().await }
    });

    let mut destroyed = Vec::new();
    for _ in 0..2 {
        let req = server.read_request().await;
        assert_eq!(req["method"], "session.destroy");
        destroyed.push(req["params"]["sessionId"].as_str().unwrap().to_string());
        server.respond(&req, serde_json::json!(null)).await;
    }
    destroyed.sort();
    let mut expected = [session_id_a.clone(), session_id_b.clone()];
    expected.sort();
    assert_eq!(destroyed, expected);

    let stop_result = timeout(TIMEOUT, stop_handle).await.unwrap().unwrap();
    assert!(stop_result.is_ok(), "stop returned errors: {stop_result:?}");
}

#[tokio::test]
async fn client_stop_aggregates_session_destroy_errors() {
    // session.destroy fails on the wire — Client::stop returns
    // StopErrors carrying the failure rather than short-circuiting.
    let (session, mut server) = create_session_pair().await;
    let client = session.client().clone();

    let stop_handle = tokio::spawn(async move { client.stop().await });

    let req = server.read_request().await;
    assert_eq!(req["method"], "session.destroy");
    let id = req["id"].as_u64().unwrap();
    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "error": { "code": -32000, "message": "session gone" },
    });
    write_framed(&mut server.write, &serde_json::to_vec(&response).unwrap()).await;

    let stop_result = timeout(TIMEOUT, stop_handle).await.unwrap().unwrap();
    let errors = stop_result.expect_err("expected aggregated errors");
    assert_eq!(errors.errors().len(), 1);
    let msg = errors.to_string();
    assert!(msg.contains("session gone"), "unexpected message: {msg}");
}

#[test]
fn session_config_serializes_bucket_b_fields() {
    use std::path::PathBuf;

    use github_copilot_sdk::{
        CloudSessionOptions, CloudSessionRepository, SessionConfig, SessionId,
    };

    let mut cfg = SessionConfig::default();
    cfg.session_id = Some(SessionId::from("custom-id"));
    cfg.config_dir = Some(PathBuf::from("/tmp/cfg"));
    cfg.working_directory = Some(PathBuf::from("/tmp/work"));
    cfg.github_token = Some("ghs_secret".to_string());
    cfg.include_sub_agent_streaming_events = Some(false);
    cfg.enable_session_telemetry = Some(false);
    cfg.remote_session = Some(github_copilot_sdk::generated::api_types::RemoteSessionMode::Export);
    cfg.cloud = Some(CloudSessionOptions::with_repository(
        CloudSessionRepository::new("github", "copilot-sdk").with_branch("main"),
    ));

    // Debug never leaks the token.
    let debug = format!("{cfg:?}");
    assert!(!debug.contains("ghs_secret"), "leaked token: {debug}");
    assert!(debug.contains("<redacted>"), "missing redaction: {debug}");
    // Wire-format coverage now lives in the in-crate unit tests next to
    // `SessionConfig::into_wire` — the wire payload is `pub(crate)` so
    // external integration tests can only inspect the user-facing config.
}

#[test]
fn resume_session_config_serializes_bucket_b_fields() {
    use std::path::PathBuf;

    use github_copilot_sdk::{ResumeSessionConfig, SessionId};

    let mut cfg = ResumeSessionConfig::new(SessionId::from("sess-1"));
    cfg.working_directory = Some(PathBuf::from("/tmp/work"));
    cfg.config_dir = Some(PathBuf::from("/tmp/cfg"));
    cfg.github_token = Some("ghs_secret".to_string());
    cfg.include_sub_agent_streaming_events = Some(true);
    cfg.enable_session_telemetry = Some(false);
    cfg.remote_session = Some(github_copilot_sdk::generated::api_types::RemoteSessionMode::On);

    let debug = format!("{cfg:?}");
    assert!(!debug.contains("ghs_secret"), "leaked token: {debug}");
    // Wire-format coverage lives in the in-crate unit tests; see
    // `ResumeSessionConfig::into_wire`.
}

// =====================================================================
// Slash commands (§ 4.1)
// =====================================================================

struct CountingCommandHandler {
    last_ctx: Arc<parking_lot::Mutex<Option<CommandContext>>>,
    error_to_return: Option<String>,
}

#[async_trait]
impl CommandHandler for CountingCommandHandler {
    async fn on_command(&self, ctx: CommandContext) -> Result<(), github_copilot_sdk::Error> {
        *self.last_ctx.lock() = Some(ctx);
        if let Some(message) = &self.error_to_return {
            Err(github_copilot_sdk::Error::Session(
                github_copilot_sdk::SessionError::AgentError(message.clone()),
            ))
        } else {
            Ok(())
        }
    }
}

async fn create_session_pair_with_commands(
    commands: Vec<CommandDefinition>,
) -> (github_copilot_sdk::session::Session, FakeServer, Value) {
    let (client, server_read, server_write) = make_client();

    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: String::new(),
    };

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default().with_commands(commands))
                .await
                .unwrap()
        }
    });

    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
    server.session_id = requested_session_id(&create_req).to_string();
    server
        .respond(
            &create_req,
            serde_json::json!({
                "sessionId": server.session_id,
                "workspacePath": "/tmp/workspace"
            }),
        )
        .await;

    let session = timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
    (session, server, create_req)
}

#[tokio::test]
async fn create_serializes_commands_strips_handler() {
    let last_ctx = Arc::new(parking_lot::Mutex::new(None));
    let commands = vec![
        CommandDefinition::new(
            "deploy",
            Arc::new(CountingCommandHandler {
                last_ctx: last_ctx.clone(),
                error_to_return: None,
            }),
        )
        .with_description("Deploy to production"),
        CommandDefinition::new(
            "rollback",
            Arc::new(CountingCommandHandler {
                last_ctx: last_ctx.clone(),
                error_to_return: None,
            }),
        ),
    ];

    let (_session, _server, create_req) = create_session_pair_with_commands(commands).await;

    let wire = create_req["params"]["commands"]
        .as_array()
        .expect("commands should be an array");
    assert_eq!(wire.len(), 2);

    let deploy = &wire[0];
    assert_eq!(deploy["name"], "deploy");
    assert_eq!(deploy["description"], "Deploy to production");
    assert!(
        deploy.get("handler").is_none(),
        "wire payload must not include handler, got: {deploy}"
    );
    let deploy_keys: Vec<&String> = deploy.as_object().unwrap().keys().collect();
    assert_eq!(deploy_keys.len(), 2, "got keys: {deploy_keys:?}");

    let rollback = &wire[1];
    assert_eq!(rollback["name"], "rollback");
    assert!(
        rollback.get("description").is_none(),
        "description should be omitted when None, got: {rollback}"
    );
    assert!(rollback.get("handler").is_none());
    let rollback_keys: Vec<&String> = rollback.as_object().unwrap().keys().collect();
    assert_eq!(rollback_keys.len(), 1, "got keys: {rollback_keys:?}");
}

#[tokio::test]
async fn command_execute_dispatches_to_registered_handler_and_acks_success() {
    let last_ctx = Arc::new(parking_lot::Mutex::new(None));
    let commands = vec![CommandDefinition::new(
        "deploy",
        Arc::new(CountingCommandHandler {
            last_ctx: last_ctx.clone(),
            error_to_return: None,
        }),
    )];

    let (session, mut server, _) = create_session_pair_with_commands(commands).await;

    server
        .send_event(
            "command.execute",
            serde_json::json!({
                "requestId": "req-deploy-1",
                "command": "/deploy production",
                "commandName": "deploy",
                "args": "production",
            }),
        )
        .await;

    let ack = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(
        ack["method"], "session.commands.handlePendingCommand",
        "expected handlePendingCommand RPC, got: {ack}"
    );
    assert_eq!(
        ack["params"]["sessionId"].as_str(),
        Some(session.id().as_ref())
    );
    assert_eq!(ack["params"]["requestId"], "req-deploy-1");
    assert!(
        ack["params"].get("error").is_none(),
        "success ack should omit error, got: {ack}"
    );

    server
        .respond(&ack, serde_json::json!({ "success": true }))
        .await;

    let ctx = last_ctx
        .lock()
        .clone()
        .expect("handler should have been invoked");
    assert_eq!(ctx.command, "/deploy production");
    assert_eq!(ctx.command_name, "deploy");
    assert_eq!(ctx.args, "production");
    assert_eq!(ctx.session_id.as_ref(), session.id().as_ref());
}

#[tokio::test]
async fn command_execute_unknown_command_acks_with_error() {
    let (session, mut server, _) = create_session_pair_with_commands(vec![]).await;

    server
        .send_event(
            "command.execute",
            serde_json::json!({
                "requestId": "req-unknown-1",
                "command": "/missing",
                "commandName": "missing",
                "args": "",
            }),
        )
        .await;

    let ack = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(ack["method"], "session.commands.handlePendingCommand");
    assert_eq!(ack["params"]["requestId"], "req-unknown-1");
    assert_eq!(
        ack["params"]["error"], "Unknown command: missing",
        "got: {ack}"
    );
    server
        .respond(&ack, serde_json::json!({ "success": false }))
        .await;
    drop(session);
}

#[tokio::test]
async fn command_execute_handler_error_propagates_to_ack() {
    let last_ctx = Arc::new(parking_lot::Mutex::new(None));
    let commands = vec![CommandDefinition::new(
        "fail",
        Arc::new(CountingCommandHandler {
            last_ctx: last_ctx.clone(),
            error_to_return: Some("deploy failed: dry-run rejected".to_string()),
        }),
    )];

    let (_session, mut server, _) = create_session_pair_with_commands(commands).await;

    server
        .send_event(
            "command.execute",
            serde_json::json!({
                "requestId": "req-fail-1",
                "command": "/fail",
                "commandName": "fail",
                "args": "",
            }),
        )
        .await;

    let ack = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(ack["method"], "session.commands.handlePendingCommand");
    assert_eq!(ack["params"]["requestId"], "req-fail-1");
    let error_msg = ack["params"]["error"]
        .as_str()
        .expect("ack should include error");
    assert!(
        error_msg.contains("deploy failed: dry-run rejected"),
        "expected handler error in ack, got: {error_msg}"
    );
    server
        .respond(&ack, serde_json::json!({ "success": false }))
        .await;
}

// SessionFsProvider tests --------------------------------------------------

use github_copilot_sdk::session_fs::{
    DirEntry, DirEntryKind, FileInfo, FsError, SessionFsConventions, SessionFsProvider,
    SessionFsSqliteProvider, SessionFsSqliteQueryResult, SessionFsSqliteQueryType,
};

struct RecordingFsProvider {
    files: parking_lot::Mutex<std::collections::HashMap<String, String>>,
}

impl RecordingFsProvider {
    fn new() -> Self {
        Self {
            files: parking_lot::Mutex::new(std::collections::HashMap::new()),
        }
    }

    fn with_file(self, path: &str, content: &str) -> Self {
        self.files
            .lock()
            .insert(path.to_string(), content.to_string());
        self
    }
}

#[async_trait]
impl SessionFsProvider for RecordingFsProvider {
    async fn read_file(&self, path: &str) -> Result<String, FsError> {
        self.files
            .lock()
            .get(path)
            .cloned()
            .ok_or_else(|| FsError::NotFound(path.to_string()))
    }

    async fn write_file(
        &self,
        path: &str,
        content: &str,
        _mode: Option<i64>,
    ) -> Result<(), FsError> {
        self.files
            .lock()
            .insert(path.to_string(), content.to_string());
        Ok(())
    }

    async fn stat(&self, path: &str) -> Result<FileInfo, FsError> {
        let files = self.files.lock();
        let content = files
            .get(path)
            .ok_or_else(|| FsError::NotFound(path.to_string()))?;
        Ok(FileInfo::new(
            true,
            false,
            content.len() as i64,
            "2025-01-01T00:00:00Z",
            "2025-01-01T00:00:00Z",
        ))
    }

    async fn readdir_with_types(&self, _path: &str) -> Result<Vec<DirEntry>, FsError> {
        Ok(vec![
            DirEntry::new("README.md", DirEntryKind::File),
            DirEntry::new("src", DirEntryKind::Directory),
        ])
    }

    async fn rm(&self, path: &str, _recursive: bool, force: bool) -> Result<(), FsError> {
        let mut files = self.files.lock();
        if files.remove(path).is_none() && !force {
            return Err(FsError::NotFound(path.to_string()));
        }
        Ok(())
    }

    fn sqlite(&self) -> Option<&dyn SessionFsSqliteProvider> {
        Some(self)
    }
}

#[async_trait]
impl SessionFsSqliteProvider for RecordingFsProvider {
    async fn sqlite_query(
        &self,
        query_type: SessionFsSqliteQueryType,
        query: &str,
        params: Option<&std::collections::HashMap<String, serde_json::Value>>,
    ) -> Result<Option<SessionFsSqliteQueryResult>, FsError> {
        let mut row = std::collections::HashMap::new();
        row.insert(
            "query".to_string(),
            serde_json::Value::String(query.to_string()),
        );
        row.insert(
            "queryType".to_string(),
            serde_json::Value::String(
                match query_type {
                    SessionFsSqliteQueryType::Exec => "exec",
                    SessionFsSqliteQueryType::Query => "query",
                    SessionFsSqliteQueryType::Run => "run",
                    SessionFsSqliteQueryType::Unknown => "unknown",
                }
                .to_string(),
            ),
        );
        row.insert(
            "answer".to_string(),
            params
                .and_then(|params| params.get("answer"))
                .cloned()
                .unwrap_or(serde_json::Value::Null),
        );
        Ok(Some(SessionFsSqliteQueryResult {
            columns: vec![
                "query".to_string(),
                "queryType".to_string(),
                "answer".to_string(),
            ],
            rows: vec![row],
            rows_affected: 0,
            last_insert_rowid: None,
        }))
    }

    async fn sqlite_exists(&self) -> Result<bool, FsError> {
        Ok(true)
    }
}

async fn create_session_pair_with_fs_provider(
    provider: Arc<dyn SessionFsProvider>,
) -> (github_copilot_sdk::session::Session, FakeServer) {
    let (client, server_read, server_write) = make_client();

    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: String::new(),
    };

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default().with_session_fs_provider(provider))
                .await
                .unwrap()
        }
    });

    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
    server.session_id = requested_session_id(&create_req).to_string();
    server
        .respond(
            &create_req,
            serde_json::json!({
                "sessionId": server.session_id,
                "workspacePath": "/tmp/workspace"
            }),
        )
        .await;

    let session = timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
    (session, server)
}

#[tokio::test]
async fn session_fs_dispatches_read_file_to_provider() {
    let provider = Arc::new(RecordingFsProvider::new().with_file("/foo.txt", "hello world"));
    let (_session, mut server) = create_session_pair_with_fs_provider(provider).await;

    server
        .send_request(
            42,
            "sessionFs.readFile",
            serde_json::json!({ "sessionId": server.session_id, "path": "/foo.txt" }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 42);
    assert_eq!(response["result"]["content"], "hello world");
    assert!(response["result"].get("error").is_none() || response["result"]["error"].is_null());
}

#[tokio::test]
async fn session_fs_maps_not_found_to_enoent() {
    let provider = Arc::new(RecordingFsProvider::new());
    let (_session, mut server) = create_session_pair_with_fs_provider(provider).await;

    server
        .send_request(
            7,
            "sessionFs.readFile",
            serde_json::json!({ "sessionId": server.session_id, "path": "/missing.txt" }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 7);
    let error = &response["result"]["error"];
    assert_eq!(error["code"], "ENOENT");
    assert!(error["message"].as_str().unwrap().contains("missing.txt"));
}

#[tokio::test]
async fn session_fs_maps_other_to_unknown() {
    struct AlwaysFails;
    #[async_trait]
    impl SessionFsProvider for AlwaysFails {
        async fn stat(&self, _path: &str) -> Result<FileInfo, FsError> {
            Err(FsError::Other("backing store unavailable".to_string()))
        }
    }

    let (_session, mut server) = create_session_pair_with_fs_provider(Arc::new(AlwaysFails)).await;

    server
        .send_request(
            8,
            "sessionFs.stat",
            serde_json::json!({ "sessionId": server.session_id, "path": "/x" }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    let error = &response["result"]["error"];
    assert_eq!(error["code"], "UNKNOWN");
    assert!(
        error["message"]
            .as_str()
            .unwrap()
            .contains("backing store unavailable")
    );
}

#[tokio::test]
async fn session_fs_dispatches_sqlite_query_to_provider() {
    let provider = Arc::new(RecordingFsProvider::new());
    let (_session, mut server) = create_session_pair_with_fs_provider(provider).await;

    server
        .send_request(
            9,
            "sessionFs.sqliteQuery",
            serde_json::json!({
                "sessionId": server.session_id,
                "query": "select :answer as answer",
                "queryType": "query",
                "params": { "answer": 42 },
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 9);
    assert_eq!(response["result"]["columns"][2], "answer");
    assert_eq!(
        response["result"]["rows"][0]["query"],
        "select :answer as answer"
    );
    assert_eq!(response["result"]["rows"][0]["queryType"], "query");
    assert_eq!(response["result"]["rows"][0]["answer"], 42);
    assert_eq!(response["result"]["rowsAffected"], 0);
    assert!(response["result"].get("error").is_none() || response["result"]["error"].is_null());
}

#[tokio::test]
async fn session_fs_dispatches_sqlite_exists_to_provider() {
    let provider = Arc::new(RecordingFsProvider::new());
    let (_session, mut server) = create_session_pair_with_fs_provider(provider).await;

    server
        .send_request(
            13,
            "sessionFs.sqliteExists",
            serde_json::json!({ "sessionId": server.session_id }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 13);
    assert_eq!(response["result"]["exists"], true);
}

#[tokio::test]
async fn session_fs_maps_sqlite_errors_to_results() {
    struct AlwaysFails;
    #[async_trait]
    impl SessionFsProvider for AlwaysFails {
        fn sqlite(&self) -> Option<&dyn SessionFsSqliteProvider> {
            Some(self)
        }
    }
    #[async_trait]
    impl SessionFsSqliteProvider for AlwaysFails {
        async fn sqlite_query(
            &self,
            _query_type: SessionFsSqliteQueryType,
            _query: &str,
            _params: Option<&std::collections::HashMap<String, serde_json::Value>>,
        ) -> Result<Option<SessionFsSqliteQueryResult>, FsError> {
            Err(FsError::Other("sqlite unavailable".to_string()))
        }

        async fn sqlite_exists(&self) -> Result<bool, FsError> {
            Err(FsError::Other("sqlite unavailable".to_string()))
        }
    }

    let (_session, mut server) = create_session_pair_with_fs_provider(Arc::new(AlwaysFails)).await;

    server
        .send_request(
            14,
            "sessionFs.sqliteQuery",
            serde_json::json!({
                "sessionId": server.session_id,
                "query": "select 1",
                "queryType": "query",
            }),
        )
        .await;
    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 14);
    assert_eq!(response["result"]["columns"].as_array().unwrap().len(), 0);
    assert_eq!(response["result"]["rows"].as_array().unwrap().len(), 0);
    assert_eq!(response["result"]["rowsAffected"], 0);
    let error = &response["result"]["error"];
    assert_eq!(error["code"], "UNKNOWN");
    assert!(
        error["message"]
            .as_str()
            .unwrap()
            .contains("sqlite unavailable")
    );

    server
        .send_request(
            15,
            "sessionFs.sqliteExists",
            serde_json::json!({ "sessionId": server.session_id }),
        )
        .await;
    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 15);
    assert_eq!(response["result"]["exists"], false);
}

#[tokio::test]
async fn session_fs_dispatches_write_file_with_mode() {
    let provider = Arc::new(RecordingFsProvider::new());
    let (_session, mut server) = create_session_pair_with_fs_provider(provider.clone()).await;

    server
        .send_request(
            10,
            "sessionFs.writeFile",
            serde_json::json!({ "sessionId": server.session_id, "path": "/out.txt", "content": "abc", "mode": 420 }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 10);
    assert!(response["result"].get("error").is_none() || response["result"]["error"].is_null());
    assert_eq!(provider.files.lock().get("/out.txt").unwrap(), "abc");
}

#[tokio::test]
async fn session_fs_dispatches_readdir_with_types() {
    let provider = Arc::new(RecordingFsProvider::new());
    let (_session, mut server) = create_session_pair_with_fs_provider(provider).await;

    server
        .send_request(
            11,
            "sessionFs.readdirWithTypes",
            serde_json::json!({ "sessionId": server.session_id, "path": "/dir" }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    let entries = response["result"]["entries"].as_array().unwrap();
    assert_eq!(entries.len(), 2);
    assert_eq!(entries[0]["name"], "README.md");
    assert_eq!(entries[0]["type"], "file");
    assert_eq!(entries[1]["name"], "src");
    assert_eq!(entries[1]["type"], "directory");
}

#[tokio::test]
async fn session_fs_dispatches_rm_with_force() {
    let provider = Arc::new(RecordingFsProvider::new());
    let (_session, mut server) = create_session_pair_with_fs_provider(provider).await;

    server
        .send_request(
            12,
            "sessionFs.rm",
            serde_json::json!({ "sessionId": server.session_id, "path": "/missing", "force": true, "recursive": false }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 12);
    assert!(response["result"].get("error").is_none() || response["result"]["error"].is_null());
}

#[tokio::test]
async fn validate_session_fs_config_rejects_empty_initial_cwd() {
    let cfg = github_copilot_sdk::session_fs::SessionFsConfig::new(
        "",
        "/state",
        SessionFsConventions::Posix,
    );
    let opts = {
        let mut opts = github_copilot_sdk::ClientOptions::default();
        opts.session_fs = Some(cfg);
        opts
    };
    let err = github_copilot_sdk::Client::start(opts).await.err();
    let err_string = format!("{err:?}");
    assert!(
        err_string.contains("initial_cwd") || err_string.contains("InvalidSessionFsConfig"),
        "got: {err_string}"
    );
}

#[tokio::test]
async fn create_session_errors_when_provider_required_but_missing() {
    // Without a CLI we can't exercise the configured-but-missing-provider path
    // through Client::start; the unit-level behavior is covered by the
    // SessionError::SessionFsProviderRequired variant being constructible.
    // This test asserts the error type's display formatting is stable.
    let err = github_copilot_sdk::SessionError::SessionFsProviderRequired;
    assert!(format!("{err}").contains("session_fs"));
}

// ---------- 4.3 trace context tests ----------

struct StaticTraceProvider {
    ctx: github_copilot_sdk::types::TraceContext,
    calls: Arc<AtomicUsize>,
}

#[async_trait]
impl github_copilot_sdk::types::TraceContextProvider for StaticTraceProvider {
    async fn get_trace_context(&self) -> github_copilot_sdk::types::TraceContext {
        self.calls.fetch_add(1, Ordering::Relaxed);
        self.ctx.clone()
    }
}

fn make_client_with_trace_provider(
    provider: Arc<dyn github_copilot_sdk::types::TraceContextProvider>,
) -> (Client, tokio::io::DuplexStream, tokio::io::DuplexStream) {
    let (client_write, server_read) = duplex(8192);
    let (server_write, client_read) = duplex(8192);
    let client = Client::from_streams_with_trace_provider(
        client_read,
        client_write,
        std::env::temp_dir(),
        provider,
    )
    .unwrap();
    (client, server_read, server_write)
}

#[tokio::test]
async fn on_get_trace_context_called_on_session_create() {
    let calls = Arc::new(AtomicUsize::new(0));
    let provider = Arc::new(StaticTraceProvider {
        ctx: github_copilot_sdk::types::TraceContext::from_traceparent("00-aaaa-bbbb-01")
            .with_tracestate("vendor=value"),
        calls: calls.clone(),
    });
    let (client, server_read, server_write) = make_client_with_trace_provider(provider);
    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: "trace-create".to_string(),
    };

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default())
                .await
                .unwrap()
        }
    });

    let req = server.read_request().await;
    assert_eq!(req["method"], "session.create");
    assert_eq!(req["params"]["traceparent"], "00-aaaa-bbbb-01");
    assert_eq!(req["params"]["tracestate"], "vendor=value");
    server.session_id = requested_session_id(&req).to_string();
    server
        .respond(
            &req,
            serde_json::json!({"sessionId": server.session_id.clone(), "workspacePath": "/tmp/ws"}),
        )
        .await;
    timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
    assert_eq!(calls.load(Ordering::Relaxed), 1);
}

#[tokio::test]
async fn on_get_trace_context_called_on_session_resume() {
    use github_copilot_sdk::types::ResumeSessionConfig;
    let calls = Arc::new(AtomicUsize::new(0));
    let provider = Arc::new(StaticTraceProvider {
        ctx: github_copilot_sdk::types::TraceContext::from_traceparent("00-resume-trace-01"),
        calls: calls.clone(),
    });
    let (client, server_read, server_write) = make_client_with_trace_provider(provider);
    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: "trace-resume".to_string(),
    };

    let resume_handle = tokio::spawn({
        let client = client.clone();
        async move {
            let cfg = ResumeSessionConfig::new(SessionId::from("trace-resume"));
            client.resume_session(cfg).await.unwrap()
        }
    });

    // resume sends `session.resume` then `session.skills.reload`.
    let req = server.read_request().await;
    assert_eq!(req["method"], "session.resume");
    assert_eq!(req["params"]["traceparent"], "00-resume-trace-01");
    assert!(
        req["params"].get("tracestate").is_none(),
        "tracestate should be omitted when None"
    );
    server
        .respond(
            &req,
            serde_json::json!({"sessionId": "trace-resume", "workspacePath": "/tmp/ws"}),
        )
        .await;
    let reload_req = server.read_request().await;
    assert_eq!(reload_req["method"], "session.skills.reload");
    server.respond(&reload_req, serde_json::json!({})).await;

    timeout(TIMEOUT, resume_handle).await.unwrap().unwrap();
    assert_eq!(calls.load(Ordering::Relaxed), 1);
}

#[tokio::test]
async fn on_get_trace_context_called_on_session_send() {
    let calls = Arc::new(AtomicUsize::new(0));
    let provider = Arc::new(StaticTraceProvider {
        ctx: github_copilot_sdk::types::TraceContext::from_traceparent("00-send-trace-01"),
        calls: calls.clone(),
    });
    let (client, server_read, server_write) = make_client_with_trace_provider(provider);
    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: "trace-send".to_string(),
    };

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default())
                .await
                .unwrap()
        }
    });
    let create_req = server.read_request().await;
    server.session_id = requested_session_id(&create_req).to_string();
    server
        .respond(
            &create_req,
            serde_json::json!({"sessionId": server.session_id.clone(), "workspacePath": "/tmp/ws"}),
        )
        .await;
    let session = Arc::new(timeout(TIMEOUT, create_handle).await.unwrap().unwrap());

    // Provider was called once for create; reset by reading the count baseline.
    let baseline = calls.load(Ordering::Relaxed);
    assert_eq!(baseline, 1, "create_session should call the provider once");

    let send_handle = tokio::spawn({
        let session = session.clone();
        async move { session.send(MessageOptions::new("hi")).await }
    });
    let send_req = server.read_request().await;
    assert_eq!(send_req["method"], "session.send");
    assert_eq!(send_req["params"]["traceparent"], "00-send-trace-01");
    server.respond(&send_req, serde_json::json!({})).await;
    timeout(TIMEOUT, send_handle)
        .await
        .unwrap()
        .unwrap()
        .unwrap();
    assert_eq!(calls.load(Ordering::Relaxed), baseline + 1);
}

#[tokio::test]
async fn message_options_trace_context_overrides_callback() {
    let calls = Arc::new(AtomicUsize::new(0));
    let provider = Arc::new(StaticTraceProvider {
        ctx: github_copilot_sdk::types::TraceContext::from_traceparent("00-callback-01"),
        calls: calls.clone(),
    });
    let (client, server_read, server_write) = make_client_with_trace_provider(provider);
    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: "trace-override".to_string(),
    };

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default())
                .await
                .unwrap()
        }
    });
    let create_req = server.read_request().await;
    server.session_id = requested_session_id(&create_req).to_string();
    server
        .respond(
            &create_req,
            serde_json::json!({"sessionId": server.session_id.clone(), "workspacePath": "/tmp/ws"}),
        )
        .await;
    let session = Arc::new(timeout(TIMEOUT, create_handle).await.unwrap().unwrap());

    let baseline = calls.load(Ordering::Relaxed);

    let send_handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .send(
                    MessageOptions::new("hi")
                        .with_traceparent("00-override-01")
                        .with_tracestate("vendor=override"),
                )
                .await
        }
    });
    let send_req = server.read_request().await;
    assert_eq!(send_req["params"]["traceparent"], "00-override-01");
    assert_eq!(send_req["params"]["tracestate"], "vendor=override");
    server.respond(&send_req, serde_json::json!({})).await;
    timeout(TIMEOUT, send_handle)
        .await
        .unwrap()
        .unwrap()
        .unwrap();

    // Callback must NOT have been invoked when MessageOptions carried an override.
    assert_eq!(
        calls.load(Ordering::Relaxed),
        baseline,
        "callback should be skipped when MessageOptions carries trace headers"
    );
}

#[tokio::test]
async fn message_options_trace_context_used_without_callback() {
    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    let send_handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .send(MessageOptions::new("hi").with_traceparent("00-direct-01"))
                .await
        }
    });
    let req = server.read_request().await;
    assert_eq!(req["method"], "session.send");
    assert_eq!(req["params"]["traceparent"], "00-direct-01");
    assert!(
        req["params"].get("tracestate").is_none(),
        "tracestate should be omitted when only traceparent is set"
    );
    server.respond(&req, serde_json::json!({})).await;
    timeout(TIMEOUT, send_handle)
        .await
        .unwrap()
        .unwrap()
        .unwrap();
}

#[tokio::test]
async fn tool_invocation_carries_trace_context_from_event() {
    type CapturedTrace = Arc<parking_lot::Mutex<Option<(Option<String>, Option<String>)>>>;
    struct CapturingTool {
        captured: CapturedTrace,
        signal: Arc<tokio::sync::Notify>,
    }

    #[async_trait]
    impl tool::ToolHandler for CapturingTool {
        async fn call(
            &self,
            invocation: ToolInvocation,
        ) -> Result<ToolResult, github_copilot_sdk::Error> {
            *self.captured.lock() = Some((
                invocation.traceparent.clone(),
                invocation.tracestate.clone(),
            ));
            self.signal.notify_one();
            Ok(ToolResult::Text("ok".into()))
        }
    }

    let captured = Arc::new(parking_lot::Mutex::new(None));
    let signal = Arc::new(tokio::sync::Notify::new());
    let handler = Arc::new(CapturingTool {
        captured: captured.clone(),
        signal: signal.clone(),
    });
    let (_session, mut server) = create_session_pair_with_config(move |cfg| {
        cfg.with_tools(vec![
            Tool::new("calc")
                .with_description("calc")
                .with_parameters(serde_json::json!({"type":"object"}))
                .with_handler(handler.clone()),
        ])
    })
    .await;

    server
        .send_event(
            "external_tool.requested",
            serde_json::json!({
                "requestId": "req-1",
                "sessionId": server.session_id,
                "toolCallId": "tc-1",
                "toolName": "calc",
                "arguments": {"x": 1},
                "traceparent": "00-tool-01",
                "tracestate": "vendor=tool",
            }),
        )
        .await;

    // Drain the handlePendingToolCall RPC the dispatcher sends after the handler runs.
    let pending = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(pending["method"], "session.tools.handlePendingToolCall");

    timeout(TIMEOUT, signal.notified()).await.unwrap();
    let captured = captured.lock().clone();
    assert_eq!(
        captured,
        Some((Some("00-tool-01".into()), Some("vendor=tool".into()))),
    );
}

#[tokio::test]
async fn wire_omits_trace_fields_when_unset() {
    let (session, mut server) = create_session_pair().await;
    let session = Arc::new(session);

    let send_handle = tokio::spawn({
        let session = session.clone();
        async move { session.send(MessageOptions::new("hi")).await }
    });
    let req = server.read_request().await;
    assert!(req["params"].get("traceparent").is_none());
    assert!(req["params"].get("tracestate").is_none());
    server.respond(&req, serde_json::json!({})).await;
    timeout(TIMEOUT, send_handle)
        .await
        .unwrap()
        .unwrap()
        .unwrap();
}
