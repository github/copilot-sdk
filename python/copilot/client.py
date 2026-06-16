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
import inspect
import logging
import os
import re
import shutil
import subprocess
import sys
import threading
import time
import uuid
from collections.abc import Awaitable, Callable, Mapping, Sequence
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path
from types import TracebackType
from typing import Any, ClassVar, Literal, TypedDict, cast, overload

from ._diagnostics import log_timing
from ._jsonrpc import JsonRpcClient, JsonRpcError, ProcessExitedError
from ._mode import (
    CopilotClientMode,
    ToolSet,
    _embedding_cache_storage_default,
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
    ExtensionInfo,
)
from .generated.rpc import (
    ClientSessionApiHandlers,
    ModelBillingTokenPrices,
    ModelBillingTokenPricesLongContext,  # noqa: F401
    OpenCanvasInstance,
    RemoteSessionMode,
    ServerRpc,
    _ConnectRequest,
    _InternalServerRpc,
    from_datetime,
    register_client_session_api_handlers,
)
from .generated.session_events import (
    SessionEvent,
    session_event_from_dict,
)
from .session import (
    AutoModeSwitchHandler,
    CommandDefinition,
    ContextTier,
    CopilotSession,
    CreateSessionFsHandler,
    CustomAgentConfig,
    DefaultAgentConfig,
    ElicitationHandler,
    ExitPlanModeHandler,
    InfiniteSessionConfig,
    LargeToolOutputConfig,
    MCPServerConfig,
    MemoryConfiguration,
    ModelCapabilitiesOverride,
    ProviderConfig,
    ReasoningEffort,
    ReasoningSummary,
    SectionTransformFn,
    SessionFsConfig,
    SessionHooks,
    SystemMessageConfig,
    UserInputHandler,
    _capabilities_to_dict,
    _PermissionHandlerFn,
)
from .session_fs_provider import SessionFsProvider, create_session_fs_adapter
from .tools import Tool

logger = logging.getLogger(__name__)

# ============================================================================
# Connection Types
# ============================================================================

_ConnectionState = Literal["disconnected", "connecting", "connected", "error"]

LogLevel = Literal["none", "error", "warning", "info", "debug", "all"]


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


def _memory_to_wire(config: Mapping[str, Any]) -> dict[str, Any]:
    """Convert a ``MemoryConfiguration`` mapping to wire format."""
    return {"enabled": config["enabled"]}


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
    """Whether to capture message content. Sets OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT."""  # noqa: E501


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


@dataclass
class ChildProcessRuntimeConnection(RuntimeConnection):
    """Base for :class:`RuntimeConnection` variants that spawn a runtime child process.

    Construct via :meth:`RuntimeConnection.stdio` or :meth:`RuntimeConnection.tcp`.
    """

    path: str | None = None
    """Path to the runtime executable. ``None`` uses the bundled binary."""

    args: Sequence[str] = ()
    """Extra command-line arguments passed to the runtime process."""


@dataclass
class StdioRuntimeConnection(ChildProcessRuntimeConnection):
    """Spawns a runtime child process and communicates over its stdin/stdout.

    Construct via :meth:`RuntimeConnection.stdio`.
    """


@dataclass
class TcpRuntimeConnection(ChildProcessRuntimeConnection):
    """Spawns a runtime child process listening on a TCP socket.

    Construct via :meth:`RuntimeConnection.tcp`.
    """

    port: int = 0
    """TCP port to listen on. ``0`` (the default) auto-allocates a free port."""

    connection_token: str | None = None
    """Shared secret the SDK sends to the spawned runtime. ``None`` auto-generates one."""


@dataclass
class UriRuntimeConnection(RuntimeConnection):
    """Connects to an already-running runtime at the specified URL.

    Construct via :meth:`RuntimeConnection.uri`.
    """

    url: str = ""
    """URL of the runtime to connect to. Accepts ``"port"``, ``"host:port"``, or a full URL."""

    connection_token: str | None = None
    """Shared secret to authenticate the connection."""


@dataclass
class _CopilotClientOptions:
    """Internal configuration carrier used by :class:`CopilotClient`.

    This is not part of the public API: ``CopilotClient`` accepts all of
    these options as keyword arguments directly.
    """

    connection: RuntimeConnection | None = None
    working_directory: str | None = None
    log_level: LogLevel = "info"
    env: dict[str, str] | None = None
    github_token: str | None = None
    base_directory: str | None = None
    use_logged_in_user: bool | None = None
    telemetry: TelemetryConfig | None = None
    session_fs: SessionFsConfig | None = None
    session_idle_timeout_seconds: int | None = None
    enable_remote_sessions: bool = False
    on_list_models: Callable[[], list[ModelInfo] | Awaitable[list[ModelInfo]]] | None = None
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
        return GetStatusResponse(str(version), int(protocol_version))

    def to_dict(self) -> dict:
        result: dict = {}
        result["version"] = self.version
        result["protocolVersion"] = self.protocol_version
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

    @staticmethod
    def from_dict(obj: Any) -> ModelLimits:
        assert isinstance(obj, dict)
        max_prompt_tokens = obj.get("max_prompt_tokens")
        max_context_window_tokens = obj.get("max_context_window_tokens")
        vision_dict = obj.get("vision")
        vision = ModelVisionLimits.from_dict(vision_dict) if vision_dict else None
        return ModelLimits(
            max_prompt_tokens=max_prompt_tokens,
            max_context_window_tokens=max_context_window_tokens,
            vision=vision,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        if self.max_prompt_tokens is not None:
            result["max_prompt_tokens"] = self.max_prompt_tokens
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
        return ModelInfo(
            id=str(id),
            name=str(name),
            capabilities=capabilities,
            policy=policy,
            billing=billing,
            supported_reasoning_efforts=supported_reasoning_efforts,
            default_reasoning_effort=default_reasoning_effort,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["id"] = self.id
        result["name"] = self.name
        result["capabilities"] = self.capabilities.to_dict()
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
_CLI_PROCESS_EXIT_TIMEOUT_SECONDS = 5


def _get_bundled_cli_path() -> str | None:
    """Get the path to the bundled CLI binary, if available."""
    # The binary is bundled in copilot/bin/ within the package
    bin_dir = Path(__file__).parent / "bin"
    if not bin_dir.exists():
        return None

    # Determine binary name based on platform
    if sys.platform == "win32":
        binary_name = "copilot.exe"
    else:
        binary_name = "copilot"

    binary_path = bin_dir / binary_name
    if binary_path.exists():
        return str(binary_path)

    return None


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
        ...     model="gpt-4",
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
        connection: RuntimeConnection | None = None,
        working_directory: str | None = None,
        log_level: LogLevel = "info",
        env: dict[str, str] | None = None,
        github_token: str | None = None,
        base_directory: str | None = None,
        use_logged_in_user: bool | None = None,
        telemetry: TelemetryConfig | None = None,
        session_fs: SessionFsConfig | None = None,
        session_idle_timeout_seconds: int | None = None,
        enable_remote_sessions: bool = False,
        on_list_models: Callable[[], list[ModelInfo] | Awaitable[list[ModelInfo]]] | None = None,
        mode: CopilotClientMode = "copilot-cli",
    ):
        """
        Initialize a new CopilotClient.

        All process-management options (``working_directory``, ``log_level``,
        ``env``, ``github_token``, …) apply only when the SDK spawns the runtime
        (stdio / tcp connections). They are ignored when connecting to an
        existing runtime via :meth:`RuntimeConnection.for_uri`.

        Args:
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
                ``~/.copilot``.
            use_logged_in_user: Use the logged-in user for authentication.
                ``None`` (default) resolves to ``True`` unless ``github_token``
                is set.
            telemetry: OpenTelemetry configuration. Providing this enables
                telemetry.
            session_fs: Connection-level session filesystem provider
                configuration.
            session_idle_timeout_seconds: Server-wide session idle timeout in
                seconds. Sessions without activity for this duration are
                automatically cleaned up. Set to ``None`` or ``0`` to disable.
            enable_remote_sessions: Enable remote session support (Mission
                Control integration). When ``True``, sessions in a GitHub
                repository working directory are accessible from GitHub web
                and mobile.
            on_list_models: Custom handler for :meth:`list_models`. When
                provided, the handler is called instead of querying the runtime
                server.

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
            connection=connection,
            working_directory=working_directory,
            log_level=log_level,
            env=env,
            github_token=github_token,
            base_directory=base_directory,
            use_logged_in_user=use_logged_in_user,
            telemetry=telemetry,
            session_fs=session_fs,
            session_idle_timeout_seconds=session_idle_timeout_seconds,
            enable_remote_sessions=enable_remote_sessions,
            on_list_models=on_list_models,
            mode=mode,
        )
        connection = (
            options.connection if options.connection is not None else RuntimeConnection.for_stdio()
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

        # Resolve connection-mode-specific state.
        self._actual_host: str = "localhost"
        self._is_external_server: bool = isinstance(connection, UriRuntimeConnection)

        if isinstance(connection, UriRuntimeConnection):
            if connection.connection_token is not None and len(connection.connection_token) == 0:
                raise ValueError("connection_token must be a non-empty string")
            self._actual_host, actual_port = self._parse_cli_url(connection.url)
            self._runtime_port: int | None = actual_port
            self._effective_connection_token: str | None = connection.connection_token
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

            # Resolve CLI path: explicit > COPILOT_CLI_PATH env var > bundled binary.
            effective_env = options.env if options.env is not None else os.environ
            self._cli_path_source: str | None = "explicit"
            if connection.path is None:
                env_cli_path = effective_env.get("COPILOT_CLI_PATH")
                if env_cli_path:
                    connection.path = env_cli_path
                    self._cli_path_source = "environment"
                else:
                    bundled_path = _get_bundled_cli_path()
                    if bundled_path:
                        connection.path = bundled_path
                        self._cli_path_source = "bundled"
                    else:
                        raise RuntimeError(
                            "Copilot CLI not found. The bundled CLI binary is not available. "
                            "Ensure you installed a platform-specific wheel, or set "
                            "RuntimeConnection.for_stdio(path=...) / "
                            "RuntimeConnection.for_tcp(path=...)."
                        )

            # Resolve use_logged_in_user default
            if options.use_logged_in_user is None:
                options.use_logged_in_user = not bool(options.github_token)

        self._process: Any = None
        self._cli_process: subprocess.Popen | None = None
        self._client: JsonRpcClient | None = None
        self._state: _ConnectionState = "disconnected"
        self._sessions: dict[str, CopilotSession] = {}
        self._sessions_lock = threading.Lock()
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

        Supports formats: "host:port", "http://host:port", "https://host:port",
        or just "port".

        Args:
            url: The CLI URL to parse.

        Returns:
            A tuple of (host, port).

        Raises:
            ValueError: If the URL format is invalid or the port is out of range.
        """
        import re

        # Remove protocol if present
        clean_url = re.sub(r"^https?://", "", url)

        # Check if it's just a port number
        if clean_url.isdigit():
            port = int(clean_url)
            if port <= 0 or port > 65535:
                raise ValueError(f"Invalid port in cli_url: {url}")
            return ("localhost", port)

        # Parse host:port format
        parts = clean_url.split(":")
        if len(parts) != 2:
            raise ValueError(f"Invalid cli_url format: {url}")

        host = parts[0] if parts[0] else "localhost"
        try:
            port = int(parts[1])
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

            # Verify protocol version compatibility
            await self._verify_protocol_version()
            log_timing(
                logger,
                logging.DEBUG,
                "CopilotClient.start protocol verification complete",
                start_time,
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
                return_code = process.poll()
                if return_code is not None and self._client:
                    stderr_output = self._client.get_stderr_output()
                    if stderr_output:
                        raise RuntimeError(
                            f"CLI process exited with code {return_code}\nstderr: {stderr_output}"
                        ) from e
            raise

    async def stop(self) -> None:
        """
        Stop the CLI server and close all active sessions.

        This method performs graceful cleanup:
        1. Closes all active sessions (releases in-memory resources)
        2. Requests runtime shutdown for SDK-owned CLI processes
        3. Closes the JSON-RPC connection
        4. Terminates the CLI server process (if spawned by this client)

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

        # Atomically take ownership of all sessions and clear the dict
        # so no other thread can access them
        with self._sessions_lock:
            sessions_to_destroy = list(self._sessions.values())
            self._sessions.clear()

        for session in sessions_to_destroy:
            try:
                await session.disconnect()
            except Exception as e:
                logger.debug(
                    "Error while cleaning up Copilot session %s",
                    session.session_id,
                    exc_info=True,
                )
                errors.append(
                    StopError(message=f"Failed to disconnect session {session.session_id}: {e}")
                )

        runtime_shutdown_completed = False
        if self._rpc is not None and self._cli_process is not None and not self._is_external_server:
            runtime_shutdown_start = time.perf_counter()
            try:
                await self._rpc.runtime.shutdown(timeout=_RUNTIME_SHUTDOWN_TIMEOUT_SECONDS)
                runtime_shutdown_completed = True
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

        # Close client
        if self._client:
            await self._client.stop()
            self._client = None
        self._rpc = None

        # Clear models cache
        async with self._models_cache_lock:
            self._models_cache = None

        # Close TCP socket wrappers without treating them as owned processes.
        if self._process is not None and self._process is not self._cli_process:
            try:
                self._process.terminate()
            except Exception:
                logger.debug("Error while closing Copilot runtime transport", exc_info=True)
            self._process = None

        # Terminate CLI process (only if we spawned it)
        if self._cli_process and not self._is_external_server:
            poll = getattr(self._cli_process, "poll", None)
            is_running = poll is None or poll() is None
            if is_running:
                if runtime_shutdown_completed:
                    try:
                        await asyncio.to_thread(
                            self._cli_process.wait,
                            timeout=_RUNTIME_SHUTDOWN_TIMEOUT_SECONDS,
                        )
                    except subprocess.TimeoutExpired:
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
                                            "Timed out waiting for CLI process to exit after kill: "
                                            f"{e}"
                                        )
                                    )
                                )
                else:
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

        Example:
            >>> # If normal stop hangs, force stop
            >>> try:
            ...     await asyncio.wait_for(client.stop(), timeout=5.0)
            ... except asyncio.TimeoutError:
            ...     await client.force_stop()
        """
        # Clear sessions immediately without trying to destroy them
        with self._sessions_lock:
            self._sessions.clear()

        # Close the transport first to signal the server immediately.
        # For external servers (TCP), this closes the socket.
        # For spawned processes (stdio), this kills the process.
        if self._process is not None or self._cli_process is not None:
            try:
                if self._is_external_server:
                    if self._process is not None:
                        self._process.terminate()  # closes the TCP socket
                    self._process = None
                    self._cli_process = None
                else:
                    if self._process is not None and self._process is not self._cli_process:
                        self._process.terminate()
                    if self._cli_process is not None:
                        self._cli_process.kill()
                    self._process = None
                    self._cli_process = None
            except Exception:
                logger.debug("Error while force-stopping Copilot CLI process", exc_info=True)

        # Then clean up the JSON-RPC client
        if self._client:
            try:
                await self._client.stop()
            except Exception:
                logger.debug(
                    "Error while stopping JSON-RPC client during force stop", exc_info=True
                )
            self._client = None
        self._rpc = None

        # Clear models cache
        async with self._models_cache_lock:
            self._models_cache = None

        self._state = "disconnected"
        if not self._is_external_server:
            self._runtime_port = None

    async def create_session(
        self,
        *,
        on_permission_request: _PermissionHandlerFn | None = None,
        model: str | None = None,
        session_id: str | None = None,
        client_name: str | None = None,
        reasoning_effort: ReasoningEffort | None = None,
        reasoning_summary: ReasoningSummary | None = None,
        context_tier: ContextTier | None = None,
        tools: list[Tool] | None = None,
        system_message: SystemMessageConfig | None = None,
        available_tools: list[str] | ToolSet | None = None,
        excluded_tools: list[str] | ToolSet | None = None,
        on_user_input_request: UserInputHandler | None = None,
        hooks: SessionHooks | None = None,
        working_directory: str | None = None,
        provider: ProviderConfig | None = None,
        enable_session_telemetry: bool | None = None,
        skip_custom_instructions: bool | None = None,
        custom_agents_local_only: bool | None = None,
        coauthor_enabled: bool | None = None,
        manage_schedule_enabled: bool | None = None,
        model_capabilities: ModelCapabilitiesOverride | None = None,
        streaming: bool | None = None,
        include_sub_agent_streaming_events: bool | None = None,
        mcp_servers: dict[str, MCPServerConfig] | None = None,
        mcp_oauth_token_storage: Literal["persistent", "in-memory"] | None = None,
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
        skill_directories: list[str] | None = None,
        plugin_directories: list[str] | None = None,
        instruction_directories: list[str] | None = None,
        disabled_skills: list[str] | None = None,
        infinite_sessions: InfiniteSessionConfig | None = None,
        large_output: LargeToolOutputConfig | None = None,
        memory: MemoryConfiguration | None = None,
        on_event: Callable[[SessionEvent], None] | None = None,
        commands: list[CommandDefinition] | None = None,
        on_elicitation_request: ElicitationHandler | None = None,
        enable_mcp_apps: bool = False,
        on_exit_plan_mode_request: ExitPlanModeHandler | None = None,
        on_auto_mode_switch_request: AutoModeSwitchHandler | None = None,
        create_session_fs_handler: CreateSessionFsHandler | None = None,
        github_token: str | None = None,
        remote_session: RemoteSessionMode | None = None,
        cloud: CloudSessionOptions | None = None,
        canvases: list[CanvasDeclaration] | None = None,
        request_canvas_renderer: bool | None = None,
        request_extensions: bool | None = None,
        extension_sdk_path: str | None = None,
        extension_info: ExtensionInfo | None = None,
        canvas_handler: CanvasHandler | None = None,
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
            model: The model to use for the session (e.g. ``"gpt-4"``).
            session_id: Optional session ID. If not provided, a UUID is generated.
            client_name: Optional client name for identification.
            reasoning_effort: Reasoning effort level for the model.
            reasoning_summary: Reasoning summary mode for supported models.
                Use ``"none"`` to suppress summary output regardless of whether
                reasoning is enabled.
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
            hooks: Lifecycle hooks for the session.
            working_directory: Working directory for the session.
            provider: Provider configuration for Azure or custom endpoints.
            enable_session_telemetry: Enables or disables internal session telemetry
                for this session. When False, disables session telemetry. When omitted
                or True, telemetry is enabled for GitHub-authenticated sessions. When
                a custom provider (BYOK) is configured, session telemetry is always
                disabled regardless of this setting. This is independent of the client
                OpenTelemetry configuration.
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
            embedding_cache_storage: Controls how embedding caches are stored.
                `"persistent"` uses disk-based storage (shared across sessions).
                `"in-memory"` stores embeddings in memory (discarded on session end).
                Defaults to `"in-memory"` in empty mode.
            custom_agents: Custom agent configurations.
            default_agent: Configuration for the default agent,
                including tool visibility controls.
            agent: Agent to use for the session.
            config_directory: Override for the configuration directory.
            enable_config_discovery: When True, automatically discovers MCP server
                configurations (e.g. ``.mcp.json``, ``.vscode/mcp.json``) and skill
                directories from the working directory and merges them with any
                explicitly provided ``mcp_servers`` and ``skill_directories``, with
                explicit values taking precedence on name collision. Custom instruction
                files (``.github/copilot-instructions.md``, ``AGENTS.md``, etc.) are
                always loaded regardless of this setting.
            skip_embedding_retrieval: When True, skips embedding-based retrieval.
            organization_custom_instructions: Organization-level custom instructions.
            enable_on_demand_instruction_discovery: Enables on-demand instruction file
                discovery.
            enable_file_hooks: Enables file-based hooks from ``.github/hooks/``.
            enable_host_git_operations: Enables git operations on the host filesystem.
            enable_session_store: Enables the cross-session store.
            enable_skills: Enables skill loading.
            skill_directories: Directories to search for skills.
            instruction_directories: Additional directories to search for custom
                instruction files.
            disabled_skills: Skills to disable.
            infinite_sessions: Infinite session configuration.
            memory: Session memory configuration.
            cloud: Creates a remote session in the cloud instead of a local
                session. Optionally associates repository metadata with the
                cloud session.
            on_event: Callback for session events.
            enable_mcp_apps: **Experimental.** Opt into MCP Apps (SEP-1865) UI
                passthrough. This parameter is part of an experimental
                wire-protocol surface and may change or be removed in a future
                release. When True, the SDK sends ``requestMcpApps: True`` on
                ``session.create``. The runtime only honors the opt-in when its
                ``MCP_APPS`` feature flag (or ``COPILOT_MCP_APPS=true`` env
                override) is on; otherwise the request is silently dropped.
                Inspect ``capabilities.ui.mcpApps`` on the create response to
                detect the drop.

        Returns:
            A :class:`CopilotSession` instance for the new session.

        Raises:
            ValueError: If ``on_permission_request`` is provided but not callable.

        Example:
            >>> session = await client.create_session(
            ...     on_permission_request=PermissionHandler.approve_all,
            ... )
            >>>
            >>> # Session with model and streaming
            >>> session = await client.create_session(
            ...     on_permission_request=PermissionHandler.approve_all,
            ...     model="gpt-4",
            ...     streaming=True,
            ... )
        """
        if on_permission_request is not None and not callable(on_permission_request):
            raise ValueError("on_permission_request must be callable when provided.")
        if not self._client:
            await self.start()

        tool_defs = []
        if tools:
            for tool in tools:
                definition: dict[str, Any] = {
                    "name": tool.name,
                    "description": tool.description,
                }
                if tool.parameters:
                    definition["parameters"] = tool.parameters
                if tool.overrides_built_in_tool:
                    definition["overridesBuiltInTool"] = True
                if tool.skip_permission:
                    definition["skipPermission"] = True
                if tool.defer is not None:
                    definition["defer"] = tool.defer
                tool_defs.append(definition)

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

        payload: dict[str, Any] = {}
        if model:
            payload["model"] = model
        if client_name:
            payload["clientName"] = client_name
        if reasoning_effort:
            payload["reasoningEffort"] = reasoning_effort
        if reasoning_summary:
            payload["reasoningSummary"] = reasoning_summary
        if context_tier:
            payload["contextTier"] = context_tier
        if tool_defs:
            payload["tools"] = tool_defs

        wire_system_message, transform_callbacks = _extract_transform_callbacks(system_message)
        if wire_system_message:
            payload["systemMessage"] = wire_system_message

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

        # Enable elicitation request callback if handler provided
        payload["requestElicitation"] = bool(on_elicitation_request)
        if enable_mcp_apps:
            payload["requestMcpApps"] = True
        payload["requestExitPlanMode"] = bool(on_exit_plan_mode_request)
        payload["requestAutoModeSwitch"] = bool(on_auto_mode_switch_request)

        # Serialize commands (name + description only) into payload
        if commands:
            payload["commands"] = [
                {"name": cmd.name, "description": cmd.description} for cmd in commands
            ]

        # Enable hooks callback if any hook handler provided
        if hooks and any(hooks.values()):
            payload["hooks"] = True

        # Add GitHub token for per-session authentication
        if github_token is not None:
            payload["gitHubToken"] = github_token

        # Add remote session mode if provided
        if remote_session is not None:
            payload["remoteSession"] = remote_session.value

        # Add cloud session options if provided
        if cloud is not None:
            payload["cloud"] = _cloud_session_options_to_dict(cloud)

        # Add working directory if provided
        if working_directory:
            payload["workingDirectory"] = working_directory

        # Add streaming option if provided
        if streaming is not None:
            payload["streaming"] = streaming

        # Include sub-agent streaming events (defaults to True)
        payload["includeSubAgentStreamingEvents"] = (
            include_sub_agent_streaming_events
            if include_sub_agent_streaming_events is not None
            else True
        )

        # Add provider configuration if provided
        if provider:
            payload["provider"] = self._convert_provider_to_wire_format(provider)

        if enable_session_telemetry is not None:
            payload["enableSessionTelemetry"] = enable_session_telemetry

        # Add model capabilities override if provided
        if model_capabilities:
            payload["modelCapabilities"] = _capabilities_to_dict(model_capabilities)

        # Add MCP servers configuration if provided
        if mcp_servers:
            payload["mcpServers"] = _mcp_servers_to_wire(mcp_servers)
        # Mode "empty" defaults MCP OAuth token storage to in-memory; caller wins.
        mcp_oauth_token_storage = _mcp_oauth_token_storage_default(mode, mcp_oauth_token_storage)
        if mcp_oauth_token_storage is not None:
            payload["mcpOAuthTokenStorage"] = mcp_oauth_token_storage
        embedding_cache_storage = _embedding_cache_storage_default(mode, embedding_cache_storage)
        if embedding_cache_storage is not None:
            payload["embeddingCacheStorage"] = embedding_cache_storage
        payload["envValueMode"] = "direct"

        # Add custom agents configuration if provided
        if custom_agents:
            payload["customAgents"] = [
                self._convert_custom_agent_to_wire_format(agent) for agent in custom_agents
            ]

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

        # Add skill directories configuration if provided
        if skill_directories:
            payload["skillDirectories"] = skill_directories

        # Add plugin directories configuration if provided
        if plugin_directories:
            payload["pluginDirectories"] = plugin_directories

        # Add instruction directories configuration if provided
        if instruction_directories is not None:
            payload["instructionDirectories"] = instruction_directories

        # Add disabled skills configuration if provided
        if disabled_skills:
            payload["disabledSkills"] = disabled_skills

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

        if not self._client:
            raise RuntimeError("Client not connected")

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

        # Propagate W3C Trace Context to CLI if OpenTelemetry is active
        trace_ctx = get_trace_context()
        payload.update(trace_ctx)

        def _initialize_session(sid: str) -> CopilotSession:
            """Create the session, wire up handlers, and register it.

            Invoked from the reader thread the instant the session.create
            response arrives (synchronously, before the next message is
            dispatched) so notifications for the new session id are routed
            to a registered session.
            """
            setup_start = time.perf_counter()
            s = CopilotSession(sid, self._client, workspace_path=None)
            if self._session_fs_config:
                if create_session_fs_handler is None:
                    raise ValueError(
                        "create_session_fs_handler is required in session config when "
                        "session_fs is enabled in client options."
                    )
                fs_provider: SessionFsProvider = create_session_fs_handler(s)
                caps = self._session_fs_config.get("capabilities")
                if caps and caps.get("sqlite"):
                    from .session_fs_provider import SessionFsSqliteProvider

                    if not isinstance(fs_provider, SessionFsSqliteProvider):
                        raise ValueError(
                            "SessionFs capabilities declare SQLite support but the provider "
                            "does not implement SessionFsSqliteProvider"
                        )
                s._client_session_apis.session_fs = create_session_fs_adapter(fs_provider)
            s._register_tools(tools)
            s._register_commands(commands)
            s._register_permission_handler(on_permission_request)
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
            if hooks:
                s._register_hooks(hooks)
            if transform_callbacks:
                s._register_transform_callbacks(transform_callbacks)
            if on_event:
                s.on(on_event)
            with self._sessions_lock:
                self._sessions[sid] = s
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

        session: CopilotSession | None = None
        registered_session_id: str | None = None

        # Pre-register non-cloud sessions BEFORE issuing the RPC so any
        # session-scoped requests the CLI emits during session.create
        # processing (e.g. sessionFs.writeFile for workspace metadata) can be
        # routed to the correct handlers.
        if local_session_id is not None:
            session = _initialize_session(local_session_id)
            registered_session_id = local_session_id

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
                nonlocal session, registered_session_id
                if session is not None:
                    return
                if not isinstance(raw_response, dict):
                    return
                sid = raw_response.get("sessionId")
                if isinstance(sid, str) and sid:
                    session = _initialize_session(sid)
                    registered_session_id = sid

            response = await self._client.request(
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
            session._workspace_path = response.get("workspacePath")
            capabilities = response.get("capabilities")
            session._set_capabilities(capabilities)
        except BaseException as exc:
            if registered_session_id is not None:
                with self._sessions_lock:
                    self._sessions.pop(registered_session_id, None)
            if not isinstance(exc, asyncio.CancelledError):
                log_timing(
                    logger,
                    logging.WARNING,
                    "CopilotClient.create_session failed",
                    total_start,
                    exc_info=True,
                    session_id=registered_session_id,
                )
            raise

        await self._apply_post_create_options_patch(
            session,
            mode,
            skip_custom_instructions,
            custom_agents_local_only,
            coauthor_enabled,
            manage_schedule_enabled,
        )

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
        reasoning_summary: ReasoningSummary | None = None,
        context_tier: ContextTier | None = None,
        tools: list[Tool] | None = None,
        system_message: SystemMessageConfig | None = None,
        available_tools: list[str] | ToolSet | None = None,
        excluded_tools: list[str] | ToolSet | None = None,
        on_user_input_request: UserInputHandler | None = None,
        hooks: SessionHooks | None = None,
        working_directory: str | None = None,
        provider: ProviderConfig | None = None,
        enable_session_telemetry: bool | None = None,
        skip_custom_instructions: bool | None = None,
        custom_agents_local_only: bool | None = None,
        coauthor_enabled: bool | None = None,
        manage_schedule_enabled: bool | None = None,
        model_capabilities: ModelCapabilitiesOverride | None = None,
        streaming: bool | None = None,
        include_sub_agent_streaming_events: bool | None = None,
        mcp_servers: dict[str, MCPServerConfig] | None = None,
        mcp_oauth_token_storage: Literal["persistent", "in-memory"] | None = None,
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
        skill_directories: list[str] | None = None,
        plugin_directories: list[str] | None = None,
        instruction_directories: list[str] | None = None,
        disabled_skills: list[str] | None = None,
        infinite_sessions: InfiniteSessionConfig | None = None,
        large_output: LargeToolOutputConfig | None = None,
        memory: MemoryConfiguration | None = None,
        on_event: Callable[[SessionEvent], None] | None = None,
        commands: list[CommandDefinition] | None = None,
        on_elicitation_request: ElicitationHandler | None = None,
        enable_mcp_apps: bool = False,
        on_exit_plan_mode_request: ExitPlanModeHandler | None = None,
        on_auto_mode_switch_request: AutoModeSwitchHandler | None = None,
        create_session_fs_handler: CreateSessionFsHandler | None = None,
        github_token: str | None = None,
        remote_session: RemoteSessionMode | None = None,
        continue_pending_work: bool | None = None,
        canvases: list[CanvasDeclaration] | None = None,
        request_canvas_renderer: bool | None = None,
        request_extensions: bool | None = None,
        extension_sdk_path: str | None = None,
        extension_info: ExtensionInfo | None = None,
        canvas_handler: CanvasHandler | None = None,
        open_canvases: list[OpenCanvasInstance] | None = None,
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
            reasoning_summary: Reasoning summary mode for supported models.
                Use ``"none"`` to suppress summary output regardless of whether
                reasoning is enabled.
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
            hooks: Lifecycle hooks for the session.
            working_directory: Working directory for the session.
            provider: Provider configuration for Azure or custom endpoints.
            enable_session_telemetry: Enables or disables internal session telemetry
                for this session. When False, disables session telemetry. When omitted
                or True, telemetry is enabled for GitHub-authenticated sessions. When
                a custom provider (BYOK) is configured, session telemetry is always
                disabled regardless of this setting. This is independent of the client
                OpenTelemetry configuration.
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
            embedding_cache_storage: Controls how embedding caches are stored.
                `"persistent"` uses disk-based storage (shared across sessions).
                `"in-memory"` stores embeddings in memory (discarded on session end).
                Defaults to `"in-memory"` in empty mode.
            custom_agents: Custom agent configurations.
            default_agent: Configuration for the default agent,
                including tool visibility controls.
            agent: Agent to use for the session.
            config_directory: Override for the configuration directory.
            enable_config_discovery: When True, automatically discovers MCP server
                configurations (e.g. ``.mcp.json``, ``.vscode/mcp.json``) and skill
                directories from the working directory and merges them with any
                explicitly provided ``mcp_servers`` and ``skill_directories``, with
                explicit values taking precedence on name collision. Custom instruction
                files (``.github/copilot-instructions.md``, ``AGENTS.md``, etc.) are
                always loaded regardless of this setting.
            skip_embedding_retrieval: When True, skips embedding-based retrieval.
            organization_custom_instructions: Organization-level custom instructions.
            enable_on_demand_instruction_discovery: Enables on-demand instruction file
                discovery.
            enable_file_hooks: Enables file-based hooks from ``.github/hooks/``.
            enable_host_git_operations: Enables git operations on the host filesystem.
            enable_session_store: Enables the cross-session store.
            enable_skills: Enables skill loading.
            skill_directories: Directories to search for skills.
            instruction_directories: Additional directories to search for custom
                instruction files.
            disabled_skills: Skills to disable.
            infinite_sessions: Infinite session configuration.
            memory: Session memory configuration.
            on_event: Callback for session events.
            enable_mcp_apps: **Experimental.** Opt into MCP Apps (SEP-1865) UI
                passthrough on resume. This parameter is part of an experimental
                wire-protocol surface and may change or be removed in a future
                release. When True, the SDK sends ``requestMcpApps: True`` on
                ``session.resume``. The runtime only honors the opt-in when its
                ``MCP_APPS`` feature flag (or ``COPILOT_MCP_APPS=true`` env
                override) is on; otherwise the request is silently dropped.
                Inspect ``capabilities.ui.mcpApps`` on the resume response to
                detect the drop.
            continue_pending_work: When True, instructs the runtime to continue any
                tool calls or permission prompts that were still pending when the
                session was last suspended. When False (the default), the runtime
                treats pending work as interrupted on resume.

        Returns:
            A :class:`CopilotSession` instance for the resumed session.

        Raises:
            RuntimeError: If the session does not exist or the client is not connected.
            ValueError: If ``on_permission_request`` is not a valid callable.

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
        if on_permission_request is not None and not callable(on_permission_request):
            raise ValueError("on_permission_request must be callable when provided.")
        if not self._client:
            await self.start()

        tool_defs = []
        if tools:
            for tool in tools:
                definition: dict[str, Any] = {
                    "name": tool.name,
                    "description": tool.description,
                }
                if tool.parameters:
                    definition["parameters"] = tool.parameters
                if tool.overrides_built_in_tool:
                    definition["overridesBuiltInTool"] = True
                if tool.skip_permission:
                    definition["skipPermission"] = True
                if tool.defer is not None:
                    definition["defer"] = tool.defer
                tool_defs.append(definition)

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

        payload: dict[str, Any] = {"sessionId": session_id}

        if client_name:
            payload["clientName"] = client_name
        if model:
            payload["model"] = model
        if reasoning_effort:
            payload["reasoningEffort"] = reasoning_effort
        if reasoning_summary:
            payload["reasoningSummary"] = reasoning_summary
        if context_tier:
            payload["contextTier"] = context_tier
        if tool_defs:
            payload["tools"] = tool_defs
        wire_system_message, transform_callbacks = _extract_transform_callbacks(system_message)
        if wire_system_message:
            payload["systemMessage"] = wire_system_message
        if available_tools is not None:
            payload["availableTools"] = available_tools
        if excluded_tools is not None:
            payload["excludedTools"] = excluded_tools
        payload["toolFilterPrecedence"] = "excluded"
        if provider:
            payload["provider"] = self._convert_provider_to_wire_format(provider)
        if enable_session_telemetry is not None:
            payload["enableSessionTelemetry"] = enable_session_telemetry
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

        # Enable permission request callback if handler provided
        payload["requestPermission"] = bool(on_permission_request)

        if on_user_input_request:
            payload["requestUserInput"] = True

        # Enable elicitation request callback if handler provided
        payload["requestElicitation"] = bool(on_elicitation_request)
        if enable_mcp_apps:
            payload["requestMcpApps"] = True
        payload["requestExitPlanMode"] = bool(on_exit_plan_mode_request)
        payload["requestAutoModeSwitch"] = bool(on_auto_mode_switch_request)

        # Serialize commands (name + description only) into payload
        if commands:
            payload["commands"] = [
                {"name": cmd.name, "description": cmd.description} for cmd in commands
            ]

        if hooks and any(hooks.values()):
            payload["hooks"] = True

        # Add GitHub token for per-session authentication
        if github_token is not None:
            payload["gitHubToken"] = github_token

        # Add remote session mode if provided
        if remote_session is not None:
            payload["remoteSession"] = remote_session.value

        if working_directory:
            payload["workingDirectory"] = working_directory
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

        if continue_pending_work is not None:
            payload["continuePendingWork"] = continue_pending_work

        # TODO: disable_resume is not a keyword arg yet; keeping for future use
        if mcp_servers:
            payload["mcpServers"] = _mcp_servers_to_wire(mcp_servers)
        # Mode "empty" defaults MCP OAuth token storage to in-memory; caller wins.
        mcp_oauth_token_storage = _mcp_oauth_token_storage_default(mode, mcp_oauth_token_storage)
        if mcp_oauth_token_storage is not None:
            payload["mcpOAuthTokenStorage"] = mcp_oauth_token_storage
        embedding_cache_storage = _embedding_cache_storage_default(mode, embedding_cache_storage)
        if embedding_cache_storage is not None:
            payload["embeddingCacheStorage"] = embedding_cache_storage
        payload["envValueMode"] = "direct"

        if custom_agents:
            payload["customAgents"] = [
                self._convert_custom_agent_to_wire_format(a) for a in custom_agents
            ]

        # Add default agent configuration if provided
        if default_agent:
            payload["defaultAgent"] = self._convert_default_agent_to_wire_format(default_agent)

        if agent:
            payload["agent"] = agent
        if skill_directories:
            payload["skillDirectories"] = skill_directories
        if plugin_directories:
            payload["pluginDirectories"] = plugin_directories
        if instruction_directories is not None:
            payload["instructionDirectories"] = instruction_directories
        if disabled_skills:
            payload["disabledSkills"] = disabled_skills

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

        if not self._client:
            raise RuntimeError("Client not connected")

        total_start = time.perf_counter()
        # Propagate W3C Trace Context to CLI if OpenTelemetry is active
        trace_ctx = get_trace_context()
        payload.update(trace_ctx)

        # Create and register the session before issuing the RPC so that
        # events emitted by the CLI (e.g. session.start) are not dropped.
        setup_start = time.perf_counter()
        session = CopilotSession(session_id, self._client, workspace_path=None)
        if self._session_fs_config:
            if create_session_fs_handler is None:
                raise ValueError(
                    "create_session_fs_handler is required in session config when "
                    "session_fs is enabled in client options."
                )
            fs_provider: SessionFsProvider = create_session_fs_handler(session)
            caps = self._session_fs_config.get("capabilities")
            if caps and caps.get("sqlite"):
                from .session_fs_provider import SessionFsSqliteProvider

                if not isinstance(fs_provider, SessionFsSqliteProvider):
                    raise ValueError(
                        "SessionFs capabilities declare SQLite support but the provider "
                        "does not implement SessionFsSqliteProvider"
                    )
            session._client_session_apis.session_fs = create_session_fs_adapter(fs_provider)
        session._register_tools(tools)
        session._register_commands(commands)
        session._register_permission_handler(on_permission_request)
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
        if hooks:
            session._register_hooks(hooks)
        if transform_callbacks:
            session._register_transform_callbacks(transform_callbacks)
        if on_event:
            session.on(on_event)
        with self._sessions_lock:
            self._sessions[session_id] = session
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

        try:
            rpc_start = time.perf_counter()
            response = await self._client.request("session.resume", payload)
            log_timing(
                logger,
                logging.DEBUG,
                "CopilotClient.resume_session session resume request completed successfully",
                rpc_start,
                session_id=session_id,
            )
            session._workspace_path = response.get("workspacePath")
            capabilities = response.get("capabilities")
            session._set_capabilities(capabilities)
            open_canvases_raw = response.get("openCanvases")
            if isinstance(open_canvases_raw, list):
                session._set_open_canvases(
                    [OpenCanvasInstance.from_dict(inst) for inst in open_canvases_raw]
                )
        except BaseException as exc:
            with self._sessions_lock:
                self._sessions.pop(session_id, None)
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

        await self._apply_post_create_options_patch(
            session,
            mode,
            skip_custom_instructions,
            custom_agents_local_only,
            coauthor_enabled,
            manage_schedule_enabled,
        )

        log_timing(
            logger,
            logging.DEBUG,
            "CopilotClient.resume_session complete",
            total_start,
            session_id=session_id,
        )
        return session

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
            ...     print(f"Session: {session.sessionId}")
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

        response = await self._client.request("session.delete", {"sessionId": session_id})

        success = response.get("success", False)
        if not success:
            error = response.get("error", "Unknown error")
            raise RuntimeError(f"Failed to delete session {session_id}: {error}")

        # Remove from local sessions map if present
        with self._sessions_lock:
            if session_id in self._sessions:
                del self._sessions[session_id]

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
            connect_result = await _InternalServerRpc(self._client)._connect(
                _ConnectRequest(token=self._effective_connection_token)
            )
            server_version = connect_result.protocol_version
        except JsonRpcError as err:
            if err.code == -32601 or err.message == "Unhandled method connect":
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
        if "wire_api" in provider:
            wire_provider["wireApi"] = provider["wire_api"]
        if "bearer_token" in provider:
            wire_provider["bearerToken"] = provider["bearer_token"]
        if "headers" in provider:
            wire_provider["headers"] = provider["headers"]
        if "model_id" in provider:
            wire_provider["modelId"] = provider["model_id"]
        if "wire_model" in provider:
            wire_provider["wireModel"] = provider["wire_model"]
        if "max_prompt_tokens" in provider:
            wire_provider["maxPromptTokens"] = provider["max_prompt_tokens"]
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
        mode (stdio or TCP).

        Raises:
            RuntimeError: If the server fails to start or times out.
        """
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

        if opts.enable_remote_sessions:
            args.append("--remote")

        # If cli_path is a .js file, run it with node
        # Note that we can't rely on the shebang as Windows doesn't support it
        if cli_path.endswith(".js"):
            args = ["node", cli_path] + args
        else:
            args = [cli_path] + args
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

        # Get environment variables
        if opts.env is None:
            env = dict(os.environ)
        else:
            env = dict(opts.env)

        # Set auth token in environment if provided
        if opts.github_token:
            env["COPILOT_SDK_AUTH_TOKEN"] = opts.github_token

        # Mode "empty": disable the runtime's system keychain probe so per-tenant
        # credentials don't leak through a shared keytar store.
        if opts.mode == "empty":
            env["COPILOT_DISABLE_KEYTAR"] = "1"

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

        async def read_port():
            if not process or not process.stdout:
                raise RuntimeError("Process not started or stdout not available")
            while True:
                line = await loop.run_in_executor(None, process.stdout.readline)
                if not line:
                    raise RuntimeError("CLI process exited before announcing port")

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
            raise RuntimeError("Timeout waiting for CLI server to start")

    async def _connect_to_server(self) -> None:
        """Connect to the runtime via the configured transport.

        Uses either stdio or TCP based on the client configuration.

        Raises:
            RuntimeError: If the connection fails.
        """
        setup_start = time.perf_counter()
        if isinstance(self._connection, StdioRuntimeConnection):
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
        self._client.on_close = lambda: setattr(self, "_state", "disconnected")
        self._rpc = ServerRpc(self._client)

        # Set up notification handler for session events
        # Note: This handler is called from the event loop (thread-safe scheduling)
        def handle_notification(method: str, params: dict):
            if method == "session.event":
                session_id = params["sessionId"]
                event_dict = params["event"]
                # Convert dict to SessionEvent object
                event = session_event_from_dict(event_dict)
                with self._sessions_lock:
                    session = self._sessions.get(session_id)
                if session:
                    session._dispatch_event(event)
            elif method == "session.lifecycle":
                # Handle session lifecycle events
                lifecycle_event = _session_lifecycle_event_from_dict(params)
                self._dispatch_lifecycle_event(lifecycle_event)

        self._client.set_notification_handler(handle_notification)
        self._client.set_request_handler("userInput.request", self._handle_user_input_request)
        self._client.set_request_handler(
            "exitPlanMode.request", self._handle_exit_plan_mode_request
        )
        self._client.set_request_handler(
            "autoModeSwitch.request", self._handle_auto_mode_switch_request
        )
        self._client.set_request_handler("hooks.invoke", self._handle_hooks_invoke)
        self._client.set_request_handler(
            "systemMessage.transform", self._handle_system_message_transform
        )
        register_client_session_api_handlers(self._client, self._get_client_session_handlers)

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

        # Create a TCP socket connection with timeout
        import socket

        # Connection timeout constant
        TCP_CONNECTION_TIMEOUT = 10  # seconds

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(TCP_CONNECTION_TIMEOUT)

        try:
            tcp_connect_start = time.perf_counter()
            logger.info(
                "CopilotClient._connect_via_tcp connecting to CLI server",
                extra={"host": self._actual_host, "port": self._runtime_port},
            )
            sock.connect((self._actual_host, self._runtime_port))
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
            raise RuntimeError(
                f"Failed to connect to CLI server at {self._actual_host}:{self._runtime_port}: {e}"
            )

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

        self._process = SocketWrapper(sock_file, sock)  # type: ignore
        self._client = JsonRpcClient(self._process)
        self._client.on_close = lambda: setattr(self, "_state", "disconnected")
        self._rpc = ServerRpc(self._client)

        # Set up notification handler for session events
        def handle_notification(method: str, params: dict):
            if method == "session.event":
                session_id = params["sessionId"]
                event_dict = params["event"]
                # Convert dict to SessionEvent object
                event = session_event_from_dict(event_dict)
                session = self._sessions.get(session_id)
                if session:
                    session._dispatch_event(event)
            elif method == "session.lifecycle":
                # Handle session lifecycle events
                lifecycle_event = _session_lifecycle_event_from_dict(params)
                self._dispatch_lifecycle_event(lifecycle_event)

        self._client.set_notification_handler(handle_notification)
        self._client.set_request_handler("userInput.request", self._handle_user_input_request)
        self._client.set_request_handler(
            "exitPlanMode.request", self._handle_exit_plan_mode_request
        )
        self._client.set_request_handler(
            "autoModeSwitch.request", self._handle_auto_mode_switch_request
        )
        self._client.set_request_handler("hooks.invoke", self._handle_hooks_invoke)
        self._client.set_request_handler(
            "systemMessage.transform", self._handle_system_message_transform
        )
        register_client_session_api_handlers(self._client, self._get_client_session_handlers)

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

        try:
            await session.rpc.options.update(params)
        except BaseException:
            with self._sessions_lock:
                self._sessions.pop(session.session_id, None)
            try:
                await session.disconnect()
            except BaseException:
                pass
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
            params["capabilities"] = self._session_fs_config["capabilities"]

        await self._client.request("sessionFs.setProvider", params)

    def _get_client_session_handlers(self, session_id: str) -> ClientSessionApiHandlers:
        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if session is None:
            raise ValueError(f"unknown session {session_id}")
        return session._client_session_apis

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

    async def _handle_hooks_invoke(self, params: dict) -> dict:
        """
        Handle a hooks invocation from the CLI server.

        Args:
            params: The hooks invocation parameters from the server.

        Returns:
            A dict containing the hook output.

        Raises:
            ValueError: If the request payload is invalid.
        """
        session_id = params.get("sessionId")
        hook_type = params.get("hookType")
        input_data = params.get("input")

        if not session_id or not hook_type:
            raise ValueError("invalid hooks invoke payload")

        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"unknown session {session_id}")

        output = await session._handle_hooks_invoke(hook_type, input_data)
        return {"output": output}

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
