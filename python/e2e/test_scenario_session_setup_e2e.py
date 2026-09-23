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
    async def test_should_route_subscribed_session_start_for_preallocated_session_id(
        self,
        ctx: E2ETestContext,
    ):
        client, _capture_path = create_scenario_client(ctx, "preallocated-event")
        start_events = []
        session_start_received = asyncio.Event()

        def on_event(event) -> None:
            if isinstance(event.data, SessionStartData):
                start_events.append(event)
                session_start_received.set()

        try:
            session = await client.create_session(
                session_id="scenario-session",
                on_event=on_event,
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                await asyncio.wait_for(session_start_received.wait(), timeout=5)
                assert session.session_id == "scenario-session"
                assert len(start_events) == 1
                assert start_events[0].data.session_id == "scenario-session"
                assert start_events[0].data.producer == "scenario-fake-cli"
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
