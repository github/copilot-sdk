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

    /// <summary>
    /// Cancelled when the runtime aborts this in-flight request. Subclasses that
    /// issue their own I/O should pass this through so the upstream call is torn
    /// down too.
    /// </summary>
    public CancellationToken CancellationToken { get; init; }
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
/// Base class for SDK consumers who want to observe or mutate the LLM inference
/// requests the runtime issues. An instance is returned directly from
/// <see cref="LlmInferenceConfig.Handler"/>.
/// </summary>
/// <remarks>
/// <para>
/// Default behaviour is a transparent pass-through: each request is forwarded to
/// its original URL via a shared <see cref="HttpClient"/> (HTTP) or a
/// <see cref="ClientWebSocket"/> (WebSocket), and the upstream response is
/// streamed back to the runtime unchanged. Consumers subclass and override one
/// or more virtual methods to interpose:
/// </para>
/// <list type="bullet">
/// <item><see cref="TransformRequestAsync"/> — mutate the outbound HTTP request.</item>
/// <item><see cref="ForwardAsync"/> — replace the upstream HTTP call entirely
/// (e.g. to return a canned <see cref="HttpResponseMessage"/> for a cache hit).</item>
/// <item><see cref="TransformResponseAsync"/> — mutate the upstream HTTP response
/// on its way back to the runtime.</item>
/// <item><see cref="ForwardWebSocketAsync"/> — replace the upstream WebSocket open
/// (e.g. to set custom upgrade headers).</item>
/// <item><see cref="TransformRequestMessageAsync"/> / <see cref="TransformResponseMessageAsync"/>
/// — observe or mutate WebSocket messages in either direction.</item>
/// </list>
/// <para>
/// The same subclass handles both transports — dispatch keys on
/// <see cref="LlmInferenceRequest.Transport"/>.
/// </para>
/// </remarks>
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

        var ctx = new LlmRequestContext
        {
            RequestId = request.RequestId,
            SessionId = request.SessionId,
            Transport = request.Transport,
            CancellationToken = request.CancellationToken,
        };

        if (request.Transport == LlmInferenceTransport.WebSocket)
        {
            await HandleWebSocketAsync(request, ctx).ConfigureAwait(false);
        }
        else
        {
            await HandleHttpAsync(request, ctx).ConfigureAwait(false);
        }
    }

    // ─── HTTP virtual hooks ────────────────────────────────────────────

    /// <summary>
    /// Mutates the outbound HTTP request before it is issued. Default: pass
    /// through unchanged.
    /// </summary>
    protected virtual Task<HttpRequestMessage> TransformRequestAsync(HttpRequestMessage request, LlmRequestContext ctx) =>
        Task.FromResult(request);

    /// <summary>
    /// Issues the upstream HTTP call. Default: a shared <see cref="HttpClient"/>
    /// with response-headers-read streaming and the context's cancellation token
    /// wired through. Override to short-circuit with a canned response or to use
    /// a different client.
    /// </summary>
    protected virtual Task<HttpResponseMessage> ForwardAsync(HttpRequestMessage request, LlmRequestContext ctx) =>
        s_sharedHttpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, ctx.CancellationToken);

    /// <summary>
    /// Mutates the upstream HTTP response before it streams back to the runtime.
    /// Default: pass through unchanged.
    /// </summary>
    protected virtual Task<HttpResponseMessage> TransformResponseAsync(HttpResponseMessage response, LlmRequestContext ctx) =>
        Task.FromResult(response);

    // ─── WebSocket virtual hooks ───────────────────────────────────────

    /// <summary>
    /// Opens the upstream WebSocket. Default: a <see cref="ClientWebSocket"/>
    /// connected to the original URL. Override to set custom upgrade headers or
    /// use a different client.
    /// </summary>
    protected virtual async Task<WebSocket> ForwardWebSocketAsync(string url, IReadOnlyDictionary<string, IReadOnlyList<string>> headers, LlmRequestContext ctx)
    {
        var ws = new ClientWebSocket();
#if !NETSTANDARD2_0
        foreach (var (name, values) in headers)
        {
            if (s_forbiddenRequestHeaders.Contains(name))
            {
                continue;
            }

            try
            {
                ws.Options.SetRequestHeader(name, string.Join(", ", values));
            }
            catch
            {
                // Some headers are managed by the handshake; ignore rejections.
            }
        }
#endif
        await ws.ConnectAsync(ToWebSocketUri(url), ctx.CancellationToken).ConfigureAwait(false);
        return ws;
    }

    /// <summary>
    /// Observes or mutates an outbound (request) WebSocket message — one the
    /// runtime is sending to the upstream. Return <see langword="null"/> to drop
    /// the message. Default: pass through unchanged.
    /// </summary>
    protected virtual ValueTask<LlmWebSocketMessage?> TransformRequestMessageAsync(LlmWebSocketMessage message, LlmRequestContext ctx) =>
        new(message);

    /// <summary>
    /// Observes or mutates an inbound (response) WebSocket message — one the
    /// upstream is sending back to the runtime. Return <see langword="null"/> to
    /// drop the message. Default: pass through unchanged.
    /// </summary>
    protected virtual ValueTask<LlmWebSocketMessage?> TransformResponseMessageAsync(LlmWebSocketMessage message, LlmRequestContext ctx) =>
        new(message);

    // ─── HTTP dispatch ─────────────────────────────────────────────────

    private async Task HandleHttpAsync(LlmInferenceRequest req, LlmRequestContext ctx)
    {
        using var initialRequest = await BuildHttpRequestAsync(req).ConfigureAwait(false);
        using var transformed = await TransformRequestAsync(initialRequest, ctx).ConfigureAwait(false);
        using var response = await ForwardAsync(transformed, ctx).ConfigureAwait(false);
        using var finalResponse = await TransformResponseAsync(response, ctx).ConfigureAwait(false);
        await StreamResponseToSinkAsync(finalResponse, req, ctx).ConfigureAwait(false);
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

    // ─── WebSocket dispatch ────────────────────────────────────────────

    private async Task HandleWebSocketAsync(LlmInferenceRequest req, LlmRequestContext ctx)
    {
        using var upstream = await ForwardWebSocketAsync(req.Url, req.Headers, ctx).ConfigureAwait(false);

        // Ack the upgrade to the runtime (mirrors the protocol's 101-equivalent
        // start frame the runtime is waiting for).
        await req.ResponseBody.StartAsync(new LlmInferenceResponseInit { Status = 101 }).ConfigureAwait(false);

        using var pumpCts = CancellationTokenSource.CreateLinkedTokenSource(req.CancellationToken);
        var token = pumpCts.Token;

        // Upstream → runtime: read messages off the socket and write them to the
        // response sink.
        var serverPump = Task.Run(async () =>
        {
            while (upstream.State == WebSocketState.Open)
            {
                var message = await ReceiveMessageAsync(upstream, token).ConfigureAwait(false);
                if (message is null)
                {
                    break;
                }

                var mutated = await TransformResponseMessageAsync(message.Value, ctx).ConfigureAwait(false);
                if (mutated is null)
                {
                    continue;
                }

                if (mutated.Value.IsBinary)
                {
                    await req.ResponseBody.WriteAsync(mutated.Value.Data).ConfigureAwait(false);
                }
                else
                {
                    await req.ResponseBody.WriteAsync(mutated.Value.GetText()).ConfigureAwait(false);
                }
            }
        }, token);

        // Runtime → upstream: read request-body chunks and forward each as one
        // WebSocket message. The runtime sends WS text frames as UTF-8 bytes, so
        // surface them as text by default.
        var clientPump = Task.Run(async () =>
        {
            await foreach (var chunk in req.RequestBody.WithCancellation(token).ConfigureAwait(false))
            {
                var mutated = await TransformRequestMessageAsync(new LlmWebSocketMessage(chunk, isBinary: false), ctx).ConfigureAwait(false);
                if (mutated is null)
                {
                    continue;
                }

                var type = mutated.Value.IsBinary ? WebSocketMessageType.Binary : WebSocketMessageType.Text;
                await upstream.SendAsync(new ArraySegment<byte>(mutated.Value.Data.ToArray()), type, endOfMessage: true, token).ConfigureAwait(false);
            }
        }, token);

        var first = await Task.WhenAny(clientPump, serverPump).ConfigureAwait(false);

        // Whichever side won, tear the upstream down so the loser unwinds.
        pumpCts.Cancel();
        await CloseWebSocketQuietlyAsync(upstream).ConfigureAwait(false);

        if (first == clientPump && clientPump.IsFaulted)
        {
            // Runtime cancellation propagating out of the request iterator.
            await ObserveQuietlyAsync(serverPump).ConfigureAwait(false);
            await clientPump.ConfigureAwait(false);
            return;
        }

        await ObserveQuietlyAsync(clientPump).ConfigureAwait(false);
        await ObserveQuietlyAsync(serverPump).ConfigureAwait(false);

        await req.ResponseBody.EndAsync().ConfigureAwait(false);
    }

    private static async Task<LlmWebSocketMessage?> ReceiveMessageAsync(WebSocket socket, CancellationToken cancellationToken)
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
            // The losing pump's teardown exception is expected; swallow it.
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
