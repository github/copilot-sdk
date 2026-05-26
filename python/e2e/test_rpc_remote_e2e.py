"""E2E coverage for session.remote RPC methods."""

from __future__ import annotations

import asyncio
import time

import pytest

from copilot.generated.rpc import (
    RemoteEnableRequest,
    RemoteNotifySteerableChangedRequest,
    RemoteSessionMode,
    SessionsGetPersistedRemoteSteerableRequest,
)
from copilot.generated.session_events import SessionRemoteSteerableChangedData
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


async def _wait_for_remote_steerable_event(session, expected: bool) -> None:
    deadline = time.monotonic() + 30.0
    while time.monotonic() < deadline:
        events = await session.get_events()
        if any(
            isinstance(evt.data, SessionRemoteSteerableChangedData)
            and evt.data.remote_steerable is expected
            for evt in events
        ):
            return
        await asyncio.sleep(0.2)
    pytest.fail(f"Timed out waiting for session.remote_steerable_changed={expected}.")


def _assert_not_unhandled(exc: Exception, method: str) -> None:
    assert f"unhandled method {method}".lower() not in str(exc).lower()


class TestRpcRemote:
    async def test_remote_off_is_noop_or_implemented_error(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            try:
                result = await session.rpc.remote.enable(
                    RemoteEnableRequest(mode=RemoteSessionMode.OFF)
                )
            except Exception as exc:
                _assert_not_unhandled(exc, "session.remote.enable")
            else:
                assert result.remote_steerable is False
                assert not result.url
        finally:
            await session.disconnect()

    async def test_remote_disable_is_noop_or_implemented_error(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            try:
                await session.rpc.remote.disable()
            except Exception as exc:
                _assert_not_unhandled(exc, "session.remote.disable")
        finally:
            await session.disconnect()

    async def test_notify_steerable_changed_event_and_persist_flag(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await session.rpc.remote.notify_steerable_changed(
                RemoteNotifySteerableChangedRequest(remote_steerable=True)
            )
            await _wait_for_remote_steerable_event(session, True)
            persisted = await ctx.client.rpc.sessions.get_persisted_remote_steerable(
                SessionsGetPersistedRemoteSteerableRequest(session_id=session.session_id)
            )
            assert persisted.remote_steerable is True

            await session.rpc.remote.notify_steerable_changed(
                RemoteNotifySteerableChangedRequest(remote_steerable=False)
            )
            await _wait_for_remote_steerable_event(session, False)
            persisted = await ctx.client.rpc.sessions.get_persisted_remote_steerable(
                SessionsGetPersistedRemoteSteerableRequest(session_id=session.session_id)
            )
            assert persisted.remote_steerable is False
        finally:
            await session.disconnect()
