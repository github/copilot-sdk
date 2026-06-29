use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::hooks::{
    ErrorOccurredInput, ErrorOccurredOutput, HookContext, PostToolUseFailureInput,
    PostToolUseFailureOutput, PostToolUseInput, PostToolUseOutput, PreToolUseInput,
    PreToolUseOutput, SessionEndInput, SessionEndOutput, SessionHooks, SessionStartInput,
    SessionStartOutput, UserPromptSubmittedInput, UserPromptSubmittedOutput,
};

use super::support::{assert_unsupported_hooks_error, with_e2e_context};

#[tokio::test]
async fn rejects_extended_sdk_callback_hooks() {
    with_e2e_context(
        "hooks_extended",
        "rejects_extended_sdk_callback_hooks",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                match client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_hooks(Arc::new(ExtendedHooks)),
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

struct ExtendedHooks;

#[async_trait]
impl SessionHooks for ExtendedHooks {
    async fn on_user_prompt_submitted(
        &self,
        _input: UserPromptSubmittedInput,
        _ctx: HookContext,
    ) -> Option<UserPromptSubmittedOutput> {
        Some(UserPromptSubmittedOutput {
            modified_prompt: Some("not used".to_string()),
            ..UserPromptSubmittedOutput::default()
        })
    }

    async fn on_session_start(
        &self,
        _input: SessionStartInput,
        _ctx: HookContext,
    ) -> Option<SessionStartOutput> {
        Some(SessionStartOutput {
            additional_context: Some("not used".to_string()),
            ..SessionStartOutput::default()
        })
    }

    async fn on_session_end(
        &self,
        _input: SessionEndInput,
        _ctx: HookContext,
    ) -> Option<SessionEndOutput> {
        Some(SessionEndOutput {
            session_summary: Some("not used".to_string()),
            ..SessionEndOutput::default()
        })
    }

    async fn on_error_occurred(
        &self,
        _input: ErrorOccurredInput,
        _ctx: HookContext,
    ) -> Option<ErrorOccurredOutput> {
        Some(ErrorOccurredOutput {
            error_handling: Some("skip".to_string()),
            ..ErrorOccurredOutput::default()
        })
    }

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
        Some(PostToolUseOutput {
            suppress_output: Some(false),
            ..PostToolUseOutput::default()
        })
    }

    async fn on_post_tool_use_failure(
        &self,
        _input: PostToolUseFailureInput,
        _ctx: HookContext,
    ) -> Option<PostToolUseFailureOutput> {
        Some(PostToolUseFailureOutput {
            additional_context: Some("not used".to_string()),
        })
    }
}
