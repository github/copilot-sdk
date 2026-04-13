using System.Text.Json;
using GitHub.Copilot.SDK;
using Microsoft.Extensions.AI;

var cliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH");

using var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = cliPath,
    GitHubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
});

await client.StartAsync();

try
{
    var analyzeCodebase = AIFunctionFactory.Create(
        (string query) => $"Analysis result for: {query}",
        new AIFunctionFactoryOptions
        {
            Name = "analyze-codebase",
            Description = "Performs deep analysis of the codebase",
        });

    await using var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "claude-haiku-4.5",
        Tools = [analyzeCodebase],
        defaultAgent = new DefaultAgentConfig
        {
            ExcludedTools = ["analyze-codebase"],
        },
        CustomAgents =
        [
            new CustomAgentConfig
            {
                Name = "researcher",
                DisplayName = "Research Agent",
                Description = "A research agent that can only read and search files, not modify them",
                Tools = ["grep", "glob", "view", "analyze-codebase"],
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
