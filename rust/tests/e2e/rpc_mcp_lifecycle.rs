use std::collections::HashMap;
use std::path::Path;

use github_copilot_sdk::rpc::{
    McpConfigureGitHubResult, McpIsServerRunningRequest, McpListToolsRequest,
    McpStartServersResult, McpStopServerRequest,
};
use github_copilot_sdk::session::Session;
use github_copilot_sdk::session_events::McpServerStatus;
use github_copilot_sdk::{Error, McpServerConfig, McpStdioServerConfig};
use serde::de::DeserializeOwned;
use serde_json::{Value, json};

use super::support::{wait_for_condition, with_e2e_context};

#[tokio::test]
async fn should_list_tools_and_report_running_status_for_connected_server() {
    with_e2e_context(
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
    with_e2e_context(
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
    with_e2e_context(
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

#[tokio::test]
async fn should_start_and_restart_mcp_server() {
    with_e2e_context(
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

#[tokio::test]
async fn should_register_and_unregister_external_mcp_client() {
    with_e2e_context(
        "rpc_mcp_lifecycle",
        "should_register_and_unregister_external_mcp_client",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let host_server = "rpc-lifecycle-extclient-host";
                let client = ctx.start_client().await;
                let session =
                    client
                        .create_session(ctx.approve_all_session_config().with_mcp_servers(
                            create_test_mcp_servers(ctx.repo_root(), host_server),
                        ))
                        .await
                        .expect("create session");
                wait_for_mcp_server_status(&session, host_server, McpServerStatus::Connected).await;

                let external_name = "rpc-lifecycle-external-client";
                assert!(!is_mcp_server_running(&session, external_name).await);

                call_session_rpc(
                    &session,
                    "session.mcp.registerExternalClient",
                    json!({
                        "serverName": external_name,
                        "client": { "id": external_name },
                        "transport": { "kind": "in-process" },
                        "config": { "command": "noop" }
                    }),
                )
                .await
                .expect("register external MCP client");
                assert!(is_mcp_server_running(&session, external_name).await);

                call_session_rpc(
                    &session,
                    "session.mcp.unregisterExternalClient",
                    json!({ "serverName": external_name }),
                )
                .await
                .expect("unregister external MCP client");
                assert!(!is_mcp_server_running(&session, external_name).await);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_reload_mcp_servers_with_config() {
    with_e2e_context(
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
    with_e2e_context(
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

#[tokio::test]
async fn should_respond_to_mcp_oauth_request_without_pending_request() {
    with_e2e_context(
        "rpc_mcp_lifecycle",
        "should_respond_to_mcp_oauth_request_without_pending_request",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let host_server = "rpc-lifecycle-oauth-host";
                let client = ctx.start_client().await;
                let session =
                    client
                        .create_session(ctx.approve_all_session_config().with_mcp_servers(
                            create_test_mcp_servers(ctx.repo_root(), host_server),
                        ))
                        .await
                        .expect("create session");
                wait_for_mcp_server_status(&session, host_server, McpServerStatus::Connected).await;

                let result = call_session_rpc(
                    &session,
                    "session.mcp.oauth.respond",
                    json!({
                        "requestId": format!("missing-{}", uuid::Uuid::new_v4().simple())
                    }),
                )
                .await
                .expect("respond to missing MCP OAuth request");
                assert!(result.is_object());

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
) -> HashMap<String, McpServerConfig> {
    HashMap::from([(server_name.to_string(), test_mcp_server_config(repo_root))])
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
