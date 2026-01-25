/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;
using System.Diagnostics.Metrics;

namespace GitHub.Copilot.SDK.Telemetry;

/// <summary>
/// Provides OpenTelemetry instrumentation for the GitHub Copilot SDK.
/// </summary>
/// <remarks>
/// <para>
/// Telemetry is disabled by default. Enable it using one of these methods:
/// </para>
/// <list type="bullet">
///     <item>Set the AppContext switch: <c>AppContext.SetSwitch("GitHub.Copilot.EnableOpenTelemetry", true)</c></item>
///     <item>Set the environment variable: <c>GITHUB_COPILOT_ENABLE_OPEN_TELEMETRY=true</c></item>
/// </list>
/// <para>
/// Then configure your TracerProvider and MeterProvider to listen:
/// </para>
/// <code>
/// services.AddOpenTelemetry()
///     .WithTracing(tracing => tracing.AddSource("GitHub.Copilot.SDK"))
///     .WithMetrics(metrics => metrics.AddMeter("GitHub.Copilot.SDK"));
/// </code>
/// </remarks>
public static class CopilotTelemetry
{
    private static readonly Lazy<bool> s_isEnabled = new(DetermineIfEnabled);

    /// <summary>
    /// Gets the ActivitySource for creating spans.
    /// </summary>
    internal static ActivitySource ActivitySource { get; } = new(
        OpenTelemetryConstants.ActivitySourceName,
        typeof(CopilotTelemetry).Assembly.GetName().Version?.ToString() ?? "1.0.0");

    /// <summary>
    /// Gets the Meter for recording metrics.
    /// </summary>
    internal static Meter Meter { get; } = new(
        OpenTelemetryConstants.MeterName,
        typeof(CopilotTelemetry).Assembly.GetName().Version?.ToString() ?? "1.0.0");

    // Metrics instruments
    internal static Counter<long> TokensInputCounter { get; } = Meter.CreateCounter<long>(
        OpenTelemetryConstants.MetricTokensInput,
        unit: "{token}",
        description: "Number of input tokens used");

    internal static Counter<long> TokensOutputCounter { get; } = Meter.CreateCounter<long>(
        OpenTelemetryConstants.MetricTokensOutput,
        unit: "{token}",
        description: "Number of output tokens generated");

    internal static Counter<double> CostCounter { get; } = Meter.CreateCounter<double>(
        OpenTelemetryConstants.MetricCostTotal,
        unit: "{dollar}",
        description: "Total cost of operations");

    internal static Counter<long> ToolExecutionsCounter { get; } = Meter.CreateCounter<long>(
        OpenTelemetryConstants.MetricToolExecutions,
        unit: "{execution}",
        description: "Number of tool executions");

    internal static Counter<long> ErrorsCounter { get; } = Meter.CreateCounter<long>(
        OpenTelemetryConstants.MetricErrors,
        unit: "{error}",
        description: "Number of errors");

    internal static Histogram<double> DurationHistogram { get; } = Meter.CreateHistogram<double>(
        OpenTelemetryConstants.MetricDuration,
        unit: "ms",
        description: "Duration of operations in milliseconds");

    /// <summary>
    /// Gets a value indicating whether telemetry is enabled.
    /// </summary>
    public static bool IsEnabled => s_isEnabled.Value;

    private static bool DetermineIfEnabled()
    {
        // Check AppContext switch first
        if (AppContext.TryGetSwitch(OpenTelemetryConstants.EnableTelemetrySwitch, out var isEnabled))
        {
            return isEnabled;
        }

        // Fall back to environment variable
        var envValue = Environment.GetEnvironmentVariable(OpenTelemetryConstants.EnableTelemetryEnvVar);
        return string.Equals(envValue, "true", StringComparison.OrdinalIgnoreCase) ||
               string.Equals(envValue, "1", StringComparison.Ordinal);
    }

    /// <summary>
    /// Starts an activity (span) if telemetry is enabled and there are listeners.
    /// </summary>
    internal static Activity? StartActivity(string name, ActivityKind kind = ActivityKind.Internal)
    {
        if (!IsEnabled)
        {
            return null;
        }

        return ActivitySource.StartActivity(name, kind);
    }

    /// <summary>
    /// Sets common GenAI attributes on an activity.
    /// </summary>
    internal static void SetGenAiAttributes(Activity? activity, string? model = null)
    {
        if (activity is null) return;

        activity.SetTag(OpenTelemetryConstants.GenAiSystem, "github-copilot");

        if (!string.IsNullOrEmpty(model))
        {
            activity.SetTag(OpenTelemetryConstants.GenAiRequestModel, model);
        }
    }

    /// <summary>
    /// Records token usage metrics.
    /// </summary>
    internal static void RecordTokenUsage(
        long? inputTokens,
        long? outputTokens,
        double? cost,
        string? model,
        string? sessionId)
    {
        if (!IsEnabled) return;

        var tags = new TagList
        {
            { OpenTelemetryConstants.GenAiSystem, "github-copilot" }
        };

        if (!string.IsNullOrEmpty(model))
        {
            tags.Add(OpenTelemetryConstants.GenAiRequestModel, model);
        }

        if (!string.IsNullOrEmpty(sessionId))
        {
            tags.Add(OpenTelemetryConstants.CopilotSessionId, sessionId);
        }

        if (inputTokens.HasValue)
        {
            TokensInputCounter.Add(inputTokens.Value, tags);
        }

        if (outputTokens.HasValue)
        {
            TokensOutputCounter.Add(outputTokens.Value, tags);
        }

        if (cost.HasValue)
        {
            CostCounter.Add(cost.Value, tags);
        }
    }

    /// <summary>
    /// Records a tool execution metric.
    /// </summary>
    internal static void RecordToolExecution(string toolName, bool success, string? sessionId)
    {
        if (!IsEnabled) return;

        var tags = new TagList
        {
            { OpenTelemetryConstants.GenAiToolName, toolName },
            { OpenTelemetryConstants.CopilotSuccess, success }
        };

        if (!string.IsNullOrEmpty(sessionId))
        {
            tags.Add(OpenTelemetryConstants.CopilotSessionId, sessionId);
        }

        ToolExecutionsCounter.Add(1, tags);

        if (!success)
        {
            ErrorsCounter.Add(1, tags);
        }
    }

    /// <summary>
    /// Records an error metric.
    /// </summary>
    internal static void RecordError(string errorType, string? sessionId)
    {
        if (!IsEnabled) return;

        var tags = new TagList
        {
            { "error.type", errorType }
        };

        if (!string.IsNullOrEmpty(sessionId))
        {
            tags.Add(OpenTelemetryConstants.CopilotSessionId, sessionId);
        }

        ErrorsCounter.Add(1, tags);
    }

    /// <summary>
    /// Records duration metric.
    /// </summary>
    internal static void RecordDuration(double durationMs, string operationType, string? sessionId)
    {
        if (!IsEnabled) return;

        var tags = new TagList
        {
            { OpenTelemetryConstants.GenAiOperationName, operationType }
        };

        if (!string.IsNullOrEmpty(sessionId))
        {
            tags.Add(OpenTelemetryConstants.CopilotSessionId, sessionId);
        }

        DurationHistogram.Record(durationMs, tags);
    }
}
