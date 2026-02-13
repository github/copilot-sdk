"""Unit tests for define_tool"""

import json

import pytest
from pydantic import BaseModel, Field

from copilot import ToolInvocation, define_tool
from copilot.tools import _normalize_result


class TestDefineTool:
    def test_creates_tool_with_correct_name_and_description(self):
        class Params(BaseModel):
            query: str

        @define_tool("search", description="Search for something")
        def search(params: Params, invocation: ToolInvocation) -> str:
            return "result"

        assert search.name == "search"
        assert search.description == "Search for something"
        assert search.handler is not None
        assert search.parameters is not None

    def test_infers_name_from_function(self):
        class Params(BaseModel):
            query: str

        @define_tool(description="Search for something")
        def my_search_tool(params: Params) -> str:
            return "result"

        assert my_search_tool.name == "my_search_tool"

    def test_generates_schema_from_pydantic_model(self):
        class Params(BaseModel):
            city: str = Field(description="City name")
            unit: str = Field(description="Temperature unit")

        @define_tool("get_weather", description="Get weather")
        def get_weather(params: Params, invocation: ToolInvocation) -> str:
            return "sunny"

        schema = get_weather.parameters
        assert schema is not None
        assert schema["type"] == "object"
        assert "city" in schema["properties"]
        assert "unit" in schema["properties"]
        assert schema["properties"]["city"]["description"] == "City name"

    async def test_handler_receives_typed_arguments(self):
        class Params(BaseModel):
            name: str
            count: int

        received_params = None

        @define_tool("test", description="Test tool")
        def test_tool(params: Params, invocation: ToolInvocation) -> str:
            nonlocal received_params
            received_params = params
            return "ok"

        invocation: ToolInvocation = {
            "session_id": "session-1",
            "tool_call_id": "call-1",
            "tool_name": "test",
            "arguments": {"name": "Alice", "count": 42},
        }

        await test_tool.handler(invocation)

        assert received_params is not None
        assert received_params.name == "Alice"
        assert received_params.count == 42

    async def test_handler_receives_invocation(self):
        class Params(BaseModel):
            pass

        received_inv = None

        @define_tool("test", description="Test tool")
        def test_tool(params: Params, invocation: ToolInvocation) -> str:
            nonlocal received_inv
            received_inv = invocation
            return "ok"

        invocation: ToolInvocation = {
            "session_id": "session-123",
            "tool_call_id": "call-456",
            "tool_name": "test",
            "arguments": {},
        }

        await test_tool.handler(invocation)

        assert received_inv["session_id"] == "session-123"
        assert received_inv["tool_call_id"] == "call-456"

    async def test_zero_param_handler(self):
        """Handler with no parameters: def handler() -> str"""
        called = False

        @define_tool("test", description="Test tool")
        def test_tool() -> str:
            nonlocal called
            called = True
            return "ok"

        invocation: ToolInvocation = {
            "session_id": "s1",
            "tool_call_id": "c1",
            "tool_name": "test",
            "arguments": {},
        }

        result = await test_tool.handler(invocation)

        assert called
        assert result["textResultForLlm"] == "ok"

    async def test_invocation_only_handler(self):
        """Handler with only invocation: def handler(invocation) -> str"""
        received_inv = None

        @define_tool("test", description="Test tool")
        def test_tool(invocation: ToolInvocation) -> str:
            nonlocal received_inv
            received_inv = invocation
            return "ok"

        invocation: ToolInvocation = {
            "session_id": "s1",
            "tool_call_id": "c1",
            "tool_name": "test",
            "arguments": {},
        }

        await test_tool.handler(invocation)

        assert received_inv is not None
        assert received_inv["session_id"] == "s1"

    async def test_params_only_handler(self):
        """Handler with only params: def handler(params) -> str"""

        class Params(BaseModel):
            value: str

        received_params = None

        @define_tool("test", description="Test tool")
        def test_tool(params: Params) -> str:
            nonlocal received_params
            received_params = params
            return "ok"

        invocation: ToolInvocation = {
            "session_id": "s1",
            "tool_call_id": "c1",
            "tool_name": "test",
            "arguments": {"value": "hello"},
        }

        await test_tool.handler(invocation)

        assert received_params is not None
        assert received_params.value == "hello"

    async def test_handler_error_is_hidden_from_llm(self):
        class Params(BaseModel):
            pass

        @define_tool("failing", description="A failing tool")
        def failing_tool(params: Params, invocation: ToolInvocation) -> str:
            raise ValueError("secret error message")

        invocation: ToolInvocation = {
            "session_id": "s1",
            "tool_call_id": "c1",
            "tool_name": "failing",
            "arguments": {},
        }

        result = await failing_tool.handler(invocation)

        assert result["resultType"] == "failure"
        assert "secret error message" not in result["textResultForLlm"]
        assert "error" in result["textResultForLlm"].lower()
        # But the actual error is stored internally
        assert result["error"] == "secret error message"

    async def test_function_style_api(self):
        class Params(BaseModel):
            value: str

        tool = define_tool(
            "my_tool",
            description="My tool",
            handler=lambda params, inv: params.value.upper(),
            params_type=Params,
        )

        assert tool.name == "my_tool"
        assert tool.description == "My tool"

        result = await tool.handler(
            {
                "session_id": "s",
                "tool_call_id": "c",
                "tool_name": "my_tool",
                "arguments": {"value": "hello"},
            }
        )
        assert result["textResultForLlm"] == "HELLO"

    def test_function_style_requires_name(self):
        class Params(BaseModel):
            value: str

        with pytest.raises(ValueError, match="name is required"):
            define_tool(
                description="My tool",
                handler=lambda params, inv: params.value.upper(),
                params_type=Params,
            )


class TestNormalizeResult:
    def test_none_returns_empty_success(self):
        result = _normalize_result(None)
        assert result["textResultForLlm"] == ""
        assert result["resultType"] == "success"

    def test_string_passes_through(self):
        result = _normalize_result("hello world")
        assert result["textResultForLlm"] == "hello world"
        assert result["resultType"] == "success"

    def test_dict_with_result_type_passes_through(self):
        input_result = {
            "textResultForLlm": "custom",
            "resultType": "failure",
            "error": "some error",
        }
        result = _normalize_result(input_result)
        assert result["textResultForLlm"] == "custom"
        assert result["resultType"] == "failure"

    def test_dict_is_json_serialized(self):
        result = _normalize_result({"key": "value", "num": 42})
        parsed = json.loads(result["textResultForLlm"])
        assert parsed == {"key": "value", "num": 42}
        assert result["resultType"] == "success"

    def test_list_is_json_serialized(self):
        result = _normalize_result(["a", "b", "c"])
        assert result["textResultForLlm"] == '["a", "b", "c"]'
        assert result["resultType"] == "success"

    def test_pydantic_model_is_serialized(self):
        class Response(BaseModel):
            status: str
            count: int

        result = _normalize_result(Response(status="ok", count=5))
        parsed = json.loads(result["textResultForLlm"])
        assert parsed == {"status": "ok", "count": 5}

    def test_list_of_pydantic_models_is_serialized(self):
        class Item(BaseModel):
            name: str
            value: int

        items = [Item(name="a", value=1), Item(name="b", value=2)]
        result = _normalize_result(items)
        parsed = json.loads(result["textResultForLlm"])
        assert parsed == [{"name": "a", "value": 1}, {"name": "b", "value": 2}]
        assert result["resultType"] == "success"

    def test_raises_for_unserializable_value(self):
        # Functions cannot be JSON serialized
        with pytest.raises(TypeError, match="Failed to serialize"):
            _normalize_result(lambda x: x)
