using GitHub.Copilot.SDK;

var hookLog = new List<string>();

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
        OnPermissionRequest = (request, invocation) =>
            Task.FromResult(new PermissionRequestResult { Kind = "approved" }),
        Hooks = new SessionHooks
        {
            OnSessionStart = (input, invocation) =>
            {
                hookLog.Add("onSessionStart");
                return Task.FromResult<SessionStartHookOutput?>(null);
            },
            OnSessionEnd = (input, invocation) =>
            {
                hookLog.Add("onSessionEnd");
                return Task.FromResult<SessionEndHookOutput?>(null);
            },
            OnPreToolUse = (input, invocation) =>
            {
                hookLog.Add($"onPreToolUse:{input.ToolName}");
                return Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput { PermissionDecision = "allow" });
            },
            OnPostToolUse = (input, invocation) =>
            {
                hookLog.Add($"onPostToolUse:{input.ToolName}");
                return Task.FromResult<PostToolUseHookOutput?>(null);
            },
            OnUserPromptSubmitted = (input, invocation) =>
            {
                hookLog.Add("onUserPromptSubmitted");
                return Task.FromResult<UserPromptSubmittedHookOutput?>(null);
            },
            OnErrorOccurred = (input, invocation) =>
            {
                hookLog.Add($"onErrorOccurred:{input.Error}");
                return Task.FromResult<ErrorOccurredHookOutput?>(null);
            },
        },
    });

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "List the files in the current directory using the glob tool with pattern '*.md'.",
    });

    if (response != null)
    {
        Console.WriteLine(response.Data?.Content);
    }

    Console.WriteLine("\n--- Hook execution log ---");
    foreach (var entry in hookLog)
    {
        Console.WriteLine($"  {entry}");
    }
    Console.WriteLine($"\nTotal hooks fired: {hookLog.Count}");
}
finally
{
    await client.StopAsync();
}
