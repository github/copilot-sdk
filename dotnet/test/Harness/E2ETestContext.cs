/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.Logging;
using System.Diagnostics;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using System.Text.RegularExpressions;

namespace GitHub.Copilot.Test.Harness;

public sealed class E2ETestContext : IAsyncDisposable
{
    private const string DefaultGitHubToken = "fake-token-for-e2e-tests";

    public string HomeDir { get; }
    public string WorkDir { get; }
    public string ProxyUrl { get; }

    /// <summary>Optional logger injected by tests; applied to all clients created via <see cref="CreateClient"/>.</summary>
    public ILogger? Logger { get; set; }

    private readonly CapiProxy _proxy;
    private readonly string _repoRoot;
    private readonly object _clientsLock = new();
    private readonly List<CopilotClient> _persistentClients = [];
    private readonly List<CopilotClient> _transientClients = [];

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
        await proxy.SetCopilotUserByTokenAsync(DefaultGitHubToken, new CopilotUserConfig(
            Login: "e2e-test-user",
            CopilotPlan: "individual_pro",
            Endpoints: new CopilotUserEndpoints(Api: proxyUrl, Telemetry: "https://localhost:1/telemetry"),
            AnalyticsTrackingId: "e2e-test-tracking-id"));

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

        // As of CLI 1.0.64-1 the @github/copilot package is a thin loader; the
        // runnable index.js ships in the installed platform package
        // (e.g. @github/copilot-linux-x64). Exactly one is installed.
        var githubModules = Path.Join(repoRoot, "nodejs", "node_modules", "@github");
        if (Directory.Exists(githubModules))
        {
            var candidate = Directory.EnumerateDirectories(githubModules, "copilot-*")
                .Select(dir => Path.Join(dir, "index.js"))
                .FirstOrDefault(File.Exists);
            if (candidate != null)
                return candidate;
        }

        throw new InvalidOperationException(
            $"CLI not found under {githubModules}. Run 'npm install' in the nodejs directory first.");
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

    public Dictionary<string, string> GetEnvironment()
    {
        var env = Environment.GetEnvironmentVariables()
            .Cast<System.Collections.DictionaryEntry>()
            .ToDictionary(e => (string)e.Key, e => e.Value?.ToString());

        env["COPILOT_API_URL"] = ProxyUrl;
        // Route GitHub API calls (e.g. the MCP registry policy check) to the
        // replay proxy so MCP enablement stays hermetic. Without this the CLI
        // reaches the real api.github.com, which is slow/unreachable on macOS
        // CI runners and makes MCP servers time out before reaching connected.
        env["COPILOT_DEBUG_GITHUB_API_URL"] = ProxyUrl;
        env["COPILOT_HOME"] = HomeDir;
        env["GH_CONFIG_DIR"] = HomeDir;
        env["XDG_CONFIG_HOME"] = HomeDir;
        env["XDG_STATE_HOME"] = HomeDir;
        env["COPILOT_MCP_APPS"] = "true";
        env["MCP_APPS"] = "true";
        if (!string.IsNullOrEmpty(_proxy.ConnectProxyUrl) && !string.IsNullOrEmpty(_proxy.CaFilePath))
        {
            const string noProxy = "127.0.0.1,localhost,::1";
            env["HTTP_PROXY"] = _proxy.ConnectProxyUrl;
            env["HTTPS_PROXY"] = _proxy.ConnectProxyUrl;
            env["http_proxy"] = _proxy.ConnectProxyUrl;
            env["https_proxy"] = _proxy.ConnectProxyUrl;
            env["NO_PROXY"] = noProxy;
            env["no_proxy"] = noProxy;
            env["NODE_EXTRA_CA_CERTS"] = _proxy.CaFilePath;
            env["SSL_CERT_FILE"] = _proxy.CaFilePath;
            env["REQUESTS_CA_BUNDLE"] = _proxy.CaFilePath;
            env["CURL_CA_BUNDLE"] = _proxy.CaFilePath;
            env["GIT_SSL_CAINFO"] = _proxy.CaFilePath;
            env["GH_TOKEN"] = "";
            env["GITHUB_TOKEN"] = "";
            env["GH_ENTERPRISE_TOKEN"] = "";
            env["GITHUB_ENTERPRISE_TOKEN"] = "";
        }

        env["GITHUB_TOKEN"] = env["GH_TOKEN"] = DefaultGitHubToken;

        return env!;
    }

    private static string? GetEffectiveGitHubTokenForTests()
    {
        return Environment.GetEnvironmentVariable("GITHUB_ACTIONS") == "true"
            ? DefaultGitHubToken
            : Environment.GetEnvironmentVariable("GITHUB_TOKEN");
    }

    // Auth-relevant environment variables that host-side native code in the
    // loaded cdylib reads from this process's environment (directly via
    // std::env::var, or indirectly via the `gh auth token` subprocess it spawns)
    // rather than from the environment passed to copilot_runtime_host_start.
    // Deliberately limited to vars that GetEnvironment() re-sets on every call, so
    // mirroring them onto the shared process env cannot leak stale values into a
    // later test that copies the process env.
    private static readonly string[] HostSideAuthEnvVars =
    {
        "COPILOT_DEBUG_GITHUB_API_URL",
        "GH_TOKEN",
        "GITHUB_TOKEN",
        "GH_CONFIG_DIR",
    };

    [DllImport("libc", EntryPoint = "setenv", CharSet = CharSet.Ansi,
        BestFitMapping = false, ThrowOnUnmappableChar = true)]
    private static extern int NativeSetEnv(string name, string value, int overwrite);

    // Sets an environment variable on both the managed cache and (on Unix) the
    // libc environment block, so native getenv/std::env::var readers in the loaded
    // cdylib observe it. On Windows the managed setter already reaches native
    // GetEnvironmentVariableW, so setenv is not needed.
    private static void SetProcessEnvironmentVariable(string name, string value)
    {
        Environment.SetEnvironmentVariable(name, value);
        if (!RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
            _ = NativeSetEnv(name, value, 1);
        }
    }

    // Mirrors CopilotClient's default-connection resolution: the no-Connection
    // case honors COPILOT_SDK_DEFAULT_CONNECTION (from options.Environment, else
    // the process env), defaulting to stdio.
    private static bool IsDefaultConnectionInProcess(IReadOnlyDictionary<string, string>? environment)
    {
        var value = environment is not null
            && environment.TryGetValue("COPILOT_SDK_DEFAULT_CONNECTION", out var fromOptions)
                ? fromOptions
                : Environment.GetEnvironmentVariable("COPILOT_SDK_DEFAULT_CONNECTION");
        return string.Equals(value, "inprocess", StringComparison.OrdinalIgnoreCase);
    }

    public CopilotClient CreateClient(
        bool? useStdio = null,
        CopilotClientOptions? options = null,
        bool autoInjectGitHubToken = true,
        bool persistent = false)
    {
        options ??= new CopilotClientOptions();

        options.WorkingDirectory ??= WorkDir;
        options.Environment ??= GetEnvironment();
        options.Logger ??= Logger;

        // Build the connection. If the caller supplied one, just ensure the runtime path is set.
        // When neither a Connection nor useStdio is specified, leave Connection null so
        // CopilotClient honors COPILOT_SDK_DEFAULT_CONNECTION (defaulting to stdio); useStdio
        // is a convenience shortcut to pin stdio/tcp. Passing both a Connection and useStdio is ambiguous.
        if (useStdio is not null && options.Connection is not null)
        {
            throw new ArgumentException(
                "Specify either useStdio or options.Connection, not both. " +
                "Use options.Connection (e.g. RuntimeConnection.ForStdio() / RuntimeConnection.ForTcp()) to control transport when supplying a Connection.",
                nameof(useStdio));
        }

        var cliPath = GetCliPath(_repoRoot);
        switch (options.Connection)
        {
            case null when useStdio == true:
                options.Connection = RuntimeConnection.ForStdio(path: cliPath);
                break;
            case null when useStdio == false:
                options.Connection = RuntimeConnection.ForTcp(path: cliPath);
                break;
            case null:
                // useStdio is null: leave Connection unset so CopilotClient's
                // ResolveDefaultConnection honors COPILOT_SDK_DEFAULT_CONNECTION
                // (stdio by default, or in-process). The CLI path flows through
                // options.Environment["COPILOT_CLI_PATH"] (GetEnvironment copies
                // the process env, where CI's setup-copilot sets it).
                break;
            case ChildProcessRuntimeConnection child when child.Path is null:
                child.Path = cliPath;
                break;
        }

        // In-process hosting workaround (applies only when this session actually
        // uses the in-process FFI transport): several auth code paths run
        // host-side in this process (the loaded cdylib) and read the ambient
        // process environment rather than the environment passed to
        // copilot_runtime_host_start — e.g. native fetch_copilot_user reads
        // COPILOT_DEBUG_GITHUB_API_URL via std::env::var, and the gh-CLI fallback
        // spawns `gh auth token`, which inherits this process's GH_TOKEN /
        // GITHUB_TOKEN / GH_CONFIG_DIR. So our per-test redirects and
        // cleared tokens in options.Environment are invisible to them and auth
        // escapes the replay proxy -> 401. Mirror just the auth-relevant vars onto
        // this process's real environment block so those host-side reads observe
        // them. Gated to in-process only (and to a narrow var set) so stdio/tcp
        // tests never mutate the shared process environment. Note .NET's
        // Environment.SetEnvironmentVariable does NOT reach libc getenv on Unix, so
        // we also call setenv directly. Safe because E2E tests run serially
        // (DisableTestParallelization) and in-process is single-runtime-per-process.
        // Remove once the runtime threads the host_start environment into these
        // host-side reads instead of the global process env.
        var isInProcess = options.Connection is InProcessRuntimeConnection
            || (options.Connection is null && IsDefaultConnectionInProcess(options.Environment));
        if (isInProcess)
        {
            foreach (var name in HostSideAuthEnvVars)
            {
                if (options.Environment.TryGetValue(name, out var value))
                {
                    SetProcessEnvironmentVariable(name, value);
                }
            }
        }

        // Auto-inject auth token unless connecting to an existing runtime via URI.
        var isExistingRuntime = options.Connection is UriRuntimeConnection;
        if (autoInjectGitHubToken
            && string.IsNullOrEmpty(options.GitHubToken)
            && !isExistingRuntime)
        {
            options.GitHubToken = GetEffectiveGitHubTokenForTests();
        }

        var client = new CopilotClient(options);
        lock (_clientsLock)
        {
            if (persistent)
            {
                _persistentClients.Add(client);
            }
            else
            {
                _transientClients.Add(client);
            }
        }
        return client;
    }

    public void UntrackClient(CopilotClient client)
    {
        lock (_clientsLock)
        {
            _persistentClients.Remove(client);
            _transientClients.Remove(client);
        }
    }

    public async Task CleanupAfterTestAsync()
    {
        // Per-test cleanup only stops clients created for a specific test.
        // The shared persistent client and temp directories are cleaned when the fixture is disposed.
        var errors = new List<Exception>();
        CopilotClient[] transientClients;

        lock (_clientsLock)
        {
            transientClients = [.. _transientClients];
            _transientClients.Clear();
        }

        foreach (var client in transientClients)
        {
            try
            {
                await client.ForceStopAsync();
            }
            catch (Exception ex) when (IsTransientCleanupException(ex))
            {
                errors.Add(ex);
            }
        }

        if (errors.Count == 1)
        {
            throw errors[0];
        }
        if (errors.Count > 1)
        {
            throw new AggregateException(errors);
        }
    }

    public async ValueTask DisposeAsync()
    {
        var errors = new List<Exception>();
        CopilotClient[] clients;

        lock (_clientsLock)
        {
            clients = [.. _persistentClients.Concat(_transientClients)];
            _persistentClients.Clear();
            _transientClients.Clear();
        }

        foreach (var client in clients)
        {
            try
            {
                await client.ForceStopAsync();
            }
            catch (Exception ex) when (IsTransientCleanupException(ex))
            {
                errors.Add(ex);
            }
        }

        // Skip writing snapshots in CI to avoid corrupting them on test failures
        var isCI = !string.IsNullOrEmpty(Environment.GetEnvironmentVariable("GITHUB_ACTIONS"));
        try { await _proxy.StopAsync(skipWritingCache: isCI); } catch (Exception ex) when (IsTransientCleanupException(ex)) { errors.Add(ex); }

        try { await DeleteDirectoryAsync(HomeDir); } catch (Exception ex) when (IsTransientCleanupException(ex)) { errors.Add(ex); }
        try { await DeleteDirectoryAsync(WorkDir); } catch (Exception ex) when (IsTransientCleanupException(ex)) { errors.Add(ex); }

        if (errors.Count == 1)
        {
            throw errors[0];
        }
        if (errors.Count > 1)
        {
            throw new AggregateException(errors);
        }
    }

    private static async Task DeleteDirectoryAsync(string path)
    {
        const int maxAttempts = 40;
        var delay = TimeSpan.FromMilliseconds(50);
        var lastException = (Exception?)null;

        for (var attempt = 1; attempt <= maxAttempts; attempt++)
        {
            if (!Directory.Exists(path))
            {
                return;
            }

            try
            {
                Directory.Delete(path, recursive: true);
                return;
            }
            catch (Exception ex) when (IsTransientCleanupException(ex))
            {
                lastException = ex;
                if (attempt == maxAttempts)
                {
                    break;
                }

                await Task.Delay(delay);
                delay = TimeSpan.FromMilliseconds(Math.Min(delay.TotalMilliseconds * 2, 250));
            }
        }

        if (Directory.Exists(path))
        {
            throw new IOException($"Failed to delete directory '{path}' after {maxAttempts} attempts.", lastException);
        }
    }

    private static bool IsTransientCleanupException(Exception exception)
        => exception is IOException or UnauthorizedAccessException;
}
