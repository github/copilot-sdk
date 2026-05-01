/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
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

    private static string ResolveSymlinks(string path)
    {
        if (OperatingSystem.IsWindows())
        {
            return Path.GetFullPath(path);
        }

        IntPtr resolved = IntPtr.Zero;
        try
        {
            resolved = NativeRealpath(path, IntPtr.Zero);
            if (resolved == IntPtr.Zero)
            {
                return Path.GetFullPath(path);
            }
            return Marshal.PtrToStringAnsi(resolved) ?? Path.GetFullPath(path);
        }
        catch
        {
            return Path.GetFullPath(path);
        }
        finally
        {
            if (resolved != IntPtr.Zero)
            {
                NativeFree(resolved);
            }
        }
    }

    [DllImport("libc", EntryPoint = "realpath", CharSet = CharSet.Ansi, BestFitMapping = false, ThrowOnUnmappableChar = true)]
    private static extern IntPtr NativeRealpath([MarshalAs(UnmanagedType.LPStr)] string path, IntPtr resolved);

    [DllImport("libc", EntryPoint = "free")]
    private static extern void NativeFree(IntPtr ptr);

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

    public CopilotClient CreateClient(bool useStdio = true, CopilotClientOptions? options = null, bool autoInjectGitHubToken = true)
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
