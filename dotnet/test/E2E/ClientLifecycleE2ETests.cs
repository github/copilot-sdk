/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

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

        var session = await CreateSessionAsync();
        var evt = await created.Task.WaitAsync(TimeSpan.FromSeconds(10));

        Assert.Equal(SessionLifecycleEventTypes.Created, evt.Type);
        Assert.Equal(session.SessionId, evt.SessionId);
    }

    [Fact]
    public async Task Should_Filter_Session_Lifecycle_Events_By_Type()
    {
        var created = new TaskCompletionSource<SessionLifecycleEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var subscription = Client.On(SessionLifecycleEventTypes.Created, evt => created.TrySetResult(evt));

        var session = await CreateSessionAsync();
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

        var session = await CreateSessionAsync();
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
}
