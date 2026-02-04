// Source: mcp/overview.md:143
using GitHub.Copilot.SDK;

await using var client = new CopilotClient();
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    McpServers = new Dictionary<string, object>
    {
        ["my-local-server"] = new McpLocalServerConfig
        {
            Type = "local",
            Command = "node",
            Args = new[] { "./mcp-server.js" },
            Tools = new[] { "*" },
        },
    },
});