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

internal class ListRequest
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

internal class GetCurrentRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

public class SessionModelSwitchToResult
{
    [JsonPropertyName("modelId")]
    public string? ModelId { get; set; }
}

internal class SwitchToRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    [JsonPropertyName("modelId")]
    public string ModelId { get; set; } = string.Empty;
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
        var request = new ListRequest { Model = model };
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
    }

    public ModelApi Model { get; }
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
        var request = new GetCurrentRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<SessionModelGetCurrentResult>(_rpc, "session.model.getCurrent", [request], cancellationToken);
    }

    /// <summary>Calls "session.model.switchTo".</summary>
    public async Task<SessionModelSwitchToResult> SwitchToAsync(string modelId, CancellationToken cancellationToken = default)
    {
        var request = new SwitchToRequest { SessionId = _sessionId, ModelId = modelId };
        return await CopilotClient.InvokeRpcAsync<SessionModelSwitchToResult>(_rpc, "session.model.switchTo", [request], cancellationToken);
    }
}

[JsonSourceGenerationOptions(
    JsonSerializerDefaults.Web,
    AllowOutOfOrderMetadataProperties = true,
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
[JsonSerializable(typeof(AccountGetQuotaResult))]
[JsonSerializable(typeof(AccountGetQuotaResultQuotaSnapshotsValue))]
[JsonSerializable(typeof(GetCurrentRequest))]
[JsonSerializable(typeof(ListRequest))]
[JsonSerializable(typeof(Model))]
[JsonSerializable(typeof(ModelBilling))]
[JsonSerializable(typeof(ModelCapabilities))]
[JsonSerializable(typeof(ModelCapabilitiesLimits))]
[JsonSerializable(typeof(ModelCapabilitiesSupports))]
[JsonSerializable(typeof(ModelPolicy))]
[JsonSerializable(typeof(ModelsListResult))]
[JsonSerializable(typeof(PingRequest))]
[JsonSerializable(typeof(PingResult))]
[JsonSerializable(typeof(SessionModelGetCurrentResult))]
[JsonSerializable(typeof(SessionModelSwitchToResult))]
[JsonSerializable(typeof(SwitchToRequest))]
[JsonSerializable(typeof(Tool))]
[JsonSerializable(typeof(ToolsListResult))]
internal partial class RpcJsonContext : JsonSerializerContext;