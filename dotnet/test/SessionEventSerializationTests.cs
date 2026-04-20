/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Generic;
using System.Text.Json;
using Xunit;

namespace GitHub.Copilot.SDK.Test;

public class SessionEventSerializationTests
{
    public static TheoryData<SessionEvent, string> JsonElementBackedEvents => new()
    {
        {
            new AssistantMessageEvent
            {
                Id = Guid.Parse("11111111-1111-1111-1111-111111111111"),
                Timestamp = DateTimeOffset.Parse("2026-03-15T21:26:02.642Z"),
                ParentId = Guid.Parse("22222222-2222-2222-2222-222222222222"),
                Data = new AssistantMessageData
                {
                    MessageId = "msg-1",
                    Content = "",
                    ToolRequests =
                    [
                        new AssistantMessageToolRequest
                        {
                            ToolCallId = "call-1",
                            Name = "view",
                            Arguments = ParseJsonElement("""{"path":"README.md"}"""),
                            Type = AssistantMessageToolRequestType.Function,
                        },
                    ],
                },
            },
            "assistant.message"
        },
        {
            new ToolExecutionStartEvent
            {
                Id = Guid.Parse("33333333-3333-3333-3333-333333333333"),
                Timestamp = DateTimeOffset.Parse("2026-03-15T21:26:02.642Z"),
                ParentId = Guid.Parse("44444444-4444-4444-4444-444444444444"),
                Data = new ToolExecutionStartData
                {
                    ToolCallId = "call-1",
                    ToolName = "view",
                    Arguments = ParseJsonElement("""{"path":"README.md"}"""),
                },
            },
            "tool.execution_start"
        },
        {
            new ToolExecutionCompleteEvent
            {
                Id = Guid.Parse("55555555-5555-5555-5555-555555555555"),
                Timestamp = DateTimeOffset.Parse("2026-03-15T21:26:02.642Z"),
                ParentId = Guid.Parse("66666666-6666-6666-6666-666666666666"),
                Data = new ToolExecutionCompleteData
                {
                    ToolCallId = "call-1",
                    Success = true,
                    Result = new ToolExecutionCompleteResult
                    {
                        Content = "ok",
                        DetailedContent = "ok",
                    },
                    ToolTelemetry = new Dictionary<string, object>
                    {
                        ["properties"] = ParseJsonElement("""{"command":"view"}"""),
                        ["metrics"] = ParseJsonElement("""{"resultLength":2}"""),
                    },
                },
            },
            "tool.execution_complete"
        },
        {
            new SessionShutdownEvent
            {
                Id = Guid.Parse("77777777-7777-7777-7777-777777777777"),
                Timestamp = DateTimeOffset.Parse("2026-03-15T21:26:52.987Z"),
                ParentId = Guid.Parse("88888888-8888-8888-8888-888888888888"),
                Data = new SessionShutdownData
                {
                    ShutdownType = ShutdownType.Routine,
                    TotalPremiumRequests = 1,
                    TotalApiDurationMs = 100,
                    SessionStartTime = 1773609948932,
                    CodeChanges = new ShutdownCodeChanges
                    {
                        LinesAdded = 1,
                        LinesRemoved = 0,
                        FilesModified = ["README.md"],
                    },
                    ModelMetrics = new Dictionary<string, ShutdownModelMetric>
                    {
                        ["gpt-5.4"] = new ShutdownModelMetric
                        {
                            Requests = new ShutdownModelMetricRequests { Count = 1, Cost = 1 },
                            Usage = new ShutdownModelMetricUsage
                            {
                                InputTokens = 10,
                                OutputTokens = 5,
                                CacheReadTokens = 0,
                                CacheWriteTokens = 0,
                            },
                        },
                    },
                    CurrentModel = "gpt-5.4",
                },
            },
            "session.shutdown"
        }
    };

    private static JsonElement ParseJsonElement(string json)
    {
        using var document = JsonDocument.Parse(json);
        return document.RootElement.Clone();
    }

    [Theory]
    [MemberData(nameof(JsonElementBackedEvents))]
    public void SessionEvent_ToJson_RoundTrips_JsonElementBackedPayloads(SessionEvent sessionEvent, string expectedType)
    {
        var serialized = sessionEvent.ToJson();

        using var document = JsonDocument.Parse(serialized);
        var root = document.RootElement;

        Assert.Equal(expectedType, root.GetProperty("type").GetString());

        switch (expectedType)
        {
            case "assistant.message":
                Assert.Equal(
                    "README.md",
                    root.GetProperty("data")
                        .GetProperty("toolRequests")[0]
                        .GetProperty("arguments")
                        .GetProperty("path")
                        .GetString());
                break;

            case "tool.execution_start":
                Assert.Equal(
                    "README.md",
                    root.GetProperty("data")
                        .GetProperty("arguments")
                        .GetProperty("path")
                        .GetString());
                break;

            case "tool.execution_complete":
                Assert.Equal(
                    "view",
                    root.GetProperty("data")
                        .GetProperty("toolTelemetry")
                        .GetProperty("properties")
                        .GetProperty("command")
                        .GetString());
                break;

            case "session.shutdown":
                Assert.Equal(
                    1,
                    root.GetProperty("data")
                        .GetProperty("modelMetrics")
                        .GetProperty("gpt-5.4")
                        .GetProperty("requests")
                        .GetProperty("count")
                        .GetInt32());
                break;
        }
    }
}
