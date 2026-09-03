#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{AutopilotObjectiveGetStateResult, AutopilotObjectiveStatus};

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
    assert_eq!(active.status, AutopilotObjectiveStatus::Active);
    assert!(active.pause_reason.is_none());
    assert!(active.completion_summary.is_none());
    assert!(active.credit_limit.is_none());
    let active_json = serde_json::to_value(active).unwrap();
    assert!(active_json.get("pauseReason").is_none());
    assert!(active_json.get("completionSummary").is_none());
    assert!(active_json.get("creditLimit").is_none());

    let paused: AutopilotObjectiveGetStateResult = serde_json::from_str(
        r#"{"state":{"id":2,"objective":"Wait for approval","status":"paused","turnCount":3,"pauseReason":"Approval required","creditCountNanoAiu":"9007199254740993","creditLimit":{"creditsUsed":9.007199254740993,"creditsUsedNanoAiu":"9007199254740993"}}}"#,
    )
    .unwrap();
    let paused = paused.state.unwrap();
    assert_eq!(paused.status, AutopilotObjectiveStatus::Paused);
    assert_eq!(paused.pause_reason.as_deref(), Some("Approval required"));
    assert_eq!(paused.credit_count_nano_aiu, "9007199254740993");
    assert!(paused.credit_limit.as_ref().unwrap().credits.is_none());

    let completed: AutopilotObjectiveGetStateResult = serde_json::from_str(
        r#"{"state":{"id":3,"objective":"Publish the SDK","status":"completed","turnCount":4,"completionSummary":"Published","creditCountNanoAiu":"9007199254740994","creditLimit":{"credits":2.5,"creditsUsed":1.25,"creditsUsedNanoAiu":"1250000000"}}}"#,
    )
    .unwrap();
    let completed = completed.state.unwrap();
    assert_eq!(completed.status, AutopilotObjectiveStatus::Completed);
    assert_eq!(completed.completion_summary.as_deref(), Some("Published"));
    let credit_limit = completed.credit_limit.unwrap();
    assert_eq!(credit_limit.credits, Some(2.5));
    assert_eq!(credit_limit.credits_used_nano_aiu, "1250000000");
}
