"""
Cloud Session Unit Tests

Tests for the cloud session SDK API: Mission Control task creation,
event polling, steering, and error handling.
"""

from __future__ import annotations

import asyncio
import json
from datetime import UTC
from typing import Any
from unittest.mock import patch

import pytest

from copilot import (
    CloudSession,
    CloudSessionError,
    CloudSessionEvent,
    CopilotClient,
    MissionControlCommandType,
    MissionControlTask,
    MissionControlTaskSession,
    SubprocessConfig,
)
from copilot.cloud.mission_control_client import MissionControlClient

# ---------------------------------------------------------------------------
# Fixtures / helpers
# ---------------------------------------------------------------------------

TASK = MissionControlTask(
    id="task-1",
    name="Cloud task",
    state="running",
    status="ready",
    creator_id=1,
    owner_id=2,
    session_count=1,
    created_at="2026-05-11T10:00:00.000Z",
    updated_at="2026-05-11T10:01:00.000Z",
    repo_id=3,
    sessions=[
        MissionControlTaskSession(
            id="mc-session-1",
            task_id="task-1",
            state="running",
            created_at="2026-05-11T10:00:30.000Z",
            updated_at="2026-05-11T10:00:30.000Z",
            owner_id=2,
            repo_id=3,
        )
    ],
)

REQUESTED_EVENT = CloudSessionEvent(
    id="event-1",
    timestamp="2026-05-11T10:00:00.000Z",
    type="session.requested",
    parent_id=None,
)

IDLE_EVENT = CloudSessionEvent(
    id="event-2",
    timestamp="2026-05-11T10:00:01.000Z",
    type="session.idle",
    parent_id="event-1",
    data={},
)


def _task_dict() -> dict[str, Any]:
    """Return a JSON-serialisable dict for the test task."""
    return {
        "id": TASK.id,
        "name": TASK.name,
        "state": TASK.state,
        "status": TASK.status,
        "creator_id": TASK.creator_id,
        "owner_id": TASK.owner_id,
        "session_count": TASK.session_count,
        "created_at": TASK.created_at,
        "updated_at": TASK.updated_at,
        "repo_id": TASK.repo_id,
        "sessions": [
            {
                "id": s.id,
                "task_id": s.task_id,
                "state": s.state,
                "created_at": s.created_at,
                "updated_at": s.updated_at,
                "owner_id": s.owner_id,
                "repo_id": s.repo_id,
            }
            for s in TASK.sessions
        ],
    }


def _event_dict(event: CloudSessionEvent) -> dict[str, Any]:
    """Return the wire-format dict for an event."""
    d: dict[str, Any] = {
        "id": event.id,
        "timestamp": event.timestamp,
        "type": event.type,
        "parentId": event.parent_id,
    }
    if event.data is not None:
        d["data"] = event.data
    if event.ephemeral is not None:
        d["ephemeral"] = event.ephemeral
    return d


class _FakeHTTPResponse:
    """Simulates urllib responses for mocking."""

    def __init__(self, body: str, status: int = 200) -> None:
        self._body = body.encode("utf-8")
        self.status = status
        self.code = status

    def read(self) -> bytes:
        return self._body

    def __enter__(self) -> _FakeHTTPResponse:
        return self

    def __exit__(self, *args: Any) -> None:
        pass


def _make_url_responses(responses: list[tuple[str, int]]) -> Any:
    """Create a side_effect for urllib.request.urlopen that returns successive responses."""
    import urllib.error

    call_idx = 0

    def _side_effect(req: Any) -> _FakeHTTPResponse:
        nonlocal call_idx
        if call_idx >= len(responses):
            raise RuntimeError("Unexpected HTTP request")
        body, status = responses[call_idx]
        call_idx += 1
        if status >= 400:
            error = urllib.error.HTTPError(
                req.full_url if hasattr(req, "full_url") else str(req),
                status,
                "Error",
                {},  # type: ignore[arg-type]
                None,
            )
            # Patch the read method to return the body
            error.read = lambda: body.encode("utf-8")  # type: ignore[assignment]
            raise error
        return _FakeHTTPResponse(body, status)

    return _side_effect


def _make_url_responses_tracking(
    responses: list[tuple[str, int]],
) -> tuple[Any, list[Any]]:
    """Like _make_url_responses but also returns a list of captured requests."""
    import urllib.error

    captured: list[Any] = []
    call_idx = 0

    def _side_effect(req: Any) -> _FakeHTTPResponse:
        nonlocal call_idx
        captured.append(req)
        if call_idx >= len(responses):
            raise RuntimeError("Unexpected HTTP request")
        body, status = responses[call_idx]
        call_idx += 1
        if status >= 400:
            error = urllib.error.HTTPError(
                req.full_url if hasattr(req, "full_url") else str(req),
                status,
                "Error",
                {},  # type: ignore[arg-type]
                None,
            )
            error.read = lambda: body.encode("utf-8")  # type: ignore[assignment]
            raise error
        return _FakeHTTPResponse(body, status)

    return _side_effect, captured


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------


class TestCloudSessions:
    @pytest.mark.asyncio
    async def test_creates_mission_control_cloud_task_and_attaches(self) -> None:
        """Creates a Mission Control cloud task and attaches to task events."""
        responses = [
            (json.dumps(_task_dict()), 200),
            (json.dumps({"events": [_event_dict(REQUESTED_EVENT)]}), 200),
        ]
        side_effect, captured = _make_url_responses_tracking(responses)
        progress: list[str] = []

        client = CopilotClient(
            SubprocessConfig(
                cli_path="/dev/null",
                github_token="token-1",
                env={
                    "COPILOT_MC_BASE_URL": "https://mc.test/agents",
                    "COPILOT_MC_FRONTEND_URL": "https://github.test",
                },
            ),
            auto_start=False,
        )

        with patch("urllib.request.urlopen", side_effect=side_effect):
            session = await client.create_cloud_session(
                {
                    "repository": {"owner": "github", "name": "copilot-sdk", "branch": "main"},
                    "initial_event_timeout_ms": 0,
                    "on_progress": lambda event: progress.append(event.phase),
                }
            )

        assert session.metadata.task_id == "task-1"
        assert session.metadata.mission_control_session_id == "mc-session-1"
        assert session.metadata.frontend_url == "https://github.test/copilot/tasks/task-1"
        assert session.metadata.repository == {
            "owner": "github",
            "name": "copilot-sdk",
            "branch": "main",
        }
        assert session.metadata.state == "running"
        assert session.metadata.status == "ready"

        messages = session.get_messages()
        assert len(messages) == 1
        assert messages[0].id == "event-1"
        assert messages[0].type == "session.requested"

        assert progress == [
            "creating_task",
            "provisioning_sandbox",
            "waiting_for_session",
            "connected",
        ]

        # Verify the create-task request
        create_req = captured[0]
        assert create_req.full_url == "https://mc.test/agents/tasks"
        assert create_req.method == "POST"
        assert create_req.get_header("Authorization") == "Bearer token-1"
        assert create_req.get_header("X-copilot-agent-slug") == "copilot-developer-sandbox"
        assert json.loads(create_req.data) == {
            "repositories": [{"owner": "github", "name": "copilot-sdk"}],
        }

        # Verify the events request
        events_req = captured[1]
        assert events_req.full_url == "https://mc.test/agents/tasks/task-1/events"
        assert events_req.method == "GET"

        await session.disconnect()

    @pytest.mark.asyncio
    async def test_creates_repo_less_cloud_task(self) -> None:
        """Creates a repo-less cloud task when owner is provided."""
        responses = [
            (json.dumps(_task_dict()), 200),
            (json.dumps({"events": []}), 200),
        ]
        side_effect, captured = _make_url_responses_tracking(responses)

        client = CopilotClient(
            SubprocessConfig(
                cli_path="/dev/null", env={"COPILOT_MC_BASE_URL": "https://mc.test/agents"}
            ),
            auto_start=False,
        )

        with patch("urllib.request.urlopen", side_effect=side_effect):
            session = await client.create_cloud_session(
                {"owner": "github", "initial_event_timeout_ms": 0}
            )

        assert session.metadata.owner == "github"

        # Verify the create-task request body
        create_req = captured[0]
        assert json.loads(create_req.data) == {"owner": "github"}

        await session.disconnect()

    @pytest.mark.asyncio
    async def test_requires_owner_when_no_repository(self) -> None:
        """Requires an owner when creating a repo-less cloud task."""
        client = CopilotClient(
            SubprocessConfig(
                cli_path="/dev/null", env={"COPILOT_MC_BASE_URL": "https://mc.test/agents"}
            ),
            auto_start=False,
        )

        with pytest.raises(ValueError, match="owner is required when repository is omitted"):
            await client.create_cloud_session({"initial_event_timeout_ms": 0})

    @pytest.mark.asyncio
    async def test_sends_user_messages_through_steer_api(self) -> None:
        """Sends cloud session user messages through the Mission Control steer API."""
        responses = [
            ("", 404),  # get_task returns 404
            (json.dumps({"events": []}), 200),  # list_task_events
            ("", 200),  # steer
        ]
        side_effect, captured = _make_url_responses_tracking(responses)

        client = CopilotClient(
            SubprocessConfig(
                cli_path="/dev/null", env={"COPILOT_MC_BASE_URL": "https://mc.test/agents"}
            ),
            auto_start=False,
        )

        with patch("urllib.request.urlopen", side_effect=side_effect):
            session = await client.connect_cloud_session("task-1", {"initial_event_timeout_ms": 0})
            await session.send(prompt="hello cloud")

        steer_req = captured[2]
        assert steer_req.full_url == "https://mc.test/agents/tasks/task-1/steer"
        assert steer_req.method == "POST"
        assert json.loads(steer_req.data) == {
            "type": "user_message",
            "content": "hello cloud",
        }

        await session.disconnect()

    @pytest.mark.asyncio
    async def test_sorts_and_deduplicates_events(self) -> None:
        """Sorts replayed events and deduplicates events observed during polling."""
        polled_event = CloudSessionEvent(
            id="event-3",
            timestamp="2026-05-11T10:00:02.000Z",
            type="session.idle",
            parent_id="event-2",
            data={},
        )
        # Initial connect: get_task returns task, list_task_events returns events out of order
        responses_connect = [
            (json.dumps(_task_dict()), 200),
            (
                json.dumps({"events": [_event_dict(IDLE_EVENT), _event_dict(REQUESTED_EVENT)]}),
                200,
            ),
        ]

        side_effect_connect, _ = _make_url_responses_tracking(responses_connect)

        client = CopilotClient(
            SubprocessConfig(
                cli_path="/dev/null", env={"COPILOT_MC_BASE_URL": "https://mc.test/agents"}
            ),
            auto_start=False,
        )

        with patch("urllib.request.urlopen", side_effect=side_effect_connect):
            session = await client.connect_cloud_session(
                "task-1",
                {"initial_event_timeout_ms": 0, "poll_interval_ms": 50},
            )

        # Events should be sorted chronologically
        message_ids = [e.id for e in session.get_messages()]
        assert message_ids == ["event-1", "event-2"]

        # Now set up poll responses (includes previously seen events + new one)
        poll_responses = [
            (
                json.dumps(
                    {
                        "events": [
                            _event_dict(IDLE_EVENT),
                            _event_dict(REQUESTED_EVENT),
                            _event_dict(polled_event),
                        ]
                    }
                ),
                200,
            ),
        ]
        poll_side_effect, _ = _make_url_responses_tracking(poll_responses)

        seen: list[str] = []
        session.on(lambda event: seen.append(event.id))

        with patch("urllib.request.urlopen", side_effect=poll_side_effect):
            # Wait for the poller to run
            await asyncio.sleep(0.15)

        # Only the new event should have been dispatched to the handler
        assert seen == ["event-3"]
        # All events should be in order
        assert [e.id for e in session.get_messages()] == ["event-1", "event-2", "event-3"]

        await session.disconnect()

    @pytest.mark.asyncio
    async def test_surfaces_error_responses_as_cloud_session_errors(self) -> None:
        """Surfaces Mission Control error responses as typed CloudSessionError."""
        responses = [
            (json.dumps({"message": "blocked"}), 403),
        ]
        side_effect, _ = _make_url_responses_tracking(responses)

        client = CopilotClient(
            SubprocessConfig(
                cli_path="/dev/null", env={"COPILOT_MC_BASE_URL": "https://mc.test/agents"}
            ),
            auto_start=False,
        )

        with patch("urllib.request.urlopen", side_effect=side_effect):
            with pytest.raises(CloudSessionError) as exc_info:
                await client.create_cloud_session(
                    {
                        "repository": {"owner": "github", "name": "copilot-sdk"},
                        "initial_event_timeout_ms": 0,
                    }
                )

        err = exc_info.value
        assert str(err) == "blocked"
        assert err.reason == "policy_blocked"
        assert err.status == 403

    @pytest.mark.asyncio
    async def test_connect_cloud_session_with_existing_task(self) -> None:
        """connectCloudSession populates metadata from existing task."""
        responses = [
            (json.dumps(_task_dict()), 200),  # get_task
            (json.dumps({"events": [_event_dict(REQUESTED_EVENT)]}), 200),
        ]
        side_effect, _ = _make_url_responses_tracking(responses)

        client = CopilotClient(
            SubprocessConfig(
                cli_path="/dev/null",
                env={
                    "COPILOT_MC_BASE_URL": "https://mc.test/agents",
                    "COPILOT_MC_FRONTEND_URL": "https://github.test",
                },
            ),
            auto_start=False,
        )

        with patch("urllib.request.urlopen", side_effect=side_effect):
            session = await client.connect_cloud_session("task-1", {"initial_event_timeout_ms": 0})

        assert session.metadata.task_id == "task-1"
        assert session.metadata.mission_control_session_id == "mc-session-1"
        assert session.metadata.frontend_url == "https://github.test/copilot/tasks/task-1"

        await session.disconnect()

    @pytest.mark.asyncio
    async def test_connect_cloud_session_with_missing_task(self) -> None:
        """connectCloudSession uses fallback metadata when task not found."""
        responses = [
            ("", 404),  # get_task returns 404
            (json.dumps({"events": []}), 200),
        ]
        side_effect, _ = _make_url_responses_tracking(responses)

        client = CopilotClient(
            SubprocessConfig(
                cli_path="/dev/null",
                env={
                    "COPILOT_MC_BASE_URL": "https://mc.test/agents",
                    "COPILOT_MC_FRONTEND_URL": "https://github.test",
                },
            ),
            auto_start=False,
        )

        with patch("urllib.request.urlopen", side_effect=side_effect):
            session = await client.connect_cloud_session(
                "task-missing", {"initial_event_timeout_ms": 0}
            )

        assert session.metadata.task_id == "task-missing"
        assert session.metadata.mission_control_session_id is None
        assert session.metadata.frontend_url == "https://github.test/copilot/tasks/task-missing"

        await session.disconnect()

    @pytest.mark.asyncio
    async def test_disconnect_prevents_further_sends(self) -> None:
        """Disconnected sessions reject send calls."""
        responses = [
            ("", 404),
            (json.dumps({"events": []}), 200),
        ]
        side_effect, _ = _make_url_responses_tracking(responses)

        client = CopilotClient(
            SubprocessConfig(
                cli_path="/dev/null", env={"COPILOT_MC_BASE_URL": "https://mc.test/agents"}
            ),
            auto_start=False,
        )

        with patch("urllib.request.urlopen", side_effect=side_effect):
            session = await client.connect_cloud_session("task-1", {"initial_event_timeout_ms": 0})

        await session.disconnect()

        with pytest.raises(RuntimeError, match="disconnected"):
            await session.send(prompt="should fail")

    @pytest.mark.asyncio
    async def test_mission_control_command_types(self) -> None:
        """Verify MissionControlCommandType enum values."""
        assert MissionControlCommandType.USER_MESSAGE.value == "user_message"
        assert MissionControlCommandType.ASK_USER_RESPONSE.value == "ask_user_response"
        assert MissionControlCommandType.PLAN_APPROVAL_RESPONSE.value == "plan_approval_response"
        assert MissionControlCommandType.PERMISSION_RESPONSE.value == "permission_response"
        assert MissionControlCommandType.ELICITATION_RESPONSE.value == "elicitation_response"
        assert MissionControlCommandType.ABORT.value == "abort"
        assert MissionControlCommandType.MODE_SWITCH.value == "mode_switch"

    @pytest.mark.asyncio
    async def test_typed_event_handlers(self) -> None:
        """Typed event handlers only receive matching event types."""
        responses = [
            (json.dumps(_task_dict()), 200),
            (
                json.dumps({"events": [_event_dict(REQUESTED_EVENT), _event_dict(IDLE_EVENT)]}),
                200,
            ),
        ]
        side_effect, _ = _make_url_responses_tracking(responses)

        client = CopilotClient(
            SubprocessConfig(
                cli_path="/dev/null", env={"COPILOT_MC_BASE_URL": "https://mc.test/agents"}
            ),
            auto_start=False,
        )

        idle_events: list[CloudSessionEvent] = []

        with patch("urllib.request.urlopen", side_effect=side_effect):
            session = await client.connect_cloud_session(
                "task-1", {"initial_event_timeout_ms": 0, "poll_interval_ms": 50}
            )

        # Register typed handler — events from connect() are already recorded
        session.on("session.idle", lambda e: idle_events.append(e))

        # Send events again via polling to trigger typed handler
        poll_new_idle = CloudSessionEvent(
            id="event-4",
            timestamp="2026-05-11T10:00:04.000Z",
            type="session.idle",
            data={},
        )
        poll_responses = [
            (
                json.dumps({"events": [_event_dict(poll_new_idle)]}),
                200,
            ),
        ]
        poll_side_effect, _ = _make_url_responses_tracking(poll_responses)

        with patch("urllib.request.urlopen", side_effect=poll_side_effect):
            await asyncio.sleep(0.15)

        assert len(idle_events) == 1
        assert idle_events[0].id == "event-4"

        await session.disconnect()

    @pytest.mark.asyncio
    async def test_event_handler_unsubscribe(self) -> None:
        """Unsubscribing removes the handler from future events."""
        mc_client = MissionControlClient(
            base_url="https://mc.test/agents",
            frontend_base_url="https://github.test",
        )

        from datetime import datetime

        from copilot.cloud.types import CloudSessionMetadata

        metadata = CloudSessionMetadata(
            task_id="task-1",
            frontend_url="https://github.test/copilot/tasks/task-1",
            created_at=datetime.now(UTC),
            updated_at=datetime.now(UTC),
        )

        session = CloudSession(
            client=mc_client,
            metadata=metadata,
            initial_event_timeout_ms=0,
        )

        # Manually connect with empty events
        with patch(
            "urllib.request.urlopen",
            side_effect=_make_url_responses([(json.dumps({"events": []}), 200)]),
        ):
            await session.connect()

        seen: list[str] = []
        unsubscribe = session.on(lambda e: seen.append(e.id))

        # Dispatch an event manually
        session._record_events(
            [CloudSessionEvent(id="e1", timestamp="2026-01-01T00:00:00Z", type="test")]
        )
        assert seen == ["e1"]

        # Unsubscribe and dispatch another
        unsubscribe()
        session._record_events(
            [CloudSessionEvent(id="e2", timestamp="2026-01-01T00:00:01Z", type="test")]
        )
        assert seen == ["e1"]  # Still only e1

        await session.disconnect()

    @pytest.mark.asyncio
    async def test_context_manager(self) -> None:
        """CloudSession works as an async context manager."""
        mc_client = MissionControlClient(
            base_url="https://mc.test/agents",
            frontend_base_url="https://github.test",
        )

        from datetime import datetime

        from copilot.cloud.types import CloudSessionMetadata

        metadata = CloudSessionMetadata(
            task_id="task-1",
            frontend_url="https://github.test/copilot/tasks/task-1",
            created_at=datetime.now(UTC),
            updated_at=datetime.now(UTC),
        )

        with patch(
            "urllib.request.urlopen",
            side_effect=_make_url_responses([(json.dumps({"events": []}), 200)]),
        ):
            async with CloudSession(
                client=mc_client,
                metadata=metadata,
                initial_event_timeout_ms=0,
            ) as session:
                await session.connect()
                assert not session._is_disconnected

        assert session._is_disconnected
