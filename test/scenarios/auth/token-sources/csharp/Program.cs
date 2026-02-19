using System.Diagnostics;
using GitHub.Copilot.SDK;

// Token resolution priority:
// 1. COPILOT_GITHUB_TOKEN
// 2. GH_TOKEN
// 3. GITHUB_TOKEN
// 4. gh CLI fallback
static string? ResolveToken()
{
    var copilotToken = Environment.GetEnvironmentVariable("COPILOT_GITHUB_TOKEN");
    if (!string.IsNullOrEmpty(copilotToken)) return copilotToken;

    var ghToken = Environment.GetEnvironmentVariable("GH_TOKEN");
    if (!string.IsNullOrEmpty(ghToken)) return ghToken;

    var githubToken = Environment.GetEnvironmentVariable("GITHUB_TOKEN");
    if (!string.IsNullOrEmpty(githubToken)) return githubToken;

    // Fallback: gh CLI
    try
    {
        var process = Process.Start(new ProcessStartInfo("gh", "auth token")
        {
            RedirectStandardOutput = true,
            UseShellExecute = false,
        });
        var token = process?.StandardOutput.ReadToEnd().Trim();
        process?.WaitForExit();
        if (!string.IsNullOrEmpty(token)) return token;
    }
    catch
    {
        // gh CLI not available
    }

    return null;
}

var token = ResolveToken();

var opts = new CopilotClientOptions
{
    CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH"),
};

if (token != null)
{
    opts.GithubToken = token;
}

using var client = new CopilotClient(opts);
await client.StartAsync();

try
{
    var session = await client.CreateSessionAsync(new SessionConfig
    {
        Model = "gpt-4.1",
    });

    var response = await session.SendAndWaitAsync(new MessageOptions
    {
        Prompt = "What is the capital of France?",
    });

    if (response != null)
    {
        Console.WriteLine(response.Data?.Content);
    }

    await session.DisposeAsync();
}
finally
{
    await client.StopAsync();
}
