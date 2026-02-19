using GitHub.Copilot.SDK;

const string PiratePrompt = "You are a pirate. Always say Arrr!";
const string RobotPrompt = "You are a robot. Always say BEEP BOOP!";

var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
    GithubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
});

await client.StartAsync();

try
{
    var session1Task = client.CreateSessionAsync(new SessionConfig
    {
        Model = "gpt-4.1",
        SystemMessage = new SystemMessageConfig { Mode = SystemMessageMode.Replace, Content = PiratePrompt },
        AvailableTools = [],
    });

    var session2Task = client.CreateSessionAsync(new SessionConfig
    {
        Model = "gpt-4.1",
        SystemMessage = new SystemMessageConfig { Mode = SystemMessageMode.Replace, Content = RobotPrompt },
        AvailableTools = [],
    });

    var session1 = await session1Task;
    var session2 = await session2Task;

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

    await session1.DisposeAsync();
    await session2.DisposeAsync();
}
finally
{
    await client.StopAsync();
}
