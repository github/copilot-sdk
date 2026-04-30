//! Permission-policy helpers that compose with an existing
//! [`SessionHandler`](crate::handler::SessionHandler).
//!
//! These wrap an inner handler and override **only** permission requests,
//! forwarding every other event (tool calls, user input, elicitation,
//! session events) to the inner handler. Use them when you have a custom
//! tool handler — typically a [`ToolHandlerRouter`](crate::tool::ToolHandlerRouter) —
//! but want a one-line policy for permission prompts.
//!
//! For a full handler that approves or denies everything, see
//! [`ApproveAllHandler`](crate::handler::ApproveAllHandler) and
//! [`DenyAllHandler`](crate::handler::DenyAllHandler).
//!
//! # Example
//!
//! ```rust,no_run
//! # use std::sync::Arc;
//! # use github_copilot_sdk::handler::ApproveAllHandler;
//! # use github_copilot_sdk::permission;
//! # use github_copilot_sdk::tool::ToolHandlerRouter;
//! let router = ToolHandlerRouter::new(vec![], Arc::new(ApproveAllHandler));
//! // Inherit the router's tool dispatch but auto-approve all permission prompts:
//! let handler = permission::approve_all(Arc::new(router));
//! ```

use std::sync::Arc;

use async_trait::async_trait;

use crate::handler::{HandlerEvent, HandlerResponse, PermissionResult, SessionHandler};
use crate::types::PermissionRequestData;

/// Wrap `inner` so that every [`HandlerEvent::PermissionRequest`] is
/// auto-approved. All other events are forwarded to `inner`.
pub fn approve_all(inner: Arc<dyn SessionHandler>) -> Arc<dyn SessionHandler> {
    Arc::new(PermissionOverrideHandler {
        inner,
        policy: Policy::ApproveAll,
    })
}

/// Wrap `inner` so that every [`HandlerEvent::PermissionRequest`] is
/// auto-denied. All other events are forwarded to `inner`.
pub fn deny_all(inner: Arc<dyn SessionHandler>) -> Arc<dyn SessionHandler> {
    Arc::new(PermissionOverrideHandler {
        inner,
        policy: Policy::DenyAll,
    })
}

/// Wrap `inner` with a closure-based policy: `predicate` is called for each
/// permission request; `true` approves, `false` denies. All other events
/// are forwarded to `inner`.
///
/// ```rust,no_run
/// # use std::sync::Arc;
/// # use github_copilot_sdk::handler::ApproveAllHandler;
/// # use github_copilot_sdk::permission;
/// let inner = Arc::new(ApproveAllHandler);
/// let handler = permission::approve_if(inner, |data| {
///     // Inspect data.extra (the raw JSON payload) for custom policy.
///     data.extra.get("tool").and_then(|v| v.as_str()) != Some("shell")
/// });
/// # let _ = handler;
/// ```
pub fn approve_if<F>(inner: Arc<dyn SessionHandler>, predicate: F) -> Arc<dyn SessionHandler>
where
    F: Fn(&PermissionRequestData) -> bool + Send + Sync + 'static,
{
    Arc::new(PermissionOverrideHandler {
        inner,
        policy: Policy::Predicate(Arc::new(predicate)),
    })
}

enum Policy {
    ApproveAll,
    DenyAll,
    Predicate(Arc<dyn Fn(&PermissionRequestData) -> bool + Send + Sync>),
}

struct PermissionOverrideHandler {
    inner: Arc<dyn SessionHandler>,
    policy: Policy,
}

#[async_trait]
impl SessionHandler for PermissionOverrideHandler {
    async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
        match event {
            HandlerEvent::PermissionRequest { ref data, .. } => {
                let approved = match &self.policy {
                    Policy::ApproveAll => true,
                    Policy::DenyAll => false,
                    Policy::Predicate(f) => f(data),
                };
                HandlerResponse::Permission(if approved {
                    PermissionResult::Approved
                } else {
                    PermissionResult::Denied
                })
            }
            other => self.inner.on_event(other).await,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::handler::ApproveAllHandler;
    use crate::types::{RequestId, SessionId};

    fn request() -> HandlerEvent {
        HandlerEvent::PermissionRequest {
            session_id: SessionId::from("s1"),
            request_id: RequestId::new("1"),
            data: PermissionRequestData {
                extra: serde_json::json!({"tool": "shell"}),
                ..Default::default()
            },
        }
    }

    #[tokio::test]
    async fn approve_all_approves_permission_requests() {
        let h = approve_all(Arc::new(ApproveAllHandler));
        match h.on_event(request()).await {
            HandlerResponse::Permission(PermissionResult::Approved) => {}
            other => panic!("expected Approved, got {other:?}"),
        }
    }

    #[tokio::test]
    async fn deny_all_denies_permission_requests() {
        let h = deny_all(Arc::new(ApproveAllHandler));
        match h.on_event(request()).await {
            HandlerResponse::Permission(PermissionResult::Denied) => {}
            other => panic!("expected Denied, got {other:?}"),
        }
    }

    #[tokio::test]
    async fn approve_if_consults_predicate() {
        let h = approve_if(Arc::new(ApproveAllHandler), |data| {
            data.extra.get("tool").and_then(|v| v.as_str()) != Some("shell")
        });
        match h.on_event(request()).await {
            HandlerResponse::Permission(PermissionResult::Denied) => {}
            other => panic!("expected Denied for shell, got {other:?}"),
        }
    }

    #[tokio::test]
    async fn non_permission_events_forward_to_inner() {
        let h = deny_all(Arc::new(ApproveAllHandler));
        let event = HandlerEvent::ExitPlanMode {
            session_id: SessionId::from("s1"),
            data: crate::types::ExitPlanModeData::default(),
        };
        match h.on_event(event).await {
            HandlerResponse::ExitPlanMode(_) => {}
            other => panic!("expected ExitPlanMode forwarded, got {other:?}"),
        }
    }
}
