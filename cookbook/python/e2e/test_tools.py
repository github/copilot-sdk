"""E2E Tools Tests"""

import os

import pytest
from pydantic import BaseModel, Field

from copilot import ToolInvocation, define_tool

from .testharness import E2ETestContext, get_final_assistant_message

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestTools:
    async def test_invokes_built_in_tools(self, ctx: E2ETestContext):
        readme_path = os.path.join(ctx.work_dir, "README.md")
        with open(readme_path, "w") as f:
            f.write("# ELIZA, the only chatbot you'll ever need")

        session = await ctx.client.create_session()

        await session.send({"prompt": "What's the first line of README.md in this directory?"})
        assistant_message = await get_final_assistant_message(session)
        assert "ELIZA" in assistant_message.data.content

    async def test_invokes_custom_tool(self, ctx: E2ETestContext):
        class EncryptParams(BaseModel):
            input: str = Field(description="String to encrypt")

        @define_tool("encrypt_string", description="Encrypts a string")
        def encrypt_string(params: EncryptParams, invocation: ToolInvocation) -> str:
            return params.input.upper()

        session = await ctx.client.create_session({"tools": [encrypt_string]})

        await session.send({"prompt": "Use encrypt_string to encrypt this string: Hello"})
        assistant_message = await get_final_assistant_message(session)
        assert "HELLO" in assistant_message.data.content

    async def test_handles_tool_calling_errors(self, ctx: E2ETestContext):
        @define_tool("get_user_location", description="Gets the user's location")
        def get_user_location() -> str:
            raise Exception("Melbourne")

        session = await ctx.client.create_session({"tools": [get_user_location]})

        await session.send(
            {"prompt": "What is my location? If you can't find out, just say 'unknown'."}
        )
        answer = await get_final_assistant_message(session)

        # Check the underlying traffic
        traffic = await ctx.get_exchanges()
        last_conversation = traffic[-1]

        tool_calls = []
        for msg in last_conversation["request"]["messages"]:
            if msg.get("role") == "assistant" and "tool_calls" in msg:
                tool_calls.extend(msg["tool_calls"])

        assert len(tool_calls) == 1
        tool_call = tool_calls[0]
        assert tool_call["type"] == "function"
        assert tool_call["function"]["name"] == "get_user_location"

        tool_results = [
            msg for msg in last_conversation["request"]["messages"] if msg.get("role") == "tool"
        ]
        assert len(tool_results) == 1
        tool_result = tool_results[0]
        assert tool_result["tool_call_id"] == tool_call["id"]

        # The error message "Melbourne" should NOT be exposed to the LLM
        assert "Melbourne" not in tool_result["content"]

        # The assistant should not see the exception information
        assert "Melbourne" not in (answer.data.content or "")
        assert "unknown" in (answer.data.content or "").lower()

    async def test_can_receive_and_return_complex_types(self, ctx: E2ETestContext):
        class DbQuery(BaseModel):
            table: str
            ids: list[int]
            sortAscending: bool

        class DbQueryParams(BaseModel):
            query: DbQuery

        class City(BaseModel):
            countryId: int
            cityName: str
            population: int

        expected_session_id = None

        @define_tool("db_query", description="Performs a database query")
        def db_query(params: DbQueryParams, invocation: ToolInvocation) -> list[City]:
            assert params.query.table == "cities"
            assert params.query.ids == [12, 19]
            assert params.query.sortAscending is True
            assert invocation["session_id"] == expected_session_id

            return [
                City(countryId=19, cityName="Passos", population=135460),
                City(countryId=12, cityName="San Lorenzo", population=204356),
            ]

        session = await ctx.client.create_session({"tools": [db_query]})
        expected_session_id = session.session_id

        await session.send(
            {
                "prompt": "Perform a DB query for the 'cities' table using IDs 12 and 19, "
                "sorting ascending. Reply only with lines of the form: [cityname] [population]"
            }
        )

        assistant_message = await get_final_assistant_message(session)
        response_content = assistant_message.data.content or ""

        assert response_content != ""
        assert "Passos" in response_content
        assert "San Lorenzo" in response_content
        assert "135460" in response_content.replace(",", "")
        assert "204356" in response_content.replace(",", "")
