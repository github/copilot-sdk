/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

fn data() -> PermissionRequestData {
    PermissionRequestData {
        extra: serde_json::json!({ "tool": "shell" }),
        ..Default::default()
    }
}

#[tokio::test]
async fn approve_all_approves() {
    let h = approve_all();
    assert!(matches!(
        h.handle(SessionId::from("s"), RequestId::new("1"), data())
            .await,
        PermissionResult::Decision {
            decision: crate::types::PermissionDecision::ApproveOnce(_),
            ..
        }
    ));
}

#[tokio::test]
async fn approve_all_fails_when_managed_settings_enabled() {
    let h = approve_all();
    let mut request = data();
    request.managed_settings_enabled = true;
    assert!(matches!(
        h.handle(SessionId::from("s"), RequestId::new("1"), request)
            .await,
        PermissionResult::Decision {
            decision: crate::types::PermissionDecision::UserNotAvailable(_),
            ..
        }
    ));
}

#[tokio::test]
async fn deny_all_denies() {
    let h = deny_all();
    assert!(matches!(
        h.handle(SessionId::from("s"), RequestId::new("1"), data())
            .await,
        PermissionResult::Decision {
            decision: crate::types::PermissionDecision::Reject(_),
            ..
        }
    ));
}

#[tokio::test]
async fn approve_if_consults_predicate() {
    let h = approve_if(|d| d.extra.get("tool").and_then(|v| v.as_str()) != Some("shell"));
    assert!(matches!(
        h.handle(SessionId::from("s"), RequestId::new("1"), data())
            .await,
        PermissionResult::Decision {
            decision: crate::types::PermissionDecision::Reject(_),
            ..
        }
    ));
}

#[tokio::test]
async fn approve_if_leaves_managed_approval_pending_when_predicate_approves() {
    let h = approve_if(|_| true);
    let mut request = data();
    request.managed_approval_required = Some(true);
    assert!(matches!(
        h.handle(SessionId::from("s"), RequestId::new("1"), request)
            .await,
        PermissionResult::NoResult
    ));
}

#[tokio::test]
async fn approve_if_still_rejects_managed_request_when_predicate_denies() {
    let h = approve_if(|_| false);
    let mut request = data();
    request.managed_approval_required = Some(true);
    assert!(matches!(
        h.handle(SessionId::from("s"), RequestId::new("1"), request)
            .await,
        PermissionResult::Decision {
            decision: crate::types::PermissionDecision::Reject(_),
            ..
        }
    ));
}

#[tokio::test]
async fn resolve_handler_policy_wins() {
    struct AlwaysApprove;
    #[async_trait]
    impl PermissionHandler for AlwaysApprove {
        async fn handle(
            &self,
            _: SessionId,
            _: RequestId,
            _: PermissionRequestData,
        ) -> PermissionResult {
            PermissionResult::approve_once()
        }
    }
    let resolved = resolve_handler(Some(Arc::new(AlwaysApprove)), Some(Policy::DenyAll)).unwrap();
    // Policy wins -- the AlwaysApprove handler is discarded.
    assert!(matches!(
        resolved
            .handle(SessionId::from("s"), RequestId::new("1"), data())
            .await,
        PermissionResult::Decision {
            decision: crate::types::PermissionDecision::Reject(_),
            ..
        }
    ));
}

#[tokio::test]
async fn resolve_handler_with_only_handler() {
    struct H;
    #[async_trait]
    impl PermissionHandler for H {
        async fn handle(
            &self,
            _: SessionId,
            _: RequestId,
            _: PermissionRequestData,
        ) -> PermissionResult {
            PermissionResult::approve_once()
        }
    }
    let resolved = resolve_handler(Some(Arc::new(H)), None).unwrap();
    assert!(matches!(
        resolved
            .handle(SessionId::from("s"), RequestId::new("1"), data())
            .await,
        PermissionResult::Decision {
            decision: crate::types::PermissionDecision::ApproveOnce(_),
            ..
        }
    ));
}

#[test]
fn resolve_handler_with_neither_returns_none() {
    assert!(resolve_handler(None, None).is_none());
}
