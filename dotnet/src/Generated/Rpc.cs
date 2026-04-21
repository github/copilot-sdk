/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

#pragma warning disable CS0612 // Type or member is obsolete
#pragma warning disable CS0618 // Type or member is obsolete (with message)

using System.ComponentModel.DataAnnotations;
using System.Diagnostics.CodeAnalysis;
using System.Text.Json;
using System.Text.Json.Serialization;
using StreamJsonRpc;

namespace GitHub.Copilot.SDK.Rpc;

/// <summary>Diagnostic IDs for the Copilot SDK.</summary>
internal static class Diagnostics
{
    /// <summary>Indicates an experimental API that may change or be removed.</summary>
    internal const string Experimental = "GHCP001";
}

/// <summary>RPC data type for Ping operations.</summary>
public sealed class PingResult
{
    /// <summary>Echoed message (or default greeting).</summary>
    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    /// <summary>Server protocol version number.</summary>
    [JsonPropertyName("protocolVersion")]
    public long ProtocolVersion { get; set; }

    /// <summary>Server timestamp in milliseconds.</summary>
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }
}

/// <summary>RPC data type for Ping operations.</summary>
internal sealed class PingRequest
{
    /// <summary>Optional message to echo back.</summary>
    [JsonPropertyName("message")]
    public string? Message { get; set; }
}

/// <summary>Billing information.</summary>
public sealed class ModelBilling
{
    /// <summary>Billing cost multiplier relative to the base rate.</summary>
    [JsonPropertyName("multiplier")]
    public double Multiplier { get; set; }
}

/// <summary>Vision-specific limits.</summary>
public sealed class ModelCapabilitiesLimitsVision
{
    /// <summary>Maximum image size in bytes.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("max_prompt_image_size")]
    public long MaxPromptImageSize { get; set; }

    /// <summary>Maximum number of images per prompt.</summary>
    [Range((double)1, (double)long.MaxValue)]
    [JsonPropertyName("max_prompt_images")]
    public long MaxPromptImages { get; set; }

    /// <summary>MIME types the model accepts.</summary>
    [JsonPropertyName("supported_media_types")]
    public IList<string> SupportedMediaTypes { get => field ??= []; set; }
}

/// <summary>Token limits for prompts, outputs, and context window.</summary>
public sealed class ModelCapabilitiesLimits
{
    /// <summary>Maximum total context window size in tokens.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("max_context_window_tokens")]
    public long? MaxContextWindowTokens { get; set; }

    /// <summary>Maximum number of output/completion tokens.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("max_output_tokens")]
    public long? MaxOutputTokens { get; set; }

    /// <summary>Maximum number of prompt/input tokens.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("max_prompt_tokens")]
    public long? MaxPromptTokens { get; set; }

    /// <summary>Vision-specific limits.</summary>
    [JsonPropertyName("vision")]
    public ModelCapabilitiesLimitsVision? Vision { get; set; }
}

/// <summary>Feature flags indicating what the model supports.</summary>
public sealed class ModelCapabilitiesSupports
{
    /// <summary>Whether this model supports reasoning effort configuration.</summary>
    [JsonPropertyName("reasoningEffort")]
    public bool? ReasoningEffort { get; set; }

    /// <summary>Whether this model supports vision/image input.</summary>
    [JsonPropertyName("vision")]
    public bool? Vision { get; set; }
}

/// <summary>Model capabilities and limits.</summary>
public sealed class ModelCapabilities
{
    /// <summary>Token limits for prompts, outputs, and context window.</summary>
    [JsonPropertyName("limits")]
    public ModelCapabilitiesLimits? Limits { get; set; }

    /// <summary>Feature flags indicating what the model supports.</summary>
    [JsonPropertyName("supports")]
    public ModelCapabilitiesSupports? Supports { get; set; }
}

/// <summary>Policy state (if applicable).</summary>
public sealed class ModelPolicy
{
    /// <summary>Current policy state for this model.</summary>
    [JsonPropertyName("state")]
    public string State { get; set; } = string.Empty;

    /// <summary>Usage terms or conditions for this model.</summary>
    [JsonPropertyName("terms")]
    public string Terms { get; set; } = string.Empty;
}

/// <summary>RPC data type for Model operations.</summary>
public sealed class Model
{
    /// <summary>Billing information.</summary>
    [JsonPropertyName("billing")]
    public ModelBilling? Billing { get; set; }

    /// <summary>Model capabilities and limits.</summary>
    [JsonPropertyName("capabilities")]
    public ModelCapabilities Capabilities { get => field ??= new(); set; }

    /// <summary>Default reasoning effort level (only present if model supports reasoning effort).</summary>
    [JsonPropertyName("defaultReasoningEffort")]
    public string? DefaultReasoningEffort { get; set; }

    /// <summary>Model identifier (e.g., "claude-sonnet-4.5").</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Display name.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Policy state (if applicable).</summary>
    [JsonPropertyName("policy")]
    public ModelPolicy? Policy { get; set; }

    /// <summary>Supported reasoning effort levels (only present if model supports reasoning effort).</summary>
    [JsonPropertyName("supportedReasoningEfforts")]
    public IList<string>? SupportedReasoningEfforts { get; set; }
}

/// <summary>RPC data type for ModelList operations.</summary>
public sealed class ModelList
{
    /// <summary>List of available models with full metadata.</summary>
    [JsonPropertyName("models")]
    public IList<Model> Models { get => field ??= []; set; }
}

/// <summary>RPC data type for Tool operations.</summary>
public sealed class Tool
{
    /// <summary>Description of what the tool does.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    /// <summary>Optional instructions for how to use this tool effectively.</summary>
    [JsonPropertyName("instructions")]
    public string? Instructions { get; set; }

    /// <summary>Tool identifier (e.g., "bash", "grep", "str_replace_editor").</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP tools).</summary>
    [JsonPropertyName("namespacedName")]
    public string? NamespacedName { get; set; }

    /// <summary>JSON Schema for the tool's input parameters.</summary>
    [JsonPropertyName("parameters")]
    public IDictionary<string, object>? Parameters { get; set; }
}

/// <summary>RPC data type for ToolList operations.</summary>
public sealed class ToolList
{
    /// <summary>List of available built-in tools with metadata.</summary>
    [JsonPropertyName("tools")]
    public IList<Tool> Tools { get => field ??= []; set; }
}

/// <summary>RPC data type for ToolsList operations.</summary>
internal sealed class ToolsListRequest
{
    /// <summary>Optional model ID — when provided, the returned tool list reflects model-specific overrides.</summary>
    [JsonPropertyName("model")]
    public string? Model { get; set; }
}

/// <summary>RPC data type for AccountQuotaSnapshot operations.</summary>
public sealed class AccountQuotaSnapshot
{
    /// <summary>Number of requests included in the entitlement.</summary>
    [JsonPropertyName("entitlementRequests")]
    public long EntitlementRequests { get; set; }

    /// <summary>Whether the user has an unlimited usage entitlement.</summary>
    [JsonPropertyName("isUnlimitedEntitlement")]
    public bool IsUnlimitedEntitlement { get; set; }

    /// <summary>Number of overage requests made this period.</summary>
    [Range(0, double.MaxValue)]
    [JsonPropertyName("overage")]
    public double Overage { get; set; }

    /// <summary>Whether overage is allowed when quota is exhausted.</summary>
    [JsonPropertyName("overageAllowedWithExhaustedQuota")]
    public bool OverageAllowedWithExhaustedQuota { get; set; }

    /// <summary>Percentage of entitlement remaining.</summary>
    [JsonPropertyName("remainingPercentage")]
    public double RemainingPercentage { get; set; }

    /// <summary>Date when the quota resets (ISO 8601 string).</summary>
    [JsonPropertyName("resetDate")]
    public string? ResetDate { get; set; }

    /// <summary>Whether usage is still permitted after quota exhaustion.</summary>
    [JsonPropertyName("usageAllowedWithExhaustedQuota")]
    public bool UsageAllowedWithExhaustedQuota { get; set; }

    /// <summary>Number of requests used so far this period.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("usedRequests")]
    public long UsedRequests { get; set; }
}

/// <summary>RPC data type for AccountGetQuota operations.</summary>
public sealed class AccountGetQuotaResult
{
    /// <summary>Quota snapshots keyed by type (e.g., chat, completions, premium_interactions).</summary>
    [JsonPropertyName("quotaSnapshots")]
    public IDictionary<string, AccountQuotaSnapshot> QuotaSnapshots { get => field ??= new Dictionary<string, AccountQuotaSnapshot>(); set; }
}

/// <summary>RPC data type for DiscoveredMcpServer operations.</summary>
public sealed class DiscoveredMcpServer
{
    /// <summary>Whether the server is enabled (not in the disabled list).</summary>
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }

    /// <summary>Server name (config key).</summary>
    [RegularExpression("^[0-9a-zA-Z_.@-]+(\\/[0-9a-zA-Z_.@-]+)*$")]
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Configuration source.</summary>
    [JsonPropertyName("source")]
    public DiscoveredMcpServerSource Source { get; set; }

    /// <summary>Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio).</summary>
    [JsonPropertyName("type")]
    public DiscoveredMcpServerType? Type { get; set; }
}

/// <summary>RPC data type for McpDiscover operations.</summary>
public sealed class McpDiscoverResult
{
    /// <summary>MCP servers discovered from all sources.</summary>
    [JsonPropertyName("servers")]
    public IList<DiscoveredMcpServer> Servers { get => field ??= []; set; }
}

/// <summary>RPC data type for McpDiscover operations.</summary>
internal sealed class McpDiscoverRequest
{
    /// <summary>Working directory used as context for discovery (e.g., plugin resolution).</summary>
    [JsonPropertyName("workingDirectory")]
    public string? WorkingDirectory { get; set; }
}

/// <summary>RPC data type for McpConfigList operations.</summary>
public sealed class McpConfigList
{
    /// <summary>All MCP servers from user config, keyed by name.</summary>
    [JsonPropertyName("servers")]
    public IDictionary<string, object> Servers { get => field ??= new Dictionary<string, object>(); set; }
}

/// <summary>RPC data type for McpConfigAdd operations.</summary>
internal sealed class McpConfigAddRequest
{
    /// <summary>MCP server configuration (local/stdio or remote/http).</summary>
    [JsonPropertyName("config")]
    public object Config { get; set; } = null!;

    /// <summary>Unique name for the MCP server.</summary>
    [RegularExpression("^[0-9a-zA-Z_.@-]+(\\/[0-9a-zA-Z_.@-]+)*$")]
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>RPC data type for McpConfigUpdate operations.</summary>
internal sealed class McpConfigUpdateRequest
{
    /// <summary>MCP server configuration (local/stdio or remote/http).</summary>
    [JsonPropertyName("config")]
    public object Config { get; set; } = null!;

    /// <summary>Name of the MCP server to update.</summary>
    [RegularExpression("^[0-9a-zA-Z_.@-]+(\\/[0-9a-zA-Z_.@-]+)*$")]
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>RPC data type for McpConfigRemove operations.</summary>
internal sealed class McpConfigRemoveRequest
{
    /// <summary>Name of the MCP server to remove.</summary>
    [RegularExpression("^[0-9a-zA-Z_.@-]+(\\/[0-9a-zA-Z_.@-]+)*$")]
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>RPC data type for ServerSkill operations.</summary>
public sealed class ServerSkill
{
    /// <summary>Description of what the skill does.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    /// <summary>Whether the skill is currently enabled (based on global config).</summary>
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }

    /// <summary>Unique identifier for the skill.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Absolute path to the skill file.</summary>
    [JsonPropertyName("path")]
    public string? Path { get; set; }

    /// <summary>The project path this skill belongs to (only for project/inherited skills).</summary>
    [JsonPropertyName("projectPath")]
    public string? ProjectPath { get; set; }

    /// <summary>Source location type (e.g., project, personal-copilot, plugin, builtin).</summary>
    [JsonPropertyName("source")]
    public string Source { get; set; } = string.Empty;

    /// <summary>Whether the skill can be invoked by the user as a slash command.</summary>
    [JsonPropertyName("userInvocable")]
    public bool UserInvocable { get; set; }
}

/// <summary>RPC data type for ServerSkillList operations.</summary>
public sealed class ServerSkillList
{
    /// <summary>All discovered skills across all sources.</summary>
    [JsonPropertyName("skills")]
    public IList<ServerSkill> Skills { get => field ??= []; set; }
}

/// <summary>RPC data type for SkillsDiscover operations.</summary>
internal sealed class SkillsDiscoverRequest
{
    /// <summary>Optional list of project directory paths to scan for project-scoped skills.</summary>
    [JsonPropertyName("projectPaths")]
    public IList<string>? ProjectPaths { get; set; }

    /// <summary>Optional list of additional skill directory paths to include.</summary>
    [JsonPropertyName("skillDirectories")]
    public IList<string>? SkillDirectories { get; set; }
}

/// <summary>RPC data type for SkillsConfigSetDisabledSkills operations.</summary>
internal sealed class SkillsConfigSetDisabledSkillsRequest
{
    /// <summary>List of skill names to disable.</summary>
    [JsonPropertyName("disabledSkills")]
    public IList<string> DisabledSkills { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionFsSetProvider operations.</summary>
public sealed class SessionFsSetProviderResult
{
    /// <summary>Whether the provider was set successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for SessionFsSetProvider operations.</summary>
internal sealed class SessionFsSetProviderRequest
{
    /// <summary>Path conventions used by this filesystem.</summary>
    [JsonPropertyName("conventions")]
    public SessionFsSetProviderConventions Conventions { get; set; }

    /// <summary>Initial working directory for sessions.</summary>
    [JsonPropertyName("initialCwd")]
    public string InitialCwd { get; set; } = string.Empty;

    /// <summary>Path within each session's SessionFs where the runtime stores files for that session.</summary>
    [JsonPropertyName("sessionStatePath")]
    public string SessionStatePath { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionsFork operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionsForkResult
{
    /// <summary>The new forked session's ID.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionsFork operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsForkRequest
{
    /// <summary>Source session ID to fork from.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Optional event ID boundary. When provided, the fork includes only events before this ID (exclusive). When omitted, all events are included.</summary>
    [JsonPropertyName("toEventId")]
    public string? ToEventId { get; set; }
}

/// <summary>RPC data type for Log operations.</summary>
public sealed class LogResult
{
    /// <summary>The unique identifier of the emitted session event.</summary>
    [JsonPropertyName("eventId")]
    public Guid EventId { get; set; }
}

/// <summary>RPC data type for Log operations.</summary>
internal sealed class LogRequest
{
    /// <summary>When true, the message is transient and not persisted to the session event log on disk.</summary>
    [JsonPropertyName("ephemeral")]
    public bool? Ephemeral { get; set; }

    /// <summary>Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".</summary>
    [JsonPropertyName("level")]
    public SessionLogLevel? Level { get; set; }

    /// <summary>Human-readable message.</summary>
    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Optional URL the user can open in their browser for more details.</summary>
    [Url]
    [StringSyntax(StringSyntaxAttribute.Uri)]
    [JsonPropertyName("url")]
    public string? Url { get; set; }
}

/// <summary>RPC data type for CurrentModel operations.</summary>
public sealed class CurrentModel
{
    /// <summary>Currently active model identifier.</summary>
    [JsonPropertyName("modelId")]
    public string? ModelId { get; set; }
}

/// <summary>RPC data type for SessionModelGetCurrent operations.</summary>
internal sealed class SessionModelGetCurrentRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for ModelSwitchTo operations.</summary>
public sealed class ModelSwitchToResult
{
    /// <summary>Currently active model identifier after the switch.</summary>
    [JsonPropertyName("modelId")]
    public string? ModelId { get; set; }
}

/// <summary>RPC data type for ModelCapabilitiesOverrideLimitsVision operations.</summary>
public sealed class ModelCapabilitiesOverrideLimitsVision
{
    /// <summary>Maximum image size in bytes.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("max_prompt_image_size")]
    public long? MaxPromptImageSize { get; set; }

    /// <summary>Maximum number of images per prompt.</summary>
    [Range((double)1, (double)long.MaxValue)]
    [JsonPropertyName("max_prompt_images")]
    public long? MaxPromptImages { get; set; }

    /// <summary>MIME types the model accepts.</summary>
    [JsonPropertyName("supported_media_types")]
    public IList<string>? SupportedMediaTypes { get; set; }
}

/// <summary>Token limits for prompts, outputs, and context window.</summary>
public sealed class ModelCapabilitiesOverrideLimits
{
    /// <summary>Maximum total context window size in tokens.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("max_context_window_tokens")]
    public long? MaxContextWindowTokens { get; set; }

    /// <summary>Gets or sets the <c>max_output_tokens</c> value.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("max_output_tokens")]
    public long? MaxOutputTokens { get; set; }

    /// <summary>Gets or sets the <c>max_prompt_tokens</c> value.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("max_prompt_tokens")]
    public long? MaxPromptTokens { get; set; }

    /// <summary>Gets or sets the <c>vision</c> value.</summary>
    [JsonPropertyName("vision")]
    public ModelCapabilitiesOverrideLimitsVision? Vision { get; set; }
}

/// <summary>Feature flags indicating what the model supports.</summary>
public sealed class ModelCapabilitiesOverrideSupports
{
    /// <summary>Gets or sets the <c>reasoningEffort</c> value.</summary>
    [JsonPropertyName("reasoningEffort")]
    public bool? ReasoningEffort { get; set; }

    /// <summary>Gets or sets the <c>vision</c> value.</summary>
    [JsonPropertyName("vision")]
    public bool? Vision { get; set; }
}

/// <summary>Override individual model capabilities resolved by the runtime.</summary>
public sealed class ModelCapabilitiesOverride
{
    /// <summary>Token limits for prompts, outputs, and context window.</summary>
    [JsonPropertyName("limits")]
    public ModelCapabilitiesOverrideLimits? Limits { get; set; }

    /// <summary>Feature flags indicating what the model supports.</summary>
    [JsonPropertyName("supports")]
    public ModelCapabilitiesOverrideSupports? Supports { get; set; }
}

/// <summary>RPC data type for ModelSwitchTo operations.</summary>
internal sealed class ModelSwitchToRequest
{
    /// <summary>Override individual model capabilities resolved by the runtime.</summary>
    [JsonPropertyName("modelCapabilities")]
    public ModelCapabilitiesOverride? ModelCapabilities { get; set; }

    /// <summary>Model identifier to switch to.</summary>
    [JsonPropertyName("modelId")]
    public string ModelId { get; set; } = string.Empty;

    /// <summary>Reasoning effort level to use for the model.</summary>
    [JsonPropertyName("reasoningEffort")]
    public string? ReasoningEffort { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionModeGet operations.</summary>
internal sealed class SessionModeGetRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for ModeSet operations.</summary>
internal sealed class ModeSetRequest
{
    /// <summary>The agent mode. Valid values: "interactive", "plan", "autopilot".</summary>
    [JsonPropertyName("mode")]
    public SessionMode Mode { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for NameGet operations.</summary>
public sealed class NameGetResult
{
    /// <summary>The session name, falling back to the auto-generated summary, or null if neither exists.</summary>
    [JsonPropertyName("name")]
    public string? Name { get; set; }
}

/// <summary>RPC data type for SessionNameGet operations.</summary>
internal sealed class SessionNameGetRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for NameSet operations.</summary>
internal sealed class NameSetRequest
{
    /// <summary>New session name (1–100 characters, trimmed of leading/trailing whitespace).</summary>
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [MaxLength(100)]
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for PlanRead operations.</summary>
public sealed class PlanReadResult
{
    /// <summary>The content of the plan file, or null if it does not exist.</summary>
    [JsonPropertyName("content")]
    public string? Content { get; set; }

    /// <summary>Whether the plan file exists in the workspace.</summary>
    [JsonPropertyName("exists")]
    public bool Exists { get; set; }

    /// <summary>Absolute file path of the plan file, or null if workspace is not enabled.</summary>
    [JsonPropertyName("path")]
    public string? Path { get; set; }
}

/// <summary>RPC data type for SessionPlanRead operations.</summary>
internal sealed class SessionPlanReadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for PlanUpdate operations.</summary>
internal sealed class PlanUpdateRequest
{
    /// <summary>The new content for the plan file.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionPlanDelete operations.</summary>
internal sealed class SessionPlanDeleteRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for WorkspacesGetWorkspaceResultWorkspace operations.</summary>
public sealed class WorkspacesGetWorkspaceResultWorkspace
{
    /// <summary>Gets or sets the <c>branch</c> value.</summary>
    [JsonPropertyName("branch")]
    public string? Branch { get; set; }

    /// <summary>Gets or sets the <c>chronicle_sync_dismissed</c> value.</summary>
    [JsonPropertyName("chronicle_sync_dismissed")]
    public bool? ChronicleSyncDismissed { get; set; }

    /// <summary>Gets or sets the <c>created_at</c> value.</summary>
    [JsonPropertyName("created_at")]
    public DateTimeOffset? CreatedAt { get; set; }

    /// <summary>Gets or sets the <c>cwd</c> value.</summary>
    [JsonPropertyName("cwd")]
    public string? Cwd { get; set; }

    /// <summary>Gets or sets the <c>git_root</c> value.</summary>
    [JsonPropertyName("git_root")]
    public string? GitRoot { get; set; }

    /// <summary>Gets or sets the <c>host_type</c> value.</summary>
    [JsonPropertyName("host_type")]
    public WorkspacesGetWorkspaceResultWorkspaceHostType? HostType { get; set; }

    /// <summary>Gets or sets the <c>id</c> value.</summary>
    [JsonPropertyName("id")]
    public Guid Id { get; set; }

    /// <summary>Gets or sets the <c>mc_last_event_id</c> value.</summary>
    [JsonPropertyName("mc_last_event_id")]
    public string? McLastEventId { get; set; }

    /// <summary>Gets or sets the <c>mc_session_id</c> value.</summary>
    [JsonPropertyName("mc_session_id")]
    public string? McSessionId { get; set; }

    /// <summary>Gets or sets the <c>mc_task_id</c> value.</summary>
    [JsonPropertyName("mc_task_id")]
    public string? McTaskId { get; set; }

    /// <summary>Gets or sets the <c>name</c> value.</summary>
    [JsonPropertyName("name")]
    public string? Name { get; set; }

    /// <summary>Gets or sets the <c>remote_steerable</c> value.</summary>
    [JsonPropertyName("remote_steerable")]
    public bool? RemoteSteerable { get; set; }

    /// <summary>Gets or sets the <c>repository</c> value.</summary>
    [JsonPropertyName("repository")]
    public string? Repository { get; set; }

    /// <summary>Gets or sets the <c>session_sync_level</c> value.</summary>
    [JsonPropertyName("session_sync_level")]
    public WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel? SessionSyncLevel { get; set; }

    /// <summary>Gets or sets the <c>summary</c> value.</summary>
    [JsonPropertyName("summary")]
    public string? Summary { get; set; }

    /// <summary>Gets or sets the <c>summary_count</c> value.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("summary_count")]
    public long? SummaryCount { get; set; }

    /// <summary>Gets or sets the <c>updated_at</c> value.</summary>
    [JsonPropertyName("updated_at")]
    public DateTimeOffset? UpdatedAt { get; set; }
}

/// <summary>RPC data type for WorkspacesGetWorkspace operations.</summary>
public sealed class WorkspacesGetWorkspaceResult
{
    /// <summary>Current workspace metadata, or null if not available.</summary>
    [JsonPropertyName("workspace")]
    public WorkspacesGetWorkspaceResultWorkspace? Workspace { get; set; }
}

/// <summary>RPC data type for SessionWorkspacesGetWorkspace operations.</summary>
internal sealed class SessionWorkspacesGetWorkspaceRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for WorkspacesListFiles operations.</summary>
public sealed class WorkspacesListFilesResult
{
    /// <summary>Relative file paths in the workspace files directory.</summary>
    [JsonPropertyName("files")]
    public IList<string> Files { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionWorkspacesListFiles operations.</summary>
internal sealed class SessionWorkspacesListFilesRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for WorkspacesReadFile operations.</summary>
public sealed class WorkspacesReadFileResult
{
    /// <summary>File content as a UTF-8 string.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;
}

/// <summary>RPC data type for WorkspacesReadFile operations.</summary>
internal sealed class WorkspacesReadFileRequest
{
    /// <summary>Relative path within the workspace files directory.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for WorkspacesCreateFile operations.</summary>
internal sealed class WorkspacesCreateFileRequest
{
    /// <summary>File content to write as a UTF-8 string.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Relative path within the workspace files directory.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for InstructionsSources operations.</summary>
public sealed class InstructionsSources
{
    /// <summary>Glob pattern from frontmatter — when set, this instruction applies only to matching files.</summary>
    [JsonPropertyName("applyTo")]
    public string? ApplyTo { get; set; }

    /// <summary>Raw content of the instruction file.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Short description (body after frontmatter) for use in instruction tables.</summary>
    [JsonPropertyName("description")]
    public string? Description { get; set; }

    /// <summary>Unique identifier for this source (used for toggling).</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Human-readable label.</summary>
    [JsonPropertyName("label")]
    public string Label { get; set; } = string.Empty;

    /// <summary>Where this source lives — used for UI grouping.</summary>
    [JsonPropertyName("location")]
    public InstructionsSourcesLocation Location { get; set; }

    /// <summary>File path relative to repo or absolute for home.</summary>
    [JsonPropertyName("sourcePath")]
    public string SourcePath { get; set; } = string.Empty;

    /// <summary>Category of instruction source — used for merge logic.</summary>
    [JsonPropertyName("type")]
    public InstructionsSourcesType Type { get; set; }
}

/// <summary>RPC data type for InstructionsGetSources operations.</summary>
public sealed class InstructionsGetSourcesResult
{
    /// <summary>Instruction sources for the session.</summary>
    [JsonPropertyName("sources")]
    public IList<InstructionsSources> Sources { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionInstructionsGetSources operations.</summary>
internal sealed class SessionInstructionsGetSourcesRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for FleetStart operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class FleetStartResult
{
    /// <summary>Whether fleet mode was successfully activated.</summary>
    [JsonPropertyName("started")]
    public bool Started { get; set; }
}

/// <summary>RPC data type for FleetStart operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class FleetStartRequest
{
    /// <summary>Optional user prompt to combine with fleet instructions.</summary>
    [JsonPropertyName("prompt")]
    public string? Prompt { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for AgentInfo operations.</summary>
public sealed class AgentInfo
{
    /// <summary>Description of the agent's purpose.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    /// <summary>Human-readable display name.</summary>
    [JsonPropertyName("displayName")]
    public string DisplayName { get; set; } = string.Empty;

    /// <summary>Unique identifier of the custom agent.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>RPC data type for AgentList operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AgentList
{
    /// <summary>Available custom agents.</summary>
    [JsonPropertyName("agents")]
    public IList<AgentInfo> Agents { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionAgentList operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionAgentListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for AgentGetCurrent operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AgentGetCurrentResult
{
    /// <summary>Currently selected custom agent, or null if using the default agent.</summary>
    [JsonPropertyName("agent")]
    public AgentInfo? Agent { get; set; }
}

/// <summary>RPC data type for SessionAgentGetCurrent operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionAgentGetCurrentRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for AgentSelect operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AgentSelectResult
{
    /// <summary>The newly selected custom agent.</summary>
    [JsonPropertyName("agent")]
    public AgentInfo Agent { get => field ??= new(); set; }
}

/// <summary>RPC data type for AgentSelect operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class AgentSelectRequest
{
    /// <summary>Name of the custom agent to select.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionAgentDeselect operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionAgentDeselectRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for AgentReload operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AgentReloadResult
{
    /// <summary>Reloaded custom agents.</summary>
    [JsonPropertyName("agents")]
    public IList<AgentInfo> Agents { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionAgentReload operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionAgentReloadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for Skill operations.</summary>
public sealed class Skill
{
    /// <summary>Description of what the skill does.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    /// <summary>Whether the skill is currently enabled.</summary>
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }

    /// <summary>Unique identifier for the skill.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Absolute path to the skill file.</summary>
    [JsonPropertyName("path")]
    public string? Path { get; set; }

    /// <summary>Source location type (e.g., project, personal, plugin).</summary>
    [JsonPropertyName("source")]
    public string Source { get; set; } = string.Empty;

    /// <summary>Whether the skill can be invoked by the user as a slash command.</summary>
    [JsonPropertyName("userInvocable")]
    public bool UserInvocable { get; set; }
}

/// <summary>RPC data type for SkillList operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SkillList
{
    /// <summary>Available skills.</summary>
    [JsonPropertyName("skills")]
    public IList<Skill> Skills { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionSkillsList operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionSkillsListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SkillsEnable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SkillsEnableRequest
{
    /// <summary>Name of the skill to enable.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SkillsDisable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SkillsDisableRequest
{
    /// <summary>Name of the skill to disable.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionSkillsReload operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionSkillsReloadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for McpServer operations.</summary>
public sealed class McpServer
{
    /// <summary>Error message if the server failed to connect.</summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }

    /// <summary>Server name (config key).</summary>
    [RegularExpression("^[0-9a-zA-Z_.@-]+(\\/[0-9a-zA-Z_.@-]+)*$")]
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Configuration source: user, workspace, plugin, or builtin.</summary>
    [JsonPropertyName("source")]
    public McpServerSource? Source { get; set; }

    /// <summary>Connection status: connected, failed, needs-auth, pending, disabled, or not_configured.</summary>
    [JsonPropertyName("status")]
    public McpServerStatus Status { get; set; }
}

/// <summary>RPC data type for McpServerList operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpServerList
{
    /// <summary>Configured MCP servers.</summary>
    [JsonPropertyName("servers")]
    public IList<McpServer> Servers { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionMcpList operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionMcpListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for McpEnable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class McpEnableRequest
{
    /// <summary>Name of the MCP server to enable.</summary>
    [RegularExpression("^[0-9a-zA-Z_.@-]+(\\/[0-9a-zA-Z_.@-]+)*$")]
    [JsonPropertyName("serverName")]
    public string ServerName { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for McpDisable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class McpDisableRequest
{
    /// <summary>Name of the MCP server to disable.</summary>
    [RegularExpression("^[0-9a-zA-Z_.@-]+(\\/[0-9a-zA-Z_.@-]+)*$")]
    [JsonPropertyName("serverName")]
    public string ServerName { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionMcpReload operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionMcpReloadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for Plugin operations.</summary>
public sealed class Plugin
{
    /// <summary>Whether the plugin is currently enabled.</summary>
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }

    /// <summary>Marketplace the plugin came from.</summary>
    [JsonPropertyName("marketplace")]
    public string Marketplace { get; set; } = string.Empty;

    /// <summary>Plugin name.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Installed version.</summary>
    [JsonPropertyName("version")]
    public string? Version { get; set; }
}

/// <summary>RPC data type for PluginList operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PluginList
{
    /// <summary>Installed plugins.</summary>
    [JsonPropertyName("plugins")]
    public IList<Plugin> Plugins { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionPluginsList operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionPluginsListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for Extension operations.</summary>
public sealed class Extension
{
    /// <summary>Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper').</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Extension name (directory name).</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Process ID if the extension is running.</summary>
    [JsonPropertyName("pid")]
    public long? Pid { get; set; }

    /// <summary>Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/).</summary>
    [JsonPropertyName("source")]
    public ExtensionSource Source { get; set; }

    /// <summary>Current status: running, disabled, failed, or starting.</summary>
    [JsonPropertyName("status")]
    public ExtensionStatus Status { get; set; }
}

/// <summary>RPC data type for ExtensionList operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ExtensionList
{
    /// <summary>Discovered extensions and their current status.</summary>
    [JsonPropertyName("extensions")]
    public IList<Extension> Extensions { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionExtensionsList operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionExtensionsListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for ExtensionsEnable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class ExtensionsEnableRequest
{
    /// <summary>Source-qualified extension ID to enable.</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for ExtensionsDisable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class ExtensionsDisableRequest
{
    /// <summary>Source-qualified extension ID to disable.</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionExtensionsReload operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionExtensionsReloadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for HandleToolCall operations.</summary>
public sealed class HandleToolCallResult
{
    /// <summary>Whether the tool call result was handled successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for ToolsHandlePendingToolCall operations.</summary>
internal sealed class ToolsHandlePendingToolCallRequest
{
    /// <summary>Error message if the tool call failed.</summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }

    /// <summary>Request ID of the pending tool call.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Tool call result (string or expanded result object).</summary>
    [JsonPropertyName("result")]
    public object? Result { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for CommandsHandlePendingCommand operations.</summary>
public sealed class CommandsHandlePendingCommandResult
{
    /// <summary>Whether the command was handled successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for CommandsHandlePendingCommand operations.</summary>
internal sealed class CommandsHandlePendingCommandRequest
{
    /// <summary>Error message if the command handler failed.</summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }

    /// <summary>Request ID from the command invocation event.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>The elicitation response (accept with form values, decline, or cancel).</summary>
public sealed class UIElicitationResponse
{
    /// <summary>The user's response: accept (submitted), decline (rejected), or cancel (dismissed).</summary>
    [JsonPropertyName("action")]
    public UIElicitationResponseAction Action { get; set; }

    /// <summary>The form values submitted by the user (present when action is 'accept').</summary>
    [JsonPropertyName("content")]
    public IDictionary<string, object>? Content { get; set; }
}

/// <summary>JSON Schema describing the form fields to present to the user.</summary>
public sealed class UIElicitationSchema
{
    /// <summary>Form field definitions, keyed by field name.</summary>
    [JsonPropertyName("properties")]
    public IDictionary<string, object> Properties { get => field ??= new Dictionary<string, object>(); set; }

    /// <summary>List of required field names.</summary>
    [JsonPropertyName("required")]
    public IList<string>? Required { get; set; }

    /// <summary>Schema type indicator (always 'object').</summary>
    [JsonPropertyName("type")]
    public string Type { get; set; } = string.Empty;
}

/// <summary>RPC data type for UIElicitation operations.</summary>
internal sealed class UIElicitationRequest
{
    /// <summary>Message describing what information is needed from the user.</summary>
    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    /// <summary>JSON Schema describing the form fields to present to the user.</summary>
    [JsonPropertyName("requestedSchema")]
    public UIElicitationSchema RequestedSchema { get => field ??= new(); set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for UIElicitation operations.</summary>
public sealed class UIElicitationResult
{
    /// <summary>Whether the response was accepted. False if the request was already resolved by another client.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for UIHandlePendingElicitation operations.</summary>
internal sealed class UIHandlePendingElicitationRequest
{
    /// <summary>The unique request ID from the elicitation.requested event.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>The elicitation response (accept with form values, decline, or cancel).</summary>
    [JsonPropertyName("result")]
    public UIElicitationResponse Result { get => field ??= new(); set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for PermissionRequest operations.</summary>
public sealed class PermissionRequestResult
{
    /// <summary>Whether the permission request was handled successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Polymorphic base type discriminated by <c>kind</c>.</summary>
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "kind",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(PermissionDecisionApproved), "approved")]
[JsonDerivedType(typeof(PermissionDecisionDeniedByRules), "denied-by-rules")]
[JsonDerivedType(typeof(PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser), "denied-no-approval-rule-and-could-not-request-from-user")]
[JsonDerivedType(typeof(PermissionDecisionDeniedInteractivelyByUser), "denied-interactively-by-user")]
[JsonDerivedType(typeof(PermissionDecisionDeniedByContentExclusionPolicy), "denied-by-content-exclusion-policy")]
[JsonDerivedType(typeof(PermissionDecisionDeniedByPermissionRequestHook), "denied-by-permission-request-hook")]
public partial class PermissionDecision
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("kind")]
    public virtual string Kind { get; set; } = string.Empty;
}


/// <summary>The <c>approved</c> variant of <see cref="PermissionDecision"/>.</summary>
public partial class PermissionDecisionApproved : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "approved";
}

/// <summary>The <c>denied-by-rules</c> variant of <see cref="PermissionDecision"/>.</summary>
public partial class PermissionDecisionDeniedByRules : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "denied-by-rules";

    /// <summary>Rules that denied the request.</summary>
    [JsonPropertyName("rules")]
    public required object[] Rules { get; set; }
}

/// <summary>The <c>denied-no-approval-rule-and-could-not-request-from-user</c> variant of <see cref="PermissionDecision"/>.</summary>
public partial class PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "denied-no-approval-rule-and-could-not-request-from-user";
}

/// <summary>The <c>denied-interactively-by-user</c> variant of <see cref="PermissionDecision"/>.</summary>
public partial class PermissionDecisionDeniedInteractivelyByUser : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "denied-interactively-by-user";

    /// <summary>Optional feedback from the user explaining the denial.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("feedback")]
    public string? Feedback { get; set; }
}

/// <summary>The <c>denied-by-content-exclusion-policy</c> variant of <see cref="PermissionDecision"/>.</summary>
public partial class PermissionDecisionDeniedByContentExclusionPolicy : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "denied-by-content-exclusion-policy";

    /// <summary>Human-readable explanation of why the path was excluded.</summary>
    [JsonPropertyName("message")]
    public required string Message { get; set; }

    /// <summary>File path that triggered the exclusion.</summary>
    [JsonPropertyName("path")]
    public required string Path { get; set; }
}

/// <summary>The <c>denied-by-permission-request-hook</c> variant of <see cref="PermissionDecision"/>.</summary>
public partial class PermissionDecisionDeniedByPermissionRequestHook : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "denied-by-permission-request-hook";

    /// <summary>Whether to interrupt the current agent turn.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("interrupt")]
    public bool? Interrupt { get; set; }

    /// <summary>Optional message from the hook explaining the denial.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("message")]
    public string? Message { get; set; }
}

/// <summary>RPC data type for PermissionDecision operations.</summary>
internal sealed class PermissionDecisionRequest
{
    /// <summary>Request ID of the pending permission request.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Gets or sets the <c>result</c> value.</summary>
    [JsonPropertyName("result")]
    public PermissionDecision Result { get => field ??= new(); set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for ShellExec operations.</summary>
public sealed class ShellExecResult
{
    /// <summary>Unique identifier for tracking streamed output.</summary>
    [JsonPropertyName("processId")]
    public string ProcessId { get; set; } = string.Empty;
}

/// <summary>RPC data type for ShellExec operations.</summary>
internal sealed class ShellExecRequest
{
    /// <summary>Shell command to execute.</summary>
    [JsonPropertyName("command")]
    public string Command { get; set; } = string.Empty;

    /// <summary>Working directory (defaults to session working directory).</summary>
    [JsonPropertyName("cwd")]
    public string? Cwd { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Timeout in milliseconds (default: 30000).</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]
    [JsonPropertyName("timeout")]
    public TimeSpan? Timeout { get; set; }
}

/// <summary>RPC data type for ShellKill operations.</summary>
public sealed class ShellKillResult
{
    /// <summary>Whether the signal was sent successfully.</summary>
    [JsonPropertyName("killed")]
    public bool Killed { get; set; }
}

/// <summary>RPC data type for ShellKill operations.</summary>
internal sealed class ShellKillRequest
{
    /// <summary>Process identifier returned by shell.exec.</summary>
    [JsonPropertyName("processId")]
    public string ProcessId { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Signal to send (default: SIGTERM).</summary>
    [JsonPropertyName("signal")]
    public ShellKillSignal? Signal { get; set; }
}

/// <summary>Post-compaction context window usage breakdown.</summary>
public sealed class HistoryCompactContextWindow
{
    /// <summary>Token count from non-system messages (user, assistant, tool).</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("conversationTokens")]
    public long? ConversationTokens { get; set; }

    /// <summary>Current total tokens in the context window (system + conversation + tool definitions).</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("currentTokens")]
    public long CurrentTokens { get; set; }

    /// <summary>Current number of messages in the conversation.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("messagesLength")]
    public long MessagesLength { get; set; }

    /// <summary>Token count from system message(s).</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("systemTokens")]
    public long? SystemTokens { get; set; }

    /// <summary>Maximum token count for the model's context window.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("tokenLimit")]
    public long TokenLimit { get; set; }

    /// <summary>Token count from tool definitions.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("toolDefinitionsTokens")]
    public long? ToolDefinitionsTokens { get; set; }
}

/// <summary>RPC data type for HistoryCompact operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class HistoryCompactResult
{
    /// <summary>Post-compaction context window usage breakdown.</summary>
    [JsonPropertyName("contextWindow")]
    public HistoryCompactContextWindow? ContextWindow { get; set; }

    /// <summary>Number of messages removed during compaction.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("messagesRemoved")]
    public long MessagesRemoved { get; set; }

    /// <summary>Whether compaction completed successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }

    /// <summary>Number of tokens freed by compaction.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("tokensRemoved")]
    public long TokensRemoved { get; set; }
}

/// <summary>RPC data type for SessionHistoryCompact operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionHistoryCompactRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for HistoryTruncate operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class HistoryTruncateResult
{
    /// <summary>Number of events that were removed.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("eventsRemoved")]
    public long EventsRemoved { get; set; }
}

/// <summary>RPC data type for HistoryTruncate operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class HistoryTruncateRequest
{
    /// <summary>Event ID to truncate to. This event and all events after it are removed from the session.</summary>
    [JsonPropertyName("eventId")]
    public string EventId { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Aggregated code change metrics.</summary>
public sealed class UsageMetricsCodeChanges
{
    /// <summary>Number of distinct files modified.</summary>
    [JsonPropertyName("filesModifiedCount")]
    public long FilesModifiedCount { get; set; }

    /// <summary>Total lines of code added.</summary>
    [JsonPropertyName("linesAdded")]
    public long LinesAdded { get; set; }

    /// <summary>Total lines of code removed.</summary>
    [JsonPropertyName("linesRemoved")]
    public long LinesRemoved { get; set; }
}

/// <summary>Request count and cost metrics for this model.</summary>
public sealed class UsageMetricsModelMetricRequests
{
    /// <summary>User-initiated premium request cost (with multiplier applied).</summary>
    [JsonPropertyName("cost")]
    public double Cost { get; set; }

    /// <summary>Number of API requests made with this model.</summary>
    [JsonPropertyName("count")]
    public long Count { get; set; }
}

/// <summary>Token usage metrics for this model.</summary>
public sealed class UsageMetricsModelMetricUsage
{
    /// <summary>Total tokens read from prompt cache.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("cacheReadTokens")]
    public long CacheReadTokens { get; set; }

    /// <summary>Total tokens written to prompt cache.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("cacheWriteTokens")]
    public long CacheWriteTokens { get; set; }

    /// <summary>Total input tokens consumed.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("inputTokens")]
    public long InputTokens { get; set; }

    /// <summary>Total output tokens produced.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("outputTokens")]
    public long OutputTokens { get; set; }

    /// <summary>Total output tokens used for reasoning.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("reasoningTokens")]
    public long? ReasoningTokens { get; set; }
}

/// <summary>RPC data type for UsageMetricsModelMetric operations.</summary>
public sealed class UsageMetricsModelMetric
{
    /// <summary>Request count and cost metrics for this model.</summary>
    [JsonPropertyName("requests")]
    public UsageMetricsModelMetricRequests Requests { get => field ??= new(); set; }

    /// <summary>Token usage metrics for this model.</summary>
    [JsonPropertyName("usage")]
    public UsageMetricsModelMetricUsage Usage { get => field ??= new(); set; }
}

/// <summary>RPC data type for UsageGetMetrics operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UsageGetMetricsResult
{
    /// <summary>Aggregated code change metrics.</summary>
    [JsonPropertyName("codeChanges")]
    public UsageMetricsCodeChanges CodeChanges { get => field ??= new(); set; }

    /// <summary>Currently active model identifier.</summary>
    [JsonPropertyName("currentModel")]
    public string? CurrentModel { get; set; }

    /// <summary>Input tokens from the most recent main-agent API call.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("lastCallInputTokens")]
    public long LastCallInputTokens { get; set; }

    /// <summary>Output tokens from the most recent main-agent API call.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("lastCallOutputTokens")]
    public long LastCallOutputTokens { get; set; }

    /// <summary>Per-model token and request metrics, keyed by model identifier.</summary>
    [JsonPropertyName("modelMetrics")]
    public IDictionary<string, UsageMetricsModelMetric> ModelMetrics { get => field ??= new Dictionary<string, UsageMetricsModelMetric>(); set; }

    /// <summary>Session start timestamp (epoch milliseconds).</summary>
    [JsonPropertyName("sessionStartTime")]
    public long SessionStartTime { get; set; }

    /// <summary>Total time spent in model API calls (milliseconds).</summary>
    [Range(0, double.MaxValue)]
    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]
    [JsonPropertyName("totalApiDurationMs")]
    public TimeSpan TotalApiDurationMs { get; set; }

    /// <summary>Total user-initiated premium request cost across all models (may be fractional due to multipliers).</summary>
    [JsonPropertyName("totalPremiumRequestCost")]
    public double TotalPremiumRequestCost { get; set; }

    /// <summary>Raw count of user-initiated API requests.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("totalUserRequests")]
    public long TotalUserRequests { get; set; }
}

/// <summary>RPC data type for SessionUsageGetMetrics operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionUsageGetMetricsRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Describes a filesystem error.</summary>
public sealed class SessionFsError
{
    /// <summary>Error classification.</summary>
    [JsonPropertyName("code")]
    public SessionFsErrorCode Code { get; set; }

    /// <summary>Free-form detail about the error, for logging/diagnostics.</summary>
    [JsonPropertyName("message")]
    public string? Message { get; set; }
}

/// <summary>RPC data type for SessionFsReadFile operations.</summary>
public sealed class SessionFsReadFileResult
{
    /// <summary>File content as UTF-8 string.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Describes a filesystem error.</summary>
    [JsonPropertyName("error")]
    public SessionFsError? Error { get; set; }
}

/// <summary>RPC data type for SessionFsReadFile operations.</summary>
public sealed class SessionFsReadFileRequest
{
    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsWriteFile operations.</summary>
public sealed class SessionFsWriteFileRequest
{
    /// <summary>Content to write.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Optional POSIX-style mode for newly created files.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("mode")]
    public long? Mode { get; set; }

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsAppendFile operations.</summary>
public sealed class SessionFsAppendFileRequest
{
    /// <summary>Content to append.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Optional POSIX-style mode for newly created files.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("mode")]
    public long? Mode { get; set; }

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsExists operations.</summary>
public sealed class SessionFsExistsResult
{
    /// <summary>Whether the path exists.</summary>
    [JsonPropertyName("exists")]
    public bool Exists { get; set; }
}

/// <summary>RPC data type for SessionFsExists operations.</summary>
public sealed class SessionFsExistsRequest
{
    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsStat operations.</summary>
public sealed class SessionFsStatResult
{
    /// <summary>ISO 8601 timestamp of creation.</summary>
    [JsonPropertyName("birthtime")]
    public DateTimeOffset Birthtime { get; set; }

    /// <summary>Describes a filesystem error.</summary>
    [JsonPropertyName("error")]
    public SessionFsError? Error { get; set; }

    /// <summary>Whether the path is a directory.</summary>
    [JsonPropertyName("isDirectory")]
    public bool IsDirectory { get; set; }

    /// <summary>Whether the path is a file.</summary>
    [JsonPropertyName("isFile")]
    public bool IsFile { get; set; }

    /// <summary>ISO 8601 timestamp of last modification.</summary>
    [JsonPropertyName("mtime")]
    public DateTimeOffset Mtime { get; set; }

    /// <summary>File size in bytes.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("size")]
    public long Size { get; set; }
}

/// <summary>RPC data type for SessionFsStat operations.</summary>
public sealed class SessionFsStatRequest
{
    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsMkdir operations.</summary>
public sealed class SessionFsMkdirRequest
{
    /// <summary>Optional POSIX-style mode for newly created directories.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("mode")]
    public long? Mode { get; set; }

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Create parent directories as needed.</summary>
    [JsonPropertyName("recursive")]
    public bool? Recursive { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsReaddir operations.</summary>
public sealed class SessionFsReaddirResult
{
    /// <summary>Entry names in the directory.</summary>
    [JsonPropertyName("entries")]
    public IList<string> Entries { get => field ??= []; set; }

    /// <summary>Describes a filesystem error.</summary>
    [JsonPropertyName("error")]
    public SessionFsError? Error { get; set; }
}

/// <summary>RPC data type for SessionFsReaddir operations.</summary>
public sealed class SessionFsReaddirRequest
{
    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsReaddirWithTypesEntry operations.</summary>
public sealed class SessionFsReaddirWithTypesEntry
{
    /// <summary>Entry name.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Entry type.</summary>
    [JsonPropertyName("type")]
    public SessionFsReaddirWithTypesEntryType Type { get; set; }
}

/// <summary>RPC data type for SessionFsReaddirWithTypes operations.</summary>
public sealed class SessionFsReaddirWithTypesResult
{
    /// <summary>Directory entries with type information.</summary>
    [JsonPropertyName("entries")]
    public IList<SessionFsReaddirWithTypesEntry> Entries { get => field ??= []; set; }

    /// <summary>Describes a filesystem error.</summary>
    [JsonPropertyName("error")]
    public SessionFsError? Error { get; set; }
}

/// <summary>RPC data type for SessionFsReaddirWithTypes operations.</summary>
public sealed class SessionFsReaddirWithTypesRequest
{
    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsRm operations.</summary>
public sealed class SessionFsRmRequest
{
    /// <summary>Ignore errors if the path does not exist.</summary>
    [JsonPropertyName("force")]
    public bool? Force { get; set; }

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Remove directories and their contents recursively.</summary>
    [JsonPropertyName("recursive")]
    public bool? Recursive { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsRename operations.</summary>
public sealed class SessionFsRenameRequest
{
    /// <summary>Destination path using SessionFs conventions.</summary>
    [JsonPropertyName("dest")]
    public string Dest { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Source path using SessionFs conventions.</summary>
    [JsonPropertyName("src")]
    public string Src { get; set; } = string.Empty;
}

/// <summary>Configuration source.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<DiscoveredMcpServerSource>))]
public enum DiscoveredMcpServerSource
{
    /// <summary>The <c>user</c> variant.</summary>
    [JsonStringEnumMemberName("user")]
    User,
    /// <summary>The <c>workspace</c> variant.</summary>
    [JsonStringEnumMemberName("workspace")]
    Workspace,
    /// <summary>The <c>plugin</c> variant.</summary>
    [JsonStringEnumMemberName("plugin")]
    Plugin,
    /// <summary>The <c>builtin</c> variant.</summary>
    [JsonStringEnumMemberName("builtin")]
    Builtin,
}


/// <summary>Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio).</summary>
[JsonConverter(typeof(JsonStringEnumConverter<DiscoveredMcpServerType>))]
public enum DiscoveredMcpServerType
{
    /// <summary>The <c>stdio</c> variant.</summary>
    [JsonStringEnumMemberName("stdio")]
    Stdio,
    /// <summary>The <c>http</c> variant.</summary>
    [JsonStringEnumMemberName("http")]
    Http,
    /// <summary>The <c>sse</c> variant.</summary>
    [JsonStringEnumMemberName("sse")]
    Sse,
    /// <summary>The <c>memory</c> variant.</summary>
    [JsonStringEnumMemberName("memory")]
    Memory,
}


/// <summary>Path conventions used by this filesystem.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionFsSetProviderConventions>))]
public enum SessionFsSetProviderConventions
{
    /// <summary>The <c>windows</c> variant.</summary>
    [JsonStringEnumMemberName("windows")]
    Windows,
    /// <summary>The <c>posix</c> variant.</summary>
    [JsonStringEnumMemberName("posix")]
    Posix,
}


/// <summary>Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionLogLevel>))]
public enum SessionLogLevel
{
    /// <summary>The <c>info</c> variant.</summary>
    [JsonStringEnumMemberName("info")]
    Info,
    /// <summary>The <c>warning</c> variant.</summary>
    [JsonStringEnumMemberName("warning")]
    Warning,
    /// <summary>The <c>error</c> variant.</summary>
    [JsonStringEnumMemberName("error")]
    Error,
}


/// <summary>The agent mode. Valid values: "interactive", "plan", "autopilot".</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionMode>))]
public enum SessionMode
{
    /// <summary>The <c>interactive</c> variant.</summary>
    [JsonStringEnumMemberName("interactive")]
    Interactive,
    /// <summary>The <c>plan</c> variant.</summary>
    [JsonStringEnumMemberName("plan")]
    Plan,
    /// <summary>The <c>autopilot</c> variant.</summary>
    [JsonStringEnumMemberName("autopilot")]
    Autopilot,
}


/// <summary>Defines the allowed values.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<WorkspacesGetWorkspaceResultWorkspaceHostType>))]
public enum WorkspacesGetWorkspaceResultWorkspaceHostType
{
    /// <summary>The <c>github</c> variant.</summary>
    [JsonStringEnumMemberName("github")]
    Github,
    /// <summary>The <c>ado</c> variant.</summary>
    [JsonStringEnumMemberName("ado")]
    Ado,
}


/// <summary>Defines the allowed values.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel>))]
public enum WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel
{
    /// <summary>The <c>local</c> variant.</summary>
    [JsonStringEnumMemberName("local")]
    Local,
    /// <summary>The <c>user</c> variant.</summary>
    [JsonStringEnumMemberName("user")]
    User,
    /// <summary>The <c>repo_and_user</c> variant.</summary>
    [JsonStringEnumMemberName("repo_and_user")]
    RepoAndUser,
}


/// <summary>Where this source lives — used for UI grouping.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<InstructionsSourcesLocation>))]
public enum InstructionsSourcesLocation
{
    /// <summary>The <c>user</c> variant.</summary>
    [JsonStringEnumMemberName("user")]
    User,
    /// <summary>The <c>repository</c> variant.</summary>
    [JsonStringEnumMemberName("repository")]
    Repository,
    /// <summary>The <c>working-directory</c> variant.</summary>
    [JsonStringEnumMemberName("working-directory")]
    WorkingDirectory,
}


/// <summary>Category of instruction source — used for merge logic.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<InstructionsSourcesType>))]
public enum InstructionsSourcesType
{
    /// <summary>The <c>home</c> variant.</summary>
    [JsonStringEnumMemberName("home")]
    Home,
    /// <summary>The <c>repo</c> variant.</summary>
    [JsonStringEnumMemberName("repo")]
    Repo,
    /// <summary>The <c>model</c> variant.</summary>
    [JsonStringEnumMemberName("model")]
    Model,
    /// <summary>The <c>vscode</c> variant.</summary>
    [JsonStringEnumMemberName("vscode")]
    Vscode,
    /// <summary>The <c>nested-agents</c> variant.</summary>
    [JsonStringEnumMemberName("nested-agents")]
    NestedAgents,
    /// <summary>The <c>child-instructions</c> variant.</summary>
    [JsonStringEnumMemberName("child-instructions")]
    ChildInstructions,
}


/// <summary>Configuration source: user, workspace, plugin, or builtin.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<McpServerSource>))]
public enum McpServerSource
{
    /// <summary>The <c>user</c> variant.</summary>
    [JsonStringEnumMemberName("user")]
    User,
    /// <summary>The <c>workspace</c> variant.</summary>
    [JsonStringEnumMemberName("workspace")]
    Workspace,
    /// <summary>The <c>plugin</c> variant.</summary>
    [JsonStringEnumMemberName("plugin")]
    Plugin,
    /// <summary>The <c>builtin</c> variant.</summary>
    [JsonStringEnumMemberName("builtin")]
    Builtin,
}


/// <summary>Connection status: connected, failed, needs-auth, pending, disabled, or not_configured.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<McpServerStatus>))]
public enum McpServerStatus
{
    /// <summary>The <c>connected</c> variant.</summary>
    [JsonStringEnumMemberName("connected")]
    Connected,
    /// <summary>The <c>failed</c> variant.</summary>
    [JsonStringEnumMemberName("failed")]
    Failed,
    /// <summary>The <c>needs-auth</c> variant.</summary>
    [JsonStringEnumMemberName("needs-auth")]
    NeedsAuth,
    /// <summary>The <c>pending</c> variant.</summary>
    [JsonStringEnumMemberName("pending")]
    Pending,
    /// <summary>The <c>disabled</c> variant.</summary>
    [JsonStringEnumMemberName("disabled")]
    Disabled,
    /// <summary>The <c>not_configured</c> variant.</summary>
    [JsonStringEnumMemberName("not_configured")]
    NotConfigured,
}


/// <summary>Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/).</summary>
[JsonConverter(typeof(JsonStringEnumConverter<ExtensionSource>))]
public enum ExtensionSource
{
    /// <summary>The <c>project</c> variant.</summary>
    [JsonStringEnumMemberName("project")]
    Project,
    /// <summary>The <c>user</c> variant.</summary>
    [JsonStringEnumMemberName("user")]
    User,
}


/// <summary>Current status: running, disabled, failed, or starting.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<ExtensionStatus>))]
public enum ExtensionStatus
{
    /// <summary>The <c>running</c> variant.</summary>
    [JsonStringEnumMemberName("running")]
    Running,
    /// <summary>The <c>disabled</c> variant.</summary>
    [JsonStringEnumMemberName("disabled")]
    Disabled,
    /// <summary>The <c>failed</c> variant.</summary>
    [JsonStringEnumMemberName("failed")]
    Failed,
    /// <summary>The <c>starting</c> variant.</summary>
    [JsonStringEnumMemberName("starting")]
    Starting,
}


/// <summary>The user's response: accept (submitted), decline (rejected), or cancel (dismissed).</summary>
[JsonConverter(typeof(JsonStringEnumConverter<UIElicitationResponseAction>))]
public enum UIElicitationResponseAction
{
    /// <summary>The <c>accept</c> variant.</summary>
    [JsonStringEnumMemberName("accept")]
    Accept,
    /// <summary>The <c>decline</c> variant.</summary>
    [JsonStringEnumMemberName("decline")]
    Decline,
    /// <summary>The <c>cancel</c> variant.</summary>
    [JsonStringEnumMemberName("cancel")]
    Cancel,
}


/// <summary>Signal to send (default: SIGTERM).</summary>
[JsonConverter(typeof(JsonStringEnumConverter<ShellKillSignal>))]
public enum ShellKillSignal
{
    /// <summary>The <c>SIGTERM</c> variant.</summary>
    [JsonStringEnumMemberName("SIGTERM")]
    SIGTERM,
    /// <summary>The <c>SIGKILL</c> variant.</summary>
    [JsonStringEnumMemberName("SIGKILL")]
    SIGKILL,
    /// <summary>The <c>SIGINT</c> variant.</summary>
    [JsonStringEnumMemberName("SIGINT")]
    SIGINT,
}


/// <summary>Error classification.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionFsErrorCode>))]
public enum SessionFsErrorCode
{
    /// <summary>The <c>ENOENT</c> variant.</summary>
    [JsonStringEnumMemberName("ENOENT")]
    ENOENT,
    /// <summary>The <c>UNKNOWN</c> variant.</summary>
    [JsonStringEnumMemberName("UNKNOWN")]
    UNKNOWN,
}


/// <summary>Entry type.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionFsReaddirWithTypesEntryType>))]
public enum SessionFsReaddirWithTypesEntryType
{
    /// <summary>The <c>file</c> variant.</summary>
    [JsonStringEnumMemberName("file")]
    File,
    /// <summary>The <c>directory</c> variant.</summary>
    [JsonStringEnumMemberName("directory")]
    Directory,
}


/// <summary>Provides server-scoped RPC methods (no session required).</summary>
public sealed class ServerRpc
{
    private readonly JsonRpc _rpc;

    internal ServerRpc(JsonRpc rpc)
    {
        _rpc = rpc;
        Models = new ServerModelsApi(rpc);
        Tools = new ServerToolsApi(rpc);
        Account = new ServerAccountApi(rpc);
        Mcp = new ServerMcpApi(rpc);
        Skills = new ServerSkillsApi(rpc);
        SessionFs = new ServerSessionFsApi(rpc);
        Sessions = new ServerSessionsApi(rpc);
    }

    /// <summary>Calls "ping".</summary>
    public async Task<PingResult> PingAsync(string? message = null, CancellationToken cancellationToken = default)
    {
        var request = new PingRequest { Message = message };
        return await CopilotClient.InvokeRpcAsync<PingResult>(_rpc, "ping", [request], cancellationToken);
    }

    /// <summary>Models APIs.</summary>
    public ServerModelsApi Models { get; }

    /// <summary>Tools APIs.</summary>
    public ServerToolsApi Tools { get; }

    /// <summary>Account APIs.</summary>
    public ServerAccountApi Account { get; }

    /// <summary>Mcp APIs.</summary>
    public ServerMcpApi Mcp { get; }

    /// <summary>Skills APIs.</summary>
    public ServerSkillsApi Skills { get; }

    /// <summary>SessionFs APIs.</summary>
    public ServerSessionFsApi SessionFs { get; }

    /// <summary>Sessions APIs.</summary>
    public ServerSessionsApi Sessions { get; }
}

/// <summary>Provides server-scoped Models APIs.</summary>
public sealed class ServerModelsApi
{
    private readonly JsonRpc _rpc;

    internal ServerModelsApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Calls "models.list".</summary>
    public async Task<ModelList> ListAsync(CancellationToken cancellationToken = default)
    {
        return await CopilotClient.InvokeRpcAsync<ModelList>(_rpc, "models.list", [], cancellationToken);
    }
}

/// <summary>Provides server-scoped Tools APIs.</summary>
public sealed class ServerToolsApi
{
    private readonly JsonRpc _rpc;

    internal ServerToolsApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Calls "tools.list".</summary>
    public async Task<ToolList> ListAsync(string? model = null, CancellationToken cancellationToken = default)
    {
        var request = new ToolsListRequest { Model = model };
        return await CopilotClient.InvokeRpcAsync<ToolList>(_rpc, "tools.list", [request], cancellationToken);
    }
}

/// <summary>Provides server-scoped Account APIs.</summary>
public sealed class ServerAccountApi
{
    private readonly JsonRpc _rpc;

    internal ServerAccountApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Calls "account.getQuota".</summary>
    public async Task<AccountGetQuotaResult> GetQuotaAsync(CancellationToken cancellationToken = default)
    {
        return await CopilotClient.InvokeRpcAsync<AccountGetQuotaResult>(_rpc, "account.getQuota", [], cancellationToken);
    }
}

/// <summary>Provides server-scoped Mcp APIs.</summary>
public sealed class ServerMcpApi
{
    private readonly JsonRpc _rpc;

    internal ServerMcpApi(JsonRpc rpc)
    {
        _rpc = rpc;
        Config = new ServerMcpConfigApi(rpc);
    }

    /// <summary>Calls "mcp.discover".</summary>
    public async Task<McpDiscoverResult> DiscoverAsync(string? workingDirectory = null, CancellationToken cancellationToken = default)
    {
        var request = new McpDiscoverRequest { WorkingDirectory = workingDirectory };
        return await CopilotClient.InvokeRpcAsync<McpDiscoverResult>(_rpc, "mcp.discover", [request], cancellationToken);
    }

    /// <summary>Config APIs.</summary>
    public ServerMcpConfigApi Config { get; }
}

/// <summary>Provides server-scoped McpConfig APIs.</summary>
public sealed class ServerMcpConfigApi
{
    private readonly JsonRpc _rpc;

    internal ServerMcpConfigApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Calls "mcp.config.list".</summary>
    public async Task<McpConfigList> ListAsync(CancellationToken cancellationToken = default)
    {
        return await CopilotClient.InvokeRpcAsync<McpConfigList>(_rpc, "mcp.config.list", [], cancellationToken);
    }

    /// <summary>Calls "mcp.config.add".</summary>
    public async Task AddAsync(string name, object config, CancellationToken cancellationToken = default)
    {
        var request = new McpConfigAddRequest { Name = name, Config = config };
        await CopilotClient.InvokeRpcAsync(_rpc, "mcp.config.add", [request], cancellationToken);
    }

    /// <summary>Calls "mcp.config.update".</summary>
    public async Task UpdateAsync(string name, object config, CancellationToken cancellationToken = default)
    {
        var request = new McpConfigUpdateRequest { Name = name, Config = config };
        await CopilotClient.InvokeRpcAsync(_rpc, "mcp.config.update", [request], cancellationToken);
    }

    /// <summary>Calls "mcp.config.remove".</summary>
    public async Task RemoveAsync(string name, CancellationToken cancellationToken = default)
    {
        var request = new McpConfigRemoveRequest { Name = name };
        await CopilotClient.InvokeRpcAsync(_rpc, "mcp.config.remove", [request], cancellationToken);
    }
}

/// <summary>Provides server-scoped Skills APIs.</summary>
public sealed class ServerSkillsApi
{
    private readonly JsonRpc _rpc;

    internal ServerSkillsApi(JsonRpc rpc)
    {
        _rpc = rpc;
        Config = new ServerSkillsConfigApi(rpc);
    }

    /// <summary>Calls "skills.discover".</summary>
    public async Task<ServerSkillList> DiscoverAsync(IList<string>? projectPaths = null, IList<string>? skillDirectories = null, CancellationToken cancellationToken = default)
    {
        var request = new SkillsDiscoverRequest { ProjectPaths = projectPaths, SkillDirectories = skillDirectories };
        return await CopilotClient.InvokeRpcAsync<ServerSkillList>(_rpc, "skills.discover", [request], cancellationToken);
    }

    /// <summary>Config APIs.</summary>
    public ServerSkillsConfigApi Config { get; }
}

/// <summary>Provides server-scoped SkillsConfig APIs.</summary>
public sealed class ServerSkillsConfigApi
{
    private readonly JsonRpc _rpc;

    internal ServerSkillsConfigApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Calls "skills.config.setDisabledSkills".</summary>
    public async Task SetDisabledSkillsAsync(IList<string> disabledSkills, CancellationToken cancellationToken = default)
    {
        var request = new SkillsConfigSetDisabledSkillsRequest { DisabledSkills = disabledSkills };
        await CopilotClient.InvokeRpcAsync(_rpc, "skills.config.setDisabledSkills", [request], cancellationToken);
    }
}

/// <summary>Provides server-scoped SessionFs APIs.</summary>
public sealed class ServerSessionFsApi
{
    private readonly JsonRpc _rpc;

    internal ServerSessionFsApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Calls "sessionFs.setProvider".</summary>
    public async Task<SessionFsSetProviderResult> SetProviderAsync(string initialCwd, string sessionStatePath, SessionFsSetProviderConventions conventions, CancellationToken cancellationToken = default)
    {
        var request = new SessionFsSetProviderRequest { InitialCwd = initialCwd, SessionStatePath = sessionStatePath, Conventions = conventions };
        return await CopilotClient.InvokeRpcAsync<SessionFsSetProviderResult>(_rpc, "sessionFs.setProvider", [request], cancellationToken);
    }
}

/// <summary>Provides server-scoped Sessions APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ServerSessionsApi
{
    private readonly JsonRpc _rpc;

    internal ServerSessionsApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Calls "sessions.fork".</summary>
    public async Task<SessionsForkResult> ForkAsync(string sessionId, string? toEventId = null, CancellationToken cancellationToken = default)
    {
        var request = new SessionsForkRequest { SessionId = sessionId, ToEventId = toEventId };
        return await CopilotClient.InvokeRpcAsync<SessionsForkResult>(_rpc, "sessions.fork", [request], cancellationToken);
    }
}

/// <summary>Provides typed session-scoped RPC methods.</summary>
public sealed class SessionRpc
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal SessionRpc(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
        Model = new ModelApi(rpc, sessionId);
        Mode = new ModeApi(rpc, sessionId);
        Name = new NameApi(rpc, sessionId);
        Plan = new PlanApi(rpc, sessionId);
        Workspaces = new WorkspacesApi(rpc, sessionId);
        Instructions = new InstructionsApi(rpc, sessionId);
        Fleet = new FleetApi(rpc, sessionId);
        Agent = new AgentApi(rpc, sessionId);
        Skills = new SkillsApi(rpc, sessionId);
        Mcp = new McpApi(rpc, sessionId);
        Plugins = new PluginsApi(rpc, sessionId);
        Extensions = new ExtensionsApi(rpc, sessionId);
        Tools = new ToolsApi(rpc, sessionId);
        Commands = new CommandsApi(rpc, sessionId);
        Ui = new UiApi(rpc, sessionId);
        Permissions = new PermissionsApi(rpc, sessionId);
        Shell = new ShellApi(rpc, sessionId);
        History = new HistoryApi(rpc, sessionId);
        Usage = new UsageApi(rpc, sessionId);
    }

    /// <summary>Model APIs.</summary>
    public ModelApi Model { get; }

    /// <summary>Mode APIs.</summary>
    public ModeApi Mode { get; }

    /// <summary>Name APIs.</summary>
    public NameApi Name { get; }

    /// <summary>Plan APIs.</summary>
    public PlanApi Plan { get; }

    /// <summary>Workspaces APIs.</summary>
    public WorkspacesApi Workspaces { get; }

    /// <summary>Instructions APIs.</summary>
    public InstructionsApi Instructions { get; }

    /// <summary>Fleet APIs.</summary>
    public FleetApi Fleet { get; }

    /// <summary>Agent APIs.</summary>
    public AgentApi Agent { get; }

    /// <summary>Skills APIs.</summary>
    public SkillsApi Skills { get; }

    /// <summary>Mcp APIs.</summary>
    public McpApi Mcp { get; }

    /// <summary>Plugins APIs.</summary>
    public PluginsApi Plugins { get; }

    /// <summary>Extensions APIs.</summary>
    public ExtensionsApi Extensions { get; }

    /// <summary>Tools APIs.</summary>
    public ToolsApi Tools { get; }

    /// <summary>Commands APIs.</summary>
    public CommandsApi Commands { get; }

    /// <summary>Ui APIs.</summary>
    public UiApi Ui { get; }

    /// <summary>Permissions APIs.</summary>
    public PermissionsApi Permissions { get; }

    /// <summary>Shell APIs.</summary>
    public ShellApi Shell { get; }

    /// <summary>History APIs.</summary>
    public HistoryApi History { get; }

    /// <summary>Usage APIs.</summary>
    public UsageApi Usage { get; }

    /// <summary>Calls "session.log".</summary>
    public async Task<LogResult> LogAsync(string message, SessionLogLevel? level = null, bool? ephemeral = null, string? url = null, CancellationToken cancellationToken = default)
    {
        var request = new LogRequest { SessionId = _sessionId, Message = message, Level = level, Ephemeral = ephemeral, Url = url };
        return await CopilotClient.InvokeRpcAsync<LogResult>(_rpc, "session.log", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Model APIs.</summary>
public sealed class ModelApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal ModelApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.model.getCurrent".</summary>
    public async Task<CurrentModel> GetCurrentAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionModelGetCurrentRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<CurrentModel>(_rpc, "session.model.getCurrent", [request], cancellationToken);
    }

    /// <summary>Calls "session.model.switchTo".</summary>
    public async Task<ModelSwitchToResult> SwitchToAsync(string modelId, string? reasoningEffort = null, ModelCapabilitiesOverride? modelCapabilities = null, CancellationToken cancellationToken = default)
    {
        var request = new ModelSwitchToRequest { SessionId = _sessionId, ModelId = modelId, ReasoningEffort = reasoningEffort, ModelCapabilities = modelCapabilities };
        return await CopilotClient.InvokeRpcAsync<ModelSwitchToResult>(_rpc, "session.model.switchTo", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Mode APIs.</summary>
public sealed class ModeApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal ModeApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.mode.get".</summary>
    public async Task<SessionMode> GetAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionModeGetRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionMode>(_rpc, "session.mode.get", [request], cancellationToken);
    }

    /// <summary>Calls "session.mode.set".</summary>
    public async Task SetAsync(SessionMode mode, CancellationToken cancellationToken = default)
    {
        var request = new ModeSetRequest { SessionId = _sessionId, Mode = mode };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.mode.set", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Name APIs.</summary>
public sealed class NameApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal NameApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.name.get".</summary>
    public async Task<NameGetResult> GetAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionNameGetRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<NameGetResult>(_rpc, "session.name.get", [request], cancellationToken);
    }

    /// <summary>Calls "session.name.set".</summary>
    public async Task SetAsync(string name, CancellationToken cancellationToken = default)
    {
        var request = new NameSetRequest { SessionId = _sessionId, Name = name };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.name.set", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Plan APIs.</summary>
public sealed class PlanApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal PlanApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.plan.read".</summary>
    public async Task<PlanReadResult> ReadAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionPlanReadRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<PlanReadResult>(_rpc, "session.plan.read", [request], cancellationToken);
    }

    /// <summary>Calls "session.plan.update".</summary>
    public async Task UpdateAsync(string content, CancellationToken cancellationToken = default)
    {
        var request = new PlanUpdateRequest { SessionId = _sessionId, Content = content };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.plan.update", [request], cancellationToken);
    }

    /// <summary>Calls "session.plan.delete".</summary>
    public async Task DeleteAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionPlanDeleteRequest { SessionId = _sessionId };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.plan.delete", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Workspaces APIs.</summary>
public sealed class WorkspacesApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal WorkspacesApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.workspaces.getWorkspace".</summary>
    public async Task<WorkspacesGetWorkspaceResult> GetWorkspaceAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionWorkspacesGetWorkspaceRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<WorkspacesGetWorkspaceResult>(_rpc, "session.workspaces.getWorkspace", [request], cancellationToken);
    }

    /// <summary>Calls "session.workspaces.listFiles".</summary>
    public async Task<WorkspacesListFilesResult> ListFilesAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionWorkspacesListFilesRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<WorkspacesListFilesResult>(_rpc, "session.workspaces.listFiles", [request], cancellationToken);
    }

    /// <summary>Calls "session.workspaces.readFile".</summary>
    public async Task<WorkspacesReadFileResult> ReadFileAsync(string path, CancellationToken cancellationToken = default)
    {
        var request = new WorkspacesReadFileRequest { SessionId = _sessionId, Path = path };
        return await CopilotClient.InvokeRpcAsync<WorkspacesReadFileResult>(_rpc, "session.workspaces.readFile", [request], cancellationToken);
    }

    /// <summary>Calls "session.workspaces.createFile".</summary>
    public async Task CreateFileAsync(string path, string content, CancellationToken cancellationToken = default)
    {
        var request = new WorkspacesCreateFileRequest { SessionId = _sessionId, Path = path, Content = content };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.workspaces.createFile", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Instructions APIs.</summary>
public sealed class InstructionsApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal InstructionsApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.instructions.getSources".</summary>
    public async Task<InstructionsGetSourcesResult> GetSourcesAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionInstructionsGetSourcesRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<InstructionsGetSourcesResult>(_rpc, "session.instructions.getSources", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Fleet APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class FleetApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal FleetApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.fleet.start".</summary>
    public async Task<FleetStartResult> StartAsync(string? prompt = null, CancellationToken cancellationToken = default)
    {
        var request = new FleetStartRequest { SessionId = _sessionId, Prompt = prompt };
        return await CopilotClient.InvokeRpcAsync<FleetStartResult>(_rpc, "session.fleet.start", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Agent APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AgentApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal AgentApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.agent.list".</summary>
    public async Task<AgentList> ListAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionAgentListRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<AgentList>(_rpc, "session.agent.list", [request], cancellationToken);
    }

    /// <summary>Calls "session.agent.getCurrent".</summary>
    public async Task<AgentGetCurrentResult> GetCurrentAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionAgentGetCurrentRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<AgentGetCurrentResult>(_rpc, "session.agent.getCurrent", [request], cancellationToken);
    }

    /// <summary>Calls "session.agent.select".</summary>
    public async Task<AgentSelectResult> SelectAsync(string name, CancellationToken cancellationToken = default)
    {
        var request = new AgentSelectRequest { SessionId = _sessionId, Name = name };
        return await CopilotClient.InvokeRpcAsync<AgentSelectResult>(_rpc, "session.agent.select", [request], cancellationToken);
    }

    /// <summary>Calls "session.agent.deselect".</summary>
    public async Task DeselectAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionAgentDeselectRequest { SessionId = _sessionId };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.agent.deselect", [request], cancellationToken);
    }

    /// <summary>Calls "session.agent.reload".</summary>
    public async Task<AgentReloadResult> ReloadAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionAgentReloadRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<AgentReloadResult>(_rpc, "session.agent.reload", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Skills APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SkillsApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal SkillsApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.skills.list".</summary>
    public async Task<SkillList> ListAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionSkillsListRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SkillList>(_rpc, "session.skills.list", [request], cancellationToken);
    }

    /// <summary>Calls "session.skills.enable".</summary>
    public async Task EnableAsync(string name, CancellationToken cancellationToken = default)
    {
        var request = new SkillsEnableRequest { SessionId = _sessionId, Name = name };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.skills.enable", [request], cancellationToken);
    }

    /// <summary>Calls "session.skills.disable".</summary>
    public async Task DisableAsync(string name, CancellationToken cancellationToken = default)
    {
        var request = new SkillsDisableRequest { SessionId = _sessionId, Name = name };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.skills.disable", [request], cancellationToken);
    }

    /// <summary>Calls "session.skills.reload".</summary>
    public async Task ReloadAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionSkillsReloadRequest { SessionId = _sessionId };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.skills.reload", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Mcp APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal McpApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.mcp.list".</summary>
    public async Task<McpServerList> ListAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionMcpListRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<McpServerList>(_rpc, "session.mcp.list", [request], cancellationToken);
    }

    /// <summary>Calls "session.mcp.enable".</summary>
    public async Task EnableAsync(string serverName, CancellationToken cancellationToken = default)
    {
        var request = new McpEnableRequest { SessionId = _sessionId, ServerName = serverName };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.mcp.enable", [request], cancellationToken);
    }

    /// <summary>Calls "session.mcp.disable".</summary>
    public async Task DisableAsync(string serverName, CancellationToken cancellationToken = default)
    {
        var request = new McpDisableRequest { SessionId = _sessionId, ServerName = serverName };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.mcp.disable", [request], cancellationToken);
    }

    /// <summary>Calls "session.mcp.reload".</summary>
    public async Task ReloadAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionMcpReloadRequest { SessionId = _sessionId };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.mcp.reload", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Plugins APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PluginsApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal PluginsApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.plugins.list".</summary>
    public async Task<PluginList> ListAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionPluginsListRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<PluginList>(_rpc, "session.plugins.list", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Extensions APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ExtensionsApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal ExtensionsApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.extensions.list".</summary>
    public async Task<ExtensionList> ListAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionExtensionsListRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<ExtensionList>(_rpc, "session.extensions.list", [request], cancellationToken);
    }

    /// <summary>Calls "session.extensions.enable".</summary>
    public async Task EnableAsync(string id, CancellationToken cancellationToken = default)
    {
        var request = new ExtensionsEnableRequest { SessionId = _sessionId, Id = id };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.extensions.enable", [request], cancellationToken);
    }

    /// <summary>Calls "session.extensions.disable".</summary>
    public async Task DisableAsync(string id, CancellationToken cancellationToken = default)
    {
        var request = new ExtensionsDisableRequest { SessionId = _sessionId, Id = id };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.extensions.disable", [request], cancellationToken);
    }

    /// <summary>Calls "session.extensions.reload".</summary>
    public async Task ReloadAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionExtensionsReloadRequest { SessionId = _sessionId };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.extensions.reload", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Tools APIs.</summary>
public sealed class ToolsApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal ToolsApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.tools.handlePendingToolCall".</summary>
    public async Task<HandleToolCallResult> HandlePendingToolCallAsync(string requestId, object? result = null, string? error = null, CancellationToken cancellationToken = default)
    {
        var request = new ToolsHandlePendingToolCallRequest { SessionId = _sessionId, RequestId = requestId, Result = result, Error = error };
        return await CopilotClient.InvokeRpcAsync<HandleToolCallResult>(_rpc, "session.tools.handlePendingToolCall", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Commands APIs.</summary>
public sealed class CommandsApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal CommandsApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.commands.handlePendingCommand".</summary>
    public async Task<CommandsHandlePendingCommandResult> HandlePendingCommandAsync(string requestId, string? error = null, CancellationToken cancellationToken = default)
    {
        var request = new CommandsHandlePendingCommandRequest { SessionId = _sessionId, RequestId = requestId, Error = error };
        return await CopilotClient.InvokeRpcAsync<CommandsHandlePendingCommandResult>(_rpc, "session.commands.handlePendingCommand", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Ui APIs.</summary>
public sealed class UiApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal UiApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.ui.elicitation".</summary>
    public async Task<UIElicitationResponse> ElicitationAsync(string message, UIElicitationSchema requestedSchema, CancellationToken cancellationToken = default)
    {
        var request = new UIElicitationRequest { SessionId = _sessionId, Message = message, RequestedSchema = requestedSchema };
        return await CopilotClient.InvokeRpcAsync<UIElicitationResponse>(_rpc, "session.ui.elicitation", [request], cancellationToken);
    }

    /// <summary>Calls "session.ui.handlePendingElicitation".</summary>
    public async Task<UIElicitationResult> HandlePendingElicitationAsync(string requestId, UIElicitationResponse result, CancellationToken cancellationToken = default)
    {
        var request = new UIHandlePendingElicitationRequest { SessionId = _sessionId, RequestId = requestId, Result = result };
        return await CopilotClient.InvokeRpcAsync<UIElicitationResult>(_rpc, "session.ui.handlePendingElicitation", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Permissions APIs.</summary>
public sealed class PermissionsApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal PermissionsApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.permissions.handlePendingPermissionRequest".</summary>
    public async Task<PermissionRequestResult> HandlePendingPermissionRequestAsync(string requestId, PermissionDecision result, CancellationToken cancellationToken = default)
    {
        var request = new PermissionDecisionRequest { SessionId = _sessionId, RequestId = requestId, Result = result };
        return await CopilotClient.InvokeRpcAsync<PermissionRequestResult>(_rpc, "session.permissions.handlePendingPermissionRequest", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Shell APIs.</summary>
public sealed class ShellApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal ShellApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.shell.exec".</summary>
    public async Task<ShellExecResult> ExecAsync(string command, string? cwd = null, TimeSpan? timeout = null, CancellationToken cancellationToken = default)
    {
        var request = new ShellExecRequest { SessionId = _sessionId, Command = command, Cwd = cwd, Timeout = timeout };
        return await CopilotClient.InvokeRpcAsync<ShellExecResult>(_rpc, "session.shell.exec", [request], cancellationToken);
    }

    /// <summary>Calls "session.shell.kill".</summary>
    public async Task<ShellKillResult> KillAsync(string processId, ShellKillSignal? signal = null, CancellationToken cancellationToken = default)
    {
        var request = new ShellKillRequest { SessionId = _sessionId, ProcessId = processId, Signal = signal };
        return await CopilotClient.InvokeRpcAsync<ShellKillResult>(_rpc, "session.shell.kill", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped History APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class HistoryApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal HistoryApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.history.compact".</summary>
    public async Task<HistoryCompactResult> CompactAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionHistoryCompactRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<HistoryCompactResult>(_rpc, "session.history.compact", [request], cancellationToken);
    }

    /// <summary>Calls "session.history.truncate".</summary>
    public async Task<HistoryTruncateResult> TruncateAsync(string eventId, CancellationToken cancellationToken = default)
    {
        var request = new HistoryTruncateRequest { SessionId = _sessionId, EventId = eventId };
        return await CopilotClient.InvokeRpcAsync<HistoryTruncateResult>(_rpc, "session.history.truncate", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Usage APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UsageApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal UsageApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.usage.getMetrics".</summary>
    public async Task<UsageGetMetricsResult> GetMetricsAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionUsageGetMetricsRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<UsageGetMetricsResult>(_rpc, "session.usage.getMetrics", [request], cancellationToken);
    }
}

/// <summary>Handles `sessionFs` client session API methods.</summary>
public interface ISessionFsHandler
{
    /// <summary>Handles "sessionFs.readFile".</summary>
    Task<SessionFsReadFileResult> ReadFileAsync(SessionFsReadFileRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.writeFile".</summary>
    Task<SessionFsError?> WriteFileAsync(SessionFsWriteFileRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.appendFile".</summary>
    Task<SessionFsError?> AppendFileAsync(SessionFsAppendFileRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.exists".</summary>
    Task<SessionFsExistsResult> ExistsAsync(SessionFsExistsRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.stat".</summary>
    Task<SessionFsStatResult> StatAsync(SessionFsStatRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.mkdir".</summary>
    Task<SessionFsError?> MkdirAsync(SessionFsMkdirRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.readdir".</summary>
    Task<SessionFsReaddirResult> ReaddirAsync(SessionFsReaddirRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.readdirWithTypes".</summary>
    Task<SessionFsReaddirWithTypesResult> ReaddirWithTypesAsync(SessionFsReaddirWithTypesRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.rm".</summary>
    Task<SessionFsError?> RmAsync(SessionFsRmRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.rename".</summary>
    Task<SessionFsError?> RenameAsync(SessionFsRenameRequest request, CancellationToken cancellationToken = default);
}

/// <summary>Provides all client session API handler groups for a session.</summary>
public sealed class ClientSessionApiHandlers
{
    /// <summary>Optional handler for SessionFs client session API methods.</summary>
    public ISessionFsHandler? SessionFs { get; set; }
}

/// <summary>Registers client session API handlers on a JSON-RPC connection.</summary>
public static class ClientSessionApiRegistration
{
    /// <summary>
    /// Registers handlers for server-to-client session API calls.
    /// Each incoming call includes a <c>sessionId</c> in its params object,
    /// which is used to resolve the session's handler group.
    /// </summary>
    public static void RegisterClientSessionApiHandlers(JsonRpc rpc, Func<string, ClientSessionApiHandlers> getHandlers)
    {
        var registerSessionFsReadFileMethod = (Func<SessionFsReadFileRequest, CancellationToken, Task<SessionFsReadFileResult>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.ReadFileAsync(request, cancellationToken);
        });
        rpc.AddLocalRpcMethod(registerSessionFsReadFileMethod.Method, registerSessionFsReadFileMethod.Target!, new JsonRpcMethodAttribute("sessionFs.readFile")
        {
            UseSingleObjectParameterDeserialization = true
        });
        var registerSessionFsWriteFileMethod = (Func<SessionFsWriteFileRequest, CancellationToken, Task<SessionFsError?>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.WriteFileAsync(request, cancellationToken);
        });
        rpc.AddLocalRpcMethod(registerSessionFsWriteFileMethod.Method, registerSessionFsWriteFileMethod.Target!, new JsonRpcMethodAttribute("sessionFs.writeFile")
        {
            UseSingleObjectParameterDeserialization = true
        });
        var registerSessionFsAppendFileMethod = (Func<SessionFsAppendFileRequest, CancellationToken, Task<SessionFsError?>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.AppendFileAsync(request, cancellationToken);
        });
        rpc.AddLocalRpcMethod(registerSessionFsAppendFileMethod.Method, registerSessionFsAppendFileMethod.Target!, new JsonRpcMethodAttribute("sessionFs.appendFile")
        {
            UseSingleObjectParameterDeserialization = true
        });
        var registerSessionFsExistsMethod = (Func<SessionFsExistsRequest, CancellationToken, Task<SessionFsExistsResult>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.ExistsAsync(request, cancellationToken);
        });
        rpc.AddLocalRpcMethod(registerSessionFsExistsMethod.Method, registerSessionFsExistsMethod.Target!, new JsonRpcMethodAttribute("sessionFs.exists")
        {
            UseSingleObjectParameterDeserialization = true
        });
        var registerSessionFsStatMethod = (Func<SessionFsStatRequest, CancellationToken, Task<SessionFsStatResult>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.StatAsync(request, cancellationToken);
        });
        rpc.AddLocalRpcMethod(registerSessionFsStatMethod.Method, registerSessionFsStatMethod.Target!, new JsonRpcMethodAttribute("sessionFs.stat")
        {
            UseSingleObjectParameterDeserialization = true
        });
        var registerSessionFsMkdirMethod = (Func<SessionFsMkdirRequest, CancellationToken, Task<SessionFsError?>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.MkdirAsync(request, cancellationToken);
        });
        rpc.AddLocalRpcMethod(registerSessionFsMkdirMethod.Method, registerSessionFsMkdirMethod.Target!, new JsonRpcMethodAttribute("sessionFs.mkdir")
        {
            UseSingleObjectParameterDeserialization = true
        });
        var registerSessionFsReaddirMethod = (Func<SessionFsReaddirRequest, CancellationToken, Task<SessionFsReaddirResult>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.ReaddirAsync(request, cancellationToken);
        });
        rpc.AddLocalRpcMethod(registerSessionFsReaddirMethod.Method, registerSessionFsReaddirMethod.Target!, new JsonRpcMethodAttribute("sessionFs.readdir")
        {
            UseSingleObjectParameterDeserialization = true
        });
        var registerSessionFsReaddirWithTypesMethod = (Func<SessionFsReaddirWithTypesRequest, CancellationToken, Task<SessionFsReaddirWithTypesResult>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.ReaddirWithTypesAsync(request, cancellationToken);
        });
        rpc.AddLocalRpcMethod(registerSessionFsReaddirWithTypesMethod.Method, registerSessionFsReaddirWithTypesMethod.Target!, new JsonRpcMethodAttribute("sessionFs.readdirWithTypes")
        {
            UseSingleObjectParameterDeserialization = true
        });
        var registerSessionFsRmMethod = (Func<SessionFsRmRequest, CancellationToken, Task<SessionFsError?>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.RmAsync(request, cancellationToken);
        });
        rpc.AddLocalRpcMethod(registerSessionFsRmMethod.Method, registerSessionFsRmMethod.Target!, new JsonRpcMethodAttribute("sessionFs.rm")
        {
            UseSingleObjectParameterDeserialization = true
        });
        var registerSessionFsRenameMethod = (Func<SessionFsRenameRequest, CancellationToken, Task<SessionFsError?>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.RenameAsync(request, cancellationToken);
        });
        rpc.AddLocalRpcMethod(registerSessionFsRenameMethod.Method, registerSessionFsRenameMethod.Target!, new JsonRpcMethodAttribute("sessionFs.rename")
        {
            UseSingleObjectParameterDeserialization = true
        });
    }
}

[JsonSourceGenerationOptions(
    JsonSerializerDefaults.Web,
    AllowOutOfOrderMetadataProperties = true,
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
[JsonSerializable(typeof(AccountGetQuotaResult))]
[JsonSerializable(typeof(AccountQuotaSnapshot))]
[JsonSerializable(typeof(AgentGetCurrentResult))]
[JsonSerializable(typeof(AgentInfo))]
[JsonSerializable(typeof(AgentList))]
[JsonSerializable(typeof(AgentReloadResult))]
[JsonSerializable(typeof(AgentSelectRequest))]
[JsonSerializable(typeof(AgentSelectResult))]
[JsonSerializable(typeof(CommandsHandlePendingCommandRequest))]
[JsonSerializable(typeof(CommandsHandlePendingCommandResult))]
[JsonSerializable(typeof(CurrentModel))]
[JsonSerializable(typeof(DiscoveredMcpServer))]
[JsonSerializable(typeof(Extension))]
[JsonSerializable(typeof(ExtensionList))]
[JsonSerializable(typeof(ExtensionsDisableRequest))]
[JsonSerializable(typeof(ExtensionsEnableRequest))]
[JsonSerializable(typeof(FleetStartRequest))]
[JsonSerializable(typeof(FleetStartResult))]
[JsonSerializable(typeof(HandleToolCallResult))]
[JsonSerializable(typeof(HistoryCompactContextWindow))]
[JsonSerializable(typeof(HistoryCompactResult))]
[JsonSerializable(typeof(HistoryTruncateRequest))]
[JsonSerializable(typeof(HistoryTruncateResult))]
[JsonSerializable(typeof(InstructionsGetSourcesResult))]
[JsonSerializable(typeof(InstructionsSources))]
[JsonSerializable(typeof(LogRequest))]
[JsonSerializable(typeof(LogResult))]
[JsonSerializable(typeof(McpConfigAddRequest))]
[JsonSerializable(typeof(McpConfigList))]
[JsonSerializable(typeof(McpConfigRemoveRequest))]
[JsonSerializable(typeof(McpConfigUpdateRequest))]
[JsonSerializable(typeof(McpDisableRequest))]
[JsonSerializable(typeof(McpDiscoverRequest))]
[JsonSerializable(typeof(McpDiscoverResult))]
[JsonSerializable(typeof(McpEnableRequest))]
[JsonSerializable(typeof(McpServer))]
[JsonSerializable(typeof(McpServerList))]
[JsonSerializable(typeof(ModeSetRequest))]
[JsonSerializable(typeof(Model))]
[JsonSerializable(typeof(ModelBilling))]
[JsonSerializable(typeof(ModelCapabilities))]
[JsonSerializable(typeof(ModelCapabilitiesLimits))]
[JsonSerializable(typeof(ModelCapabilitiesLimitsVision))]
[JsonSerializable(typeof(ModelCapabilitiesOverride))]
[JsonSerializable(typeof(ModelCapabilitiesOverrideLimits))]
[JsonSerializable(typeof(ModelCapabilitiesOverrideLimitsVision))]
[JsonSerializable(typeof(ModelCapabilitiesOverrideSupports))]
[JsonSerializable(typeof(ModelCapabilitiesSupports))]
[JsonSerializable(typeof(ModelList))]
[JsonSerializable(typeof(ModelPolicy))]
[JsonSerializable(typeof(ModelSwitchToRequest))]
[JsonSerializable(typeof(ModelSwitchToResult))]
[JsonSerializable(typeof(NameGetResult))]
[JsonSerializable(typeof(NameSetRequest))]
[JsonSerializable(typeof(PermissionDecision))]
[JsonSerializable(typeof(PermissionDecisionRequest))]
[JsonSerializable(typeof(PermissionRequestResult))]
[JsonSerializable(typeof(PingRequest))]
[JsonSerializable(typeof(PingResult))]
[JsonSerializable(typeof(PlanReadResult))]
[JsonSerializable(typeof(PlanUpdateRequest))]
[JsonSerializable(typeof(Plugin))]
[JsonSerializable(typeof(PluginList))]
[JsonSerializable(typeof(ServerSkill))]
[JsonSerializable(typeof(ServerSkillList))]
[JsonSerializable(typeof(SessionAgentDeselectRequest))]
[JsonSerializable(typeof(SessionAgentGetCurrentRequest))]
[JsonSerializable(typeof(SessionAgentListRequest))]
[JsonSerializable(typeof(SessionAgentReloadRequest))]
[JsonSerializable(typeof(SessionExtensionsListRequest))]
[JsonSerializable(typeof(SessionExtensionsReloadRequest))]
[JsonSerializable(typeof(SessionFsAppendFileRequest))]
[JsonSerializable(typeof(SessionFsError))]
[JsonSerializable(typeof(SessionFsExistsRequest))]
[JsonSerializable(typeof(SessionFsExistsResult))]
[JsonSerializable(typeof(SessionFsMkdirRequest))]
[JsonSerializable(typeof(SessionFsReadFileRequest))]
[JsonSerializable(typeof(SessionFsReadFileResult))]
[JsonSerializable(typeof(SessionFsReaddirRequest))]
[JsonSerializable(typeof(SessionFsReaddirResult))]
[JsonSerializable(typeof(SessionFsReaddirWithTypesEntry))]
[JsonSerializable(typeof(SessionFsReaddirWithTypesRequest))]
[JsonSerializable(typeof(SessionFsReaddirWithTypesResult))]
[JsonSerializable(typeof(SessionFsRenameRequest))]
[JsonSerializable(typeof(SessionFsRmRequest))]
[JsonSerializable(typeof(SessionFsSetProviderRequest))]
[JsonSerializable(typeof(SessionFsSetProviderResult))]
[JsonSerializable(typeof(SessionFsStatRequest))]
[JsonSerializable(typeof(SessionFsStatResult))]
[JsonSerializable(typeof(SessionFsWriteFileRequest))]
[JsonSerializable(typeof(SessionHistoryCompactRequest))]
[JsonSerializable(typeof(SessionInstructionsGetSourcesRequest))]
[JsonSerializable(typeof(SessionMcpListRequest))]
[JsonSerializable(typeof(SessionMcpReloadRequest))]
[JsonSerializable(typeof(SessionMode))]
[JsonSerializable(typeof(SessionModeGetRequest))]
[JsonSerializable(typeof(SessionModelGetCurrentRequest))]
[JsonSerializable(typeof(SessionNameGetRequest))]
[JsonSerializable(typeof(SessionPlanDeleteRequest))]
[JsonSerializable(typeof(SessionPlanReadRequest))]
[JsonSerializable(typeof(SessionPluginsListRequest))]
[JsonSerializable(typeof(SessionSkillsListRequest))]
[JsonSerializable(typeof(SessionSkillsReloadRequest))]
[JsonSerializable(typeof(SessionUsageGetMetricsRequest))]
[JsonSerializable(typeof(SessionWorkspacesGetWorkspaceRequest))]
[JsonSerializable(typeof(SessionWorkspacesListFilesRequest))]
[JsonSerializable(typeof(SessionsForkRequest))]
[JsonSerializable(typeof(SessionsForkResult))]
[JsonSerializable(typeof(ShellExecRequest))]
[JsonSerializable(typeof(ShellExecResult))]
[JsonSerializable(typeof(ShellKillRequest))]
[JsonSerializable(typeof(ShellKillResult))]
[JsonSerializable(typeof(Skill))]
[JsonSerializable(typeof(SkillList))]
[JsonSerializable(typeof(SkillsConfigSetDisabledSkillsRequest))]
[JsonSerializable(typeof(SkillsDisableRequest))]
[JsonSerializable(typeof(SkillsDiscoverRequest))]
[JsonSerializable(typeof(SkillsEnableRequest))]
[JsonSerializable(typeof(Tool))]
[JsonSerializable(typeof(ToolList))]
[JsonSerializable(typeof(ToolsHandlePendingToolCallRequest))]
[JsonSerializable(typeof(ToolsListRequest))]
[JsonSerializable(typeof(UIElicitationRequest))]
[JsonSerializable(typeof(UIElicitationResponse))]
[JsonSerializable(typeof(UIElicitationResult))]
[JsonSerializable(typeof(UIElicitationSchema))]
[JsonSerializable(typeof(UIHandlePendingElicitationRequest))]
[JsonSerializable(typeof(UsageGetMetricsResult))]
[JsonSerializable(typeof(UsageMetricsCodeChanges))]
[JsonSerializable(typeof(UsageMetricsModelMetric))]
[JsonSerializable(typeof(UsageMetricsModelMetricRequests))]
[JsonSerializable(typeof(UsageMetricsModelMetricUsage))]
[JsonSerializable(typeof(WorkspacesCreateFileRequest))]
[JsonSerializable(typeof(WorkspacesGetWorkspaceResult))]
[JsonSerializable(typeof(WorkspacesGetWorkspaceResultWorkspace))]
[JsonSerializable(typeof(WorkspacesListFilesResult))]
[JsonSerializable(typeof(WorkspacesReadFileRequest))]
[JsonSerializable(typeof(WorkspacesReadFileResult))]
internal partial class RpcJsonContext : JsonSerializerContext;