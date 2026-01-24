"""
Unit tests for the steering module.
"""

import asyncio
from datetime import datetime
from unittest.mock import AsyncMock, MagicMock

import pytest

from copilot.steering import (
    ConversationManager,
    MessageQueue,
    Priority,
    QueuedMessage,
    QueueFullError,
    ShutdownSentinel,
    StreamingInputGenerator,
    SHUTDOWN_SENTINEL,
)


class TestPriority:
    def test_priority_ordering(self):
        """Test that priority values are ordered correctly."""
        assert Priority.LOW.value == 0
        assert Priority.NORMAL.value == 1
        assert Priority.HIGH.value == 2
        assert Priority.URGENT.value == 3

        assert Priority.URGENT > Priority.HIGH > Priority.NORMAL > Priority.LOW


class TestQueuedMessage:
    def test_creation(self):
        """Test basic message creation."""
        msg = QueuedMessage(
            request_id="test-1",
            content="Hello",
            priority=Priority.NORMAL,
            session_id="session-1",
        )
        assert msg.request_id == "test-1"
        assert msg.content == "Hello"
        assert msg.priority == Priority.NORMAL
        assert msg.session_id == "session-1"
        assert isinstance(msg.queued_at, datetime)

    def test_priority_comparison(self):
        """Test that higher priority messages sort first."""
        low = QueuedMessage(
            request_id="low",
            content="Low",
            priority=Priority.LOW,
            session_id="s",
            sequence_number=1,
        )
        normal = QueuedMessage(
            request_id="normal",
            content="Normal",
            priority=Priority.NORMAL,
            session_id="s",
            sequence_number=2,
        )
        urgent = QueuedMessage(
            request_id="urgent",
            content="Urgent",
            priority=Priority.URGENT,
            session_id="s",
            sequence_number=3,
        )

        # Higher priority should sort first (be "less than")
        assert urgent < normal < low
        assert not low < urgent

    def test_fifo_within_same_priority(self):
        """Test FIFO ordering for messages with same priority."""
        first = QueuedMessage(
            request_id="first",
            content="First",
            priority=Priority.NORMAL,
            session_id="s",
            sequence_number=1,
        )
        second = QueuedMessage(
            request_id="second",
            content="Second",
            priority=Priority.NORMAL,
            session_id="s",
            sequence_number=2,
        )

        # Earlier sequence should sort first
        assert first < second
        assert not second < first

    def test_comparison_with_sentinel(self):
        """Test that messages sort before shutdown sentinel."""
        msg = QueuedMessage(
            request_id="msg",
            content="Test",
            priority=Priority.URGENT,
            session_id="s",
        )

        assert msg < SHUTDOWN_SENTINEL
        assert not SHUTDOWN_SENTINEL < msg


class TestShutdownSentinel:
    def test_singleton(self):
        """Test that SHUTDOWN_SENTINEL is used consistently."""
        assert isinstance(SHUTDOWN_SENTINEL, ShutdownSentinel)

    def test_comparison_with_messages(self):
        """Test sentinel always sorts last."""
        msg = QueuedMessage(
            request_id="msg",
            content="Test",
            priority=Priority.LOW,
            session_id="s",
        )

        assert msg < SHUTDOWN_SENTINEL
        assert SHUTDOWN_SENTINEL > msg
        assert not SHUTDOWN_SENTINEL < msg

    def test_comparison_with_itself(self):
        """Test sentinel comparison with itself."""
        sentinel1 = ShutdownSentinel()
        sentinel2 = ShutdownSentinel()

        assert not sentinel1 < sentinel2
        assert sentinel1 <= sentinel2
        assert sentinel1 >= sentinel2


class TestMessageQueue:
    @pytest.mark.asyncio
    async def test_put_and_get(self):
        """Test basic put and get operations."""
        queue = MessageQueue(max_depth=10)

        msg = QueuedMessage(
            request_id="test",
            content="Hello",
            priority=Priority.NORMAL,
            session_id="session-1",
        )

        await queue.put(msg)
        assert queue.qsize() == 1

        result = await queue.get()
        assert result == msg
        assert queue.qsize() == 0

    @pytest.mark.asyncio
    async def test_priority_ordering(self):
        """Test that messages are returned in priority order."""
        queue = MessageQueue(max_depth=10)

        low = QueuedMessage(
            request_id="low",
            content="Low",
            priority=Priority.LOW,
            session_id="s",
        )
        urgent = QueuedMessage(
            request_id="urgent",
            content="Urgent",
            priority=Priority.URGENT,
            session_id="s",
        )
        normal = QueuedMessage(
            request_id="normal",
            content="Normal",
            priority=Priority.NORMAL,
            session_id="s",
        )

        # Add in arbitrary order
        await queue.put(low)
        await queue.put(urgent)
        await queue.put(normal)

        # Should come out in priority order
        assert (await queue.get()).request_id == "urgent"
        assert (await queue.get()).request_id == "normal"
        assert (await queue.get()).request_id == "low"

    @pytest.mark.asyncio
    async def test_queue_full_error(self):
        """Test that QueueFullError is raised when queue is full."""
        queue = MessageQueue(max_depth=2)

        msg1 = QueuedMessage(
            request_id="1", content="1", priority=Priority.NORMAL, session_id="s"
        )
        msg2 = QueuedMessage(
            request_id="2", content="2", priority=Priority.NORMAL, session_id="s"
        )
        msg3 = QueuedMessage(
            request_id="3", content="3", priority=Priority.NORMAL, session_id="s"
        )

        await queue.put(msg1)
        await queue.put(msg2)

        with pytest.raises(QueueFullError):
            await queue.put(msg3)

    @pytest.mark.asyncio
    async def test_shutdown_signal(self):
        """Test shutdown signaling."""
        queue = MessageQueue()

        assert not queue.is_shutdown()
        queue.signal_shutdown()
        assert queue.is_shutdown()

        # Should be able to get the sentinel
        result = await queue.get()
        assert isinstance(result, ShutdownSentinel)

    @pytest.mark.asyncio
    async def test_empty_and_full(self):
        """Test empty and full properties."""
        queue = MessageQueue(max_depth=2)

        assert queue.empty()
        assert not queue.full()

        msg = QueuedMessage(
            request_id="1", content="1", priority=Priority.NORMAL, session_id="s"
        )
        await queue.put(msg)

        assert not queue.empty()
        assert not queue.full()

        msg2 = QueuedMessage(
            request_id="2", content="2", priority=Priority.NORMAL, session_id="s"
        )
        await queue.put(msg2)

        assert queue.full()


class TestStreamingInputGenerator:
    @pytest.mark.asyncio
    async def test_yields_messages(self):
        """Test that generator yields messages in correct format."""
        queue = MessageQueue()
        generator = StreamingInputGenerator(queue)

        msg = QueuedMessage(
            request_id="test-1",
            content="Hello world",
            priority=Priority.NORMAL,
            session_id="session-123",
            metadata={"custom": "value"},
        )
        await queue.put(msg)
        queue.signal_shutdown()

        messages = []
        async for message in generator:
            messages.append(message)

        assert len(messages) == 1
        assert messages[0]["type"] == "user"
        assert messages[0]["message"]["role"] == "user"
        assert messages[0]["message"]["content"] == "Hello world"
        assert messages[0]["metadata"]["request_id"] == "test-1"
        assert messages[0]["metadata"]["priority"] == "NORMAL"
        assert messages[0]["metadata"]["session_id"] == "session-123"
        assert messages[0]["metadata"]["custom"] == "value"

    @pytest.mark.asyncio
    async def test_stops_on_shutdown(self):
        """Test that generator stops when shutdown sentinel is received."""
        queue = MessageQueue()
        generator = StreamingInputGenerator(queue)

        msg1 = QueuedMessage(
            request_id="1", content="First", priority=Priority.NORMAL, session_id="s"
        )
        msg2 = QueuedMessage(
            request_id="2", content="Second", priority=Priority.NORMAL, session_id="s"
        )
        await queue.put(msg1)
        await queue.put(msg2)
        queue.signal_shutdown()

        messages = []
        async for message in generator:
            messages.append(message)

        assert len(messages) == 2

    @pytest.mark.asyncio
    async def test_priority_order_preserved(self):
        """Test that messages come out in priority order."""
        queue = MessageQueue()
        generator = StreamingInputGenerator(queue)

        low = QueuedMessage(
            request_id="low", content="Low", priority=Priority.LOW, session_id="s"
        )
        urgent = QueuedMessage(
            request_id="urgent",
            content="Urgent",
            priority=Priority.URGENT,
            session_id="s",
        )

        await queue.put(low)
        await queue.put(urgent)
        queue.signal_shutdown()

        messages = []
        async for message in generator:
            messages.append(message)

        assert messages[0]["metadata"]["request_id"] == "urgent"
        assert messages[1]["metadata"]["request_id"] == "low"


class TestConversationManager:
    @pytest.mark.asyncio
    async def test_queue_message(self):
        """Test queuing messages."""
        mock_session = MagicMock()
        mock_session.session_id = "test-session"
        mock_session.send = AsyncMock(return_value="msg-id")

        manager = ConversationManager(mock_session)

        request_id = await manager.queue_message("Hello", Priority.NORMAL)
        assert request_id == "req-1"
        assert manager.is_started
        assert manager.queue_size >= 0  # May have been processed already

        await manager.stop()

    @pytest.mark.asyncio
    async def test_auto_start(self):
        """Test that manager auto-starts on first message."""
        mock_session = MagicMock()
        mock_session.session_id = "test-session"
        mock_session.send = AsyncMock(return_value="msg-id")

        manager = ConversationManager(mock_session)
        assert not manager.is_started

        await manager.queue_message("Hello", Priority.NORMAL)
        assert manager.is_started

        await manager.stop()

    @pytest.mark.asyncio
    async def test_custom_request_id(self):
        """Test using custom request ID."""
        mock_session = MagicMock()
        mock_session.session_id = "test-session"
        mock_session.send = AsyncMock(return_value="msg-id")

        manager = ConversationManager(mock_session)

        request_id = await manager.queue_message(
            "Hello", Priority.NORMAL, request_id="custom-123"
        )
        assert request_id == "custom-123"

        await manager.stop()

    @pytest.mark.asyncio
    async def test_stop_without_start(self):
        """Test that stop() works even if never started."""
        mock_session = MagicMock()
        mock_session.session_id = "test-session"

        manager = ConversationManager(mock_session)
        await manager.stop()  # Should not raise

    @pytest.mark.asyncio
    async def test_context_manager(self):
        """Test async context manager usage."""
        mock_session = MagicMock()
        mock_session.session_id = "test-session"
        mock_session.send = AsyncMock(return_value="msg-id")

        async with ConversationManager(mock_session) as manager:
            await manager.queue_message("Hello", Priority.NORMAL)
            assert manager.is_started

        # Should be stopped after exiting context
        assert not manager.is_started

    @pytest.mark.asyncio
    async def test_message_sends_to_session(self):
        """Test that queued messages are sent to session."""
        mock_session = MagicMock()
        mock_session.session_id = "test-session"
        mock_session.send = AsyncMock(return_value="msg-id")

        manager = ConversationManager(mock_session)

        await manager.queue_message("Hello world", Priority.NORMAL)

        # Give the processor task time to run
        await asyncio.sleep(0.1)

        # Verify send was called
        mock_session.send.assert_called()
        call_args = mock_session.send.call_args[0][0]
        assert call_args["prompt"] == "Hello world"

        await manager.stop()

    @pytest.mark.asyncio
    async def test_priority_processing_order(self):
        """Test that messages are processed in priority order."""
        mock_session = MagicMock()
        mock_session.session_id = "test-session"

        processed_prompts = []

        async def capture_send(options):
            processed_prompts.append(options["prompt"])
            return "msg-id"

        mock_session.send = capture_send

        manager = ConversationManager(mock_session)

        # Queue messages in arbitrary order
        await manager.queue_message("Low priority", Priority.LOW)
        await manager.queue_message("Urgent!", Priority.URGENT)
        await manager.queue_message("Normal", Priority.NORMAL)

        # Give processor time to work
        await asyncio.sleep(0.2)

        await manager.stop()

        # Should be processed in priority order
        assert processed_prompts == ["Urgent!", "Normal", "Low priority"]

    @pytest.mark.asyncio
    async def test_queue_full_error(self):
        """Test that QueueFullError is raised when queue is full."""
        mock_session = MagicMock()
        mock_session.session_id = "test-session"

        # Use a blocking send to keep the queue from draining
        send_started = asyncio.Event()
        send_continue = asyncio.Event()

        async def blocking_send(options):
            send_started.set()
            await send_continue.wait()
            return "msg-id"

        mock_session.send = blocking_send

        manager = ConversationManager(mock_session, max_queue_depth=2)

        # Queue first message - this will be picked up by processor immediately
        await manager.queue_message("1", Priority.NORMAL)
        
        # Wait for send to start (message 1 is now being processed)
        await send_started.wait()
        
        # Now queue 2 more - these should fill the queue
        await manager.queue_message("2", Priority.NORMAL)
        await manager.queue_message("3", Priority.NORMAL)

        # This should fail since queue is full and send is blocked
        with pytest.raises(QueueFullError):
            await manager.queue_message("4", Priority.NORMAL)

        # Unblock and cleanup
        send_continue.set()
        await manager.stop(timeout=1.0)

    @pytest.mark.asyncio
    async def test_attachments_forwarded(self):
        """Test that attachments are forwarded to session."""
        mock_session = MagicMock()
        mock_session.session_id = "test-session"
        mock_session.send = AsyncMock(return_value="msg-id")

        manager = ConversationManager(mock_session)

        attachments = [{"type": "file", "path": "/test/file.py"}]
        await manager.queue_message(
            "Check this file", Priority.NORMAL, attachments=attachments
        )

        await asyncio.sleep(0.1)

        mock_session.send.assert_called()
        call_args = mock_session.send.call_args[0][0]
        assert call_args["attachments"] == attachments

        await manager.stop()


class TestYieldWhileProcessing:
    """Tests to verify the SDK can accept yields during processing (spec requirement)."""

    @pytest.mark.asyncio
    async def test_yield_while_processing(self):
        """Verify that new messages can be queued while previous ones are processing."""
        mock_session = MagicMock()
        mock_session.session_id = "test-session"

        first_processing_started = asyncio.Event()
        first_processing_done = asyncio.Event()
        messages_yielded = []
        call_count = 0

        async def slow_send(options):
            nonlocal call_count
            call_count += 1
            messages_yielded.append(options["prompt"])
            
            if call_count == 1:
                first_processing_started.set()
                # Wait for the test to queue more messages
                await first_processing_done.wait()
            return "msg-id"

        mock_session.send = slow_send

        manager = ConversationManager(mock_session)

        # Queue first message
        await manager.queue_message("message 1", Priority.NORMAL)

        # Wait for processing to start
        await first_processing_started.wait()

        # These yields must succeed while msg1 is still processing
        await manager.queue_message("message 2", Priority.NORMAL)
        await manager.queue_message("message 3", Priority.NORMAL)

        # Verify queue accepted the messages (non-blocking put)
        assert manager.queue_size >= 2

        # Let first processing complete
        first_processing_done.set()
        
        # Give time for all messages to process
        await asyncio.sleep(0.3)

        await manager.stop()

        # All messages should have been processed
        assert messages_yielded == ["message 1", "message 2", "message 3"]
