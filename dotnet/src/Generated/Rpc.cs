/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

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
public class PingResult
{
    /// <summary>Echoed message (or default greeting).</summary>
    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    /// <summary>Server timestamp in milliseconds.</summary>
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    /// <summary>Server protocol version number.</summary>
    [JsonPropertyName("protocolVersion")]
    public long ProtocolVersion { get; set; }
}

/// <summary>RPC data type for Ping operations.</summary>
internal class PingRequest
{
    /// <summary>Optional message to echo back.</summary>
    [JsonPropertyName("message")]
    public string? Message { get; set; }
}

/// <summary>Feature flags indicating what the model supports.</summary>
public class ModelCapabilitiesSupports
{
    /// <summary>Whether this model supports vision/image input.</summary>
    [JsonPropertyName("vision")]
    public bool? Vision { get; set; }

    /// <summary>Whether this model supports reasoning effort configuration.</summary>
    [JsonPropertyName("reasoningEffort")]
    public bool? ReasoningEffort { get; set; }
}

/// <summary>Vision-specific limits.</summary>
public class ModelCapabilitiesLimitsVision
{
    /// <summary>MIME types the model accepts.</summary>
    [JsonPropertyName("supported_media_types")]
    public IList<string> SupportedMediaTypes { get => field ??= []; set; }

    /// <summary>Maximum number of images per prompt.</summary>
    [Range(typeof(long), "1", "long.MaxValue")]
    [JsonPropertyName("max_prompt_images")]
    public long MaxPromptImages { get; set; }

    /// <summary>Maximum image size in bytes.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("max_prompt_image_size")]
    public long MaxPromptImageSize { get; set; }
}

/// <summary>Token limits for prompts, outputs, and context window.</summary>
public class ModelCapabilitiesLimits
{
    /// <summary>Maximum number of prompt/input tokens.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("max_prompt_tokens")]
    public long? MaxPromptTokens { get; set; }

    /// <summary>Maximum number of output/completion tokens.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("max_output_tokens")]
    public long? MaxOutputTokens { get; set; }

    /// <summary>Maximum total context window size in tokens.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("max_context_window_tokens")]
    public long MaxContextWindowTokens { get; set; }

    /// <summary>Vision-specific limits.</summary>
    [JsonPropertyName("vision")]
    public ModelCapabilitiesLimitsVision? Vision { get; set; }
}

/// <summary>Model capabilities and limits.</summary>
public class ModelCapabilities
{
    /// <summary>Feature flags indicating what the model supports.</summary>
    [JsonPropertyName("supports")]
    public ModelCapabilitiesSupports Supports { get => field ??= new(); set; }

    /// <summary>Token limits for prompts, outputs, and context window.</summary>
    [JsonPropertyName("limits")]
    public ModelCapabilitiesLimits Limits { get => field ??= new(); set; }
}

/// <summary>Policy state (if applicable).</summary>
public class ModelPolicy
{
    /// <summary>Current policy state for this model.</summary>
    [JsonPropertyName("state")]
    public string State { get; set; } = string.Empty;

    /// <summary>Usage terms or conditions for this model.</summary>
    [JsonPropertyName("terms")]
    public string Terms { get; set; } = string.Empty;
}

/// <summary>Billing information.</summary>
public class ModelBilling
{
    /// <summary>Billing cost multiplier relative to the base rate.</summary>
    [JsonPropertyName("multiplier")]
    public double Multiplier { get; set; }
}

/// <summary>RPC data type for Model operations.</summary>
public class Model
{
    /// <summary>Model identifier (e.g., "claude-sonnet-4.5").</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Display name.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Model capabilities and limits.</summary>
    [JsonPropertyName("capabilities")]
    public ModelCapabilities Capabilities { get => field ??= new(); set; }

    /// <summary>Policy state (if applicable).</summary>
    [JsonPropertyName("policy")]
    public ModelPolicy? Policy { get; set; }

    /// <summary>Billing information.</summary>
    [JsonPropertyName("billing")]
    public ModelBilling? Billing { get; set; }

    /// <summary>Supported reasoning effort levels (only present if model supports reasoning effort).</summary>
    [JsonPropertyName("supportedReasoningEfforts")]
    public IList<string>? SupportedReasoningEfforts { get; set; }

    /// <summary>Default reasoning effort level (only present if model supports reasoning effort).</summary>
    [JsonPropertyName("defaultReasoningEffort")]
    public string? DefaultReasoningEffort { get; set; }
}

/// <summary>RPC data type for ModelList operations.</summary>
public class ModelList
{
    /// <summary>List of available models with full metadata.</summary>
    [JsonPropertyName("models")]
    public List<Model> Models { get; set; }
}

/// <summary>RPC data type for Tool operations.</summary>
public class Tool
{
    /// <summary>Tool identifier (e.g., "bash", "grep", "str_replace_editor").</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP tools).</summary>
    [JsonPropertyName("namespacedName")]
    public string? NamespacedName { get; set; }

    /// <summary>Description of what the tool does.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    /// <summary>JSON Schema for the tool's input parameters.</summary>
    [JsonPropertyName("parameters")]
    public IDictionary<string, object>? Parameters { get; set; }

    /// <summary>Optional instructions for how to use this tool effectively.</summary>
    [JsonPropertyName("instructions")]
    public string? Instructions { get; set; }
}

/// <summary>RPC data type for ToolList operations.</summary>
public class ToolList
{
    /// <summary>List of available built-in tools with metadata.</summary>
    [JsonPropertyName("tools")]
    public List<Tool> Tools { get; set; }
}

/// <summary>RPC data type for ToolsList operations.</summary>
internal class ToolsListRequest
{
    /// <summary>Optional model ID — when provided, the returned tool list reflects model-specific overrides.</summary>
    [JsonPropertyName("model")]
    public string? Model { get; set; }
}

/// <summary>RPC data type for AccountQuotaSnapshot operations.</summary>
public class AccountQuotaSnapshot
{
    /// <summary>Number of requests included in the entitlement.</summary>
    [JsonPropertyName("entitlementRequests")]
    public long EntitlementRequests { get; set; }

    /// <summary>Number of requests used so far this period.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("usedRequests")]
    public long UsedRequests { get; set; }

    /// <summary>Percentage of entitlement remaining.</summary>
    [JsonPropertyName("remainingPercentage")]
    public double RemainingPercentage { get; set; }

    /// <summary>Number of overage requests made this period.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("overage")]
    public long Overage { get; set; }

    /// <summary>Whether pay-per-request usage is allowed when quota is exhausted.</summary>
    [JsonPropertyName("overageAllowedWithExhaustedQuota")]
    public bool OverageAllowedWithExhaustedQuota { get; set; }

    /// <summary>Date when the quota resets (ISO 8601).</summary>
    [JsonPropertyName("resetDate")]
    public DateTimeOffset? ResetDate { get; set; }
}

/// <summary>RPC data type for AccountQuota operations.</summary>
public class AccountQuota
{
    /// <summary>Quota snapshots keyed by type (e.g., chat, completions, premium_interactions).</summary>
    [JsonPropertyName("quotaSnapshots")]
    public IDictionary<string, AccountQuotaSnapshot> QuotaSnapshots { get => field ??= new Dictionary<string, AccountQuotaSnapshot>(); set; }
}

/// <summary>RPC data type for DiscoveredMcpServer operations.</summary>
public class DiscoveredMcpServer
{
    /// <summary>Server name (config key).</summary>
    [RegularExpression("^[0-9a-zA-Z_.@-]+(\\/[0-9a-zA-Z_.@-]+)*$")]
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Server transport type: stdio, http, sse, or memory (local configs are normalized to stdio).</summary>
    [JsonPropertyName("type")]
    public DiscoveredMcpServerType? Type { get; set; }

    /// <summary>Configuration source.</summary>
    [JsonPropertyName("source")]
    public DiscoveredMcpServerSource Source { get; set; }

    /// <summary>Whether the server is enabled (not in the disabled list).</summary>
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }
}

/// <summary>RPC data type for McpDiscover operations.</summary>
public class McpDiscoverResult
{
    /// <summary>MCP servers discovered from all sources.</summary>
    [JsonPropertyName("servers")]
    public List<DiscoveredMcpServer> Servers { get; set; }
}

/// <summary>RPC data type for McpDiscover operations.</summary>
internal class McpDiscoverRequest
{
    /// <summary>Working directory used as context for discovery (e.g., plugin resolution).</summary>
    [JsonPropertyName("workingDirectory")]
    public string? WorkingDirectory { get; set; }
}

/// <summary>RPC data type for SessionFsSetProvider operations.</summary>
public class SessionFsSetProviderResult
{
    /// <summary>Whether the provider was set successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for SessionFsSetProvider operations.</summary>
internal class SessionFsSetProviderRequest
{
    /// <summary>Initial working directory for sessions.</summary>
    [JsonPropertyName("initialCwd")]
    public string InitialCwd { get; set; } = string.Empty;

    /// <summary>Path within each session's SessionFs where the runtime stores files for that session.</summary>
    [JsonPropertyName("sessionStatePath")]
    public string SessionStatePath { get; set; } = string.Empty;

    /// <summary>Path conventions used by this filesystem.</summary>
    [JsonPropertyName("conventions")]
    public SessionFsSetProviderConventions Conventions { get; set; }
}

/// <summary>RPC data type for SessionsFork operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class SessionsForkResult
{
    /// <summary>The new forked session's ID.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionsFork operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionsForkRequest
{
    /// <summary>Source session ID to fork from.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Optional event ID boundary. When provided, the fork includes only events before this ID (exclusive). When omitted, all events are included.</summary>
    [JsonPropertyName("toEventId")]
    public string? ToEventId { get; set; }
}

/// <summary>RPC data type for Log operations.</summary>
public class LogResult
{
    /// <summary>The unique identifier of the emitted session event.</summary>
    [JsonPropertyName("eventId")]
    public Guid EventId { get; set; }
}

/// <summary>RPC data type for Log operations.</summary>
internal class LogRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Human-readable message.</summary>
    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    /// <summary>Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".</summary>
    [JsonPropertyName("level")]
    public SessionLogLevel? Level { get; set; }

    /// <summary>When true, the message is transient and not persisted to the session event log on disk.</summary>
    [JsonPropertyName("ephemeral")]
    public bool? Ephemeral { get; set; }

    /// <summary>Optional URL the user can open in their browser for more details.</summary>
    [Url]
    [StringSyntax(StringSyntaxAttribute.Uri)]
    [JsonPropertyName("url")]
    public string? Url { get; set; }
}

/// <summary>RPC data type for ModelCurrent operations.</summary>
public class ModelCurrent
{
    /// <summary>Currently active model identifier.</summary>
    [JsonPropertyName("modelId")]
    public string? ModelId { get; set; }
}

/// <summary>RPC data type for SessionModelGetCurrent operations.</summary>
internal class SessionModelGetCurrentRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for ModelSwitchTo operations.</summary>
public class ModelSwitchToResult
{
    /// <summary>Currently active model identifier after the switch.</summary>
    [JsonPropertyName("modelId")]
    public string? ModelId { get; set; }
}

/// <summary>Feature flags indicating what the model supports.</summary>
public class ModelCapabilitiesOverrideSupports
{
    /// <summary>Gets or sets the <c>vision</c> value.</summary>
    [JsonPropertyName("vision")]
    public bool? Vision { get; set; }

    /// <summary>Gets or sets the <c>reasoningEffort</c> value.</summary>
    [JsonPropertyName("reasoningEffort")]
    public bool? ReasoningEffort { get; set; }
}

/// <summary>RPC data type for ModelCapabilitiesOverrideLimitsVision operations.</summary>
public class ModelCapabilitiesOverrideLimitsVision
{
    /// <summary>MIME types the model accepts.</summary>
    [JsonPropertyName("supported_media_types")]
    public IList<string>? SupportedMediaTypes { get; set; }

    /// <summary>Maximum number of images per prompt.</summary>
    [Range(typeof(long), "1", "long.MaxValue")]
    [JsonPropertyName("max_prompt_images")]
    public long? MaxPromptImages { get; set; }

    /// <summary>Maximum image size in bytes.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("max_prompt_image_size")]
    public long? MaxPromptImageSize { get; set; }
}

/// <summary>Token limits for prompts, outputs, and context window.</summary>
public class ModelCapabilitiesOverrideLimits
{
    /// <summary>Gets or sets the <c>max_prompt_tokens</c> value.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("max_prompt_tokens")]
    public long? MaxPromptTokens { get; set; }

    /// <summary>Gets or sets the <c>max_output_tokens</c> value.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("max_output_tokens")]
    public long? MaxOutputTokens { get; set; }

    /// <summary>Maximum total context window size in tokens.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("max_context_window_tokens")]
    public long? MaxContextWindowTokens { get; set; }

    /// <summary>Gets or sets the <c>vision</c> value.</summary>
    [JsonPropertyName("vision")]
    public ModelCapabilitiesOverrideLimitsVision? Vision { get; set; }
}

/// <summary>Override individual model capabilities resolved by the runtime.</summary>
public class ModelCapabilitiesOverride
{
    /// <summary>Feature flags indicating what the model supports.</summary>
    [JsonPropertyName("supports")]
    public ModelCapabilitiesOverrideSupports? Supports { get; set; }

    /// <summary>Token limits for prompts, outputs, and context window.</summary>
    [JsonPropertyName("limits")]
    public ModelCapabilitiesOverrideLimits? Limits { get; set; }
}

/// <summary>RPC data type for ModelSwitchTo operations.</summary>
internal class ModelSwitchToRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Model identifier to switch to.</summary>
    [JsonPropertyName("modelId")]
    public string ModelId { get; set; } = string.Empty;

    /// <summary>Reasoning effort level to use for the model.</summary>
    [JsonPropertyName("reasoningEffort")]
    public string? ReasoningEffort { get; set; }

    /// <summary>Override individual model capabilities resolved by the runtime.</summary>
    [JsonPropertyName("modelCapabilities")]
    public ModelCapabilitiesOverride? ModelCapabilities { get; set; }
}

/// <summary>RPC data type for SessionModeGet operations.</summary>
internal class SessionModeGetRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for ModeSet operations.</summary>
internal class ModeSetRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>The agent mode. Valid values: "interactive", "plan", "autopilot".</summary>
    [JsonPropertyName("mode")]
    public SessionMode Mode { get; set; }
}

/// <summary>RPC data type for Plan operations.</summary>
public class Plan
{
    /// <summary>Whether the plan file exists in the workspace.</summary>
    [JsonPropertyName("exists")]
    public bool Exists { get; set; }

    /// <summary>The content of the plan file, or null if it does not exist.</summary>
    [JsonPropertyName("content")]
    public string? Content { get; set; }

    /// <summary>Absolute file path of the plan file, or null if workspace is not enabled.</summary>
    [JsonPropertyName("path")]
    public string? Path { get; set; }
}

/// <summary>RPC data type for SessionPlanRead operations.</summary>
internal class SessionPlanReadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for PlanUpdate operations.</summary>
public class PlanUpdateResult
{
}

/// <summary>RPC data type for PlanUpdate operations.</summary>
internal class PlanUpdateRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>The new content for the plan file.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;
}

/// <summary>RPC data type for PlanDelete operations.</summary>
public class PlanDelete
{
}

/// <summary>RPC data type for SessionPlanDelete operations.</summary>
internal class SessionPlanDeleteRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for WorkspaceFiles operations.</summary>
public class WorkspaceFiles
{
    /// <summary>Relative file paths in the workspace files directory.</summary>
    [JsonPropertyName("files")]
    public IList<string> Files { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionWorkspaceListFiles operations.</summary>
internal class SessionWorkspaceListFilesRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for WorkspaceReadFile operations.</summary>
public class WorkspaceReadFileResult
{
    /// <summary>File content as a UTF-8 string.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;
}

/// <summary>RPC data type for WorkspaceReadFile operations.</summary>
internal class WorkspaceReadFileRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Relative path within the workspace files directory.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;
}

/// <summary>RPC data type for WorkspaceCreateFile operations.</summary>
public class WorkspaceCreateFileResult
{
}

/// <summary>RPC data type for WorkspaceCreateFile operations.</summary>
internal class WorkspaceCreateFileRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Relative path within the workspace files directory.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>File content to write as a UTF-8 string.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;
}

/// <summary>RPC data type for FleetStart operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class FleetStartResult
{
    /// <summary>Whether fleet mode was successfully activated.</summary>
    [JsonPropertyName("started")]
    public bool Started { get; set; }
}

/// <summary>RPC data type for FleetStart operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class FleetStartRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Optional user prompt to combine with fleet instructions.</summary>
    [JsonPropertyName("prompt")]
    public string? Prompt { get; set; }
}

/// <summary>RPC data type for Agent operations.</summary>
public class Agent
{
    /// <summary>Unique identifier of the custom agent.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Human-readable display name.</summary>
    [JsonPropertyName("displayName")]
    public string DisplayName { get; set; } = string.Empty;

    /// <summary>Description of the agent's purpose.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;
}

/// <summary>RPC data type for AgentList operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class AgentList
{
    /// <summary>Available custom agents.</summary>
    [JsonPropertyName("agents")]
    public List<Agent> Agents { get; set; }
}

/// <summary>RPC data type for SessionAgentList operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionAgentListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for AgentCurrentAgent operations.</summary>
public class AgentCurrentAgent
{
    /// <summary>Unique identifier of the custom agent.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Human-readable display name.</summary>
    [JsonPropertyName("displayName")]
    public string DisplayName { get; set; } = string.Empty;

    /// <summary>Description of the agent's purpose.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;
}

/// <summary>RPC data type for AgentCurrent operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class AgentCurrent
{
    /// <summary>Currently selected custom agent, or null if using the default agent.</summary>
    [JsonPropertyName("agent")]
    public AgentCurrentAgent? Agent { get; set; }
}

/// <summary>RPC data type for SessionAgentGetCurrent operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionAgentGetCurrentRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>The newly selected custom agent.</summary>
public class AgentSelectAgent
{
    /// <summary>Unique identifier of the custom agent.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Human-readable display name.</summary>
    [JsonPropertyName("displayName")]
    public string DisplayName { get; set; } = string.Empty;

    /// <summary>Description of the agent's purpose.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;
}

/// <summary>RPC data type for AgentSelect operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class AgentSelectResult
{
    /// <summary>The newly selected custom agent.</summary>
    [JsonPropertyName("agent")]
    public AgentSelectAgent Agent { get => field ??= new(); set; }
}

/// <summary>RPC data type for AgentSelect operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class AgentSelectRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Name of the custom agent to select.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>RPC data type for AgentDeselect operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class AgentDeselect
{
}

/// <summary>RPC data type for SessionAgentDeselect operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionAgentDeselectRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for AgentReloadAgent operations.</summary>
public class AgentReloadAgent
{
    /// <summary>Unique identifier of the custom agent.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Human-readable display name.</summary>
    [JsonPropertyName("displayName")]
    public string DisplayName { get; set; } = string.Empty;

    /// <summary>Description of the agent's purpose.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;
}

/// <summary>RPC data type for AgentReload operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class AgentReload
{
    /// <summary>Reloaded custom agents.</summary>
    [JsonPropertyName("agents")]
    public List<AgentReloadAgent> Agents { get; set; }
}

/// <summary>RPC data type for SessionAgentReload operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionAgentReloadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for Skill operations.</summary>
public class Skill
{
    /// <summary>Unique identifier for the skill.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Description of what the skill does.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    /// <summary>Source location type (e.g., project, personal, plugin).</summary>
    [JsonPropertyName("source")]
    public string Source { get; set; } = string.Empty;

    /// <summary>Whether the skill can be invoked by the user as a slash command.</summary>
    [JsonPropertyName("userInvocable")]
    public bool UserInvocable { get; set; }

    /// <summary>Whether the skill is currently enabled.</summary>
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }

    /// <summary>Absolute path to the skill file.</summary>
    [JsonPropertyName("path")]
    public string? Path { get; set; }
}

/// <summary>RPC data type for SkillList operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class SkillList
{
    /// <summary>Available skills.</summary>
    [JsonPropertyName("skills")]
    public List<Skill> Skills { get; set; }
}

/// <summary>RPC data type for SessionSkillsList operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionSkillsListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SkillsEnable operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class SkillsEnableResult
{
}

/// <summary>RPC data type for SkillsEnable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SkillsEnableRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Name of the skill to enable.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>RPC data type for SkillsDisable operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class SkillsDisableResult
{
}

/// <summary>RPC data type for SkillsDisable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SkillsDisableRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Name of the skill to disable.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>RPC data type for SkillsReload operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class SkillsReload
{
}

/// <summary>RPC data type for SessionSkillsReload operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionSkillsReloadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for McpServer operations.</summary>
public class McpServer
{
    /// <summary>Server name (config key).</summary>
    [RegularExpression("^[0-9a-zA-Z_.@-]+(\\/[0-9a-zA-Z_.@-]+)*$")]
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Connection status: connected, failed, needs-auth, pending, disabled, or not_configured.</summary>
    [JsonPropertyName("status")]
    public McpServerStatus Status { get; set; }

    /// <summary>Configuration source: user, workspace, plugin, or builtin.</summary>
    [JsonPropertyName("source")]
    public DiscoveredMcpServerSource? Source { get; set; }

    /// <summary>Error message if the server failed to connect.</summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }
}

/// <summary>RPC data type for McpList operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class McpList
{
    /// <summary>Configured MCP servers.</summary>
    [JsonPropertyName("servers")]
    public List<McpServer> Servers { get; set; }
}

/// <summary>RPC data type for SessionMcpList operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionMcpListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for McpEnable operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class McpEnableResult
{
}

/// <summary>RPC data type for McpEnable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class McpEnableRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Name of the MCP server to enable.</summary>
    [RegularExpression("^[0-9a-zA-Z_.@-]+(\\/[0-9a-zA-Z_.@-]+)*$")]
    [JsonPropertyName("serverName")]
    public string ServerName { get; set; } = string.Empty;
}

/// <summary>RPC data type for McpDisable operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class McpDisableResult
{
}

/// <summary>RPC data type for McpDisable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class McpDisableRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Name of the MCP server to disable.</summary>
    [RegularExpression("^[0-9a-zA-Z_.@-]+(\\/[0-9a-zA-Z_.@-]+)*$")]
    [JsonPropertyName("serverName")]
    public string ServerName { get; set; } = string.Empty;
}

/// <summary>RPC data type for McpReload operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class McpReload
{
}

/// <summary>RPC data type for SessionMcpReload operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionMcpReloadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for Plugin operations.</summary>
public class Plugin
{
    /// <summary>Plugin name.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Marketplace the plugin came from.</summary>
    [JsonPropertyName("marketplace")]
    public string Marketplace { get; set; } = string.Empty;

    /// <summary>Installed version.</summary>
    [JsonPropertyName("version")]
    public string? Version { get; set; }

    /// <summary>Whether the plugin is currently enabled.</summary>
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }
}

/// <summary>RPC data type for PluginList operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class PluginList
{
    /// <summary>Installed plugins.</summary>
    [JsonPropertyName("plugins")]
    public List<Plugin> Plugins { get; set; }
}

/// <summary>RPC data type for SessionPluginsList operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionPluginsListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for Extension operations.</summary>
public class Extension
{
    /// <summary>Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper').</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Extension name (directory name).</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/).</summary>
    [JsonPropertyName("source")]
    public ExtensionSource Source { get; set; }

    /// <summary>Current status: running, disabled, failed, or starting.</summary>
    [JsonPropertyName("status")]
    public ExtensionStatus Status { get; set; }

    /// <summary>Process ID if the extension is running.</summary>
    [JsonPropertyName("pid")]
    public long? Pid { get; set; }
}

/// <summary>RPC data type for ExtensionList operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class ExtensionList
{
    /// <summary>Discovered extensions and their current status.</summary>
    [JsonPropertyName("extensions")]
    public List<Extension> Extensions { get; set; }
}

/// <summary>RPC data type for SessionExtensionsList operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionExtensionsListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for ExtensionsEnable operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class ExtensionsEnableResult
{
}

/// <summary>RPC data type for ExtensionsEnable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class ExtensionsEnableRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Source-qualified extension ID to enable.</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;
}

/// <summary>RPC data type for ExtensionsDisable operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class ExtensionsDisableResult
{
}

/// <summary>RPC data type for ExtensionsDisable operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class ExtensionsDisableRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Source-qualified extension ID to disable.</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;
}

/// <summary>RPC data type for ExtensionsReload operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class ExtensionsReload
{
}

/// <summary>RPC data type for SessionExtensionsReload operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionExtensionsReloadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for HandleToolCall operations.</summary>
public class HandleToolCallResult
{
    /// <summary>Whether the tool call result was handled successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for ToolsHandlePendingToolCall operations.</summary>
internal class ToolsHandlePendingToolCallRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Request ID of the pending tool call.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Tool call result (string or expanded result object).</summary>
    [JsonPropertyName("result")]
    public object? Result { get; set; }

    /// <summary>Error message if the tool call failed.</summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }
}

/// <summary>RPC data type for CommandsHandlePendingCommand operations.</summary>
public class CommandsHandlePendingCommandResult
{
    /// <summary>Whether the command was handled successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for CommandsHandlePendingCommand operations.</summary>
internal class CommandsHandlePendingCommandRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Request ID from the command invocation event.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Error message if the command handler failed.</summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }
}

/// <summary>The elicitation response (accept with form values, decline, or cancel).</summary>
public class UiElicitationResponse
{
    /// <summary>The user's response: accept (submitted), decline (rejected), or cancel (dismissed).</summary>
    [JsonPropertyName("action")]
    public ElicitationResponseAction Action { get; set; }

    /// <summary>The form values submitted by the user (present when action is 'accept').</summary>
    [JsonPropertyName("content")]
    public IDictionary<string, object>? Content { get; set; }
}

/// <summary>JSON Schema describing the form fields to present to the user.</summary>
public class UiElicitationSchema
{
    /// <summary>Schema type indicator (always 'object').</summary>
    [JsonPropertyName("type")]
    public string Type { get; set; } = string.Empty;

    /// <summary>Form field definitions, keyed by field name.</summary>
    [JsonPropertyName("properties")]
    public IDictionary<string, object> Properties { get => field ??= new Dictionary<string, object>(); set; }

    /// <summary>List of required field names.</summary>
    [JsonPropertyName("required")]
    public IList<string>? Required { get; set; }
}

/// <summary>RPC data type for UiElicitation operations.</summary>
internal class UiElicitationRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Message describing what information is needed from the user.</summary>
    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    /// <summary>JSON Schema describing the form fields to present to the user.</summary>
    [JsonPropertyName("requestedSchema")]
    public UiElicitationSchema RequestedSchema { get => field ??= new(); set; }
}

/// <summary>RPC data type for UiElicitation operations.</summary>
public class UiElicitationResult
{
    /// <summary>Whether the response was accepted. False if the request was already resolved by another client.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for HandlePendingElicitation operations.</summary>
internal class HandlePendingElicitationRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>The unique request ID from the elicitation.requested event.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>The elicitation response (accept with form values, decline, or cancel).</summary>
    [JsonPropertyName("result")]
    public UiElicitationResponse Result { get => field ??= new(); set; }
}

/// <summary>RPC data type for PermissionRequest operations.</summary>
public class PermissionRequestResult
{
    /// <summary>Whether the permission request was handled successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for PermissionDecision operations.</summary>
internal class PermissionDecisionRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Request ID of the pending permission request.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Gets or sets the <c>result</c> value.</summary>
    [JsonPropertyName("result")]
    public object Result { get; set; } = null!;
}

/// <summary>RPC data type for ShellExec operations.</summary>
public class ShellExecResult
{
    /// <summary>Unique identifier for tracking streamed output.</summary>
    [JsonPropertyName("processId")]
    public string ProcessId { get; set; } = string.Empty;
}

/// <summary>RPC data type for ShellExec operations.</summary>
internal class ShellExecRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Shell command to execute.</summary>
    [JsonPropertyName("command")]
    public string Command { get; set; } = string.Empty;

    /// <summary>Working directory (defaults to session working directory).</summary>
    [JsonPropertyName("cwd")]
    public string? Cwd { get; set; }

    /// <summary>Timeout in milliseconds (default: 30000).</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]
    [JsonPropertyName("timeout")]
    public TimeSpan? Timeout { get; set; }
}

/// <summary>RPC data type for ShellKill operations.</summary>
public class ShellKillResult
{
    /// <summary>Whether the signal was sent successfully.</summary>
    [JsonPropertyName("killed")]
    public bool Killed { get; set; }
}

/// <summary>RPC data type for ShellKill operations.</summary>
internal class ShellKillRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Process identifier returned by shell.exec.</summary>
    [JsonPropertyName("processId")]
    public string ProcessId { get; set; } = string.Empty;

    /// <summary>Signal to send (default: SIGTERM).</summary>
    [JsonPropertyName("signal")]
    public ShellKillSignal? Signal { get; set; }
}

/// <summary>Post-compaction context window usage breakdown.</summary>
public class HistoryCompactContextWindow
{
    /// <summary>Maximum token count for the model's context window.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("tokenLimit")]
    public long TokenLimit { get; set; }

    /// <summary>Current total tokens in the context window (system + conversation + tool definitions).</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("currentTokens")]
    public long CurrentTokens { get; set; }

    /// <summary>Current number of messages in the conversation.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("messagesLength")]
    public long MessagesLength { get; set; }

    /// <summary>Token count from system message(s).</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("systemTokens")]
    public long? SystemTokens { get; set; }

    /// <summary>Token count from non-system messages (user, assistant, tool).</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("conversationTokens")]
    public long? ConversationTokens { get; set; }

    /// <summary>Token count from tool definitions.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("toolDefinitionsTokens")]
    public long? ToolDefinitionsTokens { get; set; }
}

/// <summary>RPC data type for HistoryCompact operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class HistoryCompact
{
    /// <summary>Whether compaction completed successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }

    /// <summary>Number of tokens freed by compaction.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("tokensRemoved")]
    public long TokensRemoved { get; set; }

    /// <summary>Number of messages removed during compaction.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("messagesRemoved")]
    public long MessagesRemoved { get; set; }

    /// <summary>Post-compaction context window usage breakdown.</summary>
    [JsonPropertyName("contextWindow")]
    public HistoryCompactContextWindow? ContextWindow { get; set; }
}

/// <summary>RPC data type for SessionHistoryCompact operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionHistoryCompactRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for HistoryTruncate operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class HistoryTruncateResult
{
    /// <summary>Number of events that were removed.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("eventsRemoved")]
    public long EventsRemoved { get; set; }
}

/// <summary>RPC data type for HistoryTruncate operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class HistoryTruncateRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Event ID to truncate to. This event and all events after it are removed from the session.</summary>
    [JsonPropertyName("eventId")]
    public string EventId { get; set; } = string.Empty;
}

/// <summary>Aggregated code change metrics.</summary>
public class UsageMetricsCodeChanges
{
    /// <summary>Total lines of code added.</summary>
    [JsonPropertyName("linesAdded")]
    public long LinesAdded { get; set; }

    /// <summary>Total lines of code removed.</summary>
    [JsonPropertyName("linesRemoved")]
    public long LinesRemoved { get; set; }

    /// <summary>Number of distinct files modified.</summary>
    [JsonPropertyName("filesModifiedCount")]
    public long FilesModifiedCount { get; set; }
}

/// <summary>Request count and cost metrics for this model.</summary>
public class UsageMetricsModelMetricRequests
{
    /// <summary>Number of API requests made with this model.</summary>
    [JsonPropertyName("count")]
    public long Count { get; set; }

    /// <summary>User-initiated premium request cost (with multiplier applied).</summary>
    [JsonPropertyName("cost")]
    public double Cost { get; set; }
}

/// <summary>Token usage metrics for this model.</summary>
public class UsageMetricsModelMetricUsage
{
    /// <summary>Total input tokens consumed.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("inputTokens")]
    public long InputTokens { get; set; }

    /// <summary>Total output tokens produced.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("outputTokens")]
    public long OutputTokens { get; set; }

    /// <summary>Total tokens read from prompt cache.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("cacheReadTokens")]
    public long CacheReadTokens { get; set; }

    /// <summary>Total tokens written to prompt cache.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("cacheWriteTokens")]
    public long CacheWriteTokens { get; set; }

    /// <summary>Total output tokens used for reasoning.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("reasoningTokens")]
    public long? ReasoningTokens { get; set; }
}

/// <summary>RPC data type for UsageMetricsModelMetric operations.</summary>
public class UsageMetricsModelMetric
{
    /// <summary>Request count and cost metrics for this model.</summary>
    [JsonPropertyName("requests")]
    public UsageMetricsModelMetricRequests Requests { get => field ??= new(); set; }

    /// <summary>Token usage metrics for this model.</summary>
    [JsonPropertyName("usage")]
    public UsageMetricsModelMetricUsage Usage { get => field ??= new(); set; }
}

/// <summary>RPC data type for UsageMetrics operations.</summary>
[Experimental(Diagnostics.Experimental)]
public class UsageMetrics
{
    /// <summary>Total user-initiated premium request cost across all models (may be fractional due to multipliers).</summary>
    [JsonPropertyName("totalPremiumRequestCost")]
    public double TotalPremiumRequestCost { get; set; }

    /// <summary>Raw count of user-initiated API requests.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("totalUserRequests")]
    public long TotalUserRequests { get; set; }

    /// <summary>Total time spent in model API calls (milliseconds).</summary>
    [Range(0, double.MaxValue)]
    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]
    [JsonPropertyName("totalApiDurationMs")]
    public TimeSpan TotalApiDurationMs { get; set; }

    /// <summary>Session start timestamp (epoch milliseconds).</summary>
    [JsonPropertyName("sessionStartTime")]
    public long SessionStartTime { get; set; }

    /// <summary>Aggregated code change metrics.</summary>
    [JsonPropertyName("codeChanges")]
    public UsageMetricsCodeChanges CodeChanges { get => field ??= new(); set; }

    /// <summary>Per-model token and request metrics, keyed by model identifier.</summary>
    [JsonPropertyName("modelMetrics")]
    public IDictionary<string, UsageMetricsModelMetric> ModelMetrics { get => field ??= new Dictionary<string, UsageMetricsModelMetric>(); set; }

    /// <summary>Currently active model identifier.</summary>
    [JsonPropertyName("currentModel")]
    public string? CurrentModel { get; set; }

    /// <summary>Input tokens from the most recent main-agent API call.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("lastCallInputTokens")]
    public long LastCallInputTokens { get; set; }

    /// <summary>Output tokens from the most recent main-agent API call.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("lastCallOutputTokens")]
    public long LastCallOutputTokens { get; set; }
}

/// <summary>RPC data type for SessionUsageGetMetrics operations.</summary>
[Experimental(Diagnostics.Experimental)]
internal class SessionUsageGetMetricsRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsReadFile operations.</summary>
public class SessionFsReadFileResult
{
    /// <summary>File content as UTF-8 string.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsReadFile operations.</summary>
public class SessionFsReadFileRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsWriteFile operations.</summary>
public class SessionFsWriteFileRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Content to write.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Optional POSIX-style mode for newly created files.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("mode")]
    public long? Mode { get; set; }
}

/// <summary>RPC data type for SessionFsAppendFile operations.</summary>
public class SessionFsAppendFileRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Content to append.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Optional POSIX-style mode for newly created files.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("mode")]
    public long? Mode { get; set; }
}

/// <summary>RPC data type for SessionFsExists operations.</summary>
public class SessionFsExistsResult
{
    /// <summary>Whether the path exists.</summary>
    [JsonPropertyName("exists")]
    public bool Exists { get; set; }
}

/// <summary>RPC data type for SessionFsExists operations.</summary>
public class SessionFsExistsRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsStat operations.</summary>
public class SessionFsStatResult
{
    /// <summary>Whether the path is a file.</summary>
    [JsonPropertyName("isFile")]
    public bool IsFile { get; set; }

    /// <summary>Whether the path is a directory.</summary>
    [JsonPropertyName("isDirectory")]
    public bool IsDirectory { get; set; }

    /// <summary>File size in bytes.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("size")]
    public long Size { get; set; }

    /// <summary>ISO 8601 timestamp of last modification.</summary>
    [JsonPropertyName("mtime")]
    public DateTimeOffset Mtime { get; set; }

    /// <summary>ISO 8601 timestamp of creation.</summary>
    [JsonPropertyName("birthtime")]
    public DateTimeOffset Birthtime { get; set; }
}

/// <summary>RPC data type for SessionFsStat operations.</summary>
public class SessionFsStatRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsMkdir operations.</summary>
public class SessionFsMkdirRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Create parent directories as needed.</summary>
    [JsonPropertyName("recursive")]
    public bool? Recursive { get; set; }

    /// <summary>Optional POSIX-style mode for newly created directories.</summary>
    [Range(typeof(long), "0", "long.MaxValue")]
    [JsonPropertyName("mode")]
    public long? Mode { get; set; }
}

/// <summary>RPC data type for SessionFsReaddir operations.</summary>
public class SessionFsReaddirResult
{
    /// <summary>Entry names in the directory.</summary>
    [JsonPropertyName("entries")]
    public IList<string> Entries { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionFsReaddir operations.</summary>
public class SessionFsReaddirRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsReaddirWithTypesEntry operations.</summary>
public class SessionFsReaddirWithTypesEntry
{
    /// <summary>Entry name.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Entry type.</summary>
    [JsonPropertyName("type")]
    public SessionFsReaddirWithTypesEntryType Type { get; set; }
}

/// <summary>RPC data type for SessionFsReaddirWithTypes operations.</summary>
public class SessionFsReaddirWithTypesResult
{
    /// <summary>Directory entries with type information.</summary>
    [JsonPropertyName("entries")]
    public List<SessionFsReaddirWithTypesEntry> Entries { get; set; }
}

/// <summary>RPC data type for SessionFsReaddirWithTypes operations.</summary>
public class SessionFsReaddirWithTypesRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionFsRm operations.</summary>
public class SessionFsRmRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Remove directories and their contents recursively.</summary>
    [JsonPropertyName("recursive")]
    public bool? Recursive { get; set; }

    /// <summary>Ignore errors if the path does not exist.</summary>
    [JsonPropertyName("force")]
    public bool? Force { get; set; }
}

/// <summary>RPC data type for SessionFsRename operations.</summary>
public class SessionFsRenameRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Source path using SessionFs conventions.</summary>
    [JsonPropertyName("src")]
    public string Src { get; set; } = string.Empty;

    /// <summary>Destination path using SessionFs conventions.</summary>
    [JsonPropertyName("dest")]
    public string Dest { get; set; } = string.Empty;
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
[JsonConverter(typeof(JsonStringEnumConverter<ElicitationResponseAction>))]
public enum ElicitationResponseAction
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
public class ServerRpc
{
    private readonly JsonRpc _rpc;

    internal ServerRpc(JsonRpc rpc)
    {
        _rpc = rpc;
        Models = new ServerModelsApi(rpc);
        Tools = new ServerToolsApi(rpc);
        Account = new ServerAccountApi(rpc);
        Mcp = new ServerMcpApi(rpc);
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

    /// <summary>SessionFs APIs.</summary>
    public ServerSessionFsApi SessionFs { get; }

    /// <summary>Sessions APIs.</summary>
    public ServerSessionsApi Sessions { get; }
}

/// <summary>Provides server-scoped Models APIs.</summary>
public class ServerModelsApi
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
public class ServerToolsApi
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
public class ServerAccountApi
{
    private readonly JsonRpc _rpc;

    internal ServerAccountApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Calls "account.getQuota".</summary>
    public async Task<AccountQuota> GetQuotaAsync(CancellationToken cancellationToken = default)
    {
        return await CopilotClient.InvokeRpcAsync<AccountQuota>(_rpc, "account.getQuota", [], cancellationToken);
    }
}

/// <summary>Provides server-scoped Mcp APIs.</summary>
public class ServerMcpApi
{
    private readonly JsonRpc _rpc;

    internal ServerMcpApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Calls "mcp.discover".</summary>
    public async Task<McpDiscoverResult> DiscoverAsync(string? workingDirectory = null, CancellationToken cancellationToken = default)
    {
        var request = new McpDiscoverRequest { WorkingDirectory = workingDirectory };
        return await CopilotClient.InvokeRpcAsync<McpDiscoverResult>(_rpc, "mcp.discover", [request], cancellationToken);
    }
}

/// <summary>Provides server-scoped SessionFs APIs.</summary>
public class ServerSessionFsApi
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
public class ServerSessionsApi
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
public class SessionRpc
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal SessionRpc(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
        Model = new ModelApi(rpc, sessionId);
        Mode = new ModeApi(rpc, sessionId);
        Plan = new PlanApi(rpc, sessionId);
        Workspace = new WorkspaceApi(rpc, sessionId);
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

    /// <summary>Plan APIs.</summary>
    public PlanApi Plan { get; }

    /// <summary>Workspace APIs.</summary>
    public WorkspaceApi Workspace { get; }

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
public class ModelApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal ModelApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.model.getCurrent".</summary>
    public async Task<ModelCurrent> GetCurrentAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionModelGetCurrentRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<ModelCurrent>(_rpc, "session.model.getCurrent", [request], cancellationToken);
    }

    /// <summary>Calls "session.model.switchTo".</summary>
    public async Task<ModelSwitchToResult> SwitchToAsync(string modelId, string? reasoningEffort = null, ModelCapabilitiesOverride? modelCapabilities = null, CancellationToken cancellationToken = default)
    {
        var request = new ModelSwitchToRequest { SessionId = _sessionId, ModelId = modelId, ReasoningEffort = reasoningEffort, ModelCapabilities = modelCapabilities };
        return await CopilotClient.InvokeRpcAsync<ModelSwitchToResult>(_rpc, "session.model.switchTo", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Mode APIs.</summary>
public class ModeApi
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

/// <summary>Provides session-scoped Plan APIs.</summary>
public class PlanApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal PlanApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.plan.read".</summary>
    public async Task<Plan> ReadAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionPlanReadRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<Plan>(_rpc, "session.plan.read", [request], cancellationToken);
    }

    /// <summary>Calls "session.plan.update".</summary>
    public async Task<PlanUpdateResult> UpdateAsync(string content, CancellationToken cancellationToken = default)
    {
        var request = new PlanUpdateRequest { SessionId = _sessionId, Content = content };
        return await CopilotClient.InvokeRpcAsync<PlanUpdateResult>(_rpc, "session.plan.update", [request], cancellationToken);
    }

    /// <summary>Calls "session.plan.delete".</summary>
    public async Task<PlanDelete> DeleteAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionPlanDeleteRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<PlanDelete>(_rpc, "session.plan.delete", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Workspace APIs.</summary>
public class WorkspaceApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal WorkspaceApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.workspace.listFiles".</summary>
    public async Task<WorkspaceFiles> ListFilesAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionWorkspaceListFilesRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<WorkspaceFiles>(_rpc, "session.workspace.listFiles", [request], cancellationToken);
    }

    /// <summary>Calls "session.workspace.readFile".</summary>
    public async Task<WorkspaceReadFileResult> ReadFileAsync(string path, CancellationToken cancellationToken = default)
    {
        var request = new WorkspaceReadFileRequest { SessionId = _sessionId, Path = path };
        return await CopilotClient.InvokeRpcAsync<WorkspaceReadFileResult>(_rpc, "session.workspace.readFile", [request], cancellationToken);
    }

    /// <summary>Calls "session.workspace.createFile".</summary>
    public async Task<WorkspaceCreateFileResult> CreateFileAsync(string path, string content, CancellationToken cancellationToken = default)
    {
        var request = new WorkspaceCreateFileRequest { SessionId = _sessionId, Path = path, Content = content };
        return await CopilotClient.InvokeRpcAsync<WorkspaceCreateFileResult>(_rpc, "session.workspace.createFile", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Fleet APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public class FleetApi
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
public class AgentApi
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
    public async Task<AgentCurrent> GetCurrentAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionAgentGetCurrentRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<AgentCurrent>(_rpc, "session.agent.getCurrent", [request], cancellationToken);
    }

    /// <summary>Calls "session.agent.select".</summary>
    public async Task<AgentSelectResult> SelectAsync(string name, CancellationToken cancellationToken = default)
    {
        var request = new AgentSelectRequest { SessionId = _sessionId, Name = name };
        return await CopilotClient.InvokeRpcAsync<AgentSelectResult>(_rpc, "session.agent.select", [request], cancellationToken);
    }

    /// <summary>Calls "session.agent.deselect".</summary>
    public async Task<AgentDeselect> DeselectAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionAgentDeselectRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<AgentDeselect>(_rpc, "session.agent.deselect", [request], cancellationToken);
    }

    /// <summary>Calls "session.agent.reload".</summary>
    public async Task<AgentReload> ReloadAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionAgentReloadRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<AgentReload>(_rpc, "session.agent.reload", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Skills APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public class SkillsApi
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
    public async Task<SkillsEnableResult> EnableAsync(string name, CancellationToken cancellationToken = default)
    {
        var request = new SkillsEnableRequest { SessionId = _sessionId, Name = name };
        return await CopilotClient.InvokeRpcAsync<SkillsEnableResult>(_rpc, "session.skills.enable", [request], cancellationToken);
    }

    /// <summary>Calls "session.skills.disable".</summary>
    public async Task<SkillsDisableResult> DisableAsync(string name, CancellationToken cancellationToken = default)
    {
        var request = new SkillsDisableRequest { SessionId = _sessionId, Name = name };
        return await CopilotClient.InvokeRpcAsync<SkillsDisableResult>(_rpc, "session.skills.disable", [request], cancellationToken);
    }

    /// <summary>Calls "session.skills.reload".</summary>
    public async Task<SkillsReload> ReloadAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionSkillsReloadRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SkillsReload>(_rpc, "session.skills.reload", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Mcp APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public class McpApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal McpApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.mcp.list".</summary>
    public async Task<McpList> ListAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionMcpListRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<McpList>(_rpc, "session.mcp.list", [request], cancellationToken);
    }

    /// <summary>Calls "session.mcp.enable".</summary>
    public async Task<McpEnableResult> EnableAsync(string serverName, CancellationToken cancellationToken = default)
    {
        var request = new McpEnableRequest { SessionId = _sessionId, ServerName = serverName };
        return await CopilotClient.InvokeRpcAsync<McpEnableResult>(_rpc, "session.mcp.enable", [request], cancellationToken);
    }

    /// <summary>Calls "session.mcp.disable".</summary>
    public async Task<McpDisableResult> DisableAsync(string serverName, CancellationToken cancellationToken = default)
    {
        var request = new McpDisableRequest { SessionId = _sessionId, ServerName = serverName };
        return await CopilotClient.InvokeRpcAsync<McpDisableResult>(_rpc, "session.mcp.disable", [request], cancellationToken);
    }

    /// <summary>Calls "session.mcp.reload".</summary>
    public async Task<McpReload> ReloadAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionMcpReloadRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<McpReload>(_rpc, "session.mcp.reload", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Plugins APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public class PluginsApi
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
public class ExtensionsApi
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
    public async Task<ExtensionsEnableResult> EnableAsync(string id, CancellationToken cancellationToken = default)
    {
        var request = new ExtensionsEnableRequest { SessionId = _sessionId, Id = id };
        return await CopilotClient.InvokeRpcAsync<ExtensionsEnableResult>(_rpc, "session.extensions.enable", [request], cancellationToken);
    }

    /// <summary>Calls "session.extensions.disable".</summary>
    public async Task<ExtensionsDisableResult> DisableAsync(string id, CancellationToken cancellationToken = default)
    {
        var request = new ExtensionsDisableRequest { SessionId = _sessionId, Id = id };
        return await CopilotClient.InvokeRpcAsync<ExtensionsDisableResult>(_rpc, "session.extensions.disable", [request], cancellationToken);
    }

    /// <summary>Calls "session.extensions.reload".</summary>
    public async Task<ExtensionsReload> ReloadAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionExtensionsReloadRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<ExtensionsReload>(_rpc, "session.extensions.reload", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Tools APIs.</summary>
public class ToolsApi
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
public class CommandsApi
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
public class UiApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal UiApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.ui.elicitation".</summary>
    public async Task<UiElicitationResponse> ElicitationAsync(string message, UiElicitationSchema requestedSchema, CancellationToken cancellationToken = default)
    {
        var request = new UiElicitationRequest { SessionId = _sessionId, Message = message, RequestedSchema = requestedSchema };
        return await CopilotClient.InvokeRpcAsync<UiElicitationResponse>(_rpc, "session.ui.elicitation", [request], cancellationToken);
    }

    /// <summary>Calls "session.ui.handlePendingElicitation".</summary>
    public async Task<UiElicitationResult> HandlePendingElicitationAsync(string requestId, UiElicitationResponse result, CancellationToken cancellationToken = default)
    {
        var request = new HandlePendingElicitationRequest { SessionId = _sessionId, RequestId = requestId, Result = result };
        return await CopilotClient.InvokeRpcAsync<UiElicitationResult>(_rpc, "session.ui.handlePendingElicitation", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Permissions APIs.</summary>
public class PermissionsApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal PermissionsApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.permissions.handlePendingPermissionRequest".</summary>
    public async Task<PermissionRequestResult> HandlePendingPermissionRequestAsync(string requestId, object result, CancellationToken cancellationToken = default)
    {
        var request = new PermissionDecisionRequest { SessionId = _sessionId, RequestId = requestId, Result = result };
        return await CopilotClient.InvokeRpcAsync<PermissionRequestResult>(_rpc, "session.permissions.handlePendingPermissionRequest", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Shell APIs.</summary>
public class ShellApi
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
public class HistoryApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal HistoryApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.history.compact".</summary>
    public async Task<HistoryCompact> CompactAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionHistoryCompactRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<HistoryCompact>(_rpc, "session.history.compact", [request], cancellationToken);
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
public class UsageApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal UsageApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.usage.getMetrics".</summary>
    public async Task<UsageMetrics> GetMetricsAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionUsageGetMetricsRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<UsageMetrics>(_rpc, "session.usage.getMetrics", [request], cancellationToken);
    }
}

/// <summary>Handles `sessionFs` client session API methods.</summary>
public interface ISessionFsHandler
{
    /// <summary>Handles "sessionFs.readFile".</summary>
    Task<SessionFsReadFileResult> ReadFileAsync(SessionFsReadFileRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.writeFile".</summary>
    Task WriteFileAsync(SessionFsWriteFileRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.appendFile".</summary>
    Task AppendFileAsync(SessionFsAppendFileRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.exists".</summary>
    Task<SessionFsExistsResult> ExistsAsync(SessionFsExistsRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.stat".</summary>
    Task<SessionFsStatResult> StatAsync(SessionFsStatRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.mkdir".</summary>
    Task MkdirAsync(SessionFsMkdirRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.readdir".</summary>
    Task<SessionFsReaddirResult> ReaddirAsync(SessionFsReaddirRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.readdirWithTypes".</summary>
    Task<SessionFsReaddirWithTypesResult> ReaddirWithTypesAsync(SessionFsReaddirWithTypesRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.rm".</summary>
    Task RmAsync(SessionFsRmRequest request, CancellationToken cancellationToken = default);
    /// <summary>Handles "sessionFs.rename".</summary>
    Task RenameAsync(SessionFsRenameRequest request, CancellationToken cancellationToken = default);
}

/// <summary>Provides all client session API handler groups for a session.</summary>
public class ClientSessionApiHandlers
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
        var registerSessionFsWriteFileMethod = (Func<SessionFsWriteFileRequest, CancellationToken, Task>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            await handler.WriteFileAsync(request, cancellationToken);
        });
        rpc.AddLocalRpcMethod(registerSessionFsWriteFileMethod.Method, registerSessionFsWriteFileMethod.Target!, new JsonRpcMethodAttribute("sessionFs.writeFile")
        {
            UseSingleObjectParameterDeserialization = true
        });
        var registerSessionFsAppendFileMethod = (Func<SessionFsAppendFileRequest, CancellationToken, Task>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            await handler.AppendFileAsync(request, cancellationToken);
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
        var registerSessionFsMkdirMethod = (Func<SessionFsMkdirRequest, CancellationToken, Task>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            await handler.MkdirAsync(request, cancellationToken);
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
        var registerSessionFsRmMethod = (Func<SessionFsRmRequest, CancellationToken, Task>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            await handler.RmAsync(request, cancellationToken);
        });
        rpc.AddLocalRpcMethod(registerSessionFsRmMethod.Method, registerSessionFsRmMethod.Target!, new JsonRpcMethodAttribute("sessionFs.rm")
        {
            UseSingleObjectParameterDeserialization = true
        });
        var registerSessionFsRenameMethod = (Func<SessionFsRenameRequest, CancellationToken, Task>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            await handler.RenameAsync(request, cancellationToken);
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
[JsonSerializable(typeof(AccountQuota))]
[JsonSerializable(typeof(AccountQuotaSnapshot))]
[JsonSerializable(typeof(Agent))]
[JsonSerializable(typeof(AgentCurrent))]
[JsonSerializable(typeof(AgentCurrentAgent))]
[JsonSerializable(typeof(AgentDeselect))]
[JsonSerializable(typeof(AgentList))]
[JsonSerializable(typeof(AgentReload))]
[JsonSerializable(typeof(AgentReloadAgent))]
[JsonSerializable(typeof(AgentSelectAgent))]
[JsonSerializable(typeof(AgentSelectRequest))]
[JsonSerializable(typeof(AgentSelectResult))]
[JsonSerializable(typeof(CommandsHandlePendingCommandRequest))]
[JsonSerializable(typeof(CommandsHandlePendingCommandResult))]
[JsonSerializable(typeof(DiscoveredMcpServer))]
[JsonSerializable(typeof(Extension))]
[JsonSerializable(typeof(ExtensionList))]
[JsonSerializable(typeof(ExtensionsDisableRequest))]
[JsonSerializable(typeof(ExtensionsDisableResult))]
[JsonSerializable(typeof(ExtensionsEnableRequest))]
[JsonSerializable(typeof(ExtensionsEnableResult))]
[JsonSerializable(typeof(ExtensionsReload))]
[JsonSerializable(typeof(FleetStartRequest))]
[JsonSerializable(typeof(FleetStartResult))]
[JsonSerializable(typeof(HandlePendingElicitationRequest))]
[JsonSerializable(typeof(HandleToolCallResult))]
[JsonSerializable(typeof(HistoryCompact))]
[JsonSerializable(typeof(HistoryCompactContextWindow))]
[JsonSerializable(typeof(HistoryTruncateRequest))]
[JsonSerializable(typeof(HistoryTruncateResult))]
[JsonSerializable(typeof(LogRequest))]
[JsonSerializable(typeof(LogResult))]
[JsonSerializable(typeof(McpDisableRequest))]
[JsonSerializable(typeof(McpDisableResult))]
[JsonSerializable(typeof(McpDiscoverRequest))]
[JsonSerializable(typeof(McpDiscoverResult))]
[JsonSerializable(typeof(McpEnableRequest))]
[JsonSerializable(typeof(McpEnableResult))]
[JsonSerializable(typeof(McpList))]
[JsonSerializable(typeof(McpReload))]
[JsonSerializable(typeof(McpServer))]
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
[JsonSerializable(typeof(ModelCurrent))]
[JsonSerializable(typeof(ModelList))]
[JsonSerializable(typeof(ModelPolicy))]
[JsonSerializable(typeof(ModelSwitchToRequest))]
[JsonSerializable(typeof(ModelSwitchToResult))]
[JsonSerializable(typeof(PermissionDecisionRequest))]
[JsonSerializable(typeof(PermissionRequestResult))]
[JsonSerializable(typeof(PingRequest))]
[JsonSerializable(typeof(PingResult))]
[JsonSerializable(typeof(Plan))]
[JsonSerializable(typeof(PlanDelete))]
[JsonSerializable(typeof(PlanUpdateRequest))]
[JsonSerializable(typeof(PlanUpdateResult))]
[JsonSerializable(typeof(Plugin))]
[JsonSerializable(typeof(PluginList))]
[JsonSerializable(typeof(SessionAgentDeselectRequest))]
[JsonSerializable(typeof(SessionAgentGetCurrentRequest))]
[JsonSerializable(typeof(SessionAgentListRequest))]
[JsonSerializable(typeof(SessionAgentReloadRequest))]
[JsonSerializable(typeof(SessionExtensionsListRequest))]
[JsonSerializable(typeof(SessionExtensionsReloadRequest))]
[JsonSerializable(typeof(SessionFsAppendFileRequest))]
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
[JsonSerializable(typeof(SessionMcpListRequest))]
[JsonSerializable(typeof(SessionMcpReloadRequest))]
[JsonSerializable(typeof(SessionMode))]
[JsonSerializable(typeof(SessionModeGetRequest))]
[JsonSerializable(typeof(SessionModelGetCurrentRequest))]
[JsonSerializable(typeof(SessionPlanDeleteRequest))]
[JsonSerializable(typeof(SessionPlanReadRequest))]
[JsonSerializable(typeof(SessionPluginsListRequest))]
[JsonSerializable(typeof(SessionSkillsListRequest))]
[JsonSerializable(typeof(SessionSkillsReloadRequest))]
[JsonSerializable(typeof(SessionUsageGetMetricsRequest))]
[JsonSerializable(typeof(SessionWorkspaceListFilesRequest))]
[JsonSerializable(typeof(SessionsForkRequest))]
[JsonSerializable(typeof(SessionsForkResult))]
[JsonSerializable(typeof(ShellExecRequest))]
[JsonSerializable(typeof(ShellExecResult))]
[JsonSerializable(typeof(ShellKillRequest))]
[JsonSerializable(typeof(ShellKillResult))]
[JsonSerializable(typeof(Skill))]
[JsonSerializable(typeof(SkillList))]
[JsonSerializable(typeof(SkillsDisableRequest))]
[JsonSerializable(typeof(SkillsDisableResult))]
[JsonSerializable(typeof(SkillsEnableRequest))]
[JsonSerializable(typeof(SkillsEnableResult))]
[JsonSerializable(typeof(SkillsReload))]
[JsonSerializable(typeof(Tool))]
[JsonSerializable(typeof(ToolList))]
[JsonSerializable(typeof(ToolsHandlePendingToolCallRequest))]
[JsonSerializable(typeof(ToolsListRequest))]
[JsonSerializable(typeof(UiElicitationRequest))]
[JsonSerializable(typeof(UiElicitationResponse))]
[JsonSerializable(typeof(UiElicitationResult))]
[JsonSerializable(typeof(UiElicitationSchema))]
[JsonSerializable(typeof(UsageMetrics))]
[JsonSerializable(typeof(UsageMetricsCodeChanges))]
[JsonSerializable(typeof(UsageMetricsModelMetric))]
[JsonSerializable(typeof(UsageMetricsModelMetricRequests))]
[JsonSerializable(typeof(UsageMetricsModelMetricUsage))]
[JsonSerializable(typeof(WorkspaceCreateFileRequest))]
[JsonSerializable(typeof(WorkspaceCreateFileResult))]
[JsonSerializable(typeof(WorkspaceFiles))]
[JsonSerializable(typeof(WorkspaceReadFileRequest))]
[JsonSerializable(typeof(WorkspaceReadFileResult))]
internal partial class RpcJsonContext : JsonSerializerContext;