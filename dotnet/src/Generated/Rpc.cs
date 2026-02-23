/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

using System.Text.Json;
using System.Text.Json.Serialization;
using StreamJsonRpc;

namespace GitHub.Copilot.SDK.Rpc;

public class PingResult
{
    /// <summary>Echoed message (or default greeting)</summary>
    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    /// <summary>Server timestamp in milliseconds</summary>
    [JsonPropertyName("timestamp")]
    public double Timestamp { get; set; }

    /// <summary>Server protocol version number</summary>
    [JsonPropertyName("protocolVersion")]
    public double ProtocolVersion { get; set; }
}

internal class PingRequest
{
    [JsonPropertyName("message")]
    public string? Message { get; set; }
}

public class ModelCapabilitiesSupports
{
    [JsonPropertyName("vision")]
    public bool Vision { get; set; }

    /// <summary>Whether this model supports reasoning effort configuration</summary>
    [JsonPropertyName("reasoningEffort")]
    public bool ReasoningEffort { get; set; }
}

public class ModelCapabilitiesLimits
{
    [JsonPropertyName("max_prompt_tokens")]
    public double? MaxPromptTokens { get; set; }

    [JsonPropertyName("max_output_tokens")]
    public double? MaxOutputTokens { get; set; }

    [JsonPropertyName("max_context_window_tokens")]
    public double MaxContextWindowTokens { get; set; }
}

/// <summary>Model capabilities and limits</summary>
public class ModelCapabilities
{
    [JsonPropertyName("supports")]
    public ModelCapabilitiesSupports Supports { get; set; } = new();

    [JsonPropertyName("limits")]
    public ModelCapabilitiesLimits Limits { get; set; } = new();
}

/// <summary>Policy state (if applicable)</summary>
public class ModelPolicy
{
    [JsonPropertyName("state")]
    public string State { get; set; } = string.Empty;

    [JsonPropertyName("terms")]
    public string Terms { get; set; } = string.Empty;
}

/// <summary>Billing information</summary>
public class ModelBilling
{
    [JsonPropertyName("multiplier")]
    public double Multiplier { get; set; }
}

public class Model
{
    /// <summary>Model identifier (e.g., "claude-sonnet-4.5")</summary>
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    /// <summary>Display name</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Model capabilities and limits</summary>
    [JsonPropertyName("capabilities")]
    public ModelCapabilities Capabilities { get; set; } = new();

    /// <summary>Policy state (if applicable)</summary>
    [JsonPropertyName("policy")]
    public ModelPolicy? Policy { get; set; }

    /// <summary>Billing information</summary>
    [JsonPropertyName("billing")]
    public ModelBilling? Billing { get; set; }

    /// <summary>Supported reasoning effort levels (only present if model supports reasoning effort)</summary>
    [JsonPropertyName("supportedReasoningEfforts")]
    public List<string>? SupportedReasoningEfforts { get; set; }

    /// <summary>Default reasoning effort level (only present if model supports reasoning effort)</summary>
    [JsonPropertyName("defaultReasoningEffort")]
    public string? DefaultReasoningEffort { get; set; }
}

public class ModelsListResult
{
    /// <summary>List of available models with full metadata</summary>
    [JsonPropertyName("models")]
    public List<Model> Models { get; set; } = new();
}

public class Tool
{
    /// <summary>Tool identifier (e.g., "bash", "grep", "str_replace_editor")</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP tools)</summary>
    [JsonPropertyName("namespacedName")]
    public string? NamespacedName { get; set; }

    /// <summary>Description of what the tool does</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    /// <summary>JSON Schema for the tool's input parameters</summary>
    [JsonPropertyName("parameters")]
    public Dictionary<string, object>? Parameters { get; set; }

    /// <summary>Optional instructions for how to use this tool effectively</summary>
    [JsonPropertyName("instructions")]
    public string? Instructions { get; set; }
}

public class ToolsListResult
{
    /// <summary>List of available built-in tools with metadata</summary>
    [JsonPropertyName("tools")]
    public List<Tool> Tools { get; set; } = new();
}

internal class ToolsListRequest
{
    [JsonPropertyName("model")]
    public string? Model { get; set; }
}

public class AccountGetQuotaResultQuotaSnapshotsValue
{
    /// <summary>Number of requests included in the entitlement</summary>
    [JsonPropertyName("entitlementRequests")]
    public double EntitlementRequests { get; set; }

    /// <summary>Number of requests used so far this period</summary>
    [JsonPropertyName("usedRequests")]
    public double UsedRequests { get; set; }

    /// <summary>Percentage of entitlement remaining</summary>
    [JsonPropertyName("remainingPercentage")]
    public double RemainingPercentage { get; set; }

    /// <summary>Number of overage requests made this period</summary>
    [JsonPropertyName("overage")]
    public double Overage { get; set; }

    /// <summary>Whether pay-per-request usage is allowed when quota is exhausted</summary>
    [JsonPropertyName("overageAllowedWithExhaustedQuota")]
    public bool OverageAllowedWithExhaustedQuota { get; set; }

    /// <summary>Date when the quota resets (ISO 8601)</summary>
    [JsonPropertyName("resetDate")]
    public string? ResetDate { get; set; }
}

public class AccountGetQuotaResult
{
    /// <summary>Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)</summary>
    [JsonPropertyName("quotaSnapshots")]
    public Dictionary<string, AccountGetQuotaResultQuotaSnapshotsValue> QuotaSnapshots { get; set; } = new();
}

public class SessionModelGetCurrentResult
{
    [JsonPropertyName("modelId")]
    public string? ModelId { get; set; }
}

internal class SessionModelGetCurrentRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

public class SessionModelSwitchToResult
{
    [JsonPropertyName("modelId")]
    public string? ModelId { get; set; }
}

internal class SessionModelSwitchToRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    [JsonPropertyName("modelId")]
    public string ModelId { get; set; } = string.Empty;
}

public class SessionModeGetResult
{
    /// <summary>The current agent mode.</summary>
    [JsonPropertyName("mode")]
    public SessionModeGetResultMode Mode { get; set; }
}

internal class SessionModeGetRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

public class SessionModeSetResult
{
    /// <summary>The agent mode after switching.</summary>
    [JsonPropertyName("mode")]
    public SessionModeGetResultMode Mode { get; set; }
}

internal class SessionModeSetRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    [JsonPropertyName("mode")]
    public SessionModeGetResultMode Mode { get; set; }
}

public class SessionPlanReadResult
{
    /// <summary>Whether plan.md exists in the workspace</summary>
    [JsonPropertyName("exists")]
    public bool Exists { get; set; }

    /// <summary>The content of plan.md, or null if it does not exist</summary>
    [JsonPropertyName("content")]
    public string? Content { get; set; }
}

internal class SessionPlanReadRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

public class SessionPlanUpdateResult
{
}

internal class SessionPlanUpdateRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;
}

public class SessionPlanDeleteResult
{
}

internal class SessionPlanDeleteRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

public class SessionWorkspaceListFilesResult
{
    /// <summary>Relative file paths in the workspace files directory</summary>
    [JsonPropertyName("files")]
    public List<string> Files { get; set; } = new();
}

internal class SessionWorkspaceListFilesRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

public class SessionWorkspaceReadFileResult
{
    /// <summary>File content as a UTF-8 string</summary>
    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;
}

internal class SessionWorkspaceReadFileRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;
}

public class SessionWorkspaceCreateFileResult
{
}

internal class SessionWorkspaceCreateFileRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    [JsonPropertyName("path")]
    public string Path { get; set; } = string.Empty;

    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;
}

public class SessionFleetStartResult
{
    /// <summary>Whether fleet mode was successfully activated</summary>
    [JsonPropertyName("started")]
    public bool Started { get; set; }
}

internal class SessionFleetStartRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    [JsonPropertyName("prompt")]
    public string? Prompt { get; set; }
}

public class Agent
{
    /// <summary>Unique identifier of the custom agent</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Human-readable display name</summary>
    [JsonPropertyName("displayName")]
    public string DisplayName { get; set; } = string.Empty;

    /// <summary>Description of the agent's purpose</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;
}

public class SessionAgentListResult
{
    /// <summary>Available custom agents</summary>
    [JsonPropertyName("agents")]
    public List<Agent> Agents { get; set; } = new();
}

internal class SessionAgentListRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

public class SessionAgentGetCurrentResultAgent
{
    /// <summary>Unique identifier of the custom agent</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Human-readable display name</summary>
    [JsonPropertyName("displayName")]
    public string DisplayName { get; set; } = string.Empty;

    /// <summary>Description of the agent's purpose</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;
}

public class SessionAgentGetCurrentResult
{
    /// <summary>Currently selected custom agent, or null if using the default agent</summary>
    [JsonPropertyName("agent")]
    public SessionAgentGetCurrentResultAgent? Agent { get; set; }
}

internal class SessionAgentGetCurrentRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>The newly selected custom agent</summary>
public class SessionAgentSelectResultAgent
{
    /// <summary>Unique identifier of the custom agent</summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>Human-readable display name</summary>
    [JsonPropertyName("displayName")]
    public string DisplayName { get; set; } = string.Empty;

    /// <summary>Description of the agent's purpose</summary>
    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;
}

public class SessionAgentSelectResult
{
    /// <summary>The newly selected custom agent</summary>
    [JsonPropertyName("agent")]
    public SessionAgentSelectResultAgent Agent { get; set; } = new();
}

internal class SessionAgentSelectRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
}

public class SessionAgentDeselectResult
{
}

internal class SessionAgentDeselectRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

public class SessionCompactionCompactResult
{
    /// <summary>Whether compaction completed successfully</summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }

    /// <summary>Number of tokens freed by compaction</summary>
    [JsonPropertyName("tokensRemoved")]
    public double TokensRemoved { get; set; }

    /// <summary>Number of messages removed during compaction</summary>
    [JsonPropertyName("messagesRemoved")]
    public double MessagesRemoved { get; set; }
}

internal class SessionCompactionCompactRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

[JsonConverter(typeof(JsonStringEnumConverter<SessionModeGetResultMode>))]
public enum SessionModeGetResultMode
{
    [JsonStringEnumMemberName("interactive")]
    Interactive,
    [JsonStringEnumMemberName("plan")]
    Plan,
    [JsonStringEnumMemberName("autopilot")]
    Autopilot,
}


/// <summary>Typed server-scoped RPC methods (no session required).</summary>
public class ServerRpc
{
    private readonly JsonRpc _rpc;

    internal ServerRpc(JsonRpc rpc)
    {
        _rpc = rpc;
        Models = new ModelsApi(rpc);
        Tools = new ToolsApi(rpc);
        Account = new AccountApi(rpc);
    }

    /// <summary>Calls "ping".</summary>
    public async Task<PingResult> PingAsync(string? message = null, CancellationToken cancellationToken = default)
    {
        var request = new PingRequest { Message = message };
        return await CopilotClient.InvokeRpcAsync<PingResult>(_rpc, "ping", [request], cancellationToken);
    }

    /// <summary>Models APIs.</summary>
    public ModelsApi Models { get; }

    /// <summary>Tools APIs.</summary>
    public ToolsApi Tools { get; }

    /// <summary>Account APIs.</summary>
    public AccountApi Account { get; }
}

/// <summary>Server-scoped Models APIs.</summary>
public class ModelsApi
{
    private readonly JsonRpc _rpc;

    internal ModelsApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Calls "models.list".</summary>
    public async Task<ModelsListResult> ListAsync(CancellationToken cancellationToken = default)
    {
        return await CopilotClient.InvokeRpcAsync<ModelsListResult>(_rpc, "models.list", [], cancellationToken);
    }
}

/// <summary>Server-scoped Tools APIs.</summary>
public class ToolsApi
{
    private readonly JsonRpc _rpc;

    internal ToolsApi(JsonRpc rpc)
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

/// <summary>Server-scoped Account APIs.</summary>
public class AccountApi
{
    private readonly JsonRpc _rpc;

    internal AccountApi(JsonRpc rpc)
    {
        _rpc = rpc;
    }

    /// <summary>Calls "account.getQuota".</summary>
    public async Task<AccountGetQuotaResult> GetQuotaAsync(CancellationToken cancellationToken = default)
    {
        return await CopilotClient.InvokeRpcAsync<AccountGetQuotaResult>(_rpc, "account.getQuota", [], cancellationToken);
    }
}

/// <summary>Typed session-scoped RPC methods.</summary>
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
    }

    public ModelApi Model { get; }

    public ModeApi Mode { get; }

    public PlanApi Plan { get; }

    public WorkspaceApi Workspace { get; }

    public FleetApi Fleet { get; }

    public AgentApi Agent { get; }

    public CompactionApi Compaction { get; }
}

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
    public async Task<SessionModelSwitchToResult> SwitchToAsync(string modelId, CancellationToken cancellationToken = default)
    {
        var request = new SessionModelSwitchToRequest { SessionId = _sessionId, ModelId = modelId };
        return await CopilotClient.InvokeRpcAsync<SessionModelSwitchToResult>(_rpc, "session.model.switchTo", [request], cancellationToken);
    }
}

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
    public async Task<SessionFleetStartResult> StartAsync(string? prompt, CancellationToken cancellationToken = default)
    {
        var request = new SessionFleetStartRequest { SessionId = _sessionId, Prompt = prompt };
        return await CopilotClient.InvokeRpcAsync<SessionFleetStartResult>(_rpc, "session.fleet.start", [request], cancellationToken);
    }
}

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
[JsonSerializable(typeof(SessionModeGetRequest))]
[JsonSerializable(typeof(SessionModeGetResult))]
[JsonSerializable(typeof(SessionModeSetRequest))]
[JsonSerializable(typeof(SessionModeSetResult))]
[JsonSerializable(typeof(SessionModelGetCurrentRequest))]
[JsonSerializable(typeof(SessionModelGetCurrentResult))]
[JsonSerializable(typeof(SessionModelSwitchToRequest))]
[JsonSerializable(typeof(SessionModelSwitchToResult))]
[JsonSerializable(typeof(SessionPlanDeleteRequest))]
[JsonSerializable(typeof(SessionPlanDeleteResult))]
[JsonSerializable(typeof(SessionPlanReadRequest))]
[JsonSerializable(typeof(SessionPlanReadResult))]
[JsonSerializable(typeof(SessionPlanUpdateRequest))]
[JsonSerializable(typeof(SessionPlanUpdateResult))]
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