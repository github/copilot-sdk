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
    ElicitationContext,
    ElicitationHandler,
    ElicitationParams,
    ElicitationResult,
    InputOptions,
    SessionCapabilities,
    SessionUiApi,
    SessionUiCapabilities,
)
from .tools import define_tool

__version__ = "0.1.0"

__all__ = [
    "CommandContext",
    "CommandDefinition",
    "CopilotClient",
    "CopilotSession",
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
    "SessionCapabilities",
    "SessionUiApi",
    "SessionUiCapabilities",
    "SubprocessConfig",
    "define_tool",
]
