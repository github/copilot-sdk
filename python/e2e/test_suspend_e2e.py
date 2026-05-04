"""
E2E coverage for the ``session.suspend`` RPC.

Suspend cancels in-flight work, rejects pending external tool requests, drains
notifications, and flushes state so a later client can resume consistently.
"""

from __future__ import annotations

import asyncio
import inspect
import os
from typing import Any

import pytest

from copilot import CopilotClient
from copilot.client import ExternalServerConfig, SubprocessConfig
from copilot.session import PermissionHandler, PermissionRequestResult
from copilot.tools import Tool, ToolInvocation, ToolResult

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

SUSPEND_TIMEOUT = 60.0


def _make_subprocess_client(ctx: E2ETestContext, *, use_stdio: bool = True) -> CopilotClient:
    github_token = (
        "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
    )
    return CopilotClient(
        SubprocessConfig(
            cli_path=ctx.cli_path,
            cwd=ctx.work_dir,
            env=ctx.get_env(),
            github_token=github_token,
            use_stdio=use_stdio,
            tcp_connection_token="py-tcp-shared-test-token",
        )
    )


def _make_tool(name: str, handler) -> Tool:
    async def wrapped(invocation: ToolInvocation) -> ToolResult:
        args = invocation.arguments or {}
        result = handler(args)
        if inspect.isawaitable(result):
            result = await result
        return ToolResult(text_result_for_llm=str(result))

    return Tool(
        name=name,
        description="Transforms a value",
        parameters={
            "type": "object",
            "properties": {
                "value": {
                    "type": "string",
                    "description": "Value to transform",
                }
            },
            "required": ["value"],
        },
        handler=wrapped,
    )


async def _safe_force_stop(client: CopilotClient) -> None:
    try:
        await client.stop()
    except Exception:
        await client.force_stop()


async def _safe_disconnect(session: Any) -> None:
    try:
        await session.disconnect()
    except Exception:
        # Suspend can leave the SDK-side session already closed; ignore teardown races.
        pass


class TestSuspend:
    async def test_should_suspend_idle_session_without_throwing(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            await session.send_and_wait("Reply with: SUSPEND_IDLE_OK")
            await asyncio.wait_for(session.rpc.suspend(), timeout=SUSPEND_TIMEOUT)
        finally:
            await _safe_disconnect(session)

    async def test_should_allow_resume_and_continue_conversation_after_suspend(
        self, ctx: E2ETestContext
    ):
        server = _make_subprocess_client(ctx, use_stdio=False)
        await server.start()
        try:
            cli_url = f"localhost:{server.actual_port}"
            session_id: str

            first_client = CopilotClient(
                ExternalServerConfig(url=cli_url, tcp_connection_token="py-tcp-shared-test-token")
            )
            try:
                session1 = await first_client.create_session(
                    on_permission_request=PermissionHandler.approve_all
                )
                session_id = session1.session_id

                await session1.send_and_wait(
                    "Remember the magic word: SUSPENSE. Reply with: SUSPEND_TURN_ONE"
                )
                await asyncio.wait_for(session1.rpc.suspend(), timeout=SUSPEND_TIMEOUT)
                await session1.disconnect()
            finally:
                await _safe_force_stop(first_client)

            resumed_client = CopilotClient(
                ExternalServerConfig(url=cli_url, tcp_connection_token="py-tcp-shared-test-token")
            )
            try:
                session2 = await resumed_client.resume_session(
                    session_id,
                    on_permission_request=PermissionHandler.approve_all,
                )
                try:
                    follow_up = await session2.send_and_wait(
                        "What was the magic word I asked you to remember? Reply with just the word."
                    )
                    assert follow_up is not None
                    assert "SUSPENSE" in (follow_up.data.content or "").upper()
                finally:
                    await _safe_disconnect(session2)
            finally:
                await _safe_force_stop(resumed_client)
        finally:
            await _safe_force_stop(server)

    async def test_should_cancel_pending_permission_request_when_suspending(
        self, ctx: E2ETestContext
    ):
        captured_request: asyncio.Future = asyncio.get_event_loop().create_future()
        release_permission_handler: asyncio.Future = asyncio.get_event_loop().create_future()
        tool_invoked = False

        async def hold_permission(request, _invocation):
            if not captured_request.done():
                captured_request.set_result(request)
            return await release_permission_handler

        def tool_handler(args):
            nonlocal tool_invoked
            tool_invoked = True
            return f"SHOULD_NOT_RUN_{args.get('value', '')}"

        session = await ctx.client.create_session(
            on_permission_request=hold_permission,
            tools=[_make_tool("suspend_cancel_permission_tool", tool_handler)],
        )
        try:
            await session.send(
                "Use suspend_cancel_permission_tool with value 'omega', then reply with the result."
            )
            await asyncio.wait_for(captured_request, timeout=SUSPEND_TIMEOUT)

            await asyncio.wait_for(session.rpc.suspend(), timeout=SUSPEND_TIMEOUT)

            assert not tool_invoked
        finally:
            if not release_permission_handler.done():
                release_permission_handler.set_result(
                    PermissionRequestResult(kind="user-not-available")
                )
            await _safe_disconnect(session)

    async def test_should_reject_pending_external_tool_when_suspending(self, ctx: E2ETestContext):
        tool_started: asyncio.Future = asyncio.get_event_loop().create_future()
        external_tool_requested: asyncio.Future = asyncio.get_event_loop().create_future()
        release_tool: asyncio.Future = asyncio.get_event_loop().create_future()

        async def blocking_tool(args):
            value = args["value"]
            if not tool_started.done():
                tool_started.set_result(value)
            return await release_tool

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            tools=[_make_tool("suspend_reject_external_tool", blocking_tool)],
        )
        unsubscribe = session.on(
            lambda event: (
                external_tool_requested.set_result(event)
                if (
                    not external_tool_requested.done()
                    and event.type.value == "external_tool.requested"
                    and event.data.tool_name == "suspend_reject_external_tool"
                )
                else None
            )
        )
        try:
            await session.send(
                "Use suspend_reject_external_tool with value 'sigma', then reply with the result."
            )
            requested_event, started_value = await asyncio.wait_for(
                asyncio.gather(external_tool_requested, tool_started),
                timeout=SUSPEND_TIMEOUT,
            )
            assert requested_event.data.request_id
            assert started_value == "sigma"

            await asyncio.wait_for(session.rpc.suspend(), timeout=SUSPEND_TIMEOUT)
        finally:
            unsubscribe()
            if not release_tool.done():
                release_tool.set_result("RELEASED_AFTER_SUSPEND")
            await _safe_disconnect(session)
