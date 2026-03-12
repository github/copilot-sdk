/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

using System.Text.Json;
using System.Text.Json.Serialization;
using StreamJsonRpc;

namespace GitHub.Copilot.SDK.Rpc;

/// <summary>RPC data type for Ping operations.</summary>
public class PingResult
{
    /// <summary>Echoed message (or default greeting).</summary>
    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    /// <summary>Server timestamp in milliseconds.</summary>
    [JsonPropertyName("timestamp")]
    public double Timestamp { get; set; }

    /// <summary>Server protocol version number.</summary>
    [JsonPropertyName("protocolVersion")]
    public double ProtocolVersion { get; set; }
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

/// <summary>Token limits for prompts, outputs, and context window.</summary>
public class ModelCapabilitiesLimits
{
    /// <summary>Maximum number of prompt/input tokens.</summary>
    [JsonPropertyName("max_prompt_tokens")]
    public double? MaxPromptTokens { get; set; }

    /// <summary>Maximum number of output/completion tokens.</summary>
    [JsonPropertyName("max_output_tokens")]
    public double? MaxOutputTokens { get; set; }

    /// <summary>Maximum total context window size in tokens.</summary>
    [JsonPropertyName("max_context_window_tokens")]
    public double MaxContextWindowTokens { get; set; }
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
    public List<string>? SupportedReasoningEfforts { get; set; }

    /// <summary>Default reasoning effort level (only present if model supports reasoning effort).</summary>
    [JsonPropertyName("defaultReasoningEffort")]
    public string? DefaultReasoningEffort { get; set; }
}

/// <summary>RPC data type for ModelsList operations.</summary>
public class ModelsListResult
{
    /// <summary>List of available models with full metadata.</summary>
    [JsonPropertyName("models")]
    public List<Model> Models { get => field ??= []; set; }
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
    public Dictionary<string, object>? Parameters { get; set; }

    /// <summary>Optional instructions for how to use this tool effectively.</summary>
    [JsonPropertyName("instructions")]
    public string? Instructions { get; set; }
}

/// <summary>RPC data type for ToolsList operations.</summary>
public class ToolsListResult
{
    /// <summary>List of available built-in tools with metadata.</summary>
    [JsonPropertyName("tools")]
    public List<Tool> Tools { get => field ??= []; set; }
}

/// <summary>RPC data type for ToolsList operations.</summary>
internal class ToolsListRequest
{
    /// <summary>Optional model ID — when provided, the returned tool list reflects model-specific overrides.</summary>
    [JsonPropertyName("model")]
    public string? Model { get; set; }
}

/// <summary>RPC data type for AccountGetQuotaResultQuotaSnapshotsValue operations.</summary>
public class AccountGetQuotaResultQuotaSnapshotsValue
{
    /// <summary>Number of requests included in the entitlement.</summary>
    [JsonPropertyName("entitlementRequests")]
    public double EntitlementRequests { get; set; }

    /// <summary>Number of requests used so far this period.</summary>
    [JsonPropertyName("usedRequests")]
    public double UsedRequests { get; set; }

    /// <summary>Percentage of entitlement remaining.</summary>
    [JsonPropertyName("remainingPercentage")]
    public double RemainingPercentage { get; set; }

    /// <summary>Number of overage requests made this period.</summary>
    [JsonPropertyName("overage")]
    public double Overage { get; set; }

    /// <summary>Whether pay-per-request usage is allowed when quota is exhausted.</summary>
    [JsonPropertyName("overageAllowedWithExhaustedQuota")]
    public bool OverageAllowedWithExhaustedQuota { get; set; }

    /// <summary>Date when the quota resets (ISO 8601).</summary>
    [JsonPropertyName("resetDate")]
    public string? ResetDate { get; set; }
}

/// <summary>RPC data type for AccountGetQuota operations.</summary>
public class AccountGetQuotaResult
{
    /// <summary>Quota snapshots keyed by type (e.g., chat, completions, premium_interactions).</summary>
    [JsonPropertyName("quotaSnapshots")]
    public Dictionary<string, AccountGetQuotaResultQuotaSnapshotsValue> QuotaSnapshots { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionLog operations.</summary>
public class SessionLogResult
{
    /// <summary>The unique identifier of the emitted session event.</summary>
    [JsonPropertyName("eventId")]
    public Guid EventId { get; set; }
}

/// <summary>RPC data type for SessionLog operations.</summary>
internal class SessionLogRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Human-readable message.</summary>
    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    /// <summary>Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".</summary>
    [JsonPropertyName("level")]
    public SessionLogRequestLevel? Level { get; set; }

    /// <summary>When true, the message is transient and not persisted to the session event log on disk.</summary>
    [JsonPropertyName("ephemeral")]
    public bool? Ephemeral { get; set; }
}

/// <summary>RPC data type for SessionModelGetCurrent operations.</summary>
public class SessionModelGetCurrentResult
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

/// <summary>RPC data type for SessionModelSwitchTo operations.</summary>
public class SessionModelSwitchToResult
{
    /// <summary>Currently active model identifier after the switch.</summary>
    [JsonPropertyName("modelId")]
    public string? ModelId { get; set; }
}

/// <summary>RPC data type for SessionModelSwitchTo operations.</summary>
internal class SessionModelSwitchToRequest
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
}

/// <summary>RPC data type for SessionModeGet operations.</summary>
public class SessionModeGetResult
{
    /// <summary>The current agent mode.</summary>
    [JsonPropertyName("mode")]
    public SessionModeGetResultMode Mode { get; set; }
}

/// <summary>RPC data type for SessionModeGet operations.</summary>
internal class SessionModeGetRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionModeSet operations.</summary>
public class SessionModeSetResult
{
    /// <summary>The agent mode after switching.</summary>
    [JsonPropertyName("mode")]
    public SessionModeGetResultMode Mode { get; set; }
}

/// <summary>RPC data type for SessionModeSet operations.</summary>
internal class SessionModeSetRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>The mode to switch to. Valid values: "interactive", "plan", "autopilot".</summary>
    [JsonPropertyName("mode")]
    public SessionModeGetResultMode Mode { get; set; }
}

/// <summary>RPC data type for SessionPlanRead operations.</summary>
public class SessionPlanReadResult
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

/// <summary>RPC data type for SessionPlanUpdate operations.</summary>
public class SessionPlanUpdateResult
{
}

/// <summary>RPC data type for SessionPlanUpdate operations.</summary>
internal class SessionPlanUpdateRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>The new content for the plan file.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionPlanDelete operations.</summary>
public class SessionPlanDeleteResult
{
}

/// <summary>RPC data type for SessionPlanDelete operations.</summary>
internal class SessionPlanDeleteRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionWorkspaceListFiles operations.</summary>
public class SessionWorkspaceListFilesResult
{
    /// <summary>Relative file paths in the workspace files directory.</summary>
    [JsonPropertyName("files")]
    public List<string> Files { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionWorkspaceListFiles operations.</summary>
internal class SessionWorkspaceListFilesRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionWorkspaceReadFile operations.</summary>
public class SessionWorkspaceReadFileResult
{
    /// <summary>File content as a UTF-8 string.</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionWorkspaceReadFile operations.</summary>
internal class SessionWorkspaceReadFileRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Relative path within the workspace files directory.</summary>
    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionWorkspaceCreateFile operations.</summary>
public class SessionWorkspaceCreateFileResult
{
}

/// <summary>RPC data type for SessionWorkspaceCreateFile operations.</summary>
internal class SessionWorkspaceCreateFileRequest
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

/// <summary>RPC data type for SessionFleetStart operations.</summary>
public class SessionFleetStartResult
{
    /// <summary>Whether fleet mode was successfully activated.</summary>
    [JsonPropertyName("started")]
    public bool Started { get; set; }
}

/// <summary>RPC data type for SessionFleetStart operations.</summary>
internal class SessionFleetStartRequest
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

/// <summary>RPC data type for SessionAgentList operations.</summary>
public class SessionAgentListResult
{
    /// <summary>Available custom agents.</summary>
    [JsonPropertyName("agents")]
    public List<Agent> Agents { get => field ??= []; set; }
}

/// <summary>RPC data type for SessionAgentList operations.</summary>
internal class SessionAgentListRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionAgentGetCurrentResultAgent operations.</summary>
public class SessionAgentGetCurrentResultAgent
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

/// <summary>RPC data type for SessionAgentGetCurrent operations.</summary>
public class SessionAgentGetCurrentResult
{
    /// <summary>Currently selected custom agent, or null if using the default agent.</summary>
    [JsonPropertyName("agent")]
    public SessionAgentGetCurrentResultAgent? Agent { get; set; }
}

/// <summary>RPC data type for SessionAgentGetCurrent operations.</summary>
internal class SessionAgentGetCurrentRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>The newly selected custom agent.</summary>
public class SessionAgentSelectResultAgent
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

/// <summary>RPC data type for SessionAgentSelect operations.</summary>
public class SessionAgentSelectResult
{
    /// <summary>The newly selected custom agent.</summary>
    [JsonPropertyName("agent")]
    public SessionAgentSelectResultAgent Agent { get => field ??= new(); set; }
}

/// <summary>RPC data type for SessionAgentSelect operations.</summary>
internal class SessionAgentSelectRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Name of the custom agent to select.</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionAgentDeselect operations.</summary>
public class SessionAgentDeselectResult
{
}

/// <summary>RPC data type for SessionAgentDeselect operations.</summary>
internal class SessionAgentDeselectRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionCompactionCompact operations.</summary>
public class SessionCompactionCompactResult
{
    /// <summary>Whether compaction completed successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }

    /// <summary>Number of tokens freed by compaction.</summary>
    [JsonPropertyName("tokensRemoved")]
    public double TokensRemoved { get; set; }

    /// <summary>Number of messages removed during compaction.</summary>
    [JsonPropertyName("messagesRemoved")]
    public double MessagesRemoved { get; set; }
}

/// <summary>RPC data type for SessionCompactionCompact operations.</summary>
internal class SessionCompactionCompactRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionToolsHandlePendingToolCall operations.</summary>
public class SessionToolsHandlePendingToolCallResult
{
    /// <summary>Whether the tool call result was handled successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for SessionToolsHandlePendingToolCall operations.</summary>
internal class SessionToolsHandlePendingToolCallRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Gets or sets the <c>requestId</c> value.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Gets or sets the <c>result</c> value.</summary>
    [JsonPropertyName("result")]
    public object? Result { get; set; }

    /// <summary>Gets or sets the <c>error</c> value.</summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }
}

/// <summary>RPC data type for SessionPermissionsHandlePendingPermissionRequest operations.</summary>
public class SessionPermissionsHandlePendingPermissionRequestResult
{
    /// <summary>Whether the permission request was handled successfully.</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }
}

/// <summary>RPC data type for SessionPermissionsHandlePendingPermissionRequest operations.</summary>
internal class SessionPermissionsHandlePendingPermissionRequestRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Gets or sets the <c>requestId</c> value.</summary>
    [JsonPropertyName("requestId")]
    public string RequestId { get; set; } = string.Empty;

    /// <summary>Gets or sets the <c>result</c> value.</summary>
    [JsonPropertyName("result")]
    public object Result { get; set; } = null!;
}

/// <summary>RPC data type for SessionShellExec operations.</summary>
public class SessionShellExecResult
{
    /// <summary>Unique identifier for tracking streamed output.</summary>
    [JsonPropertyName("processId")]
    public string ProcessId { get; set; } = string.Empty;
}

/// <summary>RPC data type for SessionShellExec operations.</summary>
internal class SessionShellExecRequest
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
    [JsonPropertyName("timeout")]
    public double? Timeout { get; set; }
}

/// <summary>RPC data type for SessionShellKill operations.</summary>
public class SessionShellKillResult
{
    /// <summary>Whether the signal was sent successfully.</summary>
    [JsonPropertyName("killed")]
    public bool Killed { get; set; }
}

/// <summary>RPC data type for SessionShellKill operations.</summary>
internal class SessionShellKillRequest
{
    /// <summary>Target session identifier.</summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Process identifier returned by shell.exec.</summary>
    [JsonPropertyName("processId")]
    public string ProcessId { get; set; } = string.Empty;

    /// <summary>Signal to send (default: SIGTERM).</summary>
    [JsonPropertyName("signal")]
    public SessionShellKillRequestSignal? Signal { get; set; }
}

/// <summary>Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionLogRequestLevel>))]
public enum SessionLogRequestLevel
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


/// <summary>The current agent mode.</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionModeGetResultMode>))]
public enum SessionModeGetResultMode
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


/// <summary>Signal to send (default: SIGTERM).</summary>
[JsonConverter(typeof(JsonStringEnumConverter<SessionShellKillRequestSignal>))]
public enum SessionShellKillRequestSignal
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
    public async Task<ModelsListResult> ListAsync(CancellationToken cancellationToken = default)
    {
        return await CopilotClient.InvokeRpcAsync<ModelsListResult>(_rpc, "models.list", [], cancellationToken);
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
    public async Task<ToolsListResult> ListAsync(string? model = null, CancellationToken cancellationToken = default)
    {
        var request = new ToolsListRequest { Model = model };
        return await CopilotClient.InvokeRpcAsync<ToolsListResult>(_rpc, "tools.list", [request], cancellationToken);
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
    public async Task<AccountGetQuotaResult> GetQuotaAsync(CancellationToken cancellationToken = default)
    {
        return await CopilotClient.InvokeRpcAsync<AccountGetQuotaResult>(_rpc, "account.getQuota", [], cancellationToken);
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
        Compaction = new CompactionApi(rpc, sessionId);
        Tools = new ToolsApi(rpc, sessionId);
        Permissions = new PermissionsApi(rpc, sessionId);
        Shell = new ShellApi(rpc, sessionId);
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

    /// <summary>Compaction APIs.</summary>
    public CompactionApi Compaction { get; }

    /// <summary>Tools APIs.</summary>
    public ToolsApi Tools { get; }

    /// <summary>Permissions APIs.</summary>
    public PermissionsApi Permissions { get; }

    /// <summary>Shell APIs.</summary>
    public ShellApi Shell { get; }

    /// <summary>Calls "session.log".</summary>
    public async Task<SessionLogResult> LogAsync(string message, SessionLogRequestLevel? level = null, bool? ephemeral = null, CancellationToken cancellationToken = default)
    {
        var request = new SessionLogRequest { SessionId = _sessionId, Message = message, Level = level, Ephemeral = ephemeral };
        return await CopilotClient.InvokeRpcAsync<SessionLogResult>(_rpc, "session.log", [request], cancellationToken);
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
    public async Task<SessionModelGetCurrentResult> GetCurrentAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionModelGetCurrentRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionModelGetCurrentResult>(_rpc, "session.model.getCurrent", [request], cancellationToken);
    }

    /// <summary>Calls "session.model.switchTo".</summary>
    public async Task<SessionModelSwitchToResult> SwitchToAsync(string modelId, string? reasoningEffort = null, CancellationToken cancellationToken = default)
    {
        var request = new SessionModelSwitchToRequest { SessionId = _sessionId, ModelId = modelId, ReasoningEffort = reasoningEffort };
        return await CopilotClient.InvokeRpcAsync<SessionModelSwitchToResult>(_rpc, "session.model.switchTo", [request], cancellationToken);
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
    public async Task<SessionModeGetResult> GetAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionModeGetRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionModeGetResult>(_rpc, "session.mode.get", [request], cancellationToken);
    }

    /// <summary>Calls "session.mode.set".</summary>
    public async Task<SessionModeSetResult> SetAsync(SessionModeGetResultMode mode, CancellationToken cancellationToken = default)
    {
        var request = new SessionModeSetRequest { SessionId = _sessionId, Mode = mode };
        return await CopilotClient.InvokeRpcAsync<SessionModeSetResult>(_rpc, "session.mode.set", [request], cancellationToken);
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
    public async Task<SessionPlanReadResult> ReadAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionPlanReadRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionPlanReadResult>(_rpc, "session.plan.read", [request], cancellationToken);
    }

    /// <summary>Calls "session.plan.update".</summary>
    public async Task<SessionPlanUpdateResult> UpdateAsync(string content, CancellationToken cancellationToken = default)
    {
        var request = new SessionPlanUpdateRequest { SessionId = _sessionId, Content = content };
        return await CopilotClient.InvokeRpcAsync<SessionPlanUpdateResult>(_rpc, "session.plan.update", [request], cancellationToken);
    }

    /// <summary>Calls "session.plan.delete".</summary>
    public async Task<SessionPlanDeleteResult> DeleteAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionPlanDeleteRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionPlanDeleteResult>(_rpc, "session.plan.delete", [request], cancellationToken);
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
    public async Task<SessionWorkspaceListFilesResult> ListFilesAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionWorkspaceListFilesRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionWorkspaceListFilesResult>(_rpc, "session.workspace.listFiles", [request], cancellationToken);
    }

    /// <summary>Calls "session.workspace.readFile".</summary>
    public async Task<SessionWorkspaceReadFileResult> ReadFileAsync(string path, CancellationToken cancellationToken = default)
    {
        var request = new SessionWorkspaceReadFileRequest { SessionId = _sessionId, Path = path };
        return await CopilotClient.InvokeRpcAsync<SessionWorkspaceReadFileResult>(_rpc, "session.workspace.readFile", [request], cancellationToken);
    }

    /// <summary>Calls "session.workspace.createFile".</summary>
    public async Task<SessionWorkspaceCreateFileResult> CreateFileAsync(string path, string content, CancellationToken cancellationToken = default)
    {
        var request = new SessionWorkspaceCreateFileRequest { SessionId = _sessionId, Path = path, Content = content };
        return await CopilotClient.InvokeRpcAsync<SessionWorkspaceCreateFileResult>(_rpc, "session.workspace.createFile", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Fleet APIs.</summary>
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
    public async Task<SessionFleetStartResult> StartAsync(string? prompt = null, CancellationToken cancellationToken = default)
    {
        var request = new SessionFleetStartRequest { SessionId = _sessionId, Prompt = prompt };
        return await CopilotClient.InvokeRpcAsync<SessionFleetStartResult>(_rpc, "session.fleet.start", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Agent APIs.</summary>
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
    public async Task<SessionAgentListResult> ListAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionAgentListRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionAgentListResult>(_rpc, "session.agent.list", [request], cancellationToken);
    }

    /// <summary>Calls "session.agent.getCurrent".</summary>
    public async Task<SessionAgentGetCurrentResult> GetCurrentAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionAgentGetCurrentRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionAgentGetCurrentResult>(_rpc, "session.agent.getCurrent", [request], cancellationToken);
    }

    /// <summary>Calls "session.agent.select".</summary>
    public async Task<SessionAgentSelectResult> SelectAsync(string name, CancellationToken cancellationToken = default)
    {
        var request = new SessionAgentSelectRequest { SessionId = _sessionId, Name = name };
        return await CopilotClient.InvokeRpcAsync<SessionAgentSelectResult>(_rpc, "session.agent.select", [request], cancellationToken);
    }

    /// <summary>Calls "session.agent.deselect".</summary>
    public async Task<SessionAgentDeselectResult> DeselectAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionAgentDeselectRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionAgentDeselectResult>(_rpc, "session.agent.deselect", [request], cancellationToken);
    }
}

/// <summary>Provides session-scoped Compaction APIs.</summary>
public class CompactionApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal CompactionApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.compaction.compact".</summary>
    public async Task<SessionCompactionCompactResult> CompactAsync(CancellationToken cancellationToken = default)
    {
        var request = new SessionCompactionCompactRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionCompactionCompactResult>(_rpc, "session.compaction.compact", [request], cancellationToken);
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
    public async Task<SessionToolsHandlePendingToolCallResult> HandlePendingToolCallAsync(string requestId, object? result = null, string? error = null, CancellationToken cancellationToken = default)
    {
        var request = new SessionToolsHandlePendingToolCallRequest { SessionId = _sessionId, RequestId = requestId, Result = result, Error = error };
        return await CopilotClient.InvokeRpcAsync<SessionToolsHandlePendingToolCallResult>(_rpc, "session.tools.handlePendingToolCall", [request], cancellationToken);
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
    public async Task<SessionPermissionsHandlePendingPermissionRequestResult> HandlePendingPermissionRequestAsync(string requestId, object result, CancellationToken cancellationToken = default)
    {
        var request = new SessionPermissionsHandlePendingPermissionRequestRequest { SessionId = _sessionId, RequestId = requestId, Result = result };
        return await CopilotClient.InvokeRpcAsync<SessionPermissionsHandlePendingPermissionRequestResult>(_rpc, "session.permissions.handlePendingPermissionRequest", [request], cancellationToken);
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
    public async Task<SessionShellExecResult> ExecAsync(string command, string? cwd = null, double? timeout = null, CancellationToken cancellationToken = default)
    {
        var request = new SessionShellExecRequest { SessionId = _sessionId, Command = command, Cwd = cwd, Timeout = timeout };
        return await CopilotClient.InvokeRpcAsync<SessionShellExecResult>(_rpc, "session.shell.exec", [request], cancellationToken);
    }

    /// <summary>Calls "session.shell.kill".</summary>
    public async Task<SessionShellKillResult> KillAsync(string processId, SessionShellKillRequestSignal? signal = null, CancellationToken cancellationToken = default)
    {
        var request = new SessionShellKillRequest { SessionId = _sessionId, ProcessId = processId, Signal = signal };
        return await CopilotClient.InvokeRpcAsync<SessionShellKillResult>(_rpc, "session.shell.kill", [request], cancellationToken);
    }
}

[JsonSourceGenerationOptions(
    JsonSerializerDefaults.Web,
    AllowOutOfOrderMetadataProperties = true,
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
[JsonSerializable(typeof(AccountGetQuotaResult))]
[JsonSerializable(typeof(AccountGetQuotaResultQuotaSnapshotsValue))]
[JsonSerializable(typeof(Agent))]
[JsonSerializable(typeof(Model))]
[JsonSerializable(typeof(ModelBilling))]
[JsonSerializable(typeof(ModelCapabilities))]
[JsonSerializable(typeof(ModelCapabilitiesLimits))]
[JsonSerializable(typeof(ModelCapabilitiesSupports))]
[JsonSerializable(typeof(ModelPolicy))]
[JsonSerializable(typeof(ModelsListResult))]
[JsonSerializable(typeof(PingRequest))]
[JsonSerializable(typeof(PingResult))]
[JsonSerializable(typeof(SessionAgentDeselectRequest))]
[JsonSerializable(typeof(SessionAgentDeselectResult))]
[JsonSerializable(typeof(SessionAgentGetCurrentRequest))]
[JsonSerializable(typeof(SessionAgentGetCurrentResult))]
[JsonSerializable(typeof(SessionAgentGetCurrentResultAgent))]
[JsonSerializable(typeof(SessionAgentListRequest))]
[JsonSerializable(typeof(SessionAgentListResult))]
[JsonSerializable(typeof(SessionAgentSelectRequest))]
[JsonSerializable(typeof(SessionAgentSelectResult))]
[JsonSerializable(typeof(SessionAgentSelectResultAgent))]
[JsonSerializable(typeof(SessionCompactionCompactRequest))]
[JsonSerializable(typeof(SessionCompactionCompactResult))]
[JsonSerializable(typeof(SessionFleetStartRequest))]
[JsonSerializable(typeof(SessionFleetStartResult))]
[JsonSerializable(typeof(SessionLogRequest))]
[JsonSerializable(typeof(SessionLogResult))]
[JsonSerializable(typeof(SessionModeGetRequest))]
[JsonSerializable(typeof(SessionModeGetResult))]
[JsonSerializable(typeof(SessionModeSetRequest))]
[JsonSerializable(typeof(SessionModeSetResult))]
[JsonSerializable(typeof(SessionModelGetCurrentRequest))]
[JsonSerializable(typeof(SessionModelGetCurrentResult))]
[JsonSerializable(typeof(SessionModelSwitchToRequest))]
[JsonSerializable(typeof(SessionModelSwitchToResult))]
[JsonSerializable(typeof(SessionPermissionsHandlePendingPermissionRequestRequest))]
[JsonSerializable(typeof(SessionPermissionsHandlePendingPermissionRequestResult))]
[JsonSerializable(typeof(SessionPlanDeleteRequest))]
[JsonSerializable(typeof(SessionPlanDeleteResult))]
[JsonSerializable(typeof(SessionPlanReadRequest))]
[JsonSerializable(typeof(SessionPlanReadResult))]
[JsonSerializable(typeof(SessionPlanUpdateRequest))]
[JsonSerializable(typeof(SessionPlanUpdateResult))]
[JsonSerializable(typeof(SessionShellExecRequest))]
[JsonSerializable(typeof(SessionShellExecResult))]
[JsonSerializable(typeof(SessionShellKillRequest))]
[JsonSerializable(typeof(SessionShellKillResult))]
[JsonSerializable(typeof(SessionToolsHandlePendingToolCallRequest))]
[JsonSerializable(typeof(SessionToolsHandlePendingToolCallResult))]
[JsonSerializable(typeof(SessionWorkspaceCreateFileRequest))]
[JsonSerializable(typeof(SessionWorkspaceCreateFileResult))]
[JsonSerializable(typeof(SessionWorkspaceListFilesRequest))]
[JsonSerializable(typeof(SessionWorkspaceListFilesResult))]
[JsonSerializable(typeof(SessionWorkspaceReadFileRequest))]
[JsonSerializable(typeof(SessionWorkspaceReadFileResult))]
[JsonSerializable(typeof(Tool))]
[JsonSerializable(typeof(ToolsListRequest))]
[JsonSerializable(typeof(ToolsListResult))]
internal partial class RpcJsonContext : JsonSerializerContext;