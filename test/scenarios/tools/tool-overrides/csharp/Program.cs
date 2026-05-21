using System.ComponentModel;
using GitHub.Copilot;
using Microsoft.Extensions.AI;

using var client = new CopilotClient(new CopilotClientOptions
{
    Connection = RuntimeConnection.ForStdio(path: Environment.GetEnvironmentVariable("COPILOT_CLI_PATH")),
    GitHubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
});

await client.StartAsync();

try
{
    await using var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "claude-haiku-4.5",
        OnPermissionRequest = PermissionHandler.ApproveAll,
        Tools = [CopilotTool.DefineTool((Delegate)CustomGrep, new CopilotToolOptions
        {
            OverridesBuiltInTool = true
        }, new AIFunctionFactoryOptions
        {
            Name = "grep",
            Description = "A custom grep implementation that overrides the built-in",
        })],
    });

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "Use grep to search for the word 'hello'",
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

[Description("A custom grep implementation that overrides the built-in")]
static string CustomGrep([Description("Search query")] string query)
    => $"CUSTOM_GREP_RESULT: {query}";
