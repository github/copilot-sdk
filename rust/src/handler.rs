use async_trait::async_trait;
use serde_json::Value;

use crate::types::{
    ElicitationRequest, ElicitationResult, RequestId, SessionEvent, SessionId, ToolInvocation,
    ToolResult,
};

/// Events dispatched by the SDK session event loop to the handler.
///
/// The handler returns a [`HandlerResponse`] indicating how the SDK should
/// respond to the CLI. For fire-and-forget events (`SessionEvent`), the
/// response is ignored.
#[non_exhaustive]
pub enum HandlerEvent {
    /// Informational session event from the timeline (e.g. assistant.message_delta,
    /// session.idle, tool.execution_start). Fire-and-forget — return `HandlerResponse::Ok`.
    SessionEvent {
        session_id: SessionId,
        event: Box<SessionEvent>,
    },

    /// The CLI requests permission for an action. Return `HandlerResponse::Permission(..)`.
    PermissionRequest {
        session_id: SessionId,
        request_id: RequestId,
        data: Value,
    },

    /// The CLI requests user input. Return `HandlerResponse::UserInput(..)`.
    /// The handler may block (e.g. awaiting a UI dialog) — this is expected.
    UserInput {
        session_id: SessionId,
        question: String,
        choices: Option<Vec<String>>,
        allow_freeform: Option<bool>,
    },

    /// The CLI requests execution of a client-defined tool.
    /// Return `HandlerResponse::ToolResult(..)`.
    ExternalTool { invocation: ToolInvocation },

    /// The CLI broadcasts an elicitation request for the provider to handle.
    /// Return `HandlerResponse::Elicitation(..)`.
    ElicitationRequest {
        session_id: SessionId,
        request_id: RequestId,
        request: ElicitationRequest,
    },

    /// The CLI requests exiting plan mode. Return `HandlerResponse::ExitPlanMode(..)`.
    ExitPlanMode { session_id: SessionId, data: Value },
}

/// Response from the handler back to the SDK, used to construct the
/// JSON-RPC reply sent to the CLI.
#[non_exhaustive]
pub enum HandlerResponse {
    /// No response needed (used for fire-and-forget `SessionEvent`s).
    Ok,
    /// Permission decision.
    Permission(PermissionResult),
    /// User input response (or `None` to signal no input available).
    UserInput(Option<UserInputResponse>),
    /// Result of a tool execution.
    ToolResult(ToolResult),
    /// Elicitation result (accept/decline/cancel with optional form data).
    Elicitation(ElicitationResult),
    /// Exit plan mode decision.
    ExitPlanMode(ExitPlanModeResult),
}

/// Result of a permission request.
///
/// The protocol supports several denial reasons — use the most specific
/// variant so the CLI can report accurate diagnostics.
///
/// **`NoResult`** is only valid for notification-based permission flows
/// (`permission.requested`). For request-based flows (`permission.request`),
/// returning `NoResult` is a protocol error on v2 servers.
#[non_exhaustive]
#[derive(Debug, Clone)]
pub enum PermissionResult {
    /// The action was approved.
    Approved,
    /// Denied by a static rule (e.g. policy or allowlist).
    DeniedByRules,
    /// Denied interactively by the user.
    DeniedByUser,
    /// No approval rule exists and the user could not be asked.
    DeniedNoApprovalRule,
    /// No decision was made — leave the pending request unanswered.
    ///
    /// Only valid for notification-based (`permission.requested`) flows.
    /// Using this in a request-based (`permission.request`) flow will
    /// result in an error on protocol v2 servers.
    NoResult,
}

/// Response to a user input request.
#[non_exhaustive]
#[derive(Debug, Clone)]
pub struct UserInputResponse {
    pub answer: String,
    pub was_freeform: bool,
}

impl UserInputResponse {
    /// Create a new response.
    pub fn new(answer: impl Into<String>, was_freeform: bool) -> Self {
        Self {
            answer: answer.into(),
            was_freeform,
        }
    }
}

/// Result of an exit-plan-mode request.
#[non_exhaustive]
#[derive(Debug, Clone, Default)]
pub struct ExitPlanModeResult {
    pub approved: bool,
    pub selected_action: Option<String>,
    pub feedback: Option<String>,
}

impl ExitPlanModeResult {
    /// Create a new approved/denied result with no extra fields populated.
    pub fn new(approved: bool) -> Self {
        Self {
            approved,
            ..Default::default()
        }
    }

    /// Set the user's selected action (e.g. `"autopilot"`).
    pub fn with_selected_action(mut self, action: impl Into<String>) -> Self {
        self.selected_action = Some(action.into());
        self
    }

    /// Set free-text feedback to send back to the agent.
    pub fn with_feedback(mut self, feedback: impl Into<String>) -> Self {
        self.feedback = Some(feedback.into());
        self
    }
}

/// Single-method callback for session events — patterned after Tower's `Service::call()`.
///
/// Implement this trait to control how a session responds to CLI events,
/// permission requests, tool calls, and user input prompts.
///
/// The SDK's internal event loop calls [`on_event`](Self::on_event) and uses the
/// returned [`HandlerResponse`] to send the appropriate JSON-RPC reply.
///
/// # Concurrency
///
/// **Request-triggered events** (`UserInput`, `ExternalTool` via `tool.call`,
/// `ExitPlanMode`, `PermissionRequest` via `permission.request`) are awaited
/// inline in the event loop and therefore processed **serially** per session.
/// Blocking here pauses that session's event loop — which is correct, since
/// the CLI is also blocked waiting for the response.
///
/// **Notification-triggered events** (`PermissionRequest` via
/// `permission.requested`, `ExternalTool` via `external_tool.requested`) are
/// dispatched on spawned tasks and may run **concurrently** with each other
/// and with the serial event loop. Implementations must be safe for
/// concurrent invocation.
#[async_trait]
pub trait SessionHandler: Send + Sync + 'static {
    /// Handle an event from the session.
    ///
    /// See the [trait-level docs](SessionHandler#concurrency) for details on
    /// which events may be dispatched concurrently.
    async fn on_event(&self, event: HandlerEvent) -> HandlerResponse;
}

/// A [`SessionHandler`] that auto-approves all permissions and ignores all events.
///
/// Useful for CLI tools, scripts, and tests that don't need interactive
/// permission prompts or custom tool handling.
pub struct ApproveAllHandler;

#[async_trait]
impl SessionHandler for ApproveAllHandler {
    async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
        match event {
            HandlerEvent::PermissionRequest { .. } => {
                HandlerResponse::Permission(PermissionResult::Approved)
            }
            HandlerEvent::ExitPlanMode { .. } => {
                HandlerResponse::ExitPlanMode(ExitPlanModeResult {
                    approved: true,
                    ..Default::default()
                })
            }
            _ => HandlerResponse::Ok,
        }
    }
}
