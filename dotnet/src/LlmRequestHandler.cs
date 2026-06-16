/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics.CodeAnalysis;
using System.Net.WebSockets;
using System.Text;

namespace GitHub.Copilot;

/// <summary>
/// Per-request context handed to every <see cref="LlmRequestHandler"/> hook.
/// Mirrors the subset of <see cref="LlmInferenceRequest"/> fields that are
/// stable across the request lifetime, letting overrides observe routing /
/// cancellation without re-plumbing the underlying request.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public sealed class LlmRequestContext
{
    /// <summary>Opaque runtime-minted id, stable across the request lifecycle.</summary>
    public required string RequestId { get; init; }

    /// <summary>Runtime session id that triggered the request, if any.</summary>
    public string? SessionId { get; init; }

    /// <summary>Transport the runtime would otherwise use.</summary>
    public LlmInferenceTransport Transport { get; init; }

    /// <summary>Original request URL.</summary>
    public required string Url { get; init; }

    /// <summary>Original request headers.</summary>
    public required IReadOnlyDictionary<string, IReadOnlyList<string>> Headers { get; init; }

    /// <summary>
    /// Cancelled when the runtime aborts this in-flight request. Subclasses that
    /// issue their own I/O should pass this through so the upstream call is torn
    /// down too.
    /// </summary>
    public CancellationToken CancellationToken { get; init; }

    internal LlmWebSocketResponseBridge? WebSocketResponse { get; set; }
}

/// <summary>A single WebSocket message exchanged through a <see cref="LlmRequestHandler"/> hook.</summary>
[Experimental(Diagnostics.Experimental)]
public readonly struct LlmWebSocketMessage(ReadOnlyMemory<byte> data, bool isBinary)
{
    /// <summary>The message payload bytes.</summary>
    public ReadOnlyMemory<byte> Data { get; } = data;

    /// <summary>True for a binary frame; false for a UTF-8 text frame.</summary>
    public bool IsBinary { get; } = isBinary;

    /// <summary>Decodes the payload as UTF-8 text.</summary>
    public string GetText() => Encoding.UTF8.GetString(Data.ToArray());

    /// <summary>Creates a text message from a UTF-8 string.</summary>
    public static LlmWebSocketMessage Text(string text) => new(Encoding.UTF8.GetBytes(text), isBinary: false);

    /// <summary>Creates a binary message from raw bytes.</summary>
    public static LlmWebSocketMessage Binary(ReadOnlyMemory<byte> data) => new(data, isBinary: true);
}

/// <summary>
/// Terminal status for a callback-owned WebSocket connection.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public sealed class LlmWebSocketCloseStatus
{
    /// <summary>The close description, if any.</summary>
    public string? Description { get; init; }

    /// <summary>
    /// Optional error code surfaced to the runtime when the close is a failure
    /// rather than a clean end-of-stream.
    /// </summary>
    public string? ErrorCode { get; init; }

    /// <summary>The error that terminated the connection, if any.</summary>
    public Exception? Error { get; init; }

    /// <summary>Shared normal-closure instance.</summary>
    public static LlmWebSocketCloseStatus NormalClosure { get; } = new();
}

/// <summary>
/// Per-connection WebSocket handler returned by
/// <see cref="LlmRequestHandler.OpenWebSocketAsync"/>.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public abstract class CopilotWebSocketHandler : IAsyncDisposable
{
    private readonly TaskCompletionSource<LlmWebSocketCloseStatus> _completion =
        new(TaskCreationOptions.RunContinuationsAsynchronously);
    private int _closed;
    private bool _suppressCloseOnDispose;

    /// <summary>Request context for this WebSocket connection.</summary>
    protected LlmRequestContext Context { get; }

    internal Task<LlmWebSocketCloseStatus> Completion => _completion.Task;

    /// <summary>
    /// Initializes a per-connection handler for the supplied request context.
    /// </summary>
    protected CopilotWebSocketHandler(LlmRequestContext context)
    {
        Context = context;
        _ = context.WebSocketResponse ?? throw new InvalidOperationException("WebSocket response bridge is not attached.");
    }

    /// <summary>
    /// Send a message from the runtime to the upstream connection.
    /// </summary>
    public abstract Task SendRequestMessageAsync(LlmWebSocketMessage message);

    /// <summary>
    /// Send a message from the upstream connection back to the runtime.
    /// Override to mutate or duplicate messages; call <c>base</c> to emit.
    /// </summary>
    public virtual Task SendResponseMessageAsync(LlmWebSocketMessage message) =>
        Context.WebSocketResponse!.WriteAsync(message);

    /// <summary>
    /// Close the connection and finalise the runtime-facing response.
    /// </summary>
    public virtual async Task CloseAsync(LlmWebSocketCloseStatus status)
    {
        if (Interlocked.Exchange(ref _closed, 1) != 0)
        {
            return;
        }

        if (status.Error is not null)
        {
            await Context.WebSocketResponse!
                .ErrorAsync(status.Description ?? status.Error.Message, status.ErrorCode)
                .ConfigureAwait(false);
        }
        else
        {
            await Context.WebSocketResponse!.EndAsync().ConfigureAwait(false);
        }

        _completion.TrySetResult(status);
    }

    internal void SuppressCloseOnDispose() => _suppressCloseOnDispose = true;

    internal virtual Task OpenAsync() => Task.CompletedTask;

    /// <inheritdoc />
    public virtual async ValueTask DisposeAsync()
    {
        GC.SuppressFinalize(this);
        if (!_suppressCloseOnDispose && Volatile.Read(ref _closed) == 0)
        {
            await CloseAsync(LlmWebSocketCloseStatus.NormalClosure).ConfigureAwait(false);
        }
    }
}

/// <summary>
/// Default pass-through WebSocket handler. Opens the real upstream socket and
/// relays messages unchanged unless a subclass overrides the send methods.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public class ForwardingWebSocketHandler : CopilotWebSocketHandler
{
    private readonly string _url;
    private readonly IReadOnlyDictionary<string, IReadOnlyList<string>> _headers;
    private WebSocket? _upstream;
    private CancellationTokenSource? _pumpCts;
    private Task? _responsePump;

    /// <summary>
    /// Initializes a forwarding handler that will open the upstream socket on
    /// demand using the supplied URL/headers (or the values from
    /// <paramref name="context"/> when omitted).
    /// </summary>
    public ForwardingWebSocketHandler(
        LlmRequestContext context,
        string? url = null,
        IReadOnlyDictionary<string, IReadOnlyList<string>>? headers = null)
        : base(context)
    {
        _url = url ?? context.Url;
        _headers = headers ?? context.Headers;
    }

    /// <summary>
    /// Opens the upstream socket and starts the built-in response pump.
    /// </summary>
    internal override async Task OpenAsync()
    {
        if (_upstream is not null)
        {
            return;
        }

        var socket = new ClientWebSocket();
        foreach (var (name, values) in _headers)
        {
            if (s_forbiddenRequestHeaders.Contains(name))
            {
                continue;
            }

            try
            {
                socket.Options.SetRequestHeader(name, string.Join(", ", values));
            }
            catch
            {
                // Some headers are managed by the handshake; ignore rejections.
            }
        }

        await socket.ConnectAsync(LlmWebSocketHelpers.ToWebSocketUri(_url), Context.CancellationToken).ConfigureAwait(false);
        _upstream = socket;
        _pumpCts = CancellationTokenSource.CreateLinkedTokenSource(Context.CancellationToken);
        _responsePump = Task.Run(() => PumpResponsesAsync(_pumpCts.Token), _pumpCts.Token);
    }

    /// <summary>
    /// Sends a message from the runtime to the upstream connection. Subclasses may override to mutate messages.
    /// </summary>
    /// <param name="message">The message to send.</param>
    /// <returns>A <see cref="Task"/> representing the asynchronous operation.</returns>
    public override Task SendRequestMessageAsync(LlmWebSocketMessage message)
    {
        if (_upstream?.State != WebSocketState.Open)
        {
            return Task.CompletedTask;
        }

        var type = message.IsBinary ? WebSocketMessageType.Binary : WebSocketMessageType.Text;
        return _upstream.SendAsync(
            new ArraySegment<byte>(message.Data.ToArray()),
            type,
            endOfMessage: true,
            Context.CancellationToken);
    }

    /// <inheritdoc />
    public override async Task CloseAsync(LlmWebSocketCloseStatus status)
    {
        _pumpCts?.Cancel();
        if (_upstream is not null)
        {
            await LlmWebSocketHelpers.CloseWebSocketQuietlyAsync(_upstream).ConfigureAwait(false);
        }
        await base.CloseAsync(status).ConfigureAwait(false);
    }

    /// <inheritdoc />
    public override async ValueTask DisposeAsync()
    {
        GC.SuppressFinalize(this);
        try
        {
            await base.DisposeAsync().ConfigureAwait(false);
        }
        finally
        {
            _pumpCts?.Cancel();
            _pumpCts?.Dispose();
            _upstream?.Dispose();
            if (_responsePump is not null)
            {
                await LlmWebSocketHelpers.ObserveQuietlyAsync(_responsePump).ConfigureAwait(false);
            }
        }
    }

    private async Task PumpResponsesAsync(CancellationToken cancellationToken)
    {
        if (_upstream is null)
        {
            return;
        }

        try
        {
            while (_upstream.State == WebSocketState.Open)
            {
                var message = await LlmWebSocketHelpers.ReceiveMessageAsync(_upstream, cancellationToken).ConfigureAwait(false);
                if (message is null)
                {
                    break;
                }

                await SendResponseMessageAsync(message.Value).ConfigureAwait(false);
            }

            await CloseAsync(LlmWebSocketCloseStatus.NormalClosure).ConfigureAwait(false);
        }
        catch (OperationCanceledException) when (Context.CancellationToken.IsCancellationRequested)
        {
            // Runtime-side cancellation aborts the request pump; the outer
            // handler rethrows that cancellation rather than finalising here.
        }
        catch (Exception ex)
        {
            await CloseAsync(new LlmWebSocketCloseStatus
            {
                Description = ex.Message,
                Error = ex,
            }).ConfigureAwait(false);
        }
    }

    // Computed/managed by the HTTP/WS stack; forwarding them verbatim either
    // throws or corrupts the request.
    private static readonly HashSet<string> s_forbiddenRequestHeaders = new(StringComparer.OrdinalIgnoreCase)
    {
        "host",
        "connection",
        "content-length",
        "transfer-encoding",
        "keep-alive",
        "upgrade",
        "proxy-connection",
        "te",
        "trailer",
    };
}

/// <summary>
/// Base class for SDK consumers who want to observe or mutate the LLM inference
/// requests the runtime issues.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public class LlmRequestHandler : ILlmInferenceProvider
{
    private static readonly HttpClient s_sharedHttpClient = new();

    // Computed/managed by the HTTP stack; forwarding them verbatim either throws
    // or corrupts the request.
    private static readonly HashSet<string> s_forbiddenRequestHeaders = new(StringComparer.OrdinalIgnoreCase)
    {
        "host",
        "connection",
        "content-length",
        "transfer-encoding",
        "keep-alive",
        "upgrade",
        "proxy-connection",
        "te",
        "trailer",
    };

    /// <inheritdoc />
    async Task ILlmInferenceProvider.OnLlmRequestAsync(LlmInferenceRequest request)
    {
        ArgumentNullException.ThrowIfNull(request);

        var wsResponse = new LlmWebSocketResponseBridge(request.ResponseBody);
        var ctx = new LlmRequestContext
        {
            RequestId = request.RequestId,
            SessionId = request.SessionId,
            Transport = request.Transport,
            Url = request.Url,
            Headers = request.Headers,
            CancellationToken = request.CancellationToken,
        };
        ctx.WebSocketResponse = wsResponse;

        if (request.Transport == LlmInferenceTransport.WebSocket)
        {
            await HandleWebSocketAsync(request, ctx).ConfigureAwait(false);
        }
        else
        {
            await HandleHttpAsync(request, ctx).ConfigureAwait(false);
        }
    }

    /// <summary>
    /// Issue the upstream HTTP request. Override to mutate the request before
    /// calling <c>base</c>, mutate the returned response after, or replace the
    /// call entirely.
    /// </summary>
    protected virtual Task<HttpResponseMessage> SendRequestAsync(HttpRequestMessage request, LlmRequestContext ctx) =>
        s_sharedHttpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, ctx.CancellationToken);

    /// <summary>
    /// Open the upstream WebSocket connection. Override to return a custom
    /// <see cref="CopilotWebSocketHandler"/> or to construct a
    /// <see cref="ForwardingWebSocketHandler"/> against a rewritten URL.
    /// </summary>
    protected virtual Task<CopilotWebSocketHandler> OpenWebSocketAsync(LlmRequestContext ctx) =>
        Task.FromResult<CopilotWebSocketHandler>(new ForwardingWebSocketHandler(ctx));

    private async Task HandleHttpAsync(LlmInferenceRequest req, LlmRequestContext ctx)
    {
        using var request = await BuildHttpRequestAsync(req).ConfigureAwait(false);
        using var response = await SendRequestAsync(request, ctx).ConfigureAwait(false);
        await StreamResponseToSinkAsync(response, req, ctx).ConfigureAwait(false);
    }

    private static async Task<HttpRequestMessage> BuildHttpRequestAsync(LlmInferenceRequest req)
    {
        var method = new HttpMethod(req.Method.ToUpperInvariant());
        var message = new HttpRequestMessage(method, req.Url);

        var hasBody = method != HttpMethod.Get && method != HttpMethod.Head;
        var body = await DrainAsync(req.RequestBody).ConfigureAwait(false);
        if (hasBody && body.Length > 0)
        {
            message.Content = new ByteArrayContent(body);
        }

        foreach (var (name, values) in req.Headers)
        {
            if (s_forbiddenRequestHeaders.Contains(name))
            {
                continue;
            }

            if (!message.Headers.TryAddWithoutValidation(name, values))
            {
                message.Content ??= new ByteArrayContent([]);
                message.Content.Headers.TryAddWithoutValidation(name, values);
            }
        }

        return message;
    }

    private static async Task StreamResponseToSinkAsync(HttpResponseMessage response, LlmInferenceRequest req, LlmRequestContext ctx)
    {
        await req.ResponseBody.StartAsync(new LlmInferenceResponseInit
        {
            Status = (int)response.StatusCode,
            StatusText = response.ReasonPhrase,
            Headers = HeadersToMultiMap(response),
        }).ConfigureAwait(false);

#if NETSTANDARD2_0
        using var stream = await response.Content.ReadAsStreamAsync().ConfigureAwait(false);
#else
        using var stream = await response.Content.ReadAsStreamAsync(ctx.CancellationToken).ConfigureAwait(false);
#endif
        var buffer = new byte[16 * 1024];
        int read;
#if NETSTANDARD2_0
        while ((read = await stream.ReadAsync(buffer, 0, buffer.Length, ctx.CancellationToken).ConfigureAwait(false)) > 0)
        {
            await req.ResponseBody.WriteAsync(new ReadOnlyMemory<byte>(buffer, 0, read)).ConfigureAwait(false);
        }
#else
        while ((read = await stream.ReadAsync(buffer.AsMemory(), ctx.CancellationToken).ConfigureAwait(false)) > 0)
        {
            await req.ResponseBody.WriteAsync(new ReadOnlyMemory<byte>(buffer, 0, read)).ConfigureAwait(false);
        }
#endif

        await req.ResponseBody.EndAsync().ConfigureAwait(false);
    }

    private async Task HandleWebSocketAsync(LlmInferenceRequest req, LlmRequestContext ctx)
    {
        var handler = await OpenWebSocketAsync(ctx).ConfigureAwait(false);
        try
        {
            await handler.OpenAsync().ConfigureAwait(false);
            await ctx.WebSocketResponse!.StartAsync().ConfigureAwait(false);

            var clientPump = Task.Run(async () =>
            {
                await foreach (var chunk in req.RequestBody.WithCancellation(ctx.CancellationToken).ConfigureAwait(false))
                {
                    await handler.SendRequestMessageAsync(new LlmWebSocketMessage(chunk, isBinary: false)).ConfigureAwait(false);
                }
            }, ctx.CancellationToken);

            var first = await Task.WhenAny(clientPump, handler.Completion).ConfigureAwait(false);
            if (first == clientPump)
            {
                if (clientPump.IsFaulted || clientPump.IsCanceled)
                {
                    handler.SuppressCloseOnDispose();
                    await clientPump.ConfigureAwait(false);
                }

                await handler.CloseAsync(LlmWebSocketCloseStatus.NormalClosure).ConfigureAwait(false);
                await handler.Completion.ConfigureAwait(false);
                return;
            }

            var closeStatus = await handler.Completion.ConfigureAwait(false);
            if (closeStatus.Error is not null)
            {
                throw closeStatus.Error;
            }
        }
        finally
        {
            await handler.DisposeAsync().ConfigureAwait(false);
        }
    }

    private static async Task<byte[]> DrainAsync(IAsyncEnumerable<ReadOnlyMemory<byte>> stream)
    {
        using var buffer = new MemoryStream();
        await foreach (var chunk in stream.ConfigureAwait(false))
        {
            if (chunk.Length > 0)
            {
                buffer.Write(chunk.ToArray(), 0, chunk.Length);
            }
        }

        return buffer.ToArray();
    }

    private static Dictionary<string, IReadOnlyList<string>> HeadersToMultiMap(HttpResponseMessage response)
    {
        var result = new Dictionary<string, IReadOnlyList<string>>(StringComparer.OrdinalIgnoreCase);
        foreach (var header in response.Headers)
        {
            result[header.Key] = [.. header.Value];
        }

        if (response.Content is not null)
        {
            foreach (var header in response.Content.Headers)
            {
                result[header.Key] = [.. header.Value];
            }
        }

        return result;
    }

}

internal static class LlmWebSocketHelpers
{
    internal static async Task<LlmWebSocketMessage?> ReceiveMessageAsync(WebSocket socket, CancellationToken cancellationToken)
    {
        var buffer = new byte[16 * 1024];
        using var assembled = new MemoryStream();
        WebSocketReceiveResult result;
        do
        {
            try
            {
                result = await socket.ReceiveAsync(new ArraySegment<byte>(buffer), cancellationToken).ConfigureAwait(false);
            }
            catch (OperationCanceledException)
            {
                return null;
            }
            catch (WebSocketException)
            {
                return null;
            }

            if (result.MessageType == WebSocketMessageType.Close)
            {
                return null;
            }

            assembled.Write(buffer, 0, result.Count);
        }
        while (!result.EndOfMessage);

        return new LlmWebSocketMessage(assembled.ToArray(), result.MessageType == WebSocketMessageType.Binary);
    }

    internal static async Task CloseWebSocketQuietlyAsync(WebSocket socket)
    {
        try
        {
            if (socket.State is WebSocketState.Open or WebSocketState.CloseReceived)
            {
                await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, statusDescription: null, CancellationToken.None).ConfigureAwait(false);
            }
        }
        catch
        {
            // Best-effort; the socket may already be closed.
        }
    }

    [SuppressMessage("Usage", "CA1031:Do not catch general exception types", Justification = "Best-effort teardown of the losing pump.")]
    internal static async Task ObserveQuietlyAsync(Task task)
    {
        try
        {
            await task.ConfigureAwait(false);
        }
        catch
        {
            // Best-effort teardown only.
        }
    }

    internal static Uri ToWebSocketUri(string url)
    {
        var builder = new UriBuilder(url);
        if (builder.Scheme.Equals("https", StringComparison.OrdinalIgnoreCase))
        {
            builder.Scheme = "wss";
        }
        else if (builder.Scheme.Equals("http", StringComparison.OrdinalIgnoreCase))
        {
            builder.Scheme = "ws";
        }

        return builder.Uri;
    }
}

internal sealed class LlmWebSocketResponseBridge
{
    private readonly LlmInferenceResponseSink _sink;
    private readonly SemaphoreSlim _gate = new(1, 1);
    private readonly Queue<PendingAction> _pending = new();
    private bool _started;
    private bool _completed;

    internal LlmWebSocketResponseBridge(LlmInferenceResponseSink sink)
    {
        _sink = sink;
    }

    internal async Task StartAsync()
    {
        await _gate.WaitAsync().ConfigureAwait(false);
        try
        {
            if (_started)
            {
                return;
            }

            _started = true;
            await _sink.StartAsync(new LlmInferenceResponseInit { Status = 101 }).ConfigureAwait(false);
            while (_pending.Count > 0)
            {
                await ApplyAsync(_pending.Dequeue()).ConfigureAwait(false);
            }
        }
        finally
        {
            _gate.Release();
        }
    }

    internal Task WriteAsync(LlmWebSocketMessage message) => EnqueueOrApplyAsync(PendingAction.Write(message));

    internal Task EndAsync() => EnqueueOrApplyAsync(PendingAction.End());

    internal Task ErrorAsync(string message, string? code) => EnqueueOrApplyAsync(PendingAction.Error(message, code));

    private async Task EnqueueOrApplyAsync(PendingAction action)
    {
        await _gate.WaitAsync().ConfigureAwait(false);
        try
        {
            if (_completed && action.Kind == PendingActionKind.Write)
            {
                return;
            }

            if (!_started)
            {
                _pending.Enqueue(action);
                if (action.Kind is PendingActionKind.End or PendingActionKind.Error)
                {
                    _completed = true;
                }

                return;
            }

            await ApplyAsync(action).ConfigureAwait(false);
        }
        finally
        {
            _gate.Release();
        }
    }

    private async Task ApplyAsync(PendingAction action)
    {
        if (_completed && action.Kind == PendingActionKind.Write)
        {
            return;
        }

        switch (action.Kind)
        {
            case PendingActionKind.Write:
                if (action.Message!.Value.IsBinary)
                {
                    await _sink.WriteAsync(action.Message.Value.Data).ConfigureAwait(false);
                }
                else
                {
                    await _sink.WriteAsync(action.Message.Value.GetText()).ConfigureAwait(false);
                }
                break;
            case PendingActionKind.End:
                if (_completed)
                {
                    return;
                }

                _completed = true;
                await _sink.EndAsync().ConfigureAwait(false);
                break;
            case PendingActionKind.Error:
                if (_completed)
                {
                    return;
                }

                _completed = true;
                await _sink.ErrorAsync(action.ErrorMessage!, action.ErrorCode).ConfigureAwait(false);
                break;
        }
    }

    private readonly record struct PendingAction(
        PendingActionKind Kind,
        LlmWebSocketMessage? Message = null,
        string? ErrorMessage = null,
        string? ErrorCode = null)
    {
        internal static PendingAction Write(LlmWebSocketMessage message) => new(PendingActionKind.Write, message);
        internal static PendingAction End() => new(PendingActionKind.End);
        internal static PendingAction Error(string message, string? code) => new(PendingActionKind.Error, null, message, code);
    }

    private enum PendingActionKind
    {
        Write,
        End,
        Error,
    }
}
