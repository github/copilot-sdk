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

public class ScenarioTestingControlStateE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_control_state", output)
{
    private static readonly TimeSpan EventTimeout = TimeSpan.FromSeconds(60);

    [Fact]
    public async Task Should_Report_Processing_While_Scenario_Tool_Is_Running()
    {
        var toolStarted = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseTool = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);

        await using var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(WaitForScenarioAsync, "wait_for_scenario_control")],
        });

        Assert.False((await session.Rpc.Metadata.IsProcessingAsync()).Processing);

        try
        {
            var idle = TestHelper.GetNextEventOfTypeAsync<SessionIdleEvent>(session, EventTimeout);
            await session.SendAsync(new MessageOptions
            {
                Prompt = "Call wait_for_scenario_control, then reply with exactly SCENARIO_CONTROL_DONE.",
            });
            await toolStarted.Task.WaitAsync(EventTimeout);

            Assert.True((await session.Rpc.Metadata.IsProcessingAsync()).Processing);
            var activity = await session.Rpc.Metadata.ActivityAsync();
            Assert.True(activity.HasActiveWork);
            Assert.True(activity.Abortable);

            releaseTool.TrySetResult("SCENARIO_CONTROL_DONE");
            await idle;

            await TestHelper.WaitForConditionAsync(
                async () => !(await session.Rpc.Metadata.IsProcessingAsync()).Processing,
                timeout: EventTimeout,
                timeoutMessage: "Timed out waiting for processing metadata to return to idle.");

            Assert.False((await session.Rpc.Metadata.ActivityAsync()).HasActiveWork);
        }
        finally
        {
            releaseTool.TrySetResult("SCENARIO_CONTROL_DONE");
        }

        [Description("Waits for the scenario controller to release the active turn")]
        async Task<string> WaitForScenarioAsync(CancellationToken cancellationToken)
        {
            toolStarted.TrySetResult();
            return await releaseTool.Task.WaitAsync(Timeout.InfiniteTimeSpan, cancellationToken);
        }
    }
}
