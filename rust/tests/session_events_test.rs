// Unit tests for generated session-event payloads.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::session_events::{
    IndexedSearchData, PermissionApprovalEvaluation, PermissionApprovalEvaluationEvaluationStage,
    PermissionApprovalEvaluationJudgeStatus, PermissionApprovalEvaluationReasonCode,
    SandboxDecisionData, SessionEventData, TypedSessionEvent, UserMessageData,
};

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
fn indexed_search_variants_round_trip_full_event_envelopes() {
    let payloads = [
        serde_json::json!({
            "kind": "status",
            "state": "ready"
        }),
        serde_json::json!({
            "kind": "startup",
            "outcome": "failed",
            "fileCount": 0.0,
            "startupDurationMs": 1.25,
            "forcedByEnv": false,
            "warmStart": false,
            "disabledReason": "workspace_not_local",
            "errorMessage": "synthetic startup diagnostic",
            "eligible": false
        }),
        serde_json::json!({
            "kind": "server_error",
            "errorType": "unexpected_exit",
            "exitCode": 0.0,
            "errorMessage": "synthetic server diagnostic"
        }),
        serde_json::json!({
            "kind": "incremental",
            "phase": "updated",
            "changedFileCount": 0.0,
            "addedFileCount": 2.0,
            "deletedFileCount": 1.0,
            "totalChangeCount": 3.0,
            "walkDurationMs": 0.0,
            "updateDurationMs": 2.5,
            "totalDurationMs": 3.75
        }),
    ];

    for payload in payloads {
        let wire = event_envelope("session.indexed_search", payload);
        let event: TypedSessionEvent = serde_json::from_value(wire.clone()).unwrap();
        let SessionEventData::SessionIndexedSearch(data) = &event.payload else {
            panic!("expected indexed-search event");
        };
        let kind = match data {
            IndexedSearchData::Status(_) => "status",
            IndexedSearchData::Startup(_) => "startup",
            IndexedSearchData::ServerError(_) => "server_error",
            IndexedSearchData::Incremental(_) => "incremental",
        };

        assert_eq!(kind, wire["data"]["kind"].as_str().unwrap());
        assert_eq!(serde_json::to_value(event).unwrap(), wire);
    }
}

#[test]
fn sandbox_decision_variants_round_trip_full_event_envelopes() {
    let payloads = [
        serde_json::json!({
            "kind": "policy_resolved",
            "control": "filesystem",
            "outcome": "resolved",
            "backend": "bubblewrap",
            "policySource": "user_policy",
            "readwritePathsCount": 0,
            "readonlyPathsCount": 1,
            "deniedPathsCount": 0,
            "addCurrentWorkingDirectory": false,
            "allowOutbound": false,
            "allowLocalNetwork": false,
            "proxyMode": "none",
            "allowBypass": false,
            "gitAuth": false,
            "ghAuth": false,
            "keychainAccess": false,
            "effectiveFilesystemPolicy": {
                "readwritePaths": [],
                "readonlyPaths": ["fixtures"],
                "deniedPaths": []
            },
            "degradationReason": "denied_paths_unsupported"
        }),
        serde_json::json!({
            "kind": "spawn_completed",
            "control": "process",
            "outcome": "succeeded",
            "backend": "bubblewrap",
            "durationMs": 0.25
        }),
        serde_json::json!({
            "kind": "enforcement_state",
            "control": "process",
            "outcome": "engaged",
            "backend": "bubblewrap",
            "attestation": "spawn_succeeded",
            "command": "echo synthetic"
        }),
        serde_json::json!({
            "kind": "access_denied",
            "control": "filesystem",
            "outcome": "denied",
            "denialClass": "filesystem_read",
            "attestation": "builtin_policy_checked",
            "confidence": "captured",
            "deniedResource": "fixtures/restricted.txt",
            "command": "cat fixtures/restricted.txt",
            "processName": "cat"
        }),
        serde_json::json!({
            "kind": "bypass_decided",
            "control": "bypass",
            "outcome": "declined",
            "source": "user_prompted",
            "denialClass": "network_outbound",
            "confidence": "policy_corroborated",
            "deniedResource": "example.invalid",
            "command": "synthetic-request",
            "processName": "test-client"
        }),
        serde_json::json!({
            "kind": "permissive_retry_decided",
            "control": "bypass",
            "outcome": "approved",
            "source": "model_requested",
            "denialClass": "filesystem_read",
            "confidence": "sandbox_reported",
            "deniedResource": "fixtures/retry.txt",
            "command": "cat fixtures/retry.txt",
            "processName": "cat"
        }),
        serde_json::json!({
            "kind": "permissive_retry_completed",
            "control": "bypass",
            "outcome": "succeeded",
            "denialClass": "filesystem_read",
            "confidence": "output_classified",
            "deniedResource": "fixtures/retry.txt",
            "command": "cat fixtures/retry.txt",
            "processName": "cat"
        }),
    ];

    for mut payload in payloads {
        let fields = payload.as_object_mut().unwrap();
        fields.insert("platform".to_string(), serde_json::json!("linux"));
        fields.insert("enforcementPoint".to_string(), serde_json::json!("shell"));
        fields.insert("toolCallId".to_string(), serde_json::json!("sandbox-tool"));
        let wire = event_envelope("sandbox.decision", payload);
        let event: TypedSessionEvent = serde_json::from_value(wire.clone()).unwrap();
        let SessionEventData::SandboxDecision(data) = &event.payload else {
            panic!("expected sandbox-decision event");
        };
        let kind = match data {
            SandboxDecisionData::PolicyResolved(_) => "policy_resolved",
            SandboxDecisionData::SpawnCompleted(_) => "spawn_completed",
            SandboxDecisionData::EnforcementState(_) => "enforcement_state",
            SandboxDecisionData::AccessDenied(_) => "access_denied",
            SandboxDecisionData::BypassDecided(_) => "bypass_decided",
            SandboxDecisionData::PermissiveRetryDecided(_) => "permissive_retry_decided",
            SandboxDecisionData::PermissiveRetryCompleted(_) => "permissive_retry_completed",
        };

        assert_eq!(kind, wire["data"]["kind"].as_str().unwrap());
        assert_eq!(serde_json::to_value(event).unwrap(), wire);
    }
}

#[test]
fn ordinary_and_empty_payloads_round_trip_full_event_envelopes() {
    for (event_type, payload) in [
        (
            "user.message",
            serde_json::json!({"content": "synthetic message", "messageId": "message-123"}),
        ),
        ("session.idle", serde_json::json!({})),
    ] {
        let wire = event_envelope(event_type, payload);
        let event: TypedSessionEvent = serde_json::from_value(wire.clone()).unwrap();
        match &event.payload {
            SessionEventData::UserMessage(data) => assert_eq!(data.content, "synthetic message"),
            SessionEventData::SessionIdle(_) => assert_eq!(event_type, "session.idle"),
            _ => panic!("unexpected event variant"),
        }
        assert_eq!(serde_json::to_value(event).unwrap(), wire);
    }
}

#[test]
fn approval_evaluation_preserves_protocol_unknown_values() {
    let wire = serde_json::json!({
        "evaluationStage": "unknown",
        "judgeStatus": "unknown",
        "reasonCode": "unknown"
    });
    let data: PermissionApprovalEvaluation = serde_json::from_value(wire.clone()).unwrap();

    assert_eq!(
        data.evaluation_stage,
        PermissionApprovalEvaluationEvaluationStage::UnknownValue
    );
    assert_eq!(
        data.judge_status,
        PermissionApprovalEvaluationJudgeStatus::UnknownValue
    );
    assert_eq!(
        data.reason_code,
        PermissionApprovalEvaluationReasonCode::UnknownValue
    );
    assert_eq!(serde_json::to_value(data).unwrap(), wire);
}

#[test]
fn approval_evaluation_accepts_future_values_without_confusing_them_with_protocol_unknown() {
    let data: PermissionApprovalEvaluation = serde_json::from_value(serde_json::json!({
        "evaluationStage": "future-stage",
        "judgeStatus": "future-status",
        "reasonCode": "future-reason"
    }))
    .unwrap();

    assert_eq!(
        data.evaluation_stage,
        PermissionApprovalEvaluationEvaluationStage::Unknown
    );
    assert_eq!(
        data.judge_status,
        PermissionApprovalEvaluationJudgeStatus::Unknown
    );
    assert_eq!(
        data.reason_code,
        PermissionApprovalEvaluationReasonCode::Unknown
    );
}

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
