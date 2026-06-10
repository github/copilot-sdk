//! Optional session-callback traits.
//!
//! Each callback the CLI may dispatch (permission requests, elicitation
//! prompts, user-input questions, exit-plan-mode prompts,
//! auto-mode-switch prompts) has its own focused trait with a single
//! `handle` method.
//!
//! Handlers are **optional**: install only the ones the application cares
//! about. The SDK derives the corresponding wire flag on
//! `session.create` / `session.resume` from the presence of each handler,
//! so the runtime does not emit broadcasts this client would never
//! respond to.
//!
//! Tool dispatch uses its own per-tool registry built from
//! [`Tool::with_handler`](crate::types::Tool::with_handler) on entries passed to
//! [`SessionConfig::with_tools`](crate::types::SessionConfig::with_tools).

use async_trait::async_trait;
use serde::{Deserialize, Serialize};

use crate::generated::api_types::{
    PermissionDecision, PermissionDecisionApproveOnce, PermissionDecisionReject,
    PermissionDecisionUserNotAvailable,
};
use crate::types::{
    ElicitationRequest, ElicitationResult, ExitPlanModeData, PermissionRequestData, RequestId,
    SessionId,
};

/// Decision returned by a [`PermissionHandler`].
///
/// Either a concrete wire-level [`PermissionDecision`] (approve, reject,
/// approve-for-session, approve-permanently, user-not-available, …) or
/// [`PermissionResult::NoResult`], which tells the SDK to suppress its
/// response so another connected client can answer instead.
#[derive(Debug, Clone)]
pub enum PermissionResult {
    /// Send a permission decision on the wire.
    Decision(PermissionDecision),
    /// Decline to respond to this request, allowing another connected
    /// client to answer instead. The SDK suppresses the response.
    NoResult,
}

impl PermissionResult {
    /// Approve this single request.
    pub fn approve_once() -> Self {
        Self::Decision(PermissionDecision::ApproveOnce(
            PermissionDecisionApproveOnce::default(),
        ))
    }

    /// Reject the request, optionally forwarding feedback to the LLM.
    pub fn reject(feedback: impl Into<Option<String>>) -> Self {
        Self::Decision(PermissionDecision::Reject(PermissionDecisionReject {
            feedback: feedback.into(),
            ..Default::default()
        }))
    }

    /// Deny because no user is available to confirm.
    pub fn user_not_available() -> Self {
        Self::Decision(PermissionDecision::UserNotAvailable(
            PermissionDecisionUserNotAvailable::default(),
        ))
    }

    /// Decline to respond, allowing another connected client to answer
    /// instead.
    pub fn no_result() -> Self {
        Self::NoResult
    }
}

impl From<PermissionDecision> for PermissionResult {
    fn from(value: PermissionDecision) -> Self {
        Self::Decision(value)
    }
}

/// Response to a user input request.
#[derive(Debug, Clone)]
pub struct UserInputResponse {
    /// The user's answer text.
    pub answer: String,
    /// Whether the answer was free-form (not a preset choice).
    pub was_freeform: bool,
}

/// Result of an exit-plan-mode request.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ExitPlanModeResult {
    /// Whether the user approved exiting plan mode.
    pub approved: bool,
    /// The action the user selected (if any).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_action: Option<String>,
    /// Optional feedback text from the user.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feedback: Option<String>,
}

impl Default for ExitPlanModeResult {
    fn default() -> Self {
        Self {
            approved: true,
            selected_action: None,
            feedback: None,
        }
    }
}

/// Response to an auto-mode-switch request.
#[non_exhaustive]
#[derive(Debug, Clone, Copy, Eq, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum AutoModeSwitchResponse {
    /// Approve the auto-mode switch for this rate-limit cycle only.
    Yes,
    /// Approve and remember -- auto-accept future auto-mode switches in
    /// this session without prompting.
    YesAlways,
    /// Decline the auto-mode switch. The session stays on the current
    /// model and surfaces the rate-limit error.
    No,
}

/// Handler for `permission.requested` broadcasts.
///
/// Install via
/// [`SessionConfig::with_permission_handler`](crate::types::SessionConfig::with_permission_handler)
/// (or the matching method on [`ResumeSessionConfig`](crate::types::ResumeSessionConfig)).
/// When no permission handler is supplied, the SDK sends
/// `requestPermission: false` on the wire and the runtime short-circuits
/// permission prompts for this client.
#[async_trait]
pub trait PermissionHandler: Send + Sync + 'static {
    /// Resolve a permission request.
    async fn handle(
        &self,
        session_id: SessionId,
        request_id: RequestId,
        data: PermissionRequestData,
    ) -> PermissionResult;
}

/// Handler for `elicitation.requested` broadcasts.
///
/// When unset, `requestElicitation: false` goes on the wire.
#[async_trait]
pub trait ElicitationHandler: Send + Sync + 'static {
    /// Respond to an elicitation prompt (form, URL confirm, etc.).
    async fn handle(
        &self,
        session_id: SessionId,
        request_id: RequestId,
        request: ElicitationRequest,
    ) -> ElicitationResult;
}

/// Handler for `user_input.requested` events from the `ask_user` tool.
///
/// When unset, `requestUserInput: false` goes on the wire and the
/// `ask_user` tool is disabled for the session.
#[async_trait]
pub trait UserInputHandler: Send + Sync + 'static {
    /// Answer a question on behalf of the user. Return `None` to signal
    /// "no answer available".
    async fn handle(
        &self,
        session_id: SessionId,
        question: String,
        header: Option<String>,
        choices: Option<Vec<String>>,
        allow_freeform: Option<bool>,
    ) -> Option<UserInputResponse>;
}

/// Handler for `exit_plan_mode.requested` events. When unset,
/// `requestExitPlanMode: false` goes on the wire.
#[async_trait]
pub trait ExitPlanModeHandler: Send + Sync + 'static {
    /// Decide whether to leave plan mode.
    async fn handle(&self, session_id: SessionId, data: ExitPlanModeData) -> ExitPlanModeResult;
}

/// Handler for `auto_mode_switch.requested` events. When unset,
/// `requestAutoModeSwitch: false` goes on the wire.
#[async_trait]
pub trait AutoModeSwitchHandler: Send + Sync + 'static {
    /// Decide whether to fall back to the auto model after an eligible
    /// rate-limit error. `retry_after_seconds`, when present, is the
    /// number of seconds until the rate limit resets.
    async fn handle(
        &self,
        session_id: SessionId,
        error_code: Option<String>,
        retry_after_seconds: Option<f64>,
    ) -> AutoModeSwitchResponse;
}

/// A [`PermissionHandler`] that approves every request. Useful for CLI
/// tools, scripts, and tests that don't need interactive permission
/// prompts.
#[derive(Debug, Clone)]
pub struct ApproveAllHandler;

#[async_trait]
impl PermissionHandler for ApproveAllHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _data: PermissionRequestData,
    ) -> PermissionResult {
        PermissionResult::approve_once()
    }
}

/// A [`PermissionHandler`] that denies every request.
#[derive(Debug, Clone)]
pub struct DenyAllHandler;

#[async_trait]
impl PermissionHandler for DenyAllHandler {
    async fn handle(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _data: PermissionRequestData,
    ) -> PermissionResult {
        PermissionResult::reject(None)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn approve_all_handler_returns_approved() {
        let result = ApproveAllHandler
            .handle(
                SessionId::from("s1"),
                RequestId::new("1"),
                PermissionRequestData::default(),
            )
            .await;
        assert!(matches!(
            result,
            PermissionResult::Decision(PermissionDecision::ApproveOnce(_))
        ));
    }

    #[tokio::test]
    async fn deny_all_handler_returns_denied() {
        let result = DenyAllHandler
            .handle(
                SessionId::from("s1"),
                RequestId::new("1"),
                PermissionRequestData::default(),
            )
            .await;
        assert!(matches!(
            result,
            PermissionResult::Decision(PermissionDecision::Reject(_))
        ));
    }
}
