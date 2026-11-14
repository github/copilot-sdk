/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use std::sync::Arc;

use crate::handler::{ApproveAllHandler, PermissionHandler, PermissionResult};
use crate::permission;
use crate::types::{
    PermissionDecision, PermissionRequestData, RequestId, ResumeSessionConfig, SessionConfig,
    SessionId,
};

fn data() -> PermissionRequestData {
    PermissionRequestData {
        extra: serde_json::json!({"tool": "shell"}),
        ..Default::default()
    }
}

/// Apply the same policy-resolution logic that `Client::create_session`
/// uses, so tests exercise the effective handler.
fn resolve_create(mut cfg: SessionConfig) -> Option<Arc<dyn PermissionHandler>> {
    permission::resolve_handler(cfg.permission_handler.take(), cfg.permission_policy.take())
}

fn resolve_resume(mut cfg: ResumeSessionConfig) -> Option<Arc<dyn PermissionHandler>> {
    permission::resolve_handler(cfg.permission_handler.take(), cfg.permission_policy.take())
}

async fn dispatch(handler: &Arc<dyn PermissionHandler>) -> PermissionResult {
    handler
        .handle(SessionId::from("s1"), RequestId::new("1"), data())
        .await
}

#[tokio::test]
async fn approve_all_standalone_produces_handler() {
    let cfg = SessionConfig::default().approve_all_permissions();
    let h = resolve_create(cfg).expect("policy alone yields handler");
    assert!(matches!(
        dispatch(&h).await,
        PermissionResult::Decision {
            decision: PermissionDecision::ApproveOnce(_),
            ..
        }
    ));
}

/// Phase I: order between with_permission_handler and the policy
/// builder must not matter.
#[tokio::test]
async fn approve_all_is_order_independent() {
    let a = SessionConfig::default()
        .with_permission_handler(Arc::new(ApproveAllHandler))
        .approve_all_permissions();
    let b = SessionConfig::default()
        .approve_all_permissions()
        .with_permission_handler(Arc::new(ApproveAllHandler));
    let ha = resolve_create(a).unwrap();
    let hb = resolve_create(b).unwrap();
    assert!(matches!(
        dispatch(&ha).await,
        PermissionResult::Decision {
            decision: PermissionDecision::ApproveOnce(_),
            ..
        }
    ));
    assert!(matches!(
        dispatch(&hb).await,
        PermissionResult::Decision {
            decision: PermissionDecision::ApproveOnce(_),
            ..
        }
    ));
}

#[tokio::test]
async fn deny_all_is_order_independent() {
    let a = SessionConfig::default()
        .with_permission_handler(Arc::new(ApproveAllHandler))
        .deny_all_permissions();
    let b = SessionConfig::default()
        .deny_all_permissions()
        .with_permission_handler(Arc::new(ApproveAllHandler));
    let ha = resolve_create(a).unwrap();
    let hb = resolve_create(b).unwrap();
    assert!(matches!(
        dispatch(&ha).await,
        PermissionResult::Decision {
            decision: PermissionDecision::Reject(_),
            ..
        }
    ));
    assert!(matches!(
        dispatch(&hb).await,
        PermissionResult::Decision {
            decision: PermissionDecision::Reject(_),
            ..
        }
    ));
}

#[tokio::test]
async fn approve_permissions_if_is_order_independent() {
    let predicate =
        |d: &PermissionRequestData| d.extra.get("tool").and_then(|v| v.as_str()) != Some("shell");
    let a = SessionConfig::default()
        .with_permission_handler(Arc::new(ApproveAllHandler))
        .approve_permissions_if(predicate);
    let b = SessionConfig::default()
        .approve_permissions_if(predicate)
        .with_permission_handler(Arc::new(ApproveAllHandler));
    let ha = resolve_create(a).unwrap();
    let hb = resolve_create(b).unwrap();
    assert!(matches!(
        dispatch(&ha).await,
        PermissionResult::Decision {
            decision: PermissionDecision::Reject(_),
            ..
        }
    ));
    assert!(matches!(
        dispatch(&hb).await,
        PermissionResult::Decision {
            decision: PermissionDecision::Reject(_),
            ..
        }
    ));
}

#[tokio::test]
async fn resume_session_config_approve_all_is_order_independent() {
    let a = ResumeSessionConfig::new(SessionId::from("s1"))
        .with_permission_handler(Arc::new(ApproveAllHandler))
        .approve_all_permissions();
    let b = ResumeSessionConfig::new(SessionId::from("s1"))
        .approve_all_permissions()
        .with_permission_handler(Arc::new(ApproveAllHandler));
    let ha = resolve_resume(a).unwrap();
    let hb = resolve_resume(b).unwrap();
    assert!(matches!(
        dispatch(&ha).await,
        PermissionResult::Decision {
            decision: PermissionDecision::ApproveOnce(_),
            ..
        }
    ));
    assert!(matches!(
        dispatch(&hb).await,
        PermissionResult::Decision {
            decision: PermissionDecision::ApproveOnce(_),
            ..
        }
    ));
}
