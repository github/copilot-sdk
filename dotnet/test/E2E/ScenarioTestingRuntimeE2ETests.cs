/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics;
using System.Globalization;
using System.Text.Json;
using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

#pragma warning disable GHCP001

public class ScenarioTestingRuntimeE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_runtime", output)
{
    private static readonly TimeSpan TestTimeout = TimeSpan.FromSeconds(30);

    [Fact]
    public async Task Should_Start_With_Complete_Scenario_Options_And_Extension_Launch_Provider()
    {
        var (cliPath, capturePath, pidPath) = await CreateFakeRuntimeAsync("normal");
        var scenarioHome = Path.Join(Ctx.WorkDir, "scenario-client-home");
        var pluginOne = Path.GetFullPath(Path.Join(Ctx.WorkDir, "plugins", "builtin-one"));
        var pluginTwo = Path.GetFullPath(Path.Join(Ctx.WorkDir, "plugins", "builtin-two"));
        Directory.CreateDirectory(scenarioHome);
        Directory.CreateDirectory(pluginOne);
        Directory.CreateDirectory(pluginTwo);
        var launchProvider = new RecordingExtensionLaunchProvider();

        await using var client = Ctx.CreateClient(
            options: new CopilotClientOptions
            {
                Connection = RuntimeConnection.ForStdio(
                    path: cliPath,
                    args: ["--capture-file", capturePath, "--pid-file", pidPath, "--behavior", "normal"]),
                Mode = CopilotClientMode.Empty,
                BaseDirectory = scenarioHome,
                BuiltinPluginDirectories = [pluginOne, pluginTwo],
                GitHubToken = "scenario-client-runtime-token",
                UseLoggedInUser = false,
                LogLevel = CopilotLogLevel.Debug,
                SessionIdleTimeoutSeconds = 23,
                EnableRemoteSessions = true,
                Telemetry = new TelemetryConfig
                {
                    OtlpEndpoint = "http://127.0.0.1:4318",
                    OtlpProtocol = "http/protobuf",
                    FilePath = Path.Join(Ctx.WorkDir, "scenario-client-telemetry.jsonl"),
                    ExporterType = "file",
                    SourceName = "scenario-client",
                    CaptureContent = true,
                },
                ClientInfo = new CopilotClientInfo
                {
                    ApplicationName = "scenario-client",
                    ApplicationVersion = "1.2.3",
                    IntegrationName = "copilot-sdk",
                    IntegrationVersion = "4.5.6",
                },
                ExtensionLaunchProvider = launchProvider,
            });

        await client.StartAsync();

        var launchRequest = await launchProvider.Request.Task.WaitAsync(TestTimeout);
        Assert.Equal("project:runtime-e2e", launchRequest.Id);
        Assert.Equal("runtime-e2e", launchRequest.Name);
        Assert.Equal(ExtensionSource.Project, launchRequest.Source);
        Assert.Equal(Path.GetFullPath(Path.Join(Ctx.WorkDir, "extension.mjs")), launchRequest.ModulePath);

        using var capture = await WaitForCaptureAsync(
            capturePath,
            root => root.GetProperty("clientResponses").GetArrayLength() == 1);
        var root = capture.RootElement;
        var args = root.GetProperty("args").EnumerateArray().Select(item => item.GetString()).ToArray();
        var environment = root.GetProperty("env");
        var requests = root.GetProperty("requests").EnumerateArray().ToList();

        Assert.Contains("--stdio", args);
        Assert.Contains("--remote", args);
        AssertArgumentValue(args, "--log-level", "debug");
        AssertArgumentValue(args, "--auth-token-env", "COPILOT_SDK_AUTH_TOKEN");
        AssertArgumentValue(args, "--session-idle-timeout", "23");
        Assert.Contains("--no-auto-login", args);
        Assert.Equal(scenarioHome, environment.GetProperty("COPILOT_HOME").GetString());
        Assert.Equal("scenario-client-runtime-token", environment.GetProperty("COPILOT_SDK_AUTH_TOKEN").GetString());
        Assert.Equal("true", environment.GetProperty("COPILOT_OTEL_ENABLED").GetString());
        Assert.Equal("scenario-client", environment.GetProperty("COPILOT_OTEL_SOURCE_NAME").GetString());

        Assert.Equal(
            ["connect", "registerExtensionLaunchProvider", "plugins.builtin.set"],
            requests.Select(request => request.GetProperty("method").GetString()!).ToArray());

        var connect = requests[0].GetProperty("params");
        var clientInfo = connect.GetProperty("clientInfo");
        Assert.Equal("scenario-client", clientInfo.GetProperty("editorName").GetString());
        Assert.Equal("1.2.3", clientInfo.GetProperty("editorVersion").GetString());
        Assert.Equal("copilot-sdk", clientInfo.GetProperty("extensionName").GetString());
        Assert.Equal("4.5.6", clientInfo.GetProperty("extensionVersion").GetString());

        var pluginPaths = requests[2]
            .GetProperty("params")
            .GetProperty("paths")
            .EnumerateArray()
            .Select(item => item.GetString()!)
            .ToArray();
        Assert.Equal([pluginOne, pluginTwo], pluginPaths);

        var launchResponse = root.GetProperty("clientResponses")[0].GetProperty("result").GetProperty("launch");
        Assert.Equal("node", launchResponse.GetProperty("executable").GetString());
        Assert.Equal("extension-host", launchResponse.GetProperty("args")[0].GetString());
        Assert.Equal("scenario-client", launchResponse.GetProperty("env").GetProperty("HOST_KIND").GetString());
    }

    [Fact]
    public async Task Should_Cancel_Externally_When_Startup_Handshake_Hangs()
    {
        var (cliPath, capturePath, pidPath) = await CreateFakeRuntimeAsync("hang-connect");
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(
                path: cliPath,
                args: ["--capture-file", capturePath, "--pid-file", pidPath, "--behavior", "hang-connect"]),
            UseLoggedInUser = false,
        });
        using var cancellation = new CancellationTokenSource(TimeSpan.FromMilliseconds(500));

        await Assert.ThrowsAnyAsync<OperationCanceledException>(() => client.StartAsync(cancellation.Token));

        var pid = int.Parse(await File.ReadAllTextAsync(pidPath), CultureInfo.InvariantCulture);
        await AssertProcessExitedAsync(pid);
        await client.ForceStopAsync();
    }

    [Fact]
    public async Task Should_Ping_Then_Reuse_Client_Across_Two_Sessions()
    {
        await using var client = Ctx.CreateClient();
        await client.StartAsync();

        var ping = await client.PingAsync("scenario-client-reuse");
        Assert.Equal("pong: scenario-client-reuse", ping.Message);

        string firstSessionId;
        await using (var first = await Ctx.CreateSessionAsync(client))
        {
            firstSessionId = first.SessionId;
            var response = await first.SendAndWaitAsync(new MessageOptions
            {
                Prompt = "Reply with exactly FIRST_SCENARIO_SESSION.",
            });
            Assert.Contains("FIRST_SCENARIO_SESSION", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);
        }

        await using (var second = await Ctx.CreateSessionAsync(client))
        {
            Assert.NotEqual(firstSessionId, second.SessionId);
            var response = await second.SendAndWaitAsync(new MessageOptions
            {
                Prompt = "Reply with exactly SECOND_SCENARIO_SESSION.",
            });
            Assert.Contains("SECOND_SCENARIO_SESSION", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);
        }
    }

    [Fact]
    public async Task Should_Bound_Graceful_Stop_Then_Force_Stop()
    {
        var (cliPath, capturePath, pidPath) = await CreateFakeRuntimeAsync("hang-detach");
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(
                path: cliPath,
                args: ["--capture-file", capturePath, "--pid-file", pidPath, "--behavior", "hang-detach"]),
            UseLoggedInUser = false,
        });
        await using var session = await Ctx.CreateSessionAsync(client);

        var stopTask = client.StopAsync();
        var completed = await Task.WhenAny(stopTask, Task.Delay(TimeSpan.FromMilliseconds(500)));
        Assert.NotSame(stopTask, completed);

        await client.ForceStopAsync();
        await stopTask.WaitAsync(TestTimeout);

        var pid = int.Parse(await File.ReadAllTextAsync(pidPath), CultureInfo.InvariantCulture);
        await AssertProcessExitedAsync(pid);
    }

    [Fact]
    public async Task Should_Fail_Fast_After_Transport_Failure()
    {
        var (cliPath, capturePath, pidPath) = await CreateFakeRuntimeAsync("exit-after-create");
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(
                path: cliPath,
                args: ["--capture-file", capturePath, "--pid-file", pidPath, "--behavior", "exit-after-create"]),
            UseLoggedInUser = false,
        });
        await using var session = await Ctx.CreateSessionAsync(client);

        var pid = int.Parse(await File.ReadAllTextAsync(pidPath), CultureInfo.InvariantCulture);
        await AssertProcessExitedAsync(pid);

        Exception sendException;
        Exception pingException;
        try
        {
            sendException = await Assert.ThrowsAnyAsync<Exception>(
                () => session.SendAsync(new MessageOptions { Prompt = "This transport is already gone." })
                    .WaitAsync(TimeSpan.FromSeconds(3)));
            pingException = await Assert.ThrowsAnyAsync<Exception>(
                () => client.PingAsync("after-failure").WaitAsync(TimeSpan.FromSeconds(3)));
        }
        finally
        {
            await client.ForceStopAsync();
        }

        Assert.IsNotType<TimeoutException>(sendException);
        Assert.IsNotType<TimeoutException>(pingException);
    }

    private async Task<(string CliPath, string CapturePath, string PidPath)> CreateFakeRuntimeAsync(string behavior)
    {
        var cliPath = Path.Join(Ctx.WorkDir, $"scenario-client-runtime-{behavior}-{Guid.NewGuid():N}.js");
        var capturePath = Path.Join(Ctx.WorkDir, $"scenario-client-runtime-{behavior}-{Guid.NewGuid():N}.json");
        var pidPath = Path.Join(Ctx.WorkDir, $"scenario-client-runtime-{behavior}-{Guid.NewGuid():N}.pid");
        await File.WriteAllTextAsync(cliPath, FakeRuntimeScript);
        return (cliPath, capturePath, pidPath);
    }

    private static async Task<JsonDocument> WaitForCaptureAsync(
        string path,
        Func<JsonElement, bool> predicate)
    {
        JsonDocument? result = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                try
                {
                    using var stream = new FileStream(
                        path,
                        FileMode.Open,
                        FileAccess.Read,
                        FileShare.ReadWrite | FileShare.Delete);
                    using var reader = new StreamReader(stream);
                    var json = await reader.ReadToEndAsync();
                    var document = JsonDocument.Parse(json);
                    if (!predicate(document.RootElement))
                    {
                        document.Dispose();
                        return false;
                    }

                    result = document;
                    return true;
                }
                catch (Exception ex) when (ex is IOException or JsonException)
                {
                    return false;
                }
            },
            timeout: TestTimeout,
            pollInterval: TimeSpan.FromMilliseconds(50),
            timeoutMessage: $"Timed out waiting for fake runtime capture at {path}.");
        return result!;
    }

    private static void AssertArgumentValue(string?[] args, string name, string expectedValue)
    {
        var index = Array.IndexOf(args, name);
        Assert.True(index >= 0, $"Expected argument '{name}' was not present.");
        Assert.True(index + 1 < args.Length, $"Expected argument '{name}' to have a value.");
        Assert.Equal(expectedValue, args[index + 1]);
    }

    private static async Task AssertProcessExitedAsync(int pid)
    {
        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(!IsProcessRunning(pid)),
            timeout: TestTimeout,
            pollInterval: TimeSpan.FromMilliseconds(50),
            timeoutMessage: $"Expected process {pid} to exit.");
    }

    private static bool IsProcessRunning(int pid)
    {
        try
        {
            using var process = Process.GetProcessById(pid);
            return !process.HasExited;
        }
        catch (Exception ex) when (ex is ArgumentException or InvalidOperationException)
        {
            return false;
        }
    }

    private sealed class RecordingExtensionLaunchProvider : IExtensionLaunchProviderHandler
    {
        public TaskCompletionSource<ExtensionLaunchProviderResolveRequest> Request { get; } =
            new(TaskCreationOptions.RunContinuationsAsynchronously);

        public Task<ExtensionLaunchProviderResolveResult> ResolveAsync(
            ExtensionLaunchProviderResolveRequest request,
            CancellationToken cancellationToken = default)
        {
            Request.TrySetResult(request);
            return Task.FromResult(new ExtensionLaunchProviderResolveResult
            {
                Launch = new ExtensionLaunchProfile
                {
                    Executable = "node",
                    Args = ["extension-host", request.ModulePath],
                    Env = new Dictionary<string, string> { ["HOST_KIND"] = "scenario-client" },
                },
            });
        }
    }

    private const string FakeRuntimeScript = """
        const fs = require("fs");

        function argument(name) {
          const index = process.argv.indexOf(name);
          return index >= 0 ? process.argv[index + 1] : undefined;
        }

        const captureFile = argument("--capture-file");
        const pidFile = argument("--pid-file");
        const behavior = argument("--behavior") || "normal";
        const requests = [];
        const clientResponses = [];
        let nextRequestId = 1000;
        let buffer = Buffer.alloc(0);

        fs.writeFileSync(pidFile, String(process.pid));

        function saveCapture() {
          fs.writeFileSync(captureFile, JSON.stringify({
            args: process.argv.slice(2),
            requests,
            clientResponses,
            env: {
              COPILOT_HOME: process.env.COPILOT_HOME,
              COPILOT_SDK_AUTH_TOKEN: process.env.COPILOT_SDK_AUTH_TOKEN,
              COPILOT_OTEL_ENABLED: process.env.COPILOT_OTEL_ENABLED,
              COPILOT_OTEL_SOURCE_NAME: process.env.COPILOT_OTEL_SOURCE_NAME
            }
          }));
        }

        function write(message) {
          const body = JSON.stringify(message);
          process.stdout.write(`Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`);
        }

        function respond(id, result) {
          write({ jsonrpc: "2.0", id, result });
        }

        function request(method, params) {
          const id = nextRequestId++;
          write({ jsonrpc: "2.0", id, method, params });
          return id;
        }

        function handle(message) {
          if (!Object.prototype.hasOwnProperty.call(message, "method")) {
            clientResponses.push(message);
            saveCapture();
            return;
          }

          requests.push({ method: message.method, params: message.params });
          saveCapture();

          if (message.method === "connect") {
            if (behavior !== "hang-connect") {
              respond(message.id, { ok: true, protocolVersion: 4, version: "fake" });
            }
            return;
          }

          if (message.method === "registerExtensionLaunchProvider") {
            respond(message.id, {});
            setTimeout(() => request("extensionLaunchProvider.resolve", {
              id: "project:runtime-e2e",
              modulePath: require("path").resolve(process.cwd(), "extension.mjs"),
              name: "runtime-e2e",
              source: "project"
            }), 10);
            return;
          }

          if (message.method === "plugins.builtin.set") {
            respond(message.id, {});
            return;
          }

          if (message.method === "ping") {
            respond(message.id, {
              message: `pong: ${message.params?.message ?? ""}`,
              timestamp: new Date().toISOString(),
              protocolVersion: 4
            });
            return;
          }

          if (message.method === "session.create") {
            const sessionId = message.params?.sessionId ?? "fake-session";
            respond(message.id, { sessionId, workspacePath: null, capabilities: null });
            if (behavior === "exit-after-create") {
              setTimeout(() => process.exit(17), 25);
            }
            return;
          }

          if (message.method === "session.detach" && behavior === "hang-detach") {
            return;
          }

          if (message.method === "session.detach") {
            respond(message.id, { success: true });
            return;
          }

          if (message.method === "runtime.shutdown") {
            respond(message.id, {});
            setTimeout(() => process.exit(0), 10);
            return;
          }

          respond(message.id, {});
        }

        process.stdin.on("data", chunk => {
          buffer = Buffer.concat([buffer, chunk]);
          while (true) {
            const headerEnd = buffer.indexOf("\r\n\r\n");
            if (headerEnd < 0) return;
            const header = buffer.subarray(0, headerEnd).toString("utf8");
            const match = /Content-Length:\s*(\d+)/i.exec(header);
            if (!match) throw new Error("Missing Content-Length");
            const length = Number(match[1]);
            const bodyStart = headerEnd + 4;
            const bodyEnd = bodyStart + length;
            if (buffer.length < bodyEnd) return;
            const body = buffer.subarray(bodyStart, bodyEnd).toString("utf8");
            buffer = buffer.subarray(bodyEnd);
            handle(JSON.parse(body));
          }
        });

        process.stdin.resume();
        saveCapture();
        setInterval(() => {}, 1000);
        """;
}

#pragma warning restore GHCP001
