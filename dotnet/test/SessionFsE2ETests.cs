/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

/// <summary>
/// Custom fixture that creates a CopilotClient with SessionFs enabled.
/// </summary>
public class SessionFsE2EFixture : IAsyncLifetime
{
    public E2ETestContext Ctx { get; private set; } = null!;
    public CopilotClient Client { get; private set; } = null!;

    public async Task InitializeAsync()
    {
        Ctx = await E2ETestContext.CreateAsync();
        Client = new CopilotClient(new CopilotClientOptions
        {
            Cwd = Ctx.WorkDir,
            CliPath = Environment.GetEnvironmentVariable("COPILOT_CLI_PATH")
                ?? Path.Combine(FindRepoRoot(), "nodejs/node_modules/@github/copilot/index.js"),
            Environment = Ctx.GetEnvironment(),
            UseStdio = true,
            GitHubToken = !string.IsNullOrEmpty(Environment.GetEnvironmentVariable("GITHUB_ACTIONS"))
                ? "fake-token-for-e2e-tests"
                : null,
            SessionFs = new SessionFsConfig
            {
                InitialCwd = "/",
                SessionStatePath = "/session-state",
                Conventions = SessionFsConventions.Posix,
            },
        });
    }

    public async Task DisposeAsync()
    {
        if (Client is not null) await Client.ForceStopAsync();
        await Ctx.DisposeAsync();
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
}

/// <summary>
/// In memory filesystem implementation for session filesystem E2E tests.
/// </summary>
internal class InMemorySessionFsHandler : ISessionFsHandler
{
    private readonly string _sessionId;
    private readonly InMemoryFileSystem _fs;

    public InMemorySessionFsHandler(string sessionId, InMemoryFileSystem fs)
    {
        _sessionId = sessionId;
        _fs = fs;
    }

    private string Sp(string path) => $"/{_sessionId}{(path.StartsWith('/') ? path : "/" + path)}";

    public Task<SessionFsReadFileResult> ReadFileAsync(SessionFsReadFileParams request, CancellationToken ct = default)
        => Task.FromResult(new SessionFsReadFileResult { Content = _fs.ReadFile(Sp(request.Path)) });

    public Task WriteFileAsync(SessionFsWriteFileParams request, CancellationToken ct = default)
    {
        _fs.WriteFile(Sp(request.Path), request.Content);
        return Task.CompletedTask;
    }

    public Task AppendFileAsync(SessionFsAppendFileParams request, CancellationToken ct = default)
    {
        _fs.AppendFile(Sp(request.Path), request.Content);
        return Task.CompletedTask;
    }

    public Task<SessionFsExistsResult> ExistsAsync(SessionFsExistsParams request, CancellationToken ct = default)
        => Task.FromResult(new SessionFsExistsResult { Exists = _fs.Exists(Sp(request.Path)) });

    public Task<SessionFsStatResult> StatAsync(SessionFsStatParams request, CancellationToken ct = default)
    {
        var (isFile, size, mtime) = _fs.Stat(Sp(request.Path));
        return Task.FromResult(new SessionFsStatResult
        {
            IsFile = isFile,
            IsDirectory = !isFile,
            Size = size,
            Mtime = mtime.ToString("o"),
            Birthtime = mtime.ToString("o"),
        });
    }

    public Task MkdirAsync(SessionFsMkdirParams request, CancellationToken ct = default)
    {
        _fs.Mkdir(Sp(request.Path), request.Recursive ?? false);
        return Task.CompletedTask;
    }

    public Task<SessionFsReaddirResult> ReaddirAsync(SessionFsReaddirParams request, CancellationToken ct = default)
        => Task.FromResult(new SessionFsReaddirResult { Entries = _fs.Readdir(Sp(request.Path)) });

    public Task<SessionFsReaddirWithTypesResult> ReaddirWithTypesAsync(SessionFsReaddirWithTypesParams request, CancellationToken ct = default)
    {
        var entries = _fs.ReaddirWithTypes(Sp(request.Path));
        return Task.FromResult(new SessionFsReaddirWithTypesResult
        {
            Entries = entries.Select(e => new SessionFsDirEntry { Name = e.Name, Type = e.IsDirectory ? "directory" : "file" }).ToList()
        });
    }

    public Task RmAsync(SessionFsRmParams request, CancellationToken ct = default)
    {
        _fs.Remove(Sp(request.Path));
        return Task.CompletedTask;
    }

    public Task RenameAsync(SessionFsRenameParams request, CancellationToken ct = default)
    {
        _fs.Rename(Sp(request.Src), Sp(request.Dest));
        return Task.CompletedTask;
    }
}

/// <summary>
/// Simple in memory filesystem for testing. Stores files as path to content entries.
/// Directories are inferred from file paths (mkdir is tracked separately).
/// </summary>
internal class InMemoryFileSystem
{
    private readonly Dictionary<string, string> _files = new();
    private readonly HashSet<string> _directories = new() { "/" };
    private readonly Dictionary<string, DateTime> _mtimes = new();
    private readonly object _lock = new();

    public string ReadFile(string path)
    {
        lock (_lock)
        {
            if (!_files.TryGetValue(NormalizePath(path), out var content))
                throw new FileNotFoundException($"File not found: {path}");
            return content;
        }
    }

    public void WriteFile(string path, string content)
    {
        lock (_lock)
        {
            var p = NormalizePath(path);
            EnsureParentDirs(p);
            _files[p] = content;
            _mtimes[p] = DateTime.UtcNow;
        }
    }

    public void AppendFile(string path, string content)
    {
        lock (_lock)
        {
            var p = NormalizePath(path);
            EnsureParentDirs(p);
            _files[p] = _files.TryGetValue(p, out var existing) ? existing + content : content;
            _mtimes[p] = DateTime.UtcNow;
        }
    }

    public bool Exists(string path)
    {
        lock (_lock)
        {
            var p = NormalizePath(path);
            return _files.ContainsKey(p) || _directories.Contains(p);
        }
    }

    public (bool IsFile, long Size, DateTime Mtime) Stat(string path)
    {
        lock (_lock)
        {
            var p = NormalizePath(path);
            if (_files.TryGetValue(p, out var content))
                return (true, content.Length, _mtimes.GetValueOrDefault(p, DateTime.UtcNow));
            if (_directories.Contains(p))
                return (false, 0, DateTime.UtcNow);
            throw new FileNotFoundException($"Path not found: {path}");
        }
    }

    public void Mkdir(string path, bool recursive)
    {
        lock (_lock)
        {
            var p = NormalizePath(path);
            if (recursive)
                EnsureParentDirs(p + "/placeholder");
            _directories.Add(p);
        }
    }

    public List<string> Readdir(string path)
    {
        lock (_lock)
        {
            var p = NormalizePath(path);
            if (!p.EndsWith('/')) p += "/";
            var entries = new HashSet<string>();
            foreach (var key in _files.Keys)
            {
                if (key.StartsWith(p) && key.Length > p.Length)
                {
                    var rest = key[p.Length..];
                    var slash = rest.IndexOf('/');
                    entries.Add(slash >= 0 ? rest[..slash] : rest);
                }
            }
            foreach (var dir in _directories)
            {
                if (dir.StartsWith(p) && dir.Length > p.Length)
                {
                    var rest = dir[p.Length..];
                    var slash = rest.IndexOf('/');
                    entries.Add(slash >= 0 ? rest[..slash] : rest);
                }
            }
            return entries.Order().ToList();
        }
    }

    public List<(string Name, bool IsDirectory)> ReaddirWithTypes(string path)
    {
        lock (_lock)
        {
            var names = Readdir(path);
            var p = NormalizePath(path);
            if (!p.EndsWith('/')) p += "/";
            return names.Select(n =>
            {
                var full = p + n;
                var isDir = _directories.Contains(full) || _files.Keys.Any(k => k.StartsWith(full + "/"));
                return (n, isDir);
            }).ToList();
        }
    }

    public void Remove(string path)
    {
        lock (_lock)
        {
            var p = NormalizePath(path);
            _files.Remove(p);
            _directories.Remove(p);
            _mtimes.Remove(p);
        }
    }

    public void Rename(string src, string dest)
    {
        lock (_lock)
        {
            var s = NormalizePath(src);
            var d = NormalizePath(dest);
            if (_files.TryGetValue(s, out var content))
            {
                _files.Remove(s);
                EnsureParentDirs(d);
                _files[d] = content;
                _mtimes[d] = _mtimes.GetValueOrDefault(s, DateTime.UtcNow);
                _mtimes.Remove(s);
            }
        }
    }

    private static string NormalizePath(string path) => path.TrimEnd('/');

    private void EnsureParentDirs(string path)
    {
        var parts = path.Split('/');
        for (int i = 1; i < parts.Length - 1; i++)
        {
            var dir = string.Join("/", parts[..( i + 1)]);
            _directories.Add(dir);
        }
    }
}

public class SessionFsE2ETests(SessionFsE2EFixture fixture, ITestOutputHelper output) : IClassFixture<SessionFsE2EFixture>, IAsyncLifetime
{
    private readonly SessionFsE2EFixture _fixture = fixture;
    private readonly string _testName = GetTestName(output);

    private E2ETestContext Ctx => _fixture.Ctx;
    private CopilotClient Client => _fixture.Client;

    // Shared in memory filesystem across tests in this class
    private static readonly InMemoryFileSystem SharedFs = new();

    private static string GetTestName(ITestOutputHelper output)
    {
        var type = output.GetType();
        var testField = type.GetField("test", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic);
        var test = (ITest?)testField?.GetValue(output);
        return test?.TestCase.TestMethod.Method.Name ?? throw new InvalidOperationException("Couldn't find test name");
    }

    public async Task InitializeAsync()
    {
        await Ctx.ConfigureForTestAsync("session_fs", _testName);
    }

    public Task DisposeAsync() => Task.CompletedTask;

    [Fact]
    public async Task Should_Route_File_Operations_Through_The_Session_Fs_Provider()
    {
        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            CreateSessionFsHandler = s => new InMemorySessionFsHandler(s.SessionId, SharedFs),
        });

        var msg = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 100 + 200?" });
        Assert.NotNull(msg);
        Assert.Contains("300", msg!.Data.Content);
        await session.DisposeAsync();

        var content = SharedFs.ReadFile($"/{session.SessionId}/session-state/events.jsonl");
        Assert.Contains("300", content);
    }

    [Fact]
    public async Task Should_Load_Session_Data_From_Fs_Provider_On_Resume()
    {
        var session1 = await Client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            CreateSessionFsHandler = s => new InMemorySessionFsHandler(s.SessionId, SharedFs),
        });
        var sessionId = session1.SessionId;

        var msg = await session1.SendAndWaitAsync(new MessageOptions { Prompt = "What is 50 + 50?" });
        Assert.NotNull(msg);
        Assert.Contains("100", msg!.Data.Content);
        await session1.DisposeAsync();

        Assert.True(SharedFs.Exists($"/{sessionId}/session-state/events.jsonl"));

        var session2 = await Client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            CreateSessionFsHandler = s => new InMemorySessionFsHandler(s.SessionId, SharedFs),
        });

        var msg2 = await session2.SendAndWaitAsync(new MessageOptions { Prompt = "What is that times 3?" });
        await session2.DisposeAsync();
        Assert.NotNull(msg2);
        Assert.Contains("300", msg2!.Data.Content);
    }
}
