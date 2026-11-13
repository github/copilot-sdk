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
    FetchResponseMode,
    FetchSendResult,
    UsageGetMetricsResult,
    UserSettingsWriteKeyRequest,
    _InternalServerRpc,
    _SettingsRedactValueForDisplayRequest,
    register_client_global_api_handlers,
)
from copilot.rpc import (
    AgentInfo,
    AuthEnumerateQuery,
    AuthReadValue,
    BuiltinToolInputSchemaType,
    CatalogInvalidRequestError,
    CommandsApi,
    CommandsInvokeRequest,
    CommandsRespondToQueuedCommandRequest,
    DiscoveredExtensionPlugin,
    DiscoveredHook,
    HostDisposeRequest,
    HostEmptyResult,
    InstalledPluginInfo,
    InstructionSource,
    LocalSessionMetadataValue,
    LogResult,
    MCPGrantType,
    MCPSerializableServerConfig,
    MCPServerConfigHTTP,
    MCPServerConfigLocal,
    MCPStartServerRequest,
    PendingPermissionRequest,
    Plugin,
    ProtocolExternalToolDefinition,
    ProtocolSystemMessageConfig,
    QueuedCommandHandled,
    QueuedCommandNotHandled,
    QuotaWarningProjection,
    RemoteControlStatusOff,
    RemoteControlStatusResult,
    RemoteSessionMetadataValue,
    SandboxConfig,
    SandboxConfigUserPolicyFilesystem,
    ServerRpc,
    ServerSkill,
    SessionList,
    SessionModelList,
    SessionRpc,
    ShellBuildInteractiveEnvironmentRequest,
    ShellBuildInteractiveEnvironmentResult,
    Skill,
    SlashCommandAgentPromptResult,
    SlashCommandTextResult,
    TaskAgentInfo,
    UIElicitationSchemaType,
    UpdateSubagentSettingsRequest,
)


@pytest.mark.parametrize(
    "model,environment_field",
    [
        (ShellBuildInteractiveEnvironmentRequest, "hostEnvironment"),
        (ShellBuildInteractiveEnvironmentResult, "environment"),
    ],
)
def test_shell_environment_repr_redacts_values_without_changing_wire(model, environment_field):
    environment = {
        "GITHUB_TOKEN": "fake-shell-token",
        "CUSTOM_CREDENTIAL": "fake-shell-custom-credential",
    }
    wire = {environment_field: environment}
    if model is ShellBuildInteractiveEnvironmentRequest:
        wire.update(additionalBlockedNames=[], caseInsensitive=False)
    value = model.from_dict(wire)

    for secret in environment.values():
        assert secret not in repr(value)
    assert value.to_dict() == wire


@pytest.mark.parametrize("mode", list(FetchResponseMode))
def test_fetch_response_mode_matches_request_mode_and_round_trips(mode):
    wire = {
        "responseMode": mode.value,
        "url": "https://example.com",
        "status": 200,
        "statusText": "OK",
        "headers": [],
    }
    if mode == FetchResponseMode.TEXT:
        wire.update(bodyText="hello", endedAtUnverifiedEof=False)
    elif mode == FetchResponseMode.FILE:
        wire.update(path="response.txt", sizeBytes=5, endedAtUnverifiedEof=False)
    result = FetchSendResult.from_dict(wire)
    assert result.response_mode == mode
    assert result.to_dict() == wire


@pytest.mark.parametrize("value", ["anthropic", "azure", "openai"])
def test_legacy_provider_type_remains_compatible_with_config_and_endpoint(value):
    from copilot import rpc

    kind = rpc.ProviderType(value)
    config = rpc.ProviderConfig(base_url="http://localhost:1234", type=kind)
    endpoint = rpc.ProviderEndpoint(base_url="http://localhost:1234", headers={}, type=kind)

    assert "ProviderType" in rpc.__all__
    assert config.to_dict()["type"] == value
    assert endpoint.to_dict()["type"] == value
    assert rpc.ProviderConfig.from_dict(config.to_dict()).type is kind
    assert rpc.ProviderEndpoint.from_dict(endpoint.to_dict()).type is kind
    assert config.type is kind
    assert endpoint.type is kind
    named = rpc.NamedProviderConfig.from_dict(
        {"name": "local", "baseUrl": config.base_url, "type": value}
    )
    assert named.type is kind
    assert named.to_dict()["type"] == value
    current = rpc.ProviderConfig(base_url=config.base_url, type=rpc.ProviderConfigType(value))
    assert current.to_dict()["type"] == value
    assert current.type is rpc.ProviderConfigType(value)
    restored = rpc.ProviderConfig.from_dict(current.to_dict())
    assert restored.type is current.type
    assert restored == current
    named_current = rpc.NamedProviderConfig(
        name="local", base_url=config.base_url, type=current.type
    )
    assert rpc.NamedProviderConfig.from_dict(named_current.to_dict()) == named_current


def test_winml_provider_type_keeps_its_new_config_representation():
    from copilot import rpc

    config = rpc.ProviderConfig(base_url="model.gguf", type=rpc.ProviderConfigType.WINML)
    assert config.to_dict()["type"] == "winml"
    assert rpc.ProviderConfig.from_dict(config.to_dict()).type is rpc.ProviderConfigType.WINML
    with pytest.raises(ValueError):
        rpc.ProviderEndpoint.from_dict({"baseUrl": config.base_url, "headers": {}, "type": "winml"})
    with pytest.raises(ValueError):
        rpc.ProviderEndpoint(
            base_url=config.base_url, headers={}, type=rpc.ProviderConfigType.WINML
        ).to_dict()


@pytest.mark.parametrize(
    "model_name,enum_name,field,payload",
    [
        (
            "LocalModelGPURequirement",
            "LocalModelGPURequirementKind",
            "kind",
            {"kind": "unknown"},
        ),
        (
            "LocalModelTimelineMessage",
            "SessionLogLevel",
            "type",
            {"type": "info", "text": "Model ready"},
        ),
    ],
)
def test_local_model_values_preserve_published_types_and_enum_identities(
    model_name, enum_name, field, payload
):
    from copilot import rpc

    restored = getattr(rpc, model_name).from_dict(payload)

    assert getattr(restored, field) is getattr(rpc, enum_name)(payload[field])
    assert restored.to_dict() == payload


@pytest.mark.parametrize(
    "model,fields,payload",
    [
        (
            AuthEnumerateQuery,
            "kind refresh broker_available provider",
            {"kind": "accounts", "refresh": True, "brokerAvailable": False, "provider": "entra"},
        ),
        (
            ProtocolSystemMessageConfig,
            "content mode content_blocks sections",
            {"content": "Additional instructions", "mode": "append"},
        ),
    ],
)
def test_flat_union_positional_constructors_preserve_branch_field_order(model, fields, payload):
    restored = model.from_dict(payload)
    positional = model(*(getattr(restored, name) for name in fields.split()))

    assert positional.to_dict() == payload


def test_http_oauth_grant_uses_the_published_shared_enum():
    payload = {"url": "https://example.test/mcp", "oauthGrantType": "client_credentials"}
    restored = MCPServerConfigHTTP.from_dict(payload)

    assert restored.oauth_grant_type is MCPGrantType.CLIENT_CREDENTIALS
    configured = MCPServerConfigHTTP(
        url=payload["url"], oauth_grant_type=MCPGrantType.CLIENT_CREDENTIALS
    )
    assert configured.to_dict() == payload


def test_shared_permission_union_rejects_an_unrelated_dataclass():
    request = PendingPermissionRequest(request=HostEmptyResult(), request_id="request")

    with pytest.raises(AssertionError):
        request.to_dict()


@pytest.mark.parametrize("value,error", [(42, AttributeError), (None, TypeError)])
def test_uuid_rpc_field_preserves_stdlib_conversion_errors(value, error):
    with pytest.raises(error):
        LogResult.from_dict({"eventId": value})


def test_external_tool_definition_preserves_positional_constructor():
    definition = ProtocolExternalToolDefinition("Description", "lookup", None, True)
    assert definition.is_terminal is True
    expected = {"description": "Description", "name": "lookup", "isTerminal": True}
    assert definition.to_dict() == expected
    owned = expected | {"hasHandler": True}
    assert ProtocolExternalToolDefinition.from_dict(owned).to_dict() == owned


@pytest.mark.asyncio
async def test_raw_setting_write_keeps_required_null():
    client = Mock()
    client.request = AsyncMock(return_value=None)
    await _InternalServerRpc(client).user.settings._write_key(
        UserSettingsWriteKeyRequest(key="showReasoning", value=None)
    )
    client.request.assert_awaited_once_with(
        "user.settings.writeKey", {"key": "showReasoning", "value": None}
    )


@pytest.mark.parametrize(
    "model,legacy_fields,payload,metadata",
    [
        (
            AgentInfo,
            "description display_name id name disable_model_invocation hooks mcp_servers model "
            "model_policy models path prompt reasoning_effort skills source tools user_invocable",
            {
                "description": "Description",
                "displayName": "Agent",
                "id": "agent-id",
                "name": "agent",
                "prompt": "Prompt",
                "reasoningEffort": "high",
                "skills": ["skill"],
                "userInvocable": True,
            },
            {"pluginId": "builtin:tools"},
        ),
        (
            DiscoveredExtensionPlugin,
            "name",
            {"name": "tools"},
            {"id": "builtin:tools"},
        ),
        (
            DiscoveredHook,
            "enabled hook_type id origin disable_key project_path source",
            {
                "enabled": True,
                "hookType": "preToolUse",
                "id": "hook",
                "origin": "plugin",
                "disableKey": "disable-hook",
                "projectPath": "/project",
                "source": "tools",
            },
            {"pluginId": "builtin:tools", "definitionFileUri": "file:///hooks.json"},
        ),
        (
            InstalledPluginInfo,
            "enabled marketplace name description direct_source_id icon_url installed "
            "installed_from managed managed_desired_enabled source verified_source version",
            {
                "enabled": True,
                "marketplace": "catalog",
                "name": "tools",
                "installed": True,
                "installedFrom": "/catalog",
                "source": "source",
                "version": "1.0",
            },
            {"id": "mp:catalog::tools", "rootPath": "/plugins/tools"},
        ),
        (
            InstructionSource,
            "content id label location source_path type apply_to default_disabled description "
            "project_path",
            {
                "content": "Content",
                "id": "instruction",
                "label": "Instructions",
                "location": "plugin",
                "sourcePath": "/instructions.md",
                "type": "plugin",
                "projectPath": "/project",
            },
            {"pluginId": "builtin:tools"},
        ),
        (
            Plugin,
            "enabled marketplace name direct_source_id installed installed_from managed "
            "managed_desired_enabled source version",
            {
                "enabled": True,
                "marketplace": "catalog",
                "name": "tools",
                "installed": True,
                "installedFrom": "/catalog",
                "source": "source",
                "version": "1.0",
            },
            {"id": "mp:catalog::tools", "rootPath": "/plugins/tools"},
        ),
        (
            ServerSkill,
            "description enabled name source user_invocable argument_hint command_name path "
            "project_path",
            {
                "description": "Description",
                "enabled": True,
                "name": "skill",
                "source": "plugin",
                "userInvocable": True,
                "path": "/SKILL.md",
                "projectPath": "/project",
            },
            {"pluginId": "builtin:tools"},
        ),
        (
            Skill,
            "description enabled name source user_invocable argument_hint command_name "
            "declared_name mcp_origin path plugin_name",
            {
                "description": "Description",
                "enabled": True,
                "name": "skill",
                "source": "plugin",
                "userInvocable": True,
                "path": "/SKILL.md",
                "pluginName": "tools",
            },
            {"pluginId": "builtin:tools"},
        ),
    ],
)
def test_customization_metadata_preserves_positional_constructors(
    model, legacy_fields, payload, metadata
):
    restored = model.from_dict(payload)
    legacy = model(*(getattr(restored, name) for name in legacy_fields.split()))
    assert legacy.to_dict() == payload
    assert model.from_dict(legacy.to_dict()) == legacy
    extended = payload | metadata
    assert model.from_dict(extended).to_dict() == extended


def test_catalog_product_validation_refusal_preserves_typed_field():
    payload = {
        "kind": "invalid-request",
        "field": "product",
        "message": "The product must contain between 1 and 256 characters.",
    }

    result = CatalogInvalidRequestError.from_dict(payload)

    assert result.kind == "invalid-request"
    assert result.field.value == "product"
    assert result.field.name == "PRODUCT"
    assert result.to_dict() == payload


async def test_settings_redaction_keeps_required_opaque_null():
    client = Mock(request=AsyncMock(return_value={"value": None}))

    result = await _InternalServerRpc(client).settings._redact_value_for_display(
        _SettingsRedactValueForDisplayRequest(value=None, base_path=[])
    )

    client.request.assert_awaited_once_with(
        "settings.redactValueForDisplay", {"value": None, "basePath": []}
    )
    assert result.value is None


@pytest.mark.parametrize(
    "context_settings",
    [{}, {"contextManagementTools": True}, {"contextManagementTools": False}],
    ids=["absent", "enabled", "disabled"],
)
async def test_subagent_settings_getter_setter_preserve_context_management_tools(context_settings):
    settings = {
        "agents": {
            "rubber-duck": {
                "model": "model-id",
                "providerId": "provider-id",
                "autoInvoke": False,
            },
            "task": {"model": "task-model", "contextTier": "long_context"},
        },
        "disabledSubagents": ["explore"],
        "maxConcurrency": 7,
        "maxDepth": 3,
        **context_settings,
    }
    client = Mock(request=AsyncMock(side_effect=[{"subagents": settings}, {}]))
    api = SessionRpc(client, "settings-session").tools

    result = await api.get_subagent_settings()
    assert result.subagents is not None
    assert result.subagents.agents is not None
    result.subagents.agents["rubber-duck"].auto_invoke = True
    await api.update_subagent_settings(UpdateSubagentSettingsRequest(subagents=result.subagents))

    expected = {
        **settings,
        "agents": {
            **settings["agents"],
            "rubber-duck": {**settings["agents"]["rubber-duck"], "autoInvoke": True},
        },
    }
    assert client.request.await_count == 2
    client.request.assert_awaited_with(
        "session.tools.updateSubagentSettings",
        {"sessionId": "settings-session", "subagents": expected},
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


def test_sandbox_extensions_preserve_existing_positional_parameters():
    legacy_fields = [
        "enabled",
        "add_current_working_directory",
        "allow_bypass",
        "allow_dev_tool_access",
        "auth",
        "credentials",
        "managed_lsp_routing_locked",
        "managed_mcp_routing_locked",
        "sandbox_lsp_servers",
        "sandbox_mcp_servers",
        "user_policy",
    ]
    parameters = list(inspect.signature(SandboxConfig).parameters)
    assert parameters[: len(legacy_fields)] == legacy_fields

    config = SandboxConfig(True, None, None, None, None, None, None, None, False, True)
    assert config.to_dict() == {
        "enabled": True,
        "sandboxLspServers": False,
        "sandboxMcpServers": True,
    }
    assert SandboxConfig.from_dict(config.to_dict()) == config
    extended = SandboxConfig(
        True,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        False,
        True,
        sandbox_extensions=False,
        managed_extensions_routing_locked=True,
    )
    assert SandboxConfig.from_dict(extended.to_dict()) == extended
    assert extended.to_dict()["sandboxExtensions"] is False
    assert extended.to_dict()["managedExtensionsRoutingLocked"] is True


def test_sandbox_enumerate_paths_preserve_existing_positional_parameters():
    filesystem = SandboxConfigUserPolicyFilesystem(["denied"], ["readonly"], ["readwrite"])
    assert filesystem.to_dict() == {
        "deniedPaths": ["denied"],
        "readonlyPaths": ["readonly"],
        "readwritePaths": ["readwrite"],
    }

    payload = {**filesystem.to_dict(), "enumeratePaths": ["C:\\"]}
    restored = SandboxConfigUserPolicyFilesystem.from_dict(payload)
    assert restored == SandboxConfigUserPolicyFilesystem(
        ["denied"], ["readonly"], ["readwrite"], enumerate_paths=["C:\\"]
    )
    assert restored.to_dict() == payload


def test_slash_command_prompt_preserves_existing_positional_parameters():
    result = SlashCommandAgentPromptResult("Display prompt", "Agent prompt", None, None, True)

    assert result.runtime_settings_changed is True
    assert result.reservation_token is None
    payload = {
        "displayPrompt": "Display prompt",
        "kind": "agent-prompt",
        "prompt": "Agent prompt",
        "runtimeSettingsChanged": True,
    }
    assert result.to_dict() == payload

    payload["reservationToken"] = "reservation-token"
    restored = SlashCommandAgentPromptResult.from_dict(payload)
    assert restored.runtime_settings_changed is True
    assert restored.reservation_token == "reservation-token"
    assert restored.to_dict() == payload


def test_local_session_metadata_preserves_existing_positional_parameters():
    metadata = LocalSessionMetadataValue(
        False, "2026-01-01T01:00:00Z", "s1", "2026-01-01T00:00:00Z", "github/cli"
    )

    assert list(inspect.signature(LocalSessionMetadataValue).parameters)[-1] == "last_client_name"
    assert metadata.client_name == "github/cli"
    assert metadata.last_client_name is None
    payload = {
        "clientName": "github/cli",
        "isRemote": False,
        "lastClientName": "vscode-agent-host",
        "modifiedTime": "2026-01-01T01:00:00Z",
        "sessionId": "s1",
        "startTime": "2026-01-01T00:00:00Z",
    }
    restored = LocalSessionMetadataValue.from_dict(payload)
    assert restored.last_client_name == "vscode-agent-host"
    assert restored.to_dict() == payload


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


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "model,payload",
    [
        (
            MCPSerializableServerConfig,
            {"type": "stdio", "command": "example", "args": []},
        ),
        (
            MCPServerConfigLocal,
            {
                "type": "local-http",
                "socketPath": "/runtime/ide.sock",
                "headers": {},
                "ide": {
                    "ideName": "Test IDE",
                    "workspaceFolder": "/workspace",
                    "pid": 123,
                    "lockFilePath": "/runtime/ide.lock",
                    "clientVersion": "1.0.0",
                    "clientPid": 456,
                    "parentPid": 1,
                },
            },
        ),
    ],
)
async def test_mcp_start_accepts_existing_and_local_config_models(model, payload):
    client = Mock()
    client.request = AsyncMock(return_value=None)
    config = model.from_dict(payload)

    await SessionRpc(client, "ide-session").mcp.start_server(
        MCPStartServerRequest(server_name="ide", config=config)
    )

    client.request.assert_awaited_once_with(
        "session.mcp.startServer",
        {"sessionId": "ide-session", "serverName": "ide", "config": payload},
    )
    assert config.to_dict() == payload


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "config",
    [
        {
            "type": "http",
            "url": "https://example.test/mcp",
            "headers": {"Authorization": "Bearer test-token", "X-Workspace": "test"},
        },
        {
            "type": "local-http",
            "socketPath": "/runtime/ide.sock",
            "headers": {"Authorization": "Bearer test-token"},
        },
    ],
)
async def test_mcp_start_request_preserves_header_values(config):
    client = Mock()
    client.request = AsyncMock(return_value=None)
    payload = {"serverName": "ide", "config": config}
    request = MCPStartServerRequest.from_dict(payload)

    await SessionRpc(client, "ide-session").mcp.start_server(request)

    assert request.to_dict() == payload
    client.request.assert_awaited_once_with(
        "session.mcp.startServer", {"sessionId": "ide-session", **payload}
    )


def test_auth_read_value_preserves_existing_positional_parameters():
    assert list(inspect.signature(AuthReadValue).parameters) == [
        "kind",
        "account",
        "status",
        "errors",
        "auth_info",
        "auth_selection_id",
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
