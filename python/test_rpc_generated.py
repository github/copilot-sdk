"""Tests for generated RPC method behavior."""

import json
from unittest.mock import AsyncMock, call

import pytest

from copilot.rpc import (
    AutopilotObjectiveApi,
    AutopilotObjectiveStatus,
    BuiltinToolInputSchemaType,
    CommandsApi,
    CommandsInvokeRequest,
    CommandsRespondToQueuedCommandRequest,
    LocalSessionMetadataValue,
    QueuedCommandHandled,
    QueuedCommandNotHandled,
    RemoteControlStatusOff,
    RemoteControlStatusResult,
    RemoteSessionMetadataValue,
    SessionList,
    SlashCommandTextResult,
    TaskAgentInfo,
    UIElicitationSchemaType,
)


@pytest.mark.asyncio
async def test_commands_invoke_deserializes_slash_command_result():
    client = AsyncMock()
    client.request = AsyncMock(return_value={"kind": "text", "text": "hello", "markdown": True})
    api = CommandsApi(client, "sess-1")

    result = await api.invoke(CommandsInvokeRequest(name="help"))

    assert isinstance(result, SlashCommandTextResult)
    assert result.text == "hello"
    assert result.markdown is True


def test_remote_control_status_deserializes_string_discriminated_union():
    result = RemoteControlStatusResult.from_dict({"status": {"state": "off"}})

    assert isinstance(result.status, RemoteControlStatusOff)
    assert result.status.state == "off"
    assert result.status.to_dict() == {"state": "off"}


def test_ui_elicitation_schema_type_preserves_public_alias():
    assert UIElicitationSchemaType is BuiltinToolInputSchemaType


def test_session_list_deserializes_boolean_discriminated_entries():
    payload = {
        "sessions": [
            {
                "sessionId": "example-local",
                "startTime": "2026-07-26T10:00:00.000Z",
                "modifiedTime": "2026-07-26T10:05:00.000Z",
                "isRemote": False,
            },
            {
                "sessionId": "example-remote",
                "startTime": "2026-07-26T11:00:00.000Z",
                "modifiedTime": "2026-07-26T11:05:00.000Z",
                "isRemote": True,
                "remoteSessionIds": ["example-remote"],
                "repository": {"owner": "github", "name": "copilot-sdk", "branch": "main"},
            },
        ]
    }

    result = SessionList.from_dict(payload)

    local, remote = result.sessions
    assert isinstance(local, LocalSessionMetadataValue)
    assert local.session_id == "example-local"
    assert local.is_remote is False
    assert isinstance(remote, RemoteSessionMetadataValue)
    assert remote.session_id == "example-remote"
    assert remote.is_remote is True
    assert remote.repository.owner == "github"


def test_task_agent_info_deserializes_integral_float_milliseconds():
    task = TaskAgentInfo.from_dict(
        {
            "agentType": "general-purpose",
            "description": "Example task",
            "id": "agent-1",
            "prompt": "Do the task",
            "startedAt": "2026-08-19T12:00:00Z",
            "status": "running",
            "toolCallId": "tool-1",
            "type": "agent",
            "activeTimeMs": 43.0,
        }
    )

    assert task.active_time_ms == 43
    assert task.to_dict()["activeTimeMs"] == 43


@pytest.mark.parametrize(
    ("handled", "expected_type"),
    [(True, QueuedCommandHandled), (False, QueuedCommandNotHandled)],
)
def test_queued_command_result_deserializes_boolean_discriminator(handled, expected_type):
    request = CommandsRespondToQueuedCommandRequest.from_dict(
        {"requestId": "example-request", "result": {"handled": handled}}
    )

    assert isinstance(request.result, expected_type)


@pytest.mark.parametrize(
    ("variant", "expected_handled", "expected_json"),
    [
        (QueuedCommandHandled(), True, '{"handled": true}'),
        (QueuedCommandNotHandled(), False, '{"handled": false}'),
    ],
)
def test_queued_command_result_serializes_boolean_discriminator(
    variant, expected_handled, expected_json
):
    encoded = variant.to_dict()

    assert encoded["handled"] is expected_handled
    assert json.dumps(encoded) == expected_json

    request = CommandsRespondToQueuedCommandRequest(request_id="example-request", result=variant)
    round_tripped = CommandsRespondToQueuedCommandRequest.from_dict(request.to_dict())

    assert request.to_dict()["result"]["handled"] is expected_handled
    assert isinstance(round_tripped.result, type(variant))


@pytest.mark.asyncio
async def test_autopilot_objective_get_state_preserves_canonical_state():
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
                    "creditsUsed": 9007199.254740993,
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
        [call("session.autopilotObjective.getState", {"sessionId": "session-1"}) for _ in payloads]
    )
    assert results[0].state is None

    active = results[1].state
    assert active is not None
    assert active.id == 1
    assert active.objective == "Ship the release"
    assert active.status is AutopilotObjectiveStatus.ACTIVE
    assert active.turn_count == 2
    assert active.credit_count_nano_aiu == "0"
    assert active.to_dict() == {
        "creditCountNanoAiu": "0",
        "id": 1,
        "objective": "Ship the release",
        "status": "active",
        "turnCount": 2,
    }

    paused = results[2].state
    assert paused is not None
    assert paused.id == 2
    assert paused.objective == "Wait for approval"
    assert paused.status is AutopilotObjectiveStatus.PAUSED
    assert paused.turn_count == 3
    assert paused.pause_reason == "Approval required"
    assert paused.credit_count_nano_aiu == "9007199254740993"
    assert paused.credit_limit is not None
    assert paused.credit_limit.credits is None
    assert paused.credit_limit.credits_used == 9007199.254740993
    assert paused.credit_limit.credits_used_nano_aiu == "9007199254740993"

    completed = results[3].state
    assert completed is not None
    assert completed.id == 3
    assert completed.objective == "Publish the SDK"
    assert completed.status is AutopilotObjectiveStatus.COMPLETED
    assert completed.turn_count == 4
    assert completed.completion_summary == "Published"
    assert completed.credit_count_nano_aiu == "9007199254740994"
    assert completed.credit_limit is not None
    assert completed.credit_limit.credits == 2.5
    assert completed.credit_limit.credits_used == 1.25
    assert completed.credit_limit.credits_used_nano_aiu == "1250000000"
