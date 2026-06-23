# --------------------------------------------------------------------------------------------
#  Copyright (c) Microsoft Corporation. All rights reserved.
# --------------------------------------------------------------------------------------------

"""CopilotRequestHandler: observe or replace outbound model-layer HTTP/WebSocket requests.

The SDK consumer subclasses :class:`CopilotRequestHandler` and overrides one or
both seams:

* HTTP — override :meth:`CopilotRequestHandler.send_request` to mutate the
  :class:`httpx.Request`, post-process the :class:`httpx.Response`, or replace
  the call entirely. The default forwards via a shared :class:`httpx.AsyncClient`.
* WebSocket — override :meth:`CopilotRequestHandler.open_websocket` to return
  a per-connection :class:`CopilotWebSocketHandler`. The default opens a
  transparent forwarding connection via the ``websockets`` library.

:func:`create_copilot_request_adapter` converts a handler into the generated
:class:`~copilot.generated.rpc.LlmInferenceHandler` shape so the RPC dispatcher
can route inbound ``httpRequestStart`` / ``httpRequestChunk`` frames through it.
"""

from __future__ import annotations

import asyncio
import base64
from collections.abc import AsyncIterator, Callable
from dataclasses import dataclass, field
from typing import TYPE_CHECKING, Any

from .generated.rpc import (
    LlmInferenceHTTPRequestChunkRequest,
    LlmInferenceHTTPRequestChunkResult,
    LlmInferenceHTTPRequestStartRequest,
    LlmInferenceHTTPRequestStartResult,
    LlmInferenceHTTPResponseChunkError,
    LlmInferenceHTTPResponseChunkRequest,
    LlmInferenceHTTPResponseStartRequest,
    ServerLlmInferenceApi,
)

if TYPE_CHECKING:
    import httpx

# Multi-valued headers: header name → list of values.
LlmInferenceHeaders = dict[str, list[str]]

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

_shared_http_client: httpx.AsyncClient | None = None


def _get_shared_http_client() -> httpx.AsyncClient:
    global _shared_http_client
    if _shared_http_client is None:
        import httpx

        _shared_http_client = httpx.AsyncClient(timeout=None, follow_redirects=False)
    return _shared_http_client


@dataclass
class CopilotRequestContext:
    """Per-request context handed to every :class:`CopilotRequestHandler` hook."""

    request_id: str
    """Opaque runtime-minted id, stable across the request lifecycle."""

    transport: str
    """``"http"`` (plain HTTP / SSE) or ``"websocket"`` (full-duplex channel)."""

    url: str
    """Absolute request URL."""

    headers: LlmInferenceHeaders
    """HTTP request headers, multi-valued."""

    cancel_event: asyncio.Event
    """Set when the runtime cancels this in-flight request. Pass it through to
    your transport so the upstream call is torn down too."""

    session_id: str | None = None
    """Id of the runtime session that triggered this request, when in scope.
    Absent for out-of-session requests (e.g. the startup model catalog)."""

    _bridge: _CopilotWebSocketResponseBridge | None = field(default=None, repr=False)


@dataclass
class CopilotWebSocketCloseStatus:
    """Terminal status for a callback-owned WebSocket connection."""

    description: str | None = None
    error_code: str | None = None
    error: BaseException | None = None

    @classmethod
    def normal_closure(cls) -> CopilotWebSocketCloseStatus:
        return cls()


class CopilotWebSocketHandler:
    """Per-connection WebSocket handler returned by
    :meth:`CopilotRequestHandler.open_websocket`.

    Subclass and override :meth:`send_request_message` (runtime → upstream) to
    mutate, drop, or inject messages, and :meth:`send_response_message`
    (upstream → runtime) for the reverse direction. A full transport replacement
    overrides :meth:`open` to stand up its own connection and receive loop.
    """

    def __init__(self, context: CopilotRequestContext) -> None:
        bridge = context._bridge
        if bridge is None:
            raise RuntimeError("WebSocket response bridge is not attached")
        self.context = context
        self._response = bridge
        self._completion: asyncio.Future[CopilotWebSocketCloseStatus] = (
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

    async def close(self, status: CopilotWebSocketCloseStatus | None = None) -> None:
        """Initiate close: end the runtime response and resolve completion."""
        if self._closed:
            return
        self._closed = True
        status = status or CopilotWebSocketCloseStatus.normal_closure()
        if status.error is not None:
            await self._response.error(status.description or str(status.error), status.error_code)
        else:
            await self._response.end()
        if not self._completion.done():
            self._completion.set_result(status)

    async def open(self) -> None:
        """Establish the connection. Default is a no-op for custom transports."""

    async def aclose(self) -> None:
        """Final resource cleanup; closes normally if not already closed."""
        if not self._suppress_close_on_dispose and not self._closed:
            await self.close(CopilotWebSocketCloseStatus.normal_closure())


class CopilotWebSocketForwarder(CopilotWebSocketHandler):
    """Default pass-through WebSocket handler backed by the ``websockets`` library."""

    def __init__(self, context: CopilotRequestContext, url: str | None = None) -> None:
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
                "Install it or override open_websocket()."
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
            await self.close(CopilotWebSocketCloseStatus.normal_closure())
        except asyncio.CancelledError:
            raise
        except Exception as exc:
            await self.close(CopilotWebSocketCloseStatus(description=str(exc), error=exc))

    async def close(self, status: CopilotWebSocketCloseStatus | None = None) -> None:
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
                    # Best-effort teardown: the upstream may already be closed.
                    pass


class CopilotRequestHandler:
    """Base class for consumers that observe or replace LLM inference requests.

    Override :meth:`send_request` to intercept HTTP model-layer requests, or
    :meth:`open_websocket` to intercept WebSocket connections. An instance
    that overrides nothing is a transparent pass-through.
    """

    async def send_request(
        self, request: httpx.Request, ctx: CopilotRequestContext
    ) -> httpx.Response:
        """Send an HTTP request. Override to mutate request/response or replace the call."""
        return await _get_shared_http_client().send(request, stream=True)

    async def open_websocket(self, ctx: CopilotRequestContext) -> CopilotWebSocketHandler:
        """Open a per-connection WebSocket handler. Override to mutate or replace."""
        return CopilotWebSocketForwarder(ctx)

    async def _dispatch(self, exchange: _CopilotRequestExchange) -> None:
        bridge = _CopilotWebSocketResponseBridge(exchange)
        ctx = CopilotRequestContext(
            request_id=exchange.request_id,
            session_id=exchange.session_id,
            transport=exchange.transport,
            url=exchange.url,
            headers=exchange.headers,
            cancel_event=exchange.cancel_event,
            _bridge=bridge,
        )
        if exchange.transport == "websocket":
            await self._handle_web_socket(exchange, ctx)
        else:
            await self._handle_http(exchange, ctx)

    async def _handle_http(
        self, exchange: _CopilotRequestExchange, ctx: CopilotRequestContext
    ) -> None:
        request = await _build_httpx_request(exchange)
        await _run_cancellable(self._forward_http(request, exchange, ctx), exchange.cancel_event)

    async def _forward_http(
        self,
        request: httpx.Request,
        exchange: _CopilotRequestExchange,
        ctx: CopilotRequestContext,
    ) -> None:
        response = await self.send_request(request, ctx)
        try:
            await _stream_response_to_exchange(response, exchange)
        finally:
            await response.aclose()

    async def _handle_web_socket(
        self, exchange: _CopilotRequestExchange, ctx: CopilotRequestContext
    ) -> None:
        handler = await self.open_websocket(ctx)
        assert ctx._bridge is not None
        try:
            await handler.open()
            # Emit the 101 upgrade head eagerly. The runtime blocks the WS
            # connect until it receives this acknowledgement, and only then
            # starts forwarding inbound messages as request-body chunks.
            # Waiting for the first upstream message would deadlock.
            await ctx._bridge.start()

            async def pump_client() -> str:
                async for chunk in exchange.request_body:
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
                await handler.close(CopilotWebSocketCloseStatus.normal_closure())
                await handler._completion
                return

            status = await handler._completion
            if status.error is not None:
                raise status.error
        finally:
            await handler.aclose()


# ---------------------------------------------------------------------------
# Internal exchange: request body feed + response emitter
# ---------------------------------------------------------------------------


@dataclass
class _BodyItem:
    chunk: bytes | None = None
    end: bool = False
    cancel: bool = False
    cancel_reason: str | None = None


class _BodyQueue:
    """An async iterator of request-body byte chunks fed by the runtime."""

    def __init__(self) -> None:
        self._queue: asyncio.Queue[_BodyItem] = asyncio.Queue()
        self._done = False

    def push(self, item: _BodyItem) -> None:
        self._queue.put_nowait(item)

    def __aiter__(self) -> AsyncIterator[bytes]:
        return self

    async def __anext__(self) -> bytes:
        if self._done:
            raise StopAsyncIteration
        item = await self._queue.get()
        if item.cancel:
            self._done = True
            reason = (
                f"Request cancelled by runtime: {item.cancel_reason}"
                if item.cancel_reason
                else "Request cancelled by runtime"
            )
            raise RuntimeError(reason)
        if item.end:
            self._done = True
            raise StopAsyncIteration
        return item.chunk if item.chunk is not None else b""


class _CopilotRequestExchange:
    """One intercepted request in flight.

    Carries the request body stream the runtime feeds via ``httpRequestChunk``
    frames, and emits the handler's response directly to the runtime through
    the generated ``llmInference`` RPC. Replaces the former provider / sink /
    response-channel indirection with a single object the adapter owns.
    """

    def __init__(
        self,
        request_id: str,
        get_server_rpc: Callable[[], ServerLlmInferenceApi | None],
    ) -> None:
        self.request_id = request_id
        self.session_id: str | None = None
        self.method: str = "GET"
        self.url: str = ""
        self.headers: dict[str, list[str]] = {}
        self.transport: str = "http"
        self._get_server_rpc = get_server_rpc
        self._queue = _BodyQueue()
        self.cancel_event: asyncio.Event = asyncio.Event()
        self.started: bool = False
        self.finished: bool = False
        self.cancelled: bool = False
        self.task: asyncio.Task[None] | None = None

    def set_context(self, params: LlmInferenceHTTPRequestStartRequest) -> None:
        """Fill in the request context once the matching start frame arrives."""
        self.session_id = params.session_id
        self.method = params.method
        self.url = params.url
        self.headers = params.headers
        transport = params.transport
        self.transport = transport.value if transport is not None else "http"

    @property
    def request_body(self) -> _BodyQueue:
        return self._queue

    def _require_rpc(self) -> ServerLlmInferenceApi:
        rpc = self._get_server_rpc()
        if rpc is None:
            raise RuntimeError("Copilot request response used after RPC connection closed.")
        return rpc

    async def start_response(
        self,
        status: int,
        status_text: str | None = None,
        headers: LlmInferenceHeaders | None = None,
    ) -> None:
        if self.started:
            raise RuntimeError("Copilot request response start() called twice.")
        if self.finished:
            raise RuntimeError("Copilot request response already finished.")
        self.started = True
        await self._require_rpc().http_response_start(
            LlmInferenceHTTPResponseStartRequest(
                headers=headers or {},
                request_id=self.request_id,
                status=status,
                status_text=status_text,
            )
        )

    async def write_response(self, data: str | bytes) -> None:
        if self.cancelled:
            raise RuntimeError("Copilot request was cancelled by the runtime.")
        if not self.started:
            raise RuntimeError("Copilot request response write() called before start().")
        if self.finished:
            raise RuntimeError("Copilot request response write() called after end()/error().")
        is_binary = isinstance(data, (bytes, bytearray))
        payload = base64.b64encode(bytes(data)).decode("ascii") if is_binary else str(data)
        await self._require_rpc().http_response_chunk(
            LlmInferenceHTTPResponseChunkRequest(
                data=payload,
                request_id=self.request_id,
                binary=is_binary or None,
                end=False,
            )
        )

    async def end_response(self) -> None:
        if self.finished:
            return
        self.finished = True
        await self._require_rpc().http_response_chunk(
            LlmInferenceHTTPResponseChunkRequest(data="", request_id=self.request_id, end=True)
        )

    async def error_response(self, message: str, code: str | None = None) -> None:
        if self.finished:
            return
        self.finished = True
        await self._require_rpc().http_response_chunk(
            LlmInferenceHTTPResponseChunkRequest(
                data="",
                request_id=self.request_id,
                end=True,
                error=LlmInferenceHTTPResponseChunkError(message=message, code=code),
            )
        )


# ---------------------------------------------------------------------------
# Adapter: wires the handler into the generated RPC handler shape
# ---------------------------------------------------------------------------


def create_copilot_request_adapter(
    handler: CopilotRequestHandler,
    get_server_rpc: Callable[[], ServerLlmInferenceApi | None],
) -> _CopilotRequestAdapterHandler:
    """Adapt a :class:`CopilotRequestHandler` into the generated handler shape.

    Maintains a per-``request_id`` table of :class:`_CopilotRequestExchange`:
    each ``httpRequestStart`` allocates one and fires the handler in the
    background, returning immediately so the runtime's RPC reply is not gated
    on the consumer's I/O. Subsequent ``httpRequestChunk`` frames are routed
    into the matching exchange's body stream.
    """
    return _CopilotRequestAdapterHandler(handler, get_server_rpc)


class _CopilotRequestAdapterHandler:
    def __init__(
        self,
        handler: CopilotRequestHandler,
        get_server_rpc: Callable[[], ServerLlmInferenceApi | None],
    ) -> None:
        self._handler = handler
        self._get_server_rpc = get_server_rpc
        self._pending: dict[str, _CopilotRequestExchange] = {}

    def _route_chunk(
        self,
        exchange: _CopilotRequestExchange,
        params: LlmInferenceHTTPRequestChunkRequest,
    ) -> None:
        if params.cancel:
            exchange.cancelled = True
            exchange.cancel_event.set()
            exchange._queue.push(_BodyItem(cancel=True, cancel_reason=params.cancel_reason))
            return
        if params.data:
            exchange._queue.push(
                _BodyItem(chunk=_decode_chunk_data(params.data, bool(params.binary)))
            )
        if params.end:
            exchange._queue.push(_BodyItem(end=True))

    async def _run(self, exchange: _CopilotRequestExchange) -> None:
        try:
            await self._handler._dispatch(exchange)
            if not exchange.finished:
                await _finalize(
                    exchange,
                    502,
                    "Copilot request handler returned without finalising the response.",
                )
        except Exception as exc:
            if exchange.cancelled or exchange.cancel_event.is_set():
                await _finalize(exchange, 499, "Request cancelled by runtime", "cancelled")
                return
            await _finalize(exchange, 502, str(exc))
        finally:
            self._pending.pop(exchange.request_id, None)

    def _get_or_create(self, request_id: str) -> _CopilotRequestExchange:
        # The runtime dispatches httpRequestStart and httpRequestChunk frames
        # independently. get-or-create keeps the adapter correct regardless of
        # arrival order: a body chunk (including the terminal end frame) that
        # races ahead of its start frame is buffered into the same exchange
        # rather than dropped, which would otherwise hang the body drain.
        exchange = self._pending.get(request_id)
        if exchange is None:
            exchange = _CopilotRequestExchange(request_id, self._get_server_rpc)
            self._pending[request_id] = exchange
        return exchange

    async def http_request_start(
        self, params: LlmInferenceHTTPRequestStartRequest
    ) -> LlmInferenceHTTPRequestStartResult:
        # Adopt any exchange a racing chunk already created — with its buffered
        # body — rather than dropping those frames.
        exchange = self._get_or_create(params.request_id)
        exchange.set_context(params)
        exchange.task = asyncio.create_task(self._run(exchange))
        return LlmInferenceHTTPRequestStartResult()

    async def http_request_chunk(
        self, params: LlmInferenceHTTPRequestChunkRequest
    ) -> LlmInferenceHTTPRequestChunkResult:
        # May arrive before the matching start frame; get-or-create so the body
        # is buffered, never lost.
        exchange = self._get_or_create(params.request_id)
        self._route_chunk(exchange, params)
        return LlmInferenceHTTPRequestChunkResult()


async def _finalize(
    exchange: _CopilotRequestExchange,
    status: int,
    message: str,
    code: str | None = None,
) -> None:
    if exchange.finished:
        return
    try:
        if not exchange.started:
            await exchange.start_response(status)
        await exchange.error_response(message, code)
    except Exception:
        # Best-effort — the connection may already be dead.
        pass


# ---------------------------------------------------------------------------
# WebSocket response bridge
# ---------------------------------------------------------------------------


class _CopilotWebSocketResponseBridge:
    """Serialises WebSocket response writes into the exchange.

    The 101 upgrade head is emitted eagerly via :meth:`start` (the runtime
    gates the WS connect on it); subsequent writes and the terminal frame are
    serialised via a lock so the head always precedes them. The lazy-start
    path in :meth:`write` acts as a no-op backstop when ``start`` is called
    first (the normal case).
    """

    def __init__(self, exchange: _CopilotRequestExchange) -> None:
        self._exchange = exchange
        self._started = False
        self._completed = False
        self._lock = asyncio.Lock()

    async def start(self) -> None:
        """Emit the 101 upgrade acknowledgement now."""
        async with self._lock:
            if self._started:
                return
            self._started = True
            await self._exchange.start_response(101, headers={})

    async def write(self, data: str | bytes) -> None:
        async with self._lock:
            if not self._started:
                # Lazy-start backstop: emits the 101 head if a subclass calls
                # write before start(). In normal usage start() is called
                # eagerly in _handle_web_socket so this branch is never taken.
                self._started = True
                await self._exchange.start_response(101, headers={})
            if not self._completed:
                await self._exchange.write_response(data)

    async def end(self) -> None:
        async with self._lock:
            if self._completed:
                return
            self._completed = True
            await self._exchange.end_response()

    async def error(self, message: str, code: str | None = None) -> None:
        async with self._lock:
            if self._completed:
                return
            self._completed = True
            await self._exchange.error_response(message, code)


# ---------------------------------------------------------------------------
# HTTP helpers
# ---------------------------------------------------------------------------


async def _run_cancellable(coro: Any, cancel_event: asyncio.Event) -> None:
    """Run ``coro`` but abort it (and raise) when ``cancel_event`` fires."""
    task = asyncio.ensure_future(coro)
    waiter = asyncio.ensure_future(cancel_event.wait())
    try:
        done, _ = await asyncio.wait({task, waiter}, return_when=asyncio.FIRST_COMPLETED)
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
            # The awaited task was cancelled; its unwind exception is expected
            # and irrelevant — we raise the cancellation result below.
            pass
        raise RuntimeError("Request cancelled by runtime")
    finally:
        if not waiter.done():
            waiter.cancel()


async def _build_httpx_request(exchange: _CopilotRequestExchange) -> httpx.Request:
    import httpx

    header_pairs = [
        (name, value)
        for name, values in exchange.headers.items()
        if name.lower() not in _FORBIDDEN_REQUEST_HEADERS
        for value in (values or [])
    ]
    method = exchange.method.upper()
    has_body = method not in ("GET", "HEAD")
    body = await _drain_async(exchange.request_body)
    content = body if (has_body and body) else None
    return httpx.Request(method, exchange.url, headers=header_pairs, content=content)


async def _drain_async(stream: AsyncIterator[bytes]) -> bytes:
    parts: list[bytes] = []
    async for chunk in stream:
        if chunk:
            parts.append(chunk)
    return b"".join(parts)


async def _stream_response_to_exchange(
    response: httpx.Response, exchange: _CopilotRequestExchange
) -> None:
    await exchange.start_response(
        response.status_code,
        status_text=response.reason_phrase or None,
        headers=_headers_to_multi_map(response.headers),
    )
    if response.is_stream_consumed:
        # An in-memory response (built with ``content=``) has already buffered its
        # body, so its raw stream cannot be iterated; forward the buffered bytes.
        body = response.content
        if body:
            await exchange.write_response(body)
    else:
        async for chunk in response.aiter_raw():
            if chunk:
                await exchange.write_response(chunk)
    await exchange.end_response()


def _headers_to_multi_map(headers: Any) -> LlmInferenceHeaders:
    out: dict[str, list[str]] = {}
    for name, value in headers.multi_items():
        out.setdefault(name, []).append(value)
    return out


def _decode_chunk_data(data: str, binary: bool) -> bytes:
    if binary:
        return base64.b64decode(data)
    return data.encode("utf-8")


def _decode_frame(chunk: bytes) -> str:
    return chunk.decode("utf-8", errors="replace")
