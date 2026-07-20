use std::collections::HashMap;
use std::path::PathBuf;
use std::process::Stdio;
use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::handler::{McpAuthHandler, McpAuthRequest, McpAuthResult};
use github_copilot_sdk::rpc::{McpAppsCallToolRequest, McpListToolsRequest};
use github_copilot_sdk::session::Session;
use github_copilot_sdk::session_events::{McpOauthRequestReason, McpServerStatus};
use github_copilot_sdk::{IndexMap, McpHttpServerConfig, McpServerConfig, RequestId, SessionId};
use parking_lot::Mutex;
use serde::Deserialize;
use serde_json::Value;
use tokio::io::{AsyncBufReadExt, BufReader};
use tokio::process::{Child, Command};
use tokio::sync::Notify;

use super::support::{wait_for_condition, with_e2e_context_no_snapshot};

const EXPECTED_TOKEN: &str = "sdk-host-token";
const REFRESH_TOKEN: &str = "sdk-host-token-refresh";
const UPSCOPE_TOKEN: &str = "sdk-host-token-upscope";
const REAUTH_TOKEN: &str = "sdk-host-token-reauth";

#[tokio::test]
async fn should_satisfy_mcp_oauth_using_host_provided_token() {
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let mut oauth_server = OAuthMcpServer::start(
                ctx.repo_root()
                    .join("test/harness/test-mcp-oauth-server.mjs"),
            )
            .await;
            let server_name = "oauth-protected-mcp";
            let handler = Arc::new(TokenAuthHandler::default());
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_mcp_auth_handler(handler.clone())
                        .with_mcp_servers(IndexMap::from([(
                            server_name.to_string(),
                            McpServerConfig::Http(McpHttpServerConfig {
                                tools: Some(vec!["*".to_string()]),
                                timeout: None,
                                url: format!("{}/mcp", oauth_server.url),
                                headers: HashMap::new(),
                            }),
                        )])),
                )
                .await
                .expect("create session");

            wait_for_mcp_server_status(&session, server_name, McpServerStatus::Connected).await;
            let tools = session
                .rpc()
                .mcp()
                .list_tools(McpListToolsRequest {
                    server_name: server_name.to_string(),
                })
                .await
                .expect("list MCP tools");
            assert!(tools.tools.iter().any(|tool| tool.name == "whoami"));

            let request = handler
                .request
                .lock()
                .clone()
                .expect("MCP auth handler should be invoked");
            assert_eq!(request.server_name, server_name);
            assert_eq!(request.server_url, format!("{}/mcp", oauth_server.url));
            assert_eq!(request.reason, McpOauthRequestReason::Initial);
            let www_authenticate = request
                .www_authenticate_params
                .expect("WWW-Authenticate params");
            assert_eq!(
                www_authenticate.resource_metadata_url,
                Some(format!(
                    "{}/.well-known/oauth-protected-resource",
                    oauth_server.url
                ))
            );
            assert_eq!(www_authenticate.scope.as_deref(), Some("mcp.read"));
            assert_eq!(www_authenticate.error.as_deref(), Some("invalid_token"));
            let metadata: Value = serde_json::from_str(
                request
                    .resource_metadata
                    .as_deref()
                    .expect("resource metadata"),
            )
            .expect("parse resource metadata");
            assert_eq!(metadata["resource"], format!("{}/mcp", oauth_server.url));

            let requests = oauth_server.requests().await;
            assert!(
                requests
                    .iter()
                    .any(|request| request.authorization.is_none())
            );
            assert!(requests.iter().any(|request| {
                request.authorization.as_deref() == Some(&format!("Bearer {EXPECTED_TOKEN}"))
            }));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
            oauth_server.stop().await;
        })
    })
    .await;
}

#[tokio::test]
async fn should_request_replacement_tokens_across_mcp_oauth_lifecycle() {
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let mut oauth_server = OAuthMcpServer::start(
                ctx.repo_root()
                    .join("test/harness/test-mcp-oauth-server.mjs"),
            )
            .await;
            let server_name = "oauth-lifecycle-mcp";
            let handler = Arc::new(LifecycleAuthHandler::default());
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_enable_mcp_apps(true)
                        .with_mcp_auth_handler(handler.clone())
                        .with_mcp_servers(IndexMap::from([(
                            server_name.to_string(),
                            McpServerConfig::Http(McpHttpServerConfig {
                                tools: Some(vec!["*".to_string()]),
                                timeout: None,
                                url: format!("{}/mcp", oauth_server.url),
                                headers: HashMap::new(),
                            }),
                        )])),
                )
                .await
                .expect("create session");

            wait_for_mcp_server_status(&session, server_name, McpServerStatus::Connected).await;
            call_whoami(&session, server_name, "refresh").await;
            call_whoami(&session, server_name, "upscope").await;
            call_whoami(&session, server_name, "reauth").await;

            assert_eq!(
                handler.reasons.lock().as_slice(),
                [
                    McpOauthRequestReason::Initial,
                    McpOauthRequestReason::Refresh,
                    McpOauthRequestReason::Upscope,
                    McpOauthRequestReason::Refresh,
                    McpOauthRequestReason::Reauth,
                ]
            );

            let requests = oauth_server.requests().await;
            assert!(requests.iter().any(|request| {
                request.authorization.as_deref() == Some(&format!("Bearer {REFRESH_TOKEN}"))
            }));
            assert!(requests.iter().any(|request| {
                request.authorization.as_deref() == Some(&format!("Bearer {UPSCOPE_TOKEN}"))
            }));
            assert!(requests.iter().any(|request| {
                request.authorization.as_deref() == Some(&format!("Bearer {REAUTH_TOKEN}"))
            }));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
            oauth_server.stop().await;
        })
    })
    .await;
}

#[tokio::test]
async fn should_cancel_pending_mcp_oauth_request() {
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let mut oauth_server = OAuthMcpServer::start(
                ctx.repo_root()
                    .join("test/harness/test-mcp-oauth-server.mjs"),
            )
            .await;
            let server_name = "oauth-cancelled-mcp";
            let handler = Arc::new(CancelAuthHandler::default());
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_mcp_auth_handler(handler.clone())
                        .with_mcp_servers(IndexMap::from([(
                            server_name.to_string(),
                            McpServerConfig::Http(McpHttpServerConfig {
                                tools: Some(vec!["*".to_string()]),
                                timeout: None,
                                url: format!("{}/mcp", oauth_server.url),
                                headers: HashMap::new(),
                            }),
                        )])),
                )
                .await
                .expect("create session");

            wait_for_mcp_server_status(&session, server_name, McpServerStatus::NeedsAuth).await;

            // The MCP connection is kicked off by session.create, but the SDK only registers its
            // `mcp.oauth_required` event interest once create returns. If the server's initial 401
            // wins that race, the runtime records `needs-auth` WITHOUT invoking the host callback,
            // so `handler.request` is briefly `None` even after `needs-auth` is observed. A later
            // auth retry (now that interest is registered) invokes the callback with the same
            // `Initial` reason. Wait for the callback rather than sampling it the instant
            // `needs-auth` first appears, which is what made this test flaky.
            wait_for_condition("MCP OAuth request reaching the host callback", || async {
                handler.request.lock().is_some()
            })
            .await;

            let request = handler
                .request
                .lock()
                .clone()
                .expect("MCP auth handler should be invoked");
            assert_eq!(request.server_name, server_name);
            assert_eq!(request.reason, McpOauthRequestReason::Initial);

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
            oauth_server.stop().await;
        })
    })
    .await;
}

#[tokio::test]
async fn should_resolve_pending_mcp_oauth_request_through_rpc() {
    with_e2e_context_no_snapshot(|ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let mut oauth_server = OAuthMcpServer::start(
                ctx.repo_root()
                    .join("test/harness/test-mcp-oauth-server.mjs"),
            )
            .await;
            let server_name = "oauth-direct-rpc-mcp";
            let observed_request = Arc::new(Mutex::new(None));
            let request_observed = Arc::new(Notify::new());
            let release_handler = Arc::new(Notify::new());
            let handler = Arc::new(BlockingAuthHandler {
                request: observed_request.clone(),
                request_observed: request_observed.clone(),
                release: release_handler.clone(),
            });
            let client = ctx.start_client().await;
            let session = client
                .create_session(
                    ctx.approve_all_session_config()
                        .with_enable_mcp_apps(true)
                        .with_mcp_auth_handler(handler)
                        .with_mcp_servers(IndexMap::from([(
                            server_name.to_string(),
                            McpServerConfig::Http(McpHttpServerConfig {
                                tools: Some(vec!["*".to_string()]),
                                timeout: None,
                                url: format!("{}/mcp", oauth_server.url),
                                headers: HashMap::new(),
                            }),
                        )])),
                )
                .await
                .expect("create session");

            let connected =
                wait_for_mcp_server_status(&session, server_name, McpServerStatus::Connected);
            tokio::pin!(connected);
            tokio::select! {
                () = request_observed.notified() => {}
                () = &mut connected => panic!("MCP server connected before OAuth request was observed"),
            }
            let request = observed_request
                .lock()
                .clone()
                .expect("MCP auth request");
            assert_eq!(request.server_name, server_name);
            assert_eq!(request.server_url, format!("{}/mcp", oauth_server.url));
            assert_eq!(request.reason, McpOauthRequestReason::Initial);
            let www_authenticate = request
                .www_authenticate_params
                .as_ref()
                .expect("WWW-Authenticate params");
            assert_eq!(
                www_authenticate.resource_metadata_url,
                Some(format!(
                    "{}/.well-known/oauth-protected-resource",
                    oauth_server.url
                ))
            );
            assert_eq!(www_authenticate.scope.as_deref(), Some("mcp.read"));
            assert_eq!(www_authenticate.error.as_deref(), Some("invalid_token"));

            let handled = session
                .rpc()
                .mcp()
                .oauth()
                .handle_pending_request(github_copilot_sdk::rpc::McpOauthHandlePendingRequest {
                    request_id: request.request_id,
                    result: github_copilot_sdk::rpc::McpOauthPendingRequestResponse::Token(
                        github_copilot_sdk::rpc::McpOauthPendingRequestResponseToken {
                            access_token: EXPECTED_TOKEN.to_string(),
                            expires_in: Some(3600),
                            kind: github_copilot_sdk::rpc::McpOauthPendingRequestResponseTokenKind::Token,
                            token_type: Some("Bearer".to_string()),
                        },
                    ),
                })
                .await
                .expect("handle pending MCP OAuth request");
            assert!(handled.success);

            release_handler.notify_one();
            connected.await;
            let tools = session
                .rpc()
                .mcp()
                .list_tools(McpListToolsRequest {
                    server_name: server_name.to_string(),
                })
                .await
                .expect("list MCP tools");
            assert!(tools.tools.iter().any(|tool| tool.name == "whoami"));
            let requests = oauth_server.requests().await;
            assert!(
                requests
                    .iter()
                    .any(|request| {
                        request.authorization.as_deref() == Some(&format!("Bearer {EXPECTED_TOKEN}"))
                    })
            );

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
            oauth_server.stop().await;
        })
    })
    .await;
}

#[derive(Default)]
struct TokenAuthHandler {
    request: Mutex<Option<McpAuthRequest>>,
}

#[async_trait]
impl McpAuthHandler for TokenAuthHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        request_id: RequestId,
        request: McpAuthRequest,
    ) -> McpAuthResult {
        assert_eq!(request.request_id, request_id);
        *self.request.lock() = Some(request);
        McpAuthResult::Token {
            access_token: EXPECTED_TOKEN.to_string(),
            token_type: Some("Bearer".to_string()),
            expires_in: Some(3600),
        }
    }
}

#[derive(Default)]
struct LifecycleAuthHandler {
    reasons: Mutex<Vec<McpOauthRequestReason>>,
    refresh_count: Mutex<usize>,
}

#[async_trait]
impl McpAuthHandler for LifecycleAuthHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        request_id: RequestId,
        request: McpAuthRequest,
    ) -> McpAuthResult {
        assert_eq!(request.request_id, request_id);
        let reason = request.reason.clone();
        self.reasons.lock().push(reason.clone());
        let token = match reason {
            McpOauthRequestReason::Refresh => {
                let www_authenticate = request
                    .www_authenticate_params
                    .as_ref()
                    .expect("refresh WWW-Authenticate params");
                assert_eq!(www_authenticate.resource_metadata_url, None);
                assert_eq!(www_authenticate.error.as_deref(), Some("invalid_token"));
                let mut refresh_count = self.refresh_count.lock();
                *refresh_count += 1;
                if *refresh_count > 1 {
                    return McpAuthResult::Cancelled;
                }
                REFRESH_TOKEN
            }
            McpOauthRequestReason::Upscope => {
                let www_authenticate = request
                    .www_authenticate_params
                    .as_ref()
                    .expect("upscope WWW-Authenticate params");
                assert!(
                    www_authenticate
                        .resource_metadata_url
                        .as_deref()
                        .is_some_and(|url| url.ends_with("/.well-known/oauth-protected-resource"))
                );
                assert_eq!(www_authenticate.scope.as_deref(), Some("mcp.write"));
                assert_eq!(
                    www_authenticate.error.as_deref(),
                    Some("insufficient_scope")
                );
                UPSCOPE_TOKEN
            }
            McpOauthRequestReason::Reauth => REAUTH_TOKEN,
            _ => EXPECTED_TOKEN,
        };
        McpAuthResult::Token {
            access_token: token.to_string(),
            token_type: None,
            expires_in: None,
        }
    }
}

#[derive(Default)]
struct CancelAuthHandler {
    request: Mutex<Option<McpAuthRequest>>,
}

#[async_trait]
impl McpAuthHandler for CancelAuthHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        request_id: RequestId,
        request: McpAuthRequest,
    ) -> McpAuthResult {
        assert_eq!(request.request_id, request_id);
        *self.request.lock() = Some(request);
        McpAuthResult::Cancelled
    }
}

struct BlockingAuthHandler {
    request: Arc<Mutex<Option<McpAuthRequest>>>,
    request_observed: Arc<Notify>,
    release: Arc<Notify>,
}

#[async_trait]
impl McpAuthHandler for BlockingAuthHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        request_id: RequestId,
        request: McpAuthRequest,
    ) -> McpAuthResult {
        assert_eq!(request.request_id, request_id);
        *self.request.lock() = Some(request);
        self.request_observed.notify_one();
        self.release.notified().await;
        McpAuthResult::Token {
            access_token: EXPECTED_TOKEN.to_string(),
            token_type: Some("Bearer".to_string()),
            expires_in: Some(3600),
        }
    }
}

#[derive(Deserialize)]
struct OAuthMcpRequest {
    authorization: Option<String>,
}

struct OAuthMcpServer {
    child: Child,
    url: String,
}

impl OAuthMcpServer {
    async fn start(script: PathBuf) -> Self {
        let mut child = Command::new("node")
            .arg(script)
            .env("EXPECTED_TOKEN", EXPECTED_TOKEN)
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .kill_on_drop(true)
            .spawn()
            .expect("start OAuth MCP server");
        let stdout = child.stdout.take().expect("OAuth MCP stdout");
        let mut lines = BufReader::new(stdout).lines();
        let line = tokio::time::timeout(std::time::Duration::from_secs(10), lines.next_line())
            .await
            .expect("OAuth MCP server startup timeout")
            .expect("read OAuth MCP startup line")
            .expect("OAuth MCP server stdout closed");
        let url = line
            .strip_prefix("Listening: ")
            .unwrap_or_else(|| panic!("unexpected OAuth MCP startup line: {line}"))
            .to_string();
        Self { child, url }
    }

    async fn requests(&self) -> Vec<OAuthMcpRequest> {
        let text = reqwest::get(format!("{}/__requests", self.url))
            .await
            .expect("fetch OAuth MCP requests")
            .error_for_status()
            .expect("OAuth MCP request status")
            .text()
            .await
            .expect("read OAuth MCP requests");
        serde_json::from_str(&text).expect("decode OAuth MCP requests")
    }

    async fn stop(&mut self) {
        let _ = self.child.kill().await;
        let _ = self.child.wait().await;
    }
}

async fn wait_for_mcp_server_status(
    session: &Session,
    server_name: &str,
    expected_status: McpServerStatus,
) {
    wait_for_condition("MCP server status", || async {
        session
            .rpc()
            .mcp()
            .list()
            .await
            .expect("list MCP servers")
            .servers
            .iter()
            .any(|server| server.name == server_name && server.status == expected_status)
    })
    .await;
}

async fn call_whoami(session: &Session, server_name: &str, scenario: &str) {
    let result = session
        .rpc()
        .mcp()
        .apps()
        .call_tool(McpAppsCallToolRequest {
            arguments: Some(HashMap::from([(
                "scenario".to_string(),
                serde_json::Value::String(scenario.to_string()),
            )])),
            origin_server_name: server_name.to_string(),
            server_name: server_name.to_string(),
            tool_name: "whoami".to_string(),
        })
        .await
        .expect("call whoami");
    let content = result.get("content").expect("whoami content");
    assert_eq!(
        content,
        &serde_json::json!([{ "type": "text", "text": "oauth-test-user" }])
    );
}
