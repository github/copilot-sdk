"""Unit tests for define_tool"""

import json

import pytest
from pydantic import BaseModel, ConfigDict, Field, field_validator

from copilot import define_tool
from copilot.generated.rpc import ExternalToolTextResultForLlm
from copilot.tools import (
    ToolInvocation,
    ToolResult,
    _normalize_result,
    convert_mcp_call_tool_result,
)


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

        invocation = ToolInvocation(
            session_id="session-1",
            tool_call_id="call-1",
            tool_name="test",
            arguments={"name": "Alice", "count": 42},
        )

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

        invocation = ToolInvocation(
            session_id="session-123",
            tool_call_id="call-456",
            tool_name="test",
            arguments={},
        )

        await test_tool.handler(invocation)

        assert received_inv.session_id == "session-123"
        assert received_inv.tool_call_id == "call-456"

    async def test_zero_param_handler(self):
        """Handler with no parameters: def handler() -> str"""
        called = False

        @define_tool("test", description="Test tool")
        def test_tool() -> str:
            nonlocal called
            called = True
            return "ok"

        invocation = ToolInvocation(
            session_id="s1",
            tool_call_id="c1",
            tool_name="test",
            arguments={},
        )

        result = await test_tool.handler(invocation)

        assert called
        assert result.text_result_for_llm == "ok"

    async def test_invocation_only_handler(self):
        """Handler with only invocation: def handler(invocation) -> str"""
        received_inv = None

        @define_tool("test", description="Test tool")
        def test_tool(invocation: ToolInvocation) -> str:
            nonlocal received_inv
            received_inv = invocation
            return "ok"

        invocation = ToolInvocation(
            session_id="s1",
            tool_call_id="c1",
            tool_name="test",
            arguments={},
        )

        await test_tool.handler(invocation)

        assert received_inv is not None
        assert received_inv.session_id == "s1"

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

        invocation = ToolInvocation(
            session_id="s1",
            tool_call_id="c1",
            tool_name="test",
            arguments={"value": "hello"},
        )

        await test_tool.handler(invocation)

        assert received_params is not None
        assert received_params.value == "hello"

    async def test_handler_error_is_hidden_from_llm(self):
        class Params(BaseModel):
            pass

        @define_tool("failing", description="A failing tool")
        def failing_tool(params: Params, invocation: ToolInvocation) -> str:
            raise ValueError("secret error message")

        invocation = ToolInvocation(
            session_id="s1",
            tool_call_id="c1",
            tool_name="failing",
            arguments={},
        )

        result = await failing_tool.handler(invocation)

        assert result.result_type == "failure"
        assert "secret error message" not in result.text_result_for_llm
        assert "error" in result.text_result_for_llm.lower()
        # But the actual error is stored internally
        assert result.error == "secret error message"

    async def test_validation_error_is_surfaced_to_llm(self):
        class Params(BaseModel):
            username: str

            @field_validator("username")
            @classmethod
            def check_username(cls, v: str) -> str:
                if v == "admin":
                    raise ValueError("username 'admin' is reserved")
                return v

        @define_tool("validate", description="A validating tool")
        def validating_tool(params: Params) -> str:
            return "ok"

        invocation = ToolInvocation(
            session_id="s1",
            tool_call_id="c1",
            tool_name="validate",
            arguments={"username": "admin"},
        )

        result = await validating_tool.handler(invocation)

        assert result.result_type == "failure"
        assert result.text_result_for_llm.startswith("Invalid tool arguments:")
        assert "username 'admin' is reserved" in result.text_result_for_llm
        # Full detail is retained in the debug field.
        assert result.error is not None

    async def test_validation_error_extra_forbid_includes_field_name(self):
        class Params(BaseModel):
            model_config = ConfigDict(extra="forbid")

            request: str

        @define_tool("strict", description="A strict tool")
        def strict_tool(params: Params) -> str:
            return "ok"

        invocation = ToolInvocation(
            session_id="s1",
            tool_call_id="c1",
            tool_name="strict",
            arguments={"request": "ok", "extra_field": "unexpected"},
        )

        result = await strict_tool.handler(invocation)

        assert result.result_type == "failure"
        assert result.text_result_for_llm.startswith("Invalid tool arguments:")
        # The offending key name is carried in `loc` even though the generic
        # message is "Extra inputs are not permitted".
        assert "extra_field" in result.text_result_for_llm
        assert result.error is not None

    async def test_validation_error_from_handler_body_is_redacted(self):
        class Params(BaseModel):
            pass

        class Internal(BaseModel):
            count: int

        @define_tool("body", description="A tool that validates internally")
        def body_tool(params: Params) -> str:
            Internal.model_validate({"count": "secret-not-an-int"})
            return "ok"

        invocation = ToolInvocation(
            session_id="s1",
            tool_call_id="c1",
            tool_name="body",
            arguments={},
        )

        result = await body_tool.handler(invocation)

        assert result.result_type == "failure"
        # A ValidationError from the handler body must not be surfaced as an
        # argument-validation error; it stays redacted like any other exception.
        assert not result.text_result_for_llm.startswith("Invalid tool arguments:")
        assert "secret-not-an-int" not in result.text_result_for_llm
        assert "error" in result.text_result_for_llm.lower()
        assert result.error is not None

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
            ToolInvocation(
                session_id="s",
                tool_call_id="c",
                tool_name="my_tool",
                arguments={"value": "hello"},
            )
        )
        assert result.text_result_for_llm == "HELLO"

    def test_function_style_can_create_declaration_only_tool(self):
        class Params(BaseModel):
            value: str = Field(description="Value to look up")

        tool = define_tool(
            "my_tool",
            description="My tool",
            params_type=Params,
        )

        assert tool.name == "my_tool"
        assert tool.description == "My tool"
        assert tool.handler is None
        assert tool.parameters is not None
        assert tool.parameters["properties"]["value"]["description"] == "Value to look up"

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
        assert result.text_result_for_llm == ""
        assert result.result_type == "success"

    def test_string_passes_through(self):
        result = _normalize_result("hello world")
        assert result.text_result_for_llm == "hello world"
        assert result.result_type == "success"

    def test_tool_result_passes_through(self):
        input_result = ToolResult(
            text_result_for_llm="custom",
            result_type="failure",
            error="some error",
        )
        result = _normalize_result(input_result)
        assert result.text_result_for_llm == "custom"
        assert result.result_type == "failure"

    def test_dict_is_json_serialized(self):
        result = _normalize_result({"key": "value", "num": 42})
        parsed = json.loads(result.text_result_for_llm)
        assert parsed == {"key": "value", "num": 42}
        assert result.result_type == "success"

    def test_list_is_json_serialized(self):
        result = _normalize_result(["a", "b", "c"])
        assert result.text_result_for_llm == '["a", "b", "c"]'
        assert result.result_type == "success"

    def test_pydantic_model_is_serialized(self):
        class Response(BaseModel):
            status: str
            count: int

        result = _normalize_result(Response(status="ok", count=5))
        parsed = json.loads(result.text_result_for_llm)
        assert parsed == {"status": "ok", "count": 5}

    def test_list_of_pydantic_models_is_serialized(self):
        class Item(BaseModel):
            name: str
            value: int

        items = [Item(name="a", value=1), Item(name="b", value=2)]
        result = _normalize_result(items)
        parsed = json.loads(result.text_result_for_llm)
        assert parsed == [{"name": "a", "value": 1}, {"name": "b", "value": 2}]
        assert result.result_type == "success"

    def test_raises_for_unserializable_value(self):
        # Functions cannot be JSON serialized
        with pytest.raises(TypeError, match="Failed to serialize"):
            _normalize_result(lambda x: x)


class TestConvertMcpCallToolResult:
    def test_text_only_call_tool_result(self):
        result = convert_mcp_call_tool_result(
            {
                "content": [{"type": "text", "text": "hello"}],
            }
        )
        assert result.text_result_for_llm == "hello"
        assert result.result_type == "success"

    def test_multiple_text_blocks(self):
        result = convert_mcp_call_tool_result(
            {
                "content": [
                    {"type": "text", "text": "line 1"},
                    {"type": "text", "text": "line 2"},
                ],
            }
        )
        assert result.text_result_for_llm == "line 1\nline 2"

    def test_is_error_maps_to_failure(self):
        result = convert_mcp_call_tool_result(
            {
                "content": [{"type": "text", "text": "oops"}],
                "isError": True,
            }
        )
        assert result.result_type == "failure"

    def test_is_error_false_maps_to_success(self):
        result = convert_mcp_call_tool_result(
            {
                "content": [{"type": "text", "text": "ok"}],
                "isError": False,
            }
        )
        assert result.result_type == "success"

    def test_image_content_to_binary(self):
        result = convert_mcp_call_tool_result(
            {
                "content": [{"type": "image", "data": "base64data", "mimeType": "image/png"}],
            }
        )
        assert result.binary_results_for_llm is not None
        assert len(result.binary_results_for_llm) == 1
        assert result.binary_results_for_llm[0].data == "base64data"
        assert result.binary_results_for_llm[0].mime_type == "image/png"
        assert result.binary_results_for_llm[0].type == "image"

    def test_resource_text_to_text_result(self):
        result = convert_mcp_call_tool_result(
            {
                "content": [
                    {
                        "type": "resource",
                        "resource": {"uri": "file:///data.txt", "text": "file contents"},
                    },
                ],
            }
        )
        assert result.text_result_for_llm == "file contents"

    def test_resource_blob_to_binary(self):
        result = convert_mcp_call_tool_result(
            {
                "content": [
                    {
                        "type": "resource",
                        "resource": {
                            "uri": "file:///img.png",
                            "blob": "blobdata",
                            "mimeType": "image/png",
                        },
                    },
                ],
            }
        )
        assert result.binary_results_for_llm is not None
        assert len(result.binary_results_for_llm) == 1
        assert result.binary_results_for_llm[0].data == "blobdata"
        assert result.binary_results_for_llm[0].mime_type == "image/png"
        assert result.binary_results_for_llm[0].description == "file:///img.png"

    def test_resource_blob_defaults_missing_or_empty_mime_type(self):
        result = convert_mcp_call_tool_result(
            {
                "content": [
                    {
                        "type": "resource",
                        "resource": {"uri": "file:///data.bin", "blob": "binarydata"},
                    },
                    {
                        "type": "resource",
                        "resource": {
                            "uri": "file:///empty-mime.bin",
                            "blob": "binarydata2",
                            "mimeType": "",
                        },
                    },
                ],
            }
        )

        assert result.binary_results_for_llm is not None
        assert len(result.binary_results_for_llm) == 2
        assert result.binary_results_for_llm[0].mime_type == "application/octet-stream"
        assert result.binary_results_for_llm[1].mime_type == "application/octet-stream"

    def test_empty_content_array(self):
        result = convert_mcp_call_tool_result({"content": []})
        assert result.text_result_for_llm == ""
        assert result.result_type == "success"

    def test_call_tool_result_dict_is_json_serialized_by_normalize(self):
        """_normalize_result does NOT auto-detect MCP results; it JSON-serializes them."""
        result = _normalize_result({"content": [{"type": "text", "text": "hello"}]})
        parsed = json.loads(result.text_result_for_llm)
        assert parsed == {"content": [{"type": "text", "text": "hello"}]}


class TestToolReferences:
    def test_tool_references_pass_through_normalize(self):
        input_result = ToolResult(
            text_result_for_llm="found 2 tools",
            result_type="success",
            tool_references=["get_weather", "check_status"],
        )
        result = _normalize_result(input_result)
        assert result.tool_references == ["get_weather", "check_status"]

    def test_tool_references_serialized_to_wire(self):
        wire = ExternalToolTextResultForLlm(
            text_result_for_llm="found 2 tools",
            result_type="success",
            tool_references=["get_weather", "check_status"],
        )
        data = wire.to_dict()
        assert data["toolReferences"] == ["get_weather", "check_status"]

    def test_tool_references_omitted_when_none(self):
        wire = ExternalToolTextResultForLlm(
            text_result_for_llm="ok",
            result_type="success",
        )
        assert "toolReferences" not in wire.to_dict()

    def test_tool_references_round_trip_from_wire(self):
        wire = ExternalToolTextResultForLlm.from_dict(
            {
                "textResultForLlm": "found tools",
                "resultType": "success",
                "toolReferences": ["alpha", "beta"],
            }
        )
        assert wire.tool_references == ["alpha", "beta"]
