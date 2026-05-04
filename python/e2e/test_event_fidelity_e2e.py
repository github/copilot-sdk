"""E2E tests for session event ordering and required event fields."""

from __future__ import annotations

import asyncio
from pathlib import Path

import pytest

from copilot.generated.session_events import (
    AssistantMessageData,
    AssistantUsageData,
    PendingMessagesModifiedData,
    SessionUsageInfoData,
    ToolExecutionCompleteData,
    ToolExecutionStartData,
    UserMessageData,
)
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestEventFidelity:
    async def test_should_emit_events_in_correct_order_for_tool_using_conversation(
        self, ctx: E2ETestContext
    ):
        Path(ctx.work_dir, "hello.txt").write_text("Hello World", encoding="utf-8")

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        events = []
        unsubscribe = session.on(events.append)
        try:
            await session.send_and_wait("Read the file 'hello.txt' and tell me its contents.")

            types = [event.type.value for event in events]

            assert "user.message" in types
            assert "assistant.message" in types

            user_idx = types.index("user.message")
            assistant_idx = len(types) - 1 - types[::-1].index("assistant.message")
            assert user_idx < assistant_idx

            idle_idx = len(types) - 1 - types[::-1].index("session.idle")
            assert idle_idx == len(types) - 1
        finally:
            unsubscribe()
            await session.disconnect()

    async def test_should_include_valid_fields_on_all_events(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        events = []
        unsubscribe = session.on(events.append)
        try:
            await session.send_and_wait("What is 5+5? Reply with just the number.")

            for event in events:
                assert event.id is not None
                assert str(event.id)
                assert event.timestamp is not None

            user_event = next(
                (event for event in events if isinstance(event.data, UserMessageData)), None
            )
            assert user_event is not None
            assert user_event.data.content

            assistant_event = next(
                (event for event in events if isinstance(event.data, AssistantMessageData)),
                None,
            )
            assert assistant_event is not None
            assert assistant_event.data.message_id
            assert assistant_event.data.content is not None
        finally:
            unsubscribe()
            await session.disconnect()

    async def test_should_emit_tool_execution_events_with_correct_fields(self, ctx: E2ETestContext):
        Path(ctx.work_dir, "data.txt").write_text("test data", encoding="utf-8")

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        events = []
        unsubscribe = session.on(events.append)
        try:
            await session.send_and_wait("Read the file 'data.txt'.")

            tool_starts = [
                event for event in events if isinstance(event.data, ToolExecutionStartData)
            ]
            tool_completes = [
                event for event in events if isinstance(event.data, ToolExecutionCompleteData)
            ]

            assert len(tool_starts) >= 1
            assert len(tool_completes) >= 1

            assert tool_starts[0].data.tool_call_id
            assert tool_starts[0].data.tool_name
            assert tool_completes[0].data.tool_call_id
        finally:
            unsubscribe()
            await session.disconnect()

    async def test_should_emit_assistant_message_with_messageid(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        events = []
        unsubscribe = session.on(events.append)
        try:
            await session.send_and_wait("Say 'pong'.")

            assistant_events = [
                event for event in events if isinstance(event.data, AssistantMessageData)
            ]
            assert len(assistant_events) >= 1

            message = assistant_events[0]
            assert message.data.message_id
            assert "pong" in message.data.content
        finally:
            unsubscribe()
            await session.disconnect()

    async def test_should_emit_assistant_usage_event_after_model_call(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        events = []
        unsubscribe = session.on(events.append)
        try:
            await session.send_and_wait("What is 5+5? Reply with just the number.")

            usage_events = [e for e in events if isinstance(e.data, AssistantUsageData)]
            assert len(usage_events) >= 1, "Expected at least one assistant.usage event"

            last_usage = usage_events[-1]
            assert last_usage.id is not None
            assert last_usage.timestamp is not None
            assert last_usage.data.model
        finally:
            unsubscribe()
            await session.disconnect()

    async def test_should_emit_session_usage_info_event_after_model_call(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        events = []
        unsubscribe = session.on(events.append)
        try:
            await session.send_and_wait("What is 5+5? Reply with just the number.")

            usage_info_events = [e for e in events if isinstance(e.data, SessionUsageInfoData)]
            assert len(usage_info_events) >= 1, "Expected at least one session.usage_info event"

            last_info = usage_info_events[-1]
            assert last_info.data.current_tokens > 0
            assert last_info.data.messages_length > 0
            assert last_info.data.token_limit > 0
        finally:
            unsubscribe()
            await session.disconnect()

    async def test_should_emit_pending_messages_modified_event_when_message_queue_changes(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        pending_task: asyncio.Future = asyncio.get_event_loop().create_future()

        def on_event(event):
            if isinstance(event.data, PendingMessagesModifiedData) and not pending_task.done():
                pending_task.set_result(event)

        unsubscribe = session.on(on_event)
        try:
            # Fire-and-forget to trigger pending_messages.modified; then wait for it
            asyncio.ensure_future(session.send("What is 9+9? Reply with just the number."))
            pending_event = await asyncio.wait_for(pending_task, timeout=60.0)
            assert pending_event is not None

            from .testharness.helper import get_final_assistant_message

            answer = await get_final_assistant_message(session, timeout=60.0)
            assert answer is not None
            assert "18" in (answer.data.content or "")
        finally:
            unsubscribe()
            await session.disconnect()

    async def test_should_preserve_message_order_in_getmessages_after_tool_use(
        self, ctx: E2ETestContext
    ):
        Path(ctx.work_dir, "order.txt").write_text("ORDER_CONTENT_42", encoding="utf-8")

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            await session.send_and_wait("Read the file 'order.txt' and tell me what the number is.")

            messages = await session.get_messages()
            types = [m.type.value for m in messages]

            # Verify complete event ordering contract:
            # session.start → user.message → tool.execution_start → tool.execution_complete
            # → assistant.message
            def first_index(t: str) -> int:
                return types.index(t) if t in types else -1

            def last_index(t: str) -> int:
                return len(types) - 1 - types[::-1].index(t) if t in types else -1

            session_start_idx = first_index("session.start")
            user_msg_idx = first_index("user.message")
            tool_start_idx = first_index("tool.execution_start")
            tool_complete_idx = first_index("tool.execution_complete")
            assistant_msg_idx = last_index("assistant.message")

            assert session_start_idx >= 0, "Expected session.start event"
            assert user_msg_idx >= 0, "Expected user.message event"
            assert tool_start_idx >= 0, "Expected tool.execution_start event"
            assert tool_complete_idx >= 0, "Expected tool.execution_complete event"
            assert assistant_msg_idx >= 0, "Expected assistant.message event"

            assert session_start_idx < user_msg_idx, "session.start should precede user.message"
            assert user_msg_idx < tool_start_idx, "user.message should precede tool.execution_start"
            assert tool_start_idx < tool_complete_idx, (
                "tool.execution_start should precede tool.execution_complete"
            )
            assert tool_complete_idx < assistant_msg_idx, (
                "tool.execution_complete should precede final assistant.message"
            )

            # Verify user.message has our content
            user_events = [m for m in messages if isinstance(m.data, UserMessageData)]
            assert any("order.txt" in (e.data.content or "") for e in user_events)

            # Verify assistant.message references the file content
            assistant_events = [m for m in messages if isinstance(m.data, AssistantMessageData)]
            assert any("42" in (e.data.content or "") for e in assistant_events)
        finally:
            await session.disconnect()
