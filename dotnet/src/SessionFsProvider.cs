/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using System.Diagnostics.CodeAnalysis;
using System.Text.Json;

namespace GitHub.Copilot;

/// <summary>
/// Result of a SQLite query execution via <see cref="ISessionFsSqliteProvider"/>.
/// Same shape as <see cref="SessionFsSqliteQueryResult"/> but without the <c>Error</c> field,
/// since providers signal errors by throwing.
/// </summary>
public sealed class SessionFsSqliteResult
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
/// One statement in an atomic SQLite transaction passed to
/// <see cref="ISessionFsSqliteTransactionProvider.TransactionAsync"/>.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionFsSqliteStatement
{
    /// <summary>How to execute: <c>"exec"</c>, <c>"query"</c>, or <c>"run"</c>.</summary>
    public SessionFsSqliteQueryType QueryType { get; set; }

    /// <summary>SQL statement to execute.</summary>
    public string Query { get; set; } = string.Empty;

    /// <summary>Optional named bind parameters.</summary>
    public IDictionary<string, object?>? Params { get; set; }
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
/// Optional capability for session filesystem providers that support atomic SQLite transactions.
/// </summary>
public interface ISessionFsSqliteTransactionProvider
{
    /// <summary>
    /// Executes <paramref name="statements"/> atomically against the per-session database.
    /// </summary>
    /// <param name="statements">Statements to execute in order, inside a single transaction.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>One result per statement, in the same order as <paramref name="statements"/>.</returns>
    /// <exception cref="SessionFsSqliteTransactionException">
    /// Thrown to tell the runtime how the failure should be classified. Any other exception
    /// is reported as <see cref="SessionFsSqliteTransactionErrorClass.Fatal"/>.
    /// </exception>
    Task<IList<SessionFsSqliteResult>> TransactionAsync(
        IList<SessionFsSqliteStatement> statements,
        CancellationToken cancellationToken);
}

/// <summary>
/// Thrown by an <see cref="ISessionFsSqliteTransactionProvider"/> to classify a failed SQLite transaction.
/// <see cref="SessionFsSqliteTransactionErrorClass.BusyOrLocked"/> guarantees the transaction
/// rolled back and is safe to retry; <see cref="SessionFsSqliteTransactionErrorClass.PostCommitAmbiguous"/>
/// must never be retried.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public sealed class SessionFsSqliteTransactionException : Exception
{
    /// <summary>Initializes a new instance of the <see cref="SessionFsSqliteTransactionException"/> class.</summary>
    /// <param name="message">Human-readable failure description.</param>
    /// <param name="errorClass">How the runtime should classify the failure.</param>
    /// <param name="innerException">Optional underlying exception.</param>
    public SessionFsSqliteTransactionException(
        string message,
        SessionFsSqliteTransactionErrorClass errorClass,
        Exception? innerException = null)
        : base(message, innerException)
    {
        ErrorClass = errorClass;
    }

    /// <summary>Gets the failure classification reported to the runtime.</summary>
    public SessionFsSqliteTransactionErrorClass ErrorClass { get; }
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
    protected abstract Task MakeDirectoryAsync(string path, bool recursive, int? mode, CancellationToken cancellationToken);

    /// <summary>Lists entry names in a directory. Throw if the directory does not exist.</summary>
    /// <param name="path">SessionFs-relative path.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    protected abstract Task<IList<string>> ReadDirectoryAsync(string path, CancellationToken cancellationToken);

    /// <summary>Lists entries with type info in a directory. Throw if the directory does not exist.</summary>
    /// <param name="path">SessionFs-relative path.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    protected abstract Task<IList<SessionFsReaddirWithTypesEntry>> ReadDirectoryWithTypesAsync(string path, CancellationToken cancellationToken);

    /// <summary>Removes a file or directory. Throw if the path does not exist (unless <paramref name="force"/> is true).</summary>
    /// <param name="path">SessionFs-relative path.</param>
    /// <param name="recursive">Whether to remove directory contents recursively.</param>
    /// <param name="force">If true, do not throw when the path does not exist.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    protected abstract Task RemoveAsync(string path, bool recursive, bool force, CancellationToken cancellationToken);

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
            await MakeDirectoryAsync(request.Path, request.Recursive ?? false, (int?)request.Mode, cancellationToken).ConfigureAwait(false);
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
            var entries = await ReadDirectoryAsync(request.Path, cancellationToken).ConfigureAwait(false);
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
            var entries = await ReadDirectoryWithTypesAsync(request.Path, cancellationToken).ConfigureAwait(false);
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
            await RemoveAsync(request.Path, request.Recursive ?? false, request.Force ?? false, cancellationToken).ConfigureAwait(false);
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
                    kvp => ToJsonElement(kvp.Value))).ToList() ?? [],
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

    async Task<SessionFsSqliteTransactionResult> ISessionFsHandler.SqliteTransactionAsync(SessionFsSqliteTransactionRequest request, CancellationToken cancellationToken)
    {
        if (this is not ISessionFsSqliteTransactionProvider transactionProvider)
        {
            return new SessionFsSqliteTransactionResult
            {
                Error = new SessionFsSqliteTransactionError
                {
                    ErrorClass = SessionFsSqliteTransactionErrorClass.Fatal,
                    Message = "SQLite is not supported by this provider.",
                },
            };
        }

        IList<SessionFsSqliteResult> results;
        try
        {
            var statements = request.Statements.Select(statement => new SessionFsSqliteStatement
            {
                QueryType = statement.QueryType,
                Query = statement.Query,
                Params = statement.Params?.ToDictionary(kvp => kvp.Key, kvp => JsonElementToValue(kvp.Value)),
            }).ToList();
            results = await transactionProvider.TransactionAsync(statements, cancellationToken).ConfigureAwait(false);
        }
        catch (SessionFsSqliteTransactionException ex)
        {
            return new SessionFsSqliteTransactionResult
            {
                Error = new SessionFsSqliteTransactionError { ErrorClass = ex.ErrorClass, Message = ex.Message },
            };
        }
        catch (Exception ex)
        {
            return new SessionFsSqliteTransactionResult
            {
                Error = new SessionFsSqliteTransactionError
                {
                    ErrorClass = SessionFsSqliteTransactionErrorClass.Fatal,
                    Message = ex.Message,
                },
            };
        }

        try
        {
            return new SessionFsSqliteTransactionResult
            {
                Results = results.Select(result => new SessionFsSqliteQueryResult
                {
                    Rows = result.Rows?.Select(row => (IDictionary<string, JsonElement>)row.ToDictionary(
                        kvp => kvp.Key,
                        kvp => ToJsonElement(kvp.Value))).ToList() ?? [],
                    Columns = result.Columns ?? [],
                    RowsAffected = result.RowsAffected,
                    LastInsertRowid = result.LastInsertRowid,
                }).ToList(),
            };
        }
        catch (Exception ex)
        {
            return new SessionFsSqliteTransactionResult
            {
                Error = new SessionFsSqliteTransactionError
                {
                    ErrorClass = SessionFsSqliteTransactionErrorClass.PostCommitAmbiguous,
                    Message = ex.Message,
                },
            };
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

    private static JsonElement ToJsonElement(object? value) =>
        CopilotClient.ToJsonElementForWire(value) ?? JsonElement.Parse("null");

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
