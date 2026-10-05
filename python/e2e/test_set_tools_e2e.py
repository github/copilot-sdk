"""Live tool replacement E2Es using shared set_tools provider captures."""

import json

import pytest
from pydantic import BaseModel, Field

from copilot import define_tool
from copilot.session import PermissionHandler
from copilot.tools import Tool, ToolInvocation, ToolResult

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

FRUIT_PROMPT = "Use lookup_fruit to find the fruit for code 42."
FRUIT_AND_VEGETABLE_PROMPT = (
    "Use lookup_fruit to find the fruit for code 42 again, and use lookup_vegetable to find the "
    "vegetable for code 7."
)
VEGETABLE_PROMPT = "Use lookup_vegetable to find the vegetable for code 7."


class FruitParams(BaseModel):
    code: int = Field(description="Fruit code")


class VegetableParams(BaseModel):
    code: int = Field(description="Vegetable code")


def lookup_fruit(fruit: str, calls: list[int] | None = None) -> Tool:
    @define_tool("lookup_fruit", description="Looks up the fruit for a numeric code")
    def tool(params: FruitParams) -> str:
        if calls is not None:
            calls.append(params.code)
        return fruit

    return tool


def lookup_vegetable(calls: list[int] | None = None) -> Tool:
    @define_tool("lookup_vegetable", description="Looks up the vegetable for a numeric code")
    def tool(params: VegetableParams) -> str:
        if calls is not None:
            calls.append(params.code)
        return "carrot"

    return tool


def retired_lookup() -> Tool:
    async def handler(_invocation: ToolInvocation) -> ToolResult:
        return ToolResult(text_result_for_llm="retired")

    return Tool("retired_lookup", "Looks up a retired value", handler=handler)


def invalid_tool(calls: list[int]) -> Tool:
    async def handler(_invocation: ToolInvocation) -> ToolResult:
        calls.append(1)
        return ToolResult(text_result_for_llm="never")

    return Tool("invalid.tool", "Has a name the runtime rejects", handler=handler)


def offered_tools(exchange: dict) -> set[str]:
    """Names of the tools a model request offered."""
    return {
        tool["function"]["name"]
        for tool in exchange["request"].get("tools") or []
        if "function" in tool
    }


def index_of_prompt(exchanges: list[dict], prompt: str) -> int:
    """Index of the first model request that carries ``prompt`` as a user message."""
    for index, exchange in enumerate(exchanges):
        for message in exchange["request"]["messages"]:
            content = message.get("content")
            text = content if isinstance(content, str) else json.dumps(content)
            if message.get("role") == "user" and prompt in text:
                return index
    return -1


class TestSetTools:
    async def test_replaces_tools_on_a_created_session(self, ctx: E2ETestContext):
        original_lookups: list[int] = []
        replacement_lookups: list[int] = []
        vegetable_lookups: list[int] = []
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            tools=[lookup_fruit("apple", original_lookups), retired_lookup()],
        )

        first = await session.send_and_wait(FRUIT_PROMPT, timeout=30)
        assert first is not None
        assert "apple" in (first.data.content or "")

        await session.set_tools(
            [
                lookup_fruit("dragonfruit", replacement_lookups),
                lookup_vegetable(vegetable_lookups),
            ]
        )

        second = await session.send_and_wait(FRUIT_AND_VEGETABLE_PROMPT, timeout=30)
        assert second is not None
        assert "dragonfruit" in (second.data.content or "")
        assert "carrot" in (second.data.content or "")
        assert original_lookups == [42]
        assert replacement_lookups == [42]
        assert vegetable_lookups == [7]

        # Model requests after the replacement offer exactly the new tool set.
        exchanges = await ctx.get_exchanges()
        replaced_from = index_of_prompt(exchanges, FRUIT_AND_VEGETABLE_PROMPT)
        assert replaced_from > 0
        for exchange in exchanges[:replaced_from]:
            tools = offered_tools(exchange)
            assert {"lookup_fruit", "retired_lookup"} <= tools
            assert "lookup_vegetable" not in tools
        for exchange in exchanges[replaced_from:]:
            tools = offered_tools(exchange)
            assert {"lookup_fruit", "lookup_vegetable"} <= tools
            assert "retired_lookup" not in tools

        await session.disconnect()

    async def test_replaces_tools_on_a_resumed_session(self, ctx: E2ETestContext):
        created_lookups: list[int] = []
        created = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            tools=[lookup_fruit("apple", created_lookups)],
        )
        session_id = created.session_id
        first = await created.send_and_wait(FRUIT_PROMPT, timeout=30)
        assert first is not None
        assert "apple" in (first.data.content or "")
        assert created_lookups == [42]
        await created.disconnect()

        fruit_lookups: list[int] = []
        vegetable_lookups: list[int] = []
        resumed = await ctx.client.resume_session(
            session_id,
            on_permission_request=PermissionHandler.approve_all,
            tools=[lookup_fruit("apple", fruit_lookups)],
        )
        await resumed.set_tools([lookup_vegetable(vegetable_lookups)])

        answer = await resumed.send_and_wait(VEGETABLE_PROMPT, timeout=30)
        assert answer is not None
        assert "carrot" in (answer.data.content or "")
        assert vegetable_lookups == [7]
        assert fruit_lookups == []

        exchanges = await ctx.get_exchanges()
        replaced_from = index_of_prompt(exchanges, VEGETABLE_PROMPT)
        assert replaced_from > 0
        for exchange in exchanges[replaced_from:]:
            tools = offered_tools(exchange)
            assert "lookup_vegetable" in tools
            assert "lookup_fruit" not in tools

        await resumed.disconnect()

    async def test_keeps_the_previous_tools_when_a_replacement_is_rejected(
        self, ctx: E2ETestContext
    ):
        original_lookups: list[int] = []
        replacement_lookups: list[int] = []
        invalid_calls: list[int] = []
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            tools=[lookup_fruit("apple", original_lookups)],
        )

        with pytest.raises(Exception):
            await session.set_tools(
                [
                    lookup_fruit("dragonfruit", replacement_lookups),
                    invalid_tool(invalid_calls),
                ]
            )

        answer = await session.send_and_wait(FRUIT_PROMPT, timeout=30)
        assert answer is not None
        assert "apple" in (answer.data.content or "")
        assert original_lookups == [42]
        assert replacement_lookups == []
        assert invalid_calls == []

        await session.disconnect()
