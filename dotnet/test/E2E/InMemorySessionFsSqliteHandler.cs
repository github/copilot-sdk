/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
using GitHub.Copilot.SDK;
using GitHub.Copilot.SDK.Rpc;
using Microsoft.Data.Sqlite;

namespace GitHub.Copilot.SDK.Test.E2E;

internal record SqliteCall(string SessionId, string QueryType, string Query);

/// <summary>
/// A SessionFsProvider that implements <see cref="ISessionFsSqliteProvider"/> with a real
/// in-memory SQLite database, and uses a simple <see cref="ConcurrentDictionary{TKey,TValue}"/>
/// for file operations instead of touching disk.
/// </summary>
internal sealed class InMemorySessionFsSqliteHandler(string sessionId, List<SqliteCall> sqliteCalls)
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
