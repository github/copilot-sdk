/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.json.CustomAgentConfig;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.SessionConfig;

/**
 * Tests for MCP Servers and Custom Agents functionality.
 *
 * <p>
 * These tests use the shared CapiProxy infrastructure for deterministic API
 * response replay. Snapshots are stored in test/snapshots/mcp_and_agents/.
 * </p>
 */
public class McpAndAgentsTest {

    private static E2ETestContext ctx;

    @BeforeAll
    static void setup() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    // Helper method to create an MCP local server configuration
    private Map<String, Object> createLocalMcpServer(String command, List<String> args) {
        var server = new HashMap<String, Object>();
        server.put("type", "local");
        server.put("command", command);
        server.put("args", args);
        server.put("tools", List.of("*"));
        return server;
    }

    // ============ MCP Server Tests ============

    /**
     * Verifies that MCP server configuration is accepted on session create.
     *
     * @see Snapshot:
     *      mcp_and_agents/should_accept_mcp_server_configuration_on_session_create
     */
    @Test
    void testShouldAcceptMcpServerConfigurationOnSessionCreate() throws Exception {
        ctx.configureForTest("mcp_and_agents", "should_accept_mcp_server_configuration_on_session_create");

        var mcpServers = new HashMap<String, Object>();
        mcpServers.put("test-server", createLocalMcpServer("echo", List.of("hello")));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setMcpServers(mcpServers)).get();

            assertNotNull(session.getSessionId());

            // Simple interaction to verify session works
            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 2+2?")).get(60,
                    TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("4"),
                    "Response should contain 4: " + response.getData().content());

            session.close();
        }
    }

    /**
     * Verifies that MCP server configuration is accepted on session resume.
     *
     * @see Snapshot:
     *      mcp_and_agents/should_accept_mcp_server_configuration_on_session_resume
     */
    @Test
    void testShouldAcceptMcpServerConfigurationOnSessionResume() throws Exception {
        ctx.configureForTest("mcp_and_agents", "should_accept_mcp_server_configuration_on_session_resume");

        try (CopilotClient client = ctx.createClient()) {
            // Create a session first
            CopilotSession session1 = client.createSession().get();
            String sessionId = session1.getSessionId();
            session1.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60, TimeUnit.SECONDS);

            // Resume with MCP servers
            var mcpServers = new HashMap<String, Object>();
            mcpServers.put("test-server", createLocalMcpServer("echo", List.of("hello")));

            CopilotSession session2 = client
                    .resumeSession(sessionId, new ResumeSessionConfig().setMcpServers(mcpServers)).get();

            assertEquals(sessionId, session2.getSessionId());

            AssistantMessageEvent response = session2.sendAndWait(new MessageOptions().setPrompt("What is 3+3?"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("6"),
                    "Response should contain 6: " + response.getData().content());

            session2.close();
        }
    }

    /**
     * Verifies that multiple MCP servers can be configured.
     *
     * @see Snapshot:
     *      mcp_and_agents/should_accept_mcp_server_configuration_on_session_create
     */
    @Test
    void testShouldHandleMultipleMcpServers() throws Exception {
        // Use same snapshot as single MCP server test since it doesn't depend on server
        // count
        ctx.configureForTest("mcp_and_agents", "should_accept_mcp_server_configuration_on_session_create");

        var mcpServers = new HashMap<String, Object>();
        mcpServers.put("server1", createLocalMcpServer("echo", List.of("server1")));
        mcpServers.put("server2", createLocalMcpServer("echo", List.of("server2")));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setMcpServers(mcpServers)).get();

            assertNotNull(session.getSessionId());
            session.close();
        }
    }

    // ============ Custom Agent Tests ============

    /**
     * Verifies that custom agent configuration is accepted on session create.
     *
     * @see Snapshot:
     *      mcp_and_agents/should_accept_custom_agent_configuration_on_session_create
     */
    @Test
    void testShouldAcceptCustomAgentConfigurationOnSessionCreate() throws Exception {
        ctx.configureForTest("mcp_and_agents", "should_accept_custom_agent_configuration_on_session_create");

        List<CustomAgentConfig> customAgents = List.of(new CustomAgentConfig().setName("test-agent")
                .setDisplayName("Test Agent").setDescription("A test agent for SDK testing")
                .setPrompt("You are a helpful test agent.").setInfer(true));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setCustomAgents(customAgents)).get();

            assertNotNull(session.getSessionId());

            // Simple interaction to verify session works
            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 5+5?")).get(60,
                    TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("10"),
                    "Response should contain 10: " + response.getData().content());

            session.close();
        }
    }

    /**
     * Verifies that custom agent configuration is accepted on session resume.
     *
     * @see Snapshot:
     *      mcp_and_agents/should_accept_custom_agent_configuration_on_session_resume
     */
    @Test
    void testShouldAcceptCustomAgentConfigurationOnSessionResume() throws Exception {
        ctx.configureForTest("mcp_and_agents", "should_accept_custom_agent_configuration_on_session_resume");

        try (CopilotClient client = ctx.createClient()) {
            // Create a session first
            CopilotSession session1 = client.createSession().get();
            String sessionId = session1.getSessionId();
            session1.sendAndWait(new MessageOptions().setPrompt("What is 1+1?")).get(60, TimeUnit.SECONDS);

            // Resume with custom agents
            List<CustomAgentConfig> customAgents = List
                    .of(new CustomAgentConfig().setName("resume-agent").setDisplayName("Resume Agent")
                            .setDescription("An agent added on resume").setPrompt("You are a resume test agent."));

            CopilotSession session2 = client
                    .resumeSession(sessionId, new ResumeSessionConfig().setCustomAgents(customAgents)).get();

            assertEquals(sessionId, session2.getSessionId());

            AssistantMessageEvent response = session2.sendAndWait(new MessageOptions().setPrompt("What is 6+6?"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("12"),
                    "Response should contain 12: " + response.getData().content());

            session2.close();
        }
    }

    /**
     * Verifies that custom agents can be configured with tools.
     *
     * @see Snapshot:
     *      mcp_and_agents/should_accept_custom_agent_configuration_on_session_create
     */
    @Test
    void testShouldAcceptCustomAgentWithToolsConfiguration() throws Exception {
        // Use same snapshot as create test since this just verifies configuration
        // acceptance
        ctx.configureForTest("mcp_and_agents", "should_accept_custom_agent_configuration_on_session_create");

        List<CustomAgentConfig> customAgents = List.of(new CustomAgentConfig().setName("tool-agent")
                .setDisplayName("Tool Agent").setDescription("An agent with specific tools")
                .setPrompt("You are an agent with specific tools.").setTools(List.of("bash", "edit")).setInfer(true));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setCustomAgents(customAgents)).get();

            assertNotNull(session.getSessionId());
            session.close();
        }
    }

    /**
     * Verifies that custom agents can be configured with MCP servers.
     *
     * @see Snapshot:
     *      mcp_and_agents/should_accept_both_mcp_servers_and_custom_agents
     */
    @Test
    void testShouldAcceptCustomAgentWithMcpServers() throws Exception {
        // Use combined snapshot since this uses both MCP servers and custom agents
        ctx.configureForTest("mcp_and_agents", "should_accept_both_mcp_servers_and_custom_agents");

        var agentMcpServers = new HashMap<String, Object>();
        agentMcpServers.put("agent-server", createLocalMcpServer("echo", List.of("agent-mcp")));

        List<CustomAgentConfig> customAgents = List.of(new CustomAgentConfig().setName("mcp-agent")
                .setDisplayName("MCP Agent").setDescription("An agent with its own MCP servers")
                .setPrompt("You are an agent with MCP servers.").setMcpServers(agentMcpServers));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setCustomAgents(customAgents)).get();

            assertNotNull(session.getSessionId());
            session.close();
        }
    }

    /**
     * Verifies that multiple custom agents can be configured.
     *
     * @see Snapshot:
     *      mcp_and_agents/should_accept_custom_agent_configuration_on_session_create
     */
    @Test
    void testShouldAcceptMultipleCustomAgents() throws Exception {
        // Use same snapshot as create test
        ctx.configureForTest("mcp_and_agents", "should_accept_custom_agent_configuration_on_session_create");

        List<CustomAgentConfig> customAgents = List.of(
                new CustomAgentConfig().setName("agent1").setDisplayName("Agent One").setDescription("First agent")
                        .setPrompt("You are agent one."),
                new CustomAgentConfig().setName("agent2").setDisplayName("Agent Two").setDescription("Second agent")
                        .setPrompt("You are agent two.").setInfer(false));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client.createSession(new SessionConfig().setCustomAgents(customAgents)).get();

            assertNotNull(session.getSessionId());
            session.close();
        }
    }

    // ============ Combined Configuration Tests ============

    /**
     * Verifies that both MCP servers and custom agents can be configured.
     *
     * @see Snapshot:
     *      mcp_and_agents/should_accept_both_mcp_servers_and_custom_agents
     */
    @Test
    void testShouldAcceptBothMcpServersAndCustomAgents() throws Exception {
        ctx.configureForTest("mcp_and_agents", "should_accept_both_mcp_servers_and_custom_agents");

        var mcpServers = new HashMap<String, Object>();
        mcpServers.put("shared-server", createLocalMcpServer("echo", List.of("shared")));

        List<CustomAgentConfig> customAgents = List.of(new CustomAgentConfig().setName("combined-agent")
                .setDisplayName("Combined Agent").setDescription("An agent using shared MCP servers")
                .setPrompt("You are a combined test agent."));

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setMcpServers(mcpServers).setCustomAgents(customAgents)).get();

            assertNotNull(session.getSessionId());

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 7+7?")).get(60,
                    TimeUnit.SECONDS);

            assertNotNull(response);
            assertTrue(response.getData().content().contains("14"),
                    "Response should contain 14: " + response.getData().content());

            session.close();
        }
    }
}
