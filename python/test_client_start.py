"""Startup concurrency regressions without a live CLI runtime."""

import asyncio
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
