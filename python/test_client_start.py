"""Startup concurrency regressions without a live CLI runtime."""

import asyncio
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
    old_owner = Mock(stdin=None)
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


@pytest.mark.parametrize("method", ["stop", "force_stop"])
async def test_shutdown_cancels_pending_inference_requests(client, method):
    adapter = Mock()
    client._llm_inference_adapter = adapter

    await getattr(client, method)()

    adapter.cancel_pending.assert_called_once_with()
