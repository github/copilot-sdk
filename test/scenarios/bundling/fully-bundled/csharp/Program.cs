using GitHub.Copilot;

using var client = new CopilotClient(new CopilotClientOptions
{
    Connection = RuntimeConnection.ForStdio(path: Environment.GetEnvironmentVariable("COPILOT_CLI_PATH")),
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
        Prompt = "What is the capital of France?",
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
