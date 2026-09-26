/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// E2E coverage for the session-scoped user-requested shell RPC methods that were previously
/// untested: shell.executeUserRequested and shell.cancelUserRequested.
/// </summary>
public class RpcShellUserRequestedE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_shell_user_requested", output)
{
    [Fact]
    public async Task Should_Execute_User_Requested_Shell_Command()
    {
        await using var session = await CreateSessionAsync();
        var marker = $"copilotusershell{Guid.NewGuid():N}";
        var requestId = $"req-{Guid.NewGuid():N}";

        var result = await session.Rpc.Shell.ExecuteUserRequestedAsync(requestId, $"echo {marker}");

        Assert.True(result.Success, $"Expected the shell command to succeed. Error: {result.Error}");
        Assert.True(result.ExitCode == 0, $"Expected exit code 0 but got {result.ExitCode}.");
        Assert.Contains(marker, result.Output, StringComparison.Ordinal);
        Assert.False(string.IsNullOrWhiteSpace(result.ToolCallId));
    }

    [Fact]
    public async Task Should_Cancel_User_Requested_Shell_Command()
    {
        await using var session = await CreateSessionAsync();

        // Cancelling an unknown request id is a clean negative: nothing is in flight to cancel.
        var missing = await session.Rpc.Shell.CancelUserRequestedAsync($"missing-{Guid.NewGuid():N}");
        Assert.False(missing.Cancelled);

        // Poll cancellation until the runtime has registered the request. This is the authoritative
        // readiness signal and avoids depending on PowerShell-specific command syntax when the
        // Windows runtime legitimately falls back to cmd.exe.
        var requestId = $"req-{Guid.NewGuid():N}";
        var executeTask = session.Rpc.Shell.ExecuteUserRequestedAsync(
            requestId,
            CreateLongRunningCommand(seconds: 60));

        try
        {
            await TestHelper.WaitForConditionAsync(
                async () => (await session.Rpc.Shell.CancelUserRequestedAsync(requestId)).Cancelled,
                timeout: TimeSpan.FromSeconds(15),
                pollInterval: TimeSpan.FromMilliseconds(100),
                timeoutMessage: "Timed out waiting for the user-requested shell command to become cancellable.");

            // The aborted execution returns a non-success result rather than hanging.
            var result = await executeTask.WaitAsync(TimeSpan.FromSeconds(30));
            Assert.False(result.Success);
        }
        finally
        {
            if (!executeTask.IsCompleted)
            {
                try { await session.Rpc.Shell.CancelUserRequestedAsync(requestId); }
                catch (Exception ex) when (ex is TimeoutException or OperationCanceledException or ObjectDisposedException)
                {
                    // Preserve the primary test failure across expected teardown races.
                }
                try { await executeTask.WaitAsync(TimeSpan.FromSeconds(30)); }
                catch (TimeoutException) { /* best-effort drain timed out */ }
                catch (OperationCanceledException) { /* cancellation completed during teardown */ }
            }
        }
    }

    private static string CreateLongRunningCommand(int seconds)
    {
        if (OperatingSystem.IsWindows())
        {
            // ping.exe is available to both PowerShell and cmd.exe and spaces loopback requests
            // roughly one second apart. The first request is immediate, hence seconds + 1.
            return $"ping.exe -n {seconds + 1} 127.0.0.1";
        }

        return $"sleep {seconds}";
    }
}
