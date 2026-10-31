//! Auto-generated from api.schema.json — do not edit manually.

#![allow(clippy::large_enum_variant)]
#![allow(deprecated)]
#![allow(dead_code)]
#![allow(unused_imports)]
#![allow(rustdoc::invalid_html_tags)]

use std::collections::HashMap;

pub(crate) use lifecycle_diagnostics::LifecycleDiagnosticEvent;
use serde::{Deserialize, Serialize};
pub use session_events::{
    AbortReason, AgentModelPolicy, AssistedApprovalAssessment, AutoTier, BlockedPermissionAction,
    ContextTier, IndexedSearchState, ManagedPermissionsContext, ManagedSettingsResolvedSource,
    McpOauthHttpResponse, McpOauthWWWAuthenticateParams, McpServerMetadata, McpServerSource,
    McpServerStatus, McpSkillOrigin, ModelChangeSource, ModelProviderKind, ModelProviderRef,
    OmittedBinaryOmittedReason, PermissionDecisionSource, PermissionMode, PermissionPromptRequest,
    PermissionRule, PolicyHelperCapabilities, PolicyHelperFailure, ProviderQuotaState,
    ReasoningSummary, RemediationAction, SessionLimitsConfig, SessionMode, ShutdownType,
    SkillSource, TaskBlocker, TaskCompletionOutcome, UsageGetMetricsResult,
    UserToolSessionApproval, Verbosity,
};

use super::sdk_api_shared::*;
use crate::generated::{lifecycle_diagnostics, session_events};
use crate::types::{RequestId, SessionEvent, SessionId};

/// Bounded, lifecycle-fenced host authority rendered to the Assisted Permissions judge. Source-specific meaning and scope remain opaque data interpreted by the judge. The source must match the enclosing update source. At most 8 statements of up to 1,024 characters each (4,096 total); scope follows the assisted-approval context JSON bounds. A session holds at most 8 active sources and 32 KiB of rendered grants.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthenticatedAuthorityGrant {
    /// RFC 3339 expiry timestamp.
    pub expires_at: String,
    /// Stable host-issued identifier for this authority.
    pub grant_id: String,
    /// Structured scope facts constraining the authority statements.
    pub scope: serde_json::Value,
    /// Stable identifier for the trusted authority source; must match the update source.
    pub source: String,
    /// Bounded human-readable authority statements rendered to the judge.
    pub statements: Vec<String>,
}

/// Replaces or revokes one source's revocable session-scoped authority from the owning host. A non-null grant replaces only the named source; null revokes only the named source. The revision always advances the session-global fence.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthenticatedAuthorityUpdate {
    /// Replacement authority, or null to revoke the named source.
    pub grant: Option<AuthenticatedAuthorityGrant>,
    /// Monotonically increasing host-issued revision, from 1 to 2^53 - 1 so every SDK represents it exactly.
    pub revision: i64,
    /// Stable source whose independent authority slot is being updated.
    pub source: String,
}

/// Custom grammar input format accepted by a built-in tool.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BuiltinToolFormat {
    /// Grammar definition accepted by the tool.
    pub definition: String,
    /// Grammar syntax used by the format definition.
    pub syntax: String,
    /// Custom input-format discriminator.
    pub r#type: BuiltinToolFormatType,
}

/// JSON Schema object accepted by a built-in tool.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BuiltinToolInputSchema {
    /// Root type of the tool input schema.
    pub r#type: BuiltinToolInputSchemaType,
}

/// Rust-owned metadata and input schema for a built-in tool.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BuiltinToolDescriptor {
    /// Model-facing description of the tool's behavior.
    pub description: String,
    /// Optional custom input format used instead of a JSON Schema.
    pub format: Option<BuiltinToolFormat>,
    /// Whether the tool provides a specialized intention summary.
    pub has_summarise_intention: bool,
    /// JSON Schema for the tool input, or null when the tool uses a custom format.
    pub input_schema: Option<BuiltinToolInputSchema>,
    /// Optional supplemental usage instructions for the tool.
    pub instructions: Option<String>,
    /// Whether the tool executes commands in a terminal.
    pub is_terminal: bool,
    /// Stable name used to invoke the built-in tool.
    pub name: String,
    /// Policy describing which tool metadata may be recorded without obfuscation.
    pub safe_for_telemetry: serde_json::Value,
    /// Optional human-readable title for the tool.
    pub title: Option<String>,
    /// Optional tool category discriminator.
    pub r#type: Option<String>,
}

/// Lightweight metadata for a currently initialized session tool
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CurrentToolMetadata {
    /// Whether the tool is loaded on demand via tool search
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_loading: Option<bool>,
    /// Tool description
    pub description: String,
    /// JSON Schema for tool input
    #[serde(rename = "input_schema", skip_serializing_if = "Option::is_none")]
    pub input_schema: Option<HashMap<String, serde_json::Value>>,
    /// Explicitly configured MCP server display name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_server_display_name: Option<String>,
    /// Configured MCP server key for MCP-backed tools
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_server_name: Option<String>,
    /// Raw MCP tool name for MCP-backed tools
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_tool_name: Option<String>,
    /// Model-facing tool name
    pub name: String,
    /// Optional MCP/config namespaced tool name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub namespaced_name: Option<String>,
}

/// Parameters for starting fleet orchestration: an optional user prompt combined with the fleet instructions, plus the send options forwarded to the resulting turn.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FleetStartRequest {
    /// Optional attachments (files, directories, selections, blobs, GitHub references) to include with the fleet request
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attachments: Option<Vec<serde_json::Value>>,
    /// If false, this request will not trigger a Premium Request Unit charge. User requests default to billable.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) billable: Option<bool>,
    /// Optional user prompt to combine with fleet instructions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prompt: Option<String>,
    /// If true, await completion of the agentic loop for this fleet request before returning. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wait: Option<bool>,
}

/// Indicates whether fleet mode was successfully activated.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FleetStartResult {
    /// Whether fleet mode was successfully activated
    pub started: bool,
}

/// Startup-only configuration for the experimental runtime lifecycle diagnostic event stream.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct LifecycleDiagnosticsConfiguration {
    /// Retain lifecycle diagnostic events for session.diagnostics.lifecycle.read. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expose: Option<bool>,
    /// Append lifecycle diagnostic events to diagnostics.jsonl in the session state directory. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub persist: Option<bool>,
}

/// Pre-resolved working-directory context for session startup.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionContext {
    /// Active git branch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Most recent working directory for this session
    pub cwd: String,
    /// Git repository root, if the cwd was inside a git repo
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Repository host type
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_type: Option<SessionContextHostType>,
    /// Repository slug in `owner/name` form, when known
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
}

/// Persisted local session metadata, including identifiers, timestamps, summary/name, client, context, detached state, and task ID.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LocalSessionMetadataValue {
    /// Runtime client name that created/last resumed this session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Pre-resolved working-directory context for session startup.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<SessionContext>,
    /// True for detached maintenance sessions that should be hidden from normal resume lists.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_detached: Option<bool>,
    /// Always false for local sessions.
    pub is_remote: bool,
    /// GitHub task ID, when this local session is bound to one. Only present for local sessions exported to remote control.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mc_task_id: Option<String>,
    /// Last-modified time of the session's persisted state, as ISO 8601
    pub modified_time: String,
    /// Optional human-friendly name set via /rename
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Stable session identifier
    pub session_id: SessionId,
    /// Session creation time as an ISO 8601 timestamp
    pub start_time: String,
    /// Short summary of the session, when one has been derived
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
}

/// Non-secret host-managed HTTP MCP server configuration. The containing map key is the stable managed identity; credentials are supplied dynamically by the host.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedMcpServerConfig {
    /// Human-readable catalog display name.
    pub display_name: String,
    /// Maximum dynamic-header cache lifetime in milliseconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers_refresh_ttl_ms: Option<i64>,
    /// Timeout in milliseconds for tool discovery and tool calls.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
    /// Tools to include. Defaults to all tools when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
    /// Hosted MCP streamable HTTP endpoint.
    pub url: String,
}

/// Retry outcome for one plugin required by managed settings.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ManagedPluginRetryEntry {
    /// Why the plugin was not prepared. Present when `status` is `failed` or `deferred`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Plugin spec in "plugin@marketplace" form.
    pub spec: String,
    /// What the retry did for this plugin.
    pub status: ManagedPluginRetryStatus,
}

/// Memory configuration for this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MemoryConfiguration {
    /// Whether memory is enabled for the session.
    pub enabled: bool,
    /// Memory injection mode: system-prompt (default) includes memories in the system prompt; user-prompt includes them in user messages.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<MemoryMode>,
}

/// The repository the remote session targets.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataSnapshotRemoteMetadataRepository {
    /// The branch the remote session is operating on.
    pub branch: String,
    /// The GitHub repository name (without owner).
    pub name: String,
    /// The GitHub owner (user or organization) of the target repository.
    pub owner: String,
}

/// Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are immutable for the lifetime of the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataSnapshotRemoteMetadata {
    /// The pull request number the remote session is associated with, if any.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pull_request_number: Option<i64>,
    /// The repository the remote session targets.
    pub repository: MetadataSnapshotRemoteMetadataRepository,
    /// The original resource identifier (task ID or PR node ID), preserved across event-replay reconstructions. Falls back to `sessionId` when absent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resource_id: Option<String>,
    /// Whether the remote task originated from Copilot Coding Agent (cca) or a CLI `--remote` invocation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task_type: Option<MetadataSnapshotRemoteMetadataTaskType>,
}

/// One model provider available to the session — the model analog of the account `ProviderDescriptor`. Opaque id/label/kind plus a stable ordering; central code never branches on kind.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelProviderDescriptor {
    /// Opaque, stable provider id, stamped onto every model this provider returns.
    pub id: String,
    /// The neutral provider kind.
    pub kind: ModelProviderKind,
    /// Human-readable menu label, owned by the runtime so every consumer renders identical text.
    pub label: String,
    /// Stable ordering key for presenting providers in a deterministic sequence.
    pub ordering: i64,
}

/// Source descriptor for a `session.options.update` content-exclusion rule, with source name and type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OptionsUpdateAdditionalContentExclusionPolicyRuleSource {
    /// Name of the policy source.
    pub name: String,
    /// Type of the policy source.
    pub r#type: String,
}

/// Single content-exclusion rule supplied to `session.options.update`, with paths, match conditions, and source.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OptionsUpdateAdditionalContentExclusionPolicyRule {
    /// Conditions of which at least one must match.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub if_any_match: Option<Vec<String>>,
    /// Conditions none of which may match.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub if_none_match: Option<Vec<String>>,
    /// Path patterns covered by this rule.
    pub paths: Vec<String>,
    /// Source descriptor for a `session.options.update` content-exclusion rule, with source name and type.
    pub source: OptionsUpdateAdditionalContentExclusionPolicyRuleSource,
}

/// Content-exclusion policy supplied to `session.options.update`, with rules, last-updated data, and scope.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OptionsUpdateAdditionalContentExclusionPolicy {
    /// Opaque policy update timestamp supplied by the host.
    #[serde(rename = "last_updated_at")]
    pub last_updated_at: serde_json::Value,
    /// Content-exclusion rules to apply.
    pub rules: Vec<OptionsUpdateAdditionalContentExclusionPolicyRule>,
    /// Allowed values for the `OptionsUpdateAdditionalContentExclusionPolicyScope` enumeration.
    pub scope: OptionsUpdateAdditionalContentExclusionPolicyScope,
}

/// Permission-decision request variant to approve only the current permission request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveOnce {
    /// True only when a host surfaced this request to a user who approved it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approved_interactively: Option<bool>,
    /// Approve this single request only
    pub kind: PermissionDecisionApproveOnceKind,
}

/// Session-scoped approval details for specific command identifiers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalCommands {
    /// Command identifiers covered by this approval.
    pub command_identifiers: Vec<String>,
    /// Approval scoped to specific command identifiers.
    pub kind: PermissionDecisionApproveForSessionApprovalCommandsKind,
}

/// Session-scoped approval details for read-only filesystem operations.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalRead {
    /// Approval covering read-only filesystem operations.
    pub kind: PermissionDecisionApproveForSessionApprovalReadKind,
}

/// Session-scoped approval details for filesystem write operations.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalWrite {
    /// Approval covering filesystem write operations.
    pub kind: PermissionDecisionApproveForSessionApprovalWriteKind,
}

/// Session-scoped approval details for an MCP server tool, or all tools on the server when `toolName` is null.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalMcp {
    /// Approval covering an MCP tool.
    pub kind: PermissionDecisionApproveForSessionApprovalMcpKind,
    /// MCP server name.
    pub server_name: String,
    /// MCP tool name, or null to cover every tool on the server.
    pub tool_name: Option<String>,
}

/// Session-scoped approval details for MCP sampling requests from a server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalMcpSampling {
    /// Approval covering MCP sampling requests for a server.
    pub kind: PermissionDecisionApproveForSessionApprovalMcpSamplingKind,
    /// MCP server name.
    pub server_name: String,
}

/// Session-scoped approval details for writes to long-term memory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalMemory {
    /// Approval covering writes to long-term memory.
    pub kind: PermissionDecisionApproveForSessionApprovalMemoryKind,
}

/// Session-scoped approval details for a custom tool, keyed by tool name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalCustomTool {
    /// Approval covering a custom tool.
    pub kind: PermissionDecisionApproveForSessionApprovalCustomToolKind,
    /// Custom tool name.
    pub tool_name: String,
}

/// Session-scoped approval details for extension-management operations, optionally narrowed by operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalExtensionManagement {
    /// Approval covering extension lifecycle operations such as enable, disable, or reload.
    pub kind: PermissionDecisionApproveForSessionApprovalExtensionManagementKind,
    /// Optional operation identifier; when omitted, the approval covers all extension management operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub operation: Option<String>,
}

/// Session-scoped workflow approval, optionally narrowed by approval key.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalWorkflow {
    /// Optional workflow operation name or canonical approval key; when omitted, the approval covers all workflow operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approval_key: Option<String>,
    /// Approval covering workflow operations.
    pub kind: PermissionDecisionApproveForSessionApprovalWorkflowKind,
}

/// Session-scoped approval details for an extension's permission-gated capability access, keyed by extension name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess {
    /// Extension name.
    pub extension_name: String,
    /// Approval covering an extension's request to access a permission-gated capability.
    pub kind: PermissionDecisionApproveForSessionApprovalExtensionPermissionAccessKind,
}

/// Session-scoped approval details for an extension's access to sensitive environment variables, keyed by extension name and the exact set of variable names.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalExtensionEnvAccess {
    /// Names of the sensitive environment variables this approval covers. Values are never persisted.
    pub environment_variables: Vec<String>,
    /// Extension name.
    pub extension_name: String,
    /// Approval covering an extension's request to read sensitive environment variables.
    pub kind: PermissionDecisionApproveForSessionApprovalExtensionEnvAccessKind,
}

/// Permission-decision request variant to approve for the rest of the session, with optional tool approval or URL domain.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSession {
    /// Session-scoped approval to remember (tool prompts only; omitted for path/url prompts)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approval: Option<PermissionDecisionApproveForSessionApproval>,
    /// URL domain to approve for the rest of the session (URL prompts only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub domain: Option<String>,
    /// Approve and remember for the rest of the session
    pub kind: PermissionDecisionApproveForSessionKind,
}

/// Approve file-tool read access to specific directories for this logical session, including continuation or resume.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveReadOnlyForSession {
    /// Canonical directories covered by the read-only grant
    pub directories: Vec<String>,
    /// Approve read-only file-tool directory access for this session
    pub kind: PermissionDecisionApproveReadOnlyForSessionKind,
}

/// Location-scoped approval details for specific command identifiers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalCommands {
    /// Command identifiers covered by this approval.
    pub command_identifiers: Vec<String>,
    /// Approval scoped to specific command identifiers.
    pub kind: PermissionDecisionApproveForLocationApprovalCommandsKind,
}

/// Location-scoped approval details for read-only filesystem operations.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalRead {
    /// Approval covering read-only filesystem operations.
    pub kind: PermissionDecisionApproveForLocationApprovalReadKind,
}

/// Location-scoped approval details for filesystem write operations.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalWrite {
    /// Approval covering filesystem write operations.
    pub kind: PermissionDecisionApproveForLocationApprovalWriteKind,
}

/// Location-scoped approval details for an MCP server tool, or all tools on the server when `toolName` is null.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalMcp {
    /// Approval covering an MCP tool.
    pub kind: PermissionDecisionApproveForLocationApprovalMcpKind,
    /// MCP server name.
    pub server_name: String,
    /// MCP tool name, or null to cover every tool on the server.
    pub tool_name: Option<String>,
}

/// Location-scoped approval details for MCP sampling requests from a server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalMcpSampling {
    /// Approval covering MCP sampling requests for a server.
    pub kind: PermissionDecisionApproveForLocationApprovalMcpSamplingKind,
    /// MCP server name.
    pub server_name: String,
}

/// Location-scoped approval details for writes to long-term memory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalMemory {
    /// Approval covering writes to long-term memory.
    pub kind: PermissionDecisionApproveForLocationApprovalMemoryKind,
}

/// Location-scoped approval details for a custom tool, keyed by tool name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalCustomTool {
    /// Approval covering a custom tool.
    pub kind: PermissionDecisionApproveForLocationApprovalCustomToolKind,
    /// Custom tool name.
    pub tool_name: String,
}

/// Location-scoped approval details for extension-management operations, optionally narrowed by operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalExtensionManagement {
    /// Approval covering extension lifecycle operations such as enable, disable, or reload.
    pub kind: PermissionDecisionApproveForLocationApprovalExtensionManagementKind,
    /// Optional operation identifier; when omitted, the approval covers all extension management operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub operation: Option<String>,
}

/// Location-scoped workflow approval, optionally narrowed by approval key.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalWorkflow {
    /// Optional workflow operation name or canonical approval key; when omitted, the approval covers all workflow operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approval_key: Option<String>,
    /// Approval covering workflow operations.
    pub kind: PermissionDecisionApproveForLocationApprovalWorkflowKind,
}

/// Location-scoped approval details for an extension's permission-gated capability access, keyed by extension name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess {
    /// Extension name.
    pub extension_name: String,
    /// Approval covering an extension's request to access a permission-gated capability.
    pub kind: PermissionDecisionApproveForLocationApprovalExtensionPermissionAccessKind,
}

/// Location-scoped approval details for an extension's access to sensitive environment variables, keyed by extension name and the exact set of variable names.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalExtensionEnvAccess {
    /// Names of the sensitive environment variables this approval covers. Values are never persisted.
    pub environment_variables: Vec<String>,
    /// Extension name.
    pub extension_name: String,
    /// Approval covering an extension's request to read sensitive environment variables.
    pub kind: PermissionDecisionApproveForLocationApprovalExtensionEnvAccessKind,
}

/// Permission-decision request variant to approve and persist a permission for a project location, with approval details and location key.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocation {
    /// Approval to persist for this location
    pub approval: PermissionDecisionApproveForLocationApproval,
    /// Approve and persist for this project location
    pub kind: PermissionDecisionApproveForLocationKind,
    /// Location key (git root or cwd) to persist the approval to
    pub location_key: String,
}

/// Permission-decision request variant to permanently approve a URL domain across sessions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApprovePermanently {
    /// URL domain to approve permanently
    pub domain: String,
    /// Approve and persist across sessions (URL prompts only)
    pub kind: PermissionDecisionApprovePermanentlyKind,
}

/// Permission-decision request variant to reject a pending permission request, with optional feedback.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionReject {
    /// Optional feedback explaining the rejection
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feedback: Option<String>,
    /// Reject the request
    pub kind: PermissionDecisionRejectKind,
}

/// Permission-decision variant indicating no user was available to confirm the request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionUserNotAvailable {
    /// No user is available to confirm the request
    pub kind: PermissionDecisionUserNotAvailableKind,
}

/// Permission-decision variant indicating the request was approved.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproved {
    /// The permission request was approved
    pub kind: PermissionDecisionApprovedKind,
}

/// Permission-decision variant indicating approval was remembered for the session, with approval details.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApprovedForSession {
    /// The approval to add as a session-scoped rule
    pub approval: UserToolSessionApproval,
    /// Approved and remembered for the rest of the session
    pub kind: PermissionDecisionApprovedForSessionKind,
}

/// Permission-decision variant indicating approval was persisted for a project location, with approval details and location key.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApprovedForLocation {
    /// The approval to persist for this location
    pub approval: UserToolSessionApproval,
    /// Approved and persisted for this project location
    pub kind: PermissionDecisionApprovedForLocationKind,
    /// The location key (git root or cwd) to persist the approval to
    pub location_key: String,
}

/// Permission-decision variant indicating the request was cancelled before use, with an optional reason.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionCancelled {
    /// The permission request was cancelled before a response was used
    pub kind: PermissionDecisionCancelledKind,
    /// Optional explanation of why the request was cancelled
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
}

/// Permission-decision variant indicating explicit denial by permission rules, with the matching rules.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionDeniedByRules {
    /// Denied because approval rules explicitly blocked it
    pub kind: PermissionDecisionDeniedByRulesKind,
    /// Rules that denied the request
    pub rules: Vec<PermissionRule>,
}

/// Permission-decision variant indicating no approval rule matched and user confirmation was unavailable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser {
    /// Denied because no approval rule matched and user confirmation was unavailable
    pub kind: PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind,
}

/// Permission-decision variant indicating the user denied an interactive prompt, with optional feedback and force-reject flag.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionDeniedInteractivelyByUser {
    /// Optional feedback from the user explaining the denial
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feedback: Option<String>,
    /// Whether to force-reject the current agent turn
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force_reject: Option<bool>,
    /// Denied by the user during an interactive prompt
    pub kind: PermissionDecisionDeniedInteractivelyByUserKind,
}

/// Permission-decision variant indicating denial by content-exclusion policy, with path and message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionDeniedByContentExclusionPolicy {
    /// Denied by the organization's content exclusion policy
    pub kind: PermissionDecisionDeniedByContentExclusionPolicyKind,
    /// Human-readable explanation of why the path was excluded
    pub message: String,
    /// File path that triggered the exclusion
    pub path: String,
}

/// Permission-decision variant indicating denial by a permission request hook, with optional message and interrupt flag.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionDeniedByPermissionRequestHook {
    /// Whether to interrupt the current agent turn
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interrupt: Option<bool>,
    /// Denied by a permission request hook registered by an extension or plugin
    pub kind: PermissionDecisionDeniedByPermissionRequestHookKind,
    /// Optional message from the hook explaining the denial
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

/// Pending permission request ID and the decision to apply (approve/reject and scope).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionRequest {
    /// Optional informational context describing how and where this response was made. Omit it to preserve legacy behavior without attributing an origin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub decision_context: Option<PermissionDecisionContext>,
    /// Request ID of the pending permission request
    pub request_id: RequestId,
    /// The client's response to the pending permission prompt
    pub result: PermissionDecision,
}

/// Location-persisted tool approval details for specific command identifiers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsCommands {
    /// Command identifiers covered by this approval.
    pub command_identifiers: Vec<String>,
    /// Approval scoped to specific command identifiers.
    pub kind: PermissionsLocationsAddToolApprovalDetailsCommandsKind,
}

/// Location-persisted tool approval details for read-only filesystem operations.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsRead {
    /// Approval covering read-only filesystem operations.
    pub kind: PermissionsLocationsAddToolApprovalDetailsReadKind,
}

/// Location-persisted tool approval details for filesystem write operations.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsWrite {
    /// Approval covering filesystem write operations.
    pub kind: PermissionsLocationsAddToolApprovalDetailsWriteKind,
}

/// Location-persisted tool approval details for an MCP server tool, or all tools when `toolName` is null.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsMcp {
    /// Approval covering an MCP tool.
    pub kind: PermissionsLocationsAddToolApprovalDetailsMcpKind,
    /// MCP server name.
    pub server_name: String,
    /// MCP tool name, or null to cover every tool on the server.
    pub tool_name: Option<String>,
}

/// Location-persisted tool approval details for MCP sampling requests from a server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsMcpSampling {
    /// Approval covering MCP sampling requests for a server.
    pub kind: PermissionsLocationsAddToolApprovalDetailsMcpSamplingKind,
    /// MCP server name.
    pub server_name: String,
}

/// Location-persisted tool approval details for writes to long-term memory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsMemory {
    /// Approval covering writes to long-term memory.
    pub kind: PermissionsLocationsAddToolApprovalDetailsMemoryKind,
}

/// Location-persisted tool approval details for a custom tool, keyed by tool name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsCustomTool {
    /// Approval covering a custom tool.
    pub kind: PermissionsLocationsAddToolApprovalDetailsCustomToolKind,
    /// Custom tool name.
    pub tool_name: String,
}

/// Location-persisted tool approval details for extension-management operations, optionally narrowed by operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsExtensionManagement {
    /// Approval covering extension lifecycle operations such as enable, disable, or reload.
    pub kind: PermissionsLocationsAddToolApprovalDetailsExtensionManagementKind,
    /// Optional operation identifier; when omitted, the approval covers all extension management operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub operation: Option<String>,
}

/// Location-persisted workflow approval, optionally narrowed by approval key.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsWorkflow {
    /// Optional workflow operation name or canonical approval key; when omitted, the approval covers all workflow operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approval_key: Option<String>,
    /// Approval covering workflow operations.
    pub kind: PermissionsLocationsAddToolApprovalDetailsWorkflowKind,
}

/// Location-persisted tool approval details for an extension's permission-gated capability access, keyed by extension name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess {
    /// Extension name.
    pub extension_name: String,
    /// Approval covering an extension's request to access a permission-gated capability.
    pub kind: PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccessKind,
}

/// Location-persisted tool approval details for an extension's access to sensitive environment variables, keyed by extension name and the exact set of variable names.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsExtensionEnvAccess {
    /// Names of the sensitive environment variables this approval covers. Values are never persisted.
    pub environment_variables: Vec<String>,
    /// Extension name.
    pub extension_name: String,
    /// Approval covering an extension's request to read sensitive environment variables.
    pub kind: PermissionsLocationsAddToolApprovalDetailsExtensionEnvAccessKind,
}

/// Location-scoped tool approval to persist.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionLocationAddToolApprovalParams {
    /// Tool approval to persist and apply
    pub approval: PermissionsLocationsAddToolApprovalDetails,
    /// Location key (git root or cwd) to persist the approval to
    pub location_key: String,
}

/// Working directory to load persisted location permissions for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionLocationApplyParams {
    /// Working directory whose persisted location permissions should be applied
    pub working_directory: String,
}

/// Summary of persisted location permissions applied to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionLocationApplyResult {
    /// Number of persisted allowed directories added to the live path manager
    pub applied_directory_count: i64,
    /// Number of location-scoped rules added to the live permission service
    pub applied_rule_count: i64,
    /// Location-scoped rules applied to the live permission service
    pub applied_rules: Vec<PermissionRule>,
    /// Whether a different location was applied since the previous apply call
    pub changed: bool,
    /// Location key used in the location-permissions store
    pub location_key: String,
    /// Whether the location is a git repo or directory
    pub location_type: PermissionLocationType,
}

/// Working directory to resolve into a location-permissions key.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionLocationResolveParams {
    /// Working directory whose permission location should be resolved
    pub working_directory: String,
}

/// Resolved location-permissions key and type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionLocationResolveResult {
    /// Location key used in the location-permissions store
    pub location_key: String,
    /// Whether the location is a git repo or directory
    pub location_type: PermissionLocationType,
}

/// Directory path to add to the session's allowed directories.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsAddParams {
    /// Directory to add to the allow-list. The runtime resolves and validates the path before adding, then loads conventional `.github/skills/` and `.github/agents/` definitions under it when their subsystem gates are enabled. Adding the directory is therefore also a trust decision for configuration stored there.
    pub path: String,
}

/// Path to evaluate against the session's allowed directories.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsAllowedCheckParams {
    /// Path to check against the session's allowed directories
    pub path: String,
}

/// Indicates whether the supplied path is within the session's allowed directories.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsAllowedCheckResult {
    /// Whether the path is within the session's allowed directories
    pub allowed: bool,
}

/// If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsConfig {
    /// Additional directories to allow tool access to (in addition to the session's working directory). Conventional `.github/skills/` and `.github/agents/` definitions under them also join the session catalogs when their subsystem gates are enabled, so supplying a directory is a trust decision for configuration stored there. When `unrestricted` is true, these are still pre-populated on the UnrestrictedPathManager so they remain visible via getDirectories() (e.g. for @-mention completion).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_directories: Option<Vec<String>>,
    /// Whether to include the system temp directory in the allowed list (defaults to true). Ignored when `unrestricted` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_temp_directory: Option<bool>,
    /// If true, the runtime allows access to all paths without prompting. Equivalent to constructing an UnrestrictedPathManager.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unrestricted: Option<bool>,
    /// Workspace root path (special-cased to be allowed even before the directory exists). Ignored when `unrestricted` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub workspace_path: Option<String>,
}

/// Snapshot of the session's recursive directory grants, exact session-approved paths, and primary working directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsList {
    /// All directories currently allowed for tool access on this session.
    pub directories: Vec<String>,
    /// The primary working directory for this session.
    pub primary: String,
    /// Exact paths approved for this session without recursively allowing their descendants.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_approved_paths: Option<Vec<String>>,
}

/// Directory path to set as the session's new primary working directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsUpdatePrimaryParams {
    /// Directory to set as the new primary working directory for the session's permission policy.
    pub path: String,
}

/// Path to evaluate against the session's workspace (primary) directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsWorkspaceCheckParams {
    /// Path to check against the session workspace directory
    pub path: String,
}

/// Indicates whether the supplied path is within the session's workspace directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsWorkspaceCheckResult {
    /// Whether the path is within the session workspace directory
    pub allowed: bool,
}

/// Notification payload describing the permission prompt that the client just rendered.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPromptShownNotification {
    /// Human-readable description of the prompt the user is being asked to approve. Used by the runtime to fire the registered `permission_prompt` notification hook (e.g. terminal bell, desktop notification).
    pub message: String,
}

/// Indicates whether the permission decision was applied; false when the request was already resolved.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestResult {
    /// Whether the permission request was handled successfully
    pub success: bool,
}

/// If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRulesSet {
    /// Rules that auto-approve matching requests
    pub approved: Vec<PermissionRule>,
    /// Rules that auto-deny matching requests
    pub denied: Vec<PermissionRule>,
}

/// No parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsClearResumeEscalationRequest {}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsClearResumeEscalationResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Source descriptor for a `session.permissions.configure` content-exclusion rule, with source name and type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsConfigureAdditionalContentExclusionPolicyRuleSource {
    /// Name of the policy source.
    pub name: String,
    /// Type of the policy source.
    pub r#type: String,
}

/// Single content-exclusion rule supplied to `session.permissions.configure`, with paths, match conditions, and source.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsConfigureAdditionalContentExclusionPolicyRule {
    /// Conditions of which at least one must match.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub if_any_match: Option<Vec<String>>,
    /// Conditions none of which may match.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub if_none_match: Option<Vec<String>>,
    /// Path patterns covered by this rule.
    pub paths: Vec<String>,
    /// Source descriptor for a `session.permissions.configure` content-exclusion rule, with source name and type.
    pub source: PermissionsConfigureAdditionalContentExclusionPolicyRuleSource,
}

/// Content-exclusion policy supplied to `session.permissions.configure`, with rules, last-updated data, and scope.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsConfigureAdditionalContentExclusionPolicy {
    /// Opaque policy update timestamp supplied by the host.
    #[serde(rename = "last_updated_at")]
    pub last_updated_at: serde_json::Value,
    /// Content-exclusion rules to apply.
    pub rules: Vec<PermissionsConfigureAdditionalContentExclusionPolicyRule>,
    /// Allowed values for the `PermissionsConfigureAdditionalContentExclusionPolicyScope` enumeration.
    pub scope: PermissionsConfigureAdditionalContentExclusionPolicyScope,
}

/// One SDK-host source's bounded JSON value for future Assisted Permissions judge requests. Replaces only the named source; a null value clears that source.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsConfigureAssistedApprovalContext {
    /// Stable host source identifier. It isolates this source from unrelated producers and may contain ASCII letters, digits, dots, underscores, and hyphens.
    pub source: String,
    /// Arbitrary bounded JSON context. A null value clears only this source. Runtime framing states that this value is not an instruction, policy decision, human authorization, or permission.
    pub value: serde_json::Value,
}

/// If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionUrlsConfig {
    /// Initial list of allowed URL/domain patterns. Patterns may include path components. Ignored when `unrestricted` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initial_allowed: Option<Vec<String>>,
    /// If true, the runtime allows access to all URLs without prompting. Initial allow-list is ignored when this is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unrestricted: Option<bool>,
}

/// Patch of permission policy fields to apply (omit a field to leave it unchanged).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsConfigureParams {
    /// If specified, replaces the host-supplied GitHub Content Exclusion policies on the session (combined with natively-discovered policies when evaluating tool/file access). Omit to leave the current policies unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_content_exclusion_policies:
        Option<Vec<PermissionsConfigureAdditionalContentExclusionPolicy>>,
    /// If specified, sets whether path/URL read permission requests are auto-approved. Omit to leave the current value unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approve_all_read_permission_requests: Option<bool>,
    /// If specified, sets whether tool permission requests are auto-approved without prompting. Omit to leave the current value unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approve_all_tool_permission_requests: Option<bool>,
    /// If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub paths: Option<PermissionPathsConfig>,
    /// If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub rules: Option<PermissionRulesSet>,
    /// If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub urls: Option<PermissionUrlsConfig>,
}

/// Extensible [`PermissionsConfigureParams`], including inputs added after it was published.
///
/// Required inputs are [`PermissionsConfigureOptions::new`] arguments; optional inputs have fluent setters.
/// Input-only: it serialises to the flat wire request and is not deserialisable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsConfigureOptions {
    #[serde(flatten)]
    legacy: PermissionsConfigureParams,
    #[serde(skip_serializing_if = "Option::is_none")]
    assisted_approval_instructions: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    assisted_approval_context: Option<PermissionsConfigureAssistedApprovalContext>,
    #[serde(skip_serializing_if = "Option::is_none")]
    authenticated_authority: Option<AuthenticatedAuthorityUpdate>,
    #[serde(skip_serializing_if = "Option::is_none")]
    host_policy_permission_handler: Option<bool>,
}

impl PermissionsConfigureOptions {
    /// Creates options with the required inputs.
    pub fn new() -> Self {
        Self {
            legacy: PermissionsConfigureParams {
                approve_all_tool_permission_requests: None,
                approve_all_read_permission_requests: None,
                rules: None,
                paths: None,
                urls: None,
                additional_content_exclusion_policies: None,
            },
            assisted_approval_instructions: None,
            assisted_approval_context: None,
            authenticated_authority: None,
            host_policy_permission_handler: None,
        }
    }

    /// If specified, sets whether tool permission requests are auto-approved without prompting. Omit to leave the current value unchanged.
    pub fn approve_all_tool_permission_requests(mut self, value: bool) -> Self {
        self.legacy.approve_all_tool_permission_requests = Some(value);
        self
    }

    /// If specified, sets whether path/URL read permission requests are auto-approved. Omit to leave the current value unchanged.
    pub fn approve_all_read_permission_requests(mut self, value: bool) -> Self {
        self.legacy.approve_all_read_permission_requests = Some(value);
        self
    }

    /// If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged.
    pub fn rules(mut self, value: PermissionRulesSet) -> Self {
        self.legacy.rules = Some(value);
        self
    }

    /// If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.
    pub fn paths(mut self, value: PermissionPathsConfig) -> Self {
        self.legacy.paths = Some(value);
        self
    }

    /// If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged.
    pub fn urls(mut self, value: PermissionUrlsConfig) -> Self {
        self.legacy.urls = Some(value);
        self
    }

    /// If specified, replaces the host-supplied GitHub Content Exclusion policies on the session (combined with natively-discovered policies when evaluating tool/file access). Omit to leave the current policies unchanged.
    pub fn additional_content_exclusion_policies(
        mut self,
        value: Vec<PermissionsConfigureAdditionalContentExclusionPolicy>,
    ) -> Self {
        self.legacy.additional_content_exclusion_policies = Some(value);
        self
    }

    /// User-authored instructions appended verbatim to the assisted-approval judge's system prompt. Applies to subsequent judge requests in this local session without changing permission mode, pre-judge gates, or the coding agent's prompt. Omit to leave unchanged; an empty or whitespace-only string clears the instructions. Hosts must reapply their configuration after resuming a session. Remote sessions are not supported.
    pub fn assisted_approval_instructions(mut self, value: impl Into<String>) -> Self {
        self.assisted_approval_instructions = Some(value.into());
        self
    }

    /// SDK-host advisory JSON for future Assisted Permissions judge requests. The value replaces only its named source; null clears that source. Omit to leave all sources unchanged. The runtime applies strict structural and size limits, renders the source in a runtime-controlled advisory frame, and never treats the value as instructions or authorization. Session-scoped only; hosts must reapply after resume.
    pub fn assisted_approval_context(
        mut self,
        value: PermissionsConfigureAssistedApprovalContext,
    ) -> Self {
        self.assisted_approval_context = Some(value);
        self
    }

    /// Optional owner-authenticated per-source update to lifecycle-fenced session authority. Active grants from all sources are rendered together, in source order, to the Assisted Permissions judge, which alone interprets their statements and scope. Only the owning non-extension session connection, bound to the current session generation, may replace or revoke authority. Omit to leave unchanged. Hard runtime controls, mandatory policy, and later human restrictions still take precedence. Session-scoped only; hosts must reapply after resume.
    pub fn authenticated_authority(mut self, value: AuthenticatedAuthorityUpdate) -> Self {
        self.authenticated_authority = Some(value);
        self
    }

    /// Declares whether this connection's registered permission handler applies non-human host policy to the tools this connection registered. When true, unattended Assisted Autopilot sends permission requests for those tools to the handler before runtime Assisted Approval or unattended fallback can settle them. All other requests, including shell commands, file access, URLs, and tools registered by other connections or extensions, keep runtime settlement. Managed policy and other runtime enforcement still take precedence. Only the session's owning non-extension connection may set this field. The declaration ends when that connection detaches or the session is resumed, so hosts must reapply it after resume. Omit to leave unchanged; false clears it. Without a declaration, registered handlers are not consulted during unattended Assisted Autopilot. Remote sessions are not supported.
    pub fn host_policy_permission_handler(mut self, value: bool) -> Self {
        self.host_policy_permission_handler = Some(value);
        self
    }
}

impl Default for PermissionsConfigureOptions {
    fn default() -> Self {
        Self::new()
    }
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsConfigureResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// The verbatim managed `permissions` slice to interpret.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct PermissionsEvaluateManagedPolicyParams {
    /// The `permissions` object from a managed settings layer, for example `{"disableBypassPermissionsMode":"disable"}`. Omitted or null means no managed policy applies.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permissions: Option<serde_json::Value>,
}

/// The policy questions a host asks before granting elevated permissions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsEvaluateManagedPolicyResult {
    /// True when managed policy forbids bypass-permissions mode, so allow-all style flags must not take effect.
    pub bypass_permissions_disabled: bool,
    /// True when the managed layer supplies permission rules that the session must enforce.
    pub permission_rules_active: bool,
    /// True when a managed policy exists but this build could not determine its meaning. Hosts fail closed on it rather than assuming permission was granted.
    pub policy_undetermined: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsFolderTrustAddTrustedResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// No parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsGetModeRequest {}

/// Current permission mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsGetModeResult {
    /// Current permission mode
    pub mode: PermissionMode,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Scope and add/remove instructions for modifying session- or location-scoped permission rules.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsModifyRulesParams {
    /// Rules to add to the scope. Applied before `remove`/`removeAll`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub add: Option<Vec<PermissionRule>>,
    /// Specific rules to remove from the scope. Ignored when `removeAll` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remove: Option<Vec<PermissionRule>>,
    /// When true, removes every rule currently in the scope (after any `add` is applied). Useful for clearing the location scope wholesale.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remove_all: Option<bool>,
    /// Whether the change applies to ephemeral session-scoped rules (cleared at session end) or to location-scoped rules persisted via the location-permissions config file.
    pub scope: PermissionsModifyRulesScope,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsModifyRulesResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsNotifyPromptShownResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsPathsAddResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// No parameters; returns the session's recursive directory grants and exact session-approved paths.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsPathsListRequest {}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsPathsUpdatePrimaryResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// No parameters; returns currently-pending permission requests for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsPendingRequestsRequest {}

/// Clears session-scoped tool approvals and optionally clears location-scoped approvals and exact session-approved paths.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsResetSessionApprovalsRequest {
    /// Whether location-scoped approvals and exact session-approved paths are cleared too. Defaults to `true`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_location: Option<bool>,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsResetSessionApprovalsResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Allow-all toggle for tool permission requests, with an optional telemetry source.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetApproveAllRequest {
    /// Whether to auto-approve all tool permission requests
    pub enabled: bool,
    /// Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<PermissionsSetApproveAllSource>,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetApproveAllResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Permission mode to apply for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetModeRequest {
    /// Optional judge model id for assisted mode. When omitted, the session resolves the provider default: `gpt-5.5` for CAPI sessions and the active session model for BYOK sessions.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub assisted_approval_model: Option<String>,
    /// Permission mode to apply
    pub mode: PermissionMode,
    /// Optional source for permission-mode telemetry. `organization_targeting` is reserved for startup selection after the authenticated account matches an organization targeting policy; SDK callers default to `rpc` and cannot claim targeting provenance.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<PermissionModeSource>,
}

/// Indicates whether the requested permission mode was applied and reports the authoritative post-mutation mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetModeResult {
    /// Authoritative permission mode after the mutation
    pub mode: PermissionMode,
    /// Whether the operation succeeded
    pub success: bool,
}

/// Toggles whether permission prompts should be bridged into session events for this client.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetRequiredRequest {
    /// Whether the client wants `permission.requested` events bridged from the session-owned permission service. CLI clients that render prompt UI set this to `true` for as long as their listener is mounted; headless callers leave it unset (the default is `false`).
    pub required: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetRequiredResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsUrlsSetUnrestrictedModeResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Whether the URL-permission policy should run in unrestricted mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionUrlsSetUnrestrictedModeParams {
    /// Whether to allow access to all URLs without prompting. Toggles the runtime's URL-permission policy in place.
    pub enabled: bool,
}

/// Namespace that groups related caller-implemented tools for model tool discovery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProtocolExternalToolNamespace {
    /// Optional model-visible explanation of the tools in this namespace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Namespace name: 1-64 ASCII letters, digits, underscores, or hyphens.
    pub name: String,
}

/// Serializable definition of a caller-implemented tool whose execution is handled over the SDK connection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProtocolExternalToolDefinition {
    /// Tool-loading deferral policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer: Option<ProtocolExternalToolDefer>,
    /// Model-visible explanation of what the tool does.
    pub description: String,
    /// Whether the tool executes commands in a terminal.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_terminal: Option<bool>,
    /// Optional caller-defined metadata associated with the tool.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<HashMap<String, serde_json::Value>>,
    /// Unique model-visible tool name.
    pub name: String,
    /// Optional namespace grouping this tool with related tools. Where the model
    /// provider supports native tool namespaces, deferred tools sharing a namespace
    /// are presented to the model as one group; elsewhere the tool stays flat. The
    /// tool's `name` remains its unique invocation name either way.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub namespace: Option<ProtocolExternalToolNamespace>,
    /// Whether this definition replaces a built-in tool with the same name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub overrides_built_in_tool: Option<bool>,
    /// JSON Schema describing the tool's input arguments.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parameters: Option<HashMap<String, serde_json::Value>>,
    /// Whether execution bypasses the normal tool permission prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_permission: Option<bool>,
    /// Optional human-readable display title.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
}

/// Custom model-provider configuration (BYOK).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderConfig {
    /// API key. Optional for local providers like Ollama.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_key: Option<String>,
    /// Command run by the runtime before each provider request to obtain a fresh API key. Credential-command failures fail the turn without sending a model request. Bearer authentication takes precedence.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_key_command: Option<String>,
    /// Azure-specific provider options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub azure: Option<ProviderConfigAzure>,
    /// API endpoint URL.
    pub base_url: String,
    /// Bearer token for authentication. Sets the Authorization header directly. Takes precedence over apiKey when both are set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub bearer_token: Option<String>,
    /// When true, the SDK client supplies bearer tokens on demand: the runtime calls the client-session `providerToken.getToken` callback before each request and applies the returned token as an `Authorization: Bearer <token>` header. This is the bearer/OAuth scheme used by Azure AD / managed-identity tokens and provider OAuth access tokens (including Anthropic's), not a provider-specific API-key header such as Anthropic's `x-api-key`. The token-acquiring function itself stays on the SDK side and is never serialized; only this flag crosses the wire. When set alongside `apiKey`/`bearerToken`, the callback takes precedence: the runtime applies the token returned by `providerToken.getToken` as the `Authorization: Bearer` header for each request and does not send the static credential.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub has_bearer_token_provider: Option<bool>,
    /// Custom HTTP headers to include in all outbound requests to the provider.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
    /// Maximum context window tokens for the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_context_window_tokens: Option<f64>,
    /// Maximum output tokens for the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<f64>,
    /// Maximum prompt/input tokens for the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<f64>,
    /// Overrides for model capabilities when they cannot be inferred from modelId.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities: Option<ModelCapabilitiesOverride>,
    /// Well-known model ID used for capability lookup. When set, agent behavior config and token limits are inferred from this model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
    /// The product serving the model, reported in telemetry as `model_provider`. Set it when `type` alone cannot identify the product, such as Ollama or LM Studio behind an OpenAI-compatible endpoint, or a proxy or custom domain in front of Azure OpenAI. Besides telemetry, `azure_openai` makes OpenAI Responses requests omit explicit `prompt_cache_breakpoint` markers, which limit cross-turn cache reuse on Azure.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_provider: Option<ProviderConfigModelProvider>,
    /// Provider name used for model and telemetry attribution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_name: Option<String>,
    /// Provider transport. Defaults to "http".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub transport: Option<ProviderConfigTransport>,
    /// Provider type. Defaults to "openai" for generic OpenAI-compatible APIs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<ProviderConfigType>,
    /// Wire API format (openai/azure only). Defaults to "completions".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_api: Option<ProviderConfigWireApi>,
    /// The model identifier sent to the provider API for inference (the "wire" model), as opposed to modelId which is the well-known base.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_model: Option<String>,
}

/// Reattach to an existing MC session without creating a new one.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlConfigExistingMcSession {
    /// Existing MC session ID to reattach to.
    pub mc_session_id: String,
    /// Existing MC task ID for the reattached session.
    pub mc_task_id: String,
}

/// Configuration for the runtime-managed remote-control singleton.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlConfig {
    /// Reattach to an existing MC session without creating a new one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub existing_mc_session: Option<RemoteControlConfigExistingMcSession>,
    /// Whether the user explicitly requested remote (vs. implicit session-sync). Controls warning surfacing for missing-repo cases.
    pub explicit: bool,
    /// Whether remote export should be enabled.
    pub remote: bool,
    /// When true, suppresses timeline messages on successful setup.
    pub silent: bool,
    /// Whether the MC session may steer the local session (write mode).
    pub steerable: bool,
    /// Existing Mission Control task ID to attach the exported session to.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task_id: Option<String>,
}

/// GitHub repository the remote session belongs to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteSessionMetadataRepository {
    /// Branch associated with the remote session.
    pub branch: String,
    /// Repository name.
    pub name: String,
    /// Repository owner.
    pub owner: String,
}

/// Remote session metadata for the session to hand off (typically obtained from `sessions.list` with `source: "remote"`).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteSessionMetadataValue {
    /// Most recent working directory context.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<SessionContext>,
    /// Host-supplied human description of what the session is doing right now ("running tests", "waiting for approval"). Optional in the protocol and absent on hosts that do not publish it, so never rely on it -- it enriches `hostStatus`, it does not replace it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_activity: Option<String>,
    /// Live status as the owning host reports it in its session listing, so a row for a session running elsewhere can show that it is running. Absent for hosts that publish no such status (the cloud task managers), which read as idle.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_status: Option<RemoteSessionHostStatus>,
    /// Always true for remote sessions.
    pub is_remote: bool,
    /// Last-modified time as an ISO 8601 timestamp.
    pub modified_time: String,
    /// Optional human-friendly name set via /rename.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Pull request number associated with the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pull_request_number: Option<i64>,
    /// Backing remote session IDs (most recent first).
    pub remote_session_ids: Vec<String>,
    /// GitHub repository the remote session belongs to.
    pub repository: RemoteSessionMetadataRepository,
    /// Original remote resource identifier (task ID or PR node ID).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resource_id: Option<String>,
    /// Stable session identifier.
    pub session_id: SessionId,
    /// Deadline (ISO 8601) at which a CLI remote session becomes stale without further heartbeats.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stale_at: Option<String>,
    /// Session creation time as an ISO 8601 timestamp.
    pub start_time: String,
    /// Server-side task state returned by GitHub.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub state: Option<String>,
    /// Short summary of the session, when one has been derived.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    /// Whether the remote task originated from CCA or CLI `--remote`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task_type: Option<RemoteSessionMetadataTaskType>,
}

/// Repository context for the remote session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteSessionRepository {
    /// Optional branch associated with the remote session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Repository name.
    pub name: String,
    /// Repository owner or organization login.
    pub owner: String,
}

/// Current activity flags for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionActivity {
    /// Whether an in-flight operation can currently be aborted.
    pub abortable: bool,
    /// Whether the session currently has active work, including running turns or tasks.
    pub has_active_work: bool,
}

/// Internal GitHub login parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAuthLoginRequest {
    /// GitHub host URL
    pub host: String,
    /// GitHub login
    pub login: String,
    /// Whether to persist the token after login
    #[serde(skip_serializing_if = "Option::is_none")]
    pub persist: Option<bool>,
    /// GitHub authentication token
    pub token: String,
}

/// Parameters identifying a GitHub authentication to log out.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAuthLogoutUserRequest {
    /// Authentication information to log out
    pub auth_info: AuthInfo,
}

/// Authentication status and account metadata for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAuthStatus {
    /// Authentication type
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_type: Option<AuthInfoType>,
    /// Copilot plan tier (e.g., individual_pro, business)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_plan: Option<String>,
    /// Authentication host URL
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    /// Whether the session has resolved authentication
    pub is_authenticated: bool,
    /// Authenticated login/username, if available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// Human-readable authentication status description
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_message: Option<String>,
}

/// Parameters for switching the session's active authentication.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAuthSwitchRequest {
    /// Authentication information to activate
    pub auth_info: AuthInfo,
    /// Optional token paired with the authentication information
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token: Option<String>,
}

/// Map of sessionId -> bytes freed by removing the session's workspace directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionBulkDeleteResult {
    /// Map of sessionId -> bytes freed by removing the session's workspace directory. Sessions whose deletion failed are omitted from this map (failures are logged on the server but not surfaced per-id; check the map for absent IDs to detect them).
    pub freed_bytes: HashMap<String, i64>,
}

/// The IDE a host is connected to, as reported to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConnectedIdeInfo {
    /// Display name of the connected IDE, for example `VS Code`.
    pub ide_name: String,
    /// Absolute path of the workspace folder the IDE has open.
    pub workspace_folder: String,
}

/// The six normalized `/context` header buckets, computed from the same tokenization as `entries` so the two never disagree. Convenience rollups: `freeSpace` and `buffer` describe window capacity rather than occupied context, so the values do not sum to `totalTokens`.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionContextAttributionCategories {
    /// Overlapping output reservation plus post-blocking-threshold buffer.
    pub buffer: i64,
    /// Custom-instructions tokens (0 when none are configured).
    pub custom_instructions: i64,
    /// Remaining unused window capacity (clamped at 0).
    pub free_space: i64,
    /// MCP tool-definition tokens.
    pub mcp_tools: i64,
    /// Conversation (user/assistant/tool) message tokens.
    pub messages: i64,
    /// System prompt tokens, excluding custom instructions.
    pub system_prompt: i64,
    /// Non-MCP tool-definition tokens.
    pub system_tools: i64,
}

/// Successful compaction history for the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionContextAttributionCompactions {
    /// Number of successful compactions in this session.
    pub count: i64,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionContextAttributionEntriesItem {
    /// Supplementary per-entry metadata (e.g. `messageCount`, `role`, `evictable`, `pluginSource`). Values are stringified; parse as needed and ignore unrecognized keys.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attributes: Option<HashMap<String, String>>,
    /// Identifier for this entry, formed by joining its `kind` and source name (e.g. `tool:bash`, `skill:tmux`, `toolDefinition:bash`); unique within the snapshot. Use it to match the same entry across snapshots, to correlate with other APIs (skill/agent/MCP registries), and as the `parentId` target for nesting. Distinct from the human-facing `label`.
    pub id: String,
    /// Source category for this entry. Not a closed set — tolerate unknown values. Known values today: `skill`, `subagent`, `mcpServer`, `tool`, `system`, `toolDefinition`, `plugin`.
    pub kind: String,
    /// Human-readable display label, e.g. `bash` or `skill: tmux`. Presentation-only; may be localized/reformatted without notice — do not key off it.
    pub label: String,
    /// Optional `id` of the parent entry: e.g. a `plugin` entry parenting its `skill`/`mcpServer` entries, or the `system` entry parenting `toolDefinition` entries. Omitted for top-level entries.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_id: Option<String>,
    /// Token count currently in context attributable to this entry.
    pub tokens: i64,
}

/// Per-source context-window attribution, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionContextAttribution {
    /// Output reservation overlapping the displayed prompt allowance plus the tokens past the effective input budget's buffer-exhaustion blocking threshold. Mirrors `SessionContextInfo.bufferTokens`.
    pub buffer_tokens: i64,
    /// The six normalized `/context` header buckets, computed from the same tokenization as `entries` so the two never disagree. Convenience rollups: `freeSpace` and `buffer` describe window capacity rather than occupied context, so the values do not sum to `totalTokens`.
    pub categories: SessionContextAttributionCategories,
    /// Successful compaction history for the session.
    pub compactions: SessionContextAttributionCompactions,
    /// Token count at which background compaction starts. Mirrors `SessionContextInfo.compactionThreshold`.
    pub compaction_threshold: i64,
    /// Flat list of per-source attribution entries. Group by `kind` and render unrecognized kinds generically. Nesting and rollups are expressed via `parentId`.
    pub entries: Vec<SessionContextAttributionEntriesItem>,
    /// Advertised prompt allowance for the selected context tier: the denominator for context-usage displays and capacity for `categories.freeSpace` and `categories.buffer`. Mirrors `SessionContextInfo.limit`.
    pub limit: i64,
    /// The concrete model id the entire breakdown was tokenized against (feeds the per-model token multiplier). Under `Auto` (Free/Student) this is the resolved model, not the literal `auto` sentinel, so totals are not undercounted. A single-model approximation of a potentially multi-model Auto session.
    pub model_id: String,
    /// How `modelId` was chosen. Not a closed set — tolerate unknown values. Known values today: `autoResolved` (the model Auto resolved to), `selected` (the user's explicitly selected model), `default` (a fallback before any model is known).
    pub model_source: String,
    /// Effective input budget after reserving requested output against the combined context ceiling. Mirrors `SessionContextInfo.promptTokenLimit`.
    pub prompt_token_limit: i64,
    /// Total token count of the current context window the entries are measured against (system message + conversation messages + tool definitions — the same total reported by /context). Divide an entry's `tokens` by this to derive its share.
    pub total_tokens: i64,
}

/// Token breakdown for the current context window, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionContextInfo {
    /// Output reservation overlapping the displayed prompt allowance plus tokens after the effective input budget's buffer-exhaustion blocking threshold (default 95%).
    pub buffer_tokens: i64,
    /// Token count at which background compaction starts (configurable percentage of promptTokenLimit)
    pub compaction_threshold: i64,
    /// Tokens consumed by user/assistant/tool messages
    pub conversation_tokens: i64,
    /// Provider-owned model display label, or the Auto routing label. Presentation only; modelName remains the tokenization identity.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_model_name: Option<String>,
    /// Advertised prompt allowance for the selected context tier, without adding output tokens. The denominator for context-usage displays.
    pub limit: i64,
    /// Tokens consumed by MCP tool definitions (subset of toolDefinitionsTokens, excludes deferred tools)
    pub mcp_tools_tokens: i64,
    /// The model used for token counting
    pub model_name: String,
    /// Effective input budget: the selected tier's prompt allowance bounded by the combined context ceiling minus the requested output allowance. Uses DEFAULT_TOKEN_LIMIT when limits are unspecified.
    pub prompt_token_limit: i64,
    /// Provider whose catalog supplies this context window. Omitted when provider identity is unavailable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ModelProviderRef>,
    /// Tokens consumed by the system prompt
    pub system_tokens: i64,
    /// Tokens consumed by tool definitions sent to the model (excludes deferred tools)
    pub tool_definitions_tokens: i64,
    /// Sum of system, conversation and tool-definition tokens
    pub total_tokens: i64,
}

/// The enriched metadata records, with summary and context fields backfilled where available. Sessions confirmed empty and unnamed are omitted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEnrichMetadataResult {
    /// Enriched records, with summary and context backfilled. Sessions confirmed empty and unnamed may be omitted.
    pub sessions: Vec<LocalSessionMetadataValue>,
}

/// File path, content to append, and optional mode for the client-provided session filesystem. Implementations create parent directories as needed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsAppendFileRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
    /// Content to append
    pub content: String,
    /// Optional POSIX-style mode for newly created files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<i64>,
}

/// Describes a filesystem error.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsError {
    /// Error classification
    pub code: SessionFsErrorCode,
    /// Free-form detail about the error, for logging/diagnostics
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// For failed writeFile requests only: true if the provider changed the target before failing. Omit when unknown or unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub write_changed: Option<bool>,
}

/// Path to test for existence in the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsExistsRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
}

/// Indicates whether the requested path exists in the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsExistsResult {
    /// Whether the path exists
    pub exists: bool,
}

/// Directory path to create in the client-provided session filesystem, with options for recursive creation and POSIX mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsMkdirRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
    /// Create parent directories as needed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recursive: Option<bool>,
    /// Optional POSIX-style mode for newly created directories
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<i64>,
}

/// Directory path whose entries should be listed from the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
}

/// Names of entries in the requested directory, or a filesystem error if the read failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirResult {
    /// Entry names in the directory
    pub entries: Vec<String>,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

/// Directory entry returned by session filesystem `readdirWithTypes`, with name and entry type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesEntry {
    /// Entry name
    pub name: String,
    /// Entry type
    pub r#type: SessionFsReaddirWithTypesEntryType,
}

/// Directory path whose entries (with type information) should be listed from the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
}

/// Entries in the requested directory paired with file/directory type information, or a filesystem error if the read failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesResult {
    /// Directory entries with type information
    pub entries: Vec<SessionFsReaddirWithTypesEntry>,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

/// Path of the binary file to read from the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileBytesRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
}

/// File bytes as standard base64, or a filesystem error if the read failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileBytesResult {
    /// Exact file bytes encoded as standard base64
    pub content: String,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

/// A byte range in a provider-owned file. Offsets and lengths count bytes, not Unicode characters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileRangeRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
    /// Zero-based byte offset. At or beyond EOF the result contains no bytes.
    pub offset: i64,
    /// Maximum bytes to return. Omit to read through EOF; zero returns only the authoritative file byte length.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub length: Option<i64>,
}

/// Raw file bytes encoded as base64 and the authoritative total file byte length at the read, or a filesystem error.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileRangeResult {
    /// Total raw byte length of the file, independent of the requested range or base64 encoding.
    pub byte_length: i64,
    /// Standard padded base64 encoding of the requested bytes. Range boundaries may split UTF-8 characters.
    pub content: String,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

/// Path of the file to read from the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
}

/// File content as a UTF-8 string, or a filesystem error if the read failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileResult {
    /// File content as UTF-8 string
    pub content: String,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

/// Source and destination paths for renaming or moving an entry in the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsRenameRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Source path using SessionFs conventions
    pub src: String,
    /// Destination path using SessionFs conventions
    pub dest: String,
}

/// Path to remove from the client-provided session filesystem, with options for recursive removal and force.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsRmRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
    /// Remove directories and their contents recursively
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recursive: Option<bool>,
    /// Ignore errors if the path does not exist
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force: Option<bool>,
}

/// Optional capabilities declared by the provider
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSetProviderCapabilities {
    /// Whether the provider supports binary reads and writes through sessionFs.readFileBytes and sessionFs.writeFileBytes
    #[serde(skip_serializing_if = "Option::is_none")]
    pub binary: Option<bool>,
    /// Whether every session provider implements bounded readFileRange byte reads with an authoritative total file byte length. Independent of binary support. Omitted or false retains whole-file compatibility reads.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub read_file_range: Option<bool>,
    /// Whether the provider supports SQLite query/exists operations
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sqlite: Option<bool>,
}

/// Initial working directory, session-state path layout, and path conventions used to register the calling SDK client as the session filesystem provider. A registered provider is authoritative for path interpretation and filesystem facts used by workspace permission validation. Paths are interpreted lexically; home-relative paths (`~` and `~/...`) and Windows drive-relative paths such as `C:foo` are unsupported. Until provider-side canonicalization is supported, providers must not expose symlinks inside allowed roots that escape those roots.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSetProviderRequest {
    /// Optional capabilities declared by the provider
    #[serde(skip_serializing_if = "Option::is_none")]
    pub capabilities: Option<SessionFsSetProviderCapabilities>,
    /// Path conventions used by this filesystem
    pub conventions: SessionFsSetProviderConventions,
    /// Absolute initial working directory for sessions. Registering the provider establishes this path as the root of its virtual namespace; the runtime does not require the provider to materialize or stat it before creating a session.
    pub initial_cwd: String,
    /// Path within each session's SessionFs where the runtime stores files for that session
    pub session_state_path: String,
}

/// Indicates whether the calling client was registered as the session filesystem provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSetProviderResult {
    /// Whether the provider was set successfully
    pub success: bool,
}

/// Indicates whether the per-session SQLite database already exists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSqliteExistsResult {
    /// Whether the session database already exists
    pub exists: bool,
}

/// SQL query, query type, and optional bind parameters for executing a SQLite query against the per-session database. The provider applies its SQLite busy timeout for every call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSqliteQueryRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// SQL query to execute
    pub query: String,
    /// How to execute the query: 'exec' for DDL/multi-statement (no results), 'query' for SELECT (returns rows), 'run' for INSERT/UPDATE/DELETE (returns rowsAffected)
    pub query_type: SessionFsSqliteQueryType,
    /// Optional named bind parameters
    #[serde(skip_serializing_if = "Option::is_none")]
    pub params: Option<HashMap<String, serde_json::Value>>,
}

/// Query results including rows, columns, and rows affected, or a filesystem error if execution failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSqliteQueryResult {
    /// Column names from the result set
    pub columns: Vec<String>,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
    /// SQLite last_insert_rowid() value for INSERT.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_insert_rowid: Option<i64>,
    /// For SELECT: array of row objects. For others: empty array.
    pub rows: Vec<HashMap<String, serde_json::Value>>,
    /// Number of rows affected (for INSERT/UPDATE/DELETE)
    pub rows_affected: i64,
}

/// Classified SQLite transaction failure. busyOrLocked guarantees rollback; postCommitAmbiguous must never be retried.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSqliteTransactionError {
    /// Machine-readable classification of the transaction failure.
    pub error_class: SessionFsSqliteTransactionErrorClass,
    /// Human-readable transaction failure message.
    pub message: String,
}

/// One statement in an atomic SQLite transaction.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSqliteTransactionStatement {
    /// Optional named bind parameters.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub params: Option<HashMap<String, serde_json::Value>>,
    /// SQL statement to execute.
    pub query: String,
    /// How to execute the statement.
    pub query_type: SessionFsSqliteQueryType,
}

/// Statements to execute atomically. Providers apply busy handling for every call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSqliteTransactionRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Ordered SQL statements to execute in one transaction.
    pub statements: Vec<SessionFsSqliteTransactionStatement>,
}

/// Per-statement results, or a classified transaction error.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSqliteTransactionResult {
    /// Classified transaction failure, when execution did not succeed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsSqliteTransactionError>,
    /// Per-statement query results in input order.
    pub results: Vec<SessionFsSqliteQueryResult>,
}

/// Path whose metadata should be returned from the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsStatRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
}

/// Filesystem metadata for the requested path, or a filesystem error if the stat failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsStatResult {
    /// ISO 8601 timestamp of creation
    pub birthtime: String,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
    /// Whether the path is a directory
    pub is_directory: bool,
    /// Whether the path is a file
    pub is_file: bool,
    /// ISO 8601 timestamp of last modification
    pub mtime: String,
    /// File size in bytes
    pub size: i64,
}

/// File path, standard-base64-encoded bytes to write, and optional mode for the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsWriteFileBytesRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
    /// Exact file bytes encoded as standard base64
    pub content: String,
    /// Optional POSIX-style mode for newly created files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<i64>,
}

/// File path, content to write, and optional mode for the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsWriteFileRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
    /// Content to write
    pub content: String,
    /// Optional POSIX-style mode for newly created files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<i64>,
}

/// Installed plugin record for a session, with marketplace, version, install time, enabled state, cache path, and source.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstalledPlugin {
    /// Path where the plugin is cached locally
    #[serde(rename = "cache_path", skip_serializing_if = "Option::is_none")]
    pub cache_path: Option<String>,
    /// Whether the plugin is currently enabled
    pub enabled: bool,
    /// Installation timestamp (ISO-8601)
    #[serde(rename = "installed_at")]
    pub installed_at: String,
    /// Absolute path of the marketplace directory a live plugin was resolved from. Present only on live, never-persisted records — those synthesized at session start for a directory/local marketplace, whose cache_path points at the real plugin directory on disk rather than a copy under the installed-plugins cache. Its presence is what marks a record as live, and no record carrying it is ever written to the persisted installedPlugins key.
    #[serde(rename = "installed_from", skip_serializing_if = "Option::is_none")]
    pub installed_from: Option<String>,
    /// Installation-time marketplace or direct source, independent of mutable marketplace aliases. Local paths are absolute. Absent for older installs; reinstall to record provenance.
    #[serde(rename = "installed_source", skip_serializing_if = "Option::is_none")]
    pub installed_source: Option<serde_json::Value>,
    /// Marketplace the plugin came from (empty string for direct repo installs)
    pub marketplace: String,
    /// Plugin name
    pub name: String,
    /// Source descriptor for direct repo installs (when marketplace is empty)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<serde_json::Value>,
    /// Per-plugin source fingerprint (a SHA-256 hash of the plugin's catalog source spec plus its resolved source subtree — NOT a Git commit SHA) captured at marketplace install/update time. Auto-update compares it against the freshly recomputed fingerprint to detect a content change that does not bump the version. Absent for pre-existing installs and for direct (non-marketplace) installs.
    #[serde(rename = "source_sha", skip_serializing_if = "Option::is_none")]
    pub source_sha: Option<String>,
    /// Installed version, if known
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Source descriptor for a direct GitHub plugin install, with `owner/repo`, optional ref or full commit SHA, and optional subpath.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstalledPluginSourceGitHub {
    /// Optional repository-relative path to the plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Optional Git ref to resolve.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#ref: Option<String>,
    /// GitHub repository in `owner/repo` form.
    pub repo: String,
    /// Optional full 40-character hexadecimal commit SHA.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sha: Option<String>,
    /// Constant value. Always "github".
    pub source: SessionInstalledPluginSourceGitHubSource,
}

/// Source descriptor for a direct local plugin install, with a local filesystem path.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstalledPluginSourceLocal {
    /// Local filesystem path to the plugin.
    pub path: String,
    /// Constant value. Always "local".
    pub source: SessionInstalledPluginSourceLocalSource,
}

/// Source descriptor for a direct URL plugin install, with URL, optional ref or full commit SHA, and optional subpath.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstalledPluginSourceUrl {
    /// Optional source-relative path to the plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Optional Git ref to resolve.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#ref: Option<String>,
    /// Optional full 40-character hexadecimal commit SHA.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sha: Option<String>,
    /// Constant value. Always "url".
    pub source: SessionInstalledPluginSourceUrlSource,
    /// URL of the plugin source.
    pub url: String,
}

/// Baseline data provenance for a prediction.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionLimitPredictionBaselineData {
    /// End of the baseline data slice.
    pub window_end: String,
    /// Start of the baseline data slice.
    pub window_start: String,
}

/// Semantic usage tier and its AI-credit cap.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionLimitPredictionTierOption {
    /// AI-credit cap for this tier.
    pub cap: f64,
    /// Semantic usage tier.
    pub tier: SessionLimitPredictionTier,
}

/// Explainable AI-credit session-limit prediction.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionLimitPredictionDetails {
    /// Baseline data provenance.
    pub baseline_data: SessionLimitPredictionBaselineData,
    /// Client population used for the prediction.
    pub client_type: SessionLimitPredictionClientType,
    /// Resolved model family when known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub family: Option<String>,
    /// Model identifier used for lookup.
    pub model_id: String,
    /// Recommended maximum AI credits for this session.
    pub recommended_cap: f64,
    /// Tier chosen as the recommended cap.
    pub recommended_tier: SessionLimitPredictionTier,
    /// Baseline fallback level used to create the prediction.
    pub source: SessionLimitPredictionSource,
    /// Key matched at the source level, such as a model id, family id, or `global`.
    pub source_key: String,
    /// Ordered usage tiers and their AI-credit caps.
    pub tiers: Vec<SessionLimitPredictionTierOption>,
}

/// Parameters for predicting an AI-credit session limit. Omitting `modelId` uses the session's currently selected model.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionLimitPredictionRequest {
    /// Client type to size for. Defaults to `cli-interactive`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_type: Option<SessionLimitPredictionClientType>,
    /// Optional model identifier override. If omitted, the session's current model is used.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionLimitPredictionResultAvailable {
    /// Prediction result variant discriminator.
    pub kind: SessionLimitPredictionResultAvailableKind,
    /// Predicted session limit details.
    pub prediction: SessionLimitPredictionDetails,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionLimitPredictionResultUnavailable {
    /// Prediction result variant discriminator.
    pub kind: SessionLimitPredictionResultUnavailableKind,
    /// Reason no prediction is available.
    pub reason: SessionLimitPredictionUnavailableReason,
}

/// Sessions matching the filter, ordered most-recently-modified first.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionList {
    /// Sessions ordered most-recently-modified first. Discriminated by `isRemote`.
    pub sessions: Vec<serde_json::Value>,
}

/// Optional filter applied to the returned sessions
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionListFilter {
    /// Match sessions whose context.branch equals this value
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Match sessions whose context.cwd equals this value
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Match sessions whose context.gitRoot equals this value
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Match sessions whose context.repository equals this value
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
}

/// Queued repo-level startup prompts and the total hook command count after loading.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionLoadDeferredRepoHooksResult {
    /// Total hook command count (user + plugin + repo) loaded for the session by this call. Captured atomically with startupPrompts so callers don't need to read a separate counter.
    pub hook_count: i64,
    /// Repo-level startup prompts queued from repo hook configs. Empty on resume, when no repo configs were pending, or when disableAllHooks is set.
    pub startup_prompts: Vec<String>,
}

/// Enterprise permission policy expressed with the runtime's managed permission-rule syntax.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionManagedPermissions {
    /// Permission rules that allow matching operations unless another managed source, deny, or ask rule restricts them.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow: Option<Vec<String>>,
    /// Permission rules that require explicit human approval.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ask: Option<Vec<String>>,
    /// Permission rules that block matching operations. Deny has highest precedence.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deny: Option<Vec<String>>,
    /// When true, prevents Assisted Permissions from being activated. An actively Assisted session falls back to Manual Approval while the policy is in force. Omit the key or set it to false to impose no restriction.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_assisted_permissions_mode: Option<bool>,
    /// When set to `disable`, prevents bypass/allow-all permission modes. Advisory auto-approval remains available because normal prompt paths stay active. Any other value is accepted rather than failing the session, but is enforced as `disable`: the key is only present to restrict something, so a mode this runtime cannot interpret fails closed to the most restrictive one it knows. Omit the key entirely to impose no restriction.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_bypass_permissions_mode: Option<String>,
    /// Closed-world host boundary expressed as `Domain(hostname)`, `Domain(IP)`, or `Domain(*.example.com)` rules. Schemes, ports, paths, queries, and fragments are rejected because every network request must be enforceable at host-level egress. Multiple managed sources intersect their lists; an empty list denies all hosts.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limit_to: Option<Vec<String>>,
}

/// Managed settings an SDK host may inject at session startup. Only permissions are accepted in this initial contract.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionManagedSettings {
    /// Managed permission policy injected by the SDK host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permissions: Option<SessionManagedPermissions>,
}

/// Records which IDE the host is connected to, or clears it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpSetConnectedIdeInfoParams {
    /// The connected IDE. Null or omitted clears the recorded IDE, which is how a host reports that it is disconnected.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ide: Option<SessionConnectedIdeInfo>,
}

/// Public-facing projection of workspace metadata for SDK / TUI consumers
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataSnapshotWorkspace {
    /// Branch checked out at session start, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// ISO 8601 timestamp when the workspace was created
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    /// Current working directory at session start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Resolved git root for cwd, if any
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Repository host type, if known
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspaceSummaryHostType>,
    /// Workspace identifier (1:1 with sessionId)
    pub id: String,
    /// Display name for the session, if set
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Repository identifier in 'owner/repo' or 'org/project/repo' format, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// ISO 8601 timestamp when the workspace was last updated
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    /// Whether the display name was explicitly set by the user
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Point-in-time snapshot of slow-changing session identifier and state fields
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataSnapshot {
    /// True when the session was detected to be in use by another process at construction time. Local consumers may surface a confirmation prompt before fully attaching. Always false for new sessions.
    pub already_in_use: bool,
    /// Runtime client name associated with the session (telemetry identifier).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot')
    pub current_mode: MetadataSnapshotCurrentMode,
    /// Live indexed-search state for this session activation. Omitted by runtimes that do not expose indexed-search status; absence does not indicate enablement.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub indexed_search: Option<IndexedSearchState>,
    /// User-provided name supplied at session construction (via `--name`), if any. Immutable after construction.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initial_name: Option<String>,
    /// Whether this is a remote session (i.e., one whose runtime executes elsewhere and is steered through this process)
    pub is_remote: bool,
    /// ISO 8601 timestamp of when the session's persisted state was last modified on disk. For new sessions, equals startTime. For resumed sessions, reflects the previous modification time at construction.
    pub modified_time: String,
    /// Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are immutable for the lifetime of the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_metadata: Option<MetadataSnapshotRemoteMetadata>,
    /// Currently selected model identifier, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_model: Option<String>,
    /// The unique identifier of the session
    pub session_id: SessionId,
    /// Current session limits, or null when no limits are active
    pub session_limits: Option<SessionLimitsConfig>,
    /// ISO 8601 timestamp of when the session started
    pub start_time: String,
    /// Short human-readable summary of the session, if known. Omitted when no summary has been generated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    /// Absolute path to the session's current working directory
    pub working_directory: String,
    /// Public-facing workspace metadata for this session, or null if the session has no associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count, internal flags).
    pub workspace: Option<SessionMetadataSnapshotWorkspace>,
    /// Absolute path to the session's workspace directory on disk, or null if the session has no associated workspace
    pub workspace_path: Option<String>,
}

/// Cost-category metadata for a CAPI model.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelPriceCategory {
    /// CAPI model identifier.
    pub id: String,
    /// Cost category assigned to the model.
    pub price_category: ModelPickerPriceCategory,
}

/// The list of models available to this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelList {
    /// Ordered Auto routing preferences discovered for this session's account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto: Option<AutoTierMetadata>,
    /// Available models, ordered with the most preferred default first. Includes both Copilot (CAPI) models and any registry BYOK models; a BYOK model appears under its provider-qualified selection id (`provider/id`).
    pub list: Vec<serde_json::Value>,
    /// Cost categories for the full CAPI catalog, including picker-disabled models that Auto may select. Metadata only; entries absent from `list` are not manually selectable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_price_categories: Option<Vec<SessionModelPriceCategory>>,
    /// The model providers available to this session, in ordering order, resolved from the account roster; empty when no provider is entitled (logged out / seatless). Each model in `list` carries its own provider reference; this roster gives the deterministic provider sequence and lets a consumer group by provider without deriving ordering from the model list. Central code never branches on a provider kind.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub providers: Option<Vec<ModelProviderDescriptor>>,
    /// Per-quota snapshots returned alongside the model list, keyed by quota type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub quota_snapshots: Option<HashMap<String, serde_json::Value>>,
}

/// Source descriptor for a `sessions.open` content-exclusion rule, with source name and type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionOpenOptionsAdditionalContentExclusionPolicyRuleSource {
    /// Name of the policy source.
    pub name: String,
    /// Type of the policy source.
    pub r#type: String,
}

/// Single content-exclusion rule supplied to `sessions.open` options, with paths, match conditions, and source.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionOpenOptionsAdditionalContentExclusionPolicyRule {
    /// Conditions of which at least one must match.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub if_any_match: Option<Vec<String>>,
    /// Conditions none of which may match.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub if_none_match: Option<Vec<String>>,
    /// Path patterns covered by this rule.
    pub paths: Vec<String>,
    /// Source descriptor for a `sessions.open` content-exclusion rule, with source name and type.
    pub source: SessionOpenOptionsAdditionalContentExclusionPolicyRuleSource,
}

/// Content-exclusion policy supplied to `sessions.open` options, with rules, last-updated data, and scope.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionOpenOptionsAdditionalContentExclusionPolicy {
    /// Opaque policy update timestamp supplied by the host.
    #[serde(rename = "last_updated_at")]
    pub last_updated_at: serde_json::Value,
    /// Content-exclusion rules to apply.
    pub rules: Vec<SessionOpenOptionsAdditionalContentExclusionPolicyRule>,
    /// Allowed values for the `SessionOpenOptionsAdditionalContentExclusionPolicyScope` enumeration.
    pub scope: SessionOpenOptionsAdditionalContentExclusionPolicyScope,
}

/// Session construction options.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionOpenOptions {
    /// Additional content-exclusion policies to merge into the session policy set.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_content_exclusion_policies:
        Option<Vec<SessionOpenOptionsAdditionalContentExclusionPolicy>>,
    /// Additional directories the agent may access beyond the working directory. Each entry is granted to the session's file-access allow-list and surfaced to the model (system prompt context and `@`-mention completion). Conventional `.github/skills/` and `.github/agents/` definitions under each directory also join the session's project catalogs when their existing subsystem gates are enabled: added-root skills require both `enableConfigDiscovery` and effective `enableSkills`; added-root agents require `enableConfigDiscovery`. Supplying a directory therefore activates configuration from it and should be treated as a trust decision. Absolute paths are recommended; a relative path is resolved against the session's working directory. Nonexistent or unresolvable entries are skipped with a warning. This is applied during session creation and cold resume and is not persisted, so a cold resume must re-supply the directories.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_directories: Option<Vec<String>>,
    /// Runtime context discriminator for agent filtering.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_context: Option<String>,
    /// Explicit discovery-only agent roots, separate from customAgents and filesystem grants. Available without enableConfigDiscovery, subject to managed/source disables. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_directories: Option<Vec<String>>,
    /// Whether to include instructions from every MCP server in the system prompt instead of only allowlisted servers.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow_all_mcp_server_instructions: Option<bool>,
    /// Whether ask_user is explicitly disabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ask_user_disabled: Option<bool>,
    /// OAuth Client ID Metadata Document URL used by this host for MCP authorization.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_client_id_metadata_url: Option<String>,
    /// Initial authentication info for the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_info: Option<AuthInfo>,
    /// Autopilot policy. Defaults to runtime-owned continuation with at most five automatic continuations. Does not change the session mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub autopilot: Option<AutopilotOptions>,
    /// Whether a CLI host explicitly requested the initial Auto preference. False preserves a settings-derived preference without validating availability during creation; execution still validates it. Defaults to true and is ignored for non-CLI callers.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) auto_tier_is_explicit: Option<bool>,
    /// Allowlist of available tool names.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available_tools: Option<Vec<String>>,
    /// Options scoped to the built-in CAPI (Copilot API) provider.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub capi: Option<CapiSessionOptions>,
    /// Structured client kind used for runtime behavior gates.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_kind: Option<String>,
    /// Identifier of the client driving the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Whether commit-message coauthor trailers are enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub coauthor_enabled: Option<bool>,
    /// Override Copilot configuration directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<String>,
    /// Whether auto-mode continuation is enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub continue_on_auto_mode: Option<bool>,
    /// Override URL for the Copilot API endpoint.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_url: Option<String>,
    /// Whether custom agents default to local-only execution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub custom_agents_local_only: Option<bool>,
    /// Parent engagement ID for detached child telemetry rollup.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detached_from_spawning_parent_engagement_id: Option<String>,
    /// Parent session ID for detached child telemetry rollup.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detached_from_spawning_parent_session_id: Option<String>,
    /// Instruction source IDs disabled for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_instruction_sources: Option<Vec<String>>,
    /// MCP server names disabled for this session. Disabled servers are not started or authenticated on create or cold resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_mcp_servers: Option<Vec<String>>,
    /// Skill IDs disabled for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_skills: Option<Vec<String>>,
    /// Experimental: enable native model citations for supported Anthropic and OpenAI models, normalized onto the `assistant.message` event. Off by default; may change or be removed while the citations surface is experimental.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_citations: Option<bool>,
    /// Opt in to capturing file changes for session rewind and session diff. Capture cannot reconstruct changes made before it was enabled. On create it starts capture from the first turn. It is also honored on resume: for a session that already has tracked prior turns, tracking continues automatically even if this is omitted; passing it on resume additionally enables tracking for an eligible session that has no prior root turn yet. Resuming a session whose prior root turns were never tracked has no restorable baseline, so tracking stays disabled for it and rewind reports file change tracking as unavailable; the resume itself still succeeds, so sessions that predate tracking remain loadable. The opt-in is only rejected when the session can never track (a subagent session, or one without local session storage). It is intentionally absent from the mutable options update because enabling it after edits have occurred would create an incomplete, misleading baseline. Subagents share the parent session's capture store and are not tracked as separate rewind points: a file a subagent writes is attributed to whichever root user turn was open when the capture was staged, just before the tool body ran. A turn cannot open while a staged capture is still in flight, so a subagent tool that staged under the spawning turn stays attributed to it however late the write lands, while a capture it stages after the user's next message belongs to that later turn. Attribution decides which turn's rewind point counts and file preview include that write; it does not narrow which rewinds revert it, because a rewind restores every capture from the selected turn onward, so the earlier spawning turn reverts it as well.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_file_change_tracking: Option<bool>,
    /// Live gate for repository-origin file hooks, including the primary repository, trusted repository-configured roots, and accepted startup additional directories (including CLI --add-dir). False suppresses these hooks; explicit hookDirectories and SDK callbacks are independent. Additional-root eligibility and the DISABLE_ADDITIONAL_DIRECTORY_HOOKS escape hatch are captured at create/cold resume, not expanded by later grants or feature-flag changes. Discovery uses containing Git roots, so supply only trusted repositories: commands and prompts need no further approval. Policy, hook-disable settings, and sandbox rules still apply. Provider-backed sessions skip repository hooks with a warning on omission, skip silently on false, and reject explicit true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_file_hooks: Option<bool>,
    /// Opt-in: self-fetch and enforce enterprise managed settings, including managed hook policies, at session bootstrap.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_managed_settings: Option<bool>,
    /// Whether on-demand custom instruction discovery is enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_on_demand_instruction_discovery: Option<bool>,
    /// Deprecated and ignored. Shell command analysis is always enabled; retained for wire compatibility.
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_script_safety: Option<bool>,
    /// Whether skill loading is enabled. When omitted, explicit SDK skill roots or an SDK skill provider enable skills independently of enableConfigDiscovery.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_skills: Option<bool>,
    /// Whether model responses stream as delta events.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_streaming: Option<bool>,
    /// Opt in to enforcing non-overridable managed model controls on session model, Auto-tier, reasoning-effort, and context-tier changes. Managed defaults still apply when omitted; this option only turns conflicting changes into errors.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enforce_managed_model_defaults: Option<bool>,
    /// How MCP server environment values are interpreted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env_value_mode: Option<SessionOpenOptionsEnvValueMode>,
    /// Override directory for session event logs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub events_log_directory: Option<String>,
    /// Whether subagent callback events should be forwarded into the session event log sink.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub events_log_includes_subagents: Option<bool>,
    /// Built-in subagent names to exclude from this session. Excluded built-ins are hidden from agent discovery and cannot be dispatched unless a custom agent with the same name is available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub excluded_builtin_agents: Option<Vec<String>>,
    /// Managed-setting paths this trusted SDK host enforces externally or does not support, excluded from fetched, device, and policy-helper policy for this session. Exact dotted paths exclude a setting and its descendants (for example sandbox or permissions.disableBypassPermissionsMode); wildcards and array indices are not supported. Host-injected managedSettings and policy availability checks are not excluded. Not persisted; re-supply on resume, where omission restores full enforcement. Requires enableManagedSettings to self-fetch policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub excluded_managed_settings: Option<Vec<String>>,
    /// Denylist of tool names.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub excluded_tools: Option<Vec<String>>,
    /// ExP assignment ('flight') data injected by an SDK integrator, in the same JSON shape the Copilot CLI fetches from the experimentation service (CopilotExpAssignmentResponse). When supplied this is fed into the FeatureFlagService exactly like CLI-fetched assignments and ExP-backed flags wait for it. When absent the session does not block on ExP.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) exp_assignments: Option<serde_json::Value>,
    /// Feature-flag values resolved by the host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feature_flags: Option<HashMap<String, bool>>,
    /// Whether the requesting SDK session has a skill provider. The provider remains ephemeral and is never persisted in session options or history. When enableSkills is false, it remains bound but dormant and receives no callbacks. Cloud, relay, handoff, and raw sessions.open flows reject it because they cannot safely pre-register the callback handler.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) has_skill_provider: Option<bool>,
    /// Explicit discovery-only file-hook roots, separate from callback hooks and filesystem grants. Available without enableConfigDiscovery or enableFileHooks, which controls repository-origin hooks only; hook/managed disables still apply. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub hook_directories: Option<Vec<String>>,
    /// Skill scan directories and descendants excluded from discovery. Supports `~`-relative paths.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ignored_skills_locations: Option<Vec<String>>,
    /// Built-in subagent names to include in this session. When specified, only these built-ins are available, subject to runtime availability and exclusions. Custom agents with the same name remain available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub included_builtin_agents: Option<Vec<String>>,
    /// Built-in skill names to include in this session. When specified, only these runtime-bundled skills are available. Skills from other sources with the same name remain available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub included_builtin_skills: Option<Vec<String>>,
    /// Installed plugins visible to the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub installed_plugins: Option<Vec<InstalledPlugin>>,
    /// Explicit discovery-only instruction roots using existing SDK instruction formats, not filesystem grants. Available without enableConfigDiscovery, subject to instruction/managed disables. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub instruction_directories: Option<Vec<String>>,
    /// Stable integration identifier for analytics.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub integration_id: Option<String>,
    /// Declares that a human drives this session's conversation, enabling user-message analysis (sentiment, task completion criteria, and task analytics) for user-authored messages. Automated messages remain excluded. Defaults to false for SDK sessions. Applied when the session is created or loaded from storage; ignored when resuming a session that is already active, and for cloud sessions.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interactive_user: Option<bool>,
    /// Whether experimental behavior is enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_experimental_mode: Option<bool>,
    /// Startup-only experimental runtime lifecycle diagnostics configuration.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) lifecycle_diagnostics: Option<LifecycleDiagnosticsConfiguration>,
    /// Whether interactive shell sessions are logged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub log_interactive_shells: Option<bool>,
    /// Identifier sent to LSP-style integrations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub lsp_client_name: Option<String>,
    /// Non-secret host-managed HTTP MCP servers keyed by stable managed identity. Managed provenance is runtime-established from this separate field and credentials are supplied through dynamic-header refresh.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed_mcp_servers: Option<HashMap<String, ManagedMcpServerConfig>>,
    /// Permissions-only enterprise policy injected by the SDK host at session create or resume. Composes restrictively with self-fetched and device policy and is not persisted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub managed_settings: Option<SessionManagedSettings>,
    /// Maximum decoded byte size of a single inline model-facing binary tool result persisted in session events (default 10 MB).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_inline_binary_bytes: Option<i64>,
    /// Memory configuration for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub memory: Option<MemoryConfiguration>,
    /// Initial model identifier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Initial model capability overrides.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities_overrides: Option<ModelCapabilitiesOverride>,
    /// BYOK model definitions added to the selectable model list, each referencing a provider name.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub models: Option<Vec<ProviderModelConfig>>,
    /// Optional human-friendly session name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Custom model-provider configuration (BYOK).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ProviderConfig>,
    /// Named BYOK provider connections, additive to CAPI auth. Combining with `provider` is rejected.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub providers: Option<Vec<NamedProviderConfig>>,
    /// Initial reasoning effort level. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. When omitted, no effort override is applied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Initial reasoning summary mode for supported model clients.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summary: Option<SessionOpenOptionsReasoningSummary>,
    /// Whether to invalidate cached custom-instruction discovery before constructing the session. Use when instruction files may have changed earlier in the same runtime process.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub refresh_custom_instructions: Option<bool>,
    /// Telemetry-only remote-defaulted flag.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_defaulted_on: Option<bool>,
    /// Telemetry-only remote exporting flag.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_exporting: Option<bool>,
    /// Whether this session supports remote steering.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
    /// Whether the host is an interactive UI.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub running_in_interactive_mode: Option<bool>,
    /// Resolved sandbox configuration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_config: Option<SandboxConfig>,
    /// Origin of the sandbox choice. Settings-derived origins (never_configured, user_enabled, user_disabled, repository_policy) let managed policy floor a host preference; explicit below-floor changes remain policy conflicts unless a session opt-out is authorized. Also used for telemetry provenance.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_config_source: Option<SandboxConfigSource>,
    /// Capabilities enabled for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_capabilities: Option<Vec<SessionCapability>>,
    /// Optional stable session identifier to use for a new session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// Initial session limits.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_limits: Option<SessionLimitsConfig>,
    /// Per-session settings for built-in shell tools.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell: Option<ShellOptions>,
    /// Use shell.initProfile instead. Shell init profile.
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell_init_profile: Option<String>,
    /// PowerShell process flags applied to built-in and user-requested shell commands.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell_process_flags: Option<Vec<String>>,
    /// Explicit discovery-only skill roots, not filesystem grants. Available without enableConfigDiscovery, subject to skill/managed disables. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_directories: Option<Vec<String>>,
    /// Whether to skip custom instruction sources.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_custom_instructions: Option<bool>,
    /// Optional trajectory output file path.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub trajectory_file: Option<String>,
    /// Initial output verbosity level for supported models.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub verbosity: Option<Verbosity>,
    /// Working directory to anchor the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
    /// Pre-resolved working-directory context for session startup.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory_context: Option<SessionContext>,
}

/// Parameters for creating a new local session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenCreate {
    /// Whether to emit session.start during creation. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub emit_start: Option<bool>,
    /// Create a new local session.
    pub kind: SessionsOpenCreateKind,
    /// Session construction options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<SessionOpenOptions>,
}

/// Parameters for resuming a specific local session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenResume {
    /// Resume a specific local session by ID or prefix.
    pub kind: SessionsOpenResumeKind,
    /// Session resume options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<SessionOpenOptions>,
    /// Whether to emit session.resume after loading. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resume: Option<bool>,
    /// Session ID or unique prefix to resume.
    pub session_id: SessionId,
    /// Suppress workspace.yaml metadata writeback when resuming from an incidental cwd.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_resume_workspace_metadata_writeback: Option<bool>,
}

/// Parameters for resuming the most relevant local session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenResumeLast {
    /// Working-directory context used to choose the most relevant session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<SessionContext>,
    /// Resume the most relevant existing local session.
    pub kind: SessionsOpenResumeLastKind,
    /// Session resume options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<SessionOpenOptions>,
    /// Suppress workspace.yaml metadata writeback when resuming from an incidental cwd.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_resume_workspace_metadata_writeback: Option<bool>,
}

/// Parameters for attaching to an already-active session by ID.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenAttach {
    /// Attach to an already-active in-process session by ID. Unlike `resume`, this does NOT re-load from disk; the session must already be loaded by an earlier `create`/`resume` call. Returns `status: 'not_found'` when no active session matches the id. Useful for in-process consumers that need a fresh API handle to a session opened elsewhere (e.g., a peer foreground-session switch).
    pub kind: SessionsOpenAttachKind,
    /// Session ID to attach to.
    pub session_id: SessionId,
}

/// Parameters for connecting to a live remote session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenRemote {
    /// Connect to a live remote session.
    pub kind: SessionsOpenRemoteKind,
    /// Session options for the connection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<SessionOpenOptions>,
    /// Remote session identifier to connect to.
    pub remote_session_id: SessionId,
    /// Repository context for the remote session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<RemoteSessionRepository>,
}

/// Parameters for creating a new cloud session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenCloud {
    /// Create a new cloud (coding-agent) session.
    pub kind: SessionsOpenCloudKind,
    /// In-process callback invoked when the cloud task is created, before connection. Internal because function references cannot cross the JSON-RPC boundary.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) on_task_created: Option<serde_json::Value>,
    /// Session options for cloud session creation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<SessionOpenOptions>,
    /// Optional owner (user or organization login) to associate with the cloud session when no repository is provided. Ignored when `repository` is set (the repo's owner takes precedence).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub owner: Option<String>,
    /// Repository for the cloud session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<RemoteSessionRepository>,
}

/// Parameters for fetching a remote session and handing it off to a new local session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenHandoff {
    /// Fetch a remote session and hand it off to a new local session.
    pub kind: SessionsOpenHandoffKind,
    /// Remote session metadata for the session to hand off (typically obtained from `sessions.list` with `source: "remote"`).
    pub metadata: RemoteSessionMetadataValue,
    /// In-process confirmation callback `(request) => boolean | Promise<boolean>` invoked when the handoff needs the caller to confirm a non-fatal blocker (e.g. a repository mismatch between the current working directory and the remote session). Returning `true` proceeds with the handoff; returning `false` (or omitting the callback) aborts it. Marked internal because a function reference cannot cross the JSON-RPC boundary, for the same reasons as `onProgress`.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) on_confirm: Option<serde_json::Value>,
    /// In-process progress callback `(update) => void` invoked for each handoff step. Marked internal because a function reference cannot cross the JSON-RPC boundary. The host-side `handoffSession` is already declared as `AsyncGenerator<HandoffProgress, HandoffResult>`; the schema layer flattens it because it does not yet support streaming methods. The wire-clean replacement is to expose the AsyncGenerator directly (or use vscode-jsonrpc `$/progress` notifications) once the schema/transport layer supports it.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) on_progress: Option<serde_json::Value>,
    /// Session construction options for the new local session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<SessionOpenOptions>,
    /// Task type determines the handoff strategy (CCA fetches events; CLI prepares a transient session).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task_type: Option<SessionsOpenHandoffTaskType>,
}

/// `sessions.open` handoff progress update with step, status, and optional message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenProgress {
    /// Optional step message.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Step status.
    pub status: SessionsOpenProgressStatus,
    /// Handoff step.
    pub step: SessionsOpenProgressStep,
}

/// Result of opening a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionOpenResult {
    /// Remote session metadata, present when status is `connected`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<RemoteSessionMetadataValue>,
    /// Handoff progress steps, present when status is `handed_off`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub progress: Option<Vec<SessionsOpenProgress>>,
    /// Remote session ID, present when status is `connected`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_session_id: Option<SessionId>,
    /// In-process SessionClientApi handle for the opened session, returned to CLI callers as a transitional shortcut. Marked internal so the public SDK surface does not expose it; SDK consumers should construct per-session clients from `sessionId` instead.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) session_api: Option<serde_json::Value>,
    /// Opened session ID. Omitted when status is `not_found`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// Startup prompts queued by user-level hook configs at session creation. Only populated when status is `created`; resumed sessions return an empty array.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub startup_prompts: Option<Vec<String>>,
    /// Outcome of the open request.
    pub status: SessionsOpenStatus,
}

/// Plugin names (or specs) to disable in the session's authoritative working directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsDisableRequest {
    /// Plugin names or "plugin@marketplace" specs to disable. Unknown names are ignored. Non-marketplace direct installs cannot be disabled via this API; uninstall them instead. Plugin-owned MCP servers are stopped in active sessions immediately; other plugin contributions remain available until each session reloads plugins.
    pub names: Vec<String>,
}

/// Plugin names (or specs) to enable in the session's authoritative working directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsEnableRequest {
    /// Plugin names or "plugin@marketplace" specs to enable. Unknown names are ignored. Non-marketplace direct installs are always enabled and cannot be toggled via this API.
    pub names: Vec<String>,
}

/// Plugin source resolved relative to the session's authoritative working directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsInstallRequest {
    /// Plugin install spec. Accepts the same forms as the CLI: "plugin@marketplace" (marketplace install), "owner/repo" or "owner/repo:subpath" (GitHub direct), an http/https/ssh URL, or a local path. Direct (non-marketplace) installs are deprecated and will produce a deprecationWarning in the result.
    pub source: String,
}

/// Plugins required by the session's managed settings to install or update again.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsRetryManagedRequest {
    /// "plugin@marketplace" specs to retry. Omit to retry every plugin the session's managed settings require.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugins: Option<Vec<String>>,
}

/// Per-plugin outcomes of retrying the plugins required by the session's managed settings.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsRetryManagedResult {
    /// One entry per requested plugin, in request order. When no plugins were requested, one entry per plugin the managed settings require.
    pub plugins: Vec<ManagedPluginRetryEntry>,
}

/// Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPruneResult {
    /// Session IDs that would be deleted in dry-run mode (always empty otherwise)
    pub candidates: Vec<String>,
    /// Session IDs that were deleted (always empty in dry-run mode)
    pub deleted: Vec<String>,
    /// True when no deletions were actually performed
    pub dry_run: bool,
    /// Total bytes freed (actual when not dry-run, projected when dry-run)
    pub freed_bytes: i64,
    /// Session IDs that were skipped (e.g., named sessions)
    pub skipped: Vec<String>,
}

/// Existing delegation guidance for an account without delegation access.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQuotaDelegateWarning {
    /// User-facing guidance.
    pub text: String,
    /// Link to upgrade the account.
    pub url: String,
}

/// Session quota snapshot, preserving the incumbent numeric units and reset metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQuotaSnapshot {
    /// Entitled quantity in this quota's units.
    pub entitlement_requests: f64,
    /// Whether the provider reports available quota.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub has_quota: Option<bool>,
    /// Whether the entitlement is unlimited.
    pub is_unlimited_entitlement: bool,
    /// Additional usage in this quota's units.
    pub overage: f64,
    /// Whether additional usage is allowed after quota exhaustion.
    pub overage_allowed_with_exhausted_quota: bool,
    /// Additional-usage budget cap, when provided.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub overage_entitlement: Option<f64>,
    /// Percentage of the entitlement remaining.
    pub remaining_percentage: f64,
    /// Quota reset time in milliseconds since the Unix epoch, when known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reset_date_epoch_ms: Option<f64>,
    /// Whether the reset time is estimated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reset_date_estimated: Option<bool>,
    /// Whether this quota uses token-based billing.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token_based_billing: Option<bool>,
    /// Whether usage is allowed after quota exhaustion.
    pub usage_allowed_with_exhausted_quota: bool,
    /// Consumed quantity in this quota's units.
    pub used_requests: f64,
}

/// The incumbent session-owned quota and account projection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQuotaProjection {
    /// Whether the account can sign up for Copilot Free.
    pub can_signup_for_copilot_free: bool,
    /// Whether the account can delegate tasks to GitHub.
    pub delegate_available: bool,
    /// Existing delegation warning for a free account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub delegate_warning: Option<SessionQuotaDelegateWarning>,
    /// Whether dynamic workflows are enabled for the active account.
    pub dynamic_workflows_enabled: bool,
    /// Whether dynamic workflows are visible under the session's feature flags.
    pub dynamic_workflows_ui_visible: bool,
    /// Whether the account uses the free limited Copilot plan.
    pub is_free_user: bool,
    /// Whether the account uses token-based billing.
    pub is_tbb_user: bool,
    /// Whether model cost columns should be shown.
    pub model_cost_column_visible: bool,
    /// Resolved Copilot plan tier.
    pub plan_tier: SessionQuotaPlanTier,
    /// Whether premium-request costs are billable.
    pub premium_requests_billable: bool,
    /// Provider-owned account quota state, including snapshots without numeric balances.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_quotas: Option<Vec<ProviderQuotaState>>,
    /// Known quota snapshots, keyed by the provider's quota type.
    pub snapshots: HashMap<String, SessionQuotaSnapshot>,
    /// Upgrade link for a free account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub upgrade_url: Option<String>,
}

/// Session IDs to close, deactivate, and delete from disk.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsBulkDeleteRequest {
    /// Session IDs to close, deactivate, and delete from disk
    pub session_ids: Vec<String>,
}

/// Session IDs to test for live in-use locks.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsCheckInUseRequest {
    /// Session IDs to test for live in-use locks
    pub session_ids: Vec<String>,
}

/// Session IDs from the input set that are currently in use by another process.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsCheckInUseResult {
    /// Session IDs from the input set that are currently held by another running process via an alive lock file
    pub in_use: Vec<String>,
}

/// Session ID to close.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsCloseRequest {
    /// Session ID to close
    pub session_id: SessionId,
}

/// Closes a session: emits shutdown, flushes pending events to disk, releases the in-use lock, disposes the active session. Idempotent: succeeds even if the session is not currently active.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsCloseResult {}

/// A working-directory context together with the client that produced it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkingDirectoryContextWithClient {
    /// Merge-base commit SHA
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_commit: Option<String>,
    /// Current git branch name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Name of the client that created the session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Current working directory path
    pub cwd: String,
    /// Root directory of the git repository
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Head commit of the current git branch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub head_commit: Option<String>,
    /// Hosting platform type of the repository
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_type: Option<String>,
    /// Repository identifier derived from the git remote URL
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Raw host string from the git remote URL
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository_host: Option<String>,
}

/// Identity, state location and starting context for a workspace record.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsCreateWorkspaceRequest {
    /// Starting working-directory context. The record keeps `cwd`, `gitRoot`, `repository`, `hostType`, `branch`, and `clientName`. Other fields, including `repositoryHost`, `headCommit`, and `baseCommit`, are ignored. `hostType` must be `github` or `ado`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<SessionWorkingDirectoryContextWithClient>,
    /// `windows` (any letter case) selects Windows path rules. Any other value selects POSIX path rules.
    pub convention: String,
    /// User-supplied display name for the workspace
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Session ID the workspace record belongs to
    pub session_id: SessionId,
    /// Directory the session's state is written under when no session filesystem provider is configured. Ignored when a provider is configured; the provider's session state path is used instead.
    pub session_state_path: String,
}

/// The workspace record that was written.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsCreateWorkspaceResult {
    /// The created workspace record, as JSON
    pub workspace_json: String,
}

/// Session ID to delete from disk.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsDeleteRequest {
    /// Session ID to delete
    pub session_id: SessionId,
    /// Internal resolved session directory path to delete
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_path: Option<String>,
}

/// Session metadata records to enrich with summary and context information.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsEnrichMetadataRequest {
    /// Session metadata records to enrich. Records that already have summary and context are returned unchanged.
    pub sessions: Vec<LocalSessionMetadataValue>,
}

/// Token authentication accepted by session.gitHubAuth.setCredentials.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SettableTokenAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Authentication host.
    pub host: String,
    /// The token value itself. Treat as a secret.
    pub token: String,
    /// SDK-side token authentication; the host configured the token directly via the SDK.
    pub r#type: SettableTokenAuthInfoType,
}

/// New auth credentials to install on the session. Omit to leave credentials unchanged.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSetCredentialsParams {
    /// The new auth credentials to install on the session. When omitted or `undefined`, the call is a no-op and the session's existing credentials are preserved. The runtime installs the supplied value immediately for outbound model/API requests. When the credential carries a raw token (`token`, `env`, or `gh-cli`) but no `copilotUser`, the runtime additionally re-resolves `copilotUser` server-side (best-effort, asynchronously, after the synchronous install) so plan/quota/billing metadata regains fidelity; on resolution failure the verbatim credential remains installed. It does NOT otherwise validate the credential. Several variants carry secret material; treat this method's params as containing secrets at rest and in transit.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub credentials: Option<SettableAuthInfo>,
}

/// Indicates whether the credential update succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSetCredentialsResult {
    /// Whether the session ended up with a populated `copilotUser` for the installed credentials. `true` when the supplied credential already carried `copilotUser` or it was successfully re-resolved server-side. `false` when the credential is installed without `copilotUser` — either re-resolution failed, or the variant cannot be re-resolved from the credential alone (only the raw-token variants `token`, `env`, and `gh-cli` can). In both `false` cases the token swap still applied, but plan/quota/billing metadata is degraded. Present whenever a credential was supplied; omitted only when no credential was supplied (no-op call).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user_resolved: Option<bool>,
    /// Whether the operation succeeded
    pub success: bool,
}

/// Availability of built-in job tools surfaced to boundary consumers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSettingsBuiltInToolAvailabilitySnapshot {
    /// Whether the create-pull-request tool is available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub create_pull_request: Option<bool>,
    /// Whether the report-progress tool is available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub report_progress: Option<bool>,
}

/// Named Rust-owned settings predicate to evaluate for this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSettingsEvaluatePredicateRequest {
    /// Predicate name. The runtime owns the raw feature-flag names and composition logic.
    pub name: SessionSettingsPredicateName,
    /// Tool name for tool-scoped predicates such as trivial-change handling.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_name: Option<String>,
}

/// Result of evaluating a Rust-owned settings predicate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSettingsEvaluatePredicateResult {
    /// Whether the named settings predicate evaluated to enabled.
    pub enabled: bool,
}

/// Redacted job settings for a session. The job nonce is excluded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSettingsJobSnapshot {
    /// Availability of job-specific built-in tools.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub built_in_tool_availability: Option<SessionSettingsBuiltInToolAvailabilitySnapshot>,
    /// GitHub Actions event type for the job.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub event_type: Option<String>,
    /// Whether this is the workflow's trigger job.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_trigger_job: Option<bool>,
}

/// Redacted model routing settings for a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSettingsModelSnapshot {
    /// Agent service callback URL for job and progress updates.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub callback_url: Option<String>,
    /// Default reasoning effort for the selected model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_reasoning_effort: Option<String>,
    /// Agent job identifier for the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub instance_id: Option<String>,
    /// Selected model identifier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
}

/// Online-evaluation settings safe to expose across the SDK boundary.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSettingsOnlineEvaluationSnapshot {
    /// Whether online evaluation is disabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disable_online_evaluation: Option<bool>,
    /// Whether online-evaluation output-file generation is enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_online_evaluation_output_file: Option<bool>,
}

/// Redacted repository and GitHub host settings for a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSettingsRepoSnapshot {
    /// Checked-out repository branch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Checked-out commit SHA.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub commit: Option<String>,
    /// GitHub server host name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    /// Protocol used to access the GitHub host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_protocol: Option<String>,
    /// GitHub repository database ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<f64>,
    /// Repository name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// GitHub repository owner database ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub owner_id: Option<f64>,
    /// Repository owner login.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub owner_name: Option<String>,
    /// Number of commits in the pull request.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pr_commit_count: Option<f64>,
    /// Whether the repository is writable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub read_write: Option<bool>,
    /// GitHub secret-scanning service URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub secret_scanning_url: Option<String>,
    /// GitHub server base URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub server_url: Option<String>,
}

/// Redacted validation and memory-tool settings for a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSettingsValidationSnapshot {
    /// Whether advisory validation is enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub advisory_enabled: Option<bool>,
    /// Whether CodeQL validation is enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub codeql_enabled: Option<bool>,
    /// Whether code-review validation is enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code_review_enabled: Option<bool>,
    /// Model used for code-review validation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code_review_model: Option<String>,
    /// Dependabot validation timeout budget in seconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub dependabot_timeout: Option<f64>,
    /// Whether the memory-store tool is enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub memory_store_enabled: Option<bool>,
    /// Whether the memory-vote tool is enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub memory_vote_enabled: Option<bool>,
    /// Whether secret-scanning validation is enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub secret_scanning_enabled: Option<bool>,
    /// General validation timeout budget in seconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<f64>,
}

/// Redacted, serializable view of session runtime settings for SDK boundary consumers. Secrets and raw feature flags are intentionally excluded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSettingsSnapshot {
    /// Name of the SDK client that created the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Redacted job settings.
    pub job: SessionSettingsJobSnapshot,
    /// Redacted model routing settings.
    pub model: SessionSettingsModelSnapshot,
    /// Online-evaluation settings safe for SDK consumers.
    pub online_evaluation: SessionSettingsOnlineEvaluationSnapshot,
    /// Redacted repository and host settings.
    pub repo: SessionSettingsRepoSnapshot,
    /// Session start time as Unix epoch milliseconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub start_time_ms: Option<f64>,
    /// Session timeout in milliseconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout_ms: Option<f64>,
    /// Redacted validation and memory-tool settings.
    pub validation: SessionSettingsValidationSnapshot,
    /// Agent runtime version selector copied from the session settings, such as `latest` or a runtime release identifier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// UUID prefix to resolve to a unique session ID.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsFindByPrefixRequest {
    /// UUID prefix (>=7 hex chars, <36 chars). Returns the unique session ID, or undefined when there is no match or the prefix matches multiple sessions.
    pub prefix: String,
}

/// Session ID matching the prefix, omitted when no unique match exists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsFindByPrefixResult {
    /// Omitted when no unique session matches the prefix (no match or ambiguous)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

/// GitHub task ID to look up.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsFindByTaskIDRequest {
    /// GitHub task ID to look up
    pub task_id: String,
}

/// ID of the local session bound to the given GitHub task, or omitted when none.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsFindByTaskIDResult {
    /// Omitted when no local session is bound to that GitHub task
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

/// Source session identifier to fork from, optional event-ID boundary, and optional friendly name for the new session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsForkRequest {
    /// Optional friendly name to assign to the forked session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Source session ID to fork from
    pub session_id: SessionId,
    /// Optional event ID boundary. When provided, the fork includes only events before this ID (exclusive). When omitted, all events are included.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub to_event_id: Option<String>,
}

/// Identifier and optional friendly name assigned to the newly forked session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsForkResult {
    /// Friendly name assigned to the forked session, if any.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// The new forked session's ID
    pub session_id: SessionId,
}

/// Session ID whose board entry count should be returned.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetBoardEntryCountRequest {
    /// Session ID whose board entry count should be returned.
    pub session_id: SessionId,
}

/// Dynamic-context board entry count, when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetBoardEntryCountResult {
    /// Board entry count, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub count: Option<i64>,
}

/// Bounded batch request for client-owned metadata from persisted local sessions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetClientMetadataRequest {
    /// Case-sensitive keys to project from each valid bag. Each key must be non-empty, at most 256 UTF-8 bytes, and outside the reserved `copilot/` and `github/` namespaces. Omit to return every entry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub keys: Option<Vec<String>>,
    /// Session IDs to inspect. Results preserve this order.
    pub session_ids: Vec<String>,
}

/// Session ID whose event-log file path to compute.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetEventFilePathRequest {
    /// Session ID whose event-log file path to compute
    pub session_id: SessionId,
}

/// Absolute path to the session's events.jsonl file on disk.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetEventFilePathResult {
    /// Absolute path to the session's events.jsonl file
    pub file_path: String,
}

/// Optional working-directory context used to score session relevance.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetLastForContextRequest {
    /// Optional working-directory context used to score session relevance. When omitted the most-recently-modified session wins.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<SessionContext>,
}

/// Most-relevant session ID for the supplied context, or omitted when no sessions exist.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetLastForContextResult {
    /// Most-relevant session ID for the supplied context, or omitted when no sessions exist
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

/// Session ID whose persisted metadata should be read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetMetadataRequest {
    /// Session ID to inspect
    pub session_id: SessionId,
}

/// Persisted local session metadata when the session exists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetMetadataResult {
    /// Local session metadata, omitted when the session does not exist.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session: Option<LocalSessionMetadataValue>,
}

/// Session ID to look up the persisted remote-steerable flag for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetPersistedRemoteSteerableRequest {
    /// Session ID to look up the persisted remote-steerable flag for
    pub session_id: SessionId,
}

/// The session's persisted remote-steerable flag, or omitted when no value has been persisted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetPersistedRemoteSteerableResult {
    /// The session's persisted remote-steerable flag if recorded; omitted when no value has been persisted
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
}

/// Map of sessionId -> on-disk size in bytes for each session's workspace directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSizes {
    /// Map of sessionId -> on-disk size in bytes for the session's workspace directory
    pub sizes: HashMap<String, i64>,
}

/// Limit for non-empty local session IDs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsListNonEmptySessionIdsRequest {
    /// Maximum number of session IDs to return.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limit: Option<i64>,
}

/// Recent local session IDs that contain user-visible history.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsListNonEmptySessionIdsResult {
    /// Session IDs ordered newest-first.
    pub session_ids: Vec<String>,
}

/// Optional source filter, metadata-load limit, and context filter applied to the returned sessions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsListRequest {
    /// Optional filter applied to the returned sessions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filter: Option<SessionListFilter>,
    /// When true, include detached maintenance sessions. Defaults to false for user-facing session lists.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_detached: Option<bool>,
    /// When provided, only the first N local sessions (sorted by modification time, newest first) load full metadata; remaining sessions return basic info only. Use 0 to return only basic info for every local session. Has no effect on remote entries (which always carry their full shape).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata_limit: Option<i64>,
    /// Which session sources to include. Defaults to `local` for backward compatibility.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<SessionSource>,
    /// Only meaningful when `source` includes remote. When true, propagates errors from the remote service instead of silently returning an empty remote list. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub throw_on_error: Option<bool>,
}

/// Active session ID whose deferred repo-level hooks should be loaded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsLoadDeferredRepoHooksRequest {
    /// Active session ID whose deferred repo-level hooks should be loaded
    pub session_id: SessionId,
}

/// Where the session's state lives, as a root directory and the session ID under it.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsLoadWorkspaceRequest {
    /// Session ID naming the state directory under the sessions home. Rejected when it is absolute or contains a parent component, so it cannot escape the sessions home.
    pub session_id: SessionId,
    /// Root directory every session's state directory sits under
    pub sessions_home: String,
}

/// The workspace record on disk, omitted when the session has none.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsLoadWorkspaceResult {
    /// The workspace record, as JSON. Omitted when the record does not exist.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub workspace_json: Option<String>,
}

/// Age threshold and optional flags controlling which old sessions are pruned (or simulated when dryRun is true).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsPruneOldRequest {
    /// When true, only report what would be deleted without performing any deletion
    #[serde(skip_serializing_if = "Option::is_none")]
    pub dry_run: Option<bool>,
    /// Session IDs that should never be considered for pruning
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_session_ids: Option<Vec<String>>,
    /// When true, named sessions (set via /rename) are also eligible for pruning
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_named: Option<bool>,
    /// Delete sessions whose modifiedTime is at least this many days old
    pub older_than_days: i64,
}

/// Pagination options for reading an inactive or active local session's persisted event journal.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsReadPersistedEventsRequest {
    /// Opaque, process-local, single-use cursor returned by the previous persisted-event read. Omit on the first call and issue continuations sequentially; reusing the same cursor returns an expired terminal page.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cursor: Option<String>,
    /// Direction to page through persisted history. Forward starts at the beginning; backward starts with the newest events. Events in each page remain chronological. This selects the initial read only; a continuation always uses the direction bound into its cursor.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub direction: Option<EventsReadDirection>,
    /// Maximum number of events to return in this batch (1–1000, default 200). Pages may contain fewer events to keep the serialized event array within a soft 1 MiB budget including resolved binary assets; one oversized event is returned alone to guarantee progress.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max: Option<i64>,
    /// Session ID whose persisted event journal should be read.
    pub session_id: SessionId,
}

/// Session ID whose in-use lock should be released.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsReleaseLockRequest {
    /// Session ID whose in-use lock should be released
    pub session_id: SessionId,
}

/// Release the in-use lock held by this process for the given session. No-op when this process does not currently hold a lock for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsReleaseLockResult {}

/// Active session ID and an optional flag for deferring repo-level hooks until folder trust.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsReloadPluginHooksRequest {
    /// When true, skip repo-level hooks. Use before folder trust is confirmed; loadDeferredRepoHooks loads them post-trust.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_repo_hooks: Option<bool>,
    /// Active session ID to reload hooks for
    pub session_id: SessionId,
}

/// Reload all hooks (user, plugin, optionally repo) and apply them to the active session. Call after installing or removing plugins so their hooks take effect immediately. No-op when no active session matches the given sessionId.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsReloadPluginHooksResult {}

/// Session ID whose pending events should be flushed to disk.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsSaveRequest {
    /// Session ID whose pending events should be flushed to disk
    pub session_id: SessionId,
}

/// Flush a session's pending events to disk. No-op when no writer exists for the session (e.g., already closed).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsSaveResult {}

/// Manager-wide additional plugins to register; replaces any previously-configured set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsSetAdditionalPluginsRequest {
    /// Manager-wide additional plugins to register. Replaces any previously-configured set. Pass an empty array to clear.
    pub plugins: Vec<InstalledPlugin>,
}

/// Replace the manager-wide additional plugins. New session creations and subsequent hook reloads see the new set; already-running sessions keep their existing hook installation until the next reload.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsSetAdditionalPluginsResult {}

/// Patch for the singleton's steering state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsSetRemoteControlSteeringRequest {
    /// Target steering state. Today only `true` is actionable on the underlying exporter; `false` is reserved for future use.
    pub enabled: bool,
}

/// Parameters for attaching the remote-control singleton to a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsStartRemoteControlRequest {
    /// Configuration for the runtime-managed remote-control singleton.
    pub config: RemoteControlConfig,
    /// Local session id to attach remote control to.
    pub session_id: SessionId,
}

/// Parameters for stopping the remote-control singleton.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsStopRemoteControlRequest {
    /// When provided, the stop is rejected unless the singleton currently points at this session id (compare-and-swap semantics).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expected_session_id: Option<String>,
    /// When true, the singleton is unconditionally torn down regardless of `expectedSessionId`. Use during shutdown or explicit `/remote off`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force: Option<bool>,
}

/// Parameters for atomically rebinding the remote-control singleton.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsTransferRemoteControlRequest {
    /// When provided, the transfer is rejected unless the singleton currently points at this session id (compare-and-swap semantics to avoid clobbering newer state).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expected_from_session_id: Option<String>,
    /// Local session id to point remote control at.
    pub to_session_id: String,
}

/// Where the session's state lives, plus workspace-schema fields to merge into its workspace record. Stored keys outside the schema are not preserved, and a stored `fork_count` is never replaced.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsUpdateWorkspaceFieldsRequest {
    /// Workspace-schema fields to merge into the record, as a JSON object. Fields the object omits keep their stored values, except stored keys outside the schema are not preserved and a stored `fork_count` is never replaced.
    pub fields_json: String,
    /// Session ID naming the state directory under the sessions home. Rejected when it is absolute or contains a parent component, so it cannot escape the sessions home.
    pub session_id: SessionId,
    /// Root directory every session's state directory sits under
    pub sessions_home: String,
}

/// The merge completed. The record carries the supplied workspace-schema fields, but a stored `fork_count` stays.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsUpdateWorkspaceFieldsResult {}

/// Telemetry engagement ID for the session, when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTelemetryEngagement {
    /// Current telemetry engagement ID, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub engagement_id: Option<String>,
}

/// Patch of mutable session options to apply to the running session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUpdateOptionsParams {
    /// Additional content-exclusion policies to merge into the session's policy set.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_content_exclusion_policies:
        Option<Vec<OptionsUpdateAdditionalContentExclusionPolicy>>,
    /// Runtime context discriminator (e.g., `cli`, `actions`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_context: Option<String>,
    /// Explicit discovery-only agent roots, separate from customAgents and filesystem grants. Available without enableConfigDiscovery, subject to managed/source disables. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_directories: Option<Vec<String>>,
    /// Whether to include instructions from every MCP server in the system prompt instead of only allowlisted servers.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow_all_mcp_server_instructions: Option<bool>,
    /// Whether to disable the `ask_user` tool (encourages autonomous behavior).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ask_user_disabled: Option<bool>,
    /// Patch the Autopilot policy. Omitted fields retain their current values; an empty object makes no changes. Does not change the session mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub autopilot: Option<AutopilotOptions>,
    /// Allowlist of tool names available to this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available_tools: Option<Vec<String>>,
    /// Options scoped to the built-in CAPI (Copilot API) provider.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub capi: Option<CapiSessionOptions>,
    /// Identifier of the client driving the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Whether to include the `Co-authored-by` trailer in commit messages.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub coauthor_enabled: Option<bool>,
    /// Context tier for models with tiered pricing. The session uses this to derive effective `modelCapabilitiesOverrides` so compaction, truncation, token display, and request limits honor the selected tier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<OptionsUpdateContextTier>,
    /// Whether to allow auto-mode continuation across turns.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub continue_on_auto_mode: Option<bool>,
    /// Override URL for the Copilot API endpoint.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_url: Option<String>,
    /// Whether to default custom agents to local-only execution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub custom_agents_local_only: Option<bool>,
    /// Instruction source IDs to exclude from the system prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_instruction_sources: Option<Vec<String>>,
    /// Skill IDs that should be excluded from this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_skills: Option<Vec<String>>,
    /// Whether to enable repository-origin file hooks from `.github/hooks/` and trusted repository-configured roots. Does not control explicit hookDirectories or SDK callback hooks.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_file_hooks: Option<bool>,
    /// Whether to enable host git operations (context resolution, child repo scanning, git info in system prompt).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_host_git_operations: Option<bool>,
    /// Whether to discover custom instructions on demand after successful file views (AGENTS.md / CLAUDE.md / .github/copilot-instructions.md surfacing). Combined with `skipCustomInstructions`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_on_demand_instruction_discovery: Option<bool>,
    /// Whether to surface reasoning-summary events from the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_reasoning_summaries: Option<bool>,
    /// Deprecated and ignored. Shell command analysis is always enabled; retained for wire compatibility.
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_script_safety: Option<bool>,
    /// Whether to enable cross-session store writes and reads.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_session_store: Option<bool>,
    /// Whether skill loading is enabled. Explicit false disables every source, including explicit SDK roots and a bound SDK provider; changing the value invalidates the loaded skill snapshot. When omitted, creation falls back to enableConfigDiscovery unless explicit SDK skill roots or an SDK skill provider are supplied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_skills: Option<bool>,
    /// Whether to stream model responses.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_streaming: Option<bool>,
    /// How env values are passed to MCP servers (`direct` inlines literal values; `indirect` resolves at launch).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env_value_mode: Option<OptionsUpdateEnvValueMode>,
    /// Override directory for the session-events log. When unset, the runtime's default events log directory is used.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub events_log_directory: Option<String>,
    /// Whether subagent callback events should be forwarded into the session event log sink.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub events_log_includes_subagents: Option<bool>,
    /// Built-in subagent names to exclude from this session. Excluded built-ins are hidden from agent discovery and cannot be dispatched unless a custom agent with the same name is available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub excluded_builtin_agents: Option<Vec<String>>,
    /// Denylist of tool names for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub excluded_tools: Option<Vec<String>>,
    /// Map of feature-flag IDs to their boolean enabled state.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feature_flags: Option<HashMap<String, bool>>,
    /// Explicit discovery-only file-hook roots, separate from callback hooks and filesystem grants. Available without enableConfigDiscovery or enableFileHooks, which controls repository-origin hooks only; hook/managed disables still apply. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub hook_directories: Option<Vec<String>>,
    /// Skill scan directories and descendants excluded from discovery. Supports `~`-relative paths.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ignored_skills_locations: Option<Vec<String>>,
    /// Enable or disable the native image tool for this session without changing the conversation model.
    /// Omitted values preserve the current setting. Re-supply consent on cold resume.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image_generation: Option<ProtocolImageGenerationOptions>,
    /// Built-in subagent names to include in this session. When specified, only these built-ins are available, subject to runtime availability and exclusions. Custom agents with the same name remain available. Set to null to remove the allowlist restriction.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub included_builtin_agents: Option<Vec<String>>,
    /// Built-in skill names to include in this session. When specified, only these runtime-bundled skills are available. Skills from other sources with the same name remain available. Set to null to remove the allowlist restriction.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub included_builtin_skills: Option<Vec<String>>,
    /// Full set of installed plugins for the session. Replaces the existing list; the runtime invalidates the skills cache only when the list materially changes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub installed_plugins: Option<Vec<SessionInstalledPlugin>>,
    /// Explicit discovery-only instruction roots using existing SDK instruction formats, not filesystem grants. Available without enableConfigDiscovery, subject to instruction/managed disables. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub instruction_directories: Option<Vec<String>>,
    /// Stable integration identifier used for analytics and rate-limit attribution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub integration_id: Option<String>,
    /// Whether experimental capabilities are enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_experimental_mode: Option<bool>,
    /// Whether interactive shell sessions are logged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub log_interactive_shells: Option<bool>,
    /// Identifier sent to LSP-style integrations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub lsp_client_name: Option<String>,
    /// Whether to expose the `manage_schedule` tool to the agent. The runtime always owns the per-session schedule registry; this flag only controls tool exposure (typically gated to staff users).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub manage_schedule_enabled: Option<bool>,
    /// Maximum decoded byte size of a single model-facing binary tool result (e.g. an image) persisted inline in session events and re-presented to the model on later turns / resume. Larger results are persisted as a metadata-only marker and shown to the model as a short text note. Defaults to 10 MB.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_inline_binary_bytes: Option<i64>,
    /// The model ID to use for assistant turns.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Per-property model capability overrides for the selected model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities_overrides: Option<ModelCapabilitiesOverride>,
    /// Organization-level custom instructions to inject into the system prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub organization_custom_instructions: Option<String>,
    /// Custom model-provider configuration (BYOK).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ProviderConfig>,
    /// Reasoning effort for the selected model. CAPI values are model-defined and validated against the selected model; BYOK providers may define additional values. When omitted, no effort override is applied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Reasoning summary mode for supported model clients.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summary: Option<OptionsUpdateReasoningSummary>,
    /// Whether the session is running in an interactive UI.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub running_in_interactive_mode: Option<bool>,
    /// Resolved sandbox configuration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_config: Option<SandboxConfig>,
    /// Origin of the sandbox choice. Settings-derived origins (never_configured, user_enabled, user_disabled, repository_policy) let managed policy floor a host preference; explicit below-floor changes remain policy conflicts unless a session opt-out is authorized. Also used for telemetry provenance.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_config_source: Option<SandboxConfigSource>,
    /// Replaces the session's capability set with the given list. Use to enable or disable capabilities mid-session (e.g., remove `memory` for reproducible scripted runs). Omit the field to leave the existing capability set unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_capabilities: Option<Vec<SessionCapability>>,
    /// Optional session limits. Pass null to clear the session limits.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_limits: Option<SessionLimitsConfig>,
    /// Per-session settings for built-in shell tools.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell: Option<ShellOptions>,
    /// Use shell.initProfile instead. Shell init profile (`None` or `NonInteractive`).
    #[doc(hidden)]
    #[deprecated]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell_init_profile: Option<String>,
    /// PowerShell process flags applied to built-in and user-requested shell commands.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell_process_flags: Option<Vec<String>>,
    /// Explicit discovery-only skill roots, not filesystem grants. Available without enableConfigDiscovery, subject to skill/managed disables. Relative SDK paths resolve once when supplied against the request's effective workingDirectory; retained roots do not move on cwd changes. Null and omission are equivalent; [] clears only the explicit SDK contribution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_directories: Option<Vec<String>>,
    /// Whether to skip loading custom instruction sources.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_custom_instructions: Option<bool>,
    /// Whether to skip embedding retrieval pipeline initialization and execution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_embedding_retrieval: Option<bool>,
    /// When true, the selected custom agent's prompt is not injected into the user message (skill context is still injected). Used by automation triggers where the agent prompt is already in the problem statement.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_custom_agent_prompt: Option<bool>,
    /// Controls how availableTools (allowlist) and excludedTools (denylist) combine when both are set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_filter_precedence: Option<OptionsUpdateToolFilterPrecedence>,
    /// Optional path for trajectory output.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub trajectory_file: Option<String>,
    /// Output verbosity level for supported models.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub verbosity: Option<Verbosity>,
    /// Absolute working-directory path for shell tools.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// Indicates whether the session options patch was applied successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUpdateOptionsResult {
    /// Number of hooks loaded from installed plugins, returned when installedPlugins is updated
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugin_hook_count: Option<i64>,
    /// Whether the operation succeeded
    pub success: bool,
}

/// Tracked background agent task metadata, including IDs, status, timing, agent type, prompt, model, result, and latest response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskAgentInfo {
    /// ISO 8601 timestamp when the current active period began
    #[serde(skip_serializing_if = "Option::is_none")]
    pub active_started_at: Option<String>,
    /// Accumulated active execution time in milliseconds
    #[serde(skip_serializing_if = "Option::is_none")]
    pub active_time_ms: Option<i64>,
    /// Type of agent running this task
    pub agent_type: String,
    /// Whether the task is currently in the original sync wait and can be moved to background mode. False once it is already backgrounded, idle, finished, or no longer has a promotable sync waiter.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub can_promote_to_background: Option<bool>,
    /// ISO 8601 timestamp when the task finished
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completed_at: Option<String>,
    /// Short description of the task
    pub description: String,
    /// Friendly, non-unique name intended for display
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Error message when the task failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether task execution is synchronously awaited or managed in the background
    #[serde(skip_serializing_if = "Option::is_none")]
    pub execution_mode: Option<TaskExecutionMode>,
    /// Unique task identifier
    pub id: String,
    /// ISO 8601 timestamp when the agent entered idle state
    #[serde(skip_serializing_if = "Option::is_none")]
    pub idle_since: Option<String>,
    /// Most recent response text from the agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub latest_response: Option<String>,
    /// Requested model override for the task when specified
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Most recent prompt delivered to the agent. Updated whenever the agent receives a follow-up message.
    pub prompt: String,
    /// Runtime model resolved for the task when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resolved_model: Option<String>,
    /// Result text from the task when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<String>,
    /// ISO 8601 timestamp when the task was started
    pub started_at: String,
    /// Current lifecycle status of the task
    pub status: TaskStatus,
    /// Tool call ID associated with this agent task
    pub tool_call_id: String,
    /// Task kind
    pub r#type: TaskAgentInfoType,
}

/// Timestamped display line for task progress output or recent agent activity.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskProgressLine {
    /// Display message, e.g., "▸ bash", "✓ edit src/foo.ts"
    pub message: String,
    /// ISO 8601 timestamp when this event occurred
    pub timestamp: String,
}

/// Progress snapshot for an agent task, with recent activity lines and optional latest intent.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskAgentProgress {
    /// The most recent intent reported by the agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub latest_intent: Option<String>,
    /// Recent tool execution events converted to display lines
    pub recent_activity: Vec<TaskProgressLine>,
    /// Progress kind
    pub r#type: TaskAgentProgressType,
}

/// Public owner attribution for a client-owned task. Identifiers are opaque and never authorize requests.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskClientOwner {
    /// ISO 8601 timestamp when the bound join disconnected
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disconnected_at: Option<String>,
    /// Display-only owner name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Opaque identity of the currently or most recently bound session join
    pub join_id: String,
    /// Class of the task owner
    pub kind: TaskClientOwnerKind,
    /// Opaque session-scoped participant identity
    pub participant_id: String,
    /// Whether this task's bound join is currently connected
    pub presence: TaskClientOwnerPresence,
    /// Display-only owner source
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<String>,
}

/// Tracked client-owned task metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskClientInfo {
    /// ISO 8601 timestamp when the current active segment started
    #[serde(skip_serializing_if = "Option::is_none")]
    pub active_started_at: Option<String>,
    /// Accumulated active execution time in milliseconds
    pub active_time_ms: i64,
    /// Whether the currently bound owner can receive a cancellation request
    pub can_cancel: bool,
    /// Human-readable reason for terminal cancellation
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cancellation_reason: Option<String>,
    /// Owner-scoped registration and reclaim key
    pub client_task_id: String,
    /// ISO 8601 timestamp when the task reached a terminal status
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completed_at: Option<String>,
    /// Task description
    pub description: String,
    /// Optional task display name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Human-readable terminal failure message
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Optional owner-supplied terminal failure code
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error_code: Option<String>,
    /// Execution mode, which is always background for client-owned tasks
    pub execution_mode: TaskClientExecutionMode,
    /// Canonical runtime-generated task identifier
    pub id: String,
    /// ISO 8601 timestamp when the connected owner entered idle status
    #[serde(skip_serializing_if = "Option::is_none")]
    pub idle_since: Option<String>,
    /// ISO 8601 timestamp of the most recent orphan transition
    #[serde(skip_serializing_if = "Option::is_none")]
    pub orphaned_at: Option<String>,
    /// Public attribution and presence for the task owner
    pub owner: TaskClientOwner,
    /// ISO 8601 timestamp of the most recent successful reclaim
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reclaimed_at: Option<String>,
    /// Opaque successful terminal result supplied by the task owner
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
    /// Sequence number of the latest accepted owner update
    pub sequence: i64,
    /// ISO 8601 timestamp when the task started
    pub started_at: String,
    /// Client task lifecycle status
    pub status: TaskClientStatus,
    /// Task kind
    #[serde(deserialize_with = "TaskClientInfo::deserialize_type")]
    pub r#type: TaskClientType,
    /// ISO 8601 timestamp of the latest accepted lifecycle change
    pub updated_at: String,
}

impl TaskClientInfo {
    fn deserialize_type<'de, D>(deserializer: D) -> Result<TaskClientType, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "client" {
            return Err(serde::de::Error::unknown_variant(&value, &["client"]));
        }
        <TaskClientType>::deserialize(serde::de::value::StringDeserializer::<D::Error>::new(value))
    }
}

/// Generic progress for a client-owned task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskClientProgress {
    /// Most recent nonempty progress message
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_message: Option<String>,
    /// Current completion percentage from zero through one hundred
    #[serde(skip_serializing_if = "Option::is_none")]
    pub percentage: Option<f64>,
    /// Current owner-defined progress phase
    #[serde(skip_serializing_if = "Option::is_none")]
    pub phase: Option<String>,
    /// Recent server-timestamped progress messages
    pub recent_activity: Vec<TaskProgressLine>,
    /// Sequence number of the latest accepted owner update
    pub sequence: i64,
    /// Current client task lifecycle status
    pub status: TaskClientStatus,
    /// Progress kind
    #[serde(deserialize_with = "TaskClientProgress::deserialize_type")]
    pub r#type: TaskClientType,
    /// ISO 8601 timestamp of the latest accepted lifecycle change
    pub updated_at: String,
}

impl TaskClientProgress {
    fn deserialize_type<'de, D>(deserializer: D) -> Result<TaskClientType, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "client" {
            return Err(serde::de::Error::unknown_variant(&value, &["client"]));
        }
        <TaskClientType>::deserialize(serde::de::value::StringDeserializer::<D::Error>::new(value))
    }
}

/// Publishes nonterminal progress for a running or idle client task.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskClientUpdateProgress {
    /// Client task update variant discriminator.
    pub kind: TaskClientUpdateProgressKind,
    /// Optional progress message appended to recent activity when nonempty
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Optional completion percentage; null clears the current percentage
    #[serde(skip_serializing_if = "Option::is_none")]
    pub percentage: Option<f64>,
    /// Optional progress phase; null clears the current phase
    #[serde(skip_serializing_if = "Option::is_none")]
    pub phase: Option<String>,
    /// Optional active status transition
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<TaskClientActiveStatus>,
}

/// Reports successful terminal completion.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskClientUpdateCompleted {
    /// Client task update variant discriminator.
    pub kind: TaskClientUpdateCompletedKind,
    /// Optional final progress message
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Optional opaque successful terminal result
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
}

/// Reports terminal failure.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskClientUpdateFailed {
    /// Optional owner-supplied terminal failure code
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code: Option<String>,
    /// Human-readable terminal failure message
    pub error: String,
    /// Client task update variant discriminator.
    pub kind: TaskClientUpdateFailedKind,
    /// Optional final progress message
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

/// Reports terminal cancellation after external work stopped.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskClientUpdateCancelled {
    /// Client task update variant discriminator.
    pub kind: TaskClientUpdateCancelledKind,
    /// Optional final progress message
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Optional human-readable cancellation reason
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
}

/// Task completion notification with summary from the agent
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskCompleteData {
    /// Permission-protected actions that remained blocked for this genuine-user-message epoch. Present on successful completion when the objective was completed without them and on blocked completion when one or more were required.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub blocked_actions: Option<Vec<BlockedPermissionAction>>,
    /// Structured blocker details when outcome is blocked
    #[serde(skip_serializing_if = "Option::is_none")]
    pub blocker: Option<TaskBlocker>,
    /// Active autopilot objective ID evaluated by the completion reviewer
    #[serde(skip_serializing_if = "Option::is_none")]
    pub objective_id: Option<i64>,
    /// Semantic completion decision. Absent on legacy events and invalid tool calls
    #[serde(skip_serializing_if = "Option::is_none")]
    pub outcome: Option<TaskCompletionOutcome>,
    /// Label-safe runtime rationale for the completion decision (e.g. a cancellation or pause/resume downgrade), when one applies. Reviewer-authored rationale is intentionally omitted here because this event has no IFC label channel; the reviewer's findings remain available through its own labeled sub-agent events
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Whether the task was accepted as complete. False when validation failed or completion was rejected or blocked by the reviewer
    #[serde(skip_serializing_if = "Option::is_none")]
    pub success: Option<bool>,
    /// Summary of the completed task, provided by the agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
}

///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskCompletionDecision {
    /// Objective eligibility token captured when the decision was evaluated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completion_eligibility_token: Option<i64>,
    /// Whether completion was accepted after the reviewer-rejection budget was exhausted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completion_rejection_budget_exhausted: Option<bool>,
    /// Active autopilot objective evaluated by the completion reviewer.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub objective_id: Option<i64>,
    /// Semantic result of evaluating the task completion request.
    pub outcome: TaskCompletionOutcome,
    /// Rationale for the completion decision, when one is available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Whether the rationale was derived from completion-reviewer output.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reviewer_derived: Option<bool>,
    /// Information-flow metadata captured from the completion reviewer.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reviewer_result_meta: Option<serde_json::Value>,
}

/// Background tasks currently tracked by the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskList {
    /// Currently tracked tasks
    pub tasks: Vec<serde_json::Value>,
}

/// Identifier of the background task to cancel.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksCancelRequest {
    /// Task identifier
    pub id: String,
}

/// Indicates whether the background task was successfully cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksCancelResult {
    /// Whether the task was successfully cancelled
    pub cancelled: bool,
}

/// The first sync-waiting task that can currently be promoted to background mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksGetCurrentPromotableResult {
    /// The first sync-waiting task (agent first, then shell) that can currently be promoted to background mode. Omitted if no such task exists. The returned task is guaranteed to have executionMode='sync' and canPromoteToBackground=true at the time of the call.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task: Option<serde_json::Value>,
}

/// Identifier of the background task to fetch progress for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksGetProgressRequest {
    /// Task identifier (agent ID or shell ID)
    pub id: String,
}

/// Progress information for the task, or null when no task with that ID is tracked.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksGetProgressResult {
    /// Progress information for the task, discriminated by type. Returns null when no task with this ID is currently tracked.
    pub progress: serde_json::Value,
}

/// Tracked shell task metadata, including ID, command, status, timing, attachment/execution mode, log path, and PID.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskShellInfo {
    /// Whether the shell runs inside a managed PTY session or as an independent background process
    pub attachment_mode: TaskShellInfoAttachmentMode,
    /// Whether this shell task can be promoted to background mode
    #[serde(skip_serializing_if = "Option::is_none")]
    pub can_promote_to_background: Option<bool>,
    /// Command being executed
    pub command: String,
    /// ISO 8601 timestamp when the task finished
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completed_at: Option<String>,
    /// Short description of the task
    pub description: String,
    /// Whether task execution is synchronously awaited or managed in the background
    #[serde(skip_serializing_if = "Option::is_none")]
    pub execution_mode: Option<TaskExecutionMode>,
    /// Unique task identifier
    pub id: String,
    /// Path to the detached shell log, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub log_path: Option<String>,
    /// Process ID when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pid: Option<i64>,
    /// ISO 8601 timestamp when the task was started
    pub started_at: String,
    /// Current lifecycle status of the task
    pub status: TaskStatus,
    /// Task kind
    pub r#type: TaskShellInfoType,
}

/// How a shell task ended, reported with the read that reaches the end of its output.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskShellOutputExit {
    /// Process exit code, when the runtime recorded one. Absent when the shell was stopped before reporting an exit code.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exit_code: Option<i64>,
    /// Final lifecycle status of the shell task: completed, failed, or cancelled
    pub status: TaskStatus,
}

/// Progress snapshot for a shell task, with recent stdout/stderr output and optional process ID.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskShellProgress {
    /// Process ID when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pid: Option<i64>,
    /// Recent stdout/stderr lines from the running shell command
    pub recent_output: String,
    /// Progress kind
    pub r#type: TaskShellProgressType,
}

/// The promoted task as it now exists in background mode, omitted if no promotable task was waiting.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksPromoteCurrentToBackgroundResult {
    /// The promoted task as it now exists in background mode, omitted if no promotable task was waiting. Atomic operation: avoids the race window of getCurrentPromotable + promoteToBackground.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task: Option<serde_json::Value>,
}

/// Identifier of the task to promote to background mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksPromoteToBackgroundRequest {
    /// Task identifier
    pub id: String,
}

/// Indicates whether the task was successfully promoted to background mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksPromoteToBackgroundResult {
    /// Whether the task was successfully promoted to background mode
    pub promoted: bool,
}

/// Detached shell task to read output from, with an optional cursor and long-poll wait. waitMs defaults to zero (maximum 30000).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksReadOutputRequest {
    /// Opaque cursor returned by an earlier read of the same task. Omit to read from the beginning of the shell's output. If the output no longer reaches the cursor, for example because its log was removed, the request fails; read again without a cursor.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cursor: Option<String>,
    /// Identifier of a detached shell task, as returned by tasks.list
    pub id: String,
    /// Milliseconds to wait for new output, or for the shell to finish, when the cursor is already at the end of the available output. 0 (default) returns immediately. Capped at 30000ms.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wait_ms: Option<i32>,
}

/// Output a detached shell task wrote after the request cursor, with the cursor for the next read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksReadOutputResult {
    /// Opaque cursor to pass to the next read of this task. Always present, even when no output was returned.
    pub cursor: String,
    /// Present once the shell has finished and this read reaches the end of its output. Later reads with the returned cursor return no new text and the same exit details.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exit: Option<TaskShellOutputExit>,
    /// Output after the request cursor, decoded as UTF-8 and redacted. At most 64 KiB per read, so a large backlog takes several reads; the text never splits a character, and text that stops before the end of the output ends at a line break when it contains one. Empty when no new output is available. Detached shells write stdout and stderr to one output, so the text does not separate them.
    pub text: String,
}

/// Refresh metadata for any detached background shells the runtime knows about. Use after a long pause to pick up exit/output state for shells running outside the agent loop.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksRefreshResult {}

/// Registers or reclaims a client-owned task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksRegisterRequest {
    /// Whether the owner supports runtime cancellation requests
    pub cancellable: bool,
    /// Owner-scoped idempotency key used for registration and reclaim
    pub client_task_id: String,
    /// Human-readable description of the external work
    pub description: String,
    /// Optional short display name for the external work
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// Expected current sequence for idempotent registration or orphan reclaim
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expected_sequence: Option<i64>,
    /// Task kind
    #[serde(deserialize_with = "TasksRegisterRequest::deserialize_type")]
    pub r#type: TaskClientType,
}

impl TasksRegisterRequest {
    fn deserialize_type<'de, D>(deserializer: D) -> Result<TaskClientType, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let value = String::deserialize(deserializer)?;
        if value != "client" {
            return Err(serde::de::Error::unknown_variant(&value, &["client"]));
        }
        <TaskClientType>::deserialize(serde::de::value::StringDeserializer::<D::Error>::new(value))
    }
}

/// Result of registering or reclaiming a client-owned task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksRegisterResult {
    /// True only when this invocation created a new task
    pub created: bool,
    /// True only when this invocation reclaimed an orphaned task
    pub reclaimed: bool,
    /// Authoritative registered or reclaimed task
    pub task: TaskClientInfo,
}

/// Identifier of the completed or cancelled task to remove from tracking.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksRemoveRequest {
    /// Task identifier
    pub id: String,
}

/// Indicates whether the task was removed. False when the task does not exist or is still running/idle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksRemoveResult {
    /// Whether the task was removed. Returns false if the task does not exist or is still running/idle (cancel it first).
    pub removed: bool,
}

/// Identifier of the target agent task, message content, and optional sender agent ID.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksSendMessageRequest {
    /// Agent ID of the sender, if sent on behalf of another agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub from_agent_id: Option<String>,
    /// Agent task identifier
    pub id: String,
    /// Message content to send to the agent
    pub message: String,
}

/// Indicates whether the message was delivered, with an error message when delivery failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksSendMessageResult {
    /// Error message if delivery failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether the message was successfully delivered or steered
    pub sent: bool,
}

/// Agent type, prompt, name, and optional description and model override for the new task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksStartAgentRequest {
    /// Type of agent to start (e.g., 'explore', 'task', 'general-purpose')
    pub agent_type: String,
    /// Short description of the task
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Optional model override
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Friendly, non-unique name used when displaying the agent
    pub name: String,
    /// Task prompt for the agent
    pub prompt: String,
}

/// Identifier assigned to the newly started background agent task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksStartAgentResult {
    /// Generated agent ID for the background task
    pub agent_id: String,
}

/// Updates a client-owned task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksUpdateRequest {
    /// Canonical runtime-generated task identifier
    pub id: String,
    /// Owner update sequence to apply
    pub sequence: i64,
    /// Progress or terminal update payload
    pub update: TaskClientUpdate,
}

/// Result of publishing a client-owned task update.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksUpdateResult {
    /// Whether this invocation changed task state
    pub applied: bool,
    /// Whether this invocation repeated the latest accepted update
    pub duplicate: bool,
    /// Authoritative task after processing the update
    pub task: TaskClientInfo,
}

/// Wait until all in-flight background tasks (agents + shells) and any follow-up turns scheduled by their completions have settled. Returns when the runtime is fully drained or after an internal timeout (default 10 minutes; configurable via COPILOT_TASK_WAIT_TIMEOUT_SECONDS).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksWaitForPendingResult {}

/// Built-in tool metadata with identifier, optional namespaced name, description, input-parameter schema, and usage instructions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Tool {
    /// Description of what the tool does
    pub description: String,
    /// Optional instructions for how to use this tool effectively
    #[serde(skip_serializing_if = "Option::is_none")]
    pub instructions: Option<String>,
    /// Tool identifier (e.g., "bash", "grep", "str_replace_editor")
    pub name: String,
    /// Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP tools)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub namespaced_name: Option<String>,
    /// JSON Schema for the tool's input parameters
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parameters: Option<HashMap<String, serde_json::Value>>,
    /// Telemetry-safety policy for the tool name and input names, not input values. Treat omitted metadata as unsafe.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub safe_for_telemetry: Option<serde_json::Value>,
}

/// Built-in tools available for the requested model, with their parameters and instructions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolList {
    /// List of available built-in tools with metadata
    pub tools: Vec<Tool>,
}

/// A message injected by a tool result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolResultNewMessage {
    /// Message content to inject after the tool result.
    pub content: String,
    /// Source attributed to the injected message.
    pub source: String,
}

/// Expanded canonical result returned by a session tool.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolResultExpanded {
    /// Base64-encoded binary results returned to the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub binary_results_for_llm: Option<Vec<ExternalToolTextResultForLlmBinaryResultsForLlm>>,
    /// Sources returned by the tool that the model may cite.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub citable_sources: Option<Vec<serde_json::Value>>,
    /// Structured content blocks returned to the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub contents: Option<Vec<serde_json::Value>>,
    /// Error message for an unsuccessful execution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Metadata propagated with the tool result, including information-flow labels.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_meta: Option<HashMap<String, serde_json::Value>>,
    /// Messages to inject after the tool result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_messages: Option<Vec<ToolResultNewMessage>>,
    /// Whether post-tool-use failure hooks have already processed this result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub post_tool_use_failure_hooks_processed: Option<bool>,
    /// Execution outcome classification.
    pub result_type: ToolResultType,
    /// Detailed log content available for session display.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_log: Option<String>,
    /// Skill invocation metadata produced by the tool.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_invocation: Option<serde_json::Value>,
    /// Whether large-output post-processing should be skipped.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_large_output_processing: Option<bool>,
    /// Structured result content in addition to the model-facing text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub structured_content: Option<serde_json::Value>,
    /// Completion-review decision produced by the task-completion tool.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task_completion_decision: Option<TaskCompletionDecision>,
    /// Text result returned to the model.
    pub text_result_for_llm: String,
    /// Deferred tool names made available by this result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_references: Option<Vec<String>>,
    /// Tool-specific telemetry payload.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_telemetry: Option<serde_json::Value>,
    /// Optional UI resource produced by the tool.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ui_resource: Option<serde_json::Value>,
}

/// A tool name and arguments to execute through the session's native invocation pipeline.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsExecuteRequest {
    /// Arguments supplied to the tool.
    pub arguments: serde_json::Value,
    /// Name of the currently offered tool to execute.
    pub name: String,
    /// Optional identifier used to correlate this invocation with its tool call.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

/// Shell-specific names and description lines used to materialize built-in shell tool descriptors.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsShellDescriptorConfig {
    /// Additional model-facing shell description lines.
    pub description_lines: Vec<String>,
    /// Human-readable shell name.
    pub display_name: String,
    /// Tool name used to list active shells.
    pub list_shells_tool_name: String,
    /// Tool name used to read shell output.
    pub read_shell_tool_name: String,
    /// Tool name used to start shell commands.
    pub shell_tool_name: String,
    /// Stable shell type identifier.
    pub shell_type: String,
    /// Tool name used to stop shell commands.
    pub stop_shell_tool_name: String,
}

/// Options controlling how Rust-owned built-in tool descriptors are materialized.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsGetBuiltinDescriptorsRequest {
    /// Whether background task completion notifications are enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub background_task_notifications_enabled: Option<bool>,
    /// Whether tool descriptors should include authoring metadata.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_author: Option<bool>,
    /// Whether descriptors should favor fewer user-intervention prompts.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reduce_user_intervention: Option<bool>,
    /// Shell-specific names and description lines for shell tools.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell_config: Option<ToolsShellDescriptorConfig>,
    /// Whether the configured shell supports PowerShell 7 syntax.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell_supports_power_shell7_syntax: Option<bool>,
    /// Default shell timeout in milliseconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell_timeout_ms: Option<f64>,
    /// Whether semantic skill lookup is available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_embedding_enabled: Option<bool>,
}

/// Rust-owned built-in tool descriptors for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsGetBuiltinDescriptorsResult {
    /// Built-in tool descriptors materialized for the session.
    pub tools: Vec<BuiltinToolDescriptor>,
}

/// Current lightweight tool metadata snapshot for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsGetCurrentMetadataResult {
    /// Current tool metadata, or null when tools have not been initialized yet
    pub tools: Option<Vec<CurrentToolMetadata>>,
}

/// Current configured subagent settings for the session, before resolving defaults or inheritance.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsGetSubagentSettingsResult {
    /// Live subagent settings override, or current persisted settings when no override is active; null when no settings block exists
    pub subagents: Option<SubagentSettingsConfig>,
}

/// Resolve, build, and validate the runtime tool list for this session. Subagent sessions and consumer flows that need an initialized tool set before `send` invoke this. Default base-class implementation is a no-op for sessions that don't support tool validation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsInitializeAndValidateResult {}

/// No parameters; returns the external tool calls of the session and its sub-agents that are still waiting for a result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsListPendingRequestsRequest {}

/// Optional model identifier whose tool overrides should be applied to the listing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsListRequest {
    /// Optional model ID — when provided, the returned tool list reflects model-specific overrides
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
}

/// Complete externally implemented tool list for the calling connection. An empty list removes every tool previously supplied by that connection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsSetRequest {
    /// Complete replacement list for the calling connection.
    pub tools: Vec<ProtocolExternalToolDefinition>,
}

/// Empty result after replacing the calling connection's externally implemented tools.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsSetResult {}

/// Task-completion tool arguments and final result used to build a label-safe session event payload.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsTaskCompleteEventDataRequest {
    /// Final expanded result returned by the task_complete tool.
    pub final_result: ToolResultExpanded,
    /// Arguments supplied to the completed task_complete tool call.
    pub tool_args: serde_json::Value,
}

/// Empty result after applying subagent settings
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsUpdateSubagentSettingsResult {}

/// Which stored location permissions were dropped because their directory is gone.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct PermissionsLocationsCleanupOrphanedResult {
    /// Location keys whose grants were removed. Empty when every stored location still exists.
    pub removed_location_keys: Vec<String>,
}

/// Folder trust check result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsFolderTrustCheckTrustResult {
    /// Whether the folder is trusted
    pub trusted: bool,
}

/// Built-in tools available for the requested model, with their parameters and instructions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsListResult {
    /// List of available built-in tools with metadata
    pub tools: Vec<Tool>,
}

/// Result of opening a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenResult {
    /// Remote session metadata, present when status is `connected`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<RemoteSessionMetadataValue>,
    /// Handoff progress steps, present when status is `handed_off`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub progress: Option<Vec<SessionsOpenProgress>>,
    /// Remote session ID, present when status is `connected`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_session_id: Option<SessionId>,
    /// In-process SessionClientApi handle for the opened session, returned to CLI callers as a transitional shortcut. Marked internal so the public SDK surface does not expose it; SDK consumers should construct per-session clients from `sessionId` instead.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) session_api: Option<serde_json::Value>,
    /// Opened session ID. Omitted when status is `not_found`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// Startup prompts queued by user-level hook configs at session creation. Only populated when status is `created`; resumed sessions return an empty array.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub startup_prompts: Option<Vec<String>>,
    /// Outcome of the open request.
    pub status: SessionsOpenStatus,
}

/// Remote session connection result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsConnectResult {
    /// Metadata for a connected remote session.
    pub metadata: ConnectedRemoteSessionMetadata,
    /// SDK session ID for the connected remote session.
    pub session_id: SessionId,
}

/// Sessions matching the filter, ordered most-recently-modified first.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsListResult {
    /// Sessions ordered most-recently-modified first. Discriminated by `isRemote`.
    pub sessions: Vec<serde_json::Value>,
}

/// Batch of session events returned by a read, with cursor and continuation metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsReadPersistedEventsResult {
    /// Opaque cursor for the next read. Pass back unchanged in the next read.cursor to continue from where this read left off. Always present, even when no events were returned. For a backward read this cursor pages toward OLDER events; keep passing `direction: backward` with it (the cursor is also self-describing, so backward paging continues correctly).
    pub cursor: String,
    /// Cursor status: 'ok' means the cursor was applied successfully. For session.eventLog.read, 'expired' means the cursor referred to an event that no longer exists in active history and the read fell back to a boundary of the remaining history: the beginning for a forward read or the newest window for a backward read. That fallback may overlap already rendered events, so active-session consumers should reset, rebase, or deduplicate before continuing. sessions.readPersistedEvents has stricter snapshot semantics: 'expired' returns an empty terminal page and never switches to a replacement journal generation. Other persisted-read I/O failures are RPC errors with diagnostics, not cursor expiry.
    pub cursor_status: EventsCursorStatus,
    /// Session events for this batch, merged into a single stream in creation order: durable (persisted) events and ephemeral events interleave exactly as they were emitted. Set `includeEphemeral: false` to receive only durable events. Ephemeral events are never replayable once pruned from the in-memory ring, so a consumer that needs them should keep reading with a non-zero `waitMs`. For a backward (tail-first) read, the returned window contains persisted events only, still in chronological (oldest-to-newest) append order.
    pub events: Vec<SessionEvent>,
    /// True when more events are available in the read's direction. For a backward read, true means older persisted events remain before the returned window. A persisted-event page may contain fewer than `max` events because of its byte budget while still reporting hasMore true; continue according to this flag rather than the event count.
    pub has_more: bool,
}

/// ID of the local session bound to the given GitHub task, or omitted when none.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsFindByTaskIdResult {
    /// Omitted when no local session is bound to that GitHub task
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

/// Map of sessionId -> on-disk size in bytes for each session's workspace directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetSizesResult {
    /// Map of sessionId -> on-disk size in bytes for the session's workspace directory
    pub sizes: HashMap<String, i64>,
}

/// Map of sessionId -> bytes freed by removing the session's workspace directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsBulkDeleteResult {
    /// Map of sessionId -> bytes freed by removing the session's workspace directory. Sessions whose deletion failed are omitted from this map (failures are logged on the server but not surfaced per-id; check the map for absent IDs to detect them).
    pub freed_bytes: HashMap<String, i64>,
}

/// Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsPruneOldResult {
    /// Session IDs that would be deleted in dry-run mode (always empty otherwise)
    pub candidates: Vec<String>,
    /// Session IDs that were deleted (always empty in dry-run mode)
    pub deleted: Vec<String>,
    /// True when no deletions were actually performed
    pub dry_run: bool,
    /// Total bytes freed (actual when not dry-run, projected when dry-run)
    pub freed_bytes: i64,
    /// Session IDs that were skipped (e.g., named sessions)
    pub skipped: Vec<String>,
}

/// The enriched metadata records, with summary and context fields backfilled where available. Sessions confirmed empty and unnamed are omitted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsEnrichMetadataResult {
    /// Enriched records, with summary and context backfilled. Sessions confirmed empty and unnamed may be omitted.
    pub sessions: Vec<LocalSessionMetadataValue>,
}

/// Queued repo-level startup prompts and the total hook command count after loading.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsLoadDeferredRepoHooksResult {
    /// Total hook command count (user + plugin + repo) loaded for the session by this call. Captured atomically with startupPrompts so callers don't need to read a separate counter.
    pub hook_count: i64,
    /// Repo-level startup prompts queued from repo hook configs. Empty on resume, when no repo configs were pending, or when disableAllHooks is set.
    pub startup_prompts: Vec<String>,
}

/// Wrapper for the singleton's current status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsStartRemoteControlResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
}

/// Outcome of a transferRemoteControl call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsTransferRemoteControlResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
    /// Whether the rebinding actually happened.
    pub transferred: bool,
}

/// Wrapper for the singleton's current status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsSetRemoteControlSteeringResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
}

/// Outcome of a stopRemoteControl call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsStopRemoteControlResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
    /// Whether the singleton was actually torn down by this call.
    pub stopped: bool,
}

/// Wrapper for the singleton's current status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetRemoteControlStatusResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionProvidersGetCatalogParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Normalized model-provider adapter definitions available to the session, not discovered instances.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionProvidersGetCatalogResult {
    /// Available provider adapters ordered by adapterId.
    pub providers: Vec<ModelProviderAdapterDescriptor>,
}

/// Provider instances found by a discovery operation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionProvidersDiscoverResult {
    /// Discovered provider instances. Empty when passive default discovery finds no reachable provider.
    pub instances: Vec<ModelProviderInstance>,
    /// Typed operation outcome. Passive discovery can return `absent` with an empty instance list.
    pub outcome: ModelProviderOperationOutcome,
}

/// Current health information for a provider instance.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionProvidersGetStatusResult {
    /// Normalized provider instance.
    pub instance: ModelProviderInstance,
    /// Typed operation outcome.
    pub outcome: ModelProviderOperationOutcome,
    /// Open provider status value, such as `healthy`, `unreachable`, or `notInstalled`.
    pub status: String,
    /// Provider-reported version.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Models offered for agent conversations by one provider instance. Adapters exclude known-incompatible models, but retain candidates with unknown capabilities. Listing does not guarantee compatibility.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionProvidersModelsListResult {
    /// Provider-native models in provider order.
    pub models: Vec<DiscoveredModel>,
    /// Typed operation outcome.
    pub outcome: ModelProviderOperationOutcome,
}

/// Provider configuration prepared from a discovered model. Preparing a plan changes nothing: it neither registers the model with the session nor writes durable configuration. To apply it, pass `provider` and `model` to `session.provider.add`, omitting whichever the dispositions report as already configured.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionProvidersModelsPrepareConfigurationResult {
    /// Model definition prepared from the discovered model. Capability fields the provider did not report stay omitted rather than being asserted false.
    pub model: ProviderModelConfig,
    /// Whether `model` still needs to be registered. When `alreadyConfigured`, `selectionId` is already registered and the caller can select it without adding anything.
    pub model_disposition: ModelProviderConfigurationDisposition,
    /// Provider connection prepared from the instance's inference metadata. Carries no credential; supply one if the endpoint requires it.
    pub provider: NamedProviderConfig,
    /// Whether `provider` still needs to be registered. When `alreadyConfigured`, a provider with the same endpoint is already registered and `provider` restates it under its existing name; adding it again is rejected as a duplicate.
    pub provider_disposition: ModelProviderConfigurationDisposition,
    /// Provider-qualified selection id (`provider/id`) to pass to `switchTo` once the plan is applied.
    pub selection_id: String,
    /// Non-fatal warnings carried over from the discovered model, such as capabilities the provider did not report.
    pub warnings: Vec<ModelProviderWarning>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSuspendParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Result of sending a user message
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSendResult {
    /// Unique identifier assigned to the message
    pub message_id: String,
}

/// Internal request-correlated completion of one submitted message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSendAndWaitResult {
    /// Whether the request was aborted or removed before completing normally.
    pub aborted: bool,
    /// Existing event at the completion boundary. SDK helpers drain its synchronous subscriber delivery before returning. Absent for quiet queue removal.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completion_event_id: Option<String>,
    /// Identifier assigned to the submitted user message.
    pub message_id: String,
    /// The exact existing root assistant event owned by this request. Absent when the request completed without an assistant response.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub response: Option<SessionEvent>,
}

/// Result of sending zero or more user messages
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSendMessagesResult {
    /// Unique identifiers assigned to the messages, one per provided message in order. For a batch that starts a run, assistant messages use the final ID as originatingMessageId throughout that run, including tool iterations and stop-hook corrections. Immediate steering does not replace the active run's origin. Empty when no messages were provided; that run has no originatingMessageId.
    pub message_ids: Vec<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSandboxGetEnforcementStatusParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Managed sandbox enforcement state for a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSandboxGetEnforcementStatusResult {
    /// Whether an enforcement failure has permanently blocked the session.
    pub blocked: bool,
    /// The first sandbox enforcement failure that blocked the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Whether the effective managed policy requires an available sandbox backend.
    pub required: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSandboxGetCredentialSuggestionsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Possible unconfigured secrets in the sandbox shell environment, sorted by variable name, with no secret values.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSandboxGetCredentialSuggestionsResult {
    /// Candidates for user review. Empty when sandboxing is disabled or no unconfigured candidates are present.
    pub suggestions: Vec<SandboxCredentialSuggestion>,
}

/// Result of attempting to disable sandboxing for the current session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSandboxDisableForSessionResult {
    /// The authoritative sandbox enabled state after the operation.
    pub enabled: bool,
    /// Whether this call resolved the pending request and applied the session opt-out.
    pub success: bool,
}

/// Result of accepting a sandbox path grant.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSandboxGrantPathForRequestResult {
    /// Whether this call resolved the pending request and added the path to the session's sandbox policy.
    pub success: bool,
}

/// Result of aborting the current turn
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAbortResult {
    /// Error message if the abort failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether the abort completed successfully
    pub success: bool,
}

/// Result of interrupting the main agent turn.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInterruptMainTurnResult {
    /// Whether an in-flight main agent turn was interrupted. False when the main loop was not processing.
    pub interrupted: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCancelAllBackgroundAgentsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionGitHubAuthGetStatusParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Authentication status and account metadata for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionGitHubAuthGetStatusResult {
    /// Authentication type
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_type: Option<AuthInfoType>,
    /// Copilot plan tier (e.g., individual_pro, business)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_plan: Option<String>,
    /// Authentication host URL
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    /// Whether the session has resolved authentication
    pub is_authenticated: bool,
    /// Authenticated login/username, if available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// Human-readable authentication status description
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_message: Option<String>,
}

/// Indicates whether the credential update succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionGitHubAuthSetCredentialsResult {
    /// Whether the session ended up with a populated `copilotUser` for the installed credentials. `true` when the supplied credential already carried `copilotUser` or it was successfully re-resolved server-side. `false` when the credential is installed without `copilotUser` — either re-resolution failed, or the variant cannot be re-resolved from the credential alone (only the raw-token variants `token`, `env`, and `gh-cli` can). In both `false` cases the token swap still applied, but plan/quota/billing metadata is degraded. Present whenever a credential was supplied; omitted only when no credential was supplied (no-op call).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user_resolved: Option<bool>,
    /// Whether the operation succeeded
    pub success: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionGitHubAuthGetCurrentAuthInfoParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionGitHubAuthGetAllAuthAvailableParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionGitHubAuthRefreshCopilotUserParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionGitHubAuthLogoutParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionGitHubAuthLastAuthErrorsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Result of a non-interactive accounts mutation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAccountsSetResult {
    /// For a logout, whether other signed-in accounts remain.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub more_users: Option<bool>,
    /// Whether the mutation was applied.
    pub ok: bool,
}

/// A started login flow: its opaque id and first step.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAccountsLoginBeginResult {
    /// Opaque flow id used to advance or cancel this login.
    pub flow_id: String,
    /// The first step of the flow.
    pub step: AuthLoginStep,
}

/// Result of collecting a session debug bundle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionDebugCollectLogsResult {
    /// Files included in the bundle.
    pub entries: Vec<DebugCollectLogsCollectedEntry>,
    /// Destination kind that was written.
    pub kind: DebugCollectLogsResultKind,
    /// Actual archive path or staging directory path written. This may differ from the requested path when no-overwrite suffixing or fallback-to-temp-directory was needed.
    pub path: String,
    /// Optional files or directories that could not be included.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skipped_entries: Option<Vec<DebugCollectLogsSkippedEntry>>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCanvasListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Declared canvases available in this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCanvasListResult {
    /// Declared canvases available in this session
    pub canvases: Vec<DiscoveredCanvas>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCanvasListOpenParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Live open-canvas snapshot.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCanvasListOpenResult {
    /// Currently open canvas instances
    pub open_canvases: Vec<OpenCanvasInstance>,
}

/// Open canvas instance snapshot.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCanvasOpenResult {
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Owning provider identifier
    pub extension_id: String,
    /// Owning extension display name, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_name: Option<String>,
    /// Host-local PNG path for the canvas icon, when supplied
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icon: Option<String>,
    /// Input supplied when the instance was opened
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
    /// Stable caller-supplied canvas instance identifier
    pub instance_id: String,
    /// Provider-supplied status text
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// Rendered title
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// URL for web-rendered canvases
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Canvas action invocation result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCanvasActionInvokeResult {
    /// Provider-supplied action result
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
}

/// Complete current or terminal workflow run envelope.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowRunResult {
    /// One-based execution attempt represented by this envelope. Absent before the first attempt starts or when returned by an older runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attempt: Option<i64>,
    /// Error message for an errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Machine-readable failure details for a halted or errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub failure: Option<serde_json::Value>,
    /// Structured pause initiator metadata for a paused attempt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pause_info: Option<serde_json::Value>,
    /// Reason for a halted or cancelled run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Completed workflow result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
    /// Workflow run identifier.
    pub run_id: String,
    /// Partial journal and progress snapshot for a halted, cancelled, or errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub snapshot: Option<serde_json::Value>,
    /// Current or terminal workflow run status.
    pub status: WorkflowRunStatus,
}

/// Resolved persisted workflow identity and resumed run envelope.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowResumeResult {
    /// Terminal resumed run envelope.
    pub run: WorkflowRunResult,
    /// Persisted workflow name resolved for the resumed run.
    pub workflow_name: String,
}

/// Complete current or terminal workflow run envelope.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowRunFromToolResult {
    /// One-based execution attempt represented by this envelope. Absent before the first attempt starts or when returned by an older runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attempt: Option<i64>,
    /// Error message for an errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Machine-readable failure details for a halted or errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub failure: Option<serde_json::Value>,
    /// Structured pause initiator metadata for a paused attempt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pause_info: Option<serde_json::Value>,
    /// Reason for a halted or cancelled run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Completed workflow result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
    /// Workflow run identifier.
    pub run_id: String,
    /// Partial journal and progress snapshot for a halted, cancelled, or errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub snapshot: Option<serde_json::Value>,
    /// Current or terminal workflow run status.
    pub status: WorkflowRunStatus,
}

/// Resolved persisted workflow identity and resumed run envelope.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowResumeFromToolResult {
    /// Terminal resumed run envelope.
    pub run: WorkflowRunResult,
    /// Persisted workflow name resolved for the resumed run.
    pub workflow_name: String,
}

/// Complete current or terminal workflow run envelope.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowGetRunResult {
    /// One-based execution attempt represented by this envelope. Absent before the first attempt starts or when returned by an older runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attempt: Option<i64>,
    /// Error message for an errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Machine-readable failure details for a halted or errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub failure: Option<serde_json::Value>,
    /// Structured pause initiator metadata for a paused attempt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pause_info: Option<serde_json::Value>,
    /// Reason for a halted or cancelled run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Completed workflow result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
    /// Workflow run identifier.
    pub run_id: String,
    /// Partial journal and progress snapshot for a halted, cancelled, or errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub snapshot: Option<serde_json::Value>,
    /// Current or terminal workflow run status.
    pub status: WorkflowRunStatus,
}

/// A page of workflow runs in durable creation order.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowListRunsResult {
    /// Whether terminal runs newer than this page exist.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub has_more_newer: Option<bool>,
    /// Newest terminal-run cursor in this page, or null when the terminal window is empty.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub newest_seq: Option<i64>,
    /// Oldest terminal-run cursor in this page, or null when the terminal window is empty.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oldest_seq: Option<i64>,
    /// Number of terminal runs older than this page.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub omitted_older: Option<i64>,
    /// Workflow run summaries in durable creation order.
    pub runs: Vec<WorkflowRunSummary>,
}

/// Full workflow run observability detail.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowGetRunDetailResult {
    /// Epoch milliseconds when the current active segment started, or null while inactive.
    pub active_segment_started_at: Option<i64>,
    /// Durable identities and live statuses for direct workflow agents.
    pub agents: Vec<WorkflowAgentSummary>,
    /// Approved effective resource ceilings, or null until approved.
    pub approved: Option<WorkflowDeclaredLimits>,
    /// Whether the durable run state currently passes runtime resume eligibility checks.
    pub can_resume: bool,
    /// Epoch milliseconds when the run completed, or null while nonterminal.
    pub completed_at: Option<i64>,
    /// Durable resource consumption.
    pub consumed: WorkflowRunConsumed,
    /// Epoch milliseconds when the run was created.
    pub created_at: i64,
    /// Current phase identity, or null before any phase is entered.
    pub current_phase: Option<WorkflowCurrentPhase>,
    /// Resource ceilings declared by the workflow.
    pub declared_limits: WorkflowDeclaredLimits,
    /// Number of phases declared by the workflow.
    pub declared_phase_count: i64,
    /// Human-readable workflow description.
    pub description: String,
    /// Number of direct workflow agents currently live.
    pub live_agent_count: i64,
    /// Epoch milliseconds when this live-overlay snapshot was observed.
    pub observed_at: i64,
    /// Lifecycle and timing observations for each workflow phase.
    pub phases: Vec<WorkflowPhaseObservation>,
    /// Bidirectional page of durable workflow progress.
    pub progress: WorkflowProgressPage,
    /// Monotonic durable run revision.
    pub revision: i64,
    /// Workflow run identifier.
    pub run_id: String,
    /// Epoch milliseconds when execution first started, or null before start.
    pub started_at: Option<i64>,
    /// Current workflow run status.
    pub status: WorkflowRunStatus,
    /// Terminal run outcome, or null while nonterminal.
    pub terminal: Option<WorkflowRunTerminal>,
    /// Total direct workflow agents spawned across all attempts.
    pub total_spawned_agent_count: i64,
    /// Epoch milliseconds when the durable run was last updated.
    pub updated_at: i64,
    /// Registered workflow name.
    pub workflow_name: String,
}

/// A bidirectional page of workflow progress.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowGetRunProgressResult {
    /// Whether progress records newer than this page exist.
    pub has_more_newer: bool,
    /// Whether progress records older than this page exist.
    pub has_more_older: bool,
    /// Newest sequence number in this page, or null when empty.
    pub newest_seq: Option<i64>,
    /// Oldest sequence number in this page, or null when empty.
    pub oldest_seq: Option<i64>,
    /// Progress records in sequence order.
    pub records: Vec<WorkflowProgressLine>,
    /// Run revision reflected by this page.
    pub revision: i64,
}

/// Complete current or terminal workflow run envelope.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowCancelResult {
    /// One-based execution attempt represented by this envelope. Absent before the first attempt starts or when returned by an older runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attempt: Option<i64>,
    /// Error message for an errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Machine-readable failure details for a halted or errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub failure: Option<serde_json::Value>,
    /// Structured pause initiator metadata for a paused attempt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pause_info: Option<serde_json::Value>,
    /// Reason for a halted or cancelled run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Completed workflow result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
    /// Workflow run identifier.
    pub run_id: String,
    /// Partial journal and progress snapshot for a halted, cancelled, or errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub snapshot: Option<serde_json::Value>,
    /// Current or terminal workflow run status.
    pub status: WorkflowRunStatus,
}

/// Complete current or terminal workflow run envelope.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowPauseResult {
    /// One-based execution attempt represented by this envelope. Absent before the first attempt starts or when returned by an older runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attempt: Option<i64>,
    /// Error message for an errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Machine-readable failure details for a halted or errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub failure: Option<serde_json::Value>,
    /// Structured pause initiator metadata for a paused attempt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pause_info: Option<serde_json::Value>,
    /// Reason for a halted or cancelled run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Completed workflow result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
    /// Workflow run identifier.
    pub run_id: String,
    /// Partial journal and progress snapshot for a halted, cancelled, or errored run.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub snapshot: Option<serde_json::Value>,
    /// Current or terminal workflow run status.
    pub status: WorkflowRunStatus,
}

///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowPauseAtCheckpointResult {
    /// Whether this execution attempt must pause or may continue.
    pub action: WorkflowPauseCheckpointAction,
}

/// Acknowledgement that a workflow request was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowLogResult {}

/// Result of one workflow-scoped subagent call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowAgentResult {
    /// Agent result, omitted when the agent produced no result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
}

/// Result of reading a workflow journal entry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowJournalGetResult {
    /// Whether the journal contained the requested key.
    pub hit: bool,
    /// Cached JSON result. The hit field distinguishes a cached JSON null from a miss.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result_json: Option<serde_json::Value>,
}

/// Acknowledgement that a workflow request was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkflowJournalPutResult {}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelGetCurrentParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The session's authoritative model snapshot. Auto preference fields are configuration for the virtual `auto` model and do not change the selected model identifier. The context tier reflects `Session.getContextTier()`, restored from the session journal on resume.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelGetCurrentResult {
    /// Auto preference currently claimed by an in-progress activation. Null means the activation is returning to provider-default routing.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub activating_auto_tier: Option<AutoTier>,
    /// Auto preference currently committed for the session. This can remain available while another model is selected so a later switch to `auto` can reuse it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_tier: Option<AutoTier>,
    /// Context tier for models that support multiple context-window sizes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<ContextTier>,
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
    /// Latest unclaimed Auto preference waiting for a future user turn. Null means the pending request is returning to provider-default routing.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pending_auto_tier: Option<AutoTier>,
    /// Captured base model to restore when leaving plan mode. Omitted outside plan mode or when no plan override has captured a base model. Persistent agent model requirements apply to this model rather than the temporary plan model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plan_base_model_id: Option<String>,
    /// Provider of planBaseModelId, when that saved selection is provider-qualified.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plan_base_provider_id: Option<String>,
    /// Provider selected for this model. Together with modelId, identifies a catalog entry even when providers serve the same model. Omitted for unattributed legacy selections.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_id: Option<String>,
    /// Reasoning effort level currently applied to the active model, when one is set. Reads `Session.getReasoningEffort()` synchronously after `getSelectedModel()` resolves so the two values are reported as a snapshot.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
}

/// The model identifier active on the session after the switch.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelSwitchToResult {
    /// Compaction confirmation projection when status is confirmation_required
    #[serde(skip_serializing_if = "Option::is_none")]
    pub confirmation: Option<ModelSwitchConfirmation>,
    /// True when the switch was deferred (enqueued as a cancellable `/model` command) because a turn was active or another model change was already queued, rather than applied immediately. When true, the session's live model is unchanged until the queued change drains.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deferred: Option<bool>,
    /// Deprecation warnings associated with the selected model or options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deprecation_warnings: Option<Vec<String>>,
    /// User-facing outcome message for the model switch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Currently active model identifier after the switch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
    /// Authoritative model and Auto preference state after an immediate switch. For deferred switches this remains the current state until the queued change drains.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_state: Option<CurrentModel>,
    /// Persistence failure encountered after applying the model switch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub persistence_error: Option<String>,
    /// Stable queue item identifier when this request was enqueued. Remains present if the item drains before the response is returned.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub queue_id: Option<String>,
    /// Lifecycle result for the requested switch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// User-facing warning produced while applying the model switch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// Immediate acknowledgement and Auto preference snapshot after a switch request. This result never implies that a pending preference committed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelSwitchAutoTierResult {
    /// Auto preference currently claimed by an in-progress activation. Null means the activation is returning to provider-default routing.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub activating_auto_tier: Option<AutoTier>,
    /// Auto preference currently committed for the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub effective_auto_tier: Option<AutoTier>,
    /// Latest unclaimed Auto preference waiting for a future user turn.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pending_auto_tier: Option<AutoTier>,
    /// Immediate request status. `pending` means accepted but not committed.
    pub status: ModelSwitchAutoTierStatus,
    /// Earlier unclaimed preference replaced by this request. This can be present with either status, including when selecting the effective preference cancels pending work.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub superseded_auto_tier: Option<AutoTier>,
}

/// The model identifier active on the session after the switch.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelApplyStartupOverlayResult {
    /// Compaction confirmation projection when status is confirmation_required
    #[serde(skip_serializing_if = "Option::is_none")]
    pub confirmation: Option<ModelSwitchConfirmation>,
    /// True when the switch was deferred (enqueued as a cancellable `/model` command) because a turn was active or another model change was already queued, rather than applied immediately. When true, the session's live model is unchanged until the queued change drains.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deferred: Option<bool>,
    /// Deprecation warnings associated with the selected model or options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deprecation_warnings: Option<Vec<String>>,
    /// User-facing outcome message for the model switch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Currently active model identifier after the switch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
    /// Authoritative model and Auto preference state after an immediate switch. For deferred switches this remains the current state until the queued change drains.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_state: Option<CurrentModel>,
    /// Persistence failure encountered after applying the model switch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub persistence_error: Option<String>,
    /// Stable queue item identifier when this request was enqueued. Remains present if the item drains before the response is returned.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub queue_id: Option<String>,
    /// Lifecycle result for the requested switch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// User-facing warning produced while applying the model switch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SessionModelClearStartupSeedParams {
    pub expected_model: String,
    /// Provider captured with the startup seed; omission matches only an unqualified seed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expected_provider_id: Option<String>,
}

///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SessionModelClearStartupSeedResult {
    pub cleared: bool,
}

/// The applied host allowlist and effective session model policy after intersection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelSetAllowedModelsResult {
    /// Normalized host allowlist. Omitted when the host restriction was cleared, or when a relay client does not return the host policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allowed_models: Option<Vec<String>>,
    /// Effective exact IDs or repository policy patterns after applying the host restriction. Omitted by relay clients that do not return the host policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub effective_allowed_models: Option<Vec<String>>,
    /// Effective deterministic fallback model, when the policy defines one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub fallback_model: Option<String>,
    /// Selected session model after reconciling a now-disallowed concrete selection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

/// Update the session's reasoning effort without changing the selected model. Use `switchTo` instead when you also need to change the model. The runtime stores the effort on the session and applies it to subsequent turns.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelSetReasoningEffortResult {
    /// Reasoning effort level recorded on the session after the update
    pub reasoning_effort: String,
}

/// Reasoning-summaries presentation policy for the session's current model.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelGetReasoningSummariesResult {
    /// Whether the supplied `persistedShowReasoning` setting only restates the model default. This reports a redundancy, not a standing instruction to delete the setting: it stays true for as long as the setting and the model default agree, including for a user who later chose that value deliberately. It exists for a one-time migration of a legacy setting written before the model default made it redundant, so a host that acts on it owns the cleanup and must gate it behind its own persisted `done` marker, delete only a setting whose value still matches the one it read, and never repeat the deletion on a later launch. Under an off-by-default reasoning-summaries experiment an explicit `showReasoning: true` opt-in also reads as redundant on a summary-producing model, so an ungated host would keep deleting that opt-in and hide summaries again on every launch.
    pub clear_redundant_setting: bool,
    /// Whether the session's reasoning-summaries experiment assignment has settled. False means this read does not wait for a pending assignment and reports the experiment-derived fields conservatively: `showHiddenHint` is false and `visibility` falls back to `persistedShowReasoning`, or is omitted when none was supplied. A host that wants the assigned answer re-reads until this is true.
    pub experiment_settled: bool,
    /// Whether the selected model shows reasoning summaries by default.
    pub model_default_shows_summaries: bool,
    /// False until the session has a model catalog and a selected model. This read never resolves the catalog itself, and the runtime emits no readiness notification, so repeating this call alone leaves `ready` false indefinitely on a session that has neither listed models nor run a turn. `session.model.list` is the prerequisite and the signal: it resolves and caches the catalog for the session, so a successful return means the next read is ready. The exception is a session that resolves models without a Copilot catalog, that is a BYOK registry or an alternate provider: `session.model.list` returns an empty list without caching a catalog, so `ready` stays false and this policy is unavailable there. While false, every model-policy field is false and `visibility` is omitted; `experimentSettled` is independent of readiness and still reports the assignment state.
    pub ready: bool,
    /// Whether turns on this session produce reasoning summaries, honouring an explicit session reasoning-summary override.
    pub session_shows_summaries: bool,
    /// Whether to show the hint that reasoning summaries are hidden. Always false while `experimentSettled` is false.
    pub show_hidden_hint: bool,
    /// Whether the selected model emits reasoning headers.
    pub show_reasoning_headers: bool,
    /// Default reasoning visibility, and only meaningful once `ready` is true: false when the session produces no summaries, else the persisted setting, else the experiment's default. While `ready` is true it is omitted only when all three are unavailable, that is when the session produces summaries, no `persistedShowReasoning` was supplied, and `experimentSettled` is false. While `ready` is false there is no model policy to report and this field is always omitted, so an absent value alone does not imply a pending experiment.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub visibility: Option<bool>,
}

/// The list of models available to this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelListResult {
    /// Ordered Auto routing preferences discovered for this session's account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto: Option<AutoTierMetadata>,
    /// Available models, ordered with the most preferred default first. Includes both Copilot (CAPI) models and any registry BYOK models; a BYOK model appears under its provider-qualified selection id (`provider/id`).
    pub list: Vec<serde_json::Value>,
    /// Cost categories for the full CAPI catalog, including picker-disabled models that Auto may select. Metadata only; entries absent from `list` are not manually selectable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_price_categories: Option<Vec<SessionModelPriceCategory>>,
    /// The model providers available to this session, in ordering order, resolved from the account roster; empty when no provider is entitled (logged out / seatless). Each model in `list` carries its own provider reference; this roster gives the deterministic provider sequence and lets a consumer group by provider without deriving ordering from the model list. Central code never branches on a provider kind.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub providers: Option<Vec<ModelProviderDescriptor>>,
    /// Per-quota snapshots returned alongside the model list, keyed by quota type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub quota_snapshots: Option<HashMap<String, serde_json::Value>>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModeGetParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Outcome of a session mode change, including any model switch it triggered and follow-up the host must perform.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModeSetResult {
    /// Whether the host should arm an interactive continuation after the mode change.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arm_interactive_continuation: Option<bool>,
    /// Compaction confirmation required before the mode change can complete.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub confirmation: Option<ModelSwitchConfirmation>,
    /// Whether the host must defer implementing the requested mode change.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_implementation: Option<bool>,
    /// Deprecation warnings associated with the model selected by the mode change.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deprecation_warnings: Option<Vec<String>>,
    /// User-facing outcome message for the model switch triggered by the mode change.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Whether the requested mode was applied to the session. False only when an 'expectedMode' precondition did not hold, in which case any model change reported alongside it was still applied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode_applied: Option<bool>,
    /// Whether applying the mode changed the active model.
    pub model_changed: bool,
    /// Lifecycle status of the requested mode change.
    pub status: String,
    /// User-facing warning produced while applying the mode change.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionNameGetParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The session's friendly name, or null when not yet set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionNameGetResult {
    /// The session name (user-set or auto-generated), or null if not yet set
    pub name: Option<String>,
}

/// Indicates whether the auto-generated summary was applied as the session's name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionNameSetAutoResult {
    /// Whether the auto-generated summary was persisted. False if the session already has a user-set name, the summary normalized to empty, or the session does not have a workspace.
    pub applied: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Existence, contents, and resolved path of the session plan file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadResult {
    /// The content of the plan file, or null if it does not exist
    pub content: Option<String>,
    /// Whether the plan file exists in the workspace
    pub exists: bool,
    /// Absolute file path of the plan file, or null if workspace is not enabled
    pub path: Option<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanDeleteParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadSqlTodosParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Todo rows read from the session SQL database. Empty when no session database is available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadSqlTodosResult {
    /// Rows from the session SQL todos table, ordered by creation time with insertion order used to break ties when available and id used for WITHOUT ROWID tables.
    pub rows: Vec<PlanSqlTodosRow>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadSqlTodosWithDependenciesParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Todo rows + dependency edges read from the session SQL database.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadSqlTodosWithDependenciesResult {
    /// Edges from the session SQL todo_deps table. Empty when no database, no todo_deps table, or the SELECT failed. Read independently from `rows`, so a broken todo_deps table does not affect the rows result and vice versa.
    pub dependencies: Vec<PlanSqlTodoDependency>,
    /// Rows from the session SQL todos table, ordered by creation time with insertion order used to break ties when available and id used for WITHOUT ROWID tables. Empty when no database, no todos table, or the SELECT failed.
    pub rows: Vec<PlanSqlTodosRow>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesGetWorkspaceParams {
    /// Target session identifier
    pub session_id: SessionId,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesGetWorkspaceResultWorkspace {
    /// Current Git branch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Whether the per-session Chronicle upgrade prompt was dismissed for the workspace.
    #[serde(
        rename = "chronicle_sync_dismissed",
        skip_serializing_if = "Option::is_none"
    )]
    pub chronicle_sync_dismissed: Option<bool>,
    /// Name of the client that created the workspace.
    #[serde(rename = "client_name", skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Timestamp when the workspace was created.
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    /// Current working directory associated with the workspace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Git repository root associated with the workspace.
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspacesWorkspaceDetailsHostType>,
    /// Stable workspace identifier.
    pub id: String,
    /// Most recent Mission Control event identifier observed for the workspace.
    #[serde(rename = "mc_last_event_id", skip_serializing_if = "Option::is_none")]
    pub mc_last_event_id: Option<String>,
    /// Mission Control session identifier associated with the workspace.
    #[serde(rename = "mc_session_id", skip_serializing_if = "Option::is_none")]
    pub mc_session_id: Option<String>,
    /// Mission Control task identifier associated with the workspace.
    #[serde(rename = "mc_task_id", skip_serializing_if = "Option::is_none")]
    pub mc_task_id: Option<String>,
    /// Workspace display name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Whether the workspace session can be steered remotely.
    #[serde(rename = "remote_steerable", skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
    /// Repository identifier associated with the workspace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Number of persisted summaries in the workspace.
    #[serde(rename = "summary_count", skip_serializing_if = "Option::is_none")]
    pub summary_count: Option<i64>,
    /// Timestamp when the workspace was last updated.
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    /// Whether the workspace name was explicitly chosen by the user.
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Current workspace metadata for the session, including its absolute filesystem path when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesGetWorkspaceResult {
    /// Absolute filesystem path to the workspace directory. Omitted when the session has no workspace (e.g. remote sessions).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Current workspace metadata, or null if not available
    pub workspace: Option<SessionWorkspacesGetWorkspaceResultWorkspace>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesUpdateMetadataResultWorkspace {
    /// Current Git branch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Whether the per-session Chronicle upgrade prompt was dismissed for the workspace.
    #[serde(
        rename = "chronicle_sync_dismissed",
        skip_serializing_if = "Option::is_none"
    )]
    pub chronicle_sync_dismissed: Option<bool>,
    /// Name of the client that created the workspace.
    #[serde(rename = "client_name", skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Timestamp when the workspace was created.
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    /// Current working directory associated with the workspace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Git repository root associated with the workspace.
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspacesWorkspaceDetailsHostType>,
    /// Stable workspace identifier.
    pub id: String,
    /// Most recent Mission Control event identifier observed for the workspace.
    #[serde(rename = "mc_last_event_id", skip_serializing_if = "Option::is_none")]
    pub mc_last_event_id: Option<String>,
    /// Mission Control session identifier associated with the workspace.
    #[serde(rename = "mc_session_id", skip_serializing_if = "Option::is_none")]
    pub mc_session_id: Option<String>,
    /// Mission Control task identifier associated with the workspace.
    #[serde(rename = "mc_task_id", skip_serializing_if = "Option::is_none")]
    pub mc_task_id: Option<String>,
    /// Workspace display name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Whether the workspace session can be steered remotely.
    #[serde(rename = "remote_steerable", skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
    /// Repository identifier associated with the workspace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Number of persisted summaries in the workspace.
    #[serde(rename = "summary_count", skip_serializing_if = "Option::is_none")]
    pub summary_count: Option<i64>,
    /// Timestamp when the workspace was last updated.
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    /// Whether the workspace name was explicitly chosen by the user.
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Current workspace metadata for the session, including its absolute filesystem path when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesUpdateMetadataResult {
    /// Absolute filesystem path to the workspace directory. Omitted when the session has no workspace (e.g. remote sessions).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Current workspace metadata, or null if not available
    pub workspace: Option<SessionWorkspacesUpdateMetadataResultWorkspace>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesEnsureResultWorkspace {
    /// Current Git branch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Whether the per-session Chronicle upgrade prompt was dismissed for the workspace.
    #[serde(
        rename = "chronicle_sync_dismissed",
        skip_serializing_if = "Option::is_none"
    )]
    pub chronicle_sync_dismissed: Option<bool>,
    /// Name of the client that created the workspace.
    #[serde(rename = "client_name", skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Timestamp when the workspace was created.
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    /// Current working directory associated with the workspace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Git repository root associated with the workspace.
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspacesWorkspaceDetailsHostType>,
    /// Stable workspace identifier.
    pub id: String,
    /// Most recent Mission Control event identifier observed for the workspace.
    #[serde(rename = "mc_last_event_id", skip_serializing_if = "Option::is_none")]
    pub mc_last_event_id: Option<String>,
    /// Mission Control session identifier associated with the workspace.
    #[serde(rename = "mc_session_id", skip_serializing_if = "Option::is_none")]
    pub mc_session_id: Option<String>,
    /// Mission Control task identifier associated with the workspace.
    #[serde(rename = "mc_task_id", skip_serializing_if = "Option::is_none")]
    pub mc_task_id: Option<String>,
    /// Workspace display name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Whether the workspace session can be steered remotely.
    #[serde(rename = "remote_steerable", skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
    /// Repository identifier associated with the workspace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Number of persisted summaries in the workspace.
    #[serde(rename = "summary_count", skip_serializing_if = "Option::is_none")]
    pub summary_count: Option<i64>,
    /// Timestamp when the workspace was last updated.
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    /// Whether the workspace name was explicitly chosen by the user.
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Current workspace metadata for the session, including its absolute filesystem path when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesEnsureResult {
    /// Absolute filesystem path to the workspace directory. Omitted when the session has no workspace (e.g. remote sessions).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Current workspace metadata, or null if not available
    pub workspace: Option<SessionWorkspacesEnsureResultWorkspace>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesListFilesParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Relative paths of files stored in the session workspace files directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesListFilesResult {
    /// Slash-separated relative file paths in the workspace files directory
    pub files: Vec<String>,
}

/// Contents of the requested workspace file as a UTF-8 string.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesReadFileResult {
    /// File content as a UTF-8 string
    pub content: String,
}

/// Filesystem metadata for a path in the session workspace files directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesStatFileResult {
    /// Creation time in Unix epoch milliseconds
    pub birthtime_ms: f64,
    /// Whether the path identifies a directory
    pub is_directory: bool,
    /// Whether the path identifies a regular file
    pub is_file: bool,
    /// Last modification time in Unix epoch milliseconds
    pub mtime_ms: f64,
    /// Size in bytes
    pub size: f64,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesListCheckpointsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Workspace checkpoints in chronological order; empty when the workspace is not enabled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesListCheckpointsResult {
    /// Workspace checkpoints in chronological order. Empty when workspace is not enabled.
    pub checkpoints: Vec<WorkspacesCheckpoints>,
}

/// Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesReadCheckpointResult {
    /// Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing
    pub content: Option<String>,
}

/// Persisted summary metadata and refreshed workspace metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesAddSummaryResult {
    /// Metadata for the persisted summary.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<serde_json::Value>,
    /// Refreshed metadata for the containing workspace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub workspace: Option<serde_json::Value>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesTruncateSummariesResultWorkspace {
    /// Current Git branch.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Whether the per-session Chronicle upgrade prompt was dismissed for the workspace.
    #[serde(
        rename = "chronicle_sync_dismissed",
        skip_serializing_if = "Option::is_none"
    )]
    pub chronicle_sync_dismissed: Option<bool>,
    /// Name of the client that created the workspace.
    #[serde(rename = "client_name", skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Timestamp when the workspace was created.
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    /// Current working directory associated with the workspace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Git repository root associated with the workspace.
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspacesWorkspaceDetailsHostType>,
    /// Stable workspace identifier.
    pub id: String,
    /// Most recent Mission Control event identifier observed for the workspace.
    #[serde(rename = "mc_last_event_id", skip_serializing_if = "Option::is_none")]
    pub mc_last_event_id: Option<String>,
    /// Mission Control session identifier associated with the workspace.
    #[serde(rename = "mc_session_id", skip_serializing_if = "Option::is_none")]
    pub mc_session_id: Option<String>,
    /// Mission Control task identifier associated with the workspace.
    #[serde(rename = "mc_task_id", skip_serializing_if = "Option::is_none")]
    pub mc_task_id: Option<String>,
    /// Workspace display name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Whether the workspace session can be steered remotely.
    #[serde(rename = "remote_steerable", skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
    /// Repository identifier associated with the workspace.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Number of persisted summaries in the workspace.
    #[serde(rename = "summary_count", skip_serializing_if = "Option::is_none")]
    pub summary_count: Option<i64>,
    /// Timestamp when the workspace was last updated.
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    /// Whether the workspace name was explicitly chosen by the user.
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Current workspace metadata for the session, including its absolute filesystem path when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesTruncateSummariesResult {
    /// Absolute filesystem path to the workspace directory. Omitted when the session has no workspace (e.g. remote sessions).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Current workspace metadata, or null if not available
    pub workspace: Option<SessionWorkspacesTruncateSummariesResultWorkspace>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesReadAutopilotObjectiveParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Autopilot objective file content, or null when missing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesReadAutopilotObjectiveResult {
    /// Autopilot objective file content, or null when missing.
    pub content: Option<String>,
}

/// Result of writing the autopilot objective file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesWriteAutopilotObjectiveResult {
    /// Filesystem operation performed.
    pub operation: String,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesDeleteAutopilotObjectiveParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Result of deleting the autopilot objective file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesDeleteAutopilotObjectiveResult {
    /// True when a file was deleted.
    pub deleted: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesAutopilotObjectiveExistsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Whether the autopilot objective file exists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesAutopilotObjectiveExistsResult {
    /// True when the objective file exists.
    pub exists: bool,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesSaveLargePasteResultSaved {
    /// Filename within the workspace files directory
    pub filename: String,
    /// Absolute filesystem path to the saved paste file
    pub file_path: String,
    /// Size of the saved file in bytes
    pub size_bytes: i64,
}

/// Descriptor for the saved paste file, or null when the workspace is unavailable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesSaveLargePasteResult {
    /// Saved-paste descriptor, or null when the workspace is unavailable (e.g. CCA runtime, non-infinite sessions, remote sessions)
    pub saved: Option<SessionWorkspacesSaveLargePasteResultSaved>,
}

/// Workspace diff result for the requested mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesDiffResult {
    /// Default branch used for a branch diff, when branch mode was requested.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_branch: Option<String>,
    /// Changed files and their unified diffs.
    pub changes: Vec<WorkspaceDiffFileChange>,
    /// Whether the requested diff fell back to unstaged changes, either because branch diff failed or session diff was unavailable.
    pub is_fallback: bool,
    /// Effective mode used for the returned changes.
    pub mode: WorkspaceDiffMode,
    /// Diff mode requested by the client.
    pub requested_mode: WorkspaceDiffMode,
    /// Why the session diff could not be produced, when applicable. Set only when `session` mode was requested and `isFallback` is true, so a client can tell the permanent `file-change-tracking-disabled` apart from the transient `session-busy`, which the same request answers once the session settles. Never set for `unstaged` or `branch` mode, and never `unsupported-remote-session`: a remote session's captures live on its own host, so a `session`-mode diff is rejected for one rather than answered with a controller-side fallback.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unavailable_reason: Option<HistoryRewindUnavailableReason>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAutopilotObjectiveGetStateParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Canonical runtime state for the session's current autopilot objective.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAutopilotObjectiveGetStateResult {
    /// Current objective state, or `null` when the session has no objective.
    pub state: Option<AutopilotObjectiveState>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAutopilotEvaluateIdleContinuationParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Result of applying the ordinary session-idle Autopilot continuation decision.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAutopilotEvaluateIdleContinuationResult {
    /// Applied continuation outcome.
    pub outcome: AutopilotEvaluateIdleContinuationOutcome,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCompletionsGetTriggerCharactersParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Characters that, when typed in the composer, should trigger a `completions.request`. Empty when the session has no host-driven completions (e.g. local sessions, or a relay host that does not advertise `completionTriggerCharacters`).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCompletionsGetTriggerCharactersResult {
    /// Trigger characters advertised by the host (e.g. `["@", "#"]`). Empty disables host-driven completions for the session.
    pub trigger_characters: Vec<String>,
}

/// Host-driven completion items for the current composer input. Empty when the host returns no items or does not support completions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCompletionsRequestResult {
    /// Completion items in host-ranked order.
    pub items: Vec<SessionCompletionItem>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstructionsGetSourcesParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Instruction sources loaded for the session, in merge order.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstructionsGetSourcesResult {
    /// Instruction sources for the session
    pub sources: Vec<InstructionSource>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstructionsReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCustomizationsReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether fleet mode was successfully activated.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFleetStartResult {
    /// Whether fleet mode was successfully activated
    pub started: bool,
}

/// Agents available to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentListResult {
    /// Available agents
    pub agents: Vec<AgentInfo>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentGetCurrentParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The currently selected custom agent, or null when using the default agent.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentGetCurrentResult {
    /// Currently selected custom agent, or null if using the default agent
    pub agent: AgentInfo,
}

/// The newly selected custom agent.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentSelectResult {
    /// The newly selected custom agent
    pub agent: AgentInfo,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentDeselectParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Custom agents available to the session after reloading definitions from disk.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentReloadResult {
    /// Reloaded custom agents
    pub agents: Vec<AgentInfo>,
}

/// Identifier assigned to the newly started background agent task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksStartAgentResult {
    /// Generated agent ID for the background task
    pub agent_id: String,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Background tasks currently tracked by the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksListResult {
    /// Currently tracked tasks
    pub tasks: Vec<serde_json::Value>,
}

/// Result of registering or reclaiming a client-owned task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksRegisterResult {
    /// True only when this invocation created a new task
    pub created: bool,
    /// True only when this invocation reclaimed an orphaned task
    pub reclaimed: bool,
    /// Authoritative registered or reclaimed task
    pub task: TaskClientInfo,
}

/// Result of publishing a client-owned task update.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksUpdateResult {
    /// Whether this invocation changed task state
    pub applied: bool,
    /// Whether this invocation repeated the latest accepted update
    pub duplicate: bool,
    /// Authoritative task after processing the update
    pub task: TaskClientInfo,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksRefreshParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Refresh metadata for any detached background shells the runtime knows about. Use after a long pause to pick up exit/output state for shells running outside the agent loop.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksRefreshResult {}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksWaitForPendingParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Wait until all in-flight background tasks (agents + shells) and any follow-up turns scheduled by their completions have settled. Returns when the runtime is fully drained or after an internal timeout (default 10 minutes; configurable via COPILOT_TASK_WAIT_TIMEOUT_SECONDS).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksWaitForPendingResult {}

/// Progress information for the task, or null when no task with that ID is tracked.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksGetProgressResult {
    /// Progress information for the task, discriminated by type. Returns null when no task with this ID is currently tracked.
    pub progress: serde_json::Value,
}

/// Output a detached shell task wrote after the request cursor, with the cursor for the next read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksReadOutputResult {
    /// Opaque cursor to pass to the next read of this task. Always present, even when no output was returned.
    pub cursor: String,
    /// Present once the shell has finished and this read reaches the end of its output. Later reads with the returned cursor return no new text and the same exit details.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exit: Option<TaskShellOutputExit>,
    /// Output after the request cursor, decoded as UTF-8 and redacted. At most 64 KiB per read, so a large backlog takes several reads; the text never splits a character, and text that stops before the end of the output ends at a line break when it contains one. Empty when no new output is available. Detached shells write stdout and stderr to one output, so the text does not separate them.
    pub text: String,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksGetCurrentPromotableParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The first sync-waiting task that can currently be promoted to background mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksGetCurrentPromotableResult {
    /// The first sync-waiting task (agent first, then shell) that can currently be promoted to background mode. Omitted if no such task exists. The returned task is guaranteed to have executionMode='sync' and canPromoteToBackground=true at the time of the call.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task: Option<serde_json::Value>,
}

/// Indicates whether the task was successfully promoted to background mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksPromoteToBackgroundResult {
    /// Whether the task was successfully promoted to background mode
    pub promoted: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksPromoteCurrentToBackgroundParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The promoted task as it now exists in background mode, omitted if no promotable task was waiting.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksPromoteCurrentToBackgroundResult {
    /// The promoted task as it now exists in background mode, omitted if no promotable task was waiting. Atomic operation: avoids the race window of getCurrentPromotable + promoteToBackground.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task: Option<serde_json::Value>,
}

/// Indicates whether the background task was successfully cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksCancelResult {
    /// Whether the task was successfully cancelled
    pub cancelled: bool,
}

/// Indicates whether the task was removed. False when the task does not exist or is still running/idle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksRemoveResult {
    /// Whether the task was removed. Returns false if the task does not exist or is still running/idle (cancel it first).
    pub removed: bool,
}

/// Indicates whether the message was delivered, with an error message when delivery failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksSendMessageResult {
    /// Error message if delivery failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether the message was successfully delivered or steered
    pub sent: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Skills available to the session, with their enabled state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsListResult {
    /// Available skills
    pub skills: Vec<Skill>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsGetInvokedParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Skills invoked during this session, ordered by invocation time (most recent last).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsGetInvokedResult {
    /// Skills invoked during this session, ordered by invocation time (most recent last)
    pub skills: Vec<SkillsInvokedSkill>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Diagnostics from reloading skill definitions, with warnings and errors as separate lists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsReloadResult {
    /// Errors emitted while loading skills (e.g. skills that failed to load entirely)
    pub errors: Vec<String>,
    /// Warnings emitted while loading skills (e.g. skills that loaded but had issues)
    pub warnings: Vec<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsEnsureLoadedParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// MCP servers configured for the session, with their connection status and host-level state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpListResult {
    /// Host-level state, omitted when no MCP host is initialized.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<McpHostState>,
    /// Configured MCP servers
    pub servers: Vec<McpServer>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpListConfiguredParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Effective MCP configuration with optional live observations from matching already materialized servers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpListConfiguredResult {
    /// Effective configured MCP servers.
    pub servers: Vec<McpConfiguredServer>,
}

/// Tools exposed by the connected MCP server. Throws when the server is not connected.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpListToolsResult {
    /// Tools exposed by the server.
    pub tools: Vec<McpTools>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpMoveLoadingToBackgroundParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Result of moving in-flight MCP loading to the background.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpMoveLoadingToBackgroundResult {
    /// Whether an in-flight MCP load was moved to the background, releasing turns that were waiting on it. False when no MCP load was in flight or the waiting turns had already been released.
    pub moved_to_background: bool,
}

/// MCP server startup filtering result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpReloadWithConfigResult {
    /// Non-default servers allowed by policy
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allowed_servers: Option<Vec<McpAllowedServer>>,
    /// Servers whose connection attempt failed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub failed_servers: Option<Vec<McpFailedServer>>,
    /// Servers filtered out before startup
    pub filtered_servers: Vec<McpFilteredServer>,
}

/// Outcome of an MCP sampling execution: success result, failure error, or cancellation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpExecuteSamplingResult {
    /// Outcome of the sampling inference. 'success' produced a response; 'failure' encountered an error (including agent-side rejection by content filter or criteria); 'cancelled' the caller cancelled this execution via cancelSamplingExecution.
    pub action: McpSamplingExecutionAction,
    /// Error description, present when action='failure'.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// MCP CreateMessageResult payload (with optional 'tools' extension), present when action='success'. Treated as opaque at the schema layer; consumers should construct/consume it per the MCP CreateMessageResult shape.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<McpExecuteSamplingResult>,
}

/// Indicates whether an in-flight sampling execution with the given requestId was found and cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpCancelSamplingExecutionResult {
    /// True if an in-flight execution with the given requestId was found and signalled to cancel. False when no such execution is in flight (already completed, never started, or cancelled by another caller).
    pub cancelled: bool,
}

/// Env-value mode recorded on the session after the update.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpSetEnvValueModeResult {
    /// Mode recorded on the session after the update
    pub mode: McpSetEnvValueModeDetails,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpRemoveGitHubParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether the auto-managed `github` MCP server was removed (false when nothing to remove).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpRemoveGitHubResult {
    /// True when the auto-managed `github` MCP server was removed; false when no removal happened (e.g. user has explicitly configured a `github` server, or the server was not registered).
    pub removed: bool,
}

/// Result of configuring GitHub MCP.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpConfigureGitHubResult {
    /// Whether GitHub MCP configuration changed.
    pub changed: bool,
}

/// Whether the named MCP server is running.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpIsServerRunningResult {
    /// True if the server has an active client and transport.
    pub running: bool,
}

/// Indicates whether the pending MCP OAuth response was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthHandlePendingRequestResult {
    /// Whether the response was accepted. False if the request was unknown, timed out, or already resolved.
    pub success: bool,
}

/// Effect-free preparation bound to the existing local session, requester and installation, with frozen options.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthPrepareLoginParams {
    /// Name recorded by the authoritative owned installation receipt.
    pub server_name: String,
    /// Exact installation identity from owned inventory, never a server-name alias.
    pub expected_installation_id: String,
    /// Request a new authorisation rather than accepting a usable cached grant.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force_reauth: Option<bool>,
    /// Display name used by the incumbent OAuth client-registration flow.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Text shown on the loopback callback page after successful authorisation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub callback_success_message: Option<String>,
}

/// An inert runtime-issued login handle. Preparation alone performs no activation or OAuth work.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthPrepareLoginResult {
    /// Original expiry, not extended by consumption, retries or cancellation.
    pub expires_at: String,
    /// Retain with the original requester and use for one login or cancellation.
    pub login_id: String,
}

/// OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthLoginResult {
    /// Opaque authorization identifier returned only for a host-managed redirect URI. The runtime also sends it as the OAuth state value, so the callback endpoint can read state and pass it with the full callback URL to session.mcp.oauth.complete.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_id: Option<String>,
    /// URL the caller should open in a browser to complete OAuth. Omitted when cached tokens were still valid and no browser interaction was needed — the server is already reconnected in that case. For the default loopback flow, the runtime starts its listener before returning. With redirectUri, the host receives the callback and completes it through session.mcp.oauth.complete. The runtime continues the flow in the background and signals completion via session.mcp_server_status_changed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_url: Option<String>,
    /// Runtime-issued owned flow identity; never a server name or installation operation ID.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login_id: Option<String>,
    /// Explicit outcome for owned sign-in. Manual callers retain their legacy response shape.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<McpOwnedOauthLoginStatus>,
}

/// Targets only the original prepared/applying owned login on this exact session requester.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthCancelLoginParams {
    /// The same authoritative installation identity supplied during preparation.
    pub expected_installation_id: String,
    /// Runtime-issued login handle known before the effectful login request begins.
    pub login_id: String,
}

/// Honest terminal cancellation result; persistence or recovery failures remain RPC errors.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthCancelLoginResult {
    /// True after cancellation settles, false when the original login already connected successfully.
    pub cancelled: bool,
}

/// Indicates whether the pending MCP OAuth response was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthRespondResult {
    /// Whether the response was accepted. False if the request was unknown, timed out, or already resolved.
    pub success: bool,
}

/// Indicates whether the pending MCP headers refresh response was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpHeadersHandlePendingHeadersRefreshRequestResult {
    /// Whether the response was accepted. False if the request was unknown, timed out, or already resolved.
    pub success: bool,
}

/// Resource contents returned by the MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpAppsReadResourceResult {
    /// Resource contents returned by the server
    pub contents: Vec<McpAppsResourceContent>,
}

/// App-callable tools from the named MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpAppsListToolsResult {
    /// App-callable tools from the server
    pub tools: Vec<HashMap<String, serde_json::Value>>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpAppsGetHostContextParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Current host context advertised to MCP App guests.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpAppsGetHostContextResult {
    /// Current host context
    pub context: McpAppsHostContextDetails,
}

/// Diagnostic snapshot of MCP Apps wiring for the named server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpAppsDiagnoseResult {
    /// Capability negotiation snapshot
    pub capability: McpAppsDiagnoseCapability,
    /// What the server returned for this session
    pub server: McpAppsDiagnoseServer,
}

/// Resource contents returned by the MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpResourcesReadResult {
    /// Resource contents returned by the server
    pub contents: Vec<McpResourceContent>,
}

/// One page of resources advertised by the named MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpResourcesListResult {
    /// Opaque cursor for the next page, if the server has more resources
    #[serde(skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
    /// Resources advertised by the server (proxied MCP `resources/list`)
    pub resources: Vec<McpResource>,
}

/// One page of resource templates advertised by the named MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpResourcesListTemplatesResult {
    /// Opaque cursor for the next page, if the server has more resource templates
    #[serde(skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
    /// Resource templates advertised by the server (proxied MCP `resources/templates/list`)
    pub resource_templates: Vec<McpResourceTemplate>,
}

/// One page of prompts advertised by the named MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpPromptsListResult {
    /// MCP result metadata
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// Server-provided non-standard result fields
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_properties: Option<HashMap<String, serde_json::Value>>,
    /// Opaque cursor for the next page, if the server has more prompts
    #[serde(skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
    /// Prompts advertised by the server
    pub prompts: Vec<McpPrompt>,
}

/// Prompt messages returned by the MCP server without sending them to the model.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpPromptsGetResult {
    /// MCP result metadata
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// Server-provided non-standard result fields
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_properties: Option<HashMap<String, serde_json::Value>>,
    /// Description of the prompt
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Ordered prompt messages
    pub messages: Vec<McpPromptMessage>,
}

/// Per-source session diagnostics configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionDiagnosticsConfigureResult {
    /// Diagnostic thresholds keyed by supported source.
    pub sources: DiagnosticSourcesConfiguration,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionDiagnosticsReadResultEntriesItem {
    /// Agent identifier for a subagent host. Omitted for the root agent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_id: Option<String>,
    /// Typed MCP diagnostic detail.
    pub details: McpDiagnosticDetails,
    /// Severity of this emitted diagnostic record.
    pub level: DiagnosticSeverity,
    /// Human-readable diagnostic summary.
    pub message: String,
    /// Original byte count when a known-size message or data value was truncated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub original_bytes: Option<i64>,
    /// Diagnostic source identifying the typed details payload.
    pub source: SessionDiagnosticsReadResultEntriesItemSource,
    /// UTC RFC 3339 timestamp captured at the diagnostic source.
    pub timestamp: String,
    /// Whether message or data was truncated to the record-size bound.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub truncated: Option<bool>,
}

/// One cursor-addressed page of retained session diagnostics.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionDiagnosticsReadResult {
    /// Opaque cursor for the next independent read.
    pub cursor: String,
    /// Whether the requested cursor remained within the retained buffer window.
    pub cursor_status: DiagnosticCursorStatus,
    /// Number of records lost before this page when known. Omitted when a buffer generation change makes the count unknowable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub dropped_count: Option<i64>,
    /// Retained records beginning at the requested cursor.
    pub entries: Vec<SessionDiagnosticsReadResultEntriesItem>,
    /// Whether additional retained records follow this page.
    pub has_more: bool,
}

/// One cursor-addressed page of retained experimental runtime lifecycle diagnostic events.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SessionDiagnosticsLifecycleReadResult {
    /// Opaque cursor for the next independent lifecycle diagnostics read.
    pub cursor: String,
    /// Whether the requested cursor remained within the retained activation window.
    pub cursor_status: DiagnosticCursorStatus,
    /// Number of events lost before this page when known. Omitted when an activation change makes the count unknowable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub dropped_count: Option<i64>,
    /// Retained lifecycle diagnostic events beginning at the requested cursor.
    pub events: Vec<LifecycleDiagnosticEvent>,
    /// Whether additional retained lifecycle diagnostic events follow this page.
    pub has_more: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConnectorsGetCapabilitiesParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Feature detection and hard polling limits for the EXPERIMENTAL session connector API.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConnectorsGetCapabilitiesResult {
    /// Connector API contract version.
    pub api_version: i64,
    /// Current session availability. Disabled availability is reported without making a Connector request.
    pub availability: ConnectorAvailability,
    /// Whether connect and reconnect can return an opaque continuation for bounded consent polling.
    pub consent_continuation: bool,
    /// Maximum accepted wall-clock deadline in milliseconds for one continuation call.
    pub max_deadline_ms: i64,
    /// Maximum accepted polling attempts for one continuation call.
    pub max_poll_attempts: i64,
    /// Maximum accepted delay in milliseconds between polling attempts.
    pub max_poll_interval_ms: i64,
    /// Whether callers select a host-owned GitHub account through an opaque selection ID rather than supplying a provider token.
    pub opaque_account_selection: bool,
    /// Whether getAccount is supported. Absence means false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_account_selection: Option<bool>,
    /// Whether reconcile accepts forceConnectorName. Absence means false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub targeted_reconcile: Option<bool>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConnectorsGetAccountParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConnectorsGetStatusParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Authoritative session connector state. Account IDs are opaque routing identifiers and credentials are never included.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConnectorsGetStatusResult {
    /// Opaque account selection pinned to this session, when one has been selected.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub account_id: Option<String>,
    /// Connector API contract version.
    pub api_version: i64,
    /// Exact selected account and stable scope requiring an authorization update, when proven by the Connector service.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_requirement: Option<ConnectorAuthorizationRequirement>,
    /// Current feature and session availability.
    pub availability: ConnectorAvailability,
    /// Latest validated catalog snapshot, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub catalog: Option<ConnectorCatalogResult>,
    /// Number of active opaque connection continuations.
    pub pending_connections: i64,
    /// Live MCP status for every Connector-owned runtime server.
    pub runtime_servers: Vec<ConnectorRuntimeStatus>,
}

/// Validated Connector catalog snapshot cached by the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConnectorsListResult {
    /// Validated catalog entries in service order.
    pub connectors: Vec<ConnectorCatalogEntry>,
    /// Unix epoch milliseconds when this snapshot was accepted.
    pub refreshed_at_ms: i64,
    /// Monotonically increasing session-local catalog revision.
    pub revision: i64,
}

/// Validated Connector catalog snapshot cached by the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConnectorsRefreshResult {
    /// Validated catalog entries in service order.
    pub connectors: Vec<ConnectorCatalogEntry>,
    /// Unix epoch milliseconds when this snapshot was accepted.
    pub refreshed_at_ms: i64,
    /// Monotonically increasing session-local catalog revision.
    pub revision: i64,
}

/// Authoritative result after disconnect and MCP reconciliation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConnectorsDisconnectResult {
    /// Whether the service accepted the idempotent disconnect.
    pub disconnected: bool,
    /// Fresh authoritative session state after removing Connector-owned MCP servers.
    pub status: ConnectorStatus,
}

/// Authoritative session connector state. Account IDs are opaque routing identifiers and credentials are never included.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConnectorsReconcileResult {
    /// Opaque account selection pinned to this session, when one has been selected.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub account_id: Option<String>,
    /// Connector API contract version.
    pub api_version: i64,
    /// Exact selected account and stable scope requiring an authorization update, when proven by the Connector service.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_requirement: Option<ConnectorAuthorizationRequirement>,
    /// Current feature and session availability.
    pub availability: ConnectorAvailability,
    /// Latest validated catalog snapshot, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub catalog: Option<ConnectorCatalogResult>,
    /// Number of active opaque connection continuations.
    pub pending_connections: i64,
    /// Live MCP status for every Connector-owned runtime server.
    pub runtime_servers: Vec<ConnectorRuntimeStatus>,
}

/// Authoritative session connector state. Account IDs are opaque routing identifiers and credentials are never included.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConnectorsReconcileForStartupResult {
    /// Opaque account selection pinned to this session, when one has been selected.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub account_id: Option<String>,
    /// Connector API contract version.
    pub api_version: i64,
    /// Exact selected account and stable scope requiring an authorization update, when proven by the Connector service.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_requirement: Option<ConnectorAuthorizationRequirement>,
    /// Current feature and session availability.
    pub availability: ConnectorAvailability,
    /// Latest validated catalog snapshot, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub catalog: Option<ConnectorCatalogResult>,
    /// Number of active opaque connection continuations.
    pub pending_connections: i64,
    /// Live MCP status for every Connector-owned runtime server.
    pub runtime_servers: Vec<ConnectorRuntimeStatus>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConnectorsWithdrawProjectionParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Authoritative session connector state. Account IDs are opaque routing identifiers and credentials are never included.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionConnectorsWithdrawProjectionResult {
    /// Opaque account selection pinned to this session, when one has been selected.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub account_id: Option<String>,
    /// Connector API contract version.
    pub api_version: i64,
    /// Exact selected account and stable scope requiring an authorization update, when proven by the Connector service.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_requirement: Option<ConnectorAuthorizationRequirement>,
    /// Current feature and session availability.
    pub availability: ConnectorAvailability,
    /// Latest validated catalog snapshot, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub catalog: Option<ConnectorCatalogResult>,
    /// Number of active opaque connection continuations.
    pub pending_connections: i64,
    /// Live MCP status for every Connector-owned runtime server.
    pub runtime_servers: Vec<ConnectorRuntimeStatus>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionManagedSettingsGetParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Effective enterprise managed settings and contributing channels. Session events report applied policy; sessionless resolve reports an account/device snapshot, and compose reports a non-applying preview of candidate documents. Device values take precedence over server values, then the policy helper, per ordinary key, while permissions compose restrictively. Session-local SDK-client policy is included only in session results. Marked experimental while the managed-settings surface stabilizes.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionManagedSettingsGetResult {
    /// Whether an explicit enterprise policy restriction disables bypass-permissions ("yolo") mode for this session. Deny-wins across layers; an unresolved policy does not force this on by itself.
    pub bypass_permissions_disabled: bool,
    /// Whether a session-local permissions layer injected by the SDK host was present
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_managed: Option<bool>,
    /// Whether an actual device MDM/plist/registry/file managed-settings layer was present
    pub device_managed: bool,
    /// Whether managed policy could not be determined (e.g. a failed server fetch) and unresolved-policy safeguards remain active. This does not by itself disable bypass-permissions; `bypassPermissionsDisabled` reports only an explicit policy restriction.
    pub fail_closed: bool,
    /// The setting keys under enterprise management in the effective managed settings (e.g. `model`, `enabledPlugins`, `permissions`). Empty when no managed settings are in force.
    pub managed_keys: Vec<String>,
    /// Whether at least two managed sources supplied permission allowlists, so enforcement intersects them and the flattened settings payload omits `permissions.allow`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permissions_allow_intersected: Option<bool>,
    /// Reusable retained managed permission policy for managedSettings.permissions.evaluate, including source composition and session-local injection. Provided by live session snapshots; absent in older events. Refresh the context when managed policy changes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permissions_context: Option<ManagedPermissionsContext>,
    /// Explicit Policy Helper structured-failure and recheck capabilities supported by this runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_capabilities: Option<PolicyHelperCapabilities>,
    /// The latest unresolved Policy Helper failure for this exact policy context. It remains present across retained-policy refresh failures and clears only after fresh success or authoritative removal.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_failure: Option<PolicyHelperFailure>,
    /// Whether the policy-helper managed-settings layer was present. The policy helper is the weakest channel: it fills keys no enterprise source set and can never replace one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_helper_managed: Option<bool>,
    /// Whether the effective sandbox policy forces the sandbox on *only* because managed policy could not be determined, rather than because the policy requires it. Lets clients tell a user whose `--no-sandbox` was overridden that the sandbox stayed on as a fail-closed fallback, instead of attributing it to an administrator who set no such policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_enabled_by_undetermined_policy: Option<bool>,
    /// Whether the server (account/org) managed-settings layer was present
    pub server_managed: bool,
    /// The effective (resolved) managed settings values, so clients can render exactly what is enforced. Absent when no managed policy is in force.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub settings: Option<serde_json::Value>,
    /// Channel summary: `server`, `device`, `client`, or `policyHelper` when exactly one channel contributed; `mixed` when multiple channels contributed; otherwise `none`. Consult the per-channel booleans for exact provenance.
    pub source: ManagedSettingsResolvedSource,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionManagedSettingsRecheckPolicyHelperParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Plugins installed for the session, with their enabled state and version metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsListResult {
    /// Installed plugins
    pub plugins: Vec<Plugin>,
}

/// Result of installing a plugin.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsInstallResult {
    /// Set when the install path is deprecated (e.g. direct repo / URL / local installs). Callers should surface this to end users.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deprecation_warning: Option<String>,
    /// The newly installed plugin's metadata
    pub plugin: InstalledPluginInfo,
    /// Optional post-install message provided by the plugin (e.g. setup instructions)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub post_install_message: Option<String>,
    /// Number of skills discovered and installed from the plugin
    pub skills_installed: i64,
    /// Where the completed plugin tree was staged before atomic promotion
    #[serde(skip_serializing_if = "Option::is_none")]
    pub staging_mode: Option<PluginInstallStagingMode>,
}

/// Result of updating a single plugin.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsUpdateResult {
    /// Version after the update, when reported by the plugin manifest
    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_version: Option<String>,
    /// Version that was previously installed, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_version: Option<String>,
    /// Number of skills discovered and installed after the update
    pub skills_installed: i64,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsMarketplacesListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// All registered marketplaces, including built-in defaults.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsMarketplacesListResult {
    /// Registered marketplaces
    pub marketplaces: Vec<MarketplaceInfo>,
}

/// Result of registering a new marketplace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsMarketplacesAddResult {
    /// Final name of the marketplace as resolved from its manifest
    pub name: String,
}

/// Outcome of the remove attempt, including dependent-plugin info when applicable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsMarketplacesRemoveResult {
    /// Names of installed plugins that prevented removal. Populated only when `removed=false`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub dependent_plugins: Option<Vec<String>>,
    /// True when the marketplace was actually removed. False when removal was skipped because the marketplace has dependent plugins and `force` was not set.
    pub removed: bool,
}

/// Plugins advertised by the marketplace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsMarketplacesBrowseResult {
    /// Plugins advertised by the marketplace
    pub plugins: Vec<MarketplacePluginInfo>,
}

/// Result of refreshing one or more marketplace catalogs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsMarketplacesRefreshResult {
    /// Per-marketplace refresh results in deterministic order.
    pub results: Vec<MarketplaceRefreshEntry>,
}

/// A snapshot of the provider endpoint the session is currently configured to talk to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionProviderGetEndpointResult {
    /// A credential the caller should use with this endpoint. Omitted only when the endpoint accepts unauthenticated requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_key: Option<String>,
    /// Base URL to pass to the LLM client library.
    pub base_url: String,
    /// HTTP headers the caller must include on every outbound request.
    pub headers: HashMap<String, String>,
    /// Short-lived, rotating credential the caller must send on every request, in addition to `apiKey` if one is present. Omitted when the endpoint does not require one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_token: Option<ProviderSessionToken>,
    /// Transport to be used for provider requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub transport: Option<ProviderEndpointTransport>,
    /// Provider family. Matches the `type` field of a BYOK provider config.
    pub r#type: ProviderEndpointType,
    /// Wire API to be used, when required for the provider type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_api: Option<ProviderEndpointWireApi>,
}

/// The selectable model entries synthesized for the models added by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionProviderAddResult {
    /// Synthesized selectable model entries for the newly added BYOK models, each under its provider-qualified selection id (`provider/id`). Empty when only providers were added.
    pub models: Vec<serde_json::Value>,
}

/// The selectable model entries and selection ids synthesized for the synchronized BYOK models.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionProviderSyncResult {
    /// True when synchronization withdrew the selected host-managed model, leaving the session with no explicit selection, so ordinary model resolution picks the session default. Synchronization never promotes a surviving host model in its place: publishing a model offers it, and the choice of which model to use stays with the user.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_deselected: Option<bool>,
    /// Synthesized selectable model entries for the synchronized BYOK models.
    pub models: Vec<serde_json::Value>,
    /// Provider-qualified model selection ids present after synchronization.
    pub selection_ids: Vec<String>,
}

/// What the withdrawal actually removed from the registry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionProviderWithdrawResult {
    /// True when withdrawal removed the selected host-managed model, leaving the session with no explicit selection, so ordinary model resolution picks the session default. Withdrawal never promotes a surviving model in its place: the choice of which model to use stays with the user.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_deselected: Option<bool>,
    /// Providers removed because one of the withdrawn models was the last entry referencing them. A provider that merely has no models is not removed.
    pub providers_removed: Vec<String>,
    /// Selection ids that were registered and are now withdrawn. Excludes requested ids that were not present.
    pub withdrawn: Vec<String>,
}

/// Indicates whether the session options patch was applied successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionOptionsUpdateResult {
    /// Number of hooks loaded from installed plugins, returned when installedPlugins is updated
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugin_hook_count: Option<i64>,
    /// Whether the operation succeeded
    pub success: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsGetPolicyParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Runtime support and session-context standalone executable extension policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsGetPolicyResult {
    /// Executable entries discovered from this session's runtime-verified eligible plugins. Under plugin_only, independently launched targets must match these entry paths; an external package, a caller-supplied plugin label, or a synthetic transport bootstrap does not establish eligibility. Empty while pending or when no extension controller is configured.
    pub eligible_plugin_entries: Vec<EligiblePluginExtensionEntry>,
    /// Extension policy enforcement contract version. Version 1 includes standalone discovery, launch admission, and live-policy revocation.
    pub enforcement_version: i64,
    /// Whether standalone extensions are allowed, prohibited by plugin-only policy, or pending unresolved managed policy.
    pub standalone: StandaloneExtensionAdmission,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Extensions discovered for the session, with their current status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsListResult {
    /// Discovered extensions and their current status
    pub extensions: Vec<Extension>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsReconcileParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Extensions discovered for the session, with their current status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsReconcileResult {
    /// Discovered extensions and their current status
    pub extensions: Vec<Extension>,
}

/// Rust-owned built-in tool descriptors for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsGetBuiltinDescriptorsResult {
    /// Built-in tool descriptors materialized for the session.
    pub tools: Vec<BuiltinToolDescriptor>,
}

/// Task completion notification with summary from the agent
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsTaskCompleteEventDataResult {
    /// Permission-protected actions that remained blocked for this genuine-user-message epoch. Present on successful completion when the objective was completed without them and on blocked completion when one or more were required.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub blocked_actions: Option<Vec<BlockedPermissionAction>>,
    /// Structured blocker details when outcome is blocked
    #[serde(skip_serializing_if = "Option::is_none")]
    pub blocker: Option<TaskBlocker>,
    /// Active autopilot objective ID evaluated by the completion reviewer
    #[serde(skip_serializing_if = "Option::is_none")]
    pub objective_id: Option<i64>,
    /// Semantic completion decision. Absent on legacy events and invalid tool calls
    #[serde(skip_serializing_if = "Option::is_none")]
    pub outcome: Option<TaskCompletionOutcome>,
    /// Label-safe runtime rationale for the completion decision (e.g. a cancellation or pause/resume downgrade), when one applies. Reviewer-authored rationale is intentionally omitted here because this event has no IFC label channel; the reviewer's findings remain available through its own labeled sub-agent events
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Whether the task was accepted as complete. False when validation failed or completion was rejected or blocked by the reviewer
    #[serde(skip_serializing_if = "Option::is_none")]
    pub success: Option<bool>,
    /// Summary of the completed task, provided by the agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
}

/// Indicates whether the external tool call result was handled successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsHandlePendingToolCallResult {
    /// Whether the tool call result was handled successfully
    pub success: bool,
}

/// External tool calls still waiting for a result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsListPendingRequestsResult {
    /// External tool calls of the session and its sub-agents that session.tools.handlePendingToolCall would still accept, in the order they were requested. Includes calls that were pending when the session was last suspended and resumed with pending work continued, so a host that did not observe the original external_tool.requested event can answer them.
    pub items: Vec<PendingExternalToolRequest>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsInitializeAndValidateParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Resolve, build, and validate the runtime tool list for this session. Subagent sessions and consumer flows that need an initialized tool set before `send` invoke this. Default base-class implementation is a no-op for sessions that don't support tool validation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsInitializeAndValidateResult {}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsGetCurrentMetadataParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Current lightweight tool metadata snapshot for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsGetCurrentMetadataResult {
    /// Current tool metadata, or null when tools have not been initialized yet
    pub tools: Option<Vec<CurrentToolMetadata>>,
}

/// Empty result after replacing the calling connection's externally implemented tools.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsSetResult {}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsGetSubagentSettingsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Current configured subagent settings for the session, before resolving defaults or inheritance.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsGetSubagentSettingsResult {
    /// Live subagent settings override, or current persisted settings when no override is active; null when no settings block exists
    pub subagents: Option<SubagentSettingsConfig>,
}

/// Empty result after applying subagent settings
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsUpdateSubagentSettingsResult {}

/// Slash commands available in the session, after applying any include/exclude filters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsListResult {
    /// Commands available in this session
    pub commands: Vec<SlashCommandInfo>,
}

/// Whether finalizing the invocation effect succeeded, and the failure reason when it did not.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsFinalizeInvocationEffectResult {
    /// Failure reason when the invocation effect could not be finalized.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether the pending invocation effect was finalized successfully.
    pub success: bool,
}

/// Indicates whether the pending client-handled command was completed successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsHandlePendingCommandResult {
    /// Whether the command was handled successfully
    pub success: bool,
}

/// Error message produced while executing the command, if any.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsExecuteResult {
    /// Error message produced while executing the command, if any. Omitted when the handler succeeded.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

/// Indicates whether the queued-command response was matched to a pending request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsRespondToQueuedCommandResult {
    /// Whether a pending queued command with the given request ID was found and resolved. False when the request was already resolved, cancelled, or unknown.
    pub success: bool,
}

/// Whether the runtime recorded the queued-command wait.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsReportQueuedCommandWaitResult {
    /// False when the queued command is no longer pending or the awaited state is not observed.
    pub recorded: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTelemetryGetEngagementIdParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Telemetry engagement ID for the session, when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTelemetryGetEngagementIdResult {
    /// Current telemetry engagement ID, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub engagement_id: Option<String>,
}

/// Completed transient query. Ordered chunks and the terminal outcome are also delivered through `ui.ephemeral_query` session events while it runs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiEphemeralQueryResult {
    /// Answer returned by the model
    pub answer: String,
}

/// The elicitation response (accept with form values, decline, or cancel)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiElicitationResult {
    /// MCP response metadata.
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
    pub action: UIElicitationResponseAction,
    /// The form values submitted by the user (present when action is 'accept')
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<HashMap<String, serde_json::Value>>,
}

/// Indicates whether the elicitation response was accepted; false if it was already resolved by another client.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandlePendingElicitationResult {
    /// Whether the response was accepted. False if the request was already resolved by another client.
    pub success: bool,
}

/// Indicates whether the elicitation response was accepted; false if it was already resolved by another client.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandleHumanAskUserResult {
    /// Whether the response was accepted. False if the request was already resolved by another client.
    pub success: bool,
}

/// Indicates whether the pending UI request was resolved by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandlePendingUserInputResult {
    /// True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
    pub success: bool,
}

/// Indicates whether the pending UI request was resolved by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandleHumanUserInputResult {
    /// True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
    pub success: bool,
}

/// Indicates whether the pending UI request was resolved by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandlePendingSamplingResult {
    /// True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
    pub success: bool,
}

/// Indicates whether the pending UI request was resolved by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandlePendingAutoModeSwitchResult {
    /// True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
    pub success: bool,
}

/// Indicates whether the pending UI request was resolved by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandlePendingSessionLimitsExhaustedResult {
    /// True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
    pub success: bool,
}

/// Indicates whether the pending UI request was resolved by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandlePendingExitPlanModeResult {
    /// True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
    pub success: bool,
}

/// Indicates whether the pending UI request was resolved by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandleHumanExitPlanModeResult {
    /// True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
    pub success: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiRegisterDirectAutoModeSwitchHandlerParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Register an in-process handler for `auto_mode_switch.requested` events. The caller still attaches the actual listener via the standard event-subscription mechanism; this registration solely tells the server bridge to skip its own dispatch (so a remote client doesn't race the in-process handler for the same requestId).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiRegisterDirectAutoModeSwitchHandlerResult {
    /// Opaque handle representing the registration. Pass this same handle to `unregisterDirectAutoModeSwitchHandler` when the in-process handler is no longer active. Multiple registrations are reference-counted; the server bridge will only dispatch auto-mode-switch requests when no handles are active.
    pub handle: String,
}

/// Indicates whether the handle was active and the registration count was decremented.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiUnregisterDirectAutoModeSwitchHandlerResult {
    /// True if the handle was active and decremented the counter; false if the handle was unknown.
    pub unregistered: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsConfigureResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the permission decision was applied; false when the request was already resolved.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsHandlePendingPermissionRequestResult {
    /// Whether the permission request was handled successfully
    pub success: bool,
}

/// List of pending permission requests reconstructed from event history.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsPendingRequestsResult {
    /// Pending permission prompts reconstructed from the session's event history. Equivalent to the set of `permission.requested` events that have not yet been followed by a matching `permission.completed` event. Used by clients (e.g. the CLI) to hydrate UI for prompts that were emitted before the client attached to the session.
    pub items: Vec<PendingPermissionRequest>,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsSetApproveAllResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the requested permission mode was applied and reports the authoritative post-mutation mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsSetModeResult {
    /// Authoritative permission mode after the mutation
    pub mode: PermissionMode,
    /// Whether the operation succeeded
    pub success: bool,
}

/// Current permission mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsGetModeResult {
    /// Current permission mode
    pub mode: PermissionMode,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsModifyRulesResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsSetRequiredResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsResetSessionApprovalsResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsClearResumeEscalationResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsNotifyPromptShownResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Snapshot of the session's recursive directory grants, exact session-approved paths, and primary working directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsPathsListResult {
    /// All directories currently allowed for tool access on this session.
    pub directories: Vec<String>,
    /// The primary working directory for this session.
    pub primary: String,
    /// Exact paths approved for this session without recursively allowing their descendants.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_approved_paths: Option<Vec<String>>,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsPathsAddResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsPathsUpdatePrimaryResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the supplied path is within the session's allowed directories.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsPathsIsPathWithinAllowedDirectoriesResult {
    /// Whether the path is within the session's allowed directories
    pub allowed: bool,
}

/// Indicates whether the supplied path is within the session's workspace directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsPathsIsPathWithinWorkspaceResult {
    /// Whether the path is within the session workspace directory
    pub allowed: bool,
}

/// Resolved location-permissions key and type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsLocationsResolveResult {
    /// Location key used in the location-permissions store
    pub location_key: String,
    /// Whether the location is a git repo or directory
    pub location_type: PermissionLocationType,
}

/// Summary of persisted location permissions applied to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsLocationsApplyResult {
    /// Number of persisted allowed directories added to the live path manager
    pub applied_directory_count: i64,
    /// Number of location-scoped rules added to the live permission service
    pub applied_rule_count: i64,
    /// Location-scoped rules applied to the live permission service
    pub applied_rules: Vec<PermissionRule>,
    /// Whether a different location was applied since the previous apply call
    pub changed: bool,
    /// Location key used in the location-permissions store
    pub location_key: String,
    /// Whether the location is a git repo or directory
    pub location_type: PermissionLocationType,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsLocationsAddToolApprovalResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Folder trust check result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsFolderTrustIsTrustedResult {
    /// Whether the folder is trusted
    pub trusted: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsFolderTrustAddTrustedResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsUrlsSetUnrestrictedModeResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Identifier of the session event that was emitted for the log message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionLogResult {
    /// The unique identifier of the emitted session event
    pub event_id: String,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataSnapshotParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Public-facing projection of workspace metadata for SDK / TUI consumers
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataSnapshotResultWorkspace {
    /// Branch checked out at session start, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// ISO 8601 timestamp when the workspace was created
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    /// Current working directory at session start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Resolved git root for cwd, if any
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Repository host type, if known
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspaceSummaryHostType>,
    /// Workspace identifier (1:1 with sessionId)
    pub id: String,
    /// Display name for the session, if set
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Repository identifier in 'owner/repo' or 'org/project/repo' format, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// ISO 8601 timestamp when the workspace was last updated
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    /// Whether the display name was explicitly set by the user
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Point-in-time snapshot of slow-changing session identifier and state fields
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataSnapshotResult {
    /// True when the session was detected to be in use by another process at construction time. Local consumers may surface a confirmation prompt before fully attaching. Always false for new sessions.
    pub already_in_use: bool,
    /// Runtime client name associated with the session (telemetry identifier).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot')
    pub current_mode: MetadataSnapshotCurrentMode,
    /// Live indexed-search state for this session activation. Omitted by runtimes that do not expose indexed-search status; absence does not indicate enablement.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub indexed_search: Option<IndexedSearchState>,
    /// User-provided name supplied at session construction (via `--name`), if any. Immutable after construction.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initial_name: Option<String>,
    /// Whether this is a remote session (i.e., one whose runtime executes elsewhere and is steered through this process)
    pub is_remote: bool,
    /// ISO 8601 timestamp of when the session's persisted state was last modified on disk. For new sessions, equals startTime. For resumed sessions, reflects the previous modification time at construction.
    pub modified_time: String,
    /// Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are immutable for the lifetime of the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_metadata: Option<MetadataSnapshotRemoteMetadata>,
    /// Currently selected model identifier, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_model: Option<String>,
    /// The unique identifier of the session
    pub session_id: SessionId,
    /// Current session limits, or null when no limits are active
    pub session_limits: Option<SessionLimitsConfig>,
    /// ISO 8601 timestamp of when the session started
    pub start_time: String,
    /// Short human-readable summary of the session, if known. Omitted when no summary has been generated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    /// Absolute path to the session's current working directory
    pub working_directory: String,
    /// Public-facing workspace metadata for this session, or null if the session has no associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count, internal flags).
    pub workspace: Option<SessionMetadataSnapshotResultWorkspace>,
    /// Absolute path to the session's workspace directory on disk, or null if the session has no associated workspace
    pub workspace_path: Option<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataGetClientMetadataParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataIsProcessingParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether the local session is currently processing a turn or background continuation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataIsProcessingResult {
    /// Whether the session is currently processing user/agent messages. False for non-local sessions (which don't run a local agentic loop). Reflects an in-flight turn or background continuation.
    pub processing: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataActivityParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Current activity flags for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataActivityResult {
    /// Whether an in-flight operation can currently be aborted.
    pub abortable: bool,
    /// Whether the session currently has active work, including running turns or tasks.
    pub has_active_work: bool,
}

/// Token-usage breakdown for the session's current context window
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataContextInfoResultContextInfo {
    /// Output reservation overlapping the displayed prompt allowance plus tokens after the effective input budget's buffer-exhaustion blocking threshold (default 95%).
    pub buffer_tokens: i64,
    /// Token count at which background compaction starts (configurable percentage of promptTokenLimit)
    pub compaction_threshold: i64,
    /// Tokens consumed by user/assistant/tool messages
    pub conversation_tokens: i64,
    /// Provider-owned model display label, or the Auto routing label. Presentation only; modelName remains the tokenization identity.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_model_name: Option<String>,
    /// Advertised prompt allowance for the selected context tier, without adding output tokens. The denominator for context-usage displays.
    pub limit: i64,
    /// Tokens consumed by MCP tool definitions (subset of toolDefinitionsTokens, excludes deferred tools)
    pub mcp_tools_tokens: i64,
    /// The model used for token counting
    pub model_name: String,
    /// Effective input budget: the selected tier's prompt allowance bounded by the combined context ceiling minus the requested output allowance. Uses DEFAULT_TOKEN_LIMIT when limits are unspecified.
    pub prompt_token_limit: i64,
    /// Provider whose catalog supplies this context window. Omitted when provider identity is unavailable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ModelProviderRef>,
    /// Tokens consumed by the system prompt
    pub system_tokens: i64,
    /// Tokens consumed by tool definitions sent to the model (excludes deferred tools)
    pub tool_definitions_tokens: i64,
    /// Sum of system, conversation and tool-definition tokens
    pub total_tokens: i64,
}

/// Token breakdown for the session's current context window, or null if uninitialized.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataContextInfoResult {
    /// Token breakdown for the current context window, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
    pub context_info: Option<SessionMetadataContextInfoResultContextInfo>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataGetContextAttributionParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The six normalized `/context` header buckets, computed from the same tokenization as `entries` so the two never disagree. Convenience rollups: `freeSpace` and `buffer` describe window capacity rather than occupied context, so the values do not sum to `totalTokens`.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataGetContextAttributionResultContextAttributionCategories {
    /// Overlapping output reservation plus post-blocking-threshold buffer.
    pub buffer: i64,
    /// Custom-instructions tokens (0 when none are configured).
    pub custom_instructions: i64,
    /// Remaining unused window capacity (clamped at 0).
    pub free_space: i64,
    /// MCP tool-definition tokens.
    pub mcp_tools: i64,
    /// Conversation (user/assistant/tool) message tokens.
    pub messages: i64,
    /// System prompt tokens, excluding custom instructions.
    pub system_prompt: i64,
    /// Non-MCP tool-definition tokens.
    pub system_tools: i64,
}

/// Successful compaction history for the session.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataGetContextAttributionResultContextAttributionCompactions {
    /// Number of successful compactions in this session.
    pub count: i64,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataGetContextAttributionResultContextAttributionEntriesItem {
    /// Supplementary per-entry metadata (e.g. `messageCount`, `role`, `evictable`, `pluginSource`). Values are stringified; parse as needed and ignore unrecognized keys.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attributes: Option<HashMap<String, String>>,
    /// Identifier for this entry, formed by joining its `kind` and source name (e.g. `tool:bash`, `skill:tmux`, `toolDefinition:bash`); unique within the snapshot. Use it to match the same entry across snapshots, to correlate with other APIs (skill/agent/MCP registries), and as the `parentId` target for nesting. Distinct from the human-facing `label`.
    pub id: String,
    /// Source category for this entry. Not a closed set — tolerate unknown values. Known values today: `skill`, `subagent`, `mcpServer`, `tool`, `system`, `toolDefinition`, `plugin`.
    pub kind: String,
    /// Human-readable display label, e.g. `bash` or `skill: tmux`. Presentation-only; may be localized/reformatted without notice — do not key off it.
    pub label: String,
    /// Optional `id` of the parent entry: e.g. a `plugin` entry parenting its `skill`/`mcpServer` entries, or the `system` entry parenting `toolDefinition` entries. Omitted for top-level entries.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent_id: Option<String>,
    /// Token count currently in context attributable to this entry.
    pub tokens: i64,
}

/// Per-source token attribution snapshot for the current context window. The heaviest individual messages are available separately via `metadata.getContextHeaviestMessages`.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataGetContextAttributionResultContextAttribution {
    /// Output reservation overlapping the displayed prompt allowance plus the tokens past the effective input budget's buffer-exhaustion blocking threshold. Mirrors `SessionContextInfo.bufferTokens`.
    pub buffer_tokens: i64,
    /// The six normalized `/context` header buckets, computed from the same tokenization as `entries` so the two never disagree. Convenience rollups: `freeSpace` and `buffer` describe window capacity rather than occupied context, so the values do not sum to `totalTokens`.
    pub categories: SessionMetadataGetContextAttributionResultContextAttributionCategories,
    /// Successful compaction history for the session.
    pub compactions: SessionMetadataGetContextAttributionResultContextAttributionCompactions,
    /// Token count at which background compaction starts. Mirrors `SessionContextInfo.compactionThreshold`.
    pub compaction_threshold: i64,
    /// Flat list of per-source attribution entries. Group by `kind` and render unrecognized kinds generically. Nesting and rollups are expressed via `parentId`.
    pub entries: Vec<SessionMetadataGetContextAttributionResultContextAttributionEntriesItem>,
    /// Advertised prompt allowance for the selected context tier: the denominator for context-usage displays and capacity for `categories.freeSpace` and `categories.buffer`. Mirrors `SessionContextInfo.limit`.
    pub limit: i64,
    /// The concrete model id the entire breakdown was tokenized against (feeds the per-model token multiplier). Under `Auto` (Free/Student) this is the resolved model, not the literal `auto` sentinel, so totals are not undercounted. A single-model approximation of a potentially multi-model Auto session.
    pub model_id: String,
    /// How `modelId` was chosen. Not a closed set — tolerate unknown values. Known values today: `autoResolved` (the model Auto resolved to), `selected` (the user's explicitly selected model), `default` (a fallback before any model is known).
    pub model_source: String,
    /// Effective input budget after reserving requested output against the combined context ceiling. Mirrors `SessionContextInfo.promptTokenLimit`.
    pub prompt_token_limit: i64,
    /// Total token count of the current context window the entries are measured against (system message + conversation messages + tool definitions — the same total reported by /context). Divide an entry's `tokens` by this to derive its share.
    pub total_tokens: i64,
}

/// Per-source attribution breakdown for the session's current context window, or null if uninitialized.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataGetContextAttributionResult {
    /// Per-source context-window attribution, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
    pub context_attribution: Option<SessionMetadataGetContextAttributionResultContextAttribution>,
}

/// The heaviest individual messages in the session's context window, most-expensive first.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataGetContextHeaviestMessagesResult {
    /// Heaviest messages, most-expensive first.
    pub messages: Vec<ContextHeaviestMessage>,
    /// Total token count of the current context window, so callers can compute each message's share without a second call.
    pub total_tokens: i64,
}

/// Notify the session that its working directory context has changed. Emits a `session.context_changed` event so consumers (telemetry, OTel tracker, ACP, the timeline UI) can react. Use this when the host has detected a cwd/branch/repo change outside the session's normal lifecycle (e.g., after a shell command in interactive mode). For a local session, a report whose `cwd` diverges from the session's current working directory is ignored (the call still succeeds but records nothing and emits no event); move a local session's working directory via `metadata.setWorkingDirectory` instead.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataRecordContextChangeResult {}

/// Update the session's working directory. Used by the host when the user explicitly changes cwd (e.g., the `/cd` slash command). The host is responsible for any related side-effects (file index, etc.); it does NOT change the process working directory (a session's cwd is per-session, not process-global). For local sessions the runtime validates the target first (an absolute path that exists on disk) and re-bases the permission primary directory; a rejected validation fails the call before anything is mutated, persisted, or emitted. Location-scoped permission rules are then re-keyed to the new directory (best-effort). Remote sessions only record the path.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataSetWorkingDirectoryResult {
    /// Working directory after the update
    pub working_directory: String,
}

/// Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataRecomputeContextTokensResult {
    /// Tokens contributed by user/assistant/tool messages (excludes system/developer prompts).
    pub messages_token_count: i64,
    /// Tokens contributed by system/developer prompt snapshots.
    pub system_token_count: i64,
    /// Sum of tokens across chat-context and system-context messages currently held by the session.
    pub total_tokens: i64,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSettingsSnapshotParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Redacted, serializable view of session runtime settings for SDK boundary consumers. Secrets and raw feature flags are intentionally excluded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSettingsSnapshotResult {
    /// Name of the SDK client that created the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Redacted job settings.
    pub job: SessionSettingsJobSnapshot,
    /// Redacted model routing settings.
    pub model: SessionSettingsModelSnapshot,
    /// Online-evaluation settings safe for SDK consumers.
    pub online_evaluation: SessionSettingsOnlineEvaluationSnapshot,
    /// Redacted repository and host settings.
    pub repo: SessionSettingsRepoSnapshot,
    /// Session start time as Unix epoch milliseconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub start_time_ms: Option<f64>,
    /// Session timeout in milliseconds.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout_ms: Option<f64>,
    /// Redacted validation and memory-tool settings.
    pub validation: SessionSettingsValidationSnapshot,
    /// Agent runtime version selector copied from the session settings, such as `latest` or a runtime release identifier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Batch content-exclusion result. Callers must fail closed when policy evaluation is unavailable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionContentExclusionCheckPathsResult {
    /// Whether the session's policy service was available for the complete batch. When false, checks is empty and callers must treat every requested path as excluded.
    pub available: bool,
    /// Per-path decisions in request order. Empty when available is false.
    pub checks: Vec<ContentExclusionPathCheck>,
}

/// Identifier of the spawned shell process, usable with shell.kill while the process is running.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShellExecResult {
    /// Identifier usable with shell.kill while the process is running
    pub process_id: String,
}

/// Indicates whether the signal was delivered; false if the process was unknown or already exited.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShellKillResult {
    /// Whether the signal was sent successfully
    pub killed: bool,
}

/// Result of a user-requested shell command.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShellExecuteUserRequestedResult {
    /// Error output when the execution failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Process exit code, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exit_code: Option<i64>,
    /// Captured command output
    pub output: String,
    /// Whether the command completed successfully
    pub success: bool,
    /// Tool call id emitted for the shell execution
    pub tool_call_id: String,
}

/// Cancellation result for a user-requested shell command.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShellCancelUserRequestedResult {
    /// Whether an in-flight execution was found and signalled to cancel
    pub cancelled: bool,
}

/// Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryCompactResult {
    /// Post-compaction context window usage breakdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_window: Option<HistoryCompactContextWindow>,
    /// Number of messages removed during compaction
    pub messages_removed: i64,
    /// Whether compaction completed successfully
    pub success: bool,
    /// Summary text produced by compaction. Omitted when compaction did not produce a summary (e.g. failure path).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary_content: Option<String>,
    /// Number of tokens freed by compaction
    pub tokens_removed: i64,
}

/// Number of events that were removed by the truncation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryTruncateResult {
    /// Failure detail when checkpointCleanupFailed is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub checkpoint_cleanup_error: Option<String>,
    /// True when conversation truncation succeeded but post-truncation workspace checkpoint cleanup failed. History is already truncated; callers may still prune snapshots but should report a checkpoint-cleanup rather than a truncation failure.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub checkpoint_cleanup_failed: Option<bool>,
    /// Number of events that were removed
    pub events_removed: i64,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryListRewindPointsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Rewind points and file-change-tracking availability for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryListRewindPointsResult {
    /// Whether this session captured file changes from its first turn.
    pub file_change_tracking_enabled: bool,
    /// Root user turns in chronological order. Empty when `unavailableReason` is set.
    pub points: Vec<HistoryRewindPoint>,
    /// Why the listed points could not be produced, when applicable; the points list is empty whenever it is set. `unsupported-remote-session` is permanent for the session and comes with `fileChangeTrackingEnabled: false`. `session-busy` is transient and only ever reported by a session that *is* tracking (`fileChangeTrackingEnabled: true`), because the file-change captures cannot be read while work that may still mutate them is in flight; the same request succeeds once the session settles, so a client that wants points should retry rather than treat it as a failure. It is never `file-change-tracking-disabled`: an untracked local session still lists conversation-only points and reports that through `fileChangeTrackingEnabled: false`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unavailable_reason: Option<HistoryRewindUnavailableReason>,
}

/// Files and aggregate changes for a prospective rewind.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryPreviewRewindResult {
    /// Whether file restore is available for this session. This is authoritative: switch on it and read `reason` only when it is false.
    pub available: bool,
    /// Number of unique files in the preview.
    pub file_count: i64,
    /// Files ordered by path.
    pub files: Vec<HistoryRewindFilePreview>,
    /// Why file restore is unavailable, when applicable. Populated only when `available` is false and never set when `available` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<HistoryRewindUnavailableReason>,
}

/// Structured outcome of a rewind request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryRewindResult {
    /// Failure detail. Set only for the failure and partial-failure outcomes (`files-rolled-back`, `rollback-incomplete`, `truncation-failed`, `checkpoint-cleanup-failed`, `snapshot-prune-failed`); omitted for `success` and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Number of persisted events removed by conversation truncation. Present only when truncation succeeded (outcomes `success`, `checkpoint-cleanup-failed`, and `snapshot-prune-failed`); omitted for every unavailable outcome (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`) and for `truncation-failed`, `files-rolled-back`, and `rollback-incomplete`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub events_removed: Option<i64>,
    /// Overall rewind outcome. This discriminates the result: it governs which of the remaining fields are populated, so consumers must switch on it before reading `eventsRemoved`, `restoredFiles`, `skippedFiles`, or `error`. See each field for the outcomes that populate it.
    pub outcome: HistoryRewindOutcome,
    /// Absolute paths restored to their captured preimages. Always empty for conversation-only rewinds and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`); only conversation-and-files outcomes that reached the file-restore stage populate it.
    pub restored_files: Vec<String>,
    /// Captured files intentionally left unchanged. Always empty for conversation-only rewinds and for the unavailable outcomes (`session-busy`, `file-change-tracking-disabled`, `unsupported-remote-session`); only conversation-and-files outcomes that reached the file-restore stage populate it.
    pub skipped_files: Vec<HistorySkippedFileRestore>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryCancelBackgroundCompactionParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether an in-progress background compaction was cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryCancelBackgroundCompactionResult {
    /// Whether an in-progress background compaction was cancelled. False when no compaction was running, when the session is remote, or when the underlying processor was unavailable.
    pub cancelled: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryAbortManualCompactionParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether an in-progress manual compaction was aborted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryAbortManualCompactionResult {
    /// Whether an in-progress manual compaction was aborted. False when no manual compaction was running, when its abort controller was already aborted, or when the session is remote.
    pub aborted: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistorySummarizeForHandoffParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Markdown summary of the conversation context (empty when not available).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistorySummarizeForHandoffResult {
    /// Markdown summary of the conversation context produced by an LLM. Empty string when there are no messages or when the session does not support local summarization.
    pub summary: String,
}

/// What a successful clear removed. A clear that could not be applied rejects instead of reporting a count.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryClearContextResult {
    /// Number of non-system, non-developer messages that were removed from the conversation. Zero only when the window already held no conversation.
    pub messages_cleared: i64,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueuePendingItemsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Snapshot of the session's pending queued items and immediate-steering messages.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueuePendingItemsResult {
    /// How many leading entries of `steeringMessages` have already been folded into the running turn (and so have an emitted `user.message`), as opposed to still waiting for one. Absent for hosts that do not distinguish the two.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub in_flight_steering_count: Option<i64>,
    /// Pending queued items in submission order. Includes user messages, queued slash commands, and queued model changes; omits internal system items.
    pub items: Vec<QueuePendingItems>,
    /// Display text for messages currently in the immediate steering queue (interjections sent during a running turn).
    pub steering_messages: Vec<String>,
    /// ID of the running turn's user message while the model has not answered it, so `withdrawMessage` can still take it back once nothing sent after it is pending. A message leaves `items` when its turn starts, before its `user.message` is recorded; this tells that message apart from one that was removed. Absent when no turn prompt can be taken back.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub withdrawable_turn_message_id: Option<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueSnapshotParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Internal snapshot of native queue state for local session orchestration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueSnapshotResult {
    /// Queue item identifier of a model switch that has been dequeued but not yet applied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub in_flight_model_change_id: Option<String>,
    /// Insertion orders for queued items, aligned with `items`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub item_orders: Option<Vec<i64>>,
    /// User-facing pending items in FIFO order.
    pub items: Vec<QueuePendingItems>,
    /// Insertion orders for immediate steering messages, aligned with `steeringMessages`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub steering_message_orders: Option<Vec<i64>>,
    /// Immediate steering messages waiting for an active turn.
    pub steering_messages: Vec<String>,
}

/// Result of moving a queued item.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueMoveItemResult {
    /// True when the item changed position; false when it was already at the requested position.
    pub changed: bool,
}

/// Result of inserting a queued message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueInsertAtResult {
    /// Fresh stable opaque id assigned to the inserted item.
    pub id: String,
}

/// Result of removing a queued item.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueRemoveAtResult {
    /// True when the addressed item was removed.
    pub removed: bool,
}

/// Result of editing a queued message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueUpdateTextResult {
    /// True when the stored text changed.
    pub updated: bool,
}

/// Result of withdrawing a user message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueWithdrawMessageResult {
    /// True when the running turn was interrupted to withdraw the message. With removed false, the turn was interrupted but its events could not be removed, for example because the model answered first, so the message stays in the interrupted turn.
    pub interrupted: bool,
    /// True when the message left the queue or, for a running turn, history.
    pub removed: bool,
}

/// Result of editing a queued message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueAppendSteeringResult {
    /// True when the stored text changed.
    pub updated: bool,
}

/// Result of duplicating a queued item.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueDuplicateAtResult {
    /// Fresh stable opaque id assigned to the duplicate.
    pub id: String,
}

/// Result of trying to steer a queued message into a live turn.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueSendNowResult {
    /// True when the item was accepted into the steering lane; false when no main turn was live.
    pub steered: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueHasPendingParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Whether the native queue has pending work.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueHasPendingResult {
    /// True when queued or immediate native work is pending.
    pub has_pending: bool,
}

/// Whether a deferred-idle drain should run.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueBeginDeferredIdleDrainResult {
    /// True when the host should run finishDeferredIdleDrain asynchronously.
    pub should_drain: bool,
}

/// Action selected by the native deferred-idle drain.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueFinishDeferredIdleDrainResult {
    /// Whether the deferred idle was caused by an aborted foreground turn.
    pub aborted: bool,
    /// One of none, processQueue, or emitSessionIdle.
    pub action: String,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueRemoveMostRecentParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether a user-facing pending item was removed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueRemoveMostRecentResult {
    /// True if a user-facing pending item was removed (LIFO across both queues); false when no removable items remained.
    pub removed: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueClearParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether a user-facing pending item was removed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueConsumeSystemNotificationsResult {
    /// True if a user-facing pending item was removed (LIFO across both queues); false when no removable items remained.
    pub removed: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueEnqueueResumePendingParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Result of enqueueing the resume-pending wake item.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueEnqueueResumePendingResult {
    /// True when a wake item was newly queued.
    pub queued: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueProcessParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Batch of session events returned by a read, with cursor and continuation metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEventLogReadResult {
    /// Opaque cursor for the next read. Pass back unchanged in the next read.cursor to continue from where this read left off. Always present, even when no events were returned. For a backward read this cursor pages toward OLDER events; keep passing `direction: backward` with it (the cursor is also self-describing, so backward paging continues correctly).
    pub cursor: String,
    /// Cursor status: 'ok' means the cursor was applied successfully. For session.eventLog.read, 'expired' means the cursor referred to an event that no longer exists in active history and the read fell back to a boundary of the remaining history: the beginning for a forward read or the newest window for a backward read. That fallback may overlap already rendered events, so active-session consumers should reset, rebase, or deduplicate before continuing. sessions.readPersistedEvents has stricter snapshot semantics: 'expired' returns an empty terminal page and never switches to a replacement journal generation. Other persisted-read I/O failures are RPC errors with diagnostics, not cursor expiry.
    pub cursor_status: EventsCursorStatus,
    /// Session events for this batch, merged into a single stream in creation order: durable (persisted) events and ephemeral events interleave exactly as they were emitted. Set `includeEphemeral: false` to receive only durable events. Ephemeral events are never replayable once pruned from the in-memory ring, so a consumer that needs them should keep reading with a non-zero `waitMs`. For a backward (tail-first) read, the returned window contains persisted events only, still in chronological (oldest-to-newest) append order.
    pub events: Vec<SessionEvent>,
    /// True when more events are available in the read's direction. For a backward read, true means older persisted events remain before the returned window. A persisted-event page may contain fewer than `max` events because of its byte budget while still reporting hasMore true; continue according to this flag rather than the event count.
    pub has_more: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEventLogTailParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Snapshot of the current tail cursor without returning any events. Use this when a consumer wants to subscribe to live events going forward without first paginating through the entire persisted history (which would happen if `read` were called without a cursor on a long-lived session).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEventLogTailResult {
    /// Opaque cursor pointing at the current tail of the session's persisted-events history. Pass back to `read` to receive only events that arrive AFTER this snapshot. When the session has no events, this returns the same sentinel as an unset cursor (i.e. equivalent to omitting the cursor on a first read).
    pub cursor: String,
}

/// Opaque handle representing an event-type interest registration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEventLogRegisterInterestResult {
    /// Opaque handle for this registration. Pass to releaseInterest to release. Each call to registerInterest produces a fresh handle, even when the same eventType is registered multiple times.
    pub handle: String,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEventLogReleaseInterestResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUsageGetMetricsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Internal absolute code-change totals reported by the owning host.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SessionUsageSetCodeChangesParams {
    /// Absolute added-line total, replacing the previous reading.
    pub lines_added: f64,
    /// Absolute removed-line total, replacing the previous reading.
    pub lines_removed: f64,
    /// Absolute changed-file count; omission preserves the previous count.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub files_count: Option<f64>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQuotaGetParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The incumbent session-owned quota and account projection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQuotaGetResult {
    /// Whether the account can sign up for Copilot Free.
    pub can_signup_for_copilot_free: bool,
    /// Whether the account can delegate tasks to GitHub.
    pub delegate_available: bool,
    /// Existing delegation warning for a free account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub delegate_warning: Option<SessionQuotaDelegateWarning>,
    /// Whether dynamic workflows are enabled for the active account.
    pub dynamic_workflows_enabled: bool,
    /// Whether dynamic workflows are visible under the session's feature flags.
    pub dynamic_workflows_ui_visible: bool,
    /// Whether the account uses the free limited Copilot plan.
    pub is_free_user: bool,
    /// Whether the account uses token-based billing.
    pub is_tbb_user: bool,
    /// Whether model cost columns should be shown.
    pub model_cost_column_visible: bool,
    /// Resolved Copilot plan tier.
    pub plan_tier: SessionQuotaPlanTier,
    /// Whether premium-request costs are billable.
    pub premium_requests_billable: bool,
    /// Provider-owned account quota state, including snapshots without numeric balances.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_quotas: Option<Vec<ProviderQuotaState>>,
    /// Known quota snapshots, keyed by the provider's quota type.
    pub snapshots: HashMap<String, SessionQuotaSnapshot>,
    /// Upgrade link for a free account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub upgrade_url: Option<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQuotaRefreshParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The incumbent session-owned quota and account projection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQuotaRefreshResult {
    /// Whether the account can sign up for Copilot Free.
    pub can_signup_for_copilot_free: bool,
    /// Whether the account can delegate tasks to GitHub.
    pub delegate_available: bool,
    /// Existing delegation warning for a free account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub delegate_warning: Option<SessionQuotaDelegateWarning>,
    /// Whether dynamic workflows are enabled for the active account.
    pub dynamic_workflows_enabled: bool,
    /// Whether dynamic workflows are visible under the session's feature flags.
    pub dynamic_workflows_ui_visible: bool,
    /// Whether the account uses the free limited Copilot plan.
    pub is_free_user: bool,
    /// Whether the account uses token-based billing.
    pub is_tbb_user: bool,
    /// Whether model cost columns should be shown.
    pub model_cost_column_visible: bool,
    /// Resolved Copilot plan tier.
    pub plan_tier: SessionQuotaPlanTier,
    /// Whether premium-request costs are billable.
    pub premium_requests_billable: bool,
    /// Provider-owned account quota state, including snapshots without numeric balances.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider_quotas: Option<Vec<ProviderQuotaState>>,
    /// Known quota snapshots, keyed by the provider's quota type.
    pub snapshots: HashMap<String, SessionQuotaSnapshot>,
    /// Upgrade link for a free account.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub upgrade_url: Option<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQuotaTakeWarningsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// GitHub URL and remote export and steering state, with an optional warning when cloud session storage policy refuses export.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionRemoteEnableResult {
    /// Whether remote steering is enabled
    pub remote_steerable: bool,
    /// Whether this request accepted or retained remote export. False when export is disabled or refused; initialization may still be pending when true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub started: Option<bool>,
    /// GitHub frontend URL for this session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
    /// Warning explaining why cloud session storage policy prevented export. Absent when no policy warning applies.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub warning: Option<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionRemoteDisableParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Persist a steerability change as a `session.remote_steerable_changed` event. Used by the host (CLI / SDK consumer) when it has just finished enabling or disabling steering on a remote exporter that the runtime does not directly own.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionRemoteNotifySteerableChangedResult {}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionVisibilityGetParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Current sharing status and shareable GitHub URL for a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionVisibilityGetResult {
    /// Shareable GitHub URL for the session. Present when the session is synced and the URL can be resolved.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub share_url: Option<String>,
    /// Current sharing status. Absent when the session is not synced or the status could not be retrieved (e.g. the user is not authenticated).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<SessionVisibilityStatus>,
    /// Whether the session has been synced to Mission Control (i.e. has a GitHub task). When false, the session cannot be shared and `status`/`shareUrl` are absent.
    pub synced: bool,
}

/// Effective sharing status and shareable GitHub URL after updating session visibility.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionVisibilitySetResult {
    /// Shareable GitHub URL for the session. Present when the session is synced and the URL can be resolved.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub share_url: Option<String>,
    /// Effective sharing status after the update. May differ from the requested status for task types that are already visible to repository readers by default. Absent when the update could not be applied (e.g. the session is not synced or the user is not authenticated).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<SessionVisibilityStatus>,
    /// Whether the session has been synced to Mission Control (i.e. has a GitHub task). When false, the visibility change could not be applied and `status`/`shareUrl` are absent.
    pub synced: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Snapshot of the currently active recurring prompts for this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleListResult {
    /// Active scheduled prompts, ordered by id.
    pub entries: Vec<ScheduleEntry>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleHydrateParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleHasSelfPacedParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Whether the session currently has an active self-paced schedule.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleHasSelfPacedResult {
    /// True when at least one active schedule is self-paced.
    pub has_self_paced: bool,
}

/// Result of registering or re-arming a scheduled prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleAddResult {
    /// The registered or updated schedule entry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entry: Option<ScheduleEntry>,
    /// User-facing validation error, when registration failed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

/// Result of registering or re-arming a scheduled prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleAddCronResult {
    /// The registered or updated schedule entry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entry: Option<ScheduleEntry>,
    /// User-facing validation error, when registration failed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

/// Result of registering or re-arming a scheduled prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleAddAtResult {
    /// The registered or updated schedule entry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entry: Option<ScheduleEntry>,
    /// User-facing validation error, when registration failed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

/// Result of registering or re-arming a scheduled prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleAddSelfPacedResult {
    /// The registered or updated schedule entry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entry: Option<ScheduleEntry>,
    /// User-facing validation error, when registration failed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

/// Result of registering or re-arming a scheduled prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleRearmSelfPacedResult {
    /// The registered or updated schedule entry.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entry: Option<ScheduleEntry>,
    /// User-facing validation error, when registration failed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

/// Remove a scheduled prompt by id. The result entry is omitted if the id was unknown.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleStopResult {
    /// The removed entry, or omitted if no entry matched.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entry: Option<ScheduleEntry>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSqliteExistsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Current authentication information, or null when no authentication is active.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionAuthInfoResult = Option<AuthIdentity>;

/// Ordered client metadata outcomes for the requested local sessions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionsGetClientMetadataResult = Vec<serde_json::Value>;

/// The number of running background agents (task-registry agents) that were cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionCancelAllBackgroundAgentsResult = i64;

/// Authentication accounts available to the internal session host.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionGitHubAuthGetAllAuthAvailableResult = Vec<SessionAuthStatus>;

/// Whether the current authentication was logged out.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionGitHubAuthLogoutResult = bool;

/// Whether the requested authentication was logged out.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionGitHubAuthLogoutUserResult = bool;

/// Validation errors from the most recent authentication attempt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionGitHubAuthLastAuthErrorsResult = Vec<AuthValidationError>;

/// Standard MCP CallToolResult
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionMcpAppsCallToolResult = HashMap<String, serde_json::Value>;

/// Client-owned, case-sensitive string metadata persisted with a local session. Clients should namespace keys by owner. Keys must be non-empty and at most 256 UTF-8 bytes; keys under `copilot/` and `github/` are reserved. Values may contain at most 16 KiB of UTF-8 data. A bag may contain at most 128 entries and its serialized sidecar may contain at most 64 KiB. The runtime stores but never interprets these values.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionMetadataGetClientMetadataResult = HashMap<String, String>;

/// Client-owned, case-sensitive string metadata persisted with a local session. Clients should namespace keys by owner. Keys must be non-empty and at most 256 UTF-8 bytes; keys under `copilot/` and `github/` are reserved. Values may contain at most 16 KiB of UTF-8 data. A bag may contain at most 128 entries and its serialized sidecar may contain at most 64 KiB. The runtime stores but never interprets these values.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionMetadataUpdateClientMetadataResult = HashMap<String, String>;

/// Pending warnings returned and cleared by session.quota.takeWarnings.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionQuotaTakeWarningsResult = Vec<QuotaWarningProjection>;

/// Custom input-format kind.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum BuiltinToolFormatType {
    /// The tool input is parsed with the supplied grammar.
    #[serde(rename = "grammar")]
    Grammar,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Root JSON Schema type for a built-in tool input.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum BuiltinToolInputSchemaType {
    /// The tool accepts a JSON object.
    #[serde(rename = "object")]
    Object,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Repository host type
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionContextHostType {
    /// Session repository is hosted on GitHub.
    #[serde(rename = "github")]
    GitHub,
    /// Session repository is hosted on Azure DevOps.
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// What retrying did for one plugin required by managed settings.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ManagedPluginRetryStatus {
    /// The plugin was missing and is now installed.
    #[serde(rename = "installed")]
    Installed,
    /// An update was available and is now installed.
    #[serde(rename = "updated")]
    Updated,
    /// The plugin was already installed and current, so nothing was done.
    #[serde(rename = "already_present")]
    AlreadyPresent,
    /// Checking, installing, or updating the plugin failed. See `error`.
    #[serde(rename = "failed")]
    Failed,
    /// The plugin was not prepared because another session was changing plugins or the managed settings changed during the retry. It is prepared again before a later message.
    #[serde(rename = "deferred")]
    Deferred,
    /// The session's managed settings do not require this plugin, so nothing was done.
    #[serde(rename = "not_required")]
    NotRequired,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Memory injection mode. Defaults to system-prompt.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum MemoryMode {
    /// Include memories in the system prompt (default).
    #[serde(rename = "system-prompt")]
    SystemPrompt,
    /// Include memories in user messages.
    #[serde(rename = "user-prompt")]
    UserPrompt,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot')
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum MetadataSnapshotCurrentMode {
    /// The agent is responding interactively to the user.
    #[serde(rename = "interactive")]
    Interactive,
    /// The agent is preparing a plan before making changes.
    #[serde(rename = "plan")]
    Plan,
    /// The agent is working autonomously toward task completion.
    #[serde(rename = "autopilot")]
    Autopilot,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the remote task originated from Copilot Coding Agent (cca) or a CLI `--remote` invocation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum MetadataSnapshotRemoteMetadataTaskType {
    /// Remote task originated from Copilot Coding Agent.
    #[serde(rename = "cca")]
    Cca,
    /// Remote task originated from a CLI remote-session invocation.
    #[serde(rename = "cli")]
    Cli,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Allowed values for the `OptionsUpdateAdditionalContentExclusionPolicyScope` enumeration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum OptionsUpdateAdditionalContentExclusionPolicyScope {
    /// The content exclusion policy applies to the current repository.
    #[serde(rename = "repo")]
    Repo,
    /// The content exclusion policy applies across all repositories.
    #[serde(rename = "all")]
    All,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Context tier for models with tiered pricing. The session uses this to derive effective `modelCapabilitiesOverrides` so compaction, truncation, token display, and request limits honor the selected tier.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum OptionsUpdateContextTier {
    /// Use the model's default context tier and its standard token limits / pricing.
    #[serde(rename = "default")]
    Default,
    /// Use the model's long-context tier (when available) so larger inputs are accepted and tier-specific pricing applies.
    #[serde(rename = "long_context")]
    LongContext,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How env values are passed to MCP servers (`direct` inlines literal values; `indirect` resolves at launch).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum OptionsUpdateEnvValueMode {
    /// Pass MCP server environment values as literal strings.
    #[serde(rename = "direct")]
    Direct,
    /// Resolve MCP server environment values from host-side references.
    #[serde(rename = "indirect")]
    Indirect,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Reasoning summary mode for supported model clients.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum OptionsUpdateReasoningSummary {
    /// Do not request reasoning summaries from the model.
    #[serde(rename = "none")]
    None,
    /// Request a concise summary of model reasoning.
    #[serde(rename = "concise")]
    Concise,
    /// Request a detailed summary of model reasoning.
    #[serde(rename = "detailed")]
    Detailed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Approve this single request only
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveOnceKind {
    #[serde(rename = "approve-once")]
    #[default]
    ApproveOnce,
}

/// Approval scoped to specific command identifiers.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalCommandsKind {
    #[serde(rename = "commands")]
    #[default]
    Commands,
}

/// Approval covering read-only filesystem operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalReadKind {
    #[serde(rename = "read")]
    #[default]
    Read,
}

/// Approval covering filesystem write operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalWriteKind {
    #[serde(rename = "write")]
    #[default]
    Write,
}

/// Approval covering an MCP tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalMcpKind {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// Approval covering MCP sampling requests for a server.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalMcpSamplingKind {
    #[serde(rename = "mcp-sampling")]
    #[default]
    McpSampling,
}

/// Approval covering writes to long-term memory.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalMemoryKind {
    #[serde(rename = "memory")]
    #[default]
    Memory,
}

/// Approval covering a custom tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalCustomToolKind {
    #[serde(rename = "custom-tool")]
    #[default]
    CustomTool,
}

/// Approval covering extension lifecycle operations such as enable, disable, or reload.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalExtensionManagementKind {
    #[serde(rename = "extension-management")]
    #[default]
    ExtensionManagement,
}

/// Approval covering workflow operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalWorkflowKind {
    #[serde(rename = "workflow")]
    #[default]
    Workflow,
}

/// Approval covering an extension's request to access a permission-gated capability.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalExtensionPermissionAccessKind {
    #[serde(rename = "extension-permission-access")]
    #[default]
    ExtensionPermissionAccess,
}

/// Approval covering an extension's request to read sensitive environment variables.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalExtensionEnvAccessKind {
    #[serde(rename = "extension-env-access")]
    #[default]
    ExtensionEnvAccess,
}

/// Session-scoped approval to remember (tool prompts only; omitted for path/url prompts)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionDecisionApproveForSessionApproval {
    Commands(PermissionDecisionApproveForSessionApprovalCommands),
    Read(PermissionDecisionApproveForSessionApprovalRead),
    Write(PermissionDecisionApproveForSessionApprovalWrite),
    Mcp(PermissionDecisionApproveForSessionApprovalMcp),
    McpSampling(PermissionDecisionApproveForSessionApprovalMcpSampling),
    Memory(PermissionDecisionApproveForSessionApprovalMemory),
    CustomTool(PermissionDecisionApproveForSessionApprovalCustomTool),
    ExtensionManagement(PermissionDecisionApproveForSessionApprovalExtensionManagement),
    Workflow(PermissionDecisionApproveForSessionApprovalWorkflow),
    ExtensionPermissionAccess(PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess),
    ExtensionEnvAccess(PermissionDecisionApproveForSessionApprovalExtensionEnvAccess),
}

/// Approve and remember for the rest of the session
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionKind {
    #[serde(rename = "approve-for-session")]
    #[default]
    ApproveForSession,
}

/// Approve read-only file-tool directory access for this session
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveReadOnlyForSessionKind {
    #[serde(rename = "approve-read-only-for-session")]
    #[default]
    ApproveReadOnlyForSession,
}

/// Approval scoped to specific command identifiers.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalCommandsKind {
    #[serde(rename = "commands")]
    #[default]
    Commands,
}

/// Approval covering read-only filesystem operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalReadKind {
    #[serde(rename = "read")]
    #[default]
    Read,
}

/// Approval covering filesystem write operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalWriteKind {
    #[serde(rename = "write")]
    #[default]
    Write,
}

/// Approval covering an MCP tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalMcpKind {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// Approval covering MCP sampling requests for a server.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalMcpSamplingKind {
    #[serde(rename = "mcp-sampling")]
    #[default]
    McpSampling,
}

/// Approval covering writes to long-term memory.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalMemoryKind {
    #[serde(rename = "memory")]
    #[default]
    Memory,
}

/// Approval covering a custom tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalCustomToolKind {
    #[serde(rename = "custom-tool")]
    #[default]
    CustomTool,
}

/// Approval covering extension lifecycle operations such as enable, disable, or reload.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalExtensionManagementKind {
    #[serde(rename = "extension-management")]
    #[default]
    ExtensionManagement,
}

/// Approval covering workflow operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalWorkflowKind {
    #[serde(rename = "workflow")]
    #[default]
    Workflow,
}

/// Approval covering an extension's request to access a permission-gated capability.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalExtensionPermissionAccessKind {
    #[serde(rename = "extension-permission-access")]
    #[default]
    ExtensionPermissionAccess,
}

/// Approval covering an extension's request to read sensitive environment variables.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalExtensionEnvAccessKind {
    #[serde(rename = "extension-env-access")]
    #[default]
    ExtensionEnvAccess,
}

/// Approval to persist for this location
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionDecisionApproveForLocationApproval {
    Commands(PermissionDecisionApproveForLocationApprovalCommands),
    Read(PermissionDecisionApproveForLocationApprovalRead),
    Write(PermissionDecisionApproveForLocationApprovalWrite),
    Mcp(PermissionDecisionApproveForLocationApprovalMcp),
    McpSampling(PermissionDecisionApproveForLocationApprovalMcpSampling),
    Memory(PermissionDecisionApproveForLocationApprovalMemory),
    CustomTool(PermissionDecisionApproveForLocationApprovalCustomTool),
    ExtensionManagement(PermissionDecisionApproveForLocationApprovalExtensionManagement),
    Workflow(PermissionDecisionApproveForLocationApprovalWorkflow),
    ExtensionPermissionAccess(
        PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess,
    ),
    ExtensionEnvAccess(PermissionDecisionApproveForLocationApprovalExtensionEnvAccess),
}

/// Approve and persist for this project location
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationKind {
    #[serde(rename = "approve-for-location")]
    #[default]
    ApproveForLocation,
}

/// Approve and persist across sessions (URL prompts only)
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApprovePermanentlyKind {
    #[serde(rename = "approve-permanently")]
    #[default]
    ApprovePermanently,
}

/// Reject the request
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionRejectKind {
    #[serde(rename = "reject")]
    #[default]
    Reject,
}

/// No user is available to confirm the request
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionUserNotAvailableKind {
    #[serde(rename = "user-not-available")]
    #[default]
    UserNotAvailable,
}

/// The permission request was approved
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApprovedKind {
    #[serde(rename = "approved")]
    #[default]
    Approved,
}

/// Approved and remembered for the rest of the session
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApprovedForSessionKind {
    #[serde(rename = "approved-for-session")]
    #[default]
    ApprovedForSession,
}

/// Approved and persisted for this project location
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApprovedForLocationKind {
    #[serde(rename = "approved-for-location")]
    #[default]
    ApprovedForLocation,
}

/// The permission request was cancelled before a response was used
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionCancelledKind {
    #[serde(rename = "cancelled")]
    #[default]
    Cancelled,
}

/// Denied because approval rules explicitly blocked it
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionDeniedByRulesKind {
    #[serde(rename = "denied-by-rules")]
    #[default]
    DeniedByRules,
}

/// Denied because no approval rule matched and user confirmation was unavailable
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind {
    #[serde(rename = "denied-no-approval-rule-and-could-not-request-from-user")]
    #[default]
    DeniedNoApprovalRuleAndCouldNotRequestFromUser,
}

/// Denied by the user during an interactive prompt
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionDeniedInteractivelyByUserKind {
    #[serde(rename = "denied-interactively-by-user")]
    #[default]
    DeniedInteractivelyByUser,
}

/// Denied by the organization's content exclusion policy
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionDeniedByContentExclusionPolicyKind {
    #[serde(rename = "denied-by-content-exclusion-policy")]
    #[default]
    DeniedByContentExclusionPolicy,
}

/// Denied by a permission request hook registered by an extension or plugin
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionDeniedByPermissionRequestHookKind {
    #[serde(rename = "denied-by-permission-request-hook")]
    #[default]
    DeniedByPermissionRequestHook,
}

/// The client's response to the pending permission prompt
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionDecision {
    ApproveOnce(PermissionDecisionApproveOnce),
    ApproveForSession(PermissionDecisionApproveForSession),
    ApproveReadOnlyForSession(PermissionDecisionApproveReadOnlyForSession),
    ApproveForLocation(PermissionDecisionApproveForLocation),
    ApprovePermanently(PermissionDecisionApprovePermanently),
    Reject(PermissionDecisionReject),
    UserNotAvailable(PermissionDecisionUserNotAvailable),
    Approved(PermissionDecisionApproved),
    ApprovedForSession(PermissionDecisionApprovedForSession),
    ApprovedForLocation(PermissionDecisionApprovedForLocation),
    Cancelled(PermissionDecisionCancelled),
    DeniedByRules(PermissionDecisionDeniedByRules),
    DeniedNoApprovalRuleAndCouldNotRequestFromUser(
        PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser,
    ),
    DeniedInteractivelyByUser(PermissionDecisionDeniedInteractivelyByUser),
    DeniedByContentExclusionPolicy(PermissionDecisionDeniedByContentExclusionPolicy),
    DeniedByPermissionRequestHook(PermissionDecisionDeniedByPermissionRequestHook),
}

/// Approval scoped to specific command identifiers.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsCommandsKind {
    #[serde(rename = "commands")]
    #[default]
    Commands,
}

/// Approval covering read-only filesystem operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsReadKind {
    #[serde(rename = "read")]
    #[default]
    Read,
}

/// Approval covering filesystem write operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsWriteKind {
    #[serde(rename = "write")]
    #[default]
    Write,
}

/// Approval covering an MCP tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsMcpKind {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// Approval covering MCP sampling requests for a server.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsMcpSamplingKind {
    #[serde(rename = "mcp-sampling")]
    #[default]
    McpSampling,
}

/// Approval covering writes to long-term memory.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsMemoryKind {
    #[serde(rename = "memory")]
    #[default]
    Memory,
}

/// Approval covering a custom tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsCustomToolKind {
    #[serde(rename = "custom-tool")]
    #[default]
    CustomTool,
}

/// Approval covering extension lifecycle operations such as enable, disable, or reload.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsExtensionManagementKind {
    #[serde(rename = "extension-management")]
    #[default]
    ExtensionManagement,
}

/// Approval covering workflow operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsWorkflowKind {
    #[serde(rename = "workflow")]
    #[default]
    Workflow,
}

/// Approval covering an extension's request to access a permission-gated capability.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccessKind {
    #[serde(rename = "extension-permission-access")]
    #[default]
    ExtensionPermissionAccess,
}

/// Approval covering an extension's request to read sensitive environment variables.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsExtensionEnvAccessKind {
    #[serde(rename = "extension-env-access")]
    #[default]
    ExtensionEnvAccess,
}

/// Tool approval to persist and apply
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionsLocationsAddToolApprovalDetails {
    Commands(PermissionsLocationsAddToolApprovalDetailsCommands),
    Read(PermissionsLocationsAddToolApprovalDetailsRead),
    Write(PermissionsLocationsAddToolApprovalDetailsWrite),
    Mcp(PermissionsLocationsAddToolApprovalDetailsMcp),
    McpSampling(PermissionsLocationsAddToolApprovalDetailsMcpSampling),
    Memory(PermissionsLocationsAddToolApprovalDetailsMemory),
    CustomTool(PermissionsLocationsAddToolApprovalDetailsCustomTool),
    ExtensionManagement(PermissionsLocationsAddToolApprovalDetailsExtensionManagement),
    Workflow(PermissionsLocationsAddToolApprovalDetailsWorkflow),
    ExtensionPermissionAccess(PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess),
    ExtensionEnvAccess(PermissionsLocationsAddToolApprovalDetailsExtensionEnvAccess),
}

/// Whether the location is a git repo or directory
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionLocationType {
    /// The permission location is persisted at the git repository root.
    #[serde(rename = "repo")]
    Repo,
    /// The permission location is persisted at the working directory.
    #[serde(rename = "dir")]
    Dir,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Optional source for permission-mode telemetry. `organization_targeting` is reserved for startup selection after the authenticated account matches an organization targeting policy; SDK callers default to `rpc` and cannot claim targeting provenance.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionModeSource {
    /// The mode was set from a CLI command-line flag.
    #[serde(rename = "cli_flag")]
    CliFlag,
    /// The mode was set by a slash command.
    #[serde(rename = "slash_command")]
    SlashCommand,
    /// The mode was set by confirming autopilot behavior.
    #[serde(rename = "autopilot_confirmation")]
    AutopilotConfirmation,
    /// The mode was set at startup by the `defaultPermissionMode` user setting.
    #[serde(rename = "user_setting")]
    UserSetting,
    /// The mode was set at startup because the authenticated account matched an organization targeting policy.
    #[serde(rename = "organization_targeting")]
    OrganizationTargeting,
    /// The mode was set through an RPC caller.
    #[serde(rename = "rpc")]
    Rpc,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Allowed values for the `PermissionsConfigureAdditionalContentExclusionPolicyScope` enumeration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsConfigureAdditionalContentExclusionPolicyScope {
    /// The content exclusion policy applies to the current repository.
    #[serde(rename = "repo")]
    Repo,
    /// The content exclusion policy applies across all repositories.
    #[serde(rename = "all")]
    All,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the change applies to ephemeral session-scoped rules (cleared at session end) or to location-scoped rules persisted via the location-permissions config file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsModifyRulesScope {
    /// Apply the rule change only to this session.
    #[serde(rename = "session")]
    Session,
    /// Persist the rule change for this project location.
    #[serde(rename = "location")]
    Location,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsSetApproveAllSource {
    /// Allow-all was enabled from a CLI command-line flag.
    #[serde(rename = "cli_flag")]
    CliFlag,
    /// Allow-all was enabled by a slash command.
    #[serde(rename = "slash_command")]
    SlashCommand,
    /// Allow-all was enabled by confirming autopilot behavior.
    #[serde(rename = "autopilot_confirmation")]
    AutopilotConfirmation,
    /// Allow-all was enabled at startup by the `defaultPermissionMode` user setting.
    #[serde(rename = "user_setting")]
    UserSetting,
    /// Allow-all was enabled through an RPC caller.
    #[serde(rename = "rpc")]
    Rpc,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Controls whether the runtime may defer loading an external tool definition.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProtocolExternalToolDefer {
    /// The runtime may defer the tool according to its tool-loading policy.
    #[serde(rename = "auto")]
    Auto,
    /// The runtime must include the tool without deferring it.
    #[serde(rename = "never")]
    Never,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// What a remote host says one of its sessions is doing right now. Deliberately coarse: this is what a host can report for EVERY session in a catalogue listing, without a client subscribing to each one. AHP's `SessionSummary.status` is the source today; `input-needed` covers both a permission prompt and an `ask_user` question, since the summary does not say which.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteSessionHostStatus {
    /// No turn is running.
    #[serde(rename = "idle")]
    Idle,
    /// A turn is running.
    #[serde(rename = "working")]
    Working,
    /// The session is blocked on the user: a permission prompt or an `ask_user` question.
    #[serde(rename = "input-needed")]
    InputNeeded,
    /// The session ended its last turn in an error.
    #[serde(rename = "error")]
    Error,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the remote task originated from CCA or CLI `--remote`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteSessionMetadataTaskType {
    /// GitHub Copilot coding agent task.
    #[serde(rename = "cca")]
    Cca,
    /// CLI remote task.
    #[serde(rename = "cli")]
    Cli,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Origin of the sandbox choice supplied by the host. This value describes preference or session intent; it does not authorize bypassing managed policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SandboxConfigSource {
    /// The client applied the default because no sandbox preference was configured.
    #[serde(rename = "never_configured")]
    NeverConfigured,
    /// The user's persisted settings enabled the sandbox.
    #[serde(rename = "user_enabled")]
    UserEnabled,
    /// The user's persisted settings disabled the sandbox.
    #[serde(rename = "user_disabled")]
    UserDisabled,
    /// An explicit session-scoped choice selected the sandbox state, such as a command-line flag.
    #[serde(rename = "session_flag")]
    SessionFlag,
    /// The user disabled the sandbox for the current session.
    #[serde(rename = "session_disabled")]
    SessionDisabled,
    /// The client disabled the sandbox because the host cannot enforce it.
    #[serde(rename = "unsupported_host")]
    UnsupportedHost,
    /// A repository policy selected the sandbox state.
    #[serde(rename = "repository_policy")]
    RepositoryPolicy,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Session capability enabled for this session
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionCapability {
    /// TUI-specific prompt hints such as keyboard shortcuts.
    #[serde(rename = "tui-hints")]
    TuiHints,
    /// Plan-mode handling and instructions.
    #[serde(rename = "plan-mode")]
    PlanMode,
    /// Memory tool and memories prompt section.
    #[serde(rename = "memory")]
    Memory,
    /// Copilot CLI documentation tool and prompt section.
    #[serde(rename = "cli-documentation")]
    CliDocumentation,
    /// Interactive ask_user tool support.
    #[serde(rename = "ask-user")]
    AskUser,
    /// Interactive CLI identity and behavior.
    #[serde(rename = "interactive-mode")]
    InteractiveMode,
    /// Automatic hidden system notifications.
    #[serde(rename = "system-notifications")]
    SystemNotifications,
    /// SDK elicitation support.
    #[serde(rename = "elicitation")]
    Elicitation,
    /// Cross-session history tools and session-store SQL prompt/tool metadata.
    #[serde(rename = "session-store")]
    SessionStore,
    /// MCP Apps UI passthrough.
    #[serde(rename = "mcp-apps")]
    McpApps,
    /// Host-provided canvas rendering support.
    #[serde(rename = "canvas-renderer")]
    CanvasRenderer,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Error classification
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsErrorCode {
    /// The requested path does not exist.
    ENOENT,
    /// The filesystem operation failed for an unspecified reason.
    UNKNOWN,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Entry type
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsReaddirWithTypesEntryType {
    /// The entry is a file.
    #[serde(rename = "file")]
    File,
    /// The entry is a directory.
    #[serde(rename = "directory")]
    Directory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Path conventions used by this filesystem
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsSetProviderConventions {
    /// Paths use Windows path conventions.
    #[serde(rename = "windows")]
    Windows,
    /// Paths use POSIX path conventions.
    #[serde(rename = "posix")]
    Posix,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How to execute the query: 'exec' for DDL/multi-statement (no results), 'query' for SELECT (returns rows), 'run' for INSERT/UPDATE/DELETE (returns rowsAffected)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsSqliteQueryType {
    /// Execute DDL or multi-statement SQL without returning rows.
    #[serde(rename = "exec")]
    Exec,
    /// Execute a SELECT-style query and return rows.
    #[serde(rename = "query")]
    Query,
    /// Execute INSERT, UPDATE, or DELETE SQL and return affected-row metadata.
    #[serde(rename = "run")]
    Run,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// SQLite transaction failure classification.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsSqliteTransactionErrorClass {
    /// SQLite reported BUSY or LOCKED before commit; the transaction was rolled back and may be retried.
    #[serde(rename = "busyOrLocked")]
    BusyOrLocked,
    /// The statement, database, or provider failed definitively and must not be retried automatically.
    #[serde(rename = "fatal")]
    Fatal,
    /// The transport failed after the provider may have committed; retrying could duplicate effects.
    #[serde(rename = "postCommitAmbiguous")]
    PostCommitAmbiguous,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Constant value. Always "github".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionInstalledPluginSourceGitHubSource {
    #[serde(rename = "github")]
    #[default]
    GitHub,
}

/// Constant value. Always "local".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionInstalledPluginSourceLocalSource {
    #[serde(rename = "local")]
    #[default]
    Local,
}

/// Constant value. Always "url".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionInstalledPluginSourceUrlSource {
    #[serde(rename = "url")]
    #[default]
    Url,
}

/// Client population used for the prediction baseline.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionLimitPredictionClientType {
    /// Interactive CLI sessions where a user can accept, edit, or top up the limit.
    #[serde(rename = "cli-interactive")]
    CliInteractive,
    /// Prompt/non-interactive CLI sessions where the initial limit must cover more of the run.
    #[serde(rename = "cli-prompt")]
    CliPrompt,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Semantic usage tier used for a recommended cap or additional headroom.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionLimitPredictionTier {
    /// Recommended starting tier.
    #[serde(rename = "recommended")]
    Recommended,
    /// Additional headroom for longer-running sessions.
    #[serde(rename = "additional_headroom")]
    AdditionalHeadroom,
    /// Generous headroom for unusually high usage.
    #[serde(rename = "generous_headroom")]
    GenerousHeadroom,
    /// Maximum available headroom tier.
    #[serde(rename = "maximum_headroom")]
    MaximumHeadroom,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Baseline fallback level used to create the prediction.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionLimitPredictionSource {
    /// The prediction used the exact resolved model's baseline cell.
    #[serde(rename = "model")]
    Model,
    /// The exact model was unavailable, so the prediction used the model family's baseline cell.
    #[serde(rename = "family")]
    Family,
    /// No model or family cell was available, so the prediction used the global client-type baseline cell.
    #[serde(rename = "global")]
    Global,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Prediction result variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionLimitPredictionResultAvailableKind {
    #[serde(rename = "available")]
    #[default]
    Available,
}

/// Prediction result variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionLimitPredictionResultUnavailableKind {
    #[serde(rename = "unavailable")]
    #[default]
    Unavailable,
}

/// Reason a prediction could not be computed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionLimitPredictionUnavailableReason {
    /// The current model is auto and has not resolved to a concrete model yet.
    #[serde(rename = "auto_unresolved")]
    AutoUnresolved,
    /// No model was provided and the session does not currently have a selected model.
    #[serde(rename = "no_model")]
    NoModel,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Prediction result. Available results include prediction details; unavailable results include an explicit reason.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum SessionLimitPredictionResult {
    Available(SessionLimitPredictionResultAvailable),
    Unavailable(SessionLimitPredictionResultUnavailable),
}

/// Allowed values for the `SessionOpenOptionsAdditionalContentExclusionPolicyScope` enumeration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionOpenOptionsAdditionalContentExclusionPolicyScope {
    /// The content exclusion policy applies to the current repository.
    #[serde(rename = "repo")]
    Repo,
    /// The content exclusion policy applies across all repositories.
    #[serde(rename = "all")]
    All,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How MCP server environment values are interpreted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionOpenOptionsEnvValueMode {
    /// Pass MCP server environment values as literal strings.
    #[serde(rename = "direct")]
    Direct,
    /// Resolve MCP server environment values from host-side references.
    #[serde(rename = "indirect")]
    Indirect,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Initial reasoning summary mode for supported model clients.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionOpenOptionsReasoningSummary {
    /// Do not request reasoning summaries from the model.
    #[serde(rename = "none")]
    None,
    /// Request a concise summary of model reasoning.
    #[serde(rename = "concise")]
    Concise,
    /// Request a detailed summary of model reasoning.
    #[serde(rename = "detailed")]
    Detailed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Create a new local session.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenCreateKind {
    #[serde(rename = "create")]
    #[default]
    Create,
}

/// Resume a specific local session by ID or prefix.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenResumeKind {
    #[serde(rename = "resume")]
    #[default]
    Resume,
}

/// Resume the most relevant existing local session.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenResumeLastKind {
    #[serde(rename = "resumeLast")]
    #[default]
    ResumeLast,
}

/// Attach to an already-active in-process session by ID. Unlike `resume`, this does NOT re-load from disk; the session must already be loaded by an earlier `create`/`resume` call. Returns `status: 'not_found'` when no active session matches the id. Useful for in-process consumers that need a fresh API handle to a session opened elsewhere (e.g., a peer foreground-session switch).
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenAttachKind {
    #[serde(rename = "attach")]
    #[default]
    Attach,
}

/// Connect to a live remote session.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenRemoteKind {
    #[serde(rename = "remote")]
    #[default]
    Remote,
}

/// Create a new cloud (coding-agent) session.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenCloudKind {
    #[serde(rename = "cloud")]
    #[default]
    Cloud,
}

/// Fetch a remote session and hand it off to a new local session.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenHandoffKind {
    #[serde(rename = "handoff")]
    #[default]
    Handoff,
}

/// Task type determines the handoff strategy (CCA fetches events; CLI prepares a transient session).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenHandoffTaskType {
    /// GitHub Copilot coding agent task.
    #[serde(rename = "cca")]
    Cca,
    /// CLI remote task.
    #[serde(rename = "cli")]
    Cli,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Open a session by creating, resuming, attaching, connecting to a remote, or handing off.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum SessionOpenParams {
    Create(SessionsOpenCreate),
    Resume(SessionsOpenResume),
    ResumeLast(SessionsOpenResumeLast),
    Attach(SessionsOpenAttach),
    Remote(SessionsOpenRemote),
    Cloud(SessionsOpenCloud),
    Handoff(SessionsOpenHandoff),
}

/// Step status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenProgressStatus {
    /// The step has started and has not yet finished.
    #[serde(rename = "in-progress")]
    InProgress,
    /// The step has completed successfully.
    #[serde(rename = "complete")]
    Complete,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Handoff step.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenProgressStep {
    /// Loading the source session's events from the remote service.
    #[serde(rename = "load-session")]
    LoadSession,
    /// Validating that the local repository matches the remote session's repository.
    #[serde(rename = "validate-repo")]
    ValidateRepo,
    /// Checking the local working tree for uncommitted changes that would block the handoff.
    #[serde(rename = "check-changes")]
    CheckChanges,
    /// Checking out the branch associated with the remote session in the local working tree.
    #[serde(rename = "checkout-branch")]
    CheckoutBranch,
    /// Creating the new local session and seeding it with the source session's events.
    #[serde(rename = "create-session")]
    CreateSession,
    /// Persisting the newly-created local session to disk.
    #[serde(rename = "save-session")]
    SaveSession,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Outcome of the open request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenStatus {
    /// A new session was created.
    #[serde(rename = "created")]
    Created,
    /// An existing session was loaded or reattached.
    #[serde(rename = "resumed")]
    Resumed,
    /// No matching persisted session was found.
    #[serde(rename = "not_found")]
    NotFound,
    /// Connected to an existing remote session.
    #[serde(rename = "connected")]
    Connected,
    /// Remote session was handed off to a new local session.
    #[serde(rename = "handed_off")]
    HandedOff,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Copilot plan tier used by the session quota projection.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionQuotaPlanTier {
    /// Copilot Free.
    #[serde(rename = "free")]
    Free,
    /// Copilot education entitlement.
    #[serde(rename = "edu")]
    Edu,
    /// Copilot Pro.
    #[serde(rename = "pro")]
    Pro,
    /// Copilot Pro+.
    #[serde(rename = "pro_plus")]
    ProPlus,
    /// Copilot Business.
    #[serde(rename = "business")]
    Business,
    /// Copilot Enterprise.
    #[serde(rename = "enterprise")]
    Enterprise,
    /// Copilot Access.
    #[serde(rename = "access")]
    Access,
    /// Copilot Max.
    #[serde(rename = "max")]
    Max,
    /// The account's plan tier is not known.
    #[serde(rename = "unknown")]
    UnknownValue,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// SDK-side token authentication; the host configured the token directly via the SDK.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SettableTokenAuthInfoType {
    #[serde(rename = "token")]
    #[default]
    Token,
}

/// Authentication credentials accepted by session.gitHubAuth.setCredentials. Session-owned token-provider identities cannot be installed through this method.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum SettableAuthInfo {
    Hmac(HMACAuthInfo),
    Env(EnvAuthInfo),
    Token(SettableTokenAuthInfo),
    CopilotApiToken(CopilotApiTokenAuthInfo),
    User(UserAuthInfo),
    Account(AccountAuthInfo),
    GhCli(GhCliAuthInfo),
    ApiKey(ApiKeyAuthInfo),
}

/// Rust-owned settings predicates exposed across the SDK boundary. Raw feature-flag names are intentionally not part of the contract.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionSettingsPredicateName {
    /// Whether the security-tools feature flag enables security tool wiring.
    #[serde(rename = "securityToolsEnabled")]
    SecurityToolsEnabled,
    /// Whether third-party security tools should receive the security prompt.
    #[serde(rename = "thirdPartySecurityPromptEnabled")]
    ThirdPartySecurityPromptEnabled,
    /// Whether validation may run in parallel.
    #[serde(rename = "parallelValidationEnabled")]
    ParallelValidationEnabled,
    /// Whether runtime timing telemetry is enabled.
    #[serde(rename = "runtimeTimingTelemetryEnabled")]
    RuntimeTimingTelemetryEnabled,
    /// Whether the co-author hook is enabled.
    #[serde(rename = "coAuthorHookEnabled")]
    CoAuthorHookEnabled,
    /// Whether Chronicle integration is enabled.
    #[serde(rename = "chronicleEnabled")]
    ChronicleEnabled,
    /// Whether content-exclusion policy may self-fetch data.
    #[serde(rename = "contentExclusionSelfFetchEnabled")]
    ContentExclusionSelfFetchEnabled,
    /// Whether Claude Opus token-limit caps should be applied.
    #[serde(rename = "capClaudeOpusTokenLimitsEnabled")]
    CapClaudeOpusTokenLimitsEnabled,
    /// Whether code-review behavior is enabled.
    #[serde(rename = "codeReviewFeatureEnabled")]
    CodeReviewFeatureEnabled,
    /// Whether CCA should use the TypeScript autofind behavior.
    #[serde(rename = "ccaUseTsAutofindEnabled")]
    CcaUseTsAutofindEnabled,
    /// Whether the dependency checker is enabled.
    #[serde(rename = "dependencyCheckerEnabled")]
    DependencyCheckerEnabled,
    /// Whether the Dependabot checker is enabled.
    #[serde(rename = "dependabotCheckerEnabled")]
    DependabotCheckerEnabled,
    /// Whether the CodeQL checker is enabled.
    #[serde(rename = "codeqlCheckerEnabled")]
    CodeqlCheckerEnabled,
    /// Whether trivial-change handling is enabled.
    #[serde(rename = "trivialChangeEnabled")]
    TrivialChangeEnabled,
    /// Whether trivial-change skip behavior is enabled.
    #[serde(rename = "trivialChangeSkipEnabled")]
    TrivialChangeSkipEnabled,
    /// Whether trivial-change handling is enabled for code review.
    #[serde(rename = "trivialChangeEnabledForCodeReview")]
    TrivialChangeEnabledForCodeReview,
    /// Whether trivial-change skip behavior is enabled for code review.
    #[serde(rename = "trivialChangeSkipEnabledForCodeReview")]
    TrivialChangeSkipEnabledForCodeReview,
    /// Whether trivial-change handling is enabled for a specific tool.
    #[serde(rename = "trivialChangeEnabledForTool")]
    TrivialChangeEnabledForTool,
    /// Whether trivial-change skip behavior is enabled for a specific tool.
    #[serde(rename = "trivialChangeSkipEnabledForTool")]
    TrivialChangeSkipEnabledForTool,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Which session sources to include. Defaults to `local` for backward compatibility.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionSource {
    /// Return only local sessions.
    #[serde(rename = "local")]
    Local,
    /// Return only remote sessions.
    #[serde(rename = "remote")]
    Remote,
    /// Return both local and remote sessions.
    #[serde(rename = "all")]
    All,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether task execution is synchronously awaited or managed in the background
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskExecutionMode {
    /// The task was started with synchronous waiting.
    #[serde(rename = "sync")]
    Sync,
    /// The task is managed in the background.
    #[serde(rename = "background")]
    Background,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current lifecycle status of the task
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskStatus {
    /// The task is actively executing.
    #[serde(rename = "running")]
    Running,
    /// The task is waiting for additional input.
    #[serde(rename = "idle")]
    Idle,
    /// The task finished successfully.
    #[serde(rename = "completed")]
    Completed,
    /// The task finished with an error.
    #[serde(rename = "failed")]
    Failed,
    /// The task was cancelled before completion.
    #[serde(rename = "cancelled")]
    Cancelled,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Task kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskAgentInfoType {
    #[serde(rename = "agent")]
    #[default]
    Agent,
}

/// Progress kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskAgentProgressType {
    #[serde(rename = "agent")]
    #[default]
    Agent,
}

/// Active status a client owner may publish with a progress update.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskClientActiveStatus {
    /// The external owner is actively working.
    #[serde(rename = "running")]
    Running,
    /// The external owner is connected but waiting.
    #[serde(rename = "idle")]
    Idle,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Client-owned tasks always execute outside the runtime in background mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskClientExecutionMode {
    #[serde(rename = "background")]
    Background,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Connection class owning a client task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskClientOwnerKind {
    /// A discovered extension connection owns the task.
    #[serde(rename = "extension")]
    Extension,
    /// A generic SDK connection owns the task.
    #[serde(rename = "sdk")]
    Sdk,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Presence of the task's bound join.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskClientOwnerPresence {
    /// The bound session join is connected.
    #[serde(rename = "connected")]
    Connected,
    /// The bound session join is disconnected.
    #[serde(rename = "disconnected")]
    Disconnected,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Lifecycle status of a client-owned task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskClientStatus {
    /// The external owner is actively working.
    #[serde(rename = "running")]
    Running,
    /// The external owner is connected but waiting.
    #[serde(rename = "idle")]
    Idle,
    /// The owner reported successful completion.
    #[serde(rename = "completed")]
    Completed,
    /// The owner reported failure.
    #[serde(rename = "failed")]
    Failed,
    /// The owner reported or confirmed cancellation.
    #[serde(rename = "cancelled")]
    Cancelled,
    /// The bound owner join disappeared; external executor state is unknown.
    #[serde(rename = "orphaned")]
    Orphaned,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator for a client-owned task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskClientType {
    #[serde(rename = "client")]
    Client,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Client task update variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskClientUpdateProgressKind {
    #[serde(rename = "progress")]
    #[default]
    Progress,
}

/// Client task update variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskClientUpdateCompletedKind {
    #[serde(rename = "completed")]
    #[default]
    Completed,
}

/// Client task update variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskClientUpdateFailedKind {
    #[serde(rename = "failed")]
    #[default]
    Failed,
}

/// Client task update variant discriminator.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskClientUpdateCancelledKind {
    #[serde(rename = "cancelled")]
    #[default]
    Cancelled,
}

/// Progress or terminal update for a client-owned task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum TaskClientUpdate {
    Progress(TaskClientUpdateProgress),
    Completed(TaskClientUpdateCompleted),
    Failed(TaskClientUpdateFailed),
    Cancelled(TaskClientUpdateCancelled),
}

/// Whether the shell runs inside a managed PTY session or as an independent background process
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskShellInfoAttachmentMode {
    /// The shell runs in a managed PTY session.
    #[serde(rename = "attached")]
    Attached,
    /// The shell runs as an independent background process.
    #[serde(rename = "detached")]
    Detached,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Task kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskShellInfoType {
    #[serde(rename = "shell")]
    #[default]
    Shell,
}

/// Progress kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskShellProgressType {
    #[serde(rename = "shell")]
    #[default]
    Shell,
}

/// Execution outcome classification.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ToolResultType {
    /// The tool completed successfully.
    #[serde(rename = "success")]
    Success,
    /// The tool failed.
    #[serde(rename = "failure")]
    Failure,
    /// The tool exceeded its execution timeout.
    #[serde(rename = "timeout")]
    Timeout,
    /// The tool request was rejected before execution.
    #[serde(rename = "rejected")]
    Rejected,
    /// Permission policy denied the tool request.
    #[serde(rename = "denied")]
    Denied,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Canonical result returned by a session tool.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum ToolResult {
    String(String),
    ToolResultExpanded(ToolResultExpanded),
}

/// Diagnostic source identifying the typed details payload.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionDiagnosticsReadResultEntriesItemSource {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}
