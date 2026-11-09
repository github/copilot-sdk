/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Runtime.ExceptionServices;
using GitHub.Copilot.Rpc;
using Microsoft.Extensions.Logging;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    private static Dictionary<string, object?> AutoModeRecoveryCapabilities(bool enabled) => new()
    {
        ["ui"] = new Dictionary<string, object?>
        {
            ["autoModeRecovery"] = enabled,
            ["elicitation"] = true
        }
    };

    [Fact]
    public async Task SessionPrefixLookup_Preserves_Method_Group_Inference()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = request =>
        {
            Assert.Equal("sessions.findByPrefix", request.Method);
            return new Dictionary<string, object?> { ["sessionId"] = "abcdef12-abcd-4123-8123-123456789abc" };
        };
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await client.StartAsync();
        server.ClearRequests();
        var find = client.Rpc.Sessions.FindByPrefixAsync;

        Assert.Equal("abcdef12-abcd-4123-8123-123456789abc", (await find("abcdef12", CancellationToken.None)).SessionId);
        var request = Assert.Single(server.Requests);
        Assert.Equal("abcdef12", request.Params.GetProperty("prefix").GetString());
        Assert.False(request.Params.TryGetProperty("configDir", out _));
        server.ClearRequests();
        var findWithOptions = client.Rpc.Sessions.FindByPrefixWithOptionsAsync;
        Assert.Equal("abcdef12-abcd-4123-8123-123456789abc", (await findWithOptions(
            new SessionsFindByPrefixRequest { Prefix = "abcdef12", ConfigDir = "configured-home" },
            CancellationToken.None)).SessionId);
        request = Assert.Single(server.Requests);
        Assert.Equal("abcdef12", request.Params.GetProperty("prefix").GetString());
        Assert.Equal("configured-home", request.Params.GetProperty("configDir").GetString());
    }

    [Fact]
    public async Task AutoModeRecovery_Direct_Registration_Preserves_CancellationToken_Calls_And_Method_Group_Inference()
    {
        const string method = "session.ui.registerDirectAutoModeSwitchHandler";
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = request =>
        {
            Assert.Equal(method, request.Method);
            var result = new Dictionary<string, object?> { ["handle"] = "direct-handler" };
            if (request.Params.TryGetProperty("autoModeRecovery", out var recovery))
            {
                result["autoModeRecovery"] = recovery.GetBoolean();
            }
            return result;
        };
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        using var cancellation = new CancellationTokenSource();
        var register = session.Rpc.Ui.RegisterDirectAutoModeSwitchHandlerAsync;

        Assert.Equal("direct-handler", (await session.Rpc.Ui.RegisterDirectAutoModeSwitchHandlerAsync()).Handle);
        Assert.Equal("direct-handler", (await session.Rpc.Ui.RegisterDirectAutoModeSwitchHandlerAsync(default)).Handle);
        Assert.Equal("direct-handler", (await session.Rpc.Ui.RegisterDirectAutoModeSwitchHandlerAsync(cancellation.Token)).Handle);
        Assert.Equal("direct-handler", (await session.Rpc.Ui.RegisterDirectAutoModeSwitchHandlerAsync(cancellationToken: cancellation.Token)).Handle);
        Assert.Equal("direct-handler", (await register(cancellation.Token)).Handle);
        Assert.Equal("direct-handler", (await session.Rpc.Ui.RegisterDirectAutoModeSwitchHandlerWithAutoModeRecoveryAsync(request: null, cancellationToken: cancellation.Token)).Handle);

        var requests = server.Requests.Where(request => request.Method == method).ToArray();
        Assert.Equal(6, requests.Length);
        Assert.All(requests, request =>
        {
            Assert.Equal(session.SessionId, request.Params.GetProperty("sessionId").GetString());
            Assert.False(request.Params.TryGetProperty("autoModeRecovery", out _));
        });

        cancellation.Cancel();
        await Assert.ThrowsAnyAsync<OperationCanceledException>(
            () => session.Rpc.Ui.RegisterDirectAutoModeSwitchHandlerAsync(cancellation.Token));
        await Assert.ThrowsAnyAsync<OperationCanceledException>(() => register(cancellation.Token));
        await Assert.ThrowsAnyAsync<OperationCanceledException>(
            () => session.Rpc.Ui.RegisterDirectAutoModeSwitchHandlerWithAutoModeRecoveryAsync(cancellationToken: cancellation.Token));
    }

    [Theory]
    [InlineData(null)]
    [InlineData(false)]
    [InlineData(true)]
    public async Task AutoModeRecovery_Direct_Registration_Forwards_Enrollment(bool? enabled)
    {
        const string method = "session.ui.registerDirectAutoModeSwitchHandler";
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = request =>
        {
            Assert.Equal(method, request.Method);
            var result = new Dictionary<string, object?> { ["handle"] = "direct-handler" };
            if (request.Params.TryGetProperty("autoModeRecovery", out var recovery))
            {
                result["autoModeRecovery"] = recovery.GetBoolean();
            }
            return result;
        };
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());

        var acknowledged = await session.Rpc.Ui.RegisterDirectAutoModeSwitchHandlerWithAutoModeRecoveryAsync(
            new SessionUiRegisterDirectAutoModeSwitchHandlerRequest { AutoModeRecovery = enabled },
            default);

        Assert.Equal("direct-handler", acknowledged.Handle);
        Assert.Equal(enabled, acknowledged.AutoModeRecovery);
        var request = Assert.Single(server.Requests, request => request.Method == method);
        Assert.Equal(session.SessionId, request.Params.GetProperty("sessionId").GetString());
        if (enabled is { } value)
        {
            Assert.Equal(value, request.Params.GetProperty("autoModeRecovery").GetBoolean());
        }
        else
        {
            Assert.False(request.Params.TryGetProperty("autoModeRecovery", out _));
        }
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task AutoModeRecovery_Requires_Handler_Before_Connecting(bool resume)
    {
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri("http://127.0.0.1:1")
        });
        var error = await Assert.ThrowsAsync<ArgumentException>(() => resume
            ? client.ResumeSessionAsync("session-1", new ResumeSessionConfig { AutoModeRecovery = true, OnEvent = _ => { } })
            : client.CreateSessionAsync(new SessionConfig { AutoModeRecovery = true, OnEvent = _ => { } }));
        Assert.Contains("requires OnAutoModeSwitchRequest", error.Message);
    }

    [Theory]
    [InlineData(false, null)]
    [InlineData(false, false)]
    [InlineData(true, null)]
    [InlineData(true, false)]
    public async Task AutoModeRecovery_Unsupported_Enrollment_Detaches(bool resume, bool? acknowledged)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.Capabilities = acknowledged is { } value ? AutoModeRecoveryCapabilities(value) : null;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        Func<AutoModeSwitchRequest, AutoModeSwitchInvocation, Task<AutoModeSwitchResponse>> handler =
            (_, _) => Task.FromResult(AutoModeSwitchResponse.No);
        var error = await Assert.ThrowsAsync<NotSupportedException>(() => resume
            ? client.ResumeSessionAsync("session-1", new ResumeSessionConfig
            {
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = handler
            })
            : client.CreateSessionAsync(new SessionConfig
            {
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = handler
            }));
        Assert.Contains("does not support AutoModeRecovery", error.Message);
        var detach = Assert.Single(server.Requests, r => r.Method == "session.detach");
        Assert.DoesNotContain(server.Requests, request => request.Method == "session.delete");
        var callbackError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("autoModeSwitch.request", new Dictionary<string, object?>
            {
                ["sessionId"] = detach.Params.GetProperty("sessionId").GetString()
            }));
        Assert.Contains("Unknown session", callbackError.Message);
    }

    [Theory]
    [InlineData(false, true)]
    [InlineData(true, true)]
    [InlineData(false, false)]
    [InlineData(true, false)]
    public async Task AutoModeRecovery_Unsupported_Enrollment_Detach_Is_Bounded(bool resume, bool cancelRequest)
    {
        await using var server = await FakeCopilotServer.StartAsync(acceptReconnect: true);
        var logger = new RecoveryCleanupLogger();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Logger = logger
        });
        using var cancellation = new CancellationTokenSource();
        var detachStarted = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseDetach = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == "session.detach")
            {
                detachStarted.TrySetResult(request.Params.GetProperty("sessionId").GetString()!);
                // Keep the peer connected but do not acknowledge cleanup.
                await releaseDetach.Task.WaitAsync(token);
            }
        };
        Func<AutoModeSwitchRequest, AutoModeSwitchInvocation, Task<AutoModeSwitchResponse>> handler =
            (_, _) => Task.FromResult(AutoModeSwitchResponse.No);
        var initialization = resume
            ? client.ResumeSessionAsync("session-1", new ResumeSessionConfig
            {
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = handler
            }, cancellation.Token)
            : client.CreateSessionAsync(new SessionConfig
            {
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = handler
            }, cancellation.Token);
        var rejection = Record.ExceptionAsync(async () => await initialization);
        var timeout = TimeSpan.FromSeconds(5);
        Exception? failure = null;
        try
        {
            var sessionId = await detachStarted.Task.WaitAsync(timeout);
            if (cancelRequest)
            {
                cancellation.Cancel();
                var error = Assert.IsAssignableFrom<OperationCanceledException>(
                    await rejection.WaitAsync(timeout));
                Assert.Equal(cancellation.Token, error.CancellationToken);
                Assert.True(initialization.IsCanceled);
                await Assert.ThrowsAsync<InvalidOperationException>(() =>
                    client.CreateSessionAsync(new SessionConfig { SessionId = sessionId }).WaitAsync(timeout));
                Assert.Contains("Canceled",
                    await logger.CleanupWarning.Task.WaitAsync(TimeSpan.FromSeconds(15)));
            }
            else
            {
                var error = Assert.IsType<AggregateException>(
                    await rejection.WaitAsync(TimeSpan.FromSeconds(15)));
                Assert.Contains(error.InnerExceptions, inner =>
                    inner is NotSupportedException && inner.Message.Contains("does not support AutoModeRecovery"));
                Assert.Contains(error.InnerExceptions, inner => inner is OperationCanceledException);
            }

            releaseDetach.TrySetResult();
            var callbackError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
                server.SendRequestAsync("autoModeSwitch.request", new Dictionary<string, object?>
                {
                    ["sessionId"] = sessionId
                }).WaitAsync(timeout));
            Assert.Contains("disposed object", callbackError.Message);
            await Assert.ThrowsAsync<InvalidOperationException>(() =>
                client.ResumeSessionAsync(sessionId, new ResumeSessionConfig()).WaitAsync(timeout));
            await Assert.ThrowsAsync<InvalidOperationException>(() =>
                client.CreateSessionAsync(new SessionConfig { SessionId = sessionId }).WaitAsync(timeout));
            await using var unrelated = await client.ResumeSessionAsync("unrelated", new ResumeSessionConfig()).WaitAsync(timeout);
            Assert.Equal("unrelated", unrelated.SessionId);
            await unrelated.DisposeAsync();
            await client.StopAsync();
            await using var replacement = await client.ResumeSessionAsync(sessionId, new ResumeSessionConfig()).WaitAsync(timeout);
            Assert.Equal(sessionId, replacement.SessionId);
        }
        catch (Exception error)
        {
            failure = error;
        }
        finally
        {
            releaseDetach.TrySetResult();
            try
            {
                await rejection.WaitAsync(timeout);
            }
            catch (Exception cleanupError)
            {
                failure = failure is null ? cleanupError : new AggregateException(failure, cleanupError);
            }
        }
        if (failure is not null)
        {
            ExceptionDispatchInfo.Capture(failure).Throw();
        }
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task AutoModeRecovery_Rejected_Detach_Fences_Id_Until_Reconnect(bool resume)
    {
        await using var server = await FakeCopilotServer.StartAsync(acceptReconnect: true);
        server.FailSessionDetach = true;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        const string sessionId = "failed-recovery-cleanup";
        Func<AutoModeSwitchRequest, AutoModeSwitchInvocation, Task<AutoModeSwitchResponse>> handler =
            (_, _) => Task.FromResult(AutoModeSwitchResponse.No);
        var failure = await Record.ExceptionAsync(() => resume
            ? client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
            {
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = handler
            })
            : client.CreateSessionAsync(new SessionConfig
            {
                SessionId = sessionId,
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = handler
            }));
        var error = Assert.IsType<AggregateException>(failure);
        Assert.Contains(error.InnerExceptions, inner => inner is NotSupportedException);
        Assert.Contains(error.InnerExceptions, inner => inner.Message.Contains("detach rejected"));
        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            client.ResumeSessionAsync(sessionId, new ResumeSessionConfig()));
        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            client.CreateSessionAsync(new SessionConfig { SessionId = sessionId }));
        Assert.Single(server.Requests, request => request.Method is "session.create" or "session.resume");
        var callbackError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("autoModeSwitch.request", new Dictionary<string, object?> { ["sessionId"] = sessionId }));
        Assert.Contains("disposed object", callbackError.Message);
        await using (var unrelated = await client.ResumeSessionAsync("unrelated", new ResumeSessionConfig()))
        {
            Assert.Equal("unrelated", unrelated.SessionId);
        }
        await client.StopAsync();
        server.FailSessionDetach = false;
        await using var replacement = await client.ResumeSessionAsync(sessionId, new ResumeSessionConfig());
        Assert.Equal(sessionId, replacement.SessionId);
    }

    [Fact]
    public async Task AutoModeRecovery_Failed_Public_Disposal_Does_Not_Fence_Id()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.FailSessionDetach = true;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        var session = await client.CreateSessionAsync(new SessionConfig { SessionId = "public-disposal" });
        await session.DisposeAsync();
        await using var replacement = await client.ResumeSessionAsync(session.SessionId, new ResumeSessionConfig());
        Assert.Equal(session.SessionId, replacement.SessionId);
    }

    [Fact]
    public async Task AutoModeRecovery_Acknowledged_Prefix_Resume_Restores_Resident_After_Interest_Failure()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        const string canonicalId = "abcdef12-abcd-4123-8123-123456789abc";
        await using var previous = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = canonicalId,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        });
        server.ResumedSessionId = canonicalId;
        server.Capabilities = AutoModeRecoveryCapabilities(true);
        server.FailMcpAuthInterestRegistration = true;
        server.ResponseFactory = request =>
        {
            Assert.Equal("sessions.findByPrefix", request.Method);
            return new Dictionary<string, object?> { ["sessionId"] = canonicalId };
        };

        var error = await Assert.ThrowsAsync<IOException>(() => client.ResumeSessionAsync("ABCDEF12",
            new ResumeSessionConfig
            {
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.YesAlways),
                OnMcpAuthRequest = _ => Task.FromResult<McpAuthResult?>(McpAuthResult.Cancel())
            }));
        Assert.Contains("interest registration failed", error.Message);
        var callback = await server.SendRequestAsync("autoModeSwitch.request",
            new Dictionary<string, object?> { ["sessionId"] = canonicalId }).WaitAsync(TimeSpan.FromSeconds(5));
        Assert.Equal("no", callback.GetProperty("response").GetString());
        Assert.DoesNotContain(server.Requests, request => request.Method is "session.detach" or "session.delete");

        server.FailMcpAuthInterestRegistration = false;
        await previous.DisposeAsync();
        await using var retried = await client.ResumeSessionAsync(canonicalId, new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.YesAlways)
        });
        Assert.Equal(canonicalId, retried.SessionId);
    }

    [Theory]
    [InlineData(false, false)]
    [InlineData(false, true)]
    [InlineData(true, false)]
    [InlineData(true, true)]
    public async Task AutoModeRecovery_Delete_Cannot_Release_Owned_Cleanup(bool resume, bool failDetach)
    {
        await using var server = await FakeCopilotServer.StartAsync(acceptReconnect: true);
        server.FailSessionDetach = failDetach;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        const string sessionId = "abcdef12-abcd-4123-8123-123456789abc";
        var started = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var release = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == "session.detach")
            {
                started.TrySetResult();
                await release.Task.WaitAsync(token);
            }
        };
        var initialization = Record.ExceptionAsync(() => resume
            ? client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
            {
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.YesAlways)
            })
            : client.CreateSessionAsync(new SessionConfig
            {
                SessionId = sessionId,
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.YesAlways)
            }));
        var timeout = TimeSpan.FromSeconds(5);
        try
        {
            await started.Task.WaitAsync(timeout);
            await Assert.ThrowsAsync<InvalidOperationException>(() => client.DeleteSessionAsync(sessionId).WaitAsync(timeout));
            Assert.DoesNotContain(server.Requests, request => request.Method == "session.delete");
            var response = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = sessionId }).WaitAsync(timeout);
            Assert.Equal("yes_always", response.GetProperty("response").GetString());
            await Assert.ThrowsAsync<InvalidOperationException>(() =>
                client.ResumeSessionAsync(sessionId, new ResumeSessionConfig()).WaitAsync(timeout));
            release.TrySetResult();
            var error = await initialization.WaitAsync(timeout);
            if (failDetach)
            {
                Assert.IsType<AggregateException>(error);
                await Assert.ThrowsAsync<InvalidOperationException>(() => client.DeleteSessionAsync(sessionId).WaitAsync(timeout));
                await Assert.ThrowsAsync<InvalidOperationException>(() =>
                    client.ResumeSessionAsync(sessionId.ToUpperInvariant(), new ResumeSessionConfig()).WaitAsync(timeout));
                await client.StopAsync();
                server.FailSessionDetach = false;
            }
            else
            {
                Assert.IsType<NotSupportedException>(error);
            }
            await using var replacement = await client.ResumeSessionAsync(sessionId, new ResumeSessionConfig()).WaitAsync(timeout);
            await client.DeleteSessionAsync(sessionId).WaitAsync(timeout);
            Assert.Single(server.Requests, request => request.Method == "session.delete");
        }
        finally
        {
            release.TrySetResult();
            await initialization.WaitAsync(timeout);
        }
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task AutoModeRecovery_Prefix_Resident_Resume_Keeps_Accepted_Callback_Owner(bool acknowledged)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        const string canonicalId = "abcdef12-abcd-4123-8123-123456789abc";
        var previous = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = canonicalId,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        });
        server.ResumedSessionId = canonicalId;
        server.Capabilities = AutoModeRecoveryCapabilities(acknowledged);
        server.ResponseFactory = request =>
        {
            Assert.Equal("sessions.findByPrefix", request.Method);
            return new Dictionary<string, object?> { ["sessionId"] = canonicalId };
        };
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
        CopilotSession? accepted = null;
        var initialization = client.ResumeSessionAsync("ABCDEF12", new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, invocation) =>
            {
                Assert.Equal(canonicalId, invocation.SessionId);
                return Task.FromResult(AutoModeSwitchResponse.YesAlways);
            }
        });
        var timeout = TimeSpan.FromSeconds(5);
        try
        {
            await started.Task.WaitAsync(timeout);
            var before = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = canonicalId }).WaitAsync(timeout);
            Assert.Equal("yes_always", before.GetProperty("response").GetString());
            await Assert.ThrowsAsync<InvalidOperationException>(() =>
                client.CreateSessionAsync(new SessionConfig { SessionId = canonicalId }).WaitAsync(timeout));
            await Assert.ThrowsAsync<InvalidOperationException>(() => client.DeleteSessionAsync(canonicalId).WaitAsync(timeout));
            release.TrySetResult();
            if (acknowledged)
            {
                accepted = await initialization.WaitAsync(timeout);
                Assert.Equal(canonicalId, accepted.SessionId);
                Assert.NotSame(previous, accepted);
            }
            else
            {
                await Assert.ThrowsAsync<NotSupportedException>(() => initialization.WaitAsync(timeout));
            }
            var after = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = canonicalId }).WaitAsync(timeout);
            Assert.Equal("yes_always", after.GetProperty("response").GetString());
            Assert.DoesNotContain(server.Requests, request => request.Method is "session.detach" or "session.delete");
        }
        finally
        {
            release.TrySetResult();
            await Record.ExceptionAsync(() => initialization.WaitAsync(timeout));
            if (accepted is not null) await accepted.DisposeAsync();
        }
    }

    [Theory]
    [InlineData(false, false)]
    [InlineData(true, false)]
    [InlineData(false, true)]
    [InlineData(true, true)]
    public async Task AutoModeRecovery_Prefix_Cold_Resume_Detaches_Canonical_Id(bool cancelCaller, bool failDetach)
    {
        await using var server = await FakeCopilotServer.StartAsync(acceptReconnect: true);
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        const string prefix = "ABCDEF12-AB";
        const string canonicalId = "abcdef12-abcd-4123-8123-123456789abc";
        server.ResumedSessionId = canonicalId;
        server.ResponseFactory = request =>
        {
            Assert.Equal("sessions.findByPrefix", request.Method);
            Assert.Equal(prefix, request.Params.GetProperty("prefix").GetString());
            return new Dictionary<string, object?> { ["sessionId"] = canonicalId };
        };
        server.FailSessionDetach = failDetach;
        var resumeStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseResume = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var detachStarted = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseDetach = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == "session.resume")
            {
                resumeStarted.TrySetResult();
                await releaseResume.Task.WaitAsync(token);
            }
            if (request.Method == "session.detach")
            {
                detachStarted.TrySetResult(request.Params.GetProperty("sessionId").GetString()!);
                await releaseDetach.Task.WaitAsync(token);
            }
        };
        using var cancellation = new CancellationTokenSource();
        var initialization = client.ResumeSessionAsync(prefix, new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.YesAlways)
        }, cancellation.Token);
        var timeout = TimeSpan.FromSeconds(5);
        try
        {
            await resumeStarted.Task.WaitAsync(timeout);
            Assert.Equal(canonicalId, Assert.Single(server.Requests, request => request.Method == "session.resume").Params.GetProperty("sessionId").GetString());
            var before = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = canonicalId }).WaitAsync(timeout);
            Assert.Equal("yes_always", before.GetProperty("response").GetString());
            await Assert.ThrowsAsync<InvalidOperationException>(() =>
                client.CreateSessionAsync(new SessionConfig { SessionId = canonicalId }).WaitAsync(timeout));
            await Assert.ThrowsAsync<InvalidOperationException>(() =>
                client.ResumeSessionAsync("abcdef1", new ResumeSessionConfig()).WaitAsync(timeout));
            await Assert.ThrowsAsync<InvalidOperationException>(() => client.DeleteSessionAsync(canonicalId).WaitAsync(timeout));
            releaseResume.TrySetResult();
            Assert.Equal(canonicalId, await detachStarted.Task.WaitAsync(timeout));
            if (cancelCaller)
            {
                cancellation.Cancel();
                await Assert.ThrowsAnyAsync<OperationCanceledException>(() => initialization.WaitAsync(timeout));
            }
            await Assert.ThrowsAsync<InvalidOperationException>(() =>
                client.ResumeSessionAsync(canonicalId.ToUpperInvariant(), new ResumeSessionConfig()).WaitAsync(timeout));
            await Assert.ThrowsAsync<InvalidOperationException>(() => client.DeleteSessionAsync("ABCDEF1").WaitAsync(timeout));
            var during = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = canonicalId }).WaitAsync(timeout);
            Assert.Equal("yes_always", during.GetProperty("response").GetString());
            releaseDetach.TrySetResult();
            if (!cancelCaller)
            {
                if (failDetach)
                {
                    var error = await Assert.ThrowsAsync<AggregateException>(() => initialization.WaitAsync(timeout));
                    Assert.Contains(error.InnerExceptions, inner => inner is NotSupportedException);
                    Assert.Contains(error.InnerExceptions, inner => inner.Message.Contains("detach rejected"));
                }
                else
                {
                    await Assert.ThrowsAsync<NotSupportedException>(() => initialization.WaitAsync(timeout));
                }
            }
            using var retirement = new CancellationTokenSource(timeout);
            while (true)
            {
                retirement.Token.ThrowIfCancellationRequested();
                try
                {
                    await server.SendRequestAsync("autoModeSwitch.request",
                        new Dictionary<string, object?> { ["sessionId"] = canonicalId }).WaitAsync(retirement.Token);
                }
                catch (InvalidOperationException error)
                {
                    Assert.True(error.Message.Contains("Unknown session", StringComparison.OrdinalIgnoreCase)
                        || error.Message.Contains("disposed object", StringComparison.OrdinalIgnoreCase), error.Message);
                    break;
                }
                await Task.Yield();
            }
            Assert.DoesNotContain(server.Requests, request => request.Method == "session.delete");
            Assert.Single(server.Requests, request => request.Method == "session.detach");
            if (failDetach)
            {
                await Assert.ThrowsAsync<InvalidOperationException>(() => client.DeleteSessionAsync("ABCDEF1").WaitAsync(timeout));
                await Assert.ThrowsAsync<InvalidOperationException>(() =>
                    client.CreateSessionAsync(new SessionConfig { SessionId = canonicalId.ToUpperInvariant() }).WaitAsync(timeout));
                await Assert.ThrowsAsync<InvalidOperationException>(() =>
                    client.ResumeSessionAsync(prefix, new ResumeSessionConfig()).WaitAsync(timeout));
                await client.StopAsync();
                server.FailSessionDetach = false;
                await using var replacement = await client.ResumeSessionAsync(prefix, new ResumeSessionConfig()).WaitAsync(timeout);
                Assert.Equal(canonicalId, replacement.SessionId);
            }
        }
        finally
        {
            releaseResume.TrySetResult();
            releaseDetach.TrySetResult();
            await Record.ExceptionAsync(() => initialization.WaitAsync(timeout));
        }
    }

    [Fact]
    public async Task AutoModeRecovery_Ambiguous_Prefix_Leaves_Resident_Callbacks_Routed()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        const string prefix = "ambiguous-session-";
        const string firstId = prefix + "a";
        const string secondId = prefix + "b";
        await using var first = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = firstId,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.Yes)
        });
        await using var second = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = secondId,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.YesAlways)
        });
        var started = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var release = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var provisionalCalls = 0;
        server.FailSessionResume = true;
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == "session.resume")
            {
                started.TrySetResult();
                await release.Task.WaitAsync(token);
            }
        };
        var resuming = client.ResumeSessionAsync(prefix, new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) =>
            {
                Interlocked.Increment(ref provisionalCalls);
                return Task.FromResult(AutoModeSwitchResponse.No);
            }
        });
        var timeout = TimeSpan.FromSeconds(5);
        try
        {
            await started.Task.WaitAsync(timeout);
            foreach (var (id, expected) in new[] { (firstId, "yes"), (secondId, "yes_always") })
            {
                var response = await server.SendRequestAsync("autoModeSwitch.request",
                    new Dictionary<string, object?> { ["sessionId"] = id }).WaitAsync(timeout);
                Assert.Equal(expected, response.GetProperty("response").GetString());
            }
            Assert.Equal(0, provisionalCalls);
        }
        finally
        {
            release.TrySetResult();
            await Record.ExceptionAsync(() => resuming.WaitAsync(timeout));
        }
        await Assert.ThrowsAsync<IOException>(() => resuming.WaitAsync(timeout));
        Assert.DoesNotContain(server.Requests, request => request.Method is "session.detach" or "session.delete");
    }

    [Fact]
    public async Task AutoModeRecovery_Rejected_Prefix_Resume_Restores_Resident_Callback_Owner()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        const string canonicalId = "abcdef12-abcd-4123-8123-123456789abc";
        await using var previous = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = canonicalId,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        });
        server.ResumedSessionId = canonicalId;
        server.ResponseFactory = request =>
        {
            Assert.Equal("sessions.findByPrefix", request.Method);
            return new Dictionary<string, object?> { ["sessionId"] = canonicalId };
        };
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
        server.FailSessionResume = true;
        var initialization = client.ResumeSessionAsync("ABCDEF12", new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.YesAlways)
        });
        var timeout = TimeSpan.FromSeconds(5);
        try
        {
            await started.Task.WaitAsync(timeout);
            var before = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = canonicalId }).WaitAsync(timeout);
            Assert.Equal("yes_always", before.GetProperty("response").GetString());
            release.TrySetResult();
            await Assert.ThrowsAsync<IOException>(() => initialization.WaitAsync(timeout));
            var after = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = canonicalId }).WaitAsync(timeout);
            Assert.Equal("no", after.GetProperty("response").GetString());
            Assert.DoesNotContain(server.Requests, request => request.Method is "session.detach" or "session.delete");
        }
        finally
        {
            release.TrySetResult();
            await Record.ExceptionAsync(() => initialization.WaitAsync(timeout));
        }
    }

    [Fact]
    public async Task AutoModeRecovery_Prefix_Admission_Checks_Every_Overlapping_Id_And_Callback_Reentry()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        var firstStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var secondStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var release = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == "session.detach")
            {
                var sessionId = request.Params.GetProperty("sessionId").GetString();
                if (sessionId == "abcdef12-abcd-4123-8123-123456789abc") firstStarted.TrySetResult();
                if (sessionId == "abcdef12-abcd-4123-8123-123456789def") secondStarted.TrySetResult();
                await release.Task.WaitAsync(token);
            }
        };
        var timeout = TimeSpan.FromSeconds(5);
        var first = client.ResumeSessionAsync("abcdef12-abcd-4123-8123-123456789abc", new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = async (_, _) =>
            {
                await Assert.ThrowsAsync<InvalidOperationException>(() =>
                    client.DeleteSessionAsync("ABCDEF12").WaitAsync(timeout));
                await Assert.ThrowsAsync<InvalidOperationException>(() =>
                    client.ResumeSessionAsync("ABCDEF12", new ResumeSessionConfig()).WaitAsync(timeout));
                await Assert.ThrowsAsync<InvalidOperationException>(() =>
                    client.CreateSessionAsync(new SessionConfig { SessionId = "ABCDEF12" }).WaitAsync(timeout));
                return AutoModeSwitchResponse.YesAlways;
            }
        });
        var second = client.ResumeSessionAsync("abcdef12-abcd-4123-8123-123456789def", new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        });
        try
        {
            await Task.WhenAll(firstStarted.Task, secondStarted.Task).WaitAsync(timeout);
            var response = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = "abcdef12-abcd-4123-8123-123456789abc" }).WaitAsync(timeout);
            Assert.Equal("yes_always", response.GetProperty("response").GetString());
            await Assert.ThrowsAsync<InvalidOperationException>(() => client.DeleteSessionAsync("ABCDEF12").WaitAsync(timeout));
            await using var unrelated = await client.CreateSessionAsync(new SessionConfig { SessionId = "unrelated" }).WaitAsync(timeout);
            await client.DeleteSessionAsync(unrelated.SessionId).WaitAsync(timeout);
            Assert.Equal(2, server.Requests.Count(request => request.Method == "session.resume"));
            release.TrySetResult();
            await Assert.ThrowsAsync<NotSupportedException>(() => first.WaitAsync(timeout));
            await Assert.ThrowsAsync<NotSupportedException>(() => second.WaitAsync(timeout));
        }
        finally
        {
            release.TrySetResult();
            await Record.ExceptionAsync(() => first.WaitAsync(timeout));
            await Record.ExceptionAsync(() => second.WaitAsync(timeout));
        }
    }

    [Fact]
    public async Task AutoModeRecovery_Prefix_Initialization_Cannot_Overtake_Public_Delete()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var previous = await client.CreateSessionAsync(new SessionConfig { SessionId = "abcdef12-abcd-4123-8123-123456789abc" });
        var started = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var release = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == "session.delete")
            {
                started.TrySetResult();
                await release.Task.WaitAsync(token);
            }
        };
        var deletion = client.DeleteSessionAsync(previous.SessionId);
        var timeout = TimeSpan.FromSeconds(5);
        try
        {
            await started.Task.WaitAsync(timeout);
            await Assert.ThrowsAsync<InvalidOperationException>(() => client.ResumeSessionAsync("ABCDEF12", new ResumeSessionConfig
            {
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
            }).WaitAsync(timeout));
            Assert.DoesNotContain(server.Requests, request => request.Method == "session.resume");
            release.TrySetResult();
            await deletion.WaitAsync(timeout);
        }
        finally
        {
            release.TrySetResult();
            await deletion.WaitAsync(timeout);
        }
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task AutoModeRecovery_Prefix_Unsupported_Acknowledgment_Uses_Canonical_Ownership(bool resident)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        const string canonicalId = "abcdef12-abcd-4123-8123-123456789abc";
        if (resident)
        {
            _ = await client.CreateSessionAsync(new SessionConfig { SessionId = canonicalId });
        }
        server.ResumedSessionId = canonicalId;
        server.ResponseFactory = request =>
        {
            Assert.Equal("sessions.findByPrefix", request.Method);
            return new Dictionary<string, object?> { ["sessionId"] = canonicalId };
        };
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
        var initialization = client.ResumeSessionAsync("ABCDEF12", new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.YesAlways)
        });
        var timeout = TimeSpan.FromSeconds(5);
        try
        {
            await started.Task.WaitAsync(timeout);
            release.TrySetResult();
            await Assert.ThrowsAsync<NotSupportedException>(() => initialization.WaitAsync(timeout));
            Assert.DoesNotContain(server.Requests, request => request.Method == "session.delete");
            if (resident)
            {
                Assert.DoesNotContain(server.Requests, request => request.Method == "session.detach");
            }
            else
            {
                var detach = Assert.Single(server.Requests, request => request.Method == "session.detach");
                Assert.Equal(canonicalId, detach.Params.GetProperty("sessionId").GetString());
            }
        }
        finally
        {
            release.TrySetResult();
            await Record.ExceptionAsync(() => initialization.WaitAsync(timeout));
        }
    }

    [Theory]
    [InlineData(false, false)]
    [InlineData(true, false)]
    [InlineData(false, true)]
    [InlineData(true, true)]
    public async Task AutoModeRecovery_Cancelled_Enrollment_Keeps_Callbacks_Until_Cleanup_Settles(bool resume, bool cleanupFailure)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.FailSessionDetach = cleanupFailure;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        using var cancellation = new CancellationTokenSource();
        var started = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var release = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == "session.detach")
            {
                started.TrySetResult();
                await release.Task.WaitAsync(token);
            }
        };
        const string sessionId = "cancelled-recovery-callbacks";
        Func<AutoModeSwitchRequest, AutoModeSwitchInvocation, Task<AutoModeSwitchResponse>> handler =
            (_, _) => Task.FromResult(AutoModeSwitchResponse.YesAlways);
        var hooks = new SessionHooks
        {
            OnSessionEnd = (_, invocation) => Task.FromResult<SessionEndHookOutput?>(new SessionEndHookOutput
            {
                SessionSummary = "cleanup for " + invocation.SessionId
            })
        };
        var creation = resume
            ? client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
            {
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = handler,
                Hooks = hooks
            }, cancellation.Token)
            : client.CreateSessionAsync(new SessionConfig
            {
                SessionId = sessionId,
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = handler,
                Hooks = hooks
            }, cancellation.Token);
        var timeout = TimeSpan.FromSeconds(5);
        try
        {
            await started.Task.WaitAsync(timeout);
            cancellation.Cancel();
            await Assert.ThrowsAnyAsync<OperationCanceledException>(() => creation.WaitAsync(timeout));
            var response = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = sessionId }).WaitAsync(timeout);
            Assert.Equal("yes_always", response.GetProperty("response").GetString());
            var hookRequest = new Dictionary<string, object?>
            {
                ["sessionId"] = sessionId,
                ["hookType"] = "sessionEnd",
                ["input"] = new Dictionary<string, object?>
                {
                    ["sessionId"] = sessionId,
                    ["timestamp"] = 1_730_000_000_000L,
                    ["cwd"] = "workdir",
                    ["reason"] = "error"
                }
            };
            var hookResponse = await server.SendRequestAsync("hooks.invoke", hookRequest).WaitAsync(timeout);
            Assert.Equal("cleanup for " + sessionId, hookResponse.GetProperty("output").GetProperty("sessionSummary").GetString());
            release.TrySetResult();
            using var cleanupWait = new CancellationTokenSource(timeout);
            async Task WaitForCallbackRetirementAsync()
            {
                while (true)
                {
                    cleanupWait.Token.ThrowIfCancellationRequested();
                    try
                    {
                        await server.SendRequestAsync("autoModeSwitch.request",
                            new Dictionary<string, object?> { ["sessionId"] = sessionId }).WaitAsync(cleanupWait.Token);
                    }
                    catch (InvalidOperationException error)
                    {
                        Assert.True(error.Message.Contains("disposed object", StringComparison.OrdinalIgnoreCase)
                            || error.Message.Contains("unknown session", StringComparison.OrdinalIgnoreCase),
                            error.Message);
                        return;
                    }
                    await Task.Yield();
                }
            }
            await WaitForCallbackRetirementAsync();
            await Assert.ThrowsAsync<InvalidOperationException>(() =>
                server.SendRequestAsync("hooks.invoke", hookRequest).WaitAsync(timeout));
            if (cleanupFailure)
            {
                await Assert.ThrowsAsync<InvalidOperationException>(() =>
                    client.ResumeSessionAsync(sessionId, new ResumeSessionConfig()).WaitAsync(timeout));
            }
        }
        finally
        {
            release.TrySetResult();
            await Record.ExceptionAsync(() => creation.WaitAsync(timeout));
        }
    }

    [Fact]
    public async Task AutoModeRecovery_Pending_Cleanup_Does_Not_Block_Other_Ids()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.IgnoreDetach();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        const string sessionId = "pending-recovery-cleanup";
        var creation = Record.ExceptionAsync(() => client.CreateSessionAsync(new SessionConfig
        {
            SessionId = sessionId,
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        }));
        CopilotSession? unrelated = null;
        var timeout = TimeSpan.FromSeconds(5);
        try
        {
            await server.DestroyStarted.WaitAsync(timeout);
            await Assert.ThrowsAsync<InvalidOperationException>(() =>
                client.ResumeSessionAsync(sessionId, new ResumeSessionConfig()).WaitAsync(timeout));
            unrelated = await client.ResumeSessionAsync("unrelated", new ResumeSessionConfig()).WaitAsync(timeout);
            Assert.Equal("unrelated", unrelated.SessionId);
            Assert.False(creation.IsCompleted);
        }
        finally
        {
            await client.ForceStopAsync();
            if (unrelated is not null) await unrelated.DisposeAsync();
            await creation.WaitAsync(timeout);
        }
        var error = Assert.IsType<AggregateException>(await creation);
        Assert.Contains(error.InnerExceptions, inner => inner is NotSupportedException);
        Assert.Contains(error.InnerExceptions, inner => inner is IOException or ObjectDisposedException);
    }

    [Fact]
    public async Task AutoModeRecovery_Rejected_Resume_Releases_Id_Without_Detach_Or_Delete()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.FailSessionResume = true;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        const string sessionId = "rejected-resume";
        var error = await Assert.ThrowsAsync<IOException>(() => client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        }));
        Assert.Contains("session resume failed", error.Message);
        Assert.DoesNotContain(server.Requests, request => request.Method is "session.detach" or "session.delete");
        server.FailSessionResume = false;
        await using var replacement = await client.ResumeSessionAsync(sessionId, new ResumeSessionConfig());
        Assert.Equal(sessionId, replacement.SessionId);
    }

    [Fact]
    public async Task AutoModeRecovery_Unsupported_Cloud_Create_Preserves_Caller_Assigned_Id()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        const string sessionId = "caller-assigned-cloud";
        await Assert.ThrowsAsync<NotSupportedException>(() => client.CreateSessionAsync(new SessionConfig
        {
            SessionId = sessionId,
            Cloud = new CloudSessionOptions
            {
                Repository = new CloudSessionRepository { Owner = "github", Name = "copilot-sdk", Branch = "main" }
            },
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        }));
        var detach = Assert.Single(server.Requests, request => request.Method == "session.detach");
        Assert.Equal(sessionId, detach.Params.GetProperty("sessionId").GetString());
        Assert.DoesNotContain(server.Requests, request => request.Method == "session.delete");
        await using var replacement = await client.ResumeSessionAsync(sessionId, new ResumeSessionConfig());
        Assert.Equal(sessionId, replacement.SessionId);
    }

    [Theory]
    [InlineData(false, false)]
    [InlineData(true, false)]
    [InlineData(false, true)]
    [InlineData(true, true)]
    public async Task AutoModeRecovery_Cancelled_Cloud_Cleanup_Owns_Detach_And_Delete(bool failDelete, bool cancelDuringDelete)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.FailSessionDelete = failDelete;
        var logger = new RecoveryCleanupLogger();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Logger = logger
        });
        using var cancellation = new CancellationTokenSource();
        var detachStarted = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var deleteStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseDetach = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseDelete = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == "session.detach")
            {
                detachStarted.TrySetResult(request.Params.GetProperty("sessionId").GetString()!);
                await releaseDetach.Task.WaitAsync(token);
            }
            if (request.Method == "session.delete")
            {
                deleteStarted.TrySetResult();
                await releaseDelete.Task.WaitAsync(token);
            }
        };
        var creation = client.CreateSessionAsync(new SessionConfig
        {
            Cloud = new CloudSessionOptions
            {
                Repository = new CloudSessionRepository { Owner = "github", Name = "copilot-sdk", Branch = "main" }
            },
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.YesAlways)
        }, cancellation.Token);
        var timeout = TimeSpan.FromSeconds(5);
        try
        {
            var sessionId = await detachStarted.Task.WaitAsync(timeout);
            if (cancelDuringDelete)
            {
                releaseDetach.TrySetResult();
                await deleteStarted.Task.WaitAsync(timeout);
            }
            cancellation.Cancel();
            var cancellationError = await Assert.ThrowsAnyAsync<OperationCanceledException>(() => creation.WaitAsync(timeout));
            Assert.Equal(cancellation.Token, cancellationError.CancellationToken);
            if (!cancelDuringDelete)
            {
                Assert.DoesNotContain(server.Requests, request => request.Method == "session.delete");
            }
            releaseDetach.TrySetResult();
            await deleteStarted.Task.WaitAsync(timeout);
            var callback = await server.SendRequestAsync("autoModeSwitch.request",
                new Dictionary<string, object?> { ["sessionId"] = sessionId }).WaitAsync(timeout);
            Assert.Equal("yes_always", callback.GetProperty("response").GetString());
            await Assert.ThrowsAsync<InvalidOperationException>(() =>
                client.ResumeSessionAsync(sessionId, new ResumeSessionConfig()).WaitAsync(timeout));
            releaseDelete.TrySetResult();
            if (failDelete)
            {
                var cleanupError = await logger.CleanupError.Task.WaitAsync(timeout);
                Assert.Contains("Failed to delete session", cleanupError.ToString());
                await Assert.ThrowsAsync<InvalidOperationException>(() =>
                    client.ResumeSessionAsync(sessionId, new ResumeSessionConfig()).WaitAsync(timeout));
            }
            using var settlement = new CancellationTokenSource(timeout);
            while (true)
            {
                try
                {
                    await server.SendRequestAsync("autoModeSwitch.request",
                        new Dictionary<string, object?> { ["sessionId"] = sessionId }).WaitAsync(settlement.Token);
                }
                catch (InvalidOperationException error)
                {
                    Assert.True(error.Message.Contains("disposed object", StringComparison.OrdinalIgnoreCase)
                        || error.Message.Contains("unknown session", StringComparison.OrdinalIgnoreCase), error.Message);
                    if (!failDelete)
                    {
                        // Callback retirement precedes release of the cleanup reservation.
                        try
                        {
                            await using var replacement = await client.ResumeSessionAsync(sessionId, new ResumeSessionConfig()).WaitAsync(settlement.Token);
                        }
                        catch (InvalidOperationException pending) when (pending.Message ==
                            $"Session '{sessionId}' has a pending operation or rejected AutoModeRecovery cleanup.")
                        {
                            continue;
                        }
                    }
                    break;
                }
            }
            Assert.True(creation.IsCanceled);
            Assert.Single(server.Requests, request => request.Method == "session.delete");
        }
        finally
        {
            releaseDetach.TrySetResult();
            releaseDelete.TrySetResult();
            await Record.ExceptionAsync(() => creation.WaitAsync(timeout));
        }
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task AutoModeRecovery_Unsupported_Cloud_Create_Deletes_Assigned_Id(bool failDelete)
    {
        await using var server = await FakeCopilotServer.StartAsync(acceptReconnect: true);
        server.FailSessionDelete = failDelete;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        var deleteStarted = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseDelete = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == "session.delete")
            {
                deleteStarted.TrySetResult(request.Params.GetProperty("sessionId").GetString()!);
                await releaseDelete.Task.WaitAsync(token);
            }
        };
        var creation = Record.ExceptionAsync(() => client.CreateSessionAsync(new SessionConfig
        {
            Cloud = new CloudSessionOptions
            {
                Repository = new CloudSessionRepository { Owner = "github", Name = "copilot-sdk", Branch = "main" }
            },
            AutoModeRecovery = true,
            OnAutoModeSwitchRequest = (_, _) => Task.FromResult(AutoModeSwitchResponse.No)
        }));
        var timeout = TimeSpan.FromSeconds(5);
        string sessionId;
        try
        {
            sessionId = await deleteStarted.Task.WaitAsync(timeout);
            await Assert.ThrowsAsync<InvalidOperationException>(() =>
                client.ResumeSessionAsync(sessionId, new ResumeSessionConfig()).WaitAsync(timeout));
            await Assert.ThrowsAsync<InvalidOperationException>(() =>
                client.CreateSessionAsync(new SessionConfig { SessionId = sessionId }).WaitAsync(timeout));
            Assert.Single(server.Requests, request => request.Method is "session.create" or "session.resume");
        }
        finally
        {
            releaseDelete.TrySetResult();
            await creation.WaitAsync(timeout);
        }
        var failure = await creation;
        if (failDelete)
        {
            var error = Assert.IsType<AggregateException>(failure);
            Assert.Contains(error.InnerExceptions, inner => inner is NotSupportedException);
            Assert.Contains(error.InnerExceptions, inner => inner.Message.Contains("Failed to delete session"));
        }
        else
        {
            Assert.IsType<NotSupportedException>(failure);
        }
        var create = Assert.Single(server.Requests, request => request.Method == "session.create");
        Assert.False(create.Params.TryGetProperty("sessionId", out _));
        var detach = Assert.Single(server.Requests, request => request.Method == "session.detach");
        var delete = Assert.Single(server.Requests, request => request.Method == "session.delete");
        Assert.NotNull(server.LastSessionId);
        Assert.Equal(server.LastSessionId, detach.Params.GetProperty("sessionId").GetString());
        Assert.Equal(server.LastSessionId, delete.Params.GetProperty("sessionId").GetString());
        if (failDelete)
        {
            await Assert.ThrowsAsync<InvalidOperationException>(() =>
                client.ResumeSessionAsync(sessionId, new ResumeSessionConfig()).WaitAsync(timeout));
            await client.StopAsync();
        }
        await using var replacement = await client.ResumeSessionAsync(sessionId, new ResumeSessionConfig()).WaitAsync(timeout);
        Assert.Equal(sessionId, replacement.SessionId);
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task AutoModeRecovery_Cancellation_Before_Cleanup_Still_Sends_Detach(bool resume)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await client.StartAsync();
        using var cancellation = new CancellationTokenSource();
        using var releaseScheduler = new ManualResetEventSlim();
        var scheduler = new ConcurrentExclusiveSchedulerPair(TaskScheduler.Default, maxConcurrencyLevel: 1);
        var factory = new TaskFactory(CancellationToken.None, TaskCreationOptions.None,
            TaskContinuationOptions.None, scheduler.ExclusiveScheduler);
        var requestStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseResponse = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var responseSent = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var schedulerBlocked = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var detached = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseDetach = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var method = resume ? "session.resume" : "session.create";
        const string sessionId = "cancel-before-recovery-cleanup";
        server.BeforeResponseAsync = async (request, token) =>
        {
            if (request.Method == method)
            {
                requestStarted.TrySetResult();
                await releaseResponse.Task.WaitAsync(token);
            }
            if (request.Method == "session.detach")
            {
                detached.TrySetResult();
                await releaseDetach.Task.WaitAsync(token);
            }
        };
        server.AfterResponseAsync = (request, _) =>
        {
            if (request.Method == method) responseSent.TrySetResult();
            return Task.CompletedTask;
        };
        Func<AutoModeSwitchRequest, AutoModeSwitchInvocation, Task<AutoModeSwitchResponse>> handler =
            (_, _) => Task.FromResult(AutoModeSwitchResponse.No);
        var initialization = factory.StartNew(() => resume
            ? client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
            {
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = handler
            }, cancellation.Token)
            : client.CreateSessionAsync(new SessionConfig
            {
                SessionId = sessionId,
                AutoModeRecovery = true,
                OnAutoModeSwitchRequest = handler
            }, cancellation.Token)).Unwrap();
        var rejected = Record.ExceptionAsync(async () => await initialization);
        Task? blocker = null;
        var timeout = TimeSpan.FromSeconds(5);
        Exception? failure = null;
        try
        {
            await requestStarted.Task.WaitAsync(timeout);
            blocker = factory.StartNew(() =>
            {
                schedulerBlocked.TrySetResult();
                releaseScheduler.Wait();
            });
            await schedulerBlocked.Task.WaitAsync(timeout);
            releaseResponse.TrySetResult();
            await responseSent.Task.WaitAsync(timeout);
            // A callback after the response proves the RPC reader consumed it
            // while the initialization continuation is still held.
            await server.SendRequestAsync("autoModeSwitch.request", new Dictionary<string, object?>
            {
                ["sessionId"] = sessionId
            }).WaitAsync(timeout);
            cancellation.Cancel();
            releaseScheduler.Set();
            var enrollmentError = Assert.IsAssignableFrom<OperationCanceledException>(
                await rejected.WaitAsync(timeout));
            Assert.Equal(cancellation.Token, enrollmentError.CancellationToken);
            Assert.True(initialization.IsCanceled);
            await detached.Task.WaitAsync(timeout);
        }
        catch (Exception error)
        {
            failure = error;
        }
        finally
        {
            releaseResponse.TrySetResult();
            releaseDetach.TrySetResult();
            releaseScheduler.Set();
            try
            {
                if (blocker is not null) await blocker.WaitAsync(timeout);
                await rejected.WaitAsync(timeout);
                // Channel closure can still queue its final continuation; an idle pair owns no thread.
            }
            catch (Exception cleanupError)
            {
                failure = failure is null ? cleanupError : new AggregateException(failure, cleanupError);
            }
        }
        if (failure is not null) ExceptionDispatchInfo.Capture(failure).Throw();
    }

    private sealed class RecoveryCleanupLogger : ILogger
    {
        public TaskCompletionSource<string> CleanupWarning { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public TaskCompletionSource<Exception> CleanupError { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;
        public bool IsEnabled(LogLevel logLevel) => true;
        public void Log<TState>(LogLevel logLevel, EventId eventId, TState state, Exception? exception, Func<TState, Exception?, string> formatter)
        {
            var message = formatter(state, exception);
            if (logLevel == LogLevel.Warning && message.StartsWith("Failed to detach unsupported AutoModeRecovery session", StringComparison.Ordinal))
            {
                CleanupWarning.TrySetResult(message);
                if (exception is not null) CleanupError.TrySetResult(exception);
            }
        }
    }

    [Theory]
    [InlineData(false, false)]
    [InlineData(false, true)]
    [InlineData(true, false)]
    [InlineData(true, true)]
    public async Task AutoModeRecovery_Projects_Enrollment_Context_And_Capabilities(bool resume, bool enabled)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.Capabilities = enabled ? AutoModeRecoveryCapabilities(true) : null;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        AutoModeSwitchRequest? received = null;
        string? receivedSessionId = null;
        Func<AutoModeSwitchRequest, AutoModeSwitchInvocation, Task<AutoModeSwitchResponse>> handler =
            (request, invocation) =>
            {
                received = request;
                receivedSessionId = invocation.SessionId;
                return Task.FromResult(enabled ? AutoModeSwitchResponse.Yes : AutoModeSwitchResponse.YesAlways);
            };
        await using var session = resume
            ? await client.ResumeSessionAsync("session-1", new ResumeSessionConfig
            {
                AutoModeRecovery = enabled,
                OnAutoModeSwitchRequest = handler
            }.Clone())
            : await client.CreateSessionAsync(new SessionConfig
            {
                AutoModeRecovery = enabled,
                OnAutoModeSwitchRequest = handler
            }.Clone());
        var request = Assert.Single(server.Requests, r => r.Method == (resume ? "session.resume" : "session.create"));
        Assert.True(request.Params.GetProperty("requestAutoModeSwitch").GetBoolean());
        Assert.Equal(enabled, request.Params.TryGetProperty("requestAutoModeRecovery", out var flag) && flag.GetBoolean());
        var offer = new Dictionary<string, object?>
        {
            ["sessionId"] = session.SessionId,
            ["errorCode"] = enabled ? "model_overloaded" : "user_weekly_rate_limited",
            ["retryAfterSeconds"] = 3.5
        };
        if (enabled)
        {
            offer["autoModeRecovery"] = new Dictionary<string, object?>
            {
                ["requestId"] = "offer-1",
                ["originalModel"] = "explicit-model",
                ["allowAlways"] = false,
                ["interactionId"] = "interaction-1",
                ["originatingMessageId"] = "user-1"
            };
        }
        var reply = await server.SendRequestAsync("autoModeSwitch.request", offer);
        Assert.Equal(enabled ? "yes" : "yes_always", reply.GetProperty("response").GetString());
        Assert.NotNull(received);
        Assert.Equal(session.SessionId, receivedSessionId);
        Assert.Equal(offer["errorCode"], received.ErrorCode);
        Assert.Equal(3.5, received.RetryAfterSeconds);
        if (enabled)
        {
            Assert.True(session.Capabilities.Ui?.AutoModeRecovery);
            Assert.NotNull(received.AutoModeRecovery);
            Assert.Equal("offer-1", received.AutoModeRecovery.RequestId);
            Assert.Equal("explicit-model", received.AutoModeRecovery.OriginalModel);
            Assert.False(received.AutoModeRecovery.AllowAlways);
            Assert.Equal("interaction-1", received.AutoModeRecovery.InteractionId);
            Assert.Equal("user-1", received.AutoModeRecovery.OriginatingMessageId);
            await server.SendAndDrainSessionEventAsync(session, "capabilities.changed",
                new() { ["ui"] = new Dictionary<string, object?> { ["elicitation"] = false } }, TimeSpan.FromSeconds(5));
            Assert.True(session.Capabilities.Ui?.AutoModeRecovery);
            await server.SendAndDrainSessionEventAsync(session, "capabilities.changed",
                new() { ["ui"] = new Dictionary<string, object?> { ["autoModeRecovery"] = false } }, TimeSpan.FromSeconds(5));
            Assert.False(session.Capabilities.Ui?.AutoModeRecovery);
            Assert.False(session.Capabilities.Ui?.Elicitation);
        }
        else
        {
            Assert.Null(received.AutoModeRecovery);
            Assert.Null(session.Capabilities.Ui?.AutoModeRecovery);
        }
    }
}
#endif
