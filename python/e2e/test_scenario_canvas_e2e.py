"""Scenario-parity E2Es for canvas provider callback routing."""

from __future__ import annotations

from collections.abc import Sequence

import pytest

from copilot import (
    CanvasAction,
    CanvasDeclaration,
    CanvasError,
    CanvasHandler,
    OpenCanvasInstance,
)
from copilot.rpc import (
    CanvasProviderCloseRequest,
    CanvasProviderInvokeActionRequest,
    CanvasProviderOpenRequest,
    CanvasProviderOpenResult,
)
from copilot.session import PermissionHandler

from ._scenario_fake_cli import create_scenario_client, read_scenario_capture
from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _counter_canvas() -> CanvasDeclaration:
    return CanvasDeclaration(
        id="counter",
        display_name="Counter",
        description="Scenario counter canvas",
        input_schema={"type": "object"},
        actions=[
            CanvasAction(
                name="increment",
                description="Increment the counter",
                input_schema={"type": "object"},
            )
        ],
    )


class _ScenarioCanvasHandler(CanvasHandler):
    def __init__(self, failing_operation: str | None = None, *, structured: bool = True) -> None:
        self.failing_operation = failing_operation
        self.structured = structured
        self.open_calls: list[CanvasProviderOpenRequest] = []
        self.action_calls: list[CanvasProviderInvokeActionRequest] = []
        self.close_calls: list[CanvasProviderCloseRequest] = []

    def _fail_if_requested(self, operation: str) -> None:
        if self.failing_operation != operation:
            return
        if self.structured:
            raise CanvasError("scenario_canvas_error", f"{operation} failed")
        raise RuntimeError(f"{operation} failed unexpectedly")

    async def on_open(self, ctx: CanvasProviderOpenRequest) -> CanvasProviderOpenResult:
        self.open_calls.append(ctx)
        self._fail_if_requested("open")
        return CanvasProviderOpenResult(
            status="ready",
            title="Scenario Counter",
            url="https://example.test/scenario-counter",
        )

    async def on_action(self, ctx: CanvasProviderInvokeActionRequest) -> dict[str, int]:
        self.action_calls.append(ctx)
        self._fail_if_requested("action")
        return {"newValue": 42}

    async def on_close(self, ctx: CanvasProviderCloseRequest) -> None:
        self.close_calls.append(ctx)
        self._fail_if_requested("close")


def _operation_calls(
    handler: _ScenarioCanvasHandler,
    operation: str,
) -> Sequence[
    CanvasProviderOpenRequest | CanvasProviderInvokeActionRequest | CanvasProviderCloseRequest
]:
    if operation == "open":
        return handler.open_calls
    if operation == "action":
        return handler.action_calls
    return handler.close_calls


class TestScenarioCanvas:
    @pytest.mark.parametrize("operation", ["open", "action", "close"])
    async def test_should_preserve_structured_canvas_error_envelope(
        self,
        ctx: E2ETestContext,
        operation: str,
    ):
        client, capture_path = create_scenario_client(ctx, f"canvas-error-{operation}")
        handler = _ScenarioCanvasHandler(operation)
        try:
            session = await client.create_session(
                session_id="scenario-session",
                canvases=[_counter_canvas()],
                canvas_handler=handler,
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                calls = _operation_calls(handler, operation)
                assert len(calls) == 1
                assert calls[0].session_id == "scenario-session"
                assert calls[0].canvas_id == "counter"
                assert calls[0].extension_id == "python-scenario-tests"
                assert calls[0].instance_id == f"scenario-{operation}"
                assert calls[0].host is not None
                assert calls[0].host.capabilities is not None
                assert calls[0].host.capabilities.canvases is True

                capture = read_scenario_capture(capture_path)
                assert capture["callbackResponses"] == [
                    {
                        "jsonrpc": "2.0",
                        "id": "canvas-callback",
                        "error": {
                            "code": -32603,
                            "message": f"{operation} failed",
                            "data": {
                                "code": "scenario_canvas_error",
                                "message": f"{operation} failed",
                            },
                        },
                    }
                ]
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_should_wrap_unexpected_canvas_handler_error(
        self,
        ctx: E2ETestContext,
    ):
        client, capture_path = create_scenario_client(ctx, "canvas-error-open")
        handler = _ScenarioCanvasHandler("open", structured=False)
        try:
            session = await client.create_session(
                session_id="scenario-session",
                canvases=[_counter_canvas()],
                canvas_handler=handler,
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                capture = read_scenario_capture(capture_path)
                assert capture["callbackResponses"][0]["error"] == {
                    "code": -32603,
                    "message": "open failed unexpectedly",
                    "data": {
                        "code": "canvas_handler_error",
                        "message": "open failed unexpectedly",
                    },
                }
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_should_reattach_canvas_and_route_all_callbacks_before_resume_completes(
        self,
        ctx: E2ETestContext,
    ):
        client, capture_path = create_scenario_client(ctx, "canvas-resume")
        first = await client.create_session(
            session_id="scenario-session",
            canvases=[_counter_canvas()],
            canvas_handler=_ScenarioCanvasHandler(),
            on_permission_request=PermissionHandler.approve_all,
        )
        await first.disconnect()

        handler = _ScenarioCanvasHandler()
        resumed = await client.resume_session(
            "scenario-session",
            canvases=[_counter_canvas()],
            canvas_handler=handler,
            open_canvases=[
                OpenCanvasInstance(
                    canvas_id="counter",
                    extension_id="python-scenario-tests",
                    instance_id="reattached-counter",
                    input={"startValue": 3},
                    status="ready",
                )
            ],
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            assert len(handler.open_calls) == 1
            assert handler.open_calls[0].input == {"startValue": 7}
            assert len(handler.action_calls) == 1
            assert handler.action_calls[0].action_name == "increment"
            assert handler.action_calls[0].input == {"amount": 5}
            assert len(handler.close_calls) == 1
            assert handler.close_calls[0].instance_id == "scenario-close"

            capture = read_scenario_capture(capture_path)
            assert capture["callbackResponses"] == [
                {
                    "jsonrpc": "2.0",
                    "id": "resume-open",
                    "result": {
                        "status": "ready",
                        "title": "Scenario Counter",
                        "url": "https://example.test/scenario-counter",
                    },
                },
                {
                    "jsonrpc": "2.0",
                    "id": "resume-action",
                    "result": {"newValue": 42},
                },
                {
                    "jsonrpc": "2.0",
                    "id": "resume-close",
                    "result": None,
                },
            ]
        finally:
            await resumed.disconnect()
            await client.stop()
