using GitHub.Copilot.SDK;

var options = new CopilotClientOptions
{
    GithubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
};

var cliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH");
if (!string.IsNullOrEmpty(cliPath))
{
    options.CliPath = cliPath;
}

await using var client = new CopilotClient(options);
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-4.1",
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
