"""E2E tests asserting the runtime threads its session id into the LLM
inference callback for both CAPI and BYOK sessions.

Mirrors ``nodejs/test/e2e/llm_inference_session_id.e2e.test.ts``. The callback
alone services every model-layer request (no upstream server, no CAPI proxy
acting as the inference endpoint), so the only source of ``req.session_id`` is
the runtime's own per-client threading.
"""

from __future__ import annotations

from dataclasses import dataclass

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


@dataclass
class _InterceptedRequest:
    url: str
    session_id: str | None


class _SessionIdHandler(LlmRequestHandler):
    def __init__(self) -> None:
        self.records: list[_InterceptedRequest] = []

    async def on_llm_request(self, req: LlmInferenceRequest) -> None:
        self.records.append(_InterceptedRequest(url=req.url, session_id=req.session_id))
        if is_inference_url(req.url):
            await handle_inference(req)
        else:
            await handle_non_inference_model_traffic(req)


session_id_client = isolated_client_fixture(_SessionIdHandler)


class TestLlmInferenceSessionId:
    capi_session_id: str | None = None

    async def test_threads_session_id_into_capi_session(self, session_id_client):
        client, handler = session_id_client
        await client.start()
        baseline = len(handler.records)
        session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        TestLlmInferenceSessionId.capi_session_id = session.session_id
        text = ""
        try:
            result = await session.send_and_wait("Say OK.")
            text = assistant_text(result)
        finally:
            await session.disconnect()

        inference = [r for r in handler.records[baseline:] if is_inference_url(r.url)]
        assert len(inference) > 0, "expected at least one intercepted inference request"
        for r in inference:
            assert r.session_id == session.session_id, (
                "CAPI inference request must carry the runtime session id"
            )

        # Validate the final assistant response arrived (guards against truncated captures)
        assert "OK from the synthetic" in text

    async def test_threads_session_id_into_byok_session(self, session_id_client):
        client, handler = session_id_client
        await client.start()
        baseline = len(handler.records)
        session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model="claude-sonnet-4.5",
            provider={
                "type": "openai",
                "wire_api": "responses",
                "base_url": "https://byok.invalid/v1",
                "api_key": "byok-secret",
                "model_id": "claude-sonnet-4.5",
                "wire_model": "claude-sonnet-4.5",
            },
        )
        byok_session_id = session.session_id
        text = ""
        try:
            result = await session.send_and_wait("Say OK.")
            text = assistant_text(result)
        finally:
            await session.disconnect()

        inference = [r for r in handler.records[baseline:] if is_inference_url(r.url)]
        assert len(inference) > 0, "expected at least one intercepted BYOK inference request"
        for r in inference:
            assert r.session_id == byok_session_id, (
                "BYOK inference request must carry the runtime session id"
            )

        # Session ids are per-session, so the two turns must differ.
        assert byok_session_id != TestLlmInferenceSessionId.capi_session_id

        # Validate the final assistant response arrived (guards against truncated captures)
        assert "OK from the synthetic" in text
