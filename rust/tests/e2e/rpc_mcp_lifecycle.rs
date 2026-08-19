use std::path::Path;

use github_copilot_sdk::rpc::{
    McpConfigureGitHubResult, McpIsServerRunningRequest, McpListToolsRequest,
    McpStartServersResult, McpStopServerRequest,
};
use github_copilot_sdk::session::Session;
use github_copilot_sdk::session_events::McpServerStatus;
use github_copilot_sdk::{Error, IndexMap, McpServerConfig, McpStdioServerConfig};
use serde::de::DeserializeOwned;
use serde_json::{Value, json};

use super::support::wait_for_condition;

#[tokio::test]
async fn should_list_tools_and_report_running_status_for_connected_server() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_mcp_lifecycle",
        "should_list_tools_and_report_running_status_for_connected_server",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let server_name = "rpc-lifecycle-list-server";
                let client = ctx.start_client().await;
                let session =
                    client
                        .create_session(ctx.approve_all_session_config().with_mcp_servers(
                            create_test_mcp_servers(ctx.repo_root(), server_name),
                        ))
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
                assert!(!tools.tools.is_empty());
                assert!(tools.tools.iter().all(|tool| !tool.name.trim().is_empty()));

                assert!(is_mcp_server_running(&session, server_name).await);
                assert!(
                    !is_mcp_server_running(
                        &session,
                        &format!("missing-{}", uuid::Uuid::new_v4().simple())
                    )
                    .await
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_throw_when_listing_tools_for_unconnected_server() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_mcp_lifecycle",
        "should_throw_when_listing_tools_for_unconnected_server",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let server_name = "rpc-lifecycle-unconnected-host";
                let client = ctx.start_client().await;
                let session =
                    client
                        .create_session(ctx.approve_all_session_config().with_mcp_servers(
                            create_test_mcp_servers(ctx.repo_root(), server_name),
                        ))
                        .await
                        .expect("create session");
                wait_for_mcp_server_status(&session, server_name, McpServerStatus::Connected).await;

                let err = session
                    .rpc()
                    .mcp()
                    .list_tools(McpListToolsRequest {
                        server_name: format!("missing-{}", uuid::Uuid::new_v4().simple()),
                    })
                    .await
                    .expect_err("missing server should fail");
                assert_error_contains(&err, "not connected");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_stop_running_mcp_server() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_mcp_lifecycle",
        "should_stop_running_mcp_server",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let server_name = "rpc-lifecycle-stop-server";
                let client = ctx.start_client().await;
                let session =
                    client
                        .create_session(ctx.approve_all_session_config().with_mcp_servers(
                            create_test_mcp_servers(ctx.repo_root(), server_name),
                        ))
                        .await
                        .expect("create session");
                wait_for_mcp_server_status(&session, server_name, McpServerStatus::Connected).await;
                assert!(is_mcp_server_running(&session, server_name).await);

                session
                    .rpc()
                    .mcp()
                    .stop_server(McpStopServerRequest {
                        server_name: server_name.to_string(),
                    })
                    .await
                    .expect("stop MCP server");

                wait_for_mcp_running(&session, server_name, false).await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

// TODO(cli-1.0.81): CLI 1.0.81 no longer installs an MCP config from the inline start
// payload, so `session.mcp.startServer` reports "has no installed config to start".
// Re-enable once the runtime fix ships.
#[ignore = "blocked on CLI 1.0.81 MCP installed-config regression"]
#[tokio::test]
async fn should_start_and_restart_mcp_server() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_mcp_lifecycle",
        "should_start_and_restart_mcp_server",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let host_server = "rpc-lifecycle-host-server";
                let client = ctx.start_client().await;
                let session =
                    client
                        .create_session(ctx.approve_all_session_config().with_mcp_servers(
                            create_test_mcp_servers(ctx.repo_root(), host_server),
                        ))
                        .await
                        .expect("create session");
                wait_for_mcp_server_status(&session, host_server, McpServerStatus::Connected).await;

                let started_server = "rpc-lifecycle-started-server";
                let config = test_mcp_server_config(ctx.repo_root());
                let config_value = serde_json::to_value(&config).expect("serialize MCP config");
                call_session_rpc(
                    &session,
                    "session.mcp.startServer",
                    json!({ "serverName": started_server, "config": config_value }),
                )
                .await
                .expect("start MCP server");
                wait_for_mcp_running(&session, started_server, true).await;

                let tools = session
                    .rpc()
                    .mcp()
                    .list_tools(McpListToolsRequest {
                        server_name: started_server.to_string(),
                    })
                    .await
                    .expect("list started MCP tools");
                assert!(!tools.tools.is_empty());

                let config_value = serde_json::to_value(&config).expect("serialize MCP config");
                call_session_rpc(
                    &session,
                    "session.mcp.restartServer",
                    json!({ "serverName": started_server, "config": config_value }),
                )
                .await
                .expect("restart MCP server");
                wait_for_mcp_running(&session, started_server, true).await;

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

// There is deliberately no e2e test for `session.mcp.registerExternalClient`. That method is
// marked `visibility: internal` in the shared API contract: its `client` and `transport` fields
// are live in-process MCP SDK instances, so it cannot be driven over JSON-RPC, and no SDK
// exposes it as a typed method. A raw-RPC test used to pass only because older CLIs routed
// internal methods generically; it never exercised a supported wire API.

#[tokio::test]
async fn should_reload_mcp_servers_with_config() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_mcp_lifecycle",
        "should_reload_mcp_servers_with_config",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let host_server = "rpc-lifecycle-reload-host";
                let client = ctx.start_client().await;
                let session =
                    client
                        .create_session(ctx.approve_all_session_config().with_mcp_servers(
                            create_test_mcp_servers(ctx.repo_root(), host_server),
                        ))
                        .await
                        .expect("create session");
                wait_for_mcp_server_status(&session, host_server, McpServerStatus::Connected).await;

                let result: McpStartServersResult = call_session_rpc_typed(
                    &session,
                    "session.mcp.reloadWithConfig",
                    json!({
                        "config": {
                            "mcpServers": {},
                            "disabledServers": []
                        }
                    }),
                )
                .await
                .expect("reload MCP with config");

                assert!(result.filtered_servers.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_configure_github_mcp_server() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rpc_mcp_lifecycle",
        "should_configure_github_mcp_server",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let host_server = "rpc-lifecycle-configure-host";
                let client = ctx.start_client().await;
                let session =
                    client
                        .create_session(ctx.approve_all_session_config().with_mcp_servers(
                            create_test_mcp_servers(ctx.repo_root(), host_server),
                        ))
                        .await
                        .expect("create session");
                wait_for_mcp_server_status(&session, host_server, McpServerStatus::Connected).await;

                let result: McpConfigureGitHubResult = call_session_rpc_typed(
                    &session,
                    "session.mcp.configureGitHub",
                    json!({ "authInfo": { "type": "api-key" } }),
                )
                .await
                .expect("configure GitHub MCP");

                assert!(!result.changed);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn create_test_mcp_servers(
    repo_root: &Path,
    server_name: &str,
) -> IndexMap<String, McpServerConfig> {
    IndexMap::from([(server_name.to_string(), test_mcp_server_config(repo_root))])
}

fn test_mcp_server_config(repo_root: &Path) -> McpServerConfig {
    let harness_dir = repo_root.join("test").join("harness");
    let server_path = harness_dir
        .join("test-mcp-server.mjs")
        .to_string_lossy()
        .to_string();
    McpServerConfig::Stdio(McpStdioServerConfig {
        tools: Some(vec!["*".to_string()]),
        command: if cfg!(windows) {
            "node.exe".to_string()
        } else {
            "node".to_string()
        },
        args: vec![server_path],
        working_directory: Some(harness_dir.to_string_lossy().to_string()),
        ..McpStdioServerConfig::default()
    })
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

async fn wait_for_mcp_running(session: &Session, server_name: &str, expected_running: bool) {
    wait_for_condition("MCP server running state", || async {
        is_mcp_server_running(session, server_name).await == expected_running
    })
    .await;
}

async fn is_mcp_server_running(session: &Session, server_name: &str) -> bool {
    session
        .rpc()
        .mcp()
        .is_server_running(McpIsServerRunningRequest {
            server_name: server_name.to_string(),
        })
        .await
        .expect("check MCP running")
        .running
}

async fn call_session_rpc(
    session: &Session,
    method: &'static str,
    mut params: Value,
) -> Result<Value, Error> {
    params["sessionId"] = json!(session.id());
    session.client().call(method, Some(params)).await
}

async fn call_session_rpc_typed<T: DeserializeOwned>(
    session: &Session,
    method: &'static str,
    params: Value,
) -> Result<T, Error> {
    let value = call_session_rpc(session, method, params).await?;
    Ok(serde_json::from_value(value)?)
}

fn assert_error_contains(err: &Error, expected: &str) {
    let message = err.to_string();
    assert!(
        !message.to_ascii_lowercase().contains("unhandled method"),
        "{message}"
    );
    assert!(
        message
            .to_ascii_lowercase()
            .contains(&expected.to_ascii_lowercase()),
        "expected error to contain {expected:?}, got {message}"
    );
}
static E2E: super::support::SharedE2eGroup =
    super::support::SharedE2eGroup::standard("rpc_mcp_lifecycle", 6);
