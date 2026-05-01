/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

public class StreamingFidelityE2ETests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "streaming_fidelity", output)
{
    [Fact]
    public async Task Should_Produce_Delta_Events_When_Streaming_Is_Enabled()
    {
        var session = await CreateSessionAsync(new SessionConfig { Streaming = true });

        var events = new List<SessionEvent>();
        session.On(evt => { lock (events) { events.Add(evt); } });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Count from 1 to 5, separated by commas." });

        List<SessionEvent> snapshot;
        lock (events) { snapshot = [.. events]; }

        var types = snapshot.Select(e => e.Type).ToList();

        // Should have streaming deltas before the final message
        var deltaEvents = snapshot.OfType<AssistantMessageDeltaEvent>().ToList();
        Assert.NotEmpty(deltaEvents);

        // Deltas should have content
        foreach (var delta in deltaEvents)
        {
            Assert.False(string.IsNullOrEmpty(delta.Data.DeltaContent));
        }

        // Should still have a final assistant.message
        Assert.Contains("assistant.message", types);

        // Deltas should come before the final message
        var firstDeltaIdx = types.IndexOf("assistant.message_delta");
        var lastAssistantIdx = types.LastIndexOf("assistant.message");
        Assert.True(firstDeltaIdx < lastAssistantIdx);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Not_Produce_Deltas_When_Streaming_Is_Disabled()
    {
        var session = await CreateSessionAsync(new SessionConfig { Streaming = false });

        var events = new List<SessionEvent>();
        session.On(evt => { lock (events) { events.Add(evt); } });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say 'hello world'." });

        List<SessionEvent> snapshot;
        lock (events) { snapshot = [.. events]; }

        var deltaEvents = snapshot.OfType<AssistantMessageDeltaEvent>().ToList();

        // No deltas when streaming is off
        Assert.Empty(deltaEvents);

        // But should still have a final assistant.message
        var assistantEvents = snapshot.OfType<AssistantMessageEvent>().ToList();
        Assert.NotEmpty(assistantEvents);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Produce_Deltas_After_Session_Resume()
    {
        var session = await CreateSessionAsync(new SessionConfig { Streaming = false });
        await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 3 + 6?" });
        await session.DisposeAsync();

        // Resume using a new client
        using var newClient = Ctx.CreateClient();
        var session2 = await newClient.ResumeSessionAsync(session.SessionId,
            new ResumeSessionConfig { OnPermissionRequest = PermissionHandler.ApproveAll, Streaming = true });

        var events = new List<SessionEvent>();
        session2.On(evt => { lock (events) { events.Add(evt); } });

        var answer = await session2.SendAndWaitAsync(new MessageOptions { Prompt = "Now if you double that, what do you get?" });
        Assert.NotNull(answer);
        Assert.Contains("18", answer!.Data.Content ?? string.Empty);

        List<SessionEvent> snapshot;
        lock (events) { snapshot = [.. events]; }

        // Should have streaming deltas before the final message
        var deltaEvents = snapshot.OfType<AssistantMessageDeltaEvent>().ToList();
        Assert.NotEmpty(deltaEvents);

        // Deltas should have content
        foreach (var delta in deltaEvents)
        {
            Assert.False(string.IsNullOrEmpty(delta.Data.DeltaContent));
        }

        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Should_Emit_AssistantMessageStart_Before_Deltas_With_Matching_MessageId()
    {
        var session = await CreateSessionAsync(new SessionConfig { Streaming = true });

        var events = new List<SessionEvent>();
        session.On(evt => { lock (events) { events.Add(evt); } });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Count from 1 to 5, separated by commas." });

        List<SessionEvent> snapshot;
        lock (events) { snapshot = [.. events]; }

        var startEvents = snapshot.OfType<AssistantMessageStartEvent>().ToList();
        var deltaEvents = snapshot.OfType<AssistantMessageDeltaEvent>().ToList();
        var messageEvents = snapshot.OfType<AssistantMessageEvent>().ToList();

        Assert.NotEmpty(startEvents);
        Assert.NotEmpty(deltaEvents);
        Assert.NotEmpty(messageEvents);

        // The start event must have a non-empty messageId
        var firstStart = startEvents[0];
        Assert.False(string.IsNullOrEmpty(firstStart.Data.MessageId));

        // The first message_start should arrive before the first message_delta
        var firstStartIdx = snapshot.IndexOf(firstStart);
        var firstDeltaIdx = snapshot.IndexOf(deltaEvents[0]);
        Assert.True(firstStartIdx < firstDeltaIdx,
            $"Expected assistant.message_start ({firstStartIdx}) before first assistant.message_delta ({firstDeltaIdx})");

        // Every assistant.message_start should have a corresponding assistant.message
        // emitted later with the same messageId.
        foreach (var start in startEvents)
        {
            Assert.Contains(messageEvents, m => m.Data.MessageId == start.Data.MessageId);
        }

        await session.DisposeAsync();
    }
}
