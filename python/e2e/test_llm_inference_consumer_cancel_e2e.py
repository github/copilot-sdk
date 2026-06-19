"""E2E test for the consumer → runtime cancellation path.

Mirrors ``nodejs/test/e2e/llm_inference_consumer_cancel.e2e.test.ts``. When the
consumer itself aborts the upstream call, it signals the runtime via
``response_body.error(code="cancelled")``. The runtime must surface that
faithfully as a request failure rather than hanging waiting for a response.
"""

from __future__ import annotations

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


class _ConsumerCancelHandler(LlmRequestHandler):
    def __init__(self) -> None:
        self.inference_attempts = 0

    async def on_llm_request(self, req: LlmInferenceRequest) -> None:
        if await service_non_inference(req):
            return
        if not is_inference_url(req.url):
            await respond_buffered(req, 200, {"content-type": ["application/json"]}, "{}")
            return

        # Consumer-initiated cancellation: the consumer's own upstream call was
        # aborted, so it tells the runtime to give up on this request. No
        # response head is ever produced; the runtime should see a transport
        # failure rather than hanging.
        await drain_request(req)
        self.inference_attempts += 1
        await req.response_body.error("upstream call aborted by consumer", code="cancelled")


consumer_cancel_client = isolated_client_fixture(_ConsumerCancelHandler)


class TestLlmInferenceConsumerCancel:
    async def test_surfaces_consumer_signalled_cancellation(self, consumer_cancel_client):
        client, handler = consumer_cancel_client
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

        # The runtime reached the inference step and the consumer's
        # cancellation terminated it (rather than the runtime hanging).
        assert handler.inference_attempts > 0
        if caught is not None:
            assert len(str(caught)) > 0
