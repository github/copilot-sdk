use github_copilot_sdk::generated::session_events::{
    AssistantMessageData, AssistantUsageData, SessionEventType, SessionUsageInfoData,
    ToolExecutionCompleteData, ToolExecutionStartData, UserMessageData,
};

use super::support::{collect_until_idle, event_types, with_e2e_context};

#[tokio::test]
async fn should_include_valid_fields_on_all_events() {
    with_e2e_context(
        "event_fidelity",
        "should_include_valid_fields_on_all_events",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send_and_wait("What is 5+5? Reply with just the number.")
                    .await
                    .expect("send");

                let observed = collect_until_idle(events).await;
                for event in &observed {
                    assert!(!event.id.is_empty(), "event id should be set");
                    assert!(!event.timestamp.is_empty(), "event timestamp should be set");
                }
                let user = observed
                    .iter()
                    .find(|event| event.parsed_type() == SessionEventType::UserMessage)
                    .and_then(|event| event.typed_data::<UserMessageData>())
                    .expect("user.message");
                assert!(!user.content.is_empty());
                let assistant = observed
                    .iter()
                    .find(|event| event.parsed_type() == SessionEventType::AssistantMessage)
                    .and_then(|event| event.typed_data::<AssistantMessageData>())
                    .expect("assistant.message");
                assert!(!assistant.message_id.is_empty());
                assert!(!assistant.content.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_tool_execution_events_with_correct_fields() {
    with_e2e_context(
        "event_fidelity",
        "should_emit_tool_execution_events_with_correct_fields",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(ctx.work_dir().join("data.txt"), "test data")
                    .expect("write data file");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send_and_wait("Read the file 'data.txt'.")
                    .await
                    .expect("send");

                let observed = collect_until_idle(events).await;
                let start = observed
                    .iter()
                    .find(|event| event.parsed_type() == SessionEventType::ToolExecutionStart)
                    .and_then(|event| event.typed_data::<ToolExecutionStartData>())
                    .expect("tool.execution_start");
                assert!(!start.tool_call_id.is_empty());
                assert!(!start.tool_name.is_empty());
                let complete = observed
                    .iter()
                    .find(|event| event.parsed_type() == SessionEventType::ToolExecutionComplete)
                    .and_then(|event| event.typed_data::<ToolExecutionCompleteData>())
                    .expect("tool.execution_complete");
                assert!(!complete.tool_call_id.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_assistant_usage_event_after_model_call() {
    with_e2e_context(
        "event_fidelity",
        "should_emit_assistant_usage_event_after_model_call",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send_and_wait("What is 5+5? Reply with just the number.")
                    .await
                    .expect("send");

                let observed = collect_until_idle(events).await;
                let usage = observed
                    .iter()
                    .rev()
                    .find(|event| event.parsed_type() == SessionEventType::AssistantUsage)
                    .and_then(|event| event.typed_data::<AssistantUsageData>())
                    .expect("assistant.usage");
                assert!(!usage.model.is_empty());

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_session_usage_info_event_after_model_call() {
    with_e2e_context(
        "event_fidelity",
        "should_emit_session_usage_info_event_after_model_call",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send_and_wait("What is 5+5? Reply with just the number.")
                    .await
                    .expect("send");

                let observed = collect_until_idle(events).await;
                let usage = observed
                    .iter()
                    .rev()
                    .find(|event| event.parsed_type() == SessionEventType::SessionUsageInfo)
                    .and_then(|event| event.typed_data::<SessionUsageInfoData>())
                    .expect("session.usage_info");
                assert!(usage.current_tokens > 0.0);
                assert!(usage.messages_length > 0.0);
                assert!(usage.token_limit > 0.0);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_pending_messages_modified_event_when_message_queue_changes() {
    with_e2e_context(
        "event_fidelity",
        "should_emit_pending_messages_modified_event_when_message_queue_changes",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send("What is 9+9? Reply with just the number.")
                    .await
                    .expect("send");

                let observed = collect_until_idle(events).await;
                assert!(
                    observed
                        .iter()
                        .any(|event| event.parsed_type()
                            == SessionEventType::PendingMessagesModified)
                );
                let answer = observed
                    .iter()
                    .rev()
                    .find(|event| event.parsed_type() == SessionEventType::AssistantMessage)
                    .and_then(|event| event.typed_data::<AssistantMessageData>())
                    .expect("assistant.message");
                assert!(answer.content.contains("18"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_events_in_correct_order_for_tool_using_conversation() {
    with_e2e_context(
        "event_fidelity",
        "should_emit_events_in_correct_order_for_tool_using_conversation",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(ctx.work_dir().join("hello.txt"), "Hello World")
                    .expect("write hello file");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session
                    .send_and_wait("Read the file 'hello.txt' and tell me its contents.")
                    .await
                    .expect("send");

                let observed = collect_until_idle(events).await;
                let types = event_types(&observed);
                let user = types
                    .iter()
                    .position(|event_type| *event_type == "user.message")
                    .expect("user.message");
                let assistant = types
                    .iter()
                    .rposition(|event_type| *event_type == "assistant.message")
                    .expect("assistant.message");
                let idle = types
                    .iter()
                    .rposition(|event_type| *event_type == "session.idle")
                    .expect("session.idle");
                assert!(user < assistant);
                assert_eq!(idle, types.len() - 1);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_assistant_message_with_messageid() {
    with_e2e_context(
        "event_fidelity",
        "should_emit_assistant_message_with_messageid",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");
                let events = session.subscribe();

                session.send_and_wait("Say 'pong'.").await.expect("send");

                let observed = collect_until_idle(events).await;
                let assistant = observed
                    .iter()
                    .find(|event| event.parsed_type() == SessionEventType::AssistantMessage)
                    .and_then(|event| event.typed_data::<AssistantMessageData>())
                    .expect("assistant.message");
                assert!(!assistant.message_id.is_empty());
                assert!(assistant.content.contains("pong"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_preserve_message_order_in_getmessages_after_tool_use() {
    with_e2e_context(
        "event_fidelity",
        "should_preserve_message_order_in_getmessages_after_tool_use",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(ctx.work_dir().join("order.txt"), "ORDER_CONTENT_42")
                    .expect("write order file");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                session
                    .send_and_wait("Read the file 'order.txt' and tell me what the number is.")
                    .await
                    .expect("send");

                let messages = session.get_messages().await.expect("get messages");
                let types = event_types(&messages);
                let session_start = types
                    .iter()
                    .position(|event_type| *event_type == "session.start")
                    .expect("session.start");
                let user = types
                    .iter()
                    .position(|event_type| *event_type == "user.message")
                    .expect("user.message");
                let tool_start = types
                    .iter()
                    .position(|event_type| *event_type == "tool.execution_start")
                    .expect("tool.execution_start");
                let tool_complete = types
                    .iter()
                    .position(|event_type| *event_type == "tool.execution_complete")
                    .expect("tool.execution_complete");
                let assistant = types
                    .iter()
                    .rposition(|event_type| *event_type == "assistant.message")
                    .expect("assistant.message");
                assert!(session_start < user);
                assert!(user < tool_start);
                assert!(tool_start < tool_complete);
                assert!(tool_complete < assistant);

                let user_data = messages
                    .iter()
                    .find(|event| event.parsed_type() == SessionEventType::UserMessage)
                    .and_then(|event| event.typed_data::<UserMessageData>())
                    .expect("user.message");
                assert!(user_data.content.contains("order.txt"));
                let assistant_data = messages
                    .iter()
                    .rev()
                    .find(|event| event.parsed_type() == SessionEventType::AssistantMessage)
                    .and_then(|event| event.typed_data::<AssistantMessageData>())
                    .expect("assistant.message");
                assert!(assistant_data.content.contains("42"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
