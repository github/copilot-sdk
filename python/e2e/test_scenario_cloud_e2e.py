"""Scenario-parity E2Es for cloud connection and remote-steering workflows."""

from __future__ import annotations

import asyncio

import pytest

from copilot import CloudSessionOptions, CloudSessionRepository
from copilot.rpc import ConnectRemoteSessionParams, RemoteNotifySteerableChangedRequest
from copilot.session import PermissionHandler
from copilot.session_events import (
    AssistantMessageData,
    SessionRemoteSteerableChangedData,
    SessionStartData,
)

from ._scenario_fake_cli import create_scenario_client, read_scenario_capture
from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestScenarioCloud:
    async def test_should_notify_steerability_before_first_send_without_remote_enable(
        self,
        ctx: E2ETestContext,
    ):
        client, capture_path = create_scenario_client(ctx, "send")
        events = []
        steerability_received = asyncio.Event()

        def on_event(event) -> None:
            events.append(event)
            if isinstance(event.data, SessionRemoteSteerableChangedData):
                steerability_received.set()

        try:
            session = await client.create_session(
                session_id="scenario-session",
                on_event=on_event,
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                await session.rpc.remote.notify_steerable_changed(
                    RemoteNotifySteerableChangedRequest(remote_steerable=True)
                )
                response = await session.send_and_wait("Send the first cloud message.")
                await asyncio.wait_for(steerability_received.wait(), timeout=5)

                assert response is not None
                assert isinstance(response.data, AssistantMessageData)
                assert response.data.content == "scenario response"
                remote_event = next(
                    event
                    for event in events
                    if isinstance(event.data, SessionRemoteSteerableChangedData)
                )
                assert remote_event.data.remote_steerable is True

                methods = [
                    request["method"] for request in read_scenario_capture(capture_path)["requests"]
                ]
                assert methods.index("session.remote.notifySteerableChanged") < methods.index(
                    "session.send"
                )
                assert "session.remote.enable" not in methods
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_should_route_cloud_session_start_for_server_assigned_session_id(
        self,
        ctx: E2ETestContext,
    ):
        client, _capture_path = create_scenario_client(ctx, "cloud")
        start_events = []
        session_start_received = asyncio.Event()

        def on_event(event) -> None:
            if isinstance(event.data, SessionStartData):
                start_events.append(event)
                session_start_received.set()

        try:
            session = await client.create_session(
                cloud=CloudSessionOptions(
                    repository=CloudSessionRepository(
                        owner="github",
                        name="copilot-sdk",
                        branch="scenario-branch",
                    )
                ),
                on_event=on_event,
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                await asyncio.wait_for(session_start_received.wait(), timeout=5)
                assert session.session_id == "cloud-runtime-session"
                assert len(start_events) == 1
                assert start_events[0].data.session_id == session.session_id
                assert start_events[0].data.producer == "scenario-fake-cli"
                assert start_events[0].data.remote_steerable is False
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_should_resume_using_runtime_id_returned_by_cloud_connect(
        self,
        ctx: E2ETestContext,
    ):
        client, capture_path = create_scenario_client(ctx, "cloud-connect")
        try:
            await client.start()
            connection = await client.rpc.sessions.connect(
                ConnectRemoteSessionParams(session_id="remote-resource-id")
            )
            session = await client.resume_session(
                connection.session_id,
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                assert connection.session_id == "runtime-session-id"
                assert session.session_id == "runtime-session-id"
                requests = read_scenario_capture(capture_path)["requests"]
                resume = next(
                    request for request in requests if request["method"] == "session.resume"
                )
                assert resume["params"]["sessionId"] == "runtime-session-id"
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_should_expose_cloud_resource_mismatch_before_resume(
        self,
        ctx: E2ETestContext,
    ):
        client, _capture_path = create_scenario_client(ctx, "cloud-connect")
        try:
            await client.start()
            connection = await client.rpc.sessions.connect(
                ConnectRemoteSessionParams(session_id="remote-resource-id")
            )

            assert connection.session_id == "runtime-session-id"
            assert connection.metadata.session_id == "remote-resource-id"
            assert connection.metadata.resource_id == "remote-resource-id"
            assert connection.metadata.session_id != connection.session_id
            assert connection.metadata.repository.owner == "github"
            assert connection.metadata.repository.name == "copilot-sdk"
            assert connection.metadata.repository.branch == "scenario-branch"
            assert connection.metadata.pull_request_number == 42
            assert connection.metadata.state == "running"
            assert connection.metadata.summary == "Remote task summary"
        finally:
            await client.stop()
