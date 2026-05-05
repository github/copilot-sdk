/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.ComponentModel;
using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Extensions.AI;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

/// <summary>
/// Verifies that <see cref="CopilotSession.AbortAsync"/> cleanly interrupts an active
/// turn — both during streaming and during tool execution — without leaving dangling
/// state or causing exceptions in the event delivery pipeline.
/// </summary>
public class AbortE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "abort", output)
{
    [Fact]
    public async Task Should_Abort_During_Active_Streaming()
    {
        var session = await CreateSessionAsync(new SessionConfig { Streaming = true });

        var firstDeltaReceived = new TaskCompletionSource<AssistantMessageDeltaEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        var allEvents = new List<SessionEvent>();

        session.On(evt =>
        {
            lock (allEvents) { allEvents.Add(evt); }
            if (evt is AssistantMessageDeltaEvent delta)
            {
                firstDeltaReceived.TrySetResult(delta);
            }
        });

        // Fire-and-forget — we'll abort before it finishes
        _ = session.SendAsync(new MessageOptions
        {
            Prompt = "Write a very long essay about the history of computing, covering every decade from the 1940s to the 2020s in great detail.",
        });

        // Wait for at least one delta to arrive (proves streaming started)
        var delta = await firstDeltaReceived.Task.WaitAsync(TimeSpan.FromSeconds(60));
        Assert.False(string.IsNullOrEmpty(delta.Data.DeltaContent));

        // Now abort mid-stream
        await session.AbortAsync();

        List<SessionEvent> snapshot;
        lock (allEvents) { snapshot = [.. allEvents]; }

        // No session.idle should have appeared (abort cancels the turn)
        // OR if idle DID appear, it should be after the abort, which is fine
        // The key contract: no exceptions were thrown, and the session is usable afterwards
        var types = snapshot.Select(e => e.Type).ToList();
        Assert.Contains("assistant.message_delta", types);

        // Session should be usable after abort — verify by listening for the
        // recovery message rather than racing against a late idle from the
        // aborted streaming turn.
        var recoveryReceived = new TaskCompletionSource<AssistantMessageEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        session.On(evt =>
        {
            if (evt is AssistantMessageEvent msg && (msg.Data.Content?.Contains("abort_recovery_ok") == true))
            {
                recoveryReceived.TrySetResult(msg);
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Say 'abort_recovery_ok'.",
        });

        var recoveryMessage = await recoveryReceived.Task.WaitAsync(TimeSpan.FromSeconds(60));
        Assert.Contains("abort_recovery_ok", recoveryMessage.Data.Content?.ToLowerInvariant() ?? string.Empty);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Abort_During_Active_Tool_Execution()
    {
        var toolStarted = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseTool = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);

        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(SlowTool, "slow_analysis")],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        // Fire-and-forget
        _ = session.SendAsync(new MessageOptions
        {
            Prompt = "Use slow_analysis with value 'test_abort'. Wait for the result.",
        });

        // Wait for the tool to start executing
        var toolValue = await toolStarted.Task.WaitAsync(TimeSpan.FromSeconds(60));
        Assert.Equal("test_abort", toolValue);

        // Abort while the tool is running
        await session.AbortAsync();

        // Release the tool so its task doesn't leak
        releaseTool.TrySetResult("RELEASED_AFTER_ABORT");

        // Session should be usable after abort — verify by listening for the right event
        var recoveryReceived = new TaskCompletionSource<AssistantMessageEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        session.On(evt =>
        {
            if (evt is AssistantMessageEvent msg && (msg.Data.Content?.Contains("tool_abort_recovery_ok") == true))
            {
                recoveryReceived.TrySetResult(msg);
            }
        });

        await session.SendAsync(new MessageOptions
        {
            Prompt = "Say 'tool_abort_recovery_ok'.",
        });

        var recoveryMessage = await recoveryReceived.Task.WaitAsync(TimeSpan.FromSeconds(60));
        Assert.Contains("tool_abort_recovery_ok", recoveryMessage.Data.Content?.ToLowerInvariant() ?? string.Empty);

        await session.DisposeAsync();

        [Description("A slow analysis tool that blocks until released")]
        async Task<string> SlowTool([Description("Value to analyze")] string value)
        {
            toolStarted.TrySetResult(value);
            return await releaseTool.Task;
        }
    }
}
