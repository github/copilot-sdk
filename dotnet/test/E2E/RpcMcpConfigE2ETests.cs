/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text.Json;
using GitHub.Copilot.SDK.Rpc;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

public class RpcMcpConfigE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_mcp_config", output)
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
            var afterUpdate = await Client.Rpc.Mcp.Config.ListAsync();
            var updated = GetServerConfig(afterUpdate, serverName);
            Assert.Equal("node", updated.GetProperty("command").GetString());
            Assert.Equal("--version", updated.GetProperty("args")[0].GetString());

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

    [Fact]
    public async Task Should_RoundTrip_Http_Mcp_Oauth_Config_Rpc()
    {
        await Client.StartAsync();

        var serverName = $"sdk-http-oauth-{Guid.NewGuid():N}";
        var config = new McpHttpServerConfig
        {
            Url = "https://example.com/mcp",
            Headers = new Dictionary<string, string> { ["Authorization"] = "Bearer token" },
            OauthClientId = "client-id",
            OauthPublicClient = false,
            OauthGrantType = McpHttpServerConfigOauthGrantType.ClientCredentials,
            Tools = ["*"],
            Timeout = 3000,
        };
        var updatedConfig = new McpHttpServerConfig
        {
            Url = "https://example.com/updated-mcp",
            OauthClientId = "updated-client-id",
            OauthPublicClient = true,
            OauthGrantType = McpHttpServerConfigOauthGrantType.AuthorizationCode,
            Tools = ["updated-tool"],
            Timeout = 4000,
        };

        try
        {
            await Client.Rpc.Mcp.Config.AddAsync(serverName, config);
            var afterAdd = await Client.Rpc.Mcp.Config.ListAsync();
            var added = GetServerConfig(afterAdd, serverName);
            Assert.Equal("http", added.GetProperty("type").GetString());
            Assert.Equal("https://example.com/mcp", added.GetProperty("url").GetString());
            Assert.Equal("Bearer token", added.GetProperty("headers").GetProperty("Authorization").GetString());
            Assert.Equal("client-id", added.GetProperty("oauthClientId").GetString());
            Assert.False(added.GetProperty("oauthPublicClient").GetBoolean());
            Assert.Equal("client_credentials", added.GetProperty("oauthGrantType").GetString());

            await Client.Rpc.Mcp.Config.UpdateAsync(serverName, updatedConfig);
            var afterUpdate = await Client.Rpc.Mcp.Config.ListAsync();
            var updated = GetServerConfig(afterUpdate, serverName);
            Assert.Equal("https://example.com/updated-mcp", updated.GetProperty("url").GetString());
            Assert.Equal("updated-client-id", updated.GetProperty("oauthClientId").GetString());
            Assert.True(updated.GetProperty("oauthPublicClient").GetBoolean());
            Assert.Equal("authorization_code", updated.GetProperty("oauthGrantType").GetString());
            Assert.Equal("updated-tool", updated.GetProperty("tools")[0].GetString());
            Assert.Equal(4000, updated.GetProperty("timeout").GetInt32());
        }
        finally
        {
            await Client.Rpc.Mcp.Config.RemoveAsync(serverName);
        }

        var afterRemove = await Client.Rpc.Mcp.Config.ListAsync();
        Assert.DoesNotContain(serverName, afterRemove.Servers.Keys);
    }

    private static JsonElement GetServerConfig(McpConfigList list, string serverName)
    {
        Assert.True(
            list.Servers.TryGetValue(serverName, out var rawConfig),
            $"Expected MCP server '{serverName}' to be present.");
        return Assert.IsType<JsonElement>(rawConfig);
    }
}
