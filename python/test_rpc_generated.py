"""Tests for generated RPC method behavior."""

import json
from unittest.mock import AsyncMock

import pytest

from copilot.rpc import (
    BuiltinToolInputSchemaType,
    CatalogAISkillCandidate,
    CatalogCandidateSourceEmbedded,
    CatalogCandidateSourceURL,
    CatalogClientContract,
    CatalogMCPServerCandidate,
    CatalogSearchRequest,
    CatalogSearchSucceeded,
    CatalogUnsupportedKindError,
    CommandsApi,
    CommandsInvokeRequest,
    CommandsRespondToQueuedCommandRequest,
    LocalSessionMetadataValue,
    QueuedCommandHandled,
    QueuedCommandNotHandled,
    RemoteControlStatusOff,
    RemoteControlStatusResult,
    RemoteSessionMetadataValue,
    ServerCatalogApi,
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


@pytest.mark.asyncio
async def test_catalog_search_preserves_typed_candidates_handles_and_refusals():
    client = AsyncMock()
    client.request = AsyncMock(
        return_value={
            "kind": "succeeded",
            "searchId": "search-1",
            "candidates": [
                {
                    "kind": "mcp-server",
                    "handle": "mcp-handle",
                    "handleExpiresAt": "2026-09-02T12:00:00Z",
                    "mediaType": "application/mcp-server-card+json",
                    "installability": "installable",
                    "displayName": "Example MCP",
                    "source": {
                        "kind": "url",
                        "url": "https://example.com/mcp.json",
                    },
                    "provenance": {
                        "authority": "example.com",
                        "observedAt": "2026-09-02T11:00:00Z",
                        "mediaType": "application/mcp-server-card+json",
                    },
                },
                {
                    "kind": "ai-skill",
                    "handle": "skill-handle",
                    "handleExpiresAt": "2026-09-02T12:00:00Z",
                    "mediaType": "application/ai-skill",
                    "installability": "not-installable-kind",
                    "displayName": "Example skill",
                    "source": {"kind": "embedded"},
                    "provenance": {
                        "authority": "example.com",
                        "observedAt": "2026-09-02T11:00:00Z",
                        "mediaType": "application/ai-skill",
                    },
                },
            ],
            "truncated": False,
            "negotiated": {
                "runtimeProtocolVersion": 1,
                "grantedCapabilities": [
                    "mcp-server-card",
                    "ai-skill-discovery",
                ],
            },
        }
    )
    api = ServerCatalogApi(client)
    request = CatalogSearchRequest(
        contract=CatalogClientContract(
            protocol_version=1,
            required_capabilities=["mcp-server-card"],
        ),
        query="example",
    )

    result = await api.search(request)

    assert isinstance(result, CatalogSearchSucceeded)
    mcp_candidate, skill_candidate = result.candidates
    assert isinstance(mcp_candidate, CatalogMCPServerCandidate)
    assert mcp_candidate.handle == "mcp-handle"
    assert isinstance(mcp_candidate.source, CatalogCandidateSourceURL)
    assert isinstance(skill_candidate, CatalogAISkillCandidate)
    assert skill_candidate.handle == "skill-handle"
    assert isinstance(skill_candidate.source, CatalogCandidateSourceEmbedded)
    assert skill_candidate.source.to_dict() == {"kind": "embedded"}

    client.request.return_value = {
        "kind": "unsupported-kind",
        "message": "AI skills are unavailable",
        "requestedKinds": ["ai-skill"],
        "supportedKinds": ["mcp-server"],
    }
    refusal = await api.search(request)
    assert isinstance(refusal, CatalogUnsupportedKindError)


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
