use std::collections::HashMap;
use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::hooks::{
    HookContext, PreMcpToolCallInput, PreMcpToolCallOutput, SessionHooks,
};
use github_copilot_sdk::{McpServerConfig, McpStdioServerConfig};
use serde_json::{Value, json};
use tokio::sync::mpsc;

use super::support::{assistant_message_content, recv_with_timeout, with_e2e_context};

fn meta_echo_mcp_servers(repo_root: &std::path::Path) -> HashMap<String, McpServerConfig> {
    let harness_dir = repo_root.join("test").join("harness");
    let server_path = harness_dir
        .join("test-mcp-meta-echo-server.mjs")
        .to_string_lossy()
        .to_string();
    HashMap::from([(
        "meta-echo".to_string(),
        McpServerConfig::Stdio(McpStdioServerConfig {
            tools: vec!["*".to_string()],
            command: if cfg!(windows) {
                "node.exe".to_string()
            } else {
                "node".to_string()
            },
            args: vec![server_path],
            working_directory: Some(harness_dir.to_string_lossy().to_string()),
            ..McpStdioServerConfig::default()
        }),
    )])
}

struct SetMetaHooks {
    tx: mpsc::UnboundedSender<PreMcpToolCallInput>,
}

#[async_trait]
impl SessionHooks for SetMetaHooks {
    async fn on_pre_mcp_tool_call(
        &self,
        input: PreMcpToolCallInput,
        _ctx: HookContext,
    ) -> Option<PreMcpToolCallOutput> {
        let _ = self.tx.send(input);
        Some(PreMcpToolCallOutput {
            meta_to_use: Some(json!({"injected": "by-hook", "source": "test"})),
        })
    }
}

struct ReplaceMetaHooks {
    tx: mpsc::UnboundedSender<PreMcpToolCallInput>,
}

#[async_trait]
impl SessionHooks for ReplaceMetaHooks {
    async fn on_pre_mcp_tool_call(
        &self,
        input: PreMcpToolCallInput,
        _ctx: HookContext,
    ) -> Option<PreMcpToolCallOutput> {
        let _ = self.tx.send(input);
        Some(PreMcpToolCallOutput {
            meta_to_use: Some(json!({"completely": "replaced"})),
        })
    }
}

struct RemoveMetaHooks {
    tx: mpsc::UnboundedSender<PreMcpToolCallInput>,
}

#[async_trait]
impl SessionHooks for RemoveMetaHooks {
    async fn on_pre_mcp_tool_call(
        &self,
        input: PreMcpToolCallInput,
        _ctx: HookContext,
    ) -> Option<PreMcpToolCallOutput> {
        let _ = self.tx.send(input);
        Some(PreMcpToolCallOutput {
            meta_to_use: Some(Value::Null),
        })
    }
}

#[tokio::test]
async fn should_set_meta_via_premcptoolcall_hook() {
    with_e2e_context(
        "pre_mcp_tool_call_hook",
        "should_set_meta_via_premcptoolcall_hook",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (tx, mut rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_mcp_servers(meta_echo_mcp_servers(ctx.repo_root()))
                            .with_hooks(Arc::new(SetMetaHooks { tx })),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait(
                        "Use the meta-echo/echo_meta tool with value 'test-set'. Reply with just the raw tool result.",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&answer);
                assert!(
                    content.contains("injected"),
                    "Expected 'injected' in response, got: {content}"
                );
                assert!(
                    content.contains("by-hook"),
                    "Expected 'by-hook' in response, got: {content}"
                );

                let input = recv_with_timeout(&mut rx, "preMcpToolCall hook").await;
                assert_eq!(input.server_name, "meta-echo");
                assert_eq!(input.tool_name, "echo_meta");
                assert!(!input.working_directory.as_os_str().is_empty());
                assert!(input.timestamp > 0);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_replace_meta_via_premcptoolcall_hook() {
    with_e2e_context(
        "pre_mcp_tool_call_hook",
        "should_replace_meta_via_premcptoolcall_hook",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (tx, mut rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_mcp_servers(meta_echo_mcp_servers(ctx.repo_root()))
                            .with_hooks(Arc::new(ReplaceMetaHooks { tx })),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait(
                        "Use the meta-echo/echo_meta tool with value 'test-replace'. Reply with just the raw tool result.",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&answer);
                assert!(
                    content.contains("completely"),
                    "Expected 'completely' in response, got: {content}"
                );
                assert!(
                    content.contains("replaced"),
                    "Expected 'replaced' in response, got: {content}"
                );

                let input = recv_with_timeout(&mut rx, "preMcpToolCall hook").await;
                assert_eq!(input.server_name, "meta-echo");
                assert_eq!(input.tool_name, "echo_meta");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_remove_meta_via_premcptoolcall_hook() {
    with_e2e_context(
        "pre_mcp_tool_call_hook",
        "should_remove_meta_via_premcptoolcall_hook",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (tx, mut rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_mcp_servers(meta_echo_mcp_servers(ctx.repo_root()))
                            .with_hooks(Arc::new(RemoveMetaHooks { tx })),
                    )
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait(
                        "Use the meta-echo/echo_meta tool with value 'test-remove'. Reply with just the raw tool result.",
                    )
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&answer);
                assert!(
                    content.contains("\"meta\":null"),
                    "Expected '\"meta\":null' in response, got: {content}"
                );
                assert!(
                    content.contains("test-remove"),
                    "Expected 'test-remove' in response, got: {content}"
                );

                let input = recv_with_timeout(&mut rx, "preMcpToolCall hook").await;
                assert_eq!(input.server_name, "meta-echo");
                assert_eq!(input.tool_name, "echo_meta");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
