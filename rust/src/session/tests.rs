/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use serde_json::json;

use super::{
    build_mode_post_create_patch, has_managed_settings, is_autopilot_continuation_idle,
    permission_request_data, permission_response_params,
};
use crate::handler::PermissionResult;
use crate::types::{
    PermissionDecisionContext, PermissionDecisionOutcome, PermissionDecisionSource,
    PermissionDecisionSurface, RequestId, SessionEvent, SessionId,
};

#[test]
fn identifies_only_autopilot_continuation_idles() {
    let mut event = SessionEvent {
        id: "event-1".to_string(),
        timestamp: "2026-01-01T00:00:00Z".to_string(),
        parent_id: None,
        ephemeral: None,
        agent_id: None,
        debug_cli_received_at_ms: None,
        debug_ws_forwarded_at_ms: None,
        event_type: "session.idle".to_string(),
        data: json!({ "mode": "autopilot" }),
    };

    assert!(is_autopilot_continuation_idle(&event));

    event.data = json!({ "mode": "interactive" });
    assert!(!is_autopilot_continuation_idle(&event));

    event.data = json!({});
    assert!(!is_autopilot_continuation_idle(&event));
}

#[test]
fn empty_mode_post_patch_sets_empty_included_builtin_skills() {
    let patch =
        build_mode_post_create_patch(crate::ClientMode::Empty, None, None, None, None, None)
            .expect("empty mode always sends a patch");
    assert_eq!(
        patch.included_builtin_skills,
        Some(Vec::new()),
        "empty mode must fail closed with an empty includedBuiltinSkills list"
    );
    assert_eq!(patch.installed_plugins.as_ref().map(|p| p.len()), Some(0));
    // Serializes as an explicit empty array (not omitted).
    let value = serde_json::to_value(&patch).expect("serialize patch");
    assert_eq!(value["includedBuiltinSkills"], serde_json::json!([]));
}

#[test]
fn empty_mode_post_patch_preserves_explicit_builtin_skill_allowlist() {
    let patch = build_mode_post_create_patch(
        crate::ClientMode::Empty,
        Some(false),
        Some(false),
        Some(true),
        Some(true),
        Some(vec!["code-review".to_string()]),
    )
    .expect("empty mode always sends a patch");
    assert_eq!(
        patch.included_builtin_skills,
        Some(vec!["code-review".to_string()])
    );
}

#[test]
fn copilot_cli_mode_does_not_inject_included_builtin_skills() {
    // No fields set -> no patch at all.
    assert!(
        build_mode_post_create_patch(crate::ClientMode::CopilotCli, None, None, None, None, None)
            .is_none()
    );
    // A field set -> patch sent, but skills field stays absent.
    let patch = build_mode_post_create_patch(
        crate::ClientMode::CopilotCli,
        Some(true),
        None,
        None,
        None,
        None,
    )
    .expect("a set field triggers a patch");
    assert_eq!(patch.included_builtin_skills, None);
    assert!(patch.installed_plugins.is_none());
    let value = serde_json::to_value(&patch).expect("serialize patch");
    assert!(value.get("includedBuiltinSkills").is_none());

    let patch = build_mode_post_create_patch(
        crate::ClientMode::CopilotCli,
        None,
        None,
        None,
        None,
        Some(vec!["code-review".to_string()]),
    )
    .expect("an explicit allowlist triggers a patch");
    assert_eq!(
        patch.included_builtin_skills,
        Some(vec!["code-review".to_string()])
    );
}

#[test]
fn direct_injection_enables_managed_safeguards() {
    let settings = crate::types::ManagedSettings::default();
    assert!(has_managed_settings(None, Some(&settings)));
    assert!(!has_managed_settings(None, None));
}

fn attribution_context() -> PermissionDecisionContext {
    PermissionDecisionContext {
        outcome: PermissionDecisionOutcome::AutoApproved,
        response_capability: None,
        source: PermissionDecisionSource::AssistedApproval,
        surface: PermissionDecisionSurface::CopilotApp,
    }
}

#[test]
fn response_params_omit_decision_context_without_attribution() {
    for (result, expected) in [
        (
            PermissionResult::approve_once(),
            json!({ "kind": "approve-once" }),
        ),
        (PermissionResult::reject(None), json!({ "kind": "reject" })),
        (
            PermissionResult::reject(Some("bad".to_string())),
            json!({ "kind": "reject", "feedback": "bad" }),
        ),
        (
            PermissionResult::user_not_available(),
            json!({ "kind": "user-not-available" }),
        ),
    ] {
        let params = permission_response_params(
            &SessionId::from("session-1"),
            &RequestId::from("permission-1"),
            &result,
        )
        .unwrap();
        assert_eq!(
            params,
            json!({
                "sessionId": "session-1",
                "requestId": "permission-1",
                "result": expected,
            })
        );
    }
}

#[test]
fn response_params_forward_decision_context_alongside_result() {
    let params = permission_response_params(
        &SessionId::from("session-1"),
        &RequestId::from("permission-1"),
        &PermissionResult::approve_once().with_context(attribution_context()),
    )
    .unwrap();
    assert_eq!(
        params,
        json!({
            "sessionId": "session-1",
            "requestId": "permission-1",
            "result": { "kind": "approve-once" },
            "decisionContext": {
                "outcome": "auto_approved",
                "source": "assisted_approval",
                "surface": "copilot_app",
            },
        })
    );
    // The context is a sibling of `result`, never nested inside it.
    assert!(params["result"].get("decisionContext").is_none());
}

#[test]
fn response_params_suppressed_for_no_result() {
    assert!(
        permission_response_params(
            &SessionId::from("session-1"),
            &RequestId::from("permission-1"),
            &PermissionResult::NoResult,
        )
        .is_none()
    );
}

#[test]
fn with_context_is_a_no_op_on_no_result() {
    let result = PermissionResult::no_result().with_context(attribution_context());
    assert!(matches!(result, PermissionResult::NoResult));
}

#[test]
fn with_context_replaces_rather_than_nests() {
    let result = PermissionResult::approve_once()
        .with_context(attribution_context())
        .with_context(PermissionDecisionContext {
            outcome: PermissionDecisionOutcome::PromptedUser,
            response_capability: None,
            source: PermissionDecisionSource::HumanResponse,
            surface: PermissionDecisionSurface::Sdk,
        });
    let params = permission_response_params(
        &SessionId::from("session-1"),
        &RequestId::from("permission-1"),
        &result,
    )
    .unwrap();
    assert_eq!(
        params["decisionContext"],
        json!({
            "outcome": "prompted_user",
            "source": "human_response",
            "surface": "sdk",
        })
    );
}

#[test]
fn permission_request_data_reads_nested_managed_approval_metadata() {
    let data = permission_request_data(
        &json!({
            "requestId": "permission-1",
            "permissionRequest": {
                "kind": "read",
                "managedApprovalRequired": true,
                "path": "/workspace/file.txt"
            }
        }),
        false,
    );

    assert_eq!(data.managed_approval_required, Some(true));
    assert_eq!(
        data.extra["permissionRequest"]["path"],
        "/workspace/file.txt"
    );
}

#[test]
fn permission_request_data_preserves_managed_flag_when_other_fields_are_malformed() {
    let data = permission_request_data(
        &json!({
            "requestId": "permission-1",
            "permissionRequest": {
                "kind": "read",
                "managedApprovalRequired": true,
                "toolCallId": 42
            }
        }),
        false,
    );

    assert_eq!(data.managed_approval_required, Some(true));
    assert_eq!(data.extra["requestId"], "permission-1");
}

#[test]
fn permission_request_data_fails_closed_for_malformed_managed_flag() {
    let data = permission_request_data(
        &json!({
            "requestId": "permission-1",
            "permissionRequest": {
                "kind": "read",
                "managedApprovalRequired": "yes",
                "path": "/workspace/file.txt"
            }
        }),
        false,
    );

    assert_eq!(data.managed_approval_required, Some(true));
}

#[test]
fn permission_request_data_preserves_valid_false_managed_flag() {
    let data = permission_request_data(
        &json!({
            "requestId": "permission-1",
            "permissionRequest": {
                "kind": "read",
                "managedApprovalRequired": false,
                "path": "/workspace/file.txt"
            }
        }),
        false,
    );

    assert_eq!(data.managed_approval_required, Some(false));
}
