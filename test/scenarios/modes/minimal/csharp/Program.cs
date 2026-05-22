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
        AvailableTools = new List<string>(),
        SystemMessage = new SystemMessageConfig
        {
            Mode = SystemMessageMode.Replace,
            Content = "You have no tools. Respond with text only.",
        },
    });

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "Use the grep tool to search for 'SDK' in README.md.",
    });

    if (response != null)
    {
        Console.WriteLine($"Response: {response.Data?.Content}");
    }

    Console.WriteLine("Minimal mode test complete");

}
finally
{
    await client.StopAsync();
}
