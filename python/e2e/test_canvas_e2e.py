"""E2E tests for canvas RPCs."""

from __future__ import annotations

import pytest

from copilot import (
    CanvasAction,
    CanvasDeclaration,
    CanvasHandler,
)
from copilot.generated.rpc import (
    CanvasCloseRequest,
    CanvasInvokeActionRequest,
    CanvasOpenRequest,
    CanvasProviderCloseRequest,
    CanvasProviderInvokeActionRequest,
    CanvasProviderOpenRequest,
    CanvasProviderOpenResult,
)
from copilot.session import CopilotSession, PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class _CounterCanvasHandler(CanvasHandler):
    def __init__(self) -> None:
        self.open_calls: list[CanvasProviderOpenRequest] = []
        self.action_calls: list[CanvasProviderInvokeActionRequest] = []
        self.close_calls: list[CanvasProviderCloseRequest] = []

    async def on_open(self, ctx: CanvasProviderOpenRequest) -> CanvasProviderOpenResult:
        self.open_calls.append(ctx)
        return CanvasProviderOpenResult(
            url="https://example.test/counter",
            title="Counter Canvas",
            status="ready",
        )

    async def on_close(self, ctx: CanvasProviderCloseRequest) -> None:
        self.close_calls.append(ctx)

    async def on_action(self, ctx: CanvasProviderInvokeActionRequest) -> dict[str, int]:
        self.action_calls.append(ctx)
        return {"newValue": 42}


def _counter_canvas() -> CanvasDeclaration:
    return CanvasDeclaration(
        id="counter",
        display_name="Counter",
        description="A simple counter canvas for e2e testing",
        input_schema={
            "type": "object",
            "properties": {"startValue": {"type": "number"}},
        },
        actions=[
            CanvasAction(
                name="increment",
                description="Increment the counter",
                input_schema={
                    "type": "object",
                    "properties": {"amount": {"type": "number"}},
                },
            )
        ],
    )


async def _create_counter_session(
    ctx: E2ETestContext,
) -> tuple[_CounterCanvasHandler, CopilotSession]:
    handler = _CounterCanvasHandler()
    session = await ctx.client.create_session(
        on_permission_request=PermissionHandler.approve_all,
        canvases=[_counter_canvas()],
        canvas_handler=handler,
    )
    return handler, session


class TestCanvasRpc:
    async def test_should_list_canvases(self, ctx: E2ETestContext):
        _handler, session = await _create_counter_session(ctx)
        try:
            result = await session.rpc.canvas.list()

            assert len(result.canvases) == 1
            assert result.canvases[0].canvas_id == "counter"
            assert result.canvases[0].display_name == "Counter"
            assert result.canvases[0].description == "A simple counter canvas for e2e testing"
        finally:
            await session.disconnect()

    async def test_should_round_trip_canvas_open(self, ctx: E2ETestContext):
        handler, session = await _create_counter_session(ctx)
        try:
            result = await session.rpc.canvas.open(
                CanvasOpenRequest(
                    canvas_id="counter",
                    instance_id="counter-1",
                    input={"startValue": 10},
                )
            )

            assert result.url == "https://example.test/counter"
            assert result.title == "Counter Canvas"
            assert result.status == "ready"
            assert len(handler.open_calls) == 1
            assert handler.open_calls[0].canvas_id == "counter"
            assert handler.open_calls[0].instance_id == "counter-1"
            assert handler.open_calls[0].input == {"startValue": 10}

            open_list = await session.rpc.canvas.list_open()
            assert len(open_list.open_canvases) == 1
            assert open_list.open_canvases[0].instance_id == "counter-1"
        finally:
            await session.disconnect()

    async def test_should_invoke_canvas_action(self, ctx: E2ETestContext):
        handler, session = await _create_counter_session(ctx)
        try:
            await session.rpc.canvas.open(
                CanvasOpenRequest(
                    canvas_id="counter",
                    instance_id="counter-2",
                    input={},
                )
            )

            result = await session.rpc.canvas.invoke_action(
                CanvasInvokeActionRequest(
                    action_name="increment",
                    instance_id="counter-2",
                    input={"amount": 5},
                )
            )

            assert result == {"result": {"newValue": 42}}
            assert len(handler.action_calls) == 1
            assert handler.action_calls[0].canvas_id == "counter"
            assert handler.action_calls[0].instance_id == "counter-2"
            assert handler.action_calls[0].action_name == "increment"
            assert handler.action_calls[0].input == {"amount": 5}
        finally:
            await session.disconnect()

    async def test_should_run_close_lifecycle(self, ctx: E2ETestContext):
        handler, session = await _create_counter_session(ctx)
        try:
            await session.rpc.canvas.open(
                CanvasOpenRequest(
                    canvas_id="counter",
                    instance_id="counter-3",
                    input={},
                )
            )
            await session.rpc.canvas.close(CanvasCloseRequest(instance_id="counter-3"))

            assert len(handler.close_calls) == 1
            assert handler.close_calls[0].canvas_id == "counter"
            assert handler.close_calls[0].instance_id == "counter-3"

            open_list = await session.rpc.canvas.list_open()
            assert open_list.open_canvases == []
        finally:
            await session.disconnect()
