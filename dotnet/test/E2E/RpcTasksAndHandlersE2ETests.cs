/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

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

        var promote = await session.Rpc.Tasks.PromoteToBackgroundAsync("missing-task");
        Assert.False(promote.Promoted);

        var cancel = await session.Rpc.Tasks.CancelAsync("missing-task");
        Assert.False(cancel.Cancelled);

        var remove = await session.Rpc.Tasks.RemoveAsync("missing-task");
        Assert.False(remove.Removed);
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
        Assert.Equal(TaskAgentInfoExecutionMode.Background, task.ExecutionMode);
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
                    || task?.Status == TaskAgentInfoStatus.Completed
                    || task?.Status == TaskAgentInfoStatus.Failed;
            },
            timeout: TimeSpan.FromSeconds(60),
            timeoutMessage: $"Background agent task '{started.AgentId}' did not produce a final observable state.");

        Assert.NotNull(task);
        Assert.Contains("TASK_AGENT_DONE", task.LatestResponse ?? task.Result ?? string.Empty);

        if (task.Status == TaskAgentInfoStatus.Idle)
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
            result: "tool result");
        Assert.False(tool.Success);

        var command = await session.Rpc.Commands.HandlePendingCommandAsync(
            requestId: "missing-command-request",
            error: "command error");
        Assert.True(command.Success);

        var elicitation = await session.Rpc.Ui.HandlePendingElicitationAsync(
            requestId: "missing-elicitation-request",
            result: new UIElicitationResponse { Action = UIElicitationResponseAction.Cancel });
        Assert.False(elicitation.Success);

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

    private static async Task<TaskInfoAgent?> FindAgentTaskAsync(CopilotSession session, string agentId)
    {
        var tasks = await session.Rpc.Tasks.ListAsync();
        return tasks.Tasks.OfType<TaskInfoAgent>().SingleOrDefault(t => string.Equals(t.Id, agentId, StringComparison.Ordinal));
    }
}
