"""
E2E coverage for session-scoped user-requested shell RPC methods.

Mirrors ``dotnet/test/E2E/RpcShellUserRequestedE2ETests.cs`` (snapshot
category ``rpc_shell_user_requested``).
"""

from __future__ import annotations

import asyncio
import contextlib
import sys
import uuid
from pathlib import Path

import pytest

from copilot.rpc import ShellCancelUserRequestedRequest, ShellExecuteUserRequestedRequest
from copilot.session import PermissionHandler

from .testharness import E2ETestContext, wait_for_condition

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _create_marker_then_sleep_command(marker_path: Path, seconds: int) -> str:
    if sys.platform == "win32":
        return (
            f"Set-Content -LiteralPath '{marker_path}' -Value 'running'; "
            f"Start-Sleep -Seconds {seconds}"
        )
    return f"printf '%s' running > '{marker_path}'; sleep {seconds}"


class TestRpcShellUserRequested:
    async def test_should_execute_user_requested_shell_command(self, ctx: E2ETestContext):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            marker = f"copilotusershell{uuid.uuid4().hex}"
            request_id = f"req-{uuid.uuid4().hex}"

            result = await session.rpc.shell.execute_user_requested(
                ShellExecuteUserRequestedRequest(command=f"echo {marker}", request_id=request_id)
            )

            assert result.success is True, f"Expected success. Error: {result.error}"
            assert result.exit_code == 0
            assert marker in result.output
            assert (result.tool_call_id or "").strip()

    async def test_should_cancel_user_requested_shell_command(self, ctx: E2ETestContext):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            missing = await session.rpc.shell.cancel_user_requested(
                ShellCancelUserRequestedRequest(request_id=f"missing-{uuid.uuid4().hex}")
            )
            assert missing.cancelled is False

            request_id = f"req-{uuid.uuid4().hex}"
            marker_path = Path(ctx.home_dir) / f"shell-cancel-{uuid.uuid4().hex}.txt"
            execute_task = asyncio.create_task(
                session.rpc.shell.execute_user_requested(
                    ShellExecuteUserRequestedRequest(
                        request_id=request_id,
                        command=_create_marker_then_sleep_command(marker_path, seconds=60),
                    )
                )
            )

            try:
                await wait_for_condition(
                    marker_path.exists,
                    timeout=30.0,
                    poll_interval=0.1,
                    timeout_message=(
                        f"Timed out waiting for the shell command to create '{marker_path}'."
                    ),
                )

                async def cancel_took_effect() -> bool:
                    result = await session.rpc.shell.cancel_user_requested(
                        ShellCancelUserRequestedRequest(request_id=request_id)
                    )
                    return result.cancelled

                await wait_for_condition(
                    cancel_took_effect,
                    timeout=15.0,
                    poll_interval=0.1,
                    timeout_message=(
                        "Timed out waiting for the user-requested shell command "
                        "to become cancellable."
                    ),
                )

                await wait_for_condition(
                    execute_task.done,
                    timeout=30.0,
                    poll_interval=0.1,
                    timeout_message="Timed out waiting for cancelled shell command to finish.",
                )
                result = await execute_task
                assert result.success is False
            finally:
                if not execute_task.done():
                    with contextlib.suppress(Exception):
                        await session.rpc.shell.cancel_user_requested(
                            ShellCancelUserRequestedRequest(request_id=request_id)
                        )
                    with contextlib.suppress(Exception):
                        await wait_for_condition(
                            execute_task.done,
                            timeout=30.0,
                            poll_interval=0.1,
                            timeout_message="Timed out draining shell command task.",
                        )
                    if not execute_task.done():
                        execute_task.cancel()
                with contextlib.suppress(OSError):
                    marker_path.unlink(missing_ok=True)
