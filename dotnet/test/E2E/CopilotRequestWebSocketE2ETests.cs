/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER

using System.Net;
using System.Net.Sockets;
using System.Net.WebSockets;
using System.Security.Cryptography;
using System.Text;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

#pragma warning disable GHCP001 // The LLM inference surface is intentionally experimental.

/// <summary>
/// Drives a full agent turn over the WebSocket inference transport through a
/// <see cref="CopilotRequestHandler"/> subclass. A single handler services both
/// transports against an in-process fake upstream: model-layer GETs and the
/// single-shot HTTP <c>/responses</c> call are forwarded over HTTP, while the
/// main turn flows over a real WebSocket opened by a
/// <see cref="CopilotWebSocketForwarder"/>.
/// </summary>
/// <remarks>
/// This is the regression test for the WebSocket upgrade deadlock: the runtime
/// blocks the WebSocket connect until it observes the 101 response head, so the
/// handler must emit it eagerly rather than waiting for the first upstream
/// message. Without the eager start the turn never completes and this test
/// times out.
/// </remarks>
[Trait(E2ETestTraits.Backend, E2ETestTraits.SelfConfiguredBackend)]
public class CopilotRequestWebSocketE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "copilot_request_websocket", output)
{
    [Fact]
    public async Task Services_A_WebSocket_Turn_End_To_End_Via_The_Request_Handler()
    {
        await using var upstream = new FakeCopilotUpstream();
        var counters = new HandlerCounters();
        var handler = new ForwardingUpstreamHandler(upstream.BaseUrl, counters);

        // Enable the WebSocket Responses transport in the spawned runtime so the
        // main agent turn picks the WS path; single-shot calls still go over HTTP
        // through the same handler.
        var env = Ctx.GetEnvironment();
        env["COPILOT_EXP_COPILOT_CLI_WEBSOCKET_RESPONSES"] = "true";

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(),
            RequestHandler = handler,
        }, environment: env);
        await client.StartAsync();

        var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        string content;
        try
        {
            var msg = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Say OK." });
            content = msg?.Data.Content ?? string.Empty;
        }
        finally
        {
            await session.DisposeAsync();
        }

        // The HTTP hooks fired — the runtime issued model-layer GETs (catalog,
        // policy) and possibly a single-shot inference, all forwarded over HTTP.
        Assert.True(counters.HttpRequests > 0, "expected SendRequestAsync to fire");

        // The WebSocket hooks fired — the main agent turn went over the WS path
        // and we observed messages in both directions.
        Assert.True(counters.WsRequestMessages > 0, "expected SendRequestMessageAsync (runtime -> upstream) to fire");
        Assert.True(counters.WsResponseMessages > 0, "expected SendResponseMessageAsync (upstream -> runtime) to fire");
        Assert.True(upstream.WsRequestMessageCount > 0, "expected upstream WS to receive request messages");

        // The synthetic content surfaced in the assistant turn — proves the full
        // chain (runtime -> handler -> upstream -> handler -> runtime) over the
        // WebSocket transport is intact.
        // Validate the final assistant response arrived (guards against truncated captures)
        Assert.Contains("OK from synthetic", content);
    }
}

/// <summary>Cross-direction message counters shared with the test assertions.</summary>
internal sealed class HandlerCounters
{
    public int HttpRequests;
    public int WsRequestMessages;
    public int WsResponseMessages;
}

/// <summary>
/// A <see cref="CopilotRequestHandler"/> that points every intercepted request at
/// the in-process <see cref="FakeCopilotUpstream"/>: HTTP requests are rewritten
/// and forwarded by the base class, and WebSocket connections are opened against
/// the rewritten URL via a counting <see cref="CopilotWebSocketForwarder"/>.
/// </summary>
internal sealed class ForwardingUpstreamHandler(string upstreamBaseUrl, HandlerCounters counters) : CopilotRequestHandler
{
    private readonly Uri _upstream = new(upstreamBaseUrl);

    protected override Task<HttpResponseMessage> SendRequestAsync(HttpRequestMessage request, CopilotRequestContext ctx)
    {
        Interlocked.Increment(ref counters.HttpRequests);
        request.RequestUri = Rewrite(request.RequestUri!);
        return base.SendRequestAsync(request, ctx);
    }

    protected override Task<CopilotWebSocketHandler> OpenWebSocketAsync(CopilotRequestContext ctx)
    {
        ctx = new CopilotRequestContext(ctx) { Url = Rewrite(new Uri(ctx.Url)).ToString() };
        return Task.FromResult<CopilotWebSocketHandler>(new CountingForwardingWebSocketHandler(ctx, counters));
    }

    private Uri Rewrite(Uri original) => new UriBuilder(original)
    {
        Scheme = _upstream.Scheme,
        Host = _upstream.Host,
        Port = _upstream.Port,
    }.Uri;
}

/// <summary>
/// A pass-through forwarding handler that counts messages in both directions.
/// </summary>
internal sealed class CountingForwardingWebSocketHandler(
    CopilotRequestContext context,
    HandlerCounters counters)
    : CopilotWebSocketForwarder(context)
{
    public override Task SendRequestMessageAsync(CopilotWebSocketMessage message)
    {
        Interlocked.Increment(ref counters.WsRequestMessages);
        return base.SendRequestMessageAsync(message);
    }

    public override Task SendResponseMessageAsync(CopilotWebSocketMessage message)
    {
        Interlocked.Increment(ref counters.WsResponseMessages);
        return base.SendResponseMessageAsync(message);
    }
}

/// <summary>
/// In-process upstream that speaks the CAPI shapes the runtime needs: model
/// catalog (advertising the WebSocket <c>/responses</c> endpoint), policy, a
/// single-shot HTTP <c>/responses</c> SSE stream, and a WebSocket endpoint at
/// <c>/responses</c> that answers each inbound <c>response.create</c> with the
/// ordered <c>/responses</c> events the reducer expects.
/// </summary>
internal sealed class FakeCopilotUpstream : IAsyncDisposable
{
    private const string HttpText = "OK from synthetic HTTP upstream.";
    private const string WsText = "OK from synthetic WS upstream.";

    private readonly TcpListener _listener = new(IPAddress.Loopback, 0);
    private readonly CancellationTokenSource _cts = new();
    private readonly Task _loop;
    private readonly List<Task> _connections = [];
    private int _wsRequestMessages;

    public string BaseUrl { get; }

    public int WsRequestMessageCount => Volatile.Read(ref _wsRequestMessages);

    public FakeCopilotUpstream()
    {
        _listener.Start();
        var port = ((IPEndPoint)_listener.LocalEndpoint).Port;
        BaseUrl = $"http://127.0.0.1:{port}/";
        _loop = AcceptLoopAsync(_cts.Token);
    }

    private async Task AcceptLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            TcpClient client;
            try
            {
                client = await _listener.AcceptTcpClientAsync(ct).ConfigureAwait(false);
            }
            catch (OperationCanceledException) when (ct.IsCancellationRequested)
            {
                break;
            }
            catch (SocketException) when (ct.IsCancellationRequested)
            {
                break;
            }

            _connections.Add(HandleConnectionAsync(client, ct));
        }
    }

    private async Task HandleConnectionAsync(TcpClient client, CancellationToken ct)
    {
        using var connection = client;
        try
        {
            var stream = client.GetStream();
            var head = new StringBuilder();
            var next = new byte[1];
            // Do not read ahead into the first WebSocket frame or HTTP request body.
            while (head.Length < 4 ||
                head[head.Length - 4] != '\r' || head[head.Length - 3] != '\n' ||
                head[head.Length - 2] != '\r' || head[head.Length - 1] != '\n')
            {
                if (await stream.ReadAsync(next, ct).ConfigureAwait(false) == 0)
                {
                    throw new EndOfStreamException("Upstream request ended before its headers.");
                }

                head.Append((char)next[0]);
            }

            var lines = head.ToString().Split("\r\n", StringSplitOptions.RemoveEmptyEntries);
            var headers = lines.Skip(1).Select(line => line.Split(':', 2))
                .ToDictionary(parts => parts[0], parts => parts[1].Trim(), StringComparer.OrdinalIgnoreCase);
            if (headers.TryGetValue("Upgrade", out var upgrade) &&
                upgrade.Equals("websocket", StringComparison.OrdinalIgnoreCase))
            {
                var key = headers["Sec-WebSocket-Key"];
                var accept = Convert.ToBase64String(SHA1.HashData(
                    Encoding.ASCII.GetBytes(key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")));
                await stream.WriteAsync(Encoding.ASCII.GetBytes(
                    $"HTTP/1.1 101 Switching Protocols\r\nConnection: Upgrade\r\nUpgrade: websocket\r\nSec-WebSocket-Accept: {accept}\r\n\r\n"), ct).ConfigureAwait(false);
                using var socket = WebSocket.CreateFromStream(stream, isServer: true, subProtocol: null,
                    keepAliveInterval: Timeout.InfiniteTimeSpan);
                await HandleWebSocketAsync(socket, ct).ConfigureAwait(false);
            }
            else
            {
                if (headers.ContainsKey("Transfer-Encoding"))
                {
                    throw new InvalidDataException("The upstream fixture requires a fixed-length request body.");
                }

                var remaining = headers.TryGetValue("Content-Length", out var length)
                    ? int.Parse(length, System.Globalization.CultureInfo.InvariantCulture) : 0;
                if (remaining < 0) throw new InvalidDataException("Negative upstream request body length.");
                var buffer = new byte[8 * 1024];
                while (remaining > 0)
                {
                    var read = await stream.ReadAsync(buffer.AsMemory(0, Math.Min(remaining, buffer.Length)), ct).ConfigureAwait(false);
                    if (read == 0) throw new EndOfStreamException("Upstream request body was truncated.");
                    remaining -= read;
                }

                await HandleHttpAsync(stream, lines[0].Split(' ')[1], ct).ConfigureAwait(false);
            }
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested)
        {
        }
        catch (WebSocketException error) when (error.WebSocketErrorCode == WebSocketError.ConnectionClosedPrematurely)
        {
            // The runtime may dispose its transport after the final response.
        }
    }

    private async Task HandleWebSocketAsync(WebSocket socket, CancellationToken ct)
    {
        var buffer = new byte[16 * 1024];

        while (socket.State == WebSocketState.Open && !ct.IsCancellationRequested)
        {
            var message = await ReceiveTextAsync(socket, buffer, ct).ConfigureAwait(false);
            if (message is null)
            {
                break;
            }

            Interlocked.Increment(ref _wsRequestMessages);

            foreach (var (_, json) in ResponseEvents(WsText, "resp_stub_ws"))
            {
                var bytes = Encoding.UTF8.GetBytes(json);
                await socket.SendAsync(
                    new ArraySegment<byte>(bytes),
                    WebSocketMessageType.Text,
                    endOfMessage: true,
                    ct).ConfigureAwait(false);
            }
        }

        if (!ct.IsCancellationRequested &&
            (socket.State == WebSocketState.Open || socket.State == WebSocketState.CloseReceived))
        {
            await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, null, ct).ConfigureAwait(false);
        }
    }

    private static async Task<string?> ReceiveTextAsync(WebSocket socket, byte[] buffer, CancellationToken ct)
    {
        using var assembled = new MemoryStream();
        WebSocketReceiveResult result;
        do
        {
            result = await socket.ReceiveAsync(new ArraySegment<byte>(buffer), ct).ConfigureAwait(false);
            if (result.MessageType == WebSocketMessageType.Close)
            {
                return null;
            }

            assembled.Write(buffer, 0, result.Count);
        }
        while (!result.EndOfMessage);

        return Encoding.UTF8.GetString(assembled.ToArray());
    }

    private static async Task HandleHttpAsync(NetworkStream stream, string requestPath, CancellationToken ct)
    {
        var path = requestPath.Split('?', 2)[0].ToLowerInvariant();
        string contentType = "application/json";
        string body;

        if (path.EndsWith("/models", StringComparison.Ordinal))
        {
            body = ModelCatalogJson;
        }
        else if (path.Contains("/models/session"))
        {
            body = "{}";
        }
        else if (path.Contains("/policy"))
        {
            body = "{\"state\":\"enabled\"}";
        }
        else if (path.EndsWith("/responses", StringComparison.Ordinal))
        {
            contentType = "text/event-stream";
            body = BuildSse(HttpText, "resp_stub_http");
        }
        else
        {
            body = "{}";
        }

        var bytes = Encoding.UTF8.GetBytes(body);
        await stream.WriteAsync(Encoding.ASCII.GetBytes(
            $"HTTP/1.1 200 OK\r\nContent-Type: {contentType}\r\nContent-Length: {bytes.Length}\r\nConnection: close\r\n\r\n"), ct).ConfigureAwait(false);
        await stream.WriteAsync(bytes, ct).ConfigureAwait(false);
    }

    private static string BuildSse(string text, string id)
    {
        var sb = new StringBuilder();
        foreach (var (type, json) in ResponseEvents(text, id))
        {
            sb.Append("event: ").Append(type).Append("\ndata: ").Append(json).Append("\n\n");
        }

        return sb.ToString();
    }

    private static (string Type, string Json)[] ResponseEvents(string text, string id) =>
    [
        ("response.created",
            "{\"type\":\"response.created\",\"response\":{\"id\":\"" + id + "\",\"object\":\"response\",\"status\":\"in_progress\",\"output\":[]}}"),
        ("response.output_item.added",
            "{\"type\":\"response.output_item.added\",\"output_index\":0,\"item\":{\"id\":\"msg_1\",\"type\":\"message\",\"role\":\"assistant\",\"content\":[]}}"),
        ("response.content_part.added",
            "{\"type\":\"response.content_part.added\",\"output_index\":0,\"content_index\":0,\"part\":{\"type\":\"output_text\",\"text\":\"\"}}"),
        ("response.output_text.delta",
            "{\"type\":\"response.output_text.delta\",\"output_index\":0,\"content_index\":0,\"delta\":\"" + text + "\"}"),
        ("response.output_text.done",
            "{\"type\":\"response.output_text.done\",\"output_index\":0,\"content_index\":0,\"text\":\"" + text + "\"}"),
        ("response.completed",
            "{\"type\":\"response.completed\",\"response\":{\"id\":\"" + id + "\",\"object\":\"response\",\"status\":\"completed\",\"output\":[{\"id\":\"msg_1\",\"type\":\"message\",\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\"" + text + "\"}]}],\"usage\":{\"input_tokens\":5,\"output_tokens\":7,\"total_tokens\":12}}}"),
    ];

    private const string ModelCatalogJson =
        "{\"data\":[{\"id\":\"claude-sonnet-5\",\"name\":\"Claude Sonnet 5\",\"object\":\"model\",\"vendor\":\"Anthropic\",\"version\":\"1\",\"preview\":false,\"model_picker_enabled\":true,\"supported_endpoints\":[\"/responses\",\"ws:/responses\"],\"capabilities\":{\"type\":\"chat\",\"family\":\"claude-sonnet-5\",\"tokenizer\":\"o200k_base\",\"limits\":{\"max_context_window_tokens\":200000,\"max_output_tokens\":8192},\"supports\":{\"streaming\":true,\"tool_calls\":true,\"parallel_tool_calls\":true,\"vision\":true}}}]}";

    public async ValueTask DisposeAsync()
    {
        _cts.Cancel();
        _listener.Stop();
        try
        {
            await _loop.ConfigureAwait(false);
            await Task.WhenAll(_connections).ConfigureAwait(false);
        }
        finally
        {
            _cts.Dispose();
        }
    }
}

#endif
