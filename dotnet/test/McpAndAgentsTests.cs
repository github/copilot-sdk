/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.SDK.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.SDK.Test;

public class McpAndAgentsTests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "mcp-and-agents", output)
{
    [Fact]
    public async Task Should_Accept_MCP_Server_Configuration_On_Session_Create()
    {
        var mcpServers = new Dictionary<string, object>
        {
            ["test-server"] = new McpLocalServerConfig
            {
                Type = "local",
                Command = "echo",
                Args = ["hello"],
                Tools = ["*"]
            }
        };

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            McpServers = mcpServers
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        // Simple interaction to verify session works
        await session.SendAsync(new MessageOptions { Prompt = "What is 2+2?" });

        var message = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(message);
        Assert.Contains("4", message!.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Accept_MCP_Server_Configuration_On_Session_Resume()
    {
        // Create a session first
        var session1 = await Client.CreateSessionAsync();
        var sessionId = session1.SessionId;
        await session1.SendAndWaitAsync(new MessageOptions { Prompt = "What is 1+1?" });

        // Resume with MCP servers
        var mcpServers = new Dictionary<string, object>
        {
            ["test-server"] = new McpLocalServerConfig
            {
                Type = "local",
                Command = "echo",
                Args = ["hello"],
                Tools = ["*"]
            }
        };

        var session2 = await Client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            McpServers = mcpServers
        });

        Assert.Equal(sessionId, session2.SessionId);

        var message = await session2.SendAndWaitAsync(new MessageOptions { Prompt = "What is 3+3?" });
        Assert.NotNull(message);
        Assert.Contains("6", message!.Data.Content);

        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Should_Handle_Multiple_MCP_Servers()
    {
        var mcpServers = new Dictionary<string, object>
        {
            ["server1"] = new McpLocalServerConfig
            {
                Type = "local",
                Command = "echo",
                Args = ["server1"],
                Tools = ["*"]
            },
            ["server2"] = new McpLocalServerConfig
            {
                Type = "local",
                Command = "echo",
                Args = ["server2"],
                Tools = ["*"]
            }
        };

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            McpServers = mcpServers
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);
        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Accept_Custom_Agent_Configuration_On_Session_Create()
    {
        var customAgents = new List<CustomAgentConfig>
        {
            new CustomAgentConfig
            {
                Name = "test-agent",
                DisplayName = "Test Agent",
                Description = "A test agent for SDK testing",
                Prompt = "You are a helpful test agent.",
                Infer = true
            }
        };

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            CustomAgents = customAgents
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        // Simple interaction to verify session works
        await session.SendAsync(new MessageOptions { Prompt = "What is 5+5?" });

        var message = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(message);
        Assert.Contains("10", message!.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Accept_Custom_Agent_Configuration_On_Session_Resume()
    {
        // Create a session first
        var session1 = await Client.CreateSessionAsync();
        var sessionId = session1.SessionId;
        await session1.SendAndWaitAsync(new MessageOptions { Prompt = "What is 1+1?" });

        // Resume with custom agents
        var customAgents = new List<CustomAgentConfig>
        {
            new CustomAgentConfig
            {
                Name = "resume-agent",
                DisplayName = "Resume Agent",
                Description = "An agent added on resume",
                Prompt = "You are a resume test agent."
            }
        };

        var session2 = await Client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            CustomAgents = customAgents
        });

        Assert.Equal(sessionId, session2.SessionId);

        var message = await session2.SendAndWaitAsync(new MessageOptions { Prompt = "What is 6+6?" });
        Assert.NotNull(message);
        Assert.Contains("12", message!.Data.Content);

        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Should_Handle_Custom_Agent_With_Tools_Configuration()
    {
        var customAgents = new List<CustomAgentConfig>
        {
            new CustomAgentConfig
            {
                Name = "tool-agent",
                DisplayName = "Tool Agent",
                Description = "An agent with specific tools",
                Prompt = "You are an agent with specific tools.",
                Tools = ["bash", "edit"],
                Infer = true
            }
        };

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            CustomAgents = customAgents
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);
        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Handle_Custom_Agent_With_MCP_Servers()
    {
        var customAgents = new List<CustomAgentConfig>
        {
            new CustomAgentConfig
            {
                Name = "mcp-agent",
                DisplayName = "MCP Agent",
                Description = "An agent with its own MCP servers",
                Prompt = "You are an agent with MCP servers.",
                McpServers = new Dictionary<string, object>
                {
                    ["agent-server"] = new McpLocalServerConfig
                    {
                        Type = "local",
                        Command = "echo",
                        Args = ["agent-mcp"],
                        Tools = ["*"]
                    }
                }
            }
        };

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            CustomAgents = customAgents
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);
        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Handle_Multiple_Custom_Agents()
    {
        var customAgents = new List<CustomAgentConfig>
        {
            new CustomAgentConfig
            {
                Name = "agent1",
                DisplayName = "Agent One",
                Description = "First agent",
                Prompt = "You are agent one."
            },
            new CustomAgentConfig
            {
                Name = "agent2",
                DisplayName = "Agent Two",
                Description = "Second agent",
                Prompt = "You are agent two.",
                Infer = false
            }
        };

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            CustomAgents = customAgents
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);
        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Accept_Both_MCP_Servers_And_Custom_Agents()
    {
        var mcpServers = new Dictionary<string, object>
        {
            ["shared-server"] = new McpLocalServerConfig
            {
                Type = "local",
                Command = "echo",
                Args = ["shared"],
                Tools = ["*"]
            }
        };

        var customAgents = new List<CustomAgentConfig>
        {
            new CustomAgentConfig
            {
                Name = "combined-agent",
                DisplayName = "Combined Agent",
                Description = "An agent using shared MCP servers",
                Prompt = "You are a combined test agent."
            }
        };

        var session = await Client.CreateSessionAsync(new SessionConfig
        {
            McpServers = mcpServers,
            CustomAgents = customAgents
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        await session.SendAsync(new MessageOptions { Prompt = "What is 7+7?" });

        var message = await TestHelper.GetFinalAssistantMessageAsync(session);
        Assert.NotNull(message);
        Assert.Contains("14", message!.Data.Content);

        await session.DisposeAsync();
    }
}
