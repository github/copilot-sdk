/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Microsoft.Extensions.AI;
using System.ComponentModel;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ExternalToolCancellationE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "external_tool_cancellation", output)
{
    [Fact]
    public async Task Should_Cancel_Tool_Handler_When_Session_Disposes()
    {
        var toolStarted = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);
        var toolCancelled = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
        var releaseTool = new TaskCompletionSource<string>(TaskCreationOptions.RunContinuationsAsynchronously);

        var session = await CreateSessionAsync(new SessionConfig
        {
            Tools = [AIFunctionFactory.Create(SlowTool, "slow_analysis")],
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        _ = session.SendAsync(new MessageOptions
        {
            Prompt = "Use slow_analysis with value 'test_abort'. Wait for the result.",
        });

        var startedValue = await toolStarted.Task.WaitAsync(TimeSpan.FromSeconds(60));
        Assert.Equal("test_abort", startedValue);

        await session.DisposeAsync();
        await toolCancelled.Task.WaitAsync(TimeSpan.FromSeconds(60));

        releaseTool.TrySetResult("RELEASED");

        [Description("A slow analysis tool that blocks until released")]
        async Task<string> SlowTool([Description("Value to analyze")] string value, CancellationToken cancellationToken)
        {
            toolStarted.TrySetResult(value);
            try
            {
                var completed = await Task.WhenAny(releaseTool.Task, Task.Delay(Timeout.Infinite, cancellationToken));
                if (completed == releaseTool.Task)
                {
                    return await releaseTool.Task;
                }

                throw new OperationCanceledException(cancellationToken);
            }
            catch (OperationCanceledException)
            {
                toolCancelled.TrySetResult(true);
                throw;
            }
        }
    }
}
