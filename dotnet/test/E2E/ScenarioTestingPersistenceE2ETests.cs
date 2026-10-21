/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ScenarioTestingPersistenceE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_persistence", output)
{
    [Fact]
    public async Task Should_Retry_From_Existing_History_With_Empty_SendMessages()
    {
        await using var session = await CreateSessionAsync();
        var initial = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with exactly EMPTY_BATCH_CONTEXT_READY.",
        });
        Assert.Contains("EMPTY_BATCH_CONTEXT_READY", initial?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        var retry = await session.Rpc.SendMessagesAsync([], wait: true);

        Assert.Empty(retry.MessageIds);
        var events = await session.GetEventsAsync();
        Assert.Single(
            events.OfType<UserMessageEvent>(),
            evt => evt.Data.Content == "Reply with exactly EMPTY_BATCH_CONTEXT_READY.");
        Assert.Contains(
            events.OfType<AssistantMessageEvent>(),
            evt => (evt.Data.Content ?? string.Empty).Contains("EMPTY_BATCH_RETRY_DONE", StringComparison.Ordinal));
    }

    [Fact]
    public async Task Should_Truncate_History_And_Resend_From_Boundary()
    {
        const string firstPrompt = "Reply with exactly HISTORY_SCENARIO_FIRST.";
        const string discardedPrompt = "Reply with exactly HISTORY_SCENARIO_DISCARDED.";
        const string replacementPrompt = "Reply with exactly HISTORY_SCENARIO_REPLACEMENT.";

        await using var session = await CreateSessionAsync();
        await session.SendAndWaitAsync(new MessageOptions { Prompt = firstPrompt });
        await session.SendAndWaitAsync(new MessageOptions { Prompt = discardedPrompt });

        var discardedEvent = (await session.GetEventsAsync())
            .OfType<UserMessageEvent>()
            .Single(evt => evt.Data.Content == discardedPrompt);
        var truncate = await session.Rpc.History.TruncateAsync(discardedEvent.Id.ToString());

        Assert.True(truncate.EventsRemoved > 0);
        Assert.NotEqual(true, truncate.CheckpointCleanupFailed);

        var replacement = await session.SendAndWaitAsync(new MessageOptions { Prompt = replacementPrompt });
        Assert.Contains("HISTORY_SCENARIO_REPLACEMENT", replacement?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        var events = await session.GetEventsAsync();
        Assert.DoesNotContain(events.OfType<UserMessageEvent>(), evt => evt.Data.Content == discardedPrompt);
        Assert.Contains(events.OfType<UserMessageEvent>(), evt => evt.Data.Content == firstPrompt);
        Assert.Contains(events.OfType<UserMessageEvent>(), evt => evt.Data.Content == replacementPrompt);
    }

}
