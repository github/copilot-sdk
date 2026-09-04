// Unit tests for generated API types -- struct construction and field
// access. These do not require a client, session, or replay proxy.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{
    Extension, ExtensionList, ExtensionSource, ExtensionStatus, ExtensionsDisableRequest,
    ExtensionsEnableRequest, FleetStartRequest, FleetStartResult, ModelSwitchAutoTierRequest,
    ModelSwitchAutoTierResult, ModelSwitchAutoTierStatus, QueuePendingItems, QueuePendingItemsKind,
    SendAgentMode, TasksStartAgentRequest,
};
use github_copilot_sdk::session_events::{
    PermissionRequest, PermissionRequestedData, SessionEventData, TypedSessionEvent,
};
use github_copilot_sdk::{AutoTier, AutoTierPreference, SetModelOptions};

#[test]
fn session_events_deserialize_auto_tier() {
    for event_type in ["session.start", "session.resume"] {
        for (tier, wire_tier) in [
            (Some(AutoTier::Efficiency), Some("efficiency")),
            (Some(AutoTier::Balance), Some("balance")),
            (Some(AutoTier::Intelligence), Some("intelligence")),
            (None, None),
        ] {
            let mut wire = serde_json::json!({
                "id": "11111111-1111-1111-1111-111111111111",
                "timestamp": "2026-08-28T00:00:00Z",
                "parentId": null,
                "type": event_type,
                "data": {
                    "sessionId": "test-session", "version": 1,
                    "producer": "copilot", "copilotVersion": "1.0.82-1",
                    "startTime": "2026-08-28T00:00:00Z",
                    "resumeTime": "2026-08-28T00:00:00Z", "eventCount": 1
                }
            });
            if let Some(wire_tier) = wire_tier {
                wire["data"]["autoTier"] = serde_json::json!(wire_tier);
            }
            let event: TypedSessionEvent = serde_json::from_value(wire).unwrap();
            let actual: Option<AutoTier> = match event.payload {
                SessionEventData::SessionStart(data) if event_type == "session.start" => {
                    data.auto_tier
                }
                SessionEventData::SessionResume(data) if event_type == "session.resume" => {
                    data.auto_tier
                }
                _ => panic!("expected {event_type}"),
            };
            assert_eq!(actual, tier);
        }
    }
}

#[test]
fn extension_running_has_expected_status_and_source() {
    let extension = running_extension("project:demo", "demo");
    assert_eq!(extension.status, ExtensionStatus::Running);
    assert_eq!(extension.source, ExtensionSource::Project);
}

#[test]
fn disable_and_enable_requests_share_the_same_id() {
    let disable = ExtensionsDisableRequest {
        id: "project:demo".to_string(),
    };
    let enable = ExtensionsEnableRequest {
        id: disable.id.clone(),
    };
    assert_eq!(disable.id, enable.id);
}

#[test]
fn extension_list_contains_newly_added_extension_by_name() {
    let list = ExtensionList {
        extensions: vec![running_extension("project:late", "late")],
    };
    assert!(list.extensions.iter().any(|e| e.name == "late"));
}

#[test]
fn failed_extension_reports_failed_status() {
    let mut extension = running_extension("project:broken", "broken");
    extension.status = ExtensionStatus::Failed;
    assert_eq!(extension.status, ExtensionStatus::Failed);
}

#[test]
fn multiple_extensions_have_distinct_ids() {
    let list = ExtensionList {
        extensions: vec![
            running_extension("project:first", "first"),
            running_extension("user:second", "second"),
        ],
    };
    assert_eq!(list.extensions.len(), 2);
    assert_ne!(list.extensions[0].id, list.extensions[1].id);
}

#[test]
fn disabled_extension_preserves_disabled_status() {
    let mut extension = running_extension("project:disabled", "disabled");
    extension.status = ExtensionStatus::Disabled;
    assert_eq!(extension.status, ExtensionStatus::Disabled);
}

#[test]
fn fleet_start_request_and_result_fields_are_accessible() {
    let request = FleetStartRequest {
        prompt: Some("Use the custom tool".to_string()),
    };
    let result = FleetStartResult { started: true };
    assert_eq!(request.prompt.as_deref(), Some("Use the custom tool"));
    assert!(result.started);
}

#[test]
fn tasks_start_agent_request_fields_are_accessible() {
    let request = TasksStartAgentRequest {
        agent_type: "general-purpose".to_string(),
        prompt: "Say hi".to_string(),
        name: "sdk-test-task".to_string(),
        description: Some("SDK task agent".to_string()),
        model: None,
    };
    assert_eq!(request.agent_type, "general-purpose");
    assert_eq!(request.name, "sdk-test-task");
    assert_eq!(request.description.as_deref(), Some("SDK task agent"));
}

#[test]
fn permission_event_exposes_managed_approval_required() {
    let data: PermissionRequestedData = serde_json::from_value(serde_json::json!({
        "permissionRequest": {
            "kind": "read",
            "intention": "Read managed content",
            "path": "/workspace/file.txt",
            "managedApprovalRequired": true
        },
        "requestId": "permission-1"
    }))
    .unwrap();

    let PermissionRequest::Read(request) = data.permission_request else {
        panic!("expected read permission request");
    };
    assert_eq!(request.managed_approval_required, Some(true));
}

#[test]
fn queue_pending_message_id_uses_camel_case_wire_name() {
    let item = QueuePendingItems {
        agent_mode: SendAgentMode::Interactive,
        display_text: "second message".to_string(),
        id: "batch-1".to_string(),
        kind: QueuePendingItemsKind::Message,
        message_id: Some("message-2".to_string()),
    };

    let serialized = serde_json::to_value(&item).unwrap();
    assert_eq!(serialized["id"], "batch-1");
    assert_eq!(serialized["messageId"], "message-2");

    let deserialized: QueuePendingItems = serde_json::from_value(serialized).unwrap();
    assert_eq!(deserialized.message_id.as_deref(), Some("message-2"));
}

#[test]
fn queue_pending_message_id_is_optional_for_older_hosts() {
    let item: QueuePendingItems = serde_json::from_value(serde_json::json!({
        "agentMode": "interactive",
        "displayText": "/model gpt-5",
        "id": "command-1",
        "kind": "command"
    }))
    .unwrap();

    assert_eq!(item.message_id, None);
    assert!(
        serde_json::to_value(item)
            .unwrap()
            .get("messageId")
            .is_none()
    );
}

fn running_extension(id: &str, name: &str) -> Extension {
    Extension {
        id: id.to_string(),
        name: name.to_string(),
        pid: Some(42),
        source: if id.starts_with("user:") {
            ExtensionSource::User
        } else {
            ExtensionSource::Project
        },
        status: ExtensionStatus::Running,
    }
}

#[test]
fn switch_auto_tier_request_serializes_explicit_null_tier() {
    // `autoTier` is a required field whose null value means "use provider-default
    // routing", so it must survive serialization rather than being skipped.
    let request = ModelSwitchAutoTierRequest {
        auto_tier: None,
        source: None,
    };
    let wire = serde_json::to_value(&request).unwrap();

    assert_eq!(wire.get("autoTier"), Some(&serde_json::Value::Null));
    assert!(wire.get("source").is_none());
}

#[test]
fn switch_auto_tier_request_serializes_each_tier() {
    for (tier, expected) in [
        (AutoTier::Efficiency, "efficiency"),
        (AutoTier::Balance, "balance"),
        (AutoTier::Intelligence, "intelligence"),
    ] {
        let request = ModelSwitchAutoTierRequest {
            auto_tier: Some(tier),
            source: None,
        };
        let wire = serde_json::to_value(&request).unwrap();
        assert_eq!(wire["autoTier"], serde_json::json!(expected));
    }
}

#[test]
fn switch_auto_tier_result_deserializes_full_snapshot() {
    let result: ModelSwitchAutoTierResult = serde_json::from_value(serde_json::json!({
        "status": "pending",
        "effectiveAutoTier": "balance",
        "pendingAutoTier": "intelligence",
        "activatingAutoTier": null,
        "supersededAutoTier": null
    }))
    .unwrap();

    assert_eq!(result.status, ModelSwitchAutoTierStatus::Pending);
    assert_eq!(result.effective_auto_tier, Some(AutoTier::Balance));
    assert_eq!(result.pending_auto_tier, Some(AutoTier::Intelligence));
    assert_eq!(result.activating_auto_tier, None);
}

#[test]
fn set_model_options_distinguishes_unset_tier_from_reset() {
    let untouched = SetModelOptions::default();
    assert_eq!(untouched.auto_tier, None);

    let explicit = SetModelOptions::default().with_auto_tier(AutoTier::Intelligence);
    assert_eq!(
        explicit.auto_tier,
        Some(AutoTierPreference::Tier(AutoTier::Intelligence))
    );

    let cleared = SetModelOptions::default().with_reset_auto_tier();
    assert_eq!(cleared.auto_tier, Some(AutoTierPreference::Reset));
}
