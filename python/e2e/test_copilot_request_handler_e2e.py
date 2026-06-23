# --------------------------------------------------------------------------------------------
#  Copyright (c) Microsoft Corporation. All rights reserved.
# --------------------------------------------------------------------------------------------

"""E2E test for the idiomatic ``CopilotRequestHandler`` forwarding seams.

Mirrors ``nodejs/test/e2e/copilot_request_handler.e2e.test.ts``. A single
handler subclass services BOTH transports against a per-test fake upstream:

* HTTP — :meth:`send_request` rewrites the request to the local HTTP upstream,
  mutates an outbound and a response header, and forwards via httpx.
* WebSocket — :meth:`open_websocket` rewrites the URL to the local WebSocket
  upstream and returns a forwarding handler that counts messages in both
  directions.

Unlike the other inference tests (which fabricate responses inline), this one
exercises the default httpx / ``websockets`` forwarding machinery against a
real socket, proving the full chain runtime → handler → upstream → handler →
runtime is intact for whichever transport the agent turn selects.
"""

from __future__ import annotations

import json
import os
import threading
from dataclasses import dataclass, field
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import httpx
import pytest
import pytest_asyncio
from websockets.asyncio.server import serve as ws_serve

from copilot import (
    CopilotClient,
    CopilotRequestContext,
    CopilotRequestHandler,
    CopilotWebSocketForwarder,
    RuntimeConnection,
)
from copilot.session import PermissionHandler

from ._copilot_request_helpers import assistant_text, model_catalog, responses_events
from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

HTTP_TEXT = "OK from synthetic HTTP upstream."
WS_TEXT = "OK from synthetic WS upstream."


@dataclass
class _Counters:
    http_requests: int = 0
    http_responses: int = 0
    ws_request_messages: int = 0
    ws_response_messages: int = 0


@dataclass
class _Upstream:
    http_url: str
    ws_url: str
    _http_server: ThreadingHTTPServer
    _http_thread: threading.Thread
    _ws_server: object
    ws_requests: list[int] = field(default_factory=lambda: [0])

    @property
    def ws_request_count(self) -> int:
        return self.ws_requests[0]

    async def close(self) -> None:
        self._http_server.shutdown()
        self._http_thread.join(timeout=5)
        self._http_server.server_close()
        self._ws_server.close()  # type: ignore[attr-defined]
        await self._ws_server.wait_closed()  # type: ignore[attr-defined]


def _sse_body(text: str, resp_id: str) -> bytes:
    out = "".join(
        f"event: {event['type']}\ndata: {json.dumps(event)}\n\n"
        for event in responses_events(text, resp_id)
    )
    return out.encode("utf-8")


async def _start_fake_upstream() -> _Upstream:
    class _Handler(BaseHTTPRequestHandler):
        def log_message(self, *_args):  # noqa: ANN002 - silence default logging
            pass

        def _send(self, status: int, content_type: str, body: bytes) -> None:
            self.send_response(status)
            self.send_header("content-type", content_type)
            self.send_header("content-length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def _route(self) -> None:
            path = self.path.split("?", 1)[0].lower()
            length = int(self.headers.get("content-length") or 0)
            if length:
                self.rfile.read(length)
            if path.endswith("/models"):
                self._send(
                    200,
                    "application/json",
                    json.dumps(
                        model_catalog(supported_endpoints=["/responses", "ws:/responses"])
                    ).encode("utf-8"),
                )
                return
            if path.endswith("/models/session"):
                self._send(200, "application/json", b"{}")
                return
            if "/policy" in path:
                self._send(
                    200,
                    "application/json",
                    json.dumps({"state": "enabled"}).encode("utf-8"),
                )
                return
            if path.endswith("/responses"):
                self._send(200, "text/event-stream", _sse_body(HTTP_TEXT, "resp_stub_http"))
                return
            self._send(
                404,
                "application/json",
                json.dumps({"error": "not_found", "path": path}).encode("utf-8"),
            )

        def do_GET(self):  # noqa: N802
            self._route()

        def do_POST(self):  # noqa: N802
            self._route()

    http_server = ThreadingHTTPServer(("127.0.0.1", 0), _Handler)
    http_port = http_server.server_address[1]
    http_thread = threading.Thread(target=http_server.serve_forever, daemon=True)
    http_thread.start()

    ws_requests = [0]

    async def ws_handler(connection) -> None:
        async for _raw in connection:
            ws_requests[0] += 1
            for event in responses_events(WS_TEXT, "resp_stub_ws"):
                await connection.send(json.dumps(event))

    ws_server = await ws_serve(ws_handler, "127.0.0.1", 0)
    ws_port = ws_server.sockets[0].getsockname()[1]

    return _Upstream(
        http_url=f"http://127.0.0.1:{http_port}",
        ws_url=f"ws://127.0.0.1:{ws_port}",
        _http_server=http_server,
        _http_thread=http_thread,
        _ws_server=ws_server,
        ws_requests=ws_requests,
    )


class _CountingSocketHandler(CopilotWebSocketForwarder):
    """Forwarding WebSocket handler that counts messages in both directions."""

    def __init__(self, ctx: CopilotRequestContext, url: str, counters: _Counters) -> None:
        super().__init__(ctx, url=url)
        self._counters = counters

    async def send_request_message(self, data: str | bytes) -> None:
        self._counters.ws_request_messages += 1
        await super().send_request_message(data)

    async def send_response_message(self, data: str | bytes) -> None:
        self._counters.ws_response_messages += 1
        await super().send_response_message(data)


class _TestHandler(CopilotRequestHandler):
    def __init__(self, upstream: _Upstream, counters: _Counters) -> None:
        self._upstream = upstream
        self._counters = counters
        self._client = httpx.AsyncClient(timeout=None, follow_redirects=False)

    def _rewrite_http(self, url: httpx.URL) -> httpx.URL:
        up = httpx.URL(self._upstream.http_url)
        return url.copy_with(scheme=up.scheme, host=up.host, port=up.port)

    def _rewrite_ws(self, url: str) -> str:
        parsed = httpx.URL(url)
        up = httpx.URL(self._upstream.ws_url)
        return str(parsed.copy_with(scheme=up.scheme, host=up.host, port=up.port))

    async def send_request(
        self, request: httpx.Request, ctx: CopilotRequestContext
    ) -> httpx.Response:
        self._counters.http_requests += 1
        headers = dict(request.headers)
        headers["x-test-mutated"] = "1"
        rewritten = httpx.Request(
            request.method,
            self._rewrite_http(request.url),
            headers=headers,
            content=request.content,
        )
        response = await self._client.send(rewritten, stream=True)
        self._counters.http_responses += 1
        response.headers["x-test-response-mutated"] = "1"
        return response

    async def open_websocket(self, ctx: CopilotRequestContext):
        return _CountingSocketHandler(ctx, self._rewrite_ws(ctx.url), self._counters)

    async def aclose(self) -> None:
        await self._client.aclose()


@dataclass
class _HandlerFixture:
    client: CopilotClient
    upstream: _Upstream
    counters: _Counters


@pytest_asyncio.fixture(loop_scope="module")
async def handler_fixture(ctx: E2ETestContext):
    upstream = await _start_fake_upstream()
    counters = _Counters()
    handler = _TestHandler(upstream, counters)
    github_token = (
        "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
    )
    env = {**ctx.get_env(), "COPILOT_EXP_COPILOT_CLI_WEBSOCKET_RESPONSES": "true"}
    client = CopilotClient(
        connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
        working_directory=ctx.work_dir,
        env=env,
        github_token=github_token,
        request_handler=handler,
    )
    try:
        yield _HandlerFixture(client=client, upstream=upstream, counters=counters)
    finally:
        try:
            await client.stop()
        except Exception:
            # Best-effort teardown during fixture cleanup.
            pass
        await handler.aclose()
        await upstream.close()


class TestCopilotRequestHandler:
    async def test_services_http_and_websocket_via_one_handler(self, handler_fixture):
        fx = handler_fixture
        await fx.client.start()
        session = await fx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        text = ""
        try:
            result = await session.send_and_wait("Say OK.")
            text = assistant_text(result)
        finally:
            await session.disconnect()

        # The HTTP seam fired — the runtime issued model-layer GETs (catalog,
        # policy) and possibly a single-shot inference through send_request.
        assert fx.counters.http_requests > 0, "expected send_request to fire"
        assert fx.counters.http_responses > 0, "expected send_request response mutation to fire"

        # The WebSocket seam fired — the main agent turn went over the WS path
        # and we observed messages in both directions.
        assert fx.counters.ws_request_messages > 0, "expected runtime → upstream ws messages"
        assert fx.counters.ws_response_messages > 0, "expected upstream → runtime ws messages"
        assert fx.upstream.ws_request_count > 0, "expected upstream WS to receive request messages"

        # Validate the final assistant response arrived (guards against truncated captures)
        assert "OK from synthetic" in text and "upstream" in text
