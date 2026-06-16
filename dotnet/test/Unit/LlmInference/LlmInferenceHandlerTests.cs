/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Net;
using System.Net.Http;
using System.Text;
using Xunit;

namespace GitHub.Copilot.Test.Unit.LlmInference;

#pragma warning disable GHCP001 // The LLM inference surface is intentionally experimental.

public class LlmInferenceHandlerTests
{
    private static readonly TimeSpan Timeout = TimeSpan.FromSeconds(10);

    private static Task Dispatch(LlmRequestHandler handler, LlmInferenceRequest request) =>
        ((ILlmInferenceProvider)handler).OnLlmRequestAsync(request);

    private static async IAsyncEnumerable<ReadOnlyMemory<byte>> AsyncBytes(params string[] chunks)
    {
        foreach (var chunk in chunks)
        {
            await Task.Yield();
            yield return Encoding.UTF8.GetBytes(chunk);
        }
    }

    private static LlmInferenceRequest HttpRequest(
        RecordingSink sink,
        IAsyncEnumerable<ReadOnlyMemory<byte>> body,
        string method = "POST",
        string url = "https://upstream.test/v1/chat/completions",
        IReadOnlyDictionary<string, IReadOnlyList<string>>? headers = null) =>
        new()
        {
            RequestId = "req-1",
            SessionId = "session-1",
            Method = method,
            Url = url,
            Headers = headers ?? new Dictionary<string, IReadOnlyList<string>>(),
            Transport = LlmInferenceTransport.Http,
            RequestBody = body,
            ResponseBody = sink,
        };

    /// <summary>A handler whose upstream call is a canned delegate (no network).</summary>
    private sealed class StubHandler(Func<HttpRequestMessage, HttpResponseMessage> forward) : LlmRequestHandler
    {
        protected override Task<HttpResponseMessage> ForwardAsync(HttpRequestMessage request, LlmRequestContext ctx) =>
            Task.FromResult(forward(request));
    }

    /// <summary>A handler that adds a header in <c>TransformRequestAsync</c>.</summary>
    private sealed class HeaderMutatingHandler(Func<HttpRequestMessage, HttpResponseMessage> forward) : LlmRequestHandler
    {
        protected override Task<HttpRequestMessage> TransformRequestAsync(HttpRequestMessage request, LlmRequestContext ctx)
        {
            request.Headers.TryAddWithoutValidation("authorization", "Bearer swapped-token");
            return Task.FromResult(request);
        }

        protected override Task<HttpResponseMessage> ForwardAsync(HttpRequestMessage request, LlmRequestContext ctx) =>
            Task.FromResult(forward(request));
    }

    [Fact]
    public async Task Forwards_request_body_and_streams_response_back_to_the_sink()
    {
        string? forwardedBody = null;
        var handler = new StubHandler(req =>
        {
            forwardedBody = req.Content!.ReadAsStringAsync().GetAwaiter().GetResult();
            return new HttpResponseMessage(HttpStatusCode.OK)
            {
                Content = new StringContent("RESPONSE-BODY", Encoding.UTF8, "application/json"),
            };
        });

        var sink = new RecordingSink();
        var request = HttpRequest(sink, AsyncBytes("{\"hello\":", "\"world\"}"));

        await Dispatch(handler, request).WaitAsync(Timeout);

        Assert.Equal("{\"hello\":\"world\"}", forwardedBody);

        var start = Assert.Single(sink.Starts);
        Assert.Equal(200, start.Status);
        Assert.Equal("RESPONSE-BODY", sink.DecodeBinaryBody());
        Assert.True(sink.Ended);
        Assert.Null(sink.Errored);
    }

    [Fact]
    public async Task Strips_forbidden_request_headers_before_forwarding()
    {
        var forwarded = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        var handler = new StubHandler(req =>
        {
            foreach (var header in req.Headers)
            {
                forwarded[header.Key] = string.Join(",", header.Value);
            }

            return new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent("ok") };
        });

        var sink = new RecordingSink();
        var headers = new Dictionary<string, IReadOnlyList<string>>
        {
            ["host"] = ["should-be-stripped.test"],
            ["x-tenant"] = ["acme"],
        };
        var request = HttpRequest(sink, AsyncBytes("body"), headers: headers);

        await Dispatch(handler, request).WaitAsync(Timeout);

        Assert.False(forwarded.ContainsKey("host"), "the forbidden host header must be stripped");
        Assert.Equal("acme", forwarded["x-tenant"]);
    }

    [Fact]
    public async Task Lets_a_subclass_mutate_the_outbound_request_headers()
    {
        string? observedAuth = null;
        var handler = new HeaderMutatingHandler(req =>
        {
            observedAuth = req.Headers.TryGetValues("authorization", out var values)
                ? string.Join(",", values)
                : null;
            return new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent("ok") };
        });

        var sink = new RecordingSink();
        var request = HttpRequest(sink, AsyncBytes("body"));

        await Dispatch(handler, request).WaitAsync(Timeout);

        Assert.Equal("Bearer swapped-token", observedAuth);
    }

    [Fact]
    public async Task Propagates_a_non_2xx_status_verbatim_to_the_runtime()
    {
        var handler = new StubHandler(_ =>
            new HttpResponseMessage((HttpStatusCode)429)
            {
                Content = new StringContent("slow down"),
            });

        var sink = new RecordingSink();
        var request = HttpRequest(sink, AsyncBytes());

        await Dispatch(handler, request).WaitAsync(Timeout);

        var start = Assert.Single(sink.Starts);
        Assert.Equal(429, start.Status);
        Assert.Equal("slow down", sink.DecodeBinaryBody());
        Assert.True(sink.Ended);
    }
}
