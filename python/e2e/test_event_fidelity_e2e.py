"""E2E tests for session event ordering and required event fields."""

from __future__ import annotations

from pathlib import Path

import pytest

from copilot.generated.session_events import (
    AssistantMessageData,
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
