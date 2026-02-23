using GitHub.Copilot.SDK;

var options = new CopilotClientOptions
{
    GitHubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
};

var cliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH");
if (!string.IsNullOrEmpty(cliPath))
{
    options.CliPath = cliPath;
}

using var client = new CopilotClient(options);

await client.StartAsync();

try
{
    await using var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "claude-haiku-4.5",
        Streaming = true,
    });

    var chunkCount = 0;
    using var subscription = session.On(evt =>
    {
        if (evt is AssistantMessageDeltaEvent)
        {
            chunkCount++;
        }
    });

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "What is the capital of France?",
    });

    if (response != null)
    {
        Console.WriteLine(response.Data.Content);
    }
    Console.WriteLine($"\nStreaming chunks received: {chunkCount}");
}
finally
{
    await client.StopAsync();
}
