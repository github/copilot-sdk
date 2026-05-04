"""
Tests for client-scoped session-management APIs:
``delete_session``, ``get_session_metadata``, ``get_last_session_id``,
``get_foreground_session_id``, and ``set_foreground_session_id``.

The file is named ``test_client_api`` so the conftest snapshot resolver picks
up the ``test/snapshots/client_api`` folder shared with the C# suite
(``ClientSessionManagementTests.cs``).
"""

from __future__ import annotations

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestClientApi:
    async def test_should_delete_session_by_id(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        session_id = session.session_id
        await session.send_and_wait("Say OK.")
        await session.disconnect()
        await ctx.client.delete_session(session_id)

        metadata = await ctx.client.get_session_metadata(session_id)
        assert metadata is None

    async def test_should_report_error_when_deleting_unknown_session_id(self, ctx: E2ETestContext):
        await ctx.client.start()
        unknown_session_id = "00000000-0000-0000-0000-000000000000"

        with pytest.raises(Exception) as exc_info:
            await ctx.client.delete_session(unknown_session_id)
        assert f"failed to delete session {unknown_session_id}" in str(exc_info.value).lower()

    async def test_should_get_null_last_session_id_before_any_sessions_exist(
        self, ctx: E2ETestContext
    ):
        await ctx.client.start()
        result = await ctx.client.get_last_session_id()
        assert result is None

    async def test_should_track_last_session_id_after_session_created(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        await session.send_and_wait("Say OK.")
        session_id = session.session_id
        await session.disconnect()

        last_id = await ctx.client.get_last_session_id()
        assert last_id == session_id

    async def test_should_get_null_foreground_session_id_in_headless_mode(
        self, ctx: E2ETestContext
    ):
        await ctx.client.start()
        session_id = await ctx.client.get_foreground_session_id()
        assert session_id is None

    async def test_should_report_error_when_setting_foreground_session_in_headless_mode(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            with pytest.raises(Exception) as exc_info:
                await ctx.client.set_foreground_session_id(session.session_id)
            err = str(exc_info.value).lower()
            assert "tui" in err or "server" in err
        finally:
            await session.disconnect()
