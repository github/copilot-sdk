/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Data.Sqlite;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

public class SessionFsSqliteE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "session_fs_sqlite", output)
{
    private static readonly SessionFsConfig SessionFsConfig = new()
    {
        InitialCwd = "/",
        SessionStatePath = CreateSessionStatePath(),
        Conventions = SessionFsSetProviderConventions.Posix,
        Capabilities = new SessionFsSetProviderCapabilities { Sqlite = true },
    };

    private readonly List<SqliteCall> _sqliteCalls = [];

    [Fact]
    public async Task Should_Route_Sql_Queries_Through_The_Sessionfs_Sqlite_Handler()
    {
        var providerRoot = CreateProviderRoot();
        try
        {
            await using var client = CreateSessionFsClient(providerRoot);

            var session = await client.CreateSessionAsync(new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
                CreateSessionFsHandler = s => new TestSessionFsHandlerWithSqlite(s.SessionId, providerRoot, _sqliteCalls),
            });

            var msg = await session.SendAndWaitAsync(new MessageOptions
            {
                Prompt =
                    "Use the sql tool to create a table called \"items\" with columns id (TEXT PRIMARY KEY) and name (TEXT). " +
                    "Then insert a row with id \"a1\" and name \"Widget\". " +
                    "Then select all rows from items and tell me what you find.",
            });

            Assert.Contains("Widget", msg?.Data.Content ?? string.Empty);

            var sessionCalls = _sqliteCalls.Where(c => c.SessionId == session.SessionId).ToList();
            Assert.NotEmpty(sessionCalls);
            Assert.Contains(sessionCalls, c => c.Query.Contains("CREATE TABLE", StringComparison.OrdinalIgnoreCase));
            Assert.Contains(sessionCalls, c => c.Query.Contains("INSERT", StringComparison.OrdinalIgnoreCase));
            Assert.Contains(sessionCalls, c => c.Query.Contains("SELECT", StringComparison.OrdinalIgnoreCase));

            Assert.Contains(sessionCalls, c => c.QueryType == "exec");
            Assert.Contains(sessionCalls, c => c.QueryType == "query");
            Assert.Contains(sessionCalls, c => c.QueryType == "run");

            await session.DisposeAsync();
        }
        finally
        {
            await TryDeleteDirectoryAsync(providerRoot);
        }
    }

    [Fact]
    public async Task Should_Allow_Subagents_To_Use_Sql_Tool_Via_Inherited_Sessionfs()
    {
        var providerRoot = CreateProviderRoot();
        try
        {
            await using var client = CreateSessionFsClient(providerRoot);

            var session = await client.CreateSessionAsync(new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
                CreateSessionFsHandler = s => new TestSessionFsHandlerWithSqlite(s.SessionId, providerRoot, _sqliteCalls),
            });

            var events = new List<SessionEvent>();
            using var _ = session.On(evt => events.Add(evt));

            await session.SendAndWaitAsync(new MessageOptions
            {
                Prompt =
                    "Use the task tool to ask a task agent to do the following: " +
                    "Use the sql tool to run this query: INSERT INTO todos (id, title, status) VALUES ('subagent-test', 'Created by subagent', 'done')",
            });

            await session.DisposeAsync();

            var sessionCalls = _sqliteCalls.Where(c => c.SessionId == session.SessionId).ToList();
            var insertCalls = sessionCalls.Where(c => c.Query.Contains("INSERT", StringComparison.OrdinalIgnoreCase)).ToList();
            Assert.NotEmpty(insertCalls);

            var eventsPath = GetStoredPath(providerRoot, session.SessionId, $"{SessionFsConfig.SessionStatePath}/events.jsonl");
            await WaitForConditionAsync(() => File.Exists(eventsPath));
            var content = await ReadAllTextSharedAsync(eventsPath);
            var lines = content.Split('\n', StringSplitOptions.RemoveEmptyEntries);
            var sqlToolEvents = lines
                .Select(line => System.Text.Json.JsonDocument.Parse(line))
                .Where(doc =>
                    doc.RootElement.TryGetProperty("type", out var type) && type.GetString() == "tool.execution_start" &&
                    doc.RootElement.TryGetProperty("data", out var data) && data.TryGetProperty("toolName", out var toolName) && toolName.GetString() == "sql")
                .ToList();
            Assert.NotEmpty(sqlToolEvents);
            Assert.All(sqlToolEvents, evt =>
            {
                Assert.True(evt.RootElement.TryGetProperty("agentId", out var agentId));
                Assert.False(string.IsNullOrEmpty(agentId.GetString()));
            });
        }
        finally
        {
            await TryDeleteDirectoryAsync(providerRoot);
        }
    }

    private CopilotClient CreateSessionFsClient(string providerRoot)
    {
        Directory.CreateDirectory(providerRoot);
        return Ctx.CreateClient(
            useStdio: true,
            options: new CopilotClientOptions
            {
                SessionFs = SessionFsConfig,
            });
    }

    private static string CreateProviderRoot()
        => Path.Join(Path.GetTempPath(), $"copilot-sessionfs-sqlite-{Guid.NewGuid():N}");

    private static string CreateSessionStatePath()
    {
        if (OperatingSystem.IsWindows())
        {
            return "/session-state";
        }

        return Path.Join(Path.GetTempPath(), $"copilot-sessionfs-sqlite-state-{Guid.NewGuid():N}", "session-state")
            .Replace(Path.DirectorySeparatorChar, '/');
    }

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
        await TestHelper.WaitForConditionAsync(
            condition,
            timeout: timeout ?? TimeSpan.FromSeconds(30),
            timeoutMessage: "Timed out waiting for the session_fs_sqlite test condition.");
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

        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(DeleteDirectoryIfPresent(path)),
            timeout: TimeSpan.FromSeconds(5),
            timeoutMessage: $"Timed out deleting directory '{path}'.",
            transientExceptionFilter: TestHelper.IsTransientFileSystemException);

        static bool DeleteDirectoryIfPresent(string path)
        {
            if (!Directory.Exists(path))
            {
                return true;
            }

            Directory.Delete(path, recursive: true);
            return !Directory.Exists(path);
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

    private record SqliteCall(string SessionId, string QueryType, string Query);

    /// <summary>
    /// A SessionFsProvider that also implements <see cref="ISessionFsSqliteProvider"/>,
    /// backed by an in-memory SQLite database via Microsoft.Data.Sqlite.
    /// </summary>
    private sealed class TestSessionFsHandlerWithSqlite(string sessionId, string rootDir, List<SqliteCall> sqliteCalls)
        : SessionFsProvider, ISessionFsSqliteProvider
    {
        private SqliteConnection? _db;

        private SqliteConnection GetOrCreateDb()
        {
            if (_db is not null)
            {
                return _db;
            }

            _db = new SqliteConnection("Data Source=:memory:");
            _db.Open();
            using var cmd = _db.CreateCommand();
            cmd.CommandText = "PRAGMA busy_timeout = 5000";
            cmd.ExecuteNonQuery();
            return _db;
        }

        // ---- ISessionFsSqliteProvider ----

        public Task<SessionFsSqliteResult?> QueryAsync(
            SessionFsSqliteQueryType queryType,
            string query,
            IDictionary<string, object>? bindParams,
            CancellationToken cancellationToken)
        {
            sqliteCalls.Add(new SqliteCall(sessionId, queryType.Value, query));

            var trimmed = query.Trim();
            if (trimmed.Length == 0)
            {
                return Task.FromResult<SessionFsSqliteResult?>(null);
            }

            var db = GetOrCreateDb();

            if (queryType == SessionFsSqliteQueryType.Exec)
            {
                using var cmd = db.CreateCommand();
                cmd.CommandText = trimmed;
                cmd.ExecuteNonQuery();
                return Task.FromResult<SessionFsSqliteResult?>(null);
            }

            if (queryType == SessionFsSqliteQueryType.Query)
            {
                using var cmd = db.CreateCommand();
                cmd.CommandText = trimmed;
                AddParams(cmd, bindParams);

                using var reader = cmd.ExecuteReader();
                var columns = new List<string>();
                for (var i = 0; i < reader.FieldCount; i++)
                {
                    columns.Add(reader.GetName(i));
                }

                var rows = new List<IDictionary<string, object>>();
                while (reader.Read())
                {
                    var row = new Dictionary<string, object>();
                    for (var i = 0; i < reader.FieldCount; i++)
                    {
                        row[reader.GetName(i)] = reader.IsDBNull(i) ? null! : reader.GetValue(i);
                    }
                    rows.Add(row);
                }

                return Task.FromResult<SessionFsSqliteResult?>(new SessionFsSqliteResult
                {
                    Columns = columns,
                    Rows = rows,
                    RowsAffected = 0,
                });
            }

            if (queryType == SessionFsSqliteQueryType.Run)
            {
                using var cmd = db.CreateCommand();
                cmd.CommandText = trimmed;
                AddParams(cmd, bindParams);

                var rowsAffected = cmd.ExecuteNonQuery();

                // Get last insert rowid
                using var rowidCmd = db.CreateCommand();
                rowidCmd.CommandText = "SELECT last_insert_rowid()";
                var lastRowid = rowidCmd.ExecuteScalar();

                return Task.FromResult<SessionFsSqliteResult?>(new SessionFsSqliteResult
                {
                    Columns = [],
                    Rows = [],
                    RowsAffected = rowsAffected,
                    LastInsertRowid = lastRowid is long l ? l : null,
                });
            }

            throw new ArgumentException($"Unknown queryType: {queryType}");
        }

        public Task<bool> ExistsAsync(CancellationToken cancellationToken)
        {
            return Task.FromResult(_db is not null);
        }

        private static void AddParams(SqliteCommand cmd, IDictionary<string, object>? bindParams)
        {
            if (bindParams is null) return;
            foreach (var (key, value) in bindParams)
            {
                cmd.Parameters.AddWithValue(key.StartsWith(':') || key.StartsWith('$') || key.StartsWith('@') ? key : $":{key}", value ?? DBNull.Value);
            }
        }

        // ---- File operations (delegated to disk) ----

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
