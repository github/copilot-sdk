"""E2E test for the LLM inference callback over the full-duplex WebSocket
transport.

Mirrors ``nodejs/test/e2e/llm_inference_websocket.e2e.test.ts``. The fake model
catalog advertises ``/responses`` and ``ws:/responses`` so the runtime selects
the Responses wire API and is allowed to pick the WebSocket transport (the ExP
flag is enabled via the env var below). The handler services the WS channel by
answering each inbound ``response.create`` message with the ordered
``/responses`` event objects — one event per outbound WS message, raw JSON
(not SSE-framed).
"""

from __future__ import annotations

import json

import pytest

from copilot import LlmInferenceRequest, LlmInferenceResponseInit, LlmRequestHandler
from copilot.session import PermissionHandler

from ._llm_inference_helpers import (
    assistant_text,
    drain_request,
    handle_non_inference_model_traffic,
    is_inference_url,
    isolated_client_fixture,
    responses_events,
)
from .testharness import E2ETestContext  # noqa: F401  (ctx fixture dependency)

pytestmark = pytest.mark.asyncio(loop_scope="module")

WS_TEXT = "OK from the synthetic ws."


async def _handle_http_inference(req: LlmInferenceRequest) -> None:
    """Synthesize the ``/responses`` SSE stream for single-shot HTTP inference
    requests (e.g. title generation) that don't pick the WebSocket transport."""
    await drain_request(req)
    await req.response_body.start(
        LlmInferenceResponseInit(status=200, headers={"content-type": ["text/event-stream"]})
    )
    for event in responses_events(WS_TEXT, "resp_stub_ws_1"):
        await req.response_body.write(f"event: {event['type']}\ndata: {json.dumps(event)}\n\n")
    await req.response_body.end()


class _WebSocketHandler(LlmRequestHandler):
    def __init__(self) -> None:
        self.received: list[LlmInferenceRequest] = []
        self.ws_request_count = 0

    async def _handle_web_socket(self, req: LlmInferenceRequest) -> None:
        # Ack the upgrade (status 101-equivalent) before any message flows.
        await req.response_body.start(LlmInferenceResponseInit(status=101, headers={}))
        try:
            # One inbound chunk == one WS message (a `response.create` request).
            async for _outbound in req.request_body:
                self.ws_request_count += 1
                for event in responses_events(WS_TEXT, "resp_stub_ws_1"):
                    await req.response_body.write(json.dumps(event))
        except Exception:
            # Expected: the runtime cancels the request body when it closes the
            # socket at session teardown. Nothing more to do.
            pass

    async def on_llm_request(self, req: LlmInferenceRequest) -> None:
        self.received.append(req)
        if req.transport == "websocket":
            await self._handle_web_socket(req)
            return
        if is_inference_url(req.url):
            await _handle_http_inference(req)
        else:
            await handle_non_inference_model_traffic(
                req, supported_endpoints=["/responses", "ws:/responses"]
            )


ws_client = isolated_client_fixture(
    _WebSocketHandler,
    extra_env={"COPILOT_EXP_COPILOT_CLI_WEBSOCKET_RESPONSES": "true"},
)


class TestLlmInferenceWebSocket:
    async def test_completes_a_turn_over_the_websocket_transport(self, ws_client):
        client, handler = ws_client
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

        # The main agent turn (tools present, not single-shot) selected the
        # WebSocket transport and drove it through the callback.
        ws_reqs = [r for r in handler.received if r.transport == "websocket"]
        assert len(ws_reqs) > 0, "expected at least one websocket request via the callback"
        assert handler.ws_request_count > 0, "expected the runtime to send at least one ws message"

        # Validate the final assistant response arrived (guards against truncated captures)
        assert "OK from the synthetic ws" in text
