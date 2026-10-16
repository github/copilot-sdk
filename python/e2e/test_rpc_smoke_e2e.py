# Copyright (c) Microsoft Corporation. All rights reserved.

"""Representative generated RPC round trips over the real SDK transport."""

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot.rpc import PingRequest
from copilot.session import PermissionHandler
from copilot.session_events import SessionMode

from .testharness import CLI_PATH

pytestmark = pytest.mark.asyncio(loop_scope="module")


async def test_generated_rpc_round_trip():
    client = CopilotClient(connection=RuntimeConnection.for_stdio(path=CLI_PATH))
    try:
        await client.start()

        pong = await client.rpc.ping(PingRequest(message="typed rpc test"))
        assert pong.message == "pong: typed rpc test"
        assert pong.timestamp is not None

        session = await client.create_session(on_permission_request=PermissionHandler.approve_all)
        try:
            assert await session.rpc.mode.get() == SessionMode.INTERACTIVE
        finally:
            await session.disconnect()
    finally:
        await client.force_stop()
