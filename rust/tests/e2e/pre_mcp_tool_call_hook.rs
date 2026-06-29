use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::hooks::{
    HookContext, PreMcpToolCallInput, PreMcpToolCallOutput, SessionHooks,
};
use serde_json::json;

use super::support::{assert_unsupported_hooks_error, with_e2e_context};

#[tokio::test]
async fn rejects_sdk_premcptoolcall_callback_hooks() {
    with_e2e_context(
        "pre_mcp_tool_call_hook",
        "rejects_sdk_premcptoolcall_callback_hooks",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                match client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_hooks(Arc::new(PreMcpHooks)),
                    )
                    .await
                {
                    Ok(session) => {
                        session.disconnect().await.expect("disconnect session");
                        panic!("expected SDK callback hooks to be rejected");
                    }
                    Err(err) => assert_unsupported_hooks_error(err),
                }
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

struct PreMcpHooks;

#[async_trait]
impl SessionHooks for PreMcpHooks {
    async fn on_pre_mcp_tool_call(
        &self,
        _input: PreMcpToolCallInput,
        _ctx: HookContext,
    ) -> Option<PreMcpToolCallOutput> {
        Some(PreMcpToolCallOutput {
            meta_to_use: Some(json!({"injected": "by-hook"})),
        })
    }
}
