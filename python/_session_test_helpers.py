"""Live-event test helpers without E2E runtime or proxy initialization."""

import asyncio
from collections.abc import Callable

from copilot.session import CopilotSession
from copilot.session_events import SessionErrorData, SessionEvent


def wait_for_event(
    session: CopilotSession,
    predicate: Callable[[SessionEvent], bool],
    timeout: float = 30.0,
    *,
    fail_on_session_error: bool = False,
) -> asyncio.Task[SessionEvent]:
    """Subscribe synchronously and return a task for the next matching live event.

    Call before the operation that emits the event, then await or cancel the task.
    Merely scheduling an async subscriber would leave a race before it starts.
    In particular, session.idle is ephemeral and cannot be recovered from history.

    Only predicate matches complete the wait by default. Set fail_on_session_error
    to also fail on unmatched session errors when the caller requires that policy.
    """
    loop = asyncio.get_running_loop()
    result_future: asyncio.Future[SessionEvent] = loop.create_future()

    def on_event(event: SessionEvent) -> None:
        if result_future.done():
            return

        if predicate(event):
            result_future.set_result(event)
        elif fail_on_session_error and isinstance(event.data, SessionErrorData):
            result_future.set_exception(RuntimeError(event.data.message or "session error"))

    unsubscribe = session.on(on_event)

    async def wait() -> SessionEvent:
        return await asyncio.wait_for(result_future, timeout=timeout)

    def cleanup(_task: asyncio.Task[SessionEvent]) -> None:
        unsubscribe()
        result_future.cancel()
        if not result_future.cancelled():
            result_future.exception()

    task = loop.create_task(wait())
    # A task cancelled before its first step never executes a coroutine's finally.
    task.add_done_callback(cleanup)
    return task


def get_next_event_of_type(
    session: CopilotSession, event_type: str, timeout: float = 30.0
) -> asyncio.Task[SessionEvent]:
    """Subscribe before an operation; fail on session errors unless waiting for that event."""
    return wait_for_event(
        session,
        lambda event: event.type.value == event_type,
        timeout,
        fail_on_session_error=True,
    )
