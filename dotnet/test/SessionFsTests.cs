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
        Conventions = SessionFsSetProviderRequestConventions.Posix,
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
            TryDeleteDirectory(providerRoot);
        }
    }

    [Fact]
    public async Task Should_Load_Session_Data_From_Fs_Provider_On_Resume()
    {
        var providerRoot = CreateProviderRoot();
        try
        {
            await using var client = CreateSessionFsClient(providerRoot);
            Func<CopilotSession, ISessionFsHandler> createSessionFsHandler = s => new TestSessionFsHandler(s.SessionId, providerRoot);

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
            TryDeleteDirectory(providerRoot);
        }
    }

    [Fact]
    public async Task Should_Reject_SetProvider_When_Sessions_Already_Exist()
    {
        var providerRoot = CreateProviderRoot();
        try
        {
            await using var client1 = CreateSessionFsClient(providerRoot, useStdio: false);
            var createSessionFsHandler = (Func<CopilotSession, ISessionFsHandler>)(s => new TestSessionFsHandler(s.SessionId, providerRoot));

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
            TryDeleteDirectory(providerRoot);
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
        }
        finally
        {
            TryDeleteDirectory(providerRoot);
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

            await session.Rpc.Compaction.CompactAsync();
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
            TryDeleteDirectory(providerRoot);
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

    private static void TryDeleteDirectory(string path)
    {
        if (Directory.Exists(path))
        {
            Directory.Delete(path, recursive: true);
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

    private sealed class TestSessionFsHandler(string sessionId, string rootDir) : ISessionFsHandler
    {
        public async Task<SessionFsReadFileResult> ReadFileAsync(SessionFsReadFileParams request, CancellationToken cancellationToken = default)
        {
            var content = await File.ReadAllTextAsync(ResolvePath(request.Path), cancellationToken);
            return new SessionFsReadFileResult { Content = content };
        }

        public async Task WriteFileAsync(SessionFsWriteFileParams request, CancellationToken cancellationToken = default)
        {
            var fullPath = ResolvePath(request.Path);
            Directory.CreateDirectory(Path.GetDirectoryName(fullPath)!);
            await File.WriteAllTextAsync(fullPath, request.Content, cancellationToken);
        }

        public async Task AppendFileAsync(SessionFsAppendFileParams request, CancellationToken cancellationToken = default)
        {
            var fullPath = ResolvePath(request.Path);
            Directory.CreateDirectory(Path.GetDirectoryName(fullPath)!);
            await File.AppendAllTextAsync(fullPath, request.Content, cancellationToken);
        }

        public Task<SessionFsExistsResult> ExistsAsync(SessionFsExistsParams request, CancellationToken cancellationToken = default)
        {
            var fullPath = ResolvePath(request.Path);
            return Task.FromResult(new SessionFsExistsResult
            {
                Exists = File.Exists(fullPath) || Directory.Exists(fullPath),
            });
        }

        public Task<SessionFsStatResult> StatAsync(SessionFsStatParams request, CancellationToken cancellationToken = default)
        {
            var fullPath = ResolvePath(request.Path);
            if (File.Exists(fullPath))
            {
                var info = new FileInfo(fullPath);
                return Task.FromResult(new SessionFsStatResult
                {
                    IsFile = true,
                    IsDirectory = false,
                    Size = info.Length,
                    Mtime = info.LastWriteTimeUtc.ToString("O"),
                    Birthtime = info.CreationTimeUtc.ToString("O"),
                });
            }

            var dirInfo = new DirectoryInfo(fullPath);
            if (!dirInfo.Exists)
            {
                throw new FileNotFoundException($"Path does not exist: {request.Path}");
            }

            return Task.FromResult(new SessionFsStatResult
            {
                IsFile = false,
                IsDirectory = true,
                Size = 0,
                Mtime = dirInfo.LastWriteTimeUtc.ToString("O"),
                Birthtime = dirInfo.CreationTimeUtc.ToString("O"),
            });
        }

        public Task MkdirAsync(SessionFsMkdirParams request, CancellationToken cancellationToken = default)
        {
            Directory.CreateDirectory(ResolvePath(request.Path));
            return Task.CompletedTask;
        }

        public Task<SessionFsReaddirResult> ReaddirAsync(SessionFsReaddirParams request, CancellationToken cancellationToken = default)
        {
            var entries = Directory
                .EnumerateFileSystemEntries(ResolvePath(request.Path))
                .Select(Path.GetFileName)
                .Where(name => name is not null)
                .Cast<string>()
                .ToList();

            return Task.FromResult(new SessionFsReaddirResult { Entries = entries });
        }

        public Task<SessionFsReaddirWithTypesResult> ReaddirWithTypesAsync(SessionFsReaddirWithTypesParams request, CancellationToken cancellationToken = default)
        {
            var entries = Directory
                .EnumerateFileSystemEntries(ResolvePath(request.Path))
                .Select(path => new Entry
                {
                    Name = Path.GetFileName(path),
                    Type = Directory.Exists(path) ? EntryType.Directory : EntryType.File,
                })
                .ToList();

            return Task.FromResult(new SessionFsReaddirWithTypesResult { Entries = entries });
        }

        public Task RmAsync(SessionFsRmParams request, CancellationToken cancellationToken = default)
        {
            var fullPath = ResolvePath(request.Path);

            if (File.Exists(fullPath))
            {
                File.Delete(fullPath);
                return Task.CompletedTask;
            }

            if (Directory.Exists(fullPath))
            {
                Directory.Delete(fullPath, request.Recursive ?? false);
                return Task.CompletedTask;
            }

            if (request.Force == true)
            {
                return Task.CompletedTask;
            }

            throw new FileNotFoundException($"Path does not exist: {request.Path}");
        }

        public Task RenameAsync(SessionFsRenameParams request, CancellationToken cancellationToken = default)
        {
            var src = ResolvePath(request.Src);
            var dest = ResolvePath(request.Dest);
            Directory.CreateDirectory(Path.GetDirectoryName(dest)!);

            if (Directory.Exists(src))
            {
                Directory.Move(src, dest);
            }
            else
            {
                File.Move(src, dest, overwrite: true);
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
