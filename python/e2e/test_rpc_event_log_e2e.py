"""E2E coverage for session.eventLog RPC methods."""

from __future__ import annotations

import asyncio
import time
import uuid
from collections.abc import Awaitable, Callable
from typing import TYPE_CHECKING

import pytest

from copilot.rpc import (
    EventLogReadRequest,
    EventsCursorStatus,
    NameSetRequest,
    PlanUpdateRequest,
    RegisterEventInterestParams,
    ReleaseEventInterestParams,
    SessionsSaveRequest,
)
from copilot.session import PermissionHandler
from copilot.session_events import (
    PlanChangedOperation,
    SessionPlanChangedData,
    SessionTitleChangedData,
)

if TYPE_CHECKING:
    from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


async def _wait_for(
    predicate: Callable[[], Awaitable[bool]],
    *,
    timeout: float = 30.0,
    message: str,
) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if await predicate():
            return
        await asyncio.sleep(0.2)
    pytest.fail(message)


class TestRpcEventLog:
    async def test_should_read_persisted_events_from_beginning(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await session.rpc.plan.update(
                PlanUpdateRequest(content="# Event log E2E plan\n- persisted event")
            )

            observed = None

            async def has_plan_event() -> bool:
                nonlocal observed
                observed = await session.rpc.event_log.read(EventLogReadRequest(max=100, wait_ms=0))
                return any(
                    isinstance(evt.data, SessionPlanChangedData)
                    and evt.data.operation == PlanChangedOperation.CREATE
                    and evt.ephemeral is not True
                    for evt in observed.events
                )

            await _wait_for(
                has_plan_event,
                message="Timed out waiting for persisted session.plan_changed event.",
            )

            assert observed is not None
            assert observed.cursor_status == EventsCursorStatus.OK
            assert observed.cursor
            assert any(
                isinstance(evt.data, SessionPlanChangedData)
                and evt.data.operation == PlanChangedOperation.CREATE
                for evt in observed.events
            )
        finally:
            await session.disconnect()

    async def test_should_return_tail_cursor_and_read_empty_when_no_new_events(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            tail = await session.rpc.event_log.tail()
            await session.log("Ephemeral event after tail", ephemeral=True)
            request = EventLogReadRequest(
                cursor=tail.cursor, max=10, wait_ms=0, include_ephemeral=False
            )
            read = await session.rpc.event_log.read(request)

            assert tail.cursor
            assert read.cursor_status == EventsCursorStatus.OK
            assert read.events == []
            assert read.has_more is False

            await session.rpc.plan.update(PlanUpdateRequest(content="# Durable event after tail"))
            await ctx.client.rpc.sessions.save(SessionsSaveRequest(session_id=session.session_id))
            read = await session.rpc.event_log.read(request)
            assert read.cursor_status == EventsCursorStatus.OK
            assert any(
                isinstance(evt.data, SessionPlanChangedData)
                and evt.data.operation == PlanChangedOperation.CREATE
                and evt.ephemeral is not True
                for evt in read.events
            )
        finally:
            await session.disconnect()

    async def test_should_register_and_release_event_interest_idempotently(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            registered = await session.rpc.event_log.register_interest(
                RegisterEventInterestParams(event_type="session.title_changed")
            )
            assert registered.handle

            released = await session.rpc.event_log.release_interest(
                ReleaseEventInterestParams(handle=registered.handle)
            )
            assert released.success is True

            released_again = await session.rpc.event_log.release_interest(
                ReleaseEventInterestParams(handle=registered.handle)
            )
            assert released_again.success is True
        finally:
            await session.disconnect()

    async def test_should_long_poll_with_types_filter_for_title_changed_event(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            expected_title = f"EventLogTitle-{uuid.uuid4().hex}"
            tail = await session.rpc.event_log.tail()
            last_read = None

            async def read_until_title():
                nonlocal last_read
                cursor = tail.cursor
                while True:
                    last_read = await session.rpc.event_log.read(
                        EventLogReadRequest(
                            cursor=cursor,
                            max=10,
                            wait_ms=5000,
                            types=["session.title_changed"],
                        )
                    )
                    assert last_read.cursor_status == EventsCursorStatus.OK
                    assert all(
                        evt.type.value == "session.title_changed" for evt in last_read.events
                    )
                    if any(
                        isinstance(evt.data, SessionTitleChangedData)
                        and evt.data.title == expected_title
                        for evt in last_read.events
                    ):
                        return
                    # Any new event can wake a long poll, even if the filter removes it.
                    cursor = last_read.cursor

            read_task = asyncio.create_task(read_until_title())
            try:
                await session.rpc.name.set(NameSetRequest(name=expected_title))
                try:
                    await asyncio.wait_for(read_task, timeout=10.0)
                except TimeoutError as exc:
                    exc.add_note(f"Expected title {expected_title!r}; last batch: {last_read!r}")
                    raise
            finally:
                read_task.cancel()
                await asyncio.gather(read_task, return_exceptions=True)
        finally:
            await session.disconnect()
