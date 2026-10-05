/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.ComponentModel;
using System.Diagnostics;
using System.Reflection;
using GitHub.Copilot.Test.E2E;
using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class E2ETestFixtureTests
{
    [Fact]
    public async Task Failed_Process_Launch_Reclaims_Context_Directories()
    {
        var directory = Path.Join(Path.GetTempPath(), $"copilot-failed-launch-{Guid.NewGuid():N}");
        Directory.CreateDirectory(directory);
        var homeDir = Path.Join(directory, "home");
        var workDir = Path.Join(directory, "work");
        using var process = new Process
        {
            StartInfo = new ProcessStartInfo(Path.Join(directory, "missing-proxy"))
            {
                UseShellExecute = false,
                CreateNoWindow = true,
            },
        };
        await using var proxy = new ReplayProxy();
        try
        {
            var failure = Assert.Throws<Win32Exception>(() => process.Start());
            typeof(ReplayProxy).GetField("_process", BindingFlags.Instance | BindingFlags.NonPublic)!
                .SetValue(proxy, process);
            typeof(ReplayProxy).GetField("_startupTask", BindingFlags.Instance | BindingFlags.NonPublic)!
                .SetValue(proxy, Task.FromException<string>(failure));

            var observed = await Assert.ThrowsAsync<Win32Exception>(
                () => E2ETestContext.CreateAsync(proxy, homeDir, workDir));

            Assert.Same(failure, observed);
            Assert.False(Directory.Exists(homeDir), "Failed launch retained the home directory.");
            Assert.False(Directory.Exists(workDir), "Failed launch retained the work directory.");
        }
        finally
        {
            typeof(ReplayProxy).GetField("_process", BindingFlags.Instance | BindingFlags.NonPublic)!
                .SetValue(proxy, null);
            Directory.Delete(directory, recursive: true);
        }
    }

    [Fact]
    public async Task Failed_Context_Startup_Reclaims_Proxy_And_Directories()
    {
        var directory = Path.Join(Path.GetTempPath(), $"copilot-failed-startup-{Guid.NewGuid():N}");
        Directory.CreateDirectory(directory);
        var homeDir = Path.Join(directory, "home");
        var workDir = Path.Join(directory, "work");
        var startInfo = new ProcessStartInfo("node")
        {
            Arguments = "-e \"console.log('started'); setInterval(() => {}, 1000)\"",
            UseShellExecute = false,
            RedirectStandardOutput = true,
            CreateNoWindow = true,
        };
        using var process = Process.Start(startInfo)!;
        using var observer = Process.GetProcessById(process.Id);
        await using var proxy = new ReplayProxy();
        var failure = new TimeoutException("controlled proxy startup failure");
        try
        {
            typeof(ReplayProxy).GetField("_process", BindingFlags.Instance | BindingFlags.NonPublic)!
                .SetValue(proxy, process);
            typeof(ReplayProxy).GetField("_startupTask", BindingFlags.Instance | BindingFlags.NonPublic)!
                .SetValue(proxy, Task.FromException<string>(failure));
            Assert.Equal("started", await process.StandardOutput.ReadLineAsync().WaitAsync(TimeSpan.FromSeconds(10)));

            var observed = await Assert.ThrowsAsync<TimeoutException>(
                () => E2ETestContext.CreateAsync(proxy, homeDir, workDir));

            Assert.Same(failure, observed);
            Assert.False(Directory.Exists(homeDir), "Failed startup retained the home directory.");
            Assert.False(Directory.Exists(workDir), "Failed startup retained the work directory.");
            Assert.True(observer.HasExited, "Failed startup retained the proxy process.");
        }
        finally
        {
            await proxy.StopAsync(skipWritingCache: true);
            Directory.Delete(directory, recursive: true);
        }
    }

    [Fact]
    public async Task Dispose_Before_Context_Is_Created_Does_Not_Throw()
    {
        var fixture = new E2ETestFixture();

        await fixture.DisposeAsync();
    }

    [Fact]
    public void Shared_Client_Uses_InProcess_Connection_For_InProcess_Tests()
    {
        var connection = E2ETestFixture.CreateSharedConnection(useInProcessTransport: true);

        Assert.IsType<InProcessRuntimeConnection>(connection);
    }

    [Fact]
    public void Shared_Client_Preserves_Tcp_Connection_For_OutOfProcess_Tests()
    {
        var connection = Assert.IsType<TcpRuntimeConnection>(
            E2ETestFixture.CreateSharedConnection(useInProcessTransport: false));

        Assert.Equal(E2ETestFixture.SharedTcpConnectionToken, connection.ConnectionToken);
    }

    [Fact]
    public async Task Dispose_Derived_Fixtures_Without_Initialized_Context_Does_Not_Throw()
    {
        await new MultiClientTestFixture().DisposeAsync();
        await new MultiClientCommandsElicitationFixture().DisposeAsync();
        await new ConnectionTokenTestFixture().DisposeAsync();
    }

    [Fact]
    public async Task Replay_Proxy_Starts_With_Connect_Metadata_And_Stops()
    {
        var proxy = new ReplayProxy();
        try
        {
            var url = await proxy.StartAsync();
            Assert.StartsWith("http://", url);
            Assert.False(string.IsNullOrWhiteSpace(proxy.ConnectProxyUrl));
            Assert.False(string.IsNullOrWhiteSpace(proxy.CaFilePath));
        }
        finally
        {
            await proxy.StopAsync(skipWritingCache: true);
        }
    }
}
