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

        // De-race an in-flight cancellation: launch a long command that first writes a marker file
        // (so we know it is genuinely running) and then sleeps. Keep the marker outside the fixture
        // workspace so Windows cleanup is not blocked by lingering process handles.
        var requestId = $"req-{Guid.NewGuid():N}";
        var markerPath = Path.Join(Path.GetTempPath(), $"shell-cancel-{Guid.NewGuid():N}.txt");
        var executeTask = session.Rpc.Shell.ExecuteUserRequestedAsync(
            requestId,
            CreateMarkerThenSleepCommand(markerPath, seconds: 60));

        try
        {
            await WaitForFileExistsAsync(markerPath);

            // The marker proves the child process reached the command body, but the runtime may not
            // yet have registered the request in its cancellable in-flight map. Poll the cancel until
            // it takes effect so the assertion is not racy. WaitForConditionAsync stops on the first
            // call that reports Cancelled, so the command is cancelled exactly once.
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
                try { await executeTask.WaitAsync(TimeSpan.FromSeconds(30)); }
                catch { /* best-effort drain so the long command does not outlive the test */ }
            }

            TryDeleteFile(markerPath);
        }
    }

    private static string CreateMarkerThenSleepCommand(string markerPath, int seconds)
    {
        // The runtime already runs the command through the platform shell (pwsh -Command "<cmd>" on
        // Windows, sh -c "<cmd>" elsewhere), so emit the script body directly instead of spawning a
        // *second* nested shell. Cancellation kills only the shell the runtime spawned; a nested
        // powershell.exe/sh would be orphaned and keep the session working directory locked, which
        // breaks fixture cleanup on Windows (manifesting as an IOException during teardown).
        if (OperatingSystem.IsWindows())
        {
            return $"Set-Content -LiteralPath '{markerPath}' -Value 'running'; Start-Sleep -Seconds {seconds}";
        }

        return $"echo running > '{markerPath}'; sleep {seconds}";
    }

    private static async Task WaitForFileExistsAsync(string path)
    {
        await TestHelper.WaitForConditionAsync(
            () => File.Exists(path),
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: $"Timed out waiting for the shell command to create '{path}'.",
            pollInterval: TimeSpan.FromMilliseconds(100));
    }

    private static void TryDeleteFile(string path)
    {
        try
        {
            if (File.Exists(path))
            {
                File.Delete(path);
            }
        }
        catch (Exception ex) when (TestHelper.IsTransientFileSystemException(ex))
        {
            // Best-effort cleanup; the OS temp directory is reclaimed independently.
        }
    }
}
