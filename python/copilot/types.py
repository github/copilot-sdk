"""
Type definitions for the Copilot SDK
"""

from __future__ import annotations

from collections.abc import Awaitable
from dataclasses import dataclass
from typing import Any, Callable, Literal, TypedDict, Union

from typing_extensions import NotRequired

# Import generated SessionEvent types
from .generated.session_events import SessionEvent

# SessionEvent is now imported from generated types
# It provides proper type discrimination for all event types

# Valid reasoning effort levels for models that support it
ReasoningEffort = Literal["low", "medium", "high", "xhigh"]

# Connection state
ConnectionState = Literal["disconnected", "connecting", "connected", "error"]

# Log level type
LogLevel = Literal["none", "error", "warning", "info", "debug", "all"]


# Attachment type
class Attachment(TypedDict):
    type: Literal["file", "directory"]
    path: str
    displayName: NotRequired[str]


# Options for creating a CopilotClient
class CopilotClientOptions(TypedDict, total=False):
    """Options for creating a CopilotClient"""

    cli_path: str  # Path to the Copilot CLI executable (default: "copilot")
    # Working directory for the CLI process (default: current process's cwd)
    cwd: str
    port: int  # Port for the CLI server (TCP mode only, default: 0)
    use_stdio: bool  # Use stdio transport instead of TCP (default: True)
    cli_url: str  # URL of an existing Copilot CLI server to connect to over TCP
    # Format: "host:port" or "http://host:port" or just "port" (defaults to localhost)
    # Examples: "localhost:8080", "http://127.0.0.1:9000", "8080"
    # Mutually exclusive with cli_path, use_stdio
    log_level: LogLevel  # Log level
    auto_start: bool  # Auto-start the CLI server on first use (default: True)
    # Auto-restart the CLI server if it crashes (default: True)
    auto_restart: bool
    env: dict[str, str]  # Environment variables for the CLI process
    # GitHub token to use for authentication.
    # When provided, the token is passed to the CLI server via environment variable.
    # This takes priority over other authentication methods.
    github_token: str
    # Whether to use the logged-in user for authentication.
    # When True, the CLI server will attempt to use stored OAuth tokens or gh CLI auth.
    # When False, only explicit tokens (github_token or environment variables) are used.
    # Default: True (but defaults to False when github_token is provided)
    use_logged_in_user: bool


ToolResultType = Literal["success", "failure", "rejected", "denied"]


class ToolBinaryResult(TypedDict, total=False):
    data: str
    mimeType: str
    type: str
    description: str


class ToolResult(TypedDict, total=False):
    """Result of a tool invocation."""

    textResultForLlm: str
    binaryResultsForLlm: list[ToolBinaryResult]
    resultType: ToolResultType
    error: str
    sessionLog: str
    toolTelemetry: dict[str, Any]


class ToolInvocation(TypedDict):
    session_id: str
    tool_call_id: str
    tool_name: str
    arguments: Any


ToolHandler = Callable[[ToolInvocation], Union[ToolResult, Awaitable[ToolResult]]]


@dataclass
class Tool:
    name: str
    description: str
    handler: ToolHandler
    parameters: dict[str, Any] | None = None


# System message configuration (discriminated union)
# Use SystemMessageAppendConfig for default behavior, SystemMessageReplaceConfig for full control


class SystemMessageAppendConfig(TypedDict, total=False):
    """
    Append mode: Use CLI foundation with optional appended content.
    """

    mode: NotRequired[Literal["append"]]
    content: NotRequired[str]


class SystemMessageReplaceConfig(TypedDict):
    """
    Replace mode: Use caller-provided system message entirely.
    Removes all SDK guardrails including security restrictions.
    """

    mode: Literal["replace"]
    content: str


# Union type - use one or the other
SystemMessageConfig = Union[SystemMessageAppendConfig, SystemMessageReplaceConfig]


# Permission request types
class PermissionRequest(TypedDict, total=False):
    """Permission request from the server"""

    kind: Literal["shell", "write", "mcp", "read", "url"]
    toolCallId: str
    # Additional fields vary by kind


class PermissionRequestResult(TypedDict, total=False):
    """Result of a permission request"""

    kind: Literal[
        "approved",
        "denied-by-rules",
        "denied-no-approval-rule-and-could-not-request-from-user",
        "denied-interactively-by-user",
    ]
    rules: list[Any]


PermissionHandler = Callable[
    [PermissionRequest, dict[str, str]],
    Union[PermissionRequestResult, Awaitable[PermissionRequestResult]],
]


# ============================================================================
# User Input Request Types
# ============================================================================


class UserInputRequest(TypedDict, total=False):
    """Request for user input from the agent (enables ask_user tool)"""

    question: str
    choices: list[str]
    allowFreeform: bool


class UserInputResponse(TypedDict):
    """Response to a user input request"""

    answer: str
    wasFreeform: bool


UserInputHandler = Callable[
    [UserInputRequest, dict[str, str]],
    Union[UserInputResponse, Awaitable[UserInputResponse]],
]


# ============================================================================
# Hook Types
# ============================================================================


class BaseHookInput(TypedDict):
    """Base interface for all hook inputs"""

    timestamp: int
    cwd: str


class PreToolUseHookInput(TypedDict):
    """Input for pre-tool-use hook"""

    timestamp: int
    cwd: str
    toolName: str
    toolArgs: Any


class PreToolUseHookOutput(TypedDict, total=False):
    """Output for pre-tool-use hook"""

    permissionDecision: Literal["allow", "deny", "ask"]
    permissionDecisionReason: str
    modifiedArgs: Any
    additionalContext: str
    suppressOutput: bool


PreToolUseHandler = Callable[
    [PreToolUseHookInput, dict[str, str]],
    Union[PreToolUseHookOutput, None, Awaitable[Union[PreToolUseHookOutput, None]]],
]


class PostToolUseHookInput(TypedDict):
    """Input for post-tool-use hook"""

    timestamp: int
    cwd: str
    toolName: str
    toolArgs: Any
    toolResult: Any


class PostToolUseHookOutput(TypedDict, total=False):
    """Output for post-tool-use hook"""

    modifiedResult: Any
    additionalContext: str
    suppressOutput: bool


PostToolUseHandler = Callable[
    [PostToolUseHookInput, dict[str, str]],
    Union[PostToolUseHookOutput, None, Awaitable[Union[PostToolUseHookOutput, None]]],
]


class UserPromptSubmittedHookInput(TypedDict):
    """Input for user-prompt-submitted hook"""

    timestamp: int
    cwd: str
    prompt: str


class UserPromptSubmittedHookOutput(TypedDict, total=False):
    """Output for user-prompt-submitted hook"""

    modifiedPrompt: str
    additionalContext: str
    suppressOutput: bool


UserPromptSubmittedHandler = Callable[
    [UserPromptSubmittedHookInput, dict[str, str]],
    Union[
        UserPromptSubmittedHookOutput,
        None,
        Awaitable[Union[UserPromptSubmittedHookOutput, None]],
    ],
]


class SessionStartHookInput(TypedDict):
    """Input for session-start hook"""

    timestamp: int
    cwd: str
    source: Literal["startup", "resume", "new"]
    initialPrompt: NotRequired[str]


class SessionStartHookOutput(TypedDict, total=False):
    """Output for session-start hook"""

    additionalContext: str
    modifiedConfig: dict[str, Any]


SessionStartHandler = Callable[
    [SessionStartHookInput, dict[str, str]],
    Union[SessionStartHookOutput, None, Awaitable[Union[SessionStartHookOutput, None]]],
]


class SessionEndHookInput(TypedDict):
    """Input for session-end hook"""

    timestamp: int
    cwd: str
    reason: Literal["complete", "error", "abort", "timeout", "user_exit"]
    finalMessage: NotRequired[str]
    error: NotRequired[str]


class SessionEndHookOutput(TypedDict, total=False):
    """Output for session-end hook"""

    suppressOutput: bool
    cleanupActions: list[str]
    sessionSummary: str


SessionEndHandler = Callable[
    [SessionEndHookInput, dict[str, str]],
    Union[SessionEndHookOutput, None, Awaitable[Union[SessionEndHookOutput, None]]],
]


class ErrorOccurredHookInput(TypedDict):
    """Input for error-occurred hook"""

    timestamp: int
    cwd: str
    error: str
    errorContext: Literal["model_call", "tool_execution", "system", "user_input"]
    recoverable: bool


class ErrorOccurredHookOutput(TypedDict, total=False):
    """Output for error-occurred hook"""

    suppressOutput: bool
    errorHandling: Literal["retry", "skip", "abort"]
    retryCount: int
    userNotification: str


ErrorOccurredHandler = Callable[
    [ErrorOccurredHookInput, dict[str, str]],
    Union[ErrorOccurredHookOutput, None, Awaitable[Union[ErrorOccurredHookOutput, None]]],
]


class SessionHooks(TypedDict, total=False):
    """Configuration for session hooks"""

    on_pre_tool_use: PreToolUseHandler
    on_post_tool_use: PostToolUseHandler
    on_user_prompt_submitted: UserPromptSubmittedHandler
    on_session_start: SessionStartHandler
    on_session_end: SessionEndHandler
    on_error_occurred: ErrorOccurredHandler


# ============================================================================
# MCP Server Configuration Types
# ============================================================================


class MCPLocalServerConfig(TypedDict, total=False):
    """Configuration for a local/stdio MCP server."""

    tools: list[str]  # List of tools to include. [] means none. "*" means all.
    type: NotRequired[Literal["local", "stdio"]]  # Server type
    timeout: NotRequired[int]  # Timeout in milliseconds
    command: str  # Command to run
    args: list[str]  # Command arguments
    env: NotRequired[dict[str, str]]  # Environment variables
    cwd: NotRequired[str]  # Working directory


class MCPRemoteServerConfig(TypedDict, total=False):
    """Configuration for a remote MCP server (HTTP or SSE)."""

    tools: list[str]  # List of tools to include. [] means none. "*" means all.
    type: Literal["http", "sse"]  # Server type
    timeout: NotRequired[int]  # Timeout in milliseconds
    url: str  # URL of the remote server
    headers: NotRequired[dict[str, str]]  # HTTP headers


MCPServerConfig = Union[MCPLocalServerConfig, MCPRemoteServerConfig]


# ============================================================================
# Custom Agent Configuration Types
# ============================================================================


class CustomAgentConfig(TypedDict, total=False):
    """Configuration for a custom agent."""

    name: str  # Unique name of the custom agent
    display_name: NotRequired[str]  # Display name for UI purposes
    description: NotRequired[str]  # Description of what the agent does
    # List of tool names the agent can use
    tools: NotRequired[list[str] | None]
    prompt: str  # The prompt content for the agent
    # MCP servers specific to agent
    mcp_servers: NotRequired[dict[str, MCPServerConfig]]
    infer: NotRequired[bool]  # Whether agent is available for model inference


class InfiniteSessionConfig(TypedDict, total=False):
    """
    Configuration for infinite sessions with automatic context compaction
    and workspace persistence.

    When enabled, sessions automatically manage context window limits through
    background compaction and persist state to a workspace directory.
    """

    # Whether infinite sessions are enabled (default: True)
    enabled: bool
    # Context utilization threshold (0.0-1.0) at which background compaction starts.
    # Compaction runs asynchronously, allowing the session to continue processing.
    # Default: 0.80
    background_compaction_threshold: float
    # Context utilization threshold (0.0-1.0) at which the session blocks until
    # compaction completes. This prevents context overflow when compaction hasn't
    # finished in time. Default: 0.95
    buffer_exhaustion_threshold: float


# Configuration for creating a session
class SessionConfig(TypedDict, total=False):
    """Configuration for creating a session"""

    session_id: str  # Optional custom session ID
    model: str  # Model to use for this session. Use client.list_models() to see available models.
    # Reasoning effort level for models that support it.
    # Only valid for models where capabilities.supports.reasoning_effort is True.
    reasoning_effort: ReasoningEffort
    tools: list[Tool]
    system_message: SystemMessageConfig  # System message configuration
    # List of tool names to allow (takes precedence over excluded_tools)
    available_tools: list[str]
    # List of tool names to disable (ignored if available_tools is set)
    excluded_tools: list[str]
    # Handler for permission requests from the server
    on_permission_request: PermissionHandler
    # Handler for user input requests from the agent (enables ask_user tool)
    on_user_input_request: UserInputHandler
    # Hook handlers for intercepting session lifecycle events
    hooks: SessionHooks
    # Working directory for the session. Tool operations will be relative to this directory.
    working_directory: str
    # Custom provider configuration (BYOK - Bring Your Own Key)
    provider: ProviderConfig
    # Enable streaming of assistant message and reasoning chunks
    # When True, assistant.message_delta and assistant.reasoning_delta events
    # with delta_content are sent as the response is generated
    streaming: bool
    # MCP server configurations for the session
    mcp_servers: dict[str, MCPServerConfig]
    # Custom agent configurations for the session
    custom_agents: list[CustomAgentConfig]
    # Override the default configuration directory location.
    # When specified, the session will use this directory for storing config and state.
    config_dir: str
    # Directories to load skills from
    skill_directories: list[str]
    # List of skill names to disable
    disabled_skills: list[str]
    # Infinite session configuration for persistent workspaces and automatic compaction.
    # When enabled (default), sessions automatically manage context limits and persist state.
    # Set to {"enabled": False} to disable.
    infinite_sessions: InfiniteSessionConfig


# Azure-specific provider options
class AzureProviderOptions(TypedDict, total=False):
    """Azure-specific provider configuration"""

    api_version: str  # Azure API version. Defaults to "2024-10-21".


# Configuration for a custom API provider
class ProviderConfig(TypedDict, total=False):
    """Configuration for a custom API provider"""

    type: Literal["openai", "azure", "anthropic"]
    wire_api: Literal["completions", "responses"]
    base_url: str
    api_key: str
    # Bearer token for authentication. Sets the Authorization header directly.
    # Use this for services requiring bearer token auth instead of API key.
    # Takes precedence over api_key when both are set.
    bearer_token: str
    azure: AzureProviderOptions  # Azure-specific options


# Configuration for resuming a session
class ResumeSessionConfig(TypedDict, total=False):
    """Configuration for resuming a session"""

    tools: list[Tool]
    provider: ProviderConfig
    # Reasoning effort level for models that support it.
    reasoning_effort: ReasoningEffort
    on_permission_request: PermissionHandler
    # Handler for user input requests from the agent (enables ask_user tool)
    on_user_input_request: UserInputHandler
    # Hook handlers for intercepting session lifecycle events
    hooks: SessionHooks
    # Working directory for the session. Tool operations will be relative to this directory.
    working_directory: str
    # Enable streaming of assistant message chunks
    streaming: bool
    # MCP server configurations for the session
    mcp_servers: dict[str, MCPServerConfig]
    # Custom agent configurations for the session
    custom_agents: list[CustomAgentConfig]
    # Directories to load skills from
    skill_directories: list[str]
    # List of skill names to disable
    disabled_skills: list[str]
    # When True, skips emitting the session.resume event.
    # Useful for reconnecting to a session without triggering resume-related side effects.
    disable_resume: bool


# Options for sending a message to a session
class MessageOptions(TypedDict):
    """Options for sending a message to a session"""

    prompt: str  # The prompt/message to send
    # Optional file/directory attachments
    attachments: NotRequired[list[Attachment]]
    # Message processing mode
    mode: NotRequired[Literal["enqueue", "immediate"]]


# Event handler type
SessionEventHandler = Callable[[SessionEvent], None]


# Response from ping
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


# Error information from client stop
@dataclass
class StopError:
    """Error information from client stop"""

    message: str  # Error message describing what failed during cleanup

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


# Response from status.get
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


# Response from auth.getStatus
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


# Model capabilities
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

    vision: bool
    reasoning_effort: bool = False  # Whether this model supports reasoning effort

    @staticmethod
    def from_dict(obj: Any) -> ModelSupports:
        assert isinstance(obj, dict)
        vision = obj.get("vision")
        if vision is None:
            raise ValueError("Missing required field 'vision' in ModelSupports")
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
        if supports_dict is None or limits_dict is None:
            raise ValueError(
                f"Missing required fields in ModelCapabilities: supports={supports_dict}, "
                f"limits={limits_dict}"
            )
        supports = ModelSupports.from_dict(supports_dict)
        limits = ModelLimits.from_dict(limits_dict)
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


@dataclass
class SessionMetadata:
    """Metadata about a session"""

    sessionId: str  # Session identifier
    startTime: str  # ISO 8601 timestamp when session was created
    modifiedTime: str  # ISO 8601 timestamp when session was last modified
    isRemote: bool  # Whether the session is remote
    summary: str | None = None  # Optional summary of the session

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
        return SessionMetadata(
            sessionId=str(sessionId),
            startTime=str(startTime),
            modifiedTime=str(modifiedTime),
            isRemote=bool(isRemote),
            summary=summary,
        )

    def to_dict(self) -> dict:
        result: dict = {}
        result["sessionId"] = self.sessionId
        result["startTime"] = self.startTime
        result["modifiedTime"] = self.modifiedTime
        result["isRemote"] = self.isRemote
        if self.summary is not None:
            result["summary"] = self.summary
        return result
