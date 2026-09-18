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

public class GitHubAppJsExtensionBridgeE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "github_app_js_extension_bridge", output)
{
    private static readonly TimeSpan ExtensionTimeout = TimeSpan.FromSeconds(60);

    [Fact]
    public async Task Should_Bridge_Js_Extension_Canvas_Context_Log_And_Session_Continuation()
    {
        var fixture = await CreateExtensionFixtureAsync();
        await using var client = CreateExtensionClient(fixture);
        await using var session = await Ctx.CreateSessionAsync(client, CreateSessionConfig(fixture.ProjectDirectory));

        var extension = await WaitForExtensionAsync(session, fixture.ExtensionId);
        var canvas = await WaitForCanvasAsync(session, fixture.ExtensionId);
        Assert.Equal(ExtensionStatus.Running, extension.Status);
        Assert.Equal("js-app-canvas", canvas.CanvasId);
        Assert.Equal("JavaScript App Canvas", canvas.DisplayName);
        Assert.Equal("object", canvas.InputSchema!.Value.GetProperty("type").GetString());
        Assert.Equal(["set-value", "continue", "fail"], canvas.Actions!.Select(action => action.Name));

        await WaitForTraceAsync(fixture.TraceFile, "joined");
        var joined = Assert.Single(ReadTrace(fixture.TraceFile), entry => GetKind(entry) == "joined");
        Assert.Equal(session.SessionId, joined.GetProperty("sessionId").GetString());
        Assert.Equal(Path.GetFullPath(fixture.ProjectDirectory), Path.GetFullPath(joined.GetProperty("workingDirectory").GetString()!));
        var workspacePath = joined.GetProperty("workspacePath").GetString();
        Assert.False(string.IsNullOrWhiteSpace(workspacePath));
        Assert.False(string.IsNullOrEmpty(Path.GetPathRoot(workspacePath)));

        var metadata = await session.Rpc.Metadata.SnapshotAsync();
        Assert.True(
            PathsEqual(fixture.ProjectDirectory, metadata.WorkingDirectory),
            $"Expected working directory '{fixture.ProjectDirectory}', actual '{metadata.WorkingDirectory}'.");

        var opened = await session.Rpc.Canvas.OpenAsync(
            canvasId: "js-app-canvas",
            instanceId: "js-app-canvas-1",
            extensionId: fixture.ExtensionId,
            input: new Dictionary<string, object> { ["value"] = "before" });
        Assert.Equal("ready", opened.Status);
        Assert.Equal("JavaScript App Canvas: before", opened.Title);

        var continuation = await session.Rpc.Canvas.Action.InvokeAsync(
            instanceId: "js-app-canvas-1",
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
            instanceId: "js-app-canvas-1",
            actionName: "set-value",
            input: new Dictionary<string, object> { ["value"] = "after" });
        Assert.Equal("after", action.Result!.Value.GetProperty("value").GetString());

        await session.Rpc.Canvas.CloseAsync("js-app-canvas-1");
        await WaitForTraceAsync(fixture.TraceFile, "close");

        var trace = ReadTrace(fixture.TraceFile);
        var open = Assert.Single(trace, entry => GetKind(entry) == "open");
        AssertBridgeContext(open, session.SessionId, fixture.ExtensionId, "js-app-canvas-1");
        Assert.Equal("before", open.GetProperty("input").GetProperty("value").GetString());
        Assert.False(open.TryGetProperty("host", out _));

        var actions = trace.Where(entry => GetKind(entry) == "action").ToList();
        Assert.Equal(["continue", "set-value"], actions.Select(entry => entry.GetProperty("actionName").GetString()));
        Assert.All(actions, entry =>
            AssertBridgeContext(entry, session.SessionId, fixture.ExtensionId, "js-app-canvas-1"));
        Assert.Equal("after", actions[1].GetProperty("input").GetProperty("value").GetString());

        var close = Assert.Single(trace, entry => GetKind(entry) == "close");
        AssertBridgeContext(close, session.SessionId, fixture.ExtensionId, "js-app-canvas-1");
        Assert.Equal(
            ["joined", "open", "action", "sent", "action", "close"],
            trace.Select(GetKind));
    }

    [Fact]
    public async Task Should_Surface_Structured_CanvasError_From_Js_Extension()
    {
        var fixture = await CreateExtensionFixtureAsync();
        await using var client = CreateExtensionClient(fixture);
        await using var session = await Ctx.CreateSessionAsync(client, CreateSessionConfig(fixture.ProjectDirectory));

        await WaitForExtensionAsync(session, fixture.ExtensionId);
        await WaitForCanvasAsync(session, fixture.ExtensionId);
        await session.Rpc.Canvas.OpenAsync(
            canvasId: "js-app-canvas",
            instanceId: "js-app-canvas-error",
            extensionId: fixture.ExtensionId,
            input: new Dictionary<string, object> { ["value"] = "before" });

        var exception = await Assert.ThrowsAsync<IOException>(() =>
            session.Rpc.Canvas.Action.InvokeAsync(
                instanceId: "js-app-canvas-error",
                actionName: "fail",
                input: new Dictionary<string, object>()));

        Assert.Contains("The JavaScript canvas action failed.", exception.Message, StringComparison.Ordinal);
        await WaitForTraceAsync(fixture.TraceFile, "error");
        var error = Assert.Single(ReadTrace(fixture.TraceFile), entry => GetKind(entry) == "error");
        Assert.Equal("js_canvas_failed", error.GetProperty("code").GetString());
        Assert.Equal("The JavaScript canvas action failed.", error.GetProperty("message").GetString());
    }

    private CopilotClient CreateExtensionClient(ExtensionFixture fixture)
    {
        var environment = Ctx.GetEnvironment();
        environment["COPILOT_CLI_ENABLED_FEATURE_FLAGS"] = "EXTENSIONS";
        environment["APP_EXTENSION_TRACE_FILE"] = fixture.TraceFile;
        environment["APP_EXTENSION_WORKING_DIRECTORY"] = fixture.ProjectDirectory;

        return Ctx.CreateClient(
            options: new CopilotClientOptions
            {
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

    private async Task<ExtensionFixture> CreateExtensionFixtureAsync()
    {
        var extensionName = $"js-app-bridge-{Guid.NewGuid():N}";
        var projectDirectory = Path.Join(Ctx.WorkDir, $"js-extension-project-{Guid.NewGuid():N}");
        var extensionDirectory = Path.Join(projectDirectory, ".github", "extensions", extensionName);
        var traceFile = Path.Join(Ctx.WorkDir, $"{extensionName}.jsonl");
        Directory.CreateDirectory(extensionDirectory);
        await InitializeGitRepositoryAsync(projectDirectory);
        File.WriteAllText(Path.Join(extensionDirectory, "extension.mjs"), ExtensionScript);
        return new ExtensionFixture(projectDirectory, traceFile, $"project:{extensionName}");
    }

    private static async Task<RpcExtension> WaitForExtensionAsync(CopilotSession session, string extensionId)
    {
        RpcExtension? extension = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                var list = await session.Rpc.Extensions.ListAsync();
                extension = list.Extensions.FirstOrDefault(
                    item => string.Equals(item.Id, extensionId, StringComparison.Ordinal));
                return extension?.Status == ExtensionStatus.Running;
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
                        && string.Equals(item.CanvasId, "js-app-canvas", StringComparison.Ordinal));
                return canvas is not null;
            },
            timeout: ExtensionTimeout,
            pollInterval: TimeSpan.FromMilliseconds(100),
            timeoutMessage: $"Timed out waiting for canvas from extension '{extensionId}'.");
        return canvas!;
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
        if (!File.Exists(traceFile))
        {
            return [];
        }

        return File.ReadAllLines(traceFile)
            .Where(line => !string.IsNullOrWhiteSpace(line))
            .Select(line =>
            {
                using var document = JsonDocument.Parse(line);
                return document.RootElement.Clone();
            })
            .ToList();
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
        Assert.Equal("js-app-canvas", entry.GetProperty("canvasId").GetString());
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

        const traceFile = process.env.APP_EXTENSION_TRACE_FILE;
        const workingDirectory = process.env.APP_EXTENSION_WORKING_DIRECTORY;

        function record(kind, data = {}) {
          appendFileSync(traceFile, `${JSON.stringify({ kind, ...data })}\n`);
        }

        let session;
        const canvas = createCanvas({
          id: "js-app-canvas",
          displayName: "JavaScript App Canvas",
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
              title: `JavaScript App Canvas: ${context.input.value}`,
              url: `https://example.test/js-app-canvas/${context.instanceId}`
            };
          },
          onClose: context => record("close", context)
        });

        session = await joinSession({
          workingDirectory,
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
