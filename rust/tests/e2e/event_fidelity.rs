use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};

use async_trait::async_trait;
use github_copilot_sdk::session_events::{
    AssistantMessageData, AssistantUsageData, SessionEventType, SessionUsageInfoData,
    ToolExecutionCompleteData, ToolExecutionStartData, UserMessageData, UserMessageDelivery,
};
use github_copilot_sdk::tool::ToolHandler;
use github_copilot_sdk::{
    DeliveryMode, Error, MessageOptions, MessageSource, Tool, ToolInvocation, ToolResult,
};
use tokio::sync::{Mutex, mpsc};

use super::support::{collect_until_idle, event_types, recv_with_timeout, wait_for_event};

#[tokio::test]
async fn should_include_valid_fields_on_all_events() {
    super::support::with_shared_e2e_context(
        &E2E,
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
    super::support::with_shared_e2e_context(
        &E2E,
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
    super::support::with_shared_e2e_context(
        &E2E,
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
    super::support::with_shared_e2e_context(
        &E2E,
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
                assert!(usage.current_tokens > 0);
                assert!(usage.messages_length > 0);
                assert!(usage.token_limit > 0);

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_emit_pending_messages_modified_event_when_message_queue_changes() {
    super::support::with_shared_e2e_context(
        &E2E,
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
    super::support::with_shared_e2e_context(
        &E2E,
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
    super::support::with_shared_e2e_context(
        &E2E,
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
    super::support::with_shared_e2e_context(
        &E2E,
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

                let messages = session.get_events().await.expect("get messages");
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

#[tokio::test]
async fn should_order_idle_queued_and_immediate_delivery_while_busy() {
    super::support::with_dedicated_e2e_context(
        "scenario_testing_sends",
        "should_order_idle_queued_and_immediate_scenario_delivery",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let (started_tx, mut started_rx) = mpsc::unbounded_channel();
                let (release_tx, release_rx) = mpsc::channel(2);
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config().with_tools(vec![
                            Tool::new("scenario_send_blocker")
                                .with_description("Blocks the active turn until released")
                                .with_handler(Arc::new(SequencedBlockingTool {
                                    started_tx,
                                    release_rx: Mutex::new(release_rx),
                                    invocation_count: AtomicUsize::new(0),
                                })),
                        ]),
                    )
                    .await
                    .expect("create session");

                let idle_enqueue = tokio::spawn(wait_for_event(
                    session.subscribe(),
                    "idle enqueue completion",
                    |event| event.parsed_type() == SessionEventType::SessionIdle,
                ));
                let idle_enqueue_id = session
                    .send(
                        MessageOptions::new("Reply with exactly IDLE_ENQUEUE.")
                            .with_mode(DeliveryMode::Enqueue)
                            .with_source(MessageSource::Agent("scenario-client".to_string())),
                    )
                    .await
                    .expect("send idle enqueue");
                idle_enqueue.await.expect("idle enqueue task");

                let idle_immediate = tokio::spawn(wait_for_event(
                    session.subscribe(),
                    "idle immediate completion",
                    |event| event.parsed_type() == SessionEventType::SessionIdle,
                ));
                let idle_immediate_id = session
                    .send(
                        MessageOptions::new("Reply with exactly IDLE_IMMEDIATE.")
                            .with_mode(DeliveryMode::Immediate)
                            .with_source(MessageSource::Agent("scenario-client".to_string())),
                    )
                    .await
                    .expect("send idle immediate");
                idle_immediate.await.expect("idle immediate task");

                session
                    .send(
                        MessageOptions::new(
                            "Call scenario_send_blocker, then reply with its result.",
                        )
                        .with_source(MessageSource::Agent("scenario-client".to_string())),
                    )
                    .await
                    .expect("start blocking turn");
                assert_eq!(
                    recv_with_timeout(&mut started_rx, "first blocker invocation").await,
                    1
                );

                let steering_id = session
                    .send(
                        MessageOptions::new(
                            "Call scenario_send_blocker again, then reply with exactly FIRST_STEERING.",
                        )
                        .with_mode(DeliveryMode::Immediate)
                        .with_source(MessageSource::Agent("scenario-client".to_string())),
                    )
                    .await
                    .expect("send steering message");
                release_tx
                    .send("SCENARIO_SEND_BLOCKER_RELEASED".to_string())
                    .await
                    .expect("release first blocker");
                assert_eq!(
                    recv_with_timeout(&mut started_rx, "second blocker invocation").await,
                    2
                );

                let second_immediate_id = session
                    .send(
                        MessageOptions::new("Reply with exactly SECOND_IMMEDIATE.")
                            .with_mode(DeliveryMode::Immediate)
                            .with_source(MessageSource::Agent("scenario-client".to_string())),
                    )
                    .await
                    .expect("send second immediate");
                let queued_id = session
                    .send(
                        MessageOptions::new("Reply with exactly FINAL_QUEUED.")
                            .with_mode(DeliveryMode::Enqueue)
                            .with_source(MessageSource::Agent("scenario-client".to_string())),
                    )
                    .await
                    .expect("send queued message");
                let final_queued = tokio::spawn(wait_for_event(
                    session.subscribe(),
                    "final queued response",
                    |event| {
                        event.parsed_type() == SessionEventType::AssistantMessage
                            && event
                                .typed_data::<AssistantMessageData>()
                                .is_some_and(|data| data.content.contains("FINAL_QUEUED"))
                    },
                ));
                release_tx
                    .send("SCENARIO_SEND_BLOCKER_RELEASED_AGAIN".to_string())
                    .await
                    .expect("release second blocker");
                final_queued.await.expect("final queued task");

                let events = session.get_events().await.expect("get events");
                let messages = events
                    .iter()
                    .filter_map(|event| {
                        (event.parsed_type() == SessionEventType::UserMessage)
                            .then(|| event.typed_data::<UserMessageData>())
                            .flatten()
                    })
                    .collect::<Vec<_>>();
                let find = |id: &str| {
                    messages
                        .iter()
                        .find(|message| message.message_id.as_deref() == Some(id))
                        .expect("user message by id")
                };
                assert_eq!(
                    find(&idle_enqueue_id).delivery,
                    Some(UserMessageDelivery::Idle)
                );
                assert_eq!(
                    find(&idle_immediate_id).delivery,
                    Some(UserMessageDelivery::Idle)
                );
                assert_eq!(
                    find(&steering_id).delivery,
                    Some(UserMessageDelivery::Steering)
                );
                assert_eq!(
                    find(&second_immediate_id).delivery,
                    Some(UserMessageDelivery::Steering)
                );
                assert_eq!(find(&queued_id).delivery, Some(UserMessageDelivery::Queued));

                let position = |id: &str| {
                    messages
                        .iter()
                        .position(|message| message.message_id.as_deref() == Some(id))
                        .expect("message position")
                };
                assert!(position(&steering_id) < position(&second_immediate_id));
                assert!(position(&second_immediate_id) < position(&queued_id));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

struct SequencedBlockingTool {
    started_tx: mpsc::UnboundedSender<usize>,
    release_rx: Mutex<mpsc::Receiver<String>>,
    invocation_count: AtomicUsize,
}

#[async_trait]
impl ToolHandler for SequencedBlockingTool {
    async fn call(&self, _invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let mut release_rx = self.release_rx.lock().await;
        let invocation = self.invocation_count.fetch_add(1, Ordering::SeqCst) + 1;
        let _ = self.started_tx.send(invocation);
        let result = release_rx.recv().await.expect("tool release value");
        Ok(ToolResult::Text(result))
    }
}
static E2E: super::support::SharedE2eGroup =
    super::support::SharedE2eGroup::standard("event_fidelity", 8);
