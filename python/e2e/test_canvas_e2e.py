"""E2E coverage for canvas runtime dispatch."""

from __future__ import annotations

import asyncio
from typing import Any

import pytest

from copilot import (
    CanvasAction,
    CanvasActionContext,
    CanvasDeclaration,
    CanvasHandler,
    CanvasLifecycleContext,
    CanvasOpenContext,
    CanvasOpenResponse,
    ExtensionInfo,
)
from copilot._jsonrpc import JsonRpcError
from copilot.generated.rpc import (
    CanvasCloseRequest,
    CanvasInstanceAvailability,
    CanvasInvokeActionRequest,
    CanvasOpenRequest,
)

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


_EXTENSION_INFO = ExtensionInfo(source="github-app", name="counter-provider")


def _counter_declaration(*, actions: list[CanvasAction] | None = None) -> CanvasDeclaration:
    return CanvasDeclaration(
        id="counter",
        display_name="Counter",
        description="A test counter canvas",
        actions=actions,
    )


class _CounterHandler(CanvasHandler):
    def __init__(self) -> None:
        self.opens: list[CanvasOpenContext] = []
        self.closes: list[CanvasLifecycleContext] = []
        self.actions: list[CanvasActionContext] = []

    async def on_open(self, ctx: CanvasOpenContext) -> CanvasOpenResponse:
        self.opens.append(ctx)
        return CanvasOpenResponse(url=f"https://example.test/{ctx.instance_id}")

    async def on_close(self, ctx: CanvasLifecycleContext) -> None:
        self.closes.append(ctx)

    async def on_action(self, ctx: CanvasActionContext) -> Any:
        self.actions.append(ctx)
        return {"ok": True, "actionName": ctx.action_name, "input": ctx.input}


class _NoActionHandler(CanvasHandler):
    async def on_open(self, ctx: CanvasOpenContext) -> CanvasOpenResponse:
        return CanvasOpenResponse(url=f"https://example.test/{ctx.instance_id}")


async def _create_counter_session(
    ctx: E2ETestContext,
    handler: CanvasHandler,
    *,
    actions: list[CanvasAction] | None = None,
):
    return await ctx.client.create_session(
        canvases=[_counter_declaration(actions=actions)],
        request_canvas_renderer=True,
        extension_info=_EXTENSION_INFO,
        canvas_handler=handler,
    )


class TestCanvas:
    async def test_dispatches_canvas_open_to_the_provider_handler(self, ctx: E2ETestContext):
        handler = _CounterHandler()
        session = await _create_counter_session(ctx, handler)

        try:
            result = await session.rpc.canvas.open(
                CanvasOpenRequest(
                    canvas_id="counter",
                    instance_id="counter-1",
                    input={"seed": 7},
                )
            )

            assert len(handler.opens) == 1
            opened = handler.opens[0]
            assert opened.canvas_id == "counter"
            assert opened.instance_id == "counter-1"
            assert opened.input == {"seed": 7}
            assert result.canvas_id == "counter"
            assert result.instance_id == "counter-1"
            assert result.url == "https://example.test/counter-1"
            assert result.availability == CanvasInstanceAvailability.READY
        finally:
            await session.disconnect()

    async def test_dispatches_canvas_action_invoke_to_the_per_action_handler(
        self, ctx: E2ETestContext
    ):
        handler = _CounterHandler()
        session = await _create_counter_session(
            ctx,
            handler,
            actions=[CanvasAction(name="increment", description="Increment the counter")],
        )
        try:
            await session.rpc.canvas.open(
                CanvasOpenRequest(canvas_id="counter", instance_id="counter-2")
            )

            result = await session.rpc.canvas.invoke_action(
                CanvasInvokeActionRequest(
                    action_name="increment",
                    instance_id="counter-2",
                    input={"amount": 3},
                )
            )

            assert len(handler.actions) == 1
            action = handler.actions[0]
            assert action.canvas_id == "counter"
            assert action.instance_id == "counter-2"
            assert action.action_name == "increment"
            assert action.input == {"amount": 3}
            assert result.result == {
                "ok": True,
                "actionName": "increment",
                "input": {"amount": 3},
            }
        finally:
            await session.disconnect()

    async def test_dispatches_canvas_close_to_the_provider_on_close_handler(
        self, ctx: E2ETestContext
    ):
        handler = _CounterHandler()
        session = await _create_counter_session(ctx, handler)

        try:
            await session.rpc.canvas.open(
                CanvasOpenRequest(canvas_id="counter", instance_id="counter-3")
            )
            await session.rpc.canvas.close(CanvasCloseRequest(instance_id="counter-3"))
            await asyncio.sleep(0.05)

            assert len(handler.closes) == 1
            closed = handler.closes[0]
            assert closed.canvas_id == "counter"
            assert closed.instance_id == "counter-3"
        finally:
            await session.disconnect()

    async def test_returns_canvas_action_no_handler_when_declared_action_has_no_handler(
        self, ctx: E2ETestContext
    ):
        session = await _create_counter_session(
            ctx,
            _NoActionHandler(),
            actions=[CanvasAction(name="increment", description="Increment the counter")],
        )
        try:
            await session.rpc.canvas.open(
                CanvasOpenRequest(canvas_id="counter", instance_id="counter-4")
            )

            with pytest.raises(JsonRpcError) as excinfo:
                await session.rpc.canvas.invoke_action(
                    CanvasInvokeActionRequest(
                        action_name="increment",
                        instance_id="counter-4",
                        input={},
                    )
                )

            assert excinfo.value.data == {
                "code": "canvas_action_no_handler",
                "message": "No handler implemented for this canvas action",
            }
        finally:
            await session.disconnect()

    async def test_seeds_open_canvases_on_resume_from_the_runtime_resume_response(
        self, ctx: E2ETestContext
    ):
        session_a = await _create_counter_session(ctx, _CounterHandler())
        try:
            await session_a.rpc.canvas.open(
                CanvasOpenRequest(
                    canvas_id="counter",
                    instance_id="counter-resume",
                    input={"initial": True},
                )
            )

            resumed = await ctx.client.resume_session(
                session_a.session_id,
                canvases=[_counter_declaration()],
                request_canvas_renderer=True,
                extension_info=_EXTENSION_INFO,
                canvas_handler=_CounterHandler(),
            )

            try:
                matching = [
                    canvas
                    for canvas in resumed.open_canvases
                    if canvas.instance_id == "counter-resume"
                ]
                assert len(matching) == 1
                assert matching[0].canvas_id == "counter"
            finally:
                await resumed.disconnect()
        finally:
            await session_a.disconnect()
