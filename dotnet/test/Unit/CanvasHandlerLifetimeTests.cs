/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task CanvasClose_Awaits_Handler_Completion_And_Propagates_Errors(bool fail)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = _ => new Dictionary<string, object?>();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        var started = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var release = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            CanvasHandler = new CloseCallbackCanvasHandler(async token =>
            {
                started.SetResult();
                await release.Task.WaitAsync(token);
                if (fail)
                {
                    throw new InvalidOperationException("close handler failed");
                }
            })
        });
        var close = server.SendRequestAsync("canvas.close", CanvasCloseParams(session));
        try
        {
            await started.Task.WaitAsync(TimeSpan.FromSeconds(5));
            // The ping response fences dispatch of the earlier canvas callback.
            await client.PingAsync().WaitAsync(TimeSpan.FromSeconds(5));
            Assert.False(close.IsCompleted);
            release.SetResult();

            if (fail)
            {
                var error = await Assert.ThrowsAsync<InvalidOperationException>(() =>
                    close.WaitAsync(TimeSpan.FromSeconds(5)));
                Assert.Contains("close handler failed", error.Message);
            }
            else
            {
                await close.WaitAsync(TimeSpan.FromSeconds(5));
            }
        }
        finally
        {
            release.TrySetResult();
        }
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task CanvasClose_Cancels_Handler_When_Connection_Closes(bool disposeClient)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = _ => new Dictionary<string, object?>();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        var started = new TaskCompletionSource<CancellationToken>(TaskCreationOptions.RunContinuationsAsynchronously);
        var cancelled = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var release = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            CanvasHandler = new CloseCallbackCanvasHandler(async token =>
            {
                using var registration = token.Register(() => cancelled.TrySetResult());
                started.SetResult(token);
                await release.Task;
            })
        });
        var close = server.SendRequestAsync("canvas.close", CanvasCloseParams(session));
        _ = close.ContinueWith(
            static task => _ = task.Exception,
            CancellationToken.None,
            TaskContinuationOptions.OnlyOnFaulted | TaskContinuationOptions.ExecuteSynchronously,
            TaskScheduler.Default);
        try
        {
            var token = await started.Task.WaitAsync(TimeSpan.FromSeconds(5));
            await client.PingAsync().WaitAsync(TimeSpan.FromSeconds(5));
            Assert.False(token.IsCancellationRequested);

            if (disposeClient)
            {
                await client.DisposeAsync();
            }
            else
            {
                server.CloseConnection();
            }

            await cancelled.Task.WaitAsync(TimeSpan.FromSeconds(5));
            Assert.True(token.IsCancellationRequested);
        }
        finally
        {
            release.TrySetResult();
        }
    }

    private static Dictionary<string, object?> CanvasCloseParams(CopilotSession session) => new()
    {
        ["sessionId"] = session.SessionId,
        ["canvasId"] = "test-canvas",
        ["instanceId"] = "test-instance"
    };

    private sealed class CloseCallbackCanvasHandler(Func<CancellationToken, Task> close) : CanvasHandlerBase
    {
        public override Task<CanvasProviderOpenResult> OnOpenAsync(
            CanvasProviderOpenRequest context, CancellationToken cancellationToken) =>
            Task.FromResult(new CanvasProviderOpenResult { Status = "ready" });

        public override Task OnCloseAsync(
            CanvasProviderCloseRequest context, CancellationToken cancellationToken) =>
            close(cancellationToken);
    }
}
#endif
