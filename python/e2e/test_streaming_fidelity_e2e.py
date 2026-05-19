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

    async def test_should_not_produce_deltas_after_session_resume_with_streaming_disabled(
        self, ctx: E2ETestContext
    ):
        """Resume with streaming=False — no delta events, but final message arrives."""
        github_token = (
            "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
        )
        # Create and complete a turn with streaming enabled
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, streaming=True
        )
        await session.send_and_wait("What is 3 + 6?")
        session_id = session.session_id
        await session.disconnect()

        # Resume with streaming disabled
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
                session_id,
                on_permission_request=PermissionHandler.approve_all,
                streaming=False,
            )
            events = []
            session2.on(lambda event: events.append(event))

            answer = await session2.send_and_wait("Now if you double that, what do you get?")
            assert answer is not None

            delta_events = [e for e in events if e.type.value == "assistant.message_delta"]
            assert len(delta_events) == 0, "No deltas expected when streaming=False"

            assistant_events = [e for e in events if e.type.value == "assistant.message"]
            assert len(assistant_events) >= 1, "Final assistant.message must still arrive"

            await session2.disconnect()
        finally:
            await new_client.force_stop()

    async def test_should_emit_streaming_deltas_with_reasoning_effort_configured(
        self, ctx: E2ETestContext
    ):
        """Streaming + reasoning_effort produces delta events and session.start shows effort."""
        from copilot.generated.session_events import SessionStartData

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            streaming=True,
            reasoning_effort="high",
        )

        events = []
        session.on(lambda event: events.append(event))

        try:
            await session.send_and_wait("What is 15 * 17?", timeout=60.0)

            delta_events = [e for e in events if e.type.value == "assistant.message_delta"]
            assert len(delta_events) >= 1, "Expected delta events with streaming=True"

            assistant_events = [e for e in events if e.type.value == "assistant.message"]
            assert len(assistant_events) >= 1, "Expected final assistant.message"

            # Check session.start event (from get_messages) has reasoning_effort
            all_msgs = await session.get_messages()
            start_event = next((e for e in all_msgs if isinstance(e.data, SessionStartData)), None)
            assert start_event is not None, "Expected session.start event"
            assert start_event.data.reasoning_effort == "high"
        finally:
            await session.disconnect()
