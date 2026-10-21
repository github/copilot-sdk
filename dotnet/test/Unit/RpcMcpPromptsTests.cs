/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed partial class ClientSessionLifetimeTests
{
    [Fact]
    public async Task Mcp_Prompts_Bind_Session_And_Preserve_Omitted_And_Empty_Arguments()
    {
        // TypeScript owns runtime behavior; this fixture checks .NET wire encoding.
        await using var server = await FakeCopilotServer.StartAsync();
        server.ResponseFactory = request => request.Method switch
        {
            "session.mcp.prompts.list" => new Dictionary<string, object?>
            {
                ["prompts"] = Array.Empty<object>(),
            },
            "session.mcp.prompts.get" => new Dictionary<string, object?>
            {
                ["messages"] = Array.Empty<object>(),
            },
            _ => throw new InvalidOperationException($"Unexpected prompt request '{request.Method}'."),
        };
        await using var client = new CopilotClient(new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(server.Url)
        });
        await using var session = await client.CreateSessionAsync(new SessionConfig());
        server.ClearRequests();

        Assert.Empty((await session.Rpc.Mcp.Prompts.ListAsync("fixture")).Prompts);
        await session.Rpc.Mcp.Prompts.ListAsync("fixture", "page:2/opaque+cursor=");
        Assert.Empty((await session.Rpc.Mcp.Prompts.GetAsync("fixture", "echo")).Messages);
        await session.Rpc.Mcp.Prompts.GetAsync("fixture", "echo", new Dictionary<string, string>());
        await session.Rpc.Mcp.Prompts.GetAsync("fixture", "echo",
            new Dictionary<string, string> { ["topic"] = "日本語", ["style"] = "" });

        var requests = server.Requests.ToArray();
        Assert.Equal(5, requests.Length);
        for (var i = 0; i < requests.Length; i++)
        {
            var request = requests[i];
            Assert.Equal(i < 2 ? "session.mcp.prompts.list" : "session.mcp.prompts.get", request.Method);
            Assert.Equal(session.SessionId, request.Params.GetProperty("sessionId").GetString());
            Assert.Equal("fixture", request.Params.GetProperty("serverName").GetString());
            if (i >= 2)
            {
                Assert.Equal("echo", request.Params.GetProperty("promptName").GetString());
            }
        }
        Assert.False(requests[0].Params.TryGetProperty("cursor", out _));
        Assert.Equal("page:2/opaque+cursor=", requests[1].Params.GetProperty("cursor").GetString());
        Assert.False(requests[2].Params.TryGetProperty("arguments", out _));
        Assert.Empty(requests[3].Params.GetProperty("arguments").EnumerateObject());
        var arguments = requests[4].Params.GetProperty("arguments");
        Assert.Equal("日本語", arguments.GetProperty("topic").GetString());
        Assert.Equal("", arguments.GetProperty("style").GetString());
    }
}
#endif
