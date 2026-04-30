/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class ClientLifecycleEventsTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "client_lifecycle_events", output)
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
        var subscription = Client.On(_ => Interlocked.Increment(ref count));
        subscription.Dispose();

        _ = await CreateSessionAsync();
        await Task.Delay(200);

        Assert.Equal(0, count);
    }
}
