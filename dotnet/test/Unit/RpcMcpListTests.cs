/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Text.Json;
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    private static readonly string[] ExpectedMcpLifecycleStatuses = ["not_configured", "connected", "needs-auth", "stopped", "failed"];

    [Fact]
    public async Task Session_Rpc_Mcp_ListConfigured_Preserves_Runtime_Lifecycle()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        using var response = JsonDocument.Parse("""
            {
                "servers": [
                    { "name": "cold", "enabled": true, "live": { "status": "not_configured" } },
                    { "name": "old-connection", "enabled": false, "live": { "status": "connected" } },
                    { "name": "auth", "enabled": true, "live": { "status": "needs-auth" } },
                    { "name": "stopped", "enabled": true, "live": { "status": "stopped" } },
                    { "name": "failed", "enabled": true, "live": { "status": "failed", "error": "connection failed" } }
                ]
            }
            """);
        server.ResponseFactory = _ => response.RootElement;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        var inventory = await session.Rpc.Mcp.ListConfiguredAsync();
        Assert.Equal(
            ExpectedMcpLifecycleStatuses,
            inventory.Servers.Select(entry => entry.Live.Status.Value));
        Assert.False(inventory.Servers[1].Enabled);
        Assert.Equal("connection failed", inventory.Servers[4].Live.Error);
        AssertConnectorRequest(Assert.Single(server.Requests), "session.mcp.listConfigured", session.SessionId);
    }

    [Fact]
    public async Task Session_Rpc_Mcp_List_And_ListConfigured_Use_Parameterless_Wire_Contracts()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = _ => new Dictionary<string, object?> { ["servers"] = Array.Empty<object>() };
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        var mcp = session.Rpc.Mcp;
        Func<CancellationToken, Task<McpServerList>> legacyList = mcp.ListAsync;
        Func<CancellationToken, Task<McpConfiguredServerList>> configuredList = mcp.ListConfiguredAsync;
        Assert.NotNull(mcp.GetType().GetMethod("ListAsync", [typeof(CancellationToken)]));
        Assert.NotNull(mcp.GetType().GetMethod("ListConfiguredAsync", [typeof(CancellationToken)]));
        await mcp.ListAsync();
        await legacyList(CancellationToken.None);
        await mcp.ListConfiguredAsync();
        await configuredList(CancellationToken.None);

        Assert.Collection(
            server.Requests,
            request => AssertConnectorRequest(request, "session.mcp.list", session.SessionId),
            request => AssertConnectorRequest(request, "session.mcp.list", session.SessionId),
            request => AssertConnectorRequest(request, "session.mcp.listConfigured", session.SessionId),
            request => AssertConnectorRequest(request, "session.mcp.listConfigured", session.SessionId));
    }
}
#endif
