/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using GitHub.Copilot.SDK.Rpc;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

/// <summary>
/// Verifies that session-scoped RPC calls emit the expected side-effect session events.
/// Most tests are pure RPC-only and need no replay snapshot, but the truncate tests
/// drive a real user.message first so the runtime persists events to disk
/// (LocalSessionManager.SessionWriter only flushes once a user.message is observed).
/// </summary>
public class RpcEventSideEffectsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_event_side_effects", output)
{
    private static readonly TimeSpan EventTimeout = TimeSpan.FromSeconds(30);

    [Fact]
    public async Task Should_Emit_Mode_Changed_Event_When_Mode_Set()
    {
        var session = await CreateSessionAsync();

        // Subscribe before invoking RPC; events may arrive after the RPC completes.
        var modeChangedTask = TestHelper.GetNextEventOfTypeAsync<SessionModeChangedEvent>(
            session,
            evt => evt.Data.NewMode == "plan" && evt.Data.PreviousMode == "interactive",
            EventTimeout,
            timeoutDescription: "session.mode_changed event for interactive→plan");

        await session.Rpc.Mode.SetAsync(SessionMode.Plan);

        var evt = await modeChangedTask;
        Assert.Equal("plan", evt.Data.NewMode);
        Assert.Equal("interactive", evt.Data.PreviousMode);
    }

    [Fact]
    public async Task Should_Emit_Plan_Changed_Event_For_Update_And_Delete()
    {
        var session = await CreateSessionAsync();

        var createTask = TestHelper.GetNextEventOfTypeAsync<SessionPlanChangedEvent>(
            session,
            evt => evt.Data.Operation == PlanChangedOperation.Create,
            EventTimeout,
            timeoutDescription: "session.plan_changed event for plan creation");

        await session.Rpc.Plan.UpdateAsync("# Test plan\n- item");

        var createEvent = await createTask;
        Assert.Equal(PlanChangedOperation.Create, createEvent.Data.Operation);

        var deleteTask = TestHelper.GetNextEventOfTypeAsync<SessionPlanChangedEvent>(
            session,
            evt => evt.Data.Operation == PlanChangedOperation.Delete,
            EventTimeout,
            timeoutDescription: "session.plan_changed event for plan deletion");

        await session.Rpc.Plan.DeleteAsync();

        var deleteEvent = await deleteTask;
        Assert.Equal(PlanChangedOperation.Delete, deleteEvent.Data.Operation);
    }

    [Fact]
    public async Task Should_Emit_Plan_Changed_Update_Operation_On_Second_Update()
    {
        var session = await CreateSessionAsync();

        // First update creates the plan.
        await session.Rpc.Plan.UpdateAsync("# initial");

        // Second update should emit operation == "update".
        var updateTask = TestHelper.GetNextEventOfTypeAsync<SessionPlanChangedEvent>(
            session,
            evt => evt.Data.Operation == PlanChangedOperation.Update,
            EventTimeout,
            timeoutDescription: "session.plan_changed event for plan update");

        await session.Rpc.Plan.UpdateAsync("# updated content");

        var updateEvent = await updateTask;
        Assert.Equal(PlanChangedOperation.Update, updateEvent.Data.Operation);
    }

    [Fact]
    public async Task Should_Emit_Workspace_File_Changed_Event_When_File_Created()
    {
        var session = await CreateSessionAsync();
        var path = $"side-effect-{Guid.NewGuid():N}.txt";

        var changedTask = TestHelper.GetNextEventOfTypeAsync<SessionWorkspaceFileChangedEvent>(
            session,
            evt => string.Equals(evt.Data.Path, path, StringComparison.Ordinal),
            EventTimeout,
            timeoutDescription: $"session.workspace_file_changed for '{path}'");

        await session.Rpc.Workspaces.CreateFileAsync(path, "hello");

        var evt = await changedTask;
        Assert.Equal(path, evt.Data.Path);
        // Operation must be one of the defined enum values; create or update are both runtime-acceptable.
        Assert.Contains(
            evt.Data.Operation,
            new[] { WorkspaceFileChangedOperation.Create, WorkspaceFileChangedOperation.Update });
    }

    [Fact]
    public async Task Should_Emit_Title_Changed_Event_When_Name_Set()
    {
        var session = await CreateSessionAsync();
        var title = $"Renamed-{Guid.NewGuid():N}";

        // session.title_changed is ephemeral; it never lands in persisted history,
        // so we must subscribe before invoking name.set.
        var titleChangedTask = TestHelper.GetNextEventOfTypeAsync<SessionTitleChangedEvent>(
            session,
            evt => string.Equals(evt.Data.Title, title, StringComparison.Ordinal),
            EventTimeout,
            timeoutDescription: "session.title_changed event after name.set");

        await session.Rpc.Name.SetAsync(title);

        var evt = await titleChangedTask;
        Assert.Equal(title, evt.Data.Title);
    }

    [Fact]
    public async Task Should_Emit_Snapshot_Rewind_Event_And_Remove_Events_On_Truncate()
    {
        var session = await CreateSessionAsync();

        // Send a real user.message; only after one is observed does the runtime
        // begin persisting buffered events to disk (LocalSessionManager.SessionWriter
        // gates flushing on shouldSaveSession, which flips on the first user.message).
        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say SNAPSHOT_REWIND_TARGET exactly." });

        var messages = await session.GetMessagesAsync();
        var userEvent = messages.OfType<UserMessageEvent>().FirstOrDefault()
            ?? throw new InvalidOperationException("Expected at least one user.message in persisted history");
        var targetEventId = userEvent.Id.ToString();

        // session.snapshot_rewind is ephemeral; must subscribe before invoking truncate.
        var rewindTask = TestHelper.GetNextEventOfTypeAsync<SessionSnapshotRewindEvent>(
            session,
            evt => string.Equals(evt.Data.UpToEventId, targetEventId, StringComparison.OrdinalIgnoreCase),
            EventTimeout,
            timeoutDescription: "session.snapshot_rewind event after truncate");

        var truncateResult = await session.Rpc.History.TruncateAsync(targetEventId);

        Assert.True(truncateResult.EventsRemoved >= 1, "Expected truncate to remove at least the targeted event");

        var rewindEvent = await rewindTask;
        Assert.Equal(targetEventId, rewindEvent.Data.UpToEventId, ignoreCase: true);
        Assert.Equal(truncateResult.EventsRemoved, (long)rewindEvent.Data.EventsRemoved);

        // Verify the truncated event is no longer in persisted history.
        var messagesAfter = await session.GetMessagesAsync();
        Assert.DoesNotContain(messagesAfter, e => e.Id == userEvent.Id);
    }

    [Fact]
    public async Task Should_Allow_Session_Use_After_Truncate()
    {
        var session = await CreateSessionAsync();

        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say SNAPSHOT_REWIND_TARGET exactly." });

        var messages = await session.GetMessagesAsync();
        var userEvent = messages.OfType<UserMessageEvent>().FirstOrDefault()
            ?? throw new InvalidOperationException("Expected at least one user.message in persisted history");

        var truncateResult = await session.Rpc.History.TruncateAsync(userEvent.Id.ToString());
        Assert.True(truncateResult.EventsRemoved >= 1);

        // After truncation the session should still respond to RPC.
        var afterMode = await session.Rpc.Mode.GetAsync();
        Assert.True(afterMode == SessionMode.Interactive || afterMode == SessionMode.Plan || afterMode == SessionMode.Autopilot);

        // Workspace surface still works.
        _ = await session.Rpc.Workspaces.GetWorkspaceAsync();
    }
}
