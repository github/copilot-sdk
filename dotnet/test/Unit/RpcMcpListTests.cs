/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using GitHub.Copilot.Rpc;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
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
