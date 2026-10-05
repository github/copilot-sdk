//! Lifecycle hook callbacks invoked at key session points.
//!
//! Hooks let you intercept and modify CLI behavior — approve or deny tool
//! use, rewrite user prompts, inject context at session start, and handle
//! errors. Implement [`SessionHooks`](crate::hooks::SessionHooks) and pass it to
//! [`Client::create_session`](crate::Client::create_session).

use std::path::PathBuf;
use std::time::Instant;

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
    /// The runtime session ID of the session that triggered the hook.
    pub session_id: String,
    /// Unix timestamp in ms (the runtime serializes this as a JSON float).
    pub timestamp: f64,
    /// Working directory.
    #[serde(rename = "cwd")]
    pub working_directory: PathBuf,
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

/// Input for the `preMcpToolCall` hook — received before an MCP tool call is dispatched.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PreMcpToolCallInput {
    /// The runtime session ID of the session that triggered the hook.
    pub session_id: String,
    /// Unix timestamp in ms (the runtime serializes this as a JSON float).
    pub timestamp: f64,
    /// Working directory.
    #[serde(rename = "cwd")]
    pub working_directory: PathBuf,
    /// Name of the MCP server being called.
    pub server_name: String,
    /// Name of the MCP tool being called.
    pub tool_name: String,
    /// Arguments for the MCP tool call.
    pub arguments: Value,
    /// Tool call ID, if available.
    #[serde(default)]
    pub tool_call_id: Option<String>,
    /// MCP request metadata.
    #[serde(default, rename = "_meta")]
    pub meta: Option<Value>,
}

/// Output for the `preMcpToolCall` hook.
///
/// `meta_to_use` has tri-state semantics:
/// - `None`: field is absent in JSON, meaning preserve existing `_meta`
/// - `Some(Value::Null)`: serialized as JSON `null`, meaning omit `_meta`
/// - `Some(Value::Object(...))`: serialized as JSON object, meaning replace `_meta`
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PreMcpToolCallOutput {
    /// Hook-controlled metadata for the outgoing MCP request.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub meta_to_use: Option<Value>,
}

/// Input for the `postToolUse` hook — received after a tool executes.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PostToolUseInput {
    /// The runtime session ID of the session that triggered the hook.
    pub session_id: String,
    /// Unix timestamp in ms (the runtime serializes this as a JSON float).
    pub timestamp: f64,
    /// Working directory.
    #[serde(rename = "cwd")]
    pub working_directory: PathBuf,
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

/// Input for the `postToolUseFailure` hook — received after a tool execution
/// whose result was `"failure"`.
///
/// `postToolUse` only fires for successful tool executions. Register a handler
/// for `postToolUseFailure` to observe failed tool calls. The CLI extracts the
/// failure message from the tool result and passes it as the `error` field
/// (rather than passing the full result object).
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PostToolUseFailureInput {
    /// The runtime session ID of the session that triggered the hook.
    pub session_id: String,
    /// Unix timestamp in ms (the runtime serializes this as a JSON float).
    pub timestamp: f64,
    /// Working directory.
    #[serde(rename = "cwd")]
    pub working_directory: PathBuf,
    /// Name of the tool that failed.
    pub tool_name: String,
    /// Arguments that were passed to the tool.
    pub tool_args: Value,
    /// Failure message extracted from the tool's result.
    pub error: String,
}

/// Output for the `postToolUseFailure` hook.
///
/// Only `additional_context` is consumed by the host CLI — it is appended as
/// hidden guidance to the model alongside the failed tool result.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PostToolUseFailureOutput {
    /// Extra context appended to the failed tool result for the agent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_context: Option<String>,
}

/// Input for the `userPromptSubmitted` hook — received when the user sends a message.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserPromptSubmittedInput {
    /// The runtime session ID of the session that triggered the hook.
    pub session_id: String,
    /// Unix timestamp in ms (the runtime serializes this as a JSON float).
    pub timestamp: f64,
    /// Working directory.
    #[serde(rename = "cwd")]
    pub working_directory: PathBuf,
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

/// Input for the `userPromptTransformed` hook.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserPromptTransformedInput {
    /// The runtime session ID of the session that triggered the hook.
    pub session_id: String,
    /// Unix timestamp in ms.
    pub timestamp: f64,
    /// Working directory.
    #[serde(rename = "cwd")]
    pub working_directory: PathBuf,
    /// The prompt after any `userPromptSubmitted` hooks have run.
    pub prompt: String,
    /// The model-facing prompt after runtime transformations.
    pub transformed_prompt: String,
}

/// Output for the `userPromptTransformed` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UserPromptTransformedOutput {
    /// Replacement model-facing prompt to persist and send to the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub modified_transformed_prompt: Option<String>,
}

/// Input for the `sessionStart` hook.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionStartInput {
    /// The runtime session ID of the session that triggered the hook.
    pub session_id: String,
    /// Unix timestamp in ms (the runtime serializes this as a JSON float).
    pub timestamp: f64,
    /// Working directory.
    #[serde(rename = "cwd")]
    pub working_directory: PathBuf,
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
    /// The runtime session ID of the session that triggered the hook.
    pub session_id: String,
    /// Unix timestamp in ms (the runtime serializes this as a JSON float).
    pub timestamp: f64,
    /// Working directory.
    #[serde(rename = "cwd")]
    pub working_directory: PathBuf,
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
    /// The runtime session ID of the session that triggered the hook.
    pub session_id: String,
    /// Unix timestamp in ms (the runtime serializes this as a JSON float).
    pub timestamp: f64,
    /// Working directory.
    #[serde(rename = "cwd")]
    pub working_directory: PathBuf,
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

/// Input for the `agentStop` hook, received when the top-level agent reaches a natural stop.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentStopInput {
    /// The runtime session ID of the session that triggered the hook.
    pub session_id: String,
    /// Unix timestamp in ms (the runtime serializes this as a JSON float).
    pub timestamp: f64,
    /// Working directory.
    #[serde(rename = "cwd")]
    pub working_directory: PathBuf,
    /// Reason the agent stopped.
    #[serde(default)]
    pub stop_reason: Option<String>,
    /// Path to the on-disk session transcript.
    #[serde(default)]
    pub transcript_path: Option<PathBuf>,
    /// Whether this stop follows a previous block decision from the hook.
    #[serde(default, rename = "stop_hook_active")]
    pub stop_hook_active: Option<bool>,
}

/// Output for the `agentStop` hook.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentStopOutput {
    /// Set to `"block"` to keep the agent running.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub decision: Option<String>,
    /// Follow-up instruction supplied when the stop is blocked.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
}

/// Input for `subagentStart`, received before a subagent's first turn.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentStartInput {
    /// The parent session ID.
    pub session_id: String,
    /// Unix timestamp in ms.
    pub timestamp: f64,
    /// Working directory of the parent session.
    #[serde(rename = "cwd")]
    pub working_directory: PathBuf,
    /// Path to the parent session transcript.
    pub transcript_path: PathBuf,
    /// Name of the subagent definition.
    pub agent_name: String,
    /// Display name, when the definition provides one.
    #[serde(default)]
    pub agent_display_name: Option<String>,
    /// Description, when the definition provides one.
    #[serde(default)]
    pub agent_description: Option<String>,
}

/// Output for `subagentStart`.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentStartOutput {
    /// Context prepended to the subagent's first prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_context: Option<String>,
}

/// Input for `subagentStop`, received after a subagent's turn.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentStopInput {
    /// The parent session ID.
    pub session_id: String,
    /// Unix timestamp in ms.
    pub timestamp: f64,
    /// Working directory of the parent session.
    #[serde(rename = "cwd")]
    pub working_directory: PathBuf,
    /// Path to the parent session transcript.
    pub transcript_path: PathBuf,
    /// Name of the subagent definition.
    pub agent_name: String,
    /// Type of the subagent.
    pub agent_type: String,
    /// Agent ID, when available.
    #[serde(default)]
    pub agent_id: Option<String>,
    /// Display name, when the definition provides one.
    #[serde(default)]
    pub agent_display_name: Option<String>,
    /// Description, when the definition provides one.
    #[serde(default)]
    pub agent_description: Option<String>,
    /// Reason the subagent stopped (normally `"end_turn"`).
    pub stop_reason: String,
    /// The subagent's last assistant response.
    pub response: String,
}

/// Output for `subagentStop`.
#[derive(Debug, Clone, Default, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentStopOutput {
    /// Set to `"block"` to run another subagent turn; only `"allow"` and `"block"` are valid.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub decision: Option<String>,
    /// Nonempty follow-up instruction required when blocking; without a block
    /// decision this is an error, not an instruction to the child.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Replacement final response when the stop is allowed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub modified_response: Option<String>,
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
    /// Fired before an MCP tool call is dispatched.
    PreMcpToolCall {
        /// Typed input data.
        input: PreMcpToolCallInput,
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
    /// Fired after a tool execution whose result was `"failure"`.
    /// [`HookEvent::PostToolUse`] only fires on success, so observe this
    /// variant to react to failed tool calls.
    PostToolUseFailure {
        /// Typed input data.
        input: PostToolUseFailureInput,
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
    /// Fired after the runtime transforms a submitted prompt.
    UserPromptTransformed {
        /// Typed input data.
        input: UserPromptTransformedInput,
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
    /// Fired when the top-level agent reaches a natural stop.
    AgentStop {
        /// Typed input data.
        input: AgentStopInput,
        /// Session context.
        ctx: HookContext,
    },
    /// Fired before a subagent's first turn.
    SubagentStart {
        /// Typed input data.
        input: SubagentStartInput,
        /// Session context.
        ctx: HookContext,
    },
    /// Fired after a subagent's turn.
    SubagentStop {
        /// Typed input data.
        input: SubagentStopInput,
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
    /// Response for a pre-MCP-tool-call hook.
    PreMcpToolCall(PreMcpToolCallOutput),
    /// Response for a post-tool-use hook.
    PostToolUse(PostToolUseOutput),
    /// Response for a post-tool-use-failure hook.
    PostToolUseFailure(PostToolUseFailureOutput),
    /// Response for a user-prompt-submitted hook.
    UserPromptSubmitted(UserPromptSubmittedOutput),
    /// Response for a user-prompt-transformed hook.
    UserPromptTransformed(UserPromptTransformedOutput),
    /// Response for a session-start hook.
    SessionStart(SessionStartOutput),
    /// Response for a session-end hook.
    SessionEnd(SessionEndOutput),
    /// Response for an error-occurred hook.
    ErrorOccurred(ErrorOccurredOutput),
    /// Response for an agent-stop hook.
    AgentStop(AgentStopOutput),
    /// Response for a subagent-start hook.
    SubagentStart(SubagentStartOutput),
    /// Response for a subagent-stop hook.
    SubagentStop(SubagentStopOutput),
}

impl HookOutput {
    fn variant_name(&self) -> &'static str {
        match self {
            Self::None => "None",
            Self::PreToolUse(_) => "PreToolUse",
            Self::PreMcpToolCall(_) => "PreMcpToolCall",
            Self::PostToolUse(_) => "PostToolUse",
            Self::PostToolUseFailure(_) => "PostToolUseFailure",
            Self::UserPromptSubmitted(_) => "UserPromptSubmitted",
            Self::UserPromptTransformed(_) => "UserPromptTransformed",
            Self::SessionStart(_) => "SessionStart",
            Self::SessionEnd(_) => "SessionEnd",
            Self::ErrorOccurred(_) => "ErrorOccurred",
            Self::AgentStop(_) => "AgentStop",
            Self::SubagentStart(_) => "SubagentStart",
            Self::SubagentStop(_) => "SubagentStop",
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
            HookEvent::PreMcpToolCall { input, ctx } => self
                .on_pre_mcp_tool_call(input, ctx)
                .await
                .map(HookOutput::PreMcpToolCall)
                .unwrap_or(HookOutput::None),
            HookEvent::PostToolUse { input, ctx } => self
                .on_post_tool_use(input, ctx)
                .await
                .map(HookOutput::PostToolUse)
                .unwrap_or(HookOutput::None),
            HookEvent::PostToolUseFailure { input, ctx } => self
                .on_post_tool_use_failure(input, ctx)
                .await
                .map(HookOutput::PostToolUseFailure)
                .unwrap_or(HookOutput::None),
            HookEvent::UserPromptSubmitted { input, ctx } => self
                .on_user_prompt_submitted(input, ctx)
                .await
                .map(HookOutput::UserPromptSubmitted)
                .unwrap_or(HookOutput::None),
            HookEvent::UserPromptTransformed { input, ctx } => self
                .on_user_prompt_transformed(input, ctx)
                .await
                .map(HookOutput::UserPromptTransformed)
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
            HookEvent::AgentStop { input, ctx } => self
                .on_agent_stop(input, ctx)
                .await
                .map(HookOutput::AgentStop)
                .unwrap_or(HookOutput::None),
            HookEvent::SubagentStart { input, ctx } => self
                .on_subagent_start(input, ctx)
                .await
                .map(HookOutput::SubagentStart)
                .unwrap_or(HookOutput::None),
            HookEvent::SubagentStop { input, ctx } => self
                .on_subagent_stop(input, ctx)
                .await
                .map(HookOutput::SubagentStop)
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

    /// Called before an MCP tool call is dispatched. Return `Some(output)` to
    /// modify or remove request metadata, or `None` (default) to pass through unchanged.
    async fn on_pre_mcp_tool_call(
        &self,
        _input: PreMcpToolCallInput,
        _ctx: HookContext,
    ) -> Option<PreMcpToolCallOutput> {
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

    /// Called after a tool execution whose result was `"failure"`. The
    /// success-only [`on_post_tool_use`](Self::on_post_tool_use) hook does
    /// not fire for these outcomes, so override this method to observe or
    /// inject extra context after failed tool calls.
    async fn on_post_tool_use_failure(
        &self,
        _input: PostToolUseFailureInput,
        _ctx: HookContext,
    ) -> Option<PostToolUseFailureOutput> {
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

    /// Called after the runtime transforms a submitted prompt. Return
    /// `Some(output)` to replace the model-facing content before it is stored.
    async fn on_user_prompt_transformed(
        &self,
        _input: UserPromptTransformedInput,
        _ctx: HookContext,
    ) -> Option<UserPromptTransformedOutput> {
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

    /// Called when the top-level agent reaches a natural stop. Return a block
    /// decision to keep the agent running with a follow-up instruction.
    async fn on_agent_stop(
        &self,
        _input: AgentStopInput,
        _ctx: HookContext,
    ) -> Option<AgentStopOutput> {
        None
    }

    /// Called before a subagent runs. Return context to prepend to its prompt.
    async fn on_subagent_start(
        &self,
        _input: SubagentStartInput,
        _ctx: HookContext,
    ) -> Option<SubagentStartOutput> {
        None
    }

    /// Called when a subagent stops. Return a block decision with a reason
    /// to continue it, or a replacement for its final response.
    async fn on_subagent_stop(
        &self,
        _input: SubagentStopInput,
        _ctx: HookContext,
    ) -> Option<SubagentStopOutput> {
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
        "preMcpToolCall" => {
            let input: PreMcpToolCallInput = serde_json::from_value(raw_input)?;
            HookEvent::PreMcpToolCall { input, ctx }
        }
        "postToolUse" => {
            let input: PostToolUseInput = serde_json::from_value(raw_input)?;
            HookEvent::PostToolUse { input, ctx }
        }
        "postToolUseFailure" => {
            let input: PostToolUseFailureInput = serde_json::from_value(raw_input)?;
            HookEvent::PostToolUseFailure { input, ctx }
        }
        "userPromptSubmitted" => {
            let input: UserPromptSubmittedInput = serde_json::from_value(raw_input)?;
            HookEvent::UserPromptSubmitted { input, ctx }
        }
        "userPromptTransformed" => {
            let input: UserPromptTransformedInput = serde_json::from_value(raw_input)?;
            HookEvent::UserPromptTransformed { input, ctx }
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
        "agentStop" => {
            let input: AgentStopInput = serde_json::from_value(raw_input)?;
            HookEvent::AgentStop { input, ctx }
        }
        "subagentStart" => {
            let input: SubagentStartInput = serde_json::from_value(raw_input)?;
            HookEvent::SubagentStart { input, ctx }
        }
        "subagentStop" => {
            let input: SubagentStopInput = serde_json::from_value(raw_input)?;
            HookEvent::SubagentStop { input, ctx }
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

    let dispatch_start = Instant::now();
    let output = hooks.on_hook(event).await;
    tracing::debug!(
        elapsed_ms = dispatch_start.elapsed().as_millis(),
        session_id = %session_id,
        hook_type = hook_type,
        "SessionHooks::on_hook dispatch"
    );

    // Validate that the output variant matches the dispatched hook type.
    // A mismatched return (e.g. HookOutput::SessionEnd for a preToolUse
    // event) is treated as "no hook registered" to avoid sending the CLI
    // a semantically wrong response.
    let output_value = match (hook_type, &output) {
        (_, HookOutput::None) => None,
        ("preToolUse", HookOutput::PreToolUse(o)) => Some(serde_json::to_value(o)?),
        ("preMcpToolCall", HookOutput::PreMcpToolCall(o)) => Some(serde_json::to_value(o)?),
        ("postToolUse", HookOutput::PostToolUse(o)) => Some(serde_json::to_value(o)?),
        ("postToolUseFailure", HookOutput::PostToolUseFailure(o)) => Some(serde_json::to_value(o)?),
        ("userPromptSubmitted", HookOutput::UserPromptSubmitted(o)) => {
            Some(serde_json::to_value(o)?)
        }
        ("userPromptTransformed", HookOutput::UserPromptTransformed(o)) => {
            Some(serde_json::to_value(o)?)
        }
        ("sessionStart", HookOutput::SessionStart(o)) => Some(serde_json::to_value(o)?),
        ("sessionEnd", HookOutput::SessionEnd(o)) => Some(serde_json::to_value(o)?),
        ("errorOccurred", HookOutput::ErrorOccurred(o)) => Some(serde_json::to_value(o)?),
        ("agentStop", HookOutput::AgentStop(o)) => Some(serde_json::to_value(o)?),
        ("subagentStart", HookOutput::SubagentStart(o)) => Some(serde_json::to_value(o)?),
        ("subagentStop", HookOutput::SubagentStop(o)) => Some(serde_json::to_value(o)?),
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
mod tests;
