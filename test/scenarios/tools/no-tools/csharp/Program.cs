using GitHub.Copilot.SDK;

const string SystemPrompt = """
    You are a minimal assistant with no tools available.
    You cannot execute code, read files, edit files, search, or perform any actions.
    You can only respond with text based on your training data.
    If asked about your capabilities or tools, clearly state that you have no tools available.
    """;

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
        SystemMessage = new SystemMessageConfig
        {
            Mode = SystemMessageMode.Replace,
            Content = SystemPrompt,
        },
        AvailableTools = [],
    });

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "Use the bash tool to run 'echo hello'.",
    });

    if (response != null)
    {
        Console.WriteLine(response.Data?.Content);
    }
}
finally
{
    await client.StopAsync();
}
