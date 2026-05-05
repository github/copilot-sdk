/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.ComponentModel;
using GitHub.Copilot.SDK.Test.Harness;
using Microsoft.Extensions.AI;
using Xunit;
using Xunit.Abstractions;
using RpcPermissionDecisionApproveOnce = GitHub.Copilot.SDK.Rpc.PermissionDecisionApproveOnce;

namespace GitHub.Copilot.SDK.Test.E2E;

public class PendingWorkResumeE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "pending_work_resume", output)
{
    private static readonly TimeSpan PendingWorkTimeout = TimeSpan.FromSeconds(60);
    private const string SharedToken = "pending-work-resume-shared-token";

    [Fact]
    public async Task Should_Continue_Pending_Permission_Request_After_Resume()
    {
        var originalPermissionRequest = new TaskCompletionSource<PermissionRequest>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseOriginalPermission = new TaskCompletionSource<PermissionRequestResult>(TaskCreationOptions.RunContinuationsAsynchronously);
        var resumedToolInvoked = false;

        await using var server = Ctx.CreateClient(useStdio: false, options: new CopilotClientOptions { TcpConnectionToken = SharedToken });
        await server.StartAsync();
        var cliUrl = GetCliUrl(server);

        using var suspendedClient = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = SharedToken });
        var session1 = await suspendedClient.CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(ResumePermissionTool, "resume_permission_tool")],
            OnPermissionRequest = (request, _) =>
            {
                originalPermissionRequest.TrySetResult(request);
                return releaseOriginalPermission.Task;
            },
        });
        var sessionId = session1.SessionId;

        try
        {
            var permissionRequested = TestHelper.GetNextEventOfTypeAsync<PermissionRequestedEvent>(session1, PendingWorkTimeout);

            await session1.SendAsync(new MessageOptions
            {
                Prompt = "Use resume_permission_tool with value 'alpha', then reply with the result.",
            });

            var initialRequest = await originalPermissionRequest.Task.WaitAsync(PendingWorkTimeout);
            var permissionEvent = await permissionRequested;
            Assert.IsType<PermissionRequestCustomTool>(initialRequest);

            await suspendedClient.ForceStopAsync();

            await using var resumedTcpClient = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = SharedToken });
            var session2 = await resumedTcpClient.ResumeSessionAsync(sessionId, new ResumeSessionConfig
            {
                ContinuePendingWork = true,
                OnPermissionRequest = (_, _) => Task.FromResult(new PermissionRequestResult
                {
                    Kind = PermissionRequestResultKind.NoResult
                }),
                Tools =
                [
                    AIFunctionFactory.Create(
                        ([Description("Value to transform")] string value) =>
                        {
                            resumedToolInvoked = true;
                            return $"PERMISSION_RESUMED_{value.ToUpperInvariant()}";
                        },
                        "resume_permission_tool")
                ],
            });

            var permissionResult = await session2.Rpc.Permissions.HandlePendingPermissionRequestAsync(
                permissionEvent.Data.RequestId,
                new RpcPermissionDecisionApproveOnce());
            Assert.True(permissionResult.Success);

            var answer = await TestHelper.GetFinalAssistantMessageAsync(session2, PendingWorkTimeout);

            Assert.True(resumedToolInvoked);
            Assert.Contains("PERMISSION_RESUMED_ALPHA", answer?.Data.Content ?? string.Empty);

            await session2.DisposeAsync();
            await resumedTcpClient.ForceStopAsync();
        }
        finally
        {
            releaseOriginalPermission.TrySetResult(new PermissionRequestResult
            {
                Kind = PermissionRequestResultKind.UserNotAvailable,
            });
        }

        [Description("Transforms a value after permission is granted")]
        static string ResumePermissionTool([Description("Value to transform")] string value) =>
            $"ORIGINAL_SHOULD_NOT_RUN_{value}";
    }

    [Fact]
    public async Task Should_Continue_Pending_External_Tool_Request_After_Resume()
    {
        var originalToolStarted = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseOriginalTool = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);

        await using var server = Ctx.CreateClient(useStdio: false, options: new CopilotClientOptions { TcpConnectionToken = SharedToken });
        await server.StartAsync();
        var cliUrl = GetCliUrl(server);

        using var suspendedClient = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = SharedToken });
        var session1 = await suspendedClient.CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(BlockingExternalTool, "resume_external_tool")],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        var sessionId = session1.SessionId;

        try
        {
            var toolRequested = WaitForExternalToolRequestAsync(session1, "resume_external_tool");

            await session1.SendAsync(new MessageOptions
            {
                Prompt = "Use resume_external_tool with value 'beta', then reply with the result.",
            });

            var toolEvent = await toolRequested;
            Assert.Equal("beta", await originalToolStarted.Task.WaitAsync(PendingWorkTimeout));
            await suspendedClient.ForceStopAsync();

            await using var resumedClient = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = SharedToken });
            var session2 = await resumedClient.ResumeSessionAsync(sessionId, new ResumeSessionConfig
            {
                ContinuePendingWork = true,
                OnPermissionRequest = PermissionHandler.ApproveAll,
            });

            var toolResult = await session2.Rpc.Tools.HandlePendingToolCallAsync(
                toolEvent.Data.RequestId,
                result: "EXTERNAL_RESUMED_BETA");
            Assert.True(toolResult.Success);

            var answer = await TestHelper.GetFinalAssistantMessageAsync(session2, PendingWorkTimeout);

            Assert.Contains("EXTERNAL_RESUMED_BETA", answer?.Data.Content ?? string.Empty);

            await session2.DisposeAsync();
            await resumedClient.ForceStopAsync();
        }
        finally
        {
            releaseOriginalTool.TrySetResult("ORIGINAL_SHOULD_NOT_WIN");
        }

        [Description("Looks up a value after resumption")]
        async Task<string> BlockingExternalTool([Description("Value to look up")] string value)
        {
            originalToolStarted.TrySetResult(value);
            return await releaseOriginalTool.Task;
        }
    }

    [Fact]
    public async Task Should_Keep_Pending_External_Tool_Handleable_On_Warm_Resume_When_ContinuePendingWork_Is_False()
    {
        var originalToolStarted = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseOriginalTool = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var invocationCount = 0;

        await using var server = Ctx.CreateClient(useStdio: false, options: new CopilotClientOptions { TcpConnectionToken = SharedToken });
        await server.StartAsync();
        var cliUrl = GetCliUrl(server);

        using var suspendedClient = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = SharedToken });
        var session1 = await suspendedClient.CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(BlockingExternalTool, "resume_external_tool")],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        var sessionId = session1.SessionId;

        try
        {
            var toolRequested = WaitForExternalToolRequestAsync(session1, "resume_external_tool");

            await session1.SendAsync(new MessageOptions
            {
                Prompt = "Use resume_external_tool with value 'beta', then reply with the result.",
            });

            var toolEvent = await toolRequested;
            Assert.Equal("beta", await originalToolStarted.Task.WaitAsync(PendingWorkTimeout));

            await suspendedClient.ForceStopAsync();

            await using var resumedClient = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = SharedToken });
            var session2 = await resumedClient.ResumeSessionAsync(sessionId, new ResumeSessionConfig
            {
                ContinuePendingWork = false,
                OnPermissionRequest = PermissionHandler.ApproveAll,
            });

            var resumeEvent = await GetSingleResumeEventAsync(session2);
            Assert.Equal(false, resumeEvent.Data.ContinuePendingWork);
            Assert.Equal(true, resumeEvent.Data.SessionWasActive);

            var resumedResult = await session2.Rpc.Tools.HandlePendingToolCallAsync(
                toolEvent.Data.RequestId,
                result: "EXTERNAL_RESUMED_BETA");
            Assert.True(resumedResult.Success);

            // continuePendingWork=false may interrupt agent continuation before this response,
            // but the pending call should still accept an explicit completion.
            Assert.Equal(1, invocationCount);

            await session2.DisposeAsync();
            await resumedClient.ForceStopAsync();
        }
        finally
        {
            releaseOriginalTool.TrySetResult("ORIGINAL_SHOULD_NOT_WIN");
        }

        [Description("Looks up a value after resumption")]
        async Task<string> BlockingExternalTool([Description("Value to look up")] string value)
        {
            Interlocked.Increment(ref invocationCount);
            originalToolStarted.TrySetResult(value);
            return await releaseOriginalTool.Task;
        }
    }

    [Fact]
    public async Task Should_Continue_Parallel_Pending_External_Tool_Requests_After_Resume()
    {
        var originalToolAStarted = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var originalToolBStarted = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseOriginalToolA = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseOriginalToolB = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);

        await using var server = Ctx.CreateClient(useStdio: false, options: new CopilotClientOptions { TcpConnectionToken = SharedToken });
        await server.StartAsync();
        var cliUrl = GetCliUrl(server);

        using var suspendedClient = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = SharedToken });
        var session1 = await suspendedClient.CreateSessionAsync(new SessionConfig
        {
            Tools =
            [
                AIFunctionFactory.Create(BlockingToolA, "pending_lookup_a"),
                AIFunctionFactory.Create(BlockingToolB, "pending_lookup_b"),
            ],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        var sessionId = session1.SessionId;

        try
        {
            var toolRequests = WaitForExternalToolRequestsAsync(session1, ["pending_lookup_a", "pending_lookup_b"]);

            await session1.SendAsync(new MessageOptions
            {
                Prompt = "Call pending_lookup_a with value 'alpha' and pending_lookup_b with value 'beta', then reply with both results.",
            });

            var toolEvents = await toolRequests;
            await Task.WhenAll(
                originalToolAStarted.Task,
                originalToolBStarted.Task).WaitAsync(PendingWorkTimeout);
            Assert.Equal("alpha", await originalToolAStarted.Task);
            Assert.Equal("beta", await originalToolBStarted.Task);

            await suspendedClient.ForceStopAsync();

            await using var resumedClient = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = SharedToken });
            var session2 = await resumedClient.ResumeSessionAsync(sessionId, new ResumeSessionConfig
            {
                ContinuePendingWork = true,
                OnPermissionRequest = PermissionHandler.ApproveAll,
            });

            var toolA = toolEvents["pending_lookup_a"];
            var toolB = toolEvents["pending_lookup_b"];
            var resultB = await session2.Rpc.Tools.HandlePendingToolCallAsync(
                toolB.Data.RequestId,
                result: "PARALLEL_B_BETA");
            Assert.True(resultB.Success);
            var resultA = await session2.Rpc.Tools.HandlePendingToolCallAsync(
                toolA.Data.RequestId,
                result: "PARALLEL_A_ALPHA");
            Assert.True(resultA.Success);

            await session2.DisposeAsync();
            await resumedClient.ForceStopAsync();
        }
        finally
        {
            releaseOriginalToolA.TrySetResult("ORIGINAL_A_SHOULD_NOT_WIN");
            releaseOriginalToolB.TrySetResult("ORIGINAL_B_SHOULD_NOT_WIN");
        }

        [Description("Looks up the first value after resumption")]
        async Task<string> BlockingToolA([Description("Value to look up")] string value)
        {
            originalToolAStarted.TrySetResult(value);
            return await releaseOriginalToolA.Task;
        }

        [Description("Looks up the second value after resumption")]
        async Task<string> BlockingToolB([Description("Value to look up")] string value)
        {
            originalToolBStarted.TrySetResult(value);
            return await releaseOriginalToolB.Task;
        }
    }

    [Fact]
    public async Task Should_Resume_Successfully_When_No_Pending_Work_Exists()
    {
        await using var server = Ctx.CreateClient(useStdio: false, options: new CopilotClientOptions { TcpConnectionToken = SharedToken });
        await server.StartAsync();
        var cliUrl = GetCliUrl(server);

        string sessionId;
        await using (var firstClient = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = SharedToken }))
        {
            var firstSession = await firstClient.CreateSessionAsync(new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
            });
            sessionId = firstSession.SessionId;

            var firstAnswer = await firstSession.SendAndWaitAsync(new MessageOptions { Prompt = "Reply with exactly: NO_PENDING_TURN_ONE" });
            Assert.Contains("NO_PENDING_TURN_ONE", firstAnswer?.Data.Content ?? string.Empty);

            await firstSession.DisposeAsync();
        }

        await using var resumedClient = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = SharedToken });
        var resumedSession = await resumedClient.ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            ContinuePendingWork = true,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        // Resuming with ContinuePendingWork=true on a session whose previous turn already
        // completed must be a no-op for pending work and must leave the session usable.
        var followUp = await resumedSession.SendAndWaitAsync(new MessageOptions { Prompt = "Reply with exactly: NO_PENDING_TURN_TWO" });

        Assert.Contains("NO_PENDING_TURN_TWO", followUp?.Data.Content ?? string.Empty);

        await resumedSession.DisposeAsync();
    }

    [Fact]
    public async Task Should_Report_ContinuePendingWork_True_In_Resume_Event()
    {
        await using var server = Ctx.CreateClient(useStdio: false, options: new CopilotClientOptions { TcpConnectionToken = SharedToken });
        await server.StartAsync();
        var cliUrl = GetCliUrl(server);

        string sessionId;
        await using (var firstClient = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = SharedToken }))
        {
            var firstSession = await firstClient.CreateSessionAsync(new SessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
            });
            sessionId = firstSession.SessionId;

            var firstAnswer = await firstSession.SendAndWaitAsync(new MessageOptions
            {
                Prompt = "Reply with exactly: CONTINUE_PENDING_WORK_TRUE_TURN_ONE",
            });
            Assert.Contains("CONTINUE_PENDING_WORK_TRUE_TURN_ONE", firstAnswer?.Data.Content ?? string.Empty);

            await firstSession.DisposeAsync();
        }

        await using var resumedClient = Ctx.CreateClient(options: new CopilotClientOptions { CliUrl = cliUrl, TcpConnectionToken = SharedToken });
        var resumedSession = await resumedClient.ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            ContinuePendingWork = true,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var resumeEvent = await GetSingleResumeEventAsync(resumedSession);
        Assert.Equal(true, resumeEvent.Data.ContinuePendingWork);
        Assert.Equal((bool?)false, resumeEvent.Data.SessionWasActive);

        var followUp = await resumedSession.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with exactly: CONTINUE_PENDING_WORK_TRUE_TURN_TWO",
        });

        Assert.Contains("CONTINUE_PENDING_WORK_TRUE_TURN_TWO", followUp?.Data.Content ?? string.Empty);

        await resumedSession.DisposeAsync();
    }

    private static async Task<ExternalToolRequestedEvent> WaitForExternalToolRequestAsync(
        CopilotSession session,
        string toolName)
    {
        var requests = await WaitForExternalToolRequestsAsync(session, [toolName]);
        return requests[toolName];
    }

    private static async Task<Dictionary<string, ExternalToolRequestedEvent>> WaitForExternalToolRequestsAsync(
        CopilotSession session,
        IReadOnlyCollection<string> toolNames)
    {
        var expected = toolNames.ToHashSet(StringComparer.Ordinal);
        var seen = new Dictionary<string, ExternalToolRequestedEvent>(StringComparer.Ordinal);
        var tcs = new TaskCompletionSource<Dictionary<string, ExternalToolRequestedEvent>>(
            TaskCreationOptions.RunContinuationsAsynchronously);
        using var cts = new CancellationTokenSource(PendingWorkTimeout);

        using var subscription = session.On(evt =>
        {
            if (evt is ExternalToolRequestedEvent toolEvent && expected.Contains(toolEvent.Data.ToolName))
            {
                seen[toolEvent.Data.ToolName] = toolEvent;
                if (seen.Count == expected.Count)
                {
                    tcs.TrySetResult(new Dictionary<string, ExternalToolRequestedEvent>(seen, StringComparer.Ordinal));
                }
            }
            else if (evt is SessionErrorEvent error)
            {
                tcs.TrySetException(new Exception(error.Data.Message ?? "session error"));
            }
        });

        using var registration = cts.Token.Register(() => tcs.TrySetException(
            new TimeoutException($"Timeout waiting for external tool request(s): {string.Join(", ", expected)}")));

        return await tcs.Task;
    }

    private static string GetCliUrl(CopilotClient client)
    {
        var port = client.ActualPort
            ?? throw new InvalidOperationException("Expected the test server to be listening on a TCP port.");
        return $"localhost:{port}";
    }

    private static async Task<SessionResumeEvent> GetSingleResumeEventAsync(CopilotSession session)
    {
        var messages = await session.GetMessagesAsync();
        return Assert.Single(messages.OfType<SessionResumeEvent>());
    }
}
