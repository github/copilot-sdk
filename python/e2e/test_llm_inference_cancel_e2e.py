"""E2E test for the runtime → consumer cancellation path.

Mirrors ``nodejs/test/e2e/llm_inference_cancel.e2e.test.ts``. When an in-flight
turn is aborted via ``session.abort()``, the runtime cancels the
callback-served inference request; the consumer observes ``req.cancel_event``
firing so it can tear down its upstream call.
"""

from __future__ import annotations

import asyncio

import pytest

from copilot import LlmInferenceRequest, LlmRequestHandler
from copilot.session import PermissionHandler

from ._llm_inference_helpers import (
    drain_request,
    is_inference_url,
    isolated_client_fixture,
    respond_buffered,
    service_non_inference,
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


class _CancellingHandler(LlmRequestHandler):
    def __init__(self) -> None:
        self.inference_entered = False
        self.saw_abort = False
        self.abort_seen = asyncio.Event()

    async def on_llm_request(self, req: LlmInferenceRequest) -> None:
        if await service_non_inference(req):
            return
        if not is_inference_url(req.url):
            await respond_buffered(req, 200, {"content-type": ["application/json"]}, "{}")
            return

        # Inference: never produce a response. Wait for the runtime to cancel
        # us, recording the abort.
        await drain_request(req)
        self.inference_entered = True
        await req.cancel_event.wait()
        self.saw_abort = True
        self.abort_seen.set()
        try:
            await req.response_body.error("cancelled by upstream", code="cancelled")
        except Exception:
            # Runtime already dropped the request on cancel.
            pass


cancel_client = isolated_client_fixture(_CancellingHandler)


class TestLlmInferenceCancel:
    async def test_propagates_runtime_cancellation_to_consumer(self, cancel_client):
        client, handler = cancel_client
        await client.start()
        session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            await session.send("Say OK.")
            await _wait_for(lambda: handler.inference_entered, 60.0)
            await session.abort()
            await asyncio.wait_for(handler.abort_seen.wait(), timeout=30.0)
        finally:
            await session.disconnect()

        # The consumer observed the runtime-driven cancellation.
        assert handler.inference_entered is True
        assert handler.saw_abort is True
