"""Unit tests for the per-call tool context provider.

The provider is registered on a session and invoked once per tool call to
populate ``ToolInvocation.context`` before the handler runs. These tests drive
``CopilotSession._execute_tool_and_respond`` directly with a fake RPC so the
injection path is exercised without a live runtime connection.
"""

from __future__ import annotations

from typing import Any

from copilot import define_tool
from copilot.session import CopilotSession
from copilot.tools import ToolInvocation


class _FakeToolsRpc:
    def __init__(self) -> None:
        self.calls: list[Any] = []

    async def handle_pending_tool_call(self, request: Any) -> None:
        self.calls.append(request)


class _FakeRpc:
    def __init__(self) -> None:
        self.tools = _FakeToolsRpc()


def _session_with_fake_rpc(session_id: str = "sess-1") -> CopilotSession:
    session = CopilotSession(session_id, client=None)
    session._rpc = _FakeRpc()  # type: ignore[assignment]
    return session


async def test_provider_value_injected_into_invocation_context():
    seen: dict[str, Any] = {}

    @define_tool("echo", description="Echo tool")
    def echo(invocation: ToolInvocation) -> str:
        seen["context"] = invocation.context
        return "ok"

    session = _session_with_fake_rpc()
    session._register_tool_context_provider(lambda inv: {"user": "alice", "tool": inv.tool_name})

    await session._execute_tool_and_respond(
        request_id="r1",
        tool_name="echo",
        tool_call_id="c1",
        arguments={},
        handler=echo.handler,
    )

    assert seen["context"] == {"user": "alice", "tool": "echo"}


async def test_async_provider_is_awaited():
    seen: dict[str, Any] = {}

    @define_tool("echo", description="Echo tool")
    def echo(invocation: ToolInvocation) -> str:
        seen["context"] = invocation.context
        return "ok"

    async def provider(_: ToolInvocation) -> dict[str, Any]:
        return {"async": True}

    session = _session_with_fake_rpc()
    session._register_tool_context_provider(provider)

    await session._execute_tool_and_respond(
        request_id="r1",
        tool_name="echo",
        tool_call_id="c1",
        arguments={},
        handler=echo.handler,
    )

    assert seen["context"] == {"async": True}


async def test_provider_receives_full_invocation():
    seen: dict[str, ToolInvocation] = {}

    @define_tool("echo", description="Echo tool")
    def echo(invocation: ToolInvocation) -> str:
        return "ok"

    def provider(inv: ToolInvocation) -> str:
        seen["invocation"] = inv
        return "ctx"

    session = _session_with_fake_rpc("sess-42")
    session._register_tool_context_provider(provider)

    await session._execute_tool_and_respond(
        request_id="r1",
        tool_name="echo",
        tool_call_id="call-7",
        arguments={"q": "hello"},
        handler=echo.handler,
    )

    inv = seen["invocation"]
    assert inv.session_id == "sess-42"
    assert inv.tool_name == "echo"
    assert inv.tool_call_id == "call-7"
    assert inv.arguments == {"q": "hello"}


async def test_no_provider_leaves_context_none():
    seen: dict[str, Any] = {}

    @define_tool("echo", description="Echo tool")
    def echo(invocation: ToolInvocation) -> str:
        seen["context"] = invocation.context
        return "ok"

    session = _session_with_fake_rpc()

    await session._execute_tool_and_respond(
        request_id="r1",
        tool_name="echo",
        tool_call_id="c1",
        arguments={},
        handler=echo.handler,
    )

    assert seen["context"] is None


async def test_provider_returning_none_leaves_context_none():
    seen: dict[str, Any] = {}

    @define_tool("echo", description="Echo tool")
    def echo(invocation: ToolInvocation) -> str:
        seen["context"] = invocation.context
        return "ok"

    session = _session_with_fake_rpc()
    session._register_tool_context_provider(lambda _: None)

    await session._execute_tool_and_respond(
        request_id="r1",
        tool_name="echo",
        tool_call_id="c1",
        arguments={},
        handler=echo.handler,
    )

    assert seen["context"] is None


def test_register_and_clear_provider_round_trip():
    session = CopilotSession("sess-1", client=None)
    assert session._get_tool_context_provider() is None

    def provider(_: ToolInvocation) -> str:
        return "ctx"

    session._register_tool_context_provider(provider)
    assert session._get_tool_context_provider() is provider

    session._register_tool_context_provider(None)
    assert session._get_tool_context_provider() is None
