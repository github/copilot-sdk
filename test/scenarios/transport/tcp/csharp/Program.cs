using GitHub.Copilot.SDK;

var cliUrl = Environment.GetEnvironmentVariable("COPILOT_CLI_URL") ?? "localhost:3000";

using var client = new CopilotClient(new CopilotClientOptions
{
    CliUrl = cliUrl,
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
    else
    {
        Console.WriteLine("(no response)");
    }
}
finally
{
    await client.StopAsync();
}
