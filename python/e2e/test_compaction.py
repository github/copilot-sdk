"""E2E Compaction Tests"""

import pytest

from copilot.generated.session_events import SessionEventType

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestCompaction:
    @pytest.mark.timeout(120)
    async def test_should_trigger_compaction_with_low_threshold_and_emit_events(
        self, ctx: E2ETestContext
    ):
        # Create session with very low compaction thresholds to trigger compaction quickly
        session = await ctx.client.create_session(
            {
                "infinite_sessions": {
                    "enabled": True,
                    # Trigger background compaction at 0.5% context usage (~1000 tokens)
                    "background_compaction_threshold": 0.005,
                    # Block at 1% to ensure compaction runs
                    "buffer_exhaustion_threshold": 0.01,
                }
            }
        )

        compaction_start_events = []
        compaction_complete_events = []

        def on_event(event):
            if event.type == SessionEventType.SESSION_COMPACTION_START:
                compaction_start_events.append(event)
            if event.type == SessionEventType.SESSION_COMPACTION_COMPLETE:
                compaction_complete_events.append(event)

        session.on(on_event)

        # Send multiple messages to fill up the context window
        await session.send_and_wait(
            {"prompt": "Tell me a long story about a dragon. Be very detailed."}
        )
        await session.send_and_wait(
            {"prompt": "Continue the story with more details about the dragon's castle."}
        )
        await session.send_and_wait(
            {"prompt": "Now describe the dragon's treasure in great detail."}
        )

        # Should have triggered compaction at least once
        assert len(compaction_start_events) >= 1, "Expected at least 1 compaction_start event"
        assert len(compaction_complete_events) >= 1, "Expected at least 1 compaction_complete event"

        # Compaction should have succeeded
        last_complete = compaction_complete_events[-1]
        assert last_complete.data.success is True, "Expected compaction to succeed"

        # Should have removed some tokens
        if last_complete.data.tokens_removed is not None:
            assert last_complete.data.tokens_removed > 0, "Expected tokensRemoved > 0"

        # Verify the session still works after compaction
        answer = await session.send_and_wait({"prompt": "What was the story about?"})
        assert answer is not None
        assert answer.data.content is not None
        # Should remember it was about a dragon (context preserved via summary)
        assert "dragon" in answer.data.content.lower()

    async def test_should_not_emit_compaction_events_when_infinite_sessions_disabled(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session({"infinite_sessions": {"enabled": False}})

        compaction_events = []

        def on_event(event):
            if event.type in (
                SessionEventType.SESSION_COMPACTION_START,
                SessionEventType.SESSION_COMPACTION_COMPLETE,
            ):
                compaction_events.append(event)

        session.on(on_event)

        await session.send_and_wait({"prompt": "What is 2+2?"})

        # Should not have any compaction events when disabled
        assert len(compaction_events) == 0, "Expected no compaction events when disabled"
