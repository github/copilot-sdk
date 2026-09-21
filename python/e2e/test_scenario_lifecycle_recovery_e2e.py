"""Scenario-parity E2Es for recoverable session setup failures."""

from __future__ import annotations

import pytest

from copilot.session import PermissionHandler

from ._scenario_fake_cli import create_scenario_client, read_scenario_capture
from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestScenarioLifecycleRecovery:
    async def test_should_allow_retry_after_preacceptance_session_not_found(
        self,
        ctx: E2ETestContext,
    ):
        client, capture_path = create_scenario_client(ctx, "resume-retry")
        try:
            with pytest.raises(Exception) as exc_info:
                await client.resume_session(
                    "scenario-session",
                    on_permission_request=PermissionHandler.approve_all,
                )

            assert getattr(exc_info.value, "code", None) == -32001
            assert "Session not found before acceptance" in str(exc_info.value)
            assert getattr(exc_info.value, "data", None) == {"recoverable": True}

            session = await client.resume_session(
                "scenario-session",
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                assert session.session_id == "scenario-session"
                assert await session.send("Retry succeeded.") == "user-message"

                requests = read_scenario_capture(capture_path)["requests"]
                resume_requests = [
                    request for request in requests if request["method"] == "session.resume"
                ]
                assert len(resume_requests) == 2
                assert all(
                    request["params"]["sessionId"] == "scenario-session"
                    for request in resume_requests
                )
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_should_retry_resume_on_replacement_client_after_recoverable_failure(
        self,
        ctx: E2ETestContext,
    ):
        failed_client, _failed_capture = create_scenario_client(ctx, "resume-fail")
        with pytest.raises(Exception) as exc_info:
            await failed_client.resume_session(
                "scenario-session",
                on_permission_request=PermissionHandler.approve_all,
            )
        assert getattr(exc_info.value, "data", None) == {"recoverable": True}
        await failed_client.stop()

        replacement_client, replacement_capture = create_scenario_client(ctx, "send")
        try:
            session = await replacement_client.resume_session(
                "scenario-session",
                on_permission_request=PermissionHandler.approve_all,
            )
            try:
                assert await session.send("Replacement client recovered.") == "user-message"
                requests = read_scenario_capture(replacement_capture)["requests"]
                assert [request["method"] for request in requests] == [
                    "connect",
                    "session.resume",
                    "session.send",
                ]
            finally:
                await session.disconnect()
        finally:
            await replacement_client.stop()
