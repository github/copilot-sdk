use github_copilot_sdk::generated::session_events::{
    SessionCompactionCompleteData, SessionCompactionStartData, SessionEventType,
};
use github_copilot_sdk::{InfiniteSessionConfig, SessionConfig};

use super::support::{
    DEFAULT_TEST_TOKEN, assistant_message_content, collect_until_idle, wait_for_event,
    with_e2e_context,
};

#[tokio::test]
async fn should_trigger_compaction_with_low_threshold_and_emit_events() {
    with_e2e_context(
        "compaction",
        "should_trigger_compaction_with_low_threshold_and_emit_events",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        SessionConfig::default()
                            .with_github_token(DEFAULT_TEST_TOKEN)
                            .with_handler(std::sync::Arc::new(
                                github_copilot_sdk::handler::ApproveAllHandler,
                            ))
                            .with_infinite_sessions(
                                InfiniteSessionConfig::new()
                                    .with_enabled(true)
                                    .with_background_compaction_threshold(0.005)
                                    .with_buffer_exhaustion_threshold(0.01),
                            ),
                    )
                    .await
                    .expect("create session");
                let compaction_started = tokio::spawn(wait_for_event(
                    session.subscribe(),
                    "session.compaction_start",
                    |event| event.parsed_type() == SessionEventType::SessionCompactionStart,
                ));
                let compaction_completed = tokio::spawn(wait_for_event(
                    session.subscribe(),
                    "successful session.compaction_complete",
                    |event| {
                        event.parsed_type() == SessionEventType::SessionCompactionComplete
                            && event
                                .typed_data::<SessionCompactionCompleteData>()
                                .is_some_and(|data| data.success)
                    },
                ));

                session
                    .send_and_wait("Tell me a story about a dragon. Be detailed.")
                    .await
                    .expect("first send");
                session
                    .send_and_wait(
                        "Continue the story with more details about the dragon's castle.",
                    )
                    .await
                    .expect("second send");

                let start = compaction_started
                    .await
                    .expect("compaction start task")
                    .typed_data::<SessionCompactionStartData>()
                    .expect("compaction start data");
                assert!(start.conversation_tokens.unwrap_or_default() > 0.0);

                let complete = compaction_completed
                    .await
                    .expect("compaction complete task")
                    .typed_data::<SessionCompactionCompleteData>()
                    .expect("compaction complete data");
                assert!(complete.success);
                assert!(
                    complete
                        .compaction_tokens_used
                        .as_ref()
                        .and_then(|usage| usage.input_tokens)
                        .unwrap_or_default()
                        > 0.0
                );
                let summary = complete.summary_content.unwrap_or_default().to_lowercase();
                assert!(summary.contains("<overview>"));
                assert!(summary.contains("<history>"));
                assert!(summary.contains("<checkpoint_title>"));

                session
                    .send_and_wait("Now describe the dragon's treasure in great detail.")
                    .await
                    .expect("third send");
                let answer = session
                    .send_and_wait("What was the story about?")
                    .await
                    .expect("fourth send")
                    .expect("assistant message");
                let content = assistant_message_content(&answer).to_lowercase();
                assert!(content.contains("kaedrith"));
                assert!(content.contains("dragon"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_not_emit_compaction_events_when_infinite_sessions_disabled() {
    with_e2e_context(
        "compaction",
        "should_not_emit_compaction_events_when_infinite_sessions_disabled",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session =
                    client
                        .create_session(ctx.approve_all_session_config().with_infinite_sessions(
                            InfiniteSessionConfig::new().with_enabled(false),
                        ))
                        .await
                        .expect("create session");
                let events = session.subscribe();

                session.send_and_wait("What is 2+2?").await.expect("send");

                let observed = collect_until_idle(events).await;
                assert!(observed.iter().all(|event| {
                    !matches!(
                        event.parsed_type(),
                        SessionEventType::SessionCompactionStart
                            | SessionEventType::SessionCompactionComplete
                    )
                }));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
