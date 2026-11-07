"""
Copilot CLI SDK Client - Main entry point for the Copilot SDK.

This module provides the :class:`CopilotClient` class, which manages the connection
to the Copilot CLI server and provides session management capabilities.

Example:
    >>> from copilot import CopilotClient
    >>>
    >>> async with CopilotClient() as client:
    ...     session = await client.create_session()
    ...     await session.send("Hello!")
"""

from __future__ import annotations

import asyncio
import codecs
import inspect
import ipaddress
import logging
import os
import re
import shutil
import subprocess
import sys
import threading
import time
import uuid
import weakref
from collections.abc import Awaitable, Callable, Mapping, Sequence
from concurrent.futures import Future as ConcurrentFuture
from dataclasses import dataclass, field
from datetime import UTC, datetime
from pathlib import Path
from types import TracebackType
from typing import Any, ClassVar, Literal, NotRequired, TypedDict, cast, overload

from ._diagnostics import log_timing
from ._ffi_runtime_host import FfiRuntimeHost
from ._jsonrpc import JsonRpcClient, JsonRpcError, ProcessExitedError
from ._mode import (
    CopilotClientMode,
    ToolSet,
    _custom_agents_local_only_default,
    _embedding_cache_storage_default,
    _enable_experimental_mode_default,
    _enable_file_hooks_default,
    _enable_host_git_operations_default,
    _enable_on_demand_instruction_discovery_default,
    _enable_session_store_default,
    _enable_session_telemetry_default,
    _enable_skills_default,
    _mcp_oauth_token_storage_default,
    _memory_default,
    _normalize_tool_filter,
    _post_create_options_patch,
    _require_available_tools_for_empty_mode,
    _require_storage_for_empty_mode,
    _skip_embedding_retrieval_default,
    _system_message_for_mode,
    _validate_tool_filter_list,
)
from ._sdk_protocol_version import get_sdk_protocol_version
from ._telemetry import get_trace_context
from .canvas import (
    CanvasDeclaration,
    CanvasHandler,
    CanvasProviderIdentity,
    ExtensionInfo,
)
from .copilot_request_handler import (
    CopilotRequestHandler,
    _CopilotRequestAdapterHandler,
    create_copilot_request_adapter,
)
from .entra import EntraAuthOptions, _EntraTokenAdapter
from .generated.rpc import (
    ClientGlobalApiHandlers,
    ClientSessionApiHandlers,
    DiagnosticsConfiguration,
    EntraAuthConfig,
    ExtensionLaunchProviderHandler,
    GitHubTelemetryNotification,
    GitHubTokenAcquireReason,
    GitHubTokenAcquireRequest,
    GitHubTokenAcquireResult,
    HostDiagnostics,
    ModelBillingTokenPrices,
    ModelBillingTokenPricesLongContext,  # noqa: F401
    ModelProviderRef,
    NetworkAuthenticateNegotiateOptions,
    NetworkAuthenticateNegotiateRequest,
    NetworkAuthenticateNegotiateResult,
    NetworkResolveProxyOptions,
    NetworkResolveProxyRequest,
    NetworkResolveProxyResult,
    NetworkScope,
    NetworkSetProviderRequest,
    OpenCanvasInstance,
    PermissionsSetModeResult,
    RemoteSessionMode,  # noqa: F401
    SandboxConfig,
    ServerRpc,
    SessionsFindByPrefixRequest,
    UIThreadRunRequest,
    _ConnectResult,
    _HookInvokeRequest,
    _HookInvokeResponse,
    from_datetime,
    register_client_global_api_handlers,
    register_client_session_api_handlers,
)
from .generated.session_events import (
    PermissionMode,
    SessionEvent,
    session_event_from_dict,
)
from .host import AhpHost, AhpHostOptions, _AhpHostManager
from .installation_confirmation import (
    InstallationConfirmationHandler,
    _InstallationConfirmationAdapter,
)
from .session import (
    AutoModeSwitchHandler,
    AutopilotOptions,
    AutoTier,
    BearerTokenProvider,
    CommandDefinition,
    ContextTier,
    CopilotSession,
    CreateSessionFsHandler,
    CustomAgentConfig,
    DefaultAgentConfig,
    ElicitationHandler,
    ExitPlanModeHandler,
    GitHubMcpToolConfig,
    ImageGenerationConfig,
    InfiniteSessionConfig,
    LargeToolOutputConfig,
    McpAuthHandler,
    MCPServerConfig,
    MemoryConfiguration,
    ModelCapabilitiesOverride,
    NamedProviderConfig,
    ProviderConfig,
    ProviderModelConfig,
    ReasoningEffort,
    ReasoningSummary,
    SectionTransformFn,
    SessionFsConfig,
    SessionHooks,
    SessionLimitsConfig,
    SkillProvider,
    StoredMemory,
    SystemMessageConfig,
    ToolSearchConfig,
    TranscriptRecoveryReport,
    UserInputHandler,
    _capabilities_to_dict,
    _PermissionHandlerFn,
    _tools_to_wire_definitions,
)
from .session_fs_provider import (
    SessionFsProvider,
    SessionFsReadFileRangeProvider,
    create_session_fs_adapter,
)
from .tools import Tool

logger = logging.getLogger(__name__)


class GitHubTokenProviderArgs(TypedDict):
    """Arguments passed to a session-scoped :data:`GitHubTokenProvider`.

    The opaque callback registration identifier is intentionally not exposed.
    """

    host: str
    session_id: str | None
    reason: GitHubTokenAcquireReason


class GitHubTokenResult(TypedDict):
    """A GitHub token returned by a session-scoped provider."""

    kind: Literal["token"]
    accessToken: str
    expiresIn: int
    tokenType: NotRequired[str]


class GitHubTokenCancelledResult(TypedDict):
    """An explicit cancellation returned by a session-scoped provider."""

    kind: Literal["cancelled"]


GitHubTokenProviderResult = GitHubTokenResult | GitHubTokenCancelledResult
"""Result returned by a session-scoped GitHub token provider."""


GitHubTokenProvider = Callable[
    [GitHubTokenProviderArgs],
    GitHubTokenProviderResult | Awaitable[GitHubTokenProviderResult],
]
"""Acquire a GitHub credential for one session.

Token results require ``expiresIn`` to be the positive number of seconds of
remaining lifetime when the callback completes. Production GitHub tokens
typically last eight hours. Initial cancellation, callback errors, and invalid
token responses reject session creation or resume instead of falling back to
ambient authentication.
"""


def _required_string_param(params: dict, name: str) -> str:
    value = params.get(name) if isinstance(params, dict) else None
    if not isinstance(value, str):
        raise JsonRpcError(-32602, "Invalid params")
    return value


def _permission_mode_result(response: dict) -> PermissionsSetModeResult | None:
    result = response.get("permissionModeResult")
    return PermissionsSetModeResult.from_dict(result) if result is not None else None


# ============================================================================
# Connection Types
# ============================================================================

_ConnectionState = Literal["disconnected", "connecting", "connected", "error"]

LogLevel = Literal["none", "error", "warning", "info", "debug", "all"]
AskUserVariant = Literal["legacy", "elicitation"]
"""Model-facing shape of the runtime's built-in ``ask_user`` tool."""


@dataclass
class CloudSessionRepository:
    """GitHub repository metadata to associate with a cloud session."""

    owner: str
    name: str
    branch: str | None = None


@dataclass
class CloudSessionOptions:
    """Options for creating a remote session in the cloud."""

    repository: CloudSessionRepository | None = None


ExpFlagValue = str | int | float | bool | None
"""A single ExP (Experiment Platform) flag value.

ExP assignments resolve to a string, number, boolean, or ``None``.
"""


@dataclass
class ExpConfigEntry:
    """A single configuration entry in a :class:`CopilotExpAssignmentResponse`.

    Each entry carries an identifier and a bag of typed parameter values.
    """

    id: str
    """Identifier of the configuration entry."""
    parameters: dict[str, ExpFlagValue] = field(default_factory=dict)
    """Parameter values keyed by parameter name."""


@dataclass
class CopilotExpAssignmentResponse:
    """ExP ("flight") assignment data.

    Uses the same JSON shape the Copilot CLI fetches from the experimentation
    service. Serialized on the wire with PascalCase keys to match the contract
    consumed by the runtime.
    """

    features: list[str] = field(default_factory=list)
    """Enabled feature names."""
    flights: dict[str, str] = field(default_factory=dict)
    """Assigned flights keyed by flight name."""
    configs: list[ExpConfigEntry] = field(default_factory=list)
    """Configuration entries carrying typed parameter values."""
    assignment_context: str = ""
    """Assignment context string forwarded to CAPI and telemetry."""
    parameter_groups: Any | None = None
    """Opaque parameter-group payload passed through untouched. Optional."""
    flighting_version: int | None = None
    """Version of the flighting configuration. Optional."""
    impression_id: str | None = None
    """Impression identifier for the assignment. Optional."""


def _exp_assignment_response_to_dict(
    response: CopilotExpAssignmentResponse,
) -> dict[str, Any]:
    wire: dict[str, Any] = {
        "Features": list(response.features),
        "Flights": dict(response.flights),
        "Configs": [
            {"Id": entry.id, "Parameters": dict(entry.parameters)} for entry in response.configs
        ],
        "AssignmentContext": response.assignment_context,
    }
    if response.parameter_groups is not None:
        wire["ParameterGroups"] = response.parameter_groups
    if response.flighting_version is not None:
        wire["FlightingVersion"] = response.flighting_version
    if response.impression_id is not None:
        wire["ImpressionId"] = response.impression_id
    return wire


class CapiSessionOptions(TypedDict, total=False):
    """Provider-scoped Copilot API (CAPI) session options."""

    auto_tier: AutoTier
    """Routing preference used when the session model is ``auto``.

    Requires a runtime with Auto tier support and V2 Auto routing. When omitted
    on create, the runtime uses its default routing behavior. The runtime persists
    this preference across cold resume; when omitted on cold resume, it restores
    the last committed preference. On resident resume, a different tier requests a
    safe switch that takes effect after resume succeeds and never disturbs a turn
    that is already running.

    To change the preference on a live session, call
    :meth:`CopilotSession.set_auto_tier` instead.
    """

    enable_web_socket_responses: bool
    """Whether to use WebSocket transport for the CAPI Responses API.

    Enabled by default when the model advertises ``ws:/responses`` support. Set
    to ``False`` to force the HTTP Responses transport instead, which is
    equivalent to the ``COPILOT_CLI_DISABLE_WEBSOCKET_RESPONSES`` environment
    variable and useful in environments where WebSockets are blocked (e.g.
    behind a proxy).
    """


def _cloud_session_options_to_dict(options: CloudSessionOptions) -> dict[str, Any]:
    result: dict[str, Any] = {}
    if options.repository is not None:
        repository: dict[str, Any] = {
            "owner": options.repository.owner,
            "name": options.repository.name,
        }
        if options.repository.branch is not None:
            repository["branch"] = options.repository.branch
        result["repository"] = repository
    return result


def _capi_session_options_to_wire(options: CapiSessionOptions) -> dict[str, Any]:
    wire: dict[str, Any] = {}
    if "auto_tier" in options:
        wire["autoTier"] = options["auto_tier"]
    if "enable_web_socket_responses" in options:
        wire["enableWebSocketResponses"] = options["enable_web_socket_responses"]
    return wire


class DisableBypassPermissionsModes:
    """Well-known managed bypass-permissions policies."""

    DISABLE: ClassVar[str] = "disable"
    """Turn off bypass-permissions mode entirely."""

    ALLOW_AUTO_ONLY: ClassVar[str] = "allow-auto-only"
    """Permit automatic bypass but block full allow-all."""


@dataclass
class ManagedSettingsPermissions:
    """Permissions-only managed policy injected via :class:`ManagedSettings`.

    Rule strings use the same vocabulary the runtime accepts for fetched
    managed policy (e.g. ``"Read(**)"``, ``"Shell(git push *)"``); malformed
    rules are rejected by the runtime at session creation.
    """

    disable_bypass_permissions_mode: str | None = None
    """Restricts bypass-permissions mode for the session. See
    :class:`DisableBypassPermissionsModes` for well-known values. Unknown values
    are forwarded so newer runtime policies fail closed. Sent on the wire as
    ``disableBypassPermissionsMode``."""
    deny: list[str] | None = None
    """Operations that must always be denied. Unioned across managed layers."""
    ask: list[str] | None = None
    """Operations that must prompt for approval. Unioned across managed layers."""
    allow: list[str] | None = None
    """Operations permitted without prompting. Every declared ``allow`` list
    across managed layers must admit an operation for it to be allowed."""
    limit_to: list[str] | None = None
    """Closed-world host boundary expressed as ``Domain(hostname)``,
    ``Domain(IP)``, or ``Domain(*.example.com)`` rules. Schemes, ports, paths,
    queries, and fragments are rejected. Multiple managed layers intersect
    their lists; an empty list denies all hosts. Sent on the wire as
    ``limitTo``."""
    disable_assisted_permissions_mode: bool | None = None
    """When true, prevents Assisted Permissions from being activated. Omit or
    set to false to impose no restriction. Sent on the wire as
    ``disableAssistedPermissionsMode``."""


@dataclass
class ManagedSettings:
    """Host-injected enterprise managed settings for a session.

    Unlike ``enable_managed_settings`` — which asks the runtime to *self-fetch*
    account/org and device policy — this supplies the managed policy directly.
    The runtime validates it with the same managed-permission parser it uses
    for fetched policy and composes it restrictively with any self-fetched
    (server) and device-managed (MDM) layers.

    The first supported contract is permissions-only; unknown sibling keys are
    rejected by the runtime. Serialized on the wire as ``managedSettings``.
    """

    permissions: ManagedSettingsPermissions | None = None
    """Managed permission policy for the session."""


def _managed_settings_to_dict(settings: ManagedSettings) -> dict[str, Any]:
    wire: dict[str, Any] = {}
    permissions = settings.permissions
    if permissions is not None:
        perms: dict[str, Any] = {}
        if permissions.disable_bypass_permissions_mode is not None:
            perms["disableBypassPermissionsMode"] = permissions.disable_bypass_permissions_mode
        if permissions.disable_assisted_permissions_mode is not None:
            perms["disableAssistedPermissionsMode"] = permissions.disable_assisted_permissions_mode
        if permissions.deny is not None:
            perms["deny"] = list(permissions.deny)
        if permissions.ask is not None:
            perms["ask"] = list(permissions.ask)
        if permissions.allow is not None:
            perms["allow"] = list(permissions.allow)
        if permissions.limit_to is not None:
            perms["limitTo"] = list(permissions.limit_to)
        wire["permissions"] = perms
    return wire


# Implicit provider name for the singular, whole-session ``provider`` config.
# Named providers are keyed by their own ``name``.
_DEFAULT_BEARER_TOKEN_PROVIDER_NAME = "default"


def _collect_bearer_token_callbacks(
    provider: ProviderConfig | None,
    providers: list[NamedProviderConfig] | None,
) -> dict[str, BearerTokenProvider]:
    """Collect per-provider ``bearer_token_provider`` callbacks keyed by provider name.

    The singular, whole-session ``provider`` uses the implicit
    ``_DEFAULT_BEARER_TOKEN_PROVIDER_NAME``; ``providers`` entries use their own
    ``name``. The callbacks are never serialized — the wire conversion emits
    ``hasBearerTokenProvider: true`` instead and the runtime calls back over
    ``providerToken.getToken``.
    """
    callbacks: dict[str, BearerTokenProvider] = {}
    if provider is not None:
        singular = provider.get("bearer_token_provider")
        if singular is not None:
            callbacks[_DEFAULT_BEARER_TOKEN_PROVIDER_NAME] = singular
    if providers:
        for named in providers:
            callback = named.get("bearer_token_provider")
            if callback is not None:
                callbacks[named["name"]] = callback
    return callbacks


def _validate_session_fs_config(config: SessionFsConfig) -> None:
    if not config.get("initial_working_directory"):
        raise ValueError("session_fs.initial_working_directory is required")
    if not config.get("session_state_path"):
        raise ValueError("session_fs.session_state_path is required")
    if config.get("conventions") not in ("posix", "windows"):
        raise ValueError("session_fs.conventions must be either 'posix' or 'windows'")


def _mcp_servers_to_wire(
    servers: dict[str, Any],
) -> dict[str, Any]:
    """Convert MCP server configs from public API format to wire format.

    Renames ``working_directory`` key to ``cwd`` in each server config dict.
    """
    wire: dict[str, Any] = {}
    for name, config in servers.items():
        if "working_directory" in config:
            config = {**config, "cwd": config["working_directory"]}
            del config["working_directory"]
        wire[name] = config
    return wire


def _large_output_to_wire(config: Mapping[str, Any]) -> dict[str, Any]:
    """Convert a ``LargeToolOutputConfig`` mapping to wire format."""
    wire: dict[str, Any] = {}
    if "enabled" in config:
        wire["enabled"] = config["enabled"]
    if "max_size_bytes" in config:
        wire["maxSizeBytes"] = config["max_size_bytes"]
    if "output_directory" in config:
        wire["outputDir"] = config["output_directory"]
    return wire


def _requested_memory_store(
    memory: MemoryConfiguration | None,
) -> Callable[[StoredMemory], Awaitable[None]] | None:
    """Return the app memory store callback, rejecting one that memory is not enabled for."""
    store_memory = memory.get("store_memory") if memory else None
    if memory is None or store_memory is None:
        return None
    if memory.get("enabled") is not True:
        raise ValueError("memory.store_memory requires memory.enabled to be true.")
    return store_memory


def _memory_to_wire(config: Mapping[str, Any]) -> dict[str, Any]:
    """Convert a ``MemoryConfiguration`` mapping to wire format."""
    wire: dict[str, Any] = {"enabled": config["enabled"]}
    if "mode" in config:
        wire["mode"] = config["mode"]
    return wire


def _autopilot_to_wire(config: AutopilotOptions) -> dict[str, Any]:
    wire: dict[str, Any] = {}
    if "continuation_enabled" in config:
        wire["continuationEnabled"] = config["continuation_enabled"]
    if "max_continues" in config:
        wire["maxContinues"] = config["max_continues"]
    return wire


def _session_limits_to_wire(config: Mapping[str, Any]) -> dict[str, Any]:
    """Convert a ``SessionLimitsConfig`` mapping to wire format."""
    wire: dict[str, Any] = {}
    if "max_ai_credits" in config:
        wire["maxAiCredits"] = config["max_ai_credits"]
    return wire


def _tool_search_to_wire(config: Mapping[str, Any]) -> dict[str, Any]:
    """Convert a ``ToolSearchConfig`` mapping to wire format."""
    wire: dict[str, Any] = {}
    if "enabled" in config:
        wire["enabled"] = config["enabled"]
    if "defer_threshold" in config:
        wire["deferThreshold"] = config["defer_threshold"]
    return wire


def _image_generation_to_wire(config: Mapping[str, Any]) -> dict[str, Any]:
    """Convert an ``ImageGenerationConfig`` mapping to wire format."""
    wire = dict(config)
    if "save_to_disk" in wire:
        wire["saveToDisk"] = wire.pop("save_to_disk")
    return wire


def _github_mcp_tool_config_to_wire(config: Mapping[str, Any]) -> dict[str, Any]:
    """Convert a ``GitHubMcpToolConfig`` mapping to wire format."""
    wire: dict[str, Any] = {}
    if "enable_all_tools" in config:
        wire["enableAllTools"] = config["enable_all_tools"]
    if "additional_toolsets" in config:
        wire["additionalToolsets"] = config["additional_toolsets"]
    if "additional_tools" in config:
        wire["additionalTools"] = config["additional_tools"]
    if "enable_insiders_mode" in config:
        wire["enableInsidersMode"] = config["enable_insiders_mode"]
    if "disable_form_deferral" in config:
        wire["disableFormDeferral"] = config["disable_form_deferral"]
    return wire


class TelemetryConfig(TypedDict, total=False):
    """Configuration for OpenTelemetry integration with the Copilot CLI."""

    otlp_endpoint: str
    """OTLP HTTP endpoint URL for trace/metric export. Sets OTEL_EXPORTER_OTLP_ENDPOINT."""
    otlp_protocol: Literal["http/json", "http/protobuf"]
    """OTLP HTTP protocol for all signals.

    Allowed values are "http/json" and "http/protobuf". Sets OTEL_EXPORTER_OTLP_PROTOCOL.
    """
    file_path: str
    """File path for JSON-lines trace output. Sets COPILOT_OTEL_FILE_EXPORTER_PATH."""
    exporter_type: str
    """Exporter backend type: "otlp-http" or "file". Sets COPILOT_OTEL_EXPORTER_TYPE."""
    source_name: str
    """Instrumentation scope name. Sets COPILOT_OTEL_SOURCE_NAME."""
    capture_content: bool
    """Whether to capture span content. Sets OTEL_INSTRUMENTATION_GENAI_CAPTURE_CONTENT and its legacy alias."""  # noqa: E501


class ClientInfo(TypedDict, total=False):
    """Identity of the integrating application, declared on ``server.connect``.

    Declaring it lets the telemetry the runtime emits on this connection be
    attributed to a single, consistent surface (the application and its Copilot
    integration) instead of the runtime's own build. All fields are optional;
    omit any of them (or the whole object) to keep the default attribution.
    """

    application_name: str
    """Name of the application using the SDK, e.g. ``"acme-developer-portal"``."""
    application_version: str
    """Version of the application using the SDK, e.g. ``"2.4.0"``."""
    integration_name: str
    """Optional name of an application integration, such as an extension or plugin."""
    integration_version: str
    """Optional version of the integration named by ``integration_name``."""


def _client_info_to_wire(client_info: ClientInfo | None) -> dict[str, str] | None:
    """Map a snake_case :class:`ClientInfo` onto the camelCase connect wire shape.

    Empty fields are dropped. Returns ``None`` when no field carries a non-empty
    value so the caller omits the ``clientInfo`` field entirely and keeps the
    runtime's default attribution.
    """
    if not client_info:
        return None
    wire: dict[str, str] = {}
    if client_info.get("application_name"):
        wire["editorName"] = client_info["application_name"]
    if client_info.get("application_version"):
        wire["editorVersion"] = client_info["application_version"]
    if client_info.get("integration_name"):
        wire["extensionName"] = client_info["integration_name"]
    if client_info.get("integration_version"):
        wire["extensionVersion"] = client_info["integration_version"]
    return wire or None


class NetworkConfig(TypedDict):
    """**Experimental.** Network callbacks through which the SDK host takes over parts of the
    runtime's networking.

    For example when the runtime runs in WSL or a container and should use VS Code's proxy
    settings and the host's Kerberos tickets. Each configured callback replaces the runtime's own
    handling for the traffic in ``scope``; without it, the runtime handles that part itself.

    Callbacks may be sync or async and run on the client's event loop, so offload a blocking
    lookup, e.g. with :func:`asyncio.to_thread`. An awaitable a callback returns is cancelled with
    :class:`asyncio.CancelledError` when the runtime stops waiting: it cancels the request (for
    example when its wait times out) or the connection closes. The SDK then ignores any later
    result.
    """

    scope: NotRequired[NetworkScope | Literal["connection", "runtime"]]
    """``"connection"`` (default) serves only requests made for sessions this client creates or
    resumes; ``"runtime"`` also serves the runtime's shared background traffic. Only one
    connection at a time can hold ``"runtime"``, so startup fails if another client holds it.
    The registration ends when the connection closes.
    """
    resolve_proxy: NotRequired[
        Callable[[NetworkResolveProxyRequest], str | None | Awaitable[str | None]]
    ]
    """Returns the proxies for ``request.url`` as a PAC-style list such as
    ``PROXY proxy.example:8080; HTTPS proxy.example:443; DIRECT`` (the format of Electron's
    ``session.resolveProxy``); return ``DIRECT`` for a direct connection, or ``None`` to let the
    runtime resolve the proxy itself, as if no callback were registered. Entry types the runtime
    doesn't support, such as SOCKS and QUIC, are skipped. ``request.url`` is the request target
    as a PAC script sees it: without credentials or fragment, and for ``https`` and ``wss`` also
    without path and query.

    The callback decides every request that would otherwise follow ambient proxy settings: proxy
    environment variables, ``NO_PROXY`` and operating-system proxy settings are not consulted
    unless it returns ``None``, and a failing callback fails the request rather than connecting
    directly. Requests to loopback targets, and requests with an explicitly configured proxy,
    never consult it. The runtime caches answers, including ``None``, for a few seconds.
    """
    authenticate_negotiate: NotRequired[
        Callable[[NetworkAuthenticateNegotiateRequest], str | None | Awaitable[str | None]]
    ]
    """Returns a fresh, single-use base64 SPNEGO token without the ``Negotiate `` prefix for a
    proxy's Negotiate challenge, or ``None`` when unavailable. The runtime then does not look up
    Kerberos credentials itself, so a missing token or a failing callback fails the request.

    The SPN uses the platform-independent ``service/host[@REALM]`` form; validate it before
    issuing credentials. Never log or persist tokens.
    """


def _network_registration(config: NetworkConfig) -> NetworkSetProviderRequest | None:
    """Validate ``config`` and build its registration, or ``None`` when it has no callbacks."""
    scope_value = config.get("scope")
    try:
        scope = None if scope_value is None else NetworkScope(scope_value)
    except ValueError:
        raise ValueError("network.scope must be either 'connection' or 'runtime'") from None
    resolve_proxy = config.get("resolve_proxy")
    if resolve_proxy is not None and not callable(resolve_proxy):
        raise ValueError("network.resolve_proxy must be callable")
    authenticate_negotiate = config.get("authenticate_negotiate")
    if authenticate_negotiate is not None and not callable(authenticate_negotiate):
        raise ValueError("network.authenticate_negotiate must be callable")
    if resolve_proxy is None and authenticate_negotiate is None:
        return None
    return NetworkSetProviderRequest(
        scope=scope,
        resolve_proxy=None if resolve_proxy is None else NetworkResolveProxyOptions(),
        authenticate_negotiate=(
            None if authenticate_negotiate is None else NetworkAuthenticateNegotiateOptions()
        ),
    )


@dataclass
class RuntimeConnection:
    """Discriminated config describing how to reach the Copilot runtime.

    Construct via the static factories :meth:`for_stdio`, :meth:`for_tcp`,
    or :meth:`for_uri`. Each factory returns the matching subclass;
    pattern-match on the subclass (or :func:`isinstance`) to branch on the
    transport.

    Example:
        >>> CopilotClient()  # default: stdio with the bundled runtime
        >>> CopilotClient(connection=RuntimeConnection.for_uri("localhost:3000"))
    """

    @staticmethod
    def for_stdio(
        *,
        path: str | None = None,
        args: Sequence[str] = (),
    ) -> StdioRuntimeConnection:
        """Spawn a runtime child process and communicate over its stdin/stdout.

        This is the default when no :attr:`CopilotClientOptions.connection`
        is supplied.

        Args:
            path: Path to the runtime executable. When ``None``, uses the
                bundled binary.
            args: Extra command-line arguments passed to the runtime process.
        """
        return StdioRuntimeConnection(path=path, args=tuple(args))

    @staticmethod
    def for_tcp(
        *,
        port: int = 0,
        connection_token: str | None = None,
        path: str | None = None,
        args: Sequence[str] = (),
    ) -> TcpRuntimeConnection:
        """Spawn a runtime child process listening on a TCP socket.

        Args:
            port: TCP port to listen on. ``0`` (the default) auto-allocates
                a free port. If the chosen port is already in use, startup
                fails.
            connection_token: Optional shared secret the SDK sends to the
                spawned runtime to authenticate the TCP connection. When
                ``None``, a UUID is generated automatically so the loopback
                listener is safe by default.
            path: Path to the runtime executable. When ``None``, uses the
                bundled binary.
            args: Extra command-line arguments passed to the runtime process.
        """
        return TcpRuntimeConnection(
            path=path,
            args=tuple(args),
            port=port,
            connection_token=connection_token,
        )

    @staticmethod
    def for_uri(url: str, *, connection_token: str | None = None) -> UriRuntimeConnection:
        """Connect to an already-running runtime at the given URL.

        Args:
            url: URL of the runtime to connect to. Accepts ``"port"``,
                ``"host:port"``, or a full URL.
            connection_token: Optional shared secret to authenticate the
                connection. Required when the server was started with a
                token; ignored by legacy servers without ``connect`` support.
        """
        return UriRuntimeConnection(url=url, connection_token=connection_token)

    @staticmethod
    def for_inprocess(
        *,
        run_on_ui_thread: Callable[[Callable[[], None]], None | Awaitable[None]] | None = None,
    ) -> InProcessRuntimeConnection:
        """Host the runtime **in-process** via its native C ABI (FFI).

        **Experimental.** The in-process (FFI) transport is experimental and its
        behavior may change or be removed in a future release.

        Instead of spawning the runtime as a child process, the SDK loads the
        runtime's native shared library into this process and drives JSON-RPC
        over its C ABI.

        Typed authentication and ``base_directory`` are scoped to this host.
        Client-level ``env``, ``telemetry`` and ``working_directory`` are
        unsupported; set working directories on individual sessions.
        Set ``COPILOT_CLI_PATH`` only when using an externally provisioned
        compatible runtime package.

        Args:
            run_on_ui_thread: Native UI scheduler, required for OneAuth interactive
                authentication on macOS. See
                :attr:`InProcessRuntimeConnection.run_on_ui_thread` for its
                main-thread execution contract and other uses.

        Note:
            Pre-provision the native runtime with
            ``python -m copilot download-runtime --in-process`` when automatic
            downloads are disabled.
        """
        return InProcessRuntimeConnection(run_on_ui_thread=run_on_ui_thread)


@dataclass
class ChildProcessRuntimeConnection(RuntimeConnection):
    """Base for :class:`RuntimeConnection` variants that spawn a runtime child process.

    Construct via :meth:`RuntimeConnection.for_stdio` or :meth:`RuntimeConnection.for_tcp`.
    """

    path: str | None = None
    """Path to the runtime executable. ``None`` uses the bundled binary."""

    args: Sequence[str] = ()
    """Extra command-line arguments passed to the runtime process."""

    env: dict[str, str] | None = None
    """Per-connection environment variables for the spawned child process.

    When set, this mapping replaces the inherited process environment instead
    of extending it, so include ``PATH`` and anything else the runtime needs
    (the SDK still adds the variables it manages, such as the auth token).
    Do not also set :attr:`CopilotClientOptions.env` — the client rejects
    setting environment in both places. ``None`` inherits the client-level
    env (or the current process env)."""


@dataclass
class StdioRuntimeConnection(ChildProcessRuntimeConnection):
    """Spawns a runtime child process and communicates over its stdin/stdout.

    Construct via :meth:`RuntimeConnection.for_stdio`.
    """


@dataclass
class TcpRuntimeConnection(ChildProcessRuntimeConnection):
    """Spawns a runtime child process listening on a TCP socket.

    Construct via :meth:`RuntimeConnection.for_tcp`.
    """

    port: int = 0
    """TCP port to listen on. ``0`` (the default) auto-allocates a free port."""

    connection_token: str | None = None
    """Shared secret the SDK sends to the spawned runtime. ``None`` auto-generates one."""


@dataclass
class UriRuntimeConnection(RuntimeConnection):
    """Connects to an already-running runtime at the specified URL.

    Construct via :meth:`RuntimeConnection.for_uri`.
    """

    url: str = ""
    """URL of the runtime to connect to. Accepts ``"port"``, ``"host:port"``, or a full URL."""

    connection_token: str | None = None
    """Shared secret to authenticate the connection."""


@dataclass
class InProcessRuntimeConnection(RuntimeConnection):
    """Hosts the runtime in-process via its native C ABI (FFI).

    **Experimental.** The in-process (FFI) transport is experimental and its
    behavior may change or be removed in a future release.

    Construct via :meth:`RuntimeConnection.for_inprocess`. The runtime's native
    shared library is loaded into this process and JSON-RPC is driven over its
    C ABI.
    """

    run_on_ui_thread: Callable[[Callable[[], None]], None | Awaitable[None]] | None = None
    """Experimental: schedule the supplied native action exactly once on the UI
    thread (the process main thread on macOS), completing only after it returns.
    The action can block while native UI is open. No authentication data crosses
    this callback. Other in-process clients may share this process-wide capability.

    Required for OneAuth interactive authentication on macOS. This general-purpose
    scheduler may also be used for other runtime-owned native UI operations.
    Temporary OneAuth workaround; this API may be removed if no longer needed."""


class _UiThreadAdapter:
    def __init__(self, client: CopilotClient) -> None:
        self._client = client

    async def run(self, params: UIThreadRunRequest) -> None:
        connection = self._client._connection
        host = self._client._ffi_host
        if (
            not isinstance(connection, InProcessRuntimeConnection)
            or connection.run_on_ui_thread is None
            or host is None
        ):
            raise RuntimeError("No in-process run_on_ui_thread handler is configured.")
        if not 0 <= params.action_id <= 0xFFFFFFFF:
            raise ValueError("UI action ID must be an unsigned 32-bit integer.")
        result = connection.run_on_ui_thread(lambda: host.run_ui_action(params.action_id))
        if inspect.isawaitable(result):
            await result


class _GitHubTelemetryAdapter:
    """Adapts a user-provided ``on_github_telemetry`` callback to the generated
    ``GitHubTelemetryHandler`` protocol.
    """

    def __init__(
        self,
        callback: Callable[[GitHubTelemetryNotification], None | Awaitable[None]],
    ) -> None:
        self._callback = callback

    async def event(self, params: GitHubTelemetryNotification) -> None:
        try:
            result = self._callback(params)
            if inspect.isawaitable(result):
                await result
        except Exception:
            logger.warning("Error handling gitHubTelemetry.event notification", exc_info=True)


class _CancellableCallbackAdapter:
    """Runs client callbacks for runtime requests whose handlers are registered as cancellable.

    An awaitable a callback returns is cancelled when the runtime cancels the request
    (``$/cancelRequest``) or the connection closes; requests after the close fail.
    """

    def __init__(self) -> None:
        # Each in-flight callback with the future that ends its wait when the connection closes.
        self._pending: set[tuple[asyncio.Future[object], asyncio.Future[None]]] = set()
        self._closed = False

    def cancel_pending(self) -> None:
        """Cancel in-flight callbacks because the connection closed; later requests fail."""
        self._closed = True
        pending = list(self._pending)
        self._pending.clear()
        for lookup, closed in pending:
            lookup.cancel()
            if not closed.done():
                closed.set_result(None)

    async def _run_callback(self, label: str, invoke: Callable[[], object]) -> object:
        if self._closed:
            raise RuntimeError(f"{label} connection closed")
        try:
            result = invoke()
        except asyncio.CancelledError:
            # A synchronous call cannot receive the request's cancellation, so this came from the
            # callback itself. The dispatcher answers only the request's own cancellation; a
            # propagated CancelledError would leave the runtime waiting for its deadline.
            raise RuntimeError(f"{label} request cancelled") from None
        if inspect.isawaitable(result):
            result = await self._await_callback(label, result)
        return result

    async def _await_callback(self, label: str, awaitable: Awaitable[object]) -> object:
        lookup = asyncio.ensure_future(awaitable)
        # Mark a failure as retrieved, so the late failure of a callback that ignored its
        # cancellation is not reported as an unhandled task exception.
        lookup.add_done_callback(lambda done: done.cancelled() or done.exception())
        closed: asyncio.Future[None] = asyncio.get_running_loop().create_future()
        entry = (lookup, closed)
        self._pending.add(entry)
        try:
            # Unlike ``await lookup``, this raises CancelledError only when the request itself
            # is cancelled, not when the callback's own awaitable is. A closed connection ends
            # the wait even when the callback suppresses its cancellation.
            await asyncio.wait({lookup, closed}, return_when=asyncio.FIRST_COMPLETED)
        except BaseException:
            lookup.cancel()
            raise
        finally:
            self._pending.discard(entry)
            closed.cancel()
        if self._closed or not lookup.done():
            # Discard the late result of a callback that ignored its cancellation, or that
            # answered it in the same turn as the close.
            raise RuntimeError(f"{label} connection closed")
        if lookup.cancelled():
            # The dispatcher answers only the request's own cancellation; a propagated
            # CancelledError would leave the runtime waiting for its deadline.
            raise RuntimeError(f"{label} request cancelled")
        return lookup.result()


class _NetworkAdapter(_CancellableCallbackAdapter):
    """Adapts ``network`` callbacks to the generated ``NetworkHandler`` protocol."""

    def __init__(self, config: NetworkConfig) -> None:
        super().__init__()
        self._resolve_proxy = config.get("resolve_proxy")
        self._authenticate_negotiate = config.get("authenticate_negotiate")

    async def resolve_proxy(self, params: NetworkResolveProxyRequest) -> NetworkResolveProxyResult:
        callback = self._resolve_proxy
        if callback is None:
            raise RuntimeError("No network resolve_proxy callback registered")
        proxy = await self._run_callback("Proxy resolution", lambda: callback(params))
        return NetworkResolveProxyResult(proxy=cast(str | None, proxy))

    async def authenticate_negotiate(
        self, params: NetworkAuthenticateNegotiateRequest
    ) -> NetworkAuthenticateNegotiateResult:
        callback = self._authenticate_negotiate
        if callback is None:
            raise RuntimeError("No network authenticate_negotiate callback registered")
        token = await self._run_callback("Negotiate authentication", lambda: callback(params))
        return NetworkAuthenticateNegotiateResult(token=cast(str | None, token))

    async def handle_resolve_proxy(self, params: dict) -> dict:
        """Wire-level ``network.resolveProxy`` handler, registered as cancellable."""
        result = await self.resolve_proxy(NetworkResolveProxyRequest.from_dict(params))
        return result.to_dict()

    async def handle_authenticate_negotiate(self, params: dict) -> dict:
        """Wire-level ``network.authenticateNegotiate`` handler, registered as cancellable."""
        result = await self.authenticate_negotiate(
            NetworkAuthenticateNegotiateRequest.from_dict(params)
        )
        return result.to_dict()


@dataclass
class _GitHubTokenProviderRegistration:
    provider: GitHubTokenProvider
    session_id: str | None = None
    committed: bool = False


class _GitHubTokenProviderAdapter:
    """Routes global GitHub token requests to opaque session registrations."""

    def __init__(self, client: CopilotClient) -> None:
        self._client = client

    async def get_token(self, params: GitHubTokenAcquireRequest) -> GitHubTokenAcquireResult:
        with self._client._github_token_providers_lock:
            registration = self._client._github_token_providers.get(params.registration_id)
        if registration is None:
            raise JsonRpcError(
                -32603,
                "No GitHub token provider registered for registration ID "
                f"{params.registration_id!r}",
            )

        result = registration.provider(
            GitHubTokenProviderArgs(
                host=params.host,
                session_id=params.session_id or registration.session_id,
                reason=params.reason,
            )
        )
        if inspect.isawaitable(result):
            result = await result
        # The generated global-handler wrapper forwards callback results directly,
        # so the public tagged dictionary is already in the expected wire shape.
        return cast(GitHubTokenAcquireResult, result)


class _HooksAdapter:
    """Adapts session-scoped hook dispatch to the generated ``HooksHandler`` protocol.

    ``hooks.invoke`` is a client-global RPC method whose payload carries a
    ``sessionId``. This adapter routes each invocation to the matching session's
    registered hook handlers.
    """

    def __init__(self, get_session: Callable[[str], CopilotSession | None]) -> None:
        self._get_session = get_session

    async def invoke(self, params: _HookInvokeRequest) -> _HookInvokeResponse:
        session = self._get_session(params.session_id)
        if session is None:
            raise ValueError(f"unknown session {params.session_id}")
        output = await session._handle_hooks_invoke(params.hook_type.value, params.input)
        return _HookInvokeResponse(output=output)


@dataclass
class _InternalTestOptions:
    """SDK test infrastructure only; unsupported and subject to change.

    Supplied values override the owned runtime's startup environment, never
    the host process environment. None preserves the inherited value.
    """

    copilot_api_url: str | None = None
    github_api_url: str | None = None
    proxy_url: str | None = None
    no_proxy: str | None = None
    ca_certificate_path: str | None = None

    def _to_environment(self) -> dict[str, str]:
        return {
            key: value
            for key, value in {
                "COPILOT_API_URL": self.copilot_api_url,
                "COPILOT_DEBUG_GITHUB_API_URL": self.github_api_url,
                "HTTP_PROXY": self.proxy_url,
                "HTTPS_PROXY": self.proxy_url,
                "http_proxy": self.proxy_url,
                "https_proxy": self.proxy_url,
                "NO_PROXY": self.no_proxy,
                "no_proxy": self.no_proxy,
                "NODE_EXTRA_CA_CERTS": self.ca_certificate_path,
                "SSL_CERT_FILE": self.ca_certificate_path,
                "REQUESTS_CA_BUNDLE": self.ca_certificate_path,
                "CURL_CA_BUNDLE": self.ca_certificate_path,
                "GIT_SSL_CAINFO": self.ca_certificate_path,
            }.items()
            if value is not None
        }


@dataclass
class _CopilotClientOptions:
    """Internal configuration carrier used by :class:`CopilotClient`.

    This is not part of the public API: ``CopilotClient`` accepts all of
    these options as keyword arguments directly.
    """

    entra: EntraAuthConfig | None = None
    internal_test_options: _InternalTestOptions | None = None
    connection: RuntimeConnection | None = None
    working_directory: str | None = None
    log_level: LogLevel = "info"
    env: dict[str, str] | None = None
    github_token: str | None = None
    base_directory: str | None = None
    builtin_plugin_directories: tuple[str, ...] = ()
    extension_launch_provider: ExtensionLaunchProviderHandler | None = None
    installation_confirmation_handler: InstallationConfirmationHandler | None = None
    network: NetworkConfig | None = None
    use_logged_in_user: bool | None = None
    telemetry: TelemetryConfig | None = None
    session_fs: SessionFsConfig | None = None
    request_handler: CopilotRequestHandler | None = None
    session_idle_timeout_seconds: int | None = None
    client_info: ClientInfo | None = None
    on_list_models: Callable[[], list[ModelInfo] | Awaitable[list[ModelInfo]]] | None = None
    on_github_telemetry: Callable[[GitHubTelemetryNotification], None | Awaitable[None]] | None = (
        None
    )
    mode: CopilotClientMode = "copilot-cli"


# ============================================================================
# Response Types
# ============================================================================


@dataclass
class PingResponse:
    """Response from ping"""

    message: str  # Echo message with "pong: " prefix
    timestamp: datetime  # Timestamp when the ping was processed
    protocol_version: int  # Protocol version for SDK compatibility

    @staticmethod
    def from_dict(obj: Any) -> PingResponse:
        assert isinstance(obj, dict)
        message = obj.get("message")
        timestamp = obj.get("timestamp")
        protocol_version = obj.get("protocolVersion")
        if message is None or timestamp is None or protocol_version is None:
            raise ValueError(
                f"Missing required fields in PingResponse: message={message}, "
                f"timestamp={timestamp}, protocolVersion={protocol_version}"
            )
        timestamp_value = (
            datetime.fromtimestamp(timestamp / 1000, tz=UTC)
            if isinstance(timestamp, (int, float))
            else from_datetime(timestamp)
        )
        return PingResponse(str(message), timestamp_value, int(protocol_version))

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = self.message
        result["timestamp"] = self.timestamp.isoformat()
        result["protocolVersion"] = self.protocol_version
        return result


@dataclass
class StopError(Exception):
    """Error that occurred during client stop cleanup."""

    message: str  # Error message describing what failed during cleanup

    def __post_init__(self) -> None:
        Exception.__init__(self, self.message)

    @staticmethod
    def from_dict(obj: Any) -> StopError:
        assert isinstance(obj, dict)
        message = obj.get("message")
        if message is None:
            raise ValueError("Missing required field 'message' in StopError")
        return StopError(str(message))

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = self.message
        return result


@dataclass
class GetStatusResponse:
    """Response from status.get"""

    version: str  # Package version (e.g., "1.0.0")
    protocol_version: int  # Protocol version for SDK compatibility
    host_diagnostics: HostDiagnostics | None = field(default=None, kw_only=True)

    @staticmethod
    def from_dict(obj: Any) -> GetStatusResponse:
        assert isinstance(obj, dict)
        version = obj.get("version")
        protocol_version = obj.get("protocolVersion")
        if version is None or protocol_version is None:
            raise ValueError(
                f"Missing required fields in GetStatusResponse: version={version}, "
                f"protocolVersion={protocol_version}"
            )
        host_diagnostics = obj.get("hostDiagnostics")
        return GetStatusResponse(
            str(version),
            int(protocol_version),
            host_diagnostics=(
                HostDiagnostics.from_dict(host_diagnostics)
                if host_diagnostics is not None
                else None
            ),
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["version"] = self.version
        result["protocolVersion"] = self.protocol_version
        if self.host_diagnostics is not None:
            result["hostDiagnostics"] = self.host_diagnostics.to_dict()
        return result


@dataclass
class GetAuthStatusResponse:
    """Response from auth.getStatus"""

    isAuthenticated: bool  # Whether the user is authenticated
    authType: str | None = None  # Authentication type
    host: str | None = None  # GitHub host URL
    login: str | None = None  # User login name
    statusMessage: str | None = None  # Human-readable status message

    @staticmethod
    def from_dict(obj: Any) -> GetAuthStatusResponse:
        assert isinstance(obj, dict)
        isAuthenticated = obj.get("isAuthenticated")
        if isAuthenticated is None:
            raise ValueError("Missing required field 'isAuthenticated' in GetAuthStatusResponse")
        authType = obj.get("authType")
        host = obj.get("host")
        login = obj.get("login")
        statusMessage = obj.get("statusMessage")
        return GetAuthStatusResponse(
            isAuthenticated=bool(isAuthenticated),
            authType=authType,
            host=host,
            login=login,
            statusMessage=statusMessage,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["isAuthenticated"] = self.isAuthenticated
        if self.authType is not None:
            result["authType"] = self.authType
        if self.host is not None:
            result["host"] = self.host
        if self.login is not None:
            result["login"] = self.login
        if self.statusMessage is not None:
            result["statusMessage"] = self.statusMessage
        return result


# ============================================================================
# Model Types
# ============================================================================


@dataclass
class ModelVisionLimits:
    """Vision-specific limits"""

    supported_media_types: list[str] | None = None
    max_prompt_images: int | None = None
    max_prompt_image_size: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> ModelVisionLimits:
        assert isinstance(obj, dict)
        supported_media_types = obj.get("supported_media_types")
        max_prompt_images = obj.get("max_prompt_images")
        max_prompt_image_size = obj.get("max_prompt_image_size")
        return ModelVisionLimits(
            supported_media_types=supported_media_types,
            max_prompt_images=max_prompt_images,
            max_prompt_image_size=max_prompt_image_size,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.supported_media_types is not None:
            result["supported_media_types"] = self.supported_media_types
        if self.max_prompt_images is not None:
            result["max_prompt_images"] = self.max_prompt_images
        if self.max_prompt_image_size is not None:
            result["max_prompt_image_size"] = self.max_prompt_image_size
        return result


@dataclass
class ModelLimits:
    """Model limits"""

    max_prompt_tokens: int | None = None
    max_context_window_tokens: int | None = None
    vision: ModelVisionLimits | None = None
    # Last, so existing positional constructor calls keep their meaning.
    max_output_tokens: int | None = None

    @staticmethod
    def from_dict(obj: Any) -> ModelLimits:
        assert isinstance(obj, dict)
        max_prompt_tokens = obj.get("max_prompt_tokens")
        max_output_tokens = obj.get("max_output_tokens")
        max_context_window_tokens = obj.get("max_context_window_tokens")
        vision_dict = obj.get("vision")
        vision = ModelVisionLimits.from_dict(vision_dict) if vision_dict else None
        return ModelLimits(
            max_prompt_tokens=max_prompt_tokens,
            max_output_tokens=max_output_tokens,
            max_context_window_tokens=max_context_window_tokens,
            vision=vision,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.max_prompt_tokens is not None:
            result["max_prompt_tokens"] = self.max_prompt_tokens
        if self.max_output_tokens is not None:
            result["max_output_tokens"] = self.max_output_tokens
        if self.max_context_window_tokens is not None:
            result["max_context_window_tokens"] = self.max_context_window_tokens
        if self.vision is not None:
            result["vision"] = self.vision.to_dict()
        return result


@dataclass
class ModelSupports:
    """Model support flags"""

    vision: bool = False
    reasoning_effort: bool = False  # Whether this model supports reasoning effort

    @staticmethod
    def from_dict(obj: Any) -> ModelSupports:
        assert isinstance(obj, dict)
        vision = obj.get("vision", False)
        reasoning_effort = obj.get("reasoningEffort", False)
        return ModelSupports(vision=bool(vision), reasoning_effort=bool(reasoning_effort))

    def to_dict(self) -> dict:
        result: dict = {}
        result["vision"] = self.vision
        result["reasoningEffort"] = self.reasoning_effort
        return result


@dataclass
class ModelCapabilities:
    """Model capabilities and limits"""

    supports: ModelSupports
    limits: ModelLimits

    @staticmethod
    def from_dict(obj: Any) -> ModelCapabilities:
        assert isinstance(obj, dict)
        supports_dict = obj.get("supports")
        limits_dict = obj.get("limits")
        supports = ModelSupports.from_dict(supports_dict) if supports_dict else ModelSupports()
        limits = ModelLimits.from_dict(limits_dict) if limits_dict else ModelLimits()
        return ModelCapabilities(supports=supports, limits=limits)

    def to_dict(self) -> dict:
        result: dict = {}
        result["supports"] = self.supports.to_dict()
        result["limits"] = self.limits.to_dict()
        return result


@dataclass
class ModelPolicy:
    """Model policy state"""

    state: str  # "enabled", "disabled", or "unconfigured"
    terms: str

    @staticmethod
    def from_dict(obj: Any) -> ModelPolicy:
        assert isinstance(obj, dict)
        state = obj.get("state")
        terms = obj.get("terms")
        if state is None or terms is None:
            raise ValueError(
                f"Missing required fields in ModelPolicy: state={state}, terms={terms}"
            )
        return ModelPolicy(state=str(state), terms=str(terms))

    def to_dict(self) -> dict:
        result: dict = {}
        result["state"] = self.state
        result["terms"] = self.terms
        return result


@dataclass
class ModelBilling:
    """Model billing information"""

    multiplier: float | None = None
    token_prices: ModelBillingTokenPrices | None = None

    @staticmethod
    def from_dict(obj: Any) -> ModelBilling:
        assert isinstance(obj, dict)
        multiplier = obj.get("multiplier")
        tp = obj.get("tokenPrices")
        token_prices = ModelBillingTokenPrices.from_dict(tp) if tp is not None else None
        return ModelBilling(
            multiplier=float(multiplier) if multiplier is not None else None,
            token_prices=token_prices,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.multiplier is not None:
            result["multiplier"] = self.multiplier
        if self.token_prices is not None:
            result["tokenPrices"] = self.token_prices.to_dict()
        return result


@dataclass
class ModelInfo:
    """Information about an available model"""

    id: str  # Model identifier (e.g., "claude-sonnet-4.5")
    name: str  # Display name
    capabilities: ModelCapabilities  # Model capabilities and limits
    policy: ModelPolicy | None = None  # Policy state
    billing: ModelBilling | None = None  # Billing information
    # Supported reasoning effort levels (only present if model supports reasoning effort)
    supported_reasoning_efforts: list[str] | None = None
    # Default reasoning effort level (only present if model supports reasoning effort)
    default_reasoning_effort: str | None = None
    # Native serving provider; absent for unattributed and configured BYOK models.
    provider: ModelProviderRef | None = None

    @staticmethod
    def from_dict(obj: Any) -> ModelInfo:
        assert isinstance(obj, dict)
        id = obj.get("id")
        name = obj.get("name")
        capabilities_dict = obj.get("capabilities")
        if id is None or name is None or capabilities_dict is None:
            raise ValueError(
                f"Missing required fields in ModelInfo: id={id}, name={name}, "
                f"capabilities={capabilities_dict}"
            )
        capabilities = ModelCapabilities.from_dict(capabilities_dict)
        policy_dict = obj.get("policy")
        policy = ModelPolicy.from_dict(policy_dict) if policy_dict else None
        billing_dict = obj.get("billing")
        billing = ModelBilling.from_dict(billing_dict) if billing_dict else None
        supported_reasoning_efforts = obj.get("supportedReasoningEfforts")
        default_reasoning_effort = obj.get("defaultReasoningEffort")
        provider_dict = obj.get("provider")
        return ModelInfo(
            id=str(id),
            name=str(name),
            capabilities=capabilities,
            policy=policy,
            billing=billing,
            supported_reasoning_efforts=supported_reasoning_efforts,
            default_reasoning_effort=default_reasoning_effort,
            provider=ModelProviderRef.from_dict(provider_dict)
            if provider_dict is not None
            else None,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = self.id
        result["name"] = self.name
        result["capabilities"] = self.capabilities.to_dict()
        if self.provider is not None:
            result["provider"] = self.provider.to_dict()
        if self.policy is not None:
            result["policy"] = self.policy.to_dict()
        if self.billing is not None:
            result["billing"] = self.billing.to_dict()
        if self.supported_reasoning_efforts is not None:
            result["supportedReasoningEfforts"] = self.supported_reasoning_efforts
        if self.default_reasoning_effort is not None:
            result["defaultReasoningEffort"] = self.default_reasoning_effort
        return result


# ============================================================================
# Session Metadata Types
# ============================================================================


@dataclass
class SessionContext:
    """Working directory context for a session"""

    working_directory: str  # Working directory where the session was created
    git_root: str | None = None  # Git repository root (if in a git repo)
    repository: str | None = None  # GitHub repository in "owner/repo" format
    branch: str | None = None  # Current git branch

    @staticmethod
    def from_dict(obj: Any) -> SessionContext:
        assert isinstance(obj, dict)
        cwd = obj.get("cwd")
        if cwd is None:
            raise ValueError("Missing required field 'cwd' in SessionContext")
        return SessionContext(
            working_directory=str(cwd),
            git_root=obj.get("gitRoot"),
            repository=obj.get("repository"),
            branch=obj.get("branch"),
        )

    def to_dict(self) -> dict:
        result: dict = {"cwd": self.working_directory}
        if self.git_root is not None:
            result["gitRoot"] = self.git_root
        if self.repository is not None:
            result["repository"] = self.repository
        if self.branch is not None:
            result["branch"] = self.branch
        return result


@dataclass
class SessionListFilter:
    """Filter options for listing sessions"""

    working_directory: str | None = None  # Filter by exact working directory match
    git_root: str | None = None  # Filter by git root
    repository: str | None = None  # Filter by repository (owner/repo format)
    branch: str | None = None  # Filter by branch

    def to_dict(self) -> dict:
        result: dict = {}
        if self.working_directory is not None:
            result["cwd"] = self.working_directory
        if self.git_root is not None:
            result["gitRoot"] = self.git_root
        if self.repository is not None:
            result["repository"] = self.repository
        if self.branch is not None:
            result["branch"] = self.branch
        return result


@dataclass
class SessionMetadata:
    """Metadata about a session"""

    session_id: str  # Session identifier
    start_time: datetime  # Timestamp when session was created
    modified_time: datetime  # Timestamp when session was last modified
    is_remote: bool  # Whether the session is remote
    summary: str | None = None  # Optional summary of the session
    context: SessionContext | None = None  # Working directory context
    # Name of the client that created the session (for example "github/cli").
    client_name: str | None = None
    # Name of the client that most recently ran the session: the client that
    # created it, or a client that resumed it and then ran it. Resuming a session
    # only to observe it does not change this. None when unknown, such as for
    # sessions persisted before this field existed.
    last_client_name: str | None = None

    @staticmethod
    def from_dict(obj: Any) -> SessionMetadata:
        assert isinstance(obj, dict)
        session_id = obj.get("sessionId")
        start_time = obj.get("startTime")
        modified_time = obj.get("modifiedTime")
        is_remote = obj.get("isRemote")
        if session_id is None or start_time is None or modified_time is None or is_remote is None:
            raise ValueError(
                f"Missing required fields in SessionMetadata: sessionId={session_id}, "
                f"startTime={start_time}, modifiedTime={modified_time}, isRemote={is_remote}"
            )
        summary = obj.get("summary")
        context_dict = obj.get("context")
        context = SessionContext.from_dict(context_dict) if context_dict else None
        return SessionMetadata(
            session_id=str(session_id),
            start_time=_parse_session_timestamp(start_time),
            modified_time=_parse_session_timestamp(modified_time),
            is_remote=bool(is_remote),
            summary=summary,
            context=context,
            client_name=obj.get("clientName"),
            last_client_name=obj.get("lastClientName"),
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = self.session_id
        result["startTime"] = self.start_time.isoformat()
        result["modifiedTime"] = self.modified_time.isoformat()
        result["isRemote"] = self.is_remote
        if self.summary is not None:
            result["summary"] = self.summary
        if self.context is not None:
            result["context"] = self.context.to_dict()
        if self.client_name is not None:
            result["clientName"] = self.client_name
        if self.last_client_name is not None:
            result["lastClientName"] = self.last_client_name
        return result


def _parse_session_timestamp(value: Any) -> datetime:
    """Parse a wire-format timestamp into ``datetime``.

    Accepts either an ISO-8601 string (server-sent JSON) or an existing
    ``datetime`` (round-tripped from a previous parse). Returns the value
    as-is if it's already a ``datetime``.
    """
    if isinstance(value, datetime):
        return value
    return from_datetime(value)


# ============================================================================
# Session Lifecycle Types (for TUI+server mode)
# ============================================================================

SessionLifecycleEventType = Literal[
    "session.created",
    "session.deleted",
    "session.updated",
    "session.foreground",
    "session.background",
]


@dataclass
class SessionLifecycleEventMetadata:
    """Metadata for session lifecycle events."""

    start_time: datetime
    modified_time: datetime
    summary: str | None = None

    @staticmethod
    def from_dict(data: dict) -> SessionLifecycleEventMetadata:
        return SessionLifecycleEventMetadata(
            start_time=_parse_session_timestamp(data.get("startTime", "")),
            modified_time=_parse_session_timestamp(data.get("modifiedTime", "")),
            summary=data.get("summary"),
        )


@dataclass
class SessionLifecycleEventBase:
    """Base for session lifecycle event variants.

    Construct concrete variants directly (e.g. :class:`SessionCreatedEvent`,
    :class:`SessionDeletedEvent`); pattern-match on the variant class to
    branch on the event kind.
    """

    session_id: str
    metadata: SessionLifecycleEventMetadata | None = None


@dataclass
class SessionCreatedEvent(SessionLifecycleEventBase):
    """Emitted when a session is created."""

    type: ClassVar[Literal["session.created"]] = "session.created"


@dataclass
class SessionDeletedEvent(SessionLifecycleEventBase):
    """Emitted when a session is deleted."""

    type: ClassVar[Literal["session.deleted"]] = "session.deleted"


@dataclass
class SessionUpdatedEvent(SessionLifecycleEventBase):
    """Emitted when a session is updated (summary/title/etc. changed)."""

    type: ClassVar[Literal["session.updated"]] = "session.updated"


@dataclass
class SessionForegroundEvent(SessionLifecycleEventBase):
    """Emitted when a session moves to the foreground (TUI+server mode)."""

    type: ClassVar[Literal["session.foreground"]] = "session.foreground"


@dataclass
class SessionBackgroundEvent(SessionLifecycleEventBase):
    """Emitted when a session moves to the background (TUI+server mode)."""

    type: ClassVar[Literal["session.background"]] = "session.background"


SessionLifecycleEvent = (
    SessionCreatedEvent
    | SessionDeletedEvent
    | SessionUpdatedEvent
    | SessionForegroundEvent
    | SessionBackgroundEvent
)


def _session_lifecycle_event_from_dict(data: dict) -> SessionLifecycleEvent:
    """Construct the correct :class:`SessionLifecycleEvent` variant from a wire dict."""
    metadata = None
    if "metadata" in data and data["metadata"]:
        metadata = SessionLifecycleEventMetadata.from_dict(data["metadata"])
    session_id = data.get("sessionId", "")
    event_type = data.get("type")
    if event_type == "session.created":
        return SessionCreatedEvent(session_id=session_id, metadata=metadata)
    if event_type == "session.deleted":
        return SessionDeletedEvent(session_id=session_id, metadata=metadata)
    if event_type == "session.foreground":
        return SessionForegroundEvent(session_id=session_id, metadata=metadata)
    if event_type == "session.background":
        return SessionBackgroundEvent(session_id=session_id, metadata=metadata)
    # Default to ``session.updated`` for unknown event types so consumers
    # keep working across server upgrades.
    return SessionUpdatedEvent(session_id=session_id, metadata=metadata)


SessionLifecycleHandler = Callable[[SessionLifecycleEvent], None]

HandlerUnsubcribe = Callable[[], None]

# Minimum protocol version this SDK can communicate with.
# Servers reporting a version below this are rejected.
_MIN_PROTOCOL_VERSION = 3
_RUNTIME_SHUTDOWN_TIMEOUT_SECONDS = 10
_CLI_PROCESS_GRACEFUL_EXIT_TIMEOUT_SECONDS = 10
_CLI_PROCESS_EXIT_TIMEOUT_SECONDS = 5


def _extract_transform_callbacks(
    system_message: SystemMessageConfig | dict[str, Any] | None,
) -> tuple[dict[str, Any] | None, dict[str, SectionTransformFn] | None]:
    """Extract function-valued actions from system message config.

    Returns a wire-safe payload (with callable actions replaced by ``"transform"``)
    and a dict of transform callbacks keyed by section ID.
    """
    wire_system_message = cast(dict[str, Any] | None, system_message)
    if (
        not wire_system_message
        or wire_system_message.get("mode") != "customize"
        or not wire_system_message.get("sections")
    ):
        return wire_system_message, None

    callbacks: dict[str, SectionTransformFn] = {}
    wire_sections: dict[str, Any] = {}
    for section_id, override in wire_system_message["sections"].items():
        if not override:
            continue
        action = override.get("action")
        if callable(action):
            callbacks[section_id] = action
            wire_sections[section_id] = {"action": "transform"}
        else:
            wire_sections[section_id] = override

    if not callbacks:
        return wire_system_message, None

    wire_payload = {**wire_system_message, "sections": wire_sections}
    return wire_payload, callbacks


_DEFAULT_CONNECTION_ENV_VAR = "COPILOT_SDK_DEFAULT_CONNECTION"


def _session_ids_overlap(left: str, right: str) -> bool:
    if left == right:
        return True
    if not _is_uuid_session_id(left) or not _is_uuid_session_id(right):
        return False
    left, right = left.lower(), right.lower()
    return left.startswith(right) or right.startswith(left)


def _is_abbreviated_uuid_prefix(session_id: str) -> bool:
    return (
        7 <= len(session_id) < 36
        and all(character in "0123456789abcdefABCDEF" for character in session_id[:7])
        and all(character in "0123456789abcdefABCDEF-" for character in session_id[7:])
    )


def _is_uuid_session_id(session_id: str) -> bool:
    return _is_abbreviated_uuid_prefix(session_id) or (
        len(session_id) == 36
        and all(
            character == "-" if index in (8, 13, 18, 23) else character in "0123456789abcdefABCDEF"
            for index, character in enumerate(session_id)
        )
    )


class _SessionInitializationLock(asyncio.Lock):
    def __init__(self) -> None:
        super().__init__()
        self.users = 0
        self.inline_reservation: ConcurrentFuture[None] | None = None

    async def acquire(self) -> Literal[True]:
        reservation = self.inline_reservation
        if reservation is not None:
            await asyncio.shield(asyncio.wrap_future(reservation))
        return await super().acquire()

    def locked(self) -> bool:
        return self.inline_reservation is not None or super().locked()


class _SessionInitialization:
    def __init__(self, locks: list[_SessionInitializationLock]):
        self._locks = locks
        self.acquired = 0

    def release(self) -> None:
        for lock in reversed(self._locks[: self.acquired]):
            lock.release()
        for lock in self._locks:
            lock.users -= 1


def _resolve_default_connection(env: Mapping[str, str]) -> RuntimeConnection:
    """Resolve the transport when the caller supplies no explicit connection.

    Honors the ``COPILOT_SDK_DEFAULT_CONNECTION`` override (``"inprocess"`` or
    ``"stdio"``); defaults to stdio. Matches the Node/.NET/Rust default-transport
    override so the CI matrix can run the whole suite under either transport.
    """
    value = env.get(_DEFAULT_CONNECTION_ENV_VAR)
    if value is None or value == "":
        return RuntimeConnection.for_stdio()
    normalized = value.strip().lower()
    if normalized == "inprocess":
        return RuntimeConnection.for_inprocess()
    if normalized == "stdio":
        return RuntimeConnection.for_stdio()
    raise ValueError(
        f"Invalid {_DEFAULT_CONNECTION_ENV_VAR}={value!r}. Expected 'inprocess', 'stdio', or unset."
    )


def _validate_environment_options(
    options: _CopilotClientOptions, connection: RuntimeConnection
) -> None:
    """Validate env/telemetry/working-directory options against the transport.

    In-process hosts cannot accept arbitrary process-wide configuration.
    """
    if isinstance(connection, UriRuntimeConnection) and options.internal_test_options is not None:
        raise ValueError("_test_options requires a runtime owned by this client")
    if isinstance(connection, InProcessRuntimeConnection):
        if options.env is not None:
            raise ValueError(
                "env is not supported with RuntimeConnection.for_inprocess(): environment "
                "variables belong to the shared host process. Use typed client/session "
                "options or a child-process transport."
            )
        if options.telemetry is not None:
            raise ValueError(
                "telemetry is not supported with RuntimeConnection.for_inprocess(): "
                "telemetry configuration is lowered to environment variables read by "
                "native runtime code running in the shared host process, so per-client "
                "telemetry cannot be honored in-process. Configure telemetry via the "
                "host process environment, or use a child-process transport."
            )
        if options.working_directory is not None:
            raise ValueError(
                "working_directory is not supported with RuntimeConnection.for_inprocess(): "
                "the native runtime shares the host process working directory, so a "
                "per-client working directory cannot be honored in-process. Set working_directory "
                "on individual sessions instead, or use a child-process transport."
            )
        return

    if (
        isinstance(connection, ChildProcessRuntimeConnection)
        and connection.env is not None
        and options.env is not None
    ):
        raise ValueError(
            "Set environment variables via either the client-level env argument or "
            "ChildProcessRuntimeConnection.env, not both. Prefer the connection-level "
            "env for child-process transports."
        )


class CopilotClient:
    """
    Main client for interacting with the Copilot CLI.

    The CopilotClient manages the connection to the Copilot CLI server and provides
    methods to create and manage conversation sessions. It can either spawn a CLI
    server process or connect to an existing server.

    The client supports both stdio (default) and TCP transport modes for
    communication with the CLI server.

    Example:
        >>> # Create a client with default options (spawns CLI server)
        >>> client = CopilotClient()
        >>> await client.start()
        >>>
        >>> # Create a session and send a message
        >>> session = await client.create_session(
        ...     on_permission_request=PermissionHandler.approve_all,
        ...     model="gpt-5.4",
        ... )
        >>> session.on(lambda event: print(event.type))
        >>> await session.send("Hello!")
        >>>
        >>> # Clean up
        >>> await session.disconnect()
        >>> await client.stop()

        >>> # Or connect to an existing server
        >>> client = CopilotClient(
        ...     connection=RuntimeConnection.for_uri("localhost:3000"),
        ... )
    """

    def __init__(
        self,
        *,
        entra: EntraAuthConfig | None = None,
        _test_options: _InternalTestOptions | None = None,
        connection: RuntimeConnection | None = None,
        working_directory: str | None = None,
        log_level: LogLevel = "info",
        env: dict[str, str] | None = None,
        github_token: str | None = None,
        base_directory: str | None = None,
        builtin_plugin_directories: Sequence[str] | None = None,
        extension_launch_provider: ExtensionLaunchProviderHandler | None = None,
        installation_confirmation_handler: InstallationConfirmationHandler | None = None,
        network: NetworkConfig | None = None,
        use_logged_in_user: bool | None = None,
        telemetry: TelemetryConfig | None = None,
        session_fs: SessionFsConfig | None = None,
        request_handler: CopilotRequestHandler | None = None,
        session_idle_timeout_seconds: int | None = None,
        client_info: ClientInfo | None = None,
        on_list_models: Callable[[], list[ModelInfo] | Awaitable[list[ModelInfo]]] | None = None,
        on_github_telemetry: Callable[[GitHubTelemetryNotification], None | Awaitable[None]]
        | None = None,
        mode: CopilotClientMode = "copilot-cli",
    ):
        """
        Initialize a new CopilotClient.

        ``entra`` selects the host's Microsoft Entra registration for its sessions.
        Required for built-in Entra sign-in and token renewal;
        other authentication methods remain available.

        Runtime options apply to locally hosted connections. The in-process
        transport supports typed runtime options such as ``log_level``,
        ``github_token``, and ``base_directory``, but rejects per-client
        ``working_directory``, ``env``, and ``telemetry``. Options are ignored
        when connecting to an existing runtime via
        :meth:`RuntimeConnection.for_uri`.

        Args:
            entra: **Experimental.** Host Entra application and optional host-owned
                identity and token acquisition callback. May change or be removed.
            connection: How to reach the runtime. Defaults to
                :meth:`RuntimeConnection.for_stdio` with the bundled binary.
            working_directory: Working directory for the runtime process.
                ``None`` uses the current directory.
            log_level: Log level for the runtime process. Defaults to ``"info"``.
            env: Environment variables for the runtime process. ``None`` inherits
                the current env.
            github_token: GitHub token for authentication. Takes priority over
                other auth methods.
            base_directory: Base directory for Copilot data (session state,
                config, etc.). Sets the ``COPILOT_HOME`` environment variable on
                the spawned runtime. When ``None``, the runtime defaults to
                ``~/.copilot``. In-process hosts also keep their runtime cache
                in this directory's ``cache`` subdirectory.
            builtin_plugin_directories: Absolute paths to trusted plugin
                directories bundled by the host. When non-empty, the complete
                set is registered during startup before sessions can be created.
            extension_launch_provider: Connection-level extension launch profile
                provider. When set, it is registered during startup before any
                session can be created.
            installation_confirmation_handler: Experimental connection-global
                human review handler for ``installations.confirm``. Receives the
                typed request and independent request/connection cancellation
                signals, and returns only an explicit decision. This does not
                enable installation capabilities.
            network: **Experimental.** Network callbacks through which the SDK
                host decides the runtime's proxies and supplies its Kerberos
                tokens. The client installs the ``network.resolveProxy`` and
                ``network.authenticateNegotiate`` handlers and registers the
                configured callbacks and ``scope`` during startup before
                sessions can be created. May change or be removed.
            use_logged_in_user: Use the logged-in user for authentication.
                ``None`` (default) resolves to ``True`` unless ``github_token``
                is set. When ``False``, only explicit tokens such as
                ``github_token`` are used; environment variable tokens
                (``COPILOT_GITHUB_TOKEN``, ``GH_TOKEN``, ``GITHUB_TOKEN``) are
                ignored.
            telemetry: OpenTelemetry configuration. Providing this enables
                telemetry.
            session_fs: Connection-level session filesystem provider
                configuration.
            request_handler: Connection-level request handler. When set, the
                supplied handler services every model-layer HTTP/WebSocket
                request the runtime would otherwise issue (both BYOK and CAPI).
            session_idle_timeout_seconds: Server-wide session idle timeout in
                seconds. Sessions without activity for this duration are
                automatically cleaned up. Set to ``None`` or ``0`` to disable.
            client_info: Identity of the integrating application, forwarded to the
                runtime on the ``server.connect`` handshake. Declaring it lets
                the telemetry the runtime emits on this connection be attributed
                to a consistent surface instead of the runtime's own build. All
                fields are optional; omit it to keep the default attribution.
            on_list_models: Custom handler for :meth:`list_models`. When
                provided, the handler is called instead of querying the runtime
                server.
            on_github_telemetry: Internal. Callback invoked when the runtime
                forwards a GitHub telemetry event for a session. The callback
                may be sync or async. Registering a handler opts every session
                opened by this client into telemetry forwarding.

        Example:
            >>> # Default — spawns runtime using stdio with the bundled binary
            >>> client = CopilotClient()
            >>>
            >>> # Connect to an existing runtime
            >>> client = CopilotClient(
            ...     connection=RuntimeConnection.for_uri("localhost:3000"),
            ... )
            >>>
            >>> # Custom runtime path with specific log level
            >>> client = CopilotClient(
            ...     connection=RuntimeConnection.for_stdio(path="/usr/local/bin/copilot"),
            ...     log_level="debug",
            ... )
        """
        options = _CopilotClientOptions(
            entra=entra,
            internal_test_options=_test_options,
            connection=connection,
            working_directory=working_directory,
            log_level=log_level,
            env=env,
            github_token=github_token,
            base_directory=base_directory,
            builtin_plugin_directories=tuple(builtin_plugin_directories or ()),
            extension_launch_provider=extension_launch_provider,
            installation_confirmation_handler=installation_confirmation_handler,
            network=network,
            use_logged_in_user=use_logged_in_user,
            telemetry=telemetry,
            session_fs=session_fs,
            request_handler=request_handler,
            session_idle_timeout_seconds=session_idle_timeout_seconds,
            client_info=client_info,
            on_list_models=on_list_models,
            on_github_telemetry=on_github_telemetry,
            mode=mode,
        )
        connection = (
            options.connection
            if options.connection is not None
            else _resolve_default_connection(os.environ)
        )
        _validate_environment_options(options, connection)
        for path in options.builtin_plugin_directories:
            if not os.path.isabs(path):
                raise ValueError(
                    f"builtin_plugin_directories must contain only absolute paths: {path}"
                )
        # A snapshot, so later changes to the caller's dict cannot make the registration and the
        # request handlers disagree.
        self._network: NetworkConfig | None = (
            NetworkConfig(**options.network) if options.network is not None else None
        )
        self._network_registration = (
            _network_registration(self._network) if self._network is not None else None
        )
        _require_storage_for_empty_mode(
            mode=options.mode,
            base_directory=options.base_directory,
            session_fs_set=options.session_fs is not None,
            is_uri_connection=isinstance(connection, UriRuntimeConnection),
        )

        self._options: _CopilotClientOptions = options
        self._connection: RuntimeConnection = connection
        self._on_list_models = options.on_list_models
        self._on_github_telemetry = options.on_github_telemetry

        # Resolve connection-mode-specific state.
        self._actual_host: str = "localhost"
        self._is_external_server: bool = isinstance(connection, UriRuntimeConnection)
        self._cli_path_source: str | None = None
        self._ffi_host: FfiRuntimeHost | None = None
        self._inprocess_runtime_path: str | None = None
        self._inprocess_cli_entrypoint: str | None = None
        effective_env = options.env if options.env is not None else os.environ
        self._cli_login_sync_integration_id = (
            effective_env.get("GITHUB_COPILOT_INTEGRATION_ID", "").strip() or None
        )

        if isinstance(connection, UriRuntimeConnection):
            if connection.connection_token is not None and len(connection.connection_token) == 0:
                raise ValueError("connection_token must be a non-empty string")
            self._actual_host, actual_port = self._parse_cli_url(connection.url)
            self._runtime_port: int | None = actual_port
            self._effective_connection_token: str | None = connection.connection_token
        elif isinstance(connection, InProcessRuntimeConnection):
            # In-process (FFI): no child process and no per-connection token.
            self._runtime_port = None
            self._effective_connection_token = None
            self._inprocess_runtime_path = self._resolve_inprocess_runtime()
            if options.use_logged_in_user is None:
                options.use_logged_in_user = not bool(options.github_token)
        else:
            assert isinstance(connection, ChildProcessRuntimeConnection)
            self._runtime_port = None

            if isinstance(connection, TcpRuntimeConnection):
                if (
                    connection.connection_token is not None
                    and len(connection.connection_token) == 0
                ):
                    raise ValueError("connection_token must be a non-empty string")
                self._effective_connection_token = (
                    connection.connection_token
                    if connection.connection_token is not None
                    else str(uuid.uuid4())
                )
            else:
                self._effective_connection_token = None

            # Resolve runtime path: explicit CLI > COPILOT_CLI_PATH > downloaded runtime.
            # Select the environment by identity, not truthiness, so an intentionally
            # empty per-connection or client env stays authoritative (the spawned child
            # receives that empty mapping) instead of falling back to os.environ and
            # unexpectedly honoring a host COPILOT_CLI_PATH.
            if connection.env is not None:
                effective_env: Mapping[str, str] = connection.env
            elif options.env is not None:
                effective_env = options.env
            else:
                effective_env = os.environ
            connection.path = self._resolve_runtime_entrypoint(connection.path, env=effective_env)

            # Resolve use_logged_in_user default
            if options.use_logged_in_user is None:
                options.use_logged_in_user = not bool(options.github_token)

        self._process: Any = None
        self._cli_process: subprocess.Popen | None = None
        self._tcp_process_exit_error: Callable[[], str] | None = None
        self._client: JsonRpcClient | None = None
        self._state: _ConnectionState = "disconnected"
        self._start_lock = asyncio.Lock()
        self._sessions: dict[str, CopilotSession] = {}
        self._sessions_lock = threading.Lock()
        self._session_initializations: weakref.WeakValueDictionary[
            str, _SessionInitializationLock
        ] = weakref.WeakValueDictionary()
        self._active_session_callbacks: dict[str, int] = {}
        self._ordinary_session_resumes: dict[str, set[asyncio.Future[None]]] = {}
        self._resume_predecessors: dict[CopilotSession, CopilotSession | None] = {}
        self._auto_mode_recovery_cleanups: dict[JsonRpcClient, dict[str, asyncio.Future[None]]] = {}
        self._auto_mode_recovery_cleanup_tasks: set[asyncio.Task[None]] = set()
        self._session_admission_closed = False
        self._ahp_hosts = _AhpHostManager(
            self._get_session, self.create_session, self.resume_session
        )
        self._github_token_providers: dict[str, _GitHubTokenProviderRegistration] = {}
        self._github_token_providers_lock = threading.Lock()
        self._github_token_provider_adapter = _GitHubTokenProviderAdapter(self)
        self._models_cache: list[ModelInfo] | None = None
        self._models_cache_lock = asyncio.Lock()
        self._lifecycle_handlers: list[SessionLifecycleHandler] = []
        self._typed_lifecycle_handlers: dict[
            SessionLifecycleEventType, list[SessionLifecycleHandler]
        ] = {}
        self._lifecycle_handlers_lock = threading.Lock()
        self._rpc: ServerRpc | None = None
        self._negotiated_protocol_version: int | None = None
        if options.session_fs is not None:
            _validate_session_fs_config(options.session_fs)
        self._session_fs_config = options.session_fs
        self._request_handler = options.request_handler
        self._llm_inference_adapter: _CopilotRequestAdapterHandler | None = None
        self._network_adapter: _NetworkAdapter | None = None
        self._installation_confirmation_adapter: _InstallationConfirmationAdapter | None = None
        self._entra_token_provider = (
            options.entra.token_provider if isinstance(options.entra, EntraAuthOptions) else None
        )
        self._entra_token_adapter: _EntraTokenAdapter | None = None

    def _resolve_runtime_entrypoint(
        self,
        path: str | None,
        *,
        env: Mapping[str, str] | None = None,
    ) -> str:
        """Resolve the runtime executable path (explicit > env > downloaded).

        Sets ``self._cli_path_source`` for diagnostics. When
        Raises:
            RuntimeError: If no runtime path can be resolved.
        """
        if path is not None:
            self._cli_path_source = "explicit"
            return path

        lookup = env if env is not None else os.environ
        env_cli_path = lookup.get("COPILOT_CLI_PATH")
        if env_cli_path:
            self._cli_path_source = "environment"
            return env_cli_path

        from ._cli_download import ensure_runtime_wrapper

        self._cli_path_source = "downloaded"
        return ensure_runtime_wrapper()

    def _resolve_inprocess_runtime(self) -> str:
        explicit_cli = os.environ.get("COPILOT_CLI_PATH")
        if explicit_cli:
            from ._cli_download import ensure_runtime_library

            runtime_path = ensure_runtime_library(explicit_cli)
            if runtime_path is None:
                raise RuntimeError(
                    f"In-process runtime library not found next to '{explicit_cli}'."
                )
            self._cli_path_source = "environment"
            self._inprocess_cli_entrypoint = explicit_cli
            return runtime_path

        from ._cli_download import ensure_runtime_wrapper

        wrapper_path = Path(ensure_runtime_wrapper())
        self._cli_path_source = "downloaded"
        return str(wrapper_path.with_name("runtime.node"))

    @property
    def rpc(self) -> ServerRpc:
        """Typed server-scoped RPC methods."""
        if self._rpc is None:
            raise RuntimeError("Client is not connected. Call start() first.")
        return self._rpc

    @property
    def runtime_port(self) -> int | None:
        """TCP port the runtime is listening on, when using TCP transport.

        Useful for multi-client scenarios where a second client needs to connect
        to the same runtime. Only available after :meth:`start` completes and
        only when not using stdio transport.
        """
        return self._runtime_port

    def _parse_cli_url(self, url: str) -> tuple[str, int]:
        """
        Parse CLI URL into host and port.

        Supports raw TCP addresses: "host:port", "[ipv6]:port",
        "http://host:port", or just "port".

        Args:
            url: The CLI URL to parse.

        Returns:
            A tuple of (host, port).

        Raises:
            ValueError: If the URL format is invalid or the port is out of range.
        """
        if url.lower().startswith("https://"):
            raise ValueError(
                "HTTPS runtime connections are not supported: "
                "this transport uses plaintext TCP, not TLS"
            )

        clean_url = re.sub(r"^http://", "", url)

        # Check if it's just a port number
        if clean_url.isdigit():
            port = int(clean_url)
            if port <= 0 or port > 65535:
                raise ValueError(f"Invalid port in cli_url: {url}")
            return ("localhost", port)

        ipv6_match = re.match(r"^\[([^\]]+)\]:(.*)$", clean_url)
        if ipv6_match:
            host = ipv6_match.group(1)
            port_text = ipv6_match.group(2)
            try:
                ipaddress.IPv6Address(host)
            except ValueError as e:
                raise ValueError(f"Invalid cli_url format: {url}") from e
        else:
            # Parse host:port format
            parts = clean_url.split(":")
            if len(parts) != 2:
                raise ValueError(f"Invalid cli_url format: {url}")
            host = parts[0] if parts[0] else "localhost"
            port_text = parts[1]

        try:
            port = int(port_text)
        except ValueError as e:
            raise ValueError(f"Invalid port in cli_url: {url}") from e

        if port <= 0 or port > 65535:
            raise ValueError(f"Invalid port in cli_url: {url}")

        return (host, port)

    async def __aenter__(self) -> CopilotClient:
        """
        Enter the async context manager.

        Automatically starts the CLI server and establishes a connection if not
        already connected.

        Returns:
            The CopilotClient instance.

        Example:
            >>> async with CopilotClient() as client:
            ...     session = await client.create_session()
            ...     await session.send("Hello!")
        """
        await self.start()
        return self

    async def __aexit__(
        self,
        exc_type: type[BaseException] | None = None,
        exc_val: BaseException | None = None,
        exc_tb: TracebackType | None = None,
    ) -> None:
        """
        Exit the async context manager.

        Performs graceful cleanup by destroying all active sessions and stopping
        the CLI server.
        """
        await self.stop()

    async def start(self) -> None:
        """
        Start the CLI server and establish a connection.

        If connecting to an already-running runtime (via :meth:`RuntimeConnection.for_uri`),
        only establishes the connection. Otherwise, spawns the CLI server process
        and then connects.

        This method is called automatically when creating a session, so most
        callers do not need to call it explicitly.

        Raises:
            RuntimeError: If the server fails to start or the connection fails.

        Example:
            >>> client = CopilotClient()
            >>> await client.start()
            >>> # Now ready to create sessions
        """
        # Concurrent session creation can auto-start the same client. Keep the
        # state check and all transport initialization under one lock so only
        # one caller can spawn a runtime and install its connection at a time.
        async with self._start_lock:
            await self._start()

    async def _start(self) -> None:
        if self._state == "connected":
            return

        start_time = time.perf_counter()
        self._state = "connecting"

        try:
            # Only start CLI server process if not connecting to external server
            if not self._is_external_server:
                await self._start_cli_server()

            # Connect to the server
            await self._connect_to_server()
            log_timing(
                logger,
                logging.DEBUG,
                "CopilotClient.start transport setup complete",
                start_time,
            )

            try:
                await self._initialize_connection(start_time)
            except BaseException as startup_error:
                # The runtime keeps registrations made on this connection, such as an exclusive
                # runtime-scoped network registration, until it closes. Cancellation bypasses the
                # outer startup cleanup, so close the connection here for every outcome and never
                # let a cleanup error mask the startup error.
                try:
                    await self.force_stop()
                except Exception as cleanup_error:
                    raise startup_error from cleanup_error
                raise

            with self._sessions_lock:
                self._session_admission_closed = False
            self._state = "connected"
            log_timing(
                logger,
                logging.DEBUG,
                "CopilotClient.start complete",
                start_time,
            )
        except ProcessExitedError as e:
            # Process exited with error - reraise as RuntimeError with stderr
            self._state = "error"
            log_timing(
                logger,
                logging.WARNING,
                "CopilotClient.start failed",
                start_time,
                exc_info=True,
            )
            raise RuntimeError(str(e)) from None
        except Exception as e:
            self._state = "error"
            log_timing(
                logger,
                logging.WARNING,
                "CopilotClient.start failed",
                start_time,
                exc_info=True,
            )
            # Check if process exited and capture any remaining stderr
            process = self._cli_process if self._cli_process is not None else self._process
            if process and hasattr(process, "poll"):
                if isinstance(e, BrokenPipeError) and process.poll() is None:
                    try:
                        await asyncio.to_thread(process.wait, timeout=1.0)
                    except subprocess.TimeoutExpired:
                        pass
                return_code = process.poll()
                if return_code is not None and self._client and self._client.process is process:
                    raise RuntimeError(self._client._get_process_exit_error()) from e
            raise

    async def _initialize_connection(self, start_time: float) -> None:
        """Verify the protocol version and register this client's providers on a new connection."""
        await self._verify_protocol_version()
        log_timing(
            logger,
            logging.DEBUG,
            "CopilotClient.start protocol verification complete",
            start_time,
        )

        if self._network_registration is not None:
            await self.rpc.network.set_provider(self._network_registration)

        if self._options.extension_launch_provider is not None:
            await self.rpc.register_extension_launch_provider()

        if self._options.builtin_plugin_directories:
            assert self._client is not None
            await self._client.request(
                "plugins.builtin.set",
                {"paths": list(self._options.builtin_plugin_directories)},
            )

        if self._session_fs_config:
            session_fs_start = time.perf_counter()
            await self._set_session_fs_provider()
            log_timing(
                logger,
                logging.DEBUG,
                "CopilotClient.start session filesystem setup complete",
                session_fs_start,
            )

        if self._request_handler is not None:
            await self._set_llm_inference_provider()

    async def stop(self) -> None:
        """
        Stop the CLI server and close all active sessions.

        This method performs graceful cleanup:
        1. Closes all active sessions (releases in-memory resources); detach
           requests share one shutdown timeout, so an unresponsive runtime
           cannot hold the remaining steps back
        2. Requests runtime shutdown for SDK-owned CLI processes
        3. Closes owned stdio input and waits for host cleanup and natural exit
        4. Closes the JSON-RPC connection and terminates any remaining owned process

        Note: session data on disk is preserved, so sessions can be resumed
        later. To permanently remove session data before stopping, call
        :meth:`delete_session` for each session first.

        Raises:
            ExceptionGroup[StopError]: If any errors occurred during cleanup.

        Example:
            >>> try:
            ...     await client.stop()
            ... except* StopError as eg:
            ...     for error in eg.exceptions:
            ...         print(f"Cleanup error: {error.message}")
        """
        errors: list[StopError] = []
        if self._entra_token_adapter is not None:
            self._entra_token_adapter.close_connection()
        if self._installation_confirmation_adapter is not None:
            self._installation_confirmation_adapter.close_connection()
        self._ahp_hosts.disconnect()
        if self._llm_inference_adapter is not None:
            self._llm_inference_adapter.cancel_pending()

        # Atomically take ownership of all sessions and clear the dict
        # so no other thread can access them
        with self._sessions_lock:
            self._session_admission_closed = True
            sessions_to_destroy = list(self._sessions.values())
            self._sessions.clear()

        # A hung runtime never answers session.detach, so every detach shares one
        # deadline; past it, stop() moves on to runtime shutdown and termination.
        loop = asyncio.get_running_loop()
        detach_deadline = loop.time() + _RUNTIME_SHUTDOWN_TIMEOUT_SECONDS
        for session in sessions_to_destroy:
            try:
                await asyncio.wait_for(
                    session.disconnect(), timeout=max(0.0, detach_deadline - loop.time())
                )
            except TimeoutError:
                errors.append(
                    StopError(
                        message=f"Failed to disconnect session {session.session_id}: "
                        "session.detach did not complete within the "
                        f"{_RUNTIME_SHUTDOWN_TIMEOUT_SECONDS}s shutdown timeout"
                    )
                )
            except Exception as e:
                logger.debug(
                    "Error while cleaning up Copilot session %s",
                    session.session_id,
                    exc_info=True,
                )
                errors.append(
                    StopError(message=f"Failed to disconnect session {session.session_id}: {e}")
                )
            finally:
                session._mark_disconnected()
        with self._github_token_providers_lock:
            self._github_token_providers.clear()

        if (
            self._rpc is not None
            and (self._cli_process is not None or self._ffi_host is not None)
            and not self._is_external_server
        ):
            runtime_shutdown_start = time.perf_counter()
            try:
                await self._rpc.runtime.shutdown(timeout=_RUNTIME_SHUTDOWN_TIMEOUT_SECONDS)
                log_timing(
                    logger,
                    logging.DEBUG,
                    "CopilotClient.stop runtime shutdown complete",
                    runtime_shutdown_start,
                )
            except Exception as e:
                log_timing(
                    logger,
                    logging.DEBUG,
                    "CopilotClient.stop runtime shutdown failed",
                    runtime_shutdown_start,
                    exc_info=True,
                )
                errors.append(StopError(message=f"Failed to gracefully shut down runtime: {e}"))

        # Host telemetry is finalized after stdio EOF, not the shutdown response.
        # Keep the readers alive while the child drains its final output.
        if (
            self._cli_process is not None
            and not self._is_external_server
            and isinstance(self._connection, StdioRuntimeConnection)
            and self._cli_process.poll() is None
        ):
            try:
                if self._cli_process.stdin is not None:
                    self._cli_process.stdin.close()
                await asyncio.to_thread(
                    self._cli_process.wait,
                    timeout=_CLI_PROCESS_GRACEFUL_EXIT_TIMEOUT_SECONDS,
                )
            except subprocess.TimeoutExpired:
                logger.debug("Timed out waiting for graceful CLI exit; terminating the process")
            except OSError:
                logger.debug("Error while closing Copilot CLI stdin", exc_info=True)

        # Close client. Network callbacks stay available until here because the runtime may still
        # route or authenticate requests while it shuts down.
        if self._network_adapter is not None:
            self._network_adapter.cancel_pending()
        if self._client:
            rpc_client = self._client
            await rpc_client.stop()
            with self._sessions_lock:
                self._auto_mode_recovery_cleanups.pop(rpc_client, None)
            self._client = None
        self._rpc = None
        self._installation_confirmation_adapter = None

        # Clear models cache
        async with self._models_cache_lock:
            self._models_cache = None

        # Dispose the in-process FFI host and release the loaded native library.
        if self._ffi_host is not None:
            try:
                await asyncio.to_thread(self._ffi_host.dispose)
            except Exception:
                logger.debug("Error while disposing in-process FFI host", exc_info=True)
            self._ffi_host = None
            self._process = None

        # Close TCP socket wrappers without treating them as owned processes.
        if self._process is not None and self._process is not self._cli_process:
            try:
                self._process.terminate()
            except Exception:
                logger.debug("Error while closing Copilot runtime transport", exc_info=True)
            self._process = None

        # Terminate and reap an owned process that did not exit gracefully.
        if self._cli_process and not self._is_external_server:
            poll = getattr(self._cli_process, "poll", None)
            is_running = poll is None or poll() is None
            if is_running:
                self._cli_process.terminate()
                try:
                    await asyncio.to_thread(
                        self._cli_process.wait,
                        timeout=_CLI_PROCESS_EXIT_TIMEOUT_SECONDS,
                    )
                except subprocess.TimeoutExpired:
                    self._cli_process.kill()
                    try:
                        await asyncio.to_thread(
                            self._cli_process.wait,
                            timeout=_CLI_PROCESS_EXIT_TIMEOUT_SECONDS,
                        )
                    except subprocess.TimeoutExpired as e:
                        errors.append(
                            StopError(
                                message=(
                                    f"Timed out waiting for CLI process to exit after kill: {e}"
                                )
                            )
                        )
            if self._process is self._cli_process:
                self._process = None
            self._cli_process = None

        self._tcp_process_exit_error = None
        self._state = "disconnected"
        if not self._is_external_server:
            self._runtime_port = None

        if errors:
            raise ExceptionGroup("errors during CopilotClient.stop()", errors)

    async def force_stop(self) -> None:
        """
        Forcefully stop the CLI server without graceful cleanup.

        Use this when :meth:`stop` fails or takes too long. This method:
        - Clears all sessions immediately without destroying them
        - Force closes the connection (closes the underlying transport)
        - Kills the CLI process (if spawned by this client)
        - Closes owned stdin so processes behind a launcher also receive EOF
        - Waits for the spawned CLI process to exit

        Raises:
            OSError: If the spawned CLI process cannot be terminated.
            subprocess.TimeoutExpired: If owned stdin cannot close or the spawned
                CLI process does not exit within the shutdown timeout.

        Example:
            >>> # If normal stop hangs, force stop
            >>> try:
            ...     await asyncio.wait_for(client.stop(), timeout=5.0)
            ... except asyncio.TimeoutError:
            ...     await client.force_stop()
        """
        if self._entra_token_adapter is not None:
            self._entra_token_adapter.close_connection()
        if self._installation_confirmation_adapter is not None:
            self._installation_confirmation_adapter.close_connection()
        self._ahp_hosts.disconnect()
        if self._llm_inference_adapter is not None:
            self._llm_inference_adapter.cancel_pending()
        if self._network_adapter is not None:
            self._network_adapter.cancel_pending()

        # Clear sessions immediately without trying to destroy them
        with self._sessions_lock:
            self._session_admission_closed = True
            sessions = list(self._sessions.values())
            self._sessions.clear()
        for session in sessions:
            session._mark_disconnected()
        with self._github_token_providers_lock:
            self._github_token_providers.clear()

        process_error: OSError | subprocess.TimeoutExpired | None = None

        # Close the transport first to signal the server immediately.
        # For external servers (TCP), this closes the socket.
        # The FFI adapter is disposed off-thread below.
        if self._ffi_host is None and (self._process is not None or self._cli_process is not None):
            try:
                if self._is_external_server:
                    if self._process is not None:
                        self._process.terminate()  # closes the TCP socket
                    self._process = None
                    self._cli_process = None
                else:
                    if self._process is not None and self._process is not self._cli_process:
                        self._process.terminate()
                        self._process = None
            except Exception:
                logger.debug("Error while force-closing Copilot runtime transport", exc_info=True)

            if not self._is_external_server and self._cli_process is not None:
                process = self._cli_process
                try:
                    process.kill()
                    # A Windows launcher can exit while its child still holds the read end.
                    if process.stdin is not None:

                        def observe_shutdown(result: asyncio.Future[Any]) -> None:
                            if not result.cancelled() and (error := result.exception()) is not None:
                                logger.debug(
                                    "Error while completing forced CLI shutdown", exc_info=error
                                )

                        shutdown = asyncio.gather(
                            asyncio.to_thread(process.stdin.close),
                            asyncio.to_thread(
                                process.wait, timeout=_CLI_PROCESS_EXIT_TIMEOUT_SECONDS
                            ),
                        )
                        shutdown.add_done_callback(observe_shutdown)
                        try:
                            # Closing can wait for an outstanding pipe write on Windows.
                            async with asyncio.timeout(_CLI_PROCESS_EXIT_TIMEOUT_SECONDS):
                                # A deadline must not discard queued EOF delivery or reaping.
                                await asyncio.shield(shutdown)
                        except TimeoutError as error:
                            raise subprocess.TimeoutExpired(
                                process.args, _CLI_PROCESS_EXIT_TIMEOUT_SECONDS
                            ) from error
                    else:
                        await asyncio.to_thread(
                            process.wait, timeout=_CLI_PROCESS_EXIT_TIMEOUT_SECONDS
                        )
                except (OSError, subprocess.TimeoutExpired) as error:
                    process_error = error
                else:
                    if self._process is process:
                        self._process = None
                    if self._cli_process is process:
                        self._cli_process = None

        # Force-dispose the in-process FFI host before tearing down JSON-RPC.
        if self._ffi_host is not None:
            try:
                await asyncio.to_thread(self._ffi_host.dispose)
            except Exception:
                logger.debug("Error while force-disposing in-process FFI host", exc_info=True)
            self._ffi_host = None
            self._process = None

        # Then clean up the JSON-RPC client
        if self._client:
            rpc_client = self._client
            try:
                await rpc_client.stop()
            except Exception:
                logger.debug(
                    "Error while stopping JSON-RPC client during force stop", exc_info=True
                )
            with self._sessions_lock:
                self._auto_mode_recovery_cleanups.pop(rpc_client, None)
            self._client = None
        self._rpc = None
        self._installation_confirmation_adapter = None

        # Clear models cache
        async with self._models_cache_lock:
            self._models_cache = None

        self._tcp_process_exit_error = None
        self._state = "disconnected"
        if not self._is_external_server:
            self._runtime_port = None

        if process_error is not None:
            raise process_error

    async def create_session(
        self,
        *,
        on_permission_request: _PermissionHandlerFn | None = None,
        model: str | None = None,
        session_id: str | None = None,
        name: str | None = None,
        client_name: str | None = None,
        reasoning_effort: ReasoningEffort | None = None,
        allow_multiple_tool_calls: bool | None = None,
        reasoning_summary: ReasoningSummary | None = None,
        enable_experimental_mode: bool | None = None,
        context_tier: ContextTier | None = None,
        tools: list[Tool] | None = None,
        system_message: SystemMessageConfig | None = None,
        tool_search: ToolSearchConfig | None = None,
        image_generation: ImageGenerationConfig | None = None,
        available_tools: list[str] | ToolSet | None = None,
        excluded_tools: list[str] | ToolSet | None = None,
        on_user_input_request: UserInputHandler | None = None,
        ask_user_variant: AskUserVariant | None = None,
        hooks: SessionHooks | None = None,
        working_directory: str | None = None,
        additional_directories: list[str] | None = None,
        provider: ProviderConfig | None = None,
        capi: CapiSessionOptions | None = None,
        providers: list[NamedProviderConfig] | None = None,
        models: list[ProviderModelConfig] | None = None,
        enable_session_telemetry: bool | None = None,
        enable_citations: bool | None = None,
        enable_file_change_tracking: bool | None = None,
        excluded_builtin_agents: list[str] | None = None,
        session_limits: SessionLimitsConfig | None = None,
        autopilot: AutopilotOptions | None = None,
        skip_custom_instructions: bool | None = None,
        refresh_custom_instructions: bool | None = None,
        trust_working_directory: Literal["session", "user"] | None = None,
        custom_agents_local_only: bool | None = None,
        coauthor_enabled: bool | None = None,
        manage_schedule_enabled: bool | None = None,
        model_capabilities: ModelCapabilitiesOverride | None = None,
        streaming: bool | None = None,
        include_sub_agent_streaming_events: bool | None = None,
        mcp_servers: dict[str, MCPServerConfig] | None = None,
        diagnostics: DiagnosticsConfiguration | None = None,
        mcp_oauth_token_storage: Literal["persistent", "in-memory"] | None = None,
        auth_client_id_metadata_url: str | None = None,
        embedding_cache_storage: Literal["persistent", "in-memory"] | None = None,
        custom_agents: list[CustomAgentConfig] | None = None,
        default_agent: DefaultAgentConfig | dict[str, Any] | None = None,
        agent: str | None = None,
        config_directory: str | None = None,
        enable_config_discovery: bool | None = None,
        skip_embedding_retrieval: bool | None = None,
        organization_custom_instructions: str | None = None,
        enable_on_demand_instruction_discovery: bool | None = None,
        enable_file_hooks: bool | None = None,
        enable_host_git_operations: bool | None = None,
        enable_session_store: bool | None = None,
        enable_skills: bool | None = None,
        included_builtin_skills: list[str] | None = None,
        skill_directories: list[str] | None = None,
        skill_provider: SkillProvider | None = None,
        agent_directories: list[str] | None = None,
        hook_directories: list[str] | None = None,
        plugin_directories: list[str] | None = None,
        instruction_directories: list[str] | None = None,
        disabled_skills: list[str] | None = None,
        disabled_mcp_servers: list[str] | None = None,
        sandbox_config: SandboxConfig | None = None,
        infinite_sessions: InfiniteSessionConfig | None = None,
        large_output: LargeToolOutputConfig | None = None,
        memory: MemoryConfiguration | None = None,
        on_event: Callable[[SessionEvent], None] | None = None,
        commands: list[CommandDefinition] | None = None,
        on_elicitation_request: ElicitationHandler | None = None,
        on_mcp_auth_request: McpAuthHandler | None = None,
        enable_mcp_apps: bool = False,
        on_exit_plan_mode_request: ExitPlanModeHandler | None = None,
        on_auto_mode_switch_request: AutoModeSwitchHandler | None = None,
        auto_mode_recovery: bool = False,
        create_session_fs_handler: CreateSessionFsHandler | None = None,
        github_token: str | None = None,
        github_token_provider: GitHubTokenProvider | None = None,
        cloud: CloudSessionOptions | None = None,
        canvases: list[CanvasDeclaration] | None = None,
        request_canvas_renderer: bool | None = None,
        request_extensions: bool | None = None,
        extension_sdk_path: str | None = None,
        extension_info: ExtensionInfo | None = None,
        canvas_provider: CanvasProviderIdentity | None = None,
        canvas_handler: CanvasHandler | None = None,
        feature_flags: dict[str, bool] | None = None,
        exp_assignments: CopilotExpAssignmentResponse | None = None,
        enable_managed_settings: bool | None = None,
        excluded_managed_settings: list[str] | None = None,
        enforce_managed_model_defaults: bool | None = None,
        interactive_user: bool | None = None,
        permission_mode: PermissionMode | None = None,
        github_mcp_tool_config: GitHubMcpToolConfig | None = None,
        managed_settings: ManagedSettings | None = None,
    ) -> CopilotSession:
        """
        Create a new conversation session with the Copilot CLI.

        Sessions maintain conversation state, handle events, and manage tool execution.
        If the client is not yet connected, this will automatically start the
        connection.

        Args:
            on_permission_request: Optional handler for permission requests. When
                omitted, permission requests are surfaced as events and left pending
                for the consumer to resolve via the pending permission RPC.
            model: The model to use for the session (e.g. ``"gpt-5.4"``).
            session_id: Optional session ID. If not provided, a UUID is generated.
            name: Names the session at creation, as ``session.name.set`` would, and
                is validated the same way: non-empty, no leading or trailing whitespace,
                at most 100 UTF-16 units, no control characters, and no double quotes.
                A name also makes the session persist from the start, so it can be listed
                and resumed without ever running a turn. Local sessions only: cloud
                creation rejects it.
            client_name: Optional client name for identification.
            reasoning_effort: Reasoning effort level for the model.
            allow_multiple_tool_calls: Whether the model may request multiple tool calls per
                response (default True). False is a provider hint, not an execution guarantee;
                all returned calls are handled and handlers may run concurrently.
                Re-supply on cold resume.
            reasoning_summary: Reasoning summary mode for supported models.
                Use ``"none"`` to suppress summary output regardless of whether
                reasoning is enabled.
            enable_experimental_mode: Controls whether the session enables
                experimental features. Defaults to ``False`` in ``"empty"``
                mode; otherwise the runtime decides when omitted.
            coauthor_enabled: Whether to instruct the agent to include a
                ``Co-authored-by`` trailer in commit messages. Subagents inherit this
                setting. Defaults to ``False`` in ``"empty"`` mode; otherwise the
                runtime decides when omitted.
            context_tier: Context window tier for models that support it. Use
                ``"long_context"`` to pin the session to the long-context tier.
            tools: Custom tools to register with the session.
            system_message: System message configuration.
            available_tools: Allowlist of tools to enable. When specified, only
                these tools will be available. Applies to the full merged tool
                catalog including built-in tools, MCP tools, and custom tools
                registered via ``tools=``. Custom tool names must be explicitly
                included or they will be hidden from the model. Takes precedence
                over ``excluded_tools``.
            excluded_tools: List of tools to disable. Applies to all tools
                including custom tools registered via ``tools=``. Ignored if
                ``available_tools`` is set.
            on_user_input_request: Handler for user input requests.
            ask_user_variant: Model-facing shape of the ``ask_user`` tool.
                Accepted values are ``"legacy"`` and ``"elicitation"``. The
                default is ``"legacy"``. To use ``"elicitation"``, also provide
                ``on_elicitation_request`` so the host can answer structured forms.
            hooks: Lifecycle hooks for the session.
            working_directory: Working directory for the session.
            provider: Provider configuration for Azure or custom endpoints.
            capi: CAPI provider-scoped options. Set ``auto_tier`` to ``efficiency``,
                ``balance``, ``intelligence``, or ``fast`` to select an Auto routing
                preference on a runtime with Auto tier support. ``fast`` is an
                integrator-only latency preset, not a first-party GitHub Copilot
                product preference. WebSocket transport is the
                default for the CAPI Responses API whenever the model advertises
                the ``ws:/responses`` endpoint. Set
                ``enable_web_socket_responses=False`` to force the HTTP
                Responses transport, which is useful behind proxies where
                WebSockets fail. This is equivalent to setting the
                ``COPILOT_CLI_DISABLE_WEBSOCKET_RESPONSES`` environment
                variable. The option is under the ``capi`` namespace because a
                single session can host multiple providers (CAPI + BYOK), so
                transport choice is provider-level.
            providers: Named BYOK provider connections. Additive to Copilot API
                auth (unlike `provider`); combine with `models`. Cannot be
                combined with `provider`.
            models: BYOK model definitions added to the selectable model list,
                each referencing a `providers` entry by name.
            enable_session_telemetry: Enables or disables internal session telemetry
                for this session. When False, disables session telemetry. When omitted
                or True, telemetry is enabled for eligible GitHub-authenticated sessions,
                including custom providers (BYOK). Host and account policies still
                control uploads. This is independent of client OpenTelemetry configuration.
            enable_citations: **Experimental.** Enables native model citations for
                supported providers.
            enable_file_change_tracking: Opts in to capturing file changes from the
                first turn for session rewind and cumulative session diff.
            excluded_builtin_agents: Built-in agent names to exclude from the
                session. Excluded built-in agents are hidden from discovery and
                cannot be selected or invoked unless a custom agent with the same
                name is configured.
            session_limits: **Experimental.** Limits applied to this session's
                current accounting window.
            autopilot: **Experimental.** Autopilot policy, independent of session mode.
                Omitted fields use runtime defaults.
            refresh_custom_instructions: When True, invalidates the process-wide
                custom-instruction discovery cache before constructing this new
                session. Omitted or False retains the cache. Other sessions in this
                runtime may observe updated instructions on later turns or discovery.
                This does not watch files or enable disabled instruction loading.
            trust_working_directory: Trust the canonical working directory for this session
                ("session") or persistently for the user ("user"). Omitted leaves
                existing folder trust unchanged.
            model_capabilities: Override individual model capabilities resolved by the runtime.
            streaming: Whether to enable streaming responses.
            include_sub_agent_streaming_events: Whether to include sub-agent streaming
                delta events (e.g., ``assistant.message_delta``,
                ``assistant.reasoning_delta``, ``assistant.streaming_delta`` with
                ``agentId`` set). When False, only non-streaming sub-agent events and
                ``subagent.*`` lifecycle events are forwarded. Defaults to True.
            mcp_servers: MCP server configurations.
            mcp_oauth_token_storage: Controls how MCP OAuth tokens are stored.
                ``"persistent"`` uses the OS keychain (shared across sessions).
                ``"in-memory"`` stores tokens in memory (discarded on session end).
                Defaults to ``"in-memory"`` for safe multitenant behavior.
            auth_client_id_metadata_url: OAuth Client ID Metadata Document URL
                identifying the host for MCP authorization. When unset, no host
                identity is supplied.
            embedding_cache_storage: Controls how embedding caches are stored.
                `"persistent"` uses disk-based storage (shared across sessions).
                `"in-memory"` stores embeddings in memory (discarded on session end).
                Defaults to `"in-memory"` in empty mode.
            custom_agents: Custom agent configurations.
            default_agent: Configuration for the default agent,
                including tool visibility controls.
            agent: Agent to use for the session.
            config_directory: Override for the configuration directory.
            enable_config_discovery: Enables runtime discovery of supported
                configuration. Explicitly supplied configuration takes precedence
                over discovered values.
            skip_embedding_retrieval: When True, skips embedding-based retrieval.
            organization_custom_instructions: Organization-level custom instructions.
            enable_on_demand_instruction_discovery: Enables on-demand instruction file
                discovery.
            enable_file_hooks: Enables repository-origin file hooks from ``.github/hooks/``
                and trusted repository-configured roots, not explicit hook_directories
                or callback hooks.
            enable_host_git_operations: Enables git operations on the host filesystem.
            enable_session_store: Enables the cross-session store.
            enable_skills: Enables skill loading.
            skill_directories: Explicit discovery-only skill roots. No general filesystem
                grant. Available without enable_config_discovery, subject to skill/managed
                disables. Relative paths resolve once when supplied against the request's
                effective working_directory; retained roots do not move on cwd changes.
                None and omission are equivalent; [] clears only the explicit SDK roots.
            skill_provider: **Experimental.** Session-scoped provider for SDK-owned
                skills. Providers are not persisted; re-supply the provider when
                resuming the session. Synchronous providers run on the event loop,
                so keep them fast or make them async. Not supported for cloud
                sessions.
            agent_directories: Explicit discovery-only agent roots, separate from
                custom_agents. Same path and clearing rules as skill_directories. Available
                without enable_config_discovery, subject to source/managed disables.
            hook_directories: Explicit discovery-only file-hook roots, separate from callback
                hooks. Same path and clearing rules as skill_directories. Available without
                enable_config_discovery or enable_file_hooks (repository-origin hooks only);
                hook and managed disables still apply.
            instruction_directories: Explicit discovery-only instruction roots using existing
                SDK formats. Same path and clearing rules as skill_directories. Available
                without enable_config_discovery, subject to instruction/managed disables.
            disabled_skills: Skills to disable.
            disabled_mcp_servers: Exact MCP server names to disable only for this
                session. Disabled servers are not started or authenticated on
                create or cold resume; a resident resume cannot stop servers
                already running. This does not change global MCP settings.
            sandbox_config: Sandbox configuration applied when the session is constructed,
                before MCP servers start. Same shape as the sandboxConfig option of session
                options update; an identical later update is a no-op.
            infinite_sessions: Infinite session configuration.
            memory: Session memory configuration. **Experimental:** set
                ``store_memory`` to store memories in the app instead of the
                built-in memory backends; see :class:`MemoryConfiguration`.
            image_generation: **Experimental.** Image generation opt-in. Not persisted; re-supply
                after runtime restart. See :class:`ImageGenerationConfig`.
            cloud: Creates a remote session in the cloud instead of a local
                session. Optionally associates repository metadata with the
                cloud session.
            on_event: Callback for session events.
            auto_mode_recovery: Opt in to runtime-owned recovery of eligible active requests.
                Requires ``on_auto_mode_switch_request`` and a positive
                ``capabilities.ui.autoModeRecovery`` acknowledgment. Unsupported enrollment
                fails and detaches the session. Defaults to False; re-supply on resume.
                Consent has no runtime deadline. Do not resend or rewind from the handler.
            enable_mcp_apps: **Experimental.** Opt into MCP Apps (SEP-1865) UI
                passthrough. This parameter is part of an experimental
                wire-protocol surface and may change or be removed in a future
                release. When True, the SDK sends ``requestMcpApps: True`` on
                ``session.create``. The runtime only honors the opt-in when its
                ``MCP_APPS`` feature flag (or ``COPILOT_MCP_APPS=true`` env
                override) is on; otherwise the request is silently dropped.
                Inspect ``capabilities.ui.mcpApps`` on the create response to
                detect the drop.
            github_mcp_tool_config: Configuration for the built-in GitHub MCP
                server, sent as ``githubMcpToolConfig`` on ``session.create``.
                Supports ``enable_all_tools``, ``additional_toolsets``,
                ``additional_tools``, ``enable_insiders_mode``, and
                ``disable_form_deferral``. Setting ``disable_form_deferral``
                makes form-backed GitHub write tools execute directly instead
                of returning an awaiting-form stub; it does not enable MCP Apps
                on its own and has no effect unless MCP Apps are enabled for
                the session (see ``enable_mcp_apps``). Omitted from the wire
                payload entirely when None.
            feature_flags: Feature-flag values resolved by the host for this
                session. Re-supply them when resuming after a runtime restart.
                Sent on the wire as ``featureFlags``.
            exp_assignments: ExP assignment ("flight") data injected by a
                trusted integrator, in the same JSON shape the Copilot CLI
                fetches from the experimentation service
                (``CopilotExpAssignmentResponse``). When supplied, the runtime
                feeds it into the same feature-flag path as CLI-fetched
                assignments and stamps it onto telemetry and the CAPI request
                header. When absent, the session does not block on ExP. Intended
                for out-of-process integrators that fetch ExP data themselves;
                malformed payloads are dropped by the runtime (fail-open). This
                is an internal/trusted-integrator option. Sent on the wire as
                ``expAssignments``.
            enable_managed_settings: Opt-in flag. When ``True``, the runtime
                self-fetches enterprise managed settings (bypass-permissions
                policy) at session bootstrap using the session's ``github_token``.
                Requires ``github_token`` to be set; if omitted, the runtime is
                expected to reject session creation (fail-closed). When unset,
                behaves exactly as before. Sent on the wire as
                ``enableManagedSettings``.
            excluded_managed_settings: Exact dotted managed-setting paths to exclude
                from fetched, device, and policy-helper policy, such as ``sandbox``.
                Does not exclude host-injected policy or availability checks.
                Not persisted; re-supply on resume. Requires
                ``enable_managed_settings`` to self-fetch policy.
            enforce_managed_model_defaults: When ``True``, locked managed model
                controls are enforced for this session. Conflicting model changes
                are rejected, while overridable managed defaults remain mutable.
                Sent on the wire as ``enforceManagedModelDefaults``.
            interactive_user: Declares that a human drives this session's
                conversation, enabling user-message analysis telemetry for
                user-authored messages. Automated messages remain excluded.
                Defaults to False for SDK sessions. Ignored for cloud sessions.
            permission_mode: Permission mode applied before the session's first
                turn, before this method returns. Uses the same checks as
                ``session.rpc.permissions.set_mode``; a mode that managed policy
                or session capabilities refuse leaves the current mode in place.
                ``session.permission_mode_result`` reports whether it was
                applied and the authoritative mode. Omit to keep the default
                mode. Not supported for cloud sessions. Sent on the wire as
                ``permissionMode``.
            managed_settings: Host-injected enterprise managed settings for the
                session. Supplies managed policy directly instead of
                self-fetching; the runtime validates it and composes it
                restrictively with any self-fetched (server) and device-managed
                layers. Startup-only and not persisted: re-supply on
                :meth:`resume_session` (omitting it clears the injected layer).
                May be combined with ``enable_managed_settings``. Requires a
                runtime whose RPC schema includes ``managedSettings``. Sent on
                the wire as ``managedSettings``.
            github_token_provider: Callback that acquires a short-lived GitHub
                credential for this session. Mutually exclusive with
                ``github_token``. It receives the effective host, session ID
                when assigned, and acquisition reason; the registration ID is
                kept internal.

        Returns:
            A :class:`CopilotSession` instance for the new session.

        Raises:
            ValueError: If ``on_permission_request`` is provided but not callable.

        Note:
            A session retained by the filesystem factory is retired if initialization
            or the create RPC fails or is cancelled. Cancelling an accepted cloud
            creation does not delete its remote session.

        Example:
            >>> session = await client.create_session(
            ...     on_permission_request=PermissionHandler.approve_all,
            ... )
            >>>
            >>> # Session with model and streaming
            >>> session = await client.create_session(
            ...     on_permission_request=PermissionHandler.approve_all,
            ...     model="gpt-5.4",
            ...     streaming=True,
            ... )
        """
        if auto_mode_recovery and not callable(on_auto_mode_switch_request):
            raise ValueError("auto_mode_recovery requires on_auto_mode_switch_request")
        if on_permission_request is not None and not callable(on_permission_request):
            raise ValueError("on_permission_request must be callable when provided.")
        if github_token is not None and github_token_provider is not None:
            raise ValueError("github_token and github_token_provider are mutually exclusive")
        if ask_user_variant not in (None, "legacy", "elicitation"):
            raise ValueError('ask_user_variant must be "legacy" or "elicitation"')
        if cloud is not None and skill_provider is not None:
            raise ValueError("Skill providers are not supported for cloud sessions.")
        store_memory = _requested_memory_store(memory)
        if cloud is not None and store_memory is not None:
            raise ValueError("Memory stores are not supported for cloud sessions.")
        # Wait for in-progress startup too: its provider registrations must precede any session.
        if not self._client or self._start_lock.locked():
            await self.start()

        tool_defs = [definition.to_dict() for definition in _tools_to_wire_definitions(tools)]

        # Empty-mode validation and normalization
        mode = self._options.mode
        _require_available_tools_for_empty_mode(mode, _normalize_tool_filter(available_tools))
        available_tools = _normalize_tool_filter(available_tools)
        excluded_tools = _normalize_tool_filter(excluded_tools)
        _validate_tool_filter_list("available_tools", available_tools)
        _validate_tool_filter_list("excluded_tools", excluded_tools)
        # Mode "empty" strips environment_context from the system message.
        system_message = _system_message_for_mode(mode, system_message)
        # Mode "empty" defaults selected session config flags to restrictive values;
        # caller-supplied values win.
        enable_session_telemetry = _enable_session_telemetry_default(mode, enable_session_telemetry)
        skip_embedding_retrieval = _skip_embedding_retrieval_default(mode, skip_embedding_retrieval)
        memory = _memory_default(mode, memory)
        enable_on_demand_instruction_discovery = _enable_on_demand_instruction_discovery_default(
            mode, enable_on_demand_instruction_discovery
        )
        enable_file_hooks = _enable_file_hooks_default(mode, enable_file_hooks)
        enable_host_git_operations = _enable_host_git_operations_default(
            mode, enable_host_git_operations
        )
        enable_session_store = _enable_session_store_default(mode, enable_session_store)
        enable_skills = _enable_skills_default(mode, enable_skills)
        custom_agents_local_only = _custom_agents_local_only_default(mode, custom_agents_local_only)
        enable_experimental_mode = _enable_experimental_mode_default(mode, enable_experimental_mode)

        payload: dict[str, Any] = {}
        if model:
            payload["model"] = model
        if client_name:
            payload["clientName"] = client_name
        if reasoning_effort:
            payload["reasoningEffort"] = reasoning_effort
        if allow_multiple_tool_calls is not None:
            payload["allowMultipleToolCalls"] = allow_multiple_tool_calls
        if reasoning_summary:
            payload["reasoningSummary"] = reasoning_summary
        if enable_experimental_mode is not None:
            payload["isExperimentalMode"] = enable_experimental_mode
        if context_tier:
            payload["contextTier"] = context_tier
        if tools is not None:
            payload["tools"] = tool_defs

        wire_system_message, transform_callbacks = _extract_transform_callbacks(system_message)
        if wire_system_message:
            payload["systemMessage"] = wire_system_message

        if tool_search is not None:
            payload["toolSearch"] = _tool_search_to_wire(tool_search)
        if image_generation is not None:
            payload["imageGeneration"] = _image_generation_to_wire(image_generation)

        if available_tools is not None:
            payload["availableTools"] = available_tools
        if excluded_tools is not None:
            payload["excludedTools"] = excluded_tools
        # Always emit "excluded" precedence so caller-supplied excludedTools win
        # over any built-in availableTools defaults the runtime applies.
        payload["toolFilterPrecedence"] = "excluded"

        # Enable permission request callback if handler provided
        payload["requestPermission"] = bool(on_permission_request)

        # Enable user input request callback if handler provided
        if on_user_input_request:
            payload["requestUserInput"] = True
        if ask_user_variant is not None:
            payload["askUserVariant"] = ask_user_variant

        # Enable elicitation request callback if handler provided
        payload["requestElicitation"] = bool(on_elicitation_request)
        if enable_mcp_apps:
            payload["requestMcpApps"] = True
        if github_mcp_tool_config is not None:
            payload["githubMcpToolConfig"] = _github_mcp_tool_config_to_wire(github_mcp_tool_config)
        payload["requestExitPlanMode"] = bool(on_exit_plan_mode_request)
        payload["requestAutoModeSwitch"] = bool(on_auto_mode_switch_request)
        if auto_mode_recovery:
            payload["requestAutoModeRecovery"] = True

        # Serialize commands (name + description only) into payload
        if commands is not None:
            payload["commands"] = [
                {"name": cmd.name, "description": cmd.description or ""} for cmd in commands
            ]

        # Enable hooks callback if any hook handler provided
        if hooks and any(hooks.values()):
            payload["hooks"] = True

        # Add GitHub token for per-session authentication
        if github_token is not None:
            payload["gitHubToken"] = github_token

        # Add cloud session options if provided
        if cloud is not None:
            payload["cloud"] = _cloud_session_options_to_dict(cloud)

        if feature_flags is not None:
            payload["featureFlags"] = feature_flags

        # Add ExP assignment data if provided (trusted integrator)
        if exp_assignments is not None:
            payload["expAssignments"] = _exp_assignment_response_to_dict(exp_assignments)

        # Opt the runtime into self-fetching enterprise managed settings
        if enable_managed_settings is not None:
            payload["enableManagedSettings"] = enable_managed_settings
        if excluded_managed_settings is not None:
            payload["excludedManagedSettings"] = excluded_managed_settings
        if enforce_managed_model_defaults is not None:
            payload["enforceManagedModelDefaults"] = enforce_managed_model_defaults

        if interactive_user is not None:
            payload["interactiveUser"] = interactive_user

        if permission_mode is not None:
            payload["permissionMode"] = PermissionMode(permission_mode).value

        # Host-injected managed settings (permissions-only contract)
        if managed_settings is not None:
            payload["managedSettings"] = _managed_settings_to_dict(managed_settings)

        # Add working directory if provided
        if working_directory:
            payload["workingDirectory"] = working_directory
        if additional_directories is not None:
            payload["additionalDirectories"] = additional_directories

        # Add streaming option if provided
        if streaming is not None:
            payload["streaming"] = streaming

        # Include sub-agent streaming events (defaults to True)
        payload["includeSubAgentStreamingEvents"] = (
            include_sub_agent_streaming_events
            if include_sub_agent_streaming_events is not None
            else True
        )

        # Opt this connection into gitHubTelemetry.event notifications when a
        # telemetry handler was registered on the client.
        if self._on_github_telemetry is not None:
            payload["enableGitHubTelemetryForwarding"] = True

        # Add provider configuration if provided
        if provider:
            payload["provider"] = self._convert_provider_to_wire_format(provider)

        if capi is not None:
            payload["capi"] = _capi_session_options_to_wire(capi)
        # Add additive BYOK provider/model registry if provided
        if providers:
            payload["providers"] = [
                self._convert_named_provider_to_wire_format(p) for p in providers
            ]
        if models:
            payload["models"] = [self._convert_model_to_wire_format(m) for m in models]

        if enable_session_telemetry is not None:
            payload["enableSessionTelemetry"] = enable_session_telemetry
        if enable_citations is not None:
            payload["enableCitations"] = enable_citations
        if enable_file_change_tracking is not None:
            payload["enableFileChangeTracking"] = enable_file_change_tracking
        if excluded_builtin_agents is not None:
            payload["excludedBuiltinAgents"] = excluded_builtin_agents
        if session_limits is not None:
            payload["sessionLimits"] = _session_limits_to_wire(session_limits)
        if autopilot is not None:
            payload["autopilot"] = _autopilot_to_wire(autopilot)
        if refresh_custom_instructions is not None:
            payload["refreshCustomInstructions"] = refresh_custom_instructions
        if trust_working_directory is not None:
            payload["trustWorkingDirectory"] = trust_working_directory

        # Add model capabilities override if provided
        if model_capabilities:
            payload["modelCapabilities"] = _capabilities_to_dict(model_capabilities)

        # Add MCP servers configuration if provided
        if mcp_servers is not None:
            payload["mcpServers"] = _mcp_servers_to_wire(mcp_servers)
        if diagnostics is not None:
            payload["diagnostics"] = diagnostics.to_dict()
        # Mode "empty" defaults MCP OAuth token storage to in-memory; caller wins.
        mcp_oauth_token_storage = _mcp_oauth_token_storage_default(mode, mcp_oauth_token_storage)
        if mcp_oauth_token_storage is not None:
            payload["mcpOAuthTokenStorage"] = mcp_oauth_token_storage
        if auth_client_id_metadata_url is not None:
            payload["authClientIdMetadataUrl"] = auth_client_id_metadata_url
        embedding_cache_storage = _embedding_cache_storage_default(mode, embedding_cache_storage)
        if embedding_cache_storage is not None:
            payload["embeddingCacheStorage"] = embedding_cache_storage
        payload["envValueMode"] = "direct"

        # Add custom agents configuration if provided
        if custom_agents is not None:
            payload["customAgents"] = [
                self._convert_custom_agent_to_wire_format(agent) for agent in custom_agents
            ]
        if custom_agents_local_only is not None:
            payload["customAgentsLocalOnly"] = custom_agents_local_only

        # Add default agent configuration if provided
        if default_agent:
            payload["defaultAgent"] = self._convert_default_agent_to_wire_format(default_agent)

        # Add agent selection if provided
        if agent:
            payload["agent"] = agent

        # Add config directory override if provided
        if config_directory:
            payload["configDir"] = config_directory

        # Add config discovery flag if provided
        if enable_config_discovery is not None:
            payload["enableConfigDiscovery"] = enable_config_discovery
        if skip_embedding_retrieval is not None:
            payload["skipEmbeddingRetrieval"] = skip_embedding_retrieval
        if organization_custom_instructions is not None:
            payload["organizationCustomInstructions"] = organization_custom_instructions
        if enable_on_demand_instruction_discovery is not None:
            payload["enableOnDemandInstructionDiscovery"] = enable_on_demand_instruction_discovery
        if enable_file_hooks is not None:
            payload["enableFileHooks"] = enable_file_hooks
        if enable_host_git_operations is not None:
            payload["enableHostGitOperations"] = enable_host_git_operations
        if enable_session_store is not None:
            payload["enableSessionStore"] = enable_session_store
        if enable_skills is not None:
            payload["enableSkills"] = enable_skills
        if skill_provider is not None:
            payload["hasSkillProvider"] = True
        if store_memory is not None:
            payload["hasMemoryStore"] = True

        # Add skill directories configuration if provided
        if skill_directories is not None:
            payload["skillDirectories"] = skill_directories
        if agent_directories is not None:
            payload["agentDirectories"] = agent_directories
        if hook_directories is not None:
            payload["hookDirectories"] = hook_directories

        # Add plugin directories configuration if provided
        if plugin_directories:
            payload["pluginDirectories"] = plugin_directories

        # Add instruction directories configuration if provided
        if instruction_directories is not None:
            payload["instructionDirectories"] = instruction_directories

        # Add disabled skills configuration if provided
        if disabled_skills:
            payload["disabledSkills"] = disabled_skills
        if disabled_mcp_servers is not None:
            payload["disabledMcpServers"] = disabled_mcp_servers
        if sandbox_config is not None:
            payload["sandboxConfig"] = sandbox_config.to_dict()

        # Add infinite sessions configuration if provided
        if infinite_sessions:
            wire_config: dict[str, Any] = {}
            if "enabled" in infinite_sessions:
                wire_config["enabled"] = infinite_sessions["enabled"]
            if "background_compaction_threshold" in infinite_sessions:
                wire_config["backgroundCompactionThreshold"] = infinite_sessions[
                    "background_compaction_threshold"
                ]
            if "buffer_exhaustion_threshold" in infinite_sessions:
                wire_config["bufferExhaustionThreshold"] = infinite_sessions[
                    "buffer_exhaustion_threshold"
                ]
            payload["infiniteSessions"] = wire_config

        if large_output is not None:
            payload["largeOutput"] = _large_output_to_wire(large_output)

        if memory is not None:
            payload["memory"] = _memory_to_wire(memory)

        if canvases:
            payload["canvases"] = [c.to_dict() for c in canvases]
        if request_canvas_renderer is not None:
            payload["requestCanvasRenderer"] = request_canvas_renderer
        if request_extensions is not None:
            payload["requestExtensions"] = request_extensions
        if extension_sdk_path is not None:
            payload["extensionSdkPath"] = extension_sdk_path
        if extension_info is not None:
            payload["extensionInfo"] = extension_info.to_dict()
        if canvas_provider is not None:
            payload["canvasProvider"] = canvas_provider.to_dict()

        if not self._client:
            raise RuntimeError("Client not connected")

        rpc_client = self._client
        total_start = time.perf_counter()
        # For cloud sessions, let the CLI/server assign the session id and
        # register the session lazily once the response arrives. For non-cloud
        # sessions we generate the id client-side (when the caller didn't
        # supply one) so the session can be registered BEFORE the RPC — the
        # CLI may issue session-scoped requests (e.g. ``sessionFs.writeFile``
        # for workspace metadata) during ``session.create`` processing, before
        # it has sent the response.
        use_server_generated_id = cloud is not None and session_id is None
        local_session_id: str | None = (
            None if use_server_generated_id else (session_id or str(uuid.uuid4()))
        )
        if local_session_id is not None:
            payload["sessionId"] = local_session_id
        if name is not None:
            payload["name"] = name
        github_token_provider_registration_id = self._register_github_token_provider(
            github_token_provider, local_session_id
        )
        if github_token_provider_registration_id is not None:
            payload["gitHubTokenProviderRegistrationId"] = github_token_provider_registration_id

        # Propagate W3C Trace Context to CLI if OpenTelemetry is active
        trace_ctx = get_trace_context()
        payload.update(trace_ctx)

        event_loop = asyncio.get_running_loop()
        event_loop_thread = threading.get_ident()

        def _retire_unreturned_session(s: CopilotSession) -> None:
            if threading.get_ident() == event_loop_thread:
                s._mark_disconnected()
            else:
                event_loop.call_soon_threadsafe(s._mark_disconnected)

        def _initialize_session(sid: str) -> CopilotSession:
            """Create the session and wire up handlers.

            Invoked from the reader thread the instant the session.create
            response arrives (synchronously, before the next message is
            dispatched) so notifications for the new session id are routed
            to a registered session.
            """
            setup_start = time.perf_counter()
            s = CopilotSession(
                sid,
                rpc_client,
                workspace_path=None,
                managed_settings_enabled=enable_managed_settings is True
                or managed_settings is not None,
                on_disconnect=(
                    None
                    if github_token_provider_registration_id is None
                    else lambda: self._unregister_github_token_provider(
                        github_token_provider_registration_id
                    )
                ),
            )
            try:
                if self._session_fs_config:
                    if create_session_fs_handler is None:
                        raise ValueError(
                            "create_session_fs_handler is required in session config when "
                            "session_fs is enabled in client options."
                        )
                    fs_provider: SessionFsProvider = create_session_fs_handler(s)
                    caps = self._session_fs_config.get("capabilities")
                    if (
                        caps
                        and caps.get("read_file_range")
                        and not isinstance(fs_provider, SessionFsReadFileRangeProvider)
                    ):
                        raise ValueError(
                            "SessionFs capabilities declare byte-range reads but the provider "
                            "does not implement SessionFsReadFileRangeProvider"
                        )
                    if caps and caps.get("sqlite"):
                        from .session_fs_provider import SessionFsSqliteProvider

                        if not isinstance(fs_provider, SessionFsSqliteProvider):
                            raise ValueError(
                                "SessionFs capabilities declare SQLite support but the provider "
                                "does not implement SessionFsSqliteProvider"
                            )
                    if caps and caps.get("binary"):
                        from .session_fs_provider import SessionFsBinaryProvider

                        if not isinstance(fs_provider, SessionFsBinaryProvider):
                            raise ValueError(
                                "SessionFs capabilities declare binary support but the provider "
                                "does not implement SessionFsBinaryProvider"
                            )
                    s._client_session_apis.session_fs = create_session_fs_adapter(fs_provider)
                s._register_tools(tools)
                s._register_commands(commands)
                s._register_skill_provider(skill_provider)
                s._register_memory_store(store_memory)
                s._register_permission_handler(on_permission_request)
                s._register_mcp_auth_handler(on_mcp_auth_request)
                if on_user_input_request:
                    s._register_user_input_handler(on_user_input_request)
                if on_elicitation_request:
                    s._register_elicitation_handler(on_elicitation_request)
                if on_exit_plan_mode_request:
                    s._register_exit_plan_mode_handler(on_exit_plan_mode_request)
                if on_auto_mode_switch_request:
                    s._register_auto_mode_switch_handler(on_auto_mode_switch_request)
                if canvas_handler is not None:
                    s._register_canvas_handler(canvas_handler)
                s._register_bearer_token_providers(
                    _collect_bearer_token_callbacks(provider, providers)
                )
                if hooks:
                    s._register_hooks(hooks)
                if transform_callbacks:
                    s._register_transform_callbacks(transform_callbacks)
                if on_event:
                    s.on(on_event)
            except BaseException:
                _retire_unreturned_session(s)
                raise
            log_timing(
                logger,
                logging.DEBUG,
                "CopilotClient.create_session local setup complete",
                setup_start,
                session_id=sid,
                tools_count=len(tools or []),
                commands_count=len(commands or []),
                has_hooks=hooks is not None,
            )
            return s

        initialization_lock: _SessionInitialization | None = None
        release_inline_initialization: Callable[[], None] | None = None
        session: CopilotSession | None = None
        replaced_session: CopilotSession | None = None
        registered_session_id: str | None = None
        server_assigned_session_id: str | None = None
        inline_state_lock = threading.Lock()
        request_failed = False

        async def _delete_uninitialized_session(
            sid: str, release: Callable[[], None] | None = None
        ) -> None:
            try:
                response = await asyncio.wait_for(
                    rpc_client.request("session.delete", {"sessionId": sid}), timeout=10
                )
                if not response.get("success", False):
                    raise RuntimeError(
                        f"Failed to delete session {sid}: {response.get('error', 'Unknown error')}"
                    )
            except Exception:
                logger.warning(
                    "Failed to delete cloud session %s after creation failed",
                    sid,
                    exc_info=True,
                )
            finally:
                if release is not None:
                    release()

        # Pre-register non-cloud sessions BEFORE issuing the RPC so any
        # session-scoped requests the CLI emits during session.create
        # processing (e.g. sessionFs.writeFile for workspace metadata) can be
        # routed to the correct handlers.
        if local_session_id is not None:
            try:
                initialization_lock = await self._acquire_session_initialization(
                    local_session_id, rpc_client, wait_for_resumes=auto_mode_recovery
                )
                session = _initialize_session(local_session_id)
                replaced_session = self._register_session(session)
                with self._sessions_lock:
                    # A concurrent resume rollback must retain the resident behind this create.
                    self._resume_predecessors[session] = replaced_session
                registered_session_id = local_session_id
            except BaseException:
                if session is not None:
                    session._mark_disconnected()
                if initialization_lock is not None:
                    initialization_lock.release()
                self._unregister_github_token_provider(github_token_provider_registration_id)
                raise

        try:
            rpc_start = time.perf_counter()

            # For the server-assigned (cloud) path, register the session
            # synchronously from the reader thread the instant the response
            # arrives, before the next message can be dispatched. The
            # awaiter's continuation otherwise runs after the event loop has
            # already processed the first session.event notification, which
            # would silently drop because the session id isn't yet
            # registered. Non-cloud sessions are already registered above.
            def _register_inline(raw_response: Any) -> None:
                nonlocal session, registered_session_id, server_assigned_session_id
                nonlocal release_inline_initialization, replaced_session
                if session is not None:
                    return
                if not isinstance(raw_response, dict):
                    return
                sid = raw_response.get("sessionId")
                if isinstance(sid, str) and sid:
                    release = self._reserve_inline_session_initialization(sid, rpc_client)
                    try:
                        initialized = _initialize_session(sid)
                    except Exception:
                        with inline_state_lock:
                            late_failure = request_failed
                            if not late_failure:
                                server_assigned_session_id = sid
                                release_inline_initialization = release
                        if late_failure:
                            cleanup = _delete_uninitialized_session(sid, release)
                            try:
                                asyncio.run_coroutine_threadsafe(cleanup, event_loop)
                            except RuntimeError:
                                cleanup.close()
                                release()
                                logger.warning(
                                    "Failed to schedule cleanup of cloud session %s",
                                    sid,
                                    exc_info=True,
                                )
                        raise
                    with inline_state_lock:
                        late_success = request_failed
                        if not late_success:
                            release_inline_initialization = release
                            try:
                                replaced_session = self._register_session(initialized)
                                with self._sessions_lock:
                                    self._resume_predecessors[initialized] = replaced_session
                            except BaseException:
                                _retire_unreturned_session(initialized)
                                raise
                            session = initialized
                            registered_session_id = sid
                    if late_success:
                        _retire_unreturned_session(initialized)
                        release()

            response = await rpc_client.request(
                "session.create", payload, on_response_inline=_register_inline
            )
            log_timing(
                logger,
                logging.DEBUG,
                "CopilotClient.create_session session creation request completed successfully",
                rpc_start,
                session_id=registered_session_id,
            )
            if session is None:
                raise RuntimeError("session.create response did not include a sessionId")
            if local_session_id is not None and response.get("sessionId") != local_session_id:
                raise RuntimeError(
                    f"session.create returned sessionId {response.get('sessionId')} "
                    f"but the caller requested {local_session_id}"
                )
            self._assign_github_token_provider(
                github_token_provider_registration_id, session.session_id
            )
            capabilities = response.get("capabilities")
            session._set_capabilities(capabilities)
            if auto_mode_recovery:
                delete_on_failure = use_server_generated_id and (
                    (session.capabilities.get("ui") or {}).get("autoModeRecovery") is not True
                )
                await self._require_auto_mode_recovery(session, delete_on_failure=delete_on_failure)
            if on_mcp_auth_request is not None:
                await rpc_client.request(
                    "session.eventLog.registerInterest",
                    {"sessionId": session.session_id, "eventType": "mcp.oauth_required"},
                )
            session._workspace_path = response.get("workspacePath")
            session.permission_mode_result = _permission_mode_result(response)
            await self._apply_post_create_options_patch(
                session,
                mode,
                skip_custom_instructions,
                custom_agents_local_only,
                coauthor_enabled,
                manage_schedule_enabled,
                included_builtin_skills,
                delete_on_failure=use_server_generated_id and auto_mode_recovery,
            )
            self._commit_github_token_provider(
                session.session_id, github_token_provider_registration_id
            )
            self._ahp_hosts.capture(session, payload)
        except BaseException as exc:
            with inline_state_lock:
                request_failed = True
                registered_id = registered_session_id
                orphaned_id = server_assigned_session_id
                failed_session = session
            with self._sessions_lock:
                cleanup_owns_session = (
                    failed_session is not None
                    and registered_id is not None
                    and self._sessions.get(registered_id) is failed_session
                    and registered_id in self._auto_mode_recovery_cleanups.get(rpc_client, {})
                )
            cleanup_error = None
            if (
                use_server_generated_id
                and auto_mode_recovery
                and failed_session is not None
                and not cleanup_owns_session
            ):
                try:
                    await self._cleanup_failed_session(
                        failed_session,
                        delete_on_failure=True,
                        wait=not isinstance(exc, asyncio.CancelledError),
                    )
                except Exception as error:
                    cleanup_error = error
                with self._sessions_lock:
                    cleanup_owns_session = (
                        registered_id is not None
                        and self._sessions.get(registered_id) is failed_session
                        and registered_id in self._auto_mode_recovery_cleanups.get(rpc_client, {})
                    )
            with self._sessions_lock:
                if failed_session is not None:
                    replaced_session = self._rollback_resume_predecessor(failed_session)
                retiring = self._session_admission_closed or self._client is not rpc_client
                if registered_id is not None and not cleanup_owns_session:
                    if self._sessions.get(registered_id) is failed_session:
                        if (
                            not retiring
                            and replaced_session is not None
                            and not replaced_session._destroyed
                        ):
                            self._sessions[registered_id] = replaced_session
                        else:
                            del self._sessions[registered_id]
            if retiring and replaced_session is not None:
                replaced_session._mark_disconnected()
            if failed_session is not None and not cleanup_owns_session:
                failed_session._mark_disconnected()
            if orphaned_id is not None:
                await _delete_uninitialized_session(orphaned_id)
            if not cleanup_owns_session:
                self._unregister_github_token_provider(github_token_provider_registration_id)
            if not isinstance(exc, asyncio.CancelledError):
                log_timing(
                    logger,
                    logging.WARNING,
                    "CopilotClient.create_session failed",
                    total_start,
                    exc_info=True,
                    session_id=registered_session_id,
                )
            if cleanup_error is not None:
                raise exc from cleanup_error
            raise
        finally:
            with self._sessions_lock:
                if session is not None:
                    self._resume_predecessors.pop(session, None)
            if initialization_lock is not None:
                initialization_lock.release()
            with inline_state_lock:
                release = release_inline_initialization
            if release is not None:
                release()

        log_timing(
            logger,
            logging.DEBUG,
            "CopilotClient.create_session complete",
            total_start,
            session_id=registered_session_id,
        )
        return session

    async def resume_session(
        self,
        session_id: str,
        *,
        on_permission_request: _PermissionHandlerFn | None = None,
        model: str | None = None,
        client_name: str | None = None,
        reasoning_effort: ReasoningEffort | None = None,
        allow_multiple_tool_calls: bool | None = None,
        reasoning_summary: ReasoningSummary | None = None,
        enable_experimental_mode: bool | None = None,
        context_tier: ContextTier | None = None,
        tools: list[Tool] | None = None,
        system_message: SystemMessageConfig | None = None,
        tool_search: ToolSearchConfig | None = None,
        image_generation: ImageGenerationConfig | None = None,
        available_tools: list[str] | ToolSet | None = None,
        excluded_tools: list[str] | ToolSet | None = None,
        on_user_input_request: UserInputHandler | None = None,
        ask_user_variant: AskUserVariant | None = None,
        hooks: SessionHooks | None = None,
        working_directory: str | None = None,
        additional_directories: list[str] | None = None,
        provider: ProviderConfig | None = None,
        capi: CapiSessionOptions | None = None,
        providers: list[NamedProviderConfig] | None = None,
        models: list[ProviderModelConfig] | None = None,
        enable_session_telemetry: bool | None = None,
        enable_citations: bool | None = None,
        enable_file_change_tracking: bool | None = None,
        excluded_builtin_agents: list[str] | None = None,
        session_limits: SessionLimitsConfig | None = None,
        autopilot: AutopilotOptions | None = None,
        skip_custom_instructions: bool | None = None,
        custom_agents_local_only: bool | None = None,
        coauthor_enabled: bool | None = None,
        manage_schedule_enabled: bool | None = None,
        model_capabilities: ModelCapabilitiesOverride | None = None,
        streaming: bool | None = None,
        include_sub_agent_streaming_events: bool | None = None,
        mcp_servers: dict[str, MCPServerConfig] | None = None,
        diagnostics: DiagnosticsConfiguration | None = None,
        mcp_oauth_token_storage: Literal["persistent", "in-memory"] | None = None,
        auth_client_id_metadata_url: str | None = None,
        embedding_cache_storage: Literal["persistent", "in-memory"] | None = None,
        custom_agents: list[CustomAgentConfig] | None = None,
        default_agent: DefaultAgentConfig | dict[str, Any] | None = None,
        agent: str | None = None,
        config_directory: str | None = None,
        enable_config_discovery: bool | None = None,
        skip_embedding_retrieval: bool | None = None,
        organization_custom_instructions: str | None = None,
        enable_on_demand_instruction_discovery: bool | None = None,
        enable_file_hooks: bool | None = None,
        enable_host_git_operations: bool | None = None,
        enable_session_store: bool | None = None,
        enable_skills: bool | None = None,
        included_builtin_skills: list[str] | None = None,
        skill_directories: list[str] | None = None,
        skill_provider: SkillProvider | None = None,
        agent_directories: list[str] | None = None,
        hook_directories: list[str] | None = None,
        plugin_directories: list[str] | None = None,
        instruction_directories: list[str] | None = None,
        disabled_skills: list[str] | None = None,
        disabled_mcp_servers: list[str] | None = None,
        sandbox_config: SandboxConfig | None = None,
        infinite_sessions: InfiniteSessionConfig | None = None,
        large_output: LargeToolOutputConfig | None = None,
        memory: MemoryConfiguration | None = None,
        on_event: Callable[[SessionEvent], None] | None = None,
        commands: list[CommandDefinition] | None = None,
        on_elicitation_request: ElicitationHandler | None = None,
        on_mcp_auth_request: McpAuthHandler | None = None,
        enable_mcp_apps: bool = False,
        on_exit_plan_mode_request: ExitPlanModeHandler | None = None,
        on_auto_mode_switch_request: AutoModeSwitchHandler | None = None,
        auto_mode_recovery: bool = False,
        create_session_fs_handler: CreateSessionFsHandler | None = None,
        github_token: str | None = None,
        github_token_provider: GitHubTokenProvider | None = None,
        continue_pending_work: bool | None = None,
        suppress_resume_event: bool | None = None,
        allow_transcript_recovery: bool | None = None,
        canvases: list[CanvasDeclaration] | None = None,
        request_canvas_renderer: bool | None = None,
        request_extensions: bool | None = None,
        extension_sdk_path: str | None = None,
        extension_info: ExtensionInfo | None = None,
        canvas_provider: CanvasProviderIdentity | None = None,
        canvas_handler: CanvasHandler | None = None,
        open_canvases: list[OpenCanvasInstance] | None = None,
        feature_flags: dict[str, bool] | None = None,
        exp_assignments: CopilotExpAssignmentResponse | None = None,
        enable_managed_settings: bool | None = None,
        excluded_managed_settings: list[str] | None = None,
        enforce_managed_model_defaults: bool | None = None,
        interactive_user: bool | None = None,
        permission_mode: PermissionMode | None = None,
        github_mcp_tool_config: GitHubMcpToolConfig | None = None,
        managed_settings: ManagedSettings | None = None,
    ) -> CopilotSession:
        """
        Resume an existing conversation session by its ID.

        This allows you to continue a previous conversation, maintaining all
        conversation history. The session must have been previously created
        and not deleted.

        Args:
            session_id: The ID of the session to resume.
            on_permission_request: Optional handler for permission requests. When
                omitted, permission requests are surfaced as events and left pending
                for the consumer to resolve via the pending permission RPC.
            model: The model to use for the resumed session.
            client_name: Optional client name for identification.
            reasoning_effort: Reasoning effort level for the model.
            allow_multiple_tool_calls: Whether the model may request multiple tool calls per
                response (default True). False is a provider hint, not an execution guarantee;
                all returned calls are handled and handlers may run concurrently.
                Re-supply on cold resume.
            reasoning_summary: Reasoning summary mode for supported models.
                Use ``"none"`` to suppress summary output regardless of whether
                reasoning is enabled.
            enable_experimental_mode: Controls whether the session enables
                experimental features. Defaults to ``False`` in ``"empty"``
                mode; otherwise the runtime decides when omitted.
            coauthor_enabled: Whether to instruct the agent to include a
                ``Co-authored-by`` trailer in commit messages. Subagents inherit this
                setting. Defaults to ``False`` in ``"empty"`` mode; otherwise the
                runtime decides when omitted.
            context_tier: Context window tier for models that support it. Use
                ``"long_context"`` to pin the session to the long-context tier.
            tools: Custom tools to register with the session. An empty list
                replaces the tools previously registered for this session with
                none; ``None`` leaves them unchanged.
            system_message: System message configuration.
            available_tools: Allowlist of tools to enable. When specified, only
                these tools will be available. Applies to the full merged tool
                catalog including built-in tools, MCP tools, and custom tools
                registered via ``tools=``. Custom tool names must be explicitly
                included or they will be hidden from the model. Takes precedence
                over ``excluded_tools``.
            excluded_tools: List of tools to disable. Applies to all tools
                including custom tools registered via ``tools=``. Ignored if
                ``available_tools`` is set.
            on_user_input_request: Handler for user input requests.
            ask_user_variant: Model-facing shape of the ``ask_user`` tool.
                Accepted values are ``"legacy"`` and ``"elicitation"``. The
                default is ``"legacy"``. To use ``"elicitation"``, also provide
                ``on_elicitation_request`` so the host can answer structured forms.
            hooks: Lifecycle hooks for the session.
            working_directory: Working directory for the session.
            provider: Provider configuration for Azure or custom endpoints.
            capi: CAPI provider-scoped options. Omit ``auto_tier`` to preserve the
                current or persisted Auto routing preference. An explicit tier
                overrides it on cold resume, but cannot change it on an
                already-resident session. WebSocket transport is the
                default for the CAPI Responses API whenever the model advertises
                the ``ws:/responses`` endpoint. Set
                ``enable_web_socket_responses=False`` to force the HTTP
                Responses transport, which is useful behind proxies where
                WebSockets fail. This is equivalent to setting the
                ``COPILOT_CLI_DISABLE_WEBSOCKET_RESPONSES`` environment
                variable. The option is under the ``capi`` namespace because a
                single session can host multiple providers (CAPI + BYOK), so
                transport choice is provider-level.
            providers: Named BYOK provider connections. Additive to Copilot API
                auth (unlike `provider`); combine with `models`. Cannot be
                combined with `provider`.
            models: BYOK model definitions added to the selectable model list,
                each referencing a `providers` entry by name.
            enable_session_telemetry: Enables or disables internal session telemetry
                for this session. False disables telemetry; True enables it for eligible
                GitHub-authenticated sessions, including custom providers (BYOK).
                Omission preserves a resident session's choice; otherwise defaults to enabled.
                Host and account policies still control uploads. This is independent of
                client OpenTelemetry configuration.
            enable_citations: **Experimental.** Enables native model citations for
                supported providers.
            enable_file_change_tracking: Opts in to capturing file changes for
                session rewind and cumulative session diff when the resumed session
                has a valid baseline. Earlier untracked changes cannot be reconstructed.
            excluded_builtin_agents: Built-in agent names to exclude from the
                resumed session. Excluded built-in agents are hidden from discovery
                and cannot be selected or invoked unless a custom agent with the
                same name is configured.
            session_limits: **Experimental.** Limits applied to this session's
                current accounting window.
            autopilot: **Experimental.** Patch resident Autopilot policy on warm
                resume without changing mode. Re-supply on cold resume.
            model_capabilities: Override individual model capabilities resolved by the runtime.
            streaming: Whether to enable streaming responses.
            include_sub_agent_streaming_events: Whether to include sub-agent streaming
                delta events (e.g., ``assistant.message_delta``,
                ``assistant.reasoning_delta``, ``assistant.streaming_delta`` with
                ``agentId`` set). When False, only non-streaming sub-agent events and
                ``subagent.*`` lifecycle events are forwarded. Defaults to True.
            mcp_servers: MCP server configurations.
            mcp_oauth_token_storage: Controls how MCP OAuth tokens are stored.
                ``"persistent"`` uses the OS keychain (shared across sessions).
                ``"in-memory"`` stores tokens in memory (discarded on session end).
                Defaults to ``"in-memory"`` for safe multitenant behavior.
            auth_client_id_metadata_url: OAuth Client ID Metadata Document URL
                identifying the host for MCP authorization. Re-supply the same
                host identity used when the session was created.
            embedding_cache_storage: Controls how embedding caches are stored.
                `"persistent"` uses disk-based storage (shared across sessions).
                `"in-memory"` stores embeddings in memory (discarded on session end).
                Defaults to `"in-memory"` in empty mode.
            custom_agents: Custom agent configurations.
            default_agent: Configuration for the default agent,
                including tool visibility controls.
            agent: Agent to use for the session.
            config_directory: Override for the configuration directory.
            enable_config_discovery: Enables runtime discovery of supported
                configuration. Explicitly supplied configuration takes precedence
                over discovered values.
            skip_embedding_retrieval: When True, skips embedding-based retrieval.
            organization_custom_instructions: Organization-level custom instructions.
            enable_on_demand_instruction_discovery: Enables on-demand instruction file
                discovery.
            enable_file_hooks: Enables repository-origin file hooks from ``.github/hooks/``
                and trusted repository-configured roots, not explicit hook_directories
                or callback hooks.
            enable_host_git_operations: Enables git operations on the host filesystem.
            enable_session_store: Enables the cross-session store.
            enable_skills: Enables skill loading.
            skill_directories: Explicit discovery-only skill roots. No general filesystem
                grant. Available without enable_config_discovery, subject to skill/managed
                disables. Relative paths resolve once when supplied against the request's
                effective working_directory; retained roots do not move on cwd changes.
                None and omission are equivalent; [] clears only the explicit SDK roots.
            skill_provider: **Experimental.** Session-scoped provider for SDK-owned
                skills. Providers are not persisted; callers must re-supply the
                provider on resume. Synchronous providers run on the event loop,
                so keep them fast or make them async.
            agent_directories: Explicit discovery-only agent roots, separate from
                custom_agents. Same path and clearing rules as skill_directories. Available
                without enable_config_discovery, subject to source/managed disables.
            hook_directories: Explicit discovery-only file-hook roots, separate from callback
                hooks. Same path and clearing rules as skill_directories. Available without
                enable_config_discovery or enable_file_hooks (repository-origin hooks only);
                hook and managed disables still apply.
            instruction_directories: Explicit discovery-only instruction roots using existing
                SDK formats. Same path and clearing rules as skill_directories. Available
                without enable_config_discovery, subject to instruction/managed disables.
            disabled_skills: Skills to disable.
            disabled_mcp_servers: Exact MCP server names to disable only for this
                session. Disabled servers are not started or authenticated on
                create or cold resume; a resident resume cannot stop servers
                already running. This does not change global MCP settings.
            sandbox_config: Sandbox configuration applied when the session is constructed,
                before MCP servers start. Same shape as the sandboxConfig option of session
                options update; an identical later update is a no-op.
            infinite_sessions: Infinite session configuration.
            memory: Session memory configuration. **Experimental:** set
                ``store_memory`` to store memories in the app instead of the
                built-in memory backends; see :class:`MemoryConfiguration`.
            image_generation: **Experimental.** Image generation opt-in. Omission preserves a
                resident session's setting; re-supply after runtime restart.
            on_event: Callback for session events.
            auto_mode_recovery: Re-enroll in runtime-owned recovery on resume.
                Requires ``on_auto_mode_switch_request`` and a positive runtime acknowledgment.
                See :meth:`create_session` for consent and lifetime requirements.
            enable_mcp_apps: **Experimental.** Opt into MCP Apps (SEP-1865) UI
                passthrough on resume. This parameter is part of an experimental
                wire-protocol surface and may change or be removed in a future
                release. When True, the SDK sends ``requestMcpApps: True`` on
                ``session.resume``. The runtime only honors the opt-in when its
                ``MCP_APPS`` feature flag (or ``COPILOT_MCP_APPS=true`` env
                override) is on; otherwise the request is silently dropped.
                Inspect ``capabilities.ui.mcpApps`` on the resume response to
                detect the drop.
            github_mcp_tool_config: Configuration for the built-in GitHub MCP
                server, sent as ``githubMcpToolConfig`` on ``session.resume``.
                Supports ``enable_all_tools``, ``additional_toolsets``,
                ``additional_tools``, ``enable_insiders_mode``, and
                ``disable_form_deferral``. Setting ``disable_form_deferral``
                makes form-backed GitHub write tools execute directly instead
                of returning an awaiting-form stub; it does not enable MCP Apps
                on its own and has no effect unless MCP Apps are enabled for
                the session (see ``enable_mcp_apps``). Omitted from the wire
                payload entirely when None.
            continue_pending_work: When True, instructs the runtime to continue any
                tool calls or permission prompts that were still pending when the
                session was last suspended. When False (the default), the runtime
                treats pending work as interrupted on resume. Completed tool results
                already durably recorded by the runtime are preserved.
                When True, pending permission requests and external tool calls wait
                for caller-supplied results via the corresponding low-level RPC methods.
                Resuming does not re-emit restored permission requests or re-invoke
                permission handlers or already-dispatched external tool callbacks.
            suppress_resume_event: When True, skips emitting the session.resume
                event when attaching to an existing session. Defaults to False.
            allow_transcript_recovery: Whether to repair a damaged transcript on
                resume. Defaults to True in all modes. Set False to reject
                recovery. Recovery can discard a torn tail; inspect
                ``session.transcript_recovery`` for the reported affected lines.
            feature_flags: Feature-flag values resolved by the host to apply
                on resume. Sent on the wire as ``featureFlags``.
            exp_assignments: ExP assignment ("flight") data injected by a
                trusted integrator, in the same JSON shape the Copilot CLI
                fetches from the experimentation service
                (``CopilotExpAssignmentResponse``). When supplied, the runtime
                feeds it into the same feature-flag path as CLI-fetched
                assignments and stamps it onto telemetry and the CAPI request
                header. When absent, the session does not block on ExP. Intended
                for out-of-process integrators that fetch ExP data themselves;
                malformed payloads are dropped by the runtime (fail-open). This
                is an internal/trusted-integrator option. Sent on the wire as
                ``expAssignments``.
            enable_managed_settings: Opt-in flag. When ``True``, the runtime
                self-fetches enterprise managed settings (bypass-permissions
                policy) at session bootstrap using the session's ``github_token``.
                Requires ``github_token`` to be set; if omitted, the runtime is
                expected to reject session creation (fail-closed). When unset,
                behaves exactly as before. Sent on the wire as
                ``enableManagedSettings``.
            excluded_managed_settings: Exact dotted managed-setting paths to exclude.
                Same semantics as :meth:`create_session`; omitting restores full
                enforcement, including when resuming a live session.
            enforce_managed_model_defaults: When ``True``, locked managed model
                controls are enforced after resume. Conflicting model changes are
                rejected, while overridable managed defaults remain mutable. Sent
                on the wire as ``enforceManagedModelDefaults``.
            interactive_user: Re-supplies that a human drives this session so a
                session loaded from storage keeps the declaration. It is not
                persisted and is ignored if the session is already active in the
                runtime process.
            permission_mode: Permission mode applied before resumed pending work
                continues and before this method returns. If it is refused (for
                example by managed policy), continued work runs under the mode
                ``session.permission_mode_result`` reports. Same checks and
                semantics as in :meth:`create_session`. Omit to keep the
                restored mode. Sent on the wire as ``permissionMode``.
            managed_settings: Host-injected enterprise managed settings for the
                session. Must be re-supplied on resume; it replaces the prior
                injected layer, and omitting it clears that layer so warm and
                cold resume behave identically. See :meth:`create_session`. Sent
                on the wire as ``managedSettings``.
            github_token_provider: Callback that acquires a short-lived GitHub
                credential for this resumed session. Mutually exclusive with
                ``github_token``. The new registration replaces the prior
                provider only after resume succeeds.

        Returns:
            A :class:`CopilotSession` instance for the resumed session.

        Raises:
            RuntimeError: If the session does not exist or the client is not connected.
            ValueError: If ``on_permission_request`` is not a valid callable.

        Note:
            A session retained by the filesystem factory is retired if initialization
            or the resume RPC fails or is cancelled. A failed resume preserves the
            previously registered session.

        Example:
            >>> session = await client.resume_session(
            ...     "session-123",
            ...     on_permission_request=PermissionHandler.approve_all,
            ... )
            >>>
            >>> # Resume with new tools
            >>> session = await client.resume_session(
            ...     "session-123",
            ...     on_permission_request=PermissionHandler.approve_all,
            ...     tools=[my_new_tool],
            ... )
        """
        if auto_mode_recovery and not callable(on_auto_mode_switch_request):
            raise ValueError("auto_mode_recovery requires on_auto_mode_switch_request")
        if on_permission_request is not None and not callable(on_permission_request):
            raise ValueError("on_permission_request must be callable when provided.")
        if github_token is not None and github_token_provider is not None:
            raise ValueError("github_token and github_token_provider are mutually exclusive")
        if ask_user_variant not in (None, "legacy", "elicitation"):
            raise ValueError('ask_user_variant must be "legacy" or "elicitation"')
        store_memory = _requested_memory_store(memory)
        # Wait for in-progress startup too: its provider registrations must precede any session.
        if not self._client or self._start_lock.locked():
            await self.start()

        tool_defs = [definition.to_dict() for definition in _tools_to_wire_definitions(tools)]

        # Empty-mode validation and normalization
        mode = self._options.mode
        _require_available_tools_for_empty_mode(mode, _normalize_tool_filter(available_tools))
        available_tools = _normalize_tool_filter(available_tools)
        excluded_tools = _normalize_tool_filter(excluded_tools)
        _validate_tool_filter_list("available_tools", available_tools)
        _validate_tool_filter_list("excluded_tools", excluded_tools)
        system_message = _system_message_for_mode(mode, system_message)
        enable_session_telemetry = _enable_session_telemetry_default(mode, enable_session_telemetry)
        skip_embedding_retrieval = _skip_embedding_retrieval_default(mode, skip_embedding_retrieval)
        memory = _memory_default(mode, memory)
        enable_on_demand_instruction_discovery = _enable_on_demand_instruction_discovery_default(
            mode, enable_on_demand_instruction_discovery
        )
        enable_file_hooks = _enable_file_hooks_default(mode, enable_file_hooks)
        enable_host_git_operations = _enable_host_git_operations_default(
            mode, enable_host_git_operations
        )
        enable_session_store = _enable_session_store_default(mode, enable_session_store)
        enable_skills = _enable_skills_default(mode, enable_skills)
        custom_agents_local_only = _custom_agents_local_only_default(mode, custom_agents_local_only)
        enable_experimental_mode = _enable_experimental_mode_default(mode, enable_experimental_mode)

        payload: dict[str, Any] = {"sessionId": session_id}

        if client_name:
            payload["clientName"] = client_name
        if model:
            payload["model"] = model
        if reasoning_effort:
            payload["reasoningEffort"] = reasoning_effort
        if allow_multiple_tool_calls is not None:
            payload["allowMultipleToolCalls"] = allow_multiple_tool_calls
        if reasoning_summary:
            payload["reasoningSummary"] = reasoning_summary
        if enable_experimental_mode is not None:
            payload["isExperimentalMode"] = enable_experimental_mode
        if context_tier:
            payload["contextTier"] = context_tier
        if tools is not None:
            payload["tools"] = tool_defs
        wire_system_message, transform_callbacks = _extract_transform_callbacks(system_message)
        if wire_system_message:
            payload["systemMessage"] = wire_system_message
        if tool_search is not None:
            payload["toolSearch"] = _tool_search_to_wire(tool_search)
        if image_generation is not None:
            payload["imageGeneration"] = _image_generation_to_wire(image_generation)
        if available_tools is not None:
            payload["availableTools"] = available_tools
        if excluded_tools is not None:
            payload["excludedTools"] = excluded_tools
        payload["toolFilterPrecedence"] = "excluded"
        if provider:
            payload["provider"] = self._convert_provider_to_wire_format(provider)
        if capi is not None:
            payload["capi"] = _capi_session_options_to_wire(capi)
        if providers:
            payload["providers"] = [
                self._convert_named_provider_to_wire_format(p) for p in providers
            ]
        if models:
            payload["models"] = [self._convert_model_to_wire_format(m) for m in models]
        if enable_session_telemetry is not None:
            payload["enableSessionTelemetry"] = enable_session_telemetry
        if enable_citations is not None:
            payload["enableCitations"] = enable_citations
        if enable_file_change_tracking is not None:
            payload["enableFileChangeTracking"] = enable_file_change_tracking
        if excluded_builtin_agents is not None:
            payload["excludedBuiltinAgents"] = excluded_builtin_agents
        if session_limits is not None:
            payload["sessionLimits"] = _session_limits_to_wire(session_limits)
        if autopilot is not None:
            payload["autopilot"] = _autopilot_to_wire(autopilot)
        if model_capabilities:
            payload["modelCapabilities"] = _capabilities_to_dict(model_capabilities)
        if streaming is not None:
            payload["streaming"] = streaming

        # Include sub-agent streaming events (defaults to True)
        payload["includeSubAgentStreamingEvents"] = (
            include_sub_agent_streaming_events
            if include_sub_agent_streaming_events is not None
            else True
        )

        # Opt this connection into gitHubTelemetry.event notifications when a
        # telemetry handler was registered on the client.
        if self._on_github_telemetry is not None:
            payload["enableGitHubTelemetryForwarding"] = True

        # Enable permission request callback if handler provided
        payload["requestPermission"] = bool(on_permission_request)

        if on_user_input_request:
            payload["requestUserInput"] = True
        if ask_user_variant is not None:
            payload["askUserVariant"] = ask_user_variant

        # Enable elicitation request callback if handler provided
        payload["requestElicitation"] = bool(on_elicitation_request)
        if enable_mcp_apps:
            payload["requestMcpApps"] = True
        if github_mcp_tool_config is not None:
            payload["githubMcpToolConfig"] = _github_mcp_tool_config_to_wire(github_mcp_tool_config)
        payload["requestExitPlanMode"] = bool(on_exit_plan_mode_request)
        payload["requestAutoModeSwitch"] = bool(on_auto_mode_switch_request)
        if auto_mode_recovery:
            payload["requestAutoModeRecovery"] = True

        # Serialize commands (name + description only) into payload
        if commands is not None:
            payload["commands"] = [
                {"name": cmd.name, "description": cmd.description or ""} for cmd in commands
            ]

        if hooks and any(hooks.values()):
            payload["hooks"] = True

        # Add GitHub token for per-session authentication
        if github_token is not None:
            payload["gitHubToken"] = github_token

        if feature_flags is not None:
            payload["featureFlags"] = feature_flags

        # Add ExP assignment data if provided (trusted integrator)
        if exp_assignments is not None:
            payload["expAssignments"] = _exp_assignment_response_to_dict(exp_assignments)

        # Opt the runtime into self-fetching enterprise managed settings
        if enable_managed_settings is not None:
            payload["enableManagedSettings"] = enable_managed_settings
        if excluded_managed_settings is not None:
            payload["excludedManagedSettings"] = excluded_managed_settings
        if enforce_managed_model_defaults is not None:
            payload["enforceManagedModelDefaults"] = enforce_managed_model_defaults

        if interactive_user is not None:
            payload["interactiveUser"] = interactive_user

        if permission_mode is not None:
            payload["permissionMode"] = PermissionMode(permission_mode).value

        # Host-injected managed settings (permissions-only contract)
        if managed_settings is not None:
            payload["managedSettings"] = _managed_settings_to_dict(managed_settings)

        if working_directory:
            payload["workingDirectory"] = working_directory
        if additional_directories is not None:
            payload["additionalDirectories"] = additional_directories
        if config_directory:
            payload["configDir"] = config_directory
        if enable_config_discovery is not None:
            payload["enableConfigDiscovery"] = enable_config_discovery
        if skip_embedding_retrieval is not None:
            payload["skipEmbeddingRetrieval"] = skip_embedding_retrieval
        if organization_custom_instructions is not None:
            payload["organizationCustomInstructions"] = organization_custom_instructions
        if enable_on_demand_instruction_discovery is not None:
            payload["enableOnDemandInstructionDiscovery"] = enable_on_demand_instruction_discovery
        if enable_file_hooks is not None:
            payload["enableFileHooks"] = enable_file_hooks
        if enable_host_git_operations is not None:
            payload["enableHostGitOperations"] = enable_host_git_operations
        if enable_session_store is not None:
            payload["enableSessionStore"] = enable_session_store
        if enable_skills is not None:
            payload["enableSkills"] = enable_skills
        if skill_provider is not None:
            payload["hasSkillProvider"] = True
        if store_memory is not None:
            payload["hasMemoryStore"] = True

        if continue_pending_work is not None:
            payload["continuePendingWork"] = continue_pending_work
        if allow_transcript_recovery is not None:
            payload["allowTranscriptRecovery"] = allow_transcript_recovery

        if suppress_resume_event is not None:
            payload["disableResume"] = suppress_resume_event

        if mcp_servers is not None:
            payload["mcpServers"] = _mcp_servers_to_wire(mcp_servers)
        if diagnostics is not None:
            payload["diagnostics"] = diagnostics.to_dict()
        # Mode "empty" defaults MCP OAuth token storage to in-memory; caller wins.
        mcp_oauth_token_storage = _mcp_oauth_token_storage_default(mode, mcp_oauth_token_storage)
        if mcp_oauth_token_storage is not None:
            payload["mcpOAuthTokenStorage"] = mcp_oauth_token_storage
        if auth_client_id_metadata_url is not None:
            payload["authClientIdMetadataUrl"] = auth_client_id_metadata_url
        embedding_cache_storage = _embedding_cache_storage_default(mode, embedding_cache_storage)
        if embedding_cache_storage is not None:
            payload["embeddingCacheStorage"] = embedding_cache_storage
        payload["envValueMode"] = "direct"

        if custom_agents is not None:
            payload["customAgents"] = [
                self._convert_custom_agent_to_wire_format(a) for a in custom_agents
            ]
        if custom_agents_local_only is not None:
            payload["customAgentsLocalOnly"] = custom_agents_local_only

        # Add default agent configuration if provided
        if default_agent:
            payload["defaultAgent"] = self._convert_default_agent_to_wire_format(default_agent)

        if agent:
            payload["agent"] = agent
        if skill_directories is not None:
            payload["skillDirectories"] = skill_directories
        if agent_directories is not None:
            payload["agentDirectories"] = agent_directories
        if hook_directories is not None:
            payload["hookDirectories"] = hook_directories
        if plugin_directories:
            payload["pluginDirectories"] = plugin_directories
        if instruction_directories is not None:
            payload["instructionDirectories"] = instruction_directories
        if disabled_skills:
            payload["disabledSkills"] = disabled_skills
        if disabled_mcp_servers is not None:
            payload["disabledMcpServers"] = disabled_mcp_servers
        if sandbox_config is not None:
            payload["sandboxConfig"] = sandbox_config.to_dict()

        if infinite_sessions:
            wire_config: dict[str, Any] = {}
            if "enabled" in infinite_sessions:
                wire_config["enabled"] = infinite_sessions["enabled"]
            if "background_compaction_threshold" in infinite_sessions:
                wire_config["backgroundCompactionThreshold"] = infinite_sessions[
                    "background_compaction_threshold"
                ]
            if "buffer_exhaustion_threshold" in infinite_sessions:
                wire_config["bufferExhaustionThreshold"] = infinite_sessions[
                    "buffer_exhaustion_threshold"
                ]
            payload["infiniteSessions"] = wire_config

        if large_output is not None:
            payload["largeOutput"] = _large_output_to_wire(large_output)

        if memory is not None:
            payload["memory"] = _memory_to_wire(memory)

        if canvases:
            payload["canvases"] = [c.to_dict() for c in canvases]
        if open_canvases:
            payload["openCanvases"] = [inst.to_dict() for inst in open_canvases]
        if request_canvas_renderer is not None:
            payload["requestCanvasRenderer"] = request_canvas_renderer
        if request_extensions is not None:
            payload["requestExtensions"] = request_extensions
        if extension_sdk_path is not None:
            payload["extensionSdkPath"] = extension_sdk_path
        if extension_info is not None:
            payload["extensionInfo"] = extension_info.to_dict()
        if canvas_provider is not None:
            payload["canvasProvider"] = canvas_provider.to_dict()

        if not self._client:
            raise RuntimeError("Client not connected")

        rpc_client = self._client
        total_start = time.perf_counter()
        # Propagate W3C Trace Context to CLI if OpenTelemetry is active
        trace_ctx = get_trace_context()
        payload.update(trace_ctx)

        # Create and register the session before issuing the RPC so that
        # events emitted by the CLI (e.g. session.start) are not dropped.
        setup_start = time.perf_counter()
        operation_session_id = session_id
        initialization_lock = None
        if (
            auto_mode_recovery
            and not self._session_fs_config
            and _is_abbreviated_uuid_prefix(session_id)
        ):
            initialization_lock = await self._acquire_session_initialization(
                operation_session_id, rpc_client, wait_for_resumes=auto_mode_recovery
            )
            try:
                # Resolve persisted uniqueness without changing the resident callbacks.
                # Pin the full ID so resume cannot resolve an ambiguous prefix later.
                resolved = await ServerRpc(rpc_client).sessions.find_by_prefix(
                    SessionsFindByPrefixRequest(prefix=session_id, config_dir=config_directory)
                )
                if not resolved.session_id:
                    raise RuntimeError(f"no unique session matches prefix {session_id!r}")
                session_id = resolved.session_id
                payload["sessionId"] = session_id
            except BaseException:
                initialization_lock.release()
                raise
        elif auto_mode_recovery and not self._session_fs_config:
            session_id = self._resident_session_id(session_id, rpc_client)
        session = None
        try:
            session = CopilotSession(
                session_id,
                rpc_client,
                workspace_path=None,
                managed_settings_enabled=enable_managed_settings is True
                or managed_settings is not None,
            )
            if self._session_fs_config:
                if create_session_fs_handler is None:
                    raise ValueError(
                        "create_session_fs_handler is required in session config when "
                        "session_fs is enabled in client options."
                    )
                fs_provider: SessionFsProvider = create_session_fs_handler(session)
                caps = self._session_fs_config.get("capabilities")
                if (
                    caps
                    and caps.get("read_file_range")
                    and not isinstance(fs_provider, SessionFsReadFileRangeProvider)
                ):
                    raise ValueError(
                        "SessionFs capabilities declare byte-range reads but the provider "
                        "does not implement SessionFsReadFileRangeProvider"
                    )
                if caps and caps.get("sqlite"):
                    from .session_fs_provider import SessionFsSqliteProvider

                    if not isinstance(fs_provider, SessionFsSqliteProvider):
                        raise ValueError(
                            "SessionFs capabilities declare SQLite support but the provider "
                            "does not implement SessionFsSqliteProvider"
                        )
                if caps and caps.get("binary"):
                    from .session_fs_provider import SessionFsBinaryProvider

                    if not isinstance(fs_provider, SessionFsBinaryProvider):
                        raise ValueError(
                            "SessionFs capabilities declare binary support but the provider "
                            "does not implement SessionFsBinaryProvider"
                        )
                session._client_session_apis.session_fs = create_session_fs_adapter(fs_provider)
            session._register_tools(tools)
            session._register_commands(commands)
            session._register_skill_provider(skill_provider)
            session._register_memory_store(store_memory)
            session._register_permission_handler(on_permission_request)
            session._register_mcp_auth_handler(on_mcp_auth_request)
            if on_user_input_request:
                session._register_user_input_handler(on_user_input_request)
            if on_elicitation_request:
                session._register_elicitation_handler(on_elicitation_request)
            if on_exit_plan_mode_request:
                session._register_exit_plan_mode_handler(on_exit_plan_mode_request)
            if on_auto_mode_switch_request:
                session._register_auto_mode_switch_handler(on_auto_mode_switch_request)
            if canvas_handler is not None:
                session._register_canvas_handler(canvas_handler)
            session._register_bearer_token_providers(
                _collect_bearer_token_callbacks(provider, providers)
            )
            if hooks:
                session._register_hooks(hooks)
            if transform_callbacks:
                session._register_transform_callbacks(transform_callbacks)
            if on_event:
                session.on(on_event)
            if initialization_lock is None:
                initialization_lock = await self._acquire_session_initialization(
                    operation_session_id, rpc_client, wait_for_resumes=auto_mode_recovery
                )
            log_timing(
                logger,
                logging.DEBUG,
                "CopilotClient.resume_session local setup complete",
                setup_start,
                session_id=session_id,
                tools_count=len(tools or []),
                commands_count=len(commands or []),
                has_hooks=hooks is not None,
            )
            github_token_provider_registration_id = self._register_github_token_provider(
                github_token_provider, session_id
            )
            if github_token_provider_registration_id is not None:
                payload["gitHubTokenProviderRegistrationId"] = github_token_provider_registration_id
                session._set_disconnect_callback(
                    lambda: self._unregister_github_token_provider(
                        github_token_provider_registration_id
                    )
                )
            replaced_session = self._register_session(session)
            with self._sessions_lock:
                self._resume_predecessors[session] = replaced_session
        except BaseException:
            if session is not None:
                session._mark_disconnected()
            if initialization_lock is not None:
                initialization_lock.release()
            raise

        ordinary_resume: asyncio.Future[None] | None = None
        ordinary_resume_session_id = operation_session_id
        if not auto_mode_recovery:
            # Ordinary resumes keep their existing concurrent callback-registration behavior.
            ordinary_resume = asyncio.get_running_loop().create_future()
            with self._sessions_lock:
                self._ordinary_session_resumes.setdefault(operation_session_id, set()).add(
                    ordinary_resume
                )
            initialization_lock.release()
            initialization_lock = None
        keep_resumed_session_on_recovery_rejection = False
        try:
            rpc_start = time.perf_counter()
            response = await rpc_client.request("session.resume", payload)
            canonical_id = response.get("sessionId")
            if isinstance(canonical_id, str) and canonical_id and canonical_id != session_id:
                # Resolve admission before publishing the canonical owner. Never
                # remove an attachment that replaced this provisional registration.
                if not _session_ids_overlap(operation_session_id, canonical_id):
                    if initialization_lock is not None:
                        initialization_lock.release()
                        initialization_lock = None
                    initialization_lock = await self._acquire_session_initialization(
                        canonical_id, rpc_client, wait_for_resumes=auto_mode_recovery
                    )
                with self._sessions_lock:
                    replaced_session = self._rollback_resume_predecessor(session)
                    if self._sessions.get(session_id) is session:
                        if replaced_session is None:
                            del self._sessions[session_id]
                        else:
                            self._sessions[session_id] = replaced_session
                    self._resume_predecessors[session] = None
                    if ordinary_resume is not None:
                        resumes = self._ordinary_session_resumes[ordinary_resume_session_id]
                        resumes.remove(ordinary_resume)
                        if not resumes:
                            del self._ordinary_session_resumes[ordinary_resume_session_id]
                        self._ordinary_session_resumes.setdefault(canonical_id, set()).add(
                            ordinary_resume
                        )
                        ordinary_resume_session_id = canonical_id
                session_id = canonical_id
                session.session_id = canonical_id
                session._rpc = None
                replaced_session = self._register_session(session)
                with self._sessions_lock:
                    self._resume_predecessors[session] = replaced_session
                self._assign_github_token_provider(
                    github_token_provider_registration_id, canonical_id
                )
                if ordinary_resume is not None and initialization_lock is not None:
                    initialization_lock.release()
                    initialization_lock = None
            log_timing(
                logger,
                logging.DEBUG,
                "CopilotClient.resume_session session resume request completed successfully",
                rpc_start,
                session_id=session_id,
            )
            session._workspace_path = response.get("workspacePath")
            session.permission_mode_result = _permission_mode_result(response)
            recovery = response.get("transcriptRecovery")
            if recovery is not None:
                session.transcript_recovery = TranscriptRecoveryReport(
                    planned_backup_path=recovery["plannedBackupPath"],
                    invalid_line_numbers=recovery["invalidLineNumbers"],
                    session_start_moved=recovery["sessionStartMoved"],
                )
            capabilities = response.get("capabilities")
            session._set_capabilities(capabilities)
            if auto_mode_recovery:
                keep_resumed_session_on_recovery_rejection = (
                    replaced_session is not None
                    and ((capabilities or {}).get("ui") or {}).get("autoModeRecovery") is not True
                )
                await self._require_auto_mode_recovery(
                    session, detach_on_failure=replaced_session is None
                )
            open_canvases_raw = response.get("openCanvases")
            if isinstance(open_canvases_raw, list):
                session._set_open_canvases(
                    [OpenCanvasInstance.from_dict(inst) for inst in open_canvases_raw]
                )
            if on_mcp_auth_request is not None:
                await rpc_client.request(
                    "session.eventLog.registerInterest",
                    {"sessionId": session.session_id, "eventType": "mcp.oauth_required"},
                )
            await self._apply_post_create_options_patch(
                session,
                mode,
                skip_custom_instructions,
                custom_agents_local_only,
                coauthor_enabled,
                manage_schedule_enabled,
                included_builtin_skills,
            )
            self._commit_github_token_provider(session_id, github_token_provider_registration_id)
            self._ahp_hosts.capture(session, payload)
        except BaseException as exc:
            # The runtime keeps the resident session's bindings when a resume
            # fails, except when a successful resume was rejected only because
            # the runtime did not acknowledge Auto recovery.
            with self._sessions_lock:
                replaced_session = self._rollback_resume_predecessor(session)
                retiring = self._session_admission_closed or self._client is not rpc_client
                cleanup_owns_session = self._sessions.get(
                    session_id
                ) is session and session_id in self._auto_mode_recovery_cleanups.get(rpc_client, {})
                if self._sessions.get(session_id) is session and not cleanup_owns_session:
                    if (
                        not retiring
                        and replaced_session is not None
                        and not replaced_session._destroyed
                        and not keep_resumed_session_on_recovery_rejection
                    ):
                        self._sessions[session_id] = replaced_session
                    elif not keep_resumed_session_on_recovery_rejection:
                        del self._sessions[session_id]
                restored = self._sessions.get(session_id) is replaced_session
                owner = self._sessions.get(session_id)
                retained = False
                while owner is not None and owner in self._resume_predecessors:
                    owner = self._resume_predecessors[owner]
                    if owner is replaced_session:
                        retained = True
                        break
            if (
                not restored
                and not retained
                and (retiring or not keep_resumed_session_on_recovery_rejection)
                and replaced_session is not None
                and replaced_session._client is rpc_client
            ):
                replaced_session._mark_disconnected()
            if keep_resumed_session_on_recovery_rejection:
                self._commit_github_token_provider(
                    session_id, github_token_provider_registration_id
                )
            elif not cleanup_owns_session:
                session._mark_disconnected()
                self._unregister_github_token_provider(github_token_provider_registration_id)
            if not isinstance(exc, asyncio.CancelledError):
                log_timing(
                    logger,
                    logging.WARNING,
                    "CopilotClient.resume_session failed",
                    total_start,
                    exc_info=True,
                    session_id=session_id,
                )
            raise
        finally:
            with self._sessions_lock:
                self._resume_predecessors.pop(session, None)
            if ordinary_resume is not None:
                with self._sessions_lock:
                    resumes = self._ordinary_session_resumes[ordinary_resume_session_id]
                    resumes.remove(ordinary_resume)
                    if not resumes:
                        del self._ordinary_session_resumes[ordinary_resume_session_id]
                ordinary_resume.set_result(None)
            if initialization_lock is not None:
                initialization_lock.release()

        log_timing(
            logger,
            logging.DEBUG,
            "CopilotClient.resume_session complete",
            total_start,
            session_id=session_id,
        )
        return session

    async def start_ahp_host(self, options: AhpHostOptions) -> AhpHost:
        """Start an experimental, connection-owned in-process AHP listener.

        Factories preserve host-selected settings while adding application
        callbacks. Stopping the host releases participation, not session ownership.
        If canceled during startup, cleanup continues on the owning connection
        and disposes the listener once startup settles.
        """
        from .host import _validate_options

        _validate_options(options)
        if self._state != "connected":
            await self.start()
        if self._client is None:
            raise RuntimeError("Client not connected")
        return await self._ahp_hosts.start(self._client, options)

    async def ping(self, message: str | None = None) -> PingResponse:
        """
        Send a ping request to the server to verify connectivity.

        Args:
            message: Optional message to include in the ping.

        Returns:
            A PingResponse object containing the ping response.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> response = await client.ping("health check")
            >>> print(f"Server responded at {response.timestamp}")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        result = await self._client.request("ping", {"message": message})
        return PingResponse.from_dict(result)

    async def get_status(self) -> GetStatusResponse:
        """
        Get CLI status including version and protocol information.

        Returns:
            A GetStatusResponse object containing version and protocolVersion.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> status = await client.get_status()
            >>> print(f"CLI version: {status.version}")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        result = await self._client.request("status.get", {})
        return GetStatusResponse.from_dict(result)

    async def get_auth_status(self) -> GetAuthStatusResponse:
        """
        Get current authentication status.

        Returns:
            A GetAuthStatusResponse object containing authentication state.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> auth = await client.get_auth_status()
            >>> if auth.isAuthenticated:
            ...     print(f"Logged in as {auth.login}")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        result = await self._client.request("auth.getStatus", {})
        return GetAuthStatusResponse.from_dict(result)

    async def list_models(self) -> list[ModelInfo]:
        """
        List available models with their metadata.

        Results are cached after the first successful call to avoid rate limiting.
        The cache is cleared when the client disconnects.

        If a custom ``on_list_models`` handler was provided in the client options,
        it is called instead of querying the CLI server. The handler may be sync
        or async.

        Returns:
            A list of ModelInfo objects with model details.

        Raises:
            RuntimeError: If the client is not connected (when no custom handler is set).
            Exception: If not authenticated.

        Example:
            >>> models = await client.list_models()
            >>> for model in models:
            ...     print(f"{model.id}: {model.name}")
        """
        # Use asyncio lock to prevent race condition with concurrent calls
        async with self._models_cache_lock:
            # Check cache (already inside lock)
            if self._models_cache is not None:
                return list(self._models_cache)  # Return a copy to prevent cache mutation

            if self._on_list_models:
                # Use custom handler instead of CLI RPC
                result = self._on_list_models()
                if inspect.isawaitable(result):
                    models = cast(list[ModelInfo], await result)
                else:
                    models = cast(list[ModelInfo], result)
            else:
                if not self._client:
                    raise RuntimeError("Client not connected")

                # Cache miss - fetch from backend while holding lock
                response = await self._client.request("models.list", {})
                models_data = response.get("models", [])
                models = [ModelInfo.from_dict(model) for model in models_data]

            # Update cache before releasing lock (copy to prevent external mutation)
            self._models_cache = list(models)

            return list(models)  # Return a copy to prevent cache mutation

    async def list_sessions(self, filter: SessionListFilter | None = None) -> list[SessionMetadata]:
        """
        List all available sessions known to the server.

        Returns metadata about each session including ID, timestamps, and summary.

        Args:
            filter: Optional filter to narrow down the list of sessions by working directory,
                git root, repository, or branch.

        Returns:
            A list of SessionMetadata objects.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> sessions = await client.list_sessions()
            >>> for session in sessions:
            ...     print(f"Session: {session.session_id}")
            >>> # Filter sessions by repository
            >>> from copilot.client import SessionListFilter
            >>> filtered = await client.list_sessions(SessionListFilter(repository="owner/repo"))
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        payload: dict = {}
        if filter is not None:
            payload["filter"] = filter.to_dict()

        response = await self._client.request("session.list", payload)
        sessions_data = response.get("sessions", [])
        return [SessionMetadata.from_dict(session) for session in sessions_data]

    async def get_session_metadata(self, session_id: str) -> SessionMetadata | None:
        """
        Get metadata for a specific session by ID.

        This provides an efficient O(1) lookup of a single session's metadata
        instead of listing all sessions. Returns None if the session is not found.

        Args:
            session_id: The ID of the session to look up.

        Returns:
            A SessionMetadata object, or None if the session was not found.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> metadata = await client.get_session_metadata("session-123")
            >>> if metadata:
            ...     print(f"Session started at: {metadata.start_time}")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        response = await self._client.request("session.getMetadata", {"sessionId": session_id})
        session_data = response.get("session")
        if session_data is None:
            return None
        return SessionMetadata.from_dict(session_data)

    async def delete_session(self, session_id: str) -> None:
        """
        Permanently delete a session and all its data from disk, including
        conversation history, planning state, and artifacts.

        Unlike :meth:`CopilotSession.disconnect`, which only releases in-memory
        resources and preserves session data for later resumption, this method
        is irreversible. The session cannot be resumed after deletion.

        Args:
            session_id: The ID of the session to delete.

        Raises:
            RuntimeError: If the client is not connected or deletion fails.

        Example:
            >>> await client.delete_session("session-123")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        rpc_client = self._client
        initialization = await self._acquire_session_initialization(
            session_id, rpc_client, wait_for_resumes=True
        )
        try:
            response = await rpc_client.request("session.delete", {"sessionId": session_id})

            success = response.get("success", False)
            if not success:
                error = response.get("error", "Unknown error")
                raise RuntimeError(f"Failed to delete session {session_id}: {error}")

            with self._sessions_lock:
                session = self._sessions.get(session_id)
                if session is not None and session._client is rpc_client:
                    del self._sessions[session_id]
                else:
                    session = None
            if session is not None:
                session._mark_disconnected()
        finally:
            initialization.release()

    async def get_last_session_id(self) -> str | None:
        """
        Get the ID of the most recently updated session.

        This is useful for resuming the last conversation when the session ID
        was not stored.

        Returns:
            The session ID, or None if no sessions exist.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> last_id = await client.get_last_session_id()
            >>> if last_id:
            ...     config = {"on_permission_request": PermissionHandler.approve_all}
            ...     session = await client.resume_session(last_id, config)
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        response = await self._client.request("session.getLastId", {})
        return response.get("sessionId")

    async def get_foreground_session_id(self) -> str | None:
        """
        Get the ID of the session currently displayed in the TUI.

        This is only available when connecting to a server running in TUI+server mode
        (--ui-server).

        Returns:
            The session ID, or None if no foreground session is set.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> session_id = await client.get_foreground_session_id()
            >>> if session_id:
            ...     print(f"TUI is displaying session: {session_id}")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        response = await self._client.request("session.getForeground", {})
        return response.get("sessionId")

    async def set_foreground_session_id(self, session_id: str) -> None:
        """
        Request the TUI to switch to displaying the specified session.

        This is only available when connecting to a server running in TUI+server mode
        (--ui-server).

        Args:
            session_id: The ID of the session to display in the TUI.

        Raises:
            RuntimeError: If the client is not connected or the operation fails.

        Example:
            >>> await client.set_foreground_session_id("session-123")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        response = await self._client.request("session.setForeground", {"sessionId": session_id})

        success = response.get("success", False)
        if not success:
            error = response.get("error", "Unknown error")
            raise RuntimeError(f"Failed to set foreground session: {error}")

    @overload
    def on_lifecycle(self, handler: SessionLifecycleHandler, /) -> HandlerUnsubcribe:
        pass

    @overload
    def on_lifecycle(
        self, event_type: SessionLifecycleEventType, /, handler: SessionLifecycleHandler
    ) -> HandlerUnsubcribe:
        pass

    def on_lifecycle(
        self,
        event_type_or_handler: SessionLifecycleEventType | SessionLifecycleHandler,
        /,
        handler: SessionLifecycleHandler | None = None,
    ) -> HandlerUnsubcribe:
        """
        Subscribe to session lifecycle events.

        Lifecycle events are emitted when sessions are created, deleted, updated,
        or change foreground/background state (in TUI+server mode).

        Can be called in two ways:
        - on_lifecycle(handler): Subscribe to all lifecycle events
        - on_lifecycle(event_type, handler): Subscribe to a specific event type

        Args:
            event_type_or_handler: Either a specific event type to listen for,
                or a handler function for all events.
            handler: Handler function when subscribing to a specific event type.

        Returns:
            A function that, when called, unsubscribes the handler.

        Example:
            >>> # Subscribe to specific event type
            >>> unsubscribe = client.on_lifecycle(
            ...     "session.foreground", lambda e: print(e.session_id)
            ... )
            >>>
            >>> # Subscribe to all events
            >>> unsubscribe = client.on_lifecycle(lambda e: print(f"{e.type}: {e.session_id}"))
            >>>
            >>> # Later, to stop receiving events:
            >>> unsubscribe()
        """
        with self._lifecycle_handlers_lock:
            if callable(event_type_or_handler) and handler is None:
                # Wildcard subscription: on(handler)
                wildcard_handler = event_type_or_handler
                self._lifecycle_handlers.append(wildcard_handler)

                def unsubscribe_wildcard() -> None:
                    with self._lifecycle_handlers_lock:
                        if wildcard_handler in self._lifecycle_handlers:
                            self._lifecycle_handlers.remove(wildcard_handler)

                return unsubscribe_wildcard
            elif isinstance(event_type_or_handler, str) and handler is not None:
                # Typed subscription: on(event_type, handler)
                event_type = cast(SessionLifecycleEventType, event_type_or_handler)
                if event_type not in self._typed_lifecycle_handlers:
                    self._typed_lifecycle_handlers[event_type] = []
                self._typed_lifecycle_handlers[event_type].append(handler)

                def unsubscribe_typed() -> None:
                    with self._lifecycle_handlers_lock:
                        handlers = self._typed_lifecycle_handlers.get(event_type, [])
                        if handler in handlers:
                            handlers.remove(handler)

                return unsubscribe_typed
            else:
                raise ValueError(
                    "Invalid arguments: use on_lifecycle(handler) "
                    "or on_lifecycle(event_type, handler)"
                )

    def _dispatch_lifecycle_event(self, event: SessionLifecycleEvent) -> None:
        """Dispatch a lifecycle event to all registered handlers."""
        with self._lifecycle_handlers_lock:
            # Copy handlers to avoid holding lock during callbacks
            typed_handlers = list(self._typed_lifecycle_handlers.get(event.type, []))
            wildcard_handlers = list(self._lifecycle_handlers)

        # Dispatch to typed handlers
        for handler in typed_handlers:
            try:
                handler(event)
            except Exception:
                pass  # Ignore handler errors

        # Dispatch to wildcard handlers
        for handler in wildcard_handlers:
            try:
                handler(event)
            except Exception:
                pass  # Ignore handler errors

    async def _verify_protocol_version(self) -> None:
        """Send the ``connect`` handshake (with the optional token) and verify
        the server's protocol version. Falls back to ``ping`` for legacy servers
        that don't implement ``connect``."""
        if not self._client:
            raise RuntimeError("Client not connected")
        handshake_start = time.perf_counter()
        used_fallback_ping = False
        max_version = get_sdk_protocol_version()

        server_version: int | None
        try:
            connect_params: dict[str, Any] = {
                "supportedTaskKinds": ["agent", "client", "shell"],
            }
            if self._options.entra is not None:
                connect_params["entra"] = self._options.entra.to_dict()
            if self._entra_token_provider is not None:
                connect_params["entraTokenProvider"] = True
            if self._effective_connection_token is not None:
                connect_params["token"] = self._effective_connection_token
            # Opt in to GitHub telemetry forwarding at the connection level when a
            # handler is registered (mirrors the runtime, which reads this flag on the
            # `connect` handshake so the first session's un-replayable `session.start`
            # event is forwarded). Also sent on session.create/resume for older CLIs.
            if self._on_github_telemetry is not None:
                connect_params["enableGitHubTelemetryForwarding"] = True
            # Declare the integrating application's identity so the runtime attributes
            # the telemetry it emits on this connection to a consistent surface
            # instead of its own build. Omitted when the app didn't supply it.
            client_info = _client_info_to_wire(self._options.client_info)
            if client_info is not None:
                connect_params["clientInfo"] = client_info
            connect_result = _ConnectResult.from_dict(
                await self._client.request("connect", connect_params)
            )
            server_version = connect_result.protocol_version
            if (
                self._entra_token_provider is not None
                and connect_result.entra_token_provider is not True
            ):
                raise RuntimeError(
                    "This runtime does not support host-owned Entra token providers."
                )
        except JsonRpcError as err:
            if err.code == -32601 or err.message == "Unhandled method connect":
                if self._options.entra is not None:
                    raise RuntimeError(
                        "This runtime does not support Entra client configuration."
                    ) from err
                # Legacy server without `connect`; fall back to `ping`. A token, if any,
                # is silently dropped — the legacy server can't enforce one.
                used_fallback_ping = True
                ping_result = await self.ping()
                server_version = ping_result.protocol_version
            else:
                raise

        if server_version is None:
            raise RuntimeError(
                "SDK protocol version mismatch: "
                f"SDK supports versions {_MIN_PROTOCOL_VERSION}-{max_version}"
                ", but server does not report a protocol version. "
                "Please update your server to ensure compatibility."
            )

        if server_version < _MIN_PROTOCOL_VERSION or server_version > max_version:
            raise RuntimeError(
                "SDK protocol version mismatch: "
                f"SDK supports versions {_MIN_PROTOCOL_VERSION}-{max_version}"
                f", but server reports version {server_version}. "
                "Please update your SDK or server to ensure compatibility."
            )

        self._negotiated_protocol_version = server_version
        log_timing(
            logger,
            logging.DEBUG,
            "CopilotClient._verify_protocol_version protocol handshake complete",
            handshake_start,
            protocol_version=server_version,
            used_fallback_ping=used_fallback_ping,
        )

    def _convert_provider_to_wire_format(
        self, provider: ProviderConfig | dict[str, Any]
    ) -> dict[str, Any]:
        """
        Convert provider config from snake_case to camelCase wire format.

        Args:
            provider: The provider configuration in snake_case format.

        Returns:
            The provider configuration in camelCase wire format.
        """
        wire_provider: dict[str, Any] = {"type": provider.get("type")}
        if "base_url" in provider:
            wire_provider["baseUrl"] = provider["base_url"]
        if "api_key" in provider:
            wire_provider["apiKey"] = provider["api_key"]
        if "api_key_command" in provider:
            wire_provider["apiKeyCommand"] = provider["api_key_command"]
        if "wire_api" in provider:
            wire_provider["wireApi"] = provider["wire_api"]
        if "transport" in provider:
            wire_provider["transport"] = provider["transport"]
        if "model_provider" in provider:
            wire_provider["modelProvider"] = provider["model_provider"]
        if "bearer_token" in provider:
            wire_provider["bearerToken"] = provider["bearer_token"]
        if provider.get("bearer_token_provider") is not None:
            wire_provider["hasBearerTokenProvider"] = True
        if "headers" in provider:
            wire_provider["headers"] = provider["headers"]
        if "model_id" in provider:
            wire_provider["modelId"] = provider["model_id"]
        if "wire_model" in provider:
            wire_provider["wireModel"] = provider["wire_model"]
        if "max_prompt_tokens" in provider:
            wire_provider["maxPromptTokens"] = provider["max_prompt_tokens"]
        if "max_context_window_tokens" in provider:
            wire_provider["maxContextWindowTokens"] = provider["max_context_window_tokens"]
        if "max_output_tokens" in provider:
            wire_provider["maxOutputTokens"] = provider["max_output_tokens"]
        if "azure" in provider:
            azure = provider["azure"]
            wire_azure: dict[str, Any] = {}
            if "api_version" in azure:
                wire_azure["apiVersion"] = azure["api_version"]
            if wire_azure:
                wire_provider["azure"] = wire_azure
        return wire_provider

    def _convert_named_provider_to_wire_format(
        self, provider: NamedProviderConfig | dict[str, Any]
    ) -> dict[str, Any]:
        """Convert a named BYOK provider from snake_case to camelCase wire format."""
        wire: dict[str, Any] = {}
        if "name" in provider:
            wire["name"] = provider["name"]
        if "type" in provider:
            wire["type"] = provider["type"]
        if "wire_api" in provider:
            wire["wireApi"] = provider["wire_api"]
        if "model_provider" in provider:
            wire["modelProvider"] = provider["model_provider"]
        if "base_url" in provider:
            wire["baseUrl"] = provider["base_url"]
        if "api_key" in provider:
            wire["apiKey"] = provider["api_key"]
        if "bearer_token" in provider:
            wire["bearerToken"] = provider["bearer_token"]
        if provider.get("bearer_token_provider") is not None:
            wire["hasBearerTokenProvider"] = True
        if "headers" in provider:
            wire["headers"] = provider["headers"]
        if "azure" in provider:
            azure = provider["azure"]
            wire_azure: dict[str, Any] = {}
            if "api_version" in azure:
                wire_azure["apiVersion"] = azure["api_version"]
            if wire_azure:
                wire["azure"] = wire_azure
        return wire

    def _convert_model_to_wire_format(
        self, model: ProviderModelConfig | dict[str, Any]
    ) -> dict[str, Any]:
        """Convert a BYOK model definition from snake_case to camelCase wire format."""
        wire: dict[str, Any] = {}
        if "id" in model:
            wire["id"] = model["id"]
        if "provider" in model:
            wire["provider"] = model["provider"]
        if "wire_model" in model:
            wire["wireModel"] = model["wire_model"]
        if "model_id" in model:
            wire["modelId"] = model["model_id"]
        if "name" in model:
            wire["name"] = model["name"]
        if "max_prompt_tokens" in model:
            wire["maxPromptTokens"] = model["max_prompt_tokens"]
        if "max_context_window_tokens" in model:
            wire["maxContextWindowTokens"] = model["max_context_window_tokens"]
        if "max_output_tokens" in model:
            wire["maxOutputTokens"] = model["max_output_tokens"]
        if "capabilities" in model:
            wire["capabilities"] = _capabilities_to_dict(model["capabilities"])
        return wire

    def _convert_custom_agent_to_wire_format(
        self, agent: CustomAgentConfig | dict[str, Any]
    ) -> dict[str, Any]:
        """
        Convert custom agent config from snake_case to camelCase wire format.

        Args:
            agent: The custom agent configuration in snake_case format.

        Returns:
            The custom agent configuration in camelCase wire format.
        """
        wire_agent: dict[str, Any] = {"name": agent.get("name"), "prompt": agent.get("prompt")}
        if "hooks" in agent:
            wire_agent["hooks"] = agent["hooks"]
        if "display_name" in agent:
            wire_agent["displayName"] = agent["display_name"]
        if "description" in agent:
            wire_agent["description"] = agent["description"]
        if "tools" in agent:
            wire_agent["tools"] = agent["tools"]
        if "mcp_servers" in agent:
            wire_agent["mcpServers"] = _mcp_servers_to_wire(agent["mcp_servers"])
        if "infer" in agent:
            wire_agent["infer"] = agent["infer"]
        if "skills" in agent:
            wire_agent["skills"] = agent["skills"]
        if "model" in agent:
            wire_agent["model"] = agent["model"]
        if "model_policy" in agent:
            wire_agent["modelPolicy"] = agent["model_policy"]
        if "reasoning_effort" in agent:
            wire_agent["reasoningEffort"] = agent["reasoning_effort"]
        return wire_agent

    def _convert_default_agent_to_wire_format(
        self, config: DefaultAgentConfig | dict[str, Any]
    ) -> dict[str, Any]:
        """
        Convert default agent config from snake_case to camelCase wire format.

        Args:
            config: The default agent configuration in snake_case format.

        Returns:
            The default agent configuration in camelCase wire format.
        """
        wire: dict[str, Any] = {}
        if "excluded_tools" in config:
            wire["excludedTools"] = config["excluded_tools"]
        return wire

    async def _start_cli_server(self) -> None:
        """Start the runtime process.

        This spawns the runtime as a subprocess using the configured transport
        mode (stdio or TCP), or hosts it in-process for the FFI transport.

        Raises:
            RuntimeError: If the server fails to start or times out.
        """
        if isinstance(self._connection, InProcessRuntimeConnection):
            await self._start_inprocess_ffi()
            return

        assert isinstance(self._connection, ChildProcessRuntimeConnection)
        conn = self._connection
        opts = self._options
        use_stdio = isinstance(conn, StdioRuntimeConnection)
        tcp_port = conn.port if isinstance(conn, TcpRuntimeConnection) else 0

        cli_path = conn.path
        assert cli_path is not None  # resolved in __init__

        # Verify CLI exists
        if not os.path.exists(cli_path):
            original_path = cli_path
            if (cli_path := shutil.which(cli_path)) is None:
                raise RuntimeError(f"Copilot CLI not found at {original_path}")

        # Start with user-provided args, then add SDK-managed args
        args = list(conn.args) + [
            "--headless",
            "--no-auto-update",
            "--log-level",
            opts.log_level,
        ]

        # Add auth-related flags
        if opts.github_token:
            args.extend(["--auth-token-env", "COPILOT_SDK_AUTH_TOKEN"])
        if not opts.use_logged_in_user:
            args.append("--no-auto-login")

        if opts.session_idle_timeout_seconds is not None and opts.session_idle_timeout_seconds > 0:
            args.extend(["--session-idle-timeout", str(opts.session_idle_timeout_seconds)])

        # If cli_path is a .js file, run it with node
        # Note that we can't rely on the shebang as Windows doesn't support it
        if cli_path.endswith(".js"):
            args = ["node", cli_path] + args
        else:
            args = [cli_path] + args
        # Windows refuses to start a program named only by the command line once its
        # path reaches MAX_PATH, even with long paths enabled. Naming it as the
        # executable passes it as CreateProcess's application name, which has no such
        # limit. Only the runtime this SDK provisioned is started this way.
        executable = (
            cli_path if self._cli_path_source == "downloaded" and args[0] == cli_path else None
        )
        logger.info(
            "CopilotClient._start_cli_server starting Copilot CLI",
            extra={
                "cli_path": cli_path,
                "executable": args[0],
                "cli_path_source": self._cli_path_source,
                "use_stdio": use_stdio,
                "port": None if use_stdio else tcp_port,
            },
        )

        # Get environment variables. Per-connection env (ChildProcessRuntimeConnection.env)
        # takes precedence over the client-level env; the constructor already rejects
        # setting both. When neither is set, inherit the current process environment.
        conn_env = conn.env if isinstance(conn, ChildProcessRuntimeConnection) else None
        if conn_env is not None:
            env = dict(conn_env)
        elif opts.env is None:
            env = dict(os.environ)
        else:
            env = dict(opts.env)
        if opts.internal_test_options is not None:
            env.update(opts.internal_test_options._to_environment())
        # Set auth token in environment if provided
        if opts.github_token:
            env["COPILOT_SDK_AUTH_TOKEN"] = opts.github_token

        # Mode "empty": disable the runtime's system keychain probe so per-tenant
        # credentials don't leak through a shared keytar store.
        if opts.mode == "empty":
            env["COPILOT_DISABLE_KEYTAR"] = "1"
        if opts.mode != "empty":
            env["COPILOT_RUNTIME_PROCESS_FILE_LOGGING"] = "1"

        if self._effective_connection_token:
            env["COPILOT_CONNECTION_TOKEN"] = self._effective_connection_token
        if opts.base_directory:
            env["COPILOT_HOME"] = opts.base_directory

        # Set OpenTelemetry environment variables if telemetry config is provided
        telemetry = opts.telemetry
        if telemetry is not None:
            env["COPILOT_OTEL_ENABLED"] = "true"
            if "otlp_endpoint" in telemetry:
                env["OTEL_EXPORTER_OTLP_ENDPOINT"] = telemetry["otlp_endpoint"]
            if "otlp_protocol" in telemetry:
                env["OTEL_EXPORTER_OTLP_PROTOCOL"] = telemetry["otlp_protocol"]
            if "file_path" in telemetry:
                env["COPILOT_OTEL_FILE_EXPORTER_PATH"] = telemetry["file_path"]
            if "exporter_type" in telemetry:
                env["COPILOT_OTEL_EXPORTER_TYPE"] = telemetry["exporter_type"]
            if "source_name" in telemetry:
                env["COPILOT_OTEL_SOURCE_NAME"] = telemetry["source_name"]
            if "capture_content" in telemetry:
                env["OTEL_INSTRUMENTATION_GENAI_CAPTURE_CONTENT"] = (
                    "SPAN_ONLY" if telemetry["capture_content"] else "NO_CONTENT"
                )
                env["OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"] = str(
                    telemetry["capture_content"]
                ).lower()

        # On Windows, hide the console window to avoid distracting users in GUI apps
        creationflags = subprocess.CREATE_NO_WINDOW if sys.platform == "win32" else 0

        cwd = opts.working_directory or os.getcwd()

        # Choose transport mode
        spawn_start = time.perf_counter()
        if use_stdio:
            args.append("--stdio")
            # Use regular Popen with pipes (buffering=0 for unbuffered)
            self._process = subprocess.Popen(
                args,
                executable=executable,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                bufsize=0,
                cwd=cwd,
                env=env,
                creationflags=creationflags,
            )
            self._cli_process = self._process
        else:
            if tcp_port > 0:
                args.extend(["--port", str(tcp_port)])
            self._process = subprocess.Popen(
                args,
                executable=executable,
                stdin=subprocess.DEVNULL,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                cwd=cwd,
                env=env,
                creationflags=creationflags,
            )
            self._cli_process = self._process
        log_timing(
            logger,
            logging.DEBUG,
            "CopilotClient._start_cli_server subprocess spawned",
            spawn_start,
        )

        # For stdio mode, we're ready immediately
        if use_stdio:
            return

        # For TCP mode, wait for port announcement
        loop = asyncio.get_event_loop()
        process = self._process  # Capture for closure

        # Nothing else reads the child's stderr in TCP mode, and a full pipe blocks the
        # CLI, so drain it for the child's whole life, logging it as the stdio transport
        # does. Keep a bounded tail for both startup and later RPC failures.
        stderr_output = ""
        stderr_lock = threading.Lock()
        stderr = process.stderr
        assert stderr is not None  # spawned with stderr=subprocess.PIPE

        def drain_stderr() -> None:
            nonlocal stderr_output
            decoder = codecs.getincrementaldecoder("utf-8")(errors="replace")
            partial_line = ""
            streaming_line = False
            try:
                while True:
                    chunk = os.read(stderr.fileno(), 4096)
                    text = decoder.decode(chunk, final=not chunk)
                    with stderr_lock:
                        stderr_output = (stderr_output + text)[-64 * 1024 :]
                    lines = (partial_line + text).split("\n")
                    for line in lines[:-1]:
                        logger.warning("[CLI] %s", line.rstrip())
                        streaming_line = False
                    partial_line = lines[-1]
                    # Preserve short lines across reads, but never let an unterminated
                    # diagnostic grow the buffer or stop being logged once it is long.
                    if partial_line and (streaming_line or len(partial_line) >= 4096 or not chunk):
                        logger.warning("[CLI] %s", partial_line.rstrip())
                        partial_line = ""
                        streaming_line = True
                    if not chunk:
                        break
            except Exception:
                logger.debug("Error reading Copilot CLI stderr", exc_info=True)

        stderr_thread = threading.Thread(target=drain_stderr, daemon=True)
        stderr_thread.start()

        def with_stderr(message: str) -> str:
            with stderr_lock:
                output = stderr_output.strip()
            if output:
                message += f"\nstderr: {output}"
            return message

        def process_exit_error(*, before_port: bool = False) -> str:
            return_code = process.poll()
            if return_code is None:
                # Socket/stdout EOF can precede child exit. Bound the grace period
                # so a disconnected but live child cannot hold pending RPCs forever.
                try:
                    return_code = process.wait(timeout=1.0)
                except subprocess.TimeoutExpired:
                    if not before_port:
                        return with_stderr("Connection to CLI server closed unexpectedly")
                    return with_stderr("CLI process closed stdout before announcing port")
            stderr_thread.join(timeout=1.0)
            message = (
                "CLI process exited before announcing port" if before_port else "CLI process exited"
            )
            if return_code is not None:
                message += (
                    f" (exit code {return_code})" if before_port else f" with code {return_code}"
                )
            return with_stderr(message)

        self._tcp_process_exit_error = process_exit_error

        def startup_exit_error() -> str:
            return process_exit_error(before_port=True)

        async def read_port():
            if not process or not process.stdout:
                raise RuntimeError("Process not started or stdout not available")
            while True:
                line = await loop.run_in_executor(None, process.stdout.readline)
                if not line:
                    raise RuntimeError(await loop.run_in_executor(None, startup_exit_error))

                line_str = line.decode() if isinstance(line, bytes) else line
                logger.debug("[CLI] %s", line_str.rstrip())
                match = re.search(r"listening on port (\d+)", line_str, re.IGNORECASE)
                if match:
                    self._runtime_port = int(match.group(1))
                    return

        try:
            port_wait_start = time.perf_counter()
            await asyncio.wait_for(read_port(), timeout=10.0)
            log_timing(
                logger,
                logging.DEBUG,
                "CopilotClient._start_cli_server TCP port wait complete",
                port_wait_start,
                port=self._runtime_port,
            )
        except TimeoutError:
            raise RuntimeError(with_stderr("Timeout waiting for CLI server to start"))

    async def _start_inprocess_ffi(self) -> None:
        """Host the runtime in-process via the native FFI library.

        Loads the native runtime library and opens the FFI JSON-RPC connection.

        Raises:
            RuntimeError: If the native library is missing or startup fails.
        """
        assert isinstance(self._connection, InProcessRuntimeConnection)
        runtime_path = self._inprocess_runtime_path
        assert runtime_path is not None  # resolved in __init__

        logger.info(
            "CopilotClient._start_inprocess_ffi hosting Copilot runtime in-process",
            extra={"runtime_path": runtime_path, "runtime_path_source": self._cli_path_source},
        )

        opts = self._options
        args: list[str] = ["--log-level", opts.log_level]
        if opts.github_token:
            args.extend(["--auth-token-env", "COPILOT_SDK_AUTH_TOKEN"])
        if not opts.use_logged_in_user:
            args.append("--no-auto-login")
        if opts.session_idle_timeout_seconds is not None and opts.session_idle_timeout_seconds > 0:
            args.extend(["--session-idle-timeout", str(opts.session_idle_timeout_seconds)])
        if self._cli_login_sync_integration_id is not None:
            args.append(f"--cli-login-sync-integration-id={self._cli_login_sync_integration_id}")

        environment = {}
        for key, value in os.environ.items():
            try:
                key.encode("utf-8")
            except UnicodeEncodeError:
                continue
            if sys.platform == "win32":
                key = key.upper()
            try:
                value.encode("utf-8")
            except UnicodeEncodeError:
                # Match the native startup snapshot's explicit-setting precedence.
                if key not in {
                    "http_proxy",
                    "HTTP_PROXY",
                    "https_proxy",
                    "HTTPS_PROXY",
                    "COPILOT_PROXY_KERBEROS_SPN",
                    "TMPDIR",
                }:
                    continue
                value = "\ufffd"
            environment[key] = value
        if opts.internal_test_options is not None:
            environment.update(
                {
                    key.upper() if sys.platform == "win32" else key: value
                    for key, value in opts.internal_test_options._to_environment().items()
                }
            )
        if opts.github_token:
            environment["COPILOT_SDK_AUTH_TOKEN"] = opts.github_token
        if opts.base_directory:
            environment["COPILOT_HOME"] = opts.base_directory
            environment["COPILOT_CACHE_HOME"] = str(Path(opts.base_directory) / "cache")
        if opts.mode == "empty":
            environment["COPILOT_DISABLE_KEYTAR"] = "1"

        host = FfiRuntimeHost.create(
            runtime_path,
            cli_entrypoint=self._inprocess_cli_entrypoint,
            environment=environment or None,
            args=tuple(args),
        )

        # Track the host and expose its process-like adapter *before* the blocking
        # handshake. asyncio.to_thread keeps running host_start after a cancellation
        # (a thread can't be interrupted), and CancelledError bypasses start()'s
        # `except Exception`, so assigning here — as .NET does before StartAsync —
        # keeps a completed native host owned so stop()/force_stop() can dispose it
        # instead of leaking it.
        self._ffi_host = host
        self._process = host.process

        ffi_start = time.perf_counter()
        # Native startup may block, so run the handshake off the event loop.
        await asyncio.to_thread(host.start_blocking)
        log_timing(
            logger,
            logging.DEBUG,
            "CopilotClient._start_inprocess_ffi FFI host started",
            ffi_start,
        )

    async def _connect_to_server(self) -> None:
        """Connect to the runtime via the configured transport.

        Uses either stdio or TCP based on the client configuration.

        Raises:
            RuntimeError: If the connection fails.
        """
        setup_start = time.perf_counter()
        if isinstance(self._connection, (StdioRuntimeConnection, InProcessRuntimeConnection)):
            # The in-process FFI host exposes a process-like adapter (stdin/stdout),
            # so the same stdio JSON-RPC wiring drives it unchanged.
            await self._connect_via_stdio()
        else:
            await self._connect_via_tcp()
        log_timing(
            logger,
            logging.DEBUG,
            "CopilotClient._connect_to_server transport setup complete",
            setup_start,
        )

    async def _connect_via_stdio(self) -> None:
        """
        Connect to the CLI server via stdio pipes.

        Creates a JSON-RPC client using the CLI process's stdin/stdout.

        Raises:
            RuntimeError: If the CLI process is not started.
        """
        if not self._process:
            raise RuntimeError("CLI process not started")

        # Create JSON-RPC client with the process
        self._client = JsonRpcClient(self._process)
        self._client.on_close = self._handle_connection_close
        self._rpc = ServerRpc(self._client)

        self._client.set_notification_handler(self._handle_notification)
        self._client.set_request_handler("userInput.request", self._handle_user_input_request)
        self._client.set_request_handler(
            "exitPlanMode.request", self._handle_exit_plan_mode_request
        )
        self._client.set_request_handler(
            "autoModeSwitch.request", self._handle_auto_mode_switch_request
        )
        self._client.set_request_handler(
            "systemMessage.transform", self._handle_system_message_transform
        )
        self._register_session_scoped_request_handlers()
        self._register_client_session_api_handlers()
        self._register_client_global_handlers()
        self._register_session_callback_scopes()

        # Start listening for messages
        loop = asyncio.get_running_loop()
        self._client.start(loop)

    async def _connect_via_tcp(self) -> None:
        """
        Connect to the CLI server via TCP socket.

        Creates a TCP connection to the server at the configured host and port.

        Raises:
            RuntimeError: If the server port is not available or connection fails.
        """
        if not self._runtime_port:
            raise RuntimeError("Server port not available")

        # Create a TCP socket connection with timeout. create_connection resolves
        # both IPv4 and IPv6 addresses instead of forcing AF_INET.
        import socket

        # Connection timeout constant
        TCP_CONNECTION_TIMEOUT = 10  # seconds

        try:
            tcp_connect_start = time.perf_counter()
            logger.info(
                "CopilotClient._connect_via_tcp connecting to CLI server",
                extra={"host": self._actual_host, "port": self._runtime_port},
            )
            sock = socket.create_connection(
                (self._actual_host, self._runtime_port), timeout=TCP_CONNECTION_TIMEOUT
            )
            sock.settimeout(None)  # Remove timeout after connection
            log_timing(
                logger,
                logging.DEBUG,
                "CopilotClient._connect_via_tcp TCP connect complete",
                tcp_connect_start,
                host=self._actual_host,
                port=self._runtime_port,
            )
        except OSError as e:
            message = (
                f"Failed to connect to CLI server at {self._actual_host}:{self._runtime_port}: {e}"
            )
            if self._tcp_process_exit_error is not None:
                message += "\n" + await asyncio.to_thread(self._tcp_process_exit_error)
            raise RuntimeError(message) from e

        # Create a file-like wrapper for the socket
        sock_file = sock.makefile("rwb", buffering=0)

        # Create a mock process object that JsonRpcClient expects
        class SocketWrapper:
            def __init__(self, sock_file, sock_obj):
                self.stdin = sock_file
                self.stdout = sock_file
                self.stderr = None
                self._socket = sock_obj

            def terminate(self):
                import socket as _socket_mod

                # shutdown() sends TCP FIN to the server (triggering
                # server-side disconnect detection) and interrupts any
                # pending blocking reads on other threads immediately.
                try:
                    self._socket.shutdown(_socket_mod.SHUT_RDWR)
                except OSError:
                    pass  # Safe to ignore — socket may already be closed
                # Close the file wrapper — makefile() holds its own
                # reference to the fd, so socket.close() alone won't
                # release the OS resource until the wrapper is closed too.
                try:
                    self.stdin.close()
                except OSError:
                    pass  # Safe to ignore — already closed
                try:
                    self._socket.close()
                except OSError:
                    pass  # Safe to ignore — already closed

            def kill(self):
                self.terminate()

            def wait(self, timeout=None):
                pass

        self._process = SocketWrapper(sock_file, sock)
        self._client = JsonRpcClient(self._process, process_exit_error=self._tcp_process_exit_error)
        self._client.on_close = self._handle_connection_close
        self._rpc = ServerRpc(self._client)

        self._client.set_notification_handler(self._handle_notification)
        self._client.set_request_handler("userInput.request", self._handle_user_input_request)
        self._client.set_request_handler(
            "exitPlanMode.request", self._handle_exit_plan_mode_request
        )
        self._client.set_request_handler(
            "autoModeSwitch.request", self._handle_auto_mode_switch_request
        )
        self._client.set_request_handler(
            "systemMessage.transform", self._handle_system_message_transform
        )
        self._register_session_scoped_request_handlers()
        self._register_client_session_api_handlers()
        self._register_client_global_handlers()
        self._register_session_callback_scopes()

        # Start listening for messages
        loop = asyncio.get_running_loop()
        self._client.start(loop)

    async def _apply_post_create_options_patch(
        self,
        session: CopilotSession,
        mode: CopilotClientMode,
        skip_custom_instructions: bool | None,
        custom_agents_local_only: bool | None,
        coauthor_enabled: bool | None,
        manage_schedule_enabled: bool | None,
        included_builtin_skills: list[str] | None = None,
        *,
        delete_on_failure: bool = False,
    ) -> None:
        """Apply empty-mode safe defaults (or caller-supplied overrides in
        copilot-cli mode) via ``session.options.update`` after create/resume.

        If the patch is rejected, tear the session down so empty-mode callers
        never end up with a permissive session.
        """
        from .generated.rpc import SessionInstalledPlugin, SessionUpdateOptionsParams

        patch = _post_create_options_patch(
            mode,
            skip_custom_instructions,
            custom_agents_local_only,
            coauthor_enabled,
            manage_schedule_enabled,
            included_builtin_skills,
        )
        if patch is None:
            return

        params = SessionUpdateOptionsParams()
        if "skipCustomInstructions" in patch:
            params.skip_custom_instructions = patch["skipCustomInstructions"]
        if "customAgentsLocalOnly" in patch:
            params.custom_agents_local_only = patch["customAgentsLocalOnly"]
        if "coauthorEnabled" in patch:
            params.coauthor_enabled = patch["coauthorEnabled"]
        if "manageScheduleEnabled" in patch:
            params.manage_schedule_enabled = patch["manageScheduleEnabled"]
        if "installedPlugins" in patch:
            params.installed_plugins = [
                SessionInstalledPlugin.from_dict(p) if isinstance(p, dict) else p
                for p in patch["installedPlugins"]
            ]
        if "includedBuiltinSkills" in patch:
            skills = patch["includedBuiltinSkills"]
            params.included_builtin_skills = list(skills) if skills is not None else None

        try:
            await session.rpc.options.update(params)
        except BaseException as error:
            try:
                await self._cleanup_failed_session(
                    session,
                    delete_on_failure=delete_on_failure,
                    wait=not (delete_on_failure and isinstance(error, asyncio.CancelledError)),
                )
            except BaseException as cleanup_error:
                logger.debug(
                    "Error disconnecting session after options update failure",
                    exc_info=True,
                )
                raise error from cleanup_error
            raise

    async def _set_session_fs_provider(self) -> None:
        if not self._session_fs_config or not self._client:
            return

        params: dict[str, Any] = {
            "initialCwd": self._session_fs_config["initial_working_directory"],
            "sessionStatePath": self._session_fs_config["session_state_path"],
            "conventions": self._session_fs_config["conventions"],
        }
        if "capabilities" in self._session_fs_config:
            capabilities = dict(self._session_fs_config["capabilities"])
            if capabilities.pop("read_file_range", False):
                capabilities["readFileRange"] = True
            params["capabilities"] = capabilities

        await self._client.request("sessionFs.setProvider", params)

    def _register_client_global_handlers(self) -> None:
        if not self._client:
            return
        self._llm_inference_adapter = None
        self._installation_confirmation_adapter = None
        if self._request_handler is not None:
            self._llm_inference_adapter = create_copilot_request_adapter(
                self._request_handler,
                lambda: self._rpc.llm_inference if self._rpc is not None else None,
            )
        github_telemetry_adapter = None
        if self._on_github_telemetry is not None:
            github_telemetry_adapter = _GitHubTelemetryAdapter(self._on_github_telemetry)
        network_adapter = self._network_adapter = (
            _NetworkAdapter(self._network)
            if self._network is not None and self._network_registration is not None
            else None
        )
        register_client_global_api_handlers(
            self._client,
            ClientGlobalApiHandlers(
                hooks=_HooksAdapter(self._get_session),
                extension_launch_provider=self._options.extension_launch_provider,
                network=network_adapter,
                llm_inference=self._llm_inference_adapter,
                git_hub_telemetry=github_telemetry_adapter,
                git_hub_token=self._github_token_provider_adapter,
                ui_thread=_UiThreadAdapter(self),
            ),
        )
        # The runtime sends ``$/cancelRequest`` when it stops waiting for an answer; a
        # cancellable handler lets that cancel an async callback still in flight.
        if network_adapter is not None:
            self._client.set_request_handler(
                "network.resolveProxy", network_adapter.handle_resolve_proxy, cancellable=True
            )
            self._client.set_request_handler(
                "network.authenticateNegotiate",
                network_adapter.handle_authenticate_negotiate,
                cancellable=True,
            )
        if self._ffi_host is not None:
            self._ffi_host.configure_ui_thread(
                isinstance(self._connection, InProcessRuntimeConnection)
                and self._connection.run_on_ui_thread is not None
            )
        self._installation_confirmation_adapter = _InstallationConfirmationAdapter(
            self._client,
            self._options.installation_confirmation_handler,
        )
        self._installation_confirmation_adapter.register()
        if self._entra_token_provider is not None:
            self._entra_token_adapter = _EntraTokenAdapter(self._client, self._entra_token_provider)
            self._entra_token_adapter.register()
        client = self._client
        entra_token_adapter = self._entra_token_adapter
        installation_confirmation_adapter = self._installation_confirmation_adapter

        def handle_connection_close() -> None:
            if entra_token_adapter is not None:
                entra_token_adapter.close_connection()
            installation_confirmation_adapter.close_connection()
            if network_adapter is not None:
                # Cancel this connection's own callbacks: a reconnect may already have
                # replaced the client's adapter.
                loop = client._loop
                if loop is not None and not loop.is_closed():
                    try:
                        loop.call_soon_threadsafe(network_adapter.cancel_pending)
                    except RuntimeError:
                        logger.debug("Event loop closed while handling connection loss")
            if self._client is client:
                self._handle_connection_close()

        client.on_close = handle_connection_close
        self._client.set_request_handler("host.materializeSession", self._ahp_hosts.materialize)
        self._client.set_request_handler(
            "host.validateAuthentication", self._ahp_hosts.validate_authentication, ordered=True
        )
        self._client.set_notification_method_handler(
            "host.authenticationCancelled",
            lambda params: self._ahp_hosts.notification("host.authenticationCancelled", params),
        )
        self._client.set_notification_method_handler(
            "host.sessionReleased",
            lambda params: self._ahp_hosts.notification("host.sessionReleased", params),
        )
        self._client.set_notification_method_handler(
            "host.exited", lambda params: self._ahp_hosts.notification("host.exited", params)
        )

    def _register_github_token_provider(
        self, provider: GitHubTokenProvider | None, session_id: str | None
    ) -> str | None:
        if provider is None:
            return None
        registration_id = str(uuid.uuid4())
        with self._github_token_providers_lock:
            self._github_token_providers[registration_id] = _GitHubTokenProviderRegistration(
                provider, session_id
            )
        return registration_id

    def _handle_connection_close(self) -> None:
        self._state = "disconnected"
        with self._sessions_lock:
            self._session_admission_closed = True
            if self._client is not None:
                self._auto_mode_recovery_cleanups.pop(self._client, None)
            sessions = list(self._sessions.values())
        with self._github_token_providers_lock:
            self._github_token_providers.clear()
        for session in sessions:
            session._clear_skill_provider()
            session._register_memory_store(None)
        client = self._client
        llm_inference_adapter = self._llm_inference_adapter
        loop = client._loop if client is not None else None
        if loop is not None and not loop.is_closed():

            def cancel_pending_external_tools() -> None:
                self._ahp_hosts.disconnect()
                if llm_inference_adapter is not None:
                    llm_inference_adapter.cancel_pending()
                for session in sessions:
                    session._cancel_pending_external_tools()

            try:
                loop.call_soon_threadsafe(cancel_pending_external_tools)
            except RuntimeError:
                logger.debug("Event loop closed while handling connection loss")

    def _assign_github_token_provider(self, registration_id: str | None, session_id: str) -> None:
        if registration_id is None:
            return
        with self._github_token_providers_lock:
            registration = self._github_token_providers.get(registration_id)
            if registration is not None:
                registration.session_id = session_id

    def _unregister_github_token_provider(self, registration_id: str | None) -> None:
        if registration_id is None:
            return
        with self._github_token_providers_lock:
            self._github_token_providers.pop(registration_id, None)

    def _commit_github_token_provider(self, session_id: str, registration_id: str | None) -> None:
        with self._github_token_providers_lock:
            stale = [
                candidate_id
                for candidate_id, registration in self._github_token_providers.items()
                if registration.session_id == session_id and registration.committed
            ]
            for candidate_id in stale:
                self._github_token_providers.pop(candidate_id, None)
            if registration_id is not None:
                registration = self._github_token_providers.get(registration_id)
                if registration is not None:
                    registration.session_id = session_id
                    registration.committed = True

    def _reserve_inline_session_initialization(
        self, session_id: str, rpc_client: JsonRpcClient
    ) -> Callable[[], None]:
        # Reader callbacks cannot await an operation whose RPC needs this same reader.
        with self._sessions_lock:
            busy = any(
                lock.users and _session_ids_overlap(candidate, session_id)
                for candidate, lock in self._session_initializations.items()
            )
            busy = busy or any(
                _session_ids_overlap(candidate, session_id)
                and any(not resume.done() for resume in pending)
                for candidate, pending in self._ordinary_session_resumes.items()
            )
            busy = busy or any(
                _session_ids_overlap(candidate, session_id) and not cleanup.done()
                for candidate, cleanup in self._auto_mode_recovery_cleanups.get(
                    rpc_client, {}
                ).items()
            )
            if busy:
                raise RuntimeError(
                    f"Cannot initialize server-assigned session {session_id} "
                    "while another session operation is active"
                )
            lock = self._session_initializations.get(session_id)
            if lock is None:
                lock = _SessionInitializationLock()
                self._session_initializations[session_id] = lock
            reservation: ConcurrentFuture[None] = ConcurrentFuture()
            lock.inline_reservation = reservation
            lock.users += 1

        def release() -> None:
            with self._sessions_lock:
                lock.inline_reservation = None
                lock.users -= 1
            reservation.set_result(None)

        return release

    async def _acquire_session_initialization(
        self, session_id: str, rpc_client: JsonRpcClient, *, wait_for_resumes: bool = False
    ) -> _SessionInitialization:
        with self._sessions_lock:
            locks = {
                candidate: lock
                for candidate, lock in self._session_initializations.items()
                if _session_ids_overlap(candidate, session_id)
            }
            waiting_ids = {candidate for candidate, lock in locks.items() if lock.users}
            waiting_ids.update(
                candidate
                for candidate, pending in self._ordinary_session_resumes.items()
                if _session_ids_overlap(candidate, session_id)
                and (wait_for_resumes or candidate != session_id)
                and any(not resume.done() for resume in pending)
            )
            waiting_ids.update(
                candidate
                for candidate, cleanup in self._auto_mode_recovery_cleanups.get(
                    rpc_client, {}
                ).items()
                if _session_ids_overlap(candidate, session_id) and not cleanup.done()
            )
            if any(
                active
                and any(_session_ids_overlap(candidate, waiting_id) for waiting_id in waiting_ids)
                for candidate, active in self._active_session_callbacks.items()
            ):
                raise RuntimeError(
                    f"Cannot initialize session {session_id} during an active session callback"
                )
            if session_id not in locks:
                locks[session_id] = _SessionInitializationLock()
                self._session_initializations[session_id] = locks[session_id]
            for lock in locks.values():
                lock.users += 1
        acquired = _SessionInitialization([locks[candidate] for candidate in sorted(locks)])
        try:
            for lock in acquired._locks:
                await lock.acquire()
                acquired.acquired += 1
            # Ordinary resumes release initialization before RPC, but their
            # aliases still reserve the binding through canonical publication.
            with self._sessions_lock:
                resumes = tuple(
                    resume
                    for candidate, pending in self._ordinary_session_resumes.items()
                    if _session_ids_overlap(candidate, session_id)
                    and (wait_for_resumes or candidate != session_id)
                    for resume in pending
                )
            if resumes:
                await asyncio.shield(asyncio.gather(*resumes))
            with self._sessions_lock:
                cleanups = tuple(
                    cleanup
                    for candidate, cleanup in self._auto_mode_recovery_cleanups.get(
                        rpc_client, {}
                    ).items()
                    if _session_ids_overlap(candidate, session_id)
                )
            for cleanup in cleanups:
                # Keep provisional ownership and its eventual detach ahead of same-ID reuse.
                await asyncio.shield(cleanup)
        except BaseException:
            acquired.release()
            raise
        return acquired

    def _resident_session_id(self, session_id: str, rpc_client: JsonRpcClient) -> str:
        if not _is_uuid_session_id(session_id):
            return session_id
        with self._sessions_lock:
            if session_id in self._sessions:
                return session_id
            matches = {
                candidate
                for candidate in (
                    self._sessions.keys()
                    | self._ordinary_session_resumes.keys()
                    | self._auto_mode_recovery_cleanups.get(rpc_client, {}).keys()
                )
                if _session_ids_overlap(candidate, session_id) and len(candidate) >= len(session_id)
            }
            return next(iter(matches)) if len(matches) == 1 else session_id

    async def _require_auto_mode_recovery(
        self,
        session: CopilotSession,
        *,
        detach_on_failure: bool = True,
        delete_on_failure: bool = False,
    ) -> None:
        if (session.capabilities.get("ui") or {}).get("autoModeRecovery") is True:
            return
        error = RuntimeError("The runtime does not support auto_mode_recovery")
        if detach_on_failure:
            try:
                await self._cleanup_failed_session(session, delete_on_failure=delete_on_failure)
            except Exception as cleanup_error:
                raise error from cleanup_error
        raise error

    async def _cleanup_failed_session(
        self, session: CopilotSession, *, delete_on_failure: bool = False, wait: bool = True
    ) -> None:
        with self._sessions_lock:
            # ID-only detach must never remove a replacement's runtime bindings.
            if (
                self._session_admission_closed
                or self._client is not session._client
                or self._sessions.get(session.session_id) is not session
            ):
                return
            finished = asyncio.get_running_loop().create_future()
            cleanups = self._auto_mode_recovery_cleanups.setdefault(session._client, {})
            cleanups[session.session_id] = finished

        async def detach() -> None:
            cleanup_error: BaseException | None = None
            try:
                detach_error: Exception | None = None
                try:
                    await asyncio.wait_for(session._detach_runtime(), timeout=10)
                except Exception as error:
                    detach_error = error
                if delete_on_failure:
                    try:
                        response = await asyncio.wait_for(
                            session._client.request(
                                "session.delete", {"sessionId": session.session_id}
                            ),
                            timeout=10,
                        )
                        if not response.get("success", False):
                            raise RuntimeError(
                                f"Failed to delete cloud session {session.session_id}: "
                                f"{response.get('error', 'Unknown error')}"
                            )
                    except Exception as error:
                        if detach_error is not None:
                            raise ExceptionGroup(
                                "Failed to detach and delete rejected cloud session",
                                [detach_error, error],
                            )
                        raise
                if detach_error is not None:
                    raise detach_error
            except asyncio.CancelledError as error:
                cleanup_error = error
                logger.warning("Auto recovery enrollment cleanup was cancelled")
                raise
            except Exception as error:
                cleanup_error = error
                logger.warning("Auto recovery enrollment cleanup failed", exc_info=True)
                raise
            finally:
                session._mark_disconnected()
                with self._sessions_lock:
                    if self._sessions.get(session.session_id) is session:
                        del self._sessions[session.session_id]
                    if cleanup_error is None and cleanups.get(session.session_id) is finished:
                        del cleanups[session.session_id]
                if not finished.done():
                    if cleanup_error is None:
                        finished.set_result(None)
                    else:
                        finished.set_exception(cleanup_error)
                        finished.exception()

        cleanup = asyncio.create_task(detach())
        self._auto_mode_recovery_cleanup_tasks.add(cleanup)
        cleanup.add_done_callback(self._auto_mode_recovery_cleanup_tasks.discard)
        # Observe failures even when cancellation stopped the caller's wait.
        cleanup.add_done_callback(lambda task: None if task.cancelled() else task.exception())
        if wait:
            await asyncio.shield(cleanup)

    def _register_session(self, session: CopilotSession) -> CopilotSession | None:
        with self._sessions_lock:
            if (
                self._session_admission_closed
                or self._client is None
                or self._client is not session._client
            ):
                raise RuntimeError("Client not connected")
            if session.session_id in self._auto_mode_recovery_cleanups.get(session._client, {}):
                raise RuntimeError(
                    "Session is still detaching after rejected Auto recovery enrollment"
                )
            previous = self._sessions.get(session.session_id)
            session._set_release_from_client(self._forget_session)
            self._sessions[session.session_id] = session
            return previous

    def _rollback_resume_predecessor(self, session: CopilotSession) -> CopilotSession | None:
        # Called with _sessions_lock held. Later attempts must skip a failed provisional.
        predecessor = self._resume_predecessors.pop(session)
        for pending, previous in self._resume_predecessors.items():
            if previous is session:
                self._resume_predecessors[pending] = predecessor
        return predecessor

    def _forget_session(self, session: CopilotSession) -> None:
        """Stop tracking a disconnected session unless a later create or resume replaced it."""
        with self._sessions_lock:
            if self._sessions.get(session.session_id) is session:
                del self._sessions[session.session_id]

    def _get_session(self, session_id: str) -> CopilotSession | None:
        with self._sessions_lock:
            return self._sessions.get(session_id)

    def _handle_notification(self, method: str, params: dict) -> None:
        """Route a server notification to its session or the lifecycle listeners.

        The stdio and TCP connections both register this one handler, so they share
        the locked session lookup. The JSON-RPC reader schedules it on the event loop.
        """
        if method == "session.event":
            event = session_event_from_dict(params["event"])
            session = self._get_session(params["sessionId"])
            if session:
                session._dispatch_event(event)
        elif method == "session.lifecycle":
            self._dispatch_lifecycle_event(_session_lifecycle_event_from_dict(params))

    def _register_session_scoped_request_handlers(self) -> None:
        if not self._client:
            return
        # Cancellable so a runtime ``$/cancelRequest`` cancels an async provider's task.
        self._client.set_request_handler(
            "skillProvider.list", self._handle_skill_provider_list, cancellable=True
        )
        self._client.set_request_handler(
            "skillProvider.read", self._handle_skill_provider_read, cancellable=True
        )
        # Internal experimental callback backing ``MemoryConfiguration.store_memory``.
        self._client.set_request_handler(
            "memoryStore.store", self._handle_memory_store, cancellable=True
        )

    async def _set_llm_inference_provider(self) -> None:
        if self._request_handler is None or self._rpc is None:
            return
        await self._rpc.llm_inference.set_provider()

    def _register_client_session_api_handlers(self) -> None:
        rpc_client = self._client
        if rpc_client is None:
            return
        register_client_session_api_handlers(rpc_client, self._get_client_session_handlers)

    def _register_session_callback_scopes(self) -> None:
        rpc_client = self._client
        if rpc_client is None:
            return
        for method, handler in list(rpc_client.request_handlers.items()):

            async def scoped_handler(params: dict, handler=handler):
                session_id = params.get("sessionId")
                if isinstance(session_id, str):
                    with self._sessions_lock:
                        self._active_session_callbacks[session_id] = (
                            self._active_session_callbacks.get(session_id, 0) + 1
                        )
                try:
                    result = handler(params)
                    return await result if inspect.isawaitable(result) else result
                finally:
                    if isinstance(session_id, str):
                        with self._sessions_lock:
                            remaining = self._active_session_callbacks[session_id] - 1
                            if remaining:
                                self._active_session_callbacks[session_id] = remaining
                            else:
                                del self._active_session_callbacks[session_id]

            def dispatch_handler(params: dict, handler=handler, scoped_handler=scoped_handler):
                if isinstance(params.get("sessionId"), str):
                    return scoped_handler(params)
                # Ordered connection handlers must prepare synchronously.
                return handler(params)

            # Keep cancellation and ordered-dispatch metadata from registration.
            rpc_client.request_handlers[method] = dispatch_handler

    def _get_client_session_handlers(self, session_id: str) -> ClientSessionApiHandlers:
        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if session is None:
            raise ValueError(f"unknown session {session_id}")
        return session._client_session_apis

    async def _handle_skill_provider_list(self, params: dict) -> dict:
        session_id = _required_string_param(params, "sessionId")
        session = self._get_session(session_id)
        if session is None:
            raise JsonRpcError(-32603, f"Session not found: {session_id}")
        return await session._handle_skill_provider_list()

    async def _handle_skill_provider_read(self, params: dict) -> dict:
        session_id = _required_string_param(params, "sessionId")
        name = _required_string_param(params, "name")
        session = self._get_session(session_id)
        if session is None:
            raise JsonRpcError(-32603, f"Session not found: {session_id}")
        return await session._handle_skill_provider_read(name)

    async def _handle_memory_store(self, params: dict) -> None:
        memory = StoredMemory(
            session_id=_required_string_param(params, "sessionId"),
            subject=_required_string_param(params, "subject"),
            fact=_required_string_param(params, "fact"),
            citations=_required_string_param(params, "citations"),
            reason=_required_string_param(params, "reason"),
        )
        session = self._get_session(memory["session_id"])
        if session is None:
            raise JsonRpcError(-32603, "Memory store owner is unavailable")
        await session._handle_memory_store(memory)
        return None

    async def _handle_user_input_request(self, params: dict) -> dict:
        """
        Handle a user input request from the CLI server.

        Args:
            params: The user input request parameters from the server.

        Returns:
            A dict containing the user's response.

        Raises:
            ValueError: If the request payload is invalid.
        """
        session_id = params.get("sessionId")
        question = params.get("question")

        if not session_id or not question:
            raise ValueError("invalid user input request payload")

        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"unknown session {session_id}")

        result = await session._handle_user_input_request(params)
        return {"answer": result["answer"], "wasFreeform": result["wasFreeform"]}

    async def _handle_exit_plan_mode_request(self, params: dict) -> dict:
        """Handle an exitPlanMode.request callback from the CLI server."""
        session_id = params.get("sessionId")
        summary = params.get("summary")
        actions = params.get("actions")
        recommended_action = params.get("recommendedAction")

        if not session_id or not isinstance(summary, str):
            raise ValueError("invalid exit plan mode request payload")
        if not isinstance(actions, list) or not isinstance(recommended_action, str):
            raise ValueError("invalid exit plan mode request payload")

        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"unknown session {session_id}")

        return dict(await session._handle_exit_plan_mode_request(params))

    async def _handle_auto_mode_switch_request(self, params: dict) -> dict:
        """Handle an autoModeSwitch.request callback from the CLI server."""
        session_id = params.get("sessionId")
        if not session_id:
            raise ValueError("invalid auto mode switch request payload")

        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"unknown session {session_id}")

        response = await session._handle_auto_mode_switch_request(params)
        return {"response": response}

    async def _handle_system_message_transform(self, params: dict) -> dict:
        """Handle a systemMessage.transform request from the CLI server."""
        session_id = params.get("sessionId")
        sections = params.get("sections")

        if not session_id or not sections:
            raise ValueError("invalid systemMessage.transform payload")

        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"unknown session {session_id}")

        return await session._handle_system_message_transform(sections)
