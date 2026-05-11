"""
Cloud session support for the Copilot SDK.

This sub-package provides the :class:`CloudSession` class for creating and
controlling sandbox-backed cloud sessions through Mission Control, along with
the low-level :class:`MissionControlClient` HTTP client.
"""

from .cloud_session import CloudSession
from .mission_control_client import CloudSessionError, MissionControlClient
from .types import (
    CloudAskUserResponsePayload,
    CloudConnectOptions,
    CloudElicitationResponsePayload,
    CloudModeSwitchPayload,
    CloudPermissionResponsePayload,
    CloudPlanApprovalResponsePayload,
    CloudProgressEvent,
    CloudProgressPhase,
    CloudRepository,
    CloudSessionEvent,
    CloudSessionEventHandler,
    CloudSessionFailureReason,
    CloudSessionMetadata,
    CloudSessionOptions,
    MissionControlCommandType,
    MissionControlTask,
    MissionControlTaskSession,
)

__all__ = [
    "CloudAskUserResponsePayload",
    "CloudConnectOptions",
    "CloudElicitationResponsePayload",
    "CloudModeSwitchPayload",
    "CloudPermissionResponsePayload",
    "CloudPlanApprovalResponsePayload",
    "CloudProgressEvent",
    "CloudProgressPhase",
    "CloudRepository",
    "CloudSession",
    "CloudSessionError",
    "CloudSessionEvent",
    "CloudSessionEventHandler",
    "CloudSessionFailureReason",
    "CloudSessionMetadata",
    "CloudSessionOptions",
    "MissionControlClient",
    "MissionControlCommandType",
    "MissionControlTask",
    "MissionControlTaskSession",
]
