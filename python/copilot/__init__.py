"""
Copilot SDK - Python Client for GitHub Copilot CLI

JSON-RPC based SDK for programmatic control of GitHub Copilot CLI
"""

from .client import CopilotClient, ExternalServerConfig, SubprocessConfig
from .session import CopilotSession
from .tools import define_tool

__version__ = "0.1.0"

__all__ = [
    "CopilotClient",
    "CopilotSession",
    "ExternalServerConfig",
    "SubprocessConfig",
    "define_tool",
]
