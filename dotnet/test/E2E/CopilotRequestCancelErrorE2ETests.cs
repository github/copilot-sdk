/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Net.Http;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

#pragma warning disable GHCP001 // The LLM inference surface is intentionally experimental.

/// <summary>
/// Cancellation and error coverage for <see cref="CopilotRequestHandler"/>. These
/// two scenarios exercise the handler's terminal paths that the happy-path
/// session-id and WebSocket tests never reach:
/// <list type="bullet">
/// <item>
/// <description>
/// <b>Error</b> — the handler throws from <see cref="CopilotRequestHandler.SendRequestAsync"/>
/// for an inference request. The base adapter reports a transport error back to
/// the runtime rather than hanging.
/// </description>
/// </item>
/// <item>
/// <description>
/// <b>Runtime cancel</b> — the handler blocks an inference request indefinitely;
/// when the consumer aborts the turn the runtime cancels the in-flight request,
/// firing <see cref="CopilotRequestContext.CancellationToken"/>. The handler
/// observes the abort instead of leaking a stuck request.
/// </description>
/// </item>
/// </list>
/// Non-inference model-layer requests (catalog, policy, model session) are served
/// via <see cref="RecordingRequestHandler.BuildNonInferenceResponse"/> so the turn
/// reaches the inference step; the success-path SSE body is intentionally omitted
/// because neither scenario completes a turn.
/// </summary>
public class CopilotRequestCancelErrorE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "copilot_request_cancel_error", output)
{
    private CopilotClient CreateClientWith(CopilotRequestHandler handler) =>
        Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(),
            RequestHandler = handler,
        });

    [Fact]
    public async Task Reports_A_Thrown_Callback_Error_Instead_Of_Hanging()
    {
        var handler = new ThrowingRequestHandler();
        await using var client = CreateClientWith(handler);
        await client.StartAsync();

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        try
        {
            // The callback throws on inference; the turn surfaces an error (or
            // completes without an assistant message) rather than hanging.
            await Record.ExceptionAsync(() =>
                session.SendAndWaitAsync(new MessageOptions { Prompt = "Say OK." }));
        }
        finally
        {
            await session.DisposeAsync();
        }

        Assert.True(handler.InferenceAttempts > 0, "expected the inference callback to be reached and raise");
    }

    [Fact]
    public async Task Observes_Runtime_Cancellation_Of_An_In_Flight_Inference_Request()
    {
        var handler = new CancellingRequestHandler();
        await using var client = CreateClientWith(handler);
        await client.StartAsync();

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        try
        {
            await session.SendAsync(new MessageOptions { Prompt = "Say OK." });
            await WaitForAsync(() => handler.InferenceEntered, TimeSpan.FromSeconds(60));
            await session.AbortAsync();
            await WaitForAsync(() => handler.SawAbort, TimeSpan.FromSeconds(30));
        }
        finally
        {
            await session.DisposeAsync();
        }

        Assert.True(handler.InferenceEntered, "expected the inference callback to be entered");
        Assert.True(handler.SawAbort, "expected the callback to observe runtime cancellation");
    }

    private static async Task WaitForAsync(Func<bool> predicate, TimeSpan timeout)
    {
        var deadline = DateTime.UtcNow + timeout;
        while (!predicate())
        {
            if (DateTime.UtcNow > deadline)
            {
                throw new TimeoutException("WaitForAsync timed out");
            }

            await Task.Delay(50);
        }
    }
}

/// <summary>Throws from every inference request to exercise the error-reporting path.</summary>
internal sealed class ThrowingRequestHandler : CopilotRequestHandler
{
    private int _inferenceAttempts;

    public int InferenceAttempts => Volatile.Read(ref _inferenceAttempts);

    protected override Task<HttpResponseMessage> SendRequestAsync(HttpRequestMessage request, CopilotRequestContext ctx)
    {
        var url = request.RequestUri!.ToString();
        if (!RecordingRequestHandler.IsInferenceUrl(url))
        {
            return Task.FromResult(RecordingRequestHandler.BuildNonInferenceResponse(url));
        }

        Interlocked.Increment(ref _inferenceAttempts);
        throw new InvalidOperationException("synthetic-callback-transport-failure");
    }
}

/// <summary>Blocks every inference request until the runtime cancels it.</summary>
internal sealed class CancellingRequestHandler : CopilotRequestHandler
{
    private volatile bool _inferenceEntered;
    private volatile bool _sawAbort;

    public bool InferenceEntered => _inferenceEntered;

    public bool SawAbort => _sawAbort;

    protected override async Task<HttpResponseMessage> SendRequestAsync(HttpRequestMessage request, CopilotRequestContext ctx)
    {
        var url = request.RequestUri!.ToString();
        if (!RecordingRequestHandler.IsInferenceUrl(url))
        {
            return RecordingRequestHandler.BuildNonInferenceResponse(url);
        }

        _inferenceEntered = true;
        try
        {
            // Never produce a response; wait for the runtime to cancel us.
            await Task.Delay(Timeout.Infinite, ctx.CancellationToken).ConfigureAwait(false);
        }
        catch (OperationCanceledException)
        {
            _sawAbort = true;
            throw;
        }

        return RecordingRequestHandler.BuildNonInferenceResponse(url);
    }
}
