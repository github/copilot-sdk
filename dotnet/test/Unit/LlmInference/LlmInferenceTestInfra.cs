/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using System.Text;

namespace GitHub.Copilot.Test.Unit.LlmInference;

#pragma warning disable GHCP001 // The LLM inference surface is intentionally experimental.

/// <summary>
/// In-memory <see cref="ILlmInferenceResponseChannel"/> that records every
/// response frame the adapter emits and lets a test choose what
/// <c>accepted</c> value the runtime returns.
/// </summary>
internal sealed class RecordingResponseChannel(bool acceptStart = true, bool acceptChunk = true) : ILlmInferenceResponseChannel
{
    public sealed record StartFrame(long Status, string? StatusText, IDictionary<string, IList<string>> Headers);

    public sealed record ChunkFrame(string Data, bool? Binary, bool? End, LlmInferenceHttpResponseChunkError? Error);

    public List<StartFrame> Starts { get; } = [];

    public List<ChunkFrame> Chunks { get; } = [];

    private readonly TaskCompletionSource _terminal = new(TaskCreationOptions.RunContinuationsAsynchronously);

    /// <summary>Completes once a terminal response chunk (end or error) is recorded.</summary>
    public Task Terminal => _terminal.Task;

    public Task<LlmInferenceHttpResponseStartResult> HttpResponseStartAsync(string requestId, long status, IDictionary<string, IList<string>> headers, string? statusText = null)
    {
        Starts.Add(new StartFrame(status, statusText, headers));
        return Task.FromResult(new LlmInferenceHttpResponseStartResult { Accepted = acceptStart });
    }

    public Task<LlmInferenceHttpResponseChunkResult> HttpResponseChunkAsync(string requestId, string data, bool? binary = null, bool? end = null, LlmInferenceHttpResponseChunkError? error = null)
    {
        Chunks.Add(new ChunkFrame(data, binary, end, error));
        if (end == true || error is not null)
        {
            _terminal.TrySetResult();
        }

        return Task.FromResult(new LlmInferenceHttpResponseChunkResult { Accepted = acceptChunk });
    }

    /// <summary>Concatenates the UTF-8 text of all non-terminal body chunks.</summary>
    public string DecodeTextBody()
    {
        var sb = new StringBuilder();
        foreach (var chunk in Chunks)
        {
            if (chunk.Error is not null || chunk.Data.Length == 0)
            {
                continue;
            }

            sb.Append(chunk.Binary == true
                ? Encoding.UTF8.GetString(Convert.FromBase64String(chunk.Data))
                : chunk.Data);
        }

        return sb.ToString();
    }
}

/// <summary>An <see cref="ILlmInferenceProvider"/> driven by an inline delegate.</summary>
internal sealed class InlineProvider(Func<LlmInferenceRequest, Task> handler) : ILlmInferenceProvider
{
    public Task OnLlmRequestAsync(LlmInferenceRequest request) => handler(request);
}

/// <summary>Records everything written to a <see cref="LlmInferenceResponseSink"/>.</summary>
internal sealed class RecordingSink : LlmInferenceResponseSink
{
    public List<LlmInferenceResponseInit> Starts { get; } = [];

    public List<string> TextWrites { get; } = [];

    public List<byte[]> BinaryWrites { get; } = [];

    public bool Ended { get; private set; }

    public (string Message, string? Code)? Errored { get; private set; }

    /// <summary>Concatenates all binary body writes and decodes them as UTF-8.</summary>
    public string DecodeBinaryBody() => Encoding.UTF8.GetString(BinaryWrites.SelectMany(b => b).ToArray());

    public override Task StartAsync(LlmInferenceResponseInit init)
    {
        Starts.Add(init);
        return Task.CompletedTask;
    }

    public override Task WriteAsync(ReadOnlyMemory<byte> data)
    {
        BinaryWrites.Add(data.ToArray());
        return Task.CompletedTask;
    }

    public override Task WriteAsync(string text)
    {
        TextWrites.Add(text);
        return Task.CompletedTask;
    }

    public override Task EndAsync()
    {
        Ended = true;
        return Task.CompletedTask;
    }

    public override Task ErrorAsync(string message, string? code = null)
    {
        Errored = (message, code);
        return Task.CompletedTask;
    }
}

/// <summary>Convenience builders for the generated request frames.</summary>
internal static class LlmFrames
{
    public static LlmInferenceHttpRequestStartRequest Start(
        string requestId,
        string url = "https://example.test/v1/chat",
        string method = "POST",
        string? sessionId = null,
        LlmInferenceHttpRequestStartTransport? transport = null) =>
        new()
        {
            RequestId = requestId,
            Url = url,
            Method = method,
            SessionId = sessionId,
            Headers = new Dictionary<string, IList<string>>(),
            Transport = transport,
        };

    public static LlmInferenceHttpRequestChunkRequest Chunk(
        string requestId,
        string data = "",
        bool? end = null,
        bool? binary = null,
        bool? cancel = null,
        string? cancelReason = null) =>
        new()
        {
            RequestId = requestId,
            Data = data,
            End = end,
            Binary = binary,
            Cancel = cancel,
            CancelReason = cancelReason,
        };
}
