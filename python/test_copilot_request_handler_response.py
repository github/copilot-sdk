import asyncio
import base64

import httpx
import pytest

from copilot.copilot_request_handler import (
    CopilotRequestContext,
    CopilotRequestHandler,
    _CopilotRequestAdapterHandler,
    _CopilotRequestExchange,
    _stream_response_to_exchange,
    create_copilot_request_adapter,
)
from copilot.generated.rpc import (
    LlmInferenceHTTPRequestChunkRequest,
    LlmInferenceHTTPRequestStartRequest,
)


async def _wait_for(predicate, timeout: float = 2.0) -> None:
    async with asyncio.timeout(timeout):
        while not predicate():
            await asyncio.sleep(0)


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
        self.exhausted = asyncio.Event()
        self.read_blocked = asyncio.Event()
        self.read_cancelled = asyncio.Event()
        self.closed = asyncio.Event()

    async def __aiter__(self):
        for chunk in self._chunks:
            self.read_count += 1
            yield chunk
        self.exhausted.set()
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
        self.closed = asyncio.Event()

    async def __aiter__(self):
        self.owner = asyncio.current_task()
        while True:
            self.read_count += 1
            yield b"x" * 1024

    async def aclose(self) -> None:
        if self.closed.is_set():
            return
        if asyncio.current_task() is not self.owner:
            raise RuntimeError("response stream closed from a different task")
        self.closed.set()


class _WithheldAckRpc:
    def __init__(self) -> None:
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
    rpc = _WithheldAckRpc()
    exchange = _CopilotRequestExchange("request", lambda: rpc)

    pump = asyncio.create_task(_pump(response, exchange))
    await _wait_for(lambda: len(rpc.data_chunks) == 1 and stream.read_count == 33)

    assert _decoded_data(rpc) == [b"x" * 1024]
    assert rpc.outstanding_data == 1
    assert rpc.max_outstanding_data == 1
    reads_at_capacity = stream.read_count
    await asyncio.sleep(0.01)
    assert stream.read_count == reads_at_capacity
    assert len(rpc.data_chunks) == 1

    rpc.acknowledge(0)
    await _wait_for(lambda: len(rpc.data_chunks) == 2 and stream.read_count == 65)
    assert len(_decoded_data(rpc)[1]) == 32 * 1024
    assert rpc.max_outstanding_data == 1

    rpc.acknowledge(1)
    await _wait_for(lambda: len(rpc.data_chunks) == 3 and stream.exhausted.is_set())
    assert len(_decoded_data(rpc)[2]) == 32 * 1024

    rpc.acknowledge(2)
    await _wait_for(lambda: len(rpc.data_chunks) == 4)
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
    await _wait_for(lambda: len(rpc.data_chunks) == 1)
    rpc.acknowledge(0)
    await _wait_for(lambda: len(rpc.data_chunks) == 2)
    rpc.acknowledge(1)
    await asyncio.wait_for(pump, timeout=2)

    assert b"".join(_decoded_data(rpc)) == b"firstsecond"
    assert stream.closed.is_set()


@pytest.mark.asyncio
async def test_response_stream_is_closed_by_producer_when_read_ahead_is_full() -> None:
    stream = _BufferedTaskAffineStream()
    response = _response(stream)
    rpc = _WithheldAckRpc()
    exchange = _CopilotRequestExchange("request", lambda: rpc)

    pump = asyncio.create_task(_pump(response, exchange))
    await _wait_for(lambda: len(rpc.data_chunks) == 1 and stream.read_count == 33)
    rpc.reject(0, ConnectionError("runtime connection lost"))

    with pytest.raises(ConnectionError, match="runtime connection lost"):
        await asyncio.wait_for(pump, timeout=2)
    assert stream.closed.is_set()


class _ResponseHandler(CopilotRequestHandler):
    def __init__(self, response: httpx.Response) -> None:
        self._response = response

    async def send_request(
        self, request: httpx.Request, ctx: CopilotRequestContext
    ) -> httpx.Response:
        return self._response


async def _start_adapter_request(
    stream: _ControlledStream,
) -> tuple[_WithheldAckRpc, _CopilotRequestAdapterHandler, asyncio.Task[None]]:
    rpc = _WithheldAckRpc()
    adapter = create_copilot_request_adapter(_ResponseHandler(_response(stream)), lambda: rpc)
    request_id = "request"
    await adapter.http_request_start(
        LlmInferenceHTTPRequestStartRequest(
            headers={},
            method="GET",
            request_id=request_id,
            url="https://example.test/response",
        )
    )
    await adapter.http_request_chunk(
        LlmInferenceHTTPRequestChunkRequest(data="", request_id=request_id, end=True)
    )
    exchange = adapter._pending[request_id]
    assert exchange.task is not None
    return rpc, adapter, exchange.task


@pytest.mark.asyncio
async def test_runtime_cancellation_stops_pending_read_and_closes_source() -> None:
    stream = _ControlledStream([b"first"], block_after_chunks=True)
    rpc, adapter, task = await _start_adapter_request(stream)
    await asyncio.wait_for(stream.read_blocked.wait(), timeout=2)
    await _wait_for(lambda: len(rpc.data_chunks) == 1)

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
    await _wait_for(lambda: len(rpc.data_chunks) == 1)

    rpc.acknowledge(0)
    await _wait_for(lambda: len(rpc.data_chunks) == 2)
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
    await _wait_for(lambda: len(rpc.data_chunks) == 1)

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
