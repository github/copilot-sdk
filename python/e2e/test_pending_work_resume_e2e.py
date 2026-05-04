"""
E2E coverage for the ``continue_pending_work`` resume flow.

Mirrors ``dotnet/test/PendingWorkResumeTests.cs``: starts a session that gets
suspended mid-turn (with a pending permission request, a pending external tool
request, or parallel pending external tools), then resumes it on a new client
with ``continue_pending_work=True`` and confirms the runtime hands the new
client the original work to satisfy.
"""

from __future__ import annotations

import asyncio
import os
from typing import Any

import pytest

from copilot import CopilotClient
from copilot.client import ExternalServerConfig, SubprocessConfig
from copilot.generated.rpc import HandlePendingToolCallRequest, PermissionDecisionRequest
from copilot.session import PermissionHandler, PermissionRequestResult
from copilot.tools import Tool, ToolInvocation, ToolResult

from .testharness import E2ETestContext, get_final_assistant_message

pytestmark = pytest.mark.asyncio(loop_scope="module")

PENDING_WORK_TIMEOUT = 60.0


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


def _make_pending_tool(name: str, handler) -> Tool:
    """Wrap an args-style handler ``handler(dict) -> str | Awaitable[str]`` as a Tool."""

    async def wrapped(invocation: ToolInvocation) -> ToolResult:
        args = invocation.arguments or {}
        result = handler(args)
        if asyncio.iscoroutine(result):
            result = await result
        return ToolResult(text_result_for_llm=str(result))

    return Tool(
        name=name,
        description="Looks up a value after resumption",
        parameters={
            "type": "object",
            "properties": {
                "value": {
                    "type": "string",
                    "description": "Value to look up",
                }
            },
            "required": ["value"],
        },
        handler=wrapped,
    )


async def _wait_for_external_tool_requests(
    session, tool_names: list[str], timeout: float = PENDING_WORK_TIMEOUT
) -> dict[str, Any]:
    """Wait for ExternalToolRequested events for the named tools."""
    expected = set(tool_names)
    seen: dict[str, Any] = {}
    completed: asyncio.Future = asyncio.get_event_loop().create_future()

    def on_event(event):
        if completed.done():
            return
        if event.type.value == "external_tool.requested":
            tool_name = event.data.tool_name
            if tool_name in expected and tool_name not in seen:
                seen[tool_name] = event
                if len(seen) == len(expected):
                    completed.set_result(dict(seen))
        elif event.type.value == "session.error":
            msg = event.data.message or "session error"
            completed.set_exception(RuntimeError(msg))

    unsubscribe = session.on(on_event)
    try:
        return await asyncio.wait_for(completed, timeout=timeout)
    finally:
        unsubscribe()


async def _wait_for_permission_request(session, timeout: float = PENDING_WORK_TIMEOUT) -> Any:
    completed: asyncio.Future = asyncio.get_event_loop().create_future()

    def on_event(event):
        if completed.done():
            return
        if event.type.value == "permission.requested":
            completed.set_result(event)
        elif event.type.value == "session.error":
            msg = event.data.message or "session error"
            completed.set_exception(RuntimeError(msg))

    unsubscribe = session.on(on_event)
    try:
        return await asyncio.wait_for(completed, timeout=timeout)
    finally:
        unsubscribe()


async def _safe_force_stop(client: CopilotClient) -> None:
    try:
        await client.stop()
    except Exception:
        await client.force_stop()


class TestPendingWorkResume:
    async def test_should_continue_pending_permission_request_after_resume(
        self, ctx: E2ETestContext
    ):
        # Spawn a TCP server that both the suspended and resumed clients connect to.
        server = _make_subprocess_client(ctx, use_stdio=False)
        await server.start()
        try:
            cli_url = f"localhost:{server.actual_port}"

            release_original: asyncio.Future = asyncio.get_event_loop().create_future()
            captured_request: asyncio.Future = asyncio.get_event_loop().create_future()
            resumed_tool_invoked = False

            async def hold_permission(request, _invocation):
                if not captured_request.done():
                    captured_request.set_result(request)
                return await release_original

            def original_tool_handler(args):
                return f"ORIGINAL_SHOULD_NOT_RUN_{args.get('value', '')}"

            suspended_client = CopilotClient(
                ExternalServerConfig(url=cli_url, tcp_connection_token="py-tcp-shared-test-token")
            )
            session1 = await suspended_client.create_session(
                on_permission_request=hold_permission,
                tools=[_make_pending_tool("resume_permission_tool", original_tool_handler)],
            )
            session_id = session1.session_id

            try:
                permission_event_task = asyncio.create_task(_wait_for_permission_request(session1))
                await session1.send(
                    "Use resume_permission_tool with value 'alpha', then reply with the result."
                )
                _ = await captured_request
                permission_event = await permission_event_task

                # Force-stop the suspended client without releasing the in-flight
                # permission so the request remains pending in the runtime.
                await suspended_client.force_stop()

                def resumed_tool_handler(args):
                    nonlocal resumed_tool_invoked
                    resumed_tool_invoked = True
                    return f"PERMISSION_RESUMED_{args['value'].upper()}"

                resumed_client = CopilotClient(
                    ExternalServerConfig(
                        url=cli_url, tcp_connection_token="py-tcp-shared-test-token"
                    )
                )
                try:
                    session2 = await resumed_client.resume_session(
                        session_id,
                        on_permission_request=lambda req, inv: PermissionRequestResult(
                            kind="user-not-available"
                        ),
                        continue_pending_work=True,
                        tools=[_make_pending_tool("resume_permission_tool", resumed_tool_handler)],
                    )

                    permission_result = (
                        await session2.rpc.permissions.handle_pending_permission_request(
                            PermissionDecisionRequest.from_dict(
                                {
                                    "requestId": permission_event.data.request_id,
                                    "result": {"kind": "approve-once"},
                                }
                            )
                        )
                    )
                    assert permission_result.success

                    answer = await get_final_assistant_message(
                        session2, timeout=PENDING_WORK_TIMEOUT
                    )

                    assert resumed_tool_invoked
                    assert "PERMISSION_RESUMED_ALPHA" in (answer.data.content or "")
                    await session2.disconnect()
                finally:
                    await _safe_force_stop(resumed_client)
            finally:
                if not release_original.done():
                    release_original.set_result(PermissionRequestResult(kind="user-not-available"))
        finally:
            await _safe_force_stop(server)

    async def test_should_continue_pending_external_tool_request_after_resume(
        self, ctx: E2ETestContext
    ):
        server = _make_subprocess_client(ctx, use_stdio=False)
        await server.start()
        try:
            cli_url = f"localhost:{server.actual_port}"

            tool_started: asyncio.Future = asyncio.get_event_loop().create_future()
            release_original: asyncio.Future = asyncio.get_event_loop().create_future()

            async def blocking_external_tool(args):
                value = args["value"]
                if not tool_started.done():
                    tool_started.set_result(value)
                return await release_original

            suspended_client = CopilotClient(
                ExternalServerConfig(url=cli_url, tcp_connection_token="py-tcp-shared-test-token")
            )
            session1 = await suspended_client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                tools=[_make_pending_tool("resume_external_tool", blocking_external_tool)],
            )
            session_id = session1.session_id

            try:
                tool_request_task = asyncio.create_task(
                    _wait_for_external_tool_requests(session1, ["resume_external_tool"])
                )
                await session1.send(
                    "Use resume_external_tool with value 'beta', then reply with the result."
                )
                tool_events = await tool_request_task
                assert (await asyncio.wait_for(tool_started, PENDING_WORK_TIMEOUT)) == "beta"

                await suspended_client.force_stop()

                resumed_client = CopilotClient(
                    ExternalServerConfig(
                        url=cli_url, tcp_connection_token="py-tcp-shared-test-token"
                    )
                )
                try:
                    session2 = await resumed_client.resume_session(
                        session_id,
                        on_permission_request=PermissionHandler.approve_all,
                        continue_pending_work=True,
                    )

                    tool_result = await session2.rpc.tools.handle_pending_tool_call(
                        HandlePendingToolCallRequest(
                            request_id=tool_events["resume_external_tool"].data.request_id,
                            result="EXTERNAL_RESUMED_BETA",
                        )
                    )
                    assert tool_result.success

                    answer = await get_final_assistant_message(
                        session2, timeout=PENDING_WORK_TIMEOUT
                    )
                    assert "EXTERNAL_RESUMED_BETA" in (answer.data.content or "")

                    await session2.disconnect()
                finally:
                    await _safe_force_stop(resumed_client)
            finally:
                if not release_original.done():
                    release_original.set_result("ORIGINAL_SHOULD_NOT_WIN")
        finally:
            await _safe_force_stop(server)

    async def test_should_continue_parallel_pending_external_tool_requests_after_resume(
        self, ctx: E2ETestContext
    ):
        server = _make_subprocess_client(ctx, use_stdio=False)
        await server.start()
        try:
            cli_url = f"localhost:{server.actual_port}"

            tool_a_started: asyncio.Future = asyncio.get_event_loop().create_future()
            tool_b_started: asyncio.Future = asyncio.get_event_loop().create_future()
            release_a: asyncio.Future = asyncio.get_event_loop().create_future()
            release_b: asyncio.Future = asyncio.get_event_loop().create_future()

            async def tool_a(args):
                if not tool_a_started.done():
                    tool_a_started.set_result(args["value"])
                return await release_a

            async def tool_b(args):
                if not tool_b_started.done():
                    tool_b_started.set_result(args["value"])
                return await release_b

            suspended_client = CopilotClient(
                ExternalServerConfig(url=cli_url, tcp_connection_token="py-tcp-shared-test-token")
            )
            session1 = await suspended_client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                tools=[
                    _make_pending_tool("pending_lookup_a", tool_a),
                    _make_pending_tool("pending_lookup_b", tool_b),
                ],
            )
            session_id = session1.session_id

            try:
                tool_requests_task = asyncio.create_task(
                    _wait_for_external_tool_requests(
                        session1, ["pending_lookup_a", "pending_lookup_b"]
                    )
                )
                await session1.send(
                    "Call pending_lookup_a with value 'alpha' and "
                    "pending_lookup_b with value 'beta', then reply with both results."
                )
                tool_events = await tool_requests_task
                await asyncio.wait_for(
                    asyncio.gather(tool_a_started, tool_b_started), PENDING_WORK_TIMEOUT
                )
                assert tool_a_started.result() == "alpha"
                assert tool_b_started.result() == "beta"

                await suspended_client.force_stop()

                resumed_client = CopilotClient(
                    ExternalServerConfig(
                        url=cli_url, tcp_connection_token="py-tcp-shared-test-token"
                    )
                )
                try:
                    session2 = await resumed_client.resume_session(
                        session_id,
                        on_permission_request=PermissionHandler.approve_all,
                        continue_pending_work=True,
                    )

                    result_b = await session2.rpc.tools.handle_pending_tool_call(
                        HandlePendingToolCallRequest(
                            request_id=tool_events["pending_lookup_b"].data.request_id,
                            result="PARALLEL_B_BETA",
                        )
                    )
                    assert result_b.success
                    result_a = await session2.rpc.tools.handle_pending_tool_call(
                        HandlePendingToolCallRequest(
                            request_id=tool_events["pending_lookup_a"].data.request_id,
                            result="PARALLEL_A_ALPHA",
                        )
                    )
                    assert result_a.success

                    answer = await get_final_assistant_message(
                        session2, timeout=PENDING_WORK_TIMEOUT
                    )
                    content = answer.data.content or ""
                    assert "PARALLEL_A_ALPHA" in content
                    assert "PARALLEL_B_BETA" in content

                    await session2.disconnect()
                finally:
                    await _safe_force_stop(resumed_client)
            finally:
                if not release_a.done():
                    release_a.set_result("ORIGINAL_A_SHOULD_NOT_WIN")
                if not release_b.done():
                    release_b.set_result("ORIGINAL_B_SHOULD_NOT_WIN")
        finally:
            await _safe_force_stop(server)

    async def test_should_resume_successfully_when_no_pending_work_exists(
        self, ctx: E2ETestContext
    ):
        server = _make_subprocess_client(ctx, use_stdio=False)
        await server.start()
        try:
            cli_url = f"localhost:{server.actual_port}"

            first_client = CopilotClient(
                ExternalServerConfig(url=cli_url, tcp_connection_token="py-tcp-shared-test-token")
            )
            try:
                first_session = await first_client.create_session(
                    on_permission_request=PermissionHandler.approve_all,
                )
                session_id = first_session.session_id
                first_answer = await first_session.send_and_wait(
                    "Reply with exactly: NO_PENDING_TURN_ONE"
                )
                assert "NO_PENDING_TURN_ONE" in (first_answer.data.content or "")
                await first_session.disconnect()
            finally:
                await _safe_force_stop(first_client)

            resumed_client = CopilotClient(
                ExternalServerConfig(url=cli_url, tcp_connection_token="py-tcp-shared-test-token")
            )
            try:
                resumed_session = await resumed_client.resume_session(
                    session_id,
                    on_permission_request=PermissionHandler.approve_all,
                    continue_pending_work=True,
                )
                follow_up = await resumed_session.send_and_wait(
                    "Reply with exactly: NO_PENDING_TURN_TWO"
                )
                assert "NO_PENDING_TURN_TWO" in (follow_up.data.content or "")
                await resumed_session.disconnect()
            finally:
                await _safe_force_stop(resumed_client)
        finally:
            await _safe_force_stop(server)
