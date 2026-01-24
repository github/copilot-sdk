/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Runtime.InteropServices;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class CompactionTests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "compaction", output)
{
    [Fact]
    public async Task Should_Trigger_Compaction_With_Low_Threshold_And_Emit_Events()
    {
        // Create session with very low compaction thresholds to trigger compaction quickly
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            InfiniteSessions = new InfiniteSessionConfig
            {
                Enabled = true,
                // Trigger background compaction at 0.5% context usage (~1000 tokens)
                BackgroundCompactionThreshold = 0.005,
                // Block at 1% to ensure compaction runs
                BufferExhaustionThreshold = 0.01
            }
        });

        var compactionStartEvents = new List<SessionCompactionStartEvent>();
        var compactionCompleteEvents = new List<SessionCompactionCompleteEvent>();

        session.On(evt =>
        {
            if (evt is SessionCompactionStartEvent startEvt)
            {
                compactionStartEvents.Add(startEvt);
            }
            if (evt is SessionCompactionCompleteEvent completeEvt)
            {
                compactionCompleteEvents.Add(completeEvt);
            }
        });

        // Send multiple messages to fill up the context window
        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Tell me a long story about a dragon. Be very detailed."
        });
        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Continue the story with more details about the dragon's castle."
        });
        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Now describe the dragon's treasure in great detail."
        });

        // Should have triggered compaction at least once
        Assert.True(compactionStartEvents.Count >= 1, "Expected at least 1 compaction_start event");
        Assert.True(compactionCompleteEvents.Count >= 1, "Expected at least 1 compaction_complete event");

        // Compaction should have succeeded
        var lastComplete = compactionCompleteEvents[^1];
        Assert.True(lastComplete.Data.Success, "Expected compaction to succeed");

        // Should have removed some tokens
        if (lastComplete.Data.TokensRemoved.HasValue)
        {
            Assert.True(lastComplete.Data.TokensRemoved > 0, "Expected tokensRemoved > 0");
        }

        // Verify the session still works after compaction
        var answer = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "What was the story about?"
        });
        Assert.NotNull(answer);
        Assert.NotNull(answer!.Data.Content);
        // Should remember it was about a dragon (context preserved via summary)
        Assert.Contains("dragon", answer.Data.Content.ToLower());
    }

    [Fact]
    public async Task Should_Not_Emit_Compaction_Events_When_Infinite_Sessions_Disabled()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            InfiniteSessions = new InfiniteSessionConfig
            {
                Enabled = false
            }
        });

        var compactionEvents = new List<SessionEvent>();

        session.On(evt =>
        {
            if (evt is SessionCompactionStartEvent or SessionCompactionCompleteEvent)
            {
                compactionEvents.Add(evt);
            }
        });

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2+2?" });

        // Should not have any compaction events when disabled
        Assert.Empty(compactionEvents);
    }
}
