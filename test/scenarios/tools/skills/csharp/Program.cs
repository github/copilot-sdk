using GitHub.Copilot.SDK;

var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
    GithubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
});

await client.StartAsync();

try
{
    var skillsDir = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "..", "sample-skills"));

    var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "gpt-4.1",
        SkillDirectories = [skillsDir],
        OnPermissionRequest = (request, invocation) =>
            Task.FromResult(new PermissionRequestResult { Kind = "approved" }),
        Hooks = new SessionHooks
        {
            OnPreToolUse = (input, invocation) =>
                Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput { PermissionDecision = "allow" }),
        },
    });

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "Use the greeting skill to greet someone named Alice.",
    });

    if (response != null)
    {
        Console.WriteLine(response.Data?.Content);
    }

    Console.WriteLine("\nSkill directories configured successfully");

    await session.DisposeAsync();
}
finally
{
    await client.StopAsync();
}
