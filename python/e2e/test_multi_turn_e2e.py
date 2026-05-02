"""E2E tests for multi-turn tool-result continuity."""

from __future__ import annotations

from pathlib import Path

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestMultiTurn:
    async def test_should_use_tool_results_from_previous_turns(self, ctx: E2ETestContext):
        Path(ctx.work_dir, "secret.txt").write_text("The magic number is 42.", encoding="utf-8")
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            first_message = await session.send_and_wait(
                "Read the file 'secret.txt' and tell me what the magic number is."
            )
            assert first_message is not None
            assert "42" in first_message.data.content

            second_message = await session.send_and_wait(
                "What is that magic number multiplied by 2?"
            )
            assert second_message is not None
            assert "84" in second_message.data.content
        finally:
            await session.disconnect()

    async def test_should_handle_file_creation_then_reading_across_turns(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            await session.send_and_wait(
                "Create a file called 'greeting.txt' with the content 'Hello from multi-turn test'."
            )

            message = await session.send_and_wait(
                "Read the file 'greeting.txt' and tell me its exact contents."
            )
            assert message is not None
            assert "Hello from multi-turn test" in message.data.content
        finally:
            await session.disconnect()
