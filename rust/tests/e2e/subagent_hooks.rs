use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::hooks::{
    HookContext, PostToolUseInput, PostToolUseOutput, PreToolUseInput, PreToolUseOutput,
    SessionHooks,
};

use super::support::{assert_unsupported_hooks_error, with_e2e_context};

#[tokio::test]
async fn rejects_sdk_callback_hooks_for_sub_agent_hook_propagation() {
    with_e2e_context(
        "subagent_hooks",
        "rejects_sdk_callback_hooks_for_sub_agent_hook_propagation",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                match client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_hooks(Arc::new(SubagentHooks)),
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

struct SubagentHooks;

#[async_trait]
impl SessionHooks for SubagentHooks {
    async fn on_pre_tool_use(
        &self,
        _input: PreToolUseInput,
        _ctx: HookContext,
    ) -> Option<PreToolUseOutput> {
        Some(PreToolUseOutput {
            permission_decision: Some("allow".to_string()),
            ..PreToolUseOutput::default()
        })
    }

    async fn on_post_tool_use(
        &self,
        _input: PostToolUseInput,
        _ctx: HookContext,
    ) -> Option<PostToolUseOutput> {
        None
    }
}
