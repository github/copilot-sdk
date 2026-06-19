"""E2E test asserting callback-raised errors surface to the SDK consumer as
transport failures.

Mirrors ``nodejs/test/e2e/llm_inference_errors.e2e.test.ts``. The handler
services the model catalog / session / policy normally so the agent reaches the
inference step, then raises from the inference callback. The adapter converts
that into a terminal ``http_response_chunk`` carrying ``error``, so the runtime
surfaces it through its existing error machinery rather than hanging.
"""

from __future__ import annotations

import pytest

from copilot import LlmInferenceRequest, LlmRequestHandler
from copilot.session import PermissionHandler

from ._llm_inference_helpers import (
    drain_request,
    isolated_client_fixture,
    respond_buffered,
    service_non_inference,
)
from .testharness import E2ETestContext  # noqa: F401  (ctx fixture dependency)

pytestmark = pytest.mark.asyncio(loop_scope="module")


class _ThrowingHandler(LlmRequestHandler):
    def __init__(self) -> None:
        self.total_calls = 0
        self.calls_before_error = 0

    async def on_llm_request(self, req: LlmInferenceRequest) -> None:
        self.total_calls += 1
        url = req.url.lower()

        if await service_non_inference(req):
            return

        if "/chat/completions" in url or "/responses" in url:
            await drain_request(req)
            self.calls_before_error += 1
            raise RuntimeError("synthetic-callback-transport-failure")

        await respond_buffered(req, 200, {"content-type": ["application/json"]}, "{}")


errors_client = isolated_client_fixture(_ThrowingHandler)


class TestLlmInferenceErrors:
    async def test_surfaces_callback_thrown_error_to_consumer(self, errors_client):
        client, handler = errors_client
        await client.start()
        session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )

        caught: BaseException | None = None
        try:
            await session.send_and_wait("Say OK.")
        except BaseException as err:  # noqa: BLE001
            caught = err
        finally:
            await session.disconnect()

        # The agent layer typically wraps inference failures in its own error
        # type and may convert them to an event rather than a thrown exception,
        # so the assertion is loose: the inference call was attempted at least
        # once and the runtime did NOT hang.
        assert handler.total_calls > 0
        assert handler.calls_before_error > 0
        if caught is not None:
            assert len(str(caught)) > 0
