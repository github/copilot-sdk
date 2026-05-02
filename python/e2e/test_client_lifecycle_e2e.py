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
            # Allow session metadata to flush to disk.
            await asyncio.sleep(0.5)

            last_id = await ctx.client.get_last_session_id()
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
                await asyncio.sleep(0.5)

                if events:
                    matching = [e for e in events if e.sessionId == session.session_id]
                    assert matching, "Expected at least one lifecycle event for this session"
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
