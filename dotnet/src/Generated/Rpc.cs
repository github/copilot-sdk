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
using System.Threading;

namespace GitHub.Copilot.Rpc;

/// <summary>Server liveness response, including the echoed message, current server timestamp, and protocol version.</summary>
public sealed class PingResult
{
    /// <summary>Echoed message (or default greeting).</summary>
    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    /// <summary>Server protocol version number.</summary>
    [JsonPropertyName("protocolVersion")]
    public long ProtocolVersion { get; set; }

    /// <summary>ISO 8601 timestamp when the server handled the ping.</summary>
    [JsonPropertyName("timestamp")]
    public DateTimeOffset Timestamp { get; set; }
}

/// <summary>Optional message to echo back to the caller.</summary>
internal sealed class PingRequest
{
    /// <summary>Optional message to echo back.</summary>
    [JsonPropertyName("message")]
    public string? Message { get; set; }
}

/// <summary>Handshake result reporting the server's protocol version and package version on success.</summary>
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

/// <summary>Optional connection token presented by the SDK client during the handshake.</summary>
internal sealed class ConnectRequest
{
    /// <summary>Connection token; required when the server was started with COPILOT_CONNECTION_TOKEN.</summary>
    [JsonPropertyName("token")]
    public string? Token { get; set; }
}

/// <summary>Long context tier pricing (available for models with extended context windows).</summary>
public sealed class ModelBillingTokenPricesLongContext
{
    /// <summary>AI Credits cost per billing batch of cached tokens.</summary>
    [JsonPropertyName("cachePrice")]
    public double? CachePrice { get; set; }

    /// <summary>Maximum context window tokens for the long context tier.</summary>
    [JsonPropertyName("contextMax")]
    public long? ContextMax { get; set; }

    /// <summary>AI Credits cost per billing batch of input tokens.</summary>
    [JsonPropertyName("inputPrice")]
    public double? InputPrice { get; set; }

    /// <summary>AI Credits cost per billing batch of output tokens.</summary>
    [JsonPropertyName("outputPrice")]
    public double? OutputPrice { get; set; }
}

/// <summary>Token-level pricing information for this model.</summary>
public sealed class ModelBillingTokenPrices
{
    /// <summary>Number of tokens per standard billing batch.</summary>
    [JsonPropertyName("batchSize")]
    public long? BatchSize { get; set; }

    /// <summary>AI Credits cost per billing batch of cached tokens.</summary>
    [JsonPropertyName("cachePrice")]
    public double? CachePrice { get; set; }

    /// <summary>Maximum context window tokens for the default tier.</summary>
    [JsonPropertyName("contextMax")]
    public long? ContextMax { get; set; }

    /// <summary>AI Credits cost per billing batch of input tokens.</summary>
    [JsonPropertyName("inputPrice")]
    public double? InputPrice { get; set; }

    /// <summary>Long context tier pricing (available for models with extended context windows).</summary>
    [JsonPropertyName("longContext")]
    public ModelBillingTokenPricesLongContext? LongContext { get; set; }

    /// <summary>AI Credits cost per billing batch of output tokens.</summary>
    [JsonPropertyName("outputPrice")]
    public double? OutputPrice { get; set; }
}

/// <summary>Billing information.</summary>
public sealed class ModelBilling
{
    /// <summary>Billing cost multiplier relative to the base rate.</summary>
    [JsonPropertyName("multiplier")]
    public double? Multiplier { get; set; }

    /// <summary>Token-level pricing information for this model.</summary>
    [JsonPropertyName("tokenPrices")]
    public ModelBillingTokenPrices? TokenPrices { get; set; }
}

/// <summary>Vision-specific limits.</summary>
public sealed class ModelCapabilitiesLimitsVision
{
    /// <summary>Maximum image size in bytes.</summary>
    [JsonPropertyName("max_prompt_image_size")]
    public long MaxPromptImageSize { get; set; }

    /// <summary>Maximum number of images per prompt.</summary>
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
    [JsonPropertyName("max_context_window_tokens")]
    public long? MaxContextWindowTokens { get; set; }

    /// <summary>Maximum number of output/completion tokens.</summary>
    [JsonPropertyName("max_output_tokens")]
    public long? MaxOutputTokens { get; set; }

    /// <summary>Maximum number of prompt/input tokens.</summary>
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
    public ModelPolicyState State { get; set; }

    /// <summary>Usage terms or conditions for this model.</summary>
    [JsonPropertyName("terms")]
    public string? Terms { get; set; }
}

/// <summary>Schema for the `Model` type.</summary>
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

    /// <summary>Model capability category for grouping in the model picker.</summary>
    [JsonPropertyName("modelPickerCategory")]
    public ModelPickerCategory? ModelPickerCategory { get; set; }

    /// <summary>Relative cost tier for token-based billing users.</summary>
    [JsonPropertyName("modelPickerPriceCategory")]
    public ModelPickerPriceCategory? ModelPickerPriceCategory { get; set; }

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

/// <summary>List of Copilot models available to the resolved user, including capabilities and billing metadata.</summary>
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

/// <summary>Schema for the `Tool` type.</summary>
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
    public IDictionary<string, JsonElement>? Parameters { get; set; }
}

/// <summary>Built-in tools available for the requested model, with their parameters and instructions.</summary>
public sealed class ToolList
{
    /// <summary>List of available built-in tools with metadata.</summary>
    [JsonPropertyName("tools")]
    public IList<Tool> Tools { get => field ??= []; set; }
}

/// <summary>Optional model identifier whose tool overrides should be applied to the listing.</summary>
internal sealed class ToolsListRequest
{
    /// <summary>Optional model ID — when provided, the returned tool list reflects model-specific overrides.</summary>
    [JsonPropertyName("model")]
    public string? Model { get; set; }
}

/// <summary>Schema for the `AccountQuotaSnapshot` type.</summary>
public sealed class AccountQuotaSnapshot
{
    /// <summary>Number of requests included in the entitlement, or -1 for unlimited entitlements.</summary>
    [JsonPropertyName("entitlementRequests")]
    public long EntitlementRequests { get; set; }

    /// <summary>Whether the user has an unlimited usage entitlement.</summary>
    [JsonPropertyName("isUnlimitedEntitlement")]
    public bool IsUnlimitedEntitlement { get; set; }

    /// <summary>Number of additional usage requests made this period.</summary>
    [JsonPropertyName("overage")]
    public double Overage { get; set; }

    /// <summary>Whether additional usage is allowed when quota is exhausted.</summary>
    [JsonPropertyName("overageAllowedWithExhaustedQuota")]
    public bool OverageAllowedWithExhaustedQuota { get; set; }

    /// <summary>Percentage of entitlement remaining.</summary>
    [JsonPropertyName("remainingPercentage")]
    public double RemainingPercentage { get; set; }

    /// <summary>Date when the quota resets (ISO 8601 string).</summary>
    [JsonPropertyName("resetDate")]
    public DateTimeOffset? ResetDate { get; set; }

    /// <summary>Whether usage is still permitted after quota exhaustion.</summary>
    [JsonPropertyName("usageAllowedWithExhaustedQuota")]
    public bool UsageAllowedWithExhaustedQuota { get; set; }

    /// <summary>Number of requests used so far this period.</summary>
    [JsonPropertyName("usedRequests")]
    public long UsedRequests { get; set; }
}

/// <summary>Quota usage snapshots for the resolved user, keyed by quota type.</summary>
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

/// <summary>Confirmation that the secret values were registered.</summary>
public sealed class SecretsAddFilterValuesResult
{
    /// <summary>Whether the values were successfully registered.</summary>
    [JsonPropertyName("ok")]
    public bool Ok { get; set; }
}

/// <summary>Secret values to add to the redaction filter.</summary>
internal sealed class SecretsAddFilterValuesRequest
{
    /// <summary>Raw secret values to register for redaction.</summary>
    [JsonPropertyName("values")]
    public IList<string> Values { get => field ??= []; set; }
}

/// <summary>Schema for the `DiscoveredMcpServer` type.</summary>
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

    /// <summary>Configuration source: user, workspace, plugin, or builtin.</summary>
    [JsonPropertyName("source")]
    public McpServerSource Source { get; set; }

    /// <summary>Server transport type: stdio, http, sse (deprecated), or memory.</summary>
    [JsonPropertyName("type")]
    public DiscoveredMcpServerType? Type { get; set; }
}

/// <summary>MCP servers discovered from user, workspace, plugin, and built-in sources.</summary>
public sealed class McpDiscoverResult
{
    /// <summary>MCP servers discovered from all sources.</summary>
    [JsonPropertyName("servers")]
    public IList<DiscoveredMcpServer> Servers { get => field ??= []; set; }
}

/// <summary>Optional working directory used as context for MCP server discovery.</summary>
internal sealed class McpDiscoverRequest
{
    /// <summary>Working directory used as context for discovery (e.g., plugin resolution).</summary>
    [JsonPropertyName("workingDirectory")]
    public string? WorkingDirectory { get; set; }
}

/// <summary>User-configured MCP servers, keyed by server name.</summary>
public sealed class McpConfigList
{
    /// <summary>All MCP servers from user config, keyed by name.</summary>
    [JsonPropertyName("servers")]
    public IDictionary<string, JsonElement> Servers { get => field ??= new Dictionary<string, JsonElement>(); set; }
}

/// <summary>MCP server name and configuration to add to user configuration.</summary>
internal sealed class McpConfigAddRequest
{
    /// <summary>MCP server configuration (stdio process or remote HTTP/SSE).</summary>
    [JsonPropertyName("config")]
    public JsonElement Config { get; set; }

    /// <summary>Unique name for the MCP server.</summary>
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>MCP server name and replacement configuration to write to user configuration.</summary>
internal sealed class McpConfigUpdateRequest
{
    /// <summary>MCP server configuration (stdio process or remote HTTP/SSE).</summary>
    [JsonPropertyName("config")]
    public JsonElement Config { get; set; }

    /// <summary>Name of the MCP server to update.</summary>
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>MCP server name to remove from user configuration.</summary>
internal sealed class McpConfigRemoveRequest
{
    /// <summary>Name of the MCP server to remove.</summary>
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>MCP server names to enable for new sessions.</summary>
internal sealed class McpConfigEnableRequest
{
    /// <summary>Names of MCP servers to enable. Each server is removed from the persisted disabled list so new sessions spawn it. Unknown or already-enabled names are ignored.</summary>
    [JsonPropertyName("names")]
    public IList<string> Names { get => field ??= []; set; }
}

/// <summary>MCP server names to disable for new sessions.</summary>
internal sealed class McpConfigDisableRequest
{
    /// <summary>Names of MCP servers to disable. Each server is added to the persisted disabled list so new sessions skip it. Already-disabled names are ignored. Active sessions keep their current connections until they end.</summary>
    [JsonPropertyName("names")]
    public IList<string> Names { get => field ??= []; set; }
}

/// <summary>Schema for the `ServerSkill` type.</summary>
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
    public SkillSource Source { get; set; }

    /// <summary>Whether the skill can be invoked by the user as a slash command.</summary>
    [JsonPropertyName("userInvocable")]
    public bool UserInvocable { get; set; }
}

/// <summary>Skills discovered across global and project sources.</summary>
public sealed class ServerSkillList
{
    /// <summary>All discovered skills across all sources.</summary>
    [JsonPropertyName("skills")]
    public IList<ServerSkill> Skills { get => field ??= []; set; }
}

/// <summary>Optional project paths and additional skill directories to include in discovery.</summary>
internal sealed class SkillsDiscoverRequest
{
    /// <summary>Optional list of project directory paths to scan for project-scoped skills.</summary>
    [JsonPropertyName("projectPaths")]
    public IList<string>? ProjectPaths { get; set; }

    /// <summary>Optional list of additional skill directory paths to include.</summary>
    [JsonPropertyName("skillDirectories")]
    public IList<string>? SkillDirectories { get; set; }
}

/// <summary>Skill names to mark as disabled in global configuration, replacing any previous list.</summary>
internal sealed class SkillsConfigSetDisabledSkillsRequest
{
    /// <summary>List of skill names to disable.</summary>
    [JsonPropertyName("disabledSkills")]
    public IList<string> DisabledSkills { get => field ??= []; set; }
}

/// <summary>Indicates whether the calling client was registered as the session filesystem provider.</summary>
public sealed class SessionFsSetProviderResult
{
    /// <summary>Whether the provider was set successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Optional capabilities declared by the provider.</summary>
public sealed class SessionFsSetProviderCapabilities
{
    /// <summary>Whether the provider supports SQLite query/exists operations.</summary>
    [JsonPropertyName("sqlite")]
    public bool? Sqlite { get; set; }
}

/// <summary>Initial working directory, session-state path layout, and path conventions used to register the calling SDK client as the session filesystem provider.</summary>
internal sealed class SessionFsSetProviderRequest
{
    /// <summary>Optional capabilities declared by the provider.</summary>
    [JsonPropertyName("capabilities")]
    public SessionFsSetProviderCapabilities? Capabilities { get; set; }

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

/// <summary>Identifier and optional friendly name assigned to the newly forked session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionsForkResult
{
    /// <summary>Friendly name assigned to the forked session, if any.</summary>
    [JsonPropertyName("name")]
    public string? Name { get; set; }

    /// <summary>The new forked session's ID.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Source session identifier to fork from, optional event-ID boundary, and optional friendly name for the new session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsForkRequest
{
    /// <summary>Optional friendly name to assign to the forked session.</summary>
    [JsonPropertyName("name")]
    public string? Name { get; set; }

    /// <summary>Source session ID to fork from.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Optional event ID boundary. When provided, the fork includes only events before this ID (exclusive). When omitted, all events are included.</summary>
    [JsonPropertyName("toEventId")]
    public string? ToEventId { get; set; }
}

/// <summary>Repository associated with the connected remote session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ConnectedRemoteSessionMetadataRepository
{
    /// <summary>Branch associated with the remote session.</summary>
    [JsonPropertyName("branch")]
    public string Branch { get; set; } = string.Empty;

    /// <summary>Repository name.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Repository owner or organization login.</summary>
    [JsonPropertyName("owner")]
    public string Owner { get; set; } = string.Empty;
}

/// <summary>Metadata for a connected remote session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ConnectedRemoteSessionMetadata
{
    /// <summary>Neutral SDK discriminator for the connected remote session kind.</summary>
    [JsonPropertyName("kind")]
    public ConnectedRemoteSessionMetadataKind Kind { get; set; }

    /// <summary>Last session update time as an ISO 8601 string.</summary>
    [JsonPropertyName("modifiedTime")]
    public DateTimeOffset ModifiedTime { get; set; }

    /// <summary>Optional friendly session name.</summary>
    [JsonPropertyName("name")]
    public string? Name { get; set; }

    /// <summary>Pull request number associated with the session.</summary>
    [JsonPropertyName("pullRequestNumber")]
    public long? PullRequestNumber { get; set; }

    /// <summary>Repository associated with the connected remote session.</summary>
    [JsonPropertyName("repository")]
    public ConnectedRemoteSessionMetadataRepository Repository { get => field ??= new(); set; }

    /// <summary>Original remote resource identifier.</summary>
    [JsonPropertyName("resourceId")]
    public string? ResourceId { get; set; }

    /// <summary>SDK session ID for the connected remote session.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Remote session staleness deadline as an ISO 8601 string.</summary>
    [JsonPropertyName("staleAt")]
    public DateTimeOffset? StaleAt { get; set; }

    /// <summary>Session start time as an ISO 8601 string.</summary>
    [JsonPropertyName("startTime")]
    public DateTimeOffset StartTime { get; set; }

    /// <summary>Remote session state returned by the backing service.</summary>
    [JsonPropertyName("state")]
    public string? State { get; set; }

    /// <summary>Optional session summary.</summary>
    [JsonPropertyName("summary")]
    public string? Summary { get; set; }
}

/// <summary>Remote session connection result.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class RemoteSessionConnectionResult
{
    /// <summary>Metadata for a connected remote session.</summary>
    [JsonPropertyName("metadata")]
    public ConnectedRemoteSessionMetadata Metadata { get => field ??= new(); set; }

    /// <summary>SDK session ID for the connected remote session.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Remote session connection parameters.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class ConnectRemoteSessionParams
{
    /// <summary>Session ID to connect to.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Schema for the `SessionContext` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionContext
{
    /// <summary>Active git branch.</summary>
    [JsonPropertyName("branch")]
    public string? Branch { get; set; }

    /// <summary>Most recent working directory for this session.</summary>
    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    /// <summary>Git repository root, if the cwd was inside a git repo.</summary>
    [JsonPropertyName("gitRoot")]
    public string? GitRoot { get; set; }

    /// <summary>Repository host type.</summary>
    [JsonPropertyName("hostType")]
    public SessionContextHostType? HostType { get; set; }

    /// <summary>Repository slug in `owner/name` form, when known.</summary>
    [JsonPropertyName("repository")]
    public string? Repository { get; set; }
}

/// <summary>Schema for the `SessionMetadata` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionMetadata
{
    /// <summary>Runtime client name that created/last resumed this session.</summary>
    [JsonPropertyName("clientName")]
    public string? ClientName { get; set; }

    /// <summary>Schema for the `SessionContext` type.</summary>
    [JsonPropertyName("context")]
    public SessionContext? Context { get; set; }

    /// <summary>True for remote (GitHub) sessions; false for local.</summary>
    [JsonPropertyName("isRemote")]
    public bool IsRemote { get; set; }

    /// <summary>GitHub task ID, when this local session is bound to one. Only present for local sessions exported to remote control.</summary>
    [JsonPropertyName("mcTaskId")]
    public string? McTaskId { get; set; }

    /// <summary>Last-modified time of the session's persisted state, as ISO 8601.</summary>
    [JsonPropertyName("modifiedTime")]
    public string ModifiedTime { get; set; } = string.Empty;

    /// <summary>Optional human-friendly name set via /rename.</summary>
    [JsonPropertyName("name")]
    public string? Name { get; set; }

    /// <summary>Stable session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Session creation time as an ISO 8601 timestamp.</summary>
    [JsonPropertyName("startTime")]
    public string StartTime { get; set; } = string.Empty;

    /// <summary>Short summary of the session, when one has been derived.</summary>
    [JsonPropertyName("summary")]
    public string? Summary { get; set; }
}

/// <summary>Persisted sessions matching the filter, ordered most-recently-modified first.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionList
{
    /// <summary>Sessions ordered most-recently-modified first.</summary>
    [JsonPropertyName("sessions")]
    public IList<SessionMetadata> Sessions { get => field ??= []; set; }
}

/// <summary>Optional filter applied to the returned sessions.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionListFilter
{
    /// <summary>Match sessions whose context.branch equals this value.</summary>
    [JsonPropertyName("branch")]
    public string? Branch { get; set; }

    /// <summary>Match sessions whose context.cwd equals this value.</summary>
    [JsonPropertyName("cwd")]
    public string? Cwd { get; set; }

    /// <summary>Match sessions whose context.gitRoot equals this value.</summary>
    [JsonPropertyName("gitRoot")]
    public string? GitRoot { get; set; }

    /// <summary>Match sessions whose context.repository equals this value.</summary>
    [JsonPropertyName("repository")]
    public string? Repository { get; set; }
}

/// <summary>Optional metadata-load limit and context filter applied to the returned sessions.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsListRequest
{
    /// <summary>Optional filter applied to the returned sessions.</summary>
    [JsonPropertyName("filter")]
    public SessionListFilter? Filter { get; set; }

    /// <summary>When provided, only the first N sessions (sorted by modification time, newest first) load full metadata; remaining sessions return basic info only. Use 0 to return only basic info for every session.</summary>
    [JsonPropertyName("metadataLimit")]
    public long? MetadataLimit { get; set; }
}

/// <summary>ID of the local session bound to the given GitHub task, or omitted when none.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionsFindByTaskIDResult
{
    /// <summary>Omitted when no local session is bound to that GitHub task.</summary>
    [JsonPropertyName("sessionId")]
    public string? SessionId { get; set; }
}

/// <summary>GitHub task ID to look up.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsFindByTaskIDRequest
{
    /// <summary>GitHub task ID to look up.</summary>
    [JsonPropertyName("taskId")]
    public string TaskId { get; set; } = string.Empty;
}

/// <summary>Session ID matching the prefix, omitted when no unique match exists.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionsFindByPrefixResult
{
    /// <summary>Omitted when no unique session matches the prefix (no match or ambiguous).</summary>
    [JsonPropertyName("sessionId")]
    public string? SessionId { get; set; }
}

/// <summary>UUID prefix to resolve to a unique session ID.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsFindByPrefixRequest
{
    /// <summary>UUID prefix (&gt;=7 hex chars, &lt;36 chars). Returns the unique session ID, or undefined when there is no match or the prefix matches multiple sessions.</summary>
    [JsonPropertyName("prefix")]
    public string Prefix { get; set; } = string.Empty;
}

/// <summary>Most-relevant session ID for the supplied context, or omitted when no sessions exist.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionsGetLastForContextResult
{
    /// <summary>Most-relevant session ID for the supplied context, or omitted when no sessions exist.</summary>
    [JsonPropertyName("sessionId")]
    public string? SessionId { get; set; }
}

/// <summary>Optional working-directory context used to score session relevance.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsGetLastForContextRequest
{
    /// <summary>Optional working-directory context used to score session relevance. When omitted the most-recently-modified session wins.</summary>
    [JsonPropertyName("context")]
    public SessionContext? Context { get; set; }
}

/// <summary>Absolute path to the session's events.jsonl file on disk.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionsGetEventFilePathResult
{
    /// <summary>Absolute path to the session's events.jsonl file.</summary>
    [JsonPropertyName("filePath")]
    public string FilePath { get; set; } = string.Empty;
}

/// <summary>Session ID whose event-log file path to compute.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsGetEventFilePathRequest
{
    /// <summary>Session ID whose event-log file path to compute.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Map of sessionId -&gt; on-disk size in bytes for each session's workspace directory.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionSizes
{
    /// <summary>Map of sessionId -&gt; on-disk size in bytes for the session's workspace directory.</summary>
    [JsonPropertyName("sizes")]
    public IDictionary<string, long> Sizes { get => field ??= new Dictionary<string, long>(); set; }
}

/// <summary>Session IDs from the input set that are currently in use by another process.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionsCheckInUseResult
{
    /// <summary>Session IDs from the input set that are currently held by another running process via an alive lock file.</summary>
    [JsonPropertyName("inUse")]
    public IList<string> InUse { get => field ??= []; set; }
}

/// <summary>Session IDs to test for live in-use locks.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsCheckInUseRequest
{
    /// <summary>Session IDs to test for live in-use locks.</summary>
    [JsonPropertyName("sessionIds")]
    public IList<string> SessionIds { get => field ??= []; set; }
}

/// <summary>The session's persisted remote-steerable flag, or omitted when no value has been persisted.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionsGetPersistedRemoteSteerableResult
{
    /// <summary>The session's persisted remote-steerable flag if recorded; omitted when no value has been persisted.</summary>
    [JsonPropertyName("remoteSteerable")]
    public bool? RemoteSteerable { get; set; }
}

/// <summary>Session ID to look up the persisted remote-steerable flag for.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsGetPersistedRemoteSteerableRequest
{
    /// <summary>Session ID to look up the persisted remote-steerable flag for.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Closes a session: emits shutdown, flushes pending events to disk, releases the in-use lock, disposes the active session. Idempotent: succeeds even if the session is not currently active.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionsCloseResult
{
}

/// <summary>Session ID to close.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsCloseRequest
{
    /// <summary>Session ID to close.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Map of sessionId -&gt; bytes freed by removing the session's workspace directory.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionBulkDeleteResult
{
    /// <summary>Map of sessionId -&gt; bytes freed by removing the session's workspace directory. Sessions whose deletion failed are omitted from this map (failures are logged on the server but not surfaced per-id; check the map for absent IDs to detect them).</summary>
    [JsonPropertyName("freedBytes")]
    public IDictionary<string, long> FreedBytes { get => field ??= new Dictionary<string, long>(); set; }
}

/// <summary>Session IDs to close, deactivate, and delete from disk.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsBulkDeleteRequest
{
    /// <summary>Session IDs to close, deactivate, and delete from disk.</summary>
    [JsonPropertyName("sessionIds")]
    public IList<string> SessionIds { get => field ??= []; set; }
}

/// <summary>Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionPruneResult
{
    /// <summary>Session IDs that would be deleted in dry-run mode (always empty otherwise).</summary>
    [JsonPropertyName("candidates")]
    public IList<string> Candidates { get => field ??= []; set; }

    /// <summary>Session IDs that were deleted (always empty in dry-run mode).</summary>
    [JsonPropertyName("deleted")]
    public IList<string> Deleted { get => field ??= []; set; }

    /// <summary>True when no deletions were actually performed.</summary>
    [JsonPropertyName("dryRun")]
    public bool DryRun { get; set; }

    /// <summary>Total bytes freed (actual when not dry-run, projected when dry-run).</summary>
    [JsonPropertyName("freedBytes")]
    public long FreedBytes { get; set; }

    /// <summary>Session IDs that were skipped (e.g., named sessions).</summary>
    [JsonPropertyName("skipped")]
    public IList<string> Skipped { get => field ??= []; set; }
}

/// <summary>Age threshold and optional flags controlling which old sessions are pruned (or simulated when dryRun is true).</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsPruneOldRequest
{
    /// <summary>When true, only report what would be deleted without performing any deletion.</summary>
    [JsonPropertyName("dryRun")]
    public bool? DryRun { get; set; }

    /// <summary>Session IDs that should never be considered for pruning.</summary>
    [JsonPropertyName("excludeSessionIds")]
    public IList<string>? ExcludeSessionIds { get; set; }

    /// <summary>When true, named sessions (set via /rename) are also eligible for pruning.</summary>
    [JsonPropertyName("includeNamed")]
    public bool? IncludeNamed { get; set; }

    /// <summary>Delete sessions whose modifiedTime is at least this many days old.</summary>
    [JsonPropertyName("olderThanDays")]
    public long OlderThanDays { get; set; }
}

/// <summary>Flush a session's pending events to disk. No-op when no writer exists for the session (e.g., already closed).</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionsSaveResult
{
}

/// <summary>Session ID whose pending events should be flushed to disk.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsSaveRequest
{
    /// <summary>Session ID whose pending events should be flushed to disk.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Release the in-use lock held by this process for the given session. No-op when this process does not currently hold a lock for the session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionsReleaseLockResult
{
}

/// <summary>Session ID whose in-use lock should be released.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsReleaseLockRequest
{
    /// <summary>Session ID whose in-use lock should be released.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>The enriched metadata records, with summary and context fields backfilled where available. Sessions confirmed empty and unnamed are omitted.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionEnrichMetadataResult
{
    /// <summary>Enriched records, with summary and context backfilled. Sessions confirmed empty and unnamed may be omitted.</summary>
    [JsonPropertyName("sessions")]
    public IList<SessionMetadata> Sessions { get => field ??= []; set; }
}

/// <summary>Session metadata records to enrich with summary and context information.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsEnrichMetadataRequest
{
    /// <summary>Session metadata records to enrich. Records that already have summary and context are returned unchanged.</summary>
    [JsonPropertyName("sessions")]
    public IList<SessionMetadata> Sessions { get => field ??= []; set; }
}

/// <summary>Reload all hooks (user, plugin, optionally repo) and apply them to the active session. Call after installing or removing plugins so their hooks take effect immediately. No-op when no active session matches the given sessionId.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionsReloadPluginHooksResult
{
}

/// <summary>Active session ID and an optional flag for deferring repo-level hooks until folder trust.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsReloadPluginHooksRequest
{
    /// <summary>When true, skip repo-level hooks. Use before folder trust is confirmed; loadDeferredRepoHooks loads them post-trust.</summary>
    [JsonPropertyName("deferRepoHooks")]
    public bool? DeferRepoHooks { get; set; }

    /// <summary>Active session ID to reload hooks for.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Queued repo-level startup prompts and the total hook command count after loading.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionLoadDeferredRepoHooksResult
{
    /// <summary>Total hook command count (user + plugin + repo) loaded for the session by this call. Captured atomically with startupPrompts so callers don't need to read a separate counter.</summary>
    [JsonPropertyName("hookCount")]
    public long HookCount { get; set; }

    /// <summary>Repo-level startup prompts queued from repo hook configs. Empty on resume, when no repo configs were pending, or when disableAllHooks is set.</summary>
    [JsonPropertyName("startupPrompts")]
    public IList<string> StartupPrompts { get => field ??= []; set; }
}

/// <summary>Active session ID whose deferred repo-level hooks should be loaded.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsLoadDeferredRepoHooksRequest
{
    /// <summary>Active session ID whose deferred repo-level hooks should be loaded.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Replace the manager-wide additional plugins. New session creations and subsequent hook reloads see the new set; already-running sessions keep their existing hook installation until the next reload.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionsSetAdditionalPluginsResult
{
}

/// <summary>Schema for the `InstalledPlugin` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class InstalledPlugin
{
    /// <summary>Path where the plugin is cached locally.</summary>
    [JsonPropertyName("cache_path")]
    public string? CachePath { get; set; }

    /// <summary>Whether the plugin is currently enabled.</summary>
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }

    /// <summary>Installation timestamp.</summary>
    [JsonPropertyName("installed_at")]
    public string InstalledAt { get; set; } = string.Empty;

    /// <summary>Marketplace the plugin came from (empty string for direct repo installs).</summary>
    [JsonPropertyName("marketplace")]
    public string Marketplace { get; set; } = string.Empty;

    /// <summary>Plugin name.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Source for direct repo installs (when marketplace is empty).</summary>
    [JsonPropertyName("source")]
    public JsonElement? Source { get; set; }

    /// <summary>Version installed (if available).</summary>
    [JsonPropertyName("version")]
    public string? Version { get; set; }
}

/// <summary>Manager-wide additional plugins to register; replaces any previously-configured set.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionsSetAdditionalPluginsRequest
{
    /// <summary>Manager-wide additional plugins to register. Replaces any previously-configured set. Pass an empty array to clear.</summary>
    [JsonPropertyName("plugins")]
    public IList<InstalledPlugin> Plugins { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionSuspendRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Result of sending a user message.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SendResult
{
    /// <summary>Unique identifier assigned to the message.</summary>
    [JsonPropertyName("messageId")]
    public string MessageId { get; set; } = string.Empty;
}

/// <summary>A user message attachment — a file, directory, code selection, blob, or GitHub reference.</summary>
/// <remarks>Polymorphic base type discriminated by <c>type</c>.</remarks>
[Experimental(Diagnostics.Experimental)]
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "type",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(SendAttachmentFile), "file")]
[JsonDerivedType(typeof(SendAttachmentDirectory), "directory")]
[JsonDerivedType(typeof(SendAttachmentSelection), "selection")]
[JsonDerivedType(typeof(SendAttachmentGithubReference), "github_reference")]
[JsonDerivedType(typeof(SendAttachmentBlob), "blob")]
public partial class SendAttachment
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("type")]
    public virtual string Type { get; set; } = string.Empty;
}


/// <summary>Optional line range to scope the attachment to a specific section of the file.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SendAttachmentFileLineRange
{
    /// <summary>End line number (1-based, inclusive).</summary>
    [JsonPropertyName("end")]
    public long End { get; set; }

    /// <summary>Start line number (1-based).</summary>
    [JsonPropertyName("start")]
    public long Start { get; set; }
}

/// <summary>File attachment.</summary>
/// <remarks>The <c>file</c> variant of <see cref="SendAttachment"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class SendAttachmentFile : SendAttachment
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "file";

    /// <summary>User-facing display name for the attachment.</summary>
    [JsonPropertyName("displayName")]
    public required string DisplayName { get; set; }

    /// <summary>Optional line range to scope the attachment to a specific section of the file.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("lineRange")]
    public SendAttachmentFileLineRange? LineRange { get; set; }

    /// <summary>Absolute file path.</summary>
    [JsonPropertyName("path")]
    public required string Path { get; set; }
}

/// <summary>Directory attachment.</summary>
/// <remarks>The <c>directory</c> variant of <see cref="SendAttachment"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class SendAttachmentDirectory : SendAttachment
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "directory";

    /// <summary>User-facing display name for the attachment.</summary>
    [JsonPropertyName("displayName")]
    public required string DisplayName { get; set; }

    /// <summary>Absolute directory path.</summary>
    [JsonPropertyName("path")]
    public required string Path { get; set; }
}

/// <summary>End position of the selection.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SendAttachmentSelectionDetailsEnd
{
    /// <summary>End character offset within the line (0-based).</summary>
    [JsonPropertyName("character")]
    public long Character { get; set; }

    /// <summary>End line number (0-based).</summary>
    [JsonPropertyName("line")]
    public long Line { get; set; }
}

/// <summary>Start position of the selection.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SendAttachmentSelectionDetailsStart
{
    /// <summary>Start character offset within the line (0-based).</summary>
    [JsonPropertyName("character")]
    public long Character { get; set; }

    /// <summary>Start line number (0-based).</summary>
    [JsonPropertyName("line")]
    public long Line { get; set; }
}

/// <summary>Position range of the selection within the file.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SendAttachmentSelectionDetails
{
    /// <summary>End position of the selection.</summary>
    [JsonPropertyName("end")]
    public SendAttachmentSelectionDetailsEnd End { get => field ??= new(); set; }

    /// <summary>Start position of the selection.</summary>
    [JsonPropertyName("start")]
    public SendAttachmentSelectionDetailsStart Start { get => field ??= new(); set; }
}

/// <summary>Code selection attachment from an editor.</summary>
/// <remarks>The <c>selection</c> variant of <see cref="SendAttachment"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class SendAttachmentSelection : SendAttachment
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "selection";

    /// <summary>User-facing display name for the selection.</summary>
    [JsonPropertyName("displayName")]
    public required string DisplayName { get; set; }

    /// <summary>Absolute path to the file containing the selection.</summary>
    [JsonPropertyName("filePath")]
    public required string FilePath { get; set; }

    /// <summary>Position range of the selection within the file.</summary>
    [JsonPropertyName("selection")]
    public required SendAttachmentSelectionDetails Selection { get; set; }

    /// <summary>The selected text content.</summary>
    [JsonPropertyName("text")]
    public required string Text { get; set; }
}

/// <summary>GitHub issue, pull request, or discussion reference.</summary>
/// <remarks>The <c>github_reference</c> variant of <see cref="SendAttachment"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class SendAttachmentGithubReference : SendAttachment
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "github_reference";

    /// <summary>Issue, pull request, or discussion number.</summary>
    [JsonPropertyName("number")]
    public required long Number { get; set; }

    /// <summary>Type of GitHub reference.</summary>
    [JsonPropertyName("referenceType")]
    public required SendAttachmentGithubReferenceType ReferenceType { get; set; }

    /// <summary>Current state of the referenced item (e.g., open, closed, merged).</summary>
    [JsonPropertyName("state")]
    public required string State { get; set; }

    /// <summary>Title of the referenced item.</summary>
    [JsonPropertyName("title")]
    public required string Title { get; set; }

    /// <summary>URL to the referenced item on GitHub.</summary>
    [JsonPropertyName("url")]
    public required string Url { get; set; }
}

/// <summary>Blob attachment with inline base64-encoded data.</summary>
/// <remarks>The <c>blob</c> variant of <see cref="SendAttachment"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class SendAttachmentBlob : SendAttachment
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "blob";

    /// <summary>Base64-encoded content.</summary>
    [Base64String]
    [JsonPropertyName("data")]
    public required string Data { get; set; }

    /// <summary>User-facing display name for the attachment.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("displayName")]
    public string? DisplayName { get; set; }

    /// <summary>MIME type of the inline data.</summary>
    [JsonPropertyName("mimeType")]
    public required string MimeType { get; set; }
}

/// <summary>Parameters for sending a user message to the session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SendRequest
{
    /// <summary>The UI mode the agent was in when this message was sent. Defaults to the session's current mode.</summary>
    [JsonPropertyName("agentMode")]
    public SendAgentMode? AgentMode { get; set; }

    /// <summary>Optional attachments (files, directories, selections, blobs, GitHub references) to include with the message.</summary>
    [JsonPropertyName("attachments")]
    public IList<SendAttachment>? Attachments { get; set; }

    /// <summary>If false, this message will not trigger a Premium Request Unit charge. User messages default to billable.</summary>
    [JsonPropertyName("billable")]
    public bool? Billable { get; set; }

    /// <summary>If provided, this is shown in the timeline instead of `prompt`.</summary>
    [JsonPropertyName("displayPrompt")]
    public string? DisplayPrompt { get; set; }

    /// <summary>How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.</summary>
    [JsonPropertyName("mode")]
    public SendMode? Mode { get; set; }

    /// <summary>If true, adds the message to the front of the queue instead of the end.</summary>
    [JsonPropertyName("prepend")]
    public bool? Prepend { get; set; }

    /// <summary>The user message text.</summary>
    [JsonPropertyName("prompt")]
    public string Prompt { get; set; } = string.Empty;

    /// <summary>Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key.</summary>
    [JsonPropertyName("requestHeaders")]
    public IDictionary<string, string>? RequestHeaders { get; set; }

    /// <summary>If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange.</summary>
    [JsonPropertyName("requiredTool")]
    public string? RequiredTool { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Optional provenance tag copied to the resulting user.message event. Supported values are `system`, `command-*`, and `schedule-*`.</summary>
    [JsonInclude]
    [JsonPropertyName("source")]
    internal JsonElement? Source { get; set; }

    /// <summary>W3C Trace Context traceparent header for distributed tracing of this agent turn.</summary>
    [JsonPropertyName("traceparent")]
    public string? Traceparent { get; set; }

    /// <summary>W3C Trace Context tracestate header for distributed tracing.</summary>
    [JsonPropertyName("tracestate")]
    public string? Tracestate { get; set; }

    /// <summary>If true, await completion of the agentic loop for this message before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageId`; the caller can rely on the agent having processed the message before the call resolves.</summary>
    [JsonPropertyName("wait")]
    public bool? Wait { get; set; }
}

/// <summary>Result of aborting the current turn.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AbortResult
{
    /// <summary>Error message if the abort failed.</summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }

    /// <summary>Whether the abort completed successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Parameters for aborting the current turn.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class AbortRequest
{
    /// <summary>Finite reason code describing why the current turn was aborted.</summary>
    [JsonPropertyName("reason")]
    public AbortReason? Reason { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Parameters for shutting down the session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class ShutdownRequest
{
    /// <summary>Optional human-readable reason. Typically the message of the error that triggered shutdown when type is 'error'.</summary>
    [JsonPropertyName("reason")]
    public string? Reason { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Why the session is being shut down. Defaults to "routine" when omitted.</summary>
    [JsonPropertyName("type")]
    public ShutdownType? Type { get; set; }
}

/// <summary>Identifier of the session event that was emitted for the log message.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class LogResult
{
    /// <summary>The unique identifier of the emitted session event.</summary>
    [JsonPropertyName("eventId")]
    public Guid EventId { get; set; }
}

/// <summary>Message text, optional severity level, persistence flag, optional follow-up URL, and optional tip.</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>Optional actionable tip displayed alongside the message. Only honored on `level: "info"`.</summary>
    [JsonPropertyName("tip")]
    public string? Tip { get; set; }

    /// <summary>Domain category for this log entry (e.g., "mcp", "subscription", "policy", "model"). Maps to `infoType`/`warningType`/`errorType` on the emitted event. Defaults to "notification".</summary>
    [JsonPropertyName("type")]
    public string? Type { get; set; }

    /// <summary>Optional URL the user can open in their browser for more details.</summary>
    [Url]
    [StringSyntax(StringSyntaxAttribute.Uri)]
    [JsonPropertyName("url")]
    public string? Url { get; set; }
}

/// <summary>Authentication status and account metadata for the session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionAuthStatus
{
    /// <summary>Authentication type.</summary>
    [JsonPropertyName("authType")]
    public AuthInfoType? AuthType { get; set; }

    /// <summary>Copilot plan tier (e.g., individual_pro, business).</summary>
    [JsonPropertyName("copilotPlan")]
    public string? CopilotPlan { get; set; }

    /// <summary>Authentication host URL.</summary>
    [Url]
    [StringSyntax(StringSyntaxAttribute.Uri)]
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

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionAuthGetStatusRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the credential update succeeded.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionSetCredentialsResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>The new auth credentials to install on the session. When omitted or `undefined`, the call is a no-op and the session's existing credentials are preserved. The runtime stores the value verbatim and uses it for outbound model/API requests; it does NOT re-validate or re-fetch the associated Copilot user response. Several variants carry secret material; treat this method's params as containing secrets at rest and in transit.</summary>
/// <remarks>Polymorphic base type discriminated by <c>type</c>.</remarks>
[Experimental(Diagnostics.Experimental)]
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "type",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(AuthInfoHmac), "hmac")]
[JsonDerivedType(typeof(AuthInfoEnv), "env")]
[JsonDerivedType(typeof(AuthInfoToken), "token")]
[JsonDerivedType(typeof(AuthInfoCopilotApiToken), "copilot-api-token")]
[JsonDerivedType(typeof(AuthInfoUser), "user")]
[JsonDerivedType(typeof(AuthInfoGhCli), "gh-cli")]
[JsonDerivedType(typeof(AuthInfoApiKey), "api-key")]
public partial class AuthInfo
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("type")]
    public virtual string Type { get; set; } = string.Empty;
}


/// <summary>Schema for the `CopilotUserResponseEndpoints` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CopilotUserResponseEndpoints
{
    /// <summary>Gets or sets the <c>api</c> value.</summary>
    [JsonPropertyName("api")]
    public string? Api { get; set; }

    /// <summary>Gets or sets the <c>origin-tracker</c> value.</summary>
    [JsonPropertyName("origin-tracker")]
    public string? OriginTracker { get; set; }

    /// <summary>Gets or sets the <c>proxy</c> value.</summary>
    [JsonPropertyName("proxy")]
    public string? Proxy { get; set; }

    /// <summary>Gets or sets the <c>telemetry</c> value.</summary>
    [JsonPropertyName("telemetry")]
    public string? Telemetry { get; set; }
}

/// <summary>RPC data type for CopilotUserResponseOrganizationListItem operations.</summary>
public sealed class CopilotUserResponseOrganizationListItem
{
    /// <summary>Gets or sets the <c>login</c> value.</summary>
    [JsonPropertyName("login")]
    public string? Login { get; set; }

    /// <summary>Gets or sets the <c>name</c> value.</summary>
    [JsonPropertyName("name")]
    public string? Name { get; set; }
}

/// <summary>Schema for the `CopilotUserResponseQuotaSnapshotsChat` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CopilotUserResponseQuotaSnapshotsChat
{
    /// <summary>Gets or sets the <c>entitlement</c> value.</summary>
    [JsonPropertyName("entitlement")]
    public double? Entitlement { get; set; }

    /// <summary>Gets or sets the <c>has_quota</c> value.</summary>
    [JsonPropertyName("has_quota")]
    public bool? HasQuota { get; set; }

    /// <summary>Gets or sets the <c>overage_count</c> value.</summary>
    [JsonPropertyName("overage_count")]
    public double? OverageCount { get; set; }

    /// <summary>Gets or sets the <c>overage_permitted</c> value.</summary>
    [JsonPropertyName("overage_permitted")]
    public bool? OveragePermitted { get; set; }

    /// <summary>Gets or sets the <c>percent_remaining</c> value.</summary>
    [JsonPropertyName("percent_remaining")]
    public double? PercentRemaining { get; set; }

    /// <summary>Gets or sets the <c>quota_id</c> value.</summary>
    [JsonPropertyName("quota_id")]
    public string? QuotaId { get; set; }

    /// <summary>Gets or sets the <c>quota_remaining</c> value.</summary>
    [JsonPropertyName("quota_remaining")]
    public double? QuotaRemaining { get; set; }

    /// <summary>Gets or sets the <c>quota_reset_at</c> value.</summary>
    [JsonPropertyName("quota_reset_at")]
    public double? QuotaResetAt { get; set; }

    /// <summary>Gets or sets the <c>remaining</c> value.</summary>
    [JsonPropertyName("remaining")]
    public double? Remaining { get; set; }

    /// <summary>Gets or sets the <c>timestamp_utc</c> value.</summary>
    [JsonPropertyName("timestamp_utc")]
    public string? TimestampUtc { get; set; }

    /// <summary>Gets or sets the <c>token_based_billing</c> value.</summary>
    [JsonPropertyName("token_based_billing")]
    public bool? TokenBasedBilling { get; set; }

    /// <summary>Gets or sets the <c>unlimited</c> value.</summary>
    [JsonPropertyName("unlimited")]
    public bool? Unlimited { get; set; }
}

/// <summary>Schema for the `CopilotUserResponseQuotaSnapshotsCompletions` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CopilotUserResponseQuotaSnapshotsCompletions
{
    /// <summary>Gets or sets the <c>entitlement</c> value.</summary>
    [JsonPropertyName("entitlement")]
    public double? Entitlement { get; set; }

    /// <summary>Gets or sets the <c>has_quota</c> value.</summary>
    [JsonPropertyName("has_quota")]
    public bool? HasQuota { get; set; }

    /// <summary>Gets or sets the <c>overage_count</c> value.</summary>
    [JsonPropertyName("overage_count")]
    public double? OverageCount { get; set; }

    /// <summary>Gets or sets the <c>overage_permitted</c> value.</summary>
    [JsonPropertyName("overage_permitted")]
    public bool? OveragePermitted { get; set; }

    /// <summary>Gets or sets the <c>percent_remaining</c> value.</summary>
    [JsonPropertyName("percent_remaining")]
    public double? PercentRemaining { get; set; }

    /// <summary>Gets or sets the <c>quota_id</c> value.</summary>
    [JsonPropertyName("quota_id")]
    public string? QuotaId { get; set; }

    /// <summary>Gets or sets the <c>quota_remaining</c> value.</summary>
    [JsonPropertyName("quota_remaining")]
    public double? QuotaRemaining { get; set; }

    /// <summary>Gets or sets the <c>quota_reset_at</c> value.</summary>
    [JsonPropertyName("quota_reset_at")]
    public double? QuotaResetAt { get; set; }

    /// <summary>Gets or sets the <c>remaining</c> value.</summary>
    [JsonPropertyName("remaining")]
    public double? Remaining { get; set; }

    /// <summary>Gets or sets the <c>timestamp_utc</c> value.</summary>
    [JsonPropertyName("timestamp_utc")]
    public string? TimestampUtc { get; set; }

    /// <summary>Gets or sets the <c>token_based_billing</c> value.</summary>
    [JsonPropertyName("token_based_billing")]
    public bool? TokenBasedBilling { get; set; }

    /// <summary>Gets or sets the <c>unlimited</c> value.</summary>
    [JsonPropertyName("unlimited")]
    public bool? Unlimited { get; set; }
}

/// <summary>Schema for the `CopilotUserResponseQuotaSnapshotsPremiumInteractions` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CopilotUserResponseQuotaSnapshotsPremiumInteractions
{
    /// <summary>Gets or sets the <c>entitlement</c> value.</summary>
    [JsonPropertyName("entitlement")]
    public double? Entitlement { get; set; }

    /// <summary>Gets or sets the <c>has_quota</c> value.</summary>
    [JsonPropertyName("has_quota")]
    public bool? HasQuota { get; set; }

    /// <summary>Gets or sets the <c>overage_count</c> value.</summary>
    [JsonPropertyName("overage_count")]
    public double? OverageCount { get; set; }

    /// <summary>Gets or sets the <c>overage_permitted</c> value.</summary>
    [JsonPropertyName("overage_permitted")]
    public bool? OveragePermitted { get; set; }

    /// <summary>Gets or sets the <c>percent_remaining</c> value.</summary>
    [JsonPropertyName("percent_remaining")]
    public double? PercentRemaining { get; set; }

    /// <summary>Gets or sets the <c>quota_id</c> value.</summary>
    [JsonPropertyName("quota_id")]
    public string? QuotaId { get; set; }

    /// <summary>Gets or sets the <c>quota_remaining</c> value.</summary>
    [JsonPropertyName("quota_remaining")]
    public double? QuotaRemaining { get; set; }

    /// <summary>Gets or sets the <c>quota_reset_at</c> value.</summary>
    [JsonPropertyName("quota_reset_at")]
    public double? QuotaResetAt { get; set; }

    /// <summary>Gets or sets the <c>remaining</c> value.</summary>
    [JsonPropertyName("remaining")]
    public double? Remaining { get; set; }

    /// <summary>Gets or sets the <c>timestamp_utc</c> value.</summary>
    [JsonPropertyName("timestamp_utc")]
    public string? TimestampUtc { get; set; }

    /// <summary>Gets or sets the <c>token_based_billing</c> value.</summary>
    [JsonPropertyName("token_based_billing")]
    public bool? TokenBasedBilling { get; set; }

    /// <summary>Gets or sets the <c>unlimited</c> value.</summary>
    [JsonPropertyName("unlimited")]
    public bool? Unlimited { get; set; }
}

/// <summary>Schema for the `CopilotUserResponseQuotaSnapshots` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CopilotUserResponseQuotaSnapshots
{
    /// <summary>Schema for the `CopilotUserResponseQuotaSnapshotsChat` type.</summary>
    [JsonPropertyName("chat")]
    public CopilotUserResponseQuotaSnapshotsChat? Chat { get; set; }

    /// <summary>Schema for the `CopilotUserResponseQuotaSnapshotsCompletions` type.</summary>
    [JsonPropertyName("completions")]
    public CopilotUserResponseQuotaSnapshotsCompletions? Completions { get; set; }

    /// <summary>Schema for the `CopilotUserResponseQuotaSnapshotsPremiumInteractions` type.</summary>
    [JsonPropertyName("premium_interactions")]
    public CopilotUserResponseQuotaSnapshotsPremiumInteractions? PremiumInteractions { get; set; }
}

/// <summary>Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CopilotUserResponse
{
    /// <summary>Gets or sets the <c>access_type_sku</c> value.</summary>
    [JsonPropertyName("access_type_sku")]
    public string? AccessTypeSku { get; set; }

    /// <summary>Gets or sets the <c>analytics_tracking_id</c> value.</summary>
    [JsonPropertyName("analytics_tracking_id")]
    public string? AnalyticsTrackingId { get; set; }

    /// <summary>Gets or sets the <c>assigned_date</c> value.</summary>
    [JsonPropertyName("assigned_date")]
    public string? AssignedDate { get; set; }

    /// <summary>Gets or sets the <c>can_signup_for_limited</c> value.</summary>
    [JsonPropertyName("can_signup_for_limited")]
    public bool? CanSignupForLimited { get; set; }

    /// <summary>Gets or sets the <c>chat_enabled</c> value.</summary>
    [JsonPropertyName("chat_enabled")]
    public bool? ChatEnabled { get; set; }

    /// <summary>Gets or sets the <c>cli_remote_control_enabled</c> value.</summary>
    [JsonPropertyName("cli_remote_control_enabled")]
    public bool? CliRemoteControlEnabled { get; set; }

    /// <summary>Gets or sets the <c>cloud_session_storage_enabled</c> value.</summary>
    [JsonPropertyName("cloud_session_storage_enabled")]
    public bool? CloudSessionStorageEnabled { get; set; }

    /// <summary>Gets or sets the <c>codex_agent_enabled</c> value.</summary>
    [JsonPropertyName("codex_agent_enabled")]
    public bool? CodexAgentEnabled { get; set; }

    /// <summary>Gets or sets the <c>copilot_plan</c> value.</summary>
    [JsonPropertyName("copilot_plan")]
    public string? CopilotPlan { get; set; }

    /// <summary>Gets or sets the <c>copilotignore_enabled</c> value.</summary>
    [JsonPropertyName("copilotignore_enabled")]
    public bool? CopilotignoreEnabled { get; set; }

    /// <summary>Schema for the `CopilotUserResponseEndpoints` type.</summary>
    [JsonPropertyName("endpoints")]
    public CopilotUserResponseEndpoints? Endpoints { get; set; }

    /// <summary>Gets or sets the <c>is_mcp_enabled</c> value.</summary>
    [JsonPropertyName("is_mcp_enabled")]
    public bool? IsMcpEnabled { get; set; }

    /// <summary>Gets or sets the <c>limited_user_quotas</c> value.</summary>
    [JsonPropertyName("limited_user_quotas")]
    public IDictionary<string, double>? LimitedUserQuotas { get; set; }

    /// <summary>Gets or sets the <c>limited_user_reset_date</c> value.</summary>
    [JsonPropertyName("limited_user_reset_date")]
    public string? LimitedUserResetDate { get; set; }

    /// <summary>Gets or sets the <c>login</c> value.</summary>
    [JsonPropertyName("login")]
    public string? Login { get; set; }

    /// <summary>Gets or sets the <c>monthly_quotas</c> value.</summary>
    [JsonPropertyName("monthly_quotas")]
    public IDictionary<string, double>? MonthlyQuotas { get; set; }

    /// <summary>Gets or sets the <c>organization_list</c> value.</summary>
    [JsonPropertyName("organization_list")]
    public IList<CopilotUserResponseOrganizationListItem?>? OrganizationList { get; set; }

    /// <summary>Gets or sets the <c>organization_login_list</c> value.</summary>
    [JsonPropertyName("organization_login_list")]
    public IList<string>? OrganizationLoginList { get; set; }

    /// <summary>Gets or sets the <c>quota_reset_date</c> value.</summary>
    [JsonPropertyName("quota_reset_date")]
    public string? QuotaResetDate { get; set; }

    /// <summary>Gets or sets the <c>quota_reset_date_utc</c> value.</summary>
    [JsonPropertyName("quota_reset_date_utc")]
    public string? QuotaResetDateUtc { get; set; }

    /// <summary>Schema for the `CopilotUserResponseQuotaSnapshots` type.</summary>
    [JsonPropertyName("quota_snapshots")]
    public CopilotUserResponseQuotaSnapshots? QuotaSnapshots { get; set; }

    /// <summary>Gets or sets the <c>restricted_telemetry</c> value.</summary>
    [JsonPropertyName("restricted_telemetry")]
    public bool? RestrictedTelemetry { get; set; }

    /// <summary>Gets or sets the <c>token_based_billing</c> value.</summary>
    [JsonPropertyName("token_based_billing")]
    public bool? TokenBasedBilling { get; set; }
}

/// <summary>Schema for the `HMACAuthInfo` type.</summary>
/// <remarks>The <c>hmac</c> variant of <see cref="AuthInfo"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class AuthInfoHmac : AuthInfo
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "hmac";

    /// <summary>Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("copilotUser")]
    public CopilotUserResponse? CopilotUser { get; set; }

    /// <summary>HMAC secret used to sign requests.</summary>
    [JsonPropertyName("hmac")]
    public required string Hmac { get; set; }

    /// <summary>Authentication host. HMAC auth always targets the public GitHub host.</summary>
    [JsonPropertyName("host")]
    public required string Host { get; set; }
}

/// <summary>Schema for the `EnvAuthInfo` type.</summary>
/// <remarks>The <c>env</c> variant of <see cref="AuthInfo"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class AuthInfoEnv : AuthInfo
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "env";

    /// <summary>Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("copilotUser")]
    public CopilotUserResponse? CopilotUser { get; set; }

    /// <summary>Name of the environment variable the token was sourced from.</summary>
    [JsonPropertyName("envVar")]
    public required string EnvVar { get; set; }

    /// <summary>Authentication host (e.g. https://github.com or a GHES host).</summary>
    [JsonPropertyName("host")]
    public required string Host { get; set; }

    /// <summary>User login associated with the token. Undefined for server-to-server tokens (those starting with `ghs_`).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("login")]
    public string? Login { get; set; }

    /// <summary>The token value itself. Treat as a secret.</summary>
    [JsonPropertyName("token")]
    public required string Token { get; set; }
}

/// <summary>Schema for the `TokenAuthInfo` type.</summary>
/// <remarks>The <c>token</c> variant of <see cref="AuthInfo"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class AuthInfoToken : AuthInfo
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "token";

    /// <summary>Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("copilotUser")]
    public CopilotUserResponse? CopilotUser { get; set; }

    /// <summary>Authentication host.</summary>
    [JsonPropertyName("host")]
    public required string Host { get; set; }

    /// <summary>The token value itself. Treat as a secret.</summary>
    [JsonPropertyName("token")]
    public required string Token { get; set; }
}

/// <summary>Schema for the `CopilotApiTokenAuthInfo` type.</summary>
/// <remarks>The <c>copilot-api-token</c> variant of <see cref="AuthInfo"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class AuthInfoCopilotApiToken : AuthInfo
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "copilot-api-token";

    /// <summary>Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("copilotUser")]
    public CopilotUserResponse? CopilotUser { get; set; }

    /// <summary>Authentication host (always the public GitHub host).</summary>
    [JsonPropertyName("host")]
    public required string Host { get; set; }
}

/// <summary>Schema for the `UserAuthInfo` type.</summary>
/// <remarks>The <c>user</c> variant of <see cref="AuthInfo"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class AuthInfoUser : AuthInfo
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "user";

    /// <summary>Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("copilotUser")]
    public CopilotUserResponse? CopilotUser { get; set; }

    /// <summary>Authentication host.</summary>
    [JsonPropertyName("host")]
    public required string Host { get; set; }

    /// <summary>OAuth user login.</summary>
    [JsonPropertyName("login")]
    public required string Login { get; set; }
}

/// <summary>Schema for the `GhCliAuthInfo` type.</summary>
/// <remarks>The <c>gh-cli</c> variant of <see cref="AuthInfo"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class AuthInfoGhCli : AuthInfo
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "gh-cli";

    /// <summary>Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("copilotUser")]
    public CopilotUserResponse? CopilotUser { get; set; }

    /// <summary>Authentication host.</summary>
    [JsonPropertyName("host")]
    public required string Host { get; set; }

    /// <summary>User login as reported by `gh auth status`.</summary>
    [JsonPropertyName("login")]
    public required string Login { get; set; }

    /// <summary>The token returned by `gh auth token`. Treat as a secret.</summary>
    [JsonPropertyName("token")]
    public required string Token { get; set; }
}

/// <summary>Schema for the `ApiKeyAuthInfo` type.</summary>
/// <remarks>The <c>api-key</c> variant of <see cref="AuthInfo"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class AuthInfoApiKey : AuthInfo
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "api-key";

    /// <summary>The API key. Treat as a secret.</summary>
    [JsonPropertyName("apiKey")]
    public required string ApiKey { get; set; }

    /// <summary>Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("copilotUser")]
    public CopilotUserResponse? CopilotUser { get; set; }

    /// <summary>Authentication host.</summary>
    [JsonPropertyName("host")]
    public required string Host { get; set; }
}

/// <summary>New auth credentials to install on the session. Omit to leave credentials unchanged.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionSetCredentialsParams
{
    /// <summary>The new auth credentials to install on the session. When omitted or `undefined`, the call is a no-op and the session's existing credentials are preserved. The runtime stores the value verbatim and uses it for outbound model/API requests; it does NOT re-validate or re-fetch the associated Copilot user response. Several variants carry secret material; treat this method's params as containing secrets at rest and in transit.</summary>
    [JsonPropertyName("credentials")]
    public AuthInfo? Credentials { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Canvas action that the agent or host can invoke. To discover the input schema for a particular action, call the list_canvas_capabilities tool.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasAction
{
    /// <summary>Description of the action.</summary>
    [JsonPropertyName("description")]
    public string? Description { get; set; }

    /// <summary>JSON Schema for the action input.</summary>
    [JsonPropertyName("inputSchema")]
    public JsonElement? InputSchema { get; set; }

    /// <summary>Action name exposed by the canvas provider.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>Canvas available in the current session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class DiscoveredCanvas
{
    /// <summary>Actions the agent or host may invoke on an open instance.</summary>
    [JsonPropertyName("actions")]
    public IList<CanvasAction>? Actions { get; set; }

    /// <summary>Provider-local canvas identifier.</summary>
    [JsonPropertyName("canvasId")]
    public string CanvasId { get; set; } = string.Empty;

    /// <summary>Short, single-sentence description shown to the agent in canvas catalogs.</summary>
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    /// <summary>Human-readable canvas name.</summary>
    [JsonPropertyName("displayName")]
    public string DisplayName { get; set; } = string.Empty;

    /// <summary>Owning provider identifier.</summary>
    [JsonPropertyName("extensionId")]
    public string ExtensionId { get; set; } = string.Empty;

    /// <summary>Owning extension display name, when available.</summary>
    [JsonPropertyName("extensionName")]
    public string? ExtensionName { get; set; }

    /// <summary>JSON Schema for canvas open input.</summary>
    [JsonPropertyName("inputSchema")]
    public JsonElement? InputSchema { get; set; }
}

/// <summary>Declared canvases available in this session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasList
{
    /// <summary>Declared canvases available in this session.</summary>
    [JsonPropertyName("canvases")]
    public IList<DiscoveredCanvas> Canvases { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionCanvasListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Open canvas instance snapshot.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class OpenCanvasInstance
{
    /// <summary>Runtime-controlled routing state for an open canvas instance.</summary>
    [JsonPropertyName("availability")]
    public CanvasInstanceAvailability Availability { get; set; }

    /// <summary>Provider-local canvas identifier.</summary>
    [JsonPropertyName("canvasId")]
    public string CanvasId { get; set; } = string.Empty;

    /// <summary>Owning provider identifier.</summary>
    [JsonPropertyName("extensionId")]
    public string ExtensionId { get; set; } = string.Empty;

    /// <summary>Owning extension display name, when available.</summary>
    [JsonPropertyName("extensionName")]
    public string? ExtensionName { get; set; }

    /// <summary>Input supplied when the instance was opened.</summary>
    [JsonPropertyName("input")]
    public JsonElement? Input { get; set; }

    /// <summary>Stable caller-supplied canvas instance identifier.</summary>
    [JsonPropertyName("instanceId")]
    public string InstanceId { get; set; } = string.Empty;

    /// <summary>Whether this snapshot came from an idempotent reopen.</summary>
    [JsonPropertyName("reopen")]
    public bool Reopen { get; set; }

    /// <summary>Provider-supplied status text.</summary>
    [JsonPropertyName("status")]
    public string? Status { get; set; }

    /// <summary>Rendered title.</summary>
    [JsonPropertyName("title")]
    public string? Title { get; set; }

    /// <summary>URL for web-rendered canvases.</summary>
    [JsonPropertyName("url")]
    public string? Url { get; set; }
}

/// <summary>Live open-canvas snapshot.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasListOpenResult
{
    /// <summary>Currently open canvas instances.</summary>
    [JsonPropertyName("openCanvases")]
    public IList<OpenCanvasInstance> OpenCanvases { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionCanvasListOpenRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Canvas open parameters.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class CanvasOpenRequest
{
    /// <summary>Provider-local canvas identifier.</summary>
    [JsonPropertyName("canvasId")]
    public string CanvasId { get; set; } = string.Empty;

    /// <summary>Owning provider identifier. Optional when the canvasId is unique across providers; required to disambiguate when multiple providers register the same canvasId.</summary>
    [JsonPropertyName("extensionId")]
    public string? ExtensionId { get; set; }

    /// <summary>Canvas open input.</summary>
    [JsonPropertyName("input")]
    public JsonElement? Input { get; set; }

    /// <summary>Caller-supplied stable instance identifier.</summary>
    [JsonPropertyName("instanceId")]
    public string InstanceId { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Canvas close parameters.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class CanvasCloseRequest
{
    /// <summary>Open canvas instance identifier.</summary>
    [JsonPropertyName("instanceId")]
    public string InstanceId { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Canvas action invocation result.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasInvokeActionResult
{
    /// <summary>Provider-supplied action result.</summary>
    [JsonPropertyName("result")]
    public JsonElement? Result { get; set; }
}

/// <summary>Canvas action invocation parameters.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class CanvasInvokeActionRequest
{
    /// <summary>Action name to invoke.</summary>
    [JsonPropertyName("actionName")]
    public string ActionName { get; set; } = string.Empty;

    /// <summary>Action input.</summary>
    [JsonPropertyName("input")]
    public JsonElement? Input { get; set; }

    /// <summary>Open canvas instance identifier.</summary>
    [JsonPropertyName("instanceId")]
    public string InstanceId { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>The currently selected model and reasoning effort for the session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CurrentModel
{
    /// <summary>Currently active model identifier.</summary>
    [JsonPropertyName("modelId")]
    public string? ModelId { get; set; }

    /// <summary>Reasoning effort level currently applied to the active model, when one is set. Reads `Session.getReasoningEffort()` synchronously after `getSelectedModel()` resolves so the two values are reported as a snapshot.</summary>
    [JsonPropertyName("reasoningEffort")]
    public string? ReasoningEffort { get; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionModelGetCurrentRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>The model identifier active on the session after the switch.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ModelSwitchToResult
{
    /// <summary>Currently active model identifier after the switch.</summary>
    [JsonPropertyName("modelId")]
    public string? ModelId { get; set; }
}

/// <summary>Vision-specific limits.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ModelCapabilitiesOverrideLimitsVision
{
    /// <summary>Maximum image size in bytes.</summary>
    [JsonPropertyName("max_prompt_image_size")]
    public long? MaxPromptImageSize { get; set; }

    /// <summary>Maximum number of images per prompt.</summary>
    [JsonPropertyName("max_prompt_images")]
    public long? MaxPromptImages { get; set; }

    /// <summary>MIME types the model accepts.</summary>
    [JsonPropertyName("supported_media_types")]
    public IList<string>? SupportedMediaTypes { get; set; }
}

/// <summary>Token limits for prompts, outputs, and context window.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ModelCapabilitiesOverrideLimits
{
    /// <summary>Maximum total context window size in tokens.</summary>
    [JsonPropertyName("max_context_window_tokens")]
    public long? MaxContextWindowTokens { get; set; }

    /// <summary>Maximum number of output/completion tokens.</summary>
    [JsonPropertyName("max_output_tokens")]
    public long? MaxOutputTokens { get; set; }

    /// <summary>Maximum number of prompt/input tokens.</summary>
    [JsonPropertyName("max_prompt_tokens")]
    public long? MaxPromptTokens { get; set; }

    /// <summary>Vision-specific limits.</summary>
    [JsonPropertyName("vision")]
    public ModelCapabilitiesOverrideLimitsVision? Vision { get; set; }
}

/// <summary>Feature flags indicating what the model supports.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ModelCapabilitiesOverrideSupports
{
    /// <summary>Whether this model supports reasoning effort configuration.</summary>
    [JsonPropertyName("reasoningEffort")]
    public bool? ReasoningEffort { get; set; }

    /// <summary>Whether this model supports vision/image input.</summary>
    [JsonPropertyName("vision")]
    public bool? Vision { get; set; }
}

/// <summary>Override individual model capabilities resolved by the runtime.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ModelCapabilitiesOverride
{
    /// <summary>Token limits for prompts, outputs, and context window.</summary>
    [JsonPropertyName("limits")]
    public ModelCapabilitiesOverrideLimits? Limits { get; set; }

    /// <summary>Feature flags indicating what the model supports.</summary>
    [JsonPropertyName("supports")]
    public ModelCapabilitiesOverrideSupports? Supports { get; set; }
}

/// <summary>Target model identifier and optional reasoning effort, summary, and capability overrides.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class ModelSwitchToRequest
{
    /// <summary>Override individual model capabilities resolved by the runtime.</summary>
    [JsonPropertyName("modelCapabilities")]
    public ModelCapabilitiesOverride? ModelCapabilities { get; set; }

    /// <summary>Model identifier to switch to.</summary>
    [JsonPropertyName("modelId")]
    public string ModelId { get; set; } = string.Empty;

    /// <summary>Reasoning effort level to use for the model. "none" disables reasoning.</summary>
    [JsonPropertyName("reasoningEffort")]
    public string? ReasoningEffort { get; set; }

    /// <summary>Reasoning summary mode to request for supported model clients.</summary>
    [JsonPropertyName("reasoningSummary")]
    public ReasoningSummary? ReasoningSummary { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Update the session's reasoning effort without changing the selected model. Use `switchTo` instead when you also need to change the model. The runtime stores the effort on the session and applies it to subsequent turns.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ModelSetReasoningEffortResult
{
    /// <summary>Reasoning effort level recorded on the session after the update.</summary>
    [JsonPropertyName("reasoningEffort")]
    public string ReasoningEffort { get; set; } = string.Empty;
}

/// <summary>Reasoning effort level to apply to the currently selected model.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class ModelSetReasoningEffortRequest
{
    /// <summary>Reasoning effort level to apply to the currently selected model. The host is responsible for validating the value against the model's supported levels before calling.</summary>
    [JsonPropertyName("reasoningEffort")]
    public string ReasoningEffort { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionModeGetRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Agent interaction mode to apply to the session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class ModeSetRequest
{
    /// <summary>The session mode the agent is operating in.</summary>
    [JsonPropertyName("mode")]
    public SessionMode Mode { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>The session's friendly name, or null when not yet set.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class NameGetResult
{
    /// <summary>The session name (user-set or auto-generated), or null if not yet set.</summary>
    [JsonPropertyName("name")]
    public string? Name { get; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionNameGetRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>New friendly name to apply to the session.</summary>
[Experimental(Diagnostics.Experimental)]
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

/// <summary>Indicates whether the auto-generated summary was applied as the session's name.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class NameSetAutoResult
{
    /// <summary>Whether the auto-generated summary was persisted. False if the session already has a user-set name, the summary normalized to empty, or the session does not have a workspace.</summary>
    [JsonPropertyName("applied")]
    public bool Applied { get; set; }
}

/// <summary>Auto-generated session summary to apply as the session's name when no user-set name exists.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class NameSetAutoRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Auto-generated session summary. Empty/whitespace-only values are ignored; values are trimmed before persisting.</summary>
    [JsonPropertyName("summary")]
    public string Summary { get; set; } = string.Empty;
}

/// <summary>Existence, contents, and resolved path of the session plan file.</summary>
[Experimental(Diagnostics.Experimental)]
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

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionPlanReadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Replacement contents to write to the session plan file.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PlanUpdateRequest
{
    /// <summary>The new content for the plan file.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>Gets or sets the <c>client_name</c> value.</summary>
    [JsonPropertyName("client_name")]
    public string? ClientName { get; set; }

    /// <summary>Gets or sets the <c>created_at</c> value.</summary>
    [JsonPropertyName("created_at")]
    public DateTimeOffset? CreatedAt { get; set; }

    /// <summary>Gets or sets the <c>cwd</c> value.</summary>
    [JsonPropertyName("cwd")]
    public string? Cwd { get; set; }

    /// <summary>Gets or sets the <c>git_root</c> value.</summary>
    [JsonPropertyName("git_root")]
    public string? GitRoot { get; set; }

    /// <summary>Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.</summary>
    [JsonPropertyName("host_type")]
    public WorkspacesWorkspaceDetailsHostType? HostType { get; set; }

    /// <summary>Gets or sets the <c>id</c> value.</summary>
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

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

    /// <summary>Gets or sets the <c>summary_count</c> value.</summary>
    [JsonPropertyName("summary_count")]
    public long? SummaryCount { get; set; }

    /// <summary>Gets or sets the <c>updated_at</c> value.</summary>
    [JsonPropertyName("updated_at")]
    public DateTimeOffset? UpdatedAt { get; set; }

    /// <summary>Gets or sets the <c>user_named</c> value.</summary>
    [JsonPropertyName("user_named")]
    public bool? UserNamed { get; set; }
}

/// <summary>Current workspace metadata for the session, including its absolute filesystem path when available.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class WorkspacesGetWorkspaceResult
{
    /// <summary>Absolute filesystem path to the workspace directory. Omitted when the session has no workspace (e.g. remote sessions).</summary>
    [JsonPropertyName("path")]
    public string? Path { get; set; }

    /// <summary>Current workspace metadata, or null if not available.</summary>
    [JsonPropertyName("workspace")]
    public WorkspacesGetWorkspaceResultWorkspace? Workspace { get; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionWorkspacesGetWorkspaceRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Relative paths of files stored in the session workspace files directory.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class WorkspacesListFilesResult
{
    /// <summary>Relative file paths in the workspace files directory.</summary>
    [JsonPropertyName("files")]
    public IList<string> Files { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionWorkspacesListFilesRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Contents of the requested workspace file as a UTF-8 string.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class WorkspacesReadFileResult
{
    /// <summary>File content as a UTF-8 string.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;
}

/// <summary>Relative path of the workspace file to read.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class WorkspacesReadFileRequest
{
    /// <summary>Relative path within the workspace files directory.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Relative path and UTF-8 content for the workspace file to create or overwrite.</summary>
[Experimental(Diagnostics.Experimental)]
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

/// <summary>Schema for the `WorkspacesCheckpoints` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class WorkspacesCheckpoints
{
    /// <summary>Filename of the checkpoint within the workspace checkpoints directory.</summary>
    [JsonPropertyName("filename")]
    public string Filename { get; set; } = string.Empty;

    /// <summary>Checkpoint number assigned by the workspace manager.</summary>
    [JsonPropertyName("number")]
    public long Number { get; set; }

    /// <summary>Human-readable checkpoint title.</summary>
    [JsonPropertyName("title")]
    public string Title { get; set; } = string.Empty;
}

/// <summary>Workspace checkpoints in chronological order; empty when the workspace is not enabled.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class WorkspacesListCheckpointsResult
{
    /// <summary>Workspace checkpoints in chronological order. Empty when workspace is not enabled.</summary>
    [JsonPropertyName("checkpoints")]
    public IList<WorkspacesCheckpoints> Checkpoints { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionWorkspacesListCheckpointsRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class WorkspacesReadCheckpointResult
{
    /// <summary>Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing.</summary>
    [JsonPropertyName("content")]
    public string? Content { get; set; }
}

/// <summary>Checkpoint number to read.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class WorkspacesReadCheckpointRequest
{
    /// <summary>Checkpoint number to read.</summary>
    [JsonPropertyName("number")]
    public long Number { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for WorkspacesSaveLargePasteResultSaved operations.</summary>
public sealed class WorkspacesSaveLargePasteResultSaved
{
    /// <summary>Filename within the workspace files directory.</summary>
    [JsonPropertyName("filename")]
    public string Filename { get; set; } = string.Empty;

    /// <summary>Absolute filesystem path to the saved paste file.</summary>
    [JsonPropertyName("filePath")]
    public string FilePath { get; set; } = string.Empty;

    /// <summary>Size of the saved file in bytes.</summary>
    [JsonPropertyName("sizeBytes")]
    public long SizeBytes { get; set; }
}

/// <summary>Descriptor for the saved paste file, or null when the workspace is unavailable.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class WorkspacesSaveLargePasteResult
{
    /// <summary>Saved-paste descriptor, or null when the workspace is unavailable (e.g. CCA runtime, non-infinite sessions, remote sessions).</summary>
    [JsonPropertyName("saved")]
    public WorkspacesSaveLargePasteResultSaved? Saved { get; set; }
}

/// <summary>Pasted content to save as a UTF-8 file in the session workspace.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class WorkspacesSaveLargePasteRequest
{
    /// <summary>Pasted content to save as a UTF-8 file.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>A single changed file and its unified diff.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class WorkspaceDiffFileChange
{
    /// <summary>Type of change represented by this file diff.</summary>
    [JsonPropertyName("changeType")]
    public WorkspaceDiffFileChangeType ChangeType { get; set; }

    /// <summary>Unified diff content for the file. Empty when the diff was truncated.</summary>
    [JsonPropertyName("diff")]
    public string Diff { get; set; } = string.Empty;

    /// <summary>Whether the diff content was omitted because it exceeded the per-file size limit.</summary>
    [JsonPropertyName("isTruncated")]
    public bool? IsTruncated { get; set; }

    /// <summary>Original file path for renamed files.</summary>
    [JsonPropertyName("oldPath")]
    public string? OldPath { get; set; }

    /// <summary>Path to the changed file, relative to the workspace root.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;
}

/// <summary>Workspace diff result for the requested mode.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class WorkspaceDiffResult
{
    /// <summary>Default branch used for a branch diff, when branch mode was requested.</summary>
    [JsonPropertyName("baseBranch")]
    public string? BaseBranch { get; set; }

    /// <summary>Changed files and their unified diffs.</summary>
    [JsonPropertyName("changes")]
    public IList<WorkspaceDiffFileChange> Changes { get => field ??= []; set; }

    /// <summary>Whether a requested branch diff fell back to unstaged changes because branch diff failed.</summary>
    [JsonPropertyName("isFallback")]
    public bool IsFallback { get; set; }

    /// <summary>Effective mode used for the returned changes.</summary>
    [JsonPropertyName("mode")]
    public WorkspaceDiffMode Mode { get; set; }

    /// <summary>Diff mode requested by the client.</summary>
    [JsonPropertyName("requestedMode")]
    public WorkspaceDiffMode RequestedMode { get; set; }
}

/// <summary>Parameters for computing a workspace diff.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class WorkspacesDiffRequest
{
    /// <summary>Diff mode requested by the client.</summary>
    [JsonPropertyName("mode")]
    public WorkspaceDiffMode Mode { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Schema for the `InstructionsSources` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class InstructionsSources
{
    /// <summary>Glob pattern(s) from frontmatter — when set, this instruction applies only to matching files.</summary>
    [JsonPropertyName("applyTo")]
    public IList<string>? ApplyTo { get; set; }

    /// <summary>Raw content of the instruction file.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>When true, this source starts disabled and must be toggled on by the user.</summary>
    [JsonPropertyName("defaultDisabled")]
    public bool? DefaultDisabled { get; set; }

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

/// <summary>Instruction sources loaded for the session, in merge order.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class InstructionsGetSourcesResult
{
    /// <summary>Instruction sources for the session.</summary>
    [JsonPropertyName("sources")]
    public IList<InstructionsSources> Sources { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionInstructionsGetSourcesRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether fleet mode was successfully activated.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class FleetStartResult
{
    /// <summary>Whether fleet mode was successfully activated.</summary>
    [JsonPropertyName("started")]
    public bool Started { get; set; }
}

/// <summary>Optional user prompt to combine with the fleet orchestration instructions.</summary>
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

/// <summary>Schema for the `AgentInfo` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AgentInfo
{
    /// <summary>Description of the agent's purpose.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    /// <summary>Human-readable display name.</summary>
    [JsonPropertyName("displayName")]
    public string DisplayName { get; set; } = string.Empty;

    /// <summary>Stable identifier for selection. For most agents this is the same as `name`; for plugin/builtin agents it may differ. Always populated; defaults to `name` when no distinct id was assigned.</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>MCP server configurations attached to this agent, keyed by server name. Server config shape mirrors the MCP `mcpServers` schema.</summary>
    [Experimental(Diagnostics.Experimental)]
    [JsonPropertyName("mcpServers")]
    public IDictionary<string, JsonElement>? McpServers { get; set; }

    /// <summary>Preferred model id for this agent. When omitted, inherits the outer agent's model.</summary>
    [JsonPropertyName("model")]
    public string? Model { get; set; }

    /// <summary>Unique identifier of the custom agent.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Absolute local file path of the agent definition. Only set for file-based agents loaded from disk; remote agents do not have a path.</summary>
    [JsonPropertyName("path")]
    public string? Path { get; set; }

    /// <summary>Skill names preloaded into this agent's context. Omitted means none.</summary>
    [JsonPropertyName("skills")]
    public IList<string>? Skills { get; set; }

    /// <summary>Where the agent definition was loaded from.</summary>
    [JsonPropertyName("source")]
    public AgentInfoSource? Source { get; set; }

    /// <summary>Allowed tool names for this agent. Empty array means none; omitted means inherit defaults.</summary>
    [JsonPropertyName("tools")]
    public IList<string>? Tools { get; set; }

    /// <summary>Whether the agent can be selected directly by the user. Agents marked `false` are subagent-only.</summary>
    [JsonPropertyName("userInvocable")]
    public bool? UserInvocable { get; set; }
}

/// <summary>Custom agents available to the session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AgentList
{
    /// <summary>Available custom agents.</summary>
    [JsonPropertyName("agents")]
    public IList<AgentInfo> Agents { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionAgentListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>The currently selected custom agent, or null when using the default agent.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AgentGetCurrentResult
{
    /// <summary>Currently selected custom agent, or null if using the default agent.</summary>
    [JsonPropertyName("agent")]
    public AgentInfo? Agent { get; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionAgentGetCurrentRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>The newly selected custom agent.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AgentSelectResult
{
    /// <summary>The newly selected custom agent.</summary>
    [JsonPropertyName("agent")]
    public AgentInfo Agent { get => field ??= new(); set; }
}

/// <summary>Name of the custom agent to select for subsequent turns.</summary>
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

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionAgentDeselectRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Custom agents available to the session after reloading definitions from disk.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AgentReloadResult
{
    /// <summary>Reloaded custom agents.</summary>
    [JsonPropertyName("agents")]
    public IList<AgentInfo> Agents { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionAgentReloadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Identifier assigned to the newly started background agent task.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksStartAgentResult
{
    /// <summary>Generated agent ID for the background task.</summary>
    [JsonPropertyName("agentId")]
    public string AgentId { get; set; } = string.Empty;
}

/// <summary>Agent type, prompt, name, and optional description and model override for the new task.</summary>
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

/// <summary>Schema for the `TaskInfo` type.</summary>
/// <remarks>Polymorphic base type discriminated by <c>type</c>.</remarks>
[Experimental(Diagnostics.Experimental)]
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


/// <summary>Schema for the `TaskAgentInfo` type.</summary>
/// <remarks>The <c>agent</c> variant of <see cref="TaskInfo"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
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
    public TimeSpan? ActiveTime { get; set; }

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

    /// <summary>Whether task execution is synchronously awaited or managed in the background.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("executionMode")]
    public TaskExecutionMode? ExecutionMode { get; set; }

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
    public required TaskStatus Status { get; set; }

    /// <summary>Tool call ID associated with this agent task.</summary>
    [JsonPropertyName("toolCallId")]
    public required string ToolCallId { get; set; }
}

/// <summary>Schema for the `TaskShellInfo` type.</summary>
/// <remarks>The <c>shell</c> variant of <see cref="TaskInfo"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>Whether task execution is synchronously awaited or managed in the background.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("executionMode")]
    public TaskExecutionMode? ExecutionMode { get; set; }

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
    public required TaskStatus Status { get; set; }
}

/// <summary>Background tasks currently tracked by the session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TaskList
{
    /// <summary>Currently tracked tasks.</summary>
    [JsonPropertyName("tasks")]
    public IList<TaskInfo> Tasks { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionTasksListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Refresh metadata for any detached background shells the runtime knows about. Use after a long pause to pick up exit/output state for shells running outside the agent loop.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksRefreshResult
{
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionTasksRefreshRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Wait until all in-flight background tasks (agents + shells) and any follow-up turns scheduled by their completions have settled. Returns when the runtime is fully drained or after an internal timeout (default 10 minutes; configurable via COPILOT_TASK_WAIT_TIMEOUT_SECONDS).</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksWaitForPendingResult
{
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionTasksWaitForPendingRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Polymorphic base type discriminated by <c>type</c>.</summary>
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "type",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(TasksGetProgressResultProgressAgent), "agent")]
[JsonDerivedType(typeof(TasksGetProgressResultProgressShell), "shell")]
public partial class TasksGetProgressResultProgress
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("type")]
    public virtual string Type { get; set; } = string.Empty;
}


/// <summary>Schema for the `TaskProgressLine` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TaskProgressLine
{
    /// <summary>Display message, e.g., "▸ bash", "✓ edit src/foo.ts".</summary>
    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    /// <summary>ISO 8601 timestamp when this event occurred.</summary>
    [JsonPropertyName("timestamp")]
    public DateTimeOffset Timestamp { get; set; }
}

/// <summary>Schema for the `TaskAgentProgress` type.</summary>
/// <remarks>The <c>agent</c> variant of <see cref="TasksGetProgressResultProgress"/>.</remarks>
public partial class TasksGetProgressResultProgressAgent : TasksGetProgressResultProgress
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "agent";

    /// <summary>The most recent intent reported by the agent.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("latestIntent")]
    public string? LatestIntent { get; set; }

    /// <summary>Recent tool execution events converted to display lines.</summary>
    [JsonPropertyName("recentActivity")]
    public required IList<TaskProgressLine> RecentActivity { get; set; }
}

/// <summary>Schema for the `TaskShellProgress` type.</summary>
/// <remarks>The <c>shell</c> variant of <see cref="TasksGetProgressResultProgress"/>.</remarks>
public partial class TasksGetProgressResultProgressShell : TasksGetProgressResultProgress
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "shell";

    /// <summary>Process ID when available.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("pid")]
    public long? Pid { get; set; }

    /// <summary>Recent stdout/stderr lines from the running shell command.</summary>
    [JsonPropertyName("recentOutput")]
    public required string RecentOutput { get; set; }
}

/// <summary>Progress information for the task, or null when no task with that ID is tracked.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksGetProgressResult
{
    /// <summary>Progress information for the task, discriminated by type. Returns null when no task with this ID is currently tracked.</summary>
    [JsonPropertyName("progress")]
    public TasksGetProgressResultProgress? Progress { get; set; }
}

/// <summary>Identifier of the background task to fetch progress for.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class TasksGetProgressRequest
{
    /// <summary>Task identifier (agent ID or shell ID).</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>The first sync-waiting task that can currently be promoted to background mode.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksGetCurrentPromotableResult
{
    /// <summary>The first sync-waiting task (agent first, then shell) that can currently be promoted to background mode. Omitted if no such task exists. The returned task is guaranteed to have executionMode='sync' and canPromoteToBackground=true at the time of the call.</summary>
    [JsonPropertyName("task")]
    public TaskInfo? Task { get; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionTasksGetCurrentPromotableRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the task was successfully promoted to background mode.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksPromoteToBackgroundResult
{
    /// <summary>Whether the task was successfully promoted to background mode.</summary>
    [JsonPropertyName("promoted")]
    public bool Promoted { get; set; }
}

/// <summary>Identifier of the task to promote to background mode.</summary>
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

/// <summary>The promoted task as it now exists in background mode, omitted if no promotable task was waiting.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksPromoteCurrentToBackgroundResult
{
    /// <summary>The promoted task as it now exists in background mode, omitted if no promotable task was waiting. Atomic operation: avoids the race window of getCurrentPromotable + promoteToBackground.</summary>
    [JsonPropertyName("task")]
    public TaskInfo? Task { get; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionTasksPromoteCurrentToBackgroundRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the background task was successfully cancelled.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksCancelResult
{
    /// <summary>Whether the task was successfully cancelled.</summary>
    [JsonPropertyName("cancelled")]
    public bool Cancelled { get; set; }
}

/// <summary>Identifier of the background task to cancel.</summary>
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

/// <summary>Indicates whether the task was removed. False when the task does not exist or is still running/idle.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksRemoveResult
{
    /// <summary>Whether the task was removed. Returns false if the task does not exist or is still running/idle (cancel it first).</summary>
    [JsonPropertyName("removed")]
    public bool Removed { get; set; }
}

/// <summary>Identifier of the completed or cancelled task to remove from tracking.</summary>
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

/// <summary>Indicates whether the message was delivered, with an error message when delivery failed.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksSendMessageResult
{
    /// <summary>Error message if delivery failed.</summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }

    /// <summary>Whether the message was successfully delivered or steered.</summary>
    [JsonPropertyName("sent")]
    public bool Sent { get; set; }
}

/// <summary>Identifier of the target agent task, message content, and optional sender agent ID.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class TasksSendMessageRequest
{
    /// <summary>Agent ID of the sender, if sent on behalf of another agent.</summary>
    [JsonPropertyName("fromAgentId")]
    public string? FromAgentId { get; set; }

    /// <summary>Agent task identifier.</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Message content to send to the agent.</summary>
    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Schema for the `Skill` type.</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>Name of the plugin that provides the skill, when source is 'plugin'.</summary>
    [JsonPropertyName("pluginName")]
    public string? PluginName { get; set; }

    /// <summary>Source location type (e.g., project, personal-copilot, plugin, builtin).</summary>
    [JsonPropertyName("source")]
    public SkillSource Source { get; set; }

    /// <summary>Whether the skill can be invoked by the user as a slash command.</summary>
    [JsonPropertyName("userInvocable")]
    public bool UserInvocable { get; set; }
}

/// <summary>Skills available to the session, with their enabled state.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SkillList
{
    /// <summary>Available skills.</summary>
    [JsonPropertyName("skills")]
    public IList<Skill> Skills { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionSkillsListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Schema for the `SkillsInvokedSkill` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SkillsInvokedSkill
{
    /// <summary>Tools that should be auto-approved when this skill is active, captured at invocation time.</summary>
    [JsonPropertyName("allowedTools")]
    public IList<string>? AllowedTools { get; set; }

    /// <summary>Full content of the skill file.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Turn number when the skill was invoked.</summary>
    [JsonPropertyName("invokedAtTurn")]
    public long InvokedAtTurn { get; set; }

    /// <summary>Unique identifier for the skill.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Path to the SKILL.md file.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;
}

/// <summary>Skills invoked during this session, ordered by invocation time (most recent last).</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SkillsGetInvokedResult
{
    /// <summary>Skills invoked during this session, ordered by invocation time (most recent last).</summary>
    [JsonPropertyName("skills")]
    public IList<SkillsInvokedSkill> Skills { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionSkillsGetInvokedRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Name of the skill to enable for the session.</summary>
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

/// <summary>Name of the skill to disable for the session.</summary>
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

/// <summary>Diagnostics from reloading skill definitions, with warnings and errors as separate lists.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SkillsLoadDiagnostics
{
    /// <summary>Errors emitted while loading skills (e.g. skills that failed to load entirely).</summary>
    [JsonPropertyName("errors")]
    public IList<string> Errors { get => field ??= []; set; }

    /// <summary>Warnings emitted while loading skills (e.g. skills that loaded but had issues).</summary>
    [JsonPropertyName("warnings")]
    public IList<string> Warnings { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionSkillsReloadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionSkillsEnsureLoadedRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Schema for the `McpServer` type.</summary>
[Experimental(Diagnostics.Experimental)]
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

/// <summary>MCP servers configured for the session, with their connection status.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpServerList
{
    /// <summary>Configured MCP servers.</summary>
    [JsonPropertyName("servers")]
    public IList<McpServer> Servers { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionMcpListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Name of the MCP server to enable for the session.</summary>
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

/// <summary>Name of the MCP server to disable for the session.</summary>
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

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionMcpReloadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>MCP CreateMessageResult payload (with optional 'tools' extension), present when action='success'. Treated as opaque at the schema layer; consumers should construct/consume it per the MCP CreateMessageResult shape.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpExecuteSamplingResult
{
}

/// <summary>Outcome of an MCP sampling execution: success result, failure error, or cancellation.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpSamplingExecutionResult
{
    /// <summary>Outcome of the sampling inference. 'success' produced a response; 'failure' encountered an error (including agent-side rejection by content filter or criteria); 'cancelled' the caller cancelled this execution via cancelSamplingExecution.</summary>
    [JsonPropertyName("action")]
    public McpSamplingExecutionAction Action { get; set; }

    /// <summary>Error description, present when action='failure'.</summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }

    /// <summary>MCP CreateMessageResult payload (with optional 'tools' extension), present when action='success'. Treated as opaque at the schema layer; consumers should construct/consume it per the MCP CreateMessageResult shape.</summary>
    [JsonPropertyName("result")]
    public McpExecuteSamplingResult? Result { get; set; }
}

/// <summary>Raw MCP CreateMessageRequest params, as received in the `sampling.requested` event. Treated as opaque at the schema layer; the runtime converts the embedded MCP messages into the OpenAI chat-completion shape internally.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpExecuteSamplingRequest
{
}

/// <summary>Identifiers and raw MCP CreateMessageRequest params used to run a sampling inference.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class McpExecuteSamplingParams
{
    /// <summary>The original MCP JSON-RPC request ID (string or number). Used by the runtime to correlate the inference with the originating MCP request for telemetry; this is distinct from `requestId` (which is the schema-level cancellation handle).</summary>
    [JsonPropertyName("mcpRequestId")]
    public JsonElement McpRequestId { get; set; }

    /// <summary>Raw MCP CreateMessageRequest params, as received in the `sampling.requested` event. Treated as opaque at the schema layer; the runtime converts the embedded MCP messages into the OpenAI chat-completion shape internally.</summary>
    [JsonPropertyName("request")]
    public McpExecuteSamplingRequest Request { get => field ??= new(); set; }

    /// <summary>Caller-provided unique identifier for this sampling execution. Use this same ID with cancelSamplingExecution to cancel the in-flight call. Must be unique within the session for the lifetime of the call.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Name of the MCP server that initiated the sampling request.</summary>
    [JsonPropertyName("serverName")]
    public string ServerName { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether an in-flight sampling execution with the given requestId was found and cancelled.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpCancelSamplingExecutionResult
{
    /// <summary>True if an in-flight execution with the given requestId was found and signalled to cancel. False when no such execution is in flight (already completed, never started, or cancelled by another caller).</summary>
    [JsonPropertyName("cancelled")]
    public bool Cancelled { get; set; }
}

/// <summary>The requestId previously passed to executeSampling that should be cancelled.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class McpCancelSamplingExecutionParams
{
    /// <summary>The requestId previously passed to executeSampling that should be cancelled.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Env-value mode recorded on the session after the update.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpSetEnvValueModeResult
{
    /// <summary>Mode recorded on the session after the update.</summary>
    [JsonPropertyName("mode")]
    public McpSetEnvValueModeDetails Mode { get; set; }
}

/// <summary>Mode controlling how MCP server env values are resolved (`direct` or `indirect`).</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class McpSetEnvValueModeParams
{
    /// <summary>How environment-variable values supplied to MCP servers are resolved. "direct" passes literal string values; "indirect" treats values as references (e.g. names of environment variables on the host) that the runtime resolves before launch. Defaults to the runtime's startup mode; clients that intentionally launch MCP servers with literal values (e.g. CLI prompt mode and ACP) set this to "direct".</summary>
    [JsonPropertyName("mode")]
    public McpSetEnvValueModeDetails Mode { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the auto-managed `github` MCP server was removed (false when nothing to remove).</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpRemoveGitHubResult
{
    /// <summary>True when the auto-managed `github` MCP server was removed; false when no removal happened (e.g. user has explicitly configured a `github` server, or the server was not registered).</summary>
    [JsonPropertyName("removed")]
    public bool Removed { get; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionMcpRemoveGitHubRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpOauthLoginResult
{
    /// <summary>URL the caller should open in a browser to complete OAuth. Omitted when cached tokens were still valid and no browser interaction was needed — the server is already reconnected in that case. When present, the runtime starts the callback listener before returning and continues the flow in the background; completion is signaled via session.mcp_server_status_changed.</summary>
    [Url]
    [StringSyntax(StringSyntaxAttribute.Uri)]
    [JsonPropertyName("authorizationUrl")]
    public string? AuthorizationUrl { get; set; }
}

/// <summary>Remote MCP server name and optional overrides controlling reauthentication, OAuth client display name, and the callback success-page copy.</summary>
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

/// <summary>Schema for the `McpAppsResourceContent` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpAppsResourceContent
{
    /// <summary>Resource-level metadata (CSP, permissions, etc.).</summary>
    [JsonPropertyName("_meta")]
    public IDictionary<string, JsonElement>? _meta { get; set; }

    /// <summary>Base64-encoded binary content.</summary>
    [JsonPropertyName("blob")]
    public string? Blob { get; set; }

    /// <summary>MIME type of the content.</summary>
    [JsonPropertyName("mimeType")]
    public string? MimeType { get; set; }

    /// <summary>Text content (e.g. HTML).</summary>
    [JsonPropertyName("text")]
    public string? Text { get; set; }

    /// <summary>The resource URI (typically ui://...).</summary>
    [JsonPropertyName("uri")]
    public string Uri { get; set; } = string.Empty;
}

/// <summary>Resource contents returned by the MCP server.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpAppsReadResourceResult
{
    /// <summary>Resource contents returned by the server.</summary>
    [JsonPropertyName("contents")]
    public IList<McpAppsResourceContent> Contents { get => field ??= []; set; }
}

/// <summary>MCP server and resource URI to fetch.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class McpAppsReadResourceRequest
{
    /// <summary>Name of the MCP server hosting the resource.</summary>
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("serverName")]
    public string ServerName { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Resource URI (typically ui://...).</summary>
    [JsonPropertyName("uri")]
    public string Uri { get; set; } = string.Empty;
}

/// <summary>App-callable tools from the named MCP server.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpAppsListToolsResult
{
    /// <summary>App-callable tools from the server.</summary>
    [JsonPropertyName("tools")]
    public IList<IDictionary<string, JsonElement>> Tools { get => field ??= []; set; }
}

/// <summary>MCP server to list app-callable tools for.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class McpAppsListToolsRequest
{
    /// <summary>**Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.</summary>
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("originServerName")]
    public string OriginServerName { get; set; } = string.Empty;

    /// <summary>MCP server hosting the app.</summary>
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("serverName")]
    public string ServerName { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>MCP server, tool name, and arguments to invoke from an MCP App view.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class McpAppsCallToolRequest
{
    /// <summary>Tool arguments.</summary>
    [JsonPropertyName("arguments")]
    public IDictionary<string, JsonElement>? Arguments { get; set; }

    /// <summary>**Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.</summary>
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("originServerName")]
    public string OriginServerName { get; set; } = string.Empty;

    /// <summary>MCP server hosting the tool.</summary>
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("serverName")]
    public string ServerName { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>MCP tool name.</summary>
    [JsonPropertyName("toolName")]
    public string ToolName { get; set; } = string.Empty;
}

/// <summary>Host context advertised to MCP App guests.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpAppsSetHostContextDetails
{
    /// <summary>Display modes the host supports.</summary>
    [JsonPropertyName("availableDisplayModes")]
    public IList<McpAppsSetHostContextDetailsAvailableDisplayMode>? AvailableDisplayModes { get; set; }

    /// <summary>Current display mode (SEP-1865).</summary>
    [JsonPropertyName("displayMode")]
    public McpAppsSetHostContextDetailsDisplayMode? DisplayMode { get; set; }

    /// <summary>BCP-47 locale, e.g. 'en-US'.</summary>
    [JsonPropertyName("locale")]
    public string? Locale { get; set; }

    /// <summary>Platform type for responsive design.</summary>
    [JsonPropertyName("platform")]
    public McpAppsSetHostContextDetailsPlatform? Platform { get; set; }

    /// <summary>UI theme preference per SEP-1865.</summary>
    [JsonPropertyName("theme")]
    public McpAppsSetHostContextDetailsTheme? Theme { get; set; }

    /// <summary>IANA timezone, e.g. 'America/New_York'.</summary>
    [JsonPropertyName("timeZone")]
    public string? TimeZone { get; set; }

    /// <summary>Host application identifier.</summary>
    [JsonPropertyName("userAgent")]
    public string? UserAgent { get; set; }
}

/// <summary>Host context to advertise to MCP App guests.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class McpAppsSetHostContextRequest
{
    /// <summary>Host context advertised to MCP App guests.</summary>
    [JsonPropertyName("context")]
    public McpAppsSetHostContextDetails Context { get => field ??= new(); set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Current host context.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpAppsHostContextDetails
{
    /// <summary>Display modes the host supports.</summary>
    [JsonPropertyName("availableDisplayModes")]
    public IList<McpAppsHostContextDetailsAvailableDisplayMode>? AvailableDisplayModes { get; set; }

    /// <summary>Current display mode (SEP-1865).</summary>
    [JsonPropertyName("displayMode")]
    public McpAppsHostContextDetailsDisplayMode? DisplayMode { get; set; }

    /// <summary>BCP-47 locale, e.g. 'en-US'.</summary>
    [JsonPropertyName("locale")]
    public string? Locale { get; set; }

    /// <summary>Platform type for responsive design.</summary>
    [JsonPropertyName("platform")]
    public McpAppsHostContextDetailsPlatform? Platform { get; set; }

    /// <summary>UI theme preference per SEP-1865.</summary>
    [JsonPropertyName("theme")]
    public McpAppsHostContextDetailsTheme? Theme { get; set; }

    /// <summary>IANA timezone, e.g. 'America/New_York'.</summary>
    [JsonPropertyName("timeZone")]
    public string? TimeZone { get; set; }

    /// <summary>Host application identifier.</summary>
    [JsonPropertyName("userAgent")]
    public string? UserAgent { get; set; }
}

/// <summary>Current host context advertised to MCP App guests.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpAppsHostContext
{
    /// <summary>Current host context.</summary>
    [JsonPropertyName("context")]
    public McpAppsHostContextDetails Context { get => field ??= new(); set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionMcpAppsGetHostContextRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Capability negotiation snapshot.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpAppsDiagnoseCapability
{
    /// <summary>Whether the runtime advertises `extensions.io.modelcontextprotocol/ui` to MCP servers.</summary>
    [JsonPropertyName("advertised")]
    public bool Advertised { get; set; }

    /// <summary>Whether the MCP_APPS feature flag (or COPILOT_MCP_APPS env override) is on.</summary>
    [JsonPropertyName("featureFlagEnabled")]
    public bool FeatureFlagEnabled { get; set; }

    /// <summary>Whether the session has the `mcp-apps` capability.</summary>
    [JsonPropertyName("sessionHasMcpApps")]
    public bool SessionHasMcpApps { get; set; }
}

/// <summary>What the server returned for this session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpAppsDiagnoseServer
{
    /// <summary>Whether the named server is currently connected.</summary>
    [JsonPropertyName("connected")]
    public bool Connected { get; set; }

    /// <summary>Up to 5 tool names with `_meta.ui` for quick inspection.</summary>
    [JsonPropertyName("sampleToolNames")]
    public IList<string> SampleToolNames { get => field ??= []; set; }

    /// <summary>Total tools returned by the server's tools/list.</summary>
    [JsonPropertyName("toolCount")]
    public double ToolCount { get; set; }

    /// <summary>Tools whose `_meta.ui` is populated (resourceUri and/or visibility set).</summary>
    [JsonPropertyName("toolsWithUiMeta")]
    public double ToolsWithUiMeta { get; set; }
}

/// <summary>Diagnostic snapshot of MCP Apps wiring for the named server.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpAppsDiagnoseResult
{
    /// <summary>Capability negotiation snapshot.</summary>
    [JsonPropertyName("capability")]
    public McpAppsDiagnoseCapability Capability { get => field ??= new(); set; }

    /// <summary>What the server returned for this session.</summary>
    [JsonPropertyName("server")]
    public McpAppsDiagnoseServer Server { get => field ??= new(); set; }
}

/// <summary>MCP server to diagnose MCP Apps wiring for.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class McpAppsDiagnoseRequest
{
    /// <summary>MCP server to probe.</summary>
    [RegularExpression("^[^\\x00-\\x1f/\\x7f-\\x9f}]+(?:\\/[^\\x00-\\x1f/\\x7f-\\x9f}]+)*$")]
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("serverName")]
    public string ServerName { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Schema for the `Plugin` type.</summary>
[Experimental(Diagnostics.Experimental)]
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

/// <summary>Plugins installed for the session, with their enabled state and version metadata.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PluginList
{
    /// <summary>Installed plugins.</summary>
    [JsonPropertyName("plugins")]
    public IList<Plugin> Plugins { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionPluginsListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the session options patch was applied successfully.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionUpdateOptionsResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Schema for the `SessionInstalledPlugin` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionInstalledPlugin
{
    /// <summary>Path where the plugin is cached locally.</summary>
    [JsonPropertyName("cache_path")]
    public string? CachePath { get; set; }

    /// <summary>Whether the plugin is currently enabled.</summary>
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }

    /// <summary>Installation timestamp (ISO-8601).</summary>
    [JsonPropertyName("installed_at")]
    public string InstalledAt { get; set; } = string.Empty;

    /// <summary>Marketplace the plugin came from (empty string for direct repo installs).</summary>
    [JsonPropertyName("marketplace")]
    public string Marketplace { get; set; } = string.Empty;

    /// <summary>Plugin name.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Source descriptor for direct repo installs (when marketplace is empty).</summary>
    [JsonPropertyName("source")]
    public JsonElement? Source { get; set; }

    /// <summary>Installed version, if known.</summary>
    [JsonPropertyName("version")]
    public string? Version { get; set; }
}

/// <summary>Patch of mutable session options to apply to the running session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionUpdateOptionsParams
{
    /// <summary>Additional content-exclusion policies to merge into the session's policy set. Opaque shape; see `ContentExclusionApiResponse` in the runtime.</summary>
    [Experimental(Diagnostics.Experimental)]
    [JsonPropertyName("additionalContentExclusionPolicies")]
    public IList<JsonElement>? AdditionalContentExclusionPolicies { get; set; }

    /// <summary>Runtime context discriminator (e.g., `cli`, `actions`).</summary>
    [JsonPropertyName("agentContext")]
    public string? AgentContext { get; set; }

    /// <summary>Whether to disable the `ask_user` tool (encourages autonomous behavior).</summary>
    [JsonPropertyName("askUserDisabled")]
    public bool? AskUserDisabled { get; set; }

    /// <summary>Allowlist of tool names available to this session.</summary>
    [JsonPropertyName("availableTools")]
    public IList<string>? AvailableTools { get; set; }

    /// <summary>Identifier of the client driving the session.</summary>
    [JsonPropertyName("clientName")]
    public string? ClientName { get; set; }

    /// <summary>Whether to include the `Co-authored-by` trailer in commit messages.</summary>
    [JsonPropertyName("coauthorEnabled")]
    public bool? CoauthorEnabled { get; set; }

    /// <summary>Whether to allow auto-mode continuation across turns.</summary>
    [JsonPropertyName("continueOnAutoMode")]
    public bool? ContinueOnAutoMode { get; set; }

    /// <summary>Override URL for the Copilot API endpoint.</summary>
    [JsonPropertyName("copilotUrl")]
    public string? CopilotUrl { get; set; }

    /// <summary>Whether to default custom agents to local-only execution.</summary>
    [JsonPropertyName("customAgentsLocalOnly")]
    public bool? CustomAgentsLocalOnly { get; set; }

    /// <summary>Instruction source IDs to exclude from the system prompt.</summary>
    [JsonPropertyName("disabledInstructionSources")]
    public IList<string>? DisabledInstructionSources { get; set; }

    /// <summary>Skill IDs that should be excluded from this session.</summary>
    [JsonPropertyName("disabledSkills")]
    public IList<string>? DisabledSkills { get; set; }

    /// <summary>Whether to discover custom instructions on demand after successful file views (AGENTS.md / CLAUDE.md / .github/copilot-instructions.md surfacing). Combined with `skipCustomInstructions` and the runtime-side `ON_DEMAND_INSTRUCTIONS` feature flag.</summary>
    [JsonPropertyName("enableOnDemandInstructionDiscovery")]
    public bool? EnableOnDemandInstructionDiscovery { get; set; }

    /// <summary>Whether to surface reasoning-summary events from the model.</summary>
    [JsonPropertyName("enableReasoningSummaries")]
    public bool? EnableReasoningSummaries { get; set; }

    /// <summary>Whether shell-script safety heuristics are enabled.</summary>
    [JsonPropertyName("enableScriptSafety")]
    public bool? EnableScriptSafety { get; set; }

    /// <summary>Whether to stream model responses.</summary>
    [JsonPropertyName("enableStreaming")]
    public bool? EnableStreaming { get; set; }

    /// <summary>How env values are passed to MCP servers (`direct` inlines literal values; `indirect` resolves at launch).</summary>
    [JsonPropertyName("envValueMode")]
    public OptionsUpdateEnvValueMode? EnvValueMode { get; set; }

    /// <summary>Override directory for the session-events log. When unset, the runtime's default events log directory is used.</summary>
    [JsonPropertyName("eventsLogDirectory")]
    public string? EventsLogDirectory { get; set; }

    /// <summary>Denylist of tool names for this session.</summary>
    [JsonPropertyName("excludedTools")]
    public IList<string>? ExcludedTools { get; set; }

    /// <summary>Map of feature-flag IDs to their boolean enabled state.</summary>
    [JsonPropertyName("featureFlags")]
    public IDictionary<string, bool>? FeatureFlags { get; set; }

    /// <summary>Full set of installed plugins for the session. Replaces the existing list; the runtime invalidates the skills cache only when the list materially changes.</summary>
    [JsonPropertyName("installedPlugins")]
    public IList<SessionInstalledPlugin>? InstalledPlugins { get; set; }

    /// <summary>Stable integration identifier used for analytics and rate-limit attribution.</summary>
    [JsonPropertyName("integrationId")]
    public string? IntegrationId { get; set; }

    /// <summary>Whether experimental capabilities are enabled.</summary>
    [JsonPropertyName("isExperimentalMode")]
    public bool? IsExperimentalMode { get; set; }

    /// <summary>Whether interactive shell sessions are logged.</summary>
    [JsonPropertyName("logInteractiveShells")]
    public bool? LogInteractiveShells { get; set; }

    /// <summary>Identifier sent to LSP-style integrations.</summary>
    [JsonPropertyName("lspClientName")]
    public string? LspClientName { get; set; }

    /// <summary>Whether to expose the `manage_schedule` tool to the agent. The runtime always owns the per-session schedule registry; this flag only controls tool exposure (typically gated to staff users).</summary>
    [JsonPropertyName("manageScheduleEnabled")]
    public bool? ManageScheduleEnabled { get; set; }

    /// <summary>The model ID to use for assistant turns.</summary>
    [JsonPropertyName("model")]
    public string? Model { get; set; }

    /// <summary>Custom model-provider configuration (BYOK). Opaque shape; see `ProviderConfig` in the runtime.</summary>
    [Experimental(Diagnostics.Experimental)]
    [JsonPropertyName("provider")]
    public JsonElement? Provider { get; set; }

    /// <summary>Reasoning effort for the selected model (model-defined enum).</summary>
    [JsonPropertyName("reasoningEffort")]
    public string? ReasoningEffort { get; set; }

    /// <summary>Whether the session is running in an interactive UI.</summary>
    [JsonPropertyName("runningInInteractiveMode")]
    public bool? RunningInInteractiveMode { get; set; }

    /// <summary>Sandbox configuration shape; opaque to SDK consumers. See `SandboxConfig` in the runtime.</summary>
    [Experimental(Diagnostics.Experimental)]
    [JsonPropertyName("sandboxConfig")]
    public JsonElement? SandboxConfig { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Shell init profile (`None` or `NonInteractive`).</summary>
    [JsonPropertyName("shellInitProfile")]
    public string? ShellInitProfile { get; set; }

    /// <summary>Per-shell process flags (e.g., `pwsh` arguments).</summary>
    [JsonPropertyName("shellProcessFlags")]
    public IList<string>? ShellProcessFlags { get; set; }

    /// <summary>Additional directories to search for skills.</summary>
    [JsonPropertyName("skillDirectories")]
    public IList<string>? SkillDirectories { get; set; }

    /// <summary>Whether to skip loading custom instruction sources.</summary>
    [JsonPropertyName("skipCustomInstructions")]
    public bool? SkipCustomInstructions { get; set; }

    /// <summary>Optional path for trajectory output.</summary>
    [JsonPropertyName("trajectoryFile")]
    public string? TrajectoryFile { get; set; }

    /// <summary>Absolute working-directory path for shell tools.</summary>
    [JsonPropertyName("workingDirectory")]
    public string? WorkingDirectory { get; set; }
}

/// <summary>Parameters for (re)loading the merged LSP configuration set.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class LspInitializeRequest
{
    /// <summary>Force re-initialization even when LSP configs were already loaded for the working directory.</summary>
    [JsonPropertyName("force")]
    public bool? Force { get; set; }

    /// <summary>Git root used as the boundary when traversing for project-level LSP configs (supports monorepos).</summary>
    [JsonPropertyName("gitRoot")]
    public string? GitRoot { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Working directory used to load project-level LSP configs. Defaults to the session working directory when omitted.</summary>
    [JsonPropertyName("workingDirectory")]
    public string? WorkingDirectory { get; set; }
}

/// <summary>Schema for the `Extension` type.</summary>
[Experimental(Diagnostics.Experimental)]
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

/// <summary>Extensions discovered for the session, with their current status.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ExtensionList
{
    /// <summary>Discovered extensions and their current status.</summary>
    [JsonPropertyName("extensions")]
    public IList<Extension> Extensions { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionExtensionsListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Source-qualified extension identifier to enable for the session.</summary>
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

/// <summary>Source-qualified extension identifier to disable for the session.</summary>
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

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionExtensionsReloadRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the external tool call result was handled successfully.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class HandlePendingToolCallResult
{
    /// <summary>Whether the tool call result was handled successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Pending external tool call request ID, with the tool result or an error describing why it failed.</summary>
[Experimental(Diagnostics.Experimental)]
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
    public JsonElement? Result { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Resolve, build, and validate the runtime tool list for this session. Subagent sessions and consumer flows that need an initialized tool set before `send` invoke this. Default base-class implementation is a no-op for sessions that don't support tool validation.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ToolsInitializeAndValidateResult
{
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionToolsInitializeAndValidateRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Optional unstructured input hint.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SlashCommandInput
{
    /// <summary>Optional completion hint for the input (e.g. 'directory' for filesystem path completion).</summary>
    [JsonPropertyName("completion")]
    public SlashCommandInputCompletion? Completion { get; set; }

    /// <summary>Hint to display when command input has not been provided.</summary>
    [JsonPropertyName("hint")]
    public string Hint { get; set; } = string.Empty;

    /// <summary>When true, clients should pass the full text after the command name as a single argument rather than splitting on whitespace.</summary>
    [JsonPropertyName("preserveMultilineInput")]
    public bool? PreserveMultilineInput { get; set; }

    /// <summary>When true, the command requires non-empty input; clients should render the input hint as required.</summary>
    [JsonPropertyName("required")]
    public bool? Required { get; set; }
}

/// <summary>Schema for the `SlashCommandInfo` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SlashCommandInfo
{
    /// <summary>Canonical aliases without leading slashes.</summary>
    [JsonPropertyName("aliases")]
    public IList<string>? Aliases { get; set; }

    /// <summary>Whether the command may run while an agent turn is active.</summary>
    [JsonPropertyName("allowDuringAgentExecution")]
    public bool AllowDuringAgentExecution { get; set; }

    /// <summary>Human-readable command description.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    /// <summary>Whether the command is experimental.</summary>
    [JsonPropertyName("experimental")]
    public bool? Experimental { get; set; }

    /// <summary>Optional unstructured input hint.</summary>
    [JsonPropertyName("input")]
    public SlashCommandInput? Input { get; set; }

    /// <summary>Coarse command category for grouping and behavior: runtime built-in, skill-backed command, or SDK/client-owned command.</summary>
    [JsonPropertyName("kind")]
    public SlashCommandKind Kind { get; set; }

    /// <summary>Canonical command name without a leading slash.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>Slash commands available in the session, after applying any include/exclude filters.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CommandList
{
    /// <summary>Commands available in this session.</summary>
    [JsonPropertyName("commands")]
    public IList<SlashCommandInfo> Commands { get => field ??= []; set; }
}

/// <summary>Optional filters controlling which command sources to include in the listing.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CommandsListRequest
{
    /// <summary>Include runtime built-in commands.</summary>
    [JsonPropertyName("includeBuiltins")]
    public bool? IncludeBuiltins { get; set; }

    /// <summary>Include commands registered by protocol clients, including SDK clients and extensions.</summary>
    [JsonPropertyName("includeClientCommands")]
    public bool? IncludeClientCommands { get; set; }

    /// <summary>Include enabled user-invocable skills and commands.</summary>
    [JsonPropertyName("includeSkills")]
    public bool? IncludeSkills { get; set; }
}

/// <summary>Optional filters controlling which command sources to include in the listing.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class CommandsListRequestWithSession
{
    /// <summary>Include runtime built-in commands.</summary>
    [JsonPropertyName("includeBuiltins")]
    public bool? IncludeBuiltins { get; set; }

    /// <summary>Include commands registered by protocol clients, including SDK clients and extensions.</summary>
    [JsonPropertyName("includeClientCommands")]
    public bool? IncludeClientCommands { get; set; }

    /// <summary>Include enabled user-invocable skills and commands.</summary>
    [JsonPropertyName("includeSkills")]
    public bool? IncludeSkills { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Result of invoking the slash command (text output, prompt to send to the agent, or completion).</summary>
/// <remarks>Polymorphic base type discriminated by <c>kind</c>.</remarks>
[Experimental(Diagnostics.Experimental)]
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "kind",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(SlashCommandInvocationResultText), "text")]
[JsonDerivedType(typeof(SlashCommandInvocationResultAgentPrompt), "agent-prompt")]
[JsonDerivedType(typeof(SlashCommandInvocationResultCompleted), "completed")]
[JsonDerivedType(typeof(SlashCommandInvocationResultSelectSubcommand), "select-subcommand")]
public partial class SlashCommandInvocationResult
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("kind")]
    public virtual string Kind { get; set; } = string.Empty;
}


/// <summary>Schema for the `SlashCommandTextResult` type.</summary>
/// <remarks>The <c>text</c> variant of <see cref="SlashCommandInvocationResult"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class SlashCommandInvocationResultText : SlashCommandInvocationResult
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "text";

    /// <summary>Whether text contains Markdown.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("markdown")]
    public bool? Markdown { get; set; }

    /// <summary>Whether ANSI sequences should be preserved.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("preserveAnsi")]
    public bool? PreserveAnsi { get; set; }

    /// <summary>True when the invocation mutated user runtime settings; consumers caching settings should refresh.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("runtimeSettingsChanged")]
    public bool? RuntimeSettingsChanged { get; set; }

    /// <summary>Text output for the client to render.</summary>
    [JsonPropertyName("text")]
    public required string Text { get; set; }
}

/// <summary>Schema for the `SlashCommandAgentPromptResult` type.</summary>
/// <remarks>The <c>agent-prompt</c> variant of <see cref="SlashCommandInvocationResult"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class SlashCommandInvocationResultAgentPrompt : SlashCommandInvocationResult
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "agent-prompt";

    /// <summary>Prompt text to display to the user.</summary>
    [JsonPropertyName("displayPrompt")]
    public required string DisplayPrompt { get; set; }

    /// <summary>Optional target session mode for the agent prompt.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("mode")]
    public SessionMode? Mode { get; set; }

    /// <summary>Prompt to submit to the agent.</summary>
    [JsonPropertyName("prompt")]
    public required string Prompt { get; set; }

    /// <summary>True when the invocation mutated user runtime settings; consumers caching settings should refresh.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("runtimeSettingsChanged")]
    public bool? RuntimeSettingsChanged { get; set; }
}

/// <summary>Schema for the `SlashCommandCompletedResult` type.</summary>
/// <remarks>The <c>completed</c> variant of <see cref="SlashCommandInvocationResult"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class SlashCommandInvocationResultCompleted : SlashCommandInvocationResult
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "completed";

    /// <summary>Optional user-facing message describing the completed command.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("message")]
    public string? Message { get; set; }

    /// <summary>True when the invocation mutated user runtime settings; consumers caching settings should refresh.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("runtimeSettingsChanged")]
    public bool? RuntimeSettingsChanged { get; set; }
}

/// <summary>Schema for the `SlashCommandSelectSubcommandOption` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SlashCommandSelectSubcommandOption
{
    /// <summary>Human-readable description of the subcommand.</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    /// <summary>Optional group label for organizing options.</summary>
    [JsonPropertyName("group")]
    public string? Group { get; set; }

    /// <summary>Subcommand name to invoke.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>Schema for the `SlashCommandSelectSubcommandResult` type.</summary>
/// <remarks>The <c>select-subcommand</c> variant of <see cref="SlashCommandInvocationResult"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class SlashCommandInvocationResultSelectSubcommand : SlashCommandInvocationResult
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "select-subcommand";

    /// <summary>Parent command name that requires subcommand selection.</summary>
    [JsonPropertyName("command")]
    public required string Command { get; set; }

    /// <summary>Available subcommand options for the client to present.</summary>
    [JsonPropertyName("options")]
    public required IList<SlashCommandSelectSubcommandOption> Options { get; set; }

    /// <summary>True when the invocation mutated user runtime settings; consumers caching settings should refresh.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("runtimeSettingsChanged")]
    public bool? RuntimeSettingsChanged { get; set; }

    /// <summary>Human-readable title for the selection UI.</summary>
    [JsonPropertyName("title")]
    public required string Title { get; set; }
}

/// <summary>Slash command name and optional raw input string to invoke.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class CommandsInvokeRequest
{
    /// <summary>Raw input after the command name.</summary>
    [JsonPropertyName("input")]
    public string? Input { get; set; }

    /// <summary>Command name. Leading slashes are stripped and the name is matched case-insensitively.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the pending client-handled command was completed successfully.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CommandsHandlePendingCommandResult
{
    /// <summary>Whether the command was handled successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Pending command request ID and an optional error if the client handler failed.</summary>
[Experimental(Diagnostics.Experimental)]
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

/// <summary>Error message produced while executing the command, if any.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ExecuteCommandResult
{
    /// <summary>Error message produced while executing the command, if any. Omitted when the handler succeeded.</summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }
}

/// <summary>Slash command name and argument string to execute synchronously.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class ExecuteCommandParams
{
    /// <summary>Argument string to pass to the command (empty string if none).</summary>
    [JsonPropertyName("args")]
    public string Args { get; set; } = string.Empty;

    /// <summary>Name of the slash command to invoke (without the leading '/').</summary>
    [JsonPropertyName("commandName")]
    public string CommandName { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the command was accepted into the local execution queue.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class EnqueueCommandResult
{
    /// <summary>True when the command was accepted into the local execution queue. False when the call targets a session that does not support local command queueing (e.g. remote sessions).</summary>
    [JsonPropertyName("queued")]
    public bool Queued { get; set; }
}

/// <summary>Slash-prefixed command string to enqueue for FIFO processing.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class EnqueueCommandParams
{
    /// <summary>Slash-prefixed command string to enqueue, e.g. '/compact' or '/model gpt-4'. Queued FIFO with any in-flight items; if the session is idle, processing kicks off immediately.</summary>
    [JsonPropertyName("command")]
    public string Command { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the queued-command response was matched to a pending request.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CommandsRespondToQueuedCommandResult
{
    /// <summary>Whether a pending queued command with the given request ID was found and resolved. False when the request was already resolved, cancelled, or unknown.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Result of the queued command execution.</summary>
/// <remarks>Data type discriminated by <c>handled</c>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class QueuedCommandResult
{
    /// <summary>The boolean discriminator.</summary>
    [JsonPropertyName("handled")]
    public bool Handled { get; set; }

    /// <summary>When true, the runtime will not process subsequent queued commands until a new request comes in.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("stopProcessingQueue")]
    public bool? StopProcessingQueue { get; set; }
}

/// <summary>Queued-command request ID and the result indicating whether the host executed it (and whether to stop processing further queued commands).</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class CommandsRespondToQueuedCommandRequest
{
    /// <summary>Request ID from the `command.queued` event the host is responding to.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Result of the queued command execution.</summary>
    [JsonPropertyName("result")]
    public QueuedCommandResult Result { get => field ??= new(); set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Feature override key/value pairs to attach to subsequent telemetry events from this session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class TelemetrySetFeatureOverridesRequest
{
    /// <summary>Override key/value pairs to attach to subsequent telemetry events from this session. Replaces any previously-set overrides.</summary>
    [JsonPropertyName("features")]
    public IDictionary<string, string> Features { get => field ??= new Dictionary<string, string>(); set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>The elicitation response (accept with form values, decline, or cancel).</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UIElicitationResponse
{
    /// <summary>The user's response: accept (submitted), decline (rejected), or cancel (dismissed).</summary>
    [JsonPropertyName("action")]
    public UIElicitationResponseAction Action { get; set; }

    /// <summary>The form values submitted by the user (present when action is 'accept').</summary>
    [JsonPropertyName("content")]
    public IDictionary<string, JsonElement>? Content { get; set; }
}

/// <summary>JSON Schema describing the form fields to present to the user.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UIElicitationSchema
{
    /// <summary>Form field definitions, keyed by field name.</summary>
    [JsonPropertyName("properties")]
    public IDictionary<string, JsonElement> Properties { get => field ??= new Dictionary<string, JsonElement>(); set; }

    /// <summary>List of required field names.</summary>
    [JsonPropertyName("required")]
    public IList<string>? Required { get; set; }

    /// <summary>Schema type indicator (always 'object').</summary>
    [JsonPropertyName("type")]
    public string Type { get; set; } = string.Empty;
}

/// <summary>Prompt message and JSON schema describing the form fields to elicit from the user.</summary>
[Experimental(Diagnostics.Experimental)]
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

/// <summary>Indicates whether the elicitation response was accepted; false if it was already resolved by another client.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UIElicitationResult
{
    /// <summary>Whether the response was accepted. False if the request was already resolved by another client.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Pending elicitation request ID and the user's response (accept/decline/cancel + form values).</summary>
[Experimental(Diagnostics.Experimental)]
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

/// <summary>Indicates whether the pending UI request was resolved by this call.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UIHandlePendingResult
{
    /// <summary>True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Schema for the `UIUserInputResponse` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UIUserInputResponse
{
    /// <summary>The user's answer text.</summary>
    [JsonPropertyName("answer")]
    public string Answer { get; set; } = string.Empty;

    /// <summary>True if the user typed a freeform response, false if they selected a presented choice. Used by telemetry to differentiate between free text input and choice selection.</summary>
    [JsonPropertyName("wasFreeform")]
    public bool WasFreeform { get; set; }
}

/// <summary>Request ID of a pending `user_input.requested` event and the user's response.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class UIHandlePendingUserInputRequest
{
    /// <summary>The unique request ID from the user_input.requested event.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Schema for the `UIUserInputResponse` type.</summary>
    [JsonPropertyName("response")]
    public UIUserInputResponse Response { get => field ??= new(); set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Optional sampling result payload. Omit to reject/cancel the sampling request without providing a result.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UIHandlePendingSamplingResponse
{
}

/// <summary>Request ID of a pending `sampling.requested` event and an optional sampling result payload (omit to reject).</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class UIHandlePendingSamplingRequest
{
    /// <summary>The unique request ID from the sampling.requested event.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Optional sampling result payload. Omit to reject/cancel the sampling request without providing a result.</summary>
    [JsonPropertyName("response")]
    public UIHandlePendingSamplingResponse? Response { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Request ID of a pending `auto_mode_switch.requested` event and the user's response.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class UIHandlePendingAutoModeSwitchRequest
{
    /// <summary>The unique request ID from the auto_mode_switch.requested event.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>User's choice for auto-mode switching: yes (allow this turn), yes_always (allow + persist as setting), or no (decline).</summary>
    [JsonPropertyName("response")]
    public UIAutoModeSwitchResponse Response { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Schema for the `UIExitPlanModeResponse` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UIExitPlanModeResponse
{
    /// <summary>Whether the plan was approved.</summary>
    [JsonPropertyName("approved")]
    public bool Approved { get; set; }

    /// <summary>Whether subsequent edits should be auto-approved without confirmation.</summary>
    [JsonPropertyName("autoApproveEdits")]
    public bool? AutoApproveEdits { get; set; }

    /// <summary>Feedback from the user when they declined the plan or requested changes.</summary>
    [JsonPropertyName("feedback")]
    public string? Feedback { get; set; }

    /// <summary>The action the user selected. Defaults to 'autopilot' when autoApproveEdits is true, otherwise 'interactive'.</summary>
    [JsonPropertyName("selectedAction")]
    public UIExitPlanModeAction? SelectedAction { get; set; }
}

/// <summary>Request ID of a pending `exit_plan_mode.requested` event and the user's response.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class UIHandlePendingExitPlanModeRequest
{
    /// <summary>The unique request ID from the exit_plan_mode.requested event.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Schema for the `UIExitPlanModeResponse` type.</summary>
    [JsonPropertyName("response")]
    public UIExitPlanModeResponse Response { get => field ??= new(); set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Register an in-process handler for `auto_mode_switch.requested` events. The caller still attaches the actual listener via the standard event-subscription mechanism; this registration solely tells the server bridge to skip its own dispatch (so a remote client doesn't race the in-process handler for the same requestId).</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UIRegisterDirectAutoModeSwitchHandlerResult
{
    /// <summary>Opaque handle representing the registration. Pass this same handle to `unregisterDirectAutoModeSwitchHandler` when the in-process handler is no longer active. Multiple registrations are reference-counted; the server bridge will only dispatch auto-mode-switch requests when no handles are active.</summary>
    [JsonPropertyName("handle")]
    public string Handle { get; set; } = string.Empty;
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionUiRegisterDirectAutoModeSwitchHandlerRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the handle was active and the registration count was decremented.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UIUnregisterDirectAutoModeSwitchHandlerResult
{
    /// <summary>True if the handle was active and decremented the counter; false if the handle was unknown.</summary>
    [JsonPropertyName("unregistered")]
    public bool Unregistered { get; set; }
}

/// <summary>Opaque handle previously returned by `registerDirectAutoModeSwitchHandler` to release.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class UIUnregisterDirectAutoModeSwitchHandlerRequest
{
    /// <summary>Handle previously returned by `registerDirectAutoModeSwitchHandler`.</summary>
    [JsonPropertyName("handle")]
    public string Handle { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the operation succeeded.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsConfigureResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Schema for the `PermissionsConfigureAdditionalContentExclusionPolicyRuleSource` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsConfigureAdditionalContentExclusionPolicyRuleSource
{
    /// <summary>Gets or sets the <c>name</c> value.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Gets or sets the <c>type</c> value.</summary>
    [JsonPropertyName("type")]
    public string Type { get; set; } = string.Empty;
}

/// <summary>Schema for the `PermissionsConfigureAdditionalContentExclusionPolicyRule` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsConfigureAdditionalContentExclusionPolicyRule
{
    /// <summary>Gets or sets the <c>ifAnyMatch</c> value.</summary>
    [JsonPropertyName("ifAnyMatch")]
    public IList<string>? IfAnyMatch { get; set; }

    /// <summary>Gets or sets the <c>ifNoneMatch</c> value.</summary>
    [JsonPropertyName("ifNoneMatch")]
    public IList<string>? IfNoneMatch { get; set; }

    /// <summary>Gets or sets the <c>paths</c> value.</summary>
    [JsonPropertyName("paths")]
    public IList<string> Paths { get => field ??= []; set; }

    /// <summary>Schema for the `PermissionsConfigureAdditionalContentExclusionPolicyRuleSource` type.</summary>
    [JsonPropertyName("source")]
    public PermissionsConfigureAdditionalContentExclusionPolicyRuleSource Source { get => field ??= new(); set; }
}

/// <summary>Schema for the `PermissionsConfigureAdditionalContentExclusionPolicy` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsConfigureAdditionalContentExclusionPolicy
{
    /// <summary>Gets or sets the <c>last_updated_at</c> value.</summary>
    [JsonPropertyName("last_updated_at")]
    public JsonElement LastUpdatedAt { get; set; }

    /// <summary>Gets or sets the <c>rules</c> value.</summary>
    [JsonPropertyName("rules")]
    public IList<PermissionsConfigureAdditionalContentExclusionPolicyRule> Rules { get => field ??= []; set; }

    /// <summary>Allowed values for the `PermissionsConfigureAdditionalContentExclusionPolicyScope` enumeration.</summary>
    [JsonPropertyName("scope")]
    public PermissionsConfigureAdditionalContentExclusionPolicyScope Scope { get; set; }
}

/// <summary>If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionPathsConfig
{
    /// <summary>Additional directories to allow tool access to (in addition to the session's working directory). When `unrestricted` is true, these are still pre-populated on the UnrestrictedPathManager so they remain visible via getDirectories() (e.g. for @-mention completion).</summary>
    [JsonPropertyName("additionalDirectories")]
    public IList<string>? AdditionalDirectories { get; set; }

    /// <summary>Whether to include the system temp directory in the allowed list (defaults to true). Ignored when `unrestricted` is true.</summary>
    [JsonPropertyName("includeTempDirectory")]
    public bool? IncludeTempDirectory { get; set; }

    /// <summary>If true, the runtime allows access to all paths without prompting. Equivalent to constructing an UnrestrictedPathManager.</summary>
    [JsonPropertyName("unrestricted")]
    public bool? Unrestricted { get; set; }

    /// <summary>Workspace root path (special-cased to be allowed even before the directory exists). Ignored when `unrestricted` is true.</summary>
    [JsonPropertyName("workspacePath")]
    public string? WorkspacePath { get; set; }
}

/// <summary>If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionRulesSet
{
    /// <summary>Rules that auto-approve matching requests.</summary>
    [JsonPropertyName("approved")]
    public IList<PermissionRule> Approved { get => field ??= []; set; }

    /// <summary>Rules that auto-deny matching requests.</summary>
    [JsonPropertyName("denied")]
    public IList<PermissionRule> Denied { get => field ??= []; set; }
}

/// <summary>If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionUrlsConfig
{
    /// <summary>Initial list of allowed URL/domain patterns. Patterns may include path components. Ignored when `unrestricted` is true.</summary>
    [JsonPropertyName("initialAllowed")]
    public IList<string>? InitialAllowed { get; set; }

    /// <summary>If true, the runtime allows access to all URLs without prompting. Initial allow-list is ignored when this is true.</summary>
    [JsonPropertyName("unrestricted")]
    public bool? Unrestricted { get; set; }
}

/// <summary>Patch of permission policy fields to apply (omit a field to leave it unchanged).</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionsConfigureParams
{
    /// <summary>If specified, replaces the host-supplied GitHub Content Exclusion policies on the session (combined with natively-discovered policies when evaluating tool/file access). Omit to leave the current policies unchanged.</summary>
    [JsonPropertyName("additionalContentExclusionPolicies")]
    public IList<PermissionsConfigureAdditionalContentExclusionPolicy>? AdditionalContentExclusionPolicies { get; set; }

    /// <summary>If specified, sets whether path/URL read permission requests are auto-approved. Omit to leave the current value unchanged.</summary>
    [JsonPropertyName("approveAllReadPermissionRequests")]
    public bool? ApproveAllReadPermissionRequests { get; set; }

    /// <summary>If specified, sets whether tool permission requests are auto-approved without prompting. Omit to leave the current value unchanged.</summary>
    [JsonPropertyName("approveAllToolPermissionRequests")]
    public bool? ApproveAllToolPermissionRequests { get; set; }

    /// <summary>If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.</summary>
    [JsonPropertyName("paths")]
    public PermissionPathsConfig? Paths { get; set; }

    /// <summary>If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged.</summary>
    [JsonPropertyName("rules")]
    public PermissionRulesSet? Rules { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged.</summary>
    [JsonPropertyName("urls")]
    public PermissionUrlsConfig? Urls { get; set; }
}

/// <summary>Indicates whether the permission decision was applied; false when the request was already resolved.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionRequestResult
{
    /// <summary>Whether the permission request was handled successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>The client's response to the pending permission prompt.</summary>
/// <remarks>Polymorphic base type discriminated by <c>kind</c>.</remarks>
[Experimental(Diagnostics.Experimental)]
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "kind",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(PermissionDecisionApproveOnce), "approve-once")]
[JsonDerivedType(typeof(PermissionDecisionApproveForSession), "approve-for-session")]
[JsonDerivedType(typeof(PermissionDecisionApproveForLocation), "approve-for-location")]
[JsonDerivedType(typeof(PermissionDecisionApprovePermanently), "approve-permanently")]
[JsonDerivedType(typeof(PermissionDecisionReject), "reject")]
[JsonDerivedType(typeof(PermissionDecisionUserNotAvailable), "user-not-available")]
[JsonDerivedType(typeof(PermissionDecisionApproved), "approved")]
[JsonDerivedType(typeof(PermissionDecisionApprovedForSession), "approved-for-session")]
[JsonDerivedType(typeof(PermissionDecisionApprovedForLocation), "approved-for-location")]
[JsonDerivedType(typeof(PermissionDecisionCancelled), "cancelled")]
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


/// <summary>Schema for the `PermissionDecisionApproveOnce` type.</summary>
/// <remarks>The <c>approve-once</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveOnce : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "approve-once";
}

/// <summary>Session-scoped approval to remember (tool prompts only; omitted for path/url prompts).</summary>
/// <remarks>Polymorphic base type discriminated by <c>kind</c>.</remarks>
[Experimental(Diagnostics.Experimental)]
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
[JsonDerivedType(typeof(PermissionDecisionApproveForSessionApprovalExtensionManagement), "extension-management")]
[JsonDerivedType(typeof(PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess), "extension-permission-access")]
public partial class PermissionDecisionApproveForSessionApproval
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("kind")]
    public virtual string Kind { get; set; } = string.Empty;
}


/// <summary>Schema for the `PermissionDecisionApproveForSessionApprovalCommands` type.</summary>
/// <remarks>The <c>commands</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForSessionApprovalCommands : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "commands";

    /// <summary>Command identifiers covered by this approval.</summary>
    [JsonPropertyName("commandIdentifiers")]
    public required IList<string> CommandIdentifiers { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApproveForSessionApprovalRead` type.</summary>
/// <remarks>The <c>read</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForSessionApprovalRead : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "read";
}

/// <summary>Schema for the `PermissionDecisionApproveForSessionApprovalWrite` type.</summary>
/// <remarks>The <c>write</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForSessionApprovalWrite : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "write";
}

/// <summary>Schema for the `PermissionDecisionApproveForSessionApprovalMcp` type.</summary>
/// <remarks>The <c>mcp</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForSessionApprovalMcp : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "mcp";

    /// <summary>MCP server name.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }

    /// <summary>MCP tool name, or null to cover every tool on the server.</summary>
    [JsonPropertyName("toolName")]
    public string? ToolName { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApproveForSessionApprovalMcpSampling` type.</summary>
/// <remarks>The <c>mcp-sampling</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForSessionApprovalMcpSampling : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "mcp-sampling";

    /// <summary>MCP server name.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApproveForSessionApprovalMemory` type.</summary>
/// <remarks>The <c>memory</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForSessionApprovalMemory : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "memory";
}

/// <summary>Schema for the `PermissionDecisionApproveForSessionApprovalCustomTool` type.</summary>
/// <remarks>The <c>custom-tool</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForSessionApprovalCustomTool : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "custom-tool";

    /// <summary>Custom tool name.</summary>
    [JsonPropertyName("toolName")]
    public required string ToolName { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApproveForSessionApprovalExtensionManagement` type.</summary>
/// <remarks>The <c>extension-management</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForSessionApprovalExtensionManagement : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "extension-management";

    /// <summary>Optional operation identifier; when omitted, the approval covers all extension management operations.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("operation")]
    public string? Operation { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess` type.</summary>
/// <remarks>The <c>extension-permission-access</c> variant of <see cref="PermissionDecisionApproveForSessionApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess : PermissionDecisionApproveForSessionApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "extension-permission-access";

    /// <summary>Extension name.</summary>
    [JsonPropertyName("extensionName")]
    public required string ExtensionName { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApproveForSession` type.</summary>
/// <remarks>The <c>approve-for-session</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForSession : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "approve-for-session";

    /// <summary>Session-scoped approval to remember (tool prompts only; omitted for path/url prompts).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("approval")]
    public PermissionDecisionApproveForSessionApproval? Approval { get; set; }

    /// <summary>URL domain to approve for the rest of the session (URL prompts only).</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("domain")]
    public string? Domain { get; set; }
}

/// <summary>Approval to persist for this location.</summary>
/// <remarks>Polymorphic base type discriminated by <c>kind</c>.</remarks>
[Experimental(Diagnostics.Experimental)]
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
[JsonDerivedType(typeof(PermissionDecisionApproveForLocationApprovalExtensionManagement), "extension-management")]
[JsonDerivedType(typeof(PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess), "extension-permission-access")]
public partial class PermissionDecisionApproveForLocationApproval
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("kind")]
    public virtual string Kind { get; set; } = string.Empty;
}


/// <summary>Schema for the `PermissionDecisionApproveForLocationApprovalCommands` type.</summary>
/// <remarks>The <c>commands</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForLocationApprovalCommands : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "commands";

    /// <summary>Command identifiers covered by this approval.</summary>
    [JsonPropertyName("commandIdentifiers")]
    public required IList<string> CommandIdentifiers { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApproveForLocationApprovalRead` type.</summary>
/// <remarks>The <c>read</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForLocationApprovalRead : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "read";
}

/// <summary>Schema for the `PermissionDecisionApproveForLocationApprovalWrite` type.</summary>
/// <remarks>The <c>write</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForLocationApprovalWrite : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "write";
}

/// <summary>Schema for the `PermissionDecisionApproveForLocationApprovalMcp` type.</summary>
/// <remarks>The <c>mcp</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForLocationApprovalMcp : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "mcp";

    /// <summary>MCP server name.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }

    /// <summary>MCP tool name, or null to cover every tool on the server.</summary>
    [JsonPropertyName("toolName")]
    public string? ToolName { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApproveForLocationApprovalMcpSampling` type.</summary>
/// <remarks>The <c>mcp-sampling</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForLocationApprovalMcpSampling : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "mcp-sampling";

    /// <summary>MCP server name.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApproveForLocationApprovalMemory` type.</summary>
/// <remarks>The <c>memory</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForLocationApprovalMemory : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "memory";
}

/// <summary>Schema for the `PermissionDecisionApproveForLocationApprovalCustomTool` type.</summary>
/// <remarks>The <c>custom-tool</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForLocationApprovalCustomTool : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "custom-tool";

    /// <summary>Custom tool name.</summary>
    [JsonPropertyName("toolName")]
    public required string ToolName { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApproveForLocationApprovalExtensionManagement` type.</summary>
/// <remarks>The <c>extension-management</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForLocationApprovalExtensionManagement : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "extension-management";

    /// <summary>Optional operation identifier; when omitted, the approval covers all extension management operations.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("operation")]
    public string? Operation { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess` type.</summary>
/// <remarks>The <c>extension-permission-access</c> variant of <see cref="PermissionDecisionApproveForLocationApproval"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess : PermissionDecisionApproveForLocationApproval
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "extension-permission-access";

    /// <summary>Extension name.</summary>
    [JsonPropertyName("extensionName")]
    public required string ExtensionName { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApproveForLocation` type.</summary>
/// <remarks>The <c>approve-for-location</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproveForLocation : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "approve-for-location";

    /// <summary>Approval to persist for this location.</summary>
    [JsonPropertyName("approval")]
    public required PermissionDecisionApproveForLocationApproval Approval { get; set; }

    /// <summary>Location key (git root or cwd) to persist the approval to.</summary>
    [JsonPropertyName("locationKey")]
    public required string LocationKey { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApprovePermanently` type.</summary>
/// <remarks>The <c>approve-permanently</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApprovePermanently : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "approve-permanently";

    /// <summary>URL domain to approve permanently.</summary>
    [JsonPropertyName("domain")]
    public required string Domain { get; set; }
}

/// <summary>Schema for the `PermissionDecisionReject` type.</summary>
/// <remarks>The <c>reject</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionReject : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "reject";

    /// <summary>Optional feedback explaining the rejection.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("feedback")]
    public string? Feedback { get; set; }
}

/// <summary>Schema for the `PermissionDecisionUserNotAvailable` type.</summary>
/// <remarks>The <c>user-not-available</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionUserNotAvailable : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "user-not-available";
}

/// <summary>Schema for the `PermissionDecisionApproved` type.</summary>
/// <remarks>The <c>approved</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApproved : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "approved";
}

/// <summary>Schema for the `PermissionDecisionApprovedForSession` type.</summary>
/// <remarks>The <c>approved-for-session</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApprovedForSession : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "approved-for-session";

    /// <summary>The approval to add as a session-scoped rule.</summary>
    [JsonPropertyName("approval")]
    public required UserToolSessionApproval Approval { get; set; }
}

/// <summary>Schema for the `PermissionDecisionApprovedForLocation` type.</summary>
/// <remarks>The <c>approved-for-location</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionApprovedForLocation : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "approved-for-location";

    /// <summary>The approval to persist for this location.</summary>
    [JsonPropertyName("approval")]
    public required UserToolSessionApproval Approval { get; set; }

    /// <summary>The location key (git root or cwd) to persist the approval to.</summary>
    [JsonPropertyName("locationKey")]
    public required string LocationKey { get; set; }
}

/// <summary>Schema for the `PermissionDecisionCancelled` type.</summary>
/// <remarks>The <c>cancelled</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionCancelled : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "cancelled";

    /// <summary>Optional explanation of why the request was cancelled.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("reason")]
    public string? Reason { get; set; }
}

/// <summary>Schema for the `PermissionDecisionDeniedByRules` type.</summary>
/// <remarks>The <c>denied-by-rules</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionDeniedByRules : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "denied-by-rules";

    /// <summary>Rules that denied the request.</summary>
    [JsonPropertyName("rules")]
    public required IList<PermissionRule> Rules { get; set; }
}

/// <summary>Schema for the `PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser` type.</summary>
/// <remarks>The <c>denied-no-approval-rule-and-could-not-request-from-user</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "denied-no-approval-rule-and-could-not-request-from-user";
}

/// <summary>Schema for the `PermissionDecisionDeniedInteractivelyByUser` type.</summary>
/// <remarks>The <c>denied-interactively-by-user</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionDecisionDeniedInteractivelyByUser : PermissionDecision
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "denied-interactively-by-user";

    /// <summary>Optional feedback from the user explaining the denial.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("feedback")]
    public string? Feedback { get; set; }

    /// <summary>Whether to force-reject the current agent turn.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("forceReject")]
    public bool? ForceReject { get; set; }
}

/// <summary>Schema for the `PermissionDecisionDeniedByContentExclusionPolicy` type.</summary>
/// <remarks>The <c>denied-by-content-exclusion-policy</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
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

/// <summary>Schema for the `PermissionDecisionDeniedByPermissionRequestHook` type.</summary>
/// <remarks>The <c>denied-by-permission-request-hook</c> variant of <see cref="PermissionDecision"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
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

/// <summary>Pending permission request ID and the decision to apply (approve/reject and scope).</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionDecisionRequest
{
    /// <summary>Request ID of the pending permission request.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>The client's response to the pending permission prompt.</summary>
    [JsonPropertyName("result")]
    public PermissionDecision Result { get => field ??= new(); set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Schema for the `PendingPermissionRequest` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PendingPermissionRequest
{
    /// <summary>The user-facing permission prompt details (commands, write, read, mcp, url, memory, custom-tool, path, hook).</summary>
    [JsonPropertyName("request")]
    public PermissionPromptRequest Request { get; set; } = null!;

    /// <summary>Unique identifier for the pending permission request.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;
}

/// <summary>List of pending permission requests reconstructed from event history.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PendingPermissionRequestList
{
    /// <summary>Pending permission prompts reconstructed from the session's event history. Equivalent to the set of `permission.requested` events that have not yet been followed by a matching `permission.completed` event. Used by clients (e.g. the CLI) to hydrate UI for prompts that were emitted before the client attached to the session.</summary>
    [JsonPropertyName("items")]
    public IList<PendingPermissionRequest> Items { get => field ??= []; set; }
}

/// <summary>No parameters; returns currently-pending permission requests for the session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionsPendingRequestsRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the operation succeeded.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsSetApproveAllResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Allow-all toggle for tool permission requests, with an optional telemetry source.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionsSetApproveAllRequest
{
    /// <summary>Whether to auto-approve all tool permission requests.</summary>
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers.</summary>
    [JsonPropertyName("source")]
    public PermissionsSetApproveAllSource? Source { get; set; }
}

/// <summary>Indicates whether the operation succeeded.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsModifyRulesResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Scope and add/remove instructions for modifying session- or location-scoped permission rules.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionsModifyRulesParams
{
    /// <summary>Rules to add to the scope. Applied before `remove`/`removeAll`.</summary>
    [JsonPropertyName("add")]
    public IList<PermissionRule>? Add { get; set; }

    /// <summary>Specific rules to remove from the scope. Ignored when `removeAll` is true.</summary>
    [JsonPropertyName("remove")]
    public IList<PermissionRule>? Remove { get; set; }

    /// <summary>When true, removes every rule currently in the scope (after any `add` is applied). Useful for clearing the location scope wholesale.</summary>
    [JsonPropertyName("removeAll")]
    public bool? RemoveAll { get; set; }

    /// <summary>Whether the change applies to ephemeral session-scoped rules (cleared at session end) or to location-scoped rules persisted via the location-permissions config file.</summary>
    [JsonPropertyName("scope")]
    public PermissionsModifyRulesScope Scope { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the operation succeeded.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsSetRequiredResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Toggles whether permission prompts should be bridged into session events for this client.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionsSetRequiredRequest
{
    /// <summary>Whether the client wants `permission.requested` events bridged from the session-owned permission service. CLI clients that render prompt UI set this to `true` for as long as their listener is mounted; headless callers leave it unset (the default is `false`).</summary>
    [JsonPropertyName("required")]
    public bool Required { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the operation succeeded.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsResetSessionApprovalsResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>No parameters; clears all session-scoped tool permission approvals.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionsResetSessionApprovalsRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the operation succeeded.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsNotifyPromptShownResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Notification payload describing the permission prompt that the client just rendered.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionPromptShownNotification
{
    /// <summary>Human-readable description of the prompt the user is being asked to approve. Used by the runtime to fire the registered `permission_prompt` notification hook (e.g. terminal bell, desktop notification).</summary>
    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Snapshot of the session's allow-listed directories and primary working directory.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionPathsList
{
    /// <summary>All directories currently allowed for tool access on this session.</summary>
    [JsonPropertyName("directories")]
    public IList<string> Directories { get => field ??= []; set; }

    /// <summary>The primary working directory for this session.</summary>
    [JsonPropertyName("primary")]
    public string Primary { get; set; } = string.Empty;
}

/// <summary>No parameters; returns the session's allow-listed directories.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionsPathsListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the operation succeeded.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsPathsAddResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Directory path to add to the session's allowed directories.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionPathsAddParams
{
    /// <summary>Directory to add to the allow-list. The runtime resolves and validates the path before adding.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the operation succeeded.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsPathsUpdatePrimaryResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Directory path to set as the session's new primary working directory.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionPathsUpdatePrimaryParams
{
    /// <summary>Directory to set as the new primary working directory for the session's permission policy.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the supplied path is within the session's allowed directories.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionPathsAllowedCheckResult
{
    /// <summary>Whether the path is within the session's allowed directories.</summary>
    [JsonPropertyName("allowed")]
    public bool Allowed { get; set; }
}

/// <summary>Path to evaluate against the session's allowed directories.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionPathsAllowedCheckParams
{
    /// <summary>Path to check against the session's allowed directories.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the supplied path is within the session's workspace directory.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionPathsWorkspaceCheckResult
{
    /// <summary>Whether the path is within the session workspace directory.</summary>
    [JsonPropertyName("allowed")]
    public bool Allowed { get; set; }
}

/// <summary>Path to evaluate against the session's workspace (primary) directory.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionPathsWorkspaceCheckParams
{
    /// <summary>Path to check against the session workspace directory.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Resolved location-permissions key and type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionLocationResolveResult
{
    /// <summary>Location key used in the location-permissions store.</summary>
    [JsonPropertyName("locationKey")]
    public string LocationKey { get; set; } = string.Empty;

    /// <summary>Whether the location is a git repo or directory.</summary>
    [JsonPropertyName("locationType")]
    public PermissionLocationType LocationType { get; set; }
}

/// <summary>Working directory to resolve into a location-permissions key.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionLocationResolveParams
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Working directory whose permission location should be resolved.</summary>
    [JsonPropertyName("workingDirectory")]
    public string WorkingDirectory { get; set; } = string.Empty;
}

/// <summary>Summary of persisted location permissions applied to the session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionLocationApplyResult
{
    /// <summary>Number of persisted allowed directories added to the live path manager.</summary>
    [JsonPropertyName("appliedDirectoryCount")]
    public long AppliedDirectoryCount { get; set; }

    /// <summary>Number of location-scoped rules added to the live permission service.</summary>
    [JsonPropertyName("appliedRuleCount")]
    public long AppliedRuleCount { get; set; }

    /// <summary>Location-scoped rules applied to the live permission service.</summary>
    [JsonPropertyName("appliedRules")]
    public IList<PermissionRule> AppliedRules { get => field ??= []; set; }

    /// <summary>Whether a different location was applied since the previous apply call.</summary>
    [JsonPropertyName("changed")]
    public bool Changed { get; set; }

    /// <summary>Location key used in the location-permissions store.</summary>
    [JsonPropertyName("locationKey")]
    public string LocationKey { get; set; } = string.Empty;

    /// <summary>Whether the location is a git repo or directory.</summary>
    [JsonPropertyName("locationType")]
    public PermissionLocationType LocationType { get; set; }
}

/// <summary>Working directory to load persisted location permissions for.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionLocationApplyParams
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Working directory whose persisted location permissions should be applied.</summary>
    [JsonPropertyName("workingDirectory")]
    public string WorkingDirectory { get; set; } = string.Empty;
}

/// <summary>Indicates whether the operation succeeded.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsLocationsAddToolApprovalResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Tool approval to persist and apply.</summary>
/// <remarks>Polymorphic base type discriminated by <c>kind</c>.</remarks>
[Experimental(Diagnostics.Experimental)]
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "kind",
    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]
[JsonDerivedType(typeof(PermissionsLocationsAddToolApprovalDetailsCommands), "commands")]
[JsonDerivedType(typeof(PermissionsLocationsAddToolApprovalDetailsRead), "read")]
[JsonDerivedType(typeof(PermissionsLocationsAddToolApprovalDetailsWrite), "write")]
[JsonDerivedType(typeof(PermissionsLocationsAddToolApprovalDetailsMcp), "mcp")]
[JsonDerivedType(typeof(PermissionsLocationsAddToolApprovalDetailsMcpSampling), "mcp-sampling")]
[JsonDerivedType(typeof(PermissionsLocationsAddToolApprovalDetailsMemory), "memory")]
[JsonDerivedType(typeof(PermissionsLocationsAddToolApprovalDetailsCustomTool), "custom-tool")]
[JsonDerivedType(typeof(PermissionsLocationsAddToolApprovalDetailsExtensionManagement), "extension-management")]
[JsonDerivedType(typeof(PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess), "extension-permission-access")]
public partial class PermissionsLocationsAddToolApprovalDetails
{
    /// <summary>The type discriminator.</summary>
    [JsonPropertyName("kind")]
    public virtual string Kind { get; set; } = string.Empty;
}


/// <summary>Schema for the `PermissionsLocationsAddToolApprovalDetailsCommands` type.</summary>
/// <remarks>The <c>commands</c> variant of <see cref="PermissionsLocationsAddToolApprovalDetails"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionsLocationsAddToolApprovalDetailsCommands : PermissionsLocationsAddToolApprovalDetails
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "commands";

    /// <summary>Command identifiers covered by this approval.</summary>
    [JsonPropertyName("commandIdentifiers")]
    public required IList<string> CommandIdentifiers { get; set; }
}

/// <summary>Schema for the `PermissionsLocationsAddToolApprovalDetailsRead` type.</summary>
/// <remarks>The <c>read</c> variant of <see cref="PermissionsLocationsAddToolApprovalDetails"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionsLocationsAddToolApprovalDetailsRead : PermissionsLocationsAddToolApprovalDetails
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "read";
}

/// <summary>Schema for the `PermissionsLocationsAddToolApprovalDetailsWrite` type.</summary>
/// <remarks>The <c>write</c> variant of <see cref="PermissionsLocationsAddToolApprovalDetails"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionsLocationsAddToolApprovalDetailsWrite : PermissionsLocationsAddToolApprovalDetails
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "write";
}

/// <summary>Schema for the `PermissionsLocationsAddToolApprovalDetailsMcp` type.</summary>
/// <remarks>The <c>mcp</c> variant of <see cref="PermissionsLocationsAddToolApprovalDetails"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionsLocationsAddToolApprovalDetailsMcp : PermissionsLocationsAddToolApprovalDetails
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "mcp";

    /// <summary>MCP server name.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }

    /// <summary>MCP tool name, or null to cover every tool on the server.</summary>
    [JsonPropertyName("toolName")]
    public string? ToolName { get; set; }
}

/// <summary>Schema for the `PermissionsLocationsAddToolApprovalDetailsMcpSampling` type.</summary>
/// <remarks>The <c>mcp-sampling</c> variant of <see cref="PermissionsLocationsAddToolApprovalDetails"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionsLocationsAddToolApprovalDetailsMcpSampling : PermissionsLocationsAddToolApprovalDetails
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "mcp-sampling";

    /// <summary>MCP server name.</summary>
    [JsonPropertyName("serverName")]
    public required string ServerName { get; set; }
}

/// <summary>Schema for the `PermissionsLocationsAddToolApprovalDetailsMemory` type.</summary>
/// <remarks>The <c>memory</c> variant of <see cref="PermissionsLocationsAddToolApprovalDetails"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionsLocationsAddToolApprovalDetailsMemory : PermissionsLocationsAddToolApprovalDetails
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "memory";
}

/// <summary>Schema for the `PermissionsLocationsAddToolApprovalDetailsCustomTool` type.</summary>
/// <remarks>The <c>custom-tool</c> variant of <see cref="PermissionsLocationsAddToolApprovalDetails"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionsLocationsAddToolApprovalDetailsCustomTool : PermissionsLocationsAddToolApprovalDetails
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "custom-tool";

    /// <summary>Custom tool name.</summary>
    [JsonPropertyName("toolName")]
    public required string ToolName { get; set; }
}

/// <summary>Schema for the `PermissionsLocationsAddToolApprovalDetailsExtensionManagement` type.</summary>
/// <remarks>The <c>extension-management</c> variant of <see cref="PermissionsLocationsAddToolApprovalDetails"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionsLocationsAddToolApprovalDetailsExtensionManagement : PermissionsLocationsAddToolApprovalDetails
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "extension-management";

    /// <summary>Optional operation identifier; when omitted, the approval covers all extension management operations.</summary>
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonPropertyName("operation")]
    public string? Operation { get; set; }
}

/// <summary>Schema for the `PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess` type.</summary>
/// <remarks>The <c>extension-permission-access</c> variant of <see cref="PermissionsLocationsAddToolApprovalDetails"/>.</remarks>
[Experimental(Diagnostics.Experimental)]
public partial class PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess : PermissionsLocationsAddToolApprovalDetails
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Kind => "extension-permission-access";

    /// <summary>Extension name.</summary>
    [JsonPropertyName("extensionName")]
    public required string ExtensionName { get; set; }
}

/// <summary>Location-scoped tool approval to persist.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionLocationAddToolApprovalParams
{
    /// <summary>Tool approval to persist and apply.</summary>
    [JsonPropertyName("approval")]
    public PermissionsLocationsAddToolApprovalDetails Approval { get => field ??= new(); set; }

    /// <summary>Location key (git root or cwd) to persist the approval to.</summary>
    [JsonPropertyName("locationKey")]
    public string LocationKey { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Folder trust check result.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class FolderTrustCheckResult
{
    /// <summary>Whether the folder is trusted.</summary>
    [JsonPropertyName("trusted")]
    public bool Trusted { get; set; }
}

/// <summary>Folder path to check for trust.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class FolderTrustCheckParams
{
    /// <summary>Folder path to check.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the operation succeeded.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsFolderTrustAddTrustedResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Folder path to add to trusted folders.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class FolderTrustAddParams
{
    /// <summary>Folder path to mark as trusted.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the operation succeeded.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsUrlsSetUnrestrictedModeResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Whether the URL-permission policy should run in unrestricted mode.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class PermissionUrlsSetUnrestrictedModeParams
{
    /// <summary>Whether to allow access to all URLs without prompting. Toggles the runtime's URL-permission policy in place.</summary>
    [JsonPropertyName("enabled")]
    public bool Enabled { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>The repository the remote session targets.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class MetadataSnapshotRemoteMetadataRepository
{
    /// <summary>The branch the remote session is operating on.</summary>
    [JsonPropertyName("branch")]
    public string Branch { get; set; } = string.Empty;

    /// <summary>The GitHub repository name (without owner).</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>The GitHub owner (user or organization) of the target repository.</summary>
    [JsonPropertyName("owner")]
    public string Owner { get; set; } = string.Empty;
}

/// <summary>Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are immutable for the lifetime of the session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class MetadataSnapshotRemoteMetadata
{
    /// <summary>The pull request number the remote session is associated with, if any.</summary>
    [JsonPropertyName("pullRequestNumber")]
    public long? PullRequestNumber { get; set; }

    /// <summary>The repository the remote session targets.</summary>
    [JsonPropertyName("repository")]
    public MetadataSnapshotRemoteMetadataRepository Repository { get => field ??= new(); set; }

    /// <summary>The original resource identifier (task ID or PR node ID), preserved across event-replay reconstructions. Falls back to `sessionId` when absent.</summary>
    [JsonPropertyName("resourceId")]
    public string? ResourceId { get; set; }

    /// <summary>Whether the remote task originated from Copilot Coding Agent (cca) or a CLI `--remote` invocation.</summary>
    [JsonPropertyName("taskType")]
    public MetadataSnapshotRemoteMetadataTaskType? TaskType { get; set; }
}

/// <summary>Public-facing projection of workspace metadata for SDK / TUI consumers.</summary>
public sealed class SessionMetadataSnapshotWorkspace
{
    /// <summary>Branch checked out at session start, if any.</summary>
    [JsonPropertyName("branch")]
    public string? Branch { get; set; }

    /// <summary>ISO 8601 timestamp when the workspace was created.</summary>
    [JsonPropertyName("created_at")]
    public DateTimeOffset? CreatedAt { get; set; }

    /// <summary>Current working directory at session start.</summary>
    [JsonPropertyName("cwd")]
    public string? Cwd { get; set; }

    /// <summary>Resolved git root for cwd, if any.</summary>
    [JsonPropertyName("git_root")]
    public string? GitRoot { get; set; }

    /// <summary>Repository host type, if known.</summary>
    [JsonPropertyName("host_type")]
    public WorkspaceSummaryHostType? HostType { get; set; }

    /// <summary>Workspace identifier (1:1 with sessionId).</summary>
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MinLength(1)]
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Display name for the session, if set.</summary>
    [JsonPropertyName("name")]
    public string? Name { get; set; }

    /// <summary>Repository identifier in 'owner/repo' or 'org/project/repo' format, if any.</summary>
    [JsonPropertyName("repository")]
    public string? Repository { get; set; }

    /// <summary>ISO 8601 timestamp when the workspace was last updated.</summary>
    [JsonPropertyName("updated_at")]
    public DateTimeOffset? UpdatedAt { get; set; }
}

/// <summary>Point-in-time snapshot of slow-changing session identifier and state fields.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionMetadataSnapshot
{
    /// <summary>True when the session was detected to be in use by another process at construction time. Local consumers may surface a confirmation prompt before fully attaching. Always false for new sessions.</summary>
    [JsonPropertyName("alreadyInUse")]
    public bool AlreadyInUse { get; set; }

    /// <summary>Runtime client name associated with the session (telemetry identifier).</summary>
    [JsonPropertyName("clientName")]
    public string? ClientName { get; set; }

    /// <summary>The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot').</summary>
    [JsonPropertyName("currentMode")]
    public MetadataSnapshotCurrentMode CurrentMode { get; set; }

    /// <summary>User-provided name supplied at session construction (via `--name`), if any. Immutable after construction.</summary>
    [JsonPropertyName("initialName")]
    public string? InitialName { get; set; }

    /// <summary>Whether this is a remote session (i.e., one whose runtime executes elsewhere and is steered through this process).</summary>
    [JsonPropertyName("isRemote")]
    public bool IsRemote { get; set; }

    /// <summary>ISO 8601 timestamp of when the session's persisted state was last modified on disk. For new sessions, equals startTime. For resumed sessions, reflects the previous modification time at construction.</summary>
    [JsonPropertyName("modifiedTime")]
    public DateTimeOffset ModifiedTime { get; set; }

    /// <summary>Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are immutable for the lifetime of the session.</summary>
    [JsonPropertyName("remoteMetadata")]
    public MetadataSnapshotRemoteMetadata? RemoteMetadata { get; set; }

    /// <summary>Currently selected model identifier, if any.</summary>
    [JsonPropertyName("selectedModel")]
    public string? SelectedModel { get; set; }

    /// <summary>The unique identifier of the session.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>ISO 8601 timestamp of when the session started.</summary>
    [JsonPropertyName("startTime")]
    public DateTimeOffset StartTime { get; set; }

    /// <summary>Short human-readable summary of the session, if known. Omitted when no summary has been generated.</summary>
    [JsonPropertyName("summary")]
    public string? Summary { get; set; }

    /// <summary>Absolute path to the session's current working directory.</summary>
    [JsonPropertyName("workingDirectory")]
    public string WorkingDirectory { get; set; } = string.Empty;

    /// <summary>Public-facing workspace metadata for this session, or null if the session has no associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count, internal flags).</summary>
    [JsonPropertyName("workspace")]
    public SessionMetadataSnapshotWorkspace? Workspace { get; set; }

    /// <summary>Absolute path to the session's workspace directory on disk, or null if the session has no associated workspace.</summary>
    [JsonPropertyName("workspacePath")]
    public string? WorkspacePath { get; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionMetadataSnapshotRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the local session is currently processing a turn or background continuation.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class MetadataIsProcessingResult
{
    /// <summary>Whether the session is currently processing user/agent messages. False for non-local sessions (which don't run a local agentic loop). Reflects an in-flight turn or background continuation.</summary>
    [JsonPropertyName("processing")]
    public bool Processing { get; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionMetadataIsProcessingRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Token-usage breakdown for the session's current context window.</summary>
public sealed class MetadataContextInfoResultContextInfo
{
    /// <summary>Output reserve plus tokens after the buffer-exhaustion blocking threshold (default 95%).</summary>
    [JsonPropertyName("bufferTokens")]
    public long BufferTokens { get; set; }

    /// <summary>Token count at which background compaction starts (configurable percentage of promptTokenLimit).</summary>
    [JsonPropertyName("compactionThreshold")]
    public long CompactionThreshold { get; set; }

    /// <summary>Tokens consumed by user/assistant/tool messages.</summary>
    [JsonPropertyName("conversationTokens")]
    public long ConversationTokens { get; set; }

    /// <summary>Total context limit for /context display. promptTokenLimit + min(32k or 64k, outputTokenLimit) depending on model.</summary>
    [JsonPropertyName("limit")]
    public long Limit { get; set; }

    /// <summary>The model used for token counting.</summary>
    [JsonPropertyName("modelName")]
    public string ModelName { get; set; } = string.Empty;

    /// <summary>Maximum prompt tokens allowed by the model (or DEFAULT_TOKEN_LIMIT if unspecified).</summary>
    [JsonPropertyName("promptTokenLimit")]
    public long PromptTokenLimit { get; set; }

    /// <summary>Tokens consumed by the system prompt.</summary>
    [JsonPropertyName("systemTokens")]
    public long SystemTokens { get; set; }

    /// <summary>Tokens consumed by tool definitions sent to the model (excludes deferred tools).</summary>
    [JsonPropertyName("toolDefinitionsTokens")]
    public long ToolDefinitionsTokens { get; set; }

    /// <summary>Sum of system, conversation and tool-definition tokens.</summary>
    [JsonPropertyName("totalTokens")]
    public long TotalTokens { get; set; }
}

/// <summary>Token breakdown for the session's current context window, or null if uninitialized.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class MetadataContextInfoResult
{
    /// <summary>Token breakdown for the current context window, or null if the session has not yet been initialized (no system prompt or tool metadata cached).</summary>
    [JsonPropertyName("contextInfo")]
    public MetadataContextInfoResultContextInfo? ContextInfo { get; set; }
}

/// <summary>Model identifier and token limits used to compute the context-info breakdown.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class MetadataContextInfoRequest
{
    /// <summary>Maximum output tokens allowed by the target model. Pass 0 if unknown.</summary>
    [JsonPropertyName("outputTokenLimit")]
    public long OutputTokenLimit { get; set; }

    /// <summary>Maximum prompt tokens allowed by the target model. Pass 0 to use the runtime default.</summary>
    [JsonPropertyName("promptTokenLimit")]
    public long PromptTokenLimit { get; set; }

    /// <summary>Model identifier used for tokenization. Omit to use the session default. Used both for token counting and to compute display values.</summary>
    [JsonPropertyName("selectedModel")]
    public string? SelectedModel { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Notify the session that its working directory context has changed. Emits a `session.context_changed` event so consumers (telemetry, OTel tracker, ACP, the timeline UI) can react. Use this when the host has detected a cwd/branch/repo change outside the session's normal lifecycle (e.g., after a shell command in interactive mode).</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class MetadataRecordContextChangeResult
{
}

/// <summary>Updated working directory and git context. Emitted as the new payload of `session.context_changed`.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionWorkingDirectoryContext
{
    /// <summary>Merge-base commit SHA (fork point from the remote default branch).</summary>
    [JsonPropertyName("baseCommit")]
    public string? BaseCommit { get; set; }

    /// <summary>Current git branch name.</summary>
    [JsonPropertyName("branch")]
    public string? Branch { get; set; }

    /// <summary>Current working directory path.</summary>
    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    /// <summary>Root directory of the git repository, resolved via git rev-parse.</summary>
    [JsonPropertyName("gitRoot")]
    public string? GitRoot { get; set; }

    /// <summary>Head commit of the current git branch.</summary>
    [JsonPropertyName("headCommit")]
    public string? HeadCommit { get; set; }

    /// <summary>Hosting platform type of the repository.</summary>
    [JsonPropertyName("hostType")]
    public SessionWorkingDirectoryContextHostType? HostType { get; set; }

    /// <summary>Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps).</summary>
    [JsonPropertyName("repository")]
    public string? Repository { get; set; }

    /// <summary>Raw host string from the git remote URL (e.g. "github.com", "dev.azure.com").</summary>
    [JsonPropertyName("repositoryHost")]
    public string? RepositoryHost { get; set; }
}

/// <summary>Updated working-directory/git context to record on the session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class MetadataRecordContextChangeRequest
{
    /// <summary>Updated working directory and git context. Emitted as the new payload of `session.context_changed`.</summary>
    [JsonPropertyName("context")]
    public SessionWorkingDirectoryContext Context { get => field ??= new(); set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Update the session's working directory. Used by the host when the user explicitly changes cwd (e.g., the `/cd` slash command). The host is responsible for `process.chdir` and any related side-effects (file index, etc.); this method only updates the session's own recorded path.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class MetadataSetWorkingDirectoryResult
{
    /// <summary>Working directory after the update.</summary>
    [JsonPropertyName("workingDirectory")]
    public string WorkingDirectory { get; set; } = string.Empty;
}

/// <summary>Absolute path to set as the session's new working directory.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class MetadataSetWorkingDirectoryRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Absolute path to set as the session's working directory. The runtime updates the session's recorded cwd so subsequent operations (shell tools, file lookups, telemetry) anchor to it.</summary>
    [JsonPropertyName("workingDirectory")]
    public string WorkingDirectory { get; set; } = string.Empty;
}

/// <summary>Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class MetadataRecomputeContextTokensResult
{
    /// <summary>Tokens contributed by user/assistant/tool messages (excludes system/developer prompts).</summary>
    [JsonPropertyName("messagesTokenCount")]
    public long MessagesTokenCount { get; set; }

    /// <summary>Tokens contributed by system/developer prompt snapshots.</summary>
    [JsonPropertyName("systemTokenCount")]
    public long SystemTokenCount { get; set; }

    /// <summary>Sum of tokens across chat-context and system-context messages currently held by the session.</summary>
    [JsonPropertyName("totalTokens")]
    public long TotalTokens { get; set; }
}

/// <summary>Model identifier to use when re-tokenizing the session's existing messages.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class MetadataRecomputeContextTokensRequest
{
    /// <summary>Model identifier used for tokenization. The runtime token-counts both chat-context and system-context messages against this model.</summary>
    [JsonPropertyName("modelId")]
    public string ModelId { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Identifier of the spawned process, used to correlate streamed output and exit notifications.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ShellExecResult
{
    /// <summary>Unique identifier for tracking streamed output.</summary>
    [JsonPropertyName("processId")]
    public string ProcessId { get; set; } = string.Empty;
}

/// <summary>Shell command to run, with optional working directory and timeout in milliseconds.</summary>
[Experimental(Diagnostics.Experimental)]
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
    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]
    [JsonPropertyName("timeout")]
    public TimeSpan? Timeout { get; set; }
}

/// <summary>Indicates whether the signal was delivered; false if the process was unknown or already exited.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ShellKillResult
{
    /// <summary>Whether the signal was sent successfully.</summary>
    [JsonPropertyName("killed")]
    public bool Killed { get; set; }
}

/// <summary>Identifier of a process previously returned by "shell.exec" and the signal to send.</summary>
[Experimental(Diagnostics.Experimental)]
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
[Experimental(Diagnostics.Experimental)]
public sealed class HistoryCompactContextWindow
{
    /// <summary>Token count from non-system messages (user, assistant, tool).</summary>
    [JsonPropertyName("conversationTokens")]
    public long? ConversationTokens { get; set; }

    /// <summary>Current total tokens in the context window (system + conversation + tool definitions).</summary>
    [JsonPropertyName("currentTokens")]
    public long CurrentTokens { get; set; }

    /// <summary>Current number of messages in the conversation.</summary>
    [JsonPropertyName("messagesLength")]
    public long MessagesLength { get; set; }

    /// <summary>Token count from system message(s).</summary>
    [JsonPropertyName("systemTokens")]
    public long? SystemTokens { get; set; }

    /// <summary>Maximum token count for the model's context window.</summary>
    [JsonPropertyName("tokenLimit")]
    public long TokenLimit { get; set; }

    /// <summary>Token count from tool definitions.</summary>
    [JsonPropertyName("toolDefinitionsTokens")]
    public long? ToolDefinitionsTokens { get; set; }
}

/// <summary>Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class HistoryCompactResult
{
    /// <summary>Post-compaction context window usage breakdown.</summary>
    [JsonPropertyName("contextWindow")]
    public HistoryCompactContextWindow? ContextWindow { get; set; }

    /// <summary>Number of messages removed during compaction.</summary>
    [JsonPropertyName("messagesRemoved")]
    public long MessagesRemoved { get; set; }

    /// <summary>Whether compaction completed successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }

    /// <summary>Summary text produced by compaction. Omitted when compaction did not produce a summary (e.g. failure path).</summary>
    [JsonPropertyName("summaryContent")]
    public string? SummaryContent { get; set; }

    /// <summary>Number of tokens freed by compaction.</summary>
    [JsonPropertyName("tokensRemoved")]
    public long TokensRemoved { get; set; }
}

/// <summary>Optional compaction parameters.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class HistoryCompactRequest
{
    /// <summary>Optional user-provided instructions to focus the compaction summary.</summary>
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MaxLength(4000)]
    [JsonPropertyName("customInstructions")]
    public string? CustomInstructions { get; set; }
}

/// <summary>Optional compaction parameters.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class HistoryCompactRequestWithSession
{
    /// <summary>Optional user-provided instructions to focus the compaction summary.</summary>
    [UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]
    [MaxLength(4000)]
    [JsonPropertyName("customInstructions")]
    public string? CustomInstructions { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Number of events that were removed by the truncation.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class HistoryTruncateResult
{
    /// <summary>Number of events that were removed.</summary>
    [JsonPropertyName("eventsRemoved")]
    public long EventsRemoved { get; set; }
}

/// <summary>Identifier of the event to truncate to; this event and all later events are removed.</summary>
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

/// <summary>Indicates whether an in-progress background compaction was cancelled.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class HistoryCancelBackgroundCompactionResult
{
    /// <summary>Whether an in-progress background compaction was cancelled. False when no compaction was running, when the session is remote, or when the underlying processor was unavailable.</summary>
    [JsonPropertyName("cancelled")]
    public bool Cancelled { get; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionHistoryCancelBackgroundCompactionRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether an in-progress manual compaction was aborted.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class HistoryAbortManualCompactionResult
{
    /// <summary>Whether an in-progress manual compaction was aborted. False when no manual compaction was running, when its abort controller was already aborted, or when the session is remote.</summary>
    [JsonPropertyName("aborted")]
    public bool Aborted { get; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionHistoryAbortManualCompactionRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Markdown summary of the conversation context (empty when not available).</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class HistorySummarizeForHandoffResult
{
    /// <summary>Markdown summary of the conversation context produced by an LLM. Empty string when there are no messages or when the session does not support local summarization.</summary>
    [JsonPropertyName("summary")]
    public string Summary { get; set; } = string.Empty;
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionHistorySummarizeForHandoffRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Schema for the `QueuePendingItems` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class QueuePendingItems
{
    /// <summary>Human-readable text to display for this queue entry in the UI.</summary>
    [JsonPropertyName("displayText")]
    public string DisplayText { get; set; } = string.Empty;

    /// <summary>Whether this item is a queued user message or a queued slash command / model change.</summary>
    [JsonPropertyName("kind")]
    public QueuePendingItemsKind Kind { get; set; }
}

/// <summary>Snapshot of the session's pending queued items and immediate-steering messages.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class QueuePendingItemsResult
{
    /// <summary>Pending queued items in submission order. Includes user messages, queued slash commands, and queued model changes; omits internal system items.</summary>
    [JsonPropertyName("items")]
    public IList<QueuePendingItems> Items { get => field ??= []; set; }

    /// <summary>Display text for messages currently in the immediate steering queue (interjections sent during a running turn).</summary>
    [JsonPropertyName("steeringMessages")]
    public IList<string> SteeringMessages { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionQueuePendingItemsRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether a user-facing pending item was removed.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class QueueRemoveMostRecentResult
{
    /// <summary>True if a user-facing pending item was removed (LIFO across both queues); false when no removable items remained.</summary>
    [JsonPropertyName("removed")]
    public bool Removed { get; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionQueueRemoveMostRecentRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionQueueClearRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Batch of session events returned by a read, with cursor and continuation metadata.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class EventsReadResult
{
    /// <summary>Opaque cursor for the next read. Pass back unchanged in the next read.cursor to continue from where this read left off. Always present, even when no events were returned.</summary>
    [JsonPropertyName("cursor")]
    public string Cursor { get; set; } = string.Empty;

    /// <summary>Cursor status: 'ok' means the cursor was applied successfully; 'expired' means the cursor referred to an event that no longer exists in history (e.g. truncated or compacted away) and the read started from the beginning of the remaining history.</summary>
    [JsonPropertyName("cursorStatus")]
    public EventsCursorStatus CursorStatus { get; set; }

    /// <summary>Events are delivered in two batches per read: persisted events first (in append order), then ephemeral events (in seq order). When `waitMs &gt; 0` and the catch-up batches were empty, post-wait events follow the same two-batch ordering. Persisted and ephemeral events do not interleave within a single read.</summary>
    [JsonPropertyName("events")]
    public IList<SessionEvent> Events { get => field ??= []; set; }

    /// <summary>True when the read returned `max` events and more events are available immediately. When false, the next read with a non-zero `waitMs` will block until a new event arrives or the wait expires.</summary>
    [JsonPropertyName("hasMore")]
    public bool HasMore { get; set; }
}

/// <summary>Cursor, batch size, and optional long-poll/filter parameters for reading session events.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class EventLogReadRequest
{
    /// <summary>Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers.</summary>
    [JsonPropertyName("agentScope")]
    public EventsAgentScope? AgentScope { get; set; }

    /// <summary>Opaque cursor returned by a previous read. Omit on the first call to start from the beginning of the session's persisted history.</summary>
    [JsonPropertyName("cursor")]
    public string? Cursor { get; set; }

    /// <summary>Maximum number of events to return in this batch (1–1000, default 200).</summary>
    [JsonPropertyName("max")]
    public int? Max { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Either '*' to receive all event types, or a non-empty list of event types to receive.</summary>
    [JsonPropertyName("types")]
    public JsonElement? Types { get; set; }

    /// <summary>Milliseconds to wait for new events when the cursor is at the tail of history. 0 (default) returns immediately even if no events are available. Capped at 30000ms. Ephemeral events that arrive during the wait are delivered in this batch but are NOT replayable on a subsequent read (use a non-zero waitMs in your next call to capture future ephemerals as they happen).</summary>
    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]
    [JsonPropertyName("waitMs")]
    public TimeSpan? Wait { get; set; }
}

/// <summary>Snapshot of the current tail cursor without returning any events. Use this when a consumer wants to subscribe to live events going forward without first paginating through the entire persisted history (which would happen if `read` were called without a cursor on a long-lived session).</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class EventLogTailResult
{
    /// <summary>Opaque cursor pointing at the current tail of the session's persisted-events history. Pass back to `read` to receive only events that arrive AFTER this snapshot. When the session has no events, this returns the same sentinel as an unset cursor (i.e. equivalent to omitting the cursor on a first read).</summary>
    [JsonPropertyName("cursor")]
    public string Cursor { get; set; } = string.Empty;
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionEventLogTailRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Opaque handle representing an event-type interest registration.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class RegisterEventInterestResult
{
    /// <summary>Opaque handle for this registration. Pass to releaseInterest to release. Each call to registerInterest produces a fresh handle, even when the same eventType is registered multiple times.</summary>
    [JsonPropertyName("handle")]
    public string Handle { get; set; } = string.Empty;
}

/// <summary>Event type to register consumer interest for, used by runtime gating logic.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class RegisterEventInterestParams
{
    /// <summary>The event type the consumer wants the runtime to treat as 'observed' for behavior-switching gating. Some runtime code paths inspect whether any consumer is interested in a specific event type and choose a different implementation accordingly (e.g. `mcp.oauth_required`: when interest is registered the runtime delegates the full interactive OAuth flow to the consumer; when no interest is registered the runtime installs a browserless fallback that silently reuses cached tokens). SDK clients that long-poll events do NOT automatically appear as listeners to these gating checks — they must explicitly call `registerInterest` for each event type they want the runtime to count as having a consumer. Multiple registrations for the same event type from the same or different consumers are tracked independently and must each be released. See: `mcp.oauth_required`, `sampling.requested`, `auto_mode_switch.requested`, `user_input.requested`, `elicitation.requested`, `command.queued`, `exit_plan_mode.requested`.</summary>
    [JsonPropertyName("eventType")]
    public string EventType { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the operation succeeded.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class EventLogReleaseInterestResult
{
    /// <summary>Whether the operation succeeded.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>Opaque handle previously returned by `registerInterest` to release.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class ReleaseEventInterestParams
{
    /// <summary>Handle returned by a previous `registerInterest` call. Idempotent: releasing an unknown or already-released handle is a no-op (returns success). When the last outstanding handle for an event type is released, the runtime reverts to its 'no consumer' code path for that event type.</summary>
    [JsonPropertyName("handle")]
    public string Handle { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Aggregated code change metrics.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UsageMetricsCodeChanges
{
    /// <summary>Distinct file paths modified during the session.</summary>
    [JsonPropertyName("filesModified")]
    public IList<string> FilesModified { get => field ??= []; set; }

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
[Experimental(Diagnostics.Experimental)]
public sealed class UsageMetricsModelMetricRequests
{
    /// <summary>User-initiated premium request cost (with multiplier applied).</summary>
    [JsonPropertyName("cost")]
    public double Cost { get; set; }

    /// <summary>Number of API requests made with this model.</summary>
    [JsonPropertyName("count")]
    public long Count { get; set; }
}

/// <summary>Schema for the `UsageMetricsModelMetricTokenDetail` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UsageMetricsModelMetricTokenDetail
{
    /// <summary>Accumulated token count for this token type.</summary>
    [JsonPropertyName("tokenCount")]
    public long TokenCount { get; set; }
}

/// <summary>Token usage metrics for this model.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UsageMetricsModelMetricUsage
{
    /// <summary>Total tokens read from prompt cache.</summary>
    [JsonPropertyName("cacheReadTokens")]
    public long CacheReadTokens { get; set; }

    /// <summary>Total tokens written to prompt cache.</summary>
    [JsonPropertyName("cacheWriteTokens")]
    public long CacheWriteTokens { get; set; }

    /// <summary>Total input tokens consumed.</summary>
    [JsonPropertyName("inputTokens")]
    public long InputTokens { get; set; }

    /// <summary>Total output tokens produced.</summary>
    [JsonPropertyName("outputTokens")]
    public long OutputTokens { get; set; }

    /// <summary>Total output tokens used for reasoning.</summary>
    [JsonPropertyName("reasoningTokens")]
    public long? ReasoningTokens { get; set; }
}

/// <summary>Schema for the `UsageMetricsModelMetric` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UsageMetricsModelMetric
{
    /// <summary>Request count and cost metrics for this model.</summary>
    [JsonPropertyName("requests")]
    public UsageMetricsModelMetricRequests Requests { get => field ??= new(); set; }

    /// <summary>Token count details per type.</summary>
    [JsonPropertyName("tokenDetails")]
    public IDictionary<string, UsageMetricsModelMetricTokenDetail>? TokenDetails { get; set; }

    /// <summary>Accumulated nano-AI units cost for this model.</summary>
    [JsonPropertyName("totalNanoAiu")]
    public double? TotalNanoAiu { get; set; }

    /// <summary>Token usage metrics for this model.</summary>
    [JsonPropertyName("usage")]
    public UsageMetricsModelMetricUsage Usage { get => field ??= new(); set; }
}

/// <summary>Schema for the `UsageMetricsTokenDetail` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UsageMetricsTokenDetail
{
    /// <summary>Accumulated token count for this token type.</summary>
    [JsonPropertyName("tokenCount")]
    public long TokenCount { get; set; }
}

/// <summary>Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals.</summary>
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
    [JsonPropertyName("lastCallInputTokens")]
    public long LastCallInputTokens { get; set; }

    /// <summary>Output tokens from the most recent main-agent API call.</summary>
    [JsonPropertyName("lastCallOutputTokens")]
    public long LastCallOutputTokens { get; set; }

    /// <summary>Per-model token and request metrics, keyed by model identifier.</summary>
    [JsonPropertyName("modelMetrics")]
    public IDictionary<string, UsageMetricsModelMetric> ModelMetrics { get => field ??= new Dictionary<string, UsageMetricsModelMetric>(); set; }

    /// <summary>ISO 8601 timestamp when the session started.</summary>
    [JsonPropertyName("sessionStartTime")]
    public DateTimeOffset SessionStartTime { get; set; }

    /// <summary>Session-wide per-token-type accumulated token counts.</summary>
    [JsonPropertyName("tokenDetails")]
    public IDictionary<string, UsageMetricsTokenDetail>? TokenDetails { get; set; }

    /// <summary>Total time spent in model API calls (milliseconds).</summary>
    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]
    [JsonPropertyName("totalApiDurationMs")]
    public TimeSpan TotalApiDuration { get; set; }

    /// <summary>Session-wide accumulated nano-AI units cost.</summary>
    [JsonPropertyName("totalNanoAiu")]
    public double? TotalNanoAiu { get; set; }

    /// <summary>Total user-initiated premium request cost across all models (may be fractional due to multipliers).</summary>
    [JsonPropertyName("totalPremiumRequestCost")]
    public double TotalPremiumRequestCost { get; set; }

    /// <summary>Raw count of user-initiated API requests.</summary>
    [JsonPropertyName("totalUserRequests")]
    public long TotalUserRequests { get; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionUsageGetMetricsRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>GitHub URL for the session and a flag indicating whether remote steering is enabled.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class RemoteEnableResult
{
    /// <summary>Whether remote steering is enabled.</summary>
    [JsonPropertyName("remoteSteerable")]
    public bool RemoteSteerable { get; set; }

    /// <summary>GitHub frontend URL for this session.</summary>
    [Url]
    [StringSyntax(StringSyntaxAttribute.Uri)]
    [JsonPropertyName("url")]
    public string? Url { get; set; }
}

/// <summary>Optional remote session mode ("off", "export", or "on"); defaults to enabling both export and remote steering.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class RemoteEnableRequest
{
    /// <summary>Per-session remote mode. "off" disables remote, "export" exports session events to GitHub without enabling remote steering, "on" enables both export and remote steering.</summary>
    [JsonPropertyName("mode")]
    public RemoteSessionMode? Mode { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionRemoteDisableRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Persist a steerability change as a `session.remote_steerable_changed` event. Used by the host (CLI / SDK consumer) when it has just finished enabling or disabling steering on a remote exporter that the runtime does not directly own.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class RemoteNotifySteerableChangedResult
{
}

/// <summary>New remote-steerability state to persist as a `session.remote_steerable_changed` event.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class RemoteNotifySteerableChangedRequest
{
    /// <summary>Whether the session now supports remote steering via GitHub. The runtime persists this as a `session.remote_steerable_changed` event so resume/replay sees the up-to-date capability.</summary>
    [JsonPropertyName("remoteSteerable")]
    public bool RemoteSteerable { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Schema for the `ScheduleEntry` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ScheduleEntry
{
    /// <summary>Display-only label for the prompt as shown in the UI (e.g. `/skill-name` for a skill-invocation schedule). The actual enqueued prompt is `prompt`.</summary>
    [JsonPropertyName("displayPrompt")]
    public string? DisplayPrompt { get; set; }

    /// <summary>Sequential id assigned by the runtime within the session. Stable across resumes (rebuilt from the event log).</summary>
    [JsonPropertyName("id")]
    public long Id { get; set; }

    /// <summary>Interval between scheduled ticks, in milliseconds.</summary>
    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]
    [JsonPropertyName("intervalMs")]
    public TimeSpan Interval { get; set; }

    /// <summary>ISO 8601 timestamp when the next tick is scheduled to fire.</summary>
    [JsonPropertyName("nextRunAt")]
    public DateTimeOffset NextRunAt { get; set; }

    /// <summary>Prompt text that gets enqueued on every tick.</summary>
    [JsonPropertyName("prompt")]
    public string Prompt { get; set; } = string.Empty;

    /// <summary>Whether the schedule re-arms after each tick (`/every`) or fires once (`/after`).</summary>
    [JsonPropertyName("recurring")]
    public bool Recurring { get; set; }
}

/// <summary>Snapshot of the currently active recurring prompts for this session.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ScheduleList
{
    /// <summary>Active scheduled prompts, ordered by id.</summary>
    [JsonPropertyName("entries")]
    public IList<ScheduleEntry> Entries { get => field ??= []; set; }
}

/// <summary>Identifies the target session.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class SessionScheduleListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Remove a scheduled prompt by id. The result entry is omitted if the id was unknown.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ScheduleStopResult
{
    /// <summary>The removed entry, or omitted if no entry matched.</summary>
    [JsonPropertyName("entry")]
    public ScheduleEntry? Entry { get; set; }
}

/// <summary>Identifier of the scheduled prompt to remove.</summary>
[Experimental(Diagnostics.Experimental)]
internal sealed class ScheduleStopRequest
{
    /// <summary>Id of the scheduled prompt to remove.</summary>
    [JsonPropertyName("id")]
    public long Id { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Describes a filesystem error.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionFsError
{
    /// <summary>Error classification.</summary>
    [JsonPropertyName("code")]
    public SessionFsErrorCode Code { get; set; }

    /// <summary>Free-form detail about the error, for logging/diagnostics.</summary>
    [JsonPropertyName("message")]
    public string? Message { get; set; }
}

/// <summary>File content as a UTF-8 string, or a filesystem error if the read failed.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionFsReadFileResult
{
    /// <summary>File content as UTF-8 string.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Describes a filesystem error.</summary>
    [JsonPropertyName("error")]
    public SessionFsError? Error { get; set; }
}

/// <summary>Path of the file to read from the client-provided session filesystem.</summary>
public sealed class SessionFsReadFileRequest
{
    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>File path, content to write, and optional mode for the client-provided session filesystem.</summary>
public sealed class SessionFsWriteFileRequest
{
    /// <summary>Content to write.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Optional POSIX-style mode for newly created files.</summary>
    [JsonPropertyName("mode")]
    public long? Mode { get; set; }

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>File path, content to append, and optional mode for the client-provided session filesystem.</summary>
public sealed class SessionFsAppendFileRequest
{
    /// <summary>Content to append.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    /// <summary>Optional POSIX-style mode for newly created files.</summary>
    [JsonPropertyName("mode")]
    public long? Mode { get; set; }

    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the requested path exists in the client-provided session filesystem.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionFsExistsResult
{
    /// <summary>Whether the path exists.</summary>
    [JsonPropertyName("exists")]
    public bool Exists { get; set; }
}

/// <summary>Path to test for existence in the client-provided session filesystem.</summary>
public sealed class SessionFsExistsRequest
{
    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Filesystem metadata for the requested path, or a filesystem error if the stat failed.</summary>
[Experimental(Diagnostics.Experimental)]
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
    [JsonPropertyName("size")]
    public long Size { get; set; }
}

/// <summary>Path whose metadata should be returned from the client-provided session filesystem.</summary>
public sealed class SessionFsStatRequest
{
    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Directory path to create in the client-provided session filesystem, with options for recursive creation and POSIX mode.</summary>
public sealed class SessionFsMkdirRequest
{
    /// <summary>Optional POSIX-style mode for newly created directories.</summary>
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

/// <summary>Names of entries in the requested directory, or a filesystem error if the read failed.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionFsReaddirResult
{
    /// <summary>Entry names in the directory.</summary>
    [JsonPropertyName("entries")]
    public IList<string> Entries { get => field ??= []; set; }

    /// <summary>Describes a filesystem error.</summary>
    [JsonPropertyName("error")]
    public SessionFsError? Error { get; set; }
}

/// <summary>Directory path whose entries should be listed from the client-provided session filesystem.</summary>
public sealed class SessionFsReaddirRequest
{
    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Schema for the `SessionFsReaddirWithTypesEntry` type.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionFsReaddirWithTypesEntry
{
    /// <summary>Entry name.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Entry type.</summary>
    [JsonPropertyName("type")]
    public SessionFsReaddirWithTypesEntryType Type { get; set; }
}

/// <summary>Entries in the requested directory paired with file/directory type information, or a filesystem error if the read failed.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionFsReaddirWithTypesResult
{
    /// <summary>Directory entries with type information.</summary>
    [JsonPropertyName("entries")]
    public IList<SessionFsReaddirWithTypesEntry> Entries { get => field ??= []; set; }

    /// <summary>Describes a filesystem error.</summary>
    [JsonPropertyName("error")]
    public SessionFsError? Error { get; set; }
}

/// <summary>Directory path whose entries (with type information) should be listed from the client-provided session filesystem.</summary>
public sealed class SessionFsReaddirWithTypesRequest
{
    /// <summary>Path using SessionFs conventions.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Path to remove from the client-provided session filesystem, with options for recursive removal and force.</summary>
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

/// <summary>Source and destination paths for renaming or moving an entry in the client-provided session filesystem.</summary>
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

/// <summary>Query results including rows, columns, and rows affected, or a filesystem error if execution failed.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionFsSqliteQueryResult
{
    /// <summary>Column names from the result set.</summary>
    [JsonPropertyName("columns")]
    public IList<string> Columns { get => field ??= []; set; }

    /// <summary>Describes a filesystem error.</summary>
    [JsonPropertyName("error")]
    public SessionFsError? Error { get; set; }

    /// <summary>SQLite last_insert_rowid() value for INSERT.</summary>
    [JsonPropertyName("lastInsertRowid")]
    public long? LastInsertRowid { get; set; }

    /// <summary>For SELECT: array of row objects. For others: empty array.</summary>
    [JsonPropertyName("rows")]
    public IList<IDictionary<string, JsonElement>> Rows { get => field ??= []; set; }

    /// <summary>Number of rows affected (for INSERT/UPDATE/DELETE).</summary>
    [JsonPropertyName("rowsAffected")]
    public long RowsAffected { get; set; }
}

/// <summary>SQL query, query type, and optional bind parameters for executing a SQLite query against the per-session database.</summary>
public sealed class SessionFsSqliteQueryRequest
{
    /// <summary>Optional named bind parameters.</summary>
    [JsonPropertyName("params")]
    public IDictionary<string, JsonElement>? Params { get; set; }

    /// <summary>SQL query to execute.</summary>
    [JsonPropertyName("query")]
    public string Query { get; set; } = string.Empty;

    /// <summary>How to execute the query: 'exec' for DDL/multi-statement (no results), 'query' for SELECT (returns rows), 'run' for INSERT/UPDATE/DELETE (returns rowsAffected).</summary>
    [JsonPropertyName("queryType")]
    public SessionFsSqliteQueryType QueryType { get; set; }

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Indicates whether the per-session SQLite database already exists.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionFsSqliteExistsResult
{
    /// <summary>Whether the session database already exists.</summary>
    [JsonPropertyName("exists")]
    public bool Exists { get; set; }
}

/// <summary>Identifies the target session.</summary>
public sealed class SessionFsSqliteExistsRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Canvas open result returned by the provider.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasProviderOpenResult
{
    /// <summary>Provider-supplied status text.</summary>
    [JsonPropertyName("status")]
    public string? Status { get; set; }

    /// <summary>Provider-supplied title.</summary>
    [JsonPropertyName("title")]
    public string? Title { get; set; }

    /// <summary>URL for web-rendered canvases.</summary>
    [JsonPropertyName("url")]
    public string? Url { get; set; }
}

/// <summary>Host capabilities.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasHostContextCapabilities
{
    /// <summary>Whether canvas rendering is supported.</summary>
    [JsonPropertyName("canvases")]
    public bool? Canvases { get; set; }
}

/// <summary>Host context supplied by the runtime.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasHostContext
{
    /// <summary>Host capabilities.</summary>
    [JsonPropertyName("capabilities")]
    public CanvasHostContextCapabilities? Capabilities { get; set; }
}

/// <summary>Canvas open parameters sent to the provider.</summary>
public sealed class CanvasProviderOpenRequest
{
    /// <summary>Provider-local canvas identifier.</summary>
    [JsonPropertyName("canvasId")]
    public string CanvasId { get; set; } = string.Empty;

    /// <summary>Owning provider identifier.</summary>
    [JsonPropertyName("extensionId")]
    public string ExtensionId { get; set; } = string.Empty;

    /// <summary>Host context supplied by the runtime.</summary>
    [JsonPropertyName("host")]
    public CanvasHostContext? Host { get; set; }

    /// <summary>Canvas open input.</summary>
    [JsonPropertyName("input")]
    public JsonElement? Input { get; set; }

    /// <summary>Stable caller-supplied canvas instance identifier.</summary>
    [JsonPropertyName("instanceId")]
    public string InstanceId { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Canvas close parameters sent to the provider.</summary>
public sealed class CanvasProviderCloseRequest
{
    /// <summary>Provider-local canvas identifier.</summary>
    [JsonPropertyName("canvasId")]
    public string CanvasId { get; set; } = string.Empty;

    /// <summary>Owning provider identifier.</summary>
    [JsonPropertyName("extensionId")]
    public string ExtensionId { get; set; } = string.Empty;

    /// <summary>Host context supplied by the runtime.</summary>
    [JsonPropertyName("host")]
    public CanvasHostContext? Host { get; set; }

    /// <summary>Canvas instance identifier.</summary>
    [JsonPropertyName("instanceId")]
    public string InstanceId { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Canvas action invocation parameters sent to the provider.</summary>
public sealed class CanvasProviderInvokeActionRequest
{
    /// <summary>Action name to invoke.</summary>
    [JsonPropertyName("actionName")]
    public string ActionName { get; set; } = string.Empty;

    /// <summary>Provider-local canvas identifier.</summary>
    [JsonPropertyName("canvasId")]
    public string CanvasId { get; set; } = string.Empty;

    /// <summary>Owning provider identifier.</summary>
    [JsonPropertyName("extensionId")]
    public string ExtensionId { get; set; } = string.Empty;

    /// <summary>Host context supplied by the runtime.</summary>
    [JsonPropertyName("host")]
    public CanvasHostContext? Host { get; set; }

    /// <summary>Action input.</summary>
    [JsonPropertyName("input")]
    public JsonElement? Input { get; set; }

    /// <summary>Canvas instance identifier.</summary>
    [JsonPropertyName("instanceId")]
    public string InstanceId { get; set; } = string.Empty;

    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>Model capability category for grouping in the model picker.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct ModelPickerCategory : IEquatable<ModelPickerCategory>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="ModelPickerCategory"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="ModelPickerCategory"/>.</param>
    [JsonConstructor]
    public ModelPickerCategory(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="ModelPickerCategory"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Lightweight model category optimized for faster, lower-cost interactions.</summary>
    public static ModelPickerCategory Lightweight { get; } = new("lightweight");

    /// <summary>Versatile model category suitable for a broad range of tasks.</summary>
    public static ModelPickerCategory Versatile { get; } = new("versatile");

    /// <summary>Powerful model category optimized for complex tasks.</summary>
    public static ModelPickerCategory Powerful { get; } = new("powerful");

    /// <summary>Returns a value indicating whether two <see cref="ModelPickerCategory"/> instances are equivalent.</summary>
    public static bool operator ==(ModelPickerCategory left, ModelPickerCategory right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="ModelPickerCategory"/> instances are not equivalent.</summary>
    public static bool operator !=(ModelPickerCategory left, ModelPickerCategory right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is ModelPickerCategory other && Equals(other);

    /// <inheritdoc />
    public bool Equals(ModelPickerCategory other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{ModelPickerCategory}"/> for serializing <see cref="ModelPickerCategory"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<ModelPickerCategory>
    {
        /// <inheritdoc />
        public override ModelPickerCategory Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, ModelPickerCategory value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(ModelPickerCategory));
        }
    }
}


/// <summary>Relative cost tier for token-based billing users.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct ModelPickerPriceCategory : IEquatable<ModelPickerPriceCategory>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="ModelPickerPriceCategory"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="ModelPickerPriceCategory"/>.</param>
    [JsonConstructor]
    public ModelPickerPriceCategory(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="ModelPickerPriceCategory"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Lowest relative token cost tier.</summary>
    public static ModelPickerPriceCategory Low { get; } = new("low");

    /// <summary>Medium relative token cost tier.</summary>
    public static ModelPickerPriceCategory Medium { get; } = new("medium");

    /// <summary>High relative token cost tier.</summary>
    public static ModelPickerPriceCategory High { get; } = new("high");

    /// <summary>Highest relative token cost tier.</summary>
    public static ModelPickerPriceCategory VeryHigh { get; } = new("very_high");

    /// <summary>Returns a value indicating whether two <see cref="ModelPickerPriceCategory"/> instances are equivalent.</summary>
    public static bool operator ==(ModelPickerPriceCategory left, ModelPickerPriceCategory right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="ModelPickerPriceCategory"/> instances are not equivalent.</summary>
    public static bool operator !=(ModelPickerPriceCategory left, ModelPickerPriceCategory right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is ModelPickerPriceCategory other && Equals(other);

    /// <inheritdoc />
    public bool Equals(ModelPickerPriceCategory other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{ModelPickerPriceCategory}"/> for serializing <see cref="ModelPickerPriceCategory"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<ModelPickerPriceCategory>
    {
        /// <inheritdoc />
        public override ModelPickerPriceCategory Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, ModelPickerPriceCategory value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(ModelPickerPriceCategory));
        }
    }
}


/// <summary>Current policy state for this model.</summary>
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct ModelPolicyState : IEquatable<ModelPolicyState>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="ModelPolicyState"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="ModelPolicyState"/>.</param>
    [JsonConstructor]
    public ModelPolicyState(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="ModelPolicyState"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>The model is enabled by policy.</summary>
    public static ModelPolicyState Enabled { get; } = new("enabled");

    /// <summary>The model is disabled by policy.</summary>
    public static ModelPolicyState Disabled { get; } = new("disabled");

    /// <summary>No explicit policy is configured for the model.</summary>
    public static ModelPolicyState Unconfigured { get; } = new("unconfigured");

    /// <summary>Returns a value indicating whether two <see cref="ModelPolicyState"/> instances are equivalent.</summary>
    public static bool operator ==(ModelPolicyState left, ModelPolicyState right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="ModelPolicyState"/> instances are not equivalent.</summary>
    public static bool operator !=(ModelPolicyState left, ModelPolicyState right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is ModelPolicyState other && Equals(other);

    /// <inheritdoc />
    public bool Equals(ModelPolicyState other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{ModelPolicyState}"/> for serializing <see cref="ModelPolicyState"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<ModelPolicyState>
    {
        /// <inheritdoc />
        public override ModelPolicyState Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, ModelPolicyState value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(ModelPolicyState));
        }
    }
}


/// <summary>Server transport type: stdio, http, sse (deprecated), or memory.</summary>
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

    /// <summary>Server communicates over stdio with a local child process.</summary>
    public static DiscoveredMcpServerType Stdio { get; } = new("stdio");

    /// <summary>Server communicates over streamable HTTP.</summary>
    public static DiscoveredMcpServerType Http { get; } = new("http");

    /// <summary>Server communicates over Server-Sent Events (deprecated).</summary>
    public static DiscoveredMcpServerType Sse { get; } = new("sse");

    /// <summary>Server is backed by an in-memory runtime implementation.</summary>
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
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, DiscoveredMcpServerType value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(DiscoveredMcpServerType));
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

    /// <summary>Paths use Windows path conventions.</summary>
    public static SessionFsSetProviderConventions Windows { get; } = new("windows");

    /// <summary>Paths use POSIX path conventions.</summary>
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
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SessionFsSetProviderConventions value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SessionFsSetProviderConventions));
        }
    }
}


/// <summary>Neutral SDK discriminator for the connected remote session kind.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct ConnectedRemoteSessionMetadataKind : IEquatable<ConnectedRemoteSessionMetadataKind>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="ConnectedRemoteSessionMetadataKind"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="ConnectedRemoteSessionMetadataKind"/>.</param>
    [JsonConstructor]
    public ConnectedRemoteSessionMetadataKind(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="ConnectedRemoteSessionMetadataKind"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Remote CLI session.</summary>
    public static ConnectedRemoteSessionMetadataKind RemoteSession { get; } = new("remote-session");

    /// <summary>GitHub Copilot coding agent session.</summary>
    public static ConnectedRemoteSessionMetadataKind CodingAgent { get; } = new("coding-agent");

    /// <summary>Returns a value indicating whether two <see cref="ConnectedRemoteSessionMetadataKind"/> instances are equivalent.</summary>
    public static bool operator ==(ConnectedRemoteSessionMetadataKind left, ConnectedRemoteSessionMetadataKind right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="ConnectedRemoteSessionMetadataKind"/> instances are not equivalent.</summary>
    public static bool operator !=(ConnectedRemoteSessionMetadataKind left, ConnectedRemoteSessionMetadataKind right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is ConnectedRemoteSessionMetadataKind other && Equals(other);

    /// <inheritdoc />
    public bool Equals(ConnectedRemoteSessionMetadataKind other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{ConnectedRemoteSessionMetadataKind}"/> for serializing <see cref="ConnectedRemoteSessionMetadataKind"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<ConnectedRemoteSessionMetadataKind>
    {
        /// <inheritdoc />
        public override ConnectedRemoteSessionMetadataKind Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, ConnectedRemoteSessionMetadataKind value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(ConnectedRemoteSessionMetadataKind));
        }
    }
}


/// <summary>Repository host type.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct SessionContextHostType : IEquatable<SessionContextHostType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="SessionContextHostType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="SessionContextHostType"/>.</param>
    [JsonConstructor]
    public SessionContextHostType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="SessionContextHostType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Session repository is hosted on GitHub.</summary>
    public static SessionContextHostType Github { get; } = new("github");

    /// <summary>Session repository is hosted on Azure DevOps.</summary>
    public static SessionContextHostType Ado { get; } = new("ado");

    /// <summary>Returns a value indicating whether two <see cref="SessionContextHostType"/> instances are equivalent.</summary>
    public static bool operator ==(SessionContextHostType left, SessionContextHostType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="SessionContextHostType"/> instances are not equivalent.</summary>
    public static bool operator !=(SessionContextHostType left, SessionContextHostType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is SessionContextHostType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(SessionContextHostType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{SessionContextHostType}"/> for serializing <see cref="SessionContextHostType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<SessionContextHostType>
    {
        /// <inheritdoc />
        public override SessionContextHostType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SessionContextHostType value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SessionContextHostType));
        }
    }
}


/// <summary>The UI mode the agent was in when this message was sent. Defaults to the session's current mode.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct SendAgentMode : IEquatable<SendAgentMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="SendAgentMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="SendAgentMode"/>.</param>
    [JsonConstructor]
    public SendAgentMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="SendAgentMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>The agent is responding interactively to the user.</summary>
    public static SendAgentMode Interactive { get; } = new("interactive");

    /// <summary>The agent is preparing a plan before making changes.</summary>
    public static SendAgentMode Plan { get; } = new("plan");

    /// <summary>The agent is working autonomously toward task completion.</summary>
    public static SendAgentMode Autopilot { get; } = new("autopilot");

    /// <summary>The agent is in shell-focused UI mode.</summary>
    public static SendAgentMode Shell { get; } = new("shell");

    /// <summary>Returns a value indicating whether two <see cref="SendAgentMode"/> instances are equivalent.</summary>
    public static bool operator ==(SendAgentMode left, SendAgentMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="SendAgentMode"/> instances are not equivalent.</summary>
    public static bool operator !=(SendAgentMode left, SendAgentMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is SendAgentMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(SendAgentMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{SendAgentMode}"/> for serializing <see cref="SendAgentMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<SendAgentMode>
    {
        /// <inheritdoc />
        public override SendAgentMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SendAgentMode value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SendAgentMode));
        }
    }
}


/// <summary>Type of GitHub reference.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct SendAttachmentGithubReferenceType : IEquatable<SendAttachmentGithubReferenceType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="SendAttachmentGithubReferenceType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="SendAttachmentGithubReferenceType"/>.</param>
    [JsonConstructor]
    public SendAttachmentGithubReferenceType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="SendAttachmentGithubReferenceType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>GitHub issue reference.</summary>
    public static SendAttachmentGithubReferenceType Issue { get; } = new("issue");

    /// <summary>GitHub pull request reference.</summary>
    public static SendAttachmentGithubReferenceType Pr { get; } = new("pr");

    /// <summary>GitHub discussion reference.</summary>
    public static SendAttachmentGithubReferenceType Discussion { get; } = new("discussion");

    /// <summary>Returns a value indicating whether two <see cref="SendAttachmentGithubReferenceType"/> instances are equivalent.</summary>
    public static bool operator ==(SendAttachmentGithubReferenceType left, SendAttachmentGithubReferenceType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="SendAttachmentGithubReferenceType"/> instances are not equivalent.</summary>
    public static bool operator !=(SendAttachmentGithubReferenceType left, SendAttachmentGithubReferenceType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is SendAttachmentGithubReferenceType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(SendAttachmentGithubReferenceType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{SendAttachmentGithubReferenceType}"/> for serializing <see cref="SendAttachmentGithubReferenceType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<SendAttachmentGithubReferenceType>
    {
        /// <inheritdoc />
        public override SendAttachmentGithubReferenceType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SendAttachmentGithubReferenceType value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SendAttachmentGithubReferenceType));
        }
    }
}


/// <summary>How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct SendMode : IEquatable<SendMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="SendMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="SendMode"/>.</param>
    [JsonConstructor]
    public SendMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="SendMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Append the message to the normal session queue.</summary>
    public static SendMode Enqueue { get; } = new("enqueue");

    /// <summary>Interject the message during the in-progress turn.</summary>
    public static SendMode Immediate { get; } = new("immediate");

    /// <summary>Returns a value indicating whether two <see cref="SendMode"/> instances are equivalent.</summary>
    public static bool operator ==(SendMode left, SendMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="SendMode"/> instances are not equivalent.</summary>
    public static bool operator !=(SendMode left, SendMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is SendMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(SendMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{SendMode}"/> for serializing <see cref="SendMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<SendMode>
    {
        /// <inheritdoc />
        public override SendMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SendMode value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SendMode));
        }
    }
}


/// <summary>Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>Informational message.</summary>
    public static SessionLogLevel Info { get; } = new("info");

    /// <summary>Warning message that may require attention.</summary>
    public static SessionLogLevel Warning { get; } = new("warning");

    /// <summary>Error message describing a failure.</summary>
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
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SessionLogLevel value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SessionLogLevel));
        }
    }
}


/// <summary>Authentication type.</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>Authentication provided by a GitHub App HMAC credential.</summary>
    public static AuthInfoType Hmac { get; } = new("hmac");

    /// <summary>Authentication resolved from environment-provided credentials.</summary>
    public static AuthInfoType Env { get; } = new("env");

    /// <summary>Authentication from an interactive user sign-in.</summary>
    public static AuthInfoType User { get; } = new("user");

    /// <summary>Authentication delegated to the GitHub CLI.</summary>
    public static AuthInfoType GhCli { get; } = new("gh-cli");

    /// <summary>Authentication from an API key credential.</summary>
    public static AuthInfoType ApiKey { get; } = new("api-key");

    /// <summary>Authentication from a GitHub token.</summary>
    public static AuthInfoType Token { get; } = new("token");

    /// <summary>Authentication from a Copilot API token.</summary>
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
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, AuthInfoType value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(AuthInfoType));
        }
    }
}


/// <summary>Runtime-controlled routing state for an open canvas instance.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct CanvasInstanceAvailability : IEquatable<CanvasInstanceAvailability>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="CanvasInstanceAvailability"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="CanvasInstanceAvailability"/>.</param>
    [JsonConstructor]
    public CanvasInstanceAvailability(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="CanvasInstanceAvailability"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>The owning provider is currently connected and routing calls will be dispatched normally.</summary>
    public static CanvasInstanceAvailability Ready { get; } = new("ready");

    /// <summary>The owning provider is not currently connected. Routing calls fail with canvas_provider_unavailable until the agent re-issues open_canvas (which rehydrates via a fresh canvas.open) or the provider reconnects.</summary>
    public static CanvasInstanceAvailability Stale { get; } = new("stale");

    /// <summary>Returns a value indicating whether two <see cref="CanvasInstanceAvailability"/> instances are equivalent.</summary>
    public static bool operator ==(CanvasInstanceAvailability left, CanvasInstanceAvailability right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="CanvasInstanceAvailability"/> instances are not equivalent.</summary>
    public static bool operator !=(CanvasInstanceAvailability left, CanvasInstanceAvailability right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is CanvasInstanceAvailability other && Equals(other);

    /// <inheritdoc />
    public bool Equals(CanvasInstanceAvailability other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{CanvasInstanceAvailability}"/> for serializing <see cref="CanvasInstanceAvailability"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<CanvasInstanceAvailability>
    {
        /// <inheritdoc />
        public override CanvasInstanceAvailability Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, CanvasInstanceAvailability value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(CanvasInstanceAvailability));
        }
    }
}


/// <summary>Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct WorkspacesWorkspaceDetailsHostType : IEquatable<WorkspacesWorkspaceDetailsHostType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="WorkspacesWorkspaceDetailsHostType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="WorkspacesWorkspaceDetailsHostType"/>.</param>
    [JsonConstructor]
    public WorkspacesWorkspaceDetailsHostType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="WorkspacesWorkspaceDetailsHostType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Workspace repository is hosted on GitHub.</summary>
    public static WorkspacesWorkspaceDetailsHostType Github { get; } = new("github");

    /// <summary>Workspace repository is hosted on Azure DevOps.</summary>
    public static WorkspacesWorkspaceDetailsHostType Ado { get; } = new("ado");

    /// <summary>Returns a value indicating whether two <see cref="WorkspacesWorkspaceDetailsHostType"/> instances are equivalent.</summary>
    public static bool operator ==(WorkspacesWorkspaceDetailsHostType left, WorkspacesWorkspaceDetailsHostType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="WorkspacesWorkspaceDetailsHostType"/> instances are not equivalent.</summary>
    public static bool operator !=(WorkspacesWorkspaceDetailsHostType left, WorkspacesWorkspaceDetailsHostType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is WorkspacesWorkspaceDetailsHostType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(WorkspacesWorkspaceDetailsHostType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{WorkspacesWorkspaceDetailsHostType}"/> for serializing <see cref="WorkspacesWorkspaceDetailsHostType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<WorkspacesWorkspaceDetailsHostType>
    {
        /// <inheritdoc />
        public override WorkspacesWorkspaceDetailsHostType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, WorkspacesWorkspaceDetailsHostType value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(WorkspacesWorkspaceDetailsHostType));
        }
    }
}


/// <summary>Type of change represented by this file diff.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct WorkspaceDiffFileChangeType : IEquatable<WorkspaceDiffFileChangeType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="WorkspaceDiffFileChangeType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="WorkspaceDiffFileChangeType"/>.</param>
    [JsonConstructor]
    public WorkspaceDiffFileChangeType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="WorkspaceDiffFileChangeType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>The file was added.</summary>
    public static WorkspaceDiffFileChangeType Added { get; } = new("added");

    /// <summary>The file was modified.</summary>
    public static WorkspaceDiffFileChangeType Modified { get; } = new("modified");

    /// <summary>The file was deleted.</summary>
    public static WorkspaceDiffFileChangeType Deleted { get; } = new("deleted");

    /// <summary>The file was renamed.</summary>
    public static WorkspaceDiffFileChangeType Renamed { get; } = new("renamed");

    /// <summary>Returns a value indicating whether two <see cref="WorkspaceDiffFileChangeType"/> instances are equivalent.</summary>
    public static bool operator ==(WorkspaceDiffFileChangeType left, WorkspaceDiffFileChangeType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="WorkspaceDiffFileChangeType"/> instances are not equivalent.</summary>
    public static bool operator !=(WorkspaceDiffFileChangeType left, WorkspaceDiffFileChangeType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is WorkspaceDiffFileChangeType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(WorkspaceDiffFileChangeType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{WorkspaceDiffFileChangeType}"/> for serializing <see cref="WorkspaceDiffFileChangeType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<WorkspaceDiffFileChangeType>
    {
        /// <inheritdoc />
        public override WorkspaceDiffFileChangeType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, WorkspaceDiffFileChangeType value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(WorkspaceDiffFileChangeType));
        }
    }
}


/// <summary>Diff mode requested by the client.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct WorkspaceDiffMode : IEquatable<WorkspaceDiffMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="WorkspaceDiffMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="WorkspaceDiffMode"/>.</param>
    [JsonConstructor]
    public WorkspaceDiffMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="WorkspaceDiffMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Return staged, unstaged, and untracked working tree changes.</summary>
    public static WorkspaceDiffMode Unstaged { get; } = new("unstaged");

    /// <summary>Return changes compared with the default branch.</summary>
    public static WorkspaceDiffMode Branch { get; } = new("branch");

    /// <summary>Returns a value indicating whether two <see cref="WorkspaceDiffMode"/> instances are equivalent.</summary>
    public static bool operator ==(WorkspaceDiffMode left, WorkspaceDiffMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="WorkspaceDiffMode"/> instances are not equivalent.</summary>
    public static bool operator !=(WorkspaceDiffMode left, WorkspaceDiffMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is WorkspaceDiffMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(WorkspaceDiffMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{WorkspaceDiffMode}"/> for serializing <see cref="WorkspaceDiffMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<WorkspaceDiffMode>
    {
        /// <inheritdoc />
        public override WorkspaceDiffMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, WorkspaceDiffMode value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(WorkspaceDiffMode));
        }
    }
}


/// <summary>Where this source lives — used for UI grouping.</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>Instructions live in user-level configuration.</summary>
    public static InstructionsSourcesLocation User { get; } = new("user");

    /// <summary>Instructions live in repository-level configuration.</summary>
    public static InstructionsSourcesLocation Repository { get; } = new("repository");

    /// <summary>Instructions live under the current working directory.</summary>
    public static InstructionsSourcesLocation WorkingDirectory { get; } = new("working-directory");

    /// <summary>Instructions live in plugin-provided configuration.</summary>
    public static InstructionsSourcesLocation Plugin { get; } = new("plugin");

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
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, InstructionsSourcesLocation value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(InstructionsSourcesLocation));
        }
    }
}


/// <summary>Category of instruction source — used for merge logic.</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>Instructions loaded from the user's home configuration.</summary>
    public static InstructionsSourcesType Home { get; } = new("home");

    /// <summary>Instructions loaded from repository-scoped files.</summary>
    public static InstructionsSourcesType Repo { get; } = new("repo");

    /// <summary>Instructions loaded from model-specific files.</summary>
    public static InstructionsSourcesType Model { get; } = new("model");

    /// <summary>Instructions loaded from VS Code instruction files.</summary>
    public static InstructionsSourcesType Vscode { get; } = new("vscode");

    /// <summary>Instructions discovered from nested agent files.</summary>
    public static InstructionsSourcesType NestedAgents { get; } = new("nested-agents");

    /// <summary>Instructions inherited from child instruction files.</summary>
    public static InstructionsSourcesType ChildInstructions { get; } = new("child-instructions");

    /// <summary>Instructions supplied by an installed plugin.</summary>
    public static InstructionsSourcesType Plugin { get; } = new("plugin");

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
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, InstructionsSourcesType value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(InstructionsSourcesType));
        }
    }
}


/// <summary>Where the agent definition was loaded from.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct AgentInfoSource : IEquatable<AgentInfoSource>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="AgentInfoSource"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="AgentInfoSource"/>.</param>
    [JsonConstructor]
    public AgentInfoSource(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="AgentInfoSource"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Agent loaded from the user's personal agent configuration.</summary>
    public static AgentInfoSource User { get; } = new("user");

    /// <summary>Agent loaded from the current project's repository configuration.</summary>
    public static AgentInfoSource Project { get; } = new("project");

    /// <summary>Agent inherited from a parent project or workspace.</summary>
    public static AgentInfoSource Inherited { get; } = new("inherited");

    /// <summary>Agent provided by a remote runtime or service.</summary>
    public static AgentInfoSource Remote { get; } = new("remote");

    /// <summary>Agent contributed by an installed plugin.</summary>
    public static AgentInfoSource Plugin { get; } = new("plugin");

    /// <summary>Agent built into the Copilot runtime.</summary>
    public static AgentInfoSource Builtin { get; } = new("builtin");

    /// <summary>Returns a value indicating whether two <see cref="AgentInfoSource"/> instances are equivalent.</summary>
    public static bool operator ==(AgentInfoSource left, AgentInfoSource right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="AgentInfoSource"/> instances are not equivalent.</summary>
    public static bool operator !=(AgentInfoSource left, AgentInfoSource right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is AgentInfoSource other && Equals(other);

    /// <inheritdoc />
    public bool Equals(AgentInfoSource other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{AgentInfoSource}"/> for serializing <see cref="AgentInfoSource"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<AgentInfoSource>
    {
        /// <inheritdoc />
        public override AgentInfoSource Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, AgentInfoSource value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(AgentInfoSource));
        }
    }
}


/// <summary>Whether task execution is synchronously awaited or managed in the background.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct TaskExecutionMode : IEquatable<TaskExecutionMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="TaskExecutionMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="TaskExecutionMode"/>.</param>
    [JsonConstructor]
    public TaskExecutionMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="TaskExecutionMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>The task was started with synchronous waiting.</summary>
    public static TaskExecutionMode Sync { get; } = new("sync");

    /// <summary>The task is managed in the background.</summary>
    public static TaskExecutionMode Background { get; } = new("background");

    /// <summary>Returns a value indicating whether two <see cref="TaskExecutionMode"/> instances are equivalent.</summary>
    public static bool operator ==(TaskExecutionMode left, TaskExecutionMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="TaskExecutionMode"/> instances are not equivalent.</summary>
    public static bool operator !=(TaskExecutionMode left, TaskExecutionMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is TaskExecutionMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(TaskExecutionMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{TaskExecutionMode}"/> for serializing <see cref="TaskExecutionMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<TaskExecutionMode>
    {
        /// <inheritdoc />
        public override TaskExecutionMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, TaskExecutionMode value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(TaskExecutionMode));
        }
    }
}


/// <summary>Current lifecycle status of the task.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct TaskStatus : IEquatable<TaskStatus>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="TaskStatus"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="TaskStatus"/>.</param>
    [JsonConstructor]
    public TaskStatus(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="TaskStatus"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>The task is actively executing.</summary>
    public static TaskStatus Running { get; } = new("running");

    /// <summary>The task is waiting for additional input.</summary>
    public static TaskStatus Idle { get; } = new("idle");

    /// <summary>The task finished successfully.</summary>
    public static TaskStatus Completed { get; } = new("completed");

    /// <summary>The task finished with an error.</summary>
    public static TaskStatus Failed { get; } = new("failed");

    /// <summary>The task was cancelled before completion.</summary>
    public static TaskStatus Cancelled { get; } = new("cancelled");

    /// <summary>Returns a value indicating whether two <see cref="TaskStatus"/> instances are equivalent.</summary>
    public static bool operator ==(TaskStatus left, TaskStatus right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="TaskStatus"/> instances are not equivalent.</summary>
    public static bool operator !=(TaskStatus left, TaskStatus right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is TaskStatus other && Equals(other);

    /// <inheritdoc />
    public bool Equals(TaskStatus other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{TaskStatus}"/> for serializing <see cref="TaskStatus"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<TaskStatus>
    {
        /// <inheritdoc />
        public override TaskStatus Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, TaskStatus value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(TaskStatus));
        }
    }
}


/// <summary>Whether the shell runs inside a managed PTY session or as an independent background process.</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>The shell runs in a managed PTY session.</summary>
    public static TaskShellInfoAttachmentMode Attached { get; } = new("attached");

    /// <summary>The shell runs as an independent background process.</summary>
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
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, TaskShellInfoAttachmentMode value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(TaskShellInfoAttachmentMode));
        }
    }
}


/// <summary>Outcome of the sampling inference. 'success' produced a response; 'failure' encountered an error (including agent-side rejection by content filter or criteria); 'cancelled' the caller cancelled this execution via cancelSamplingExecution.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct McpSamplingExecutionAction : IEquatable<McpSamplingExecutionAction>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="McpSamplingExecutionAction"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="McpSamplingExecutionAction"/>.</param>
    [JsonConstructor]
    public McpSamplingExecutionAction(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="McpSamplingExecutionAction"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>The sampling inference completed and produced a result.</summary>
    public static McpSamplingExecutionAction Success { get; } = new("success");

    /// <summary>The sampling inference failed or was rejected.</summary>
    public static McpSamplingExecutionAction Failure { get; } = new("failure");

    /// <summary>The sampling inference was cancelled before completion.</summary>
    public static McpSamplingExecutionAction Cancelled { get; } = new("cancelled");

    /// <summary>Returns a value indicating whether two <see cref="McpSamplingExecutionAction"/> instances are equivalent.</summary>
    public static bool operator ==(McpSamplingExecutionAction left, McpSamplingExecutionAction right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="McpSamplingExecutionAction"/> instances are not equivalent.</summary>
    public static bool operator !=(McpSamplingExecutionAction left, McpSamplingExecutionAction right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is McpSamplingExecutionAction other && Equals(other);

    /// <inheritdoc />
    public bool Equals(McpSamplingExecutionAction other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{McpSamplingExecutionAction}"/> for serializing <see cref="McpSamplingExecutionAction"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<McpSamplingExecutionAction>
    {
        /// <inheritdoc />
        public override McpSamplingExecutionAction Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, McpSamplingExecutionAction value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(McpSamplingExecutionAction));
        }
    }
}


/// <summary>How environment-variable values supplied to MCP servers are resolved. "direct" passes literal string values; "indirect" treats values as references (e.g. names of environment variables on the host) that the runtime resolves before launch. Defaults to the runtime's startup mode; clients that intentionally launch MCP servers with literal values (e.g. CLI prompt mode and ACP) set this to "direct".</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct McpSetEnvValueModeDetails : IEquatable<McpSetEnvValueModeDetails>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="McpSetEnvValueModeDetails"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="McpSetEnvValueModeDetails"/>.</param>
    [JsonConstructor]
    public McpSetEnvValueModeDetails(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="McpSetEnvValueModeDetails"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Treat MCP server environment values as literal strings.</summary>
    public static McpSetEnvValueModeDetails Direct { get; } = new("direct");

    /// <summary>Treat MCP server environment values as host-side references to resolve before launch.</summary>
    public static McpSetEnvValueModeDetails Indirect { get; } = new("indirect");

    /// <summary>Returns a value indicating whether two <see cref="McpSetEnvValueModeDetails"/> instances are equivalent.</summary>
    public static bool operator ==(McpSetEnvValueModeDetails left, McpSetEnvValueModeDetails right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="McpSetEnvValueModeDetails"/> instances are not equivalent.</summary>
    public static bool operator !=(McpSetEnvValueModeDetails left, McpSetEnvValueModeDetails right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is McpSetEnvValueModeDetails other && Equals(other);

    /// <inheritdoc />
    public bool Equals(McpSetEnvValueModeDetails other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{McpSetEnvValueModeDetails}"/> for serializing <see cref="McpSetEnvValueModeDetails"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<McpSetEnvValueModeDetails>
    {
        /// <inheritdoc />
        public override McpSetEnvValueModeDetails Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, McpSetEnvValueModeDetails value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(McpSetEnvValueModeDetails));
        }
    }
}


/// <summary>Allowed values for the `McpAppsSetHostContextDetailsAvailableDisplayMode` enumeration.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct McpAppsSetHostContextDetailsAvailableDisplayMode : IEquatable<McpAppsSetHostContextDetailsAvailableDisplayMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="McpAppsSetHostContextDetailsAvailableDisplayMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="McpAppsSetHostContextDetailsAvailableDisplayMode"/>.</param>
    [JsonConstructor]
    public McpAppsSetHostContextDetailsAvailableDisplayMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="McpAppsSetHostContextDetailsAvailableDisplayMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Rendered inline within the host conversation surface.</summary>
    public static McpAppsSetHostContextDetailsAvailableDisplayMode Inline { get; } = new("inline");

    /// <summary>Rendered as a fullscreen overlay.</summary>
    public static McpAppsSetHostContextDetailsAvailableDisplayMode Fullscreen { get; } = new("fullscreen");

    /// <summary>Rendered as a picture-in-picture floating panel.</summary>
    public static McpAppsSetHostContextDetailsAvailableDisplayMode Pip { get; } = new("pip");

    /// <summary>Returns a value indicating whether two <see cref="McpAppsSetHostContextDetailsAvailableDisplayMode"/> instances are equivalent.</summary>
    public static bool operator ==(McpAppsSetHostContextDetailsAvailableDisplayMode left, McpAppsSetHostContextDetailsAvailableDisplayMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="McpAppsSetHostContextDetailsAvailableDisplayMode"/> instances are not equivalent.</summary>
    public static bool operator !=(McpAppsSetHostContextDetailsAvailableDisplayMode left, McpAppsSetHostContextDetailsAvailableDisplayMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is McpAppsSetHostContextDetailsAvailableDisplayMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(McpAppsSetHostContextDetailsAvailableDisplayMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{McpAppsSetHostContextDetailsAvailableDisplayMode}"/> for serializing <see cref="McpAppsSetHostContextDetailsAvailableDisplayMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<McpAppsSetHostContextDetailsAvailableDisplayMode>
    {
        /// <inheritdoc />
        public override McpAppsSetHostContextDetailsAvailableDisplayMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, McpAppsSetHostContextDetailsAvailableDisplayMode value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(McpAppsSetHostContextDetailsAvailableDisplayMode));
        }
    }
}


/// <summary>Current display mode (SEP-1865).</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct McpAppsSetHostContextDetailsDisplayMode : IEquatable<McpAppsSetHostContextDetailsDisplayMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="McpAppsSetHostContextDetailsDisplayMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="McpAppsSetHostContextDetailsDisplayMode"/>.</param>
    [JsonConstructor]
    public McpAppsSetHostContextDetailsDisplayMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="McpAppsSetHostContextDetailsDisplayMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Rendered inline within the host conversation surface.</summary>
    public static McpAppsSetHostContextDetailsDisplayMode Inline { get; } = new("inline");

    /// <summary>Rendered as a fullscreen overlay.</summary>
    public static McpAppsSetHostContextDetailsDisplayMode Fullscreen { get; } = new("fullscreen");

    /// <summary>Rendered as a picture-in-picture floating panel.</summary>
    public static McpAppsSetHostContextDetailsDisplayMode Pip { get; } = new("pip");

    /// <summary>Returns a value indicating whether two <see cref="McpAppsSetHostContextDetailsDisplayMode"/> instances are equivalent.</summary>
    public static bool operator ==(McpAppsSetHostContextDetailsDisplayMode left, McpAppsSetHostContextDetailsDisplayMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="McpAppsSetHostContextDetailsDisplayMode"/> instances are not equivalent.</summary>
    public static bool operator !=(McpAppsSetHostContextDetailsDisplayMode left, McpAppsSetHostContextDetailsDisplayMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is McpAppsSetHostContextDetailsDisplayMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(McpAppsSetHostContextDetailsDisplayMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{McpAppsSetHostContextDetailsDisplayMode}"/> for serializing <see cref="McpAppsSetHostContextDetailsDisplayMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<McpAppsSetHostContextDetailsDisplayMode>
    {
        /// <inheritdoc />
        public override McpAppsSetHostContextDetailsDisplayMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, McpAppsSetHostContextDetailsDisplayMode value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(McpAppsSetHostContextDetailsDisplayMode));
        }
    }
}


/// <summary>Platform type for responsive design.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct McpAppsSetHostContextDetailsPlatform : IEquatable<McpAppsSetHostContextDetailsPlatform>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="McpAppsSetHostContextDetailsPlatform"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="McpAppsSetHostContextDetailsPlatform"/>.</param>
    [JsonConstructor]
    public McpAppsSetHostContextDetailsPlatform(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="McpAppsSetHostContextDetailsPlatform"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Host runs in a web browser.</summary>
    public static McpAppsSetHostContextDetailsPlatform Web { get; } = new("web");

    /// <summary>Host runs as a desktop application.</summary>
    public static McpAppsSetHostContextDetailsPlatform Desktop { get; } = new("desktop");

    /// <summary>Host runs on a mobile device.</summary>
    public static McpAppsSetHostContextDetailsPlatform Mobile { get; } = new("mobile");

    /// <summary>Returns a value indicating whether two <see cref="McpAppsSetHostContextDetailsPlatform"/> instances are equivalent.</summary>
    public static bool operator ==(McpAppsSetHostContextDetailsPlatform left, McpAppsSetHostContextDetailsPlatform right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="McpAppsSetHostContextDetailsPlatform"/> instances are not equivalent.</summary>
    public static bool operator !=(McpAppsSetHostContextDetailsPlatform left, McpAppsSetHostContextDetailsPlatform right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is McpAppsSetHostContextDetailsPlatform other && Equals(other);

    /// <inheritdoc />
    public bool Equals(McpAppsSetHostContextDetailsPlatform other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{McpAppsSetHostContextDetailsPlatform}"/> for serializing <see cref="McpAppsSetHostContextDetailsPlatform"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<McpAppsSetHostContextDetailsPlatform>
    {
        /// <inheritdoc />
        public override McpAppsSetHostContextDetailsPlatform Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, McpAppsSetHostContextDetailsPlatform value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(McpAppsSetHostContextDetailsPlatform));
        }
    }
}


/// <summary>UI theme preference per SEP-1865.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct McpAppsSetHostContextDetailsTheme : IEquatable<McpAppsSetHostContextDetailsTheme>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="McpAppsSetHostContextDetailsTheme"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="McpAppsSetHostContextDetailsTheme"/>.</param>
    [JsonConstructor]
    public McpAppsSetHostContextDetailsTheme(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="McpAppsSetHostContextDetailsTheme"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Light UI theme.</summary>
    public static McpAppsSetHostContextDetailsTheme Light { get; } = new("light");

    /// <summary>Dark UI theme.</summary>
    public static McpAppsSetHostContextDetailsTheme Dark { get; } = new("dark");

    /// <summary>Returns a value indicating whether two <see cref="McpAppsSetHostContextDetailsTheme"/> instances are equivalent.</summary>
    public static bool operator ==(McpAppsSetHostContextDetailsTheme left, McpAppsSetHostContextDetailsTheme right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="McpAppsSetHostContextDetailsTheme"/> instances are not equivalent.</summary>
    public static bool operator !=(McpAppsSetHostContextDetailsTheme left, McpAppsSetHostContextDetailsTheme right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is McpAppsSetHostContextDetailsTheme other && Equals(other);

    /// <inheritdoc />
    public bool Equals(McpAppsSetHostContextDetailsTheme other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{McpAppsSetHostContextDetailsTheme}"/> for serializing <see cref="McpAppsSetHostContextDetailsTheme"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<McpAppsSetHostContextDetailsTheme>
    {
        /// <inheritdoc />
        public override McpAppsSetHostContextDetailsTheme Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, McpAppsSetHostContextDetailsTheme value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(McpAppsSetHostContextDetailsTheme));
        }
    }
}


/// <summary>Allowed values for the `McpAppsHostContextDetailsAvailableDisplayMode` enumeration.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct McpAppsHostContextDetailsAvailableDisplayMode : IEquatable<McpAppsHostContextDetailsAvailableDisplayMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="McpAppsHostContextDetailsAvailableDisplayMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="McpAppsHostContextDetailsAvailableDisplayMode"/>.</param>
    [JsonConstructor]
    public McpAppsHostContextDetailsAvailableDisplayMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="McpAppsHostContextDetailsAvailableDisplayMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Rendered inline within the host conversation surface.</summary>
    public static McpAppsHostContextDetailsAvailableDisplayMode Inline { get; } = new("inline");

    /// <summary>Rendered as a fullscreen overlay.</summary>
    public static McpAppsHostContextDetailsAvailableDisplayMode Fullscreen { get; } = new("fullscreen");

    /// <summary>Rendered as a picture-in-picture floating panel.</summary>
    public static McpAppsHostContextDetailsAvailableDisplayMode Pip { get; } = new("pip");

    /// <summary>Returns a value indicating whether two <see cref="McpAppsHostContextDetailsAvailableDisplayMode"/> instances are equivalent.</summary>
    public static bool operator ==(McpAppsHostContextDetailsAvailableDisplayMode left, McpAppsHostContextDetailsAvailableDisplayMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="McpAppsHostContextDetailsAvailableDisplayMode"/> instances are not equivalent.</summary>
    public static bool operator !=(McpAppsHostContextDetailsAvailableDisplayMode left, McpAppsHostContextDetailsAvailableDisplayMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is McpAppsHostContextDetailsAvailableDisplayMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(McpAppsHostContextDetailsAvailableDisplayMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{McpAppsHostContextDetailsAvailableDisplayMode}"/> for serializing <see cref="McpAppsHostContextDetailsAvailableDisplayMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<McpAppsHostContextDetailsAvailableDisplayMode>
    {
        /// <inheritdoc />
        public override McpAppsHostContextDetailsAvailableDisplayMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, McpAppsHostContextDetailsAvailableDisplayMode value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(McpAppsHostContextDetailsAvailableDisplayMode));
        }
    }
}


/// <summary>Current display mode (SEP-1865).</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct McpAppsHostContextDetailsDisplayMode : IEquatable<McpAppsHostContextDetailsDisplayMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="McpAppsHostContextDetailsDisplayMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="McpAppsHostContextDetailsDisplayMode"/>.</param>
    [JsonConstructor]
    public McpAppsHostContextDetailsDisplayMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="McpAppsHostContextDetailsDisplayMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Rendered inline within the host conversation surface.</summary>
    public static McpAppsHostContextDetailsDisplayMode Inline { get; } = new("inline");

    /// <summary>Rendered as a fullscreen overlay.</summary>
    public static McpAppsHostContextDetailsDisplayMode Fullscreen { get; } = new("fullscreen");

    /// <summary>Rendered as a picture-in-picture floating panel.</summary>
    public static McpAppsHostContextDetailsDisplayMode Pip { get; } = new("pip");

    /// <summary>Returns a value indicating whether two <see cref="McpAppsHostContextDetailsDisplayMode"/> instances are equivalent.</summary>
    public static bool operator ==(McpAppsHostContextDetailsDisplayMode left, McpAppsHostContextDetailsDisplayMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="McpAppsHostContextDetailsDisplayMode"/> instances are not equivalent.</summary>
    public static bool operator !=(McpAppsHostContextDetailsDisplayMode left, McpAppsHostContextDetailsDisplayMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is McpAppsHostContextDetailsDisplayMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(McpAppsHostContextDetailsDisplayMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{McpAppsHostContextDetailsDisplayMode}"/> for serializing <see cref="McpAppsHostContextDetailsDisplayMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<McpAppsHostContextDetailsDisplayMode>
    {
        /// <inheritdoc />
        public override McpAppsHostContextDetailsDisplayMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, McpAppsHostContextDetailsDisplayMode value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(McpAppsHostContextDetailsDisplayMode));
        }
    }
}


/// <summary>Platform type for responsive design.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct McpAppsHostContextDetailsPlatform : IEquatable<McpAppsHostContextDetailsPlatform>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="McpAppsHostContextDetailsPlatform"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="McpAppsHostContextDetailsPlatform"/>.</param>
    [JsonConstructor]
    public McpAppsHostContextDetailsPlatform(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="McpAppsHostContextDetailsPlatform"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Host runs in a web browser.</summary>
    public static McpAppsHostContextDetailsPlatform Web { get; } = new("web");

    /// <summary>Host runs as a desktop application.</summary>
    public static McpAppsHostContextDetailsPlatform Desktop { get; } = new("desktop");

    /// <summary>Host runs on a mobile device.</summary>
    public static McpAppsHostContextDetailsPlatform Mobile { get; } = new("mobile");

    /// <summary>Returns a value indicating whether two <see cref="McpAppsHostContextDetailsPlatform"/> instances are equivalent.</summary>
    public static bool operator ==(McpAppsHostContextDetailsPlatform left, McpAppsHostContextDetailsPlatform right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="McpAppsHostContextDetailsPlatform"/> instances are not equivalent.</summary>
    public static bool operator !=(McpAppsHostContextDetailsPlatform left, McpAppsHostContextDetailsPlatform right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is McpAppsHostContextDetailsPlatform other && Equals(other);

    /// <inheritdoc />
    public bool Equals(McpAppsHostContextDetailsPlatform other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{McpAppsHostContextDetailsPlatform}"/> for serializing <see cref="McpAppsHostContextDetailsPlatform"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<McpAppsHostContextDetailsPlatform>
    {
        /// <inheritdoc />
        public override McpAppsHostContextDetailsPlatform Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, McpAppsHostContextDetailsPlatform value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(McpAppsHostContextDetailsPlatform));
        }
    }
}


/// <summary>UI theme preference per SEP-1865.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct McpAppsHostContextDetailsTheme : IEquatable<McpAppsHostContextDetailsTheme>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="McpAppsHostContextDetailsTheme"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="McpAppsHostContextDetailsTheme"/>.</param>
    [JsonConstructor]
    public McpAppsHostContextDetailsTheme(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="McpAppsHostContextDetailsTheme"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Light UI theme.</summary>
    public static McpAppsHostContextDetailsTheme Light { get; } = new("light");

    /// <summary>Dark UI theme.</summary>
    public static McpAppsHostContextDetailsTheme Dark { get; } = new("dark");

    /// <summary>Returns a value indicating whether two <see cref="McpAppsHostContextDetailsTheme"/> instances are equivalent.</summary>
    public static bool operator ==(McpAppsHostContextDetailsTheme left, McpAppsHostContextDetailsTheme right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="McpAppsHostContextDetailsTheme"/> instances are not equivalent.</summary>
    public static bool operator !=(McpAppsHostContextDetailsTheme left, McpAppsHostContextDetailsTheme right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is McpAppsHostContextDetailsTheme other && Equals(other);

    /// <inheritdoc />
    public bool Equals(McpAppsHostContextDetailsTheme other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{McpAppsHostContextDetailsTheme}"/> for serializing <see cref="McpAppsHostContextDetailsTheme"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<McpAppsHostContextDetailsTheme>
    {
        /// <inheritdoc />
        public override McpAppsHostContextDetailsTheme Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, McpAppsHostContextDetailsTheme value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(McpAppsHostContextDetailsTheme));
        }
    }
}


/// <summary>How env values are passed to MCP servers (`direct` inlines literal values; `indirect` resolves at launch).</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct OptionsUpdateEnvValueMode : IEquatable<OptionsUpdateEnvValueMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="OptionsUpdateEnvValueMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="OptionsUpdateEnvValueMode"/>.</param>
    [JsonConstructor]
    public OptionsUpdateEnvValueMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="OptionsUpdateEnvValueMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Pass MCP server environment values as literal strings.</summary>
    public static OptionsUpdateEnvValueMode Direct { get; } = new("direct");

    /// <summary>Resolve MCP server environment values from host-side references.</summary>
    public static OptionsUpdateEnvValueMode Indirect { get; } = new("indirect");

    /// <summary>Returns a value indicating whether two <see cref="OptionsUpdateEnvValueMode"/> instances are equivalent.</summary>
    public static bool operator ==(OptionsUpdateEnvValueMode left, OptionsUpdateEnvValueMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="OptionsUpdateEnvValueMode"/> instances are not equivalent.</summary>
    public static bool operator !=(OptionsUpdateEnvValueMode left, OptionsUpdateEnvValueMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is OptionsUpdateEnvValueMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(OptionsUpdateEnvValueMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{OptionsUpdateEnvValueMode}"/> for serializing <see cref="OptionsUpdateEnvValueMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<OptionsUpdateEnvValueMode>
    {
        /// <inheritdoc />
        public override OptionsUpdateEnvValueMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, OptionsUpdateEnvValueMode value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(OptionsUpdateEnvValueMode));
        }
    }
}


/// <summary>Discovery source: project (.github/extensions/) or user (~/.copilot/extensions/).</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>Extension discovered from the current project's .github/extensions directory.</summary>
    public static ExtensionSource Project { get; } = new("project");

    /// <summary>Extension discovered from the user's ~/.copilot/extensions directory.</summary>
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
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, ExtensionSource value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(ExtensionSource));
        }
    }
}


/// <summary>Current status: running, disabled, failed, or starting.</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>The extension process is running.</summary>
    public static ExtensionStatus Running { get; } = new("running");

    /// <summary>The extension is installed but disabled.</summary>
    public static ExtensionStatus Disabled { get; } = new("disabled");

    /// <summary>The extension failed to start or crashed.</summary>
    public static ExtensionStatus Failed { get; } = new("failed");

    /// <summary>The extension process is starting.</summary>
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
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, ExtensionStatus value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(ExtensionStatus));
        }
    }
}


/// <summary>Optional completion hint for the input (e.g. 'directory' for filesystem path completion).</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct SlashCommandInputCompletion : IEquatable<SlashCommandInputCompletion>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="SlashCommandInputCompletion"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="SlashCommandInputCompletion"/>.</param>
    [JsonConstructor]
    public SlashCommandInputCompletion(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="SlashCommandInputCompletion"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Input should complete filesystem directories.</summary>
    public static SlashCommandInputCompletion Directory { get; } = new("directory");

    /// <summary>Returns a value indicating whether two <see cref="SlashCommandInputCompletion"/> instances are equivalent.</summary>
    public static bool operator ==(SlashCommandInputCompletion left, SlashCommandInputCompletion right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="SlashCommandInputCompletion"/> instances are not equivalent.</summary>
    public static bool operator !=(SlashCommandInputCompletion left, SlashCommandInputCompletion right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is SlashCommandInputCompletion other && Equals(other);

    /// <inheritdoc />
    public bool Equals(SlashCommandInputCompletion other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{SlashCommandInputCompletion}"/> for serializing <see cref="SlashCommandInputCompletion"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<SlashCommandInputCompletion>
    {
        /// <inheritdoc />
        public override SlashCommandInputCompletion Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SlashCommandInputCompletion value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SlashCommandInputCompletion));
        }
    }
}


/// <summary>Coarse command category for grouping and behavior: runtime built-in, skill-backed command, or SDK/client-owned command.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct SlashCommandKind : IEquatable<SlashCommandKind>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="SlashCommandKind"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="SlashCommandKind"/>.</param>
    [JsonConstructor]
    public SlashCommandKind(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="SlashCommandKind"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Command implemented by the runtime.</summary>
    public static SlashCommandKind Builtin { get; } = new("builtin");

    /// <summary>Command backed by a skill.</summary>
    public static SlashCommandKind Skill { get; } = new("skill");

    /// <summary>Command registered by an SDK client or extension.</summary>
    public static SlashCommandKind Client { get; } = new("client");

    /// <summary>Returns a value indicating whether two <see cref="SlashCommandKind"/> instances are equivalent.</summary>
    public static bool operator ==(SlashCommandKind left, SlashCommandKind right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="SlashCommandKind"/> instances are not equivalent.</summary>
    public static bool operator !=(SlashCommandKind left, SlashCommandKind right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is SlashCommandKind other && Equals(other);

    /// <inheritdoc />
    public bool Equals(SlashCommandKind other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{SlashCommandKind}"/> for serializing <see cref="SlashCommandKind"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<SlashCommandKind>
    {
        /// <inheritdoc />
        public override SlashCommandKind Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SlashCommandKind value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SlashCommandKind));
        }
    }
}


/// <summary>The user's response: accept (submitted), decline (rejected), or cancel (dismissed).</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>The user submitted the requested form values.</summary>
    public static UIElicitationResponseAction Accept { get; } = new("accept");

    /// <summary>The user explicitly declined to provide the requested input.</summary>
    public static UIElicitationResponseAction Decline { get; } = new("decline");

    /// <summary>The user dismissed the elicitation request.</summary>
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
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, UIElicitationResponseAction value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(UIElicitationResponseAction));
        }
    }
}


/// <summary>User's choice for auto-mode switching: yes (allow this turn), yes_always (allow + persist as setting), or no (decline).</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct UIAutoModeSwitchResponse : IEquatable<UIAutoModeSwitchResponse>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="UIAutoModeSwitchResponse"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="UIAutoModeSwitchResponse"/>.</param>
    [JsonConstructor]
    public UIAutoModeSwitchResponse(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="UIAutoModeSwitchResponse"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Allow the automatic mode switch for this turn.</summary>
    public static UIAutoModeSwitchResponse Yes { get; } = new("yes");

    /// <summary>Allow this mode switch and persist the preference.</summary>
    public static UIAutoModeSwitchResponse YesAlways { get; } = new("yes_always");

    /// <summary>Decline the automatic mode switch.</summary>
    public static UIAutoModeSwitchResponse No { get; } = new("no");

    /// <summary>Returns a value indicating whether two <see cref="UIAutoModeSwitchResponse"/> instances are equivalent.</summary>
    public static bool operator ==(UIAutoModeSwitchResponse left, UIAutoModeSwitchResponse right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="UIAutoModeSwitchResponse"/> instances are not equivalent.</summary>
    public static bool operator !=(UIAutoModeSwitchResponse left, UIAutoModeSwitchResponse right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is UIAutoModeSwitchResponse other && Equals(other);

    /// <inheritdoc />
    public bool Equals(UIAutoModeSwitchResponse other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{UIAutoModeSwitchResponse}"/> for serializing <see cref="UIAutoModeSwitchResponse"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<UIAutoModeSwitchResponse>
    {
        /// <inheritdoc />
        public override UIAutoModeSwitchResponse Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, UIAutoModeSwitchResponse value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(UIAutoModeSwitchResponse));
        }
    }
}


/// <summary>The action the user selected. Defaults to 'autopilot' when autoApproveEdits is true, otherwise 'interactive'.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct UIExitPlanModeAction : IEquatable<UIExitPlanModeAction>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="UIExitPlanModeAction"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="UIExitPlanModeAction"/>.</param>
    [JsonConstructor]
    public UIExitPlanModeAction(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="UIExitPlanModeAction"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Exit plan mode without starting implementation.</summary>
    public static UIExitPlanModeAction ExitOnly { get; } = new("exit_only");

    /// <summary>Exit plan mode and continue interactively.</summary>
    public static UIExitPlanModeAction Interactive { get; } = new("interactive");

    /// <summary>Exit plan mode and continue in autopilot mode.</summary>
    public static UIExitPlanModeAction Autopilot { get; } = new("autopilot");

    /// <summary>Exit plan mode and continue in autopilot mode with parallel subagent execution.</summary>
    public static UIExitPlanModeAction AutopilotFleet { get; } = new("autopilot_fleet");

    /// <summary>Returns a value indicating whether two <see cref="UIExitPlanModeAction"/> instances are equivalent.</summary>
    public static bool operator ==(UIExitPlanModeAction left, UIExitPlanModeAction right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="UIExitPlanModeAction"/> instances are not equivalent.</summary>
    public static bool operator !=(UIExitPlanModeAction left, UIExitPlanModeAction right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is UIExitPlanModeAction other && Equals(other);

    /// <inheritdoc />
    public bool Equals(UIExitPlanModeAction other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{UIExitPlanModeAction}"/> for serializing <see cref="UIExitPlanModeAction"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<UIExitPlanModeAction>
    {
        /// <inheritdoc />
        public override UIExitPlanModeAction Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, UIExitPlanModeAction value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(UIExitPlanModeAction));
        }
    }
}


/// <summary>Allowed values for the `PermissionsConfigureAdditionalContentExclusionPolicyScope` enumeration.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct PermissionsConfigureAdditionalContentExclusionPolicyScope : IEquatable<PermissionsConfigureAdditionalContentExclusionPolicyScope>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="PermissionsConfigureAdditionalContentExclusionPolicyScope"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="PermissionsConfigureAdditionalContentExclusionPolicyScope"/>.</param>
    [JsonConstructor]
    public PermissionsConfigureAdditionalContentExclusionPolicyScope(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="PermissionsConfigureAdditionalContentExclusionPolicyScope"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>The content exclusion policy applies to the current repository.</summary>
    public static PermissionsConfigureAdditionalContentExclusionPolicyScope Repo { get; } = new("repo");

    /// <summary>The content exclusion policy applies across all repositories.</summary>
    public static PermissionsConfigureAdditionalContentExclusionPolicyScope All { get; } = new("all");

    /// <summary>Returns a value indicating whether two <see cref="PermissionsConfigureAdditionalContentExclusionPolicyScope"/> instances are equivalent.</summary>
    public static bool operator ==(PermissionsConfigureAdditionalContentExclusionPolicyScope left, PermissionsConfigureAdditionalContentExclusionPolicyScope right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="PermissionsConfigureAdditionalContentExclusionPolicyScope"/> instances are not equivalent.</summary>
    public static bool operator !=(PermissionsConfigureAdditionalContentExclusionPolicyScope left, PermissionsConfigureAdditionalContentExclusionPolicyScope right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is PermissionsConfigureAdditionalContentExclusionPolicyScope other && Equals(other);

    /// <inheritdoc />
    public bool Equals(PermissionsConfigureAdditionalContentExclusionPolicyScope other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{PermissionsConfigureAdditionalContentExclusionPolicyScope}"/> for serializing <see cref="PermissionsConfigureAdditionalContentExclusionPolicyScope"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<PermissionsConfigureAdditionalContentExclusionPolicyScope>
    {
        /// <inheritdoc />
        public override PermissionsConfigureAdditionalContentExclusionPolicyScope Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, PermissionsConfigureAdditionalContentExclusionPolicyScope value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(PermissionsConfigureAdditionalContentExclusionPolicyScope));
        }
    }
}


/// <summary>Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct PermissionsSetApproveAllSource : IEquatable<PermissionsSetApproveAllSource>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="PermissionsSetApproveAllSource"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="PermissionsSetApproveAllSource"/>.</param>
    [JsonConstructor]
    public PermissionsSetApproveAllSource(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="PermissionsSetApproveAllSource"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Allow-all was enabled from a CLI command-line flag.</summary>
    public static PermissionsSetApproveAllSource CliFlag { get; } = new("cli_flag");

    /// <summary>Allow-all was enabled by a slash command.</summary>
    public static PermissionsSetApproveAllSource SlashCommand { get; } = new("slash_command");

    /// <summary>Allow-all was enabled by confirming autopilot behavior.</summary>
    public static PermissionsSetApproveAllSource AutopilotConfirmation { get; } = new("autopilot_confirmation");

    /// <summary>Allow-all was enabled through an RPC caller.</summary>
    public static PermissionsSetApproveAllSource Rpc { get; } = new("rpc");

    /// <summary>Returns a value indicating whether two <see cref="PermissionsSetApproveAllSource"/> instances are equivalent.</summary>
    public static bool operator ==(PermissionsSetApproveAllSource left, PermissionsSetApproveAllSource right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="PermissionsSetApproveAllSource"/> instances are not equivalent.</summary>
    public static bool operator !=(PermissionsSetApproveAllSource left, PermissionsSetApproveAllSource right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is PermissionsSetApproveAllSource other && Equals(other);

    /// <inheritdoc />
    public bool Equals(PermissionsSetApproveAllSource other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{PermissionsSetApproveAllSource}"/> for serializing <see cref="PermissionsSetApproveAllSource"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<PermissionsSetApproveAllSource>
    {
        /// <inheritdoc />
        public override PermissionsSetApproveAllSource Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, PermissionsSetApproveAllSource value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(PermissionsSetApproveAllSource));
        }
    }
}


/// <summary>Whether the change applies to ephemeral session-scoped rules (cleared at session end) or to location-scoped rules persisted via the location-permissions config file.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct PermissionsModifyRulesScope : IEquatable<PermissionsModifyRulesScope>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="PermissionsModifyRulesScope"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="PermissionsModifyRulesScope"/>.</param>
    [JsonConstructor]
    public PermissionsModifyRulesScope(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="PermissionsModifyRulesScope"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Apply the rule change only to this session.</summary>
    public static PermissionsModifyRulesScope Session { get; } = new("session");

    /// <summary>Persist the rule change for this project location.</summary>
    public static PermissionsModifyRulesScope Location { get; } = new("location");

    /// <summary>Returns a value indicating whether two <see cref="PermissionsModifyRulesScope"/> instances are equivalent.</summary>
    public static bool operator ==(PermissionsModifyRulesScope left, PermissionsModifyRulesScope right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="PermissionsModifyRulesScope"/> instances are not equivalent.</summary>
    public static bool operator !=(PermissionsModifyRulesScope left, PermissionsModifyRulesScope right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is PermissionsModifyRulesScope other && Equals(other);

    /// <inheritdoc />
    public bool Equals(PermissionsModifyRulesScope other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{PermissionsModifyRulesScope}"/> for serializing <see cref="PermissionsModifyRulesScope"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<PermissionsModifyRulesScope>
    {
        /// <inheritdoc />
        public override PermissionsModifyRulesScope Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, PermissionsModifyRulesScope value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(PermissionsModifyRulesScope));
        }
    }
}


/// <summary>Whether the location is a git repo or directory.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct PermissionLocationType : IEquatable<PermissionLocationType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="PermissionLocationType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="PermissionLocationType"/>.</param>
    [JsonConstructor]
    public PermissionLocationType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="PermissionLocationType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>The permission location is persisted at the git repository root.</summary>
    public static PermissionLocationType Repo { get; } = new("repo");

    /// <summary>The permission location is persisted at the working directory.</summary>
    public static PermissionLocationType Dir { get; } = new("dir");

    /// <summary>Returns a value indicating whether two <see cref="PermissionLocationType"/> instances are equivalent.</summary>
    public static bool operator ==(PermissionLocationType left, PermissionLocationType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="PermissionLocationType"/> instances are not equivalent.</summary>
    public static bool operator !=(PermissionLocationType left, PermissionLocationType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is PermissionLocationType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(PermissionLocationType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{PermissionLocationType}"/> for serializing <see cref="PermissionLocationType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<PermissionLocationType>
    {
        /// <inheritdoc />
        public override PermissionLocationType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, PermissionLocationType value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(PermissionLocationType));
        }
    }
}


/// <summary>The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot').</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct MetadataSnapshotCurrentMode : IEquatable<MetadataSnapshotCurrentMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="MetadataSnapshotCurrentMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="MetadataSnapshotCurrentMode"/>.</param>
    [JsonConstructor]
    public MetadataSnapshotCurrentMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="MetadataSnapshotCurrentMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>The agent is responding interactively to the user.</summary>
    public static MetadataSnapshotCurrentMode Interactive { get; } = new("interactive");

    /// <summary>The agent is preparing a plan before making changes.</summary>
    public static MetadataSnapshotCurrentMode Plan { get; } = new("plan");

    /// <summary>The agent is working autonomously toward task completion.</summary>
    public static MetadataSnapshotCurrentMode Autopilot { get; } = new("autopilot");

    /// <summary>Returns a value indicating whether two <see cref="MetadataSnapshotCurrentMode"/> instances are equivalent.</summary>
    public static bool operator ==(MetadataSnapshotCurrentMode left, MetadataSnapshotCurrentMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="MetadataSnapshotCurrentMode"/> instances are not equivalent.</summary>
    public static bool operator !=(MetadataSnapshotCurrentMode left, MetadataSnapshotCurrentMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is MetadataSnapshotCurrentMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(MetadataSnapshotCurrentMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{MetadataSnapshotCurrentMode}"/> for serializing <see cref="MetadataSnapshotCurrentMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<MetadataSnapshotCurrentMode>
    {
        /// <inheritdoc />
        public override MetadataSnapshotCurrentMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, MetadataSnapshotCurrentMode value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(MetadataSnapshotCurrentMode));
        }
    }
}


/// <summary>Whether the remote task originated from Copilot Coding Agent (cca) or a CLI `--remote` invocation.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct MetadataSnapshotRemoteMetadataTaskType : IEquatable<MetadataSnapshotRemoteMetadataTaskType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="MetadataSnapshotRemoteMetadataTaskType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="MetadataSnapshotRemoteMetadataTaskType"/>.</param>
    [JsonConstructor]
    public MetadataSnapshotRemoteMetadataTaskType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="MetadataSnapshotRemoteMetadataTaskType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Remote task originated from Copilot Coding Agent.</summary>
    public static MetadataSnapshotRemoteMetadataTaskType Cca { get; } = new("cca");

    /// <summary>Remote task originated from a CLI remote-session invocation.</summary>
    public static MetadataSnapshotRemoteMetadataTaskType Cli { get; } = new("cli");

    /// <summary>Returns a value indicating whether two <see cref="MetadataSnapshotRemoteMetadataTaskType"/> instances are equivalent.</summary>
    public static bool operator ==(MetadataSnapshotRemoteMetadataTaskType left, MetadataSnapshotRemoteMetadataTaskType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="MetadataSnapshotRemoteMetadataTaskType"/> instances are not equivalent.</summary>
    public static bool operator !=(MetadataSnapshotRemoteMetadataTaskType left, MetadataSnapshotRemoteMetadataTaskType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is MetadataSnapshotRemoteMetadataTaskType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(MetadataSnapshotRemoteMetadataTaskType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{MetadataSnapshotRemoteMetadataTaskType}"/> for serializing <see cref="MetadataSnapshotRemoteMetadataTaskType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<MetadataSnapshotRemoteMetadataTaskType>
    {
        /// <inheritdoc />
        public override MetadataSnapshotRemoteMetadataTaskType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, MetadataSnapshotRemoteMetadataTaskType value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(MetadataSnapshotRemoteMetadataTaskType));
        }
    }
}


/// <summary>Repository host type, if known.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct WorkspaceSummaryHostType : IEquatable<WorkspaceSummaryHostType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="WorkspaceSummaryHostType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="WorkspaceSummaryHostType"/>.</param>
    [JsonConstructor]
    public WorkspaceSummaryHostType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="WorkspaceSummaryHostType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Workspace summary repository is hosted on GitHub.</summary>
    public static WorkspaceSummaryHostType Github { get; } = new("github");

    /// <summary>Workspace summary repository is hosted on Azure DevOps.</summary>
    public static WorkspaceSummaryHostType Ado { get; } = new("ado");

    /// <summary>Returns a value indicating whether two <see cref="WorkspaceSummaryHostType"/> instances are equivalent.</summary>
    public static bool operator ==(WorkspaceSummaryHostType left, WorkspaceSummaryHostType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="WorkspaceSummaryHostType"/> instances are not equivalent.</summary>
    public static bool operator !=(WorkspaceSummaryHostType left, WorkspaceSummaryHostType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is WorkspaceSummaryHostType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(WorkspaceSummaryHostType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{WorkspaceSummaryHostType}"/> for serializing <see cref="WorkspaceSummaryHostType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<WorkspaceSummaryHostType>
    {
        /// <inheritdoc />
        public override WorkspaceSummaryHostType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, WorkspaceSummaryHostType value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(WorkspaceSummaryHostType));
        }
    }
}


/// <summary>Hosting platform type of the repository.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct SessionWorkingDirectoryContextHostType : IEquatable<SessionWorkingDirectoryContextHostType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="SessionWorkingDirectoryContextHostType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="SessionWorkingDirectoryContextHostType"/>.</param>
    [JsonConstructor]
    public SessionWorkingDirectoryContextHostType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="SessionWorkingDirectoryContextHostType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>The working directory repository is hosted on GitHub.</summary>
    public static SessionWorkingDirectoryContextHostType Github { get; } = new("github");

    /// <summary>The working directory repository is hosted on Azure DevOps.</summary>
    public static SessionWorkingDirectoryContextHostType Ado { get; } = new("ado");

    /// <summary>Returns a value indicating whether two <see cref="SessionWorkingDirectoryContextHostType"/> instances are equivalent.</summary>
    public static bool operator ==(SessionWorkingDirectoryContextHostType left, SessionWorkingDirectoryContextHostType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="SessionWorkingDirectoryContextHostType"/> instances are not equivalent.</summary>
    public static bool operator !=(SessionWorkingDirectoryContextHostType left, SessionWorkingDirectoryContextHostType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is SessionWorkingDirectoryContextHostType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(SessionWorkingDirectoryContextHostType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{SessionWorkingDirectoryContextHostType}"/> for serializing <see cref="SessionWorkingDirectoryContextHostType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<SessionWorkingDirectoryContextHostType>
    {
        /// <inheritdoc />
        public override SessionWorkingDirectoryContextHostType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SessionWorkingDirectoryContextHostType value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SessionWorkingDirectoryContextHostType));
        }
    }
}


/// <summary>Signal to send (default: SIGTERM).</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>Request graceful process termination.</summary>
    public static ShellKillSignal SIGTERM { get; } = new("SIGTERM");

    /// <summary>Forcefully terminate the process.</summary>
    public static ShellKillSignal SIGKILL { get; } = new("SIGKILL");

    /// <summary>Send an interrupt signal to the process.</summary>
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
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, ShellKillSignal value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(ShellKillSignal));
        }
    }
}


/// <summary>Whether this item is a queued user message or a queued slash command / model change.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct QueuePendingItemsKind : IEquatable<QueuePendingItemsKind>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="QueuePendingItemsKind"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="QueuePendingItemsKind"/>.</param>
    [JsonConstructor]
    public QueuePendingItemsKind(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="QueuePendingItemsKind"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>A queued user message.</summary>
    public static QueuePendingItemsKind Message { get; } = new("message");

    /// <summary>A queued slash command or model-change command.</summary>
    public static QueuePendingItemsKind Command { get; } = new("command");

    /// <summary>Returns a value indicating whether two <see cref="QueuePendingItemsKind"/> instances are equivalent.</summary>
    public static bool operator ==(QueuePendingItemsKind left, QueuePendingItemsKind right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="QueuePendingItemsKind"/> instances are not equivalent.</summary>
    public static bool operator !=(QueuePendingItemsKind left, QueuePendingItemsKind right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is QueuePendingItemsKind other && Equals(other);

    /// <inheritdoc />
    public bool Equals(QueuePendingItemsKind other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{QueuePendingItemsKind}"/> for serializing <see cref="QueuePendingItemsKind"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<QueuePendingItemsKind>
    {
        /// <inheritdoc />
        public override QueuePendingItemsKind Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, QueuePendingItemsKind value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(QueuePendingItemsKind));
        }
    }
}


/// <summary>Cursor status: 'ok' means the cursor was applied successfully; 'expired' means the cursor referred to an event that no longer exists in history (e.g. truncated or compacted away) and the read started from the beginning of the remaining history.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct EventsCursorStatus : IEquatable<EventsCursorStatus>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="EventsCursorStatus"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="EventsCursorStatus"/>.</param>
    [JsonConstructor]
    public EventsCursorStatus(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="EventsCursorStatus"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>The cursor was applied successfully.</summary>
    public static EventsCursorStatus Ok { get; } = new("ok");

    /// <summary>The cursor referred to history that is no longer available.</summary>
    public static EventsCursorStatus Expired { get; } = new("expired");

    /// <summary>Returns a value indicating whether two <see cref="EventsCursorStatus"/> instances are equivalent.</summary>
    public static bool operator ==(EventsCursorStatus left, EventsCursorStatus right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="EventsCursorStatus"/> instances are not equivalent.</summary>
    public static bool operator !=(EventsCursorStatus left, EventsCursorStatus right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is EventsCursorStatus other && Equals(other);

    /// <inheritdoc />
    public bool Equals(EventsCursorStatus other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{EventsCursorStatus}"/> for serializing <see cref="EventsCursorStatus"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<EventsCursorStatus>
    {
        /// <inheritdoc />
        public override EventsCursorStatus Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, EventsCursorStatus value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(EventsCursorStatus));
        }
    }
}


/// <summary>Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct EventsAgentScope : IEquatable<EventsAgentScope>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="EventsAgentScope"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="EventsAgentScope"/>.</param>
    [JsonConstructor]
    public EventsAgentScope(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="EventsAgentScope"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Return main-agent events and typed subagent lifecycle events.</summary>
    public static EventsAgentScope Primary { get; } = new("primary");

    /// <summary>Return events from all agents.</summary>
    public static EventsAgentScope All { get; } = new("all");

    /// <summary>Returns a value indicating whether two <see cref="EventsAgentScope"/> instances are equivalent.</summary>
    public static bool operator ==(EventsAgentScope left, EventsAgentScope right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="EventsAgentScope"/> instances are not equivalent.</summary>
    public static bool operator !=(EventsAgentScope left, EventsAgentScope right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is EventsAgentScope other && Equals(other);

    /// <inheritdoc />
    public bool Equals(EventsAgentScope other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{EventsAgentScope}"/> for serializing <see cref="EventsAgentScope"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<EventsAgentScope>
    {
        /// <inheritdoc />
        public override EventsAgentScope Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, EventsAgentScope value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(EventsAgentScope));
        }
    }
}


/// <summary>Per-session remote mode. "off" disables remote, "export" exports session events to GitHub without enabling remote steering, "on" enables both export and remote steering.</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct RemoteSessionMode : IEquatable<RemoteSessionMode>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="RemoteSessionMode"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="RemoteSessionMode"/>.</param>
    [JsonConstructor]
    public RemoteSessionMode(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="RemoteSessionMode"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Disable remote session export and steering.</summary>
    public static RemoteSessionMode Off { get; } = new("off");

    /// <summary>Export session events to GitHub without enabling remote steering.</summary>
    public static RemoteSessionMode Export { get; } = new("export");

    /// <summary>Enable both remote session export and remote steering.</summary>
    public static RemoteSessionMode On { get; } = new("on");

    /// <summary>Returns a value indicating whether two <see cref="RemoteSessionMode"/> instances are equivalent.</summary>
    public static bool operator ==(RemoteSessionMode left, RemoteSessionMode right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="RemoteSessionMode"/> instances are not equivalent.</summary>
    public static bool operator !=(RemoteSessionMode left, RemoteSessionMode right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is RemoteSessionMode other && Equals(other);

    /// <inheritdoc />
    public bool Equals(RemoteSessionMode other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{RemoteSessionMode}"/> for serializing <see cref="RemoteSessionMode"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<RemoteSessionMode>
    {
        /// <inheritdoc />
        public override RemoteSessionMode Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, RemoteSessionMode value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(RemoteSessionMode));
        }
    }
}


/// <summary>Error classification.</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>The requested path does not exist.</summary>
    public static SessionFsErrorCode ENOENT { get; } = new("ENOENT");

    /// <summary>The filesystem operation failed for an unspecified reason.</summary>
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
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SessionFsErrorCode value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SessionFsErrorCode));
        }
    }
}


/// <summary>Entry type.</summary>
[Experimental(Diagnostics.Experimental)]
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

    /// <summary>The entry is a file.</summary>
    public static SessionFsReaddirWithTypesEntryType File { get; } = new("file");

    /// <summary>The entry is a directory.</summary>
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
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SessionFsReaddirWithTypesEntryType value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SessionFsReaddirWithTypesEntryType));
        }
    }
}


/// <summary>How to execute the query: 'exec' for DDL/multi-statement (no results), 'query' for SELECT (returns rows), 'run' for INSERT/UPDATE/DELETE (returns rowsAffected).</summary>
[Experimental(Diagnostics.Experimental)]
[JsonConverter(typeof(Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct SessionFsSqliteQueryType : IEquatable<SessionFsSqliteQueryType>
{
    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="SessionFsSqliteQueryType"/> struct.</summary>
    /// <param name="value">The value to associate with this <see cref="SessionFsSqliteQueryType"/>.</param>
    [JsonConstructor]
    public SessionFsSqliteQueryType(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value);
        _value = value;
    }

    /// <summary>Gets the value associated with this <see cref="SessionFsSqliteQueryType"/>.</summary>
    public string Value => _value ?? string.Empty;

    /// <summary>Execute DDL or multi-statement SQL without returning rows.</summary>
    public static SessionFsSqliteQueryType Exec { get; } = new("exec");

    /// <summary>Execute a SELECT-style query and return rows.</summary>
    public static SessionFsSqliteQueryType Query { get; } = new("query");

    /// <summary>Execute INSERT, UPDATE, or DELETE SQL and return affected-row metadata.</summary>
    public static SessionFsSqliteQueryType Run { get; } = new("run");

    /// <summary>Returns a value indicating whether two <see cref="SessionFsSqliteQueryType"/> instances are equivalent.</summary>
    public static bool operator ==(SessionFsSqliteQueryType left, SessionFsSqliteQueryType right) => left.Equals(right);

    /// <summary>Returns a value indicating whether two <see cref="SessionFsSqliteQueryType"/> instances are not equivalent.</summary>
    public static bool operator !=(SessionFsSqliteQueryType left, SessionFsSqliteQueryType right) => !(left == right);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is SessionFsSqliteQueryType other && Equals(other);

    /// <inheritdoc />
    public bool Equals(SessionFsSqliteQueryType other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc />
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc />
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{SessionFsSqliteQueryType}"/> for serializing <see cref="SessionFsSqliteQueryType"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<SessionFsSqliteQueryType>
    {
        /// <inheritdoc />
        public override SessionFsSqliteQueryType Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            return new(GeneratedStringEnumJson.ReadValue(ref reader, typeToConvert));
        }

        /// <inheritdoc />
        public override void Write(Utf8JsonWriter writer, SessionFsSqliteQueryType value, JsonSerializerOptions options)
        {
            GeneratedStringEnumJson.WriteValue(writer, value.Value, typeof(SessionFsSqliteQueryType));
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
    }

    /// <summary>Checks server responsiveness and returns protocol information.</summary>
    /// <param name="message">Optional message to echo back.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Server liveness response, including the echoed message, current server timestamp, and protocol version.</returns>
    public async Task<PingResult> PingAsync(string? message = null, CancellationToken cancellationToken = default)
    {
        var request = new PingRequest { Message = message };
        return await CopilotClient.InvokeRpcAsync<PingResult>(_rpc, "ping", [request], cancellationToken);
    }

    /// <summary>Performs the SDK server connection handshake and validates the optional connection token.</summary>
    /// <param name="token">Connection token; required when the server was started with COPILOT_CONNECTION_TOKEN.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Handshake result reporting the server's protocol version and package version on success.</returns>
    internal async Task<ConnectResult> ConnectAsync(string? token = null, CancellationToken cancellationToken = default)
    {
        var request = new ConnectRequest { Token = token };
        return await CopilotClient.InvokeRpcAsync<ConnectResult>(_rpc, "connect", [request], cancellationToken);
    }

    /// <summary>Models APIs.</summary>
    public ServerModelsApi Models =>
        field ??
        Interlocked.CompareExchange(ref field, new(_rpc), null) ??
        field;

    /// <summary>Tools APIs.</summary>
    public ServerToolsApi Tools =>
        field ??
        Interlocked.CompareExchange(ref field, new(_rpc), null) ??
        field;

    /// <summary>Account APIs.</summary>
    public ServerAccountApi Account =>
        field ??
        Interlocked.CompareExchange(ref field, new(_rpc), null) ??
        field;

    /// <summary>Secrets APIs.</summary>
    public ServerSecretsApi Secrets =>
        field ??
        Interlocked.CompareExchange(ref field, new(_rpc), null) ??
        field;

    /// <summary>Mcp APIs.</summary>
    public ServerMcpApi Mcp =>
        field ??
        Interlocked.CompareExchange(ref field, new(_rpc), null) ??
        field;

    /// <summary>Skills APIs.</summary>
    public ServerSkillsApi Skills =>
        field ??
        Interlocked.CompareExchange(ref field, new(_rpc), null) ??
        field;

    /// <summary>SessionFs APIs.</summary>
    public ServerSessionFsApi SessionFs =>
        field ??
        Interlocked.CompareExchange(ref field, new(_rpc), null) ??
        field;

    /// <summary>Sessions APIs.</summary>
    public ServerSessionsApi Sessions =>
        field ??
        Interlocked.CompareExchange(ref field, new(_rpc), null) ??
        field;
}

/// <summary>Provides server-scoped Models APIs.</summary>
public sealed class ServerModelsApi
{
    private readonly JsonRpc _rpc;

    internal ServerModelsApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Lists Copilot models available to the authenticated user.</summary>
    /// <param name="gitHubToken">GitHub token for per-user model listing. When provided, resolves this token to determine the user's Copilot plan and available models instead of using the global auth.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>List of Copilot models available to the resolved user, including capabilities and billing metadata.</returns>
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

    /// <summary>Lists built-in tools available for a model.</summary>
    /// <param name="model">Optional model ID — when provided, the returned tool list reflects model-specific overrides.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Built-in tools available for the requested model, with their parameters and instructions.</returns>
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

    /// <summary>Gets Copilot quota usage for the authenticated user or supplied GitHub token.</summary>
    /// <param name="gitHubToken">GitHub token for per-user quota lookup. When provided, resolves this token to determine the user's quota instead of using the global auth.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Quota usage snapshots for the resolved user, keyed by quota type.</returns>
    public async Task<AccountGetQuotaResult> GetQuotaAsync(string? gitHubToken = null, CancellationToken cancellationToken = default)
    {
        var request = new AccountGetQuotaRequest { GitHubToken = gitHubToken };
        return await CopilotClient.InvokeRpcAsync<AccountGetQuotaResult>(_rpc, "account.getQuota", [request], cancellationToken);
    }
}

/// <summary>Provides server-scoped Secrets APIs.</summary>
public sealed class ServerSecretsApi
{
    private readonly JsonRpc _rpc;

    internal ServerSecretsApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Registers secret values for redaction in session logs and exports. The SDK calls this to inject dynamically generated secret values (e.g., OIDC tokens).</summary>
    /// <param name="values">Raw secret values to register for redaction.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Confirmation that the secret values were registered.</returns>
    public async Task<SecretsAddFilterValuesResult> AddFilterValuesAsync(IList<string> values, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(values);

        var request = new SecretsAddFilterValuesRequest { Values = values };
        return await CopilotClient.InvokeRpcAsync<SecretsAddFilterValuesResult>(_rpc, "secrets.addFilterValues", [request], cancellationToken);
    }
}

/// <summary>Provides server-scoped Mcp APIs.</summary>
public sealed class ServerMcpApi
{
    private readonly JsonRpc _rpc;

    internal ServerMcpApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Discovers MCP servers from user, workspace, plugin, and builtin sources.</summary>
    /// <param name="workingDirectory">Working directory used as context for discovery (e.g., plugin resolution).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>MCP servers discovered from user, workspace, plugin, and built-in sources.</returns>
    public async Task<McpDiscoverResult> DiscoverAsync(string? workingDirectory = null, CancellationToken cancellationToken = default)
    {
        var request = new McpDiscoverRequest { WorkingDirectory = workingDirectory };
        return await CopilotClient.InvokeRpcAsync<McpDiscoverResult>(_rpc, "mcp.discover", [request], cancellationToken);
    }

    /// <summary>Config APIs.</summary>
    public ServerMcpConfigApi Config =>
        field ??
        Interlocked.CompareExchange(ref field, new(_rpc), null) ??
        field;
}

/// <summary>Provides server-scoped McpConfig APIs.</summary>
public sealed class ServerMcpConfigApi
{
    private readonly JsonRpc _rpc;

    internal ServerMcpConfigApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Lists MCP servers from user configuration.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>User-configured MCP servers, keyed by server name.</returns>
    public async Task<McpConfigList> ListAsync(CancellationToken cancellationToken = default)
    {
        return await CopilotClient.InvokeRpcAsync<McpConfigList>(_rpc, "mcp.config.list", [], cancellationToken);
    }

    /// <summary>Adds an MCP server to user configuration.</summary>
    /// <param name="name">Unique name for the MCP server.</param>
    /// <param name="config">MCP server configuration (stdio process or remote HTTP/SSE).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task AddAsync(string name, object config, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(name);
        ArgumentNullException.ThrowIfNull(config);

        var request = new McpConfigAddRequest { Name = name, Config = CopilotClient.ToJsonElementForWire(config)!.Value };
        await CopilotClient.InvokeRpcAsync(_rpc, "mcp.config.add", [request], cancellationToken);
    }

    /// <summary>Updates an MCP server in user configuration.</summary>
    /// <param name="name">Name of the MCP server to update.</param>
    /// <param name="config">MCP server configuration (stdio process or remote HTTP/SSE).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task UpdateAsync(string name, object config, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(name);
        ArgumentNullException.ThrowIfNull(config);

        var request = new McpConfigUpdateRequest { Name = name, Config = CopilotClient.ToJsonElementForWire(config)!.Value };
        await CopilotClient.InvokeRpcAsync(_rpc, "mcp.config.update", [request], cancellationToken);
    }

    /// <summary>Removes an MCP server from user configuration.</summary>
    /// <param name="name">Name of the MCP server to remove.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task RemoveAsync(string name, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(name);

        var request = new McpConfigRemoveRequest { Name = name };
        await CopilotClient.InvokeRpcAsync(_rpc, "mcp.config.remove", [request], cancellationToken);
    }

    /// <summary>Enables MCP servers in user configuration for new sessions.</summary>
    /// <param name="names">Names of MCP servers to enable. Each server is removed from the persisted disabled list so new sessions spawn it. Unknown or already-enabled names are ignored.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task EnableAsync(IList<string> names, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(names);

        var request = new McpConfigEnableRequest { Names = names };
        await CopilotClient.InvokeRpcAsync(_rpc, "mcp.config.enable", [request], cancellationToken);
    }

    /// <summary>Disables MCP servers in user configuration for new sessions.</summary>
    /// <param name="names">Names of MCP servers to disable. Each server is added to the persisted disabled list so new sessions skip it. Already-disabled names are ignored. Active sessions keep their current connections until they end.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task DisableAsync(IList<string> names, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(names);

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
    }

    /// <summary>Discovers skills across global and project sources.</summary>
    /// <param name="projectPaths">Optional list of project directory paths to scan for project-scoped skills.</param>
    /// <param name="skillDirectories">Optional list of additional skill directory paths to include.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Skills discovered across global and project sources.</returns>
    public async Task<ServerSkillList> DiscoverAsync(IList<string>? projectPaths = null, IList<string>? skillDirectories = null, CancellationToken cancellationToken = default)
    {
        var request = new SkillsDiscoverRequest { ProjectPaths = projectPaths, SkillDirectories = skillDirectories };
        return await CopilotClient.InvokeRpcAsync<ServerSkillList>(_rpc, "skills.discover", [request], cancellationToken);
    }

    /// <summary>Config APIs.</summary>
    public ServerSkillsConfigApi Config =>
        field ??
        Interlocked.CompareExchange(ref field, new(_rpc), null) ??
        field;
}

/// <summary>Provides server-scoped SkillsConfig APIs.</summary>
public sealed class ServerSkillsConfigApi
{
    private readonly JsonRpc _rpc;

    internal ServerSkillsConfigApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Replaces the global list of disabled skills.</summary>
    /// <param name="disabledSkills">List of skill names to disable.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task SetDisabledSkillsAsync(IList<string> disabledSkills, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(disabledSkills);

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

    /// <summary>Registers an SDK client as the session filesystem provider.</summary>
    /// <param name="initialCwd">Initial working directory for sessions.</param>
    /// <param name="sessionStatePath">Path within each session's SessionFs where the runtime stores files for that session.</param>
    /// <param name="conventions">Path conventions used by this filesystem.</param>
    /// <param name="capabilities">Optional capabilities declared by the provider.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the calling client was registered as the session filesystem provider.</returns>
    public async Task<SessionFsSetProviderResult> SetProviderAsync(string initialCwd, string sessionStatePath, SessionFsSetProviderConventions conventions, SessionFsSetProviderCapabilities? capabilities = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(initialCwd);
        ArgumentNullException.ThrowIfNull(sessionStatePath);

        var request = new SessionFsSetProviderRequest { InitialCwd = initialCwd, SessionStatePath = sessionStatePath, Conventions = conventions, Capabilities = capabilities };
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

    /// <summary>Creates a new session by forking persisted history from an existing session.</summary>
    /// <param name="sessionId">Source session ID to fork from.</param>
    /// <param name="toEventId">Optional event ID boundary. When provided, the fork includes only events before this ID (exclusive). When omitted, all events are included.</param>
    /// <param name="name">Optional friendly name to assign to the forked session.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Identifier and optional friendly name assigned to the newly forked session.</returns>
    public async Task<SessionsForkResult> ForkAsync(string sessionId, string? toEventId = null, string? name = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionId);

        var request = new SessionsForkRequest { SessionId = sessionId, ToEventId = toEventId, Name = name };
        return await CopilotClient.InvokeRpcAsync<SessionsForkResult>(_rpc, "sessions.fork", [request], cancellationToken);
    }

    /// <summary>Connects to an existing remote session and exposes it as an SDK session.</summary>
    /// <param name="sessionId">Session ID to connect to.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Remote session connection result.</returns>
    public async Task<RemoteSessionConnectionResult> ConnectAsync(string sessionId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionId);

        var request = new ConnectRemoteSessionParams { SessionId = sessionId };
        return await CopilotClient.InvokeRpcAsync<RemoteSessionConnectionResult>(_rpc, "sessions.connect", [request], cancellationToken);
    }

    /// <summary>Lists persisted sessions, optionally filtered by working-directory context.</summary>
    /// <param name="metadataLimit">When provided, only the first N sessions (sorted by modification time, newest first) load full metadata; remaining sessions return basic info only. Use 0 to return only basic info for every session.</param>
    /// <param name="filter">Optional filter applied to the returned sessions.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Persisted sessions matching the filter, ordered most-recently-modified first.</returns>
    public async Task<SessionList> ListAsync(long? metadataLimit = null, SessionListFilter? filter = null, CancellationToken cancellationToken = default)
    {
        var request = new SessionsListRequest { MetadataLimit = metadataLimit, Filter = filter };
        return await CopilotClient.InvokeRpcAsync<SessionList>(_rpc, "sessions.list", [request], cancellationToken);
    }

    /// <summary>Finds the local session bound to a GitHub task ID, if any.</summary>
    /// <param name="taskId">GitHub task ID to look up.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>ID of the local session bound to the given GitHub task, or omitted when none.</returns>
    public async Task<SessionsFindByTaskIDResult> FindByTaskIdAsync(string taskId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(taskId);

        var request = new SessionsFindByTaskIDRequest { TaskId = taskId };
        return await CopilotClient.InvokeRpcAsync<SessionsFindByTaskIDResult>(_rpc, "sessions.findByTaskId", [request], cancellationToken);
    }

    /// <summary>Resolves a UUID prefix to a unique session ID, if exactly one session matches.</summary>
    /// <param name="prefix">UUID prefix (&gt;=7 hex chars, &lt;36 chars). Returns the unique session ID, or undefined when there is no match or the prefix matches multiple sessions.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Session ID matching the prefix, omitted when no unique match exists.</returns>
    public async Task<SessionsFindByPrefixResult> FindByPrefixAsync(string prefix, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(prefix);

        var request = new SessionsFindByPrefixRequest { Prefix = prefix };
        return await CopilotClient.InvokeRpcAsync<SessionsFindByPrefixResult>(_rpc, "sessions.findByPrefix", [request], cancellationToken);
    }

    /// <summary>Returns the most-relevant prior session for a given working-directory context.</summary>
    /// <param name="context">Optional working-directory context used to score session relevance. When omitted the most-recently-modified session wins.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Most-relevant session ID for the supplied context, or omitted when no sessions exist.</returns>
    public async Task<SessionsGetLastForContextResult> GetLastForContextAsync(SessionContext? context = null, CancellationToken cancellationToken = default)
    {
        var request = new SessionsGetLastForContextRequest { Context = context };
        return await CopilotClient.InvokeRpcAsync<SessionsGetLastForContextResult>(_rpc, "sessions.getLastForContext", [request], cancellationToken);
    }

    /// <summary>Computes the absolute path to a session's persisted events.jsonl file.</summary>
    /// <param name="sessionId">Session ID whose event-log file path to compute.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Absolute path to the session's events.jsonl file on disk.</returns>
    public async Task<SessionsGetEventFilePathResult> GetEventFilePathAsync(string sessionId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionId);

        var request = new SessionsGetEventFilePathRequest { SessionId = sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionsGetEventFilePathResult>(_rpc, "sessions.getEventFilePath", [request], cancellationToken);
    }

    /// <summary>Returns the on-disk byte size of each session's workspace directory.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Map of sessionId -&gt; on-disk size in bytes for each session's workspace directory.</returns>
    public async Task<SessionSizes> GetSizesAsync(CancellationToken cancellationToken = default)
    {
        return await CopilotClient.InvokeRpcAsync<SessionSizes>(_rpc, "sessions.getSizes", [], cancellationToken);
    }

    /// <summary>Returns the subset of the supplied session IDs that are currently held by another running process.</summary>
    /// <param name="sessionIds">Session IDs to test for live in-use locks.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Session IDs from the input set that are currently in use by another process.</returns>
    public async Task<SessionsCheckInUseResult> CheckInUseAsync(IList<string> sessionIds, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionIds);

        var request = new SessionsCheckInUseRequest { SessionIds = sessionIds };
        return await CopilotClient.InvokeRpcAsync<SessionsCheckInUseResult>(_rpc, "sessions.checkInUse", [request], cancellationToken);
    }

    /// <summary>Returns a session's persisted remote-steerable flag, if any has been recorded.</summary>
    /// <param name="sessionId">Session ID to look up the persisted remote-steerable flag for.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>The session's persisted remote-steerable flag, or omitted when no value has been persisted.</returns>
    public async Task<SessionsGetPersistedRemoteSteerableResult> GetPersistedRemoteSteerableAsync(string sessionId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionId);

        var request = new SessionsGetPersistedRemoteSteerableRequest { SessionId = sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionsGetPersistedRemoteSteerableResult>(_rpc, "sessions.getPersistedRemoteSteerable", [request], cancellationToken);
    }

    /// <summary>Closes a session: emits shutdown, flushes pending events, releases the in-use lock, and disposes the active session.</summary>
    /// <param name="sessionId">Session ID to close.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Closes a session: emits shutdown, flushes pending events to disk, releases the in-use lock, disposes the active session. Idempotent: succeeds even if the session is not currently active.</returns>
    public async Task<SessionsCloseResult> CloseAsync(string sessionId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionId);

        var request = new SessionsCloseRequest { SessionId = sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionsCloseResult>(_rpc, "sessions.close", [request], cancellationToken);
    }

    /// <summary>Closes, deactivates, and deletes a set of sessions, returning the bytes freed per session.</summary>
    /// <param name="sessionIds">Session IDs to close, deactivate, and delete from disk.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Map of sessionId -&gt; bytes freed by removing the session's workspace directory.</returns>
    public async Task<SessionBulkDeleteResult> BulkDeleteAsync(IList<string> sessionIds, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionIds);

        var request = new SessionsBulkDeleteRequest { SessionIds = sessionIds };
        return await CopilotClient.InvokeRpcAsync<SessionBulkDeleteResult>(_rpc, "sessions.bulkDelete", [request], cancellationToken);
    }

    /// <summary>Deletes sessions older than the given threshold, with optional dry-run and exclusion list.</summary>
    /// <param name="olderThanDays">Delete sessions whose modifiedTime is at least this many days old.</param>
    /// <param name="dryRun">When true, only report what would be deleted without performing any deletion.</param>
    /// <param name="includeNamed">When true, named sessions (set via /rename) are also eligible for pruning.</param>
    /// <param name="excludeSessionIds">Session IDs that should never be considered for pruning.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag.</returns>
    public async Task<SessionPruneResult> PruneOldAsync(long olderThanDays, bool? dryRun = null, bool? includeNamed = null, IList<string>? excludeSessionIds = null, CancellationToken cancellationToken = default)
    {
        var request = new SessionsPruneOldRequest { OlderThanDays = olderThanDays, DryRun = dryRun, IncludeNamed = includeNamed, ExcludeSessionIds = excludeSessionIds };
        return await CopilotClient.InvokeRpcAsync<SessionPruneResult>(_rpc, "sessions.pruneOld", [request], cancellationToken);
    }

    /// <summary>Flushes a session's pending events to disk.</summary>
    /// <param name="sessionId">Session ID whose pending events should be flushed to disk.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Flush a session's pending events to disk. No-op when no writer exists for the session (e.g., already closed).</returns>
    public async Task<SessionsSaveResult> SaveAsync(string sessionId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionId);

        var request = new SessionsSaveRequest { SessionId = sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionsSaveResult>(_rpc, "sessions.save", [request], cancellationToken);
    }

    /// <summary>Releases the in-use lock held by this process for a session.</summary>
    /// <param name="sessionId">Session ID whose in-use lock should be released.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Release the in-use lock held by this process for the given session. No-op when this process does not currently hold a lock for the session.</returns>
    public async Task<SessionsReleaseLockResult> ReleaseLockAsync(string sessionId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionId);

        var request = new SessionsReleaseLockRequest { SessionId = sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionsReleaseLockResult>(_rpc, "sessions.releaseLock", [request], cancellationToken);
    }

    /// <summary>Backfills missing summary and context fields on the supplied session metadata records.</summary>
    /// <param name="sessions">Session metadata records to enrich. Records that already have summary and context are returned unchanged.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>The enriched metadata records, with summary and context fields backfilled where available. Sessions confirmed empty and unnamed are omitted.</returns>
    public async Task<SessionEnrichMetadataResult> EnrichMetadataAsync(IList<SessionMetadata> sessions, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessions);

        var request = new SessionsEnrichMetadataRequest { Sessions = sessions };
        return await CopilotClient.InvokeRpcAsync<SessionEnrichMetadataResult>(_rpc, "sessions.enrichMetadata", [request], cancellationToken);
    }

    /// <summary>Reloads user, plugin, and (optionally) repo hooks on the active session.</summary>
    /// <param name="sessionId">Active session ID to reload hooks for.</param>
    /// <param name="deferRepoHooks">When true, skip repo-level hooks. Use before folder trust is confirmed; loadDeferredRepoHooks loads them post-trust.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Reload all hooks (user, plugin, optionally repo) and apply them to the active session. Call after installing or removing plugins so their hooks take effect immediately. No-op when no active session matches the given sessionId.</returns>
    public async Task<SessionsReloadPluginHooksResult> ReloadPluginHooksAsync(string sessionId, bool? deferRepoHooks = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionId);

        var request = new SessionsReloadPluginHooksRequest { SessionId = sessionId, DeferRepoHooks = deferRepoHooks };
        return await CopilotClient.InvokeRpcAsync<SessionsReloadPluginHooksResult>(_rpc, "sessions.reloadPluginHooks", [request], cancellationToken);
    }

    /// <summary>Loads previously-deferred repo-level hooks on the active session, returning queued startup prompts.</summary>
    /// <param name="sessionId">Active session ID whose deferred repo-level hooks should be loaded.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Queued repo-level startup prompts and the total hook command count after loading.</returns>
    public async Task<SessionLoadDeferredRepoHooksResult> LoadDeferredRepoHooksAsync(string sessionId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(sessionId);

        var request = new SessionsLoadDeferredRepoHooksRequest { SessionId = sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionLoadDeferredRepoHooksResult>(_rpc, "sessions.loadDeferredRepoHooks", [request], cancellationToken);
    }

    /// <summary>Replaces the manager-wide additional plugins registered with the session manager.</summary>
    /// <param name="plugins">Manager-wide additional plugins to register. Replaces any previously-configured set. Pass an empty array to clear.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Replace the manager-wide additional plugins. New session creations and subsequent hook reloads see the new set; already-running sessions keep their existing hook installation until the next reload.</returns>
    public async Task<SessionsSetAdditionalPluginsResult> SetAdditionalPluginsAsync(IList<InstalledPlugin> plugins, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(plugins);

        var request = new SessionsSetAdditionalPluginsRequest { Plugins = plugins };
        return await CopilotClient.InvokeRpcAsync<SessionsSetAdditionalPluginsResult>(_rpc, "sessions.setAdditionalPlugins", [request], cancellationToken);
    }
}

/// <summary>Provides typed session-scoped RPC methods.</summary>
public sealed class SessionRpc
{
    private readonly CopilotSession _session;

    internal SessionRpc(CopilotSession session)
    {
        _session = session;
    }

    internal CopilotSession Session => _session;

    /// <summary>Auth APIs.</summary>
    public AuthApi Auth =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Canvas APIs.</summary>
    public CanvasApi Canvas =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Model APIs.</summary>
    public ModelApi Model =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Mode APIs.</summary>
    public ModeApi Mode =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Name APIs.</summary>
    public NameApi Name =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Plan APIs.</summary>
    public PlanApi Plan =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Workspaces APIs.</summary>
    public WorkspacesApi Workspaces =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Instructions APIs.</summary>
    public InstructionsApi Instructions =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Fleet APIs.</summary>
    public FleetApi Fleet =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Agent APIs.</summary>
    public AgentApi Agent =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Tasks APIs.</summary>
    public TasksApi Tasks =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Skills APIs.</summary>
    public SkillsApi Skills =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Mcp APIs.</summary>
    public McpApi Mcp =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Plugins APIs.</summary>
    public PluginsApi Plugins =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Options APIs.</summary>
    public OptionsApi Options =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Lsp APIs.</summary>
    public LspApi Lsp =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Extensions APIs.</summary>
    public ExtensionsApi Extensions =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Tools APIs.</summary>
    public ToolsApi Tools =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Commands APIs.</summary>
    public CommandsApi Commands =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Telemetry APIs.</summary>
    public TelemetryApi Telemetry =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Ui APIs.</summary>
    public UiApi Ui =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Permissions APIs.</summary>
    public PermissionsApi Permissions =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Metadata APIs.</summary>
    public MetadataApi Metadata =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Shell APIs.</summary>
    public ShellApi Shell =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>History APIs.</summary>
    public HistoryApi History =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Queue APIs.</summary>
    public QueueApi Queue =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>EventLog APIs.</summary>
    public EventLogApi EventLog =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Usage APIs.</summary>
    public UsageApi Usage =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Remote APIs.</summary>
    public RemoteApi Remote =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Schedule APIs.</summary>
    public ScheduleApi Schedule =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Suspends the session while preserving persisted state for later resume.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    [Experimental(Diagnostics.Experimental)]
    public async Task SuspendAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionSuspendRequest { SessionId = _session.SessionId };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.suspend", [request], cancellationToken);
    }

    /// <summary>Sends a user message to the session and returns its message ID.</summary>
    /// <param name="prompt">The user message text.</param>
    /// <param name="displayPrompt">If provided, this is shown in the timeline instead of `prompt`.</param>
    /// <param name="attachments">Optional attachments (files, directories, selections, blobs, GitHub references) to include with the message.</param>
    /// <param name="mode">How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.</param>
    /// <param name="prepend">If true, adds the message to the front of the queue instead of the end.</param>
    /// <param name="billable">If false, this message will not trigger a Premium Request Unit charge. User messages default to billable.</param>
    /// <param name="requiredTool">If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange.</param>
    /// <param name="source">Optional provenance tag copied to the resulting user.message event. Supported values are `system`, `command-*`, and `schedule-*`.</param>
    /// <param name="agentMode">The UI mode the agent was in when this message was sent. Defaults to the session's current mode.</param>
    /// <param name="requestHeaders">Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key.</param>
    /// <param name="traceparent">W3C Trace Context traceparent header for distributed tracing of this agent turn.</param>
    /// <param name="tracestate">W3C Trace Context tracestate header for distributed tracing.</param>
    /// <param name="wait">If true, await completion of the agentic loop for this message before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageId`; the caller can rely on the agent having processed the message before the call resolves.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Result of sending a user message.</returns>
    [Experimental(Diagnostics.Experimental)]
    public async Task<SendResult> SendAsync(string prompt, string? displayPrompt = null, IList<SendAttachment>? attachments = null, SendMode? mode = null, bool? prepend = null, bool? billable = null, string? requiredTool = null, object? source = null, SendAgentMode? agentMode = null, IDictionary<string, string>? requestHeaders = null, string? traceparent = null, string? tracestate = null, bool? wait = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(prompt);
        _session.ThrowIfDisposed();

        var request = new SendRequest { SessionId = _session.SessionId, Prompt = prompt, DisplayPrompt = displayPrompt, Attachments = attachments, Mode = mode, Prepend = prepend, Billable = billable, RequiredTool = requiredTool, Source = CopilotClient.ToJsonElementForWire(source), AgentMode = agentMode, RequestHeaders = requestHeaders, Traceparent = traceparent, Tracestate = tracestate, Wait = wait };
        return await CopilotClient.InvokeRpcAsync<SendResult>(_session.Rpc, "session.send", [request], cancellationToken);
    }

    /// <summary>Aborts the current agent turn.</summary>
    /// <param name="reason">Finite reason code describing why the current turn was aborted.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Result of aborting the current turn.</returns>
    [Experimental(Diagnostics.Experimental)]
    public async Task<AbortResult> AbortAsync(AbortReason? reason = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new AbortRequest { SessionId = _session.SessionId, Reason = reason };
        return await CopilotClient.InvokeRpcAsync<AbortResult>(_session.Rpc, "session.abort", [request], cancellationToken);
    }

    /// <summary>Shuts down the session and persists its final state. Awaits any deferred sessionEnd hooks before resolving so user-supplied hook scripts complete before the runtime tears down.</summary>
    /// <param name="type">Why the session is being shut down. Defaults to "routine" when omitted.</param>
    /// <param name="reason">Optional human-readable reason. Typically the message of the error that triggered shutdown when type is 'error'.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    [Experimental(Diagnostics.Experimental)]
    public async Task ShutdownAsync(ShutdownType? type = null, string? reason = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new ShutdownRequest { SessionId = _session.SessionId, Type = type, Reason = reason };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.shutdown", [request], cancellationToken);
    }

    /// <summary>Emits a user-visible session log event.</summary>
    /// <param name="message">Human-readable message.</param>
    /// <param name="level">Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".</param>
    /// <param name="type">Domain category for this log entry (e.g., "mcp", "subscription", "policy", "model"). Maps to `infoType`/`warningType`/`errorType` on the emitted event. Defaults to "notification".</param>
    /// <param name="ephemeral">When true, the message is transient and not persisted to the session event log on disk.</param>
    /// <param name="url">Optional URL the user can open in their browser for more details.</param>
    /// <param name="tip">Optional actionable tip displayed alongside the message. Only honored on `level: "info"`.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Identifier of the session event that was emitted for the log message.</returns>
    [Experimental(Diagnostics.Experimental)]
    public async Task<LogResult> LogAsync(string message, SessionLogLevel? level = null, string? type = null, bool? ephemeral = null, string? url = null, string? tip = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(message);
        _session.ThrowIfDisposed();

        var request = new LogRequest { SessionId = _session.SessionId, Message = message, Level = level, Type = type, Ephemeral = ephemeral, Url = url, Tip = tip };
        return await CopilotClient.InvokeRpcAsync<LogResult>(_session.Rpc, "session.log", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Auth APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AuthApi
{
    private readonly CopilotSession _session;

    internal AuthApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Gets authentication status and account metadata for the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Authentication status and account metadata for the session.</returns>
    public async Task<SessionAuthStatus> GetStatusAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionAuthGetStatusRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<SessionAuthStatus>(_session.Rpc, "session.auth.getStatus", [request], cancellationToken);
    }

    /// <summary>Updates the session's auth credentials used for outbound model and API requests.</summary>
    /// <param name="credentials">The new auth credentials to install on the session. When omitted or `undefined`, the call is a no-op and the session's existing credentials are preserved. The runtime stores the value verbatim and uses it for outbound model/API requests; it does NOT re-validate or re-fetch the associated Copilot user response. Several variants carry secret material; treat this method's params as containing secrets at rest and in transit.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the credential update succeeded.</returns>
    public async Task<SessionSetCredentialsResult> SetCredentialsAsync(AuthInfo? credentials = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionSetCredentialsParams { SessionId = _session.SessionId, Credentials = credentials };
        return await CopilotClient.InvokeRpcAsync<SessionSetCredentialsResult>(_session.Rpc, "session.auth.setCredentials", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Canvas APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CanvasApi
{
    private readonly CopilotSession _session;

    internal CanvasApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Lists canvases declared for the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Declared canvases available in this session.</returns>
    public async Task<CanvasList> ListAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionCanvasListRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<CanvasList>(_session.Rpc, "session.canvas.list", [request], cancellationToken);
    }

    /// <summary>Lists currently open canvas instances for the live session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Live open-canvas snapshot.</returns>
    public async Task<CanvasListOpenResult> ListOpenAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionCanvasListOpenRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<CanvasListOpenResult>(_session.Rpc, "session.canvas.listOpen", [request], cancellationToken);
    }

    /// <summary>Opens or focuses a canvas instance.</summary>
    /// <param name="canvasId">Provider-local canvas identifier.</param>
    /// <param name="instanceId">Caller-supplied stable instance identifier.</param>
    /// <param name="extensionId">Owning provider identifier. Optional when the canvasId is unique across providers; required to disambiguate when multiple providers register the same canvasId.</param>
    /// <param name="input">Canvas open input.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Open canvas instance snapshot.</returns>
    public async Task<OpenCanvasInstance> OpenAsync(string canvasId, string instanceId, string? extensionId = null, object? input = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(canvasId);
        ArgumentNullException.ThrowIfNull(instanceId);
        _session.ThrowIfDisposed();

        var request = new CanvasOpenRequest { SessionId = _session.SessionId, CanvasId = canvasId, InstanceId = instanceId, ExtensionId = extensionId, Input = CopilotClient.ToJsonElementForWire(input) };
        return await CopilotClient.InvokeRpcAsync<OpenCanvasInstance>(_session.Rpc, "session.canvas.open", [request], cancellationToken);
    }

    /// <summary>Closes an open canvas instance.</summary>
    /// <param name="instanceId">Open canvas instance identifier.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task CloseAsync(string instanceId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(instanceId);
        _session.ThrowIfDisposed();

        var request = new CanvasCloseRequest { SessionId = _session.SessionId, InstanceId = instanceId };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.canvas.close", [request], cancellationToken);
    }

    /// <summary>Invokes an action on an open canvas instance.</summary>
    /// <param name="instanceId">Open canvas instance identifier.</param>
    /// <param name="actionName">Action name to invoke.</param>
    /// <param name="input">Action input.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Canvas action invocation result.</returns>
    public async Task<CanvasInvokeActionResult> InvokeActionAsync(string instanceId, string actionName, object? input = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(instanceId);
        ArgumentNullException.ThrowIfNull(actionName);
        _session.ThrowIfDisposed();

        var request = new CanvasInvokeActionRequest { SessionId = _session.SessionId, InstanceId = instanceId, ActionName = actionName, Input = CopilotClient.ToJsonElementForWire(input) };
        return await CopilotClient.InvokeRpcAsync<CanvasInvokeActionResult>(_session.Rpc, "session.canvas.invokeAction", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Model APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ModelApi
{
    private readonly CopilotSession _session;

    internal ModelApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Gets the currently selected model for the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>The currently selected model and reasoning effort for the session.</returns>
    public async Task<CurrentModel> GetCurrentAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionModelGetCurrentRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<CurrentModel>(_session.Rpc, "session.model.getCurrent", [request], cancellationToken);
    }

    /// <summary>Switches the session to a model and optional reasoning configuration.</summary>
    /// <param name="modelId">Model identifier to switch to.</param>
    /// <param name="reasoningEffort">Reasoning effort level to use for the model. "none" disables reasoning.</param>
    /// <param name="reasoningSummary">Reasoning summary mode to request for supported model clients.</param>
    /// <param name="modelCapabilities">Override individual model capabilities resolved by the runtime.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>The model identifier active on the session after the switch.</returns>
    public async Task<ModelSwitchToResult> SwitchToAsync(string modelId, string? reasoningEffort = null, ReasoningSummary? reasoningSummary = null, ModelCapabilitiesOverride? modelCapabilities = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(modelId);
        _session.ThrowIfDisposed();

        var request = new ModelSwitchToRequest { SessionId = _session.SessionId, ModelId = modelId, ReasoningEffort = reasoningEffort, ReasoningSummary = reasoningSummary, ModelCapabilities = modelCapabilities };
        return await CopilotClient.InvokeRpcAsync<ModelSwitchToResult>(_session.Rpc, "session.model.switchTo", [request], cancellationToken);
    }

    /// <summary>Updates the session's reasoning effort without changing the selected model.</summary>
    /// <param name="reasoningEffort">Reasoning effort level to apply to the currently selected model. The host is responsible for validating the value against the model's supported levels before calling.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Update the session's reasoning effort without changing the selected model. Use `switchTo` instead when you also need to change the model. The runtime stores the effort on the session and applies it to subsequent turns.</returns>
    public async Task<ModelSetReasoningEffortResult> SetReasoningEffortAsync(string reasoningEffort, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(reasoningEffort);
        _session.ThrowIfDisposed();

        var request = new ModelSetReasoningEffortRequest { SessionId = _session.SessionId, ReasoningEffort = reasoningEffort };
        return await CopilotClient.InvokeRpcAsync<ModelSetReasoningEffortResult>(_session.Rpc, "session.model.setReasoningEffort", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Mode APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ModeApi
{
    private readonly CopilotSession _session;

    internal ModeApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Gets the current agent interaction mode.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>The session mode the agent is operating in.</returns>
    public async Task<SessionMode> GetAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionModeGetRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<SessionMode>(_session.Rpc, "session.mode.get", [request], cancellationToken);
    }

    /// <summary>Sets the current agent interaction mode.</summary>
    /// <param name="mode">The session mode the agent is operating in.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task SetAsync(SessionMode mode, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new ModeSetRequest { SessionId = _session.SessionId, Mode = mode };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.mode.set", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Name APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class NameApi
{
    private readonly CopilotSession _session;

    internal NameApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Gets the session's friendly name.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>The session's friendly name, or null when not yet set.</returns>
    public async Task<NameGetResult> GetAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionNameGetRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<NameGetResult>(_session.Rpc, "session.name.get", [request], cancellationToken);
    }

    /// <summary>Sets the session's friendly name.</summary>
    /// <param name="name">New session name (1–100 characters, trimmed of leading/trailing whitespace).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task SetAsync(string name, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(name);
        _session.ThrowIfDisposed();

        var request = new NameSetRequest { SessionId = _session.SessionId, Name = name };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.name.set", [request], cancellationToken);
    }

    /// <summary>Persists an auto-generated session summary as the session's name when no user-set name exists.</summary>
    /// <param name="summary">Auto-generated session summary. Empty/whitespace-only values are ignored; values are trimmed before persisting.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the auto-generated summary was applied as the session's name.</returns>
    public async Task<NameSetAutoResult> SetAutoAsync(string summary, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(summary);
        _session.ThrowIfDisposed();

        var request = new NameSetAutoRequest { SessionId = _session.SessionId, Summary = summary };
        return await CopilotClient.InvokeRpcAsync<NameSetAutoResult>(_session.Rpc, "session.name.setAuto", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Plan APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PlanApi
{
    private readonly CopilotSession _session;

    internal PlanApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Reads the session plan file from the workspace.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Existence, contents, and resolved path of the session plan file.</returns>
    public async Task<PlanReadResult> ReadAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionPlanReadRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<PlanReadResult>(_session.Rpc, "session.plan.read", [request], cancellationToken);
    }

    /// <summary>Writes new content to the session plan file.</summary>
    /// <param name="content">The new content for the plan file.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task UpdateAsync(string content, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(content);
        _session.ThrowIfDisposed();

        var request = new PlanUpdateRequest { SessionId = _session.SessionId, Content = content };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.plan.update", [request], cancellationToken);
    }

    /// <summary>Deletes the session plan file from the workspace.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task DeleteAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionPlanDeleteRequest { SessionId = _session.SessionId };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.plan.delete", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Workspaces APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class WorkspacesApi
{
    private readonly CopilotSession _session;

    internal WorkspacesApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Gets current workspace metadata for the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Current workspace metadata for the session, including its absolute filesystem path when available.</returns>
    public async Task<WorkspacesGetWorkspaceResult> GetWorkspaceAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionWorkspacesGetWorkspaceRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<WorkspacesGetWorkspaceResult>(_session.Rpc, "session.workspaces.getWorkspace", [request], cancellationToken);
    }

    /// <summary>Lists files stored in the session workspace files directory.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Relative paths of files stored in the session workspace files directory.</returns>
    public async Task<WorkspacesListFilesResult> ListFilesAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionWorkspacesListFilesRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<WorkspacesListFilesResult>(_session.Rpc, "session.workspaces.listFiles", [request], cancellationToken);
    }

    /// <summary>Reads a file from the session workspace files directory.</summary>
    /// <param name="path">Relative path within the workspace files directory.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Contents of the requested workspace file as a UTF-8 string.</returns>
    public async Task<WorkspacesReadFileResult> ReadFileAsync(string path, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(path);
        _session.ThrowIfDisposed();

        var request = new WorkspacesReadFileRequest { SessionId = _session.SessionId, Path = path };
        return await CopilotClient.InvokeRpcAsync<WorkspacesReadFileResult>(_session.Rpc, "session.workspaces.readFile", [request], cancellationToken);
    }

    /// <summary>Creates or overwrites a file in the session workspace files directory.</summary>
    /// <param name="path">Relative path within the workspace files directory.</param>
    /// <param name="content">File content to write as a UTF-8 string.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task CreateFileAsync(string path, string content, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(path);
        ArgumentNullException.ThrowIfNull(content);
        _session.ThrowIfDisposed();

        var request = new WorkspacesCreateFileRequest { SessionId = _session.SessionId, Path = path, Content = content };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.workspaces.createFile", [request], cancellationToken);
    }

    /// <summary>Lists workspace checkpoints in chronological order.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Workspace checkpoints in chronological order; empty when the workspace is not enabled.</returns>
    public async Task<WorkspacesListCheckpointsResult> ListCheckpointsAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionWorkspacesListCheckpointsRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<WorkspacesListCheckpointsResult>(_session.Rpc, "session.workspaces.listCheckpoints", [request], cancellationToken);
    }

    /// <summary>Reads the content of a workspace checkpoint by number.</summary>
    /// <param name="number">Checkpoint number to read.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing.</returns>
    public async Task<WorkspacesReadCheckpointResult> ReadCheckpointAsync(long number, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new WorkspacesReadCheckpointRequest { SessionId = _session.SessionId, Number = number };
        return await CopilotClient.InvokeRpcAsync<WorkspacesReadCheckpointResult>(_session.Rpc, "session.workspaces.readCheckpoint", [request], cancellationToken);
    }

    /// <summary>Saves pasted content as a UTF-8 file in the session workspace.</summary>
    /// <param name="content">Pasted content to save as a UTF-8 file.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Descriptor for the saved paste file, or null when the workspace is unavailable.</returns>
    public async Task<WorkspacesSaveLargePasteResult> SaveLargePasteAsync(string content, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(content);
        _session.ThrowIfDisposed();

        var request = new WorkspacesSaveLargePasteRequest { SessionId = _session.SessionId, Content = content };
        return await CopilotClient.InvokeRpcAsync<WorkspacesSaveLargePasteResult>(_session.Rpc, "session.workspaces.saveLargePaste", [request], cancellationToken);
    }

    /// <summary>Computes a diff for the session workspace.</summary>
    /// <param name="mode">Diff mode requested by the client.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Workspace diff result for the requested mode.</returns>
    public async Task<WorkspaceDiffResult> DiffAsync(WorkspaceDiffMode mode, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new WorkspacesDiffRequest { SessionId = _session.SessionId, Mode = mode };
        return await CopilotClient.InvokeRpcAsync<WorkspaceDiffResult>(_session.Rpc, "session.workspaces.diff", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Instructions APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class InstructionsApi
{
    private readonly CopilotSession _session;

    internal InstructionsApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Gets instruction sources loaded for the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Instruction sources loaded for the session, in merge order.</returns>
    public async Task<InstructionsGetSourcesResult> GetSourcesAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionInstructionsGetSourcesRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<InstructionsGetSourcesResult>(_session.Rpc, "session.instructions.getSources", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Fleet APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class FleetApi
{
    private readonly CopilotSession _session;

    internal FleetApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Starts fleet mode by submitting the fleet orchestration prompt to the session.</summary>
    /// <param name="prompt">Optional user prompt to combine with fleet instructions.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether fleet mode was successfully activated.</returns>
    public async Task<FleetStartResult> StartAsync(string? prompt = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new FleetStartRequest { SessionId = _session.SessionId, Prompt = prompt };
        return await CopilotClient.InvokeRpcAsync<FleetStartResult>(_session.Rpc, "session.fleet.start", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Agent APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class AgentApi
{
    private readonly CopilotSession _session;

    internal AgentApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Lists custom agents available to the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Custom agents available to the session.</returns>
    public async Task<AgentList> ListAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionAgentListRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<AgentList>(_session.Rpc, "session.agent.list", [request], cancellationToken);
    }

    /// <summary>Gets the currently selected custom agent for the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>The currently selected custom agent, or null when using the default agent.</returns>
    public async Task<AgentGetCurrentResult> GetCurrentAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionAgentGetCurrentRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<AgentGetCurrentResult>(_session.Rpc, "session.agent.getCurrent", [request], cancellationToken);
    }

    /// <summary>Selects a custom agent for subsequent turns in the session.</summary>
    /// <param name="name">Name of the custom agent to select.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>The newly selected custom agent.</returns>
    public async Task<AgentSelectResult> SelectAsync(string name, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(name);
        _session.ThrowIfDisposed();

        var request = new AgentSelectRequest { SessionId = _session.SessionId, Name = name };
        return await CopilotClient.InvokeRpcAsync<AgentSelectResult>(_session.Rpc, "session.agent.select", [request], cancellationToken);
    }

    /// <summary>Clears the selected custom agent and returns the session to the default agent.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task DeselectAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionAgentDeselectRequest { SessionId = _session.SessionId };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.agent.deselect", [request], cancellationToken);
    }

    /// <summary>Reloads custom agent definitions and returns the refreshed list.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Custom agents available to the session after reloading definitions from disk.</returns>
    public async Task<AgentReloadResult> ReloadAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionAgentReloadRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<AgentReloadResult>(_session.Rpc, "session.agent.reload", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Tasks APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TasksApi
{
    private readonly CopilotSession _session;

    internal TasksApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Starts a background agent task in the session.</summary>
    /// <param name="agentType">Type of agent to start (e.g., 'explore', 'task', 'general-purpose').</param>
    /// <param name="prompt">Task prompt for the agent.</param>
    /// <param name="name">Short name for the agent, used to generate a human-readable ID.</param>
    /// <param name="description">Short description of the task.</param>
    /// <param name="model">Optional model override.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Identifier assigned to the newly started background agent task.</returns>
    public async Task<TasksStartAgentResult> StartAgentAsync(string agentType, string prompt, string name, string? description = null, string? model = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(agentType);
        ArgumentNullException.ThrowIfNull(prompt);
        ArgumentNullException.ThrowIfNull(name);
        _session.ThrowIfDisposed();

        var request = new TasksStartAgentRequest { SessionId = _session.SessionId, AgentType = agentType, Prompt = prompt, Name = name, Description = description, Model = model };
        return await CopilotClient.InvokeRpcAsync<TasksStartAgentResult>(_session.Rpc, "session.tasks.startAgent", [request], cancellationToken);
    }

    /// <summary>Lists background tasks tracked by the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Background tasks currently tracked by the session.</returns>
    public async Task<TaskList> ListAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionTasksListRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<TaskList>(_session.Rpc, "session.tasks.list", [request], cancellationToken);
    }

    /// <summary>Refreshes metadata for any detached background shells the runtime knows about.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Refresh metadata for any detached background shells the runtime knows about. Use after a long pause to pick up exit/output state for shells running outside the agent loop.</returns>
    public async Task<TasksRefreshResult> RefreshAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionTasksRefreshRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<TasksRefreshResult>(_session.Rpc, "session.tasks.refresh", [request], cancellationToken);
    }

    /// <summary>Waits for all in-flight background tasks and any follow-up turns to settle.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Wait until all in-flight background tasks (agents + shells) and any follow-up turns scheduled by their completions have settled. Returns when the runtime is fully drained or after an internal timeout (default 10 minutes; configurable via COPILOT_TASK_WAIT_TIMEOUT_SECONDS).</returns>
    public async Task<TasksWaitForPendingResult> WaitForPendingAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionTasksWaitForPendingRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<TasksWaitForPendingResult>(_session.Rpc, "session.tasks.waitForPending", [request], cancellationToken);
    }

    /// <summary>Returns progress information for a background task by ID.</summary>
    /// <param name="id">Task identifier (agent ID or shell ID).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Progress information for the task, or null when no task with that ID is tracked.</returns>
    public async Task<TasksGetProgressResult> GetProgressAsync(string id, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(id);
        _session.ThrowIfDisposed();

        var request = new TasksGetProgressRequest { SessionId = _session.SessionId, Id = id };
        return await CopilotClient.InvokeRpcAsync<TasksGetProgressResult>(_session.Rpc, "session.tasks.getProgress", [request], cancellationToken);
    }

    /// <summary>Returns the first sync-waiting task that can currently be promoted to background mode.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>The first sync-waiting task that can currently be promoted to background mode.</returns>
    public async Task<TasksGetCurrentPromotableResult> GetCurrentPromotableAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionTasksGetCurrentPromotableRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<TasksGetCurrentPromotableResult>(_session.Rpc, "session.tasks.getCurrentPromotable", [request], cancellationToken);
    }

    /// <summary>Promotes an eligible synchronously-waited task so it continues running in the background.</summary>
    /// <param name="id">Task identifier.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the task was successfully promoted to background mode.</returns>
    public async Task<TasksPromoteToBackgroundResult> PromoteToBackgroundAsync(string id, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(id);
        _session.ThrowIfDisposed();

        var request = new TasksPromoteToBackgroundRequest { SessionId = _session.SessionId, Id = id };
        return await CopilotClient.InvokeRpcAsync<TasksPromoteToBackgroundResult>(_session.Rpc, "session.tasks.promoteToBackground", [request], cancellationToken);
    }

    /// <summary>Atomically promotes the first promotable sync-waiting task to background mode and returns it.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>The promoted task as it now exists in background mode, omitted if no promotable task was waiting.</returns>
    public async Task<TasksPromoteCurrentToBackgroundResult> PromoteCurrentToBackgroundAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionTasksPromoteCurrentToBackgroundRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<TasksPromoteCurrentToBackgroundResult>(_session.Rpc, "session.tasks.promoteCurrentToBackground", [request], cancellationToken);
    }

    /// <summary>Cancels a background task.</summary>
    /// <param name="id">Task identifier.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the background task was successfully cancelled.</returns>
    public async Task<TasksCancelResult> CancelAsync(string id, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(id);
        _session.ThrowIfDisposed();

        var request = new TasksCancelRequest { SessionId = _session.SessionId, Id = id };
        return await CopilotClient.InvokeRpcAsync<TasksCancelResult>(_session.Rpc, "session.tasks.cancel", [request], cancellationToken);
    }

    /// <summary>Removes a completed or cancelled background task from tracking.</summary>
    /// <param name="id">Task identifier.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the task was removed. False when the task does not exist or is still running/idle.</returns>
    public async Task<TasksRemoveResult> RemoveAsync(string id, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(id);
        _session.ThrowIfDisposed();

        var request = new TasksRemoveRequest { SessionId = _session.SessionId, Id = id };
        return await CopilotClient.InvokeRpcAsync<TasksRemoveResult>(_session.Rpc, "session.tasks.remove", [request], cancellationToken);
    }

    /// <summary>Sends a message to a background agent task.</summary>
    /// <param name="id">Agent task identifier.</param>
    /// <param name="message">Message content to send to the agent.</param>
    /// <param name="fromAgentId">Agent ID of the sender, if sent on behalf of another agent.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the message was delivered, with an error message when delivery failed.</returns>
    public async Task<TasksSendMessageResult> SendMessageAsync(string id, string message, string? fromAgentId = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(id);
        ArgumentNullException.ThrowIfNull(message);
        _session.ThrowIfDisposed();

        var request = new TasksSendMessageRequest { SessionId = _session.SessionId, Id = id, Message = message, FromAgentId = fromAgentId };
        return await CopilotClient.InvokeRpcAsync<TasksSendMessageResult>(_session.Rpc, "session.tasks.sendMessage", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Skills APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SkillsApi
{
    private readonly CopilotSession _session;

    internal SkillsApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Lists skills available to the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Skills available to the session, with their enabled state.</returns>
    public async Task<SkillList> ListAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionSkillsListRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<SkillList>(_session.Rpc, "session.skills.list", [request], cancellationToken);
    }

    /// <summary>Returns the skills that have been invoked during this session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Skills invoked during this session, ordered by invocation time (most recent last).</returns>
    public async Task<SkillsGetInvokedResult> GetInvokedAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionSkillsGetInvokedRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<SkillsGetInvokedResult>(_session.Rpc, "session.skills.getInvoked", [request], cancellationToken);
    }

    /// <summary>Enables a skill for the session.</summary>
    /// <param name="name">Name of the skill to enable.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task EnableAsync(string name, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(name);
        _session.ThrowIfDisposed();

        var request = new SkillsEnableRequest { SessionId = _session.SessionId, Name = name };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.skills.enable", [request], cancellationToken);
    }

    /// <summary>Disables a skill for the session.</summary>
    /// <param name="name">Name of the skill to disable.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task DisableAsync(string name, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(name);
        _session.ThrowIfDisposed();

        var request = new SkillsDisableRequest { SessionId = _session.SessionId, Name = name };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.skills.disable", [request], cancellationToken);
    }

    /// <summary>Reloads skill definitions for the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Diagnostics from reloading skill definitions, with warnings and errors as separate lists.</returns>
    public async Task<SkillsLoadDiagnostics> ReloadAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionSkillsReloadRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<SkillsLoadDiagnostics>(_session.Rpc, "session.skills.reload", [request], cancellationToken);
    }

    /// <summary>Ensures the session's skill definitions have been loaded from disk.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task EnsureLoadedAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionSkillsEnsureLoadedRequest { SessionId = _session.SessionId };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.skills.ensureLoaded", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Mcp APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpApi
{
    private readonly CopilotSession _session;

    internal McpApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Lists MCP servers configured for the session and their connection status.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>MCP servers configured for the session, with their connection status.</returns>
    public async Task<McpServerList> ListAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionMcpListRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<McpServerList>(_session.Rpc, "session.mcp.list", [request], cancellationToken);
    }

    /// <summary>Enables an MCP server for the session.</summary>
    /// <param name="serverName">Name of the MCP server to enable.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task EnableAsync(string serverName, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(serverName);
        _session.ThrowIfDisposed();

        var request = new McpEnableRequest { SessionId = _session.SessionId, ServerName = serverName };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.mcp.enable", [request], cancellationToken);
    }

    /// <summary>Disables an MCP server for the session.</summary>
    /// <param name="serverName">Name of the MCP server to disable.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task DisableAsync(string serverName, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(serverName);
        _session.ThrowIfDisposed();

        var request = new McpDisableRequest { SessionId = _session.SessionId, ServerName = serverName };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.mcp.disable", [request], cancellationToken);
    }

    /// <summary>Reloads MCP server connections for the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task ReloadAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionMcpReloadRequest { SessionId = _session.SessionId };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.mcp.reload", [request], cancellationToken);
    }

    /// <summary>Runs an MCP sampling inference on behalf of an MCP server.</summary>
    /// <param name="requestId">Caller-provided unique identifier for this sampling execution. Use this same ID with cancelSamplingExecution to cancel the in-flight call. Must be unique within the session for the lifetime of the call.</param>
    /// <param name="serverName">Name of the MCP server that initiated the sampling request.</param>
    /// <param name="mcpRequestId">The original MCP JSON-RPC request ID (string or number). Used by the runtime to correlate the inference with the originating MCP request for telemetry; this is distinct from `requestId` (which is the schema-level cancellation handle).</param>
    /// <param name="request">Raw MCP CreateMessageRequest params, as received in the `sampling.requested` event. Treated as opaque at the schema layer; the runtime converts the embedded MCP messages into the OpenAI chat-completion shape internally.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Outcome of an MCP sampling execution: success result, failure error, or cancellation.</returns>
    public async Task<McpSamplingExecutionResult> ExecuteSamplingAsync(string requestId, string serverName, object mcpRequestId, McpExecuteSamplingRequest request, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(requestId);
        ArgumentNullException.ThrowIfNull(serverName);
        ArgumentNullException.ThrowIfNull(mcpRequestId);
        ArgumentNullException.ThrowIfNull(request);
        _session.ThrowIfDisposed();

        var rpcRequest = new McpExecuteSamplingParams { SessionId = _session.SessionId, RequestId = requestId, ServerName = serverName, McpRequestId = CopilotClient.ToJsonElementForWire(mcpRequestId)!.Value, Request = request };
        return await CopilotClient.InvokeRpcAsync<McpSamplingExecutionResult>(_session.Rpc, "session.mcp.executeSampling", [rpcRequest], cancellationToken);
    }

    /// <summary>Cancels an in-flight MCP sampling execution by request ID.</summary>
    /// <param name="requestId">The requestId previously passed to executeSampling that should be cancelled.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether an in-flight sampling execution with the given requestId was found and cancelled.</returns>
    public async Task<McpCancelSamplingExecutionResult> CancelSamplingExecutionAsync(string requestId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(requestId);
        _session.ThrowIfDisposed();

        var request = new McpCancelSamplingExecutionParams { SessionId = _session.SessionId, RequestId = requestId };
        return await CopilotClient.InvokeRpcAsync<McpCancelSamplingExecutionResult>(_session.Rpc, "session.mcp.cancelSamplingExecution", [request], cancellationToken);
    }

    /// <summary>Sets how environment-variable values supplied to MCP servers are resolved (direct or indirect).</summary>
    /// <param name="mode">How environment-variable values supplied to MCP servers are resolved. "direct" passes literal string values; "indirect" treats values as references (e.g. names of environment variables on the host) that the runtime resolves before launch. Defaults to the runtime's startup mode; clients that intentionally launch MCP servers with literal values (e.g. CLI prompt mode and ACP) set this to "direct".</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Env-value mode recorded on the session after the update.</returns>
    public async Task<McpSetEnvValueModeResult> SetEnvValueModeAsync(McpSetEnvValueModeDetails mode, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new McpSetEnvValueModeParams { SessionId = _session.SessionId, Mode = mode };
        return await CopilotClient.InvokeRpcAsync<McpSetEnvValueModeResult>(_session.Rpc, "session.mcp.setEnvValueMode", [request], cancellationToken);
    }

    /// <summary>Removes the auto-managed `github` MCP server when present.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the auto-managed `github` MCP server was removed (false when nothing to remove).</returns>
    public async Task<McpRemoveGitHubResult> RemoveGitHubAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionMcpRemoveGitHubRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<McpRemoveGitHubResult>(_session.Rpc, "session.mcp.removeGitHub", [request], cancellationToken);
    }

    /// <summary>Oauth APIs.</summary>
    public McpOauthApi Oauth =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Apps APIs.</summary>
    public McpAppsApi Apps =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;
}

/// <summary>Provides session-scoped McpOauth APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpOauthApi
{
    private readonly CopilotSession _session;

    internal McpOauthApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Starts OAuth authentication for a remote MCP server.</summary>
    /// <param name="serverName">Name of the remote MCP server to authenticate.</param>
    /// <param name="forceReauth">When true, clears any cached OAuth token for the server and runs a full new authorization. Use when the user explicitly wants to switch accounts or believes their session is stuck.</param>
    /// <param name="clientName">Optional override for the OAuth client display name shown on the consent screen. Applies to newly registered dynamic clients only — existing registrations keep the name they were created with. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass their own surface-specific label so the consent screen matches the product the user sees.</param>
    /// <param name="callbackSuccessMessage">Optional override for the body text shown on the OAuth loopback callback success page. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass surface-specific copy telling the user where to return.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.</returns>
    public async Task<McpOauthLoginResult> LoginAsync(string serverName, bool? forceReauth = null, string? clientName = null, string? callbackSuccessMessage = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(serverName);
        _session.ThrowIfDisposed();

        var request = new McpOauthLoginRequest { SessionId = _session.SessionId, ServerName = serverName, ForceReauth = forceReauth, ClientName = clientName, CallbackSuccessMessage = callbackSuccessMessage };
        return await CopilotClient.InvokeRpcAsync<McpOauthLoginResult>(_session.Rpc, "session.mcp.oauth.login", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped McpApps APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class McpAppsApi
{
    private readonly CopilotSession _session;

    internal McpAppsApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Fetch an MCP resource (typically a `ui://` MCP App bundle, per SEP-1865) from a connected server. Requires the `mcp-apps` session capability.</summary>
    /// <param name="serverName">Name of the MCP server hosting the resource.</param>
    /// <param name="uri">Resource URI (typically ui://...).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Resource contents returned by the MCP server.</returns>
    public async Task<McpAppsReadResourceResult> ReadResourceAsync(string serverName, string uri, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(serverName);
        ArgumentNullException.ThrowIfNull(uri);
        _session.ThrowIfDisposed();

        var request = new McpAppsReadResourceRequest { SessionId = _session.SessionId, ServerName = serverName, Uri = uri };
        return await CopilotClient.InvokeRpcAsync<McpAppsReadResourceResult>(_session.Rpc, "session.mcp.apps.readResource", [request], cancellationToken);
    }

    /// <summary>List tools that an MCP App view is allowed to call (SEP-1865 visibility filter). Returns tools whose `_meta.ui.visibility` is unset (default `["model","app"]`) or includes `"app"`.</summary>
    /// <param name="serverName">MCP server hosting the app.</param>
    /// <param name="originServerName">**Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>App-callable tools from the named MCP server.</returns>
    public async Task<McpAppsListToolsResult> ListToolsAsync(string serverName, string originServerName, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(serverName);
        ArgumentNullException.ThrowIfNull(originServerName);
        _session.ThrowIfDisposed();

        var request = new McpAppsListToolsRequest { SessionId = _session.SessionId, ServerName = serverName, OriginServerName = originServerName };
        return await CopilotClient.InvokeRpcAsync<McpAppsListToolsResult>(_session.Rpc, "session.mcp.apps.listTools", [request], cancellationToken);
    }

    /// <summary>Call an MCP tool from an MCP App view (SEP-1865). Enforces the visibility check that prevents an app iframe from invoking model-only tools. Returns the standard MCP `CallToolResult`.</summary>
    /// <param name="serverName">MCP server hosting the tool.</param>
    /// <param name="toolName">MCP tool name.</param>
    /// <param name="originServerName">**Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.</param>
    /// <param name="arguments">Tool arguments.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Standard MCP CallToolResult.</returns>
    public async Task<IDictionary<string, JsonElement>> CallToolAsync(string serverName, string toolName, string originServerName, IDictionary<string, JsonElement>? arguments = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(serverName);
        ArgumentNullException.ThrowIfNull(toolName);
        ArgumentNullException.ThrowIfNull(originServerName);
        _session.ThrowIfDisposed();

        var request = new McpAppsCallToolRequest { SessionId = _session.SessionId, ServerName = serverName, ToolName = toolName, OriginServerName = originServerName, Arguments = arguments };
        return await CopilotClient.InvokeRpcAsync<IDictionary<string, JsonElement>>(_session.Rpc, "session.mcp.apps.callTool", [request], cancellationToken);
    }

    /// <summary>Replace the host context returned to MCP App guests on `ui/initialize`. Hosts use this to advertise theme, locale, or other metadata to the guest UI.</summary>
    /// <param name="context">Host context advertised to MCP App guests.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task SetHostContextAsync(McpAppsSetHostContextDetails context, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(context);
        _session.ThrowIfDisposed();

        var request = new McpAppsSetHostContextRequest { SessionId = _session.SessionId, Context = context };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.mcp.apps.setHostContext", [request], cancellationToken);
    }

    /// <summary>Read the current host context advertised to MCP App guests.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Current host context advertised to MCP App guests.</returns>
    public async Task<McpAppsHostContext> GetHostContextAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionMcpAppsGetHostContextRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<McpAppsHostContext>(_session.Rpc, "session.mcp.apps.getHostContext", [request], cancellationToken);
    }

    /// <summary>Diagnose MCP Apps wiring for a specific MCP server. Reports the session capability, feature-flag state, advertised extension, and how many tools have `_meta.ui` populated.</summary>
    /// <param name="serverName">MCP server to probe.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Diagnostic snapshot of MCP Apps wiring for the named server.</returns>
    public async Task<McpAppsDiagnoseResult> DiagnoseAsync(string serverName, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(serverName);
        _session.ThrowIfDisposed();

        var request = new McpAppsDiagnoseRequest { SessionId = _session.SessionId, ServerName = serverName };
        return await CopilotClient.InvokeRpcAsync<McpAppsDiagnoseResult>(_session.Rpc, "session.mcp.apps.diagnose", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Plugins APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PluginsApi
{
    private readonly CopilotSession _session;

    internal PluginsApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Lists plugins installed for the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Plugins installed for the session, with their enabled state and version metadata.</returns>
    public async Task<PluginList> ListAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionPluginsListRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<PluginList>(_session.Rpc, "session.plugins.list", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Options APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class OptionsApi
{
    private readonly CopilotSession _session;

    internal OptionsApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Patches the genuinely-mutable subset of session options.</summary>
    /// <param name="model">The model ID to use for assistant turns.</param>
    /// <param name="reasoningEffort">Reasoning effort for the selected model (model-defined enum).</param>
    /// <param name="clientName">Identifier of the client driving the session.</param>
    /// <param name="lspClientName">Identifier sent to LSP-style integrations.</param>
    /// <param name="integrationId">Stable integration identifier used for analytics and rate-limit attribution.</param>
    /// <param name="featureFlags">Map of feature-flag IDs to their boolean enabled state.</param>
    /// <param name="isExperimentalMode">Whether experimental capabilities are enabled.</param>
    /// <param name="provider">Custom model-provider configuration (BYOK). Opaque shape; see `ProviderConfig` in the runtime.</param>
    /// <param name="workingDirectory">Absolute working-directory path for shell tools.</param>
    /// <param name="availableTools">Allowlist of tool names available to this session.</param>
    /// <param name="excludedTools">Denylist of tool names for this session.</param>
    /// <param name="enableScriptSafety">Whether shell-script safety heuristics are enabled.</param>
    /// <param name="shellInitProfile">Shell init profile (`None` or `NonInteractive`).</param>
    /// <param name="shellProcessFlags">Per-shell process flags (e.g., `pwsh` arguments).</param>
    /// <param name="sandboxConfig">Sandbox configuration shape; opaque to SDK consumers. See `SandboxConfig` in the runtime.</param>
    /// <param name="logInteractiveShells">Whether interactive shell sessions are logged.</param>
    /// <param name="envValueMode">How env values are passed to MCP servers (`direct` inlines literal values; `indirect` resolves at launch).</param>
    /// <param name="skillDirectories">Additional directories to search for skills.</param>
    /// <param name="disabledSkills">Skill IDs that should be excluded from this session.</param>
    /// <param name="enableOnDemandInstructionDiscovery">Whether to discover custom instructions on demand after successful file views (AGENTS.md / CLAUDE.md / .github/copilot-instructions.md surfacing). Combined with `skipCustomInstructions` and the runtime-side `ON_DEMAND_INSTRUCTIONS` feature flag.</param>
    /// <param name="installedPlugins">Full set of installed plugins for the session. Replaces the existing list; the runtime invalidates the skills cache only when the list materially changes.</param>
    /// <param name="customAgentsLocalOnly">Whether to default custom agents to local-only execution.</param>
    /// <param name="skipCustomInstructions">Whether to skip loading custom instruction sources.</param>
    /// <param name="disabledInstructionSources">Instruction source IDs to exclude from the system prompt.</param>
    /// <param name="coauthorEnabled">Whether to include the `Co-authored-by` trailer in commit messages.</param>
    /// <param name="trajectoryFile">Optional path for trajectory output.</param>
    /// <param name="enableStreaming">Whether to stream model responses.</param>
    /// <param name="copilotUrl">Override URL for the Copilot API endpoint.</param>
    /// <param name="askUserDisabled">Whether to disable the `ask_user` tool (encourages autonomous behavior).</param>
    /// <param name="continueOnAutoMode">Whether to allow auto-mode continuation across turns.</param>
    /// <param name="runningInInteractiveMode">Whether the session is running in an interactive UI.</param>
    /// <param name="enableReasoningSummaries">Whether to surface reasoning-summary events from the model.</param>
    /// <param name="agentContext">Runtime context discriminator (e.g., `cli`, `actions`).</param>
    /// <param name="eventsLogDirectory">Override directory for the session-events log. When unset, the runtime's default events log directory is used.</param>
    /// <param name="additionalContentExclusionPolicies">Additional content-exclusion policies to merge into the session's policy set. Opaque shape; see `ContentExclusionApiResponse` in the runtime.</param>
    /// <param name="manageScheduleEnabled">Whether to expose the `manage_schedule` tool to the agent. The runtime always owns the per-session schedule registry; this flag only controls tool exposure (typically gated to staff users).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the session options patch was applied successfully.</returns>
    public async Task<SessionUpdateOptionsResult> UpdateAsync(string? model = null, string? reasoningEffort = null, string? clientName = null, string? lspClientName = null, string? integrationId = null, IDictionary<string, bool>? featureFlags = null, bool? isExperimentalMode = null, object? provider = null, string? workingDirectory = null, IList<string>? availableTools = null, IList<string>? excludedTools = null, bool? enableScriptSafety = null, string? shellInitProfile = null, IList<string>? shellProcessFlags = null, object? sandboxConfig = null, bool? logInteractiveShells = null, OptionsUpdateEnvValueMode? envValueMode = null, IList<string>? skillDirectories = null, IList<string>? disabledSkills = null, bool? enableOnDemandInstructionDiscovery = null, IList<SessionInstalledPlugin>? installedPlugins = null, bool? customAgentsLocalOnly = null, bool? skipCustomInstructions = null, IList<string>? disabledInstructionSources = null, bool? coauthorEnabled = null, string? trajectoryFile = null, bool? enableStreaming = null, string? copilotUrl = null, bool? askUserDisabled = null, bool? continueOnAutoMode = null, bool? runningInInteractiveMode = null, bool? enableReasoningSummaries = null, string? agentContext = null, string? eventsLogDirectory = null, IList<object?>? additionalContentExclusionPolicies = null, bool? manageScheduleEnabled = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionUpdateOptionsParams { SessionId = _session.SessionId, Model = model, ReasoningEffort = reasoningEffort, ClientName = clientName, LspClientName = lspClientName, IntegrationId = integrationId, FeatureFlags = featureFlags, IsExperimentalMode = isExperimentalMode, Provider = CopilotClient.ToJsonElementForWire(provider), WorkingDirectory = workingDirectory, AvailableTools = availableTools, ExcludedTools = excludedTools, EnableScriptSafety = enableScriptSafety, ShellInitProfile = shellInitProfile, ShellProcessFlags = shellProcessFlags, SandboxConfig = CopilotClient.ToJsonElementForWire(sandboxConfig), LogInteractiveShells = logInteractiveShells, EnvValueMode = envValueMode, SkillDirectories = skillDirectories, DisabledSkills = disabledSkills, EnableOnDemandInstructionDiscovery = enableOnDemandInstructionDiscovery, InstalledPlugins = installedPlugins, CustomAgentsLocalOnly = customAgentsLocalOnly, SkipCustomInstructions = skipCustomInstructions, DisabledInstructionSources = disabledInstructionSources, CoauthorEnabled = coauthorEnabled, TrajectoryFile = trajectoryFile, EnableStreaming = enableStreaming, CopilotUrl = copilotUrl, AskUserDisabled = askUserDisabled, ContinueOnAutoMode = continueOnAutoMode, RunningInInteractiveMode = runningInInteractiveMode, EnableReasoningSummaries = enableReasoningSummaries, AgentContext = agentContext, EventsLogDirectory = eventsLogDirectory, AdditionalContentExclusionPolicies = additionalContentExclusionPolicies?.Select(static v => CopilotClient.ToJsonElementForWire(v)!.Value).ToList(), ManageScheduleEnabled = manageScheduleEnabled };
        return await CopilotClient.InvokeRpcAsync<SessionUpdateOptionsResult>(_session.Rpc, "session.options.update", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Lsp APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class LspApi
{
    private readonly CopilotSession _session;

    internal LspApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Loads the merged LSP configuration set for the session's working directory.</summary>
    /// <param name="workingDirectory">Working directory used to load project-level LSP configs. Defaults to the session working directory when omitted.</param>
    /// <param name="gitRoot">Git root used as the boundary when traversing for project-level LSP configs (supports monorepos).</param>
    /// <param name="force">Force re-initialization even when LSP configs were already loaded for the working directory.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task InitializeAsync(string? workingDirectory = null, string? gitRoot = null, bool? force = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new LspInitializeRequest { SessionId = _session.SessionId, WorkingDirectory = workingDirectory, GitRoot = gitRoot, Force = force };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.lsp.initialize", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Extensions APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ExtensionsApi
{
    private readonly CopilotSession _session;

    internal ExtensionsApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Lists extensions discovered for the session and their current status.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Extensions discovered for the session, with their current status.</returns>
    public async Task<ExtensionList> ListAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionExtensionsListRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<ExtensionList>(_session.Rpc, "session.extensions.list", [request], cancellationToken);
    }

    /// <summary>Enables an extension for the session.</summary>
    /// <param name="id">Source-qualified extension ID to enable.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task EnableAsync(string id, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(id);
        _session.ThrowIfDisposed();

        var request = new ExtensionsEnableRequest { SessionId = _session.SessionId, Id = id };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.extensions.enable", [request], cancellationToken);
    }

    /// <summary>Disables an extension for the session.</summary>
    /// <param name="id">Source-qualified extension ID to disable.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task DisableAsync(string id, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(id);
        _session.ThrowIfDisposed();

        var request = new ExtensionsDisableRequest { SessionId = _session.SessionId, Id = id };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.extensions.disable", [request], cancellationToken);
    }

    /// <summary>Reloads extension definitions and processes for the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task ReloadAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionExtensionsReloadRequest { SessionId = _session.SessionId };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.extensions.reload", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Tools APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ToolsApi
{
    private readonly CopilotSession _session;

    internal ToolsApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Provides the result for a pending external tool call.</summary>
    /// <param name="requestId">Request ID of the pending tool call.</param>
    /// <param name="result">Tool call result (string or expanded result object).</param>
    /// <param name="error">Error message if the tool call failed.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the external tool call result was handled successfully.</returns>
    public async Task<HandlePendingToolCallResult> HandlePendingToolCallAsync(string requestId, object? result = null, string? error = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(requestId);
        _session.ThrowIfDisposed();

        var request = new HandlePendingToolCallRequest { SessionId = _session.SessionId, RequestId = requestId, Result = CopilotClient.ToJsonElementForWire(result), Error = error };
        return await CopilotClient.InvokeRpcAsync<HandlePendingToolCallResult>(_session.Rpc, "session.tools.handlePendingToolCall", [request], cancellationToken);
    }

    /// <summary>Resolves, builds, and validates the runtime tool list for the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Resolve, build, and validate the runtime tool list for this session. Subagent sessions and consumer flows that need an initialized tool set before `send` invoke this. Default base-class implementation is a no-op for sessions that don't support tool validation.</returns>
    public async Task<ToolsInitializeAndValidateResult> InitializeAndValidateAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionToolsInitializeAndValidateRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<ToolsInitializeAndValidateResult>(_session.Rpc, "session.tools.initializeAndValidate", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Commands APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CommandsApi
{
    private readonly CopilotSession _session;

    internal CommandsApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Lists slash commands available in the session.</summary>
    /// <param name="request">Optional filters controlling which command sources to include in the listing.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Slash commands available in the session, after applying any include/exclude filters.</returns>
    public async Task<CommandList> ListAsync(CommandsListRequest? request = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var rpcRequest = new CommandsListRequestWithSession { SessionId = _session.SessionId, IncludeBuiltins = request?.IncludeBuiltins, IncludeSkills = request?.IncludeSkills, IncludeClientCommands = request?.IncludeClientCommands };
        return await CopilotClient.InvokeRpcAsync<CommandList>(_session.Rpc, "session.commands.list", [rpcRequest], cancellationToken);
    }

    /// <summary>Invokes a slash command in the session.</summary>
    /// <param name="name">Command name. Leading slashes are stripped and the name is matched case-insensitively.</param>
    /// <param name="input">Raw input after the command name.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Result of invoking the slash command (text output, prompt to send to the agent, or completion).</returns>
    public async Task<SlashCommandInvocationResult> InvokeAsync(string name, string? input = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(name);
        _session.ThrowIfDisposed();

        var request = new CommandsInvokeRequest { SessionId = _session.SessionId, Name = name, Input = input };
        return await CopilotClient.InvokeRpcAsync<SlashCommandInvocationResult>(_session.Rpc, "session.commands.invoke", [request], cancellationToken);
    }

    /// <summary>Reports completion of a pending client-handled slash command.</summary>
    /// <param name="requestId">Request ID from the command invocation event.</param>
    /// <param name="error">Error message if the command handler failed.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the pending client-handled command was completed successfully.</returns>
    public async Task<CommandsHandlePendingCommandResult> HandlePendingCommandAsync(string requestId, string? error = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(requestId);
        _session.ThrowIfDisposed();

        var request = new CommandsHandlePendingCommandRequest { SessionId = _session.SessionId, RequestId = requestId, Error = error };
        return await CopilotClient.InvokeRpcAsync<CommandsHandlePendingCommandResult>(_session.Rpc, "session.commands.handlePendingCommand", [request], cancellationToken);
    }

    /// <summary>Executes a slash command synchronously and returns any error.</summary>
    /// <param name="commandName">Name of the slash command to invoke (without the leading '/').</param>
    /// <param name="args">Argument string to pass to the command (empty string if none).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Error message produced while executing the command, if any.</returns>
    public async Task<ExecuteCommandResult> ExecuteAsync(string commandName, string args, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(commandName);
        ArgumentNullException.ThrowIfNull(args);
        _session.ThrowIfDisposed();

        var request = new ExecuteCommandParams { SessionId = _session.SessionId, CommandName = commandName, Args = args };
        return await CopilotClient.InvokeRpcAsync<ExecuteCommandResult>(_session.Rpc, "session.commands.execute", [request], cancellationToken);
    }

    /// <summary>Enqueues a slash command for FIFO processing on the local session.</summary>
    /// <param name="command">Slash-prefixed command string to enqueue, e.g. '/compact' or '/model gpt-4'. Queued FIFO with any in-flight items; if the session is idle, processing kicks off immediately.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the command was accepted into the local execution queue.</returns>
    public async Task<EnqueueCommandResult> EnqueueAsync(string command, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(command);
        _session.ThrowIfDisposed();

        var request = new EnqueueCommandParams { SessionId = _session.SessionId, Command = command };
        return await CopilotClient.InvokeRpcAsync<EnqueueCommandResult>(_session.Rpc, "session.commands.enqueue", [request], cancellationToken);
    }

    /// <summary>Reports whether the host actually executed a queued command and whether to continue processing.</summary>
    /// <param name="requestId">Request ID from the `command.queued` event the host is responding to.</param>
    /// <param name="result">Result of the queued command execution.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the queued-command response was matched to a pending request.</returns>
    public async Task<CommandsRespondToQueuedCommandResult> RespondToQueuedCommandAsync(string requestId, QueuedCommandResult result, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(requestId);
        ArgumentNullException.ThrowIfNull(result);
        _session.ThrowIfDisposed();

        var request = new CommandsRespondToQueuedCommandRequest { SessionId = _session.SessionId, RequestId = requestId, Result = result };
        return await CopilotClient.InvokeRpcAsync<CommandsRespondToQueuedCommandResult>(_session.Rpc, "session.commands.respondToQueuedCommand", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Telemetry APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class TelemetryApi
{
    private readonly CopilotSession _session;

    internal TelemetryApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Sets feature override key/value pairs to attach to subsequent telemetry events for the session.</summary>
    /// <param name="features">Override key/value pairs to attach to subsequent telemetry events from this session. Replaces any previously-set overrides.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task SetFeatureOverridesAsync(IDictionary<string, string> features, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(features);
        _session.ThrowIfDisposed();

        var request = new TelemetrySetFeatureOverridesRequest { SessionId = _session.SessionId, Features = features };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.telemetry.setFeatureOverrides", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Ui APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UiApi
{
    private readonly CopilotSession _session;

    internal UiApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Requests structured input from a UI-capable client.</summary>
    /// <param name="message">Message describing what information is needed from the user.</param>
    /// <param name="requestedSchema">JSON Schema describing the form fields to present to the user.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>The elicitation response (accept with form values, decline, or cancel).</returns>
    public async Task<UIElicitationResponse> ElicitationAsync(string message, UIElicitationSchema requestedSchema, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(message);
        ArgumentNullException.ThrowIfNull(requestedSchema);
        _session.ThrowIfDisposed();

        var request = new UIElicitationRequest { SessionId = _session.SessionId, Message = message, RequestedSchema = requestedSchema };
        return await CopilotClient.InvokeRpcAsync<UIElicitationResponse>(_session.Rpc, "session.ui.elicitation", [request], cancellationToken);
    }

    /// <summary>Provides the user response for a pending elicitation request.</summary>
    /// <param name="requestId">The unique request ID from the elicitation.requested event.</param>
    /// <param name="result">The elicitation response (accept with form values, decline, or cancel).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the elicitation response was accepted; false if it was already resolved by another client.</returns>
    public async Task<UIElicitationResult> HandlePendingElicitationAsync(string requestId, UIElicitationResponse result, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(requestId);
        ArgumentNullException.ThrowIfNull(result);
        _session.ThrowIfDisposed();

        var request = new UIHandlePendingElicitationRequest { SessionId = _session.SessionId, RequestId = requestId, Result = result };
        return await CopilotClient.InvokeRpcAsync<UIElicitationResult>(_session.Rpc, "session.ui.handlePendingElicitation", [request], cancellationToken);
    }

    /// <summary>Resolves a pending `user_input.requested` event with the user's response.</summary>
    /// <param name="requestId">The unique request ID from the user_input.requested event.</param>
    /// <param name="response">Schema for the `UIUserInputResponse` type.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the pending UI request was resolved by this call.</returns>
    public async Task<UIHandlePendingResult> HandlePendingUserInputAsync(string requestId, UIUserInputResponse response, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(requestId);
        ArgumentNullException.ThrowIfNull(response);
        _session.ThrowIfDisposed();

        var request = new UIHandlePendingUserInputRequest { SessionId = _session.SessionId, RequestId = requestId, Response = response };
        return await CopilotClient.InvokeRpcAsync<UIHandlePendingResult>(_session.Rpc, "session.ui.handlePendingUserInput", [request], cancellationToken);
    }

    /// <summary>Resolves a pending `sampling.requested` event with a sampling result, or rejects it.</summary>
    /// <param name="requestId">The unique request ID from the sampling.requested event.</param>
    /// <param name="response">Optional sampling result payload. Omit to reject/cancel the sampling request without providing a result.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the pending UI request was resolved by this call.</returns>
    public async Task<UIHandlePendingResult> HandlePendingSamplingAsync(string requestId, UIHandlePendingSamplingResponse? response = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(requestId);
        _session.ThrowIfDisposed();

        var request = new UIHandlePendingSamplingRequest { SessionId = _session.SessionId, RequestId = requestId, Response = response };
        return await CopilotClient.InvokeRpcAsync<UIHandlePendingResult>(_session.Rpc, "session.ui.handlePendingSampling", [request], cancellationToken);
    }

    /// <summary>Resolves a pending `auto_mode_switch.requested` event with the user's accept/decline decision.</summary>
    /// <param name="requestId">The unique request ID from the auto_mode_switch.requested event.</param>
    /// <param name="response">User's choice for auto-mode switching: yes (allow this turn), yes_always (allow + persist as setting), or no (decline).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the pending UI request was resolved by this call.</returns>
    public async Task<UIHandlePendingResult> HandlePendingAutoModeSwitchAsync(string requestId, UIAutoModeSwitchResponse response, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(requestId);
        _session.ThrowIfDisposed();

        var request = new UIHandlePendingAutoModeSwitchRequest { SessionId = _session.SessionId, RequestId = requestId, Response = response };
        return await CopilotClient.InvokeRpcAsync<UIHandlePendingResult>(_session.Rpc, "session.ui.handlePendingAutoModeSwitch", [request], cancellationToken);
    }

    /// <summary>Resolves a pending `exit_plan_mode.requested` event with the user's response.</summary>
    /// <param name="requestId">The unique request ID from the exit_plan_mode.requested event.</param>
    /// <param name="response">Schema for the `UIExitPlanModeResponse` type.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the pending UI request was resolved by this call.</returns>
    public async Task<UIHandlePendingResult> HandlePendingExitPlanModeAsync(string requestId, UIExitPlanModeResponse response, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(requestId);
        ArgumentNullException.ThrowIfNull(response);
        _session.ThrowIfDisposed();

        var request = new UIHandlePendingExitPlanModeRequest { SessionId = _session.SessionId, RequestId = requestId, Response = response };
        return await CopilotClient.InvokeRpcAsync<UIHandlePendingResult>(_session.Rpc, "session.ui.handlePendingExitPlanMode", [request], cancellationToken);
    }

    /// <summary>Registers an in-process handler for auto-mode-switch requests so the server bridge skips dispatch.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Register an in-process handler for `auto_mode_switch.requested` events. The caller still attaches the actual listener via the standard event-subscription mechanism; this registration solely tells the server bridge to skip its own dispatch (so a remote client doesn't race the in-process handler for the same requestId).</returns>
    public async Task<UIRegisterDirectAutoModeSwitchHandlerResult> RegisterDirectAutoModeSwitchHandlerAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionUiRegisterDirectAutoModeSwitchHandlerRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<UIRegisterDirectAutoModeSwitchHandlerResult>(_session.Rpc, "session.ui.registerDirectAutoModeSwitchHandler", [request], cancellationToken);
    }

    /// <summary>Unregisters a previously-registered in-process auto-mode-switch handler by its opaque handle.</summary>
    /// <param name="handle">Handle previously returned by `registerDirectAutoModeSwitchHandler`.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the handle was active and the registration count was decremented.</returns>
    public async Task<UIUnregisterDirectAutoModeSwitchHandlerResult> UnregisterDirectAutoModeSwitchHandlerAsync(string handle, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(handle);
        _session.ThrowIfDisposed();

        var request = new UIUnregisterDirectAutoModeSwitchHandlerRequest { SessionId = _session.SessionId, Handle = handle };
        return await CopilotClient.InvokeRpcAsync<UIUnregisterDirectAutoModeSwitchHandlerResult>(_session.Rpc, "session.ui.unregisterDirectAutoModeSwitchHandler", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Permissions APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsApi
{
    private readonly CopilotSession _session;

    internal PermissionsApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Replaces selected permission policy fields (rules, paths, URLs, exclusions, allow-all flags) on the session.</summary>
    /// <param name="approveAllToolPermissionRequests">If specified, sets whether tool permission requests are auto-approved without prompting. Omit to leave the current value unchanged.</param>
    /// <param name="approveAllReadPermissionRequests">If specified, sets whether path/URL read permission requests are auto-approved. Omit to leave the current value unchanged.</param>
    /// <param name="rules">If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged.</param>
    /// <param name="paths">If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.</param>
    /// <param name="urls">If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged.</param>
    /// <param name="additionalContentExclusionPolicies">If specified, replaces the host-supplied GitHub Content Exclusion policies on the session (combined with natively-discovered policies when evaluating tool/file access). Omit to leave the current policies unchanged.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the operation succeeded.</returns>
    public async Task<PermissionsConfigureResult> ConfigureAsync(bool? approveAllToolPermissionRequests = null, bool? approveAllReadPermissionRequests = null, PermissionRulesSet? rules = null, PermissionPathsConfig? paths = null, PermissionUrlsConfig? urls = null, IList<PermissionsConfigureAdditionalContentExclusionPolicy>? additionalContentExclusionPolicies = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new PermissionsConfigureParams { SessionId = _session.SessionId, ApproveAllToolPermissionRequests = approveAllToolPermissionRequests, ApproveAllReadPermissionRequests = approveAllReadPermissionRequests, Rules = rules, Paths = paths, Urls = urls, AdditionalContentExclusionPolicies = additionalContentExclusionPolicies };
        return await CopilotClient.InvokeRpcAsync<PermissionsConfigureResult>(_session.Rpc, "session.permissions.configure", [request], cancellationToken);
    }

    /// <summary>Provides a decision for a pending tool permission request.</summary>
    /// <param name="requestId">Request ID of the pending permission request.</param>
    /// <param name="result">The client's response to the pending permission prompt.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the permission decision was applied; false when the request was already resolved.</returns>
    public async Task<PermissionRequestResult> HandlePendingPermissionRequestAsync(string requestId, PermissionDecision result, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(requestId);
        ArgumentNullException.ThrowIfNull(result);
        _session.ThrowIfDisposed();

        var request = new PermissionDecisionRequest { SessionId = _session.SessionId, RequestId = requestId, Result = result };
        return await CopilotClient.InvokeRpcAsync<PermissionRequestResult>(_session.Rpc, "session.permissions.handlePendingPermissionRequest", [request], cancellationToken);
    }

    /// <summary>Reconstructs the set of pending tool permission requests from the session's event history.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>List of pending permission requests reconstructed from event history.</returns>
    public async Task<PendingPermissionRequestList> PendingRequestsAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new PermissionsPendingRequestsRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<PendingPermissionRequestList>(_session.Rpc, "session.permissions.pendingRequests", [request], cancellationToken);
    }

    /// <summary>Enables or disables automatic approval of tool permission requests for the session.</summary>
    /// <param name="enabled">Whether to auto-approve all tool permission requests.</param>
    /// <param name="source">Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the operation succeeded.</returns>
    public async Task<PermissionsSetApproveAllResult> SetApproveAllAsync(bool enabled, PermissionsSetApproveAllSource? source = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new PermissionsSetApproveAllRequest { SessionId = _session.SessionId, Enabled = enabled, Source = source };
        return await CopilotClient.InvokeRpcAsync<PermissionsSetApproveAllResult>(_session.Rpc, "session.permissions.setApproveAll", [request], cancellationToken);
    }

    /// <summary>Adds or removes session-scoped or location-scoped permission rules.</summary>
    /// <param name="scope">Whether the change applies to ephemeral session-scoped rules (cleared at session end) or to location-scoped rules persisted via the location-permissions config file.</param>
    /// <param name="add">Rules to add to the scope. Applied before `remove`/`removeAll`.</param>
    /// <param name="remove">Specific rules to remove from the scope. Ignored when `removeAll` is true.</param>
    /// <param name="removeAll">When true, removes every rule currently in the scope (after any `add` is applied). Useful for clearing the location scope wholesale.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the operation succeeded.</returns>
    public async Task<PermissionsModifyRulesResult> ModifyRulesAsync(PermissionsModifyRulesScope scope, IList<PermissionRule>? add = null, IList<PermissionRule>? remove = null, bool? removeAll = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new PermissionsModifyRulesParams { SessionId = _session.SessionId, Scope = scope, Add = add, Remove = remove, RemoveAll = removeAll };
        return await CopilotClient.InvokeRpcAsync<PermissionsModifyRulesResult>(_session.Rpc, "session.permissions.modifyRules", [request], cancellationToken);
    }

    /// <summary>Sets whether the client wants permission prompts bridged into session events.</summary>
    /// <param name="required">Whether the client wants `permission.requested` events bridged from the session-owned permission service. CLI clients that render prompt UI set this to `true` for as long as their listener is mounted; headless callers leave it unset (the default is `false`).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the operation succeeded.</returns>
    public async Task<PermissionsSetRequiredResult> SetRequiredAsync(bool required, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new PermissionsSetRequiredRequest { SessionId = _session.SessionId, Required = required };
        return await CopilotClient.InvokeRpcAsync<PermissionsSetRequiredResult>(_session.Rpc, "session.permissions.setRequired", [request], cancellationToken);
    }

    /// <summary>Clears session-scoped tool permission approvals.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the operation succeeded.</returns>
    public async Task<PermissionsResetSessionApprovalsResult> ResetSessionApprovalsAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new PermissionsResetSessionApprovalsRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<PermissionsResetSessionApprovalsResult>(_session.Rpc, "session.permissions.resetSessionApprovals", [request], cancellationToken);
    }

    /// <summary>Notifies the runtime that a permission prompt UI has been shown to the user.</summary>
    /// <param name="message">Human-readable description of the prompt the user is being asked to approve. Used by the runtime to fire the registered `permission_prompt` notification hook (e.g. terminal bell, desktop notification).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the operation succeeded.</returns>
    public async Task<PermissionsNotifyPromptShownResult> NotifyPromptShownAsync(string message, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(message);
        _session.ThrowIfDisposed();

        var request = new PermissionPromptShownNotification { SessionId = _session.SessionId, Message = message };
        return await CopilotClient.InvokeRpcAsync<PermissionsNotifyPromptShownResult>(_session.Rpc, "session.permissions.notifyPromptShown", [request], cancellationToken);
    }

    /// <summary>Paths APIs.</summary>
    public PermissionsPathsApi Paths =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Locations APIs.</summary>
    public PermissionsLocationsApi Locations =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>FolderTrust APIs.</summary>
    public PermissionsFolderTrustApi FolderTrust =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;

    /// <summary>Urls APIs.</summary>
    public PermissionsUrlsApi Urls =>
        field ??
        Interlocked.CompareExchange(ref field, new(_session), null) ??
        field;
}

/// <summary>Provides session-scoped PermissionsPaths APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsPathsApi
{
    private readonly CopilotSession _session;

    internal PermissionsPathsApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Returns the session's allowed directories and primary working directory.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Snapshot of the session's allow-listed directories and primary working directory.</returns>
    public async Task<PermissionPathsList> ListAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new PermissionsPathsListRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<PermissionPathsList>(_session.Rpc, "session.permissions.paths.list", [request], cancellationToken);
    }

    /// <summary>Adds a directory to the session's allow-list.</summary>
    /// <param name="path">Directory to add to the allow-list. The runtime resolves and validates the path before adding.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the operation succeeded.</returns>
    public async Task<PermissionsPathsAddResult> AddAsync(string path, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(path);
        _session.ThrowIfDisposed();

        var request = new PermissionPathsAddParams { SessionId = _session.SessionId, Path = path };
        return await CopilotClient.InvokeRpcAsync<PermissionsPathsAddResult>(_session.Rpc, "session.permissions.paths.add", [request], cancellationToken);
    }

    /// <summary>Updates the session's primary working directory used by the permission policy.</summary>
    /// <param name="path">Directory to set as the new primary working directory for the session's permission policy.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the operation succeeded.</returns>
    public async Task<PermissionsPathsUpdatePrimaryResult> UpdatePrimaryAsync(string path, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(path);
        _session.ThrowIfDisposed();

        var request = new PermissionPathsUpdatePrimaryParams { SessionId = _session.SessionId, Path = path };
        return await CopilotClient.InvokeRpcAsync<PermissionsPathsUpdatePrimaryResult>(_session.Rpc, "session.permissions.paths.updatePrimary", [request], cancellationToken);
    }

    /// <summary>Reports whether a path falls within any of the session's allowed directories.</summary>
    /// <param name="path">Path to check against the session's allowed directories.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the supplied path is within the session's allowed directories.</returns>
    public async Task<PermissionPathsAllowedCheckResult> IsPathWithinAllowedDirectoriesAsync(string path, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(path);
        _session.ThrowIfDisposed();

        var request = new PermissionPathsAllowedCheckParams { SessionId = _session.SessionId, Path = path };
        return await CopilotClient.InvokeRpcAsync<PermissionPathsAllowedCheckResult>(_session.Rpc, "session.permissions.paths.isPathWithinAllowedDirectories", [request], cancellationToken);
    }

    /// <summary>Reports whether a path falls within the session's workspace (primary) directory.</summary>
    /// <param name="path">Path to check against the session workspace directory.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the supplied path is within the session's workspace directory.</returns>
    public async Task<PermissionPathsWorkspaceCheckResult> IsPathWithinWorkspaceAsync(string path, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(path);
        _session.ThrowIfDisposed();

        var request = new PermissionPathsWorkspaceCheckParams { SessionId = _session.SessionId, Path = path };
        return await CopilotClient.InvokeRpcAsync<PermissionPathsWorkspaceCheckResult>(_session.Rpc, "session.permissions.paths.isPathWithinWorkspace", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped PermissionsLocations APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsLocationsApi
{
    private readonly CopilotSession _session;

    internal PermissionsLocationsApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Resolves the permission location key and type for a working directory.</summary>
    /// <param name="workingDirectory">Working directory whose permission location should be resolved.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Resolved location-permissions key and type.</returns>
    public async Task<PermissionLocationResolveResult> ResolveAsync(string workingDirectory, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(workingDirectory);
        _session.ThrowIfDisposed();

        var request = new PermissionLocationResolveParams { SessionId = _session.SessionId, WorkingDirectory = workingDirectory };
        return await CopilotClient.InvokeRpcAsync<PermissionLocationResolveResult>(_session.Rpc, "session.permissions.locations.resolve", [request], cancellationToken);
    }

    /// <summary>Applies persisted location-scoped tool approvals and allowed directories for a working directory to this session's permission service.</summary>
    /// <param name="workingDirectory">Working directory whose persisted location permissions should be applied.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Summary of persisted location permissions applied to the session.</returns>
    public async Task<PermissionLocationApplyResult> ApplyAsync(string workingDirectory, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(workingDirectory);
        _session.ThrowIfDisposed();

        var request = new PermissionLocationApplyParams { SessionId = _session.SessionId, WorkingDirectory = workingDirectory };
        return await CopilotClient.InvokeRpcAsync<PermissionLocationApplyResult>(_session.Rpc, "session.permissions.locations.apply", [request], cancellationToken);
    }

    /// <summary>Persists a tool approval for a permission location and applies its rules to this session's live permission service.</summary>
    /// <param name="locationKey">Location key (git root or cwd) to persist the approval to.</param>
    /// <param name="approval">Tool approval to persist and apply.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the operation succeeded.</returns>
    public async Task<PermissionsLocationsAddToolApprovalResult> AddToolApprovalAsync(string locationKey, PermissionsLocationsAddToolApprovalDetails approval, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(locationKey);
        ArgumentNullException.ThrowIfNull(approval);
        _session.ThrowIfDisposed();

        var request = new PermissionLocationAddToolApprovalParams { SessionId = _session.SessionId, LocationKey = locationKey, Approval = approval };
        return await CopilotClient.InvokeRpcAsync<PermissionsLocationsAddToolApprovalResult>(_session.Rpc, "session.permissions.locations.addToolApproval", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped PermissionsFolderTrust APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsFolderTrustApi
{
    private readonly CopilotSession _session;

    internal PermissionsFolderTrustApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Reports whether a folder is trusted according to the user's folder trust state.</summary>
    /// <param name="path">Folder path to check.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Folder trust check result.</returns>
    public async Task<FolderTrustCheckResult> IsTrustedAsync(string path, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(path);
        _session.ThrowIfDisposed();

        var request = new FolderTrustCheckParams { SessionId = _session.SessionId, Path = path };
        return await CopilotClient.InvokeRpcAsync<FolderTrustCheckResult>(_session.Rpc, "session.permissions.folderTrust.isTrusted", [request], cancellationToken);
    }

    /// <summary>Adds a folder to the user's trusted folders list.</summary>
    /// <param name="path">Folder path to mark as trusted.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the operation succeeded.</returns>
    public async Task<PermissionsFolderTrustAddTrustedResult> AddTrustedAsync(string path, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(path);
        _session.ThrowIfDisposed();

        var request = new FolderTrustAddParams { SessionId = _session.SessionId, Path = path };
        return await CopilotClient.InvokeRpcAsync<PermissionsFolderTrustAddTrustedResult>(_session.Rpc, "session.permissions.folderTrust.addTrusted", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped PermissionsUrls APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class PermissionsUrlsApi
{
    private readonly CopilotSession _session;

    internal PermissionsUrlsApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Toggles the runtime's URL-permission policy between unrestricted and restricted modes.</summary>
    /// <param name="enabled">Whether to allow access to all URLs without prompting. Toggles the runtime's URL-permission policy in place.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the operation succeeded.</returns>
    public async Task<PermissionsUrlsSetUnrestrictedModeResult> SetUnrestrictedModeAsync(bool enabled, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new PermissionUrlsSetUnrestrictedModeParams { SessionId = _session.SessionId, Enabled = enabled };
        return await CopilotClient.InvokeRpcAsync<PermissionsUrlsSetUnrestrictedModeResult>(_session.Rpc, "session.permissions.urls.setUnrestrictedMode", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Metadata APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class MetadataApi
{
    private readonly CopilotSession _session;

    internal MetadataApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Returns a snapshot of the session's identifying metadata, mode, agent, and remote info.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Point-in-time snapshot of slow-changing session identifier and state fields.</returns>
    public async Task<SessionMetadataSnapshot> SnapshotAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionMetadataSnapshotRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<SessionMetadataSnapshot>(_session.Rpc, "session.metadata.snapshot", [request], cancellationToken);
    }

    /// <summary>Reports whether the local session is currently processing user/agent messages.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the local session is currently processing a turn or background continuation.</returns>
    public async Task<MetadataIsProcessingResult> IsProcessingAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionMetadataIsProcessingRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<MetadataIsProcessingResult>(_session.Rpc, "session.metadata.isProcessing", [request], cancellationToken);
    }

    /// <summary>Returns the token breakdown for the session's current context window for a given model.</summary>
    /// <param name="promptTokenLimit">Maximum prompt tokens allowed by the target model. Pass 0 to use the runtime default.</param>
    /// <param name="outputTokenLimit">Maximum output tokens allowed by the target model. Pass 0 if unknown.</param>
    /// <param name="selectedModel">Model identifier used for tokenization. Omit to use the session default. Used both for token counting and to compute display values.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Token breakdown for the session's current context window, or null if uninitialized.</returns>
    public async Task<MetadataContextInfoResult> ContextInfoAsync(long promptTokenLimit, long outputTokenLimit, string? selectedModel = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new MetadataContextInfoRequest { SessionId = _session.SessionId, PromptTokenLimit = promptTokenLimit, OutputTokenLimit = outputTokenLimit, SelectedModel = selectedModel };
        return await CopilotClient.InvokeRpcAsync<MetadataContextInfoResult>(_session.Rpc, "session.metadata.contextInfo", [request], cancellationToken);
    }

    /// <summary>Records a working-directory/git context change and emits a `session.context_changed` event.</summary>
    /// <param name="context">Updated working directory and git context. Emitted as the new payload of `session.context_changed`.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Notify the session that its working directory context has changed. Emits a `session.context_changed` event so consumers (telemetry, OTel tracker, ACP, the timeline UI) can react. Use this when the host has detected a cwd/branch/repo change outside the session's normal lifecycle (e.g., after a shell command in interactive mode).</returns>
    public async Task<MetadataRecordContextChangeResult> RecordContextChangeAsync(SessionWorkingDirectoryContext context, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(context);
        _session.ThrowIfDisposed();

        var request = new MetadataRecordContextChangeRequest { SessionId = _session.SessionId, Context = context };
        return await CopilotClient.InvokeRpcAsync<MetadataRecordContextChangeResult>(_session.Rpc, "session.metadata.recordContextChange", [request], cancellationToken);
    }

    /// <summary>Updates the session's recorded working directory.</summary>
    /// <param name="workingDirectory">Absolute path to set as the session's working directory. The runtime updates the session's recorded cwd so subsequent operations (shell tools, file lookups, telemetry) anchor to it.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Update the session's working directory. Used by the host when the user explicitly changes cwd (e.g., the `/cd` slash command). The host is responsible for `process.chdir` and any related side-effects (file index, etc.); this method only updates the session's own recorded path.</returns>
    public async Task<MetadataSetWorkingDirectoryResult> SetWorkingDirectoryAsync(string workingDirectory, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(workingDirectory);
        _session.ThrowIfDisposed();

        var request = new MetadataSetWorkingDirectoryRequest { SessionId = _session.SessionId, WorkingDirectory = workingDirectory };
        return await CopilotClient.InvokeRpcAsync<MetadataSetWorkingDirectoryResult>(_session.Rpc, "session.metadata.setWorkingDirectory", [request], cancellationToken);
    }

    /// <summary>Re-tokenizes the session's existing messages against a model and returns aggregate token totals.</summary>
    /// <param name="modelId">Model identifier used for tokenization. The runtime token-counts both chat-context and system-context messages against this model.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session.</returns>
    public async Task<MetadataRecomputeContextTokensResult> RecomputeContextTokensAsync(string modelId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(modelId);
        _session.ThrowIfDisposed();

        var request = new MetadataRecomputeContextTokensRequest { SessionId = _session.SessionId, ModelId = modelId };
        return await CopilotClient.InvokeRpcAsync<MetadataRecomputeContextTokensResult>(_session.Rpc, "session.metadata.recomputeContextTokens", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Shell APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ShellApi
{
    private readonly CopilotSession _session;

    internal ShellApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Starts a shell command and streams output through session notifications.</summary>
    /// <param name="command">Shell command to execute.</param>
    /// <param name="cwd">Working directory (defaults to session working directory).</param>
    /// <param name="timeout">Timeout in milliseconds (default: 30000).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Identifier of the spawned process, used to correlate streamed output and exit notifications.</returns>
    public async Task<ShellExecResult> ExecAsync(string command, string? cwd = null, TimeSpan? timeout = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(command);
        _session.ThrowIfDisposed();

        var request = new ShellExecRequest { SessionId = _session.SessionId, Command = command, Cwd = cwd, Timeout = timeout };
        return await CopilotClient.InvokeRpcAsync<ShellExecResult>(_session.Rpc, "session.shell.exec", [request], cancellationToken);
    }

    /// <summary>Sends a signal to a shell process previously started via "shell.exec".</summary>
    /// <param name="processId">Process identifier returned by shell.exec.</param>
    /// <param name="signal">Signal to send (default: SIGTERM).</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the signal was delivered; false if the process was unknown or already exited.</returns>
    public async Task<ShellKillResult> KillAsync(string processId, ShellKillSignal? signal = null, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(processId);
        _session.ThrowIfDisposed();

        var request = new ShellKillRequest { SessionId = _session.SessionId, ProcessId = processId, Signal = signal };
        return await CopilotClient.InvokeRpcAsync<ShellKillResult>(_session.Rpc, "session.shell.kill", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped History APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class HistoryApi
{
    private readonly CopilotSession _session;

    internal HistoryApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Compacts the session history to reduce context usage.</summary>
    /// <param name="request">Optional compaction parameters.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.</returns>
    public async Task<HistoryCompactResult> CompactAsync(HistoryCompactRequest? request = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var rpcRequest = new HistoryCompactRequestWithSession { SessionId = _session.SessionId, CustomInstructions = request?.CustomInstructions };
        return await CopilotClient.InvokeRpcAsync<HistoryCompactResult>(_session.Rpc, "session.history.compact", [rpcRequest], cancellationToken);
    }

    /// <summary>Truncates persisted session history to a specific event.</summary>
    /// <param name="eventId">Event ID to truncate to. This event and all events after it are removed from the session.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Number of events that were removed by the truncation.</returns>
    public async Task<HistoryTruncateResult> TruncateAsync(string eventId, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(eventId);
        _session.ThrowIfDisposed();

        var request = new HistoryTruncateRequest { SessionId = _session.SessionId, EventId = eventId };
        return await CopilotClient.InvokeRpcAsync<HistoryTruncateResult>(_session.Rpc, "session.history.truncate", [request], cancellationToken);
    }

    /// <summary>Cancels any in-progress background compaction on a local session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether an in-progress background compaction was cancelled.</returns>
    public async Task<HistoryCancelBackgroundCompactionResult> CancelBackgroundCompactionAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionHistoryCancelBackgroundCompactionRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<HistoryCancelBackgroundCompactionResult>(_session.Rpc, "session.history.cancelBackgroundCompaction", [request], cancellationToken);
    }

    /// <summary>Aborts any in-progress manual compaction on a local session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether an in-progress manual compaction was aborted.</returns>
    public async Task<HistoryAbortManualCompactionResult> AbortManualCompactionAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionHistoryAbortManualCompactionRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<HistoryAbortManualCompactionResult>(_session.Rpc, "session.history.abortManualCompaction", [request], cancellationToken);
    }

    /// <summary>Produces a markdown summary of the session's conversation context for hand-off scenarios.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Markdown summary of the conversation context (empty when not available).</returns>
    public async Task<HistorySummarizeForHandoffResult> SummarizeForHandoffAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionHistorySummarizeForHandoffRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<HistorySummarizeForHandoffResult>(_session.Rpc, "session.history.summarizeForHandoff", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Queue APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class QueueApi
{
    private readonly CopilotSession _session;

    internal QueueApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Returns the local session's pending user-facing queued items and steering messages.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Snapshot of the session's pending queued items and immediate-steering messages.</returns>
    public async Task<QueuePendingItemsResult> PendingItemsAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionQueuePendingItemsRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<QueuePendingItemsResult>(_session.Rpc, "session.queue.pendingItems", [request], cancellationToken);
    }

    /// <summary>Removes the most recently queued user-facing item (LIFO).</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether a user-facing pending item was removed.</returns>
    public async Task<QueueRemoveMostRecentResult> RemoveMostRecentAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionQueueRemoveMostRecentRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<QueueRemoveMostRecentResult>(_session.Rpc, "session.queue.removeMostRecent", [request], cancellationToken);
    }

    /// <summary>Clears all pending queued items on the local session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task ClearAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionQueueClearRequest { SessionId = _session.SessionId };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.queue.clear", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped EventLog APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class EventLogApi
{
    private readonly CopilotSession _session;

    internal EventLogApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Reads a batch of session events from a cursor, optionally waiting for new events.</summary>
    /// <param name="cursor">Opaque cursor returned by a previous read. Omit on the first call to start from the beginning of the session's persisted history.</param>
    /// <param name="max">Maximum number of events to return in this batch (1–1000, default 200).</param>
    /// <param name="waitMs">Milliseconds to wait for new events when the cursor is at the tail of history. 0 (default) returns immediately even if no events are available. Capped at 30000ms. Ephemeral events that arrive during the wait are delivered in this batch but are NOT replayable on a subsequent read (use a non-zero waitMs in your next call to capture future ephemerals as they happen).</param>
    /// <param name="types">Either '*' to receive all event types, or a non-empty list of event types to receive.</param>
    /// <param name="agentScope">Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Batch of session events returned by a read, with cursor and continuation metadata.</returns>
    public async Task<EventsReadResult> ReadAsync(string? cursor = null, int? max = null, TimeSpan? waitMs = null, object? types = null, EventsAgentScope? agentScope = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new EventLogReadRequest { SessionId = _session.SessionId, Cursor = cursor, Max = max, Wait = waitMs, Types = CopilotClient.ToJsonElementForWire(types), AgentScope = agentScope };
        return await CopilotClient.InvokeRpcAsync<EventsReadResult>(_session.Rpc, "session.eventLog.read", [request], cancellationToken);
    }

    /// <summary>Returns a snapshot of the current tail cursor without consuming events.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Snapshot of the current tail cursor without returning any events. Use this when a consumer wants to subscribe to live events going forward without first paginating through the entire persisted history (which would happen if `read` were called without a cursor on a long-lived session).</returns>
    public async Task<EventLogTailResult> TailAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionEventLogTailRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<EventLogTailResult>(_session.Rpc, "session.eventLog.tail", [request], cancellationToken);
    }

    /// <summary>Registers consumer interest in an event type for runtime gating purposes.</summary>
    /// <param name="eventType">The event type the consumer wants the runtime to treat as 'observed' for behavior-switching gating. Some runtime code paths inspect whether any consumer is interested in a specific event type and choose a different implementation accordingly (e.g. `mcp.oauth_required`: when interest is registered the runtime delegates the full interactive OAuth flow to the consumer; when no interest is registered the runtime installs a browserless fallback that silently reuses cached tokens). SDK clients that long-poll events do NOT automatically appear as listeners to these gating checks — they must explicitly call `registerInterest` for each event type they want the runtime to count as having a consumer. Multiple registrations for the same event type from the same or different consumers are tracked independently and must each be released. See: `mcp.oauth_required`, `sampling.requested`, `auto_mode_switch.requested`, `user_input.requested`, `elicitation.requested`, `command.queued`, `exit_plan_mode.requested`.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Opaque handle representing an event-type interest registration.</returns>
    public async Task<RegisterEventInterestResult> RegisterInterestAsync(string eventType, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(eventType);
        _session.ThrowIfDisposed();

        var request = new RegisterEventInterestParams { SessionId = _session.SessionId, EventType = eventType };
        return await CopilotClient.InvokeRpcAsync<RegisterEventInterestResult>(_session.Rpc, "session.eventLog.registerInterest", [request], cancellationToken);
    }

    /// <summary>Releases a consumer's previously-registered interest in an event type.</summary>
    /// <param name="handle">Handle returned by a previous `registerInterest` call. Idempotent: releasing an unknown or already-released handle is a no-op (returns success). When the last outstanding handle for an event type is released, the runtime reverts to its 'no consumer' code path for that event type.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the operation succeeded.</returns>
    public async Task<EventLogReleaseInterestResult> ReleaseInterestAsync(string handle, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(handle);
        _session.ThrowIfDisposed();

        var request = new ReleaseEventInterestParams { SessionId = _session.SessionId, Handle = handle };
        return await CopilotClient.InvokeRpcAsync<EventLogReleaseInterestResult>(_session.Rpc, "session.eventLog.releaseInterest", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Usage APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class UsageApi
{
    private readonly CopilotSession _session;

    internal UsageApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Gets accumulated usage metrics for the session.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals.</returns>
    public async Task<UsageGetMetricsResult> GetMetricsAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionUsageGetMetricsRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<UsageGetMetricsResult>(_session.Rpc, "session.usage.getMetrics", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Remote APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class RemoteApi
{
    private readonly CopilotSession _session;

    internal RemoteApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Enables remote session export or steering.</summary>
    /// <param name="mode">Per-session remote mode. "off" disables remote, "export" exports session events to GitHub without enabling remote steering, "on" enables both export and remote steering.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>GitHub URL for the session and a flag indicating whether remote steering is enabled.</returns>
    public async Task<RemoteEnableResult> EnableAsync(RemoteSessionMode? mode = null, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new RemoteEnableRequest { SessionId = _session.SessionId, Mode = mode };
        return await CopilotClient.InvokeRpcAsync<RemoteEnableResult>(_session.Rpc, "session.remote.enable", [request], cancellationToken);
    }

    /// <summary>Disables remote session export and steering.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    public async Task DisableAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionRemoteDisableRequest { SessionId = _session.SessionId };
        await CopilotClient.InvokeRpcAsync(_session.Rpc, "session.remote.disable", [request], cancellationToken);
    }

    /// <summary>Persists a remote-steerability change emitted by the host as a session event.</summary>
    /// <param name="remoteSteerable">Whether the session now supports remote steering via GitHub. The runtime persists this as a `session.remote_steerable_changed` event so resume/replay sees the up-to-date capability.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Persist a steerability change as a `session.remote_steerable_changed` event. Used by the host (CLI / SDK consumer) when it has just finished enabling or disabling steering on a remote exporter that the runtime does not directly own.</returns>
    public async Task<RemoteNotifySteerableChangedResult> NotifySteerableChangedAsync(bool remoteSteerable, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new RemoteNotifySteerableChangedRequest { SessionId = _session.SessionId, RemoteSteerable = remoteSteerable };
        return await CopilotClient.InvokeRpcAsync<RemoteNotifySteerableChangedResult>(_session.Rpc, "session.remote.notifySteerableChanged", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Schedule APIs.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class ScheduleApi
{
    private readonly CopilotSession _session;

    internal ScheduleApi(CopilotSession session)
    {
        _session = session;
    }

    /// <summary>Lists the session's currently active scheduled prompts.</summary>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Snapshot of the currently active recurring prompts for this session.</returns>
    public async Task<ScheduleList> ListAsync(CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new SessionScheduleListRequest { SessionId = _session.SessionId };
        return await CopilotClient.InvokeRpcAsync<ScheduleList>(_session.Rpc, "session.schedule.list", [request], cancellationToken);
    }

    /// <summary>Removes a scheduled prompt by id.</summary>
    /// <param name="id">Id of the scheduled prompt to remove.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Remove a scheduled prompt by id. The result entry is omitted if the id was unknown.</returns>
    public async Task<ScheduleStopResult> StopAsync(long id, CancellationToken cancellationToken = default)
    {
        _session.ThrowIfDisposed();

        var request = new ScheduleStopRequest { SessionId = _session.SessionId, Id = id };
        return await CopilotClient.InvokeRpcAsync<ScheduleStopResult>(_session.Rpc, "session.schedule.stop", [request], cancellationToken);
    }
}

/// <summary>Handles `sessionFs` client session API methods.</summary>
[Experimental(Diagnostics.Experimental)]
public interface ISessionFsHandler
{
    /// <summary>Reads a file from the client-provided session filesystem.</summary>
    /// <param name="request">Path of the file to read from the client-provided session filesystem.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>File content as a UTF-8 string, or a filesystem error if the read failed.</returns>
    Task<SessionFsReadFileResult> ReadFileAsync(SessionFsReadFileRequest request, CancellationToken cancellationToken = default);
    /// <summary>Writes a file in the client-provided session filesystem.</summary>
    /// <param name="request">File path, content to write, and optional mode for the client-provided session filesystem.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Describes a filesystem error.</returns>
    Task<SessionFsError?> WriteFileAsync(SessionFsWriteFileRequest request, CancellationToken cancellationToken = default);
    /// <summary>Appends content to a file in the client-provided session filesystem.</summary>
    /// <param name="request">File path, content to append, and optional mode for the client-provided session filesystem.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Describes a filesystem error.</returns>
    Task<SessionFsError?> AppendFileAsync(SessionFsAppendFileRequest request, CancellationToken cancellationToken = default);
    /// <summary>Checks whether a path exists in the client-provided session filesystem.</summary>
    /// <param name="request">Path to test for existence in the client-provided session filesystem.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the requested path exists in the client-provided session filesystem.</returns>
    Task<SessionFsExistsResult> ExistsAsync(SessionFsExistsRequest request, CancellationToken cancellationToken = default);
    /// <summary>Gets metadata for a path in the client-provided session filesystem.</summary>
    /// <param name="request">Path whose metadata should be returned from the client-provided session filesystem.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Filesystem metadata for the requested path, or a filesystem error if the stat failed.</returns>
    Task<SessionFsStatResult> StatAsync(SessionFsStatRequest request, CancellationToken cancellationToken = default);
    /// <summary>Creates a directory in the client-provided session filesystem.</summary>
    /// <param name="request">Directory path to create in the client-provided session filesystem, with options for recursive creation and POSIX mode.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Describes a filesystem error.</returns>
    Task<SessionFsError?> MkdirAsync(SessionFsMkdirRequest request, CancellationToken cancellationToken = default);
    /// <summary>Lists entry names in a directory from the client-provided session filesystem.</summary>
    /// <param name="request">Directory path whose entries should be listed from the client-provided session filesystem.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Names of entries in the requested directory, or a filesystem error if the read failed.</returns>
    Task<SessionFsReaddirResult> ReaddirAsync(SessionFsReaddirRequest request, CancellationToken cancellationToken = default);
    /// <summary>Lists directory entries with type information from the client-provided session filesystem.</summary>
    /// <param name="request">Directory path whose entries (with type information) should be listed from the client-provided session filesystem.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Entries in the requested directory paired with file/directory type information, or a filesystem error if the read failed.</returns>
    Task<SessionFsReaddirWithTypesResult> ReaddirWithTypesAsync(SessionFsReaddirWithTypesRequest request, CancellationToken cancellationToken = default);
    /// <summary>Removes a file or directory from the client-provided session filesystem.</summary>
    /// <param name="request">Path to remove from the client-provided session filesystem, with options for recursive removal and force.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Describes a filesystem error.</returns>
    Task<SessionFsError?> RmAsync(SessionFsRmRequest request, CancellationToken cancellationToken = default);
    /// <summary>Renames or moves a path in the client-provided session filesystem.</summary>
    /// <param name="request">Source and destination paths for renaming or moving an entry in the client-provided session filesystem.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Describes a filesystem error.</returns>
    Task<SessionFsError?> RenameAsync(SessionFsRenameRequest request, CancellationToken cancellationToken = default);
    /// <summary>Executes a SQLite query against the per-session database.</summary>
    /// <param name="request">SQL query, query type, and optional bind parameters for executing a SQLite query against the per-session database.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Query results including rows, columns, and rows affected, or a filesystem error if execution failed.</returns>
    Task<SessionFsSqliteQueryResult> SqliteQueryAsync(SessionFsSqliteQueryRequest request, CancellationToken cancellationToken = default);
    /// <summary>Checks whether the per-session SQLite database already exists, without creating it.</summary>
    /// <param name="request">Identifies the target session.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Indicates whether the per-session SQLite database already exists.</returns>
    Task<SessionFsSqliteExistsResult> SqliteExistsAsync(SessionFsSqliteExistsRequest request, CancellationToken cancellationToken = default);
}

/// <summary>Handles `canvas` client session API methods.</summary>
[Experimental(Diagnostics.Experimental)]
public interface ICanvasHandler
{
    /// <summary>Opens a canvas instance on the provider.</summary>
    /// <param name="request">Canvas open parameters sent to the provider.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Canvas open result returned by the provider.</returns>
    Task<CanvasProviderOpenResult> OpenAsync(CanvasProviderOpenRequest request, CancellationToken cancellationToken = default);
    /// <summary>Closes a canvas instance on the provider.</summary>
    /// <param name="request">Canvas close parameters sent to the provider.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    Task CloseAsync(CanvasProviderCloseRequest request, CancellationToken cancellationToken = default);
    /// <summary>Invokes an action on an open canvas instance via the provider.</summary>
    /// <param name="request">Canvas action invocation parameters sent to the provider.</param>
    /// <param name="cancellationToken">The <see cref="CancellationToken"/> to monitor for cancellation requests. The default is <see cref="CancellationToken.None"/>.</param>
    /// <returns>Provider-supplied action result.</returns>
    Task<object> InvokeActionAsync(CanvasProviderInvokeActionRequest request, CancellationToken cancellationToken = default);
}

/// <summary>Provides all client session API handler groups for a session.</summary>
public sealed class ClientSessionApiHandlers
{
    /// <summary>Optional handler for SessionFs client session API methods.</summary>
    public ISessionFsHandler? SessionFs { get; set; }

    /// <summary>Optional handler for Canvas client session API methods.</summary>
    public ICanvasHandler? Canvas { get; set; }
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
        rpc.SetLocalRpcMethod("sessionFs.sqliteQuery", (Func<SessionFsSqliteQueryRequest, CancellationToken, ValueTask<SessionFsSqliteQueryResult>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.SqliteQueryAsync(request, cancellationToken);
        }), singleObjectParam: true);
        rpc.SetLocalRpcMethod("sessionFs.sqliteExists", (Func<SessionFsSqliteExistsRequest, CancellationToken, ValueTask<SessionFsSqliteExistsResult>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).SessionFs;
            if (handler is null) throw new InvalidOperationException($"No sessionFs handler registered for session: {request.SessionId}");
            return await handler.SqliteExistsAsync(request, cancellationToken);
        }), singleObjectParam: true);
        rpc.SetLocalRpcMethod("canvas.open", (Func<CanvasProviderOpenRequest, CancellationToken, ValueTask<CanvasProviderOpenResult>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).Canvas;
            if (handler is null) throw new InvalidOperationException($"No canvas handler registered for session: {request.SessionId}");
            return await handler.OpenAsync(request, cancellationToken);
        }), singleObjectParam: true);
        rpc.SetLocalRpcMethod("canvas.close", (Func<CanvasProviderCloseRequest, CancellationToken, ValueTask>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).Canvas;
            if (handler is null) throw new InvalidOperationException($"No canvas handler registered for session: {request.SessionId}");
            await handler.CloseAsync(request, cancellationToken);
        }), singleObjectParam: true);
        rpc.SetLocalRpcMethod("canvas.invokeAction", (Func<CanvasProviderInvokeActionRequest, CancellationToken, ValueTask<object>>)(async (request, cancellationToken) =>
        {
            var handler = getHandlers(request.SessionId).Canvas;
            if (handler is null) throw new InvalidOperationException($"No canvas handler registered for session: {request.SessionId}");
            return await handler.InvokeActionAsync(request, cancellationToken);
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
[JsonSerializable(typeof(GitHub.Copilot.AbortData), TypeInfoPropertyName = "SessionEventsAbortData")]
[JsonSerializable(typeof(GitHub.Copilot.AbortEvent), TypeInfoPropertyName = "SessionEventsAbortEvent")]
[JsonSerializable(typeof(GitHub.Copilot.AbortReason), TypeInfoPropertyName = "SessionEventsAbortReason")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantIntentData), TypeInfoPropertyName = "SessionEventsAssistantIntentData")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantIntentEvent), TypeInfoPropertyName = "SessionEventsAssistantIntentEvent")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantMessageData), TypeInfoPropertyName = "SessionEventsAssistantMessageData")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantMessageDeltaData), TypeInfoPropertyName = "SessionEventsAssistantMessageDeltaData")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantMessageDeltaEvent), TypeInfoPropertyName = "SessionEventsAssistantMessageDeltaEvent")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantMessageEvent), TypeInfoPropertyName = "SessionEventsAssistantMessageEvent")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantMessageStartData), TypeInfoPropertyName = "SessionEventsAssistantMessageStartData")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantMessageStartEvent), TypeInfoPropertyName = "SessionEventsAssistantMessageStartEvent")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantMessageToolRequest), TypeInfoPropertyName = "SessionEventsAssistantMessageToolRequest")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantMessageToolRequestType), TypeInfoPropertyName = "SessionEventsAssistantMessageToolRequestType")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantReasoningData), TypeInfoPropertyName = "SessionEventsAssistantReasoningData")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantReasoningDeltaData), TypeInfoPropertyName = "SessionEventsAssistantReasoningDeltaData")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantReasoningDeltaEvent), TypeInfoPropertyName = "SessionEventsAssistantReasoningDeltaEvent")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantReasoningEvent), TypeInfoPropertyName = "SessionEventsAssistantReasoningEvent")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantStreamingDeltaData), TypeInfoPropertyName = "SessionEventsAssistantStreamingDeltaData")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantStreamingDeltaEvent), TypeInfoPropertyName = "SessionEventsAssistantStreamingDeltaEvent")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantTurnEndData), TypeInfoPropertyName = "SessionEventsAssistantTurnEndData")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantTurnEndEvent), TypeInfoPropertyName = "SessionEventsAssistantTurnEndEvent")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantTurnStartData), TypeInfoPropertyName = "SessionEventsAssistantTurnStartData")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantTurnStartEvent), TypeInfoPropertyName = "SessionEventsAssistantTurnStartEvent")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantUsageApiEndpoint), TypeInfoPropertyName = "SessionEventsAssistantUsageApiEndpoint")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantUsageCopilotUsageTokenDetail), TypeInfoPropertyName = "SessionEventsAssistantUsageCopilotUsageTokenDetail")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantUsageData), TypeInfoPropertyName = "SessionEventsAssistantUsageData")]
[JsonSerializable(typeof(GitHub.Copilot.AssistantUsageEvent), TypeInfoPropertyName = "SessionEventsAssistantUsageEvent")]
[JsonSerializable(typeof(GitHub.Copilot.AutoModeSwitchCompletedData), TypeInfoPropertyName = "SessionEventsAutoModeSwitchCompletedData")]
[JsonSerializable(typeof(GitHub.Copilot.AutoModeSwitchCompletedEvent), TypeInfoPropertyName = "SessionEventsAutoModeSwitchCompletedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.AutoModeSwitchRequestedData), TypeInfoPropertyName = "SessionEventsAutoModeSwitchRequestedData")]
[JsonSerializable(typeof(GitHub.Copilot.AutoModeSwitchRequestedEvent), TypeInfoPropertyName = "SessionEventsAutoModeSwitchRequestedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.AutoModeSwitchResponse), TypeInfoPropertyName = "SessionEventsAutoModeSwitchResponse")]
[JsonSerializable(typeof(GitHub.Copilot.CanvasOpenedAvailability), TypeInfoPropertyName = "SessionEventsCanvasOpenedAvailability")]
[JsonSerializable(typeof(GitHub.Copilot.CanvasRegistryChangedCanvas), TypeInfoPropertyName = "SessionEventsCanvasRegistryChangedCanvas")]
[JsonSerializable(typeof(GitHub.Copilot.CanvasRegistryChangedCanvasAction), TypeInfoPropertyName = "SessionEventsCanvasRegistryChangedCanvasAction")]
[JsonSerializable(typeof(GitHub.Copilot.CapabilitiesChangedData), TypeInfoPropertyName = "SessionEventsCapabilitiesChangedData")]
[JsonSerializable(typeof(GitHub.Copilot.CapabilitiesChangedEvent), TypeInfoPropertyName = "SessionEventsCapabilitiesChangedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.CapabilitiesChangedUI), TypeInfoPropertyName = "SessionEventsCapabilitiesChangedUI")]
[JsonSerializable(typeof(GitHub.Copilot.CommandCompletedData), TypeInfoPropertyName = "SessionEventsCommandCompletedData")]
[JsonSerializable(typeof(GitHub.Copilot.CommandCompletedEvent), TypeInfoPropertyName = "SessionEventsCommandCompletedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.CommandExecuteData), TypeInfoPropertyName = "SessionEventsCommandExecuteData")]
[JsonSerializable(typeof(GitHub.Copilot.CommandExecuteEvent), TypeInfoPropertyName = "SessionEventsCommandExecuteEvent")]
[JsonSerializable(typeof(GitHub.Copilot.CommandQueuedData), TypeInfoPropertyName = "SessionEventsCommandQueuedData")]
[JsonSerializable(typeof(GitHub.Copilot.CommandQueuedEvent), TypeInfoPropertyName = "SessionEventsCommandQueuedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.CommandsChangedCommand), TypeInfoPropertyName = "SessionEventsCommandsChangedCommand")]
[JsonSerializable(typeof(GitHub.Copilot.CommandsChangedData), TypeInfoPropertyName = "SessionEventsCommandsChangedData")]
[JsonSerializable(typeof(GitHub.Copilot.CommandsChangedEvent), TypeInfoPropertyName = "SessionEventsCommandsChangedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.CompactionCompleteCompactionTokensUsed), TypeInfoPropertyName = "SessionEventsCompactionCompleteCompactionTokensUsed")]
[JsonSerializable(typeof(GitHub.Copilot.CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail), TypeInfoPropertyName = "SessionEventsCompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail")]
[JsonSerializable(typeof(GitHub.Copilot.CustomAgentsUpdatedAgent), TypeInfoPropertyName = "SessionEventsCustomAgentsUpdatedAgent")]
[JsonSerializable(typeof(GitHub.Copilot.ElicitationCompletedAction), TypeInfoPropertyName = "SessionEventsElicitationCompletedAction")]
[JsonSerializable(typeof(GitHub.Copilot.ElicitationCompletedData), TypeInfoPropertyName = "SessionEventsElicitationCompletedData")]
[JsonSerializable(typeof(GitHub.Copilot.ElicitationCompletedEvent), TypeInfoPropertyName = "SessionEventsElicitationCompletedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.ElicitationRequestedData), TypeInfoPropertyName = "SessionEventsElicitationRequestedData")]
[JsonSerializable(typeof(GitHub.Copilot.ElicitationRequestedEvent), TypeInfoPropertyName = "SessionEventsElicitationRequestedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.ElicitationRequestedMode), TypeInfoPropertyName = "SessionEventsElicitationRequestedMode")]
[JsonSerializable(typeof(GitHub.Copilot.ElicitationRequestedSchema), TypeInfoPropertyName = "SessionEventsElicitationRequestedSchema")]
[JsonSerializable(typeof(GitHub.Copilot.EmbeddedBlobResourceContents), TypeInfoPropertyName = "SessionEventsEmbeddedBlobResourceContents")]
[JsonSerializable(typeof(GitHub.Copilot.EmbeddedTextResourceContents), TypeInfoPropertyName = "SessionEventsEmbeddedTextResourceContents")]
[JsonSerializable(typeof(GitHub.Copilot.ExitPlanModeAction), TypeInfoPropertyName = "SessionEventsExitPlanModeAction")]
[JsonSerializable(typeof(GitHub.Copilot.ExitPlanModeCompletedData), TypeInfoPropertyName = "SessionEventsExitPlanModeCompletedData")]
[JsonSerializable(typeof(GitHub.Copilot.ExitPlanModeCompletedEvent), TypeInfoPropertyName = "SessionEventsExitPlanModeCompletedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.ExitPlanModeRequestedData), TypeInfoPropertyName = "SessionEventsExitPlanModeRequestedData")]
[JsonSerializable(typeof(GitHub.Copilot.ExitPlanModeRequestedEvent), TypeInfoPropertyName = "SessionEventsExitPlanModeRequestedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.ExtensionsLoadedExtension), TypeInfoPropertyName = "SessionEventsExtensionsLoadedExtension")]
[JsonSerializable(typeof(GitHub.Copilot.ExtensionsLoadedExtensionSource), TypeInfoPropertyName = "SessionEventsExtensionsLoadedExtensionSource")]
[JsonSerializable(typeof(GitHub.Copilot.ExtensionsLoadedExtensionStatus), TypeInfoPropertyName = "SessionEventsExtensionsLoadedExtensionStatus")]
[JsonSerializable(typeof(GitHub.Copilot.ExternalToolCompletedData), TypeInfoPropertyName = "SessionEventsExternalToolCompletedData")]
[JsonSerializable(typeof(GitHub.Copilot.ExternalToolCompletedEvent), TypeInfoPropertyName = "SessionEventsExternalToolCompletedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.ExternalToolRequestedData), TypeInfoPropertyName = "SessionEventsExternalToolRequestedData")]
[JsonSerializable(typeof(GitHub.Copilot.ExternalToolRequestedEvent), TypeInfoPropertyName = "SessionEventsExternalToolRequestedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.HandoffRepository), TypeInfoPropertyName = "SessionEventsHandoffRepository")]
[JsonSerializable(typeof(GitHub.Copilot.HandoffSourceType), TypeInfoPropertyName = "SessionEventsHandoffSourceType")]
[JsonSerializable(typeof(GitHub.Copilot.HookEndData), TypeInfoPropertyName = "SessionEventsHookEndData")]
[JsonSerializable(typeof(GitHub.Copilot.HookEndError), TypeInfoPropertyName = "SessionEventsHookEndError")]
[JsonSerializable(typeof(GitHub.Copilot.HookEndEvent), TypeInfoPropertyName = "SessionEventsHookEndEvent")]
[JsonSerializable(typeof(GitHub.Copilot.HookStartData), TypeInfoPropertyName = "SessionEventsHookStartData")]
[JsonSerializable(typeof(GitHub.Copilot.HookStartEvent), TypeInfoPropertyName = "SessionEventsHookStartEvent")]
[JsonSerializable(typeof(GitHub.Copilot.McpAppToolCallCompleteData), TypeInfoPropertyName = "SessionEventsMcpAppToolCallCompleteData")]
[JsonSerializable(typeof(GitHub.Copilot.McpAppToolCallCompleteError), TypeInfoPropertyName = "SessionEventsMcpAppToolCallCompleteError")]
[JsonSerializable(typeof(GitHub.Copilot.McpAppToolCallCompleteEvent), TypeInfoPropertyName = "SessionEventsMcpAppToolCallCompleteEvent")]
[JsonSerializable(typeof(GitHub.Copilot.McpAppToolCallCompleteToolMeta), TypeInfoPropertyName = "SessionEventsMcpAppToolCallCompleteToolMeta")]
[JsonSerializable(typeof(GitHub.Copilot.McpAppToolCallCompleteToolMetaUI), TypeInfoPropertyName = "SessionEventsMcpAppToolCallCompleteToolMetaUI")]
[JsonSerializable(typeof(GitHub.Copilot.McpOauthCompletedData), TypeInfoPropertyName = "SessionEventsMcpOauthCompletedData")]
[JsonSerializable(typeof(GitHub.Copilot.McpOauthCompletedEvent), TypeInfoPropertyName = "SessionEventsMcpOauthCompletedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.McpOauthRequiredData), TypeInfoPropertyName = "SessionEventsMcpOauthRequiredData")]
[JsonSerializable(typeof(GitHub.Copilot.McpOauthRequiredEvent), TypeInfoPropertyName = "SessionEventsMcpOauthRequiredEvent")]
[JsonSerializable(typeof(GitHub.Copilot.McpOauthRequiredStaticClientConfig), TypeInfoPropertyName = "SessionEventsMcpOauthRequiredStaticClientConfig")]
[JsonSerializable(typeof(GitHub.Copilot.McpServerSource), TypeInfoPropertyName = "SessionEventsMcpServerSource")]
[JsonSerializable(typeof(GitHub.Copilot.McpServerStatus), TypeInfoPropertyName = "SessionEventsMcpServerStatus")]
[JsonSerializable(typeof(GitHub.Copilot.McpServerTransport), TypeInfoPropertyName = "SessionEventsMcpServerTransport")]
[JsonSerializable(typeof(GitHub.Copilot.McpServersLoadedServer), TypeInfoPropertyName = "SessionEventsMcpServersLoadedServer")]
[JsonSerializable(typeof(GitHub.Copilot.ModelCallFailureData), TypeInfoPropertyName = "SessionEventsModelCallFailureData")]
[JsonSerializable(typeof(GitHub.Copilot.ModelCallFailureEvent), TypeInfoPropertyName = "SessionEventsModelCallFailureEvent")]
[JsonSerializable(typeof(GitHub.Copilot.ModelCallFailureSource), TypeInfoPropertyName = "SessionEventsModelCallFailureSource")]
[JsonSerializable(typeof(GitHub.Copilot.PendingMessagesModifiedData), TypeInfoPropertyName = "SessionEventsPendingMessagesModifiedData")]
[JsonSerializable(typeof(GitHub.Copilot.PendingMessagesModifiedEvent), TypeInfoPropertyName = "SessionEventsPendingMessagesModifiedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionCompletedData), TypeInfoPropertyName = "SessionEventsPermissionCompletedData")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionCompletedEvent), TypeInfoPropertyName = "SessionEventsPermissionCompletedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionPromptRequest), TypeInfoPropertyName = "SessionEventsPermissionPromptRequest")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionPromptRequestCommands), TypeInfoPropertyName = "SessionEventsPermissionPromptRequestCommands")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionPromptRequestCustomTool), TypeInfoPropertyName = "SessionEventsPermissionPromptRequestCustomTool")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionPromptRequestExtensionManagement), TypeInfoPropertyName = "SessionEventsPermissionPromptRequestExtensionManagement")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionPromptRequestExtensionPermissionAccess), TypeInfoPropertyName = "SessionEventsPermissionPromptRequestExtensionPermissionAccess")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionPromptRequestHook), TypeInfoPropertyName = "SessionEventsPermissionPromptRequestHook")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionPromptRequestMcp), TypeInfoPropertyName = "SessionEventsPermissionPromptRequestMcp")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionPromptRequestMemory), TypeInfoPropertyName = "SessionEventsPermissionPromptRequestMemory")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionPromptRequestPath), TypeInfoPropertyName = "SessionEventsPermissionPromptRequestPath")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionPromptRequestPathAccessKind), TypeInfoPropertyName = "SessionEventsPermissionPromptRequestPathAccessKind")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionPromptRequestRead), TypeInfoPropertyName = "SessionEventsPermissionPromptRequestRead")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionPromptRequestUrl), TypeInfoPropertyName = "SessionEventsPermissionPromptRequestUrl")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionPromptRequestWrite), TypeInfoPropertyName = "SessionEventsPermissionPromptRequestWrite")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequest), TypeInfoPropertyName = "SessionEventsPermissionRequest")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestCustomTool), TypeInfoPropertyName = "SessionEventsPermissionRequestCustomTool")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestExtensionManagement), TypeInfoPropertyName = "SessionEventsPermissionRequestExtensionManagement")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestExtensionPermissionAccess), TypeInfoPropertyName = "SessionEventsPermissionRequestExtensionPermissionAccess")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestHook), TypeInfoPropertyName = "SessionEventsPermissionRequestHook")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestMcp), TypeInfoPropertyName = "SessionEventsPermissionRequestMcp")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestMemory), TypeInfoPropertyName = "SessionEventsPermissionRequestMemory")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestMemoryAction), TypeInfoPropertyName = "SessionEventsPermissionRequestMemoryAction")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestMemoryDirection), TypeInfoPropertyName = "SessionEventsPermissionRequestMemoryDirection")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestRead), TypeInfoPropertyName = "SessionEventsPermissionRequestRead")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestShell), TypeInfoPropertyName = "SessionEventsPermissionRequestShell")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestShellCommand), TypeInfoPropertyName = "SessionEventsPermissionRequestShellCommand")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestShellPossibleUrl), TypeInfoPropertyName = "SessionEventsPermissionRequestShellPossibleUrl")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestUrl), TypeInfoPropertyName = "SessionEventsPermissionRequestUrl")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestWrite), TypeInfoPropertyName = "SessionEventsPermissionRequestWrite")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestedData), TypeInfoPropertyName = "SessionEventsPermissionRequestedData")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRequestedEvent), TypeInfoPropertyName = "SessionEventsPermissionRequestedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionResult), TypeInfoPropertyName = "SessionEventsPermissionResult")]
[JsonSerializable(typeof(GitHub.Copilot.PermissionRule), TypeInfoPropertyName = "SessionEventsPermissionRule")]
[JsonSerializable(typeof(GitHub.Copilot.PlanChangedOperation), TypeInfoPropertyName = "SessionEventsPlanChangedOperation")]
[JsonSerializable(typeof(GitHub.Copilot.ReasoningSummary), TypeInfoPropertyName = "SessionEventsReasoningSummary")]
[JsonSerializable(typeof(GitHub.Copilot.SamplingCompletedData), TypeInfoPropertyName = "SessionEventsSamplingCompletedData")]
[JsonSerializable(typeof(GitHub.Copilot.SamplingCompletedEvent), TypeInfoPropertyName = "SessionEventsSamplingCompletedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.SamplingRequestedData), TypeInfoPropertyName = "SessionEventsSamplingRequestedData")]
[JsonSerializable(typeof(GitHub.Copilot.SamplingRequestedEvent), TypeInfoPropertyName = "SessionEventsSamplingRequestedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.SessionEvent), TypeInfoPropertyName = "SessionEventsSessionEvent")]
[JsonSerializable(typeof(GitHub.Copilot.SessionMode), TypeInfoPropertyName = "SessionEventsSessionMode")]
[JsonSerializable(typeof(GitHub.Copilot.ShutdownCodeChanges), TypeInfoPropertyName = "SessionEventsShutdownCodeChanges")]
[JsonSerializable(typeof(GitHub.Copilot.ShutdownModelMetric), TypeInfoPropertyName = "SessionEventsShutdownModelMetric")]
[JsonSerializable(typeof(GitHub.Copilot.ShutdownModelMetricRequests), TypeInfoPropertyName = "SessionEventsShutdownModelMetricRequests")]
[JsonSerializable(typeof(GitHub.Copilot.ShutdownModelMetricTokenDetail), TypeInfoPropertyName = "SessionEventsShutdownModelMetricTokenDetail")]
[JsonSerializable(typeof(GitHub.Copilot.ShutdownModelMetricUsage), TypeInfoPropertyName = "SessionEventsShutdownModelMetricUsage")]
[JsonSerializable(typeof(GitHub.Copilot.ShutdownTokenDetail), TypeInfoPropertyName = "SessionEventsShutdownTokenDetail")]
[JsonSerializable(typeof(GitHub.Copilot.ShutdownType), TypeInfoPropertyName = "SessionEventsShutdownType")]
[JsonSerializable(typeof(GitHub.Copilot.SkillInvokedData), TypeInfoPropertyName = "SessionEventsSkillInvokedData")]
[JsonSerializable(typeof(GitHub.Copilot.SkillInvokedEvent), TypeInfoPropertyName = "SessionEventsSkillInvokedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.SkillInvokedTrigger), TypeInfoPropertyName = "SessionEventsSkillInvokedTrigger")]
[JsonSerializable(typeof(GitHub.Copilot.SkillSource), TypeInfoPropertyName = "SessionEventsSkillSource")]
[JsonSerializable(typeof(GitHub.Copilot.SkillsLoadedSkill), TypeInfoPropertyName = "SessionEventsSkillsLoadedSkill")]
[JsonSerializable(typeof(GitHub.Copilot.SubagentCompletedData), TypeInfoPropertyName = "SessionEventsSubagentCompletedData")]
[JsonSerializable(typeof(GitHub.Copilot.SubagentCompletedEvent), TypeInfoPropertyName = "SessionEventsSubagentCompletedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.SubagentDeselectedData), TypeInfoPropertyName = "SessionEventsSubagentDeselectedData")]
[JsonSerializable(typeof(GitHub.Copilot.SubagentDeselectedEvent), TypeInfoPropertyName = "SessionEventsSubagentDeselectedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.SubagentFailedData), TypeInfoPropertyName = "SessionEventsSubagentFailedData")]
[JsonSerializable(typeof(GitHub.Copilot.SubagentFailedEvent), TypeInfoPropertyName = "SessionEventsSubagentFailedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.SubagentSelectedData), TypeInfoPropertyName = "SessionEventsSubagentSelectedData")]
[JsonSerializable(typeof(GitHub.Copilot.SubagentSelectedEvent), TypeInfoPropertyName = "SessionEventsSubagentSelectedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.SubagentStartedData), TypeInfoPropertyName = "SessionEventsSubagentStartedData")]
[JsonSerializable(typeof(GitHub.Copilot.SubagentStartedEvent), TypeInfoPropertyName = "SessionEventsSubagentStartedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.SystemMessageData), TypeInfoPropertyName = "SessionEventsSystemMessageData")]
[JsonSerializable(typeof(GitHub.Copilot.SystemMessageEvent), TypeInfoPropertyName = "SessionEventsSystemMessageEvent")]
[JsonSerializable(typeof(GitHub.Copilot.SystemMessageMetadata), TypeInfoPropertyName = "SessionEventsSystemMessageMetadata")]
[JsonSerializable(typeof(GitHub.Copilot.SystemMessageRole), TypeInfoPropertyName = "SessionEventsSystemMessageRole")]
[JsonSerializable(typeof(GitHub.Copilot.SystemNotification), TypeInfoPropertyName = "SessionEventsSystemNotification")]
[JsonSerializable(typeof(GitHub.Copilot.SystemNotificationAgentCompleted), TypeInfoPropertyName = "SessionEventsSystemNotificationAgentCompleted")]
[JsonSerializable(typeof(GitHub.Copilot.SystemNotificationAgentCompletedStatus), TypeInfoPropertyName = "SessionEventsSystemNotificationAgentCompletedStatus")]
[JsonSerializable(typeof(GitHub.Copilot.SystemNotificationAgentIdle), TypeInfoPropertyName = "SessionEventsSystemNotificationAgentIdle")]
[JsonSerializable(typeof(GitHub.Copilot.SystemNotificationData), TypeInfoPropertyName = "SessionEventsSystemNotificationData")]
[JsonSerializable(typeof(GitHub.Copilot.SystemNotificationEvent), TypeInfoPropertyName = "SessionEventsSystemNotificationEvent")]
[JsonSerializable(typeof(GitHub.Copilot.SystemNotificationInstructionDiscovered), TypeInfoPropertyName = "SessionEventsSystemNotificationInstructionDiscovered")]
[JsonSerializable(typeof(GitHub.Copilot.SystemNotificationNewInboxMessage), TypeInfoPropertyName = "SessionEventsSystemNotificationNewInboxMessage")]
[JsonSerializable(typeof(GitHub.Copilot.SystemNotificationShellCompleted), TypeInfoPropertyName = "SessionEventsSystemNotificationShellCompleted")]
[JsonSerializable(typeof(GitHub.Copilot.SystemNotificationShellDetachedCompleted), TypeInfoPropertyName = "SessionEventsSystemNotificationShellDetachedCompleted")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteContent), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteContent")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteContentAudio), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteContentAudio")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteContentImage), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteContentImage")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteContentResource), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteContentResource")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteContentResourceDetails), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteContentResourceDetails")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteContentResourceLink), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteContentResourceLink")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteContentResourceLinkIcon), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteContentResourceLinkIcon")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteContentResourceLinkIconTheme), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteContentResourceLinkIconTheme")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteContentTerminal), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteContentTerminal")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteContentText), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteContentText")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteData), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteData")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteError), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteError")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteEvent), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteEvent")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteResult), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteResult")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteToolDescription), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteToolDescription")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteToolDescriptionMeta), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteToolDescriptionMeta")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteToolDescriptionMetaUI), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteToolDescriptionMetaUI")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteToolDescriptionMetaUIVisibility), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteToolDescriptionMetaUIVisibility")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteUIResource), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteUIResource")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteUIResourceMeta), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteUIResourceMeta")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteUIResourceMetaUI), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteUIResourceMetaUI")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteUIResourceMetaUICsp), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteUIResourceMetaUICsp")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteUIResourceMetaUIPermissions), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteUIResourceMetaUIPermissions")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteUIResourceMetaUIPermissionsCamera), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteUIResourceMetaUIPermissionsCamera")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteUIResourceMetaUIPermissionsClipboardWrite")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteUIResourceMetaUIPermissionsGeolocation")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone), TypeInfoPropertyName = "SessionEventsToolExecutionCompleteUIResourceMetaUIPermissionsMicrophone")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionPartialResultEvent), TypeInfoPropertyName = "SessionEventsToolExecutionPartialResultEvent")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionProgressData), TypeInfoPropertyName = "SessionEventsToolExecutionProgressData")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionProgressEvent), TypeInfoPropertyName = "SessionEventsToolExecutionProgressEvent")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionStartData), TypeInfoPropertyName = "SessionEventsToolExecutionStartData")]
[JsonSerializable(typeof(GitHub.Copilot.ToolExecutionStartEvent), TypeInfoPropertyName = "SessionEventsToolExecutionStartEvent")]
[JsonSerializable(typeof(GitHub.Copilot.ToolUserRequestedData), TypeInfoPropertyName = "SessionEventsToolUserRequestedData")]
[JsonSerializable(typeof(GitHub.Copilot.ToolUserRequestedEvent), TypeInfoPropertyName = "SessionEventsToolUserRequestedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.UserInputCompletedData), TypeInfoPropertyName = "SessionEventsUserInputCompletedData")]
[JsonSerializable(typeof(GitHub.Copilot.UserInputCompletedEvent), TypeInfoPropertyName = "SessionEventsUserInputCompletedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.UserInputRequestedData), TypeInfoPropertyName = "SessionEventsUserInputRequestedData")]
[JsonSerializable(typeof(GitHub.Copilot.UserInputRequestedEvent), TypeInfoPropertyName = "SessionEventsUserInputRequestedEvent")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageAgentMode), TypeInfoPropertyName = "SessionEventsUserMessageAgentMode")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageAttachment), TypeInfoPropertyName = "SessionEventsUserMessageAttachment")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageAttachmentBlob), TypeInfoPropertyName = "SessionEventsUserMessageAttachmentBlob")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageAttachmentDirectory), TypeInfoPropertyName = "SessionEventsUserMessageAttachmentDirectory")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageAttachmentFile), TypeInfoPropertyName = "SessionEventsUserMessageAttachmentFile")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageAttachmentFileLineRange), TypeInfoPropertyName = "SessionEventsUserMessageAttachmentFileLineRange")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageAttachmentGithubReference), TypeInfoPropertyName = "SessionEventsUserMessageAttachmentGithubReference")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageAttachmentGithubReferenceType), TypeInfoPropertyName = "SessionEventsUserMessageAttachmentGithubReferenceType")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageAttachmentSelection), TypeInfoPropertyName = "SessionEventsUserMessageAttachmentSelection")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageAttachmentSelectionDetails), TypeInfoPropertyName = "SessionEventsUserMessageAttachmentSelectionDetails")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageAttachmentSelectionDetailsEnd), TypeInfoPropertyName = "SessionEventsUserMessageAttachmentSelectionDetailsEnd")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageAttachmentSelectionDetailsStart), TypeInfoPropertyName = "SessionEventsUserMessageAttachmentSelectionDetailsStart")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageData), TypeInfoPropertyName = "SessionEventsUserMessageData")]
[JsonSerializable(typeof(GitHub.Copilot.UserMessageEvent), TypeInfoPropertyName = "SessionEventsUserMessageEvent")]
[JsonSerializable(typeof(GitHub.Copilot.UserToolSessionApproval), TypeInfoPropertyName = "SessionEventsUserToolSessionApproval")]
[JsonSerializable(typeof(GitHub.Copilot.UserToolSessionApprovalCommands), TypeInfoPropertyName = "SessionEventsUserToolSessionApprovalCommands")]
[JsonSerializable(typeof(GitHub.Copilot.UserToolSessionApprovalCustomTool), TypeInfoPropertyName = "SessionEventsUserToolSessionApprovalCustomTool")]
[JsonSerializable(typeof(GitHub.Copilot.UserToolSessionApprovalExtensionManagement), TypeInfoPropertyName = "SessionEventsUserToolSessionApprovalExtensionManagement")]
[JsonSerializable(typeof(GitHub.Copilot.UserToolSessionApprovalExtensionPermissionAccess), TypeInfoPropertyName = "SessionEventsUserToolSessionApprovalExtensionPermissionAccess")]
[JsonSerializable(typeof(GitHub.Copilot.UserToolSessionApprovalMcp), TypeInfoPropertyName = "SessionEventsUserToolSessionApprovalMcp")]
[JsonSerializable(typeof(GitHub.Copilot.UserToolSessionApprovalMemory), TypeInfoPropertyName = "SessionEventsUserToolSessionApprovalMemory")]
[JsonSerializable(typeof(GitHub.Copilot.UserToolSessionApprovalRead), TypeInfoPropertyName = "SessionEventsUserToolSessionApprovalRead")]
[JsonSerializable(typeof(GitHub.Copilot.UserToolSessionApprovalWrite), TypeInfoPropertyName = "SessionEventsUserToolSessionApprovalWrite")]
[JsonSerializable(typeof(GitHub.Copilot.WorkingDirectoryContext), TypeInfoPropertyName = "SessionEventsWorkingDirectoryContext")]
[JsonSerializable(typeof(GitHub.Copilot.WorkingDirectoryContextHostType), TypeInfoPropertyName = "SessionEventsWorkingDirectoryContextHostType")]
[JsonSerializable(typeof(GitHub.Copilot.WorkspaceFileChangedOperation), TypeInfoPropertyName = "SessionEventsWorkspaceFileChangedOperation")]
[JsonSerializable(typeof(AbortRequest))]
[JsonSerializable(typeof(AbortResult))]
[JsonSerializable(typeof(AccountGetQuotaRequest))]
[JsonSerializable(typeof(AccountGetQuotaResult))]
[JsonSerializable(typeof(AccountQuotaSnapshot))]
[JsonSerializable(typeof(AgentGetCurrentResult))]
[JsonSerializable(typeof(AgentInfo))]
[JsonSerializable(typeof(AgentList))]
[JsonSerializable(typeof(AgentReloadResult))]
[JsonSerializable(typeof(AgentSelectRequest))]
[JsonSerializable(typeof(AgentSelectResult))]
[JsonSerializable(typeof(AuthInfo))]
[JsonSerializable(typeof(CanvasAction))]
[JsonSerializable(typeof(CanvasCloseRequest))]
[JsonSerializable(typeof(CanvasHostContext))]
[JsonSerializable(typeof(CanvasHostContextCapabilities))]
[JsonSerializable(typeof(CanvasInvokeActionRequest))]
[JsonSerializable(typeof(CanvasInvokeActionResult))]
[JsonSerializable(typeof(CanvasList))]
[JsonSerializable(typeof(CanvasListOpenResult))]
[JsonSerializable(typeof(CanvasOpenRequest))]
[JsonSerializable(typeof(CanvasProviderCloseRequest))]
[JsonSerializable(typeof(CanvasProviderInvokeActionRequest))]
[JsonSerializable(typeof(CanvasProviderOpenRequest))]
[JsonSerializable(typeof(CanvasProviderOpenResult))]
[JsonSerializable(typeof(CommandList))]
[JsonSerializable(typeof(CommandsHandlePendingCommandRequest))]
[JsonSerializable(typeof(CommandsHandlePendingCommandResult))]
[JsonSerializable(typeof(CommandsInvokeRequest))]
[JsonSerializable(typeof(CommandsListRequest))]
[JsonSerializable(typeof(CommandsListRequestWithSession))]
[JsonSerializable(typeof(CommandsRespondToQueuedCommandRequest))]
[JsonSerializable(typeof(CommandsRespondToQueuedCommandResult))]
[JsonSerializable(typeof(ConnectRemoteSessionParams))]
[JsonSerializable(typeof(ConnectRequest))]
[JsonSerializable(typeof(ConnectResult))]
[JsonSerializable(typeof(ConnectedRemoteSessionMetadata))]
[JsonSerializable(typeof(ConnectedRemoteSessionMetadataRepository))]
[JsonSerializable(typeof(CopilotUserResponse))]
[JsonSerializable(typeof(CopilotUserResponseEndpoints))]
[JsonSerializable(typeof(CopilotUserResponseOrganizationListItem))]
[JsonSerializable(typeof(CopilotUserResponseQuotaSnapshots))]
[JsonSerializable(typeof(CopilotUserResponseQuotaSnapshotsChat))]
[JsonSerializable(typeof(CopilotUserResponseQuotaSnapshotsCompletions))]
[JsonSerializable(typeof(CopilotUserResponseQuotaSnapshotsPremiumInteractions))]
[JsonSerializable(typeof(CurrentModel))]
[JsonSerializable(typeof(DiscoveredCanvas))]
[JsonSerializable(typeof(DiscoveredMcpServer))]
[JsonSerializable(typeof(EnqueueCommandParams))]
[JsonSerializable(typeof(EnqueueCommandResult))]
[JsonSerializable(typeof(EventLogReadRequest))]
[JsonSerializable(typeof(EventLogReleaseInterestResult))]
[JsonSerializable(typeof(EventLogTailResult))]
[JsonSerializable(typeof(EventsReadResult))]
[JsonSerializable(typeof(ExecuteCommandParams))]
[JsonSerializable(typeof(ExecuteCommandResult))]
[JsonSerializable(typeof(Extension))]
[JsonSerializable(typeof(ExtensionList))]
[JsonSerializable(typeof(ExtensionsDisableRequest))]
[JsonSerializable(typeof(ExtensionsEnableRequest))]
[JsonSerializable(typeof(FleetStartRequest))]
[JsonSerializable(typeof(FleetStartResult))]
[JsonSerializable(typeof(FolderTrustAddParams))]
[JsonSerializable(typeof(FolderTrustCheckParams))]
[JsonSerializable(typeof(FolderTrustCheckResult))]
[JsonSerializable(typeof(HandlePendingToolCallRequest))]
[JsonSerializable(typeof(HandlePendingToolCallResult))]
[JsonSerializable(typeof(HistoryAbortManualCompactionResult))]
[JsonSerializable(typeof(HistoryCancelBackgroundCompactionResult))]
[JsonSerializable(typeof(HistoryCompactContextWindow))]
[JsonSerializable(typeof(HistoryCompactRequest))]
[JsonSerializable(typeof(HistoryCompactRequestWithSession))]
[JsonSerializable(typeof(HistoryCompactResult))]
[JsonSerializable(typeof(HistorySummarizeForHandoffResult))]
[JsonSerializable(typeof(HistoryTruncateRequest))]
[JsonSerializable(typeof(HistoryTruncateResult))]
[JsonSerializable(typeof(InstalledPlugin))]
[JsonSerializable(typeof(InstructionsGetSourcesResult))]
[JsonSerializable(typeof(InstructionsSources))]
[JsonSerializable(typeof(LogRequest))]
[JsonSerializable(typeof(LogResult))]
[JsonSerializable(typeof(LspInitializeRequest))]
[JsonSerializable(typeof(McpAppsCallToolRequest))]
[JsonSerializable(typeof(McpAppsDiagnoseCapability))]
[JsonSerializable(typeof(McpAppsDiagnoseRequest))]
[JsonSerializable(typeof(McpAppsDiagnoseResult))]
[JsonSerializable(typeof(McpAppsDiagnoseServer))]
[JsonSerializable(typeof(McpAppsHostContext))]
[JsonSerializable(typeof(McpAppsHostContextDetails))]
[JsonSerializable(typeof(McpAppsListToolsRequest))]
[JsonSerializable(typeof(McpAppsListToolsResult))]
[JsonSerializable(typeof(McpAppsReadResourceRequest))]
[JsonSerializable(typeof(McpAppsReadResourceResult))]
[JsonSerializable(typeof(McpAppsResourceContent))]
[JsonSerializable(typeof(McpAppsSetHostContextDetails))]
[JsonSerializable(typeof(McpAppsSetHostContextRequest))]
[JsonSerializable(typeof(McpCancelSamplingExecutionParams))]
[JsonSerializable(typeof(McpCancelSamplingExecutionResult))]
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
[JsonSerializable(typeof(McpExecuteSamplingParams))]
[JsonSerializable(typeof(McpExecuteSamplingRequest))]
[JsonSerializable(typeof(McpExecuteSamplingResult))]
[JsonSerializable(typeof(McpOauthLoginRequest))]
[JsonSerializable(typeof(McpOauthLoginResult))]
[JsonSerializable(typeof(McpRemoveGitHubResult))]
[JsonSerializable(typeof(McpSamplingExecutionResult))]
[JsonSerializable(typeof(McpServer))]
[JsonSerializable(typeof(McpServerList))]
[JsonSerializable(typeof(McpSetEnvValueModeParams))]
[JsonSerializable(typeof(McpSetEnvValueModeResult))]
[JsonSerializable(typeof(MetadataContextInfoRequest))]
[JsonSerializable(typeof(MetadataContextInfoResult))]
[JsonSerializable(typeof(MetadataContextInfoResultContextInfo))]
[JsonSerializable(typeof(MetadataIsProcessingResult))]
[JsonSerializable(typeof(MetadataRecomputeContextTokensRequest))]
[JsonSerializable(typeof(MetadataRecomputeContextTokensResult))]
[JsonSerializable(typeof(MetadataRecordContextChangeRequest))]
[JsonSerializable(typeof(MetadataRecordContextChangeResult))]
[JsonSerializable(typeof(MetadataSetWorkingDirectoryRequest))]
[JsonSerializable(typeof(MetadataSetWorkingDirectoryResult))]
[JsonSerializable(typeof(MetadataSnapshotRemoteMetadata))]
[JsonSerializable(typeof(MetadataSnapshotRemoteMetadataRepository))]
[JsonSerializable(typeof(ModeSetRequest))]
[JsonSerializable(typeof(Model))]
[JsonSerializable(typeof(ModelBilling))]
[JsonSerializable(typeof(ModelBillingTokenPrices))]
[JsonSerializable(typeof(ModelBillingTokenPricesLongContext))]
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
[JsonSerializable(typeof(ModelSetReasoningEffortRequest))]
[JsonSerializable(typeof(ModelSetReasoningEffortResult))]
[JsonSerializable(typeof(ModelSwitchToRequest))]
[JsonSerializable(typeof(ModelSwitchToResult))]
[JsonSerializable(typeof(ModelsListRequest))]
[JsonSerializable(typeof(NameGetResult))]
[JsonSerializable(typeof(NameSetAutoRequest))]
[JsonSerializable(typeof(NameSetAutoResult))]
[JsonSerializable(typeof(NameSetRequest))]
[JsonSerializable(typeof(OpenCanvasInstance))]
[JsonSerializable(typeof(PendingPermissionRequest))]
[JsonSerializable(typeof(PendingPermissionRequestList))]
[JsonSerializable(typeof(PermissionDecision))]
[JsonSerializable(typeof(PermissionDecisionApproveForLocationApproval))]
[JsonSerializable(typeof(PermissionDecisionApproveForSessionApproval))]
[JsonSerializable(typeof(PermissionDecisionRequest))]
[JsonSerializable(typeof(PermissionLocationAddToolApprovalParams))]
[JsonSerializable(typeof(PermissionLocationApplyParams))]
[JsonSerializable(typeof(PermissionLocationApplyResult))]
[JsonSerializable(typeof(PermissionLocationResolveParams))]
[JsonSerializable(typeof(PermissionLocationResolveResult))]
[JsonSerializable(typeof(PermissionPathsAddParams))]
[JsonSerializable(typeof(PermissionPathsAllowedCheckParams))]
[JsonSerializable(typeof(PermissionPathsAllowedCheckResult))]
[JsonSerializable(typeof(PermissionPathsConfig))]
[JsonSerializable(typeof(PermissionPathsList))]
[JsonSerializable(typeof(PermissionPathsUpdatePrimaryParams))]
[JsonSerializable(typeof(PermissionPathsWorkspaceCheckParams))]
[JsonSerializable(typeof(PermissionPathsWorkspaceCheckResult))]
[JsonSerializable(typeof(PermissionPromptShownNotification))]
[JsonSerializable(typeof(PermissionRequestResult))]
[JsonSerializable(typeof(PermissionRulesSet))]
[JsonSerializable(typeof(PermissionUrlsConfig))]
[JsonSerializable(typeof(PermissionUrlsSetUnrestrictedModeParams))]
[JsonSerializable(typeof(PermissionsConfigureAdditionalContentExclusionPolicy))]
[JsonSerializable(typeof(PermissionsConfigureAdditionalContentExclusionPolicyRule))]
[JsonSerializable(typeof(PermissionsConfigureAdditionalContentExclusionPolicyRuleSource))]
[JsonSerializable(typeof(PermissionsConfigureParams))]
[JsonSerializable(typeof(PermissionsConfigureResult))]
[JsonSerializable(typeof(PermissionsFolderTrustAddTrustedResult))]
[JsonSerializable(typeof(PermissionsLocationsAddToolApprovalDetails))]
[JsonSerializable(typeof(PermissionsLocationsAddToolApprovalResult))]
[JsonSerializable(typeof(PermissionsModifyRulesParams))]
[JsonSerializable(typeof(PermissionsModifyRulesResult))]
[JsonSerializable(typeof(PermissionsNotifyPromptShownResult))]
[JsonSerializable(typeof(PermissionsPathsAddResult))]
[JsonSerializable(typeof(PermissionsPathsListRequest))]
[JsonSerializable(typeof(PermissionsPathsUpdatePrimaryResult))]
[JsonSerializable(typeof(PermissionsPendingRequestsRequest))]
[JsonSerializable(typeof(PermissionsResetSessionApprovalsRequest))]
[JsonSerializable(typeof(PermissionsResetSessionApprovalsResult))]
[JsonSerializable(typeof(PermissionsSetApproveAllRequest))]
[JsonSerializable(typeof(PermissionsSetApproveAllResult))]
[JsonSerializable(typeof(PermissionsSetRequiredRequest))]
[JsonSerializable(typeof(PermissionsSetRequiredResult))]
[JsonSerializable(typeof(PermissionsUrlsSetUnrestrictedModeResult))]
[JsonSerializable(typeof(PingRequest))]
[JsonSerializable(typeof(PingResult))]
[JsonSerializable(typeof(PlanReadResult))]
[JsonSerializable(typeof(PlanUpdateRequest))]
[JsonSerializable(typeof(Plugin))]
[JsonSerializable(typeof(PluginList))]
[JsonSerializable(typeof(QueuePendingItems))]
[JsonSerializable(typeof(QueuePendingItemsResult))]
[JsonSerializable(typeof(QueueRemoveMostRecentResult))]
[JsonSerializable(typeof(QueuedCommandResult))]
[JsonSerializable(typeof(RegisterEventInterestParams))]
[JsonSerializable(typeof(RegisterEventInterestResult))]
[JsonSerializable(typeof(ReleaseEventInterestParams))]
[JsonSerializable(typeof(RemoteEnableRequest))]
[JsonSerializable(typeof(RemoteEnableResult))]
[JsonSerializable(typeof(RemoteNotifySteerableChangedRequest))]
[JsonSerializable(typeof(RemoteNotifySteerableChangedResult))]
[JsonSerializable(typeof(RemoteSessionConnectionResult))]
[JsonSerializable(typeof(ScheduleEntry))]
[JsonSerializable(typeof(ScheduleList))]
[JsonSerializable(typeof(ScheduleStopRequest))]
[JsonSerializable(typeof(ScheduleStopResult))]
[JsonSerializable(typeof(SecretsAddFilterValuesRequest))]
[JsonSerializable(typeof(SecretsAddFilterValuesResult))]
[JsonSerializable(typeof(SendAttachment))]
[JsonSerializable(typeof(SendAttachmentFileLineRange))]
[JsonSerializable(typeof(SendAttachmentSelectionDetails))]
[JsonSerializable(typeof(SendAttachmentSelectionDetailsEnd))]
[JsonSerializable(typeof(SendAttachmentSelectionDetailsStart))]
[JsonSerializable(typeof(SendRequest))]
[JsonSerializable(typeof(SendResult))]
[JsonSerializable(typeof(ServerSkill))]
[JsonSerializable(typeof(ServerSkillList))]
[JsonSerializable(typeof(SessionAgentDeselectRequest))]
[JsonSerializable(typeof(SessionAgentGetCurrentRequest))]
[JsonSerializable(typeof(SessionAgentListRequest))]
[JsonSerializable(typeof(SessionAgentReloadRequest))]
[JsonSerializable(typeof(SessionAuthGetStatusRequest))]
[JsonSerializable(typeof(SessionAuthStatus))]
[JsonSerializable(typeof(SessionBulkDeleteResult))]
[JsonSerializable(typeof(SessionCanvasListOpenRequest))]
[JsonSerializable(typeof(SessionCanvasListRequest))]
[JsonSerializable(typeof(SessionContext))]
[JsonSerializable(typeof(SessionEnrichMetadataResult))]
[JsonSerializable(typeof(SessionEventLogTailRequest))]
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
[JsonSerializable(typeof(SessionFsSetProviderCapabilities))]
[JsonSerializable(typeof(SessionFsSetProviderRequest))]
[JsonSerializable(typeof(SessionFsSetProviderResult))]
[JsonSerializable(typeof(SessionFsSqliteExistsRequest))]
[JsonSerializable(typeof(SessionFsSqliteExistsResult))]
[JsonSerializable(typeof(SessionFsSqliteQueryRequest))]
[JsonSerializable(typeof(SessionFsSqliteQueryResult))]
[JsonSerializable(typeof(SessionFsStatRequest))]
[JsonSerializable(typeof(SessionFsStatResult))]
[JsonSerializable(typeof(SessionFsWriteFileRequest))]
[JsonSerializable(typeof(SessionHistoryAbortManualCompactionRequest))]
[JsonSerializable(typeof(SessionHistoryCancelBackgroundCompactionRequest))]
[JsonSerializable(typeof(SessionHistorySummarizeForHandoffRequest))]
[JsonSerializable(typeof(SessionInstalledPlugin))]
[JsonSerializable(typeof(SessionInstructionsGetSourcesRequest))]
[JsonSerializable(typeof(SessionList))]
[JsonSerializable(typeof(SessionListFilter))]
[JsonSerializable(typeof(SessionLoadDeferredRepoHooksResult))]
[JsonSerializable(typeof(SessionMcpAppsGetHostContextRequest))]
[JsonSerializable(typeof(SessionMcpListRequest))]
[JsonSerializable(typeof(SessionMcpReloadRequest))]
[JsonSerializable(typeof(SessionMcpRemoveGitHubRequest))]
[JsonSerializable(typeof(SessionMetadata))]
[JsonSerializable(typeof(SessionMetadataIsProcessingRequest))]
[JsonSerializable(typeof(SessionMetadataSnapshot))]
[JsonSerializable(typeof(SessionMetadataSnapshotRequest))]
[JsonSerializable(typeof(SessionMetadataSnapshotWorkspace))]
[JsonSerializable(typeof(SessionModeGetRequest))]
[JsonSerializable(typeof(SessionModelGetCurrentRequest))]
[JsonSerializable(typeof(SessionNameGetRequest))]
[JsonSerializable(typeof(SessionPlanDeleteRequest))]
[JsonSerializable(typeof(SessionPlanReadRequest))]
[JsonSerializable(typeof(SessionPluginsListRequest))]
[JsonSerializable(typeof(SessionPruneResult))]
[JsonSerializable(typeof(SessionQueueClearRequest))]
[JsonSerializable(typeof(SessionQueuePendingItemsRequest))]
[JsonSerializable(typeof(SessionQueueRemoveMostRecentRequest))]
[JsonSerializable(typeof(SessionRemoteDisableRequest))]
[JsonSerializable(typeof(SessionScheduleListRequest))]
[JsonSerializable(typeof(SessionSetCredentialsParams))]
[JsonSerializable(typeof(SessionSetCredentialsResult))]
[JsonSerializable(typeof(SessionSizes))]
[JsonSerializable(typeof(SessionSkillsEnsureLoadedRequest))]
[JsonSerializable(typeof(SessionSkillsGetInvokedRequest))]
[JsonSerializable(typeof(SessionSkillsListRequest))]
[JsonSerializable(typeof(SessionSkillsReloadRequest))]
[JsonSerializable(typeof(SessionSuspendRequest))]
[JsonSerializable(typeof(SessionTasksGetCurrentPromotableRequest))]
[JsonSerializable(typeof(SessionTasksListRequest))]
[JsonSerializable(typeof(SessionTasksPromoteCurrentToBackgroundRequest))]
[JsonSerializable(typeof(SessionTasksRefreshRequest))]
[JsonSerializable(typeof(SessionTasksWaitForPendingRequest))]
[JsonSerializable(typeof(SessionToolsInitializeAndValidateRequest))]
[JsonSerializable(typeof(SessionUiRegisterDirectAutoModeSwitchHandlerRequest))]
[JsonSerializable(typeof(SessionUpdateOptionsParams))]
[JsonSerializable(typeof(SessionUpdateOptionsResult))]
[JsonSerializable(typeof(SessionUsageGetMetricsRequest))]
[JsonSerializable(typeof(SessionWorkingDirectoryContext))]
[JsonSerializable(typeof(SessionWorkspacesGetWorkspaceRequest))]
[JsonSerializable(typeof(SessionWorkspacesListCheckpointsRequest))]
[JsonSerializable(typeof(SessionWorkspacesListFilesRequest))]
[JsonSerializable(typeof(SessionsBulkDeleteRequest))]
[JsonSerializable(typeof(SessionsCheckInUseRequest))]
[JsonSerializable(typeof(SessionsCheckInUseResult))]
[JsonSerializable(typeof(SessionsCloseRequest))]
[JsonSerializable(typeof(SessionsCloseResult))]
[JsonSerializable(typeof(SessionsEnrichMetadataRequest))]
[JsonSerializable(typeof(SessionsFindByPrefixRequest))]
[JsonSerializable(typeof(SessionsFindByPrefixResult))]
[JsonSerializable(typeof(SessionsFindByTaskIDRequest))]
[JsonSerializable(typeof(SessionsFindByTaskIDResult))]
[JsonSerializable(typeof(SessionsForkRequest))]
[JsonSerializable(typeof(SessionsForkResult))]
[JsonSerializable(typeof(SessionsGetEventFilePathRequest))]
[JsonSerializable(typeof(SessionsGetEventFilePathResult))]
[JsonSerializable(typeof(SessionsGetLastForContextRequest))]
[JsonSerializable(typeof(SessionsGetLastForContextResult))]
[JsonSerializable(typeof(SessionsGetPersistedRemoteSteerableRequest))]
[JsonSerializable(typeof(SessionsGetPersistedRemoteSteerableResult))]
[JsonSerializable(typeof(SessionsListRequest))]
[JsonSerializable(typeof(SessionsLoadDeferredRepoHooksRequest))]
[JsonSerializable(typeof(SessionsPruneOldRequest))]
[JsonSerializable(typeof(SessionsReleaseLockRequest))]
[JsonSerializable(typeof(SessionsReleaseLockResult))]
[JsonSerializable(typeof(SessionsReloadPluginHooksRequest))]
[JsonSerializable(typeof(SessionsReloadPluginHooksResult))]
[JsonSerializable(typeof(SessionsSaveRequest))]
[JsonSerializable(typeof(SessionsSaveResult))]
[JsonSerializable(typeof(SessionsSetAdditionalPluginsRequest))]
[JsonSerializable(typeof(SessionsSetAdditionalPluginsResult))]
[JsonSerializable(typeof(ShellExecRequest))]
[JsonSerializable(typeof(ShellExecResult))]
[JsonSerializable(typeof(ShellKillRequest))]
[JsonSerializable(typeof(ShellKillResult))]
[JsonSerializable(typeof(ShutdownRequest))]
[JsonSerializable(typeof(Skill))]
[JsonSerializable(typeof(SkillList))]
[JsonSerializable(typeof(SkillsConfigSetDisabledSkillsRequest))]
[JsonSerializable(typeof(SkillsDisableRequest))]
[JsonSerializable(typeof(SkillsDiscoverRequest))]
[JsonSerializable(typeof(SkillsEnableRequest))]
[JsonSerializable(typeof(SkillsGetInvokedResult))]
[JsonSerializable(typeof(SkillsInvokedSkill))]
[JsonSerializable(typeof(SkillsLoadDiagnostics))]
[JsonSerializable(typeof(SlashCommandInfo))]
[JsonSerializable(typeof(SlashCommandInput))]
[JsonSerializable(typeof(SlashCommandInvocationResult))]
[JsonSerializable(typeof(SlashCommandSelectSubcommandOption))]
[JsonSerializable(typeof(TaskInfo))]
[JsonSerializable(typeof(TaskList))]
[JsonSerializable(typeof(TaskProgressLine))]
[JsonSerializable(typeof(TasksCancelRequest))]
[JsonSerializable(typeof(TasksCancelResult))]
[JsonSerializable(typeof(TasksGetCurrentPromotableResult))]
[JsonSerializable(typeof(TasksGetProgressRequest))]
[JsonSerializable(typeof(TasksGetProgressResult))]
[JsonSerializable(typeof(TasksGetProgressResultProgress))]
[JsonSerializable(typeof(TasksPromoteCurrentToBackgroundResult))]
[JsonSerializable(typeof(TasksPromoteToBackgroundRequest))]
[JsonSerializable(typeof(TasksPromoteToBackgroundResult))]
[JsonSerializable(typeof(TasksRefreshResult))]
[JsonSerializable(typeof(TasksRemoveRequest))]
[JsonSerializable(typeof(TasksRemoveResult))]
[JsonSerializable(typeof(TasksSendMessageRequest))]
[JsonSerializable(typeof(TasksSendMessageResult))]
[JsonSerializable(typeof(TasksStartAgentRequest))]
[JsonSerializable(typeof(TasksStartAgentResult))]
[JsonSerializable(typeof(TasksWaitForPendingResult))]
[JsonSerializable(typeof(TelemetrySetFeatureOverridesRequest))]
[JsonSerializable(typeof(Tool))]
[JsonSerializable(typeof(ToolList))]
[JsonSerializable(typeof(ToolsInitializeAndValidateResult))]
[JsonSerializable(typeof(ToolsListRequest))]
[JsonSerializable(typeof(UIElicitationRequest))]
[JsonSerializable(typeof(UIElicitationResponse))]
[JsonSerializable(typeof(UIElicitationResult))]
[JsonSerializable(typeof(UIElicitationSchema))]
[JsonSerializable(typeof(UIExitPlanModeResponse))]
[JsonSerializable(typeof(UIHandlePendingAutoModeSwitchRequest))]
[JsonSerializable(typeof(UIHandlePendingElicitationRequest))]
[JsonSerializable(typeof(UIHandlePendingExitPlanModeRequest))]
[JsonSerializable(typeof(UIHandlePendingResult))]
[JsonSerializable(typeof(UIHandlePendingSamplingRequest))]
[JsonSerializable(typeof(UIHandlePendingSamplingResponse))]
[JsonSerializable(typeof(UIHandlePendingUserInputRequest))]
[JsonSerializable(typeof(UIRegisterDirectAutoModeSwitchHandlerResult))]
[JsonSerializable(typeof(UIUnregisterDirectAutoModeSwitchHandlerRequest))]
[JsonSerializable(typeof(UIUnregisterDirectAutoModeSwitchHandlerResult))]
[JsonSerializable(typeof(UIUserInputResponse))]
[JsonSerializable(typeof(UsageGetMetricsResult))]
[JsonSerializable(typeof(UsageMetricsCodeChanges))]
[JsonSerializable(typeof(UsageMetricsModelMetric))]
[JsonSerializable(typeof(UsageMetricsModelMetricRequests))]
[JsonSerializable(typeof(UsageMetricsModelMetricTokenDetail))]
[JsonSerializable(typeof(UsageMetricsModelMetricUsage))]
[JsonSerializable(typeof(UsageMetricsTokenDetail))]
[JsonSerializable(typeof(WorkspaceDiffFileChange))]
[JsonSerializable(typeof(WorkspaceDiffResult))]
[JsonSerializable(typeof(WorkspacesCheckpoints))]
[JsonSerializable(typeof(WorkspacesCreateFileRequest))]
[JsonSerializable(typeof(WorkspacesDiffRequest))]
[JsonSerializable(typeof(WorkspacesGetWorkspaceResult))]
[JsonSerializable(typeof(WorkspacesGetWorkspaceResultWorkspace))]
[JsonSerializable(typeof(WorkspacesListCheckpointsResult))]
[JsonSerializable(typeof(WorkspacesListFilesResult))]
[JsonSerializable(typeof(WorkspacesReadCheckpointRequest))]
[JsonSerializable(typeof(WorkspacesReadCheckpointResult))]
[JsonSerializable(typeof(WorkspacesReadFileRequest))]
[JsonSerializable(typeof(WorkspacesReadFileResult))]
[JsonSerializable(typeof(WorkspacesSaveLargePasteRequest))]
[JsonSerializable(typeof(WorkspacesSaveLargePasteResult))]
[JsonSerializable(typeof(WorkspacesSaveLargePasteResultSaved))]
internal partial class RpcJsonContext : JsonSerializerContext;