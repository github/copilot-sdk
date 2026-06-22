/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using System.Collections.Concurrent;
using System.Diagnostics.CodeAnalysis;
using System.Net.WebSockets;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading.Channels;

namespace GitHub.Copilot;

/// <summary>
/// Transport the runtime would otherwise use to issue an intercepted
/// model-layer request.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public enum CopilotRequestTransport
{
    /// <summary>
    /// Plain HTTP or a streamed SSE response. Each body chunk is an opaque
    /// byte range.
    /// </summary>
    Http,

    /// <summary>
    /// Full-duplex WebSocket channel. Each request-body chunk is one inbound
    /// WebSocket message and each response-body write is one outbound message.
    /// </summary>
    WebSocket,
}

/// <summary>
/// Per-request context handed to every <see cref="CopilotRequestHandler"/> hook.
/// Exposes the routing and cancellation details of a single intercepted request
/// so overrides can observe or rewrite it.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CopilotRequestContext
{
    /// <summary>Opaque runtime-minted id, stable across the request lifecycle.</summary>
    public required string RequestId { get; init; }

    /// <summary>Runtime session id that triggered the request, if any.</summary>
    public string? SessionId { get; init; }

    /// <summary>Transport the runtime would otherwise use.</summary>
    public CopilotRequestTransport Transport { get; init; }

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

/// <summary>A single WebSocket message exchanged through a <see cref="CopilotRequestHandler"/> hook.</summary>
[Experimental(Diagnostics.Experimental)]
public readonly struct CopilotWebSocketMessage(ReadOnlyMemory<byte> data, bool isBinary)
{
    /// <summary>The message payload bytes.</summary>
    public ReadOnlyMemory<byte> Data { get; } = data;

    /// <summary>True for a binary frame; false for a UTF-8 text frame.</summary>
    public bool IsBinary { get; } = isBinary;

    /// <summary>Decodes the payload as UTF-8 text.</summary>
    public string GetText() => Encoding.UTF8.GetString(Data.ToArray());

    /// <summary>Creates a text message from a UTF-8 string.</summary>
    public static CopilotWebSocketMessage Text(string text) => new(Encoding.UTF8.GetBytes(text), isBinary: false);

    /// <summary>Creates a binary message from raw bytes.</summary>
    public static CopilotWebSocketMessage Binary(ReadOnlyMemory<byte> data) => new(data, isBinary: true);
}

/// <summary>
/// Terminal status for a callback-owned WebSocket connection.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public sealed class CopilotWebSocketCloseStatus
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
    public static CopilotWebSocketCloseStatus NormalClosure { get; } = new();
}

/// <summary>
/// Lower-level WebSocket handler with no upstream connection. This is the
/// abstract base shared by all WebSocket handlers; it does not open or forward
/// to any upstream server on its own. Subclass it directly only to service a
/// fully synthetic connection yourself. For the common case of mutating and
/// forwarding traffic to the real upstream, subclass
/// <see cref="CopilotWebSocketHandler"/> instead, which connects upstream and
/// forwards by default.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public abstract class CopilotWebSocketHandlerBase : IAsyncDisposable
{
    private readonly TaskCompletionSource<CopilotWebSocketCloseStatus> _completion =
        new(TaskCreationOptions.RunContinuationsAsynchronously);
    private int _closed;
    private bool _suppressCloseOnDispose;

    /// <summary>Request context for this WebSocket connection.</summary>
    protected CopilotRequestContext Context { get; }

    internal Task<CopilotWebSocketCloseStatus> Completion => _completion.Task;

    /// <summary>
    /// Initializes a per-connection handler for the supplied request context.
    /// </summary>
    protected CopilotWebSocketHandlerBase(CopilotRequestContext context)
    {
        Context = context;
        _ = context.WebSocketResponse ?? throw new InvalidOperationException("WebSocket response bridge is not attached.");
    }

    /// <summary>
    /// Send a message from the runtime to the upstream connection.
    /// </summary>
    public abstract Task SendRequestMessageAsync(CopilotWebSocketMessage message);

    /// <summary>
    /// Send a message from the upstream connection back to the runtime.
    /// Override to mutate or duplicate messages; call <c>base</c> to emit.
    /// </summary>
    public virtual Task SendResponseMessageAsync(CopilotWebSocketMessage message) =>
        Context.WebSocketResponse!.WriteAsync(message);

    /// <summary>
    /// Close the connection and finalise the runtime-facing response.
    /// </summary>
    public virtual async Task CloseAsync(CopilotWebSocketCloseStatus status)
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
            await CloseAsync(CopilotWebSocketCloseStatus.NormalClosure).ConfigureAwait(false);
        }
    }
}

/// <summary>
/// WebSocket handler that connects to the real upstream and forwards traffic by
/// default. This is the type returned by the default
/// <see cref="CopilotRequestHandler.OpenWebSocketAsync"/>. Override nothing to
/// get full pass-through. To mutate traffic, subclass this type and override a
/// send method, then call the base implementation to keep forwarding upstream.
/// (Subclassing <see cref="CopilotWebSocketHandlerBase"/> instead would drop
/// forwarding entirely.)
/// </summary>
[Experimental(Diagnostics.Experimental)]
public class CopilotWebSocketHandler : CopilotWebSocketHandlerBase
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
    public CopilotWebSocketHandler(
        CopilotRequestContext context,
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
            if (LlmInferenceHeaders.Forbidden.Contains(name))
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

        await socket.ConnectAsync(ToWebSocketUri(_url), Context.CancellationToken).ConfigureAwait(false);
        _upstream = socket;
        _pumpCts = CancellationTokenSource.CreateLinkedTokenSource(Context.CancellationToken);
        _responsePump = Task.Run(() => PumpResponsesAsync(_pumpCts.Token), _pumpCts.Token);
    }

    /// <summary>
    /// Sends a message from the runtime to the upstream connection. Subclasses may override to mutate messages.
    /// </summary>
    /// <param name="message">The message to send.</param>
    /// <returns>A <see cref="Task"/> representing the asynchronous operation.</returns>
    public override Task SendRequestMessageAsync(CopilotWebSocketMessage message)
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
    public override async Task CloseAsync(CopilotWebSocketCloseStatus status)
    {
        _pumpCts?.Cancel();
        if (_upstream is not null)
        {
            await CloseWebSocketQuietlyAsync(_upstream).ConfigureAwait(false);
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
                await ObserveQuietlyAsync(_responsePump).ConfigureAwait(false);
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
                var message = await ReceiveMessageAsync(_upstream, cancellationToken).ConfigureAwait(false);
                if (message is null)
                {
                    break;
                }

                await SendResponseMessageAsync(message.Value).ConfigureAwait(false);
            }

            await CloseAsync(CopilotWebSocketCloseStatus.NormalClosure).ConfigureAwait(false);
        }
        catch (OperationCanceledException) when (Context.CancellationToken.IsCancellationRequested)
        {
            // Runtime-side cancellation aborts the request pump; the outer
            // handler rethrows that cancellation rather than finalising here.
        }
        catch (Exception ex)
        {
            await CloseAsync(new CopilotWebSocketCloseStatus
            {
                Description = ex.Message,
                Error = ex,
            }).ConfigureAwait(false);
        }
    }

    private static async Task<CopilotWebSocketMessage?> ReceiveMessageAsync(WebSocket socket, CancellationToken cancellationToken)
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

        return new CopilotWebSocketMessage(assembled.ToArray(), result.MessageType == WebSocketMessageType.Binary);
    }

    private static async Task CloseWebSocketQuietlyAsync(WebSocket socket)
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
    private static async Task ObserveQuietlyAsync(Task task)
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

    private static Uri ToWebSocketUri(string url)
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

/// <summary>
/// Base class for SDK consumers who want to observe or mutate the LLM inference
/// requests the runtime issues (for both CAPI and BYOK providers). Subclass and
/// override <see cref="SendRequestAsync"/> or <see cref="OpenWebSocketAsync"/>.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public class CopilotRequestHandler
{
    private static readonly HttpClient s_sharedHttpClient = new();

    /// <summary>
    /// Issue the upstream HTTP request. Override to mutate the request before
    /// calling <c>base</c>, mutate the returned response after, or replace the
    /// call entirely.
    /// </summary>
    protected virtual Task<HttpResponseMessage> SendRequestAsync(HttpRequestMessage request, CopilotRequestContext ctx) =>
        s_sharedHttpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, ctx.CancellationToken);

    /// <summary>
    /// Open the upstream WebSocket connection. Override to return a custom
    /// <see cref="CopilotWebSocketHandlerBase"/> or to construct a
    /// <see cref="CopilotWebSocketHandler"/> against a rewritten URL.
    /// </summary>
    protected virtual Task<CopilotWebSocketHandlerBase> OpenWebSocketAsync(CopilotRequestContext ctx) =>
        Task.FromResult<CopilotWebSocketHandlerBase>(new CopilotWebSocketHandler(ctx));

    /// <summary>
    /// Entry point invoked by the adapter once per intercepted request. Routes to
    /// the HTTP or WebSocket flow and drives the consumer's overridable hooks.
    /// </summary>
    internal Task HandleAsync(LlmInferenceExchange exchange) =>
        exchange.Context.Transport == CopilotRequestTransport.WebSocket
            ? HandleWebSocketAsync(exchange)
            : HandleHttpAsync(exchange);

    private async Task HandleHttpAsync(LlmInferenceExchange exchange)
    {
        using var request = await BuildHttpRequestAsync(exchange).ConfigureAwait(false);
        using var response = await SendRequestAsync(request, exchange.Context).ConfigureAwait(false);
        await StreamResponseAsync(response, exchange).ConfigureAwait(false);
    }

    private static async Task<HttpRequestMessage> BuildHttpRequestAsync(LlmInferenceExchange exchange)
    {
        var method = new HttpMethod(exchange.Method.ToUpperInvariant());
        var message = new HttpRequestMessage(method, exchange.Context.Url);

        var hasBody = method != HttpMethod.Get && method != HttpMethod.Head;
        var body = await DrainAsync(exchange.RequestBody).ConfigureAwait(false);
        if (hasBody && body.Length > 0)
        {
            message.Content = new ByteArrayContent(body);
        }

        foreach (var (name, values) in exchange.Context.Headers)
        {
            if (LlmInferenceHeaders.Forbidden.Contains(name))
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

    private static async Task StreamResponseAsync(HttpResponseMessage response, LlmInferenceExchange exchange)
    {
        await exchange.StartResponseAsync(
            (int)response.StatusCode,
            response.ReasonPhrase,
            HeadersToMultiMap(response)).ConfigureAwait(false);

        var ct = exchange.Context.CancellationToken;
#if NETSTANDARD2_0
        using var stream = await response.Content.ReadAsStreamAsync().ConfigureAwait(false);
#else
        using var stream = await response.Content.ReadAsStreamAsync(ct).ConfigureAwait(false);
#endif
        var buffer = new byte[16 * 1024];
        int read;
#if NETSTANDARD2_0
        while ((read = await stream.ReadAsync(buffer, 0, buffer.Length, ct).ConfigureAwait(false)) > 0)
#else
        while ((read = await stream.ReadAsync(buffer.AsMemory(), ct).ConfigureAwait(false)) > 0)
#endif
        {
            await exchange.WriteResponseAsync(new ReadOnlyMemory<byte>(buffer, 0, read)).ConfigureAwait(false);
        }

        await exchange.EndResponseAsync().ConfigureAwait(false);
    }

    private async Task HandleWebSocketAsync(LlmInferenceExchange exchange)
    {
        var ctx = exchange.Context;
        var bridge = new LlmWebSocketResponseBridge(exchange);
        ctx.WebSocketResponse = bridge;

        var handler = await OpenWebSocketAsync(ctx).ConfigureAwait(false);
        try
        {
            await handler.OpenAsync().ConfigureAwait(false);

            // The runtime blocks the WebSocket connect until it receives the
            // 101 response head (the upgrade acknowledgement) and only then
            // begins forwarding inbound messages as request-body chunks. Emit
            // it eagerly here — waiting for the first upstream message would
            // deadlock, since the upstream stays silent until it receives a
            // request message the runtime won't send before the upgrade
            // completes.
            await bridge.StartAsync().ConfigureAwait(false);

            var clientPump = Task.Run(async () =>
            {
                await foreach (var chunk in exchange.RequestBody.WithCancellation(ctx.CancellationToken).ConfigureAwait(false))
                {
                    await handler.SendRequestMessageAsync(new CopilotWebSocketMessage(chunk, isBinary: false)).ConfigureAwait(false);
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

                await handler.CloseAsync(CopilotWebSocketCloseStatus.NormalClosure).ConfigureAwait(false);
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

/// <summary>
/// One intercepted request in flight. Carries the request context plus the body
/// byte stream the runtime feeds in via <c>httpRequestChunk</c> frames, and
/// emits the consumer's response straight back to the runtime through the
/// generated <c>llmInference</c> server API. Replaces the former
/// provider/sink/response-channel indirection with a single object the adapter
/// owns and the handler writes to.
/// </summary>
internal sealed class LlmInferenceExchange
{
    private readonly Func<ServerRpc?> _getServerRpc;
    private readonly Channel<BodyItem> _body = Channel.CreateUnbounded<BodyItem>(
        new UnboundedChannelOptions { SingleReader = true, SingleWriter = true });

    private bool _started;
    private bool _finished;
    private bool _cancelled;

    internal LlmInferenceExchange(string requestId, Func<ServerRpc?> getServerRpc)
    {
        RequestId = requestId;
        _getServerRpc = getServerRpc;
    }

    internal string RequestId { get; }

    internal string Method { get; set; } = "GET";

    internal CopilotRequestContext Context { get; set; } = null!;

    internal CancellationTokenSource Abort { get; } = new();

    internal bool Started => _started;

    internal bool Finished => _finished;

    internal bool Cancelled => _cancelled;

    // --- Request body feed (driven by the adapter as chunk frames arrive) ---

    internal void PushChunk(byte[] data) => _body.Writer.TryWrite(new BodyItem { Chunk = data });

    internal void PushEnd() => _body.Writer.TryWrite(new BodyItem { End = true });

    internal void PushCancel(string? reason)
    {
        _cancelled = true;
        Abort.Cancel();
        _body.Writer.TryWrite(new BodyItem { Cancel = true, CancelReason = reason });
    }

    /// <summary>
    /// Request body bytes, yielded as they arrive. A cancel frame surfaces as an
    /// <see cref="OperationCanceledException"/> so the consumer's upstream call
    /// is torn down.
    /// </summary>
    internal IAsyncEnumerable<ReadOnlyMemory<byte>> RequestBody => ReadBodyAsync(Abort.Token);

    private async IAsyncEnumerable<ReadOnlyMemory<byte>> ReadBodyAsync(
        [EnumeratorCancellation] CancellationToken cancellationToken = default)
    {
        while (await _body.Reader.WaitToReadAsync(cancellationToken).ConfigureAwait(false))
        {
            while (_body.Reader.TryRead(out var item))
            {
                if (item.Cancel)
                {
                    _body.Writer.TryComplete();
                    throw new OperationCanceledException(
                        item.CancelReason is null
                            ? "Request cancelled by runtime"
                            : $"Request cancelled by runtime: {item.CancelReason}");
                }

                if (item.End)
                {
                    _body.Writer.TryComplete();
                    yield break;
                }

                if (item.Chunk is { Length: > 0 })
                {
                    yield return item.Chunk;
                }
            }
        }
    }

    // --- Response emit (driven by the handler). Strict state machine: ---
    // StartResponseAsync once -> zero or more WriteResponseAsync -> exactly one
    // of EndResponseAsync / ErrorResponseAsync.

    internal async Task StartResponseAsync(int status, string? statusText, IReadOnlyDictionary<string, IReadOnlyList<string>>? headers)
    {
        if (_started)
        {
            throw new InvalidOperationException("LLM inference response StartAsync() called twice.");
        }

        if (_finished)
        {
            throw new InvalidOperationException("LLM inference response already finished.");
        }

        _started = true;
        await ServerRpc()
            .LlmInference.HttpResponseStartAsync(RequestId, status, ToWireHeaders(headers), statusText)
            .ConfigureAwait(false);
    }

    internal Task WriteResponseAsync(ReadOnlyMemory<byte> data) =>
        WriteChunkAsync(Convert.ToBase64String(data.ToArray()), binary: true);

    internal Task WriteResponseAsync(string text)
    {
        ArgumentNullException.ThrowIfNull(text);
        return WriteChunkAsync(text, binary: false);
    }

    internal async Task EndResponseAsync()
    {
        if (_finished)
        {
            return;
        }

        _finished = true;
        await ServerRpc().LlmInference.HttpResponseChunkAsync(RequestId, string.Empty, end: true).ConfigureAwait(false);
    }

    internal async Task ErrorResponseAsync(string message, string? code = null)
    {
        ArgumentNullException.ThrowIfNull(message);

        if (_finished)
        {
            return;
        }

        _finished = true;
        await ServerRpc()
            .LlmInference.HttpResponseChunkAsync(
                RequestId,
                string.Empty,
                end: true,
                error: new LlmInferenceHttpResponseChunkError { Message = message, Code = code })
            .ConfigureAwait(false);
    }

    private async Task WriteChunkAsync(string data, bool binary)
    {
        if (_cancelled)
        {
            throw new InvalidOperationException("LLM inference request was cancelled by the runtime.");
        }

        if (!_started)
        {
            throw new InvalidOperationException("LLM inference response WriteAsync() called before StartAsync().");
        }

        if (_finished)
        {
            throw new InvalidOperationException("LLM inference response WriteAsync() called after EndAsync()/ErrorAsync().");
        }

        await ServerRpc()
            .LlmInference.HttpResponseChunkAsync(RequestId, data, binary: binary, end: false)
            .ConfigureAwait(false);
    }

    private ServerRpc ServerRpc() =>
        _getServerRpc() ?? throw new InvalidOperationException("LLM inference response used after RPC connection closed.");

    private static Dictionary<string, IList<string>> ToWireHeaders(IReadOnlyDictionary<string, IReadOnlyList<string>>? headers)
    {
        var result = new Dictionary<string, IList<string>>(StringComparer.OrdinalIgnoreCase);
        if (headers is null)
        {
            return result;
        }

        foreach (var (name, values) in headers)
        {
            result[name] = values as IList<string> ?? [.. values];
        }

        return result;
    }

    private struct BodyItem
    {
        public byte[]? Chunk;
        public bool End;
        public bool Cancel;
        public string? CancelReason;
    }
}

/// <summary>
/// Adapts the generated <see cref="ILlmInferenceHandler"/> RPC entry points onto
/// a consumer's <see cref="CopilotRequestHandler"/>. Each <c>httpRequestStart</c>
/// allocates an <see cref="LlmInferenceExchange"/> and runs the handler in the
/// background; subsequent <c>httpRequestChunk</c> frames feed its body stream.
/// </summary>
internal sealed class LlmInferenceAdapter(CopilotRequestHandler handler, Func<ServerRpc?> getServerRpc) : ILlmInferenceHandler
{
    private readonly CopilotRequestHandler _handler = handler ?? throw new ArgumentNullException(nameof(handler));
    private readonly Func<ServerRpc?> _getServerRpc = getServerRpc ?? throw new ArgumentNullException(nameof(getServerRpc));
    private readonly ConcurrentDictionary<string, LlmInferenceExchange> _pending = new(StringComparer.Ordinal);

    public Task<LlmInferenceHttpRequestStartResult> HttpRequestStartAsync(LlmInferenceHttpRequestStartRequest request, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(request);

        var transport = request.Transport == LlmInferenceHttpRequestStartTransport.Websocket
            ? CopilotRequestTransport.WebSocket
            : CopilotRequestTransport.Http;

        // The runtime dispatches httpRequestStart and httpRequestChunk frames
        // concurrently, so body chunks (including the terminal end frame) can
        // arrive before this start frame runs. GetOrAdd adopts any exchange a
        // racing chunk already created — with its buffered body — instead of
        // dropping those frames and hanging the body drain.
        var exchange = _pending.GetOrAdd(request.RequestId, id => new LlmInferenceExchange(id, _getServerRpc));
        exchange.Method = request.Method;
        exchange.Context = new CopilotRequestContext
        {
            RequestId = request.RequestId,
            SessionId = request.SessionId,
            Transport = transport,
            Url = request.Url,
            Headers = ToReadOnlyHeaders(request.Headers),
            CancellationToken = exchange.Abort.Token,
        };

        // Return from httpRequestStart immediately (after registering state) so
        // the runtime's RPC reply is not gated on the consumer's I/O. The actual
        // handler work runs asynchronously, exactly once per request.
        _ = RunAsync(exchange);

        return Task.FromResult(new LlmInferenceHttpRequestStartResult());
    }

    public Task<LlmInferenceHttpRequestChunkResult> HttpRequestChunkAsync(LlmInferenceHttpRequestChunkRequest request, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(request);

        // A chunk may arrive before its matching httpRequestStart (frames are
        // dispatched concurrently). GetOrAdd buffers the body into the
        // exchange's channel so no chunk — in particular the terminal end
        // frame — is ever lost; the start frame later adopts this same exchange.
        var exchange = _pending.GetOrAdd(request.RequestId, id => new LlmInferenceExchange(id, _getServerRpc));
        RouteChunk(exchange, request);

        return Task.FromResult(new LlmInferenceHttpRequestChunkResult());
    }

    private async Task RunAsync(LlmInferenceExchange exchange)
    {
        try
        {
            await _handler.HandleAsync(exchange).ConfigureAwait(false);
            if (!exchange.Finished)
            {
                await FinalizeAsync(exchange, 502, "LLM inference handler returned without finalising the response (call ResponseBody.EndAsync() or .ErrorAsync()).", code: null).ConfigureAwait(false);
            }
        }
        catch (Exception ex)
        {
            if (exchange.Cancelled || exchange.Abort.IsCancellationRequested)
            {
                // The runtime already cancelled this request; the handler's throw
                // is just the abort propagating out of its upstream call.
                await FinalizeAsync(exchange, 499, "Request cancelled by runtime", code: "cancelled").ConfigureAwait(false);
                return;
            }

            await FinalizeAsync(exchange, 502, ex.Message, code: null).ConfigureAwait(false);
        }
        finally
        {
            _pending.TryRemove(exchange.RequestId, out _);
        }
    }

    private static async Task FinalizeAsync(LlmInferenceExchange exchange, int status, string message, string? code)
    {
        if (exchange.Finished)
        {
            return;
        }

        try
        {
            if (!exchange.Started)
            {
                await exchange.StartResponseAsync(status, statusText: null, headers: null).ConfigureAwait(false);
            }

            await exchange.ErrorResponseAsync(message, code).ConfigureAwait(false);
        }
        catch
        {
            // Best-effort — the connection may already be dead.
        }
    }

    private static void RouteChunk(LlmInferenceExchange exchange, LlmInferenceHttpRequestChunkRequest chunk)
    {
        if (chunk.Cancel == true)
        {
            exchange.PushCancel(chunk.CancelReason);
            return;
        }

        if (!string.IsNullOrEmpty(chunk.Data))
        {
            exchange.PushChunk(DecodeChunkData(chunk.Data, chunk.Binary == true));
        }

        if (chunk.End == true)
        {
            exchange.PushEnd();
        }
    }

    private static byte[] DecodeChunkData(string data, bool binary) =>
        binary ? Convert.FromBase64String(data) : Encoding.UTF8.GetBytes(data);

    private static Dictionary<string, IReadOnlyList<string>> ToReadOnlyHeaders(IDictionary<string, IList<string>> headers)
    {
        var result = new Dictionary<string, IReadOnlyList<string>>(StringComparer.OrdinalIgnoreCase);
        foreach (var (name, values) in headers)
        {
            result[name] = values as IReadOnlyList<string> ?? [.. values];
        }

        return result;
    }
}

/// <summary>
/// Forwards upstream WebSocket messages back to the owning
/// <see cref="LlmInferenceExchange"/>. The 101 upgrade head is emitted eagerly
/// via <see cref="StartAsync"/> (the runtime gates the connect on it);
/// thereafter writes are serialised so the head always precedes any body or
/// terminal frame.
/// </summary>
internal sealed class LlmWebSocketResponseBridge(LlmInferenceExchange exchange)
{
    private readonly SemaphoreSlim _gate = new(1, 1);
    private bool _started;
    private bool _completed;

    /// <summary>Emit the 101 upgrade head now, acknowledging the WebSocket connect.</summary>
    internal Task StartAsync() => RunAsync(terminal: false, () => Task.CompletedTask);

    internal Task WriteAsync(CopilotWebSocketMessage message) => RunAsync(terminal: false, () =>
        message.IsBinary
            ? exchange.WriteResponseAsync(message.Data)
            : exchange.WriteResponseAsync(message.GetText()));

    internal Task EndAsync() => RunAsync(terminal: true, () => exchange.EndResponseAsync());

    internal Task ErrorAsync(string message, string? code) =>
        RunAsync(terminal: true, () => exchange.ErrorResponseAsync(message, code));

    private async Task RunAsync(bool terminal, Func<Task> action)
    {
        await _gate.WaitAsync().ConfigureAwait(false);
        try
        {
            if (_completed)
            {
                return;
            }

            if (!_started)
            {
                _started = true;
                await exchange.StartResponseAsync(101, statusText: null, headers: null).ConfigureAwait(false);
            }

            if (terminal)
            {
                _completed = true;
            }

            await action().ConfigureAwait(false);
        }
        finally
        {
            _gate.Release();
        }
    }
}

internal static class LlmInferenceHeaders
{
    // Computed/managed by the HTTP/WS stack; forwarding them verbatim either
    // throws or corrupts the request.
    internal static readonly HashSet<string> Forbidden = new(StringComparer.OrdinalIgnoreCase)
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
