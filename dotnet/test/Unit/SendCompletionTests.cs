/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task Retirement_Fails_An_Unfinished_Completion_RPC_With_No_Timeout(bool force)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        var completion = new TaskCompletionSource<object?>(TaskCreationOptions.RunContinuationsAsynchronously);
        server.SendCompletion = (_, token) => completion.Task.WaitAsync(token);
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        using var cancellation = new CancellationTokenSource();
        var waiting = session.SendAndWaitAsync("unresolved work", Timeout.InfiniteTimeSpan, cancellation.Token);
        Exception? failure = null;
        try
        {
            await WaitForRequestAsync(server, "session.sendAndWait");
            if (force)
            {
                await client.ForceStopAsync().WaitAsync(TimeSpan.FromSeconds(5));
            }
            else
            {
                await session.DisposeAsync().AsTask().WaitAsync(TimeSpan.FromSeconds(5));
            }
            await Assert.ThrowsAnyAsync<IOException>(() => waiting.WaitAsync(TimeSpan.FromSeconds(5)));
            Assert.DoesNotContain(server.Requests, request => request.Method is "session.abort" or "session.destroy");
        }
        catch (Exception error)
        {
            failure = error;
        }
        finally
        {
            cancellation.Cancel();
            completion.TrySetResult(new Dictionary<string, object?> { ["messageId"] = "retired-request", ["aborted"] = false });
            try
            {
                await waiting.WaitAsync(TimeSpan.FromSeconds(5));
            }
            catch (Exception error) when (error is IOException or OperationCanceledException)
            {
            }
            catch (Exception cleanupError)
            {
                failure = failure is null ? cleanupError : new AggregateException(failure, cleanupError);
            }
        }
        if (failure is not null)
        {
            System.Runtime.ExceptionServices.ExceptionDispatchInfo.Capture(failure).Throw();
        }
    }

    [Theory]
    [InlineData(false, false)]
    [InlineData(true, false)]
    [InlineData(false, true)]
    [InlineData(true, true)]
    public async Task SendAndWait_Returns_Only_The_Runtime_Owned_Response(bool noOutput, bool autopilot)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        var completion = new TaskCompletionSource<object?>(TaskCreationOptions.RunContinuationsAsynchronously);
        server.SendCompletion = (_, token) => completion.Task.WaitAsync(token);
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        var boundaryId = Guid.NewGuid();
        var responseId = Guid.NewGuid();
        var received = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        using var subscription = session.On<SessionEvent>(evt =>
        {
            if (evt.Id == boundaryId)
            {
                received.SetResult();
            }
        });
        var waiting = session.SendAndWaitAsync("question", TimeSpan.FromSeconds(5));
        var request = await WaitForRequestAsync(server, "session.sendAndWait");
        Assert.Equal("question", request.Params.GetProperty("prompt").GetString());
        await server.SendSessionEventAsync(session.SessionId, "assistant.message", new()
        {
            ["messageId"] = "old-message",
            ["content"] = "unrelated response",
        });
        await server.SendSessionEventAsync(session.SessionId, "session.error", new()
        {
            ["errorType"] = "diagnostic",
            ["message"] = "unrelated error",
        });
        await server.SendSessionEventAsync(session.SessionId, "session.idle", new());
        var result = new Dictionary<string, object?>
        {
            ["messageId"] = "owned-request",
            ["aborted"] = false,
            ["completionEventId"] = boundaryId.ToString(),
        };
        if (!noOutput)
        {
            result["response"] = new Dictionary<string, object?>
            {
                ["id"] = responseId.ToString(),
                ["timestamp"] = "2026-01-01T00:00:00Z",
                ["type"] = "assistant.message",
                ["data"] = new Dictionary<string, object?>
                {
                    ["messageId"] = "owned-response",
                    ["content"] = "owned response",
                },
            };
        }
        completion.SetResult(result);
        await server.SendSessionEventPayloadAsync(session.SessionId, new Dictionary<string, object?>
        {
            ["id"] = boundaryId.ToString(),
            ["timestamp"] = "2026-01-01T00:00:00Z",
            ["type"] = autopilot ? "session.autopilot_stopped" : "session.idle",
            ["data"] = autopilot
                ? new Dictionary<string, object?> { ["reason"] = "task_complete", ["requestId"] = "owned-request" }
                : new Dictionary<string, object?>(),
        });
        var response = await waiting.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.True(received.Task.IsCompletedSuccessfully);
        if (noOutput)
        {
            Assert.Null(response);
        }
        else
        {
            Assert.NotNull(response);
            Assert.Equal(responseId, response.Id);
            Assert.Equal("owned response", response.Data.Content);
        }
        Assert.DoesNotContain(server.Requests, item => item.Method == "session.send");
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task SendAndWait_Dispatches_Queued_Events_Before_Judging_A_Closed_Transport(bool boundaryQueued)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        var completion = new TaskCompletionSource<object?>(TaskCreationOptions.RunContinuationsAsynchronously);
        var closed = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.SendCompletion = (_, token) => completion.Task.WaitAsync(token);
        server.AfterResponseAsync = (request, _) =>
        {
            if (request.Method == "session.sendAndWait")
            {
                // The runtime exits right after answering, with the boundary already written.
                server.CloseOutput();
                closed.TrySetResult();
            }
            return Task.CompletedTask;
        };
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        using var release = new ManualResetEventSlim();
        var handlerEntered = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var handled = new System.Collections.Concurrent.ConcurrentQueue<string>();
        using var slowHandler = session.On<SessionEvent>(evt =>
        {
            if (evt is not (AssistantMessageEvent or SessionIdleEvent))
            {
                return;
            }
            handled.Enqueue(evt.Type);
            if (evt is AssistantMessageEvent)
            {
                handlerEntered.TrySetResult();
                // A slow handler holds every later event in the session's dispatch queue.
                release.Wait(TimeSpan.FromSeconds(10));
            }
        });
        try
        {
            var boundaryId = Guid.NewGuid();
            var responseId = Guid.NewGuid();
            var assistantMessage = new Dictionary<string, object?>
            {
                ["id"] = responseId.ToString(),
                ["timestamp"] = "2026-01-01T00:00:00Z",
                ["type"] = "assistant.message",
                ["data"] = new Dictionary<string, object?>
                {
                    ["messageId"] = "owned-response",
                    ["content"] = "owned response",
                },
            };
            var waiting = session.SendAndWaitAsync("question", TimeSpan.FromSeconds(30));
            await WaitForRequestAsync(server, "session.sendAndWait");
            await server.SendSessionEventPayloadAsync(session.SessionId, assistantMessage);
            await handlerEntered.Task.WaitAsync(TimeSpan.FromSeconds(5));
            if (boundaryQueued)
            {
                await server.SendSessionEventPayloadAsync(session.SessionId, new Dictionary<string, object?>
                {
                    ["id"] = boundaryId.ToString(),
                    ["timestamp"] = "2026-01-01T00:00:01Z",
                    ["type"] = "session.idle",
                    ["data"] = new Dictionary<string, object?>(),
                });
            }
            completion.SetResult(new Dictionary<string, object?>
            {
                ["messageId"] = "owned-request",
                ["aborted"] = false,
                ["completionEventId"] = boundaryId.ToString(),
                ["response"] = assistantMessage,
            });
            await closed.Task.WaitAsync(TimeSpan.FromSeconds(5));
            // A request sent after the close fails only once the client has read to the end of the transport.
            await Assert.ThrowsAnyAsync<IOException>(() => client.PingAsync().WaitAsync(TimeSpan.FromSeconds(5)));
            // Give a wait that treats the closed transport as final the chance to fail while the handler still blocks.
            await Task.WhenAny(waiting, Task.Delay(TimeSpan.FromSeconds(1)));
            release.Set();
            if (boundaryQueued)
            {
                var response = await waiting.WaitAsync(TimeSpan.FromSeconds(5));
                Assert.NotNull(response);
                Assert.Equal(responseId, response.Id);
                Assert.Equal(["assistant.message", "session.idle"], handled.ToArray());
            }
            else
            {
                await Assert.ThrowsAsync<IOException>(() => waiting.WaitAsync(TimeSpan.FromSeconds(5)));
                Assert.Equal(["assistant.message"], handled.ToArray());
            }
        }
        finally
        {
            release.Set();
            // Graceful cleanup cannot wait for a peer whose transport was deliberately closed.
            await client.ForceStopAsync();
        }
    }
}
#endif
