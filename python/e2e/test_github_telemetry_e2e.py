"""Live CLI E2E coverage for forwarded GitHub telemetry notifications."""

from __future__ import annotations

import asyncio

import pytest

from copilot import CopilotClient, GitHubTelemetryNotification, RuntimeConnection
from copilot.session import PermissionHandler

from .testharness import DEFAULT_GITHUB_TOKEN, E2ETestContext
from .testharness.context import get_cli_path_for_tests

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestGitHubTelemetryE2E:
    async def test_should_receive_session_start_github_telemetry(self, ctx: E2ETestContext):
        received: asyncio.Future[GitHubTelemetryNotification] = (
            asyncio.get_running_loop().create_future()
        )

        def on_github_telemetry(notification: GitHubTelemetryNotification) -> None:
            if not received.done():
                received.set_result(notification)

        client = CopilotClient(
            connection=RuntimeConnection.for_stdio(path=get_cli_path_for_tests(), args=()),
            working_directory=ctx.work_dir,
            env=ctx.get_env(),
            github_token=DEFAULT_GITHUB_TOKEN,
            on_github_telemetry=on_github_telemetry,
        )

        session = None
        try:
            await client.start()
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )

            notification = await asyncio.wait_for(received, timeout=30.0)
            assert isinstance(notification.session_id, str)
            assert notification.session_id
            assert isinstance(notification.restricted, bool)
            assert notification.event is not None
            assert isinstance(notification.event.kind, str)
        finally:
            try:
                if session is not None:
                    await session.disconnect()
            finally:
                await client.stop()
