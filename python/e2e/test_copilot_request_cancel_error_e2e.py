# --------------------------------------------------------------------------------------------
#  Copyright (c) Microsoft Corporation. All rights reserved.
# --------------------------------------------------------------------------------------------

"""Cancellation and error coverage for CopilotRequestHandler.

Mirrors ``nodejs/test/e2e/copilot_request_cancel_error.e2e.test.ts``. These
two scenarios exercise the handler's terminal paths that the happy-path
session-id and HTTP/WebSocket tests never reach:

* **Error** — the handler throws from :meth:`CopilotRequestHandler.send_request`
  for an inference request. The adapter reports a transport error back to the
  runtime rather than hanging.
* **Runtime cancel** — the handler blocks an inference request indefinitely;
  when the consumer aborts the turn the runtime cancels the in-flight request,
  firing ``ctx.cancel_event``. The handler observes the abort (the ``cancel``-frame
  path) instead of leaking a stuck request.

Non-inference model-layer requests (catalog, policy, model session) are served
with minimal stubs so the turn reaches the inference step.
"""

from __future__ import annotations

import asyncio

import httpx
import pytest

from copilot import CopilotRequestContext, CopilotRequestHandler
from copilot.session import PermissionHandler

from ._copilot_request_helpers import (
    is_inference_url,
    isolated_client_fixture,
)
from .testharness import E2ETestContext  # noqa: F401  (ctx fixture dependency)

pytestmark = pytest.mark.asyncio(loop_scope="module")


async def _wait_for(predicate, timeout_s: float) -> None:
    loop = asyncio.get_event_loop()
    start = loop.time()
    while not predicate():
        if loop.time() - start > timeout_s:
            raise TimeoutError("wait_for timed out")
        await asyncio.sleep(0.05)


class _ThrowingHandler(CopilotRequestHandler):
    """Throws from every inference request to exercise the error-reporting path."""

    def __init__(self) -> None:
        self.inference_attempts = 0

    async def send_request(
        self, request: httpx.Request, ctx: CopilotRequestContext
    ) -> httpx.Response:
        url = str(request.url)
        if not is_inference_url(url):
            return await super().send_request(request, ctx)
        self.inference_attempts += 1
        raise RuntimeError("synthetic-callback-transport-failure")


class _CancellingHandler(CopilotRequestHandler):
    """Blocks every inference request until the runtime cancels it."""

    def __init__(self) -> None:
        self.inference_entered = False
        self.saw_abort = False
        self.abort_seen = asyncio.Event()

    async def send_request(
        self, request: httpx.Request, ctx: CopilotRequestContext
    ) -> httpx.Response:
        url = str(request.url)
        if not is_inference_url(url):
            return await super().send_request(request, ctx)
        self.inference_entered = True
        await ctx.cancel_event.wait()
        self.saw_abort = True
        self.abort_seen.set()
        raise RuntimeError("cancelled by runtime")


throwing_client = isolated_client_fixture(_ThrowingHandler)
cancelling_client = isolated_client_fixture(_CancellingHandler)


class TestCopilotRequestHandlerError:
    async def test_reports_thrown_callback_error_instead_of_hanging(self, throwing_client):
        client, handler = throwing_client
        await client.start()
        session = await client.create_session(on_permission_request=PermissionHandler.approve_all)
        try:
            # The callback throws on inference; the turn surfaces an error (or
            # completes without an assistant message) rather than hanging.
            await session.send_and_wait("Say OK.")
        except BaseException:  # noqa: BLE001
            pass
        finally:
            await session.disconnect()

        assert handler.inference_attempts > 0, (
            "expected the inference callback to be reached and raise"
        )


class TestCopilotRequestHandlerCancel:
    async def test_fires_cancel_event_when_consumer_aborts_in_flight_request(
        self, cancelling_client
    ):
        client, handler = cancelling_client
        await client.start()
        session = await client.create_session(on_permission_request=PermissionHandler.approve_all)
        try:
            await session.send("Say OK.")
            await _wait_for(lambda: handler.inference_entered, 60.0)
            await session.abort()
            await asyncio.wait_for(handler.abort_seen.wait(), timeout=30.0)
        finally:
            await session.disconnect()

        assert handler.inference_entered is True, "expected the inference callback to be entered"
        assert handler.saw_abort is True, (
            "expected the callback to observe runtime cancellation via cancel_event"
        )
