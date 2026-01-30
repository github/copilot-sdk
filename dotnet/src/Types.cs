/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.Extensions.AI;
using Microsoft.Extensions.Logging;

namespace GitHub.Copilot.SDK;

[JsonConverter(typeof(JsonStringEnumConverter<SystemMessageMode>))]
public enum ConnectionState
{
    [JsonStringEnumMemberName("disconnected")]
    Disconnected,
    [JsonStringEnumMemberName("connecting")]
    Connecting,
    [JsonStringEnumMemberName("connected")]
    Connected,
    [JsonStringEnumMemberName("error")]
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

    /// <summary>
    /// GitHub token to use for authentication.
    /// When provided, the token is passed to the CLI server via environment variable.
    /// This takes priority over other authentication methods.
    /// </summary>
    public string? GithubToken { get; set; }

    /// <summary>
    /// Whether to use the logged-in user for authentication.
    /// When true, the CLI server will attempt to use stored OAuth tokens or gh CLI auth.
    /// When false, only explicit tokens (GithubToken or environment variables) are used.
    /// Default: true (but defaults to false when GithubToken is provided).
    /// </summary>
    public bool? UseLoggedInUser { get; set; }
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

// ============================================================================
// User Input Handler Types
// ============================================================================

/// <summary>
/// Request for user input from the agent.
/// </summary>
public class UserInputRequest
{
    /// <summary>
    /// The question to ask the user.
    /// </summary>
    [JsonPropertyName("question")]
    public string Question { get; set; } = string.Empty;

    /// <summary>
    /// Optional choices for multiple choice questions.
    /// </summary>
    [JsonPropertyName("choices")]
    public List<string>? Choices { get; set; }

    /// <summary>
    /// Whether freeform text input is allowed.
    /// </summary>
    [JsonPropertyName("allowFreeform")]
    public bool? AllowFreeform { get; set; }
}

/// <summary>
/// Response to a user input request.
/// </summary>
public class UserInputResponse
{
    /// <summary>
    /// The user's answer.
    /// </summary>
    [JsonPropertyName("answer")]
    public string Answer { get; set; } = string.Empty;

    /// <summary>
    /// Whether the answer was freeform (not from the provided choices).
    /// </summary>
    [JsonPropertyName("wasFreeform")]
    public bool WasFreeform { get; set; }
}

/// <summary>
/// Context for a user input request invocation.
/// </summary>
public class UserInputInvocation
{
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>
/// Handler for user input requests from the agent.
/// </summary>
public delegate Task<UserInputResponse> UserInputHandler(UserInputRequest request, UserInputInvocation invocation);

// ============================================================================
// Hook Handler Types
// ============================================================================

/// <summary>
/// Context for a hook invocation.
/// </summary>
public class HookInvocation
{
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>
/// Input for a pre-tool-use hook.
/// </summary>
public class PreToolUseHookInput
{
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    [JsonPropertyName("toolName")]
    public string ToolName { get; set; } = string.Empty;

    [JsonPropertyName("toolArgs")]
    public object? ToolArgs { get; set; }
}

/// <summary>
/// Output for a pre-tool-use hook.
/// </summary>
public class PreToolUseHookOutput
{
    /// <summary>
    /// Permission decision: "allow", "deny", or "ask".
    /// </summary>
    [JsonPropertyName("permissionDecision")]
    public string? PermissionDecision { get; set; }

    [JsonPropertyName("permissionDecisionReason")]
    public string? PermissionDecisionReason { get; set; }

    [JsonPropertyName("modifiedArgs")]
    public object? ModifiedArgs { get; set; }

    [JsonPropertyName("additionalContext")]
    public string? AdditionalContext { get; set; }

    [JsonPropertyName("suppressOutput")]
    public bool? SuppressOutput { get; set; }
}

public delegate Task<PreToolUseHookOutput?> PreToolUseHandler(PreToolUseHookInput input, HookInvocation invocation);

/// <summary>
/// Input for a post-tool-use hook.
/// </summary>
public class PostToolUseHookInput
{
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    [JsonPropertyName("toolName")]
    public string ToolName { get; set; } = string.Empty;

    [JsonPropertyName("toolArgs")]
    public object? ToolArgs { get; set; }

    [JsonPropertyName("toolResult")]
    public object? ToolResult { get; set; }
}

/// <summary>
/// Output for a post-tool-use hook.
/// </summary>
public class PostToolUseHookOutput
{
    [JsonPropertyName("modifiedResult")]
    public object? ModifiedResult { get; set; }

    [JsonPropertyName("additionalContext")]
    public string? AdditionalContext { get; set; }

    [JsonPropertyName("suppressOutput")]
    public bool? SuppressOutput { get; set; }
}

public delegate Task<PostToolUseHookOutput?> PostToolUseHandler(PostToolUseHookInput input, HookInvocation invocation);

/// <summary>
/// Input for a user-prompt-submitted hook.
/// </summary>
public class UserPromptSubmittedHookInput
{
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    [JsonPropertyName("prompt")]
    public string Prompt { get; set; } = string.Empty;
}

/// <summary>
/// Output for a user-prompt-submitted hook.
/// </summary>
public class UserPromptSubmittedHookOutput
{
    [JsonPropertyName("modifiedPrompt")]
    public string? ModifiedPrompt { get; set; }

    [JsonPropertyName("additionalContext")]
    public string? AdditionalContext { get; set; }

    [JsonPropertyName("suppressOutput")]
    public bool? SuppressOutput { get; set; }
}

public delegate Task<UserPromptSubmittedHookOutput?> UserPromptSubmittedHandler(UserPromptSubmittedHookInput input, HookInvocation invocation);

/// <summary>
/// Input for a session-start hook.
/// </summary>
public class SessionStartHookInput
{
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    /// <summary>
    /// Source of the session start: "startup", "resume", or "new".
    /// </summary>
    [JsonPropertyName("source")]
    public string Source { get; set; } = string.Empty;

    [JsonPropertyName("initialPrompt")]
    public string? InitialPrompt { get; set; }
}

/// <summary>
/// Output for a session-start hook.
/// </summary>
public class SessionStartHookOutput
{
    [JsonPropertyName("additionalContext")]
    public string? AdditionalContext { get; set; }

    [JsonPropertyName("modifiedConfig")]
    public Dictionary<string, object>? ModifiedConfig { get; set; }
}

public delegate Task<SessionStartHookOutput?> SessionStartHandler(SessionStartHookInput input, HookInvocation invocation);

/// <summary>
/// Input for a session-end hook.
/// </summary>
public class SessionEndHookInput
{
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    /// <summary>
    /// Reason for session end: "complete", "error", "abort", "timeout", or "user_exit".
    /// </summary>
    [JsonPropertyName("reason")]
    public string Reason { get; set; } = string.Empty;

    [JsonPropertyName("finalMessage")]
    public string? FinalMessage { get; set; }

    [JsonPropertyName("error")]
    public string? Error { get; set; }
}

/// <summary>
/// Output for a session-end hook.
/// </summary>
public class SessionEndHookOutput
{
    [JsonPropertyName("suppressOutput")]
    public bool? SuppressOutput { get; set; }

    [JsonPropertyName("cleanupActions")]
    public List<string>? CleanupActions { get; set; }

    [JsonPropertyName("sessionSummary")]
    public string? SessionSummary { get; set; }
}

public delegate Task<SessionEndHookOutput?> SessionEndHandler(SessionEndHookInput input, HookInvocation invocation);

/// <summary>
/// Input for an error-occurred hook.
/// </summary>
public class ErrorOccurredHookInput
{
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    [JsonPropertyName("error")]
    public string Error { get; set; } = string.Empty;

    /// <summary>
    /// Context of the error: "model_call", "tool_execution", "system", or "user_input".
    /// </summary>
    [JsonPropertyName("errorContext")]
    public string ErrorContext { get; set; } = string.Empty;

    [JsonPropertyName("recoverable")]
    public bool Recoverable { get; set; }
}

/// <summary>
/// Output for an error-occurred hook.
/// </summary>
public class ErrorOccurredHookOutput
{
    [JsonPropertyName("suppressOutput")]
    public bool? SuppressOutput { get; set; }

    /// <summary>
    /// Error handling strategy: "retry", "skip", or "abort".
    /// </summary>
    [JsonPropertyName("errorHandling")]
    public string? ErrorHandling { get; set; }

    [JsonPropertyName("retryCount")]
    public int? RetryCount { get; set; }

    [JsonPropertyName("userNotification")]
    public string? UserNotification { get; set; }
}

public delegate Task<ErrorOccurredHookOutput?> ErrorOccurredHandler(ErrorOccurredHookInput input, HookInvocation invocation);

/// <summary>
/// Hook handlers configuration for a session.
/// </summary>
public class SessionHooks
{
    /// <summary>
    /// Handler called before a tool is executed.
    /// </summary>
    public PreToolUseHandler? OnPreToolUse { get; set; }

    /// <summary>
    /// Handler called after a tool has been executed.
    /// </summary>
    public PostToolUseHandler? OnPostToolUse { get; set; }

    /// <summary>
    /// Handler called when the user submits a prompt.
    /// </summary>
    public UserPromptSubmittedHandler? OnUserPromptSubmitted { get; set; }

    /// <summary>
    /// Handler called when a session starts.
    /// </summary>
    public SessionStartHandler? OnSessionStart { get; set; }

    /// <summary>
    /// Handler called when a session ends.
    /// </summary>
    public SessionEndHandler? OnSessionEnd { get; set; }

    /// <summary>
    /// Handler called when an error occurs.
    /// </summary>
    public ErrorOccurredHandler? OnErrorOccurred { get; set; }
}

[JsonConverter(typeof(JsonStringEnumConverter<SystemMessageMode>))]
public enum SystemMessageMode
{
    [JsonStringEnumMemberName("append")]
    Append,
    [JsonStringEnumMemberName("replace")]
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

/// <summary>
/// Configuration for infinite sessions with automatic context compaction and workspace persistence.
/// When enabled, sessions automatically manage context window limits through background compaction
/// and persist state to a workspace directory.
/// </summary>
public class InfiniteSessionConfig
{
    /// <summary>
    /// Whether infinite sessions are enabled. Default: true
    /// </summary>
    [JsonPropertyName("enabled")]
    public bool? Enabled { get; set; }

    /// <summary>
    /// Context utilization threshold (0.0-1.0) at which background compaction starts.
    /// Compaction runs asynchronously, allowing the session to continue processing.
    /// Default: 0.80
    /// </summary>
    [JsonPropertyName("backgroundCompactionThreshold")]
    public double? BackgroundCompactionThreshold { get; set; }

    /// <summary>
    /// Context utilization threshold (0.0-1.0) at which the session blocks until compaction completes.
    /// This prevents context overflow when compaction hasn't finished in time.
    /// Default: 0.95
    /// </summary>
    [JsonPropertyName("bufferExhaustionThreshold")]
    public double? BufferExhaustionThreshold { get; set; }
}

public class SessionConfig
{
    public string? SessionId { get; set; }
    public string? Model { get; set; }

    /// <summary>
    /// Reasoning effort level for models that support it.
    /// Valid values: "low", "medium", "high", "xhigh".
    /// Only applies to models where capabilities.supports.reasoningEffort is true.
    /// </summary>
    public string? ReasoningEffort { get; set; }

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
    /// Handler for user input requests from the agent.
    /// When provided, enables the ask_user tool for the agent to request user input.
    /// </summary>
    public UserInputHandler? OnUserInputRequest { get; set; }

    /// <summary>
    /// Hook handlers for session lifecycle events.
    /// </summary>
    public SessionHooks? Hooks { get; set; }

    /// <summary>
    /// Working directory for the session.
    /// </summary>
    public string? WorkingDirectory { get; set; }

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

    /// <summary>
    /// Directories to load skills from.
    /// </summary>
    public List<string>? SkillDirectories { get; set; }

    /// <summary>
    /// List of skill names to disable.
    /// </summary>
    public List<string>? DisabledSkills { get; set; }

    /// <summary>
    /// Infinite session configuration for persistent workspaces and automatic compaction.
    /// When enabled (default), sessions automatically manage context limits and persist state.
    /// </summary>
    public InfiniteSessionConfig? InfiniteSessions { get; set; }
}

public class ResumeSessionConfig
{
    public ICollection<AIFunction>? Tools { get; set; }
    public ProviderConfig? Provider { get; set; }

    /// <summary>
    /// Reasoning effort level for models that support it.
    /// Valid values: "low", "medium", "high", "xhigh".
    /// </summary>
    public string? ReasoningEffort { get; set; }

    /// <summary>
    /// Handler for permission requests from the server.
    /// When provided, the server will call this handler to request permission for operations.
    /// </summary>
    public PermissionHandler? OnPermissionRequest { get; set; }

    /// <summary>
    /// Handler for user input requests from the agent.
    /// When provided, enables the ask_user tool for the agent to request user input.
    /// </summary>
    public UserInputHandler? OnUserInputRequest { get; set; }

    /// <summary>
    /// Hook handlers for session lifecycle events.
    /// </summary>
    public SessionHooks? Hooks { get; set; }

    /// <summary>
    /// Working directory for the session.
    /// </summary>
    public string? WorkingDirectory { get; set; }

    /// <summary>
    /// When true, the session.resume event is not emitted.
    /// Default: false (resume event is emitted).
    /// </summary>
    public bool DisableResume { get; set; }

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

    /// <summary>
    /// Directories to load skills from.
    /// </summary>
    public List<string>? SkillDirectories { get; set; }

    /// <summary>
    /// List of skill names to disable.
    /// </summary>
    public List<string>? DisabledSkills { get; set; }
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

internal class PingRequest
{
    public string? Message { get; set; }
}

public class PingResponse
{
    public string Message { get; set; } = string.Empty;
    public long Timestamp { get; set; }
    public int? ProtocolVersion { get; set; }
}

/// <summary>
/// Response from status.get
/// </summary>
public class GetStatusResponse
{
    /// <summary>Package version (e.g., "1.0.0")</summary>
    [JsonPropertyName("version")]
    public string Version { get; set; } = string.Empty;

    /// <summary>Protocol version for SDK compatibility</summary>
    [JsonPropertyName("protocolVersion")]
    public int ProtocolVersion { get; set; }
}

/// <summary>
/// Response from auth.getStatus
/// </summary>
public class GetAuthStatusResponse
{
    /// <summary>Whether the user is authenticated</summary>
    [JsonPropertyName("isAuthenticated")]
    public bool IsAuthenticated { get; set; }

    /// <summary>Authentication type (user, env, gh-cli, hmac, api-key, token)</summary>
    [JsonPropertyName("authType")]
    public string? AuthType { get; set; }

    /// <summary>GitHub host URL</summary>
    [JsonPropertyName("host")]
    public string? Host { get; set; }

    /// <summary>User login name</summary>
    [JsonPropertyName("login")]
    public string? Login { get; set; }

    /// <summary>Human-readable status message</summary>
    [JsonPropertyName("statusMessage")]
    public string? StatusMessage { get; set; }
}

/// <summary>
/// Model vision-specific limits
/// </summary>
public class ModelVisionLimits
{
    [JsonPropertyName("supported_media_types")]
    public List<string> SupportedMediaTypes { get; set; } = new();

    [JsonPropertyName("max_prompt_images")]
    public int MaxPromptImages { get; set; }

    [JsonPropertyName("max_prompt_image_size")]
    public int MaxPromptImageSize { get; set; }
}

/// <summary>
/// Model limits
/// </summary>
public class ModelLimits
{
    [JsonPropertyName("max_prompt_tokens")]
    public int? MaxPromptTokens { get; set; }

    [JsonPropertyName("max_context_window_tokens")]
    public int MaxContextWindowTokens { get; set; }

    [JsonPropertyName("vision")]
    public ModelVisionLimits? Vision { get; set; }
}

/// <summary>
/// Model support flags
/// </summary>
public class ModelSupports
{
    [JsonPropertyName("vision")]
    public bool Vision { get; set; }

    /// <summary>
    /// Whether this model supports reasoning effort configuration.
    /// </summary>
    [JsonPropertyName("reasoningEffort")]
    public bool ReasoningEffort { get; set; }
}

/// <summary>
/// Model capabilities and limits
/// </summary>
public class ModelCapabilities
{
    [JsonPropertyName("supports")]
    public ModelSupports Supports { get; set; } = new();

    [JsonPropertyName("limits")]
    public ModelLimits Limits { get; set; } = new();
}

/// <summary>
/// Model policy state
/// </summary>
public class ModelPolicy
{
    [JsonPropertyName("state")]
    public string State { get; set; } = string.Empty;

    [JsonPropertyName("terms")]
    public string Terms { get; set; } = string.Empty;
}

/// <summary>
/// Model billing information
/// </summary>
public class ModelBilling
{
    [JsonPropertyName("multiplier")]
    public double Multiplier { get; set; }
}

/// <summary>
/// Information about an available model
/// </summary>
public class ModelInfo
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

    /// <summary>Policy state</summary>
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

/// <summary>
/// Response from models.list
/// </summary>
public class GetModelsResponse
{
    [JsonPropertyName("models")]
    public List<ModelInfo> Models { get; set; } = new();
}

[JsonSourceGenerationOptions(
    JsonSerializerDefaults.Web,
    AllowOutOfOrderMetadataProperties = true,
    NumberHandling = JsonNumberHandling.AllowReadingFromString,
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
[JsonSerializable(typeof(AzureOptions))]
[JsonSerializable(typeof(CustomAgentConfig))]
[JsonSerializable(typeof(GetAuthStatusResponse))]
[JsonSerializable(typeof(GetModelsResponse))]
[JsonSerializable(typeof(GetStatusResponse))]
[JsonSerializable(typeof(McpLocalServerConfig))]
[JsonSerializable(typeof(McpRemoteServerConfig))]
[JsonSerializable(typeof(MessageOptions))]
[JsonSerializable(typeof(ModelBilling))]
[JsonSerializable(typeof(ModelCapabilities))]
[JsonSerializable(typeof(ModelInfo))]
[JsonSerializable(typeof(ModelLimits))]
[JsonSerializable(typeof(ModelPolicy))]
[JsonSerializable(typeof(ModelSupports))]
[JsonSerializable(typeof(ModelVisionLimits))]
[JsonSerializable(typeof(PermissionRequest))]
[JsonSerializable(typeof(PermissionRequestResult))]
[JsonSerializable(typeof(PingRequest))]
[JsonSerializable(typeof(PingResponse))]
[JsonSerializable(typeof(ProviderConfig))]
[JsonSerializable(typeof(SessionMetadata))]
[JsonSerializable(typeof(SystemMessageConfig))]
[JsonSerializable(typeof(ToolBinaryResult))]
[JsonSerializable(typeof(ToolInvocation))]
[JsonSerializable(typeof(ToolResultObject))]
[JsonSerializable(typeof(JsonElement))]
[JsonSerializable(typeof(JsonElement?))]
internal partial class TypesJsonContext : JsonSerializerContext;
