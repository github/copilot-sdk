"""Structured-output tests against the released runtime and shared provider captures."""

import asyncio
import json

import pytest
from pydantic import BaseModel, ConfigDict

from copilot import define_tool
from copilot.rpc import (
    JSONSchemaResponseFormat,
    ResponseFormat,
    ResponseFormatType,
    SendMessageItem,
    SendMessagesRequest,
)
from copilot.session import PermissionHandler
from copilot.session_events import AssistantMessageData, SessionErrorData, SessionIdleData

pytestmark = pytest.mark.asyncio(loop_scope="module")


class Inventory(BaseModel):
    model_config = ConfigDict(extra="forbid")
    count: int
    color: str


class Answer(BaseModel):
    model_config = ConfigDict(extra="forbid")
    answer: int


class First(BaseModel):
    model_config = ConfigDict(extra="forbid")
    first: int


class Second(BaseModel):
    model_config = ConfigDict(extra="forbid")
    second: int


def config(ctx):
    return {
        "model": "gpt-4.1",
        "available_tools": [],
        "on_permission_request": PermissionHandler.approve_all,
        "provider": {
            "type": "openai",
            "wire_api": "completions",
            "base_url": ctx.proxy_url,
            "model_id": "gpt-4.1",
            "wire_model": "gpt-4.1",
            "api_key": "fake-token-for-e2e-tests",
            "headers": {
                "Copilot-Integration-Id": "copilot-developer-cli",
                "Copilot-Harness-Id": "copilot-sdk",
                "X-GitHub-Api-Version": "2026-08-01",
            },
        },
    }


async def test_infers_typed_result_after_custom_tool(ctx):
    calls = 0

    @define_tool("get_inventory", description="Get the current widget inventory.")
    def get_inventory() -> str:
        nonlocal calls
        calls += 1
        return "The inventory contains 42 red widgets."

    async with await ctx.client.create_session(**config(ctx), tools=[get_inventory]) as session:
        result = await session.send_and_wait_typed(
            "Call get_inventory, then report the widget count and color.", Inventory, timeout=30
        )
        assert result == Inventory(count=42, color="red")
        assert calls > 0
        ordinary = await session.send_and_wait(
            "Now reply with exactly the plain text HELLO, not JSON.", timeout=30
        )
        assert ordinary.data.content.strip() == "HELLO"


async def test_sends_explicit_schema_for_message_and_batch(ctx):
    async with await ctx.client.create_session(**config(ctx)) as session:
        idle = asyncio.get_running_loop().create_future()
        replies = []

        def observe(event):
            if event.agent_id:
                return
            if isinstance(event.data, AssistantMessageData):
                replies.append(event.data)
            elif isinstance(event.data, SessionIdleData) and not idle.done():
                idle.set_result(None)
            elif isinstance(event.data, SessionErrorData) and not idle.done():
                idle.set_exception(RuntimeError(event.data.message))

        unsubscribe = session.on(observe)
        schema = Inventory.model_json_schema()
        try:
            accepted = await session.rpc.send_messages(
                SendMessagesRequest(
                    messages=[
                        SendMessageItem(prompt="There are 42 red widgets in stock."),
                        SendMessageItem(prompt="Report the widget count and color."),
                    ],
                    response_format=ResponseFormat(
                        type=ResponseFormatType.JSON_SCHEMA,
                        json_schema=JSONSchemaResponseFormat(
                            name="inventory", schema=schema, strict=True
                        ),
                    ),
                )
            )
            await asyncio.wait_for(idle, 30)
            final = next(
                item
                for item in reversed(replies)
                if item.originating_message_id == accepted.message_ids[-1]
            )
            assert Inventory.model_validate_json(final.content) == Inventory(count=42, color="red")
        finally:
            unsubscribe()
        updated = await session.send_and_wait(
            "The inventory now has 21 blue widgets. Report the new count and color.",
            response_schema=schema,
            timeout=30,
        )
        assert Inventory.model_validate_json(updated.data.content) == Inventory(
            count=21, color="blue"
        )


async def test_typed_wait_returns_stop_hook_correction_after_terminal_tool(ctx):
    calls = 0
    stops = 0

    @define_tool(
        "lookup_number",
        description="Return the number needed for the calculation.",
        is_terminal=True,
        skip_permission=True,
    )
    def lookup_number() -> int:
        nonlocal calls
        calls += 1
        return 58

    def stop_hook(_input, _invocation):
        nonlocal stops
        stops += 1
        if stops == 1:
            return {
                "decision": "block",
                "reason": "Correct the answer to 99, not 63. Do not use tools.",
            }
        return None

    async with await ctx.client.create_session(
        **config(ctx), tools=[lookup_number], hooks={"on_agent_stop": stop_hook}
    ) as session:
        replies = []
        unsubscribe = session.on(
            lambda event: (
                replies.append(event.data)
                if not event.agent_id
                and isinstance(event.data, AssistantMessageData)
                and not event.data.tool_requests
                else None
            )
        )
        try:
            result = await session.send_and_wait_typed(
                "Call lookup_number exactly once, then add 5 to the returned number. "
                "Do not guess its result.",
                Answer,
                timeout=30,
            )
            assert result.answer == 99
            assert calls == 1
            assert stops == 2
            assert [json.loads(reply.content)["answer"] for reply in replies] == [63, 99]
            assert replies[0].originating_message_id == replies[1].originating_message_id
        finally:
            unsubscribe()


async def test_typed_wait_returns_late_steering_response(ctx):
    stops = 0
    steering_id = None

    async def stop_hook(_input, _invocation):
        nonlocal stops, steering_id
        stops += 1
        if stops == 1:
            steering_id = await session.send(
                "Change the answer to 99. Do not use tools.", mode="immediate"
            )
        return None

    async with await ctx.client.create_session(
        **config(ctx), hooks={"on_agent_stop": stop_hook}
    ) as session:
        result = await session.send_and_wait_typed(
            "What is 19 + 23? Do not use tools.", Answer, timeout=30
        )
        assert result.answer == 99
        assert stops == 2
        assert steering_id


async def test_concurrent_typed_sends_return_their_own_results(ctx):
    entered = asyncio.Event()
    release = asyncio.Event()

    @define_tool("first_number", description="Get the number for the first question.")
    async def first_number() -> int:
        entered.set()
        await release.wait()
        return 42

    async with await ctx.client.create_session(**config(ctx), tools=[first_number]) as session:
        first = asyncio.create_task(
            session.send_and_wait_typed(
                "Call first_number exactly once and report its returned number.", First, timeout=30
            )
        )
        try:
            await asyncio.wait_for(entered.wait(), 30)
            second = asyncio.create_task(
                session.send_and_wait_typed("What is 30 + 7? Do not use tools.", Second, timeout=30)
            )
            async with asyncio.timeout(30):
                while not (await session.rpc.queue.pending_items()).items:
                    await asyncio.sleep(0.01)
            release.set()
            assert (await first).first == 42
            assert (await second).second == 37
        finally:
            release.set()
            if not first.done():
                first.cancel()
                await asyncio.gather(first, return_exceptions=True)
