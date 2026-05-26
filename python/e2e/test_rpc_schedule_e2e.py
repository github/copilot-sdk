"""E2E coverage for session.schedule RPC methods."""

from __future__ import annotations

import pytest

from copilot.generated.rpc import ScheduleStopRequest
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestRpcSchedule:
    async def test_should_list_no_schedules_for_fresh_session(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            result = await session.rpc.schedule.list()
            assert result.entries == []
        finally:
            await session.disconnect()

    async def test_should_return_null_entry_when_stopping_unknown_schedule(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            result = await session.rpc.schedule.stop(ScheduleStopRequest(id=2_147_483_647))
            assert result.entry is None
            assert (await session.rpc.schedule.list()).entries == []
        finally:
            await session.disconnect()
