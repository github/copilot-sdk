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
}
#endif
