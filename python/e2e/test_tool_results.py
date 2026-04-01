"""E2E Tool Results Tests"""

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

        await session.send("What's the weather in Paris?")
        assistant_message = await get_final_assistant_message(session)
        assert "sunny" in assistant_message.data.content.lower() or "72" in assistant_message.data.content

    async def test_should_handle_tool_result_with_failure_resulttype(
        self, ctx: E2ETestContext
    ):
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

        answer = await session.send_and_wait(
            "Check the status of the service using check_status. If it fails, say 'service is down'."
        )
        assert answer is not None
        assert "service is down" in answer.data.content.lower()

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
