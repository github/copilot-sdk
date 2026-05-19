use github_copilot_sdk::SessionEvent;
use github_copilot_sdk::generated::session_events::SessionEventType;

use super::support::{
    assistant_message_content, collect_until_idle, event_types, with_e2e_context,
};

#[tokio::test]
async fn should_use_tool_results_from_previous_turns() {
    with_e2e_context(
        "multi_turn",
        "should_use_tool_results_from_previous_turns",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(ctx.work_dir().join("secret.txt"), "The magic number is 42.")
                    .expect("write secret");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let first_events = session.subscribe();
                let first = session
                    .send_and_wait(
                        "Read the file 'secret.txt' and tell me what the magic number is.",
                    )
                    .await
                    .expect("first send")
                    .expect("assistant message");
                assert!(assistant_message_content(&first).contains("42"));
                assert_tool_turn_ordering(
                    &collect_until_idle(first_events).await,
                    "file read turn",
                );

                let second = session
                    .send_and_wait("What is that magic number multiplied by 2?")
                    .await
                    .expect("second send")
                    .expect("assistant message");
                assert!(assistant_message_content(&second).contains("84"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_handle_file_creation_then_reading_across_turns() {
    with_e2e_context(
        "multi_turn",
        "should_handle_file_creation_then_reading_across_turns",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let create_events = session.subscribe();
                session
                    .send_and_wait(
                        "Create a file called 'greeting.txt' with the content \
                         'Hello from multi-turn test'.",
                    )
                    .await
                    .expect("create file turn");
                assert_eq!(
                    std::fs::read_to_string(ctx.work_dir().join("greeting.txt"))
                        .expect("read greeting"),
                    "Hello from multi-turn test"
                );
                assert_tool_turn_ordering(
                    &collect_until_idle(create_events).await,
                    "file creation turn",
                );

                let read_events = session.subscribe();
                let answer = session
                    .send_and_wait("Read the file 'greeting.txt' and tell me its exact contents.")
                    .await
                    .expect("read file turn")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("Hello from multi-turn test"));
                assert_tool_turn_ordering(&collect_until_idle(read_events).await, "file read turn");

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

fn assert_tool_turn_ordering(events: &[SessionEvent], turn_description: &str) {
    let observed_types = event_types(events).join(", ");
    let user_message = index_of(events, SessionEventType::UserMessage, 0);
    let tool_starts: Vec<_> = events
        .iter()
        .enumerate()
        .filter(|(_, event)| event.parsed_type() == SessionEventType::ToolExecutionStart)
        .collect();
    let tool_completes: Vec<_> = events
        .iter()
        .enumerate()
        .filter(|(_, event)| event.parsed_type() == SessionEventType::ToolExecutionComplete)
        .collect();

    assert!(
        user_message.is_some(),
        "expected user.message in {turn_description}; observed: {observed_types}"
    );
    assert!(
        !tool_starts.is_empty(),
        "expected tool.execution_start in {turn_description}; observed: {observed_types}"
    );
    assert!(
        !tool_completes.is_empty(),
        "expected tool.execution_complete in {turn_description}; observed: {observed_types}"
    );
    assert!(user_message.unwrap() < tool_starts[0].0);

    let last_tool_complete = tool_completes
        .last()
        .map(|(index, _)| *index)
        .expect("last tool completion");
    let assistant = index_of(
        events,
        SessionEventType::AssistantMessage,
        last_tool_complete + 1,
    )
    .expect("assistant.message after tools");
    let idle = index_of(events, SessionEventType::SessionIdle, assistant + 1)
        .expect("session.idle after assistant");
    assert!(last_tool_complete < assistant);
    assert!(assistant < idle);
}

fn index_of(
    events: &[SessionEvent],
    event_type: SessionEventType,
    start_index: usize,
) -> Option<usize> {
    events
        .iter()
        .enumerate()
        .skip(start_index)
        .find_map(|(index, event)| (event.parsed_type() == event_type).then_some(index))
}
