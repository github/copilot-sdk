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
import os
import re
import shutil
import subprocess
import sys
import threading
import uuid
from collections.abc import Awaitable, Callable
from dataclasses import KW_ONLY, dataclass, field
from pathlib import Path
from types import TracebackType
from typing import Any, Literal, TypedDict, cast, overload

from ._jsonrpc import JsonRpcClient, ProcessExitedError
from ._sdk_protocol_version import get_sdk_protocol_version
from ._telemetry import get_trace_context, trace_context
from .generated.rpc import (
    ClientSessionApiHandlers,
    ServerRpc,
    register_client_session_api_handlers,
)
from .generated.session_events import (
    PermissionRequest,
    SessionEvent,
    session_event_from_dict,
)
from .session import (
    CommandDefinition,
    CopilotSession,
    CreateSessionFsHandler,
    CustomAgentConfig,
    DefaultAgentConfig,
    ElicitationHandler,
    InfiniteSessionConfig,
    MCPServerConfig,
    ProviderConfig,
    ReasoningEffort,
    SectionTransformFn,
    SessionFsConfig,
    SessionHooks,
    SystemMessageConfig,
    UserInputHandler,
    _PermissionHandlerFn,
)
from .session_fs_provider import create_session_fs_adapter
from .tools import Tool, ToolInvocation, ToolResult

# ============================================================================
# Connection Types
# ============================================================================

ConnectionState = Literal["disconnected", "connecting", "connected", "error"]

LogLevel = Literal["none", "error", "warning", "info", "debug", "all"]


def _validate_session_fs_config(config: SessionFsConfig) -> None:
    if not config.get("initial_cwd"):
        raise ValueError("session_fs.initial_cwd is required")
    if not config.get("session_state_path"):
        raise ValueError("session_fs.session_state_path is required")
    if config.get("conventions") not in ("posix", "windows"):
        raise ValueError("session_fs.conventions must be either 'posix' or 'windows'")


class TelemetryConfig(TypedDict, total=False):
    """Configuration for OpenTelemetry integration with the Copilot CLI."""

    otlp_endpoint: str
    """OTLP HTTP endpoint URL for trace/metric export. Sets OTEL_EXPORTER_OTLP_ENDPOINT."""
    file_path: str
    """File path for JSON-lines trace output. Sets COPILOT_OTEL_FILE_EXPORTER_PATH."""
    exporter_type: str
    """Exporter backend type: "otlp-http" or "file". Sets COPILOT_OTEL_EXPORTER_TYPE."""
    source_name: str
    """Instrumentation scope name. Sets COPILOT_OTEL_SOURCE_NAME."""
    capture_content: bool
    """Whether to capture message content. Sets OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT."""  # noqa: E501


@dataclass
class SubprocessConfig:
    """Config for spawning a local Copilot CLI subprocess.

    Example:
        >>> config = SubprocessConfig(github_token="ghp_...")
        >>> client = CopilotClient(config)

        >>> # Custom CLI path with TCP transport
        >>> config = SubprocessConfig(
        ...     cli_path="/usr/local/bin/copilot",
        ...     use_stdio=False,
        ...     log_level="debug",
        ... )
    """

    cli_path: str | None = None
    """Path to the Copilot CLI executable. ``None`` uses the bundled binary."""

    cli_args: list[str] = field(default_factory=list)
    """Extra arguments passed to the CLI executable (inserted before SDK-managed args)."""

    _: KW_ONLY

    cwd: str | None = None
    """Working directory for the CLI process. ``None`` uses the current directory."""

    use_stdio: bool = True
    """Use stdio transport (``True``, default) or TCP (``False``)."""

    port: int = 0
    """TCP port for the CLI server (only when ``use_stdio=False``). 0 means random."""

    log_level: LogLevel = "info"
    """Log level for the CLI process."""

    env: dict[str, str] | None = None
    """Environment variables for the CLI process. ``None`` inherits the current env."""

    github_token: str | None = None
    """GitHub token for authentication. Takes priority over other auth methods."""

    use_logged_in_user: bool | None = None
    """Use the logged-in user for authentication.

    ``None`` (default) resolves to ``True`` unless ``github_token`` is set.
    """

    telemetry: TelemetryConfig | None = None
    """OpenTelemetry configuration. Providing this enables telemetry — no separate flag needed."""

    session_fs: SessionFsConfig | None = None
    """Connection-level session filesystem provider configuration."""


@dataclass
class ExternalServerConfig:
    """Config for connecting to an existing Copilot CLI server over TCP.

    Example:
        >>> config = ExternalServerConfig(url="localhost:3000")
        >>> client = CopilotClient(config)
    """

    url: str
    """Server URL. Supports ``"host:port"``, ``"http://host:port"``, or just ``"port"``."""

    _: KW_ONLY

    session_fs: SessionFsConfig | None = None
    """Connection-level session filesystem provider configuration."""


# ============================================================================
# Response Types
# ============================================================================


@dataclass
class PingResponse:
    """Response from ping"""

    message: str  # Echo message with "pong: " prefix
    timestamp: int  # Server timestamp in milliseconds
    protocolVersion: int  # Protocol version for SDK compatibility

    @staticmethod
    def from_dict(obj: Any) -> PingResponse:
        assert isinstance(obj, dict)
        message = obj.get("message")
        timestamp = obj.get("timestamp")
        protocolVersion = obj.get("protocolVersion")
        if message is None or timestamp is None or protocolVersion is None:
            raise ValueError(
                f"Missing required fields in PingResponse: message={message}, "
                f"timestamp={timestamp}, protocolVersion={protocolVersion}"
            )
        return PingResponse(str(message), int(timestamp), int(protocolVersion))

    def to_dict(self) -> dict:
        result: dict = {}
        result["message"] = self.message
        result["timestamp"] = self.timestamp
        result["protocolVersion"] = self.protocolVersion
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
    protocolVersion: int  # Protocol version for SDK compatibility

    @staticmethod
    def from_dict(obj: Any) -> GetStatusResponse:
        assert isinstance(obj, dict)
        version = obj.get("version")
        protocolVersion = obj.get("protocolVersion")
        if version is None or protocolVersion is None:
            raise ValueError(
                f"Missing required fields in GetStatusResponse: version={version}, "
                f"protocolVersion={protocolVersion}"
            )
        return GetStatusResponse(str(version), int(protocolVersion))

    def to_dict(self) -> dict:
        result: dict = {}
        result["version"] = self.version
        result["protocolVersion"] = self.protocolVersion
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
class ModelVisionLimitsOverride:
    supported_media_types: list[str] | None = None
    max_prompt_images: int | None = None
    max_prompt_image_size: int | None = None


@dataclass
class ModelLimitsOverride:
    max_prompt_tokens: int | None = None
    max_output_tokens: int | None = None
    max_context_window_tokens: int | None = None
    vision: ModelVisionLimitsOverride | None = None


@dataclass
class ModelSupportsOverride:
    vision: bool | None = None
    reasoning_effort: bool | None = None


@dataclass
class ModelCapabilitiesOverride:
    supports: ModelSupportsOverride | None = None
    limits: ModelLimitsOverride | None = None


def _capabilities_to_dict(caps: ModelCapabilitiesOverride) -> dict:
    result: dict = {}
    if caps.supports is not None:
        s: dict = {}
        if caps.supports.vision is not None:
            s["vision"] = caps.supports.vision
        if caps.supports.reasoning_effort is not None:
            s["reasoningEffort"] = caps.supports.reasoning_effort
        if s:
            result["supports"] = s
    if caps.limits is not None:
        lim: dict = {}
        if caps.limits.max_prompt_tokens is not None:
            lim["max_prompt_tokens"] = caps.limits.max_prompt_tokens
        if caps.limits.max_output_tokens is not None:
            lim["max_output_tokens"] = caps.limits.max_output_tokens
        if caps.limits.max_context_window_tokens is not None:
            lim["max_context_window_tokens"] = caps.limits.max_context_window_tokens
        if caps.limits.vision is not None:
            v: dict = {}
            if caps.limits.vision.supported_media_types is not None:
                v["supported_media_types"] = caps.limits.vision.supported_media_types
            if caps.limits.vision.max_prompt_images is not None:
                v["max_prompt_images"] = caps.limits.vision.max_prompt_images
            if caps.limits.vision.max_prompt_image_size is not None:
                v["max_prompt_image_size"] = caps.limits.vision.max_prompt_image_size
            if v:
                lim["vision"] = v
        if lim:
            result["limits"] = lim
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

    multiplier: float

    @staticmethod
    def from_dict(obj: Any) -> ModelBilling:
        assert isinstance(obj, dict)
        multiplier = obj.get("multiplier")
        if multiplier is None:
            raise ValueError("Missing required field 'multiplier' in ModelBilling")
        return ModelBilling(multiplier=float(multiplier))

    def to_dict(self) -> dict:
        result: dict = {}
        result["multiplier"] = self.multiplier
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

    cwd: str  # Working directory where the session was created
    gitRoot: str | None = None  # Git repository root (if in a git repo)
    repository: str | None = None  # GitHub repository in "owner/repo" format
    branch: str | None = None  # Current git branch

    @staticmethod
    def from_dict(obj: Any) -> SessionContext:
        assert isinstance(obj, dict)
        cwd = obj.get("cwd")
        if cwd is None:
            raise ValueError("Missing required field 'cwd' in SessionContext")
        return SessionContext(
            cwd=str(cwd),
            gitRoot=obj.get("gitRoot"),
            repository=obj.get("repository"),
            branch=obj.get("branch"),
        )

    def to_dict(self) -> dict:
        result: dict = {"cwd": self.cwd}
        if self.gitRoot is not None:
            result["gitRoot"] = self.gitRoot
        if self.repository is not None:
            result["repository"] = self.repository
        if self.branch is not None:
            result["branch"] = self.branch
        return result


@dataclass
class SessionListFilter:
    """Filter options for listing sessions"""

    cwd: str | None = None  # Filter by exact cwd match
    gitRoot: str | None = None  # Filter by git root
    repository: str | None = None  # Filter by repository (owner/repo format)
    branch: str | None = None  # Filter by branch

    def to_dict(self) -> dict:
        result: dict = {}
        if self.cwd is not None:
            result["cwd"] = self.cwd
        if self.gitRoot is not None:
            result["gitRoot"] = self.gitRoot
        if self.repository is not None:
            result["repository"] = self.repository
        if self.branch is not None:
            result["branch"] = self.branch
        return result


@dataclass
class SessionMetadata:
    """Metadata about a session"""

    sessionId: str  # Session identifier
    startTime: str  # ISO 8601 timestamp when session was created
    modifiedTime: str  # ISO 8601 timestamp when session was last modified
    isRemote: bool  # Whether the session is remote
    summary: str | None = None  # Optional summary of the session
    context: SessionContext | None = None  # Working directory context

    @staticmethod
    def from_dict(obj: Any) -> SessionMetadata:
        assert isinstance(obj, dict)
        sessionId = obj.get("sessionId")
        startTime = obj.get("startTime")
        modifiedTime = obj.get("modifiedTime")
        isRemote = obj.get("isRemote")
        if sessionId is None or startTime is None or modifiedTime is None or isRemote is None:
            raise ValueError(
                f"Missing required fields in SessionMetadata: sessionId={sessionId}, "
                f"startTime={startTime}, modifiedTime={modifiedTime}, isRemote={isRemote}"
            )
        summary = obj.get("summary")
        context_dict = obj.get("context")
        context = SessionContext.from_dict(context_dict) if context_dict else None
        return SessionMetadata(
            sessionId=str(sessionId),
            startTime=str(startTime),
            modifiedTime=str(modifiedTime),
            isRemote=bool(isRemote),
            summary=summary,
            context=context,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = self.sessionId
        result["startTime"] = self.startTime
        result["modifiedTime"] = self.modifiedTime
        result["isRemote"] = self.isRemote
        if self.summary is not None:
            result["summary"] = self.summary
        if self.context is not None:
            result["context"] = self.context.to_dict()
        return result


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

    startTime: str
    modifiedTime: str
    summary: str | None = None

    @staticmethod
    def from_dict(data: dict) -> SessionLifecycleEventMetadata:
        return SessionLifecycleEventMetadata(
            startTime=data.get("startTime", ""),
            modifiedTime=data.get("modifiedTime", ""),
            summary=data.get("summary"),
        )


@dataclass
class SessionLifecycleEvent:
    """Session lifecycle event notification."""

    type: SessionLifecycleEventType
    sessionId: str
    metadata: SessionLifecycleEventMetadata | None = None

    @staticmethod
    def from_dict(data: dict) -> SessionLifecycleEvent:
        metadata = None
        if "metadata" in data and data["metadata"]:
            metadata = SessionLifecycleEventMetadata.from_dict(data["metadata"])
        return SessionLifecycleEvent(
            type=data.get("type", "session.updated"),
            sessionId=data.get("sessionId", ""),
            metadata=metadata,
        )


SessionLifecycleHandler = Callable[[SessionLifecycleEvent], None]

HandlerUnsubcribe = Callable[[], None]

NO_RESULT_PERMISSION_V2_ERROR = (
    "Permission handlers cannot return 'no-result' when connected to a protocol v2 server."
)

# Minimum protocol version this SDK can communicate with.
# Servers reporting a version below this are rejected.
MIN_PROTOCOL_VERSION = 2


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
        >>> client = CopilotClient(ExternalServerConfig(url="localhost:3000"))
    """

    def __init__(
        self,
        config: SubprocessConfig | ExternalServerConfig | None = None,
        *,
        auto_start: bool = True,
        on_list_models: Callable[[], list[ModelInfo] | Awaitable[list[ModelInfo]]] | None = None,
    ):
        """
        Initialize a new CopilotClient.

        Args:
            config: Connection configuration. Pass a :class:`SubprocessConfig` to
                spawn a local CLI process, or an :class:`ExternalServerConfig` to
                connect to an existing server. Defaults to ``SubprocessConfig()``.
            auto_start: Automatically start the connection on first use
                (default: ``True``).
            on_list_models: Custom handler for :meth:`list_models`. When provided,
                the handler is called instead of querying the CLI server.

        Example:
            >>> # Default — spawns CLI server using stdio
            >>> client = CopilotClient()
            >>>
            >>> # Connect to an existing server
            >>> client = CopilotClient(ExternalServerConfig(url="localhost:3000"))
            >>>
            >>> # Custom CLI path with specific log level
            >>> client = CopilotClient(
            ...     SubprocessConfig(
            ...         cli_path="/usr/local/bin/copilot",
            ...         log_level="debug",
            ...     )
            ... )
        """
        if config is None:
            config = SubprocessConfig()

        self._config: SubprocessConfig | ExternalServerConfig = config
        self._auto_start = auto_start
        self._on_list_models = on_list_models

        # Resolve connection-mode-specific state
        self._actual_host: str = "localhost"
        self._is_external_server: bool = isinstance(config, ExternalServerConfig)

        if isinstance(config, ExternalServerConfig):
            self._actual_host, actual_port = self._parse_cli_url(config.url)
            self._actual_port: int | None = actual_port
        else:
            self._actual_port = None

            # Resolve CLI path: explicit > COPILOT_CLI_PATH env var > bundled binary
            effective_env = config.env if config.env is not None else os.environ
            if config.cli_path is None:
                env_cli_path = effective_env.get("COPILOT_CLI_PATH")
                if env_cli_path:
                    config.cli_path = env_cli_path
                else:
                    bundled_path = _get_bundled_cli_path()
                    if bundled_path:
                        config.cli_path = bundled_path
                    else:
                        raise RuntimeError(
                            "Copilot CLI not found. The bundled CLI binary is not available. "
                            "Ensure you installed a platform-specific wheel, or provide cli_path."
                        )

            # Resolve use_logged_in_user default
            if config.use_logged_in_user is None:
                config.use_logged_in_user = not bool(config.github_token)

        self._process: subprocess.Popen | None = None
        self._client: JsonRpcClient | None = None
        self._state: ConnectionState = "disconnected"
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
        if config.session_fs is not None:
            _validate_session_fs_config(config.session_fs)
        self._session_fs_config = config.session_fs

    @property
    def rpc(self) -> ServerRpc:
        """Typed server-scoped RPC methods."""
        if self._rpc is None:
            raise RuntimeError("Client is not connected. Call start() first.")
        return self._rpc

    @property
    def actual_port(self) -> int | None:
        """The actual TCP port the CLI server is listening on, if using TCP transport.

        Useful for multi-client scenarios where a second client needs to connect
        to the same server. Only available after :meth:`start` completes and
        only when not using stdio transport.
        """
        return self._actual_port

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

        If connecting to an external server (via :class:`ExternalServerConfig`),
        only establishes the connection. Otherwise, spawns the CLI server process
        and then connects.

        This method is called automatically when creating a session if ``auto_start``
        is True (default).

        Raises:
            RuntimeError: If the server fails to start or the connection fails.

        Example:
            >>> client = CopilotClient(auto_start=False)
            >>> await client.start()
            >>> # Now ready to create sessions
        """
        if self._state == "connected":
            return

        self._state = "connecting"

        try:
            # Only start CLI server process if not connecting to external server
            if not self._is_external_server:
                await self._start_cli_server()

            # Connect to the server
            await self._connect_to_server()

            # Verify protocol version compatibility
            await self._verify_protocol_version()

            if self._session_fs_config:
                await self._set_session_fs_provider()

            self._state = "connected"
        except ProcessExitedError as e:
            # Process exited with error - reraise as RuntimeError with stderr
            self._state = "error"
            raise RuntimeError(str(e)) from None
        except Exception as e:
            self._state = "error"
            # Check if process exited and capture any remaining stderr
            if self._process and hasattr(self._process, "poll"):
                return_code = self._process.poll()
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
        2. Closes the JSON-RPC connection
        3. Terminates the CLI server process (if spawned by this client)

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
                errors.append(
                    StopError(message=f"Failed to disconnect session {session.session_id}: {e}")
                )

        # Close client
        if self._client:
            await self._client.stop()
            self._client = None
        self._rpc = None

        # Clear models cache
        async with self._models_cache_lock:
            self._models_cache = None

        # Kill CLI process (only if we spawned it)
        if self._process and not self._is_external_server:
            self._process.terminate()
            try:
                self._process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self._process.kill()
            self._process = None

        self._state = "disconnected"
        if not self._is_external_server:
            self._actual_port = None

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
        if self._process:
            try:
                if self._is_external_server:
                    self._process.terminate()  # closes the TCP socket
                else:
                    self._process.kill()
                    self._process = None
            except Exception:
                pass

        # Then clean up the JSON-RPC client
        if self._client:
            try:
                await self._client.stop()
            except Exception:
                pass  # Ignore errors during force stop
            self._client = None
        self._rpc = None

        # Clear models cache
        async with self._models_cache_lock:
            self._models_cache = None

        self._state = "disconnected"
        if not self._is_external_server:
            self._actual_port = None

    async def create_session(
        self,
        *,
        on_permission_request: _PermissionHandlerFn,
        model: str | None = None,
        session_id: str | None = None,
        client_name: str | None = None,
        reasoning_effort: ReasoningEffort | None = None,
        tools: list[Tool] | None = None,
        system_message: SystemMessageConfig | None = None,
        available_tools: list[str] | None = None,
        excluded_tools: list[str] | None = None,
        on_user_input_request: UserInputHandler | None = None,
        hooks: SessionHooks | None = None,
        working_directory: str | None = None,
        provider: ProviderConfig | None = None,
        model_capabilities: ModelCapabilitiesOverride | None = None,
        streaming: bool | None = None,
        include_sub_agent_streaming_events: bool | None = None,
        mcp_servers: dict[str, MCPServerConfig] | None = None,
        custom_agents: list[CustomAgentConfig] | None = None,
        default_agent: DefaultAgentConfig | dict[str, Any] | None = None,
        agent: str | None = None,
        config_dir: str | None = None,
        enable_config_discovery: bool | None = None,
        skill_directories: list[str] | None = None,
        disabled_skills: list[str] | None = None,
        infinite_sessions: InfiniteSessionConfig | None = None,
        on_event: Callable[[SessionEvent], None] | None = None,
        commands: list[CommandDefinition] | None = None,
        on_elicitation_request: ElicitationHandler | None = None,
        create_session_fs_handler: CreateSessionFsHandler | None = None,
    ) -> CopilotSession:
        """
        Create a new conversation session with the Copilot CLI.

        Sessions maintain conversation state, handle events, and manage tool execution.
        If the client is not connected and ``auto_start`` is enabled, this will
        automatically start the connection.

        Args:
            on_permission_request: Handler for permission requests. Use
                ``PermissionHandler.approve_all`` to allow all permissions.
            model: The model to use for the session (e.g. ``"gpt-4"``).
            session_id: Optional session ID. If not provided, a UUID is generated.
            client_name: Optional client name for identification.
            reasoning_effort: Reasoning effort level for the model.
            tools: Custom tools to register with the session.
            system_message: System message configuration.
            available_tools: Allowlist of built-in tools to enable.
            excluded_tools: List of built-in tools to disable.
            on_user_input_request: Handler for user input requests.
            hooks: Lifecycle hooks for the session.
            working_directory: Working directory for the session.
            provider: Provider configuration for Azure or custom endpoints.
            model_capabilities: Override individual model capabilities resolved by the runtime.
            streaming: Whether to enable streaming responses.
            include_sub_agent_streaming_events: Whether to include sub-agent streaming
                delta events (e.g., ``assistant.message_delta``,
                ``assistant.reasoning_delta``, ``assistant.streaming_delta`` with
                ``agentId`` set). When False, only non-streaming sub-agent events and
                ``subagent.*`` lifecycle events are forwarded. Defaults to True.
            mcp_servers: MCP server configurations.
            custom_agents: Custom agent configurations.
            default_agent: Configuration for the default agent,
                including tool visibility controls.
            agent: Agent to use for the session.
            config_dir: Override for the configuration directory.
            enable_config_discovery: When True, automatically discovers MCP server
                configurations (e.g. ``.mcp.json``, ``.vscode/mcp.json``) and skill
                directories from the working directory and merges them with any
                explicitly provided ``mcp_servers`` and ``skill_directories``, with
                explicit values taking precedence on name collision. Custom instruction
                files (``.github/copilot-instructions.md``, ``AGENTS.md``, etc.) are
                always loaded regardless of this setting.
            skill_directories: Directories to search for skills.
            disabled_skills: Skills to disable.
            infinite_sessions: Infinite session configuration.
            on_event: Callback for session events.

        Returns:
            A :class:`CopilotSession` instance for the new session.

        Raises:
            RuntimeError: If the client is not connected and auto_start is disabled.
            ValueError: If ``on_permission_request`` is not a valid callable.

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
        if not on_permission_request or not callable(on_permission_request):
            raise ValueError(
                "A valid on_permission_request handler is required. "
                "Use PermissionHandler.approve_all or provide a custom handler."
            )
        if not self._client:
            if self._auto_start:
                await self.start()
            else:
                raise RuntimeError("Client not connected. Call start() first.")

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
                tool_defs.append(definition)

        payload: dict[str, Any] = {}
        if model:
            payload["model"] = model
        if client_name:
            payload["clientName"] = client_name
        if reasoning_effort:
            payload["reasoningEffort"] = reasoning_effort
        if tool_defs:
            payload["tools"] = tool_defs

        wire_system_message, transform_callbacks = _extract_transform_callbacks(system_message)
        if wire_system_message:
            payload["systemMessage"] = wire_system_message

        if available_tools is not None:
            payload["availableTools"] = available_tools
        if excluded_tools is not None:
            payload["excludedTools"] = excluded_tools

        # Always enable permission request callback
        payload["requestPermission"] = True

        # Enable user input request callback if handler provided
        if on_user_input_request:
            payload["requestUserInput"] = True

        # Enable elicitation request callback if handler provided
        payload["requestElicitation"] = bool(on_elicitation_request)

        # Serialize commands (name + description only) into payload
        if commands:
            payload["commands"] = [
                {"name": cmd.name, "description": cmd.description} for cmd in commands
            ]

        # Enable hooks callback if any hook handler provided
        if hooks and any(hooks.values()):
            payload["hooks"] = True

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

        # Add model capabilities override if provided
        if model_capabilities:
            payload["modelCapabilities"] = _capabilities_to_dict(model_capabilities)

        # Add MCP servers configuration if provided
        if mcp_servers:
            payload["mcpServers"] = mcp_servers
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
        if config_dir:
            payload["configDir"] = config_dir

        # Add config discovery flag if provided
        if enable_config_discovery is not None:
            payload["enableConfigDiscovery"] = enable_config_discovery

        # Add skill directories configuration if provided
        if skill_directories:
            payload["skillDirectories"] = skill_directories

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

        if not self._client:
            raise RuntimeError("Client not connected")

        actual_session_id = session_id or str(uuid.uuid4())
        payload["sessionId"] = actual_session_id

        # Propagate W3C Trace Context to CLI if OpenTelemetry is active
        trace_ctx = get_trace_context()
        payload.update(trace_ctx)

        # Create and register the session before issuing the RPC so that
        # events emitted by the CLI (e.g. session.start) are not dropped.
        session = CopilotSession(actual_session_id, self._client, workspace_path=None)
        if self._session_fs_config:
            if create_session_fs_handler is None:
                raise ValueError(
                    "create_session_fs_handler is required in session config when "
                    "session_fs is enabled in client options."
                )
            session._client_session_apis.session_fs = create_session_fs_adapter(
                create_session_fs_handler(session)
            )
        session._register_tools(tools)
        session._register_commands(commands)
        session._register_permission_handler(on_permission_request)
        if on_user_input_request:
            session._register_user_input_handler(on_user_input_request)
        if on_elicitation_request:
            session._register_elicitation_handler(on_elicitation_request)
        if hooks:
            session._register_hooks(hooks)
        if transform_callbacks:
            session._register_transform_callbacks(transform_callbacks)
        if on_event:
            session.on(on_event)
        with self._sessions_lock:
            self._sessions[actual_session_id] = session

        try:
            response = await self._client.request("session.create", payload)
            session._workspace_path = response.get("workspacePath")
            capabilities = response.get("capabilities")
            session._set_capabilities(capabilities)
        except BaseException:
            with self._sessions_lock:
                self._sessions.pop(actual_session_id, None)
            raise

        return session

    async def resume_session(
        self,
        session_id: str,
        *,
        on_permission_request: _PermissionHandlerFn,
        model: str | None = None,
        client_name: str | None = None,
        reasoning_effort: ReasoningEffort | None = None,
        tools: list[Tool] | None = None,
        system_message: SystemMessageConfig | None = None,
        available_tools: list[str] | None = None,
        excluded_tools: list[str] | None = None,
        on_user_input_request: UserInputHandler | None = None,
        hooks: SessionHooks | None = None,
        working_directory: str | None = None,
        provider: ProviderConfig | None = None,
        model_capabilities: ModelCapabilitiesOverride | None = None,
        streaming: bool | None = None,
        include_sub_agent_streaming_events: bool | None = None,
        mcp_servers: dict[str, MCPServerConfig] | None = None,
        custom_agents: list[CustomAgentConfig] | None = None,
        default_agent: DefaultAgentConfig | dict[str, Any] | None = None,
        agent: str | None = None,
        config_dir: str | None = None,
        enable_config_discovery: bool | None = None,
        skill_directories: list[str] | None = None,
        disabled_skills: list[str] | None = None,
        infinite_sessions: InfiniteSessionConfig | None = None,
        on_event: Callable[[SessionEvent], None] | None = None,
        commands: list[CommandDefinition] | None = None,
        on_elicitation_request: ElicitationHandler | None = None,
        create_session_fs_handler: CreateSessionFsHandler | None = None,
    ) -> CopilotSession:
        """
        Resume an existing conversation session by its ID.

        This allows you to continue a previous conversation, maintaining all
        conversation history. The session must have been previously created
        and not deleted.

        Args:
            session_id: The ID of the session to resume.
            on_permission_request: Handler for permission requests. Use
                ``PermissionHandler.approve_all`` to allow all permissions.
            model: The model to use for the resumed session.
            client_name: Optional client name for identification.
            reasoning_effort: Reasoning effort level for the model.
            tools: Custom tools to register with the session.
            system_message: System message configuration.
            available_tools: Allowlist of built-in tools to enable.
            excluded_tools: List of built-in tools to disable.
            on_user_input_request: Handler for user input requests.
            hooks: Lifecycle hooks for the session.
            working_directory: Working directory for the session.
            provider: Provider configuration for Azure or custom endpoints.
            model_capabilities: Override individual model capabilities resolved by the runtime.
            streaming: Whether to enable streaming responses.
            include_sub_agent_streaming_events: Whether to include sub-agent streaming
                delta events (e.g., ``assistant.message_delta``,
                ``assistant.reasoning_delta``, ``assistant.streaming_delta`` with
                ``agentId`` set). When False, only non-streaming sub-agent events and
                ``subagent.*`` lifecycle events are forwarded. Defaults to True.
            mcp_servers: MCP server configurations.
            custom_agents: Custom agent configurations.
            default_agent: Configuration for the default agent,
                including tool visibility controls.
            agent: Agent to use for the session.
            config_dir: Override for the configuration directory.
            enable_config_discovery: When True, automatically discovers MCP server
                configurations (e.g. ``.mcp.json``, ``.vscode/mcp.json``) and skill
                directories from the working directory and merges them with any
                explicitly provided ``mcp_servers`` and ``skill_directories``, with
                explicit values taking precedence on name collision. Custom instruction
                files (``.github/copilot-instructions.md``, ``AGENTS.md``, etc.) are
                always loaded regardless of this setting.
            skill_directories: Directories to search for skills.
            disabled_skills: Skills to disable.
            infinite_sessions: Infinite session configuration.
            on_event: Callback for session events.

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
        if not on_permission_request or not callable(on_permission_request):
            raise ValueError(
                "A valid on_permission_request handler is required. "
                "Use PermissionHandler.approve_all or provide a custom handler."
            )
        if not self._client:
            if self._auto_start:
                await self.start()
            else:
                raise RuntimeError("Client not connected. Call start() first.")

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
                tool_defs.append(definition)

        payload: dict[str, Any] = {"sessionId": session_id}

        if client_name:
            payload["clientName"] = client_name
        if model:
            payload["model"] = model
        if reasoning_effort:
            payload["reasoningEffort"] = reasoning_effort
        if tool_defs:
            payload["tools"] = tool_defs
        wire_system_message, transform_callbacks = _extract_transform_callbacks(system_message)
        if wire_system_message:
            payload["systemMessage"] = wire_system_message
        if available_tools is not None:
            payload["availableTools"] = available_tools
        if excluded_tools is not None:
            payload["excludedTools"] = excluded_tools
        if provider:
            payload["provider"] = self._convert_provider_to_wire_format(provider)
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

        # Always enable permission request callback
        payload["requestPermission"] = True

        if on_user_input_request:
            payload["requestUserInput"] = True

        # Enable elicitation request callback if handler provided
        payload["requestElicitation"] = bool(on_elicitation_request)

        # Serialize commands (name + description only) into payload
        if commands:
            payload["commands"] = [
                {"name": cmd.name, "description": cmd.description} for cmd in commands
            ]

        if hooks and any(hooks.values()):
            payload["hooks"] = True

        if working_directory:
            payload["workingDirectory"] = working_directory
        if config_dir:
            payload["configDir"] = config_dir
        if enable_config_discovery is not None:
            payload["enableConfigDiscovery"] = enable_config_discovery

        # TODO: disable_resume is not a keyword arg yet; keeping for future use
        if mcp_servers:
            payload["mcpServers"] = mcp_servers
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

        if not self._client:
            raise RuntimeError("Client not connected")

        # Propagate W3C Trace Context to CLI if OpenTelemetry is active
        trace_ctx = get_trace_context()
        payload.update(trace_ctx)

        # Create and register the session before issuing the RPC so that
        # events emitted by the CLI (e.g. session.start) are not dropped.
        session = CopilotSession(session_id, self._client, workspace_path=None)
        if self._session_fs_config:
            if create_session_fs_handler is None:
                raise ValueError(
                    "create_session_fs_handler is required in session config when "
                    "session_fs is enabled in client options."
                )
            session._client_session_apis.session_fs = create_session_fs_adapter(
                create_session_fs_handler(session)
            )
        session._register_tools(tools)
        session._register_commands(commands)
        session._register_permission_handler(on_permission_request)
        if on_user_input_request:
            session._register_user_input_handler(on_user_input_request)
        if on_elicitation_request:
            session._register_elicitation_handler(on_elicitation_request)
        if hooks:
            session._register_hooks(hooks)
        if transform_callbacks:
            session._register_transform_callbacks(transform_callbacks)
        if on_event:
            session.on(on_event)
        with self._sessions_lock:
            self._sessions[session_id] = session

        try:
            response = await self._client.request("session.resume", payload)
            session._workspace_path = response.get("workspacePath")
            capabilities = response.get("capabilities")
            session._set_capabilities(capabilities)
        except BaseException:
            with self._sessions_lock:
                self._sessions.pop(session_id, None)
            raise

        return session

    def get_state(self) -> ConnectionState:
        """
        Get the current connection state of the client.

        Returns:
            The current connection state: "disconnected", "connecting",
            "connected", or "error".

        Example:
            >>> if client.get_state() == "connected":
            ...     session = await client.create_session()
        """
        return self._state

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
            filter: Optional filter to narrow down the list of sessions by cwd, git root,
                repository, or branch.

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
            ...     print(f"Session started at: {metadata.startTime}")
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
    def on(self, handler: SessionLifecycleHandler, /) -> HandlerUnsubcribe: ...

    @overload
    def on(
        self, event_type: SessionLifecycleEventType, /, handler: SessionLifecycleHandler
    ) -> HandlerUnsubcribe: ...

    def on(
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
        - on(handler): Subscribe to all lifecycle events
        - on(event_type, handler): Subscribe to a specific event type

        Args:
            event_type_or_handler: Either a specific event type to listen for,
                or a handler function for all events.
            handler: Handler function when subscribing to a specific event type.

        Returns:
            A function that, when called, unsubscribes the handler.

        Example:
            >>> # Subscribe to specific event type
            >>> unsubscribe = client.on("session.foreground", lambda e: print(e.sessionId))
            >>>
            >>> # Subscribe to all events
            >>> unsubscribe = client.on(lambda e: print(f"{e.type}: {e.sessionId}"))
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
                raise ValueError("Invalid arguments: use on(handler) or on(event_type, handler)")

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
        """Verify that the server's protocol version is within the supported range
        and store the negotiated version."""
        max_version = get_sdk_protocol_version()
        ping_result = await self.ping()
        server_version = ping_result.protocolVersion

        if server_version is None:
            raise RuntimeError(
                "SDK protocol version mismatch: "
                f"SDK supports versions {MIN_PROTOCOL_VERSION}-{max_version}"
                ", but server does not report a protocol version. "
                "Please update your server to ensure compatibility."
            )

        if server_version < MIN_PROTOCOL_VERSION or server_version > max_version:
            raise RuntimeError(
                "SDK protocol version mismatch: "
                f"SDK supports versions {MIN_PROTOCOL_VERSION}-{max_version}"
                f", but server reports version {server_version}. "
                "Please update your SDK or server to ensure compatibility."
            )

        self._negotiated_protocol_version = server_version

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
            wire_agent["mcpServers"] = agent["mcp_servers"]
        if "infer" in agent:
            wire_agent["infer"] = agent["infer"]
        if "skills" in agent:
            wire_agent["skills"] = agent["skills"]
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
        """
        Start the CLI server process.

        This spawns the CLI server as a subprocess using the configured transport
        mode (stdio or TCP).

        Raises:
            RuntimeError: If the server fails to start or times out.
        """
        assert isinstance(self._config, SubprocessConfig)
        cfg = self._config

        cli_path = cfg.cli_path
        assert cli_path is not None  # resolved in __init__

        # Verify CLI exists
        if not os.path.exists(cli_path):
            original_path = cli_path
            if (cli_path := shutil.which(cli_path)) is None:
                raise RuntimeError(f"Copilot CLI not found at {original_path}")

        # Start with user-provided cli_args, then add SDK-managed args
        args = list(cfg.cli_args) + [
            "--headless",
            "--no-auto-update",
            "--log-level",
            cfg.log_level,
        ]

        # Add auth-related flags
        if cfg.github_token:
            args.extend(["--auth-token-env", "COPILOT_SDK_AUTH_TOKEN"])
        if not cfg.use_logged_in_user:
            args.append("--no-auto-login")

        # If cli_path is a .js file, run it with node
        # Note that we can't rely on the shebang as Windows doesn't support it
        if cli_path.endswith(".js"):
            args = ["node", cli_path] + args
        else:
            args = [cli_path] + args

        # Get environment variables
        if cfg.env is None:
            env = dict(os.environ)
        else:
            env = dict(cfg.env)

        # Set auth token in environment if provided
        if cfg.github_token:
            env["COPILOT_SDK_AUTH_TOKEN"] = cfg.github_token

        # Set OpenTelemetry environment variables if telemetry config is provided
        telemetry = cfg.telemetry
        if telemetry is not None:
            env["COPILOT_OTEL_ENABLED"] = "true"
            if "otlp_endpoint" in telemetry:
                env["OTEL_EXPORTER_OTLP_ENDPOINT"] = telemetry["otlp_endpoint"]
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

        cwd = cfg.cwd or os.getcwd()

        # Choose transport mode
        if cfg.use_stdio:
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
        else:
            if cfg.port > 0:
                args.extend(["--port", str(cfg.port)])
            self._process = subprocess.Popen(
                args,
                stdin=subprocess.DEVNULL,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                cwd=cwd,
                env=env,
                creationflags=creationflags,
            )

        # For stdio mode, we're ready immediately
        if cfg.use_stdio:
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
                match = re.search(r"listening on port (\d+)", line_str, re.IGNORECASE)
                if match:
                    self._actual_port = int(match.group(1))
                    return

        try:
            await asyncio.wait_for(read_port(), timeout=10.0)
        except TimeoutError:
            raise RuntimeError("Timeout waiting for CLI server to start")

    async def _connect_to_server(self) -> None:
        """
        Connect to the CLI server via the configured transport.

        Uses either stdio or TCP based on the client configuration.

        Raises:
            RuntimeError: If the connection fails.
        """
        use_stdio = isinstance(self._config, SubprocessConfig) and self._config.use_stdio
        if use_stdio:
            await self._connect_via_stdio()
        else:
            await self._connect_via_tcp()

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
                lifecycle_event = SessionLifecycleEvent.from_dict(params)
                self._dispatch_lifecycle_event(lifecycle_event)

        self._client.set_notification_handler(handle_notification)
        # Protocol v3 servers send tool calls / permission requests as broadcast events.
        # Protocol v2 servers use the older tool.call / permission.request RPC model.
        # We always register v2 adapters because handlers are set up before version
        # negotiation; a v3 server will simply never send these requests.
        self._client.set_request_handler("tool.call", self._handle_tool_call_request_v2)
        self._client.set_request_handler("permission.request", self._handle_permission_request_v2)
        self._client.set_request_handler("userInput.request", self._handle_user_input_request)
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
        if not self._actual_port:
            raise RuntimeError("Server port not available")

        # Create a TCP socket connection with timeout
        import socket

        # Connection timeout constant
        TCP_CONNECTION_TIMEOUT = 10  # seconds

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(TCP_CONNECTION_TIMEOUT)

        try:
            sock.connect((self._actual_host, self._actual_port))
            sock.settimeout(None)  # Remove timeout after connection
        except OSError as e:
            raise RuntimeError(
                f"Failed to connect to CLI server at {self._actual_host}:{self._actual_port}: {e}"
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
                lifecycle_event = SessionLifecycleEvent.from_dict(params)
                self._dispatch_lifecycle_event(lifecycle_event)

        self._client.set_notification_handler(handle_notification)
        # Protocol v3 servers send tool calls / permission requests as broadcast events.
        # Protocol v2 servers use the older tool.call / permission.request RPC model.
        # We always register v2 adapters; a v3 server will simply never send these requests.
        self._client.set_request_handler("tool.call", self._handle_tool_call_request_v2)
        self._client.set_request_handler("permission.request", self._handle_permission_request_v2)
        self._client.set_request_handler("userInput.request", self._handle_user_input_request)
        self._client.set_request_handler("hooks.invoke", self._handle_hooks_invoke)
        self._client.set_request_handler(
            "systemMessage.transform", self._handle_system_message_transform
        )
        register_client_session_api_handlers(self._client, self._get_client_session_handlers)

        # Start listening for messages
        loop = asyncio.get_running_loop()
        self._client.start(loop)

    async def _set_session_fs_provider(self) -> None:
        if not self._session_fs_config or not self._client:
            return

        await self._client.request(
            "sessionFs.setProvider",
            {
                "initialCwd": self._session_fs_config["initial_cwd"],
                "sessionStatePath": self._session_fs_config["session_state_path"],
                "conventions": self._session_fs_config["conventions"],
            },
        )

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

    # ========================================================================
    # Protocol v2 backward-compatibility adapters
    # ========================================================================

    async def _handle_tool_call_request_v2(self, params: dict) -> dict:
        """Handle a v2-style tool.call RPC request from the server."""
        session_id = params.get("sessionId")
        tool_call_id = params.get("toolCallId")
        tool_name = params.get("toolName")

        if not session_id or not tool_call_id or not tool_name:
            raise ValueError("invalid tool call payload")

        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"unknown session {session_id}")

        handler = session._get_tool_handler(tool_name)
        if not handler:
            return {
                "result": {
                    "textResultForLlm": (
                        f"Tool '{tool_name}' is not supported by this client instance."
                    ),
                    "resultType": "failure",
                    "error": f"tool '{tool_name}' not supported",
                    "toolTelemetry": {},
                }
            }

        arguments = params.get("arguments")
        invocation = ToolInvocation(
            session_id=session_id,
            tool_call_id=tool_call_id,
            tool_name=tool_name,
            arguments=arguments,
        )

        tp = params.get("traceparent")
        ts = params.get("tracestate")

        try:
            with trace_context(tp, ts):
                result = handler(invocation)
                if inspect.isawaitable(result):
                    result = await result

            tool_result: ToolResult = result  # type: ignore[assignment]
            return {
                "result": {
                    "textResultForLlm": tool_result.text_result_for_llm,
                    "resultType": tool_result.result_type,
                    "error": tool_result.error,
                    "toolTelemetry": tool_result.tool_telemetry or {},
                }
            }
        except Exception as exc:
            return {
                "result": {
                    "textResultForLlm": (
                        "Invoking this tool produced an error."
                        " Detailed information is not available."
                    ),
                    "resultType": "failure",
                    "error": str(exc),
                    "toolTelemetry": {},
                }
            }

    async def _handle_permission_request_v2(self, params: dict) -> dict:
        """Handle a v2-style permission.request RPC request from the server."""
        session_id = params.get("sessionId")
        permission_request = params.get("permissionRequest")

        if not session_id or not permission_request:
            raise ValueError("invalid permission request payload")

        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"unknown session {session_id}")

        try:
            perm_request = PermissionRequest.from_dict(permission_request)
            result = await session._handle_permission_request(perm_request)
            if result.kind == "no-result":
                raise ValueError(NO_RESULT_PERMISSION_V2_ERROR)
            result_payload: dict = {"kind": result.kind}
            if result.rules is not None:
                result_payload["rules"] = result.rules
            if result.feedback is not None:
                result_payload["feedback"] = result.feedback
            if result.message is not None:
                result_payload["message"] = result.message
            if result.path is not None:
                result_payload["path"] = result.path
            return {"result": result_payload}
        except ValueError as exc:
            if str(exc) == NO_RESULT_PERMISSION_V2_ERROR:
                raise
            return {
                "result": {
                    "kind": "denied-no-approval-rule-and-could-not-request-from-user",
                }
            }
        except Exception:  # pylint: disable=broad-except
            return {
                "result": {
                    "kind": "denied-no-approval-rule-and-could-not-request-from-user",
                }
            }
