/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.ComponentModel;
using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;
using System.Text.Json;
using System.Text.Json.Serialization;
using GitHub.Copilot.SDK.Rpc;
using Microsoft.Extensions.AI;
using Microsoft.Extensions.Logging;

namespace GitHub.Copilot.SDK;

/// <summary>
/// Represents the connection state of the Copilot client.
/// </summary>
[JsonConverter(typeof(JsonStringEnumConverter<ConnectionState>))]
public enum ConnectionState
{
    /// <summary>The client is not connected to the server.</summary>
    [JsonStringEnumMemberName("disconnected")]
    Disconnected,
    /// <summary>The client is establishing a connection to the server.</summary>
    [JsonStringEnumMemberName("connecting")]
    Connecting,
    /// <summary>The client is connected and ready to communicate.</summary>
    [JsonStringEnumMemberName("connected")]
    Connected,
    /// <summary>The connection is in an error state.</summary>
    [JsonStringEnumMemberName("error")]
    Error
}

/// <summary>
/// Configuration options for creating a <see cref="CopilotClient"/> instance.
/// </summary>
public class CopilotClientOptions
{
    /// <summary>
    /// Initializes a new instance of the <see cref="CopilotClientOptions"/> class.
    /// </summary>
    public CopilotClientOptions() { }

    /// <summary>
    /// Initializes a new instance of the <see cref="CopilotClientOptions"/> class
    /// by copying the properties of the specified instance.
    /// </summary>
    protected CopilotClientOptions(CopilotClientOptions? other)
    {
        if (other is null) return;

        AutoStart = other.AutoStart;
#pragma warning disable CS0618 // Obsolete member
        AutoRestart = other.AutoRestart;
#pragma warning restore CS0618
        CliArgs = (string[]?)other.CliArgs?.Clone();
        CliPath = other.CliPath;
        CliUrl = other.CliUrl;
        Cwd = other.Cwd;
        Environment = other.Environment;
        GitHubToken = other.GitHubToken;
        Logger = other.Logger;
        LogLevel = other.LogLevel;
        Port = other.Port;
        Telemetry = other.Telemetry;
        UseLoggedInUser = other.UseLoggedInUser;
        UseStdio = other.UseStdio;
        OnListModels = other.OnListModels;
        SessionFs = other.SessionFs;
    }

    /// <summary>
    /// Path to the Copilot CLI executable. If not specified, uses the bundled CLI from the SDK.
    /// </summary>
    public string? CliPath { get; set; }
    /// <summary>
    /// Additional command-line arguments to pass to the CLI process.
    /// </summary>
    public string[]? CliArgs { get; set; }
    /// <summary>
    /// Working directory for the CLI process.
    /// </summary>
    public string? Cwd { get; set; }
    /// <summary>
    /// Port number for the CLI server when not using stdio transport.
    /// </summary>
    public int Port { get; set; }
    /// <summary>
    /// Whether to use stdio transport for communication with the CLI server.
    /// </summary>
    public bool UseStdio { get; set; } = true;
    /// <summary>
    /// URL of an existing CLI server to connect to instead of starting a new one.
    /// </summary>
    public string? CliUrl { get; set; }
    /// <summary>
    /// Log level for the CLI server (e.g., "info", "debug", "warn", "error").
    /// </summary>
    public string LogLevel { get; set; } = "info";
    /// <summary>
    /// Whether to automatically start the CLI server if it is not already running.
    /// </summary>
    public bool AutoStart { get; set; } = true;
    /// <summary>
    /// Obsolete. This option has no effect.
    /// </summary>
    [Obsolete("AutoRestart has no effect and will be removed in a future release.")]
    public bool AutoRestart { get; set; }
    /// <summary>
    /// Environment variables to pass to the CLI process.
    /// </summary>
    public IReadOnlyDictionary<string, string>? Environment { get; set; }
    /// <summary>
    /// Logger instance for SDK diagnostic output.
    /// </summary>
    public ILogger? Logger { get; set; }

    /// <summary>
    /// GitHub token to use for authentication.
    /// When provided, the token is passed to the CLI server via environment variable.
    /// This takes priority over other authentication methods.
    /// </summary>
    public string? GitHubToken { get; set; }

    /// <summary>
    /// Obsolete. Use <see cref="GitHubToken"/> instead.
    /// </summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    [Obsolete("Use GitHubToken instead.", error: false)]
    public string? GithubToken
    {
        get => GitHubToken;
        set => GitHubToken = value;
    }

    /// <summary>
    /// Whether to use the logged-in user for authentication.
    /// When true, the CLI server will attempt to use stored OAuth tokens or gh CLI auth.
    /// When false, only explicit tokens (GitHubToken or environment variables) are used.
    /// Default: true (but defaults to false when GitHubToken is provided).
    /// </summary>
    public bool? UseLoggedInUser { get; set; }

    /// <summary>
    /// Custom handler for listing available models.
    /// When provided, <c>ListModelsAsync()</c> calls this handler instead of
    /// querying the CLI server. Useful in BYOK mode to return models
    /// available from your custom provider.
    /// </summary>
    public Func<CancellationToken, Task<IList<ModelInfo>>>? OnListModels { get; set; }

    /// <summary>
    /// Custom session filesystem provider configuration.
    /// When set, the client registers as the session filesystem provider on connect,
    /// routing session-scoped file I/O through per-session handlers created via
    /// <see cref="SessionConfig.CreateSessionFsHandler"/> or <see cref="ResumeSessionConfig.CreateSessionFsHandler"/>.
    /// </summary>
    public SessionFsConfig? SessionFs { get; set; }

    /// <summary>
    /// OpenTelemetry configuration for the CLI server.
    /// When set to a non-<see langword="null"/> instance, the CLI server is started with OpenTelemetry instrumentation enabled.
    /// </summary>
    public TelemetryConfig? Telemetry { get; set; }

    /// <summary>
    /// Creates a shallow clone of this <see cref="CopilotClientOptions"/> instance.
    /// </summary>
    /// <remarks>
    /// Mutable collection properties are copied into new collection instances so that modifications
    /// to those collections on the clone do not affect the original.
    /// Other reference-type properties (for example delegates and the logger) are not
    /// deep-cloned; the original and the clone will share those objects.
    /// </remarks>
    public virtual CopilotClientOptions Clone()
    {
        return new(this);
    }
}

/// <summary>
/// OpenTelemetry configuration for the Copilot CLI server.
/// </summary>
public sealed class TelemetryConfig
{
    /// <summary>
    /// OTLP exporter endpoint URL.
    /// </summary>
    /// <remarks>
    /// Maps to the <c>OTEL_EXPORTER_OTLP_ENDPOINT</c> environment variable.
    /// </remarks>
    public string? OtlpEndpoint { get; set; }

    /// <summary>
    /// File path for the file exporter.
    /// </summary>
    /// <remarks>
    /// Maps to the <c>COPILOT_OTEL_FILE_EXPORTER_PATH</c> environment variable.
    /// </remarks>
    public string? FilePath { get; set; }

    /// <summary>
    /// Exporter type (<c>"otlp-http"</c> or <c>"file"</c>).
    /// </summary>
    /// <remarks>
    /// Maps to the <c>COPILOT_OTEL_EXPORTER_TYPE</c> environment variable.
    /// </remarks>
    public string? ExporterType { get; set; }

    /// <summary>
    /// Source name for telemetry spans.
    /// </summary>
    /// <remarks>
    /// Maps to the <c>COPILOT_OTEL_SOURCE_NAME</c> environment variable.
    /// </remarks>
    public string? SourceName { get; set; }

    /// <summary>
    /// Whether to capture message content as part of telemetry.
    /// </summary>
    /// <remarks>
    /// Maps to the <c>OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT</c> environment variable.
    /// </remarks>
    public bool? CaptureContent { get; set; }
}

/// <summary>
/// Configuration for a custom session filesystem provider.
/// </summary>
public sealed class SessionFsConfig
{
    /// <summary>
    /// Initial working directory for sessions (user's project directory).
    /// </summary>
    public required string InitialCwd { get; init; }

    /// <summary>
    /// Path within each session's SessionFs where the runtime stores
    /// session-scoped files (events, workspace, checkpoints, and temp files).
    /// </summary>
    public required string SessionStatePath { get; init; }

    /// <summary>
    /// Path conventions used by this filesystem provider.
    /// </summary>
    public required SessionFsSetProviderConventions Conventions { get; init; }
}

/// <summary>
/// Represents a binary result returned by a tool invocation.
/// </summary>
public class ToolBinaryResult
{
    /// <summary>
    /// Base64-encoded binary data.
    /// </summary>
    [JsonPropertyName("data")]
    public string Data { get; set; } = string.Empty;

    /// <summary>
    /// MIME type of the binary data (e.g., "image/png").
    /// </summary>
    [JsonPropertyName("mimeType")]
    public string MimeType { get; set; } = string.Empty;

    /// <summary>
    /// Type identifier for the binary result.
    /// </summary>
    [JsonPropertyName("type")]
    public string Type { get; set; } = string.Empty;

    /// <summary>
    /// Optional human-readable description of the binary result.
    /// </summary>
    [JsonPropertyName("description")]
    public string? Description { get; set; }
}

/// <summary>
/// Represents the structured result of a tool execution.
/// </summary>
public class ToolResultObject
{
    /// <summary>
    /// Text result to be consumed by the language model.
    /// </summary>
    [JsonPropertyName("textResultForLlm")]
    public string TextResultForLlm { get; set; } = string.Empty;

    /// <summary>
    /// Binary results (e.g., images) to be consumed by the language model.
    /// </summary>
    [JsonPropertyName("binaryResultsForLlm")]
    public IList<ToolBinaryResult>? BinaryResultsForLlm { get; set; }

    /// <summary>
    /// Result type indicator.
    /// <list type="bullet">
    /// <item><description><c>"success"</c> — the tool executed successfully.</description></item>
    /// <item><description><c>"failure"</c> — the tool encountered an error.</description></item>
    /// <item><description><c>"rejected"</c> — the tool invocation was rejected.</description></item>
    /// <item><description><c>"denied"</c> — the tool invocation was denied by a permission check.</description></item>
    /// </list>
    /// </summary>
    [JsonPropertyName("resultType")]
    public string ResultType { get; set; } = "success";

    /// <summary>
    /// Error message if the tool execution failed.
    /// </summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }

    /// <summary>
    /// Log entry for the session history.
    /// </summary>
    [JsonPropertyName("sessionLog")]
    public string? SessionLog { get; set; }

    /// <summary>
    /// Custom telemetry data associated with the tool execution.
    /// </summary>
    [JsonPropertyName("toolTelemetry")]
    public IDictionary<string, object>? ToolTelemetry { get; set; }

    /// <summary>
    /// Converts the result of an <see cref="AIFunction"/> invocation into a
    /// <see cref="ToolResultObject"/>. Handles <see cref="ToolResultAIContent"/>,
    /// <see cref="AIContent"/>, and falls back to JSON serialization.
    /// </summary>
    internal static ToolResultObject ConvertFromInvocationResult(object? result, JsonSerializerOptions jsonOptions)
    {
        if (result is ToolResultAIContent trac)
        {
            return trac.Result;
        }

        if (TryConvertFromAIContent(result) is { } aiConverted)
        {
            return aiConverted;
        }

        return new ToolResultObject
        {
            ResultType = "success",
            TextResultForLlm = result is JsonElement { ValueKind: JsonValueKind.String } je
                ? je.GetString()!
                : JsonSerializer.Serialize(result, jsonOptions.GetTypeInfo(typeof(object))),
        };
    }

    /// <summary>
    /// Attempts to convert a result from an <see cref="AIFunction"/> invocation into a
    /// <see cref="ToolResultObject"/>. Handles <see cref="TextContent"/>,
    /// <see cref="DataContent"/>, and collections of <see cref="AIContent"/>.
    /// Returns <see langword="null"/> if the value is not a recognized <see cref="AIContent"/> type.
    /// </summary>
    internal static ToolResultObject? TryConvertFromAIContent(object? result)
    {
        if (result is AIContent singleContent)
        {
            return ConvertAIContents([singleContent]);
        }

        if (result is IEnumerable<AIContent> contentList)
        {
            return ConvertAIContents(contentList);
        }

        return null;
    }

    private static ToolResultObject ConvertAIContents(IEnumerable<AIContent> contents)
    {
        List<string>? textParts = null;
        List<ToolBinaryResult>? binaryResults = null;

        foreach (var content in contents)
        {
            switch (content)
            {
                case TextContent textContent:
                    if (textContent.Text is { } text)
                    {
                        (textParts ??= []).Add(text);
                    }
                    break;

                case DataContent dataContent:
                    (binaryResults ??= []).Add(new ToolBinaryResult
                    {
                        Data = dataContent.Base64Data.ToString(),
                        MimeType = dataContent.MediaType ?? "application/octet-stream",
                        Type = dataContent.HasTopLevelMediaType("image") ? "image" : "resource",
                    });
                    break;

                default:
                    (textParts ??= []).Add(SerializeAIContent(content));
                    break;
            }
        }

        return new ToolResultObject
        {
            TextResultForLlm = textParts is not null ? string.Join("\n", textParts) : "",
            ResultType = "success",
            BinaryResultsForLlm = binaryResults,
        };
    }

    private static string SerializeAIContent(AIContent content) =>
        JsonSerializer.Serialize(content, AIJsonUtilities.DefaultOptions.GetTypeInfo(typeof(AIContent)));
}

/// <summary>
/// Contains context for a tool invocation callback.
/// </summary>
public class ToolInvocation
{
    /// <summary>
    /// Identifier of the session that triggered the tool call.
    /// </summary>
    public string SessionId { get; set; } = string.Empty;
    /// <summary>
    /// Unique identifier of this specific tool call.
    /// </summary>
    public string ToolCallId { get; set; } = string.Empty;
    /// <summary>
    /// Name of the tool being invoked.
    /// </summary>
    public string ToolName { get; set; } = string.Empty;
    /// <summary>
    /// Arguments passed to the tool by the language model.
    /// </summary>
    public object? Arguments { get; set; }
}

/// <summary>
/// Delegate for handling tool invocations and returning a result.
/// </summary>
public delegate Task<object?> ToolHandler(ToolInvocation invocation);

/// <summary>Describes the kind of a permission request result.</summary>
[JsonConverter(typeof(PermissionRequestResultKind.Converter))]
[DebuggerDisplay("{Value,nq}")]
public readonly struct PermissionRequestResultKind : IEquatable<PermissionRequestResultKind>
{
    /// <summary>Gets the kind indicating the permission was approved.</summary>
    public static PermissionRequestResultKind Approved { get; } = new("approved");

    /// <summary>Gets the kind indicating the permission was denied by rules.</summary>
    public static PermissionRequestResultKind DeniedByRules { get; } = new("denied-by-rules");

    /// <summary>Gets the kind indicating the permission was denied because no approval rule was found and the user could not be prompted.</summary>
    public static PermissionRequestResultKind DeniedCouldNotRequestFromUser { get; } = new("denied-no-approval-rule-and-could-not-request-from-user");

    /// <summary>Gets the kind indicating the permission was denied interactively by the user.</summary>
    public static PermissionRequestResultKind DeniedInteractivelyByUser { get; } = new("denied-interactively-by-user");

    /// <summary>Gets the kind indicating the permission was denied interactively by the user.</summary>
    public static PermissionRequestResultKind NoResult { get; } = new("no-result");

    /// <summary>Gets the underlying string value of this <see cref="PermissionRequestResultKind"/>.</summary>
    public string Value => _value ?? string.Empty;

    private readonly string? _value;

    /// <summary>Initializes a new instance of the <see cref="PermissionRequestResultKind"/> struct.</summary>
    /// <param name="value">The string value for this kind.</param>
    [JsonConstructor]
    public PermissionRequestResultKind(string value) => _value = value;

    /// <inheritdoc/>
    public static bool operator ==(PermissionRequestResultKind left, PermissionRequestResultKind right) => left.Equals(right);

    /// <inheritdoc/>
    public static bool operator !=(PermissionRequestResultKind left, PermissionRequestResultKind right) => !left.Equals(right);

    /// <inheritdoc/>
    public override bool Equals([NotNullWhen(true)] object? obj) => obj is PermissionRequestResultKind other && Equals(other);

    /// <inheritdoc/>
    public bool Equals(PermissionRequestResultKind other) => string.Equals(Value, other.Value, StringComparison.OrdinalIgnoreCase);

    /// <inheritdoc/>
    public override int GetHashCode() => StringComparer.OrdinalIgnoreCase.GetHashCode(Value);

    /// <inheritdoc/>
    public override string ToString() => Value;

    /// <summary>Provides a <see cref="JsonConverter{PermissionRequestResultKind}"/> for serializing <see cref="PermissionRequestResultKind"/> instances.</summary>
    [EditorBrowsable(EditorBrowsableState.Never)]
    public sealed class Converter : JsonConverter<PermissionRequestResultKind>
    {
        /// <inheritdoc/>
        public override PermissionRequestResultKind Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            if (reader.TokenType != JsonTokenType.String)
            {
                throw new JsonException("Expected string for PermissionRequestResultKind.");
            }

            var value = reader.GetString();
            if (value is null)
            {
                throw new JsonException("PermissionRequestResultKind value cannot be null.");
            }

            return new PermissionRequestResultKind(value);
        }

        /// <inheritdoc/>
        public override void Write(Utf8JsonWriter writer, PermissionRequestResultKind value, JsonSerializerOptions options) =>
            writer.WriteStringValue(value.Value);
    }
}

/// <summary>
/// Result of a permission request evaluation.
/// </summary>
public class PermissionRequestResult
{
    /// <summary>
    /// Permission decision kind.
    /// <list type="bullet">
    /// <item><description><c>"approved"</c> — the operation is allowed.</description></item>
    /// <item><description><c>"denied-by-rules"</c> — denied by configured permission rules.</description></item>
    /// <item><description><c>"denied-interactively-by-user"</c> — the user explicitly denied the request.</description></item>
    /// <item><description><c>"denied-no-approval-rule-and-could-not-request-from-user"</c> — no rule matched and user approval was unavailable.</description></item>
    /// <item><description><c>"no-result"</c> — leave the pending permission request unanswered.</description></item>
    /// </list>
    /// </summary>
    [JsonPropertyName("kind")]
    public PermissionRequestResultKind Kind { get; set; }

    /// <summary>
    /// Permission rules to apply for the decision.
    /// </summary>
    [JsonPropertyName("rules")]
    public IList<object>? Rules { get; set; }
}

/// <summary>
/// Contains context for a permission request callback.
/// </summary>
public class PermissionInvocation
{
    /// <summary>
    /// Identifier of the session that triggered the permission request.
    /// </summary>
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>
/// Delegate for handling permission requests and returning a decision.
/// </summary>
public delegate Task<PermissionRequestResult> PermissionRequestHandler(PermissionRequest request, PermissionInvocation invocation);

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
    public IList<string>? Choices { get; set; }

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
    /// <summary>
    /// Identifier of the session that triggered the user input request.
    /// </summary>
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>
/// Handler for user input requests from the agent.
/// </summary>
public delegate Task<UserInputResponse> UserInputHandler(UserInputRequest request, UserInputInvocation invocation);

// ============================================================================
// Command Handler Types
// ============================================================================

/// <summary>
/// Defines a slash-command that users can invoke from the CLI TUI.
/// </summary>
public class CommandDefinition
{
    /// <summary>
    /// Command name (without leading <c>/</c>). For example, <c>"deploy"</c>.
    /// </summary>
    public required string Name { get; set; }

    /// <summary>
    /// Human-readable description shown in the command completion UI.
    /// </summary>
    public string? Description { get; set; }

    /// <summary>
    /// Handler invoked when the command is executed.
    /// </summary>
    public required CommandHandler Handler { get; set; }
}

/// <summary>
/// Context passed to a <see cref="CommandHandler"/> when a command is executed.
/// </summary>
public class CommandContext
{
    /// <summary>
    /// Session ID where the command was invoked.
    /// </summary>
    public string SessionId { get; set; } = string.Empty;

    /// <summary>
    /// The full command text (e.g., <c>/deploy production</c>).
    /// </summary>
    public string Command { get; set; } = string.Empty;

    /// <summary>
    /// Command name without leading <c>/</c>.
    /// </summary>
    public string CommandName { get; set; } = string.Empty;

    /// <summary>
    /// Raw argument string after the command name.
    /// </summary>
    public string Args { get; set; } = string.Empty;
}

/// <summary>
/// Delegate for handling slash-command executions.
/// </summary>
public delegate Task CommandHandler(CommandContext context);

// ============================================================================
// Elicitation Types (UI — client → server)
// ============================================================================

/// <summary>
/// JSON Schema describing the form fields to present for an elicitation dialog.
/// </summary>
public class ElicitationSchema
{
    /// <summary>
    /// Schema type indicator (always <c>"object"</c>).
    /// </summary>
    [JsonPropertyName("type")]
    public string Type { get; set; } = "object";

    /// <summary>
    /// Form field definitions, keyed by field name.
    /// </summary>
    [JsonPropertyName("properties")]
    public IDictionary<string, object> Properties { get => field ??= new Dictionary<string, object>(); set; }

    /// <summary>
    /// List of required field names.
    /// </summary>
    [JsonPropertyName("required")]
    public IList<string>? Required { get; set; }
}

/// <summary>
/// Parameters for an elicitation request sent from the SDK to the server.
/// </summary>
public class ElicitationParams
{
    /// <summary>
    /// Message describing what information is needed from the user.
    /// </summary>
    public required string Message { get; set; }

    /// <summary>
    /// JSON Schema describing the form fields to present.
    /// </summary>
    public required ElicitationSchema RequestedSchema { get; set; }
}

/// <summary>
/// Result returned from an elicitation dialog.
/// </summary>
public class ElicitationResult
{
    /// <summary>
    /// User action: <c>"accept"</c> (submitted), <c>"decline"</c> (rejected), or <c>"cancel"</c> (dismissed).
    /// </summary>
    public UIElicitationResponseAction Action { get; set; }

    /// <summary>
    /// Form values submitted by the user (present when <see cref="Action"/> is <c>Accept</c>).
    /// </summary>
    public IDictionary<string, object>? Content { get; set; }
}

/// <summary>
/// Options for the <see cref="ISessionUiApi.InputAsync"/> convenience method.
/// </summary>
public class InputOptions
{
    /// <summary>Title label for the input field.</summary>
    public string? Title { get; set; }

    /// <summary>Descriptive text shown below the field.</summary>
    public string? Description { get; set; }

    /// <summary>Minimum character length.</summary>
    public int? MinLength { get; set; }

    /// <summary>Maximum character length.</summary>
    public int? MaxLength { get; set; }

    /// <summary>Semantic format hint (e.g., <c>"email"</c>, <c>"uri"</c>, <c>"date"</c>, <c>"date-time"</c>).</summary>
    public string? Format { get; set; }

    /// <summary>Default value pre-populated in the field.</summary>
    public string? Default { get; set; }
}

/// <summary>
/// Provides UI methods for eliciting information from the user during a session.
/// </summary>
public interface ISessionUiApi
{
    /// <summary>
    /// Shows a generic elicitation dialog with a custom schema.
    /// </summary>
    /// <param name="elicitationParams">The elicitation parameters including message and schema.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <returns>The <see cref="ElicitationResult"/> with the user's response.</returns>
    /// <exception cref="InvalidOperationException">Thrown if the host does not support elicitation.</exception>
    Task<ElicitationResult> ElicitationAsync(ElicitationParams elicitationParams, CancellationToken cancellationToken = default);

    /// <summary>
    /// Shows a confirmation dialog and returns the user's boolean answer.
    /// Returns <c>false</c> if the user declines or cancels.
    /// </summary>
    /// <param name="message">The message to display.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <returns><c>true</c> if the user confirmed; otherwise <c>false</c>.</returns>
    /// <exception cref="InvalidOperationException">Thrown if the host does not support elicitation.</exception>
    Task<bool> ConfirmAsync(string message, CancellationToken cancellationToken = default);

    /// <summary>
    /// Shows a selection dialog with the given options.
    /// Returns the selected value, or <c>null</c> if the user declines/cancels.
    /// </summary>
    /// <param name="message">The message to display.</param>
    /// <param name="options">The options to present.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <returns>The selected string, or <c>null</c> if the user declined/cancelled.</returns>
    /// <exception cref="InvalidOperationException">Thrown if the host does not support elicitation.</exception>
    Task<string?> SelectAsync(string message, string[] options, CancellationToken cancellationToken = default);

    /// <summary>
    /// Shows a text input dialog.
    /// Returns the entered text, or <c>null</c> if the user declines/cancels.
    /// </summary>
    /// <param name="message">The message to display.</param>
    /// <param name="options">Optional input field options.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <returns>The entered string, or <c>null</c> if the user declined/cancelled.</returns>
    /// <exception cref="InvalidOperationException">Thrown if the host does not support elicitation.</exception>
    Task<string?> InputAsync(string message, InputOptions? options = null, CancellationToken cancellationToken = default);
}

// ============================================================================
// Elicitation Types (server → client callback)
// ============================================================================

/// <summary>
/// Context for an elicitation handler invocation, combining the request data
/// with session context. Mirrors the single-argument pattern of <see cref="CommandContext"/>.
/// </summary>
public class ElicitationContext
{
    /// <summary>Identifier of the session that triggered the elicitation request.</summary>
    public string SessionId { get; set; } = string.Empty;

    /// <summary>Message describing what information is needed from the user.</summary>
    public string Message { get; set; } = string.Empty;

    /// <summary>JSON Schema describing the form fields to present.</summary>
    public ElicitationSchema? RequestedSchema { get; set; }

    /// <summary>Elicitation mode: <c>"form"</c> for structured input, <c>"url"</c> for browser redirect.</summary>
    public ElicitationRequestedMode? Mode { get; set; }

    /// <summary>The source that initiated the request (e.g., MCP server name).</summary>
    public string? ElicitationSource { get; set; }

    /// <summary>URL to open in the user's browser (url mode only).</summary>
    public string? Url { get; set; }
}

/// <summary>
/// Delegate for handling elicitation requests from the server.
/// </summary>
public delegate Task<ElicitationResult> ElicitationHandler(ElicitationContext context);

// ============================================================================
// Session Capabilities
// ============================================================================

/// <summary>
/// Represents the capabilities reported by the host for a session.
/// </summary>
public class SessionCapabilities
{
    /// <summary>
    /// UI-related capabilities.
    /// </summary>
    public SessionUiCapabilities? Ui { get; set; }
}

/// <summary>
/// UI-specific capability flags for a session.
/// </summary>
public class SessionUiCapabilities
{
    /// <summary>
    /// Whether the host supports interactive elicitation dialogs.
    /// </summary>
    public bool? Elicitation { get; set; }
}

// ============================================================================
// Hook Handler Types
// ============================================================================

/// <summary>
/// Context for a hook invocation.
/// </summary>
public class HookInvocation
{
    /// <summary>
    /// Identifier of the session that triggered the hook.
    /// </summary>
    public string SessionId { get; set; } = string.Empty;
}

/// <summary>
/// Input for a pre-tool-use hook.
/// </summary>
public class PreToolUseHookInput
{
    /// <summary>
    /// Unix timestamp in milliseconds when the tool use was initiated.
    /// </summary>
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    /// <summary>
    /// Current working directory of the session.
    /// </summary>
    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    /// <summary>
    /// Name of the tool about to be executed.
    /// </summary>
    [JsonPropertyName("toolName")]
    public string ToolName { get; set; } = string.Empty;

    /// <summary>
    /// Arguments that will be passed to the tool.
    /// </summary>
    [JsonPropertyName("toolArgs")]
    public object? ToolArgs { get; set; }
}

/// <summary>
/// Output for a pre-tool-use hook.
/// </summary>
public class PreToolUseHookOutput
{
    /// <summary>
    /// Permission decision for the pending tool call.
    /// <list type="bullet">
    /// <item><description><c>"allow"</c> — permit the tool to execute.</description></item>
    /// <item><description><c>"deny"</c> — block the tool from executing.</description></item>
    /// <item><description><c>"ask"</c> — fall through to the normal permission prompt.</description></item>
    /// </list>
    /// </summary>
    [JsonPropertyName("permissionDecision")]
    public string? PermissionDecision { get; set; }

    /// <summary>
    /// Human-readable reason for the permission decision.
    /// </summary>
    [JsonPropertyName("permissionDecisionReason")]
    public string? PermissionDecisionReason { get; set; }

    /// <summary>
    /// Modified arguments to pass to the tool instead of the original ones.
    /// </summary>
    [JsonPropertyName("modifiedArgs")]
    public object? ModifiedArgs { get; set; }

    /// <summary>
    /// Additional context to inject into the conversation for the language model.
    /// </summary>
    [JsonPropertyName("additionalContext")]
    public string? AdditionalContext { get; set; }

    /// <summary>
    /// Whether to suppress the tool's output from the conversation.
    /// </summary>
    [JsonPropertyName("suppressOutput")]
    public bool? SuppressOutput { get; set; }
}

/// <summary>
/// Delegate invoked before a tool is executed, allowing modification or denial of the call.
/// </summary>
public delegate Task<PreToolUseHookOutput?> PreToolUseHandler(PreToolUseHookInput input, HookInvocation invocation);

/// <summary>
/// Input for a post-tool-use hook.
/// </summary>
public class PostToolUseHookInput
{
    /// <summary>
    /// Unix timestamp in milliseconds when the tool execution completed.
    /// </summary>
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    /// <summary>
    /// Current working directory of the session.
    /// </summary>
    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    /// <summary>
    /// Name of the tool that was executed.
    /// </summary>
    [JsonPropertyName("toolName")]
    public string ToolName { get; set; } = string.Empty;

    /// <summary>
    /// Arguments that were passed to the tool.
    /// </summary>
    [JsonPropertyName("toolArgs")]
    public object? ToolArgs { get; set; }

    /// <summary>
    /// Result returned by the tool execution.
    /// </summary>
    [JsonPropertyName("toolResult")]
    public object? ToolResult { get; set; }
}

/// <summary>
/// Output for a post-tool-use hook.
/// </summary>
public class PostToolUseHookOutput
{
    /// <summary>
    /// Modified result to replace the original tool result.
    /// </summary>
    [JsonPropertyName("modifiedResult")]
    public object? ModifiedResult { get; set; }

    /// <summary>
    /// Additional context to inject into the conversation for the language model.
    /// </summary>
    [JsonPropertyName("additionalContext")]
    public string? AdditionalContext { get; set; }

    /// <summary>
    /// Whether to suppress the tool's output from the conversation.
    /// </summary>
    [JsonPropertyName("suppressOutput")]
    public bool? SuppressOutput { get; set; }
}

/// <summary>
/// Delegate invoked after a tool has been executed, allowing modification of the result.
/// </summary>
public delegate Task<PostToolUseHookOutput?> PostToolUseHandler(PostToolUseHookInput input, HookInvocation invocation);

/// <summary>
/// Input for a user-prompt-submitted hook.
/// </summary>
public class UserPromptSubmittedHookInput
{
    /// <summary>
    /// Unix timestamp in milliseconds when the prompt was submitted.
    /// </summary>
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    /// <summary>
    /// Current working directory of the session.
    /// </summary>
    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    /// <summary>
    /// The user's prompt text.
    /// </summary>
    [JsonPropertyName("prompt")]
    public string Prompt { get; set; } = string.Empty;
}

/// <summary>
/// Output for a user-prompt-submitted hook.
/// </summary>
public class UserPromptSubmittedHookOutput
{
    /// <summary>
    /// Modified prompt to use instead of the original user prompt.
    /// </summary>
    [JsonPropertyName("modifiedPrompt")]
    public string? ModifiedPrompt { get; set; }

    /// <summary>
    /// Additional context to inject into the conversation for the language model.
    /// </summary>
    [JsonPropertyName("additionalContext")]
    public string? AdditionalContext { get; set; }

    /// <summary>
    /// Whether to suppress the prompt's output from the conversation.
    /// </summary>
    [JsonPropertyName("suppressOutput")]
    public bool? SuppressOutput { get; set; }
}

/// <summary>
/// Delegate invoked when the user submits a prompt, allowing modification of the prompt.
/// </summary>
public delegate Task<UserPromptSubmittedHookOutput?> UserPromptSubmittedHandler(UserPromptSubmittedHookInput input, HookInvocation invocation);

/// <summary>
/// Input for a session-start hook.
/// </summary>
public class SessionStartHookInput
{
    /// <summary>
    /// Unix timestamp in milliseconds when the session started.
    /// </summary>
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    /// <summary>
    /// Current working directory of the session.
    /// </summary>
    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    /// <summary>
    /// Source of the session start.
    /// <list type="bullet">
    /// <item><description><c>"startup"</c> — initial application startup.</description></item>
    /// <item><description><c>"resume"</c> — resuming a previous session.</description></item>
    /// <item><description><c>"new"</c> — starting a brand new session.</description></item>
    /// </list>
    /// </summary>
    [JsonPropertyName("source")]
    public string Source { get; set; } = string.Empty;

    /// <summary>
    /// Initial prompt provided when the session was started.
    /// </summary>
    [JsonPropertyName("initialPrompt")]
    public string? InitialPrompt { get; set; }
}

/// <summary>
/// Output for a session-start hook.
/// </summary>
public class SessionStartHookOutput
{
    /// <summary>
    /// Additional context to inject into the session for the language model.
    /// </summary>
    [JsonPropertyName("additionalContext")]
    public string? AdditionalContext { get; set; }

    /// <summary>
    /// Modified session configuration to apply at startup.
    /// </summary>
    [JsonPropertyName("modifiedConfig")]
    public IDictionary<string, object>? ModifiedConfig { get; set; }
}

/// <summary>
/// Delegate invoked when a session starts, allowing injection of context or config changes.
/// </summary>
public delegate Task<SessionStartHookOutput?> SessionStartHandler(SessionStartHookInput input, HookInvocation invocation);

/// <summary>
/// Input for a session-end hook.
/// </summary>
public class SessionEndHookInput
{
    /// <summary>
    /// Unix timestamp in milliseconds when the session ended.
    /// </summary>
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    /// <summary>
    /// Current working directory of the session.
    /// </summary>
    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    /// <summary>
    /// Reason for session end.
    /// <list type="bullet">
    /// <item><description><c>"complete"</c> — the session finished normally.</description></item>
    /// <item><description><c>"error"</c> — the session ended due to an error.</description></item>
    /// <item><description><c>"abort"</c> — the session was aborted.</description></item>
    /// <item><description><c>"timeout"</c> — the session timed out.</description></item>
    /// <item><description><c>"user_exit"</c> — the user exited the session.</description></item>
    /// </list>
    /// </summary>
    [JsonPropertyName("reason")]
    public string Reason { get; set; } = string.Empty;

    /// <summary>
    /// Final message from the assistant before the session ended.
    /// </summary>
    [JsonPropertyName("finalMessage")]
    public string? FinalMessage { get; set; }

    /// <summary>
    /// Error message if the session ended due to an error.
    /// </summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }
}

/// <summary>
/// Output for a session-end hook.
/// </summary>
public class SessionEndHookOutput
{
    /// <summary>
    /// Whether to suppress the session end output from the conversation.
    /// </summary>
    [JsonPropertyName("suppressOutput")]
    public bool? SuppressOutput { get; set; }

    /// <summary>
    /// List of cleanup action identifiers to execute after the session ends.
    /// </summary>
    [JsonPropertyName("cleanupActions")]
    public IList<string>? CleanupActions { get; set; }

    /// <summary>
    /// Summary of the session to persist for future reference.
    /// </summary>
    [JsonPropertyName("sessionSummary")]
    public string? SessionSummary { get; set; }
}

/// <summary>
/// Delegate invoked when a session ends, allowing cleanup actions or summary generation.
/// </summary>
public delegate Task<SessionEndHookOutput?> SessionEndHandler(SessionEndHookInput input, HookInvocation invocation);

/// <summary>
/// Input for an error-occurred hook.
/// </summary>
public class ErrorOccurredHookInput
{
    /// <summary>
    /// Unix timestamp in milliseconds when the error occurred.
    /// </summary>
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    /// <summary>
    /// Current working directory of the session.
    /// </summary>
    [JsonPropertyName("cwd")]
    public string Cwd { get; set; } = string.Empty;

    /// <summary>
    /// Error message describing what went wrong.
    /// </summary>
    [JsonPropertyName("error")]
    public string Error { get; set; } = string.Empty;

    /// <summary>
    /// Context of the error.
    /// <list type="bullet">
    /// <item><description><c>"model_call"</c> — error during a model API call.</description></item>
    /// <item><description><c>"tool_execution"</c> — error during tool execution.</description></item>
    /// <item><description><c>"system"</c> — internal system error.</description></item>
    /// <item><description><c>"user_input"</c> — error processing user input.</description></item>
    /// </list>
    /// </summary>
    [JsonPropertyName("errorContext")]
    public string ErrorContext { get; set; } = string.Empty;

    /// <summary>
    /// Whether the error is recoverable and the session can continue.
    /// </summary>
    [JsonPropertyName("recoverable")]
    public bool Recoverable { get; set; }
}

/// <summary>
/// Output for an error-occurred hook.
/// </summary>
public class ErrorOccurredHookOutput
{
    /// <summary>
    /// Whether to suppress the error output from the conversation.
    /// </summary>
    [JsonPropertyName("suppressOutput")]
    public bool? SuppressOutput { get; set; }

    /// <summary>
    /// Error handling strategy.
    /// <list type="bullet">
    /// <item><description><c>"retry"</c> — retry the failed operation.</description></item>
    /// <item><description><c>"skip"</c> — skip the failed operation and continue.</description></item>
    /// <item><description><c>"abort"</c> — abort the session.</description></item>
    /// </list>
    /// </summary>
    [JsonPropertyName("errorHandling")]
    public string? ErrorHandling { get; set; }

    /// <summary>
    /// Number of times to retry the failed operation.
    /// </summary>
    [JsonPropertyName("retryCount")]
    public int? RetryCount { get; set; }

    /// <summary>
    /// Message to display to the user about the error.
    /// </summary>
    [JsonPropertyName("userNotification")]
    public string? UserNotification { get; set; }
}

/// <summary>
/// Delegate invoked when an error occurs, allowing custom error handling strategies.
/// </summary>
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

/// <summary>
/// Specifies how a custom system message is applied to the session.
/// </summary>
[JsonConverter(typeof(JsonStringEnumConverter<SystemMessageMode>))]
public enum SystemMessageMode
{
    /// <summary>Append the custom system message to the default system message.</summary>
    [JsonStringEnumMemberName("append")]
    Append,
    /// <summary>Replace the default system message entirely.</summary>
    [JsonStringEnumMemberName("replace")]
    Replace,
    /// <summary>Override individual sections of the system prompt.</summary>
    [JsonStringEnumMemberName("customize")]
    Customize
}

/// <summary>
/// Specifies the operation to perform on a system prompt section.
/// </summary>
[JsonConverter(typeof(JsonStringEnumConverter<SectionOverrideAction>))]
public enum SectionOverrideAction
{
    /// <summary>Replace the section content entirely.</summary>
    [JsonStringEnumMemberName("replace")]
    Replace,
    /// <summary>Remove the section from the prompt.</summary>
    [JsonStringEnumMemberName("remove")]
    Remove,
    /// <summary>Append content after the existing section.</summary>
    [JsonStringEnumMemberName("append")]
    Append,
    /// <summary>Prepend content before the existing section.</summary>
    [JsonStringEnumMemberName("prepend")]
    Prepend,
    /// <summary>Transform the section content via a callback.</summary>
    [JsonStringEnumMemberName("transform")]
    Transform
}

/// <summary>
/// Override operation for a single system prompt section.
/// </summary>
public class SectionOverride
{
    /// <summary>
    /// The operation to perform on this section. Ignored when Transform is set.
    /// </summary>
    [JsonPropertyName("action")]
    public SectionOverrideAction? Action { get; set; }

    /// <summary>
    /// Content for the override. Optional for all actions. Ignored for remove.
    /// </summary>
    [JsonPropertyName("content")]
    public string? Content { get; set; }

    /// <summary>
    /// Transform callback. When set, takes precedence over Action.
    /// Receives current section content, returns transformed content.
    /// Not serialized — the SDK handles this locally.
    /// </summary>
    [JsonIgnore]
    public Func<string, Task<string>>? Transform { get; set; }
}

/// <summary>
/// Known system prompt section identifiers for the "customize" mode.
/// </summary>
public static class SystemPromptSections
{
    /// <summary>Agent identity preamble and mode statement.</summary>
    public const string Identity = "identity";
    /// <summary>Response style, conciseness rules, output formatting preferences.</summary>
    public const string Tone = "tone";
    /// <summary>Tool usage patterns, parallel calling, batching guidelines.</summary>
    public const string ToolEfficiency = "tool_efficiency";
    /// <summary>CWD, OS, git root, directory listing, available tools.</summary>
    public const string EnvironmentContext = "environment_context";
    /// <summary>Coding rules, linting/testing, ecosystem tools, style.</summary>
    public const string CodeChangeRules = "code_change_rules";
    /// <summary>Tips, behavioral best practices, behavioral guidelines.</summary>
    public const string Guidelines = "guidelines";
    /// <summary>Environment limitations, prohibited actions, security policies.</summary>
    public const string Safety = "safety";
    /// <summary>Per-tool usage instructions.</summary>
    public const string ToolInstructions = "tool_instructions";
    /// <summary>Repository and organization custom instructions.</summary>
    public const string CustomInstructions = "custom_instructions";
    /// <summary>End-of-prompt instructions: parallel tool calling, persistence, task completion.</summary>
    public const string LastInstructions = "last_instructions";
}

/// <summary>
/// Configuration for the system message used in a session.
/// </summary>
public class SystemMessageConfig
{
    /// <summary>
    /// How the system message is applied (append, replace, or customize).
    /// </summary>
    public SystemMessageMode? Mode { get; set; }

    /// <summary>
    /// Content of the system message. Used by append and replace modes.
    /// In customize mode, additional content appended after all sections.
    /// </summary>
    public string? Content { get; set; }

    /// <summary>
    /// Section-level overrides for customize mode.
    /// Keys are section identifiers (see <see cref="SystemPromptSections"/>).
    /// </summary>
    public IDictionary<string, SectionOverride>? Sections { get; set; }
}

/// <summary>
/// Configuration for a custom model provider.
/// </summary>
public class ProviderConfig
{
    /// <summary>
    /// Provider type identifier (e.g., "openai", "azure").
    /// </summary>
    [JsonPropertyName("type")]
    public string? Type { get; set; }

    /// <summary>
    /// Wire API format to use (e.g., "chat-completions").
    /// </summary>
    [JsonPropertyName("wireApi")]
    public string? WireApi { get; set; }

    /// <summary>
    /// Base URL of the provider's API endpoint.
    /// </summary>
    [JsonPropertyName("baseUrl")]
    public string BaseUrl { get; set; } = string.Empty;

    /// <summary>
    /// API key for authenticating with the provider.
    /// </summary>
    [JsonPropertyName("apiKey")]
    public string? ApiKey { get; set; }

    /// <summary>
    /// Bearer token for authentication. Sets the Authorization header directly.
    /// Use this for services requiring bearer token auth instead of API key.
    /// Takes precedence over ApiKey when both are set.
    /// </summary>
    [JsonPropertyName("bearerToken")]
    public string? BearerToken { get; set; }

    /// <summary>
    /// Azure-specific configuration options.
    /// </summary>
    [JsonPropertyName("azure")]
    public AzureOptions? Azure { get; set; }

    /// <summary>
    /// Custom HTTP headers to include in outbound provider requests.
    /// </summary>
    [JsonPropertyName("headers")]
    public IDictionary<string, string>? Headers { get; set; }
}

/// <summary>
/// Azure OpenAI-specific provider options.
/// </summary>
public class AzureOptions
{
    /// <summary>
    /// Azure OpenAI API version to use (e.g., "2024-02-01").
    /// </summary>
    [JsonPropertyName("apiVersion")]
    public string? ApiVersion { get; set; }
}

// ============================================================================
// MCP Server Configuration Types
// ============================================================================

/// <summary>
/// Abstract base class for MCP server configurations.
/// </summary>
[JsonPolymorphic(
    TypeDiscriminatorPropertyName = "type",
    IgnoreUnrecognizedTypeDiscriminators = true)]
[JsonDerivedType(typeof(McpStdioServerConfig), "stdio")]
[JsonDerivedType(typeof(McpHttpServerConfig), "http")]
public abstract class McpServerConfig
{
    private protected McpServerConfig() { }

    /// <summary>
    /// List of tools to include from this server. Empty list means none. Use "*" for all.
    /// </summary>
    [JsonPropertyName("tools")]
    public IList<string> Tools { get => field ??= []; set; }

    /// <summary>
    /// The server type discriminator.
    /// </summary>
    [JsonIgnore]
    public virtual string Type => "unknown";

    /// <summary>
    /// Optional timeout in milliseconds for tool calls to this server.
    /// </summary>
    [JsonPropertyName("timeout")]
    public int? Timeout { get; set; }
}

/// <summary>
/// Configuration for a local/stdio MCP server.
/// </summary>
public sealed class McpStdioServerConfig : McpServerConfig
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "stdio";

    /// <summary>
    /// Command to run the MCP server.
    /// </summary>
    [JsonPropertyName("command")]
    public string Command { get; set; } = string.Empty;

    /// <summary>
    /// Arguments to pass to the command.
    /// </summary>
    [JsonPropertyName("args")]
    public IList<string> Args { get => field ??= []; set; }

    /// <summary>
    /// Environment variables to pass to the server.
    /// </summary>
    [JsonPropertyName("env")]
    public IDictionary<string, string>? Env { get; set; }

    /// <summary>
    /// Working directory for the server process.
    /// </summary>
    [JsonPropertyName("cwd")]
    public string? Cwd { get; set; }
}

/// <summary>
/// Configuration for a remote MCP server (HTTP or SSE).
/// </summary>
public sealed class McpHttpServerConfig : McpServerConfig
{
    /// <inheritdoc />
    [JsonIgnore]
    public override string Type => "http";

    /// <summary>
    /// URL of the remote server.
    /// </summary>
    [JsonPropertyName("url")]
    public string Url { get; set; } = string.Empty;

    /// <summary>
    /// Optional HTTP headers to include in requests.
    /// </summary>
    [JsonPropertyName("headers")]
    public IDictionary<string, string>? Headers { get; set; }
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
    public IList<string>? Tools { get; set; }

    /// <summary>
    /// The prompt content for the agent.
    /// </summary>
    [JsonPropertyName("prompt")]
    public string Prompt { get; set; } = string.Empty;

    /// <summary>
    /// MCP servers specific to this agent.
    /// </summary>
    [JsonPropertyName("mcpServers")]
    public IDictionary<string, McpServerConfig>? McpServers { get; set; }

    /// <summary>
    /// Whether the agent should be available for model inference.
    /// </summary>
    [JsonPropertyName("infer")]
    public bool? Infer { get; set; }

    /// <summary>
    /// List of skill names to preload into this agent's context.
    /// When set, the full content of each listed skill is eagerly injected into
    /// the agent's context at startup. Skills are resolved by name from the
    /// session's configured skill directories (<see cref="SessionConfig.SkillDirectories"/>).
    /// When omitted, no skills are injected (opt-in model).
    /// </summary>
    [JsonPropertyName("skills")]
    public IList<string>? Skills { get; set; }
}

/// <summary>
/// Configuration for the default agent (the built-in agent that handles turns when no custom agent is selected).
/// Use <see cref="ExcludedTools"/> to hide specific tools from the default agent
/// while keeping them available to custom sub-agents.
/// </summary>
public class DefaultAgentConfig
{
    /// <summary>
    /// List of tool names to exclude from the default agent.
    /// These tools remain available to custom sub-agents that reference them
    /// in their <see cref="CustomAgentConfig.Tools"/> list.
    /// </summary>
    public IList<string>? ExcludedTools { get; set; }
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

/// <summary>
/// Configuration options for creating a new Copilot session.
/// </summary>
public class SessionConfig
{
    /// <summary>
    /// Initializes a new instance of the <see cref="SessionConfig"/> class.
    /// </summary>
    public SessionConfig() { }

    /// <summary>
    /// Initializes a new instance of the <see cref="SessionConfig"/> class
    /// by copying the properties of the specified instance.
    /// </summary>
    protected SessionConfig(SessionConfig? other)
    {
        if (other is null) return;

        AvailableTools = other.AvailableTools is not null ? [.. other.AvailableTools] : null;
        ClientName = other.ClientName;
        Commands = other.Commands is not null ? [.. other.Commands] : null;
        ConfigDir = other.ConfigDir;
        CustomAgents = other.CustomAgents is not null ? [.. other.CustomAgents] : null;
        DefaultAgent = other.DefaultAgent;
        Agent = other.Agent;
        DisabledSkills = other.DisabledSkills is not null ? [.. other.DisabledSkills] : null;
        EnableConfigDiscovery = other.EnableConfigDiscovery;
        ExcludedTools = other.ExcludedTools is not null ? [.. other.ExcludedTools] : null;
        Hooks = other.Hooks;
        InfiniteSessions = other.InfiniteSessions;
        McpServers = other.McpServers is not null
            ? (other.McpServers is Dictionary<string, McpServerConfig> dict
                ? new Dictionary<string, McpServerConfig>(dict, dict.Comparer)
                : new Dictionary<string, McpServerConfig>(other.McpServers))
            : null;
        Model = other.Model;
        ModelCapabilities = other.ModelCapabilities;
        OnElicitationRequest = other.OnElicitationRequest;
        OnEvent = other.OnEvent;
        OnPermissionRequest = other.OnPermissionRequest;
        OnUserInputRequest = other.OnUserInputRequest;
        Provider = other.Provider;
        ReasoningEffort = other.ReasoningEffort;
        CreateSessionFsHandler = other.CreateSessionFsHandler;
        SessionId = other.SessionId;
        SkillDirectories = other.SkillDirectories is not null ? [.. other.SkillDirectories] : null;
        Streaming = other.Streaming;
        IncludeSubAgentStreamingEvents = other.IncludeSubAgentStreamingEvents;
        SystemMessage = other.SystemMessage;
        Tools = other.Tools is not null ? [.. other.Tools] : null;
        WorkingDirectory = other.WorkingDirectory;
    }

    /// <summary>
    /// Optional session identifier; a new ID is generated if not provided.
    /// </summary>
    public string? SessionId { get; set; }

    /// <summary>
    /// Client name to identify the application using the SDK.
    /// Included in the User-Agent header for API requests.
    /// </summary>
    public string? ClientName { get; set; }

    /// <summary>
    /// Model identifier to use for this session (e.g., "gpt-4o").
    /// </summary>
    public string? Model { get; set; }

    /// <summary>
    /// Reasoning effort level for models that support it.
    /// Valid values: "low", "medium", "high", "xhigh".
    /// Only applies to models where capabilities.supports.reasoningEffort is true.
    /// </summary>
    public string? ReasoningEffort { get; set; }

    /// <summary>
    /// Per-property overrides for model capabilities, deep-merged over runtime defaults.
    /// </summary>
    public ModelCapabilitiesOverride? ModelCapabilities { get; set; }

    /// <summary>
    /// Override the default configuration directory location.
    /// When specified, the session will use this directory for storing config and state.
    /// </summary>
    public string? ConfigDir { get; set; }

    /// <summary>
    /// When <see langword="true"/>, automatically discovers MCP server configurations
    /// (e.g. <c>.mcp.json</c>, <c>.vscode/mcp.json</c>) and skill directories from
    /// the working directory and merges them with any explicitly provided
    /// <see cref="McpServers"/> and <see cref="SkillDirectories"/>, with explicit
    /// values taking precedence on name collision.
    /// <para>
    /// Custom instruction files (<c>.github/copilot-instructions.md</c>, <c>AGENTS.md</c>, etc.)
    /// are always loaded from the working directory regardless of this setting.
    /// </para>
    /// </summary>
    public bool? EnableConfigDiscovery { get; set; }

    /// <summary>
    /// Custom tool functions available to the language model during the session.
    /// </summary>
    public ICollection<AIFunction>? Tools { get; set; }
    /// <summary>
    /// System message configuration for the session.
    /// </summary>
    public SystemMessageConfig? SystemMessage { get; set; }
    /// <summary>
    /// List of tool names to allow; only these tools will be available when specified.
    /// </summary>
    public IList<string>? AvailableTools { get; set; }
    /// <summary>
    /// List of tool names to exclude from the session.
    /// </summary>
    public IList<string>? ExcludedTools { get; set; }
    /// <summary>
    /// Custom model provider configuration for the session.
    /// </summary>
    public ProviderConfig? Provider { get; set; }

    /// <summary>
    /// Handler for permission requests from the server.
    /// When provided, the server will call this handler to request permission for operations.
    /// </summary>
    public PermissionRequestHandler? OnPermissionRequest { get; set; }

    /// <summary>
    /// Handler for user input requests from the agent.
    /// When provided, enables the ask_user tool for the agent to request user input.
    /// </summary>
    public UserInputHandler? OnUserInputRequest { get; set; }

    /// <summary>
    /// Slash commands registered for this session.
    /// When the CLI has a TUI, each command appears as <c>/name</c> for the user to invoke.
    /// The handler is called when the user executes the command.
    /// </summary>
    public IList<CommandDefinition>? Commands { get; set; }

    /// <summary>
    /// Handler for elicitation requests from the server or MCP tools.
    /// When provided, the server will route elicitation requests to this handler
    /// and report elicitation as a supported capability.
    /// </summary>
    public ElicitationHandler? OnElicitationRequest { get; set; }

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
    /// Include sub-agent streaming events in the event stream. When true, streaming
    /// delta events from sub-agents (e.g., <c>assistant.message_delta</c>,
    /// <c>assistant.reasoning_delta</c>, <c>assistant.streaming_delta</c> with
    /// <c>agentId</c> set) are forwarded to this connection. When false, only
    /// non-streaming sub-agent events and <c>subagent.*</c> lifecycle events are
    /// forwarded; streaming deltas from sub-agents are suppressed.
    /// Default: true.
    /// </summary>
    public bool IncludeSubAgentStreamingEvents { get; set; } = true;

    /// <summary>
    /// MCP server configurations for the session.
    /// Keys are server names, values are server configurations (<see cref="McpStdioServerConfig"/> or <see cref="McpHttpServerConfig"/>).
    /// </summary>
    public IDictionary<string, McpServerConfig>? McpServers { get; set; }

    /// <summary>
    /// Custom agent configurations for the session.
    /// </summary>
    public IList<CustomAgentConfig>? CustomAgents { get; set; }

    /// <summary>
    /// Configuration for the default agent (the built-in agent that handles turns when no custom agent is selected).
    /// Use <see cref="DefaultAgentConfig.ExcludedTools"/> to hide specific tools from the default agent
    /// while keeping them available to custom sub-agents.
    /// </summary>
    public DefaultAgentConfig? DefaultAgent { get; set; }

    /// <summary>
    /// Name of the custom agent to activate when the session starts.
    /// Must match the <see cref="CustomAgentConfig.Name"/> of one of the agents in <see cref="CustomAgents"/>.
    /// </summary>
    public string? Agent { get; set; }

    /// <summary>
    /// Directories to load skills from.
    /// </summary>
    public IList<string>? SkillDirectories { get; set; }

    /// <summary>
    /// List of skill names to disable.
    /// </summary>
    public IList<string>? DisabledSkills { get; set; }

    /// <summary>
    /// Infinite session configuration for persistent workspaces and automatic compaction.
    /// When enabled (default), sessions automatically manage context limits and persist state.
    /// </summary>
    public InfiniteSessionConfig? InfiniteSessions { get; set; }

    /// <summary>
    /// Optional event handler that is registered on the session before the
    /// session.create RPC is issued.
    /// </summary>
    /// <remarks>
    /// Equivalent to calling <see cref="CopilotSession.On"/> immediately
    /// after creation, but executes earlier in the lifecycle so no events are missed.
    /// Using this property rather than <see cref="CopilotSession.On"/> guarantees that early events emitted 
    /// by the CLI during session creation (e.g. session.start) are delivered to the handler.
    /// </remarks>
    public SessionEventHandler? OnEvent { get; set; }

    /// <summary>
    /// Supplies a handler for session filesystem operations.
    /// This is used only when <see cref="CopilotClientOptions.SessionFs"/> is configured.
    /// </summary>
    public Func<CopilotSession, SessionFsProvider>? CreateSessionFsHandler { get; set; }

    /// <summary>
    /// Creates a shallow clone of this <see cref="SessionConfig"/> instance.
    /// </summary>
    /// <remarks>
    /// Mutable collection properties are copied into new collection instances so that modifications
    /// to those collections on the clone do not affect the original.
    /// Other reference-type properties (for example provider configuration, system messages,
    /// hooks, infinite session configuration, and delegates) are not deep-cloned; the original
    /// and the clone will share those nested objects, and changes to them may affect both.
    /// </remarks>
    public virtual SessionConfig Clone()
    {
        return new(this);
    }
}

/// <summary>
/// Configuration options for resuming an existing Copilot session.
/// </summary>
public class ResumeSessionConfig
{
    /// <summary>
    /// Initializes a new instance of the <see cref="ResumeSessionConfig"/> class.
    /// </summary>
    public ResumeSessionConfig() { }

    /// <summary>
    /// Initializes a new instance of the <see cref="ResumeSessionConfig"/> class
    /// by copying the properties of the specified instance.
    /// </summary>
    protected ResumeSessionConfig(ResumeSessionConfig? other)
    {
        if (other is null) return;

        AvailableTools = other.AvailableTools is not null ? [.. other.AvailableTools] : null;
        ClientName = other.ClientName;
        Commands = other.Commands is not null ? [.. other.Commands] : null;
        ConfigDir = other.ConfigDir;
        CustomAgents = other.CustomAgents is not null ? [.. other.CustomAgents] : null;
        DefaultAgent = other.DefaultAgent;
        Agent = other.Agent;
        DisabledSkills = other.DisabledSkills is not null ? [.. other.DisabledSkills] : null;
        DisableResume = other.DisableResume;
        EnableConfigDiscovery = other.EnableConfigDiscovery;
        ExcludedTools = other.ExcludedTools is not null ? [.. other.ExcludedTools] : null;
        Hooks = other.Hooks;
        InfiniteSessions = other.InfiniteSessions;
        McpServers = other.McpServers is not null
            ? (other.McpServers is Dictionary<string, McpServerConfig> dict
                ? new Dictionary<string, McpServerConfig>(dict, dict.Comparer)
                : new Dictionary<string, McpServerConfig>(other.McpServers))
            : null;
        Model = other.Model;
        ModelCapabilities = other.ModelCapabilities;
        OnElicitationRequest = other.OnElicitationRequest;
        OnEvent = other.OnEvent;
        OnPermissionRequest = other.OnPermissionRequest;
        OnUserInputRequest = other.OnUserInputRequest;
        Provider = other.Provider;
        ReasoningEffort = other.ReasoningEffort;
        CreateSessionFsHandler = other.CreateSessionFsHandler;
        SkillDirectories = other.SkillDirectories is not null ? [.. other.SkillDirectories] : null;
        Streaming = other.Streaming;
        IncludeSubAgentStreamingEvents = other.IncludeSubAgentStreamingEvents;
        SystemMessage = other.SystemMessage;
        Tools = other.Tools is not null ? [.. other.Tools] : null;
        WorkingDirectory = other.WorkingDirectory;
    }

    /// <summary>
    /// Client name to identify the application using the SDK.
    /// Included in the User-Agent header for API requests.
    /// </summary>
    public string? ClientName { get; set; }

    /// <summary>
    /// Model to use for this session. Can change the model when resuming.
    /// </summary>
    public string? Model { get; set; }

    /// <summary>
    /// Custom tool functions available to the language model during the resumed session.
    /// </summary>
    public ICollection<AIFunction>? Tools { get; set; }

    /// <summary>
    /// System message configuration.
    /// </summary>
    public SystemMessageConfig? SystemMessage { get; set; }

    /// <summary>
    /// List of tool names to allow. When specified, only these tools will be available.
    /// Takes precedence over ExcludedTools.
    /// </summary>
    public IList<string>? AvailableTools { get; set; }

    /// <summary>
    /// List of tool names to disable. All other tools remain available.
    /// Ignored if AvailableTools is specified.
    /// </summary>
    public IList<string>? ExcludedTools { get; set; }

    /// <summary>
    /// Custom model provider configuration for the resumed session.
    /// </summary>
    public ProviderConfig? Provider { get; set; }

    /// <summary>
    /// Reasoning effort level for models that support it.
    /// Valid values: "low", "medium", "high", "xhigh".
    /// </summary>
    public string? ReasoningEffort { get; set; }

    /// <summary>
    /// Per-property overrides for model capabilities, deep-merged over runtime defaults.
    /// </summary>
    public ModelCapabilitiesOverride? ModelCapabilities { get; set; }

    /// <summary>
    /// Handler for permission requests from the server.
    /// When provided, the server will call this handler to request permission for operations.
    /// </summary>
    public PermissionRequestHandler? OnPermissionRequest { get; set; }

    /// <summary>
    /// Handler for user input requests from the agent.
    /// When provided, enables the ask_user tool for the agent to request user input.
    /// </summary>
    public UserInputHandler? OnUserInputRequest { get; set; }

    /// <summary>
    /// Slash commands registered for this session.
    /// When the CLI has a TUI, each command appears as <c>/name</c> for the user to invoke.
    /// The handler is called when the user executes the command.
    /// </summary>
    public IList<CommandDefinition>? Commands { get; set; }

    /// <summary>
    /// Handler for elicitation requests from the server or MCP tools.
    /// When provided, the server will route elicitation requests to this handler
    /// and report elicitation as a supported capability.
    /// </summary>
    public ElicitationHandler? OnElicitationRequest { get; set; }

    /// <summary>
    /// Hook handlers for session lifecycle events.
    /// </summary>
    public SessionHooks? Hooks { get; set; }

    /// <summary>
    /// Working directory for the session.
    /// </summary>
    public string? WorkingDirectory { get; set; }

    /// <summary>
    /// Override the default configuration directory location.
    /// </summary>
    public string? ConfigDir { get; set; }

    /// <summary>
    /// When <see langword="true"/>, automatically discovers MCP server configurations
    /// (e.g. <c>.mcp.json</c>, <c>.vscode/mcp.json</c>) and skill directories from
    /// the working directory and merges them with any explicitly provided
    /// <see cref="McpServers"/> and <see cref="SkillDirectories"/>, with explicit
    /// values taking precedence on name collision.
    /// <para>
    /// Custom instruction files (<c>.github/copilot-instructions.md</c>, <c>AGENTS.md</c>, etc.)
    /// are always loaded from the working directory regardless of this setting.
    /// </para>
    /// </summary>
    public bool? EnableConfigDiscovery { get; set; }

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
    /// Include sub-agent streaming events in the event stream. When true, streaming
    /// delta events from sub-agents (e.g., <c>assistant.message_delta</c>,
    /// <c>assistant.reasoning_delta</c>, <c>assistant.streaming_delta</c> with
    /// <c>agentId</c> set) are forwarded to this connection. When false, only
    /// non-streaming sub-agent events and <c>subagent.*</c> lifecycle events are
    /// forwarded; streaming deltas from sub-agents are suppressed.
    /// Default: true.
    /// </summary>
    public bool IncludeSubAgentStreamingEvents { get; set; } = true;

    /// <summary>
    /// MCP server configurations for the session.
    /// Keys are server names, values are server configurations (<see cref="McpStdioServerConfig"/> or <see cref="McpHttpServerConfig"/>).
    /// </summary>
    public IDictionary<string, McpServerConfig>? McpServers { get; set; }

    /// <summary>
    /// Custom agent configurations for the session.
    /// </summary>
    public IList<CustomAgentConfig>? CustomAgents { get; set; }

    /// <summary>
    /// Configuration for the default agent (the built-in agent that handles turns when no custom agent is selected).
    /// Use <see cref="DefaultAgentConfig.ExcludedTools"/> to hide specific tools from the default agent
    /// while keeping them available to custom sub-agents.
    /// </summary>
    public DefaultAgentConfig? DefaultAgent { get; set; }

    /// <summary>
    /// Name of the custom agent to activate when the session starts.
    /// Must match the <see cref="CustomAgentConfig.Name"/> of one of the agents in <see cref="CustomAgents"/>.
    /// </summary>
    public string? Agent { get; set; }

    /// <summary>
    /// Directories to load skills from.
    /// </summary>
    public IList<string>? SkillDirectories { get; set; }

    /// <summary>
    /// List of skill names to disable.
    /// </summary>
    public IList<string>? DisabledSkills { get; set; }

    /// <summary>
    /// Infinite session configuration for persistent workspaces and automatic compaction.
    /// </summary>
    public InfiniteSessionConfig? InfiniteSessions { get; set; }

    /// <summary>
    /// Optional event handler registered before the session.resume RPC is issued,
    /// ensuring early events are delivered. See <see cref="SessionConfig.OnEvent"/>.
    /// </summary>
    public SessionEventHandler? OnEvent { get; set; }

    /// <summary>
    /// Supplies a handler for session filesystem operations.
    /// This is used only when <see cref="CopilotClientOptions.SessionFs"/> is configured.
    /// </summary>
    public Func<CopilotSession, SessionFsProvider>? CreateSessionFsHandler { get; set; }

    /// <summary>
    /// Creates a shallow clone of this <see cref="ResumeSessionConfig"/> instance.
    /// </summary>
    /// <remarks>
    /// Mutable collection properties are copied into new collection instances so that modifications
    /// to those collections on the clone do not affect the original.
    /// Other reference-type properties (for example provider configuration, system messages,
    /// hooks, infinite session configuration, and delegates) are not deep-cloned; the original
    /// and the clone will share those nested objects, and changes to them may affect both.
    /// </remarks>
    public virtual ResumeSessionConfig Clone()
    {
        return new(this);
    }
}

/// <summary>
/// Options for sending a message in a Copilot session.
/// </summary>
public class MessageOptions
{
    /// <summary>
    /// Initializes a new instance of the <see cref="MessageOptions"/> class.
    /// </summary>
    public MessageOptions() { }

    /// <summary>
    /// Initializes a new instance of the <see cref="MessageOptions"/> class
    /// by copying the properties of the specified instance.
    /// </summary>
    protected MessageOptions(MessageOptions? other)
    {
        if (other is null) return;

        Attachments = other.Attachments is not null ? [.. other.Attachments] : null;
        Mode = other.Mode;
        Prompt = other.Prompt;
        RequestHeaders = other.RequestHeaders is not null
            ? new Dictionary<string, string>(other.RequestHeaders)
            : null;
    }

    /// <summary>
    /// The prompt text to send to the assistant.
    /// </summary>
    public string Prompt { get; set; } = string.Empty;
    /// <summary>
    /// File or data attachments to include with the message.
    /// </summary>
    public IList<UserMessageAttachment>? Attachments { get; set; }
    /// <summary>
    /// Interaction mode for the message (e.g., "plan", "edit").
    /// </summary>
    public string? Mode { get; set; }
    /// <summary>
    /// Custom per-turn HTTP headers for outbound model requests.
    /// </summary>
    public IDictionary<string, string>? RequestHeaders { get; set; }

    /// <summary>
    /// Creates a shallow clone of this <see cref="MessageOptions"/> instance.
    /// </summary>
    /// <remarks>
    /// Mutable collection properties are copied into new collection instances so that modifications
    /// to those collections on the clone do not affect the original.
    /// Other reference-type properties (for example attachment items) are not deep-cloned;
    /// the original and the clone will share those nested objects.
    /// </remarks>
    public virtual MessageOptions Clone()
    {
        return new(this);
    }
}

/// <summary>
/// Delegate for handling session events emitted during a Copilot session.
/// </summary>
public delegate void SessionEventHandler(SessionEvent sessionEvent);

/// <summary>
/// Working directory context for a session.
/// </summary>
public class SessionContext
{
    /// <summary>Working directory where the session was created.</summary>
    public string Cwd { get; set; } = string.Empty;
    /// <summary>Git repository root (if in a git repo).</summary>
    public string? GitRoot { get; set; }
    /// <summary>GitHub repository in "owner/repo" format.</summary>
    public string? Repository { get; set; }
    /// <summary>Current git branch.</summary>
    public string? Branch { get; set; }
}

/// <summary>
/// Filter options for listing sessions.
/// </summary>
public class SessionListFilter
{
    /// <summary>Filter by exact cwd match.</summary>
    public string? Cwd { get; set; }
    /// <summary>Filter by git root.</summary>
    public string? GitRoot { get; set; }
    /// <summary>Filter by repository (owner/repo format).</summary>
    public string? Repository { get; set; }
    /// <summary>Filter by branch.</summary>
    public string? Branch { get; set; }
}

/// <summary>
/// Metadata describing a Copilot session.
/// </summary>
public class SessionMetadata
{
    /// <summary>
    /// Unique identifier of the session.
    /// </summary>
    public string SessionId { get; set; } = string.Empty;
    /// <summary>
    /// Time when the session was created.
    /// </summary>
    public DateTime StartTime { get; set; }
    /// <summary>
    /// Time when the session was last modified.
    /// </summary>
    public DateTime ModifiedTime { get; set; }
    /// <summary>
    /// Human-readable summary of the session.
    /// </summary>
    public string? Summary { get; set; }
    /// <summary>
    /// Whether the session is running on a remote server.
    /// </summary>
    public bool IsRemote { get; set; }
    /// <summary>Working directory context (cwd, git info) from session creation.</summary>
    public SessionContext? Context { get; set; }
}

internal class PingRequest
{
    public string? Message { get; set; }
}

/// <summary>
/// Response from a server ping request.
/// </summary>
public class PingResponse
{
    /// <summary>
    /// Echo of the ping message.
    /// </summary>
    public string Message { get; set; } = string.Empty;
    /// <summary>
    /// Server timestamp when the ping was processed.
    /// </summary>
    public long Timestamp { get; set; }
    /// <summary>
    /// Protocol version supported by the server.
    /// </summary>
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

    /// <summary>
    /// Authentication type.
    /// <list type="bullet">
    /// <item><description><c>"user"</c> — authenticated via user login.</description></item>
    /// <item><description><c>"env"</c> — authenticated via environment variable.</description></item>
    /// <item><description><c>"gh-cli"</c> — authenticated via the GitHub CLI.</description></item>
    /// <item><description><c>"hmac"</c> — authenticated via HMAC signature.</description></item>
    /// <item><description><c>"api-key"</c> — authenticated via API key.</description></item>
    /// <item><description><c>"token"</c> — authenticated via explicit token.</description></item>
    /// </list>
    /// </summary>
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
    /// <summary>
    /// List of supported image MIME types (e.g., "image/png", "image/jpeg").
    /// </summary>
    [JsonPropertyName("supported_media_types")]
    public IList<string> SupportedMediaTypes { get => field ??= []; set; }

    /// <summary>
    /// Maximum number of images allowed in a single prompt.
    /// </summary>
    [JsonPropertyName("max_prompt_images")]
    public int MaxPromptImages { get; set; }

    /// <summary>
    /// Maximum size in bytes for a single prompt image.
    /// </summary>
    [JsonPropertyName("max_prompt_image_size")]
    public int MaxPromptImageSize { get; set; }
}

/// <summary>
/// Model limits
/// </summary>
public class ModelLimits
{
    /// <summary>
    /// Maximum number of tokens allowed in the prompt.
    /// </summary>
    [JsonPropertyName("max_prompt_tokens")]
    public int? MaxPromptTokens { get; set; }

    /// <summary>
    /// Maximum total tokens in the context window.
    /// </summary>
    [JsonPropertyName("max_context_window_tokens")]
    public int MaxContextWindowTokens { get; set; }

    /// <summary>
    /// Vision-specific limits for the model.
    /// </summary>
    [JsonPropertyName("vision")]
    public ModelVisionLimits? Vision { get; set; }
}

/// <summary>
/// Model support flags
/// </summary>
public class ModelSupports
{
    /// <summary>
    /// Whether this model supports image/vision inputs.
    /// </summary>
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
    /// <summary>
    /// Feature support flags for the model.
    /// </summary>
    [JsonPropertyName("supports")]
    public ModelSupports Supports { get; set; } = new();

    /// <summary>
    /// Token and resource limits for the model.
    /// </summary>
    [JsonPropertyName("limits")]
    public ModelLimits Limits { get; set; } = new();
}

/// <summary>
/// Model policy state
/// </summary>
public class ModelPolicy
{
    /// <summary>
    /// Policy state of the model (e.g., "enabled", "disabled").
    /// </summary>
    [JsonPropertyName("state")]
    public string State { get; set; } = string.Empty;

    /// <summary>
    /// Terms or conditions associated with using the model.
    /// </summary>
    [JsonPropertyName("terms")]
    public string Terms { get; set; } = string.Empty;
}

/// <summary>
/// Model billing information
/// </summary>
public class ModelBilling
{
    /// <summary>
    /// Billing cost multiplier relative to the base model rate.
    /// </summary>
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
    public IList<string>? SupportedReasoningEfforts { get; set; }

    /// <summary>Default reasoning effort level (only present if model supports reasoning effort)</summary>
    [JsonPropertyName("defaultReasoningEffort")]
    public string? DefaultReasoningEffort { get; set; }
}

/// <summary>
/// Response from models.list
/// </summary>
public class GetModelsResponse
{
    /// <summary>
    /// List of available models.
    /// </summary>
    [JsonPropertyName("models")]
    public IList<ModelInfo> Models { get => field ??= []; set; }
}

// ============================================================================
// Session Lifecycle Types (for TUI+server mode)
// ============================================================================

/// <summary>
/// Types of session lifecycle events
/// </summary>
public static class SessionLifecycleEventTypes
{
    /// <summary>A new session was created.</summary>
    public const string Created = "session.created";
    /// <summary>A session was deleted.</summary>
    public const string Deleted = "session.deleted";
    /// <summary>A session was updated.</summary>
    public const string Updated = "session.updated";
    /// <summary>A session was brought to the foreground.</summary>
    public const string Foreground = "session.foreground";
    /// <summary>A session was moved to the background.</summary>
    public const string Background = "session.background";
}

/// <summary>
/// Metadata for session lifecycle events
/// </summary>
public class SessionLifecycleEventMetadata
{
    /// <summary>
    /// ISO 8601 timestamp when the session was created.
    /// </summary>
    [JsonPropertyName("startTime")]
    public string StartTime { get; set; } = string.Empty;

    /// <summary>
    /// ISO 8601 timestamp when the session was last modified.
    /// </summary>
    [JsonPropertyName("modifiedTime")]
    public string ModifiedTime { get; set; } = string.Empty;

    /// <summary>
    /// Human-readable summary of the session.
    /// </summary>
    [JsonPropertyName("summary")]
    public string? Summary { get; set; }
}

/// <summary>
/// Session lifecycle event notification
/// </summary>
public class SessionLifecycleEvent
{
    /// <summary>
    /// Type of lifecycle event (see <see cref="SessionLifecycleEventTypes"/>).
    /// </summary>
    [JsonPropertyName("type")]
    public string Type { get; set; } = string.Empty;

    /// <summary>
    /// Identifier of the session this event pertains to.
    /// </summary>
    [JsonPropertyName("sessionId")]
    public string SessionId { get; set; } = string.Empty;

    /// <summary>
    /// Metadata associated with the session lifecycle event.
    /// </summary>
    [JsonPropertyName("metadata")]
    public SessionLifecycleEventMetadata? Metadata { get; set; }
}

/// <summary>
/// Response from session.getForeground
/// </summary>
public class GetForegroundSessionResponse
{
    /// <summary>
    /// Identifier of the current foreground session, or null if none.
    /// </summary>
    [JsonPropertyName("sessionId")]
    public string? SessionId { get; set; }

    /// <summary>
    /// Workspace path associated with the foreground session.
    /// </summary>
    [JsonPropertyName("workspacePath")]
    public string? WorkspacePath { get; set; }
}

/// <summary>
/// Response from session.setForeground
/// </summary>
public class SetForegroundSessionResponse
{
    /// <summary>
    /// Whether the foreground session was set successfully.
    /// </summary>
    [JsonPropertyName("success")]
    public bool Success { get; set; }

    /// <summary>
    /// Error message if the operation failed.
    /// </summary>
    [JsonPropertyName("error")]
    public string? Error { get; set; }
}

/// <summary>
/// Content data for a single system prompt section in a transform RPC call.
/// </summary>
public class SystemMessageTransformSection
{
    /// <summary>
    /// The content of the section.
    /// </summary>
    [JsonPropertyName("content")]
    public string? Content { get; set; }
}

/// <summary>
/// Response to a systemMessage.transform RPC call.
/// </summary>
public class SystemMessageTransformRpcResponse
{
    /// <summary>
    /// The transformed sections keyed by section identifier.
    /// </summary>
    [JsonPropertyName("sections")]
    public IDictionary<string, SystemMessageTransformSection>? Sections { get; set; }
}

[JsonSourceGenerationOptions(
    JsonSerializerDefaults.Web,
    AllowOutOfOrderMetadataProperties = true,
    NumberHandling = JsonNumberHandling.AllowReadingFromString,
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
[JsonSerializable(typeof(AzureOptions))]
[JsonSerializable(typeof(CustomAgentConfig))]
[JsonSerializable(typeof(GetAuthStatusResponse))]
[JsonSerializable(typeof(GetForegroundSessionResponse))]
[JsonSerializable(typeof(GetModelsResponse))]
[JsonSerializable(typeof(GetStatusResponse))]
[JsonSerializable(typeof(McpServerConfig))]
[JsonSerializable(typeof(MessageOptions))]
[JsonSerializable(typeof(ModelBilling))]
[JsonSerializable(typeof(ModelCapabilities))]
[JsonSerializable(typeof(ModelCapabilitiesOverride))]
[JsonSerializable(typeof(ModelInfo))]
[JsonSerializable(typeof(ModelLimits))]
[JsonSerializable(typeof(ModelPolicy))]
[JsonSerializable(typeof(ModelSupports))]
[JsonSerializable(typeof(ModelVisionLimits))]
[JsonSerializable(typeof(PermissionRequestResult))]
[JsonSerializable(typeof(PingRequest))]
[JsonSerializable(typeof(PingResponse))]
[JsonSerializable(typeof(ProviderConfig))]
[JsonSerializable(typeof(SessionContext))]
[JsonSerializable(typeof(SessionLifecycleEvent))]
[JsonSerializable(typeof(SessionLifecycleEventMetadata))]
[JsonSerializable(typeof(SessionListFilter))]
[JsonSerializable(typeof(SectionOverride))]
[JsonSerializable(typeof(SessionMetadata))]
[JsonSerializable(typeof(SetForegroundSessionResponse))]
[JsonSerializable(typeof(SystemMessageConfig))]
[JsonSerializable(typeof(ToolBinaryResult))]
[JsonSerializable(typeof(ToolInvocation))]
[JsonSerializable(typeof(ToolResultObject))]
[JsonSerializable(typeof(JsonElement))]
[JsonSerializable(typeof(JsonElement?))]
internal partial class TypesJsonContext : JsonSerializerContext;
