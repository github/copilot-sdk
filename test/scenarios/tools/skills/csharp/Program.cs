using GitHub.Copilot.SDK;

using var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
    GitHubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN"),
});

await client.StartAsync();

try
{
    var skillsDir = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "..", "sample-skills"));

    await using var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "claude-haiku-4.5",
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
}
finally
{
    await client.StopAsync();
}
