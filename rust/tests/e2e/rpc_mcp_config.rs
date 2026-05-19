use github_copilot_sdk::generated::api_types::{
    McpConfigAddRequest, McpConfigDisableRequest, McpConfigEnableRequest, McpConfigRemoveRequest,
    McpConfigUpdateRequest,
};
use serde_json::json;

use super::support::with_e2e_context;

#[tokio::test]
async fn should_call_server_mcp_config_rpcs() {
    with_e2e_context(
        "rpc_mcp_config",
        "should_call_server_mcp_config_rpcs",
        |ctx| {
            Box::pin(async move {
                let server_name = "rust-sdk-test-mcp-config";
                let client = ctx.start_client().await;
                let config = client.rpc().mcp().config();
                let _ = config
                    .remove(McpConfigRemoveRequest {
                        name: server_name.to_string(),
                    })
                    .await;

                let initial = config.list().await.expect("initial list");
                assert!(!initial.servers.contains_key(server_name));

                config
                    .add(McpConfigAddRequest {
                        name: server_name.to_string(),
                        config: json!({ "command": "node", "args": [] }),
                    })
                    .await
                    .expect("add");
                let after_add = config.list().await.expect("list after add");
                assert!(after_add.servers.contains_key(server_name));

                config
                    .update(McpConfigUpdateRequest {
                        name: server_name.to_string(),
                        config: json!({ "command": "node", "args": ["--version"] }),
                    })
                    .await
                    .expect("update");
                let after_update = config.list().await.expect("list after update");
                let updated = after_update
                    .servers
                    .get(server_name)
                    .expect("updated server");
                assert_eq!(
                    updated.get("command").and_then(|v| v.as_str()),
                    Some("node")
                );
                assert_eq!(
                    updated
                        .get("args")
                        .and_then(|v| v.as_array())
                        .and_then(|args| args.first())
                        .and_then(|v| v.as_str()),
                    Some("--version")
                );

                config
                    .disable(McpConfigDisableRequest {
                        names: vec![server_name.to_string()],
                    })
                    .await
                    .expect("disable");
                config
                    .enable(McpConfigEnableRequest {
                        names: vec![server_name.to_string()],
                    })
                    .await
                    .expect("enable");
                config
                    .remove(McpConfigRemoveRequest {
                        name: server_name.to_string(),
                    })
                    .await
                    .expect("remove");

                let after_remove = config.list().await.expect("list after remove");
                assert!(!after_remove.servers.contains_key(server_name));

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_round_trip_http_mcp_oauth_config_rpc() {
    with_e2e_context(
        "rpc_mcp_config",
        "should_round_trip_http_mcp_oauth_config_rpc",
        |ctx| {
            Box::pin(async move {
                let server_name = "rust-sdk-http-oauth-mcp-config";
                let client = ctx.start_client().await;
                let config = client.rpc().mcp().config();
                let _ = config
                    .remove(McpConfigRemoveRequest {
                        name: server_name.to_string(),
                    })
                    .await;

                config
                    .add(McpConfigAddRequest {
                        name: server_name.to_string(),
                        config: json!({
                            "type": "http",
                            "url": "https://example.com/mcp",
                            "headers": { "Authorization": "Bearer token" },
                            "oauthClientId": "client-id",
                            "oauthPublicClient": false,
                            "oauthGrantType": "client_credentials",
                            "tools": ["*"],
                            "timeout": 3000
                        }),
                    })
                    .await
                    .expect("add");
                let after_add = config.list().await.expect("list after add");
                let added = after_add.servers.get(server_name).expect("added server");
                assert_eq!(added.get("type").and_then(|v| v.as_str()), Some("http"));
                assert_eq!(
                    added.get("url").and_then(|v| v.as_str()),
                    Some("https://example.com/mcp")
                );
                assert_eq!(
                    added
                        .get("headers")
                        .and_then(|v| v.get("Authorization"))
                        .and_then(|v| v.as_str()),
                    Some("Bearer token")
                );
                assert_eq!(
                    added.get("oauthClientId").and_then(|v| v.as_str()),
                    Some("client-id")
                );
                assert_eq!(
                    added.get("oauthPublicClient").and_then(|v| v.as_bool()),
                    Some(false)
                );
                assert_eq!(
                    added.get("oauthGrantType").and_then(|v| v.as_str()),
                    Some("client_credentials")
                );

                config
                    .update(McpConfigUpdateRequest {
                        name: server_name.to_string(),
                        config: json!({
                            "type": "http",
                            "url": "https://example.com/updated-mcp",
                            "oauthClientId": "updated-client-id",
                            "oauthPublicClient": true,
                            "oauthGrantType": "authorization_code",
                            "tools": ["updated-tool"],
                            "timeout": 4000
                        }),
                    })
                    .await
                    .expect("update");
                let after_update = config.list().await.expect("list after update");
                let updated = after_update
                    .servers
                    .get(server_name)
                    .expect("updated server");
                assert_eq!(
                    updated.get("url").and_then(|v| v.as_str()),
                    Some("https://example.com/updated-mcp")
                );
                assert_eq!(
                    updated.get("oauthClientId").and_then(|v| v.as_str()),
                    Some("updated-client-id")
                );
                assert_eq!(
                    updated.get("oauthPublicClient").and_then(|v| v.as_bool()),
                    Some(true)
                );
                assert_eq!(
                    updated.get("oauthGrantType").and_then(|v| v.as_str()),
                    Some("authorization_code")
                );
                assert_eq!(
                    updated
                        .get("tools")
                        .and_then(|v| v.as_array())
                        .and_then(|tools| tools.first())
                        .and_then(|v| v.as_str()),
                    Some("updated-tool")
                );
                assert_eq!(updated.get("timeout").and_then(|v| v.as_i64()), Some(4000));

                config
                    .remove(McpConfigRemoveRequest {
                        name: server_name.to_string(),
                    })
                    .await
                    .expect("remove");
                let after_remove = config.list().await.expect("list after remove");
                assert!(!after_remove.servers.contains_key(server_name));

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
