// Unit tests for event-envelope compatibility and payload layout regressions.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::session_events::{SessionEventData, TypedSessionEvent};
use github_copilot_sdk::{SessionEvent, SessionEventWithMetadata};

fn event_envelope(event_type: &str, data: serde_json::Value) -> serde_json::Value {
    serde_json::json!({
        "id": "10000000-0000-4000-8000-000000000001",
        "timestamp": "2026-09-18T22:00:00Z",
        "parentId": "10000000-0000-4000-8000-000000000000",
        "ephemeral": true,
        "agentId": "test-agent",
        "type": event_type,
        "data": data
    })
}

#[test]
fn checkpoint_marker_round_trips_in_session_event_envelopes() {
    for checkpoint in [None, Some(false), Some(true)] {
        let mut wire = event_envelope(
            "session.info",
            serde_json::json!({"infoType": "status", "message": "Checkpoint state"}),
        );
        if let Some(checkpoint) = checkpoint {
            wire["checkpoint"] = serde_json::json!(checkpoint);
        }

        let event: SessionEventWithMetadata = serde_json::from_value(wire.clone()).unwrap();
        assert_eq!(event.checkpoint, checkpoint);
        assert_eq!(event.event.event_type, "session.info");
        assert_eq!(serde_json::to_value(event).unwrap(), wire);
        let typed: SessionEventWithMetadata<TypedSessionEvent> =
            serde_json::from_value(wire.clone()).unwrap();
        assert_eq!(typed.checkpoint, checkpoint);
        assert_eq!(serde_json::to_value(typed).unwrap(), wire);
    }
}

#[test]
fn session_events_preserve_existing_struct_literals() {
    let wire = event_envelope(
        "user.message",
        serde_json::json!({"content": "Existing application"}),
    );
    let event = SessionEvent {
        id: wire["id"].as_str().unwrap().to_owned(),
        timestamp: wire["timestamp"].as_str().unwrap().to_owned(),
        parent_id: Some(wire["parentId"].as_str().unwrap().to_owned()),
        ephemeral: Some(true),
        agent_id: Some("test-agent".to_owned()),
        debug_cli_received_at_ms: None,
        debug_ws_forwarded_at_ms: None,
        event_type: "user.message".to_owned(),
        data: wire["data"].clone(),
    };
    let decoded: TypedSessionEvent = serde_json::from_value(wire.clone()).unwrap();
    let typed = TypedSessionEvent {
        id: decoded.id,
        timestamp: decoded.timestamp,
        parent_id: decoded.parent_id,
        ephemeral: decoded.ephemeral,
        agent_id: decoded.agent_id,
        payload: decoded.payload,
    };
    assert_eq!(serde_json::to_value(event).unwrap(), wire);
    assert_eq!(serde_json::to_value(typed).unwrap(), wire);
}

#[test]
fn image_tool_usage_round_trips_without_inline_payload_storage() {
    for (success, total_nano_aiu) in [(true, 0.0), (true, 598_500_000.0), (false, 598_500_000.0)] {
        let wire = event_envelope(
            "tool.execution_complete",
            serde_json::json!({
                "toolCallId": "image-call",
                "success": success,
                "toolUsage": {
                    "requestId": "image-usage",
                    "model": "gpt-image-2.5-flare",
                    "inputTokens": 21,
                    "outputTokens": 196,
                    "totalTokens": 217,
                    "inputTokensDetails": { "textTokens": 21, "imageTokens": 0 },
                    "outputTokensDetails": { "textTokens": 0, "imageTokens": 196 },
                    "copilotUsage": { "totalNanoAiu": total_nano_aiu }
                }
            }),
        );
        let event: TypedSessionEvent = serde_json::from_value(wire.clone()).unwrap();
        let SessionEventData::ToolExecutionComplete(data) = &event.payload else {
            panic!("expected tool-completion event");
        };
        assert_eq!(
            std::mem::size_of_val(&data.tool_usage),
            std::mem::size_of::<usize>(),
            "Image usage should occupy only an optional pointer in the shared event"
        );
        assert_eq!(
            data.tool_usage.as_ref().unwrap().model,
            "gpt-image-2.5-flare"
        );
        assert_eq!(serde_json::to_value(event).unwrap(), wire);
    }
}
