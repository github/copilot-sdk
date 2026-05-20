/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using System.Text.Json;

namespace GitHub.Copilot.SDK;

/// <summary>
/// Result of a SQLite query execution via <see cref="ISessionFsSqliteProvider"/>.
/// Same shape as <see cref="SessionFsSqliteQueryResult"/> but without the <c>Error</c> field,
/// since providers signal errors by throwing.
/// </summary>
public class SessionFsSqliteResult
{
    /// <summary>Column names from the result set.</summary>
    public IList<string> Columns { get; set; } = [];

    /// <summary>For SELECT: rows as column-keyed dictionaries. For others: empty.</summary>
    public IList<IDictionary<string, object>> Rows { get; set; } = [];

    /// <summary>Number of rows affected (for INSERT/UPDATE/DELETE).</summary>
    public long RowsAffected { get; set; }

    /// <summary>Last inserted row ID (for INSERT).</summary>
    public long? LastInsertRowid { get; set; }
}

/// <summary>
/// Optional interface for <see cref="SessionFsProvider"/> subclasses that support
/// per-session SQLite databases. Implement this interface on your provider to enable
/// the runtime's SQL tool to route queries through your SessionFs implementation.
/// </summary>
public interface ISessionFsSqliteProvider
{
    /// <summary>
    /// Executes a SQLite query against the per-session database.
    /// </summary>
    /// <param name="queryType">How to execute: <c>"exec"</c> for DDL/multi-statement, <c>"query"</c> for SELECT, <c>"run"</c> for INSERT/UPDATE/DELETE.</param>
    /// <param name="query">SQL query to execute.</param>
    /// <param name="bindParams">Optional named bind parameters.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>The query result, or <c>null</c> for exec-type queries.</returns>
    Task<SessionFsSqliteResult?> QueryAsync(
        SessionFsSqliteQueryType queryType,
        string query,
        IDictionary<string, object?>? bindParams,
        CancellationToken cancellationToken);

    /// <summary>
    /// Checks whether the per-session SQLite database already exists, without creating it.
    /// </summary>
    /// <param name="cancellationToken">Cancellation token.</param>
    Task<bool> ExistsAsync(CancellationToken cancellationToken);
}

/// <summary>
/// Base class for session filesystem providers. Subclasses override the
/// virtual methods and use normal C# patterns (return values, throw exceptions).
/// The base class catches exceptions and converts them to <see cref="SessionFsError"/>
/// results expected by the runtime.
/// To add SQLite support, also implement <see cref="ISessionFsSqliteProvider"/>.
/// </summary>
public abstract class SessionFsProvider : ISessionFsHandler
{
    /// <summary>Reads the full content of a file. Throw if the file does not exist.</summary>
    /// <param name="path">SessionFs-relative path.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>The file content as a UTF-8 string.</returns>
    protected abstract Task<string> ReadFileAsync(string path, CancellationToken cancellationToken);

    /// <summary>Writes content to a file, creating it (and parent directories) if needed.</summary>
    /// <param name="path">SessionFs-relative path.</param>
    /// <param name="content">Content to write.</param>
    /// <param name="mode">Optional POSIX-style permission mode. Null means use OS default.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    protected abstract Task WriteFileAsync(string path, string content, int? mode, CancellationToken cancellationToken);

    /// <summary>Appends content to a file, creating it (and parent directories) if needed.</summary>
    /// <param name="path">SessionFs-relative path.</param>
    /// <param name="content">Content to append.</param>
    /// <param name="mode">Optional POSIX-style permission mode. Null means use OS default.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    protected abstract Task AppendFileAsync(string path, string content, int? mode, CancellationToken cancellationToken);

    /// <summary>Checks whether a path exists.</summary>
    /// <param name="path">SessionFs-relative path.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns><c>true</c> if the path exists, <c>false</c> otherwise.</returns>
    protected abstract Task<bool> ExistsAsync(string path, CancellationToken cancellationToken);

    /// <summary>Gets metadata about a file or directory. Throw if the path does not exist.</summary>
    /// <param name="path">SessionFs-relative path.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    protected abstract Task<SessionFsStatResult> StatAsync(string path, CancellationToken cancellationToken);

    /// <summary>Creates a directory (and optionally parents). Does not fail if it already exists.</summary>
    /// <param name="path">SessionFs-relative path.</param>
    /// <param name="recursive">Whether to create parent directories.</param>
    /// <param name="mode">Optional POSIX-style permission mode (e.g., 0x1FF for 0777). Null means use OS default.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    protected abstract Task MkdirAsync(string path, bool recursive, int? mode, CancellationToken cancellationToken);

    /// <summary>Lists entry names in a directory. Throw if the directory does not exist.</summary>
    /// <param name="path">SessionFs-relative path.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    protected abstract Task<IList<string>> ReaddirAsync(string path, CancellationToken cancellationToken);

    /// <summary>Lists entries with type info in a directory. Throw if the directory does not exist.</summary>
    /// <param name="path">SessionFs-relative path.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    protected abstract Task<IList<SessionFsReaddirWithTypesEntry>> ReaddirWithTypesAsync(string path, CancellationToken cancellationToken);

    /// <summary>Removes a file or directory. Throw if the path does not exist (unless <paramref name="force"/> is true).</summary>
    /// <param name="path">SessionFs-relative path.</param>
    /// <param name="recursive">Whether to remove directory contents recursively.</param>
    /// <param name="force">If true, do not throw when the path does not exist.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    protected abstract Task RmAsync(string path, bool recursive, bool force, CancellationToken cancellationToken);

    /// <summary>Renames/moves a file or directory.</summary>
    /// <param name="src">Source path.</param>
    /// <param name="dest">Destination path.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    protected abstract Task RenameAsync(string src, string dest, CancellationToken cancellationToken);

    // ---- ISessionFsHandler implementation (private, handles error mapping) ----

    async Task<SessionFsReadFileResult> ISessionFsHandler.ReadFileAsync(SessionFsReadFileRequest request, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(request);

        try
        {
            var content = await ReadFileAsync(request.Path, cancellationToken).ConfigureAwait(false);
            return new SessionFsReadFileResult { Content = content };
        }
        catch (Exception ex)
        {
            return new SessionFsReadFileResult { Error = ToSessionFsError(ex) };
        }
    }

    async Task<SessionFsError?> ISessionFsHandler.WriteFileAsync(SessionFsWriteFileRequest request, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(request);

        try
        {
            await WriteFileAsync(request.Path, request.Content, (int?)request.Mode, cancellationToken).ConfigureAwait(false);
            return null;
        }
        catch (Exception ex)
        {
            return ToSessionFsError(ex);
        }
    }

    async Task<SessionFsError?> ISessionFsHandler.AppendFileAsync(SessionFsAppendFileRequest request, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(request);

        try
        {
            await AppendFileAsync(request.Path, request.Content, (int?)request.Mode, cancellationToken).ConfigureAwait(false);
            return null;
        }
        catch (Exception ex)
        {
            return ToSessionFsError(ex);
        }
    }

    async Task<SessionFsExistsResult> ISessionFsHandler.ExistsAsync(SessionFsExistsRequest request, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(request);

        try
        {
            var exists = await ExistsAsync(request.Path, cancellationToken).ConfigureAwait(false);
            return new SessionFsExistsResult { Exists = exists };
        }
        catch
        {
            return new SessionFsExistsResult { Exists = false };
        }
    }

    async Task<SessionFsStatResult> ISessionFsHandler.StatAsync(SessionFsStatRequest request, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(request);

        try
        {
            return await StatAsync(request.Path, cancellationToken).ConfigureAwait(false);
        }
        catch (Exception ex)
        {
            return new SessionFsStatResult { Error = ToSessionFsError(ex) };
        }
    }

    async Task<SessionFsError?> ISessionFsHandler.MkdirAsync(SessionFsMkdirRequest request, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(request);

        try
        {
            await MkdirAsync(request.Path, request.Recursive ?? false, (int?)request.Mode, cancellationToken).ConfigureAwait(false);
            return null;
        }
        catch (Exception ex)
        {
            return ToSessionFsError(ex);
        }
    }

    async Task<SessionFsReaddirResult> ISessionFsHandler.ReaddirAsync(SessionFsReaddirRequest request, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(request);

        try
        {
            var entries = await ReaddirAsync(request.Path, cancellationToken).ConfigureAwait(false);
            return new SessionFsReaddirResult { Entries = entries };
        }
        catch (Exception ex)
        {
            return new SessionFsReaddirResult { Error = ToSessionFsError(ex) };
        }
    }

    async Task<SessionFsReaddirWithTypesResult> ISessionFsHandler.ReaddirWithTypesAsync(SessionFsReaddirWithTypesRequest request, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(request);

        try
        {
            var entries = await ReaddirWithTypesAsync(request.Path, cancellationToken).ConfigureAwait(false);
            return new SessionFsReaddirWithTypesResult { Entries = entries };
        }
        catch (Exception ex)
        {
            return new SessionFsReaddirWithTypesResult { Error = ToSessionFsError(ex) };
        }
    }

    async Task<SessionFsError?> ISessionFsHandler.RmAsync(SessionFsRmRequest request, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(request);

        try
        {
            await RmAsync(request.Path, request.Recursive ?? false, request.Force ?? false, cancellationToken).ConfigureAwait(false);
            return null;
        }
        catch (Exception ex)
        {
            return ToSessionFsError(ex);
        }
    }

    async Task<SessionFsError?> ISessionFsHandler.RenameAsync(SessionFsRenameRequest request, CancellationToken cancellationToken)
    {
        ArgumentNullException.ThrowIfNull(request);

        try
        {
            await RenameAsync(request.Src, request.Dest, cancellationToken).ConfigureAwait(false);
            return null;
        }
        catch (Exception ex)
        {
            return ToSessionFsError(ex);
        }
    }

    async Task<SessionFsSqliteQueryResult> ISessionFsHandler.SqliteQueryAsync(SessionFsSqliteQueryRequest request, CancellationToken cancellationToken)
    {
        if (this is not ISessionFsSqliteProvider sqliteProvider)
        {
            return new SessionFsSqliteQueryResult
            {
                Error = new SessionFsError { Code = SessionFsErrorCode.UNKNOWN, Message = "SQLite is not supported by this provider." },
            };
        }

        try
        {
            var bindParams = request.Params?.ToDictionary(
                kvp => kvp.Key,
                kvp => JsonElementToValue(kvp.Value));
            var result = await sqliteProvider.QueryAsync(request.QueryType, request.Query, bindParams, cancellationToken).ConfigureAwait(false);

            return new SessionFsSqliteQueryResult
            {
                Rows = result?.Rows?.Select(row => (IDictionary<string, JsonElement>)row.ToDictionary(
                    kvp => kvp.Key,
                    kvp => JsonSerializer.SerializeToElement(kvp.Value, TypesJsonContext.Default.Object))).ToList() ?? [],
                Columns = result?.Columns ?? [],
                RowsAffected = result?.RowsAffected ?? 0,
                LastInsertRowid = result?.LastInsertRowid,
            };
        }
        catch (Exception ex)
        {
            return new SessionFsSqliteQueryResult { Error = ToSessionFsError(ex) };
        }
    }

    async Task<SessionFsSqliteExistsResult> ISessionFsHandler.SqliteExistsAsync(SessionFsSqliteExistsRequest request, CancellationToken cancellationToken)
    {
        if (this is not ISessionFsSqliteProvider sqliteProvider)
        {
            return new SessionFsSqliteExistsResult { Exists = false };
        }

        try
        {
            var exists = await sqliteProvider.ExistsAsync(cancellationToken).ConfigureAwait(false);
            return new SessionFsSqliteExistsResult { Exists = exists };
        }
        catch
        {
            return new SessionFsSqliteExistsResult { Exists = false };
        }
    }


    private static SessionFsError ToSessionFsError(Exception ex)
    {
        var code = ex is FileNotFoundException or DirectoryNotFoundException
            ? SessionFsErrorCode.ENOENT
            : SessionFsErrorCode.UNKNOWN;
        return new SessionFsError { Code = code, Message = ex.Message };
    }

    private static object? JsonElementToValue(JsonElement element) => element.ValueKind switch
    {
        JsonValueKind.Null => null,
        JsonValueKind.True => true,
        JsonValueKind.False => false,
        JsonValueKind.String => element.GetString(),
        JsonValueKind.Number => element.TryGetInt64(out var l) ? l : element.GetDouble(),
        _ => element.GetRawText(),
    };
}
