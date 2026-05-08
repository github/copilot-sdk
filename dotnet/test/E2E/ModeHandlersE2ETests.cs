/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

public class ModeHandlersE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "mode_handlers", output)
{
    private const string Token = "mode-handler-token";
    private const string AutoModePrompt = "Explain that auto mode recovered from a rate limit in one short sentence.";

    [Fact]
    public async Task Should_Invoke_Exit_Plan_Mode_Handler_When_Model_Uses_Tool()
    {
        const string summary = "Greeting file implementation plan";
        await ConfigureAuthenticatedUserAsync();

        var handlerTask = new TaskCompletionSource<(ExitPlanModeRequest Request, ExitPlanModeInvocation Invocation)>(
            TaskCreationOptions.RunContinuationsAsynchronously);

        await using var client = CreateAuthenticatedClient();
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            GitHubToken = Token,
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnExitPlanMode = (request, invocation) =>
            {
                handlerTask.TrySetResult((request, invocation));
                return Task.FromResult(new ExitPlanModeResult
                {
                    Approved = true,
                    SelectedAction = "interactive",
                    Feedback = "Approved by the C# E2E test",
                });
            },
        });

        var requestedEventTask = TestHelper.GetNextEventOfTypeAsync<ExitPlanModeRequestedEvent>(
            session,
            evt => evt.Data.Summary == summary,
            TimeSpan.FromSeconds(30),
            timeoutDescription: "exit_plan_mode.requested event");
        var completedEventTask = TestHelper.GetNextEventOfTypeAsync<ExitPlanModeCompletedEvent>(
            session,
            evt => evt.Data.Approved == true && evt.Data.SelectedAction == "interactive",
            TimeSpan.FromSeconds(30),
            timeoutDescription: "exit_plan_mode.completed event");

        var response = await session.SendAndWaitAsync(new MessageOptions
        {
            Mode = "plan",
            Prompt = "Create a brief implementation plan for adding a greeting.txt file, then request approval with exit_plan_mode.",
        }, timeout: TimeSpan.FromSeconds(120));

        var (request, invocation) = await handlerTask.Task.WaitAsync(TimeSpan.FromSeconds(10));
        Assert.Equal(session.SessionId, invocation.SessionId);
        Assert.Equal(summary, request.Summary);
        Assert.Equal(["interactive", "autopilot", "exit_only"], request.Actions);
        Assert.Equal("interactive", request.RecommendedAction);
        Assert.NotNull(request.PlanContent);

        var requestedEvent = await requestedEventTask;
        Assert.Equal(request.Summary, requestedEvent.Data.Summary);
        Assert.Equal(request.Actions, requestedEvent.Data.Actions);
        Assert.Equal(request.RecommendedAction, requestedEvent.Data.RecommendedAction);

        var completedEvent = await completedEventTask;
        Assert.True(completedEvent.Data.Approved);
        Assert.Equal("interactive", completedEvent.Data.SelectedAction);
        Assert.Equal("Approved by the C# E2E test", completedEvent.Data.Feedback);

        Assert.NotNull(response);
    }

    [Fact]
    public async Task Should_Invoke_Auto_Mode_Switch_Handler_When_Rate_Limited()
    {
        await ConfigureAuthenticatedUserAsync();

        var handlerTask = new TaskCompletionSource<(AutoModeSwitchRequest Request, AutoModeSwitchInvocation Invocation)>(
            TaskCreationOptions.RunContinuationsAsynchronously);

        await using var client = CreateAuthenticatedClient();
        var session = await client.CreateSessionAsync(new SessionConfig
        {
            GitHubToken = Token,
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnAutoModeSwitch = (request, invocation) =>
            {
                handlerTask.TrySetResult((request, invocation));
                return Task.FromResult(AutoModeSwitchResponse.Yes);
            },
        });

        var requestedEventTask = GetNextEventOfTypeAllowingRateLimitAsync<AutoModeSwitchRequestedEvent>(
            session,
            evt => evt.Data.ErrorCode == "user_weekly_rate_limited" && evt.Data.RetryAfterSeconds == 1,
            TimeSpan.FromSeconds(30),
            timeoutDescription: "auto_mode_switch.requested event");
        var completedEventTask = GetNextEventOfTypeAllowingRateLimitAsync<AutoModeSwitchCompletedEvent>(
            session,
            evt => evt.Data.Response == "yes",
            TimeSpan.FromSeconds(30),
            timeoutDescription: "auto_mode_switch.completed event");
        var modelChangeTask = GetNextEventOfTypeAllowingRateLimitAsync<SessionModelChangeEvent>(
            session,
            evt => evt.Data.Cause == "rate_limit_auto_switch",
            TimeSpan.FromSeconds(30),
            timeoutDescription: "rate-limit auto-mode model change");
        var idleEventTask = GetNextEventOfTypeAllowingRateLimitAsync<SessionIdleEvent>(
            session,
            static _ => true,
            TimeSpan.FromSeconds(30),
            timeoutDescription: "session.idle after auto-mode switch");

        var messageId = await session.SendAsync(new MessageOptions
        {
            Prompt = AutoModePrompt,
        });
        Assert.NotEmpty(messageId);

        var (request, invocation) = await handlerTask.Task.WaitAsync(TimeSpan.FromSeconds(10));
        Assert.Equal(session.SessionId, invocation.SessionId);
        Assert.Equal("user_weekly_rate_limited", request.ErrorCode);
        Assert.Equal(1, request.RetryAfterSeconds);

        var requestedEvent = await requestedEventTask;
        Assert.Equal(request.ErrorCode, requestedEvent.Data.ErrorCode);
        Assert.Equal(request.RetryAfterSeconds, requestedEvent.Data.RetryAfterSeconds);

        var completedEvent = await completedEventTask;
        Assert.Equal("yes", completedEvent.Data.Response);

        var modelChange = await modelChangeTask;
        Assert.Equal("rate_limit_auto_switch", modelChange.Data.Cause);
        await idleEventTask;
    }

    private CopilotClient CreateAuthenticatedClient()
    {
        var env = new Dictionary<string, string>(Ctx.GetEnvironment())
        {
            ["COPILOT_DEBUG_GITHUB_API_URL"] = Ctx.ProxyUrl,
        };

        return Ctx.CreateClient(options: new CopilotClientOptions { Environment = env });
    }

    private Task ConfigureAuthenticatedUserAsync()
    {
        return Ctx.SetCopilotUserByTokenAsync(Token, new CopilotUserConfig(
            Login: "mode-handler-user",
            CopilotPlan: "individual_pro",
            Endpoints: new CopilotUserEndpoints(Api: Ctx.ProxyUrl, Telemetry: "https://localhost:1/telemetry"),
            AnalyticsTrackingId: "mode-handler-tracking-id"));
    }

    private static async Task<T> GetNextEventOfTypeAllowingRateLimitAsync<T>(
        CopilotSession session,
        Func<T, bool> predicate,
        TimeSpan? timeout = null,
        string? timeoutDescription = null) where T : SessionEvent
    {
        var tcs = new TaskCompletionSource<T>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var cts = new CancellationTokenSource(timeout ?? TimeSpan.FromSeconds(30));

        using var subscription = session.On(evt =>
        {
            if (evt is T matched && predicate(matched))
            {
                tcs.TrySetResult(matched);
            }
            else if (evt is SessionErrorEvent { Data.ErrorType: not "rate_limit" } error)
            {
                tcs.TrySetException(new Exception(error.Data.Message ?? "session error"));
            }
        });

        cts.Token.Register(() => tcs.TrySetException(
            new TimeoutException($"Timeout waiting for {timeoutDescription ?? $"event of type '{typeof(T).Name}'"}")));

        return await tcs.Task;
    }
}
