"""E2E tests for mode handlers."""

from __future__ import annotations

import asyncio

import pytest

from copilot.generated.session_events import (
    AutoModeSwitchCompletedData,
    AutoModeSwitchRequestedData,
    ExitPlanModeCompletedData,
    ExitPlanModeRequestedData,
    SessionIdleData,
    SessionModelChangeData,
)
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

MODE_HANDLER_TOKEN = "mode-handler-token"
PLAN_SUMMARY = "Greeting file implementation plan"
PLAN_PROMPT = (
    "Create a brief implementation plan for adding a greeting.txt file, then request "
    "approval with exit_plan_mode."
)
AUTO_MODE_PROMPT = "Explain that auto mode recovered from a rate limit in one short sentence."


@pytest.fixture(scope="module")
async def mode_ctx(ctx: E2ETestContext):
    """Configure per-token user responses for mode-handler tests."""
    proxy_url = ctx.proxy_url
    ctx.client._config.env["COPILOT_DEBUG_GITHUB_API_URL"] = proxy_url

    await ctx.set_copilot_user_by_token(
        MODE_HANDLER_TOKEN,
        {
            "login": "mode-handler-user",
            "copilot_plan": "individual_pro",
            "endpoints": {
                "api": proxy_url,
                "telemetry": "https://localhost:1/telemetry",
            },
            "analytics_tracking_id": "mode-handler-tracking-id",
        },
    )

    return ctx


async def _wait_for_event(session, predicate, timeout: float = 30.0):
    """Wait for the first session event matching predicate."""
    loop = asyncio.get_event_loop()
    fut: asyncio.Future = loop.create_future()

    def on_event(event):
        if not fut.done() and predicate(event):
            fut.set_result(event)

    unsubscribe = session.on(on_event)
    try:
        return await asyncio.wait_for(fut, timeout=timeout)
    finally:
        unsubscribe()


class TestModeHandlers:
    async def test_should_invoke_exit_plan_mode_handler_when_model_uses_tool(
        self, mode_ctx: E2ETestContext
    ):
        exit_plan_mode_requests = []

        async def on_exit_plan_mode(request, invocation):
            exit_plan_mode_requests.append(request)
            assert invocation["session_id"] == session.session_id
            return {
                "approved": True,
                "selectedAction": "interactive",
                "feedback": "Approved by the Python E2E test",
            }

        session = await mode_ctx.client.create_session(
            github_token=MODE_HANDLER_TOKEN,
            on_permission_request=PermissionHandler.approve_all,
            on_exit_plan_mode=on_exit_plan_mode,
        )

        try:
            requested_event = asyncio.create_task(
                _wait_for_event(
                    session,
                    lambda event: (
                        isinstance(event.data, ExitPlanModeRequestedData)
                        and event.data.summary == PLAN_SUMMARY
                    ),
                )
            )
            completed_event = asyncio.create_task(
                _wait_for_event(
                    session,
                    lambda event: (
                        isinstance(event.data, ExitPlanModeCompletedData)
                        and event.data.approved is True
                        and event.data.selected_action == "interactive"
                    ),
                )
            )

            response = await session.send_and_wait(
                PLAN_PROMPT,
                mode="plan",  # type: ignore[arg-type]
            )

            assert len(exit_plan_mode_requests) == 1
            request = exit_plan_mode_requests[0]
            assert request["summary"] == PLAN_SUMMARY
            assert request["actions"] == ["interactive", "autopilot", "exit_only"]
            assert request["recommendedAction"] == "interactive"
            assert request.get("planContent") is not None

            requested = await requested_event
            assert requested.data.summary == PLAN_SUMMARY

            completed = await completed_event
            assert completed.data.approved is True
            assert completed.data.selected_action == "interactive"
            assert completed.data.feedback == "Approved by the Python E2E test"
            assert response is not None
        finally:
            await session.disconnect()

    async def test_should_invoke_auto_mode_switch_handler_when_rate_limited(
        self, mode_ctx: E2ETestContext
    ):
        auto_mode_switch_requests = []

        async def on_auto_mode_switch(request, invocation):
            auto_mode_switch_requests.append(request)
            assert invocation["session_id"] == session.session_id
            return "yes"

        session = await mode_ctx.client.create_session(
            github_token=MODE_HANDLER_TOKEN,
            on_permission_request=PermissionHandler.approve_all,
            on_auto_mode_switch=on_auto_mode_switch,
        )

        try:
            requested_event = asyncio.create_task(
                _wait_for_event(
                    session,
                    lambda event: (
                        isinstance(event.data, AutoModeSwitchRequestedData)
                        and event.data.error_code == "user_weekly_rate_limited"
                        and event.data.retry_after_seconds == 1
                    ),
                )
            )
            completed_event = asyncio.create_task(
                _wait_for_event(
                    session,
                    lambda event: (
                        isinstance(event.data, AutoModeSwitchCompletedData)
                        and event.data.response == "yes"
                    ),
                )
            )
            model_change_event = asyncio.create_task(
                _wait_for_event(
                    session,
                    lambda event: (
                        isinstance(event.data, SessionModelChangeData)
                        and event.data.cause == "rate_limit_auto_switch"
                    ),
                )
            )
            idle_event = asyncio.create_task(
                _wait_for_event(
                    session,
                    lambda event: isinstance(event.data, SessionIdleData),
                )
            )

            message_id = await session.send(AUTO_MODE_PROMPT)
            assert message_id

            requested = await requested_event
            assert requested.data.error_code == "user_weekly_rate_limited"
            assert requested.data.retry_after_seconds == 1

            completed = await completed_event
            assert completed.data.response == "yes"

            model_change = await model_change_event
            assert model_change.data.cause == "rate_limit_auto_switch"
            idle = await idle_event
            assert isinstance(idle.data, SessionIdleData)

            assert len(auto_mode_switch_requests) == 1
            request = auto_mode_switch_requests[0]
            assert request["errorCode"] == "user_weekly_rate_limited"
            assert request["retryAfterSeconds"] == 1
        finally:
            await session.disconnect()
