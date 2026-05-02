/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
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
    }
}
