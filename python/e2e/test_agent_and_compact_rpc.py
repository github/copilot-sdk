"""E2E tests for Agent Selection and Session Compaction RPC APIs."""

import pytest

from copilot import CopilotClient, PermissionHandler
from copilot.generated.rpc import SessionAgentSelectParams

from .testharness import CLI_PATH, E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestAgentSelectionRpc:
    @pytest.mark.asyncio
    async def test_should_list_available_custom_agents(self):
        """Test listing available custom agents via RPC."""
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()
            session = await client.create_session(
                {
                    "on_permission_request": PermissionHandler.approve_all,
                    "custom_agents": [
                        {
                            "name": "test-agent",
                            "display_name": "Test Agent",
                            "description": "A test agent",
                            "prompt": "You are a test agent.",
                        },
                        {
                            "name": "another-agent",
                            "display_name": "Another Agent",
                            "description": "Another test agent",
                            "prompt": "You are another agent.",
                        },
                    ],
                }
            )

            result = await session.rpc.agent.list()
            assert result.agents is not None
            assert len(result.agents) == 2
            assert result.agents[0].name == "test-agent"
            assert result.agents[0].display_name == "Test Agent"
            assert result.agents[0].description == "A test agent"
            assert result.agents[1].name == "another-agent"

            await session.destroy()
            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_return_null_when_no_agent_is_selected(self):
        """Test getCurrent returns null when no agent is selected."""
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()
            session = await client.create_session(
                {
                    "on_permission_request": PermissionHandler.approve_all,
                    "custom_agents": [
                        {
                            "name": "test-agent",
                            "display_name": "Test Agent",
                            "description": "A test agent",
                            "prompt": "You are a test agent.",
                        }
                    ],
                }
            )

            result = await session.rpc.agent.get_current()
            assert result.agent is None

            await session.destroy()
            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_select_and_get_current_agent(self):
        """Test selecting an agent and verifying getCurrent returns it."""
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()
            session = await client.create_session(
                {
                    "on_permission_request": PermissionHandler.approve_all,
                    "custom_agents": [
                        {
                            "name": "test-agent",
                            "display_name": "Test Agent",
                            "description": "A test agent",
                            "prompt": "You are a test agent.",
                        }
                    ],
                }
            )

            # Select the agent
            select_result = await session.rpc.agent.select(
                SessionAgentSelectParams(name="test-agent")
            )
            assert select_result.agent is not None
            assert select_result.agent.name == "test-agent"
            assert select_result.agent.display_name == "Test Agent"

            # Verify getCurrent returns the selected agent
            current_result = await session.rpc.agent.get_current()
            assert current_result.agent is not None
            assert current_result.agent.name == "test-agent"

            await session.destroy()
            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_deselect_current_agent(self):
        """Test deselecting the current agent."""
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()
            session = await client.create_session(
                {
                    "on_permission_request": PermissionHandler.approve_all,
                    "custom_agents": [
                        {
                            "name": "test-agent",
                            "display_name": "Test Agent",
                            "description": "A test agent",
                            "prompt": "You are a test agent.",
                        }
                    ],
                }
            )

            # Select then deselect
            await session.rpc.agent.select(SessionAgentSelectParams(name="test-agent"))
            await session.rpc.agent.deselect()

            # Verify no agent is selected
            current_result = await session.rpc.agent.get_current()
            assert current_result.agent is None

            await session.destroy()
            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_return_empty_list_when_no_custom_agents_configured(self):
        """Test listing agents returns empty when none configured."""
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()
            session = await client.create_session(
                {"on_permission_request": PermissionHandler.approve_all}
            )

            result = await session.rpc.agent.list()
            assert result.agents == []

            await session.destroy()
            await client.stop()
        finally:
            await client.force_stop()


class TestSessionCompactionRpc:
    @pytest.mark.asyncio
    async def test_should_compact_session_history_after_messages(self, ctx: E2ETestContext):
        """Test compacting session history via RPC."""
        session = await ctx.client.create_session(
            {"on_permission_request": PermissionHandler.approve_all}
        )

        # Send a message to create some history
        await session.send_and_wait({"prompt": "What is 2+2?"})

        # Compact the session
        result = await session.rpc.compaction.compact()
        assert isinstance(result.success, bool)
        assert isinstance(result.tokens_removed, (int, float))
        assert isinstance(result.messages_removed, (int, float))

        await session.destroy()
