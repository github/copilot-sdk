/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
//
// Generated from: api.schema.json
// Generated at: 2026-02-13T15:31:22.569Z

using System.Diagnostics.CodeAnalysis;
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

public class ModelGetCurrentResult
{
    [JsonPropertyName("modelId")]
    public string? ModelId { get; set; }
}

internal class GetCurrentRequest
{
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;
}

public class ModelSwitchToResult
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

internal static class ServerRpc
{
    /// <summary>Calls "ping" via JSON-RPC.</summary>
    /// <param name="message">Optional message to echo back</param>
    internal static async Task<PingResult> PingAsync(JsonRpc rpc, string? message = null, CancellationToken cancellationToken = default)
    {
        var request = new PingRequest { Message = message };
        return await CopilotClient.InvokeRpcAsync<PingResult>(rpc, "ping", [request], cancellationToken);
    }

    internal static class Models
    {
        /// <summary>Calls "models.list" via JSON-RPC.</summary>
        internal static async Task<ModelsListResult> ListAsync(JsonRpc rpc, CancellationToken cancellationToken = default)
        {
            return await CopilotClient.InvokeRpcAsync<ModelsListResult>(rpc, "models.list", [], cancellationToken);
        }
    }

    internal static class Tools
    {
        /// <summary>Calls "tools.list" via JSON-RPC.</summary>
        /// <param name="model">Optional model ID — when provided, the returned tool list reflects model-specific overrides</param>
        internal static async Task<ToolsListResult> ListAsync(JsonRpc rpc, string? model = null, CancellationToken cancellationToken = default)
        {
            var request = new ListRequest { Model = model };
            return await CopilotClient.InvokeRpcAsync<ToolsListResult>(rpc, "tools.list", [request], cancellationToken);
        }
    }

    internal static class Account
    {
        /// <summary>Calls "account.getQuota" via JSON-RPC.</summary>
        internal static async Task<AccountGetQuotaResult> GetQuotaAsync(JsonRpc rpc, CancellationToken cancellationToken = default)
        {
            return await CopilotClient.InvokeRpcAsync<AccountGetQuotaResult>(rpc, "account.getQuota", [], cancellationToken);
        }
    }
}

/// <summary>Typed session-scoped RPC methods. Automatically binds the session ID.</summary>
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

    /// <summary>Model APIs.</summary>
    public ModelApi Model { get; }
}

/// <summary>Session-scoped Model APIs.</summary>
public class ModelApi
{
    private readonly JsonRpc _rpc;
    private readonly string _sessionId;

    internal ModelApi(JsonRpc rpc, string sessionId)
    {
        _rpc = rpc;
        _sessionId = sessionId;
    }

    /// <summary>Calls "session.model.getCurrent" via JSON-RPC.</summary>
    [Experimental("CopilotSdk001")]
    public async Task<ModelGetCurrentResult> GetCurrentAsync(CancellationToken cancellationToken = default)
    {
        var request = new GetCurrentRequest { SessionId = _sessionId };
        return await CopilotClient.InvokeRpcAsync<ModelGetCurrentResult>(_rpc, "session.model.getCurrent", [request], cancellationToken);
    }

    /// <summary>Calls "session.model.switchTo" via JSON-RPC.</summary>
    [Experimental("CopilotSdk001")]
    public async Task<ModelSwitchToResult> SwitchToAsync(string modelId, CancellationToken cancellationToken = default)
    {
        var request = new SwitchToRequest { SessionId = _sessionId, ModelId = modelId };
        return await CopilotClient.InvokeRpcAsync<ModelSwitchToResult>(_rpc, "session.model.switchTo", [request], cancellationToken);
    }
}

