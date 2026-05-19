/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

public class ClientLifecycleE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "client_lifecycle", output)
{
    [Fact]
    public async Task Should_Receive_Session_Created_Lifecycle_Event()
    {
        var created = new TaskCompletionSource<SessionLifecycleEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var subscription = Client.On(evt =>
        {
            if (evt.Type == SessionLifecycleEventTypes.Created)
            {
                created.TrySetResult(evt);
            }
        });

        await using var session = await CreateSessionAsync();
        var evt = await created.Task.WaitAsync(TimeSpan.FromSeconds(10));

        Assert.Equal(SessionLifecycleEventTypes.Created, evt.Type);
        Assert.Equal(session.SessionId, evt.SessionId);
    }

    [Fact]
    public async Task Should_Filter_Session_Lifecycle_Events_By_Type()
    {
        var created = new TaskCompletionSource<SessionLifecycleEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var subscription = Client.On(SessionLifecycleEventTypes.Created, evt => created.TrySetResult(evt));

        await using var session = await CreateSessionAsync();
        var evt = await created.Task.WaitAsync(TimeSpan.FromSeconds(10));

        Assert.Equal(SessionLifecycleEventTypes.Created, evt.Type);
        Assert.Equal(session.SessionId, evt.SessionId);
    }

    [Fact]
    public async Task Disposing_Lifecycle_Subscription_Stops_Receiving_Events()
    {
        var count = 0;
        var created = new TaskCompletionSource<SessionLifecycleEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        var subscription = Client.On(_ => Interlocked.Increment(ref count));
        subscription.Dispose();
        using var activeSubscription = Client.On(SessionLifecycleEventTypes.Created, evt => created.TrySetResult(evt));

        await using var session = await CreateSessionAsync();
        var evt = await created.Task.WaitAsync(TimeSpan.FromSeconds(10));

        Assert.Equal(session.SessionId, evt.SessionId);
        Assert.Equal(0, Interlocked.CompareExchange(ref count, 0, 0));
    }

    [Theory]
    [InlineData(true)]   // async dispose path (DisposeAsync)
    [InlineData(false)]  // sync dispose path (Dispose)
    public async Task Dispose_Disconnects_Client_And_Disposes_Rpc_Surface(bool useAsyncDispose)
    {
        var client = Ctx.CreateClient();
        await client.StartAsync();

        Assert.Equal(ConnectionState.Connected, client.State);

        if (useAsyncDispose)
        {
            await client.DisposeAsync();
        }
        else
        {
            client.Dispose();
        }

        Assert.Equal(ConnectionState.Disconnected, client.State);
        Assert.Throws<ObjectDisposedException>(() => client.Rpc);
    }

    [Fact]
    public async Task Should_Receive_Session_Updated_Lifecycle_Event_For_Non_Ephemeral_Activity()
    {
        await using var session = await CreateSessionAsync();

        var updated = new TaskCompletionSource<SessionLifecycleEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var subscription = Client.On(SessionLifecycleEventTypes.Updated, evt =>
        {
            if (string.Equals(evt.SessionId, session.SessionId, StringComparison.Ordinal))
            {
                updated.TrySetResult(evt);
            }
        });

        // session.mode.set emits a non-ephemeral session.mode_changed event,
        // which the runtime forwards as session.updated to lifecycle subscribers.
        await session.Rpc.Mode.SetAsync(SessionMode.Plan);

        var evt = await updated.Task.WaitAsync(TimeSpan.FromSeconds(15));
        Assert.Equal(SessionLifecycleEventTypes.Updated, evt.Type);
        Assert.Equal(session.SessionId, evt.SessionId);
    }

    [Fact]
    public async Task Should_Receive_Session_Deleted_Lifecycle_Event_When_Deleted()
    {
        var session = await CreateSessionAsync();
        var sessionId = session.SessionId;

        // The runtime persists session state to disk only after the first user.message
        // (LocalSessionManager.SessionWriter gates flushing on shouldSaveSession).
        // session.delete fails with "Session file not found" otherwise, so prime
        // persistence with a real LLM round-trip first.
        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say SESSION_DELETED_OK exactly." });

        var deleted = new TaskCompletionSource<SessionLifecycleEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var subscription = Client.On(SessionLifecycleEventTypes.Deleted, evt =>
        {
            if (string.Equals(evt.SessionId, sessionId, StringComparison.Ordinal))
            {
                deleted.TrySetResult(evt);
            }
        });

        // Do NOT DisposeAsync the session before deleting: dispose sends session.destroy
        // which closes in-memory state but does not remove the disk file; calling
        // delete afterwards still succeeds, but skipping dispose keeps the test minimal.
        await Client.DeleteSessionAsync(sessionId);

        var evt = await deleted.Task.WaitAsync(TimeSpan.FromSeconds(15));
        Assert.Equal(SessionLifecycleEventTypes.Deleted, evt.Type);
        Assert.Equal(sessionId, evt.SessionId);

        await session.DisposeAsync();
    }
}
