/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Collections.Concurrent;
using System.Text;
using System.Text.RegularExpressions;

namespace GitHub.Copilot.Test.E2E;

#pragma warning disable GHCP001 // The LLM inference surface is intentionally experimental.

/// <summary>
/// An <see cref="ILlmInferenceProvider"/> for e2e tests that records every
/// intercepted request (url + threaded session id) and fabricates well-formed
/// responses for every model-layer endpoint, so an agent turn completes
/// entirely off-network — no upstream server and no CAPI proxy acting as the
/// inference endpoint.
/// </summary>
/// <remarks>
/// All response bodies are emitted as raw JSON string literals rather than via
/// <c>JsonSerializer</c>: the test project disables reflection-based STJ on
/// net8.0 (<c>JsonSerializerIsReflectionEnabledByDefault=false</c>), so
/// serializing anonymous types would throw at runtime.
/// </remarks>
internal sealed class RecordingInferenceProvider : ILlmInferenceProvider
{
    internal const string SyntheticText = "OK from the synthetic stream.";

    private static readonly Regex WantsStreamRegex = new("\"stream\"\\s*:\\s*true", RegexOptions.Compiled);

    private readonly ConcurrentQueue<InterceptedRequest> _records = new();

    public IReadOnlyCollection<InterceptedRequest> Records => _records;

    public IReadOnlyList<InterceptedRequest> InferenceRequests =>
        [.. _records.Where(r => IsInferenceUrl(r.Url))];

    public async Task OnLlmRequestAsync(LlmInferenceRequest request)
    {
        _records.Enqueue(new InterceptedRequest(request.Url, request.SessionId));

        if (IsInferenceUrl(request.Url))
        {
            await HandleInferenceAsync(request).ConfigureAwait(false);
        }
        else
        {
            await HandleNonInferenceModelTrafficAsync(request).ConfigureAwait(false);
        }
    }

    internal static bool IsInferenceUrl(string url)
    {
        var u = url.ToLowerInvariant();
        return u.EndsWith("/chat/completions", StringComparison.Ordinal)
            || u.EndsWith("/responses", StringComparison.Ordinal)
            || u.EndsWith("/v1/messages", StringComparison.Ordinal)
            || u.EndsWith("/messages", StringComparison.Ordinal);
    }

    private static async Task<string> DrainRequestAsync(LlmInferenceRequest req)
    {
        using var buffer = new MemoryStream();
        await foreach (var chunk in req.RequestBody.ConfigureAwait(false))
        {
            if (chunk.Length > 0)
            {
                buffer.Write(chunk.ToArray(), 0, chunk.Length);
            }
        }

        return Encoding.UTF8.GetString(buffer.ToArray());
    }

    private static async Task RespondBufferedAsync(LlmInferenceRequest req, int status, string contentType, string body)
    {
        await DrainRequestAsync(req).ConfigureAwait(false);
        await req.ResponseBody.StartAsync(new LlmInferenceResponseInit
        {
            Status = status,
            Headers = Headers(contentType),
        }).ConfigureAwait(false);
        if (body.Length > 0)
        {
            await req.ResponseBody.WriteAsync(body).ConfigureAwait(false);
        }

        await req.ResponseBody.EndAsync().ConfigureAwait(false);
    }

    /// <summary>
    /// Serves the non-inference model-layer GETs/POSTs the runtime issues
    /// (catalog, model session, policy). These flow through the same callback
    /// but carry no session id (they happen outside an agent turn).
    /// </summary>
    private static async Task HandleNonInferenceModelTrafficAsync(LlmInferenceRequest req)
    {
        var url = req.Url.ToLowerInvariant();
        if (url.EndsWith("/models", StringComparison.Ordinal))
        {
            await RespondBufferedAsync(req, 200, "application/json", ModelCatalogJson).ConfigureAwait(false);
            return;
        }

        if (url.Contains("/models/session", StringComparison.Ordinal))
        {
            await RespondBufferedAsync(req, 200, "application/json", "{}").ConfigureAwait(false);
            return;
        }

        if (url.Contains("/policy", StringComparison.Ordinal))
        {
            await RespondBufferedAsync(req, 200, "application/json", "{\"state\":\"enabled\"}").ConfigureAwait(false);
            return;
        }

        await RespondBufferedAsync(req, 200, "application/json", "{}").ConfigureAwait(false);
    }

    /// <summary>
    /// Synthesizes a well-formed inference response so the agent turn completes.
    /// The runtime selects <c>/responses</c> for both the CAPI and BYOK sessions
    /// here; <c>/chat/completions</c> is handled too for robustness.
    /// </summary>
    private static async Task HandleInferenceAsync(LlmInferenceRequest req)
    {
        var bodyText = await DrainRequestAsync(req).ConfigureAwait(false);
        var wantsStream = WantsStreamRegex.IsMatch(bodyText);
        var url = req.Url.ToLowerInvariant();

        if (url.Contains("/responses", StringComparison.Ordinal))
        {
            if (!wantsStream)
            {
                await req.ResponseBody.StartAsync(new LlmInferenceResponseInit { Status = 200, Headers = Headers("application/json") }).ConfigureAwait(false);
                await req.ResponseBody.WriteAsync(BufferedResponseJson).ConfigureAwait(false);
                await req.ResponseBody.EndAsync().ConfigureAwait(false);
                return;
            }

            await req.ResponseBody.StartAsync(new LlmInferenceResponseInit { Status = 200, Headers = Headers("text/event-stream") }).ConfigureAwait(false);
            foreach (var sseEvent in ResponsesStreamEvents)
            {
                await req.ResponseBody.WriteAsync(sseEvent).ConfigureAwait(false);
            }

            await req.ResponseBody.EndAsync().ConfigureAwait(false);
            return;
        }

        if (url.Contains("/chat/completions", StringComparison.Ordinal) && wantsStream)
        {
            await req.ResponseBody.StartAsync(new LlmInferenceResponseInit { Status = 200, Headers = Headers("text/event-stream") }).ConfigureAwait(false);
            foreach (var sseEvent in ChatCompletionStreamEvents)
            {
                await req.ResponseBody.WriteAsync(sseEvent).ConfigureAwait(false);
            }

            await req.ResponseBody.EndAsync().ConfigureAwait(false);
            return;
        }

        // /chat/completions non-streaming — buffered JSON.
        await req.ResponseBody.StartAsync(new LlmInferenceResponseInit { Status = 200, Headers = Headers("application/json") }).ConfigureAwait(false);
        await req.ResponseBody.WriteAsync(BufferedChatCompletionJson).ConfigureAwait(false);
        await req.ResponseBody.EndAsync().ConfigureAwait(false);
    }

    private static Dictionary<string, IReadOnlyList<string>> Headers(string contentType) =>
        new() { ["content-type"] = [contentType] };

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
