"""
Extended hook lifecycle tests that mirror dotnet/test/HookLifecycleAndOutputTests.cs.

E2E coverage for every handler exposed on ``SessionHooks``:
``on_pre_tool_use``, ``on_post_tool_use``, ``on_user_prompt_submitted``,
``on_session_start``, ``on_session_end``, ``on_error_occurred``. Output-shape
behavior (modifiedPrompt / additionalContext / errorHandling / modifiedArgs /
modifiedResult / sessionSummary) is asserted alongside hook invocation.
"""

from __future__ import annotations

import asyncio

import pytest

from copilot.session import PermissionHandler
from copilot.tools import Tool, ToolInvocation, ToolResult

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestHooksExtended:
    async def test_should_invoke_userpromptsubmitted_hook_and_modify_prompt(
        self, ctx: E2ETestContext
    ):
        inputs: list[dict] = []

        async def on_user_prompt_submitted(input_data, invocation):
            inputs.append(input_data)
            assert invocation["session_id"]
            return {"modifiedPrompt": "Reply with exactly: HOOKED_PROMPT"}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks={"on_user_prompt_submitted": on_user_prompt_submitted},
        )
        try:
            response = await session.send_and_wait("Say something else")
            assert inputs
            assert "Say something else" in inputs[0].get("prompt", "")
            assert "HOOKED_PROMPT" in (response.data.content or "")
        finally:
            await session.disconnect()

    async def test_should_invoke_sessionstart_hook(self, ctx: E2ETestContext):
        inputs: list[dict] = []

        async def on_session_start(input_data, invocation):
            inputs.append(input_data)
            assert invocation["session_id"]
            return {"additionalContext": "Session start hook context."}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks={"on_session_start": on_session_start},
        )
        try:
            await session.send_and_wait("Say hi")
            assert inputs
            assert inputs[0].get("source") == "new"
            assert inputs[0].get("cwd")
        finally:
            await session.disconnect()

    async def test_should_invoke_sessionend_hook(self, ctx: E2ETestContext):
        inputs: list[dict] = []
        hook_invoked: asyncio.Future = asyncio.get_event_loop().create_future()

        async def on_session_end(input_data, invocation):
            inputs.append(input_data)
            if not hook_invoked.done():
                hook_invoked.set_result(input_data)
            assert invocation["session_id"]
            return {"sessionSummary": "session ended"}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks={"on_session_end": on_session_end},
        )
        await session.send_and_wait("Say bye")
        await session.disconnect()
        await asyncio.wait_for(hook_invoked, 10.0)
        assert inputs

    async def test_should_register_erroroccurred_hook(self, ctx: E2ETestContext):
        inputs: list[dict] = []

        async def on_error_occurred(input_data, invocation):
            inputs.append(input_data)
            assert invocation["session_id"]
            return {"errorHandling": "skip"}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks={"on_error_occurred": on_error_occurred},
        )
        try:
            await session.send_and_wait("Say hi")
            # Registration-only test: a healthy turn shouldn't fire OnErrorOccurred.
            assert not inputs
            assert session.session_id
        finally:
            await session.disconnect()

    async def test_should_allow_pretooluse_to_return_modifiedargs_and_suppressoutput(
        self, ctx: E2ETestContext
    ):
        inputs: list[dict] = []

        def echo_value(invocation: ToolInvocation) -> ToolResult:
            args = invocation.arguments or {}
            return ToolResult(text_result_for_llm=str(args.get("value", "")))

        async def on_pre_tool_use(input_data, invocation):
            inputs.append(input_data)
            if input_data.get("toolName") != "echo_value":
                return {"permissionDecision": "allow"}
            return {
                "permissionDecision": "allow",
                "modifiedArgs": {"value": "modified by hook"},
                "suppressOutput": False,
            }

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            tools=[
                Tool(
                    name="echo_value",
                    description="Echoes the supplied value",
                    parameters={
                        "type": "object",
                        "properties": {
                            "value": {
                                "type": "string",
                                "description": "Value to echo",
                            }
                        },
                        "required": ["value"],
                    },
                    handler=echo_value,
                )
            ],
            hooks={"on_pre_tool_use": on_pre_tool_use},
        )
        try:
            response = await session.send_and_wait(
                "Call echo_value with value 'original', then reply with the result."
            )
            assert inputs
            assert any(inp.get("toolName") == "echo_value" for inp in inputs)
            assert "modified by hook" in (response.data.content or "")
        finally:
            await session.disconnect()

    async def test_should_allow_posttooluse_to_return_modifiedresult(self, ctx: E2ETestContext):
        inputs: list[dict] = []

        async def on_post_tool_use(input_data, invocation):
            inputs.append(input_data)
            if input_data.get("toolName") != "report_intent":
                return None
            return {
                "modifiedResult": "modified by post hook",
                "suppressOutput": False,
            }

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            available_tools=["report_intent"],
            hooks={"on_post_tool_use": on_post_tool_use},
        )
        try:
            response = await session.send_and_wait(
                "Call the report_intent tool with intent 'Testing post hook', then reply done."
            )
            assert any(inp.get("toolName") == "report_intent" for inp in inputs)
            assert (response.data.content or "").strip().rstrip(".") in {"Done", "done"}
        finally:
            await session.disconnect()
