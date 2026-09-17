// Unit tests for generated session-event payloads.

#![allow(clippy::unwrap_used)]

use github_copilot_sdk::session_events::{
    PermissionApprovalEvaluation, PermissionApprovalEvaluationEvaluationStage,
    PermissionApprovalEvaluationJudgeStatus, PermissionApprovalEvaluationReasonCode,
    UserMessageData,
};

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
