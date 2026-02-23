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
        SystemMessage = new SystemMessageConfig { Mode = SystemMessageMode.Replace, Content = "You are a helpful assistant. Answer questions about attached files concisely." },
        AvailableTools = [],
    });

    var sampleFile = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "..", "sample-data.txt"));

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "What languages are listed in the attached file?",
        Attachments =
        [
            new UserMessageDataAttachmentsItemFile { Path = sampleFile, DisplayName = "sample-data.txt" },
        ],
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
