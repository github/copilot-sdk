//! Event handler traits for session lifecycle.
//!
//! The [`SessionHandler`](crate::handler::SessionHandler) trait is the primary extension point — implement
//! [`on_event`](crate::handler::SessionHandler::on_event) to control how sessions respond to
//! CLI events, permission requests, tool calls, and user input prompts.

use async_trait::async_trait;

use crate::types::{
    ElicitationRequest, ElicitationResult, ExitPlanModeData, PermissionRequestData, RequestId,
    SessionEvent, SessionId, ToolInvocation, ToolResult,
};

/// Events dispatched by the SDK session event loop to the handler.
///
/// The handler returns a [`HandlerResponse`] indicating how the SDK should
/// respond to the CLI. For fire-and-forget events (`SessionEvent`), the
/// response is ignored.
#[non_exhaustive]
#[derive(Debug)]
pub enum HandlerEvent {
    /// Informational session event from the timeline (e.g. assistant.message_delta,
    /// session.idle, tool.execution_start). Fire-and-forget — return `HandlerResponse::Ok`.
    SessionEvent {
        /// The session that emitted this event.
        session_id: SessionId,
        /// The event payload.
        event: SessionEvent,
    },

    /// The CLI requests permission for an action. Return `HandlerResponse::Permission(..)`.
    PermissionRequest {
        /// The requesting session.
        session_id: SessionId,
        /// Unique ID to correlate the response.
        request_id: RequestId,
        /// Permission request payload.
        data: PermissionRequestData,
    },

    /// The CLI requests user input. Return `HandlerResponse::UserInput(..)`.
    /// The handler may block (e.g. awaiting a UI dialog) — this is expected.
    UserInput {
        /// The requesting session.
        session_id: SessionId,
        /// The question text to present.
        question: String,
        /// Optional multiple-choice options.
        choices: Option<Vec<String>>,
        /// Whether free-form text input is allowed.
        allow_freeform: Option<bool>,
    },

    /// The CLI requests execution of a client-defined tool.
    /// Return `HandlerResponse::ToolResult(..)`.
    ExternalTool {
        /// The tool call to execute.
        invocation: ToolInvocation,
    },

    /// The CLI broadcasts an elicitation request for the provider to handle.
    /// Return `HandlerResponse::Elicitation(..)`.
    ElicitationRequest {
        /// The requesting session.
        session_id: SessionId,
        /// Unique ID to correlate the response.
        request_id: RequestId,
        /// The elicitation request payload.
        request: ElicitationRequest,
    },

    /// The CLI requests exiting plan mode. Return `HandlerResponse::ExitPlanMode(..)`.
    ExitPlanMode {
        /// The requesting session.
        session_id: SessionId,
        /// Plan mode exit payload.
        data: ExitPlanModeData,
    },
}

/// Response from the handler back to the SDK, used to construct the
/// JSON-RPC reply sent to the CLI.
#[non_exhaustive]
#[derive(Debug)]
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
#[derive(Debug, Clone)]
pub enum PermissionResult {
    /// Permission granted.
    Approved,
    /// Permission denied.
    Denied,
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
#[derive(Debug, Clone)]
pub struct ExitPlanModeResult {
    /// Whether the user approved exiting plan mode.
    pub approved: bool,
    /// The action the user selected (if any).
    pub selected_action: Option<String>,
    /// Optional feedback text from the user.
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

/// Callback trait for session events.
///
/// Implement this trait to control how a session responds to CLI events,
/// permission requests, tool calls, user input prompts, elicitations, and
/// plan-mode exits. There are two styles of implementation — pick whichever
/// fits your use case:
///
/// 1. **Per-event methods (recommended for most handlers).** Override the
///    specific `on_*` methods you care about; every method has a safe
///    default so you only write what you need. This is the pattern used by
///    [`serenity::EventHandler`][serenity], `lapin`, and most Rust SDKs
///    that dispatch broker/client callbacks.
/// 2. **Single [`on_event`](Self::on_event) method.** Override this one
///    method and `match` on [`HandlerEvent`] yourself. Useful for logging
///    middleware, custom routing, or when you want an exhaustiveness check
///    across all variants.
///
/// When you override [`on_event`](Self::on_event) directly, the per-event methods are not
/// called — your implementation is entirely responsible for dispatch. The
/// default [`on_event`](Self::on_event) fans out to the per-event methods.
///
/// [serenity]: https://docs.rs/serenity/latest/serenity/client/trait.EventHandler.html
///
/// # Default behavior
///
/// - Permission requests → **denied** (safe default).
/// - User input → `None` (no answer available).
/// - External tool calls → failure result with "no handler registered".
/// - Elicitation → `"cancel"`.
/// - Exit plan mode → [`ExitPlanModeResult::default`].
/// - Session events → ignored (fire-and-forget).
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
///
/// # Example
///
/// ```no_run
/// use async_trait::async_trait;
/// use copilot::handler::{PermissionResult, SessionHandler};
/// use copilot::types::{PermissionRequestData, RequestId, SessionId};
///
/// struct ApproveReadsOnly;
///
/// #[async_trait]
/// impl SessionHandler for ApproveReadsOnly {
///     async fn on_permission_request(
///         &self,
///         _sid: SessionId,
///         _rid: RequestId,
///         data: PermissionRequestData,
///     ) -> PermissionResult {
///         match data.extra.get("tool").and_then(|v| v.as_str()) {
///             Some("view") | Some("ls") | Some("grep") => PermissionResult::Approved,
///             _ => PermissionResult::Denied,
///         }
///     }
/// }
/// ```
#[async_trait]
pub trait SessionHandler: Send + Sync + 'static {
    /// Handle an event from the session.
    ///
    /// The default implementation destructures `event` and calls the
    /// matching per-event method (e.g. [`on_permission_request`](Self::on_permission_request)
    /// for [`HandlerEvent::PermissionRequest`]). Override this method only
    /// if you want a single dispatch point with exhaustive matching — most
    /// handlers should override the per-event methods instead.
    ///
    /// See the [trait-level docs](SessionHandler#concurrency) for details on
    /// which events may be dispatched concurrently.
    async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
        match event {
            HandlerEvent::SessionEvent { session_id, event } => {
                self.on_session_event(session_id, event).await;
                HandlerResponse::Ok
            }
            HandlerEvent::PermissionRequest {
                session_id,
                request_id,
                data,
            } => HandlerResponse::Permission(
                self.on_permission_request(session_id, request_id, data)
                    .await,
            ),
            HandlerEvent::UserInput {
                session_id,
                question,
                choices,
                allow_freeform,
            } => HandlerResponse::UserInput(
                self.on_user_input(session_id, question, choices, allow_freeform)
                    .await,
            ),
            HandlerEvent::ExternalTool { invocation } => {
                HandlerResponse::ToolResult(self.on_external_tool(invocation).await)
            }
            HandlerEvent::ElicitationRequest {
                session_id,
                request_id,
                request,
            } => HandlerResponse::Elicitation(
                self.on_elicitation(session_id, request_id, request).await,
            ),
            HandlerEvent::ExitPlanMode { session_id, data } => {
                HandlerResponse::ExitPlanMode(self.on_exit_plan_mode(session_id, data).await)
            }
        }
    }

    /// Informational timeline event (assistant messages, tool execution
    /// markers, session idle, etc.). Fire-and-forget — the return value is
    /// ignored.
    ///
    /// Default: do nothing.
    async fn on_session_event(&self, _session_id: SessionId, _event: SessionEvent) {}

    /// The CLI is asking whether the agent may perform a privileged action.
    ///
    /// Default: [`PermissionResult::Denied`]. The default-deny posture
    /// matches the CLI's safety model; override to implement your own
    /// policy (see the [`permission`](crate::permission) module for common
    /// wrappers like `approve_all` / `approve_if`).
    async fn on_permission_request(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _data: PermissionRequestData,
    ) -> PermissionResult {
        PermissionResult::Denied
    }

    /// The CLI is asking the user a question (optionally with a list of
    /// choices).
    ///
    /// Default: `None` — the CLI interprets this as "no answer available"
    /// and falls back to its own prompt behavior.
    async fn on_user_input(
        &self,
        _session_id: SessionId,
        _question: String,
        _choices: Option<Vec<String>>,
        _allow_freeform: Option<bool>,
    ) -> Option<UserInputResponse> {
        None
    }

    /// The CLI wants to invoke a client-defined ("external") tool.
    ///
    /// Default: a failure [`ToolResult`] indicating no tool handler is
    /// registered. Typical implementations route to a
    /// [`ToolHandlerRouter`](crate::tool::ToolHandlerRouter) which
    /// dispatches to tools registered via
    /// [`define_tool`](crate::tool::define_tool) or custom
    /// [`ToolHandler`](crate::tool::ToolHandler) impls.
    async fn on_external_tool(&self, invocation: ToolInvocation) -> ToolResult {
        let msg = format!("No handler registered for tool '{}'", invocation.tool_name);
        ToolResult::Expanded(crate::types::ToolResultExpanded {
            text_result_for_llm: msg.clone(),
            result_type: "failure".to_string(),
            session_log: None,
            error: Some(msg),
        })
    }

    /// The CLI is requesting an elicitation (structured form / URL prompt).
    ///
    /// Default: cancel.
    async fn on_elicitation(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _request: ElicitationRequest,
    ) -> ElicitationResult {
        ElicitationResult {
            action: "cancel".to_string(),
            content: None,
        }
    }

    /// The CLI is asking the user whether to exit plan mode.
    ///
    /// Default: [`ExitPlanModeResult::default`] (approved with no action).
    async fn on_exit_plan_mode(
        &self,
        _session_id: SessionId,
        _data: ExitPlanModeData,
    ) -> ExitPlanModeResult {
        ExitPlanModeResult::default()
    }
}

/// A [`SessionHandler`] that auto-approves all permissions and ignores all events.
///
/// Useful for CLI tools, scripts, and tests that don't need interactive
/// permission prompts or custom tool handling.
#[derive(Debug, Clone)]
pub struct ApproveAllHandler;

#[async_trait]
impl SessionHandler for ApproveAllHandler {
    async fn on_permission_request(
        &self,
        _session_id: SessionId,
        _request_id: RequestId,
        _data: PermissionRequestData,
    ) -> PermissionResult {
        PermissionResult::Approved
    }
}

/// A [`SessionHandler`] that denies all permission requests and otherwise
/// relies on the trait's default fallback responses for every other event
/// (e.g. tool invocations return "unhandled", elicitations cancel, plan-mode
/// prompts decline). This is the safe default used when no handler is set on
/// [`SessionConfig::handler`](crate::types::SessionConfig::handler) — sessions
/// will not stall on permission prompts (they're denied immediately) but no
/// privileged actions will be taken without an explicit opt-in.
#[derive(Debug, Clone)]
pub struct DenyAllHandler;

#[async_trait]
impl SessionHandler for DenyAllHandler {
    // All defaults are already safe: permissions deny, everything else is a
    // sensible fallback. We just reuse them here for clarity.
}

#[cfg(test)]
mod tests {
    use serde_json::Value;

    use super::*;
    use crate::types::{PermissionRequestData, RequestId, SessionId};

    fn perm_data() -> PermissionRequestData {
        PermissionRequestData { extra: Value::Null }
    }

    // A handler that overrides only `on_permission_request` (per-method style).
    struct ApproveViaPerMethod;

    #[async_trait]
    impl SessionHandler for ApproveViaPerMethod {
        async fn on_permission_request(
            &self,
            _: SessionId,
            _: RequestId,
            _: PermissionRequestData,
        ) -> PermissionResult {
            PermissionResult::Approved
        }
    }

    // A handler that overrides `on_event` directly (legacy / routing style).
    struct ApproveViaOnEvent;

    #[async_trait]
    impl SessionHandler for ApproveViaOnEvent {
        async fn on_event(&self, event: HandlerEvent) -> HandlerResponse {
            match event {
                HandlerEvent::PermissionRequest { .. } => {
                    HandlerResponse::Permission(PermissionResult::Approved)
                }
                _ => HandlerResponse::Ok,
            }
        }
    }

    #[tokio::test]
    async fn per_method_override_dispatches_via_default_on_event() {
        let h = ApproveViaPerMethod;
        let resp = h
            .on_event(HandlerEvent::PermissionRequest {
                session_id: SessionId::from("s1".to_string()),
                request_id: RequestId::new("r1"),
                data: perm_data(),
            })
            .await;
        assert!(matches!(
            resp,
            HandlerResponse::Permission(PermissionResult::Approved)
        ));
    }

    #[tokio::test]
    async fn on_event_override_short_circuits_per_method_defaults() {
        let h = ApproveViaOnEvent;
        let resp = h
            .on_event(HandlerEvent::PermissionRequest {
                session_id: SessionId::from("s1".to_string()),
                request_id: RequestId::new("r1"),
                data: perm_data(),
            })
            .await;
        assert!(matches!(
            resp,
            HandlerResponse::Permission(PermissionResult::Approved)
        ));
    }

    #[tokio::test]
    async fn deny_all_handler_uses_default_permission_deny() {
        let h = DenyAllHandler;
        let resp = h
            .on_event(HandlerEvent::PermissionRequest {
                session_id: SessionId::from("s1".to_string()),
                request_id: RequestId::new("r1"),
                data: perm_data(),
            })
            .await;
        assert!(matches!(
            resp,
            HandlerResponse::Permission(PermissionResult::Denied)
        ));
    }

    #[tokio::test]
    async fn default_on_external_tool_returns_failure() {
        let h = DenyAllHandler;
        let resp = h
            .on_event(HandlerEvent::ExternalTool {
                invocation: crate::types::ToolInvocation {
                    session_id: SessionId::from("s1".to_string()),
                    tool_call_id: "tc1".to_string(),
                    tool_name: "missing".to_string(),
                    arguments: Value::Null,
                },
            })
            .await;
        match resp {
            HandlerResponse::ToolResult(crate::types::ToolResult::Expanded(exp)) => {
                assert_eq!(exp.result_type, "failure");
                assert!(exp.text_result_for_llm.contains("missing"));
                assert_eq!(exp.error.as_deref(), Some(exp.text_result_for_llm.as_str()));
            }
            other => panic!("unexpected response: {other:?}"),
        }
    }

    #[tokio::test]
    async fn default_on_elicitation_returns_cancel() {
        let h = DenyAllHandler;
        let resp = h
            .on_event(HandlerEvent::ElicitationRequest {
                session_id: SessionId::from("s1".to_string()),
                request_id: RequestId::new("r1"),
                request: crate::types::ElicitationRequest {
                    message: "test".to_string(),
                    requested_schema: None,
                    mode: Some(crate::types::ElicitationMode::Form),
                    elicitation_source: None,
                    url: None,
                },
            })
            .await;
        match resp {
            HandlerResponse::Elicitation(r) => assert_eq!(r.action, "cancel"),
            other => panic!("unexpected response: {other:?}"),
        }
    }
}
