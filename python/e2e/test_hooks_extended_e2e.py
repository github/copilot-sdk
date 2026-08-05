"""
Extended hook lifecycle tests that mirror dotnet/test/HookLifecycleAndOutputTests.cs.

E2E coverage for every handler exposed on ``SessionHooks``:
``on_pre_tool_use``, ``on_post_tool_use``, ``on_post_tool_use_failure``,
``on_user_prompt_submitted``, ``on_user_prompt_transformed``, ``on_session_start``,
``on_session_end``,
``on_error_occurred``, ``on_agent_stop``. Output-shape behavior (modifiedPrompt /
additionalContext / errorHandling / modifiedArgs / modifiedResult /
sessionSummary) is asserted alongside hook invocation.
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
        invocation_session_ids: list[str] = []

        async def on_user_prompt_submitted(input_data, invocation):
            inputs.append(input_data)
            invocation_session_ids.append(invocation["session_id"])
            return {"modifiedPrompt": "Reply with exactly: HOOKED_PROMPT"}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks={"on_user_prompt_submitted": on_user_prompt_submitted},
        )
        try:
            response = await session.send_and_wait("Say something else")
            assert inputs
            assert all(session_id == session.session_id for session_id in invocation_session_ids)
            assert "Say something else" in inputs[0].get("prompt", "")
            assert "HOOKED_PROMPT" in (response.data.content or "")
        finally:
            await session.disconnect()

    async def test_should_invoke_userprompttransformed_hook_and_modify_transformed_prompt(
        self, ctx: E2ETestContext
    ):
        inputs: list[dict] = []

        async def on_user_prompt_transformed(input_data, invocation):
            assert invocation["session_id"]
            inputs.append(input_data)
            return {"modifiedTransformedPrompt": "Reply with exactly: HOOKED_TRANSFORMED_PROMPT"}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks={"on_user_prompt_transformed": on_user_prompt_transformed},
        )
        try:
            response = await session.send_and_wait("Answer the request above.")
            assert inputs
            assert "Answer the request above." in inputs[0]["prompt"]
            assert "Answer the request above." in inputs[0]["transformedPrompt"]
            assert "<current_datetime>" in inputs[0]["transformedPrompt"]
            assert inputs[0]["timestamp"].timestamp() > 0
            assert inputs[0]["workingDirectory"]
            assert "HOOKED_TRANSFORMED_PROMPT" in (response.data.content or "")
        finally:
            await session.disconnect()

    async def test_should_invoke_sessionstart_hook(self, ctx: E2ETestContext):
        inputs: list[dict] = []
        invocation_session_ids: list[str] = []

        async def on_session_start(input_data, invocation):
            inputs.append(input_data)
            invocation_session_ids.append(invocation["session_id"])
            return {"additionalContext": "Session start hook context."}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks={"on_session_start": on_session_start},
        )
        try:
            await session.send_and_wait("Say hi")
            assert inputs
            assert all(session_id == session.session_id for session_id in invocation_session_ids)
            assert inputs[0].get("source") == "new"
            assert inputs[0].get("workingDirectory")
        finally:
            await session.disconnect()

    async def test_should_invoke_sessionend_hook(self, ctx: E2ETestContext):
        inputs: list[dict] = []
        invocation_session_ids: list[str] = []
        hook_invoked: asyncio.Future = asyncio.get_event_loop().create_future()

        async def on_session_end(input_data, invocation):
            inputs.append(input_data)
            invocation_session_ids.append(invocation["session_id"])
            if not hook_invoked.done():
                hook_invoked.set_result(input_data)
            return {"sessionSummary": "session ended"}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks={"on_session_end": on_session_end},
        )
        await session.send_and_wait("Say bye")
        await session.disconnect()
        await asyncio.wait_for(hook_invoked, 10.0)
        assert inputs
        assert all(session_id == session.session_id for session_id in invocation_session_ids)

    async def test_should_register_erroroccurred_hook(self, ctx: E2ETestContext):
        inputs: list[dict] = []
        invocation_session_ids: list[str] = []

        async def on_error_occurred(input_data, invocation):
            inputs.append(input_data)
            invocation_session_ids.append(invocation["session_id"])
            return {"errorHandling": "skip"}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks={"on_error_occurred": on_error_occurred},
        )
        try:
            await session.send_and_wait("Say hi")
            # Registration-only test: a healthy turn shouldn't fire OnErrorOccurred.
            assert not inputs
            assert not invocation_session_ids
            assert session.session_id
        finally:
            await session.disconnect()

    async def test_should_invoke_agentstop_hook_and_apply_block_response(self, ctx: E2ETestContext):
        inputs: list[dict] = []

        async def on_agent_stop(input_data, invocation):
            assert invocation["session_id"] == session.session_id
            inputs.append(input_data)
            if len(inputs) == 1:
                return {
                    "decision": "block",
                    "reason": "Reply with exactly: AGENT_STOP_CONTINUED",
                }
            return None

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks={"on_agent_stop": on_agent_stop},
        )
        try:
            response = await session.send_and_wait("Reply with exactly: AGENT_STOP_INITIAL")
            assert len(inputs) == 2
            assert inputs[0].get("stopHookActive") is not True
            assert inputs[1].get("stopHookActive") is True
            assert inputs[0].get("stopReason") == "end_turn"
            assert inputs[0].get("transcriptPath")
            assert "AGENT_STOP_CONTINUED" in (response.data.content or "")
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
            if input_data.get("toolName") != "view":
                return None
            return {
                "modifiedResult": {
                    "textResultForLlm": "modified by post hook",
                    "resultType": "success",
                    "toolTelemetry": {},
                },
                "suppressOutput": False,
            }

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            hooks={"on_post_tool_use": on_post_tool_use},
        )
        try:
            response = await session.send_and_wait(
                "Call the view tool to read the current directory, then reply done."
            )
            assert any(inp.get("toolName") == "view" for inp in inputs)
            assert "done" in (response.data.content or "").lower()
        finally:
            await session.disconnect()

    @pytest.mark.skip(
        reason="Fails with 1.0.64-0 runtime: built-in tools are not available when hooks "
        "restrict availableTools, so the failure path cannot be exercised. "
        "Follow up with runtime team."
    )
    async def test_should_invoke_posttoolusefailure_hook_for_failed_tool_result(
        self, ctx: E2ETestContext
    ):
        failure_inputs: list[dict] = []
        post_tool_use_inputs: list[dict] = []
        invocation_session_ids: list[str] = []

        async def on_post_tool_use(input_data, invocation):
            post_tool_use_inputs.append(input_data)
            return None

        async def on_post_tool_use_failure(input_data, invocation):
            failure_inputs.append(input_data)
            invocation_session_ids.append(invocation["session_id"])
            return {"additionalContext": "HOOK_FAILURE_GUIDANCE_APPLIED"}

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            available_tools=["report_intent"],
            hooks={
                "on_post_tool_use": on_post_tool_use,
                "on_post_tool_use_failure": on_post_tool_use_failure,
            },
        )
        try:
            response = await session.send_and_wait(
                "Call the view tool with path 'missing.txt'. "
                "If it fails, use the hook guidance to answer."
            )
            assert not post_tool_use_inputs
            assert len(failure_inputs) == 1
            assert all(session_id == session.session_id for session_id in invocation_session_ids)
            failure_input = failure_inputs[0]
            assert failure_input["toolName"] == "view"
            assert "does not exist" in failure_input["error"]
            assert "missing.txt" in failure_input["toolArgs"]["path"]
            assert failure_input["timestamp"].timestamp() > 0
            assert failure_input["workingDirectory"]
            assert "HOOK_FAILURE_GUIDANCE_APPLIED" in (response.data.content or "")
        finally:
            await session.disconnect()
