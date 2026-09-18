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

[Trait(E2ETestTraits.Backend, E2ETestTraits.CapiOnly)]
public class GitHubAppCallbacksE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "github_app_callbacks", output)
{
    private const string ModeHandlerToken = "github-app-mode-handler-token";
    private const string AutoModePrompt = "Explain that the GitHub app recovered from a rate limit in one short sentence.";

    [Fact]
    public async Task Should_Run_App_Prompt_And_Tool_Hooks_With_Full_Context_And_Suppression()
    {
        UserPromptSubmittedHookInput? submitted = null;
        UserPromptTransformedHookInput? transformed = null;
        PreToolUseHookInput? preTool = null;
        PostToolUseHookInput? postTool = null;
        CopilotSession? session = null;

        session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(AppHookTool, "app_hook_tool")],
            Hooks = new SessionHooks
            {
                OnUserPromptSubmitted = (input, invocation) =>
                {
                    Assert.Equal(session!.SessionId, invocation.SessionId);
                    submitted = input;
                    return Task.FromResult<UserPromptSubmittedHookOutput?>(new UserPromptSubmittedHookOutput
                    {
                        SuppressOutput = true,
                    });
                },
                OnUserPromptTransformed = (input, invocation) =>
                {
                    Assert.Equal(session!.SessionId, invocation.SessionId);
                    transformed = input;
                    return Task.FromResult<UserPromptTransformedHookOutput?>(new UserPromptTransformedHookOutput
                    {
                        ModifiedTransformedPrompt =
                            "Call app_hook_tool with value 'original', then reply with exactly APP_POST_RESULT.",
                    });
                },
                OnPreToolUse = (input, invocation) =>
                {
                    if (input.ToolName != "app_hook_tool")
                    {
                        return Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput
                        {
                            PermissionDecision = "allow",
                        });
                    }

                    Assert.Equal(session!.SessionId, invocation.SessionId);
                    preTool = input;
                    return Task.FromResult<PreToolUseHookOutput?>(new PreToolUseHookOutput
                    {
                        PermissionDecision = "allow",
                        ModifiedArgs = new Dictionary<string, object> { ["value"] = "pre-hook" },
                        SuppressOutput = false,
                    });
                },
                OnPostToolUse = (input, invocation) =>
                {
                    if (input.ToolName != "app_hook_tool")
                    {
                        return Task.FromResult<PostToolUseHookOutput?>(null);
                    }

                    Assert.Equal(session!.SessionId, invocation.SessionId);
                    postTool = input;
                    return Task.FromResult<PostToolUseHookOutput?>(new PostToolUseHookOutput
                    {
                        ModifiedResult = new ToolResultObject
                        {
                            TextResultForLlm = "APP_POST_RESULT",
                            ResultType = "success",
                            ToolTelemetry = new Dictionary<string, object>(),
                        },
                        SuppressOutput = false,
                    });
                },
            },
        });

        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Original hidden app hook prompt.",
            DisplayPrompt = "Run app hook pipeline",
            Source = MessageSource.Agent("github-app"),
        });

        AssertHookContext(submitted, session.SessionId);
        AssertHookContext(transformed, session.SessionId);
        AssertHookContext(preTool, session.SessionId);
        AssertHookContext(postTool, session.SessionId);
        Assert.Equal("original", preTool!.ToolArgs!.Value.GetProperty("value").GetString());
        Assert.Equal("pre-hook", postTool!.ToolArgs!.Value.GetProperty("value").GetString());
        Assert.Contains("APP_TOOL_PRE-HOOK", postTool.ToolResult!.Value.ToString(), StringComparison.OrdinalIgnoreCase);
        Assert.Contains("APP_POST_RESULT", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        [Description("Returns an app-owned hook value")]
        static string AppHookTool([Description("Value to transform")] string value) =>
            $"APP_TOOL_{value.ToUpperInvariant()}";
    }

    [Fact]
    public async Task Should_Handle_App_User_Input_And_Form_Url_Elicitation_Outcomes()
    {
        var events = new List<SessionEvent>();
        var allEventsReceived = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var (cliPath, capturePath) = await GitHubAppTestCli.CreateAsync(Ctx);
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Connection = RuntimeConnection.ForStdio(
                path: cliPath,
                args: ["--capture-file", capturePath, "--behavior", "emit-ui-events"]),
            UseLoggedInUser = false,
        });

        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnEvent = evt =>
            {
                lock (events)
                {
                    events.Add(evt);
                    if (events.Count(evt => evt is UserInputRequestedEvent or ElicitationRequestedEvent) == 4)
                    {
                        allEventsReceived.TrySetResult();
                    }
                }
            },
        });

        await allEventsReceived.Task.WaitAsync(TimeSpan.FromSeconds(30));

        UserInputRequestedEvent userInput;
        List<ElicitationRequestedEvent> elicitations;
        lock (events)
        {
            userInput = Assert.Single(events.OfType<UserInputRequestedEvent>());
            elicitations = events.OfType<ElicitationRequestedEvent>().ToList();
        }

        Assert.Equal("Choose an app action", userInput.Data.Question);
        Assert.NotNull(userInput.Data.Choices);
        Assert.Equal(["Approve", "Decline"], userInput.Data.Choices);
        Assert.True(userInput.Data.AllowFreeform);
        Assert.True((await session.Rpc.Ui.HandlePendingUserInputAsync(
            userInput.Data.RequestId,
            new UIUserInputResponse { Answer = "Approve", WasFreeform = false })).Success);

        var form = Assert.Single(elicitations, evt => evt.Data.RequestId == "app-form-accept");
        Assert.Equal(ElicitationRequestedMode.Form, form.Data.Mode);
        Assert.Equal("name", Assert.Single(form.Data.RequestedSchema!.Properties).Key);
        Assert.True((await session.Rpc.Ui.HandlePendingElicitationAsync(
            form.Data.RequestId,
            new UIElicitationResponse
            {
                Action = UIElicitationResponseAction.Accept,
                Content = new Dictionary<string, JsonElement>
                {
                    ["name"] = JsonDocument.Parse("\"Mona\"").RootElement.Clone(),
                },
            })).Success);

        var url = Assert.Single(elicitations, evt => evt.Data.RequestId == "app-url-decline");
        Assert.Equal(ElicitationRequestedMode.Url, url.Data.Mode);
        Assert.Equal("https://example.test/authorize", url.Data.Url);
        Assert.True((await session.Rpc.Ui.HandlePendingElicitationAsync(
            url.Data.RequestId,
            new UIElicitationResponse { Action = UIElicitationResponseAction.Decline })).Success);

        var cancelled = Assert.Single(elicitations, evt => evt.Data.RequestId == "app-form-cancel");
        Assert.True((await session.Rpc.Ui.HandlePendingElicitationAsync(
            cancelled.Data.RequestId,
            new UIElicitationResponse { Action = UIElicitationResponseAction.Cancel })).Success);

        var stale = await session.Rpc.Ui.HandlePendingElicitationAsync(
            "stale-app-request",
            new UIElicitationResponse { Action = UIElicitationResponseAction.Cancel });
        Assert.False(stale.Success);

        var requests = await GitHubAppTestCli.ReadRequestsAsync(capturePath);
        var userInputResponse = RequestParameters(Assert.Single(
            requests,
            request => request.GetProperty("method").GetString() == "session.ui.handlePendingUserInput"));
        Assert.Equal("app-user-input", userInputResponse.GetProperty("requestId").GetString());
        Assert.Equal("Approve", userInputResponse.GetProperty("response").GetProperty("answer").GetString());
        Assert.False(userInputResponse.GetProperty("response").GetProperty("wasFreeform").GetBoolean());

        var elicitationResponses = requests
            .Where(request => request.GetProperty("method").GetString() == "session.ui.handlePendingElicitation")
            .Select(RequestParameters)
            .ToDictionary(request => request.GetProperty("requestId").GetString()!);
        Assert.Equal("accept", elicitationResponses["app-form-accept"].GetProperty("result").GetProperty("action").GetString());
        Assert.Equal(
            "Mona",
            elicitationResponses["app-form-accept"].GetProperty("result").GetProperty("content").GetProperty("name").GetString());
        Assert.Equal("decline", elicitationResponses["app-url-decline"].GetProperty("result").GetProperty("action").GetString());
        Assert.Equal("cancel", elicitationResponses["app-form-cancel"].GetProperty("result").GetProperty("action").GetString());
        Assert.Equal("cancel", elicitationResponses["stale-app-request"].GetProperty("result").GetProperty("action").GetString());
    }

    [Fact]
    public async Task Should_Cancel_App_Host_Callback_When_Channel_Disconnects()
    {
        var callbackStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var callbackCancelled = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);

        await using var client = Ctx.CreateClient();
        var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(BlockingHostCallback, "app_host_callback")],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        _ = session.SendAsync(new MessageOptions
        {
            Prompt = "Call app_host_callback with value 'disconnect' and wait for it.",
            DisplayPrompt = "Run disconnectable app callback",
            Source = MessageSource.Agent("github-app"),
        });

        await callbackStarted.Task.WaitAsync(TimeSpan.FromSeconds(60));
        await client.ForceStopAsync();
        await callbackCancelled.Task.WaitAsync(TimeSpan.FromSeconds(60));

        [Description("Waits for app host channel cancellation")]
        async Task<string> BlockingHostCallback(
            [Description("Callback value")] string value,
            CancellationToken cancellationToken)
        {
            Assert.Equal("disconnect", value);
            callbackStarted.TrySetResult();
            try
            {
                await Task.Delay(Timeout.Infinite, cancellationToken);
                return "UNREACHABLE";
            }
            catch (OperationCanceledException)
            {
                callbackCancelled.TrySetResult();
                throw;
            }
        }
    }

    [Fact]
    public async Task Should_Approve_App_Exit_Plan_With_Full_Callback_And_Event_State()
    {
        const string summary = "GitHub app implementation plan";
        await ConfigureAuthenticatedUserAsync();

        var callback = new TaskCompletionSource<(ExitPlanModeRequest Request, ExitPlanModeInvocation Invocation)>(
            TaskCreationOptions.RunContinuationsAsynchronously);
        await using var client = CreateAuthenticatedClient();
        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            GitHubToken = ModeHandlerToken,
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnExitPlanModeRequest = (request, invocation) =>
            {
                callback.TrySetResult((request, invocation));
                return Task.FromResult(new ExitPlanModeResult
                {
                    Approved = true,
                    SelectedAction = "interactive",
                    Feedback = "Approved by the GitHub app",
                });
            },
        });

        var userMessageTask = TestHelper.GetNextEventOfTypeAsync<UserMessageEvent>(
            session,
            evt => evt.Data.Source == "agent-github-app",
            TimeSpan.FromSeconds(30),
            timeoutDescription: "GitHub app exit-plan user message");
        var requestedTask = TestHelper.GetNextEventOfTypeAsync<ExitPlanModeRequestedEvent>(
            session,
            evt => evt.Data.Summary == summary,
            TimeSpan.FromSeconds(30),
            timeoutDescription: "GitHub app exit-plan request");
        var completedTask = TestHelper.GetNextEventOfTypeAsync<ExitPlanModeCompletedEvent>(
            session,
            evt => evt.Data.Approved == true &&
                evt.Data.SelectedAction.GetValueOrDefault() == ExitPlanModeAction.Interactive,
            TimeSpan.FromSeconds(30),
            timeoutDescription: "GitHub app exit-plan completion");

        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            AgentMode = AgentMode.Plan,
            Prompt = "Create a GitHub app plan, then request approval with exit_plan_mode.",
            DisplayPrompt = "Review proposed GitHub app plan",
            Source = MessageSource.Agent("github-app"),
        }, timeout: TimeSpan.FromSeconds(120));

        var userMessage = await userMessageTask;
        Assert.Equal("Review proposed GitHub app plan", userMessage.Data.Content);
        Assert.Equal(UserMessageAgentMode.Plan, userMessage.Data.AgentMode);

        var (request, invocation) = await callback.Task.WaitAsync(TimeSpan.FromSeconds(30));
        Assert.Equal(session.SessionId, invocation.SessionId);
        Assert.Equal(summary, request.Summary);
        Assert.Equal(["autopilot", "interactive", "exit_only"], request.Actions);
        Assert.Equal("interactive", request.RecommendedAction);
        Assert.NotNull(request.PlanContent);

        var requested = await requestedTask;
        Assert.Equal(request.Summary, requested.Data.Summary);
        Assert.Equal(request.Actions, requested.Data.Actions.Select(action => action.Value));
        Assert.Equal(request.RecommendedAction, requested.Data.RecommendedAction.Value);

        var completed = await completedTask;
        Assert.True(completed.Data.Approved);
        Assert.Equal(ExitPlanModeAction.Interactive, completed.Data.SelectedAction);
        Assert.Equal("Approved by the GitHub app", completed.Data.Feedback);
        Assert.NotNull(response);
    }

    [Fact]
    public async Task Should_Auto_Switch_App_Mode_After_Rate_Limit()
    {
        await ConfigureAuthenticatedUserAsync();

        var callback = new TaskCompletionSource<(AutoModeSwitchRequest Request, AutoModeSwitchInvocation Invocation)>(
            TaskCreationOptions.RunContinuationsAsynchronously);
        await using var client = CreateAuthenticatedClient();
        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            GitHubToken = ModeHandlerToken,
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnAutoModeSwitchRequest = (request, invocation) =>
            {
                callback.TrySetResult((request, invocation));
                return Task.FromResult(AutoModeSwitchResponse.Yes);
            },
        });

        const long expectedRetryAfter = 1;
        var userMessageTask = GetNextEventAllowingRateLimitAsync<UserMessageEvent>(
            session,
            evt => evt.Data.Source == "agent-github-app",
            "GitHub app auto-switch user message");
        var requestedTask = GetNextEventAllowingRateLimitAsync<AutoModeSwitchRequestedEvent>(
            session,
            evt => evt.Data.ErrorCode == "user_weekly_rate_limited" &&
                evt.Data.RetryAfterSeconds == expectedRetryAfter,
            "GitHub app auto-switch request");
        var completedTask = GetNextEventAllowingRateLimitAsync<AutoModeSwitchCompletedEvent>(
            session,
            evt => evt.Data.Response == AutoModeSwitchResponse.Yes,
            "GitHub app auto-switch completion");
        var modelChangeTask = GetNextEventAllowingRateLimitAsync<SessionModelChangeEvent>(
            session,
            evt => evt.Data.Cause == "rate_limit_auto_switch",
            "GitHub app rate-limit model change");
        var idleTask = GetNextEventAllowingRateLimitAsync<SessionIdleEvent>(
            session,
            static _ => true,
            "GitHub app auto-switch idle");

        var messageId = await session.SendAsync(new MessageOptions
        {
            Prompt = AutoModePrompt,
            DisplayPrompt = "Continue GitHub app request automatically",
            Source = MessageSource.Agent("github-app"),
        });
        Assert.NotEmpty(messageId);

        var userMessage = await userMessageTask;
        Assert.Equal("Continue GitHub app request automatically", userMessage.Data.Content);

        var (request, invocation) = await callback.Task.WaitAsync(TimeSpan.FromSeconds(30));
        Assert.Equal(session.SessionId, invocation.SessionId);
        Assert.Equal("user_weekly_rate_limited", request.ErrorCode);
        Assert.Equal(expectedRetryAfter, request.RetryAfterSeconds);

        var requested = await requestedTask;
        Assert.Equal(request.ErrorCode, requested.Data.ErrorCode);
        Assert.Equal(request.RetryAfterSeconds, requested.Data.RetryAfterSeconds);
        Assert.Equal(AutoModeSwitchResponse.Yes, (await completedTask).Data.Response);
        Assert.Equal("rate_limit_auto_switch", (await modelChangeTask).Data.Cause);
        await idleTask;
    }

    private CopilotClient CreateAuthenticatedClient()
    {
        var environment = new Dictionary<string, string>(Ctx.GetEnvironment())
        {
            ["COPILOT_DEBUG_GITHUB_API_URL"] = Ctx.ProxyUrl,
        };

        return Ctx.CreateClient(environment: environment);
    }

    private Task ConfigureAuthenticatedUserAsync() =>
        Ctx.SetCopilotUserByTokenAsync(ModeHandlerToken, new CopilotUserConfig(
            Login: "github-app-mode-handler-user",
            CopilotPlan: "individual_pro",
            Endpoints: new CopilotUserEndpoints(Api: Ctx.ProxyUrl, Telemetry: "https://localhost:1/telemetry"),
            AnalyticsTrackingId: "github-app-mode-handler-tracking-id"));

    private static async Task<T> GetNextEventAllowingRateLimitAsync<T>(
        CopilotSession session,
        Func<T, bool> predicate,
        string description) where T : SessionEvent
    {
        var result = new TaskCompletionSource<T>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(30));
        using var subscription = session.On<SessionEvent>(evt =>
        {
            if (evt is T typed && predicate(typed))
            {
                result.TrySetResult(typed);
            }
            else if (evt is SessionErrorEvent { Data.ErrorType: not "rate_limit" } error)
            {
                result.TrySetException(new Exception(error.Data.Message ?? "session error"));
            }
        });

        using var registration = timeout.Token.Register(
            () => result.TrySetException(new TimeoutException($"Timed out waiting for {description}.")));
        return await result.Task;
    }

    private static void AssertHookContext(object? input, string sessionId)
    {
        Assert.NotNull(input);
        var type = input.GetType();
        Assert.Equal(sessionId, type.GetProperty("SessionId")!.GetValue(input));
        Assert.True((DateTimeOffset)type.GetProperty("Timestamp")!.GetValue(input)! > DateTimeOffset.UnixEpoch);
        Assert.False(string.IsNullOrWhiteSpace((string)type.GetProperty("WorkingDirectory")!.GetValue(input)!));
    }

    private static JsonElement RequestParameters(JsonElement request)
    {
        var parameters = request.GetProperty("params");
        return parameters.ValueKind == JsonValueKind.Array ? parameters[0] : parameters;
    }
}
