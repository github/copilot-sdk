"""
Cloud session — remote-control client for a sandbox-backed cloud session.

The :class:`CloudSession` class polls Mission Control for task events and
exposes methods for sending user messages and steering commands.
"""

from __future__ import annotations

import asyncio
import json
import logging
from collections.abc import Callable
from typing import Any, overload

from .mission_control_client import MissionControlClient
from .types import (
    CloudAskUserResponsePayload,
    CloudElicitationResponsePayload,
    CloudModeSwitchPayload,
    CloudPermissionResponsePayload,
    CloudPlanApprovalResponsePayload,
    CloudSessionEvent,
    CloudSessionEventHandler,
    CloudSessionMetadata,
    ElicitationResult,
    ExitPlanModeResult,
    MissionControlCommandType,
)

logger = logging.getLogger(__name__)

_DEFAULT_POLL_INTERVAL_S = 5.0
_DEFAULT_INITIAL_EVENT_TIMEOUT_S = 10.0
_DEFAULT_INITIAL_EVENT_POLL_INTERVAL_S = 0.5


class CloudSession:
    """Remote-control client for a cloud sandbox session.

    After construction, call :meth:`connect` to start receiving events.

    Args:
        client: The Mission Control HTTP client.
        metadata: Metadata describing the cloud session/task.
        poll_interval_ms: Milliseconds between event polls (default: 5000).
        initial_event_timeout_ms: Milliseconds to wait for the first event (default: 10000).
        initial_event_poll_interval_ms: Milliseconds between polls while waiting for the
            first event (default: 500).
        on_event_poll_error: Callback invoked when an event poll fails.
    """

    def __init__(
        self,
        *,
        client: MissionControlClient,
        metadata: CloudSessionMetadata,
        poll_interval_ms: int | None = None,
        initial_event_timeout_ms: int | None = None,
        initial_event_poll_interval_ms: int | None = None,
        on_event_poll_error: Callable[[Exception], None] | None = None,
    ) -> None:
        self._client = client
        self.metadata = metadata
        self.session_id = metadata.mission_control_session_id or metadata.task_id

        self._poll_interval_s = (
            poll_interval_ms / 1000 if poll_interval_ms is not None else _DEFAULT_POLL_INTERVAL_S
        )
        self._initial_event_timeout_s = (
            initial_event_timeout_ms / 1000
            if initial_event_timeout_ms is not None
            else _DEFAULT_INITIAL_EVENT_TIMEOUT_S
        )
        self._initial_event_poll_interval_s = (
            initial_event_poll_interval_ms / 1000
            if initial_event_poll_interval_ms is not None
            else _DEFAULT_INITIAL_EVENT_POLL_INTERVAL_S
        )
        self._on_event_poll_error = on_event_poll_error

        self._event_handlers: set[CloudSessionEventHandler] = set()
        self._typed_event_handlers: dict[str, set[CloudSessionEventHandler]] = {}
        self._events: list[CloudSessionEvent] = []
        self._seen_event_ids: set[str] = set()
        self._seen_event_ids_at_last_timestamp: set[str] = set()
        self._last_seen_timestamp: str | None = None
        self._poller_task: asyncio.Task[None] | None = None
        self._is_polling = False
        self._is_disconnected = False
        self._remote_steerable = True

    # ------------------------------------------------------------------
    # Connection lifecycle
    # ------------------------------------------------------------------

    async def connect(self) -> None:
        """Connect to the cloud session and start event polling."""
        initial_events = await self._wait_for_initial_events()
        self._record_events(initial_events)
        self._start_event_polling()

    # ------------------------------------------------------------------
    # Event subscription
    # ------------------------------------------------------------------

    @overload
    def on(self, handler: CloudSessionEventHandler, /) -> Callable[[], None]: ...

    @overload
    def on(self, event_type: str, handler: CloudSessionEventHandler, /) -> Callable[[], None]: ...

    def on(
        self,
        event_type_or_handler: str | CloudSessionEventHandler,
        handler: CloudSessionEventHandler | None = None,
        /,
    ) -> Callable[[], None]:
        """Register an event handler.

        Can be called with a wildcard handler::

            session.on(lambda event: print(event.type))

        Or with a specific event type::

            session.on("session.idle", lambda event: print("idle"))

        Returns a callable that removes the handler when invoked.
        """
        if isinstance(event_type_or_handler, str) and handler is not None:
            event_type = event_type_or_handler
            if event_type not in self._typed_event_handlers:
                self._typed_event_handlers[event_type] = set()
            self._typed_event_handlers[event_type].add(handler)

            def _unsubscribe() -> None:
                handlers = self._typed_event_handlers.get(event_type)
                if handlers:
                    handlers.discard(handler)

            return _unsubscribe

        wildcard_handler = event_type_or_handler
        assert callable(wildcard_handler)
        self._event_handlers.add(wildcard_handler)

        def _unsubscribe() -> None:
            self._event_handlers.discard(wildcard_handler)

        return _unsubscribe

    # ------------------------------------------------------------------
    # Sending messages & steering
    # ------------------------------------------------------------------

    async def send(self, *, prompt: str) -> None:
        """Send a user message to the cloud session.

        Args:
            prompt: The message text to send.
        """
        self._assert_connected()
        await self.submit_remote_command(MissionControlCommandType.USER_MESSAGE, prompt)

    async def send_and_wait(
        self,
        *,
        prompt: str,
        timeout: float | None = None,
    ) -> CloudSessionEvent | None:
        """Send a message and wait for the session to reach idle.

        Returns the last ``assistant.message`` event received before idle,
        or ``None`` if the session became idle without an assistant message.

        Args:
            prompt: The message text to send.
            timeout: Maximum seconds to wait (default: 60).
        """
        effective_timeout = timeout if timeout is not None else 60.0
        last_assistant_message: CloudSessionEvent | None = None
        done = asyncio.Event()
        error_holder: list[Exception] = []

        def _handler(event: CloudSessionEvent) -> None:
            nonlocal last_assistant_message
            if event.type == "assistant.message":
                last_assistant_message = event
            elif event.type == "session.idle":
                done.set()
            elif event.type == "session.error":
                msg = event.data.get("message", "Unknown error") if event.data else "Unknown error"
                error_holder.append(Exception(msg))
                done.set()

        unsubscribe = self.on(_handler)
        try:
            await self.send(prompt=prompt)
            await asyncio.wait_for(done.wait(), timeout=effective_timeout)
            if error_holder:
                raise error_holder[0]
            return last_assistant_message
        except TimeoutError as exc:
            raise TimeoutError(
                f"Timeout after {effective_timeout}s waiting for session.idle"
            ) from exc
        finally:
            unsubscribe()

    async def abort(self) -> None:
        """Abort the current cloud session operation."""
        self._assert_connected()
        await self.submit_remote_command(MissionControlCommandType.ABORT)

    async def submit_remote_command(
        self,
        command_type: MissionControlCommandType,
        content: str | None = None,
    ) -> None:
        """Send a raw steering command to Mission Control.

        Args:
            command_type: The type of steering command.
            content: Optional payload content.
        """
        self._assert_connected()
        if not self._remote_steerable:
            raise RuntimeError("This session is read-only — remote steering is not enabled")
        request: dict[str, Any] = {"type": command_type.value}
        if content is not None:
            request["content"] = content
        await self._client.steer_task(self.metadata.task_id, request)

    # ------------------------------------------------------------------
    # Response helpers
    # ------------------------------------------------------------------

    async def respond_to_permission(self, payload: CloudPermissionResponsePayload) -> None:
        """Respond to a permission request."""
        wire = _to_camel_case_dict(payload)
        await self.submit_remote_command(
            MissionControlCommandType.PERMISSION_RESPONSE, json.dumps(wire)
        )

    async def respond_to_ask_user(self, payload: CloudAskUserResponsePayload) -> None:
        """Respond to an ask-user prompt."""
        wire = _to_camel_case_dict(payload)
        await self.submit_remote_command(
            MissionControlCommandType.ASK_USER_RESPONSE, json.dumps(wire)
        )

    async def respond_to_elicitation(self, payload: CloudElicitationResponsePayload) -> None:
        """Respond to an elicitation prompt."""
        wire = _to_camel_case_dict(payload)
        await self.submit_remote_command(
            MissionControlCommandType.ELICITATION_RESPONSE, json.dumps(wire)
        )

    async def respond_to_exit_plan_mode(self, payload: CloudPlanApprovalResponsePayload) -> None:
        """Respond to a plan approval prompt."""
        wire = _to_camel_case_dict(payload)
        await self.submit_remote_command(
            MissionControlCommandType.PLAN_APPROVAL_RESPONSE, json.dumps(wire)
        )

    async def switch_mode(self, payload: CloudModeSwitchPayload) -> None:
        """Switch the cloud session mode."""
        await self.submit_remote_command(
            MissionControlCommandType.MODE_SWITCH, json.dumps(dict(payload))
        )

    async def respond_to_elicitation_result(
        self, prompt_id: str, result: ElicitationResult
    ) -> None:
        """Convenience: respond to an elicitation with a prompt ID and result."""
        payload = CloudElicitationResponsePayload(
            prompt_id=prompt_id,
            **result,  # type: ignore[typeddict-item]
        )
        await self.respond_to_elicitation(payload)

    async def respond_to_plan_approval(self, prompt_id: str, result: ExitPlanModeResult) -> None:
        """Convenience: respond to a plan approval with a prompt ID and result."""
        payload = CloudPlanApprovalResponsePayload(
            prompt_id=prompt_id,
            **result,  # type: ignore[typeddict-item]
        )
        await self.respond_to_exit_plan_mode(payload)

    # ------------------------------------------------------------------
    # Event access
    # ------------------------------------------------------------------

    def get_messages(self) -> list[CloudSessionEvent]:
        """Return a copy of all events received so far."""
        return list(self._events)

    # ------------------------------------------------------------------
    # Disconnect
    # ------------------------------------------------------------------

    async def disconnect(self) -> None:
        """Disconnect from the cloud session and stop event polling."""
        self._stop_event_polling()
        self._event_handlers.clear()
        self._typed_event_handlers.clear()
        self._is_disconnected = True

    async def destroy(self) -> None:
        """Alias for :meth:`disconnect`."""
        await self.disconnect()

    async def __aenter__(self) -> CloudSession:
        return self

    async def __aexit__(
        self,
        exc_type: type[BaseException] | None,
        exc_val: BaseException | None,
        exc_tb: Any,
    ) -> None:
        await self.disconnect()

    # ------------------------------------------------------------------
    # Event polling internals
    # ------------------------------------------------------------------

    def _start_event_polling(self) -> None:
        if self._poller_task is not None or self._is_disconnected:
            return

        self._poller_task = asyncio.ensure_future(self._poll_loop())

    def _stop_event_polling(self) -> None:
        if self._poller_task is not None:
            self._poller_task.cancel()
            self._poller_task = None

    async def _poll_loop(self) -> None:
        try:
            while not self._is_disconnected:
                await asyncio.sleep(self._poll_interval_s)
                try:
                    await self._poll_events()
                except Exception as exc:
                    self._report_poll_error(exc)
        except asyncio.CancelledError:
            pass

    async def _wait_for_initial_events(self) -> list[CloudSessionEvent]:
        deadline = asyncio.get_event_loop().time() + self._initial_event_timeout_s
        while True:
            events = await self._client.list_task_events(self.metadata.task_id)
            if events:
                return _sort_events_chronologically(events)
            if self._initial_event_timeout_s <= 0 or asyncio.get_event_loop().time() >= deadline:
                return []
            await asyncio.sleep(self._initial_event_poll_interval_s)

    async def _poll_events(self) -> None:
        if self._is_polling or self._is_disconnected:
            return
        self._is_polling = True
        try:
            events = await self._client.list_task_events(self.metadata.task_id)
            new_events = self._collect_new_events(events)
            self._record_events(new_events)
        finally:
            self._is_polling = False

    def _collect_new_events(self, events: list[CloudSessionEvent]) -> list[CloudSessionEvent]:
        new: list[CloudSessionEvent] = []
        for event in events:
            if event.id in self._seen_event_ids:
                continue
            if self._last_seen_timestamp is None:
                new.append(event)
                continue
            order = _compare_strings(event.timestamp, self._last_seen_timestamp)
            if order > 0:
                new.append(event)
            elif order == 0 and event.id not in self._seen_event_ids_at_last_timestamp:
                new.append(event)
        return _sort_events_chronologically(new)

    def _record_events(self, events: list[CloudSessionEvent]) -> None:
        for event in _sort_events_chronologically(events):
            if event.id in self._seen_event_ids:
                continue
            self._seen_event_ids.add(event.id)
            self._events.append(event)
            self._mark_event_as_seen_at_timestamp(event)
            self._update_remote_steerable(event)
            self._dispatch_event(event)

    def _mark_event_as_seen_at_timestamp(self, event: CloudSessionEvent) -> None:
        if self._last_seen_timestamp != event.timestamp:
            self._last_seen_timestamp = event.timestamp
            self._seen_event_ids_at_last_timestamp = set()
        self._seen_event_ids_at_last_timestamp.add(event.id)

    def _update_remote_steerable(self, event: CloudSessionEvent) -> None:
        if event.type == "session.remote_steerable_changed" and event.data:
            self._remote_steerable = bool(event.data.get("remoteSteerable", True))

    def _dispatch_event(self, event: CloudSessionEvent) -> None:
        typed_handlers = self._typed_event_handlers.get(event.type)
        if typed_handlers:
            for handler in list(typed_handlers):
                try:
                    handler(event)
                except Exception:
                    pass

        for handler in list(self._event_handlers):
            try:
                handler(event)
            except Exception:
                pass

    def _report_poll_error(self, error: Exception) -> None:
        if self._on_event_poll_error:
            self._on_event_poll_error(error)

    def _assert_connected(self) -> None:
        if self._is_disconnected:
            raise RuntimeError("Cloud session is disconnected")


# ------------------------------------------------------------------
# Module-level helpers
# ------------------------------------------------------------------


def _sort_events_chronologically(
    events: list[CloudSessionEvent],
) -> list[CloudSessionEvent]:
    return sorted(events, key=lambda e: (e.timestamp, e.id))


def _compare_strings(a: str, b: str) -> int:
    if a > b:
        return 1
    if a < b:
        return -1
    return 0


def _snake_to_camel(name: str) -> str:
    """Convert a snake_case name to camelCase."""
    parts = name.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def _to_camel_case_dict(d: dict[str, Any]) -> dict[str, Any]:
    """Convert a dict with snake_case keys to camelCase keys."""
    return {_snake_to_camel(k): v for k, v in d.items()}
