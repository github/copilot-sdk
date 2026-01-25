/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

namespace GitHub.Copilot.SDK.Telemetry;

/// <summary>
/// Constants for OpenTelemetry instrumentation following GenAI Semantic Conventions.
/// </summary>
/// <remarks>
/// See: https://opentelemetry.io/docs/specs/semconv/gen-ai/
/// </remarks>
internal static class OpenTelemetryConstants
{
    /// <summary>
    /// The AppContext switch to enable OpenTelemetry telemetry.
    /// </summary>
    public const string EnableTelemetrySwitch = "GitHub.Copilot.EnableOpenTelemetry";

    /// <summary>
    /// Environment variable to enable OpenTelemetry telemetry.
    /// </summary>
    public const string EnableTelemetryEnvVar = "GITHUB_COPILOT_ENABLE_OPEN_TELEMETRY";

    /// <summary>
    /// The ActivitySource name for GitHub Copilot SDK telemetry.
    /// </summary>
    public const string ActivitySourceName = "GitHub.Copilot.SDK";

    /// <summary>
    /// The Meter name for GitHub Copilot SDK metrics.
    /// </summary>
    public const string MeterName = "GitHub.Copilot.SDK";

    // GenAI Semantic Convention attribute names
    // See: https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-spans/

    /// <summary>The name of the GenAI system (e.g., "github-copilot").</summary>
    public const string GenAiSystem = "gen_ai.system";

    /// <summary>The operation being performed (e.g., "chat").</summary>
    public const string GenAiOperationName = "gen_ai.operation.name";

    /// <summary>The model requested by the user.</summary>
    public const string GenAiRequestModel = "gen_ai.request.model";

    /// <summary>The model that generated the response.</summary>
    public const string GenAiResponseModel = "gen_ai.response.model";

    /// <summary>Number of input tokens used.</summary>
    public const string GenAiUsageInputTokens = "gen_ai.usage.input_tokens";

    /// <summary>Number of output tokens generated.</summary>
    public const string GenAiUsageOutputTokens = "gen_ai.usage.output_tokens";

    /// <summary>The name of the tool being called.</summary>
    public const string GenAiToolName = "gen_ai.tool.name";

    /// <summary>The unique identifier for the tool call.</summary>
    public const string GenAiToolCallId = "gen_ai.tool.call_id";

    // Copilot-specific attributes

    /// <summary>The session identifier.</summary>
    public const string CopilotSessionId = "copilot.session.id";

    /// <summary>The turn identifier within a session.</summary>
    public const string CopilotTurnId = "copilot.turn.id";

    /// <summary>The subagent name.</summary>
    public const string CopilotSubagentName = "copilot.subagent.name";

    /// <summary>The hook type.</summary>
    public const string CopilotHookType = "copilot.hook.type";

    /// <summary>The hook invocation identifier.</summary>
    public const string CopilotHookInvocationId = "copilot.hook.invocation_id";

    /// <summary>Whether the operation succeeded.</summary>
    public const string CopilotSuccess = "copilot.success";

    /// <summary>The error message if failed.</summary>
    public const string CopilotErrorMessage = "copilot.error.message";

    /// <summary>The cost of the operation.</summary>
    public const string CopilotCost = "copilot.cost";

    /// <summary>Duration of the operation in milliseconds.</summary>
    public const string CopilotDurationMs = "copilot.duration_ms";

    /// <summary>Cache read tokens.</summary>
    public const string CopilotCacheReadTokens = "copilot.cache.read_tokens";

    /// <summary>Cache write tokens.</summary>
    public const string CopilotCacheWriteTokens = "copilot.cache.write_tokens";

    // Span names

    /// <summary>Span name for a session.</summary>
    public const string SpanNameSession = "copilot.session";

    /// <summary>Span name for an assistant turn.</summary>
    public const string SpanNameTurn = "copilot.turn";

    /// <summary>Span name for tool execution.</summary>
    public const string SpanNameToolExecution = "copilot.tool_execution";

    /// <summary>Span name for subagent execution.</summary>
    public const string SpanNameSubagent = "copilot.subagent";

    /// <summary>Span name for hook execution.</summary>
    public const string SpanNameHook = "copilot.hook";

    /// <summary>Span name for inference/LLM call.</summary>
    public const string SpanNameInference = "copilot.inference";

    // Metric names

    /// <summary>Counter for total tokens used.</summary>
    public const string MetricTokensTotal = "copilot.tokens.total";

    /// <summary>Counter for input tokens.</summary>
    public const string MetricTokensInput = "copilot.tokens.input";

    /// <summary>Counter for output tokens.</summary>
    public const string MetricTokensOutput = "copilot.tokens.output";

    /// <summary>Counter for total cost.</summary>
    public const string MetricCostTotal = "copilot.cost.total";

    /// <summary>Histogram for operation duration.</summary>
    public const string MetricDuration = "copilot.duration";

    /// <summary>Counter for tool executions.</summary>
    public const string MetricToolExecutions = "copilot.tool_executions";

    /// <summary>Counter for session errors.</summary>
    public const string MetricErrors = "copilot.errors";
}
