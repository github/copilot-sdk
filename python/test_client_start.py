"""Startup concurrency regressions without a live CLI runtime."""

import asyncio
from unittest.mock import AsyncMock

import pytest

from copilot import CopilotClient, RuntimeConnection


@pytest.fixture
def client():
    client = CopilotClient(connection=RuntimeConnection.for_stdio(path="unused-cli"))
    client._start_cli_server = AsyncMock()
    client._connect_to_server = AsyncMock()
    client._verify_protocol_version = AsyncMock()
    return client


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
            await second
        assert not first.done()
    finally:
        release.set()
        await asyncio.wait_for(first, timeout=1)

    await client.start()
    client._start_cli_server.assert_awaited_once()
    client._verify_protocol_version.assert_awaited_once()
