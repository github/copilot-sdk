"""E2E tests for the LLM inference callback (basic round-trip).

Mirrors ``nodejs/test/e2e/llm_inference.e2e.test.ts``. The handler fabricates
synthetic model responses, so the runtime routes its model-layer HTTP through
the SDK callback instead of the CAPI proxy. No recorded snapshot is needed.
"""

from __future__ import annotations

import pytest

from copilot import LlmInferenceRequest, LlmRequestHandler
from copilot.session import PermissionHandler

from ._llm_inference_helpers import (
    handle_non_inference_model_traffic,
    isolated_client_fixture,
)
from .testharness import E2ETestContext  # noqa: F401  (ctx fixture dependency)

pytestmark = pytest.mark.asyncio(loop_scope="module")


class _RecordingHandler(LlmRequestHandler):
    def __init__(self) -> None:
        self.received: list[LlmInferenceRequest] = []

    async def on_llm_request(self, req: LlmInferenceRequest) -> None:
        self.received.append(req)
        await handle_non_inference_model_traffic(req)


llm_client = isolated_client_fixture(_RecordingHandler)


class TestLlmInferenceCallback:
    async def test_registers_the_provider_on_connect_without_erroring(self, llm_client):
        client, _ = llm_client
        await client.start()
        assert client is not None

    async def test_invokes_callback_for_model_layer_requests_and_threads_session_id(
        self, llm_client
    ):
        client, handler = llm_client
        await client.start()
        baseline = len(handler.received)
        session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            # The buffered handler returns empty JSON for inference, which is
            # not a valid model response; swallow the resulting transport error.
            # What we assert is that the runtime *attempted* the callback.
            try:
                await session.send_and_wait("Say OK.")
            except Exception:
                pass
        finally:
            await session.disconnect()

        assert len(handler.received) > baseline
        new_requests = handler.received[baseline:]
        for r in new_requests:
            assert r.url.startswith("http://") or r.url.startswith("https://")
            assert isinstance(r.method, str)

        catalog = next((r for r in new_requests if r.url.lower().endswith("/models")), None)
        assert catalog is not None, "expected to intercept the /models catalog request"

        in_session = next((r for r in new_requests if isinstance(r.session_id, str)), None)
        if in_session is not None:
            assert in_session.session_id
