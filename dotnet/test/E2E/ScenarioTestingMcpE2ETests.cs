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
/// Representative scenario coverage for MCP lifecycle, OAuth, configuration, and MCP Apps.
/// </summary>
public class ScenarioTestingMcpE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_mcp", output)
{
    private static readonly TimeSpan EventTimeout = TimeSpan.FromSeconds(60);
    private const string ExpectedToken = "scenario-client-mcp-token";

    [Fact]
    public async Task Should_List_Reload_Restart_And_Report_Scenario_Mcp_State()
    {
        const string serverName = "scenario-client-lifecycle";
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            ClientName = "scenario-client",
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
    public async Task Should_Provide_First_Party_Scenario_Token_And_Cancel_Third_Party_Oauth()
    {
        await using var firstParty = await ScenarioOAuthMcpServer.StartAsync(ExpectedToken);
        await using var thirdParty = await ScenarioOAuthMcpServer.StartAsync(ExpectedToken);
        const string firstPartyName = "scenario-client-first-party";
        const string thirdPartyName = "scenario-client-third-party";
        var requests = Channel.CreateUnbounded<McpAuthContext>();

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            ClientName = "scenario-client",
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
    public async Task Should_Reconnect_With_Cached_Scenario_Token_Then_Return_Interactive_Oauth_Url()
    {
        await using var oauthServer = await ScenarioOAuthMcpServer.StartAsync(ExpectedToken);
        const string serverName = "scenario-client-oauth-reconnect";
        var tokenRequests = 0;

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            ClientName = "scenario-client",
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
        var tokenRequestsAfterInitialConnect = Volatile.Read(ref tokenRequests);
        Assert.True(tokenRequestsAfterInitialConnect >= 1);
        var serverRequestsAfterInitialConnect = (await oauthServer.GetRequestsAsync()).Count;

        await session.Rpc.Mcp.RestartServerAsync(serverName);
        await TestHelper.WaitForConditionAsync(
            async () => (await oauthServer.GetRequestsAsync()).Count > serverRequestsAfterInitialConnect,
            timeout: EventTimeout,
            pollInterval: TimeSpan.FromMilliseconds(50),
            timeoutMessage: "Timed out waiting for the MCP server to reconnect after restart.");
        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);
        Assert.Equal(tokenRequestsAfterInitialConnect, Volatile.Read(ref tokenRequests));
        Assert.Contains(
            await oauthServer.GetRequestsAsync(),
            request => request.Authorization == $"Bearer {ExpectedToken}");

        var cached = await session.Rpc.Mcp.Oauth.ProbeAsync(serverName);
        Assert.IsType<McpOauthProbeResultAuthenticated>(cached);

        var interactive = await session.Rpc.Mcp.Oauth.LoginAsync(
            serverName,
            forceReauth: true,
            clientName: "scenario client",
            callbackSuccessMessage: "Return to your application.",
            clientId: "scenario-client-client",
            publicClient: true);
        Assert.NotNull(interactive.AuthorizationUrl);
        Assert.StartsWith($"{oauthServer.Url}/authorize", interactive.AuthorizationUrl, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Should_Manage_And_Discover_Scenario_Mcp_Config_Lifecycle()
    {
        var serverName = $"scenario-client-config-{Guid.NewGuid():N}";
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
                Env = new Dictionary<string, string> { ["SCENARIO_CONFIG_VERSION"] = "2" },
                Tools = ["*"],
            });
            var updated = GetServerConfig(await Client.Rpc.Mcp.Config.ListAsync(), serverName);
            Assert.Equal("2", updated.GetProperty("env").GetProperty("SCENARIO_CONFIG_VERSION").GetString());

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
    public async Task Should_List_Mcp_App_Visible_Tools_And_Read_Resource()
    {
        const string serverName = "scenario-mcp-app";
        const string resourceUri = "ui://scenario/app";
        var environment = Ctx.GetEnvironment();
        environment["COPILOT_MCP_APPS"] = "true";
        environment["MCP_APPS"] = "true";
        await using var client = Ctx.CreateClient(environment: environment);
        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            EnableMcpApps = true,
            McpServers = new Dictionary<string, McpServerConfig>
            {
                [serverName] = new McpStdioServerConfig
                {
                    Command = "node",
                    Args = [Path.Join(FindTestHarnessDir(), "test-mcp-app-server.mjs")],
                    Tools = ["*"],
                },
            },
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);

        var tools = await session.Rpc.Mcp.Apps.ListToolsAsync(
            serverName,
            originServerName: serverName);
        var tool = Assert.Single(tools.Tools);
        Assert.Equal("app_visible", tool["name"].GetString());
        Assert.Equal(
            ["model", "app"],
            tool["_meta"].GetProperty("ui.visibility")
                .EnumerateArray().Select(value => value.GetString()));

        using var value = JsonDocument.Parse("\"scenario-value\"");
        var call = await session.Rpc.Mcp.Apps.CallToolAsync(
            serverName,
            "app_visible",
            originServerName: serverName,
            arguments: new Dictionary<string, JsonElement>
            {
                ["value"] = value.RootElement.Clone(),
            });
        Assert.Equal(
            "APP_VISIBLE:scenario-value",
            call["content"][0].GetProperty("text").GetString());

        var resource = Assert.Single(
            (await session.Rpc.Mcp.Apps.ReadResourceAsync(serverName, resourceUri)).Contents);
        Assert.Equal(resourceUri, resource.Uri);
        Assert.Equal("text/html", resource.MimeType);
        Assert.Equal("<html><body>SCENARIO_MCP_APP</body></html>", resource.Text);
        Assert.Equal(
            "https://api.example.test",
            Assert.Single(
                resource.Meta!["ui.csp"].GetProperty("connectDomains").EnumerateArray())
                .GetString());
    }

    [Fact]
    public async Task Should_Enforce_Mcp_App_Origin_Server()
    {
        const string serverName = "scenario-client-origin";
        const string otherServerName = "scenario-client-other-origin";
        var servers = CreateTestMcpServers(serverName, otherServerName);
        ((McpStdioServerConfig)servers[serverName]).Env =
            new Dictionary<string, string> { ["SCENARIO_ORIGIN_VALUE"] = "origin-ok" };

        var environment = Ctx.GetEnvironment();
        environment["COPILOT_MCP_APPS"] = "true";
        environment["MCP_APPS"] = "true";
        await using var client = Ctx.CreateClient(environment: environment);
        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            ClientName = "scenario-client",
            EnableMcpApps = true,
            McpServers = servers,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);
        await WaitForMcpServerStatusAsync(session, otherServerName, McpServerStatus.Connected);

        using var argument = JsonDocument.Parse("""{"name":"SCENARIO_ORIGIN_VALUE"}""");
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
    public async Task Should_Preserve_Disabled_Scenario_Mcp_Servers_Across_Reload_And_Resume()
    {
        const string enabledName = "scenario-client-enabled-mcp";
        const string disabledName = "scenario-client-disabled-mcp";
        var client1 = Ctx.CreateClient();
        var session1 = await Ctx.CreateSessionAsync(client1, new SessionConfig
        {
            ClientName = "scenario-client",
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
            Prompt = "Reply with exactly SCENARIO_MCP_DISABLED_STATE.",
        });
        Assert.Contains("SCENARIO_MCP_DISABLED_STATE", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);
        await session1.Rpc.SuspendAsync();
        await session1.DisposeAsync();
        await client1.ForceStopAsync();

        await using var client2 = Ctx.CreateClient();
        await using var session2 = await Ctx.ResumeSessionAsync(client2, sessionId, new ResumeSessionConfig
        {
            ClientName = "scenario-client",
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

    private sealed class ScenarioOAuthMcpServer : IAsyncDisposable
    {
        private readonly Process _process;
        private readonly HttpClient _http = new();

        private ScenarioOAuthMcpServer(Process process, string url)
        {
            _process = process;
            Url = url;
        }

        public string Url { get; }

        public static async Task<ScenarioOAuthMcpServer> StartAsync(string expectedToken)
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
                    return new ScenarioOAuthMcpServer(process, line["Listening: ".Length..]);
                }
            }

            throw new TimeoutException($"Timed out waiting for OAuth MCP server: {await stderr}");
        }

        public async Task<List<ScenarioOAuthRequest>> GetRequestsAsync()
        {
            var json = await _http.GetStringAsync($"{Url}/__requests");
            using var document = JsonDocument.Parse(json);
            return document.RootElement.EnumerateArray()
                .Select(element => new ScenarioOAuthRequest(
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

    private sealed record ScenarioOAuthRequest(string? Authorization, string Path);
}
