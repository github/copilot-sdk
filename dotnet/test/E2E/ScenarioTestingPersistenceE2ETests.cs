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
    private sealed class MessagesBackendFactAttribute : FactAttribute
    {
        public MessagesBackendFactAttribute()
        {
            if (E2ETestBackendConfiguration.Current is E2ETestBackend.OpenAIResponses or E2ETestBackend.OpenAICompletions)
                Skip = "Assistant prefill is rejected only by the Anthropic Messages API";
        }
    }

    // The Messages API rejects a request that ends in an assistant message, so an
    // empty batch after the final reply must end without a model request.
    // OpenAI BYOK backends accept a trailing assistant message, so this is
    // pinned to the Messages endpoint (CAPI and the Anthropic backend). A fresh
    // replay endpoint isolates the process-wide model catalog so this snapshot's
    // /v1/messages endpoint controls routing.
    [MessagesBackendFact]
    public async Task Should_Not_Prefill_Claude_When_Empty_SendMessages_Follows_The_Final_Reply()
    {
        await using var context = await E2ETestContext.CreateAsync();
        await context.ConfigureForTestAsync("scenario_testing_persistence", replayOnly: true);
        await using var client = context.CreateClient();
        await using var session = await context.CreateSessionAsync(client, new SessionConfig
        {
            Model = "claude-sonnet-5",
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
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
        Assert.Empty(events.OfType<SessionErrorEvent>());
        Assert.Equal(
            ["EMPTY_BATCH_CONTEXT_READY"],
            events.OfType<AssistantMessageEvent>().Select(evt => evt.Data.Content ?? string.Empty));
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
