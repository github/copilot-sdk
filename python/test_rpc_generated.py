"""Tests for generated RPC method behavior."""

import inspect
import json
from datetime import timedelta
from unittest.mock import AsyncMock, Mock

import pytest

from copilot._jsonrpc import JsonRpcClient
from copilot.generated.rpc import (
    ClientGlobalApiHandlers,
    CustomizationReloadOutcome,
    CustomizationReloadStatus,
    CustomizationReloadSubsystem,
    UsageGetMetricsResult,
    register_client_global_api_handlers,
)
from copilot.rpc import (
    AuthReadValue,
    BuiltinToolInputSchemaType,
    CommandsApi,
    CommandsInvokeRequest,
    CommandsRespondToQueuedCommandRequest,
    HostDisposeRequest,
    HostEmptyResult,
    LocalSessionMetadataValue,
    MCPServerConfigHTTP,
    QueuedCommandHandled,
    QueuedCommandNotHandled,
    QuotaWarningProjection,
    RemoteControlStatusOff,
    RemoteControlStatusResult,
    RemoteSessionMetadataValue,
    SandboxConfig,
    ServerRpc,
    SessionList,
    SessionModelList,
    SessionRpc,
    SlashCommandTextResult,
    TaskAgentInfo,
    UIElicitationSchemaType,
)


async def test_session_usage_get_metrics_deserializes_shared_event_type():
    payload = {
        "codeChanges": {
            "filesModified": ["example.py"],
            "filesModifiedCount": 1,
            "linesAdded": 4,
            "linesRemoved": 2,
        },
        "lastCallInputTokens": 12,
        "lastCallOutputTokens": 7,
        "modelMetrics": {},
        "sessionStartTime": "2026-10-07T00:00:00Z",
        "totalApiDurationMs": 125,
        "totalPremiumRequestCost": 1.5,
        "totalUserRequests": 2,
        "aiCreditsStatus": "partial",
    }
    client = Mock(request=AsyncMock(return_value=payload))

    result = await SessionRpc(client, "usage-session").usage.get_metrics(timeout=2.5)

    assert isinstance(result, UsageGetMetricsResult)
    assert result.code_changes.lines_added == 4
    assert result.last_call_input_tokens == 12
    assert result.total_user_requests == 2
    assert result.session_start_time.isoformat() == "2026-10-07T00:00:00+00:00"
    assert result.total_api_duration == timedelta(milliseconds=125)
    assert result.ai_credits_status.value == "partial"
    client.request.assert_awaited_once_with(
        "session.usage.getMetrics", {"sessionId": "usage-session"}, timeout=2.5
    )


async def test_quota_warnings_deserialize_records_and_empty_lists():
    payload = [
        {
            "warningType": "quota",
            "message": "Usage is almost exhausted",
            "url": "https://example.test",
        },
        {"warningType": "limit", "message": "A limit was reached"},
    ]
    client = Mock(request=AsyncMock(side_effect=[payload, []]))
    api = SessionRpc(client, "quota-session").quota

    warnings = await api.take_warnings()

    assert all(isinstance(warning, QuotaWarningProjection) for warning in warnings)
    assert [warning.warning_type for warning in warnings] == ["quota", "limit"]
    assert warnings[0].message == payload[0]["message"]
    assert warnings[0].url == payload[0]["url"]
    assert warnings[1].url is None
    assert [warning.to_dict() for warning in warnings] == payload
    assert await api.take_warnings() == []
    assert client.request.await_count == 2
    client.request.assert_awaited_with("session.quota.takeWarnings", {"sessionId": "quota-session"})


def test_customization_reload_outcome_preserves_future_enum_values():
    payload = {"status": "newStatus", "subsystem": "newSubsystem", "detail": "new component"}
    outcome = CustomizationReloadOutcome.from_dict(payload)

    assert outcome.status.value == payload["status"]
    assert outcome.subsystem.value == payload["subsystem"]
    assert outcome.to_dict() == payload
    assert outcome.status is CustomizationReloadStatus("newStatus")
    assert outcome.subsystem is CustomizationReloadSubsystem("newSubsystem")
    assert CustomizationReloadOutcome.from_dict(payload).status is outcome.status
    assert CustomizationReloadOutcome.from_dict(payload).subsystem is outcome.subsystem
    assert CustomizationReloadStatus("anotherStatus") is not outcome.status
    assert CustomizationReloadSubsystem("anotherSubsystem") is not outcome.subsystem
    assert CustomizationReloadStatus("reloaded") is CustomizationReloadStatus.RELOADED
    assert CustomizationReloadSubsystem("skills") is CustomizationReloadSubsystem.SKILLS
    with pytest.raises(ValueError):
        CustomizationReloadStatus(None)


async def test_host_dispose_deserializes_empty_acknowledgement():
    client = Mock(request=AsyncMock(return_value={}))

    result = await ServerRpc(client).host.dispose(HostDisposeRequest(host_id="host"))

    assert isinstance(result, HostEmptyResult)
    assert result.to_dict() == {}
    client.request.assert_awaited_once_with("host.dispose", {"hostId": "host"})


async def test_host_shutdown_handler_round_trips_empty_acknowledgement():
    client = JsonRpcClient(Mock())
    client._send_message = AsyncMock()
    host = Mock(shutdown=AsyncMock(return_value=HostEmptyResult()))
    register_client_global_api_handlers(client, ClientGlobalApiHandlers(host=host))

    await client._dispatch_request(
        {"jsonrpc": "2.0", "id": "shutdown", "method": "host.shutdown", "params": {}},
        client.request_handlers["host.shutdown"],
        cancellation_id=None,
    )

    host.shutdown.assert_awaited_once()
    assert isinstance(host.shutdown.call_args.args[0], HostEmptyResult)
    client._send_message.assert_awaited_once_with(
        {"jsonrpc": "2.0", "id": "shutdown", "result": {}}
    )


def test_sandbox_config_round_trips_allow_bypass_and_omits_when_absent():
    configured = SandboxConfig(enabled=True, allow_bypass=True)

    assert configured.to_dict() == {"enabled": True, "allowBypass": True}
    assert SandboxConfig.from_dict(configured.to_dict()).allow_bypass is True
    assert SandboxConfig(enabled=True).to_dict() == {"enabled": True}


def test_mcp_oauth_scopes_preserve_existing_positional_parameters():
    parameters = inspect.signature(MCPServerConfigHTTP).parameters

    assert parameters["oauth_scopes"].kind is inspect.Parameter.KEYWORD_ONLY
    assert parameters["oidc"].kind is inspect.Parameter.POSITIONAL_OR_KEYWORD

    config = MCPServerConfigHTTP.from_dict(
        {
            "url": "https://example.test/mcp",
            "oauthScopes": ["tools:read", "resources:read"],
            "oidc": True,
        }
    )
    assert config.oauth_scopes == ["tools:read", "resources:read"]
    assert config.oidc is True


def test_auth_read_value_preserves_existing_positional_parameters():
    assert list(inspect.signature(AuthReadValue).parameters) == [
        "kind",
        "account",
        "status",
        "errors",
        "auth_info",
    ]
    payload = {
        "kind": "activeAccount",
        "authInfo": {"type": "user", "host": "https://github.com", "login": "octocat"},
    }
    result = AuthReadValue.from_dict(payload)
    assert result.auth_info.login == "octocat"
    assert result.status is None
    assert result.errors is None
    assert result.to_dict() == payload


def test_model_list_preserves_existing_positional_parameters():
    models = [{"id": "fixture-model"}]
    prices = []
    providers = []
    quotas = {"premium": {"remaining": 42}}

    result = SessionModelList(models, prices, providers, quotas)

    assert result.list is models
    assert result.auto is None
    assert result.model_price_categories is prices
    assert result.providers is providers
    assert result.quota_snapshots is quotas
    assert result.to_dict() == {
        "list": models,
        "modelPriceCategories": prices,
        "providers": providers,
        "quotaSnapshots": quotas,
    }


def test_model_list_round_trips_metadata_without_remapping_existing_fields():
    payload = {
        "list": [{"id": "fixture-model"}],
        "modelPriceCategories": [],
        "providers": [],
        "quotaSnapshots": {"premium": {"remaining": 42}},
        "auto": {
            "defaultTier": "premium-v2",
            "tiers": [
                {
                    "id": "premium-v2",
                    "displayName": "Premium",
                    "description": "Provider tier",
                    "type": "auto",
                    "status": {"enabled": True},
                }
            ],
        },
    }

    result = SessionModelList.from_dict(payload)

    assert result.auto is not None
    assert result.auto.default_tier == "premium-v2"
    assert result.auto.tiers[0].id == "premium-v2"
    assert result.model_price_categories == []
    assert result.providers == []
    assert result.quota_snapshots == payload["quotaSnapshots"]
    assert result.to_dict() == payload


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
