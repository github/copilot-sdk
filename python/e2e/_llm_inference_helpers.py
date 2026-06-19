"""Shared fixtures and synthetic-upstream helpers for the LLM inference
callback e2e tests.

The ``llm_inference*`` tests have no recorded snapshots: the registered
callback fabricates well-formed model responses and the runtime routes all of
its model-layer HTTP/WebSocket traffic through that callback instead of the
CAPI proxy. These helpers centralise the synthetic CAPI shapes (model catalog,
policy, ``/responses`` SSE, ``/chat/completions``) so each test file can focus
on the behaviour it is exercising.

The leading underscore keeps pytest from collecting this module as a test.
"""

from __future__ import annotations

import json
import os
import re

import pytest_asyncio

from copilot import (
    CopilotClient,
    LlmInferenceConfig,
    LlmInferenceRequest,
    LlmInferenceResponseInit,
    LlmRequestHandler,
    RuntimeConnection,
)
from copilot.generated.session_events import AssistantMessageData

from .testharness import E2ETestContext

SYNTHETIC_TEXT = "OK from the synthetic stream."


def sse(event: str, data: dict) -> str:
    """Frame a single Server-Sent Events message: ``event:``/``data:`` + blank line."""
    return f"event: {event}\ndata: {json.dumps(data)}\n\n"


def stream_true(body_text: str) -> bool:
    return re.search(r'"stream"\s*:\s*true', body_text) is not None


def is_inference_url(url: str) -> bool:
    u = url.lower()
    return (
        u.endswith("/chat/completions")
        or u.endswith("/responses")
        or u.endswith("/v1/messages")
        or u.endswith("/messages")
    )


def model_catalog(supported_endpoints: list[str] | None = None) -> dict:
    """The synthetic ``/models`` catalog payload.

    Passing ``supported_endpoints=["/responses", "ws:/responses"]`` lets the
    runtime pick the WebSocket Responses transport (when the matching ExP flag
    is enabled).
    """
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
    """The ordered ``/responses`` event objects the runtime's reducer expects.

    Used raw (one object == one WebSocket message) for the WS path and
    SSE-framed for the HTTP path.
    """
    return [
        {
            "type": "response.created",
            "response": {"id": resp_id, "object": "response", "status": "in_progress", "output": []},
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
        {"type": "response.output_text.delta", "output_index": 0, "content_index": 0, "delta": text},
        {"type": "response.output_text.done", "output_index": 0, "content_index": 0, "text": text},
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


async def drain_request(req: LlmInferenceRequest) -> str:
    parts: list[bytes] = []
    async for chunk in req.request_body:
        parts.append(chunk)
    return b"".join(parts).decode("utf-8")


async def respond_buffered(
    req: LlmInferenceRequest, status: int, headers: dict[str, list[str]], body: str
) -> None:
    await drain_request(req)
    await req.response_body.start(LlmInferenceResponseInit(status=status, headers=headers))
    if body:
        await req.response_body.write(body)
    await req.response_body.end()


async def service_non_inference(req: LlmInferenceRequest) -> bool:
    """Serve the model catalog, model session and policy endpoints.

    Returns ``True`` when the request was one of those (and has been answered),
    ``False`` otherwise so the caller can decide how to handle it.
    """
    url = req.url.lower()
    if url.endswith("/models"):
        await respond_buffered(
            req, 200, {"content-type": ["application/json"]}, json.dumps(model_catalog())
        )
        return True
    if "/models/session" in url:
        await respond_buffered(req, 200, {}, "{}")
        return True
    if "/policy" in url:
        await respond_buffered(req, 200, {}, json.dumps({"state": "enabled"}))
        return True
    return False


async def handle_non_inference_model_traffic(
    req: LlmInferenceRequest, supported_endpoints: list[str] | None = None
) -> None:
    """Serve every non-inference model-layer request, including an empty-JSON
    fallback for anything unrecognised."""
    url = req.url.lower()
    if url.endswith("/models"):
        await respond_buffered(
            req,
            200,
            {"content-type": ["application/json"]},
            json.dumps(model_catalog(supported_endpoints)),
        )
        return
    if "/models/session" in url:
        await respond_buffered(req, 200, {}, "{}")
        return
    if "/policy" in url:
        await respond_buffered(req, 200, {}, json.dumps({"state": "enabled"}))
        return
    await respond_buffered(req, 200, {"content-type": ["application/json"]}, "{}")


async def handle_inference(req: LlmInferenceRequest, text: str = SYNTHETIC_TEXT) -> None:
    """Synthesize a well-formed inference response.

    Dispatches by URL and the request body's ``stream`` flag: ``/responses``
    streams an SSE event sequence (or returns a buffered Responses object when
    ``stream`` is false), ``/chat/completions`` streams chat-completion chunks
    (or returns a buffered completion). The unified callback carries no field
    telling the consumer which code path the runtime took, so it dispatches by
    URL exactly as a real reverse proxy would.
    """
    body_text = await drain_request(req)
    wants_stream = stream_true(body_text)
    url = req.url.lower()

    if "/responses" in url:
        if not wants_stream:
            await req.response_body.start(
                LlmInferenceResponseInit(status=200, headers={"content-type": ["application/json"]})
            )
            await req.response_body.write(json.dumps(responses_events(text)[-1]["response"]))
            await req.response_body.end()
            return
        await req.response_body.start(
            LlmInferenceResponseInit(status=200, headers={"content-type": ["text/event-stream"]})
        )
        for event in responses_events(text):
            await req.response_body.write(sse(event["type"], event))
        await req.response_body.end()
        return

    if "/chat/completions" in url and wants_stream:
        await req.response_body.start(
            LlmInferenceResponseInit(status=200, headers={"content-type": ["text/event-stream"]})
        )
        base = {
            "id": "chatcmpl-stub-1",
            "object": "chat.completion.chunk",
            "created": 1,
            "model": "claude-sonnet-4.5",
        }
        chunks = [
            {**base, "choices": [{"index": 0, "delta": {"role": "assistant", "content": ""}, "finish_reason": None}]},
            {**base, "choices": [{"index": 0, "delta": {"content": text}, "finish_reason": None}]},
            {
                **base,
                "choices": [{"index": 0, "delta": {}, "finish_reason": "stop"}],
                "usage": {"prompt_tokens": 5, "completion_tokens": 7, "total_tokens": 12},
            },
        ]
        for chunk in chunks:
            await req.response_body.write("data: " + json.dumps(chunk) + "\n\n")
        await req.response_body.write("data: [DONE]\n\n")
        await req.response_body.end()
        return

    await req.response_body.start(
        LlmInferenceResponseInit(status=200, headers={"content-type": ["application/json"]})
    )
    await req.response_body.write(
        json.dumps(
            {
                "id": "chatcmpl-stub-1",
                "object": "chat.completion",
                "created": 1,
                "model": "claude-sonnet-4.5",
                "choices": [
                    {"index": 0, "message": {"role": "assistant", "content": text}, "finish_reason": "stop"}
                ],
                "usage": {"prompt_tokens": 5, "completion_tokens": 7, "total_tokens": 12},
            }
        )
    )
    await req.response_body.end()


def assistant_text(event) -> str:
    if event is not None and isinstance(event.data, AssistantMessageData):
        return event.data.content
    return ""


def build_isolated_client(
    ctx: E2ETestContext,
    handler: LlmRequestHandler,
    extra_env: dict[str, str] | None = None,
) -> CopilotClient:
    """Build a CopilotClient wired to ``handler`` via ``LlmInferenceConfig``.

    The shared ``ctx`` fixture's client has no inference callback, so each
    inference test owns an isolated client carrying its own handler.
    ``extra_env`` is merged into the spawned runtime's environment (e.g. to
    flip an ExP flag for the WebSocket transport).
    """
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
        llm_inference=LlmInferenceConfig(handler=handler),
    )


def isolated_client_fixture(make_handler, extra_env: dict[str, str] | None = None):
    """Build a module-scoped pytest-asyncio fixture yielding ``(client, handler)``.

    ``make_handler`` is a zero-arg callable returning a fresh handler instance.
    """

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
