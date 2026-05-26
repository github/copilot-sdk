/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using System.Text.Json;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class RpcTasksAndHandlersE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_tasks_and_handlers", output)
{
    private static async Task AssertImplementedFailureAsync(Func<Task> action, string method)
    {
        var ex = await Assert.ThrowsAnyAsync<Exception>(action);
        Assert.DoesNotContain($"Unhandled method {method}", ex.ToString(), StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Should_List_Task_State_And_Return_False_For_Missing_Task_Operations()
    {
        var session = await CreateSessionAsync();

        var tasks = await session.Rpc.Tasks.ListAsync();
        Assert.NotNull(tasks.Tasks);
        Assert.Empty(tasks.Tasks);

        var refresh = await session.Rpc.Tasks.RefreshAsync();
        Assert.NotNull(refresh);

        using var waitCts = new CancellationTokenSource(TimeSpan.FromSeconds(30));
        var waitForPending = await session.Rpc.Tasks.WaitForPendingAsync(waitCts.Token);
        Assert.NotNull(waitForPending);

        var progress = await session.Rpc.Tasks.GetProgressAsync("missing-task");
        Assert.Null(progress.Progress);

        var currentPromotable = await session.Rpc.Tasks.GetCurrentPromotableAsync();
        Assert.Null(currentPromotable.Task);

        var promote = await session.Rpc.Tasks.PromoteToBackgroundAsync("missing-task");
        Assert.False(promote.Promoted);

        var promoteCurrent = await session.Rpc.Tasks.PromoteCurrentToBackgroundAsync();
        Assert.Null(promoteCurrent.Task);

        var cancel = await session.Rpc.Tasks.CancelAsync("missing-task");
        Assert.False(cancel.Cancelled);

        var remove = await session.Rpc.Tasks.RemoveAsync("missing-task");
        Assert.False(remove.Removed);

        var sendMessage = await session.Rpc.Tasks.SendMessageAsync("missing-task", "hello from the SDK E2E test");
        Assert.False(sendMessage.Sent);
        Assert.False(string.IsNullOrWhiteSpace(sendMessage.Error));
    }

    [Fact]
    public async Task Should_Report_Implemented_Error_For_Missing_Task_Agent_Type()
    {
        var session = await CreateSessionAsync();

        await AssertImplementedFailureAsync(
            () => session.Rpc.Tasks.StartAgentAsync(
                agentType: "missing-agent-type",
                prompt: "Say hi",
                name: "sdk-test-task"),
            "session.tasks.startAgent");
    }

    [Fact]
    public async Task Should_Report_Implemented_Error_For_Invalid_Task_Agent_Model()
    {
        var session = await CreateSessionAsync();

        await AssertImplementedFailureAsync(
            () => session.Rpc.Tasks.StartAgentAsync(
                agentType: "general-purpose",
                prompt: "Say hi",
                name: "sdk-test-task",
                description: "SDK task agent validation",
                model: "not-a-real-model"),
            "session.tasks.startAgent");

        var tasks = await session.Rpc.Tasks.ListAsync();
        Assert.Empty(tasks.Tasks);
    }

    [Fact]
    public async Task Should_Start_Background_Agent_And_Report_Task_Details()
    {
        var session = await CreateSessionAsync();

        var ready = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Reply with TASK_AGENT_READY exactly.",
        });
        Assert.Contains("TASK_AGENT_READY", ready?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        var taskCompletionNotification =
            new TaskCompletionSource<AssistantMessageEvent>(TaskCreationOptions.RunContinuationsAsynchronously);
        using var subscription = session.On<SessionEvent>(evt =>
        {
            switch (evt)
            {
                case AssistantMessageEvent assistantMessage
                    when assistantMessage.Data.Content?.Contains("TASK_AGENT_DONE", StringComparison.Ordinal) == true:
                    taskCompletionNotification.TrySetResult(assistantMessage);
                    break;
                case SessionErrorEvent error:
                    taskCompletionNotification.TrySetException(new Exception(error.Data.Message ?? "session error"));
                    break;
            }
        });

        var started = await session.Rpc.Tasks.StartAgentAsync(
            agentType: "general-purpose",
            prompt: "Reply with TASK_AGENT_DONE exactly.",
            name: "sdk-background-agent",
            description: "SDK background agent coverage");
        Assert.False(string.IsNullOrWhiteSpace(started.AgentId));

        TaskInfoAgent? task = null;
        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                task = await FindAgentTaskAsync(session, started.AgentId);
                return task is not null;
            },
            timeout: TimeSpan.FromSeconds(30),
            timeoutMessage: $"Background agent task '{started.AgentId}' did not appear in session.tasks.list.");

        Assert.NotNull(task);
        Assert.Equal(started.AgentId, task.Id);
        Assert.Equal("general-purpose", task.AgentType);
        Assert.Equal("Reply with TASK_AGENT_DONE exactly.", task.Prompt);
        Assert.Equal("SDK background agent coverage", task.Description);
        Assert.Equal(GitHub.Copilot.Rpc.TaskExecutionMode.Background, task.ExecutionMode);
        Assert.False(task.CanPromoteToBackground.GetValueOrDefault());
        Assert.NotEqual(default, task.StartedAt);

        var promote = await session.Rpc.Tasks.PromoteToBackgroundAsync(started.AgentId);
        Assert.False(promote.Promoted);

        await TestHelper.WaitForConditionAsync(
            async () =>
            {
                task = await FindAgentTaskAsync(session, started.AgentId);
                return task?.LatestResponse?.Contains("TASK_AGENT_DONE", StringComparison.Ordinal) == true
                    || task?.Result?.Contains("TASK_AGENT_DONE", StringComparison.Ordinal) == true
                    || task?.Status == GitHub.Copilot.Rpc.TaskStatus.Completed
                    || task?.Status == GitHub.Copilot.Rpc.TaskStatus.Failed;
            },
            timeout: TimeSpan.FromSeconds(60),
            timeoutMessage: $"Background agent task '{started.AgentId}' did not produce a final observable state.");

        Assert.NotNull(task);
        Assert.Contains("TASK_AGENT_DONE", task.LatestResponse ?? task.Result ?? string.Empty);
        await taskCompletionNotification.Task.WaitAsync(TimeSpan.FromSeconds(30));

        if (task.Status == GitHub.Copilot.Rpc.TaskStatus.Idle)
        {
            var cancel = await session.Rpc.Tasks.CancelAsync(started.AgentId);
            Assert.True(cancel.Cancelled);
        }

        var remove = await session.Rpc.Tasks.RemoveAsync(started.AgentId);
        Assert.True(remove.Removed);

        var afterRemove = await session.Rpc.Tasks.ListAsync();
        Assert.DoesNotContain(afterRemove.Tasks.OfType<TaskInfoAgent>(), t => string.Equals(t.Id, started.AgentId, StringComparison.Ordinal));
    }

    [Fact]
    public async Task Should_Return_Expected_Results_For_Missing_Pending_Handler_RequestIds()
    {
        var session = await CreateSessionAsync();

        var tool = await session.Rpc.Tools.HandlePendingToolCallAsync(
            requestId: "missing-tool-request",
            result: JsonDocument.Parse("\"tool result\"").RootElement.Clone());
        Assert.False(tool.Success);

        var command = await session.Rpc.Commands.HandlePendingCommandAsync(
            requestId: "missing-command-request",
            error: "command error");
        Assert.True(command.Success);

        var elicitation = await session.Rpc.Ui.HandlePendingElicitationAsync(
            requestId: "missing-elicitation-request",
            result: new UIElicitationResponse { Action = UIElicitationResponseAction.Cancel });
        Assert.False(elicitation.Success);

        var userInput = await session.Rpc.Ui.HandlePendingUserInputAsync(
            requestId: "missing-user-input-request",
            response: new UIUserInputResponse { Answer = "typed answer", WasFreeform = true });
        Assert.False(userInput.Success);

        var sampling = await session.Rpc.Ui.HandlePendingSamplingAsync(
            requestId: "missing-sampling-request",
            response: new UIHandlePendingSamplingResponse());
        Assert.False(sampling.Success);

        var autoModeSwitch = await session.Rpc.Ui.HandlePendingAutoModeSwitchAsync(
            requestId: "missing-auto-mode-switch-request",
            response: UIAutoModeSwitchResponse.No);
        Assert.False(autoModeSwitch.Success);

        var exitPlanMode = await session.Rpc.Ui.HandlePendingExitPlanModeAsync(
            requestId: "missing-exit-plan-mode-request",
            response: new UIExitPlanModeResponse
            {
                Approved = false,
                Feedback = "No pending plan approval",
                SelectedAction = UIExitPlanModeAction.ExitOnly,
            });
        Assert.False(exitPlanMode.Success);

        var permission = await session.Rpc.Permissions.HandlePendingPermissionRequestAsync(
            requestId: "missing-permission-request",
            result: new PermissionDecisionReject { Feedback = "not approved" });
        Assert.False(permission.Success);

        var permanentPermission = await session.Rpc.Permissions.HandlePendingPermissionRequestAsync(
            requestId: "missing-permanent-permission-request",
            result: new PermissionDecisionApprovePermanently { Domain = "example.com" });
        Assert.False(permanentPermission.Success);

        var sessionApproval = await session.Rpc.Permissions.HandlePendingPermissionRequestAsync(
            requestId: "missing-session-approval-request",
            result: new PermissionDecisionApproveForSession
            {
                Approval = new PermissionDecisionApproveForSessionApprovalCustomTool
                {
                    ToolName = "missing-tool",
                },
            });
        Assert.False(sessionApproval.Success);

        var locationApproval = await session.Rpc.Permissions.HandlePendingPermissionRequestAsync(
            requestId: "missing-location-approval-request",
            result: new PermissionDecisionApproveForLocation
            {
                Approval = new PermissionDecisionApproveForLocationApprovalCustomTool
                {
                    ToolName = "missing-tool",
                },
                LocationKey = "missing-location",
            });
        Assert.False(locationApproval.Success);
    }

    [Fact]
    public async Task Should_Round_Trip_Rpc_Elicitation_Through_Config_Handler()
    {
        var handlerContext = new TaskCompletionSource<ElicitationContext>(TaskCreationOptions.RunContinuationsAsynchronously);
        var session = await CreateSessionAsync(new SessionConfig
        {
            OnElicitationRequest = context =>
            {
                handlerContext.TrySetResult(context);
                return Task.FromResult(new ElicitationResult
                {
                    Action = UIElicitationResponseAction.Accept,
                    Content = new Dictionary<string, object>
                    {
                        ["answer"] = "from handler",
                        ["confirmed"] = true,
                    },
                });
            },
        });

        var schema = new UIElicitationSchema
        {
            Type = "object",
            Properties = new Dictionary<string, JsonElement>
            {
                ["answer"] = ParseJsonElement("""{"type":"string"}"""),
                ["confirmed"] = ParseJsonElement("""{"type":"boolean"}"""),
            },
            Required = ["answer"],
        };

        var response = await session.Rpc.Ui.ElicitationAsync("Need details", schema);
        var context = await handlerContext.Task.WaitAsync(TimeSpan.FromSeconds(30));

        Assert.Equal(session.SessionId, context.SessionId);
        Assert.Equal("Need details", context.Message);
        Assert.NotNull(context.RequestedSchema);
        Assert.Equal("object", context.RequestedSchema.Type);
        Assert.Contains("answer", context.RequestedSchema.Properties.Keys);
        Assert.Contains("confirmed", context.RequestedSchema.Properties.Keys);
        Assert.Equal(["answer"], context.RequestedSchema.Required);

        Assert.Equal(UIElicitationResponseAction.Accept, response.Action);
        Assert.NotNull(response.Content);
        Assert.Equal("from handler", response.Content["answer"].GetString());
        Assert.True(response.Content["confirmed"].GetBoolean());
    }

    [Fact]
    public async Task Should_Register_And_Unregister_Direct_Auto_Mode_Switch_Handler()
    {
        var session = await CreateSessionAsync();

        var missing = await session.Rpc.Ui.UnregisterDirectAutoModeSwitchHandlerAsync("missing-direct-auto-mode-handle");
        Assert.False(missing.Unregistered);

        var registration = await session.Rpc.Ui.RegisterDirectAutoModeSwitchHandlerAsync();
        Assert.False(string.IsNullOrWhiteSpace(registration.Handle));

        var unregister = await session.Rpc.Ui.UnregisterDirectAutoModeSwitchHandlerAsync(registration.Handle);
        Assert.True(unregister.Unregistered);

        var unregisterAgain = await session.Rpc.Ui.UnregisterDirectAutoModeSwitchHandlerAsync(registration.Handle);
        Assert.False(unregisterAgain.Unregistered);
    }

    private static async Task<TaskInfoAgent?> FindAgentTaskAsync(CopilotSession session, string agentId)
    {
        var tasks = await session.Rpc.Tasks.ListAsync();
        return tasks.Tasks.OfType<TaskInfoAgent>().SingleOrDefault(t => string.Equals(t.Id, agentId, StringComparison.Ordinal));
    }

    private static JsonElement ParseJsonElement(string json)
    {
        using var document = JsonDocument.Parse(json);
        return document.RootElement.Clone();
    }
}
