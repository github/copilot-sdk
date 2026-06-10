/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// E2E coverage for the public session-scoped MCP lifecycle RPC methods:
/// listTools, isServerRunning, and stopServer.
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
