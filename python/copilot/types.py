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
    model: Literal["gpt-5", "claude-sonnet-4", "claude-sonnet-4.5", "claude-haiku-4.5"]
    tools: list[Tool]
    system_message: SystemMessageConfig  # System message configuration
    # List of tool names to allow (takes precedence over excluded_tools)
    available_tools: list[str]
    # List of tool names to disable (ignored if available_tools is set)
    excluded_tools: list[str]
    # Handler for permission requests from the server
    on_permission_request: PermissionHandler
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
    on_permission_request: PermissionHandler
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


# Response from status.get
class GetStatusResponse(TypedDict):
    """Response from status.get"""

    version: str  # Package version (e.g., "1.0.0")
    protocolVersion: int  # Protocol version for SDK compatibility


# Response from auth.getStatus
class GetAuthStatusResponse(TypedDict):
    """Response from auth.getStatus"""

    isAuthenticated: bool  # Whether the user is authenticated
    authType: NotRequired[
        Literal["user", "env", "gh-cli", "hmac", "api-key", "token"]
    ]  # Authentication type
    host: NotRequired[str]  # GitHub host URL
    login: NotRequired[str]  # User login name
    statusMessage: NotRequired[str]  # Human-readable status message


# Model capabilities
class ModelVisionLimits(TypedDict, total=False):
    """Vision-specific limits"""

    supported_media_types: list[str]
    max_prompt_images: int
    max_prompt_image_size: int


class ModelLimits(TypedDict, total=False):
    """Model limits"""

    max_prompt_tokens: int
    max_context_window_tokens: int
    vision: ModelVisionLimits


class ModelSupports(TypedDict):
    """Model support flags"""

    vision: bool


class ModelCapabilities(TypedDict):
    """Model capabilities and limits"""

    supports: ModelSupports
    limits: ModelLimits


class ModelPolicy(TypedDict):
    """Model policy state"""

    state: Literal["enabled", "disabled", "unconfigured"]
    terms: str


class ModelBilling(TypedDict):
    """Model billing information"""

    multiplier: float


class ModelInfo(TypedDict):
    """Information about an available model"""

    id: str  # Model identifier (e.g., "claude-sonnet-4.5")
    name: str  # Display name
    capabilities: ModelCapabilities  # Model capabilities and limits
    policy: NotRequired[ModelPolicy]  # Policy state
    billing: NotRequired[ModelBilling]  # Billing information


class GetModelsResponse(TypedDict):
    """Response from models.list"""

    models: list[ModelInfo]


class SessionMetadata(TypedDict):
    """Metadata about a session"""

    sessionId: str  # Session identifier
    startTime: str  # ISO 8601 timestamp when session was created
    modifiedTime: str  # ISO 8601 timestamp when session was last modified
    summary: NotRequired[str]  # Optional summary of the session
    isRemote: bool  # Whether the session is remote
