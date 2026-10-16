//! Permission policy primitives that produce a [`PermissionHandler`](crate::handler::PermissionHandler).
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

use crate::handler::{PermissionHandler, PermissionResult, permission_handler_failure};
use crate::types::{PermissionRequestData, RequestId, SessionId};

/// Return a [`PermissionHandler`] that approves requests when managed settings
/// are disabled.
///
/// When managed settings are enabled, the handler logs an error and returns a
/// user-not-available decision.
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
        (handler, None) => handler,
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
            if matches!(self.policy, Policy::ApproveAll) && data.managed_settings_enabled {
                permission_handler_failure(
                    "approve-all policy cannot be used when managed settings are enabled",
                )
            } else if data.managed_approval_required == Some(true) {
                PermissionResult::no_result()
            } else {
                PermissionResult::approve_once()
            }
        } else {
            PermissionResult::reject(None)
        }
    }
}

#[cfg(test)]
mod tests;
