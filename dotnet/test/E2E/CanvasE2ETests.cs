/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class CanvasE2ETests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "canvas", output)
{
    [Fact]
    public async Task DispatchesCanvasOpenToProviderHandler()
    {
        var opens = new List<CanvasOpenContext>();
        await using var session = await CreateSessionAsync(CreateCanvasSessionConfig(new RecordingCanvasHandler(opens: opens)));

        var result = await session.Rpc.Canvas.OpenAsync(
            canvasId: "counter",
            instanceId: "counter-1",
            input: new Dictionary<string, object> { ["seed"] = 7 });

        var open = Assert.Single(opens);
        Assert.Equal("counter", open.CanvasId);
        Assert.Equal("counter-1", open.InstanceId);
        Assert.Equal(7, open.Input.GetProperty("seed").GetInt32());
        Assert.Equal("counter", result.CanvasId);
        Assert.Equal("counter-1", result.InstanceId);
        Assert.Equal("https://example.test/counter-1", result.Url);
        Assert.Equal(CanvasInstanceAvailability.Ready, result.Availability);
    }

    [Fact]
    public async Task DispatchesCanvasActionInvokeToHandler()
    {
        var actions = new List<CanvasActionContext>();
        await using var session = await CreateSessionAsync(CreateCanvasSessionConfig(new RecordingCanvasHandler(actions: actions)));

        await session.Rpc.Canvas.OpenAsync(canvasId: "counter", instanceId: "counter-2");
        var result = await session.Rpc.Canvas.InvokeActionAsync(
            instanceId: "counter-2",
            actionName: "increment",
            input: new Dictionary<string, object> { ["amount"] = 3 });

        var action = Assert.Single(actions);
        Assert.Equal("counter", action.CanvasId);
        Assert.Equal("counter-2", action.InstanceId);
        Assert.Equal("increment", action.ActionName);
        Assert.Equal(3, action.Input.GetProperty("amount").GetInt32());

        var actionResult = result.Result;
        Assert.NotNull(actionResult);
        var payload = actionResult!.Value;
        Assert.True(payload.GetProperty("ok").GetBoolean());
        Assert.Equal("increment", payload.GetProperty("actionName").GetString());
        Assert.Equal(3, payload.GetProperty("input").GetProperty("amount").GetInt32());
    }

    [Fact]
    public async Task DispatchesCanvasCloseToOnCloseHandler()
    {
        var closes = new List<CanvasLifecycleContext>();
        await using var session = await CreateSessionAsync(CreateCanvasSessionConfig(new RecordingCanvasHandler(closes: closes)));

        await session.Rpc.Canvas.OpenAsync(canvasId: "counter", instanceId: "counter-3");
        await session.Rpc.Canvas.CloseAsync(instanceId: "counter-3");
        await Task.Delay(50);

        var close = Assert.Single(closes);
        Assert.Equal("counter", close.CanvasId);
        Assert.Equal("counter-3", close.InstanceId);
    }

    [Fact]
    public async Task ReturnsCanvasActionNoHandlerForDeclaredActionWithoutHandler()
    {
        await using var session = await CreateSessionAsync(CreateCanvasSessionConfig(new OpenOnlyCanvasHandler()));

        await session.Rpc.Canvas.OpenAsync(canvasId: "counter", instanceId: "counter-4");
        var ex = await Assert.ThrowsAsync<IOException>(() => session.Rpc.Canvas.InvokeActionAsync(
            instanceId: "counter-4",
            actionName: "increment",
            input: new Dictionary<string, object>()));

        Assert.Contains("canvas_action_no_handler", ex.Message, StringComparison.Ordinal);
    }

    [Fact]
    public async Task SeedsOpenCanvasesOnResumeFromRuntime()
    {
        await using var sessionA = await CreateSessionAsync(CreateCanvasSessionConfig(new OpenOnlyCanvasHandler()));

        await sessionA.Rpc.Canvas.OpenAsync(
            canvasId: "counter",
            instanceId: "counter-resume",
            input: new Dictionary<string, object> { ["initial"] = true });

        await using var resumed = await ResumeSessionAsync(sessionA.SessionId, CreateCanvasResumeConfig(new OpenOnlyCanvasHandler()));

        Assert.NotEmpty(resumed.OpenCanvases);
        var match = Assert.Single(resumed.OpenCanvases, canvas => canvas.InstanceId == "counter-resume");
        Assert.Equal("counter", match.CanvasId);
    }

    private static SessionConfig CreateCanvasSessionConfig(ICanvasHandler handler) => new()
    {
        Canvases = [CreateCounterCanvas()],
        CanvasHandler = handler,
        RequestCanvasRenderer = true,
        ExtensionInfo = new ExtensionInfo { Source = "github-app", Name = "counter-provider" },
        OnPermissionRequest = PermissionHandler.ApproveAll,
    };

    private static ResumeSessionConfig CreateCanvasResumeConfig(ICanvasHandler handler) => new()
    {
        Canvases = [CreateCounterCanvas()],
        CanvasHandler = handler,
        RequestCanvasRenderer = true,
        ExtensionInfo = new ExtensionInfo { Source = "github-app", Name = "counter-provider" },
        OnPermissionRequest = PermissionHandler.ApproveAll,
    };

    private static CanvasDeclaration CreateCounterCanvas() => new()
    {
        Id = "counter",
        DisplayName = "Counter",
        Description = "A test counter canvas",
        Actions =
        [
            new CanvasAction
            {
                Name = "increment",
                Description = "Increment the counter",
            },
        ],
    };

    private class OpenOnlyCanvasHandler : CanvasHandlerBase
    {
        public override Task<CanvasOpenResponse> OnOpenAsync(CanvasOpenContext context, CancellationToken cancellationToken)
            => Task.FromResult(new CanvasOpenResponse { Url = $"https://example.test/{context.InstanceId}" });
    }

    private sealed class RecordingCanvasHandler(
        List<CanvasOpenContext>? opens = null,
        List<CanvasLifecycleContext>? closes = null,
        List<CanvasActionContext>? actions = null) : OpenOnlyCanvasHandler
    {
        public override Task<CanvasOpenResponse> OnOpenAsync(CanvasOpenContext context, CancellationToken cancellationToken)
        {
            opens?.Add(CloneOpenContext(context));
            return base.OnOpenAsync(context, cancellationToken);
        }

        public override Task OnCloseAsync(CanvasLifecycleContext context, CancellationToken cancellationToken)
        {
            closes?.Add(context);
            return Task.CompletedTask;
        }

        public override Task<object?> OnActionAsync(CanvasActionContext context, CancellationToken cancellationToken)
        {
            actions?.Add(CloneActionContext(context));
            return Task.FromResult<object?>(new Dictionary<string, object?>
            {
                ["ok"] = true,
                ["actionName"] = context.ActionName,
                ["input"] = context.Input.Clone(),
            });
        }
    }

    private static CanvasOpenContext CloneOpenContext(CanvasOpenContext context) => new()
    {
        SessionId = context.SessionId,
        ExtensionId = context.ExtensionId,
        CanvasId = context.CanvasId,
        InstanceId = context.InstanceId,
        Input = context.Input.Clone(),
        Host = context.Host,
    };

    private static CanvasActionContext CloneActionContext(CanvasActionContext context) => new()
    {
        SessionId = context.SessionId,
        ExtensionId = context.ExtensionId,
        CanvasId = context.CanvasId,
        InstanceId = context.InstanceId,
        ActionName = context.ActionName,
        Input = context.Input.Clone(),
        Host = context.Host,
    };
}
