/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ScenarioTestingCanvasE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_canvas", output)
{
    private static readonly TimeSpan EventTimeout = TimeSpan.FromSeconds(60);

    [Fact]
    public async Task Should_Run_Ordered_Scenario_Canvas_Lifecycle_With_Exact_Context_And_Snapshot()
    {
        var handler = new RecordingCanvasHandler();
        await using var session = await CreateSessionAsync(CreateSessionConfig(handler));

        CanvasList list = await WaitForCanvasRegistryAsync(session);
        var canvas = Assert.Single(list.Canvases);
        Assert.Equal("scenario:builtin:e2e-window", canvas.ExtensionId);
        Assert.Equal("scenario-inspector", canvas.CanvasId);
        Assert.Equal("Scenario Inspector", canvas.DisplayName);
        Assert.Equal("Displays scenario-owned state.", canvas.Description);
        Assert.Equal("object", canvas.InputSchema!.Value.GetProperty("type").GetString());
        var action = Assert.Single(canvas.Actions!);
        Assert.Equal("replace", action.Name);
        Assert.Equal("Replaces the displayed value.", action.Description);
        Assert.Equal("object", action.InputSchema!.Value.GetProperty("type").GetString());

        var opened = await session.Rpc.Canvas.OpenAsync(
            canvasId: "scenario-inspector",
            instanceId: "scenario-inspector-1",
            extensionId: canvas.ExtensionId,
            input: new Dictionary<string, object> { ["value"] = "before" });

        Assert.Equal("ready", opened.Status);
        Assert.Equal("Scenario Inspector: before", opened.Title);
        Assert.Equal("https://example.test/scenario-inspector/scenario-inspector-1", opened.Url);
        AssertRequest(handler.OpenRequests.Single(), session.SessionId, "scenario-inspector-1");
        Assert.Equal("before", handler.OpenRequests[0].Input!.Value.GetProperty("value").GetString());

        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(session.OpenCanvases.Count == 1),
            timeout: EventTimeout,
            timeoutMessage: "Timed out waiting for the scenario canvas snapshot.");
        AssertOpenCanvas(Assert.Single(session.OpenCanvases), "scenario-inspector-1", "before");

        var actionResult = await session.Rpc.Canvas.Action.InvokeAsync(
            instanceId: "scenario-inspector-1",
            actionName: "replace",
            input: new Dictionary<string, object> { ["value"] = "after" });

        Assert.Equal("after", actionResult.Result!.Value.GetProperty("value").GetString());
        AssertRequest(handler.ActionRequests.Single(), session.SessionId, "scenario-inspector-1");
        Assert.Equal("replace", handler.ActionRequests[0].ActionName);
        Assert.Equal("after", handler.ActionRequests[0].Input!.Value.GetProperty("value").GetString());

        var liveSnapshot = Assert.Single((await session.Rpc.Canvas.ListOpenAsync()).OpenCanvases);
        AssertOpenCanvas(liveSnapshot, "scenario-inspector-1", "before");

        await session.Rpc.Canvas.CloseAsync("scenario-inspector-1");

        AssertRequest(handler.CloseRequests.Single(), session.SessionId, "scenario-inspector-1");
        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(session.OpenCanvases.Count == 0),
            timeout: EventTimeout,
            timeoutMessage: "Timed out waiting for the scenario canvas to close.");
        Assert.Empty((await session.Rpc.Canvas.ListOpenAsync()).OpenCanvases);
        Assert.Equal(
            ["open:scenario-inspector-1", "action:scenario-inspector-1:replace", "close:scenario-inspector-1"],
            handler.Callbacks);
    }

    [Theory]
    [InlineData("open", true)]
    [InlineData("action", true)]
    [InlineData("close", false)]
    public async Task Should_Handle_Structured_Scenario_Canvas_Error(string operation, bool surfacesToCaller)
    {
        var handler = new RecordingCanvasHandler { StructuredErrorOperation = operation };
        await using var session = await CreateSessionAsync(CreateSessionConfig(handler));
        var canvas = Assert.Single((await WaitForCanvasRegistryAsync(session)).Canvases);
        const string instanceId = "scenario-inspector-error";
        var input = new Dictionary<string, object> { ["value"] = "before" };

        if (operation != "open")
        {
            await session.Rpc.Canvas.OpenAsync(
                canvasId: "scenario-inspector",
                instanceId,
                extensionId: canvas.ExtensionId,
                input);
        }

        Task InvokeAsync() => operation switch
        {
            "open" => session.Rpc.Canvas.OpenAsync(
                canvasId: "scenario-inspector",
                instanceId,
                extensionId: canvas.ExtensionId,
                input),
            "action" => session.Rpc.Canvas.Action.InvokeAsync(
                instanceId,
                actionName: "replace",
                input: new Dictionary<string, object> { ["value"] = "after" }),
            "close" => session.Rpc.Canvas.CloseAsync(instanceId),
            _ => throw new ArgumentOutOfRangeException(nameof(operation)),
        };

        IOException? exception = null;
        if (surfacesToCaller)
        {
            exception = await Assert.ThrowsAsync<IOException>(InvokeAsync);
        }
        else
        {
            await InvokeAsync();
        }

        var expectedCode = $"scenario_canvas_{operation}_failed";
        var expectedMessage = $"The scenario canvas {operation} operation failed.";
        Assert.Equal(expectedCode, handler.ThrownError?.Code);
        Assert.Equal(expectedMessage, handler.ThrownError?.Message);
        if (surfacesToCaller)
        {
            Assert.Contains(expectedMessage, exception!.Message, StringComparison.Ordinal);
        }
        Assert.Equal(
            operation switch
            {
                "open" => [$"open:{instanceId}"],
                "action" => [$"open:{instanceId}", $"action:{instanceId}:replace"],
                "close" => [$"open:{instanceId}", $"close:{instanceId}"],
                _ => throw new ArgumentOutOfRangeException(nameof(operation)),
            },
            handler.Callbacks);
    }

    [Fact]
    public async Task Should_Reattach_Scenario_Canvas_And_Route_All_Callbacks_After_Resume()
    {
        var originalHandler = new RecordingCanvasHandler();
        var client1 = Ctx.CreateClient();
        var session1 = await Ctx.CreateSessionAsync(client1, CreateSessionConfig(originalHandler));
        var sessionId = session1.SessionId;
        var response = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with exactly SCENARIO_CANVAS_READY.",
        });
        Assert.Equal("SCENARIO_CANVAS_READY", response?.Data.Content);
        var canvas = Assert.Single((await WaitForCanvasRegistryAsync(session1)).Canvases);
        await session1.Rpc.Canvas.OpenAsync(
            canvasId: "scenario-inspector",
            instanceId: "scenario-inspector-resume",
            extensionId: canvas.ExtensionId,
            input: new Dictionary<string, object> { ["value"] = "persisted" });
        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(session1.OpenCanvases.Count == 1),
            timeout: EventTimeout,
            timeoutMessage: "Timed out waiting for the pre-resume canvas snapshot.");
        var snapshot = session1.OpenCanvases.ToList();

        await session1.Rpc.SuspendAsync();
        await session1.DisposeAsync();
        await client1.ForceStopAsync();

        var resumedHandler = new RecordingCanvasHandler();
        var client2 = Ctx.CreateClient();
        await using var session2 = await Ctx.ResumeSessionAsync(
            client2,
            sessionId,
            CreateResumeConfig(resumedHandler, snapshot));

        await resumedHandler.Opened.Task.WaitAsync(EventTimeout);
        AssertRequest(resumedHandler.OpenRequests.Single(), sessionId, "scenario-inspector-resume");
        Assert.Equal("persisted", resumedHandler.OpenRequests[0].Input!.Value.GetProperty("value").GetString());
        AssertOpenCanvas(
            await WaitForOpenCanvasAsync(session2, "scenario-inspector-resume"),
            "scenario-inspector-resume",
            "persisted");

        var result = await session2.Rpc.Canvas.Action.InvokeAsync(
            instanceId: "scenario-inspector-resume",
            actionName: "replace",
            input: new Dictionary<string, object> { ["value"] = "resumed" });
        Assert.Equal("resumed", result.Result!.Value.GetProperty("value").GetString());

        await session2.Rpc.Canvas.CloseAsync("scenario-inspector-resume");
        Assert.Equal(
            ["open:scenario-inspector-resume", "action:scenario-inspector-resume:replace", "close:scenario-inspector-resume"],
            resumedHandler.Callbacks);
        Assert.Empty((await session2.Rpc.Canvas.ListOpenAsync()).OpenCanvases);
    }

    private static SessionConfig CreateSessionConfig(RecordingCanvasHandler handler) => new()
    {
        Streaming = true,
        OnPermissionRequest = PermissionHandler.ApproveAll,
        RequestCanvasRenderer = true,
        CanvasProvider = CreateProvider(),
        Canvases = CreateCanvases(),
        CanvasHandler = handler,
    };

    private static ResumeSessionConfig CreateResumeConfig(
        RecordingCanvasHandler handler,
        IList<OpenCanvasInstance> openCanvases) => new()
        {
            Streaming = true,
            ContinuePendingWork = false,
            OnPermissionRequest = PermissionHandler.ApproveAll,
            RequestCanvasRenderer = true,
            CanvasProvider = CreateProvider(),
            Canvases = CreateCanvases(),
            CanvasHandler = handler,
            OpenCanvases = openCanvases,
        };

    private static CanvasProviderIdentity CreateProvider() => new()
    {
        Id = "scenario:builtin:e2e-window",
        Name = "scenario client E2E",
    };

    private static IList<CanvasDeclaration> CreateCanvases()
    {
        using var inputSchema = JsonDocument.Parse(
            """{"type":"object","properties":{"value":{"type":"string"}},"required":["value"]}""");
        return
        [
            new CanvasDeclaration
            {
                Id = "scenario-inspector",
                DisplayName = "Scenario Inspector",
                Description = "Displays scenario-owned state.",
                InputSchema = inputSchema.RootElement.Clone(),
                Actions =
                [
                    new CanvasAction
                    {
                        Name = "replace",
                        Description = "Replaces the displayed value.",
                        InputSchema = inputSchema.RootElement.Clone(),
                    },
                ],
            },
        ];
    }

    private static async Task<CanvasList> WaitForCanvasRegistryAsync(CopilotSession session)
    {
        CanvasList? result = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                result = await session.Rpc.Canvas.ListAsync();
                return result.Canvases.Count == 1;
            },
            timeout: EventTimeout,
            pollInterval: TimeSpan.FromMilliseconds(100),
            timeoutMessage: "Timed out waiting for the scenario canvas registry.");
        return result!;
    }

    private static async Task<OpenCanvasInstance> WaitForOpenCanvasAsync(
        CopilotSession session,
        string instanceId)
    {
        OpenCanvasInstance? result = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                result = (await session.Rpc.Canvas.ListOpenAsync()).OpenCanvases
                    .SingleOrDefault(canvas => canvas.InstanceId == instanceId);
                return result is not null;
            },
            timeout: EventTimeout,
            pollInterval: TimeSpan.FromMilliseconds(100),
            timeoutMessage: $"Timed out waiting for open scenario canvas '{instanceId}'.");
        return result!;
    }

    private static void AssertRequest(CanvasProviderOpenRequest request, string sessionId, string instanceId)
    {
        Assert.Equal(sessionId, request.SessionId);
        Assert.Equal("scenario:builtin:e2e-window", request.ExtensionId);
        Assert.Equal("scenario-inspector", request.CanvasId);
        Assert.Equal(instanceId, request.InstanceId);
        Assert.Null(request.Host);
    }

    private static void AssertRequest(CanvasProviderInvokeActionRequest request, string sessionId, string instanceId)
    {
        Assert.Equal(sessionId, request.SessionId);
        Assert.Equal("scenario:builtin:e2e-window", request.ExtensionId);
        Assert.Equal("scenario-inspector", request.CanvasId);
        Assert.Equal(instanceId, request.InstanceId);
        Assert.Null(request.Host);
    }

    private static void AssertRequest(CanvasProviderCloseRequest request, string sessionId, string instanceId)
    {
        Assert.Equal(sessionId, request.SessionId);
        Assert.Equal("scenario:builtin:e2e-window", request.ExtensionId);
        Assert.Equal("scenario-inspector", request.CanvasId);
        Assert.Equal(instanceId, request.InstanceId);
        Assert.Null(request.Host);
    }

    private static void AssertOpenCanvas(
        OpenCanvasInstance canvas,
        string expectedInstanceId,
        string expectedInput)
    {
        Assert.Equal("scenario-inspector", canvas.CanvasId);
        Assert.Equal("scenario:builtin:e2e-window", canvas.ExtensionId);
        Assert.Equal("scenario client E2E", canvas.ExtensionName);
        Assert.Equal(expectedInstanceId, canvas.InstanceId);
        Assert.Equal(expectedInput, canvas.Input!.Value.GetProperty("value").GetString());
        Assert.Equal("ready", canvas.Status);
        Assert.Equal($"Scenario Inspector: {expectedInput}", canvas.Title);
        Assert.StartsWith("https://example.test/scenario-inspector/", canvas.Url, StringComparison.Ordinal);
    }

    private sealed class RecordingCanvasHandler : CanvasHandlerBase
    {
        public string? StructuredErrorOperation { get; init; }
        public CanvasException? ThrownError { get; private set; }
        public List<string> Callbacks { get; } = [];
        public List<CanvasProviderOpenRequest> OpenRequests { get; } = [];
        public List<CanvasProviderInvokeActionRequest> ActionRequests { get; } = [];
        public List<CanvasProviderCloseRequest> CloseRequests { get; } = [];
        public TaskCompletionSource Opened { get; } =
            new(TaskCreationOptions.RunContinuationsAsynchronously);

        public override Task<CanvasProviderOpenResult> OnOpenAsync(
            CanvasProviderOpenRequest request,
            CancellationToken cancellationToken)
        {
            OpenRequests.Add(request);
            Callbacks.Add($"open:{request.InstanceId}");
            Opened.TrySetResult();
            ThrowStructuredError("open");
            var value = request.Input!.Value.GetProperty("value").GetString();
            return Task.FromResult(new CanvasProviderOpenResult
            {
                Status = "ready",
                Title = $"Scenario Inspector: {value}",
                Url = $"https://example.test/scenario-inspector/{request.InstanceId}",
            });
        }

        public override Task<object?> OnActionAsync(
            CanvasProviderInvokeActionRequest request,
            CancellationToken cancellationToken)
        {
            ActionRequests.Add(request);
            Callbacks.Add($"action:{request.InstanceId}:{request.ActionName}");
            ThrowStructuredError("action");

            return Task.FromResult<object?>(request.Input!.Value.Clone());
        }

        public override Task OnCloseAsync(
            CanvasProviderCloseRequest request,
            CancellationToken cancellationToken)
        {
            CloseRequests.Add(request);
            Callbacks.Add($"close:{request.InstanceId}");
            ThrowStructuredError("close");
            return Task.CompletedTask;
        }

        private void ThrowStructuredError(string operation)
        {
            if (StructuredErrorOperation != operation)
            {
                return;
            }

            ThrownError = new CanvasException(
                $"scenario_canvas_{operation}_failed",
                $"The scenario canvas {operation} operation failed.");
            throw ThrownError;
        }
    }
}
