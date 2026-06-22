/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Text.RegularExpressions;

namespace GitHub.Copilot.Test.E2E;

#pragma warning disable GHCP001 // The LLM inference surface is intentionally experimental.

/// <summary>
/// A <see cref="CopilotRequestHandler"/> subclass for e2e tests that records every
/// intercepted request (url + threaded session id) and fully replaces the
/// upstream call with a fabricated, well-formed response for every model-layer
/// endpoint, so an agent turn completes entirely off-network — no upstream
/// server and no CAPI proxy acting as the inference endpoint.
/// </summary>
/// <remarks>
/// <para>
/// This exercises the public extension surface end to end: a consumer subclasses
/// <see cref="CopilotRequestHandler"/> and overrides <see cref="SendRequestAsync"/> to
/// short-circuit the upstream HTTP call with any <see cref="HttpResponseMessage"/>
/// it likes. The base class streams that response back to the runtime.
/// </para>
/// <para>
/// All response bodies are emitted as raw JSON string literals rather than via
/// <c>JsonSerializer</c>: the test project disables reflection-based STJ on
/// net8.0 (<c>JsonSerializerIsReflectionEnabledByDefault=false</c>), so
/// serializing anonymous types would throw at runtime.
/// </para>
/// </remarks>
internal sealed class RecordingRequestHandler : CopilotRequestHandler
{
    internal const string SyntheticText = "OK from the synthetic stream.";

    private static readonly Regex WantsStreamRegex = new("\"stream\"\\s*:\\s*true", RegexOptions.Compiled);

    private readonly ConcurrentQueue<InterceptedRequest> _records = new();

    public IReadOnlyCollection<InterceptedRequest> Records => _records;

    public IReadOnlyList<InterceptedRequest> InferenceRequests =>
        [.. _records.Where(r => IsInferenceUrl(r.Url))];

    protected override async Task<HttpResponseMessage> SendRequestAsync(HttpRequestMessage request, CopilotRequestContext ctx)
    {
        var url = request.RequestUri!.ToString();
        _records.Enqueue(new InterceptedRequest(url, ctx.SessionId));

        var bodyText = request.Content is null
            ? string.Empty
#if NET8_0_OR_GREATER
            : await request.Content.ReadAsStringAsync(ctx.CancellationToken).ConfigureAwait(false);
#else
            : await request.Content.ReadAsStringAsync().ConfigureAwait(false);
#endif

        return IsInferenceUrl(url)
            ? BuildInferenceResponse(url, bodyText)
            : BuildNonInferenceResponse(url);
    }

    internal static bool IsInferenceUrl(string url)
    {
        var u = url.ToLowerInvariant();
        return u.EndsWith("/chat/completions", StringComparison.Ordinal)
            || u.EndsWith("/responses", StringComparison.Ordinal)
            || u.EndsWith("/v1/messages", StringComparison.Ordinal)
            || u.EndsWith("/messages", StringComparison.Ordinal);
    }

    /// <summary>
    /// Synthesizes a well-formed inference response so the agent turn completes.
    /// The runtime selects <c>/responses</c> for both the CAPI and BYOK sessions
    /// here; <c>/chat/completions</c> is handled too for robustness.
    /// </summary>
    private static HttpResponseMessage BuildInferenceResponse(string url, string bodyText)
    {
        var wantsStream = WantsStreamRegex.IsMatch(bodyText);
        var u = url.ToLowerInvariant();

        if (u.Contains("/responses", StringComparison.Ordinal))
        {
            return wantsStream
                ? Sse(string.Concat(ResponsesStreamEvents))
                : Json(BufferedResponseJson);
        }

        if (u.Contains("/chat/completions", StringComparison.Ordinal) && wantsStream)
        {
            return Sse(string.Concat(ChatCompletionStreamEvents));
        }

        // /chat/completions non-streaming (and any other inference url) — buffered JSON.
        return Json(BufferedChatCompletionJson);
    }

    /// <summary>
    /// Serves the non-inference model-layer GETs/POSTs the runtime issues
    /// (catalog, model session, policy). These flow through the same callback
    /// but carry no session id (they happen outside an agent turn). Shared with
    /// the cancel/error e2e handlers so the turn can reach the inference step.
    /// </summary>
    internal static HttpResponseMessage BuildNonInferenceResponse(string url)
    {
        var u = url.ToLowerInvariant();
        if (u.EndsWith("/models", StringComparison.Ordinal))
        {
            return Json(ModelCatalogJson);
        }

        if (u.Contains("/models/session", StringComparison.Ordinal))
        {
            return Json("{}");
        }

        if (u.Contains("/policy", StringComparison.Ordinal))
        {
            return Json("{\"state\":\"enabled\"}");
        }

        return Json("{}");
    }

    internal static HttpResponseMessage Json(string body) => new(HttpStatusCode.OK)
    {
        Content = new StringContent(body, Encoding.UTF8, "application/json"),
    };

    private static HttpResponseMessage Sse(string body) => new(HttpStatusCode.OK)
    {
        Content = new StringContent(body, Encoding.UTF8, "text/event-stream"),
    };

    private static readonly string[] ResponsesStreamEvents =
    [
        "event: response.created\ndata: {\"type\":\"response.created\",\"response\":{\"id\":\"resp_stub_1\",\"object\":\"response\",\"status\":\"in_progress\",\"output\":[]}}\n\n",
        "event: response.output_item.added\ndata: {\"type\":\"response.output_item.added\",\"output_index\":0,\"item\":{\"id\":\"msg_1\",\"type\":\"message\",\"role\":\"assistant\",\"content\":[]}}\n\n",
        "event: response.content_part.added\ndata: {\"type\":\"response.content_part.added\",\"output_index\":0,\"content_index\":0,\"part\":{\"type\":\"output_text\",\"text\":\"\"}}\n\n",
        "event: response.output_text.delta\ndata: {\"type\":\"response.output_text.delta\",\"output_index\":0,\"content_index\":0,\"delta\":\"" + SyntheticText + "\"}\n\n",
        "event: response.output_text.done\ndata: {\"type\":\"response.output_text.done\",\"output_index\":0,\"content_index\":0,\"text\":\"" + SyntheticText + "\"}\n\n",
        "event: response.completed\ndata: {\"type\":\"response.completed\",\"response\":{\"id\":\"resp_stub_1\",\"object\":\"response\",\"status\":\"completed\",\"output\":[{\"id\":\"msg_1\",\"type\":\"message\",\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\"" + SyntheticText + "\"}]}],\"usage\":{\"input_tokens\":5,\"output_tokens\":7,\"total_tokens\":12}}}\n\n",
    ];

    private static readonly string[] ChatCompletionStreamEvents =
    [
        "data: {\"id\":\"chatcmpl-stub-1\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"claude-sonnet-4.5\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"\"},\"finish_reason\":null}]}\n\n",
        "data: {\"id\":\"chatcmpl-stub-1\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"claude-sonnet-4.5\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"" + SyntheticText + "\"},\"finish_reason\":null}]}\n\n",
        "data: {\"id\":\"chatcmpl-stub-1\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"claude-sonnet-4.5\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":7,\"total_tokens\":12}}\n\n",
        "data: [DONE]\n\n",
    ];

    private static readonly string BufferedResponseJson =
        "{\"id\":\"resp_stub_1\",\"object\":\"response\",\"status\":\"completed\",\"output\":[{\"id\":\"msg_1\",\"type\":\"message\",\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\"" + SyntheticText + "\"}]}],\"usage\":{\"input_tokens\":5,\"output_tokens\":7,\"total_tokens\":12}}";

    private static readonly string BufferedChatCompletionJson =
        "{\"id\":\"chatcmpl-stub-1\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"claude-sonnet-4.5\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"" + SyntheticText + "\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":7,\"total_tokens\":12}}";

    private const string ModelCatalogJson =
        "{\"data\":[{\"id\":\"claude-sonnet-4.5\",\"name\":\"Claude Sonnet 4.5\",\"object\":\"model\",\"vendor\":\"Anthropic\",\"version\":\"1\",\"preview\":false,\"model_picker_enabled\":true,\"capabilities\":{\"type\":\"chat\",\"family\":\"claude-sonnet-4.5\",\"tokenizer\":\"o200k_base\",\"limits\":{\"max_context_window_tokens\":200000,\"max_output_tokens\":8192},\"supports\":{\"streaming\":true,\"tool_calls\":true,\"parallel_tool_calls\":true,\"vision\":true}}}]}";
}

/// <summary>A single request the callback intercepted.</summary>
internal sealed record InterceptedRequest(string Url, string? SessionId);
