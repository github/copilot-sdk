/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
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
    public async Task Should_Emit_Subagent_Selected_And_Deselected_Events()
    {
        var session = await CreateSessionAsync(new SessionConfig { CustomAgents = [CreateCustomAgents()[0]] });

        var selectedEventTask = TestHelper.GetNextEventOfTypeAsync<SubagentSelectedEvent>(
            session,
            static _ => true,
            timeout: TimeSpan.FromSeconds(30),
            timeoutDescription: "subagent.selected event");
        var selectResult = await session.Rpc.Agent.SelectAsync("test-agent");
        var selectedEvent = await selectedEventTask;

        Assert.NotNull(selectResult.Agent);
        Assert.Equal("test-agent", selectedEvent.Data.AgentName);
        Assert.Equal("Test Agent", selectedEvent.Data.AgentDisplayName);

        var deselectedEventTask = TestHelper.GetNextEventOfTypeAsync<SubagentDeselectedEvent>(
            session,
            static _ => true,
            timeout: TimeSpan.FromSeconds(30),
            timeoutDescription: "subagent.deselected event");
        await session.Rpc.Agent.DeselectAsync();
        await deselectedEventTask;

        var currentResult = await session.Rpc.Agent.GetCurrentAsync();
        Assert.Null(currentResult.Agent);
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
        var reloadAgent = CreateReloadAgent($"reload-test-agent-{Guid.NewGuid():N}");
        var session = await CreateSessionAsync(new SessionConfig { CustomAgents = [reloadAgent] });

        var before = await session.Rpc.Agent.ListAsync();
        AssertReloadAgent(before.Agents, reloadAgent);

        var result = await session.Rpc.Agent.ReloadAsync();
        var current = await session.Rpc.Agent.ListAsync();
        Assert.NotNull(result.Agents);
        Assert.Equal(
            result.Agents.Select(agent => agent.Name).OrderBy(name => name, StringComparer.Ordinal),
            current.Agents.Select(agent => agent.Name).OrderBy(name => name, StringComparer.Ordinal));
        Assert.Equal(
            result.Agents.Select(agent => agent.DisplayName).OrderBy(name => name, StringComparer.Ordinal),
            current.Agents.Select(agent => agent.DisplayName).OrderBy(name => name, StringComparer.Ordinal));
    }

    private static void AssertReloadAgent(IEnumerable<AgentInfo> agents, CustomAgentConfig expected)
    {
        var agent = Assert.Single(agents, agent => string.Equals(agent.Name, expected.Name, StringComparison.Ordinal));
        Assert.Equal(expected.DisplayName, agent.DisplayName);
        Assert.Equal(expected.Description, agent.Description);
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

    private static CustomAgentConfig CreateReloadAgent(string name) =>
        new()
        {
            Name = name,
            DisplayName = "Reload Test Agent",
            Description = "Used by the agent reload RPC test.",
            Prompt = "You are a reload test agent.",
        };
}
