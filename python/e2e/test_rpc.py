"""E2E RPC Tests"""

import pytest

from copilot import CopilotClient
from copilot.generated.rpc import PingParams

from .testharness import CLI_PATH, E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestRpc:
    @pytest.mark.asyncio
    async def test_should_call_rpc_ping_with_typed_params(self):
        """Test calling rpc.ping with typed params and result"""
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()

            result = await client.rpc.ping(PingParams(message="typed rpc test"))
            assert result.message == "pong: typed rpc test"
            assert isinstance(result.timestamp, (int, float))

            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_should_call_rpc_models_list(self):
        """Test calling rpc.models.list with typed result"""
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()

            auth_status = await client.get_auth_status()
            if not auth_status.isAuthenticated:
                await client.stop()
                return

            result = await client.rpc.models.list()
            assert result.models is not None
            assert isinstance(result.models, list)

            await client.stop()
        finally:
            await client.force_stop()

    # account.getQuota is defined in schema but not yet implemented in CLI
    @pytest.mark.skip(reason="account.getQuota not yet implemented in CLI")
    @pytest.mark.asyncio
    async def test_should_call_rpc_account_get_quota(self):
        """Test calling rpc.account.getQuota when authenticated"""
        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()

            auth_status = await client.get_auth_status()
            if not auth_status.isAuthenticated:
                await client.stop()
                return

            result = await client.rpc.account.get_quota()
            assert result.quota_snapshots is not None
            assert isinstance(result.quota_snapshots, dict)

            await client.stop()
        finally:
            await client.force_stop()


class TestSessionRpc:
    # session.model.getCurrent is defined in schema but not yet implemented in CLI
    @pytest.mark.skip(reason="session.model.getCurrent not yet implemented in CLI")
    async def test_should_call_session_rpc_model_get_current(self, ctx: E2ETestContext):
        """Test calling session.rpc.model.getCurrent"""
        session = await ctx.client.create_session({"model": "claude-sonnet-4.5"})

        result = await session.rpc.model.get_current()
        assert result.model_id is not None
        assert isinstance(result.model_id, str)

    # session.model.switchTo is defined in schema but not yet implemented in CLI
    @pytest.mark.skip(reason="session.model.switchTo not yet implemented in CLI")
    async def test_should_call_session_rpc_model_switch_to(self, ctx: E2ETestContext):
        """Test calling session.rpc.model.switchTo"""
        from copilot.generated.rpc import SessionModelSwitchToParams

        session = await ctx.client.create_session({"model": "claude-sonnet-4.5"})

        # Get initial model
        before = await session.rpc.model.get_current()
        assert before.model_id is not None

        # Switch to a different model
        result = await session.rpc.model.switch_to(SessionModelSwitchToParams(model_id="gpt-4.1"))
        assert result.model_id == "gpt-4.1"

        # Verify the switch persisted
        after = await session.rpc.model.get_current()
        assert after.model_id == "gpt-4.1"
