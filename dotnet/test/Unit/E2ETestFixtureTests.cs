/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.ComponentModel;
using System.Diagnostics;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Reflection;
using System.Text.Json;
using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.E2E;
using GitHub.Copilot.Test.Harness;
using Xunit;

namespace GitHub.Copilot.Test.Unit;

public class E2ETestFixtureTests
{
    [Theory]
    [InlineData("default", true)]
    [InlineData("stdio", true)]
    [InlineData("tcp", true)]
    [InlineData("inprocess", true)]
    [InlineData("uri", false)]
    public void Extension_Launch_Provider_Respects_Runtime_Ownership_And_Caller_Provider(string transport, bool expected)
    {
        var options = new CopilotClientOptions { Connection = ConnectionFor(transport) };

        E2ETestContext.ConfigureExtensionLaunchProvider(options, true, Path.Join("cli", "copilot-sdk"));

        Assert.Equal(expected, options.ExtensionLaunchProvider is not null);
        var provider = new CallerExtensionLaunchProvider();
        options.ExtensionLaunchProvider = provider;

        E2ETestContext.ConfigureExtensionLaunchProvider(options, true, Path.Join("cli", "copilot-sdk"));

        Assert.Same(provider, options.ExtensionLaunchProvider);
    }

    private static RuntimeConnection? ConnectionFor(string transport) => transport switch
    {
        "default" => null,
        "stdio" => RuntimeConnection.ForStdio(),
        "tcp" => RuntimeConnection.ForTcp(),
        "inprocess" => RuntimeConnection.ForInProcess(),
        "uri" => RuntimeConnection.ForUri("localhost:12345"),
        _ => throw new ArgumentException("Unknown transport.", nameof(transport)),
    };

    private sealed class CallerExtensionLaunchProvider : IExtensionLaunchProviderHandler
    {
        public Task<ExtensionLaunchProviderResolveResult> ResolveAsync(
            ExtensionLaunchProviderResolveRequest request,
            CancellationToken cancellationToken = default) =>
            throw new InvalidOperationException("Provider resolution is not part of client configuration.");
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task Clients_Keep_Environment_And_Working_Directory_Scoped_To_Their_Context(bool inProcess)
    {
        var ambientDirectory = Directory.GetCurrentDirectory();
        var ambientApiUrl = Environment.GetEnvironmentVariable("COPILOT_API_URL");
        var ambientHome = Environment.GetEnvironmentVariable("COPILOT_HOME");
        await using var first = await E2ETestContext.CreateAsync();
        await using var second = await E2ETestContext.CreateAsync();
        using var firstClient = first.CreateClient(new CopilotClientOptions
        {
            Connection = inProcess ? RuntimeConnection.ForInProcess() : RuntimeConnection.ForStdio(),
        });
        using var secondClient = second.CreateClient(new CopilotClientOptions
        {
            Connection = inProcess ? RuntimeConnection.ForInProcess() : RuntimeConnection.ForStdio(),
        });

        Assert.NotEqual(first.HomeDir, second.HomeDir);
        Assert.NotEqual(first.WorkDir, second.WorkDir);
        Assert.NotEqual(first.ProxyUrl, second.ProxyUrl);
        Assert.Equal(ambientDirectory, Directory.GetCurrentDirectory());
        Assert.Equal(ambientApiUrl, Environment.GetEnvironmentVariable("COPILOT_API_URL"));
        Assert.Equal(ambientHome, Environment.GetEnvironmentVariable("COPILOT_HOME"));
    }

    [Fact]
    public async Task InProcess_Clients_Reject_Explicit_Fixture_Environment()
    {
        await using var context = await E2ETestContext.CreateAsync();
        var error = Assert.Throws<ArgumentException>(() => context.CreateClient(
            new CopilotClientOptions { Connection = RuntimeConnection.ForInProcess() },
            environment: new Dictionary<string, string> { ["EXPLICIT_FIXTURE_ENV"] = "value" }));
        Assert.Contains("Environment", error.Message);
    }

    [Fact]
    public async Task Default_Telemetry_Endpoint_Uses_The_Owned_Proxy()
    {
        await using var context = await E2ETestContext.CreateAsync();
        using var client = new HttpClient();
        using var userRequest = new HttpRequestMessage(HttpMethod.Get, context.ProxyUrl + "/copilot_internal/user");
        userRequest.Headers.Authorization = new AuthenticationHeaderValue("Bearer", context.GetEnvironment()["GH_TOKEN"]);
        using var userResponse = await client.SendAsync(userRequest);
        Assert.Equal(HttpStatusCode.OK, userResponse.StatusCode);
        using var user = JsonDocument.Parse(await userResponse.Content.ReadAsStringAsync());
        var telemetryEndpoint = user.RootElement.GetProperty("endpoints").GetProperty("telemetry").GetString();
        Assert.Equal(context.ProxyUrl, telemetryEndpoint);
        using var content = new StringContent("{}\n");
        using var telemetryResponse = await client.PostAsync(telemetryEndpoint + "/telemetry", content);
        Assert.Equal(HttpStatusCode.NoContent, telemetryResponse.StatusCode);
        Assert.Equal(string.Empty, await telemetryResponse.Content.ReadAsStringAsync());
        Assert.Empty(await context.GetExchangesAsync());
    }

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
            // Proxy cleanup disposes its Process object; pin this independent observer first.
            Assert.NotSame(process, observer);
            _ = observer.Handle;
            Assert.False(observer.HasExited);

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

    [Theory]
    [InlineData(false, null)]
    [InlineData(true, 0)]
    [InlineData(true, 1)]
    public void Proxy_Startup_Diagnostics_Preserve_Process_State_And_Bounded_Output(bool largeOutput, int? exitCode)
    {
        var output = new ReplayProxy.StartupDiagnostics();
        output.AppendOutput("old stdout");
        output.AppendError("old stderr");
        if (largeOutput)
        {
            output.AppendOutput(new string('x', 12_000));
            output.AppendError(new string('y', 12_000));
        }
        output.AppendOutput("tsx server.ts");
        output.AppendError("proxy startup error");

        var diagnostics = output.Describe(1234, TimeSpan.FromSeconds(30), exitCode);

        Assert.Contains("PID 1234", diagnostics);
        Assert.Contains(exitCode is int code ? $"exited with code {code}" : "running", diagnostics);
        Assert.Contains("--- stdout tail ---", diagnostics);
        Assert.Contains("tsx server.ts", diagnostics);
        Assert.Contains("--- stderr tail ---", diagnostics);
        Assert.Contains("proxy startup error", diagnostics);
        if (largeOutput)
        {
            Assert.DoesNotContain("old stdout", diagnostics);
            Assert.DoesNotContain("old stderr", diagnostics);
        }
        Assert.True(diagnostics.Length < 8500, $"Diagnostic length: {diagnostics.Length}");
    }

}
