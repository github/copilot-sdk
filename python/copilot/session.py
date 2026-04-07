"""
Copilot Session - represents a single conversation session with the Copilot CLI.

This module provides the CopilotSession class for managing individual
conversation sessions with the Copilot CLI, along with all session-related
configuration and handler types.
"""

from __future__ import annotations

import asyncio
import functools
import inspect
import os
import pathlib
import threading
from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from types import TracebackType
from typing import TYPE_CHECKING, Any, Literal, NotRequired, Required, TypedDict, cast

from ._jsonrpc import JsonRpcError, ProcessExitedError
from ._telemetry import get_trace_context, trace_context
from .generated.rpc import (
    Action,
    Kind,
    Level,
    Property,
    PropertyType,
    RequestedSchema,
    RequestedSchemaType,
    ResultResult,
    SessionCommandsHandlePendingCommandParams,
    SessionLogParams,
    SessionModelSwitchToParams,
    SessionPermissionsHandlePendingPermissionRequestParams,
    SessionPermissionsHandlePendingPermissionRequestParamsResult,
    SessionRpc,
    SessionToolsHandlePendingToolCallParams,
    SessionUIElicitationParams,
    SessionUIHandlePendingElicitationParams,
    SessionUIHandlePendingElicitationParamsResult,
)
from .generated.rpc import (
    ModelCapabilitiesOverride as _RpcModelCapabilitiesOverride,
)
from .generated.session_events import (
    PermissionRequest,
    SessionEvent,
    SessionEventType,
    session_event_from_dict,
)
from .tools import Tool, ToolHandler, ToolInvocation, ToolResult

if TYPE_CHECKING:
    from .client import ModelCapabilitiesOverride

# Re-export SessionEvent under an alias used internally
SessionEventTypeAlias = SessionEvent

# ============================================================================
# Reasoning Effort
# ============================================================================

ReasoningEffort = Literal["low", "medium", "high", "xhigh"]

# ============================================================================
# Attachment Types
# ============================================================================


class SelectionRange(TypedDict):
    line: int
    character: int


class Selection(TypedDict):
    start: SelectionRange
    end: SelectionRange


class FileAttachment(TypedDict):
    """File attachment."""

    type: Literal["file"]
    path: str
    displayName: NotRequired[str]


class DirectoryAttachment(TypedDict):
    """Directory attachment."""

    type: Literal["directory"]
    path: str
    displayName: NotRequired[str]


class SelectionAttachment(TypedDict):
    """Selection attachment with text from a file."""

    type: Literal["selection"]
    filePath: str
    displayName: str
    selection: NotRequired[Selection]
    text: NotRequired[str]


class BlobAttachment(TypedDict):
    """Inline base64-encoded content attachment (e.g. images)."""

    type: Literal["blob"]
    data: str
    """Base64-encoded content"""
    mimeType: str
    """MIME type of the inline data"""
    displayName: NotRequired[str]


Attachment = FileAttachment | DirectoryAttachment | SelectionAttachment | BlobAttachment

# ============================================================================
# System Message Configuration
# ============================================================================


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


# Known system prompt section identifiers for the "customize" mode.

SectionTransformFn = Callable[[str], str | Awaitable[str]]
"""Transform callback: receives current section content, returns new content."""

SectionOverrideAction = Literal["replace", "remove", "append", "prepend"] | SectionTransformFn
"""Override action: a string literal for static overrides, or a callback for transforms."""

SystemPromptSection = Literal[
    "identity",
    "tone",
    "tool_efficiency",
    "environment_context",
    "code_change_rules",
    "guidelines",
    "safety",
    "tool_instructions",
    "custom_instructions",
    "last_instructions",
]

SYSTEM_PROMPT_SECTIONS: dict[SystemPromptSection, str] = {
    "identity": "Agent identity preamble and mode statement",
    "tone": "Response style, conciseness rules, output formatting preferences",
    "tool_efficiency": "Tool usage patterns, parallel calling, batching guidelines",
    "environment_context": "CWD, OS, git root, directory listing, available tools",
    "code_change_rules": "Coding rules, linting/testing, ecosystem tools, style",
    "guidelines": "Tips, behavioral best practices, behavioral guidelines",
    "safety": "Environment limitations, prohibited actions, security policies",
    "tool_instructions": "Per-tool usage instructions",
    "custom_instructions": "Repository and organization custom instructions",
    "last_instructions": (
        "End-of-prompt instructions: parallel tool calling, persistence, task completion"
    ),
}


class SectionOverride(TypedDict, total=False):
    """Override operation for a single system prompt section."""

    action: Required[SectionOverrideAction]
    content: NotRequired[str]


class SystemMessageCustomizeConfig(TypedDict, total=False):
    """
    Customize mode: Override individual sections of the system prompt.
    Keeps the SDK-managed prompt structure while allowing targeted modifications.
    """

    mode: Required[Literal["customize"]]
    sections: NotRequired[dict[SystemPromptSection, SectionOverride]]
    content: NotRequired[str]


SystemMessageConfig = (
    SystemMessageAppendConfig | SystemMessageReplaceConfig | SystemMessageCustomizeConfig
)

# ============================================================================
# Permission Types
# ============================================================================

PermissionRequestResultKind = Literal[
    "approved",
    "denied-by-rules",
    "denied-by-content-exclusion-policy",
    "denied-no-approval-rule-and-could-not-request-from-user",
    "denied-interactively-by-user",
    "no-result",
]


@dataclass
class PermissionRequestResult:
    """Result of a permission request."""

    kind: PermissionRequestResultKind = "denied-no-approval-rule-and-could-not-request-from-user"
    rules: list[Any] | None = None
    feedback: str | None = None
    message: str | None = None
    path: str | None = None


_PermissionHandlerFn = Callable[
    [PermissionRequest, dict[str, str]],
    PermissionRequestResult | Awaitable[PermissionRequestResult],
]


class PermissionHandler:
    @staticmethod
    def approve_all(
        request: PermissionRequest, invocation: dict[str, str]
    ) -> PermissionRequestResult:
        return PermissionRequestResult(kind="approved")


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
    UserInputResponse | Awaitable[UserInputResponse],
]

# ============================================================================
# Command Types
# ============================================================================


@dataclass
class CommandContext:
    """Context passed to a command handler when a command is executed."""

    session_id: str
    """Session ID where the command was invoked."""
    command: str
    """The full command text (e.g. ``"/deploy production"``)."""
    command_name: str
    """Command name without leading ``/``."""
    args: str
    """Raw argument string after the command name."""


CommandHandler = Callable[[CommandContext], Awaitable[None] | None]
"""Handler invoked when a registered command is executed by a user."""


@dataclass
class CommandDefinition:
    """Definition of a slash command registered with the session.

    When the CLI is running with a TUI, registered commands appear as
    ``/commandName`` for the user to invoke.
    """

    name: str
    """Command name (without leading ``/``)."""
    handler: CommandHandler
    """Handler invoked when the command is executed."""
    description: str | None = None
    """Human-readable description shown in command completion UI."""


# ============================================================================
# Session Capabilities
# ============================================================================


class SessionUiCapabilities(TypedDict, total=False):
    """UI capabilities reported by the CLI host."""

    elicitation: bool
    """Whether the host supports interactive elicitation dialogs."""


class SessionCapabilities(TypedDict, total=False):
    """Capabilities reported by the CLI host for this session."""

    ui: SessionUiCapabilities


# ============================================================================
# Elicitation Types (client → server)
# ============================================================================

ElicitationFieldValue = str | float | bool | list[str]
"""Possible value types in elicitation form content."""


class ElicitationResult(TypedDict, total=False):
    """Result returned from an elicitation request."""

    action: Required[Literal["accept", "decline", "cancel"]]
    """User action: ``"accept"`` (submitted), ``"decline"`` (rejected),
    or ``"cancel"`` (dismissed)."""
    content: dict[str, ElicitationFieldValue]
    """Form values submitted by the user (present when action is ``"accept"``)."""


class ElicitationParams(TypedDict):
    """Parameters for a raw elicitation request."""

    message: str
    """Message describing what information is needed from the user."""
    requestedSchema: dict[str, Any]
    """JSON Schema describing the form fields to present."""


class InputOptions(TypedDict, total=False):
    """Options for the ``input()`` convenience method."""

    title: str
    """Title label for the input field."""
    description: str
    """Descriptive text shown below the field."""
    minLength: int
    """Minimum text length."""
    maxLength: int
    """Maximum text length."""
    format: str
    """Input format hint (e.g. ``"email"``, ``"uri"``, ``"date"``)."""
    default: str
    """Default value for the input field."""


# ============================================================================
# Elicitation Types (server → client callback)
# ============================================================================


class ElicitationContext(TypedDict, total=False):
    """Context for an elicitation handler invocation, combining the request data
    with session context. Mirrors the single-argument pattern of CommandContext."""

    session_id: Required[str]
    """Identifier of the session that triggered the elicitation request."""
    message: Required[str]
    """Message describing what information is needed from the user."""
    requestedSchema: dict[str, Any]
    """JSON Schema describing the form fields to present."""
    mode: Literal["form", "url"]
    """Elicitation mode: ``"form"`` for structured input, ``"url"`` for browser redirect."""
    elicitationSource: str
    """The source that initiated the request (e.g. MCP server name)."""
    url: str
    """URL to open in the browser (when mode is ``"url"``)."""


ElicitationHandler = Callable[
    [ElicitationContext],
    ElicitationResult | Awaitable[ElicitationResult],
]
"""Handler invoked when the server dispatches an elicitation request to this client."""


# ============================================================================
# Session UI API
# ============================================================================


class SessionUiApi:
    """Interactive UI methods for showing dialogs to the user.

    Only available when the CLI host supports elicitation
    (``session.capabilities["ui"]["elicitation"] is True``).

    Obtained via :attr:`CopilotSession.ui`.
    """

    def __init__(self, session: CopilotSession) -> None:
        self._session = session

    async def elicitation(self, params: ElicitationParams) -> ElicitationResult:
        """Shows a generic elicitation dialog with a custom schema.

        Args:
            params: Elicitation parameters including message and requestedSchema.

        Returns:
            The user's response (action + optional content).

        Raises:
            RuntimeError: If the host does not support elicitation.
        """
        self._session._assert_elicitation()
        rpc_result = await self._session.rpc.ui.elicitation(
            SessionUIElicitationParams(
                message=params["message"],
                requested_schema=RequestedSchema.from_dict(params["requestedSchema"]),
            )
        )
        result: ElicitationResult = {"action": rpc_result.action.value}  # type: ignore[typeddict-item]
        if rpc_result.content is not None:
            result["content"] = rpc_result.content
        return result

    async def confirm(self, message: str) -> bool:
        """Shows a confirmation dialog and returns the user's boolean answer.

        Args:
            message: The question to ask the user.

        Returns:
            ``True`` if the user accepted, ``False`` otherwise.

        Raises:
            RuntimeError: If the host does not support elicitation.
        """
        self._session._assert_elicitation()
        rpc_result = await self._session.rpc.ui.elicitation(
            SessionUIElicitationParams(
                message=message,
                requested_schema=RequestedSchema(
                    type=RequestedSchemaType.OBJECT,
                    properties={
                        "confirmed": Property(type=PropertyType.BOOLEAN, default=True),
                    },
                    required=["confirmed"],
                ),
            )
        )
        return (
            rpc_result.action == Action.ACCEPT
            and rpc_result.content is not None
            and rpc_result.content.get("confirmed") is True
        )

    async def select(self, message: str, options: list[str]) -> str | None:
        """Shows a selection dialog with a list of options.

        Args:
            message: Instruction to show the user.
            options: List of choices the user can pick from.

        Returns:
            The selected string, or ``None`` if the user declined/cancelled.

        Raises:
            RuntimeError: If the host does not support elicitation.
        """
        self._session._assert_elicitation()
        rpc_result = await self._session.rpc.ui.elicitation(
            SessionUIElicitationParams(
                message=message,
                requested_schema=RequestedSchema(
                    type=RequestedSchemaType.OBJECT,
                    properties={
                        "selection": Property(type=PropertyType.STRING, enum=options),
                    },
                    required=["selection"],
                ),
            )
        )
        if (
            rpc_result.action == Action.ACCEPT
            and rpc_result.content is not None
            and rpc_result.content.get("selection") is not None
        ):
            return str(rpc_result.content["selection"])
        return None

    async def input(self, message: str, options: InputOptions | None = None) -> str | None:
        """Shows a text input dialog.

        Args:
            message: Instruction to show the user.
            options: Optional constraints for the input field.

        Returns:
            The entered text, or ``None`` if the user declined/cancelled.

        Raises:
            RuntimeError: If the host does not support elicitation.
        """
        self._session._assert_elicitation()
        field: dict[str, Any] = {"type": "string"}
        if options:
            for key in ("title", "description", "minLength", "maxLength", "format", "default"):
                if key in options:
                    field[key] = options[key]

        rpc_result = await self._session.rpc.ui.elicitation(
            SessionUIElicitationParams(
                message=message,
                requested_schema=RequestedSchema.from_dict(
                    {
                        "type": "object",
                        "properties": {"value": field},
                        "required": ["value"],
                    }
                ),
            )
        )
        if (
            rpc_result.action == Action.ACCEPT
            and rpc_result.content is not None
            and rpc_result.content.get("value") is not None
        ):
            return str(rpc_result.content["value"])
        return None


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
    PreToolUseHookOutput | None | Awaitable[PreToolUseHookOutput | None],
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
    PostToolUseHookOutput | None | Awaitable[PostToolUseHookOutput | None],
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
    UserPromptSubmittedHookOutput | None | Awaitable[UserPromptSubmittedHookOutput | None],
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
    SessionStartHookOutput | None | Awaitable[SessionStartHookOutput | None],
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
    SessionEndHookOutput | None | Awaitable[SessionEndHookOutput | None],
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
    ErrorOccurredHookOutput | None | Awaitable[ErrorOccurredHookOutput | None],
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


MCPServerConfig = MCPLocalServerConfig | MCPRemoteServerConfig

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


# ============================================================================
# Session Configuration
# ============================================================================


class AzureProviderOptions(TypedDict, total=False):
    """Azure-specific provider configuration"""

    api_version: str  # Azure API version. Defaults to "2024-10-21".


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


class SessionConfig(TypedDict, total=False):
    """Configuration for creating a session"""

    session_id: str  # Optional custom session ID
    # Client name to identify the application using the SDK.
    # Included in the User-Agent header for API requests.
    client_name: str
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
    on_permission_request: _PermissionHandlerFn
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
    # Name of the custom agent to activate when the session starts.
    # Must match the name of one of the agents in custom_agents.
    agent: str
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
    # Optional event handler that is registered on the session before the
    # session.create RPC is issued, ensuring early events (e.g. session.start)
    # are delivered. Equivalent to calling session.on(handler) immediately
    # after creation, but executes earlier in the lifecycle so no events are missed.
    on_event: Callable[[SessionEvent], None]
    # Slash commands to register with the session.
    # When the CLI has a TUI, each command appears as /name for the user to invoke.
    commands: list[CommandDefinition]
    # Handler for elicitation requests from the server.
    # When provided, the server calls back to this client for form-based UI dialogs.
    on_elicitation_request: ElicitationHandler


class ResumeSessionConfig(TypedDict, total=False):
    """Configuration for resuming a session"""

    # Client name to identify the application using the SDK.
    # Included in the User-Agent header for API requests.
    client_name: str
    # Model to use for this session. Can change the model when resuming.
    model: str
    tools: list[Tool]
    system_message: SystemMessageConfig  # System message configuration
    # List of tool names to allow (takes precedence over excluded_tools)
    available_tools: list[str]
    # List of tool names to disable (ignored if available_tools is set)
    excluded_tools: list[str]
    provider: ProviderConfig
    # Reasoning effort level for models that support it.
    reasoning_effort: ReasoningEffort
    on_permission_request: _PermissionHandlerFn
    # Handler for user input requestsfrom the agent (enables ask_user tool)
    on_user_input_request: UserInputHandler
    # Hook handlers for intercepting session lifecycle events
    hooks: SessionHooks
    # Working directory for the session. Tool operations will be relative to this directory.
    working_directory: str
    # Override the default configuration directory location.
    config_dir: str
    # Enable streaming of assistant message chunks
    streaming: bool
    # MCP server configurations for the session
    mcp_servers: dict[str, MCPServerConfig]
    # Custom agent configurations for the session
    custom_agents: list[CustomAgentConfig]
    # Name of the custom agent to activate when the session starts.
    # Must match the name of one of the agents in custom_agents.
    agent: str
    # Directories to load skills from
    skill_directories: list[str]
    # List of skill names to disable
    disabled_skills: list[str]
    # Infinite session configuration for persistent workspaces and automatic compaction.
    infinite_sessions: InfiniteSessionConfig
    # When True, skips emitting the session.resume event.
    # Useful for reconnecting to a session without triggering resume-related side effects.
    disable_resume: bool
    # Optional event handler registered before the session.resume RPC is issued,
    # ensuring early events are delivered. See SessionConfig.on_event.
    on_event: Callable[[SessionEvent], None]
    # Slash commands to register with the session.
    commands: list[CommandDefinition]
    # Handler for elicitation requests from the server.
    on_elicitation_request: ElicitationHandler


SessionEventHandler = Callable[[SessionEvent], None]


class CopilotSession:
    """
    Represents a single conversation session with the Copilot CLI.

    A session maintains conversation state, handles events, and manages tool execution.
    Sessions are created via :meth:`CopilotClient.create_session` or resumed via
    :meth:`CopilotClient.resume_session`.

    The session provides methods to send messages, subscribe to events, retrieve
    conversation history, and manage the session lifecycle.

    Attributes:
        session_id: The unique identifier for this session.

    Example:
        >>> async with await client.create_session(
        ...     on_permission_request=PermissionHandler.approve_all,
        ... ) as session:
        ...     # Subscribe to events
        ...     unsubscribe = session.on(lambda event: print(event.type))
        ...
        ...     # Send a message
        ...     await session.send("Hello, world!")
        ...
        ...     # Clean up
        ...     unsubscribe()
    """

    def __init__(
        self, session_id: str, client: Any, workspace_path: os.PathLike[str] | str | None = None
    ):
        """
        Initialize a new CopilotSession.

        Note:
            This constructor is internal. Use :meth:`CopilotClient.create_session`
            to create sessions.

        Args:
            session_id: The unique identifier for this session.
            client: The internal client connection to the Copilot CLI.
            workspace_path: Path to the session workspace directory
                (when infinite sessions enabled).
        """
        self.session_id = session_id
        self._client = client
        self._workspace_path = os.fsdecode(workspace_path) if workspace_path is not None else None
        self._event_handlers: set[Callable[[SessionEvent], None]] = set()
        self._event_handlers_lock = threading.Lock()
        self._tool_handlers: dict[str, ToolHandler] = {}
        self._tool_handlers_lock = threading.Lock()
        self._permission_handler: _PermissionHandlerFn | None = None
        self._permission_handler_lock = threading.Lock()
        self._user_input_handler: UserInputHandler | None = None
        self._user_input_handler_lock = threading.Lock()
        self._hooks: SessionHooks | None = None
        self._hooks_lock = threading.Lock()
        self._transform_callbacks: dict[str, SectionTransformFn] | None = None
        self._transform_callbacks_lock = threading.Lock()
        self._command_handlers: dict[str, CommandHandler] = {}
        self._command_handlers_lock = threading.Lock()
        self._elicitation_handler: ElicitationHandler | None = None
        self._elicitation_handler_lock = threading.Lock()
        self._capabilities: SessionCapabilities = {}
        self._rpc: SessionRpc | None = None
        self._destroyed = False

    @property
    def rpc(self) -> SessionRpc:
        """Typed session-scoped RPC methods."""
        if self._rpc is None:
            self._rpc = SessionRpc(self._client, self.session_id)
        return self._rpc

    @property
    def capabilities(self) -> SessionCapabilities:
        """Host capabilities reported when the session was created or resumed.

        Use this to check feature support before calling capability-gated APIs.
        """
        return self._capabilities

    @property
    def ui(self) -> SessionUiApi:
        """Interactive UI methods for showing dialogs to the user.

        Only available when the CLI host supports elicitation
        (``session.capabilities.get("ui", {}).get("elicitation") is True``).

        Example:
            >>> ui_caps = session.capabilities.get("ui", {})
            >>> if ui_caps.get("elicitation"):
            ...     ok = await session.ui.confirm("Deploy to production?")
        """
        return SessionUiApi(self)

    @functools.cached_property
    def workspace_path(self) -> pathlib.Path | None:
        """
        Path to the session workspace directory when infinite sessions are enabled.

        Contains checkpoints/, plan.md, and files/ subdirectories.
        None if infinite sessions are disabled.
        """
        # Done as a property as self._workspace_path is directly set from a server
        # response post-init. So it was either make sure all places directly setting
        # the attribute handle the None case appropriately, use a setter for the
        # attribute to do the conversion, or just do the conversion lazily via a getter.
        return pathlib.Path(self._workspace_path) if self._workspace_path else None

    async def send(
        self,
        prompt: str,
        *,
        attachments: list[Attachment] | None = None,
        mode: Literal["enqueue", "immediate"] | None = None,
    ) -> str:
        """
        Send a message to this session.

        The message is processed asynchronously. Subscribe to events via :meth:`on`
        to receive streaming responses and other session events. Use
        :meth:`send_and_wait` to block until the assistant finishes processing.

        Args:
            prompt: The message text to send.
            attachments: Optional file, directory, or selection attachments.
            mode: Message delivery mode (``"enqueue"`` or ``"immediate"``).

        Returns:
            The message ID assigned by the server, which can be used to correlate events.

        Raises:
            Exception: If the session has been disconnected or the connection fails.

        Example:
            >>> message_id = await session.send(
            ...     "Explain this code",
            ...     attachments=[{"type": "file", "path": "./src/main.py"}],
            ... )
        """
        params: dict[str, Any] = {
            "sessionId": self.session_id,
            "prompt": prompt,
        }
        if attachments is not None:
            params["attachments"] = attachments
        if mode is not None:
            params["mode"] = mode
        params.update(get_trace_context())

        response = await self._client.request("session.send", params)
        return response["messageId"]

    async def send_and_wait(
        self,
        prompt: str,
        *,
        attachments: list[Attachment] | None = None,
        mode: Literal["enqueue", "immediate"] | None = None,
        timeout: float = 60.0,
    ) -> SessionEvent | None:
        """
        Send a message to this session and wait until the session becomes idle.

        This is a convenience method that combines :meth:`send` with waiting for
        the session.idle event. Use this when you want to block until the assistant
        has finished processing the message.

        Events are still delivered to handlers registered via :meth:`on` while waiting.

        Args:
            prompt: The message text to send.
            attachments: Optional file, directory, or selection attachments.
            mode: Message delivery mode (``"enqueue"`` or ``"immediate"``).
            timeout: Timeout in seconds (default: 60). Controls how long to wait;
                does not abort in-flight agent work.

        Returns:
            The final assistant message event, or None if none was received.

        Raises:
            TimeoutError: If the timeout is reached before session becomes idle.
            Exception: If the session has been disconnected or the connection fails.

        Example:
            >>> response = await session.send_and_wait("What is 2+2?")
            >>> if response:
            ...     print(response.data.content)
        """
        idle_event = asyncio.Event()
        error_event: Exception | None = None
        last_assistant_message: SessionEvent | None = None

        def handler(event: SessionEventTypeAlias) -> None:
            nonlocal last_assistant_message, error_event
            if event.type == SessionEventType.ASSISTANT_MESSAGE:
                last_assistant_message = event
            elif event.type == SessionEventType.SESSION_IDLE:
                idle_event.set()
            elif event.type == SessionEventType.SESSION_ERROR:
                error_event = Exception(
                    f"Session error: {getattr(event.data, 'message', str(event.data))}"
                )
                idle_event.set()

        unsubscribe = self.on(handler)
        try:
            await self.send(prompt, attachments=attachments, mode=mode)
            await asyncio.wait_for(idle_event.wait(), timeout=timeout)
            if error_event:
                raise error_event
            return last_assistant_message
        except TimeoutError:
            raise TimeoutError(f"Timeout after {timeout}s waiting for session.idle")
        finally:
            unsubscribe()

    def on(self, handler: Callable[[SessionEvent], None]) -> Callable[[], None]:
        """
        Subscribe to events from this session.

        Events include assistant messages, tool executions, errors, and session
        state changes. Multiple handlers can be registered and will all receive
        events.

        Args:
            handler: A callback function that receives session events. The function
                takes a single :class:`SessionEvent` argument and returns None.

        Returns:
            A function that, when called, unsubscribes the handler.

        Example:
            >>> def handle_event(event):
            ...     if event.type == "assistant.message":
            ...         print(f"Assistant: {event.data.content}")
            ...     elif event.type == "session.error":
            ...         print(f"Error: {event.data.message}")
            >>> unsubscribe = session.on(handle_event)
            >>> # Later, to stop receiving events:
            >>> unsubscribe()
        """
        with self._event_handlers_lock:
            self._event_handlers.add(handler)

        def unsubscribe():
            with self._event_handlers_lock:
                self._event_handlers.discard(handler)

        return unsubscribe

    def _dispatch_event(self, event: SessionEvent) -> None:
        """
        Dispatch an event to all registered handlers.

        Broadcast request events (external_tool.requested, permission.requested) are handled
        internally before being forwarded to user handlers.

        Note:
            This method is internal and should not be called directly.

        Args:
            event: The session event to dispatch to all handlers.
        """
        # Handle broadcast request events (protocol v3) before dispatching to user handlers.
        # Fire-and-forget: the response is sent asynchronously via RPC.
        self._handle_broadcast_event(event)

        with self._event_handlers_lock:
            handlers = list(self._event_handlers)

        for handler in handlers:
            try:
                handler(event)
            except Exception as e:
                print(f"Error in session event handler: {e}")

    def _handle_broadcast_event(self, event: SessionEvent) -> None:
        """Handle broadcast request events by executing local handlers and responding via RPC.

        Implements the protocol v3 broadcast model where tool calls and permission requests
        are broadcast as session events to all clients.
        """
        if event.type == SessionEventType.EXTERNAL_TOOL_REQUESTED:
            request_id = event.data.request_id
            tool_name = event.data.tool_name
            if not request_id or not tool_name:
                return

            handler = self._get_tool_handler(tool_name)
            if not handler:
                return  # This client doesn't handle this tool; another client will.

            tool_call_id = event.data.tool_call_id or ""
            arguments = event.data.arguments
            tp = getattr(event.data, "traceparent", None)
            ts = getattr(event.data, "tracestate", None)
            asyncio.ensure_future(
                self._execute_tool_and_respond(
                    request_id, tool_name, tool_call_id, arguments, handler, tp, ts
                )
            )

        elif event.type == SessionEventType.PERMISSION_REQUESTED:
            request_id = event.data.request_id
            permission_request = event.data.permission_request
            if not request_id or not permission_request:
                return

            resolved_by_hook = getattr(event.data, "resolved_by_hook", None)
            if resolved_by_hook:
                return  # Already resolved by a permissionRequest hook; no client action needed.

            with self._permission_handler_lock:
                perm_handler = self._permission_handler
            if not perm_handler:
                return  # This client doesn't handle permissions; another client will.

            asyncio.ensure_future(
                self._execute_permission_and_respond(request_id, permission_request, perm_handler)
            )

        elif event.type == SessionEventType.COMMAND_EXECUTE:
            request_id = event.data.request_id
            command_name = event.data.command_name
            command = event.data.command
            args = event.data.args
            if not request_id or not command_name:
                return
            asyncio.ensure_future(
                self._execute_command_and_respond(
                    request_id, command_name, command or "", args or ""
                )
            )

        elif event.type == SessionEventType.ELICITATION_REQUESTED:
            with self._elicitation_handler_lock:
                handler = self._elicitation_handler
            if not handler:
                return
            request_id = event.data.request_id
            if not request_id:
                return
            context: ElicitationContext = {
                "session_id": self.session_id,
                "message": event.data.message or "",
            }
            if event.data.requested_schema is not None:
                context["requestedSchema"] = event.data.requested_schema.to_dict()
            if event.data.mode is not None:
                context["mode"] = event.data.mode.value
            if event.data.elicitation_source is not None:
                context["elicitationSource"] = event.data.elicitation_source
            if event.data.url is not None:
                context["url"] = event.data.url
            asyncio.ensure_future(self._handle_elicitation_request(context, request_id))

        elif event.type == SessionEventType.CAPABILITIES_CHANGED:
            cap: SessionCapabilities = {}
            if event.data.ui is not None:
                ui_cap: SessionUiCapabilities = {}
                if event.data.ui.elicitation is not None:
                    ui_cap["elicitation"] = event.data.ui.elicitation
                cap["ui"] = ui_cap
            self._capabilities = {**self._capabilities, **cap}

    async def _execute_tool_and_respond(
        self,
        request_id: str,
        tool_name: str,
        tool_call_id: str,
        arguments: Any,
        handler: ToolHandler,
        traceparent: str | None = None,
        tracestate: str | None = None,
    ) -> None:
        """Execute a tool handler and send the result back via HandlePendingToolCall RPC."""
        try:
            invocation = ToolInvocation(
                session_id=self.session_id,
                tool_call_id=tool_call_id,
                tool_name=tool_name,
                arguments=arguments,
            )

            with trace_context(traceparent, tracestate):
                result = handler(invocation)
                if inspect.isawaitable(result):
                    result = await result

            tool_result: ToolResult
            if result is None:
                tool_result = ToolResult(
                    text_result_for_llm="Tool returned no result.",
                    result_type="failure",
                    error="tool returned no result",
                    tool_telemetry={},
                )
            else:
                tool_result = result  # type: ignore[assignment]

            # Exception-originated failures (from define_tool's exception handler) are
            # sent via the top-level error param so the CLI formats them with its
            # standard "Failed to execute..." message. Deliberate user-returned
            # failures send the full structured result to preserve metadata.
            if tool_result._from_exception:
                await self.rpc.tools.handle_pending_tool_call(
                    SessionToolsHandlePendingToolCallParams(
                        request_id=request_id,
                        error=tool_result.error,
                    )
                )
            else:
                await self.rpc.tools.handle_pending_tool_call(
                    SessionToolsHandlePendingToolCallParams(
                        request_id=request_id,
                        result=ResultResult(
                            text_result_for_llm=tool_result.text_result_for_llm,
                            result_type=tool_result.result_type,
                            error=tool_result.error,
                            tool_telemetry=tool_result.tool_telemetry,
                        ),
                    )
                )
        except Exception as exc:
            try:
                await self.rpc.tools.handle_pending_tool_call(
                    SessionToolsHandlePendingToolCallParams(
                        request_id=request_id,
                        error=str(exc),
                    )
                )
            except (JsonRpcError, ProcessExitedError, OSError):
                pass  # Connection lost or RPC error — nothing we can do

    async def _execute_permission_and_respond(
        self,
        request_id: str,
        permission_request: Any,
        handler: _PermissionHandlerFn,
    ) -> None:
        """Execute a permission handler and respond via RPC."""
        try:
            result = handler(permission_request, {"session_id": self.session_id})
            if inspect.isawaitable(result):
                result = await result

            result = cast(PermissionRequestResult, result)
            if result.kind == "no-result":
                return

            perm_result = SessionPermissionsHandlePendingPermissionRequestParamsResult(
                kind=Kind(result.kind),
                rules=result.rules,
                feedback=result.feedback,
                message=result.message,
                path=result.path,
            )

            await self.rpc.permissions.handle_pending_permission_request(
                SessionPermissionsHandlePendingPermissionRequestParams(
                    request_id=request_id,
                    result=perm_result,
                )
            )
        except Exception:
            try:
                await self.rpc.permissions.handle_pending_permission_request(
                    SessionPermissionsHandlePendingPermissionRequestParams(
                        request_id=request_id,
                        result=SessionPermissionsHandlePendingPermissionRequestParamsResult(
                            kind=Kind.DENIED_NO_APPROVAL_RULE_AND_COULD_NOT_REQUEST_FROM_USER,
                        ),
                    )
                )
            except (JsonRpcError, ProcessExitedError, OSError):
                pass  # Connection lost or RPC error — nothing we can do

    async def _execute_command_and_respond(
        self,
        request_id: str,
        command_name: str,
        command: str,
        args: str,
    ) -> None:
        """Execute a command handler and send the result back via RPC."""
        with self._command_handlers_lock:
            handler = self._command_handlers.get(command_name)

        if not handler:
            try:
                await self.rpc.commands.handle_pending_command(
                    SessionCommandsHandlePendingCommandParams(
                        request_id=request_id,
                        error=f"Unknown command: {command_name}",
                    )
                )
            except (JsonRpcError, ProcessExitedError, OSError):
                pass  # Connection lost — nothing we can do
            return

        try:
            ctx = CommandContext(
                session_id=self.session_id,
                command=command,
                command_name=command_name,
                args=args,
            )
            result = handler(ctx)
            if inspect.isawaitable(result):
                await result
            await self.rpc.commands.handle_pending_command(
                SessionCommandsHandlePendingCommandParams(request_id=request_id)
            )
        except Exception as exc:
            message = str(exc)
            try:
                await self.rpc.commands.handle_pending_command(
                    SessionCommandsHandlePendingCommandParams(
                        request_id=request_id,
                        error=message,
                    )
                )
            except (JsonRpcError, ProcessExitedError, OSError):
                pass  # Connection lost — nothing we can do

    async def _handle_elicitation_request(
        self,
        context: ElicitationContext,
        request_id: str,
    ) -> None:
        """Handle an elicitation.requested broadcast event.

        Invokes the registered handler and responds via handlePendingElicitation RPC.
        Auto-cancels on error so the server doesn't hang.
        """
        with self._elicitation_handler_lock:
            handler = self._elicitation_handler
        if not handler:
            return
        try:
            result = handler(context)
            if inspect.isawaitable(result):
                result = await result
            result = cast(ElicitationResult, result)
            action_val = result.get("action", "cancel")
            rpc_result = SessionUIHandlePendingElicitationParamsResult(
                action=Action(action_val),
                content=result.get("content"),
            )
            await self.rpc.ui.handle_pending_elicitation(
                SessionUIHandlePendingElicitationParams(
                    request_id=request_id,
                    result=rpc_result,
                )
            )
        except Exception:
            # Handler failed — attempt to cancel so the request doesn't hang
            try:
                await self.rpc.ui.handle_pending_elicitation(
                    SessionUIHandlePendingElicitationParams(
                        request_id=request_id,
                        result=SessionUIHandlePendingElicitationParamsResult(
                            action=Action.CANCEL,
                        ),
                    )
                )
            except (JsonRpcError, ProcessExitedError, OSError):
                pass  # Connection lost or RPC error — nothing we can do

    def _assert_elicitation(self) -> None:
        """Raises if the host does not support elicitation."""
        ui_caps = self._capabilities.get("ui", {})
        if not ui_caps.get("elicitation"):
            raise RuntimeError(
                "Elicitation is not supported by the host. "
                "Check session.capabilities before calling UI methods."
            )

    def _register_commands(self, commands: list[CommandDefinition] | None) -> None:
        """Register command handlers for this session.

        Args:
            commands: A list of CommandDefinition objects, or None to clear all commands.
        """
        with self._command_handlers_lock:
            self._command_handlers.clear()
            if not commands:
                return
            for cmd in commands:
                self._command_handlers[cmd.name] = cmd.handler

    def _register_elicitation_handler(self, handler: ElicitationHandler | None) -> None:
        """Register the elicitation handler for this session.

        Args:
            handler: The handler to invoke when the server dispatches an
                elicitation request, or None to remove the handler.
        """
        with self._elicitation_handler_lock:
            self._elicitation_handler = handler

    def _set_capabilities(self, capabilities: SessionCapabilities | None) -> None:
        """Set the host capabilities for this session.

        Args:
            capabilities: The capabilities object from the create/resume response.
        """
        self._capabilities: SessionCapabilities = capabilities if capabilities is not None else {}

    def _register_tools(self, tools: list[Tool] | None) -> None:
        """
        Register custom tool handlers for this session.

        Tools allow the assistant to execute custom functions. When the assistant
        invokes a tool, the corresponding handler is called with the tool arguments.

        Note:
            This method is internal. Tools are typically registered when creating
            a session via :meth:`CopilotClient.create_session`.

        Args:
            tools: A list of Tool objects with their handlers, or None to clear
                all registered tools.
        """
        with self._tool_handlers_lock:
            self._tool_handlers.clear()
            if not tools:
                return
            for tool in tools:
                if not tool.name or not tool.handler:
                    continue
                self._tool_handlers[tool.name] = tool.handler

    def _get_tool_handler(self, name: str) -> ToolHandler | None:
        """
        Retrieve a registered tool handler by name.

        Note:
            This method is internal and should not be called directly.

        Args:
            name: The name of the tool to retrieve.

        Returns:
            The tool handler if found, or None if no handler is registered
            for the given name.
        """
        with self._tool_handlers_lock:
            return self._tool_handlers.get(name)

    def _register_permission_handler(self, handler: _PermissionHandlerFn | None) -> None:
        """
        Register a handler for permission requests.

        When the assistant needs permission to perform certain actions (e.g.,
        file operations), this handler is called to approve or deny the request.

        Note:
            This method is internal. Permission handlers are typically registered
            when creating a session via :meth:`CopilotClient.create_session`.

        Args:
            handler: The permission handler function, or None to remove the handler.
        """
        with self._permission_handler_lock:
            self._permission_handler = handler

    async def _handle_permission_request(
        self, request: PermissionRequest
    ) -> PermissionRequestResult:
        """
        Handle a permission request from the Copilot CLI.

        Note:
            This method is internal and should not be called directly.

        Args:
            request: The permission request data from the CLI.

        Returns:
            A dictionary containing the permission decision with a "kind" key.
        """
        with self._permission_handler_lock:
            handler = self._permission_handler

        if not handler:
            # No handler registered, deny permission
            return PermissionRequestResult()

        try:
            result = handler(request, {"session_id": self.session_id})
            if inspect.isawaitable(result):
                result = await result
            return cast(PermissionRequestResult, result)
        except Exception:  # pylint: disable=broad-except
            # Handler failed, deny permission
            return PermissionRequestResult()

    def _register_user_input_handler(self, handler: UserInputHandler | None) -> None:
        """
        Register a handler for user input requests.

        When the agent needs input from the user (via ask_user tool),
        this handler is called to provide the response.

        Note:
            This method is internal. User input handlers are typically registered
            when creating a session via :meth:`CopilotClient.create_session`.

        Args:
            handler: The user input handler function, or None to remove the handler.
        """
        with self._user_input_handler_lock:
            self._user_input_handler = handler

    async def _handle_user_input_request(self, request: dict) -> UserInputResponse:
        """
        Handle a user input request from the Copilot CLI.

        Note:
            This method is internal and should not be called directly.

        Args:
            request: The user input request data from the CLI.

        Returns:
            A dictionary containing the user's response.
        """
        with self._user_input_handler_lock:
            handler = self._user_input_handler

        if not handler:
            raise RuntimeError("User input requested but no handler registered")

        try:
            result = handler(
                UserInputRequest(
                    question=request.get("question", ""),
                    choices=request.get("choices") or [],
                    allowFreeform=request.get("allowFreeform", True),
                ),
                {"session_id": self.session_id},
            )
            if inspect.isawaitable(result):
                result = await result
            return cast(UserInputResponse, result)
        except Exception:
            raise

    def _register_transform_callbacks(
        self, callbacks: dict[str, SectionTransformFn] | None
    ) -> None:
        """Register transform callbacks for system message sections."""
        with self._transform_callbacks_lock:
            self._transform_callbacks = callbacks

    def _register_hooks(self, hooks: SessionHooks | None) -> None:
        """
        Register hook handlers for session lifecycle events.

        Hooks allow custom logic to be executed at various points during
        the session lifecycle (before/after tool use, session start/end, etc.).

        Note:
            This method is internal. Hooks are typically registered
            when creating a session via :meth:`CopilotClient.create_session`.

        Args:
            hooks: The hooks configuration object, or None to remove all hooks.
        """
        with self._hooks_lock:
            self._hooks = hooks

    async def _handle_system_message_transform(
        self, sections: dict[str, dict[str, str]]
    ) -> dict[str, dict[str, dict[str, str]]]:
        """Handle a systemMessage.transform request from the runtime."""
        with self._transform_callbacks_lock:
            callbacks = self._transform_callbacks

        result: dict[str, dict[str, str]] = {}
        for section_id, section_data in sections.items():
            content = section_data.get("content", "")
            callback = callbacks.get(section_id) if callbacks else None
            if callback:
                try:
                    transformed = callback(content)
                    if inspect.isawaitable(transformed):
                        transformed = await transformed
                    result[section_id] = {"content": str(transformed)}
                except Exception:
                    result[section_id] = {"content": content}
            else:
                result[section_id] = {"content": content}
        return {"sections": result}

    async def _handle_hooks_invoke(self, hook_type: str, input_data: Any) -> Any:
        """
        Handle a hooks invocation from the Copilot CLI.

        Note:
            This method is internal and should not be called directly.

        Args:
            hook_type: The type of hook being invoked.
            input_data: The input data for the hook.

        Returns:
            The hook output, or None if no handler is registered.
        """
        with self._hooks_lock:
            hooks = self._hooks

        if not hooks:
            return None

        handler_map = {
            "preToolUse": hooks.get("on_pre_tool_use"),
            "postToolUse": hooks.get("on_post_tool_use"),
            "userPromptSubmitted": hooks.get("on_user_prompt_submitted"),
            "sessionStart": hooks.get("on_session_start"),
            "sessionEnd": hooks.get("on_session_end"),
            "errorOccurred": hooks.get("on_error_occurred"),
        }

        handler = handler_map.get(hook_type)
        if not handler:
            return None

        try:
            result = handler(input_data, {"session_id": self.session_id})
            if inspect.isawaitable(result):
                result = await result
            return result
        except Exception:  # pylint: disable=broad-except
            # Hook failed, return None
            return None

    async def get_messages(self) -> list[SessionEvent]:
        """
        Retrieve all events and messages from this session's history.

        This returns the complete conversation history including user messages,
        assistant responses, tool executions, and other session events.

        Returns:
            A list of all session events in chronological order.

        Raises:
            Exception: If the session has been disconnected or the connection fails.

        Example:
            >>> events = await session.get_messages()
            >>> for event in events:
            ...     if event.type == "assistant.message":
            ...         print(f"Assistant: {event.data.content}")
        """
        response = await self._client.request("session.getMessages", {"sessionId": self.session_id})
        # Convert dict events to SessionEvent objects
        events_dicts = response["events"]
        return [session_event_from_dict(event_dict) for event_dict in events_dicts]

    async def disconnect(self) -> None:
        """
        Disconnect this session and release all in-memory resources (event handlers,
        tool handlers, permission handlers).

        Session state on disk (conversation history, planning state, artifacts)
        is preserved, so the conversation can be resumed later by calling
        :meth:`CopilotClient.resume_session` with the session ID. To
        permanently remove all session data including files on disk, use
        :meth:`CopilotClient.delete_session` instead.

        After calling this method, the session object can no longer be used.

        This method is idempotent—calling it multiple times is safe and will
        not raise an error if the session is already disconnected.

        Raises:
            Exception: If the connection fails (on first disconnect call).

        Example:
            >>> # Clean up when done — session can still be resumed later
            >>> await session.disconnect()
        """
        # Ensure that the check and update of _destroyed are atomic so that
        # only the first caller proceeds to send the destroy RPC.
        with self._event_handlers_lock:
            if self._destroyed:
                return
            self._destroyed = True

        try:
            await self._client.request("session.destroy", {"sessionId": self.session_id})
        finally:
            # Clear handlers even if the request fails.
            with self._event_handlers_lock:
                self._event_handlers.clear()
            with self._tool_handlers_lock:
                self._tool_handlers.clear()
            with self._permission_handler_lock:
                self._permission_handler = None
            with self._command_handlers_lock:
                self._command_handlers.clear()
            with self._elicitation_handler_lock:
                self._elicitation_handler = None

    async def destroy(self) -> None:
        """
        .. deprecated::
            Use :meth:`disconnect` instead. This method will be removed in a future release.

        Disconnect this session and release all in-memory resources.
        Session data on disk is preserved for later resumption.

        Raises:
            Exception: If the connection fails.
        """
        import warnings

        warnings.warn(
            "destroy() is deprecated, use disconnect() instead",
            DeprecationWarning,
            stacklevel=2,
        )
        await self.disconnect()

    async def __aenter__(self) -> CopilotSession:
        """Enable use as an async context manager."""
        return self

    async def __aexit__(
        self,
        exc_type: type[BaseException] | None = None,
        exc_val: BaseException | None = None,
        exc_tb: TracebackType | None = None,
    ) -> None:
        """
        Exit the async context manager.

        Automatically disconnects the session and releases all associated resources.
        """
        await self.disconnect()

    async def abort(self) -> None:
        """
        Abort the currently processing message in this session.

        Use this to cancel a long-running request. The session remains valid
        and can continue to be used for new messages.

        Raises:
            Exception: If the session has been disconnected or the connection fails.

        Example:
            >>> import asyncio
            >>>
            >>> # Start a long-running request
            >>> task = asyncio.create_task(session.send("Write a very long story..."))
            >>>
            >>> # Abort after 5 seconds
            >>> await asyncio.sleep(5)
            >>> await session.abort()
        """
        await self._client.request("session.abort", {"sessionId": self.session_id})

    async def set_model(
        self,
        model: str,
        *,
        reasoning_effort: str | None = None,
        model_capabilities: ModelCapabilitiesOverride | None = None,
    ) -> None:
        """
        Change the model for this session.

        The new model takes effect for the next message. Conversation history
        is preserved.

        Args:
            model: Model ID to switch to (e.g., "gpt-4.1", "claude-sonnet-4").
            reasoning_effort: Optional reasoning effort level for the new model
                (e.g., "low", "medium", "high", "xhigh").
            model_capabilities: Override individual model capabilities resolved by the runtime.

        Raises:
            Exception: If the session has been destroyed or the connection fails.

        Example:
            >>> await session.set_model("gpt-4.1")
            >>> await session.set_model("claude-sonnet-4.6", reasoning_effort="high")
        """
        rpc_caps = None
        if model_capabilities is not None:
            from .client import _capabilities_to_dict

            rpc_caps = _RpcModelCapabilitiesOverride.from_dict(
                _capabilities_to_dict(model_capabilities)
            )
        await self.rpc.model.switch_to(
            SessionModelSwitchToParams(
                model_id=model,
                reasoning_effort=reasoning_effort,
                model_capabilities=rpc_caps,
            )
        )

    async def log(
        self,
        message: str,
        *,
        level: str | None = None,
        ephemeral: bool | None = None,
    ) -> None:
        """
        Log a message to the session timeline.

        The message appears in the session event stream and is visible to SDK consumers
        and (for non-ephemeral messages) persisted to the session event log on disk.

        Args:
            message: The human-readable message to log.
            level: Log severity level ("info", "warning", "error"). Defaults to "info".
            ephemeral: When True, the message is transient and not persisted to disk.

        Raises:
            Exception: If the session has been destroyed or the connection fails.

        Example:
            >>> await session.log("Processing started")
            >>> await session.log("Something looks off", level="warning")
            >>> await session.log("Operation failed", level="error")
            >>> await session.log("Temporary status update", ephemeral=True)
        """
        params = SessionLogParams(
            message=message,
            level=Level(level) if level is not None else None,
            ephemeral=ephemeral,
        )
        await self.rpc.log(params)
