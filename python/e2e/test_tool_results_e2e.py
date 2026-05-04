"""E2E Tool Results Tests"""

import asyncio

import pytest
from pydantic import BaseModel, Field

from copilot import define_tool
from copilot.session import PermissionHandler
from copilot.tools import ToolInvocation, ToolResult

from .testharness import E2ETestContext, get_final_assistant_message

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestToolResults:
    async def test_should_handle_structured_toolresultobject_from_custom_tool(
        self, ctx: E2ETestContext
    ):
        class WeatherParams(BaseModel):
            city: str = Field(description="City name")

        @define_tool("get_weather", description="Gets weather for a city")
        def get_weather(params: WeatherParams, invocation: ToolInvocation) -> ToolResult:
            return ToolResult(
                text_result_for_llm=f"The weather in {params.city} is sunny and 72°F",
                result_type="success",
            )

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, tools=[get_weather]
        )

        try:
            await session.send("What's the weather in Paris?")
            assistant_message = await get_final_assistant_message(session)
            assert (
                "sunny" in assistant_message.data.content.lower()
                or "72" in assistant_message.data.content
            )
        finally:
            await session.disconnect()

    async def test_should_handle_tool_result_with_failure_resulttype(self, ctx: E2ETestContext):
        @define_tool("check_status", description="Checks the status of a service")
        def check_status(invocation: ToolInvocation) -> ToolResult:
            return ToolResult(
                text_result_for_llm="Service unavailable",
                result_type="failure",
                error="API timeout",
            )

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, tools=[check_status]
        )

        try:
            answer = await session.send_and_wait(
                "Check the status of the service using check_status."
                " If it fails, say 'service is down'."
            )
            assert answer is not None
            assert "service is down" in answer.data.content.lower()
        finally:
            await session.disconnect()

    async def test_should_preserve_tooltelemetry_and_not_stringify_structured_results_for_llm(
        self, ctx: E2ETestContext
    ):
        class AnalyzeParams(BaseModel):
            file: str = Field(description="File to analyze")

        @define_tool("analyze_code", description="Analyzes code for issues")
        def analyze_code(params: AnalyzeParams, invocation: ToolInvocation) -> ToolResult:
            return ToolResult(
                text_result_for_llm=f"Analysis of {params.file}: no issues found",
                result_type="success",
                tool_telemetry={
                    "metrics": {"analysisTimeMs": 150},
                    "properties": {"analyzer": "eslint"},
                },
            )

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, tools=[analyze_code]
        )

        try:
            await session.send("Analyze the file main.ts for issues.")
            assistant_message = await get_final_assistant_message(session)
            assert "no issues" in assistant_message.data.content.lower()

            # Verify the LLM received just textResultForLlm, not stringified JSON
            traffic = await ctx.get_exchanges()
            last_conversation = traffic[-1]
            tool_results = [
                m for m in last_conversation["request"]["messages"] if m["role"] == "tool"
            ]
            assert len(tool_results) == 1
            assert "toolTelemetry" not in tool_results[0]["content"]
            assert "resultType" not in tool_results[0]["content"]
        finally:
            await session.disconnect()

    async def test_should_handle_tool_result_with_rejected_resulttype(self, ctx: E2ETestContext):
        tool_handler_called = False
        tool_complete_future: asyncio.Future = asyncio.get_event_loop().create_future()

        @define_tool("deploy_service", description="Deploys a service")
        def deploy_service(invocation: ToolInvocation) -> ToolResult:
            nonlocal tool_handler_called
            tool_handler_called = True
            return ToolResult(
                text_result_for_llm=(
                    "Deployment rejected: policy violation"
                    " - production deployments require approval"
                ),
                result_type="rejected",
            )

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, tools=[deploy_service]
        )

        def on_event(event):
            if event.type.value == "tool.execution_complete" and not tool_complete_future.done():
                tool_complete_future.set_result(event)

        unsubscribe = session.on(on_event)
        try:
            asyncio.ensure_future(
                session.send(
                    "Deploy the service using deploy_service."
                    " If it's rejected, tell me it was 'rejected by policy'."
                )
            )
            tool_evt = await asyncio.wait_for(tool_complete_future, timeout=60.0)

            assert tool_handler_called, "Tool handler should have been called"
            assert not tool_evt.data.success
            error = tool_evt.data.error
            assert error is not None
            error_code = error if isinstance(error, str) else getattr(error, "code", None)
            assert error_code == "rejected"
            error_msg = error if isinstance(error, str) else getattr(error, "message", None)
            assert "Deployment rejected" in (error_msg or "")

            # Session should reach idle
            idle_future: asyncio.Future = asyncio.get_event_loop().create_future()
            session.on(
                lambda e: (
                    idle_future.set_result(e)
                    if e.type.value == "session.idle" and not idle_future.done()
                    else None
                )
            )
            await asyncio.wait_for(idle_future, timeout=30.0)
        finally:
            unsubscribe()
            await session.disconnect()

    async def test_should_handle_tool_result_with_denied_resulttype(self, ctx: E2ETestContext):
        tool_handler_called = False
        tool_complete_future: asyncio.Future = asyncio.get_event_loop().create_future()

        @define_tool("access_secret", description="Accesses a secret")
        def access_secret(invocation: ToolInvocation) -> ToolResult:
            nonlocal tool_handler_called
            tool_handler_called = True
            return ToolResult(
                text_result_for_llm="Access denied: insufficient permissions to read secrets",
                result_type="denied",
            )

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, tools=[access_secret]
        )

        def on_event(event):
            if event.type.value == "tool.execution_complete" and not tool_complete_future.done():
                tool_complete_future.set_result(event)

        unsubscribe = session.on(on_event)
        try:
            asyncio.ensure_future(
                session.send(
                    "Use access_secret to get the API key."
                    " If access is denied, tell me it was 'access denied'."
                )
            )
            tool_evt = await asyncio.wait_for(tool_complete_future, timeout=60.0)

            assert tool_handler_called, "Tool handler should have been called"
            assert not tool_evt.data.success
            error = tool_evt.data.error
            assert error is not None
            error_code = error if isinstance(error, str) else getattr(error, "code", None)
            assert error_code == "denied"
            error_msg = error if isinstance(error, str) else getattr(error, "message", None)
            assert "Access denied" in (error_msg or "")

            answer = await get_final_assistant_message(session, timeout=60.0)
            assert answer is not None
        finally:
            unsubscribe()
            await session.disconnect()
