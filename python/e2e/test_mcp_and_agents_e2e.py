"""
Tests for MCP servers and custom agents functionality
"""

import asyncio
import time
from pathlib import Path

import pytest

from copilot.generated.rpc import McpServerStatus
from copilot.session import CustomAgentConfig, MCPServerConfig, PermissionHandler

from .testharness import E2ETestContext

TEST_MCP_SERVER = str(
    (Path(__file__).parents[2] / "test" / "harness" / "test-mcp-server.mjs").resolve()
)
TEST_HARNESS_DIR = str((Path(__file__).parents[2] / "test" / "harness").resolve())

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _test_mcp_servers(*server_names: str) -> dict[str, MCPServerConfig]:
    return {
        server_name: {
            "command": "node",
            "args": [TEST_MCP_SERVER],
            "tools": ["*"],
            "working_directory": TEST_HARNESS_DIR,
        }
        for server_name in server_names
    }


async def _wait_for_mcp_server_status(
    session, server_name: str, expected_status: McpServerStatus = McpServerStatus.CONNECTED
) -> None:
    deadline = time.monotonic() + 60
    last_status = "<not listed>"

    while time.monotonic() < deadline:
        result = await session.rpc.mcp.list()
        server = next((s for s in result.servers if s.name == server_name), None)
        if server is not None and server.status == expected_status:
            return
        last_status = server.status if server is not None else "<not listed>"
        await asyncio.sleep(0.2)

    raise AssertionError(
        f"{server_name} did not reach {expected_status.value}; last status was {last_status}"
    )


class TestMCPServers:
    async def test_should_accept_mcp_server_configuration_on_session_create(
        self, ctx: E2ETestContext
    ):
        """Test that MCP server configuration is accepted on session create"""
        mcp_servers = _test_mcp_servers("test-server")

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, mcp_servers=mcp_servers
        )

        assert session.session_id is not None
        await _wait_for_mcp_server_status(session, "test-server")

        # Simple interaction to verify session works
        message = await session.send_and_wait("What is 2+2?")
        assert message is not None
        assert "4" in message.data.content

        await session.disconnect()

    async def test_should_accept_mcp_server_configuration_without_args(self, ctx: E2ETestContext):
        """Test that MCP server configuration works without args field"""
        mcp_servers: dict[str, MCPServerConfig] = {
            "test-server": {
                "command": "git",
                "tools": ["*"],
            }
        }

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, mcp_servers=mcp_servers
        )

        assert session.session_id is not None

        await session.disconnect()

    async def test_should_accept_mcp_server_configuration_on_session_resume(
        self, ctx: E2ETestContext
    ):
        """Test that MCP server configuration is accepted on session resume"""
        # Create a session first
        session1 = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        session_id = session1.session_id
        await session1.send_and_wait("What is 1+1?")

        # Resume with MCP servers
        mcp_servers = _test_mcp_servers("test-server")

        session2 = await ctx.client.resume_session(
            session_id,
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=mcp_servers,
        )

        assert session2.session_id == session_id
        await _wait_for_mcp_server_status(session2, "test-server")

        await session2.disconnect()

    async def test_should_pass_literal_env_values_to_mcp_server_subprocess(
        self, ctx: E2ETestContext
    ):
        """Test that env values are passed as literals to MCP server subprocess"""
        mcp_servers: dict[str, MCPServerConfig] = {
            "env-echo": {
                "command": "node",
                "args": [TEST_MCP_SERVER],
                "tools": ["*"],
                "env": {"TEST_SECRET": "hunter2"},
                "working_directory": TEST_HARNESS_DIR,
            }
        }

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, mcp_servers=mcp_servers
        )

        assert session.session_id is not None
        await _wait_for_mcp_server_status(session, "env-echo")

        message = await session.send_and_wait(
            "Use the env-echo/get_env tool to read the TEST_SECRET "
            "environment variable. Reply with just the value, nothing else."
        )
        assert message is not None
        assert "hunter2" in message.data.content

        await session.disconnect()


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

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, custom_agents=custom_agents
        )

        assert session.session_id is not None

        # Simple interaction to verify session works
        message = await session.send_and_wait("What is 5+5?")
        assert message is not None
        assert "10" in message.data.content

        await session.disconnect()

    async def test_should_accept_custom_agent_configuration_on_session_resume(
        self, ctx: E2ETestContext
    ):
        """Test that custom agent configuration is accepted on session resume"""
        # Create a session first
        session1 = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        session_id = session1.session_id
        await session1.send_and_wait("What is 1+1?")

        # Resume with custom agents
        custom_agents: list[CustomAgentConfig] = [
            {
                "name": "resume-agent",
                "display_name": "Resume Agent",
                "description": "An agent added on resume",
                "prompt": "You are a resume test agent.",
            }
        ]

        session2 = await ctx.client.resume_session(
            session_id,
            on_permission_request=PermissionHandler.approve_all,
            custom_agents=custom_agents,
        )

        assert session2.session_id == session_id

        message = await session2.send_and_wait("What is 6+6?")
        assert message is not None
        assert "12" in message.data.content

        await session2.disconnect()

    async def test_should_handle_multiple_mcp_servers(self, ctx: E2ETestContext):
        """Multiple MCP servers can be configured at once."""
        mcp_servers = _test_mcp_servers("server1", "server2")

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=mcp_servers,
        )
        try:
            assert session.session_id is not None
            await _wait_for_mcp_server_status(session, "server1")
            await _wait_for_mcp_server_status(session, "server2")
            import re

            assert re.match(r"^[a-f0-9-]+$", session.session_id)
        finally:
            await session.disconnect()


class TestCombinedConfiguration:
    async def test_should_accept_both_mcp_servers_and_custom_agents(self, ctx: E2ETestContext):
        """Test that both MCP servers and custom agents can be configured together"""
        mcp_servers = _test_mcp_servers("shared-server")

        custom_agents: list[CustomAgentConfig] = [
            {
                "name": "combined-agent",
                "display_name": "Combined Agent",
                "description": "An agent using shared MCP servers",
                "prompt": "You are a combined test agent.",
            }
        ]

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=mcp_servers,
            custom_agents=custom_agents,
        )

        assert session.session_id is not None
        await _wait_for_mcp_server_status(session, "shared-server")

        await session.disconnect()

    async def test_should_handle_custom_agent_with_tools_configuration(self, ctx: E2ETestContext):
        """A custom agent can advertise specific tools."""
        custom_agents: list[CustomAgentConfig] = [
            {
                "name": "tool-agent",
                "display_name": "Tool Agent",
                "description": "An agent with specific tools",
                "prompt": "You are an agent with specific tools.",
                "tools": ["bash", "edit"],
                "infer": True,
            }
        ]

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            custom_agents=custom_agents,
        )
        try:
            import re

            assert session.session_id is not None
            assert re.match(r"^[a-f0-9-]+$", session.session_id)
        finally:
            await session.disconnect()

    async def test_should_handle_custom_agent_with_mcp_servers(self, ctx: E2ETestContext):
        """A custom agent can declare its own MCP servers."""
        custom_agents: list[CustomAgentConfig] = [
            {
                "name": "mcp-agent",
                "display_name": "MCP Agent",
                "description": "An agent with its own MCP servers",
                "prompt": "You are an agent with MCP servers.",
                "mcp_servers": _test_mcp_servers("agent-server"),
            }
        ]

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            custom_agents=custom_agents,
        )
        try:
            import re

            assert session.session_id is not None
            assert re.match(r"^[a-f0-9-]+$", session.session_id)
        finally:
            await session.disconnect()

    async def test_should_handle_multiple_custom_agents(self, ctx: E2ETestContext):
        """Multiple custom agents can be configured at once."""
        custom_agents: list[CustomAgentConfig] = [
            {
                "name": "agent1",
                "display_name": "Agent One",
                "description": "First agent",
                "prompt": "You are agent one.",
            },
            {
                "name": "agent2",
                "display_name": "Agent Two",
                "description": "Second agent",
                "prompt": "You are agent two.",
                "infer": False,
            },
        ]

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            custom_agents=custom_agents,
        )
        try:
            import re

            assert session.session_id is not None
            assert re.match(r"^[a-f0-9-]+$", session.session_id)
        finally:
            await session.disconnect()
