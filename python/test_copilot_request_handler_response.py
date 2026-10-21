import asyncio
import base64

import httpx
import pytest

from copilot.copilot_request_handler import (
    CopilotRequestContext,
    CopilotRequestHandler,
    CopilotWebSocketHandler,
    _CopilotRequestAdapterHandler,
    _CopilotRequestExchange,
    _stream_response_to_exchange,
    create_copilot_request_adapter,
)
from copilot.generated.rpc import (
    LlmInferenceHTTPRequestChunkRequest,
    LlmInferenceHTTPRequestStartRequest,
    LlmInferenceHTTPRequestStartTransport,
)


@pytest.mark.parametrize("agent_invocation_id", ["logical-invocation", None, ""])
async def test_http_start_invocation_identity_survives_physical_retries(agent_invocation_id):
    contexts = []

    class Handler(CopilotRequestHandler):
        async def send_request(self, request, ctx):
            contexts.append(ctx)
            assert request.content == b"request-body"
            return httpx.Response(200)

    for request_id in ["physical-first", "physical-retry"]:
        rpc = _WithheldAckRpc()
        adapter = create_copilot_request_adapter(Handler(), lambda: rpc)
        await adapter.http_request_start(
            LlmInferenceHTTPRequestStartRequest(
                request_id=request_id,
                agent_invocation_id=agent_invocation_id,
                session_id="session",
                agent_id="trajectory",
                parent_agent_id="parent",
                interaction_type="conversation-agent",
                method="POST",
                url="https://example.test",
                headers={"x-multiple": ["one", "two"]},
            )
        )
        task = adapter._pending[request_id].task
        await adapter.http_request_chunk(
            LlmInferenceHTTPRequestChunkRequest(
                request_id=request_id,
                data=base64.b64encode(b"request-body").decode("ascii"),
                binary=True,
                end=True,
            )
        )
        await asyncio.wait_for(task, timeout=2)
        ctx = contexts[-1]
        assert ctx.agent_invocation_id == agent_invocation_id
        assert ctx.request_id == request_id
        assert (ctx.session_id, ctx.agent_id, ctx.parent_agent_id) == (
            "session",
            "trajectory",
            "parent",
        )
        assert ctx.interaction_type == "conversation-agent"
        assert ctx.headers == {"x-multiple": ["one", "two"]}
        assert rpc.chunks[-1].error is None
    assert len(contexts) == 2


@pytest.mark.parametrize("legacy", [False, True])
async def test_websocket_invocation_identity_is_per_queued_message(legacy):
    messages = []
    connection_contexts = []

    class LegacySocket(CopilotWebSocketHandler):
        async def send_request_message(self, data):
            messages.append((data, None))
            messages_changed.set()

    class Socket(LegacySocket):
        async def send_request_message_with_context(self, data, ctx):
            messages.append((data, ctx))
            messages_changed.set()

    class Handler(CopilotRequestHandler):
        async def open_websocket(self, ctx):
            connection_contexts.append(ctx)
            return LegacySocket(ctx) if legacy else Socket(ctx)

    messages_changed = asyncio.Event()
    rpc = _WithheldAckRpc()
    adapter = create_copilot_request_adapter(Handler(), lambda: rpc)
    ids = ["opening-invocation", "later-invocation", "later-invocation", None, ""]
    for index, invocation_id in enumerate(ids[:2]):
        await adapter.http_request_chunk(
            LlmInferenceHTTPRequestChunkRequest(
                request_id="reused-socket",
                data=f"message-{index}",
                agent_invocation_id=invocation_id,
            )
        )
    await adapter.http_request_start(
        LlmInferenceHTTPRequestStartRequest(
            request_id="reused-socket",
            agent_invocation_id="opening-invocation",
            session_id="session",
            agent_id="trajectory",
            transport=LlmInferenceHTTPRequestStartTransport.WEBSOCKET,
            method="GET",
            url="wss://example.test",
            headers={},
        )
    )
    task = adapter._pending["reused-socket"].task
    await _wait_for(lambda: len(messages) == 2, messages_changed)
    for index in range(2, len(ids)):
        await adapter.http_request_chunk(
            LlmInferenceHTTPRequestChunkRequest(
                request_id="reused-socket", data=f"message-{index}", agent_invocation_id=ids[index]
            )
        )
    await adapter.http_request_chunk(
        LlmInferenceHTTPRequestChunkRequest(request_id="reused-socket", data="", end=True)
    )
    await asyncio.wait_for(task, timeout=2)

    assert [data for data, _ in messages] == [f"message-{i}" for i in range(len(ids))]
    assert rpc.starts[0].status == 101
    assert rpc.chunks[-1].error is None
    if not legacy:
        connection_ctx = connection_contexts[0]
        assert connection_ctx.agent_invocation_id == "opening-invocation"
        assert [ctx.agent_invocation_id for _, ctx in messages] == ids
        for _, ctx in messages:
            assert ctx is not connection_ctx
            assert (ctx.request_id, ctx.session_id, ctx.agent_id) == (
                "reused-socket",
                "session",
                "trajectory",
            )
            assert ctx.cancel_event is connection_ctx.cancel_event


async def _wait_for(predicate, changed: asyncio.Event, timeout: float = 2.0) -> None:
    async with asyncio.timeout(timeout):
        while True:
            changed.clear()
            if predicate():
                return
            await changed.wait()


class _ControlledStream(httpx.AsyncByteStream):
    def __init__(
        self,
        chunks: list[bytes],
        *,
        error: Exception | None = None,
        block_after_chunks: bool = False,
    ) -> None:
        self._chunks = chunks
        self._error = error
        self._block_after_chunks = block_after_chunks
        self.read_count = 0
        self.changed = asyncio.Event()
        self.exhausted = asyncio.Event()
        self.read_blocked = asyncio.Event()
        self.read_cancelled = asyncio.Event()
        self.closed = asyncio.Event()

    async def __aiter__(self):
        for chunk in self._chunks:
            self.read_count += 1
            self.changed.set()
            yield chunk
        self.exhausted.set()
        self.changed.set()
        if self._error is not None:
            raise self._error
        if self._block_after_chunks:
            self.read_blocked.set()
            try:
                await asyncio.Event().wait()
            except asyncio.CancelledError:
                self.read_cancelled.set()
                raise

    async def aclose(self) -> None:
        self.closed.set()


class _TaskAffineStream(httpx.AsyncByteStream):
    def __init__(self) -> None:
        self.owner: asyncio.Task[None] | None = None
        self.closed = asyncio.Event()

    async def __aiter__(self):
        owner = asyncio.current_task()
        self.owner = owner
        yield b"first"
        if asyncio.current_task() is not owner:
            raise RuntimeError("response iterator resumed from a different task")
        yield b"second"

    async def aclose(self) -> None:
        if self.closed.is_set():
            return
        if asyncio.current_task() is not self.owner:
            raise RuntimeError("response stream closed from a different task")
        self.closed.set()


class _BufferedTaskAffineStream(httpx.AsyncByteStream):
    def __init__(self) -> None:
        self.owner: asyncio.Task[None] | None = None
        self.read_count = 0
        self.changed = asyncio.Event()
        self.closed = asyncio.Event()

    async def __aiter__(self):
        self.owner = asyncio.current_task()
        while True:
            self.read_count += 1
            self.changed.set()
            yield b"x" * 1024

    async def aclose(self) -> None:
        if self.closed.is_set():
            return
        if asyncio.current_task() is not self.owner:
            raise RuntimeError("response stream closed from a different task")
        self.closed.set()


class _WithheldAckRpc:
    def __init__(self, changed: asyncio.Event | None = None) -> None:
        self.changed = changed if changed is not None else asyncio.Event()
        self.starts = []
        self.chunks = []
        self.acks: list[asyncio.Future[None]] = []
        self.outstanding_data = 0
        self.max_outstanding_data = 0

    @property
    def data_chunks(self):
        return [chunk for chunk in self.chunks if not chunk.end]

    async def http_response_start(self, params):
        self.starts.append(params)

    async def http_response_chunk(self, params):
        self.chunks.append(params)
        self.changed.set()
        if params.end:
            return
        ack = asyncio.get_running_loop().create_future()
        self.acks.append(ack)
        self.outstanding_data += 1
        self.max_outstanding_data = max(self.max_outstanding_data, self.outstanding_data)
        try:
            _ = await ack
        finally:
            self.outstanding_data -= 1

    def acknowledge(self, index: int) -> None:
        self.acks[index].set_result(None)

    def reject(self, index: int, error: Exception) -> None:
        self.acks[index].set_exception(error)


def _response(stream: httpx.AsyncByteStream) -> httpx.Response:
    return httpx.Response(
        200,
        headers={"content-type": "application/octet-stream"},
        stream=stream,
        request=httpx.Request("GET", "https://example.test/response"),
    )


async def _pump(response: httpx.Response, exchange: _CopilotRequestExchange) -> None:
    try:
        await _stream_response_to_exchange(response, exchange)
    finally:
        await response.aclose()


def _decoded_data(rpc: _WithheldAckRpc) -> list[bytes]:
    return [base64.b64decode(chunk.data) for chunk in rpc.data_chunks]


@pytest.mark.asyncio
async def test_reads_ahead_and_coalesces_with_one_data_rpc_outstanding() -> None:
    stream = _ControlledStream([b"x" * 1024] * 70)
    response = _response(stream)
    rpc = _WithheldAckRpc(stream.changed)
    exchange = _CopilotRequestExchange("request", lambda: rpc)

    pump = asyncio.create_task(_pump(response, exchange))
    await _wait_for(lambda: len(rpc.data_chunks) == 1 and stream.read_count == 33, rpc.changed)

    assert _decoded_data(rpc) == [b"x" * 1024]
    assert rpc.outstanding_data == 1
    assert rpc.max_outstanding_data == 1
    reads_at_capacity = stream.read_count
    await asyncio.sleep(0.01)
    assert stream.read_count == reads_at_capacity
    assert len(rpc.data_chunks) == 1

    rpc.acknowledge(0)
    await _wait_for(lambda: len(rpc.data_chunks) == 2 and stream.read_count == 65, rpc.changed)
    assert len(_decoded_data(rpc)[1]) == 32 * 1024
    assert rpc.max_outstanding_data == 1

    rpc.acknowledge(1)
    await _wait_for(lambda: len(rpc.data_chunks) == 3 and stream.exhausted.is_set(), rpc.changed)
    assert len(_decoded_data(rpc)[2]) == 32 * 1024

    rpc.acknowledge(2)
    await _wait_for(lambda: len(rpc.data_chunks) == 4, rpc.changed)
    assert len(_decoded_data(rpc)[3]) == 5 * 1024
    rpc.acknowledge(3)
    await asyncio.wait_for(pump, timeout=2)

    assert b"".join(_decoded_data(rpc)) == b"x" * (70 * 1024)
    assert rpc.max_outstanding_data == 1
    assert rpc.chunks[-1].end is True
    assert rpc.chunks[-1].error is None
    assert stream.closed.is_set()


@pytest.mark.asyncio
async def test_response_iterator_is_advanced_by_one_persistent_task() -> None:
    stream = _TaskAffineStream()
    response = _response(stream)
    rpc = _WithheldAckRpc()
    exchange = _CopilotRequestExchange("request", lambda: rpc)

    pump = asyncio.create_task(_pump(response, exchange))
    await _wait_for(lambda: len(rpc.data_chunks) == 1, rpc.changed)
    rpc.acknowledge(0)
    await _wait_for(lambda: len(rpc.data_chunks) == 2, rpc.changed)
    rpc.acknowledge(1)
    await asyncio.wait_for(pump, timeout=2)

    assert b"".join(_decoded_data(rpc)) == b"firstsecond"
    assert stream.closed.is_set()


@pytest.mark.asyncio
async def test_response_stream_is_closed_by_producer_when_read_ahead_is_full() -> None:
    stream = _BufferedTaskAffineStream()
    response = _response(stream)
    rpc = _WithheldAckRpc(stream.changed)
    exchange = _CopilotRequestExchange("request", lambda: rpc)

    pump = asyncio.create_task(_pump(response, exchange))
    await _wait_for(lambda: len(rpc.data_chunks) == 1 and stream.read_count == 33, rpc.changed)
    rpc.reject(0, ConnectionError("runtime connection lost"))

    with pytest.raises(ConnectionError, match="runtime connection lost"):
        await asyncio.wait_for(pump, timeout=2)
    assert stream.closed.is_set()


class _ResponseHandler(CopilotRequestHandler):
    def __init__(self, response: httpx.Response) -> None:
        self._response = response
        self.context: CopilotRequestContext | None = None

    async def send_request(
        self, request: httpx.Request, ctx: CopilotRequestContext
    ) -> httpx.Response:
        self.context = ctx
        return self._response


async def _start_adapter_request(
    stream: _ControlledStream,
    *,
    handler: _ResponseHandler | None = None,
    agent_invocation_id: str | None = None,
) -> tuple[_WithheldAckRpc, _CopilotRequestAdapterHandler, asyncio.Task[None]]:
    rpc = _WithheldAckRpc()
    adapter = create_copilot_request_adapter(
        handler if handler is not None else _ResponseHandler(_response(stream)), lambda: rpc
    )
    request_id = "request"
    await adapter.http_request_start(
        LlmInferenceHTTPRequestStartRequest(
            headers={},
            method="GET",
            request_id=request_id,
            url="https://example.test/response",
            session_id="session",
            agent_id="agent",
            parent_agent_id="parent",
            agent_invocation_id=agent_invocation_id,
        )
    )
    await adapter.http_request_chunk(
        LlmInferenceHTTPRequestChunkRequest(data="", request_id=request_id, end=True)
    )
    exchange = adapter._pending[request_id]
    assert exchange.task is not None
    return rpc, adapter, exchange.task


@pytest.mark.asyncio
@pytest.mark.parametrize("agent_invocation_id", [None, "first-invocation", "second-invocation"])
async def test_request_context_preserves_invocation_identity(
    agent_invocation_id: str | None,
) -> None:
    stream = _ControlledStream([])
    handler = _ResponseHandler(_response(stream))
    _, _, task = await _start_adapter_request(
        stream, handler=handler, agent_invocation_id=agent_invocation_id
    )
    await asyncio.wait_for(task, timeout=2)

    assert handler.context is not None
    assert handler.context.request_id == "request"
    assert handler.context.session_id == "session"
    assert handler.context.agent_id == "agent"
    assert handler.context.parent_agent_id == "parent"
    assert handler.context.agent_invocation_id == agent_invocation_id
    assert stream.closed.is_set()


@pytest.mark.asyncio
async def test_runtime_cancellation_stops_pending_read_and_closes_source() -> None:
    stream = _ControlledStream([b"first"], block_after_chunks=True)
    rpc, adapter, task = await _start_adapter_request(stream)
    await asyncio.wait_for(stream.read_blocked.wait(), timeout=2)
    await _wait_for(lambda: len(rpc.data_chunks) == 1, rpc.changed)

    await adapter.http_request_chunk(
        LlmInferenceHTTPRequestChunkRequest(
            data="",
            request_id="request",
            cancel=True,
            cancel_reason="consumer stopped",
        )
    )
    await asyncio.wait_for(task, timeout=2)

    assert stream.read_cancelled.is_set()
    assert stream.closed.is_set()
    assert rpc.outstanding_data == 0
    assert rpc.max_outstanding_data == 1
    assert rpc.chunks[-1].end is True
    assert rpc.chunks[-1].error.code == "cancelled"


@pytest.mark.asyncio
async def test_upstream_error_follows_all_buffered_bytes() -> None:
    stream = _ControlledStream(
        [b"first", b"partial"],
        error=RuntimeError("synthetic upstream failure"),
    )
    rpc, _, task = await _start_adapter_request(stream)
    await asyncio.wait_for(stream.exhausted.wait(), timeout=2)
    await _wait_for(lambda: len(rpc.data_chunks) == 1, rpc.changed)

    rpc.acknowledge(0)
    await _wait_for(lambda: len(rpc.data_chunks) == 2, rpc.changed)
    rpc.acknowledge(1)
    await asyncio.wait_for(task, timeout=2)

    assert _decoded_data(rpc) == [b"first", b"partial"]
    assert rpc.chunks[-1].end is True
    assert rpc.chunks[-1].error.message == "synthetic upstream failure"
    assert stream.closed.is_set()


@pytest.mark.asyncio
async def test_rpc_rejection_stops_pending_read_and_closes_source() -> None:
    stream = _ControlledStream([b"first"], block_after_chunks=True)
    rpc, _, task = await _start_adapter_request(stream)
    await asyncio.wait_for(stream.read_blocked.wait(), timeout=2)
    await _wait_for(lambda: len(rpc.data_chunks) == 1, rpc.changed)

    rpc.reject(0, ConnectionError("runtime connection lost"))
    await asyncio.wait_for(task, timeout=2)

    assert stream.read_cancelled.is_set()
    assert stream.closed.is_set()
    assert rpc.outstanding_data == 0
    assert rpc.max_outstanding_data == 1
    assert rpc.chunks[-1].end is True
    assert rpc.chunks[-1].error.message == "runtime connection lost"


@pytest.mark.asyncio
async def test_connection_loss_stops_idle_read_and_closes_source() -> None:
    stream = _ControlledStream([], block_after_chunks=True)
    rpc, adapter, task = await _start_adapter_request(stream)
    await asyncio.wait_for(stream.read_blocked.wait(), timeout=2)

    adapter.cancel_pending()
    await asyncio.wait_for(task, timeout=2)

    assert stream.read_cancelled.is_set()
    assert stream.closed.is_set()
    assert rpc.data_chunks == []
    assert rpc.chunks[-1].end is True
    assert rpc.chunks[-1].error.code == "cancelled"
