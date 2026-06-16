/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Text;
using Xunit;

namespace GitHub.Copilot.Test.Unit.LlmInference;

#pragma warning disable GHCP001 // The LLM inference surface is intentionally experimental.

public class LlmInferenceAdapterTests
{
    private static readonly TimeSpan Timeout = TimeSpan.FromSeconds(10);

    private static LlmInferenceAdapter CreateAdapter(ILlmInferenceProvider provider, RecordingResponseChannel channel)
    {
        ILlmInferenceResponseChannel current = channel;
        return new LlmInferenceAdapter(provider, () => current);
    }

    [Fact]
    public async Task Stages_request_chunks_that_arrive_before_their_start_frame_and_replays_them_in_order()
    {
        var received = new List<string>();
        var done = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var provider = new InlineProvider(async req =>
        {
            await foreach (var chunk in req.RequestBody)
            {
                received.Add(Encoding.UTF8.GetString(chunk.ToArray()));
            }

            await req.ResponseBody.StartAsync(new LlmInferenceResponseInit { Status = 200 });
            await req.ResponseBody.EndAsync();
            done.SetResult();
        });

        var channel = new RecordingResponseChannel();
        var adapter = CreateAdapter(provider, channel);

        // Chunks arrive BEFORE the start frame (a reordering the runtime should
        // never produce). They must be staged and replayed once start registers.
        await adapter.HttpRequestChunkAsync(LlmFrames.Chunk("r1", "hello ", end: false));
        await adapter.HttpRequestChunkAsync(LlmFrames.Chunk("r1", "world", end: false));
        await adapter.HttpRequestChunkAsync(LlmFrames.Chunk("r1", "", end: true));

        await adapter.HttpRequestStartAsync(LlmFrames.Start("r1"));

        await done.Task.WaitAsync(Timeout);
        Assert.Equal("hello world", string.Concat(received));
    }

    [Fact]
    public async Task Emits_a_buffered_response_as_start_then_body_then_terminal_end()
    {
        var done = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var provider = new InlineProvider(async req =>
        {
            await foreach (var _ in req.RequestBody)
            {
                // drain
            }

            await req.ResponseBody.StartAsync(new LlmInferenceResponseInit
            {
                Status = 200,
                Headers = new Dictionary<string, IReadOnlyList<string>> { ["content-type"] = ["application/json"] },
            });
            await req.ResponseBody.WriteAsync("OK");
            await req.ResponseBody.EndAsync();
            done.SetResult();
        });

        var channel = new RecordingResponseChannel();
        var adapter = CreateAdapter(provider, channel);

        await adapter.HttpRequestStartAsync(LlmFrames.Start("r2"));
        await adapter.HttpRequestChunkAsync(LlmFrames.Chunk("r2", "", end: true));

        await done.Task.WaitAsync(Timeout);

        var start = Assert.Single(channel.Starts);
        Assert.Equal(200, start.Status);
        Assert.Equal("OK", channel.DecodeTextBody());

        var terminal = Assert.Single(channel.Chunks, c => c.End == true);
        Assert.Null(terminal.Error);
    }

    [Fact]
    public async Task Aborts_the_provider_and_throws_from_write_when_the_runtime_rejects_a_response_frame()
    {
        var aborted = false;
        var writeThrew = false;
        var settled = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var provider = new InlineProvider(async req =>
        {
            req.CancellationToken.Register(() => aborted = true);
            await foreach (var _ in req.RequestBody)
            {
                // drain
            }

            await req.ResponseBody.StartAsync(new LlmInferenceResponseInit { Status = 200 });
            try
            {
                await req.ResponseBody.WriteAsync("rejected-chunk");
            }
            catch (InvalidOperationException)
            {
                writeThrew = true;
            }

            settled.SetResult();
        });

        // The runtime accepts the start frame but rejects the body chunk.
        var channel = new RecordingResponseChannel(acceptStart: true, acceptChunk: false);
        var adapter = CreateAdapter(provider, channel);

        await adapter.HttpRequestStartAsync(LlmFrames.Start("r3"));
        await adapter.HttpRequestChunkAsync(LlmFrames.Chunk("r3", "", end: true));

        await settled.Task.WaitAsync(Timeout);
        Assert.True(writeThrew, "write should throw after the runtime rejects the chunk");
        Assert.True(aborted, "the provider's cancellation token should fire on rejection");
    }

    [Fact]
    public async Task Surfaces_a_runtime_cancel_chunk_as_a_cancelled_terminal_error()
    {
        var observedCancellation = false;
        var done = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var provider = new InlineProvider(async req =>
        {
            try
            {
                await foreach (var _ in req.RequestBody)
                {
                    // The cancel frame surfaces as an OperationCanceledException here.
                }
            }
            catch (OperationCanceledException)
            {
                observedCancellation = true;
                throw;
            }
            finally
            {
                done.TrySetResult();
            }
        });

        var channel = new RecordingResponseChannel();
        var adapter = CreateAdapter(provider, channel);

        await adapter.HttpRequestStartAsync(LlmFrames.Start("r4"));
        await adapter.HttpRequestChunkAsync(LlmFrames.Chunk("r4", cancel: true, cancelReason: "turn aborted"));

        await done.Task.WaitAsync(Timeout);
        await channel.Terminal.WaitAsync(Timeout);
        Assert.True(observedCancellation, "the request body iterator should throw on a cancel frame");

        // The adapter finalises a cancelled request as a 499 + error{code:cancelled}.
        var terminal = Assert.Single(channel.Chunks, c => c.Error is not null);
        Assert.Equal("cancelled", terminal.Error!.Code);
    }

    [Fact]
    public async Task Threads_the_runtime_session_id_into_the_request()
    {
        string? observedSessionId = null;
        var done = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var provider = new InlineProvider(async req =>
        {
            observedSessionId = req.SessionId;
            await foreach (var _ in req.RequestBody)
            {
                // drain
            }

            await req.ResponseBody.StartAsync(new LlmInferenceResponseInit { Status = 200 });
            await req.ResponseBody.EndAsync();
            done.SetResult();
        });

        var channel = new RecordingResponseChannel();
        var adapter = CreateAdapter(provider, channel);

        await adapter.HttpRequestStartAsync(LlmFrames.Start("r5", sessionId: "session-123"));
        await adapter.HttpRequestChunkAsync(LlmFrames.Chunk("r5", "", end: true));

        await done.Task.WaitAsync(Timeout);
        Assert.Equal("session-123", observedSessionId);
    }
}
