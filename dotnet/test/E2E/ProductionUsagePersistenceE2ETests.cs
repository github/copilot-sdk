/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ProductionUsagePersistenceE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ProductionUsageE2ETestBase(fixture, "production_usage_persistence", output)
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
    public async Task Should_Page_Persisted_Events_Backward_Without_Resuming()
    {
        const string firstPrompt = "Reply with exactly PERSISTED_APP_FIRST.";
        const string secondPrompt = "Reply with exactly PERSISTED_APP_SECOND.";
        var session = await CreateSessionAsync();
        var sessionId = session.SessionId;

        await session.SendAndWaitAsync(new MessageOptions { Prompt = firstPrompt });
        await session.SendAndWaitAsync(new MessageOptions { Prompt = secondPrompt });
        await Client.Rpc.Sessions.SaveAsync(sessionId);
        await session.DisposeAsync();

        var pages = new List<EventsReadResult>();
        EventsReadResult page = await Client.Rpc.Sessions.ReadPersistedEventsAsync(
            sessionId,
            max: 3,
            direction: EventsReadDirection.Backward);
        pages.Add(page);

        while (page.HasMore)
        {
            Assert.False(string.IsNullOrWhiteSpace(page.Cursor));
            page = await Client.Rpc.Sessions.ReadPersistedEventsAsync(
                sessionId,
                cursor: page.Cursor,
                max: 3);
            pages.Add(page);
        }

        Assert.All(pages, current => Assert.Equal(EventsCursorStatus.Ok, current.CursorStatus));
        var events = pages.SelectMany(current => current.Events).ToList();
        Assert.Equal(events.Count, events.Select(evt => evt.Id).Distinct().Count());

        var userMessages = events
            .OfType<UserMessageEvent>()
            .Select(evt => evt.Data.Content)
            .ToList();
        Assert.Contains(firstPrompt, userMessages);
        Assert.Contains(secondPrompt, userMessages);
        Assert.True(
            userMessages.IndexOf(secondPrompt) < userMessages.IndexOf(firstPrompt),
            "Backward pages should expose the newer user turn before the older turn.");
    }

    [Fact]
    public async Task Should_Truncate_History_And_Resend_From_Boundary()
    {
        const string firstPrompt = "Reply with exactly HISTORY_APP_FIRST.";
        const string discardedPrompt = "Reply with exactly HISTORY_APP_DISCARDED.";
        const string replacementPrompt = "Reply with exactly HISTORY_APP_REPLACEMENT.";

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
        Assert.Contains("HISTORY_APP_REPLACEMENT", replacement?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        var events = await session.GetEventsAsync();
        Assert.DoesNotContain(events.OfType<UserMessageEvent>(), evt => evt.Data.Content == discardedPrompt);
        Assert.Contains(events.OfType<UserMessageEvent>(), evt => evt.Data.Content == firstPrompt);
        Assert.Contains(events.OfType<UserMessageEvent>(), evt => evt.Data.Content == replacementPrompt);
    }

    [Fact]
    public async Task Should_List_Read_And_Diff_App_Workspace_State()
    {
        await using var session = await CreateSessionAsync();
        var workspaceFile = $"app-state-{Guid.NewGuid():N}.txt";
        const string workspaceContent = "APP_WORKSPACE_STATE";

        await session.Rpc.Workspaces.CreateFileAsync(workspaceFile, workspaceContent);

        var listed = await session.Rpc.Workspaces.ListFilesAsync();
        var read = await session.Rpc.Workspaces.ReadFileAsync(workspaceFile);
        var diff = await session.Rpc.Workspaces.DiffAsync(WorkspaceDiffMode.Session);

        Assert.Contains(workspaceFile, listed.Files);
        Assert.Equal(workspaceContent, read.Content);
        Assert.Equal(WorkspaceDiffMode.Session, diff.RequestedMode);
        Assert.True(
            diff.Mode == WorkspaceDiffMode.Session || diff.Mode == WorkspaceDiffMode.Unstaged,
            $"Unexpected effective workspace diff mode: {diff.Mode}");
        Assert.Equal(diff.Mode == WorkspaceDiffMode.Unstaged, diff.IsFallback);
        if (diff.IsFallback)
        {
            Assert.NotNull(diff.UnavailableReason);
        }
    }
}
