"""
E2E coverage for ``session.shell.*`` and ``session.fleet.*`` RPCs.

Mirrors ``dotnet/test/RpcShellAndFleetTests.cs`` (snapshot category
``rpc_shell_and_fleet``).
"""

from __future__ import annotations

import asyncio
import sys
import uuid
from pathlib import Path

import pytest

from copilot.generated.rpc import FleetStartRequest, ShellExecRequest, ShellKillRequest
from copilot.generated.session_events import (
    AssistantMessageData,
    SessionErrorData,
    ToolExecutionCompleteData,
    ToolExecutionStartData,
    UserMessageData,
)
from copilot.session import PermissionHandler
from copilot.tools import Tool, ToolInvocation, ToolResult

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _write_file_command(marker_path: Path, marker: str) -> str:
    if sys.platform == "win32":
        return (
            f"powershell -NoLogo -NoProfile -Command "
            f"\"Set-Content -LiteralPath '{marker_path}' -Value '{marker}'\""
        )
    return f"sh -c \"printf '%s' '{marker}' > '{marker_path}'\""


async def _wait_for_file_text(path: Path, expected: str, *, timeout: float = 30.0) -> None:
    deadline = asyncio.get_event_loop().time() + timeout
    while asyncio.get_event_loop().time() < deadline:
        if path.exists():
            text = path.read_text(encoding="utf-8")
            if expected in text:
                return
        await asyncio.sleep(0.1)
    raise TimeoutError(f"Timed out waiting for shell command to write '{expected}' to '{path}'.")


class TestRpcShellAndFleet:
    async def test_should_execute_shell_command(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        marker_path = Path(ctx.work_dir) / f"shell-rpc-{uuid.uuid4().hex}.txt"
        marker = "copilot-sdk-shell-rpc"

        result = await session.rpc.shell.exec(
            ShellExecRequest(command=_write_file_command(marker_path, marker), cwd=ctx.work_dir)
        )
        assert (result.process_id or "").strip()
        await _wait_for_file_text(marker_path, marker)

        await session.disconnect()

    async def test_should_kill_shell_process(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        if sys.platform == "win32":
            command = 'powershell -NoLogo -NoProfile -Command "Start-Sleep -Seconds 30"'
        else:
            command = "sleep 30"

        exec_result = await session.rpc.shell.exec(ShellExecRequest(command=command))
        assert (exec_result.process_id or "").strip()

        kill_result = await session.rpc.shell.kill(
            ShellKillRequest(process_id=exec_result.process_id)
        )
        assert kill_result.killed

        await session.disconnect()

    async def test_should_start_fleet_and_complete_custom_tool_task(self, ctx: E2ETestContext):
        marker_path = Path(ctx.work_dir) / f"fleet-rpc-{uuid.uuid4().hex}.txt"
        marker = "copilot-sdk-fleet-rpc"
        tool_name = "record_fleet_completion"

        def record_fleet_completion(invocation: ToolInvocation) -> ToolResult:
            args = invocation.arguments or {}
            content = str(args.get("content", ""))
            marker_path.write_text(content, encoding="utf-8")
            return ToolResult(text_result_for_llm=content)

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            tools=[
                Tool(
                    name=tool_name,
                    description="Records completion of the fleet validation task.",
                    parameters={
                        "type": "object",
                        "properties": {"content": {"type": "string", "description": "Marker"}},
                        "required": ["content"],
                    },
                    handler=record_fleet_completion,
                )
            ],
        )

        prompt = (
            f"Use the {tool_name} tool with content '{marker}', "
            "then report that the fleet task is complete."
        )
        result = await session.rpc.fleet.start(FleetStartRequest(prompt=prompt))
        assert result.started
        await _wait_for_file_text(marker_path, marker)

        async def _wait_for_messages(timeout: float = 120.0):
            deadline = asyncio.get_event_loop().time() + timeout
            while asyncio.get_event_loop().time() < deadline:
                messages = await session.get_messages()
                if any(
                    isinstance(m.data, AssistantMessageData)
                    and "fleet task" in (m.data.content or "").lower()
                    for m in messages
                ):
                    return messages
                if any(isinstance(m.data, SessionErrorData) for m in messages):
                    raise RuntimeError("Session error while waiting for fleet completion")
                await asyncio.sleep(0.25)
            raise TimeoutError("Timed out waiting for fleet-mode assistant reply.")

        messages = await _wait_for_messages()
        assert any(
            isinstance(m.data, UserMessageData) and prompt in (m.data.content or "")
            for m in messages
        )
        assert any(
            isinstance(m.data, ToolExecutionStartData) and m.data.tool_name == tool_name
            for m in messages
        )
        assert any(
            isinstance(m.data, ToolExecutionCompleteData)
            and m.data.success
            and (
                getattr(m.data, "result", None) is not None
                and marker in (m.data.result.content or "")
            )
            for m in messages
        )
        assert any(
            isinstance(m.data, AssistantMessageData)
            and "fleet task" in (m.data.content or "").lower()
            for m in messages
        )

        await session.disconnect()
