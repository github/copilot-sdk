"""
Type definitions for the cloud session API.

These types mirror the Node SDK's cloud session types, adapted to Python
conventions (snake_case, dataclasses, TypedDict, enums).
"""

from __future__ import annotations

import enum
from collections.abc import Callable
from dataclasses import dataclass, field
from datetime import datetime
from typing import Any, Literal, NotRequired, TypedDict

# ============================================================================
# Repository & Progress
# ============================================================================


class CloudRepository(TypedDict):
    """Repository context used when creating a cloud sandbox task."""

    owner: str
    name: str
    branch: NotRequired[str]


CloudProgressPhase = Literal[
    "creating_task",
    "provisioning_sandbox",
    "waiting_for_session",
    "connected",
]


@dataclass
class CloudProgressEvent:
    """Progress phases emitted while creating or attaching to a cloud session."""

    phase: CloudProgressPhase
    elapsed_ms: float | None = None
    task_id: str | None = None


CloudSessionFailureReason = Literal[
    "policy_blocked",
    "validation",
    "timeout",
    "network",
    "server",
]


# ============================================================================
# Mission Control Task
# ============================================================================


@dataclass
class MissionControlTaskSession:
    """A session within a Mission Control task."""

    id: str
    task_id: str
    state: str
    created_at: str
    updated_at: str
    owner_id: int
    agent_task_id: str | None = None
    name: str | None = None
    repo_id: int | None = None

    @staticmethod
    def from_dict(data: dict[str, Any]) -> MissionControlTaskSession:
        return MissionControlTaskSession(
            id=data["id"],
            task_id=data["task_id"],
            state=data["state"],
            created_at=data["created_at"],
            updated_at=data["updated_at"],
            owner_id=data["owner_id"],
            agent_task_id=data.get("agent_task_id"),
            name=data.get("name"),
            repo_id=data.get("repo_id"),
        )


@dataclass
class MissionControlTask:
    """Represents a Mission Control task."""

    id: str
    name: str
    state: str
    status: str
    creator_id: int
    owner_id: int
    session_count: int
    created_at: str
    updated_at: str
    repo_id: int | None = None
    sessions: list[MissionControlTaskSession] = field(default_factory=list)

    @staticmethod
    def from_dict(data: dict[str, Any]) -> MissionControlTask:
        sessions_data = data.get("sessions") or []
        return MissionControlTask(
            id=data["id"],
            name=data["name"],
            state=data["state"],
            status=data["status"],
            creator_id=data["creator_id"],
            owner_id=data["owner_id"],
            session_count=data["session_count"],
            created_at=data["created_at"],
            updated_at=data["updated_at"],
            repo_id=data.get("repo_id"),
            sessions=[MissionControlTaskSession.from_dict(s) for s in sessions_data],
        )


# ============================================================================
# Cloud Session Metadata
# ============================================================================


@dataclass
class CloudSessionMetadata:
    """Metadata about a cloud session, populated from a Mission Control task."""

    task_id: str
    frontend_url: str
    created_at: datetime
    updated_at: datetime
    mission_control_session_id: str | None = None
    owner: str | None = None
    repository: CloudRepository | None = None
    state: str | None = None
    status: str | None = None


# ============================================================================
# Cloud Session Events
# ============================================================================


@dataclass
class CloudSessionEvent:
    """A single event from a cloud session's event stream.

    Cloud session events include standard session events plus cloud-specific
    events like ``session.requested``.
    """

    id: str
    timestamp: str
    type: str
    parent_id: str | None = None
    data: dict[str, Any] | None = None
    ephemeral: bool | None = None

    @staticmethod
    def from_dict(data: dict[str, Any]) -> CloudSessionEvent:
        return CloudSessionEvent(
            id=data["id"],
            timestamp=data["timestamp"],
            type=data["type"],
            parent_id=data.get("parentId"),
            data=data.get("data"),
            ephemeral=data.get("ephemeral"),
        )


CloudSessionEventHandler = Callable[[CloudSessionEvent], None]
"""Event handler callback type for cloud session events."""


# ============================================================================
# Mission Control Command Types
# ============================================================================


class MissionControlCommandType(enum.StrEnum):
    """Command types for steering a cloud session through Mission Control."""

    USER_MESSAGE = "user_message"
    ASK_USER_RESPONSE = "ask_user_response"
    PLAN_APPROVAL_RESPONSE = "plan_approval_response"
    PERMISSION_RESPONSE = "permission_response"
    ELICITATION_RESPONSE = "elicitation_response"
    ABORT = "abort"
    MODE_SWITCH = "mode_switch"


# ============================================================================
# Steering Payloads
# ============================================================================


class CloudAskUserResponsePayload(TypedDict):
    """Payload for responding to an ask_user prompt."""

    prompt_id: str
    answer: str
    was_freeform: bool
    dismissed: NotRequired[bool]


class CloudPlanApprovalResponsePayload(TypedDict):
    """Payload for responding to a plan approval prompt."""

    prompt_id: str
    approved: bool
    selected_action: NotRequired[str]
    auto_approve_edits: NotRequired[bool]
    feedback: NotRequired[str]


class CloudPermissionResponsePayload(TypedDict):
    """Payload for responding to a permission prompt."""

    prompt_id: str
    approved: bool
    scope: Literal["once", "session"]


ElicitationFieldValue = str | int | float | bool | list[str]
"""Primitive field value in an elicitation result."""


class CloudElicitationResponsePayload(TypedDict):
    """Payload for responding to an elicitation prompt."""

    prompt_id: str
    action: Literal["accept", "decline", "cancel"]
    content: NotRequired[dict[str, ElicitationFieldValue]]


class CloudModeSwitchPayload(TypedDict):
    """Payload for switching the session mode."""

    mode: Literal["interactive", "plan", "autopilot"]


class ElicitationResult(TypedDict):
    """Result returned from an elicitation request."""

    action: Literal["accept", "decline", "cancel"]
    content: NotRequired[dict[str, ElicitationFieldValue]]


class ExitPlanModeResult(TypedDict):
    """Result returned from an exit-plan-mode request."""

    approved: bool
    selected_action: NotRequired[str]
    feedback: NotRequired[str]


# ============================================================================
# Options Types
# ============================================================================


class CloudSessionOptions(TypedDict, total=False):
    """Options for creating a new cloud session.

    Either ``repository`` or ``owner`` must be provided. If ``repository`` is
    omitted, ``owner`` is required for billing/authorization.
    """

    owner: str
    """Billing/authorization owner for repo-less cloud sandboxes."""

    repository: CloudRepository
    """Repository context for the cloud sandbox."""

    mission_control_base_url: str
    copilot_api_base_url: str
    frontend_base_url: str
    auth_token: str
    integration_id: str
    poll_interval_ms: int
    initial_event_timeout_ms: int
    initial_event_poll_interval_ms: int
    on_progress: Callable[[CloudProgressEvent], None]
    on_cloud_task_created: Callable[[MissionControlTask], None]
    on_event_poll_error: Callable[[Exception], None]


class CloudConnectOptions(TypedDict, total=False):
    """Options for connecting to an existing cloud session.

    Same as :class:`CloudSessionOptions` but ``repository`` is optional.
    """

    owner: str
    repository: CloudRepository
    mission_control_base_url: str
    copilot_api_base_url: str
    frontend_base_url: str
    auth_token: str
    integration_id: str
    poll_interval_ms: int
    initial_event_timeout_ms: int
    initial_event_poll_interval_ms: int
    on_progress: Callable[[CloudProgressEvent], None]
    on_event_poll_error: Callable[[Exception], None]
