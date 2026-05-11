use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::hooks::{
    HookContext, PostToolUseInput, PreToolUseInput, PreToolUseOutput, SessionHooks,
};
use tokio::sync::mpsc;

use super::support::{recv_with_timeout, with_e2e_context};

#[tokio::test]
async fn should_invoke_pretooluse_hook_when_model_runs_a_tool() {
    with_e2e_context(
        "hooks",
        "should_invoke_pretooluse_hook_when_model_runs_a_tool",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(ctx.work_dir().join("hello.txt"), "Hello from the test!")
                    .expect("write hello");
                let (pre_tx, mut pre_rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_hooks(Arc::new(
                        RecordingHooks {
                            pre_tx: Some(pre_tx),
                            post_tx: None,
                            deny: false,
                        },
                    )))
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Read the contents of hello.txt and tell me what it says")
                    .await
                    .expect("send");

                let input = recv_with_timeout(&mut pre_rx, "preToolUse hook").await;
                assert_eq!(input.0, *session.id());
                assert!(!input.1.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_invoke_posttooluse_hook_after_model_runs_a_tool() {
    with_e2e_context(
        "hooks",
        "should_invoke_posttooluse_hook_after_model_runs_a_tool",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(ctx.work_dir().join("world.txt"), "World from the test!")
                    .expect("write world");
                let (post_tx, mut post_rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_hooks(Arc::new(
                        RecordingHooks {
                            pre_tx: None,
                            post_tx: Some(post_tx),
                            deny: false,
                        },
                    )))
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Read the contents of world.txt and tell me what it says")
                    .await
                    .expect("send");

                let input = recv_with_timeout(&mut post_rx, "postToolUse hook").await;
                assert_eq!(input.0, *session.id());
                assert!(!input.1.is_empty());
                assert!(input.2);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_invoke_both_pretooluse_and_posttooluse_hooks_for_single_tool_call() {
    with_e2e_context(
        "hooks",
        "should_invoke_both_pretooluse_and_posttooluse_hooks_for_single_tool_call",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(ctx.work_dir().join("both.txt"), "Testing both hooks!")
                    .expect("write both");
                let (pre_tx, mut pre_rx) = mpsc::unbounded_channel();
                let (post_tx, mut post_rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_hooks(Arc::new(
                        RecordingHooks {
                            pre_tx: Some(pre_tx),
                            post_tx: Some(post_tx),
                            deny: false,
                        },
                    )))
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Read the contents of both.txt")
                    .await
                    .expect("send");

                let pre = recv_with_timeout(&mut pre_rx, "preToolUse hook").await;
                let post = recv_with_timeout(&mut post_rx, "postToolUse hook").await;
                assert_eq!(pre.0, *session.id());
                assert_eq!(post.0, *session.id());
                assert_eq!(pre.1, post.1);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_deny_tool_execution_when_pretooluse_returns_deny() {
    with_e2e_context(
        "hooks",
        "should_deny_tool_execution_when_pretooluse_returns_deny",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let original_content = "Original content that should not be modified";
                let protected_path = ctx.work_dir().join("protected.txt");
                std::fs::write(&protected_path, original_content).expect("write protected");
                let (pre_tx, mut pre_rx) = mpsc::unbounded_channel();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_hooks(Arc::new(
                        RecordingHooks {
                            pre_tx: Some(pre_tx),
                            post_tx: None,
                            deny: true,
                        },
                    )))
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Edit protected.txt and replace 'Original' with 'Modified'")
                    .await
                    .expect("send");

                let pre = recv_with_timeout(&mut pre_rx, "preToolUse hook").await;
                assert_eq!(pre.0, *session.id());
                assert_eq!(
                    std::fs::read_to_string(protected_path).expect("read protected"),
                    original_content
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

struct RecordingHooks {
    pre_tx: Option<mpsc::UnboundedSender<(github_copilot_sdk::SessionId, String)>>,
    post_tx: Option<mpsc::UnboundedSender<(github_copilot_sdk::SessionId, String, bool)>>,
    deny: bool,
}

#[async_trait]
impl SessionHooks for RecordingHooks {
    async fn on_pre_tool_use(
        &self,
        input: PreToolUseInput,
        ctx: HookContext,
    ) -> Option<PreToolUseOutput> {
        if let Some(pre_tx) = &self.pre_tx {
            let _ = pre_tx.send((ctx.session_id, input.tool_name));
        }
        Some(PreToolUseOutput {
            permission_decision: Some(if self.deny { "deny" } else { "allow" }.to_string()),
            ..PreToolUseOutput::default()
        })
    }

    async fn on_post_tool_use(
        &self,
        input: PostToolUseInput,
        ctx: HookContext,
    ) -> Option<github_copilot_sdk::hooks::PostToolUseOutput> {
        if let Some(post_tx) = &self.post_tx {
            let _ = post_tx.send((
                ctx.session_id,
                input.tool_name,
                !input.tool_result.is_null(),
            ));
        }
        None
    }
}
