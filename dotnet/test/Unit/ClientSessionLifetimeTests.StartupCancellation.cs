/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Theory]
    [InlineData("start")]
    [InlineData("ping")]
    [InlineData("create")]
    public async Task Call_Joining_Pending_Startup_Observes_Its_Own_CancellationToken(string call)
    {
        var timeout = TimeSpan.FromSeconds(5);
        await using var server = await FakeCopilotServer.StartAsync();
        var entered = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var release = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method != "connect") return;
            entered.TrySetResult();
            await release.Task.WaitAsync(token);
        };
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var starting = client.StartAsync();
        try
        {
            await entered.Task.WaitAsync(timeout);
            using var canceled = new CancellationTokenSource();
            using var live = new CancellationTokenSource();
            var joined = Join(canceled.Token);
            var waiting = Join(live.Token);
            canceled.Cancel();

            await Assert.ThrowsAnyAsync<OperationCanceledException>(() => joined.WaitAsync(timeout));

            // Canceling one joined call leaves the shared startup running for every other caller.
            Assert.False(starting.IsCompleted);
            Assert.False(waiting.IsCompleted);
            release.TrySetResult();
            await starting.WaitAsync(timeout);
            await waiting.WaitAsync(timeout);
        }
        finally
        {
            release.TrySetResult();
        }

        Task Join(CancellationToken cancellationToken) => call switch
        {
            "start" => client.StartAsync(cancellationToken),
            "ping" => client.PingAsync(cancellationToken: cancellationToken),
            _ => client.CreateSessionAsync(new SessionConfig(), cancellationToken),
        };
    }
}
#endif
