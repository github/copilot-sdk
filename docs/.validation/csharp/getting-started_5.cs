// Source: getting-started.md:1251
using GitHub.Copilot.SDK;

using var client = new CopilotClient(new CopilotClientOptions
{
    CliUrl = "localhost:4321",
    UseStdio = false
});

// Use the client normally
await using var session = await client.CreateSessionAsync();
// ...