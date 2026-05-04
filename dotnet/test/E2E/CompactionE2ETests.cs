/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

public class CompactionE2ETests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "compaction", output)
{
    private static readonly TimeSpan CompactionTimeout = TimeSpan.FromSeconds(60);

    [Fact]
    public async Task Should_Trigger_Compaction_With_Low_Threshold_And_Emit_Events()
    {
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            InfiniteSessions = new InfiniteSessionConfig
            {
                Enabled = true,
                BackgroundCompactionThreshold = 0.005,
                BufferExhaustionThreshold = 0.01
            }
        });

        // The first prompt leaves the session below the compaction processor's minimum
        // message count. The second prompt is therefore the first deterministic point
        // at which low thresholds can trigger compaction.
        var compactionStarted = TestHelper.GetNextEventOfTypeAsync<SessionCompactionStartEvent>(
            session,
            CompactionTimeout);
        var compactionCompleted = TestHelper.GetNextEventOfTypeAsync<SessionCompactionCompleteEvent>(
            session,
            evt => evt.Data.Success,
            CompactionTimeout,
            timeoutDescription: "successful compaction completion");

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Tell me a story about a dragon. Be detailed."
        });
        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Continue the story with more details about the dragon's castle."
        });

        var startEvent = await compactionStarted;
        var completeEvent = await compactionCompleted;

        Assert.True(startEvent.Data.ConversationTokens.GetValueOrDefault() > 0, "Expected compaction to report conversation tokens at start");
        Assert.True(completeEvent.Data.Success, "Expected compaction to succeed");
        Assert.NotNull(completeEvent.Data.CompactionTokensUsed);
        Assert.True(completeEvent.Data.CompactionTokensUsed!.InputTokens.GetValueOrDefault() > 0, "Expected compaction call to consume input tokens");
        Assert.Contains("<overview>", completeEvent.Data.SummaryContent ?? string.Empty, StringComparison.OrdinalIgnoreCase);
        Assert.Contains("<history>", completeEvent.Data.SummaryContent ?? string.Empty, StringComparison.OrdinalIgnoreCase);
        Assert.Contains("<checkpoint_title>", completeEvent.Data.SummaryContent ?? string.Empty, StringComparison.OrdinalIgnoreCase);

        await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Now describe the dragon's treasure in great detail."
        });

        var answer = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "What was the story about?"
        });

        var content = answer?.Data.Content ?? string.Empty;
        Assert.Contains("Kaedrith", content, StringComparison.OrdinalIgnoreCase);
        Assert.Contains("dragon", content, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Should_Not_Emit_Compaction_Events_When_Infinite_Sessions_Disabled()
    {
        await using var session = await CreateSessionAsync(new SessionConfig
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
