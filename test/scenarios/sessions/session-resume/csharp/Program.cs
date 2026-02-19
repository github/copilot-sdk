using GitHub.Copilot.SDK;

var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
    GithubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
});

await client.StartAsync();

try
{
    // 1. Create a session
    var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "gpt-4.1",
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
    var resumed = await client.ResumeSessionAsync(sessionId);

    // 5. Ask for the secret word
    var response = await resumed.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "What was the secret word I told you?",
    });

    if (response != null)
    {
        Console.WriteLine(response.Data?.Content);
    }

    await resumed.DisposeAsync();
}
finally
{
    await client.StopAsync();
}
