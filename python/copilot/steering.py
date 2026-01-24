"""
Steering - Streaming conversation mode for queuing messages with priority support.

This module provides the infrastructure for queuing multiple user messages
while the CLI processes previous ones, with support for priority-based ordering
and graceful shutdown.

Example:
    >>> from copilot import CopilotClient
    >>> from copilot.steering import ConversationManager, Priority
    >>>
    >>> async with CopilotClient() as client:
    ...     session = await client.create_session()
    ...     manager = ConversationManager(session)
    ...
    ...     # Queue messages - returns immediately even if CLI is busy
    ...     await manager.queue_message("req-1", "What is Python?", Priority.NORMAL)
    ...     await manager.queue_message("req-2", "URGENT: Fix the bug!", Priority.URGENT)
    ...     await manager.queue_message("req-3", "Tell me a joke", Priority.LOW)
    ...
    ...     # Messages processed in order: req-2, req-1, req-3
    ...
    ...     # Graceful shutdown
    ...     await manager.stop()
"""

from __future__ import annotations

import asyncio
from dataclasses import dataclass, field
from datetime import datetime
from enum import IntEnum
from typing import Any, AsyncIterator, Callable, Optional

from .session import CopilotSession
from .types import Attachment


class Priority(IntEnum):
    """Message priority levels for queue ordering.

    Higher values are processed first. Within the same priority,
    messages are processed in FIFO order.
    """

    LOW = 0
    NORMAL = 1
    HIGH = 2
    URGENT = 3


class QueueFullError(Exception):
    """Raised when the message queue is full and cannot accept new messages."""

    pass


class ShutdownSentinel:
    """Marker to signal generator termination.

    Always sorts last in the priority queue to ensure all pending
    messages are processed before shutdown.
    """

    def __lt__(self, other: object) -> bool:
        """Always sort after real messages."""
        return False

    def __le__(self, other: object) -> bool:
        """Always sort after real messages."""
        return isinstance(other, ShutdownSentinel)

    def __gt__(self, other: object) -> bool:
        """Always sort after real messages."""
        return not isinstance(other, ShutdownSentinel)

    def __ge__(self, other: object) -> bool:
        """Always sort after real messages."""
        return True


# Singleton sentinel instance
SHUTDOWN_SENTINEL = ShutdownSentinel()


@dataclass(order=False)
class QueuedMessage:
    """Represents a message queued for processing.

    Attributes:
        request_id: Unique identifier for this message request.
        content: The message content/prompt text.
        priority: Processing priority (higher = processed first).
        session_id: The session this message belongs to.
        queued_at: When the message was queued.
        metadata: Additional metadata for the message.
        sequence_number: For FIFO ordering within same priority.
        attachments: Optional file/directory attachments.
    """

    request_id: str
    content: str
    priority: Priority
    session_id: str
    queued_at: datetime = field(default_factory=datetime.now)
    metadata: dict[str, Any] = field(default_factory=dict)
    sequence_number: int = 0
    attachments: list[Attachment] = field(default_factory=list)

    def __lt__(self, other: object) -> bool:
        """Priority queue ordering: higher priority first, then FIFO."""
        if isinstance(other, ShutdownSentinel):
            return True  # Real messages come before sentinel
        if not isinstance(other, QueuedMessage):
            return NotImplemented
        if self.priority != other.priority:
            return self.priority.value > other.priority.value  # Higher = first
        return self.sequence_number < other.sequence_number  # Earlier = first

    def __le__(self, other: object) -> bool:
        if isinstance(other, ShutdownSentinel):
            return True
        if not isinstance(other, QueuedMessage):
            return NotImplemented
        return self < other or (
            self.priority == other.priority
            and self.sequence_number == other.sequence_number
        )

    def __gt__(self, other: object) -> bool:
        if isinstance(other, ShutdownSentinel):
            return False
        if not isinstance(other, QueuedMessage):
            return NotImplemented
        return not self <= other

    def __ge__(self, other: object) -> bool:
        if isinstance(other, ShutdownSentinel):
            return False
        if not isinstance(other, QueuedMessage):
            return NotImplemented
        return not self < other


class MessageQueue:
    """Priority queue for conversation messages.

    Provides non-blocking put() and blocking get() semantics with
    priority-based ordering. Thread-safe for use with asyncio.

    Attributes:
        max_depth: Maximum number of messages the queue can hold.
    """

    def __init__(self, max_depth: int = 100):
        """Initialize the message queue.

        Args:
            max_depth: Maximum queue size. Defaults to 100.
        """
        self._max_depth = max_depth
        self._queue: asyncio.PriorityQueue[QueuedMessage | ShutdownSentinel] = (
            asyncio.PriorityQueue(maxsize=max_depth)
        )
        self._shutdown_event = asyncio.Event()
        self._sequence_counter = 0

    @property
    def max_depth(self) -> int:
        """Maximum queue capacity."""
        return self._max_depth

    def qsize(self) -> int:
        """Return the current queue size."""
        return self._queue.qsize()

    def empty(self) -> bool:
        """Return True if the queue is empty."""
        return self._queue.empty()

    def full(self) -> bool:
        """Return True if the queue is full."""
        return self._queue.full()

    def _next_sequence(self) -> int:
        """Get the next sequence number for FIFO ordering."""
        seq = self._sequence_counter
        self._sequence_counter += 1
        return seq

    async def put(self, message: QueuedMessage) -> None:
        """Add a message to the queue.

        This is non-blocking - it raises QueueFullError if the queue is full
        rather than waiting.

        Args:
            message: The message to queue.

        Raises:
            QueueFullError: If the queue is at max capacity.
        """
        # Assign sequence number if not set
        if message.sequence_number == 0:
            message.sequence_number = self._next_sequence()

        try:
            self._queue.put_nowait(message)
        except asyncio.QueueFull:
            raise QueueFullError(f"Queue full (max={self._max_depth})")

    async def get(self) -> QueuedMessage | ShutdownSentinel:
        """Get the next message from the queue.

        Blocks until a message is available or shutdown is signaled.

        Returns:
            The next message to process, or ShutdownSentinel if shutting down.
        """
        return await self._queue.get()

    def signal_shutdown(self) -> None:
        """Signal the generator to terminate.

        This unblocks any waiting get() calls and causes the generator
        to terminate gracefully.
        """
        self._shutdown_event.set()
        try:
            self._queue.put_nowait(SHUTDOWN_SENTINEL)
        except asyncio.QueueFull:
            # Queue is full, but we still set the event
            pass

    def is_shutdown(self) -> bool:
        """Check if shutdown has been signaled."""
        return self._shutdown_event.is_set()


class StreamingInputGenerator:
    """Async generator that yields messages from queue to SDK.

    This class bridges the message queue to the SDK's async iterator
    interface, yielding formatted message dicts until shutdown.

    Example:
        >>> queue = MessageQueue()
        >>> generator = StreamingInputGenerator(queue)
        >>> async for message in generator:
        ...     # Process message
        ...     pass
    """

    def __init__(self, queue: MessageQueue):
        """Initialize the generator.

        Args:
            queue: The message queue to consume from.
        """
        self._queue = queue

    def __aiter__(self) -> AsyncIterator[dict[str, Any]]:
        """Return self as the async iterator."""
        return self._generate()

    async def _generate(self) -> AsyncIterator[dict[str, Any]]:
        """Yield messages until shutdown sentinel received."""
        while True:
            item = await self._queue.get()

            if isinstance(item, ShutdownSentinel):
                break

            # Format for SDK consumption
            yield {
                "type": "user",
                "message": {
                    "role": "user",
                    "content": item.content,
                },
                "metadata": {
                    "request_id": item.request_id,
                    "priority": item.priority.name,
                    "session_id": item.session_id,
                    "queued_at": item.queued_at.isoformat(),
                    **item.metadata,
                },
                "attachments": item.attachments,
            }


class ConversationManager:
    """Orchestrates message flow from callers to SDK session.

    The ConversationManager provides a high-level interface for queuing
    messages while the CLI processes previous ones. It handles:

    - Non-blocking message queuing with priority support
    - Automatic session interaction
    - Graceful shutdown

    Example:
        >>> async with CopilotClient() as client:
        ...     session = await client.create_session()
        ...     manager = ConversationManager(session)
        ...
        ...     # Queue messages - returns immediately
        ...     await manager.queue_message("req-1", "Hello", Priority.NORMAL)
        ...     await manager.queue_message("req-2", "Urgent!", Priority.URGENT)
        ...
        ...     # Stop when done
        ...     await manager.stop()
    """

    def __init__(
        self,
        session: CopilotSession,
        max_queue_depth: int = 100,
        on_response: Optional[Callable[[dict[str, Any]], None]] = None,
    ):
        """Initialize the conversation manager.

        Args:
            session: The CopilotSession to send messages to.
            max_queue_depth: Maximum number of queued messages. Defaults to 100.
            on_response: Optional callback for responses/events from the session.
        """
        self._session = session
        self._max_queue_depth = max_queue_depth
        self._on_response = on_response
        self._queue: Optional[MessageQueue] = None
        self._generator: Optional[StreamingInputGenerator] = None
        self._processor_task: Optional[asyncio.Task[None]] = None
        self._started = False
        self._request_counter = 0

    @property
    def session(self) -> CopilotSession:
        """The underlying session."""
        return self._session

    @property
    def is_started(self) -> bool:
        """Whether the manager has been started."""
        return self._started

    @property
    def queue_size(self) -> int:
        """Current number of queued messages."""
        return self._queue.qsize() if self._queue else 0

    def _generate_request_id(self) -> str:
        """Generate a unique request ID."""
        self._request_counter += 1
        return f"req-{self._request_counter}"

    async def queue_message(
        self,
        content: str,
        priority: Priority = Priority.NORMAL,
        request_id: Optional[str] = None,
        attachments: Optional[list[Attachment]] = None,
        metadata: Optional[dict[str, Any]] = None,
    ) -> str:
        """Queue a message for processing.

        This method returns immediately, even if the CLI is busy processing
        a previous message. Messages are processed in priority order.

        Args:
            content: The message content/prompt.
            priority: Processing priority. Defaults to NORMAL.
            request_id: Optional custom request ID. Auto-generated if not provided.
            attachments: Optional file/directory attachments.
            metadata: Optional additional metadata.

        Returns:
            The request ID for this message.

        Raises:
            QueueFullError: If the queue is at max capacity.
        """
        # Auto-start on first message
        if not self._started:
            await self._start()

        if request_id is None:
            request_id = self._generate_request_id()

        msg = QueuedMessage(
            request_id=request_id,
            content=content,
            priority=priority,
            session_id=self._session.session_id,
            attachments=attachments or [],
            metadata=metadata or {},
        )
        await self._queue.put(msg)  # type: ignore[union-attr]
        return request_id

    async def _start(self) -> None:
        """Start the conversation processing loop."""
        if self._started:
            return

        self._queue = MessageQueue(max_depth=self._max_queue_depth)
        self._generator = StreamingInputGenerator(self._queue)
        self._started = True

        # Start the processor task that consumes from the generator
        self._processor_task = asyncio.create_task(self._process_messages())

    async def _process_messages(self) -> None:
        """Process messages from the generator, sending them to the session."""
        if self._generator is None:
            return

        async for message_dict in self._generator:
            try:
                # Extract message details
                content = message_dict["message"]["content"]
                attachments = message_dict.get("attachments", [])
                request_id = message_dict["metadata"]["request_id"]

                # Send to session - this is non-blocking in the SDK
                # The session.send returns a message ID
                await self._session.send(
                    {
                        "prompt": content,
                        "attachments": attachments if attachments else None,
                    }
                )

                # If we have a response callback, we could wire it up here
                # For now, the caller should use session.on() for events

            except Exception as e:
                # Log error but continue processing
                print(f"Error processing message {message_dict.get('metadata', {}).get('request_id', 'unknown')}: {e}")

    async def stop(self, timeout: Optional[float] = None) -> None:
        """Gracefully stop the conversation manager.

        Signals shutdown and waits for pending messages to be processed.

        Args:
            timeout: Maximum time to wait for pending messages. Defaults to None (wait forever).
        """
        if not self._started or self._queue is None:
            return

        # Signal shutdown
        self._queue.signal_shutdown()

        # Wait for processor task to complete
        if self._processor_task is not None:
            try:
                if timeout is not None:
                    await asyncio.wait_for(self._processor_task, timeout=timeout)
                else:
                    await self._processor_task
            except asyncio.TimeoutError:
                self._processor_task.cancel()
                try:
                    await self._processor_task
                except asyncio.CancelledError:
                    pass
            except asyncio.CancelledError:
                pass

        self._started = False
        self._queue = None
        self._generator = None
        self._processor_task = None

    async def clear_queue(self) -> int:
        """Clear all pending messages from the queue.

        Returns:
            The number of messages that were cleared.
        """
        if self._queue is None:
            return 0

        count = 0
        while not self._queue.empty():
            try:
                # We need to drain the queue
                # Create a new empty queue and swap
                item = self._queue._queue.get_nowait()
                if not isinstance(item, ShutdownSentinel):
                    count += 1
            except asyncio.QueueEmpty:
                break

        return count

    async def __aenter__(self) -> "ConversationManager":
        """Async context manager entry."""
        return self

    async def __aexit__(
        self,
        exc_type: Optional[type],
        exc_val: Optional[BaseException],
        exc_tb: Optional[Any],
    ) -> None:
        """Async context manager exit - ensures clean shutdown."""
        await self.stop()
