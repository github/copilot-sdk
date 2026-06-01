"""E2E Compaction Tests"""

import asyncio

import pytest

from copilot.session import PermissionHandler
from copilot.session_events import (
    SessionCompactionCompleteData,
    SessionCompactionStartData,
    SessionErrorData,
    SessionEventType,
)

from .testharness import E2ETestContext

pytestmark = [
    pytest.mark.asyncio(loop_scope="module"),
]


class TestCompaction:
    @pytest.mark.timeout(180)
    async def test_should_trigger_compaction_with_low_threshold_and_emit_events(
        self, ctx: E2ETestContext
    ):
        # Create session with very low compaction thresholds to trigger compaction quickly
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            infinite_sessions={
                "enabled": True,
                # Trigger background compaction at 0.5% context usage (~1000 tokens)
                "background_compaction_threshold": 0.005,
                # Block at 1% to ensure compaction runs
                "buffer_exhaustion_threshold": 0.01,
            },
        )

        # The first prompt leaves the session below the compaction processor's minimum
        # message count. The second prompt is therefore the first deterministic point
        # at which low thresholds can trigger compaction. Register event waiters before
        # any prompts are sent so we never miss the events.
        loop = asyncio.get_event_loop()
        compaction_started_future: asyncio.Future = loop.create_future()
        # Wait specifically for a *successful* compaction_complete so that any transient
        # failed compaction event the daemon may emit before a successful retry is ignored
        # (mirrors the dotnet/rust references).
        compaction_completed_future: asyncio.Future = loop.create_future()

        def _on_compaction_event(event):
            if (
                not compaction_started_future.done()
                and event.type == SessionEventType.SESSION_COMPACTION_START
                and isinstance(event.data, SessionCompactionStartData)
            ):
                compaction_started_future.set_result(event)
            elif (
                not compaction_completed_future.done()
                and event.type == SessionEventType.SESSION_COMPACTION_COMPLETE
                and isinstance(event.data, SessionCompactionCompleteData)
                and event.data.success
            ):
                compaction_completed_future.set_result(event)
            elif isinstance(event.data, SessionErrorData):
                msg = event.data.message or "session error"
                if not compaction_started_future.done():
                    compaction_started_future.set_exception(RuntimeError(msg))
                if not compaction_completed_future.done():
                    compaction_completed_future.set_exception(RuntimeError(msg))

        unsubscribe_compaction = session.on(_on_compaction_event)

        try:
            await session.send_and_wait("Tell me a story about a dragon. Be detailed.")
            await session.send_and_wait(
                "Continue the story with more details about the dragon's castle."
            )

            start_event = await asyncio.wait_for(compaction_started_future, timeout=60.0)
            complete_event = await asyncio.wait_for(compaction_completed_future, timeout=60.0)
        except BaseException:
            if not compaction_started_future.done():
                compaction_started_future.cancel()
            if not compaction_completed_future.done():
                compaction_completed_future.cancel()
            raise
        finally:
            unsubscribe_compaction()

        assert start_event.type == SessionEventType.SESSION_COMPACTION_START
        assert isinstance(start_event.data, SessionCompactionStartData)
        assert (start_event.data.conversation_tokens or 0) > 0, (
            "Expected compaction to report conversation tokens at start"
        )

        assert complete_event.type == SessionEventType.SESSION_COMPACTION_COMPLETE
        assert isinstance(complete_event.data, SessionCompactionCompleteData)
        assert complete_event.data.success is True, "Expected compaction to succeed"
        assert complete_event.data.compaction_tokens_used is not None, (
            "Expected compaction tokens-used data"
        )
        assert (complete_event.data.compaction_tokens_used.input_tokens or 0) > 0, (
            "Expected compaction call to consume input tokens"
        )
        summary = (complete_event.data.summary_content or "").lower()
        assert "<overview>" in summary, "Expected summary to contain <overview>"
        assert "<history>" in summary, "Expected summary to contain <history>"
        assert "<checkpoint_title>" in summary, "Expected summary to contain <checkpoint_title>"

        await session.send_and_wait("Now describe the dragon's treasure in great detail.")

        # Verify the session still works after compaction
        answer = await session.send_and_wait("What was the story about?")
        assert answer is not None
        content = (answer.data.content or "").lower()
        # Should remember it was about a dragon (context preserved via summary)
        assert "kaedrith" in content, f"Expected answer to mention 'Kaedrith', got: {content!r}"
        assert "dragon" in content, f"Expected answer to mention 'dragon', got: {content!r}"

    async def test_should_not_emit_compaction_events_when_infinite_sessions_disabled(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            infinite_sessions={"enabled": False},
        )

        compaction_events = []

        def on_event(event):
            if event.type in (
                SessionEventType.SESSION_COMPACTION_START,
                SessionEventType.SESSION_COMPACTION_COMPLETE,
            ):
                compaction_events.append(event)

        session.on(on_event)

        await session.send_and_wait("What is 2+2?")

        # Should not have any compaction events when disabled
        assert len(compaction_events) == 0, "Expected no compaction events when disabled"
