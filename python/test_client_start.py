"""Startup concurrency regressions without a live CLI runtime."""

import asyncio
import subprocess
import sys
import threading
from unittest.mock import AsyncMock, Mock

import pytest

import copilot
from copilot import (
    CopilotClient,
    GetStatusResponse,
    HostDiagnostics,
    HostErrorReportingStatus,
    RuntimeConnection,
)
from copilot._jsonrpc import JsonRpcClient, JsonRpcError, ProcessExitedError
from copilot._sdk_protocol_version import get_sdk_protocol_version


@pytest.fixture
def client():
    client = CopilotClient(connection=RuntimeConnection.for_stdio(path="unused-cli"))
    client._start_cli_server = AsyncMock()
    client._connect_to_server = AsyncMock()
    client._verify_protocol_version = AsyncMock()
    return client


@pytest.mark.parametrize("error_reporting", [None, "pending", "enabled", "disabled"])
async def test_get_status_preserves_optional_host_diagnostics(client, error_reporting):
    payload = {"version": "1.2.3", "protocolVersion": 2}
    if error_reporting is not None:
        payload["hostDiagnostics"] = {"errorReporting": error_reporting}
    client._client = Mock()
    client._client.request = AsyncMock(return_value=payload)

    status = await client.get_status()

    client._client.request.assert_awaited_once_with("status.get", {})
    assert status.version == "1.2.3"
    assert status.protocol_version == 2
    if error_reporting is None:
        assert status.host_diagnostics is None
    else:
        assert isinstance(status.host_diagnostics, HostDiagnostics)
        assert status.host_diagnostics.error_reporting is HostErrorReportingStatus(error_reporting)
        assert status.host_diagnostics.to_dict() == {"errorReporting": error_reporting}
    assert status.to_dict() == payload


def test_host_error_reporting_status_is_public():
    assert "HostErrorReportingStatus" in copilot.__all__
    diagnostics = HostDiagnostics(error_reporting=HostErrorReportingStatus.ENABLED)

    assert diagnostics.to_dict() == {"errorReporting": "enabled"}


def test_status_retains_existing_positional_construction():
    status = GetStatusResponse("1.2.3", 2)

    assert status.host_diagnostics is None
    assert status.to_dict() == {"version": "1.2.3", "protocolVersion": 2}


@pytest.mark.parametrize(
    "phase", ["_start_cli_server", "_connect_to_server", "_verify_protocol_version"]
)
async def test_concurrent_start_initializes_transport_once(client, phase):
    entered = asyncio.Event()
    release = asyncio.Event()

    async def block():
        entered.set()
        await release.wait()

    getattr(client, phase).side_effect = block
    first = asyncio.create_task(client.start())
    await asyncio.wait_for(entered.wait(), timeout=1)
    second = asyncio.create_task(client.start())
    try:
        await asyncio.sleep(0)
        assert not second.done()
    finally:
        release.set()
        await asyncio.wait_for(asyncio.gather(first, second), timeout=1)

    client._start_cli_server.assert_awaited_once()
    client._connect_to_server.assert_awaited_once()
    client._verify_protocol_version.assert_awaited_once()
    await client.start()
    client._start_cli_server.assert_awaited_once()


async def test_start_can_retry_after_failure(client):
    client._start_cli_server.side_effect = [RuntimeError("startup failed"), None]

    with pytest.raises(RuntimeError, match="startup failed"):
        await client.start()
    await asyncio.wait_for(client.start(), timeout=1)

    assert client._start_cli_server.await_count == 2
    client._connect_to_server.assert_awaited_once()
    client._verify_protocol_version.assert_awaited_once()


@pytest.mark.parametrize("owner", ["process", "ffi"])
async def test_restart_after_connection_loss_disposes_previous_owner(client, owner):
    old_rpc = Mock(stop=AsyncMock(), _loop=asyncio.get_running_loop())
    # A running owner: the connection is lost only when its reader closes.
    old_owner = Mock(stdin=None, poll=Mock(return_value=None))
    client._client = old_rpc
    client._state = "connected"
    if owner == "process":
        client._process = client._cli_process = old_owner
    else:
        client._ffi_host = old_owner
    reader_waiting = asyncio.Event()
    release_reader = threading.Event()
    loop = asyncio.get_running_loop()
    sessions_lock = client._sessions_lock
    reader = threading.Thread(target=client._handle_connection_close)

    class GatedLock:
        def __enter__(self):
            if threading.current_thread() is reader:
                loop.call_soon_threadsafe(reader_waiting.set)
                release_reader.wait()
            return sessions_lock.__enter__()

        def __exit__(self, *args):
            return sessions_lock.__exit__(*args)

    client._sessions_lock = GatedLock()

    async def restart():
        old_rpc.stop.assert_awaited_once()
        if owner == "process":
            old_owner.kill.assert_called_once_with()
            old_owner.wait.assert_called_once()
        else:
            old_owner.dispose.assert_called_once_with()
        client._client = Mock(stop=AsyncMock())

    reader.start()
    try:
        await asyncio.wait_for(reader_waiting.wait(), timeout=1)
        await client.start()
        client._start_cli_server.assert_not_awaited()
        assert client._client is old_rpc

        release_reader.set()
        await asyncio.to_thread(reader.join)
        client._start_cli_server.side_effect = restart
        await client.start()
        client._verify_protocol_version.assert_awaited_once()
    finally:
        release_reader.set()
        await asyncio.to_thread(reader.join)
        await client.force_stop()


@pytest.mark.parametrize("operation", ["pending-request", "request", "notify"])
async def test_start_after_request_fails_on_lost_connection_reconnects(client, operation):
    # The runtime stand-in exits when it receives the first request byte.
    with subprocess.Popen(
        [sys._base_executable, "-c", "import sys; sys.stdin.buffer.read(1)"],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
    ) as process:
        rpc = JsonRpcClient(process)
        caller_started = threading.Event()
        closing = asyncio.Event()
        loop = asyncio.get_running_loop()

        def on_close():
            loop.call_soon_threadsafe(closing.set)
            # Connection teardown can take a while; a caller that sees the failed
            # request must not get ahead of it.
            caller_started.wait(timeout=0.5)
            client._handle_connection_close()

        rpc.on_close = on_close
        client._client = rpc
        client._process = client._cli_process = process
        client._state = "connected"
        rpc.start()
        pending = asyncio.create_task(rpc.request("ping"))
        try:
            if operation != "pending-request":
                await asyncio.wait_for(closing.wait(), timeout=5)
            with pytest.raises(ProcessExitedError):
                await asyncio.wait_for(
                    pending if operation == "pending-request" else getattr(rpc, operation)("ping"),
                    timeout=5,
                )
            await client.start()
            caller_started.set()

            client._start_cli_server.assert_awaited_once()
            assert client._client is not rpc
        finally:
            caller_started.set()
            await client.force_stop()
            pending.cancel()
            await asyncio.gather(pending, return_exceptions=True)


@pytest.mark.parametrize("owner", ["process", "ffi"])
@pytest.mark.parametrize("operation", ["start", "stop"])
async def test_restart_retries_owner_cleanup_after_cancellation(client, owner, operation):
    old_rpc = Mock(stop=AsyncMock(), _loop=asyncio.get_running_loop())
    old_owner = Mock(stdin=None, poll=Mock(return_value=None))
    client._client = old_rpc
    client._state = "disconnected"
    client._connection_lost = True
    entered = asyncio.Event()
    finished = asyncio.Event()
    release = threading.Event()
    loop = asyncio.get_running_loop()

    def cleanup(*args, **kwargs):
        loop.call_soon_threadsafe(entered.set)
        release.wait()
        loop.call_soon_threadsafe(finished.set)

    if owner == "process":
        client._process = client._cli_process = old_owner
        old_owner.wait.side_effect = cleanup
    else:
        client._ffi_host = old_owner
        old_owner.dispose.side_effect = cleanup

    async def restart():
        old_rpc.stop.assert_awaited_once()
        if owner == "process":
            assert old_owner.wait.call_count == 2
        else:
            assert old_owner.dispose.call_count == 2
        client._client = Mock(stop=AsyncMock())

    task = asyncio.create_task(getattr(client, operation)())
    try:
        await asyncio.wait_for(entered.wait(), timeout=1)
        task.cancel()
        with pytest.raises(asyncio.CancelledError):
            await task
        release.set()
        await asyncio.wait_for(finished.wait(), timeout=1)
        client._start_cli_server.side_effect = restart
        await client.start()
    finally:
        release.set()
        if not task.done():
            task.cancel()
        await asyncio.gather(task, return_exceptions=True)
        if entered.is_set():
            await asyncio.wait_for(finished.wait(), timeout=1)
        await client.force_stop()


async def test_cancelling_waiting_start_does_not_cancel_active_start(client):
    entered = asyncio.Event()
    release = asyncio.Event()

    async def block():
        entered.set()
        await release.wait()

    client._start_cli_server.side_effect = block
    first = asyncio.create_task(client.start())
    await asyncio.wait_for(entered.wait(), timeout=1)
    second = asyncio.create_task(client.start())
    try:
        await asyncio.sleep(0)
        second.cancel()
        with pytest.raises(asyncio.CancelledError):
            _ = await second
        assert not first.done()
    finally:
        release.set()
        await asyncio.wait_for(first, timeout=1)

    await client.start()
    client._start_cli_server.assert_awaited_once()
    client._verify_protocol_version.assert_awaited_once()


STATUS_CALLS = {
    "ping": (
        "ping",
        {"message": None},
        {"message": "pong", "timestamp": "2026-01-01T00:00:00Z", "protocolVersion": 3},
    ),
    "get_status": ("status.get", {}, {"version": "1.2.3", "protocolVersion": 3}),
    "get_auth_status": ("auth.getStatus", {}, {"isAuthenticated": False}),
    "list_models": ("models.list", {}, {"models": []}),
}


async def _settle(task: asyncio.Task) -> None:
    # Enough turns for a call that does not wait for start() to send or fail.
    for _ in range(10):
        if task.done():
            return
        await asyncio.sleep(0)


@pytest.mark.parametrize("phase", ["_start_cli_server", "_verify_protocol_version"])
@pytest.mark.parametrize("method", list(STATUS_CALLS))
async def test_status_call_waits_for_in_progress_start(client, method, phase):
    """_start_cli_server holds start() before the transport exists, _verify_protocol_version
    after it exists but before the handshake has completed."""
    wire_method, params, payload = STATUS_CALLS[method]
    rpc = Mock(request=AsyncMock(return_value=payload), stop=AsyncMock())
    entered = asyncio.Event()
    release = asyncio.Event()

    async def connect():
        client._client = rpc

    async def block():
        entered.set()
        await release.wait()

    client._connect_to_server.side_effect = connect
    getattr(client, phase).side_effect = block
    start = asyncio.create_task(client.start())
    await asyncio.wait_for(entered.wait(), timeout=1)
    call = asyncio.create_task(getattr(client, method)())
    try:
        await _settle(call)
        assert not call.done()
        rpc.request.assert_not_awaited()
    finally:
        release.set()
        await asyncio.wait_for(start, timeout=1)

    await asyncio.wait_for(call, timeout=1)
    rpc.request.assert_awaited_once_with(wire_method, params)


@pytest.mark.parametrize("method", list(STATUS_CALLS))
async def test_status_call_reports_failure_of_the_start_it_waited_for(client, method):
    rpc = Mock(request=AsyncMock(), stop=AsyncMock())
    entered = asyncio.Event()
    release = asyncio.Event()

    async def connect():
        client._client = rpc

    async def refuse():
        entered.set()
        await release.wait()
        raise RuntimeError("handshake refused")

    client._connect_to_server.side_effect = connect
    client._verify_protocol_version.side_effect = refuse
    start = asyncio.create_task(client.start())
    await asyncio.wait_for(entered.wait(), timeout=1)
    call = asyncio.create_task(getattr(client, method)())
    await _settle(call)
    release.set()

    with pytest.raises(RuntimeError, match="handshake refused"):
        await asyncio.wait_for(start, timeout=1)
    with pytest.raises(RuntimeError, match="Client failed to start: handshake refused"):
        await asyncio.wait_for(call, timeout=1)
    # With no start in progress, a status request neither waits nor starts the client.
    with pytest.raises(RuntimeError, match="^Client not connected$"):
        await client.get_status()
    client._start_cli_server.assert_awaited_once()
    rpc.request.assert_not_awaited()


async def test_status_call_keeps_original_start_failure_when_retry_is_queued(client):
    entered = asyncio.Event()
    release = asyncio.Event()
    failure = RuntimeError("first handshake refused")
    rpc = Mock(
        request=AsyncMock(return_value={"version": "1.2.3", "protocolVersion": 3}),
        stop=AsyncMock(),
    )

    async def connect():
        client._client = rpc

    async def refuse():
        entered.set()
        await release.wait()
        raise failure

    client._connect_to_server.side_effect = connect

    async def verify():
        if not entered.is_set():
            await refuse()

    client._verify_protocol_version.side_effect = verify
    start = asyncio.create_task(client.start())
    await asyncio.wait_for(entered.wait(), timeout=1)
    retry = asyncio.create_task(client.start())
    await asyncio.sleep(0)
    call = asyncio.create_task(client.get_status())
    await _settle(call)
    release.set()
    try:
        with pytest.raises(RuntimeError, match="first handshake refused"):
            await asyncio.wait_for(start, timeout=1)
        await asyncio.wait_for(retry, timeout=1)
        with pytest.raises(
            RuntimeError, match="Client failed to start: first handshake refused"
        ) as exc:
            await asyncio.wait_for(call, timeout=1)
        assert exc.value.__cause__ is failure
        assert (await client.get_status()).version == "1.2.3"
    finally:
        for task in (start, retry, call):
            task.cancel()
        await asyncio.gather(start, retry, call, return_exceptions=True)
        await client.force_stop()


async def test_cancelling_status_waiter_preserves_start_for_other_requests(client):
    entered = asyncio.Event()
    release = asyncio.Event()
    rpc = Mock(
        request=AsyncMock(return_value={"version": "1.2.3", "protocolVersion": 3}),
        stop=AsyncMock(),
    )

    async def connect():
        client._client = rpc
        entered.set()
        await release.wait()

    client._connect_to_server.side_effect = connect
    start = asyncio.create_task(client.start())
    await asyncio.wait_for(entered.wait(), timeout=1)
    cancelled = asyncio.create_task(client.get_status())
    remaining = asyncio.create_task(client.get_status())
    try:
        await _settle(cancelled)
        cancelled.cancel()
        with pytest.raises(asyncio.CancelledError):
            await cancelled
        assert not start.done()
        release.set()
        await asyncio.wait_for(start, timeout=1)
        assert (await asyncio.wait_for(remaining, timeout=1)).version == "1.2.3"
        rpc.request.assert_awaited_once_with("status.get", {})
    finally:
        release.set()
        for task in (start, cancelled, remaining):
            task.cancel()
        await asyncio.gather(start, cancelled, remaining, return_exceptions=True)
        await client.force_stop()


@pytest.mark.parametrize("method", list(STATUS_CALLS))
async def test_status_call_follows_replacement_start_queued_after_success(client, method):
    wire_method, params, response = STATUS_CALLS[method]
    entered = asyncio.Event()
    release = asyncio.Event()
    replacing = asyncio.Event()
    release_replacement = asyncio.Event()
    old_rpc = Mock(request=AsyncMock(), stop=AsyncMock())
    new_rpc = Mock(request=AsyncMock(return_value=response), stop=AsyncMock())
    connects = 0

    async def connect():
        nonlocal connects
        connects += 1
        if connects == 1:
            client._client = old_rpc
            entered.set()
            await release.wait()
        else:
            client._client = new_rpc

    client._connect_to_server.side_effect = connect

    real_start = client._start
    starts = 0

    async def start_then_lose_connection():
        nonlocal starts
        starts += 1
        await real_start()
        if starts == 1:
            # The reader thread closes the first connection right after startup succeeds.
            client._state = "disconnected"
            client._connection_lost = True

    client._start = start_then_lose_connection
    real_force_stop = client.force_stop

    async def gated_force_stop():
        replacing.set()
        await release_replacement.wait()
        client._client = None

    client.force_stop = gated_force_stop
    first = asyncio.create_task(client.start())
    await asyncio.wait_for(entered.wait(), timeout=1)
    second = asyncio.create_task(client.start())
    await asyncio.sleep(0)
    call = asyncio.create_task(getattr(client, method)())
    await _settle(call)
    release.set()
    try:
        await asyncio.wait_for(replacing.wait(), timeout=1)
        await _settle(call)
        assert not call.done()
        release_replacement.set()
        await asyncio.wait_for(first, timeout=1)
        await asyncio.wait_for(second, timeout=1)
        await asyncio.wait_for(call, timeout=1)
        old_rpc.request.assert_not_awaited()
        new_rpc.request.assert_awaited_once_with(wire_method, params)
    finally:
        release.set()
        release_replacement.set()
        for task in (first, second, call):
            task.cancel()
        await asyncio.gather(first, second, call, return_exceptions=True)
        client.force_stop = real_force_stop
        await client.force_stop()


async def test_stop_and_force_stop_do_not_wait_for_a_model_callback(client):
    started = asyncio.Event()
    release = asyncio.Event()
    calls = 0

    async def on_list_models():
        nonlocal calls
        calls += 1
        started.set()
        await release.wait()
        return []

    client._on_list_models = on_list_models
    listing = asyncio.create_task(client.list_models())
    try:
        await asyncio.wait_for(started.wait(), timeout=1)
        await asyncio.wait_for(client.force_stop(), timeout=1)
        await asyncio.wait_for(client.stop(), timeout=1)
        release.set()
        await asyncio.wait_for(listing, timeout=1)
        # The fetch that straddled the stop must not repopulate the cleared cache.
        await asyncio.wait_for(client.list_models(), timeout=1)
        assert calls == 2
    finally:
        release.set()
        listing.cancel()
        await asyncio.gather(listing, return_exceptions=True)


async def test_list_models_uses_the_replacement_client_after_waiting_for_the_cache_lock(client):
    old_rpc = Mock(request=AsyncMock(side_effect=RuntimeError("closed")), stop=AsyncMock())
    new_rpc = Mock(request=AsyncMock(return_value={"models": []}), stop=AsyncMock())
    client._client = old_rpc
    await client._models_cache_lock.acquire()
    call = asyncio.create_task(client.list_models())
    try:
        await _settle(call)
        assert not call.done()
        # Another fetch is still holding the lock while the client is restarted.
        client._client = new_rpc
        client._models_cache_lock.release()
        assert await asyncio.wait_for(call, timeout=1) == []
        old_rpc.request.assert_not_awaited()
        new_rpc.request.assert_awaited_once_with("models.list", {})
    finally:
        if client._models_cache_lock.locked():
            client._models_cache_lock.release()
        call.cancel()
        await asyncio.gather(call, return_exceptions=True)


async def test_list_models_waits_for_restart_while_the_old_client_is_still_installed(client):
    fetching = asyncio.Event()
    release_fetch = asyncio.Event()
    stopping = asyncio.Event()
    release_stop = asyncio.Event()

    async def fetch(method, params):
        assert (method, params) == ("models.list", {})
        fetching.set()
        await release_fetch.wait()
        raise RuntimeError("closed transport")

    async def stop():
        stopping.set()
        await release_stop.wait()

    old_rpc = Mock(request=AsyncMock(side_effect=fetch), stop=AsyncMock(side_effect=stop))
    new_rpc = Mock(request=AsyncMock(return_value={"models": []}), stop=AsyncMock())
    client._client = old_rpc
    client._state = "connected"

    async def connect():
        client._client = new_rpc

    client._connect_to_server.side_effect = connect
    first = asyncio.create_task(client.list_models())
    queued = restart = None
    try:
        await asyncio.wait_for(fetching.wait(), timeout=1)
        queued = asyncio.create_task(client.list_models())
        await _settle(queued)
        assert not queued.done()
        client._state = "disconnected"
        client._connection_lost = True
        restart = asyncio.create_task(client.start())
        await asyncio.wait_for(stopping.wait(), timeout=1)
        assert client._client is old_rpc
        release_fetch.set()
        with pytest.raises(RuntimeError, match="closed transport"):
            await asyncio.wait_for(first, timeout=1)
        await _settle(queued)
        assert not queued.done()
        old_rpc.request.assert_awaited_once_with("models.list", {})
        release_stop.set()
        await asyncio.wait_for(restart, timeout=1)
        assert await asyncio.wait_for(queued, timeout=1) == []
        new_rpc.request.assert_awaited_once_with("models.list", {})
    finally:
        release_fetch.set()
        release_stop.set()
        tasks = [task for task in (first, queued, restart) if task is not None]
        for task in tasks:
            task.cancel()
        await asyncio.gather(*tasks, return_exceptions=True)
        await client.force_stop()


async def test_list_models_after_stop_does_not_wait_for_a_hung_model_callback(client):
    started = asyncio.Event()
    hang = asyncio.Event()
    calls = 0

    async def on_list_models():
        nonlocal calls
        calls += 1
        if calls == 1:
            started.set()
            await hang.wait()
        return []

    client._on_list_models = on_list_models
    hung = asyncio.create_task(client.list_models())
    queued = None
    try:
        await asyncio.wait_for(started.wait(), timeout=1)
        queued = asyncio.create_task(client.list_models())
        await _settle(queued)
        await asyncio.wait_for(client.force_stop(), timeout=1)
        # A call made after the stop must not queue behind the hung fetch.
        assert await asyncio.wait_for(client.list_models(), timeout=1) == []
        assert client._models_cache == []
        # The old queue drains once the hung fetch ends; its calls refetch rather than
        # caching a result from before the stop.
        hang.set()
        await asyncio.wait_for(hung, timeout=1)
        assert await asyncio.wait_for(queued, timeout=1) == []
    finally:
        hang.set()
        tasks = [t for t in (hung, queued) if t is not None]
        for task in tasks:
            task.cancel()
        await asyncio.gather(*tasks, return_exceptions=True)


async def test_legacy_ping_fallback_does_not_wait_for_its_own_start():
    client = CopilotClient(connection=RuntimeConnection.for_stdio(path="unused-cli"))

    async def request(method, params):
        if method == "connect":
            raise JsonRpcError(-32601, "Unhandled method connect")
        assert (method, params) == ("ping", {"message": None})
        return {
            "message": "pong",
            "timestamp": "2026-01-01T00:00:00Z",
            "protocolVersion": get_sdk_protocol_version(),
        }

    rpc = Mock(request=AsyncMock(side_effect=request), stop=AsyncMock())

    async def connect():
        client._client = rpc

    client._start_cli_server = AsyncMock()
    client._connect_to_server = AsyncMock(side_effect=connect)

    await asyncio.wait_for(client.start(), timeout=1)

    assert client._state == "connected"
    assert [call.args[0] for call in rpc.request.await_args_list] == ["connect", "ping"]


@pytest.mark.parametrize("method", ["stop", "force_stop"])
async def test_shutdown_cancels_pending_inference_requests(client, method):
    adapter = Mock()
    client._llm_inference_adapter = adapter

    await getattr(client, method)()

    adapter.cancel_pending.assert_called_once_with()
