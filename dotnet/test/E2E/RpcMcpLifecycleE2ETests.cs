/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// E2E coverage for the session-scoped MCP lifecycle RPC methods that were previously untested:
/// listTools, isServerRunning, stopServer, startServer, restartServer, registerExternalClient,
/// unregisterExternalClient, reloadWithConfig, configureGitHub, and oauth.respond.
/// </summary>
public class RpcMcpLifecycleE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_mcp_lifecycle", output)
{
    [Fact]
    public async Task Should_List_Tools_And_Report_Running_Status_For_Connected_Server()
    {
        const string serverName = "rpc-lifecycle-list-server";
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers(serverName),
        });
        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);

        var tools = await session.Rpc.Mcp.ListToolsAsync(serverName);
        Assert.NotNull(tools.Tools);
        Assert.NotEmpty(tools.Tools);
        Assert.All(tools.Tools, tool => Assert.False(string.IsNullOrWhiteSpace(tool.Name)));

        // A connected server reports running; a name that was never configured does not.
        Assert.True((await session.Rpc.Mcp.IsServerRunningAsync(serverName)).Running);
        Assert.False((await session.Rpc.Mcp.IsServerRunningAsync($"missing-{Guid.NewGuid():N}")).Running);
    }

    [Fact]
    public async Task Should_Throw_When_Listing_Tools_For_Unconnected_Server()
    {
        const string serverName = "rpc-lifecycle-unconnected-host";
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers(serverName),
        });
        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);

        // The MCP host is initialized (a server is connected), but the requested server is not,
        // so listTools reaches the runtime and fails with a domain error rather than "Unhandled method".
        var ex = await Assert.ThrowsAnyAsync<Exception>(
            () => session.Rpc.Mcp.ListToolsAsync($"missing-{Guid.NewGuid():N}"));
        var message = ex.ToString();
        AssertNotUnhandledMethod(message);
        Assert.Contains("not connected", message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Should_Stop_Running_Mcp_Server()
    {
        const string serverName = "rpc-lifecycle-stop-server";
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers(serverName),
        });
        await WaitForMcpServerStatusAsync(session, serverName, McpServerStatus.Connected);
        Assert.True((await session.Rpc.Mcp.IsServerRunningAsync(serverName)).Running);

        await session.Rpc.Mcp.StopServerAsync(serverName);

        await WaitForMcpRunningAsync(session, serverName, expectedRunning: false);
    }

    [Fact]
    public async Task Should_Start_And_Restart_Mcp_Server()
    {
        const string hostServer = "rpc-lifecycle-host-server";
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers(hostServer),
        });
        await WaitForMcpServerStatusAsync(session, hostServer, McpServerStatus.Connected);

        // Start a brand-new server through the lifecycle API, reusing the exact stdio config shape
        // the bulk session-config path uses so the runtime accepts and connects it.
        const string startedServer = "rpc-lifecycle-started-server";
        var config = CreateTestMcpServers(startedServer)[startedServer];

        await session.Rpc.Mcp.StartServerAsync(startedServer, config);
        await WaitForMcpRunningAsync(session, startedServer, expectedRunning: true);

        // The freshly started server exposes its tools just like a config-provided server.
        var tools = await session.Rpc.Mcp.ListToolsAsync(startedServer);
        Assert.NotEmpty(tools.Tools);

        // Restart stops then starts the same server; it must end up running again.
        await session.Rpc.Mcp.RestartServerAsync(startedServer, config);
        await WaitForMcpRunningAsync(session, startedServer, expectedRunning: true);
    }

    [Fact]
    public async Task Should_Register_And_Unregister_External_Mcp_Client()
    {
        const string hostServer = "rpc-lifecycle-extclient-host";
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers(hostServer),
        });
        await WaitForMcpServerStatusAsync(session, hostServer, McpServerStatus.Connected);

        const string externalName = "rpc-lifecycle-external-client";
        Assert.False((await session.Rpc.Mcp.IsServerRunningAsync(externalName)).Running);

        // The runtime stores the supplied client/transport handles on the host registry, so the
        // registered name immediately reports as running until it is unregistered again.
        await session.Rpc.Mcp.RegisterExternalClientAsync(
            externalName,
            client: new Dictionary<string, object> { ["id"] = externalName },
            transport: new Dictionary<string, object> { ["kind"] = "in-process" },
            config: new Dictionary<string, object> { ["command"] = "noop" });
        Assert.True((await session.Rpc.Mcp.IsServerRunningAsync(externalName)).Running);

        await session.Rpc.Mcp.UnregisterExternalClientAsync(externalName);
        Assert.False((await session.Rpc.Mcp.IsServerRunningAsync(externalName)).Running);
    }

    [Fact]
    public async Task Should_Reload_Mcp_Servers_With_Config()
    {
        const string hostServer = "rpc-lifecycle-reload-host";
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers(hostServer),
        });
        await WaitForMcpServerStatusAsync(session, hostServer, McpServerStatus.Connected);

        // reloadWithConfig drives the runtime's reloadMcpServers with an explicit host config and
        // returns the startup filtering result. Reloading an empty server set is a valid no-op.
        var result = await session.Rpc.Mcp.ReloadWithConfigAsync(new Dictionary<string, object>
        {
            ["mcpServers"] = new Dictionary<string, object>(),
            ["disabledServers"] = new List<string>(),
        });

        Assert.NotNull(result);
        Assert.NotNull(result.FilteredServers);
        Assert.Empty(result.FilteredServers);
    }

    [Fact]
    public async Task Should_Configure_GitHub_Mcp_Server()
    {
        const string hostServer = "rpc-lifecycle-configure-host";
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers(hostServer),
        });
        await WaitForMcpServerStatusAsync(session, hostServer, McpServerStatus.Connected);

        // configureGitHub forwards a typed auth-info union to the runtime. An "api-key" auth info is
        // a recognized type that the runtime declines to act on, so configuration is left unchanged
        // (changed=false) while still proving the method is wired through to the handler.
        var result = await session.Rpc.Mcp.ConfigureGitHubAsync(new Dictionary<string, object?>
        {
            ["type"] = "api-key",
        });

        Assert.NotNull(result);
        Assert.False(result.Changed);
    }

    [Fact]
    public async Task Should_Respond_To_Mcp_Oauth_Request_Without_Pending_Request()
    {
        const string hostServer = "rpc-lifecycle-oauth-host";
        await using var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers(hostServer),
        });
        await WaitForMcpServerStatusAsync(session, hostServer, McpServerStatus.Connected);

        // With no pending OAuth request, the runtime's respondToMcpOAuth is a tolerant no-op: it
        // looks up the request id, finds nothing, and returns an empty result without throwing. The
        // call must reach the runtime and complete successfully, proving the method is wired.
        var result = await session.Rpc.Mcp.Oauth.RespondAsync($"missing-{Guid.NewGuid():N}");
        Assert.NotNull(result);
    }

    private static Task WaitForMcpRunningAsync(CopilotSession session, string serverName, bool expectedRunning) =>
        Harness.TestHelper.WaitForConditionAsync(
            async () => (await session.Rpc.Mcp.IsServerRunningAsync(serverName)).Running == expectedRunning,
            timeout: TimeSpan.FromSeconds(60),
            pollInterval: TimeSpan.FromMilliseconds(200),
            timeoutMessage: $"{serverName} running={expectedRunning}");

    private static void AssertNotUnhandledMethod(string message)
    {
        Assert.DoesNotContain("Unhandled method", message, StringComparison.OrdinalIgnoreCase);
    }
}
