"""
Test helper functions for E2E tests.
"""

import asyncio
import os

from copilot import CopilotSession


async def get_final_assistant_message(session: CopilotSession, timeout: float = 10.0):
    """
    Wait for and return the final assistant message from a session turn.

    Args:
        session: The session to wait on
        timeout: Maximum time to wait in seconds

    Returns:
        The final assistant message event

    Raises:
        TimeoutError: If no message arrives within timeout
        RuntimeError: If a session error occurs
    """
    result_future: asyncio.Future = asyncio.get_event_loop().create_future()

    final_assistant_message = None

    def on_event(event):
        nonlocal final_assistant_message
        if result_future.done():
            return

        if event.type.value == "assistant.message":
            final_assistant_message = event
        elif event.type.value == "session.idle":
            if final_assistant_message is not None:
                result_future.set_result(final_assistant_message)
        elif event.type.value == "session.error":
            msg = event.data.message if event.data.message else "session error"
            result_future.set_exception(RuntimeError(msg))

    # Subscribe to future events
    unsubscribe = session.on(on_event)

    try:
        # Also check existing messages in case the response already arrived
        existing = await _get_existing_final_response(session)
        if existing is not None:
            return existing

        return await asyncio.wait_for(result_future, timeout=timeout)
    finally:
        unsubscribe()


async def _get_existing_final_response(session: CopilotSession):
    """Check existing messages for a final response."""
    messages = await session.get_messages()

    # Find last user message
    final_user_message_index = -1
    for i in range(len(messages) - 1, -1, -1):
        if messages[i].type.value == "user.message":
            final_user_message_index = i
            break

    if final_user_message_index < 0:
        current_turn_messages = messages
    else:
        current_turn_messages = messages[final_user_message_index:]

    # Check for errors
    for msg in current_turn_messages:
        if msg.type.value == "session.error":
            err_msg = msg.data.message if msg.data.message else "session error"
            raise RuntimeError(err_msg)

    # Find session.idle and get last assistant message before it
    session_idle_index = -1
    for i, msg in enumerate(current_turn_messages):
        if msg.type.value == "session.idle":
            session_idle_index = i
            break

    if session_idle_index != -1:
        # Find last assistant.message before session.idle
        for i in range(session_idle_index - 1, -1, -1):
            if current_turn_messages[i].type.value == "assistant.message":
                return current_turn_messages[i]

    return None


def write_file(work_dir: str, filename: str, content: str) -> str:
    """
    Write content to a file in the work directory.

    Args:
        work_dir: The working directory
        filename: The name of the file
        content: The content to write

    Returns:
        The full path to the created file
    """
    filepath = os.path.join(work_dir, filename)
    with open(filepath, "w") as f:
        f.write(content)
    return filepath


def read_file(work_dir: str, filename: str) -> str:
    """
    Read content from a file in the work directory.

    Args:
        work_dir: The working directory
        filename: The name of the file

    Returns:
        The content of the file
    """
    filepath = os.path.join(work_dir, filename)
    with open(filepath) as f:
        return f.read()


async def get_next_event_of_type(session: CopilotSession, event_type: str, timeout: float = 30.0):
    """
    Wait for and return the next event of a specific type from a session.

    Args:
        session: The session to wait on
        event_type: The event type to wait for (e.g., "tool.execution_start", "session.idle")
        timeout: Maximum time to wait in seconds

    Returns:
        The matching event

    Raises:
        TimeoutError: If no matching event arrives within timeout
        RuntimeError: If a session error occurs
    """
    result_future: asyncio.Future = asyncio.get_event_loop().create_future()

    def on_event(event):
        if result_future.done():
            return

        if event.type.value == event_type:
            result_future.set_result(event)
        elif event.type.value == "session.error":
            msg = event.data.message if event.data.message else "session error"
            result_future.set_exception(RuntimeError(msg))

    unsubscribe = session.on(on_event)

    try:
        return await asyncio.wait_for(result_future, timeout=timeout)
    finally:
        unsubscribe()
