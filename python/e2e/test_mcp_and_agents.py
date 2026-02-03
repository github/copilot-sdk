"""
Tests for MCP servers and custom agents functionality
"""

import pytest

from copilot import CustomAgentConfig, MCPServerConfig

from .testharness import E2ETestContext, get_final_assistant_message

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestMCPServers:
    async def test_should_accept_mcp_server_configuration_on_session_create(
        self, ctx: E2ETestContext
    ):
        """Test that MCP server configuration is accepted on session create"""
        mcp_servers: dict[str, MCPServerConfig] = {
            "test-server": {
                "type": "local",
                "command": "echo",
                "args": ["hello"],
                "tools": ["*"],
            }
        }

        session = await ctx.client.create_session({"mcp_servers": mcp_servers})

        assert session.session_id is not None

        # Simple interaction to verify session works
        message = await session.send_and_wait({"prompt": "What is 2+2?"})
        assert message is not None
        assert "4" in message.data.content

        await session.destroy()

    async def test_should_accept_mcp_server_configuration_on_session_resume(
        self, ctx: E2ETestContext
    ):
        """Test that MCP server configuration is accepted on session resume"""
        # Create a session first
        session1 = await ctx.client.create_session()
        session_id = session1.session_id
        await session1.send_and_wait({"prompt": "What is 1+1?"})

        # Resume with MCP servers
        mcp_servers: dict[str, MCPServerConfig] = {
            "test-server": {
                "type": "local",
                "command": "echo",
                "args": ["hello"],
                "tools": ["*"],
            }
        }

        session2 = await ctx.client.resume_session(session_id, {"mcp_servers": mcp_servers})

        assert session2.session_id == session_id

        message = await session2.send_and_wait({"prompt": "What is 3+3?"})
        assert message is not None
        assert "6" in message.data.content

        await session2.destroy()


class TestCustomAgents:
    async def test_should_accept_custom_agent_configuration_on_session_create(
        self, ctx: E2ETestContext
    ):
        """Test that custom agent configuration is accepted on session create"""
        custom_agents: list[CustomAgentConfig] = [
            {
                "name": "test-agent",
                "display_name": "Test Agent",
                "description": "A test agent for SDK testing",
                "prompt": "You are a helpful test agent.",
                "infer": True,
            }
        ]

        session = await ctx.client.create_session({"custom_agents": custom_agents})

        assert session.session_id is not None

        # Simple interaction to verify session works
        message = await session.send_and_wait({"prompt": "What is 5+5?"})
        assert message is not None
        assert "10" in message.data.content

        await session.destroy()

    async def test_should_accept_custom_agent_configuration_on_session_resume(
        self, ctx: E2ETestContext
    ):
        """Test that custom agent configuration is accepted on session resume"""
        # Create a session first
        session1 = await ctx.client.create_session()
        session_id = session1.session_id
        await session1.send_and_wait({"prompt": "What is 1+1?"})

        # Resume with custom agents
        custom_agents: list[CustomAgentConfig] = [
            {
                "name": "resume-agent",
                "display_name": "Resume Agent",
                "description": "An agent added on resume",
                "prompt": "You are a resume test agent.",
            }
        ]

        session2 = await ctx.client.resume_session(session_id, {"custom_agents": custom_agents})

        assert session2.session_id == session_id

        message = await session2.send_and_wait({"prompt": "What is 6+6?"})
        assert message is not None
        assert "12" in message.data.content

        await session2.destroy()


class TestCombinedConfiguration:
    async def test_should_accept_both_mcp_servers_and_custom_agents(self, ctx: E2ETestContext):
        """Test that both MCP servers and custom agents can be configured together"""
        mcp_servers: dict[str, MCPServerConfig] = {
            "shared-server": {
                "type": "local",
                "command": "echo",
                "args": ["shared"],
                "tools": ["*"],
            }
        }

        custom_agents: list[CustomAgentConfig] = [
            {
                "name": "combined-agent",
                "display_name": "Combined Agent",
                "description": "An agent using shared MCP servers",
                "prompt": "You are a combined test agent.",
            }
        ]

        session = await ctx.client.create_session(
            {"mcp_servers": mcp_servers, "custom_agents": custom_agents}
        )

        assert session.session_id is not None

        await session.send({"prompt": "What is 7+7?"})
        message = await get_final_assistant_message(session)
        assert "14" in message.data.content

        await session.destroy()
