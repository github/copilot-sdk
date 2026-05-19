use github_copilot_sdk::{ResumeSessionConfig, SessionId};

use super::support::with_e2e_context;

#[tokio::test]
async fn should_throw_when_sending_to_disconnected_session() {
    with_e2e_context(
        "error_resilience",
        "should_throw_when_sending_to_disconnected_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                session.disconnect().await.expect("disconnect session");

                assert!(session.send_and_wait("Hello").await.is_err());

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_throw_when_getting_messages_from_disconnected_session() {
    with_e2e_context(
        "error_resilience",
        "should_throw_when_getting_messages_from_disconnected_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                session.disconnect().await.expect("disconnect session");

                assert!(session.get_messages().await.is_err());

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_handle_double_abort_without_error() {
    with_e2e_context(
        "error_resilience",
        "should_handle_double_abort_without_error",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session.abort().await.expect("first abort");
                session.abort().await.expect("second abort");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_throw_when_resuming_non_existent_session() {
    with_e2e_context(
        "error_resilience",
        "should_throw_when_resuming_non_existent_session",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;

                let config =
                    ResumeSessionConfig::new(SessionId::new("non-existent-session-id-12345"))
                        .with_handler(std::sync::Arc::new(
                            github_copilot_sdk::handler::ApproveAllHandler,
                        ))
                        .with_github_token(super::support::DEFAULT_TEST_TOKEN);
                assert!(client.resume_session(config).await.is_err());

                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
