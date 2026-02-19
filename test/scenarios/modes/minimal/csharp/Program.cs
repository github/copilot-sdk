using GitHub.Copilot.SDK;

using var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
    GithubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
});

await client.StartAsync();

try
{
    await using var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "gpt-4.1",
        AvailableTools = new List<string>(),
        SystemMessage = new SystemMessageConfig
        {
            Mode = SystemMessageMode.Replace,
            Content = "You have no tools. Respond with text only.",
        },
    });

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "What is the capital of France?",
    });

    if (response != null)
    {
        Console.WriteLine($"Response: {response.Data?.Content}");
    }

    Console.WriteLine("Minimal mode test complete");

}
finally
{
    await client.StopAsync();
}
