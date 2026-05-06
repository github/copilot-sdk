/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

/// <summary>
/// Targeted edge-case tests for the shell RPC API (<c>shell.exec</c>, <c>shell.kill</c>).
/// These tests close several runtime branches that the basic exec/kill tests miss:
/// timeout-triggered SIGTERM, command-not-found error path, kill on unknown processId,
/// kill with terminating signals, kill with an invalid signal, and the custom-cwd path.
/// All assertions are based on observable side effects (file existence, process gone) so
/// they remain deterministic without relying on streamed shell.output / shell.exit RPC
/// notifications which the SDK does not surface as session events.
/// </summary>
public class RpcShellEdgeCaseE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_shell_edge_cases", output)
{
    [Fact]
    public async Task Shell_Exec_With_Timeout_Kills_Long_Running_Command()
    {
        var session = await CreateSessionAsync();
        var markerPath = Path.Join(Ctx.WorkDir, $"shell-timeout-{Guid.NewGuid():N}.txt");
        var startedPath = Path.Join(Ctx.WorkDir, $"shell-timeout-started-{Guid.NewGuid():N}.txt");

        // Sleep 30s but timeout at 200ms — runtime should SIGTERM the child before the
        // sleep completes, which means the marker file must NEVER appear within a wait
        // window comfortably greater than the timeout but well under the sleep duration.
        var command = OperatingSystem.IsWindows()
            ? $"echo started>\"{startedPath}\" & for /L %i in (1,1,2147483647) do @rem & echo should-not-exist>\"{markerPath}\""
            : $"printf 'started' > '{startedPath}'; sleep 30; printf 'should-not-exist' > '{markerPath}'";

        var result = await session.Rpc.Shell.ExecAsync(command, timeout: TimeSpan.FromMilliseconds(200));
        Assert.False(string.IsNullOrWhiteSpace(result.ProcessId));

        await TestHelper.WaitForConditionAsync(
            () => File.Exists(startedPath),
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: "Timed-out shell command did not start.");

        await AssertProcessMapCleanedUpAsync(session, result.ProcessId, "Timed-out shell command");

        Assert.False(File.Exists(markerPath), "Marker file should not exist; timeout should have killed the child before the sleep completed.");
    }

    [Fact]
    public async Task Shell_Exec_With_Custom_Cwd_Honors_Override()
    {
        var session = await CreateSessionAsync();

        var subDir = Path.Join(Ctx.WorkDir, $"shell-cwd-{Guid.NewGuid():N}");
        Directory.CreateDirectory(subDir);
        var markerPath = Path.Join(subDir, "marker.txt");
        const string marker = "shell-cwd-marker";

        // Write the marker as a path RELATIVE to cwd so we can prove the runtime used the
        // override (default cwd is Ctx.WorkDir, not subDir). If the cwd parameter is
        // ignored, the relative-path write would land in WorkDir, not subDir.
        var command = OperatingSystem.IsWindows()
            ? $"powershell -NoLogo -NoProfile -Command \"Set-Content -LiteralPath 'marker.txt' -Value '{marker}'\""
            : $"sh -c \"printf '%s' '{marker}' > marker.txt\"";

        var result = await session.Rpc.Shell.ExecAsync(command, cwd: subDir);
        Assert.False(string.IsNullOrWhiteSpace(result.ProcessId));

        await TestHelper.WaitForConditionAsync(
            async () => File.Exists(markerPath) && (await File.ReadAllTextAsync(markerPath)).Contains(marker, StringComparison.Ordinal),
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: $"Timed out waiting for shell command to write marker to '{markerPath}'.",
            transientExceptionFilter: TestHelper.IsTransientFileSystemException);
    }

    [Fact]
    public async Task Shell_Exec_With_Nonexistent_Command_Returns_ProcessId_And_Cleans_Up()
    {
        var session = await CreateSessionAsync();
        var markerPath = Path.Join(Ctx.WorkDir, $"shell-not-found-{Guid.NewGuid():N}.txt");

        // shell:true means the OS shell will print "not found" to stderr and exit 127 (POSIX)
        // or 1 (cmd.exe). Either way the runtime must accept the request, return a processId,
        // and clean up the process map so a subsequent kill returns killed:false.
        var missingCommand = "definitely-not-a-real-command-" + Guid.NewGuid().ToString("N");
        var command = OperatingSystem.IsWindows()
            ? $"{missingCommand} & echo done>\"{markerPath}\" & exit /b 1"
            : $"{missingCommand}; code=$?; printf 'done' > '{markerPath}'; exit $code";

        var result = await session.Rpc.Shell.ExecAsync(command);
        Assert.False(string.IsNullOrWhiteSpace(result.ProcessId));

        await TestHelper.WaitForConditionAsync(
            () => File.Exists(markerPath),
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: "Failed shell command did not reach its marker.");

        await AssertProcessMapCleanedUpAsync(session, result.ProcessId, "Failed shell command");
    }

    [Fact]
    public async Task Shell_Kill_Unknown_ProcessId_Returns_False()
    {
        var session = await CreateSessionAsync();

        var killResult = await session.Rpc.Shell.KillAsync($"unknown-{Guid.NewGuid():N}");

        Assert.False(killResult.Killed);
    }

    [Theory]
    [InlineData(ShellKillSignal.SIGTERM)]
    [InlineData(ShellKillSignal.SIGKILL)]
    public async Task Shell_Kill_Cleans_Up_After_Terminating_Signal(ShellKillSignal signal)
    {
        var session = await CreateSessionAsync();
        var command = OperatingSystem.IsWindows()
            ? "powershell -NoLogo -NoProfile -Command \"Start-Sleep -Seconds 60\""
            : "sleep 60";

        var execResult = await session.Rpc.Shell.ExecAsync(command);
        Assert.False(string.IsNullOrWhiteSpace(execResult.ProcessId));

        var killResult = await session.Rpc.Shell.KillAsync(execResult.ProcessId, signal);
        Assert.True(killResult.Killed);

        await AssertProcessMapCleanedUpAsync(session, execResult.ProcessId, $"Process killed with {signal}");
    }

    [Fact]
    public async Task Shell_Exec_With_Stderr_Output_Cleans_Up()
    {
        var session = await CreateSessionAsync();
        var markerPath = Path.Join(Ctx.WorkDir, $"shell-stderr-{Guid.NewGuid():N}.txt");

        // Command that writes to stderr and exits non-zero. Exercises the runtime's stderr
        // stream-flush path and cleanup-on-non-zero-exit path. The marker proves the
        // command reached the end before the single kill probe checks cleanup.
        var command = OperatingSystem.IsWindows()
            ? $"powershell -NoLogo -NoProfile -Command \"[Console]::Error.WriteLine('boom'); Set-Content -LiteralPath '{markerPath}' -Value 'done'; exit 2\""
            : $"echo boom 1>&2; printf 'done' > '{markerPath}'; exit 2";

        var result = await session.Rpc.Shell.ExecAsync(command);
        Assert.False(string.IsNullOrWhiteSpace(result.ProcessId));

        await TestHelper.WaitForConditionAsync(
            () => File.Exists(markerPath),
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: "stderr-only command did not reach its marker.");

        await AssertProcessMapCleanedUpAsync(session, result.ProcessId, "stderr-only command");
    }

    [Fact]
    public async Task Shell_Exec_With_Large_Stdout_Cleans_Up()
    {
        var session = await CreateSessionAsync();
        var markerPath = Path.Join(Ctx.WorkDir, $"shell-stdout-{Guid.NewGuid():N}.txt");

        // Print a payload large enough to exceed the runtime's 64KB chunk threshold so the
        // chunked-output path is executed. We use a single 200KB write so the runtime has to
        // emit at least 3 chunks (200KB / 64KB ≈ 4).
        var command = OperatingSystem.IsWindows()
            ? $"powershell -NoLogo -NoProfile -Command \"Write-Host ('x' * 204800); Set-Content -LiteralPath '{markerPath}' -Value 'done'\""
            : $"printf '%0.s=' $(seq 1 204800); printf 'done' > '{markerPath}'";

        var result = await session.Rpc.Shell.ExecAsync(command);
        Assert.False(string.IsNullOrWhiteSpace(result.ProcessId));

        await TestHelper.WaitForConditionAsync(
            () => File.Exists(markerPath),
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: "Large-output command did not reach its marker.");

        await AssertProcessMapCleanedUpAsync(session, result.ProcessId, "Large-output command");
    }

    private static async Task AssertProcessMapCleanedUpAsync(CopilotSession session, string processId, string scenario)
    {
        // The shell RPC surface exposes kill but not a non-mutating status API.
        // Give the runtime's close/exit handler a bounded grace period, then
        // probe exactly once; if this returns true, the assertion fails instead
        // of letting a polling kill make the test pass by cleaning up itself.
        await Task.Delay(TimeSpan.FromSeconds(1));
        var killResult = await session.Rpc.Shell.KillAsync(processId);
        Assert.False(killResult.Killed, $"{scenario} should have already exited and been removed from the runtime's process map.");
    }
}
