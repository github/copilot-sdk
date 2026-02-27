using GitHub.Copilot.SDK;

using var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
    GitHubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
});

await client.StartAsync();

try
{
    await using var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "claude-haiku-4.5",
    });

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "Use the grep tool to search for the word 'SDK' in README.md and show the matching lines.",
    });

    if (response != null)
    {
        Console.WriteLine($"Response: {response.Data?.Content}");
    }

    Console.WriteLine("Default mode test complete");

}
finally
{
    await client.StopAsync();
}
