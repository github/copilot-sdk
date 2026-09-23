//! Compatibility tests for observed identity, not an application association reducer.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{QueuePendingItemsResult, SendResult};
use github_copilot_sdk::session_events::{
    AssistantMessageData, AssistantTurnStartData, SubagentStartedData, TypedSessionEvent,
    UserMessageData,
};
use github_copilot_sdk::types::{SessionEvent, SessionEventNotification};
use serde_json::{Value, json};

fn fixture() -> Value {
    serde_json::from_str(include_str!("fixtures/execution-identity-v1.json")).unwrap()
}

fn event(fixture: &Value, name: &str) -> SessionEvent {
    serde_json::from_value(fixture["notifications"][name]["params"]["event"].clone()).unwrap()
}

#[test]
fn identity_fixture_round_trips_through_public_event_types() {
    let fixture = fixture();
    assert_eq!(fixture["version"], 1);
    assert_eq!(fixture["synthetic"], true);

    for notification in fixture["notifications"].as_object().unwrap().values() {
        let routed: SessionEventNotification =
            serde_json::from_value(notification["params"].clone()).unwrap();
        let typed: TypedSessionEvent =
            serde_json::from_value(notification["params"]["event"].clone()).unwrap();

        assert_eq!(typed.id, routed.event.id);
        assert_eq!(typed.agent_id, routed.event.agent_id);
        assert_eq!(
            serde_json::to_value(routed).unwrap(),
            notification["params"]
        );
        assert_eq!(
            serde_json::to_value(typed).unwrap(),
            notification["params"]["event"]
        );
    }
}

#[test]
fn message_identity_is_distinct_from_event_queue_and_assistant_identity() {
    let fixture = fixture();
    let pending: QueuePendingItemsResult =
        serde_json::from_value(fixture["pendingItems"].clone()).unwrap();
    assert_eq!(
        serde_json::to_value(&pending).unwrap(),
        fixture["pendingItems"]
    );
    assert_eq!(pending.items[0].id, pending.items[1].id);

    for (name, item) in ["user", "system"].into_iter().zip(&pending.items) {
        let accepted: SendResult =
            serde_json::from_value(fixture["sendResults"][name].clone()).unwrap();
        let notification = event(&fixture, name);
        let user = notification.typed_data::<UserMessageData>().unwrap();
        assert_eq!(
            user.message_id.as_deref(),
            Some(accepted.message_id.as_str())
        );
        assert_eq!(item.message_id, user.message_id);
        assert_ne!(item.id, accepted.message_id);
        assert_ne!(notification.id, accepted.message_id);
        assert_ne!(
            notification.parent_id.as_deref(),
            user.message_id.as_deref()
        );
    }

    let user = event(&fixture, "user")
        .typed_data::<UserMessageData>()
        .unwrap();
    let assistant = event(&fixture, "assistant")
        .typed_data::<AssistantMessageData>()
        .unwrap();
    assert_eq!(assistant.originating_message_id, user.message_id);
    assert_ne!(Some(assistant.message_id), user.message_id);
    assert_ne!(assistant.turn_id, user.turn_id);
}

#[test]
fn loop_and_interaction_keys_can_repeat_without_replacing_message_identity() {
    let fixture = fixture();
    let user = event(&fixture, "user")
        .typed_data::<UserMessageData>()
        .unwrap();
    let system = event(&fixture, "system")
        .typed_data::<UserMessageData>()
        .unwrap();
    assert_eq!(user.turn_id, system.turn_id);
    assert_eq!(user.interaction_id, system.interaction_id);
    assert_ne!(user.message_id, system.message_id);

    let worker = event(&fixture, "worker");
    let worker_data = worker.typed_data::<AssistantMessageData>().unwrap();
    assert!(worker.agent_id.is_some());
    assert_ne!(worker_data.originating_message_id, user.message_id);
    assert_ne!(worker_data.interaction_id, user.interaction_id);
}

#[test]
fn spawn_and_correction_identity_preserve_distinct_relationships() {
    let fixture = fixture();
    let started = event(&fixture, "workerStarted");
    let spawn = started.typed_data::<SubagentStartedData>().unwrap();
    let worker = event(&fixture, "worker");
    assert_eq!(started.agent_id, worker.agent_id);
    assert_eq!(spawn.tool_call_id, "tool-spawn-a");
    assert_ne!(
        started.agent_id.as_deref(),
        Some(spawn.tool_call_id.as_str())
    );

    let user = event(&fixture, "user")
        .typed_data::<UserMessageData>()
        .unwrap();
    let corrected = event(&fixture, "correctedAssistant")
        .typed_data::<AssistantMessageData>()
        .unwrap();
    assert_eq!(corrected.originating_message_id, user.message_id);
    assert_ne!(corrected.interaction_id, user.interaction_id);
}

#[test]
fn absent_identity_is_not_inferred_from_an_event_or_assistant_message_id() {
    let fixture = fixture();
    let legacy = event(&fixture, "legacy")
        .typed_data::<UserMessageData>()
        .unwrap();
    assert!(legacy.message_id.is_none());
    assert!(legacy.turn_id.is_none());
    assert!(legacy.interaction_id.is_none());
    assert!(legacy.parent_agent_task_id.is_none());

    let mut assistant = event(&fixture, "assistant");
    for key in ["originatingMessageId", "turnId", "interactionId"] {
        assistant.data.as_object_mut().unwrap().remove(key);
    }
    let data = assistant.typed_data::<AssistantMessageData>().unwrap();
    assert!(data.originating_message_id.is_none());
    assert!(data.turn_id.is_none());
    assert!(data.interaction_id.is_none());
    assert!(!data.message_id.is_empty());

    let mut turn = event(&fixture, "turnStart");
    turn.data.as_object_mut().unwrap().remove("interactionId");
    let data = turn.typed_data::<AssistantTurnStartData>().unwrap();
    assert_eq!(data.turn_id, "0");
    assert!(data.interaction_id.is_none());
    turn.data.as_object_mut().unwrap().remove("turnId");
    assert!(turn.typed_data::<AssistantTurnStartData>().is_none());
}

#[test]
fn empty_identity_remains_empty_and_malformed_identity_is_not_coerced() {
    let fixture = fixture();
    let mut empty = event(&fixture, "user");
    for key in ["messageId", "turnId", "interactionId"] {
        empty.data[key] = json!("");
    }
    let data = empty.typed_data::<UserMessageData>().unwrap();
    assert_eq!(data.message_id.as_deref(), Some(""));
    assert_eq!(data.turn_id.as_deref(), Some(""));
    assert_eq!(data.interaction_id.as_deref(), Some(""));

    for key in ["messageId", "turnId", "interactionId"] {
        let mut malformed = event(&fixture, "user");
        malformed.data[key] = json!(0);
        assert!(malformed.typed_data::<UserMessageData>().is_none());
        assert_eq!(malformed.data[key], 0);
    }

    for response in [
        json!({}),
        json!({"messageId": null}),
        json!({"messageId": 0}),
    ] {
        assert!(serde_json::from_value::<SendResult>(response).is_err());
    }
}

#[test]
fn identity_fixture_contains_no_message_or_tool_content() {
    fn check(value: &Value) {
        match value {
            Value::Object(fields) => {
                for (key, value) in fields {
                    if matches!(
                        key.as_str(),
                        "content"
                            | "displayText"
                            | "agentDescription"
                            | "agentDisplayName"
                            | "agentName"
                    ) {
                        assert_eq!(value, "");
                    }
                    assert!(!matches!(
                        key.as_str(),
                        "prompt"
                            | "arguments"
                            | "attachments"
                            | "transformedContent"
                            | "reasoningText"
                            | "result"
                    ));
                    check(value);
                }
            }
            Value::Array(values) => values.iter().for_each(check),
            _ => {}
        }
    }
    check(&fixture());
}
