/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#if NET8_0_OR_GREATER
using System.Diagnostics;
using System.Globalization;
using System.Reflection;
using GitHub.Copilot.Test.E2E;
using GitHub.Copilot.Test.Harness;
using Microsoft.Extensions.Logging;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public sealed class RuntimeStartupProcessObserverTests
{
    [Fact]
    public async Task Scenario_Exit_Request_Ends_The_Retained_Runtime_Lifetime()
    {
        var directory = Path.Combine(Path.GetTempPath(), $"copilot-scenario-exit-{Guid.NewGuid():N}");
        Directory.CreateDirectory(directory);
        var scriptPath = Path.Combine(directory, "runtime.cjs");
        var capturePath = Path.Combine(directory, "capture.json");
        var pidPath = Path.Combine(directory, "runtime.pid");
        var exitPath = Path.Combine(directory, "exit");
        var script = (string)typeof(ScenarioTestingRuntimeE2ETests)
            .GetField("FakeRuntimeScript", BindingFlags.NonPublic | BindingFlags.Static)!.GetRawConstantValue()!;
        try
        {
            await File.WriteAllTextAsync(scriptPath, script);
            await using var client = new CopilotClient(new CopilotClientOptions
            {
                Connection = RuntimeConnection.ForStdio(path: "node",
                    args: [scriptPath, "--capture-file", capturePath, "--pid-file", pidPath, "--exit-file", exitPath]),
                UseLoggedInUser = false,
            });
            // Startup is a precondition for the process-lifetime assertions, not a latency contract.
            await client.StartAsync();
            using var observer = Process.GetProcessById(
                int.Parse(await File.ReadAllTextAsync(pidPath), CultureInfo.InvariantCulture));
            try
            {
                _ = observer.Handle;
                Assert.False(observer.HasExited);

                await File.WriteAllTextAsync(exitPath, string.Empty);
                await observer.WaitForExitAsync().WaitAsync(TimeSpan.FromSeconds(5));
                Assert.True(observer.HasExited);
                Assert.Equal(1, observer.ExitCode);
                var error = await Assert.ThrowsAnyAsync<Exception>(
                    () => client.PingAsync("after-fixture-exit").WaitAsync(TimeSpan.FromSeconds(5)));
                Assert.IsNotType<TimeoutException>(error);
            }
            finally
            {
                await client.ForceStopAsync();
            }
        }
        finally
        {
            Directory.Delete(directory, recursive: true);
        }
    }

    [Fact]
    public async Task OAuth_Startup_Cleanup_Disposes_Only_Its_Own_Process_Alias()
    {
        var startupMethod = typeof(McpOAuthE2ETests)
            .GetNestedType("OAuthMcpServer", BindingFlags.NonPublic)!
            .GetMethod("WaitForListeningAsync", BindingFlags.Public | BindingFlags.Static)!;
        using var process = Process.Start(new ProcessStartInfo("node")
        {
            Arguments = "-e \"console.log('started'); setInterval(() => {}, 1000)\"",
            UseShellExecute = false,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            CreateNoWindow = true,
        }) ?? throw new InvalidOperationException("Could not start the OAuth observer test process.");
        using var observer = Process.GetProcessById(process.Id);
        using var cancellation = new CancellationTokenSource();
        Task? startup = null;
        Task StartOwner() => (Task)startupMethod.Invoke(null, [process, cancellation.Token])!;
        try
        {
            _ = observer.Handle;
            Assert.Equal("started", await process.StandardOutput.ReadLineAsync().WaitAsync(TimeSpan.FromSeconds(5)));
            startup = StartOwner();
            cancellation.Cancel();
            await Assert.ThrowsAnyAsync<OperationCanceledException>(
                () => startup.WaitAsync(TimeSpan.FromSeconds(5)));
            Assert.Throws<InvalidOperationException>(() => process.HasExited);
            Assert.True(observer.HasExited, "OAuth cleanup retained the observed process.");
        }
        finally
        {
            cancellation.Cancel();
            startup ??= StartOwner();
            try
            {
                await startup;
            }
            catch (OperationCanceledException)
            {
                // Join the startup owner's teardown without signaling the observer.
            }
        }
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task Startup_Observer_Retains_The_Admitted_Process_After_It_Exits(bool useStdio)
    {
        var directory = Path.Combine(Path.GetTempPath(), $"copilot-startup-observer-{Guid.NewGuid():N}");
        Directory.CreateDirectory(directory);
        var pidPath = Path.Combine(directory, "runtime.pid");
        using var child = new Process
        {
            StartInfo = new ProcessStartInfo("node")
            {
                Arguments = "-e \"console.log('started'); process.stdin.resume()\"",
                UseShellExecute = false,
                RedirectStandardInput = true,
                RedirectStandardOutput = true,
                CreateNoWindow = true,
            },
        };
        var started = false;
        var reaped = false;
        using var observer = new RuntimeStartupProcessObserver(pidPath, useStdio);
        try
        {
            started = child.Start();
            Assert.True(started, "Could not start the observer test process.");
            Assert.Equal("started", await child.StandardOutput.ReadLineAsync().WaitAsync(TimeSpan.FromSeconds(5)));
            await File.WriteAllTextAsync(pidPath, child.Id.ToString(CultureInfo.InvariantCulture));
            KeyValuePair<string, object?>[] unrelated = [new("Method", "ping")];
            observer.Log(LogLevel.Debug, default, unrelated,
                null, static (_, _) => string.Empty);
            Assert.Throws<InvalidOperationException>(() => observer.Process);

            KeyValuePair<string, object?>[] admission = useStdio
                ? [new("Method", "connect"), new("Status", "Succeeded")]
                : [new("Line", "listening on port 12345")];
            observer.Log(LogLevel.Debug, default, admission, null, static (_, _) => string.Empty);
            var retained = observer.Process;
            Assert.False(retained.HasExited);

            child.StandardInput.Close();
            await child.WaitForExitAsync().WaitAsync(TimeSpan.FromSeconds(5));
            reaped = true;
            child.Dispose();
            Assert.True(retained.HasExited);
            // A later PID publication can identify a live foreign process, never this activation.
            await File.WriteAllTextAsync(pidPath, Environment.ProcessId.ToString(CultureInfo.InvariantCulture));
            observer.Log(LogLevel.Debug, default, admission, null, static (_, _) => string.Empty);
            Assert.Same(retained, observer.Process);
            Assert.True(observer.Process.HasExited);
        }
        finally
        {
            if (started && !reaped && !child.HasExited)
            {
                child.Kill();
                await child.WaitForExitAsync().WaitAsync(TimeSpan.FromSeconds(5));
            }
            Directory.Delete(directory, recursive: true);
        }
    }
}
#endif
