/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Runtime.CompilerServices;
using System.Text.RegularExpressions;

namespace GitHub.Copilot.SDK.Test.Harness;

public class E2ETestContext : IAsyncDisposable
{
    public string HomeDir { get; }
    public string WorkDir { get; }
    public string ProxyUrl { get; }

    private readonly CapiProxy _proxy;
    private readonly string _repoRoot;

    private E2ETestContext(string homeDir, string workDir, string proxyUrl, CapiProxy proxy, string repoRoot)
    {
        HomeDir = homeDir;
        WorkDir = workDir;
        ProxyUrl = proxyUrl;
        _proxy = proxy;
        _repoRoot = repoRoot;
    }

    public static async Task<E2ETestContext> CreateAsync()
    {
        var repoRoot = FindRepoRoot();

        var homeDir = Path.Combine(Path.GetTempPath(), $"copilot-test-config-{Guid.NewGuid()}");
        var workDir = Path.Combine(Path.GetTempPath(), $"copilot-test-work-{Guid.NewGuid()}");

        Directory.CreateDirectory(homeDir);
        Directory.CreateDirectory(workDir);

        var proxy = new CapiProxy();
        var proxyUrl = await proxy.StartAsync();

        return new E2ETestContext(homeDir, workDir, proxyUrl, proxy, repoRoot);
    }

    private static string FindRepoRoot()
    {
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        while (dir != null)
        {
            if (Directory.Exists(Path.Combine(dir.FullName, "nodejs")))
                return dir.FullName;
            dir = dir.Parent;
        }
        throw new InvalidOperationException("Could not find repository root");
    }

    private static string GetCliPath(string repoRoot)
    {
        var envPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH");
        if (!string.IsNullOrEmpty(envPath)) return envPath;

        var path = Path.Combine(repoRoot, "nodejs/node_modules/@github/copilot/index.js");
        if (!File.Exists(path))
            throw new InvalidOperationException($"CLI not found at {path}. Run 'npm install' in the nodejs directory first.");

        return path;
    }

    public async Task ConfigureForTestAsync(string testFile, [CallerMemberName] string? testName = null)
    {
        // Convert test method names to lowercase snake_case for snapshot filenames
        // to avoid case collisions on case-insensitive filesystems (macOS/Windows)
        var sanitizedName = Regex.Replace(testName!, @"[^a-zA-Z0-9]", "_").ToLowerInvariant();
        var snapshotPath = Path.Combine(_repoRoot, "test", "snapshots", testFile, $"{sanitizedName}.yaml");
        await _proxy.ConfigureAsync(snapshotPath, WorkDir);
    }

    public Task<List<ParsedHttpExchange>> GetExchangesAsync() => _proxy.GetExchangesAsync();

    public IReadOnlyDictionary<string, string> GetEnvironment()
    {
        var env = Environment.GetEnvironmentVariables()
            .Cast<System.Collections.DictionaryEntry>()
            .ToDictionary(e => (string)e.Key, e => e.Value?.ToString());

        env["COPILOT_API_URL"] = ProxyUrl;
        env["XDG_CONFIG_HOME"] = HomeDir;
        env["XDG_STATE_HOME"] = HomeDir;

        return env!;
    }

    public CopilotClient CreateClient() => new(new CopilotClientOptions
    {
        Cwd = WorkDir,
        Environment = GetEnvironment(),
        GithubToken = !string.IsNullOrEmpty(Environment.GetEnvironmentVariable("CI")) ? "fake-token-for-e2e-tests" : null,
    });

    public async ValueTask DisposeAsync()
    {
        // Skip writing snapshots in CI to avoid corrupting them on test failures
        var isCI = !string.IsNullOrEmpty(Environment.GetEnvironmentVariable("CI"));
        await _proxy.StopAsync(skipWritingCache: isCI);

        try { if (Directory.Exists(HomeDir)) Directory.Delete(HomeDir, true); } catch { }
        try { if (Directory.Exists(WorkDir)) Directory.Delete(WorkDir, true); } catch { }
    }
}
