// Unit tests for generated API types -- struct construction and field
// access. These do not require a client, session, or replay proxy.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{
    Extension, ExtensionList, ExtensionSource, ExtensionStatus, ExtensionsDisableRequest,
    ExtensionsEnableRequest, FleetStartRequest, FleetStartResult, ModelSetAllowedModelsRequest,
    ModelSetAllowedModelsResult, TasksStartAgentRequest,
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
fn model_allowed_models_request_and_result_preserve_contract_fields() {
    let replace = ModelSetAllowedModelsRequest {
        allowed_models: Some(vec!["gpt-5.4".to_string(), "gpt-5-mini".to_string()]),
    };
    assert_eq!(
        serde_json::to_value(&replace).unwrap(),
        serde_json::json!({ "allowedModels": ["gpt-5.4", "gpt-5-mini"] })
    );

    let clear = ModelSetAllowedModelsRequest::default();
    assert_eq!(clear.allowed_models, None);
    assert_eq!(serde_json::to_value(&clear).unwrap(), serde_json::json!({}));

    let explicit_null: ModelSetAllowedModelsRequest =
        serde_json::from_value(serde_json::json!({ "allowedModels": null })).unwrap();
    assert_eq!(explicit_null.allowed_models, None);

    let result = ModelSetAllowedModelsResult {
        allowed_models: Some(vec!["gpt-5.4".to_string()]),
        effective_allowed_models: Some(vec!["gpt-5.4".to_string()]),
        fallback_model: Some("gpt-5.4".to_string()),
        model_id: Some("gpt-5.4".to_string()),
    };
    assert_eq!(
        result.allowed_models.as_deref(),
        Some(["gpt-5.4".to_string()].as_slice())
    );
    assert_eq!(
        result.effective_allowed_models.as_deref(),
        Some(["gpt-5.4".to_string()].as_slice())
    );
    assert_eq!(result.fallback_model.as_deref(), Some("gpt-5.4"));
    assert_eq!(result.model_id.as_deref(), Some("gpt-5.4"));
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
