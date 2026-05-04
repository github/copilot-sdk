"""
E2E tests for session abort functionality.

Verifies that session.abort() cleanly interrupts an active turn — both during
streaming and during tool execution — without leaving dangling state or causing
exceptions in the event delivery pipeline.

Mirrors dotnet/test/E2E/AbortE2ETests.cs (snapshot category ``abort``).
"""

from __future__ import annotations

import asyncio

import pytest

from copilot.session import PermissionHandler
from copilot.tools import Tool, ToolInvocation, ToolResult

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestAbort:
    async def test_should_abort_during_active_streaming(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            streaming=True,
        )

        events = []
        first_delta: asyncio.Future = asyncio.get_event_loop().create_future()

        def on_event(event):
            events.append(event)
            if event.type.value == "assistant.message_delta" and not first_delta.done():
                first_delta.set_result(event)

        unsubscribe = session.on(on_event)
        try:
            # Fire-and-forget — we'll abort before it finishes
            asyncio.ensure_future(
                session.send(
                    "Write a very long essay about the history of computing,"
                    " covering every decade from the 1940s to the 2020s in great detail."
                )
            )

            # Wait for at least one delta to arrive (proves streaming started)
            delta = await asyncio.wait_for(first_delta, timeout=60.0)
            assert delta.data.delta_content

            # Abort mid-stream
            await session.abort()

            types = [e.type.value for e in events]
            assert "assistant.message_delta" in types

            # Session should be in a usable state after abort
            follow_up = await session.send_and_wait("Say 'abort_recovery_ok'.", timeout=60.0)
            assert follow_up is not None
            assert "abort_recovery_ok" in (follow_up.data.content or "").lower()
        finally:
            unsubscribe()
            await session.disconnect()

    async def test_should_abort_during_active_tool_execution(self, ctx: E2ETestContext):
        tool_started: asyncio.Future = asyncio.get_event_loop().create_future()
        release_tool: asyncio.Future = asyncio.get_event_loop().create_future()

        async def slow_tool_handler(invocation: ToolInvocation) -> ToolResult:
            value = (invocation.arguments or {}).get("value", "")
            if not tool_started.done():
                tool_started.set_result(value)
            result = await asyncio.wait_for(release_tool, timeout=60.0)
            return ToolResult(text_result_for_llm=str(result))

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            tools=[
                Tool(
                    name="slow_analysis",
                    description="A slow analysis tool that blocks until released",
                    parameters={
                        "type": "object",
                        "properties": {
                            "value": {"type": "string", "description": "Value to analyze"}
                        },
                        "required": ["value"],
                    },
                    handler=slow_tool_handler,
                )
            ],
        )

        try:
            # Fire-and-forget
            asyncio.ensure_future(
                session.send("Use slow_analysis with value 'test_abort'. Wait for the result.")
            )

            # Wait for the tool to start executing
            tool_value = await asyncio.wait_for(tool_started, timeout=60.0)
            assert tool_value == "test_abort"

            # Abort while the tool is running
            await session.abort()

            # Release the tool so its task doesn't leak
            if not release_tool.done():
                release_tool.set_result("RELEASED_AFTER_ABORT")

            # Session should be usable after abort
            recovery_received: asyncio.Future = asyncio.get_event_loop().create_future()

            def check_recovery(event):
                if (
                    event.type.value == "assistant.message"
                    and "tool_abort_recovery_ok" in (event.data.content or "").lower()
                    and not recovery_received.done()
                ):
                    recovery_received.set_result(event)

            unsubscribe = session.on(check_recovery)
            try:
                await session.send("Say 'tool_abort_recovery_ok'.")
                recovery_message = await asyncio.wait_for(recovery_received, timeout=60.0)
                assert "tool_abort_recovery_ok" in (recovery_message.data.content or "").lower()
            finally:
                unsubscribe()
        finally:
            if not release_tool.done():
                release_tool.set_result("CLEANUP")
            await session.disconnect()
