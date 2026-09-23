/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

#pragma warning disable GHCP001

public class ScenarioTestingCloudE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_cloud", output)
{
    private static readonly TimeSpan TestTimeout = TimeSpan.FromSeconds(30);

    [Fact]
    public async Task Should_Notify_Steerability_Then_Send_First_Message_Without_Remote_Enable()
    {
        await using var session = await CreateSessionAsync();

        await session.Rpc.Remote.NotifySteerableChangedAsync(true);
        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with exactly SCENARIO_STEERABLE_FIRST_SEND.",
        });

        Assert.Contains("SCENARIO_STEERABLE_FIRST_SEND", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        var events = await session.GetEventsAsync();
        var remoteIndex = -1;
        var messageIndex = -1;
        for (var i = 0; i < events.Count; i++)
        {
            if (remoteIndex < 0 && events[i] is SessionRemoteSteerableChangedEvent { Data.RemoteSteerable: true })
            {
                remoteIndex = i;
            }

            if (messageIndex < 0
                && events[i] is UserMessageEvent user
                && user.Data.TransformedContent?.Contains("SCENARIO_STEERABLE_FIRST_SEND", StringComparison.Ordinal) == true)
            {
                messageIndex = i;
            }
        }

        Assert.True(remoteIndex >= 0, "Expected the persisted steerability notification.");
        Assert.True(messageIndex > remoteIndex, "Expected steerability to be persisted before the first send.");
    }

    [Fact]
    [Trait(E2ETestTraits.Backend, E2ETestTraits.SelfConfiguredBackend)]
    public async Task Should_Route_First_Cloud_Event_For_Server_Assigned_Session_Id()
    {
        var (cliPath, capturePath) = await ScenarioTestingTestCli.CreateAsync(Ctx);
        var firstEvent = new TaskCompletionSource<SessionEvent>(
            TaskCreationOptions.RunContinuationsAsynchronously);
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(
                path: cliPath,
                args: ["--capture-file", capturePath, "--behavior", "cloud-assigned-event"]),
            UseLoggedInUser = false,
        });

        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            Cloud = new CloudSessionOptions
            {
                Repository = new CloudSessionRepository
                {
                    Owner = "github",
                    Name = "copilot-sdk",
                    Branch = "main",
                },
            },
            OnEvent = evt => firstEvent.TrySetResult(evt),
        });

        Assert.Equal("server-assigned-cloud-session", session.SessionId);
        var started = Assert.IsType<SessionStartEvent>(
            await firstEvent.Task.WaitAsync(TestTimeout));
        Assert.Equal(session.SessionId, started.Data.SessionId);

        var create = Assert.Single(
            await ScenarioTestingTestCli.ReadRequestsAsync(capturePath),
            request => request.GetProperty("method").GetString() == "session.create")
            .GetProperty("params");
        Assert.False(create.TryGetProperty("sessionId", out _));
        Assert.Equal("github", create.GetProperty("cloud").GetProperty("repository").GetProperty("owner").GetString());
    }

    [Fact]
    [Trait(E2ETestTraits.Backend, E2ETestTraits.SelfConfiguredBackend)]
    public async Task Should_Resume_Using_Runtime_Id_Returned_By_Cloud_Connect()
    {
        var (cliPath, capturePath) = await CreateFakeCloudRuntimeAsync();
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(
                path: cliPath,
                args: ["--capture-file", capturePath, "--resource-id", "github/copilot-sdk#123"]),
            UseLoggedInUser = false,
        });
        await client.StartAsync();

        var connection = await client.Rpc.Sessions.ConnectAsync("cloud-control-session");
        Assert.Equal("runtime-session-id", connection.SessionId);
        Assert.Equal("runtime-session-id", connection.Metadata.SessionId);
        Assert.Equal("github/copilot-sdk#123", connection.Metadata.ResourceId);

        await using var resumed = await Ctx.ResumeSessionAsync(client, connection.SessionId);
        Assert.Equal(connection.SessionId, resumed.SessionId);

        using var capture = await WaitForCaptureAsync(
            capturePath,
            root => GetRequests(root, "session.resume").Count == 1);
        var connectRequest = Assert.Single(GetRequests(capture.RootElement, "sessions.connect"))
            .GetProperty("params");
        var resumeRequest = Assert.Single(GetRequests(capture.RootElement, "session.resume"))
            .GetProperty("params");
        Assert.Equal("cloud-control-session", connectRequest.GetProperty("sessionId").GetString());
        Assert.Equal(connection.SessionId, resumeRequest.GetProperty("sessionId").GetString());
    }

    [Fact]
    [Trait(E2ETestTraits.Backend, E2ETestTraits.SelfConfiguredBackend)]
    public async Task Should_Expose_Cloud_Resource_Mismatch_Before_Resume()
    {
        const string ExpectedResourceId = "github/copilot-sdk#123";
        var (cliPath, capturePath) = await CreateFakeCloudRuntimeAsync();
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(
                path: cliPath,
                args: ["--capture-file", capturePath, "--resource-id", "github/other-repository#456"]),
            UseLoggedInUser = false,
        });
        await client.StartAsync();

        var connection = await client.Rpc.Sessions.ConnectAsync("cloud-control-session");
        Assert.NotEqual(ExpectedResourceId, connection.Metadata.ResourceId);

        using var capture = await WaitForCaptureAsync(
            capturePath,
            root => GetRequests(root, "sessions.connect").Count == 1);
        Assert.Empty(GetRequests(capture.RootElement, "session.resume"));
    }

    private async Task<(string CliPath, string CapturePath)> CreateFakeCloudRuntimeAsync()
    {
        var cliPath = Path.Join(Ctx.WorkDir, $"scenario-client-cloud-{Guid.NewGuid():N}.js");
        var capturePath = Path.Join(Ctx.WorkDir, $"scenario-client-cloud-{Guid.NewGuid():N}.json");
        await File.WriteAllTextAsync(cliPath, FakeCloudRuntimeScript);
        return (cliPath, capturePath);
    }

    private static List<JsonElement> GetRequests(JsonElement root, string method) =>
        root.GetProperty("requests")
            .EnumerateArray()
            .Where(item => item.GetProperty("method").GetString() == method)
            .ToList();

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
                    var document = JsonDocument.Parse(await reader.ReadToEndAsync());
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
            timeoutMessage: $"Timed out waiting for fake cloud runtime capture at {path}.");
        return result!;
    }

    private const string FakeCloudRuntimeScript = """
        const fs = require("fs");

        function argument(name) {
          const index = process.argv.indexOf(name);
          return index >= 0 ? process.argv[index + 1] : undefined;
        }

        const captureFile = argument("--capture-file");
        const resourceId = argument("--resource-id");
        const requests = [];
        let buffer = Buffer.alloc(0);

        function saveCapture() {
          fs.writeFileSync(captureFile, JSON.stringify({ requests }));
        }

        function write(message) {
          const body = JSON.stringify(message);
          process.stdout.write(`Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`);
        }

        function respond(id, result) {
          write({ jsonrpc: "2.0", id, result });
        }

        function handle(message) {
          if (!Object.prototype.hasOwnProperty.call(message, "method")) return;

          requests.push({ method: message.method, params: message.params });
          saveCapture();

          if (message.method === "connect") {
            respond(message.id, { ok: true, protocolVersion: 3, version: "fake" });
            return;
          }

          if (message.method === "sessions.connect") {
            respond(message.id, {
              sessionId: "runtime-session-id",
              metadata: {
                kind: "coding-agent",
                modifiedTime: "2026-09-17T20:00:00Z",
                name: "Cloud task",
                repository: {
                  branch: "main",
                  name: "copilot-sdk",
                  owner: "github"
                },
                resourceId,
                sessionId: "runtime-session-id",
                startTime: "2026-09-17T19:00:00Z",
                state: "active"
              }
            });
            return;
          }

          if (message.method === "session.resume") {
            respond(message.id, {
              sessionId: message.params.sessionId,
              workspacePath: null,
              capabilities: null
            });
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

          respond(message.id, { success: true });
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
