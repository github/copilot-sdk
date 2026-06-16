/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using System.Collections.Concurrent;
using System.Diagnostics.CodeAnalysis;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading.Channels;

namespace GitHub.Copilot;

/// <summary>
/// Transport the runtime would otherwise use to issue an intercepted
/// model-layer request.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public enum LlmInferenceTransport
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
/// An outbound model-layer HTTP (or WebSocket) request the runtime is asking
/// the SDK consumer to service on its behalf.
/// </summary>
/// <remarks>
/// This is a low-level shape: URL / method / headers verbatim, body bytes
/// delivered as an async sequence, and the response delivered through the
/// <see cref="ResponseBody"/> sink. The runtime does not classify the request
/// (no provider type, endpoint kind, or wire API); consumers that need that
/// information derive it from the URL / headers themselves.
/// </remarks>
[Experimental(Diagnostics.Experimental)]
public sealed class LlmInferenceRequest
{
    /// <summary>Opaque runtime-minted id, stable across the request lifecycle.</summary>
    public required string RequestId { get; init; }

    /// <summary>
    /// Id of the runtime session that triggered this request, when one is in
    /// scope. <see langword="null"/> for out-of-session requests (e.g. startup
    /// model catalog).
    /// </summary>
    public string? SessionId { get; init; }

    /// <summary>HTTP method (<c>GET</c>, <c>POST</c>, ...).</summary>
    public required string Method { get; init; }

    /// <summary>Absolute request URL.</summary>
    public required string Url { get; init; }

    /// <summary>HTTP request headers, lowercased names mapped to multi-valued lists.</summary>
    public required IReadOnlyDictionary<string, IReadOnlyList<string>> Headers { get; init; }

    /// <summary>
    /// Transport the runtime would otherwise use. <see cref="LlmInferenceTransport.Http"/>
    /// covers plain HTTP and SSE responses; <see cref="LlmInferenceTransport.WebSocket"/>
    /// indicates a full-duplex message channel. Consumers branch on this to
    /// decide whether to service the request with an HTTP client or a WebSocket
    /// client.
    /// </summary>
    public LlmInferenceTransport Transport { get; init; }

    /// <summary>
    /// Request body bytes, yielded as they arrive from the runtime. Always
    /// enumerable; an empty body yields zero chunks before completing. For
    /// WebSocket transport each element is one inbound message.
    /// </summary>
    public required IAsyncEnumerable<ReadOnlyMemory<byte>> RequestBody { get; init; }

    /// <summary>
    /// Cancelled when the runtime aborts this in-flight request (e.g. the agent
    /// turn was aborted upstream). Pass it straight to <c>HttpClient.SendAsync</c>
    /// / your transport so the upstream call is torn down too. After it fires,
    /// writes to <see cref="ResponseBody"/> are ignored.
    /// </summary>
    public CancellationToken CancellationToken { get; init; }

    /// <summary>
    /// Sink the consumer writes the upstream response into. Call
    /// <see cref="LlmInferenceResponseSink.StartAsync"/> exactly once before
    /// writing body chunks, then zero or more
    /// <see cref="LlmInferenceResponseSink.WriteAsync(ReadOnlyMemory{byte})"/>
    /// calls, and finish with <see cref="LlmInferenceResponseSink.EndAsync"/> or
    /// <see cref="LlmInferenceResponseSink.ErrorAsync"/>.
    /// </summary>
    public required LlmInferenceResponseSink ResponseBody { get; init; }
}

/// <summary>Response head passed to <see cref="LlmInferenceResponseSink.StartAsync"/>.</summary>
[Experimental(Diagnostics.Experimental)]
public sealed class LlmInferenceResponseInit
{
    /// <summary>HTTP status code (101 acknowledges a WebSocket upgrade).</summary>
    public int Status { get; init; }

    /// <summary>Optional HTTP status reason phrase.</summary>
    public string? StatusText { get; init; }

    /// <summary>Response headers, lowercased names mapped to multi-valued lists.</summary>
    public IReadOnlyDictionary<string, IReadOnlyList<string>>? Headers { get; init; }
}

/// <summary>
/// Sink the consumer writes the upstream response into. The state machine is
/// strict: <see cref="StartAsync"/> once → zero or more <c>WriteAsync</c> →
/// exactly one of <see cref="EndAsync"/> or <see cref="ErrorAsync"/>. Calling
/// out of order throws.
/// </summary>
[Experimental(Diagnostics.Experimental)]
public abstract class LlmInferenceResponseSink
{
    /// <summary>Sends the response head (status + headers) back to the runtime.</summary>
    public abstract Task StartAsync(LlmInferenceResponseInit init);

    /// <summary>Sends a binary body chunk (base64-encoded on the wire).</summary>
    public abstract Task WriteAsync(ReadOnlyMemory<byte> data);

    /// <summary>Sends a UTF-8 text body chunk.</summary>
    public abstract Task WriteAsync(string text);

    /// <summary>Marks end-of-stream cleanly.</summary>
    public abstract Task EndAsync();

    /// <summary>Marks end-of-stream with a transport-level failure.</summary>
    public abstract Task ErrorAsync(string message, string? code = null);
}

/// <summary>
/// Implemented by SDK consumers to service the LLM inference requests the
/// runtime would otherwise issue itself. The same callback handles both
/// buffered and streaming responses — the consumer just calls
/// <see cref="LlmInferenceResponseSink.WriteAsync(ReadOnlyMemory{byte})"/> zero
/// or more times before <see cref="LlmInferenceResponseSink.EndAsync"/>.
/// </summary>
/// <remarks>
/// Prefer subclassing <see cref="LlmRequestHandler"/> for a transparent
/// pass-through starting point; implement this interface directly only when you
/// need full control over the raw byte streams.
/// </remarks>
[Experimental(Diagnostics.Experimental)]
public interface ILlmInferenceProvider
{
    /// <summary>
    /// Invoked by the runtime once per outbound LLM request the consumer has
    /// opted to handle. The consumer is responsible for eventually calling
    /// either <see cref="LlmInferenceResponseSink.EndAsync"/> or
    /// <see cref="LlmInferenceResponseSink.ErrorAsync"/>; failing to do so leaks
    /// runtime state. Throwing surfaces a transport-level failure to the runtime
    /// (equivalent to <c>ResponseBody.ErrorAsync(...)</c> when
    /// <see cref="LlmInferenceResponseSink.StartAsync"/> has not yet been called).
    /// </summary>
    Task OnLlmRequestAsync(LlmInferenceRequest request);
}

/// <summary>
/// Adapts an <see cref="ILlmInferenceProvider"/> into the generated
/// <see cref="ILlmInferenceHandler"/> shape consumed by the SDK's RPC
/// dispatcher.
/// </summary>
/// <remarks>
/// Maintains a per-<c>requestId</c> state table: each <c>httpRequestStart</c>
/// allocates a body channel + response sink and fires
/// <see cref="ILlmInferenceProvider.OnLlmRequestAsync"/> in the background.
/// Subsequent <c>httpRequestChunk</c> frames are routed into the channel. The
/// sink translates <c>Start</c> / <c>Write</c> / <c>End</c> / <c>Error</c> calls
/// into outbound <c>llmInference.httpResponseStart</c> /
/// <c>llmInference.httpResponseChunk</c> calls.
/// </remarks>
internal sealed class LlmInferenceAdapter : ILlmInferenceHandler
{
    private readonly ILlmInferenceProvider _provider;
    private readonly Func<ILlmInferenceResponseChannel?> _getChannel;
    private readonly ConcurrentDictionary<string, PendingState> _pending = new(StringComparer.Ordinal);

    // Defense-in-depth backstop: chunks that arrive before their start frame
    // (a reordering the runtime's single ordered dispatch should make
    // impossible) are staged here and drained the moment httpRequestStart
    // registers the matching state, so a body byte is never silently dropped.
    private readonly ConcurrentDictionary<string, List<LlmInferenceHttpRequestChunkRequest>> _staged = new(StringComparer.Ordinal);

    internal LlmInferenceAdapter(ILlmInferenceProvider provider, Func<ServerRpc?> getServerRpc)
        : this(provider, WrapServerRpc(getServerRpc ?? throw new ArgumentNullException(nameof(getServerRpc))))
    {
    }

    internal LlmInferenceAdapter(ILlmInferenceProvider provider, Func<ILlmInferenceResponseChannel?> getChannel)
    {
        _provider = provider ?? throw new ArgumentNullException(nameof(provider));
        _getChannel = getChannel ?? throw new ArgumentNullException(nameof(getChannel));
    }

    /// <summary>
    /// Adapts a <see cref="ServerRpc"/> getter into a response-channel getter,
    /// caching the wrapper so a new one is allocated only when the underlying
    /// connection changes (e.g. reconnect).
    /// </summary>
    private static Func<ILlmInferenceResponseChannel?> WrapServerRpc(Func<ServerRpc?> getServerRpc)
    {
        ServerRpc? cachedRpc = null;
        ILlmInferenceResponseChannel? cachedChannel = null;
        return () =>
        {
            var rpc = getServerRpc();
            if (rpc is null)
            {
                return null;
            }

            if (!ReferenceEquals(rpc, cachedRpc))
            {
                cachedRpc = rpc;
                cachedChannel = new ServerRpcResponseChannel(rpc);
            }

            return cachedChannel;
        };
    }

    public Task<LlmInferenceHttpRequestStartResult> HttpRequestStartAsync(LlmInferenceHttpRequestStartRequest request, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(request);

        var state = new PendingState();
        _pending[request.RequestId] = state;

        if (_staged.TryRemove(request.RequestId, out var stagedChunks))
        {
            foreach (var chunk in stagedChunks)
            {
                RouteChunk(state, chunk);
            }
        }

        var sink = new AdapterResponseSink(request.RequestId, state, _getChannel, _pending);
        state.Sink = sink;

        var transport = request.Transport == LlmInferenceHttpRequestStartTransport.Websocket
            ? LlmInferenceTransport.WebSocket
            : LlmInferenceTransport.Http;

        var llmRequest = new LlmInferenceRequest
        {
            RequestId = request.RequestId,
            SessionId = request.SessionId,
            Method = request.Method,
            Url = request.Url,
            Headers = ToReadOnlyHeaders(request.Headers),
            Transport = transport,
            RequestBody = state.Body.ReadAllAsync(state.Abort.Token),
            CancellationToken = state.Abort.Token,
            ResponseBody = sink,
        };

        // Return from httpRequestStart immediately (after registering state) so
        // the runtime's RPC reply is not gated on the consumer's I/O. The actual
        // provider work runs asynchronously.
        _ = RunProviderAsync(llmRequest, state, sink);

        return Task.FromResult(new LlmInferenceHttpRequestStartResult());
    }

    public Task<LlmInferenceHttpRequestChunkResult> HttpRequestChunkAsync(LlmInferenceHttpRequestChunkRequest request, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(request);

        if (_pending.TryGetValue(request.RequestId, out var state))
        {
            RouteChunk(state, request);
        }
        else
        {
            _staged.AddOrUpdate(
                request.RequestId,
                _ => [request],
                (_, list) =>
                {
                    list.Add(request);
                    return list;
                });
        }

        return Task.FromResult(new LlmInferenceHttpRequestChunkResult());
    }

    private async Task RunProviderAsync(LlmInferenceRequest request, PendingState state, AdapterResponseSink sink)
    {
        try
        {
            await _provider.OnLlmRequestAsync(request).ConfigureAwait(false);
            if (!state.Finished)
            {
                await FailViaSink(
                    sink,
                    state,
                    "LLM inference provider returned without finalising the response (call ResponseBody.EndAsync() or .ErrorAsync()).").ConfigureAwait(false);
            }
        }
        catch (Exception ex)
        {
            if (state.Cancelled || state.Abort.IsCancellationRequested)
            {
                // The runtime already cancelled this request; the provider's
                // throw is just the abort propagating out of its upstream call.
                await FinishCancelled(sink, state).ConfigureAwait(false);
                return;
            }

            await FailViaSink(sink, state, ex.Message).ConfigureAwait(false);
        }
    }

    private static async Task FailViaSink(AdapterResponseSink sink, PendingState state, string message)
    {
        if (state.Finished)
        {
            return;
        }

        try
        {
            if (!state.Started)
            {
                await sink.StartAsync(new LlmInferenceResponseInit { Status = 502 }).ConfigureAwait(false);
            }

            await sink.ErrorAsync(message).ConfigureAwait(false);
        }
        catch
        {
            // Best-effort — the connection may already be dead.
        }
    }

    private static async Task FinishCancelled(AdapterResponseSink sink, PendingState state)
    {
        if (state.Finished)
        {
            return;
        }

        try
        {
            if (!state.Started)
            {
                await sink.StartAsync(new LlmInferenceResponseInit { Status = 499 }).ConfigureAwait(false);
            }

            await sink.ErrorAsync("Request cancelled by runtime", "cancelled").ConfigureAwait(false);
        }
        catch
        {
            // Best-effort — the runtime already dropped the request on cancel.
        }
    }

    private static void RouteChunk(PendingState state, LlmInferenceHttpRequestChunkRequest chunk)
    {
        if (chunk.Cancel == true)
        {
            state.Cancelled = true;
            state.Abort.Cancel();
            state.Body.PushCancel(chunk.CancelReason);
            return;
        }

        if (!string.IsNullOrEmpty(chunk.Data))
        {
            state.Body.PushChunk(DecodeChunkData(chunk.Data, chunk.Binary == true));
        }

        if (chunk.End == true)
        {
            state.Body.PushEnd();
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

    private sealed class PendingState
    {
        public BodyChannel Body { get; } = new();

        public CancellationTokenSource Abort { get; } = new();

        public bool Started { get; set; }

        public bool Finished { get; set; }

        public bool Cancelled { get; set; }

        public AdapterResponseSink? Sink { get; set; }
    }

    /// <summary>
    /// An unbounded channel of request-body items exposed as an
    /// <see cref="IAsyncEnumerable{T}"/> of byte chunks. A cancel item surfaces
    /// as an <see cref="OperationCanceledException"/> out of the enumerator so
    /// the consumer's upstream call is torn down.
    /// </summary>
    private sealed class BodyChannel
    {
        private readonly Channel<Item> _channel = Channel.CreateUnbounded<Item>(
            new UnboundedChannelOptions { SingleReader = true, SingleWriter = true });

        public void PushChunk(byte[] data) => _channel.Writer.TryWrite(new Item { Chunk = data });

        public void PushEnd() => _channel.Writer.TryWrite(new Item { End = true });

        public void PushCancel(string? reason) => _channel.Writer.TryWrite(new Item { Cancel = true, CancelReason = reason });

        public async IAsyncEnumerable<ReadOnlyMemory<byte>> ReadAllAsync([EnumeratorCancellation] CancellationToken cancellationToken = default)
        {
            while (await _channel.Reader.WaitToReadAsync(cancellationToken).ConfigureAwait(false))
            {
                while (_channel.Reader.TryRead(out var item))
                {
                    if (item.Cancel)
                    {
                        _channel.Writer.TryComplete();
                        throw new OperationCanceledException(
                            item.CancelReason is null
                                ? "Request cancelled by runtime"
                                : $"Request cancelled by runtime: {item.CancelReason}");
                    }

                    if (item.End)
                    {
                        _channel.Writer.TryComplete();
                        yield break;
                    }

                    if (item.Chunk is { Length: > 0 })
                    {
                        yield return item.Chunk;
                    }
                }
            }
        }

        private struct Item
        {
            public byte[]? Chunk;
            public bool End;
            public bool Cancel;
            public string? CancelReason;
        }
    }

    private sealed class AdapterResponseSink(
        string requestId,
        PendingState state,
        Func<ILlmInferenceResponseChannel?> getChannel,
        ConcurrentDictionary<string, PendingState> pending) : LlmInferenceResponseSink
    {
        public override async Task StartAsync(LlmInferenceResponseInit init)
        {
            ArgumentNullException.ThrowIfNull(init);

            if (state.Started)
            {
                throw new InvalidOperationException("LLM inference response sink StartAsync() called twice.");
            }

            if (state.Finished)
            {
                throw new InvalidOperationException("LLM inference response sink already finished.");
            }

            state.Started = true;
            var result = await Channel()
                .HttpResponseStartAsync(requestId, init.Status, ToWireHeaders(init.Headers), init.StatusText)
                .ConfigureAwait(false);
            if (!result.Accepted)
            {
                RejectedByRuntime();
            }
        }

        public override Task WriteAsync(ReadOnlyMemory<byte> data) =>
            WriteChunk(Convert.ToBase64String(data.ToArray()), binary: true);

        public override Task WriteAsync(string text)
        {
            ArgumentNullException.ThrowIfNull(text);
            return WriteChunk(text, binary: false);
        }

        public override async Task EndAsync()
        {
            if (state.Finished)
            {
                return;
            }

            state.Finished = true;
            pending.TryRemove(requestId, out _);
            await Channel().HttpResponseChunkAsync(requestId, string.Empty, end: true).ConfigureAwait(false);
        }

        public override async Task ErrorAsync(string message, string? code = null)
        {
            ArgumentNullException.ThrowIfNull(message);

            if (state.Finished)
            {
                return;
            }

            state.Finished = true;
            pending.TryRemove(requestId, out _);
            await Channel()
                .HttpResponseChunkAsync(
                    requestId,
                    string.Empty,
                    end: true,
                    error: new LlmInferenceHttpResponseChunkError { Message = message, Code = code })
                .ConfigureAwait(false);
        }

        private async Task WriteChunk(string data, bool binary)
        {
            if (state.Cancelled)
            {
                throw new InvalidOperationException("LLM inference request was cancelled by the runtime.");
            }

            if (!state.Started)
            {
                throw new InvalidOperationException("LLM inference response sink WriteAsync() called before StartAsync().");
            }

            if (state.Finished)
            {
                throw new InvalidOperationException("LLM inference response sink WriteAsync() called after EndAsync()/ErrorAsync().");
            }

            var result = await Channel()
                .HttpResponseChunkAsync(requestId, data, binary: binary, end: false)
                .ConfigureAwait(false);
            if (!result.Accepted)
            {
                RejectedByRuntime();
            }
        }

        private ILlmInferenceResponseChannel Channel() =>
            getChannel() ?? throw new InvalidOperationException("LLM inference response sink used after RPC connection closed.");

        // The runtime acknowledges every response frame with accepted; accepted:
        // false means it has dropped the request (e.g. it cancelled), so we abort
        // the provider's upstream work and stop emitting.
        private void RejectedByRuntime()
        {
            if (!state.Cancelled)
            {
                state.Cancelled = true;
                state.Abort.Cancel();
            }

            state.Finished = true;
            pending.TryRemove(requestId, out _);
            throw new InvalidOperationException("LLM inference response was rejected by the runtime (request no longer active).");
        }

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
    }
}

/// <summary>
/// Minimal seam over the runtime-bound <c>llmInference</c> server API the
/// adapter uses to push response frames back to the runtime. Extracted as an
/// interface so the adapter can be unit-tested without a live JSON-RPC
/// connection.
/// </summary>
internal interface ILlmInferenceResponseChannel
{
    Task<LlmInferenceHttpResponseStartResult> HttpResponseStartAsync(string requestId, long status, IDictionary<string, IList<string>> headers, string? statusText = null);

    Task<LlmInferenceHttpResponseChunkResult> HttpResponseChunkAsync(string requestId, string data, bool? binary = null, bool? end = null, LlmInferenceHttpResponseChunkError? error = null);
}

/// <summary>
/// Production <see cref="ILlmInferenceResponseChannel"/> backed by the generated
/// <see cref="ServerRpc"/> client.
/// </summary>
internal sealed class ServerRpcResponseChannel(ServerRpc serverRpc) : ILlmInferenceResponseChannel
{
    public Task<LlmInferenceHttpResponseStartResult> HttpResponseStartAsync(string requestId, long status, IDictionary<string, IList<string>> headers, string? statusText = null) =>
        serverRpc.LlmInference.HttpResponseStartAsync(requestId, status, headers, statusText);

    public Task<LlmInferenceHttpResponseChunkResult> HttpResponseChunkAsync(string requestId, string data, bool? binary = null, bool? end = null, LlmInferenceHttpResponseChunkError? error = null) =>
        serverRpc.LlmInference.HttpResponseChunkAsync(requestId, data, binary, end, error);
}
