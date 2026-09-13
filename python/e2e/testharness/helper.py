"""
Test helper functions for E2E tests.
"""

import asyncio
import inspect
import os
import time
from collections.abc import Awaitable, Callable

from _session_test_helpers import get_next_event_of_type as get_next_event_of_type
from _session_test_helpers import wait_for_event as wait_for_event


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


async def wait_for_condition(
    condition: Callable[[], bool | Awaitable[bool]],
    *,
    timeout: float = 120.0,
    poll_interval: float = 0.1,
    timeout_message: str = "Timed out waiting for condition.",
) -> None:
    """Poll until condition returns true, with timeout only as a failsafe."""
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        result = condition()
        if inspect.isawaitable(result):
            result = await result
        if result:
            return
        await asyncio.sleep(poll_interval)

    result = condition()
    if inspect.isawaitable(result):
        result = await result
    if result:
        return
    raise TimeoutError(timeout_message)
