/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.AI;
using System.Diagnostics;
using System.Diagnostics.Metrics;
using System.Globalization;
using System.Reflection;
using System.Text;
using System.Text.Encodings.Web;
using System.Text.Json;

namespace GitHub.Copilot.SDK;

/// <summary>
/// Provides OpenTelemetry instrumentation for the Copilot SDK, implementing
/// the Semantic Conventions for Generative AI systems.
/// </summary>
/// <remarks>
/// <para>
/// This class provides an implementation of the Semantic Conventions for Generative AI systems,
/// defined at <see href="https://opentelemetry.io/docs/specs/semconv/gen-ai/" />.
/// The specification is still experimental and subject to change; as such, the telemetry output
/// by this instrumentation is also subject to change.
/// </para>
/// <para>
/// Telemetry is emitted using <see cref="System.Diagnostics.ActivitySource"/> for traces and
/// <see cref="Meter"/> for metrics. No dependency on OpenTelemetry
/// libraries is required. To collect the telemetry, configure an <see cref="ActivityListener"/>
/// or use the OpenTelemetry SDK with the appropriate source name (default <c>"github.copilot.sdk"</c>).
/// </para>
/// </remarks>
internal sealed class CopilotTelemetry : IDisposable
{
    private static readonly JsonWriterOptions s_jsonWriterOptions = new() { Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping };

    private static readonly string? s_sdkVersion =
        typeof(CopilotTelemetry).Assembly.GetCustomAttribute<AssemblyInformationalVersionAttribute>()?.InformationalVersion;

    internal readonly ActivitySource ActivitySource;
    private readonly Meter _meter;

    internal readonly Histogram<double> OperationDurationHistogram;
    internal readonly Histogram<int> TokenUsageHistogram;
    internal readonly Histogram<double> TimeToFirstChunkHistogram;
    internal readonly Histogram<double> TimePerOutputChunkHistogram;

    /// <summary>
    /// Gets or sets whether potentially sensitive data should be included in telemetry.
    /// </summary>
    public bool EnableSensitiveData { get; }

    public CopilotTelemetry(TelemetryConfig? config)
    {
        string sourceName = config?.SourceName ?? OpenTelemetryConsts.DefaultSourceName;

        EnableSensitiveData = config?.EnableSensitiveData ??
            string.Equals(
                Environment.GetEnvironmentVariable(OpenTelemetryConsts.CaptureMessageContentEnvVar),
                "true",
                StringComparison.OrdinalIgnoreCase);

        ActivitySource = new ActivitySource(sourceName, s_sdkVersion);
        _meter = new Meter(sourceName, s_sdkVersion);

        OperationDurationHistogram = _meter.CreateHistogram<double>(
            OpenTelemetryConsts.GenAI.Client.OperationDuration.Name,
            OpenTelemetryConsts.SecondsUnit,
            OpenTelemetryConsts.GenAI.Client.OperationDuration.Description,
            advice: new() { HistogramBucketBoundaries = OpenTelemetryConsts.GenAI.Client.OperationDuration.ExplicitBucketBoundaries });

        TokenUsageHistogram = _meter.CreateHistogram<int>(
            OpenTelemetryConsts.GenAI.Client.TokenUsage.Name,
            OpenTelemetryConsts.TokensUnit,
            OpenTelemetryConsts.GenAI.Client.TokenUsage.Description,
            advice: new() { HistogramBucketBoundaries = OpenTelemetryConsts.GenAI.Client.TokenUsage.ExplicitBucketBoundaries });

        TimeToFirstChunkHistogram = _meter.CreateHistogram<double>(
            OpenTelemetryConsts.GenAI.Client.TimeToFirstChunk.Name,
            OpenTelemetryConsts.SecondsUnit,
            OpenTelemetryConsts.GenAI.Client.TimeToFirstChunk.Description,
            advice: new() { HistogramBucketBoundaries = OpenTelemetryConsts.GenAI.Client.TimeToFirstChunk.ExplicitBucketBoundaries });

        TimePerOutputChunkHistogram = _meter.CreateHistogram<double>(
            OpenTelemetryConsts.GenAI.Client.TimePerOutputChunk.Name,
            OpenTelemetryConsts.SecondsUnit,
            OpenTelemetryConsts.GenAI.Client.TimePerOutputChunk.Description,
            advice: new() { HistogramBucketBoundaries = OpenTelemetryConsts.GenAI.Client.TimePerOutputChunk.ExplicitBucketBoundaries });
    }

    /// <summary>Starts an <c>invoke_agent</c> activity for a session turn.</summary>
    public Activity? StartInvokeAgentActivity(
        string sessionId,
        string? model,
        string providerName,
        string? serverAddress,
        int? serverPort,
        string? agentName = null,
        string? agentDescription = null,
        ActivityContext parentContext = default)
    {
        if (!ActivitySource.HasListeners())
        {
            return null;
        }

        string displayName = string.IsNullOrWhiteSpace(agentName)
            ? OpenTelemetryConsts.GenAI.OperationNames.InvokeAgent
            : $"{OpenTelemetryConsts.GenAI.OperationNames.InvokeAgent} {agentName}";

        ActivityTagsCollection tags = new()
        {
            { OpenTelemetryConsts.GenAI.Operation.Name, OpenTelemetryConsts.GenAI.OperationNames.InvokeAgent },
            { OpenTelemetryConsts.GenAI.Provider.Name, providerName },
            { OpenTelemetryConsts.GenAI.Agent.Id, sessionId },
            { OpenTelemetryConsts.GenAI.Conversation.Id, sessionId },
        };

        if (!string.IsNullOrWhiteSpace(model))
        {
            tags.Add(OpenTelemetryConsts.GenAI.Request.Model, model);
        }

        if (!string.IsNullOrWhiteSpace(agentName))
        {
            tags.Add(OpenTelemetryConsts.GenAI.Agent.Name, agentName);
        }

        if (!string.IsNullOrWhiteSpace(agentDescription))
        {
            tags.Add(OpenTelemetryConsts.GenAI.Agent.Description, agentDescription);
        }

        if (!string.IsNullOrWhiteSpace(serverAddress))
        {
            tags.Add(OpenTelemetryConsts.Server.Address, serverAddress);
            if (serverPort is int port)
            {
                tags.Add(OpenTelemetryConsts.Server.Port, port);
            }
        }

        return ActivitySource.StartActivity(displayName, ActivityKind.Client, parentContext, tags);
    }

    /// <summary>Starts a <c>chat</c> activity for an individual LLM turn within an invoke_agent span.</summary>
    public Activity? StartChatActivity(
        string? model,
        string providerName,
        string? serverAddress,
        int? serverPort,
        ActivityContext parentContext,
        string? conversationId = null)
    {
        if (!ActivitySource.HasListeners())
        {
            return null;
        }

        string displayName = string.IsNullOrWhiteSpace(model)
            ? OpenTelemetryConsts.GenAI.OperationNames.Chat
            : $"{OpenTelemetryConsts.GenAI.OperationNames.Chat} {model}";

        ActivityTagsCollection tags = new()
        {
            { OpenTelemetryConsts.GenAI.Operation.Name, OpenTelemetryConsts.GenAI.OperationNames.Chat },
            { OpenTelemetryConsts.GenAI.Provider.Name, providerName },
        };

        if (!string.IsNullOrWhiteSpace(model))
        {
            tags.Add(OpenTelemetryConsts.GenAI.Request.Model, model);
        }

        if (!string.IsNullOrWhiteSpace(conversationId))
        {
            tags.Add(OpenTelemetryConsts.GenAI.Conversation.Id, conversationId);
        }

        if (!string.IsNullOrWhiteSpace(serverAddress))
        {
            tags.Add(OpenTelemetryConsts.Server.Address, serverAddress);
            if (serverPort is int port)
            {
                tags.Add(OpenTelemetryConsts.Server.Port, port);
            }
        }

        return ActivitySource.StartActivity(displayName, ActivityKind.Client, parentContext, tags);
    }

    /// <summary>Starts an <c>execute_tool</c> activity for a tool call.</summary>
    public Activity? StartExecuteToolActivity(string toolName, string toolCallId, string? description, object? arguments, ActivityContext parentContext = default)
    {
        if (!ActivitySource.HasListeners())
        {
            return null;
        }

        string displayName = $"{OpenTelemetryConsts.GenAI.OperationNames.ExecuteTool} {toolName}";

        ActivityTagsCollection tags = new()
        {
            { OpenTelemetryConsts.GenAI.Operation.Name, OpenTelemetryConsts.GenAI.OperationNames.ExecuteTool },
            { OpenTelemetryConsts.GenAI.Tool.Name, toolName },
            { OpenTelemetryConsts.GenAI.Tool.CallId, toolCallId },
            { OpenTelemetryConsts.GenAI.Tool.Type, "function" },
        };

        if (!string.IsNullOrWhiteSpace(description))
        {
            tags.Add(OpenTelemetryConsts.GenAI.Tool.Description, description);
        }

        if (EnableSensitiveData && arguments is not null)
        {
            tags.Add(OpenTelemetryConsts.GenAI.Tool.CallArguments, SerializeTagValue(arguments));
        }

        return ActivitySource.StartActivity(displayName, ActivityKind.Internal, parentContext, tags);
    }

    /// <summary>Records token usage metrics at turn completion (so error.type can be included).</summary>
    public void RecordTokenUsageMetrics(
        int? inputTokens,
        int? outputTokens,
        string? requestModel,
        string? responseModel,
        string providerName,
        string? serverAddress,
        int? serverPort,
        Exception? error,
        string operationName)
    {
        if (!TokenUsageHistogram.Enabled)
        {
            return;
        }

        TagList baseTags = CreateMetricTags(operationName, requestModel, responseModel, providerName, serverAddress, serverPort);

        if (error is not null)
        {
            baseTags.Add(OpenTelemetryConsts.Error.Type, error.GetType().Name);
        }

        if (inputTokens is int inputCount)
        {
            TagList tags = baseTags;
            tags.Add(OpenTelemetryConsts.GenAI.Token.Type, OpenTelemetryConsts.TokenTypeInput);
            TokenUsageHistogram.Record(inputCount, tags);
        }

        if (outputTokens is int outputCount)
        {
            TagList tags = baseTags;
            tags.Add(OpenTelemetryConsts.GenAI.Token.Type, OpenTelemetryConsts.TokenTypeOutput);
            TokenUsageHistogram.Record(outputCount, tags);
        }
    }

    /// <summary>Records operation duration metric.</summary>
    public void RecordOperationDuration(
        double durationSeconds,
        string? requestModel,
        string? responseModel,
        string providerName,
        string? serverAddress,
        int? serverPort,
        Exception? error,
        string operationName)
    {
        if (OperationDurationHistogram.Enabled)
        {
            TagList tags = CreateMetricTags(operationName, requestModel, responseModel, providerName, serverAddress, serverPort);

            if (error is not null)
            {
                tags.Add(OpenTelemetryConsts.Error.Type, error.GetType().Name);
            }

            OperationDurationHistogram.Record(durationSeconds, tags);
        }
    }

    public void RecordTimeToFirstChunk(
        double durationSeconds,
        string? requestModel,
        string? responseModel,
        string providerName,
        string? serverAddress,
        int? serverPort)
    {
        if (TimeToFirstChunkHistogram.Enabled)
        {
            TimeToFirstChunkHistogram.Record(
                durationSeconds,
                CreateMetricTags(
                    OpenTelemetryConsts.GenAI.OperationNames.Chat,
                    requestModel,
                    responseModel,
                    providerName,
                    serverAddress,
                    serverPort));
        }
    }

    public void RecordTimePerOutputChunk(
        double durationSeconds,
        string? requestModel,
        string? responseModel,
        string providerName,
        string? serverAddress,
        int? serverPort)
    {
        if (TimePerOutputChunkHistogram.Enabled)
        {
            TimePerOutputChunkHistogram.Record(
                durationSeconds,
                CreateMetricTags(
                    OpenTelemetryConsts.GenAI.OperationNames.Chat,
                    requestModel,
                    responseModel,
                    providerName,
                    serverAddress,
                    serverPort));
        }
    }

    public void SetExecuteToolResult(Activity? activity, object? result)
    {
        if (EnableSensitiveData &&
            result is not null &&
            activity is { IsAllDataRequested: true })
        {
            activity.SetTag(OpenTelemetryConsts.GenAI.Tool.CallResult, SerializeTagValue(result));
        }
    }

    /// <summary>Records an error on an activity.</summary>
    public static void RecordError(Activity? activity, Exception error)
    {
        activity?
            .SetTag(OpenTelemetryConsts.Error.Type, error.GetType().Name)
            .SetStatus(ActivityStatusCode.Error, error.Message);
    }

    /// <summary>
    /// Normalizes a provider type string to its OpenTelemetry semantic convention name.
    /// Only the providers supported by BYOK are mapped; all others default to <c>"github"</c>.
    /// </summary>
    private static string NormalizeProviderName(string? providerType)
    {
        return providerType?.Trim().ToLowerInvariant() switch
        {
            "anthropic" => "anthropic",
            "azure" => "azure.ai.openai",
            "openai" => "openai",
            _ => OpenTelemetryConsts.DefaultProviderName,
        };
    }

    private static (string? Address, int? Port) ParseServerAddress(string? baseUrl)
    {
        if (!string.IsNullOrWhiteSpace(baseUrl) &&
            Uri.TryCreate(baseUrl, UriKind.Absolute, out var uri) &&
            !string.IsNullOrWhiteSpace(uri.Host))
        {
            return (uri.Host, uri.Port > 0 ? uri.Port : null);
        }

        return (null, null);
    }

    private static TagList CreateMetricTags(
        string operationName,
        string? requestModel,
        string? responseModel,
        string providerName,
        string? serverAddress,
        int? serverPort)
    {
        TagList tags = default;
        tags.Add(OpenTelemetryConsts.GenAI.Operation.Name, operationName);
        tags.Add(OpenTelemetryConsts.GenAI.Provider.Name, providerName);

        if (!string.IsNullOrWhiteSpace(requestModel))
        {
            tags.Add(OpenTelemetryConsts.GenAI.Request.Model, requestModel);
        }

        if (!string.IsNullOrWhiteSpace(responseModel))
        {
            tags.Add(OpenTelemetryConsts.GenAI.Response.Model, responseModel);
        }

        if (!string.IsNullOrWhiteSpace(serverAddress))
        {
            tags.Add(OpenTelemetryConsts.Server.Address, serverAddress);
            if (serverPort is int port)
            {
                tags.Add(OpenTelemetryConsts.Server.Port, port);
            }
        }

        return tags;
    }

    private static string SerializeTagValue(object value)
    {
        return value switch
        {
            JsonElement jsonElement => jsonElement.GetRawText(),
            string text => text,
            bool boolean => boolean ? "true" : "false",
            float number => number.ToString("R", CultureInfo.InvariantCulture),
            double number => number.ToString("R", CultureInfo.InvariantCulture),
            IFormattable formattable => formattable.ToString(null, CultureInfo.InvariantCulture),
            _ => value.ToString() ?? string.Empty,
        };
    }

    public void Dispose()
    {
        ActivitySource.Dispose();
        _meter.Dispose();
    }

    /// <summary>
    /// Tracks telemetry state for a single session, managing the invoke_agent span
    /// lifecycle across <c>Send</c> / <c>DispatchEvent</c> / turn-completion boundaries.
    /// All public methods are thread-safe.
    /// </summary>
    public sealed class AgentTurnTracker
    {
        private readonly CopilotTelemetry _telemetry;
        private readonly string _sessionId;
        private readonly object _lock = new();

        private readonly string? _requestModel;
        private readonly string? _agentName;
        private readonly string? _agentDescription;
        private readonly string? _systemInstructionsJson;
        private readonly string? _toolDefinitionsJson;
        private readonly bool _isStreaming;

        // Per-invoke_agent mutable state; guarded by _lock.
        private Activity? _agentActivity;
        private long _agentTimestamp;
        private List<OtelMsg>? _agentInputMessages;
        private List<OtelMsg>? _agentOutputMessages;
        private Dictionary<string, SubagentState>? _activeSubagents;

        // Pending tool call parent contexts; guarded by _lock.
        // Tool execute_tool spans are created in OnToolCall (not from ToolExecutionStartEvent)
        // because OnToolCall is where the AIFunction actually runs — creating the Activity
        // there makes it Activity.Current during execution so child spans parent correctly,
        // and the span measures actual SDK-side tool execution time.
        // ToolExecutionStartEvent carries ParentToolCallId (which identifies the owning
        // subagent), so we stash the correct parent context here for OnToolCall to consume.
        private Dictionary<string, ActivityContext>? _pendingToolParents;

        // Tracks tool call IDs that originated from MCP server tools, mapping to
        // the MCP server name, so that ToolExecutionCompleteEvent can emit the
        // correct message type with the right server_tool_call_response discriminator.
        private Dictionary<string, string>? _serverToolCallIds;

        // Agent-level accumulated usage; guarded by _lock.
        // Tracks totals across all chat turns for the invoke_agent span.
        private string? _agentResponseModel;
        private string? _agentResponseId;
        private int _agentTotalInputTokens;
        private int _agentTotalOutputTokens;
        private int _agentTotalCacheReadTokens;
        private int _agentTotalCacheCreationTokens;
        private double _agentTotalCost;
        private double _agentTotalAiu;

        // Per-chat-turn mutable state; guarded by _lock.
        // Reset on each AssistantTurnStartEvent.
        private Activity? _turnActivity;
        private long _turnTimestamp;
        private bool _firstOutputChunkRecorded;
        private TimeSpan _lastOutputChunkElapsed;
        private string? _responseModel;
        private string? _responseId;
        private int _inputTokens;
        private int _outputTokens;
        private int _cacheReadTokens;
        private int _cacheCreationTokens;
        private List<OtelMsg>? _inputMessages;
        private List<OtelMsg>? _outputMessages;

        // Copilot-specific per-turn attributes from AssistantUsageData.
        private double? _turnCost;
        private double? _turnServerDuration;
        private string? _turnInitiator;
        private double? _turnAiu;
        private string? _turnId;
        private string? _turnInteractionId;

        internal AgentTurnTracker(
            CopilotTelemetry telemetry,
            string sessionId,
            string? model,
            ProviderConfig? provider,
            SystemMessageConfig? systemMessage,
            ICollection<AIFunction>? tools,
            bool streaming,
            string? agentName = null,
            string? agentDescription = null)
        {
            _telemetry = telemetry;
            _sessionId = sessionId;
            _requestModel = model;
            _agentName = agentName;
            _agentDescription = agentDescription;
            ProviderName = NormalizeProviderName(provider?.Type);
            (ServerAddress, ServerPort) = ParseServerAddress(provider?.BaseUrl);
            _systemInstructionsJson = BuildSystemInstructionsJson(systemMessage);
            _toolDefinitionsJson = BuildToolDefinitionsJson(tools);
            _isStreaming = streaming;
        }

        internal string ProviderName { get; }

        internal string? ServerAddress { get; }

        internal int? ServerPort { get; }

        /// <summary>Gets the <see cref="ActivityContext"/> of the current invoke_agent activity, if any.</summary>
        internal ActivityContext GetActivityContext()
        {
            lock (_lock)
            {
                return _agentActivity?.Context ?? default;
            }
        }

        /// <summary>
        /// Gets the parent <see cref="ActivityContext"/> for a tool call, which may differ
        /// from the root invoke_agent when a subagent initiated the tool call.
        /// Consumes the stored context (one-time use).
        /// </summary>
        internal ActivityContext GetToolCallParentContext(string toolCallId)
        {
            lock (_lock)
            {
                if (_pendingToolParents is not null && _pendingToolParents.Remove(toolCallId, out var ctx))
                {
                    return ctx;
                }

                return _agentActivity?.Context ?? default;
            }
        }

        /// <summary>
        /// Closes any active spans with an error status. Called when the session is disposed
        /// while a turn may still be in progress, ensuring spans are not orphaned.
        /// </summary>
        internal void CompleteOnDispose()
        {
            lock (_lock)
            {
                if (_agentActivity is not null)
                {
                    var disposeError = new ObjectDisposedException("Session disposed while agent turn was in progress");
                    CompleteChatTurnLocked(disposeError);
                    CompleteAgentTurnLocked(disposeError);
                }
            }
        }

        /// <summary>
        /// Called at the start of each <c>SendAsync</c>. Starts a new invoke_agent span
        /// if one isn't already active, and records a user message event.
        /// </summary>
        internal void BeginSend(string? prompt)
        {
            lock (_lock)
            {
                if (_agentActivity is null)
                {
                    _agentActivity = _telemetry.StartInvokeAgentActivity(
                        _sessionId,
                        _requestModel,
                        ProviderName,
                        ServerAddress,
                        ServerPort,
                        _agentName,
                        _agentDescription);
                    _agentTimestamp = Stopwatch.GetTimestamp();
                    _agentInputMessages = [];
                }

                // Agent-level input = what the caller sent (all user prompts).
                if (_agentInputMessages is not null && !string.IsNullOrWhiteSpace(prompt))
                {
                    _agentInputMessages.Add(new("user", [new("text", Content: prompt)]));
                }

                // Record the user prompt as an input message on the first chat
                // turn's message list when that turn starts. Stash it here for now.
                _pendingUserPrompt = prompt;
            }
        }

        private string? _pendingUserPrompt;

        /// <summary>
        /// Processes a dispatched session event, enriching the current span and
        /// completing the turn on idle/error events.
        /// </summary>
        internal void ProcessEvent(SessionEvent sessionEvent)
        {
            lock (_lock)
            {
                // Route subagent events by ParentToolCallId.
                var parentToolCallId = GetParentToolCallId(sessionEvent);
                if (!string.IsNullOrEmpty(parentToolCallId))
                {
                    if (_activeSubagents?.TryGetValue(parentToolCallId, out var subagentState) == true)
                    {
                        ProcessSubagentEventLocked(subagentState, sessionEvent);
                    }

                    return;
                }

                // Handle subagent lifecycle events.
                switch (sessionEvent)
                {
                    case SubagentStartedEvent started:
                        BeginSubagentLocked(started);
                        return;
                    case SubagentCompletedEvent completed when completed.Data is not null:
                        CompleteSubagentLocked(completed.Data.ToolCallId, error: null);
                        return;
                    case SubagentFailedEvent failed when failed.Data is not null:
                        CompleteSubagentLocked(failed.Data.ToolCallId,
                            new InvalidOperationException($"Subagent '{failed.Data.AgentName}' failed: {failed.Data.Error}"));
                        return;
                }

                // Record chunk timing for main agent events during a turn.
                RecordOutputChunkMetricLocked();

                // Per-turn event processing (writes to the chat child span).
                if (_turnActivity is not null)
                {
                    switch (sessionEvent)
                    {
                        case AssistantMessageEvent messageEvent:
                            {
                                List<OtelPart> parts = [];
                                if (!string.IsNullOrWhiteSpace(messageEvent.Data?.ReasoningText))
                                {
                                    parts.Add(new("reasoning", Content: messageEvent.Data.ReasoningText));
                                }

                                if (!string.IsNullOrWhiteSpace(messageEvent.Data?.Content))
                                {
                                    parts.Add(new("text", Content: messageEvent.Data.Content));
                                }

                                if (parts.Count > 0)
                                {
                                    _outputMessages?.Add(new("assistant", parts));
                                }

                                break;
                            }

                        case AssistantUsageEvent usageEvent:
                            _responseModel = usageEvent.Data.Model;
                            if (!string.IsNullOrWhiteSpace(usageEvent.Data.ApiCallId))
                            {
                                _responseId = usageEvent.Data.ApiCallId;
                            }
                            else if (!string.IsNullOrWhiteSpace(usageEvent.Data.ProviderCallId))
                            {
                                _responseId = usageEvent.Data.ProviderCallId;
                            }
                            if (usageEvent.Data.InputTokens is double inTok)
                            {
                                _inputTokens += (int)inTok;
                            }
                            if (usageEvent.Data.OutputTokens is double outTok)
                            {
                                _outputTokens += (int)outTok;
                            }
                            if (usageEvent.Data.CacheReadTokens is double cacheRead)
                            {
                                _cacheReadTokens += (int)cacheRead;
                            }
                            if (usageEvent.Data.CacheWriteTokens is double cacheWrite)
                            {
                                _cacheCreationTokens += (int)cacheWrite;
                            }

                            // Copilot-specific vendor attributes
                            if (usageEvent.Data.Cost is double cost)
                            {
                                _turnCost = (_turnCost ?? 0) + cost;
                            }
                            if (usageEvent.Data.Duration is double dur)
                            {
                                _turnServerDuration = (_turnServerDuration ?? 0) + dur;
                            }
                            if (!string.IsNullOrWhiteSpace(usageEvent.Data.Initiator))
                            {
                                _turnInitiator = usageEvent.Data.Initiator;
                            }
                            if (usageEvent.Data.CopilotUsage is { } copilotUsage)
                            {
                                _turnAiu = (_turnAiu ?? 0) + copilotUsage.TotalNanoAiu;
                            }
                            break;

                        case SessionModelChangeEvent modelChangeEvent:
                            _responseModel = modelChangeEvent.Data.NewModel;
                            break;

                        case ToolExecutionStartEvent toolStartEvent:
                            {
                                if (toolStartEvent.Data is { } startData)
                                {
                                    var isServerTool = startData.McpServerName is not null;
                                    if (isServerTool && startData.ToolCallId is not null)
                                    {
                                        _serverToolCallIds ??= [];
                                        _serverToolCallIds[startData.ToolCallId] = startData.McpServerName!;
                                    }

                                    _outputMessages?.Add(new("assistant",
                                    [
                                        new(isServerTool ? "server_tool_call" : "tool_call",
                                        Id: startData.ToolCallId,
                                        Name: startData.ToolName,
                                        Arguments: startData.Arguments,
                                        McpServerName: startData.McpServerName)
                                    ]));

                                    // For main agent tool calls, parent is the root invoke_agent.
                                    if (_agentActivity is not null && startData.ToolCallId is not null)
                                    {
                                        _pendingToolParents ??= [];
                                        _pendingToolParents[startData.ToolCallId] = _agentActivity.Context;
                                    }
                                }

                                break;
                            }

                        case ToolExecutionCompleteEvent toolCompleteEvent:
                            {
                                if (toolCompleteEvent.Data is { } toolData)
                                {
                                    var resultContent = toolData.Result?.Content ?? toolData.Error?.Message;
                                    string? serverName = null;
                                    var isServerTool = _serverToolCallIds is not null
                                        && _serverToolCallIds.Remove(toolData.ToolCallId, out serverName);

                                    _inputMessages?.Add(new("tool",
                                    [
                                        new(isServerTool ? "server_tool_call_response" : "tool_call_response",
                                        Id: toolData.ToolCallId,
                                        Response: resultContent,
                                        McpServerName: serverName)
                                    ]));
                                }

                                break;
                            }
                    }
                }

                // Copilot-specific lifecycle events emitted as span events on the
                // current activity (chat turn if active, otherwise invoke_agent).
                {
                    var target = _turnActivity ?? _agentActivity;
                    if (target is not null)
                    {
                        switch (sessionEvent)
                        {
                            case SessionTruncationEvent { Data: { } trunc }:
                                {
                                    ActivityTagsCollection truncTags = [];
                                    if (trunc.TokenLimit != 0)
                                    {
                                        truncTags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.TokenLimit, (long)trunc.TokenLimit);
                                    }
                                    if (trunc.PreTruncationTokensInMessages != 0)
                                    {
                                        truncTags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.PreTokens, (long)trunc.PreTruncationTokensInMessages);
                                    }
                                    if (trunc.PostTruncationTokensInMessages != 0)
                                    {
                                        truncTags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.PostTokens, (long)trunc.PostTruncationTokensInMessages);
                                    }
                                    if (trunc.PreTruncationMessagesLength != 0)
                                    {
                                        truncTags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.PreMessages, (long)trunc.PreTruncationMessagesLength);
                                    }
                                    if (trunc.PostTruncationMessagesLength != 0)
                                    {
                                        truncTags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.PostMessages, (long)trunc.PostTruncationMessagesLength);
                                    }
                                    if (trunc.TokensRemovedDuringTruncation != 0)
                                    {
                                        truncTags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.TokensRemoved, (long)trunc.TokensRemovedDuringTruncation);
                                    }
                                    if (trunc.MessagesRemovedDuringTruncation != 0)
                                    {
                                        truncTags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.MessagesRemoved, (long)trunc.MessagesRemovedDuringTruncation);
                                    }
                                    if (trunc.PerformedBy is not null)
                                    {
                                        truncTags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.PerformedBy, trunc.PerformedBy);
                                    }
                                    target.AddEvent(new(OpenTelemetryConsts.GenAI.CopilotEvent.SessionTruncation, tags: truncTags));
                                    break;
                                }

                            case SessionCompactionStartEvent:
                                target.AddEvent(new(OpenTelemetryConsts.GenAI.CopilotEvent.SessionCompactionStart));
                                break;

                            case SessionCompactionCompleteEvent { Data: { } compaction }:
                                {
                                    ActivityTagsCollection tags = new()
                                {
                                    { OpenTelemetryConsts.GenAI.CopilotEvent.Success, compaction.Success },
                                };

                                    if (_telemetry.EnableSensitiveData && compaction.Error is not null)
                                    {
                                        tags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.Message, compaction.Error);
                                    }

                                    if (compaction.PreCompactionTokens is { } preTokens)
                                    {
                                        tags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.PreTokens, (long)preTokens);
                                    }

                                    if (compaction.PostCompactionTokens is { } postTokens)
                                    {
                                        tags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.PostTokens, (long)postTokens);
                                    }

                                    if (compaction.TokensRemoved is { } tokensRemoved)
                                    {
                                        tags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.TokensRemoved, (long)tokensRemoved);
                                    }

                                    if (compaction.MessagesRemoved is { } messagesRemoved)
                                    {
                                        tags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.MessagesRemoved, (long)messagesRemoved);
                                    }

                                    target.AddEvent(new(OpenTelemetryConsts.GenAI.CopilotEvent.SessionCompactionComplete, tags: tags));
                                    break;
                                }

                            case SkillInvokedEvent { Data: { } skill }:
                                {
                                    ActivityTagsCollection tags = new()
                                {
                                    { OpenTelemetryConsts.GenAI.CopilotEvent.SkillName, skill.Name },
                                    { OpenTelemetryConsts.GenAI.CopilotEvent.SkillPath, skill.Path },
                                };

                                    if (skill.PluginName is not null)
                                    {
                                        tags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.SkillPluginName, skill.PluginName);
                                    }

                                    if (skill.PluginVersion is not null)
                                    {
                                        tags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.SkillPluginVersion, skill.PluginVersion);
                                    }

                                    if (_telemetry.EnableSensitiveData)
                                    {
                                        tags.Add(OpenTelemetryConsts.GenAI.CopilotEvent.SkillContent, skill.Content);
                                    }

                                    target.AddEvent(new(OpenTelemetryConsts.GenAI.CopilotEvent.SkillInvoked, tags: tags));
                                    break;
                                }
                        }
                    }
                }

                // Lifecycle events that start/end turns or the overall agent operation.
                switch (sessionEvent)
                {
                    case AssistantTurnStartEvent turnStartEvent:
                        BeginChatTurnLocked();
                        if (turnStartEvent.Data is { } turnStartData)
                        {
                            _turnId = turnStartData.TurnId;
                            _turnInteractionId = turnStartData.InteractionId;
                        }
                        break;

                    case AssistantTurnEndEvent:
                        CompleteChatTurnLocked(error: null);
                        break;

                    case SessionIdleEvent:
                        CompleteChatTurnLocked(error: null);
                        CompleteAgentTurnLocked(error: null);
                        break;

                    case SessionErrorEvent errorEvent:
                        var ex = new InvalidOperationException($"Session error: {errorEvent.Data?.Message ?? "unknown error"}");
                        CompleteChatTurnLocked(ex);
                        CompleteAgentTurnLocked(ex);
                        break;
                }
            }
        }

        /// <summary>
        /// Completes the current turn with an error (e.g. from a failed SendAsync).
        /// </summary>
        internal void CompleteTurnWithError(Exception error)
        {
            lock (_lock)
            {
                CompleteChatTurnLocked(error);
                CompleteAgentTurnLocked(error);
            }
        }

        /// <summary>
        /// Starts a new chat child span for an LLM turn.
        /// Caller must hold <see cref="_lock"/>.
        /// </summary>
        private void BeginChatTurnLocked()
        {
            Debug.Assert(Monitor.IsEntered(_lock));

            // If there's already an active turn, complete it first (shouldn't normally happen).
            CompleteChatTurnLocked(error: null);

            _responseModel = null;
            _responseId = null;
            _inputTokens = 0;
            _outputTokens = 0;
            _cacheReadTokens = 0;
            _cacheCreationTokens = 0;
            _firstOutputChunkRecorded = false;
            _lastOutputChunkElapsed = TimeSpan.Zero;
            _inputMessages = [];
            _outputMessages = [];
            _turnCost = null;
            _turnServerDuration = null;
            _turnInitiator = null;
            _turnAiu = null;
            _turnId = null;
            _turnInteractionId = null;

            // Add stashed user prompt as input message for the first turn.
            if (!string.IsNullOrWhiteSpace(_pendingUserPrompt))
            {
                _inputMessages.Add(new("user", [new("text", Content: _pendingUserPrompt)]));
                _pendingUserPrompt = null;
            }

            var parentContext = _agentActivity?.Context ?? default;
            _turnActivity = _telemetry.StartChatActivity(
                _requestModel,
                ProviderName,
                ServerAddress,
                ServerPort,
                parentContext,
                _sessionId);

            _turnTimestamp = Stopwatch.GetTimestamp();
        }

        /// <summary>
        /// Completes the current chat child span with per-turn attributes and metrics.
        /// Caller must hold <see cref="_lock"/>.
        /// </summary>
        private void CompleteChatTurnLocked(Exception? error)
        {
            Debug.Assert(Monitor.IsEntered(_lock));

            var activity = _turnActivity;
            if (activity is null)
            {
                return;
            }

            var timestamp = _turnTimestamp;
            var inputMessages = _inputMessages;
            var outputMessages = _outputMessages;
            var responseModel = _responseModel;
            var responseId = _responseId;
            var inputTokens = _inputTokens;
            var outputTokens = _outputTokens;
            var cacheReadTokens = _cacheReadTokens;
            var cacheCreationTokens = _cacheCreationTokens;
            var turnCost = _turnCost;
            var turnServerDuration = _turnServerDuration;
            var turnInitiator = _turnInitiator;
            var turnAiu = _turnAiu;
            var turnId = _turnId;
            var turnInteractionId = _turnInteractionId;

            _turnActivity = null;
            _turnTimestamp = 0;
            _inputMessages = null;
            _outputMessages = null;
            _responseModel = null;
            _responseId = null;
            _inputTokens = 0;
            _outputTokens = 0;
            _cacheReadTokens = 0;
            _cacheCreationTokens = 0;
            _turnCost = null;
            _turnServerDuration = null;
            _turnInitiator = null;
            _turnAiu = null;
            _turnId = null;
            _turnInteractionId = null;

            if (error is not null)
            {
                RecordError(activity, error);
            }

            var finishReason = error is not null ? "error" : "stop";
            activity.SetTag(OpenTelemetryConsts.GenAI.Response.FinishReasons, new[] { finishReason });

            // Agent-level output = only the final turn's output (what the agent
            // returns to the caller). Each turn overwrites; the last one wins.
            if (outputMessages is { Count: > 0 })
            {
                _agentOutputMessages = [];
                foreach (var msg in outputMessages)
                {
                    _agentOutputMessages.Add(msg with { FinishReason = finishReason });
                }
            }

            // Accumulate agent-level usage across turns.
            if (responseModel is not null)
            {
                _agentResponseModel = responseModel;
            }
            if (responseId is not null)
            {
                _agentResponseId = responseId;
            }
            _agentTotalInputTokens += inputTokens;
            _agentTotalOutputTokens += outputTokens;
            _agentTotalCacheReadTokens += cacheReadTokens;
            _agentTotalCacheCreationTokens += cacheCreationTokens;
            if (turnCost is double c)
            {
                _agentTotalCost += c;
            }
            if (turnAiu is double a)
            {
                _agentTotalAiu += a;
            }

            // Set usage-related span attributes for this LLM turn
            if (activity.IsAllDataRequested)
            {
                if (responseModel is not null)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Response.Model, responseModel);
                }
                if (responseId is not null)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Response.Id, responseId);
                }
                if (inputTokens > 0)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Usage.InputTokens, inputTokens);
                }
                if (outputTokens > 0)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Usage.OutputTokens, outputTokens);
                }
                if (cacheReadTokens > 0)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Usage.CacheReadInputTokens, cacheReadTokens);
                }
                if (cacheCreationTokens > 0)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Usage.CacheCreationInputTokens, cacheCreationTokens);
                }

                // Copilot vendor-prefixed attributes on chat spans
                if (turnCost is not null)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Copilot.Cost, turnCost.Value);
                }
                if (turnServerDuration is not null)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Copilot.ServerDuration, turnServerDuration.Value);
                }
                if (turnInitiator is not null)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Copilot.Initiator, turnInitiator);
                }
                if (turnAiu is not null)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Copilot.Aiu, turnAiu.Value);
                }
                if (turnId is not null)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Copilot.TurnId, turnId);
                }
                if (turnInteractionId is not null)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Copilot.InteractionId, turnInteractionId);
                }
            }

            // Set input/output message content as span attributes (sensitive)
            if (_telemetry.EnableSensitiveData)
            {
                var inputMessagesJson = BuildMessagesJson(inputMessages);
                if (!string.IsNullOrWhiteSpace(inputMessagesJson))
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Input.Messages, inputMessagesJson);
                }

                var outputMessagesJson = BuildMessagesJson(outputMessages, finishReason: finishReason);
                if (!string.IsNullOrWhiteSpace(outputMessagesJson))
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Output.Messages, outputMessagesJson);
                }
            }

            // Token usage metrics (per-turn)
            _telemetry.RecordTokenUsageMetrics(
                inputTokens > 0 ? inputTokens : null,
                outputTokens > 0 ? outputTokens : null,
                _requestModel,
                responseModel,
                ProviderName,
                ServerAddress,
                ServerPort,
                error,
                OpenTelemetryConsts.GenAI.OperationNames.Chat);

            // Per-turn operation duration
            if (_telemetry.OperationDurationHistogram.Enabled)
            {
                _telemetry.RecordOperationDuration(
                    Stopwatch.GetElapsedTime(timestamp).TotalSeconds,
                    _requestModel,
                    responseModel,
                    ProviderName,
                    ServerAddress,
                    ServerPort,
                    error: error,
                    operationName: OpenTelemetryConsts.GenAI.OperationNames.Chat);
            }

            _firstOutputChunkRecorded = false;
            _lastOutputChunkElapsed = TimeSpan.Zero;
            activity.Dispose();
        }

        /// <summary>
        /// Completes the invoke_agent span and records overall operation duration.
        /// Caller must hold <see cref="_lock"/>.
        /// </summary>
        private void CompleteAgentTurnLocked(Exception? error)
        {
            Debug.Assert(Monitor.IsEntered(_lock));

            var activity = _agentActivity;
            if (activity is null)
            {
                return;
            }

            var timestamp = _agentTimestamp;

            _agentActivity = null;
            _agentTimestamp = 0;
            _pendingUserPrompt = null;
            var agentInputMessages = _agentInputMessages;
            var agentOutputMessages = _agentOutputMessages;
            _agentInputMessages = null;
            _agentOutputMessages = null;

            // Complete any remaining subagents before closing the parent.
            if (_activeSubagents is { Count: > 0 })
            {
                foreach (var key in _activeSubagents.Keys.ToList())
                {
                    CompleteSubagentLocked(key, error);
                }
            }

            _activeSubagents = null;
            _pendingToolParents = null;
            _serverToolCallIds = null;

            if (error is not null)
            {
                RecordError(activity, error);
            }

            var finishReason = error is not null ? "error" : "stop";
            activity.SetTag(OpenTelemetryConsts.GenAI.Response.FinishReasons, new[] { finishReason });

            // Set accumulated usage across all chat turns on the invoke_agent span.
            var agentResponseModel = _agentResponseModel;
            var agentResponseId = _agentResponseId;
            var agentTotalInputTokens = _agentTotalInputTokens;
            var agentTotalOutputTokens = _agentTotalOutputTokens;
            var agentTotalCacheReadTokens = _agentTotalCacheReadTokens;
            var agentTotalCacheCreationTokens = _agentTotalCacheCreationTokens;
            var agentTotalCost = _agentTotalCost;
            var agentTotalAiu = _agentTotalAiu;
            _agentResponseModel = null;
            _agentResponseId = null;
            _agentTotalInputTokens = 0;
            _agentTotalOutputTokens = 0;
            _agentTotalCacheReadTokens = 0;
            _agentTotalCacheCreationTokens = 0;
            _agentTotalCost = 0;
            _agentTotalAiu = 0;

            if (activity.IsAllDataRequested)
            {
                if (agentResponseModel is not null)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Response.Model, agentResponseModel);
                }
                if (agentResponseId is not null)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Response.Id, agentResponseId);
                }
                if (agentTotalInputTokens > 0)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Usage.InputTokens, agentTotalInputTokens);
                }
                if (agentTotalOutputTokens > 0)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Usage.OutputTokens, agentTotalOutputTokens);
                }
                if (agentTotalCacheReadTokens > 0)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Usage.CacheReadInputTokens, agentTotalCacheReadTokens);
                }
                if (agentTotalCacheCreationTokens > 0)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Usage.CacheCreationInputTokens, agentTotalCacheCreationTokens);
                }

                // Copilot vendor-prefixed attributes on invoke_agent span
                if (agentTotalCost > 0)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Copilot.Cost, agentTotalCost);
                }
                if (agentTotalAiu > 0)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Copilot.Aiu, agentTotalAiu);
                }
            }

            // Agent-level input = caller's message; output = agent's final response.
            if (_telemetry.EnableSensitiveData)
            {
                var inputJson = BuildMessagesJson(agentInputMessages);
                if (inputJson is not null)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Input.Messages, inputJson);
                }

                var outputJson = BuildMessagesJson(agentOutputMessages);
                if (outputJson is not null)
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.Output.Messages, outputJson);
                }

                if (!string.IsNullOrWhiteSpace(_systemInstructionsJson))
                {
                    activity.SetTag(OpenTelemetryConsts.GenAI.SystemInstructions, _systemInstructionsJson);
                }
            }

            if (!string.IsNullOrWhiteSpace(_toolDefinitionsJson))
            {
                activity.SetTag(OpenTelemetryConsts.GenAI.Tool.Definitions, _toolDefinitionsJson);
            }

            if (_telemetry.OperationDurationHistogram.Enabled)
            {
                _telemetry.RecordOperationDuration(
                    Stopwatch.GetElapsedTime(timestamp).TotalSeconds,
                    _requestModel,
                    agentResponseModel,
                    ProviderName,
                    ServerAddress,
                    ServerPort,
                    error: error,
                    operationName: OpenTelemetryConsts.GenAI.OperationNames.InvokeAgent);
            }

            activity.Dispose();
        }

        /// <summary>
        /// Records streaming chunk timing metrics.
        /// Caller must hold <see cref="_lock"/>.
        /// </summary>
        private void RecordOutputChunkMetricLocked()
        {
            Debug.Assert(Monitor.IsEntered(_lock));

            if (!_isStreaming || _turnTimestamp == 0)
            {
                return;
            }

            var elapsed = Stopwatch.GetElapsedTime(_turnTimestamp);

            if (!_firstOutputChunkRecorded)
            {
                _firstOutputChunkRecorded = true;
                _lastOutputChunkElapsed = elapsed;
                _telemetry.RecordTimeToFirstChunk(
                    elapsed.TotalSeconds,
                    _requestModel,
                    null, // response model not yet known during streaming
                    ProviderName,
                    ServerAddress,
                    ServerPort);
                return;
            }

            var delta = elapsed - _lastOutputChunkElapsed;
            _lastOutputChunkElapsed = elapsed;
            _telemetry.RecordTimePerOutputChunk(
                delta.TotalSeconds,
                _requestModel,
                null, // response model not yet known during streaming
                ProviderName,
                ServerAddress,
                ServerPort);
        }

        /// <summary>
        /// Extracts <c>ParentToolCallId</c> from events that carry it.
        /// A non-null/non-empty value indicates the event belongs to a subagent.
        /// </summary>
        private static string? GetParentToolCallId(SessionEvent evt) => evt switch
        {
            AssistantUsageEvent e => e.Data?.ParentToolCallId,
            AssistantMessageEvent e => e.Data?.ParentToolCallId,
            AssistantMessageDeltaEvent e => e.Data?.ParentToolCallId,
            ToolExecutionStartEvent e => e.Data?.ParentToolCallId,
            ToolExecutionCompleteEvent e => e.Data?.ParentToolCallId,
            _ => null,
        };

        /// <summary>
        /// Creates a nested <c>invoke_agent</c> + <c>chat</c> span pair for a subagent.
        /// Caller must hold <see cref="_lock"/>.
        /// </summary>
        private void BeginSubagentLocked(SubagentStartedEvent started)
        {
            Debug.Assert(Monitor.IsEntered(_lock));

            var data = started.Data;
            if (data is null)
            {
                return;
            }

            var parentContext = _agentActivity?.Context ?? default;
            var invokeActivity = _telemetry.StartInvokeAgentActivity(
                _sessionId,
                _requestModel,
                ProviderName,
                ServerAddress,
                ServerPort,
                agentName: data.AgentName,
                agentDescription: data.AgentDescription,
                parentContext: parentContext);

            if (invokeActivity is null)
            {
                return;
            }

            var chatActivity = _telemetry.StartChatActivity(
                _requestModel,
                ProviderName,
                ServerAddress,
                ServerPort,
                invokeActivity.Context,
                _sessionId);

            var state = new SubagentState
            {
                InvokeAgentActivity = invokeActivity,
                InvokeAgentTimestamp = Stopwatch.GetTimestamp(),
                ChatActivity = chatActivity,
                AgentName = data.AgentName,
            };

            _activeSubagents ??= new(StringComparer.Ordinal);
            _activeSubagents[data.ToolCallId] = state;
        }

        /// <summary>
        /// Routes an event to its owning subagent's spans.
        /// Caller must hold <see cref="_lock"/>.
        /// </summary>
        private void ProcessSubagentEventLocked(SubagentState subagent, SessionEvent sessionEvent)
        {
            Debug.Assert(Monitor.IsEntered(_lock));

            switch (sessionEvent)
            {
                case AssistantUsageEvent usageEvent:
                    subagent.ResponseModel = usageEvent.Data.Model;

                    // Update response model on chat span if the subagent is using
                    // a different model than what was set at span creation time.
                    if (!string.IsNullOrWhiteSpace(usageEvent.Data.Model))
                    {
                        subagent.ChatActivity?.SetTag(OpenTelemetryConsts.GenAI.Response.Model, usageEvent.Data.Model);
                    }

                    if (!string.IsNullOrWhiteSpace(usageEvent.Data.ApiCallId))
                    {
                        subagent.ResponseId = usageEvent.Data.ApiCallId;
                    }
                    else if (!string.IsNullOrWhiteSpace(usageEvent.Data.ProviderCallId))
                    {
                        subagent.ResponseId = usageEvent.Data.ProviderCallId;
                    }

                    if (usageEvent.Data.InputTokens is double inTok)
                    {
                        subagent.InputTokens += (int)inTok;
                    }

                    if (usageEvent.Data.OutputTokens is double outTok)
                    {
                        subagent.OutputTokens += (int)outTok;
                    }

                    if (usageEvent.Data.CacheReadTokens is double cacheRead)
                    {
                        subagent.CacheReadTokens += (int)cacheRead;
                    }

                    if (usageEvent.Data.CacheWriteTokens is double cacheWrite)
                    {
                        subagent.CacheCreationTokens += (int)cacheWrite;
                    }

                    break;

                case AssistantMessageEvent messageEvent:
                    {
                        List<OtelPart> parts = [];
                        if (!string.IsNullOrWhiteSpace(messageEvent.Data?.ReasoningText))
                        {
                            parts.Add(new("reasoning", Content: messageEvent.Data.ReasoningText));
                        }

                        if (!string.IsNullOrWhiteSpace(messageEvent.Data?.Content))
                        {
                            parts.Add(new("text", Content: messageEvent.Data.Content));
                        }

                        if (parts.Count > 0)
                        {
                            subagent.OutputMessages.Add(new("assistant", parts));
                        }

                        break;
                    }

                case ToolExecutionStartEvent toolStartEvent:
                    {
                        if (toolStartEvent.Data is { } startData)
                        {
                            var isServerTool = startData.McpServerName is not null;
                            if (isServerTool && startData.ToolCallId is not null)
                            {
                                _serverToolCallIds ??= [];
                                _serverToolCallIds[startData.ToolCallId] = startData.McpServerName!;
                            }

                            subagent.OutputMessages.Add(new("assistant",
                            [
                                new(isServerTool ? "server_tool_call" : "tool_call",
                                Id: startData.ToolCallId,
                                Name: startData.ToolName,
                                Arguments: startData.Arguments,
                                McpServerName: startData.McpServerName)
                            ]));

                            // Store the parent context for OnToolCall to use.
                            // For subagent tool calls, parent is the subagent's invoke_agent.
                            if (subagent.InvokeAgentActivity is not null && startData.ToolCallId is not null)
                            {
                                _pendingToolParents ??= [];
                                _pendingToolParents[startData.ToolCallId] = subagent.InvokeAgentActivity.Context;
                            }
                        }

                        break;
                    }

                case ToolExecutionCompleteEvent toolCompleteEvent:
                    {
                        if (toolCompleteEvent.Data is { } toolData)
                        {
                            var resultContent = toolData.Result?.Content ?? toolData.Error?.Message;
                            string? serverName = null;
                            var isServerTool = _serverToolCallIds is not null
                                && _serverToolCallIds.Remove(toolData.ToolCallId, out serverName);

                            subagent.InputMessages.Add(new("tool",
                            [
                                new(isServerTool ? "server_tool_call_response" : "tool_call_response",
                                Id: toolData.ToolCallId,
                                Response: resultContent,
                                McpServerName: serverName)
                            ]));
                        }

                        break;
                    }
            }
        }

        /// <summary>
        /// Completes a subagent's <c>chat</c> and <c>invoke_agent</c> spans.
        /// Caller must hold <see cref="_lock"/>.
        /// </summary>
        private void CompleteSubagentLocked(string toolCallId, Exception? error)
        {
            Debug.Assert(Monitor.IsEntered(_lock));

            if (_activeSubagents is null || !_activeSubagents.Remove(toolCallId, out var subagent))
            {
                return;
            }

            var finishReason = error is not null ? "error" : "stop";

            // -- Complete the chat child span --
            var chatActivity = subagent.ChatActivity;
            if (chatActivity is not null)
            {
                if (error is not null)
                {
                    RecordError(chatActivity, error);
                }

                chatActivity.SetTag(OpenTelemetryConsts.GenAI.Response.FinishReasons, new[] { finishReason });

                if (chatActivity.IsAllDataRequested)
                {
                    if (subagent.ResponseModel is not null)
                    {
                        chatActivity.SetTag(OpenTelemetryConsts.GenAI.Response.Model, subagent.ResponseModel);
                    }

                    if (subagent.ResponseId is not null)
                    {
                        chatActivity.SetTag(OpenTelemetryConsts.GenAI.Response.Id, subagent.ResponseId);
                    }

                    if (subagent.InputTokens > 0)
                    {
                        chatActivity.SetTag(OpenTelemetryConsts.GenAI.Usage.InputTokens, subagent.InputTokens);
                    }

                    if (subagent.OutputTokens > 0)
                    {
                        chatActivity.SetTag(OpenTelemetryConsts.GenAI.Usage.OutputTokens, subagent.OutputTokens);
                    }

                    if (subagent.CacheReadTokens > 0)
                    {
                        chatActivity.SetTag(OpenTelemetryConsts.GenAI.Usage.CacheReadInputTokens, subagent.CacheReadTokens);
                    }

                    if (subagent.CacheCreationTokens > 0)
                    {
                        chatActivity.SetTag(OpenTelemetryConsts.GenAI.Usage.CacheCreationInputTokens, subagent.CacheCreationTokens);
                    }
                }

                if (_telemetry.EnableSensitiveData)
                {
                    if (subagent.InputMessages.Count > 0)
                    {
                        var inputJson = BuildMessagesJson(subagent.InputMessages);
                        if (inputJson is not null)
                        {
                            chatActivity.SetTag(OpenTelemetryConsts.GenAI.Input.Messages, inputJson);
                        }
                    }

                    if (subagent.OutputMessages.Count > 0)
                    {
                        var outputJson = BuildMessagesJson(subagent.OutputMessages, finishReason: finishReason);
                        if (outputJson is not null)
                        {
                            chatActivity.SetTag(OpenTelemetryConsts.GenAI.Output.Messages, outputJson);
                        }
                    }
                }

                _telemetry.RecordTokenUsageMetrics(
                    subagent.InputTokens > 0 ? subagent.InputTokens : null,
                    subagent.OutputTokens > 0 ? subagent.OutputTokens : null,
                    subagent.ResponseModel ?? _requestModel,
                    subagent.ResponseModel,
                    ProviderName,
                    ServerAddress,
                    ServerPort,
                    error,
                    OpenTelemetryConsts.GenAI.OperationNames.Chat);

                chatActivity.Dispose();
            }

            // -- Complete the invoke_agent span --
            var invokeActivity = subagent.InvokeAgentActivity;
            if (invokeActivity is not null)
            {
                if (error is not null)
                {
                    RecordError(invokeActivity, error);
                }

                invokeActivity.SetTag(OpenTelemetryConsts.GenAI.Response.FinishReasons, new[] { finishReason });

                if (invokeActivity.IsAllDataRequested)
                {
                    if (subagent.ResponseModel is not null)
                    {
                        invokeActivity.SetTag(OpenTelemetryConsts.GenAI.Response.Model, subagent.ResponseModel);
                    }

                    if (subagent.ResponseId is not null)
                    {
                        invokeActivity.SetTag(OpenTelemetryConsts.GenAI.Response.Id, subagent.ResponseId);
                    }

                    if (subagent.InputTokens > 0)
                    {
                        invokeActivity.SetTag(OpenTelemetryConsts.GenAI.Usage.InputTokens, subagent.InputTokens);
                    }

                    if (subagent.OutputTokens > 0)
                    {
                        invokeActivity.SetTag(OpenTelemetryConsts.GenAI.Usage.OutputTokens, subagent.OutputTokens);
                    }

                    if (subagent.CacheReadTokens > 0)
                    {
                        invokeActivity.SetTag(OpenTelemetryConsts.GenAI.Usage.CacheReadInputTokens, subagent.CacheReadTokens);
                    }

                    if (subagent.CacheCreationTokens > 0)
                    {
                        invokeActivity.SetTag(OpenTelemetryConsts.GenAI.Usage.CacheCreationInputTokens, subagent.CacheCreationTokens);
                    }
                }

                if (_telemetry.EnableSensitiveData && subagent.OutputMessages.Count > 0)
                {
                    var outputJson = BuildMessagesJson(
                        subagent.OutputMessages.Select(m => m with { FinishReason = finishReason }).ToList());
                    if (outputJson is not null)
                    {
                        invokeActivity.SetTag(OpenTelemetryConsts.GenAI.Output.Messages, outputJson);
                    }
                }

                if (_telemetry.OperationDurationHistogram.Enabled)
                {
                    _telemetry.RecordOperationDuration(
                        Stopwatch.GetElapsedTime(subagent.InvokeAgentTimestamp).TotalSeconds,
                        subagent.ResponseModel ?? _requestModel,
                        subagent.ResponseModel,
                        ProviderName,
                        ServerAddress,
                        ServerPort,
                        error: error,
                        operationName: OpenTelemetryConsts.GenAI.OperationNames.InvokeAgent);
                }

                invokeActivity.Dispose();
            }
        }

        /// <summary>Tracks mutable state for an active subagent's spans.</summary>
        private sealed class SubagentState
        {
            public Activity? InvokeAgentActivity;
            public long InvokeAgentTimestamp;
            public Activity? ChatActivity;
            public string? AgentName;
            public string? ResponseModel;
            public string? ResponseId;
            public int InputTokens;
            public int OutputTokens;
            public int CacheReadTokens;
            public int CacheCreationTokens;
            public List<OtelMsg> InputMessages = [];
            public List<OtelMsg> OutputMessages = [];
        }

        private static string? BuildMessagesJson(List<OtelMsg>? messages, string? finishReason = null)
        {
            if (messages is not { Count: > 0 })
            {
                return null;
            }

            using var stream = new MemoryStream();
            using (var writer = new Utf8JsonWriter(stream, s_jsonWriterOptions))
            {
                writer.WriteStartArray();
                foreach (var message in messages)
                {
                    if (message.Parts.Count == 0)
                    {
                        continue;
                    }

                    writer.WriteStartObject();
                    writer.WriteString("role", message.Role);
                    writer.WritePropertyName("parts");
                    writer.WriteStartArray();
                    foreach (var part in message.Parts)
                    {
                        writer.WriteStartObject();
                        writer.WriteString("type", part.Type);

                        switch (part.Type)
                        {
                            case "server_tool_call":
                                if (part.Id is not null)
                                {
                                    writer.WriteString("id", part.Id);
                                }

                                if (part.Name is not null)
                                {
                                    writer.WriteString("name", part.Name);
                                }

                                // Spec requires a nested server_tool_call object with a type discriminator.
                                // MCP tools use type "mcp" with a server_name field per the MEAI convention.
                                writer.WritePropertyName("server_tool_call");
                                writer.WriteStartObject();
                                writer.WriteString("type", "mcp");
                                if (part.McpServerName is not null)
                                {
                                    writer.WriteString("server_name", part.McpServerName);
                                }
                                if (part.Arguments is not null)
                                {
                                    writer.WritePropertyName("arguments");
                                    WriteJsonValue(writer, part.Arguments);
                                }

                                writer.WriteEndObject();
                                break;

                            case "server_tool_call_response":
                                if (part.Id is not null)
                                {
                                    writer.WriteString("id", part.Id);
                                }

                                // Spec requires a nested server_tool_call_response object with a type discriminator.
                                writer.WritePropertyName("server_tool_call_response");
                                writer.WriteStartObject();
                                writer.WriteString("type", "mcp");
                                if (part.McpServerName is not null)
                                {
                                    writer.WriteString("server_name", part.McpServerName);
                                }
                                if (part.Response is not null)
                                {
                                    writer.WritePropertyName("response");
                                    WriteJsonValue(writer, part.Response);
                                }

                                writer.WriteEndObject();
                                break;

                            default:
                                if (part.Content is not null)
                                {
                                    writer.WriteString("content", part.Content);
                                }

                                if (part.Id is not null)
                                {
                                    writer.WriteString("id", part.Id);
                                }

                                if (part.Name is not null)
                                {
                                    writer.WriteString("name", part.Name);
                                }

                                if (part.Arguments is not null)
                                {
                                    writer.WritePropertyName("arguments");
                                    WriteJsonValue(writer, part.Arguments);
                                }

                                if (part.Response is not null)
                                {
                                    writer.WritePropertyName("response");
                                    WriteJsonValue(writer, part.Response);
                                }

                                break;
                        }

                        writer.WriteEndObject();
                    }

                    writer.WriteEndArray();
                    var effectiveFinishReason = message.FinishReason ?? finishReason;
                    if (effectiveFinishReason is not null)
                    {
                        writer.WriteString("finish_reason", effectiveFinishReason);
                    }

                    writer.WriteEndObject();
                }

                writer.WriteEndArray();
                writer.Flush();
            }

            return MemoryStreamToUtf8String(stream);
        }

        private static void WriteJsonValue(Utf8JsonWriter writer, object value)
        {
            switch (value)
            {
                case JsonElement jsonElement:
                    jsonElement.WriteTo(writer);
                    break;
                case string text:
                    writer.WriteStringValue(text);
                    break;
                default:
                    writer.WriteStringValue(value.ToString());
                    break;
            }
        }

        private sealed record OtelMsg(
            string Role,
            List<OtelPart> Parts,
            string? FinishReason = null);

        private sealed record OtelPart(
            string Type,
            string? Content = null,
            string? Id = null,
            string? Name = null,
            object? Arguments = null,
            object? Response = null,
            string? McpServerName = null);

        internal static string? BuildSystemInstructionsJson(SystemMessageConfig? systemMessage)
        {
            if (string.IsNullOrWhiteSpace(systemMessage?.Content))
            {
                return null;
            }

            using var stream = new MemoryStream();
            using (var writer = new Utf8JsonWriter(stream, s_jsonWriterOptions))
            {
                writer.WriteStartArray();
                writer.WriteStartObject();
                writer.WriteString("type", "text");
                writer.WriteString("content", systemMessage.Content);
                writer.WriteEndObject();
                writer.WriteEndArray();
                writer.Flush();
            }

            return MemoryStreamToUtf8String(stream);
        }

        internal static string? BuildToolDefinitionsJson(ICollection<AIFunction>? tools)
        {
            if (tools is not { Count: > 0 })
            {
                return null;
            }

            using var stream = new MemoryStream();
            using (var writer = new Utf8JsonWriter(stream, s_jsonWriterOptions))
            {
                writer.WriteStartArray();
                foreach (var tool in tools)
                {
                    writer.WriteStartObject();
                    writer.WriteString("type", "function");
                    writer.WriteString("name", tool.Name);

                    if (!string.IsNullOrWhiteSpace(tool.Description))
                    {
                        writer.WriteString("description", tool.Description);
                    }

                    if (tool.JsonSchema.ValueKind is not JsonValueKind.Undefined and not JsonValueKind.Null)
                    {
                        writer.WritePropertyName("parameters");
                        tool.JsonSchema.WriteTo(writer);
                    }

                    writer.WriteEndObject();
                }

                writer.WriteEndArray();
                writer.Flush();
            }

            return MemoryStreamToUtf8String(stream);
        }

        private static string MemoryStreamToUtf8String(MemoryStream stream) =>
            Encoding.UTF8.GetString(stream.GetBuffer(), 0, (int)stream.Length);
    }
}
