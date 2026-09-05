// Unit tests for generated session-event payloads.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::session_events::UserMessageData;

#[test]
fn user_message_id_uses_camel_case_wire_name() {
    let data = UserMessageData {
        content: "queued message".to_string(),
        message_id: Some("message-123".to_string()),
        ..Default::default()
    };

    let serialized = serde_json::to_value(&data).unwrap();
    assert_eq!(serialized["messageId"], "message-123");

    let deserialized: UserMessageData = serde_json::from_value(serialized).unwrap();
    assert_eq!(deserialized.message_id.as_deref(), Some("message-123"));
}

#[test]
fn user_message_id_is_optional_for_older_hosts() {
    let data: UserMessageData = serde_json::from_value(serde_json::json!({
        "content": "legacy message"
    }))
    .unwrap();

    assert_eq!(data.message_id, None);
    assert!(
        serde_json::to_value(data)
            .unwrap()
            .get("messageId")
            .is_none()
    );
}
