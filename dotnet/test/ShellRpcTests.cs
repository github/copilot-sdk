/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class ShellRpcTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "shell_rpc", output)
{
    [Fact]
    public async Task Should_Execute_Shell_Command()
    {
        var session = await CreateSessionAsync();

        var result = await session.Rpc.Shell.ExecAsync("echo copilot-sdk-shell-rpc");

        Assert.False(string.IsNullOrWhiteSpace(result.ProcessId));
    }

    [Fact]
    public async Task Should_Kill_Shell_Process()
    {
        var session = await CreateSessionAsync();
        var command = OperatingSystem.IsWindows()
            ? "powershell -NoLogo -NoProfile -Command \"Start-Sleep -Seconds 30\""
            : "sleep 30";

        var execResult = await session.Rpc.Shell.ExecAsync(command);
        Assert.False(string.IsNullOrWhiteSpace(execResult.ProcessId));

        var killResult = await session.Rpc.Shell.KillAsync(execResult.ProcessId);

        Assert.True(killResult.Killed);
    }
}
