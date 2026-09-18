/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.AI;
using System.ComponentModel;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ProductionUsageLifecycleRecoveryE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ProductionUsageE2ETestBase(fixture, "production_usage_lifecycle_recovery", output)
{
    private static readonly TimeSpan LifecycleTimeout = TimeSpan.FromSeconds(60);

    [Fact]
    public async Task Should_Abort_Active_App_Turn_And_Remain_Usable()
    {
        var toolStarted = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseTool = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            Streaming = true,
            Tools = [AIFunctionFactory.Create(BlockingLookup, "app_blocking_lookup")],
        });

        _ = session.SendAsync(new MessageOptions
        {
            Prompt = "Call app_blocking_lookup with key 'abort', then reply with the result.",
            DisplayPrompt = "Run cancellable app lookup",
            Source = MessageSource.Agent("production-client"),
        });

        Assert.Equal("abort", await toolStarted.Task.WaitAsync(LifecycleTimeout));
        await session.AbortAsync();
        releaseTool.TrySetResult("APP_ABORTED_TOOL_RESULT");

        var recovery = new TaskCompletionSource<AssistantMessageEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var recoverySubscription = session.On<AssistantMessageEvent>(message =>
        {
            if (message.Data.Content?.Contains("APP_ABORT_RECOVERY_OK", StringComparison.Ordinal) == true)
            {
                recovery.TrySetResult(message);
            }
        });
        await session.SendAsync(new MessageOptions
        {
            Prompt = "Reply with exactly APP_ABORT_RECOVERY_OK.",
            DisplayPrompt = "Verify app session recovery",
            Source = MessageSource.Agent("production-client"),
        });
        Assert.Contains(
            "APP_ABORT_RECOVERY_OK",
            (await recovery.Task.WaitAsync(LifecycleTimeout)).Data.Content ?? string.Empty,
            StringComparison.Ordinal);

        [Description("Blocks an app-owned lookup until released")]
        async Task<string> BlockingLookup(
            [Description("Lookup key")] string key,
            CancellationToken cancellationToken)
        {
            toolStarted.TrySetResult(key);
            return await releaseTool.Task.WaitAsync(Timeout.InfiniteTimeSpan, cancellationToken);
        }
    }

    [Fact]
    public async Task Should_Suspend_Disconnect_And_Resume_App_State_Without_Delete()
    {
        const string connectionToken = "production-client-lifecycle-token";
        await using var server = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForTcp(connectionToken: connectionToken),
        });
        await server.StartAsync();
        var cliUrl = $"localhost:{server.RuntimePort}";

        string sessionId;
        await using (var firstClient = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(cliUrl, connectionToken: connectionToken),
        }))
        {
            var firstSession = await Ctx.CreateSessionAsync(firstClient, new SessionConfig
            {
                Streaming = true,
                OnPermissionRequest = PermissionHandler.ApproveAll,
            });
            sessionId = firstSession.SessionId;
            var initialized = await firstSession.SendAndWaitAsync(new MessageOptions
            {
                Prompt = "Remember APP_LIFECYCLE_MEMORY and reply with exactly APP_LIFECYCLE_INITIALIZED.",
                Source = MessageSource.Agent("production-client"),
            });
            Assert.Contains("APP_LIFECYCLE_INITIALIZED", initialized?.Data.Content ?? string.Empty, StringComparison.Ordinal);

            await firstSession.Rpc.SuspendAsync();
            await firstSession.DisposeAsync();
            await firstClient.ForceStopAsync();
        }

        await using var secondClient = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForUri(cliUrl, connectionToken: connectionToken),
        });
        await using var resumed = await Ctx.ResumeSessionAsync(secondClient, sessionId, new ResumeSessionConfig
        {
            Streaming = true,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var response = await resumed.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with exactly the app lifecycle memory value from the earlier turn.",
            Source = MessageSource.Agent("production-client"),
        });
        Assert.Contains("APP_LIFECYCLE_MEMORY", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);
        Assert.Contains((await resumed.GetEventsAsync()).OfType<SessionResumeEvent>(), _ => true);
    }

    [Fact]
    public async Task Should_Classify_Delete_Not_Found_For_App_Cleanup()
    {
        var (cliPath, capturePath) = await ProductionUsageTestCli.CreateAsync(Ctx);
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(
                path: cliPath,
                args: ["--capture-file", capturePath, "--behavior", "delete-not-found"]),
            UseLoggedInUser = false,
        });

        const string missingId = "missing-production-client-session";
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(() => client.DeleteSessionAsync(missingId));
        Assert.Equal(
            $"Failed to delete session {missingId}: Session file not found",
            exception.Message);

        var requests = await ProductionUsageTestCli.ReadRequestsAsync(capturePath);
        var delete = Assert.Single(requests, request => request.GetProperty("method").GetString() == "session.delete");
        var parameters = delete.GetProperty("params");
        var request = parameters.ValueKind == JsonValueKind.Array ? parameters[0] : parameters;
        Assert.Equal(missingId, request.GetProperty("sessionId").GetString());
    }

    [Fact]
    public async Task Should_Allow_Caller_Retry_After_Preacceptance_Session_Not_Found()
    {
        var (cliPath, capturePath) = await ProductionUsageTestCli.CreateAsync(Ctx);
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(
                path: cliPath,
                args: ["--capture-file", capturePath, "--behavior", "resume-not-found-once"]),
            UseLoggedInUser = false,
        });

        const string sessionId = "production-client-retry-session";
        var first = await Assert.ThrowsAnyAsync<Exception>(() =>
            Ctx.ResumeSessionAsync(client, sessionId, new ResumeSessionConfig
            {
                OnPermissionRequest = PermissionHandler.ApproveAll,
            }));
        Assert.Contains("Session not found", first.ToString(), StringComparison.OrdinalIgnoreCase);

        await using var resumed = await Ctx.ResumeSessionAsync(client, sessionId, new ResumeSessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        Assert.Equal(sessionId, resumed.SessionId);

        var requests = await ProductionUsageTestCli.ReadRequestsAsync(capturePath);
        Assert.Equal(2, requests.Count(request => request.GetProperty("method").GetString() == "session.resume"));
    }
}
