/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class RpcQueueE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_queue", output)
{
    [Fact]
    public async Task Fresh_Queue_Is_Empty_And_Empty_Mutations_Are_Noops()
    {
        await using var session = await CreateSessionAsync();

        await AssertQueueEmptyAsync(session);

        var remove = await session.Rpc.Queue.RemoveMostRecentAsync();
        Assert.False(remove.Removed);
        await AssertQueueEmptyAsync(session);

        await session.Rpc.Queue.ClearAsync();
        await AssertQueueEmptyAsync(session);

        var removeAfterClear = await session.Rpc.Queue.RemoveMostRecentAsync();
        Assert.False(removeAfterClear.Removed);
        await AssertQueueEmptyAsync(session);
    }

    [Fact]
    public async Task PendingItems_Reports_Queued_Command_And_Remove_And_Clear_Update_Queue()
    {
        await using var session = await CreateSessionAsync();
        var interest = await session.Rpc.EventLog.RegisterInterestAsync("command.queued");
        CommandQueuedEvent? firstEvent = null;
        bool respondedToFirst = false;

        try
        {
            var firstCommand = $"/sdk-queue-first-{Guid.NewGuid():N}";
            var secondCommand = $"/sdk-queue-second-{Guid.NewGuid():N}";
            var thirdCommand = $"/sdk-queue-third-{Guid.NewGuid():N}";
            var firstQueued = new TaskCompletionSource<CommandQueuedEvent>(TaskCreationOptions.RunContinuationsAsynchronously);

            using var subscription = session.On<SessionEvent>(evt =>
            {
                if (evt is CommandQueuedEvent queued &&
                    string.Equals(queued.Data.Command, firstCommand, StringComparison.Ordinal))
                {
                    firstQueued.TrySetResult(queued);
                }
            });

            var first = await session.Rpc.Commands.EnqueueAsync(firstCommand);
            Assert.True(first.Queued);

            firstEvent = await firstQueued.Task.WaitAsync(TimeSpan.FromSeconds(30));

            var second = await session.Rpc.Commands.EnqueueAsync(secondCommand);
            Assert.True(second.Queued);

            await WaitForCommandInPendingItemsAsync(session, secondCommand);

            var remove = await session.Rpc.Queue.RemoveMostRecentAsync();
            Assert.True(remove.Removed);
            await WaitForCommandNotInPendingItemsAsync(session, secondCommand);

            var third = await session.Rpc.Commands.EnqueueAsync(thirdCommand);
            Assert.True(third.Queued);

            await WaitForCommandInPendingItemsAsync(session, thirdCommand);

            await session.Rpc.Queue.ClearAsync();
            await WaitForCommandNotInPendingItemsAsync(session, thirdCommand);

            var completed = await session.Rpc.Commands.RespondToQueuedCommandAsync(
                firstEvent.Data.RequestId,
                new QueuedCommandResult
                {
                    Handled = true,
                    StopProcessingQueue = true,
                });
            respondedToFirst = completed.Success;
            Assert.True(completed.Success);
            await WaitForQueueEmptyAsync(
                session,
                "Timed out waiting for queue to empty after completing the blocked command.");
        }
        finally
        {
            if (!respondedToFirst && firstEvent is not null)
            {
                _ = await session.Rpc.Commands.RespondToQueuedCommandAsync(
                    firstEvent.Data.RequestId,
                    new QueuedCommandResult
                    {
                        Handled = true,
                        StopProcessingQueue = true,
                    });
            }

            await session.Rpc.Queue.ClearAsync();
            if (!string.IsNullOrWhiteSpace(interest.Handle))
            {
                _ = await session.Rpc.EventLog.ReleaseInterestAsync(interest.Handle);
            }
        }
    }

    private static async Task AssertQueueEmptyAsync(CopilotSession session)
    {
        var pending = await session.Rpc.Queue.PendingItemsAsync();
        Assert.Empty(pending.Items);
        Assert.Empty(pending.SteeringMessages);
    }

    private static async Task WaitForCommandInPendingItemsAsync(CopilotSession session, string command)
    {
        QueuePendingItems? item = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var pending = await session.Rpc.Queue.PendingItemsAsync();
                item = pending.Items.SingleOrDefault(i => IsPendingCommand(i, command));
                return item is not null;
            },
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: $"Timed out waiting for queued command '{command}' to appear in pending items.");

        Assert.NotNull(item);
        Assert.Equal(QueuePendingItemsKind.Command, item.Kind);
        Assert.Contains(command.TrimStart('/'), item.DisplayText, StringComparison.Ordinal);
    }

    private static async Task WaitForCommandNotInPendingItemsAsync(CopilotSession session, string command)
    {
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var pending = await session.Rpc.Queue.PendingItemsAsync();
                return !pending.Items.Any(i => IsPendingCommand(i, command));
            },
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: $"Timed out waiting for queued command '{command}' to leave pending items.");
    }

    private static async Task WaitForQueueEmptyAsync(CopilotSession session, string timeoutMessage)
    {
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var pending = await session.Rpc.Queue.PendingItemsAsync();
                return pending.Items.Count == 0 && pending.SteeringMessages.Count == 0;
            },
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: timeoutMessage);

        await AssertQueueEmptyAsync(session);
    }

    private static bool IsPendingCommand(QueuePendingItems item, string command)
    {
        return item.Kind == QueuePendingItemsKind.Command &&
            (string.Equals(item.DisplayText, command, StringComparison.Ordinal) ||
            item.DisplayText.Contains(command.TrimStart('/'), StringComparison.Ordinal));
    }
}
