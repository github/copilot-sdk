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
/// End-to-end coverage for representative production SDK workflows.
/// These tests intentionally compose APIs that are otherwise covered individually.
/// </summary>
public class ProductionUsageCompositionE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ProductionUsageE2ETestBase(fixture, "production_usage_composition", output)
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
                    ExtensionId = "production-client:trace-viewer",
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
        Assert.Equal("production-client:trace-viewer", attachment.ExtensionId);
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

            var finalQueuedResponse = TestHelper.GetNextEventOfTypeAsync<AssistantMessageEvent>(
                session,
                message => message.Data.Content?.Contains("QUEUED_APP_MESSAGE", StringComparison.Ordinal) == true,
                EventTimeout,
                "the queued app response");
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
            await finalQueuedResponse;

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
            CreateAppSessionConfig(originalCanvasHandler, includeTool: false, includeMcp: true));
        var sessionId = session1.SessionId;
        await WaitForMcpServerStatusAsync(session1, "app-resume-mcp", McpServerStatus.Connected);
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
        await client1.StopAsync();

        var resumedCanvasHandler = new AppCanvasHandler();
        var client2 = Ctx.CreateClient();
        await using var session2 = await Ctx.ResumeSessionAsync(
            client2,
            sessionId,
            CreateAppResumeConfig(resumedCanvasHandler, openCanvases, includeMcp: true));

        var restoredOpenRequest = await resumedCanvasHandler.Opened.Task.WaitAsync(EventTimeout);
        Assert.Equal("app-counter-1", restoredOpenRequest.InstanceId);
        Assert.Equal(40, restoredOpenRequest.Input!.Value.GetProperty("start").GetInt32());

        var restoredCanvas = await WaitForOpenCanvasAsync(session2, "app-counter-1");
        Assert.Equal("app-counter-1", restoredCanvas.InstanceId);
        Assert.Equal("app-counter", restoredCanvas.CanvasId);
        Assert.Equal(40, restoredCanvas.Input!.Value.GetProperty("start").GetInt32());

        var action = await session2.Rpc.Canvas.Action.InvokeAsync(
            instanceId: "app-counter-1",
            actionName: "increment",
            input: new Dictionary<string, object> { ["delta"] = 2 });
        Assert.Equal(42, action.Result!.Value.GetProperty("count").GetInt32());
        Assert.Single(resumedCanvasHandler.ActionRequests);

        await WaitForMcpServerStatusAsync(session2, "app-resume-mcp", McpServerStatus.Connected);
        var mcpTools = await session2.Rpc.Mcp.ListToolsAsync("app-resume-mcp");
        Assert.NotEmpty(mcpTools.Tools);

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
    [Trait(E2ETestTraits.Backend, E2ETestTraits.SelfConfiguredBackend)]
    public async Task Should_Resume_With_Reattached_App_Provider()
    {
        var initialProviderTokenRequest = new TaskCompletionSource<ProviderTokenArgs>(
            TaskCreationOptions.RunContinuationsAsynchronously);
        var initialRequestHandler = new RecordingRequestHandler();
        var client1 = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(),
            RequestHandler = initialRequestHandler,
        });
        var createConfig = new SessionConfig
        {
            Model = "app-resume-provider/app-model",
            OnPermissionRequest = PermissionHandler.ApproveAll,
        };
        ConfigureAppProvider(
            createConfig,
            args =>
            {
                initialProviderTokenRequest.TrySetResult(args);
                return Task.FromResult("initial-app-provider-token");
            });
        var session1 = await Ctx.CreateSessionAsync(client1, createConfig);
        var sessionId = session1.SessionId;
        var initialResponse = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Create persisted history before the provider resume.",
        });
        Assert.Contains(
            RecordingRequestHandler.SyntheticText,
            initialResponse?.Data.Content ?? string.Empty,
            StringComparison.Ordinal);

        var initialProviderRequest = await initialProviderTokenRequest.Task.WaitAsync(EventTimeout);
        Assert.Equal(sessionId, initialProviderRequest.SessionId);
        Assert.Equal("app-resume-provider", initialProviderRequest.ProviderName);
        Assert.Contains(
            initialRequestHandler.InferenceRequests,
            request => request.Url.StartsWith("https://app-resume.invalid/", StringComparison.Ordinal)
                && request.SessionId == sessionId);

        await session1.Rpc.SuspendAsync();
        await session1.DisposeAsync();
        await client1.StopAsync();

        var providerTokenRequest = new TaskCompletionSource<ProviderTokenArgs>(
            TaskCreationOptions.RunContinuationsAsynchronously);
        var resumedRequestHandler = new RecordingRequestHandler();
        var client2 = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(),
            RequestHandler = resumedRequestHandler,
        });
        var resumeConfig = new ResumeSessionConfig
        {
            Model = "app-resume-provider/app-model",
            OnPermissionRequest = PermissionHandler.ApproveAll,
        };
        ConfigureAppProvider(
            resumeConfig,
            args =>
            {
                providerTokenRequest.TrySetResult(args);
                return Task.FromResult("resumed-app-provider-token");
            });
        await using var session2 = await Ctx.ResumeSessionAsync(client2, sessionId, resumeConfig);

        var response = await session2.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the reattached app provider.",
        });
        Assert.Contains(
            RecordingRequestHandler.SyntheticText,
            response?.Data.Content ?? string.Empty,
            StringComparison.Ordinal);

        var providerRequest = await providerTokenRequest.Task.WaitAsync(EventTimeout);
        Assert.Equal(sessionId, providerRequest.SessionId);
        Assert.Equal("app-resume-provider", providerRequest.ProviderName);
        Assert.Contains(
            resumedRequestHandler.InferenceRequests,
            request => request.Url.StartsWith("https://app-resume.invalid/", StringComparison.Ordinal)
                && request.SessionId == sessionId);
    }

    [Fact]
    public async Task Should_Retry_Resume_On_Replacement_Client_After_Recoverable_Setup_Failure()
    {
        var originalHandler = new AppCanvasHandler();
        var client1 = Ctx.CreateClient();
        var session1 = await Ctx.CreateSessionAsync(client1, CreateAppSessionConfig(originalHandler));
        var sessionId = session1.SessionId;
        var initialResponse = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with exactly APP_RETRY_RESUME_READY.",
        });
        Assert.Contains("APP_RETRY_RESUME_READY", initialResponse?.Data.Content ?? string.Empty, StringComparison.Ordinal);
        var canvas = Assert.Single((await session1.Rpc.Canvas.ListAsync()).Canvases);
        await session1.Rpc.Canvas.OpenAsync(
            canvasId: "app-counter",
            instanceId: "app-retry-canvas",
            extensionId: canvas.ExtensionId,
            input: new Dictionary<string, object> { ["start"] = 40 });
        await TestHelper.WaitForConditionAsync(
            () => Task.FromResult(session1.OpenCanvases.Count == 1),
            timeout: EventTimeout,
            timeoutMessage: "Timed out waiting for the retry canvas snapshot.");
        var openCanvases = session1.OpenCanvases.ToList();
        await session1.LogAsync("APP_RETRY_RESUME_HISTORY");

        await session1.Rpc.SuspendAsync();
        await session1.DisposeAsync();
        await client1.StopAsync();

        var failingClient = Ctx.CreateClient();
        var failingConfig = CreateAppResumeConfig(new AppCanvasHandler(), openCanvases);
        failingConfig.Tools =
        [
            AIFunctionFactory.Create(() => "first", "duplicate_app_tool"),
            AIFunctionFactory.Create(() => "second", "duplicate_app_tool"),
        ];
        await Assert.ThrowsAnyAsync<ArgumentException>(() =>
            Ctx.ResumeSessionAsync(failingClient, sessionId, failingConfig));
        await failingClient.ForceStopAsync();

        var replacementHandler = new AppCanvasHandler();
        var replacementClient = Ctx.CreateClient();
        await using var resumed = await Ctx.ResumeSessionAsync(
            replacementClient,
            sessionId,
            CreateAppResumeConfig(replacementHandler, openCanvases));

        var reopened = await replacementHandler.Opened.Task.WaitAsync(EventTimeout);
        Assert.Equal("app-retry-canvas", reopened.InstanceId);
        await TestHelper.WaitForConditionAsync(
            async () => (await resumed.Rpc.Canvas.ListOpenAsync()).OpenCanvases.Count == 1,
            timeout: EventTimeout,
            timeoutMessage: "Timed out waiting for the replacement client to restore the open canvas.");
        Assert.Single((await resumed.Rpc.Canvas.ListOpenAsync()).OpenCanvases);
    }

    [Fact]
    public async Task Should_Not_Emit_Redundant_Model_Change_When_Resuming_Same_Model()
    {
        var client1 = Ctx.CreateClient();
        var session1 = await Ctx.CreateSessionAsync(client1, new SessionConfig
        {
            Model = "claude-sonnet-5",
        });
        var sessionId = session1.SessionId;
        Assert.Equal("claude-sonnet-5", (await session1.Rpc.Model.GetCurrentAsync()).ModelId);
        var initialResponse = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with exactly APP_SAME_MODEL_HISTORY_READY.",
        });
        Assert.Contains("APP_SAME_MODEL_HISTORY_READY", initialResponse?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        await session1.Rpc.SuspendAsync();
        await session1.DisposeAsync();
        await client1.StopAsync();

        var earlyEvents = new List<SessionEvent>();
        var client2 = Ctx.CreateClient();
        await using var resumed = await Ctx.ResumeSessionAsync(client2, sessionId, new ResumeSessionConfig
        {
            Model = "claude-sonnet-5",
            OnEvent = earlyEvents.Add,
        });

        Assert.Equal("claude-sonnet-5", (await resumed.Rpc.Model.GetCurrentAsync()).ModelId);
        var persistedEvents = await resumed.GetEventsAsync();
        Assert.DoesNotContain(earlyEvents, evt => evt is SessionModelChangeEvent);
        Assert.DoesNotContain(persistedEvents, evt => evt is SessionModelChangeEvent);
    }

    [Fact]
    public async Task Should_Read_Persisted_App_Events_Without_Resuming()
    {
        var client1 = Ctx.CreateClient();
        var session1 = await Ctx.CreateSessionAsync(client1);
        var sessionId = session1.SessionId;
        var response = await session1.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with exactly APP_PERSISTED_HISTORY.",
        });
        Assert.Contains("APP_PERSISTED_HISTORY", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        await session1.Rpc.SuspendAsync();
        await session1.DisposeAsync();
        await client1.StopAsync();

        await using var client2 = Ctx.CreateClient();
        await client2.StartAsync();
        var persisted = await client2.Rpc.Sessions.ReadPersistedEventsAsync(sessionId);

        Assert.Equal(EventsCursorStatus.Ok, persisted.CursorStatus);
        Assert.False(persisted.HasMore);
        Assert.Contains(
            persisted.Events.OfType<UserMessageEvent>(),
            evt => evt.Data.TransformedContent?.Contains("APP_PERSISTED_HISTORY", StringComparison.Ordinal) == true);
        Assert.Contains(
            persisted.Events.OfType<AssistantMessageEvent>(),
            evt => evt.Data.Content?.Contains("APP_PERSISTED_HISTORY", StringComparison.Ordinal) == true);
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

    private static SessionConfig CreateAppSessionConfig(
        AppCanvasHandler canvasHandler,
        bool includeTool = true,
        bool includeMcp = false)
    {
        var config = new SessionConfig
        {
            Streaming = true,
            OnPermissionRequest = PermissionHandler.ApproveAll,
            RequestCanvasRenderer = true,
            CanvasProvider = new CanvasProviderIdentity
            {
                Id = "app:builtin:test-window",
                Name = "production client",
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
        if (includeMcp)
        {
            config.McpServers = CreateTestMcpServers("app-resume-mcp");
        }

        return config;
    }

    private static ResumeSessionConfig CreateAppResumeConfig(
        AppCanvasHandler canvasHandler,
        IList<OpenCanvasInstance> openCanvases,
        bool includeMcp = false)
    {
        var config = new ResumeSessionConfig
        {
            Streaming = true,
            ContinuePendingWork = false,
            Tools = [AIFunctionFactory.Create(AppHostLookup, "app_host_lookup")],
            OnPermissionRequest = PermissionHandler.ApproveAll,
            RequestCanvasRenderer = true,
            CanvasProvider = new CanvasProviderIdentity
            {
                Id = "app:builtin:test-window",
                Name = "production client",
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
        if (includeMcp)
        {
            config.McpServers = CreateTestMcpServers("app-resume-mcp");
        }
        return config;
    }

    private static void ConfigureAppProvider(
        SessionConfigBase config,
        Func<ProviderTokenArgs, Task<string>>? providerTokenProvider)
    {
        if (providerTokenProvider is null)
        {
            return;
        }

        config.Providers =
        [
            new NamedProviderConfig
            {
                Name = "app-resume-provider",
                Type = "openai",
                WireApi = "responses",
                BaseUrl = "https://app-resume.invalid/v1",
                BearerTokenProvider = providerTokenProvider,
            },
        ];
        config.Models =
        [
            new ProviderModelConfig
            {
                Provider = "app-resume-provider",
                Id = "app-model",
                WireModel = "app-wire-model",
            },
        ];
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
            timeoutMessage: $"Timed out waiting for open app canvas '{instanceId}'.");
        return result!;
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
