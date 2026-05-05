"""E2E tests for multi-turn tool-result continuity."""

from __future__ import annotations

from pathlib import Path
from typing import Any

import pytest

from copilot.generated.session_events import (
    AssistantMessageData,
    SessionIdleData,
    ToolExecutionCompleteData,
    ToolExecutionStartData,
    UserMessageData,
)
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _assert_tool_turn_ordering(events: list[Any], turn_description: str) -> None:
    """Assert that within a turn's events, the ordering contract holds:
    user.message → tool.execution_start(s) → tool.execution_complete(s)
    → assistant.message → session.idle
    """
    types = [e.type.value for e in events]
    observed = ", ".join(types)

    user_idx = next((i for i, e in enumerate(events) if isinstance(e.data, UserMessageData)), -1)
    tool_starts = [
        (i, e) for i, e in enumerate(events) if isinstance(e.data, ToolExecutionStartData)
    ]
    tool_completes = [
        (i, e) for i, e in enumerate(events) if isinstance(e.data, ToolExecutionCompleteData)
    ]

    assert user_idx >= 0, f"Expected user.message in {turn_description}. Observed: {observed}"
    assert tool_starts, f"Expected tool.execution_start events in {turn_description}"
    assert tool_completes, f"Expected tool.execution_complete events in {turn_description}"

    first_tool_start_idx = tool_starts[0][0]
    assert user_idx < first_tool_start_idx, (
        f"Expected user.message before first tool start in {turn_description}. Observed: {observed}"
    )

    # Each complete should have a matching start with same tool_call_id
    complete_call_ids = {e.data.tool_call_id for _, e in tool_completes}
    start_call_ids = {e.data.tool_call_id for _, e in tool_starts}
    for cid in complete_call_ids:
        assert cid in start_call_ids, (
            f"tool.execution_complete call_id {cid} has no matching start in {turn_description}"
        )

    last_tool_complete_idx = tool_completes[-1][0]
    # Find assistant.message after last tool complete
    assistant_after_tools_idx = next(
        (
            i
            for i, e in enumerate(events)
            if i > last_tool_complete_idx and isinstance(e.data, AssistantMessageData)
        ),
        -1,
    )
    idle_idx = next(
        (
            i
            for i, e in enumerate(events)
            if i > max(assistant_after_tools_idx, 0) and isinstance(e.data, SessionIdleData)
        ),
        -1,
    )

    assert assistant_after_tools_idx >= 0, (
        "Expected assistant.message after tool completion in "
        f"{turn_description}. Observed: {observed}"
    )
    assert idle_idx >= 0, (
        f"Expected session.idle after assistant.message in {turn_description}. Observed: {observed}"
    )
    assert last_tool_complete_idx < assistant_after_tools_idx, (
        f"Expected final tool completion before final assistant message in {turn_description}. "
        f"Observed: {observed}"
    )
    assert assistant_after_tools_idx < idle_idx, (
        f"Expected final assistant message before idle in {turn_description}. Observed: {observed}"
    )


class TestMultiTurn:
    async def test_should_use_tool_results_from_previous_turns(self, ctx: E2ETestContext):
        Path(ctx.work_dir, "secret.txt").write_text("The magic number is 42.", encoding="utf-8")
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        events: list = []
        unsubscribe = session.on(events.append)
        try:
            first_message = await session.send_and_wait(
                "Read the file 'secret.txt' and tell me what the magic number is."
            )
            assert first_message is not None
            assert "42" in first_message.data.content
            turn1_events = list(events)
            events.clear()
            _assert_tool_turn_ordering(turn1_events, "file read turn")

            second_message = await session.send_and_wait(
                "What is that magic number multiplied by 2?"
            )
            assert second_message is not None
            assert "84" in second_message.data.content
        finally:
            unsubscribe()
            await session.disconnect()

    async def test_should_handle_file_creation_then_reading_across_turns(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        events: list = []
        unsubscribe = session.on(events.append)
        try:
            await session.send_and_wait(
                "Create a file called 'greeting.txt' with the content 'Hello from multi-turn test'."
            )
            turn1_events = list(events)
            events.clear()
            _assert_tool_turn_ordering(turn1_events, "file creation turn")
            assert Path(ctx.work_dir, "greeting.txt").read_text(encoding="utf-8") == (
                "Hello from multi-turn test"
            )

            message = await session.send_and_wait(
                "Read the file 'greeting.txt' and tell me its exact contents."
            )
            assert message is not None
            assert "Hello from multi-turn test" in message.data.content
            turn2_events = list(events)
            _assert_tool_turn_ordering(turn2_events, "file read turn")
        finally:
            unsubscribe()
            await session.disconnect()
