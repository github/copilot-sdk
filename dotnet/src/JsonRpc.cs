/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Buffers;
using System.Collections.Concurrent;
using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;
using System.Globalization;
using System.Reflection;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.Json.Serialization.Metadata;
using System.Text.Unicode;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;

namespace GitHub.Copilot.SDK;

/// <summary>
/// A lightweight JSON-RPC 2.0 implementation covering only the features used
/// by this SDK to talk to the Copilot CLI. Messages are framed using the
/// LSP-style header convention (<c>Content-Length: N\r\n\r\n</c> followed by
/// N bytes of JSON body) — the same wire format used by the Language Server
/// Protocol and the Copilot CLI's other language SDKs (Go, Node, Python).
/// This is not a general-purpose JSON-RPC stack: it is narrowly scoped to the
/// methods, transports, and framing the CLI uses.
/// </summary>
internal sealed partial class JsonRpc : IDisposable
{
    private const int ErrorCodeMethodNotFound = -32601;
    private const int ErrorCodeInternalError = -32603;

    private readonly Stream _sendStream;
    private readonly Stream _receiveStream;
    private readonly JsonSerializerOptions _serializerOptions;
    private readonly ILogger _logger;
    private readonly ConcurrentDictionary<long, PendingRequest> _pendingRequests = new();
    private readonly ConcurrentDictionary<string, MethodRegistration> _methods = new();
    private readonly TaskCompletionSource _completionSource = new(TaskCreationOptions.RunContinuationsAsynchronously);
    private readonly SemaphoreSlim _writeLock = new(1, 1);
    private readonly CancellationTokenSource _disposeCts = new();
    private long _nextId;
    private bool _disposed;

    /// <summary>
    /// Initializes a new <see cref="JsonRpc"/>.
    /// </summary>
    /// <param name="sendStream">The stream to write outgoing messages to.</param>
    /// <param name="receiveStream">The stream to read incoming messages from.</param>
    /// <param name="serializerOptions">JSON serializer options (should include all needed source-gen contexts).</param>
    /// <param name="logger">Optional logger for diagnostics.</param>
    public JsonRpc(Stream sendStream, Stream receiveStream, JsonSerializerOptions serializerOptions, ILogger? logger = null)
    {
        _sendStream = sendStream;
        _receiveStream = receiveStream;
        _serializerOptions = serializerOptions;
        _logger = logger ?? NullLogger.Instance;
    }

    /// <summary>
    /// A <see cref="Task"/> that completes when the connection is closed or faulted.
    /// </summary>
    public Task Completion => _completionSource.Task;

    /// <summary>
    /// Begins reading messages from the receive stream. Call once after registering all method handlers.
    /// </summary>
    public void StartListening()
    {
        _ = ReadLoopAsync(_disposeCts.Token);
    }

    /// <summary>
    /// Sends a JSON-RPC request and waits for the response.
    /// </summary>
    public async Task<T> InvokeAsync<T>(string method, object?[]? args, CancellationToken cancellationToken)
    {
        var id = Interlocked.Increment(ref _nextId);
        var pending = new PendingRequest();
        _pendingRequests[id] = pending;

        CancellationTokenRegistration cancelRegistration = default;
        try
        {
            if (cancellationToken.CanBeCanceled)
            {
                cancelRegistration = cancellationToken.Register(static state =>
                {
                    var (self, reqId, ct) = ((JsonRpc, long, CancellationToken))state!;
                    if (self._pendingRequests.TryRemove(reqId, out var p))
                    {
                        p.TrySetCanceled(ct);
                    }

                    // Best-effort cancel notification
                    _ = self.SendCancelNotificationAsync(reqId);
                }, (this, id, cancellationToken));
            }

            // Send request message
            await SendMessageAsync(new JsonRpcRequest
            {
                Id = id,
                Method = method,
                Params = SerializeArgs(args),
            }, JsonRpcWireContext.Default.JsonRpcRequest, cancellationToken).ConfigureAwait(false);

            var responseElement = await pending.Task.ConfigureAwait(false);

            if (responseElement.ValueKind == JsonValueKind.Null || responseElement.ValueKind == JsonValueKind.Undefined)
            {
                return default!;
            }

            return (T)responseElement.Deserialize(_serializerOptions.GetTypeInfo(typeof(T)))!;
        }
        finally
        {
            _pendingRequests.TryRemove(id, out _);
            await cancelRegistration.DisposeAsync().ConfigureAwait(false);
        }
    }

    /// <summary>
    /// Registers a method handler that receives positional parameters.
    /// If singleObjectParam is false (the default), parameter names and types are inferred from the delegate's signature.
    /// If singleObjectParam is true, the entire params object is deserialized as the handler's first parameter.
    /// </summary>
    public void SetLocalRpcMethod(string methodName, Delegate handler, bool singleObjectParam = false)
    {
        _methods[methodName] = new MethodRegistration(handler, singleObjectParam);
    }

    /// <inheritdoc />
    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        _disposed = true;
        _disposeCts.Cancel();

        // Fail all pending requests
        foreach (var kvp in _pendingRequests)
        {
            if (_pendingRequests.TryRemove(kvp.Key, out var pending))
            {
                pending.TrySetException(new ObjectDisposedException(nameof(JsonRpc)));
            }
        }

        _completionSource.TrySetResult();
        _writeLock.Dispose();
    }

    private async Task SendMessageAsync<T>(T message, JsonTypeInfo<T> typeInfo, CancellationToken cancellationToken)
    {
        // "Content-Length: " (16) + max int digits (10) + "\r\n\r\n" (4)
        const int MaxHeaderLength = 30;

        var json = JsonSerializer.SerializeToUtf8Bytes(message, typeInfo);

        var headerBuf = ArrayPool<byte>.Shared.Rent(MaxHeaderLength);
        bool wrote = Utf8.TryWrite(headerBuf, $"Content-Length: {json.Length}\r\n\r\n", out int headerLen);
        Debug.Assert(wrote && headerLen > 0);

        await _writeLock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            await _sendStream.WriteAsync(headerBuf.AsMemory(0, headerLen), cancellationToken).ConfigureAwait(false);
            await _sendStream.WriteAsync(json, cancellationToken).ConfigureAwait(false);
            await _sendStream.FlushAsync(cancellationToken).ConfigureAwait(false);
        }
        finally
        {
            _writeLock.Release();
            ArrayPool<byte>.Shared.Return(headerBuf);
        }
    }

    private async Task ReadLoopAsync(CancellationToken cancellationToken)
    {
        var buffer = new byte[256];
        int carried = 0; // bytes in buffer carried over from previous read
        try
        {
            while (!cancellationToken.IsCancellationRequested)
            {
                // Read headers and body
                var (contentLength, buf, newCarried) = await ReadMessageAsync(buffer, carried, cancellationToken).ConfigureAwait(false);
                if (contentLength < 0)
                {
                    break; // Stream ended
                }

                // Keep the (possibly grown) buffer and carry-over count for next iteration
                buffer = buf;
                carried = newCarried;

                // Parse the raw JSON. Body is at buffer[0..contentLength], carried bytes
                // for the next message are at buffer[contentLength..contentLength+carried].
                JsonElement? message = null;
                try
                {
                    using var doc = JsonDocument.Parse(buffer.AsMemory(0, contentLength));
                    message = doc.RootElement.Clone();
                }
                catch (JsonException ex)
                {
                    _logger.LogWarning(ex, "Failed to parse incoming JSON-RPC message");
                }

                // Always move carried bytes to the front, even on parse failure — otherwise
                // the next ReadMessageAsync call would scan stale body bytes as headers.
                // This must happen AFTER parsing because the carried region overlaps where
                // the body lived.
                if (carried > 0)
                {
                    Buffer.BlockCopy(buffer, contentLength, buffer, 0, carried);
                }

                if (message is not { } parsed)
                {
                    continue;
                }

                // Route the message
                if (parsed.TryGetProperty("id", out var idProp) && !parsed.TryGetProperty("method", out _))
                {
                    // It's a response to one of our requests
                    HandleResponse(parsed, idProp);
                }
                else if (parsed.TryGetProperty("method", out var methodProp) && methodProp.GetString() is string methodName)
                {
                    _ = HandleIncomingMethodAsync(methodName, parsed, cancellationToken);
                }
            }
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
        {
            // Normal shutdown
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "JSON-RPC read loop ended");
        }
        finally
        {
            // Fail all pending requests
            foreach (var kvp in _pendingRequests)
            {
                if (_pendingRequests.TryRemove(kvp.Key, out var pending))
                {
                    pending.TrySetException(new ConnectionLostException());
                }
            }

            _completionSource.TrySetResult();
        }
    }

    /// <summary>
    /// Reads headers and body in one pass.
    /// On return, body is at buffer[0..ContentLength], and any overflow bytes
    /// from the next message are at buffer[ContentLength..ContentLength+Carried].
    /// The caller must move the carried bytes to the front before the next call.
    /// </summary>
    /// <param name="buffer">Shared buffer (may be grown).</param>
    /// <param name="carried">Bytes already in buffer[0..carried] from a previous read.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    private async ValueTask<(int ContentLength, byte[] Buffer, int Carried)> ReadMessageAsync(byte[] buffer, int carried, CancellationToken cancellationToken)
    {
        // Read until we find the \r\n\r\n header terminator.
        // carried bytes are already at buffer[0..carried].
        int filled = carried;
        int headerEnd = -1; // index of first byte after \r\n\r\n

        // Check carried bytes first for a header terminator
        {
            int pos = buffer.AsSpan(0, filled).IndexOf("\r\n\r\n"u8);
            if (pos >= 0)
            {
                headerEnd = pos + 4;
            }
        }

        while (headerEnd < 0)
        {
            if (filled == buffer.Length)
            {
                Array.Resize(ref buffer, buffer.Length * 2);
            }

            int bytesRead = await _receiveStream.ReadAsync(buffer.AsMemory(filled, buffer.Length - filled), cancellationToken).ConfigureAwait(false);
            if (bytesRead == 0)
            {
                // Clean EOF only if we haven't started a frame; otherwise the peer truncated mid-header.
                if (filled == 0)
                {
                    return (-1, buffer, 0);
                }

                throw new EndOfStreamException("Stream ended while reading JSON-RPC headers.");
            }

            filled += bytesRead;

            // Scan for \r\n\r\n starting from where a match could begin
            int scanStart = Math.Max(filled - bytesRead - 3, 0);
            int pos = buffer.AsSpan(scanStart, filled - scanStart).IndexOf("\r\n\r\n"u8);
            if (pos >= 0)
            {
                headerEnd = scanStart + pos + 4;
            }
        }

        // Parse Content-Length. LSP framing puts each header on its own \r\n-terminated
        // line; we walk the lines and require an exact "Content-Length: " prefix at the
        // start of one of them. A substring match anywhere in the header block would
        // false-positive on values like "X-Trace: Content-Length: 5" and desync the stream.
        // A missing or unparseable Content-Length means the framing is broken — there's
        // no safe way to resync, so throw and let the read loop terminate the connection.
        int contentLength = -1;
        ReadOnlySpan<byte> prefix = "Content-Length: "u8;
        // headerEnd points just past the \r\n\r\n terminator. Drop only the trailing
        // empty line's \r\n; each remaining header line is still \r\n-terminated and
        // gets split out by the IndexOf below.
        var headerLines = buffer.AsSpan(0, headerEnd - 2);
        while (!headerLines.IsEmpty)
        {
            int lineEnd = headerLines.IndexOf("\r\n"u8);
            ReadOnlySpan<byte> line = lineEnd >= 0 ? headerLines.Slice(0, lineEnd) : headerLines;

            if (line.StartsWith(prefix) &&
                (contentLength >= 0 ||
                 !int.TryParse(line.Slice(prefix.Length), NumberStyles.None, CultureInfo.InvariantCulture, out contentLength) ||
                 contentLength < 0))
            {
                throw new InvalidDataException("JSON-RPC frame has a missing, duplicate, or invalid Content-Length header.");
            }

            headerLines = lineEnd >= 0 ? headerLines.Slice(lineEnd + 2) : default;
        }

        if (contentLength < 0)
        {
            throw new InvalidDataException("JSON-RPC frame is missing the Content-Length header.");
        }

        // Bytes after the header that we already have
        int extraBytes = filled - headerEnd;

        // Ensure buffer is large enough for the body and any overflow already read.
        int needed = Math.Max(contentLength, extraBytes);
        if (needed > buffer.Length)
        {
            var newBuffer = new byte[needed];
            Buffer.BlockCopy(buffer, headerEnd, newBuffer, 0, extraBytes);
            buffer = newBuffer;
        }
        else if (extraBytes > 0)
        {
            Buffer.BlockCopy(buffer, headerEnd, buffer, 0, extraBytes);
        }

        // Read remaining body bytes if we don't have enough
        if (extraBytes < contentLength)
        {
            await _receiveStream.ReadExactlyAsync(buffer.AsMemory(extraBytes, contentLength - extraBytes), cancellationToken).ConfigureAwait(false);
            return (contentLength, buffer, 0);
        }

        // We read more than the body — overflow belongs to the next message
        int overflow = extraBytes - contentLength;
        return (contentLength, buffer, overflow);
    }

    private void HandleResponse(JsonElement message, JsonElement idProp)
    {
        if (!idProp.TryGetInt64(out long id))
        {
            return;
        }

        if (!_pendingRequests.TryRemove(id, out var pending))
        {
            return;
        }

        if (message.TryGetProperty("error", out var errorProp))
        {
            var errorMessage = errorProp.TryGetProperty("message", out var msgProp)
                ? msgProp.GetString() ?? "Unknown error"
                : "Unknown error";
            var errorCode = errorProp.TryGetProperty("code", out var codeProp) && codeProp.ValueKind == JsonValueKind.Number
                ? codeProp.GetInt32()
                : 0;
            pending.TrySetException(new RemoteRpcException(errorMessage, errorCode));
        }
        else if (message.TryGetProperty("result", out var resultProp))
        {
            pending.TrySetResult(resultProp.Clone());
        }
        else
        {
            // Per JSON-RPC 2.0, a response must have either "result" or "error".
            // Treat missing result as null result.
            pending.TrySetResult(default);
        }
    }

    private async Task HandleIncomingMethodAsync(string methodName, JsonElement message, CancellationToken cancellationToken)
    {
        try
        {
            JsonElement? requestId = null;
            if (message.TryGetProperty("id", out var idProp))
            {
                requestId = idProp;
            }

            if (!_methods.TryGetValue(methodName, out var registration))
            {
                if (requestId.HasValue)
                {
                    await SendErrorResponseAsync(requestId.Value, ErrorCodeMethodNotFound, $"Method not found: {methodName}", cancellationToken).ConfigureAwait(false);
                }
                return;
            }

            message.TryGetProperty("params", out var paramsProp);

            try
            {
                var result = await InvokeHandlerAsync(registration, paramsProp, cancellationToken).ConfigureAwait(false);

                if (requestId.HasValue)
                {
                    await SendResultResponseAsync(requestId.Value, result, cancellationToken).ConfigureAwait(false);
                }
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                if (_logger.IsEnabled(LogLevel.Debug))
                {
                    _logger.LogDebug("Error handling JSON-RPC method {Method}: {Error}", methodName, ex.Message);
                }
                if (requestId.HasValue)
                {
                    await SendErrorResponseAsync(requestId.Value, ErrorCodeInternalError, ex.Message, cancellationToken).ConfigureAwait(false);
                }
            }
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
        {
            // Normal shutdown — cancellation propagated from the read loop.
        }
        catch (Exception ex)
        {
            // Belt-and-braces: this method is fire-and-forget from the read loop, so any
            // exception escaping here would become an unobserved task exception. The most
            // likely sources are IOException/ObjectDisposedException from sending the error
            // response after the underlying transport is gone.
            if (_logger.IsEnabled(LogLevel.Debug))
            {
                _logger.LogDebug(ex, "Unobserved error in JSON-RPC method dispatch for {Method}", methodName);
            }
        }
    }

    private async ValueTask<object?> InvokeHandlerAsync(MethodRegistration registration, JsonElement paramsProp, CancellationToken cancellationToken)
    {
        var parameters = registration.Parameters;

        // Build argument list
        var invokeArgs = new object?[parameters.Length];

        if (registration.SingleObjectParam)
        {
            // Single-object deserialization: entire `params` → first parameter.
            // Every singleObjectParam handler has shape (TRequest, CancellationToken),
            // so `params` must be a JSON object.
            if (paramsProp.ValueKind != JsonValueKind.Object)
            {
                throw new InvalidOperationException(
                    $"Expected JSON object for `params` of single-object-param handler; got '{paramsProp.ValueKind}'.");
            }

            for (int i = 0; i < parameters.Length; i++)
            {
                if (parameters[i].ParameterType == typeof(CancellationToken))
                {
                    invokeArgs[i] = cancellationToken;
                }
                else if (i == 0)
                {
                    invokeArgs[i] = paramsProp.Deserialize(_serializerOptions.GetTypeInfo(parameters[i].ParameterType));
                }
            }
        }
        else if (paramsProp.ValueKind == JsonValueKind.Array)
        {
            // Positional parameters. Optional params (with defaults) are filled when absent.
            int jsonIndex = 0;
            int arrayLength = paramsProp.GetArrayLength();
            for (int i = 0; i < parameters.Length; i++)
            {
                if (parameters[i].ParameterType == typeof(CancellationToken))
                {
                    invokeArgs[i] = cancellationToken;
                }
                else if (jsonIndex < arrayLength)
                {
                    invokeArgs[i] = paramsProp[jsonIndex].Deserialize(_serializerOptions.GetTypeInfo(parameters[i].ParameterType));
                    jsonIndex++;
                }
                else
                {
                    invokeArgs[i] = parameters[i].HasDefaultValue ? parameters[i].DefaultValue : null;
                }
            }
        }
        else if (paramsProp.ValueKind == JsonValueKind.Object)
        {
            // Named parameters. The CLI sends notifications/requests as a JSON object whose
            // property names match the handler's parameter names (camelCased per web defaults).
            // Look up each parameter by name; missing optional parameters fall back to defaults.
            for (int i = 0; i < parameters.Length; i++)
            {
                if (parameters[i].ParameterType == typeof(CancellationToken))
                {
                    invokeArgs[i] = cancellationToken;
                }
                else if (parameters[i].Name is { } paramName &&
                         TryGetPropertyCaseInsensitive(paramsProp, paramName, out var valueProp))
                {
                    invokeArgs[i] = valueProp.Deserialize(_serializerOptions.GetTypeInfo(parameters[i].ParameterType));
                }
                else
                {
                    invokeArgs[i] = parameters[i].HasDefaultValue ? parameters[i].DefaultValue : null;
                }
            }
        }
        else
        {
            // Missing/null `params` for a handler with required positional parameters is a
            // protocol violation. Surface it as an error rather than silently filling defaults.
            throw new InvalidOperationException(
                $"Unsupported JSON-RPC params shape '{paramsProp.ValueKind}' for handler with positional parameters.");
        }

        // Invoke
        var result = registration.Handler.DynamicInvoke(invokeArgs);

        // Handlers return one of: a synchronous value, Task (void async), or ValueTask<T>.
        if (result is Task task)
        {
            // Task<T> handlers are not supported — use ValueTask<T> for results.
            Debug.Assert(!task.GetType().IsGenericType, "Task<T> handlers are not supported; use ValueTask<T>.");
            await task.ConfigureAwait(false);
            return null;
        }

        if (result is not null && registration.ReturnsValueTaskOfT)
        {
            var resultType = result.GetType();
            var asTask = (Task)resultType.GetMethod("AsTask")!.Invoke(result, null)!;
            await asTask.ConfigureAwait(false);
            return asTask.GetType().GetProperty("Result")!.GetValue(asTask);
        }

        return result;
    }

    private static bool TryGetPropertyCaseInsensitive(JsonElement obj, string name, out JsonElement value)
    {
        // Fast path: exact match. The CLI uses camelCase property names that match the
        // C# parameter names exactly, so this should hit in the common case.
        if (obj.TryGetProperty(name, out value))
        {
            return true;
        }

        foreach (var prop in obj.EnumerateObject())
        {
            if (string.Equals(prop.Name, name, StringComparison.OrdinalIgnoreCase))
            {
                value = prop.Value;
                return true;
            }
        }

        value = default;
        return false;
    }

    private JsonElement? SerializeArgs(object?[]? args)
    {
        if (args is null || args.Length == 0)
        {
            return null;
        }

        // The Copilot CLI uses vscode-jsonrpc-style request handlers, which expect
        // `params` to be the single request object (not wrapped in a positional array).
        // The other SDKs (Node, Python, Go) all send single-object params, and every
        // generated call site here passes exactly one request object. For the rare
        // multi-arg case, fall back to a positional array.
        if (args.Length == 1)
        {
            var arg = args[0];
            if (arg is null)
            {
                return null;
            }

            var typeInfo = _serializerOptions.GetTypeInfo(arg.GetType());
            return JsonSerializer.SerializeToElement(arg, typeInfo);
        }

        // Source-generated JsonSerializerOptions do not provide metadata for object[],
        // so build the JSON array manually, serializing each element with a TypeInfo
        // looked up by its runtime type from the merged resolver.
        var buffer = new ArrayBufferWriter<byte>();
        using (var writer = new Utf8JsonWriter(buffer))
        {
            writer.WriteStartArray();
            foreach (var arg in args)
            {
                if (arg is null)
                {
                    writer.WriteNullValue();
                }
                else
                {
                    var typeInfo = _serializerOptions.GetTypeInfo(arg.GetType());
                    JsonSerializer.Serialize(writer, arg, typeInfo);
                }
            }

            writer.WriteEndArray();
        }

        using var doc = JsonDocument.Parse(buffer.WrittenMemory);
        return doc.RootElement.Clone();
    }

    private async Task SendResultResponseAsync(JsonElement id, object? result, CancellationToken cancellationToken)
    {
        try
        {
            // Convert the result to a JsonElement using the runtime type, looked up via
            // the merged resolver. Source-gen serialization of an `object`-typed property
            // would otherwise have no way to find metadata for the actual response type
            // (e.g. SystemMessageTransformRpcResponse, SessionFsReadFileResult, ...).
            JsonElement? resultElement = null;
            if (result is not null)
            {
                var typeInfo = _serializerOptions.GetTypeInfo(result.GetType());
                resultElement = JsonSerializer.SerializeToElement(result, typeInfo);
            }

            await SendMessageAsync(new JsonRpcResponse
            {
                Id = id,
                Result = resultElement,
            }, JsonRpcWireContext.Default.JsonRpcResponse, cancellationToken).ConfigureAwait(false);
        }
        catch (Exception ex) when (ex is IOException or ObjectDisposedException or OperationCanceledException)
        {
            // Connection lost during response — nothing we can do
        }
    }

    private async Task SendErrorResponseAsync(JsonElement id, int code, string message, CancellationToken cancellationToken)
    {
        try
        {
            await SendMessageAsync(new JsonRpcErrorResponse
            {
                Id = id,
                Error = new JsonRpcError { Code = code, Message = message },
            }, JsonRpcWireContext.Default.JsonRpcErrorResponse, cancellationToken).ConfigureAwait(false);
        }
        catch (Exception ex) when (ex is IOException or ObjectDisposedException or OperationCanceledException)
        {
            // Connection lost during error response — nothing we can do
        }
    }

    private async Task SendCancelNotificationAsync(long requestId)
    {
        try
        {
            await SendMessageAsync(new JsonRpcNotification
            {
                Method = "$/cancelRequest",
                Params = JsonSerializer.SerializeToElement(
                    new CancelRequestParams { Id = requestId },
                    CancelRequestParamsContext.Default.CancelRequestParams),
            }, JsonRpcWireContext.Default.JsonRpcNotification, CancellationToken.None).ConfigureAwait(false);
        }
        catch (Exception ex) when (ex is IOException or ObjectDisposedException or OperationCanceledException)
        {
            // Best effort — connection may already be gone
        }
    }

    private sealed class PendingRequest() : TaskCompletionSource<JsonElement>(TaskCreationOptions.RunContinuationsAsynchronously);

    private sealed class MethodRegistration
    {
        public MethodRegistration(Delegate handler, bool singleObjectParam)
        {
            Handler = handler;
            SingleObjectParam = singleObjectParam;
            Parameters = handler.Method.GetParameters();
            ReturnsValueTaskOfT =
                handler.Method.ReturnType.IsGenericType &&
                handler.Method.ReturnType.GetGenericTypeDefinition() == typeof(ValueTask<>);
        }

        public Delegate Handler { get; }
        public bool SingleObjectParam { get; }
        public ParameterInfo[] Parameters { get; }
        public bool ReturnsValueTaskOfT { get; }
    }

    [JsonSourceGenerationOptions(
        JsonSerializerDefaults.Web,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
    [JsonSerializable(typeof(JsonRpcRequest))]
    [JsonSerializable(typeof(JsonRpcResponse))]
    [JsonSerializable(typeof(JsonRpcErrorResponse))]
    [JsonSerializable(typeof(JsonRpcNotification))]
    private partial class JsonRpcWireContext : JsonSerializerContext;

    private sealed class JsonRpcRequest
    {
        [JsonPropertyName("jsonrpc")]
        public string Jsonrpc { get; } = "2.0";

        [JsonPropertyName("id")]
        public long Id { get; set; }

        [JsonPropertyName("method")]
        public string Method { get; set; } = string.Empty;

        [JsonPropertyName("params")]
        public JsonElement? Params { get; set; }
    }

    private sealed class JsonRpcResponse
    {
        [JsonPropertyName("jsonrpc")]
        public string Jsonrpc { get; } = "2.0";

        [JsonPropertyName("id")]
        public JsonElement Id { get; set; }

        [JsonPropertyName("result")]
        public JsonElement? Result { get; set; }
    }

    private sealed class JsonRpcErrorResponse
    {
        [JsonPropertyName("jsonrpc")]
        public string Jsonrpc { get; } = "2.0";

        [JsonPropertyName("id")]
        public JsonElement Id { get; set; }

        [JsonPropertyName("error")]
        public JsonRpcError? Error { get; set; }
    }

    private sealed class JsonRpcError
    {
        [JsonPropertyName("code")]
        public int Code { get; set; }

        [JsonPropertyName("message")]
        public string Message { get; set; } = string.Empty;
    }

    private sealed class JsonRpcNotification
    {
        [JsonPropertyName("jsonrpc")]
        public string Jsonrpc { get; } = "2.0";

        [JsonPropertyName("method")]
        public string Method { get; set; } = string.Empty;

        [JsonPropertyName("params")]
        public JsonElement? Params { get; set; }
    }

    private sealed class CancelRequestParams
    {
        [JsonPropertyName("id")]
        public long Id { get; set; }
    }

    [JsonSerializable(typeof(CancelRequestParams))]
    private partial class CancelRequestParamsContext : JsonSerializerContext;
}

/// <summary>
/// Thrown when the JSON-RPC connection is lost unexpectedly.
/// </summary>
internal sealed class ConnectionLostException() : IOException("The JSON-RPC connection was lost.");

/// <summary>
/// Thrown when the remote side returns a JSON-RPC error response.
/// </summary>
internal sealed class RemoteRpcException(string message, int errorCode, Exception? innerException = null) : Exception(message, innerException)
{
    public int ErrorCode { get; } = errorCode;
}
