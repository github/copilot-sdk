/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER

using System.Net;
using System.Net.Sockets;
using System.Net.WebSockets;
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
/// <see cref="ForwardingCopilotWebSocketHandler"/>.
/// </summary>
/// <remarks>
/// This is the regression test for the WebSocket upgrade deadlock: the runtime
/// blocks the WebSocket connect until it observes the 101 response head, so the
/// handler must emit it eagerly rather than waiting for the first upstream
/// message. Without the eager start the turn never completes and this test
/// times out.
/// </remarks>
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
            Environment = env,
        });
        await client.StartAsync();

        var session = await client.CreateSessionAsync(new SessionConfig
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
/// the rewritten URL via a counting <see cref="ForwardingCopilotWebSocketHandler"/>.
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
        var wsUrl = Rewrite(new Uri(ctx.Url)).ToString();
        return Task.FromResult<CopilotWebSocketHandler>(new CountingForwardingWebSocketHandler(ctx, wsUrl, counters));
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
    string url,
    HandlerCounters counters)
    : ForwardingCopilotWebSocketHandler(context, url)
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

    private readonly HttpListener _listener = new();
    private readonly CancellationTokenSource _cts = new();
    private readonly Task _loop;
    private int _wsRequestMessages;

    public string BaseUrl { get; }

    public int WsRequestMessageCount => Volatile.Read(ref _wsRequestMessages);

    public FakeCopilotUpstream()
    {
        var port = GetFreePort();
        BaseUrl = $"http://127.0.0.1:{port}/";
        _listener.Prefixes.Add(BaseUrl);
        _listener.Start();
        _loop = Task.Run(() => AcceptLoopAsync(_cts.Token), _cts.Token);
    }

    private async Task AcceptLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            HttpListenerContext context;
            try
            {
                context = await _listener.GetContextAsync().ConfigureAwait(false);
            }
            catch
            {
                break;
            }

            _ = Task.Run(() => HandleContextAsync(context, ct), ct);
        }
    }

    private async Task HandleContextAsync(HttpListenerContext context, CancellationToken ct)
    {
        try
        {
            if (context.Request.IsWebSocketRequest)
            {
                await HandleWebSocketAsync(context, ct).ConfigureAwait(false);
            }
            else
            {
                await HandleHttpAsync(context, ct).ConfigureAwait(false);
            }
        }
        catch
        {
            // Best-effort: the runtime tears connections down as turns complete.
        }
    }

    private async Task HandleWebSocketAsync(HttpListenerContext context, CancellationToken ct)
    {
        var wsContext = await context.AcceptWebSocketAsync(subProtocol: null).ConfigureAwait(false);
        var socket = wsContext.WebSocket;
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

        if (socket.State == WebSocketState.Open || socket.State == WebSocketState.CloseReceived)
        {
            try
            {
                await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, null, CancellationToken.None).ConfigureAwait(false);
            }
            catch
            {
                // Already torn down.
            }
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

    private static async Task HandleHttpAsync(HttpListenerContext context, CancellationToken ct)
    {
        if (context.Request.HasEntityBody)
        {
            using var input = context.Request.InputStream;
            var drain = new byte[8 * 1024];
            while (await input.ReadAsync(drain.AsMemory(), ct).ConfigureAwait(false) > 0)
            {
                // Discard the request body; the synthetic response is fixed.
            }
        }

        var path = context.Request.Url!.AbsolutePath.ToLowerInvariant();
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
        context.Response.StatusCode = 200;
        context.Response.ContentType = contentType;
        context.Response.ContentLength64 = bytes.Length;
        await context.Response.OutputStream.WriteAsync(bytes.AsMemory(), ct).ConfigureAwait(false);
        context.Response.OutputStream.Close();
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
        "{\"data\":[{\"id\":\"claude-sonnet-4.5\",\"name\":\"Claude Sonnet 4.5\",\"object\":\"model\",\"vendor\":\"Anthropic\",\"version\":\"1\",\"preview\":false,\"model_picker_enabled\":true,\"supported_endpoints\":[\"/responses\",\"ws:/responses\"],\"capabilities\":{\"type\":\"chat\",\"family\":\"claude-sonnet-4.5\",\"tokenizer\":\"o200k_base\",\"limits\":{\"max_context_window_tokens\":200000,\"max_output_tokens\":8192},\"supports\":{\"streaming\":true,\"tool_calls\":true,\"parallel_tool_calls\":true,\"vision\":true}}}]}";

    private static int GetFreePort()
    {
        using var probe = new TcpListener(IPAddress.Loopback, 0);
        probe.Start();
        return ((IPEndPoint)probe.LocalEndpoint).Port;
    }

    public async ValueTask DisposeAsync()
    {
        _cts.Cancel();
        try
        {
            _listener.Stop();
            _listener.Close();
        }
        catch
        {
            // Already stopped.
        }

        try
        {
            await _loop.ConfigureAwait(false);
        }
        catch
        {
            // Accept loop unwinds on listener shutdown.
        }

        _cts.Dispose();
    }
}

#endif
