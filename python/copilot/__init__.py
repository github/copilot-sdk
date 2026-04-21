"""
Copilot SDK - Python Client for GitHub Copilot CLI

JSON-RPC based SDK for programmatic control of GitHub Copilot CLI
"""

from .client import (
    CopilotClient,
    ExternalServerConfig,
    ModelCapabilitiesOverride,
    ModelLimitsOverride,
    ModelSupportsOverride,
    ModelVisionLimitsOverride,
    SubprocessConfig,
)
from .session import (
    CommandContext,
    CommandDefinition,
    CopilotSession,
    CreateSessionFsHandler,
    ElicitationContext,
    ElicitationHandler,
    ElicitationParams,
    ElicitationResult,
    InputOptions,
    ProviderConfig,
    SessionCapabilities,
    SessionFsConfig,
    SessionUiApi,
    SessionUiCapabilities,
)
from .session_fs_provider import (
    SessionFsFileInfo,
    SessionFsProvider,
    create_session_fs_adapter,
)
from .tools import convert_mcp_call_tool_result, define_tool

__version__ = "0.1.0"

__all__ = [
    "CommandContext",
    "CommandDefinition",
    "CopilotClient",
    "CopilotSession",
    "CreateSessionFsHandler",
    "ElicitationHandler",
    "ElicitationParams",
    "ElicitationContext",
    "ElicitationResult",
    "ExternalServerConfig",
    "InputOptions",
    "ModelCapabilitiesOverride",
    "ModelLimitsOverride",
    "ModelSupportsOverride",
    "ModelVisionLimitsOverride",
    "ProviderConfig",
    "SessionCapabilities",
    "SessionFsConfig",
    "SessionFsFileInfo",
    "SessionFsProvider",
    "create_session_fs_adapter",
    "SessionUiApi",
    "SessionUiCapabilities",
    "SubprocessConfig",
    "convert_mcp_call_tool_result",
    "define_tool",
]
