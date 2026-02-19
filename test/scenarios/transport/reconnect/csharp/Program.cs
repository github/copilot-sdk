using GitHub.Copilot.SDK;

var cliUrl = Environment.GetEnvironmentVariable("COPILOT_CLI_URL") ?? "localhost:3000";

using var client = new CopilotClient(new CopilotClientOptions { CliUrl = cliUrl });
await client.StartAsync();

try
{
    // First session
    Console.WriteLine("--- Session 1 ---");
    var session1 = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "gpt-4.1",
    });

    var response1 = await session1.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "What is the capital of France?",
    });

    if (response1?.Data?.Content != null)
    {
        Console.WriteLine(response1.Data.Content);
    }
    else
    {
        Console.Error.WriteLine("No response content received for session 1");
        Environment.Exit(1);
    }

    await session1.DisposeAsync();
    Console.WriteLine("Session 1 destroyed\n");

    // Second session — tests that the server accepts new sessions
    Console.WriteLine("--- Session 2 ---");
    var session2 = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "gpt-4.1",
    });

    var response2 = await session2.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "What is the capital of France?",
    });

    if (response2?.Data?.Content != null)
    {
        Console.WriteLine(response2.Data.Content);
    }
    else
    {
        Console.Error.WriteLine("No response content received for session 2");
        Environment.Exit(1);
    }

    await session2.DisposeAsync();
    Console.WriteLine("Session 2 destroyed");

    Console.WriteLine("\nReconnect test passed — both sessions completed successfully");
}
finally
{
    await client.StopAsync();
}
