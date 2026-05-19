"""
Client lifecycle tests covering ``client.on(...)`` lifecycle event subscriptions
and connection-state transitions across ``start``/``stop``.

Mirrors ``dotnet/test/ClientLifecycleTests.cs`` plus the existing ``client_lifecycle``
nodejs scenarios so the YAML snapshots under ``test/snapshots/client_lifecycle/``
can be reused.
"""

from __future__ import annotations

import asyncio
import os

import pytest

from copilot import CopilotClient
from copilot.client import SubprocessConfig
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


async def _wait_for_condition(predicate, timeout: float = 10.0) -> None:
    deadline = asyncio.get_running_loop().time() + timeout
    while True:
        if predicate():
            return
        if asyncio.get_running_loop().time() >= deadline:
            raise TimeoutError("condition was not met before timeout")
        await asyncio.sleep(0.05)


async def _wait_for_last_session_id(client) -> str:
    last_id = None

    async def poll() -> bool:
        nonlocal last_id
        last_id = await client.get_last_session_id()
        return bool(last_id)

    deadline = asyncio.get_running_loop().time() + 10.0
    while True:
        if await poll():
            return last_id
        if asyncio.get_running_loop().time() >= deadline:
            raise TimeoutError("last session id was not persisted before timeout")
        await asyncio.sleep(0.05)


def _make_isolated_client(ctx: E2ETestContext) -> CopilotClient:
    """Build a client with the same isolated env as ctx.client but disjoint state.

    Used to exercise lifecycle tests that need a known-empty state directory
    or that explicitly drive start/stop transitions.
    """
    github_token = (
        "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
    )
    return CopilotClient(
        SubprocessConfig(
            cli_path=ctx.cli_path,
            cwd=ctx.work_dir,
            env=ctx.get_env(),
            github_token=github_token,
        )
    )


class TestClientLifecycle:
    async def test_should_return_last_session_id_after_sending_a_message(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await session.send_and_wait("Say hello")

            last_id = await _wait_for_last_session_id(ctx.client)
            assert last_id
        finally:
            await session.disconnect()

    async def test_should_emit_session_lifecycle_events(self, ctx: E2ETestContext):
        events: list = []
        unsubscribe = ctx.client.on(events.append)
        try:
            session = await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                await session.send_and_wait("Say hello")

                await _wait_for_condition(
                    lambda: any(
                        getattr(e, "sessionId", None) == session.session_id for e in events
                    ),
                    timeout=10.0,
                )
            finally:
                await session.disconnect()
        finally:
            unsubscribe()

    async def test_should_receive_session_created_lifecycle_event(self, ctx: E2ETestContext):
        loop = asyncio.get_event_loop()
        created: asyncio.Future = loop.create_future()

        def handler(event):
            if event.type == "session.created" and not created.done():
                created.set_result(event)

        unsubscribe = ctx.client.on(handler)
        try:
            session = await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                event = await asyncio.wait_for(created, 10.0)
                assert event.type == "session.created"
                assert event.sessionId == session.session_id
            finally:
                await session.disconnect()
        finally:
            unsubscribe()

    async def test_should_filter_session_lifecycle_events_by_type(self, ctx: E2ETestContext):
        loop = asyncio.get_event_loop()
        created: asyncio.Future = loop.create_future()

        def handler(event):
            if not created.done():
                created.set_result(event)

        unsubscribe = ctx.client.on("session.created", handler)
        try:
            session = await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                event = await asyncio.wait_for(created, 10.0)
                assert event.type == "session.created"
                assert event.sessionId == session.session_id
            finally:
                await session.disconnect()
        finally:
            unsubscribe()

    async def test_disposing_lifecycle_subscription_stops_receiving_events(
        self, ctx: E2ETestContext
    ):
        loop = asyncio.get_event_loop()
        unsubscribed_count = 0

        def disposed_handler(_event):
            nonlocal unsubscribed_count
            unsubscribed_count += 1

        unsubscribe_disposed = ctx.client.on(disposed_handler)
        unsubscribe_disposed()  # Immediately dispose first subscription.

        active_event: asyncio.Future = loop.create_future()
        unsubscribe_active = ctx.client.on(
            "session.created",
            lambda evt: active_event.set_result(evt) if not active_event.done() else None,
        )
        try:
            session = await ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                event = await asyncio.wait_for(active_event, 10.0)
                assert event.sessionId == session.session_id
                assert unsubscribed_count == 0, "Disposed handler should not have fired"
            finally:
                await session.disconnect()
        finally:
            unsubscribe_active()

    async def test_stop_disconnects_client_and_disposes_rpc_surface(self, ctx: E2ETestContext):
        client = _make_isolated_client(ctx)
        await client.start()
        try:
            assert client.get_state() == "connected"
        finally:
            await client.stop()

        assert client.get_state() == "disconnected"

        with pytest.raises(RuntimeError):
            _ = client.rpc

    async def test_should_receive_session_updated_lifecycle_event_for_non_ephemeral_activity(
        self, ctx: E2ETestContext
    ):
        """Changing session mode emits a session.updated lifecycle event."""
        from copilot.generated.rpc import ModeSetRequest, SessionMode

        loop = asyncio.get_event_loop()
        updated: asyncio.Future = loop.create_future()

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )

        def handler(event):
            if (
                event.type == "session.updated"
                and event.sessionId == session.session_id
                and not updated.done()
            ):
                updated.set_result(event)

        unsubscribe = ctx.client.on(handler)
        try:
            await session.rpc.mode.set(ModeSetRequest(mode=SessionMode.PLAN))
            event = await asyncio.wait_for(updated, timeout=15.0)
            assert event.type == "session.updated"
            assert event.sessionId == session.session_id
        finally:
            unsubscribe()
            await session.disconnect()

    async def test_should_receive_session_deleted_lifecycle_event_when_deleted(
        self, ctx: E2ETestContext
    ):
        """Deleting a session emits a session.deleted lifecycle event."""
        loop = asyncio.get_event_loop()
        deleted: asyncio.Future = loop.create_future()

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        session_id = session.session_id

        # Do a turn so the session is persisted
        message = await session.send_and_wait("Say SESSION_DELETED_OK exactly.", timeout=60.0)
        assert message is not None
        assert "SESSION_DELETED_OK" in (message.data.content or "")

        def handler(event):
            if (
                event.type == "session.deleted"
                and event.sessionId == session_id
                and not deleted.done()
            ):
                deleted.set_result(event)

        unsubscribe = ctx.client.on(handler)
        try:
            await session.disconnect()
            await ctx.client.delete_session(session_id)

            event = await asyncio.wait_for(deleted, timeout=15.0)
            assert event.type == "session.deleted"
            assert event.sessionId == session_id
        finally:
            unsubscribe()
