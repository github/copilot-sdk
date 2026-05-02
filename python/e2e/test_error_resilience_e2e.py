"""E2E tests for session lifecycle error handling."""

from __future__ import annotations

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestErrorResilience:
    async def test_should_throw_when_sending_to_disconnected_session(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        await session.disconnect()

        with pytest.raises(Exception):
            await session.send_and_wait("Hello")

    async def test_should_throw_when_getting_messages_from_disconnected_session(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        await session.disconnect()

        with pytest.raises(Exception):
            await session.get_messages()

    async def test_should_handle_double_abort_without_error(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            await session.abort()
            await session.abort()
        finally:
            await session.disconnect()

    async def test_should_throw_when_resuming_non_existent_session(self, ctx: E2ETestContext):
        with pytest.raises(Exception):
            await ctx.client.resume_session(
                "non-existent-session-id-12345",
                on_permission_request=PermissionHandler.approve_all,
            )
