/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using GitHub.Copilot.Rpc;
using Microsoft.Extensions.Logging;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Fact]
    public async Task Resume_Custom_Id_Does_Not_Replace_A_Longer_Custom_Id_Callback()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var resident = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "session-123",
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        });
        var started = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var release = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == "session.resume")
            {
                started.TrySetResult();
                await release.Task.WaitAsync(token);
            }
        };
        var resume = client.ResumeSessionAsync("session-1", new ResumeSessionConfig
        {
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.Yes)
        });
        try
        {
            await started.Task.WaitAsync(TimeSpan.FromSeconds(5));
            var original = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = resident.SessionId });
            Assert.Equal("no", original.GetProperty("response").GetString());
            var provisional = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = "session-1" });
            Assert.Equal("yes", provisional.GetProperty("response").GetString());
        }
        finally
        {
            release.TrySetResult();
        }
        await using var resumed = await resume;
        Assert.Equal("session-1", resumed.SessionId);
        Assert.DoesNotContain(server.Requests, request => request.Method == "sessions.findByPrefix");
    }

    [Theory]
    [InlineData("abcdef1", true)]
    [InlineData("ABCDEF12-ABCD", true)]
    [InlineData("abcdef12-", true)]
    [InlineData("abcdef12-abcd-4123-8123-123456789ab", true)]
    [InlineData("abcdef12-abcd-4123-8123-123456789abc", false)]
    [InlineData("abcdef", false)]
    [InlineData("abc-def1", false)]
    [InlineData("deadbeef_task1", false)]
    [InlineData("ébcdef1", false)]
    [InlineData("abcdef1Ｇ", false)]
    public async Task Resume_Resolves_Only_Runtime_Uuid_Prefixes(string sessionId, bool resolve)
    {
        const string canonicalId = "abcdef12-abcd-4123-8123-123456789abc";
        await using var server = await FakeCopilotServer.StartAsync();
        server.Capabilities = AutoModeRecoveryCapabilities(true);
        server.ResponseFactory = request =>
        {
            if (request.Method == "session.ui.registerDirectAutoModeSwitchHandler")
                return new Dictionary<string, object?> { ["handle"] = "direct-handler", ["autoModeRecovery"] = true };
            Assert.Equal("sessions.findByPrefix", request.Method);
            Assert.Equal(sessionId, request.Params.GetProperty("prefix").GetString());
            return new Dictionary<string, object?> { ["sessionId"] = canonicalId };
        };
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });

        await using var resumed = await client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        });

        var resume = Assert.Single(server.Requests, request => request.Method == "session.resume");
        Assert.Equal(resolve ? canonicalId : sessionId, resume.Params.GetProperty("sessionId").GetString());
        Assert.Equal(resolve ? canonicalId : sessionId, resumed.SessionId);
        Assert.Equal(resolve ? 1 : 0, server.Requests.Count(request => request.Method == "sessions.findByPrefix"));
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task Exact_Hex_Provider_Resume_Skips_Host_Lookup(bool recovery)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.Capabilities = AutoModeRecoveryCapabilities(true);
        server.ResponseFactory = request => request.Method switch
        {
            "sessionFs.setProvider" => new Dictionary<string, object?> { ["success"] = true },
            "session.ui.registerDirectAutoModeSwitchHandler" => new Dictionary<string, object?>
            {
                ["handle"] = "direct-handler",
                ["autoModeRecovery"] = true
            },
            _ => throw new InvalidOperationException($"Unexpected RPC method '{request.Method}'.")
        };
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            SessionFs = new SessionFsConfig
            {
                InitialWorkingDirectory = "/",
                SessionStatePath = "/session-state",
                Conventions = SessionFsSetProviderConventions.Posix
            }
        });
        await using var session = await client.ResumeSessionAsync("abcdef0", new ResumeSessionConfig
        {
            AutoModeRecovery = recovery,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No),
            CreateSessionFsProvider = _ => new TextOnlySessionFsProvider()
        });
        Assert.Equal("abcdef0", session.SessionId);
        Assert.DoesNotContain(server.Requests, request => request.Method == "sessions.findByPrefix");
    }

    [Fact]
    public async Task Resume_Without_Recovery_Preserves_Exact_Hex_Id_And_Configured_Storage()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var resumed = await client.ResumeSessionAsync("abcdef0", new ResumeSessionConfig
        {
            ConfigDirectory = "configured-home"
        });
        Assert.Equal("abcdef0", resumed.SessionId);
        Assert.DoesNotContain(server.Requests, request => request.Method == "sessions.findByPrefix");
        var resume = Assert.Single(server.Requests, request => request.Method == "session.resume");
        Assert.Equal("configured-home", resume.Params.GetProperty("configDir").GetString());
        Assert.Equal("abcdef0", resume.Params.GetProperty("sessionId").GetString());
    }

    [Theory]
    [InlineData("abcdef1")]
    [InlineData("abcdef12-abcd-4123-8123-123456789abc")]
    public async Task AutoModeRecovery_Exact_Tracked_Id_Still_Rejects_Without_Lookup(string sessionId)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = _ => new Dictionary<string, object?>
        {
            ["sessionId"] = "abcdef12-abcd-4123-8123-123456789abc"
        };
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var resident = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = sessionId,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        });

        var error = await Assert.ThrowsAsync<InvalidOperationException>(() => client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.YesAlways)
        }));

        Assert.Contains("already tracked", error.Message);
        var callback = await server.SendRequestAsync("autoModeSwitch.request",
            new Dictionary<string, object?> { ["sessionId"] = sessionId });
        Assert.Equal("no", callback.GetProperty("response").GetString());
        Assert.DoesNotContain(server.Requests, request => request.Method is "sessions.findByPrefix" or "session.resume" or "session.detach" or "session.delete");
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task AutoModeRecovery_Uuid_Prefix_Lookup_Failure_Preserves_Resident_Callbacks(bool ambiguous)
    {
        const string prefix = "ABCDEF12";
        const string residentId = "abcdef12-abcd-4123-8123-123456789abc";
        const string hiddenId = "abcdef12-abcd-4123-8123-123456789def";
        var persistedIds = ambiguous ? new[] { residentId, hiddenId } : [];
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var resident = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = residentId,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        });
        server.ResponseFactory = request =>
        {
            Assert.Equal("sessions.findByPrefix", request.Method);
            Assert.Equal(prefix, request.Params.GetProperty("prefix").GetString());
            var matches = persistedIds.Where(id => id.StartsWith(prefix, StringComparison.OrdinalIgnoreCase)).ToArray();
            return new Dictionary<string, object?> { ["sessionId"] = matches.Length == 1 ? matches[0] : null };
        };
        var started = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var release = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.FailSessionResume = true;
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method is "sessions.findByPrefix" or "session.resume")
            {
                started.TrySetResult(request.Method);
                await release.Task.WaitAsync(token);
            }
        };
        var provisionalCalls = 0;
        var initialization = client.ResumeSessionAsync(prefix, new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) =>
            {
                Interlocked.Increment(ref provisionalCalls);
                return Task.FromResult(AutoModeSwitchResponse.YesAlways);
            }
        });
        var timeout = TimeSpan.FromSeconds(5);
        try
        {
            var method = await started.Task.WaitAsync(timeout);
            var duringLookup = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = residentId }).WaitAsync(timeout);
            Assert.Equal("no", duringLookup.GetProperty("response").GetString());
            Assert.Equal("sessions.findByPrefix", method);
            Assert.Equal(0, provisionalCalls);
            await Assert.ThrowsAsync<InvalidOperationException>(() => client.DeleteSessionAsync(residentId).WaitAsync(timeout));
            release.TrySetResult();
            var error = await Assert.ThrowsAsync<InvalidOperationException>(() => initialization.WaitAsync(timeout));
            Assert.Contains("unique", error.Message, StringComparison.OrdinalIgnoreCase);
            Assert.Contains(prefix, error.Message);
            var afterLookup = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = residentId }).WaitAsync(timeout);
            Assert.Equal("no", afterLookup.GetProperty("response").GetString());
            Assert.Equal(0, provisionalCalls);
            Assert.Single(server.Requests, request => request.Method == "sessions.findByPrefix");
            Assert.DoesNotContain(server.Requests, request => request.Method is "session.resume" or "session.detach" or "session.delete");
        }
        finally
        {
            release.TrySetResult();
            await Record.ExceptionAsync(() => initialization.WaitAsync(timeout));
        }
    }

    [Theory]
    [InlineData(true, false)]
    [InlineData(false, false)]
    [InlineData(true, true)]
    public async Task AutoModeRecovery_Uuid_Prefix_Publishes_Canonical_Owner_Only_For_Resume(bool acknowledged, bool rejectResume)
    {
        const string prefix = "ABCDEF12";
        const string canonicalId = "abcdef12-abcd-4123-8123-123456789abc";
        const string otherId = "abcdef12-abcd-4123-8123-123456789def";
        var lookupStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseLookup = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var traceStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        using var releaseTrace = new ManualResetEventSlim();
        var resumeStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseResume = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var logger = new PrefixLookupLogger(() =>
        {
            traceStarted.TrySetResult();
            releaseTrace.Wait();
        });
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Logger = logger
        });
        await using var resident = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = canonicalId,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        });
        await using var other = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = otherId,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.Yes)
        });
        server.Capabilities = acknowledged ? AutoModeRecoveryCapabilities(true) : null;
        server.FailSessionResume = rejectResume;
        server.ResponseFactory = request =>
        {
            Assert.Equal("sessions.findByPrefix", request.Method);
            Assert.Equal(prefix, request.Params.GetProperty("prefix").GetString());
            return new Dictionary<string, object?> { ["sessionId"] = canonicalId };
        };
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == "sessions.findByPrefix")
            {
                lookupStarted.TrySetResult();
                await releaseLookup.Task.WaitAsync(token);
            }
            if (request.Method == "session.resume")
            {
                resumeStarted.TrySetResult();
                await releaseResume.Task.WaitAsync(token);
            }
        };
        var initialization = client.ResumeSessionAsync(prefix, new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, invocation) =>
            {
                Assert.Equal(canonicalId, invocation.SessionId);
                return Task.FromResult(AutoModeSwitchResponse.YesAlways);
            }
        });
        var timeout = TimeSpan.FromSeconds(5);
        CopilotSession? accepted = null;
        try
        {
            await lookupStarted.Task.WaitAsync(timeout);
            var duringLookup = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = canonicalId }).WaitAsync(timeout);
            Assert.Equal("no", duringLookup.GetProperty("response").GetString());
            await Assert.ThrowsAsync<InvalidOperationException>(() => client.DeleteSessionAsync(canonicalId).WaitAsync(timeout));
            await Assert.ThrowsAsync<InvalidOperationException>(() => client.ResumeSessionAsync(canonicalId, new ResumeSessionConfig()).WaitAsync(timeout));
            await Assert.ThrowsAsync<InvalidOperationException>(() => client.CreateSessionAsync(new SessionConfig { SessionId = prefix.ToLowerInvariant() }).WaitAsync(timeout));
            releaseLookup.TrySetResult();
            await traceStarted.Task.WaitAsync(timeout);
            var beforeResume = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = canonicalId }).WaitAsync(timeout);
            Assert.Equal("no", beforeResume.GetProperty("response").GetString());
            await Assert.ThrowsAsync<InvalidOperationException>(() => client.DeleteSessionAsync(prefix.ToLowerInvariant()).WaitAsync(timeout));
            releaseTrace.Set();
            await resumeStarted.Task.WaitAsync(timeout);
            var resume = Assert.Single(server.Requests, request => request.Method == "session.resume");
            Assert.Equal(canonicalId, resume.Params.GetProperty("sessionId").GetString());
            foreach (var (id, expected) in new[] { (canonicalId.ToUpperInvariant(), "yes_always"), (otherId, "yes") })
            {
                var callback = await server.SendRequestAsync("autoModeSwitch.request",
                    new Dictionary<string, object?> { ["sessionId"] = id }).WaitAsync(timeout);
                Assert.Equal(expected, callback.GetProperty("response").GetString());
            }
            await Assert.ThrowsAsync<InvalidOperationException>(() => client.DeleteSessionAsync(canonicalId).WaitAsync(timeout));
            releaseResume.TrySetResult();
            if (rejectResume)
            {
                await Assert.ThrowsAsync<IOException>(() => initialization.WaitAsync(timeout));
            }
            else if (!acknowledged)
            {
                await Assert.ThrowsAsync<NotSupportedException>(() => initialization.WaitAsync(timeout));
            }
            else
            {
                accepted = await initialization.WaitAsync(timeout);
                Assert.Equal(canonicalId, accepted.SessionId);
                Assert.NotSame(resident, accepted);
            }
            var afterResume = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = canonicalId }).WaitAsync(timeout);
            Assert.Equal(rejectResume ? "no" : "yes_always", afterResume.GetProperty("response").GetString());
            Assert.Single(server.Requests, request => request.Method == "sessions.findByPrefix");
            Assert.DoesNotContain(server.Requests, request => request.Method is "session.detach" or "session.delete");
        }
        finally
        {
            releaseLookup.TrySetResult();
            releaseTrace.Set();
            releaseResume.TrySetResult();
            await Record.ExceptionAsync(() => initialization.WaitAsync(timeout));
            if (accepted is not null) await accepted.DisposeAsync();
        }
    }

    [Fact]
    public async Task AutoModeRecovery_Cancelled_Uuid_Prefix_Lookup_Leaves_Resident_Owner()
    {
        const string canonicalId = "abcdef12-abcd-4123-8123-123456789abc";
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var resident = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = canonicalId,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        });
        server.ResponseFactory = request =>
        {
            Assert.Equal("sessions.findByPrefix", request.Method);
            return new Dictionary<string, object?> { ["sessionId"] = canonicalId };
        };
        var started = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var release = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == "sessions.findByPrefix")
            {
                started.TrySetResult();
                await release.Task.WaitAsync(token);
            }
        };
        using var cancellation = new CancellationTokenSource();
        var initialization = client.ResumeSessionAsync("ABCDEF12", new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.YesAlways)
        }, cancellation.Token);
        var timeout = TimeSpan.FromSeconds(5);
        try
        {
            await started.Task.WaitAsync(timeout);
            cancellation.Cancel();
            var error = await Assert.ThrowsAnyAsync<OperationCanceledException>(() => initialization.WaitAsync(timeout));
            Assert.Equal(cancellation.Token, error.CancellationToken);
            var callback = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = canonicalId }).WaitAsync(timeout);
            Assert.Equal("no", callback.GetProperty("response").GetString());
            Assert.DoesNotContain(server.Requests, request => request.Method is "session.resume" or "session.detach" or "session.delete");
            await client.DeleteSessionAsync(canonicalId).WaitAsync(timeout);
            Assert.Single(server.Requests, request => request.Method == "session.delete");
        }
        finally
        {
            release.TrySetResult();
            await Record.ExceptionAsync(() => initialization.WaitAsync(timeout));
        }
    }

    private sealed class PrefixLookupLogger(Action onLookupCompleted) : ILogger
    {
        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;
        public bool IsEnabled(LogLevel logLevel) => true;
        public void Log<TState>(LogLevel logLevel, EventId eventId, TState state, Exception? exception, Func<TState, Exception?, string> formatter)
        {
            if (state is IEnumerable<KeyValuePair<string, object?>> fields
                && fields.Any(field => field.Key == "Method" && field.Value is "sessions.findByPrefix")
                && fields.Any(field => field.Key == "Status" && field.Value is "Succeeded"))
            {
                onLookupCompleted();
            }
        }
    }
}
#endif
