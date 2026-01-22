// AUTO-GENERATED FILE - DO NOT EDIT
//
// Generated from: @github/copilot/session-events.schema.json
// Generated for: Rust SDK
//
// To update these types, regenerate from the schema.

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// Session event from the Copilot CLI.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEvent {
    /// Event data.
    pub data: EventData,
    /// Whether this event is ephemeral.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ephemeral: Option<bool>,
    /// Event ID.
    pub id: String,
    /// Parent event ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_id: Option<String>,
    /// Event timestamp.
    pub timestamp: DateTime<Utc>,
    /// Event type.
    #[serde(rename = "type")]
    pub r#type: SessionEventType,
}

/// Event data containing all possible fields.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EventData {
    /// Context information.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<ContextUnion>,
    /// Copilot version.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_version: Option<String>,
    /// Producer.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub producer: Option<String>,
    /// Selected model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_model: Option<String>,
    /// Session ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<String>,
    /// Start time.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub start_time: Option<DateTime<Utc>>,
    /// Version.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<f64>,
    /// Event count.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub event_count: Option<f64>,
    /// Resume time.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resume_time: Option<DateTime<Utc>>,
    /// Error type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error_type: Option<String>,
    /// Message content.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Stack trace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stack: Option<String>,
    /// Info type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub info_type: Option<String>,
    /// New model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_model: Option<String>,
    /// Previous model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_model: Option<String>,
    /// Handoff time.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub handoff_time: Option<DateTime<Utc>>,
    /// Remote session ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_session_id: Option<String>,
    /// Repository information.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<Repository>,
    /// Source type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_type: Option<SourceType>,
    /// Summary.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    /// Messages removed during truncation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub messages_removed_during_truncation: Option<f64>,
    /// Who performed the truncation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub performed_by: Option<String>,
    /// Post truncation messages length.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub post_truncation_messages_length: Option<f64>,
    /// Post truncation tokens in messages.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub post_truncation_tokens_in_messages: Option<f64>,
    /// Pre truncation messages length.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pre_truncation_messages_length: Option<f64>,
    /// Pre truncation tokens in messages.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pre_truncation_tokens_in_messages: Option<f64>,
    /// Token limit.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token_limit: Option<f64>,
    /// Tokens removed during truncation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tokens_removed_during_truncation: Option<f64>,
    /// Current tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub current_tokens: Option<f64>,
    /// Messages length.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub messages_length: Option<f64>,
    /// Compaction tokens used.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub compaction_tokens_used: Option<CompactionTokensUsed>,
    /// Error information.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<ErrorUnion>,
    /// Messages removed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub messages_removed: Option<f64>,
    /// Post compaction tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub post_compaction_tokens: Option<f64>,
    /// Pre compaction messages length.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pre_compaction_messages_length: Option<f64>,
    /// Pre compaction tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pre_compaction_tokens: Option<f64>,
    /// Success flag.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub success: Option<bool>,
    /// Summary content.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary_content: Option<String>,
    /// Tokens removed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tokens_removed: Option<f64>,
    /// Attachments.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attachments: Option<Vec<EventAttachment>>,
    /// Content.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<String>,
    /// Source.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<String>,
    /// Transformed content.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub transformed_content: Option<String>,
    /// Turn ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub turn_id: Option<String>,
    /// Intent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub intent: Option<String>,
    /// Reasoning ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_id: Option<String>,
    /// Delta content for streaming.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub delta_content: Option<String>,
    /// Message ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message_id: Option<String>,
    /// Parent tool call ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_tool_call_id: Option<String>,
    /// Tool requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_requests: Option<Vec<ToolRequest>>,
    /// Total response size in bytes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_response_size_bytes: Option<f64>,
    /// API call ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_call_id: Option<String>,
    /// Cache read tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_read_tokens: Option<f64>,
    /// Cache write tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_write_tokens: Option<f64>,
    /// Cost.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cost: Option<f64>,
    /// Duration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub duration: Option<f64>,
    /// Initiator.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initiator: Option<String>,
    /// Input tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_tokens: Option<f64>,
    /// Model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Output tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_tokens: Option<f64>,
    /// Provider call ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_call_id: Option<String>,
    /// Quota snapshots.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub quota_snapshots: Option<HashMap<String, QuotaSnapshot>>,
    /// Reason.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Arguments.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<serde_json::Value>,
    /// Tool call ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
    /// Tool name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_name: Option<String>,
    /// Partial output.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub partial_output: Option<String>,
    /// Progress message.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub progress_message: Option<String>,
    /// Is user requested.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_user_requested: Option<bool>,
    /// Result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<ToolResultData>,
    /// Tool telemetry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_telemetry: Option<HashMap<String, serde_json::Value>>,
    /// Agent description.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_description: Option<String>,
    /// Agent display name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_display_name: Option<String>,
    /// Agent name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_name: Option<String>,
    /// Tools.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
    /// Hook invocation ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub hook_invocation_id: Option<String>,
    /// Hook type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub hook_type: Option<String>,
    /// Input.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
    /// Output.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output: Option<serde_json::Value>,
    /// Metadata.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<Metadata>,
    /// Name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Role.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub role: Option<Role>,
}

/// Attachment in event data.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EventAttachment {
    /// Display name.
    pub display_name: String,
    /// Path.
    pub path: String,
    /// Type.
    #[serde(rename = "type")]
    pub r#type: AttachmentType,
}

/// Attachment type.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum AttachmentType {
    /// Directory attachment.
    Directory,
    /// File attachment.
    File,
}

/// Compaction tokens used.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CompactionTokensUsed {
    /// Cached input tokens.
    pub cached_input: f64,
    /// Input tokens.
    pub input: f64,
    /// Output tokens.
    pub output: f64,
}

/// Context information as a class.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ContextClass {
    /// Branch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Current working directory.
    pub cwd: String,
    /// Git root.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Repository.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
}

/// Context can be either a string or a structured object.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum ContextUnion {
    /// Structured context.
    Class(ContextClass),
    /// String context.
    String(String),
}

/// Error information as a class.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ErrorClass {
    /// Error code.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code: Option<String>,
    /// Error message.
    pub message: String,
    /// Stack trace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stack: Option<String>,
}

/// Error can be either a string or a structured object.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum ErrorUnion {
    /// Structured error.
    Class(ErrorClass),
    /// String error.
    String(String),
}

/// Metadata.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Metadata {
    /// Prompt version.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prompt_version: Option<String>,
    /// Variables.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub variables: Option<HashMap<String, serde_json::Value>>,
}

/// Quota snapshot.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QuotaSnapshot {
    /// Entitlement requests.
    pub entitlement_requests: f64,
    /// Is unlimited entitlement.
    pub is_unlimited_entitlement: bool,
    /// Overage.
    pub overage: f64,
    /// Overage allowed with exhausted quota.
    pub overage_allowed_with_exhausted_quota: bool,
    /// Remaining percentage.
    pub remaining_percentage: f64,
    /// Reset date.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reset_date: Option<DateTime<Utc>>,
    /// Usage allowed with exhausted quota.
    pub usage_allowed_with_exhausted_quota: bool,
    /// Used requests.
    pub used_requests: f64,
}

/// Repository information.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Repository {
    /// Branch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Name.
    pub name: String,
    /// Owner.
    pub owner: String,
}

/// Tool result data.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolResultData {
    /// Content.
    pub content: String,
}

/// Tool request.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolRequest {
    /// Arguments.
    pub arguments: serde_json::Value,
    /// Name.
    pub name: String,
    /// Tool call ID.
    pub tool_call_id: String,
    /// Type.
    #[serde(rename = "type", skip_serializing_if = "Option::is_none")]
    pub r#type: Option<ToolRequestType>,
}

/// Tool request type.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum ToolRequestType {
    /// Custom tool.
    Custom,
    /// Function tool.
    Function,
}

/// Role.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum Role {
    /// Developer role.
    Developer,
    /// System role.
    System,
}

/// Source type.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum SourceType {
    /// Local source.
    Local,
    /// Remote source.
    Remote,
}

/// Session event type.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionEventType {
    /// Abort event.
    #[serde(rename = "abort")]
    Abort,
    /// Assistant intent event.
    #[serde(rename = "assistant.intent")]
    AssistantIntent,
    /// Assistant message event.
    #[serde(rename = "assistant.message")]
    AssistantMessage,
    /// Assistant message delta event (streaming).
    #[serde(rename = "assistant.message_delta")]
    AssistantMessageDelta,
    /// Assistant reasoning event.
    #[serde(rename = "assistant.reasoning")]
    AssistantReasoning,
    /// Assistant reasoning delta event (streaming).
    #[serde(rename = "assistant.reasoning_delta")]
    AssistantReasoningDelta,
    /// Assistant turn end event.
    #[serde(rename = "assistant.turn_end")]
    AssistantTurnEnd,
    /// Assistant turn start event.
    #[serde(rename = "assistant.turn_start")]
    AssistantTurnStart,
    /// Assistant usage event.
    #[serde(rename = "assistant.usage")]
    AssistantUsage,
    /// Hook end event.
    #[serde(rename = "hook.end")]
    HookEnd,
    /// Hook start event.
    #[serde(rename = "hook.start")]
    HookStart,
    /// Pending messages modified event.
    #[serde(rename = "pending_messages.modified")]
    PendingMessagesModified,
    /// Session compaction complete event.
    #[serde(rename = "session.compaction_complete")]
    SessionCompactionComplete,
    /// Session compaction start event.
    #[serde(rename = "session.compaction_start")]
    SessionCompactionStart,
    /// Session error event.
    #[serde(rename = "session.error")]
    SessionError,
    /// Session handoff event.
    #[serde(rename = "session.handoff")]
    SessionHandoff,
    /// Session idle event.
    #[serde(rename = "session.idle")]
    SessionIdle,
    /// Session info event.
    #[serde(rename = "session.info")]
    SessionInfo,
    /// Session model change event.
    #[serde(rename = "session.model_change")]
    SessionModelChange,
    /// Session resume event.
    #[serde(rename = "session.resume")]
    SessionResume,
    /// Session start event.
    #[serde(rename = "session.start")]
    SessionStart,
    /// Session truncation event.
    #[serde(rename = "session.truncation")]
    SessionTruncation,
    /// Session usage info event.
    #[serde(rename = "session.usage_info")]
    SessionUsageInfo,
    /// Subagent completed event.
    #[serde(rename = "subagent.completed")]
    SubagentCompleted,
    /// Subagent failed event.
    #[serde(rename = "subagent.failed")]
    SubagentFailed,
    /// Subagent selected event.
    #[serde(rename = "subagent.selected")]
    SubagentSelected,
    /// Subagent started event.
    #[serde(rename = "subagent.started")]
    SubagentStarted,
    /// System message event.
    #[serde(rename = "system.message")]
    SystemMessage,
    /// Tool execution complete event.
    #[serde(rename = "tool.execution_complete")]
    ToolExecutionComplete,
    /// Tool execution partial result event.
    #[serde(rename = "tool.execution_partial_result")]
    ToolExecutionPartialResult,
    /// Tool execution progress event.
    #[serde(rename = "tool.execution_progress")]
    ToolExecutionProgress,
    /// Tool execution start event.
    #[serde(rename = "tool.execution_start")]
    ToolExecutionStart,
    /// Tool user requested event.
    #[serde(rename = "tool.user_requested")]
    ToolUserRequested,
    /// User message event.
    #[serde(rename = "user.message")]
    UserMessage,
}
