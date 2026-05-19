use std::sync::Arc;

use github_copilot_sdk::ResumeSessionConfig;
use github_copilot_sdk::generated::session_events::{
    AssistantMessageData, AssistantMessageDeltaData, AssistantMessageStartData, SessionEventType,
    SessionStartData,
};
use github_copilot_sdk::handler::ApproveAllHandler;

use super::support::{collect_until_idle, event_types, with_e2e_context};

#[tokio::test]
async fn should_produce_delta_events_when_streaming_is_enabled() {
    with_e2e_context(
        "streaming_fidelity",
        "should_produce_delta_events_when_streaming_is_enabled",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_streaming(true))
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send_and_wait("Count from 1 to 5, separated by commas.")
                    .await
                    .expect("send");

                let observed = collect_until_idle(events).await;
                let types = event_types(&observed);
                let deltas: Vec<_> = observed
                    .iter()
                    .filter(|event| event.parsed_type() == SessionEventType::AssistantMessageDelta)
                    .collect();
                assert!(
                    !deltas.is_empty(),
                    "expected assistant.message_delta events"
                );
                for delta in deltas {
                    let data = delta
                        .typed_data::<AssistantMessageDeltaData>()
                        .expect("assistant.message_delta data");
                    assert!(!data.delta_content.is_empty());
                }
                let first_delta = types
                    .iter()
                    .position(|event_type| *event_type == "assistant.message_delta")
                    .expect("first delta index");
                let final_message = types
                    .iter()
                    .rposition(|event_type| *event_type == "assistant.message")
                    .expect("assistant message index");
                assert!(first_delta < final_message);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_not_produce_deltas_when_streaming_is_disabled() {
    with_e2e_context(
        "streaming_fidelity",
        "should_not_produce_deltas_when_streaming_is_disabled",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_streaming(false))
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send_and_wait("Say 'hello world'.")
                    .await
                    .expect("send");

                let observed = collect_until_idle(events).await;
                assert!(
                    observed
                        .iter()
                        .all(|event| event.parsed_type() != SessionEventType::AssistantMessageDelta),
                    "streaming-disabled sessions should not emit assistant.message_delta"
                );
                assert!(
                    observed
                        .iter()
                        .any(|event| event.parsed_type() == SessionEventType::AssistantMessage),
                    "expected final assistant.message"
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_produce_deltas_after_session_resume() {
    with_e2e_context(
        "streaming_fidelity",
        "should_produce_deltas_after_session_resume",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_streaming(false))
                    .await
                    .expect("create session");
                session
                    .send_and_wait("What is 3 + 6?")
                    .await
                    .expect("first send");
                let session_id = session.id().clone();
                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop first client");

                let new_client = ctx.start_client().await;
                let resumed = new_client
                    .resume_session(
                        ResumeSessionConfig::new(session_id)
                            .with_streaming(true)
                            .with_handler(Arc::new(ApproveAllHandler))
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN),
                    )
                    .await
                    .expect("resume session");
                let events = resumed.subscribe();

                let answer = resumed
                    .send_and_wait("Now if you double that, what do you get?")
                    .await
                    .expect("second send")
                    .expect("assistant message");
                assert!(
                    answer
                        .typed_data::<AssistantMessageData>()
                        .expect("assistant.message data")
                        .content
                        .contains("18")
                );

                let observed = collect_until_idle(events).await;
                assert_has_content_deltas(&observed);

                resumed.disconnect().await.expect("disconnect resumed");
                new_client.stop().await.expect("stop new client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_not_produce_deltas_after_session_resume_with_streaming_disabled() {
    with_e2e_context(
        "streaming_fidelity",
        "should_not_produce_deltas_after_session_resume_with_streaming_disabled",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_streaming(true))
                    .await
                    .expect("create session");
                session
                    .send_and_wait("What is 3 + 6?")
                    .await
                    .expect("first send");
                let session_id = session.id().clone();
                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop first client");

                let new_client = ctx.start_client().await;
                let resumed = new_client
                    .resume_session(
                        ResumeSessionConfig::new(session_id)
                            .with_streaming(false)
                            .with_handler(Arc::new(ApproveAllHandler))
                            .with_github_token(super::support::DEFAULT_TEST_TOKEN),
                    )
                    .await
                    .expect("resume session");
                let events = resumed.subscribe();

                let answer = resumed
                    .send_and_wait("Now if you double that, what do you get?")
                    .await
                    .expect("second send")
                    .expect("assistant message");
                assert!(answer
                    .typed_data::<AssistantMessageData>()
                    .expect("assistant.message data")
                    .content
                    .contains("18"));

                let observed = collect_until_idle(events).await;
                assert!(
                    observed
                        .iter()
                        .all(|event| event.parsed_type() != SessionEventType::AssistantMessageDelta),
                    "streaming-disabled resumed sessions should not emit deltas"
                );
                assert!(observed
                    .iter()
                    .any(|event| event.parsed_type() == SessionEventType::AssistantMessage));

                resumed.disconnect().await.expect("disconnect resumed");
                new_client.stop().await.expect("stop new client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_streaming_deltas_with_reasoning_effort_configured() {
    with_e2e_context(
        "streaming_fidelity",
        "should_emit_streaming_deltas_with_reasoning_effort_configured",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_streaming(true)
                            .with_reasoning_effort("high"),
                    )
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send_and_wait("What is 15 * 17?")
                    .await
                    .expect("send");

                let observed = collect_until_idle(events).await;
                assert_has_content_deltas(&observed);
                let assistant = observed
                    .iter()
                    .rev()
                    .find(|event| event.parsed_type() == SessionEventType::AssistantMessage)
                    .and_then(|event| event.typed_data::<AssistantMessageData>())
                    .expect("assistant.message");
                assert!(assistant.content.contains("255"));

                let start = session
                    .get_messages()
                    .await
                    .expect("get messages")
                    .into_iter()
                    .find(|event| event.parsed_type() == SessionEventType::SessionStart)
                    .and_then(|event| event.typed_data::<SessionStartData>())
                    .expect("session.start");
                assert_eq!(start.reasoning_effort.as_deref(), Some("high"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_assistantmessage_start_before_deltas_with_matching_messageid() {
    with_e2e_context(
        "streaming_fidelity",
        "should_emit_assistantmessagestart_before_deltas_with_matching_messageid",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config().with_streaming(true))
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send_and_wait("Count from 1 to 5, separated by commas.")
                    .await
                    .expect("send");

                let observed = collect_until_idle(events).await;
                let start_indices: Vec<_> = observed
                    .iter()
                    .enumerate()
                    .filter_map(|(index, event)| {
                        (event.parsed_type() == SessionEventType::AssistantMessageStart)
                            .then_some(index)
                    })
                    .collect();
                let delta_indices: Vec<_> = observed
                    .iter()
                    .enumerate()
                    .filter_map(|(index, event)| {
                        (event.parsed_type() == SessionEventType::AssistantMessageDelta)
                            .then_some(index)
                    })
                    .collect();
                assert!(
                    !start_indices.is_empty(),
                    "expected assistant.message_start"
                );
                assert!(
                    !delta_indices.is_empty(),
                    "expected assistant.message_delta"
                );
                assert!(start_indices[0] < delta_indices[0]);

                let message_ids: Vec<_> = observed
                    .iter()
                    .filter_map(|event| event.typed_data::<AssistantMessageData>())
                    .map(|data| data.message_id)
                    .collect();
                for start_index in start_indices {
                    let data = observed[start_index]
                        .typed_data::<AssistantMessageStartData>()
                        .expect("assistant.message_start data");
                    assert!(!data.message_id.is_empty());
                    assert!(message_ids.contains(&data.message_id));
                }

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn assert_has_content_deltas(events: &[github_copilot_sdk::SessionEvent]) {
    let deltas: Vec<_> = events
        .iter()
        .filter(|event| event.parsed_type() == SessionEventType::AssistantMessageDelta)
        .collect();
    assert!(
        !deltas.is_empty(),
        "expected assistant.message_delta events"
    );
    for delta in deltas {
        let data = delta
            .typed_data::<AssistantMessageDeltaData>()
            .expect("assistant.message_delta data");
        assert!(!data.delta_content.is_empty());
    }
}
