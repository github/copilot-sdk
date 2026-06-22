# --------------------------------------------------------------------------------------------
#  Copyright (c) Microsoft Corporation. All rights reserved.
# --------------------------------------------------------------------------------------------

"""Shared fixtures and response-builder helpers for the CopilotRequestHandler e2e tests.

The ``copilot_request_*`` tests have no recorded snapshots: the registered
handler fabricates well-formed model responses and the runtime routes all of
its model-layer HTTP/WebSocket traffic through that handler instead of the
CAPI proxy. These helpers centralise the synthetic CAPI shapes (model catalog,
policy, ``/responses`` SSE, ``/chat/completions``) so each test file can focus
on the behaviour it is exercising.

The leading underscore keeps pytest from collecting this module as a test.
"""

from __future__ import annotations

import json
import os
import re

import httpx
import pytest_asyncio

from copilot import CopilotClient, CopilotRequestHandler, RuntimeConnection
from copilot.generated.session_events import AssistantMessageData

from .testharness import E2ETestContext

SYNTHETIC_TEXT = "OK from the synthetic stream."


def sse(event: str, data: dict) -> str:
    """Frame a single Server-Sent Events message: ``event:``/``data:`` + blank line."""
    return f"event: {event}\ndata: {json.dumps(data)}\n\n"


def is_inference_url(url: str) -> bool:
    """Return True if ``url`` is a model inference endpoint.

    Strips query parameters before matching so URLs like
    ``/chat/completions?api-version=2024-02`` are handled correctly.
    """
    path = url.lower().split("?", 1)[0]
    return (
        path.endswith("/chat/completions")
        or path.endswith("/responses")
        or path.endswith("/v1/messages")
        or path.endswith("/messages")
    )


def _wants_stream(body: bytes) -> bool:
    return re.search(rb'"stream"\s*:\s*true', body) is not None


def model_catalog(supported_endpoints: list[str] | None = None) -> dict:
    """The synthetic ``/models`` catalog payload."""
    model: dict = {
        "id": "claude-sonnet-4.5",
        "name": "Claude Sonnet 4.5",
        "object": "model",
        "vendor": "Anthropic",
        "version": "1",
        "preview": False,
        "model_picker_enabled": True,
        "capabilities": {
            "type": "chat",
            "family": "claude-sonnet-4.5",
            "tokenizer": "o200k_base",
            "limits": {"max_context_window_tokens": 200000, "max_output_tokens": 8192},
            "supports": {
                "streaming": True,
                "tool_calls": True,
                "parallel_tool_calls": True,
                "vision": True,
            },
        },
    }
    if supported_endpoints is not None:
        model["supported_endpoints"] = supported_endpoints
    return {"data": [model]}


def responses_events(text: str, resp_id: str = "resp_stub_1") -> list[dict]:
    """The ordered ``/responses`` event objects the runtime's reducer expects."""
    return [
        {
            "type": "response.created",
            "response": {
                "id": resp_id,
                "object": "response",
                "status": "in_progress",
                "output": [],
            },
        },
        {
            "type": "response.output_item.added",
            "output_index": 0,
            "item": {"id": "msg_1", "type": "message", "role": "assistant", "content": []},
        },
        {
            "type": "response.content_part.added",
            "output_index": 0,
            "content_index": 0,
            "part": {"type": "output_text", "text": ""},
        },
        {
            "type": "response.output_text.delta",
            "output_index": 0,
            "content_index": 0,
            "delta": text,
        },
        {
            "type": "response.output_text.done",
            "output_index": 0,
            "content_index": 0,
            "text": text,
        },
        {
            "type": "response.completed",
            "response": {
                "id": resp_id,
                "object": "response",
                "status": "completed",
                "output": [
                    {
                        "id": "msg_1",
                        "type": "message",
                        "role": "assistant",
                        "content": [{"type": "output_text", "text": text}],
                    }
                ],
                "usage": {"input_tokens": 5, "output_tokens": 7, "total_tokens": 12},
            },
        },
    ]


def build_non_inference_response(
    url: str, supported_endpoints: list[str] | None = None
) -> httpx.Response:
    """Build a minimal ``httpx.Response`` for non-inference model-layer requests."""
    path = url.lower().split("?", 1)[0]  # strip query params before matching
    if path.endswith("/models"):
        return httpx.Response(
            200,
            headers={"content-type": "application/json"},
            content=json.dumps(model_catalog(supported_endpoints)).encode(),
        )
    if "/models/session" in path:
        return httpx.Response(200, headers={"content-type": "application/json"}, content=b"{}")
    if "/policy" in path:
        return httpx.Response(
            200,
            headers={"content-type": "application/json"},
            content=json.dumps({"state": "enabled"}).encode(),
        )
    return httpx.Response(200, headers={"content-type": "application/json"}, content=b"{}")


def build_inference_response(request: httpx.Request, text: str = SYNTHETIC_TEXT) -> httpx.Response:
    """Build a synthetic inference response for ``/responses`` or ``/chat/completions``.

    Dispatches by URL and the request body's ``stream`` flag: ``/responses``
    streams an SSE event sequence (or returns a buffered Responses object when
    ``stream`` is false), ``/chat/completions`` streams chat-completion chunks
    (or returns a buffered completion).
    """
    body = request.content  # already drained when send_request is called
    wants_stream = _wants_stream(body)
    url = str(request.url).lower()

    if "/responses" in url:
        if not wants_stream:
            return httpx.Response(
                200,
                headers={"content-type": "application/json"},
                content=json.dumps(responses_events(text)[-1]["response"]).encode(),
            )
        stream_body = "".join(sse(e["type"], e) for e in responses_events(text))
        return httpx.Response(
            200,
            headers={"content-type": "text/event-stream"},
            content=stream_body.encode(),
        )

    if "/chat/completions" in url and wants_stream:
        base = {
            "id": "chatcmpl-stub-1",
            "object": "chat.completion.chunk",
            "created": 1,
            "model": "claude-sonnet-4.5",
        }
        chunks = [
            {
                **base,
                "choices": [
                    {
                        "index": 0,
                        "delta": {"role": "assistant", "content": ""},
                        "finish_reason": None,
                    }
                ],
            },
            {
                **base,
                "choices": [{"index": 0, "delta": {"content": text}, "finish_reason": None}],
            },
            {
                **base,
                "choices": [{"index": 0, "delta": {}, "finish_reason": "stop"}],
                "usage": {"prompt_tokens": 5, "completion_tokens": 7, "total_tokens": 12},
            },
        ]
        stream_body = (
            "".join("data: " + json.dumps(c) + "\n\n" for c in chunks) + "data: [DONE]\n\n"
        )
        return httpx.Response(
            200,
            headers={"content-type": "text/event-stream"},
            content=stream_body.encode(),
        )

    return httpx.Response(
        200,
        headers={"content-type": "application/json"},
        content=json.dumps(
            {
                "id": "chatcmpl-stub-1",
                "object": "chat.completion",
                "created": 1,
                "model": "claude-sonnet-4.5",
                "choices": [
                    {
                        "index": 0,
                        "message": {"role": "assistant", "content": text},
                        "finish_reason": "stop",
                    }
                ],
                "usage": {"prompt_tokens": 5, "completion_tokens": 7, "total_tokens": 12},
            }
        ).encode(),
    )


def assistant_text(event) -> str:
    if event is not None and isinstance(event.data, AssistantMessageData):
        return event.data.content
    return ""


def build_isolated_client(
    ctx: E2ETestContext,
    handler: CopilotRequestHandler,
    extra_env: dict[str, str] | None = None,
) -> CopilotClient:
    """Build a CopilotClient wired to ``handler`` via ``request_handler``."""
    github_token = (
        "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
    )
    env = ctx.get_env()
    if extra_env:
        env = {**env, **extra_env}
    return CopilotClient(
        connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
        working_directory=ctx.work_dir,
        env=env,
        github_token=github_token,
        request_handler=handler,
    )


def isolated_client_fixture(make_handler, extra_env: dict[str, str] | None = None):
    """Build a module-scoped pytest-asyncio fixture yielding ``(client, handler)``."""

    @pytest_asyncio.fixture(loop_scope="module")
    async def _fixture(ctx: E2ETestContext):
        handler = make_handler()
        client = build_isolated_client(ctx, handler, extra_env)
        try:
            yield client, handler
        finally:
            try:
                await client.stop()
            except Exception:
                pass

    return _fixture
