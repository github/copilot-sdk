/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using GitHub.Copilot.Rpc;
using GitHub.Copilot.Test.Harness;
using Xunit;
using Xunit.Abstractions;

namespace GitHub.Copilot.Test.E2E;

public class SessionMcpAndAgentConfigE2ETests(E2ETestFixture fixture, ITestOutputHelper output) : E2ETestBase(fixture, "mcp_and_agents", output)
{
    [Fact]
    public async Task Should_Accept_MCP_Server_Configuration_On_Session_Create()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers("test-server")
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);
        await WaitForMcpServerStatusAsync(session, "test-server", McpServerStatus.Connected);

        // Simple interaction to verify session works
        var message = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2+2?" });
        Assert.NotNull(message);
        Assert.Contains("4", message!.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Accept_MCP_Server_Configuration_Without_Args()
    {
        var mcpServers = new Dictionary<string, McpServerConfig>
        {
            ["test-server"] = new McpStdioServerConfig
            {
                Command = "dotnet",
                Tools = ["*"]
            }
        };

        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = mcpServers
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Accept_MCP_Server_Configuration_On_Session_Resume()
    {
        // Create a session first
        var session1 = await CreateSessionAsync();
        var sessionId = session1.SessionId;
        await session1.SendAndWaitAsync(new MessageOptions { Prompt = "What is 1+1?" });
        await session1.DisposeAsync();

        // Resume with MCP servers
        var session2 = await ResumeSessionAsync(sessionId, new ResumeSessionConfig
        {
            McpServers = CreateTestMcpServers("test-server")
        });

        Assert.Equal(sessionId, session2.SessionId);
        await WaitForMcpServerStatusAsync(session2, "test-server", McpServerStatus.Connected);

        await session2.DisposeAsync();
    }

    [Fact]
    public async Task Should_Handle_Multiple_MCP_Servers()
    {
        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers("server1", "server2")
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);
        await WaitForMcpServerStatusAsync(session, "server1", McpServerStatus.Connected);
        await WaitForMcpServerStatusAsync(session, "server2", McpServerStatus.Connected);
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

        var session = await CreateSessionAsync(new SessionConfig
        {
            CustomAgents = customAgents
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        // Simple interaction to verify session works
        var message = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 5+5?" });
        Assert.NotNull(message);
        Assert.Contains("10", message!.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Accept_Custom_Agent_Configuration_On_Session_Resume()
    {
        // Create a session first
        var session1 = await CreateSessionAsync();
        var sessionId = session1.SessionId;
        await session1.SendAndWaitAsync(new MessageOptions { Prompt = "What is 1+1?" });
        await session1.DisposeAsync();

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

        var session2 = await ResumeSessionAsync(sessionId, new ResumeSessionConfig
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

        var session = await CreateSessionAsync(new SessionConfig
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
                McpServers = CreateTestMcpServers("agent-server")
            }
        };

        var session = await CreateSessionAsync(new SessionConfig
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

        var session = await CreateSessionAsync(new SessionConfig
        {
            CustomAgents = customAgents
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);
        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Pass_Literal_Env_Values_To_Mcp_Server_Subprocess()
    {
        var testHarnessDir = FindTestHarnessDir();
        var mcpServers = new Dictionary<string, McpServerConfig>
        {
            ["env-echo"] = new McpStdioServerConfig
            {
                Command = "node",
                Args = [Path.Combine(testHarnessDir, "test-mcp-server.mjs")],
                Env = new Dictionary<string, string> { ["TEST_SECRET"] = "hunter2" },
                WorkingDirectory = testHarnessDir,
                Tools = ["*"]
            }
        };

        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = mcpServers,
            OnPermissionRequest = PermissionHandler.ApproveAll,
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);

        var message = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the env-echo/get_env tool to read the TEST_SECRET environment variable. Reply with just the value, nothing else."
        });

        Assert.NotNull(message);
        Assert.Contains("hunter2", message!.Data.Content);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Round_Trip_Mcp_Server_Elicitation_Request()
    {
        var testHarnessDir = FindTestHarnessDir();
        var configPath = Path.Join(Ctx.WorkDir, $"elicitation-config-{Guid.NewGuid():N}.json");
        await File.WriteAllTextAsync(
            configPath,
            """
            [
              {
                "message": "Pick a color.",
                "requestedSchema": {
                  "type": "object",
                  "properties": {
                    "color": {
                      "type": "string",
                      "enum": ["red", "blue"]
                    }
                  },
                  "required": ["color"]
                }
              }
            ]
            """);

        var elicitationContext = new TaskCompletionSource<ElicitationContext>(TaskCreationOptions.RunContinuationsAsynchronously);
        var mcpServers = new Dictionary<string, McpServerConfig>
        {
            ["test-elicitation-server"] = new McpStdioServerConfig
            {
                Command = "node",
                Args =
                [
                    Path.Join(testHarnessDir, "test-mcp-elicitation-server.mjs"),
                    "--config",
                    configPath
                ],
                WorkingDirectory = testHarnessDir,
                Tools = ["*"]
            }
        };

        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = mcpServers,
            OnPermissionRequest = PermissionHandler.ApproveAll,
            OnElicitationRequest = context =>
            {
                elicitationContext.TrySetResult(context);
                return Task.FromResult(new ElicitationResult
                {
                    Action = UIElicitationResponseAction.Accept,
                    Content = new Dictionary<string, object> { ["color"] = "blue" }
                });
            },
        });

        await WaitForMcpServerStatusAsync(session, "test-elicitation-server", McpServerStatus.Connected);

        var message = await session.SendAndWaitAsync(new MessageOptions
        {
            Prompt = "Use the test-elicitation-server-request_user_input tool and tell me the chosen color. Reply with just the color."
        });

        var request = await elicitationContext.Task.WaitAsync(TimeSpan.FromSeconds(60));

        Assert.Equal("Pick a color.", request.Message);
        Assert.Equal(ElicitationRequestedMode.Form, request.Mode);
        Assert.Contains("test-elicitation-server", request.ElicitationSource ?? string.Empty, StringComparison.Ordinal);
        Assert.NotNull(request.RequestedSchema);
        Assert.Equal("object", request.RequestedSchema!.Type);
        Assert.Contains("color", request.RequestedSchema.Properties.Keys);
        Assert.Contains("blue", message?.Data.Content ?? string.Empty);

        await session.DisposeAsync();
    }

    [Fact]
    public async Task Should_Accept_Both_MCP_Servers_And_Custom_Agents()
    {
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

        var session = await CreateSessionAsync(new SessionConfig
        {
            McpServers = CreateTestMcpServers("shared-server"),
            CustomAgents = customAgents
        });

        Assert.Matches(@"^[a-f0-9-]+$", session.SessionId);
        await WaitForMcpServerStatusAsync(session, "shared-server", McpServerStatus.Connected);
        await session.DisposeAsync();
    }

}
