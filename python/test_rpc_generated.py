"""Tests for generated RPC method behavior."""

import json
from typing import Any
from unittest.mock import AsyncMock

import pytest

from copilot.rpc import (
    BuiltinToolInputSchemaType,
    CatalogAISkillCandidate,
    CatalogAuthenticationRequiredError,
    CatalogCandidateSourceEmbedded,
    CatalogCandidateSourceURL,
    CatalogClientContract,
    CatalogMCPServerCandidate,
    CatalogNetworkFailureError,
    CatalogSearchRequest,
    CatalogSearchSucceeded,
    CommandsApi,
    CommandsInvokeRequest,
    CommandsRespondToQueuedCommandRequest,
    LocalSessionMetadataValue,
    QueuedCommandHandled,
    QueuedCommandNotHandled,
    RemoteControlStatusOff,
    RemoteControlStatusResult,
    RemoteSessionMetadataValue,
    SandboxConfig,
    ServerCatalogApi,
    SessionList,
    SlashCommandTextResult,
    TaskAgentInfo,
    UIElicitationSchemaType,
)

OPAQUE_MCP_HANDLE = "opaque:mcp/01-do-not-parse"
OPAQUE_SKILL_HANDLE = "opaque:skill/02-do-not-parse"


def test_sandbox_config_round_trips_allow_bypass_and_omits_when_absent():
    configured = SandboxConfig(enabled=True, allow_bypass=True)

    assert configured.to_dict() == {"enabled": True, "allowBypass": True}
    assert SandboxConfig.from_dict(configured.to_dict()).allow_bypass is True
    assert SandboxConfig(enabled=True).to_dict() == {"enabled": True}


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
async def test_closed_union_preserves_typed_nested_variants_and_opaque_handles():
    client = AsyncMock()
    client.request = AsyncMock(
        return_value={
            "kind": "succeeded",
            "rawCard": {"secret": "must-not-survive"},
            "searchId": "search-01",
            "candidates": [
                {
                    "kind": "mcp-server",
                    "handle": OPAQUE_MCP_HANDLE,
                    "handleExpiresAt": "2026-09-02T12:00:00Z",
                    "mediaType": "application/mcp-server-card+json",
                    "installability": "installable",
                    "displayName": "Example MCP",
                    "rawCard": {"secret": "must-not-survive"},
                    "source": {
                        "kind": "url",
                        "url": "https://catalog.example/mcp.json",
                        "rawCard": {"secret": "must-not-survive"},
                    },
                    "provenance": {
                        "authority": "catalog.example",
                        "observedAt": "2026-09-02T11:00:00Z",
                        "mediaType": "application/mcp-server-card+json",
                    },
                },
                {
                    "kind": "ai-skill",
                    "handle": OPAQUE_SKILL_HANDLE,
                    "handleExpiresAt": "2026-09-02T12:00:00Z",
                    "mediaType": "application/ai-skill",
                    "installability": "not-installable-kind",
                    "displayName": "Example skill",
                    "rawCard": {"secret": "must-not-survive"},
                    "source": {
                        "kind": "embedded",
                        "rawCard": {"secret": "must-not-survive"},
                    },
                    "provenance": {
                        "authority": "catalog.example",
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

    result = await api.search(
        CatalogSearchRequest(
            contract=CatalogClientContract(
                protocol_version=1,
                required_capabilities=[],
            ),
            query="example",
        )
    )

    assert isinstance(result, CatalogSearchSucceeded)
    mcp, skill = result.candidates
    assert isinstance(mcp, CatalogMCPServerCandidate)
    assert isinstance(skill, CatalogAISkillCandidate)
    assert mcp.handle == OPAQUE_MCP_HANDLE
    assert skill.handle == OPAQUE_SKILL_HANDLE
    assert isinstance(mcp.source, CatalogCandidateSourceURL)
    assert isinstance(skill.source, CatalogCandidateSourceEmbedded)
    encoded = result.to_dict()
    assert "rawCard" not in encoded
    for candidate in encoded["candidates"]:
        assert {"card", "cardData", "rawCard"}.isdisjoint(candidate)
        assert "rawCard" not in candidate["source"]


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("payload", "expected_type"),
    [
        (
            {
                "kind": "authentication-required",
                "reason": "no-credential",
                "message": "Sign in is required.",
            },
            CatalogAuthenticationRequiredError,
        ),
        (
            {
                "kind": "network-failure",
                "reason": "timeout",
                "retryAfterSeconds": 30,
                "message": "The catalogue timed out.",
            },
            CatalogNetworkFailureError,
        ),
    ],
)
async def test_closed_union_preserves_refusals_and_failures(payload, expected_type):
    client = AsyncMock()
    client.request = AsyncMock(return_value=payload)
    api = ServerCatalogApi(client)

    result = await api.search(
        CatalogSearchRequest(
            contract=CatalogClientContract(
                protocol_version=1,
                required_capabilities=[],
            ),
            query="example",
        )
    )

    assert isinstance(result, expected_type)


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "case",
    [
        "unknown-result",
        "missing-result",
        "unknown-candidate",
        "missing-candidate",
        "unknown-source",
        "missing-source",
    ],
)
async def test_closed_union_rejects_unknown_and_missing_discriminators(case):
    candidate: dict[str, Any] = {
        "kind": "mcp-server",
        "handle": OPAQUE_MCP_HANDLE,
        "handleExpiresAt": "2026-09-02T12:00:00Z",
        "mediaType": "application/mcp-server-card+json",
        "installability": "installable",
        "displayName": "Example MCP",
        "source": {
            "kind": "url",
            "url": "https://catalog.example/mcp.json",
        },
        "provenance": {
            "authority": "catalog.example",
            "observedAt": "2026-09-02T11:00:00Z",
            "mediaType": "application/mcp-server-card+json",
        },
    }
    payload: dict[str, Any] = {
        "kind": "succeeded",
        "searchId": "search-invalid",
        "candidates": [candidate],
        "truncated": False,
        "negotiated": {
            "runtimeProtocolVersion": 1,
            "grantedCapabilities": [],
        },
    }
    if case == "unknown-result":
        payload = {"kind": "future-result", "rawCard": {"secret": "must-not-survive"}}
    elif case == "missing-result":
        payload = {"rawCard": {"secret": "must-not-survive"}}
    elif case == "unknown-candidate":
        candidate["kind"] = "future-candidate"
        candidate["rawCard"] = {"secret": "must-not-survive"}
    elif case == "missing-candidate":
        del candidate["kind"]
        candidate["rawCard"] = {"secret": "must-not-survive"}
    elif case == "unknown-source":
        candidate["source"] = {
            "kind": "future-source",
            "rawCard": {"secret": "must-not-survive"},
        }
    elif case == "missing-source":
        candidate["source"] = {"rawCard": {"secret": "must-not-survive"}}

    client = AsyncMock()
    client.request = AsyncMock(return_value=payload)
    api = ServerCatalogApi(client)

    with pytest.raises(ValueError, match="Unknown .* kind"):
        await api.search(
            CatalogSearchRequest(
                contract=CatalogClientContract(
                    protocol_version=1,
                    required_capabilities=[],
                ),
                query="example",
            )
        )
