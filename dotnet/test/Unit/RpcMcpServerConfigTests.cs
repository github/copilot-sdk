/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Text.Json;
using System.Text.Json.Nodes;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Theory]
    [InlineData("session.mcp.startServer", "stdio-command-args")]
    [InlineData("session.mcp.startServer", "http-url")]
    [InlineData("session.mcp.startServer", "stdio-all-fields")]
    [InlineData("session.mcp.startServer", "http-all-fields")]
    [InlineData("session.mcp.startServer", "dictionary-explicit-null")]
    [InlineData("session.mcp.restartServer", "stdio-command-args")]
    [InlineData("session.mcp.restartServer", "http-url")]
    [InlineData("session.mcp.restartServer", "stdio-all-fields")]
    [InlineData("session.mcp.restartServer", "http-all-fields")]
    [InlineData("session.mcp.restartServer", "dictionary-explicit-null")]
    [InlineData("mcp.config.add", "stdio-command-args")]
    [InlineData("mcp.config.add", "http-url")]
    [InlineData("mcp.config.add", "stdio-all-fields")]
    [InlineData("mcp.config.add", "http-all-fields")]
    [InlineData("mcp.config.add", "dictionary-explicit-null")]
    [InlineData("mcp.config.update", "stdio-command-args")]
    [InlineData("mcp.config.update", "http-url")]
    [InlineData("mcp.config.update", "stdio-all-fields")]
    [InlineData("mcp.config.update", "http-all-fields")]
    [InlineData("mcp.config.update", "dictionary-explicit-null")]
    public async Task Mcp_Server_Config_Rpc_Payload_Carries_Only_The_Fields_The_Caller_Set(string method, string shape)
    {
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = McpServerConfigResponse;
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();
        var (config, expected) = McpServerConfigShape(shape);

        switch (method)
        {
            case "session.mcp.startServer":
                await session.Rpc.Mcp.StartServerAsync("server", config);
                break;
            case "session.mcp.restartServer":
                await session.Rpc.Mcp.RestartServerAsync("server", config);
                break;
            case "mcp.config.add":
                await client.Rpc.Mcp.Config.AddAsync("server", config);
                break;
            case "mcp.config.update":
                await client.Rpc.Mcp.Config.UpdateAsync("server", config);
                break;
            default:
                throw new ArgumentOutOfRangeException(nameof(method));
        }

        var request = Assert.Single(server.Requests);
        Assert.Equal(method, request.Method);
        AssertJsonEqual(expected, request.Params.GetProperty("config"));
    }

    [Fact]
    public async Task Session_Config_Mcp_Servers_Send_The_Type_And_Only_The_Fields_The_Caller_Set()
    {
        await using var server = await FakeCopilotServer.StartAsync();
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });

        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            McpServers = new Dictionary<string, McpServerConfig>
            {
                ["stdio"] = new McpStdioServerConfig { Command = "node", Args = ["server.js"] },
                ["http"] = new McpHttpServerConfig { Url = "http://127.0.0.1:9/mcp" },
            },
        });

        var create = Assert.Single(server.Requests, request => request.Method == "session.create");
        var servers = create.Params.GetProperty("mcpServers");
        AssertJsonEqual("""{"type":"stdio","command":"node","args":["server.js"]}""", servers.GetProperty("stdio"));
        AssertJsonEqual("""{"type":"http","url":"http://127.0.0.1:9/mcp"}""", servers.GetProperty("http"));
    }

    private static (object Config, string Expected) McpServerConfigShape(string shape) => shape switch
    {
        "stdio-command-args" => (
            new McpStdioServerConfig { Command = "node", Args = ["server.js"] },
            """{"command":"node","args":["server.js"]}"""),
        "http-url" => (
            new McpHttpServerConfig { Url = "http://127.0.0.1:9/mcp" },
            """{"url":"http://127.0.0.1:9/mcp"}"""),
        "stdio-all-fields" => (
            new McpStdioServerConfig
            {
                Command = "node",
                Args = ["server.js"],
                Env = new Dictionary<string, string> { ["MODE"] = "test" },
                WorkingDirectory = "/work",
                Timeout = 30000,
                Tools = ["*"],
            },
            """{"command":"node","args":["server.js"],"env":{"MODE":"test"},"cwd":"/work","timeout":30000,"tools":["*"]}"""),
        "http-all-fields" => (
            new McpHttpServerConfig
            {
                Url = "http://127.0.0.1:9/mcp",
                Headers = new Dictionary<string, string> { ["X-Mode"] = "test" },
                OauthClientId = "client",
                OauthPublicClient = false,
                OauthGrantType = McpHttpServerConfigOauthGrantType.ClientCredentials,
                OauthScopes = ["read"],
                Timeout = 3000,
                Tools = [],
            },
            """{"url":"http://127.0.0.1:9/mcp","headers":{"X-Mode":"test"},"oauthClientId":"client","oauthPublicClient":false,"oauthGrantType":"client_credentials","oauthScopes":["read"],"timeout":3000,"tools":[]}"""),
        "dictionary-explicit-null" => (
            new Dictionary<string, object?> { ["command"] = "node", ["timeout"] = null },
            """{"command":"node","timeout":null}"""),
        _ => throw new ArgumentOutOfRangeException(nameof(shape)),
    };

    private static void AssertJsonEqual(string expected, JsonElement actual) =>
        Assert.True(
            JsonNode.DeepEquals(JsonNode.Parse(expected), JsonNode.Parse(actual.GetRawText())),
            $"Expected {expected} but the request carried {actual.GetRawText()}");

    private static Dictionary<string, object?> McpServerConfigResponse(RpcRequestRecord request) =>
        request.Method switch
        {
            "session.mcp.startServer" or "session.mcp.restartServer" or "mcp.config.add" or "mcp.config.update" =>
                new Dictionary<string, object?>(),
            _ => throw new InvalidOperationException($"Unexpected MCP server config request '{request.Method}'."),
        };
}
#endif
