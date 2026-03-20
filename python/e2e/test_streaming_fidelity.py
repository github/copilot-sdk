"""E2E Streaming Fidelity Tests"""

import os

import pytest

from copilot import CopilotClient
from copilot.client import SubprocessConfig
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestStreamingFidelity:
    async def test_should_produce_delta_events_when_streaming_is_enabled(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, streaming=True
        )

        events = []
        session.on(lambda event: events.append(event))

        await session.send_and_wait("Count from 1 to 5, separated by commas.")

        types = [e.type.value for e in events]

        # Should have streaming deltas before the final message
        delta_events = [e for e in events if e.type.value == "assistant.message_delta"]
        assert len(delta_events) >= 1

        # Deltas should have content
        for delta in delta_events:
            delta_content = getattr(delta.data, "delta_content", None)
            assert delta_content is not None
            assert isinstance(delta_content, str)

        # Should still have a final assistant.message
        assert "assistant.message" in types

        # Deltas should come before the final message
        first_delta_idx = types.index("assistant.message_delta")
        last_assistant_idx = len(types) - 1 - types[::-1].index("assistant.message")
        assert first_delta_idx < last_assistant_idx

        await session.disconnect()

    async def test_should_not_produce_deltas_when_streaming_is_disabled(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, streaming=False
        )

        events = []
        session.on(lambda event: events.append(event))

        await session.send_and_wait("Say 'hello world'.")

        delta_events = [e for e in events if e.type.value == "assistant.message_delta"]

        # No deltas when streaming is off
        assert len(delta_events) == 0

        # But should still have a final assistant.message
        assistant_events = [e for e in events if e.type.value == "assistant.message"]
        assert len(assistant_events) >= 1

        await session.disconnect()

    async def test_should_produce_deltas_after_session_resume(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, streaming=False
        )
        await session.send_and_wait("What is 3 + 6?")
        await session.disconnect()

        # Resume using a new client
        github_token = (
            "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
        )
        new_client = CopilotClient(
            SubprocessConfig(
                cli_path=ctx.cli_path,
                cwd=ctx.work_dir,
                env=ctx.get_env(),
                github_token=github_token,
            )
        )

        try:
            session2 = await new_client.resume_session(
                session.session_id,
                on_permission_request=PermissionHandler.approve_all,
                streaming=True,
            )
            events = []
            session2.on(lambda event: events.append(event))

            answer = await session2.send_and_wait("Now if you double that, what do you get?")
            assert answer is not None
            assert "18" in answer.data.content

            # Should have streaming deltas before the final message
            delta_events = [e for e in events if e.type.value == "assistant.message_delta"]
            assert len(delta_events) >= 1

            # Deltas should have content
            for delta in delta_events:
                delta_content = getattr(delta.data, "delta_content", None)
                assert delta_content is not None
                assert isinstance(delta_content, str)

            await session2.disconnect()
        finally:
            await new_client.force_stop()
