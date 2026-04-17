use std::path::PathBuf;

use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use serde_json::Value;

use crate::types::SessionId;

/// Context provided to every hook invocation.
#[non_exhaustive]
#[derive(Debug, Clone)]
pub struct HookContext {
    pub session_id: SessionId,
}

impl HookContext {
    /// Create a new hook context.
    pub fn new(session_id: SessionId) -> Self {
        Self { session_id }
    }
}

/// Input for the `preToolUse` hook — received before a tool executes.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PreToolUseInput {
    pub timestamp: i64,
    pub cwd: PathBuf,
    pub tool_name: String,
    pub tool_args: Value,
}

/// Output for the `preToolUse` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PreToolUseOutput {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permission_decision: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permission_decision_reason: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub modified_args: Option<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_context: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_output: Option<bool>,
}

/// Input for the `postToolUse` hook — received after a tool executes.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PostToolUseInput {
    pub timestamp: i64,
    pub cwd: PathBuf,
    pub tool_name: String,
    pub tool_args: Value,
    pub tool_result: Value,
}

/// Output for the `postToolUse` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PostToolUseOutput {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub modified_result: Option<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_context: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_output: Option<bool>,
}

/// Input for the `userPromptSubmitted` hook — received when the user sends a message.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserPromptSubmittedInput {
    pub timestamp: i64,
    pub cwd: PathBuf,
    pub prompt: String,
}

/// Output for the `userPromptSubmitted` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UserPromptSubmittedOutput {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub modified_prompt: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_context: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_output: Option<bool>,
}

/// Input for the `sessionStart` hook.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionStartInput {
    pub timestamp: i64,
    pub cwd: PathBuf,
    /// How the session was started: `"startup"`, `"resume"`, or `"new"`.
    pub source: String,
    #[serde(default)]
    pub initial_prompt: Option<String>,
}

/// Output for the `sessionStart` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionStartOutput {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_context: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub modified_config: Option<Value>,
}

/// Input for the `sessionEnd` hook.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEndInput {
    pub timestamp: i64,
    pub cwd: PathBuf,
    /// Why the session ended: `"complete"`, `"error"`, `"abort"`, `"timeout"`, `"user_exit"`.
    pub reason: String,
    #[serde(default)]
    pub final_message: Option<String>,
    #[serde(default)]
    pub error: Option<String>,
}

/// Output for the `sessionEnd` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEndOutput {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_output: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cleanup_actions: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_summary: Option<String>,
}

/// Input for the `errorOccurred` hook.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ErrorOccurredInput {
    pub timestamp: i64,
    pub cwd: PathBuf,
    pub error: String,
    /// Context where the error occurred: `"model_call"`, `"tool_execution"`, `"system"`, `"user_input"`.
    pub error_context: String,
    pub recoverable: bool,
}

/// Output for the `errorOccurred` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ErrorOccurredOutput {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_output: Option<bool>,
    /// How to handle the error: `"retry"`, `"skip"`, or `"abort"`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error_handling: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_count: Option<u32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_notification: Option<String>,
}

/// Events dispatched to [`SessionHooks::on_hook`] at CLI lifecycle points.
///
/// Each variant carries the typed input for that hook plus the shared
/// [`HookContext`]. The handler returns a matching [`HookOutput`] variant
/// (or [`HookOutput::None`] to signal "no hook registered").
#[non_exhaustive]
pub enum HookEvent {
    PreToolUse {
        input: PreToolUseInput,
        ctx: HookContext,
    },
    PostToolUse {
        input: PostToolUseInput,
        ctx: HookContext,
    },
    UserPromptSubmitted {
        input: UserPromptSubmittedInput,
        ctx: HookContext,
    },
    SessionStart {
        input: SessionStartInput,
        ctx: HookContext,
    },
    SessionEnd {
        input: SessionEndInput,
        ctx: HookContext,
    },
    ErrorOccurred {
        input: ErrorOccurredInput,
        ctx: HookContext,
    },
}

/// Response from [`SessionHooks::on_hook`] back to the SDK.
///
/// Return the variant matching the [`HookEvent`] you received, or
/// [`HookOutput::None`] to indicate no hook is registered for that event.
#[non_exhaustive]
pub enum HookOutput {
    /// No hook registered — the SDK returns an empty output object to the CLI.
    None,
    PreToolUse(PreToolUseOutput),
    PostToolUse(PostToolUseOutput),
    UserPromptSubmitted(UserPromptSubmittedOutput),
    SessionStart(SessionStartOutput),
    SessionEnd(SessionEndOutput),
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

/// Single-method callback for session hooks — invoked by the CLI at key
/// lifecycle points (tool use, prompt submission, session start/end, errors).
///
/// Implement this trait to intercept and modify CLI behavior at hook points.
/// The SDK's internal event loop calls [`on_hook`](Self::on_hook) and uses the
/// returned [`HookOutput`] to send the appropriate JSON-RPC reply.
///
/// The default implementation returns [`HookOutput::None`] for all events,
/// meaning "no hook registered." Override `on_hook` and match on the
/// [`HookEvent`] variants you care about.
///
/// The CLI sends `hooks.invoke` JSON-RPC requests when hooks are enabled
/// on the session (via `hooks: true` in [`SessionConfig`](crate::types::SessionConfig)).
#[async_trait]
pub trait SessionHooks: Send + Sync + 'static {
    /// Handle a hook event from the session.
    async fn on_hook(&self, _event: HookEvent) -> HookOutput {
        HookOutput::None
    }
}

/// Dispatches a `hooks.invoke` request to [`SessionHooks::on_hook`].
///
/// Returns `Ok(Value)` shaped like `{ "output": ... }` on success.
/// If no hook is registered ([`HookOutput::None`]), the output is an empty
/// object: `{ "output": {} }`.
pub(crate) async fn dispatch_hook(
    hooks: &dyn SessionHooks,
    session_id: &str,
    hook_type: &str,
    raw_input: Value,
) -> Result<Value, crate::Error> {
    let ctx = HookContext {
        session_id: session_id.into(),
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
                session_id = session_id,
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
                session_id = session_id,
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
        let result = dispatch_hook(&hooks, "sess-1", "preToolUse", input)
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
        let result = dispatch_hook(&hooks, "sess-1", "preToolUse", input)
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
        let result = dispatch_hook(&hooks, "sess-1", "userPromptSubmitted", input)
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
        let result = dispatch_hook(&hooks, "sess-1", "sessionEnd", input)
            .await
            .unwrap();
        assert_eq!(result["output"], serde_json::json!({}));
    }

    #[tokio::test]
    async fn dispatch_unknown_hook_type() {
        let hooks = TestHooks;
        let input = serde_json::json!({});
        let result = dispatch_hook(&hooks, "sess-1", "unknownHook", input)
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
        let result = dispatch_hook(&hooks, "sess-1", "preToolUse", input)
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
        let result = dispatch_hook(&hooks, "sess-1", "postToolUse", input)
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
        let result = dispatch_hook(&hooks, "sess-1", "sessionStart", input)
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
        let result = dispatch_hook(&hooks, "sess-1", "errorOccurred", input)
            .await
            .unwrap();
        assert_eq!(result["output"]["errorHandling"], "retry");
        assert_eq!(result["output"]["retryCount"], 3);
    }
}
