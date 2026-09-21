/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Diagnostics;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;
using RpcExtension = GitHub.Copilot.Rpc.Extension;

namespace GitHub.Copilot.Test.E2E;

public class ScenarioTestingJsExtensionBridgeE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_js_extension_bridge", output)
{
    private static readonly TimeSpan ExtensionTimeout = TimeSpan.FromSeconds(60);

    [Fact]
    public async Task Should_Run_Standalone_Empty_Mode_Extension_Canvas_Without_Conversation()
    {
        var fixture = await CreateExtensionFixtureAsync();
        await using var client = CreateExtensionClient(fixture, CopilotClientMode.Empty);
        var config = CreateSessionConfig(fixture.ProjectDirectory);
        config.AvailableTools = new ToolSet();
        config.EnableSessionStore = false;
        config.SkipCustomInstructions = true;

        await using var session = await Ctx.CreateSessionAsync(client, config);

        var extension = await WaitForExtensionAsync(session, fixture.ExtensionId);
        var canvas = await WaitForCanvasAsync(session, fixture.ExtensionId);
        Assert.Equal(ExtensionSource.Project, extension.Source);
        Assert.Equal(ExtensionStatus.Running, extension.Status);
        Assert.Equal("js-scenario-canvas", canvas.CanvasId);
        Assert.NotNull(extension.Pid);

        await session.Rpc.Plugins.ReloadAsync(new SessionPluginsReloadRequest
        {
            ReloadExtensions = false,
        });
        var extensionAfterPluginReload = await WaitForExtensionAsync(session, fixture.ExtensionId);
        Assert.Equal(extension.Pid, extensionAfterPluginReload.Pid);
        Assert.Equal(
            "js-scenario-canvas",
            (await WaitForCanvasAsync(session, fixture.ExtensionId)).CanvasId);

        await WaitForTraceAsync(fixture.TraceFile, "joined");
        var opened = await OpenCanvasWhenRegisteredAsync(
            session,
            canvasId: "js-scenario-canvas",
            instanceId: "standalone-canvas-1",
            extensionId: fixture.ExtensionId,
            input: new Dictionary<string, object> { ["value"] = "standalone" });
        Assert.Equal("ready", opened.Status);

        var action = await session.Rpc.Canvas.Action.InvokeAsync(
            instanceId: "standalone-canvas-1",
            actionName: "set-value",
            input: new Dictionary<string, object> { ["value"] = "updated" });
        Assert.Equal("updated", action.Result!.Value.GetProperty("value").GetString());

        await session.Rpc.Canvas.CloseAsync("standalone-canvas-1");
        await WaitForTraceAsync(fixture.TraceFile, "close");

        var trace = ReadTrace(fixture.TraceFile);
        var joined = trace.Where(entry => GetKind(entry) == "joined").ToList();
        Assert.NotEmpty(joined);
        Assert.All(joined, entry =>
        {
            Assert.Equal(session.SessionId, entry.GetProperty("sessionId").GetString());
            Assert.Equal(
                Path.GetFullPath(fixture.ProjectDirectory),
                Path.GetFullPath(entry.GetProperty("workingDirectory").GetString()!));
        });
        Assert.Equal(
            ["open", "action", "close"],
            trace.Where(entry => GetKind(entry) != "joined").Select(GetKind));
        Assert.DoesNotContain(trace, entry => GetKind(entry) == "sent");
        Assert.Empty(await Ctx.GetExchangesAsync());
    }

    [Fact]
    public async Task Should_Persist_Server_Extension_Enablement_For_Future_Sessions()
    {
        var fixture = await CreateExtensionFixtureAsync(ExtensionSource.User);
        await using var client = CreateExtensionClient(fixture);
        await using var activeSession = await Ctx.CreateSessionAsync(
            client,
            CreateSessionConfig(fixture.ProjectDirectory));

        var active = await WaitForExtensionAsync(activeSession, fixture.ExtensionId);
        Assert.Equal(ExtensionStatus.Running, active.Status);

        await client.Rpc.User.Settings.ReloadAsync();
        var discovered = await client.Rpc.Extensions.DiscoverAsync();
        var discoveredExtension = Assert.Single(
            discovered.Extensions,
            extension => extension.Id == fixture.ExtensionId);
        Assert.True(discoveredExtension.Enabled);
        Assert.Equal(DiscoveredExtensionSource.User, discoveredExtension.Source);
        Assert.Empty((await client.Rpc.Plugins.ListAsync()).Plugins);

        await client.Rpc.Extensions.DisableAsync([fixture.ExtensionId]);
        Assert.Equal(
            ExtensionStatus.Running,
            (await WaitForExtensionAsync(activeSession, fixture.ExtensionId)).Status);

        await using var disabledSession = await Ctx.CreateSessionAsync(
            client,
            CreateSessionConfig(fixture.ProjectDirectory));
        var disabled = await WaitForExtensionAsync(
            disabledSession,
            fixture.ExtensionId,
            ExtensionStatus.Disabled);
        Assert.Null(disabled.Pid);

        await client.Rpc.Extensions.EnableAsync([fixture.ExtensionId]);
        Assert.Equal(
            ExtensionStatus.Disabled,
            (await WaitForExtensionAsync(
                disabledSession,
                fixture.ExtensionId,
                ExtensionStatus.Disabled)).Status);

        await using var enabledSession = await Ctx.CreateSessionAsync(
            client,
            CreateSessionConfig(fixture.ProjectDirectory));
        var enabled = await WaitForExtensionAsync(enabledSession, fixture.ExtensionId);
        Assert.Equal(ExtensionStatus.Running, enabled.Status);
        Assert.NotNull(enabled.Pid);
    }

    [Fact]
    public async Task Should_Bridge_Js_Extension_Canvas_Context_Log_And_Session_Continuation()
    {
        var fixture = await CreateExtensionFixtureAsync();
        await using var client = CreateExtensionClient(fixture);
        await using var session = await Ctx.CreateSessionAsync(client, CreateSessionConfig(fixture.ProjectDirectory));

        var extension = await WaitForExtensionAsync(session, fixture.ExtensionId);
        var canvas = await WaitForCanvasAsync(session, fixture.ExtensionId);
        Assert.Equal(ExtensionStatus.Running, extension.Status);
        Assert.Equal("js-scenario-canvas", canvas.CanvasId);
        Assert.Equal("JavaScript Scenario Canvas", canvas.DisplayName);
        Assert.Equal("object", canvas.InputSchema!.Value.GetProperty("type").GetString());
        Assert.Equal(["set-value", "continue", "fail"], canvas.Actions!.Select(action => action.Name));

        await WaitForTraceAsync(fixture.TraceFile, "joined");
        var joined = ReadTrace(fixture.TraceFile)
            .Where(entry => GetKind(entry) == "joined")
            .ToList();
        Assert.NotEmpty(joined);
        Assert.All(joined, entry =>
        {
            Assert.Equal(session.SessionId, entry.GetProperty("sessionId").GetString());
            Assert.Equal(
                Path.GetFullPath(fixture.ProjectDirectory),
                Path.GetFullPath(entry.GetProperty("workingDirectory").GetString()!));
        });
        var workspacePath = joined[^1].GetProperty("workspacePath").GetString();
        Assert.False(string.IsNullOrWhiteSpace(workspacePath));
        Assert.False(string.IsNullOrEmpty(Path.GetPathRoot(workspacePath)));

        var metadata = await session.Rpc.Metadata.SnapshotAsync();
        Assert.True(
            PathsEqual(fixture.ProjectDirectory, metadata.WorkingDirectory),
            $"Expected working directory '{fixture.ProjectDirectory}', actual '{metadata.WorkingDirectory}'.");

        var opened = await OpenCanvasWhenRegisteredAsync(
            session,
            canvasId: "js-scenario-canvas",
            instanceId: "js-scenario-canvas-1",
            extensionId: fixture.ExtensionId,
            input: new Dictionary<string, object> { ["value"] = "before" });
        Assert.Equal("ready", opened.Status);
        Assert.Equal("JavaScript Scenario Canvas: before", opened.Title);

        var continuation = await session.Rpc.Canvas.Action.InvokeAsync(
            instanceId: "js-scenario-canvas-1",
            actionName: "continue",
            input: new Dictionary<string, object>());
        Assert.False(string.IsNullOrWhiteSpace(
            continuation.Result!.Value.GetProperty("messageId").GetString()));
        await WaitForTraceAsync(fixture.TraceFile, "sent");
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var events = await session.GetEventsAsync();
                return events.OfType<SessionInfoEvent>().Any(evt => evt.Data.Message == "JS_EXTENSION_LOG")
                    && events.OfType<AssistantMessageEvent>().Any(
                        evt => (evt.Data.Content ?? string.Empty).Contains(
                            "JS_EXTENSION_CONTINUATION",
                            StringComparison.Ordinal));
            },
            timeout: ExtensionTimeout,
            pollInterval: TimeSpan.FromMilliseconds(100),
            timeoutMessage: "Timed out waiting for the extension log and continuation.");

        var action = await session.Rpc.Canvas.Action.InvokeAsync(
            instanceId: "js-scenario-canvas-1",
            actionName: "set-value",
            input: new Dictionary<string, object> { ["value"] = "after" });
        Assert.Equal("after", action.Result!.Value.GetProperty("value").GetString());

        await session.Rpc.Canvas.CloseAsync("js-scenario-canvas-1");
        await WaitForTraceAsync(fixture.TraceFile, "close");

        var trace = ReadTrace(fixture.TraceFile);
        var open = Assert.Single(trace, entry => GetKind(entry) == "open");
        AssertBridgeContext(open, session.SessionId, fixture.ExtensionId, "js-scenario-canvas-1");
        Assert.Equal("before", open.GetProperty("input").GetProperty("value").GetString());
        Assert.False(open.TryGetProperty("host", out _));

        var actions = trace.Where(entry => GetKind(entry) == "action").ToList();
        Assert.Equal(["continue", "set-value"], actions.Select(entry => entry.GetProperty("actionName").GetString()));
        Assert.All(actions, entry =>
            AssertBridgeContext(entry, session.SessionId, fixture.ExtensionId, "js-scenario-canvas-1"));
        Assert.Equal("after", actions[1].GetProperty("input").GetProperty("value").GetString());

        var close = Assert.Single(trace, entry => GetKind(entry) == "close");
        AssertBridgeContext(close, session.SessionId, fixture.ExtensionId, "js-scenario-canvas-1");
        Assert.Equal(
            ["open", "action", "sent", "action", "close"],
            trace.Where(entry => GetKind(entry) != "joined").Select(GetKind));
    }

    [Fact]
    public async Task Should_Surface_Structured_CanvasError_From_Js_Extension()
    {
        var fixture = await CreateExtensionFixtureAsync();
        await using var client = CreateExtensionClient(fixture);
        await using var session = await Ctx.CreateSessionAsync(client, CreateSessionConfig(fixture.ProjectDirectory));

        await WaitForExtensionAsync(session, fixture.ExtensionId);
        await WaitForCanvasAsync(session, fixture.ExtensionId);
        await OpenCanvasWhenRegisteredAsync(
            session,
            canvasId: "js-scenario-canvas",
            instanceId: "js-scenario-canvas-error",
            extensionId: fixture.ExtensionId,
            input: new Dictionary<string, object> { ["value"] = "before" });

        var exception = await Assert.ThrowsAsync<IOException>(() =>
            session.Rpc.Canvas.Action.InvokeAsync(
                instanceId: "js-scenario-canvas-error",
                actionName: "fail",
                input: new Dictionary<string, object>()));

        Assert.Contains("The JavaScript canvas action failed.", exception.Message, StringComparison.Ordinal);
        await WaitForTraceAsync(fixture.TraceFile, "error");
        var error = Assert.Single(ReadTrace(fixture.TraceFile), entry => GetKind(entry) == "error");
        Assert.Equal("js_canvas_failed", error.GetProperty("code").GetString());
        Assert.Equal("The JavaScript canvas action failed.", error.GetProperty("message").GetString());
    }

    private CopilotClient CreateExtensionClient(
        ExtensionFixture fixture,
        CopilotClientMode mode = CopilotClientMode.CopilotCli)
    {
        var environment = Ctx.GetEnvironment();
        environment["COPILOT_CLI_ENABLED_FEATURE_FLAGS"] = "EXTENSIONS";
        environment["SCENARIO_EXTENSION_TRACE_FILE"] = fixture.TraceFile;
        environment["SCENARIO_EXTENSION_WORKING_DIRECTORY"] = fixture.ProjectDirectory;

        return Ctx.CreateClient(
            options: new CopilotClientOptions
            {
                Mode = mode,
                BaseDirectory = mode == CopilotClientMode.Empty ? Ctx.HomeDir : null,
                Connection = RuntimeConnection.ForStdio(
                    path: Ctx.GetLegacyCliPath(),
                    args: ["--yolo"]),
            },
            environment: environment);
    }

    private static SessionConfig CreateSessionConfig(string workingDirectory) => new()
    {
        EnableConfigDiscovery = true,
        RequestExtensions = true,
        WorkingDirectory = workingDirectory,
        OnPermissionRequest = PermissionHandler.ApproveAll,
    };

    private async Task<ExtensionFixture> CreateExtensionFixtureAsync(
        ExtensionSource source = default)
    {
        var extensionName = $"js-scenario-bridge-{Guid.NewGuid():N}";
        var projectDirectory = Path.Join(Ctx.WorkDir, $"js-extension-project-{Guid.NewGuid():N}");
        source = source == default ? ExtensionSource.Project : source;
        var extensionDirectory = source == ExtensionSource.User
            ? Path.Join(Ctx.HomeDir, "extensions", extensionName)
            : Path.Join(projectDirectory, ".github", "extensions", extensionName);
        var traceFile = Path.Join(Ctx.WorkDir, $"{extensionName}.jsonl");
        Directory.CreateDirectory(projectDirectory);
        Directory.CreateDirectory(extensionDirectory);
        await InitializeGitRepositoryAsync(projectDirectory);
        File.WriteAllText(Path.Join(extensionDirectory, "extension.mjs"), ExtensionScript);
        return new ExtensionFixture(
            projectDirectory,
            traceFile,
            $"{source.Value}:{extensionName}");
    }

    private static async Task<RpcExtension> WaitForExtensionAsync(
        CopilotSession session,
        string extensionId,
        ExtensionStatus expectedStatus = default)
    {
        expectedStatus = expectedStatus == default ? ExtensionStatus.Running : expectedStatus;
        RpcExtension? extension = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var list = await session.Rpc.Extensions.ListAsync();
                extension = list.Extensions.FirstOrDefault(
                    item => string.Equals(item.Id, extensionId, StringComparison.Ordinal));
                return extension?.Status == expectedStatus;
            },
            timeout: ExtensionTimeout,
            pollInterval: TimeSpan.FromMilliseconds(100),
            timeoutMessage: $"Timed out waiting for extension '{extensionId}'.",
            transientExceptionFilter: ex =>
                ex.ToString().Contains("Extensions not available", StringComparison.OrdinalIgnoreCase));
        return extension!;
    }

    private static async Task<DiscoveredCanvas> WaitForCanvasAsync(CopilotSession session, string extensionId)
    {
        DiscoveredCanvas? canvas = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var list = await session.Rpc.Canvas.ListAsync();
                canvas = list.Canvases.FirstOrDefault(
                    item => string.Equals(item.ExtensionId, extensionId, StringComparison.Ordinal)
                        && string.Equals(item.CanvasId, "js-scenario-canvas", StringComparison.Ordinal));
                return canvas is not null;
            },
            timeout: ExtensionTimeout,
            pollInterval: TimeSpan.FromMilliseconds(100),
            timeoutMessage: $"Timed out waiting for canvas from extension '{extensionId}'.");
        return canvas!;
    }

    private static async Task<OpenCanvasInstance> OpenCanvasWhenRegisteredAsync(
        CopilotSession session,
        string canvasId,
        string instanceId,
        string extensionId,
        object input)
    {
        OpenCanvasInstance? opened = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                try
                {
                    opened = await session.Rpc.Canvas.OpenAsync(
                        canvasId,
                        instanceId,
                        extensionId,
                        input);
                    return true;
                }
                catch (IOException exception)
                    when (exception.Message.Contains("is not registered", StringComparison.OrdinalIgnoreCase))
                {
                    return false;
                }
            },
            timeout: ExtensionTimeout,
            pollInterval: TimeSpan.FromMilliseconds(100),
            timeoutMessage: $"Timed out waiting for canvas '{extensionId}/{canvasId}' to become invokable.");
        return opened!;
    }

    private static async Task WaitForTraceAsync(string traceFile, string kind)
    {
        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(
                File.Exists(traceFile)
                && ReadTrace(traceFile).Any(entry => GetKind(entry) == kind)),
            timeout: ExtensionTimeout,
            pollInterval: TimeSpan.FromMilliseconds(100),
            timeoutMessage: $"Timed out waiting for extension trace entry '{kind}'.");
    }

    private static List<JsonElement> ReadTrace(string traceFile)
    {
        for (var attempt = 0; ; attempt++)
        {
            if (!File.Exists(traceFile))
            {
                return [];
            }

            try
            {
                return File.ReadAllLines(traceFile)
                    .Where(line => !string.IsNullOrWhiteSpace(line))
                    .Select(line =>
                    {
                        using var document = JsonDocument.Parse(line);
                        return document.RootElement.Clone();
                    })
                    .ToList();
            }
            catch (Exception exception)
                when (attempt < 9 && exception is IOException or JsonException)
            {
                Thread.Sleep(20);
            }
        }
    }

    private static string GetKind(JsonElement entry) => entry.GetProperty("kind").GetString()!;

    private static void AssertBridgeContext(
        JsonElement entry,
        string sessionId,
        string extensionId,
        string instanceId)
    {
        Assert.Equal(sessionId, entry.GetProperty("sessionId").GetString());
        Assert.Equal(extensionId, entry.GetProperty("extensionId").GetString());
        Assert.Equal("js-scenario-canvas", entry.GetProperty("canvasId").GetString());
        Assert.Equal(instanceId, entry.GetProperty("instanceId").GetString());
    }

    private static bool PathsEqual(string expected, string? actual) =>
        actual is not null
        && string.Equals(
            Path.GetFullPath(expected).TrimEnd(Path.DirectorySeparatorChar),
            Path.GetFullPath(actual).TrimEnd(Path.DirectorySeparatorChar),
            OperatingSystem.IsWindows() ? StringComparison.OrdinalIgnoreCase : StringComparison.Ordinal);

    private static async Task InitializeGitRepositoryAsync(string projectDirectory)
    {
        var startInfo = new ProcessStartInfo("git")
        {
            WorkingDirectory = projectDirectory,
            Arguments = "init --quiet",
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
        };

        // .NET Framework drops inherited environment variables with empty values.
        // Remove the indexed Git config group so GIT_CONFIG_COUNT cannot reference
        // a value that disappeared while ProcessStartInfo copied the environment.
        foreach (var name in startInfo.Environment.Keys
            .Where(name =>
                name.Equals("GIT_CONFIG_COUNT", StringComparison.OrdinalIgnoreCase)
                || name.StartsWith("GIT_CONFIG_KEY_", StringComparison.OrdinalIgnoreCase)
                || name.StartsWith("GIT_CONFIG_VALUE_", StringComparison.OrdinalIgnoreCase))
            .ToArray())
        {
            startInfo.Environment.Remove(name);
        }

        using var process = new Process
        {
            StartInfo = startInfo,
        };

        if (!process.Start())
        {
            throw new InvalidOperationException("Failed to start git init.");
        }

        await process.WaitForExitAsync();
        if (process.ExitCode != 0)
        {
            throw new InvalidOperationException(
                $"git init failed with exit code {process.ExitCode}: {await process.StandardError.ReadToEndAsync()}");
        }
    }

    private sealed record ExtensionFixture(
        string ProjectDirectory,
        string TraceFile,
        string ExtensionId);

    private const string ExtensionScript = """
        import { appendFileSync } from "node:fs";
        import { CanvasError, createCanvas, joinSession } from "@github/copilot-sdk/extension";

        const traceFile = process.env.SCENARIO_EXTENSION_TRACE_FILE;
        const workingDirectory = process.env.SCENARIO_EXTENSION_WORKING_DIRECTORY;

        function record(kind, data = {}) {
          appendFileSync(traceFile, `${JSON.stringify({ kind, ...data })}\n`);
        }

        let session;
        const canvas = createCanvas({
          id: "js-scenario-canvas",
          displayName: "JavaScript Scenario Canvas",
          description: "Exercises the JavaScript extension bridge.",
          inputSchema: {
            type: "object",
            properties: { value: { type: "string" } },
            required: ["value"]
          },
          actions: [
            {
              name: "set-value",
              description: "Sets the displayed value.",
              inputSchema: {
                type: "object",
                properties: { value: { type: "string" } },
                required: ["value"]
              },
              handler: context => {
                record("action", context);
                return { value: context.input.value };
              }
            },
            {
              name: "continue",
              description: "Continues the host session.",
              handler: async context => {
                record("action", context);
                const messageId = await session.send(
                  "Reply with exactly JS_EXTENSION_CONTINUATION."
                );
                record("sent", { messageId });
                return { messageId };
              }
            },
            {
              name: "fail",
              description: "Throws a structured CanvasError.",
              handler: context => {
                record("action", context);
                const error = new CanvasError(
                  "js_canvas_failed",
                  "The JavaScript canvas action failed."
                );
                record("error", { code: error.code, message: error.message });
                throw error;
              }
            }
          ],
          open: context => {
            record("open", context);
            return {
              status: "ready",
              title: `JavaScript Scenario Canvas: ${context.input.value}`,
              url: `https://example.test/js-scenario-canvas/${context.instanceId}`
            };
          },
          onClose: context => record("close", context)
        });

        session = await joinSession({
          workingDirectory,
          tools: [],
          canvases: [canvas]
        });

        record("joined", {
          sessionId: session.sessionId,
          workspacePath: session.workspacePath ?? null,
          workingDirectory,
          cwd: process.cwd()
        });
        await session.log("JS_EXTENSION_LOG");

        setInterval(() => {}, 60_000).unref?.();
        """;
}
