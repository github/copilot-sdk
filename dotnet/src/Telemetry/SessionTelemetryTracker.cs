/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
using System.Diagnostics;

namespace GitHub.Copilot.SDK.Telemetry;

/// <summary>
/// Tracks active spans for a Copilot session based on session events.
/// </summary>
internal sealed class SessionTelemetryTracker : IDisposable
{
    private readonly string _sessionId;
    private readonly ConcurrentDictionary<string, Activity> _turnActivities = new();
    private readonly ConcurrentDictionary<string, Activity> _toolActivities = new();
    private readonly ConcurrentDictionary<string, Activity> _subagentActivities = new();
    private readonly ConcurrentDictionary<string, Activity> _hookActivities = new();
    private readonly object _disposeLock = new();
    private Activity? _sessionActivity;
    private string? _currentModel;
    private bool _disposed;

    public SessionTelemetryTracker(string sessionId)
    {
        _sessionId = sessionId;
    }

    /// <summary>
    /// Processes a session event and creates/completes appropriate spans.
    /// </summary>
    public void ProcessEvent(SessionEvent sessionEvent)
    {
        if (!CopilotTelemetry.IsEnabled)
        {
            return;
        }

        lock (_disposeLock)
        {
            if (_disposed)
            {
                return;
            }

            ProcessEventCore(sessionEvent);
        }
    }

    private void ProcessEventCore(SessionEvent sessionEvent)
    {
        switch (sessionEvent)
        {
            // Session lifecycle
            case SessionStartEvent startEvent:
                OnSessionStart(startEvent);
                break;
            case SessionIdleEvent:
            case SessionErrorEvent:
                OnSessionEnd(sessionEvent as SessionErrorEvent);
                break;

            // Model changes
            case SessionModelChangeEvent modelChangeEvent:
                _currentModel = modelChangeEvent.Data.NewModel;
                _sessionActivity?.SetTag(OpenTelemetryConstants.GenAiResponseModel, _currentModel);
                break;

            // Turn lifecycle
            case AssistantTurnStartEvent turnStartEvent:
                OnTurnStart(turnStartEvent);
                break;
            case AssistantTurnEndEvent turnEndEvent:
                OnTurnEnd(turnEndEvent);
                break;

            // Tool execution
            case ToolExecutionStartEvent toolStartEvent:
                OnToolExecutionStart(toolStartEvent);
                break;
            case ToolExecutionCompleteEvent toolCompleteEvent:
                OnToolExecutionComplete(toolCompleteEvent);
                break;

            // Subagent lifecycle
            case SubagentStartedEvent subagentStartEvent:
                OnSubagentStart(subagentStartEvent);
                break;
            case SubagentCompletedEvent subagentCompletedEvent:
                OnSubagentComplete(subagentCompletedEvent, success: true);
                break;
            case SubagentFailedEvent subagentFailedEvent:
                OnSubagentFailed(subagentFailedEvent);
                break;

            // Hook lifecycle
            case HookStartEvent hookStartEvent:
                OnHookStart(hookStartEvent);
                break;
            case HookEndEvent hookEndEvent:
                OnHookEnd(hookEndEvent);
                break;

            // Usage/metrics
            case AssistantUsageEvent usageEvent:
                OnUsage(usageEvent);
                break;
        }
    }

    private void OnSessionStart(SessionStartEvent startEvent)
    {
        _sessionActivity = CopilotTelemetry.StartActivity(
            OpenTelemetryConstants.SpanNameSession,
            ActivityKind.Server);

        if (_sessionActivity is null) return;

        _currentModel = startEvent.Data.SelectedModel;

        _sessionActivity.SetTag(OpenTelemetryConstants.CopilotSessionId, _sessionId);
        CopilotTelemetry.SetGenAiAttributes(_sessionActivity, _currentModel);
        _sessionActivity.SetTag(OpenTelemetryConstants.GenAiOperationName, "chat");

        if (startEvent.Data.Context != null)
        {
            _sessionActivity.SetTag("copilot.context.cwd", startEvent.Data.Context.Cwd);
            if (startEvent.Data.Context.Repository != null)
            {
                _sessionActivity.SetTag("copilot.context.repository", startEvent.Data.Context.Repository);
            }
        }
    }

    private void OnSessionEnd(SessionErrorEvent? errorEvent)
    {
        if (_sessionActivity is null) return;

        if (errorEvent != null)
        {
            _sessionActivity.SetStatus(ActivityStatusCode.Error, errorEvent.Data.Message);
            _sessionActivity.SetTag(OpenTelemetryConstants.CopilotErrorMessage, errorEvent.Data.Message);
            _sessionActivity.SetTag("error.type", errorEvent.Data.ErrorType);
            CopilotTelemetry.RecordError(errorEvent.Data.ErrorType, _sessionId);
        }
        else
        {
            _sessionActivity.SetStatus(ActivityStatusCode.Ok);
        }

        _sessionActivity.Dispose();
        _sessionActivity = null;
    }

    private void OnTurnStart(AssistantTurnStartEvent turnStartEvent)
    {
        var turnActivity = CopilotTelemetry.StartActivity(
            OpenTelemetryConstants.SpanNameTurn,
            ActivityKind.Internal);

        if (turnActivity is null) return;

        turnActivity.SetTag(OpenTelemetryConstants.CopilotSessionId, _sessionId);
        turnActivity.SetTag(OpenTelemetryConstants.CopilotTurnId, turnStartEvent.Data.TurnId);
        CopilotTelemetry.SetGenAiAttributes(turnActivity, _currentModel);

        _turnActivities[turnStartEvent.Data.TurnId] = turnActivity;
    }

    private void OnTurnEnd(AssistantTurnEndEvent turnEndEvent)
    {
        if (_turnActivities.TryRemove(turnEndEvent.Data.TurnId, out var turnActivity))
        {
            turnActivity.SetStatus(ActivityStatusCode.Ok);
            turnActivity.Dispose();
        }
    }

    private void OnToolExecutionStart(ToolExecutionStartEvent toolStartEvent)
    {
        var toolActivity = CopilotTelemetry.StartActivity(
            OpenTelemetryConstants.SpanNameToolExecution,
            ActivityKind.Internal);

        if (toolActivity is null) return;

        toolActivity.SetTag(OpenTelemetryConstants.CopilotSessionId, _sessionId);
        toolActivity.SetTag(OpenTelemetryConstants.GenAiToolName, toolStartEvent.Data.ToolName);
        toolActivity.SetTag(OpenTelemetryConstants.GenAiToolCallId, toolStartEvent.Data.ToolCallId);

        if (toolStartEvent.Data.ParentToolCallId != null)
        {
            toolActivity.SetTag("copilot.parent_tool_call_id", toolStartEvent.Data.ParentToolCallId);
        }

        _toolActivities[toolStartEvent.Data.ToolCallId] = toolActivity;
    }

    private void OnToolExecutionComplete(ToolExecutionCompleteEvent toolCompleteEvent)
    {
        if (_toolActivities.TryRemove(toolCompleteEvent.Data.ToolCallId, out var toolActivity))
        {
            toolActivity.SetTag(OpenTelemetryConstants.CopilotSuccess, toolCompleteEvent.Data.Success);

            if (toolCompleteEvent.Data.Success)
            {
                toolActivity.SetStatus(ActivityStatusCode.Ok);
            }
            else
            {
                toolActivity.SetStatus(ActivityStatusCode.Error, toolCompleteEvent.Data.Error?.Message);
                if (toolCompleteEvent.Data.Error != null)
                {
                    toolActivity.SetTag(OpenTelemetryConstants.CopilotErrorMessage, toolCompleteEvent.Data.Error.Message);
                }
            }

            // Record metric - get tool name before disposing
            var toolName = toolActivity.GetTagItem(OpenTelemetryConstants.GenAiToolName)?.ToString() ?? "unknown";
            CopilotTelemetry.RecordToolExecution(toolName, toolCompleteEvent.Data.Success, _sessionId);

            toolActivity.Dispose();
        }
    }

    private void OnSubagentStart(SubagentStartedEvent subagentStartEvent)
    {
        var subagentActivity = CopilotTelemetry.StartActivity(
            OpenTelemetryConstants.SpanNameSubagent,
            ActivityKind.Internal);

        if (subagentActivity is null) return;

        subagentActivity.SetTag(OpenTelemetryConstants.CopilotSessionId, _sessionId);
        subagentActivity.SetTag(OpenTelemetryConstants.CopilotSubagentName, subagentStartEvent.Data.AgentName);
        subagentActivity.SetTag("copilot.subagent.display_name", subagentStartEvent.Data.AgentDisplayName);
        subagentActivity.SetTag(OpenTelemetryConstants.GenAiToolCallId, subagentStartEvent.Data.ToolCallId);

        _subagentActivities[subagentStartEvent.Data.ToolCallId] = subagentActivity;
    }

    private void OnSubagentComplete(SubagentCompletedEvent subagentCompletedEvent, bool success)
    {
        if (_subagentActivities.TryRemove(subagentCompletedEvent.Data.ToolCallId, out var subagentActivity))
        {
            subagentActivity.SetTag(OpenTelemetryConstants.CopilotSuccess, success);
            subagentActivity.SetStatus(ActivityStatusCode.Ok);
            subagentActivity.Dispose();
        }
    }

    private void OnSubagentFailed(SubagentFailedEvent subagentFailedEvent)
    {
        if (_subagentActivities.TryRemove(subagentFailedEvent.Data.ToolCallId, out var subagentActivity))
        {
            subagentActivity.SetTag(OpenTelemetryConstants.CopilotSuccess, false);
            subagentActivity.SetStatus(ActivityStatusCode.Error, subagentFailedEvent.Data.Error);
            subagentActivity.SetTag(OpenTelemetryConstants.CopilotErrorMessage, subagentFailedEvent.Data.Error);
            subagentActivity.Dispose();

            CopilotTelemetry.RecordError("subagent_failed", _sessionId);
        }
    }

    private void OnHookStart(HookStartEvent hookStartEvent)
    {
        var hookActivity = CopilotTelemetry.StartActivity(
            OpenTelemetryConstants.SpanNameHook,
            ActivityKind.Internal);

        if (hookActivity is null) return;

        hookActivity.SetTag(OpenTelemetryConstants.CopilotSessionId, _sessionId);
        hookActivity.SetTag(OpenTelemetryConstants.CopilotHookType, hookStartEvent.Data.HookType);
        hookActivity.SetTag(OpenTelemetryConstants.CopilotHookInvocationId, hookStartEvent.Data.HookInvocationId);

        _hookActivities[hookStartEvent.Data.HookInvocationId] = hookActivity;
    }

    private void OnHookEnd(HookEndEvent hookEndEvent)
    {
        if (_hookActivities.TryRemove(hookEndEvent.Data.HookInvocationId, out var hookActivity))
        {
            hookActivity.SetTag(OpenTelemetryConstants.CopilotSuccess, hookEndEvent.Data.Success);

            if (hookEndEvent.Data.Success)
            {
                hookActivity.SetStatus(ActivityStatusCode.Ok);
            }
            else
            {
                hookActivity.SetStatus(ActivityStatusCode.Error, hookEndEvent.Data.Error?.Message);
                if (hookEndEvent.Data.Error != null)
                {
                    hookActivity.SetTag(OpenTelemetryConstants.CopilotErrorMessage, hookEndEvent.Data.Error.Message);
                }
            }

            hookActivity.Dispose();
        }
    }

    private void OnUsage(AssistantUsageEvent usageEvent)
    {
        var data = usageEvent.Data;

        // Create an inference span for the LLM call
        using var inferenceActivity = CopilotTelemetry.StartActivity(
            OpenTelemetryConstants.SpanNameInference,
            ActivityKind.Client);

        if (inferenceActivity != null)
        {
            inferenceActivity.SetTag(OpenTelemetryConstants.CopilotSessionId, _sessionId);
            inferenceActivity.SetTag(OpenTelemetryConstants.GenAiOperationName, "chat");

            if (data.Model != null)
            {
                inferenceActivity.SetTag(OpenTelemetryConstants.GenAiResponseModel, data.Model);
            }

            if (data.InputTokens.HasValue)
            {
                inferenceActivity.SetTag(OpenTelemetryConstants.GenAiUsageInputTokens, (long)data.InputTokens.Value);
            }

            if (data.OutputTokens.HasValue)
            {
                inferenceActivity.SetTag(OpenTelemetryConstants.GenAiUsageOutputTokens, (long)data.OutputTokens.Value);
            }

            if (data.Cost.HasValue)
            {
                inferenceActivity.SetTag(OpenTelemetryConstants.CopilotCost, data.Cost.Value);
            }

            if (data.Duration.HasValue)
            {
                inferenceActivity.SetTag(OpenTelemetryConstants.CopilotDurationMs, data.Duration.Value);
                CopilotTelemetry.RecordDuration(data.Duration.Value, "inference", _sessionId);
            }

            if (data.CacheReadTokens.HasValue)
            {
                inferenceActivity.SetTag(OpenTelemetryConstants.CopilotCacheReadTokens, (long)data.CacheReadTokens.Value);
            }

            if (data.CacheWriteTokens.HasValue)
            {
                inferenceActivity.SetTag(OpenTelemetryConstants.CopilotCacheWriteTokens, (long)data.CacheWriteTokens.Value);
            }

            inferenceActivity.SetStatus(ActivityStatusCode.Ok);
        }

        // Record metrics
        CopilotTelemetry.RecordTokenUsage(
            inputTokens: data.InputTokens.HasValue ? (long)data.InputTokens.Value : null,
            outputTokens: data.OutputTokens.HasValue ? (long)data.OutputTokens.Value : null,
            cost: data.Cost,
            model: data.Model,
            sessionId: _sessionId);
    }

    public void Dispose()
    {
        lock (_disposeLock)
        {
            if (_disposed) return;
            _disposed = true;

            // Clean up session activity
            _sessionActivity?.Dispose();
            _sessionActivity = null;

            // Dispose all orphaned activities in each dictionary
            DisposeActivities(_turnActivities);
            DisposeActivities(_toolActivities);
            DisposeActivities(_subagentActivities);
            DisposeActivities(_hookActivities);
        }
    }

    private static void DisposeActivities(ConcurrentDictionary<string, Activity> activities)
    {
        foreach (var kvp in activities)
        {
            kvp.Value.Dispose();
        }
        activities.Clear();
    }
}
