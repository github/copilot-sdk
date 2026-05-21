//! Permission policy primitives that produce a [`PermissionHandler`].
//!
//! Compose these into a session via the builder methods
//! [`SessionConfig::approve_all_permissions`](crate::types::SessionConfig::approve_all_permissions),
//! [`deny_all_permissions`](crate::types::SessionConfig::deny_all_permissions),
//! and [`approve_permissions_if`](crate::types::SessionConfig::approve_permissions_if).
//! The same primitives are also available as standalone functions that
//! return an `Arc<dyn PermissionHandler>` you can install via
//! [`SessionConfig::with_permission_handler`](crate::types::SessionConfig::with_permission_handler).
//!
//! For a one-shot approve / deny without composition, see
//! [`ApproveAllHandler`](crate::handler::ApproveAllHandler) and
//! [`DenyAllHandler`](crate::handler::DenyAllHandler).

use std::sync::Arc;

use async_trait::async_trait;

use crate::handler::{PermissionHandler, PermissionResult};
use crate::types::{PermissionRequestData, RequestId, SessionId};

/// Return a [`PermissionHandler`] that approves every request.
pub fn approve_all() -> Arc<dyn PermissionHandler> {
    Arc::new(PolicyHandler {
        policy: Policy::ApproveAll,
    })
}

/// Return a [`PermissionHandler`] that denies every request.
pub fn deny_all() -> Arc<dyn PermissionHandler> {
    Arc::new(PolicyHandler {
        policy: Policy::DenyAll,
    })
}

/// Return a [`PermissionHandler`] that consults a predicate for each
/// request. `true` approves, `false` denies.
///
/// ```rust,no_run
/// # use github_copilot_sdk::permission;
/// let handler = permission::approve_if(|data| {
///     data.extra.get("tool").and_then(|v| v.as_str()) != Some("shell")
/// });
/// # let _ = handler;
/// ```
pub fn approve_if<F>(predicate: F) -> Arc<dyn PermissionHandler>
where
    F: Fn(&PermissionRequestData) -> bool + Send + Sync + 'static,
{
    Arc::new(PolicyHandler {
        policy: Policy::Predicate(Arc::new(predicate)),
    })
}

/// Internal policy enum used by both the standalone helpers and the
/// `SessionConfig` policy builders.
///
/// Stored as `pub(crate)` on `SessionConfig::permission_policy` so that
/// the order of `with_permission_handler(...)` and the policy builders
/// does not matter -- the policy is applied at `Client::create_session`
/// time.
#[derive(Clone)]
pub(crate) enum Policy {
    ApproveAll,
    DenyAll,
    Predicate(Arc<dyn Fn(&PermissionRequestData) -> bool + Send + Sync>),
}

impl std::fmt::Debug for Policy {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::ApproveAll => f.write_str("Policy::ApproveAll"),
            Self::DenyAll => f.write_str("Policy::DenyAll"),
            Self::Predicate(_) => f.write_str("Policy::Predicate(<fn>)"),
        }
    }
}

/// Resolve the effective permission handler for a session, given the
/// caller-supplied handler and policy. Called by `Client::create_session`
/// and `Client::resume_session`.
///
/// Semantics:
/// - When `policy` is `Some`, the policy entirely replaces the handler
///   for permission decisions. (Caller-supplied handler, if any, is
///   discarded -- the policy is what answers permission requests.)
/// - When `policy` is `None` and `handler` is `Some`, the handler stands.
/// - When both are `None`, returns `None` (no handler -- the SDK sends
///   `requestPermission: false`).
pub(crate) fn resolve_handler(
    handler: Option<Arc<dyn PermissionHandler>>,
    policy: Option<Policy>,
) -> Option<Arc<dyn PermissionHandler>> {
    match (handler, policy) {
        (_, Some(policy)) => Some(Arc::new(PolicyHandler { policy })),
        (Some(h), None) => Some(h),
        (None, None) => None,
    }
}

struct PolicyHandler {
    policy: Policy,
}

#[async_trait]
impl PermissionHandler for PolicyHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        data: PermissionRequestData,
    ) -> PermissionResult {
        let approved = match &self.policy {
            Policy::ApproveAll => true,
            Policy::DenyAll => false,
            Policy::Predicate(f) => f(&data),
        };
        if approved {
            PermissionResult::Approved
        } else {
            PermissionResult::Denied
        }
    }
}

#[cfg(test)]
mod tests {
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
            h.handle(SessionId::from("s"), RequestId::new("1"), data()).await,
            PermissionResult::Approved
        ));
    }

    #[tokio::test]
    async fn deny_all_denies() {
        let h = deny_all();
        assert!(matches!(
            h.handle(SessionId::from("s"), RequestId::new("1"), data()).await,
            PermissionResult::Denied
        ));
    }

    #[tokio::test]
    async fn approve_if_consults_predicate() {
        let h = approve_if(|d| d.extra.get("tool").and_then(|v| v.as_str()) != Some("shell"));
        assert!(matches!(
            h.handle(SessionId::from("s"), RequestId::new("1"), data()).await,
            PermissionResult::Denied
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
                PermissionResult::Approved
            }
        }
        let resolved =
            resolve_handler(Some(Arc::new(AlwaysApprove)), Some(Policy::DenyAll)).unwrap();
        // Policy wins -- the AlwaysApprove handler is discarded.
        assert!(matches!(
            resolved
                .handle(SessionId::from("s"), RequestId::new("1"), data())
                .await,
            PermissionResult::Denied
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
                PermissionResult::Approved
            }
        }
        let resolved = resolve_handler(Some(Arc::new(H)), None).unwrap();
        assert!(matches!(
            resolved
                .handle(SessionId::from("s"), RequestId::new("1"), data())
                .await,
            PermissionResult::Approved
        ));
    }

    #[test]
    fn resolve_handler_with_neither_returns_none() {
        assert!(resolve_handler(None, None).is_none());
    }
}