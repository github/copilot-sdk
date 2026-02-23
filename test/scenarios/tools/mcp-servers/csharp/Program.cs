using GitHub.Copilot.SDK;

using var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
    GitHubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
});

await client.StartAsync();

try
{
    var mcpServers = new Dictionary<string, object>();
    var mcpServerCmd = Environment.GetEnvironmentVariable("MCP_SERVER_CMD");
    if (!string.IsNullOrEmpty(mcpServerCmd))
    {
        var mcpArgs = Environment.GetEnvironmentVariable("MCP_SERVER_ARGS");
        mcpServers["example"] = new Dictionary<string, object>
        {
            { "type", "stdio" },
            { "command", mcpServerCmd },
            { "args", string.IsNullOrEmpty(mcpArgs) ? Array.Empty<string>() : mcpArgs.Split(' ') },
        };
    }

    var config = new SessionConfig
    {
        Model = "claude-haiku-4.5",
        AvailableTools = new List<string>(),
        SystemMessage = new SystemMessageConfig
        {
            Mode = SystemMessageMode.Replace,
            Content = "You are a helpful assistant. Answer questions concisely.",
        },
    };

    if (mcpServers.Count > 0)
    {
        config.McpServers = mcpServers;
    }

    await using var session = await client.CreateSessionAsync(config);

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "What is the capital of France?",
    });

    if (response != null)
    {
        Console.WriteLine(response.Data?.Content);
    }

    if (mcpServers.Count > 0)
    {
        Console.WriteLine($"\nMCP servers configured: {string.Join(", ", mcpServers.Keys)}");
    }
    else
    {
        Console.WriteLine("\nNo MCP servers configured (set MCP_SERVER_CMD to test with a real server)");
    }
}
finally
{
    await client.StopAsync();
}
