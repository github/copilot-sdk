/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Collections.Concurrent;
using System.Diagnostics;
using Microsoft.Extensions.AI;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task ExternalTool_Exports_Connected_Trace_Across_Async_Callback(bool fail)
    {
        using var toolSource = new ActivitySource($"tool-test-{Guid.NewGuid():N}");
        var exported = new ConcurrentQueue<Activity>();
        var handlerStopped = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        ActivityTraceId traceId = default;
        using var listener = new ActivityListener
        {
            ShouldListenTo = source => source == toolSource || source.Name == "github.copilot",
            Sample = (ref ActivityCreationOptions<ActivityContext> options) =>
                options.Source == toolSource || options.Parent.TraceId == traceId
                    ? ActivitySamplingResult.AllDataAndRecorded
                    : ActivitySamplingResult.None,
            ActivityStopped = activity =>
            {
                if (activity.Recorded && activity.TraceId == traceId)
                {
                    exported.Enqueue(activity);
                    if (activity.OperationName == "copilot.tool_handler")
                    {
                        handlerStopped.TrySetResult();
                    }
                }
            },
        };
        ActivitySource.AddActivityListener(listener);
        using var remote = toolSource.StartActivity("external_tool")!;
        traceId = remote.TraceId;
        remote.TraceStateString = "tenant=example";
        remote.Stop();

        using var ambient = new Activity("host").SetIdFormat(ActivityIdFormat.W3C).Start();
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        Activity? handler = null;
        Activity? child = null;
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(async () =>
            {
                handler = Activity.Current;
                await Task.Yield();
                Assert.Same(handler, Activity.Current);
                Assert.NotNull(handler);
                handler.AddBaggage("tool-tenant", "example");
                using (child = toolSource.StartActivity("GET", ActivityKind.Client))
                {
                    Assert.NotNull(child);
                    Assert.Equal("example", child.GetBaggageItem("tool-tenant"));
                    await Task.Yield();
                    Assert.Same(child, Activity.Current);
                }
                Assert.Same(handler, Activity.Current);
                if (fail)
                {
                    throw new InvalidOperationException("tool failure");
                }
                return "tool result";
            }, "traced_tool")],
        });

        await server.SendSessionEventAsync(session.SessionId, "external_tool.requested", new()
        {
            ["requestId"] = "traced-request",
            ["sessionId"] = session.SessionId,
            ["toolCallId"] = "traced-call",
            ["toolName"] = "traced_tool",
            ["arguments"] = new Dictionary<string, object?>(),
            ["traceparent"] = remote.Id,
            ["tracestate"] = remote.TraceStateString,
        });
        var response = await WaitForRequestAsync(server, "session.tools.handlePendingToolCall");
        Assert.Equal(fail, response.Params.TryGetProperty("error", out _));
        if (fail)
        {
            Assert.Contains("tool failure", response.Params.GetProperty("error").GetString());
        }
        else
        {
            Assert.Equal("tool result", response.Params.GetProperty("result").GetProperty("textResultForLlm").GetString());
        }

        // Check the missing exported parent before waiting for its stop notification.
        Assert.NotNull(handler);
        Assert.Equal("github.copilot", handler.Source.Name);
        await handlerStopped.Task.WaitAsync(TimeSpan.FromSeconds(10));
        Assert.NotNull(child);
        Assert.Equal(remote.SpanId, handler.ParentSpanId);
        Assert.Equal(handler.SpanId, child.ParentSpanId);
        Assert.Equal(traceId, handler.TraceId);
        Assert.Equal(traceId, child.TraceId);
        Assert.Equal(remote.TraceStateString, handler.TraceStateString);
        Assert.Equal(remote.TraceStateString, child.TraceStateString);
        Assert.Collection(exported, span => Assert.Same(remote, span), span => Assert.Same(child, span), span => Assert.Same(handler, span));
        Assert.Same(ambient, Activity.Current);
    }

    [Theory]
    [InlineData(null, ActivityTraceFlags.None)]
    [InlineData(null, ActivityTraceFlags.Recorded)]
    [InlineData(ActivitySamplingResult.None, ActivityTraceFlags.None)]
    [InlineData(ActivitySamplingResult.None, ActivityTraceFlags.Recorded)]
    [InlineData(ActivitySamplingResult.PropagationData, ActivityTraceFlags.None)]
    [InlineData(ActivitySamplingResult.PropagationData, ActivityTraceFlags.Recorded)]
    [InlineData(ActivitySamplingResult.AllData, ActivityTraceFlags.None)]
    [InlineData(ActivitySamplingResult.AllData, ActivityTraceFlags.Recorded)]
    [InlineData(ActivitySamplingResult.AllDataAndRecorded, ActivityTraceFlags.None)]
    [InlineData(ActivitySamplingResult.AllDataAndRecorded, ActivityTraceFlags.Recorded)]
    public async Task ExternalTool_Preserves_Propagation_And_Listener_Sampling(
        ActivitySamplingResult? sampling, ActivityTraceFlags flags)
    {
        var traceId = ActivityTraceId.CreateRandom();
        var spanId = ActivitySpanId.CreateRandom();
        var remote = new ActivityContext(traceId, spanId, flags, "tenant=example", isRemote: true);
        ActivityContext? sampledParent = null;
        using var listener = new ActivityListener
        {
            ShouldListenTo = source => sampling is not null && source.Name == "github.copilot",
            Sample = (ref ActivityCreationOptions<ActivityContext> options) =>
            {
                if (options.Parent.TraceId != traceId)
                {
                    return ActivitySamplingResult.None;
                }
                sampledParent = options.Parent;
                return sampling!.Value;
            },
        };
        ActivitySource.AddActivityListener(listener);
        using var ambient = new Activity("host").SetIdFormat(ActivityIdFormat.W3C).Start();
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        Activity? handler = null;
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(async () =>
            {
                handler = Activity.Current;
                await Task.Yield();
                Assert.Same(handler, Activity.Current);
                return "done";
            }, "traced_tool")],
        });

        await server.SendSessionEventAsync(session.SessionId, "external_tool.requested", new()
        {
            ["requestId"] = "sampled-request",
            ["sessionId"] = session.SessionId,
            ["toolCallId"] = "sampled-call",
            ["toolName"] = "traced_tool",
            ["arguments"] = new Dictionary<string, object?>(),
            ["traceparent"] = $"00-{traceId}-{spanId}-{(byte)flags:x2}",
            ["tracestate"] = remote.TraceState,
        });
        var response = await WaitForRequestAsync(server, "session.tools.handlePendingToolCall");
        Assert.False(response.Params.TryGetProperty("error", out _));
        Assert.NotNull(handler);
        Assert.Equal(traceId, handler.TraceId);
        Assert.Equal(spanId, handler.ParentSpanId);
        Assert.Equal(remote.TraceState, handler.TraceStateString);
        var expectedFlags = sampling switch
        {
            null or ActivitySamplingResult.None => flags,
            ActivitySamplingResult.AllDataAndRecorded => ActivityTraceFlags.Recorded,
            _ => ActivityTraceFlags.None,
        };
        Assert.Equal(expectedFlags, handler.ActivityTraceFlags);
        Assert.Equal(sampling is ActivitySamplingResult.AllData or ActivitySamplingResult.AllDataAndRecorded, handler.IsAllDataRequested);
        if (sampling is not null)
        {
            Assert.Equal(remote, sampledParent);
            Assert.True(sampledParent!.Value.IsRemote);
        }
        Assert.Same(ambient, Activity.Current);
    }
}
#endif
