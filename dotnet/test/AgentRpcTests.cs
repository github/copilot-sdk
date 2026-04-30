/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class AgentRpcTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "agent_rpc", output)
{
    [Fact]
    public async Task Should_Reload_Custom_Agents()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            CustomAgents =
            [
                new CustomAgentConfig
                {
                    Name = "reload-test-agent",
                    DisplayName = "Reload Test Agent",
                    Description = "Used by the agent reload RPC test.",
                    Prompt = "You are a reload test agent.",
                },
            ],
        });

        var result = await session.Rpc.Agent.ReloadAsync();

        Assert.NotNull(result.Agents);
    }
}
