using GitHub.Copilot.SDK;

var cliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH");

using var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = cliPath,
    GitHubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
});

await client.StartAsync();

try
{
    await using var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "claude-haiku-4.5",
        CustomAgents =
        [
            new CustomAgentConfig
            {
                Name = "researcher",
                DisplayName = "Research Agent",
                Description = "A research agent that can only read and search files, not modify them",
                Tools = ["grep", "glob", "view"],
                Prompt = "You are a research assistant. You can search and read files but cannot modify anything. When asked about your capabilities, list the tools you have access to.",
            },
        ],
    });

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "What custom agents are available? Describe the researcher agent and its capabilities.",
    });

    if (response != null)
    {
        Console.WriteLine(response.Data.Content);
    }
}
finally
{
    await client.StopAsync();
}
