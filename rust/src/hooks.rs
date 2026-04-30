//! Lifecycle hook callbacks invoked at key session points.
//!
//! Hooks let you intercept and modify CLI behavior — approve or deny tool
//! use, rewrite user prompts, inject context at session start, and handle
//! errors. Implement [`SessionHooks`](crate::hooks::SessionHooks) and pass it to
//! [`Client::create_session`](crate::Client::create_session).

use std::path::PathBuf;

use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use serde_json::Value;

use crate::types::SessionId;

/// Context provided to every hook invocation.
#[derive(Debug, Clone)]
pub struct HookContext {
    /// The session this hook was triggered in.
    pub session_id: SessionId,
}

/// Input for the `preToolUse` hook — received before a tool executes.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PreToolUseInput {
    /// Unix timestamp (ms).
    pub timestamp: i64,
    /// Working directory.
    pub cwd: PathBuf,
    /// Name of the tool about to execute.
    pub tool_name: String,
    /// Arguments passed to the tool.
    pub tool_args: Value,
}

/// Output for the `preToolUse` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PreToolUseOutput {
    /// "allow" or "deny".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permission_decision: Option<String>,
    /// Reason for the decision (shown to the agent).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permission_decision_reason: Option<String>,
    /// Replacement arguments for the tool.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub modified_args: Option<Value>,
    /// Extra context injected into the agent's prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_context: Option<String>,
    /// Suppress the hook's output from the session log.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_output: Option<bool>,
}

/// Input for the `postToolUse` hook — received after a tool executes.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PostToolUseInput {
    /// Unix timestamp (ms).
    pub timestamp: i64,
    /// Working directory.
    pub cwd: PathBuf,
    /// Name of the tool that executed.
    pub tool_name: String,
    /// Arguments that were passed to the tool.
    pub tool_args: Value,
    /// Result returned by the tool.
    pub tool_result: Value,
}

/// Output for the `postToolUse` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PostToolUseOutput {
    /// Replacement result for the tool.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub modified_result: Option<Value>,
    /// Extra context injected into the agent's prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_context: Option<String>,
    /// Suppress the hook's output from the session log.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_output: Option<bool>,
}

/// Input for the `userPromptSubmitted` hook — received when the user sends a message.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserPromptSubmittedInput {
    /// Unix timestamp (ms).
    pub timestamp: i64,
    /// Working directory.
    pub cwd: PathBuf,
    /// The user's message text.
    pub prompt: String,
}

/// Output for the `userPromptSubmitted` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UserPromptSubmittedOutput {
    /// Replacement prompt text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub modified_prompt: Option<String>,
    /// Extra context injected into the agent's prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_context: Option<String>,
    /// Suppress the hook's output from the session log.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_output: Option<bool>,
}

/// Input for the `sessionStart` hook.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionStartInput {
    /// Unix timestamp (ms).
    pub timestamp: i64,
    /// Working directory.
    pub cwd: PathBuf,
    /// How the session was started: `"startup"`, `"resume"`, or `"new"`.
    pub source: String,
    /// The first user message, if any.
    #[serde(default)]
    pub initial_prompt: Option<String>,
}

/// Output for the `sessionStart` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionStartOutput {
    /// Extra context injected at session start.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_context: Option<String>,
    /// Config overrides applied to the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub modified_config: Option<Value>,
}

/// Input for the `sessionEnd` hook.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEndInput {
    /// Unix timestamp (ms).
    pub timestamp: i64,
    /// Working directory.
    pub cwd: PathBuf,
    /// Why the session ended: `"complete"`, `"error"`, `"abort"`, `"timeout"`, `"user_exit"`.
    pub reason: String,
    /// The last assistant message.
    #[serde(default)]
    pub final_message: Option<String>,
    /// Error message, if the session ended due to an error.
    #[serde(default)]
    pub error: Option<String>,
}

/// Output for the `sessionEnd` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEndOutput {
    /// Suppress the hook's output from the session log.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_output: Option<bool>,
    /// Actions to run during cleanup.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cleanup_actions: Option<Vec<String>>,
    /// Summary text for the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_summary: Option<String>,
}

/// Input for the `errorOccurred` hook.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ErrorOccurredInput {
    /// Unix timestamp (ms).
    pub timestamp: i64,
    /// Working directory.
    pub cwd: PathBuf,
    /// The error message.
    pub error: String,
    /// Context where the error occurred: `"model_call"`, `"tool_execution"`, `"system"`, `"user_input"`.
    pub error_context: String,
    /// Whether the error is recoverable.
    pub recoverable: bool,
}

/// Output for the `errorOccurred` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ErrorOccurredOutput {
    /// Suppress the hook's output from the session log.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_output: Option<bool>,
    /// How to handle the error: `"retry"`, `"skip"`, or `"abort"`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error_handling: Option<String>,
    /// Number of retries to attempt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_count: Option<u32>,
    /// Message to show the user.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_notification: Option<String>,
}

/// Events dispatched to [`SessionHooks::on_hook`] at CLI lifecycle points.
///
/// Each variant carries the typed input for that hook plus the shared
/// [`HookContext`]. The handler returns a matching [`HookOutput`] variant
/// (or [`HookOutput::None`] to signal "no hook registered").
#[non_exhaustive]
#[derive(Debug)]
pub enum HookEvent {
    /// Fired before a tool executes.
    PreToolUse {
        /// Typed input data.
        input: PreToolUseInput,
        /// Session context.
        ctx: HookContext,
    },
    /// Fired after a tool executes.
    PostToolUse {
        /// Typed input data.
        input: PostToolUseInput,
        /// Session context.
        ctx: HookContext,
    },
    /// Fired when the user sends a message.
    UserPromptSubmitted {
        /// Typed input data.
        input: UserPromptSubmittedInput,
        /// Session context.
        ctx: HookContext,
    },
    /// Fired at session creation or resume.
    SessionStart {
        /// Typed input data.
        input: SessionStartInput,
        /// Session context.
        ctx: HookContext,
    },
    /// Fired when the session ends.
    SessionEnd {
        /// Typed input data.
        input: SessionEndInput,
        /// Session context.
        ctx: HookContext,
    },
    /// Fired when an error occurs.
    ErrorOccurred {
        /// Typed input data.
        input: ErrorOccurredInput,
        /// Session context.
        ctx: HookContext,
    },
}

/// Response from [`SessionHooks::on_hook`] back to the SDK.
///
/// Return the variant matching the [`HookEvent`] you received, or
/// [`HookOutput::None`] to indicate no hook is registered for that event.
#[non_exhaustive]
#[derive(Debug)]
pub enum HookOutput {
    /// No hook registered — the SDK returns an empty output object to the CLI.
    None,
    /// Response for a pre-tool-use hook.
    PreToolUse(PreToolUseOutput),
    /// Response for a post-tool-use hook.
    PostToolUse(PostToolUseOutput),
    /// Response for a user-prompt-submitted hook.
    UserPromptSubmitted(UserPromptSubmittedOutput),
    /// Response for a session-start hook.
    SessionStart(SessionStartOutput),
    /// Response for a session-end hook.
    SessionEnd(SessionEndOutput),
    /// Response for an error-occurred hook.
    ErrorOccurred(ErrorOccurredOutput),
}

impl HookOutput {
    fn variant_name(&self) -> &'static str {
        match self {
            Self::None => "None",
            Self::PreToolUse(_) => "PreToolUse",
            Self::PostToolUse(_) => "PostToolUse",
            Self::UserPromptSubmitted(_) => "UserPromptSubmitted",
            Self::SessionStart(_) => "SessionStart",
            Self::SessionEnd(_) => "SessionEnd",
            Self::ErrorOccurred(_) => "ErrorOccurred",
        }
    }
}

/// Callback trait for session hooks — invoked by the CLI at key lifecycle
/// points (tool use, prompt submission, session start/end, errors).
///
/// Implement this trait to intercept and modify CLI behavior at hook points.
/// There are two styles of implementation — pick whichever fits:
///
/// 1. **Per-hook methods (recommended).** Override the specific `on_*` hook
///    methods you care about; every hook has a default that returns `None`
///    (meaning "no hook registered, use CLI default behavior").
/// 2. **Single [`on_hook`](Self::on_hook) method.** Override this one and
///    `match` on [`HookEvent`] yourself — useful for logging middleware or
///    shared dispatch logic.
///
/// Hooks only fire when hooks are enabled on the session (via
/// [`SessionConfig::hooks = Some(true)`](crate::types::SessionConfig::hooks),
/// which [`SessionConfig::with_hooks`](crate::types::SessionConfig::with_hooks)
/// sets automatically).
#[async_trait]
pub trait SessionHooks: Send + Sync + 'static {
    /// Top-level dispatch. The default implementation fans out to the
    /// per-hook methods below; override this only if you want a single
    /// matching point across all hook types.
    async fn on_hook(&self, event: HookEvent) -> HookOutput {
        match event {
            HookEvent::PreToolUse { input, ctx } => self
                .on_pre_tool_use(input, ctx)
                .await
                .map(HookOutput::PreToolUse)
                .unwrap_or(HookOutput::None),
            HookEvent::PostToolUse { input, ctx } => self
                .on_post_tool_use(input, ctx)
                .await
                .map(HookOutput::PostToolUse)
                .unwrap_or(HookOutput::None),
            HookEvent::UserPromptSubmitted { input, ctx } => self
                .on_user_prompt_submitted(input, ctx)
                .await
                .map(HookOutput::UserPromptSubmitted)
                .unwrap_or(HookOutput::None),
            HookEvent::SessionStart { input, ctx } => self
                .on_session_start(input, ctx)
                .await
                .map(HookOutput::SessionStart)
                .unwrap_or(HookOutput::None),
            HookEvent::SessionEnd { input, ctx } => self
                .on_session_end(input, ctx)
                .await
                .map(HookOutput::SessionEnd)
                .unwrap_or(HookOutput::None),
            HookEvent::ErrorOccurred { input, ctx } => self
                .on_error_occurred(input, ctx)
                .await
                .map(HookOutput::ErrorOccurred)
                .unwrap_or(HookOutput::None),
        }
    }

    /// Called before a tool executes. Return `Some(output)` to approve/deny
    /// or modify the call, or `None` (default) to pass through unchanged.
    async fn on_pre_tool_use(
        &self,
        _input: PreToolUseInput,
        _ctx: HookContext,
    ) -> Option<PreToolUseOutput> {
        None
    }

    /// Called after a tool executes. Return `Some(output)` to inject
    /// additional context or signal post-processing decisions; `None`
    /// (default) means no follow-up.
    async fn on_post_tool_use(
        &self,
        _input: PostToolUseInput,
        _ctx: HookContext,
    ) -> Option<PostToolUseOutput> {
        None
    }

    /// Called when the user submits a prompt. Return `Some(output)` to
    /// rewrite the prompt or inject extra context; `None` (default) passes
    /// through unchanged.
    async fn on_user_prompt_submitted(
        &self,
        _input: UserPromptSubmittedInput,
        _ctx: HookContext,
    ) -> Option<UserPromptSubmittedOutput> {
        None
    }

    /// Called at session creation or resume. Return `Some(output)` to
    /// inject startup context.
    async fn on_session_start(
        &self,
        _input: SessionStartInput,
        _ctx: HookContext,
    ) -> Option<SessionStartOutput> {
        None
    }

    /// Called when the session ends. Return `Some(output)` if your hook
    /// needs to signal cleanup behavior.
    async fn on_session_end(
        &self,
        _input: SessionEndInput,
        _ctx: HookContext,
    ) -> Option<SessionEndOutput> {
        None
    }

    /// Called when the CLI reports an error. Return `Some(output)` to
    /// influence retry behavior or surface a user-facing notification.
    async fn on_error_occurred(
        &self,
        _input: ErrorOccurredInput,
        _ctx: HookContext,
    ) -> Option<ErrorOccurredOutput> {
        None
    }
}

/// Dispatches a `hooks.invoke` request to [`SessionHooks::on_hook`].
///
/// Returns `Ok(Value)` shaped like `{ "output": ... }` on success.
/// If no hook is registered ([`HookOutput::None`]), the output is an empty
/// object: `{ "output": {} }`.
pub(crate) async fn dispatch_hook(
    hooks: &dyn SessionHooks,
    session_id: &SessionId,
    hook_type: &str,
    raw_input: Value,
) -> Result<Value, crate::Error> {
    let ctx = HookContext {
        session_id: session_id.clone(),
    };

    let event = match hook_type {
        "preToolUse" => {
            let input: PreToolUseInput = serde_json::from_value(raw_input)?;
            HookEvent::PreToolUse { input, ctx }
        }
        "postToolUse" => {
            let input: PostToolUseInput = serde_json::from_value(raw_input)?;
            HookEvent::PostToolUse { input, ctx }
        }
        "userPromptSubmitted" => {
            let input: UserPromptSubmittedInput = serde_json::from_value(raw_input)?;
            HookEvent::UserPromptSubmitted { input, ctx }
        }
        "sessionStart" => {
            let input: SessionStartInput = serde_json::from_value(raw_input)?;
            HookEvent::SessionStart { input, ctx }
        }
        "sessionEnd" => {
            let input: SessionEndInput = serde_json::from_value(raw_input)?;
            HookEvent::SessionEnd { input, ctx }
        }
        "errorOccurred" => {
            let input: ErrorOccurredInput = serde_json::from_value(raw_input)?;
            HookEvent::ErrorOccurred { input, ctx }
        }
        _ => {
            tracing::warn!(
                hook_type = hook_type,
                session_id = %session_id,
                "unknown hook type"
            );
            return Ok(serde_json::json!({ "output": {} }));
        }
    };

    let output = hooks.on_hook(event).await;

    // Validate that the output variant matches the dispatched hook type.
    // A mismatched return (e.g. HookOutput::SessionEnd for a preToolUse
    // event) is treated as "no hook registered" to avoid sending the CLI
    // a semantically wrong response.
    let output_value = match (hook_type, &output) {
        (_, HookOutput::None) => None,
        ("preToolUse", HookOutput::PreToolUse(o)) => Some(serde_json::to_value(o)?),
        ("postToolUse", HookOutput::PostToolUse(o)) => Some(serde_json::to_value(o)?),
        ("userPromptSubmitted", HookOutput::UserPromptSubmitted(o)) => {
            Some(serde_json::to_value(o)?)
        }
        ("sessionStart", HookOutput::SessionStart(o)) => Some(serde_json::to_value(o)?),
        ("sessionEnd", HookOutput::SessionEnd(o)) => Some(serde_json::to_value(o)?),
        ("errorOccurred", HookOutput::ErrorOccurred(o)) => Some(serde_json::to_value(o)?),
        _ => {
            tracing::warn!(
                hook_type = hook_type,
                session_id = %session_id,
                output_variant = output.variant_name(),
                "hook returned mismatched output variant, treating as unregistered"
            );
            None
        }
    };

    Ok(serde_json::json!({ "output": output_value.unwrap_or(Value::Object(Default::default())) }))
}

#[cfg(test)]
mod tests {
    use super::*;

    struct TestHooks;

    #[async_trait]
    impl SessionHooks for TestHooks {
        async fn on_hook(&self, event: HookEvent) -> HookOutput {
            match event {
                HookEvent::PreToolUse { input, .. } => {
                    if input.tool_name == "dangerous_tool" {
                        HookOutput::PreToolUse(PreToolUseOutput {
                            permission_decision: Some("deny".to_string()),
                            permission_decision_reason: Some("blocked by policy".to_string()),
                            ..Default::default()
                        })
                    } else {
                        HookOutput::None
                    }
                }
                HookEvent::UserPromptSubmitted { input, .. } => {
                    HookOutput::UserPromptSubmitted(UserPromptSubmittedOutput {
                        modified_prompt: Some(format!("[prefixed] {}", input.prompt)),
                        ..Default::default()
                    })
                }
                _ => HookOutput::None,
            }
        }
    }

    #[tokio::test]
    async fn dispatch_pre_tool_use_deny() {
        let hooks = TestHooks;
        let input = serde_json::json!({
            "timestamp": 1234567890,
            "cwd": "/tmp",
            "toolName": "dangerous_tool",
            "toolArgs": {}
        });
        let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "preToolUse", input)
            .await
            .unwrap();
        let output = &result["output"];
        assert_eq!(output["permissionDecision"], "deny");
        assert_eq!(output["permissionDecisionReason"], "blocked by policy");
    }

    #[tokio::test]
    async fn dispatch_pre_tool_use_passthrough() {
        let hooks = TestHooks;
        let input = serde_json::json!({
            "timestamp": 1234567890,
            "cwd": "/tmp",
            "toolName": "safe_tool",
            "toolArgs": {"key": "value"}
        });
        let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "preToolUse", input)
            .await
            .unwrap();
        // No hook registered for this tool — output should be empty object
        assert_eq!(result["output"], serde_json::json!({}));
    }

    #[tokio::test]
    async fn dispatch_user_prompt_submitted() {
        let hooks = TestHooks;
        let input = serde_json::json!({
            "timestamp": 1234567890,
            "cwd": "/tmp",
            "prompt": "hello world"
        });
        let result = dispatch_hook(
            &hooks,
            &SessionId::new("sess-1"),
            "userPromptSubmitted",
            input,
        )
        .await
        .unwrap();
        assert_eq!(result["output"]["modifiedPrompt"], "[prefixed] hello world");
    }

    #[tokio::test]
    async fn dispatch_unregistered_hook_returns_empty() {
        let hooks = TestHooks;
        let input = serde_json::json!({
            "timestamp": 1234567890,
            "cwd": "/tmp",
            "reason": "complete"
        });
        // TestHooks doesn't handle SessionEnd
        let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "sessionEnd", input)
            .await
            .unwrap();
        assert_eq!(result["output"], serde_json::json!({}));
    }

    #[tokio::test]
    async fn dispatch_unknown_hook_type() {
        let hooks = TestHooks;
        let input = serde_json::json!({});
        let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "unknownHook", input)
            .await
            .unwrap();
        assert_eq!(result["output"], serde_json::json!({}));
    }

    #[tokio::test]
    async fn dispatch_mismatched_output_returns_empty() {
        struct MismatchHooks;
        #[async_trait]
        impl SessionHooks for MismatchHooks {
            async fn on_hook(&self, _event: HookEvent) -> HookOutput {
                // Always return SessionEnd output regardless of event type
                HookOutput::SessionEnd(SessionEndOutput {
                    session_summary: Some("oops".to_string()),
                    ..Default::default()
                })
            }
        }

        let hooks = MismatchHooks;
        let input = serde_json::json!({
            "timestamp": 1234567890,
            "cwd": "/tmp",
            "toolName": "some_tool",
            "toolArgs": {}
        });
        // preToolUse event gets a SessionEnd output — should be treated as empty
        let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "preToolUse", input)
            .await
            .unwrap();
        assert_eq!(result["output"], serde_json::json!({}));
    }

    #[tokio::test]
    async fn dispatch_post_tool_use_default() {
        let hooks = TestHooks;
        let input = serde_json::json!({
            "timestamp": 1234567890,
            "cwd": "/tmp",
            "toolName": "some_tool",
            "toolArgs": {},
            "toolResult": "success"
        });
        let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "postToolUse", input)
            .await
            .unwrap();
        assert_eq!(result["output"], serde_json::json!({}));
    }

    #[tokio::test]
    async fn dispatch_session_start() {
        struct StartHooks;
        #[async_trait]
        impl SessionHooks for StartHooks {
            async fn on_hook(&self, event: HookEvent) -> HookOutput {
                match event {
                    HookEvent::SessionStart { .. } => {
                        HookOutput::SessionStart(SessionStartOutput {
                            additional_context: Some("extra context".to_string()),
                            ..Default::default()
                        })
                    }
                    _ => HookOutput::None,
                }
            }
        }

        let hooks = StartHooks;
        let input = serde_json::json!({
            "timestamp": 1234567890,
            "cwd": "/tmp",
            "source": "new"
        });
        let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "sessionStart", input)
            .await
            .unwrap();
        assert_eq!(result["output"]["additionalContext"], "extra context");
    }

    #[tokio::test]
    async fn dispatch_error_occurred() {
        struct ErrorHooks;
        #[async_trait]
        impl SessionHooks for ErrorHooks {
            async fn on_hook(&self, event: HookEvent) -> HookOutput {
                match event {
                    HookEvent::ErrorOccurred { .. } => {
                        HookOutput::ErrorOccurred(ErrorOccurredOutput {
                            error_handling: Some("retry".to_string()),
                            retry_count: Some(3),
                            ..Default::default()
                        })
                    }
                    _ => HookOutput::None,
                }
            }
        }

        let hooks = ErrorHooks;
        let input = serde_json::json!({
            "timestamp": 1234567890,
            "cwd": "/tmp",
            "error": "model timeout",
            "errorContext": "model_call",
            "recoverable": true
        });
        let result = dispatch_hook(&hooks, &SessionId::new("sess-1"), "errorOccurred", input)
            .await
            .unwrap();
        assert_eq!(result["output"]["errorHandling"], "retry");
        assert_eq!(result["output"]["retryCount"], 3);
    }
}
