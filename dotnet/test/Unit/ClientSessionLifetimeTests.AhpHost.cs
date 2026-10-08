/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Collections.Concurrent;
using System.Text.Json;
using GitHub.Copilot.Rpc;
using Microsoft.Extensions.Logging;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    private static Dictionary<string, object?> CreateAhpHostResult(JsonElement parameters)
    {
        var result = new Dictionary<string, object?>
        {
            ["hostId"] = parameters.GetProperty("hostId").GetString()
        };
        if (parameters.TryGetProperty("localServer", out var local) && local.ValueKind == JsonValueKind.Object)
        {
            result["url"] = "ws://127.0.0.1:12345";
            result["token"] = "test-token";
        }
        if (parameters.TryGetProperty("githubEnvironment", out var github) && github.ValueKind == JsonValueKind.Object)
            result["environmentId"] = "environment-123";
        return result;
    }

    [Fact]
    public async Task Ahp_Requires_Explicit_Transport_Before_Connecting()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url) });
        await Assert.ThrowsAsync<ArgumentNullException>(() => client.StartAhpHostAsync(null!));
        await Assert.ThrowsAsync<ArgumentException>(() => client.StartAhpHostAsync(new()));
        Assert.Empty(server.Requests);
    }

    [Theory]
    [InlineData("", "compute")]
    [InlineData(" ", "compute")]
    [InlineData("host", "")]
    [InlineData("host", " ")]
    public async Task Ahp_Forwards_Environment_Fields_For_Runtime_Validation(string name, string computeId)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var host = await client.StartAhpHostAsync(new()
        {
            GitHubEnvironment = new() { Name = name, ComputeId = computeId }
        });
        var request = Assert.Single(server.Requests, request => request.Method == "host.start").Params;
        var environment = request.GetProperty("githubEnvironment");
        Assert.Equal(name, environment.GetProperty("name").GetString());
        Assert.Equal(computeId, environment.GetProperty("computeId").GetString());
    }

    [Fact]
    public async Task Ahp_Snapshots_Transport_Settings_Before_Connecting()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url) });
        var connecting = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var finishConnect = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, cancellationToken) =>
        {
            if (request.Method == "connect")
            {
                connecting.TrySetResult();
                await finishConnect.Task.WaitAsync(cancellationToken);
            }
        };
        var local = new HostLocalServerOptions { Hostname = "127.0.0.1", Port = 0, Token = "original", RequireConnectionToken = true };
        var github = new HostGitHubEnvironmentOptions { Name = "original", ComputeId = "compute", RequireConnectionBinding = false };
        var pending = client.StartAhpHostAsync(new() { LocalServer = local, GitHubEnvironment = github });
        await connecting.Task.WaitAsync(TimeSpan.FromSeconds(10));
        try
        {
            local.Hostname = "localhost";
            local.Port = 12345;
            local.Token = "replacement";
            local.RequireConnectionToken = false;
            github.Name = "";
            github.ComputeId = "";
            github.RequireConnectionBinding = true;
        }
        finally
        {
            finishConnect.TrySetResult();
        }
        await using var host = await pending;
        var request = Assert.Single(server.Requests, request => request.Method == "host.start").Params;
        var localSettings = request.GetProperty("localServer");
        Assert.Equal("127.0.0.1", localSettings.GetProperty("hostname").GetString());
        Assert.Equal(0, localSettings.GetProperty("port").GetInt32());
        Assert.Equal("original", localSettings.GetProperty("token").GetString());
        Assert.True(localSettings.GetProperty("requireConnectionToken").GetBoolean());
        var githubSettings = request.GetProperty("githubEnvironment");
        Assert.Equal("original", githubSettings.GetProperty("name").GetString());
        Assert.Equal("compute", githubSettings.GetProperty("computeId").GetString());
        Assert.False(githubSettings.GetProperty("requireConnectionBinding").GetBoolean());
    }

    [Theory]
    [InlineData(true, false)]
    [InlineData(false, true)]
    [InlineData(true, true)]
    public async Task Ahp_Forwards_Explicit_Transports_And_Optional_Readiness(bool local, bool github)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var host = await client.StartAhpHostAsync(new()
        {
            ComputeId = "compute",
            LocalServer = local ? new() { Hostname = "127.0.0.1", Port = 0, Token = "test-token", RequireConnectionToken = true } : null,
            GitHubEnvironment = github ? new() { Name = "test host", ComputeId = "compute" } : null
        });
        var request = Assert.Single(server.Requests, request => request.Method == "host.start").Params;
        Assert.Equal("compute", request.GetProperty("computeId").GetString());
        Assert.Equal(local, request.TryGetProperty("localServer", out var localSettings) && localSettings.ValueKind == JsonValueKind.Object);
        Assert.Equal(github, request.TryGetProperty("githubEnvironment", out var githubSettings) && githubSettings.ValueKind == JsonValueKind.Object);
        foreach (var oldField in new[] { "hostname", "port", "token", "requireConnectionToken" })
            Assert.False(request.TryGetProperty(oldField, out _));
        if (local)
        {
            Assert.Equal("test-token", localSettings.GetProperty("token").GetString());
            Assert.Equal(0, localSettings.GetProperty("port").GetInt32());
        }
        if (github)
        {
            Assert.Equal("test host", githubSettings.GetProperty("name").GetString());
            Assert.Equal("compute", githubSettings.GetProperty("computeId").GetString());
        }
        Assert.Equal(local ? "ws://127.0.0.1:12345" : null, host.Url);
        Assert.Equal(local ? "test-token" : null, host.Token);
        Assert.Equal(github ? "environment-123" : null, host.EnvironmentId);
        Assert.Null(host.Pid);
    }

    [Fact]
    public async Task Ahp_Legacy_Positional_Start_And_Additive_Request_Overloads()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();
        var local = new HostLocalServerOptions { Port = 12345 };
        var github = new HostGitHubEnvironmentOptions { Name = "host", ComputeId = "compute" };

        await client.Rpc.Host.StartAsync("legacy", local, github, true, false, CancellationToken.None);
        await client.Rpc.Host.StartAsync("defaults");
        await client.Rpc.Host.StartAsync(new HostStartRequest
        {
            HostId = "request",
            ComputeId = "compute",
            LocalServer = local,
            GitHubEnvironment = github,
            SessionFactory = true,
            ResumeFactory = false
        }, CancellationToken.None);

        var requests = server.Requests.Where(request => request.Method == "host.start").ToArray();
        Assert.Equal(3, requests.Length);
        var legacy = requests[0].Params;
        Assert.Equal("legacy", legacy.GetProperty("hostId").GetString());
        Assert.False(legacy.TryGetProperty("computeId", out _));
        Assert.Equal("defaults", requests[1].Params.GetProperty("hostId").GetString());
        Assert.False(requests[1].Params.TryGetProperty("localServer", out _));
        var additive = requests[2].Params;
        Assert.Equal("request", additive.GetProperty("hostId").GetString());
        Assert.Equal("compute", additive.GetProperty("computeId").GetString());
        foreach (var parameters in new[] { legacy, additive })
        {
            Assert.Equal(12345, parameters.GetProperty("localServer").GetProperty("port").GetInt32());
            Assert.Equal("compute", parameters.GetProperty("githubEnvironment").GetProperty("computeId").GetString());
            Assert.True(parameters.GetProperty("sessionFactory").GetBoolean());
            Assert.False(parameters.GetProperty("resumeFactory").GetBoolean());
        }
    }

    [Fact]
    public async Task Ahp_List_Sessions_Uses_Owning_Host()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var host = await client.StartAhpHostAsync(new() { LocalServer = new() });

        var result = await host.ListSessionsAsync();

        Assert.Empty(result.Sessions);
        var request = Assert.Single(server.Requests, request => request.Method == "host.listSessions").Params;
        Assert.Equal(host.HostId, request.GetProperty("hostId").GetString());
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task Ahp_Only_Suppresses_Cancellation_From_The_Released_Handoff(bool expectedCancellation)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        var logger = new AhpFactoryLogger();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url), Logger = logger });
        var entered = new TaskCompletionSource<CancellationToken>(TaskCreationOptions.RunContinuationsAsynchronously);
        var factory = new TaskCompletionSource<CopilotSession>();
        var host = await client.StartAhpHostAsync(new()
        {
            LocalServer = new(),
            CreateSession = request => { entered.TrySetResult(request.CancellationToken); return factory.Task; }
        });
        var pending = server.SendRequestAsync("host.materializeSession", new()
        {
            ["hostId"] = host.HostId,
            ["handoffId"] = "cancelled",
            ["config"] = new Dictionary<string, object?> { ["sessionId"] = "requested" }
        });
        var token = await entered.Task.WaitAsync(TimeSpan.FromSeconds(5));
        await server.SendRequestAsync("host.sessionReleased", new() { ["hostId"] = host.HostId, ["handoffId"] = "cancelled" });
        await Assert.ThrowsAsync<InvalidOperationException>(() => pending.WaitAsync(TimeSpan.FromSeconds(5)));
        factory.SetException(new OperationCanceledException(expectedCancellation ? token : CancellationToken.None));
        if (expectedCancellation)
            await Assert.ThrowsAsync<TimeoutException>(() => logger.Failure.Task.WaitAsync(TimeSpan.FromMilliseconds(200)));
        else
            Assert.IsAssignableFrom<OperationCanceledException>(await logger.Failure.Task.WaitAsync(TimeSpan.FromSeconds(5)));
    }

    private sealed class AhpFactoryLogger : ILogger
    {
        public TaskCompletionSource<Exception?> Failure { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;
        public bool IsEnabled(LogLevel logLevel) => true;
        public void Log<TState>(LogLevel logLevel, EventId eventId, TState state, Exception? exception, Func<TState, Exception?, string> formatter)
        {
            if (formatter(state, exception) == "AHP session factory failed") Failure.TrySetResult(exception);
        }
    }

    [Fact]
    public async Task Ahp_Cancelled_Startup_Disposes_After_Start_Completes()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url) });
        var entered = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var finish = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var disposed = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == "host.start")
            {
                entered.TrySetResult(request.Params.GetProperty("hostId").GetString()!);
                await finish.Task.WaitAsync(token);
            }
            if (request.Method == "host.dispose")
                disposed.TrySetResult(request.Params.GetProperty("hostId").GetString()!);
        };
        using var cancellation = new CancellationTokenSource();
        var pending = client.StartAhpHostAsync(new() { LocalServer = new() }, cancellation.Token);
        var hostId = await entered.Task.WaitAsync(TimeSpan.FromSeconds(5));
        cancellation.Cancel();
        await Assert.ThrowsAnyAsync<OperationCanceledException>(() => pending.WaitAsync(TimeSpan.FromSeconds(5)));
        Assert.False(disposed.Task.IsCompleted);
        finish.SetResult();
        Assert.Equal(hostId, await disposed.Task.WaitAsync(TimeSpan.FromSeconds(5)));
    }

    [Fact]
    public async Task Ahp_Disposal_Is_Repeated_And_Exit_Is_Once()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url) });
        var exits = new ConcurrentQueue<AhpHostExit>();
        var exited = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var host = await client.StartAhpHostAsync(new()
        {
            LocalServer = new(),
            OnExit = report => { exits.Enqueue(report); exited.TrySetResult(); return Task.CompletedTask; }
        });
        Assert.Null(host.Pid);
        await Task.WhenAll(host.DisposeAsync().AsTask(), host.DisposeAsync().AsTask());
        Assert.Equal(2, server.Requests.Count(request => request.Method == "host.dispose"));
        for (var i = 0; i < 2; i++)
            await server.SendRequestAsync("host.exited", new() { ["hostId"] = host.HostId, ["reason"] = "disposed" });
        await exited.Task.WaitAsync(TimeSpan.FromSeconds(5));
        await client.StopAsync();
        Assert.Equal(HostExitReason.Disposed, Assert.Single(exits).Reason);
        await Assert.ThrowsAnyAsync<Exception>(() => host.DisposeAsync().AsTask());
    }

    [Theory]
    [InlineData(false, false)]
    [InlineData(false, true)]
    [InlineData(true, false)]
    [InlineData(true, true)]
    public async Task Ahp_Factories_Preserve_Config_And_Release_Exact_Original(bool resume, bool enabled)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url) });
        CopilotSession? original = null;
        CancellationToken cancellation = default;
        var released = new TaskCompletionSource<CopilotSession>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseCount = 0;
        var host = await client.StartAhpHostAsync(new()
        {
            LocalServer = new(),
            CreateSession = async request =>
            {
                cancellation = request.CancellationToken;
                return original = await client.CreateSessionAsync(request.Config, cancellation);
            },
            ResumeSession = async request =>
            {
                cancellation = request.CancellationToken;
                return original = await client.ResumeSessionAsync(request.SessionId, request.Config, cancellation);
            },
            OnSessionReleased = session =>
            {
                Interlocked.Increment(ref releaseCount);
                released.TrySetResult(session);
                return Task.CompletedTask;
            }
        });
        var config = new Dictionary<string, object?>
        {
            ["sessionId"] = "original",
            ["workingDirectory"] = "/workspace",
            ["configDir"] = "/config",
            ["additionalDirectories"] = Array.Empty<string>(),
            ["mcpOAuthTokenStorage"] = "in-memory",
            ["gitHubToken"] = "test-auth",
            ["enableMcpApps"] = enabled,
            ["streaming"] = enabled,
            ["enableExperimentalMode"] = false,
            ["infiniteSessions"] = new Dictionary<string, object?> { ["enabled"] = false, ["backgroundCompactionThreshold"] = 0.7 },
            ["featureFlags"] = new Dictionary<string, object?> { ["Arbitrary.MixedCase"] = true }
        };
        if (resume)
        {
            config["suppressResumeEvent"] = enabled;
            config["continuePendingWork"] = false;
        }
        var result = await server.SendRequestAsync("host.materializeSession", new()
        {
            ["hostId"] = host.HostId,
            ["handoffId"] = "handoff",
            ["resume"] = resume,
            ["config"] = config
        });
        Assert.Equal("original", result.GetProperty("sessionId").GetString());
        await server.SendRequestAsync("host.sessionReleased", new() { ["hostId"] = "wrong", ["handoffId"] = "handoff" });
        Assert.False(cancellation.IsCancellationRequested);
        for (var i = 0; i < 2; i++)
            await server.SendRequestAsync("host.sessionReleased", new() { ["hostId"] = host.HostId, ["handoffId"] = "handoff" });
        var releasedSession = await released.Task.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.Same(original, releasedSession);
        Assert.True(cancellation.IsCancellationRequested);
        Assert.Equal(1, releaseCount);
        Assert.DoesNotContain(server.Requests, request => request.Method is "session.destroy" or "session.detach");
        await original!.GetEventsAsync();
    }

    [Fact]
    public async Task Ahp_Cancellation_Callback_Can_Wait_For_Rpc()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url) });
        var cancelled = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var released = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        CancellationTokenRegistration registration = default;
        AhpHost? host = null;
        host = await client.StartAhpHostAsync(new()
        {
            LocalServer = new(),
            CreateSession = async request =>
            {
                registration = request.CancellationToken.Register(() =>
                {
                    try
                    {
                        host!.DisposeAsync().AsTask().WaitAsync(TimeSpan.FromSeconds(5)).GetAwaiter().GetResult();
                        cancelled.TrySetResult();
                    }
                    catch (Exception error) { cancelled.TrySetException(error); }
                });
                return await client.CreateSessionAsync(request.Config);
            },
            OnSessionReleased = _ =>
            {
                Assert.True(cancelled.Task.IsCompletedSuccessfully);
                released.TrySetResult();
                return Task.CompletedTask;
            }
        });
        try
        {
            await server.SendRequestAsync("host.materializeSession", new()
            {
                ["hostId"] = host.HostId,
                ["handoffId"] = "callback",
                ["config"] = new Dictionary<string, object?> { ["sessionId"] = "callback" }
            });
            await server.SendRequestAsync("host.sessionReleased", new() { ["hostId"] = host.HostId, ["handoffId"] = "callback" })
                .WaitAsync(TimeSpan.FromSeconds(10));
            await cancelled.Task.WaitAsync(TimeSpan.FromSeconds(10));
            await released.Task.WaitAsync(TimeSpan.FromSeconds(5));
        }
        finally { registration.Dispose(); }
    }

    [Theory]
    [InlineData("streaming", false)]
    [InlineData("streaming", true)]
    [InlineData("enableMcpApps", false)]
    [InlineData("enableMcpApps", true)]
    [InlineData("suppressResumeEvent", false)]
    [InlineData("suppressResumeEvent", true)]
    public async Task Ahp_Rejects_Changed_Boolean_Settings(string setting, bool expected)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url) });
        var host = await client.StartAhpHostAsync(new()
        {
            LocalServer = new(),
            ResumeSession = request =>
            {
                switch (setting)
                {
                    case "streaming": request.Config.Streaming = !expected; break;
                    case "enableMcpApps": request.Config.EnableMcpApps = !expected; break;
                    case "suppressResumeEvent": request.Config.SuppressResumeEvent = !expected; break;
                }
                return client.ResumeSessionAsync(request.SessionId, request.Config);
            }
        });
        var error = await Assert.ThrowsAsync<InvalidOperationException>(() => server.SendRequestAsync("host.materializeSession", new()
        {
            ["hostId"] = host.HostId,
            ["handoffId"] = "changed",
            ["resume"] = true,
            ["config"] = new Dictionary<string, object?> { ["sessionId"] = "changed", [setting] = expected }
        }));
        Assert.Contains("preserve", error.Message);
    }

    [Fact]
    public async Task Ahp_Pending_Cancellation_Unblocks_And_Releases_Late_Result()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url) });
        var original = await client.CreateSessionAsync(new() { SessionId = "late" });
        var entered = new TaskCompletionSource<CancellationToken>(TaskCreationOptions.RunContinuationsAsynchronously);
        var factory = new TaskCompletionSource<CopilotSession>(TaskCreationOptions.RunContinuationsAsynchronously);
        var released = new TaskCompletionSource<CopilotSession>(TaskCreationOptions.RunContinuationsAsynchronously);
        var host = await client.StartAhpHostAsync(new()
        {
            LocalServer = new(),
            CreateSession = request => { entered.TrySetResult(request.CancellationToken); return factory.Task; },
            OnSessionReleased = session => { released.TrySetResult(session); return Task.CompletedTask; }
        });
        var parameters = new Dictionary<string, object?>
        {
            ["hostId"] = host.HostId,
            ["handoffId"] = "pending",
            ["config"] = new Dictionary<string, object?> { ["sessionId"] = "late" }
        };
        var pending = server.SendRequestAsync("host.materializeSession", parameters);
        var cancellation = await entered.Task.WaitAsync(TimeSpan.FromSeconds(5));
        await Assert.ThrowsAsync<InvalidOperationException>(() => server.SendRequestAsync("host.materializeSession", parameters));
        await server.SendRequestAsync("host.sessionReleased", new() { ["hostId"] = host.HostId, ["handoffId"] = "pending" });
        await Assert.ThrowsAsync<InvalidOperationException>(() => pending.WaitAsync(TimeSpan.FromSeconds(5)));
        Assert.True(cancellation.IsCancellationRequested);
        factory.SetResult(original);
        var releasedSession = await released.Task.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.Same(original, releasedSession);
        Assert.DoesNotContain(server.Requests, request => request.Method is "session.destroy" or "session.detach");
    }

    [Fact]
    public async Task Ahp_Rejects_Modified_Config_But_Allows_Retained_Resume()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new() { Connection = RuntimeConnection.ForUri(server.Url) });
        CopilotSession? original = null;
        var released = new TaskCompletionSource<CopilotSession>(TaskCreationOptions.RunContinuationsAsynchronously);
        var host = await client.StartAhpHostAsync(new()
        {
            LocalServer = new(),
            CreateSession = async request =>
            {
                request.Config.WorkingDirectory = "/wrong";
                return original = await client.CreateSessionAsync(request.Config);
            },
            ResumeSession = _ => Task.FromResult(original!),
            OnSessionReleased = session => { released.TrySetResult(session); return Task.CompletedTask; }
        });
        var parameters = new Dictionary<string, object?>
        {
            ["hostId"] = host.HostId,
            ["handoffId"] = "create",
            ["config"] = new Dictionary<string, object?> { ["sessionId"] = "retained", ["workingDirectory"] = "/selected" }
        };
        var error = await Assert.ThrowsAsync<InvalidOperationException>(() => server.SendRequestAsync("host.materializeSession", parameters));
        Assert.Contains("preserve", error.Message);
        var releasedSession = await released.Task.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.Same(original, releasedSession);
        parameters["handoffId"] = "resume";
        parameters["resume"] = true;
        var result = await server.SendRequestAsync("host.materializeSession", parameters);
        Assert.Equal("retained", result.GetProperty("sessionId").GetString());
    }
}
#endif
