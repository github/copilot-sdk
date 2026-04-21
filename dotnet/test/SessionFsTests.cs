/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Extensions.AI;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class SessionFsTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "session_fs", output)
{
    private static readonly SessionFsConfig SessionFsConfig = new()
    {
        InitialCwd = "/",
        SessionStatePath = "/session-state",
        Conventions = SessionFsSetProviderConventions.Posix,
    };

    [Fact]
    public async Task Should_Route_File_Operations_Through_The_Session_Fs_Provider()
    {
        var providerRoot = CreateProviderRoot();
        try
        {
            await using var client = CreateSessionFsClient(providerRoot);

            var session = await client.CreateSessionAsync(new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
                CreateSessionFsHandler = s => new TestSessionFsHandler(s.SessionId, providerRoot),
            });

            var msg = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 100 + 200?" });
            Assert.Contains("300", msg?.Data.Content ?? string.Empty);
            await session.DisposeAsync();

            var eventsPath = GetStoredPath(providerRoot, session.SessionId, "/session-state/events.jsonl");
            await WaitForConditionAsync(() => File.Exists(eventsPath));
            var content = await ReadAllTextSharedAsync(eventsPath);
            Assert.Contains("300", content);
        }
        finally
        {
            await TryDeleteDirectoryAsync(providerRoot);
        }
    }

    [Fact]
    public async Task Should_Load_Session_Data_From_Fs_Provider_On_Resume()
    {
        var providerRoot = CreateProviderRoot();
        try
        {
            await using var client = CreateSessionFsClient(providerRoot);
            Func<CopilotSession, SessionFsProvider> createSessionFsHandler = s => new TestSessionFsHandler(s.SessionId, providerRoot);

            var session1 = await client.CreateSessionAsync(new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
                CreateSessionFsHandler = createSessionFsHandler,
            });
            var sessionId = session1.SessionId;

            var msg = await session1.SendAndWaitAsync(new MessageOptions { Prompt = "What is 50 + 50?" });
            Assert.Contains("100", msg?.Data.Content ?? string.Empty);
            await session1.DisposeAsync();

            var eventsPath = GetStoredPath(providerRoot, sessionId, "/session-state/events.jsonl");
            await WaitForConditionAsync(() => File.Exists(eventsPath));

            var session2 = await client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
                CreateSessionFsHandler = createSessionFsHandler,
            });

            var msg2 = await session2.SendAndWaitAsync(new MessageOptions { Prompt = "What is that times 3?" });
            Assert.Contains("300", msg2?.Data.Content ?? string.Empty);
            await session2.DisposeAsync();
        }
        finally
        {
            await TryDeleteDirectoryAsync(providerRoot);
        }
    }

    [Fact]
    public async Task Should_Reject_SetProvider_When_Sessions_Already_Exist()
    {
        var providerRoot = CreateProviderRoot();
        try
        {
            await using var client1 = CreateSessionFsClient(providerRoot, useStdio: false);
            var createSessionFsHandler = (Func<CopilotSession, SessionFsProvider>)(s => new TestSessionFsHandler(s.SessionId, providerRoot));

            _ = await client1.CreateSessionAsync(new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
                CreateSessionFsHandler = createSessionFsHandler,
            });

            var port = client1.ActualPort
                ?? throw new InvalidOperationException("Client1 is not using TCP mode; ActualPort is null");

            var client2 = Ctx.CreateClient(
                useStdio: false,
                options: new CopilotClientOptions
                {
                    CliUrl = $"localhost:{port}",
                    LogLevel = "error",
                    SessionFs = SessionFsConfig,
                });

            try
            {
                await Assert.ThrowsAnyAsync<Exception>(() => client2.StartAsync());
            }
            finally
            {
                try
                {
                    await client2.ForceStopAsync();
                }
                catch (IOException ex)
                {
                    Console.Error.WriteLine($"Ignoring expected teardown IOException from ForceStopAsync: {ex.Message}");
                }
            }
        }
        finally
        {
            await TryDeleteDirectoryAsync(providerRoot);
        }
    }

    [Fact]
    public async Task Should_Map_Large_Output_Handling_Into_SessionFs()
    {
        var providerRoot = CreateProviderRoot();
        try
        {
            const int largeContentSize = 100_000;
            var suppliedFileContent = new string('x', largeContentSize);

            await using var client = CreateSessionFsClient(providerRoot);
            var session = await client.CreateSessionAsync(new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
                CreateSessionFsHandler = s => new TestSessionFsHandler(s.SessionId, providerRoot),
                Tools =
                [
                    AIFunctionFactory.Create(() => suppliedFileContent, "get_big_string", "Returns a large string")
                ],
            });

            await session.SendAndWaitAsync(new MessageOptions
            {
                Prompt = "Call the get_big_string tool and reply with the word DONE only.",
            });

            var messages = await session.GetMessagesAsync();
            var toolResult = FindToolCallResult(messages, "get_big_string");
            Assert.NotNull(toolResult);
            Assert.Contains("/session-state/temp/", toolResult);

            var match = System.Text.RegularExpressions.Regex.Match(
                toolResult!,
                @"([/\\]session-state[/\\]temp[/\\][^\s]+)");
            Assert.True(match.Success);

            var fileContent = await ReadAllTextSharedAsync(GetStoredPath(providerRoot, session.SessionId, match.Groups[1].Value));
            Assert.Equal(suppliedFileContent, fileContent);
            await session.DisposeAsync();
        }
        finally
        {
            await TryDeleteDirectoryAsync(providerRoot);
        }
    }

    [Fact]
    public async Task Should_Succeed_With_Compaction_While_Using_SessionFs()
    {
        var providerRoot = CreateProviderRoot();
        try
        {
            await using var client = CreateSessionFsClient(providerRoot);
            var session = await client.CreateSessionAsync(new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
                CreateSessionFsHandler = s => new TestSessionFsHandler(s.SessionId, providerRoot),
            });

            SessionCompactionCompleteEvent? compactionEvent = null;
            using var _ = session.On(evt =>
            {
                if (evt is SessionCompactionCompleteEvent complete)
                {
                    compactionEvent = complete;
                }
            });

            await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2+2?" });

            var eventsPath = GetStoredPath(providerRoot, session.SessionId, "/session-state/events.jsonl");
            await WaitForConditionAsync(() => File.Exists(eventsPath), TimeSpan.FromSeconds(30));
            var contentBefore = await ReadAllTextSharedAsync(eventsPath);
            Assert.DoesNotContain("checkpointNumber", contentBefore);

            await session.Rpc.History.CompactAsync();
            await WaitForConditionAsync(() => compactionEvent is not null, TimeSpan.FromSeconds(30));
            Assert.True(compactionEvent!.Data.Success);

            await WaitForConditionAsync(async () =>
            {
                var content = await ReadAllTextSharedAsync(eventsPath);
                return content.Contains("checkpointNumber", StringComparison.Ordinal);
            }, TimeSpan.FromSeconds(30));
        }
        finally
        {
            await TryDeleteDirectoryAsync(providerRoot);
        }
    }

    [Fact]
    public async Task Should_Write_Workspace_Metadata_Via_SessionFs()
    {
        var providerRoot = CreateProviderRoot();
        try
        {
            await using var client = CreateSessionFsClient(providerRoot);
            var session = await client.CreateSessionAsync(new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
                CreateSessionFsHandler = s => new TestSessionFsHandler(s.SessionId, providerRoot),
            });

            var msg = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 7 * 8?" });
            Assert.Contains("56", msg?.Data.Content ?? string.Empty);

            // WorkspaceManager should have created workspace.yaml via sessionFs
            var workspaceYamlPath = GetStoredPath(providerRoot, session.SessionId, "/session-state/workspace.yaml");
            await WaitForConditionAsync(() => File.Exists(workspaceYamlPath));
            var yaml = await ReadAllTextSharedAsync(workspaceYamlPath);
            Assert.Contains("id:", yaml);

            // Checkpoint index should also exist
            var indexPath = GetStoredPath(providerRoot, session.SessionId, "/session-state/checkpoints/index.md");
            await WaitForConditionAsync(() => File.Exists(indexPath));

            await session.DisposeAsync();
        }
        finally
        {
            await TryDeleteDirectoryAsync(providerRoot);
        }
    }

    [Fact]
    public async Task Should_Persist_Plan_Md_Via_SessionFs()
    {
        var providerRoot = CreateProviderRoot();
        try
        {
            await using var client = CreateSessionFsClient(providerRoot);
            var session = await client.CreateSessionAsync(new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
                CreateSessionFsHandler = s => new TestSessionFsHandler(s.SessionId, providerRoot),
            });

            // Write a plan via the session RPC
            await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2 + 3?" });
            await session.Rpc.Plan.UpdateAsync("# Test Plan\n\nThis is a test.");

            var planPath = GetStoredPath(providerRoot, session.SessionId, "/session-state/plan.md");
            await WaitForConditionAsync(() => File.Exists(planPath));
            var content = await ReadAllTextSharedAsync(planPath);
            Assert.Contains("# Test Plan", content);

            await session.DisposeAsync();
        }
        finally
        {
            await TryDeleteDirectoryAsync(providerRoot);
        }
    }

    private CopilotClient CreateSessionFsClient(string providerRoot, bool useStdio = true)
    {
        Directory.CreateDirectory(providerRoot);
        return Ctx.CreateClient(
            useStdio: useStdio,
            options: new CopilotClientOptions
            {
                SessionFs = SessionFsConfig,
            });
    }

    private static string? FindToolCallResult(IReadOnlyList<SessionEvent> messages, string toolName)
    {
        var callId = messages
            .OfType<ToolExecutionStartEvent>()
            .FirstOrDefault(m => string.Equals(m.Data.ToolName, toolName, StringComparison.Ordinal))
            ?.Data.ToolCallId;

        if (callId is null)
        {
            return null;
        }

        return messages
            .OfType<ToolExecutionCompleteEvent>()
            .FirstOrDefault(m => string.Equals(m.Data.ToolCallId, callId, StringComparison.Ordinal))
            ?.Data.Result?.Content;
    }

    private static string CreateProviderRoot()
        => Path.Join(Path.GetTempPath(), $"copilot-sessionfs-{Guid.NewGuid():N}");

    private static string GetStoredPath(string providerRoot, string sessionId, string sessionPath)
    {
        var safeSessionId = NormalizeRelativePathSegment(sessionId, nameof(sessionId));
        var relativeSegments = sessionPath
            .TrimStart('/', '\\')
            .Split(['/', '\\'], StringSplitOptions.RemoveEmptyEntries)
            .Select(segment => NormalizeRelativePathSegment(segment, nameof(sessionPath)))
            .ToArray();

        return Path.Join([providerRoot, safeSessionId, .. relativeSegments]);
    }

    private static async Task WaitForConditionAsync(Func<bool> condition, TimeSpan? timeout = null)
    {
        await WaitForConditionAsync(() => Task.FromResult(condition()), timeout);
    }

    private static async Task WaitForConditionAsync(Func<Task<bool>> condition, TimeSpan? timeout = null)
    {
        var deadline = DateTime.UtcNow + (timeout ?? TimeSpan.FromSeconds(30));
        Exception? lastException = null;
        while (DateTime.UtcNow < deadline)
        {
            try
            {
                if (await condition())
                {
                    return;
                }
            }
            catch (IOException ex)
            {
                lastException = ex;
            }
            catch (UnauthorizedAccessException ex)
            {
                lastException = ex;
            }

            await Task.Delay(100);
        }

        throw new TimeoutException("Timed out waiting for condition.", lastException);
    }

    private static async Task<string> ReadAllTextSharedAsync(string path, CancellationToken cancellationToken = default)
    {
        await using var stream = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite | FileShare.Delete);
        using var reader = new StreamReader(stream);
        return await reader.ReadToEndAsync(cancellationToken);
    }

    private static async Task TryDeleteDirectoryAsync(string path)
    {
        if (!Directory.Exists(path))
        {
            return;
        }

        var deadline = DateTime.UtcNow + TimeSpan.FromSeconds(5);
        Exception? lastException = null;

        while (DateTime.UtcNow < deadline)
        {
            try
            {
                if (!Directory.Exists(path))
                {
                    return;
                }

                Directory.Delete(path, recursive: true);
                return;
            }
            catch (IOException ex)
            {
                lastException = ex;
            }
            catch (UnauthorizedAccessException ex)
            {
                lastException = ex;
            }

            await Task.Delay(100);
        }

        if (lastException is not null)
        {
            throw lastException;
        }
    }

    private static string NormalizeRelativePathSegment(string segment, string paramName)
    {
        if (string.IsNullOrWhiteSpace(segment))
        {
            throw new InvalidOperationException($"{paramName} must not be empty.");
        }

        var normalized = segment.TrimStart(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
        if (Path.IsPathRooted(normalized) || normalized.Contains(Path.VolumeSeparatorChar))
        {
            throw new InvalidOperationException($"{paramName} must be a relative path segment: {segment}");
        }

        return normalized;
    }

    private sealed class TestSessionFsHandler(string sessionId, string rootDir) : SessionFsProvider
    {
        protected override async Task<string> ReadFileAsync(string path, CancellationToken cancellationToken)
        {
            return await File.ReadAllTextAsync(ResolvePath(path), cancellationToken);
        }

        protected override async Task WriteFileAsync(string path, string content, int? mode, CancellationToken cancellationToken)
        {
            var fullPath = ResolvePath(path);
            Directory.CreateDirectory(Path.GetDirectoryName(fullPath)!);
            await File.WriteAllTextAsync(fullPath, content, cancellationToken);
        }

        protected override async Task AppendFileAsync(string path, string content, int? mode, CancellationToken cancellationToken)
        {
            var fullPath = ResolvePath(path);
            Directory.CreateDirectory(Path.GetDirectoryName(fullPath)!);
            await File.AppendAllTextAsync(fullPath, content, cancellationToken);
        }

        protected override Task<bool> ExistsAsync(string path, CancellationToken cancellationToken)
        {
            var fullPath = ResolvePath(path);
            return Task.FromResult(File.Exists(fullPath) || Directory.Exists(fullPath));
        }

        protected override Task<SessionFsStatResult> StatAsync(string path, CancellationToken cancellationToken)
        {
            var fullPath = ResolvePath(path);
            if (File.Exists(fullPath))
            {
                var info = new FileInfo(fullPath);
                return Task.FromResult(new SessionFsStatResult
                {
                    IsFile = true,
                    IsDirectory = false,
                    Size = info.Length,
                    Mtime = info.LastWriteTimeUtc,
                    Birthtime = info.CreationTimeUtc,
                });
            }

            var dirInfo = new DirectoryInfo(fullPath);
            if (!dirInfo.Exists)
            {
                throw new DirectoryNotFoundException($"Path does not exist: {path}");
            }

            return Task.FromResult(new SessionFsStatResult
            {
                IsFile = false,
                IsDirectory = true,
                Size = 0,
                Mtime = dirInfo.LastWriteTimeUtc,
                Birthtime = dirInfo.CreationTimeUtc,
            });
        }

        protected override Task MkdirAsync(string path, bool recursive, int? mode, CancellationToken cancellationToken)
        {
            Directory.CreateDirectory(ResolvePath(path));
            return Task.CompletedTask;
        }

        protected override Task<IList<string>> ReaddirAsync(string path, CancellationToken cancellationToken)
        {
            IList<string> entries = Directory
                .EnumerateFileSystemEntries(ResolvePath(path))
                .Select(Path.GetFileName)
                .Where(name => name is not null)
                .Cast<string>()
                .ToList();
            return Task.FromResult(entries);
        }

        protected override Task<IList<SessionFsReaddirWithTypesEntry>> ReaddirWithTypesAsync(string path, CancellationToken cancellationToken)
        {
            IList<SessionFsReaddirWithTypesEntry> entries = Directory
                .EnumerateFileSystemEntries(ResolvePath(path))
                .Select(p => new SessionFsReaddirWithTypesEntry
                {
                    Name = Path.GetFileName(p),
                    Type = Directory.Exists(p) ? SessionFsReaddirWithTypesEntryType.Directory : SessionFsReaddirWithTypesEntryType.File,
                })
                .ToList();
            return Task.FromResult(entries);
        }

        protected override Task RmAsync(string path, bool recursive, bool force, CancellationToken cancellationToken)
        {
            var fullPath = ResolvePath(path);

            if (File.Exists(fullPath))
            {
                File.Delete(fullPath);
                return Task.CompletedTask;
            }

            if (Directory.Exists(fullPath))
            {
                Directory.Delete(fullPath, recursive);
                return Task.CompletedTask;
            }

            if (force)
            {
                return Task.CompletedTask;
            }

            throw new FileNotFoundException($"Path does not exist: {path}");
        }

        protected override Task RenameAsync(string src, string dest, CancellationToken cancellationToken)
        {
            var srcPath = ResolvePath(src);
            var destPath = ResolvePath(dest);
            Directory.CreateDirectory(Path.GetDirectoryName(destPath)!);

            if (Directory.Exists(srcPath))
            {
                Directory.Move(srcPath, destPath);
            }
            else
            {
                File.Move(srcPath, destPath, overwrite: true);
            }

            return Task.CompletedTask;
        }

        private string ResolvePath(string sessionPath)
        {
            var normalizedSessionId = NormalizeRelativePathSegment(sessionId, nameof(sessionId));
            var sessionRoot = Path.GetFullPath(Path.Join(rootDir, normalizedSessionId));
            var relativeSegments = sessionPath
                .TrimStart('/', '\\')
                .Split(['/', '\\'], StringSplitOptions.RemoveEmptyEntries)
                .Select(segment => NormalizeRelativePathSegment(segment, nameof(sessionPath)))
                .ToArray();

            var fullPath = Path.GetFullPath(Path.Join([sessionRoot, .. relativeSegments]));
            if (!fullPath.StartsWith(sessionRoot, StringComparison.Ordinal))
            {
                throw new InvalidOperationException($"Path escapes session root: {sessionPath}");
            }

            return fullPath;
        }
    }
}
