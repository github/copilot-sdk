/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Collections.Concurrent;
using System.Net;
using System.Net.Sockets;
using System.Diagnostics;
using System.Reflection;
using System.Runtime.CompilerServices;
using System.Text;
using System.Text.Json;
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class ClientSessionLifetimeTests
{
    private sealed record RpcRequestRecord(string Method, JsonElement Params);

    [Theory]
    [InlineData("static")]
    [InlineData("")]
    public async Task GitHubTokenProvider_Is_Mutually_Exclusive_With_Static_Token(string staticToken)
    {
        await using var client = new CopilotClient();
        var config = new SessionConfig
        {
            GitHubToken = staticToken,
            GitHubTokenProvider = _ => Task.FromResult(GitHubTokenProviderResult.Cancel())
        };

        var error = await Assert.ThrowsAsync<ArgumentException>(() => client.CreateSessionAsync(config));

        Assert.Contains("cannot be used together", error.Message);
    }

    [Fact]
    public async Task GitHubTokenProvider_Is_Released_When_Session_Is_Deleted()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            GitHubTokenProvider = _ => Task.FromResult(GitHubTokenProviderResult.Cancel())
        });
        var registrationId = Assert.Single(server.Requests, request => request.Method == "session.create")
            .Params.GetProperty("gitHubTokenProviderRegistrationId").GetString();

        await client.DeleteSessionAsync(session.SessionId);

        var error = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("gitHubToken.getToken", TokenRequest(registrationId)));
        Assert.Contains("Unknown GitHub token provider registration ID", error.Message);
        await session.DisposeAsync();
    }

    [Fact]
    public async Task GitHubTokenProvider_Is_Serialized_And_Maps_Callbacks()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        GitHubTokenProviderArgs? callbackArgs = null;
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            GitHubTokenProvider = args =>
            {
                callbackArgs = args;
                return Task.FromResult(GitHubTokenProviderResult.FromToken(new GitHubToken
                {
                    AccessToken = "secret-token",
                    TokenType = "bearer",
                    ExpiresIn = 8 * 60 * 60
                }));
            }
        });
        var request = Assert.Single(server.Requests, request => request.Method == "session.create");
        var registrationId = request.Params.GetProperty("gitHubTokenProviderRegistrationId").GetString();
        Assert.False(string.IsNullOrEmpty(registrationId));
        Assert.False(request.Params.TryGetProperty("gitHubToken", out _));

        var result = await server.SendRequestAsync("gitHubToken.getToken", new Dictionary<string, object?>
        {
            ["registrationId"] = registrationId,
            ["host"] = "github.example.com",
            ["sessionId"] = session.SessionId,
            ["reason"] = "refresh"
        });

        Assert.True(result.TryGetProperty("kind", out var kind), result.ToString());
        Assert.Equal("token", kind.GetString());
        Assert.Equal("secret-token", result.GetProperty("accessToken").GetString());
        Assert.Equal(8 * 60 * 60, result.GetProperty("expiresIn").GetInt64());
        Assert.NotNull(callbackArgs);
        Assert.Equal("github.example.com", callbackArgs.Host);
        Assert.Equal(session.SessionId, callbackArgs.SessionId);
        Assert.Equal(GitHubTokenRequestReason.Refresh, callbackArgs.Reason);
        Assert.DoesNotContain("secret-token", new GitHubToken
        {
            AccessToken = "secret-token",
            ExpiresIn = 8 * 60 * 60
        }.ToString());

        await session.DisposeAsync();
        var error = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("gitHubToken.getToken", new Dictionary<string, object?>
            {
                ["registrationId"] = registrationId,
                ["host"] = "github.com",
                ["reason"] = "initial"
            }));
        Assert.Contains("Unknown GitHub token provider registration ID", error.Message);

        server.ClearRequests();
        var resumed = await client.ResumeSessionAsync("resumed-session", new ResumeSessionConfig
        {
            GitHubTokenProvider = _ => Task.FromResult(GitHubTokenProviderResult.Cancel())
        });
        var resumeRequest = Assert.Single(server.Requests, request => request.Method == "session.resume");
        Assert.False(string.IsNullOrEmpty(
            resumeRequest.Params.GetProperty("gitHubTokenProviderRegistrationId").GetString()));
        await resumed.DisposeAsync();
    }

    [Fact]
    public async Task GitHubTokenProvider_Handles_Cancellation_Errors_And_Rollback()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var cancelledSession = await client.CreateSessionAsync(new SessionConfig
        {
            GitHubTokenProvider = _ => Task.FromResult(GitHubTokenProviderResult.Cancel())
        });
        var cancelledId = Assert.Single(server.Requests, request => request.Method == "session.create")
            .Params.GetProperty("gitHubTokenProviderRegistrationId").GetString();
        var cancelled = await server.SendRequestAsync("gitHubToken.getToken", TokenRequest(cancelledId));
        Assert.True(cancelled.TryGetProperty("kind", out var cancelledKind), cancelled.ToString());
        Assert.Equal("cancelled", cancelledKind.GetString());
        await cancelledSession.DisposeAsync();

        server.ClearRequests();
        var providerSession = await client.CreateSessionAsync(new SessionConfig
        {
            GitHubTokenProvider = _ => Task.FromException<GitHubTokenProviderResult>(
                new InvalidOperationException("provider failed"))
        });
        var providerId = Assert.Single(server.Requests, request => request.Method == "session.create")
            .Params.GetProperty("gitHubTokenProviderRegistrationId").GetString();
        var callbackError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("gitHubToken.getToken", TokenRequest(providerId)));
        Assert.Contains("provider failed", callbackError.Message);
        await providerSession.DisposeAsync();

        server.ClearRequests();
        server.FailSessionCreate();
        await Assert.ThrowsAsync<IOException>(() => client.CreateSessionAsync(new SessionConfig
        {
            GitHubTokenProvider = _ => Task.FromResult(GitHubTokenProviderResult.Cancel())
        }));
        var rolledBackId = Assert.Single(server.Requests, request => request.Method == "session.create")
            .Params.GetProperty("gitHubTokenProviderRegistrationId").GetString();
        var rollbackError = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("gitHubToken.getToken", TokenRequest(rolledBackId)));
        Assert.Contains("Unknown GitHub token provider registration ID", rollbackError.Message);
    }

    [Fact]
    public async Task GitHubTokenProvider_Resume_Replaces_Ownership()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var first = await client.CreateSessionAsync(new SessionConfig
        {
            SessionId = "replacement-session",
            GitHubTokenProvider = _ => Task.FromResult(GitHubTokenProviderResult.Cancel())
        });
        var firstId = Assert.Single(server.Requests, request => request.Method == "session.create")
            .Params.GetProperty("gitHubTokenProviderRegistrationId").GetString();

        await first.DisposeAsync();
        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("gitHubToken.getToken", TokenRequest(firstId)));

        server.ClearRequests();
        var resumed = await client.ResumeSessionAsync("replacement-session", new ResumeSessionConfig
        {
            GitHubTokenProvider = _ => Task.FromResult(GitHubTokenProviderResult.Cancel())
        });
        var secondId = Assert.Single(server.Requests, request => request.Method == "session.resume")
            .Params.GetProperty("gitHubTokenProviderRegistrationId").GetString();

        var result = await server.SendRequestAsync("gitHubToken.getToken", TokenRequest(secondId));
        Assert.Equal("cancelled", result.GetProperty("kind").GetString());

        await resumed.DisposeAsync();
        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            server.SendRequestAsync("gitHubToken.getToken", TokenRequest(secondId)));
    }

    private static Dictionary<string, object?> TokenRequest(string? registrationId) => new()
    {
        ["registrationId"] = registrationId,
        ["host"] = "github.com",
        ["reason"] = "initial"
    };

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
    public async Task ForceStopAsync_Unblocks_StopAsync_When_Session_Destroy_Hangs()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.DelayDestroy();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        _ = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var stopTask = client.StopAsync();
        await server.DestroyStarted;

        await client.ForceStopAsync();
        await stopTask.WaitAsync(TimeSpan.FromSeconds(5));

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

    [Theory]
    [InlineData(AutoTier.Efficiency, "efficiency", null)]
    [InlineData(AutoTier.Balance, "balance", null)]
    [InlineData(AutoTier.Intelligence, "intelligence", null)]
    [InlineData(AutoTier.Efficiency, "efficiency", false)]
    [InlineData(AutoTier.Balance, "balance", false)]
    [InlineData(AutoTier.Intelligence, "intelligence", false)]
    public async Task SessionRequests_Serialize_CapiAutoTier(AutoTier tier, string expectedTier, bool? enableWebSocketResponses)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var capi = new CapiSessionOptions { AutoTier = tier, EnableWebSocketResponses = enableWebSocketResponses };

        await using var created = await client.CreateSessionAsync(new SessionConfig
        {
            Model = "auto",
            Capi = capi,
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        await using var resumed = await client.ResumeSessionAsync("resume-with-auto-tier", new ResumeSessionConfig
        {
            Model = "auto",
            Capi = capi,
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        foreach (var method in new[] { "session.create", "session.resume" })
        {
            var request = Assert.Single(server.Requests, request => request.Method == method);
            var serializedCapi = request.Params.GetProperty("capi");
            Assert.Equal(expectedTier, serializedCapi.GetProperty("autoTier").GetString());
            if (enableWebSocketResponses.HasValue)
            {
                Assert.Equal(enableWebSocketResponses.Value, serializedCapi.GetProperty("enableWebSocketResponses").GetBoolean());
            }
            else
            {
                Assert.False(serializedCapi.TryGetProperty("enableWebSocketResponses", out _));
            }
        }
    }

    [Theory]
    [InlineData(false, null)]
    [InlineData(true, null)]
    [InlineData(true, false)]
    public async Task SessionRequests_Omit_CapiAutoTier_WhenUnset(bool includeCapi, bool? enableWebSocketResponses)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var capi = includeCapi ? new CapiSessionOptions { EnableWebSocketResponses = enableWebSocketResponses } : null;

        await using var created = await client.CreateSessionAsync(new SessionConfig
        {
            Model = "auto",
            Capi = capi,
            OnPermissionRequest = PermissionHandler.ApproveAll
        });
        await using var resumed = await client.ResumeSessionAsync("resume-without-auto-tier", new ResumeSessionConfig
        {
            Capi = capi,
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        foreach (var method in new[] { "session.create", "session.resume" })
        {
            var request = Assert.Single(server.Requests, request => request.Method == method);
            Assert.Equal(includeCapi, request.Params.TryGetProperty("capi", out var serializedCapi));
            if (includeCapi)
            {
                Assert.False(serializedCapi.TryGetProperty("autoTier", out _));
                if (enableWebSocketResponses.HasValue)
                {
                    Assert.Equal(enableWebSocketResponses.Value, serializedCapi.GetProperty("enableWebSocketResponses").GetBoolean());
                }
                else
                {
                    Assert.Empty(serializedCapi.EnumerateObject());
                }
            }
        }
    }

    [Fact]
    public async Task SessionRequests_Serialize_AdditionalDirectories()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        await using var created = await client.CreateSessionAsync(new SessionConfig
        {
            AdditionalDirectories = ["/repo/shared", "/repo/generated"],
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var createRequest = Assert.Single(server.Requests, request => request.Method == "session.create");
        Assert.Collection(
            createRequest.Params.GetProperty("additionalDirectories").EnumerateArray(),
            value => Assert.Equal("/repo/shared", value.GetString()),
            value => Assert.Equal("/repo/generated", value.GetString()));

        server.ClearRequests();

        await using var resumed = await client.ResumeSessionAsync("resume-with-additional-directories", new ResumeSessionConfig
        {
            AdditionalDirectories = ["/repo/resumed"],
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var resumeRequest = Assert.Single(server.Requests, request => request.Method == "session.resume");
        Assert.Collection(
            resumeRequest.Params.GetProperty("additionalDirectories").EnumerateArray(),
            value => Assert.Equal("/repo/resumed", value.GetString()));
    }

    [Fact]
    public async Task SessionRequests_Serialize_Terminal_Tools()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        var terminalTool = CopilotTool.DefineTool(
            (Func<string>)(() => "done"),
            new CopilotToolOptions { IsTerminal = true });
        var plainTool = CopilotTool.DefineTool((Func<string>)(() => "continue"));

        await using var created = await client.CreateSessionAsync(new SessionConfig
        {
            Tools = [terminalTool, plainTool],
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var createRequest = Assert.Single(server.Requests, request => request.Method == "session.create");
        var createTools = createRequest.Params.GetProperty("tools");
        Assert.True(createTools[0].GetProperty("isTerminal").GetBoolean());
        Assert.False(createTools[1].TryGetProperty("isTerminal", out _));

        server.ClearRequests();

        await using var resumed = await client.ResumeSessionAsync("resume-with-terminal-tool", new ResumeSessionConfig
        {
            Tools = [terminalTool],
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var resumeRequest = Assert.Single(server.Requests, request => request.Method == "session.resume");
        Assert.True(resumeRequest.Params.GetProperty("tools")[0].GetProperty("isTerminal").GetBoolean());
    }

    [Fact]
    public async Task EmptyMode_Create_Sends_Empty_IncludedBuiltinSkills()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Mode = CopilotClientMode.Empty,
            BaseDirectory = Path.GetTempPath(),
        });

        await using var created = await client.CreateSessionAsync(new SessionConfig
        {
            AvailableTools = [],
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var update = Assert.Single(server.Requests, request => request.Method == "session.options.update");
        Assert.True(update.Params.TryGetProperty("includedBuiltinSkills", out var skills));
        Assert.Equal(JsonValueKind.Array, skills.ValueKind);
        Assert.Equal(0, skills.GetArrayLength());
        // Adjacent unconditional plugin isolation is still present.
        Assert.True(update.Params.TryGetProperty("installedPlugins", out var plugins));
        Assert.Equal(0, plugins.GetArrayLength());
    }

    [Fact]
    public async Task EmptyMode_Resume_Sends_Empty_IncludedBuiltinSkills()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Mode = CopilotClientMode.Empty,
            BaseDirectory = Path.GetTempPath(),
        });

        await using var resumed = await client.ResumeSessionAsync("resume-empty-skills", new ResumeSessionConfig
        {
            AvailableTools = [],
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var update = Assert.Single(server.Requests, request => request.Method == "session.options.update");
        Assert.True(update.Params.TryGetProperty("includedBuiltinSkills", out var skills));
        Assert.Equal(JsonValueKind.Array, skills.ValueKind);
        Assert.Equal(0, skills.GetArrayLength());
    }

    [Fact]
    public async Task EmptyMode_Resume_Preserves_Explicit_IncludedBuiltinSkills()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Mode = CopilotClientMode.Empty,
            BaseDirectory = Path.GetTempPath(),
        });

        await using var resumed = await client.ResumeSessionAsync("resume-selected-skills", new ResumeSessionConfig
        {
            AvailableTools = [],
            IncludedBuiltinSkills = ["code-review"],
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var update = Assert.Single(server.Requests, request => request.Method == "session.options.update");
        var skills = update.Params.GetProperty("includedBuiltinSkills");
        Assert.Equal(["code-review"], skills.EnumerateArray().Select(value => value.GetString()));
    }

    [Fact]
    public async Task EmptyMode_Create_With_EnableSkills_Still_Sends_Empty_IncludedBuiltinSkills()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Mode = CopilotClientMode.Empty,
            BaseDirectory = Path.GetTempPath(),
        });

        // Caller opts into their own custom skills. Runtime-bundled built-ins must
        // still be excluded: the empty post-patch cannot be weakened by the caller.
        await using var created = await client.CreateSessionAsync(new SessionConfig
        {
            AvailableTools = [],
            EnableSkills = true,
            SkillDirectories = [Path.Combine(Path.GetTempPath(), "skills")],
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var update = Assert.Single(server.Requests, request => request.Method == "session.options.update");
        Assert.True(update.Params.TryGetProperty("includedBuiltinSkills", out var skills));
        Assert.Equal(JsonValueKind.Array, skills.ValueKind);
        Assert.Equal(0, skills.GetArrayLength());
    }

    [Fact]
    public async Task EmptyMode_Create_Preserves_Explicit_IncludedBuiltinSkills()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url),
            Mode = CopilotClientMode.Empty,
            BaseDirectory = Path.GetTempPath(),
        });

        await using var created = await client.CreateSessionAsync(new SessionConfig
        {
            AvailableTools = [],
            IncludedBuiltinSkills = ["code-review"],
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var update = Assert.Single(server.Requests, request => request.Method == "session.options.update");
        var skills = update.Params.GetProperty("includedBuiltinSkills");
        Assert.Equal(["code-review"], skills.EnumerateArray().Select(value => value.GetString()));
    }

    [Fact]
    public async Task CopilotCliMode_Create_Does_Not_Inject_IncludedBuiltinSkills()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        await using var created = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        // In the default copilot-cli mode with no overridable options set, no
        // options patch is sent at all, so the field is never injected.
        Assert.DoesNotContain(server.Requests, request =>
            request.Method == "session.options.update"
            && request.Params.TryGetProperty("includedBuiltinSkills", out _));
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
    public async Task SendAndWaitAsync_Skips_Autopilot_Continuation_Idle()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var sendTask = session.SendAndWaitAsync(new MessageOptions { Prompt = "keep going" });
        await WaitForRequestAsync(server, "session.send");

        var continuationIdleProcessed = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        using var subscription = session.On<SessionIdleEvent>(idle =>
        {
            if (idle.Data.Mode == SessionMode.Autopilot)
            {
                continuationIdleProcessed.TrySetResult();
            }
        });

        DispatchEvent(session, new AssistantMessageEvent
        {
            Id = Guid.NewGuid(),
            Data = new AssistantMessageData
            {
                Content = "intermediate",
                MessageId = "assistant-1"
            }
        });
        DispatchEvent(session, new SessionIdleEvent
        {
            Id = Guid.NewGuid(),
            Data = new SessionIdleData { Mode = SessionMode.Autopilot }
        });

        await continuationIdleProcessed.Task.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.False(sendTask.IsCompleted);

        DispatchEvent(session, new AssistantMessageEvent
        {
            Id = Guid.NewGuid(),
            Data = new AssistantMessageData
            {
                Content = "final",
                MessageId = "assistant-2"
            }
        });
        DispatchEvent(session, new SessionIdleEvent
        {
            Id = Guid.NewGuid(),
            Data = new SessionIdleData { Mode = SessionMode.Interactive }
        });

        var result = await sendTask.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.NotNull(result);
        Assert.Equal("final", result.Data.Content);
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

    [Fact]
    public async Task CreateSessionAsync_Serializes_ManagedSettings_Permissions()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();
        var permissionInvocation = new TaskCompletionSource<PermissionInvocation>(
            TaskCreationOptions.RunContinuationsAsynchronously);

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            ManagedSettings = new ManagedSettings
            {
                Permissions = new ManagedSettingsPermissions
                {
                    DisableBypassPermissionsMode = DisableBypassPermissionsModes.Disable,
                    Deny = ["shell(rm*)"],
                    Ask = ["write"],
                    Allow = []
                }
            },
            OnPermissionRequest = (_, invocation) =>
            {
                permissionInvocation.TrySetResult(invocation);
                return Task.FromResult(PermissionDecision.NoResult());
            }
        });

        var request = Assert.Single(server.Requests, request => request.Method == "session.create");
        Assert.False(request.Params.TryGetProperty("enableManagedSettings", out _));
        var permissions = request.Params.GetProperty("managedSettings").GetProperty("permissions");
        Assert.Equal("disable", permissions.GetProperty("disableBypassPermissionsMode").GetString());
        Assert.Equal("shell(rm*)", Assert.Single(permissions.GetProperty("deny").EnumerateArray()).GetString());
        Assert.Equal("write", Assert.Single(permissions.GetProperty("ask").EnumerateArray()).GetString());
        Assert.Empty(permissions.GetProperty("allow").EnumerateArray());

        DispatchEvent(session, new PermissionRequestedEvent
        {
            Data = new PermissionRequestedData
            {
                PermissionRequest = new PermissionRequest { Kind = "read" },
                RequestId = "managed-permission"
            }
        });
        var invocation = await permissionInvocation.Task.WaitAsync(TimeSpan.FromSeconds(5));
        Assert.True(invocation.ManagedSettingsEnabled);
    }

    [Fact]
    public async Task CreateSessionAsync_Serializes_Future_ManagedSettings_Bypass_Mode()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            ManagedSettings = new ManagedSettings
            {
                Permissions = new ManagedSettingsPermissions
                {
                    DisableBypassPermissionsMode = "future-fail-closed-mode"
                }
            },
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var request = Assert.Single(server.Requests, request => request.Method == "session.create");
        var permissions = request.Params.GetProperty("managedSettings").GetProperty("permissions");
        Assert.Equal(
            "future-fail-closed-mode",
            permissions.GetProperty("disableBypassPermissionsMode").GetString());
    }

    [Fact]
    public async Task PermissionResponse_Forwards_DecisionContext_As_Sibling_Of_Result()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (_, _) => Task.FromResult<PermissionDecision>(
                new PermissionDecisionApproveOnce
                {
                    DecisionContext = new PermissionDecisionContext
                    {
                        Outcome = PermissionDecisionOutcome.AutoApproved,
                        Source = PermissionDecisionSource.HostPolicy,
                        Surface = PermissionDecisionSurface.Sdk
                    }
                })
        });

        DispatchEvent(session, new PermissionRequestedEvent
        {
            Data = new PermissionRequestedData
            {
                PermissionRequest = new PermissionRequest { Kind = "read" },
                RequestId = "req-with-context"
            }
        });

        var request = await WaitForRequestAsync(server, "session.permissions.handlePendingPermissionRequest");

        Assert.True(request.Params.TryGetProperty("decisionContext", out var decisionContext));
        Assert.Equal("auto_approved", decisionContext.GetProperty("outcome").GetString());
        Assert.Equal("host_policy", decisionContext.GetProperty("source").GetString());
        Assert.Equal("sdk", decisionContext.GetProperty("surface").GetString());

        var result = request.Params.GetProperty("result");
        Assert.Equal("approve-once", result.GetProperty("kind").GetString());
        Assert.False(result.TryGetProperty("decisionContext", out _));
    }

    [Fact]
    public async Task PermissionResponse_Omits_DecisionContext_When_Not_Supplied()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (_, _) => Task.FromResult(PermissionDecision.ApproveOnce())
        });

        DispatchEvent(session, new PermissionRequestedEvent
        {
            Data = new PermissionRequestedData
            {
                PermissionRequest = new PermissionRequest { Kind = "read" },
                RequestId = "req-no-context"
            }
        });

        var request = await WaitForRequestAsync(server, "session.permissions.handlePendingPermissionRequest");

        Assert.False(request.Params.TryGetProperty("decisionContext", out _));
        var result = request.Params.GetProperty("result");
        Assert.Equal("approve-once", result.GetProperty("kind").GetString());
        Assert.False(result.TryGetProperty("decisionContext", out _));
    }

    [Fact]
    public async Task PermissionResponse_Uses_Latest_Context_When_Reassigned()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (_, _) =>
            {
                var decision = new PermissionDecisionApproveOnce
                {
                    DecisionContext = new PermissionDecisionContext
                    {
                        Outcome = PermissionDecisionOutcome.PromptedUser,
                        Source = PermissionDecisionSource.HumanResponse,
                        Surface = PermissionDecisionSurface.Tui
                    }
                };
                decision.DecisionContext = new PermissionDecisionContext
                {
                    Outcome = PermissionDecisionOutcome.AutoApproved,
                    Source = PermissionDecisionSource.HostPolicy,
                    Surface = PermissionDecisionSurface.Sdk
                };
                return Task.FromResult<PermissionDecision>(decision);
            }
        });

        DispatchEvent(session, new PermissionRequestedEvent
        {
            Data = new PermissionRequestedData
            {
                PermissionRequest = new PermissionRequest { Kind = "read" },
                RequestId = "req-replace-context"
            }
        });

        var request = await WaitForRequestAsync(server, "session.permissions.handlePendingPermissionRequest");

        var decisionContext = request.Params.GetProperty("decisionContext");
        Assert.Equal("auto_approved", decisionContext.GetProperty("outcome").GetString());
        Assert.Equal("host_policy", decisionContext.GetProperty("source").GetString());
        Assert.Equal("sdk", decisionContext.GetProperty("surface").GetString());
    }

    [Fact]
    public async Task PermissionResponse_Is_Suppressed_For_NoResult_Even_With_Context()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();

        var handlerInvoked = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (_, _) =>
            {
                handlerInvoked.TrySetResult();
                return Task.FromResult<PermissionDecision>(
                    new PermissionDecisionNoResult
                    {
                        DecisionContext = new PermissionDecisionContext
                        {
                            Outcome = PermissionDecisionOutcome.PromptedUser,
                            Source = PermissionDecisionSource.HumanResponse,
                            Surface = PermissionDecisionSurface.Sdk
                        }
                    });
            }
        });

        DispatchEvent(session, new PermissionRequestedEvent
        {
            Data = new PermissionRequestedData
            {
                PermissionRequest = new PermissionRequest { Kind = "read" },
                RequestId = "req-no-result"
            }
        });

        await handlerInvoked.Task.WaitAsync(TimeSpan.FromSeconds(5));
        // Give the send path a chance to (incorrectly) fire before asserting suppression.
        await Task.Delay(200);

        Assert.DoesNotContain(server.Requests, request => request.Method == "session.permissions.handlePendingPermissionRequest");
    }

    [Fact]
    public async Task PermissionResponse_Never_Nests_DecisionContext_Inside_Result()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = (_, _) => Task.FromResult<PermissionDecision>(
                new PermissionDecisionReject
                {
                    Feedback = "denied by policy",
                    DecisionContext = new PermissionDecisionContext
                    {
                        Outcome = PermissionDecisionOutcome.AutopilotDenied,
                        Source = PermissionDecisionSource.HostPolicy,
                        Surface = PermissionDecisionSurface.Sdk
                    }
                })
        });

        DispatchEvent(session, new PermissionRequestedEvent
        {
            Data = new PermissionRequestedData
            {
                PermissionRequest = new PermissionRequest { Kind = "read" },
                RequestId = "req-reject-context"
            }
        });

        var request = await WaitForRequestAsync(server, "session.permissions.handlePendingPermissionRequest");

        var result = request.Params.GetProperty("result");
        Assert.Equal("reject", result.GetProperty("kind").GetString());
        Assert.Equal("denied by policy", result.GetProperty("feedback").GetString());
        // The context provenance must never be serialized inside the decision itself.
        Assert.False(result.TryGetProperty("decisionContext", out _));
        // It is forwarded as a sibling instead.
        Assert.True(request.Params.TryGetProperty("decisionContext", out _));
    }

    [Fact]
    public async Task CreateSessionAsync_Omits_ManagedSettings_When_Unset()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });
        await client.StartAsync();

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll
        });

        var request = Assert.Single(server.Requests, request => request.Method == "session.create");
        Assert.False(request.Params.TryGetProperty("managedSettings", out _));
    }

    [Fact]
    public async Task ResumeSessionAsync_Serializes_ManagedSettings_Permissions()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions { Connection = RuntimeConnection.ForUri(server.Url) });

        await using var session = await client.ResumeSessionAsync("session-managed", new ResumeSessionConfig
        {
            ManagedSettings = new ManagedSettings
            {
                Permissions = new ManagedSettingsPermissions
                {
                    Deny = ["shell(rm*)"]
                }
            },
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnEvent = _ => { }
        });

        var request = Assert.Single(server.Requests, request => request.Method == "session.resume");
        var permissions = request.Params.GetProperty("managedSettings").GetProperty("permissions");
        Assert.Equal("shell(rm*)", Assert.Single(permissions.GetProperty("deny").EnumerateArray()).GetString());
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
        private readonly Task _serverTask;
        private readonly List<RpcRequestRecord> _requests = [];
        private readonly object _requestsLock = new();
        private readonly ConcurrentDictionary<int, TaskCompletionSource<JsonElement>> _pendingRequests = new();
        private NetworkStream? _stream;
        private int _nextRequestId;
        private string? _lastSessionId;
        private bool _delayDestroy;
        private bool _failRuntimeShutdown;
        private bool _failSessionCreate;

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

        public void FailSessionCreate()
        {
            _failSessionCreate = true;
        }

        public async Task<JsonElement> SendRequestAsync(string method, Dictionary<string, object?> parameters)
        {
            var stream = _stream ?? throw new InvalidOperationException("Client is not connected.");
            var id = Interlocked.Increment(ref _nextRequestId);
            var completion = new TaskCompletionSource<JsonElement>(TaskCreationOptions.RunContinuationsAsynchronously);
            if (!_pendingRequests.TryAdd(id, completion))
            {
                throw new InvalidOperationException("Failed to track callback request.");
            }

            await WriteMessageAsync(stream, new Dictionary<string, object?>
            {
                ["jsonrpc"] = "2.0",
                ["id"] = id,
                ["method"] = method,
                ["params"] = parameters
            }, _cts.Token);
            return await completion.Task.WaitAsync(_cts.Token);
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
            _stream = stream;

            while (!_cts.Token.IsCancellationRequested)
            {
                using var message = await ReadMessageAsync(stream, _cts.Token);
                if (message is null)
                {
                    return;
                }

                var root = message.RootElement;
                if (root.TryGetProperty("method", out _))
                {
                    await HandleRequestAsync(stream, root, _cts.Token);
                    continue;
                }

                if (root.TryGetProperty("id", out var responseId)
                    && responseId.TryGetInt32(out var id)
                    && _pendingRequests.TryRemove(id, out var completion))
                {
                    if (root.TryGetProperty("error", out var error))
                    {
                        completion.TrySetException(new InvalidOperationException(
                            error.GetProperty("message").GetString()));
                    }
                    else
                    {
                        completion.TrySetResult(root.GetProperty("result").Clone());
                    }
                }
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
            if (method == "session.create" && _failSessionCreate)
            {
                _failSessionCreate = false;
                await WriteMessageAsync(stream, new Dictionary<string, object?>
                {
                    ["jsonrpc"] = "2.0",
                    ["id"] = id,
                    ["error"] = new Dictionary<string, object?>
                    {
                        ["code"] = -32000,
                        ["message"] = "session create failed"
                    }
                }, cancellationToken);
                return;
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
                "session.options.update" => new Dictionary<string, object?>
                {
                    ["success"] = true
                },
                "session.mcp.oauth.handlePendingRequest" => new Dictionary<string, object?>
                {
                    ["success"] = true
                },
                "session.permissions.handlePendingPermissionRequest" => new Dictionary<string, object?>
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
