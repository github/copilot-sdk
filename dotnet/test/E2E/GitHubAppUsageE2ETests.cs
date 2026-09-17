/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Microsoft.Extensions.AI;
using System.ComponentModel;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

/// <summary>
/// End-to-end coverage for representative SDK workflows used by github/github-app.
/// These tests intentionally compose APIs that are otherwise covered individually.
/// </summary>
public class GitHubAppUsageE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "github_app_usage", output)
{
    private static readonly TimeSpan EventTimeout = TimeSpan.FromSeconds(60);

    [Fact]
    public async Task Should_Send_App_Message_With_Metadata_And_Extension_Context()
    {
        using var payload = JsonDocument.Parse("""{"selection":"TRACE_SENTINEL","line":42}""");
        await using var session = await CreateSessionAsync(new SessionConfig { Streaming = true });
        var idle = TestHelper.GetNextEventOfTypeAsync<SessionIdleEvent>(session, EventTimeout);

        var messageId = await session.SendAsync(new MessageOptions
        {
            Prompt = "Reply with exactly TRACE_SENTINEL from the attached extension context.",
            DisplayPrompt = "Analyze the selected trace entry",
            Mode = "enqueue",
            AgentMode = AgentMode.Interactive,
            Source = MessageSource.Agent("trace-viewer"),
            Attachments =
            [
                new AttachmentExtensionContext
                {
                    CapturedAt = DateTimeOffset.Parse("2026-09-17T20:00:00Z"),
                    ExtensionId = "github-app:trace-viewer",
                    CanvasId = "trace",
                    InstanceId = "trace-1",
                    Title = "Selected trace entry",
                    Payload = payload.RootElement.Clone(),
                },
            ],
        });

        await idle;

        var events = await session.GetEventsAsync();
        var userMessage = Assert.Single(
            events.OfType<UserMessageEvent>(),
            evt => string.Equals(evt.Data.MessageId, messageId, StringComparison.Ordinal));
        Assert.Equal("Analyze the selected trace entry", userMessage.Data.Content);
        Assert.Equal(UserMessageDelivery.Idle, userMessage.Data.Delivery);
        Assert.Equal(UserMessageAgentMode.Interactive, userMessage.Data.AgentMode);
        Assert.Equal("agent-trace-viewer", userMessage.Data.Source);
        Assert.Contains("TRACE_SENTINEL", userMessage.Data.TransformedContent ?? string.Empty, StringComparison.Ordinal);

        var attachment = Assert.IsType<AttachmentExtensionContext>(Assert.Single(userMessage.Data.Attachments!));
        Assert.Equal("github-app:trace-viewer", attachment.ExtensionId);
        Assert.Equal("trace", attachment.CanvasId);
        Assert.Equal("trace-1", attachment.InstanceId);
        Assert.Equal("Selected trace entry", attachment.Title);
        Assert.Equal("TRACE_SENTINEL", attachment.Payload!.Value.GetProperty("selection").GetString());

        var assistantMessage = events.OfType<AssistantMessageEvent>().Last();
        Assert.Contains("TRACE_SENTINEL", assistantMessage.Data.Content ?? string.Empty, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Should_Classify_Queued_And_Immediate_App_Messages_While_Busy()
    {
        var toolStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseTool = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(WaitForReleaseAsync, "wait_for_app_release")],
        });
        var userMessages = new List<UserMessageEvent>();
        var userMessagesLock = new object();
        using var subscription = session.On<UserMessageEvent>(message =>
        {
            lock (userMessagesLock)
            {
                userMessages.Add(message);
            }
        });

        try
        {
            await session.SendAsync(new MessageOptions
            {
                Prompt = "Call wait_for_app_release, then reply with its result.",
            });
            await toolStarted.Task.WaitAsync(EventTimeout);

            var queuedMessageId = await session.SendAsync(new MessageOptions
            {
                Prompt = "Reply with QUEUED_APP_MESSAGE after the active turn.",
                DisplayPrompt = "Queued follow-up",
                Mode = "enqueue",
                Source = MessageSource.System,
            });
            var steeringMessageId = await session.SendAsync(new MessageOptions
            {
                Prompt = "Reply with STEERING_APP_MESSAGE instead.",
                DisplayPrompt = "Immediate steering update",
                Mode = "immediate",
                Source = MessageSource.Agent("session-coordinator"),
            });

            releaseTool.TrySetResult("ACTIVE_TURN_RELEASED");

            await TestHelper.WaitForConditionAsync(
                () =>
                {
                    lock (userMessagesLock)
                    {
                        return Task.FromResult(
                            userMessages.Any(evt => evt.Data.MessageId == queuedMessageId) &&
                            userMessages.Any(evt => evt.Data.MessageId == steeringMessageId));
                    }
                },
                timeout: EventTimeout,
                timeoutMessage: "Timed out waiting for queued and steering messages to be consumed.");

            List<UserMessageEvent> observedMessages;
            lock (userMessagesLock)
            {
                observedMessages = [.. userMessages];
            }

            var queued = Assert.Single(observedMessages, evt => evt.Data.MessageId == queuedMessageId);
            Assert.Equal("Queued follow-up", queued.Data.Content);
            Assert.Equal(UserMessageDelivery.Queued, queued.Data.Delivery);
            Assert.Equal("system", queued.Data.Source);

            var steering = Assert.Single(observedMessages, evt => evt.Data.MessageId == steeringMessageId);
            Assert.Equal("Immediate steering update", steering.Data.Content);
            Assert.Equal(UserMessageDelivery.Steering, steering.Data.Delivery);
            Assert.Equal("agent-session-coordinator", steering.Data.Source);
        }
        finally
        {
            releaseTool.TrySetResult("RELEASED_AFTER_TEST");
        }

        [Description("Waits until the app releases the active turn")]
        async Task<string> WaitForReleaseAsync(CancellationToken cancellationToken)
        {
            toolStarted.TrySetResult();
            return await releaseTool.Task.WaitAsync(Timeout.InfiniteTimeSpan, cancellationToken);
        }
    }

    [Fact]
    public async Task Should_Resume_With_Reattached_App_Host_State()
    {
        var originalCanvasHandler = new AppCanvasHandler();
        var client1 = Ctx.CreateClient();
        var session1 = await Ctx.CreateSessionAsync(
            client1,
            CreateAppSessionConfig(originalCanvasHandler, includeTool: false));
        var sessionId = session1.SessionId;
        var initialResponse = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Remember APP_RESUME_MARKER and reply with exactly INITIALIZED.",
        });
        Assert.Contains("INITIALIZED", initialResponse?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        var canvas = Assert.Single((await session1.Rpc.Canvas.ListAsync()).Canvases);
        await session1.Rpc.Canvas.OpenAsync(
            canvasId: "app-counter",
            instanceId: "app-counter-1",
            extensionId: canvas.ExtensionId,
            input: new Dictionary<string, object> { ["start"] = 40 });
        await session1.LogAsync("APP_HOST_STATE_MARKER");

        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(session1.OpenCanvases.Count == 1),
            timeout: EventTimeout,
            timeoutMessage: "Timed out waiting for the open canvas snapshot.");
        var openCanvases = session1.OpenCanvases.ToList();

        await session1.Rpc.SuspendAsync();
        await session1.DisposeAsync();
        await client1.ForceStopAsync();

        var resumedCanvasHandler = new AppCanvasHandler();
        var client2 = Ctx.CreateClient();
        await using var session2 = await Ctx.ResumeSessionAsync(
            client2,
            sessionId,
            CreateAppResumeConfig(resumedCanvasHandler, openCanvases));

        var restoredOpenRequest = await resumedCanvasHandler.Opened.Task.WaitAsync(EventTimeout);
        Assert.Equal("app-counter-1", restoredOpenRequest.InstanceId);
        Assert.Equal(40, restoredOpenRequest.Input!.Value.GetProperty("start").GetInt32());

        var restoredCanvas = Assert.Single((await session2.Rpc.Canvas.ListOpenAsync()).OpenCanvases);
        Assert.Equal("app-counter-1", restoredCanvas.InstanceId);
        Assert.Equal("app-counter", restoredCanvas.CanvasId);
        Assert.Equal(40, restoredCanvas.Input!.Value.GetProperty("start").GetInt32());

        var action = await session2.Rpc.Canvas.Action.InvokeAsync(
            instanceId: "app-counter-1",
            actionName: "increment",
            input: new Dictionary<string, object> { ["delta"] = 2 });
        Assert.Equal(42, action.Result!.Value.GetProperty("count").GetInt32());
        Assert.Single(resumedCanvasHandler.ActionRequests);

        var response = await session2.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Call app_host_lookup with key ALPHA, then reply with exactly its result.",
        });
        Assert.Contains("APP_HOST_VALUE_ALPHA", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        var events = await session2.GetEventsAsync();
        Assert.Contains(events.OfType<SessionInfoEvent>(), evt => evt.Data.Message == "APP_HOST_STATE_MARKER");
        Assert.Single(events.OfType<SessionResumeEvent>());
    }

    [Fact]
    public async Task Should_Propagate_Canvas_Handler_Error()
    {
        var handler = new AppCanvasHandler { ThrowOnAction = true };
        await using var session = await CreateSessionAsync(CreateAppSessionConfig(handler));
        var canvas = Assert.Single((await session.Rpc.Canvas.ListAsync()).Canvases);
        await session.Rpc.Canvas.OpenAsync(
            canvasId: "app-counter",
            instanceId: "app-counter-error",
            extensionId: canvas.ExtensionId,
            input: new Dictionary<string, object> { ["start"] = 0 });

        var exception = await Assert.ThrowsAnyAsync<Exception>(() =>
            session.Rpc.Canvas.Action.InvokeAsync(
                instanceId: "app-counter-error",
                actionName: "increment",
                input: new Dictionary<string, object> { ["delta"] = 1 }));

        Assert.Contains("The app canvas could not increment.", exception.ToString(), StringComparison.Ordinal);
    }

    private static SessionConfig CreateAppSessionConfig(AppCanvasHandler canvasHandler, bool includeTool = true)
    {
        var config = new SessionConfig
        {
            Streaming = true,
            OnPermissionRequest = PermissionHandler.ApproveAll,
            RequestCanvasRenderer = true,
            CanvasProvider = new CanvasProviderIdentity
            {
                Id = "app:builtin:test-window",
                Name = "GitHub App",
            },
            Canvases =
            [
                new CanvasDeclaration
                {
                    Id = "app-counter",
                    DisplayName = "App Counter",
                    Description = "Represents an app-hosted canvas.",
                    Actions =
                    [
                        new CanvasAction
                        {
                            Name = "increment",
                            Description = "Increments the counter.",
                        },
                    ],
                },
            ],
            CanvasHandler = canvasHandler,
        };
        if (includeTool)
        {
            config.Tools = [AIFunctionFactory.Create(AppHostLookup, "app_host_lookup")];
        }

        return config;
    }

    private static ResumeSessionConfig CreateAppResumeConfig(
        AppCanvasHandler canvasHandler,
        IList<OpenCanvasInstance> openCanvases)
    {
        return new ResumeSessionConfig
        {
            Streaming = true,
            ContinuePendingWork = false,
            Tools = [AIFunctionFactory.Create(AppHostLookup, "app_host_lookup")],
            OnPermissionRequest = PermissionHandler.ApproveAll,
            RequestCanvasRenderer = true,
            CanvasProvider = new CanvasProviderIdentity
            {
                Id = "app:builtin:test-window",
                Name = "GitHub App",
            },
            Canvases =
            [
                new CanvasDeclaration
                {
                    Id = "app-counter",
                    DisplayName = "App Counter",
                    Description = "Represents an app-hosted canvas.",
                    Actions =
                    [
                        new CanvasAction
                        {
                            Name = "increment",
                            Description = "Increments the counter.",
                        },
                    ],
                },
            ],
            CanvasHandler = canvasHandler,
            OpenCanvases = openCanvases,
        };
    }

    [Description("Looks up app-owned host state")]
    private static string AppHostLookup([Description("Lookup key")] string key) =>
        $"APP_HOST_VALUE_{key.ToUpperInvariant()}";

    private sealed class AppCanvasHandler : CanvasHandlerBase
    {
        public bool ThrowOnAction { get; init; }
        public TaskCompletionSource<CanvasProviderOpenRequest> Opened { get; } =
            new(TaskCreationOptions.RunContinuationsAsynchronously);
        public List<CanvasProviderInvokeActionRequest> ActionRequests { get; } = [];

        public override Task<CanvasProviderOpenResult> OnOpenAsync(
            CanvasProviderOpenRequest request,
            CancellationToken cancellationToken)
        {
            Opened.TrySetResult(request);
            return Task.FromResult(new CanvasProviderOpenResult
            {
                Status = "ready",
                Title = "App Counter",
                Url = $"https://example.test/canvas/{request.InstanceId}",
            });
        }

        public override Task<object?> OnActionAsync(
            CanvasProviderInvokeActionRequest request,
            CancellationToken cancellationToken)
        {
            if (ThrowOnAction)
            {
                throw new CanvasException(
                    "app_canvas_action_failed",
                    "The app canvas could not increment.");
            }

            ActionRequests.Add(request);
            var delta = request.Input!.Value.GetProperty("delta").GetInt32();
            using var result = JsonDocument.Parse($$"""{"count":{{40 + delta}}}""");
            return Task.FromResult<object?>(result.RootElement.Clone());
        }
    }
}
