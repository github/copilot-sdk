/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Diagnostics;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Channels;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// GitHub App-shaped coverage for MCP lifecycle, OAuth, configuration, and MCP Apps.
/// </summary>
public class GitHubAppMcpE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "github_app_mcp", output)
{
    private static readonly TimeSpan EventTimeout = TimeSpan.FromSeconds(60);
    private const string ExpectedToken = "github-app-mcp-token";

    [Fact]
    public async Task Should_List_Reload_Restart_And_Report_App_Mcp_State()
    {
        const string serverName = "github-app-lifecycle";
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            ClientName = "github-app",
            McpServers = CreateTestMcpServers(serverName),
        });
        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);

        var initial = await session.Rpc.Mcp.ListAsync();
        Assert.NotNull(initial.Host);
        Assert.Empty(initial.Host!.FailedServers);
        Assert.Empty(initial.Host.NeedsAuthServers);
        Assert.Empty(initial.Host.PendingConnections);
        Assert.Equal(McpServerStatus.Connected, Assert.Single(initial.Servers).Status);

        var tools = await session.Rpc.Mcp.ListToolsAsync(serverName);
        Assert.Contains(tools.Tools, tool => tool.Name == "get_env");

        var statusEvents = Channel.CreateUnbounded<SessionMcpServerStatusChangedEvent>();
        using var subscription = session.On<SessionMcpServerStatusChangedEvent>(
            evt => statusEvents.Writer.TryWrite(evt));

        await session.Rpc.Mcp.RestartServerAsync(serverName);
        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);
        await session.Rpc.Mcp.ReloadAsync();
        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);

        var connectedEvent = await ReadMatchingAsync(
            statusEvents.Reader,
            evt => evt.Data.ServerName == serverName && evt.Data.Status == McpServerStatus.Connected);
        Assert.Equal(serverName, connectedEvent.Data.ServerName);
        Assert.True((await session.Rpc.Mcp.IsServerRunningAsync(serverName)).Running);
    }

    [Fact]
    public async Task Should_Provide_First_Party_App_Token_And_Cancel_Third_Party_Oauth()
    {
        await using var firstParty = await AppOAuthMcpServer.StartAsync(ExpectedToken);
        await using var thirdParty = await AppOAuthMcpServer.StartAsync(ExpectedToken);
        const string firstPartyName = "github-app-first-party";
        const string thirdPartyName = "github-app-third-party";
        var requests = Channel.CreateUnbounded<McpAuthContext>();

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            ClientName = "github-app",
            OnMcpAuthRequest = request =>
            {
                requests.Writer.TryWrite(request);
                return Task.FromResult<McpAuthResult?>(
                    request.ServerName == firstPartyName
                        ? McpAuthResult.FromToken(new McpAuthToken
                        {
                            AccessToken = ExpectedToken,
                            TokenType = "Bearer",
                            ExpiresIn = 3600,
                        })
                        : McpAuthResult.Cancel());
            },
            McpServers = new Dictionary<string, McpServerConfig>
            {
                [firstPartyName] = new McpHttpServerConfig
                {
                    Url = $"{firstParty.Url}/mcp",
                    Tools = ["*"],
                },
                [thirdPartyName] = new McpHttpServerConfig
                {
                    Url = $"{thirdParty.Url}/mcp",
                    Tools = ["*"],
                },
            },
        });

        await session.Rpc.Mcp.ReloadAsync();
        await WaitForMcpServerStatusAsync(session, firstPartyName, McpServerStatus.Connected);
        await WaitForMcpServerStatusAsync(session, thirdPartyName, McpServerStatus.NeedsAuth);

        var observed = new List<McpAuthContext>();
        while (observed.Select(request => request.ServerName).Distinct(StringComparer.Ordinal).Count() < 2)
        {
            observed.Add(await requests.Reader.ReadAsync().AsTask().WaitAsync(EventTimeout));
        }

        Assert.Contains(observed, request =>
            request.ServerName == firstPartyName && request.Reason == McpOauthRequestReason.Initial);
        Assert.Contains(observed, request =>
            request.ServerName == thirdPartyName && request.Reason == McpOauthRequestReason.Initial);

        var firstPartyRequests = await firstParty.GetRequestsAsync();
        Assert.Contains(firstPartyRequests, request => request.Authorization == $"Bearer {ExpectedToken}");
        var state = await session.Rpc.Mcp.ListAsync();
        Assert.Contains(thirdPartyName, state.Host!.NeedsAuthServers.Keys);
    }

    [Fact]
    public async Task Should_Reconnect_With_Cached_App_Token_Then_Return_Interactive_Oauth_Url()
    {
        await using var oauthServer = await AppOAuthMcpServer.StartAsync(ExpectedToken);
        const string serverName = "github-app-oauth-reconnect";
        var tokenRequests = 0;

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            ClientName = "github-app",
            OnMcpAuthRequest = request =>
            {
                Interlocked.Increment(ref tokenRequests);
                return Task.FromResult<McpAuthResult?>(McpAuthResult.FromToken(new McpAuthToken
                {
                    AccessToken = ExpectedToken,
                    TokenType = "Bearer",
                    ExpiresIn = 3600,
                }));
            },
            McpServers = new Dictionary<string, McpServerConfig>
            {
                [serverName] = new McpHttpServerConfig
                {
                    Url = $"{oauthServer.Url}/mcp",
                    Tools = ["*"],
                },
            },
        });

        await session.Rpc.Mcp.ReloadAsync();
        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);
        Assert.True(tokenRequests >= 1);

        await session.Rpc.Mcp.RestartServerAsync(serverName);
        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);
        Assert.Contains(
            await oauthServer.GetRequestsAsync(),
            request => request.Authorization == $"Bearer {ExpectedToken}");

        var cached = await session.Rpc.Mcp.Oauth.ProbeAsync(serverName);
        Assert.IsType<McpOauthProbeResultAuthenticated>(cached);

        var interactive = await session.Rpc.Mcp.Oauth.LoginAsync(
            serverName,
            forceReauth: true,
            clientName: "GitHub App",
            callbackSuccessMessage: "Return to GitHub.",
            clientId: "github-app-client",
            publicClient: true);
        Assert.NotNull(interactive.AuthorizationUrl);
        Assert.StartsWith($"{oauthServer.Url}/authorize", interactive.AuthorizationUrl, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Should_Manage_And_Discover_App_Mcp_Config_Lifecycle()
    {
        var serverName = $"github-app-config-{Guid.NewGuid():N}";
        var testServer = Path.Join(FindTestHarnessDir(), "test-mcp-server.mjs");
        await Client.StartAsync();

        try
        {
            await Client.Rpc.Mcp.Config.AddAsync(serverName, new McpStdioServerConfig
            {
                Command = "node",
                Args = [testServer],
                Tools = ["get_env"],
            });

            var afterAdd = await Client.Rpc.Mcp.Config.ListAsync();
            Assert.Contains(serverName, afterAdd.Servers.Keys);
            var discovered = await Client.Rpc.Mcp.DiscoverAsync(
                workingDirectory: Ctx.WorkDir,
                includeEffectiveSource: true);
            var enabled = Assert.Single(discovered.Servers, server => server.Name == serverName);
            Assert.True(enabled.Enabled);
            Assert.NotNull(enabled.EffectiveSource);

            await Client.Rpc.Mcp.Config.UpdateAsync(serverName, new McpStdioServerConfig
            {
                Command = "node",
                Args = [testServer],
                Env = new Dictionary<string, string> { ["APP_CONFIG_VERSION"] = "2" },
                Tools = ["*"],
            });
            var updated = GetServerConfig(await Client.Rpc.Mcp.Config.ListAsync(), serverName);
            Assert.Equal("2", updated.GetProperty("env").GetProperty("APP_CONFIG_VERSION").GetString());

            await Client.Rpc.Mcp.Config.DisableAsync([serverName]);
            var disabled = await Client.Rpc.Mcp.DiscoverAsync(Ctx.WorkDir);
            Assert.False(Assert.Single(disabled.Servers, server => server.Name == serverName).Enabled);

            await Client.Rpc.Mcp.Config.EnableAsync([serverName]);
            var reenabled = await Client.Rpc.Mcp.DiscoverAsync(Ctx.WorkDir);
            Assert.True(Assert.Single(reenabled.Servers, server => server.Name == serverName).Enabled);
        }
        finally
        {
            await Client.Rpc.Mcp.Config.RemoveAsync(serverName);
        }

        Assert.DoesNotContain(serverName, (await Client.Rpc.Mcp.Config.ListAsync()).Servers.Keys);
    }

    [Fact]
    public async Task Should_Enforce_Mcp_App_Origin_Server()
    {
        const string serverName = "github-app-origin";
        const string otherServerName = "github-app-other-origin";
        var servers = CreateTestMcpServers(serverName, otherServerName);
        ((McpStdioServerConfig)servers[serverName]).Env =
            new Dictionary<string, string> { ["APP_ORIGIN_VALUE"] = "origin-ok" };

        var environment = Ctx.GetEnvironment();
        environment["COPILOT_MCP_APPS"] = "true";
        environment["MCP_APPS"] = "true";
        await using var client = Ctx.CreateClient(environment: environment);
        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            ClientName = "github-app",
            EnableMcpApps = true,
            McpServers = servers,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);
        await WaitForMcpServerStatusAsync(session, otherServerName, McpServerStatus.Connected);

        using var argument = JsonDocument.Parse("""{"name":"APP_ORIGIN_VALUE"}""");
        var sameOrigin = await session.Rpc.Mcp.Apps.CallToolAsync(
            serverName,
            "get_env",
            originServerName: serverName,
            arguments: new Dictionary<string, JsonElement>
            {
                ["name"] = argument.RootElement.GetProperty("name").Clone(),
            });
        Assert.Contains("origin-ok", sameOrigin["content"].GetRawText(), StringComparison.Ordinal);

        var crossOrigin = await Assert.ThrowsAnyAsync<Exception>(() =>
            session.Rpc.Mcp.Apps.CallToolAsync(
                serverName,
                "get_env",
                originServerName: otherServerName,
                arguments: new Dictionary<string, JsonElement>
                {
                    ["name"] = argument.RootElement.GetProperty("name").Clone(),
                }));
        Assert.Contains("origin", crossOrigin.ToString(), StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Should_Preserve_Disabled_App_Mcp_Servers_Across_Reload_And_Resume()
    {
        const string enabledName = "github-app-enabled-mcp";
        const string disabledName = "github-app-disabled-mcp";
        var client1 = Ctx.CreateClient();
        var session1 = await Ctx.CreateSessionAsync(client1, new SessionConfig
        {
            ClientName = "github-app",
            EnableSessionStore = true,
            McpServers = CreateTestMcpServers(enabledName, disabledName),
            DisabledMcpServers = [disabledName],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        await WaitForMcpServerStatusAsync(session1, enabledName, McpServerStatus.Connected);
        await WaitForMcpServerStatusAsync(session1, disabledName, McpServerStatus.Disabled);

        await session1.Rpc.Mcp.ReloadAsync();
        await WaitForMcpServerStatusAsync(session1, enabledName, McpServerStatus.Connected);
        await WaitForMcpServerStatusAsync(session1, disabledName, McpServerStatus.Disabled);
        Assert.Contains(disabledName, (await session1.Rpc.Mcp.ListAsync()).Host!.DisabledServers);

        var sessionId = session1.SessionId;
        var response = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with exactly APP_MCP_DISABLED_STATE.",
        });
        Assert.Contains("APP_MCP_DISABLED_STATE", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);
        await session1.Rpc.SuspendAsync();
        await session1.DisposeAsync();
        await client1.ForceStopAsync();

        await using var client2 = Ctx.CreateClient();
        await using var session2 = await Ctx.ResumeSessionAsync(client2, sessionId, new ResumeSessionConfig
        {
            ClientName = "github-app",
            EnableSessionStore = true,
            McpServers = CreateTestMcpServers(enabledName, disabledName),
            DisabledMcpServers = [disabledName],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        await WaitForMcpServerStatusAsync(session2, enabledName, McpServerStatus.Connected);
        await WaitForMcpServerStatusAsync(session2, disabledName, McpServerStatus.Disabled);
        var resumed = await session2.Rpc.Mcp.ListAsync();
        Assert.Contains(disabledName, resumed.Host!.DisabledServers);
        Assert.DoesNotContain(resumed.Host.PendingConnections, name => name == disabledName);
    }

    private static JsonElement GetServerConfig(McpConfigList list, string serverName)
    {
        Assert.True(list.Servers.TryGetValue(serverName, out var config));
        return Assert.IsType<JsonElement>(config);
    }

    private static async Task<T> ReadMatchingAsync<T>(
        ChannelReader<T> reader,
        Func<T, bool> predicate)
    {
        using var timeout = new CancellationTokenSource(EventTimeout);
        while (await reader.WaitToReadAsync(timeout.Token))
        {
            while (reader.TryRead(out var item))
            {
                if (predicate(item))
                {
                    return item;
                }
            }
        }

        throw new TimeoutException("Timed out waiting for matching MCP event.");
    }

    private sealed class AppOAuthMcpServer : IAsyncDisposable
    {
        private readonly Process _process;
        private readonly HttpClient _http = new();

        private AppOAuthMcpServer(Process process, string url)
        {
            _process = process;
            Url = url;
        }

        public string Url { get; }

        public static async Task<AppOAuthMcpServer> StartAsync(string expectedToken)
        {
            var script = Path.Join(FindTestHarnessDir(), "test-mcp-oauth-server.mjs");
            var startInfo = new ProcessStartInfo
            {
                FileName = "node",
                Arguments = $"\"{script.Replace("\"", "\\\"")}\"",
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
            };
            startInfo.Environment["EXPECTED_TOKEN"] = expectedToken;

            var process = Process.Start(startInfo)
                ?? throw new InvalidOperationException("Failed to start OAuth MCP server.");
            var stderr = process.StandardError.ReadToEndAsync();
            using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(10));
            while (!timeout.IsCancellationRequested)
            {
                var line = await process.StandardOutput.ReadLineAsync(timeout.Token);
                if (line is null)
                {
                    throw new InvalidOperationException($"OAuth MCP server exited before listening: {await stderr}");
                }

                if (line.StartsWith("Listening: ", StringComparison.Ordinal))
                {
                    return new AppOAuthMcpServer(process, line["Listening: ".Length..]);
                }
            }

            throw new TimeoutException($"Timed out waiting for OAuth MCP server: {await stderr}");
        }

        public async Task<List<AppOAuthRequest>> GetRequestsAsync()
        {
            var json = await _http.GetStringAsync($"{Url}/__requests");
            using var document = JsonDocument.Parse(json);
            return document.RootElement.EnumerateArray()
                .Select(element => new AppOAuthRequest(
                    element.TryGetProperty("authorization", out var authorization)
                        && authorization.ValueKind == JsonValueKind.String
                            ? authorization.GetString()
                            : null,
                    element.GetProperty("path").GetString()!))
                .ToList();
        }

        public async ValueTask DisposeAsync()
        {
            _http.Dispose();
            if (!_process.HasExited)
            {
                _process.Kill(entireProcessTree: true);
                await _process.WaitForExitAsync();
            }
            _process.Dispose();
        }
    }

    private sealed record AppOAuthRequest(string? Authorization, string Path);
}
