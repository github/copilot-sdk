"""Unit tests for the canvas SDK surface."""

from __future__ import annotations

import threading
from typing import Any

import pytest

from copilot._jsonrpc import JsonRpcError
from copilot.canvas import (
    CanvasAction,
    CanvasActionContext,
    CanvasDeclaration,
    CanvasError,
    CanvasHandler,
    CanvasOpenContext,
    CanvasOpenResponse,
    ExtensionInfo,
    OpenCanvasInstance,
    _action_context_from_params,
    _lifecycle_context_from_params,
    _open_context_from_params,
)
from copilot.client import CopilotClient


def test_canvas_declaration_serializes_camelcase_and_drops_optional():
    decl = CanvasDeclaration(
        id="my-canvas",
        display_name="My Canvas",
        description="Does the thing",
    )
    assert decl.to_dict() == {
        "id": "my-canvas",
        "displayName": "My Canvas",
        "description": "Does the thing",
    }


def test_canvas_declaration_serializes_input_schema_and_actions():
    action = CanvasAction(
        name="refresh",
        description="Refresh the canvas",
    )
    decl = CanvasDeclaration(
        id="c",
        display_name="C",
        description="D",
        input_schema={"type": "object"},
        actions=[action],
    )
    payload = decl.to_dict()
    assert payload["inputSchema"] == {"type": "object"}
    assert payload["actions"] == [action.to_dict()]


def test_extension_info_serializes():
    info = ExtensionInfo(source="github-app", name="my-ext")
    assert info.to_dict() == {"source": "github-app", "name": "my-ext"}


def test_canvas_open_response_drops_none_fields():
    assert CanvasOpenResponse().to_dict() == {}
    assert CanvasOpenResponse(url="https://x", status="ok").to_dict() == {
        "url": "https://x",
        "status": "ok",
    }


def test_canvas_error_envelope_and_factories():
    err = CanvasError("oops", "something broke")
    assert err.code == "oops"
    assert err.message == "something broke"
    assert err.to_envelope() == {"code": "oops", "message": "something broke"}

    no_handler = CanvasError.no_handler()
    assert no_handler.code == "canvas_action_no_handler"

    unset = CanvasError.handler_unset()
    assert unset.code == "canvas_handler_unset"


async def test_default_canvas_handler_on_action_raises_no_handler():
    class StubHandler(CanvasHandler):
        async def on_open(self, ctx: CanvasOpenContext) -> CanvasOpenResponse:
            return CanvasOpenResponse()

    handler = StubHandler()
    ctx = CanvasActionContext(
        session_id="s",
        extension_id="e",
        canvas_id="c",
        instance_id="i",
        action_name="any",
        input=None,
    )
    with pytest.raises(CanvasError) as excinfo:
        await handler.on_action(ctx)
    assert excinfo.value.code == "canvas_action_no_handler"


def test_context_helpers_parse_params():
    base = {
        "sessionId": "s",
        "extensionId": "e",
        "canvasId": "c",
        "instanceId": "i",
        "input": {"foo": 1},
        "host": {"capabilities": {"canvases": True}},
    }
    open_ctx = _open_context_from_params(base)
    assert open_ctx.session_id == "s"
    assert open_ctx.canvas_id == "c"
    assert open_ctx.input == {"foo": 1}
    assert open_ctx.host is not None and open_ctx.host.capabilities.canvases is True

    close_ctx = _lifecycle_context_from_params(base)
    assert close_ctx.canvas_id == "c"
    assert close_ctx.instance_id == "i"

    action_ctx = _action_context_from_params({**base, "actionName": "refresh"})
    assert action_ctx.action_name == "refresh"


class _StubSession:
    """Minimal CopilotSession stand-in for the inbound dispatch tests."""

    def __init__(self, handler: CanvasHandler | None) -> None:
        self._handler = handler
        self._open_canvases: list[OpenCanvasInstance] = []
        self._open_canvases_lock = threading.Lock()

    def _get_canvas_handler(self) -> CanvasHandler | None:
        return self._handler

    def _set_open_canvases(self, instances: list[OpenCanvasInstance]) -> None:
        with self._open_canvases_lock:
            self._open_canvases = list(instances)

    @property
    def open_canvases(self) -> list[OpenCanvasInstance]:
        with self._open_canvases_lock:
            return list(self._open_canvases)


def _make_client_with_session(session_id: str, session: Any) -> CopilotClient:
    """Construct a CopilotClient skeleton sufficient for testing the inbound
    canvas dispatch helpers without actually launching the CLI."""
    client = CopilotClient.__new__(CopilotClient)
    client._sessions = {session_id: session}
    client._sessions_lock = threading.Lock()
    return client


async def test_handle_canvas_open_dispatches_to_handler():
    class Handler(CanvasHandler):
        def __init__(self) -> None:
            self.received: CanvasOpenContext | None = None

        async def on_open(self, ctx: CanvasOpenContext) -> CanvasOpenResponse:
            self.received = ctx
            return CanvasOpenResponse(url="https://canvas.example", title="Hi")

        async def on_action(self, ctx: CanvasActionContext) -> Any:
            return {"echo": ctx.input}

    handler = Handler()
    session = _StubSession(handler)
    client = _make_client_with_session("sess-1", session)

    result = await client._handle_canvas_open(
        {
            "sessionId": "sess-1",
            "extensionId": "ext",
            "canvasId": "c",
            "instanceId": "i",
            "input": {"q": 1},
        }
    )
    assert result == {"url": "https://canvas.example", "title": "Hi"}
    assert handler.received is not None
    assert handler.received.canvas_id == "c"


async def test_handle_canvas_open_raises_when_handler_unset():
    session = _StubSession(handler=None)
    client = _make_client_with_session("sess-1", session)

    with pytest.raises(CanvasError) as excinfo:
        await client._handle_canvas_open(
            {
                "sessionId": "sess-1",
                "extensionId": "ext",
                "canvasId": "c",
                "instanceId": "i",
            }
        )
    assert excinfo.value.code == "canvas_handler_unset"


async def test_handle_canvas_action_returns_arbitrary_value():
    class Handler(CanvasHandler):
        async def on_open(self, ctx: CanvasOpenContext) -> CanvasOpenResponse:
            return CanvasOpenResponse()

        async def on_action(self, ctx: CanvasActionContext) -> Any:
            return [1, 2, 3]

    client = _make_client_with_session("sess-1", _StubSession(Handler()))
    result = await client._handle_canvas_action_invoke(
        {
            "sessionId": "sess-1",
            "extensionId": "ext",
            "canvasId": "c",
            "instanceId": "i",
            "actionName": "do",
        }
    )
    assert result == [1, 2, 3]


async def test_canvas_request_handler_translates_canvas_error():
    err = CanvasError("bad", "fail")

    async def coro(params: dict) -> Any:
        raise err

    wrapped = CopilotClient._canvas_request_handler(coro)
    with pytest.raises(JsonRpcError) as excinfo:
        await wrapped({})
    assert excinfo.value.code == -32603
    assert excinfo.value.message == "fail"
    assert excinfo.value.data == {"code": "bad", "message": "fail"}


def test_set_open_canvases_round_trip():
    from copilot.generated.rpc import CanvasInstanceAvailability

    inst = OpenCanvasInstance(
        availability=CanvasInstanceAvailability.READY,
        canvas_id="c",
        extension_id="e",
        instance_id="i",
        reopen=False,
    )
    session = _StubSession(handler=None)
    session._set_open_canvases([inst])
    assert session.open_canvases == [inst]
