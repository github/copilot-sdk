"""
E2E coverage for session-event side effects triggered by RPC calls.

Mirrors ``dotnet/test/RpcEventSideEffectsE2ETests.cs`` (snapshot category
``rpc_event_side_effects``).
"""

from __future__ import annotations

import asyncio

import pytest

from copilot.generated.rpc import (
    HistoryTruncateRequest,
    ModeSetRequest,
    NameSetRequest,
    PlanUpdateRequest,
    SessionMode,
    WorkspacesCreateFileRequest,
)
from copilot.generated.session_events import (
    PlanChangedOperation,
    SessionModeChangedData,
    SessionPlanChangedData,
    SessionSnapshotRewindData,
    SessionTitleChangedData,
    SessionWorkspaceFileChangedData,
    WorkspaceFileChangedOperation,
)
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


async def _wait_for_event(session, predicate, timeout: float = 15.0):
    """Wait for the first session event matching predicate."""
    loop = asyncio.get_event_loop()
    fut: asyncio.Future = loop.create_future()

    def on_event(event):
        if not fut.done() and predicate(event):
            fut.set_result(event)

    unsub = session.on(on_event)
    try:
        return await asyncio.wait_for(fut, timeout=timeout)
    finally:
        unsub()


class TestRpcEventSideEffects:
    async def test_should_emit_mode_changed_event_when_mode_set(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            changed_future: asyncio.Future = asyncio.get_event_loop().create_future()

            def on_event(event):
                if isinstance(event.data, SessionModeChangedData) and not changed_future.done():
                    changed_future.set_result(event)

            unsubscribe = session.on(on_event)
            try:
                await session.rpc.mode.set(ModeSetRequest(mode=SessionMode.PLAN))
                event = await asyncio.wait_for(changed_future, timeout=15.0)

                assert isinstance(event.data, SessionModeChangedData)
                assert event.data.new_mode == SessionMode.PLAN.value
                assert event.data.previous_mode == SessionMode.INTERACTIVE.value
            finally:
                unsubscribe()
        finally:
            await session.disconnect()

    async def test_should_emit_plan_changed_event_for_update_and_delete(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            create_future: asyncio.Future = asyncio.get_event_loop().create_future()
            delete_future: asyncio.Future = asyncio.get_event_loop().create_future()

            def on_event(event):
                if isinstance(event.data, SessionPlanChangedData):
                    if (
                        event.data.operation == PlanChangedOperation.CREATE
                        and not create_future.done()
                    ):
                        create_future.set_result(event)
                    elif (
                        event.data.operation == PlanChangedOperation.DELETE
                        and not delete_future.done()
                    ):
                        delete_future.set_result(event)

            unsubscribe = session.on(on_event)
            try:
                await session.rpc.plan.update(PlanUpdateRequest(content="# Plan step 1"))
                create_evt = await asyncio.wait_for(create_future, timeout=15.0)
                assert create_evt.data.operation == PlanChangedOperation.CREATE

                await session.rpc.plan.delete()
                delete_evt = await asyncio.wait_for(delete_future, timeout=15.0)
                assert delete_evt.data.operation == PlanChangedOperation.DELETE
            finally:
                unsubscribe()
        finally:
            await session.disconnect()

    async def test_should_emit_plan_changed_update_operation_on_second_update(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            # Create the plan first
            await session.rpc.plan.update(PlanUpdateRequest(content="# Initial plan"))

            update_future: asyncio.Future = asyncio.get_event_loop().create_future()

            def on_event(event):
                if (
                    isinstance(event.data, SessionPlanChangedData)
                    and event.data.operation == PlanChangedOperation.UPDATE
                    and not update_future.done()
                ):
                    update_future.set_result(event)

            unsubscribe = session.on(on_event)
            try:
                await session.rpc.plan.update(PlanUpdateRequest(content="# Updated plan"))
                update_evt = await asyncio.wait_for(update_future, timeout=15.0)
                assert update_evt.data.operation == PlanChangedOperation.UPDATE
            finally:
                unsubscribe()
        finally:
            await session.disconnect()

    async def test_should_emit_workspace_file_changed_event_when_file_created(
        self, ctx: E2ETestContext
    ):
        import uuid

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            path = f"event-side-effect-{uuid.uuid4().hex}.txt"
            create_future: asyncio.Future = asyncio.get_event_loop().create_future()

            def on_event(event):
                if (
                    isinstance(event.data, SessionWorkspaceFileChangedData)
                    and event.data.path == path
                    and event.data.operation == WorkspaceFileChangedOperation.CREATE
                    and not create_future.done()
                ):
                    create_future.set_result(event)

            unsubscribe = session.on(on_event)
            try:
                await session.rpc.workspaces.create_file(
                    WorkspacesCreateFileRequest(path=path, content="hello")
                )
                evt = await asyncio.wait_for(create_future, timeout=15.0)
                assert evt.data.path == path
                assert evt.data.operation == WorkspaceFileChangedOperation.CREATE
            finally:
                unsubscribe()
        finally:
            await session.disconnect()

    async def test_should_emit_title_changed_event_when_name_set(self, ctx: E2ETestContext):
        import uuid

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            new_name = f"Title-{uuid.uuid4().hex}"
            title_future: asyncio.Future = asyncio.get_event_loop().create_future()

            def on_event(event):
                if (
                    isinstance(event.data, SessionTitleChangedData)
                    and event.data.title == new_name
                    and not title_future.done()
                ):
                    title_future.set_result(event)

            unsubscribe = session.on(on_event)
            try:
                await session.rpc.name.set(NameSetRequest(name=new_name))
                evt = await asyncio.wait_for(title_future, timeout=15.0)
                assert evt.data.title == new_name
            finally:
                unsubscribe()
        finally:
            await session.disconnect()

    async def test_should_emit_snapshot_rewind_event_and_remove_events_on_truncate(
        self, ctx: E2ETestContext
    ):
        """Truncating history emits a session.snapshot_rewind event."""
        from copilot.generated.session_events import UserMessageData

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await session.send_and_wait("Say SNAPSHOT_REWIND_TARGET exactly.", timeout=60.0)

            events = await session.get_messages()
            user_msgs = [e for e in events if isinstance(e.data, UserMessageData)]
            assert len(user_msgs) >= 1
            first_user_event_id = str(user_msgs[0].id)

            rewind_future: asyncio.Future = asyncio.get_event_loop().create_future()

            def on_event(event):
                if isinstance(event.data, SessionSnapshotRewindData) and not rewind_future.done():
                    rewind_future.set_result(event)

            unsubscribe = session.on(on_event)
            try:
                await session.rpc.history.truncate(
                    HistoryTruncateRequest(event_id=first_user_event_id)
                )
                evt = await asyncio.wait_for(rewind_future, timeout=15.0)
                assert isinstance(evt.data, SessionSnapshotRewindData)
                assert evt.data.events_removed >= 1
                assert evt.data.up_to_event_id.lower() == first_user_event_id.lower()

                messages_after = await session.get_messages()
                assert not any(e.id == user_msgs[0].id for e in messages_after)
            except Exception as exc:
                if "unhandled method" in str(exc).lower():
                    pytest.skip("session.history.truncate not supported in this CLI build")
                raise
            finally:
                unsubscribe()
        finally:
            await session.disconnect()

    async def test_should_allow_session_use_after_truncate(self, ctx: E2ETestContext):
        """Session remains usable after history truncation."""
        from copilot.generated.session_events import UserMessageData

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await session.send_and_wait("Say SNAPSHOT_REWIND_TARGET exactly.", timeout=60.0)

            events = await session.get_messages()
            user_msgs = [e for e in events if isinstance(e.data, UserMessageData)]
            assert len(user_msgs) >= 1
            first_user_event_id = str(user_msgs[0].id)

            try:
                truncate_result = await session.rpc.history.truncate(
                    HistoryTruncateRequest(event_id=first_user_event_id)
                )
                assert truncate_result.events_removed >= 1
            except Exception as exc:
                if "unhandled method" in str(exc).lower():
                    pytest.skip("session.history.truncate not supported in this CLI build")
                raise

            mode = await session.rpc.mode.get()
            assert mode in (
                SessionMode.INTERACTIVE,
                SessionMode.PLAN,
                SessionMode.AUTOPILOT,
            )
            workspace = await session.rpc.workspaces.get_workspace()
            assert workspace is not None
        finally:
            await session.disconnect()
