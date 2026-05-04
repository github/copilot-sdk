/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.ComponentModel;
using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Extensions.AI;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

/// <summary>
/// E2E coverage for the <c>session.suspend</c> RPC. Suspend is a graceful shutdown
/// counterpart to <see cref="CopilotSession.AbortAsync"/>: it (1) cancels the current
/// processing turn, (2) cancels all pending permission requests (resolving them with a
/// "cancelled" outcome at the runtime), (3) rejects all pending external tool requests,
/// (4) drains any in-flight notification turns, and (5) flushes pending writes to disk
/// before the RPC returns. After suspend, the session has no pending work and the
/// conversation log is durably persisted, so a subsequent
/// <see cref="CopilotClient.ResumeSessionAsync"/> on the same session id observes a
/// consistent state.
///
/// Suspend is NOT a handoff for pending work — pending permissions/tools are cancelled
/// rather than preserved. Tests that need to hand pending work to a new client should
/// use <see cref="CopilotClient.ForceStopAsync"/> with
/// <see cref="ResumeSessionConfig.ContinuePendingWork"/> instead (see
/// <c>PendingWorkResumeE2ETests</c>).
/// </summary>
public class SuspendE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "suspend", output)
{
    private static readonly TimeSpan SuspendTimeout = TimeSpan.FromSeconds(60);

    [Fact]
    public async Task Should_Suspend_Idle_Session_Without_Throwing()
    {
        var session = await CreateSessionAsync();

        // Run a short turn so the session has some persisted state, then suspend.
        await session.SendAndWaitAsync(new MessageOptions { Prompt = "Reply with: SUSPEND_IDLE_OK" });

        // Suspend on an idle session must succeed (no current processing to cancel,
        // notification turns already drained, but pending writes still get flushed).
        await session.Rpc.SuspendAsync().WaitAsync(SuspendTimeout);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Allow_Resume_And_Continue_Conversation_After_Suspend()
    {
        const string sharedToken = "suspend-shared-token";
        await using var server = Ctx.CreateClient(useStdio: false, options: new CopilotClientOptions { TcpConnectionToken = sharedToken });
        await server.StartAsync();
        var cliUrl = GetCliUrl(server);

        string sessionId;
        await using (var client1 = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = sharedToken }))
        {
            var session1 = await client1.CreateSessionAsync(new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
            });
            sessionId = session1.SessionId;

            await session1.SendAndWaitAsync(new MessageOptions
            {
                Prompt = "Remember the magic word: SUSPENSE. Reply with: SUSPEND_TURN_ONE",
            });

            // Graceful suspend rather than ForceStopAsync — must drain and flush state
            // before the client tears down so the next session sees a consistent log.
            await session1.Rpc.SuspendAsync().WaitAsync(SuspendTimeout);
            await session1.DisposeAsync();
        }

        // A different client should be able to pick the session back up. The previous
        // turn was completed before suspend, so there is no pending work to continue.
        await using var client2 = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = sharedToken });
        var session2 = await client2.ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var followUp = await session2.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "What was the magic word I asked you to remember? Reply with just the word.",
        });
        Assert.Contains("SUSPENSE", followUp?.Data.Content ?? string.Empty, StringComparison.OrdinalIgnoreCase);

        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Should_Cancel_Pending_Permission_Request_When_Suspending()
    {
        // Per the runtime impl, suspend resolves all pending permission requests with
        // a "cancelled" outcome on the runtime side and clears them. The SDK-side
        // permission handler task is left dangling (the runtime no longer awaits it),
        // and the underlying tool function is never invoked because the cancelled
        // permission means the runtime never grants execution.
        var permissionHandlerEntered = new TaskCompletionSource<PermissionRequest>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releasePermissionHandler = new TaskCompletionSource<PermissionRequestResult>(TaskCreationOptions.RunContinuationsAsynchronously);
        var toolInvoked = false;

        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(SuspendCancelPermissionTool, "suspend_cancel_permission_tool")],
            OnPermissionRequest = (request, _) =>
            {
                permissionHandlerEntered.TrySetResult(request);
                return releasePermissionHandler.Task;
            },
        });

        try
        {
            // Fire and forget — the SDK send task may complete (with whatever final
            // assistant message the runtime emits after cancellation) or remain pending
            // until the client connection drops. We don't depend on a specific outcome.
            _ = session.SendAsync(new MessageOptions
            {
                Prompt = "Use suspend_cancel_permission_tool with value 'omega', then reply with the result.",
            });

            var requestObserved = await permissionHandlerEntered.Task.WaitAsync(SuspendTimeout);
            Assert.IsType<PermissionRequestCustomTool>(requestObserved);

            // Suspend must complete promptly — it cancels the in-flight pending
            // permission request (resolving it as "cancelled" inside the runtime),
            // drains notification turns, and flushes pending writes to disk. The
            // runtime resolves the cancelled permission *before* it would have invoked
            // the tool, so by the time SuspendAsync returns (after the drain), the
            // tool function is guaranteed never to have been invoked — no Task.Delay
            // probe is needed.
            await session.Rpc.SuspendAsync().WaitAsync(SuspendTimeout);

            Assert.False(toolInvoked,
                "Tool should not have been invoked: suspend cancels the pending permission, so the runtime never grants tool execution. Suspend's drain semantics guarantee this is observable immediately after SuspendAsync returns.");
        }
        finally
        {
            // Defensive: release the dangling SDK-side handler task so it doesn't keep
            // a stray TaskCompletionSource alive after the test ends.
            releasePermissionHandler.TrySetResult(new PermissionRequestResult
            {
                Kind = PermissionRequestResultKind.UserNotAvailable,
            });
        }

        await session.DisposeAsync();

        [Description("Transforms a value (should not run when suspend cancels permission)")]
        string SuspendCancelPermissionTool([Description("Value to transform")] string value)
        {
            toolInvoked = true;
            return $"SHOULD_NOT_RUN_{value}";
        }
    }

    [Fact]
    public async Task Should_Reject_Pending_External_Tool_When_Suspending()
    {
        // Per the runtime impl, suspend rejects all pending external tool requests
        // with an Error("Session suspended") and clears them. We register the tool as
        // a local SDK tool but force it to never return so the runtime hands it back
        // out as an "external" pending tool request that the test can observe.
        var toolStarted = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseTool = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var externalToolRequested = new TaskCompletionSource<ExternalToolRequestedEvent>(TaskCreationOptions.RunContinuationsAsynchronously);

        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(BlockingTool, "suspend_reject_external_tool")],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        using var subscription = session.On(evt =>
        {
            if (evt is ExternalToolRequestedEvent ext && ext.Data.ToolName == "suspend_reject_external_tool")
            {
                externalToolRequested.TrySetResult(ext);
            }
        });

        try
        {
            // Fire-and-forget the prompt — the SDK send task may complete with an error
            // or remain pending; we don't depend on a specific outcome.
            _ = session.SendAsync(new MessageOptions
            {
                Prompt = "Use suspend_reject_external_tool with value 'sigma', then reply with the result.",
            });

            // Wait for the tool to start executing (blocks on releaseTool).
            Assert.Equal("sigma", await toolStarted.Task.WaitAsync(SuspendTimeout));

            // Suspend must complete promptly — it rejects the pending external tool
            // with an Error("Session suspended"), drains notification turns, and
            // flushes pending writes.
            await session.Rpc.SuspendAsync().WaitAsync(SuspendTimeout);
        }
        finally
        {
            // Defensive: release the dangling SDK-side tool function so its Task
            // doesn't outlive the test.
            releaseTool.TrySetResult("RELEASED_AFTER_SUSPEND");
        }

        await session.DisposeAsync();

        [Description("Looks up a value externally")]
        async Task<string> BlockingTool([Description("Value to look up")] string value)
        {
            toolStarted.TrySetResult(value);
            return await releaseTool.Task;
        }
    }

    private static string GetCliUrl(CopilotClient client)
    {
        var port = client.ActualPort
            ?? throw new InvalidOperationException("Expected the test server to be listening on a TCP port.");
        return $"localhost:{port}";
    }
}
