/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;
using GitHub.Copilot.SDK.Rpc;

namespace GitHub.Copilot.SDK.Test;

/// <summary>
/// Covers generated RPC methods that do not fit the smaller namespace-specific E2E files.
/// </summary>
public class AdditionalRpcTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "additional_rpc", output)
{
    private static async Task AssertImplementedFailureAsync(Func<Task> action, string method)
    {
        var ex = await Assert.ThrowsAnyAsync<Exception>(action);
        Assert.DoesNotContain($"Unhandled method {method}", ex.ToString(), StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Should_Discover_Server_Mcp_And_Skills()
    {
        await Client.StartAsync();

        var mcp = await Client.Rpc.Mcp.DiscoverAsync(workingDirectory: Ctx.WorkDir);
        Assert.NotNull(mcp.Servers);

        var skills = await Client.Rpc.Skills.DiscoverAsync(projectPaths: [Ctx.WorkDir]);
        Assert.NotNull(skills.Skills);

        try
        {
            await Client.Rpc.Skills.Config.SetDisabledSkillsAsync(["sdk-test-skill"]);
        }
        finally
        {
            await Client.Rpc.Skills.Config.SetDisabledSkillsAsync([]);
        }
    }

    [Fact]
    public async Task Should_Fork_Session()
    {
        var session = await CreateSessionAsync();

        var ex = await Assert.ThrowsAnyAsync<Exception>(() => Client.Rpc.Sessions.ForkAsync(session.SessionId));
        Assert.Contains("not found or has no persisted events", ex.ToString(), StringComparison.OrdinalIgnoreCase);
        Assert.DoesNotContain("Unhandled method sessions.fork", ex.ToString(), StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Should_Get_And_Set_Session_Metadata()
    {
        var session = await CreateSessionAsync();

        await session.Rpc.Name.SetAsync("SDK test session");
        var name = await session.Rpc.Name.GetAsync();
        Assert.Equal("SDK test session", name.Name);

        var sources = await session.Rpc.Instructions.GetSourcesAsync();
        Assert.NotNull(sources.Sources);
    }

    [Fact]
    public async Task Should_Call_Session_Tasks_Rpcs()
    {
        var session = await CreateSessionAsync();

        var tasks = await session.Rpc.Tasks.ListAsync();
        Assert.NotNull(tasks.Tasks);

        var promote = await session.Rpc.Tasks.PromoteToBackgroundAsync("missing-task");
        Assert.False(promote.Promoted);

        var cancel = await session.Rpc.Tasks.CancelAsync("missing-task");
        Assert.False(cancel.Cancelled);

        var remove = await session.Rpc.Tasks.RemoveAsync("missing-task");
        Assert.False(remove.Removed);

        await AssertImplementedFailureAsync(
            () => session.Rpc.Tasks.StartAgentAsync(
                agentType: "missing-agent-type",
                prompt: "Say hi",
                name: "sdk-test-task"),
            "session.tasks.startAgent");
    }

    [Fact]
    public async Task Should_Call_Remaining_Session_Rpcs()
    {
        var session = await CreateSessionAsync();

        await AssertImplementedFailureAsync(
            () => session.Rpc.History.TruncateAsync("missing-event"),
            "session.history.truncate");

        var metrics = await session.Rpc.Usage.GetMetricsAsync();
        Assert.True(metrics.SessionStartTime > 0);
        Assert.True(metrics.TotalNanoAiu is null or >= 0);
        if (metrics.TokenDetails is not null)
        {
            Assert.All(metrics.TokenDetails.Values, detail => Assert.True(detail.TokenCount >= 0));
        }

        Assert.All(
            metrics.ModelMetrics.Values,
            modelMetric =>
            {
                Assert.True(modelMetric.TotalNanoAiu is null or >= 0);
                if (modelMetric.TokenDetails is not null)
                {
                    Assert.All(modelMetric.TokenDetails.Values, detail => Assert.True(detail.TokenCount >= 0));
                }
            });

        try
        {
            var approveAll = await session.Rpc.Permissions.SetApproveAllAsync(true);
            Assert.True(approveAll.Success);

            var reset = await session.Rpc.Permissions.ResetSessionApprovalsAsync();
            Assert.True(reset.Success);
        }
        finally
        {
            await session.Rpc.Permissions.SetApproveAllAsync(false);
        }

        await AssertImplementedFailureAsync(
            () => session.Rpc.Mcp.Oauth.LoginAsync("missing-server"),
            "session.mcp.oauth.login");
    }

    [Fact]
    public async Task Should_Call_Pending_Handler_Rpcs_Directly()
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
