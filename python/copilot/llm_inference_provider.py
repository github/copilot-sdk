# --------------------------------------------------------------------------------------------
#  Copyright (c) Microsoft Corporation. All rights reserved.
# --------------------------------------------------------------------------------------------

"""Low-level LLM inference provider types and the RPC adapter.

The SDK consumer implements :class:`LlmInferenceProvider` (usually by
subclassing the idiomatic :class:`~copilot.llm_request_handler.LlmRequestHandler`).
:func:`create_llm_inference_adapter` converts a provider into an object that
conforms to the generated :class:`~copilot.generated.rpc.LlmInferenceHandler`
protocol, wiring the inbound ``httpRequestStart`` / ``httpRequestChunk`` frames
into the provider and translating the provider's response writes back into
outbound ``httpResponseStart`` / ``httpResponseChunk`` RPCs.
"""

from __future__ import annotations

import asyncio
import base64
from collections.abc import AsyncIterator, Awaitable, Callable
from dataclasses import dataclass, field
from typing import Protocol, runtime_checkable

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

# Headers are multi-valued: a header name maps to a list of values.
LlmInferenceHeaders = dict[str, list[str]]


@dataclass
class LlmInferenceResponseInit:
    """Response head passed to :meth:`LlmInferenceResponseSink.start`."""

    status: int
    status_text: str | None = None
    headers: LlmInferenceHeaders | None = None


@runtime_checkable
class LlmInferenceResponseSink(Protocol):
    """Sink the consumer writes the upstream response into.

    The state machine is strict: ``start`` once, then zero or more ``write``
    calls, finishing with exactly one of ``end`` or ``error``. Calling out of
    order raises.
    """

    async def start(self, init: LlmInferenceResponseInit) -> None:
        """Send the response head (status + headers) back to the runtime."""
        ...

    async def write(self, data: str | bytes) -> None:
        """Send a body chunk. ``str`` is encoded as UTF-8; ``bytes`` is sent as binary."""
        ...

    async def end(self) -> None:
        """Mark end-of-stream cleanly."""
        ...

    async def error(self, message: str, code: str | None = None) -> None:
        """Mark end-of-stream with a transport-level failure."""
        ...


@dataclass
class LlmInferenceRequest:
    """An outbound model-layer HTTP request the runtime is asking the SDK to handle.

    This is a low-level shape: URL / method / headers verbatim, body bytes
    delivered as an async iterator, response delivered through
    :attr:`response_body`. The runtime does not classify the request; consumers
    that need a provider type or endpoint kind derive it from the URL / headers.
    """

    request_id: str
    """Opaque runtime-minted id, stable across the request lifecycle."""

    method: str
    """HTTP method (``GET``, ``POST``, ...)."""

    url: str
    """Absolute URL."""

    headers: LlmInferenceHeaders
    """HTTP request headers, multi-valued."""

    transport: str
    """``"http"`` (plain HTTP / SSE) or ``"websocket"`` (full-duplex channel)."""

    request_body: AsyncIterator[bytes]
    """Request body bytes, yielded as they arrive. Empty bodies yield zero chunks."""

    cancel_event: asyncio.Event
    """Set when the runtime cancels this in-flight request. Pass it through to
    your transport so the upstream call is torn down too. After it fires, writes
    to :attr:`response_body` are ignored."""

    response_body: LlmInferenceResponseSink
    """Sink the consumer writes the upstream response into."""

    session_id: str | None = None
    """Id of the runtime session that triggered this request, when in scope.
    Absent for out-of-session requests (e.g. the startup model catalog)."""


@runtime_checkable
class LlmInferenceProvider(Protocol):
    """Interface for an LLM inference provider.

    The consumer implements :meth:`on_llm_request`. The same callback handles
    both buffered and streaming responses; the consumer just calls
    ``response_body.write`` zero or more times before ``end``.
    """

    async def on_llm_request(self, request: LlmInferenceRequest) -> None:
        """Service a single outbound LLM HTTP request.

        The consumer must eventually call either ``response_body.end()`` or
        ``response_body.error(...)``; failing to do so leaks runtime state.
        Raising surfaces a transport-level failure to the runtime.
        """
        ...


@dataclass
class LlmInferenceConfig:
    """Connection-level LLM inference callback configuration.

    Passed as the ``llm_inference`` client option. The ``handler`` is registered
    process-wide and invoked for every model-layer HTTP/WebSocket request the
    runtime would otherwise issue, for both BYOK and CAPI traffic.
    """

    handler: LlmInferenceProvider



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


@dataclass
class _PendingState:
    queue: _BodyQueue
    cancel_event: asyncio.Event
    started: bool = False
    finished: bool = False
    cancelled: bool = False
    task: asyncio.Task[None] | None = field(default=None)


def _decode_chunk_data(data: str, binary: bool) -> bytes:
    if binary:
        return base64.b64decode(data)
    return data.encode("utf-8")


class _RuntimeRejectedError(RuntimeError):
    """Raised when the runtime drops an in-flight request (``accepted: False``)."""


def create_llm_inference_adapter(
    provider: LlmInferenceProvider,
    get_server_rpc: Callable[[], ServerLlmInferenceApi | None],
) -> "_LlmInferenceAdapter":
    """Adapt an :class:`LlmInferenceProvider` into the generated handler shape.

    Maintains a per-``request_id`` state table: each ``http_request_start``
    allocates a body queue + response sink and fires ``provider.on_llm_request``
    in the background. Subsequent ``http_request_chunk`` frames are routed into
    the queue. The sink translates ``start`` / ``write`` / ``end`` / ``error``
    calls into outbound ``httpResponseStart`` / ``httpResponseChunk`` RPCs.

    ``http_request_start`` returns immediately after registering state so the
    runtime's RPC reply is not gated on the consumer's I/O.
    """
    return _LlmInferenceAdapter(provider, get_server_rpc)


class _LlmInferenceAdapter:
    def __init__(
        self,
        provider: LlmInferenceProvider,
        get_server_rpc: Callable[[], ServerLlmInferenceApi | None],
    ) -> None:
        self._provider = provider
        self._get_server_rpc = get_server_rpc
        self._pending: dict[str, _PendingState] = {}
        # Defense-in-depth backstop: chunks that arrive before their start frame
        # (a reordering the runtime's single ordered dispatch should make
        # impossible) are staged here and drained the moment the matching
        # http_request_start registers state, so a body byte is never dropped.
        self._staged: dict[str, list[LlmInferenceHTTPRequestChunkRequest]] = {}

    def _route_chunk(self, state: _PendingState, params: LlmInferenceHTTPRequestChunkRequest) -> None:
        if params.cancel:
            state.cancelled = True
            state.cancel_event.set()
            state.queue.push(_BodyItem(cancel=True, cancel_reason=params.cancel_reason))
            return
        if params.data:
            state.queue.push(_BodyItem(chunk=_decode_chunk_data(params.data, bool(params.binary))))
        if params.end:
            state.queue.push(_BodyItem(end=True))

    def _require_rpc(self) -> ServerLlmInferenceApi:
        rpc = self._get_server_rpc()
        if rpc is None:
            raise RuntimeError("LLM inference response sink used after RPC connection closed.")
        return rpc

    def _make_sink(self, request_id: str, state: _PendingState) -> LlmInferenceResponseSink:
        adapter = self

        def reject() -> None:
            # The runtime acknowledges every response frame with ``accepted``.
            # ``accepted: False`` means it has dropped the request, so we abort
            # the provider's upstream work and stop emitting.
            if not state.cancelled:
                state.cancelled = True
                state.cancel_event.set()
            state.finished = True
            adapter._pending.pop(request_id, None)
            raise _RuntimeRejectedError(
                "LLM inference response was rejected by the runtime (request no longer active)."
            )

        class _Sink:
            async def start(self, init: LlmInferenceResponseInit) -> None:
                if state.started:
                    raise RuntimeError("LLM inference response sink.start() called twice.")
                if state.finished:
                    raise RuntimeError("LLM inference response sink already finished.")
                state.started = True
                result = await adapter._require_rpc().http_response_start(
                    LlmInferenceHTTPResponseStartRequest(
                        headers=init.headers or {},
                        request_id=request_id,
                        status=init.status,
                        status_text=init.status_text,
                    )
                )
                if not result.accepted:
                    reject()

            async def write(self, data: str | bytes) -> None:
                if state.cancelled:
                    raise RuntimeError("LLM inference request was cancelled by the runtime.")
                if not state.started:
                    raise RuntimeError("LLM inference response sink.write() called before start().")
                if state.finished:
                    raise RuntimeError("LLM inference response sink.write() called after end()/error().")
                is_binary = isinstance(data, bytes | bytearray)
                payload = (
                    base64.b64encode(bytes(data)).decode("ascii")
                    if is_binary
                    else str(data)
                )
                result = await adapter._require_rpc().http_response_chunk(
                    LlmInferenceHTTPResponseChunkRequest(
                        data=payload,
                        request_id=request_id,
                        binary=is_binary or None,
                        end=False,
                    )
                )
                if not result.accepted:
                    reject()

            async def end(self) -> None:
                if state.finished:
                    return
                state.finished = True
                adapter._pending.pop(request_id, None)
                await adapter._require_rpc().http_response_chunk(
                    LlmInferenceHTTPResponseChunkRequest(data="", request_id=request_id, end=True)
                )

            async def error(self, message: str, code: str | None = None) -> None:
                if state.finished:
                    return
                state.finished = True
                adapter._pending.pop(request_id, None)
                await adapter._require_rpc().http_response_chunk(
                    LlmInferenceHTTPResponseChunkRequest(
                        data="",
                        request_id=request_id,
                        end=True,
                        error=LlmInferenceHTTPResponseChunkError(message=message, code=code),
                    )
                )

        return _Sink()

    async def _fail_via_sink(
        self, sink: LlmInferenceResponseSink, state: _PendingState, message: str
    ) -> None:
        if state.finished:
            return
        try:
            if not state.started:
                await sink.start(LlmInferenceResponseInit(status=502))
            await sink.error(message)
        except Exception:
            # Best-effort — the connection may already be dead.
            pass

    async def _finish_cancelled(self, sink: LlmInferenceResponseSink, state: _PendingState) -> None:
        if state.finished:
            return
        try:
            if not state.started:
                await sink.start(LlmInferenceResponseInit(status=499))
            await sink.error("Request cancelled by runtime", code="cancelled")
        except Exception:
            # Best-effort — the runtime already dropped the request on cancel.
            pass

    async def _run_provider(
        self, request: LlmInferenceRequest, sink: LlmInferenceResponseSink, state: _PendingState
    ) -> None:
        try:
            await self._provider.on_llm_request(request)
            if not state.finished:
                await self._fail_via_sink(
                    sink,
                    state,
                    "LLM inference provider returned without finalising the response "
                    "(call response_body.end() or .error()).",
                )
        except _RuntimeRejectedError:
            # The runtime already dropped the request; nothing more to emit.
            pass
        except Exception as exc:
            if state.cancelled or state.cancel_event.is_set():
                await self._finish_cancelled(sink, state)
                return
            await self._fail_via_sink(sink, state, str(exc))

    async def http_request_start(
        self, params: LlmInferenceHTTPRequestStartRequest
    ) -> LlmInferenceHTTPRequestStartResult:
        state = _PendingState(queue=_BodyQueue(), cancel_event=asyncio.Event())
        self._pending[params.request_id] = state

        staged = self._staged.pop(params.request_id, None)
        if staged:
            for chunk in staged:
                self._route_chunk(state, chunk)

        sink = self._make_sink(params.request_id, state)
        transport = (
            params.transport.value if params.transport is not None else "http"
        )
        request = LlmInferenceRequest(
            request_id=params.request_id,
            session_id=params.session_id,
            method=params.method,
            url=params.url,
            headers=params.headers,
            transport=transport,
            request_body=state.queue,
            cancel_event=state.cancel_event,
            response_body=sink,
        )
        state.task = asyncio.create_task(self._run_provider(request, sink, state))
        return LlmInferenceHTTPRequestStartResult()

    async def http_request_chunk(
        self, params: LlmInferenceHTTPRequestChunkRequest
    ) -> LlmInferenceHTTPRequestChunkResult:
        state = self._pending.get(params.request_id)
        if state is None:
            self._staged.setdefault(params.request_id, []).append(params)
            return LlmInferenceHTTPRequestChunkResult()
        self._route_chunk(state, params)
        return LlmInferenceHTTPRequestChunkResult()
