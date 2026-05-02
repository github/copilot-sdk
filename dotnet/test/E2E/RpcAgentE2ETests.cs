/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test.E2E;

public class RpcAgentE2ETests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "rpc_agents", output)
{
    [Fact]
    public async Task Should_List_Available_Custom_Agents()
    {
        var session = await CreateSessionAsync(new SessionConfig { CustomAgents = CreateCustomAgents() });

        var result = await session.Rpc.Agent.ListAsync();

        Assert.Equal(2, result.Agents.Count);
        Assert.Equal("test-agent", result.Agents[0].Name);
        Assert.Equal("Test Agent", result.Agents[0].DisplayName);
        Assert.Equal("A test agent", result.Agents[0].Description);
        Assert.Equal("another-agent", result.Agents[1].Name);
    }

    [Fact]
    public async Task Should_Return_Null_When_No_Agent_Is_Selected()
    {
        var session = await CreateSessionAsync(new SessionConfig { CustomAgents = [CreateCustomAgents()[0]] });

        var result = await session.Rpc.Agent.GetCurrentAsync();

        Assert.Null(result.Agent);
    }

    [Fact]
    public async Task Should_Select_And_Get_Current_Agent()
    {
        var session = await CreateSessionAsync(new SessionConfig { CustomAgents = [CreateCustomAgents()[0]] });

        var selectResult = await session.Rpc.Agent.SelectAsync("test-agent");
        Assert.NotNull(selectResult.Agent);
        Assert.Equal("test-agent", selectResult.Agent.Name);
        Assert.Equal("Test Agent", selectResult.Agent.DisplayName);

        var currentResult = await session.Rpc.Agent.GetCurrentAsync();
        Assert.NotNull(currentResult.Agent);
        Assert.Equal("test-agent", currentResult.Agent.Name);
    }

    [Fact]
    public async Task Should_Deselect_Current_Agent()
    {
        var session = await CreateSessionAsync(new SessionConfig { CustomAgents = [CreateCustomAgents()[0]] });

        await session.Rpc.Agent.SelectAsync("test-agent");
        await session.Rpc.Agent.DeselectAsync();

        var currentResult = await session.Rpc.Agent.GetCurrentAsync();
        Assert.Null(currentResult.Agent);
    }

    [Fact]
    public async Task Should_Return_Empty_List_When_No_Custom_Agents_Configured()
    {
        var session = await CreateSessionAsync();

        var result = await session.Rpc.Agent.ListAsync();

        Assert.Empty(result.Agents);
    }

    [Fact]
    public async Task Should_Call_Agent_Reload()
    {
        var session = await CreateSessionAsync(new SessionConfig { CustomAgents = [CreateReloadAgent()] });

        var before = await session.Rpc.Agent.ListAsync();
        Assert.Single(before.Agents, agent => string.Equals(agent.Name, "reload-test-agent", StringComparison.Ordinal));

        var result = await session.Rpc.Agent.ReloadAsync();
        Assert.NotNull(result.Agents);

        // Lock in current runtime behavior so a fix becomes a test failure rather than a
        // silent regression: the runtime currently drops session-configured CustomAgents
        // on reload (it reloads only on-disk agents). Once the runtime preserves session
        // CustomAgents across reload, flip this to `Assert.Single(result.Agents,
        // a => a.Name == "reload-test-agent")` and update the comment.
        Assert.DoesNotContain(result.Agents, a => string.Equals(a.Name, "reload-test-agent", StringComparison.Ordinal));
    }

    private static List<CustomAgentConfig> CreateCustomAgents() =>
    [
        new()
        {
            Name = "test-agent",
            DisplayName = "Test Agent",
            Description = "A test agent",
            Prompt = "You are a test agent."
        },
        new()
        {
            Name = "another-agent",
            DisplayName = "Another Agent",
            Description = "Another test agent",
            Prompt = "You are another agent."
        }
    ];

    private static CustomAgentConfig CreateReloadAgent() =>
        new()
        {
            Name = "reload-test-agent",
            DisplayName = "Reload Test Agent",
            Description = "Used by the agent reload RPC test.",
            Prompt = "You are a reload test agent.",
        };
}
