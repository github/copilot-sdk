/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class ServerMcpConfigRpcTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "server_mcp_config_rpc", output)
{
    [Fact]
    public async Task Should_Call_Server_Mcp_Config_Rpcs()
    {
        await Client.StartAsync();

        var serverName = $"sdk-test-{Guid.NewGuid():N}";
        var config = new Dictionary<string, object>
        {
            ["command"] = "node",
            ["args"] = Array.Empty<string>(),
        };
        var updatedConfig = new Dictionary<string, object>
        {
            ["command"] = "node",
            ["args"] = new[] { "--version" },
        };

        var initial = await Client.Rpc.Mcp.Config.ListAsync();
        Assert.DoesNotContain(serverName, initial.Servers.Keys);

        try
        {
            await Client.Rpc.Mcp.Config.AddAsync(serverName, config);
            var afterAdd = await Client.Rpc.Mcp.Config.ListAsync();
            Assert.Contains(serverName, afterAdd.Servers.Keys);

            await Client.Rpc.Mcp.Config.UpdateAsync(serverName, updatedConfig);
            await Client.Rpc.Mcp.Config.DisableAsync([serverName]);
            await Client.Rpc.Mcp.Config.EnableAsync([serverName]);
        }
        finally
        {
            await Client.Rpc.Mcp.Config.RemoveAsync(serverName);
        }

        var afterRemove = await Client.Rpc.Mcp.Config.ListAsync();
        Assert.DoesNotContain(serverName, afterRemove.Servers.Keys);
    }
}
