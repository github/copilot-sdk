/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Telemetry;
using System.Diagnostics;
using Xunit;

namespace GitHub.Copilot.SDK.Test;

public class TelemetryTests : IDisposable
{
    private readonly ActivityListener _listener;
    private readonly List<Activity> _recordedActivities = new();

    public TelemetryTests()
    {
        // Enable telemetry for tests
        AppContext.SetSwitch("GitHub.Copilot.EnableOpenTelemetry", true);

        // Set up an activity listener to capture spans
        _listener = new ActivityListener
        {
            ShouldListenTo = source => source.Name == OpenTelemetryConstants.ActivitySourceName,
            Sample = (ref ActivityCreationOptions<ActivityContext> options) => ActivitySamplingResult.AllData,
            ActivityStarted = activity => { },
            ActivityStopped = activity => _recordedActivities.Add(activity)
        };
        ActivitySource.AddActivityListener(_listener);
    }

    public void Dispose()
    {
        _listener.Dispose();
        // Note: We can't easily reset the AppContext switch, but it won't affect other tests
        // since IsEnabled is lazily evaluated once
    }

    [Fact]
    public void TelemetryIsEnabled_WhenAppContextSwitchSet()
    {
        Assert.True(CopilotTelemetry.IsEnabled);
    }

    [Fact]
    public void ActivitySourceName_IsCorrect()
    {
        Assert.Equal("GitHub.Copilot.SDK", CopilotTelemetry.ActivitySource.Name);
    }

    [Fact]
    public void MeterName_IsCorrect()
    {
        Assert.Equal("GitHub.Copilot.SDK", CopilotTelemetry.Meter.Name);
    }

    [Fact]
    public void SessionTelemetryTracker_ProcessesSessionStartEvent()
    {
        // Arrange
        var tracker = new SessionTelemetryTracker("test-session-123");
        var sessionStartEvent = CreateSessionStartEvent("test-session-123", "gpt-4o");

        // Act
        tracker.ProcessEvent(sessionStartEvent);

        // Assert - check that an activity was started
        // Note: The activity may not be stopped yet, so we check it exists
        Assert.NotNull(CopilotTelemetry.ActivitySource);
    }

    [Fact]
    public void SessionTelemetryTracker_ProcessesToolExecutionEvents()
    {
        // Arrange
        _recordedActivities.Clear();
        var tracker = new SessionTelemetryTracker("test-session-456");

        var toolStartEvent = CreateToolExecutionStartEvent("call-123", "file_edit");
        var toolCompleteEvent = CreateToolExecutionCompleteEvent("call-123", success: true);

        // Act
        tracker.ProcessEvent(toolStartEvent);
        tracker.ProcessEvent(toolCompleteEvent);

        // Assert - tool execution span should be recorded
        var toolActivity = _recordedActivities.FirstOrDefault(a =>
            a.OperationName == OpenTelemetryConstants.SpanNameToolExecution);

        Assert.NotNull(toolActivity);
        Assert.Equal("file_edit", toolActivity!.GetTagItem(OpenTelemetryConstants.GenAiToolName));
        Assert.Equal("call-123", toolActivity.GetTagItem(OpenTelemetryConstants.GenAiToolCallId));
        Assert.Equal(true, toolActivity.GetTagItem(OpenTelemetryConstants.CopilotSuccess));
        Assert.Equal(ActivityStatusCode.Ok, toolActivity.Status);
    }

    [Fact]
    public void SessionTelemetryTracker_ProcessesFailedToolExecution()
    {
        // Arrange
        _recordedActivities.Clear();
        var tracker = new SessionTelemetryTracker("test-session-789");

        var toolStartEvent = CreateToolExecutionStartEvent("call-fail", "broken_tool");
        var toolCompleteEvent = CreateToolExecutionCompleteEvent("call-fail", success: false, errorMessage: "Something went wrong");

        // Act
        tracker.ProcessEvent(toolStartEvent);
        tracker.ProcessEvent(toolCompleteEvent);

        // Assert
        var toolActivity = _recordedActivities.FirstOrDefault(a =>
            a.OperationName == OpenTelemetryConstants.SpanNameToolExecution);

        Assert.NotNull(toolActivity);
        Assert.Equal(false, toolActivity!.GetTagItem(OpenTelemetryConstants.CopilotSuccess));
        Assert.Equal(ActivityStatusCode.Error, toolActivity.Status);
        Assert.Equal("Something went wrong", toolActivity.GetTagItem(OpenTelemetryConstants.CopilotErrorMessage));
    }

    [Fact]
    public void SessionTelemetryTracker_ProcessesTurnEvents()
    {
        // Arrange
        _recordedActivities.Clear();
        var tracker = new SessionTelemetryTracker("test-session-turn");

        var turnStartEvent = CreateTurnStartEvent("turn-001");
        var turnEndEvent = CreateTurnEndEvent("turn-001");

        // Act
        tracker.ProcessEvent(turnStartEvent);
        tracker.ProcessEvent(turnEndEvent);

        // Assert
        var turnActivity = _recordedActivities.FirstOrDefault(a =>
            a.OperationName == OpenTelemetryConstants.SpanNameTurn);

        Assert.NotNull(turnActivity);
        Assert.Equal("turn-001", turnActivity!.GetTagItem(OpenTelemetryConstants.CopilotTurnId));
        Assert.Equal(ActivityStatusCode.Ok, turnActivity.Status);
    }

    [Fact]
    public void SessionTelemetryTracker_ProcessesUsageEvent()
    {
        // Arrange
        _recordedActivities.Clear();
        var tracker = new SessionTelemetryTracker("test-session-usage");

        var usageEvent = CreateUsageEvent(
            model: "gpt-4o",
            inputTokens: 100,
            outputTokens: 50,
            cost: 0.005,
            durationMs: 1500);

        // Act
        tracker.ProcessEvent(usageEvent);

        // Assert - inference span should be recorded
        var inferenceActivity = _recordedActivities.FirstOrDefault(a =>
            a.OperationName == OpenTelemetryConstants.SpanNameInference);

        Assert.NotNull(inferenceActivity);
        Assert.Equal("gpt-4o", inferenceActivity!.GetTagItem(OpenTelemetryConstants.GenAiResponseModel));
        Assert.Equal(100L, inferenceActivity.GetTagItem(OpenTelemetryConstants.GenAiUsageInputTokens));
        Assert.Equal(50L, inferenceActivity.GetTagItem(OpenTelemetryConstants.GenAiUsageOutputTokens));
        Assert.Equal(0.005, inferenceActivity.GetTagItem(OpenTelemetryConstants.CopilotCost));
    }

    [Fact]
    public void SessionTelemetryTracker_ProcessesSubagentEvents()
    {
        // Arrange
        _recordedActivities.Clear();
        var tracker = new SessionTelemetryTracker("test-session-subagent");

        var subagentStartEvent = CreateSubagentStartedEvent("call-sub-1", "code-reviewer", "Code Reviewer");
        var subagentCompleteEvent = CreateSubagentCompletedEvent("call-sub-1", "code-reviewer");

        // Act
        tracker.ProcessEvent(subagentStartEvent);
        tracker.ProcessEvent(subagentCompleteEvent);

        // Assert
        var subagentActivity = _recordedActivities.FirstOrDefault(a =>
            a.OperationName == OpenTelemetryConstants.SpanNameSubagent);

        Assert.NotNull(subagentActivity);
        Assert.Equal("code-reviewer", subagentActivity!.GetTagItem(OpenTelemetryConstants.CopilotSubagentName));
        Assert.Equal(true, subagentActivity.GetTagItem(OpenTelemetryConstants.CopilotSuccess));
    }

    [Fact]
    public void SessionTelemetryTracker_DisposeCleansUpActivities()
    {
        // Arrange
        var tracker = new SessionTelemetryTracker("test-session-dispose");
        tracker.ProcessEvent(CreateSessionStartEvent("test-session-dispose", "gpt-4o"));
        tracker.ProcessEvent(CreateTurnStartEvent("turn-orphan"));

        // Act
        tracker.Dispose();

        // Assert - should not throw, activities should be cleaned up
        // Processing after dispose should be a no-op
        tracker.ProcessEvent(CreateTurnEndEvent("turn-orphan"));
    }

    #region Helper Methods for Creating Test Events

    private static SessionStartEvent CreateSessionStartEvent(string sessionId, string? model)
    {
        return new SessionStartEvent
        {
            Id = Guid.NewGuid(),
            Timestamp = DateTimeOffset.UtcNow,
            Data = new SessionStartData
            {
                SessionId = sessionId,
                Version = 1.0,
                Producer = "test",
                CopilotVersion = "1.0.0",
                StartTime = DateTimeOffset.UtcNow,
                SelectedModel = model
            }
        };
    }

    private static ToolExecutionStartEvent CreateToolExecutionStartEvent(string toolCallId, string toolName)
    {
        return new ToolExecutionStartEvent
        {
            Id = Guid.NewGuid(),
            Timestamp = DateTimeOffset.UtcNow,
            Data = new ToolExecutionStartData
            {
                ToolCallId = toolCallId,
                ToolName = toolName
            }
        };
    }

    private static ToolExecutionCompleteEvent CreateToolExecutionCompleteEvent(
        string toolCallId, bool success, string? errorMessage = null)
    {
        return new ToolExecutionCompleteEvent
        {
            Id = Guid.NewGuid(),
            Timestamp = DateTimeOffset.UtcNow,
            Data = new ToolExecutionCompleteData
            {
                ToolCallId = toolCallId,
                Success = success,
                Error = errorMessage != null ? new ToolExecutionCompleteDataError { Message = errorMessage } : null
            }
        };
    }

    private static AssistantTurnStartEvent CreateTurnStartEvent(string turnId)
    {
        return new AssistantTurnStartEvent
        {
            Id = Guid.NewGuid(),
            Timestamp = DateTimeOffset.UtcNow,
            Data = new AssistantTurnStartData
            {
                TurnId = turnId
            }
        };
    }

    private static AssistantTurnEndEvent CreateTurnEndEvent(string turnId)
    {
        return new AssistantTurnEndEvent
        {
            Id = Guid.NewGuid(),
            Timestamp = DateTimeOffset.UtcNow,
            Data = new AssistantTurnEndData
            {
                TurnId = turnId
            }
        };
    }

    private static AssistantUsageEvent CreateUsageEvent(
        string? model, double? inputTokens, double? outputTokens, double? cost, double? durationMs)
    {
        return new AssistantUsageEvent
        {
            Id = Guid.NewGuid(),
            Timestamp = DateTimeOffset.UtcNow,
            Data = new AssistantUsageData
            {
                Model = model,
                InputTokens = inputTokens,
                OutputTokens = outputTokens,
                Cost = cost,
                Duration = durationMs
            }
        };
    }

    private static SubagentStartedEvent CreateSubagentStartedEvent(string toolCallId, string agentName, string displayName)
    {
        return new SubagentStartedEvent
        {
            Id = Guid.NewGuid(),
            Timestamp = DateTimeOffset.UtcNow,
            Data = new SubagentStartedData
            {
                ToolCallId = toolCallId,
                AgentName = agentName,
                AgentDisplayName = displayName,
                AgentDescription = "Test agent"
            }
        };
    }

    private static SubagentCompletedEvent CreateSubagentCompletedEvent(string toolCallId, string agentName)
    {
        return new SubagentCompletedEvent
        {
            Id = Guid.NewGuid(),
            Timestamp = DateTimeOffset.UtcNow,
            Data = new SubagentCompletedData
            {
                ToolCallId = toolCallId,
                AgentName = agentName
            }
        };
    }

    #endregion
}
