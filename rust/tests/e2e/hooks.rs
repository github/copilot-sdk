use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::hooks::{
    HookContext, PostToolUseInput, PostToolUseOutput, PreToolUseInput, PreToolUseOutput,
    SessionHooks,
};

use super::support::{assert_unsupported_hooks_error, with_e2e_context};

#[tokio::test]
async fn rejects_sdk_callback_hooks() {
    with_e2e_context("hooks", "rejects_sdk_callback_hooks", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            assert_unsupported_hooks(
                &client,
                ctx.approve_all_session_config()
                    .with_hooks(Arc::new(RecordingHooks)),
            )
            .await;
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

async fn assert_unsupported_hooks(
    client: &github_copilot_sdk::Client,
    config: github_copilot_sdk::SessionConfig,
) {
    match client.create_session(config).await {
        Ok(session) => {
            session.disconnect().await.expect("disconnect session");
            panic!("expected SDK callback hooks to be rejected");
        }
        Err(err) => assert_unsupported_hooks_error(err),
    }
}

struct RecordingHooks;

#[async_trait]
impl SessionHooks for RecordingHooks {
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
