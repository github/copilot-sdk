"""E2E RPC Tests"""

import pytest

from copilot import CopilotClient, PermissionHandler
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
        session = await ctx.client.create_session(
            {"model": "claude-sonnet-4.5", "on_permission_request": PermissionHandler.approve_all}
        )

        result = await session.rpc.model.get_current()
        assert result.model_id is not None
        assert isinstance(result.model_id, str)

    # session.model.switchTo is defined in schema but not yet implemented in CLI
    @pytest.mark.skip(reason="session.model.switchTo not yet implemented in CLI")
    async def test_should_call_session_rpc_model_switch_to(self, ctx: E2ETestContext):
        """Test calling session.rpc.model.switchTo"""
        from copilot.generated.rpc import SessionModelSwitchToParams

        session = await ctx.client.create_session(
            {"model": "claude-sonnet-4.5", "on_permission_request": PermissionHandler.approve_all}
        )

        # Get initial model
        before = await session.rpc.model.get_current()
        assert before.model_id is not None

        # Switch to a different model
        result = await session.rpc.model.switch_to(SessionModelSwitchToParams(model_id="gpt-4.1"))
        assert result.model_id == "gpt-4.1"

        # Verify the switch persisted
        after = await session.rpc.model.get_current()
        assert after.model_id == "gpt-4.1"

    @pytest.mark.asyncio
    async def test_get_and_set_session_mode(self):
        """Test getting and setting session mode"""
        from copilot.generated.rpc import Mode, SessionModeSetParams

        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()
            session = await client.create_session(
                {"on_permission_request": PermissionHandler.approve_all}
            )

            # Get initial mode (default should be interactive)
            initial = await session.rpc.mode.get()
            assert initial.mode == Mode.INTERACTIVE

            # Switch to plan mode
            plan_result = await session.rpc.mode.set(SessionModeSetParams(mode=Mode.PLAN))
            assert plan_result.mode == Mode.PLAN

            # Verify mode persisted
            after_plan = await session.rpc.mode.get()
            assert after_plan.mode == Mode.PLAN

            # Switch back to interactive
            interactive_result = await session.rpc.mode.set(
                SessionModeSetParams(mode=Mode.INTERACTIVE)
            )
            assert interactive_result.mode == Mode.INTERACTIVE

            await session.destroy()
            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_read_update_and_delete_plan(self):
        """Test reading, updating, and deleting plan"""
        from copilot.generated.rpc import SessionPlanUpdateParams

        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()
            session = await client.create_session(
                {"on_permission_request": PermissionHandler.approve_all}
            )

            # Initially plan should not exist
            initial = await session.rpc.plan.read()
            assert initial.exists is False
            assert initial.content is None

            # Create/update plan
            plan_content = "# Test Plan\n\n- Step 1\n- Step 2"
            await session.rpc.plan.update(SessionPlanUpdateParams(content=plan_content))

            # Verify plan exists and has correct content
            after_update = await session.rpc.plan.read()
            assert after_update.exists is True
            assert after_update.content == plan_content

            # Delete plan
            await session.rpc.plan.delete()

            # Verify plan is deleted
            after_delete = await session.rpc.plan.read()
            assert after_delete.exists is False
            assert after_delete.content is None

            await session.destroy()
            await client.stop()
        finally:
            await client.force_stop()

    @pytest.mark.asyncio
    async def test_create_list_and_read_workspace_files(self):
        """Test creating, listing, and reading workspace files"""
        from copilot.generated.rpc import (
            SessionWorkspaceCreateFileParams,
            SessionWorkspaceReadFileParams,
        )

        client = CopilotClient({"cli_path": CLI_PATH, "use_stdio": True})

        try:
            await client.start()
            session = await client.create_session(
                {"on_permission_request": PermissionHandler.approve_all}
            )

            # Initially no files
            initial_files = await session.rpc.workspace.list_files()
            assert initial_files.files == []

            # Create a file
            file_content = "Hello, workspace!"
            await session.rpc.workspace.create_file(
                SessionWorkspaceCreateFileParams(content=file_content, path="test.txt")
            )

            # List files
            after_create = await session.rpc.workspace.list_files()
            assert "test.txt" in after_create.files

            # Read file
            read_result = await session.rpc.workspace.read_file(
                SessionWorkspaceReadFileParams(path="test.txt")
            )
            assert read_result.content == file_content

            # Create nested file
            await session.rpc.workspace.create_file(
                SessionWorkspaceCreateFileParams(content="Nested content", path="subdir/nested.txt")
            )

            after_nested = await session.rpc.workspace.list_files()
            assert "test.txt" in after_nested.files
            assert any("nested.txt" in f for f in after_nested.files)

            await session.destroy()
            await client.stop()
        finally:
            await client.force_stop()
