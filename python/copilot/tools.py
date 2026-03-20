"""
Tool definition utilities for the Copilot SDK.

Provides a decorator-based API for defining tools with automatic JSON schema
generation from Pydantic models.
"""

from __future__ import annotations

import inspect
import json
from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from typing import Any, Literal, TypeVar, get_type_hints, overload

from pydantic import BaseModel

ToolResultType = Literal["success", "failure", "rejected", "denied"]


@dataclass
class ToolBinaryResult:
    """Binary content returned by a tool."""

    data: str = ""
    mime_type: str = ""
    type: str = ""
    description: str = ""


@dataclass
class ToolResult:
    """Result of a tool invocation."""

    text_result_for_llm: str = ""
    result_type: ToolResultType = "success"
    error: str | None = None
    binary_results_for_llm: list[ToolBinaryResult] | None = None
    session_log: str | None = None
    tool_telemetry: dict[str, Any] | None = None


@dataclass
class ToolInvocation:
    """Context passed to a tool handler when invoked."""

    session_id: str = ""
    tool_call_id: str = ""
    tool_name: str = ""
    arguments: Any = None


ToolHandler = Callable[[ToolInvocation], ToolResult | Awaitable[ToolResult]]


@dataclass
class Tool:
    name: str
    description: str
    handler: ToolHandler
    parameters: dict[str, Any] | None = None
    overrides_built_in_tool: bool = False
    skip_permission: bool = False


T = TypeVar("T", bound=BaseModel)
R = TypeVar("R")


@overload
def define_tool(
    name: str | None = None,
    *,
    description: str | None = None,
    overrides_built_in_tool: bool = False,
    skip_permission: bool = False,
) -> Callable[[Callable[..., Any]], Tool]: ...


@overload
def define_tool(
    name: str,
    *,
    description: str | None = None,
    handler: Callable[[T, ToolInvocation], R],
    params_type: type[T],
    overrides_built_in_tool: bool = False,
    skip_permission: bool = False,
) -> Tool: ...


def define_tool(
    name: str | None = None,
    *,
    description: str | None = None,
    handler: Callable[[Any, ToolInvocation], Any] | None = None,
    params_type: type[BaseModel] | None = None,
    overrides_built_in_tool: bool = False,
    skip_permission: bool = False,
) -> Tool | Callable[[Callable[[Any, ToolInvocation], Any]], Tool]:
    """
    Define a tool with automatic JSON schema generation from Pydantic models.

    Can be used as a decorator or as a function:

    Decorator usage (recommended):

        from pydantic import BaseModel, Field

        class LookupIssueParams(BaseModel):
            id: str = Field(description="Issue identifier")

        @define_tool(description="Fetch issue details")
        def lookup_issue(params: LookupIssueParams) -> str:
            return fetch_issue(params.id).summary

    Function usage:

        tool = define_tool(
            "lookup_issue",
            description="Fetch issue details",
            handler=lambda params, inv: fetch_issue(params.id).summary,
            params_type=LookupIssueParams
        )

    Args:
        name: The tool name (defaults to function name)
        description: Description of what the tool does (shown to the LLM)
        handler: Optional handler function (if not using as decorator)
        params_type: Optional Pydantic model type for parameters (inferred from
                    type hints when using as decorator)
        overrides_built_in_tool: When True, explicitly indicates this tool is intended
                    to override a built-in tool of the same name. If not set and the
                    name clashes with a built-in tool, the runtime will return an error.
        skip_permission: When True, the tool can execute without a permission prompt.

    Returns:
        A Tool instance
    """

    def decorator(fn: Callable[..., Any]) -> Tool:
        tool_name = name if name is not None else getattr(fn, "__name__", "unknown")

        sig = inspect.signature(fn)
        param_names = list(sig.parameters.keys())
        hints = get_type_hints(fn)
        num_params = len(param_names)

        # Detect handler signature:
        # - 0 params: handler()
        # - 1 param, ToolInvocation: handler(invocation)
        # - 1 param, Pydantic: handler(params)
        # - 2 params: handler(params, invocation)
        ptype = params_type
        first_param_type = hints.get(param_names[0]) if param_names else None

        if num_params == 0:
            takes_params = False
            takes_invocation = False
        elif num_params == 1 and first_param_type is ToolInvocation:
            takes_params = False
            takes_invocation = True
        else:
            takes_params = True
            takes_invocation = num_params >= 2
            if ptype is None and _is_pydantic_model(first_param_type):
                ptype = first_param_type

        # Generate schema from Pydantic model
        schema = None
        if ptype is not None and _is_pydantic_model(ptype):
            schema = ptype.model_json_schema()

        async def wrapped_handler(invocation: ToolInvocation) -> ToolResult:
            try:
                # Build args based on detected signature
                call_args = []
                if takes_params:
                    args = invocation.arguments or {}
                    if ptype is not None and _is_pydantic_model(ptype):
                        call_args.append(ptype.model_validate(args))
                    else:
                        call_args.append(args)
                if takes_invocation:
                    call_args.append(invocation)

                result = fn(*call_args)

                if inspect.isawaitable(result):
                    result = await result

                return _normalize_result(result)

            except Exception as exc:
                # Don't expose detailed error information to the LLM for security reasons.
                # The actual error is stored in the 'error' field for debugging.
                return ToolResult(
                    text_result_for_llm="Invoking this tool produced an error. "
                    "Detailed information is not available.",
                    result_type="failure",
                    error=str(exc),
                    tool_telemetry={},
                )

        return Tool(
            name=tool_name,
            description=description or "",
            parameters=schema,
            handler=wrapped_handler,
            overrides_built_in_tool=overrides_built_in_tool,
            skip_permission=skip_permission,
        )

    # If handler is provided, call decorator immediately
    if handler is not None:
        if name is None:
            raise ValueError("name is required when using define_tool with handler=")
        return decorator(handler)

    # Otherwise return decorator for @define_tool(...) usage
    return decorator


def _is_pydantic_model(t: Any) -> bool:
    """Check if a type is a Pydantic BaseModel subclass."""
    try:
        return isinstance(t, type) and issubclass(t, BaseModel)
    except TypeError:
        return False


def _normalize_result(result: Any) -> ToolResult:
    """
    Convert any return value to a ToolResult.

    - None returns empty success
    - Strings pass through directly
    - ToolResult passes through
    - Everything else gets JSON-serialized (with Pydantic support)
    """
    if result is None:
        return ToolResult(
            text_result_for_llm="",
            result_type="success",
        )

    # ToolResult dataclass passes through directly
    if isinstance(result, ToolResult):
        return result

    # Strings pass through directly
    if isinstance(result, str):
        return ToolResult(
            text_result_for_llm=result,
            result_type="success",
        )

    # Everything else gets JSON-serialized (with Pydantic model support)
    def default(obj: Any) -> Any:
        if isinstance(obj, BaseModel):
            return obj.model_dump()
        raise TypeError(f"Object of type {type(obj).__name__} is not JSON serializable")

    try:
        json_str = json.dumps(result, default=default)
    except (TypeError, ValueError) as exc:
        raise TypeError(f"Failed to serialize tool result: {exc}") from exc

    return ToolResult(
        text_result_for_llm=json_str,
        result_type="success",
    )
