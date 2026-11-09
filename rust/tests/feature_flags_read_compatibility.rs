#![cfg(test)]
#![allow(clippy::unwrap_used)]

use std::collections::HashMap;

use github_copilot_sdk::Error;
use github_copilot_sdk::rpc::{
    FeatureFlagServiceReadParams, FeatureFlagServiceReadResult, SessionFeatureFlagsReadOptions,
    SessionFeatureFlagsReadParams, SessionFeatureFlagsReadResult,
};
use github_copilot_sdk::session::Session;
use github_copilot_sdk::types::SessionFeatureFlagsReadResultWithDisplayState;
use serde_json::json;

async fn _legacy_read_signature(
    session: &Session,
    params: SessionFeatureFlagsReadParams,
) -> Result<SessionFeatureFlagsReadResult, Error> {
    session.rpc().feature_flags().read(params).await
}

async fn _display_read_signature(
    session: &Session,
    params: SessionFeatureFlagsReadOptions,
) -> Result<SessionFeatureFlagsReadResultWithDisplayState, Error> {
    session
        .rpc()
        .feature_flags()
        .read_with_options(params)
        .await
}

#[test]
fn legacy_shared_read_input_keeps_complete_literals_and_patterns() {
    let params = FeatureFlagServiceReadParams {
        after_revision: None,
        flags: None,
        overrides: None,
        wait_for_exp_response: None,
    };
    assert_eq!(serde_json::to_value(&params).unwrap(), json!({}));
    let FeatureFlagServiceReadParams {
        after_revision: _,
        flags: _,
        overrides: _,
        wait_for_exp_response: _,
    } = params;
}

#[test]
fn display_read_options_keep_extended_wire_values() {
    let params = SessionFeatureFlagsReadOptions::new()
        .display_only(true)
        .service_id("display-service");
    assert_eq!(
        serde_json::to_value(params).unwrap(),
        json!({ "displayOnly": true, "serviceId": "display-service" }),
    );
}

#[test]
fn legacy_feature_flag_results_keep_complete_literals_and_patterns() {
    let response = FeatureFlagServiceReadResult {
        flags: HashMap::from([("enabled".to_owned(), true)]),
        overrides: vec![Some(false), None],
        snapshot: Default::default(),
    };
    let FeatureFlagServiceReadResult {
        flags,
        overrides,
        snapshot,
    } = response;
    let response = SessionFeatureFlagsReadResult {
        flags,
        overrides,
        snapshot,
    };
    let SessionFeatureFlagsReadResult {
        flags,
        overrides,
        snapshot,
    } = response;

    assert!(flags["enabled"]);
    assert_eq!(overrides, vec![Some(false), None]);
    assert!(!snapshot.active);

    let response: SessionFeatureFlagsReadResultWithDisplayState = serde_json::from_value(json!({
        "flags": flags,
        "overrides": overrides,
        "snapshot": snapshot,
        "displayState": { "available": true },
    }))
    .unwrap();
    assert_eq!(response.display_state, Some(json!({ "available": true })));
    assert!(response.flags["enabled"]);
    assert_eq!(response.overrides, vec![Some(false), None]);
}
