/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Rpc;
using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class AgentAndCompactRpcTests(E2ETestFixture fixture, ITestOutputHelper output)
    : E2ETestBase(fixture, "agent_and_compact_rpc", output)
{
    [Fact]
    public async Task Should_List_Available_Custom_Agents()
    {
        var customAgents = new List<CustomAgentConfig>
        {
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
        };

        var session = await CreateSessionAsync(new SessionConfig { CustomAgents = customAgents });

        var result = await session.Rpc.Agent.ListAsync();
        Assert.NotNull(result.Agents);
        Assert.Equal(2, result.Agents.Count);
        Assert.Equal("test-agent", result.Agents[0].Name);
        Assert.Equal("Test Agent", result.Agents[0].DisplayName);
        Assert.Equal("A test agent", result.Agents[0].Description);
        Assert.Equal("another-agent", result.Agents[1].Name);
    }

    [Fact]
    public async Task Should_Return_Null_When_No_Agent_Is_Selected()
    {
        var customAgents = new List<CustomAgentConfig>
        {
            new()
            {
                Name = "test-agent",
                DisplayName = "Test Agent",
                Description = "A test agent",
                Prompt = "You are a test agent."
            }
        };

        var session = await CreateSessionAsync(new SessionConfig { CustomAgents = customAgents });

        var result = await session.Rpc.Agent.GetCurrentAsync();
        Assert.Null(result.Agent);
    }

    [Fact]
    public async Task Should_Select_And_Get_Current_Agent()
    {
        var customAgents = new List<CustomAgentConfig>
        {
            new()
            {
                Name = "test-agent",
                DisplayName = "Test Agent",
                Description = "A test agent",
                Prompt = "You are a test agent."
            }
        };

        var session = await CreateSessionAsync(new SessionConfig { CustomAgents = customAgents });

        // Select the agent
        var selectResult = await session.Rpc.Agent.SelectAsync("test-agent");
        Assert.NotNull(selectResult.Agent);
        Assert.Equal("test-agent", selectResult.Agent.Name);
        Assert.Equal("Test Agent", selectResult.Agent.DisplayName);

        // Verify getCurrent returns the selected agent
        var currentResult = await session.Rpc.Agent.GetCurrentAsync();
        Assert.NotNull(currentResult.Agent);
        Assert.Equal("test-agent", currentResult.Agent.Name);
    }

    [Fact]
    public async Task Should_Deselect_Current_Agent()
    {
        var customAgents = new List<CustomAgentConfig>
        {
            new()
            {
                Name = "test-agent",
                DisplayName = "Test Agent",
                Description = "A test agent",
                Prompt = "You are a test agent."
            }
        };

        var session = await CreateSessionAsync(new SessionConfig { CustomAgents = customAgents });

        // Select then deselect
        await session.Rpc.Agent.SelectAsync("test-agent");
        await session.Rpc.Agent.DeselectAsync();

        // Verify no agent is selected
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
    public async Task Should_Compact_Session_History_After_Messages()
    {
        var session = await CreateSessionAsync();

        // Send a message to create some history
        await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2+2?" });

        // Compact the session
        var result = await session.Rpc.Compaction.CompactAsync();
        Assert.NotNull(result);
    }
}
