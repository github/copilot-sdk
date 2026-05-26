"""E2E coverage for session.queue RPC methods."""

from __future__ import annotations

import asyncio
import time
import uuid

import pytest

from copilot.generated.rpc import (
    CommandsRespondToQueuedCommandRequest,
    EnqueueCommandParams,
    QueuedCommandHandled,
    QueuePendingItems,
    QueuePendingItemsKind,
    RegisterEventInterestParams,
    ReleaseEventInterestParams,
)
from copilot.generated.session_events import CommandQueuedData
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _is_pending_command(item: QueuePendingItems, command: str) -> bool:
    return item.kind == QueuePendingItemsKind.COMMAND and (
        item.display_text == command or command.lstrip("/") in item.display_text
    )


async def _wait_for_command_in_pending_items(session, command: str) -> QueuePendingItems:
    deadline = time.monotonic() + 30.0
    last_items = []
    while time.monotonic() < deadline:
        pending = await session.rpc.queue.pending_items()
        last_items = pending.items
        for item in pending.items:
            if _is_pending_command(item, command):
                assert item.kind == QueuePendingItemsKind.COMMAND
                assert command.lstrip("/") in item.display_text
                return item
        await asyncio.sleep(0.2)
    raise AssertionError(f"Timed out waiting for {command!r} in pending items: {last_items!r}")


async def _wait_for_command_not_in_pending_items(session, command: str) -> None:
    deadline = time.monotonic() + 30.0
    while time.monotonic() < deadline:
        pending = await session.rpc.queue.pending_items()
        if not any(_is_pending_command(item, command) for item in pending.items):
            return
        await asyncio.sleep(0.2)
    pytest.fail(f"Timed out waiting for {command!r} to leave pending items.")


async def _assert_queue_empty(session) -> None:
    pending = await session.rpc.queue.pending_items()
    assert pending.items == []
    assert pending.steering_messages == []


class TestRpcQueue:
    async def test_fresh_queue_is_empty_and_empty_mutations_are_noops(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await _assert_queue_empty(session)

            remove = await session.rpc.queue.remove_most_recent()
            assert remove.removed is False
            await _assert_queue_empty(session)

            await session.rpc.queue.clear()
            await _assert_queue_empty(session)

            remove_after_clear = await session.rpc.queue.remove_most_recent()
            assert remove_after_clear.removed is False
        finally:
            await session.disconnect()

    async def test_pending_items_reports_queued_command_and_mutations_update_queue(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        interest = None
        first_event = None
        responded_to_first = False
        try:
            interest = await session.rpc.event_log.register_interest(
                RegisterEventInterestParams(event_type="command.queued")
            )

            first_command = f"/sdk-queue-first-{uuid.uuid4().hex}"
            second_command = f"/sdk-queue-second-{uuid.uuid4().hex}"
            third_command = f"/sdk-queue-third-{uuid.uuid4().hex}"
            first_queued: asyncio.Future = asyncio.get_event_loop().create_future()

            def on_event(event):
                if (
                    isinstance(event.data, CommandQueuedData)
                    and event.data.command == first_command
                    and not first_queued.done()
                ):
                    first_queued.set_result(event)

            unsubscribe = session.on(on_event)
            try:
                first = await session.rpc.commands.enqueue(
                    EnqueueCommandParams(command=first_command)
                )
                assert first.queued is True
                first_event = await asyncio.wait_for(first_queued, timeout=30.0)
            finally:
                unsubscribe()

            second = await session.rpc.commands.enqueue(
                EnqueueCommandParams(command=second_command)
            )
            assert second.queued is True
            await _wait_for_command_in_pending_items(session, second_command)

            remove = await session.rpc.queue.remove_most_recent()
            assert remove.removed is True
            await _wait_for_command_not_in_pending_items(session, second_command)

            third = await session.rpc.commands.enqueue(EnqueueCommandParams(command=third_command))
            assert third.queued is True
            await _wait_for_command_in_pending_items(session, third_command)

            await session.rpc.queue.clear()
            await _wait_for_command_not_in_pending_items(session, third_command)

            completed = await session.rpc.commands.respond_to_queued_command(
                CommandsRespondToQueuedCommandRequest(
                    request_id=first_event.data.request_id,
                    result=QueuedCommandHandled(stop_processing_queue=True),
                )
            )
            responded_to_first = completed.success
            assert completed.success is True

            deadline = time.monotonic() + 30.0
            while time.monotonic() < deadline:
                pending = await session.rpc.queue.pending_items()
                if pending.items == [] and pending.steering_messages == []:
                    break
                await asyncio.sleep(0.2)
            await _assert_queue_empty(session)
        finally:
            if not responded_to_first and first_event is not None:
                await session.rpc.commands.respond_to_queued_command(
                    CommandsRespondToQueuedCommandRequest(
                        request_id=first_event.data.request_id,
                        result=QueuedCommandHandled(stop_processing_queue=True),
                    )
                )
            await session.rpc.queue.clear()
            if interest is not None and interest.handle:
                await session.rpc.event_log.release_interest(
                    ReleaseEventInterestParams(handle=interest.handle)
                )
            await session.disconnect()
