"""Scenario-parity E2Es for session setup ordering."""

from __future__ import annotations

import asyncio

import pytest

from copilot.session import PermissionHandler
from copilot.session_events import SessionStartData

from ._scenario_fake_cli import create_scenario_client, read_scenario_capture
from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestScenarioSessionSetup:
    async def test_should_route_first_subscribed_event_for_preallocated_session_id(
        self,
        ctx: E2ETestContext,
    ):
        client, _capture_path = create_scenario_client(ctx, "preallocated-event")
        events = []
        event_received = asyncio.Event()

        def on_event(event) -> None:
            events.append(event)
            event_received.set()

        try:
            session = await client.create_session(
                session_id="scenario-session",
                on_event=on_event,
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                await asyncio.wait_for(event_received.wait(), timeout=5)
                assert session.session_id == "scenario-session"
                assert len(events) == 1
                assert isinstance(events[0].data, SessionStartData)
                assert events[0].data.session_id == "scenario-session"
                assert events[0].data.producer == "scenario-fake-cli"
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_should_create_then_reload_mcp_in_order(
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
                await session.rpc.mcp.reload()
                assert [
                    request["method"] for request in read_scenario_capture(capture_path)["requests"]
                ] == [
                    "connect",
                    "session.create",
                    "session.mcp.reload",
                ]
            finally:
                await session.disconnect()
        finally:
            await client.stop()
