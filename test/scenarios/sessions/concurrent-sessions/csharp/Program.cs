using GitHub.Copilot.SDK;

const string PiratePrompt = "You are a pirate. Always say Arrr!";
const string RobotPrompt = "You are a robot. Always say BEEP BOOP!";

using var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
    GitHubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
});

await client.StartAsync();

try
{
    var session1Task = client.CreateSessionAsync(new SessionConfig
    {
        Model = "claude-haiku-4.5",
        SystemMessage = new SystemMessageConfig { Mode = SystemMessageMode.Replace, Content = PiratePrompt },
        AvailableTools = [],
    });

    var session2Task = client.CreateSessionAsync(new SessionConfig
    {
        Model = "claude-haiku-4.5",
        SystemMessage = new SystemMessageConfig { Mode = SystemMessageMode.Replace, Content = RobotPrompt },
        AvailableTools = [],
    });

    await using var session1 = await session1Task;
    await using var session2 = await session2Task;

    var response1Task = session1.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "What is the capital of France?",
    });

    var response2Task = session2.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "What is the capital of France?",
    });

    var response1 = await response1Task;
    var response2 = await response2Task;

    if (response1 != null)
    {
        Console.WriteLine($"Session 1 (pirate): {response1.Data?.Content}");
    }
    if (response2 != null)
    {
        Console.WriteLine($"Session 2 (robot): {response2.Data?.Content}");
    }
}
finally
{
    await client.StopAsync();
}
