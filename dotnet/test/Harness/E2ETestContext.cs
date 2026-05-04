/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Runtime.CompilerServices;
using System.Text.RegularExpressions;
using Microsoft.Extensions.Logging;

namespace GitHub.Copilot.SDK.Test.Harness;

public sealed class E2ETestContext : IAsyncDisposable
{
    public string HomeDir { get; }
    public string WorkDir { get; }
    public string ProxyUrl { get; }

    /// <summary>Optional logger injected by tests; applied to all clients created via <see cref="CreateClient"/>.</summary>
    public ILogger? Logger { get; set; }

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

        // Resolve symlinks (e.g., macOS /var -> /private/var) so paths
        // match what spawned subprocesses see when they resolve their cwd.
        homeDir = ResolveSymlinks(homeDir);
        workDir = ResolveSymlinks(workDir);

        var proxy = new CapiProxy();
        var proxyUrl = await proxy.StartAsync();

        return new E2ETestContext(homeDir, workDir, proxyUrl, proxy, repoRoot);
    }

    /// <summary>
    /// Returns a canonical path with symlinks resolved in every directory
    /// component. .NET has no built-in equivalent of POSIX <c>realpath</c>
    /// that walks all parents, so we walk the components ourselves and use
    /// <see cref="DirectoryInfo.ResolveLinkTarget(bool)"/> on each one.
    /// On Windows, where the test temp paths don't traverse symlinks,
    /// <see cref="Path.GetFullPath(string)"/> is sufficient.
    /// </summary>
    private static string ResolveSymlinks(string path)
    {
        if (OperatingSystem.IsWindows())
        {
            return Path.GetFullPath(path);
        }

        try
        {
            var fullPath = Path.GetFullPath(path);
            var root = Path.GetPathRoot(fullPath);
            if (string.IsNullOrEmpty(root))
            {
                return fullPath;
            }

            var components = fullPath
                .Substring(root.Length)
                .Split(Path.DirectorySeparatorChar, StringSplitOptions.RemoveEmptyEntries);

            var resolved = root;
            foreach (var component in components)
            {
                resolved = Path.Join(resolved, component);
                try
                {
                    var info = new DirectoryInfo(resolved);
                    if (info.Exists && info.LinkTarget != null)
                    {
                        var target = info.ResolveLinkTarget(returnFinalTarget: true);
                        if (target != null && !string.IsNullOrEmpty(target.FullName))
                        {
                            resolved = target.FullName;
                        }
                    }
                }
                catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
                {
                    // Component we can't inspect; keep what we have and continue.
                }
            }

            return resolved;
        }
        catch (Exception ex) when (ex is IOException
            or UnauthorizedAccessException
            or ArgumentException
            or NotSupportedException
            or PathTooLongException)
        {
            return Path.GetFullPath(path);
        }
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

    public Task<List<ParsedHttpExchange>> GetExchangesAsync()
    {
        return _proxy.GetExchangesAsync();
    }

    public Task SetCopilotUserByTokenAsync(string token, CopilotUserConfig response)
    {
        return _proxy.SetCopilotUserByTokenAsync(token, response);
    }

    public IReadOnlyDictionary<string, string> GetEnvironment()
    {
        var env = Environment.GetEnvironmentVariables()
            .Cast<System.Collections.DictionaryEntry>()
            .ToDictionary(e => (string)e.Key, e => e.Value?.ToString());

        env["COPILOT_API_URL"] = ProxyUrl;
        env["COPILOT_HOME"] = HomeDir;
        env["XDG_CONFIG_HOME"] = HomeDir;
        env["XDG_STATE_HOME"] = HomeDir;

        return env!;
    }

    public CopilotClient CreateClient(bool? useStdio = null, CopilotClientOptions? options = null, bool autoInjectGitHubToken = true)
    {
        options ??= new CopilotClientOptions();

        options.Cwd ??= WorkDir;
        options.Environment ??= GetEnvironment();
        options.UseStdio = useStdio;
        options.Logger ??= Logger;

        if (string.IsNullOrEmpty(options.CliUrl))
        {
            options.CliPath ??= GetCliPath(_repoRoot);
        }

        if (autoInjectGitHubToken
            && !string.IsNullOrEmpty(Environment.GetEnvironmentVariable("GITHUB_ACTIONS"))
            && string.IsNullOrEmpty(options.GitHubToken)
            && string.IsNullOrEmpty(options.CliUrl))
        {
            options.GitHubToken = "fake-token-for-e2e-tests";
        }

        return new(options);
    }

    public async ValueTask DisposeAsync()
    {
        // Skip writing snapshots in CI to avoid corrupting them on test failures
        var isCI = !string.IsNullOrEmpty(Environment.GetEnvironmentVariable("GITHUB_ACTIONS"));
        await _proxy.StopAsync(skipWritingCache: isCI);

        try { if (Directory.Exists(HomeDir)) Directory.Delete(HomeDir, true); } catch { }
        try { if (Directory.Exists(WorkDir)) Directory.Delete(WorkDir, true); } catch { }
    }
}
