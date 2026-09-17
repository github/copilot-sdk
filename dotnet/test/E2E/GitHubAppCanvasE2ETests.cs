/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class GitHubAppCanvasE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "github_app_canvas", output)
{
    private static readonly TimeSpan EventTimeout = TimeSpan.FromSeconds(60);

    [Fact]
    public async Task Should_Run_Ordered_App_Canvas_Lifecycle_With_Exact_Context_And_Snapshot()
    {
        var handler = new RecordingCanvasHandler();
        await using var session = await CreateSessionAsync(CreateSessionConfig(handler));

        CanvasList list = await WaitForCanvasRegistryAsync(session);
        var canvas = Assert.Single(list.Canvases);
        Assert.Equal("app:builtin:e2e-window", canvas.ExtensionId);
        Assert.Equal("app-inspector", canvas.CanvasId);
        Assert.Equal("App Inspector", canvas.DisplayName);
        Assert.Equal("Displays app-owned state.", canvas.Description);
        Assert.Equal("object", canvas.InputSchema!.Value.GetProperty("type").GetString());
        var action = Assert.Single(canvas.Actions!);
        Assert.Equal("replace", action.Name);
        Assert.Equal("Replaces the displayed value.", action.Description);
        Assert.Equal("object", action.InputSchema!.Value.GetProperty("type").GetString());

        var opened = await session.Rpc.Canvas.OpenAsync(
            canvasId: "app-inspector",
            instanceId: "app-inspector-1",
            extensionId: canvas.ExtensionId,
            input: new Dictionary<string, object> { ["value"] = "before" });

        Assert.Equal("ready", opened.Status);
        Assert.Equal("App Inspector: before", opened.Title);
        Assert.Equal("https://example.test/app-inspector/app-inspector-1", opened.Url);
        AssertRequest(handler.OpenRequests.Single(), session.SessionId, "app-inspector-1");
        Assert.Equal("before", handler.OpenRequests[0].Input!.Value.GetProperty("value").GetString());

        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(session.OpenCanvases.Count == 1),
            timeout: EventTimeout,
            timeoutMessage: "Timed out waiting for the app canvas snapshot.");
        AssertOpenCanvas(Assert.Single(session.OpenCanvases), "app-inspector-1", "before");

        var actionResult = await session.Rpc.Canvas.Action.InvokeAsync(
            instanceId: "app-inspector-1",
            actionName: "replace",
            input: new Dictionary<string, object> { ["value"] = "after" });

        Assert.Equal("after", actionResult.Result!.Value.GetProperty("value").GetString());
        AssertRequest(handler.ActionRequests.Single(), session.SessionId, "app-inspector-1");
        Assert.Equal("replace", handler.ActionRequests[0].ActionName);
        Assert.Equal("after", handler.ActionRequests[0].Input!.Value.GetProperty("value").GetString());

        var liveSnapshot = Assert.Single((await session.Rpc.Canvas.ListOpenAsync()).OpenCanvases);
        AssertOpenCanvas(liveSnapshot, "app-inspector-1", "before");

        await session.Rpc.Canvas.CloseAsync("app-inspector-1");

        AssertRequest(handler.CloseRequests.Single(), session.SessionId, "app-inspector-1");
        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(session.OpenCanvases.Count == 0),
            timeout: EventTimeout,
            timeoutMessage: "Timed out waiting for the app canvas to close.");
        Assert.Empty((await session.Rpc.Canvas.ListOpenAsync()).OpenCanvases);
        Assert.Equal(
            ["open:app-inspector-1", "action:app-inspector-1:replace", "close:app-inspector-1"],
            handler.Callbacks);
    }

    [Fact]
    public async Task Should_Surface_Structured_App_Canvas_Error()
    {
        var handler = new RecordingCanvasHandler { ThrowStructuredError = true };
        await using var session = await CreateSessionAsync(CreateSessionConfig(handler));
        var canvas = Assert.Single((await WaitForCanvasRegistryAsync(session)).Canvases);
        await session.Rpc.Canvas.OpenAsync(
            canvasId: "app-inspector",
            instanceId: "app-inspector-error",
            extensionId: canvas.ExtensionId,
            input: new Dictionary<string, object> { ["value"] = "before" });

        var exception = await Assert.ThrowsAsync<IOException>(() =>
            session.Rpc.Canvas.Action.InvokeAsync(
                instanceId: "app-inspector-error",
                actionName: "replace",
                input: new Dictionary<string, object> { ["value"] = "after" }));

        Assert.Equal("app_canvas_replace_failed", handler.ThrownError?.Code);
        Assert.Equal("The app canvas value could not be replaced.", handler.ThrownError?.Message);
        Assert.Contains("The app canvas value could not be replaced.", exception.Message, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Should_Reattach_App_Canvas_And_Route_All_Callbacks_After_Resume()
    {
        var originalHandler = new RecordingCanvasHandler();
        var client1 = Ctx.CreateClient();
        var session1 = await Ctx.CreateSessionAsync(client1, CreateSessionConfig(originalHandler));
        var sessionId = session1.SessionId;
        var response = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with exactly APP_CANVAS_READY.",
        });
        Assert.Equal("APP_CANVAS_READY", response?.Data.Content);
        var canvas = Assert.Single((await WaitForCanvasRegistryAsync(session1)).Canvases);
        await session1.Rpc.Canvas.OpenAsync(
            canvasId: "app-inspector",
            instanceId: "app-inspector-resume",
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
        AssertRequest(resumedHandler.OpenRequests.Single(), sessionId, "app-inspector-resume");
        Assert.Equal("persisted", resumedHandler.OpenRequests[0].Input!.Value.GetProperty("value").GetString());
        AssertOpenCanvas(
            Assert.Single((await session2.Rpc.Canvas.ListOpenAsync()).OpenCanvases),
            "app-inspector-resume",
            "persisted");

        var result = await session2.Rpc.Canvas.Action.InvokeAsync(
            instanceId: "app-inspector-resume",
            actionName: "replace",
            input: new Dictionary<string, object> { ["value"] = "resumed" });
        Assert.Equal("resumed", result.Result!.Value.GetProperty("value").GetString());

        await session2.Rpc.Canvas.CloseAsync("app-inspector-resume");
        Assert.Equal(
            ["open:app-inspector-resume", "action:app-inspector-resume:replace", "close:app-inspector-resume"],
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
        Id = "app:builtin:e2e-window",
        Name = "GitHub App E2E",
    };

    private static IList<CanvasDeclaration> CreateCanvases()
    {
        using var inputSchema = JsonDocument.Parse(
            """{"type":"object","properties":{"value":{"type":"string"}},"required":["value"]}""");
        return
        [
            new CanvasDeclaration
            {
                Id = "app-inspector",
                DisplayName = "App Inspector",
                Description = "Displays app-owned state.",
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
            timeoutMessage: "Timed out waiting for the app canvas registry.");
        return result!;
    }

    private static void AssertRequest(CanvasProviderOpenRequest request, string sessionId, string instanceId)
    {
        Assert.Equal(sessionId, request.SessionId);
        Assert.Equal("app:builtin:e2e-window", request.ExtensionId);
        Assert.Equal("app-inspector", request.CanvasId);
        Assert.Equal(instanceId, request.InstanceId);
        Assert.Null(request.Host);
    }

    private static void AssertRequest(CanvasProviderInvokeActionRequest request, string sessionId, string instanceId)
    {
        Assert.Equal(sessionId, request.SessionId);
        Assert.Equal("app:builtin:e2e-window", request.ExtensionId);
        Assert.Equal("app-inspector", request.CanvasId);
        Assert.Equal(instanceId, request.InstanceId);
        Assert.Null(request.Host);
    }

    private static void AssertRequest(CanvasProviderCloseRequest request, string sessionId, string instanceId)
    {
        Assert.Equal(sessionId, request.SessionId);
        Assert.Equal("app:builtin:e2e-window", request.ExtensionId);
        Assert.Equal("app-inspector", request.CanvasId);
        Assert.Equal(instanceId, request.InstanceId);
        Assert.Null(request.Host);
    }

    private static void AssertOpenCanvas(
        OpenCanvasInstance canvas,
        string expectedInstanceId,
        string expectedInput)
    {
        Assert.Equal("app-inspector", canvas.CanvasId);
        Assert.Equal("app:builtin:e2e-window", canvas.ExtensionId);
        Assert.Equal("GitHub App E2E", canvas.ExtensionName);
        Assert.Equal(expectedInstanceId, canvas.InstanceId);
        Assert.Equal(expectedInput, canvas.Input!.Value.GetProperty("value").GetString());
        Assert.Equal("ready", canvas.Status);
        Assert.Equal($"App Inspector: {expectedInput}", canvas.Title);
        Assert.StartsWith("https://example.test/app-inspector/", canvas.Url, StringComparison.Ordinal);
    }

    private sealed class RecordingCanvasHandler : CanvasHandlerBase
    {
        public bool ThrowStructuredError { get; init; }
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
            var value = request.Input!.Value.GetProperty("value").GetString();
            return Task.FromResult(new CanvasProviderOpenResult
            {
                Status = "ready",
                Title = $"App Inspector: {value}",
                Url = $"https://example.test/app-inspector/{request.InstanceId}",
            });
        }

        public override Task<object?> OnActionAsync(
            CanvasProviderInvokeActionRequest request,
            CancellationToken cancellationToken)
        {
            ActionRequests.Add(request);
            Callbacks.Add($"action:{request.InstanceId}:{request.ActionName}");
            if (ThrowStructuredError)
            {
                ThrownError = new CanvasException(
                    "app_canvas_replace_failed",
                    "The app canvas value could not be replaced.");
                throw ThrownError;
            }

            return Task.FromResult<object?>(request.Input!.Value.Clone());
        }

        public override Task OnCloseAsync(
            CanvasProviderCloseRequest request,
            CancellationToken cancellationToken)
        {
            CloseRequests.Add(request);
            Callbacks.Add($"close:{request.InstanceId}");
            return Task.CompletedTask;
        }
    }
}
