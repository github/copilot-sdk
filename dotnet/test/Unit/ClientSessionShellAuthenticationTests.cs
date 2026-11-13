/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Fact]
    public async Task ShellAuthentication_Create_And_Resume_Request_Prompts_Only_With_A_Handler()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        Func<ShellAuthenticationRequest, CancellationToken, Task<ShellAuthenticationResponse>> handler =
            (_, _) => Task.FromResult(ShellAuthenticationResponse.Cancel());

        await using var created = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "secret-create",
            OnShellAuthenticationRequest = handler
        });
        await using var resumed = await client.ResumeSessionAsync("secret-resume", new ResumeSessionConfig
        {
            OnShellAuthenticationRequest = handler
        });
        await using var plainCreated = await client.CreateSessionAsync(new SessionConfig { SessionId = "plain-create" });
        await using var plainResumed = await client.ResumeSessionAsync("plain-resume", new ResumeSessionConfig());

        bool? RequestShellAuthentication(string method, string sessionId)
        {
            var request = Assert.Single(server.Requests, request =>
                request.Method == method && request.Params.GetProperty("sessionId").GetString() == sessionId);
            return request.Params.TryGetProperty("requestShellAuthentication", out var flag) ? flag.GetBoolean() : null;
        }
        Assert.Equal(
            new bool?[] { true, true, null, null },
            new[]
            {
                RequestShellAuthentication("session.create", "secret-create"),
                RequestShellAuthentication("session.resume", "secret-resume"),
                RequestShellAuthentication("session.create", "plain-create"),
                RequestShellAuthentication("session.resume", "plain-resume"),
            });
    }

    [Fact]
    public async Task ShellAuthentication_Request_Returns_Only_A_Well_Formed_Answer()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        var logger = new ShellAuthenticationLogger();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url), Logger = logger });
        var failure = new InvalidOperationException("rejected HUNTER2-FAKE-0003");
        var gaveUp = new OperationCanceledException("handler gave up");
        var answers = new Queue<Func<ShellAuthenticationResponse>>(
        [
            () => ShellAuthenticationResponse.Accept("HUNTER2-FAKE-0001"),
            () => throw failure,
            () => new ShellAuthenticationResponse { Action = new ShellAuthenticationResponseAction("maybe"), Value = "unused" },
            () => ShellAuthenticationResponse.Cancel(),
            () => throw gaveUp,
        ]);
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "secret-answers",
            OnShellAuthenticationRequest = (_, _) => Task.FromResult(answers.Dequeue()())
        });

        var results = new List<string>();
        for (var i = 0; i < 5; i++)
        {
            var result = await server.SendRequestAsync("shellAuthentication.request", ShellAuthenticationRequestParams(session.SessionId, $"secret-{i}"));
            results.Add(result.GetRawText());
        }

        Assert.Equal(
            [
                """{"action":"accept","value":"HUNTER2-FAKE-0001"}""",
                """{"action":"cancel"}""",
                """{"action":"cancel"}""",
                """{"action":"cancel"}""",
                """{"action":"cancel"}""",
            ],
            results);
        // A handler's own cancellation is a failure too; only a closed prompt's is expected.
        // Only the exception's type is logged, never its message or the exception itself.
        Assert.Equal(
            new (string Message, Exception? Exception)[]
            {
                ("Shell authentication handler failed with System.InvalidOperationException. SessionId=secret-answers, RequestId=secret-1", null),
                ("Shell authentication handler failed with System.OperationCanceledException. SessionId=secret-answers, RequestId=secret-4", null),
            },
            logger.ShellAuthenticationFailures);
        Assert.DoesNotContain(logger.Entries, entry => entry.Message.Contains("HUNTER2", StringComparison.Ordinal));
    }

    [Fact]
    public async Task ShellAuthentication_Request_Passes_The_Runtime_Fields()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var requests = new List<(ShellAuthenticationRequest Request, string SessionId)>();
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "secret-fields",
            OnShellAuthenticationRequest = (request, _) =>
            {
                requests.Add((request, request.SessionId));
                return Task.FromResult(ShellAuthenticationResponse.Cancel());
            }
        });

        await server.SendRequestAsync("shellAuthentication.request", ShellAuthenticationRequestParams(session.SessionId, "secret-1"));
        var userName = ShellAuthenticationRequestParams(session.SessionId, "secret-2");
        userName["inputKind"] = "text";
        await server.SendRequestAsync("shellAuthentication.request", userName);

        Assert.Equal(2, requests.Count);
        var (secret, sessionId) = requests[0];
        Assert.Equal(session.SessionId, sessionId);
        Assert.Equal(
            ("secret-1", "ssh", "Enter passphrase for key '/k': ", ShellAuthenticationInputKind.Secret, "ssh host", "/work"),
            (secret.RequestId, secret.Program, secret.Prompt, secret.InputKind, secret.Command, secret.Cwd));
        Assert.Equal(ShellAuthenticationInputKind.Text, requests[1].Request.InputKind);
    }

    [Fact]
    public async Task ShellAuthentication_Cancel_Closes_A_Pending_Prompt()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        var logger = new ShellAuthenticationLogger();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url), Logger = logger });
        var started = new TaskCompletionSource<CancellationToken>(TaskCreationOptions.RunContinuationsAsynchronously);
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "secret-cancel",
            OnShellAuthenticationRequest = async (_, cancellationToken) =>
            {
                started.TrySetResult(cancellationToken);
                await Task.Delay(Timeout.Infinite, cancellationToken);
                return ShellAuthenticationResponse.Accept("too late");
            }
        });

        var pending = await server.SendRequestWithoutWaitingAsync("shellAuthentication.request", ShellAuthenticationRequestParams(session.SessionId, "secret-1"));
        var token = await started.Task.WaitAsync(TimeSpan.FromSeconds(5));
        await server.SendRequestAsync("shellAuthentication.cancel", new Dictionary<string, object?>
        {
            ["sessionId"] = session.SessionId,
            ["requestId"] = "secret-1"
        });

        var result = await pending.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.Equal("cancel", result.GetProperty("action").GetString());
        Assert.True(token.IsCancellationRequested);
        Assert.Empty(logger.ShellAuthenticationFailures);
    }

    [Fact]
    public async Task ShellAuthentication_Duplicate_Request_Cancels_Without_Closing_The_Open_Prompt()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var calls = 0;
        var started = new TaskCompletionSource<CancellationToken>(TaskCreationOptions.RunContinuationsAsynchronously);
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "secret-duplicate",
            OnShellAuthenticationRequest = async (_, cancellationToken) =>
            {
                Interlocked.Increment(ref calls);
                started.TrySetResult(cancellationToken);
                await Task.Delay(Timeout.Infinite, cancellationToken);
                return ShellAuthenticationResponse.Accept("too late");
            }
        });

        var pending = await server.SendRequestWithoutWaitingAsync("shellAuthentication.request", ShellAuthenticationRequestParams(session.SessionId, "secret-1"));
        var token = await started.Task.WaitAsync(TimeSpan.FromSeconds(5));
        var duplicate = await server.SendRequestAsync("shellAuthentication.request", ShellAuthenticationRequestParams(session.SessionId, "secret-1"))
            .WaitAsync(TimeSpan.FromSeconds(5));

        Assert.Equal("cancel", duplicate.GetProperty("action").GetString());
        Assert.False(token.IsCancellationRequested);
        Assert.False(pending.IsCompleted);
        Assert.Equal(1, Volatile.Read(ref calls));

        await server.SendRequestAsync("shellAuthentication.cancel", new Dictionary<string, object?>
        {
            ["sessionId"] = session.SessionId,
            ["requestId"] = "secret-1"
        });
        var result = await pending.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.Equal("cancel", result.GetProperty("action").GetString());
    }

    [Fact]
    public async Task ShellAuthentication_Cancels_A_Handler_That_Never_Finishes()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var started = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var never = new TaskCompletionSource<ShellAuthenticationResponse>();
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "secret-never",
            OnShellAuthenticationRequest = (_, _) =>
            {
                started.TrySetResult();
                return never.Task;
            }
        });

        var pending = await server.SendRequestWithoutWaitingAsync("shellAuthentication.request", ShellAuthenticationRequestParams(session.SessionId, "secret-1"));
        await started.Task.WaitAsync(TimeSpan.FromSeconds(5));
        await server.SendRequestAsync("shellAuthentication.cancel", new Dictionary<string, object?>
        {
            ["sessionId"] = session.SessionId,
            ["requestId"] = "secret-1"
        });

        var result = await pending.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.Equal("cancel", result.GetProperty("action").GetString());
    }

    [Fact]
    public async Task ShellAuthentication_Malformed_Request_Cancels_Without_Asking()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var calls = 0;
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "secret-malformed",
            OnShellAuthenticationRequest = (_, _) =>
            {
                Interlocked.Increment(ref calls);
                return Task.FromResult(ShellAuthenticationResponse.Accept("unused"));
            }
        });

        var unknownKind = ShellAuthenticationRequestParams(session.SessionId, "secret-1");
        unknownKind["inputKind"] = "password";
        var unknownSession = ShellAuthenticationRequestParams("unknown-session", "secret-2");
        var noRequestId = ShellAuthenticationRequestParams(session.SessionId, "secret-3");
        noRequestId.Remove("requestId");
        foreach (var parameters in new[] { unknownKind, unknownSession, noRequestId })
        {
            var result = await server.SendRequestAsync("shellAuthentication.request", parameters);
            Assert.Equal("cancel", result.GetProperty("action").GetString());
        }
        Assert.Equal(0, calls);
    }

    [Fact]
    public async Task ShellAuthentication_Disposing_The_Session_Or_Losing_The_Connection_Cancels_Open_Prompts()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var tokens = new List<TaskCompletionSource<CancellationToken>>
        {
            new(TaskCreationOptions.RunContinuationsAsynchronously),
            new(TaskCreationOptions.RunContinuationsAsynchronously),
        };
        Func<ShellAuthenticationRequest, CancellationToken, Task<ShellAuthenticationResponse>> handler = async (request, cancellationToken) =>
        {
            tokens[request.RequestId == "secret-1" ? 0 : 1].TrySetResult(cancellationToken);
            await Task.Delay(Timeout.Infinite, cancellationToken);
            return ShellAuthenticationResponse.Accept("too late");
        };
        var disposed = await client.CreateSessionAsync(new SessionConfig { SessionId = "secret-disposed", OnShellAuthenticationRequest = handler });
        await using var connected = await client.CreateSessionAsync(new SessionConfig { SessionId = "secret-connected", OnShellAuthenticationRequest = handler });

        _ = await server.SendRequestWithoutWaitingAsync("shellAuthentication.request", ShellAuthenticationRequestParams(disposed.SessionId, "secret-1"));
        var disposedToken = await tokens[0].Task.WaitAsync(TimeSpan.FromSeconds(5));
        await disposed.DisposeAsync();
        Assert.True(disposedToken.IsCancellationRequested);

        _ = await server.SendRequestWithoutWaitingAsync("shellAuthentication.request", ShellAuthenticationRequestParams(connected.SessionId, "secret-2"));
        var connectedToken = await tokens[1].Task.WaitAsync(TimeSpan.FromSeconds(5));
        server.CloseConnection();
        await Task.Delay(Timeout.Infinite, connectedToken).ContinueWith(_ => { }, TaskScheduler.Default).WaitAsync(TimeSpan.FromSeconds(5));
        Assert.True(connectedToken.IsCancellationRequested);
    }

    private static Dictionary<string, object?> ShellAuthenticationRequestParams(string sessionId, string requestId) => new()
    {
        ["sessionId"] = sessionId,
        ["requestId"] = requestId,
        ["program"] = "ssh",
        ["prompt"] = "Enter passphrase for key '/k': ",
        ["inputKind"] = "secret",
        ["command"] = "ssh host",
        ["cwd"] = "/work"
    };

    [Fact]
    public async Task ShellAuthentication_A_Throwing_Cancellation_Callback_Does_Not_Break_Cancel_Or_Teardown()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var started = new ConcurrentDictionary<string, TaskCompletionSource>();
        TaskCompletionSource Started(string requestId) =>
            started.GetOrAdd(requestId, _ => new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously));
        Func<ShellAuthenticationRequest, CancellationToken, Task<ShellAuthenticationResponse>> handler = async (request, cancellationToken) =>
        {
            cancellationToken.Register(() => throw new InvalidOperationException("the dialog failed to close"));
            Started(request.RequestId).TrySetResult();
            await Task.Delay(Timeout.Infinite, cancellationToken);
            return ShellAuthenticationResponse.Accept("too late");
        };
        await using var cancelled = await client.CreateSessionAsync(new SessionConfig { SessionId = "secret-cancelled", OnShellAuthenticationRequest = handler });
        var disposed = await client.CreateSessionAsync(new SessionConfig { SessionId = "secret-disposed", OnShellAuthenticationRequest = handler });
        await using var stopped = await client.CreateSessionAsync(new SessionConfig { SessionId = "secret-stopped", OnShellAuthenticationRequest = handler });

        // The runtime's cancel still succeeds and the prompt answers cancel.
        var pending = await server.SendRequestWithoutWaitingAsync("shellAuthentication.request", ShellAuthenticationRequestParams(cancelled.SessionId, "secret-1"));
        await Started("secret-1").Task.WaitAsync(TimeSpan.FromSeconds(5));
        await server.SendRequestAsync("shellAuthentication.cancel", new Dictionary<string, object?>
        {
            ["sessionId"] = cancelled.SessionId,
            ["requestId"] = "secret-1"
        });
        Assert.Equal("cancel", (await pending.WaitAsync(TimeSpan.FromSeconds(5))).GetProperty("action").GetString());

        // Disposing the session and stopping the client still complete.
        _ = await server.SendRequestWithoutWaitingAsync("shellAuthentication.request", ShellAuthenticationRequestParams(disposed.SessionId, "secret-2"));
        await Started("secret-2").Task.WaitAsync(TimeSpan.FromSeconds(5));
        await disposed.DisposeAsync().AsTask().WaitAsync(TimeSpan.FromSeconds(5));

        _ = await server.SendRequestWithoutWaitingAsync("shellAuthentication.request", ShellAuthenticationRequestParams(stopped.SessionId, "secret-3"));
        await Started("secret-3").Task.WaitAsync(TimeSpan.FromSeconds(5));
        await client.StopAsync().WaitAsync(TimeSpan.FromSeconds(5));
    }

    [Fact]
    public async Task ShellAuthentication_Request_Completion_Preserves_Queued_Cancellation_Callbacks()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var started = new TaskCompletionSource<CancellationToken>(TaskCreationOptions.RunContinuationsAsynchronously);
        var answer = new TaskCompletionSource<ShellAuthenticationResponse>(TaskCreationOptions.RunContinuationsAsynchronously);
        var cancelled = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var notificationQueued = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseNotification = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var notificationEntered = 0;
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "secret-queued-cancellation",
            OnShellAuthenticationRequest = (_, token) =>
            {
                token.Register(() => cancelled.TrySetResult());
                started.TrySetResult(token);
                return answer.Task;
            }
        });
        var pending = await server.SendRequestWithoutWaitingAsync("shellAuthentication.request", ShellAuthenticationRequestParams(session.SessionId, "secret-1"));
        var token = await started.Task.WaitAsync(TimeSpan.FromSeconds(5));

        // CancelAsync flows this context to its worker. Hold it before it reads the source's
        // registrations, without changing the thread pool or blocking the request's own context.
        var notificationContext = new AsyncLocal<bool>(change =>
        {
            if (change.ThreadContextChanged && change.CurrentValue && token.IsCancellationRequested
                && Interlocked.CompareExchange(ref notificationEntered, 1, 0) == 0)
            {
                notificationQueued.TrySetResult();
                releaseNotification.Task.GetAwaiter().GetResult();
            }
        });
        Task disposal;
        try
        {
            notificationContext.Value = true;
            disposal = session.DisposeAsync().AsTask();
        }
        finally
        {
            notificationContext.Value = false;
        }
        try
        {
            await disposal.WaitAsync(TimeSpan.FromSeconds(5));
            await notificationQueued.Task.WaitAsync(TimeSpan.FromSeconds(5));
            // The client and session can both cancel the same prompt before notifications start.
            await server.SendRequestAsync("shellAuthentication.cancel", new Dictionary<string, object?>
            {
                ["sessionId"] = session.SessionId,
                ["requestId"] = "secret-1"
            }).WaitAsync(TimeSpan.FromSeconds(5));
            answer.TrySetResult(ShellAuthenticationResponse.Accept("too late"));
            Assert.Equal("cancel", (await pending.WaitAsync(TimeSpan.FromSeconds(5))).GetProperty("action").GetString());
        }
        finally
        {
            answer.TrySetResult(ShellAuthenticationResponse.Cancel());
            releaseNotification.TrySetResult();
        }
        await cancelled.Task.WaitAsync(TimeSpan.FromSeconds(5));
    }

    [Fact]
    public async Task ShellAuthentication_A_Blocking_Cancellation_Callback_Does_Not_Block_The_Reader_Or_Teardown()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var started = new ConcurrentDictionary<string, TaskCompletionSource>();
        var blocked = new ConcurrentDictionary<string, TaskCompletionSource>();
        var releases = new ConcurrentDictionary<string, ManualResetEventSlim>();
        TaskCompletionSource Signal(ConcurrentDictionary<string, TaskCompletionSource> signals, string requestId) =>
            signals.GetOrAdd(requestId, _ => new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously));
        ManualResetEventSlim Release(string requestId) => releases.GetOrAdd(requestId, _ => new ManualResetEventSlim());
        Func<ShellAuthenticationRequest, CancellationToken, Task<ShellAuthenticationResponse>> handler = async (request, cancellationToken) =>
        {
            // An app callback that waits, for example on a call that only the connection's reader can answer.
            cancellationToken.Register(() =>
            {
                Signal(blocked, request.RequestId).TrySetResult();
                Release(request.RequestId).Wait(TimeSpan.FromSeconds(30));
            });
            Signal(started, request.RequestId).TrySetResult();
            await Task.Delay(Timeout.Infinite, cancellationToken);
            return ShellAuthenticationResponse.Accept("too late");
        };
        try
        {
            await using var cancelled = await client.CreateSessionAsync(new SessionConfig { SessionId = "secret-cancelled", OnShellAuthenticationRequest = handler });
            var disposed = await client.CreateSessionAsync(new SessionConfig { SessionId = "secret-disposed", OnShellAuthenticationRequest = handler });
            await using var stopped = await client.CreateSessionAsync(new SessionConfig { SessionId = "secret-stopped", OnShellAuthenticationRequest = handler });

            // The runtime's cancel is answered while the callback still waits, and the prompt answers cancel once it returns.
            var pending = await server.SendRequestWithoutWaitingAsync("shellAuthentication.request", ShellAuthenticationRequestParams(cancelled.SessionId, "secret-1"));
            await Signal(started, "secret-1").Task.WaitAsync(TimeSpan.FromSeconds(5));
            await server.SendRequestAsync("shellAuthentication.cancel", new Dictionary<string, object?>
            {
                ["sessionId"] = cancelled.SessionId,
                ["requestId"] = "secret-1"
            }).WaitAsync(TimeSpan.FromSeconds(5));
            await Signal(blocked, "secret-1").Task.WaitAsync(TimeSpan.FromSeconds(5));
            Release("secret-1").Set();
            Assert.Equal("cancel", (await pending.WaitAsync(TimeSpan.FromSeconds(5))).GetProperty("action").GetString());

            // Disposing the session and stopping the client complete while their callbacks wait.
            _ = await server.SendRequestWithoutWaitingAsync("shellAuthentication.request", ShellAuthenticationRequestParams(disposed.SessionId, "secret-2"));
            await Signal(started, "secret-2").Task.WaitAsync(TimeSpan.FromSeconds(5));
            await disposed.DisposeAsync().AsTask().WaitAsync(TimeSpan.FromSeconds(5));
            await Signal(blocked, "secret-2").Task.WaitAsync(TimeSpan.FromSeconds(5));

            _ = await server.SendRequestWithoutWaitingAsync("shellAuthentication.request", ShellAuthenticationRequestParams(stopped.SessionId, "secret-3"));
            await Signal(started, "secret-3").Task.WaitAsync(TimeSpan.FromSeconds(5));
            await client.StopAsync().WaitAsync(TimeSpan.FromSeconds(5));
            await Signal(blocked, "secret-3").Task.WaitAsync(TimeSpan.FromSeconds(5));
        }
        finally
        {
            foreach (var release in releases.Values)
            {
                release.Set();
            }
        }
    }

    private sealed class ShellAuthenticationLogger : ILogger
    {
        private readonly ConcurrentQueue<(string Message, Exception? Exception)> _entries = new();

        public IReadOnlyCollection<(string Message, Exception? Exception)> Entries => _entries;

        public IEnumerable<(string Message, Exception? Exception)> ShellAuthenticationFailures =>
            _entries.Where(entry => entry.Message.StartsWith("Shell authentication handler failed", StringComparison.Ordinal));

        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;

        public bool IsEnabled(LogLevel logLevel) => true;

        public void Log<TState>(LogLevel logLevel, EventId eventId, TState state, Exception? exception, Func<TState, Exception?, string> formatter) =>
            _entries.Enqueue((formatter(state, exception), exception));
    }
}
#endif
