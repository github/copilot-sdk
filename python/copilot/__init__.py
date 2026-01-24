"""
Copilot SDK - Python Client for GitHub Copilot CLI

JSON-RPC based SDK for programmatic control of GitHub Copilot CLI
"""

from .client import CopilotClient
from .session import CopilotSession
from .steering import (
    ConversationManager,
    MessageQueue,
    Priority,
    QueuedMessage,
    QueueFullError,
    ShutdownSentinel,
    StreamingInputGenerator,
)
from .tools import define_tool
from .types import (
    AzureProviderOptions,
    ConnectionState,
    CustomAgentConfig,
    GetAuthStatusResponse,
    GetStatusResponse,
    MCPLocalServerConfig,
    MCPRemoteServerConfig,
    MCPServerConfig,
    MessageOptions,
    ModelBilling,
    ModelCapabilities,
    ModelInfo,
    ModelPolicy,
    PermissionHandler,
    PermissionRequest,
    PermissionRequestResult,
    ProviderConfig,
    ResumeSessionConfig,
    SessionConfig,
    SessionEvent,
    SessionMetadata,
    Tool,
    ToolHandler,
    ToolInvocation,
    ToolResult,
)

__version__ = "0.1.0"

__all__ = [
    "AzureProviderOptions",
    "ConversationManager",
    "CopilotClient",
    "CopilotSession",
    "ConnectionState",
    "CustomAgentConfig",
    "GetAuthStatusResponse",
    "GetStatusResponse",
    "MCPLocalServerConfig",
    "MCPRemoteServerConfig",
    "MCPServerConfig",
    "MessageOptions",
    "MessageQueue",
    "ModelBilling",
    "ModelCapabilities",
    "ModelInfo",
    "ModelPolicy",
    "PermissionHandler",
    "PermissionRequest",
    "PermissionRequestResult",
    "Priority",
    "ProviderConfig",
    "QueuedMessage",
    "QueueFullError",
    "ResumeSessionConfig",
    "SessionConfig",
    "SessionEvent",
    "SessionMetadata",
    "ShutdownSentinel",
    "StreamingInputGenerator",
    "Tool",
    "ToolHandler",
    "ToolInvocation",
    "ToolResult",
    "define_tool",
]
