// Unit tests for generated API types -- struct construction and field
// access. These do not require a client, session, or replay proxy.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{
    AutopilotObjectiveGetStateResult, AutopilotObjectiveStatus, Extension, ExtensionList,
    ExtensionSource, ExtensionStatus, ExtensionsDisableRequest, ExtensionsEnableRequest,
    FleetStartRequest, FleetStartResult, TasksStartAgentRequest,
};
use github_copilot_sdk::session_events::{PermissionRequest, PermissionRequestedData};

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
fn autopilot_objective_state_preserves_canonical_payloads() {
    let no_objective: AutopilotObjectiveGetStateResult =
        serde_json::from_str(r#"{"state":null}"#).unwrap();
    assert!(no_objective.state.is_none());

    let active: AutopilotObjectiveGetStateResult = serde_json::from_str(
        r#"{"state":{"id":1,"objective":"Ship the release","status":"active","turnCount":2,"creditCountNanoAiu":"0"}}"#,
    )
    .unwrap();
    let active = active.state.unwrap();
    assert_eq!(active.id, 1);
    assert_eq!(active.objective, "Ship the release");
    assert_eq!(active.status, AutopilotObjectiveStatus::Active);
    assert_eq!(active.turn_count, 2);
    assert_eq!(active.credit_count_nano_aiu, "0");
    let active_json = serde_json::to_value(active).unwrap();
    assert!(active_json.get("pauseReason").is_none());
    assert!(active_json.get("completionSummary").is_none());
    assert!(active_json.get("creditLimit").is_none());

    let paused: AutopilotObjectiveGetStateResult = serde_json::from_str(
        r#"{"state":{"id":2,"objective":"Wait for approval","status":"paused","turnCount":3,"pauseReason":"Approval required","creditCountNanoAiu":"9007199254740993","creditLimit":{"creditsUsed":9007199.254740993,"creditsUsedNanoAiu":"9007199254740993"}}}"#,
    )
    .unwrap();
    let paused = paused.state.unwrap();
    assert_eq!(paused.id, 2);
    assert_eq!(paused.objective, "Wait for approval");
    assert_eq!(paused.status, AutopilotObjectiveStatus::Paused);
    assert_eq!(paused.turn_count, 3);
    assert_eq!(paused.pause_reason.as_deref(), Some("Approval required"));
    assert_eq!(paused.credit_count_nano_aiu, "9007199254740993");
    let paused_credit_limit = paused.credit_limit.unwrap();
    assert_eq!(paused_credit_limit.credits, None);
    assert_eq!(paused_credit_limit.credits_used, 9007199.254740993);
    assert_eq!(
        paused_credit_limit.credits_used_nano_aiu,
        "9007199254740993"
    );

    let completed: AutopilotObjectiveGetStateResult = serde_json::from_str(
        r#"{"state":{"id":3,"objective":"Publish the SDK","status":"completed","turnCount":4,"completionSummary":"Published","creditCountNanoAiu":"9007199254740994","creditLimit":{"credits":2.5,"creditsUsed":1.25,"creditsUsedNanoAiu":"1250000000"}}}"#,
    )
    .unwrap();
    let completed = completed.state.unwrap();
    assert_eq!(completed.id, 3);
    assert_eq!(completed.objective, "Publish the SDK");
    assert_eq!(completed.status, AutopilotObjectiveStatus::Completed);
    assert_eq!(completed.turn_count, 4);
    assert_eq!(completed.completion_summary.as_deref(), Some("Published"));
    assert_eq!(completed.credit_count_nano_aiu, "9007199254740994");
    let completed_credit_limit = completed.credit_limit.unwrap();
    assert_eq!(completed_credit_limit.credits, Some(2.5));
    assert_eq!(completed_credit_limit.credits_used, 1.25);
    assert_eq!(completed_credit_limit.credits_used_nano_aiu, "1250000000");
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
