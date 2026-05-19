/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
using GitHub.Copilot.SDK;
using GitHub.Copilot.SDK.Rpc;

namespace GitHub.Copilot.SDK.Test.E2E;

internal record SqliteCall(string SessionId, string QueryType, string Query);

/// <summary>
/// A SessionFsProvider that implements <see cref="ISessionFsSqliteProvider"/> with stub SQLite
/// responses, and uses a simple <see cref="ConcurrentDictionary{TKey,TValue}"/> for file
/// operations instead of touching disk.
///
/// Returns canned responses based on query type rather than executing real SQL, since the
/// CAPI replay snapshots contain pre-recorded tool results.
/// </summary>
internal sealed class InMemorySessionFsSqliteHandler(string sessionId, List<SqliteCall> sqliteCalls)
    : SessionFsProvider, ISessionFsSqliteProvider
{
    internal ConcurrentDictionary<string, string> Files { get; } = new();
    private readonly ConcurrentDictionary<string, byte> _directories = new();
    private bool _hadQuery;

    // ---- ISessionFsSqliteProvider ----

    public Task<SessionFsSqliteResult?> QueryAsync(
        SessionFsSqliteQueryType queryType,
        string query,
        IDictionary<string, object>? bindParams,
        CancellationToken cancellationToken)
    {
        sqliteCalls.Add(new SqliteCall(sessionId, queryType.Value, query));
        _hadQuery = true;

        var trimmed = query.Trim();
        if (trimmed.Length == 0)
        {
            return Task.FromResult<SessionFsSqliteResult?>(null);
        }

        // Return canned results based on query type. The CLI formats tool results from the
        // SessionFsSqliteResult, and the CAPI replay snapshots contain the expected formatted
        // output. These stubs produce results that match the snapshot expectations.
        if (queryType == SessionFsSqliteQueryType.Exec)
        {
            return Task.FromResult<SessionFsSqliteResult?>(null);
        }

        if (queryType == SessionFsSqliteQueryType.Query)
        {
            var upper = trimmed.ToUpperInvariant();
            if (upper.Contains("SELECT"))
            {
                return Task.FromResult<SessionFsSqliteResult?>(new SessionFsSqliteResult
                {
                    Columns = ["id", "name"],
                    Rows = [["a1", "Widget"]],
                    RowsAffected = 0,
                });
            }

            return Task.FromResult<SessionFsSqliteResult?>(new SessionFsSqliteResult
            {
                Columns = [],
                Rows = [],
                RowsAffected = 0,
            });
        }

        if (queryType == SessionFsSqliteQueryType.Run)
        {
            return Task.FromResult<SessionFsSqliteResult?>(new SessionFsSqliteResult
            {
                Columns = [],
                Rows = [],
                RowsAffected = 1,
                LastInsertRowid = 1,
            });
        }

        throw new ArgumentException($"Unknown queryType: {queryType}");
    }

    public Task<bool> ExistsAsync(CancellationToken cancellationToken)
    {
        return Task.FromResult(_hadQuery);
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
