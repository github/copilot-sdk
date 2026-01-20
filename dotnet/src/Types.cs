/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json.Serialization;
using Microsoft.Extensions.AI;
using Microsoft.Extensions.Logging;

namespace GitHub.Copilot.SDK;

public enum ConnectionState
{
    Disconnected,
    Connecting,
    Connected,
    Error
}

public class CopilotClientOptions
{
    public string? CliPath { get; set; }
    public string[]? CliArgs { get; set; }
    public string? Cwd { get; set; }
    public int Port { get; set; }
    public bool UseStdio { get; set; } = true;
    public string? CliUrl { get; set; }
    public string LogLevel { get; set; } = "info";
    public bool AutoStart { get; set; } = true;
    public bool AutoRestart { get; set; } = true;
    public IReadOnlyDictionary<string, string>? Environment { get; set; }
    public ILogger? Logger { get; set; }
}

public class ToolBinaryResult
{
    [JsonPropertyName("data")]
    public string Data { get; set; } = string.Empty;

    [JsonPropertyName("mimeType")]
    public string MimeType { get; set; } = string.Empty;

    [JsonPropertyName("type")]
    public string Type { get; set; } = string.Empty;

    [JsonPropertyName("description")]
    public string? Description { get; set; }
}

public class ToolResultObject
{
    [JsonPropertyName("textResultForLlm")]
    public string TextResultForLlm { get; set; } = string.Empty;

    [JsonPropertyName("binaryResultsForLlm")]
    public List<ToolBinaryResult>? BinaryResultsForLlm { get; set; }

    [JsonPropertyName("resultType")]
    public string ResultType { get; set; } = "success";

    [JsonPropertyName("error")]
    public string? Error { get; set; }

    [JsonPropertyName("sessionLog")]
    public string? SessionLog { get; set; }

    [JsonPropertyName("toolTelemetry")]
    public Dictionary<string, object>? ToolTelemetry { get; set; }
}

public class ToolInvocation
{
    public string SessionId { get; set; } = string.Empty;
    public string ToolCallId { get; set; } = string.Empty;
    public string ToolName { get; set; } = string.Empty;
    public object? Arguments { get; set; }
}

public delegate Task<object?> ToolHandler(ToolInvocation invocation);

public class PermissionRequest
{
    [JsonPropertyName("kind")]
    public string Kind { get; set; } = string.Empty;

    [JsonPropertyName("toolCallId")]
    public string? ToolCallId { get; set; }

    [JsonExtensionData]
    public Dictionary<string, object>? ExtensionData { get; set; }
}

public class PermissionRequestResult
{
    [JsonPropertyName("kind")]
    public string Kind { get; set; } = string.Empty;

    [JsonPropertyName("rules")]
    public List<object>? Rules { get; set; }
}

public class PermissionInvocation
{
    public string SessionId { get; set; } = string.Empty;
}

public delegate Task<PermissionRequestResult> PermissionHandler(PermissionRequest request, PermissionInvocation invocation);

public enum SystemMessageMode
{
    Append,
    Replace
}

public class SystemMessageConfig
{
    public SystemMessageMode? Mode { get; set; }
    public string? Content { get; set; }
}

public class ProviderConfig
{
    [JsonPropertyName("type")]
    public string? Type { get; set; }

    [JsonPropertyName("wireApi")]
    public string? WireApi { get; set; }

    [JsonPropertyName("baseUrl")]
    public string BaseUrl { get; set; } = string.Empty;

    [JsonPropertyName("apiKey")]
    public string? ApiKey { get; set; }

    /// <summary>
    /// Bearer token for authentication. Sets the Authorization header directly.
    /// Use this for services requiring bearer token auth instead of API key.
    /// Takes precedence over ApiKey when both are set.
    /// </summary>
    [JsonPropertyName("bearerToken")]
    public string? BearerToken { get; set; }

    [JsonPropertyName("azure")]
    public AzureOptions? Azure { get; set; }
}

public class AzureOptions
{
    [JsonPropertyName("apiVersion")]
    public string? ApiVersion { get; set; }
}

// ============================================================================
// MCP Server Configuration Types
// ============================================================================

/// <summary>
/// Configuration for a local/stdio MCP server.
/// </summary>
public class McpLocalServerConfig
{
    /// <summary>
    /// List of tools to include from this server. Empty list means none. Use "*" for all.
    /// </summary>
    [JsonPropertyName("tools")]
    public List<string> Tools { get; set; } = new();

    /// <summary>
    /// Server type. Defaults to "local".
    /// </summary>
    [JsonPropertyName("type")]
    public string? Type { get; set; }

    /// <summary>
    /// Optional timeout in milliseconds for tool calls to this server.
    /// </summary>
    [JsonPropertyName("timeout")]
    public int? Timeout { get; set; }

    /// <summary>
    /// Command to run the MCP server.
    /// </summary>
    [JsonPropertyName("command")]
    public string Command { get; set; } = string.Empty;

    /// <summary>
    /// Arguments to pass to the command.
    /// </summary>
    [JsonPropertyName("args")]
    public List<string> Args { get; set; } = new();

    /// <summary>
    /// Environment variables to pass to the server.
    /// </summary>
    [JsonPropertyName("env")]
    public Dictionary<string, string>? Env { get; set; }

    /// <summary>
    /// Working directory for the server process.
    /// </summary>
    [JsonPropertyName("cwd")]
    public string? Cwd { get; set; }
}

/// <summary>
/// Configuration for a remote MCP server (HTTP or SSE).
/// </summary>
public class McpRemoteServerConfig
{
    /// <summary>
    /// List of tools to include from this server. Empty list means none. Use "*" for all.
    /// </summary>
    [JsonPropertyName("tools")]
    public List<string> Tools { get; set; } = new();

    /// <summary>
    /// Server type. Must be "http" or "sse".
    /// </summary>
    [JsonPropertyName("type")]
    public string Type { get; set; } = "http";

    /// <summary>
    /// Optional timeout in milliseconds for tool calls to this server.
    /// </summary>
    [JsonPropertyName("timeout")]
    public int? Timeout { get; set; }

    /// <summary>
    /// URL of the remote server.
    /// </summary>
    [JsonPropertyName("url")]
    public string Url { get; set; } = string.Empty;

    /// <summary>
    /// Optional HTTP headers to include in requests.
    /// </summary>
    [JsonPropertyName("headers")]
    public Dictionary<string, string>? Headers { get; set; }
}

// ============================================================================
// Custom Agent Configuration Types
// ============================================================================

/// <summary>
/// Configuration for a custom agent.
/// </summary>
public class CustomAgentConfig
{
    /// <summary>
    /// Unique name of the custom agent.
    /// </summary>
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;

    /// <summary>
    /// Display name for UI purposes.
    /// </summary>
    [JsonPropertyName("displayName")]
    public string? DisplayName { get; set; }

    /// <summary>
    /// Description of what the agent does.
    /// </summary>
    [JsonPropertyName("description")]
    public string? Description { get; set; }

    /// <summary>
    /// List of tool names the agent can use. Null for all tools.
    /// </summary>
    [JsonPropertyName("tools")]
    public List<string>? Tools { get; set; }

    /// <summary>
    /// The prompt content for the agent.
    /// </summary>
    [JsonPropertyName("prompt")]
    public string Prompt { get; set; } = string.Empty;

    /// <summary>
    /// MCP servers specific to this agent.
    /// </summary>
    [JsonPropertyName("mcpServers")]
    public Dictionary<string, object>? McpServers { get; set; }

    /// <summary>
    /// Whether the agent should be available for model inference.
    /// </summary>
    [JsonPropertyName("infer")]
    public bool? Infer { get; set; }
}

public class SessionConfig
{
    public string? SessionId { get; set; }
    public string? Model { get; set; }

    /// <summary>
    /// Override the default configuration directory location.
    /// When specified, the session will use this directory for storing config and state.
    /// </summary>
    public string? ConfigDir { get; set; }

    public ICollection<AIFunction>? Tools { get; set; }
    public SystemMessageConfig? SystemMessage { get; set; }
    public List<string>? AvailableTools { get; set; }
    public List<string>? ExcludedTools { get; set; }
    public ProviderConfig? Provider { get; set; }

    /// <summary>
    /// Handler for permission requests from the server.
    /// When provided, the server will call this handler to request permission for operations.
    /// </summary>
    public PermissionHandler? OnPermissionRequest { get; set; }

    /// <summary>
    /// Enable streaming of assistant message and reasoning chunks.
    /// When true, assistant.message_delta and assistant.reasoning_delta events
    /// with deltaContent are sent as the response is generated.
    /// </summary>
    public bool Streaming { get; set; }

    /// <summary>
    /// MCP server configurations for the session.
    /// Keys are server names, values are server configurations (McpLocalServerConfig or McpRemoteServerConfig).
    /// </summary>
    public Dictionary<string, object>? McpServers { get; set; }

    /// <summary>
    /// Custom agent configurations for the session.
    /// </summary>
    public List<CustomAgentConfig>? CustomAgents { get; set; }
}

public class ResumeSessionConfig
{
    public ICollection<AIFunction>? Tools { get; set; }
    public ProviderConfig? Provider { get; set; }

    /// <summary>
    /// Handler for permission requests from the server.
    /// When provided, the server will call this handler to request permission for operations.
    /// </summary>
    public PermissionHandler? OnPermissionRequest { get; set; }

    /// <summary>
    /// Enable streaming of assistant message and reasoning chunks.
    /// When true, assistant.message_delta and assistant.reasoning_delta events
    /// with deltaContent are sent as the response is generated.
    /// </summary>
    public bool Streaming { get; set; }

    /// <summary>
    /// MCP server configurations for the session.
    /// Keys are server names, values are server configurations (McpLocalServerConfig or McpRemoteServerConfig).
    /// </summary>
    public Dictionary<string, object>? McpServers { get; set; }

    /// <summary>
    /// Custom agent configurations for the session.
    /// </summary>
    public List<CustomAgentConfig>? CustomAgents { get; set; }
}

public class MessageOptions
{
    public string Prompt { get; set; } = string.Empty;
    public List<UserMessageDataAttachmentsItem>? Attachments { get; set; }
    public string? Mode { get; set; }
}

public delegate void SessionEventHandler(SessionEvent sessionEvent);

public class SessionMetadata
{
    public string SessionId { get; set; } = string.Empty;
    public DateTime StartTime { get; set; }
    public DateTime ModifiedTime { get; set; }
    public string? Summary { get; set; }
    public bool IsRemote { get; set; }
}

public class PingResponse
{
    public string Message { get; set; } = string.Empty;
    public long Timestamp { get; set; }
    public int? ProtocolVersion { get; set; }
}
