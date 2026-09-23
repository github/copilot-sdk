/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class ScenarioTestingEmptyRuntimeE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : ScenarioTestingE2ETestBase(fixture, "scenario_testing_empty_runtime", output)
{
    [Fact]
    public async Task Empty_Mode_Minimal_Toolless_Session_Has_No_Tools()
    {
        await using var client = Ctx.CreateClient(options: new CopilotClientOptions
        {
            Mode = CopilotClientMode.Empty,
            BaseDirectory = Ctx.HomeDir,
        });
        await using var session = await Ctx.CreateSessionAsync(client, new SessionConfig
        {
            AvailableTools = new ToolSet(),
            OnPermissionRequest = PermissionHandler.ApproveAll,
            SystemMessage = new SystemMessageConfig
            {
                Mode = SystemMessageMode.Replace,
                Content = "Reply to every request with exactly EMPTY_SCENARIO_READY.",
            },
        });

        var response = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Start." });
        Assert.Contains("EMPTY_SCENARIO_READY", response?.Data.Content ?? string.Empty, StringComparison.Ordinal);

        var exchanges = await Ctx.GetExchangesAsync();
        Assert.Empty(GetToolNames(exchanges[^1]));
        Assert.DoesNotContain("Current working directory:", GetSystemMessage(exchanges[^1]), StringComparison.OrdinalIgnoreCase);
    }
}
