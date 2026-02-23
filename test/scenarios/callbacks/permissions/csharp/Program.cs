using GitHub.Copilot.SDK;

var permissionLog = new List<string>();

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
        {
            var toolName = request.ExtensionData?.TryGetValue("toolName", out var value) == true
                ? value?.ToString() ?? "unknown"
                : "unknown";
            permissionLog.Add($"approved:{toolName}");
            return Task.FromResult(new PermissionRequestResult { Kind = "approved" });
        },
        Hooks = new SessionHooks
        {
            OnPreToolUse = (input, invocation) =>
                Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput { PermissionDecision = "allow" }),
        },
    });

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "List the files in the current directory using glob with pattern '*.md'.",
    });

    if (response != null)
    {
        Console.WriteLine(response.Data?.Content);
    }

    Console.WriteLine("\n--- Permission request log ---");
    foreach (var entry in permissionLog)
    {
        Console.WriteLine($"  {entry}");
    }
    Console.WriteLine($"\nTotal permission requests: {permissionLog.Count}");
}
finally
{
    await client.StopAsync();
}
