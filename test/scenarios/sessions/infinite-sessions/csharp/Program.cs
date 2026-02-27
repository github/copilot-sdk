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
        AvailableTools = new List<string>(),
        SystemMessage = new SystemMessageConfig
        {
            Mode = SystemMessageMode.Replace,
            Content = "You are a helpful assistant. Answer concisely in one sentence.",
        },
        InfiniteSessions = new InfiniteSessionConfig
        {
            Enabled = true,
            BackgroundCompactionThreshold = 0.80,
            BufferExhaustionThreshold = 0.95,
        },
    });

    var prompts = new[]
    {
        "What is the capital of France?",
        "What is the capital of Japan?",
        "What is the capital of Brazil?",
    };

    foreach (var prompt in prompts)
    {
        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = prompt,
        });

        if (response != null)
        {
            Console.WriteLine($"Q: {prompt}");
            Console.WriteLine($"A: {response.Data?.Content}\n");
        }
    }

    Console.WriteLine("Infinite sessions test complete — all messages processed successfully");
}
finally
{
    await client.StopAsync();
}
