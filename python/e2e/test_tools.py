"""E2E Tools Tests"""

import os

import pytest
from pydantic import BaseModel, Field

from copilot import define_tool
from copilot.session import PermissionHandler, PermissionRequestResult
from copilot.tools import ToolInvocation

from .testharness import E2ETestContext, get_final_assistant_message

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestTools:
    async def test_invokes_built_in_tools(self, ctx: E2ETestContext):
        readme_path = os.path.join(ctx.work_dir, "README.md")
        with open(readme_path, "w") as f:
            f.write("# ELIZA, the only chatbot you'll ever need")

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )

        await session.send("What's the first line of README.md in this directory?")
        assistant_message = await get_final_assistant_message(session)
        assert "ELIZA" in assistant_message.data.content

    async def test_invokes_custom_tool(self, ctx: E2ETestContext):
        class EncryptParams(BaseModel):
            input: str = Field(description="String to encrypt")

        @define_tool("encrypt_string", description="Encrypts a string")
        def encrypt_string(params: EncryptParams, invocation: ToolInvocation) -> str:
            return params.input.upper()

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, tools=[encrypt_string]
        )

        await session.send("Use encrypt_string to encrypt this string: Hello")
        assistant_message = await get_final_assistant_message(session)
        assert "HELLO" in assistant_message.data.content

    async def test_handles_tool_calling_errors(self, ctx: E2ETestContext):
        @define_tool("get_user_location", description="Gets the user's location")
        def get_user_location() -> str:
            raise Exception("Melbourne")

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, tools=[get_user_location]
        )

        await session.send("What is my location? If you can't find out, just say 'unknown'.")
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
            assert invocation.session_id == expected_session_id

            return [
                City(countryId=19, cityName="Passos", population=135460),
                City(countryId=12, cityName="San Lorenzo", population=204356),
            ]

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, tools=[db_query]
        )
        expected_session_id = session.session_id

        await session.send(
            "Perform a DB query for the 'cities' table using IDs 12 and 19, "
            "sorting ascending. Reply only with lines of the form: [cityname] [population]"
        )

        assistant_message = await get_final_assistant_message(session)
        response_content = assistant_message.data.content or ""

        assert response_content != ""
        assert "Passos" in response_content
        assert "San Lorenzo" in response_content
        assert "135460" in response_content.replace(",", "")
        assert "204356" in response_content.replace(",", "")

    async def test_skippermission_sent_in_tool_definition(self, ctx: E2ETestContext):
        class LookupParams(BaseModel):
            id: str = Field(description="ID to look up")

        @define_tool(
            "safe_lookup",
            description="A safe lookup that skips permission",
            skip_permission=True,
        )
        def safe_lookup(params: LookupParams, invocation: ToolInvocation) -> str:
            return f"RESULT: {params.id}"

        did_run_permission_request = False

        def tracking_handler(request, invocation):
            nonlocal did_run_permission_request
            did_run_permission_request = True
            return PermissionRequestResult(kind="no-result")

        session = await ctx.client.create_session(
            on_permission_request=tracking_handler, tools=[safe_lookup]
        )

        await session.send("Use safe_lookup to look up 'test123'")
        assistant_message = await get_final_assistant_message(session)
        assert "RESULT: test123" in assistant_message.data.content
        assert not did_run_permission_request

    async def test_overrides_built_in_tool_with_custom_tool(self, ctx: E2ETestContext):
        class GrepParams(BaseModel):
            query: str = Field(description="Search query")

        @define_tool(
            "grep",
            description="A custom grep implementation that overrides the built-in",
            overrides_built_in_tool=True,
        )
        def custom_grep(params: GrepParams, invocation: ToolInvocation) -> str:
            return f"CUSTOM_GREP_RESULT: {params.query}"

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all, tools=[custom_grep]
        )

        await session.send("Use grep to search for the word 'hello'")
        assistant_message = await get_final_assistant_message(session)
        assert "CUSTOM_GREP_RESULT" in assistant_message.data.content

    async def test_invokes_custom_tool_with_permission_handler(self, ctx: E2ETestContext):
        class EncryptParams(BaseModel):
            input: str = Field(description="String to encrypt")

        @define_tool("encrypt_string", description="Encrypts a string")
        def encrypt_string(params: EncryptParams, invocation: ToolInvocation) -> str:
            return params.input.upper()

        permission_requests = []

        def on_permission_request(request, invocation):
            permission_requests.append(request)
            return PermissionRequestResult(kind="approved")

        session = await ctx.client.create_session(
            on_permission_request=on_permission_request, tools=[encrypt_string]
        )

        await session.send("Use encrypt_string to encrypt this string: Hello")
        assistant_message = await get_final_assistant_message(session)
        assert "HELLO" in assistant_message.data.content

        # Should have received a custom-tool permission request
        custom_tool_requests = [r for r in permission_requests if r.kind.value == "custom-tool"]
        assert len(custom_tool_requests) > 0
        assert custom_tool_requests[0].tool_name == "encrypt_string"

    async def test_denies_custom_tool_when_permission_denied(self, ctx: E2ETestContext):
        tool_handler_called = False

        class EncryptParams(BaseModel):
            input: str = Field(description="String to encrypt")

        @define_tool("encrypt_string", description="Encrypts a string")
        def encrypt_string(params: EncryptParams, invocation: ToolInvocation) -> str:
            nonlocal tool_handler_called
            tool_handler_called = True
            return params.input.upper()

        def on_permission_request(request, invocation):
            return PermissionRequestResult(kind="denied-interactively-by-user")

        session = await ctx.client.create_session(
            on_permission_request=on_permission_request, tools=[encrypt_string]
        )

        await session.send("Use encrypt_string to encrypt this string: Hello")
        await get_final_assistant_message(session)

        # The tool handler should NOT have been called since permission was denied
        assert not tool_handler_called
