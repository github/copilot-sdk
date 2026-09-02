// Unit tests for generated API types -- struct construction and field
// access. These do not require a client, session, or replay proxy.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{
    AgentInfo, AgentList, Extension, ExtensionList, ExtensionSource, ExtensionStatus,
    ExtensionsDisableRequest, ExtensionsEnableRequest, FleetStartRequest, FleetStartResult,
    ServerAgentList, TasksStartAgentRequest,
};
use github_copilot_sdk::session_events::{
    PermissionRequest, PermissionRequestedData, SessionEventData, TypedSessionEvent,
};
use github_copilot_sdk::{AutoTier, CustomAgentHandoff};

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
fn agent_info_handoffs_round_trip_in_order() {
    let wire = serde_json::json!({
        "id": "planner",
        "name": "planner",
        "displayName": "Planner",
        "description": "Plans work",
        "handoffs": [
            {
                "label": "Implement",
                "agent": "implementer"
            },
            {
                "label": "Review",
                "agent": "reviewer",
                "prompt": "Review the implementation.",
                "send": true,
                "model": "gpt-5.4"
            }
        ]
    });

    let info: AgentInfo = serde_json::from_value(wire.clone()).unwrap();
    let handoffs: &[CustomAgentHandoff] = info.handoffs.as_deref().unwrap();
    assert_eq!(handoffs.len(), 2);
    assert_eq!(handoffs[0].label, "Implement");
    assert_eq!(handoffs[0].agent, "implementer");
    assert_eq!(handoffs[0].send, None);
    assert_eq!(handoffs[1].label, "Review");
    assert_eq!(
        handoffs[1].prompt.as_deref(),
        Some("Review the implementation.")
    );
    assert_eq!(handoffs[1].send, Some(true));
    assert_eq!(handoffs[1].model.as_deref(), Some("gpt-5.4"));
    assert_eq!(serde_json::to_value(info).unwrap(), wire);
}

#[test]
fn agent_info_omits_handoffs_when_absent() {
    let info: AgentInfo = serde_json::from_value(serde_json::json!({
        "id": "planner",
        "name": "planner",
        "displayName": "Planner",
        "description": "Plans work"
    }))
    .unwrap();
    assert!(info.handoffs.is_none());
    assert!(
        serde_json::to_value(info)
            .unwrap()
            .get("handoffs")
            .is_none()
    );
}

#[test]
fn agent_list_rpcs_expose_handoffs() {
    let wire = serde_json::json!({
        "agents": [{
            "id": "planner",
            "name": "planner",
            "displayName": "Planner",
            "description": "Plans work",
            "handoffs": [{
                "label": "Implement",
                "agent": "implementer"
            }]
        }]
    });

    let discovered: ServerAgentList = serde_json::from_value(wire.clone()).unwrap();
    let session_agents: AgentList = serde_json::from_value(wire).unwrap();
    assert_eq!(
        discovered.agents[0].handoffs.as_ref().unwrap()[0].agent,
        "implementer"
    );
    assert_eq!(
        session_agents.agents[0].handoffs.as_ref().unwrap()[0].agent,
        "implementer"
    );
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
