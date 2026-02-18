using GitHub.Copilot.SDK;

var piratePrompt = "You are a pirate. Always respond in pirate speak. Say 'Arrr!' in every response. Use nautical terms and pirate slang throughout.";

await using var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
    GithubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
});

await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-4.1",
    SystemMessage = new SystemMessageConfig
    {
        Mode = SystemMessageMode.Replace,
        Content = piratePrompt,
    },
    AvailableTools = [],
});

var response = await session.SendAndWaitAsync(new MessageOptions
{
    Prompt = "What is the capital of France?",
});

if (response != null)
{
    Console.WriteLine(response.Data?.Content);
}
