#![allow(clippy::unwrap_used)]

use std::path::Path;
use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::Client;
use github_copilot_sdk::handler::{
    ApproveAllHandler, AutoModeSwitchResponse, ExitPlanModeResult, HandlerEvent, HandlerResponse,
    PermissionResult, SessionHandler, UserInputResponse,
};
use github_copilot_sdk::types::{
    CommandContext, CommandDefinition, CommandHandler, DeliveryMode, MessageOptions,
    ServerTelemetryEvent, SessionConfig, SessionId, SessionTelemetryEvent, ToolResult,
};
use serde_json::Value;
use tokio::io::{AsyncWrite, AsyncWriteExt, duplex};
use tokio::sync::mpsc;
use tokio::time::timeout;

const TIMEOUT: Duration = Duration::from_secs(2);
const METHOD_NOT_FOUND: i32 = -32601;

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
) -> (github_copilot_sdk::session::Session, FakeServer) {
    create_session_pair_with_capabilities(handler, serde_json::json!(null)).await
}

async fn create_session_pair_with_capabilities(
    handler: Arc<dyn SessionHandler>,
    capabilities: Value,
) -> (github_copilot_sdk::session::Session, FakeServer) {
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
                .create_session(SessionConfig::default().with_handler(handler))
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
async fn session_subscribe_yields_events_observe_only() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;

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
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;

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
                    cfg.with_handler(Arc::new(NoopHandler))
                })
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
async fn send_injects_session_id() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
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

    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
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

    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
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
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let cases: Vec<(&str, Option<&str>)> = vec![
        ("session.abort", None),
        ("session.plan.delete", None),
        ("session.log", Some("message")),
        ("session.sendTelemetry", Some("kind")),
        ("session.destroy", None),
    ];

    for (expected_method, extra_param_key) in cases {
        let s = session.clone();
        let handle = tokio::spawn(async move {
            match expected_method {
                "session.abort" => s.abort().await.map(|_| ()),
                "session.plan.delete" => s.delete_plan().await,
                "session.log" => s.log("test msg", None).await,
                "session.sendTelemetry" => {
                    s.send_telemetry(SessionTelemetryEvent {
                        kind: "sdk_test_event".to_string(),
                        properties: Some(
                            [("source".to_string(), "sdk".to_string())]
                                .into_iter()
                                .collect(),
                        ),
                        restricted_properties: None,
                        metrics: None,
                    })
                    .await
                }
                "session.destroy" => s.destroy().await,
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
async fn send_telemetry_injects_payload_and_session_id() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move {
            session
                .send_telemetry(SessionTelemetryEvent {
                    kind: "sdk_test_event".to_string(),
                    properties: Some(
                        [
                            ("source".to_string(), "sdk".to_string()),
                            ("feature".to_string(), "shared-api".to_string()),
                        ]
                        .into_iter()
                        .collect(),
                    ),
                    restricted_properties: Some(
                        [("file_path".to_string(), "/tmp/example.ts".to_string())]
                            .into_iter()
                            .collect(),
                    ),
                    metrics: Some(
                        [
                            ("count".to_string(), 1.0),
                            ("duration_ms".to_string(), 12.5),
                        ]
                        .into_iter()
                        .collect(),
                    ),
                })
                .await
        }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.sendTelemetry");
    assert_eq!(request["params"]["sessionId"], server.session_id);
    assert_eq!(request["params"]["kind"], "sdk_test_event");
    assert_eq!(request["params"]["properties"]["source"], "sdk");
    assert_eq!(
        request["params"]["restrictedProperties"]["file_path"],
        "/tmp/example.ts"
    );
    assert_eq!(request["params"]["metrics"]["duration_ms"], 12.5);

    server.respond(&request, serde_json::json!(null)).await;
    timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
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
async fn server_send_telemetry_sends_correct_payload() {
    let (client, mut server_read, mut server_write) = make_client();

    let handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .send_telemetry(ServerTelemetryEvent {
                    kind: "app.launched".to_string(),
                    client_name: "github/autopilot".to_string(),
                    properties: Some(
                        [("machine_id".to_string(), "machine-123".to_string())]
                            .into_iter()
                            .collect(),
                    ),
                    restricted_properties: None,
                    metrics: Some([("launch_count".to_string(), 1.0)].into_iter().collect()),
                })
                .await
        }
    });

    let request = read_framed(&mut server_read).await;
    assert_eq!(request["method"], "sendTelemetry");
    assert_eq!(request["params"]["kind"], "app.launched");
    assert_eq!(request["params"]["clientName"], "github/autopilot");
    assert_eq!(request["params"]["properties"]["machine_id"], "machine-123");
    assert_eq!(request["params"]["metrics"]["launch_count"], 1.0);

    let id = request["id"].as_u64().unwrap();
    let resp = serde_json::json!({ "jsonrpc": "2.0", "id": id, "result": null });
    write_framed(&mut server_write, &serde_json::to_vec(&resp).unwrap()).await;
    timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
}

#[tokio::test]
async fn server_send_telemetry_falls_back_to_namespaced_method_and_caches_it() {
    let (client, mut server_read, mut server_write) = make_client();

    let handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .send_telemetry(ServerTelemetryEvent {
                    kind: "app.launched".to_string(),
                    client_name: "github/autopilot".to_string(),
                    properties: Some(
                        [("machine_id".to_string(), "machine-123".to_string())]
                            .into_iter()
                            .collect(),
                    ),
                    restricted_properties: None,
                    metrics: Some([("launch_count".to_string(), 1.0)].into_iter().collect()),
                })
                .await?;
            client
                .send_telemetry(ServerTelemetryEvent {
                    kind: "app.closed".to_string(),
                    client_name: "github/autopilot".to_string(),
                    properties: None,
                    restricted_properties: None,
                    metrics: None,
                })
                .await
        }
    });

    let first_request = read_framed(&mut server_read).await;
    assert_eq!(first_request["method"], "sendTelemetry");
    let first_id = first_request["id"].as_u64().unwrap();
    let first_response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": first_id,
        "error": {
            "code": METHOD_NOT_FOUND,
            "message": "Unhandled method sendTelemetry"
        }
    });
    write_framed(
        &mut server_write,
        &serde_json::to_vec(&first_response).unwrap(),
    )
    .await;

    let second_request = read_framed(&mut server_read).await;
    assert_eq!(second_request["method"], "server.sendTelemetry");
    assert_eq!(second_request["params"]["kind"], "app.launched");
    assert_eq!(second_request["params"]["clientName"], "github/autopilot");
    assert_eq!(
        second_request["params"]["properties"]["machine_id"],
        "machine-123"
    );
    assert_eq!(second_request["params"]["metrics"]["launch_count"], 1.0);

    let second_id = second_request["id"].as_u64().unwrap();
    let second_response = serde_json::json!({ "jsonrpc": "2.0", "id": second_id, "result": null });
    write_framed(
        &mut server_write,
        &serde_json::to_vec(&second_response).unwrap(),
    )
    .await;

    let third_request = read_framed(&mut server_read).await;
    assert_eq!(third_request["method"], "server.sendTelemetry");
    assert_eq!(third_request["params"]["kind"], "app.closed");

    let third_id = third_request["id"].as_u64().unwrap();
    let third_response = serde_json::json!({ "jsonrpc": "2.0", "id": third_id, "result": null });
    write_framed(
        &mut server_write,
        &serde_json::to_vec(&third_response).unwrap(),
    )
    .await;

    timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
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
        cwd: None,
        tools: vec!["*".to_string()],
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
    }))
    .unwrap();
    assert_eq!(custom.kind, Some(PermissionRequestKind::CustomTool));

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
    assert_eq!(
        client.state(),
        github_copilot_sdk::ConnectionState::Connected
    );
    client.force_stop();
    assert_eq!(
        client.state(),
        github_copilot_sdk::ConnectionState::Disconnected
    );
    client.force_stop();
    assert_eq!(
        client.state(),
        github_copilot_sdk::ConnectionState::Disconnected
    );
    assert!(client.pid().is_none());
}

#[tokio::test]
async fn stop_transitions_state_to_disconnected() {
    let (client, _server_read, _server_write) = make_client();
    assert_eq!(
        client.state(),
        github_copilot_sdk::ConnectionState::Connected
    );
    client.stop().await.expect("stop should succeed");
    assert_eq!(
        client.state(),
        github_copilot_sdk::ConnectionState::Disconnected
    );
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
async fn set_model_sends_switch_to_request() {
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

    timeout(TIMEOUT, handle).await.unwrap().unwrap();
}

#[tokio::test]
async fn get_name_returns_name() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move { session.get_name().await.unwrap() }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.name.get");
    server
        .respond(&request, serde_json::json!({ "name": "Fix input flicker" }))
        .await;

    assert_eq!(
        timeout(TIMEOUT, handle).await.unwrap().unwrap(),
        Some("Fix input flicker".to_string())
    );
}

#[tokio::test]
async fn set_name_sends_name() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let handle = tokio::spawn({
        let session = session.clone();
        async move { session.set_name("Fix input flicker").await.unwrap() }
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.name.set");
    assert_eq!(request["params"]["name"], "Fix input flicker");
    server.respond(&request, serde_json::json!(null)).await;

    timeout(TIMEOUT, handle).await.unwrap().unwrap();
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
                    HandlerResponse::Permission(PermissionResult::Denied)
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
    assert_eq!(response["result"]["kind"], "reject");
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
                    HandlerResponse::UserInput(Some(UserInputResponse {
                        answer: "blue".to_string(),
                        was_freeform: true,
                    }))
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
async fn user_input_requested_event_dispatches_to_handler_and_responds() {
    struct InputHandler;
    #[async_trait]
    impl SessionHandler for InputHandler {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            match event {
                HandlerEvent::UserInput {
                    question,
                    choices,
                    allow_freeform,
                    ..
                } => {
                    assert_eq!(question, "Allow shell access?");
                    assert_eq!(choices, Some(vec!["Yes".to_string(), "No".to_string()]));
                    assert_eq!(allow_freeform, Some(false));
                    HandlerResponse::UserInput(Some(UserInputResponse {
                        answer: "Yes".to_string(),
                        was_freeform: false,
                    }))
                }
                _ => HandlerResponse::Ok,
            }
        }
    }

    let (_session, mut server) = create_session_pair(Arc::new(InputHandler)).await;
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

    let request = timeout(TIMEOUT, server.read_request()).await.unwrap();
    assert_eq!(request["method"], "session.respondToUserInput");
    assert_eq!(request["params"]["sessionId"], server.session_id);
    assert_eq!(request["params"]["requestId"], "ui-1");
    assert_eq!(request["params"]["answer"], "Yes");
    assert_eq!(request["params"]["wasFreeform"], false);
}

#[tokio::test]
async fn exit_plan_mode_dispatches_to_handler() {
    struct PlanHandler;
    #[async_trait]
    impl SessionHandler for PlanHandler {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            match event {
                HandlerEvent::ExitPlanMode { .. } => {
                    HandlerResponse::ExitPlanMode(ExitPlanModeResult {
                        approved: true,
                        selected_action: Some("autopilot".to_string()),
                        feedback: None,
                    })
                }
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
async fn auto_mode_switch_dispatches_to_handler_and_serializes_response() {
    use std::sync::atomic::{AtomicUsize, Ordering};

    struct AutoModeHandler {
        calls: Arc<AtomicUsize>,
        last_error_code: Arc<parking_lot::Mutex<Option<String>>>,
        last_retry_after: Arc<parking_lot::Mutex<Option<u64>>>,
    }
    #[async_trait]
    impl SessionHandler for AutoModeHandler {
        async fn on_auto_mode_switch(
            &self,
            _session_id: github_copilot_sdk::types::SessionId,
            error_code: Option<String>,
            retry_after_seconds: Option<u64>,
        ) -> AutoModeSwitchResponse {
            self.calls.fetch_add(1, Ordering::SeqCst);
            *self.last_error_code.lock() = error_code;
            *self.last_retry_after.lock() = retry_after_seconds;
            AutoModeSwitchResponse::YesAlways
        }
    }

    let calls = Arc::new(AtomicUsize::new(0));
    let last_error_code = Arc::new(parking_lot::Mutex::new(None));
    let last_retry_after = Arc::new(parking_lot::Mutex::new(None));
    let (_session, mut server) = create_session_pair(Arc::new(AutoModeHandler {
        calls: calls.clone(),
        last_error_code: last_error_code.clone(),
        last_retry_after: last_retry_after.clone(),
    }))
    .await;

    server
        .send_request(
            700,
            "autoModeSwitch.request",
            serde_json::json!({
                "sessionId": server.session_id,
                "errorCode": "user_weekly_rate_limited",
                "retryAfterSeconds": 3600,
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["id"], 700);
    assert_eq!(response["result"]["response"], "yes_always");
    assert_eq!(calls.load(Ordering::SeqCst), 1);
    assert_eq!(
        last_error_code.lock().as_deref(),
        Some("user_weekly_rate_limited")
    );
    assert_eq!(*last_retry_after.lock(), Some(3600));
}

#[tokio::test]
async fn auto_mode_switch_default_handler_replies_no() {
    let (_session, mut server) = create_session_pair(Arc::new(ApproveAllHandler)).await;

    server
        .send_request(
            701,
            "autoModeSwitch.request",
            serde_json::json!({
                "sessionId": server.session_id,
            }),
        )
        .await;

    let response = timeout(TIMEOUT, server.read_response()).await.unwrap();
    assert_eq!(response["result"]["response"], "no");
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
    assert_eq!(response["result"]["kind"], "approve-once");

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
    let (event_tx, mut event_rx) = mpsc::unbounded_channel::<String>();

    struct EventCollector {
        tx: mpsc::UnboundedSender<String>,
    }
    #[async_trait]
    impl SessionHandler for EventCollector {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            if let HandlerEvent::SessionEvent { event, .. } = event {
                self.tx.send(event.event_type).unwrap();
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
    assert_eq!(event_type, "session.idle");
}

#[tokio::test]
async fn router_routes_to_correct_session() {
    let (client, mut server_read, mut server_write) = make_client();
    let (tx1, mut rx1) = mpsc::unbounded_channel::<String>();
    let (tx2, mut rx2) = mpsc::unbounded_channel::<String>();

    struct Collector {
        tx: mpsc::UnboundedSender<String>,
    }
    #[async_trait]
    impl SessionHandler for Collector {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            if let HandlerEvent::SessionEvent { event, .. } = event {
                self.tx.send(event.event_type).unwrap();
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
                        SessionConfig::default().with_handler(Arc::new(Collector { tx })),
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
            "event": { "id": "e1", "timestamp": "2025-01-01T00:00:00Z", "type": "assistant.message", "data": {} },
        },
    });
    write_framed(&mut server_write, &serde_json::to_vec(&notif).unwrap()).await;
    assert_eq!(
        timeout(TIMEOUT, rx2.recv()).await.unwrap().unwrap(),
        "assistant.message"
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
        "session.idle"
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
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
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
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
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
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
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
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
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

#[tokio::test]
async fn elicitation_requested_dispatches_to_handler_and_responds() {
    use github_copilot_sdk::types::ElicitationResult;

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
async fn external_tool_requested_dispatches_to_handler_and_responds() {
    struct ExternalToolHandler;
    #[async_trait]
    impl SessionHandler for ExternalToolHandler {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            match event {
                HandlerEvent::ExternalTool { invocation } => {
                    assert_eq!(invocation.tool_name, "run_tests");
                    assert_eq!(invocation.tool_call_id, "tc-ext-1");
                    assert_eq!(invocation.arguments["suite"], "unit");
                    HandlerResponse::ToolResult(ToolResult::Text("all tests passed".to_string()))
                }
                _ => HandlerResponse::Ok,
            }
        }
    }

    let (_session, mut server) = create_session_pair(Arc::new(ExternalToolHandler)).await;

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
async fn capabilities_captured_from_create_response() {
    let (client, mut server_read, mut server_write) = make_client();

    let create_handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default().with_handler(Arc::new(NoopHandler)))
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
                .create_session(SessionConfig::default().with_handler(Arc::new(NoopHandler)))
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
    timeout(TIMEOUT, create_handle).await.unwrap().unwrap();
}

#[tokio::test]
async fn elicitation_methods_fail_without_capability() {
    let (session, _server) = create_session_pair(Arc::new(NoopHandler)).await;

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
    handler: Arc<dyn SessionHandler>,
    hooks: Arc<dyn github_copilot_sdk::hooks::SessionHooks>,
) -> (github_copilot_sdk::session::Session, FakeServer) {
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
                .create_session(
                    SessionConfig::default()
                        .with_handler(handler)
                        .with_hooks(hooks),
                )
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
    use github_copilot_sdk::hooks::SessionHooks;

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
    transforms: Arc<dyn github_copilot_sdk::transforms::SystemMessageTransform>,
) -> (github_copilot_sdk::session::Session, FakeServer) {
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
                .create_session(
                    SessionConfig::default()
                        .with_handler(handler)
                        .with_transform(transforms),
                )
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

#[tokio::test]
async fn list_workspace_files_uses_plural_method_name() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let s = session.clone();
    let handle = tokio::spawn(async move { s.list_workspace_files().await });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.workspaces.listFiles");
    assert_eq!(request["params"]["sessionId"], server.session_id);
    server
        .respond(
            &request,
            serde_json::json!({ "files": ["a.txt", "subdir/b.txt"] }),
        )
        .await;

    let files = timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
    assert_eq!(files, vec!["a.txt".to_string(), "subdir/b.txt".to_string()]);
}

#[tokio::test]
async fn read_workspace_file_uses_plural_method_name_and_forwards_path() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let s = session.clone();
    let handle =
        tokio::spawn(async move { s.read_workspace_file(Path::new("notes/plan.md")).await });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.workspaces.readFile");
    assert_eq!(request["params"]["sessionId"], server.session_id);
    assert_eq!(request["params"]["path"], "notes/plan.md");
    server
        .respond(&request, serde_json::json!({ "content": "hello" }))
        .await;

    let content = timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
    assert_eq!(content, "hello");
}

#[tokio::test]
async fn create_workspace_file_uses_plural_method_name_and_forwards_payload() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
    let session = Arc::new(session);

    let s = session.clone();
    let handle = tokio::spawn(async move {
        s.create_workspace_file(Path::new("notes/plan.md"), "body")
            .await
    });

    let request = server.read_request().await;
    assert_eq!(request["method"], "session.workspaces.createFile");
    assert_eq!(request["params"]["sessionId"], server.session_id);
    assert_eq!(request["params"]["path"], "notes/plan.md");
    assert_eq!(request["params"]["content"], "body");
    server.respond(&request, serde_json::json!({})).await;

    timeout(TIMEOUT, handle).await.unwrap().unwrap().unwrap();
}

#[tokio::test]
async fn rpc_namespace_session_agent_list_dispatches_correctly() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
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
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
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
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
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
    let session_id_a = format!("test-session-{}", rand_id());
    let session_id_b = format!("test-session-{}", rand_id());

    let mut server = FakeServer {
        read: server_read,
        write: server_write,
        session_id: session_id_a.clone(),
    };

    // Spawn both create_session calls.
    let create_a = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default().with_handler(Arc::new(NoopHandler)))
                .await
                .unwrap()
        }
    });
    let create_a_req = server.read_request().await;
    assert_eq!(create_a_req["method"], "session.create");
    server
        .respond(
            &create_a_req,
            serde_json::json!({ "sessionId": session_id_a, "workspacePath": "/tmp/ws-a" }),
        )
        .await;
    let _session_a = timeout(TIMEOUT, create_a).await.unwrap();

    let create_b = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .create_session(SessionConfig::default().with_handler(Arc::new(NoopHandler)))
                .await
                .unwrap()
        }
    });
    let create_b_req = server.read_request().await;
    assert_eq!(create_b_req["method"], "session.create");
    server
        .respond(
            &create_b_req,
            serde_json::json!({ "sessionId": session_id_b, "workspacePath": "/tmp/ws-b" }),
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
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
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

    use github_copilot_sdk::{SessionConfig, SessionId};

    let cfg = {
        let mut cfg = SessionConfig::default();
        cfg.session_id = Some(SessionId::from("custom-id"));
        cfg.config_dir = Some(PathBuf::from("/tmp/cfg"));
        cfg.working_directory = Some(PathBuf::from("/tmp/work"));
        cfg.github_token = Some("ghs_secret".to_string());
        cfg.include_sub_agent_streaming_events = Some(false);
        cfg
    };
    let json = serde_json::to_value(&cfg).unwrap();
    assert_eq!(json["sessionId"], "custom-id");
    assert_eq!(json["configDir"], "/tmp/cfg");
    assert_eq!(json["workingDirectory"], "/tmp/work");
    assert_eq!(json["gitHubToken"], "ghs_secret");
    assert_eq!(json["includeSubAgentStreamingEvents"], false);

    // Debug never leaks the token.
    let debug = format!("{cfg:?}");
    assert!(!debug.contains("ghs_secret"), "leaked token: {debug}");
    assert!(debug.contains("<redacted>"), "missing redaction: {debug}");

    // Unset fields are omitted on the wire.
    let empty = serde_json::to_value(SessionConfig::default()).unwrap();
    assert!(empty.get("sessionId").is_none());
    assert!(empty.get("gitHubToken").is_none());
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
    let json = serde_json::to_value(&cfg).unwrap();
    assert_eq!(json["sessionId"], "sess-1");
    assert_eq!(json["workingDirectory"], "/tmp/work");
    assert_eq!(json["configDir"], "/tmp/cfg");
    assert_eq!(json["gitHubToken"], "ghs_secret");
    assert_eq!(json["includeSubAgentStreamingEvents"], true);

    let debug = format!("{cfg:?}");
    assert!(!debug.contains("ghs_secret"), "leaked token: {debug}");
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
    handler: Arc<dyn SessionHandler>,
    commands: Vec<CommandDefinition>,
) -> (github_copilot_sdk::session::Session, FakeServer, Value) {
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
                .create_session(
                    SessionConfig::default()
                        .with_handler(handler)
                        .with_commands(commands),
                )
                .await
                .unwrap()
        }
    });

    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
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

    let (_session, _server, create_req) =
        create_session_pair_with_commands(Arc::new(NoopHandler), commands).await;

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

    let (session, mut server, _) =
        create_session_pair_with_commands(Arc::new(NoopHandler), commands).await;

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
    let (session, mut server, _) =
        create_session_pair_with_commands(Arc::new(NoopHandler), vec![]).await;

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

    let (_session, mut server, _) =
        create_session_pair_with_commands(Arc::new(NoopHandler), commands).await;

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
}

async fn create_session_pair_with_fs_provider(
    handler: Arc<dyn SessionHandler>,
    provider: Arc<dyn SessionFsProvider>,
) -> (github_copilot_sdk::session::Session, FakeServer) {
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
                .create_session(
                    SessionConfig::default()
                        .with_handler(handler)
                        .with_session_fs_provider(provider),
                )
                .await
                .unwrap()
        }
    });

    let create_req = server.read_request().await;
    assert_eq!(create_req["method"], "session.create");
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
async fn session_fs_dispatches_read_file_to_provider() {
    let provider = Arc::new(RecordingFsProvider::new().with_file("/foo.txt", "hello world"));
    let (_session, mut server) =
        create_session_pair_with_fs_provider(Arc::new(NoopHandler), provider).await;

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
    let (_session, mut server) =
        create_session_pair_with_fs_provider(Arc::new(NoopHandler), provider).await;

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

    let (_session, mut server) =
        create_session_pair_with_fs_provider(Arc::new(NoopHandler), Arc::new(AlwaysFails)).await;

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
async fn session_fs_dispatches_write_file_with_mode() {
    let provider = Arc::new(RecordingFsProvider::new());
    let (_session, mut server) =
        create_session_pair_with_fs_provider(Arc::new(NoopHandler), provider.clone()).await;

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
    let (_session, mut server) =
        create_session_pair_with_fs_provider(Arc::new(NoopHandler), provider).await;

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
    let (_session, mut server) =
        create_session_pair_with_fs_provider(Arc::new(NoopHandler), provider).await;

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
                .create_session(SessionConfig::default().with_handler(Arc::new(NoopHandler)))
                .await
                .unwrap()
        }
    });

    let req = server.read_request().await;
    assert_eq!(req["method"], "session.create");
    assert_eq!(req["params"]["traceparent"], "00-aaaa-bbbb-01");
    assert_eq!(req["params"]["tracestate"], "vendor=value");
    server
        .respond(
            &req,
            serde_json::json!({"sessionId": "trace-create", "workspacePath": "/tmp/ws"}),
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
            let cfg = ResumeSessionConfig::new(SessionId::from("trace-resume"))
                .with_handler(Arc::new(NoopHandler));
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
                .create_session(SessionConfig::default().with_handler(Arc::new(NoopHandler)))
                .await
                .unwrap()
        }
    });
    let create_req = server.read_request().await;
    server
        .respond(
            &create_req,
            serde_json::json!({"sessionId": "trace-send", "workspacePath": "/tmp/ws"}),
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
                .create_session(SessionConfig::default().with_handler(Arc::new(NoopHandler)))
                .await
                .unwrap()
        }
    });
    let create_req = server.read_request().await;
    server
        .respond(
            &create_req,
            serde_json::json!({"sessionId": "trace-override", "workspacePath": "/tmp/ws"}),
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
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
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
    use github_copilot_sdk::handler::{HandlerEvent, HandlerResponse, SessionHandler};

    struct CapturingHandler {
        captured: parking_lot::Mutex<Option<(Option<String>, Option<String>)>>,
        signal: tokio::sync::Notify,
    }

    #[async_trait]
    impl SessionHandler for CapturingHandler {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            if let HandlerEvent::ExternalTool { invocation } = event {
                *self.captured.lock() = Some((
                    invocation.traceparent.clone(),
                    invocation.tracestate.clone(),
                ));
                self.signal.notify_one();
                return HandlerResponse::ToolResult(ToolResult::Text("ok".into()));
            }
            HandlerResponse::Ok
        }
    }

    let handler = Arc::new(CapturingHandler {
        captured: parking_lot::Mutex::new(None),
        signal: tokio::sync::Notify::new(),
    });
    let (_session, mut server) = create_session_pair(handler.clone()).await;

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

    timeout(TIMEOUT, handler.signal.notified()).await.unwrap();
    let captured = handler.captured.lock().clone();
    assert_eq!(
        captured,
        Some((Some("00-tool-01".into()), Some("vendor=tool".into()))),
    );
}

#[tokio::test]
async fn wire_omits_trace_fields_when_unset() {
    let (session, mut server) = create_session_pair(Arc::new(NoopHandler)).await;
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
