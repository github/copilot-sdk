"""Memory store callback unit tests."""

import asyncio
import io
import json
from typing import Any
from unittest.mock import AsyncMock, Mock

import pytest

from copilot import (
    CloudSessionOptions,
    CopilotClient,
    RuntimeConnection,
    StoredMemory,
)
from copilot._jsonrpc import JsonRpcClient, JsonRpcError
from copilot.session import CopilotSession, MemoryConfiguration

_PARAMS = {
    "sessionId": "session-1",
    "subject": "testing",
    "fact": "Use pytest.",
    "citations": "README.md:1",
    "reason": "Repo convention.",
}


@pytest.mark.asyncio
async def test_create_and_resume_bind_store_before_request() -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    captured: list[tuple[str, dict]] = []
    stored: list[str] = []

    async def request(method: str, params: dict, **kwargs: Any) -> Any:
        captured.append((method, dict(params)))
        if method in ("session.create", "session.resume"):
            await client._handle_memory_store({**_PARAMS, "sessionId": params["sessionId"]})
            result = {"sessionId": params["sessionId"], "workspacePath": None}
            if "on_response_inline" in kwargs:
                kwargs["on_response_inline"](result)
            return result
        return None

    client._client = Mock()
    client._client.request = request

    async def store(memory: StoredMemory) -> None:
        stored.append(memory["session_id"])

    memory: MemoryConfiguration = {"enabled": True, "store_memory": store}
    await client.create_session(session_id="created", memory=memory)
    await client.resume_session("resumed", memory=memory)

    payloads = {method: params for method, params in captured}
    for method in ("session.create", "session.resume"):
        assert payloads[method]["hasMemoryStore"] is True
        assert payloads[method]["memory"] == {"enabled": True}
    assert stored == ["created", "resumed"]
    assert "session.memory.configureStore" not in payloads


@pytest.mark.asyncio
async def test_store_memory_requires_enabled_memory_before_any_request() -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    client._client = Mock()
    client._client.request = AsyncMock()

    async def store(memory: StoredMemory) -> None:
        pass

    memory: MemoryConfiguration = {"enabled": False, "store_memory": store}
    with pytest.raises(ValueError, match="memory.store_memory requires memory.enabled"):
        await client.create_session(memory=memory)
    with pytest.raises(ValueError, match="memory.store_memory requires memory.enabled"):
        await client.resume_session("resumed", memory=memory)
    with pytest.raises(ValueError, match="not supported for cloud sessions"):
        await client.create_session(
            cloud=CloudSessionOptions(), memory={"enabled": True, "store_memory": store}
        )
    client._client.request.assert_not_called()


@pytest.mark.asyncio
async def test_memory_store_request_invokes_callback() -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    session = CopilotSession("session-1", Mock())
    stored: list[StoredMemory] = []

    async def store(memory: StoredMemory) -> None:
        stored.append(memory)

    session._memory_store_handler = store
    client._sessions["session-1"] = session

    assert await client._handle_memory_store(_PARAMS) is None
    assert stored == [
        {
            "session_id": "session-1",
            "subject": "testing",
            "fact": "Use pytest.",
            "citations": "README.md:1",
            "reason": "Repo convention.",
        }
    ]


@pytest.mark.asyncio
async def test_memory_store_errors_are_generic() -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))

    with pytest.raises(JsonRpcError) as unknown_session:
        await client._handle_memory_store(_PARAMS)
    assert unknown_session.value.message == "Memory store owner is unavailable"

    session = CopilotSession("session-1", Mock())
    client._sessions["session-1"] = session
    with pytest.raises(JsonRpcError) as no_handler:
        await client._handle_memory_store(_PARAMS)
    assert no_handler.value.message == "No memory store is registered"

    async def store(memory: StoredMemory) -> None:
        raise RuntimeError("secret-store-error")

    session._memory_store_handler = store
    with pytest.raises(JsonRpcError) as exc_info:
        await client._handle_memory_store(_PARAMS)
    assert (exc_info.value.code, exc_info.value.message, exc_info.value.data) == (
        -32603,
        "Memory store callback failed",
        None,
    )
    assert exc_info.value.__cause__ is None


class _WriteOnlyProcess:
    def __init__(self) -> None:
        self.stdin = io.BytesIO()
        self.stdout = io.BytesIO()
        self.stderr = None

    def poll(self) -> None:
        return None


@pytest.mark.parametrize("trigger", ["cancel_request", "connection_close"])
@pytest.mark.asyncio
async def test_memory_store_callback_is_cancelled_by_runtime(trigger: str) -> None:
    entered = asyncio.Event()
    cancelled = asyncio.Event()

    async def store(memory: StoredMemory) -> None:
        entered.set()
        try:
            await asyncio.Event().wait()
        except asyncio.CancelledError:
            cancelled.set()
            raise

    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    process = _WriteOnlyProcess()
    rpc_client = JsonRpcClient(process)
    rpc_client._loop = asyncio.get_running_loop()
    client._client = rpc_client
    client._register_session_scoped_request_handlers()
    session = CopilotSession("session-1", rpc_client)
    session._register_memory_store(store)
    client._sessions["session-1"] = session

    rpc_client._handle_request(
        {"jsonrpc": "2.0", "id": 7, "method": "memoryStore.store", "params": _PARAMS}
    )
    await asyncio.wait_for(entered.wait(), timeout=5)
    if trigger == "connection_close":
        rpc_client._fail_pending_requests()
        await asyncio.wait_for(cancelled.wait(), timeout=5)
        await asyncio.sleep(0.05)
        assert process.stdin.getvalue() == b""
        return
    rpc_client._handle_cancel_request({"id": 7})
    await asyncio.wait_for(cancelled.wait(), timeout=5)
    for _ in range(100):
        if process.stdin.getvalue():
            break
        await asyncio.sleep(0.01)

    _header, payload = process.stdin.getvalue().split(b"\r\n\r\n", 1)
    assert json.loads(payload) == {
        "jsonrpc": "2.0",
        "id": 7,
        "error": {"code": -32800, "message": "Request cancelled", "data": None},
    }


@pytest.mark.asyncio
async def test_memory_store_callback_is_skipped_when_cancelled_before_dispatch() -> None:
    called = False

    async def store(memory: StoredMemory) -> None:
        nonlocal called
        called = True

    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    process = _WriteOnlyProcess()
    rpc_client = JsonRpcClient(process)
    rpc_client._loop = asyncio.get_running_loop()
    client._client = rpc_client
    client._register_session_scoped_request_handlers()
    session = CopilotSession("session-1", rpc_client)
    session._register_memory_store(store)
    client._sessions["session-1"] = session

    rpc_client._handle_request(
        {"jsonrpc": "2.0", "id": 7, "method": "memoryStore.store", "params": _PARAMS}
    )
    rpc_client._handle_cancel_request({"id": 7})
    for _ in range(100):
        if process.stdin.getvalue():
            break
        await asyncio.sleep(0.01)

    assert not called
    _header, payload = process.stdin.getvalue().split(b"\r\n\r\n", 1)
    assert json.loads(payload)["error"]["code"] == -32800


@pytest.mark.parametrize("cancel_style", ["raise", "cancel_current_task"])
@pytest.mark.asyncio
async def test_memory_store_callback_self_cancellation_is_a_failure(cancel_style: str) -> None:
    async def store(memory: StoredMemory) -> None:
        if cancel_style == "raise":
            raise asyncio.CancelledError()
        task = asyncio.current_task()
        assert task is not None
        task.cancel()
        await asyncio.sleep(0)

    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    process = _WriteOnlyProcess()
    rpc_client = JsonRpcClient(process)
    rpc_client._loop = asyncio.get_running_loop()
    client._client = rpc_client
    client._register_session_scoped_request_handlers()
    session = CopilotSession("session-1", rpc_client)
    session._register_memory_store(store)
    client._sessions["session-1"] = session

    rpc_client._handle_request(
        {"jsonrpc": "2.0", "id": 7, "method": "memoryStore.store", "params": _PARAMS}
    )
    for _ in range(100):
        if process.stdin.getvalue():
            break
        await asyncio.sleep(0.01)

    _header, payload = process.stdin.getvalue().split(b"\r\n\r\n", 1)
    assert json.loads(payload)["error"] == {
        "code": -32603,
        "message": "Memory store callback failed",
        "data": None,
    }


class _HostAbort(BaseException):
    """A host-defined BaseException that is not an Exception, like pytest's ``Failed``."""


@pytest.mark.parametrize("shape", ["custom-base-exception", "pytest-fail"])
@pytest.mark.asyncio
async def test_memory_store_callback_raising_base_exception_is_a_failure(shape: str) -> None:
    async def store(memory: StoredMemory) -> None:
        if shape == "pytest-fail":
            pytest.fail("store assertion failed")
        raise _HostAbort("store assertion failed")

    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    process = _WriteOnlyProcess()
    rpc_client = JsonRpcClient(process)
    rpc_client._loop = asyncio.get_running_loop()
    client._client = rpc_client
    client._register_session_scoped_request_handlers()
    session = CopilotSession("session-1", rpc_client)
    session._register_memory_store(store)
    client._sessions["session-1"] = session

    rpc_client._handle_request(
        {"jsonrpc": "2.0", "id": 7, "method": "memoryStore.store", "params": _PARAMS}
    )
    for _ in range(100):
        if process.stdin.getvalue():
            break
        await asyncio.sleep(0.01)

    assert process.stdin.getvalue(), "memoryStore.store was never answered"
    _header, payload = process.stdin.getvalue().split(b"\r\n\r\n", 1)
    assert json.loads(payload)["error"] == {
        "code": -32603,
        "message": "Memory store callback failed",
        "data": None,
    }


@pytest.mark.parametrize("teardown", ["delete", "close"])
@pytest.mark.asyncio
async def test_memory_store_is_cleared_on_client_teardown(teardown: str) -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    session = CopilotSession("session-1", Mock())
    session._register_memory_store(AsyncMock())
    client._sessions["session-1"] = session

    if teardown == "delete":
        client._client = session._client
        client._client.request = AsyncMock(return_value={"success": True})
        await client.delete_session("session-1")
    else:
        client._client = Mock(_loop=asyncio.get_running_loop())
        client._handle_connection_close()
        await asyncio.sleep(0)

    assert session._memory_store_handler is None
