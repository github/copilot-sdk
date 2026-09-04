"""
E2E tests for external tool cancellation.
"""

from __future__ import annotations

import asyncio

import pytest

from copilot.session import PermissionHandler
from copilot.tools import Tool, ToolInvocation, ToolResult

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestExternalToolCancellation:
    async def test_should_cancel_tool_handler_when_session_disconnects(
        self, ctx: E2ETestContext
    ):
        tool_started = asyncio.Event()
        tool_cancelled = asyncio.Event()
        release_tool: asyncio.Future = asyncio.get_event_loop().create_future()

        async def slow_tool_handler(invocation: ToolInvocation) -> ToolResult:
            _ = (invocation.arguments or {}).get("value", "")
            tool_started.set()
            try:
                result = await asyncio.wait_for(release_tool, timeout=120.0)
                return ToolResult(text_result_for_llm=str(result))
            except asyncio.CancelledError:
                tool_cancelled.set()
                raise

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
            asyncio.ensure_future(
                session.send("Use slow_analysis with value 'test_abort'. Wait for the result.")
            )
            await asyncio.wait_for(tool_started.wait(), timeout=60.0)
            await session.disconnect()
            await asyncio.wait_for(tool_cancelled.wait(), timeout=60.0)
        finally:
            if not release_tool.done():
                release_tool.set_result("RELEASED")
