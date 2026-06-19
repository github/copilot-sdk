# --------------------------------------------------------------------------------------------
#  Copyright (c) Microsoft Corporation. All rights reserved.
# --------------------------------------------------------------------------------------------

"""Idiomatic, httpx-based base class for servicing LLM inference requests.

Most consumers subclass :class:`LlmRequestHandler` and override a single seam:

* HTTP — override :meth:`LlmRequestHandler.send_request` to mutate the
  :class:`httpx.Request`, post-process the :class:`httpx.Response`, or replace
  the call entirely. The default forwards via a shared :class:`httpx.AsyncClient`.
* WebSocket — override :meth:`LlmRequestHandler.open_web_socket` to return a
  per-connection :class:`CopilotWebSocketHandler`. The default opens a
  transparent forwarding connection.

Consumers who need full control can instead override
:meth:`LlmRequestHandler.on_llm_request` and drive the low-level
:class:`~copilot.llm_inference_provider.LlmInferenceRequest` directly.
"""

from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator
from dataclasses import dataclass, field
from typing import TYPE_CHECKING, Any

from .llm_inference_provider import (
    LlmInferenceHeaders,
    LlmInferenceProvider,
    LlmInferenceRequest,
    LlmInferenceResponseInit,
    LlmInferenceResponseSink,
)

if TYPE_CHECKING:
    import httpx


# Hop-by-hop and length headers the transport recomputes; forwarding them
# verbatim corrupts the request.
_FORBIDDEN_REQUEST_HEADERS = frozenset(
    {
        "host",
        "connection",
        "content-length",
        "transfer-encoding",
        "keep-alive",
        "upgrade",
        "proxy-connection",
        "te",
        "trailer",
    }
)

_shared_http_client: "httpx.AsyncClient | None" = None


def _get_shared_http_client() -> "httpx.AsyncClient":
    global _shared_http_client
    if _shared_http_client is None:
        import httpx

        _shared_http_client = httpx.AsyncClient(timeout=None, follow_redirects=False)
    return _shared_http_client


@dataclass
class LlmRequestContext:
    """Per-request context handed to every :class:`LlmRequestHandler` hook."""

    request_id: str
    transport: str
    url: str
    headers: LlmInferenceHeaders
    cancel_event: asyncio.Event
    session_id: str | None = None
    _bridge: "_LlmWebSocketResponseBridge | None" = field(default=None, repr=False)


@dataclass
class LlmWebSocketCloseStatus:
    """Terminal status for a callback-owned WebSocket connection."""

    description: str | None = None
    error_code: str | None = None
    error: BaseException | None = None

    @classmethod
    def normal_closure(cls) -> "LlmWebSocketCloseStatus":
        return cls()


class CopilotWebSocketHandler:
    """Per-connection WebSocket handler returned by :meth:`LlmRequestHandler.open_web_socket`.

    Subclass and override :meth:`send_request_message` (runtime → upstream) to
    mutate, drop, or inject messages, and :meth:`send_response_message`
    (upstream → runtime) for the reverse direction. A full transport
    replacement overrides :meth:`open` to stand up its own connection and
    receive loop.
    """

    def __init__(self, context: LlmRequestContext) -> None:
        bridge = context._bridge
        if bridge is None:
            raise RuntimeError("WebSocket response bridge is not attached")
        self.context = context
        self._response = bridge
        self._completion: asyncio.Future[LlmWebSocketCloseStatus] = (
            asyncio.get_event_loop().create_future()
        )
        self._closed = False
        self._suppress_close_on_dispose = False

    async def send_response_message(self, data: str | bytes) -> None:
        """Forward an upstream message to the runtime response."""
        await self._response.write(data)

    async def send_request_message(self, data: str | bytes) -> None:
        """Forward a runtime message to the upstream connection. Override to mutate."""
        raise NotImplementedError

    async def close(self, status: LlmWebSocketCloseStatus | None = None) -> None:
        """Initiate close: end the runtime response and resolve completion."""
        if self._closed:
            return
        self._closed = True
        status = status or LlmWebSocketCloseStatus.normal_closure()
        if status.error is not None:
            await self._response.error(
                status.description or str(status.error), status.error_code
            )
        else:
            await self._response.end()
        if not self._completion.done():
            self._completion.set_result(status)

    async def open(self) -> None:
        """Establish the connection. Default is a no-op for custom transports."""

    async def aclose(self) -> None:
        """Final resource cleanup; closes normally if not already closed."""
        if not self._suppress_close_on_dispose and not self._closed:
            await self.close(LlmWebSocketCloseStatus.normal_closure())


class ForwardingWebSocketHandler(CopilotWebSocketHandler):
    """Default pass-through WebSocket handler backed by the ``websockets`` library."""

    def __init__(self, context: LlmRequestContext, url: str | None = None) -> None:
        super().__init__(context)
        self._url = url or context.url
        self._upstream: Any | None = None
        self._receive_task: asyncio.Task[None] | None = None

    async def send_request_message(self, data: str | bytes) -> None:
        if self._upstream is None:
            return
        await self._upstream.send(data)

    async def open(self) -> None:
        if self._upstream is not None:
            return
        try:
            import websockets
        except ImportError as exc:  # pragma: no cover - optional dependency
            raise RuntimeError(
                "WebSocket forwarding requires the 'websockets' package. "
                "Install it or override open_web_socket()."
            ) from exc

        headers = [
            (name, value)
            for name, values in self.context.headers.items()
            if name.lower() not in _FORBIDDEN_REQUEST_HEADERS
            for value in (values or [])
        ]
        self._upstream = await websockets.connect(self._url, additional_headers=headers)
        self._receive_task = asyncio.create_task(self._receive_loop())

    async def _receive_loop(self) -> None:
        try:
            async for message in self._upstream:  # type: ignore[union-attr]
                await self.send_response_message(message)
            await self.close(LlmWebSocketCloseStatus.normal_closure())
        except asyncio.CancelledError:
            raise
        except Exception as exc:
            await self.close(LlmWebSocketCloseStatus(description=str(exc), error=exc))

    async def close(self, status: LlmWebSocketCloseStatus | None = None) -> None:
        if self._upstream is not None:
            try:
                await self._upstream.close()
            except Exception:
                # Best-effort; the socket may already be closed.
                pass
        await super().close(status)

    async def aclose(self) -> None:
        try:
            await super().aclose()
        finally:
            if self._receive_task is not None:
                self._receive_task.cancel()
            if self._upstream is not None:
                try:
                    await self._upstream.close()
                except Exception:
                    pass


class LlmRequestHandler(LlmInferenceProvider):
    """Base class for consumers that observe or replace LLM inference requests."""

    async def on_llm_request(self, request: LlmInferenceRequest) -> None:
        bridge = _LlmWebSocketResponseBridge(request.response_body)
        ctx = LlmRequestContext(
            request_id=request.request_id,
            session_id=request.session_id,
            transport=request.transport,
            url=request.url,
            headers=request.headers,
            cancel_event=request.cancel_event,
            _bridge=bridge,
        )
        if request.transport == "websocket":
            await self._handle_web_socket(request, ctx)
        else:
            await self._handle_http(request, ctx)

    async def send_request(self, request: "httpx.Request", ctx: LlmRequestContext) -> "httpx.Response":
        """Send an HTTP request. Override to mutate request/response or replace the call."""
        return await _get_shared_http_client().send(request, stream=True)

    async def open_web_socket(self, ctx: LlmRequestContext) -> CopilotWebSocketHandler:
        """Open a per-connection WebSocket handler. Override to mutate or replace."""
        return ForwardingWebSocketHandler(ctx)

    async def _handle_http(self, req: LlmInferenceRequest, ctx: LlmRequestContext) -> None:
        request = await _build_httpx_request(req)
        await _run_cancellable(
            self._forward_http(request, req, ctx), req.cancel_event
        )

    async def _forward_http(
        self, request: "httpx.Request", req: LlmInferenceRequest, ctx: LlmRequestContext
    ) -> None:
        response = await self.send_request(request, ctx)
        try:
            await _stream_response_to_sink(response, req)
        finally:
            await response.aclose()

    async def _handle_web_socket(self, req: LlmInferenceRequest, ctx: LlmRequestContext) -> None:
        handler = await self.open_web_socket(ctx)
        assert ctx._bridge is not None
        try:
            await handler.open()
            await ctx._bridge.start()

            async def pump_client() -> str:
                async for chunk in req.request_body:
                    await handler.send_request_message(_decode_frame(chunk))
                return "client-complete"

            client_task = asyncio.create_task(pump_client())
            completion = asyncio.ensure_future(handler._completion)
            done, _ = await asyncio.wait(
                {client_task, completion}, return_when=asyncio.FIRST_COMPLETED
            )

            if client_task in done and client_task.exception() is not None:
                handler._suppress_close_on_dispose = True
                raise client_task.exception()  # type: ignore[misc]

            if client_task in done:
                await handler.close(LlmWebSocketCloseStatus.normal_closure())
                await handler._completion
                return

            status = await handler._completion
            if status.error is not None:
                raise status.error
        finally:
            await handler.aclose()


async def _run_cancellable(coro: Any, cancel_event: asyncio.Event) -> None:
    """Run ``coro`` but abort it (and raise) when ``cancel_event`` fires."""
    task = asyncio.ensure_future(coro)
    waiter = asyncio.ensure_future(cancel_event.wait())
    try:
        done, _ = await asyncio.wait(
            {task, waiter}, return_when=asyncio.FIRST_COMPLETED
        )
        if task in done:
            exc = task.exception()
            if exc is not None:
                raise exc
            return
        # Cancellation fired first.
        task.cancel()
        try:
            await task
        except (asyncio.CancelledError, Exception):
            pass
        raise RuntimeError("Request cancelled by runtime")
    finally:
        if not waiter.done():
            waiter.cancel()


async def _build_httpx_request(req: LlmInferenceRequest) -> "httpx.Request":
    import httpx

    header_pairs = [
        (name, value)
        for name, values in req.headers.items()
        if name.lower() not in _FORBIDDEN_REQUEST_HEADERS
        for value in (values or [])
    ]
    method = req.method.upper()
    has_body = method not in ("GET", "HEAD")
    body = await _drain_async(req.request_body)
    content = body if (has_body and body) else None
    return httpx.Request(method, req.url, headers=header_pairs, content=content)


async def _drain_async(stream: AsyncIterator[bytes]) -> bytes:
    parts: list[bytes] = []
    async for chunk in stream:
        if chunk:
            parts.append(chunk)
    return b"".join(parts)


async def _stream_response_to_sink(response: "httpx.Response", req: LlmInferenceRequest) -> None:
    await req.response_body.start(
        LlmInferenceResponseInit(
            status=response.status_code,
            status_text=response.reason_phrase or None,
            headers=_headers_to_multi_map(response.headers),
        )
    )
    async for chunk in response.aiter_raw():
        if chunk:
            await req.response_body.write(chunk)
    await req.response_body.end()


def _headers_to_multi_map(headers: Any) -> LlmInferenceHeaders:
    out: dict[str, list[str]] = {}
    for name, value in headers.multi_items():
        out.setdefault(name, []).append(value)
    return out


def _decode_frame(chunk: bytes) -> str:
    return chunk.decode("utf-8", errors="replace")


class _LlmWebSocketResponseBridge:
    """Serialises WebSocket response writes into the sink, buffering until start."""

    def __init__(self, sink: LlmInferenceResponseSink) -> None:
        self._sink = sink
        self._pending: list[Any] = []
        self._started = False
        self._completed = False
        self._lock = asyncio.Lock()

    async def start(self) -> None:
        async with self._lock:
            if self._started:
                return
            self._started = True
            await self._sink.start(LlmInferenceResponseInit(status=101, headers={}))
            pending = self._pending
            self._pending = []
        for action in pending:
            await action()

    async def write(self, data: str | bytes) -> None:
        async def action() -> None:
            if not self._completed:
                await self._sink.write(data)

        await self._enqueue_or_buffer(action)

    async def end(self) -> None:
        async def action() -> None:
            if self._completed:
                return
            self._completed = True
            await self._sink.end()

        await self._enqueue_or_buffer(action)

    async def error(self, message: str, code: str | None = None) -> None:
        async def action() -> None:
            if self._completed:
                return
            self._completed = True
            await self._sink.error(message, code)

        await self._enqueue_or_buffer(action)

    async def _enqueue_or_buffer(self, action: Any) -> None:
        if not self._started:
            self._pending.append(action)
            return
        async with self._lock:
            await action()
