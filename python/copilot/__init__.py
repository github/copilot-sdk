"""
Copilot SDK - Python Client for GitHub Copilot CLI

JSON-RPC based SDK for programmatic control of GitHub Copilot CLI
"""

from .client import CopilotClient
from .session import CopilotSession
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
    PingResponse,
    ProviderConfig,
    ResumeSessionConfig,
    SessionConfig,
    SessionEvent,
    SessionMetadata,
    StopError,
    Tool,
    ToolHandler,
    ToolInvocation,
    ToolResult,
)

__version__ = "0.1.0"

__all__ = [
    "AzureProviderOptions",
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
    "ModelBilling",
    "ModelCapabilities",
    "ModelInfo",
    "ModelPolicy",
    "PermissionHandler",
    "PermissionRequest",
    "PermissionRequestResult",
    "PingResponse",
    "ProviderConfig",
    "ResumeSessionConfig",
    "SessionConfig",
    "SessionEvent",
    "SessionMetadata",
    "StopError",
    "Tool",
    "ToolHandler",
    "ToolInvocation",
    "ToolResult",
    "define_tool",
]
