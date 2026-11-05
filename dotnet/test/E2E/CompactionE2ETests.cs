/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;
using GitHub.Copilot.Test.Harness;
using Microsoft.Extensions.Logging;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

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
        var elapsed = Stopwatch.StartNew();
        using var compactionStarted = TestHelper.ObserveEvent<SessionCompactionStartEvent>(session);
        using var compactionCompleted = TestHelper.ObserveEvent<SessionCompactionCompleteEvent>(
            session,
            evt => evt.Data.Success);

        var firstResponse = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Tell me a story about a dragon. Be detailed."
        });
        var firstResponseElapsedMs = elapsed.ElapsedMilliseconds;
        var secondResponse = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Continue the story with more details about the dragon's castle."
        });
        var secondResponseElapsedMs = elapsed.ElapsedMilliseconds;

        SessionCompactionStartEvent startEvent;
        SessionCompactionCompleteEvent completeEvent;
        try
        {
            await Task.WhenAll(compactionStarted.Task, compactionCompleted.Task).WaitAsync(CompactionTimeout);
            startEvent = await compactionStarted.Task;
            completeEvent = await compactionCompleted.Task;
        }
        catch (TimeoutException)
        {
            try
            {
                var events = await session.GetEventsAsync();
                var exchanges = await Ctx.GetExchangesAsync();
                Logger.LogWarning(
                    "SDK compaction failure: firstResponse={FirstResponse}, secondResponse={SecondResponse}, firstResponseElapsedMs={FirstElapsedMs}, secondResponseElapsedMs={SecondElapsedMs}, startDelivered={StartDelivered}, completionDelivered={CompletionDelivered}, persistedEvents={Events}, requests={Requests}",
                    firstResponse is not null,
                    secondResponse is not null,
                    firstResponseElapsedMs,
                    secondResponseElapsedMs,
                    compactionStarted.Task.Status == TaskStatus.RanToCompletion,
                    compactionCompleted.Task.Status == TaskStatus.RanToCompletion,
                    string.Join(", ", events.GroupBy(evt => evt.GetType().Name).Select(group => $"{group.Key}={group.Count()}")),
                    string.Join(", ", exchanges.Select(exchange => $"model={exchange.Request.Model}; messages={exchange.Request.Messages.Count}; compaction={exchange.CompactionUsage is not null}")));
            }
            catch (Exception diagnosticError)
            {
                Logger.LogWarning(diagnosticError, "SDK compaction failure diagnostics could not be collected");
            }
            throw;
        }

        Assert.True(startEvent.Data.ConversationTokens.GetValueOrDefault() > 0, "Expected compaction to report conversation tokens at start");
        Assert.True(completeEvent.Data.Success, "Expected compaction to succeed");
        Assert.NotNull(completeEvent.Data.CompactionTokensUsed);
        // Replay has no prompt usage; recording must preserve the provider's value or absence.
        if (Environment.GetEnvironmentVariable("GITHUB_ACTIONS") == "true"
            || E2ETestBackendConfiguration.Current != E2ETestBackend.Capi)
        {
            Assert.Equal(0, completeEvent.Data.CompactionTokensUsed!.InputTokens.GetValueOrDefault());
        }
        else
        {
            var exchanges = await Ctx.GetExchangesAsync();
            var compaction = Assert.Single(exchanges, exchange =>
                exchange.CompactionUsage is not null
                && exchange.CompactionUsage.Summary == completeEvent.Data.SummaryContent);
            Assert.Equal(compaction.CompactionUsage!.InputTokens, completeEvent.Data.CompactionTokensUsed!.InputTokens);
        }
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
        Assert.Contains("Vessaryn", content, StringComparison.OrdinalIgnoreCase);
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

        session.On<SessionEvent>(evt =>
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
