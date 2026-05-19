/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
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
        SessionStatePath = "/session-state",
        Conventions = SessionFsSetProviderConventions.Posix,
        Capabilities = new SessionFsSetProviderCapabilities { Sqlite = true },
    };

    private readonly List<SqliteCall> _sqliteCalls = [];

    [Fact]
    public async Task Should_Route_Sql_Queries_Through_The_Sessionfs_Sqlite_Handler()
    {
        await using var client = CreateSessionFsClient();

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            CreateSessionFsHandler = s => new TestSessionFsHandlerWithSqlite(s.SessionId, _sqliteCalls),
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

    [Fact]
    public async Task Should_Allow_Subagents_To_Use_Sql_Tool_Via_Inherited_Sessionfs()
    {
        await using var client = CreateSessionFsClient();

        var handler = (TestSessionFsHandlerWithSqlite?)null;
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            CreateSessionFsHandler = s =>
            {
                handler = new TestSessionFsHandlerWithSqlite(s.SessionId, _sqliteCalls);
                return handler;
            },
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

        // Verify that the sql tool execution in events.jsonl came from the subagent (has agentId)
        Assert.NotNull(handler);
        var eventsKey = $"/{session.SessionId}/session-state/events.jsonl";
        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(handler!.Files.ContainsKey(eventsKey)),
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: "Timed out waiting for events.jsonl to be written.");
        Assert.True(handler!.Files.TryGetValue(eventsKey, out var content));
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

    private CopilotClient CreateSessionFsClient()
    {
        return Ctx.CreateClient(
            useStdio: true,
            options: new CopilotClientOptions
            {
                SessionFs = SessionFsConfig,
            });
    }

    private record SqliteCall(string SessionId, string QueryType, string Query);

    /// <summary>
    /// A SessionFsProvider that implements <see cref="ISessionFsSqliteProvider"/> with a real
    /// in-memory SQLite database, and uses a simple <see cref="ConcurrentDictionary{TKey,TValue}"/>
    /// for file operations instead of touching disk.
    /// </summary>
    private sealed class TestSessionFsHandlerWithSqlite(string sessionId, List<SqliteCall> sqliteCalls)
        : SessionFsProvider, ISessionFsSqliteProvider
    {
        internal ConcurrentDictionary<string, string> Files { get; } = new();
        private readonly ConcurrentDictionary<string, byte> _directories = new();
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

        // ---- File operations (in-memory) ----

        private string Resolve(string path) => $"/{sessionId}{(path.StartsWith('/') ? path : "/" + path)}";

        protected override Task<string> ReadFileAsync(string path, CancellationToken cancellationToken)
        {
            var key = Resolve(path);
            if (!Files.TryGetValue(key, out var content))
                throw new FileNotFoundException($"File not found: {path}");
            return Task.FromResult(content);
        }

        protected override Task WriteFileAsync(string path, string content, int? mode, CancellationToken cancellationToken)
        {
            Files[Resolve(path)] = content;
            return Task.CompletedTask;
        }

        protected override Task AppendFileAsync(string path, string content, int? mode, CancellationToken cancellationToken)
        {
            Files.AddOrUpdate(Resolve(path), content, (_, existing) => existing + content);
            return Task.CompletedTask;
        }

        protected override Task<bool> ExistsAsync(string path, CancellationToken cancellationToken)
        {
            var key = Resolve(path);
            return Task.FromResult(Files.ContainsKey(key) || _directories.ContainsKey(key));
        }

        protected override Task<SessionFsStatResult> StatAsync(string path, CancellationToken cancellationToken)
        {
            var key = Resolve(path);
            if (Files.TryGetValue(key, out var fileContent))
                return Task.FromResult(new SessionFsStatResult { IsFile = true, IsDirectory = false, Size = fileContent.Length });
            if (_directories.ContainsKey(key))
                return Task.FromResult(new SessionFsStatResult { IsFile = false, IsDirectory = true, Size = 0 });
            throw new FileNotFoundException($"Path does not exist: {path}");
        }

        protected override Task MkdirAsync(string path, bool recursive, int? mode, CancellationToken cancellationToken)
        {
            _directories[Resolve(path)] = 0;
            return Task.CompletedTask;
        }

        protected override Task<IList<string>> ReaddirAsync(string path, CancellationToken cancellationToken)
            => Task.FromResult<IList<string>>([]);

        protected override Task<IList<SessionFsReaddirWithTypesEntry>> ReaddirWithTypesAsync(string path, CancellationToken cancellationToken)
            => Task.FromResult<IList<SessionFsReaddirWithTypesEntry>>([]);

        protected override Task RmAsync(string path, bool recursive, bool force, CancellationToken cancellationToken)
        {
            var key = Resolve(path);
            Files.TryRemove(key, out _);
            _directories.TryRemove(key, out _);
            return Task.CompletedTask;
        }

        protected override Task RenameAsync(string src, string dest, CancellationToken cancellationToken)
        {
            var srcKey = Resolve(src);
            var destKey = Resolve(dest);
            if (Files.TryRemove(srcKey, out var content))
                Files[destKey] = content;
            return Task.CompletedTask;
        }
    }
}
