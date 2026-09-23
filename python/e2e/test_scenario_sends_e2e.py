"""Scenario-parity E2Es for send serialization and cancellation boundaries."""

from __future__ import annotations

import asyncio
from typing import Literal

import pytest

from copilot.session import AgentMessageSource, PermissionHandler

from ._scenario_fake_cli import create_scenario_client, read_scenario_capture
from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _send_requests(capture_path) -> list[dict]:
    return [
        request
        for request in read_scenario_capture(capture_path)["requests"]
        if request["method"] == "session.send"
    ]


class TestScenarioSends:
    async def test_should_send_complete_message_wire_shape(
        self,
        ctx: E2ETestContext,
    ):
        client, capture_path = create_scenario_client(ctx, "send")
        try:
            session = await client.create_session(
                session_id="scenario-session",
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                message_id = await session.send(
                    "Run the extension workflow.",
                    attachments=[
                        {
                            "type": "selection",
                            "filePath": "src/example.py",
                            "displayName": "example.py:4-6",
                            "selection": {
                                "start": {"line": 4, "character": 2},
                                "end": {"line": 6, "character": 8},
                            },
                            "text": "selected text",
                        },
                        {
                            "type": "extension_context",
                            "capturedAt": "2026-01-02T03:04:05.000Z",
                            "extensionId": "scenario-extension",
                            "title": "Scenario context",
                            "canvasId": "scenario-canvas",
                            "instanceId": "scenario-instance",
                            "payload": {
                                "metadata": {
                                    "source": "scenario",
                                    "priority": 7,
                                }
                            },
                        },
                    ],
                    source=AgentMessageSource("extension-agent"),
                    mode="immediate",
                    agent_mode="plan",
                    request_headers={"X-Scenario": "complete-wire-shape"},
                    display_prompt="Visible extension prompt",
                )

                assert message_id == "user-message"
                assert _send_requests(capture_path) == [
                    {
                        "method": "session.send",
                        "params": {
                            "sessionId": "scenario-session",
                            "prompt": "Run the extension workflow.",
                            "attachments": [
                                {
                                    "type": "selection",
                                    "filePath": "src/example.py",
                                    "displayName": "example.py:4-6",
                                    "selection": {
                                        "start": {"line": 4, "character": 2},
                                        "end": {"line": 6, "character": 8},
                                    },
                                    "text": "selected text",
                                },
                                {
                                    "type": "extension_context",
                                    "capturedAt": "2026-01-02T03:04:05.000Z",
                                    "extensionId": "scenario-extension",
                                    "title": "Scenario context",
                                    "canvasId": "scenario-canvas",
                                    "instanceId": "scenario-instance",
                                    "payload": {
                                        "metadata": {
                                            "source": "scenario",
                                            "priority": 7,
                                        }
                                    },
                                },
                            ],
                            "source": "agent-extension-agent",
                            "mode": "immediate",
                            "agentMode": "plan",
                            "requestHeaders": {
                                "X-Scenario": "complete-wire-shape",
                            },
                            "displayPrompt": "Visible extension prompt",
                        },
                    }
                ]
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    @pytest.mark.parametrize("mode", [None, "enqueue", "immediate"])
    async def test_should_not_dispatch_pre_cancelled_send(
        self,
        ctx: E2ETestContext,
        mode: Literal["enqueue", "immediate"] | None,
    ):
        client, capture_path = create_scenario_client(ctx, "send")
        try:
            session = await client.create_session(
                session_id="scenario-session",
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                send_task = asyncio.create_task(
                    session.send("This must not be dispatched.", mode=mode)
                )
                assert send_task.cancel()

                with pytest.raises(asyncio.CancelledError):
                    await send_task

                assert _send_requests(capture_path) == []
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    @pytest.mark.parametrize("mode", [None, "enqueue", "immediate"])
    async def test_should_not_replay_send_after_ambiguous_transport_loss(
        self,
        ctx: E2ETestContext,
        mode: Literal["enqueue", "immediate"] | None,
    ):
        client, capture_path = create_scenario_client(ctx, "send-fail")
        session = await client.create_session(
            session_id="scenario-session",
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            with pytest.raises(Exception) as exc_info:
                await session.send("Lose the transport after accepting this.", mode=mode)

            assert type(exc_info.value).__name__ == "ProcessExitedError"
            requests = _send_requests(capture_path)
            assert len(requests) == 1
            assert requests[0]["params"]["prompt"] == ("Lose the transport after accepting this.")
            if mode is None:
                assert "mode" not in requests[0]["params"]
            else:
                assert requests[0]["params"]["mode"] == mode

            with pytest.raises(Exception) as retry_exc_info:
                await session.send("Fail immediately after transport loss.")
            assert type(retry_exc_info.value).__name__ == "ProcessExitedError"
            assert len(_send_requests(capture_path)) == 1
        finally:
            await client.force_stop()
