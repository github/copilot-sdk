"""E2E test for the LLM inference callback over a fully-mocked streaming
response.

Mirrors ``nodejs/test/e2e/llm_inference_stream.e2e.test.ts``. The callback
services every model-layer request and answers the inference call with a
chunked SSE event stream; the test asserts the synthetic content surfaces in
the assistant turn.
"""

from __future__ import annotations

import pytest

from copilot import LlmInferenceRequest, LlmRequestHandler
from copilot.session import PermissionHandler

from ._llm_inference_helpers import (
    assistant_text,
    handle_inference,
    handle_non_inference_model_traffic,
    is_inference_url,
    isolated_client_fixture,
)
from .testharness import E2ETestContext  # noqa: F401  (ctx fixture dependency)

pytestmark = pytest.mark.asyncio(loop_scope="module")


class _StreamingHandler(LlmRequestHandler):
    def __init__(self) -> None:
        self.received: list[LlmInferenceRequest] = []

    async def on_llm_request(self, req: LlmInferenceRequest) -> None:
        self.received.append(req)
        if is_inference_url(req.url):
            await handle_inference(req)
        else:
            await handle_non_inference_model_traffic(req)


stream_client = isolated_client_fixture(_StreamingHandler)


class TestLlmInferenceStream:
    async def test_completes_a_turn_via_chunked_sse_response(self, stream_client):
        client, handler = stream_client
        await client.start()
        session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        text = ""
        try:
            result = await session.send_and_wait("Say OK.")
            text = assistant_text(result)
        finally:
            await session.disconnect()

        inference = [r for r in handler.received if is_inference_url(r.url)]
        assert len(inference) > 0, "expected at least one inference request via the callback"

        # Validate the final assistant response arrived (guards against truncated captures)
        assert "OK from the synthetic" in text
