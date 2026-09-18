/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Microsoft.Extensions.AI;
using System.ComponentModel;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ProductionUsageControlStateE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ProductionUsageE2ETestBase(fixture, "production_usage_control_state", output)
{
    private static readonly TimeSpan EventTimeout = TimeSpan.FromSeconds(60);

    [Fact]
    public async Task Should_Compose_Mode_Name_Plan_Client_Metadata_And_Objective_State()
    {
        await using var session = await CreateSessionAsync();
        const string sessionName = "App control state";
        const string plan = "# App plan\n- Verify control state";
        const string objective = """{"objective":"VERIFY_APP_CONTROL","status":"active"}""";

        await session.Rpc.Mode.SetAsync(SessionMode.Plan);
        await session.Rpc.Name.SetAsync(sessionName);
        await session.Rpc.Plan.UpdateAsync(plan);
        var metadata = await session.Rpc.Metadata.UpdateClientMetadataAsync(
            set: new Dictionary<string, string>
            {
                ["production-client/control-mode"] = "plan",
                ["production-client/objective"] = "VERIFY_APP_CONTROL",
            });
        var objectiveWrite = await session.Rpc.Workspaces.WriteAutopilotObjectiveAsync(objective);

        Assert.Equal("create", objectiveWrite.Operation);
        Assert.True((await session.Rpc.Workspaces.AutopilotObjectiveExistsAsync()).Exists);
        Assert.Equal(objective, (await session.Rpc.Workspaces.ReadAutopilotObjectiveAsync()).Content);
        Assert.Equal(plan, (await session.Rpc.Plan.ReadAsync()).Content);
        Assert.Equal(sessionName, (await session.Rpc.Name.GetAsync()).Name);
        Assert.Equal("VERIFY_APP_CONTROL", metadata["production-client/objective"]);

        var snapshot = await session.Rpc.Metadata.SnapshotAsync();
        Assert.Equal(session.SessionId, snapshot.SessionId);
        Assert.Equal(MetadataSnapshotCurrentMode.Plan, snapshot.CurrentMode);
        Assert.Null(snapshot.InitialName);

        var deleted = await session.Rpc.Workspaces.DeleteAutopilotObjectiveAsync();
        Assert.True(deleted.Deleted);
        Assert.False((await session.Rpc.Workspaces.AutopilotObjectiveExistsAsync()).Exists);
    }

    [Fact]
    public async Task Should_Report_Processing_While_App_Tool_Is_Running()
    {
        var toolStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseTool = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(WaitForAppAsync, "wait_for_app_control")],
        });

        Assert.False((await session.Rpc.Metadata.IsProcessingAsync()).Processing);

        try
        {
            var idle = TestHelper.GetNextEventOfTypeAsync<SessionIdleEvent>(session, EventTimeout);
            await session.SendAsync(new MessageOptions
            {
                Prompt = "Call wait_for_app_control, then reply with exactly APP_CONTROL_DONE.",
            });
            await toolStarted.Task.WaitAsync(EventTimeout);

            Assert.True((await session.Rpc.Metadata.IsProcessingAsync()).Processing);
            var activity = await session.Rpc.Metadata.ActivityAsync();
            Assert.True(activity.HasActiveWork);
            Assert.True(activity.Abortable);

            releaseTool.TrySetResult("APP_CONTROL_DONE");
            await idle;

            await TestHelper.WaitForConditionAsync(
                async () => !(await session.Rpc.Metadata.IsProcessingAsync()).Processing,
                timeout: EventTimeout,
                timeoutMessage: "Timed out waiting for processing metadata to return to idle.");

            Assert.False((await session.Rpc.Metadata.ActivityAsync()).HasActiveWork);
        }
        finally
        {
            releaseTool.TrySetResult("APP_CONTROL_DONE");
        }

        [Description("Waits for the app controller to release the active turn")]
        async Task<string> WaitForAppAsync(CancellationToken cancellationToken)
        {
            toolStarted.TrySetResult();
            return await releaseTool.Task.WaitAsync(Timeout.InfiniteTimeSpan, cancellationToken);
        }
    }
}
