/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class RpcEventLogE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_event_log", output)
{
    private static readonly TimeSpan EventLogTimeout = TimeSpan.FromSeconds(30);
    private static readonly string[] TitleChangedEventTypes = ["session.title_changed"];

    [Fact]
    public async Task Should_Read_Persisted_Events_From_Beginning()
    {
        await using var session = await CreateSessionAsync();

        await session.Rpc.Plan.UpdateAsync("# Event log E2E plan\n- persisted event");

        EventsReadResult? read = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                read = await session.Rpc.EventLog.ReadAsync(max: 100, waitMs: TimeSpan.Zero);
                return read.Events
                    .OfType<SessionPlanChangedEvent>()
                    .Any(evt => evt.Data.Operation == PlanChangedOperation.Create && evt.Ephemeral != true);
            },
            timeout: EventLogTimeout,
            timeoutMessage: "Timed out waiting for session.eventLog.read to return the persisted session.plan_changed event.");

        Assert.NotNull(read);
        Assert.Equal(EventsCursorStatus.Ok, read.CursorStatus);
        Assert.False(string.IsNullOrWhiteSpace(read.Cursor));
        Assert.Contains(
            read.Events.OfType<SessionPlanChangedEvent>(),
            evt => evt.Data.Operation == PlanChangedOperation.Create);
    }

    [Fact]
    public async Task Should_Return_Tail_Cursor_And_Read_Empty_When_No_New_Events()
    {
        await using var session = await CreateSessionAsync();

        EventLogTailResult? tail = null;
        EventsReadResult? read = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                tail = await session.Rpc.EventLog.TailAsync();
                read = await session.Rpc.EventLog.ReadAsync(
                    cursor: tail.Cursor,
                    max: 10,
                    waitMs: TimeSpan.Zero);
                return read.CursorStatus == EventsCursorStatus.Ok && read.Events.Count == 0;
            },
            timeout: EventLogTimeout,
            timeoutMessage: "Timed out waiting for a stable event-log tail cursor with no immediately available events.");

        Assert.NotNull(tail);
        Assert.False(string.IsNullOrWhiteSpace(tail.Cursor));
        Assert.NotNull(read);
        Assert.Empty(read.Events);
        Assert.False(read.HasMore);
    }

    [Fact]
    public async Task Should_Register_And_Release_Event_Interest_Idempotently()
    {
        await using var session = await CreateSessionAsync();

        var registered = await session.Rpc.EventLog.RegisterInterestAsync("session.title_changed");
        Assert.False(string.IsNullOrWhiteSpace(registered.Handle));

        var released = await session.Rpc.EventLog.ReleaseInterestAsync(registered.Handle);
        Assert.True(released.Success);

        var releasedAgain = await session.Rpc.EventLog.ReleaseInterestAsync(registered.Handle);
        Assert.True(releasedAgain.Success);
    }

    [Fact]
    public async Task Should_LongPoll_With_Types_Filter_For_TitleChanged_Event()
    {
        await using var session = await CreateSessionAsync();

        EventsReadResult? read = null;
        string expectedTitle = string.Empty;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                expectedTitle = $"EventLogTitle-{Guid.NewGuid():N}";
                var tail = await session.Rpc.EventLog.TailAsync();
                var readTask = session.Rpc.EventLog.ReadAsync(
                    cursor: tail.Cursor,
                    max: 10,
                    waitMs: TimeSpan.FromSeconds(5),
                    types: TitleChangedEventTypes);

                await session.Rpc.Name.SetAsync(expectedTitle);
                read = await readTask;

                return read.Events
                    .OfType<SessionTitleChangedEvent>()
                    .Any(evt => string.Equals(evt.Data.Title, expectedTitle, StringComparison.Ordinal));
            },
            timeout: EventLogTimeout,
            timeoutMessage: "Timed out waiting for filtered session.eventLog.read to return session.title_changed.");

        Assert.NotNull(read);
        Assert.Equal(EventsCursorStatus.Ok, read.CursorStatus);
        Assert.All(read.Events, evt => Assert.Equal("session.title_changed", evt.Type));
        Assert.Contains(
            read.Events.OfType<SessionTitleChangedEvent>(),
            evt => string.Equals(evt.Data.Title, expectedTitle, StringComparison.Ordinal));
    }
}
