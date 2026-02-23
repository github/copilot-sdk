using GitHub.Copilot.SDK;

using var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
    GitHubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
});

await client.StartAsync();

try
{
    // 1. Create a session
    await using var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "claude-haiku-4.5",
        AvailableTools = new List<string>(),
    });

    // 2. Send the secret word
    await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "Remember this: the secret word is PINEAPPLE.",
    });

    // 3. Get the session ID
    var sessionId = session.SessionId;

    // 4. Resume the session with the same ID
    await using var resumed = await client.ResumeSessionAsync(sessionId);
    Console.WriteLine("Session resumed");

    // 5. Ask for the secret word
    var response = await resumed.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "What was the secret word I told you?",
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
