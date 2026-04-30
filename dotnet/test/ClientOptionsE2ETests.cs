/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Net;
using System.Net.Sockets;
using System.Text.Json;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class ClientOptionsE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "client_options", output)
{
    [Fact]
    public async Task AutoStart_False_Requires_Explicit_Start()
    {
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            AutoStart = false,
        });

        Assert.Equal(ConnectionState.Disconnected, client.State);

        var ex = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            client.CreateSessionAsync(new SessionConfig { OnPermissionRequest = PermissionHandler.ApproveAll }));
        Assert.Contains("StartAsync", ex.Message, StringComparison.Ordinal);

        await client.StartAsync();
        Assert.Equal(ConnectionState.Connected, client.State);

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });
        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Listen_On_Configured_Tcp_Port()
    {
        var port = GetAvailableTcpPort();
        await using var client = Ctx.CreateClient(
            useStdio: false,
            options: new CopilotClientOptions
            {
                Port = port,
            });

        await client.StartAsync();

        Assert.Equal(ConnectionState.Connected, client.State);
        Assert.Equal(port, client.ActualPort);

        var response = await client.PingAsync("fixed-port");
        Assert.Equal("pong: fixed-port", response.Message);
    }

    [Fact]
    public async Task Should_Use_Client_Cwd_For_Default_WorkingDirectory()
    {
        var clientCwd = Path.Join(Ctx.WorkDir, "client-cwd");
        Directory.CreateDirectory(clientCwd);
        await File.WriteAllTextAsync(Path.Join(clientCwd, "marker.txt"), "I am in the client cwd");

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Cwd = clientCwd,
        });

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        var message = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Read the file marker.txt and tell me what it says",
        });

        Assert.Contains("client cwd", message?.Data.Content ?? string.Empty);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Propagate_Process_Options_To_Spawned_Cli()
    {
        var cliPath = Path.Join(Ctx.WorkDir, $"fake-cli-{Guid.NewGuid():N}.js");
        var capturePath = Path.Join(Ctx.WorkDir, $"fake-cli-capture-{Guid.NewGuid():N}.json");
        var telemetryPath = Path.Join(Ctx.WorkDir, "telemetry.jsonl");
        await File.WriteAllTextAsync(cliPath, FakeStdioCliScript);

        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            AutoStart = false,
            CliPath = cliPath,
            CliArgs = ["--capture-file", capturePath],
            GitHubToken = "process-option-token",
            LogLevel = "debug",
            SessionIdleTimeoutSeconds = 17,
            Telemetry = new TelemetryConfig
            {
                OtlpEndpoint = "http://127.0.0.1:4318",
                FilePath = telemetryPath,
                ExporterType = "file",
                SourceName = "dotnet-sdk-e2e",
                CaptureContent = true,
            },
            UseLoggedInUser = false,
        });

        await client.StartAsync();

        using var capture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var root = capture.RootElement;
        var args = root.GetProperty("args").EnumerateArray().Select(e => e.GetString()).ToArray();
        var env = root.GetProperty("env");

        AssertArgumentValue(args, "--log-level", "debug");
        Assert.Contains("--stdio", args);
        AssertArgumentValue(args, "--auth-token-env", "COPILOT_SDK_AUTH_TOKEN");
        Assert.Contains("--no-auto-login", args);
        AssertArgumentValue(args, "--session-idle-timeout", "17");
        Assert.Equal(Path.GetFullPath(Ctx.WorkDir), root.GetProperty("cwd").GetString());

        Assert.Equal("process-option-token", env.GetProperty("COPILOT_SDK_AUTH_TOKEN").GetString());
        Assert.Equal("true", env.GetProperty("COPILOT_OTEL_ENABLED").GetString());
        Assert.Equal("http://127.0.0.1:4318", env.GetProperty("OTEL_EXPORTER_OTLP_ENDPOINT").GetString());
        Assert.Equal(telemetryPath, env.GetProperty("COPILOT_OTEL_FILE_EXPORTER_PATH").GetString());
        Assert.Equal("file", env.GetProperty("COPILOT_OTEL_EXPORTER_TYPE").GetString());
        Assert.Equal("dotnet-sdk-e2e", env.GetProperty("COPILOT_OTEL_SOURCE_NAME").GetString());
        Assert.Equal("true", env.GetProperty("OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT").GetString());

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            EnableConfigDiscovery = true,
            IncludeSubAgentStreamingEvents = false,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        using var updatedCapture = JsonDocument.Parse(await File.ReadAllTextAsync(capturePath));
        var createRequest = updatedCapture.RootElement
            .GetProperty("requests")
            .EnumerateArray()
            .Single(request => request.GetProperty("method").GetString() == "session.create")
            .GetProperty("params");
        Assert.True(createRequest.GetProperty("enableConfigDiscovery").GetBoolean());
        Assert.False(createRequest.GetProperty("includeSubAgentStreamingEvents").GetBoolean());

        await session.DisposeAsync();
    }

    private static int GetAvailableTcpPort()
    {
        var listener = new TcpListener(IPAddress.Loopback, 0);
        listener.Start();
        try
        {
            return ((IPEndPoint)listener.LocalEndpoint).Port;
        }
        finally
        {
            listener.Stop();
        }
    }

    private static void AssertArgumentValue(string?[] args, string name, string expectedValue)
    {
        var index = Array.IndexOf(args, name);
        Assert.True(index >= 0, $"Expected argument '{name}' was not present. Args: {string.Join(" ", args)}");
        Assert.True(index + 1 < args.Length, $"Expected argument '{name}' to have a value.");
        Assert.Equal(expectedValue, args[index + 1]);
    }

    private const string FakeStdioCliScript = """
        const fs = require("fs");

        const captureIndex = process.argv.indexOf("--capture-file");
        const captureFile = captureIndex >= 0 ? process.argv[captureIndex + 1] : undefined;
        const requests = [];

        function saveCapture() {
          if (!captureFile) {
            return;
          }

          fs.writeFileSync(captureFile, JSON.stringify({
            args: process.argv.slice(2),
            cwd: process.cwd(),
            requests,
            env: {
              COPILOT_SDK_AUTH_TOKEN: process.env.COPILOT_SDK_AUTH_TOKEN,
              COPILOT_OTEL_ENABLED: process.env.COPILOT_OTEL_ENABLED,
              OTEL_EXPORTER_OTLP_ENDPOINT: process.env.OTEL_EXPORTER_OTLP_ENDPOINT,
              COPILOT_OTEL_FILE_EXPORTER_PATH: process.env.COPILOT_OTEL_FILE_EXPORTER_PATH,
              COPILOT_OTEL_EXPORTER_TYPE: process.env.COPILOT_OTEL_EXPORTER_TYPE,
              COPILOT_OTEL_SOURCE_NAME: process.env.COPILOT_OTEL_SOURCE_NAME,
              OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT: process.env.OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT
            }
          }));
        }

        saveCapture();

        let buffer = Buffer.alloc(0);

        process.stdin.on("data", chunk => {
          buffer = Buffer.concat([buffer, chunk]);
          processBuffer();
        });

        process.stdin.resume();

        function processBuffer() {
          while (true) {
            const headerEnd = buffer.indexOf("\r\n\r\n");
            if (headerEnd < 0) {
              return;
            }

            const header = buffer.subarray(0, headerEnd).toString("utf8");
            const match = /Content-Length:\s*(\d+)/i.exec(header);
            if (!match) {
              throw new Error("Missing Content-Length header");
            }

            const length = Number(match[1]);
            const bodyStart = headerEnd + 4;
            const bodyEnd = bodyStart + length;
            if (buffer.length < bodyEnd) {
              return;
            }

            const body = buffer.subarray(bodyStart, bodyEnd).toString("utf8");
            buffer = buffer.subarray(bodyEnd);
            handleMessage(JSON.parse(body));
          }
        }

        function handleMessage(message) {
          if (!Object.prototype.hasOwnProperty.call(message, "id")) {
            return;
          }

          requests.push({ method: message.method, params: message.params });
          saveCapture();

          if (message.method === "ping") {
            writeResponse(message.id, { message: "pong", protocolVersion: 3 });
            return;
          }

          if (message.method === "session.create") {
            const sessionId = message.params?.sessionId ?? message.params?.[0]?.sessionId ?? "fake-session";
            writeResponse(message.id, { sessionId, workspacePath: null, capabilities: null });
            return;
          }

          writeResponse(message.id, {});
        }

        function writeResponse(id, result) {
          const body = JSON.stringify({ jsonrpc: "2.0", id, result });
          process.stdout.write(`Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`);
        }
        """;
}
