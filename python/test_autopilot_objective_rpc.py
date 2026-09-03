"""Tests for the generated Autopilot objective RPC binding."""

from unittest.mock import AsyncMock, call

import pytest

from copilot.rpc import AutopilotObjectiveApi, AutopilotObjectiveStatus


@pytest.mark.asyncio
async def test_get_state_preserves_canonical_objective_state():
    payloads = [
        {"state": None},
        {
            "state": {
                "id": 1,
                "objective": "Ship the release",
                "status": "active",
                "turnCount": 2,
                "creditCountNanoAiu": "0",
            }
        },
        {
            "state": {
                "id": 2,
                "objective": "Wait for approval",
                "status": "paused",
                "turnCount": 3,
                "pauseReason": "Approval required",
                "creditCountNanoAiu": "9007199254740993",
                "creditLimit": {
                    "creditsUsed": 9.007199254740993,
                    "creditsUsedNanoAiu": "9007199254740993",
                },
            }
        },
        {
            "state": {
                "id": 3,
                "objective": "Publish the SDK",
                "status": "completed",
                "turnCount": 4,
                "completionSummary": "Published",
                "creditCountNanoAiu": "9007199254740994",
                "creditLimit": {
                    "credits": 2.5,
                    "creditsUsed": 1.25,
                    "creditsUsedNanoAiu": "1250000000",
                },
            }
        },
    ]
    client = AsyncMock()
    client.request = AsyncMock(side_effect=payloads)
    api = AutopilotObjectiveApi(client, "session-1")

    results = [await api.get_state() for _ in payloads]

    client.request.assert_has_awaits(
        [
            call("session.autopilotObjective.getState", {"sessionId": "session-1"})
            for _ in payloads
        ]
    )
    assert results[0].state is None

    active = results[1].state
    assert active is not None
    assert active.status is AutopilotObjectiveStatus.ACTIVE
    assert "pauseReason" not in active.to_dict()
    assert "completionSummary" not in active.to_dict()
    assert "creditLimit" not in active.to_dict()

    paused = results[2].state
    assert paused is not None
    assert paused.status is AutopilotObjectiveStatus.PAUSED
    assert paused.pause_reason == "Approval required"
    assert paused.credit_limit is not None
    assert paused.credit_limit.credits is None
    assert paused.credit_count_nano_aiu == "9007199254740993"

    completed = results[3].state
    assert completed is not None
    assert completed.status is AutopilotObjectiveStatus.COMPLETED
    assert completed.completion_summary == "Published"
    assert completed.credit_limit is not None
    assert completed.credit_limit.credits_used_nano_aiu == "1250000000"
