"""Installation confirmation receiver support."""

from __future__ import annotations

import asyncio
import inspect
import logging
from collections.abc import Awaitable
from dataclasses import dataclass
from typing import Literal, Protocol

from ._jsonrpc import JsonRpcClient
from .generated.rpc import (
    InstallationConfirmationRequest,
    InstallationConfirmationResponse,
    InstallationDecision,
)

logger = logging.getLogger(__name__)

InstallationConfirmationDecision = InstallationDecision | Literal["confirm", "decline", "cancel"]
"""Decision returned by an installation confirmation handler."""


@dataclass(frozen=True)
class InstallationConfirmationContext:
    """Cancellation signal for one installation review on its original connection."""

    cancelled: asyncio.Event
    """Set when the runtime retires this request or the original connection closes."""


class InstallationConfirmationHandler(Protocol):
    """Collect a fresh human decision for one complete installation review.

    Match the request's operation and optional policy session metadata to the
    exact action previously registered on this connection before presenting it.
    The SDK echoes the original challenge and fingerprint, so the handler
    returns only an explicit decision.
    """

    def __call__(
        self,
        request: InstallationConfirmationRequest,
        context: InstallationConfirmationContext,
    ) -> InstallationConfirmationDecision | Awaitable[InstallationConfirmationDecision]:
        """Return ``"confirm"``, ``"decline"`` or ``"cancel"``."""


class _InstallationConfirmationAdapter:
    def __init__(
        self,
        client: JsonRpcClient,
        handler: InstallationConfirmationHandler | None,
    ) -> None:
        self._client = client
        self._handler = handler
        self._loop = asyncio.get_running_loop()
        self._connection_closed = asyncio.Event()

    def register(self) -> None:
        self._client.set_raw_request_handler("installations.confirm", self.handle_request)

    def close_connection(self) -> None:
        if self._connection_closed.is_set():
            return
        if self._loop.is_closed():
            return
        self._loop.call_soon_threadsafe(self._connection_closed.set)

    async def handle_request(self, message: dict) -> None:
        request_id = message["id"]
        request_cancelled = self._client.incoming_request_cancelled_event(request_id)
        cancelled = asyncio.Event()
        if request_cancelled.is_set() or self._connection_closed.is_set():
            cancelled.set()
        context = InstallationConfirmationContext(cancelled=cancelled)

        try:
            request = InstallationConfirmationRequest.from_dict(message.get("params"))
        except Exception:
            await self._client._send_error_response(  # noqa: SLF001
                request_id,
                -32602,
                "Invalid installation confirmation review",
                None,
            )
            return

        if self._handler is None:
            await self._client._send_error_response(  # noqa: SLF001
                request_id,
                -32603,
                "No installations client-global handler registered",
                None,
            )
            return

        confirmation_id = request.confirmation_id
        review_fingerprint = request.review_fingerprint
        handler_task = asyncio.create_task(self._call_handler(request, context))
        request_cancelled_task = asyncio.create_task(request_cancelled.wait())
        connection_closed_task = asyncio.create_task(self._connection_closed.wait())
        wait_tasks = {handler_task, request_cancelled_task, connection_closed_task}

        try:
            done, pending = await asyncio.wait(wait_tasks, return_when=asyncio.FIRST_COMPLETED)
            for task in pending - {handler_task}:
                task.cancel()

            if connection_closed_task in done or self._connection_closed.is_set():
                cancelled.set()
                _drop_late_handler_result(handler_task)
                return

            if request_cancelled_task in done or request_cancelled.is_set():
                cancelled.set()
                _drop_late_handler_result(handler_task)
                await self._client._send_error_response(  # noqa: SLF001
                    request_id,
                    -32800,
                    "Installation confirmation request cancelled",
                    None,
                )
                return

            decision = await handler_task
            if self._connection_closed.is_set():
                cancelled.set()
                return
            if request_cancelled.is_set():
                cancelled.set()
                await self._client._send_error_response(  # noqa: SLF001
                    request_id,
                    -32800,
                    "Installation confirmation request cancelled",
                    None,
                )
                return

            try:
                normalised = _normalise_decision(decision)
            except ValueError:
                await self._client._send_error_response(  # noqa: SLF001
                    request_id,
                    -32603,
                    "Invalid installation confirmation decision",
                    None,
                )
                return

            response = InstallationConfirmationResponse(
                confirmation_id=confirmation_id,
                decision=normalised,
                review_fingerprint=review_fingerprint,
            )
            await self._client._send_response(request_id, response.to_dict())  # noqa: SLF001
        except Exception as exc:  # pylint: disable=broad-except
            if self._connection_closed.is_set():
                cancelled.set()
                return
            await self._client._send_error_response(  # noqa: SLF001
                request_id,
                -32603,
                str(exc) or "Installation confirmation handler failed",
                None,
            )
        finally:
            for task in (request_cancelled_task, connection_closed_task):
                if not task.done():
                    task.cancel()

    async def _call_handler(
        self,
        request: InstallationConfirmationRequest,
        context: InstallationConfirmationContext,
    ) -> InstallationConfirmationDecision:
        assert self._handler is not None
        result = self._handler(request, context)
        if inspect.isawaitable(result):
            result = await result
        return result


def _normalise_decision(decision: InstallationConfirmationDecision) -> InstallationDecision:
    if isinstance(decision, InstallationDecision):
        return decision
    if isinstance(decision, str):
        try:
            return InstallationDecision(decision)
        except ValueError as exc:
            raise ValueError("Invalid installation confirmation decision") from exc
    raise ValueError("Invalid installation confirmation decision")


def _drop_late_handler_result(task: asyncio.Task) -> None:
    if task.done():
        try:
            task.result()
        except Exception:
            logger.debug("Installation confirmation handler failed after retirement", exc_info=True)
        return

    def consume(completed: asyncio.Task) -> None:
        try:
            completed.result()
        except Exception:
            logger.debug("Installation confirmation handler failed after retirement", exc_info=True)

    task.add_done_callback(consume)
