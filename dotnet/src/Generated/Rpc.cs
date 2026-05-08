/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

#pragma warning disable CS0612 // Type or member is obsolete
#pragma warning disable CS0618 // Type or member is obsolete (with message)

using System.ComponentModel;
using System.ComponentModel.DataAnnotations;
using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;
using System.Text.Json;
using System.Text.Json.Serialization;

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

/// <summary>RPC data type for Connect operations.</summary>
internal sealed class ConnectResult
{
    /// <summary>Always true on success.</summary>
    [JsonPropertyName("ok")]
    public bool Ok { get; set; }

    /// <summary>Server protocol version number.</summary>
    [JsonPropertyName("protocolVersion")]
    public long ProtocolVersion { get; set; }

    /// <summary>Server package version.</summary>
    [JsonPropertyName("version")]
    public string Version { get; set; } = string.Empty;
}

/// <summary>RPC data type for Connect operations.</summary>
internal sealed class ConnectRequest
{
    /// <summary>Connection token; required when the server was started with COPILOT_CONNECTION_TOKEN.</summary>
    [JsonPropertyName("token")]
    public string? Token { get; set; }
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
    public string? Terms { get; set; }
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

/// <summary>RPC data type for ModelsList operations.</summary>
internal sealed class ModelsListRequest
{
    /// <summary>GitHub token for per-user model listing. When provided, resolves this token to determine the user's Copilot plan and available models instead of using the global auth.</summary>
    [JsonPropertyName("gitHubToken")]
    public string? GitHubToken { get; set; }
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

/// <summary>RPC data type for AccountGetQuota operations.</summary>
internal sealed class AccountGetQuotaRequest
{
    /// <summary>GitHub token for per-user quota lookup. When provided, resolves this token to determine the user's quota instead of using the global auth.</summary>
    [JsonPropertyName("gitHubToken")]
    public string? GitHubToken { get; set; }
}

/// <summary>RPC data type for DiscoveredMcpServer operations.</summary>
public sealed class DiscoveredMcpServer
{
    /// <summary>Whether the server is enabled (not in the disabled list).</summary>
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }

    /// <summary>Server name (config key).</summary>
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
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
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
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
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>RPC data type for McpConfigRemove operations.</summary>
internal sealed class McpConfigRemoveRequest
{
    /// <summary>Name of the MCP server to remove.</summary>
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>RPC data type for McpConfigEnable operations.</summary>
internal sealed class McpConfigEnableRequest
{
    /// <summary>Names of MCP servers to enable. Each server is removed from the persisted disabled list so new sessions spawn it. Unknown or already-enabled names are ignored.</summary>
    [JsonPropertyName("names")]
    public IList<string> Names { get => field ??= []; set; }
}

/// <summary>RPC data type for McpConfigDisable operations.</summary>
internal sealed class McpConfigDisableRequest
{
    /// <summary>Names of MCP servers to disable. Each server is added to the persisted disabled list so new sessions skip it. Already-disabled names are ignored. Active sessions keep their current connections until they end.</summary>
    [JsonPropertyName("names")]
    public IList<string> Names { get => field ??= []; set; }
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

/// <summary>RPC data type for SessionSuspend operations.</summary>
internal sealed class SessionSuspendRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
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

/// <summary>RPC data type for SessionAuthStatus operations.</summary>
public sealed class SessionAuthStatus
{
    /// <summary>Authentication type.</summary>
    [JsonPropertyName("authType")]
    public AuthInfoType? AuthType { get; set; }

    /// <summary>Copilot plan tier (e.g., individual_pro, business).</summary>
    [JsonPropertyName("copilotPlan")]
    public string? CopilotPlan { get; set; }

    /// <summary>Authentication host URL.</summary>
    [JsonPropertyName("host")]
    public string? Host { get; set; }

    /// <summary>Whether the session has resolved authentication.</summary>
    [JsonPropertyName("isAuthenticated")]
    public bool IsAuthenticated { get; set; }

    /// <summary>Authenticated login/username, if available.</summary>
    [JsonPropertyName("login")]
    public string? Login { get; set; }

    /// <summary>Human-readable authentication status description.</summary>
    [JsonPropertyName("statusMessage")]
    public string? StatusMessage { get; set; }
}

/// <summary>RPC data type for SessionAuthGetStatus operations.</summary>
internal sealed class SessionAuthGetStatusRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
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
    /// <summary>The session name (user-set or auto-generated), or null if not yet set.</summary>
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

    /// <summary>Gets or sets the <c>user_named</c> value.</summary>
    [JsonPropertyName("user_named")]
    public bool? UserNamed { get; set; }
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

    /// <summary>Absolute local file path of the agent definition. Only set for file-based agents loaded from disk; remote agents do not have a path.</summary>
    [JsonPropertyName("path")]
    public string? Path { get; set; }
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

/// <summary>RPC data type for TasksStartAgent operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksStartAgentResult
{
    /// <summary>Generated agent ID for the background task.</summary>
    [JsonPropertyName("agentId")]
    public string AgentId { get; set; } = string.Empty;
}

/// <summary>RPC data type for TasksStartAgent operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class TasksStartAgentRequest
{
    /// <summary>Type of agent to start (e.g., 'explore', 'task', 'general-purpose').</summary>
    [JsonPropertyName("agentType")]
    public string AgentType { get; set; } = string.Empty;

    /// <summary>Short description of the task.</summary>
    [JsonPropertyName("description")]
    public string? Description { get; set; }

    /// <summary>Optional model override.</summary>
    [JsonPropertyName("model")]
    public string? Model { get; set; }

    /// <summary>Short name for the agent, used to generate a human-readable ID.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Task prompt for the agent.</summary>
    [JsonPropertyName("prompt")]
    public string Prompt { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Polymorphic base type discriminated by <c>type</c>.</summary>
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "type",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(TaskInfoAgent), "agent")]
[JsonDerivedType(typeof(TaskInfoShell), "shell")]
public partial class TaskInfo
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("type")]
    public virtual string Type { get; set; } = string.Empty;
}


/// <summary>The <c>agent</c> variant of <see cref="TaskInfo"/>.</summary>
public partial class TaskInfoAgent : TaskInfo
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "agent";

    /// <summary>ISO 8601 timestamp when the current active period began.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("activeStartedAt")]
    public DateTimeOffset? ActiveStartedAt { get; set; }

    /// <summary>Accumulated active execution time in milliseconds.</summary>
    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("activeTimeMs")]
    public TimeSpan? ActiveTimeMs { get; set; }

    /// <summary>Type of agent running this task.</summary>
    [JsonPropertyName("agentType")]
    public required string AgentType { get; set; }

    /// <summary>Whether the task is currently in the original sync wait and can be moved to background mode. False once it is already backgrounded, idle, finished, or no longer has a promotable sync waiter.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("canPromoteToBackground")]
    public bool? CanPromoteToBackground { get; set; }

    /// <summary>ISO 8601 timestamp when the task finished.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("completedAt")]
    public DateTimeOffset? CompletedAt { get; set; }

    /// <summary>Short description of the task.</summary>
    [JsonPropertyName("description")]
    public required string Description { get; set; }

    /// <summary>Error message when the task failed.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("error")]
    public string? Error { get; set; }

    /// <summary>How the agent is currently being managed by the runtime.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("executionMode")]
    public TaskAgentInfoExecutionMode? ExecutionMode { get; set; }

    /// <summary>Unique task identifier.</summary>
    [JsonPropertyName("id")]
    public required string Id { get; set; }

    /// <summary>ISO 8601 timestamp when the agent entered idle state.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("idleSince")]
    public DateTimeOffset? IdleSince { get; set; }

    /// <summary>Most recent response text from the agent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("latestResponse")]
    public string? LatestResponse { get; set; }

    /// <summary>Model used for the task when specified.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("model")]
    public string? Model { get; set; }

    /// <summary>Prompt passed to the agent.</summary>
    [JsonPropertyName("prompt")]
    public required string Prompt { get; set; }

    /// <summary>Result text from the task when available.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("result")]
    public string? Result { get; set; }

    /// <summary>ISO 8601 timestamp when the task was started.</summary>
    [JsonPropertyName("startedAt")]
    public required DateTimeOffset StartedAt { get; set; }

    /// <summary>Current lifecycle status of the task.</summary>
    [JsonPropertyName("status")]
    public required TaskAgentInfoStatus Status { get; set; }

    /// <summary>Tool call ID associated with this agent task.</summary>
    [JsonPropertyName("toolCallId")]
    public required string ToolCallId { get; set; }
}

/// <summary>The <c>shell</c> variant of <see cref="TaskInfo"/>.</summary>
public partial class TaskInfoShell : TaskInfo
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "shell";

    /// <summary>Whether the shell runs inside a managed PTY session or as an independent background process.</summary>
    [JsonPropertyName("attachmentMode")]
    public required TaskShellInfoAttachmentMode AttachmentMode { get; set; }

    /// <summary>Whether this shell task can be promoted to background mode.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("canPromoteToBackground")]
    public bool? CanPromoteToBackground { get; set; }

    /// <summary>Command being executed.</summary>
    [JsonPropertyName("command")]
    public required string Command { get; set; }

    /// <summary>ISO 8601 timestamp when the task finished.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("completedAt")]
    public DateTimeOffset? CompletedAt { get; set; }

    /// <summary>Short description of the task.</summary>
    [JsonPropertyName("description")]
    public required string Description { get; set; }

    /// <summary>Whether the shell command is currently sync-waited or background-managed.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("executionMode")]
    public TaskShellInfoExecutionMode? ExecutionMode { get; set; }

    /// <summary>Unique task identifier.</summary>
    [JsonPropertyName("id")]
    public required string Id { get; set; }

    /// <summary>Path to the detached shell log, when available.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("logPath")]
    public string? LogPath { get; set; }

    /// <summary>Process ID when available.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("pid")]
    public long? Pid { get; set; }

    /// <summary>ISO 8601 timestamp when the task was started.</summary>
    [JsonPropertyName("startedAt")]
    public required DateTimeOffset StartedAt { get; set; }

    /// <summary>Current lifecycle status of the task.</summary>
    [JsonPropertyName("status")]
    public required TaskShellInfoStatus Status { get; set; }
}

/// <summary>RPC data type for TaskList operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TaskList
{
    /// <summary>Currently tracked tasks.</summary>
    [JsonPropertyName("tasks")]
    public IList<TaskInfo> Tasks { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionTasksList operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionTasksListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for TasksPromoteToBackground operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksPromoteToBackgroundResult
{
    /// <summary>Whether the task was successfully promoted to background mode.</summary>
    [JsonPropertyName("promoted")]
    public bool Promoted { get; set; }
}

/// <summary>RPC data type for TasksPromoteToBackground operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class TasksPromoteToBackgroundRequest
{
    /// <summary>Task identifier.</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for TasksCancel operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksCancelResult
{
    /// <summary>Whether the task was successfully cancelled.</summary>
    [JsonPropertyName("cancelled")]
    public bool Cancelled { get; set; }
}

/// <summary>RPC data type for TasksCancel operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class TasksCancelRequest
{
    /// <summary>Task identifier.</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for TasksRemove operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksRemoveResult
{
    /// <summary>Whether the task was removed. Returns false if the task does not exist or is still running/idle (cancel it first).</summary>
    [JsonPropertyName("removed")]
    public bool Removed { get; set; }
}

/// <summary>RPC data type for TasksRemove operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class TasksRemoveRequest
{
    /// <summary>Task identifier.</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

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
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
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
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
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
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
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

/// <summary>RPC data type for McpOauthLogin operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpOauthLoginResult
{
    /// <summary>URL the caller should open in a browser to complete OAuth. Omitted when cached tokens were still valid and no browser interaction was needed — the server is already reconnected in that case. When present, the runtime starts the callback listener before returning and continues the flow in the background; completion is signaled via session.mcp_server_status_changed.</summary>
    [JsonPropertyName("authorizationUrl")]
    public string? AuthorizationUrl { get; set; }
}

/// <summary>RPC data type for McpOauthLogin operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class McpOauthLoginRequest
{
    /// <summary>Optional override for the body text shown on the OAuth loopback callback success page. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass surface-specific copy telling the user where to return.</summary>
    [JsonPropertyName("callbackSuccessMessage")]
    public string? CallbackSuccessMessage { get; set; }

    /// <summary>Optional override for the OAuth client display name shown on the consent screen. Applies to newly registered dynamic clients only — existing registrations keep the name they were created with. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass their own surface-specific label so the consent screen matches the product the user sees.</summary>
    [JsonPropertyName("clientName")]
    public string? ClientName { get; set; }

    /// <summary>When true, clears any cached OAuth token for the server and runs a full new authorization. Use when the user explicitly wants to switch accounts or believes their session is stuck.</summary>
    [JsonPropertyName("forceReauth")]
    public bool? ForceReauth { get; set; }

    /// <summary>Name of the remote MCP server to authenticate.</summary>
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("serverName")]
    public string ServerName { get; set; } = string.Empty;

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

/// <summary>RPC data type for HandlePendingToolCall operations.</summary>
public sealed class HandlePendingToolCallResult
{
    /// <summary>Whether the tool call result was handled successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for HandlePendingToolCall operations.</summary>
internal sealed class HandlePendingToolCallRequest
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
[JsonDerivedType(typeof(PermissionDecisionApproveOnce), "approve-once")]
[JsonDerivedType(typeof(PermissionDecisionApproveForSession), "approve-for-session")]
[JsonDerivedType(typeof(PermissionDecisionApproveForLocation), "approve-for-location")]
[JsonDerivedType(typeof(PermissionDecisionApprovePermanently), "approve-permanently")]
[JsonDerivedType(typeof(PermissionDecisionReject), "reject")]
[JsonDerivedType(typeof(PermissionDecisionUserNotAvailable), "user-not-available")]
public partial class PermissionDecision
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("kind")]
    public virtual string Kind { get; set; } = string.Empty;
}


/// <summary>The <c>approve-once</c> variant of <see cref="PermissionDecision"/>.</summary>
public partial class PermissionDecisionApproveOnce : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "approve-once";
}

/// <summary>The approval to add as a session-scoped rule.</summary>
/// <remarks>Polymorphic base type discriminated by <c>kind</c>.</remarks>
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "kind",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(PermissionDecisionApproveForSessionApprovalCommands), "commands")]
[JsonDerivedType(typeof(PermissionDecisionApproveForSessionApprovalRead), "read")]
[JsonDerivedType(typeof(PermissionDecisionApproveForSessionApprovalWrite), "write")]
[JsonDerivedType(typeof(PermissionDecisionApproveForSessionApprovalMcp), "mcp")]
[JsonDerivedType(typeof(PermissionDecisionApproveForSessionApprovalMcpSampling), "mcp-sampling")]
[JsonDerivedType(typeof(PermissionDecisionApproveForSessionApprovalMemory), "memory")]
[JsonDerivedType(typeof(PermissionDecisionApproveForSessionApprovalCustomTool), "custom-tool")]
public partial class PermissionDecisionApproveForSessionApproval
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("kind")]
    public virtual string Kind { get; set; } = string.Empty;
}


/// <summary>The <c>commands</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</summary>
public partial class PermissionDecisionApproveForSessionApprovalCommands : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "commands";

    /// <summary>Gets or sets the <c>commandIdentifiers</c> value.</summary>
    [JsonPropertyName("commandIdentifiers")]
    public required IList<string> CommandIdentifiers { get; set; }
}

/// <summary>The <c>read</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</summary>
public partial class PermissionDecisionApproveForSessionApprovalRead : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "read";
}

/// <summary>The <c>write</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</summary>
public partial class PermissionDecisionApproveForSessionApprovalWrite : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "write";
}

/// <summary>The <c>mcp</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</summary>
public partial class PermissionDecisionApproveForSessionApprovalMcp : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "mcp";

    /// <summary>Gets or sets the <c>serverName</c> value.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }

    /// <summary>Gets or sets the <c>toolName</c> value.</summary>
    [JsonPropertyName("toolName")]
    public string? ToolName { get; set; }
}

/// <summary>The <c>mcp-sampling</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</summary>
public partial class PermissionDecisionApproveForSessionApprovalMcpSampling : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "mcp-sampling";

    /// <summary>Gets or sets the <c>serverName</c> value.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }
}

/// <summary>The <c>memory</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</summary>
public partial class PermissionDecisionApproveForSessionApprovalMemory : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "memory";
}

/// <summary>The <c>custom-tool</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</summary>
public partial class PermissionDecisionApproveForSessionApprovalCustomTool : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "custom-tool";

    /// <summary>Gets or sets the <c>toolName</c> value.</summary>
    [JsonPropertyName("toolName")]
    public required string ToolName { get; set; }
}

/// <summary>The <c>approve-for-session</c> variant of <see cref="PermissionDecision"/>.</summary>
public partial class PermissionDecisionApproveForSession : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "approve-for-session";

    /// <summary>The approval to add as a session-scoped rule.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("approval")]
    public PermissionDecisionApproveForSessionApproval? Approval { get; set; }

    /// <summary>The URL domain to approve for this session.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("domain")]
    public string? Domain { get; set; }
}

/// <summary>The approval to persist for this location.</summary>
/// <remarks>Polymorphic base type discriminated by <c>kind</c>.</remarks>
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "kind",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(PermissionDecisionApproveForLocationApprovalCommands), "commands")]
[JsonDerivedType(typeof(PermissionDecisionApproveForLocationApprovalRead), "read")]
[JsonDerivedType(typeof(PermissionDecisionApproveForLocationApprovalWrite), "write")]
[JsonDerivedType(typeof(PermissionDecisionApproveForLocationApprovalMcp), "mcp")]
[JsonDerivedType(typeof(PermissionDecisionApproveForLocationApprovalMcpSampling), "mcp-sampling")]
[JsonDerivedType(typeof(PermissionDecisionApproveForLocationApprovalMemory), "memory")]
[JsonDerivedType(typeof(PermissionDecisionApproveForLocationApprovalCustomTool), "custom-tool")]
public partial class PermissionDecisionApproveForLocationApproval
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("kind")]
    public virtual string Kind { get; set; } = string.Empty;
}


/// <summary>The <c>commands</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</summary>
public partial class PermissionDecisionApproveForLocationApprovalCommands : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "commands";

    /// <summary>Gets or sets the <c>commandIdentifiers</c> value.</summary>
    [JsonPropertyName("commandIdentifiers")]
    public required IList<string> CommandIdentifiers { get; set; }
}

/// <summary>The <c>read</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</summary>
public partial class PermissionDecisionApproveForLocationApprovalRead : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "read";
}

/// <summary>The <c>write</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</summary>
public partial class PermissionDecisionApproveForLocationApprovalWrite : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "write";
}

/// <summary>The <c>mcp</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</summary>
public partial class PermissionDecisionApproveForLocationApprovalMcp : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "mcp";

    /// <summary>Gets or sets the <c>serverName</c> value.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }

    /// <summary>Gets or sets the <c>toolName</c> value.</summary>
    [JsonPropertyName("toolName")]
    public string? ToolName { get; set; }
}

/// <summary>The <c>mcp-sampling</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</summary>
public partial class PermissionDecisionApproveForLocationApprovalMcpSampling : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "mcp-sampling";

    /// <summary>Gets or sets the <c>serverName</c> value.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }
}

/// <summary>The <c>memory</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</summary>
public partial class PermissionDecisionApproveForLocationApprovalMemory : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "memory";
}

/// <summary>The <c>custom-tool</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</summary>
public partial class PermissionDecisionApproveForLocationApprovalCustomTool : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "custom-tool";

    /// <summary>Gets or sets the <c>toolName</c> value.</summary>
    [JsonPropertyName("toolName")]
    public required string ToolName { get; set; }
}

/// <summary>The <c>approve-for-location</c> variant of <see cref="PermissionDecision"/>.</summary>
public partial class PermissionDecisionApproveForLocation : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "approve-for-location";

    /// <summary>The approval to persist for this location.</summary>
    [JsonPropertyName("approval")]
    public required PermissionDecisionApproveForLocationApproval Approval { get; set; }

    /// <summary>The location key (git root or cwd) to persist the approval to.</summary>
    [JsonPropertyName("locationKey")]
    public required string LocationKey { get; set; }
}

/// <summary>The <c>approve-permanently</c> variant of <see cref="PermissionDecision"/>.</summary>
public partial class PermissionDecisionApprovePermanently : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "approve-permanently";

    /// <summary>The URL domain to approve permanently.</summary>
    [JsonPropertyName("domain")]
    public required string Domain { get; set; }
}

/// <summary>The <c>reject</c> variant of <see cref="PermissionDecision"/>.</summary>
public partial class PermissionDecisionReject : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "reject";

    /// <summary>Optional feedback from the user explaining the denial.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("feedback")]
    public string? Feedback { get; set; }
}

/// <summary>The <c>user-not-available</c> variant of <see cref="PermissionDecision"/>.</summary>
public partial class PermissionDecisionUserNotAvailable : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "user-not-available";
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

/// <summary>RPC data type for PermissionsSetApproveAll operations.</summary>
public sealed class PermissionsSetApproveAllResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for PermissionsSetApproveAll operations.</summary>
internal sealed class PermissionsSetApproveAllRequest
{
    /// <summary>Whether to auto-approve all tool permission requests.</summary>
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for PermissionsResetSessionApprovals operations.</summary>
public sealed class PermissionsResetSessionApprovalsResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for PermissionsResetSessionApprovals operations.</summary>
internal sealed class PermissionsResetSessionApprovalsRequest
{
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

/// <summary>RPC data type for UsageMetricsModelMetricTokenDetail operations.</summary>
public sealed class UsageMetricsModelMetricTokenDetail
{
    /// <summary>Accumulated token count for this token type.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("tokenCount")]
    public long TokenCount { get; set; }
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

    /// <summary>Token count details per type.</summary>
    [JsonPropertyName("tokenDetails")]
    public IDictionary<string, UsageMetricsModelMetricTokenDetail>? TokenDetails { get; set; }

    /// <summary>Accumulated nano-AI units cost for this model.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("totalNanoAiu")]
    public long? TotalNanoAiu { get; set; }

    /// <summary>Token usage metrics for this model.</summary>
    [JsonPropertyName("usage")]
    public UsageMetricsModelMetricUsage Usage { get => field ??= new(); set; }
}

/// <summary>RPC data type for UsageMetricsTokenDetail operations.</summary>
public sealed class UsageMetricsTokenDetail
{
    /// <summary>Accumulated token count for this token type.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("tokenCount")]
    public long TokenCount { get; set; }
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

    /// <summary>Session-wide per-token-type accumulated token counts.</summary>
    [JsonPropertyName("tokenDetails")]
    public IDictionary<string, UsageMetricsTokenDetail>? TokenDetails { get; set; }

    /// <summary>Total time spent in model API calls (milliseconds).</summary>
    [Range(0, double.MaxValue)]
    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]
    [JsonPropertyName("totalApiDurationMs")]
    public TimeSpan TotalApiDurationMs { get; set; }

    /// <summary>Session-wide accumulated nano-AI units cost.</summary>
    [Range((double)0, (double)long.MaxValue)]
    [JsonPropertyName("totalNanoAiu")]
    public long? TotalNanoAiu { get; set; }

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

/// <summary>RPC data type for RemoteEnable operations.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class RemoteEnableResult
{
    /// <summary>Whether remote steering is enabled.</summary>
    [JsonPropertyName("remoteSteerable")]
    public bool RemoteSteerable { get; set; }

    /// <summary>Mission Control frontend URL for this session.</summary>
    [JsonPropertyName("url")]
    public string? Url { get; set; }
}

/// <summary>RPC data type for SessionRemoteEnable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionRemoteEnableRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionRemoteDisable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionRemoteDisableRequest
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
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct DiscoveredMcpServerSource : IEquatable<DiscoveredMcpServerSource>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="DiscoveredMcpServerSource"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="DiscoveredMcpServerSource"/>.</param>
    [JsonConstructor]
    public DiscoveredMcpServerSource(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="DiscoveredMcpServerSource"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>user</c> value.</summary>
    public static DiscoveredMcpServerSource User { get; } = new("user");

    /// <summary>Gets the <c>workspace</c> value.</summary>
    public static DiscoveredMcpServerSource Workspace { get; } = new("workspace");

    /// <summary>Gets the <c>plugin</c> value.</summary>
    public static DiscoveredMcpServerSource Plugin { get; } = new("plugin");

    /// <summary>Gets the <c>builtin</c> value.</summary>
    public static DiscoveredMcpServerSource Builtin { get; } = new("builtin");

    /// <summary>Returns a value indicating whether two <see cref="DiscoveredMcpServerSource"/> instances are equivalent.</summary>
    public static bool operator ==(DiscoveredMcpServerSource left, DiscoveredMcpServerSource right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="DiscoveredMcpServerSource"/> instances are not equivalent.</summary>
    public static bool operator !=(DiscoveredMcpServerSource left, DiscoveredMcpServerSource right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is DiscoveredMcpServerSource other && Equals(other);

    /// <inheritdoc />
    public bool Equals(DiscoveredMcpServerSource other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{DiscoveredMcpServerSource}"/> for serializing <see cref="DiscoveredMcpServerSource"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<DiscoveredMcpServerSource>
    {
        /// <inheritdoc />
        public override DiscoveredMcpServerSource Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, DiscoveredMcpServerSource value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(DiscoveredMcpServerSource));
        }
    }
}


/// <summary>Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio).</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct DiscoveredMcpServerType : IEquatable<DiscoveredMcpServerType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="DiscoveredMcpServerType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="DiscoveredMcpServerType"/>.</param>
    [JsonConstructor]
    public DiscoveredMcpServerType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="DiscoveredMcpServerType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>stdio</c> value.</summary>
    public static DiscoveredMcpServerType Stdio { get; } = new("stdio");

    /// <summary>Gets the <c>http</c> value.</summary>
    public static DiscoveredMcpServerType Http { get; } = new("http");

    /// <summary>Gets the <c>sse</c> value.</summary>
    public static DiscoveredMcpServerType Sse { get; } = new("sse");

    /// <summary>Gets the <c>memory</c> value.</summary>
    public static DiscoveredMcpServerType Memory { get; } = new("memory");

    /// <summary>Returns a value indicating whether two <see cref="DiscoveredMcpServerType"/> instances are equivalent.</summary>
    public static bool operator ==(DiscoveredMcpServerType left, DiscoveredMcpServerType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="DiscoveredMcpServerType"/> instances are not equivalent.</summary>
    public static bool operator !=(DiscoveredMcpServerType left, DiscoveredMcpServerType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is DiscoveredMcpServerType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(DiscoveredMcpServerType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{DiscoveredMcpServerType}"/> for serializing <see cref="DiscoveredMcpServerType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<DiscoveredMcpServerType>
    {
        /// <inheritdoc />
        public override DiscoveredMcpServerType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, DiscoveredMcpServerType value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(DiscoveredMcpServerType));
        }
    }
}


/// <summary>Path conventions used by this filesystem.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct SessionFsSetProviderConventions : IEquatable<SessionFsSetProviderConventions>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="SessionFsSetProviderConventions"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="SessionFsSetProviderConventions"/>.</param>
    [JsonConstructor]
    public SessionFsSetProviderConventions(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="SessionFsSetProviderConventions"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>windows</c> value.</summary>
    public static SessionFsSetProviderConventions Windows { get; } = new("windows");

    /// <summary>Gets the <c>posix</c> value.</summary>
    public static SessionFsSetProviderConventions Posix { get; } = new("posix");

    /// <summary>Returns a value indicating whether two <see cref="SessionFsSetProviderConventions"/> instances are equivalent.</summary>
    public static bool operator ==(SessionFsSetProviderConventions left, SessionFsSetProviderConventions right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="SessionFsSetProviderConventions"/> instances are not equivalent.</summary>
    public static bool operator !=(SessionFsSetProviderConventions left, SessionFsSetProviderConventions right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is SessionFsSetProviderConventions other && Equals(other);

    /// <inheritdoc />
    public bool Equals(SessionFsSetProviderConventions other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{SessionFsSetProviderConventions}"/> for serializing <see cref="SessionFsSetProviderConventions"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<SessionFsSetProviderConventions>
    {
        /// <inheritdoc />
        public override SessionFsSetProviderConventions Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SessionFsSetProviderConventions value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SessionFsSetProviderConventions));
        }
    }
}


/// <summary>Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct SessionLogLevel : IEquatable<SessionLogLevel>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="SessionLogLevel"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="SessionLogLevel"/>.</param>
    [JsonConstructor]
    public SessionLogLevel(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="SessionLogLevel"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>info</c> value.</summary>
    public static SessionLogLevel Info { get; } = new("info");

    /// <summary>Gets the <c>warning</c> value.</summary>
    public static SessionLogLevel Warning { get; } = new("warning");

    /// <summary>Gets the <c>error</c> value.</summary>
    public static SessionLogLevel Error { get; } = new("error");

    /// <summary>Returns a value indicating whether two <see cref="SessionLogLevel"/> instances are equivalent.</summary>
    public static bool operator ==(SessionLogLevel left, SessionLogLevel right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="SessionLogLevel"/> instances are not equivalent.</summary>
    public static bool operator !=(SessionLogLevel left, SessionLogLevel right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is SessionLogLevel other && Equals(other);

    /// <inheritdoc />
    public bool Equals(SessionLogLevel other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{SessionLogLevel}"/> for serializing <see cref="SessionLogLevel"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<SessionLogLevel>
    {
        /// <inheritdoc />
        public override SessionLogLevel Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SessionLogLevel value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SessionLogLevel));
        }
    }
}


/// <summary>Authentication type.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct AuthInfoType : IEquatable<AuthInfoType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="AuthInfoType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="AuthInfoType"/>.</param>
    [JsonConstructor]
    public AuthInfoType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="AuthInfoType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>hmac</c> value.</summary>
    public static AuthInfoType Hmac { get; } = new("hmac");

    /// <summary>Gets the <c>env</c> value.</summary>
    public static AuthInfoType Env { get; } = new("env");

    /// <summary>Gets the <c>user</c> value.</summary>
    public static AuthInfoType User { get; } = new("user");

    /// <summary>Gets the <c>gh-cli</c> value.</summary>
    public static AuthInfoType GhCli { get; } = new("gh-cli");

    /// <summary>Gets the <c>api-key</c> value.</summary>
    public static AuthInfoType ApiKey { get; } = new("api-key");

    /// <summary>Gets the <c>token</c> value.</summary>
    public static AuthInfoType Token { get; } = new("token");

    /// <summary>Gets the <c>copilot-api-token</c> value.</summary>
    public static AuthInfoType CopilotApiToken { get; } = new("copilot-api-token");

    /// <summary>Returns a value indicating whether two <see cref="AuthInfoType"/> instances are equivalent.</summary>
    public static bool operator ==(AuthInfoType left, AuthInfoType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="AuthInfoType"/> instances are not equivalent.</summary>
    public static bool operator !=(AuthInfoType left, AuthInfoType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is AuthInfoType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(AuthInfoType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{AuthInfoType}"/> for serializing <see cref="AuthInfoType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<AuthInfoType>
    {
        /// <inheritdoc />
        public override AuthInfoType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, AuthInfoType value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(AuthInfoType));
        }
    }
}


/// <summary>The agent mode. Valid values: "interactive", "plan", "autopilot".</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct SessionMode : IEquatable<SessionMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="SessionMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="SessionMode"/>.</param>
    [JsonConstructor]
    public SessionMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="SessionMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>interactive</c> value.</summary>
    public static SessionMode Interactive { get; } = new("interactive");

    /// <summary>Gets the <c>plan</c> value.</summary>
    public static SessionMode Plan { get; } = new("plan");

    /// <summary>Gets the <c>autopilot</c> value.</summary>
    public static SessionMode Autopilot { get; } = new("autopilot");

    /// <summary>Returns a value indicating whether two <see cref="SessionMode"/> instances are equivalent.</summary>
    public static bool operator ==(SessionMode left, SessionMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="SessionMode"/> instances are not equivalent.</summary>
    public static bool operator !=(SessionMode left, SessionMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is SessionMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(SessionMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{SessionMode}"/> for serializing <see cref="SessionMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<SessionMode>
    {
        /// <inheritdoc />
        public override SessionMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SessionMode value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SessionMode));
        }
    }
}


/// <summary>Defines the allowed values.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct WorkspacesGetWorkspaceResultWorkspaceHostType : IEquatable<WorkspacesGetWorkspaceResultWorkspaceHostType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="WorkspacesGetWorkspaceResultWorkspaceHostType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="WorkspacesGetWorkspaceResultWorkspaceHostType"/>.</param>
    [JsonConstructor]
    public WorkspacesGetWorkspaceResultWorkspaceHostType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="WorkspacesGetWorkspaceResultWorkspaceHostType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>github</c> value.</summary>
    public static WorkspacesGetWorkspaceResultWorkspaceHostType Github { get; } = new("github");

    /// <summary>Gets the <c>ado</c> value.</summary>
    public static WorkspacesGetWorkspaceResultWorkspaceHostType Ado { get; } = new("ado");

    /// <summary>Returns a value indicating whether two <see cref="WorkspacesGetWorkspaceResultWorkspaceHostType"/> instances are equivalent.</summary>
    public static bool operator ==(WorkspacesGetWorkspaceResultWorkspaceHostType left, WorkspacesGetWorkspaceResultWorkspaceHostType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="WorkspacesGetWorkspaceResultWorkspaceHostType"/> instances are not equivalent.</summary>
    public static bool operator !=(WorkspacesGetWorkspaceResultWorkspaceHostType left, WorkspacesGetWorkspaceResultWorkspaceHostType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is WorkspacesGetWorkspaceResultWorkspaceHostType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(WorkspacesGetWorkspaceResultWorkspaceHostType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{WorkspacesGetWorkspaceResultWorkspaceHostType}"/> for serializing <see cref="WorkspacesGetWorkspaceResultWorkspaceHostType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<WorkspacesGetWorkspaceResultWorkspaceHostType>
    {
        /// <inheritdoc />
        public override WorkspacesGetWorkspaceResultWorkspaceHostType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, WorkspacesGetWorkspaceResultWorkspaceHostType value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(WorkspacesGetWorkspaceResultWorkspaceHostType));
        }
    }
}


/// <summary>Defines the allowed values.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel : IEquatable<WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel"/>.</param>
    [JsonConstructor]
    public WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>local</c> value.</summary>
    public static WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel Local { get; } = new("local");

    /// <summary>Gets the <c>user</c> value.</summary>
    public static WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel User { get; } = new("user");

    /// <summary>Gets the <c>repo_and_user</c> value.</summary>
    public static WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel RepoAndUser { get; } = new("repo_and_user");

    /// <summary>Returns a value indicating whether two <see cref="WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel"/> instances are equivalent.</summary>
    public static bool operator ==(WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel left, WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel"/> instances are not equivalent.</summary>
    public static bool operator !=(WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel left, WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel other && Equals(other);

    /// <inheritdoc />
    public bool Equals(WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel}"/> for serializing <see cref="WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel>
    {
        /// <inheritdoc />
        public override WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(WorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel));
        }
    }
}


/// <summary>Where this source lives — used for UI grouping.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct InstructionsSourcesLocation : IEquatable<InstructionsSourcesLocation>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="InstructionsSourcesLocation"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="InstructionsSourcesLocation"/>.</param>
    [JsonConstructor]
    public InstructionsSourcesLocation(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="InstructionsSourcesLocation"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>user</c> value.</summary>
    public static InstructionsSourcesLocation User { get; } = new("user");

    /// <summary>Gets the <c>repository</c> value.</summary>
    public static InstructionsSourcesLocation Repository { get; } = new("repository");

    /// <summary>Gets the <c>working-directory</c> value.</summary>
    public static InstructionsSourcesLocation WorkingDirectory { get; } = new("working-directory");

    /// <summary>Returns a value indicating whether two <see cref="InstructionsSourcesLocation"/> instances are equivalent.</summary>
    public static bool operator ==(InstructionsSourcesLocation left, InstructionsSourcesLocation right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="InstructionsSourcesLocation"/> instances are not equivalent.</summary>
    public static bool operator !=(InstructionsSourcesLocation left, InstructionsSourcesLocation right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is InstructionsSourcesLocation other && Equals(other);

    /// <inheritdoc />
    public bool Equals(InstructionsSourcesLocation other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{InstructionsSourcesLocation}"/> for serializing <see cref="InstructionsSourcesLocation"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<InstructionsSourcesLocation>
    {
        /// <inheritdoc />
        public override InstructionsSourcesLocation Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, InstructionsSourcesLocation value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(InstructionsSourcesLocation));
        }
    }
}


/// <summary>Category of instruction source — used for merge logic.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct InstructionsSourcesType : IEquatable<InstructionsSourcesType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="InstructionsSourcesType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="InstructionsSourcesType"/>.</param>
    [JsonConstructor]
    public InstructionsSourcesType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="InstructionsSourcesType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>home</c> value.</summary>
    public static InstructionsSourcesType Home { get; } = new("home");

    /// <summary>Gets the <c>repo</c> value.</summary>
    public static InstructionsSourcesType Repo { get; } = new("repo");

    /// <summary>Gets the <c>model</c> value.</summary>
    public static InstructionsSourcesType Model { get; } = new("model");

    /// <summary>Gets the <c>vscode</c> value.</summary>
    public static InstructionsSourcesType Vscode { get; } = new("vscode");

    /// <summary>Gets the <c>nested-agents</c> value.</summary>
    public static InstructionsSourcesType NestedAgents { get; } = new("nested-agents");

    /// <summary>Gets the <c>child-instructions</c> value.</summary>
    public static InstructionsSourcesType ChildInstructions { get; } = new("child-instructions");

    /// <summary>Returns a value indicating whether two <see cref="InstructionsSourcesType"/> instances are equivalent.</summary>
    public static bool operator ==(InstructionsSourcesType left, InstructionsSourcesType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="InstructionsSourcesType"/> instances are not equivalent.</summary>
    public static bool operator !=(InstructionsSourcesType left, InstructionsSourcesType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is InstructionsSourcesType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(InstructionsSourcesType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{InstructionsSourcesType}"/> for serializing <see cref="InstructionsSourcesType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<InstructionsSourcesType>
    {
        /// <inheritdoc />
        public override InstructionsSourcesType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, InstructionsSourcesType value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(InstructionsSourcesType));
        }
    }
}


/// <summary>How the agent is currently being managed by the runtime.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct TaskAgentInfoExecutionMode : IEquatable<TaskAgentInfoExecutionMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="TaskAgentInfoExecutionMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="TaskAgentInfoExecutionMode"/>.</param>
    [JsonConstructor]
    public TaskAgentInfoExecutionMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="TaskAgentInfoExecutionMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>sync</c> value.</summary>
    public static TaskAgentInfoExecutionMode Sync { get; } = new("sync");

    /// <summary>Gets the <c>background</c> value.</summary>
    public static TaskAgentInfoExecutionMode Background { get; } = new("background");

    /// <summary>Returns a value indicating whether two <see cref="TaskAgentInfoExecutionMode"/> instances are equivalent.</summary>
    public static bool operator ==(TaskAgentInfoExecutionMode left, TaskAgentInfoExecutionMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="TaskAgentInfoExecutionMode"/> instances are not equivalent.</summary>
    public static bool operator !=(TaskAgentInfoExecutionMode left, TaskAgentInfoExecutionMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is TaskAgentInfoExecutionMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(TaskAgentInfoExecutionMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{TaskAgentInfoExecutionMode}"/> for serializing <see cref="TaskAgentInfoExecutionMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<TaskAgentInfoExecutionMode>
    {
        /// <inheritdoc />
        public override TaskAgentInfoExecutionMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, TaskAgentInfoExecutionMode value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(TaskAgentInfoExecutionMode));
        }
    }
}


/// <summary>Current lifecycle status of the task.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct TaskAgentInfoStatus : IEquatable<TaskAgentInfoStatus>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="TaskAgentInfoStatus"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="TaskAgentInfoStatus"/>.</param>
    [JsonConstructor]
    public TaskAgentInfoStatus(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="TaskAgentInfoStatus"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>running</c> value.</summary>
    public static TaskAgentInfoStatus Running { get; } = new("running");

    /// <summary>Gets the <c>idle</c> value.</summary>
    public static TaskAgentInfoStatus Idle { get; } = new("idle");

    /// <summary>Gets the <c>completed</c> value.</summary>
    public static TaskAgentInfoStatus Completed { get; } = new("completed");

    /// <summary>Gets the <c>failed</c> value.</summary>
    public static TaskAgentInfoStatus Failed { get; } = new("failed");

    /// <summary>Gets the <c>cancelled</c> value.</summary>
    public static TaskAgentInfoStatus Cancelled { get; } = new("cancelled");

    /// <summary>Returns a value indicating whether two <see cref="TaskAgentInfoStatus"/> instances are equivalent.</summary>
    public static bool operator ==(TaskAgentInfoStatus left, TaskAgentInfoStatus right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="TaskAgentInfoStatus"/> instances are not equivalent.</summary>
    public static bool operator !=(TaskAgentInfoStatus left, TaskAgentInfoStatus right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is TaskAgentInfoStatus other && Equals(other);

    /// <inheritdoc />
    public bool Equals(TaskAgentInfoStatus other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{TaskAgentInfoStatus}"/> for serializing <see cref="TaskAgentInfoStatus"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<TaskAgentInfoStatus>
    {
        /// <inheritdoc />
        public override TaskAgentInfoStatus Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, TaskAgentInfoStatus value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(TaskAgentInfoStatus));
        }
    }
}


/// <summary>Whether the shell runs inside a managed PTY session or as an independent background process.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct TaskShellInfoAttachmentMode : IEquatable<TaskShellInfoAttachmentMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="TaskShellInfoAttachmentMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="TaskShellInfoAttachmentMode"/>.</param>
    [JsonConstructor]
    public TaskShellInfoAttachmentMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="TaskShellInfoAttachmentMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>attached</c> value.</summary>
    public static TaskShellInfoAttachmentMode Attached { get; } = new("attached");

    /// <summary>Gets the <c>detached</c> value.</summary>
    public static TaskShellInfoAttachmentMode Detached { get; } = new("detached");

    /// <summary>Returns a value indicating whether two <see cref="TaskShellInfoAttachmentMode"/> instances are equivalent.</summary>
    public static bool operator ==(TaskShellInfoAttachmentMode left, TaskShellInfoAttachmentMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="TaskShellInfoAttachmentMode"/> instances are not equivalent.</summary>
    public static bool operator !=(TaskShellInfoAttachmentMode left, TaskShellInfoAttachmentMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is TaskShellInfoAttachmentMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(TaskShellInfoAttachmentMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{TaskShellInfoAttachmentMode}"/> for serializing <see cref="TaskShellInfoAttachmentMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<TaskShellInfoAttachmentMode>
    {
        /// <inheritdoc />
        public override TaskShellInfoAttachmentMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, TaskShellInfoAttachmentMode value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(TaskShellInfoAttachmentMode));
        }
    }
}


/// <summary>Whether the shell command is currently sync-waited or background-managed.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct TaskShellInfoExecutionMode : IEquatable<TaskShellInfoExecutionMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="TaskShellInfoExecutionMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="TaskShellInfoExecutionMode"/>.</param>
    [JsonConstructor]
    public TaskShellInfoExecutionMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="TaskShellInfoExecutionMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>sync</c> value.</summary>
    public static TaskShellInfoExecutionMode Sync { get; } = new("sync");

    /// <summary>Gets the <c>background</c> value.</summary>
    public static TaskShellInfoExecutionMode Background { get; } = new("background");

    /// <summary>Returns a value indicating whether two <see cref="TaskShellInfoExecutionMode"/> instances are equivalent.</summary>
    public static bool operator ==(TaskShellInfoExecutionMode left, TaskShellInfoExecutionMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="TaskShellInfoExecutionMode"/> instances are not equivalent.</summary>
    public static bool operator !=(TaskShellInfoExecutionMode left, TaskShellInfoExecutionMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is TaskShellInfoExecutionMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(TaskShellInfoExecutionMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{TaskShellInfoExecutionMode}"/> for serializing <see cref="TaskShellInfoExecutionMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<TaskShellInfoExecutionMode>
    {
        /// <inheritdoc />
        public override TaskShellInfoExecutionMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, TaskShellInfoExecutionMode value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(TaskShellInfoExecutionMode));
        }
    }
}


/// <summary>Current lifecycle status of the task.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct TaskShellInfoStatus : IEquatable<TaskShellInfoStatus>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="TaskShellInfoStatus"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="TaskShellInfoStatus"/>.</param>
    [JsonConstructor]
    public TaskShellInfoStatus(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="TaskShellInfoStatus"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>running</c> value.</summary>
    public static TaskShellInfoStatus Running { get; } = new("running");

    /// <summary>Gets the <c>idle</c> value.</summary>
    public static TaskShellInfoStatus Idle { get; } = new("idle");

    /// <summary>Gets the <c>completed</c> value.</summary>
    public static TaskShellInfoStatus Completed { get; } = new("completed");

    /// <summary>Gets the <c>failed</c> value.</summary>
    public static TaskShellInfoStatus Failed { get; } = new("failed");

    /// <summary>Gets the <c>cancelled</c> value.</summary>
    public static TaskShellInfoStatus Cancelled { get; } = new("cancelled");

    /// <summary>Returns a value indicating whether two <see cref="TaskShellInfoStatus"/> instances are equivalent.</summary>
    public static bool operator ==(TaskShellInfoStatus left, TaskShellInfoStatus right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="TaskShellInfoStatus"/> instances are not equivalent.</summary>
    public static bool operator !=(TaskShellInfoStatus left, TaskShellInfoStatus right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is TaskShellInfoStatus other && Equals(other);

    /// <inheritdoc />
    public bool Equals(TaskShellInfoStatus other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{TaskShellInfoStatus}"/> for serializing <see cref="TaskShellInfoStatus"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<TaskShellInfoStatus>
    {
        /// <inheritdoc />
        public override TaskShellInfoStatus Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, TaskShellInfoStatus value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(TaskShellInfoStatus));
        }
    }
}


/// <summary>Configuration source: user, workspace, plugin, or builtin.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct McpServerSource : IEquatable<McpServerSource>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="McpServerSource"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="McpServerSource"/>.</param>
    [JsonConstructor]
    public McpServerSource(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="McpServerSource"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>user</c> value.</summary>
    public static McpServerSource User { get; } = new("user");

    /// <summary>Gets the <c>workspace</c> value.</summary>
    public static McpServerSource Workspace { get; } = new("workspace");

    /// <summary>Gets the <c>plugin</c> value.</summary>
    public static McpServerSource Plugin { get; } = new("plugin");

    /// <summary>Gets the <c>builtin</c> value.</summary>
    public static McpServerSource Builtin { get; } = new("builtin");

    /// <summary>Returns a value indicating whether two <see cref="McpServerSource"/> instances are equivalent.</summary>
    public static bool operator ==(McpServerSource left, McpServerSource right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="McpServerSource"/> instances are not equivalent.</summary>
    public static bool operator !=(McpServerSource left, McpServerSource right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is McpServerSource other && Equals(other);

    /// <inheritdoc />
    public bool Equals(McpServerSource other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{McpServerSource}"/> for serializing <see cref="McpServerSource"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<McpServerSource>
    {
        /// <inheritdoc />
        public override McpServerSource Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, McpServerSource value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(McpServerSource));
        }
    }
}


/// <summary>Connection status: connected, failed, needs-auth, pending, disabled, or not_configured.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct McpServerStatus : IEquatable<McpServerStatus>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="McpServerStatus"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="McpServerStatus"/>.</param>
    [JsonConstructor]
    public McpServerStatus(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="McpServerStatus"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>connected</c> value.</summary>
    public static McpServerStatus Connected { get; } = new("connected");

    /// <summary>Gets the <c>failed</c> value.</summary>
    public static McpServerStatus Failed { get; } = new("failed");

    /// <summary>Gets the <c>needs-auth</c> value.</summary>
    public static McpServerStatus NeedsAuth { get; } = new("needs-auth");

    /// <summary>Gets the <c>pending</c> value.</summary>
    public static McpServerStatus Pending { get; } = new("pending");

    /// <summary>Gets the <c>disabled</c> value.</summary>
    public static McpServerStatus Disabled { get; } = new("disabled");

    /// <summary>Gets the <c>not_configured</c> value.</summary>
    public static McpServerStatus NotConfigured { get; } = new("not_configured");

    /// <summary>Returns a value indicating whether two <see cref="McpServerStatus"/> instances are equivalent.</summary>
    public static bool operator ==(McpServerStatus left, McpServerStatus right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="McpServerStatus"/> instances are not equivalent.</summary>
    public static bool operator !=(McpServerStatus left, McpServerStatus right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is McpServerStatus other && Equals(other);

    /// <inheritdoc />
    public bool Equals(McpServerStatus other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{McpServerStatus}"/> for serializing <see cref="McpServerStatus"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<McpServerStatus>
    {
        /// <inheritdoc />
        public override McpServerStatus Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, McpServerStatus value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(McpServerStatus));
        }
    }
}


/// <summary>Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/).</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct ExtensionSource : IEquatable<ExtensionSource>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="ExtensionSource"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="ExtensionSource"/>.</param>
    [JsonConstructor]
    public ExtensionSource(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="ExtensionSource"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>project</c> value.</summary>
    public static ExtensionSource Project { get; } = new("project");

    /// <summary>Gets the <c>user</c> value.</summary>
    public static ExtensionSource User { get; } = new("user");

    /// <summary>Returns a value indicating whether two <see cref="ExtensionSource"/> instances are equivalent.</summary>
    public static bool operator ==(ExtensionSource left, ExtensionSource right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="ExtensionSource"/> instances are not equivalent.</summary>
    public static bool operator !=(ExtensionSource left, ExtensionSource right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is ExtensionSource other && Equals(other);

    /// <inheritdoc />
    public bool Equals(ExtensionSource other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{ExtensionSource}"/> for serializing <see cref="ExtensionSource"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<ExtensionSource>
    {
        /// <inheritdoc />
        public override ExtensionSource Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, ExtensionSource value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(ExtensionSource));
        }
    }
}


/// <summary>Current status: running, disabled, failed, or starting.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct ExtensionStatus : IEquatable<ExtensionStatus>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="ExtensionStatus"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="ExtensionStatus"/>.</param>
    [JsonConstructor]
    public ExtensionStatus(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="ExtensionStatus"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>running</c> value.</summary>
    public static ExtensionStatus Running { get; } = new("running");

    /// <summary>Gets the <c>disabled</c> value.</summary>
    public static ExtensionStatus Disabled { get; } = new("disabled");

    /// <summary>Gets the <c>failed</c> value.</summary>
    public static ExtensionStatus Failed { get; } = new("failed");

    /// <summary>Gets the <c>starting</c> value.</summary>
    public static ExtensionStatus Starting { get; } = new("starting");

    /// <summary>Returns a value indicating whether two <see cref="ExtensionStatus"/> instances are equivalent.</summary>
    public static bool operator ==(ExtensionStatus left, ExtensionStatus right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="ExtensionStatus"/> instances are not equivalent.</summary>
    public static bool operator !=(ExtensionStatus left, ExtensionStatus right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is ExtensionStatus other && Equals(other);

    /// <inheritdoc />
    public bool Equals(ExtensionStatus other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{ExtensionStatus}"/> for serializing <see cref="ExtensionStatus"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<ExtensionStatus>
    {
        /// <inheritdoc />
        public override ExtensionStatus Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, ExtensionStatus value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(ExtensionStatus));
        }
    }
}


/// <summary>The user's response: accept (submitted), decline (rejected), or cancel (dismissed).</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct UIElicitationResponseAction : IEquatable<UIElicitationResponseAction>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="UIElicitationResponseAction"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="UIElicitationResponseAction"/>.</param>
    [JsonConstructor]
    public UIElicitationResponseAction(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="UIElicitationResponseAction"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>accept</c> value.</summary>
    public static UIElicitationResponseAction Accept { get; } = new("accept");

    /// <summary>Gets the <c>decline</c> value.</summary>
    public static UIElicitationResponseAction Decline { get; } = new("decline");

    /// <summary>Gets the <c>cancel</c> value.</summary>
    public static UIElicitationResponseAction Cancel { get; } = new("cancel");

    /// <summary>Returns a value indicating whether two <see cref="UIElicitationResponseAction"/> instances are equivalent.</summary>
    public static bool operator ==(UIElicitationResponseAction left, UIElicitationResponseAction right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="UIElicitationResponseAction"/> instances are not equivalent.</summary>
    public static bool operator !=(UIElicitationResponseAction left, UIElicitationResponseAction right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is UIElicitationResponseAction other && Equals(other);

    /// <inheritdoc />
    public bool Equals(UIElicitationResponseAction other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{UIElicitationResponseAction}"/> for serializing <see cref="UIElicitationResponseAction"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<UIElicitationResponseAction>
    {
        /// <inheritdoc />
        public override UIElicitationResponseAction Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, UIElicitationResponseAction value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(UIElicitationResponseAction));
        }
    }
}


/// <summary>Signal to send (default: SIGTERM).</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct ShellKillSignal : IEquatable<ShellKillSignal>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="ShellKillSignal"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="ShellKillSignal"/>.</param>
    [JsonConstructor]
    public ShellKillSignal(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="ShellKillSignal"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>SIGTERM</c> value.</summary>
    public static ShellKillSignal SIGTERM { get; } = new("SIGTERM");

    /// <summary>Gets the <c>SIGKILL</c> value.</summary>
    public static ShellKillSignal SIGKILL { get; } = new("SIGKILL");

    /// <summary>Gets the <c>SIGINT</c> value.</summary>
    public static ShellKillSignal SIGINT { get; } = new("SIGINT");

    /// <summary>Returns a value indicating whether two <see cref="ShellKillSignal"/> instances are equivalent.</summary>
    public static bool operator ==(ShellKillSignal left, ShellKillSignal right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="ShellKillSignal"/> instances are not equivalent.</summary>
    public static bool operator !=(ShellKillSignal left, ShellKillSignal right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is ShellKillSignal other && Equals(other);

    /// <inheritdoc />
    public bool Equals(ShellKillSignal other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{ShellKillSignal}"/> for serializing <see cref="ShellKillSignal"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<ShellKillSignal>
    {
        /// <inheritdoc />
        public override ShellKillSignal Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, ShellKillSignal value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(ShellKillSignal));
        }
    }
}


/// <summary>Error classification.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct SessionFsErrorCode : IEquatable<SessionFsErrorCode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="SessionFsErrorCode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="SessionFsErrorCode"/>.</param>
    [JsonConstructor]
    public SessionFsErrorCode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="SessionFsErrorCode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>ENOENT</c> value.</summary>
    public static SessionFsErrorCode ENOENT { get; } = new("ENOENT");

    /// <summary>Gets the <c>UNKNOWN</c> value.</summary>
    public static SessionFsErrorCode UNKNOWN { get; } = new("UNKNOWN");

    /// <summary>Returns a value indicating whether two <see cref="SessionFsErrorCode"/> instances are equivalent.</summary>
    public static bool operator ==(SessionFsErrorCode left, SessionFsErrorCode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="SessionFsErrorCode"/> instances are not equivalent.</summary>
    public static bool operator !=(SessionFsErrorCode left, SessionFsErrorCode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is SessionFsErrorCode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(SessionFsErrorCode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{SessionFsErrorCode}"/> for serializing <see cref="SessionFsErrorCode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<SessionFsErrorCode>
    {
        /// <inheritdoc />
        public override SessionFsErrorCode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SessionFsErrorCode value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SessionFsErrorCode));
        }
    }
}


/// <summary>Entry type.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct SessionFsReaddirWithTypesEntryType : IEquatable<SessionFsReaddirWithTypesEntryType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="SessionFsReaddirWithTypesEntryType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="SessionFsReaddirWithTypesEntryType"/>.</param>
    [JsonConstructor]
    public SessionFsReaddirWithTypesEntryType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="SessionFsReaddirWithTypesEntryType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Gets the <c>file</c> value.</summary>
    public static SessionFsReaddirWithTypesEntryType File { get; } = new("file");

    /// <summary>Gets the <c>directory</c> value.</summary>
    public static SessionFsReaddirWithTypesEntryType Directory { get; } = new("directory");

    /// <summary>Returns a value indicating whether two <see cref="SessionFsReaddirWithTypesEntryType"/> instances are equivalent.</summary>
    public static bool operator ==(SessionFsReaddirWithTypesEntryType left, SessionFsReaddirWithTypesEntryType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="SessionFsReaddirWithTypesEntryType"/> instances are not equivalent.</summary>
    public static bool operator !=(SessionFsReaddirWithTypesEntryType left, SessionFsReaddirWithTypesEntryType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is SessionFsReaddirWithTypesEntryType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(SessionFsReaddirWithTypesEntryType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{SessionFsReaddirWithTypesEntryType}"/> for serializing <see cref="SessionFsReaddirWithTypesEntryType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<SessionFsReaddirWithTypesEntryType>
    {
        /// <inheritdoc />
        public override SessionFsReaddirWithTypesEntryType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GitHub.Copilot.SDK.GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SessionFsReaddirWithTypesEntryType value, JsonSerializerOptions options)
        {
            GitHub.Copilot.SDK.GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SessionFsReaddirWithTypesEntryType));
        }
    }
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

    /// <summary>Calls "connect".</summary>
    internal async Task<ConnectResult> ConnectAsync(string? token = null, CancellationToken cancellationToken = default)
    {
        var request = new ConnectRequest { Token = token };
        return await CopilotClient.InvokeRpcAsync<ConnectResult>(_rpc, "connect", [request], cancellationToken);
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
    public async Task<ModelList> ListAsync(string? gitHubToken = null, CancellationToken cancellationToken = default)
    {
        var request = new ModelsListRequest { GitHubToken = gitHubToken };
        return await CopilotClient.InvokeRpcAsync<ModelList>(_rpc, "models.list", [request], cancellationToken);
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
    public async Task<AccountGetQuotaResult> GetQuotaAsync(string? gitHubToken = null, CancellationToken cancellationToken = default)
    {
        var request = new AccountGetQuotaRequest { GitHubToken = gitHubToken };
        return await CopilotClient.InvokeRpcAsync<AccountGetQuotaResult>(_rpc, "account.getQuota", [request], cancellationToken);
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

    /// <summary>Calls "mcp.config.enable".</summary>
    public async Task EnableAsync(IList<string> names, CancellationToken cancellationToken = default)
    {
        var request = new McpConfigEnableRequest { Names = names };
        await CopilotClient.InvokeRpcAsync(_rpc, "mcp.config.enable", [request], cancellationToken);
    }

    /// <summary>Calls "mcp.config.disable".</summary>
    public async Task DisableAsync(IList<string> names, CancellationToken cancellationToken = default)
    {
        var request = new McpConfigDisableRequest { Names = names };
        await CopilotClient.InvokeRpcAsync(_rpc, "mcp.config.disable", [request], cancellationToken);
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
        Auth = new AuthApi(rpc, sessionId);
        Model = new ModelApi(rpc, sessionId);
        Mode = new ModeApi(rpc, sessionId);
        Name = new NameApi(rpc, sessionId);
        Plan = new PlanApi(rpc, sessionId);
        Workspaces = new WorkspacesApi(rpc, sessionId);
        Instructions = new InstructionsApi(rpc, sessionId);
        Fleet = new FleetApi(rpc, sessionId);
        Agent = new AgentApi(rpc, sessionId);
        Tasks = new TasksApi(rpc, sessionId);
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
        Remote = new RemoteApi(rpc, sessionId);
    }

    /// <summary>Auth APIs.</summary>
    public AuthApi Auth { get; }

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

    /// <summary>Tasks APIs.</summary>
    public TasksApi Tasks { get; }

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

    /// <summary>Remote APIs.</summary>
    public RemoteApi Remote { get; }

    /// <summary>Calls "session.suspend".</summary>
    public async Task SuspendAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionSuspendRequest { SessionId = _sessionId };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.suspend", [request], cancellationToken);
    }

    /// <summary>Calls "session.log".</summary>
    public async Task<LogResult> LogAsync(string message, SessionLogLevel? level = null, bool? ephemeral = null, string? url = null, CancellationToken cancellationToken = default)
    {
        var request = new LogRequest { SessionId = _sessionId, Message = message, Level = level, Ephemeral = ephemeral, Url = url };
        return await CopilotClient.InvokeRpcAsync<LogResult>(_rpc, "session.log", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Auth APIs.</summary>
public sealed class AuthApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal AuthApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.auth.getStatus".</summary>
    public async Task<SessionAuthStatus> GetStatusAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionAuthGetStatusRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionAuthStatus>(_rpc, "session.auth.getStatus", [request], cancellationToken);
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

/// <summary>Provides session-scoped Tasks APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal TasksApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.tasks.startAgent".</summary>
    public async Task<TasksStartAgentResult> StartAgentAsync(string agentType, string prompt, string name, string? description = null, string? model = null, CancellationToken cancellationToken = default)
    {
        var request = new TasksStartAgentRequest { SessionId = _sessionId, AgentType = agentType, Prompt = prompt, Name = name, Description = description, Model = model };
        return await CopilotClient.InvokeRpcAsync<TasksStartAgentResult>(_rpc, "session.tasks.startAgent", [request], cancellationToken);
    }

    /// <summary>Calls "session.tasks.list".</summary>
    public async Task<TaskList> ListAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionTasksListRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<TaskList>(_rpc, "session.tasks.list", [request], cancellationToken);
    }

    /// <summary>Calls "session.tasks.promoteToBackground".</summary>
    public async Task<TasksPromoteToBackgroundResult> PromoteToBackgroundAsync(string id, CancellationToken cancellationToken = default)
    {
        var request = new TasksPromoteToBackgroundRequest { SessionId = _sessionId, Id = id };
        return await CopilotClient.InvokeRpcAsync<TasksPromoteToBackgroundResult>(_rpc, "session.tasks.promoteToBackground", [request], cancellationToken);
    }

    /// <summary>Calls "session.tasks.cancel".</summary>
    public async Task<TasksCancelResult> CancelAsync(string id, CancellationToken cancellationToken = default)
    {
        var request = new TasksCancelRequest { SessionId = _sessionId, Id = id };
        return await CopilotClient.InvokeRpcAsync<TasksCancelResult>(_rpc, "session.tasks.cancel", [request], cancellationToken);
    }

    /// <summary>Calls "session.tasks.remove".</summary>
    public async Task<TasksRemoveResult> RemoveAsync(string id, CancellationToken cancellationToken = default)
    {
        var request = new TasksRemoveRequest { SessionId = _sessionId, Id = id };
        return await CopilotClient.InvokeRpcAsync<TasksRemoveResult>(_rpc, "session.tasks.remove", [request], cancellationToken);
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
        Oauth = new McpOauthApi(rpc, sessionId);
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

    /// <summary>Oauth APIs.</summary>
    public McpOauthApi Oauth { get; }
}

/// <summary>Provides session-scoped McpOauth APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpOauthApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal McpOauthApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.mcp.oauth.login".</summary>
    public async Task<McpOauthLoginResult> LoginAsync(string serverName, bool? forceReauth = null, string? clientName = null, string? callbackSuccessMessage = null, CancellationToken cancellationToken = default)
    {
        var request = new McpOauthLoginRequest { SessionId = _sessionId, ServerName = serverName, ForceReauth = forceReauth, ClientName = clientName, CallbackSuccessMessage = callbackSuccessMessage };
        return await CopilotClient.InvokeRpcAsync<McpOauthLoginResult>(_rpc, "session.mcp.oauth.login", [request], cancellationToken);
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
    public async Task<HandlePendingToolCallResult> HandlePendingToolCallAsync(string requestId, object? result = null, string? error = null, CancellationToken cancellationToken = default)
    {
        var request = new HandlePendingToolCallRequest { SessionId = _sessionId, RequestId = requestId, Result = result, Error = error };
        return await CopilotClient.InvokeRpcAsync<HandlePendingToolCallResult>(_rpc, "session.tools.handlePendingToolCall", [request], cancellationToken);
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

    /// <summary>Calls "session.permissions.setApproveAll".</summary>
    public async Task<PermissionsSetApproveAllResult> SetApproveAllAsync(bool enabled, CancellationToken cancellationToken = default)
    {
        var request = new PermissionsSetApproveAllRequest { SessionId = _sessionId, Enabled = enabled };
        return await CopilotClient.InvokeRpcAsync<PermissionsSetApproveAllResult>(_rpc, "session.permissions.setApproveAll", [request], cancellationToken);
    }

    /// <summary>Calls "session.permissions.resetSessionApprovals".</summary>
    public async Task<PermissionsResetSessionApprovalsResult> ResetSessionApprovalsAsync(CancellationToken cancellationToken = default)
    {
        var request = new PermissionsResetSessionApprovalsRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<PermissionsResetSessionApprovalsResult>(_rpc, "session.permissions.resetSessionApprovals", [request], cancellationToken);
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

/// <summary>Provides session-scoped Remote APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class RemoteApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal RemoteApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.remote.enable".</summary>
    public async Task<RemoteEnableResult> EnableAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionRemoteEnableRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<RemoteEnableResult>(_rpc, "session.remote.enable", [request], cancellationToken);
    }

    /// <summary>Calls "session.remote.disable".</summary>
    public async Task DisableAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionRemoteDisableRequest { SessionId = _sessionId };
        await CopilotClient.InvokeRpcAsync(_rpc, "session.remote.disable", [request], cancellationToken);
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
internal static class ClientSessionApiRegistration
{
    /// <summary>
    /// Registers handlers for server-to-client session API calls.
    /// Each incoming call includes a <c>sessionId</c> in its params object,
    /// which is used to resolve the session's handler group.
    /// </summary>
    public static void RegisterClientSessionApiHandlers(JsonRpc rpc, Func<string, ClientSessionApiHandlers> getHandlers)
    {
        rpc.SetLocalRpcMethod("sessionFs.readFile", (Func<SessionFsReadFileRequest, CancellationToken, ValueTask<SessionFsReadFileResult>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.ReadFileAsync(request, cancellationToken);
        }), singleObjectParam: true);
        rpc.SetLocalRpcMethod("sessionFs.writeFile", (Func<SessionFsWriteFileRequest, CancellationToken, ValueTask<SessionFsError?>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.WriteFileAsync(request, cancellationToken);
        }), singleObjectParam: true);
        rpc.SetLocalRpcMethod("sessionFs.appendFile", (Func<SessionFsAppendFileRequest, CancellationToken, ValueTask<SessionFsError?>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.AppendFileAsync(request, cancellationToken);
        }), singleObjectParam: true);
        rpc.SetLocalRpcMethod("sessionFs.exists", (Func<SessionFsExistsRequest, CancellationToken, ValueTask<SessionFsExistsResult>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.ExistsAsync(request, cancellationToken);
        }), singleObjectParam: true);
        rpc.SetLocalRpcMethod("sessionFs.stat", (Func<SessionFsStatRequest, CancellationToken, ValueTask<SessionFsStatResult>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.StatAsync(request, cancellationToken);
        }), singleObjectParam: true);
        rpc.SetLocalRpcMethod("sessionFs.mkdir", (Func<SessionFsMkdirRequest, CancellationToken, ValueTask<SessionFsError?>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.MkdirAsync(request, cancellationToken);
        }), singleObjectParam: true);
        rpc.SetLocalRpcMethod("sessionFs.readdir", (Func<SessionFsReaddirRequest, CancellationToken, ValueTask<SessionFsReaddirResult>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.ReaddirAsync(request, cancellationToken);
        }), singleObjectParam: true);
        rpc.SetLocalRpcMethod("sessionFs.readdirWithTypes", (Func<SessionFsReaddirWithTypesRequest, CancellationToken, ValueTask<SessionFsReaddirWithTypesResult>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.ReaddirWithTypesAsync(request, cancellationToken);
        }), singleObjectParam: true);
        rpc.SetLocalRpcMethod("sessionFs.rm", (Func<SessionFsRmRequest, CancellationToken, ValueTask<SessionFsError?>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.RmAsync(request, cancellationToken);
        }), singleObjectParam: true);
        rpc.SetLocalRpcMethod("sessionFs.rename", (Func<SessionFsRenameRequest, CancellationToken, ValueTask<SessionFsError?>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.RenameAsync(request, cancellationToken);
        }), singleObjectParam: true);
    }
}

[JsonSourceGenerationOptions(
    JsonSerializerDefaults.Web,
    AllowOutOfOrderMetadataProperties = true,
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
[JsonSerializable(typeof(bool))]
[JsonSerializable(typeof(double))]
[JsonSerializable(typeof(int))]
[JsonSerializable(typeof(long))]
[JsonSerializable(typeof(string))]
[JsonSerializable(typeof(AccountGetQuotaRequest))]
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
[JsonSerializable(typeof(ConnectRequest))]
[JsonSerializable(typeof(ConnectResult))]
[JsonSerializable(typeof(CurrentModel))]
[JsonSerializable(typeof(DiscoveredMcpServer))]
[JsonSerializable(typeof(Extension))]
[JsonSerializable(typeof(ExtensionList))]
[JsonSerializable(typeof(ExtensionsDisableRequest))]
[JsonSerializable(typeof(ExtensionsEnableRequest))]
[JsonSerializable(typeof(FleetStartRequest))]
[JsonSerializable(typeof(FleetStartResult))]
[JsonSerializable(typeof(HandlePendingToolCallRequest))]
[JsonSerializable(typeof(HandlePendingToolCallResult))]
[JsonSerializable(typeof(HistoryCompactContextWindow))]
[JsonSerializable(typeof(HistoryCompactResult))]
[JsonSerializable(typeof(HistoryTruncateRequest))]
[JsonSerializable(typeof(HistoryTruncateResult))]
[JsonSerializable(typeof(InstructionsGetSourcesResult))]
[JsonSerializable(typeof(InstructionsSources))]
[JsonSerializable(typeof(LogRequest))]
[JsonSerializable(typeof(LogResult))]
[JsonSerializable(typeof(McpConfigAddRequest))]
[JsonSerializable(typeof(McpConfigDisableRequest))]
[JsonSerializable(typeof(McpConfigEnableRequest))]
[JsonSerializable(typeof(McpConfigList))]
[JsonSerializable(typeof(McpConfigRemoveRequest))]
[JsonSerializable(typeof(McpConfigUpdateRequest))]
[JsonSerializable(typeof(McpDisableRequest))]
[JsonSerializable(typeof(McpDiscoverRequest))]
[JsonSerializable(typeof(McpDiscoverResult))]
[JsonSerializable(typeof(McpEnableRequest))]
[JsonSerializable(typeof(McpOauthLoginRequest))]
[JsonSerializable(typeof(McpOauthLoginResult))]
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
[JsonSerializable(typeof(ModelsListRequest))]
[JsonSerializable(typeof(NameGetResult))]
[JsonSerializable(typeof(NameSetRequest))]
[JsonSerializable(typeof(PermissionDecision))]
[JsonSerializable(typeof(PermissionDecisionApproveForLocationApproval))]
[JsonSerializable(typeof(PermissionDecisionApproveForSessionApproval))]
[JsonSerializable(typeof(PermissionDecisionRequest))]
[JsonSerializable(typeof(PermissionRequestResult))]
[JsonSerializable(typeof(PermissionsResetSessionApprovalsRequest))]
[JsonSerializable(typeof(PermissionsResetSessionApprovalsResult))]
[JsonSerializable(typeof(PermissionsSetApproveAllRequest))]
[JsonSerializable(typeof(PermissionsSetApproveAllResult))]
[JsonSerializable(typeof(PingRequest))]
[JsonSerializable(typeof(PingResult))]
[JsonSerializable(typeof(PlanReadResult))]
[JsonSerializable(typeof(PlanUpdateRequest))]
[JsonSerializable(typeof(Plugin))]
[JsonSerializable(typeof(PluginList))]
[JsonSerializable(typeof(RemoteEnableResult))]
[JsonSerializable(typeof(ServerSkill))]
[JsonSerializable(typeof(ServerSkillList))]
[JsonSerializable(typeof(SessionAgentDeselectRequest))]
[JsonSerializable(typeof(SessionAgentGetCurrentRequest))]
[JsonSerializable(typeof(SessionAgentListRequest))]
[JsonSerializable(typeof(SessionAgentReloadRequest))]
[JsonSerializable(typeof(SessionAuthGetStatusRequest))]
[JsonSerializable(typeof(SessionAuthStatus))]
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
[JsonSerializable(typeof(SessionRemoteDisableRequest))]
[JsonSerializable(typeof(SessionRemoteEnableRequest))]
[JsonSerializable(typeof(SessionSkillsListRequest))]
[JsonSerializable(typeof(SessionSkillsReloadRequest))]
[JsonSerializable(typeof(SessionSuspendRequest))]
[JsonSerializable(typeof(SessionTasksListRequest))]
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
[JsonSerializable(typeof(TaskInfo))]
[JsonSerializable(typeof(TaskList))]
[JsonSerializable(typeof(TasksCancelRequest))]
[JsonSerializable(typeof(TasksCancelResult))]
[JsonSerializable(typeof(TasksPromoteToBackgroundRequest))]
[JsonSerializable(typeof(TasksPromoteToBackgroundResult))]
[JsonSerializable(typeof(TasksRemoveRequest))]
[JsonSerializable(typeof(TasksRemoveResult))]
[JsonSerializable(typeof(TasksStartAgentRequest))]
[JsonSerializable(typeof(TasksStartAgentResult))]
[JsonSerializable(typeof(Tool))]
[JsonSerializable(typeof(ToolList))]
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
[JsonSerializable(typeof(UsageMetricsModelMetricTokenDetail))]
[JsonSerializable(typeof(UsageMetricsModelMetricUsage))]
[JsonSerializable(typeof(UsageMetricsTokenDetail))]
[JsonSerializable(typeof(WorkspacesCreateFileRequest))]
[JsonSerializable(typeof(WorkspacesGetWorkspaceResult))]
[JsonSerializable(typeof(WorkspacesGetWorkspaceResultWorkspace))]
[JsonSerializable(typeof(WorkspacesListFilesResult))]
[JsonSerializable(typeof(WorkspacesReadFileRequest))]
[JsonSerializable(typeof(WorkspacesReadFileResult))]
internal partial class RpcJsonContext : JsonSerializerContext;