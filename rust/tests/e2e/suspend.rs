use std::sync::Arc;

use github_copilot_sdk::ResumeSessionConfig;

use super::support::{DEFAULT_TEST_TOKEN, assistant_message_content, with_e2e_context};

#[tokio::test]
async fn should_suspend_idle_session_without_throwing() {
    with_e2e_context(
        "suspend",
        "should_suspend_idle_session_without_throwing",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Reply with: SUSPEND_IDLE_OK")
                    .await
                    .expect("send");
                session.rpc().suspend().await.expect("suspend session");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_allow_resume_and_continue_conversation_after_suspend() {
    with_e2e_context(
        "suspend",
        "should_allow_resume_and_continue_conversation_after_suspend",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session
                    .send_and_wait(
                        "Remember the magic word: SUSPENSE. Reply with: SUSPEND_TURN_ONE",
                    )
                    .await
                    .expect("first send");
                let session_id = session.id().clone();
                session.rpc().suspend().await.expect("suspend session");
                session.disconnect().await.expect("disconnect first session");
                client.stop().await.expect("stop first client");

                let second_client = ctx.start_client().await;
                let resumed = second_client
                    .resume_session(
                        ResumeSessionConfig::new(session_id)
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(Arc::new(
                                github_copilot_sdk::handler::ApproveAllHandler,
                            )),
                    )
                    .await
                    .expect("resume session");
                let answer = resumed
                    .send_and_wait(
                        "What was the magic word I asked you to remember? Reply with just the word.",
                    )
                    .await
                    .expect("follow-up send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer)
                    .to_lowercase()
                    .contains("suspense"));

                resumed.disconnect().await.expect("disconnect resumed");
                second_client.stop().await.expect("stop second client");
            })
        },
    )
    .await;
}
