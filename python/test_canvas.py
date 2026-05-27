"""Unit tests for the canvas SDK surface."""

from __future__ import annotations

from typing import Any, cast

import pytest

from copilot._jsonrpc import JsonRpcError
from copilot.canvas import (
    CanvasAction,
    CanvasDeclaration,
    CanvasError,
    CanvasHandler,
    ExtensionInfo,
    OpenCanvasInstance,
)
from copilot.generated.rpc import (
    CanvasInstanceAvailability,
    CanvasProviderCloseRequest,
    CanvasProviderInvokeActionRequest,
    CanvasProviderOpenRequest,
    CanvasProviderOpenResult,
)
from copilot.session import CopilotSession


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
    assert CanvasProviderOpenResult().to_dict() == {}
    assert CanvasProviderOpenResult(url="https://x", status="ok").to_dict() == {
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
        async def on_open(self, ctx: CanvasProviderOpenRequest) -> CanvasProviderOpenResult:
            return CanvasProviderOpenResult()

    handler = StubHandler()
    ctx = CanvasProviderInvokeActionRequest(
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


async def test_register_canvas_handler_wires_generated_canvas_adapter():
    class Handler(CanvasHandler):
        def __init__(self) -> None:
            self.open_calls: list[CanvasProviderOpenRequest] = []
            self.close_calls: list[CanvasProviderCloseRequest] = []
            self.action_calls: list[CanvasProviderInvokeActionRequest] = []

        async def on_open(self, ctx: CanvasProviderOpenRequest) -> CanvasProviderOpenResult:
            self.open_calls.append(ctx)
            return CanvasProviderOpenResult(
                url="https://canvas.example", title="Hi", status="ready"
            )

        async def on_close(self, ctx: CanvasProviderCloseRequest) -> None:
            self.close_calls.append(ctx)

        async def on_action(self, ctx: CanvasProviderInvokeActionRequest) -> Any:
            self.action_calls.append(ctx)
            return {"echo": ctx.input}

    session = CopilotSession("sess-1", client=None)
    handler = Handler()
    session._register_canvas_handler(handler)

    adapter = session._client_session_apis.canvas
    assert adapter is not None
    assert session._get_canvas_handler() is handler

    open_request = CanvasProviderOpenRequest(
        canvas_id="c",
        extension_id="ext",
        instance_id="i",
        session_id="sess-1",
        input={"q": 1},
    )
    open_result = await adapter.open(open_request)
    assert open_result.to_dict() == {
        "url": "https://canvas.example",
        "title": "Hi",
        "status": "ready",
    }
    assert handler.open_calls == [open_request]

    close_request = CanvasProviderCloseRequest(
        canvas_id="c",
        extension_id="ext",
        instance_id="i",
        session_id="sess-1",
    )
    await adapter.close(close_request)
    assert handler.close_calls == [close_request]

    action_request = CanvasProviderInvokeActionRequest(
        action_name="refresh",
        canvas_id="c",
        extension_id="ext",
        instance_id="i",
        session_id="sess-1",
        input={"value": 1},
    )
    action_result = await adapter.invoke(action_request)
    assert action_result == {"echo": {"value": 1}}
    assert handler.action_calls == [action_request]


async def test_canvas_adapter_translates_canvas_error_to_jsonrpc_error():
    class Handler(CanvasHandler):
        async def on_open(self, ctx: CanvasProviderOpenRequest) -> CanvasProviderOpenResult:
            raise CanvasError("bad", "fail")

    session = CopilotSession("sess-1", client=None)
    session._register_canvas_handler(Handler())

    adapter = cast(Any, session._client_session_apis.canvas)
    with pytest.raises(JsonRpcError) as excinfo:
        await adapter.open(
            CanvasProviderOpenRequest(
                canvas_id="c",
                extension_id="ext",
                instance_id="i",
                session_id="sess-1",
            )
        )
    assert excinfo.value.code == -32603
    assert excinfo.value.message == "fail"
    assert excinfo.value.data == {"code": "bad", "message": "fail"}


def test_register_canvas_handler_can_clear_generated_handler():
    session = CopilotSession("sess-1", client=None)
    session._register_canvas_handler(None)
    assert session._client_session_apis.canvas is None


def test_set_open_canvases_round_trip():
    inst = OpenCanvasInstance(
        availability=CanvasInstanceAvailability.READY,
        canvas_id="c",
        extension_id="e",
        instance_id="i",
        reopen=False,
    )
    session = CopilotSession("sess-1", client=None)
    session._set_open_canvases([inst])
    assert session.open_canvases == [inst]
