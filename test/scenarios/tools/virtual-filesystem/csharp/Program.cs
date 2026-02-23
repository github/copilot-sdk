using System.ComponentModel;
using GitHub.Copilot.SDK;
using Microsoft.Extensions.AI;

// In-memory virtual filesystem
var virtualFs = new Dictionary<string, string>();

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
        AvailableTools = [],
        Tools =
        [
            AIFunctionFactory.Create(
                ([Description("File path")] string path, [Description("File content")] string content) =>
                {
                    virtualFs[path] = content;
                    return $"Created {path} ({content.Length} bytes)";
                },
                "create_file",
                "Create or overwrite a file at the given path with the provided content"),
            AIFunctionFactory.Create(
                ([Description("File path")] string path) =>
                {
                    return virtualFs.TryGetValue(path, out var content)
                        ? content
                        : $"Error: file not found: {path}";
                },
                "read_file",
                "Read the contents of a file at the given path"),
            AIFunctionFactory.Create(
                () =>
                {
                    return virtualFs.Count == 0
                        ? "No files"
                        : string.Join("\n", virtualFs.Keys);
                },
                "list_files",
                "List all files in the virtual filesystem"),
        ],
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
        Prompt = "Create a file called plan.md with a brief 3-item project plan for building a CLI tool. Then read it back and tell me what you wrote.",
    });

    if (response != null)
    {
        Console.WriteLine(response.Data?.Content);
    }

    // Dump the virtual filesystem to prove nothing touched disk
    Console.WriteLine("\n--- Virtual filesystem contents ---");
    foreach (var (path, content) in virtualFs)
    {
        Console.WriteLine($"\n[{path}]");
        Console.WriteLine(content);
    }
}
finally
{
    await client.StopAsync();
}
