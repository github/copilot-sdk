"""
E2E coverage for server-scoped remote-control RPC methods.

Mirrors ``dotnet/test/E2E/RpcServerRemoteControlE2ETests.cs`` (snapshot
category ``rpc_server_remote_control``).
"""

from __future__ import annotations

import contextlib
import uuid

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot.rpc import (
    RemoteControlConfig,
    RemoteControlStatusOff,
    SessionsSetRemoteControlSteeringRequest,
    SessionsStartRemoteControlRequest,
    SessionsStopRemoteControlRequest,
    SessionsTransferRemoteControlRequest,
)

from .testharness import DEFAULT_GITHUB_TOKEN, E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _create_dedicated_client(ctx: E2ETestContext) -> CopilotClient:
    return CopilotClient(
        connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
        working_directory=ctx.work_dir,
        env=ctx.get_env(),
        github_token=DEFAULT_GITHUB_TOKEN,
    )


async def _stop_client(client: CopilotClient) -> None:
    with contextlib.suppress(ExceptionGroup):
        await client.stop()


class TestRpcServerRemoteControl:
    async def test_should_report_remote_control_status_as_off(self, ctx: E2ETestContext):
        client = _create_dedicated_client(ctx)
        try:
            await client.start()

            result = await client.rpc.sessions.get_remote_control_status()

            assert isinstance(result.status, RemoteControlStatusOff)
            assert result.status.state == "off"
        finally:
            await _stop_client(client)

    async def test_should_treat_set_steering_as_no_op_when_off(self, ctx: E2ETestContext):
        client = _create_dedicated_client(ctx)
        try:
            await client.start()

            result = await client.rpc.sessions.set_remote_control_steering(
                SessionsSetRemoteControlSteeringRequest(enabled=False)
            )

            assert isinstance(result.status, RemoteControlStatusOff)
        finally:
            await _stop_client(client)

    async def test_should_report_not_stopped_when_remote_control_is_off(self, ctx: E2ETestContext):
        client = _create_dedicated_client(ctx)
        try:
            await client.start()

            result = await client.rpc.sessions.stop_remote_control(
                SessionsStopRemoteControlRequest()
            )

            assert result.stopped is False
            assert isinstance(result.status, RemoteControlStatusOff)
        finally:
            await _stop_client(client)

    async def test_should_reject_transfer_when_off_with_compare_and_swap(self, ctx: E2ETestContext):
        client = _create_dedicated_client(ctx)
        try:
            await client.start()

            result = await client.rpc.sessions.transfer_remote_control(
                SessionsTransferRemoteControlRequest(
                    to_session_id=f"rc-to-{uuid.uuid4().hex}",
                    expected_from_session_id=f"rc-from-{uuid.uuid4().hex}",
                )
            )

            assert result.transferred is False
            assert isinstance(result.status, RemoteControlStatusOff)
        finally:
            await _stop_client(client)

    async def test_should_reach_runtime_when_starting_remote_control_for_unknown_session(
        self, ctx: E2ETestContext
    ):
        client = _create_dedicated_client(ctx)
        try:
            await client.start()

            try:
                with pytest.raises(Exception) as excinfo:
                    await client.rpc.sessions.start_remote_control(
                        SessionsStartRemoteControlRequest(
                            session_id=f"missing-session-{uuid.uuid4().hex}",
                            config=RemoteControlConfig(
                                explicit=False,
                                remote=False,
                                silent=True,
                                steerable=False,
                            ),
                        )
                    )
                message = str(excinfo.value)
                assert "Unhandled method".lower() not in message.lower()
                assert "session" in message.lower() or "remote" in message.lower(), message
            finally:
                with contextlib.suppress(Exception):
                    await client.rpc.sessions.stop_remote_control(
                        SessionsStopRemoteControlRequest(force=True)
                    )
        finally:
            await _stop_client(client)
