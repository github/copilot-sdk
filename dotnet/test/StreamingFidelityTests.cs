/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class StreamingFidelityTests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "streaming_fidelity", output)
{
    [Fact]
    public async Task Should_Produce_Delta_Events_When_Streaming_Is_Enabled()
    {
        var session = await CreateSessionAsync(new SessionConfig { Streaming = true });

        var events = new List<SessionEvent>();
        session.On(evt => events.Add(evt));

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Count from 1 to 5, separated by commas." });

        var types = events.Select(e => e.Type).ToList();

        // Should have streaming deltas before the final message
        var deltaEvents = events.OfType<AssistantMessageDeltaEvent>().ToList();
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
        session.On(evt => events.Add(evt));

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say 'hello world'." });

        var deltaEvents = events.OfType<AssistantMessageDeltaEvent>().ToList();

        // No deltas when streaming is off
        Assert.Empty(deltaEvents);

        // But should still have a final assistant.message
        var assistantEvents = events.OfType<AssistantMessageEvent>().ToList();
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
        session2.On(evt => events.Add(evt));

        var answer = await session2.SendAndWaitAsync(new MessageOptions { Prompt = "Now if you double that, what do you get?" });
        Assert.NotNull(answer);
        Assert.Contains("18", answer!.Data.Content ?? string.Empty);

        // Should have streaming deltas before the final message
        var deltaEvents = events.OfType<AssistantMessageDeltaEvent>().ToList();
        Assert.NotEmpty(deltaEvents);

        // Deltas should have content
        foreach (var delta in deltaEvents)
        {
            Assert.False(string.IsNullOrEmpty(delta.Data.DeltaContent));
        }

        await session2.DisposeAsync();
    }
}
