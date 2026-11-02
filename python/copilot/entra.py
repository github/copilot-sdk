"""Experimental host-owned Microsoft Entra acquisition."""

from __future__ import annotations

import asyncio
import logging
from collections.abc import Awaitable
from dataclasses import dataclass, field
from typing import Protocol

from ._jsonrpc import JsonRpcClient
from .generated.rpc import (
    EntraAuthConfig,
    EntraTokenAcquireRequest,
    EntraTokenAcquireResult,
    EntraTokenGetTokenRequest,
)

logger = logging.getLogger(__name__)

EntraTokenProviderArgs = EntraTokenAcquireRequest
"""**Experimental.** Request for host-owned Entra identity or resource acquisition."""


@dataclass(repr=False)
class EntraTokenProviderResult(EntraTokenAcquireResult):
    """**Experimental.** Acquisition result whose representation never includes a credential."""

    def __repr__(self) -> str:
        return f"EntraTokenProviderResult(kind={self.kind!r})"


@dataclass(frozen=True)
class EntraTokenProviderContext:
    """**Experimental.** Cancellation lifetime of one acquisition on its original connection."""

    cancelled: asyncio.Event


class EntraTokenProvider(Protocol):
    """**Experimental.** Select/revalidate an identity or acquire its resource token.

    Renewal must be silent and account-pinned. Unavailability and cancellation
    never open runtime sign-in.
    """

    def __call__(
        self, request: EntraTokenProviderArgs, context: EntraTokenProviderContext
    ) -> EntraTokenProviderResult | Awaitable[EntraTokenProviderResult]:
        """Return the matching identity, token, unavailable, or cancelled result."""


@dataclass
class EntraAuthOptions(EntraAuthConfig):
    """**Experimental.** Entra application and optional host-owned acquisition."""

    token_provider: EntraTokenProvider | None = field(default=None, repr=False, compare=False)
    """**Experimental.** Host callback; omission retains built-in runtime authentication."""


class _EntraTokenAdapter:
    def __init__(self, client: JsonRpcClient, provider: EntraTokenProvider) -> None:
        self._client = client
        self._provider = provider
        self._loop = asyncio.get_running_loop()
        self._closed = asyncio.Event()

    def register(self) -> None:
        self._client.set_raw_request_handler("entraToken.getToken", self.handle_request)

    def close_connection(self) -> None:
        if not self._loop.is_closed():
            self._loop.call_soon_threadsafe(self._closed.set)

    async def handle_request(self, message: dict) -> None:
        request_id = message["id"]
        retired = self._client.incoming_request_cancelled_event(request_id)
        cancelled = asyncio.Event()
        context = EntraTokenProviderContext(cancelled)
        try:
            request = EntraTokenGetTokenRequest.from_dict(message.get("params")).request
        except (AssertionError, KeyError, TypeError, ValueError):
            await self._client._send_error_response(  # noqa: SLF001
                request_id, -32602, "Invalid Entra acquisition request", None
            )
            return
        if retired.is_set() or self._closed.is_set():
            cancelled.set()
            if not self._closed.is_set():
                await self._client._send_error_response(  # noqa: SLF001
                    request_id, -32800, "Entra acquisition cancelled", None
                )
            return
        handler = asyncio.create_task(self._invoke(request, context, retired))
        cancellation = asyncio.create_task(retired.wait())
        closure = asyncio.create_task(self._closed.wait())
        try:
            await asyncio.wait(
                {handler, cancellation, closure}, return_when=asyncio.FIRST_COMPLETED
            )
            if self._closed.is_set() or retired.is_set():
                cancelled.set()
                if not self._closed.is_set():
                    await self._client._send_error_response(  # noqa: SLF001
                        request_id, -32800, "Entra acquisition cancelled", None
                    )
                return
            result = await handler
            await self._client._send_response(request_id, result.to_dict())  # noqa: SLF001
        except asyncio.CancelledError:
            cancelled.set()
            if not self._closed.is_set():
                if handler.cancelled() and not retired.is_set():
                    await self._client._send_error_response(  # noqa: SLF001
                        request_id, -32603, "Entra token provider failed", None
                    )
                else:
                    await self._client._send_error_response(  # noqa: SLF001
                        request_id, -32800, "Entra acquisition cancelled", None
                    )
        except Exception:
            await self._client._send_error_response(  # noqa: SLF001
                request_id, -32603, "Entra token provider failed", None
            )
        finally:
            cancellation.cancel()
            closure.cancel()
            await asyncio.gather(cancellation, closure, return_exceptions=True)
            if handler.done():
                _observe_retired_handler(handler)
            else:
                cancelled.set()
                handler.add_done_callback(_observe_retired_handler)

    async def _invoke(
        self,
        request: EntraTokenProviderArgs,
        context: EntraTokenProviderContext,
        retired: asyncio.Event,
    ) -> EntraTokenAcquireResult:
        if self._closed.is_set() or retired.is_set() or context.cancelled.is_set():
            context.cancelled.set()
            raise RuntimeError("Entra acquisition cancelled")
        result = self._provider(request, context)
        if isinstance(result, EntraTokenAcquireResult):
            return result
        return await result


def _observe_retired_handler(task: asyncio.Task) -> None:
    if not task.cancelled() and task.exception() is not None:
        logger.debug("Entra token provider task failed")
