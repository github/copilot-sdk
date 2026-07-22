/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Net;
using System.Net.Sockets;
using System.Diagnostics;
using System.Reflection;
using System.Runtime.CompilerServices;
using System.Text;
using System.Text.Json;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class ClientSessionLifetimeTests
{
    private sealed record RpcRequestRecord(string Method, JsonElement Params);

    [Fact]
    public async Task StopAsync_Requests_Runtime_Shutdown_For_Owned_Process()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();
        using var process = StartExitedProcess();
        await ReplaceConnectionCliProcessAsync(client, process);

        await client.StopAsync();

        Assert.Equal(1, server.RuntimeShutdownCount);
    }

    [Fact]
    public async Task DisposeAsync_Requests_Runtime_Shutdown_For_Owned_Process()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();
        using var process = StartExitedProcess();
        await ReplaceConnectionCliProcessAsync(client, process);

        await client.DisposeAsync();

        Assert.Equal(1, server.RuntimeShutdownCount);
    }

    [Fact]
    public async Task StopAsync_Does_Not_Throw_When_Runtime_Shutdown_Fails()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.FailRuntimeShutdown();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();
        using var process = StartExitedProcess();
        await ReplaceConnectionCliProcessAsync(client, process);

        await client.StopAsync();

        Assert.Equal(1, server.RuntimeShutdownCount);
    }

    [Fact]
    public async Task ForceStopAsync_And_External_Stop_Do_Not_Request_Runtime_Shutdown()
    {
        await using var forceServer = await FakeCopilotServer.StartAsync();
        await using var forceClient = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(forceServer.Url) });
        await forceClient.StartAsync();
        using var process = StartExitedProcess();
        await ReplaceConnectionCliProcessAsync(forceClient, process);

        await forceClient.ForceStopAsync();

        Assert.Equal(0, forceServer.RuntimeShutdownCount);

        await using var externalServer = await FakeCopilotServer.StartAsync();
        await using var externalClient = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(externalServer.Url) });
        await externalClient.StartAsync();

        await externalClient.StopAsync();

        Assert.Equal(0, externalServer.RuntimeShutdownCount);
    }

    [Fact]
    public async Task Dropped_Session_Remains_Rooted_By_Client()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        var weakSession = await CreateDroppedSessionAsync(client);

        ForceCollect();

        Assert.True(
            weakSession.TryGetTarget(out _),
            "CopilotClient should root created sessions until they are explicitly disposed or the client stops.");
        AssertSessionCount(client, sessions: 1);
        GC.KeepAlive(client);
    }

    [Fact]
    public async Task Disposed_Session_Is_Removed_From_Client()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        AssertSessionCount(client, sessions: 1);

        await session.DisposeAsync();

        AssertSessionCount(client, sessions: 0);
    }

    [Fact]
    public async Task Disposing_Session_Remains_Rooted_Until_Destroy_Completes()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.DelayDestroy();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        AssertSessionCount(client, sessions: 1);

        var disposeTask = session.DisposeAsync().AsTask();
        await server.DestroyStarted;

        AssertSessionCount(client, sessions: 1);

        server.CompleteDestroy();
        await disposeTask;

        AssertSessionCount(client, sessions: 0);
    }

    [Fact]
    public async Task StopAsync_Removes_Rooted_Sessions()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        _ = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        AssertSessionCount(client, sessions: 1);

        await client.StopAsync();

        AssertSessionCount(client, sessions: 0);
    }

    [Fact]
    public async Task StopAsync_Keeps_Session_Rooted_Until_Destroy_Completes()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.DelayDestroy();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        _ = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        AssertSessionCount(client, sessions: 1);

        var stopTask = client.StopAsync();
        await server.DestroyStarted;

        AssertSessionCount(client, sessions: 1);

        server.CompleteDestroy();
        await stopTask;

        AssertSessionCount(client, sessions: 0);
    }

    [Fact]
    public async Task ResumeSessionAsync_Throws_When_Same_Client_Already_Tracks_Session()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        var sessionId = "same-session-id";
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = sessionId,
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        AssertSessionCount(client, sessions: 1);

        var exception = await Assert.ThrowsAsync<InvalidOperationException>(() => client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        }));
        Assert.Contains(sessionId, exception.Message);
        AssertSessionCount(client, sessions: 1);
    }

    [Fact]
    public async Task CreateSessionAsync_Serializes_CustomAgent_ReasoningEffort()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            CustomAgents =
            [
                new CustomAgentConfig
                {
                    Name = "reasoning-agent",
                    Prompt = "Think carefully.",
                    ReasoningEffort = "high"
                }
            ],
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var request = Assert.Single(server.Requests, request => request.Method == "session.create");
        var agent = Assert.Single(request.Params.GetProperty("customAgents").EnumerateArray());
        Assert.Equal("high", agent.GetProperty("reasoningEffort").GetString());
    }

    [Fact]
    public async Task CreateSessionAsync_Omits_CustomAgent_ReasoningEffort_When_Unset()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            CustomAgents =
            [
                new CustomAgentConfig
                {
                    Name = "default-agent",
                    Prompt = "Use runtime defaults."
                }
            ],
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var request = Assert.Single(server.Requests, request => request.Method == "session.create");
        var agent = Assert.Single(request.Params.GetProperty("customAgents").EnumerateArray());
        Assert.False(agent.TryGetProperty("reasoningEffort", out _));
    }

    [Fact]
    public async Task CreateSessionAsync_Registers_McpAuth_Interest_Only_When_Handler_Configured()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        await using var withoutAuth = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnEvent = _ => { }
        });

        Assert.DoesNotContain(server.Requests, request =>
            request.Method == "session.eventLog.registerInterest"
            && request.Params.GetProperty("eventType").GetString() == "mcp.oauth_required");
        Assert.Contains(server.Requests, request =>
            request.Method == "session.create"
            && request.Params.GetProperty("requestPermission").GetBoolean());

        server.ClearRequests();

        await using var withAuth = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnMcpAuthRequest = _ => Task.FromResult<McpAuthResult?>(McpAuthResult.Cancel())
        });

        Assert.Collection(
            server.Requests.Take(2),
            request => Assert.Equal("session.create", request.Method),
            request =>
            {
                Assert.Equal("session.eventLog.registerInterest", request.Method);
                Assert.Equal("mcp.oauth_required", request.Params.GetProperty("eventType").GetString());
            });
    }

    [Fact]
    public async Task CreateSessionAsync_Registers_McpAuth_Interest_After_Cloud_Create_When_Handler_Configured()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var cloud = new CloudSessionOptions
        {
            Repository = new CloudSessionRepository
            {
                Owner = "github",
                Name = "copilot-sdk",
                Branch = "main"
            }
        };

        await using var withoutAuth = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            Cloud = cloud
        });

        Assert.DoesNotContain(server.Requests, request =>
            request.Method == "session.eventLog.registerInterest"
            && request.Params.GetProperty("eventType").GetString() == "mcp.oauth_required");

        server.ClearRequests();

        await using var withAuth = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnMcpAuthRequest = _ => Task.FromResult<McpAuthResult?>(McpAuthResult.Cancel()),
            Cloud = cloud
        });

        Assert.Collection(
            server.Requests.Take(2),
            request => Assert.Equal("session.create", request.Method),
            request =>
            {
                Assert.Equal("session.eventLog.registerInterest", request.Method);
                Assert.Equal("mcp.oauth_required", request.Params.GetProperty("eventType").GetString());
            });
    }

    [Fact]
    public async Task ResumeSessionAsync_Registers_McpAuth_Interest_Only_When_Handler_Configured()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        await using var withoutAuth = await client.ResumeSessionAsync("session-without-auth", new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnEvent = _ => { }
        });

        Assert.DoesNotContain(server.Requests, request =>
            request.Method == "session.eventLog.registerInterest"
            && request.Params.GetProperty("eventType").GetString() == "mcp.oauth_required");
        Assert.Contains(server.Requests, request =>
            request.Method == "session.resume"
            && request.Params.GetProperty("requestPermission").GetBoolean());

        server.ClearRequests();

        await using var withAuth = await client.ResumeSessionAsync("session-with-auth", new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnMcpAuthRequest = _ => Task.FromResult<McpAuthResult?>(McpAuthResult.Cancel())
        });

        Assert.Collection(
            server.Requests.Take(2),
            request => Assert.Equal("session.resume", request.Method),
            request =>
            {
                Assert.Equal("session.eventLog.registerInterest", request.Method);
                Assert.Equal("mcp.oauth_required", request.Params.GetProperty("eventType").GetString());
            });
    }

    [Fact]
    public async Task McpAuth_Handler_Exception_Cancels_Pending_Request()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnMcpAuthRequest = _ => throw new ApplicationException("boom")
        });

        DispatchEvent(session, new McpOauthRequiredEvent
        {
            Data = new McpOauthRequiredData
            {
                RequestId = "mcp-auth-request-1",
                ServerName = "oauth-mcp",
                ServerUrl = "http://localhost/mcp",
                Reason = McpOauthRequestReason.Initial
            }
        });

        var request = await WaitForRequestAsync(server, "session.mcp.oauth.handlePendingRequest");
        Assert.Equal("mcp-auth-request-1", request.Params.GetProperty("requestId").GetString());
        Assert.Equal("cancelled", request.Params.GetProperty("result").GetProperty("kind").GetString());
    }

    [Fact]
    public async Task Generated_Session_Rpc_Throws_When_Session_Disposed()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        await session.DisposeAsync();

        await Assert.ThrowsAsync<ObjectDisposedException>(() => session.Rpc.Model.GetCurrentAsync());
    }

    [Fact]
    public async Task SendAndWaitAsync_Completes_When_SessionIdle_Notification_Is_Dropped()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ConfigureDroppedIdleCompletion();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var response = await session.SendAndWaitAsync(
            new MessageOptions { Prompt = "complete without idle" },
            timeout: TimeSpan.FromSeconds(2));

        Assert.NotNull(response);
        Assert.Equal("completed response", response.Data.Content);
        Assert.Contains(server.Requests, request => request.Method == "session.metadata.activity");
    }

    [Fact]
    public async Task SendAndWaitAsync_Preserves_Event_Completion_When_Legacy_Runtime_Lacks_Fallback_Rpcs()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ConfigureLegacyRuntimeCompletion();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var response = await session.SendAndWaitAsync(
            new MessageOptions { Prompt = "complete from delayed legacy idle" },
            timeout: TimeSpan.FromSeconds(3));

        Assert.NotNull(response);
        Assert.Equal("completed response", response.Data.Content);
        Assert.Contains(server.Requests, request => request.Method == "session.tasks.waitForPending");
    }

    [Fact]
    public async Task SendAndWaitAsync_DroppedIdle_Fallback_Flushes_Preceding_Event_Handlers()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ConfigureDroppedIdleCompletion(delayEvents: true);
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var completionTask = session.SendAndWaitAsync(
            new MessageOptions { Prompt = "flush handlers before completion" },
            timeout: TimeSpan.FromSeconds(5));

        var handlerStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseHandler = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        using var subscription = session.On<AssistantMessageEvent>(_ =>
        {
            handlerStarted.TrySetResult();
            releaseHandler.Task.GetAwaiter().GetResult();
        });

        server.ReleaseCompletionEvents();
        await handlerStarted.Task.WaitAsync(TimeSpan.FromSeconds(2));
        await Task.Delay(200);

        Assert.False(completionTask.IsCompleted, "Completion must wait for preceding FIFO event handlers to finish.");

        releaseHandler.TrySetResult();
        var response = await completionTask;
        Assert.Equal("completed response", response?.Data.Content);
    }

    [Fact]
    public async Task SendAndWaitAsync_DroppedIdle_Fallback_Rechecks_Activity_After_Final_Barrier()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ConfigureActivityReactivationDuringFinalBarrier();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var barrierHandlerStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseBarrierHandler = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        using var subscription = session.On<AssistantTurnEndEvent>(evt =>
        {
            if (evt.Data.TurnId == "activity-reactivation-barrier")
            {
                barrierHandlerStarted.TrySetResult();
                releaseBarrierHandler.Task.GetAwaiter().GetResult();
            }
        });

        var completionTask = session.SendAndWaitAsync(
            new MessageOptions { Prompt = "recheck activity after final barrier" },
            timeout: TimeSpan.FromSeconds(5));

        try
        {
            await barrierHandlerStarted.Task.WaitAsync(TimeSpan.FromSeconds(2));
            await server.ActivityReactivated.WaitAsync(TimeSpan.FromSeconds(2));
            Assert.False(completionTask.IsCompleted);

            releaseBarrierHandler.TrySetResult();
            await server.ReactivatedActivityObserved.WaitAsync(TimeSpan.FromSeconds(2));
            Assert.False(completionTask.IsCompleted, "Completion must not use stale idle activity after the barrier.");

            server.CompleteReactivatedActivity();
            var response = await completionTask;
            Assert.Equal("completed response", response?.Data.Content);
        }
        finally
        {
            releaseBarrierHandler.TrySetResult();
            server.CompleteReactivatedActivity();
        }
    }

    [MethodImpl(MethodImplOptions.NoInlining)]
    private static async Task<WeakReference<CopilotSession>> CreateDroppedSessionAsync(CopilotClient client)
    {
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        return new WeakReference<CopilotSession>(session);
    }

    private static void ForceCollect()
    {
        GC.Collect();
        GC.WaitForPendingFinalizers();
        GC.Collect();
    }

    private static void AssertSessionCount(CopilotClient client, int sessions)
    {
        Assert.Equal(sessions, GetPrivateDictionaryCount(client, "_sessions"));
    }

    private static int GetPrivateDictionaryCount(CopilotClient client, string fieldName)
    {
        var field = typeof(CopilotClient).GetField(fieldName, BindingFlags.Instance | BindingFlags.NonPublic)
            ?? throw new InvalidOperationException($"Field '{fieldName}' was not found.");
        var dictionary = field.GetValue(client)
            ?? throw new InvalidOperationException($"Field '{fieldName}' was null.");
        var count = dictionary.GetType().GetProperty("Count")
            ?? throw new InvalidOperationException($"Field '{fieldName}' does not expose Count.");

        return (int)count.GetValue(dictionary)!;
    }

    private static void DispatchEvent(CopilotSession session, SessionEvent evt)
    {
        var method = typeof(CopilotSession).GetMethod("DispatchEvent", BindingFlags.Instance | BindingFlags.NonPublic)
            ?? throw new InvalidOperationException("DispatchEvent method was not found.");
        method.Invoke(session, [evt]);
    }

    private static async Task<RpcRequestRecord> WaitForRequestAsync(FakeCopilotServer server, string method)
    {
        using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(5));
        while (!timeout.IsCancellationRequested)
        {
            var request = server.Requests.FirstOrDefault(request => request.Method == method);
            if (request is not null)
            {
                return request;
            }

            await Task.Delay(20, CancellationToken.None);
        }

        throw new TimeoutException($"Timed out waiting for RPC method '{method}'.");
    }

    private static async Task ReplaceConnectionCliProcessAsync(CopilotClient client, Process process)
    {
        var field = typeof(CopilotClient).GetField("_connectionTask", BindingFlags.Instance | BindingFlags.NonPublic)
            ?? throw new InvalidOperationException("_connectionTask field was not found.");
        var connectionTask = (Task)field.GetValue(client)!;
        await connectionTask;

        var resultProperty = connectionTask.GetType().GetProperty(nameof(Task<object>.Result))
            ?? throw new InvalidOperationException("Connection task result property was not found.");
        var connection = resultProperty.GetValue(connectionTask)!;
        var connectionType = connection.GetType();
        var rpc = connectionType.GetProperty("Rpc")!.GetValue(connection);
        var networkStream = connectionType.GetProperty("NetworkStream")!.GetValue(connection);
        var constructor = connectionType.GetConstructors(BindingFlags.Instance | BindingFlags.NonPublic | BindingFlags.Public).Single();
        var updatedConnection = constructor.Invoke([rpc, process, networkStream, null, null]);
        var fromResult = typeof(Task).GetMethod(nameof(Task.FromResult))!.MakeGenericMethod(connectionType);
        field.SetValue(client, fromResult.Invoke(null, [updatedConnection]));
    }

    private static Process StartExitedProcess()
    {
        var startInfo = OperatingSystem.IsWindows()
            ? new ProcessStartInfo(Environment.GetEnvironmentVariable("COMSPEC") ?? "cmd.exe", "/c exit 0")
            : new ProcessStartInfo("/bin/sh", "-c \"exit 0\"");
        startInfo.UseShellExecute = false;
        var process = Process.Start(startInfo)
            ?? throw new InvalidOperationException("Failed to start test process.");
        process.WaitForExit();
        return process;
    }

    private sealed class FakeCopilotServer : IAsyncDisposable
    {
        private readonly TcpListener _listener;
        private readonly CancellationTokenSource _cts = new();
        private readonly SemaphoreSlim _writeLock = new(1, 1);
        private readonly TaskCompletionSource _destroyStarted = new(TaskCreationOptions.RunContinuationsAsynchronously);
        private readonly TaskCompletionSource _allowDestroy = new(TaskCreationOptions.RunContinuationsAsynchronously);
        private readonly TaskCompletionSource _allowCompletionEvents = new(TaskCreationOptions.RunContinuationsAsynchronously);
        private readonly TaskCompletionSource _activityReactivated = new(TaskCreationOptions.RunContinuationsAsynchronously);
        private readonly TaskCompletionSource _reactivatedActivityObserved = new(TaskCreationOptions.RunContinuationsAsynchronously);
        private readonly Task _serverTask;
        private readonly List<RpcRequestRecord> _requests = [];
        private readonly object _requestsLock = new();
        private string? _lastSessionId;
        private bool _delayDestroy;
        private bool _failRuntimeShutdown;
        private bool _emitCompletionWithoutIdle;
        private bool _delayCompletionEvents;
        private bool _emitDelayedIdle;
        private bool _fallbackMethodsUnavailable;
        private bool _reactivateDuringFinalBarrier;
        private bool _hasActiveWork;
        private int _activityRequestCount;

        private FakeCopilotServer(TcpListener listener)
        {
            _listener = listener;
            _serverTask = RunAsync();
        }

        public string Url
        {
            get
            {
                var endpoint = (IPEndPoint)_listener.LocalEndpoint;
                return $"http://127.0.0.1:{endpoint.Port}";
            }
        }

        public static Task<FakeCopilotServer> StartAsync()
        {
            var listener = new TcpListener(IPAddress.Loopback, 0);
            listener.Start();
            return Task.FromResult(new FakeCopilotServer(listener));
        }

        public Task DestroyStarted => _destroyStarted.Task;

        public Task ActivityReactivated => _activityReactivated.Task;

        public Task ReactivatedActivityObserved => _reactivatedActivityObserved.Task;

        public int RuntimeShutdownCount { get; private set; }

        public IReadOnlyList<RpcRequestRecord> Requests
        {
            get
            {
                lock (_requestsLock)
                {
                    return _requests.ToArray();
                }
            }
        }

        public void ClearRequests()
        {
            lock (_requestsLock)
            {
                _requests.Clear();
            }
        }

        public void DelayDestroy()
        {
            _delayDestroy = true;
        }

        public void CompleteDestroy()
        {
            _allowDestroy.TrySetResult();
        }

        public void FailRuntimeShutdown()
        {
            _failRuntimeShutdown = true;
        }

        public void ConfigureDroppedIdleCompletion(bool delayEvents = false)
        {
            _emitCompletionWithoutIdle = true;
            _delayCompletionEvents = delayEvents;
            if (!delayEvents)
            {
                _allowCompletionEvents.TrySetResult();
            }
        }

        public void ConfigureLegacyRuntimeCompletion()
        {
            ConfigureDroppedIdleCompletion();
            _emitDelayedIdle = true;
            _fallbackMethodsUnavailable = true;
        }

        public void ReleaseCompletionEvents()
        {
            _allowCompletionEvents.TrySetResult();
        }

        public void ConfigureActivityReactivationDuringFinalBarrier()
        {
            ConfigureDroppedIdleCompletion();
            _reactivateDuringFinalBarrier = true;
        }

        public void CompleteReactivatedActivity()
        {
            Volatile.Write(ref _hasActiveWork, false);
        }

        public async ValueTask DisposeAsync()
        {
            _allowDestroy.TrySetResult();
            _cts.Cancel();
            _listener.Stop();

            try
            {
                await _serverTask;
            }
            catch (Exception ex) when (ex is OperationCanceledException or ObjectDisposedException or IOException or SocketException)
            {
            }

            _cts.Dispose();
            _writeLock.Dispose();
        }

        private async Task RunAsync()
        {
            using var tcpClient = await _listener.AcceptTcpClientAsync(_cts.Token);
            using var stream = tcpClient.GetStream();

            while (!_cts.Token.IsCancellationRequested)
            {
                using var request = await ReadMessageAsync(stream, _cts.Token);
                if (request is null)
                {
                    return;
                }

                await HandleRequestAsync(stream, request.RootElement, _cts.Token);
            }
        }

        private async Task HandleRequestAsync(Stream stream, JsonElement request, CancellationToken cancellationToken)
        {
            if (!request.TryGetProperty("id", out var idElement))
            {
                return;
            }

            var id = idElement.Clone();
            var method = request.GetProperty("method").GetString();
            if (method == "runtime.shutdown" && _failRuntimeShutdown)
            {
                RuntimeShutdownCount++;
                await WriteMessageAsync(stream, new Dictionary<string, object?>
                {
                    ["jsonrpc"] = "2.0",
                    ["id"] = id,
                    ["error"] = new Dictionary<string, object?>
                    {
                        ["code"] = -32000,
                        ["message"] = "runtime shutdown failed"
                    }
                }, cancellationToken);
                return;
            }

            var paramsElement = request.TryGetProperty("params", out var rawParams)
                ? rawParams.Clone()
                : JsonDocument.Parse("{}").RootElement.Clone();
            lock (_requestsLock)
            {
                _requests.Add(new RpcRequestRecord(method!, paramsElement));
            }

            if (_fallbackMethodsUnavailable
                && method is "session.tasks.waitForPending" or "session.metadata.activity")
            {
                await WriteMessageAsync(stream, new Dictionary<string, object?>
                {
                    ["jsonrpc"] = "2.0",
                    ["id"] = id,
                    ["error"] = new Dictionary<string, object?>
                    {
                        ["code"] = -32601,
                        ["message"] = $"Method not found: {method}"
                    }
                }, cancellationToken);
                return;
            }

            var activityRequestNumber = 0;
            if (method == "session.metadata.activity")
            {
                activityRequestNumber = Interlocked.Increment(ref _activityRequestCount);
                if (_reactivateDuringFinalBarrier && activityRequestNumber == 2)
                {
                    await EmitActivityReactivationBarrierEventAsync(stream, cancellationToken);
                }
                else if (_reactivateDuringFinalBarrier && activityRequestNumber >= 3 && Volatile.Read(ref _hasActiveWork))
                {
                    _reactivatedActivityObserved.TrySetResult();
                }
            }

            object? result = method switch
            {
                "connect" => new Dictionary<string, object?>
                {
                    ["ok"] = true,
                    ["protocolVersion"] = 3,
                    ["version"] = "test"
                },
                "session.create" => CreateSessionResult(request),
                "session.resume" => CreateSessionResult(request),
                "session.eventLog.registerInterest" => new Dictionary<string, object?>
                {
                    ["id"] = "interest-1"
                },
                "session.send" => new Dictionary<string, object?>
                {
                    ["messageId"] = "message-1"
                },
                "session.metadata.activity" => new Dictionary<string, object?>
                {
                    ["abortable"] = false,
                    ["hasActiveWork"] = Volatile.Read(ref _hasActiveWork)
                },
                "session.tasks.waitForPending" => new Dictionary<string, object?>(),
                "session.mcp.oauth.handlePendingRequest" => new Dictionary<string, object?>
                {
                    ["success"] = true
                },
                "session.delete" => new Dictionary<string, object?>
                {
                    ["success"] = true
                },
                "session.destroy" => await DestroySessionAsync(cancellationToken),
                "runtime.shutdown" => HandleRuntimeShutdown(),
                _ => throw new InvalidOperationException($"Unexpected RPC method '{method}'.")
            };

            await WriteMessageAsync(stream, new Dictionary<string, object?>
            {
                ["jsonrpc"] = "2.0",
                ["id"] = id,
                ["result"] = result
            }, cancellationToken);

            if (_reactivateDuringFinalBarrier && method == "session.metadata.activity" && activityRequestNumber == 2)
            {
                Volatile.Write(ref _hasActiveWork, true);
                _activityReactivated.TrySetResult();
            }

            if (method == "session.send" && _emitCompletionWithoutIdle)
            {
                _ = EmitCompletionWithoutIdleAsync(stream, cancellationToken);
            }
        }

        private Task EmitActivityReactivationBarrierEventAsync(Stream stream, CancellationToken cancellationToken)
        {
            return WriteMessageAsync(stream, new Dictionary<string, object?>
            {
                ["jsonrpc"] = "2.0",
                ["method"] = "session.event",
                ["params"] = new Dictionary<string, object?>
                {
                    ["sessionId"] = _lastSessionId,
                    ["event"] = new Dictionary<string, object?>
                    {
                        ["type"] = "assistant.turn_end",
                        ["id"] = Guid.NewGuid().ToString(),
                        ["timestamp"] = DateTimeOffset.UtcNow.ToString("O"),
                        ["data"] = new Dictionary<string, object?>
                        {
                            ["turnId"] = "activity-reactivation-barrier"
                        }
                    }
                }
            }, cancellationToken);
        }

        private async Task EmitCompletionWithoutIdleAsync(Stream stream, CancellationToken cancellationToken)
        {
            if (_delayCompletionEvents)
            {
                await _allowCompletionEvents.Task.WaitAsync(cancellationToken);
            }

            await WriteMessageAsync(stream, new Dictionary<string, object?>
            {
                ["jsonrpc"] = "2.0",
                ["method"] = "session.event",
                ["params"] = new Dictionary<string, object?>
                {
                    ["sessionId"] = _lastSessionId,
                    ["event"] = new Dictionary<string, object?>
                    {
                        ["type"] = "assistant.message",
                        ["id"] = Guid.NewGuid().ToString(),
                        ["timestamp"] = DateTimeOffset.UtcNow.ToString("O"),
                        ["data"] = new Dictionary<string, object?>
                        {
                            ["content"] = "completed response",
                            ["messageId"] = "assistant-message-1"
                        }
                    }
                }
            }, cancellationToken);

            await WriteMessageAsync(stream, new Dictionary<string, object?>
            {
                ["jsonrpc"] = "2.0",
                ["method"] = "session.event",
                ["params"] = new Dictionary<string, object?>
                {
                    ["sessionId"] = _lastSessionId,
                    ["event"] = new Dictionary<string, object?>
                    {
                        ["type"] = "assistant.turn_end",
                        ["id"] = Guid.NewGuid().ToString(),
                        ["timestamp"] = DateTimeOffset.UtcNow.ToString("O"),
                        ["data"] = new Dictionary<string, object?>
                        {
                            ["turnId"] = "turn-1"
                        }
                    }
                }
            }, cancellationToken);

            if (_emitDelayedIdle)
            {
                await Task.Delay(TimeSpan.FromMilliseconds(500), cancellationToken);
                await WriteMessageAsync(stream, new Dictionary<string, object?>
                {
                    ["jsonrpc"] = "2.0",
                    ["method"] = "session.event",
                    ["params"] = new Dictionary<string, object?>
                    {
                        ["sessionId"] = _lastSessionId,
                        ["event"] = new Dictionary<string, object?>
                        {
                            ["type"] = "session.idle",
                            ["id"] = Guid.NewGuid().ToString(),
                            ["timestamp"] = DateTimeOffset.UtcNow.ToString("O"),
                            ["data"] = new Dictionary<string, object?>()
                        }
                    }
                }, cancellationToken);
            }
        }

        private Dictionary<string, object?> CreateSessionResult(JsonElement request)
        {
            string? sessionId = null;
            if (request.TryGetProperty("params", out var paramsProp)
                && paramsProp.ValueKind == JsonValueKind.Object
                && paramsProp.TryGetProperty("sessionId", out var sidProp)
                && sidProp.ValueKind == JsonValueKind.String)
            {
                sessionId = sidProp.GetString();
            }
            if (string.IsNullOrEmpty(sessionId))
            {
                sessionId = Guid.NewGuid().ToString();
            }
            _lastSessionId = sessionId;

            return new Dictionary<string, object?>
            {
                ["sessionId"] = _lastSessionId,
                ["workspacePath"] = null,
                ["capabilities"] = null
            };
        }

        private async Task<Dictionary<string, object?>> DestroySessionAsync(CancellationToken cancellationToken)
        {
            if (_delayDestroy)
            {
                _destroyStarted.TrySetResult();
                await _allowDestroy.Task.WaitAsync(cancellationToken);
            }

            return [];
        }

        private Dictionary<string, object?> HandleRuntimeShutdown()
        {
            RuntimeShutdownCount++;
            return [];
        }

        private async Task WriteMessageAsync(Stream stream, object payload, CancellationToken cancellationToken)
        {
            using var bodyStream = new MemoryStream();
            using (var writer = new Utf8JsonWriter(bodyStream))
            {
                WriteJsonValue(writer, payload);
            }

            var body = bodyStream.ToArray();
            var header = Encoding.ASCII.GetBytes($"Content-Length: {body.Length}\r\n\r\n");

            await _writeLock.WaitAsync(cancellationToken);
            try
            {
                await stream.WriteAsync(header, cancellationToken);
                await stream.WriteAsync(body, cancellationToken);
                await stream.FlushAsync(cancellationToken);
            }
            finally
            {
                _writeLock.Release();
            }
        }

        private static void WriteJsonValue(Utf8JsonWriter writer, object? value)
        {
            switch (value)
            {
                case null:
                    writer.WriteNullValue();
                    break;

                case string stringValue:
                    writer.WriteStringValue(stringValue);
                    break;

                case bool boolValue:
                    writer.WriteBooleanValue(boolValue);
                    break;

                case int intValue:
                    writer.WriteNumberValue(intValue);
                    break;

                case long longValue:
                    writer.WriteNumberValue(longValue);
                    break;

                case JsonElement jsonElement:
                    jsonElement.WriteTo(writer);
                    break;

                case Dictionary<string, object?> dictionary:
                    writer.WriteStartObject();
                    foreach (var (propertyName, propertyValue) in dictionary)
                    {
                        writer.WritePropertyName(propertyName);
                        WriteJsonValue(writer, propertyValue);
                    }
                    writer.WriteEndObject();
                    break;

                case object?[] array:
                    writer.WriteStartArray();
                    foreach (var item in array)
                    {
                        WriteJsonValue(writer, item);
                    }
                    writer.WriteEndArray();
                    break;

                default:
                    throw new InvalidOperationException($"Unexpected JSON value type '{value.GetType().Name}'.");
            }
        }

        private static async Task<JsonDocument?> ReadMessageAsync(Stream stream, CancellationToken cancellationToken)
        {
            var headerBytes = new List<byte>();
            while (true)
            {
                var value = await ReadByteAsync(stream, cancellationToken);
                if (value < 0)
                {
                    return null;
                }

                headerBytes.Add((byte)value);
                var count = headerBytes.Count;
                if (count >= 4 &&
                    headerBytes[count - 4] == '\r' &&
                    headerBytes[count - 3] == '\n' &&
                    headerBytes[count - 2] == '\r' &&
                    headerBytes[count - 1] == '\n')
                {
                    break;
                }
            }

            var header = Encoding.ASCII.GetString([.. headerBytes]);
            var contentLength = header
                .Split(["\r\n"], StringSplitOptions.RemoveEmptyEntries)
                .Select(line => line.Split(':', 2))
                .Where(parts => parts.Length == 2 && parts[0].Equals("Content-Length", StringComparison.OrdinalIgnoreCase))
                .Select(parts => int.Parse(parts[1].Trim(), System.Globalization.CultureInfo.InvariantCulture))
                .Single();

            var body = new byte[contentLength];
            var offset = 0;
            while (offset < body.Length)
            {
                var read = await stream.ReadAsync(body.AsMemory(offset, body.Length - offset), cancellationToken);
                if (read == 0)
                {
                    return null;
                }

                offset += read;
            }

            return JsonDocument.Parse(body);
        }

        private static async Task<int> ReadByteAsync(Stream stream, CancellationToken cancellationToken)
        {
            var buffer = new byte[1];
            var read = await stream.ReadAsync(buffer, cancellationToken);
            return read == 0 ? -1 : buffer[0];
        }
    }
}
#endif
