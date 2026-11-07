/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Diagnostics;
using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class StdioShutdownTests
{
    [Theory]
    [InlineData("stop")]
    [InlineData("dispose")]
    [InlineData("force")]
    [InlineData("fallback")]
    [InlineData("start-failure")]
    public async Task Owned_Stdio_Runtime_Finishes_Host_Cleanup_Before_Graceful_Stop_Returns(string operation)
    {
        var directory = Path.Combine(Path.GetTempPath(), $"copilot-shutdown-{Guid.NewGuid():N}");
        Directory.CreateDirectory(directory);
        var script = Path.Combine(E2ETestBase.FindTestHarnessDir(), "stdio-shutdown-runtime.cjs");
        var marker = Path.Combine(directory, "telemetry.jsonl");
        var pidPath = Path.Combine(directory, "runtime.pid");

        try
        {
            using var observer = operation == "start-failure"
                ? new RuntimeStartupProcessObserver(pidPath, useStdio: true)
                : null;
            await using var client = new CopilotClient(new CopilotClientOptions
            {
                Connection = RuntimeConnection.ForStdio(path: "node", args: [script, marker, operation, pidPath]),
                UseLoggedInUser = false,
                Logger = observer,
            });
            if (operation == "start-failure")
            {
                // Failed startup includes owned-process cleanup, which has its own longer budgets.
                var error = await Assert.ThrowsAsync<InvalidOperationException>(() => client.StartAsync());
                Assert.Contains("protocol version mismatch", error.Message);
                Assert.True(observer!.Process.HasExited, "Failed startup retained the runtime process.");
                Assert.False(File.Exists(marker));
                return;
            }

            await client.StartAsync().WaitAsync(TimeSpan.FromSeconds(5));
            using var process = Process.GetProcessById(
                int.Parse(await File.ReadAllTextAsync(pidPath), System.Globalization.CultureInfo.InvariantCulture));
            _ = process.Handle;
            var elapsed = Stopwatch.StartNew();

            switch (operation)
            {
                case "stop":
                    await client.StopAsync().WaitAsync(TimeSpan.FromSeconds(5));
                    break;
                case "dispose":
                    await client.DisposeAsync().AsTask().WaitAsync(TimeSpan.FromSeconds(5));
                    break;
                case "fallback":
                    // Allow shutdown RPC, graceful exit, kill/reap, and stderr drain their separate budgets.
                    await client.StopAsync().WaitAsync(TimeSpan.FromSeconds(40));
                    Assert.True(elapsed.Elapsed >= TimeSpan.FromSeconds(10),
                        "Graceful stop must wait for its exit timeout before terminating the child.");
                    break;
                default:
                    await client.ForceStopAsync().WaitAsync(TimeSpan.FromSeconds(5));
                    break;
            }

            Assert.True(process.HasExited);
            await client.DisposeAsync();
            await client.DisposeAsync();
            if (operation == "force")
            {
                Assert.False(File.Exists(marker));
            }
            else
            {
                Assert.Equal("{\"type\":\"span\"}\n", await File.ReadAllTextAsync(marker));
            }
        }
        finally
        {
            Directory.Delete(directory, recursive: true);
        }
    }
}
#endif
